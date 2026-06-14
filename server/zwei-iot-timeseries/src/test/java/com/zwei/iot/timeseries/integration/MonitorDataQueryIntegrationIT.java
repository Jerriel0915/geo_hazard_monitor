package com.zwei.iot.timeseries.integration;

import com.zwei.iot.timeseries.config.IotdbProperties;
import com.zwei.iot.timeseries.domain.*;
import com.zwei.iot.timeseries.service.IotdbJdbcClient;
import com.zwei.iot.timeseries.service.IotdbTimeSeriesService;
import com.zwei.iot.timeseries.support.IotdbPathResolver;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;

import static com.zwei.iot.timeseries.domain.TimeWindowSpec.WindowGranularity.HOUR;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 使用本地运行中的 IoTDB 容器做集成测试。
 * 前置条件: docker run apache/iotdb:2.0.6-standalone -p 6667:6667
 */
@Tag("integration")
@DisplayName("IoTDB 集成测试 (本地容器)")
class MonitorDataQueryIntegrationIT {

    private static final String DB = "root.zwei_it_test";
    private static final long T0 = 1_700_000_000_000L;

    private static IotdbTimeSeriesService service;

    @BeforeAll
    static void setUp() throws Exception {
        IotdbProperties props = new IotdbProperties();
        props.setHost("localhost");
        props.setPort(6667);
        props.setDatabase(DB);
        props.setUsername("root");
        props.setPassword("root");

        IotdbJdbcClient jdbcClient = new IotdbJdbcClient(props);
        IotdbPathResolver pathResolver = new IotdbPathResolver(props);
        ExpressionSpecRenderer renderer = new ExpressionSpecRenderer();
        service = new IotdbTimeSeriesService(jdbcClient, props, pathResolver, renderer);

        // 建库 + 预建 schema
        Class.forName("org.apache.iotdb.jdbc.IoTDBDriver");
        try (Connection conn = DriverManager.getConnection(
                "jdbc:iotdb://localhost:6667/", "root", "root");
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE DATABASE " + DB);
            service.createSensorSchema(1L, "rain_01", List.of("rainfall"));

            // 写入 3 个数据点: 10.0, 12.0, 18.0
            stmt.execute(String.format(
                    "INSERT INTO %s.d1.srain_01(timestamp,rainfall,quality) ALIGNED VALUES(%d,10.0,0)",
                    DB, T0));
            stmt.execute(String.format(
                    "INSERT INTO %s.d1.srain_01(timestamp,rainfall,quality) ALIGNED VALUES(%d,12.0,0)",
                    DB, T0 + 1_800_000));
            stmt.execute(String.format(
                    "INSERT INTO %s.d1.srain_01(timestamp,rainfall,quality) ALIGNED VALUES(%d,18.0,0)",
                    DB, T0 + 3_600_000));
        }
    }

    @AfterAll
    static void tearDown() throws Exception {
        try (Connection conn = DriverManager.getConnection(
                "jdbc:iotdb://localhost:6667/", "root", "root");
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP DATABASE " + DB);
        }
    }

    @Test
    @DisplayName("queryAggregate — 单 AVG 在 RAW 窗口下算得 AVG=13.33")
    void aggregate_avg() {
        TimeWindowSpec window = new TimeWindowSpec(T0, T0 + 3_600_001L, TimeWindowSpec.WindowGranularity.RAW);

        List<AggregationResultVO> results = service.queryAggregate(
                1L, "rain_01", "rainfall", window,
                List.of(new ExpressionSpec.FunctionCall(AggregationFunction.AVG)),
                null, null);

        assertThat(results).hasSize(1);
        double avg = results.get(0).metrics().get("AVG");
        assertThat(avg).isBetween(13.0, 14.0); // (10+12+18)/3 ≈ 13.33
    }

    @Test
    @DisplayName("queryAggregate — delta LAST-FIRST = 8")
    void aggregate_delta() {
        TimeWindowSpec window = new TimeWindowSpec(T0, T0 + 3_600_001L, TimeWindowSpec.WindowGranularity.RAW);

        ExpressionSpec delta = new ExpressionSpec.BinaryOp(
                new ExpressionSpec.FunctionCall(AggregationFunction.LAST_VALUE),
                ExpressionSpec.BinaryOperator.SUB,
                new ExpressionSpec.FunctionCall(AggregationFunction.FIRST_VALUE));

        List<AggregationResultVO> results = service.queryAggregate(
                1L, "rain_01", "rainfall", window, List.of(delta), null, null);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).metrics().get("DELTA")).isEqualTo(8.0);
    }

    @Test
    @DisplayName("queryTrend — 上升趋势 direction='rising'")
    void trend_rising() {
        TrendReportVO vo = service.queryTrend(1L, "rain_01", "rainfall",
                new TimeWindowSpec(T0, T0 + 3_600_001L, TimeWindowSpec.WindowGranularity.RAW));

        assertThat(vo.trendDirection()).isEqualTo("rising");
        assertThat(vo.slopePerMs()).isPositive();
    }

    @Test
    @DisplayName("queryCompleteness — 3 个数据点")
    void completeness_basic() {
        CompletenessReportVO vo = service.queryCompleteness(1L, "rain_01", "rainfall",
                new TimeWindowSpec(T0, T0 + 3_600_001L, TimeWindowSpec.WindowGranularity.RAW),
                1_800_000L);

        assertThat(vo.actualPoints()).isEqualTo(3L);
        assertThat(vo.expectedPoints()).isGreaterThanOrEqualTo(2L);
    }

    @Test
    @DisplayName("queryRangeBySensor — 区间数据 + 数值范围 WHERE")
    void range_valueFilter() {
        var result = service.queryRangeBySensor(
                1L, "rain_01", List.of("rainfall"),
                T0, T0 + 3_600_001L, 12.0, 50.0, 100, 0);

        assertThat(result).containsKey("rainfall");
        // 只有 12.0 和 18.0 符合 >= 12,10.0 被排除
        assertThat(result.get("rainfall")).hasSize(2);
    }
}
