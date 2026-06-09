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
 * 告警状态变更日志 alarm_record_log
 *
 * @author zwei
 */
@Setter
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AlarmRecordLog implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;
    /**
     * 告警记录ID
     */
    private Long alarmId;
    /**
     * 变更前状态
     */
    private Integer fromStatus;
    /**
     * 变更后状态
     */
    private Integer toStatus;
    /**
     * 处置类型: 开始处置/已销警/标记误报/批量销警/批量误报
     */
    private String disposalType;
    /**
     * 操作人
     */
    private String operator;
    /**
     * 处置结果描述
     */
    private String disposalResult;
    /**
     * 操作备注
     */
    private String note;
    /**
     * 创建时间
     */
    private Date createTime;
}
