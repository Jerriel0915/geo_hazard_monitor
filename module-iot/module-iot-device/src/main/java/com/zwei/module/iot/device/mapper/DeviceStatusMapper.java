package com.zwei.module.iot.device.mapper;

import java.util.List;
import com.zwei.module.iot.device.domain.DeviceStatus;

/**
 * 设备实时状态Mapper接口
 * 
 * @author linx
 * @date 2025-09-05
 */
public interface DeviceStatusMapper 
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
     * 删除设备实时状态
     * 
     * @param deviceId 设备实时状态主键
     * @return 结果
     */
    public int deleteDeviceStatusByDeviceId(Long deviceId);

    /**
     * 批量删除设备实时状态
     * 
     * @param deviceIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteDeviceStatusByDeviceIds(Long[] deviceIds);
}