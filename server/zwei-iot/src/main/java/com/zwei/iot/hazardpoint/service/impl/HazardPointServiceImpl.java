package com.zwei.iot.hazardpoint.service.impl;

import com.zwei.common.constant.IotConstants;
import com.zwei.common.exception.ServiceException;
import com.zwei.iot.cache.config.CacheWarmupTaskRegistry;
import com.zwei.iot.cache.service.IotCacheService;
import com.zwei.iot.cache.warmup.HazardPointWarmupTask;
import com.zwei.iot.hazardpoint.domain.HazardPoint;
import com.zwei.iot.hazardpoint.mapper.HazardPointMapper;
import com.zwei.iot.hazardpoint.service.IHazardPointService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
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
public class HazardPointServiceImpl implements IHazardPointService
{
    private final HazardPointMapper hazardPointMapper;
    private final IotCacheService cacheService;
    private final CacheWarmupTaskRegistry registry;

    @Autowired
    public HazardPointServiceImpl(HazardPointMapper hazardPointMapper, IotCacheService cacheService,
                                  CacheWarmupTaskRegistry registry) {
        this.hazardPointMapper = hazardPointMapper;
        this.cacheService = cacheService;
        this.registry = registry;
    }

    @PostConstruct
    public void init() {
        registry.registerTask(new HazardPointWarmupTask(this, cacheService));
    }

    /**
     * 根据条件分页查询隐患点列表
     *
     * @param hazardPoint 隐患点信息
     * @return 隐患点集合
     */
    @Override
    public List<HazardPoint> selectHazardPointList(HazardPoint hazardPoint)
    {
        return hazardPointMapper.selectHazardPointList(hazardPoint);
    }

    /**
     * 根据ID查询隐患点
     *
     * @param id 隐患点ID
     * @return 隐患点信息
     */
    @Override
    public HazardPoint selectHazardPointById(Long id)
    {
        // 先尝试从缓存获取
        HazardPoint cached = cacheService.getHazardPoint(id);
        if (cached != null) {
            return cached;
        }
        // 缓存未命中，查询数据库并缓存
        HazardPoint point = hazardPointMapper.selectHazardPointById(id);
        if (point != null) {
            cacheService.cacheHazardPoint(point);
        }
        return point;
    }

    /**
     * 新增隐患点
     *
     * @param hazardPoint 隐患点信息
     * @return 结果
     */
    @Override
    public int insertHazardPoint(HazardPoint hazardPoint)
    {
        // 校验编号唯一性
        if (!checkHazardPointCodeUnique(hazardPoint.getCode()))
        {
            throw new ServiceException("隐患点编号已存在");
        }
        // 默认状态为监测中
        if (hazardPoint.getStatus() == null)
        {
            hazardPoint.setStatus(IotConstants.HAZARD_POINT_STATUS_MONITORING);
        }
        int result = hazardPointMapper.insertHazardPoint(hazardPoint);
        // 缓存新增的隐患点
        if (result > 0 && hazardPoint.getId() != null) {
            cacheService.cacheHazardPoint(hazardPoint);
        }
        return result;
    }

    /**
     * 修改隐患点
     *
     * @param hazardPoint 隐患点信息
     * @return 结果
     */
    @Override
    public int updateHazardPoint(HazardPoint hazardPoint)
    {
        int result = hazardPointMapper.updateHazardPoint(hazardPoint);
        // 更新缓存
        if (result > 0 && hazardPoint.getId() != null) {
            cacheService.evictHazardPoint(hazardPoint.getId());
        }
        return result;
    }

    /**
     * 删除隐患点
     *
     * @param id 隐患点ID
     * @return 结果
     */
    @Override
    public int deleteHazardPointById(Long id)
    {
        int result = hazardPointMapper.deleteHazardPointById(id);
        // 删除缓存
        if (result > 0) {
            cacheService.evictHazardPoint(id);
        }
        return result;
    }

    /**
     * 批量删除隐患点
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    @Override
    public int deleteHazardPointByIds(Long[] ids)
    {
        int result = hazardPointMapper.deleteHazardPointByIds(ids);
        // 批量删除缓存
        if (result > 0) {
            cacheService.evictHazardPointList(ids);
        }
        return result;
    }

    /**
     * 校验隐患点编号是否唯一
     *
     * @param code 隐患点编号
     * @return 结果
     */
    @Override
    public boolean checkHazardPointCodeUnique(String code)
    {
        if (!StringUtils.hasText(code))
        {
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
    public int updateHazardPointPause(Long id, boolean pause)
    {
        // 停测: 状态变为2, 恢复: 状态变为1
        Integer newStatus = pause ? IotConstants.HAZARD_POINT_STATUS_PAUSED : IotConstants.HAZARD_POINT_STATUS_MONITORING;
        int result = hazardPointMapper.updateHazardPointStatus(id, newStatus);
        if (result > 0) {
            cacheService.evictHazardPoint(id);
        }
        return result;
    }

    /**
     * 完结隐患点
     *
     * @param id 隐患点ID
     * @return 结果
     */
    @Override
    public int completeHazardPoint(Long id)
    {
        // 完结: 状态变为3
        int result = hazardPointMapper.updateHazardPointStatus(id, IotConstants.HAZARD_POINT_STATUS_COMPLETED);
        if (result > 0) {
            cacheService.evictHazardPoint(id);
        }
        return result;
    }

    /**
     * 批量操作隐患点(停测/恢复/完结)
     *
     * @param ids 隐患点ID数组
     * @param operation 操作类型: pause/resume/complete
     * @return 结果
     */
    @Override
    public int batchOperateHazardPoint(Long[] ids, String operation)
    {
        if (ids == null || ids.length == 0)
        {
            throw new ServiceException("请选择要操作的隐患点");
        }

        Integer newStatus;
        if (IotConstants.OPERATION_PAUSE.equals(operation))
        {
            newStatus = IotConstants.HAZARD_POINT_STATUS_PAUSED;
        }
        else if (IotConstants.OPERATION_RESUME.equals(operation))
        {
            newStatus = IotConstants.HAZARD_POINT_STATUS_MONITORING;
        }
        else if (IotConstants.OPERATION_COMPLETE.equals(operation))
        {
            newStatus = IotConstants.HAZARD_POINT_STATUS_COMPLETED;
        }
        else
        {
            throw new ServiceException("无效的操作类型");
        }

        int result = hazardPointMapper.batchUpdateHazardPointStatus(Arrays.asList(ids), newStatus);
        if (result > 0) {
            cacheService.evictHazardPointList(ids);
        }
        return result;
    }
}
