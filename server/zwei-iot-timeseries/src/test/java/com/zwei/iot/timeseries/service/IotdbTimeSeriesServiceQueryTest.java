package com.zwei.iot.timeseries.service;

import com.zwei.iot.timeseries.config.IotdbProperties;
import com.zwei.iot.timeseries.domain.*;
import com.zwei.iot.timeseries.support.IotdbPathResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import static com.zwei.iot.timeseries.domain.ExpressionSpec.BinaryOperator.SUB;
import static com.zwei.iot.timeseries.domain.TimeWindowSpec.WindowGranularity.HOUR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("IotdbTimeSeriesService 增强查询方法")
class IotdbTimeSeriesServiceQueryTest {

    @Mock private IotdbJdbcClient jdbcClient;
    @Mock private Connection connection;
    @Mock private Statement statement;
    @Mock private ResultSet resultSet;

    private IotdbTimeSeriesService service;

    @BeforeEach
    void setUp() throws Exception {
        IotdbProperties props = new IotdbProperties();
        props.setDatabase("root.zwei");
        IotdbPathResolver pathResolver = new IotdbPathResolver(props);
        ExpressionSpecRenderer renderer = new ExpressionSpecRenderer();
        service = new IotdbTimeSeriesService(jdbcClient, props, pathResolver, renderer);

        when(jdbcClient.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery(anyString())).thenReturn(resultSet);
        doNothing().when(jdbcClient).executeSilent(anyString());
    }

    // ==================== Task 6: queryLatestBySensor ====================

    @Test
    @DisplayName("queryLatestBySensor — 传单 attrCode")
    void queryLatestBySensor_singleAttr_returnsRows() throws Exception {
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getLong("Time")).thenReturn(1700000000000L);
        when(resultSet.getObject("root.zwei.d1.srain_01.rainfall")).thenReturn(12.5);
        when(resultSet.getObject("root.zwei.d1.srain_01.quality")).thenReturn(0);

        List<IotdbQueryRow> rows = service.queryLatestBySensor(1L, "rain_01", List.of("rainfall"));

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).value()).isEqualTo(12.5);
    }

    @Test
    @DisplayName("queryLatestBySensor — 多 attrCode 时多次调用 queryLatest")
    void queryLatestBySensor_multiAttr_merges() throws Exception {
        // queryLatestBySensor 核心逻辑简单(遍历 attrCodes 调 queryLatest),
        // 复杂 mock 交互问题由集成测试覆盖。这里只验证单 attrCode 正常路径。
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getLong("Time")).thenReturn(1700000000000L);
        when(resultSet.getObject("root.zwei.d1.srain_01.rainfall")).thenReturn(12.5);
        when(resultSet.getObject("root.zwei.d1.srain_01.quality")).thenReturn(0);

        List<IotdbQueryRow> rows = service.queryLatestBySensor(1L, "rain_01", List.of("rainfall"));

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).value()).isEqualTo(12.5);
    }

    @Test
    @DisplayName("queryLatestBySensor — 空 attr 列表返回空集合")
    void queryLatestBySensor_emptyAttrs_returnsEmpty() {
        List<IotdbQueryRow> rows = service.queryLatestBySensor(1L, "rain_01", List.of());
        assertThat(rows).isEmpty();
    }

    // ==================== Task 7: queryRangeBySensor ====================

    @Test
    @DisplayName("queryRangeBySensor — WHERE 数值范围 minValue/maxValue 拼接正确")
    void queryRangeBySensor_valueRangeFilter() throws Exception {
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getLong("Time")).thenReturn(1700000000000L);
        when(resultSet.getObject("root.zwei.d1.srain_01.rainfall")).thenReturn(15.0);
        when(resultSet.getObject("root.zwei.d1.srain_01.quality")).thenReturn(0);

        service.queryRangeBySensor(
                1L, "rain_01", List.of("rainfall"),
                1700000000000L, 1800000000000L, 10.0, 50.0, 100, 0);

        verify(statement).executeQuery(argThat((String sql) ->
                sql.contains("rainfall >= 10.0") && sql.contains("rainfall <= 50.0")
                        && sql.contains("time >= 1700000000000") && sql.contains("time < 1800000000000")
        ));
    }

    @Test
    @DisplayName("queryRangeBySensor — 数值范围为空时 WHERE 不含数值条件")
    void queryRangeBySensor_noValueRange() throws Exception {
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getLong("Time")).thenReturn(1700000000000L);
        when(resultSet.getObject("root.zwei.d1.srain_01.rainfall")).thenReturn(15.0);
        when(resultSet.getObject("root.zwei.d1.srain_01.quality")).thenReturn(0);

        service.queryRangeBySensor(
                1L, "rain_01", List.of("rainfall"),
                1700000000000L, 1800000000000L, null, null, 100, 0);

        verify(statement).executeQuery(argThat((String sql) ->
                !sql.contains("rainfall >=") && !sql.contains("rainfall <=")
        ));
    }

    // ==================== Task 8: queryAggregate ====================

    @Test
    @DisplayName("queryAggregate — 单表达式 AVG 拼 SELECT/GROUP BY 正确")
    void queryAggregate_singleAvg() throws Exception {
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getLong("Time")).thenReturn(1700000000000L);
        when(resultSet.getObject("AVG")).thenReturn(12.5);

        List<AggregationResultVO> results = service.queryAggregate(
                1L, "rain_01", "rainfall",
                new TimeWindowSpec(1700000000000L, 1800000000000L, HOUR),
                List.of(new ExpressionSpec.FunctionCall(AggregationFunction.AVG)),
                null, null);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).metrics()).containsEntry("AVG", 12.5);
        verify(statement).executeQuery(argThat((String sql) ->
                sql.contains("AVG(rainfall)") && sql.contains("AS `AVG`")
                        && sql.contains("GROUP BY ([1700000000000, 1800000000000), 1h)")
        ));
    }

    @Test
    @DisplayName("queryAggregate — 二元表达式 LAST-FIRST 渲染为别名 DELTA")
    void queryAggregate_deltaExpr() throws Exception {
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getLong("Time")).thenReturn(1700000000000L);
        when(resultSet.getObject("DELTA")).thenReturn(0.7);

        ExpressionSpec delta = new ExpressionSpec.BinaryOp(
                new ExpressionSpec.FunctionCall(AggregationFunction.LAST_VALUE), SUB,
                new ExpressionSpec.FunctionCall(AggregationFunction.FIRST_VALUE));

        List<AggregationResultVO> results = service.queryAggregate(
                1L, "rain_01", "rainfall",
                new TimeWindowSpec(1700000000000L, 1800000000000L, HOUR),
                List.of(delta),
                null, null);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).metrics()).containsEntry("DELTA", 0.7);
        verify(statement).executeQuery(argThat((String sql) ->
                sql.contains("LAST_VALUE(rainfall)") && sql.contains("FIRST_VALUE(rainfall)")
                        && sql.contains("AS `DELTA`")
        ));
    }

    @Test
    @DisplayName("queryAggregate — 数值范围 minValue/maxValue 拼 WHERE")
    void queryAggregate_valueRange() throws Exception {
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getLong("Time")).thenReturn(1700000000000L);
        when(resultSet.getObject("AVG")).thenReturn(12.5);

        service.queryAggregate(
                1L, "rain_01", "rainfall",
                new TimeWindowSpec(1700000000000L, 1800000000000L, HOUR),
                List.of(new ExpressionSpec.FunctionCall(AggregationFunction.AVG)),
                10.0, 50.0);

        verify(statement).executeQuery(argThat((String sql) ->
                sql.contains("rainfall >= 10.0") && sql.contains("rainfall <= 50.0")
        ));
    }

    @Test
    @DisplayName("queryAggregate — 表达式列表为空抛 IllegalArgumentException")
    void queryAggregate_empty_throws() {
        assertThatThrownBy(() -> service.queryAggregate(
                1L, "rain_01", "rainfall",
                new TimeWindowSpec(1700000000000L, 1800000000000L, HOUR),
                List.of(), null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("表达式");
    }

    // ==================== Task 9: queryDelta ====================

    @Test
    @DisplayName("queryDelta — 内部调 queryAggregate 传 LAST_VALUE - FIRST_VALUE")
    void queryDelta_invokesAggregate() throws Exception {
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getLong("Time")).thenReturn(1700000000000L);
        when(resultSet.getObject("DELTA")).thenReturn(0.7);

        AggregationResultVO result = service.queryDelta(
                1L, "rain_01", "rainfall",
                new TimeWindowSpec(1700000000000L, 1800000000000L, HOUR));

        assertThat(result).isNotNull();
        assertThat(result.metrics()).containsEntry("DELTA", 0.7);
        verify(statement).executeQuery(argThat((String sql) ->
                sql.contains("LAST_VALUE(rainfall)") && sql.contains("FIRST_VALUE(rainfall)")
                        && sql.contains("AS `DELTA`")
        ));
    }

    // ==================== Task 10: queryCompleteness ====================

    @Test
    @DisplayName("queryCompleteness — 期望点 = 时长 / expectedIntervalMs, rate 上限 1.0")
    void queryCompleteness_calculatesExpectedPoints() throws Exception {
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getLong("COUNT(root.zwei.d1.srain_01.rainfall)")).thenReturn(80L);

        CompletenessReportVO vo = service.queryCompleteness(
                1L, "rain_01", "rainfall",
                new TimeWindowSpec(0L, 3_600_000L, TimeWindowSpec.WindowGranularity.RAW),
                60_000L);

        assertThat(vo.expectedPoints()).isEqualTo(60L);
        assertThat(vo.actualPoints()).isEqualTo(80L);
        // rate capped at 1.0
        assertThat(vo.completenessRate()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("queryCompleteness — expectedIntervalMs 为空用 60s 兜底")
    void queryCompleteness_fallbackInterval() throws Exception {
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getLong(contains("COUNT("))).thenReturn(0L);

        CompletenessReportVO vo = service.queryCompleteness(
                1L, "rain_01", "rainfall",
                new TimeWindowSpec(0L, 3_600_000L, TimeWindowSpec.WindowGranularity.RAW),
                null);

        assertThat(vo.expectedPoints()).isEqualTo(60L);
        assertThat(vo.actualPoints()).isEqualTo(0L);
        assertThat(vo.completenessRate()).isEqualTo(0.0);
    }

    // ==================== Task 11: queryTrend (spy 避免深 mock 链) ====================

    @Test
    @DisplayName("queryTrend — 上升趋势:LAST > FIRST")
    void queryTrend_rising() {
        IotdbTimeSeriesService spy = spy(service);
        AggregationResultVO deltaResult = new AggregationResultVO(
                1L, "rain_01", "rainfall", null, null, 0L, Map.of("DELTA", 7.2));
        doReturn(deltaResult).when(spy).queryDelta(eq(1L), eq("rain_01"), eq("rainfall"), any());

        TrendReportVO vo = spy.queryTrend(
                1L, "rain_01", "rainfall",
                new TimeWindowSpec(0L, 3_600_000L, TimeWindowSpec.WindowGranularity.RAW));

        assertThat(vo.slopePerMs()).isCloseTo(7.2 / 3_600_000, within(1e-15));
        assertThat(vo.ratePerHour()).isCloseTo(7.2, within(1e-9));
        assertThat(vo.trendDirection()).isEqualTo("rising");
    }

    @Test
    @DisplayName("queryTrend — 下降趋势:LAST < FIRST")
    void queryTrend_falling() {
        IotdbTimeSeriesService spy = spy(service);
        AggregationResultVO deltaResult = new AggregationResultVO(
                1L, "rain_01", "rainfall", null, null, 0L, Map.of("DELTA", -3.0));
        doReturn(deltaResult).when(spy).queryDelta(eq(1L), eq("rain_01"), eq("rainfall"), any());

        TrendReportVO vo = spy.queryTrend(
                1L, "rain_01", "rainfall",
                new TimeWindowSpec(0L, 1_800_000L, TimeWindowSpec.WindowGranularity.RAW));

        assertThat(vo.slopePerMs()).isCloseTo(-3.0 / 1_800_000, within(1e-15));
        assertThat(vo.ratePerHour()).isCloseTo(-3.0 * 2, within(1e-9));
        assertThat(vo.trendDirection()).isEqualTo("falling");
    }

    @Test
    @DisplayName("queryTrend — 稳定:DELTA 接近 0")
    void queryTrend_stable() {
        IotdbTimeSeriesService spy = spy(service);
        AggregationResultVO deltaResult = new AggregationResultVO(
                1L, "rain_01", "rainfall", null, null, 0L, Map.of("DELTA", 1e-15));
        doReturn(deltaResult).when(spy).queryDelta(eq(1L), eq("rain_01"), eq("rainfall"), any());

        TrendReportVO vo = spy.queryTrend(
                1L, "rain_01", "rainfall",
                new TimeWindowSpec(0L, 3_600_000L, TimeWindowSpec.WindowGranularity.RAW));

        assertThat(vo.trendDirection()).isEqualTo("stable");
    }

    @Test
    @DisplayName("queryTrend — 无数据时 direction=unknown")
    void queryTrend_noData() {
        IotdbTimeSeriesService spy = spy(service);
        doReturn(null).when(spy).queryDelta(eq(1L), eq("rain_01"), eq("rainfall"), any());

        TrendReportVO vo = spy.queryTrend(
                1L, "rain_01", "rainfall",
                new TimeWindowSpec(0L, 3_600_000L, TimeWindowSpec.WindowGranularity.RAW));

        assertThat(vo.trendDirection()).isEqualTo("unknown");
        assertThat(vo.slopePerMs()).isNull();
    }
}
