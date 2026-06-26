package com.zwei.iot.timeseries.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MonitorQueryProperties 降采样间隔计算")
class MonitorQueryPropertiesTest {

    private final MonitorQueryProperties props = new MonitorQueryProperties();

    @Test
    @DisplayName("短区间(30s, 目标2000点) → 1s")
    void computeInterval_shortRange_1s() {
        assertThat(props.computeDownsampleInterval(30_000L, 2000)).isEqualTo("1s");
    }

    @Test
    @DisplayName("7天区间(目标2000点) → 10m (桶宽5.04min,取≥的最小nice间隔)")
    void computeInterval_7days_10m() {
        long sevenDays = 7L * 24 * 60 * 60 * 1000;
        assertThat(props.computeDownsampleInterval(sevenDays, 2000)).isEqualTo("10m");
    }

    @Test
    @DisplayName("1年区间(目标2000点) → 6h (桶宽4.38h,取≥的最小nice间隔)")
    void computeInterval_1year_6h() {
        long oneYear = 365L * 24 * 60 * 60 * 1000;
        assertThat(props.computeDownsampleInterval(oneYear, 2000)).isEqualTo("6h");
    }

    @Test
    @DisplayName("1小时区间(目标2000点) → 1m")
    void computeInterval_1hour_1m() {
        long oneHour = 60 * 60 * 1000L;
        assertThat(props.computeDownsampleInterval(oneHour, 2000)).isEqualTo("1m");
    }

    @Test
    @DisplayName("targetPoints <= 0 时回退到 maxChartPoints 默认值")
    void computeInterval_zeroTarget_fallsBackToDefault() {
        long oneHour = 60 * 60 * 1000L;
        assertThat(props.computeDownsampleInterval(oneHour, 0)).isEqualTo("1m");
    }
}
