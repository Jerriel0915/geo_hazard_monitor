package com.zwei.iot.hazardpoint.service.impl;

import com.zwei.common.constant.IotConstants;
import com.zwei.common.exception.ServiceException;
import com.zwei.iot.hazardpoint.domain.HazardPoint;
import com.zwei.iot.hazardpoint.domain.HazardPointGroup;
import com.zwei.iot.hazardpoint.mapper.HazardPointMapper;
import com.zwei.iot.hazardpoint.mapper.HazardPointGroupMapper;
import com.zwei.iot.hazardpoint.service.IHazardPointService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;

/**
 * 隐患点Service实现
 *
 * @author zwei
 */
@Service
public class HazardPointServiceImpl implements IHazardPointService {
    private final HazardPointMapper hazardPointMapper;
    private final HazardPointGroupMapper hazardPointGroupMapper;

    @Autowired
    public HazardPointServiceImpl(HazardPointMapper hazardPointMapper,
                                  HazardPointGroupMapper hazardPointGroupMapper) {
        this.hazardPointMapper = hazardPointMapper;
        this.hazardPointGroupMapper = hazardPointGroupMapper;
    }

    /**
     * 根据条件分页查询隐患点列表
     *
     * @param hazardPoint 隐患点信息
     * @return 隐患点集合
     */
    @Override
    public List<HazardPoint> selectHazardPointList(HazardPoint hazardPoint) {
        return hazardPointMapper.selectHazardPointList(hazardPoint);
    }

    /**
     * 根据ID查询隐患点
     *
     * @param id 隐患点ID
     * @return 隐患点信息
     */
    @Override
    @Cacheable(value = "hazardPoint", key = "#id")
    public HazardPoint selectHazardPointById(Long id) {
        return hazardPointMapper.selectHazardPointById(id);
    }

    /**
     * 新增隐患点
     *
     * @param hazardPoint 隐患点信息
     * @return 结果
     */
    @Override
    @Caching(evict = {
            @CacheEvict(value = "hazardPoint", key = "#hazardPoint.id"),
            @CacheEvict(value = "hazardPointList", allEntries = true)
    })
    public int insertHazardPoint(HazardPoint hazardPoint) {
        if (!checkHazardPointCodeUnique(hazardPoint.getCode())) {
            throw new ServiceException("隐患点编号已存在");
        }
        if (hazardPoint.getStatus() == null) {
            hazardPoint.setStatus(IotConstants.HAZARD_POINT_STATUS_MONITORING);
        }
        hazardPoint.setGroupName(resolveGroupName(hazardPoint.getGroupId()));
        return hazardPointMapper.insertHazardPoint(hazardPoint);
    }

    /**
     * 修改隐患点
     *
     * @param hazardPoint 隐患点信息
     * @return 结果
     */
    @Override
    @Caching(evict = {
            @CacheEvict(value = "hazardPoint", key = "#hazardPoint.id"),
            @CacheEvict(value = "hazardPointList", allEntries = true)
    })
    public int updateHazardPoint(HazardPoint hazardPoint) {
        HazardPoint existing = hazardPointMapper.selectHazardPointById(hazardPoint.getId());
        if (existing == null) {
            throw new ServiceException("隐患点不存在");
        }
        hazardPoint.setGroupName(resolveGroupName(hazardPoint.getGroupId()));
        return hazardPointMapper.updateHazardPoint(hazardPoint);
    }

    /**
     * 删除隐患点
     *
     * @param id 隐患点ID
     * @return 结果
     */
    @Override
    @Caching(evict = {
            @CacheEvict(value = "hazardPoint", key = "#id"),
            @CacheEvict(value = "hazardPointList", allEntries = true)
    })
    public int deleteHazardPointById(Long id) {
        return hazardPointMapper.deleteHazardPointById(id);
    }

    /**
     * 批量删除隐患点
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    @Override
    @Caching(evict = {
            @CacheEvict(value = "hazardPoint", allEntries = true),
            @CacheEvict(value = "hazardPointList", allEntries = true)
    })
    public int deleteHazardPointByIds(Long[] ids) {
        return hazardPointMapper.deleteHazardPointByIds(ids);
    }

    /**
     * 校验隐患点编号是否唯一
     *
     * @param code 隐患点编号
     * @return 结果
     */
    @Override
    public boolean checkHazardPointCodeUnique(String code) {
        if (!StringUtils.hasText(code)) {
            return true;
        }
        HazardPoint existing = hazardPointMapper.checkHazardPointCodeUnique(code);
        return existing == null;
    }

    /**
     * 停测/恢复隐患点
     *
     * @param id 隐患点ID
     * @param pause true-停测, false-恢复
     * @return 结果
     */
    @Override
    @CacheEvict(value = "hazardPoint", key = "#id")
    public int updateHazardPointPause(Long id, boolean pause) {
        Integer newStatus = pause ? IotConstants.HAZARD_POINT_STATUS_PAUSED : IotConstants.HAZARD_POINT_STATUS_MONITORING;
        return hazardPointMapper.updateHazardPointStatus(id, newStatus);
    }

    /**
     * 完结隐患点
     *
     * @param id 隐患点ID
     * @return 结果
     */
    @Override
    @CacheEvict(value = "hazardPoint", key = "#id")
    public int completeHazardPoint(Long id) {
        return hazardPointMapper.updateHazardPointStatus(id, IotConstants.HAZARD_POINT_STATUS_COMPLETED);
    }

    /**
     * 批量操作隐患点(停测/恢复/完结)
     *
     * @param ids 隐患点ID数组
     * @param operation 操作类型: pause/resume/complete
     * @return 结果
     */
    @Override
    @Caching(evict = {
            @CacheEvict(value = "hazardPoint", allEntries = true),
            @CacheEvict(value = "hazardPointList", allEntries = true)
    })
    public int batchOperateHazardPoint(Long[] ids, String operation) {
        if (ids == null || ids.length == 0) {
            throw new ServiceException("请选择要操作的隐患点");
        }

        Integer newStatus;
        if (IotConstants.OPERATION_PAUSE.equals(operation)) {
            newStatus = IotConstants.HAZARD_POINT_STATUS_PAUSED;
        } else if (IotConstants.OPERATION_RESUME.equals(operation)) {
            newStatus = IotConstants.HAZARD_POINT_STATUS_MONITORING;
        } else if (IotConstants.OPERATION_COMPLETE.equals(operation)) {
            newStatus = IotConstants.HAZARD_POINT_STATUS_COMPLETED;
        } else {
            throw new ServiceException("无效的操作类型");
        }

        return hazardPointMapper.batchUpdateHazardPointStatus(Arrays.asList(ids), newStatus);
    }

    private String resolveGroupName(Long groupId) {
        if (groupId == null) {
            return null;
        }
        HazardPointGroup group = hazardPointGroupMapper.selectHazardPointGroupById(groupId);
        if (group == null) {
            throw new ServiceException("隐患点分组不存在");
        }
        return group.getName();
    }
}
