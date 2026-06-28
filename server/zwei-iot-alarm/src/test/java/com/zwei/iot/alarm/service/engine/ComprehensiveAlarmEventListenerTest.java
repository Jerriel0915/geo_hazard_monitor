package com.zwei.iot.alarm.service.engine;

import com.zwei.common.event.AlarmTriggeredEvent;
import com.zwei.common.event.MonitorDataIngestedEvent;
import com.zwei.iot.alarm.domain.AlarmStrategy;
import com.zwei.iot.alarm.mapper.AlarmStrategyMapper;
import com.zwei.iot.device.service.IDeviceHazardRelationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ComprehensiveAlarmEventListener 单元测试。
 *
 * <p>覆盖 4 个场景:
 * <ol>
 *   <li>MonitorDataIngestedEvent → scope 匹配 → 执行策略</li>
 *   <li>MonitorDataIngestedEvent → scope 不匹配 → 跳过</li>
 *   <li>AlarmTriggeredEvent COMPREHENSIVE 类型 → 跳过防循环</li>
 *   <li>AlarmTriggeredEvent THRESHOLD 类型 → scope 匹配 → 执行策略</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
class ComprehensiveAlarmEventListenerTest {

    @Mock private AlarmStrategyMapper strategyMapper;
    @Mock private StrategyScopeResolver scopeResolver;
    @Mock private ComprehensiveAlarmExecutionService executionService;
    @Mock private IDeviceHazardRelationService hazardRelationService;
    @InjectMocks private ComprehensiveAlarmEventListener listener;

    private AlarmStrategy buildStrategy(Long id, String mode) {
        return AlarmStrategy.builder().id(id).name("策略" + id).triggerMode(mode).isEnabled(1).build();
    }

    @Test
    void onDataIngested_scopeMatches_executes() {
        AlarmStrategy s = buildStrategy(1L, "REALTIME");
        when(strategyMapper.selectEnabledByTriggerMode("REALTIME")).thenReturn(List.of(s));

        MonitorDataIngestedEvent event = mock(MonitorDataIngestedEvent.class);
        when(event.getDeviceId()).thenReturn(10L);
        when(hazardRelationService.getHazardPointIdsByDeviceIds(List.of(10L)))
            .thenReturn(List.of(5L));
        when(scopeResolver.isHazardPointInScope(1L, 5L)).thenReturn(true);

        listener.onDataIngested(event);

        verify(executionService).execute(s, event, "DATA_INGEST");
    }

    @Test
    void onDataIngested_scopeNotMatch_skips() {
        AlarmStrategy s = buildStrategy(1L, "REALTIME");
        when(strategyMapper.selectEnabledByTriggerMode("REALTIME")).thenReturn(List.of(s));

        MonitorDataIngestedEvent event = mock(MonitorDataIngestedEvent.class);
        when(event.getDeviceId()).thenReturn(10L);
        when(hazardRelationService.getHazardPointIdsByDeviceIds(List.of(10L)))
            .thenReturn(List.of(5L));
        when(scopeResolver.isHazardPointInScope(1L, 5L)).thenReturn(false);

        listener.onDataIngested(event);

        verify(executionService, never()).execute(any(), any(), anyString());
    }

    @Test
    void onAlarmTriggered_comprehensiveType_skips() {
        AlarmTriggeredEvent event = new AlarmTriggeredEvent(
            1L, 5L, 2, "COMPREHENSIVE", "综合告警", "首次告警");

        listener.onAlarmTriggered(event);

        verify(strategyMapper, never()).selectEnabledByTriggerMode(anyString());
        verify(executionService, never()).execute(any(), any(), anyString());
    }

    @Test
    void onAlarmTriggered_thresholdType_executes() {
        AlarmStrategy s = buildStrategy(1L, "REALTIME");
        when(strategyMapper.selectEnabledByTriggerMode("REALTIME")).thenReturn(List.of(s));

        AlarmTriggeredEvent event = new AlarmTriggeredEvent(
            1L, 5L, 2, "THRESHOLD", "阈值告警", "首次告警");
        when(scopeResolver.isHazardPointInScope(1L, 5L)).thenReturn(true);

        listener.onAlarmTriggered(event);

        verify(executionService).execute(s, event, "ALARM_TRIGGER");
    }
}
