package com.zwei.iot.device.domain.brief;

import java.util.Date;

/**
 * 设备摘要 (供 report 渲染表格)。
 */
public record DeviceBrief(
    Long id,
    String code,
    String name,
    Integer deviceType,
    Integer sensorCount,
    Integer onlineStatus,    // 0=离线 1=在线 null=未注册
    Date lastReportAt
) {}
