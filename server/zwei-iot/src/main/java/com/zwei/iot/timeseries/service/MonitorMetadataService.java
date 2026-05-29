package com.zwei.iot.timeseries.service;

import com.zwei.common.exception.ServiceException;
import com.zwei.iot.device.domain.DeviceSensor;
import com.zwei.iot.device.domain.SensorAttribute;
import com.zwei.iot.device.mapper.DeviceSensorMapper;
import com.zwei.iot.device.mapper.SensorAttributeMapper;
import com.zwei.iot.timeseries.domain.SensorMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * 监测点元数据解析。
 *
 * <p>传感器与属性元数据聚合，在报文解析前补齐时序写入所需上下文。
 * 设备存在性由 CONNECT 鉴权阶段保障，此处不再重复校验。</p>
 */
@Service
public class MonitorMetadataService {
    private final DeviceSensorMapper deviceSensorMapper;
    private final SensorAttributeMapper sensorAttributeMapper;

    @Autowired
    public MonitorMetadataService(DeviceSensorMapper deviceSensorMapper,
                                  SensorAttributeMapper sensorAttributeMapper) {
        this.deviceSensorMapper = deviceSensorMapper;
        this.sensorAttributeMapper = sensorAttributeMapper;
    }

    /**
     * 根据设备ID与传感器编号解析元数据。
     *
     * @param deviceId 设备ID
     * @param sensorNo 传感器编号
     * @return 传感器元数据
     * @throws ServiceException 当传感器不存在或未启用时抛出
     */
    public SensorMetadata requireSensorMetadata(Long deviceId, String sensorNo) {
        DeviceSensor condition = DeviceSensor.builder()
                .deviceId(deviceId)
                .sensorNo(sensorNo)
                .status(1)
                .build();
        List<DeviceSensor> sensors = deviceSensorMapper.selectSensorList(condition);
        if (sensors == null || sensors.isEmpty()) {
            throw new ServiceException("传感器不存在或未启用: " + sensorNo);
        }
        DeviceSensor sensor = sensors.get(0);
        List<SensorAttribute> attributes = sensorAttributeMapper.selectAttributeListBySensorId(sensor.getId());
        attributes.sort(Comparator.comparing(SensorAttribute::getId));
        return SensorMetadata.builder()
                .deviceId(deviceId)
                .sensorId(sensor.getId())
                .attributes(attributes)
                .build();
    }
}
