package com.zwei.iot.alarm.dispatch.mapper;

import com.zwei.iot.alarm.dispatch.domain.AlarmDispatchRuleHazardPoint;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 通知规则-隐患点关联 Mapper
 */
@Mapper
public interface AlarmDispatchRuleHazardPointMapper {

    /** 按 ruleId 物理删除（重建模式） */
    int deleteByRuleId(@Param("ruleId") Long ruleId);

    /** 批量插入 */
    int batchInsert(@Param("list") List<AlarmDispatchRuleHazardPoint> list);

    /** 按 ruleId 查询 */
    List<AlarmDispatchRuleHazardPoint> selectByRuleId(@Param("ruleId") Long ruleId);

    /** 按 ruleIds 批量查询（IN） */
    List<AlarmDispatchRuleHazardPoint> selectByRuleIds(@Param("ruleIds") List<Long> ruleIds);
}
