package com.zwei.iot.alarm.channel;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

/**
 * 通知模板变量上下文
 *
 * @author zwei
 */
@Data
@Builder
public class NotifyContext {
    /** threshold / comprehensive / offline */
    private String sourceType;
    /** alarm_record.id 或 device.id */
    private Long sourceId;
    /** 告警时填（来自 AlarmRecord 反规范化） */
    private String hazardPointName;
    /** 告警/离线都可能填 */
    private String deviceName;
    /** 离线时填（暂留空，未来扩展） */
    private String deviceCode;
    /** 告警时填（字典 label） */
    private String alarmLevel;
    /** 告警标题（来自 AlarmNotification.title） */
    private String alarmTitle;
    /** alarm_time / offline 通知时刻 */
    private Date eventTime;
    /** 暂留空（未来扩展） */
    private Date lastReportTime;
}
