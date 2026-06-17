package com.zwei.iot.alarm.dispatch.domain;

import com.zwei.common.core.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * 通知规则主表实体
 */
@Setter
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AlarmDispatchRule extends BaseEntity {

    private Long id;
    private String name;
    /** 事件类型: ALARM / OFFLINE */
    private String eventType;
    /** 订阅告警等级（逗号分隔）: 1,2,3,4 */
    private String alarmLevels;
    /** 通知渠道（逗号分隔）: SYSTEM,SMS,EMAIL */
    private String channels;
    private Integer isEnabled;
    private Integer delFlag;
}
