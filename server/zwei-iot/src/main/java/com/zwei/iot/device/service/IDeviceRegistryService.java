package com.zwei.iot.device.service;

import com.zwei.iot.device.domain.dto.DeviceRegisterRequest;
import com.zwei.iot.device.domain.vo.DeviceRegistryResult;

/**
 * 设备注册中心服务
 */
public interface IDeviceRegistryService {
    DeviceRegistryResult register(DeviceRegisterRequest request);
}
