package com.zwei.iot.alarm.algolib.service.impl;

import com.zwei.common.config.RuoYiConfig;
import com.zwei.common.exception.ServiceException;
import com.zwei.common.utils.file.FileUploadUtils;
import com.zwei.iot.alarm.algolib.domain.AlgoInfo;
import com.zwei.iot.alarm.algolib.domain.AlgoVersion;
import com.zwei.iot.alarm.algolib.mapper.AlgoInfoMapper;
import com.zwei.iot.alarm.algolib.mapper.AlgoVersionMapper;
import com.zwei.iot.alarm.algolib.service.IAlgoVersionService;
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

    public AlgoVersionServiceImpl(AlgoInfoMapper algoInfoMapper,
                                  AlgoVersionMapper algoVersionMapper) {
        this.algoInfoMapper = algoInfoMapper;
        this.algoVersionMapper = algoVersionMapper;
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
        try {
            relativePath = FileUploadUtils.extractAlgoLibFilename(file);
            File dest = new File(getProfilePath() + File.separator + relativePath);
            if (!dest.getParentFile().exists() && !dest.getParentFile().mkdirs()) {
                throw new IOException("创建目录失败: " + dest.getParent());
            }
            file.transferTo(dest);
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
                .remark(remark)
                .createBy(createBy)
                .createTime(new Date())
                .build();
        algoVersionMapper.insert(version);

        return version.getId();
    }

    @Override
    public int delete(Long id) {
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
}
