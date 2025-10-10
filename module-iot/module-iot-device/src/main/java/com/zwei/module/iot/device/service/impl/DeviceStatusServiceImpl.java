package com.zwei.module.iot.device.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.zwei.module.iot.device.mapper.DeviceStatusMapper;
import com.zwei.module.iot.device.domain.DeviceStatus;
import com.zwei.module.iot.device.service.IDeviceStatusService;

/**
 * 设备实时状态Service业务层处理
 * 
 * @author linx
 * @date 2025-09-05
 */
@Service
public class DeviceStatusServiceImpl implements IDeviceStatusService 
{
    @Autowired
    private DeviceStatusMapper deviceStatusMapper;

    /**
     * 查询设备实时状态
     * 
     * @param deviceId 设备实时状态主键
     * @return 设备实时状态
     */
    @Override
    public DeviceStatus selectDeviceStatusByDeviceId(Long deviceId)
    {
        return deviceStatusMapper.selectDeviceStatusByDeviceId(deviceId);
    }

    /**
     * 查询设备实时状态列表
     * 
     * @param deviceStatus 设备实时状态
     * @return 设备实时状态
     */
    @Override
    public List<DeviceStatus> selectDeviceStatusList(DeviceStatus deviceStatus)
    {
        return deviceStatusMapper.selectDeviceStatusList(deviceStatus);
    }

    /**
     * 新增设备实时状态
     * 
     * @param deviceStatus 设备实时状态
     * @return 结果
     */
    @Override
    public int insertDeviceStatus(DeviceStatus deviceStatus)
    {
        return deviceStatusMapper.insertDeviceStatus(deviceStatus);
    }

    /**
     * 修改设备实时状态
     * 
     * @param deviceStatus 设备实时状态
     * @return 结果
     */
    @Override
    public int updateDeviceStatus(DeviceStatus deviceStatus)
    {
        return deviceStatusMapper.updateDeviceStatus(deviceStatus);
    }

    /**
     * 批量删除设备实时状态
     * 
     * @param deviceIds 需要删除的设备实时状态主键
     * @return 结果
     */
    @Override
    public int deleteDeviceStatusByDeviceIds(Long[] deviceIds)
    {
        return deviceStatusMapper.deleteDeviceStatusByDeviceIds(deviceIds);
    }

    /**
     * 删除设备实时状态信息
     * 
     * @param deviceId 设备实时状态主键
     * @return 结果
     */
    @Override
    public int deleteDeviceStatusByDeviceId(Long deviceId)
    {
        return deviceStatusMapper.deleteDeviceStatusByDeviceId(deviceId);
    }
}