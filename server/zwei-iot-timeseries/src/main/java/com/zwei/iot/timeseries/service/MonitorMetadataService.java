package com.zwei.iot.timeseries.service;

import com.zwei.iot.device.domain.SensorMetadata;
import com.zwei.iot.device.service.IDeviceSensorQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 监测元数据解析服务。
 *
 * <p>根据 MQTT 主题中的 deviceId + sensorNo 解析传感器元数据（含属性列表），
 * 供报文解析器使用以将原始数据映射到正确的 attrCode。
 *
 * <p>通过 {@link IDeviceSensorQueryService#requireSensorMetadata} 跨模块查询，
 * 传感器不存在或未启用时直接抛 {@link com.zwei.common.exception.ServiceException}。
 */
@Service
public class MonitorMetadataService {
    private final IDeviceSensorQueryService deviceSensorQueryService;

    @Autowired
    public MonitorMetadataService(IDeviceSensorQueryService deviceSensorQueryService) {
        this.deviceSensorQueryService = deviceSensorQueryService;
    }

    /**
     * 获取传感器元数据（含属性列表）。
     *
     * @param deviceId 设备主键（由 MQTT 鉴权会话提供，已受信）
     * @param sensorNo MQTT 主题中的传感器编号
     * @return 传感器元数据（永不返回 null）
     * @throws com.zwei.common.exception.ServiceException 传感器不存在或未启用
     */
    public SensorMetadata requireSensorMetadata(Long deviceId, String sensorNo) {
        return deviceSensorQueryService.requireSensorMetadata(deviceId, sensorNo);
    }
}
