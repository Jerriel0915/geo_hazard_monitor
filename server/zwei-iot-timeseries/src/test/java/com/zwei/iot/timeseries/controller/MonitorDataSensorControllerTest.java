package com.zwei.iot.timeseries.controller;

import com.zwei.common.core.domain.AjaxResult;
import com.zwei.iot.timeseries.domain.*;
import com.zwei.iot.timeseries.service.IotdbTimeSeriesService;
import com.zwei.iot.timeseries.service.MonitorDataAggregationService;
import com.zwei.iot.timeseries.service.MonitorDataAnalysisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MonitorDataSensorController")
class MonitorDataSensorControllerTest {

    @Mock private MonitorDataAggregationService aggregationService;
    @Mock private MonitorDataAnalysisService analysisService;
    @Mock private IotdbTimeSeriesService iotdbService;

    private MonitorDataSensorController controller;

    @BeforeEach
    void setUp() {
        controller = new MonitorDataSensorController(iotdbService, aggregationService, analysisService);
    }

    @Test
    @DisplayName("/latest — 返回 sensor 下所有 attr 最新值")
    void latest_ok() {
        when(aggregationService.latestBySensor(eq(1L), eq("rain_01"), any()))
                .thenReturn(Map.of("rainfall", IotdbQueryRow.builder().time(1700000000000L).value(12.5).quality(0).build()));

        AjaxResult result = controller.latest(1L, "rain_01", null);

        assertThat(result.get("code")).isEqualTo(200);
        @SuppressWarnings("unchecked")
        Map<String, IotdbQueryRow> data = (Map<String, IotdbQueryRow>) result.get("data");
        assertThat(data.get("rainfall").value()).isEqualTo(12.5);
    }

    @Test
    @DisplayName("/aggregate — POST 接表达式 body,返回聚合结果")
    void aggregate_ok() {
        SensorAggregationVO vo = new SensorAggregationVO(1L, "rain_01", "雨量计",
                List.of(new AggregationResultVO(1L, "rain_01", "rainfall", "雨量", "mm", 0L, Map.of("AVG", 12.5))));
        when(aggregationService.aggregateAllAttrs(eq(1L), eq("rain_01"), any(), any(), any(), any()))
                .thenReturn(vo);

        AjaxResult result = controller.aggregate(1L, "rain_01",
                "2024-01-01 00:00:00", "2024-01-02 00:00:00", "hour", null, null,
                List.of(new ExpressionSpec.FunctionCall(AggregationFunction.AVG)));

        assertThat(result.get("code")).isEqualTo(200);
        SensorAggregationVO data = (SensorAggregationVO) result.get("data");
        assertThat(data.results().get(0).metrics().get("AVG")).isEqualTo(12.5);
    }

    @Test
    @DisplayName("/completeness — 返回完整度报告")
    void completeness_ok() {
        CompletenessReportVO report = new CompletenessReportVO(1L, "rain_01", "rainfall", 60L, 50L, 0.83, 0.17, 1700000000000L);
        when(analysisService.completeness(eq(1L), eq("rain_01"), eq("rainfall"), any(), eq(60_000L)))
                .thenReturn(report);

        AjaxResult result = controller.completeness(1L, "rain_01", "rainfall",
                "2024-01-01 00:00:00", "2024-01-02 00:00:00", 60_000L);

        assertThat(result.get("code")).isEqualTo(200);
        CompletenessReportVO data = (CompletenessReportVO) result.get("data");
        assertThat(data.completenessRate()).isEqualTo(0.83);
    }

    @Test
    @DisplayName("/trend — 返回趋势报告")
    void trend_ok() {
        TrendReportVO report = new TrendReportVO(1L, "rain_01", "rainfall", 0L, 3_600_000L,
                1.0e-7, 0.36, 8.64, 0.0, 1.0, "rising");
        when(analysisService.trend(eq(1L), eq("rain_01"), eq("rainfall"), any())).thenReturn(report);

        AjaxResult result = controller.trend(1L, "rain_01", "rainfall",
                "2024-01-01 00:00:00", "2024-01-02 00:00:00");

        assertThat(result.get("code")).isEqualTo(200);
        TrendReportVO data = (TrendReportVO) result.get("data");
        assertThat(data.trendDirection()).isEqualTo("rising");
    }

    @Test
    @DisplayName("/range — 返回区间数据")
    void range_ok() {
        when(iotdbService.queryRangeBySensor(eq(1L), eq("rain_01"), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(Map.of("rainfall", List.of(IotdbQueryRow.builder().time(1700000000000L).value(12.5).quality(0).build())));

        AjaxResult result = controller.range(1L, "rain_01", null,
                "2024-01-01 00:00:00", "2024-01-02 00:00:00", null, null, 5000, 0);

        assertThat(result.get("code")).isEqualTo(200);
        @SuppressWarnings("unchecked")
        Map<String, List<IotdbQueryRow>> data = (Map<String, List<IotdbQueryRow>>) result.get("data");
        assertThat(data.get("rainfall").get(0).value()).isEqualTo(12.5);
    }
}
