package com.zwei.iot.alarm.mapper;

import com.zwei.iot.alarm.domain.AlarmDispatchRule;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AlarmDispatchRuleMapper {

    List<AlarmDispatchRule> selectRuleList(AlarmDispatchRule rule);

    List<AlarmDispatchRule> selectEnabledRules();

    AlarmDispatchRule selectRuleById(Long id);

    int insertRule(AlarmDispatchRule rule);

    int updateRule(AlarmDispatchRule rule);

    int deleteRuleById(Long id);
}
