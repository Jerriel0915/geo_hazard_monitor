package com.zwei.iot.alarm.service;

import com.zwei.iot.alarm.domain.AlarmDispatchRule;

import java.util.List;

/**
 * 告警分发规则服务接口
 *
 * @author zwei
 */
public interface IAlarmDispatchService {

    List<AlarmDispatchRule> selectList(AlarmDispatchRule rule);

    AlarmDispatchRule selectById(Long id);

    int insert(AlarmDispatchRule rule);

    int update(AlarmDispatchRule rule);

    int delete(Long id);

    /**
     * 获取所有启用的分发规则
     */
    List<AlarmDispatchRule> selectEnabledRules();
}
