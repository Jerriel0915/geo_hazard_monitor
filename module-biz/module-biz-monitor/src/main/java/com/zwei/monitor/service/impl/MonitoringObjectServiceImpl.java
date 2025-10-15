package com.zwei.monitor.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.zwei.monitor.mapper.MonitoringObjectMapper;
import com.zwei.monitor.mapper.MonitoringPointMapper;
import com.zwei.monitor.mapper.PointDeviceMappingMapper;
import com.zwei.monitor.domain.MonitoringObject;
import com.zwei.monitor.domain.MonitoringPoint;
import com.zwei.monitor.service.IMonitoringObjectService;
import lombok.extern.slf4j.Slf4j;

/**
 * 监测对象基本信息表Service业务层处理
 * 
 * @author zwei
 * @date 2025-10-15
 */
@Slf4j
@Service
public class MonitoringObjectServiceImpl implements IMonitoringObjectService 
{
    @Autowired
    private MonitoringObjectMapper monitoringObjectMapper;

    @Autowired
    private MonitoringPointMapper monitoringPointMapper;

    @Autowired
    private PointDeviceMappingMapper pointDeviceMappingMapper;

    /**
     * 查询监测对象基本信息表
     * 
     * @param id 监测对象基本信息表主键
     * @return 监测对象基本信息表
     */
    @Override
    public MonitoringObject selectMonitoringObjectById(Long id)
    {
        log.info("查询监测对象信息，ID: {}", id);
        return monitoringObjectMapper.selectMonitoringObjectById(id);
    }

    /**
     * 查询监测对象基本信息表列表
     * 
     * @param monitoringObject 监测对象基本信息表
     * @return 监测对象基本信息表
     */
    @Override
    public List<MonitoringObject> selectMonitoringObjectList(MonitoringObject monitoringObject)
    {
        log.info("查询监测对象列表");
        return monitoringObjectMapper.selectMonitoringObjectList(monitoringObject);
    }

    /**
     * 新增监测对象基本信息表
     * 
     * @param monitoringObject 监测对象基本信息表
     * @return 结果
     */
    @Override
    public int insertMonitoringObject(MonitoringObject monitoringObject)
    {
        log.info("新增监测对象: {}", monitoringObject.getName());
        return monitoringObjectMapper.insertMonitoringObject(monitoringObject);
    }

    /**
     * 修改监测对象基本信息表
     * 
     * @param monitoringObject 监测对象基本信息表
     * @return 结果
     */
    @Override
    public int updateMonitoringObject(MonitoringObject monitoringObject)
    {
        log.info("更新监测对象，ID: {}", monitoringObject.getId());
        return monitoringObjectMapper.updateMonitoringObject(monitoringObject);
    }

    /**
     * 批量删除监测对象基本信息表
     * 
     * @param ids 需要删除的监测对象基本信息表主键集合
     * @return 结果
     */
    @Override
    public int deleteMonitoringObjectByIds(Long[] ids)
    {
        log.info("批量删除监测对象，数量: {}", ids.length);
        
        // 先删除关联的测点设备映射
        for (Long id : ids) {
            List<MonitoringPoint> points = monitoringPointMapper.selectMonitoringPointByObjectId(id);
            if (points != null && !points.isEmpty()) {
                Long[] pointIds = points.stream().map(MonitoringPoint::getId).toArray(Long[]::new);
                pointDeviceMappingMapper.deleteByPointIds(pointIds);
                // 删除关联的监测点位
                monitoringPointMapper.deleteMonitoringPointByIds(pointIds);
            }
        }
        
        // 删除监测对象
        return monitoringObjectMapper.deleteMonitoringObjectByIds(ids);
    }

    /**
     * 删除监测对象基本信息表信息
     * 
     * @param id 监测对象基本信息表主键
     * @return 结果
     */
    @Override
    public int deleteMonitoringObjectById(Long id)
    {
        log.info("删除监测对象，ID: {}", id);
        
        // 先删除关联的测点设备映射
        List<MonitoringPoint> points = monitoringPointMapper.selectMonitoringPointByObjectId(id);
        if (points != null && !points.isEmpty()) {
            Long[] pointIds = points.stream().map(MonitoringPoint::getId).toArray(Long[]::new);
            pointDeviceMappingMapper.deleteByPointIds(pointIds);
            // 删除关联的监测点位
            monitoringPointMapper.deleteMonitoringPointByIds(pointIds);
        }
        
        // 删除监测对象
        return monitoringObjectMapper.deleteMonitoringObjectById(id);
    }
}