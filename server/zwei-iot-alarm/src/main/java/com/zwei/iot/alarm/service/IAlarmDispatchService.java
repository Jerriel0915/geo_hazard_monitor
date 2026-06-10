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

    /**
     * 校验分发规则在指定隐患点下 name 唯一
     *
     * @param name          规则名称
     * @param hazardPointId 隐患点ID（null 表示全局规则）
     * @param id            排除的规则ID（新增传 0L）
     * @return true=唯一，false=已存在
     */
    boolean checkDispatchRuleUnique(String name, Long hazardPointId, Long id);
}
