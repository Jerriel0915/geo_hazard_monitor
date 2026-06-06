package com.zwei.iot.timeseries.service;

import com.zwei.iot.device.domain.SensorMetadata;
import com.zwei.iot.device.service.IDeviceSensorQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MonitorMetadataService {
    private final IDeviceSensorQueryService deviceSensorQueryService;

    @Autowired
    public MonitorMetadataService(IDeviceSensorQueryService deviceSensorQueryService) {
        this.deviceSensorQueryService = deviceSensorQueryService;
    }

    public SensorMetadata requireSensorMetadata(Long deviceId, String sensorNo) {
        return deviceSensorQueryService.requireSensorMetadata(deviceId, sensorNo);
    }
}
