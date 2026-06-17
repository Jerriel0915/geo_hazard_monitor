package com.zwei.iot.alarm.dispatch.service;

import com.zwei.iot.alarm.dispatch.dto.AlarmDispatchRuleCreateRequest;
import com.zwei.iot.alarm.dispatch.dto.AlarmDispatchRuleDetailVO;
import com.zwei.iot.alarm.dispatch.dto.AlarmDispatchRuleItemVO;
import com.zwei.iot.alarm.dispatch.dto.AlarmDispatchRuleQuery;

import java.util.List;

/**
 * 通知规则 Service
 */
public interface IAlarmDispatchRuleService {

    /** 分页列表（带关联展开汇总） */
    List<AlarmDispatchRuleItemVO> selectList(AlarmDispatchRuleQuery query);

    /** 详情（包含所有关联数据） */
    AlarmDispatchRuleDetailVO selectDetail(Long id);

    /** 创建（事务：主表 + 三张关联表） */
    int create(AlarmDispatchRuleCreateRequest req);

    /** 更新（事务：先删后插关联表） */
    int update(Long id, AlarmDispatchRuleCreateRequest req);

    /** 逻辑删除（连带物理删除关联表） */
    int delete(Long id);

    /** 启用/禁用 */
    int toggleEnabled(Long id, Integer isEnabled);

    /**
     * 接收人选项（前端勾选用）
     */
    RecipientOptions selectRecipientOptions();

    /**
     * 接收人选项聚合（角色/部门/用户）
     */
    record RecipientOptions(
        List<AlarmDispatchRuleDetailVO.RoleOption> roles,
        List<AlarmDispatchRuleDetailVO.DeptOption> depts,
        List<AlarmDispatchRuleDetailVO.UserOption> users
    ) {}
}
