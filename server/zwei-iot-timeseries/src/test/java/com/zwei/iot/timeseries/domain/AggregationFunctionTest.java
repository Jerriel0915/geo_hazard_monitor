package com.zwei.iot.timeseries.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AggregationFunction 白名单枚举")
class AggregationFunctionTest {

    @ParameterizedTest
    @CsvSource({
        "AVG,          AVG(value)",
        "MAX,          MAX(value)",
        "MIN,          MIN(value)",
        "SUM,          SUM(value)",
        "COUNT,        COUNT(value)",
        "FIRST_VALUE,  FIRST_VALUE(value)",
        "LAST_VALUE,   LAST_VALUE(value)",
        "EXTREME,      EXTREME(value)",
        "STDDEV,       STDDEV(value)",
    })
    @DisplayName("getIotdbExpr — 普通聚合函数")
    void getIotdbExpr_regularFunctions(String name, String expected) {
        AggregationFunction func = AggregationFunction.valueOf(name);
        assertThat(func.getIotdbExpr("value")).isEqualTo(expected);
    }

    @Test
    @DisplayName("getIotdbExpr — P50 带 quantile 参数")
    void getIotdbExpr_p50() {
        assertThat(AggregationFunction.P50.getIotdbExpr("value")).isEqualTo("QUANTILE(value, 0.5)");
    }

    @Test
    @DisplayName("getIotdbExpr — P95 带 quantile 参数")
    void getIotdbExpr_p95() {
        assertThat(AggregationFunction.P95.getIotdbExpr("value")).isEqualTo("QUANTILE(value, 0.95)");
    }

    @Test
    @DisplayName("getIotdbExpr — P99 带 quantile 参数")
    void getIotdbExpr_p99() {
        assertThat(AggregationFunction.P99.getIotdbExpr("value")).isEqualTo("QUANTILE(value, 0.99)");
    }

    @ParameterizedTest
    @EnumSource(value = AggregationFunction.class, names = {"P50", "P95", "P99"})
    @DisplayName("needsQuartileParam — 百分位返回 true")
    void needsQuartileParam_percentile(AggregationFunction func) {
        assertThat(func.needsQuartileParam()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = AggregationFunction.class, mode = EnumSource.Mode.EXCLUDE, names = {"P50", "P95", "P99"})
    @DisplayName("needsQuartileParam — 普通函数返回 false")
    void needsQuartileParam_regular(AggregationFunction func) {
        assertThat(func.needsQuartileParam()).isFalse();
    }
}
