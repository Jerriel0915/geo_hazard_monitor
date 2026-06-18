package com.zwei.iot.alarm.dispatch.mapper;

import com.zwei.iot.alarm.dispatch.domain.AlarmDispatchRuleDevice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 通知规则-设备关联 Mapper（离线通知专用）
 */
@Mapper
public interface AlarmDispatchRuleDeviceMapper {

    /** 按 ruleId 物理删除（重建模式） */
    int deleteByRuleId(@Param("ruleId") Long ruleId);

    /** 批量插入 */
    int batchInsert(@Param("list") List<AlarmDispatchRuleDevice> list);

    /** 按 ruleId 查询 */
    List<AlarmDispatchRuleDevice> selectByRuleId(@Param("ruleId") Long ruleId);

    /** 按 ruleIds 批量查询（IN） */
    List<AlarmDispatchRuleDevice> selectByRuleIds(@Param("ruleIds") List<Long> ruleIds);
}
