package com.zwei.monitor.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.zwei.monitor.mapper.MonitoringPointMapper;
import com.zwei.monitor.mapper.PointDeviceMappingMapper;
import com.zwei.monitor.domain.MonitoringPoint;
import com.zwei.monitor.domain.PointDevice;
import com.zwei.monitor.service.IMonitoringPointService;
import lombok.extern.slf4j.Slf4j;

/**
 * 监测点位(测站点)Service业务层处理
 * 
 * @author zwei
 * @date 2025-10-15
 */
@Slf4j
@Service
public class MonitoringPointServiceImpl implements IMonitoringPointService 
{
    @Autowired
    private MonitoringPointMapper monitoringPointMapper;

    @Autowired
    private PointDeviceMappingMapper pointDeviceMappingMapper;

    /**
     * 查询监测点位(测站点)
     * 
     * @param id 监测点位(测站点)主键
     * @return 监测点位(测站点)
     */
    @Override
    public MonitoringPoint selectMonitoringPointById(Long id)
    {
        log.info("查询监测点位信息，ID: {}", id);
        return monitoringPointMapper.selectMonitoringPointById(id);
    }

    /**
     * 查询监测点位(测站点)列表
     * 
     * @param monitoringPoint 监测点位(测站点)
     * @return 监测点位(测站点)
     */
    @Override
    public List<MonitoringPoint> selectMonitoringPointList(MonitoringPoint monitoringPoint)
    {
        log.info("查询监测点位列表");
        return monitoringPointMapper.selectMonitoringPointList(monitoringPoint);
    }

    /**
     * 根据对象ID查询监测点位列表
     * 
     * @param objectId 对象ID
     * @return 监测点位集合
     */
    @Override
    public List<MonitoringPoint> selectMonitoringPointByObjectId(Long objectId)
    {
        log.info("查询对象下的监测点位列表，对象ID: {}", objectId);
        return monitoringPointMapper.selectMonitoringPointByObjectId(objectId);
    }

    /**
     * 新增监测点位(测站点)
     * 
     * @param monitoringPoint 监测点位(测站点)
     * @return 结果
     */
    @Override
    public int insertMonitoringPoint(MonitoringPoint monitoringPoint)
    {
        log.info("新增监测点位: {}", monitoringPoint.getName());
        return monitoringPointMapper.insertMonitoringPoint(monitoringPoint);
    }

    /**
     * 修改监测点位(测站点)
     * 
     * @param monitoringPoint 监测点位(测站点)
     * @return 结果
     */
    @Override
    public int updateMonitoringPoint(MonitoringPoint monitoringPoint)
    {
        log.info("更新监测点位，ID: {}", monitoringPoint.getId());
        return monitoringPointMapper.updateMonitoringPoint(monitoringPoint);
    }

    /**
     * 批量删除监测点位(测站点)
     * 
     * @param ids 需要删除的监测点位(测站点)主键集合
     * @return 结果
     */
    @Override
    public int deleteMonitoringPointByIds(Long[] ids)
    {
        log.info("批量删除监测点位，数量: {}", ids.length);
        
        // 先删除关联的测点设备映射
        pointDeviceMappingMapper.deleteByPointIds(ids);
        
        // 删除监测点位
        return monitoringPointMapper.deleteMonitoringPointByIds(ids);
    }

    /**
     * 删除监测点位(测站点)信息
     * 
     * @param id 监测点位(测站点)主键
     * @return 结果
     */
    @Override
    public int deleteMonitoringPointById(Long id)
    {
        log.info("删除监测点位，ID: {}", id);
        
        // 先删除关联的测点设备映射
        pointDeviceMappingMapper.deleteByPointId(id);
        
        // 删除监测点位
        return monitoringPointMapper.deleteMonitoringPointById(id);
    }

    /**
     * 根据监测点位ID查询关联的设备映射
     * 
     * @param pointId 监测点位ID
     * @return 设备映射集合
     */
    @Override
    public List<PointDevice> selectByPointId(Long pointId)
    {
        log.info("查询监测点位ID: {} 关联的设备映射", pointId);
        return pointDeviceMappingMapper.selectByPointId(pointId);
    }
}