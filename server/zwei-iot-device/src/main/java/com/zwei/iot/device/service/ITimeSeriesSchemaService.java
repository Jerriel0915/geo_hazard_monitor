package com.zwei.iot.device.service;

import java.util.List;

public interface ITimeSeriesSchemaService {
    void createSensorSchema(Long deviceId, String sensorCode, List<String> attrCodes);
}
