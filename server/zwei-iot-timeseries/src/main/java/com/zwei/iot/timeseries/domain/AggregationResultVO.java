package com.zwei.iot.timeseries.domain;

import java.io.Serializable;
import java.util.Map;

/**
 * 单 (deviceId, sensorCode, attrCode) 维度的聚合结果。
 *
 * <p>时间戳 {@code time} 对应 GROUP BY 后的分组时间(RAW 时为 0)。
 * {@code metrics} 是表达式别名 → 数值的字典,如 {@code {"AVG": 12.5, "DELTA": 0.7}}。</p>
 */
public record AggregationResultVO(
        Long deviceId,
        String sensorCode,
        String attrCode,
        String attrName,
        String unit,
        long time,
        Map<String, Double> metrics
) implements Serializable {
    private static final long serialVersionUID = 1L;
}
