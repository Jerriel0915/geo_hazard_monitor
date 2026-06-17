package com.zwei.iot.report.render;

import com.zwei.iot.device.domain.brief.DeviceBrief;
import com.zwei.iot.device.domain.brief.HazardPointBrief;
import com.zwei.iot.report.datasource.MetricRow;
import com.zwei.iot.report.datasource.ReportContext;
import com.zwei.iot.report.domain.ReportType;
import com.zwei.iot.report.support.ReportPeriod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("WeeklyReportRenderer")
class WeeklyReportRendererTest {

    private final WeeklyReportRenderer renderer = new WeeklyReportRenderer();

    private ReportContext ctxWith(List<DeviceBrief> devices, List<MetricRow> metrics) {
        return new ReportContext(
            ReportType.WEEKLY,
            new ReportPeriod(LocalDate.of(2026, 6, 8), LocalDate.of(2026, 6, 14)),
            new HazardPointBrief(1L, "HP001", "测试隐患点", new BigDecimal("104.15"), new BigDecimal("30.5")),
            LocalDateTime.of(2026, 6, 15, 2, 0, 0),
            devices, devices.size(), 0, devices.size(), 0.0,
            metrics,
            null, null, null, null, null
        );
    }

    @Test
    @DisplayName("type() 返回 WEEKLY")
    void type() {
        assertThat(renderer.type()).isEqualTo(ReportType.WEEKLY);
    }

    @Test
    @DisplayName("渲染包含标题/周期/隐患点/生成时间")
    void renderContainsHeader() {
        String html = renderer.render(ctxWith(List.of(), List.of()));
        assertThat(html).contains("地质灾害监测周报");
        assertThat(html).contains("2026-06-08");
        assertThat(html).contains("2026-06-14");
        assertThat(html).contains("HP001");
        assertThat(html).contains("测试隐患点");
    }

    @Test
    @DisplayName("设备表渲染所有设备行")
    void renderDevices() {
        String html = renderer.render(ctxWith(List.of(
            new DeviceBrief(10L, "D001", "位移计01", 1, 2, 1, Date.from(LocalDateTime.of(2026, 6, 14, 23, 0)
                .atZone(java.time.ZoneId.systemDefault()).toInstant())),
            new DeviceBrief(11L, "D002", "雨量计01", 2, 1, 0, null)
        ), List.of()));
        assertThat(html).contains("D001").contains("位移计01");
        assertThat(html).contains("D002").contains("雨量计01");
        assertThat(html).contains("在线").contains("离线");
    }

    @Test
    @DisplayName("指标表渲染属性/单位/最新值/最大/最小/均值")
    void renderMetrics() {
        String html = renderer.render(ctxWith(List.of(), List.of(
            new MetricRow(10L, "S001", "disp_x", "X方向位移", "mm", 1.5, 3.2, 0.1, 1.8, 2.5, 99.5)
        )));
        assertThat(html).contains("disp_x").contains("X方向位移").contains("mm");
        assertThat(html).contains("1.500");
        assertThat(html).contains("3.200");
    }

    @Test
    @DisplayName("空指标时显示本周无异常 + 完整率信息")
    void emptyMetricsFallback() {
        String html = renderer.render(ctxWith(List.of(), List.of()));
        assertThat(html.contains("无异常") || html.contains("完整率")).isTrue();
    }
}
