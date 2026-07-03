package com.zwei.iot.device.service.impl;

import com.zwei.iot.device.domain.Device;
import com.zwei.iot.device.mapper.DeviceMapper;
import com.zwei.iot.device.service.IDeviceAuthQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DeviceAuthQueryServiceImpl implements IDeviceAuthQueryService {
    private final DeviceMapper deviceMapper;

    @Autowired
    public DeviceAuthQueryServiceImpl(DeviceMapper deviceMapper) { this.deviceMapper = deviceMapper; }

    @Override public Device findByAuthUsername(String authUsername) { return deviceMapper.selectDeviceByAuthUsername(authUsername); }

    @Override
    public void updateAuthInfo(Long deviceId, String lastAuthTime, String lastAuthIp) {
        deviceMapper.updateDevice(Device.builder()
                .id(deviceId)
                .lastAuthTime(lastAuthTime)
                .lastAuthIp(lastAuthIp)
                .build());
    }
}
