package com.zwei.iot.alarm.dispatch.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

/**
 * 通知规则-接收人关联（ROLE/DEPT/USER，'*' 表示该类型全部）
 */
@Setter
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AlarmDispatchRuleRecipient implements Serializable {

    private Long ruleId;
    /** ROLE / DEPT / USER */
    private String recipientType;
    /** 角色/部门/用户 ID；"*" 表示该类型全部 */
    private String recipientId;
}
