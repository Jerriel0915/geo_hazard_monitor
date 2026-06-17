package com.zwei.iot.report.datasource;

/**
 * 单条指标汇总行 (供渲染器拼表格)。
 *
 * @param deviceId         设备ID
 * @param sensorCode       传感器编号
 * @param attrCode         属性编码
 * @param attrName         属性名称
 * @param unit             单位
 * @param latest           最新值
 * @param maxValue         周期内最大
 * @param minValue         周期内最小
 * @param avgValue         周期内平均
 * @param sumValue         周期内累计变化量 (sum)
 * @param completenessPct  完整率 (0-100)
 */
public record MetricRow(
        Long deviceId,
        String sensorCode,
        String attrCode,
        String attrName,
        String unit,
        Double latest,
        Double maxValue,
        Double minValue,
        Double avgValue,
        Double sumValue,
        Double completenessPct
) {}
