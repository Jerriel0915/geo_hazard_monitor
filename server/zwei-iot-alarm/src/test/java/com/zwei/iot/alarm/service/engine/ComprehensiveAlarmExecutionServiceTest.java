package com.zwei.iot.alarm.service.engine;

import com.zwei.common.event.AlarmTriggeredEvent;
import com.zwei.common.event.MonitorDataIngestedEvent;
import com.zwei.iot.alarm.domain.AlarmRecord;
import com.zwei.iot.alarm.domain.AlarmStrategy;
import com.zwei.iot.alarm.mapper.AlarmStrategyMapper;
import com.zwei.iot.alarm.mapper.AlarmStrategyHazardPointMapper;
import com.zwei.iot.alarm.mapper.StrategyExecutionLogMapper;
import com.zwei.iot.alarm.service.IAlarmRecordService;
import com.zwei.iot.timeseries.compute.ScriptCacheOps;
import com.zwei.iot.timeseries.compute.ScriptSensorQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ComprehensiveAlarmExecutionService 单元测试。
 *
 * <p>覆盖 6 个核心场景:
 * <ol>
 *   <li>CRON 触发 → 脚本返回 0 (无告警)</li>
 *   <li>CRON 触发 → 脚本返回 2 (触发告警)</li>
 *   <li>DATA_INGEST 触发 → event 变量注入脚本</li>
 *   <li>脚本异常 → 记录 FAIL 日志</li>
 *   <li>空范围 → 跳过执行</li>
 *   <li>静默期 → dedup 部分拦截</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
class ComprehensiveAlarmExecutionServiceTest {

    @Mock private AlarmStrategyMapper strategyMapper;
    @Mock private AlarmStrategyHazardPointMapper bindingMapper;
    @Mock private GroovyScriptExecutor groovyScriptExecutor;
    @Mock private IAlarmRecordService alarmRecordService;
    @Mock private AlarmDedupService dedupService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private ScriptCacheOps cacheOps;
    @Mock private ScriptSensorQuery scriptSensorQuery;
    @Mock private StrategyExecutionLogMapper executionLogMapper;
    @Mock private StrategyScopeResolver scopeResolver;
    @InjectMocks private ComprehensiveAlarmExecutionService service;

    private AlarmStrategy buildStrategy() {
        return AlarmStrategy.builder()
            .id(1L).name("测试策略").triggerMode("CRON")
            .scriptContent("return 0").silenceMinutes(0).isEnabled(1)
            .build();
    }

    @Test
    void execute_cron_noAlarm() {
        AlarmStrategy strategy = buildStrategy();
        when(scopeResolver.resolveScope(1L)).thenReturn(List.of(10L, 20L));
        when(groovyScriptExecutor.executeWithTools(anyString(), anyMap(), anyMap()))
            .thenReturn(0);

        var result = service.execute(strategy, null, "CRON");

        assertNotNull(result);
        assertEquals(0, result.getAlarmLevel());
        verify(executionLogMapper).insertLog(any());
        verify(dedupService, never()).shouldTriggerAlarm(anyLong(), anyLong(), anyInt(), anyInt(), anyInt());
    }

    @Test
    void execute_cron_triggersAlarm() {
        AlarmStrategy strategy = buildStrategy();
        strategy.setScriptContent("return 2");
        when(scopeResolver.resolveScope(1L)).thenReturn(List.of(10L));
        when(groovyScriptExecutor.executeWithTools(anyString(), anyMap(), anyMap()))
            .thenReturn(2);
        when(dedupService.shouldTriggerAlarm(1L, 10L, 2, 1, 0)).thenReturn(true);
        when(alarmRecordService.createOrUpdateAlarm(any())).thenReturn(
            AlarmRecord.builder().id(100L).hazardPointId(10L).alarmLevel(2).build());

        var result = service.execute(strategy, null, "CRON");

        assertEquals(2, result.getAlarmLevel());
        verify(alarmRecordService).createOrUpdateAlarm(any());
        verify(eventPublisher).publishEvent(any(AlarmTriggeredEvent.class));
    }

    @Test
    void execute_dataIngest_eventPassedToScript() {
        AlarmStrategy strategy = buildStrategy();
        MonitorDataIngestedEvent event = mock(MonitorDataIngestedEvent.class);
        when(scopeResolver.resolveScope(1L)).thenReturn(List.of(10L));
        when(groovyScriptExecutor.executeWithTools(anyString(), anyMap(), anyMap()))
            .thenReturn(0);

        service.execute(strategy, event, "DATA_INGEST");

        // 验证 event 变量被传入 variables
        verify(groovyScriptExecutor).executeWithTools(
            anyString(),
            argThat(vars -> vars.containsKey("event")),
            anyMap());
    }

    @Test
    void execute_scriptThrows_logsFail() {
        AlarmStrategy strategy = buildStrategy();
        when(scopeResolver.resolveScope(1L)).thenReturn(List.of(10L));
        when(groovyScriptExecutor.executeWithTools(anyString(), anyMap(), anyMap()))
            .thenThrow(new RuntimeException("脚本执行失败"));

        var result = service.execute(strategy, null, "CRON");

        assertNotNull(result);
        verify(executionLogMapper).insertLog(argThat(log ->
            "FAIL".equals(log.getResultStatus()) &&
            log.getErrorMessage() != null));
    }

    @Test
    void execute_emptyScope_skips() {
        AlarmStrategy strategy = buildStrategy();
        when(scopeResolver.resolveScope(1L)).thenReturn(List.of());

        var result = service.execute(strategy, null, "CRON");

        verify(groovyScriptExecutor, never()).executeWithTools(anyString(), anyMap(), anyMap());
        verify(executionLogMapper).insertLog(argThat(log -> "NO_ALARM".equals(log.getResultStatus())));
    }

    @Test
    void execute_silencePeriod_dedupBlocksAlarm() {
        AlarmStrategy strategy = buildStrategy();
        strategy.setScriptContent("return 3");
        when(scopeResolver.resolveScope(1L)).thenReturn(List.of(10L, 20L));
        when(groovyScriptExecutor.executeWithTools(anyString(), anyMap(), anyMap()))
            .thenReturn(3);
        when(dedupService.shouldTriggerAlarm(eq(1L), eq(10L), eq(3), eq(1), eq(0)))
            .thenReturn(false);
        when(dedupService.shouldTriggerAlarm(eq(1L), eq(20L), eq(3), eq(1), eq(0)))
            .thenReturn(true);
        when(alarmRecordService.createOrUpdateAlarm(any())).thenReturn(
            AlarmRecord.builder().id(200L).hazardPointId(20L).alarmLevel(3).build());

        var result = service.execute(strategy, null, "CRON");

        assertEquals(3, result.getAlarmLevel());
        verify(alarmRecordService, times(1)).createOrUpdateAlarm(any());
        verify(eventPublisher, times(1)).publishEvent(any(AlarmTriggeredEvent.class));
    }
}
