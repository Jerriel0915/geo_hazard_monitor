package com.zwei.iot.device.service.impl;

import com.zwei.common.exception.ServiceException;
import com.zwei.iot.device.domain.DeviceSensor;
import com.zwei.iot.device.domain.SensorAttribute;
import com.zwei.iot.device.domain.SensorMetadata;
import com.zwei.iot.device.mapper.DeviceSensorMapper;
import com.zwei.iot.device.mapper.SensorAttributeMapper;
import com.zwei.iot.device.service.IDeviceSensorQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Comparator;
import java.util.List;

@Service
public class DeviceSensorQueryServiceImpl implements IDeviceSensorQueryService {
    private final DeviceSensorMapper sensorMapper;
    private final SensorAttributeMapper attributeMapper;

    @Autowired
    public DeviceSensorQueryServiceImpl(DeviceSensorMapper sensorMapper, SensorAttributeMapper attributeMapper) {
        this.sensorMapper = sensorMapper; this.attributeMapper = attributeMapper;
    }

    @Override
    public SensorMetadata requireSensorMetadata(Long deviceId, String sensorNo) {
        DeviceSensor condition = DeviceSensor.builder().deviceId(deviceId).sensorNo(sensorNo).status(1).build();
        List<DeviceSensor> sensors = sensorMapper.selectSensorList(condition);
        if (sensors == null || sensors.isEmpty()) throw new ServiceException("传感器不存在或未启用: " + sensorNo);
        DeviceSensor sensor = sensors.get(0);
        List<SensorAttribute> attributes = attributeMapper.selectAttributeListBySensorId(sensor.getId());
        attributes.sort(Comparator.comparing(SensorAttribute::getId));
        return SensorMetadata.builder().deviceId(deviceId).sensorId(sensor.getId()).attributes(attributes).build();
    }
}
