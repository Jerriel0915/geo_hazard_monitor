package com.zwei.iot.alarm.dispatch.dto;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 通知规则列表项
 */
@Data
public class AlarmDispatchRuleItemVO {

    private Long id;
    private String name;
    private String eventType;
    private List<String> alarmLevels;
    private List<String> channels;

    // 列表展示用汇总（避免 N+1）
    private boolean hazardPointAll;
    private List<String> hazardPointNames;
    private boolean deviceAll;
    private List<String> deviceNames;

    private boolean recipientAll;
    private String recipientSummary;

    private Integer isEnabled;
    private Date createTime;
    private String createBy;
    private String remark;
}
