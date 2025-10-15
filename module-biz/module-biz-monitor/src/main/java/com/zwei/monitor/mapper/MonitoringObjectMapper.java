package com.zwei.monitor.mapper;

import java.util.List;
import com.zwei.monitor.domain.MonitoringObject;

/**
 * 监测对象基本信息表Mapper接口
 * 
 * @author zwei
 * @date 2025-10-15
 */
public interface MonitoringObjectMapper 
{
    /**
     * 查询监测对象基本信息表
     * 
     * @param id 监测对象基本信息表主键
     * @return 监测对象基本信息表
     */
    public MonitoringObject selectMonitoringObjectById(Long id);

    /**
     * 查询监测对象基本信息表列表
     * 
     * @param monitoringObject 监测对象基本信息表
     * @return 监测对象基本信息表集合
     */
    public List<MonitoringObject> selectMonitoringObjectList(MonitoringObject monitoringObject);

    /**
     * 新增监测对象基本信息表
     * 
     * @param monitoringObject 监测对象基本信息表
     * @return 结果
     */
    public int insertMonitoringObject(MonitoringObject monitoringObject);

    /**
     * 修改监测对象基本信息表
     * 
     * @param monitoringObject 监测对象基本信息表
     * @return 结果
     */
    public int updateMonitoringObject(MonitoringObject monitoringObject);

    /**
     * 删除监测对象基本信息表
     * 
     * @param id 监测对象基本信息表主键
     * @return 结果
     */
    public int deleteMonitoringObjectById(Long id);

    /**
     * 批量删除监测对象基本信息表
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteMonitoringObjectByIds(Long[] ids);
}