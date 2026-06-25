package com.zwei.iot.device.service.impl;

import com.zwei.iot.device.domain.DeviceOnlineEventLog;
import com.zwei.iot.device.mapper.DeviceOnlineEventLogMapper;
import com.zwei.iot.device.service.IDeviceOnlineEventLogService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 设备上下线事件日志查询服务实现。
 */
@Service
public class DeviceOnlineEventLogServiceImpl implements IDeviceOnlineEventLogService {

    private final DeviceOnlineEventLogMapper mapper;

    public DeviceOnlineEventLogServiceImpl(DeviceOnlineEventLogMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<DeviceOnlineEventLog> selectByDeviceId(Long deviceId, int limit) {
        return mapper.selectByDeviceId(deviceId, limit);
    }
}
