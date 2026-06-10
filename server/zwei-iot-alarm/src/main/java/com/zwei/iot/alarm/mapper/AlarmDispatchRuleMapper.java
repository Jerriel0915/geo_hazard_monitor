package com.zwei.iot.alarm.mapper;

import com.zwei.iot.alarm.domain.AlarmDispatchRule;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AlarmDispatchRuleMapper {

    List<AlarmDispatchRule> selectRuleList(AlarmDispatchRule rule);

    List<AlarmDispatchRule> selectEnabledRules();

    AlarmDispatchRule selectRuleById(Long id);

    /**
     * 校验分发规则在指定隐患点下 name 唯一
     *
     * @param name          规则名称
     * @param hazardPointId 隐患点ID（null 表示全局规则）
     * @param id            排除的规则ID（新增传 0L）
     * @return 命中的规则（null 表示唯一）
     */
    AlarmDispatchRule checkDispatchRuleUnique(@org.apache.ibatis.annotations.Param("name") String name,
                                              @org.apache.ibatis.annotations.Param("hazardPointId") Long hazardPointId,
                                              @org.apache.ibatis.annotations.Param("id") Long id);

    int insertRule(AlarmDispatchRule rule);

    int updateRule(AlarmDispatchRule rule);

    int deleteRuleById(Long id);
}
