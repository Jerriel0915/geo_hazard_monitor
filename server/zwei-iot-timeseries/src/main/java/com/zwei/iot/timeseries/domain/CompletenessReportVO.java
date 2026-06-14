package com.zwei.iot.timeseries.domain;

import java.io.Serializable;

/**
 * 完整度报告 — 在指定时间窗口内,传感器"应该上报 N 次,实际只上报 M 次"的统计。
 *
 * <p>{@code expectedPoints} = 时长 / 期望采样间隔
 * <br>{@code completenessRate} = actualPoints / expectedPoints(0-1)
 * <br>{@code missingRate} = 1 - completenessRate</p>
 */
public record CompletenessReportVO(
        Long deviceId,
        String sensorCode,
        String attrCode,
        long expectedPoints,
        long actualPoints,
        double completenessRate,
        double missingRate,
        Long lastReportAt
) implements Serializable {
    private static final long serialVersionUID = 1L;
}
