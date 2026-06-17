package com.zwei.iot.alarm.dispatch.dto;

import lombok.Data;

/**
 * 通知规则列表查询参数
 */
@Data
public class AlarmDispatchRuleQuery {

    private String name;
    private String eventType;
    private Integer isEnabled;
    private Integer pageNum = 1;
    private Integer pageSize = 10;
}
