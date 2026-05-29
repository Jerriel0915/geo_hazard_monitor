package com.zwei.iot.device.service.impl;

import com.zwei.iot.device.domain.DeviceRegistrationLog;
import com.zwei.iot.device.mapper.DeviceRegistrationLogMapper;
import com.zwei.iot.device.service.DeviceRegistrationLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 设备注册日志服务实现
 */
@Service
public class DeviceRegistrationLogServiceImpl implements DeviceRegistrationLogService {
    private final DeviceRegistrationLogMapper logMapper;

    public DeviceRegistrationLogServiceImpl(DeviceRegistrationLogMapper logMapper) {
        this.logMapper = logMapper;
    }

    @Override
    public DeviceRegistrationLog selectByRequestId(String requestId) {
        return logMapper.selectByRequestId(requestId);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(DeviceRegistrationLog log) {
        logMapper.insert(log);
    }
}
