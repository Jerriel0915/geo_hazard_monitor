package com.zwei.iot.alarm.service.impl;

import com.zwei.iot.alarm.domain.ActionType;
import com.zwei.iot.alarm.domain.AlarmRecord;
import com.zwei.iot.alarm.domain.AlarmRecordActionLog;
import com.zwei.iot.alarm.domain.AlarmRecordTriggerDetail;
import com.zwei.iot.alarm.mapper.AlarmRecordActionLogMapper;
import com.zwei.iot.alarm.mapper.AlarmRecordMapper;
import com.zwei.iot.alarm.mapper.AlarmRecordTriggerDetailMapper;
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
 * AlarmRecordServiceImpl 单元测试 — 覆盖三分支 + dispose/batchDispose + action_log。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AlarmRecordServiceImpl")
class AlarmRecordServiceImplTest {

    @Mock private AlarmRecordMapper recordMapper;
    @Mock private AlarmRecordActionLogMapper actionLogMapper;
    @Mock private AlarmRecordTriggerDetailMapper triggerDetailMapper;

    private AlarmRecordServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AlarmRecordServiceImpl(recordMapper, actionLogMapper, triggerDetailMapper);
    }

    // ──────────── createOrUpdateAlarm 三分支 ────────────

    @Nested
    @DisplayName("createOrUpdateAlarm")
    class CreateOrUpdate {

        @Test
        @DisplayName("新建 → status=1 + 写 CREATE 日志 + 写触发明细")
        void newAlarmWritesCreateLog() {
            when(recordMapper.selectActiveByCriteria(1L, 100L)).thenReturn(null);
            doAnswer(inv -> { inv.<AlarmRecord>getArgument(0).setId(500L); return 1; })
                    .when(recordMapper).insertRecord(any(AlarmRecord.class));

            AlarmRecord input = AlarmRecord.builder()
                    .hazardPointId(100L).hazardPointName("测试")
                    .criteriaId(1L)
                    .alarmLevel(3).alarmLevelText("橙色")
                    .alarmType("THRESHOLD").alarmMessage("test")
                    .currentValue(new BigDecimal("15.5"))
                    .createBy("SYSTEM").createTime(new Date())
                    .build();

            AlarmRecord result = service.createOrUpdateAlarm(input);

            assertThat(result.getId()).isEqualTo(500L);
            assertThat(result.getStatus()).isEqualTo(1);

            // CREATE 日志断言：to_value="1"，action_type=CREATE
            ArgumentCaptor<AlarmRecordActionLog> logCaptor = ArgumentCaptor.forClass(AlarmRecordActionLog.class);
            verify(actionLogMapper).insertLog(logCaptor.capture());
            assertThat(logCaptor.getValue().getActionType()).isEqualTo(ActionType.CREATE.name());
            assertThat(logCaptor.getValue().getToValue()).isEqualTo("1");

            // 触发明细断言
            verify(triggerDetailMapper).insertDetail(argThat(d -> d.getAlarmLevel() == 3));
        }

        @Test
        @DisplayName("再次触发同级 → 更新 triggerCount + 写 RE_TRIGGER 日志（不写 LEVEL_CHANGE）")
        void reTriggerSameLevelWritesOnlyReTriggerLog() {
            AlarmRecord existing = AlarmRecord.builder()
                    .id(500L).criteriaId(1L).hazardPointId(100L)
                    .status(1).triggerCount(3).alarmLevel(3)
                    .build();
            when(recordMapper.selectActiveByCriteria(1L, 100L)).thenReturn(existing);
            when(recordMapper.updateTriggerCount(eq(500L), anyString(), eq(4))).thenReturn(1);

            AlarmRecord input = AlarmRecord.builder()
                    .hazardPointId(100L).criteriaId(1L)
                    .alarmLevel(3)  // 同级
                    .alarmType("THRESHOLD").createBy("SYSTEM").createTime(new Date())
                    .build();

            service.createOrUpdateAlarm(input);

            verify(recordMapper, never()).updateAlarmLevel(anyLong(), anyInt(), anyString(), anyString(), anyInt());
            // 仅 1 条 RE_TRIGGER，无 LEVEL_CHANGE
            verify(actionLogMapper, times(1)).insertLog(argThat(l ->
                    ActionType.RE_TRIGGER.name().equals(l.getActionType())));
            verify(actionLogMapper, never()).insertLog(argThat(l ->
                    ActionType.LEVEL_CHANGE.name().equals(l.getActionType())));
        }

        @Test
        @DisplayName("再次触发等级变化(3→4) → 更新主表 alarmLevel + 写 RE_TRIGGER + LEVEL_CHANGE 两条日志")
        void reTriggerLevelChangeWritesTwoLogs() {
            AlarmRecord existing = AlarmRecord.builder()
                    .id(500L).criteriaId(1L).hazardPointId(100L)
                    .status(1).triggerCount(3).alarmLevel(3)
                    .build();
            when(recordMapper.selectActiveByCriteria(1L, 100L)).thenReturn(existing);
            when(recordMapper.updateAlarmLevel(eq(500L), eq(4), eq("红色"), anyString(), eq(4))).thenReturn(1);

            AlarmRecord input = AlarmRecord.builder()
                    .hazardPointId(100L).criteriaId(1L)
                    .alarmLevel(4)  // 等级变化
                    .alarmType("THRESHOLD").createBy("SYSTEM").createTime(new Date())
                    .build();

            service.createOrUpdateAlarm(input);

            // 更新主表 alarmLevel
            verify(recordMapper).updateAlarmLevel(eq(500L), eq(4), eq("红色"), anyString(), eq(4));
            verify(recordMapper, never()).updateTriggerCount(anyLong(), anyString(), anyInt());

            // 两条日志：RE_TRIGGER + LEVEL_CHANGE，先捕获再断言
            ArgumentCaptor<AlarmRecordActionLog> logCaptor = ArgumentCaptor.forClass(AlarmRecordActionLog.class);
            verify(actionLogMapper, times(2)).insertLog(logCaptor.capture());
            List<AlarmRecordActionLog> captured = logCaptor.getAllValues();
            assertThat(captured).extracting(AlarmRecordActionLog::getActionType)
                    .containsExactlyInAnyOrder(ActionType.RE_TRIGGER.name(), ActionType.LEVEL_CHANGE.name());

            AlarmRecordActionLog levelChangeLog = captured.stream()
                    .filter(l -> ActionType.LEVEL_CHANGE.name().equals(l.getActionType()))
                    .findFirst().orElseThrow();
            assertThat(levelChangeLog.getFromValue()).isEqualTo("3");
            assertThat(levelChangeLog.getToValue()).isEqualTo("4");

            // 触发明细等级 = 新等级 4
            verify(triggerDetailMapper).insertDetail(argThat(d -> d.getAlarmLevel() == 4));
        }
    }

    // ──────────── dispose ────────────

    @Nested
    @DisplayName("dispose 状态流转")
    class Dispose {

        @Test
        @DisplayName("待处理→处理中(status=2) → 写 FEEDBACK 日志")
        void pendingToFeedbackWritesFeedbackLog() {
            AlarmRecord record = AlarmRecord.builder().id(1L).status(1).build();
            when(recordMapper.selectRecordById(1L)).thenReturn(record);
            when(recordMapper.updateStatus(eq(1L), eq(2), eq("处理中"), eq("admin"), anyString(), any())).thenReturn(1);

            int rows = service.dispose(1L, 2, "现场已派员", "a.txt,b.txt", "派员核查", "admin");

            assertThat(rows).isEqualTo(1);
            ArgumentCaptor<AlarmRecordActionLog> logCaptor = ArgumentCaptor.forClass(AlarmRecordActionLog.class);
            verify(actionLogMapper).insertLog(logCaptor.capture());
            assertThat(logCaptor.getValue().getActionType()).isEqualTo(ActionType.FEEDBACK.name());
            assertThat(logCaptor.getValue().getToValue()).isEqualTo("2");
            assertThat(logCaptor.getValue().getDescription()).isEqualTo("现场已派员");
            assertThat(logCaptor.getValue().getAttachments()).isEqualTo("a.txt,b.txt");
            assertThat(logCaptor.getValue().getRemarks()).isEqualTo("派员核查");
        }

        @Test
        @DisplayName("销警(status=3) → 写 DISPOSE_CLOSE 日志")
        void disposeClose() {
            AlarmRecord record = AlarmRecord.builder().id(1L).status(1).build();
            when(recordMapper.selectRecordById(1L)).thenReturn(record);
            when(recordMapper.updateStatus(eq(1L), eq(3), eq("已销警"), eq("admin"), anyString(), any())).thenReturn(1);

            service.dispose(1L, 3, null, null, "解除", "admin");

            ArgumentCaptor<AlarmRecordActionLog> logCaptor = ArgumentCaptor.forClass(AlarmRecordActionLog.class);
            verify(actionLogMapper).insertLog(logCaptor.capture());
            assertThat(logCaptor.getValue().getActionType()).isEqualTo(ActionType.DISPOSE_CLOSE.name());
            assertThat(logCaptor.getValue().getToValue()).isEqualTo("3");
        }

        @Test
        @DisplayName("误报(status=4) → 写 DISPOSE_FALSE_ALARM 日志")
        void disposeFalseAlarm() {
            AlarmRecord record = AlarmRecord.builder().id(1L).status(1).build();
            when(recordMapper.selectRecordById(1L)).thenReturn(record);
            when(recordMapper.updateStatus(eq(1L), eq(4), eq("误报"), eq("admin"), anyString(), any())).thenReturn(1);

            service.dispose(1L, 4, null, null, "传感器故障", "admin");

            verify(actionLogMapper).insertLog(argThat(l ->
                    ActionType.DISPOSE_FALSE_ALARM.name().equals(l.getActionType())));
        }

        @Test
        @DisplayName("记录不存在返回 0")
        void recordNotFound() {
            when(recordMapper.selectRecordById(99L)).thenReturn(null);
            assertThat(service.dispose(99L, 2, null, null, null, "admin")).isZero();
        }
    }

    // ──────────── batchDispose ────────────

    @Nested
    @DisplayName("batchDispose 批量处置")
    class BatchDispose {

        @Test
        @DisplayName("批量销警：逐条写 DISPOSE_CLOSE 日志")
        void batchClose() {
            Long[] ids = {1L, 2L, 3L};
            when(recordMapper.batchUpdateStatus(eq(ids), eq(3), eq("已销警"), eq("admin"), anyString())).thenReturn(3);

            int rows = service.batchDispose(ids, 3, null, null, "批量销警", "admin");
            assertThat(rows).isEqualTo(3);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<AlarmRecordActionLog>> captor = ArgumentCaptor.forClass(List.class);
            verify(actionLogMapper).batchInsertLogs(captor.capture());
            assertThat(captor.getValue()).hasSize(3);
            assertThat(captor.getValue()).allSatisfy(l -> {
                assertThat(l.getActionType()).isEqualTo(ActionType.DISPOSE_CLOSE.name());
                assertThat(l.getToValue()).isEqualTo("3");
            });
        }

        @Test
        @DisplayName("空数组返回 0")
        void emptyIds() {
            assertThat(service.batchDispose(new Long[0], 3, null, null, null, "admin")).isZero();
        }
    }

    // ──────────── 查询方法 ────────────

    @Test
    @DisplayName("selectActionLogsByAlarmRecordId 委托 mapper")
    void selectActionLogs() {
        when(actionLogMapper.selectLogsByAlarmRecordId(1L)).thenReturn(List.of());
        service.selectActionLogsByAlarmRecordId(1L);
        verify(actionLogMapper).selectLogsByAlarmRecordId(1L);
    }

    @Test
    @DisplayName("selectTriggerDetailsByAlarmRecordId 委托 mapper")
    void selectTriggerDetails() {
        when(triggerDetailMapper.selectByAlarmRecordId(1L)).thenReturn(List.of());
        service.selectTriggerDetailsByAlarmRecordId(1L);
        verify(triggerDetailMapper).selectByAlarmRecordId(1L);
    }
}
