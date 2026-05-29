package com.zwei.iot.device.domain.vo;

import com.zwei.iot.device.domain.Device;

/**
 * 设备注册结果
 */
public record DeviceRegistryResult(Device device, boolean created) {
}
