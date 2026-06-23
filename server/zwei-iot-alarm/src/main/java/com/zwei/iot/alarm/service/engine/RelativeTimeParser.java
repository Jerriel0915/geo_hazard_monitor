package com.zwei.iot.alarm.service.engine;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 相对时间表达式解析器。
 *
 * <p>语法: {@code now} 或 {@code now} 后跟 1 个或多个偏移段。
 * 每段格式: {@code [+-]?\d+[smhd]}
 * 第一段如果有符号则必须显式写出；后续段可省略符号，此时继承上一段的符号。
 * 首段省略符号时默认为 {@code -}（减）。
 *
 * <p>示例:
 * <ul>
 *   <li>{@code now} → 当前时刻</li>
 *   <li>{@code now-5h} → 5 小时前</li>
 *   <li>{@code now+30m} → 30 分钟后</li>
 *   <li>{@code now-1d12h} → 1 天 12 小时前（12h 继承 - 号）</li>
 *   <li>{@code now+1d12h} → 1 天 12 小时后（12h 继承 + 号）</li>
 *   <li>{@code now-1d+12h} → 1 天前再过 12 小时（显式符号覆盖）</li>
 * </ul>
 *
 * <p>单位: s=秒 m=分 h=时 d=天
 */
public final class RelativeTimeParser {

    private static final Pattern SEG = Pattern.compile("([+-]?)(\\d+)([smhd])");
    private static final Map<Character, ChronoUnit> UNITS = Map.of(
            's', ChronoUnit.SECONDS,
            'm', ChronoUnit.MINUTES,
            'h', ChronoUnit.HOURS,
            'd', ChronoUnit.DAYS);

    private RelativeTimeParser() {}

    /** 判断字符串是否为相对表达式（以 "now" 开头）。 */
    public static boolean isRelative(String expr) {
        return expr != null && expr.startsWith("now");
    }

    /** 解析相对表达式为 Instant；非法时抛 IllegalArgumentException。 */
    public static Instant resolve(String expr) {
        if (expr == null || expr.isBlank()) {
            throw new IllegalArgumentException("empty expr");
        }
        if (!expr.startsWith("now")) {
            throw new IllegalArgumentException("not a relative expr: " + expr);
        }
        Instant t = Instant.now();
        if (expr.length() == 3) return t;

        String tail = expr.substring(3);
        Matcher m = SEG.matcher(tail);
        int lastEnd = 0;
        char currentSign = '-';  // inherited by segments that omit the sign
        while (m.find()) {
            if (m.start() != lastEnd) {
                throw new IllegalArgumentException("gap or invalid char in expr: " + expr);
            }
            String signStr = m.group(1);
            if (!signStr.isEmpty()) {
                currentSign = signStr.charAt(0);
            }
            long n = Long.parseLong(m.group(2));
            char unitCode = m.group(3).charAt(0);
            ChronoUnit unit = UNITS.get(unitCode);
            if (unit == null) {
                throw new IllegalArgumentException("unknown unit: " + unitCode);
            }
            if (currentSign == '-') n = -n;
            t = t.plus(n, unit);
            lastEnd = m.end();
        }
        if (lastEnd != tail.length()) {
            throw new IllegalArgumentException("trailing garbage in expr: " + expr);
        }
        return t;
    }
}
