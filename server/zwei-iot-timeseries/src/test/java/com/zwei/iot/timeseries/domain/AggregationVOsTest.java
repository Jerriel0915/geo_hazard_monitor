package com.zwei.iot.timeseries.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Aggregation / Sensor / Completeness / Trend VOs")
class AggregationVOsTest {

    @Test
    @DisplayName("AggregationResultVO 持有指标与 metrics 字典")
    void aggregationResultVO() {
        AggregationResultVO vo = new AggregationResultVO(
                1L, "rain_01", "rainfall", "雨量", "mm",
                1700000000000L, Map.of("AVG", 12.5, "MAX", 30.0));
        assertThat(vo.deviceId()).isEqualTo(1L);
        assertThat(vo.sensorCode()).isEqualTo("rain_01");
        assertThat(vo.attrCode()).isEqualTo("rainfall");
        assertThat(vo.metrics()).containsEntry("AVG", 12.5).containsEntry("MAX", 30.0);
    }

    @Test
    @DisplayName("SensorAggregationVO 持有 sensor 元数据 + 结果列表")
    void sensorAggregationVO() {
        AggregationResultVO inner = new AggregationResultVO(
                1L, "rain_01", "rainfall", "雨量", "mm", 0L, Map.of());
        SensorAggregationVO vo = new SensorAggregationVO(1L, "rain_01", "雨量计", List.of(inner));
        assertThat(vo.results()).hasSize(1);
        assertThat(vo.sensorName()).isEqualTo("雨量计");
    }

    @Test
    @DisplayName("CompletenessReportVO 持有完整度统计")
    void completenessReportVO() {
        CompletenessReportVO vo = new CompletenessReportVO(
                1L, "rain_01", "rainfall", 100L, 80L, 0.8, 0.2, 1700000000000L);
        assertThat(vo.expectedPoints()).isEqualTo(100L);
        assertThat(vo.actualPoints()).isEqualTo(80L);
        assertThat(vo.completenessRate()).isEqualTo(0.8);
    }

    @Test
    @DisplayName("TrendReportVO 持有趋势/变化率")
    void trendReportVO() {
        TrendReportVO vo = new TrendReportVO(
                1L, "rain_01", "rainfall",
                1700000000000L, 1700003600000L,
                1.0e-7, 0.36, 8.64, 0.0, 1.0, "rising");
        assertThat(vo.slopePerMs()).isEqualTo(1.0e-7);
        assertThat(vo.ratePerHour()).isEqualTo(0.36);
        assertThat(vo.trendDirection()).isEqualTo("rising");
    }
}
