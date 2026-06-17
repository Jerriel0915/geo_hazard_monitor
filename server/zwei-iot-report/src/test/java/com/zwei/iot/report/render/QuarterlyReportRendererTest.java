package com.zwei.iot.report.render;

import com.zwei.iot.device.domain.brief.AlarmSummary;
import com.zwei.iot.device.domain.brief.HazardPointBrief;
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

@DisplayName("QuarterlyReportRenderer")
class QuarterlyReportRendererTest {

    private final QuarterlyReportRenderer renderer = new QuarterlyReportRenderer();

    private ReportContext ctx(Map<String, Integer> monthly, Map<String, String> trends) {
        return new ReportContext(
            ReportType.QUARTERLY,
            new ReportPeriod(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 6, 30)),
            new HazardPointBrief(1L, "HP001", "测试", new BigDecimal("104"), new BigDecimal("30")),
            LocalDateTime.of(2026, 7, 1, 3, 0),
            List.of(), 5, 5, 0, 100.0,
            List.of(),
            new AlarmSummary(1L, 10, 3, 2, Map.of(1, 5, 3, 5), Map.of(1, 2, 3, 8)),
            List.of(),
            trends,
            Map.of("disp_x", 0.5),
            monthly
        );
    }

    @Test
    @DisplayName("type() 返回 QUARTERLY")
    void type() { assertThat(renderer.type()).isEqualTo(ReportType.QUARTERLY); }

    @Test
    @DisplayName("渲染包含季报标题与章节")
    void renderSections() {
        String html = renderer.render(ctx(
            Map.of("2026-04", 3, "2026-05", 5, "2026-06", 2),
            Map.of("disp_x", "UP", "rainfall", "STABLE")));
        assertThat(html).contains("地质灾害监测季报");
        assertThat(html).contains("季度风险总览");
        assertThat(html).contains("趋势分析");
        assertThat(html).contains("告警分布");
        assertThat(html).contains("设备运行汇总");
        assertThat(html).contains("风险评估与建议");
    }

    @Test
    @DisplayName("月度告警分布按月渲染")
    void monthlyAlarmDistribution() {
        String html = renderer.render(ctx(
            Map.of("2026-04", 3, "2026-05", 5, "2026-06", 2),
            Map.of()));
        assertThat(html).contains("2026-04");
        assertThat(html).contains("2026-05");
        assertThat(html).contains("2026-06");
    }

    @Test
    @DisplayName("趋势方向用 ↑↓→ 符号")
    void trendSymbols() {
        String html = renderer.render(ctx(Map.of(),
            Map.of("disp_x", "UP", "rainfall", "DOWN", "tilt", "STABLE")));
        assertThat(html).contains("↑");
        assertThat(html).contains("↓");
        assertThat(html).contains("→");
    }

    @Test
    @DisplayName("风险评估章节包含评级文字")
    void riskLevelShown() {
        String html = renderer.render(ctx(Map.of(),
            Map.of("a", "UP", "b", "UP", "c", "UP")));
        assertThat(html).contains("评级");
        assertThat(html).contains("风险");
    }
}
