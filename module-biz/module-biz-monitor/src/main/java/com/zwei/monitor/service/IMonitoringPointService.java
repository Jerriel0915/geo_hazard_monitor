package com.zwei.monitor.service;

import java.util.List;
import com.zwei.monitor.domain.MonitoringPoint;
import com.zwei.monitor.domain.PointDevice;

/**
 * 监测点位(测站点)Service接口
 * 
 * @author zwei
 * @date 2025-10-15
 */
public interface IMonitoringPointService 
{
    /**
     * 查询监测点位(测站点)
     * 
     * @param id 监测点位(测站点)主键
     * @return 监测点位(测站点)
     */
    public MonitoringPoint selectMonitoringPointById(Long id);

    /**
     * 查询监测点位(测站点)列表
     * 
     * @param monitoringPoint 监测点位(测站点)
     * @return 监测点位(测站点)集合
     */
    public List<MonitoringPoint> selectMonitoringPointList(MonitoringPoint monitoringPoint);

    /**
     * 根据对象ID查询监测点位列表
     * 
     * @param objectId 对象ID
     * @return 监测点位集合
     */
    public List<MonitoringPoint> selectMonitoringPointByObjectId(Long objectId);

    /**
     * 新增监测点位(测站点)
     * 
     * @param monitoringPoint 监测点位(测站点)
     * @return 结果
     */
    public int insertMonitoringPoint(MonitoringPoint monitoringPoint);

    /**
     * 修改监测点位(测站点)
     * 
     * @param monitoringPoint 监测点位(测站点)
     * @return 结果
     */
    public int updateMonitoringPoint(MonitoringPoint monitoringPoint);

    /**
     * 批量删除监测点位(测站点)
     * 
     * @param ids 需要删除的监测点位(测站点)主键集合
     * @return 结果
     */
    public int deleteMonitoringPointByIds(Long[] ids);

    /**
     * 删除监测点位(测站点)信息
     * 
     * @param id 监测点位(测站点)主键
     * @return 结果
     */
    public int deleteMonitoringPointById(Long id);

    /**
     * 根据监测点位ID查询关联的设备映射
     * 
     * @param pointId 监测点位ID
     * @return 设备映射集合
     */
    public List<PointDevice> selectByPointId(Long pointId);
}