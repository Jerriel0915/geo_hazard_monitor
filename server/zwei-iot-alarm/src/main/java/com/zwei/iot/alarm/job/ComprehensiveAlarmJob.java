package com.zwei.iot.alarm.job;

import com.zwei.common.event.AlarmTriggeredEvent;
import com.zwei.iot.alarm.domain.AlarmConstants;
import com.zwei.iot.alarm.domain.AlarmRecord;
import com.zwei.iot.alarm.domain.AlarmStrategy;
import com.zwei.iot.alarm.mapper.AlarmStrategyHazardPointMapper;
import com.zwei.iot.alarm.mapper.AlarmStrategyMapper;
import com.zwei.iot.alarm.service.IAlarmRecordService;
import com.zwei.iot.alarm.service.engine.AlarmDedupService;
import com.zwei.iot.alarm.service.engine.GroovyScriptExecutor;
import com.zwei.iot.timeseries.compute.ScriptCacheOps;
import com.zwei.iot.timeseries.compute.ScriptSensorQuery;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 综合告警策略周期触发任务。
 * <p>
 * 每分钟扫描一次所有启用的 CRON 模式策略，检查是否达到触发时间，
 * 执行 Groovy 脚本并生成告警记录。
 *
 * @author zwei
 */
@Component
public class ComprehensiveAlarmJob {

    private static final Logger log = LoggerFactory.getLogger(ComprehensiveAlarmJob.class);

    /** 策略并发执行线程池 — 互不相关的策略并行执行，避免串行耗时累积超过调度间隔 */
    private final ExecutorService strategyExecutor = Executors.newFixedThreadPool(4);

    private final AlarmStrategyMapper strategyMapper;
    private final AlarmStrategyHazardPointMapper bindingMapper;
    private final GroovyScriptExecutor scriptExecutor;
    private final IAlarmRecordService alarmRecordService;
    private final ApplicationEventPublisher eventPublisher;
    private final AlarmDedupService dedupService;
    private final ScriptCacheOps cacheOps;
    private final ScriptSensorQuery scriptSensorQuery;

    public ComprehensiveAlarmJob(AlarmStrategyMapper strategyMapper,
                                 AlarmStrategyHazardPointMapper bindingMapper,
                                 GroovyScriptExecutor scriptExecutor,
                                 IAlarmRecordService alarmRecordService,
                                 ApplicationEventPublisher eventPublisher,
                                 AlarmDedupService dedupService,
                                 ScriptCacheOps cacheOps,
                                 ScriptSensorQuery scriptSensorQuery) {
        this.strategyMapper = strategyMapper;
        this.bindingMapper = bindingMapper;
        this.scriptExecutor = scriptExecutor;
        this.alarmRecordService = alarmRecordService;
        this.eventPublisher = eventPublisher;
        this.dedupService = dedupService;
        this.cacheOps = cacheOps;
        this.scriptSensorQuery = scriptSensorQuery;
    }

    /**
     * 每分钟执行一次，检查 CRON 模式的综合策略。
     */
    @Scheduled(fixedDelay = 60_000)
    public void executeCronStrategies() {
        List<AlarmStrategy> strategies = strategyMapper.selectEnabledByTriggerMode("CRON");
        if (strategies.isEmpty()) {
            return;
        }

        CountDownLatch latch = new CountDownLatch(strategies.size());
        for (AlarmStrategy strategy : strategies) {
            strategyExecutor.execute(() -> {
                try {
                    executeStrategy(strategy);
                } catch (Exception e) {
                    log.error("综合策略执行失败 strategyId={} name={}", strategy.getId(), strategy.getName(), e);
                    updateResult(strategy.getId(), "FAIL");
                } finally {
                    latch.countDown();
                }
            });
        }
        try {
            latch.await(45, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @PreDestroy
    void shutdown() {
        strategyExecutor.shutdown();
        try {
            if (!strategyExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                strategyExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            strategyExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void executeStrategy(AlarmStrategy strategy) {
        if (strategy.getScriptContent() == null || strategy.getScriptContent().isEmpty()) {
            log.debug("策略无脚本内容, 跳过 strategyId={}", strategy.getId());
            return;
        }

        // 优先级1: 直接绑定的隐患点
        // TODO: Task 9 will delete this class; temporary parse for compilation
        List<String> scopeValues = bindingMapper.selectScopeValuesByStrategyId(strategy.getId());
        List<Long> hazardPointIds = scopeValues.stream()
                .filter(s -> !s.startsWith("*") && !s.startsWith("group:"))
                .map(Long::parseLong)
                .collect(java.util.stream.Collectors.toList());

        // 优先级2: 无直接绑定时，通过监测类型ID兜底查找关联的隐患点
        if (hazardPointIds.isEmpty() && strategy.getMonitorTypeId() != null) {
            hazardPointIds = strategyMapper.selectHazardPointIdsByMonitorTypeId(strategy.getMonitorTypeId());
            if (!hazardPointIds.isEmpty()) {
                log.debug("策略通过监测类型兜底匹配到隐患点 strategyId={} monitorTypeId={} hpCount={}",
                        strategy.getId(), strategy.getMonitorTypeId(), hazardPointIds.size());
            }
        }

        if (hazardPointIds.isEmpty()) {
            log.debug("策略无绑定隐患点（直接绑定+监测类型兜底均为空）, 跳过 strategyId={}", strategy.getId());
            return;
        }

        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        // 执行脚本
        Map<String, Object> variables = new HashMap<>();
        variables.put("hazardPointIds", hazardPointIds);
        variables.put("currentTime", now);

        Map<String, Object> tools = new HashMap<>();
        tools.put("cache", cacheOps);
        tools.put("sensor", scriptSensorQuery);

        Integer alarmLevel = scriptExecutor.executeWithTools(strategy.getScriptContent(), variables, tools);
        if (alarmLevel == null || alarmLevel <= 0) {
            updateResult(strategy.getId(), "NO_ALARM");
            return;
        }

        int silenceMinutes = strategy.getSilenceMinutes() != null ? strategy.getSilenceMinutes() : 0;
        int triggeredCount = 0;

        // 为每个隐患点独立去重并创建告警
        for (Long hpId : hazardPointIds) {
            boolean shouldTrigger = dedupService.shouldTriggerAlarm(
                    strategy.getId(), /* as criteriaId surrogate for dedup */
                    hpId, alarmLevel, 1, silenceMinutes);
            if (!shouldTrigger) {
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
            triggeredCount++;

            // 发布告警事件
            eventPublisher.publishEvent(new AlarmTriggeredEvent(
                    saved.getId(), saved.getHazardPointId(),
                    saved.getAlarmLevel(), saved.getAlarmType(), saved.getAlarmMessage(),
                    saved.getTriggerReason()));
        }

        if (triggeredCount == 0) {
            updateResult(strategy.getId(), "NO_ALARM");
            return;
        }

        updateResult(strategy.getId(), "SUCCESS");
        log.info("综合策略告警已触发: strategyId={} level={} hpCount={}",
                strategy.getId(), alarmLevel, hazardPointIds.size());
    }

    private void updateResult(Long strategyId, String result) {
        strategyMapper.updateLastRunResult(strategyId,
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()),
                result);
    }

}
