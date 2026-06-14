# zwei-iot-timeseries 查询能力增强 — 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 `zwei-iot-timeseries` 模块的查询能力从"基础时序"升级为通用可配置基础设施,支持白名单聚合函数、表达式组合、数值范围筛选、完整度、趋势分析。

**Architecture:** 渐进式增强,3 层 service 并列: `MonitorDataAggregationService` (聚合 + delta) + `MonitorDataAnalysisService` (完整度 + 趋势) + 现有 `MonitorDataQueryService` (latest/page/chart 保持不动)。`IotdbTimeSeriesService` 增强底层 SQL 能力,新增 6 个公共方法 + 私有 `ExpressionSpecRenderer`。

**Tech Stack:** Java 17 + Spring Boot 4.0.3 + IoTDB 2.0+ (JDBC 1.3.4 驱动) + JUnit 5 + Mockito + AssertJ + Testcontainers (IoTDB 2.0.2)

**Reference Spec:** `docs/superpowers/specs/2026-06-14-timeseries-query-enhancement-design.md`

**分两阶段交付:**
- **阶段 1(任务 1-11):** 基础设施 — Domain + IotdbTimeSeriesService 增强
- **阶段 2(任务 12-16):** API 层 — Service + Controller + 集成测试 + 文档

---

## 文件结构总览

```
server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/
├── domain/
│   ├── AggregationFunction.java          [新]  枚举,白名单
│   ├── ExpressionSpec.java               [新]  sealed interface + 3 子 record + BinaryOperator
│   ├── ExpressionSpecRenderer.java       [新]  渲染 ExpressionSpec → SQL 字符串
│   ├── TimeWindowSpec.java               [新]  record + WindowGranularity 嵌套枚举
│   ├── AggregationResultVO.java          [新]  record
│   ├── SensorAggregationVO.java          [新]  record
│   ├── CompletenessReportVO.java         [新]  record
│   └── TrendReportVO.java                [新]  record
├── service/
│   ├── IotdbTimeSeriesService.java       [改]  新增 6 公共方法 + 私有 renderExpression
│   ├── MonitorDataAggregationService.java [新]  聚合 + delta
│   └── MonitorDataAnalysisService.java    [新]  完整度 + 趋势
└── controller/
    └── MonitorDataSensorController.java   [新]  5 端点

server/zwei-iot-timeseries/src/test/java/com/zwei/iot/timeseries/
├── domain/
│   ├── AggregationFunctionTest.java
│   ├── ExpressionSpecRendererTest.java
│   └── TimeWindowSpecTest.java
├── service/
│   ├── IotdbTimeSeriesServiceQueryTest.java
│   ├── MonitorDataAggregationServiceTest.java
│   ├── MonitorDataAnalysisServiceTest.java
│   └── MonitorDataSensorControllerTest.java
└── integration/
    └── MonitorDataQueryIntegrationIT.java [Testcontainers]

server/zwei-iot-timeseries/CLAUDE.md  [改]  新增"查询能力矩阵"+"ExpressionSpec DSL"小节
CLAUDE.md                              [改]  模块索引表 +2
.claude/index.json                     [改]  service 索引 +2
```

---

# 阶段 1:基础设施(任务 1-11)

## Task 1: `AggregationFunction` 枚举(白名单)

**Files:**
- Create: `server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/domain/AggregationFunction.java`
- Create: `server/zwei-iot-timeseries/src/test/java/com/zwei/iot/timeseries/domain/AggregationFunctionTest.java`

- [ ] **Step 1: 写失败测试 `AggregationFunctionTest`**

```java
package com.zwei.iot.timeseries.domain;

import org.junit.jupiter.api.DisplayName;
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

    @ParameterizedTest
    @CsvSource({
        "P50, QUANTILE(value, 0.5)",
        "P95, QUANTILE(value, 0.95)",
        "P99, QUANTILE(value, 0.99)",
    })
    @DisplayName("getIotdbExpr — 百分位带 quantile 参数")
    void getIotdbExpr_percentile(String name, String expected) {
        AggregationFunction func = AggregationFunction.valueOf(name);
        assertThat(func.getIotdbExpr("value")).isEqualTo(expected);
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
```

- [ ] **Step 2: 跑测试,确认失败**

```bash
cd D:/Code/Projects/geo_hazard_monitor/server
mvn -pl zwei-iot-timeseries -Dtest=AggregationFunctionTest test
```

预期: **编译失败** — `AggregationFunction` 类不存在

- [ ] **Step 3: 写最小实现 `AggregationFunction.java`**

```java
package com.zwei.iot.timeseries.domain;

/**
 * 聚合函数白名单枚举。
 *
 * <p>封闭枚举,SQL 拼接只能从这里取值,无法注入任意函数。
 * 支持普通聚合 (AVG/MAX/MIN/SUM/COUNT/FIRST_VALUE/LAST_VALUE/EXTREME/STDDEV)
 * 与百分位 (P50/P95/P99,IoTDB 用 QUANTILE 实现)。</p>
 */
public enum AggregationFunction {
    AVG("AVG", null),
    MAX("MAX", null),
    MIN("MIN", null),
    SUM("SUM", null),
    COUNT("COUNT", null),
    FIRST_VALUE("FIRST_VALUE", null),
    LAST_VALUE("LAST_VALUE", null),
    EXTREME("EXTREME", null),
    STDDEV("STDDEV", null),
    P50("QUANTILE", 0.5),
    P95("QUANTILE", 0.95),
    P99("QUANTILE", 0.99);

    private final String iotdbFunc;
    private final Double quartileParam;

    AggregationFunction(String iotdbFunc, Double quartileParam) {
        this.iotdbFunc = iotdbFunc;
        this.quartileParam = quartileParam;
    }

    /**
     * 渲染为 IoTDB 表达式字符串。
     *
     * @param attrCode 业务指标编码,必须已通过 IotdbPathResolver 校验
     * @return IoTDB 表达式,如 {@code AVG(value)} 或 {@code QUANTILE(value, 0.95)}
     */
    public String getIotdbExpr(String attrCode) {
        if (quartileParam != null) {
            return "QUANTILE(" + attrCode + ", " + quartileParam + ")";
        }
        return iotdbFunc + "(" + attrCode + ")";
    }

    public boolean needsQuartileParam() {
        return quartileParam != null;
    }
}
```

- [ ] **Step 4: 跑测试,确认通过**

```bash
mvn -pl zwei-iot-timeseries -Dtest=AggregationFunctionTest test
```

预期: **测试通过**,BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
cd D:/Code/Projects/geo_hazard_monitor
git add server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/domain/AggregationFunction.java
git add server/zwei-iot-timeseries/src/test/java/com/zwei/iot/timeseries/domain/AggregationFunctionTest.java
git commit -m "feat(timeseries): 新增 AggregationFunction 白名单枚举"
```

---

## Task 2: `ExpressionSpec` sealed interface

**Files:**
- Create: `server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/domain/ExpressionSpec.java`

- [ ] **Step 1: 写失败测试 `ExpressionSpecTest`(嵌套类)**

```java
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
```

- [ ] **Step 2: 跑测试,确认失败**

```bash
cd D:/Code/Projects/geo_hazard_monitor/server
mvn -pl zwei-iot-timeseries -Dtest=ExpressionSpecTest test
```

预期: **编译失败** — `ExpressionSpec` 不存在

- [ ] **Step 3: 写最小实现 `ExpressionSpec.java`**

```java
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
```

- [ ] **Step 4: 跑测试,确认通过**

```bash
mvn -pl zwei-iot-timeseries -Dtest=ExpressionSpecTest test
```

预期: **测试通过**

- [ ] **Step 5: Commit**

```bash
cd D:/Code/Projects/geo_hazard_monitor
git add server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/domain/ExpressionSpec.java
git add server/zwei-iot-timeseries/src/test/java/com/zwei/iot/timeseries/domain/ExpressionSpecTest.java
git commit -m "feat(timeseries): 新增 ExpressionSpec sealed interface DSL"
```

---

## Task 3: `ExpressionSpecRenderer` 渲染器

**Files:**
- Create: `server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/domain/ExpressionSpecRenderer.java`
- Create: `server/zwei-iot-timeseries/src/test/java/com/zwei/iot/timeseries/domain/ExpressionSpecRendererTest.java`

- [ ] **Step 1: 写失败测试 `ExpressionSpecRendererTest`**

```java
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
        // 构造 6 层嵌套
        ExpressionSpec expr = new Constant(1.0);
        for (int i = 0; i < 6; i++) {
            expr = new BinaryOp(expr, ADD, new Constant(1.0));
        }
        assertThatThrownBy(() -> renderer.render(expr, "value"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("嵌套过深");
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
        // 构造超长别名:6 层嵌套
        ExpressionSpec expr = new Constant(1.0);
        for (int i = 0; i < 6; i++) {
            expr = new BinaryOp(expr, ADD, new Constant(1.0));
        }
        assertThatThrownBy(() -> renderer.alias(expr))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("别名");
    }
}
```

- [ ] **Step 2: 跑测试,确认失败**

```bash
cd D:/Code/Projects/geo_hazard_monitor/server
mvn -pl zwei-iot-timeseries -Dtest=ExpressionSpecRendererTest test
```

预期: **编译失败** — `ExpressionSpecRenderer` 不存在

- [ ] **Step 3: 写最小实现 `ExpressionSpecRenderer.java`**

```java
package com.zwei.iot.timeseries.domain;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * 表达式渲染器 — 把 {@link ExpressionSpec} 树转为 IoTDB SQL 字符串 + 别名。
 *
 * <p>渲染方法 {@link #render} 递归遍历表达式树,拼出 IoTDB 表达式片段;
 * {@link #alias} 同步生成可作为 ResultSet 列名 + 返回 Map key 的别名。</p>
 */
@Component
public class ExpressionSpecRenderer {

    private static final int MAX_DEPTH = 5;
    private static final int MAX_ALIAS_LENGTH = 64;
    private static final Pattern ALIAS_ALLOWED = Pattern.compile("[a-zA-Z0-9_\\-()/]*");

    /**
     * 递归渲染为 IoTDB 表达式。
     *
     * @param expr     表达式树
     * @param attrCode 业务指标编码
     * @return IoTDB 表达式字符串,带括号
     * @throws IllegalArgumentException 嵌套深度超过 5 层
     */
    public String render(ExpressionSpec expr, String attrCode) {
        return render(expr, attrCode, 0);
    }

    private String render(ExpressionSpec expr, String attrCode, int depth) {
        if (depth > MAX_DEPTH) {
            throw new IllegalArgumentException("表达式嵌套过深 (max=" + MAX_DEPTH + ")");
        }
        if (expr instanceof ExpressionSpec.FunctionCall fc) {
            return fc.func().getIotdbExpr(attrCode);
        } else if (expr instanceof ExpressionSpec.Constant c) {
            return String.valueOf(c.value());
        } else if (expr instanceof ExpressionSpec.BinaryOp bo) {
            return "(" + render(bo.left(), attrCode, depth + 1)
                    + " " + bo.op().getSymbol() + " "
                    + render(bo.right(), attrCode, depth + 1) + ")";
        }
        throw new IllegalArgumentException("未知 ExpressionSpec: " + expr.getClass());
    }

    /**
     * 生成别名(用于 SELECT AS 别名 + 返回 Map key)。
     *
     * <p>特殊映射: {@code LAST_VALUE - FIRST_VALUE} → {@code DELTA}。
     * 别名仅允许字母数字 + {@code _-/()},长度 ≤ 64。</p>
     *
     * @throws IllegalArgumentException 别名格式非法或超长
     */
    public String alias(ExpressionSpec expr) {
        String alias = doAlias(expr);
        if (alias.length() > MAX_ALIAS_LENGTH) {
            throw new IllegalArgumentException("别名过长 (max=" + MAX_ALIAS_LENGTH + "): " + alias);
        }
        if (!ALIAS_ALLOWED.matcher(alias).matches()) {
            throw new IllegalArgumentException("别名含非法字符: " + alias);
        }
        return alias;
    }

    private String doAlias(ExpressionSpec expr) {
        if (expr instanceof ExpressionSpec.FunctionCall fc) {
            return fc.func().name();
        } else if (expr instanceof ExpressionSpec.Constant c) {
            return String.valueOf(c.value());
        } else if (expr instanceof ExpressionSpec.BinaryOp bo) {
            String left = doAlias(bo.left());
            String right = doAlias(bo.right());
            // 特殊映射: LAST_VALUE - FIRST_VALUE → DELTA
            if (bo.op() == ExpressionSpec.BinaryOperator.SUB
                    && "LAST_VALUE".equals(left) && "FIRST_VALUE".equals(right)) {
                return "DELTA";
            }
            return "(" + left + bo.op().getSymbol() + right + ")";
        }
        throw new IllegalArgumentException("未知 ExpressionSpec: " + expr.getClass());
    }
}
```

- [ ] **Step 4: 跑测试,确认通过**

```bash
mvn -pl zwei-iot-timeseries -Dtest=ExpressionSpecRendererTest test
```

预期: **测试通过**(深度 6 和别名 6 层嵌套都会触发,后者别名长度会超 64)

- [ ] **Step 5: Commit**

```bash
cd D:/Code/Projects/geo_hazard_monitor
git add server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/domain/ExpressionSpecRenderer.java
git add server/zwei-iot-timeseries/src/test/java/com/zwei/iot/timeseries/domain/ExpressionSpecRendererTest.java
git commit -m "feat(timeseries): 新增 ExpressionSpecRenderer 渲染器 + 别名生成"
```

---

## Task 4: `TimeWindowSpec` record

**Files:**
- Create: `server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/domain/TimeWindowSpec.java`
- Create: `server/zwei-iot-timeseries/src/test/java/com/zwei/iot/timeseries/domain/TimeWindowSpecTest.java`

- [ ] **Step 1: 写失败测试 `TimeWindowSpecTest`**

```java
package com.zwei.iot.timeseries.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.zwei.iot.timeseries.domain.TimeWindowSpec.WindowGranularity.*;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TimeWindowSpec")
class TimeWindowSpecTest {

    @Test
    @DisplayName("RAW 粒度 GROUP BY 间隔为 null")
    void raw_noGroupBy() {
        TimeWindowSpec spec = new TimeWindowSpec(0L, 1000L, RAW);
        assertThat(spec.granularity().toGroupByInterval()).isNull();
    }

    @Test
    @DisplayName("HOUR 粒度 GROUP BY 间隔为 1h")
    void hour_groupBy() {
        assertThat(new TimeWindowSpec(0L, 1000L, HOUR).granularity().toGroupByInterval()).isEqualTo("1h");
    }

    @Test
    @DisplayName("DAY 粒度 GROUP BY 间隔为 1d")
    void day_groupBy() {
        assertThat(new TimeWindowSpec(0L, 1000L, DAY).granularity().toGroupByInterval()).isEqualTo("1d");
    }

    @Test
    @DisplayName("CUSTOM 粒度需要传入 customMillis")
    void custom_groupBy() {
        TimeWindowSpec spec = new TimeWindowSpec(0L, 1000L, CUSTOM);
        assertThat(spec.granularity().toGroupByInterval()).isEqualTo("?");
        assertThat(spec.granularity().toGroupByInterval(60000L)).isEqualTo("60000ms");
    }

    @Test
    @DisplayName("isAggregated — RAW 返回 false,其他返回 true")
    void isAggregated() {
        assertThat(RAW.isAggregated()).isFalse();
        assertThat(HOUR.isAggregated()).isTrue();
        assertThat(DAY.isAggregated()).isTrue();
        assertThat(CUSTOM.isAggregated()).isTrue();
    }

    @Test
    @DisplayName("TimeWindowSpec 持有 startTime / endTime / granularity")
    void record_holdsFields() {
        TimeWindowSpec spec = new TimeWindowSpec(100L, 200L, HOUR);
        assertThat(spec.startTime()).isEqualTo(100L);
        assertThat(spec.endTime()).isEqualTo(200L);
        assertThat(spec.granularity()).isEqualTo(HOUR);
    }
}
```

- [ ] **Step 2: 跑测试,确认失败**

```bash
cd D:/Code/Projects/geo_hazard_monitor/server
mvn -pl zwei-iot-timeseries -Dtest=TimeWindowSpecTest test
```

预期: **编译失败**

- [ ] **Step 3: 写最小实现 `TimeWindowSpec.java`**

```java
package com.zwei.iot.timeseries.domain;

/**
 * 时间窗口 + 聚合粒度参数。
 *
 * <p>粒度决定是否走 IoTDB GROUP BY 降采样。</p>
 *
 * @param startTime   开始时间(毫秒时间戳),可空
 * @param endTime     结束时间(毫秒时间戳),可空
 * @param granularity 粒度
 */
public record TimeWindowSpec(Long startTime, Long endTime, WindowGranularity granularity) {

    /**
     * 聚合粒度,决定 IoTDB GROUP BY 间隔。
     */
    public enum WindowGranularity {
        RAW(null),
        HOUR("1h"),
        DAY("1d"),
        CUSTOM("?");

        private final String defaultInterval;

        WindowGranularity(String defaultInterval) {
            this.defaultInterval = defaultInterval;
        }

        public String toGroupByInterval() {
            return defaultInterval;
        }

        public String toGroupByInterval(Long customMillis) {
            if (this != CUSTOM) {
                return defaultInterval;
            }
            if (customMillis == null || customMillis <= 0) {
                throw new IllegalArgumentException("CUSTOM 粒度必须传入正整数毫秒值");
            }
            return customMillis + "ms";
        }

        public boolean isAggregated() {
            return this != RAW;
        }
    }
}
```

- [ ] **Step 4: 跑测试,确认通过**

```bash
mvn -pl zwei-iot-timeseries -Dtest=TimeWindowSpecTest test
```

预期: **测试通过**

- [ ] **Step 5: Commit**

```bash
cd D:/Code/Projects/geo_hazard_monitor
git add server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/domain/TimeWindowSpec.java
git add server/zwei-iot-timeseries/src/test/java/com/zwei/iot/timeseries/domain/TimeWindowSpecTest.java
git commit -m "feat(timeseries): 新增 TimeWindowSpec 时间窗口规格"
```

---

## Task 5: 4 个 VO(AggregationResultVO / SensorAggregationVO / CompletenessReportVO / TrendReportVO)

**Files:**
- Create: `server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/domain/AggregationResultVO.java`
- Create: `server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/domain/SensorAggregationVO.java`
- Create: `server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/domain/CompletenessReportVO.java`
- Create: `server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/domain/TrendReportVO.java`
- Create: `server/zwei-iot-timeseries/src/test/java/com/zwei/iot/timeseries/domain/AggregationVOsTest.java`

- [ ] **Step 1: 写失败测试 `AggregationVOsTest`**

```java
package com.zwei.iot.timeseries.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Aggregation / Sensor / Completeness / Trend VOs")
class AggregationVOsTest {

    @Test
    @DisplayName("AggregationResultVO 持有指标与 metrics 字典")
    void aggregationResultVO() {
        AggregationResultVO vo = new AggregationResultVO(
                1L, "rain_01", "rainfall", "雨量", "mm",
                1700000000000L, Map.of("AVG", 12.5, "MAX", 30.0));
        assertThat(vo.deviceId()).isEqualTo(1L);
        assertThat(vo.sensorCode()).isEqualTo("rain_01");
        assertThat(vo.attrCode()).isEqualTo("rainfall");
        assertThat(vo.metrics()).containsEntry("AVG", 12.5).containsEntry("MAX", 30.0);
    }

    @Test
    @DisplayName("SensorAggregationVO 持有 sensor 元数据 + 结果列表")
    void sensorAggregationVO() {
        AggregationResultVO inner = new AggregationResultVO(
                1L, "rain_01", "rainfall", "雨量", "mm", 0L, Map.of());
        SensorAggregationVO vo = new SensorAggregationVO(1L, "rain_01", "雨量计", List.of(inner));
        assertThat(vo.results()).hasSize(1);
        assertThat(vo.sensorName()).isEqualTo("雨量计");
    }

    @Test
    @DisplayName("CompletenessReportVO 持有完整度统计")
    void completenessReportVO() {
        CompletenessReportVO vo = new CompletenessReportVO(
                1L, "rain_01", "rainfall", 100L, 80L, 0.8, 0.2, 1700000000000L);
        assertThat(vo.expectedPoints()).isEqualTo(100L);
        assertThat(vo.actualPoints()).isEqualTo(80L);
        assertThat(vo.completenessRate()).isEqualTo(0.8);
    }

    @Test
    @DisplayName("TrendReportVO 持有趋势/变化率")
    void trendReportVO() {
        TrendReportVO vo = new TrendReportVO(
                1L, "rain_01", "rainfall",
                1700000000000L, 1700003600000L,
                1.0e-7, 0.36, 8.64, 0.0, 1.0, "rising");
        assertThat(vo.slopePerMs()).isEqualTo(1.0e-7);
        assertThat(vo.ratePerHour()).isEqualTo(0.36);
        assertThat(vo.trendDirection()).isEqualTo("rising");
    }
}
```

- [ ] **Step 2: 跑测试,确认失败**

```bash
cd D:/Code/Projects/geo_hazard_monitor/server
mvn -pl zwei-iot-timeseries -Dtest=AggregationVOsTest test
```

预期: **编译失败** — 4 个 VO 不存在

- [ ] **Step 3: 写 `AggregationResultVO.java`**

```java
package com.zwei.iot.timeseries.domain;

import java.io.Serializable;
import java.util.Map;

/**
 * 单 (deviceId, sensorCode, attrCode) 维度的聚合结果。
 *
 * <p>时间戳 {@code time} 对应 GROUP BY 后的分组时间(RAW 时为 0)。
 * {@code metrics} 是表达式别名 → 数值的字典,如 {@code {"AVG": 12.5, "DELTA": 0.7}}。</p>
 */
public record AggregationResultVO(
        Long deviceId,
        String sensorCode,
        String attrCode,
        String attrName,
        String unit,
        long time,
        Map<String, Double> metrics
) implements Serializable {
    private static final long serialVersionUID = 1L;
}
```

- [ ] **Step 4: 写 `SensorAggregationVO.java`**

```java
package com.zwei.iot.timeseries.domain;

import java.io.Serializable;
import java.util.List;

/**
 * sensorCode 维度的批量聚合响应 — 包含该 sensor 下所有 attrCode 的聚合结果。
 */
public record SensorAggregationVO(
        Long deviceId,
        String sensorCode,
        String sensorName,
        List<AggregationResultVO> results
) implements Serializable {
    private static final long serialVersionUID = 1L;
}
```

- [ ] **Step 5: 写 `CompletenessReportVO.java`**

```java
package com.zwei.iot.timeseries.domain;

import java.io.Serializable;

/**
 * 完整度报告 — 在指定时间窗口内,传感器"应该上报 N 次,实际只上报 M 次"的统计。
 *
 * <p>{@code expectedPoints} = 时长 / 期望采样间隔
 * <br>{@code completenessRate} = actualPoints / expectedPoints(0-1)
 * <br>{@code missingRate} = 1 - completenessRate</p>
 */
public record CompletenessReportVO(
        Long deviceId,
        String sensorCode,
        String attrCode,
        long expectedPoints,
        long actualPoints,
        double completenessRate,
        double missingRate,
        Long lastReportAt
) implements Serializable {
    private static final long serialVersionUID = 1L;
}
```

- [ ] **Step 6: 写 `TrendReportVO.java`**

```java
package com.zwei.iot.timeseries.domain;

import java.io.Serializable;

/**
 * 趋势 / 变化率报告 — 端点斜率近似((LAST_VALUE - FIRST_VALUE) / 时长)。
 *
 * <p>注意:本类使用的是"端点斜率"近似,不是严格最小二乘回归。
 * 在数据单调、噪声小的场景下近似度高;噪声大时偏差较大。</p>
 *
 * @param slopePerMs   每毫秒的变化量(原始斜率)
 * @param ratePerHour  每小时的变化量(slopePerMs × 3,600,000)
 * @param ratePerDay   每天的变化量(slopePerMs × 86,400,000)
 * @param trendDirection "rising" / "falling" / "stable" / "unknown"
 */
public record TrendReportVO(
        Long deviceId,
        String sensorCode,
        String attrCode,
        long startTime,
        long endTime,
        Double slopePerMs,
        Double ratePerHour,
        Double ratePerDay,
        Double firstValue,
        Double lastValue,
        String trendDirection
) implements Serializable {
    private static final long serialVersionUID = 1L;
}
```

- [ ] **Step 7: 跑测试,确认通过**

```bash
mvn -pl zwei-iot-timeseries -Dtest=AggregationVOsTest test
```

预期: **测试通过**

- [ ] **Step 8: Commit**

```bash
cd D:/Code/Projects/geo_hazard_monitor
git add server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/domain/AggregationResultVO.java
git add server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/domain/SensorAggregationVO.java
git add server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/domain/CompletenessReportVO.java
git add server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/domain/TrendReportVO.java
git add server/zwei-iot-timeseries/src/test/java/com/zwei/iot/timeseries/domain/AggregationVOsTest.java
git commit -m "feat(timeseries): 新增 4 个 VO(Aggregation/Sensor/Completeness/Trend)"
```

---

## Task 6: `IotdbTimeSeriesService.queryLatestBySensor`

**Files:**
- Modify: `server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/service/IotdbTimeSeriesService.java:1-480`
- Create: `server/zwei-iot-timeseries/src/test/java/com/zwei/iot/timeseries/service/IotdbTimeSeriesServiceQueryTest.java`

- [ ] **Step 1: 写失败测试**

```java
package com.zwei.iot.timeseries.service;

import com.zwei.iot.timeseries.config.IotdbProperties;
import com.zwei.iot.timeseries.domain.IotdbQueryRow;
import com.zwei.iot.timeseries.support.IotdbPathResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("IotdbTimeSeriesService 增强查询方法")
class IotdbTimeSeriesServiceQueryTest {

    @Mock private IotdbJdbcClient jdbcClient;
    @Mock private Connection connection;
    @Mock private PreparedStatement statement;
    @Mock private ResultSet resultSet;

    private IotdbTimeSeriesService service;
    private IotdbPathResolver pathResolver;

    @BeforeEach
    void setUp() throws Exception {
        IotdbProperties props = new IotdbProperties();
        props.setDatabase("root.zwei");
        pathResolver = new IotdbPathResolver(props);
        service = new IotdbTimeSeriesService(jdbcClient, props, pathResolver);

        // 默认 stub:让 Connection 链可用
        when(jdbcClient.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery(anyString())).thenReturn(resultSet);
    }

    @Test
    @DisplayName("queryLatestBySensor — 传单 attrCode 时等价于 queryLatest")
    void queryLatestBySensor_singleAttr_returnsRows() throws Exception {
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getLong("Time")).thenReturn(1700000000000L);
        when(resultSet.getObject("root.zwei.d1.srain_01.rainfall")).thenReturn(12.5);
        when(resultSet.getObject("root.zwei.d1.srain_01.quality")).thenReturn(0);

        List<IotdbQueryRow> rows = service.queryLatestBySensor(1L, "rain_01", List.of("rainfall"));

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).value()).isEqualTo(12.5);
        verify(statement).executeQuery(contains("ORDER BY TIME DESC LIMIT 1"));
    }

    @Test
    @DisplayName("queryLatestBySensor — 多 attrCode 时多次查询并合并")
    void queryLatestBySensor_multiAttr_merges() throws Exception {
        when(resultSet.next()).thenReturn(true, false, true, false);
        when(resultSet.getLong("Time")).thenReturn(1700000000000L, 1700000001000L);
        when(resultSet.getObject("root.zwei.d1.srain_01.rainfall")).thenReturn(12.5);
        when(resultSet.getObject("root.zwei.d1.srain_01.battery")).thenReturn(85.0);
        when(resultSet.getObject(contains(".quality"))).thenReturn(0);

        List<IotdbQueryRow> rows = service.queryLatestBySensor(1L, "rain_01", List.of("rainfall", "battery"));

        assertThat(rows).hasSize(2);
        assertThat(rows).extracting(IotdbQueryRow::value).containsExactlyInAnyOrder(12.5, 85.0);
    }

    @Test
    @DisplayName("queryLatestBySensor — 空 attr 列表返回空集合")
    void queryLatestBySensor_emptyAttrs_returnsEmpty() {
        List<IotdbQueryRow> rows = service.queryLatestBySensor(1L, "rain_01", List.of());
        assertThat(rows).isEmpty();
        verify(statement, never()).executeQuery(anyString());
    }
}
```

- [ ] **Step 2: 跑测试,确认失败**

```bash
cd D:/Code/Projects/geo_hazard_monitor/server
mvn -pl zwei-iot-timeseries -Dtest=IotdbTimeSeriesServiceQueryTest#queryLatestBySensor_singleAttr_returnsRows+queryLatestBySensor_multiAttr_merges+queryLatestBySensor_emptyAttrs_returnsEmpty test
```

预期: **编译失败** — 方法不存在

- [ ] **Step 3: 实现 `queryLatestBySensor`**

在 `IotdbTimeSeriesService.java` 中新增:

```java
    /**
     * 批量查询某传感器下多个 attrCode 的最新值。
     *
     * @param deviceId   设备ID
     * @param sensorCode 传感器编号
     * @param attrCodes  指标编码列表
     * @return 每个 attrCode 的最新值行(单测点)
     */
    public List<IotdbQueryRow> queryLatestBySensor(Long deviceId, String sensorCode, List<String> attrCodes) {
        if (attrCodes == null || attrCodes.isEmpty()) {
            return List.of();
        }
        List<IotdbQueryRow> rows = new ArrayList<>();
        for (String attrCode : attrCodes) {
            IotdbQueryRow row = queryLatest(deviceId, sensorCode, attrCode);
            if (row != null && row.value() != null) {
                rows.add(row);
            }
        }
        return rows;
    }
```

放在 `queryRangePaged` 之后,`countRange` 之前。

- [ ] **Step 4: 跑测试,确认通过**

```bash
mvn -pl zwei-iot-timeseries -Dtest=IotdbTimeSeriesServiceQueryTest test
```

预期: **测试通过**

- [ ] **Step 5: Commit**

```bash
cd D:/Code/Projects/geo_hazard_monitor
git add server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/service/IotdbTimeSeriesService.java
git add server/zwei-iot-timeseries/src/test/java/com/zwei/iot/timeseries/service/IotdbTimeSeriesServiceQueryTest.java
git commit -m "feat(timeseries): IotdbTimeSeriesService 新增 queryLatestBySensor"
```

---

## Task 7: `IotdbTimeSeriesService.queryRangeBySensor` (含数值范围 WHERE)

**Files:**
- Modify: `server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/service/IotdbTimeSeriesService.java`

- [ ] **Step 1: 添加失败测试到 `IotdbTimeSeriesServiceQueryTest.java`**

```java
    @Test
    @DisplayName("queryRangeBySensor — WHERE 数值范围 minValue 拼接正确")
    void queryRangeBySensor_valueRangeFilter() throws Exception {
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getLong("Time")).thenReturn(1700000000000L);
        when(resultSet.getObject("root.zwei.d1.srain_01.rainfall")).thenReturn(15.0);
        when(resultSet.getObject("root.zwei.d1.srain_01.quality")).thenReturn(0);

        service.queryRangeBySensor(
                1L, "rain_01", List.of("rainfall"),
                1700000000000L, 1800000000000L, 10.0, 50.0, 100, 0);

        verify(statement).executeQuery(argThat((String sql) ->
                sql.contains("rainfall >= 10.0") && sql.contains("rainfall <= 50.0")
                        && sql.contains("time >= 1700000000000") && sql.contains("time < 1800000000000")
        ));
    }

    @Test
    @DisplayName("queryRangeBySensor — 数值范围为空时 WHERE 不含数值条件")
    void queryRangeBySensor_noValueRange() throws Exception {
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getLong("Time")).thenReturn(1700000000000L);
        when(resultSet.getObject("root.zwei.d1.srain_01.rainfall")).thenReturn(15.0);
        when(resultSet.getObject("root.zwei.d1.srain_01.quality")).thenReturn(0);

        service.queryRangeBySensor(
                1L, "rain_01", List.of("rainfall"),
                1700000000000L, 1800000000000L, null, null, 100, 0);

        verify(statement).executeQuery(argThat((String sql) ->
                !sql.contains("rainfall >=") && !sql.contains("rainfall <=")
        ));
    }
```

- [ ] **Step 2: 跑测试,确认失败**

```bash
mvn -pl zwei-iot-timeseries -Dtest=IotdbTimeSeriesServiceQueryTest test
```

预期: **编译失败** — 方法不存在

- [ ] **Step 3: 实现 `queryRangeBySensor`**

```java
    /**
     * 查询某传感器下多个 attrCode 的区间数据(支持数值范围 WHERE)。
     *
     * @param deviceId   设备ID
     * @param sensorCode 传感器编号
     * @param attrCodes  指标编码列表
     * @param startTime  开始时间(毫秒),可空
     * @param endTime    结束时间(毫秒),可空
     * @param minValue   数值下限(可空,WHERE {@code attrCode >= minValue})
     * @param maxValue   数值上限(可空,WHERE {@code attrCode <= maxValue})
     * @param limit      返回条数上限
     * @param offset     偏移量
     * @return {@code Map<attrCode, List<IotdbQueryRow>>}
     */
    public Map<String, List<IotdbQueryRow>> queryRangeBySensor(
            Long deviceId, String sensorCode, List<String> attrCodes,
            Long startTime, Long endTime,
            Double minValue, Double maxValue,
            int limit, int offset) {
        if (attrCodes == null || attrCodes.isEmpty()) {
            return Map.of();
        }
        Map<String, List<IotdbQueryRow>> result = new LinkedHashMap<>();
        for (String attrCode : attrCodes) {
            result.put(attrCode, queryRangeWithValueFilter(
                    deviceId, sensorCode, attrCode,
                    startTime, endTime, minValue, maxValue, limit, offset));
        }
        return result;
    }

    private List<IotdbQueryRow> queryRangeWithValueFilter(
            Long deviceId, String sensorCode, String attrCode,
            Long startTime, Long endTime,
            Double minValue, Double maxValue,
            int limit, int offset) {
        ensureMeasurement(attrCode, deviceId, sensorCode, "DOUBLE", "GORILLA");
        ensureMeasurement("quality", deviceId, sensorCode, "INT32", "RLE");
        StringBuilder sql = new StringBuilder("SELECT ")
                .append(attrCode).append(", quality FROM ")
                .append(pathResolver.buildSensorPath(deviceId, sensorCode));
        List<String> where = new ArrayList<>();
        if (startTime != null) where.add("time >= " + startTime);
        if (endTime != null)   where.add("time < " + endTime);
        if (minValue != null)  where.add(attrCode + " >= " + minValue);
        if (maxValue != null)  where.add(attrCode + " <= " + maxValue);
        if (!where.isEmpty())  sql.append(" WHERE ").append(String.join(" AND ", where));
        sql.append(" ORDER BY TIME DESC LIMIT ").append(limit).append(" OFFSET ").append(offset);

        String attrCol = pathResolver.buildMeasurementPath(deviceId, sensorCode, attrCode);
        String qualityCol = pathResolver.buildMeasurementPath(deviceId, sensorCode, "quality");
        List<IotdbQueryRow> rows = new ArrayList<>();
        try (Connection connection = jdbcClient.getConnection();
             Statement statement = connection.createStatement()) {
            ResultSet rs = statement.executeQuery(sql.toString());
            while (rs.next()) {
                Double v = safeGetDouble(rs, attrCol);
                if (v == null) continue;
                rows.add(IotdbQueryRow.builder()
                        .time(rs.getLong("Time"))
                        .value(v)
                        .quality(safeGetInteger(rs, qualityCol))
                        .build());
            }
            return rows;
        } catch (SQLException e) {
            throw new ServiceException("查询 IoTDB 区间数据失败")
                    .setDetailMessage(e.getMessage());
        }
    }
```

并在类顶部加 `import java.util.LinkedHashMap; import java.util.Map;`

- [ ] **Step 4: 跑测试,确认通过**

```bash
mvn -pl zwei-iot-timeseries -Dtest=IotdbTimeSeriesServiceQueryTest test
```

预期: **测试通过**

- [ ] **Step 5: Commit**

```bash
cd D:/Code/Projects/geo_hazard_monitor
git add server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/service/IotdbTimeSeriesService.java
git commit -m "feat(timeseries): IotdbTimeSeriesService 新增 queryRangeBySensor + 数值范围 WHERE"
```

---

## Task 8: `IotdbTimeSeriesService.queryAggregate` (核心 — 表达式驱动)

**Files:**
- Modify: `server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/service/IotdbTimeSeriesService.java`

- [ ] **Step 1: 添加失败测试到 `IotdbTimeSeriesServiceQueryTest.java`**

```java
import com.zwei.iot.timeseries.domain.AggregationFunction;
import com.zwei.iot.timeseries.domain.AggregationResultVO;
import com.zwei.iot.timeseries.domain.ExpressionSpec;
import com.zwei.iot.timeseries.domain.ExpressionSpecRenderer;
import com.zwei.iot.timeseries.domain.TimeWindowSpec;
import static com.zwei.iot.timeseries.domain.TimeWindowSpec.WindowGranularity.HOUR;
import static com.zwei.iot.timeseries.domain.ExpressionSpec.BinaryOperator.SUB;

    @Test
    @DisplayName("queryAggregate — 单表达式 AVG 拼 SELECT/GROUP BY 正确")
    void queryAggregate_singleAvg() throws Exception {
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getLong("Time")).thenReturn(1700000000000L);
        when(resultSet.getObject("`AVG`")).thenReturn(12.5);

        List<AggregationResultVO> results = service.queryAggregate(
                1L, "rain_01", "rainfall",
                new TimeWindowSpec(1700000000000L, 1800000000000L, HOUR),
                List.of(new ExpressionSpec.FunctionCall(AggregationFunction.AVG)),
                null, null);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).metrics()).containsEntry("AVG", 12.5);
        verify(statement).executeQuery(argThat((String sql) ->
                sql.contains("AVG(root.zwei.d1.srain_01.rainfall)")
                        && sql.contains("GROUP BY ([1700000000000, 1800000000000), 1h)")
        ));
    }

    @Test
    @DisplayName("queryAggregate — 二元表达式 LAST-FIRST 渲染为别名 DELTA")
    void queryAggregate_deltaExpr() throws Exception {
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getLong("Time")).thenReturn(1700000000000L);
        when(resultSet.getObject("`DELTA`")).thenReturn(0.7);

        ExpressionSpec delta = new ExpressionSpec.BinaryOp(
                new ExpressionSpec.FunctionCall(AggregationFunction.LAST_VALUE), SUB,
                new ExpressionSpec.FunctionCall(AggregationFunction.FIRST_VALUE));

        List<AggregationResultVO> results = service.queryAggregate(
                1L, "rain_01", "rainfall",
                new TimeWindowSpec(1700000000000L, 1800000000000L, HOUR),
                List.of(delta),
                null, null);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).metrics()).containsEntry("DELTA", 0.7);
        verify(statement).executeQuery(argThat((String sql) ->
                sql.contains("LAST_VALUE(root.zwei.d1.srain_01.rainfall) - FIRST_VALUE(root.zwei.d1.srain_01.rainfall)")
                        && sql.contains("AS `DELTA`")
        ));
    }

    @Test
    @DisplayName("queryAggregate — 数值范围 minValue/maxValue 拼 WHERE")
    void queryAggregate_valueRange() throws Exception {
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getLong("Time")).thenReturn(1700000000000L);
        when(resultSet.getObject("`AVG`")).thenReturn(12.5);

        service.queryAggregate(
                1L, "rain_01", "rainfall",
                new TimeWindowSpec(1700000000000L, 1800000000000L, HOUR),
                List.of(new ExpressionSpec.FunctionCall(AggregationFunction.AVG)),
                10.0, 50.0);

        verify(statement).executeQuery(argThat((String sql) ->
                sql.contains("rainfall >= 10.0") && sql.contains("rainfall <= 50.0")
        ));
    }

    @Test
    @DisplayName("queryAggregate — 表达式列表为空抛 IllegalArgumentException")
    void queryAggregate_empty_throws() {
        assertThatThrownBy(() -> service.queryAggregate(
                1L, "rain_01", "rainfall",
                new TimeWindowSpec(1700000000000L, 1800000000000L, HOUR),
                List.of(), null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("表达式");
    }
```

同时给 `@BeforeEach` 加上:

```java
        // 注入 renderer(需要修改 service 构造器,见 Step 3)
```

并在测试类顶部的 import 区域添加:

```java
import java.util.List;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
```

- [ ] **Step 2: 跑测试,确认失败**

```bash
mvn -pl zwei-iot-timeseries -Dtest=IotdbTimeSeriesServiceQueryTest test
```

预期: **编译失败** — `queryAggregate` 不存在

- [ ] **Step 3: 改造 `IotdbTimeSeriesService` 构造器 + 实现 `queryAggregate`**

**修改构造器(在第 65-71 行附近):**

```java
    @Autowired
    public IotdbTimeSeriesService(IotdbJdbcClient jdbcClient,
                                  IotdbProperties properties,
                                  IotdbPathResolver pathResolver,
                                  ExpressionSpecRenderer renderer) {
        this.jdbcClient = jdbcClient;
        this.properties = properties;
        this.pathResolver = pathResolver;
        this.renderer = renderer;
    }

    private final ExpressionSpecRenderer renderer;
```

**新增 `queryAggregate` 方法:**

```java
    /**
     * 多表达式聚合查询(支持白名单函数 + 表达式组合 + 数值范围 WHERE + 时间窗口 GROUP BY)。
     *
     * <p>对应 IoTDB SQL 模板:</p>
     * <pre>
     * SELECT {expr1} AS `alias1`, {expr2} AS `alias2`, ... FROM {sensorPath}
     * [WHERE time >= start AND time < end AND attr >= min AND attr <= max]
     * [GROUP BY ([start, end), interval)]
     * </pre>
     *
     * @return 按时间分组的结果列表,每行 = {@code AggregationResultVO}
     */
    public List<AggregationResultVO> queryAggregate(
            Long deviceId, String sensorCode, String attrCode,
            TimeWindowSpec window,
            List<ExpressionSpec> expressions,
            Double minValue, Double maxValue) {
        if (expressions == null || expressions.isEmpty()) {
            throw new IllegalArgumentException("表达式列表不能为空");
        }
        ensureMeasurement(attrCode, deviceId, sensorCode, "DOUBLE", "GORILLA");
        ensureMeasurement("quality", deviceId, sensorCode, "INT32", "RLE");

        String sensorPath = pathResolver.buildSensorPath(deviceId, sensorCode);
        StringBuilder sql = new StringBuilder("SELECT ");
        List<String> aliases = new ArrayList<>();
        for (int i = 0; i < expressions.size(); i++) {
            ExpressionSpec expr = expressions.get(i);
            String exprSql = renderer.render(expr, attrCode);
            String alias = renderer.alias(expr);
            sql.append(exprSql).append(" AS `").append(alias).append("`");
            aliases.add(alias);
            if (i < expressions.size() - 1) sql.append(", ");
        }
        sql.append(" FROM ").append(sensorPath);

        // WHERE
        List<String> where = new ArrayList<>();
        if (window.startTime() != null) where.add("time >= " + window.startTime());
        if (window.endTime() != null)   where.add("time < " + window.endTime());
        if (minValue != null)           where.add(attrCode + " >= " + minValue);
        if (maxValue != null)           where.add(attrCode + " <= " + maxValue);
        if (!where.isEmpty())           sql.append(" WHERE ").append(String.join(" AND ", where));

        // GROUP BY
        if (window.granularity() != TimeWindowSpec.WindowGranularity.RAW) {
            long start = window.startTime() != null ? window.startTime() : 0L;
            long end   = window.endTime()   != null ? window.endTime()   : System.currentTimeMillis();
            String interval = window.granularity().toGroupByInterval();
            sql.append(" GROUP BY ([").append(start).append(", ").append(end)
                    .append("), ").append(interval).append(")");
        }

        List<AggregationResultVO> results = new ArrayList<>();
        try (Connection connection = jdbcClient.getConnection();
             Statement statement = connection.createStatement()) {
            ResultSet rs = statement.executeQuery(sql.toString());
            while (rs.next()) {
                Map<String, Double> metrics = new LinkedHashMap<>();
                for (String alias : aliases) {
                    Double v = safeGetDouble(rs, "`" + alias + "`");
                    if (v != null) {
                        metrics.put(alias, v);
                    }
                }
                results.add(new AggregationResultVO(
                        deviceId, sensorCode, attrCode, null, null,
                        rs.getLong("Time"),
                        metrics
                ));
            }
            return results;
        } catch (SQLException e) {
            throw new ServiceException("查询 IoTDB 多表达式聚合失败")
                    .setDetailMessage(e.getMessage());
        }
    }
```

- [ ] **Step 4: 同步更新 `IotdbTimeSeriesServiceQueryTest` 的 `setUp`**

由于构造器现在多了一个 `ExpressionSpecRenderer` 参数,需要更新 `setUp`:

```java
    @BeforeEach
    void setUp() throws Exception {
        IotdbProperties props = new IotdbProperties();
        props.setDatabase("root.zwei");
        pathResolver = new IotdbPathResolver(props);
        ExpressionSpecRenderer renderer = new ExpressionSpecRenderer();
        service = new IotdbTimeSeriesService(jdbcClient, props, pathResolver, renderer);

        when(jdbcClient.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery(anyString())).thenReturn(resultSet);
    }
```

- [ ] **Step 5: 跑测试,确认通过**

```bash
mvn -pl zwei-iot-timeseries -Dtest=IotdbTimeSeriesServiceQueryTest test
```

预期: **所有 9 个测试通过**

- [ ] **Step 6: Commit**

```bash
cd D:/Code/Projects/geo_hazard_monitor
git add server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/service/IotdbTimeSeriesService.java
git add server/zwei-iot-timeseries/src/test/java/com/zwei/iot/timeseries/service/IotdbTimeSeriesServiceQueryTest.java
git commit -m "feat(timeseries): IotdbTimeSeriesService 新增 queryAggregate(多表达式 + 数值范围 + GROUP BY)"
```

---

## Task 9: `IotdbTimeSeriesService.queryDelta` (便捷方法)

**Files:**
- Modify: `server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/service/IotdbTimeSeriesService.java`

- [ ] **Step 1: 添加失败测试**

```java
    @Test
    @DisplayName("queryDelta — 内部调 queryAggregate 传 LAST_VALUE - FIRST_VALUE")
    void queryDelta_invokesAggregate() throws Exception {
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getLong("Time")).thenReturn(1700000000000L);
        when(resultSet.getObject("`DELTA`")).thenReturn(0.7);

        AggregationResultVO result = service.queryDelta(
                1L, "rain_01", "rainfall",
                new TimeWindowSpec(1700000000000L, 1800000000000L, HOUR));

        assertThat(result.metrics()).containsEntry("DELTA", 0.7);
        verify(statement).executeQuery(argThat((String sql) ->
                sql.contains("LAST_VALUE(") && sql.contains("FIRST_VALUE(") && sql.contains("AS `DELTA`")
        ));
    }
```

加上 import:

```java
import com.zwei.iot.timeseries.domain.AggregationResultVO;
```

- [ ] **Step 2: 跑测试,确认失败**

```bash
mvn -pl zwei-iot-timeseries -Dtest=IotdbTimeSeriesServiceQueryTest#queryDelta_invokesAggregate test
```

预期: **编译失败**

- [ ] **Step 3: 实现 `queryDelta`**

```java
    /**
     * 计算时间窗口内某指标的首末差值 (LAST_VALUE - FIRST_VALUE)。
     *
     * <p>等价于 {@code queryAggregate} 传 {@code BinaryOp(LAST_VALUE, SUB, FIRST_VALUE)}。</p>
     *
     * @return 单个 {@link AggregationResultVO},{@code metrics} 含 {@code DELTA} 键
     */
    public AggregationResultVO queryDelta(
            Long deviceId, String sensorCode, String attrCode, TimeWindowSpec window) {
        ExpressionSpec delta = new ExpressionSpec.BinaryOp(
                new ExpressionSpec.FunctionCall(AggregationFunction.LAST_VALUE),
                ExpressionSpec.BinaryOperator.SUB,
                new ExpressionSpec.FunctionCall(AggregationFunction.FIRST_VALUE));
        List<AggregationResultVO> results = queryAggregate(
                deviceId, sensorCode, attrCode, window, List.of(delta), null, null);
        if (results.isEmpty()) {
            return null;
        }
        AggregationResultVO first = results.get(0);
        return new AggregationResultVO(
                first.deviceId(), first.sensorCode(), first.attrCode(),
                first.attrName(), first.unit(),
                first.time(), first.metrics());
    }
```

- [ ] **Step 4: 跑测试,确认通过**

```bash
mvn -pl zwei-iot-timeseries -Dtest=IotdbTimeSeriesServiceQueryTest test
```

预期: **所有测试通过**

- [ ] **Step 5: Commit**

```bash
cd D:/Code/Projects/geo_hazard_monitor
git add server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/service/IotdbTimeSeriesService.java
git commit -m "feat(timeseries): IotdbTimeSeriesService 新增 queryDelta 便捷方法"
```

---

## Task 10: `IotdbTimeSeriesService.queryCompleteness`

**Files:**
- Modify: `server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/service/IotdbTimeSeriesService.java`

- [ ] **Step 1: 添加失败测试**

```java
import com.zwei.iot.timeseries.domain.CompletenessReportVO;

    @Test
    @DisplayName("queryCompleteness — 期望点 = 时长 / expectedIntervalMs")
    void queryCompleteness_calculatesExpectedPoints() throws Exception {
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getLong("COUNT(root.zwei.d1.srain_01.rainfall)")).thenReturn(80L);

        CompletenessReportVO vo = service.queryCompleteness(
                1L, "rain_01", "rainfall",
                new TimeWindowSpec(0L, 3_600_000L, TimeWindowSpec.WindowGranularity.RAW),
                60_000L);

        assertThat(vo.expectedPoints()).isEqualTo(60L);   // 3,600,000 / 60,000 = 60
        assertThat(vo.actualPoints()).isEqualTo(80L);
        assertThat(vo.completenessRate()).isEqualTo(80.0 / 60.0);
    }

    @Test
    @DisplayName("queryCompleteness — expectedIntervalMs 为空用 60s 兜底")
    void queryCompleteness_fallbackInterval() throws Exception {
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getLong(contains("COUNT("))).thenReturn(0L);

        CompletenessReportVO vo = service.queryCompleteness(
                1L, "rain_01", "rainfall",
                new TimeWindowSpec(0L, 3_600_000L, TimeWindowSpec.WindowGranularity.RAW),
                null);

        assertThat(vo.expectedPoints()).isEqualTo(60L);   // 3,600,000 / 60,000(兜底)
        assertThat(vo.actualPoints()).isEqualTo(0L);
        assertThat(vo.completenessRate()).isEqualTo(0.0);
    }
```

- [ ] **Step 2: 跑测试,确认失败**

```bash
mvn -pl zwei-iot-timeseries -Dtest=IotdbTimeSeriesServiceQueryTest#queryCompleteness_calculatesExpectedPoints+queryCompleteness_fallbackInterval test
```

预期: **编译失败**

- [ ] **Step 3: 实现 `queryCompleteness`**

```java
    /**
     * 计算时间窗口内的数据完整度。
     *
     * <p>期望点 = (endTime - startTime) / expectedIntervalMs(若为空则用 60s 兜底)。
     * 实际点 = IoTDB COUNT 查询结果。</p>
     */
    public CompletenessReportVO queryCompleteness(
            Long deviceId, String sensorCode, String attrCode,
            TimeWindowSpec window, Long expectedIntervalMs) {
        long start = window.startTime() != null ? window.startTime() : 0L;
        long end = window.endTime() != null ? window.endTime() : System.currentTimeMillis();
        long interval = expectedIntervalMs != null && expectedIntervalMs > 0 ? expectedIntervalMs : 60_000L;
        long expectedPoints = (end - start) / interval;
        if (expectedPoints <= 0) {
            expectedPoints = 1;
        }

        ensureMeasurement(attrCode, deviceId, sensorCode, "DOUBLE", "GORILLA");
        StringBuilder sql = new StringBuilder("SELECT COUNT(")
                .append(attrCode).append(") FROM ")
                .append(pathResolver.buildSensorPath(deviceId, sensorCode));
        if (window.startTime() != null || window.endTime() != null) {
            sql.append(" WHERE ");
            if (window.startTime() != null) sql.append("time >= ").append(window.startTime());
            if (window.startTime() != null && window.endTime() != null) sql.append(" AND ");
            if (window.endTime() != null) sql.append("time < ").append(window.endTime());
        }
        String countCol = "COUNT(" + pathResolver.buildMeasurementPath(deviceId, sensorCode, attrCode) + ")";
        long actualPoints = 0;
        try (Connection connection = jdbcClient.getConnection();
             Statement statement = connection.createStatement()) {
            ResultSet rs = statement.executeQuery(sql.toString());
            if (rs.next()) {
                actualPoints = rs.getLong(countCol);
            }
        } catch (SQLException e) {
            log.warn("查询 IoTDB 完整度失败: deviceId={}, sensorCode={}, attrCode={}", deviceId, sensorCode, attrCode, e);
        }
        double rate = expectedPoints > 0 ? (double) actualPoints / expectedPoints : 0D;
        rate = Math.min(rate, 1.0);
        return new CompletenessReportVO(
                deviceId, sensorCode, attrCode,
                expectedPoints, actualPoints, rate, 1.0 - rate,
                actualPoints > 0 ? end : null);
    }
```

- [ ] **Step 4: 跑测试,确认通过**

```bash
mvn -pl zwei-iot-timeseries -Dtest=IotdbTimeSeriesServiceQueryTest test
```

预期: **所有测试通过**

- [ ] **Step 5: Commit**

```bash
cd D:/Code/Projects/geo_hazard_monitor
git add server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/service/IotdbTimeSeriesService.java
git commit -m "feat(timeseries): IotdbTimeSeriesService 新增 queryCompleteness"
```

---

## Task 11: `IotdbTimeSeriesService.queryTrend`

**Files:**
- Modify: `server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/service/IotdbTimeSeriesService.java`

- [ ] **Step 1: 添加失败测试**

```java
import com.zwei.iot.timeseries.domain.TrendReportVO;

    @Test
    @DisplayName("queryTrend — 上升趋势:LAST > FIRST")
    void queryTrend_rising() throws Exception {
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getObject("`DELTA`")).thenReturn(7.2);

        TrendReportVO vo = service.queryTrend(
                1L, "rain_01", "rainfall",
                new TimeWindowSpec(0L, 3_600_000L, TimeWindowSpec.WindowGranularity.RAW));

        assertThat(vo.slopePerMs()).isEqualTo(7.2 / 3_600_000);
        assertThat(vo.ratePerHour()).isEqualTo(7.2);
        assertThat(vo.trendDirection()).isEqualTo("rising");
    }

    @Test
    @DisplayName("queryTrend — 下降趋势:LAST < FIRST")
    void queryTrend_falling() throws Exception {
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getObject("`DELTA`")).thenReturn(-3.0);

        TrendReportVO vo = service.queryTrend(
                1L, "rain_01", "rainfall",
                new TimeWindowSpec(0L, 1_800_000L, TimeWindowSpec.WindowGranularity.RAW));

        assertThat(vo.slopePerMs()).isEqualTo(-3.0 / 1_800_000);
        assertThat(vo.ratePerHour()).isEqualTo(-3.0 * 2);   // -3 / 1,800,000 * 3,600,000 = -6
        assertThat(vo.trendDirection()).isEqualTo("falling");
    }

    @Test
    @DisplayName("queryTrend — 稳定:DELTA 接近 0")
    void queryTrend_stable() throws Exception {
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getObject("`DELTA`")).thenReturn(1e-15);

        TrendReportVO vo = service.queryTrend(
                1L, "rain_01", "rainfall",
                new TimeWindowSpec(0L, 3_600_000L, TimeWindowSpec.WindowGranularity.RAW));

        assertThat(vo.trendDirection()).isEqualTo("stable");
    }

    @Test
    @DisplayName("queryTrend — 无数据时 direction=unknown,数值为 null")
    void queryTrend_noData() throws Exception {
        when(resultSet.next()).thenReturn(false);

        TrendReportVO vo = service.queryTrend(
                1L, "rain_01", "rainfall",
                new TimeWindowSpec(0L, 3_600_000L, TimeWindowSpec.WindowGranularity.RAW));

        assertThat(vo.trendDirection()).isEqualTo("unknown");
        assertThat(vo.slopePerMs()).isNull();
    }
```

- [ ] **Step 2: 跑测试,确认失败**

```bash
mvn -pl zwei-iot-timeseries -Dtest=IotdbTimeSeriesServiceQueryTest#queryTrend_rising+queryTrend_falling+queryTrend_stable+queryTrend_noData test
```

预期: **编译失败**

- [ ] **Step 3: 实现 `queryTrend`**

```java
    /**
     * 计算时间窗口内某指标的端点斜率(变化率近似)。
     *
     * <p>采用端点斜率近似:(LAST_VALUE - FIRST_VALUE) / 时长,
     * 不是严格最小二乘回归。噪声大的数据偏差较大。</p>
     *
     * @return 趋势报告,无数据时 direction="unknown"
     */
    public TrendReportVO queryTrend(
            Long deviceId, String sensorCode, String attrCode, TimeWindowSpec window) {
        long start = window.startTime() != null ? window.startTime() : 0L;
        long end = window.endTime() != null ? window.endTime() : System.currentTimeMillis();
        long duration = end - start;
        if (duration <= 0) {
            return new TrendReportVO(deviceId, sensorCode, attrCode, start, end,
                    null, null, null, null, null, "unknown");
        }

        // 复用 queryAggregate: 算 LAST - FIRST
        AggregationResultVO delta = queryDelta(deviceId, sensorCode, attrCode, window);
        if (delta == null || delta.metrics() == null || !delta.metrics().containsKey("DELTA")) {
            return new TrendReportVO(deviceId, sensorCode, attrCode, start, end,
                    null, null, null, null, null, "unknown");
        }
        double deltaValue = delta.metrics().get("DELTA");
        double slopePerMs = deltaValue / duration;
        double ratePerHour = slopePerMs * 3_600_000D;
        double ratePerDay = slopePerMs * 86_400_000D;

        String direction;
        if (Math.abs(slopePerMs) < 1.0e-9) {
            direction = "stable";
        } else if (slopePerMs > 0) {
            direction = "rising";
        } else {
            direction = "falling";
        }

        return new TrendReportVO(deviceId, sensorCode, attrCode, start, end,
                slopePerMs, ratePerHour, ratePerDay, null, null, direction);
    }
```

- [ ] **Step 4: 跑测试,确认通过**

```bash
mvn -pl zwei-iot-timeseries -Dtest=IotdbTimeSeriesServiceQueryTest test
```

预期: **所有测试通过**

- [ ] **Step 5: Commit**

```bash
cd D:/Code/Projects/geo_hazard_monitor
git add server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/service/IotdbTimeSeriesService.java
git commit -m "feat(timeseries): IotdbTimeSeriesService 新增 queryTrend 端点斜率近似"
```

**阶段 1 完成标记** ✅ — 7 个新 domain + 6 个 IotdbTimeSeriesService 新方法,共 11 个 commit

---

# 阶段 2:API 层(任务 12-16)

## Task 12: `MonitorDataAggregationService`(聚合 + delta)

**Files:**
- Create: `server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/service/MonitorDataAggregationService.java`
- Create: `server/zwei-iot-timeseries/src/test/java/com/zwei/iot/timeseries/service/MonitorDataAggregationServiceTest.java`

- [ ] **Step 1: 写失败测试 `MonitorDataAggregationServiceTest`**

```java
package com.zwei.iot.timeseries.service;

import com.zwei.common.exception.ServiceException;
import com.zwei.iot.device.domain.DeviceSensor;
import com.zwei.iot.device.domain.SensorAttribute;
import com.zwei.iot.device.service.IDeviceSensorService;
import com.zwei.iot.timeseries.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static com.zwei.iot.timeseries.domain.TimeWindowSpec.WindowGranularity.HOUR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MonitorDataAggregationService")
class MonitorDataAggregationServiceTest {

    @Mock private IotdbTimeSeriesService iotdbService;
    @Mock private IDeviceSensorService deviceSensorService;

    private MonitorDataAggregationService service;

    @BeforeEach
    void setUp() {
        service = new MonitorDataAggregationService(iotdbService, deviceSensorService);
    }

    private DeviceSensor fakeSensor(List<SensorAttribute> attrs) {
        DeviceSensor sensor = new DeviceSensor();
        sensor.setId(1L);
        sensor.setDeviceId(1L);
        sensor.setSensorCode("rain_01");
        sensor.setSensorName("雨量计");
        sensor.setAttrList(attrs);
        return sensor;
    }

    private SensorAttribute attr(String code, String name, String unit) {
        SensorAttribute a = new SensorAttribute();
        a.setAttrCode(code);
        a.setAttrName(name);
        a.setUnit(unit);
        return a;
    }

    @Test
    @DisplayName("aggregateAllAttrs — 串行遍历 sensor 下所有 attrCode,逐个调 iotdbService")
    void aggregateAllAttrs_serialPerAttr() {
        when(deviceSensorService.selectSensorByDeviceIdAndCode(1L, "rain_01"))
                .thenReturn(fakeSensor(List.of(
                        attr("rainfall", "雨量", "mm"),
                        attr("battery", "电池电压", "V")
                )));
        when(iotdbService.queryAggregate(eq(1L), eq("rain_01"), any(), any(), any(), any(), any()))
                .thenReturn(List.of(new AggregationResultVO(
                        1L, "rain_01", "rainfall", "雨量", "mm", 0L, Map.of("AVG", 12.5))));

        SensorAggregationVO vo = service.aggregateAllAttrs(
                1L, "rain_01",
                new TimeWindowSpec(0L, 3600_000L, HOUR),
                List.of(new ExpressionSpec.FunctionCall(AggregationFunction.AVG)),
                null, null);

        assertThat(vo.sensorCode()).isEqualTo("rain_01");
        assertThat(vo.results()).hasSize(2);  // 两个 attrCode 各一个结果
    }

    @Test
    @DisplayName("aggregateAllAttrs — sensor 不存在抛 ServiceException")
    void aggregateAllAttrs_sensorNotFound() {
        when(deviceSensorService.selectSensorByDeviceIdAndCode(1L, "missing")).thenReturn(null);

        assertThatThrownBy(() -> service.aggregateAllAttrs(
                1L, "missing",
                new TimeWindowSpec(0L, 3600_000L, HOUR),
                List.of(new ExpressionSpec.FunctionCall(AggregationFunction.AVG)),
                null, null))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("传感器不存在");
    }

    @Test
    @DisplayName("aggregateAllAttrs — attrList 为空抛 ServiceException")
    void aggregateAllAttrs_emptyAttrs() {
        when(deviceSensorService.selectSensorByDeviceIdAndCode(1L, "rain_01"))
                .thenReturn(fakeSensor(List.of()));

        assertThatThrownBy(() -> service.aggregateAllAttrs(
                1L, "rain_01",
                new TimeWindowSpec(0L, 3600_000L, HOUR),
                List.of(new ExpressionSpec.FunctionCall(AggregationFunction.AVG)),
                null, null))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("无监测指标");
    }

    @Test
    @DisplayName("delta — 等价于传 LAST_VALUE - FIRST_VALUE")
    void delta_invokesAggregateWithDeltaExpr() {
        when(deviceSensorService.selectSensorByDeviceIdAndCode(1L, "rain_01"))
                .thenReturn(fakeSensor(List.of(attr("rainfall", "雨量", "mm"))));
        when(iotdbService.queryAggregate(eq(1L), eq("rain_01"), eq("rainfall"), any(), any(), any(), any()))
                .thenReturn(List.of(new AggregationResultVO(
                        1L, "rain_01", "rainfall", "雨量", "mm", 0L, Map.of("DELTA", 0.7))));

        SensorAggregationVO vo = service.delta(1L, "rain_01", new TimeWindowSpec(0L, 3600_000L, HOUR));

        assertThat(vo.results().get(0).metrics()).containsEntry("DELTA", 0.7);
    }
}
```

- [ ] **Step 2: 跑测试,确认失败**

```bash
cd D:/Code/Projects/geo_hazard_monitor/server
mvn -pl zwei-iot-timeseries -Dtest=MonitorDataAggregationServiceTest test
```

预期: **编译失败** — 类不存在

- [ ] **Step 3: 实现 `MonitorDataAggregationService.java`**

```java
package com.zwei.iot.timeseries.service;

import com.zwei.common.exception.ServiceException;
import com.zwei.iot.device.domain.DeviceSensor;
import com.zwei.iot.device.domain.SensorAttribute;
import com.zwei.iot.device.service.IDeviceSensorService;
import com.zwei.iot.timeseries.domain.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 监测数据聚合查询服务 — 支持白名单函数 + 表达式组合 + 数值范围 + 时间窗口。
 *
 * <p>串行遍历 sensor 下所有 attrCode 调 IotdbTimeSeriesService,
 * 合并为 {@link SensorAggregationVO} 返回。</p>
 */
@Service
public class MonitorDataAggregationService {

    private final IotdbTimeSeriesService iotdbService;
    private final IDeviceSensorService deviceSensorService;

    @Autowired
    public MonitorDataAggregationService(IotdbTimeSeriesService iotdbService,
                                         IDeviceSensorService deviceSensorService) {
        this.iotdbService = iotdbService;
        this.deviceSensorService = deviceSensorService;
    }

    /**
     * 批量聚合 — 传感器下所有 attrCode 各算一次,合并返回。
     *
     * @throws ServiceException 传感器不存在或 attrList 为空
     */
    public SensorAggregationVO aggregateAllAttrs(
            Long deviceId, String sensorCode,
            TimeWindowSpec window,
            List<ExpressionSpec> expressions,
            Double minValue, Double maxValue) {
        DeviceSensor sensor = deviceSensorService.selectSensorByDeviceIdAndCode(deviceId, sensorCode);
        if (sensor == null) {
            throw new ServiceException("传感器不存在: deviceId=" + deviceId + ", sensorCode=" + sensorCode);
        }
        List<SensorAttribute> attrs = sensor.getAttrList();
        if (attrs == null || attrs.isEmpty()) {
            throw new ServiceException("该传感器无监测指标: sensorCode=" + sensorCode);
        }
        List<AggregationResultVO> results = new ArrayList<>();
        for (SensorAttribute attribute : attrs) {
            List<AggregationResultVO> attrResults = iotdbService.queryAggregate(
                    deviceId, sensorCode, attribute.getAttrCode(),
                    window, expressions, minValue, maxValue);
            for (AggregationResultVO r : attrResults) {
                results.add(new AggregationResultVO(
                        r.deviceId(), r.sensorCode(), r.attrCode(),
                        attribute.getAttrName(), attribute.getUnit(),
                        r.time(), r.metrics()));
            }
        }
        return new SensorAggregationVO(deviceId, sensorCode, sensor.getSensorName(), results);
    }

    /**
     * 单指标聚合 — 直接代理 iotdbService。
     */
    public List<AggregationResultVO> aggregate(
            Long deviceId, String sensorCode, String attrCode,
            TimeWindowSpec window,
            List<ExpressionSpec> expressions,
            Double minValue, Double maxValue) {
        return iotdbService.queryAggregate(
                deviceId, sensorCode, attrCode, window, expressions, minValue, maxValue);
    }

    /**
     * delta 便捷方法 — 等价于 aggregateAllAttrs 传 LAST_VALUE - FIRST_VALUE。
     */
    public SensorAggregationVO delta(Long deviceId, String sensorCode, TimeWindowSpec window) {
        ExpressionSpec deltaExpr = new ExpressionSpec.BinaryOp(
                new ExpressionSpec.FunctionCall(AggregationFunction.LAST_VALUE),
                ExpressionSpec.BinaryOperator.SUB,
                new ExpressionSpec.FunctionCall(AggregationFunction.FIRST_VALUE));
        return aggregateAllAttrs(deviceId, sensorCode, window, List.of(deltaExpr), null, null);
    }

    /**
     * 传感器下所有 attrCode 的最新值(可按 attrCode 过滤)。
     *
     * @param deviceId   设备ID
     * @param sensorCode 传感器编号
     * @param attrCode   可选,只返回该 attrCode 的最新值
     * @return {@code Map<attrCode, IotdbQueryRow>},attrCode 不存在时 value 为 null
     */
    public java.util.Map<String, IotdbQueryRow> latestBySensor(
            Long deviceId, String sensorCode, String attrCode) {
        DeviceSensor sensor = deviceSensorService.selectSensorByDeviceIdAndCode(deviceId, sensorCode);
        if (sensor == null) {
            throw new ServiceException("传感器不存在: deviceId=" + deviceId + ", sensorCode=" + sensorCode);
        }
        List<SensorAttribute> attrs = sensor.getAttrList();
        if (attrs == null || attrs.isEmpty()) {
            return java.util.Map.of();
        }
        java.util.Map<String, IotdbQueryRow> result = new java.util.LinkedHashMap<>();
        for (SensorAttribute a : attrs) {
            if (attrCode != null && !attrCode.isBlank() && !attrCode.equals(a.getAttrCode())) {
                continue;
            }
            IotdbQueryRow row = iotdbService.queryLatest(deviceId, sensorCode, a.getAttrCode());
            result.put(a.getAttrCode(), row);
        }
        return result;
    }
}
```

- [ ] **Step 4: 跑测试,确认通过**

```bash
mvn -pl zwei-iot-timeseries -Dtest=MonitorDataAggregationServiceTest test
```

预期: **测试通过**

- [ ] **Step 5: Commit**

```bash
cd D:/Code/Projects/geo_hazard_monitor
git add server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/service/MonitorDataAggregationService.java
git add server/zwei-iot-timeseries/src/test/java/com/zwei/iot/timeseries/service/MonitorDataAggregationServiceTest.java
git commit -m "feat(timeseries): 新增 MonitorDataAggregationService 聚合 + delta"
```

---

## Task 13: `MonitorDataAnalysisService`(完整度 + 趋势)

**Files:**
- Create: `server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/service/MonitorDataAnalysisService.java`
- Create: `server/zwei-iot-timeseries/src/test/java/com/zwei/iot/timeseries/service/MonitorDataAnalysisServiceTest.java`

- [ ] **Step 1: 写失败测试**

```java
package com.zwei.iot.timeseries.service;

import com.zwei.common.exception.ServiceException;
import com.zwei.iot.device.domain.DeviceSensor;
import com.zwei.iot.device.domain.SensorAttribute;
import com.zwei.iot.device.service.IDeviceSensorService;
import com.zwei.iot.timeseries.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.zwei.iot.timeseries.domain.TimeWindowSpec.WindowGranularity.RAW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MonitorDataAnalysisService")
class MonitorDataAnalysisServiceTest {

    @Mock private IotdbTimeSeriesService iotdbService;
    @Mock private IDeviceSensorService deviceSensorService;

    private MonitorDataAnalysisService service;

    @BeforeEach
    void setUp() {
        service = new MonitorDataAnalysisService(iotdbService, deviceSensorService);
    }

    private DeviceSensor sensor(String code, List<SensorAttribute> attrs) {
        DeviceSensor s = new DeviceSensor();
        s.setId(1L);
        s.setDeviceId(1L);
        s.setSensorCode(code);
        s.setSensorName("测试传感器");
        s.setAttrList(attrs);
        return s;
    }

    private SensorAttribute attr(String code) {
        SensorAttribute a = new SensorAttribute();
        a.setAttrCode(code);
        a.setAttrName(code);
        return a;
    }

    @Test
    @DisplayName("completeness — 期望点 = 时长 / expectedIntervalMs,实际点来自 iotdb")
    void completeness_basic() {
        when(deviceSensorService.selectSensorByDeviceIdAndCode(1L, "rain_01"))
                .thenReturn(sensor("rain_01", List.of(attr("rainfall"))));
        when(iotdbService.queryCompleteness(eq(1L), eq("rain_01"), eq("rainfall"), any(), eq(60_000L)))
                .thenReturn(new CompletenessReportVO(1L, "rain_01", "rainfall", 60L, 50L, 50.0/60.0, 10.0/60.0, 1700000000000L));

        CompletenessReportVO vo = service.completeness(
                1L, "rain_01", "rainfall",
                new TimeWindowSpec(0L, 3_600_000L, RAW), 60_000L);

        assertThat(vo.expectedPoints()).isEqualTo(60L);
        assertThat(vo.actualPoints()).isEqualTo(50L);
    }

    @Test
    @DisplayName("completeness — expectedIntervalMs 为 null 时直接传 null 给 iotdb(由其兜底)")
    void completeness_nullInterval() {
        when(deviceSensorService.selectSensorByDeviceIdAndCode(1L, "rain_01"))
                .thenReturn(sensor("rain_01", List.of(attr("rainfall"))));
        when(iotdbService.queryCompleteness(eq(1L), eq("rain_01"), eq("rainfall"), any(), eq(null)))
                .thenReturn(new CompletenessReportVO(1L, "rain_01", "rainfall", 60L, 0L, 0.0, 1.0, null));

        CompletenessReportVO vo = service.completeness(
                1L, "rain_01", "rainfall",
                new TimeWindowSpec(0L, 3_600_000L, RAW), null);

        assertThat(vo.completenessRate()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("trend — 直接代理 iotdbService.queryTrend")
    void trend_proxyToIotdb() {
        TrendReportVO expected = new TrendReportVO(1L, "rain_01", "rainfall",
                0L, 3_600_000L, 1.0e-7, 0.36, 8.64, 0.0, 1.0, "rising");
        when(iotdbService.queryTrend(eq(1L), eq("rain_01"), eq("rainfall"), any())).thenReturn(expected);

        TrendReportVO vo = service.trend(1L, "rain_01", "rainfall",
                new TimeWindowSpec(0L, 3_600_000L, RAW));

        assertThat(vo).isEqualTo(expected);
    }

    @Test
    @DisplayName("completeness — sensor 不存在抛 ServiceException")
    void completeness_sensorNotFound() {
        when(deviceSensorService.selectSensorByDeviceIdAndCode(1L, "missing")).thenReturn(null);

        assertThatThrownBy(() -> service.completeness(
                1L, "missing", "rainfall",
                new TimeWindowSpec(0L, 3_600_000L, RAW), 60_000L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("传感器不存在");
    }
}
```

- [ ] **Step 2: 跑测试,确认失败**

```bash
cd D:/Code/Projects/geo_hazard_monitor/server
mvn -pl zwei-iot-timeseries -Dtest=MonitorDataAnalysisServiceTest test
```

预期: **编译失败**

- [ ] **Step 3: 实现 `MonitorDataAnalysisService.java`**

```java
package com.zwei.iot.timeseries.service;

import com.zwei.common.exception.ServiceException;
import com.zwei.iot.device.domain.DeviceSensor;
import com.zwei.iot.device.service.IDeviceSensorService;
import com.zwei.iot.timeseries.domain.CompletenessReportVO;
import com.zwei.iot.timeseries.domain.TimeWindowSpec;
import com.zwei.iot.timeseries.domain.TrendReportVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 监测数据派生分析服务 — 完整度 + 趋势。
 */
@Service
public class MonitorDataAnalysisService {

    private final IotdbTimeSeriesService iotdbService;
    private final IDeviceSensorService deviceSensorService;

    @Autowired
    public MonitorDataAnalysisService(IotdbTimeSeriesService iotdbService,
                                      IDeviceSensorService deviceSensorService) {
        this.iotdbService = iotdbService;
        this.deviceSensorService = deviceSensorService;
    }

    /**
     * 数据完整度统计。
     *
     * @param deviceId          设备ID
     * @param sensorCode        传感器编号
     * @param attrCode          指标编码
     * @param window            时间窗口
     * @param expectedIntervalMs 期望采样间隔(毫秒),为 null 时由 iotdbService 用 60s 兜底
     * @throws ServiceException 传感器不存在
     */
    public CompletenessReportVO completeness(
            Long deviceId, String sensorCode, String attrCode,
            TimeWindowSpec window, Long expectedIntervalMs) {
        DeviceSensor sensor = deviceSensorService.selectSensorByDeviceIdAndCode(deviceId, sensorCode);
        if (sensor == null) {
            throw new ServiceException("传感器不存在: deviceId=" + deviceId + ", sensorCode=" + sensorCode);
        }
        return iotdbService.queryCompleteness(deviceId, sensorCode, attrCode, window, expectedIntervalMs);
    }

    /**
     * 趋势/变化率(端点斜率近似)。
     */
    public TrendReportVO trend(
            Long deviceId, String sensorCode, String attrCode, TimeWindowSpec window) {
        DeviceSensor sensor = deviceSensorService.selectSensorByDeviceIdAndCode(deviceId, sensorCode);
        if (sensor == null) {
            throw new ServiceException("传感器不存在: deviceId=" + deviceId + ", sensorCode=" + sensorCode);
        }
        return iotdbService.queryTrend(deviceId, sensorCode, attrCode, window);
    }
}
```

- [ ] **Step 4: 跑测试,确认通过**

```bash
mvn -pl zwei-iot-timeseries -Dtest=MonitorDataAnalysisServiceTest test
```

预期: **测试通过**

- [ ] **Step 5: Commit**

```bash
cd D:/Code/Projects/geo_hazard_monitor
git add server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/service/MonitorDataAnalysisService.java
git add server/zwei-iot-timeseries/src/test/java/com/zwei/iot/timeseries/service/MonitorDataAnalysisServiceTest.java
git commit -m "feat(timeseries): 新增 MonitorDataAnalysisService 完整度 + 趋势"
```

---

## Task 14: `MonitorDataSensorController` 5 个新端点

**Files:**
- Create: `server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/controller/MonitorDataSensorController.java`
- Create: `server/zwei-iot-timeseries/src/test/java/com/zwei/iot/timeseries/controller/MonitorDataSensorControllerTest.java`

- [ ] **Step 1: 写失败测试 `MonitorDataSensorControllerTest`**

```java
package com.zwei.iot.timeseries.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zwei.iot.timeseries.domain.*;
import com.zwei.iot.timeseries.service.MonitorDataAggregationService;
import com.zwei.iot.timeseries.service.MonitorDataAnalysisService;
import com.zwei.iot.timeseries.service.IotdbTimeSeriesService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static com.zwei.iot.timeseries.domain.TimeWindowSpec.WindowGranularity.HOUR;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MonitorDataSensorController.class)
@DisplayName("MonitorDataSensorController")
class MonitorDataSensorControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private MonitorDataAggregationService aggregationService;
    @MockBean private MonitorDataAnalysisService analysisService;
    @MockBean private IotdbTimeSeriesService iotdbService;

    @Test
    @DisplayName("/latest — 200 返回 sensor 下所有 attr 最新值")
    void latest_ok() throws Exception {
        when(aggregationService.latestBySensor(eq(1L), eq("rain_01"), any()))
                .thenReturn(Map.of("rainfall", new IotdbQueryRow(1700000000000L, 12.5, 0)));

        mockMvc.perform(get("/api/v1/monitor-data/sensor/latest")
                        .param("deviceId", "1")
                        .param("sensorCode", "rain_01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.rainfall.value").value(12.5));
    }

    @Test
    @DisplayName("/aggregate — POST 接表达式 body,200 返回聚合结果")
    void aggregate_ok() throws Exception {
        ExpressionSpec expr = new ExpressionSpec.BinaryOp(
                new ExpressionSpec.FunctionCall(AggregationFunction.LAST_VALUE),
                ExpressionSpec.BinaryOperator.SUB,
                new ExpressionSpec.FunctionCall(AggregationFunction.FIRST_VALUE));
        SensorAggregationVO vo = new SensorAggregationVO(1L, "rain_01", "雨量计",
                List.of(new AggregationResultVO(1L, "rain_01", "rainfall", "雨量", "mm", 0L, Map.of("DELTA", 0.7))));

        when(aggregationService.aggregateAllAttrs(eq(1L), eq("rain_01"), any(), any(), any(), any()))
                .thenReturn(vo);

        mockMvc.perform(post("/api/v1/monitor-data/sensor/aggregate")
                        .param("deviceId", "1")
                        .param("sensorCode", "rain_01")
                        .param("startTime", "2024-01-01 00:00:00")
                        .param("endTime", "2024-01-02 00:00:00")
                        .param("granularity", "hour")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(expr))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.results[0].metrics.DELTA").value(0.7));
    }

    @Test
    @DisplayName("/completeness — 200 返回完整度报告")
    void completeness_ok() throws Exception {
        CompletenessReportVO report = new CompletenessReportVO(1L, "rain_01", "rainfall", 60L, 50L, 0.83, 0.17, 1700000000000L);
        when(analysisService.completeness(eq(1L), eq("rain_01"), eq("rainfall"), any(), eq(60_000L)))
                .thenReturn(report);

        mockMvc.perform(get("/api/v1/monitor-data/sensor/completeness")
                        .param("deviceId", "1")
                        .param("sensorCode", "rain_01")
                        .param("attrCode", "rainfall")
                        .param("startTime", "2024-01-01 00:00:00")
                        .param("endTime", "2024-01-02 00:00:00")
                        .param("expectedIntervalMs", "60000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.completenessRate").value(0.83));
    }

    @Test
    @DisplayName("/trend — 200 返回趋势报告")
    void trend_ok() throws Exception {
        TrendReportVO report = new TrendReportVO(1L, "rain_01", "rainfall", 0L, 3_600_000L,
                1.0e-7, 0.36, 8.64, 0.0, 1.0, "rising");
        when(analysisService.trend(eq(1L), eq("rain_01"), eq("rainfall"), any())).thenReturn(report);

        mockMvc.perform(get("/api/v1/monitor-data/sensor/trend")
                        .param("deviceId", "1")
                        .param("sensorCode", "rain_01")
                        .param("attrCode", "rainfall")
                        .param("startTime", "2024-01-01 00:00:00")
                        .param("endTime", "2024-01-02 00:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trendDirection").value("rising"));
    }

    @Test
    @DisplayName("/range — 200 返回区间数据")
    void range_ok() throws Exception {
        when(iotdbService.queryRangeBySensor(eq(1L), eq("rain_01"), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(Map.of("rainfall", List.of(new IotdbQueryRow(1700000000000L, 12.5, 0))));

        mockMvc.perform(get("/api/v1/monitor-data/sensor/range")
                        .param("deviceId", "1")
                        .param("sensorCode", "rain_01")
                        .param("startTime", "2024-01-01 00:00:00")
                        .param("endTime", "2024-01-02 00:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rainfall[0].value").value(12.5));
    }
}
```

- [ ] **Step 2: 跑测试,确认失败**

```bash
cd D:/Code/Projects/geo_hazard_monitor/server
mvn -pl zwei-iot-timeseries -Dtest=MonitorDataSensorControllerTest test
```

预期: **编译失败** — Controller 不存在

- [ ] **Step 3: 实现 `MonitorDataSensorController.java`**

```java
package com.zwei.iot.timeseries.controller;

import com.zwei.common.core.domain.AjaxResult;
import com.zwei.common.utils.DateUtils;
import com.zwei.common.utils.StringUtils;
import com.zwei.iot.timeseries.domain.*;
import com.zwei.iot.timeseries.service.MonitorDataAggregationService;
import com.zwei.iot.timeseries.service.MonitorDataAnalysisService;
import com.zwei.iot.timeseries.service.IotdbTimeSeriesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 传感器维度监测数据查询接口。
 *
 * <p>基于 (deviceId, sensorCode) 入口,提供批量最新值、区间数据、多表达式聚合、完整度、趋势。
 * 与现有 hazardPointId 维度的 /latest /page /chart 接口并存,前端可按需选择调用。</p>
 */
@RestController
@RequestMapping("/api/v1/monitor-data/sensor")
@PreAuthorize("@ss.hasPermi('basic:device:query')")
public class MonitorDataSensorController {

    private final IotdbTimeSeriesService iotdbTimeSeriesService;
    private final MonitorDataAggregationService aggregationService;
    private final MonitorDataAnalysisService analysisService;

    @Autowired
    public MonitorDataSensorController(
            IotdbTimeSeriesService iotdbTimeSeriesService,
            MonitorDataAggregationService aggregationService,
            MonitorDataAnalysisService analysisService) {
        this.iotdbTimeSeriesService = iotdbTimeSeriesService;
        this.aggregationService = aggregationService;
        this.analysisService = analysisService;
    }

    /** 1. 传感器下所有指标最新值(可指定 attrCode 过滤) */
    @GetMapping("/latest")
    public AjaxResult latest(@RequestParam Long deviceId,
                             @RequestParam String sensorCode,
                             @RequestParam(required = false) String attrCode) {
        return AjaxResult.success("成功",
                aggregationService.latestBySensor(deviceId, sensorCode, attrCode));
    }

    /** 2. 区间数据(支持数值范围) */
    @GetMapping("/range")
    public AjaxResult range(@RequestParam Long deviceId,
                            @RequestParam String sensorCode,
                            @RequestParam(required = false) String attrCode,
                            @RequestParam String startTime,
                            @RequestParam String endTime,
                            @RequestParam(required = false) Double minValue,
                            @RequestParam(required = false) Double maxValue,
                            @RequestParam(defaultValue = "5000") int limit,
                            @RequestParam(defaultValue = "0") int offset) {
        Long startMillis = toMillis(startTime);
        Long endMillis = toMillis(endTime);
        // 单 attrCode 模式;后续可扩展 sensor 下批量
        List<String> attrCodes = StringUtils.isBlank(attrCode) ? List.of() : List.of(attrCode);
        return AjaxResult.success("成功",
                iotdbTimeSeriesService.queryRangeBySensor(deviceId, sensorCode, attrCodes,
                        startMillis, endMillis, minValue, maxValue, limit, offset));
    }

    /** 3. 多表达式聚合(POST 因 body 复杂) */
    @PostMapping("/aggregate")
    public AjaxResult aggregate(@RequestParam Long deviceId,
                                @RequestParam String sensorCode,
                                @RequestParam String startTime,
                                @RequestParam String endTime,
                                @RequestParam(required = false, defaultValue = "raw") String granularity,
                                @RequestParam(required = false) Double minValue,
                                @RequestParam(required = false) Double maxValue,
                                @RequestBody List<ExpressionSpec> expressions) {
        TimeWindowSpec window = new TimeWindowSpec(
                toMillis(startTime), toMillis(endTime),
                TimeWindowSpec.WindowGranularity.valueOf(granularity.toUpperCase()));
        SensorAggregationVO vo = aggregationService.aggregateAllAttrs(
                deviceId, sensorCode, window, expressions, minValue, maxValue);
        return AjaxResult.success("成功", vo);
    }

    /** 4. 完整度 */
    @GetMapping("/completeness")
    public AjaxResult completeness(@RequestParam Long deviceId,
                                   @RequestParam String sensorCode,
                                   @RequestParam String attrCode,
                                   @RequestParam String startTime,
                                   @RequestParam String endTime,
                                   @RequestParam(required = false) Long expectedIntervalMs) {
        TimeWindowSpec window = new TimeWindowSpec(
                toMillis(startTime), toMillis(endTime), TimeWindowSpec.WindowGranularity.RAW);
        return AjaxResult.success("成功",
                analysisService.completeness(deviceId, sensorCode, attrCode, window, expectedIntervalMs));
    }

    /** 5. 趋势(端点斜率近似) */
    @GetMapping("/trend")
    public AjaxResult trend(@RequestParam Long deviceId,
                            @RequestParam String sensorCode,
                            @RequestParam String attrCode,
                            @RequestParam String startTime,
                            @RequestParam String endTime) {
        TimeWindowSpec window = new TimeWindowSpec(
                toMillis(startTime), toMillis(endTime), TimeWindowSpec.WindowGranularity.RAW);
        return AjaxResult.success("成功",
                analysisService.trend(deviceId, sensorCode, attrCode, window));
    }

    private Long toMillis(String text) {
        if (StringUtils.isBlank(text)) {
            return null;
        }
        return DateUtils.parseDate(text).getTime();
    }
}
```

- [ ] **Step 4: 跑测试,确认通过**

```bash
mvn -pl zwei-iot-timeseries -Dtest=MonitorDataSensorControllerTest test
```

预期: **所有 5 个测试通过**

- [ ] **Step 5: Commit**

```bash
cd D:/Code/Projects/geo_hazard_monitor
git add server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/controller/MonitorDataSensorController.java
git add server/zwei-iot-timeseries/src/test/java/com/zwei/iot/timeseries/controller/MonitorDataSensorControllerTest.java
git commit -m "feat(timeseries): 新增 MonitorDataSensorController 5 个端点"
```

---

## Task 15: 集成测试(Testcontainers IoTDB 2.0)

**Files:**
- Create: `server/zwei-iot-timeseries/src/test/resources/junit-platform.properties`(可选,启用 IT 命名)
- Create: `server/zwei-iot-timeseries/src/test/java/com/zwei/iot/timeseries/integration/MonitorDataQueryIntegrationIT.java`
- Modify: `server/zwei-iot-timeseries/pom.xml`(加 testcontainers 依赖)

- [ ] **Step 1: 在 `pom.xml` 添加 testcontainers 依赖**

在 `<dependencies>` 块中,在现有 test 依赖下添加:

```xml
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>testcontainers</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
```

并在父 POM `server/pom.xml` 的 `<dependencyManagement>` 块中确认 testcontainers 版本已管理(若没有,加):

```xml
                <dependency>
                    <groupId>org.testcontainers</groupId>
                    <artifactId>testcontainers-bom</artifactId>
                    <version>1.19.7</version>
                    <type>pom</type>
                    <scope>import</scope>
                </dependency>
```

- [ ] **Step 2: 创建 `junit-platform.properties`(启用 IT 命名)**

`server/zwei-iot-timeseries/src/test/resources/junit-platform.properties`:

```properties
# 集成测试默认排除,只跑 -P integration 时才跑
includeTags=
```

或者用 Maven Failsafe(若项目已配置),本任务用 Failsafe 替代。

**前提:** 检查项目根 `server/pom.xml` 是否已配 Failsafe;若没有,跳过本步骤的 `junit-platform.properties` 创建,直接用 `@DisabledIfSystemProperty(named = "skipITs", matches = "true")` 标记。

- [ ] **Step 3: 创建集成测试 `MonitorDataQueryIntegrationIT.java`**

```java
package com.zwei.iot.timeseries.integration;

import com.zwei.iot.timeseries.config.IotdbProperties;
import com.zwei.iot.timeseries.domain.*;
import com.zwei.iot.timeseries.service.IotdbTimeSeriesService;
import com.zwei.iot.timeseries.support.IotdbPathResolver;
import com.zwei.iot.timeseries.domain.ExpressionSpecRenderer;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import static com.zwei.iot.timeseries.domain.ExpressionSpec.BinaryOperator.SUB;
import static com.zwei.iot.timeseries.domain.TimeWindowSpec.WindowGranularity.HOUR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

@Testcontainers
@DisabledIfSystemProperty(named = "skipITs", matches = "true")
@DisplayName("IoTDB 2.0 集成测试 (Testcontainers)")
class MonitorDataQueryIntegrationIT {

    @Container
    static GenericContainer<?> iotdb = new GenericContainer<>(DockerImageName.parse("apache/iotdb:2.0.2-standalone"))
            .withExposedPorts(6667);

    private static IotdbTimeSeriesService service;
    private static IotdbJdbcClient jdbcClient;
    private static IotdbPathResolver pathResolver;

    @BeforeAll
    static void setUp() throws Exception {
        assumeThat(iotdb.isRunning()).isTrue();
        String host = iotdb.getHost();
        Integer port = iotdb.getMappedPort(6667);

        IotdbProperties props = new IotdbProperties();
        props.setHost(host);
        props.setPort(port);
        props.setDatabase("root.zwei_test");
        props.setUsername("root");
        props.setPassword("root");

        jdbcClient = new IotdbJdbcClient(props);
        pathResolver = new IotdbPathResolver(props);
        ExpressionSpecRenderer renderer = new ExpressionSpecRenderer();
        service = new IotdbTimeSeriesService(jdbcClient, props, pathResolver, renderer);

        // 准备测试数据:3 个数据点,值分别为 10.0, 12.0, 18.0
        try (Connection conn = DriverManager.getConnection(
                String.format("jdbc:iotdb://%s:%d/", host, port), "root", "root");
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE DATABASE root.zwei_test");
            // 触发 schema 创建
            service.createSensorSchema(1L, "rain_01", List.of("rainfall"));
            long t0 = 1_700_000_000_000L;
            stmt.execute(String.format(
                    "INSERT INTO root.zwei_test.d1.srain_01(timestamp,rainfall,quality) ALIGNED VALUES(%d,%d,%d)",
                    t0, 10, 0));
            stmt.execute(String.format(
                    "INSERT INTO root.zwei_test.d1.srain_01(timestamp,rainfall,quality) ALIGNED VALUES(%d,%d,%d)",
                    t0 + 1_800_000, 12, 0));   // +30 分钟
            stmt.execute(String.format(
                    "INSERT INTO root.zwei_test.d1.srain_01(timestamp,rainfall,quality) ALIGNED VALUES(%d,%d,%d)",
                    t0 + 3_600_000, 18, 0));   // +60 分钟
        }
    }

    @Test
    @DisplayName("queryAggregate — 单 AVG 在 1h GROUP BY 下算得 10 / 12 / 18 的 AVG")
    void aggregate_avg_oneHour() {
        TimeWindowSpec window = new TimeWindowSpec(
                1_700_000_000_000L, 1_700_000_000_000L + 3_600_000L + 1, HOUR);

        List<AggregationResultVO> results = service.queryAggregate(
                1L, "rain_01", "rainfall", window,
                List.of(new ExpressionSpec.FunctionCall(AggregationFunction.AVG)),
                null, null);

        assertThat(results).isNotEmpty();
        // 3 个数据点分属 1h / 1h / 1h 三个 1h 分组,AVG 分别为 10, 12, 18
        assertThat(results).extracting(r -> r.metrics().get("AVG"))
                .contains(10.0, 12.0, 18.0);
    }

    @Test
    @DisplayName("queryAggregate — delta 表达式 LAST-FIRST 在全窗口算得 18 - 10 = 8")
    void aggregate_delta() {
        TimeWindowSpec window = new TimeWindowSpec(
                1_700_000_000_000L, 1_700_000_000_000L + 3_600_000L + 1, HOUR);

        ExpressionSpec delta = new ExpressionSpec.BinaryOp(
                new ExpressionSpec.FunctionCall(AggregationFunction.LAST_VALUE), SUB,
                new ExpressionSpec.FunctionCall(AggregationFunction.FIRST_VALUE));

        List<AggregationResultVO> results = service.queryAggregate(
                1L, "rain_01", "rainfall", window, List.of(delta), null, null);

        // 单 1h 分组(实际数据全在 0-60min 区间)
        assertThat(results).isNotEmpty();
        assertThat(results.get(0).metrics().get("DELTA")).isEqualTo(8.0);
    }

    @Test
    @DisplayName("queryTrend — 上升趋势 direction='rising'")
    void trend_rising() {
        TrendReportVO vo = service.queryTrend(1L, "rain_01", "rainfall",
                new TimeWindowSpec(1_700_000_000_000L, 1_700_000_000_000L + 3_600_000L + 1,
                        TimeWindowSpec.WindowGranularity.RAW));

        assertThat(vo.trendDirection()).isEqualTo("rising");
        assertThat(vo.slopePerMs()).isGreaterThan(0);
    }

    @Test
    @DisplayName("queryCompleteness — 3 个数据点 / 期望间隔 1.8e6ms = 1 个分组,各 1 点")
    void completeness_basic() {
        CompletenessReportVO vo = service.queryCompleteness(1L, "rain_01", "rainfall",
                new TimeWindowSpec(1_700_000_000_000L, 1_700_000_000_000L + 3_600_000L + 1,
                        TimeWindowSpec.WindowGranularity.RAW),
                1_800_000L);

        assertThat(vo.actualPoints()).isEqualTo(3L);
        // 期望 = 3,600,001 / 1,800,000 ≈ 2
        assertThat(vo.expectedPoints()).isGreaterThanOrEqualTo(2L);
    }
}
```

- [ ] **Step 4: 跑测试(需 Docker 可用,默认 `-DskipITs=true` 跳过)**

```bash
mvn -pl zwei-iot-timeseries -Dtest=MonitorDataQueryIntegrationIT verify -DskipITs=false
```

预期: Docker 拉取 `apache/iotdb:2.0.2-standalone` 镜像,启动容器,所有 4 个测试通过

- [ ] **Step 5: 默认 `mvn test` 跳过**

```bash
mvn -pl zwei-iot-timeseries test
```

预期: 集成测试被 `skipITs=true` 跳过,只跑单元测试

- [ ] **Step 6: Commit**

```bash
cd D:/Code/Projects/geo_hazard_monitor
git add server/zwei-iot-timeseries/pom.xml
git add server/zwei-iot-timeseries/src/test/java/com/zwei/iot/timeseries/integration/
git commit -m "test(timeseries): 新增 Testcontainers IoTDB 集成测试"
```

---

## Task 16: 文档同步

**Files:**
- Modify: `server/zwei-iot-timeseries/CLAUDE.md`
- Modify: `CLAUDE.md`(根)
- Modify: `.claude/index.json`

- [ ] **Step 1: 修改 `server/zwei-iot-timeseries/CLAUDE.md`**

在 "## 对外接口 (Controller)" 表格后,**新增** "## 查询能力矩阵" 小节:

```markdown
## 查询能力矩阵

| 维度 | hazardPointId 入口(保留) | sensorCode 入口(新增) |
|---|---|---|
| 最新值 | `/api/v1/monitor-data/latest` | `/api/v1/monitor-data/sensor/latest` |
| 区间数据 | `/api/v1/monitor-data/page` | `/api/v1/monitor-data/sensor/range` (支持 minValue/maxValue 数值 WHERE) |
| 图表 + max/min/avg | `/api/v1/monitor-data/chart` | `/api/v1/monitor-data/sensor/aggregate` (多表达式 + 数值范围) |
| delta (LAST-FIRST) | — | `/api/v1/monitor-data/sensor/aggregate` (传 `LAST_VALUE - FIRST_VALUE` 表达式) |
| 完整度 | — | `/api/v1/monitor-data/sensor/completeness` |
| 趋势/变化率 | — | `/api/v1/monitor-data/sensor/trend` |

## ExpressionSpec DSL

`AggregationFunction` 白名单枚举:`AVG/MAX/MIN/SUM/COUNT/FIRST_VALUE/LAST_VALUE/EXTREME/STDDEV/P50/P95/P99`。

表达式组合通过 sealed `ExpressionSpec`:
- `FunctionCall(func)` — 单函数
- `BinaryOp(left, op, right)` — 二元运算,op ∈ {+, -, *, /}
- `Constant(value)` — 标量

常见组合:
- `MAX - MIN` → 极差(别名 `MAX-MIN`)
- `LAST_VALUE - FIRST_VALUE` → **delta**(别名 `DELTA`,自动映射)
- `(MAX - MIN) / AVG` → 变异系数(别名 `(MAX-MIN)/AVG`)

## 安全边界

- SQL 拼接仅从 `AggregationFunction` 枚举取值,无法注入任意函数
- `ExpressionSpec` 是 sealed interface,子类型编译期固定,无法运行时注入新节点
- 表达式嵌套深度 ≤ 5,别名长度 ≤ 64,字符仅允许 `[a-zA-Z0-9_\-()/]`
- `minValue`/`maxValue` 是 `Double`,Java 类型保证是数字
- 路径 `attrCode`/`sensorCode` 走 `IotdbPathResolver`,不直接拼 SQL 字符串
```

并把 "## 核心实现类索引 (P0)" 表格中追加 2 行:

```markdown
| `MonitorDataAggregationService`  | `service/MonitorDataAggregationService.java` | `aggregate` / `aggregateAllAttrs` / `delta` — 表达式驱动聚合 |
| `MonitorDataAnalysisService`     | `service/MonitorDataAnalysisService.java`    | `completeness` / `trend` — 派生统计 |
```

"## 主要子包" 表格的 controller 行追加 1 个:

```markdown
| `controller` | `MonitorDataController` (现有) + `MonitorDataSensorController` (新增,sensor 维度 5 端点) |
```

并在 Changelog 表格追加一行:

```markdown
| 2026-06-14 14:30 | 新增查询能力增强: ExpressionSpec DSL + 数值范围 + 完整度/趋势 — 详见 `docs/superpowers/specs/2026-06-14-timeseries-query-enhancement-design.md` |
```

- [ ] **Step 2: 修改根 `CLAUDE.md`**

"### 模块索引" 表格中,`zwei-iot-timeseries` 行的 "一句话职责" 改为:

```
IoTDB 读写 + 监测数据查询 + 表达式驱动聚合 + 完整度/趋势
```

并在 Changelog 表格追加一行:

```markdown
| 2026-06-14 14:30 | 增强 iot-timeseries 查询能力: 7 domain + 2 service + 1 controller + 6 IotdbTimeSeriesService 新方法 — 详见 `docs/superpowers/specs/2026-06-14-timeseries-query-enhancement-design.md` |
```

- [ ] **Step 3: 修改 `.claude/index.json`**

在 services 数组中,`zwei-iot-timeseries` 下追加 2 条:

```json
{
  "name": "MonitorDataAggregationService",
  "class": "com.zwei.iot.timeseries.service.MonitorDataAggregationService",
  "responsibility": "表达式驱动聚合查询:白名单函数 + 表达式组合 + 数值范围 + 时间窗口 GROUP BY"
},
{
  "name": "MonitorDataAnalysisService",
  "class": "com.zwei.iot.timeseries.service.MonitorDataAnalysisService",
  "responsibility": "派生统计:数据完整度(CompletenessReportVO) + 趋势/变化率(TrendReportVO)"
}
```

- [ ] **Step 4: 跑全量单元测试,确认无回归**

```bash
cd D:/Code/Projects/geo_hazard_monitor/server
mvn -pl zwei-iot-timeseries test
```

预期: 全部单元测试通过(集成测试默认 `-DskipITs=true` 跳过)

- [ ] **Step 5: 跑覆盖率,目标 ≥ 80%**

```bash
mvn -pl zwei-iot-timeseries test jacoco:report
```

查看 `target/site/jacoco/index.html`,确认 `IotdbTimeSeriesService` / `MonitorDataAggregationService` / `MonitorDataAnalysisService` / `MonitorDataSensorController` 覆盖率 ≥ 80%

- [ ] **Step 6: Commit**

```bash
cd D:/Code/Projects/geo_hazard_monitor
git add server/zwei-iot-timeseries/CLAUDE.md
git add CLAUDE.md
git add .claude/index.json
git commit -m "docs(timeseries): 同步查询能力增强文档"
```

---

# 完成 ✅

**预期产出:**
- 16 个新文件(7 domain + 2 service + 1 controller + 6 test) + 1 个修改文件(IotdbTimeSeriesService)
- 16 个 commit,每个 commit 独立可回滚
- 单元测试覆盖率 ≥ 80%
- Testcontainers 集成测试,`mvn verify -DskipITs=false` 单独跑

**下一步:**
- 阶段 1(任务 1-11):基础设施,预计 1 天
- 阶段 2(任务 12-16):API 层 + 集成测试 + 文档,预计 1 天
- 真实 IoTDB 上线后用线上数据回归测试,校准趋势/完整度
