package com.zwei.iot.report.render;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RiskAssessor 综合风险评级")
class RiskAssessorTest {

    @Test
    @DisplayName("高告警频次 + 高级别 → 极高或高")
    void highFrequencyUpIsExtreme() {
        RiskAssessor.Risk r = RiskAssessor.assess(50, 4, 2, 60.0);
        assertThat(r.level()).isIn("极高", "高");
        assertThat(r.color()).isIn("#ff4d4f", "#fa8c16");
    }

    @Test
    @DisplayName("中等告警 + 多个上升趋势 → 中或高")
    void mediumUpIsHigh() {
        RiskAssessor.Risk r = RiskAssessor.assess(15, 3, 2, 80.0);
        assertThat(r.level()).isIn("高", "极高", "中");
    }

    @Test
    @DisplayName("低告警 + 趋势稳定 → 低")
    void lowStableIsLow() {
        RiskAssessor.Risk r = RiskAssessor.assess(2, 1, 0, 95.0);
        assertThat(r.level()).isEqualTo("低");
    }

    @Test
    @DisplayName("零告警 → 低")
    void zeroAlarmIsLow() {
        RiskAssessor.Risk r = RiskAssessor.assess(0, 0, 0, 99.0);
        assertThat(r.level()).isEqualTo("低");
        assertThat(r.color()).isEqualTo("#67c23a");
    }
}
