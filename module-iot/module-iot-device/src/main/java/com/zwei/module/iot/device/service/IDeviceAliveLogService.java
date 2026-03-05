package com.zwei.module.iot.device.service;

import com.zwei.module.iot.device.domain.DeviceAliveLog;

import java.util.List;

/**
 * 状态日志Service接口
 * 
 * @author linx
 * @date 2025-09-05
 */
public interface IDeviceAliveLogService 
{
    /**
     * 查询状态日志
     * 
     * @param id 状态日志主键
     * @return 状态日志
     */
    public DeviceAliveLog selectDeviceAliveLogById(Long id);

    /**
     * 查询状态日志列表
     * 
     * @param deviceAliveLog 状态日志
     * @return 状态日志集合
     */
    public List<DeviceAliveLog> selectDeviceAliveLogList(DeviceAliveLog deviceAliveLog);

    /**
     * 新增状态日志
     * 
     * @param deviceAliveLog 状态日志
     * @return 结果
     */
    public int insertDeviceAliveLog(DeviceAliveLog deviceAliveLog);

    /**
     * 修改状态日志
     * 
     * @param deviceAliveLog 状态日志
     * @return 结果
     */
    public int updateDeviceAliveLog(DeviceAliveLog deviceAliveLog);

    /**
     * 批量删除状态日志
     * 
     * @param ids 需要删除的状态日志主键集合
     * @return 结果
     */
    @Deprecated
    public int deleteDeviceAliveLogByIds(Long[] ids);

    /**
     * 删除状态日志信息
     * 
     * @param id 状态日志主键
     * @return 结果
     */
    @Deprecated
    public int deleteDeviceAliveLogById(Long id);
}