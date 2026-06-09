package com.zwei.iot.alarm.service.engine;

import com.zwei.iot.alarm.domain.AlarmCriteria;
import com.zwei.iot.alarm.domain.LevelConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CriteriaEvaluator V3.0 单元测试 — level_config 多指标逐级评估。
 */
@DisplayName("CriteriaEvaluator V3.0")
class CriteriaEvaluatorTest {

    private CriteriaEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new CriteriaEvaluator();
    }

    // ── 辅助方法：构建单条件单等级的 levelConfig JSON ──
    private String singleLevelConfig(String level, String subject, String operator, double threshold) {
        return "{\"" + level + "\":{\"logicOperator\":\"AND\",\"conditions\":[{" +
                "\"subject\":\"" + subject + "\",\"operator\":\"" + operator + "\",\"threshold\":" + threshold + "}]}}";
    }

    private String multiLevelConfig(
            String level1, String subj1, String op1, double t1,
            String level2, String subj2, String op2, double t2) {
        return "{" +
                "\"" + level1 + "\":{\"logicOperator\":\"AND\",\"conditions\":[" +
                "{\"subject\":\"" + subj1 + "\",\"operator\":\"" + op1 + "\",\"threshold\":" + t1 + "}]}," +
                "\"" + level2 + "\":{\"logicOperator\":\"AND\",\"conditions\":[" +
                "{\"subject\":\"" + subj2 + "\",\"operator\":\"" + op2 + "\",\"threshold\":" + t2 + "}]}}";
    }

    // ──── 单条件单等级 ────

    @Nested
    @DisplayName("单条件单等级 — 基本比较操作符")
    class SingleConditionSingleLevel {

        @ParameterizedTest
        @CsvSource({
                "GT,  10.0,  9.9,  0",
                "GT,  10.0, 10.0,  0",
                "GT,  10.0, 10.1,  1",
                "GTE, 10.0,  9.9,  0",
                "GTE, 10.0, 10.0,  1",
                "GTE, 10.0, 10.1,  1",
                "LT,  10.0, 10.1,  0",
                "LT,  10.0, 10.0,  0",
                "LT,  10.0,  9.9,  1",
                "LTE, 10.0, 10.1,  0",
                "LTE, 10.0, 10.0,  1",
                "LTE, 10.0,  9.9,  1",
                "EQ,  10.0, 10.001,0",
                "EQ,  10.0, 10.0,  1",
                "NEQ, 10.0, 10.0,  0",
                "NEQ, 10.0,  9.9,  1",
        })
        @DisplayName("比较操作符边界")
        void basicOps(String op, double threshold, double value, int expectedLevel) {
            String json = singleLevelConfig("blue", "water_level", op, threshold);
            AlarmCriteria c = AlarmCriteria.builder().levelConfig(json).build();

            int result = evaluator.evaluate(c, Map.of("water_level", value));
            assertThat(result).isEqualTo(expectedLevel);
        }

        @Test
        @DisplayName("BETWEEN: 区间内触发")
        void betweenInRange() {
            String json = "{\"blue\":{\"logicOperator\":\"AND\",\"conditions\":[{" +
                    "\"subject\":\"value\",\"operator\":\"BETWEEN\",\"threshold\":5.0,\"thresholdMax\":15.0}]}}";
            AlarmCriteria c = AlarmCriteria.builder().levelConfig(json).build();
            assertThat(evaluator.evaluate(c, Map.of("value", 10.0))).isEqualTo(1);
        }

        @Test
        @DisplayName("BETWEEN: 低于下界不触发")
        void betweenBelowMin() {
            String json = "{\"blue\":{\"logicOperator\":\"AND\",\"conditions\":[{" +
                    "\"subject\":\"value\",\"operator\":\"BETWEEN\",\"threshold\":5.0,\"thresholdMax\":15.0}]}}";
            AlarmCriteria c = AlarmCriteria.builder().levelConfig(json).build();
            assertThat(evaluator.evaluate(c, Map.of("value", 3.0))).isZero();
        }
    }

    // ──── 多指标 AND/OR ────

    @Nested
    @DisplayName("多指标逻辑组合")
    class MultiCondition {

        @Test
        @DisplayName("AND: 全部满足才触发")
        void andAllMustMatch() {
            String json = "{\"blue\":{\"logicOperator\":\"AND\",\"conditions\":[" +
                    "{\"subject\":\"rainfall\",\"operator\":\"GT\",\"threshold\":5}," +
                    "{\"subject\":\"water_level\",\"operator\":\"LT\",\"threshold\":20}]}}";
            AlarmCriteria c = AlarmCriteria.builder().levelConfig(json).build();

            assertThat(evaluator.evaluate(c, Map.of("rainfall", 10.0, "water_level", 15.0))).isEqualTo(1);
            assertThat(evaluator.evaluate(c, Map.of("rainfall", 10.0, "water_level", 25.0))).isZero();
            assertThat(evaluator.evaluate(c, Map.of("rainfall", 3.0, "water_level", 15.0))).isZero();
        }

        @Test
        @DisplayName("OR: 任一满足即触发")
        void orAnyMatches() {
            String json = "{\"blue\":{\"logicOperator\":\"OR\",\"conditions\":[" +
                    "{\"subject\":\"rainfall\",\"operator\":\"LT\",\"threshold\":5}," +
                    "{\"subject\":\"rainfall\",\"operator\":\"GT\",\"threshold\":100}]}}";
            AlarmCriteria c = AlarmCriteria.builder().levelConfig(json).build();

            assertThat(evaluator.evaluate(c, Map.of("rainfall", 3.0))).isEqualTo(1);
            assertThat(evaluator.evaluate(c, Map.of("rainfall", 200.0))).isEqualTo(1);
            assertThat(evaluator.evaluate(c, Map.of("rainfall", 50.0))).isZero();
        }

        @Test
        @DisplayName("AND 三个条件")
        void andThreeConditions() {
            String json = "{\"yellow\":{\"logicOperator\":\"AND\",\"conditions\":[" +
                    "{\"subject\":\"a\",\"operator\":\"GT\",\"threshold\":5}," +
                    "{\"subject\":\"b\",\"operator\":\"GT\",\"threshold\":10}," +
                    "{\"subject\":\"c\",\"operator\":\"LT\",\"threshold\":50}]}}";
            AlarmCriteria c = AlarmCriteria.builder().levelConfig(json).build();

            assertThat(evaluator.evaluate(c, Map.of("a", 10.0, "b", 20.0, "c", 30.0))).isEqualTo(2);
            assertThat(evaluator.evaluate(c, Map.of("a", 10.0, "b", 20.0, "c", 60.0))).isZero();
        }
    }

    // ──── 多级分级判定 ────

    @Nested
    @DisplayName("多级分级判定 — 从高到低")
    class MultiLevel {

        @Test
        @DisplayName("满足红色时返回 4（而非低等级）")
        void highestLevelWins() {
            String json = multiLevelConfig(
                    "blue", "rainfall", "GT", 10,
                    "red", "rainfall", "GT", 100);
            AlarmCriteria c = AlarmCriteria.builder().levelConfig(json).build();

            assertThat(evaluator.evaluate(c, Map.of("rainfall", 200.0))).isEqualTo(4);
        }

        @Test
        @DisplayName("仅满足蓝色条件")
        void onlyBlue() {
            String json = multiLevelConfig(
                    "blue", "rainfall", "GT", 10,
                    "yellow", "rainfall", "GT", 20);
            AlarmCriteria c = AlarmCriteria.builder().levelConfig(json).build();

            assertThat(evaluator.evaluate(c, Map.of("rainfall", 15.0))).isEqualTo(1);
        }

        @Test
        @DisplayName("满足黄色不满足橙色 → 返回 2")
        void yellowButNotOrange() {
            String json = multiLevelConfig(
                    "yellow", "rainfall", "GT", 20,
                    "orange", "rainfall", "GT", 50);
            AlarmCriteria c = AlarmCriteria.builder().levelConfig(json).build();

            assertThat(evaluator.evaluate(c, Map.of("rainfall", 30.0))).isEqualTo(2);
        }

        @Test
        @DisplayName("四级全配：分级触发")
        void allFourLevels() {
            String json = "{" +
                    "\"blue\":{\"logicOperator\":\"AND\",\"conditions\":[{\"subject\":\"v\",\"operator\":\"GT\",\"threshold\":10}]}," +
                    "\"yellow\":{\"logicOperator\":\"AND\",\"conditions\":[{\"subject\":\"v\",\"operator\":\"GT\",\"threshold\":20}]}," +
                    "\"orange\":{\"logicOperator\":\"AND\",\"conditions\":[{\"subject\":\"v\",\"operator\":\"GT\",\"threshold\":50}]}," +
                    "\"red\":{\"logicOperator\":\"AND\",\"conditions\":[{\"subject\":\"v\",\"operator\":\"GT\",\"threshold\":100}]}}";
            AlarmCriteria c = AlarmCriteria.builder().levelConfig(json).build();

            assertThat(evaluator.evaluate(c, Map.of("v", 5.0))).isZero();
            assertThat(evaluator.evaluate(c, Map.of("v", 15.0))).isEqualTo(1);
            assertThat(evaluator.evaluate(c, Map.of("v", 30.0))).isEqualTo(2);
            assertThat(evaluator.evaluate(c, Map.of("v", 60.0))).isEqualTo(3);
            assertThat(evaluator.evaluate(c, Map.of("v", 200.0))).isEqualTo(4);
        }
    }

    // ──── 不同等级需要不同的多指标 ────

    @Nested
    @DisplayName("不同等级不同指标组合")
    class DifferentIndicatorsPerLevel {

        @Test
        @DisplayName("蓝色仅看雨量 > 10，红色需雨量 > 100 且位移 > 50")
        void blueSimpleRedComplex() {
            String json = "{" +
                    "\"blue\":{\"logicOperator\":\"AND\",\"conditions\":[{\"subject\":\"rainfall\",\"operator\":\"GT\",\"threshold\":10}]}," +
                    "\"red\":{\"logicOperator\":\"AND\",\"conditions\":[" +
                    "{\"subject\":\"rainfall\",\"operator\":\"GT\",\"threshold\":100}," +
                    "{\"subject\":\"displacement\",\"operator\":\"GT\",\"threshold\":50}]}}";
            AlarmCriteria c = AlarmCriteria.builder().levelConfig(json).build();

            // 雨量大但位移小 → 红不触发
            assertThat(evaluator.evaluate(c, Map.of("rainfall", 200.0, "displacement", 30.0))).isEqualTo(1);
            // 两个都满足 → 红触发
            assertThat(evaluator.evaluate(c, Map.of("rainfall", 200.0, "displacement", 60.0))).isEqualTo(4);
            // 都不满足
            assertThat(evaluator.evaluate(c, Map.of("rainfall", 5.0, "displacement", 60.0))).isZero();
        }
    }

    // ──── 边界 / 异常 ────

    @Nested
    @DisplayName("边界与异常输入")
    class EdgeCases {

        @Test
        @DisplayName("空 subjectValues 返回 0")
        void emptyValues() {
            String json = singleLevelConfig("blue", "v", "GT", 10);
            assertThat(evaluator.evaluate(AlarmCriteria.builder().levelConfig(json).build(), Map.of())).isZero();
        }

        @Test
        @DisplayName("null subjectValues 返回 0")
        void nullValues() {
            String json = singleLevelConfig("blue", "v", "GT", 10);
            assertThat(evaluator.evaluate(AlarmCriteria.builder().levelConfig(json).build(), null)).isZero();
        }

        @Test
        @DisplayName("subject 不在 subjectValues 中 → 值=null → false")
        void missingSubject() {
            String json = singleLevelConfig("blue", "rainfall", "GT", 10);
            assertThat(evaluator.evaluate(AlarmCriteria.builder().levelConfig(json).build(), Map.of("other", 20.0))).isZero();
        }

        @Test
        @DisplayName("null levelConfig")
        void nullLevelConfig() {
            AlarmCriteria c = AlarmCriteria.builder().levelConfig(null).build();
            assertThat(evaluator.evaluate(c, Map.of("v", 100.0))).isZero();
        }

        @Test
        @DisplayName("空字符串 levelConfig")
        void emptyLevelConfig() {
            AlarmCriteria c = AlarmCriteria.builder().levelConfig("").build();
            assertThat(evaluator.evaluate(c, Map.of("v", 100.0))).isZero();
        }

        @Test
        @DisplayName("非法 JSON levelConfig 不抛异常")
        void invalidJsonNoThrow() {
            AlarmCriteria c = AlarmCriteria.builder().levelConfig("not-json").build();
            assertThat(evaluator.evaluate(c, Map.of("v", 100.0))).isZero();
        }

        @Test
        @DisplayName("条件数量为空 → 不触发")
        void emptyConditions() {
            String json = "{\"blue\":{\"logicOperator\":\"AND\",\"conditions\":[]}}";
            AlarmCriteria c = AlarmCriteria.builder().levelConfig(json).build();
            assertThat(evaluator.evaluate(c, Map.of("v", 100.0))).isZero();
        }

        @Test
        @DisplayName("threshold 为 null → false")
        void nullThreshold() {
            String json = "{\"blue\":{\"logicOperator\":\"AND\",\"conditions\":[{\"subject\":\"v\",\"operator\":\"GT\"}]}}";
            AlarmCriteria c = AlarmCriteria.builder().levelConfig(json).build();
            assertThat(evaluator.evaluate(c, Map.of("v", 100.0))).isZero();
        }
    }

    // ──── parseLevelConfig ────

    @Nested
    @DisplayName("parseLevelConfig")
    class ParseLevelConfig {

        @Test
        @DisplayName("正确解析四级配置")
        void parseFourLevels() {
            String json = "{\"blue\":{\"logicOperator\":\"AND\",\"conditions\":[]},\"red\":{\"logicOperator\":\"OR\",\"conditions\":[]}}";
            Map<String, LevelConfig> result = evaluator.parseLevelConfig(json);
            assertThat(result).hasSize(2);
            assertThat(result.get("blue").getLogicOperator()).isEqualTo("AND");
            assertThat(result.get("red").getLogicOperator()).isEqualTo("OR");
        }
    }

    // ──── extractSubjects ────

    @Test
    @DisplayName("extractSubjects 提取所有引用的主语")
    void extractSubjects() {
        String json = "{" +
                "\"blue\":{\"logicOperator\":\"AND\",\"conditions\":[" +
                "{\"subject\":\"rainfall\",\"operator\":\"GT\",\"threshold\":10}," +
                "{\"subject\":\"water_level\",\"operator\":\"GT\",\"threshold\":5}]}," +
                "\"red\":{\"logicOperator\":\"AND\",\"conditions\":[" +
                "{\"subject\":\"rainfall\",\"operator\":\"GT\",\"threshold\":100}]}}";
        List<String> subjects = evaluator.extractSubjects(json);
        assertThat(subjects).containsExactlyInAnyOrder("rainfall", "water_level");
    }
}
