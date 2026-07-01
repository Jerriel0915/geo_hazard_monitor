package com.zwei.iot.parser.service;

import com.zwei.iot.parser.domain.DataParseStrategy;
import com.zwei.iot.parser.dto.LastRunTimeEntry;
import com.zwei.iot.parser.mapper.DataParseLogMapper;
import com.zwei.iot.parser.mapper.DataParseStrategyMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 验证 {@link StrategyLastRunTimeUpdater} 定时批量回写 last_run_time (B4 修复)。
 */
@DisplayName("StrategyLastRunTimeUpdater — 定时批量回写")
class StrategyLastRunTimeUpdaterTest {

    private StrategyLastRunTimeUpdater updater;
    private DataParseStrategyMapper strategyMapper;
    private DataParseLogMapper logMapper;

    @BeforeEach
    void setUp() {
        updater = new StrategyLastRunTimeUpdater();
        strategyMapper = mock(DataParseStrategyMapper.class);
        logMapper = mock(DataParseLogMapper.class);
        ReflectionTestUtils.setField(updater, "strategyMapper", strategyMapper);
        ReflectionTestUtils.setField(updater, "logMapper", logMapper);
    }

    @Test
    @DisplayName("有日志的策略应批量回写 last_run_time")
    void syncsLastRunTimeFromLogs() {
        DataParseStrategy s1 = strategy(1L);
        DataParseStrategy s2 = strategy(2L);
        when(strategyMapper.selectEnabled()).thenReturn(List.of(s1, s2));

        Date t1 = new Date(1700000000000L);
        Date t2 = new Date(1700000001000L);
        when(logMapper.selectLatestCreateTimeByStrategyIds(List.of(1L, 2L)))
                .thenReturn(List.of(
                        Map.of("strategyId", 1L, "lastRunTime", t1),
                        Map.of("strategyId", 2L, "lastRunTime", t2)));

        updater.syncLastRunTime();

        verify(strategyMapper).batchUpdateLastRunTime(argThat(entries -> {
            @SuppressWarnings("unchecked")
            List<LastRunTimeEntry> list = (List<LastRunTimeEntry>) entries;
            return list.size() == 2
                    && list.stream().anyMatch(e -> e.strategyId().equals(1L) && e.lastRunTime().equals(t1))
                    && list.stream().anyMatch(e -> e.strategyId().equals(2L) && e.lastRunTime().equals(t2));
        }));
    }

    @Test
    @DisplayName("无日志的策略不更新（selectEnabled 返回空时跳过）")
    void noEnabledStrategiesSkipsSync() {
        when(strategyMapper.selectEnabled()).thenReturn(List.of());

        updater.syncLastRunTime();

        verify(logMapper, never()).selectLatestCreateTimeByStrategyIds(any());
        verify(strategyMapper, never()).batchUpdateLastRunTime(any());
    }

    private DataParseStrategy strategy(Long id) {
        DataParseStrategy s = new DataParseStrategy();
        s.setId(id);
        s.setStatus(1);
        return s;
    }
}
