package com.zwei.iot.alarm.service.engine;

import com.zwei.iot.alarm.domain.AlarmCriteria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Collections;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CriteriaEvaluator 单元测试 — 覆盖所有操作符、逻辑组合、分级表达式。
 */
@DisplayName("CriteriaEvaluator")
class CriteriaEvaluatorTest {

    private CriteriaEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new CriteriaEvaluator();
    }

    // ──────────── single condition operators ────────────

    @Nested
    @DisplayName("单条件操作符")
    class SingleCondition {

        @ParameterizedTest
        @CsvSource({
                "GT,  10.0,  9.9,  false",
                "GT,  10.0, 10.0,  false",
                "GT,  10.0, 10.1,  true",
                "GTE, 10.0,  9.9,  false",
                "GTE, 10.0, 10.0,  true",
                "GTE, 10.0, 10.1,  true",
                "LT,  10.0, 10.1,  false",
                "LT,  10.0, 10.0,  false",
                "LT,  10.0,  9.9,  true",
                "LTE, 10.0, 10.1,  false",
                "LTE, 10.0, 10.0,  true",
                "LTE, 10.0,  9.9,  true",
                "EQ,  10.0, 10.001, false",
                "NEQ, 10.0, 15.0,  true",
        })
        @DisplayName("基本比较操作符")
        void comparisonOps(String op, Double threshold, Double value, boolean expectedMatch) {
            String json = "[{\"indicator\":\"value\",\"operator\":\"" + op + "\",\"threshold\":" + threshold + "}]";
            AlarmCriteria criteria = AlarmCriteria.builder()
                    .conditionsJson(json)
                    .logicOperator("AND")
                    .blueExpression("0")  // always-satisfied level expression, test focuses on condition ops
                    .build();

            int result = evaluator.evaluate(criteria, value);
            assertThat(result > 0).isEqualTo(expectedMatch);
        }

        @Test
        @DisplayName("EQ: 精确匹配触发")
        void eqExactMatch() {
            String json = "[{\"indicator\":\"value\",\"operator\":\"EQ\",\"threshold\":10.0}]";
            AlarmCriteria criteria = AlarmCriteria.builder()
                    .conditionsJson(json).logicOperator("AND")
                    .blueExpression("0").build();

            assertThat(evaluator.evaluate(criteria, 10.0)).isGreaterThan(0);
        }

        @Test
        @DisplayName("EQ: 微小偏差不触发 (>= epsilon)")
        void eqEpsilonBoundary() {
            String json = "[{\"indicator\":\"value\",\"operator\":\"EQ\",\"threshold\":10.0}]";
            AlarmCriteria criteria = AlarmCriteria.builder()
                    .conditionsJson(json).logicOperator("AND")
                    .blueExpression("0").build();

            // 10.001 - 10.0 = 0.001 NOT < 0.0001 → false (must be outside epsilon)
            assertThat(evaluator.evaluate(criteria, 10.001)).isZero();
        }

        @Test
        @DisplayName("NEQ: 相等时不应该触发")
        void neqEqualDoesNotTrigger() {
            String json = "[{\"indicator\":\"value\",\"operator\":\"NEQ\",\"threshold\":10.0}]";
            AlarmCriteria criteria = AlarmCriteria.builder()
                    .conditionsJson(json).logicOperator("AND")
                    .blueExpression("0").build();

            assertThat(evaluator.evaluate(criteria, 10.0)).isZero();
        }

        @DisplayName("BETWEEN: 值在区间内触发")
        void betweenInRange() {
            String json = "[{\"indicator\":\"value\",\"operator\":\"BETWEEN\",\"threshold\":5.0,\"thresholdMax\":15.0}]";
            AlarmCriteria criteria = AlarmCriteria.builder()
                    .conditionsJson(json).logicOperator("AND")
                    .blueExpression("0").build();

            assertThat(evaluator.evaluate(criteria, 10.0)).isGreaterThan(0);
        }

        @Test
        @DisplayName("BETWEEN: 值低于下界不触发")
        void betweenBelowMin() {
            String json = "[{\"indicator\":\"value\",\"operator\":\"BETWEEN\",\"threshold\":5.0,\"thresholdMax\":15.0}]";
            AlarmCriteria criteria = AlarmCriteria.builder()
                    .conditionsJson(json).logicOperator("AND")
                    .blueExpression("0").build();

            assertThat(evaluator.evaluate(criteria, 3.0)).isZero();
        }

        @Test
        @DisplayName("BETWEEN: 值超过上界不触发")
        void betweenAboveMax() {
            String json = "[{\"indicator\":\"value\",\"operator\":\"BETWEEN\",\"threshold\":5.0,\"thresholdMax\":15.0}]";
            AlarmCriteria criteria = AlarmCriteria.builder()
                    .conditionsJson(json).logicOperator("AND")
                    .blueExpression("0").build();

            assertThat(evaluator.evaluate(criteria, 20.0)).isZero();
        }
    }

    // ──────────── null / empty input ────────────

    @Nested
    @DisplayName("空值/边界输入")
    class NullAndEmpty {

        @Test
        @DisplayName("null 测量值返回 0")
        void nullValueReturnsZero() {
            AlarmCriteria criteria = AlarmCriteria.builder()
                    .conditionsJson("[{\"indicator\":\"value\",\"operator\":\"GT\",\"threshold\":5}]")
                    .logicOperator("AND")
                    .blueExpression("5")
                    .build();

            assertThat(evaluator.evaluate(criteria, null)).isZero();
        }

        @Test
        @DisplayName("null conditions_json 仅用分级表达式判定")
        void nullConditionsJsonFallsBackToLevelExpression() {
            AlarmCriteria criteria = AlarmCriteria.builder()
                    .conditionsJson(null)
                    .logicOperator("AND")
                    .blueExpression("5")
                    .build();

            assertThat(evaluator.evaluate(criteria, 10.0)).isEqualTo(1);
        }

        @Test
        @DisplayName("空 conditions_json 仅用分级表达式判定")
        void emptyConditionsJsonFallsBack() {
            AlarmCriteria criteria = AlarmCriteria.builder()
                    .conditionsJson("")
                    .logicOperator("AND")
                    .redExpression("100")
                    .build();

            assertThat(evaluator.evaluate(criteria, 200.0)).isEqualTo(4);
        }

        @Test
        @DisplayName("无效 conditions_json 不抛异常")
        void invalidJsonDoesNotThrow() {
            AlarmCriteria criteria = AlarmCriteria.builder()
                    .conditionsJson("not-valid-json")
                    .logicOperator("AND")
                    .blueExpression("5")
                    .build();

            // should not throw, fall back to level expressions
            assertThat(evaluator.evaluate(criteria, 10.0)).isEqualTo(1);
        }
    }

    // ──────────── AND/OR logic ────────────

    @Nested
    @DisplayName("AND/OR 逻辑组合")
    class LogicCombinations {

        @Test
        @DisplayName("AND: 全部条件满足才触发")
        void andAllMustMatch() {
            String json = "[" +
                    "{\"indicator\":\"value\",\"operator\":\"GT\",\"threshold\":5}," +
                    "{\"indicator\":\"value\",\"operator\":\"LT\",\"threshold\":20}" +
                    "]";
            AlarmCriteria criteria = AlarmCriteria.builder()
                    .conditionsJson(json).logicOperator("AND")
                    .blueExpression("5").build();

            assertThat(evaluator.evaluate(criteria, 10.0)).isGreaterThan(0);
            assertThat(evaluator.evaluate(criteria, 3.0)).isZero();
            assertThat(evaluator.evaluate(criteria, 25.0)).isZero();
        }

        @Test
        @DisplayName("OR: 任一条件满足即触发")
        void orAnyMatches() {
            String json = "[" +
                    "{\"indicator\":\"value\",\"operator\":\"LT\",\"threshold\":5}," +
                    "{\"indicator\":\"value\",\"operator\":\"GT\",\"threshold\":100}" +
                    "]";
            AlarmCriteria criteria = AlarmCriteria.builder()
                    .conditionsJson(json).logicOperator("OR")
                    .blueExpression("0").build();

            assertThat(evaluator.evaluate(criteria, 3.0)).isGreaterThan(0);
            assertThat(evaluator.evaluate(criteria, 200.0)).isGreaterThan(0);
            assertThat(evaluator.evaluate(criteria, 50.0)).isZero();
        }
    }

    // ──────────── level expressions ────────────

    @Nested
    @DisplayName("分级表达式")
    class LevelExpressions {

        @Test
        @DisplayName("从高到低判定: 满足红色时返回4（不是低等级）")
        void highestLevelWins() {
            AlarmCriteria criteria = AlarmCriteria.builder()
                    .conditionsJson("[{\"indicator\":\"value\",\"operator\":\"GT\",\"threshold\":5}]")
                    .logicOperator("AND")
                    .blueExpression("10")
                    .yellowExpression("20")
                    .orangeExpression("50")
                    .redExpression("100")
                    .build();

            assertThat(evaluator.evaluate(criteria, 200.0)).isEqualTo(4);
        }

        @Test
        @DisplayName("仅蓝色条件满足时返回1")
        void onlyBlueTriggers() {
            AlarmCriteria criteria = AlarmCriteria.builder()
                    .conditionsJson("[{\"indicator\":\"value\",\"operator\":\"GT\",\"threshold\":5}]")
                    .logicOperator("AND")
                    .blueExpression("10")
                    .yellowExpression("20")
                    .build();

            assertThat(evaluator.evaluate(criteria, 15.0)).isEqualTo(1);
        }

        @Test
        @DisplayName("无分级表达式匹配时返回0")
        void noLevelMatched() {
            AlarmCriteria criteria = AlarmCriteria.builder()
                    .conditionsJson("[{\"indicator\":\"value\",\"operator\":\"GT\",\"threshold\":5}]")
                    .logicOperator("AND")
                    .yellowExpression("20")
                    .build();

            assertThat(evaluator.evaluate(criteria, 10.0)).isZero();
        }

        @ParameterizedTest
        @CsvSource({
                "GT 5,  10.0, true",
                "GT 5,   3.0, false",
                "LT 5,   3.0, true",
                "LT 5,  10.0, false",
                "LTE 5,  5.0, true",
                "GTE 5,  5.0, true",
                "5,      6.0, true",
                "5,      4.0, false",
        })
        @DisplayName("表达式前缀解析")
        void expressionPrefixes(String expr, Double value, boolean triggers) {
            AlarmCriteria criteria = AlarmCriteria.builder()
                    .conditionsJson(null)
                    .blueExpression(expr)
                    .build();

            int result = evaluator.evaluate(criteria, value);
            assertThat(result > 0).isEqualTo(triggers);
        }

        @Test
        @DisplayName("非法数字表达式不抛异常返回0")
        void invalidNumberExpressionReturnsZero() {
            AlarmCriteria criteria = AlarmCriteria.builder()
                    .conditionsJson("[{\"indicator\":\"value\",\"operator\":\"GT\",\"threshold\":5}]")
                    .logicOperator("AND")
                    .blueExpression("abc")
                    .build();

            assertThat(evaluator.evaluate(criteria, 10.0)).isZero();
        }
    }

    // ──────────── edge cases ────────────

    @Test
    @DisplayName("无 conditions_json 也无分级表达式 → 返回 0")
    void noConditionsAndNoLevelExpressions() {
        AlarmCriteria criteria = AlarmCriteria.builder()
                .conditionsJson(null)
                .logicOperator("AND")
                .build();

        assertThat(evaluator.evaluate(criteria, 100.0)).isZero();
    }

    @Test
    @DisplayName("parseConditions 返回空列表时回退到分级表达式")
    void parseReturnsEmptyListFallback() {
        AlarmCriteria criteria = AlarmCriteria.builder()
                .conditionsJson("[]")
                .logicOperator("AND")
                .blueExpression("5")
                .build();

        assertThat(evaluator.evaluate(criteria, 10.0)).isEqualTo(1);
    }
}
