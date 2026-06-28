package com.zwei.iot.alarm.algolib.service.impl;

import com.zwei.common.config.RuoYiConfig;
import com.zwei.common.exception.ServiceException;
import com.zwei.common.utils.file.FileUploadUtils;
import com.zwei.iot.alarm.algolib.domain.AlgoInfo;
import com.zwei.iot.alarm.algolib.domain.AlgoVersion;
import com.zwei.iot.alarm.algolib.mapper.AlgoInfoMapper;
import com.zwei.iot.alarm.algolib.mapper.AlgoVersionMapper;
import com.zwei.iot.alarm.algolib.service.IAlgoVersionService;
import com.zwei.iot.alarm.service.engine.AlgoResult;
import com.zwei.iot.alarm.service.engine.PythonAlgoExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Date;
import java.util.List;

/**
 * 算法版本 Service 实现。
 *
 * @author zwei
 */
@Service
public class AlgoVersionServiceImpl implements IAlgoVersionService {

    private static final Logger log = LoggerFactory.getLogger(AlgoVersionServiceImpl.class);

    /** 单文件最大 100MB */
    private static final long MAX_SIZE = 100L * 1024 * 1024;

    private final AlgoInfoMapper algoInfoMapper;
    private final AlgoVersionMapper algoVersionMapper;
    private final PythonAlgoExecutor pythonAlgoExecutor;

    public AlgoVersionServiceImpl(AlgoInfoMapper algoInfoMapper,
                                  AlgoVersionMapper algoVersionMapper,
                                  PythonAlgoExecutor pythonAlgoExecutor) {
        this.algoInfoMapper = algoInfoMapper;
        this.algoVersionMapper = algoVersionMapper;
        this.pythonAlgoExecutor = pythonAlgoExecutor;
    }

    /** 测试可通过子类覆盖注入；生产用 RuoYiConfig.getProfile() */
    protected String getProfilePath() {
        return RuoYiConfig.getProfile();
    }

    @Override
    public List<AlgoVersion> selectByAlgoId(Long algoId) {
        return algoVersionMapper.selectByAlgoId(algoId);
    }

    @Override
    public AlgoVersion selectById(Long id) {
        return algoVersionMapper.selectById(id);
    }

    @Override
    public Long upload(Long algoId, String versionNo, String remark,
                       MultipartFile file, String createBy) {
        // 1. 校验文件
        if (file == null || file.isEmpty()) {
            throw new ServiceException("上传文件不能为空");
        }
        String original = file.getOriginalFilename();
        if (original == null || !original.toLowerCase().endsWith(".zip")) {
            throw new ServiceException("仅支持 zip 格式算法包");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new ServiceException("文件大小不能超过 100MB");
        }

        // 2. 校验算法存在
        AlgoInfo algo = algoInfoMapper.selectById(algoId);
        if (algo == null) {
            throw new ServiceException("算法不存在或已删除: " + algoId);
        }

        // 3. 校验版本号唯一
        if (!checkVersionUnique(algoId, versionNo)) {
            throw new ServiceException("版本号已存在: " + versionNo);
        }

        // 4. 落盘
        String relativePath;
        String workPathRelative;
        try {
            relativePath = FileUploadUtils.extractAlgoLibFilename(file);
            File dest = new File(getProfilePath() + File.separator + relativePath);
            if (!dest.getParentFile().exists() && !dest.getParentFile().mkdirs()) {
                throw new IOException("创建目录失败: " + dest.getParent());
            }
            file.transferTo(dest);

            // 4.1 解压到工作目录
            String workspaceDir = "algo-workspace";
            workPathRelative = workspaceDir + "/" + algo.getCode() + "/" + versionNo;
            File workDir = new File(getProfilePath() + File.separator + workPathRelative);
            if (workDir.exists()) {
                deleteDirectory(workDir);
            }
            if (!workDir.mkdirs()) {
                throw new ServiceException("创建工作目录失败: " + workDir.getAbsolutePath());
            }
            try {
                java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(
                        new java.io.FileInputStream(dest));
                java.util.zip.ZipEntry entry;
                byte[] buffer = new byte[1024];
                while ((entry = zis.getNextEntry()) != null) {
                    File entryFile = new File(workDir, entry.getName());
                    if (!entryFile.getCanonicalPath().startsWith(workDir.getCanonicalPath())) {
                        zis.close();
                        throw new ServiceException("zip 包含非法路径: " + entry.getName());
                    }
                    if (entry.isDirectory()) {
                        entryFile.mkdirs();
                    } else {
                        entryFile.getParentFile().mkdirs();
                        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(entryFile)) {
                            int len;
                            while ((len = zis.read(buffer)) > 0) {
                                fos.write(buffer, 0, len);
                            }
                        }
                    }
                    zis.closeEntry();
                }
                zis.close();
            } catch (java.io.IOException e) {
                throw new ServiceException("解压失败: " + e.getMessage());
            }

            // 4.2 校验 algo_entry.py 存在
            if (!new File(workDir, "algo_entry.py").exists()) {
                deleteDirectory(workDir);
                throw new ServiceException("算法包缺少 algo_entry.py 入口文件");
            }
        } catch (IOException e) {
            log.error("算法包上传落盘失败 algoId={}, versionNo={}", algoId, versionNo, e);
            throw new ServiceException("算法包保存失败: " + e.getMessage());
        }

        // 5. 计算 SHA256
        String sha256;
        try {
            sha256 = sha256Hex(new File(getProfilePath() + File.separator + relativePath));
        } catch (Exception e) {
            log.warn("SHA256 计算失败，继续入库: {}", e.getMessage());
            sha256 = null;
        }

        // 6. 入库
        AlgoVersion version = AlgoVersion.builder()
                .algoId(algoId)
                .versionNo(versionNo)
                .fileName(relativePath)
                .originalName(original)
                .fileSize(file.getSize())
                .sha256(sha256)
                .workPath(workPathRelative)
                .remark(remark)
                .createBy(createBy)
                .createTime(new Date())
                .build();
        algoVersionMapper.insert(version);

        return version.getId();
    }

    @Override
    public int delete(Long id) {
        AlgoVersion version = algoVersionMapper.selectById(id);
        if (version == null) {
            return 0;
        }
        if (version.getWorkPath() != null) {
            File workDir = new File(getProfilePath() + File.separator + version.getWorkPath());
            if (workDir.exists()) {
                deleteDirectory(workDir);
            }
        }
        return algoVersionMapper.softDeleteById(id);
    }

    @Override
    public boolean checkVersionUnique(Long algoId, String versionNo) {
        return algoVersionMapper.checkVersionUnique(algoId, versionNo) == null;
    }

    private static String sha256Hex(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] bytes = Files.readAllBytes(Paths.get(file.getAbsolutePath()));
        byte[] hash = digest.digest(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    @Override
    public AlgoResult describe(String algoCode, String versionNo) {
        return pythonAlgoExecutor.describe(algoCode, versionNo);
    }

    @Override
    public AlgoResult describeLatest(String algoCode) {
        AlgoInfo algo = algoInfoMapper.selectByCode(algoCode);
        if (algo == null) {
            return AlgoResult.fail("算法不存在: " + algoCode);
        }
        AlgoVersion version = algoVersionMapper.selectLatestByAlgoId(algo.getId());
        if (version == null) {
            return AlgoResult.fail("算法无可用版本");
        }
        return pythonAlgoExecutor.describe(algoCode, version.getVersionNo());
    }

    private static void deleteDirectory(File dir) {
        File[] children = dir.listFiles();
        if (children != null) {
            for (File child : children) {
                if (child.isDirectory()) {
                    deleteDirectory(child);
                } else {
                    child.delete();
                }
            }
        }
        dir.delete();
    }
}
