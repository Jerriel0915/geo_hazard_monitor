package com.zwei.module.iot.device.service.impl;

import com.zwei.module.iot.device.domain.DeviceStatus;
import com.zwei.module.iot.device.mapper.DeviceStatusMapper;
import com.zwei.module.iot.device.service.IDeviceStatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 设备实时状态Service业务层处理
 *
 * @author linx
 * @date 2025-09-05
 */
@Service
public class DeviceStatusServiceImpl implements IDeviceStatusService {
    private final DeviceStatusMapper deviceStatusMapper;

    @Autowired
    DeviceStatusServiceImpl(DeviceStatusMapper deviceStatusMapper) {
        this.deviceStatusMapper = deviceStatusMapper;
    }

    /**
     * 查询设备实时状态
     *
     * @param deviceId 设备实时状态主键
     * @return 设备实时状态
     */
    @Override
    public DeviceStatus selectDeviceStatusByDeviceId(String deviceId) {
        return deviceStatusMapper.selectDeviceStatusByDeviceId(deviceId);
    }

    /**
     * 查询设备实时状态列表
     *
     * @param deviceStatus 设备实时状态
     * @return 设备实时状态
     */
    @Override
    public List<DeviceStatus> selectDeviceStatusList(DeviceStatus deviceStatus) {
        return deviceStatusMapper.selectDeviceStatusList(deviceStatus);
    }

    /**
     * 新增设备实时状态
     *
     * @param deviceStatus 设备实时状态
     * @return 结果
     */
    @Override
    @Transactional
    public int insertDeviceStatus(DeviceStatus deviceStatus) {
        return deviceStatusMapper.insertDeviceStatus(deviceStatus);
    }

    /**
     * 修改设备实时状态
     *
     * @param deviceStatus 设备实时状态
     * @return 结果
     */
    @Override
    @Transactional
    public int updateDeviceStatus(DeviceStatus deviceStatus) {
        return deviceStatusMapper.updateDeviceStatus(deviceStatus);
    }

    /**
     * 批量删除设备实时状态
     *
     * @param deviceIds 需要删除的设备实时状态主键
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteDeviceStatusByDeviceIds(Long[] deviceIds) {
        return deviceStatusMapper.deleteDeviceStatusByDeviceIds(deviceIds);
    }

    /**
     * 删除设备实时状态信息
     *
     * @param deviceId 设备实时状态主键
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteDeviceStatusByDeviceId(Long deviceId) {
        return deviceStatusMapper.deleteDeviceStatusByDeviceId(deviceId);
    }
}