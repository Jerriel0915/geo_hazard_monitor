package com.zwei.iot.device.service;

import com.zwei.iot.device.domain.Device;

public interface IDeviceAuthQueryService {
    Device findByAuthUsername(String authUsername);
    void updateDevice(Device device);
}
