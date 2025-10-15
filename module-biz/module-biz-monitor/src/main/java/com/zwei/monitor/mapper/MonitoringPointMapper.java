package com.zwei.monitor.mapper;

import java.util.List;
import com.zwei.monitor.domain.MonitoringPoint;

/**
 * 监测点位(测站点)Mapper接口
 * 
 * @author zwei
 * @date 2025-10-15
 */
public interface MonitoringPointMapper 
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
     * 删除监测点位(测站点)
     * 
     * @param id 监测点位(测站点)主键
     * @return 结果
     */
    public int deleteMonitoringPointById(Long id);

    /**
     * 批量删除监测点位(测站点)
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteMonitoringPointByIds(Long[] ids);
    
    /**
     * 批量删除对象下的所有监测点位
     * 
     * @param objectIds 对象ID集合
     * @return 结果
     */
    public int deleteMonitoringPointByObjectIds(Long[] objectIds);
}