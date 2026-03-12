package com.zwei.module.iot.device.mapper;

import com.zwei.module.iot.device.domain.DeviceAliveLog;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 状态日志Mapper接口
 * 
 * @author linx
 * @date 2025-09-05
 */
@Mapper
public interface DeviceAliveLogMapper 
{
    /**
     * 查询状态日志
     * 
     * @param id 状态日志主键
     * @return 状态日志
     */
    DeviceAliveLog selectDeviceAliveLogById(Long id);

    /**
     * 查询状态日志列表
     * 
     * @param deviceAliveLog 状态日志
     * @return 状态日志集合
     */
    List<DeviceAliveLog> selectDeviceAliveLogList(DeviceAliveLog deviceAliveLog);

    /**
     * 新增状态日志
     * 
     * @param deviceAliveLog 状态日志
     * @return 结果
     */
    int insertDeviceAliveLog(DeviceAliveLog deviceAliveLog);

    /**
     * 修改状态日志
     * 
     * @param deviceAliveLog 状态日志
     * @return 结果
     */
    int updateDeviceAliveLog(DeviceAliveLog deviceAliveLog);

    /**
     * 删除状态日志
     * 
     * @param id 状态日志主键
     * @return 结果
     */
    int deleteDeviceAliveLogById(Long id);

    /**
     * 批量删除状态日志
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    int deleteDeviceAliveLogByIds(Long[] ids);
}