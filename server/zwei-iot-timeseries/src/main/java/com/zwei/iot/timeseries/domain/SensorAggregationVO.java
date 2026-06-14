package com.zwei.iot.timeseries.domain;

import java.io.Serializable;
import java.util.List;

/**
 * sensorCode 维度的批量聚合响应 — 包含该 sensor 下所有 attrCode 的聚合结果。
 */
public record SensorAggregationVO(
        Long deviceId,
        String sensorCode,
        String sensorName,
        List<AggregationResultVO> results
) implements Serializable {
    private static final long serialVersionUID = 1L;
}
