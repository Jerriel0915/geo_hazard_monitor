package com.zwei.iot.report.render;

import com.zwei.iot.device.domain.brief.AlarmSummary;
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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MonthlyReportRenderer")
class MonthlyReportRendererTest {

    private final MonthlyReportRenderer renderer = new MonthlyReportRenderer();

    private ReportContext ctx(AlarmSummary summary) {
        return new ReportContext(
            ReportType.MONTHLY,
            new ReportPeriod(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30)),
            new HazardPointBrief(1L, "HP001", "测试", new BigDecimal("104"), new BigDecimal("30")),
            LocalDateTime.of(2026, 7, 1, 2, 30),
            List.of(), 0, 0, 0, 0.0,
            List.of(new MetricRow(10L, "S001", "disp_x", "X方向位移", "mm", 1.5, 3.2, 0.1, 1.8, 2.5, 99.5)),
            summary, List.of(),
            null, null, null
        );
    }

    @Test
    @DisplayName("type() 返回 MONTHLY")
    void type() { assertThat(renderer.type()).isEqualTo(ReportType.MONTHLY); }

    @Test
    @DisplayName("渲染包含月报标题与章节")
    void renderHasTitleAndSections() {
        String html = renderer.render(ctx(
            new AlarmSummary(1L, 0, 0, 0, Map.of(), Map.of())));
        assertThat(html).contains("地质灾害监测月报");
        assertThat(html).contains("设备运行汇总");
        assertThat(html).contains("监测数据汇总");
        assertThat(html).contains("风险情况");
        assertThat(html).contains("关键事件");
        assertThat(html).contains("分析与建议");
    }

    @Test
    @DisplayName("风险章节按级别统计次数, 最高级别标色")
    void riskByLevel() {
        String html = renderer.render(ctx(
            new AlarmSummary(1L, 5, 3, 2,
                Map.of(1, 2, 2, 1, 3, 2),
                Map.of(1, 2, 3, 3))));
        assertThat(html).contains("蓝色");
        assertThat(html).contains("黄色");
        assertThat(html).contains("橙色");
        assertThat(html).contains("#fa8c16");
    }

    @Test
    @DisplayName("无告警时风险章节显示本月无告警")
    void noAlarm() {
        String html = renderer.render(ctx(
            new AlarmSummary(1L, 0, 0, 0, Map.of(), Map.of())));
        assertThat(html.contains("本月无告警") || html.contains("无告警")).isTrue();
    }
}
