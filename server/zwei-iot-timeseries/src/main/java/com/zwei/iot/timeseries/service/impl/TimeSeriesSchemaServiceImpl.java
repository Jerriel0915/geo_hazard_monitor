package com.zwei.iot.timeseries.service.impl;

import com.zwei.iot.device.service.ITimeSeriesSchemaService;
import com.zwei.iot.timeseries.service.IotdbTimeSeriesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class TimeSeriesSchemaServiceImpl implements ITimeSeriesSchemaService {
    private final IotdbTimeSeriesService iotdbTimeSeriesService;

    @Autowired
    public TimeSeriesSchemaServiceImpl(IotdbTimeSeriesService iotdbTimeSeriesService) { this.iotdbTimeSeriesService = iotdbTimeSeriesService; }

    @Override
    public void createSensorSchema(Long deviceId, String sensorCode, List<String> attrCodes) {
        iotdbTimeSeriesService.createSensorSchema(deviceId, sensorCode, attrCodes);
    }
}
