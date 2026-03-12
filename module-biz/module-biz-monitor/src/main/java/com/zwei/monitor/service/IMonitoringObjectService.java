package com.zwei.monitor.service;

import com.zwei.monitor.domain.MonitoringObject;

import java.util.List;

/**
 * 监测对象基本信息表Service接口
 * 
 * @author zwei
 * @date 2025-10-15
 */
public interface IMonitoringObjectService 
{
    /**
     * 查询监测对象基本信息表
     * 
     * @param id 监测对象基本信息表主键
     * @return 监测对象基本信息表
     */
    MonitoringObject selectMonitoringObjectById(Long id);

    /**
     * 查询监测对象基本信息表列表
     * 
     * @param monitoringObject 监测对象基本信息表
     * @return 监测对象基本信息表集合
     */
    List<MonitoringObject> selectMonitoringObjectList(MonitoringObject monitoringObject);

    /**
     * 新增监测对象基本信息表
     * 
     * @param monitoringObject 监测对象基本信息表
     * @return 结果
     */
    int insertMonitoringObject(MonitoringObject monitoringObject);

    /**
     * 修改监测对象基本信息表
     * 
     * @param monitoringObject 监测对象基本信息表
     * @return 结果
     */
    int updateMonitoringObject(MonitoringObject monitoringObject);

    /**
     * 批量删除监测对象基本信息表
     * 
     * @param ids 需要删除的监测对象基本信息表主键集合
     * @return 结果
     */
    int deleteMonitoringObjectByIds(Long[] ids);

    /**
     * 删除监测对象基本信息表信息
     * 
     * @param id 监测对象基本信息表主键
     * @return 结果
     */
    int deleteMonitoringObjectById(Long id);
}