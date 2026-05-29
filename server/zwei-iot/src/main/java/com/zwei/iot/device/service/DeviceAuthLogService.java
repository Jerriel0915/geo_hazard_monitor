package com.zwei.iot.device.service;

import com.zwei.iot.device.domain.DeviceAuthLog;

/**
 * 设备认证日志服务
 */
public interface DeviceAuthLogService {
    void save(DeviceAuthLog log);
}
