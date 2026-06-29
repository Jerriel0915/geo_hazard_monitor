package com.zwei.iot.alarm.domain;

import com.zwei.common.core.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.util.Date;

/**
 * 综合告警策略表 alarm_strategy
 *
 * @author zwei
 */
@Setter
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AlarmStrategy extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;
    /**
     * 策略名称
     */
    private String name;
    /**
     * 策略描述
     */
    private String description;
    /**
     * 监测类型ID（NULL=仅按隐患点绑定生效；非NULL=兜底策略，适用所有关联该监测类型的隐患点）
     */
    private Long monitorTypeId;
    /**
     * 触发模式: REALTIME/CRON
     */
    private String triggerMode;
    /**
     * Cron表达式
     */
    private String cronExpression;
    /**
     * 脚本类型: GROOVY/JAVASCRIPT
     */
    private String scriptType;
    /**
     * 脚本内容
     */
    private String scriptContent;
    /**
     * 默认告警等级: 1=蓝 2=黄 3=橙 4=红
     */
    private Integer defaultAlarmLevel;
    /**
     * 静默周期（分钟）
     */
    private Integer silenceMinutes;
    /**
     * 启用状态
     */
    private Integer isEnabled;
    /**
     * 最近执行时间
     */
    private Date lastRunTime;
    /**
     * 最近执行结果
     */
    private String lastRunResult;
    /**
     * 删除标记
     */
    private Integer delFlag;
    /**
     * 绑定隐患点数量（非持久化字段，由 SQL 子查询填充）
     */
    private Integer scopeCount;
}
