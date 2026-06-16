package com.zwei.iot.alarm.domain;

import com.zwei.common.core.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 告警记录表 alarm_record
 *
 * @author zwei
 */
@Setter
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AlarmRecord extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;
    /**
     * 隐患点ID
     */
    private Long hazardPointId;
    /**
     * 隐患点名称
     */
    private String hazardPointName;
    /**
     * 触发设备ID
     */
    private Long deviceId;
    /**
     * 触发传感器ID
     */
    private Long sensorId;
    /**
     * 触发监测内容ID
     */
    private Long monitorContentId;
    /**
     * 告警等级: 1=蓝色 2=黄色 3=橙色 4=红色
     */
    private Integer alarmLevel;
    /**
     * 告警等级文本
     */
    private String alarmLevelText;
    /**
     * 告警类型: THRESHOLD/COMPREHENSIVE
     */
    private String alarmType;
    /**
     * 告警消息
     */
    private String alarmMessage;
    /**
     * 触发的判据ID
     */
    private Long criteriaId;
    /**
     * 触发的综合策略ID
     */
    private Long strategyId;
    /**
     * 当前测量值
     */
    private BigDecimal currentValue;
    /**
     * 触发阈值
     */
    private BigDecimal thresholdValue;
    /**
     * 触发条件快照 JSON
     */
    private String triggerConditions;
    /**
     * 首次触发时间
     */
    private Date firstTriggerTime;
    /**
     * 最近触发时间
     */
    private Date lastTriggerTime;
    /**
     * 累计触发次数
     */
    private Integer triggerCount;
    /**
     * 警情状态: 1=待处理 2=处理中 3=已销警 4=误报
     */
    private Integer status;
    /**
     * 状态名称
     */
    private String statusName;
    /**
     * 处置人
     */
    private String resolvedBy;
    /**
     * 处置时间
     */
    private Date resolvedAt;
    /**
     * 处置备注
     */
    private String resolutionNote;

    // ── 以下为查询辅助字段（非数据库列）──

    /**
     * 告警时间起始（查询用）
     */
    private String startTime;
    /**
     * 告警时间截止（查询用）
     */
    private String endTime;
    /**
     * 处置人模糊匹配（查询用）
     */
    private String personName;
    /**
     * 告警等级多选，逗号分隔（查询用）
     */
    private String alarmLevels;
    /**
     * 告警类型多选，逗号分隔（查询用）
     */
    private String alarmTypes;
    /**
     * 警情状态多选，逗号分隔（查询用，覆盖 status 单值）
     */
    private String statusList;
}
