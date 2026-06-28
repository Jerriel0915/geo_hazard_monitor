package com.zwei.iot.alarm.service.engine;

import com.zwei.iot.alarm.mapper.AlarmStrategyHazardPointMapper;
import com.zwei.iot.device.domain.brief.HazardPointBrief;
import com.zwei.iot.device.service.IHazardPointQueryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StrategyScopeResolverTest {

    @Mock private AlarmStrategyHazardPointMapper bindingMapper;
    @Mock private IHazardPointQueryService hazardPointQueryService;
    @InjectMocks private StrategyScopeResolver resolver;

    @Test
    void resolveScope_star_returnsAllMonitoring() {
        when(bindingMapper.selectScopeValuesByStrategyId(1L)).thenReturn(List.of("*"));
        when(hazardPointQueryService.listMonitoring()).thenReturn(List.of(
            new HazardPointBrief(10L, "HP-10", "隐患点10", new BigDecimal("116.0"), new BigDecimal("40.0")),
            new HazardPointBrief(20L, "HP-20", "隐患点20", new BigDecimal("117.0"), new BigDecimal("41.0"))
        ));
        List<Long> result = resolver.resolveScope(1L);
        assertEquals(2, result.size());
        assertTrue(result.contains(10L));
        assertTrue(result.contains(20L));
    }

    @Test
    void resolveScope_group() {
        when(bindingMapper.selectScopeValuesByStrategyId(2L)).thenReturn(List.of("group:3"));
        when(hazardPointQueryService.listIdsByGroupId(3L)).thenReturn(List.of(30L, 31L));
        List<Long> result = resolver.resolveScope(2L);
        assertEquals(2, result.size());
        assertTrue(result.contains(30L));
        assertTrue(result.contains(31L));
    }

    @Test
    void resolveScope_specificId() {
        when(bindingMapper.selectScopeValuesByStrategyId(3L)).thenReturn(List.of("123"));
        List<Long> result = resolver.resolveScope(3L);
        assertEquals(1, result.size());
        assertTrue(result.contains(123L));
    }

    @Test
    void isHazardPointInScope_star_returnsTrue() {
        when(bindingMapper.selectScopeValuesByStrategyId(1L)).thenReturn(List.of("*"));
        assertTrue(resolver.isHazardPointInScope(1L, 999L));
    }

    @Test
    void isHazardPointInScope_notInScope_returnsFalse() {
        when(bindingMapper.selectScopeValuesByStrategyId(2L)).thenReturn(List.of("123"));
        assertFalse(resolver.isHazardPointInScope(2L, 456L));
    }
}
