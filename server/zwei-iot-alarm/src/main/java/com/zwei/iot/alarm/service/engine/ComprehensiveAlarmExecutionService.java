package com.zwei.iot.alarm.service.engine;

import com.zwei.common.event.AlarmTriggeredEvent;
import com.zwei.iot.alarm.domain.AlarmConstants;
import com.zwei.iot.alarm.domain.AlarmRecord;
import com.zwei.iot.alarm.domain.AlarmStrategy;
import com.zwei.iot.alarm.domain.StrategyExecutionLog;
import com.zwei.iot.alarm.domain.dto.ExecutionResult;
import com.zwei.iot.alarm.mapper.AlarmStrategyHazardPointMapper;
import com.zwei.iot.alarm.mapper.AlarmStrategyMapper;
import com.zwei.iot.alarm.mapper.StrategyExecutionLogMapper;
import com.zwei.iot.alarm.service.IAlarmRecordService;
import com.zwei.iot.timeseries.compute.ScriptCacheOps;
import com.zwei.iot.timeseries.compute.ScriptSensorQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 综合告警统一执行服务 — 三种触发源（CRON/DataIngest/AlarmTrigger）的统一入口。
 *
 * <p>核心方法 {@link #execute(AlarmStrategy, Object, String)} 完成以下流程:
 * <ol>
 *   <li>通过 {@link StrategyScopeResolver} 解析策略绑定的隐患点列表</li>
 *   <li>构建脚本变量 (hazardPointIds / currentTime / event) 和工具 bean (cache / sensor / log)</li>
 *   <li>调用 {@link GroovyScriptExecutor} 执行 Groovy 脚本，返回告警等级 (0-4)</li>
 *   <li>等级 > 0 时，对每个隐患点执行去重检查 → 创建/更新 AlarmRecord → 发布 AlarmTriggeredEvent</li>
 *   <li>写入 {@link StrategyExecutionLog} 记录执行日志，更新策略 lastRunResult</li>
 * </ol>
 *
 * @author zwei
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ComprehensiveAlarmExecutionService {

    private final AlarmStrategyMapper strategyMapper;
    private final AlarmStrategyHazardPointMapper bindingMapper;
    private final GroovyScriptExecutor groovyScriptExecutor;
    private final IAlarmRecordService alarmRecordService;
    private final AlarmDedupService dedupService;
    private final ApplicationEventPublisher eventPublisher;
    private final ScriptCacheOps cacheOps;
    private final ScriptSensorQuery scriptSensorQuery;
    private final StrategyExecutionLogMapper executionLogMapper;
    private final StrategyScopeResolver scopeResolver;

    /**
     * 执行综合告警策略。
     *
     * @param strategy     策略定义（含脚本内容、静默周期等）
     * @param triggerEvent 触发事件 (MonitorDataIngestedEvent / AlarmTriggeredEvent / null=CRON)
     * @param triggerType  触发类型: CRON / DATA_INGEST / ALARM_TRIGGER
     * @return 执行结果 (告警等级 + 触发记录 + 耗时 + 脚本日志)
     */
    public ExecutionResult execute(AlarmStrategy strategy, Object triggerEvent, String triggerType) {
        long start = System.currentTimeMillis();

        // 1. Scope 解析 — 展开策略绑定的隐患点列表
        List<Long> hazardPointIds = scopeResolver.resolveScope(strategy.getId());

        // 2. 空范围快速返回 — 记录 NO_ALARM 日志
        if (hazardPointIds == null || hazardPointIds.isEmpty()) {
            return saveAndReturn(strategy, triggerType, triggerEvent, new ArrayList<>(),
                null, "NO_ALARM", start, null, null, 0, new ArrayList<>());
        }

        // 3. 构建脚本变量和工具
        ScriptLogger scriptLogger = new ScriptLogger(strategy.getId());
        Map<String, Object> variables = new HashMap<>();
        variables.put("hazardPointIds", hazardPointIds);
        variables.put("currentTime", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        if (triggerEvent != null) {
            variables.put("event", triggerEvent);
        }

        Map<String, Object> tools = new HashMap<>();
        tools.put("cache", cacheOps);
        tools.put("sensor", scriptSensorQuery);
        tools.put("log", scriptLogger);

        // 4. 执行 Groovy 脚本
        Integer alarmLevel = null;
        String errorMessage = null;
        try {
            alarmLevel = groovyScriptExecutor.executeWithTools(
                strategy.getScriptContent(), variables, tools);
        } catch (Exception e) {
            errorMessage = e.getMessage();
            log.error("[ComprehensiveAlarm] 策略 {} 脚本执行异常: {}", strategy.getId(), e.getMessage(), e);
        }

        // 5. 告警触发 — 等级 > 0 时对每个隐患点执行去重+创建
        int triggeredCount = 0;
        List<AlarmRecord> triggeredRecords = new ArrayList<>();
        if (alarmLevel != null && alarmLevel > 0) {
            triggeredRecords = triggerAlarms(strategy, hazardPointIds, alarmLevel);
            triggeredCount = triggeredRecords.size();
        }

        // 6. 确定执行结果状态
        String resultStatus;
        if (errorMessage != null) {
            resultStatus = "FAIL";
        } else if (alarmLevel != null && alarmLevel > 0) {
            resultStatus = "SUCCESS";
        } else {
            resultStatus = "NO_ALARM";
        }

        // 7. 写执行日志 + 返回
        return saveAndReturn(strategy, triggerType, triggerEvent, hazardPointIds,
            alarmLevel, resultStatus, start, scriptLogger, errorMessage, triggeredCount,
            triggeredRecords);
    }

    /**
     * 告警触发逻辑。对每个隐患点: 去重检查 → 创建/更新 AlarmRecord → 发布 AlarmTriggeredEvent。
     *
     * @param strategy       策略
     * @param hazardPointIds 隐患点 ID 列表
     * @param alarmLevel     脚本返回的告警等级 (1-4)
     * @return 成功触发的告警记录列表
     */
    private List<AlarmRecord> triggerAlarms(AlarmStrategy strategy, List<Long> hazardPointIds,
                                            int alarmLevel) {
        int silenceMinutes = strategy.getSilenceMinutes() != null
                             ? strategy.getSilenceMinutes() : 0;
        List<AlarmRecord> records = new ArrayList<>();

        for (Long hpId : hazardPointIds) {
            try {
                boolean shouldTrigger = dedupService.shouldTriggerAlarm(
                    strategy.getId(), hpId, alarmLevel, 1, silenceMinutes);
                if (!shouldTrigger) {
                    log.debug("[ComprehensiveAlarm] 策略 {} 隐患点 {} 等级 {} 被去重拦截",
                        strategy.getId(), hpId, alarmLevel);
                    continue;
                }

                AlarmRecord record = AlarmRecord.builder()
                    .hazardPointId(hpId)
                    .alarmLevel(alarmLevel)
                    .alarmLevelText(AlarmConstants.resolveLevelText(alarmLevel))
                    .alarmType("COMPREHENSIVE")
                    .alarmMessage("综合策略告警: " + strategy.getName())
                    .strategyId(strategy.getId())
                    .currentValue(BigDecimal.ZERO)
                    .createBy(AlarmConstants.SYSTEM_OPERATOR)
                    .createTime(new Date())
                    .build();

                AlarmRecord saved = alarmRecordService.createOrUpdateAlarm(record);
                records.add(saved);

                eventPublisher.publishEvent(new AlarmTriggeredEvent(
                    saved.getId(), saved.getHazardPointId(),
                    saved.getAlarmLevel(), saved.getAlarmType(),
                    saved.getAlarmMessage(), saved.getTriggerReason()));
            } catch (Exception e) {
                log.error("[ComprehensiveAlarm] 综合告警触发失败，跳过该隐患点: strategyId={} hazardPointId={}",
                    strategy.getId(), hpId, e);
            }
        }
        return records;
    }

    /**
     * 保存执行日志并返回 ExecutionResult。
     *
     * @param strategy       策略
     * @param triggerType    触发类型
     * @param triggerEvent   触发事件 (可为 null)
     * @param hazardPointIds 隐患点 ID 列表
     * @param alarmLevel     脚本返回的告警等级 (可为 null)
     * @param resultStatus   执行状态: SUCCESS / NO_ALARM / FAIL
     * @param startMs        执行开始时间戳
     * @param scriptLogger   脚本日志记录器 (可为 null)
     * @param errorMessage   错误信息 (可为 null)
     * @param triggeredCount 触发告警数量
     * @param triggeredRecords 触发的告警记录列表 (用于 ExecutionResult 返回)
     * @return 执行结果
     */
    private ExecutionResult saveAndReturn(AlarmStrategy strategy, String triggerType,
                                          Object triggerEvent, List<Long> hazardPointIds,
                                          Integer alarmLevel, String resultStatus,
                                          long startMs, ScriptLogger scriptLogger,
                                          String errorMessage, int triggeredCount,
                                          List<AlarmRecord> triggeredRecords) {
        long durationMs = System.currentTimeMillis() - startMs;

        // 构建触发事件摘要 (截断防止超长)
        String triggerSource = null;
        if (triggerEvent != null) {
            triggerSource = triggerEvent.toString();
            if (triggerSource.length() > 2000) {
                triggerSource = triggerSource.substring(0, 2000);
            }
        }

        // 隐患点 ID 列表 → 逗号分隔字符串
        String hpIdsStr = null;
        if (!hazardPointIds.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < hazardPointIds.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(hazardPointIds.get(i));
            }
            hpIdsStr = sb.toString();
        }

        // 写 execution_log
        StrategyExecutionLog logEntry = new StrategyExecutionLog();
        logEntry.setStrategyId(strategy.getId());
        logEntry.setTriggerType(triggerType);
        logEntry.setTriggerSource(triggerSource);
        logEntry.setHazardPointIds(hpIdsStr);
        logEntry.setResultLevel(alarmLevel);
        logEntry.setResultStatus(resultStatus);
        logEntry.setDurationMs(durationMs);
        logEntry.setScriptLogs(scriptLogger != null ? scriptLogger.toJson() : null);
        logEntry.setErrorMessage(errorMessage);
        logEntry.setTriggeredCount(triggeredCount);
        executionLogMapper.insertLog(logEntry);

        // 更新策略最近执行结果
        strategyMapper.updateLastRunResult(
            strategy.getId(),
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()),
            resultStatus);

        return new ExecutionResult(alarmLevel, triggeredRecords, durationMs,
            scriptLogger != null ? scriptLogger.toJson() : null);
    }
}
