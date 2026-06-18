package com.zwei.iot.device.domain.brief;

import java.util.Date;

/**
 * 单条告警事件 (供 report 展示 Top N)。
 */
public record AlarmEvent(
    Long id,
    Date firstTriggerTime,
    Date lastTriggerTime,
    int alarmLevel,
    String alarmLevelText,
    String alarmType,
    String deviceName,
    String hazardPointName,
    String alarmMessage,
    int status,
    String statusName,
    int triggerCount
) {}
