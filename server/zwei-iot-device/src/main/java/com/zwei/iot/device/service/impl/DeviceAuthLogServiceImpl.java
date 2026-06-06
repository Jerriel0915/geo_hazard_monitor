package com.zwei.iot.device.service.impl;

import com.zwei.iot.device.domain.DeviceAuthLog;
import com.zwei.iot.device.mapper.DeviceAuthLogMapper;
import com.zwei.iot.device.service.DeviceAuthLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 设备认证日志服务实现
 */
@Service
public class DeviceAuthLogServiceImpl implements DeviceAuthLogService {
    private final DeviceAuthLogMapper deviceAuthLogMapper;

    public DeviceAuthLogServiceImpl(DeviceAuthLogMapper deviceAuthLogMapper) {
        this.deviceAuthLogMapper = deviceAuthLogMapper;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(DeviceAuthLog log) {
        deviceAuthLogMapper.insert(log);
    }
}
