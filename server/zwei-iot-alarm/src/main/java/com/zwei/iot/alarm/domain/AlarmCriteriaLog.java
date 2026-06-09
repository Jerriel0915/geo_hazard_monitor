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
 * 告警判据变更日志 alarm_criteria_log
 *
 * @author zwei
 */
@Setter
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AlarmCriteriaLog implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;
    /**
     * 判据ID
     */
    private Long criteriaId;
    /**
     * 变更后的版本号
     */
    private Integer version;
    /**
     * 变更类型: CREATE/UPDATE/DELETE/TOGGLE
     */
    private String changeType;
    /**
     * 变更前的值 JSON
     */
    private String oldValue;
    /**
     * 变更后的值 JSON
     */
    private String newValue;
    /**
     * 操作人
     */
    private String createBy;
    /**
     * 操作时间
     */
    private Date createTime;
}
