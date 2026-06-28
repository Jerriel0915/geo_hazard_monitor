package com.zwei.iot.alarm.domain.dto;

import com.zwei.iot.alarm.domain.AlarmRecord;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 综合告警策略执行结果。
 */
@Data
@AllArgsConstructor
public class ExecutionResult {
    private Integer alarmLevel;
    private List<AlarmRecord> triggeredRecords;
    private long durationMs;
    private String scriptLogs;
}
