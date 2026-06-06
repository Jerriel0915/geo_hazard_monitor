package com.zwei.iot.device.service;

import com.zwei.iot.device.domain.DeviceRegistrationLog;

/**
 * 设备注册日志服务
 */
public interface DeviceRegistrationLogService {
    DeviceRegistrationLog selectByRequestId(String requestId);

    void save(DeviceRegistrationLog log);
}
