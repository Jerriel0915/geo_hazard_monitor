package com.zwei.common.event;

/**
 * 告警触发事件 — 供通知模块、日志模块监听。
 * <p>
 * 在报警引擎成功创建或更新告警记录后发布，
 * 供 AlarmStreamPublisher、AlarmNotifier 等组件消费。
 *
 * @author zwei
 */
public class AlarmTriggeredEvent {

    private final Long alarmId;
    private final Long hazardPointId;
    private final Integer alarmLevel;
    private final String alarmType;
    private final String alarmMessage;
    /** 触发原因: 首次告警 / 超过静默期 / 等级变化 */
    private final String triggerReason;

    public AlarmTriggeredEvent(Long alarmId, Long hazardPointId, Integer alarmLevel,
                               String alarmType, String alarmMessage, String triggerReason) {
        this.alarmId = alarmId;
        this.hazardPointId = hazardPointId;
        this.alarmLevel = alarmLevel;
        this.alarmType = alarmType;
        this.alarmMessage = alarmMessage;
        this.triggerReason = triggerReason;
    }

    public Long getAlarmId() {
        return alarmId;
    }

    public Long getHazardPointId() {
        return hazardPointId;
    }

    public Integer getAlarmLevel() {
        return alarmLevel;
    }

    public String getAlarmType() {
        return alarmType;
    }

    public String getAlarmMessage() {
        return alarmMessage;
    }

    public String getTriggerReason() {
        return triggerReason;
    }
}
