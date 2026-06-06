package com.zwei.iot.device.mapper;

import com.zwei.iot.device.domain.DeviceRegistrationLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 设备注册日志 Mapper
 */
@Mapper
public interface DeviceRegistrationLogMapper {
    DeviceRegistrationLog selectByRequestId(String requestId);

    int insert(DeviceRegistrationLog log);
}
