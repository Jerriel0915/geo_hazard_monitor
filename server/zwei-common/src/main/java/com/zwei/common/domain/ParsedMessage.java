package com.zwei.common.domain;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 解析后的标准化报文 — parser 模块的对外统一产出格式。
 *
 * <p>作为 Redis Stream 中传输的流契约（JSON 序列化），
 * 由 zwei-iot-parser 写入，zwei-iot-timeseries 消费。
 *
 * @param deviceCode  设备编码 (device.code)
 * @param sensorCode  传感器编码 (sensor.sensorCode)
 * @param sourceType  源协议标识（"sys" / "gb" / 自定义）
 * @param dataTime    数据采集时间 epoch 毫秒
 * @param receiveTime 服务端接收时间 epoch 毫秒
 * @param payloadHash 原始报文 SHA-256（用于幂等去重）
 * @param properties  解析出的属性值列表
 */
public record ParsedMessage(
        String deviceCode,
        String sensorCode,
        String sourceType,
        long dataTime,
        long receiveTime,
        String payloadHash,
        List<PropertyValue> properties
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
