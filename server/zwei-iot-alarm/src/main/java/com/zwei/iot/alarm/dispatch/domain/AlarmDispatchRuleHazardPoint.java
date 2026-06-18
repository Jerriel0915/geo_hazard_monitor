package com.zwei.iot.alarm.dispatch.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

/**
 * 通知规则-隐患点关联（'*' 表示全部）
 */
@Setter
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AlarmDispatchRuleHazardPoint implements Serializable {

    private Long ruleId;
    /** 隐患点ID；"*" 表示全部 */
    private String hazardPointId;
}
