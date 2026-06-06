package com.zwei.iot.timeseries.domain;

import java.io.Serializable;
import java.util.List;

/**
 * 监测数据图表响应对象。
 *
 * <p>封装图表接口的完整返回结构，包含时间标签、数据值与统计信息。</p>
 */
public record ChartDataVO(
        String seriesName,
        String deviceName,
        String sensorName,
        List<String> labels,
        List<Double> values,
        String unit,
        String attrName,
        Double maxValue,
        Double minValue,
        Double avgValue
) implements Serializable {
    private static final long serialVersionUID = 1L;
}
