package com.zwei.iot.alarm.service.engine;

import com.zwei.iot.alarm.domain.LevelCondition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("CriteriaEvaluator.normalizeSubject 段数校验")
class CriteriaEvaluatorTest {

    private final CriteriaEvaluator evaluator = new CriteriaEvaluator();

    private String normalize(String subject) throws Exception {
        Method m = CriteriaEvaluator.class.getDeclaredMethod("normalizeSubject", String.class);
        m.setAccessible(true);
        return (String) m.invoke(evaluator, subject);
    }

    @Test
    @DisplayName("传感器模式 4 段格式: 原样返回")
    void sensorModeFourParts() throws Exception {
        assertThat(normalize("DEV001.current.payload.water_level")).isEqualTo("DEV001.current.payload.water_level");
        assertThat(normalize("DEV001.prev.payload.rainfall_hour")).isEqualTo("DEV001.prev.payload.rainfall_hour");
    }

    @Test
    @DisplayName("监测类型模式 3 段格式: 原样返回")
    void monitorTypeModeThreeParts() throws Exception {
        assertThat(normalize("current.payload.water_level")).isEqualTo("current.payload.water_level");
        assertThat(normalize("prev.device.onlineStatus")).isEqualTo("prev.device.onlineStatus");
    }

    @Test
    @DisplayName("device 维度: 无视 current/prev 都接受")
    void deviceDimensionAcceptsBothKinds() throws Exception {
        assertThat(normalize("current.device.lastReportTime")).isEqualTo("current.device.lastReportTime");
        assertThat(normalize("sensorA.prev.device.onlineStatus")).isEqualTo("sensorA.prev.device.onlineStatus");
    }

    @Test
    @DisplayName("packet 维度: 仅 attrCode=dataTime 合法 (但 normalizeSubject 不校验 attrCode)")
    void packetDimensionAcceptsAnyAttrCode() throws Exception {
        assertThat(normalize("current.packet.dataTime")).isEqualTo("current.packet.dataTime");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "current.unknown.water_level",         // 非法 dimension
            "sensorA.future.payload.water_level",  // 非法 kind
            "current.payload",                     // 2 段, 太短
            "a.b.c.d.e",                           // 5 段, 太长
            "payload.current.water_level",         // 老格式不兼容
            ""                                     // 空
    })
    @DisplayName("非法格式: 返回 null")
    void invalidFormatsReturnNull(String subject) throws Exception {
        assertThat(normalize(subject)).isNull();
    }

    @Test
    @DisplayName("null / 空白: 返回 null")
    void nullAndBlank() throws Exception {
        assertThat(normalize(null)).isNull();
        assertThat(normalize("   ")).isNull();
    }

    // ── 多态比较测试 ──

    @Test
    void evaluateCondition_stringContains_hit() {
        LevelCondition c = new LevelCondition();
        c.setOperator("CONTAINS");
        c.setValueType("STRING");
        c.setThreshold("sensor");
        assertTrue(evaluator.evaluateCondition(c, "sensor_001"));
    }

    @Test
    void evaluateCondition_stringContains_miss() {
        LevelCondition c = new LevelCondition();
        c.setOperator("CONTAINS");
        c.setValueType("STRING");
        c.setThreshold("xyz");
        assertFalse(evaluator.evaluateCondition(c, "sensor_001"));
    }

    @Test
    void evaluateCondition_stringGt_ascii() {
        LevelCondition c = new LevelCondition();
        c.setOperator("GT");
        c.setValueType("STRING");
        c.setThreshold("apple");
        assertTrue(evaluator.evaluateCondition(c, "banana"));
        assertFalse(evaluator.evaluateCondition(c, "apple"));
    }

    @Test
    void evaluateCondition_booleanEq_true() {
        LevelCondition c = new LevelCondition();
        c.setOperator("EQ");
        c.setValueType("BOOLEAN");
        c.setThreshold(1);
        assertTrue(evaluator.evaluateCondition(c, 1));
        assertFalse(evaluator.evaluateCondition(c, 0));
    }

    @Test
    void evaluateCondition_datetimeGt_absolute() {
        LevelCondition c = new LevelCondition();
        c.setOperator("GT");
        c.setValueType("DATETIME");
        c.setThreshold("2026-06-23T10:00:00Z");
        java.time.Instant v = java.time.Instant.parse("2026-06-23T11:00:00Z");
        assertTrue(evaluator.evaluateCondition(c, v));
    }

    @Test
    void evaluateCondition_datetimeLt_relative() {
        LevelCondition c = new LevelCondition();
        c.setOperator("LT");
        c.setValueType("DATETIME");
        c.setThreshold("now-5h");   // 5h 前
        java.time.Instant stale = java.time.Instant.now().minus(10, java.time.temporal.ChronoUnit.HOURS);
        java.time.Instant fresh = java.time.Instant.now().minus(1, java.time.temporal.ChronoUnit.HOURS);
        assertTrue(evaluator.evaluateCondition(c, stale));   // 10h 前 < 5h 前
        assertFalse(evaluator.evaluateCondition(c, fresh));  // 1h 前 > 5h 前
    }

    @Test
    void evaluateCondition_unknownValueType_fallbackToNumber() {
        LevelCondition c = new LevelCondition();
        c.setOperator("GT");
        c.setValueType(null);       // 未知 -> 默认 NUMBER
        c.setThreshold(5.0);
        assertTrue(evaluator.evaluateCondition(c, 10.0));
    }
}
