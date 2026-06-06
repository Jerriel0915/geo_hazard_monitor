package com.zwei.iot.timeseries.support;

import com.zwei.iot.timeseries.config.IotdbProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * IoTDB 树模型路径解析。
 * <p>根据设备ID、传感器编号和指标编码动态生成 IoTDB 路径，确保数据按照预定义树模型结构写入。</p>
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
