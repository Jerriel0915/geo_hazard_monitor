package com.zwei.iot.alarm.domain;

import lombok.Data;
import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 综合告警策略执行日志 alarm_strategy_execution_log
 *
 * @author zwei
 */
@Data
public class StrategyExecutionLog implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long strategyId;
    private String triggerType;
    private String triggerSource;
    private String hazardPointIds;
    private Integer resultLevel;
    private String resultStatus;
    private Long durationMs;
    private String scriptLogs;
    private String errorMessage;
    private Integer triggeredCount;
    private Date createTime;
}
