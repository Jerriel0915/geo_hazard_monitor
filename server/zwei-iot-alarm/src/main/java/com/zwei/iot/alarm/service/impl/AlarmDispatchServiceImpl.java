package com.zwei.iot.alarm.service.impl;

import com.zwei.iot.alarm.domain.AlarmDispatchRule;
import com.zwei.iot.alarm.mapper.AlarmDispatchRuleMapper;
import com.zwei.iot.alarm.service.IAlarmDispatchService;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 告警分发规则服务实现
 *
 * @author zwei
 */
@Service
public class AlarmDispatchServiceImpl implements IAlarmDispatchService {

    private final AlarmDispatchRuleMapper ruleMapper;

    public AlarmDispatchServiceImpl(AlarmDispatchRuleMapper ruleMapper) {
        this.ruleMapper = ruleMapper;
    }

    @Override
    public List<AlarmDispatchRule> selectList(AlarmDispatchRule rule) {
        return ruleMapper.selectRuleList(rule);
    }

    @Override
    public AlarmDispatchRule selectById(Long id) {
        return ruleMapper.selectRuleById(id);
    }

    @Override
    public int insert(AlarmDispatchRule rule) {
        rule.setCreateTime(new Date());
        return ruleMapper.insertRule(rule);
    }

    @Override
    public int update(AlarmDispatchRule rule) {
        rule.setUpdateTime(new Date());
        return ruleMapper.updateRule(rule);
    }

    @Override
    public int delete(Long id) {
        return ruleMapper.deleteRuleById(id);
    }

    @Override
    public List<AlarmDispatchRule> selectEnabledRules() {
        return ruleMapper.selectEnabledRules();
    }
}
