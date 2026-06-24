package com.zwei.iot.timeseries.support;

import com.zwei.common.exception.ServiceException;
import com.zwei.iot.timeseries.config.IotdbProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * IoTDB 时序路径解析器。
 *
 * <h3>路径模型</h3>
 * <pre>
 * 数据库:    root.{database}
 * 传感器:    root.{database}.d{deviceId}.s{sensorCode}
 * 指标列:    root.{database}.d{deviceId}.s{sensorCode}.{attrCode}
 * </pre>
 *
 * <p>示例：设备 101 传感器 "1" 的雨量指标 → {@code root.geo_hazard.d101.s1.rainfall_hour}
 *
 * <p>所有 IoTDB DDL/DML 均通过此解析器生成路径，确保全链路路径格式一致。
 *
 * <p>标识符会经过白名单校验，防止 SQL 注入。
 */
@Component
public class IotdbPathResolver {

    /** IoTDB 路径标识符白名单：仅允许字母、数字、下划线，长度 1~64 */
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[A-Za-z0-9_]{1,64}$");

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
     * @param sensorCode 传感器编码
     * @return 传感器路径
     */
    public String buildSensorPath(Long deviceId, String sensorCode) {
        validateIdentifier(sensorCode, "sensorCode");
        return properties.getDatabase() + ".d" + deviceId + ".s" + sensorCode;
    }

    /**
     * 生成指标级路径。
     *
     * @param deviceId 设备ID
     * @param sensorCode 传感器编码
     * @param attrCode 指标编码
     * @return 指标路径
     */
    public String buildMeasurementPath(Long deviceId, String sensorCode, String attrCode) {
        validateIdentifier(sensorCode, "sensorCode");
        validateIdentifier(attrCode, "attrCode");
        return buildSensorPath(deviceId, sensorCode) + "." + attrCode;
    }

    /**
     * 校验标识符是否符合 IoTDB 路径白名单，防止 SQL 注入。
     *
     * @param value 标识符值
     * @param label 字段名（用于错误消息）
     * @throws ServiceException 当标识符不合法时抛出
     */
    static void validateIdentifier(String value, String label) {
        if (value == null || !IDENTIFIER_PATTERN.matcher(value).matches()) {
            throw new ServiceException("非法的IoTDB路径标识符: " + label + "="
                    + (value != null ? value.substring(0, Math.min(value.length(), 200)) : "null"));
        }
    }
}
