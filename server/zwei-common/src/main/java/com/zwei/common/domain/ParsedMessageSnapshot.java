package com.zwei.common.domain;

import java.util.Map;

/**
 * ParsedMessage 的精简快照, 用于 Redis 缓存上一条消息 (prevData)。
 *
 * <p>由 {@code zwei-iot-timeseries.LastMessageStore} 维护,
 * 被 {@code AlarmEvaluationEngine} 用作 prev 维度数据源。
 *
 * @param deviceCode 设备编码
 * @param sensorCode 传感器编码
 * @param dataTime   数据采集时间 (epoch ms)
 * @param properties 属性值映射 (attrCode -> value), 含固有属性 + 计算属性结果
 */
public record ParsedMessageSnapshot(
        String deviceCode,
        String sensorCode,
        long dataTime,
        Map<String, Object> properties
) {
}
