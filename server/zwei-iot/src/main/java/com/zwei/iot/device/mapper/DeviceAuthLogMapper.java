package com.zwei.iot.device.mapper;

import com.zwei.iot.device.domain.DeviceAuthLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 设备认证日志 Mapper
 */
@Mapper
public interface DeviceAuthLogMapper {
    int insert(DeviceAuthLog log);
}
