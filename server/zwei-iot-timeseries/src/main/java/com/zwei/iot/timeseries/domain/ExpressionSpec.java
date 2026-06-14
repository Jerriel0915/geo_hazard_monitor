package com.zwei.iot.timeseries.domain;

/**
 * 表达式 DSL — 用 sealed interface 表达可嵌套的聚合函数组合。
 *
 * <p>封闭类型,子类型在编译期固定,无法运行时注入新节点。
 * 支持:</p>
 * <ul>
 *   <li>{@link FunctionCall} — 单个聚合函数</li>
 *   <li>{@link BinaryOp} — 二元运算(可嵌套),支持 + - * /</li>
 *   <li>{@link Constant} — 标量常量</li>
 * </ul>
 */
public sealed interface ExpressionSpec {

    /** 单个聚合函数,如 {@code AVG(attr)} */
    record FunctionCall(AggregationFunction func) implements ExpressionSpec {}

    /** 二元运算,可嵌套,支持 + - * / */
    record BinaryOp(ExpressionSpec left, BinaryOperator op, ExpressionSpec right) implements ExpressionSpec {}

    /** 标量常量,如 {@code 0.5} */
    record Constant(double value) implements ExpressionSpec {}

    /** 二元运算符 */
    enum BinaryOperator {
        ADD("+"), SUB("-"), MUL("*"), DIV("/");

        private final String symbol;

        BinaryOperator(String symbol) {
            this.symbol = symbol;
        }

        public String getSymbol() {
            return symbol;
        }
    }
}
