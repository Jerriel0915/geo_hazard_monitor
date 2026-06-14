package com.zwei.iot.timeseries.service;

import com.zwei.common.exception.ServiceException;
import com.zwei.iot.device.domain.DeviceSensor;
import com.zwei.iot.device.domain.SensorAttribute;
import com.zwei.iot.device.service.IDeviceSensorService;
import com.zwei.iot.timeseries.domain.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 监测数据聚合查询服务 — 支持白名单函数 + 表达式组合 + 数值范围 + 时间窗口。
 *
 * <p>串行遍历 sensor 下所有 attrCode 调 IotdbTimeSeriesService,
 * 合并为 {@link SensorAggregationVO} 返回。</p>
 */
@Service
public class MonitorDataAggregationService {

    private final IotdbTimeSeriesService iotdbService;
    private final IDeviceSensorService deviceSensorService;

    @Autowired
    public MonitorDataAggregationService(IotdbTimeSeriesService iotdbService,
                                         IDeviceSensorService deviceSensorService) {
        this.iotdbService = iotdbService;
        this.deviceSensorService = deviceSensorService;
    }

    /**
     * 批量聚合 — 传感器下所有 attrCode 各算一次,合并返回。
     *
     * @throws ServiceException 传感器不存在或 attrList 为空
     */
    public SensorAggregationVO aggregateAllAttrs(
            Long deviceId, String sensorCode,
            TimeWindowSpec window,
            List<ExpressionSpec> expressions,
            Double minValue, Double maxValue) {
        DeviceSensor sensor = deviceSensorService.selectSensorByDeviceIdAndCode(deviceId, sensorCode);
        if (sensor == null) {
            throw new ServiceException("传感器不存在: deviceId=" + deviceId + ", sensorCode=" + sensorCode);
        }
        List<SensorAttribute> attrs = sensor.getAttrList();
        if (attrs == null || attrs.isEmpty()) {
            throw new ServiceException("该传感器无监测指标: sensorCode=" + sensorCode);
        }
        List<AggregationResultVO> results = new ArrayList<>();
        for (SensorAttribute attribute : attrs) {
            List<AggregationResultVO> attrResults = iotdbService.queryAggregate(
                    deviceId, sensorCode, attribute.getAttrCode(),
                    window, expressions, minValue, maxValue);
            for (AggregationResultVO r : attrResults) {
                results.add(new AggregationResultVO(
                        r.deviceId(), r.sensorCode(), r.attrCode(),
                        attribute.getAttrName(), attribute.getUnit(),
                        r.time(), r.metrics()));
            }
        }
        return new SensorAggregationVO(deviceId, sensorCode, sensor.getSensorName(), results);
    }

    /**
     * 单指标聚合 — 直接代理 iotdbService。
     */
    public List<AggregationResultVO> aggregate(
            Long deviceId, String sensorCode, String attrCode,
            TimeWindowSpec window,
            List<ExpressionSpec> expressions,
            Double minValue, Double maxValue) {
        return iotdbService.queryAggregate(
                deviceId, sensorCode, attrCode, window, expressions, minValue, maxValue);
    }

    /**
     * delta 便捷方法 — 等价于 aggregateAllAttrs 传 LAST_VALUE - FIRST_VALUE。
     */
    public SensorAggregationVO delta(Long deviceId, String sensorCode, TimeWindowSpec window) {
        ExpressionSpec deltaExpr = new ExpressionSpec.BinaryOp(
                new ExpressionSpec.FunctionCall(AggregationFunction.LAST_VALUE),
                ExpressionSpec.BinaryOperator.SUB,
                new ExpressionSpec.FunctionCall(AggregationFunction.FIRST_VALUE));
        return aggregateAllAttrs(deviceId, sensorCode, window, List.of(deltaExpr), null, null);
    }

    /**
     * 传感器下所有 attrCode 的最新值(可按 attrCode 过滤)。
     *
     * @param deviceId   设备ID
     * @param sensorCode 传感器编号
     * @param attrCode   可选,只返回该 attrCode 的最新值
     * @return {@code Map<attrCode, IotdbQueryRow>},attrCode 不存在时 value 为 null
     */
    public Map<String, IotdbQueryRow> latestBySensor(
            Long deviceId, String sensorCode, String attrCode) {
        DeviceSensor sensor = deviceSensorService.selectSensorByDeviceIdAndCode(deviceId, sensorCode);
        if (sensor == null) {
            throw new ServiceException("传感器不存在: deviceId=" + deviceId + ", sensorCode=" + sensorCode);
        }
        List<SensorAttribute> attrs = sensor.getAttrList();
        if (attrs == null || attrs.isEmpty()) {
            return Map.of();
        }
        Map<String, IotdbQueryRow> result = new java.util.LinkedHashMap<>();
        for (SensorAttribute a : attrs) {
            if (attrCode != null && !attrCode.isBlank() && !attrCode.equals(a.getAttrCode())) {
                continue;
            }
            IotdbQueryRow row = iotdbService.queryLatest(deviceId, sensorCode, a.getAttrCode());
            result.put(a.getAttrCode(), row);
        }
        return result;
    }
}
