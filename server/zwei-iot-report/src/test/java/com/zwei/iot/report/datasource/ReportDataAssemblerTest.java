package com.zwei.iot.report.datasource;

import com.zwei.iot.device.domain.DeviceSensor;
import com.zwei.iot.device.domain.brief.AlarmSummary;
import com.zwei.iot.device.domain.brief.DeviceBrief;
import com.zwei.iot.device.domain.brief.HazardPointBrief;
import com.zwei.iot.device.service.IAlarmQueryService;
import com.zwei.iot.device.service.IDeviceHazardRelationService;
import com.zwei.iot.device.service.IDeviceSensorService;
import com.zwei.iot.report.domain.ReportType;
import com.zwei.iot.report.support.ReportPeriod;
import com.zwei.iot.timeseries.service.IotdbTimeSeriesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("ReportDataAssembler")
class ReportDataAssemblerTest {

    private IDeviceHazardRelationService deviceRelation;
    private IDeviceSensorService sensorService;
    private IotdbTimeSeriesService timeSeries;
    private IAlarmQueryService alarmQuery;
    private ReportDataAssembler assembler;

    @BeforeEach
    void setUp() {
        deviceRelation = mock(IDeviceHazardRelationService.class);
        sensorService = mock(IDeviceSensorService.class);
        timeSeries = mock(IotdbTimeSeriesService.class);
        alarmQuery = mock(IAlarmQueryService.class);
        assembler = new ReportDataAssembler(deviceRelation, sensorService, timeSeries, alarmQuery);
    }

    @Test
    @DisplayName("周报: 设备在线率正确计算, 不查告警")
    void weeklyDevicesAndNoAlarm() {
        HazardPointBrief hp = new HazardPointBrief(
                1L, "HP001", "测试隐患点", new BigDecimal("104"), new BigDecimal("30"));

        // DeviceBrief 是 7 字段 (id, code, name, deviceType, sensorCount, onlineStatus, lastReportAt)
        when(deviceRelation.getDevicesByHazardPoint(1L)).thenReturn(List.of(
                new DeviceBrief(10L, "D1", "设备1", 1, 2, 1, null),
                new DeviceBrief(11L, "D2", "设备2", 2, 1, 0, null),
                new DeviceBrief(12L, "D3", "设备3", 3, 1, 1, null)
        ));
        when(sensorService.selectSensorListByDeviceId(anyLong())).thenReturn(List.of());

        ReportContext ctx = assembler.build(
                ReportType.WEEKLY,
                new ReportPeriod(LocalDate.of(2026, 6, 8), LocalDate.of(2026, 6, 14)),
                hp);

        assertThat(ctx.deviceTotal()).isEqualTo(3);
        assertThat(ctx.deviceOnline()).isEqualTo(2);
        assertThat(ctx.deviceOffline()).isEqualTo(1);
        assertThat(ctx.onlineRatePct()).isCloseTo(66.67, within(0.01));
        assertThat(ctx.alarmSummary()).isNull();
        assertThat(ctx.alarmTopEvents()).isNull();
        verifyNoInteractions(alarmQuery);
    }

    @Test
    @DisplayName("月报: 查询告警摘要与 Top 事件")
    void monthlyTriggersAlarmQuery() {
        HazardPointBrief hp = new HazardPointBrief(
                1L, "HP001", "测试", new BigDecimal("104"), new BigDecimal("30"));

        when(deviceRelation.getDevicesByHazardPoint(1L)).thenReturn(List.of());
        when(sensorService.selectSensorListByDeviceId(anyLong())).thenReturn(List.of());

        // AlarmSummary: (hazardPointId, total, maxLevel, pendingCount, levelCount, statusCount)
        // statusCount 键是 Integer
        AlarmSummary summary = new AlarmSummary(1L, 5, 3, 2,
                java.util.Map.of(), java.util.Map.of());
        when(alarmQuery.summarizeByHazardPoint(eq(1L), any(), any())).thenReturn(summary);
        when(alarmQuery.listTopByHazardPoint(eq(1L), any(), any(), eq(10))).thenReturn(List.of());

        ReportContext ctx = assembler.build(
                ReportType.MONTHLY,
                new ReportPeriod(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30)),
                hp);

        assertThat(ctx.alarmSummary()).isEqualTo(summary);
        verify(alarmQuery).summarizeByHazardPoint(eq(1L), any(), any());
        verify(alarmQuery).listTopByHazardPoint(eq(1L), any(), any(), eq(10));
    }

    @Test
    @DisplayName("季报: 查询月度告警分布")
    void quarterlyTriggersMonthlyCount() {
        HazardPointBrief hp = new HazardPointBrief(
                1L, "HP001", "测试", new BigDecimal("104"), new BigDecimal("30"));

        when(deviceRelation.getDevicesByHazardPoint(1L)).thenReturn(List.of());
        when(sensorService.selectSensorListByDeviceId(anyLong())).thenReturn(List.of());
        when(alarmQuery.summarizeByHazardPoint(anyLong(), any(), any()))
                .thenReturn(new AlarmSummary(1L, 0, 0, 0,
                        java.util.Map.of(), java.util.Map.of()));
        when(alarmQuery.countByMonth(eq(1L), any(), any()))
                .thenReturn(java.util.Map.of("2026-04", 3, "2026-05", 5, "2026-06", 2));

        ReportContext ctx = assembler.build(
                ReportType.QUARTERLY,
                new ReportPeriod(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 6, 30)),
                hp);

        assertThat(ctx.alarmMonthlyCount()).containsEntry("2026-05", 5);
        assertThat(ctx.alarmMonthlyCount()).containsEntry("2026-04", 3);
        // 趋势 maps should be non-null (even if empty due to no metrics)
        assertThat(ctx.trendDirections()).isNotNull();
        assertThat(ctx.trendSlopes()).isNotNull();
    }

    @Test
    @DisplayName("空设备列表: 不抛异常, 指标为空")
    void emptyDevicesDoesNotThrow() {
        HazardPointBrief hp = new HazardPointBrief(
                1L, "HP001", "空隐患点", new BigDecimal("104"), new BigDecimal("30"));

        when(deviceRelation.getDevicesByHazardPoint(1L)).thenReturn(List.of());

        ReportContext ctx = assembler.build(
                ReportType.WEEKLY,
                new ReportPeriod(LocalDate.of(2026, 6, 8), LocalDate.of(2026, 6, 14)),
                hp);

        assertThat(ctx.deviceTotal()).isEqualTo(0);
        assertThat(ctx.deviceOnline()).isEqualTo(0);
        assertThat(ctx.onlineRatePct()).isEqualTo(0.0);
        assertThat(ctx.metrics()).isEmpty();
    }
}
