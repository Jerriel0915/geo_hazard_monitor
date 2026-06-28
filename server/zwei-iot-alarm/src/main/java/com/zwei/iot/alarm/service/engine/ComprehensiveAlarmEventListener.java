package com.zwei.iot.alarm.service.engine;

import com.zwei.common.event.AlarmTriggeredEvent;
import com.zwei.common.event.MonitorDataIngestedEvent;
import com.zwei.iot.alarm.domain.AlarmStrategy;
import com.zwei.iot.alarm.mapper.AlarmStrategyMapper;
import com.zwei.iot.device.service.IDeviceHazardRelationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * REALTIME 模式综合告警事件监听器。
 *
 * <p>监听两种事件:
 * <ul>
 *   <li>{@link MonitorDataIngestedEvent} — 数据入库后触发，通过设备→隐患点关系过滤策略 scope</li>
 *   <li>{@link AlarmTriggeredEvent} — 其他类型告警触发后级联，跳过 COMPREHENSIVE 类型防止无限循环</li>
 * </ul>
 *
 * <p>循环防护：当 {@code AlarmTriggeredEvent.alarmType == "COMPREHENSIVE"} 时直接返回，
 * 因为这是综合告警自身发出的告警事件，不应再次触发策略评估。
 *
 * @author zwei
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ComprehensiveAlarmEventListener {

    private final AlarmStrategyMapper strategyMapper;
    private final StrategyScopeResolver scopeResolver;
    private final ComprehensiveAlarmExecutionService executionService;
    private final IDeviceHazardRelationService hazardRelationService;

    /**
     * 监听数据入库事件。
     *
     * <p>流程:
     * <ol>
     *   <li>查询所有启用的 REALTIME 策略</li>
     *   <li>通过设备 ID 反查关联的隐患点 ID 列表</li>
     *   <li>对每个策略检查是否有隐患点在其 scope 范围内</li>
     *   <li>匹配则委托 {@link ComprehensiveAlarmExecutionService#execute} 执行</li>
     * </ol>
     *
     * @param event 监测数据入库事件
     */
    @Async("alarmEvalExecutor")
    @EventListener
    public void onDataIngested(MonitorDataIngestedEvent event) {
        List<AlarmStrategy> strategies = strategyMapper.selectEnabledByTriggerMode("REALTIME");
        if (strategies.isEmpty()) return;

        List<Long> eventHazardPointIds = hazardRelationService
            .getHazardPointIdsByDeviceIds(Collections.singletonList(event.getDeviceId()));
        if (eventHazardPointIds.isEmpty()) return;

        for (AlarmStrategy strategy : strategies) {
            boolean matched = eventHazardPointIds.stream()
                .anyMatch(hpId -> scopeResolver.isHazardPointInScope(strategy.getId(), hpId));
            if (!matched) continue;

            try {
                executionService.execute(strategy, event, "DATA_INGEST");
            } catch (Exception e) {
                log.error("REALTIME 策略执行失败 strategyId={}", strategy.getId(), e);
            }
        }
    }

    /**
     * 监听告警触发事件。
     *
     * <p>循环防护：跳过 COMPREHENSIVE 类型告警，因为综合策略触发的告警不应再次触发策略评估。
     * 其他类型（如 THRESHOLD）则级联检查策略 scope 并执行。
     *
     * @param event 告警触发事件
     */
    @Async("alarmEvalExecutor")
    @EventListener
    public void onAlarmTriggered(AlarmTriggeredEvent event) {
        if ("COMPREHENSIVE".equals(event.getAlarmType())) return;

        List<AlarmStrategy> strategies = strategyMapper.selectEnabledByTriggerMode("REALTIME");
        if (strategies.isEmpty()) return;

        for (AlarmStrategy strategy : strategies) {
            if (!scopeResolver.isHazardPointInScope(strategy.getId(), event.getHazardPointId()))
                continue;

            try {
                executionService.execute(strategy, event, "ALARM_TRIGGER");
            } catch (Exception e) {
                log.error("REALTIME 策略执行失败 strategyId={}", strategy.getId(), e);
            }
        }
    }
}
