package com.zwei.iot.timeseries.support;

import com.zwei.iot.timeseries.config.IotdbProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * IoTDB 时序路径解析器。
 *
 * <h3>路径模型</h3>
 * <pre>
 * 数据库:    root.{database}
 * 传感器:    root.{database}.d{deviceId}.s{sensorNo}
 * 指标列:    root.{database}.d{deviceId}.s{sensorNo}.{attrCode}
 * </pre>
 *
 * <p>示例：设备 101 传感器 "1" 的雨量指标 → {@code root.geo_hazard.d101.s1.rainfall_hour}
 *
 * <p>所有 IoTDB DDL/DML 均通过此解析器生成路径，确保全链路路径格式一致。
 */
@Component
public class IotdbPathResolver {
    private final IotdbProperties properties;

    /**
     * 构造 IoTDB 路径解析器。
     *
     * @param properties IoTDB 配置
     */
    @Autowired
    public IotdbPathResolver(IotdbProperties properties) {
        this.properties = properties;
    }

    /**
     * 生成传感器级路径。
     *
     * @param deviceId 设备ID
     * @param sensorNo 传感器编号
     * @return 传感器路径
     */
    public String buildSensorPath(Long deviceId, String sensorNo) {
        return properties.getDatabase() + ".d" + deviceId + ".s" + sensorNo;
    }

    /**
     * 生成指标级路径。
     *
     * @param deviceId 设备ID
     * @param sensorNo 传感器编号
     * @param attrCode 指标编码
     * @return 指标路径
     */
    public String buildMeasurementPath(Long deviceId, String sensorNo, String attrCode) {
        return buildSensorPath(deviceId, sensorNo) + "." + attrCode;
    }
}
