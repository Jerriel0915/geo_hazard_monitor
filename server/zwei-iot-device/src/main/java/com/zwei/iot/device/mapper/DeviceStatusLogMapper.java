package com.zwei.iot.device.mapper;

import com.zwei.iot.device.domain.DeviceStatusLog;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DeviceStatusLogMapper {
    int insert(DeviceStatusLog log);

    List<DeviceStatusLog> selectByDeviceId(Long deviceId);
}
