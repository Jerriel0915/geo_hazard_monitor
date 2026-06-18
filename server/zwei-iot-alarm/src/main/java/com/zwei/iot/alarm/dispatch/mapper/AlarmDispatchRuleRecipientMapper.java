package com.zwei.iot.alarm.dispatch.mapper;

import com.zwei.iot.alarm.dispatch.domain.AlarmDispatchRuleRecipient;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 通知规则-接收人关联 Mapper（ROLE/DEPT/USER）
 */
@Mapper
public interface AlarmDispatchRuleRecipientMapper {

    /** 按 ruleId 物理删除（重建模式） */
    int deleteByRuleId(@Param("ruleId") Long ruleId);

    /** 批量插入 */
    int batchInsert(@Param("list") List<AlarmDispatchRuleRecipient> list);

    /** 按 ruleId 查询 */
    List<AlarmDispatchRuleRecipient> selectByRuleId(@Param("ruleId") Long ruleId);

    /** 按 ruleIds 批量查询（IN） */
    List<AlarmDispatchRuleRecipient> selectByRuleIds(@Param("ruleIds") List<Long> ruleIds);
}
