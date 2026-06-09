package com.zwei.iot.alarm.service.impl;

import com.zwei.iot.alarm.domain.AlarmRecord;
import com.zwei.iot.alarm.domain.AlarmRecordLog;
import com.zwei.iot.alarm.mapper.AlarmRecordLogMapper;
import com.zwei.iot.alarm.mapper.AlarmRecordMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AlarmRecordServiceImpl 单元测试 — 覆盖告警创建去重、状态流转、批量处置。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AlarmRecordServiceImpl")
class AlarmRecordServiceImplTest {

    @Mock
    private AlarmRecordMapper recordMapper;
    @Mock
    private AlarmRecordLogMapper logMapper;

    private AlarmRecordServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AlarmRecordServiceImpl(recordMapper, logMapper);
    }

    // ──────────── createOrUpdateAlarm ────────────

    @Nested
    @DisplayName("createOrUpdateAlarm")
    class CreateOrUpdate {

        @Test
        @DisplayName("新阈值告警 → 创建记录, status=1(待处理), 记录日志")
        void newThresholdAlarm() {
            when(recordMapper.selectActiveByCriteria(1L, 100L)).thenReturn(null);
            doAnswer(inv -> {
                AlarmRecord r = inv.getArgument(0);
                r.setId(500L);
                return 1;
            }).when(recordMapper).insertRecord(any(AlarmRecord.class));
            when(logMapper.insertLog(any(AlarmRecordLog.class))).thenReturn(1);

            AlarmRecord input = AlarmRecord.builder()
                    .hazardPointId(100L).hazardPointName("测试隐患点")
                    .criteriaId(1L)
                    .alarmLevel(3).alarmLevelText("橙色")
                    .alarmType("THRESHOLD")
                    .alarmMessage("test")
                    .currentValue(new BigDecimal("15.5"))
                    .createBy("SYSTEM").createTime(new Date())
                    .build();

            AlarmRecord result = service.createOrUpdateAlarm(input);

            assertThat(result.getId()).isEqualTo(500L);
            assertThat(result.getStatus()).isEqualTo(1);
            assertThat(result.getStatusName()).isEqualTo("待处理");
            assertThat(result.getTriggerCount()).isEqualTo(1);

            ArgumentCaptor<AlarmRecordLog> logCaptor = ArgumentCaptor.forClass(AlarmRecordLog.class);
            verify(logMapper).insertLog(logCaptor.capture());
            assertThat(logCaptor.getValue().getToStatus()).isEqualTo(1);
            assertThat(logCaptor.getValue().getAlarmId()).isEqualTo(500L);
        }

        @Test
        @DisplayName("已存在活动阈值告警 → 不创建新记录, 更新触发次数")
        void existingThresholdAlarmUpdatesCount() {
            AlarmRecord existing = AlarmRecord.builder()
                    .id(500L).hazardPointId(100L)
                    .criteriaId(1L)
                    .status(1).statusName("待处理")
                    .triggerCount(3)
                    .build();
            when(recordMapper.selectActiveByCriteria(1L, 100L)).thenReturn(existing);
            when(recordMapper.updateTriggerCount(eq(500L), anyString(), eq(4))).thenReturn(1);

            AlarmRecord input = AlarmRecord.builder()
                    .hazardPointId(100L).criteriaId(1L)
                    .alarmLevel(3).alarmType("THRESHOLD")
                    .createBy("SYSTEM").createTime(new Date())
                    .build();

            AlarmRecord result = service.createOrUpdateAlarm(input);

            assertThat(result.getId()).isEqualTo(500L);
            verify(recordMapper, never()).insertRecord(any());
        }

        @Test
        @DisplayName("新综合告警(strategyId) → 用 selectActiveByStrategy 去重")
        void newComprehensiveAlarmStrategyDedup() {
            when(recordMapper.selectActiveByStrategy(2L, 100L)).thenReturn(null);
            doAnswer(inv -> {
                AlarmRecord r = inv.getArgument(0);
                r.setId(600L);
                return 1;
            }).when(recordMapper).insertRecord(any(AlarmRecord.class));
            when(logMapper.insertLog(any())).thenReturn(1);

            AlarmRecord input = AlarmRecord.builder()
                    .hazardPointId(100L)
                    .strategyId(2L)  // no criteriaId, uses strategyId path
                    .alarmLevel(2).alarmType("COMPREHENSIVE")
                    .alarmMessage("综合策略")
                    .createBy("SYSTEM").createTime(new Date())
                    .build();

            AlarmRecord result = service.createOrUpdateAlarm(input);

            assertThat(result.getId()).isEqualTo(600L);
            verify(recordMapper).selectActiveByStrategy(2L, 100L);
            verify(recordMapper, never()).selectActiveByCriteria(anyLong(), anyLong());
        }

        @Test
        @DisplayName("已存在综合告警 → 更新计数不创建新记录")
        void existingComprehensiveUpdatesCount() {
            AlarmRecord existing = AlarmRecord.builder()
                    .id(600L).hazardPointId(100L)
                    .strategyId(2L)
                    .status(1).triggerCount(1)
                    .build();
            when(recordMapper.selectActiveByStrategy(2L, 100L)).thenReturn(existing);
            when(recordMapper.updateTriggerCount(eq(600L), anyString(), eq(2))).thenReturn(1);

            AlarmRecord input = AlarmRecord.builder()
                    .hazardPointId(100L).strategyId(2L)
                    .alarmLevel(2).alarmType("COMPREHENSIVE")
                    .createBy("SYSTEM").createTime(new Date())
                    .build();

            AlarmRecord result = service.createOrUpdateAlarm(input);
            assertThat(result.getId()).isEqualTo(600L);
            verify(recordMapper, never()).insertRecord(any());
        }
    }

    // ──────────── dispose (status transition) ────────────

    @Nested
    @DisplayName("dispose 状态流转")
    class Dispose {

        @Test
        @DisplayName("待处理→处理中: 状态更新 + 日志记录")
        void pendingToProcessing() {
            AlarmRecord record = AlarmRecord.builder()
                    .id(1L).status(1).statusName("待处理").build();
            when(recordMapper.selectRecordById(1L)).thenReturn(record);
            when(recordMapper.updateStatus(eq(1L), eq(2), eq("处理中"), eq("admin"), anyString(), anyString())).thenReturn(1);
            when(logMapper.insertLog(any(AlarmRecordLog.class))).thenReturn(1);

            int rows = service.dispose(1L, 2, "开始处置", "admin");

            assertThat(rows).isEqualTo(1);
            ArgumentCaptor<AlarmRecordLog> logCaptor = ArgumentCaptor.forClass(AlarmRecordLog.class);
            verify(logMapper).insertLog(logCaptor.capture());
            assertThat(logCaptor.getValue().getFromStatus()).isEqualTo(1);
            assertThat(logCaptor.getValue().getToStatus()).isEqualTo(2);
            assertThat(logCaptor.getValue().getOperator()).isEqualTo("admin");
        }

        @Test
        @DisplayName("记录不存在时返回 0")
        void recordNotFound() {
            when(recordMapper.selectRecordById(99L)).thenReturn(null);

            assertThat(service.dispose(99L, 2, "note", "admin")).isZero();
            verify(recordMapper, never()).updateStatus(anyLong(), anyInt(), anyString(), anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("处置: 待处理→已销警")
        void pendingToResolved() {
            AlarmRecord record = AlarmRecord.builder().id(1L).status(1).build();
            when(recordMapper.selectRecordById(1L)).thenReturn(record);
            when(recordMapper.updateStatus(eq(1L), eq(3), eq("已销警"), eq("admin"), anyString(), anyString())).thenReturn(1);
            when(logMapper.insertLog(any())).thenReturn(1);

            int rows = service.dispose(1L, 3, "问题已处理", "admin");
            assertThat(rows).isEqualTo(1);
        }

        @Test
        @DisplayName("处置: 待处理→误报")
        void pendingToFalseAlarm() {
            AlarmRecord record = AlarmRecord.builder().id(1L).status(1).build();
            when(recordMapper.selectRecordById(1L)).thenReturn(record);
            when(recordMapper.updateStatus(eq(1L), eq(4), eq("误报"), eq("admin"), anyString(), anyString())).thenReturn(1);
            when(logMapper.insertLog(any())).thenReturn(1);

            int rows = service.dispose(1L, 4, "传感器故障误触发", "admin");
            assertThat(rows).isEqualTo(1);
        }
    }

    // ──────────── batchDispose ────────────

    @Nested
    @DisplayName("batchDispose 批量处置")
    class BatchDispose {

        @Test
        @DisplayName("批量销警: 更新记录 + 批量日志")
        void batchResolve() {
            Long[] ids = {1L, 2L, 3L};
            when(recordMapper.batchUpdateStatus(eq(ids), eq(3), eq("已销警"), eq("admin"), anyString())).thenReturn(3);
            when(logMapper.batchInsertLogs(anyList())).thenReturn(3);

            int rows = service.batchDispose(ids, 3, "admin");
            assertThat(rows).isEqualTo(3);
            verify(logMapper).batchInsertLogs(argThat(list -> list.size() == 3));
        }

        @Test
        @DisplayName("空 ID 数组返回 0")
        void emptyIds() {
            assertThat(service.batchDispose(new Long[0], 3, "admin")).isZero();
            verify(recordMapper, never()).batchUpdateStatus(any(), anyInt(), anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("null ID 数组返回 0")
        void nullIds() {
            assertThat(service.batchDispose(null, 3, "admin")).isZero();
        }
    }

    // ──────────── status name resolution ────────────

    @Test
    @DisplayName("resolveStatusName: null → 待处理")
    void nullStatus() {
        assertThat(service.dispose(1L, null, "", "admin")).isZero();
    }
}
