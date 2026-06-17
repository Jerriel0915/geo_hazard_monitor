package com.zwei.iot.alarm.dispatch.service;

import java.util.Set;

public interface IAlarmRecipientResolver {

    /**
     * 把规则里的接收人配置展开为去重后的 userId 列表
     * ROLE: 查 sys_user_role（'*' 则所有活跃用户）
     * DEPT: 查 sys_user.dept_id（'*' 则所有活跃用户）
     * USER: 直接用（'*' 则所有活跃用户）
     */
    Set<Long> resolveUserIds(Long ruleId);
}
