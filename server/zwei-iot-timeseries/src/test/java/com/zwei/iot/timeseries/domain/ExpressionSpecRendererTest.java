package com.zwei.iot.timeseries.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.zwei.iot.timeseries.domain.ExpressionSpec.BinaryOperator.*;
import static com.zwei.iot.timeseries.domain.ExpressionSpec.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ExpressionSpecRenderer")
class ExpressionSpecRendererTest {

    private final ExpressionSpecRenderer renderer = new ExpressionSpecRenderer();

    @Test
    @DisplayName("render — 单函数 FunctionCall")
    void render_singleFunction() {
        ExpressionSpec expr = new FunctionCall(AggregationFunction.AVG);
        assertThat(renderer.render(expr, "value")).isEqualTo("AVG(value)");
    }

    @Test
    @DisplayName("render — 百分位函数")
    void render_percentile() {
        ExpressionSpec expr = new FunctionCall(AggregationFunction.P95);
        assertThat(renderer.render(expr, "value")).isEqualTo("QUANTILE(value, 0.95)");
    }

    @Test
    @DisplayName("render — Constant")
    void render_constant() {
        ExpressionSpec expr = new Constant(0.5);
        assertThat(renderer.render(expr, "value")).isEqualTo("0.5");
    }

    @Test
    @DisplayName("render — 二元运算 MAX - MIN")
    void render_binaryOp() {
        ExpressionSpec expr = new BinaryOp(
                new FunctionCall(AggregationFunction.MAX), SUB,
                new FunctionCall(AggregationFunction.MIN));
        assertThat(renderer.render(expr, "value")).isEqualTo("(MAX(value) - MIN(value))");
    }

    @Test
    @DisplayName("render — 嵌套 (MAX-MIN)/AVG")
    void render_nested() {
        ExpressionSpec expr = new BinaryOp(
                new BinaryOp(
                        new FunctionCall(AggregationFunction.MAX), SUB,
                        new FunctionCall(AggregationFunction.MIN)),
                DIV,
                new FunctionCall(AggregationFunction.AVG));
        assertThat(renderer.render(expr, "value"))
                .isEqualTo("((MAX(value) - MIN(value)) / AVG(value))");
    }

    @Test
    @DisplayName("render — 嵌套深度超过 5 层抛 IllegalArgumentException")
    void render_tooDeep_throws() {
        ExpressionSpec expr = buildDeepNested(6);
        assertThatThrownBy(() -> renderer.render(expr, "value"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("嵌套过深");
    }

    private ExpressionSpec buildDeepNested(int layers) {
        ExpressionSpec expr = new Constant(1.0);
        for (int i = 0; i < layers; i++) {
            expr = new BinaryOp(expr, ADD, new Constant(1.0));
        }
        return expr;
    }

    @Test
    @DisplayName("alias — FunctionCall 直接用枚举名")
    void alias_functionCall() {
        ExpressionSpec expr = new FunctionCall(AggregationFunction.AVG);
        assertThat(renderer.alias(expr)).isEqualTo("AVG");
    }

    @Test
    @DisplayName("alias — BinaryOp 用运算符符号拼装")
    void alias_binaryOp() {
        ExpressionSpec expr = new BinaryOp(
                new FunctionCall(AggregationFunction.MAX), SUB,
                new FunctionCall(AggregationFunction.MIN));
        assertThat(renderer.alias(expr)).isEqualTo("MAX-MIN");
    }

    @Test
    @DisplayName("alias — 嵌套 (MAX-MIN)/AVG")
    void alias_nested() {
        ExpressionSpec expr = new BinaryOp(
                new BinaryOp(
                        new FunctionCall(AggregationFunction.MAX), SUB,
                        new FunctionCall(AggregationFunction.MIN)),
                DIV,
                new FunctionCall(AggregationFunction.AVG));
        assertThat(renderer.alias(expr)).isEqualTo("(MAX-MIN)/AVG");
    }

    @Test
    @DisplayName("alias — LAST-FIRST 简写为 DELTA")
    void alias_lastMinusFirst() {
        ExpressionSpec expr = new BinaryOp(
                new FunctionCall(AggregationFunction.LAST_VALUE), SUB,
                new FunctionCall(AggregationFunction.FIRST_VALUE));
        assertThat(renderer.alias(expr)).isEqualTo("DELTA");
    }

    @Test
    @DisplayName("alias — Constant 用数值字符串")
    void alias_constant() {
        assertThat(renderer.alias(new Constant(0.5))).isEqualTo("0.5");
    }

    @Test
    @DisplayName("alias — 长度超过 64 抛 IllegalArgumentException")
    void alias_tooLong_throws() {
        ExpressionSpec expr = buildDeepNested(6);
        assertThatThrownBy(() -> renderer.alias(expr))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("别名");
    }
}
