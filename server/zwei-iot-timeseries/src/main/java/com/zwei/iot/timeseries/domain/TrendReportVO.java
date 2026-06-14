package com.zwei.iot.timeseries.domain;

import java.io.Serializable;

/**
 * 趋势 / 变化率报告 — 端点斜率近似((LAST_VALUE - FIRST_VALUE) / 时长)。
 *
 * <p>注意:本类使用的是"端点斜率"近似,不是严格最小二乘回归。
 * 在数据单调、噪声小的场景下近似度高;噪声大时偏差较大。</p>
 *
 * @param slopePerMs   每毫秒的变化量(原始斜率)
 * @param ratePerHour  每小时的变化量(slopePerMs × 3,600,000)
 * @param ratePerDay   每天的变化量(slopePerMs × 86,400,000)
 * @param trendDirection "rising" / "falling" / "stable" / "unknown"
 */
public record TrendReportVO(
        Long deviceId,
        String sensorCode,
        String attrCode,
        long startTime,
        long endTime,
        Double slopePerMs,
        Double ratePerHour,
        Double ratePerDay,
        Double firstValue,
        Double lastValue,
        String trendDirection
) implements Serializable {
    private static final long serialVersionUID = 1L;
}
