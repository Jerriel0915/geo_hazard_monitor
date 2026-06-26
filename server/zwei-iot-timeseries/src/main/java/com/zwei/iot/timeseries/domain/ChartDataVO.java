package com.zwei.iot.timeseries.domain;

import java.io.Serializable;
import java.util.List;

/**
 * 监测数据图表响应对象。
 *
 * <p>封装图表接口的完整返回结构，包含时间标签、数据值与统计信息。
 * 新增 {@code sampled} / {@code downsampleInterval} / {@code pointCount} 字段，
 * 用于标识是否已自动降采样及降采样粒度。</p>
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
        Double avgValue,
        boolean sampled,
        String downsampleInterval,
        long pointCount
) implements Serializable {
    private static final long serialVersionUID = 2L;

    /**
     * 向后兼容构造器 — 不含降采样字段，默认 sampled=false。
     */
    public ChartDataVO(String seriesName, String deviceName, String sensorName,
                       List<String> labels, List<Double> values, String unit, String attrName,
                       Double maxValue, Double minValue, Double avgValue) {
        this(seriesName, deviceName, sensorName, labels, values, unit, attrName,
                maxValue, minValue, avgValue, false, null, values != null ? values.size() : 0L);
    }
}
