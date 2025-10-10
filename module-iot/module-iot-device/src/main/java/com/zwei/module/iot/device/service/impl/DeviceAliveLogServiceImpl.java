package com.zwei.module.iot.device.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.zwei.module.iot.device.mapper.DeviceAliveLogMapper;
import com.zwei.module.iot.device.domain.DeviceAliveLog;
import com.zwei.module.iot.device.service.IDeviceAliveLogService;

/**
 * 状态日志Service业务层处理
 * 
 * @author linx
 * @date 2025-09-05
 */
@Service
public class DeviceAliveLogServiceImpl implements IDeviceAliveLogService 
{
    @Autowired
    private DeviceAliveLogMapper deviceAliveLogMapper;

    /**
     * 查询状态日志
     * 
     * @param id 状态日志主键
     * @return 状态日志
     */
    @Override
    public DeviceAliveLog selectDeviceAliveLogById(Long id)
    {
        return deviceAliveLogMapper.selectDeviceAliveLogById(id);
    }

    /**
     * 查询状态日志列表
     * 
     * @param deviceAliveLog 状态日志
     * @return 状态日志
     */
    @Override
    public List<DeviceAliveLog> selectDeviceAliveLogList(DeviceAliveLog deviceAliveLog)
    {
        return deviceAliveLogMapper.selectDeviceAliveLogList(deviceAliveLog);
    }

    /**
     * 新增状态日志
     * 
     * @param deviceAliveLog 状态日志
     * @return 结果
     */
    @Override
    public int insertDeviceAliveLog(DeviceAliveLog deviceAliveLog)
    {
        return deviceAliveLogMapper.insertDeviceAliveLog(deviceAliveLog);
    }

    /**
     * 修改状态日志
     * 
     * @param deviceAliveLog 状态日志
     * @return 结果
     */
    @Override
    public int updateDeviceAliveLog(DeviceAliveLog deviceAliveLog)
    {
        return deviceAliveLogMapper.updateDeviceAliveLog(deviceAliveLog);
    }

    /**
     * 批量删除状态日志
     * 
     * @param ids 需要删除的状态日志主键
     * @return 结果
     */
    @Override
    public int deleteDeviceAliveLogByIds(Long[] ids)
    {
        return deviceAliveLogMapper.deleteDeviceAliveLogByIds(ids);
    }

    /**
     * 删除状态日志信息
     * 
     * @param id 状态日志主键
     * @return 结果
     */
    @Override
    public int deleteDeviceAliveLogById(Long id)
    {
        return deviceAliveLogMapper.deleteDeviceAliveLogById(id);
    }
}