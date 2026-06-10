package com.zwei.iot.alarm.service.impl;

import com.zwei.common.exception.ServiceException;
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
        if (!checkDispatchRuleUnique(rule.getName(), rule.getHazardPointId(), 0L)) {
            throw new ServiceException("新增失败，该隐患点下已存在同名分发规则");
        }
        rule.setCreateTime(new Date());
        return ruleMapper.insertRule(rule);
    }

    @Override
    public int update(AlarmDispatchRule rule) {
        if (!checkDispatchRuleUnique(rule.getName(), rule.getHazardPointId(), rule.getId())) {
            throw new ServiceException("修改失败，该隐患点下已存在同名分发规则");
        }
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

    @Override
    public boolean checkDispatchRuleUnique(String name, Long hazardPointId, Long id) {
        return ruleMapper.checkDispatchRuleUnique(name, hazardPointId, id) == null;
    }
}
