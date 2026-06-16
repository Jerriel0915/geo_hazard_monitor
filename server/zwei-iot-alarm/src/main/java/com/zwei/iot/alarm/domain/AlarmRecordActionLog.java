package com.zwei.iot.alarm.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 告警动作日志 alarm_record_action_log。
 * <p>
 * 全动作流水：创建/再触发/等级变化/反馈/销警/误报/通知。
 *
 * @author zwei
 */
@Setter
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AlarmRecordActionLog implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;
    /** 告警记录ID */
    private Long alarmRecordId;
    /** 动作类型 (ActionType 枚举 name) */
    private String actionType;
    /** 变更前值 (状态或等级) */
    private String fromValue;
    /** 变更后值 (状态或等级) */
    private String toValue;
    /** 备注/反馈内容 */
    private String remarks;
    /** 描述内容 (FEEDBACK 等动作附带) */
    private String description;
    /** 附件文件名，多个逗号分隔 (/common/upload 返回的 fileName) */
    private String attachments;
    /** 操作人 */
    private String operator;
    /** 创建时间 */
    private Date createTime;
}
