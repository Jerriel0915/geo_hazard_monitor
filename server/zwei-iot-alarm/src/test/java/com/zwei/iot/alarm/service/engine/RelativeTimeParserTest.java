package com.zwei.iot.alarm.service.engine;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

class RelativeTimeParserTest {

    @Test
    void parseNow_returnsCurrentInstant() {
        Instant before = Instant.now();
        Instant result = RelativeTimeParser.resolve("now");
        Instant after = Instant.now();
        assertFalse(result.isBefore(before));
        assertFalse(result.isAfter(after));
    }

    @ParameterizedTest
    @CsvSource({
            "now-5h,   HOURS,   5,  -1",
            "now+30m,  MINUTES, 30, +1",
            "now-1d,   DAYS,    1,  -1",
            "now+10s,  SECONDS, 10, +1"
    })
    void parseSingleOffset(String expr, ChronoUnit unit, long amount, int sign) {
        Instant before = Instant.now();
        Instant result = RelativeTimeParser.resolve(expr);
        Instant expected = before.plus(sign * amount, unit);
        // 允许 2 秒测试延迟
        assertTrue(Math.abs(result.getEpochSecond() - expected.getEpochSecond()) <= 2,
                "expr=" + expr + " got=" + result + " expected~=" + expected);
    }

    @Test
    void parseMultiOffset_nowMinus1d12h() {
        Instant before = Instant.now();
        Instant result = RelativeTimeParser.resolve("now-1d12h");
        Instant expected = before.minus(1, ChronoUnit.DAYS).minus(12, ChronoUnit.HOURS);
        assertTrue(Math.abs(result.getEpochSecond() - expected.getEpochSecond()) <= 2,
                "got=" + result + " expected~=" + expected);
    }

    @Test
    void parseMultiOffset_positiveInheritance() {
        Instant before = Instant.now();
        Instant result = RelativeTimeParser.resolve("now+1d12h");
        Instant expected = before.plus(1, ChronoUnit.DAYS).plus(12, ChronoUnit.HOURS);
        assertTrue(Math.abs(result.getEpochSecond() - expected.getEpochSecond()) <= 2,
                "now+1d12h 应为 +1 天 +12 小时（正号继承）");
    }

    @Test
    void parseMultiOffset_signSwitch() {
        Instant before = Instant.now();
        Instant result = RelativeTimeParser.resolve("now-1d+12h");
        Instant expected = before.minus(1, ChronoUnit.DAYS).plus(12, ChronoUnit.HOURS);
        assertTrue(Math.abs(result.getEpochSecond() - expected.getEpochSecond()) <= 2,
                "now-1d+12h 应为 -1 天 +12 小时（显式符号覆盖）");
    }

    @Test
    void parseFirstSegmentImplicitSign_defaultsToMinus() {
        Instant before = Instant.now();
        Instant result = RelativeTimeParser.resolve("now5h");
        Instant expected = before.minus(5, ChronoUnit.HOURS);
        assertTrue(Math.abs(result.getEpochSecond() - expected.getEpochSecond()) <= 2,
                "now5h 应等同于 now-5h（首段省略符号默认为 -）");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "5h", "now-5x", "now--5h", "now-5", "now-abc", "abc-5h"})
    void parseInvalid_throws(String expr) {
        assertThrows(IllegalArgumentException.class, () -> RelativeTimeParser.resolve(expr),
                "expr=" + expr + " 应当抛 IllegalArgumentException");
    }

    @Test
    void isRelative_recognizesPrefix() {
        assertTrue(RelativeTimeParser.isRelative("now"));
        assertTrue(RelativeTimeParser.isRelative("now-5h"));
        assertFalse(RelativeTimeParser.isRelative("2026-06-23T10:00:00"));
        assertFalse(RelativeTimeParser.isRelative(null));
        assertFalse(RelativeTimeParser.isRelative(""));
    }
}
