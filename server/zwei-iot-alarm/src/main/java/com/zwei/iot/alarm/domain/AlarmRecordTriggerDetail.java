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
 * 告警触发明细 alarm_record_trigger_detail。
 * <p>
 * 引擎每次触发写一条数据快照。
 *
 * @author zwei
 */
@Setter
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AlarmRecordTriggerDetail implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;
    /** 告警记录ID */
    private Long alarmRecordId;
    /** 告警时间 */
    private Date triggerTime;
    /** 触发时等级 1-4 */
    private Integer alarmLevel;
    /** THRESHOLD / COMPREHENSIVE */
    private String alarmType;
    /** 告警描述 */
    private String alarmMessage;
    /** 创建时间 */
    private Date createTime;
}
