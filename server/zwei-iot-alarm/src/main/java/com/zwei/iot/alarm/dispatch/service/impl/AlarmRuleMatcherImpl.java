package com.zwei.iot.alarm.dispatch.service.impl;

import com.zwei.iot.alarm.dispatch.domain.AlarmDispatchRule;
import com.zwei.iot.alarm.dispatch.mapper.AlarmDispatchRuleMapper;
import com.zwei.iot.alarm.dispatch.service.IAlarmRuleMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AlarmRuleMatcherImpl implements IAlarmRuleMatcher {

    @Autowired
    private AlarmDispatchRuleMapper ruleMapper;

    @Override
    public List<AlarmDispatchRule> matchAlarmRules(Long hazardPointId, String alarmLevel, String eventType) {
        return ruleMapper.matchAlarmRules(
            hazardPointId == null ? null : String.valueOf(hazardPointId),
            alarmLevel,
            eventType);
    }

    @Override
    public List<AlarmDispatchRule> matchOfflineRules(Long deviceId) {
        return ruleMapper.matchOfflineRules(
            deviceId == null ? null : String.valueOf(deviceId));
    }
}
