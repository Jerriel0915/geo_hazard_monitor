# GroupedRuleBuilder UI 调整与多值类型支持 实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 让告警判据配置界面支持多种数据类型（NUMBER/DATETIME/STRING/BOOLEAN）+ 中文回显 + 宽度自适应 + 相对时间，同步扩展后端评估引擎支持多态比较。

**架构：** 复用现有 `monitor_content.indicator_type` 字段做 valueType 判别（无 DB 迁移）；后端 `LevelCondition.threshold: Double → Object` + `Map<String, Double> → Map<String, Object>`；`CriteriaEvaluator` 按 valueType 分派到 4 个比较方法；新增 `RelativeTimeParser` 解析 `now-5h` 类表达式。前端 `ConditionRow.vue` 按 valueType 切换操作符与控件、`useIndicatorTree.ts` 修复中文回显路径。

**技术栈：** Java 17 + Spring Boot 4.0.3 + MyBatis + FastJSON2 + Lombok / Vue 3 + TypeScript + Element Plus 2.6

**关联规格：** [`docs/superpowers/specs/2026-06-23-grouped-rule-builder-ui-design.md`](../specs/2026-06-23-grouped-rule-builder-ui-design.md)

---

## 文件清单（创建/修改/测试）

### 后端

| 操作 | 路径 | 职责 |
|---|---|---|
| 创建 | `server/zwei-iot-monitor/src/main/java/com/zwei/iot/monitor/constant/IndicatorValueType.java` | indicator_type code → valueType 映射常量 |
| 创建 | `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/engine/RelativeTimeParser.java` | 解析 `now-5h` 等表达式为 Instant |
| 创建 | `server/zwei-iot-alarm/src/test/java/com/zwei/iot/alarm/service/engine/RelativeTimeParserTest.java` | 相对时间解析单测 |
| 修改 | `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/domain/LevelCondition.java` | threshold/thresholdMax Double→Object；新增 valueType |
| 修改 | `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/engine/AlarmEvaluationEngine.java` | Map Double→Object；按 pv.value() 类型放入；payload 注入 valueType；device/packet 硬编码 valueType |
| 修改 | `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/engine/CriteriaEvaluator.java` | evaluateCondition(cond, Object) 多态分派；新增 CONTAINS、compareDatetime（含相对）、compareString、compareBoolean |
| 修改 | `server/zwei-iot-alarm/src/test/java/com/zwei/iot/alarm/service/engine/CriteriaEvaluatorTest.java` | 新增多类型比较用例 |

### 前端

| 操作 | 路径 | 职责 |
|---|---|---|
| 创建 | `web/src/utils/indicatorType.ts` | 抽出 IndicatorTypeEnum + 加 valueType 字段 + helper |
| 修改 | `web/src/views/basic/MonitorType.vue` | 删除内联 enum，import 新 util |
| 修改 | `web/src/api/monitorType.ts` | MonitorContentItem 加 valueType?: string |
| 修改 | `web/src/views/alarm/composables/useIndicatorTree.ts` | meta 加 valueType；payload 叶子查映射注入；device/packet 硬编码；重写 prefixDisplayLabels 用中文 labelPrefix |
| 修改 | `web/src/views/alarm/components/ConditionRow.vue` | 按 valueType 切换 operators + 控件 + 宽度 + DATETIME 模式切换 |
| 修改 | `web/src/views/alarm/AlarmCriteria.vue` | Condition.threshold union；DATETIME 相对模式序列化 |

---

## 任务 1：创建 indicator_type → valueType 共享常量（后端）

**文件：**
- 创建：`server/zwei-iot-monitor/src/main/java/com/zwei/iot/monitor/constant/IndicatorValueType.java`

- [ ] **步骤 1：创建映射常量类**

写入完整内容：

```java
package com.zwei.iot.monitor.constant;

import java.util.Map;

/**
 * 监测内容 indicator_type → 数据类型 (valueType) 映射。
 *
 * <p>复用 monitor_content.indicator_type 字段作为类型判别，避免新增 DB 列。
 * 未知 code 默认 NUMBER（向后兼容）。
 *
 * <p>valueType 取值: NUMBER / DATETIME / STRING / BOOLEAN
 */
public final class IndicatorValueType {

    public static final String NUMBER = "NUMBER";
    public static final String DATETIME = "DATETIME";
    public static final String STRING = "STRING";
    public static final String BOOLEAN = "BOOLEAN";

    private static final Map<String, String> MAPPING = Map.ofEntries(
            Map.entry("wy", NUMBER),    // 位移
            Map.entry("wd", NUMBER),    // 温度
            Map.entry("jd", NUMBER),    // 角度
            Map.entry("yl", NUMBER),    // 压力
            Map.entry("sw", NUMBER),    // 水位
            Map.entry("jsd", NUMBER),   // 加速度
            Map.entry("hsl", NUMBER),   // 含水率
            Map.entry("ljn", NUMBER),   // 力矩
            Map.entry("zdl", NUMBER),   // 震动频率
            Map.entry("dl", NUMBER),    // 电量
            Map.entry("dx", BOOLEAN),   // 断线
            Map.entry("sg", STRING),    // 声光
            Map.entry("sp", STRING)     // 视频
    );

    private IndicatorValueType() {}

    /** 返回 indicator_type code 对应的 valueType；未知返回 NUMBER。 */
    public static String of(String indicatorType) {
        if (indicatorType == null || indicatorType.isBlank()) return NUMBER;
        return MAPPING.getOrDefault(indicatorType.trim().toLowerCase(), NUMBER);
    }
}
```

- [ ] **步骤 2：编译验证**

运行：`mvn compile -pl zwei-iot-monitor -am -q`
预期：BUILD SUCCESS，无错误。

- [ ] **步骤 3：Commit**

```bash
git add server/zwei-iot-monitor/src/main/java/com/zwei/iot/monitor/constant/IndicatorValueType.java
git commit -m "feat(alarm): 新增 IndicatorValueType 映射 indicator_type → valueType"
```

---

## 任务 2：RelativeTimeParser + 单元测试（TDD）

**文件：**
- 创建测试：`server/zwei-iot-alarm/src/test/java/com/zwei/iot/alarm/service/engine/RelativeTimeParserTest.java`
- 创建实现：`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/engine/RelativeTimeParser.java`

- [ ] **步骤 1：先写失败的测试**

完整测试文件内容：

```java
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
```

- [ ] **步骤 2：运行测试验证失败**

运行：`mvn test -pl zwei-iot-alarm -Dtest=RelativeTimeParserTest -q`
预期：编译失败，报 `cannot find symbol: class RelativeTimeParser`。

- [ ] **步骤 3：编写实现**

完整实现文件内容：

```java
package com.zwei.iot.alarm.service.engine;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 相对时间表达式解析器。
 *
 * <p>语法: {@code now} 或 {@code now([+-]\d+[smhd])}
 *
 * <p>示例:
 * <ul>
 *   <li>{@code now} → 当前时刻</li>
 *   <li>{@code now-5h} → 5 小时前</li>
 *   <li>{@code now+30m} → 30 分钟后</li>
 *   <li>{@code now-1d12h} → 1 天 12 小时前</li>
 * </ul>
 *
 * <p>单位: s=秒 m=分 h=时 d=天
 */
public final class RelativeTimeParser {

    private static final Pattern SEG = Pattern.compile("([+-])(\\d+)([smhd])");
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
        while (m.find()) {
            if (m.start() != lastEnd) {
                throw new IllegalArgumentException("gap or invalid char in expr: " + expr);
            }
            char sign = m.group(1).charAt(0);
            long n = Long.parseLong(m.group(2));
            char unitCode = m.group(3).charAt(0);
            ChronoUnit unit = UNITS.get(unitCode);
            if (unit == null) {
                throw new IllegalArgumentException("unknown unit: " + unitCode);
            }
            if (sign == '-') n = -n;
            t = t.plus(n, unit);
            lastEnd = m.end();
        }
        if (lastEnd != tail.length()) {
            throw new IllegalArgumentException("trailing garbage in expr: " + expr);
        }
        return t;
    }
}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`mvn test -pl zwei-iot-alarm -Dtest=RelativeTimeParserTest -q`
预期：BUILD SUCCESS，所有测试通过。

- [ ] **步骤 5：Commit**

```bash
git add server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/engine/RelativeTimeParser.java \
        server/zwei-iot-alarm/src/test/java/com/zwei/iot/alarm/service/engine/RelativeTimeParserTest.java
git commit -m "feat(alarm): RelativeTimeParser 支持 now-5h 相对时间表达式"
```

---

## 任务 3：LevelCondition 字段改造（Double→Object + valueType）

**文件：**
- 修改：`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/domain/LevelCondition.java`

- [ ] **步骤 1：修改 threshold/thresholdMax 类型 + 新增 valueType**

将原文件第 36-48 行（threshold / thresholdMax / unit 字段附近）替换为：

```java
    /**
     * 运算符: GT / GTE / LT / LTE / EQ / NEQ / BETWEEN / CONTAINS
     */
    private String operator;

    /**
     * 数据类型: NUMBER / DATETIME / STRING / BOOLEAN
     * <p>用于 CriteriaEvaluator 多态分派；为 null 时按 NUMBER 兼容老数据。
     */
    private String valueType;

    /**
     * 阈值 — 按 valueType 解释:
     * <ul>
     *   <li>NUMBER: Double</li>
     *   <li>DATETIME: String (ISO-8601 或 "now-5h" 相对)</li>
     *   <li>STRING: String</li>
     *   <li>BOOLEAN: Integer (1/0)</li>
     * </ul>
     */
    private Object threshold;

    /**
     * 阈值上限（BETWEEN 时使用） — NUMBER: Double；DATETIME: String
     */
    private Object thresholdMax;

    /**
     * 单位
     */
    private String unit;
```

- [ ] **步骤 2：更新对应 getter/setter 签名**

将原文件第 96-110 行（getThreshold/setThreshold/getThresholdMax/setThresholdMax）替换为：

```java
    public Object getThreshold() {
        return threshold;
    }

    public void setThreshold(Object threshold) {
        this.threshold = threshold;
    }

    public Object getThresholdMax() {
        return thresholdMax;
    }

    public void setThresholdMax(Object thresholdMax) {
        this.thresholdMax = thresholdMax;
    }

    public String getValueType() {
        return valueType;
    }

    public void setValueType(String valueType) {
        this.valueType = valueType;
    }
```

- [ ] **步骤 3：编译验证**

运行：`mvn compile -pl zwei-iot-alarm -am -q`
预期：BUILD SUCCESS。如果有其他类引用 `Double threshold = cond.getThreshold()`，编译会报错——继续到步骤 4 修复。

- [ ] **步骤 4：修复 CriteriaEvaluator 中的类型引用**

`CriteriaEvaluator.java` 第 215 行 `Double threshold = cond.getThreshold();` → 改为 `Object thresholdObj = cond.getThreshold();`；第 240 行 `cond.getThresholdMax()` 返回 Object，需做 Double cast。

具体改造延后到任务 5 完整重写 evaluateCondition。本步骤仅确保编译通过（最小临时补丁）：

```java
// CriteriaEvaluator.java:215 临时改为：
Object thresholdObj = cond.getThreshold();
if (!(thresholdObj instanceof Number)) {
    log.debug("[Alarm][Criteria][Cond] threshold 非 Number 跳过 (临时兼容) subject={}", cond.getSubject());
    return false;
}
Double threshold = ((Number) thresholdObj).doubleValue();
```

`cond.getThresholdMax()` 第 240 行类似处理：

```java
Object maxObj = cond.getThresholdMax();
if (!(maxObj instanceof Number)) {
    log.debug("[Alarm][Criteria][Cond] BETWEEN thresholdMax 非 Number 跳过 subject={}", cond.getSubject());
    return false;
}
double thresholdMax = ((Number) maxObj).doubleValue();
return value >= threshold && value <= thresholdMax;
```

- [ ] **步骤 5：运行现有测试确认未破坏 NUMBER 路径**

运行：`mvn test -pl zwei-iot-alarm -Dtest=CriteriaEvaluatorTest -q`
预期：所有 11 个原测试 PASS（normalizeSubject 相关，不涉及 threshold 类型）。

- [ ] **步骤 6：Commit**

```bash
git add server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/domain/LevelCondition.java \
        server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/engine/CriteriaEvaluator.java
git commit -m "refactor(alarm): LevelCondition.threshold Double → Object + 新增 valueType"
```

---

## 任务 4：AlarmEvaluationEngine 多态 subjectValues + valueType 注入

**文件：**
- 修改：`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/engine/AlarmEvaluationEngine.java`

- [ ] **步骤 1：将 subjectValues 改为 Map<String, Object>**

第 109 行：
```java
        Map<String, Object> subjectValues = new HashMap<>();
```

- [ ] **步骤 2：bucket 1 (current payload) 支持多类型 + 注入 valueType**

替换第 115-124 行：

```java
        // ── bucket 1: 本 sensorCode 的 current payload ──
        // 多类型支持: payload 值原样放入 (Number/String/Boolean)，DATETIME 由 device/packet 维度提供
        if (event.getProperties() != null) {
            for (PropertyValue pv : event.getProperties()) {
                Object v = pv.value();
                if (v == null) continue;
                String key1 = prefix + "current.payload." + pv.identifier();
                String key2 = "current.payload." + pv.identifier();
                subjectValues.put(key1, v);
                subjectValues.put(key2, v);
            }
        }
```

- [ ] **步骤 3：bucket 2 (prev payload) 支持多类型**

替换第 127-135 行：

```java
        // ── bucket 2: 本 sensorCode 的 prev payload ──
        if (prev != null && prev.properties() != null) {
            for (Map.Entry<String, Object> e : prev.properties().entrySet()) {
                Object v = e.getValue();
                if (v == null) continue;
                subjectValues.put(prefix + "prev.payload." + e.getKey(), v);
                subjectValues.put("prev.payload." + e.getKey(), v);
            }
        }
```

- [ ] **步骤 4：bucket 3 (packet.dataTime) 改为 Instant**

替换第 137-143 行：

```java
        // ── bucket 3: packet.dataTime (DATETIME，存 Instant) ──
        Instant currentInstant = Instant.ofEpochMilli(currentDataTime);
        subjectValues.put(prefix + "current.packet.dataTime", currentInstant);
        subjectValues.put("current.packet.dataTime", currentInstant);
        if (prev != null) {
            Instant prevInstant = Instant.ofEpochMilli(prev.dataTime());
            subjectValues.put(prefix + "prev.packet.dataTime", prevInstant);
            subjectValues.put("prev.packet.dataTime", prevInstant);
        }
```

- [ ] **步骤 5：bucket 4 (device.*) 拆分 onlineStatus / lastReportTime 类型**

替换第 145-156 行：

```java
        // ── bucket 4: device.* (onlineStatus=BOOLEAN, lastReportTime=DATETIME) ──
        DeviceBasicInfo dev = deviceQueryService.getBasicInfoById(event.getDeviceId());
        if (dev != null) {
            Integer online = dev.online() ? 1 : 0;
            Instant lastReport = Instant.ofEpochMilli(dev.lastReportAt());
            for (String kind : new String[]{"current", "prev"}) {
                subjectValues.put(prefix + kind + ".device.onlineStatus", online);
                subjectValues.put(prefix + kind + ".device.lastReportTime", lastReport);
                subjectValues.put(kind + ".device.onlineStatus", online);
                subjectValues.put(kind + ".device.lastReportTime", lastReport);
            }
        }
```

- [ ] **步骤 6：导入 java.time.Instant**

在 import 区追加（如尚未存在）：

```java
import java.time.Instant;
```

- [ ] **步骤 7：调用 CriteriaEvaluator 的方法签名同步**

搜索 `CriteriaEvaluator` 被调用的地方（evaluateLevel / evaluateCriteria 等方法签名中含 `Map<String, Double>` 的）。把所有 `Map<String, Double>` 改为 `Map<String, Object>`。

具体在 `CriteriaEvaluator.java` 与 `AlarmEvaluationEngine.java` 内 grep：

运行：`grep -rn "Map<String, Double>" server/zwei-iot-alarm/src/main/java/`
对每处改为 `Map<String, Object>`。

- [ ] **步骤 8：编译验证**

运行：`mvn compile -pl zwei-iot-alarm -am -q`
预期：BUILD SUCCESS。

- [ ] **步骤 9：运行全模块测试**

运行：`mvn test -pl zwei-iot-alarm -q`
预期：所有现有测试 PASS。

- [ ] **步骤 10：Commit**

```bash
git add server/zwei-iot-alarm/
git commit -m "refactor(alarm): subjectValues Map<Double→Object> + DATETIME 存 Instant + BOOLEAN 存 Integer"
```

---

## 任务 5：CriteriaEvaluator 多态分派 + CONTAINS（TDD）

**文件：**
- 修改测试：`server/zwei-iot-alarm/src/test/java/com/zwei/iot/alarm/service/engine/CriteriaEvaluatorTest.java`
- 修改实现：`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/engine/CriteriaEvaluator.java`

- [ ] **步骤 1：先写失败的新测试**

在 `CriteriaEvaluatorTest.java` 末尾追加（保留现有 11 个 normalizeSubject 测试不动）：

```java
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
        assertTrue(evaluator.evaluateCondition(c, stale));   // 10h 前 < 5h 前 ✓
        assertFalse(evaluator.evaluateCondition(c, fresh));  // 1h 前 > 5h 前 ✗
    }

    @Test
    void evaluateCondition_unknownValueType_fallbackToNumber() {
        LevelCondition c = new LevelCondition();
        c.setOperator("GT");
        c.setValueType(null);       // 未知 → 默认 NUMBER
        c.setThreshold(5.0);
        assertTrue(evaluator.evaluateCondition(c, 10.0));
    }
```

同时确保测试类有字段：
```java
private final CriteriaEvaluator evaluator = new CriteriaEvaluator();
```

如已存在，跳过。

- [ ] **步骤 2：运行测试验证失败**

运行：`mvn test -pl zwei-iot-alarm -Dtest=CriteriaEvaluatorTest -q`
预期：新增的 7 个测试中至少 6 个 FAIL（CONTAINS / 字符串 GT / 时间 LT 相对 / 等）。

- [ ] **步骤 3：重写 evaluateCondition 多态分派**

将 `CriteriaEvaluator.java` 第 209-246 行整个 `evaluateCondition` 方法替换为：

```java
    /**
     * 评估单个条件 — 按 valueType 多态分派。
     */
    boolean evaluateCondition(LevelCondition cond, Object value) {
        if (value == null || cond == null || cond.getOperator() == null) {
            log.debug("[Alarm][Criteria][Cond] 输入无效 value={} operator={} subject={}",
                    value, cond != null ? cond.getOperator() : null, cond != null ? cond.getSubject() : null);
            return false;
        }
        if (cond.getThreshold() == null) {
            log.debug("[Alarm][Criteria][Cond] threshold=null 跳过 subject={} operator={}",
                    cond.getSubject(), cond.getOperator());
            return false;
        }

        String valueType = cond.getValueType() != null ? cond.getValueType() : IndicatorValueType.NUMBER;
        switch (valueType) {
            case IndicatorValueType.STRING:   return compareString(cond, value);
            case IndicatorValueType.DATETIME: return compareDatetime(cond, value);
            case IndicatorValueType.BOOLEAN:  return compareNumber(cond, toNumber(value));
            case IndicatorValueType.NUMBER:
            default:                          return compareNumber(cond, toNumber(value));
        }
    }

    private static Double toNumber(Object v) {
        if (v instanceof Number n) return n.doubleValue();
        return null;
    }

    boolean compareNumber(LevelCondition cond, Double value) {
        if (value == null) return false;
        Object tObj = cond.getThreshold();
        if (!(tObj instanceof Number)) return false;
        double threshold = ((Number) tObj).doubleValue();
        switch (cond.getOperator().toUpperCase()) {
            case "GT":  return value > threshold;
            case "GTE": return value >= threshold;
            case "LT":  return value < threshold;
            case "LTE": return value <= threshold;
            case "EQ":  return Math.abs(value - threshold) < 0.0001;
            case "NEQ": return Math.abs(value - threshold) >= 0.0001;
            case "BETWEEN": {
                Object maxObj = cond.getThresholdMax();
                if (!(maxObj instanceof Number)) return false;
                return value >= threshold && value <= ((Number) maxObj).doubleValue();
            }
            default: return false;
        }
    }

    boolean compareString(LevelCondition cond, Object value) {
        if (!(value instanceof String) || !(cond.getThreshold() instanceof String)) return false;
        String s = (String) value;
        String t = (String) cond.getThreshold();
        switch (cond.getOperator().toUpperCase()) {
            case "CONTAINS": return s.contains(t);
            case "EQ":       return s.equals(t);
            case "NEQ":      return !s.equals(t);
            case "GT":       return s.compareTo(t) > 0;
            case "LT":       return s.compareTo(t) < 0;
            default:         return false;
        }
    }

    boolean compareDatetime(LevelCondition cond, Object value) {
        java.time.Instant v = toInstant(value);
        if (v == null) return false;
        java.time.Instant t = resolveTime(cond.getThreshold());
        if (t == null) return false;
        switch (cond.getOperator().toUpperCase()) {
            case "GT": return v.isAfter(t);
            case "LT": return v.isBefore(t);
            case "BETWEEN": {
                java.time.Instant tMax = resolveTime(cond.getThresholdMax());
                if (tMax == null) return false;
                return !v.isBefore(t) && !v.isAfter(tMax);
            }
            default: return false;
        }
    }

    private static java.time.Instant toInstant(Object v) {
        if (v instanceof java.time.Instant i) return i;
        if (v instanceof java.time.temporal.TemporalAccessor ta) return java.time.Instant.from(ta);
        if (v instanceof Number n) return java.time.Instant.ofEpochMilli(n.longValue());
        if (v instanceof String s) {
            try { return java.time.Instant.parse(s); } catch (Exception ignored) {}
        }
        return null;
    }

    private static java.time.Instant resolveTime(Object threshold) {
        if (!(threshold instanceof String s)) return null;
        if (RelativeTimeParser.isRelative(s)) {
            try { return RelativeTimeParser.resolve(s); }
            catch (Exception e) { return null; }
        }
        try { return java.time.Instant.parse(s); }
        catch (Exception e) { return null; }
    }
```

- [ ] **步骤 4：添加 import**

在 `CriteriaEvaluator.java` import 区追加（如不存在）：

```java
import com.zwei.iot.monitor.constant.IndicatorValueType;
```

确认 `zwei-iot-alarm` 的 `pom.xml` 依赖 `zwei-iot-monitor`。如未依赖，加：

```xml
<dependency>
    <groupId>com.zwei</groupId>
    <artifactId>zwei-iot-monitor</artifactId>
</dependency>
```

- [ ] **步骤 5：运行测试验证通过**

运行：`mvn test -pl zwei-iot-alarm -Dtest=CriteriaEvaluatorTest -q`
预期：18 个测试全部 PASS（11 原有 + 7 新增）。

- [ ] **步骤 6：运行全模块测试**

运行：`mvn test -pl zwei-iot-alarm -q`
预期：所有 79 个测试 PASS。

- [ ] **步骤 7：Commit**

```bash
git add server/zwei-iot-alarm/
git commit -m "feat(alarm): CriteriaEvaluator 多态分派 (NUMBER/DATETIME/STRING/BOOLEAN) + CONTAINS"
```

---

## 任务 6：前端共享 indicatorType util

**文件：**
- 创建：`web/src/utils/indicatorType.ts`
- 修改：`web/src/views/basic/MonitorType.vue`
- 修改：`web/src/api/monitorType.ts`

- [ ] **步骤 1：创建共享 util**

完整文件：

```ts
/**
 * 监测内容 indicator_type → valueType 映射
 * 与后端 com.zwei.iot.monitor.constant.IndicatorValueType 保持同步
 */

export type ValueType = 'NUMBER' | 'DATETIME' | 'STRING' | 'BOOLEAN'

export interface IndicatorTypeMeta {
  code: string
  name: string
  unit: string
  valueType: ValueType
}

export const INDICATOR_TYPE_META: Record<string, IndicatorTypeMeta> = {
  wy:  { code: 'wy',  name: '位移',   unit: 'mm',   valueType: 'NUMBER' },
  wd:  { code: 'wd',  name: '温度',   unit: '℃',    valueType: 'NUMBER' },
  jd:  { code: 'jd',  name: '角度',   unit: '°',    valueType: 'NUMBER' },
  yl:  { code: 'yl',  name: '压力',   unit: 'MPa',  valueType: 'NUMBER' },
  sw:  { code: 'sw',  name: '水位',   unit: 'm',    valueType: 'NUMBER' },
  jsd: { code: 'jsd', name: '加速度', unit: 'm/s²', valueType: 'NUMBER' },
  hsl: { code: 'hsl', name: '含水率', unit: '%',    valueType: 'NUMBER' },
  ljn: { code: 'ljn', name: '力矩',   unit: 'N/m²', valueType: 'NUMBER' },
  zdl: { code: 'zdl', name: '震动频率', unit: 'Hz',  valueType: 'NUMBER' },
  dl:  { code: 'dl',  name: '电量',   unit: 'V',    valueType: 'NUMBER' },
  dx:  { code: 'dx',  name: '断线',   unit: '',     valueType: 'BOOLEAN' },
  sg:  { code: 'sg',  name: '声光',   unit: '',     valueType: 'STRING' },
  sp:  { code: 'sp',  name: '视频',   unit: '',     valueType: 'STRING' },
}

export const INDICATOR_TYPE_OPTIONS = Object.values(INDICATOR_TYPE_META)

/** 返回 indicator_type code 对应的 valueType；未知返回 NUMBER */
export function getValueType(code?: string): ValueType {
  if (!code) return 'NUMBER'
  return INDICATOR_TYPE_META[code.trim().toLowerCase()]?.valueType ?? 'NUMBER'
}

export function indicatorTypeLabel(code?: string): string {
  if (!code) return ''
  return INDICATOR_TYPE_META[code.trim().toLowerCase()]?.name ?? ''
}
```

- [ ] **步骤 2：修改 MonitorType.vue 用新 util**

定位 `web/src/views/basic/MonitorType.vue:381-402`，删除 `const IndicatorTypeEnum = {...} as const` / `indicatorTypeOptions` / `indicatorTypeLabel` 三段。

在 `<script setup>` 区追加 import：
```ts
import { INDICATOR_TYPE_OPTIONS as indicatorTypeOptions, indicatorTypeLabel } from '@/utils/indicatorType'
```

- [ ] **步骤 3：修改 api/monitorType.ts**

在 `MonitorContentItem` 接口（第 4-16 行）末尾追加：
```ts
  /** 数据类型 (前端从 indicator_type 派生，后端不持久化) */
  valueType?: 'NUMBER' | 'DATETIME' | 'STRING' | 'BOOLEAN'
```

- [ ] **步骤 4：类型检查**

运行：`cd web && npm run build -- --noEmit 2>&1 | head -20`
（或直接 `npx vue-tsc --noEmit`）
预期：无 TS 错误。

- [ ] **步骤 5：Commit**

```bash
git add web/src/utils/indicatorType.ts web/src/views/basic/MonitorType.vue web/src/api/monitorType.ts
git commit -m "refactor(web): 抽出 indicatorType 共享 util + 加 valueType 字段"
```

---

## 任务 7：useIndicatorTree 中文回显 + valueType 注入

**文件：**
- 修改：`web/src/views/alarm/composables/useIndicatorTree.ts`

- [ ] **步骤 1：扩展 IndicatorTreeNode 与 Condition 类型**

第 4-12 行 `IndicatorTreeNode` 接口的 `meta` 改为：
```ts
  meta?: { subjectType: string; valueKind?: string; valueType?: ValueType }
```

第 14-20 行 `Condition` 接口扩展：
```ts
export interface Condition {
  subject: string
  subjectType?: 'CONTENT' | 'DEVICE' | 'PACKET'
  valueType?: ValueType
  operator: string
  threshold: number | string | boolean
  thresholdMax?: number | string
  unit?: string
  /** DATETIME 编辑态字段（仅前端用，序列化时合并入 threshold 字符串） */
  thresholdMode?: 'ABSOLUTE' | 'RELATIVE'
  relDirection?: '+' | '-'
  relValue?: number
  relUnit?: 's' | 'm' | 'h' | 'd'
}
```

文件顶部 import 追加：
```ts
import type {ValueType} from '@/utils/indicatorType'
```

- [ ] **步骤 2：重写 prefixDisplayLabels 用中文 labelPrefix**

替换原第 73-86 行：

```ts
/** 深拷贝节点树:
 *  - 叶子节点 (disabled=false): value 加 valuePrefix, displayLabel 设为 labelPrefix + ownLabel (中文路径)
 *  - 中间分组节点 (disabled=true): 保持自身 label 不变，递归处理子节点，labelPrefix 累加自身 label
 */
function prefixDisplayLabels(nodes: IndicatorTreeNode[], valuePrefix: string, labelPrefix: string): IndicatorTreeNode[] {
  return nodes.map(n => {
    const copy: IndicatorTreeNode = {...n}
    const ownLabel = n.displayLabel || n.label
    if (!n.disabled) {
      copy.value = `${valuePrefix}.${n.value}`
      copy.displayLabel = labelPrefix ? `${labelPrefix} / ${ownLabel}` : ownLabel
    } else if (n.children) {
      const newLabelPrefix = labelPrefix ? `${labelPrefix} / ${ownLabel}` : ownLabel
      copy.children = prefixDisplayLabels(n.children, valuePrefix, newLabelPrefix)
    }
    return copy
  })
}
```

- [ ] **步骤 3：buildDimensionChildren 加 valueType**

替换原第 38-68 行整段 `buildDimensionChildren` 函数：

```ts
function buildDimensionChildren(contents: MonitorContentItem[], valueKind: 'current' | 'prev' = 'current'): IndicatorTreeNode[] {
  const payloadChildren: IndicatorTreeNode[] = (contents || []).map(c => {
    const vt = getValueType(c.indicatorType)
    const shortLabel = c.unit ? `${c.name} (${c.unit})` : c.name
    return {
      value: `payload.${c.code}`,
      label: shortLabel,
      displayLabel: shortLabel,
      unit: c.unit || undefined,
      meta: {subjectType: 'CONTENT' as const, valueKind, valueType: vt},
    }
  })
  return [
    {
      value: 'payload', label: '数据载荷信息', displayLabel: '数据载荷信息', disabled: true,
      children: payloadChildren,
    },
    {
      value: 'device', label: '设备基础信息', displayLabel: '设备基础信息', disabled: true,
      children: [
        {value: 'device.onlineStatus', label: '在线状态', displayLabel: '在线状态',
          meta: {subjectType: 'DEVICE' as const, valueKind, valueType: 'BOOLEAN'}},
        {value: 'device.lastReportTime', label: '最后上报时间', displayLabel: '最后上报时间',
          meta: {subjectType: 'DEVICE' as const, valueKind, valueType: 'DATETIME'}},
      ],
    },
    {
      value: 'packet', label: '数据包信息', displayLabel: '数据包信息', disabled: true,
      children: [
        {value: 'packet.dataTime', label: '数据时间', displayLabel: '数据时间',
          meta: {subjectType: 'PACKET' as const, valueKind, valueType: 'DATETIME'}},
      ],
    },
  ]
}
```

import 区追加：
```ts
import {getValueType} from '@/utils/indicatorType'
```

- [ ] **步骤 4：buildFromMonitorType 传中文 labelPrefix**

原第 106-124 行 buildFromMonitorType 内 children 数组改为：

```ts
      const tree: IndicatorTreeNode[] = [
        {
          value: 'current', label: '当前值', displayLabel: '当前值', disabled: true,
          children: prefixDisplayLabels(buildDimensionChildren(contents, 'current'), 'current', '当前值'),
        },
        {
          value: 'prev', label: '上一值', displayLabel: '上一值', disabled: true,
          children: prefixDisplayLabels(buildDimensionChildren(contents, 'prev'), 'prev', '上一值'),
        },
      ]
```

- [ ] **步骤 5：buildFromSensors 传 sensorName 作为 labelPrefix**

原第 139-165 行的 tree 构造与外层 prefixDisplayLabels 改为：

```ts
    const tree: IndicatorTreeNode[] = sensors.map(s => {
      const contents = seenTypes.get(s.monitorTypeId) || []
      return {
        value: s.sensorCode,
        label: s.sensorName,
        displayLabel: s.sensorName,
        disabled: true,
        children: [
          {
            value: 'current', label: '当前值', displayLabel: '当前值', disabled: true,
            children: prefixDisplayLabels(buildDimensionChildren(contents, 'current'), 'current', '当前值'),
          },
          {
            value: 'prev', label: '上一值', displayLabel: '上一值', disabled: true,
            children: prefixDisplayLabels(buildDimensionChildren(contents, 'prev'), 'prev', '上一值'),
          },
        ],
      } satisfies IndicatorTreeNode
    })

    // 外层再以 sensorCode 为 valuePrefix、sensorName 为 labelPrefix 应用一次（叠加传感器名前缀）
    const prefixedTree = tree.map(sensorNode => ({
      ...sensorNode,
      children: prefixDisplayLabels(sensorNode.children!, sensorNode.value, sensorNode.label),
    }))
```

- [ ] **步骤 6：类型检查**

运行：`cd web && npx vue-tsc --noEmit 2>&1 | head -30`
预期：无错误。

- [ ] **步骤 7：Commit**

```bash
git add web/src/views/alarm/composables/useIndicatorTree.ts
git commit -m "feat(alarm-web): useIndicatorTree 中文回显 + meta.valueType 注入"
```

---

## 任务 8：ConditionRow.vue 控件分化 + 宽度自适应 + DATETIME 模式切换

**文件：**
- 修改：`web/src/views/alarm/components/ConditionRow.vue`

- [ ] **步骤 1：完整重写模板**

替换 `<template>` 区（第 1-42 行）：

```vue
<template>
  <div class="condition-row">
    <el-tree-select
        :model-value="condition.subject"
        :data="indicatorTree"
        placeholder="选择指标"
        filterable
        check-strictly
        size="small"
        class="cond-field subject-field"
        :style="{width: subjectWidth}"
        :render-after-expand="false"
        node-key="value"
        :props="{ children: 'children', label: 'displayLabel', value: 'value', disabled: 'disabled' }"
        @update:model-value="onSubjectChange"
    />

    <el-select
        :model-value="condition.operator"
        size="small"
        class="cond-field operator-field"
        @update:model-value="(v: string) => updateField('operator', v)"
    >
      <el-option v-for="op in currentOperators" :key="op.value" :label="op.label" :value="op.value"/>
    </el-select>

    <!-- NUMBER 单值 -->
    <el-input-number
        v-if="valueType === 'NUMBER' && condition.operator !== 'BETWEEN'"
        :model-value="Number(condition.threshold) || 0"
        size="small"
        :precision="2"
        :step="0.1"
        controls-position="right"
        class="cond-field threshold-field"
        @update:model-value="(v: number | null) => updateField('threshold', v ?? 0)"
    />
    <!-- NUMBER BETWEEN: 两个 input-number -->
    <template v-else-if="valueType === 'NUMBER' && condition.operator === 'BETWEEN'">
      <el-input-number
          :model-value="Number(condition.threshold) || 0"
          size="small"
          :precision="2"
          :step="0.1"
          controls-position="right"
          class="cond-field threshold-field"
          @update:model-value="(v: number | null) => updateField('threshold', v ?? 0)"
      />
      <span class="cond-tilde">~</span>
      <el-input-number
          :model-value="Number(condition.thresholdMax) || 0"
          size="small"
          :precision="2"
          :step="0.1"
          controls-position="right"
          class="cond-field threshold-field"
          @update:model-value="(v: number | null) => updateField('thresholdMax', v ?? 0)"
      />
    </template>

    <!-- DATETIME -->
    <template v-else-if="valueType === 'DATETIME'">
      <el-select
          :model-value="condition.thresholdMode || 'ABSOLUTE'"
          size="small"
          class="cond-field mode-field"
          @update:model-value="(v: string) => updateField('thresholdMode', v as 'ABSOLUTE'|'RELATIVE')"
      >
        <el-option label="绝对" value="ABSOLUTE"/>
        <el-option label="相对" value="RELATIVE"/>
      </el-select>
      <el-date-picker
          v-if="(condition.thresholdMode || 'ABSOLUTE') === 'ABSOLUTE' && condition.operator !== 'BETWEEN'"
          :model-value="condition.threshold as string"
          type="datetime"
          size="small"
          value-format="YYYY-MM-DDTHH:mm:ss"
          class="cond-field threshold-field"
          @update:model-value="(v: string) => updateField('threshold', v)"
      />
      <el-date-picker
          v-else-if="(condition.thresholdMode || 'ABSOLUTE') === 'ABSOLUTE' && condition.operator === 'BETWEEN'"
          :model-value="datetimeRange"
          type="datetimerange"
          size="small"
          value-format="YYYY-MM-DDTHH:mm:ss"
          class="cond-field threshold-range-field"
          @update:model-value="onDatetimeRangeChange"
      />
      <!-- 相对模式 -->
      <template v-else>
        <span class="cond-now-label">当前时间</span>
        <el-select
            :model-value="condition.relDirection || '-'"
            size="small"
            class="cond-field rel-dir-field"
            @update:model-value="(v: string) => updateField('relDirection', v as '+'|'-')"
        >
          <el-option label="-" value="-"/>
          <el-option label="+" value="+"/>
        </el-select>
        <el-input-number
            :model-value="condition.relValue || 0"
            size="small"
            :min="0"
            :step="1"
            controls-position="right"
            class="cond-field rel-value-field"
            @update:model-value="(v: number | null) => updateField('relValue', v ?? 0)"
        />
        <el-select
            :model-value="condition.relUnit || 'h'"
            size="small"
            class="cond-field rel-unit-field"
            @update:model-value="(v: string) => updateField('relUnit', v as 's'|'m'|'h'|'d')"
        >
          <el-option label="秒" value="s"/>
          <el-option label="分" value="m"/>
          <el-option label="时" value="h"/>
          <el-option label="天" value="d"/>
        </el-select>
      </template>
    </template>

    <!-- STRING -->
    <el-input
        v-else-if="valueType === 'STRING'"
        :model-value="String(condition.threshold || '')"
        size="small"
        class="cond-field threshold-field"
        placeholder="输入字符串"
        @update:model-value="(v: string) => updateField('threshold', v)"
    />

    <!-- BOOLEAN -->
    <el-select
        v-else-if="valueType === 'BOOLEAN'"
        :model-value="Number(condition.threshold) || 0"
        size="small"
        class="cond-field threshold-field"
        @update:model-value="(v: number) => updateField('threshold', v)"
    >
      <el-option :label="booleanTrueLabel" :value="1"/>
      <el-option :label="booleanFalseLabel" :value="0"/>
    </el-select>

    <!-- 兜底: NUMBER 单值（valueType 为空时） -->
    <el-input-number
        v-else
        :model-value="Number(condition.threshold) || 0"
        size="small"
        :precision="2"
        :step="0.1"
        controls-position="right"
        class="cond-field threshold-field"
        @update:model-value="(v: number | null) => updateField('threshold', v ?? 0)"
    />

    <span v-if="condition.unit && valueType === 'NUMBER'" class="cond-unit">{{ condition.unit }}</span>
    <el-button size="small" type="danger" text @click="$emit('remove')">
      <el-icon><Delete/></el-icon>
    </el-button>
  </div>
</template>
```

- [ ] **步骤 2：重写 script**

替换 `<script setup lang="ts">` 区（第 45-82 行）：

```ts
<script setup lang="ts">
import {computed} from 'vue'
import {Delete} from '@element-plus/icons-vue'
import type {IndicatorTreeNode, Condition} from '../composables/useIndicatorTree'
import type {ValueType} from '@/utils/indicatorType'

const props = defineProps<{
  condition: Condition
  indicatorTree: IndicatorTreeNode[]
  nodeMap: Map<string, IndicatorTreeNode>
}>()

const emit = defineEmits<{
  'update:condition': [c: Condition]
  remove: []
}>()

interface Op { value: string; label: string }

const OPERATOR_SETS: Record<ValueType, Op[]> = {
  NUMBER: [
    {value: 'GT', label: '>'},
    {value: 'GTE', label: '>='},
    {value: 'LT', label: '<'},
    {value: 'LTE', label: '<='},
    {value: 'EQ', label: '=='},
    {value: 'NEQ', label: '!='},
    {value: 'BETWEEN', label: '介于'},
  ],
  DATETIME: [
    {value: 'GT', label: '晚于'},
    {value: 'LT', label: '早于'},
    {value: 'BETWEEN', label: '介于时段'},
  ],
  STRING: [
    {value: 'CONTAINS', label: '包含'},
    {value: 'EQ', label: '等于'},
    {value: 'NEQ', label: '不等于'},
    {value: 'GT', label: '大于(ASCII)'},
    {value: 'LT', label: '小于(ASCII)'},
  ],
  BOOLEAN: [
    {value: 'EQ', label: '等于'},
    {value: 'NEQ', label: '不等于'},
  ],
}

const currentNode = computed(() => props.nodeMap.get(props.condition.subject))
const valueType = computed<ValueType>(() => currentNode.value?.meta?.valueType || 'NUMBER')

const currentOperators = computed(() => OPERATOR_SETS[valueType.value] || OPERATOR_SETS.NUMBER)

const subjectWidth = computed(() => {
  const label = currentNode.value?.displayLabel || ''
  return `${Math.max(200, Math.min(500, label.length * 14 + 40))}px`
})

const booleanTrueLabel = computed(() => {
  const s = props.condition.subject || ''
  return s.endsWith('onlineStatus') ? '在线' : '是'
})
const booleanFalseLabel = computed(() => {
  const s = props.condition.subject || ''
  return s.endsWith('onlineStatus') ? '离线' : '否'
})

const datetimeRange = computed<[string, string] | null>(() => {
  const a = props.condition.threshold as string
  const b = props.condition.thresholdMax as string
  return a && b ? [a, b] : null
})

function onDatetimeRangeChange(v: [string, string] | null) {
  const updated: Condition = {
    ...props.condition,
    threshold: v?.[0] ?? '',
    thresholdMax: v?.[1] ?? '',
  }
  emit('update:condition', updated)
}

function onSubjectChange(val: string) {
  const node = props.nodeMap.get(val)
  const vt = (node?.meta?.valueType as ValueType) || 'NUMBER'
  const updated: Condition = {
    ...props.condition,
    subject: val,
    subjectType: (node?.meta?.subjectType as Condition['subjectType']) || undefined,
    valueType: vt,
    unit: node?.unit || undefined,
    // 切换 subject 时重置 operator 到该类型允许的第一个
    operator: OPERATOR_SETS[vt]?.[0]?.value || 'GT',
    // DATETIME 默认绝对模式
    thresholdMode: vt === 'DATETIME' ? 'ABSOLUTE' : undefined,
    threshold: vt === 'NUMBER' ? 0 : vt === 'BOOLEAN' ? 1 : '',
  }
  emit('update:condition', updated)
}

function updateField(field: string, value: any) {
  const updated: Condition = {...props.condition, [field]: value}
  // 相对模式编辑时同步序列化 threshold 字符串
  if (field === 'relDirection' || field === 'relValue' || field === 'relUnit') {
    const dir = (updated.relDirection || '-') as '+' | '-'
    const n = updated.relValue || 0
    const unit = updated.relUnit || 'h'
    updated.threshold = n > 0 ? `now${dir}${n}${unit}` : 'now'
  }
  // 切换 operator 到 BETWEEN 时初始化 thresholdMax
  if (field === 'operator' && value === 'BETWEEN' && updated.thresholdMax === undefined) {
    if (updated.valueType === 'NUMBER') updated.thresholdMax = 0
    else if (updated.valueType === 'DATETIME') updated.thresholdMax = ''
  }
  emit('update:condition', updated)
}
</script>
```

- [ ] **步骤 3：更新样式（CSS）**

替换 `<style scoped>` 区（第 84-114 行）：

```css
.condition-row {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
  flex-wrap: wrap;
}

.cond-field { flex-shrink: 0; }
.subject-field { min-width: 200px; }
.operator-field { width: 96px; }
.threshold-field { width: 180px; }
.threshold-range-field { width: 320px; }
.mode-field { width: 72px; }
.rel-dir-field { width: 56px; }
.rel-value-field { width: 88px; }
.rel-unit-field { width: 64px; }

.cond-unit {
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
  min-width: 24px;
}

.cond-tilde {
  font-size: 13px;
  color: #606266;
  padding: 0 2px;
}

.cond-now-label {
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
}
```

- [ ] **步骤 4：类型检查**

运行：`cd web && npx vue-tsc --noEmit 2>&1 | head -30`
预期：无错误。

- [ ] **步骤 5：Commit**

```bash
git add web/src/views/alarm/components/ConditionRow.vue
git commit -m "feat(alarm-web): ConditionRow 按 valueType 分化控件 + 宽度自适应 + DATETIME 相对模式"
```

---

## 任务 9：AlarmCriteria.vue 序列化适配 + 反序列化回显

**文件：**
- 修改：`web/src/views/alarm/AlarmCriteria.vue`

- [ ] **步骤 1：搜索现有 Condition 序列化代码**

运行：`grep -n "threshold\|migrateToGroups\|buildLevelConfig\|condition.subject" web/src/views/alarm/AlarmCriteria.vue`

定位序列化与反序列化（加载已存在判据时）逻辑位置。

- [ ] **步骤 2：反序列化（回显时拆解 threshold 字符串）**

在解析 level_config 后、回填到表单前，遍历每个 condition 做拆解。新增辅助函数：

```ts
function hydrateCondition(c: Condition): Condition {
  if (c.valueType !== 'DATETIME' || !c.threshold) return c
  const t = String(c.threshold)
  if (t.startsWith('now')) {
    const match = /^now([+-])(\d+)([smhd])$/.exec(t)
    if (match) {
      return {
        ...c,
        thresholdMode: 'RELATIVE',
        relDirection: match[1] as '+' | '-',
        relValue: Number(match[2]),
        relUnit: match[3] as 's' | 'm' | 'h' | 'd',
      }
    }
    return {...c, thresholdMode: 'RELATIVE', relDirection: '-', relValue: 0, relUnit: 'h'}
  }
  return {...c, thresholdMode: 'ABSOLUTE'}
}
```

在回显（编辑模式加载）时对每个 condition 调用 `hydrateCondition`。

- [ ] **步骤 3：序列化（保存时合并 rel* 到 threshold）**

保存前遍历每个 condition：

```ts
function serializeCondition(c: Condition): Condition {
  if (c.valueType === 'DATETIME' && c.thresholdMode === 'RELATIVE') {
    const dir = c.relDirection || '-'
    const n = c.relValue || 0
    const unit = c.relUnit || 'h'
    const {thresholdMode, relDirection, relValue, relUnit, ...rest} = c
    return {...rest, threshold: n > 0 ? `now${dir}${n}${unit}` : 'now'}
  }
  // 删除编辑态字段
  const {thresholdMode, relDirection, relValue, relUnit, ...rest} = c
  return rest
}
```

保存到 `level_config` 前对每个 condition 调用 `serializeCondition`。

- [ ] **步骤 4：类型检查**

运行：`cd web && npx vue-tsc --noEmit 2>&1 | head -30`
预期：无错误。

- [ ] **步骤 5：Commit**

```bash
git add web/src/views/alarm/AlarmCriteria.vue
git commit -m "feat(alarm-web): AlarmCriteria DATETIME 相对时间序列化/反序列化"
```

---

## 任务 10：端到端验证

**文件：** 无（验证任务）

- [ ] **步骤 1：后端全模块构建**

运行：`cd server && mvn clean package -DskipTests -pl zwei-admin -am -q`
预期：BUILD SUCCESS（18 modules）。

- [ ] **步骤 2：重启后端**

```bash
# 关掉旧进程
taskkill //F //IM java.exe 2>&1 || true
# 启动（在 server/ 目录下）
java -jar -Dspring.profiles.active=local zwei-admin/target/zwei-admin.jar &
```

预期：约 15s 后看到 `Started RuoYiApplication`，无异常。

- [ ] **步骤 3：前端类型检查 + Vite 启动**

运行：`cd web && npm run build 2>&1 | tail -10`
预期：vue-tsc 类型检查通过 + 构建成功。

启动 dev：
```bash
cd web && npm run dev &
```

- [ ] **步骤 4：手工验证（按规格测试场景）**

打开浏览器 `http://localhost:5173/`，进入「告警 > 判据」编辑一条判据，依次验证：

1. ✅ 监测类型模式选「当前值 / 数据载荷信息 / 小时雨量」→ 回显 `当前值 / 数据载荷信息 / 小时雨量 (mm)`
2. ✅ 传感器模式选「雨量计 SNS001 / 上一值 / 设备基础信息 / 最后上报时间」→ 回显全中文 + DATETIME 控件 + 「晚于」operator
3. ✅ 操作符切到「介于」→ 出现两个 input-number（NUMBER）或 datetimerange（DATETIME）
4. ✅ 选 `device.onlineStatus` → 控件变 el-select 在线/离线
5. ✅ 单位 span 在 NUMBER 显示，DATETIME/STRING/BOOLEAN 不显示
6. ✅ 长路径 → 指标框宽度撑开至 500px 上限
7. ✅ DATETIME 模式切「相对」→ 控件变 `[当前时间][−][5][小时▼]`，保存后回看仍正确
8. ✅ 保存判据后查 alarm_criteria 表 level_config，DATETIME 相对条件 threshold 为 `now-5h`

- [ ] **步骤 5：运行后端告警评估用例（可选）**

模拟一条数据触发告警，查看 alarm_record 表是否有新记录、SSE 是否推送。

- [ ] **步骤 6：Commit（如果有任何修复）**

如步骤 1-5 发现问题并修复了，单独 commit；否则跳过。

---

## 自检结果

**规格覆盖度**：

| 规格章节 | 任务覆盖 |
|---|---|
| valueType 集合与映射（indicator_type → valueType） | 任务 1（BE 常量）+ 任务 6（FE util） |
| 操作符与控件分化 | 任务 8（ConditionRow.vue） |
| 相对时间语法 | 任务 2（RelativeTimeParser BE）+ 任务 8（FE 控件）+ 任务 9（序列化） |
| 数据模型变更（Condition/LevelCondition） | 任务 3（BE）+ 任务 7（FE type） |
| 后端改动清单（4 文件） | 任务 1 + 3 + 4 + 5 |
| 前端改动清单（6 文件） | 任务 6 + 7 + 8 + 9 |
| 兼容性（未知 code → NUMBER） | 任务 1 of() 默认 + 任务 5 evaluateCondition default 分支 |
| 测试策略 | 任务 2 + 任务 5（BE TDD）+ 任务 10（FE 手工） |

**占位符扫描**：✅ 无 TODO / 待定 / "类似任务 N"；每个步骤都有具体代码。

**类型一致性**：
- `LevelCondition.threshold`: Object（任务 3 设定，任务 5 使用）✅
- `Map<String, Object>` subjectValues（任务 4 设定，任务 5 消费）✅
- `IndicatorTreeNode.meta.valueType`: 任务 7 设定，任务 8 使用 ✅
- `Condition.thresholdMode/relDirection/relValue/relUnit`: 任务 7 类型定义、任务 8 编辑、任务 9 序列化 ✅
- `RelativeTimeParser.resolve/isRelative`: 任务 2 定义、任务 5 使用 ✅
- 前后端 valueType 字符串值（NUMBER/DATETIME/STRING/BOOLEAN）：任务 1 与任务 6 一致 ✅

---

## 执行交接

计划已完成并保存到 `docs/superpowers/plans/2026-06-23-grouped-rule-builder-ui.md`。两种执行方式：

**1. 子代理驱动（推荐）** - 每个任务调度一个新的子代理，任务间进行审查，快速迭代

**2. 内联执行** - 在当前会话中使用 executing-plans 执行任务，批量执行并设有检查点

选哪种方式？
