package com.zwei.iot.alarm.dispatch.service;

import com.zwei.iot.alarm.dispatch.domain.AlarmDispatchRule;

import java.util.List;

public interface IAlarmRuleMatcher {

    /**
     * 告警事件：匹配 ALARM 类型 + 等级匹配 + 隐患点匹配（含 '*'）
     */
    List<AlarmDispatchRule> matchAlarmRules(Long hazardPointId, String alarmLevel);

    /**
     * 设备离线：匹配 OFFLINE 类型 + 设备匹配（含 '*'）
     */
    List<AlarmDispatchRule> matchOfflineRules(Long deviceId);
}
