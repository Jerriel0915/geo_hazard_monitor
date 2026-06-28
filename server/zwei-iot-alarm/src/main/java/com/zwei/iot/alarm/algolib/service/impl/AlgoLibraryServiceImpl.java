package com.zwei.iot.alarm.algolib.service.impl;

import com.zwei.common.exception.ServiceException;
import com.zwei.iot.alarm.algolib.domain.AlgoInfo;
import com.zwei.iot.alarm.algolib.domain.AlgoVersion;
import com.zwei.iot.alarm.algolib.mapper.AlgoInfoMapper;
import com.zwei.iot.alarm.algolib.mapper.AlgoVersionMapper;
import com.zwei.iot.alarm.algolib.service.IAlgoLibraryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.Date;
import java.util.List;

/**
 * 算法信息 Service 实现。
 *
 * @author zwei
 */
@Service
public class AlgoLibraryServiceImpl implements IAlgoLibraryService {

    private final AlgoInfoMapper algoInfoMapper;
    private final AlgoVersionMapper algoVersionMapper;

    public AlgoLibraryServiceImpl(AlgoInfoMapper algoInfoMapper,
                                  AlgoVersionMapper algoVersionMapper) {
        this.algoInfoMapper = algoInfoMapper;
        this.algoVersionMapper = algoVersionMapper;
    }

    @Override
    public List<AlgoInfo> selectList(AlgoInfo query) {
        return algoInfoMapper.selectList(query);
    }

    @Override
    public AlgoInfo selectDetailById(Long id) {
        AlgoInfo info = algoInfoMapper.selectById(id);
        if (info == null) return null;
        List<AlgoVersion> versions = algoVersionMapper.selectByAlgoId(id);
        info.setVersions(versions);
        return info;
    }

    @Override
    public int insert(AlgoInfo algoInfo) {
        if (!checkCodeUnique(algoInfo.getCode(), 0L)) {
            throw new ServiceException("新增失败，算法编码已存在: " + algoInfo.getCode());
        }
        if (algoInfo.getStatus() == null) algoInfo.setStatus(1);
        Date now = new Date();
        algoInfo.setCreateTime(now);
        algoInfo.setUpdateTime(now);
        return algoInfoMapper.insert(algoInfo);
    }

    @Override
    public int update(AlgoInfo algoInfo) {
        // code 字段不可修改
        algoInfo.setCode(null);
        algoInfo.setUpdateTime(new Date());
        return algoInfoMapper.update(algoInfo);
    }

    @Override
    public int updateStatus(Long id, Integer status, String updateBy) {
        AlgoInfo update = AlgoInfo.builder()
                .id(id).status(status).updateBy(updateBy).updateTime(new Date()).build();
        return algoInfoMapper.update(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteWithVersions(Long id) {
        AlgoInfo info = algoInfoMapper.selectById(id);
        if (info == null) return 0;

        int rows = algoInfoMapper.softDelete(id);
        if (rows > 0) {
            algoVersionMapper.softDeleteByAlgoId(id);
            // 物理删除工作目录
            if (info.getCode() != null) {
                try {
                    File workDir = new File(com.zwei.common.config.RuoYiConfig.getProfile()
                            + File.separator + "algo-workspace" + File.separator + info.getCode());
                    if (workDir.exists()) {
                        deleteDirectoryRecursive(workDir);
                    }
                } catch (Exception e) {
                    // 工作目录删除失败不影响逻辑删除
                }
            }
        }
        return rows;
    }

    private static void deleteDirectoryRecursive(File dir) {
        File[] children = dir.listFiles();
        if (children != null) {
            for (File child : children) {
                if (child.isDirectory()) {
                    deleteDirectoryRecursive(child);
                } else {
                    child.delete();
                }
            }
        }
        dir.delete();
    }

    @Override
    public boolean checkCodeUnique(String code, Long id) {
        return algoInfoMapper.checkCodeUnique(code, id) == null;
    }
}
