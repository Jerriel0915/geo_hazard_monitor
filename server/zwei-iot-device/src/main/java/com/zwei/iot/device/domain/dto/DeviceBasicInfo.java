package com.zwei.iot.device.domain.dto;

/**
 * 设备基础信息精简 DTO, 供告警引擎解析 device 维度 subject。
 *
 * <p>由 {@code DeviceQueryServiceImpl.getBasicInfoById} 装配, 字段来源:
 * <ul>
 *   <li>{@code online} — {@code device_online_status.status}</li>
 *   <li>{@code lastReportAt} — {@code device.last_report_time} (epoch seconds)</li>
 *   <li>{@code status} — {@code device.status} (1-正常 2-维修 3-停用)</li>
 * </ul>
 */
public record DeviceBasicInfo(
        boolean online,
        long lastReportAt,
        int status
) {
}
