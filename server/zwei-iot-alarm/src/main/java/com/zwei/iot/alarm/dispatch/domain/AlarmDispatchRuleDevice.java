package com.zwei.iot.alarm.dispatch.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

/**
 * 通知规则-设备关联（'*' 表示全部，离线通知专用）
 */
@Setter
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AlarmDispatchRuleDevice implements Serializable {

    private Long ruleId;
    /** 设备ID；"*" 表示全部 */
    private String deviceId;
}
