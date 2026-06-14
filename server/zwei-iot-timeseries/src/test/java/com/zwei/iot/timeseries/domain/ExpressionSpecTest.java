package com.zwei.iot.timeseries.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ExpressionSpec sealed interface")
class ExpressionSpecTest {

    @Test
    @DisplayName("FunctionCall 持有 AggregationFunction")
    void functionCall_holdsFunc() {
        ExpressionSpec.FunctionCall fc = new ExpressionSpec.FunctionCall(AggregationFunction.AVG);
        assertThat(fc.func()).isEqualTo(AggregationFunction.AVG);
    }

    @Test
    @DisplayName("BinaryOp 持有左右表达式与运算符")
    void binaryOp_holdsOperands() {
        ExpressionSpec left = new ExpressionSpec.FunctionCall(AggregationFunction.MAX);
        ExpressionSpec right = new ExpressionSpec.FunctionCall(AggregationFunction.MIN);
        ExpressionSpec.BinaryOp op = new ExpressionSpec.BinaryOp(
                left, ExpressionSpec.BinaryOperator.SUB, right);
        assertThat(op.left()).isEqualTo(left);
        assertThat(op.op()).isEqualTo(ExpressionSpec.BinaryOperator.SUB);
        assertThat(op.right()).isEqualTo(right);
    }

    @Test
    @DisplayName("Constant 持有 double 值")
    void constant_holdsValue() {
        ExpressionSpec.Constant c = new ExpressionSpec.Constant(0.5);
        assertThat(c.value()).isEqualTo(0.5);
    }

    @Test
    @DisplayName("BinaryOperator.SUB 的符号是 '-'")
    void binaryOperatorSub_symbol() {
        assertThat(ExpressionSpec.BinaryOperator.SUB.getSymbol()).isEqualTo("-");
    }
}
