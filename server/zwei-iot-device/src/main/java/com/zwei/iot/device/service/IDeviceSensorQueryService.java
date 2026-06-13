package com.zwei.iot.device.service;

import com.zwei.iot.device.domain.SensorMetadata;

public interface IDeviceSensorQueryService {
    SensorMetadata requireSensorMetadata(Long deviceId, String sensorCode);
}
