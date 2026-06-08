package com.zwei.iot.device.mapper;

import com.zwei.iot.device.domain.DeviceOnlineEventLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DeviceOnlineEventLogMapper {
    List<DeviceOnlineEventLog> selectByDeviceId(@Param("deviceId") Long deviceId, @Param("limit") int limit);
}
