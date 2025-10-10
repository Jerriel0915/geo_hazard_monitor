package com.zwei.module.iot.device.service;

import java.util.List;
import com.zwei.module.iot.device.domain.DeviceStatus;

/**
 * 设备实时状态Service接口
 * 
 * @author linx
 * @date 2025-09-05
 */
public interface IDeviceStatusService 
{
    /**
     * 查询设备实时状态
     * 
     * @param deviceId 设备实时状态主键
     * @return 设备实时状态
     */
    public DeviceStatus selectDeviceStatusByDeviceId(Long deviceId);

    /**
     * 查询设备实时状态列表
     * 
     * @param deviceStatus 设备实时状态
     * @return 设备实时状态集合
     */
    public List<DeviceStatus> selectDeviceStatusList(DeviceStatus deviceStatus);

    /**
     * 新增设备实时状态
     * 
     * @param deviceStatus 设备实时状态
     * @return 结果
     */
    public int insertDeviceStatus(DeviceStatus deviceStatus);

    /**
     * 修改设备实时状态
     * 
     * @param deviceStatus 设备实时状态
     * @return 结果
     */
    public int updateDeviceStatus(DeviceStatus deviceStatus);

    /**
     * 批量删除设备实时状态
     * 
     * @param deviceIds 需要删除的设备实时状态主键集合
     * @return 结果
     */
    public int deleteDeviceStatusByDeviceIds(Long[] deviceIds);

    /**
     * 删除设备实时状态信息
     * 
     * @param deviceId 设备实时状态主键
     * @return 结果
     */
    public int deleteDeviceStatusByDeviceId(Long deviceId);
}