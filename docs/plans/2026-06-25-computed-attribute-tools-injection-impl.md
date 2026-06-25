# 计算属性脚本工具注入 实现计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 让 `ComputedAttributeEvaluator` 调用的 Groovy 脚本通过 `cache.*` 与 `sensor.*` 实例方法访问 `CacheUtil` 与 `SensorDataQueryUtil`，不破坏 parser 模块边界与沙箱安全性。

**Architecture:** 在 `zwei-iot-timeseries/compute/` 新增两个 `@Component` wrapper (`ScriptCacheOps` / `ScriptSensorQuery`) 包装静态工具类。给 `GroovyScriptEngine.executeComputed` 增加 4 参重载（`extraBindings` Map），旧 3 参版本委托保留向后兼容。调用方构建 `Map.of("cache", cacheOps, "sensor", sensorQuery)` 透传。

**Tech Stack:** Java 17, Spring Boot 4.0.3, JUnit 5 + Mockito 5 (含 `mockStatic`) + AssertJ, Groovy 沙箱 (`SecureASTCustomizer`)。

**Spec:** `docs/superpowers/specs/2026-06-25-computed-attribute-tools-injection-design.md`

---

## 关键约定

- **TDD 严格遵守**: 每个任务先写测试看 RED，再写实现看 GREEN，再 commit。
- **包路径**:
  - wrapper 主代码: `com.zwei.iot.timeseries.compute`
  - wrapper 测试: `com.zwei.iot.timeseries.compute`
  - parser 改造: `com.zwei.iot.parser.engine`
- **不要**为 wrapper 添加额外业务逻辑 — 仅"实例外壳"委托静态方法。任何增强是 YAGNI。
- **异常策略差异**:
  - `ScriptCacheOps`: 异常**透传**（cache 失败应让脚本感知）
  - `ScriptSensorQuery`: 异常**吞噬**返回 null（主链路保护）
- **mockito-inline**: `spring-boot-starter-test` 已包含 mockito-core，但 `mockStatic` 需要 mockito-inline。如果运行时报错，在 `zwei-iot-timeseries/pom.xml` 的 `mockito` 依赖下加 `<classifier>inline</classifier>` 或显式加 `mockito-inline` 依赖。先尝试不加，遇到错误再加。

---

## Task 1: ScriptCacheOps — 基础 getter 委托

**Files:**
- Create: `server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/compute/ScriptCacheOps.java`
- Create: `server/zwei-iot-timeseries/src/test/java/com/zwei/iot/timeseries/compute/ScriptCacheOpsTest.java`

**Step 1.1: 写失败测试 — getter 委托**

`ScriptCacheOpsTest.java`:

```java
package com.zwei.iot.timeseries.compute;

import com.zwei.common.utils.CacheUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

@DisplayName("ScriptCacheOps (CacheUtil 实例外壳)")
class ScriptCacheOpsTest {

    @Test
    @DisplayName("getInt(key) 委托 CacheUtil.getInt")
    void getIntDelegates() {
        try (MockedStatic<CacheUtil> mocked = mockStatic(CacheUtil.class)) {
            mocked.when(() -> CacheUtil.getInt("k")).thenReturn(42);
            assertThat(new ScriptCacheOps().getInt("k")).isEqualTo(42);
            mocked.verify(() -> CacheUtil.getInt("k"));
        }
    }

    @Test
    @DisplayName("getInt(key, default) 委托默认值重载")
    void getIntDefaultDelegates() {
        try (MockedStatic<CacheUtil> mocked = mockStatic(CacheUtil.class)) {
            mocked.when(() -> CacheUtil.getInt("k", 7)).thenReturn(7);
            assertThat(new ScriptCacheOps().getInt("k", 7)).isEqualTo(7);
            mocked.verify(() -> CacheUtil.getInt("k", 7));
        }
    }
}
```

**Step 1.2: 运行测试验证 RED**

```bash
cd server && mvn test -pl zwei-iot-timeseries -Dtest=ScriptCacheOpsTest -q
```
Expected: FAIL (ScriptCacheOps 不存在 / 方法缺失 → 编译错误)

**Step 1.3: 写最小实现**

`ScriptCacheOps.java`:

```java
package com.zwei.iot.timeseries.compute;

import com.zwei.common.utils.CacheUtil;
import org.springframework.stereotype.Component;

/**
 * Groovy 脚本可调用的 Redis 缓存实例外壳 — 委托 {@link CacheUtil} 静态方法。
 *
 * <p>沙箱禁 {@code Class} receiver, 无法直接绑定 {@code CacheUtil.class} 让脚本调静态方法。
 * 本类作为 {@code @Component} 实例注入到脚本 Binding 的 {@code cache} 变量。
 *
 * <p>异常策略: <b>透传</b> — cache 失败应让脚本感知 (与 ScriptSensorQuery 的"吞噬"相反)。
 */
@Component
public class ScriptCacheOps {

    public Integer getInt(String key) { return CacheUtil.getInt(key); }
    public int getInt(String key, int defaultValue) { return CacheUtil.getInt(key, defaultValue); }
}
```

**Step 1.4: 运行测试验证 GREEN**

```bash
cd server && mvn test -pl zwei-iot-timeseries -Dtest=ScriptCacheOpsTest -q
```
Expected: 2 tests PASS

**Step 1.5: Commit**

```bash
git add server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/compute/ScriptCacheOps.java \
        server/zwei-iot-timeseries/src/test/java/com/zwei/iot/timeseries/compute/ScriptCacheOpsTest.java
git commit -m "feat(compute): ScriptCacheOps 基础 getInt 委托 (TDD)"
```

---

## Task 2: ScriptCacheOps — 补全其余 19 个方法

**Files:**
- Modify: `server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/compute/ScriptCacheOps.java`
- Modify: `server/zwei-iot-timeseries/src/test/java/com/zwei/iot/timeseries/compute/ScriptCacheOpsTest.java`

**Step 2.1: 扩充测试 — 剩余 getter + 通用操作**

在 `ScriptCacheOpsTest.java` 追加以下测试方法（保留 Task 1 的两个）：

```java
    @Test
    @DisplayName("getLong / getDouble / getFloat / getBigDecimal 委托")
    void numericGettersDelegate() {
        try (MockedStatic<CacheUtil> mocked = mockStatic(CacheUtil.class)) {
            mocked.when(() -> CacheUtil.getLong("k")).thenReturn(100L);
            mocked.when(() -> CacheUtil.getLong("k", 1L)).thenReturn(100L);
            mocked.when(() -> CacheUtil.getDouble("k")).thenReturn(3.14);
            mocked.when(() -> CacheUtil.getDouble("k", 0.0)).thenReturn(3.14);
            mocked.when(() -> CacheUtil.getFloat("k")).thenReturn(1.5f);
            mocked.when(() -> CacheUtil.getFloat("k", 0.0f)).thenReturn(1.5f);
            BigDecimal bd = new BigDecimal("99.9");
            mocked.when(() -> CacheUtil.getBigDecimal("k")).thenReturn(bd);
            mocked.when(() -> CacheUtil.getBigDecimal("k", BigDecimal.ZERO)).thenReturn(bd);

            ScriptCacheOps ops = new ScriptCacheOps();
            assertThat(ops.getLong("k")).isEqualTo(100L);
            assertThat(ops.getLong("k", 1L)).isEqualTo(100L);
            assertThat(ops.getDouble("k")).isEqualTo(3.14);
            assertThat(ops.getDouble("k", 0.0)).isEqualTo(3.14);
            assertThat(ops.getFloat("k")).isEqualTo(1.5f);
            assertThat(ops.getFloat("k", 0.0f)).isEqualTo(1.5f);
            assertThat(ops.getBigDecimal("k")).isEqualByComparingTo(bd);
            assertThat(ops.getBigDecimal("k", BigDecimal.ZERO)).isEqualByComparingTo(bd);

            mocked.verify(() -> CacheUtil.getLong("k"));
            mocked.verify(() -> CacheUtil.getLong("k", 1L));
            mocked.verify(() -> CacheUtil.getDouble("k"));
            mocked.verify(() -> CacheUtil.getDouble("k", 0.0));
            mocked.verify(() -> CacheUtil.getFloat("k"));
            mocked.verify(() -> CacheUtil.getFloat("k", 0.0f));
            mocked.verify(() -> CacheUtil.getBigDecimal("k"));
            mocked.verify(() -> CacheUtil.getBigDecimal("k", BigDecimal.ZERO));
        }
    }

    @Test
    @DisplayName("getString / getBoolean 委托")
    void stringAndBooleanDelegate() {
        try (MockedStatic<CacheUtil> mocked = mockStatic(CacheUtil.class)) {
            mocked.when(() -> CacheUtil.getString("k")).thenReturn("v");
            mocked.when(() -> CacheUtil.getString("k", "d")).thenReturn("v");
            mocked.when(() -> CacheUtil.getBoolean("k")).thenReturn(true);
            mocked.when(() -> CacheUtil.getBoolean("k", false)).thenReturn(true);

            ScriptCacheOps ops = new ScriptCacheOps();
            assertThat(ops.getString("k")).isEqualTo("v");
            assertThat(ops.getString("k", "d")).isEqualTo("v");
            assertThat(ops.getBoolean("k")).isTrue();
            assertThat(ops.getBoolean("k", false)).isTrue();

            mocked.verify(() -> CacheUtil.getString("k"));
            mocked.verify(() -> CacheUtil.getString("k", "d"));
            mocked.verify(() -> CacheUtil.getBoolean("k"));
            mocked.verify(() -> CacheUtil.getBoolean("k", false));
        }
    }

    @Test
    @DisplayName("通用操作 set/delete/hasKey/expire/getExpire 委托")
    void universalOpsDelegate() {
        try (MockedStatic<CacheUtil> mocked = mockStatic(CacheUtil.class)) {
            ScriptCacheOps ops = new ScriptCacheOps();

            ops.set("k", "v");
            mocked.verify(() -> CacheUtil.set("k", "v"));

            ops.set("k", "v", 30L, TimeUnit.MINUTES);
            mocked.verify(() -> CacheUtil.set("k", "v", 30L, TimeUnit.MINUTES));

            mocked.when(() -> CacheUtil.delete("k")).thenReturn(true);
            assertThat(ops.delete("k")).isTrue();

            mocked.when(() -> CacheUtil.hasKey("k")).thenReturn(true);
            assertThat(ops.hasKey("k")).isTrue();

            mocked.when(() -> CacheUtil.expire("k", 60L)).thenReturn(true);
            assertThat(ops.expire("k", 60L)).isTrue();

            mocked.when(() -> CacheUtil.expire("k", 5L, TimeUnit.SECONDS)).thenReturn(true);
            assertThat(ops.expire("k", 5L, TimeUnit.SECONDS)).isTrue();

            mocked.when(() -> CacheUtil.getExpire("k")).thenReturn(42L);
            assertThat(ops.getExpire("k")).isEqualTo(42L);
        }
    }

    @Test
    @DisplayName("CacheUtil 抛异常时 wrapper 透传 (不吞噬)")
    void exceptionPropagated() {
        try (MockedStatic<CacheUtil> mocked = mockStatic(CacheUtil.class)) {
            mocked.when(() -> CacheUtil.getInt("k"))
                  .thenThrow(new RuntimeException("redis down"));
            new ScriptCacheOps().getInt("k");
        }  // AssertJ 异常断言见下
    }
```

把上面最后一个测试的断言替换为：

```java
    @Test
    @DisplayName("CacheUtil 抛异常时 wrapper 透传 (不吞噬)")
    void exceptionPropagated() {
        try (MockedStatic<CacheUtil> mocked = mockStatic(CacheUtil.class)) {
            mocked.when(() -> CacheUtil.getInt("k"))
                  .thenThrow(new RuntimeException("redis down"));

            org.assertj.core.api.Assertions.assertThatThrownBy(
                    () -> new ScriptCacheOps().getInt("k"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("redis down");
        }
    }
```

**Step 2.2: 运行测试验证 RED**

```bash
cd server && mvn test -pl zwei-iot-timeseries -Dtest=ScriptCacheOpsTest -q
```
Expected: FAIL (新增方法不存在 / 透传断言失败)

**Step 2.3: 补全 ScriptCacheOps 实现**

把 `ScriptCacheOps.java` 替换为以下完整内容：

```java
package com.zwei.iot.timeseries.compute;

import com.zwei.common.utils.CacheUtil;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

/**
 * Groovy 脚本可调用的 Redis 缓存实例外壳 — 委托 {@link CacheUtil} 静态方法。
 *
 * <p>沙箱禁 {@code Class} receiver, 无法直接绑定 {@code CacheUtil.class} 让脚本调静态方法。
 * 本类作为 {@code @Component} 实例注入到脚本 Binding 的 {@code cache} 变量。
 *
 * <p>异常策略: <b>透传</b> — cache 失败应让脚本感知 (与 ScriptSensorQuery 的"吞噬"相反)。
 */
@Component
public class ScriptCacheOps {

    // ==================== getters (7 类 × 2 重载) ====================

    public Integer getInt(String key) { return CacheUtil.getInt(key); }
    public int getInt(String key, int defaultValue) { return CacheUtil.getInt(key, defaultValue); }

    public Long getLong(String key) { return CacheUtil.getLong(key); }
    public long getLong(String key, long defaultValue) { return CacheUtil.getLong(key, defaultValue); }

    public Double getDouble(String key) { return CacheUtil.getDouble(key); }
    public double getDouble(String key, double defaultValue) { return CacheUtil.getDouble(key, defaultValue); }

    public Float getFloat(String key) { return CacheUtil.getFloat(key); }
    public float getFloat(String key, float defaultValue) { return CacheUtil.getFloat(key, defaultValue); }

    public BigDecimal getBigDecimal(String key) { return CacheUtil.getBigDecimal(key); }
    public BigDecimal getBigDecimal(String key, BigDecimal defaultValue) { return CacheUtil.getBigDecimal(key, defaultValue); }

    public String getString(String key) { return CacheUtil.getString(key); }
    public String getString(String key, String defaultValue) { return CacheUtil.getString(key, defaultValue); }

    public Boolean getBoolean(String key) { return CacheUtil.getBoolean(key); }
    public boolean getBoolean(String key, boolean defaultValue) { return CacheUtil.getBoolean(key, defaultValue); }

    // ==================== 通用操作 ====================

    public void set(String key, Object value) { CacheUtil.set(key, value); }
    public void set(String key, Object value, long timeout, TimeUnit unit) { CacheUtil.set(key, value, timeout, unit); }

    public boolean delete(String key) { return CacheUtil.delete(key); }
    public boolean hasKey(String key) { return CacheUtil.hasKey(key); }
    public boolean expire(String key, long timeout) { return CacheUtil.expire(key, timeout); }
    public boolean expire(String key, long timeout, TimeUnit unit) { return CacheUtil.expire(key, timeout, unit); }
    public long getExpire(String key) { return CacheUtil.getExpire(key); }
}
```

**Step 2.4: 运行测试验证 GREEN**

```bash
cd server && mvn test -pl zwei-iot-timeseries -Dtest=ScriptCacheOpsTest -q
```
Expected: 6 tests PASS (2 from Task 1 + 4 new)

**Step 2.5: Commit**

```bash
git add server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/compute/ScriptCacheOps.java \
        server/zwei-iot-timeseries/src/test/java/com/zwei/iot/timeseries/compute/ScriptCacheOpsTest.java
git commit -m "feat(compute): ScriptCacheOps 补全 21 方法委托 (TDD)"
```

---

## Task 3: ScriptSensorQuery — 委托 + 异常吞噬

**Files:**
- Create: `server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/compute/ScriptSensorQuery.java`
- Create: `server/zwei-iot-timeseries/src/test/java/com/zwei/iot/timeseries/compute/ScriptSensorQueryTest.java`

**Step 3.1: 写失败测试**

`ScriptSensorQueryTest.java`:

```java
package com.zwei.iot.timeseries.compute;

import com.zwei.iot.timeseries.domain.SensorSnapshot;
import com.zwei.iot.timeseries.util.SensorDataQueryUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

@DisplayName("ScriptSensorQuery (SensorDataQueryUtil 实例外壳)")
class ScriptSensorQueryTest {

    @Test
    @DisplayName("query 委托 SensorDataQueryUtil.query")
    void queryDelegates() {
        SensorSnapshot snap = new SensorSnapshot(1700000000000L, Map.of("rain", 25.5));
        try (MockedStatic<SensorDataQueryUtil> mocked = mockStatic(SensorDataQueryUtil.class)) {
            mocked.when(() -> SensorDataQueryUtil.query(1L, "WY_1", 1700000000000L, "rain"))
                  .thenReturn(snap);

            SensorSnapshot out = new ScriptSensorQuery().query(1L, "WY_1", 1700000000000L, "rain");

            assertThat(out).isSameAs(snap);
            mocked.verify(() -> SensorDataQueryUtil.query(1L, "WY_1", 1700000000000L, "rain"));
        }
    }

    @Test
    @DisplayName("query 返回 null (无数据): 透传 null, 不抛")
    void queryNullPropagated() {
        try (MockedStatic<SensorDataQueryUtil> mocked = mockStatic(SensorDataQueryUtil.class)) {
            mocked.when(() -> SensorDataQueryUtil.query(1L, "WY_1", 0L, "rain"))
                  .thenReturn(null);

            assertThat(new ScriptSensorQuery().query(1L, "WY_1", 0L, "rain")).isNull();
        }
    }

    @Test
    @DisplayName("query 抛异常: wrapper 吞噬返回 null (主链路保护)")
    void queryExceptionSwallowed() {
        try (MockedStatic<SensorDataQueryUtil> mocked = mockStatic(SensorDataQueryUtil.class)) {
            mocked.when(() -> SensorDataQueryUtil.query(1L, "WY_1", 0L, "rain"))
                  .thenThrow(new RuntimeException("IoTDB down"));

            assertThat(new ScriptSensorQuery().query(1L, "WY_1", 0L, "rain")).isNull();
        }
    }
}
```

**Step 3.2: 运行测试验证 RED**

```bash
cd server && mvn test -pl zwei-iot-timeseries -Dtest=ScriptSensorQueryTest -q
```
Expected: FAIL (ScriptSensorQuery 不存在 → 编译错误)

**Step 3.3: 写实现**

`ScriptSensorQuery.java`:

```java
package com.zwei.iot.timeseries.compute;

import com.zwei.iot.timeseries.domain.SensorSnapshot;
import com.zwei.iot.timeseries.util.SensorDataQueryUtil;
import org.springframework.stereotype.Component;

/**
 * Groovy 脚本可调用的传感器数据查询实例外壳 — 委托 {@link SensorDataQueryUtil}。
 *
 * <p>异常策略: <b>吞噬</b> — 任何 RuntimeException 返回 null。
 * 计算属性求值在主链路 (MonitorIngestFacade.ingest) 上, 不能因 sensor 查询失败让整条消息失败。
 * 与 {@link ScriptCacheOps} 的"透传"策略相反。
 */
@Component
public class ScriptSensorQuery {

    public SensorSnapshot query(long deviceId, String sensorCode, long time, String attrCode) {
        try {
            return SensorDataQueryUtil.query(deviceId, sensorCode, time, attrCode);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
```

**Step 3.4: 运行测试验证 GREEN**

```bash
cd server && mvn test -pl zwei-iot-timeseries -Dtest=ScriptSensorQueryTest -q
```
Expected: 3 tests PASS

**Step 3.5: Commit**

```bash
git add server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/compute/ScriptSensorQuery.java \
        server/zwei-iot-timeseries/src/test/java/com/zwei/iot/timeseries/compute/ScriptSensorQueryTest.java
git commit -m "feat(compute): ScriptSensorQuery wrapper 委托 + 异常吞噬 (TDD)"
```

---

## Task 4: GroovyScriptEngine 新增 4 参 executeComputed 重载

**Files:**
- Modify: `server/zwei-iot-parser/src/main/java/com/zwei/iot/parser/engine/GroovyScriptEngine.java:128`
- Modify: `server/zwei-iot-parser/src/test/java/com/zwei/iot/parser/engine/GroovyScriptEngineComputedTest.java`

**Step 4.1: 写失败测试 — 4 参重载 + extraBindings 进入 Binding**

在 `GroovyScriptEngineComputedTest.java` 末尾追加（保留现有 4 个测试）：

```java
    @Test
    @DisplayName("4 参重载: extraBindings 进入 Binding, 脚本可调实例方法")
    void extraBindingsInjected() {
        String script = """
            def compute(curData, prevData) {
                def out = new LinkedHashMap<String, Object>()
                out.put('doubled', counter.double(21))
                return out
            }
        """;

        // counter 是一个普通 Java 对象, 验证实例方法被脚本调用
        Map<String, Object> bindings = new LinkedHashMap<>();
        bindings.put("counter", new Object() {
            @SuppressWarnings("unused")
            public int double(int x) { return x * 2; }
        });

        Map<String, Object> out = engine.executeComputed(script, Map.of(), null, bindings);

        assertThat(out).hasSize(1);
        assertThat(out.get("doubled")).isEqualTo(42);
    }

    @Test
    @DisplayName("3 参重载仍工作: 委托到 4 参 + 空 Map (回归)")
    void threeArgStillWorks() {
        String script = """
            def compute(curData, prevData) {
                def out = new LinkedHashMap<String, Object>()
                out.put('ok', true)
                return out
            }
        """;
        Map<String, Object> out = engine.executeComputed(script, Map.of(), Map.of());
        assertThat(out.get("ok")).isEqualTo(Boolean.TRUE);
    }
```

需要在 import 块加：

```java
import java.util.LinkedHashMap;
```

**Step 4.2: 运行测试验证 RED**

```bash
cd server && mvn test -pl zwei-iot-parser -Dtest=GroovyScriptEngineComputedTest -q
```
Expected: FAIL (4 参 executeComputed 不存在 → 编译错误)

**Step 4.3: 改造 GroovyScriptEngine.executeComputed**

替换 `GroovyScriptEngine.java` 第 113-156 行（含原 3 参 `executeComputed`）为：

```java
    /**
     * 执行合并后的计算属性脚本 (3 参向后兼容版本)。
     *
     * <p>委托到 4 参重载, extraBindings 传空 Map。
     *
     * @see #executeComputed(String, Map, Map, Map)
     */
    public Map<String, Object> executeComputed(String scriptCode,
                                                Map<String, Object> curData,
                                                Map<String, Object> prevData) {
        return executeComputed(scriptCode, curData, prevData, Map.of());
    }

    /**
     * 执行合并后的计算属性脚本, 支持通过 extraBindings 注入额外变量到 Groovy Binding。
     *
     * <p>与 {@link #execute} 共享沙箱配置 ({@link #createSecureConfig()}) 和 executor,
     * 但调用约定不同: 脚本必须定义 {@code compute(curData, prevData)} 主入口,
     * 返回 {@code Map<String, Object>}(attrCode -> value)。
     *
     * <p>典型用法: 调用方传入 {@code Map.of("cache", cacheOps, "sensor", sensorQuery)},
     * 脚本里以 {@code cache.getInt('k')} / {@code sensor.query(...)} 形式访问。
     *
     * <p>失败永远返回空 Map, 不抛异常 (主链路数据接入可用性优先)。
     *
     * @param scriptCode    ComputedScriptAssembler.assemble() 产物
     * @param curData       当前精简消息 Map
     * @param prevData      上一条精简消息 Map, 首次上报时为 null
     * @param extraBindings 额外 Binding 变量 (可为 null / 空)
     * @return 计算结果 Map; 失败时为空 Map
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> executeComputed(String scriptCode,
                                                Map<String, Object> curData,
                                                Map<String, Object> prevData,
                                                Map<String, Object> extraBindings) {
        Future<Map<String, Object>> future = executor.submit(() -> {
            try {
                GroovyShell shell = new GroovyShell(createSecureConfig());
                Binding binding = new Binding();
                binding.setVariable("builtin", builtInFunctions);
                if (extraBindings != null) {
                    extraBindings.forEach(binding::setVariable);
                }
                Script script = shell.parse(scriptCode);
                script.setBinding(binding);
                Object result = script.invokeMethod(
                        "compute", new Object[]{curData, prevData});
                return result instanceof Map ? (Map<String, Object>) result : Map.of();
            } catch (Exception e) {
                log.warn("Computed script execution failed", e);
                return Map.of();
            }
        });
        try {
            return future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.warn("Computed script timed out ({}s)", TIMEOUT_SECONDS);
            future.cancel(true);
            return Map.of();
        } catch (Exception e) {
            log.warn("Computed script interrupted", e);
            return Map.of();
        }
    }
```

**Step 4.4: 运行测试验证 GREEN**

```bash
cd server && mvn test -pl zwei-iot-parser -Dtest=GroovyScriptEngineComputedTest -q
```
Expected: 6 tests PASS (原 4 个 + 新 2 个)

**Step 4.5: Commit**

```bash
git add server/zwei-iot-parser/src/main/java/com/zwei/iot/parser/engine/GroovyScriptEngine.java \
        server/zwei-iot-parser/src/test/java/com/zwei/iot/parser/engine/GroovyScriptEngineComputedTest.java
git commit -m "feat(parser): GroovyScriptEngine 新增 4 参 executeComputed (extraBindings)"
```

---

## Task 5: ComputedAttributeEvaluator 注入两个 wrapper

**Files:**
- Modify: `server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/compute/ComputedAttributeEvaluator.java`
- Modify: `server/zwei-iot-timeseries/src/test/java/com/zwei/iot/timeseries/compute/ComputedAttributeEvaluatorTest.java`

**Step 5.1: 改测试 — stub 改 4 参签名 + 注入 wrapper**

修改 `ComputedAttributeEvaluatorTest.java`：

a) 在 `setUp()` 增加：

```java
    private ScriptCacheOps cacheOps;
    private ScriptSensorQuery sensorQuery;
```

并改 `evaluator` 构造：

```java
        cacheOps = mock(ScriptCacheOps.class);
        sensorQuery = mock(ScriptSensorQuery.class);

        evaluator = new ComputedAttributeEvaluator(
                sensorQuery, registry, assembler, lastMessageStore, scriptEngine,
                cacheOps, sensorQuery);
```

注: 字段名冲突! 已有 `sensorQuery` (IDeviceSensorQueryService) 与新增 `ScriptSensorQuery sensorQuery`。
**冲突解决**: 把 IDeviceSensorQueryService 字段重命名为 `deviceSensorQuery`:
- 声明 `private IDeviceSensorQueryService deviceSensorQuery;`
- `setUp()`: `deviceSensorQuery = mock(IDeviceSensorQueryService.class);`
- 构造器参数顺序保持，参数名改 `deviceSensorQuery`
- `stubSensor()` 内的 `sensorQuery.requireSensorMetadata(...)` 改为 `deviceSensorQuery.requireSensorMetadata(...)`
- `sensorQueryThrows()` 测试里同样改

或者：把新字段命名为 `scriptSensorQuery`，避免冲突。**推荐用后者**（影响面小）：

```java
    private ScriptCacheOps cacheOps;
    private ScriptSensorQuery scriptSensorQuery;
```

构造器调用：

```java
        cacheOps = mock(ScriptCacheOps.class);
        scriptSensorQuery = mock(ScriptSensorQuery.class);

        evaluator = new ComputedAttributeEvaluator(
                sensorQuery, registry, assembler, lastMessageStore, scriptEngine,
                cacheOps, scriptSensorQuery);
```

b) 把所有 `when(scriptEngine.executeComputed(anyString(), any(), ...))` 改为 4 参版本。例如：

```java
// 旧:
when(scriptEngine.executeComputed(anyString(), any(), isNull()))
// 新:
when(scriptEngine.executeComputed(anyString(), any(), isNull(), any()))
```

或：

```java
// 旧:
when(scriptEngine.executeComputed(anyString(), any(), any()))
// 新:
when(scriptEngine.executeComputed(anyString(), any(), any(), any()))
```

涉及测试方法: `firstReport` / `allScriptsFail` / `nonNumericResult` / `lastMessageStorePut` / `evaluatorExceptionReturnsEmpty`（如有）。

**Step 5.2: 运行测试验证 RED**

```bash
cd server && mvn test -pl zwei-iot-timeseries -Dtest=ComputedAttributeEvaluatorTest -q
```
Expected: FAIL (evaluator 构造器签名变化 → 编译错误)

**Step 5.3: 改 ComputedAttributeEvaluator 实现**

修改 `ComputedAttributeEvaluator.java`：

a) 新增 import:

```java
import java.util.HashMap;
```

b) 新增字段 + 构造器参数:

```java
    private final ScriptCacheOps cacheOps;
    private final ScriptSensorQuery sensorQuery;

    public ComputedAttributeEvaluator(IDeviceSensorQueryService sensorQuery,
                                       ComputedAttributeRegistry registry,
                                       ComputedScriptAssembler assembler,
                                       LastMessageStore lastMessageStore,
                                       GroovyScriptEngine scriptEngine,
                                       ScriptCacheOps cacheOps,
                                       ScriptSensorQuery scriptSensorQuery) {
        this.sensorQuery = sensorQuery;
        this.registry = registry;
        this.assembler = assembler;
        this.lastMessageStore = lastMessageStore;
        this.scriptEngine = scriptEngine;
        this.cacheOps = cacheOps;
        this.sensorQuery = scriptSensorQuery;
    }
```

**冲突注意**: 已有字段 `sensorQuery` (类型 IDeviceSensorQueryService) 与新增 `ScriptSensorQuery sensorQuery` 类型不同但同名。

**解决**: 把新字段命名为 `scriptSensorQuery`:

```java
    private final ScriptCacheOps cacheOps;
    private final ScriptSensorQuery scriptSensorQuery;

    public ComputedAttributeEvaluator(IDeviceSensorQueryService sensorQuery,
                                       ComputedAttributeRegistry registry,
                                       ComputedScriptAssembler assembler,
                                       LastMessageStore lastMessageStore,
                                       GroovyScriptEngine scriptEngine,
                                       ScriptCacheOps cacheOps,
                                       ScriptSensorQuery scriptSensorQuery) {
        this.sensorQuery = sensorQuery;
        this.registry = registry;
        this.assembler = assembler;
        this.lastMessageStore = lastMessageStore;
        this.scriptEngine = scriptEngine;
        this.cacheOps = cacheOps;
        this.scriptSensorQuery = scriptSensorQuery;
    }
```

c) 改造 `evaluate()` 第 78 行附近（构建 tools Map + 调 4 参）:

```java
            // 6. 执行
            Map<String, Object> tools = new HashMap<>();
            tools.put("cache", cacheOps);
            tools.put("sensor", scriptSensorQuery);
            Map<String, Object> results = scriptEngine.executeComputed(script, curData, prevData, tools);
```

**Step 5.4: 运行测试验证 GREEN**

```bash
cd server && mvn test -pl zwei-iot-timeseries -Dtest=ComputedAttributeEvaluatorTest -q
```
Expected: 6 tests PASS (原有数量保持不变)

**Step 5.5: Commit**

```bash
git add server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/compute/ComputedAttributeEvaluator.java \
        server/zwei-iot-timeseries/src/test/java/com/zwei/iot/timeseries/compute/ComputedAttributeEvaluatorTest.java
git commit -m "feat(compute): ComputedAttributeEvaluator 注入 cache/sensor wrapper (TDD)"
```

---

## Task 6: ComputedAttributeTestController 同步改造

**Files:**
- Modify: `server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/compute/controller/ComputedAttributeTestController.java`

**Step 6.1: 改造 controller**

无对应单测 (此 controller 仅做在线测试，集成验证在 Task 7)。

a) 新增 import:

```java
import com.zwei.iot.timeseries.compute.ScriptCacheOps;
import com.zwei.iot.timeseries.compute.ScriptSensorQuery;
import java.util.HashMap;
```

b) 新增字段 + 构造器参数:

```java
    private final ScriptCacheOps cacheOps;
    private final ScriptSensorQuery scriptSensorQuery;

    @Autowired
    public ComputedAttributeTestController(ComputedAttributeRegistry registry,
                                           ComputedScriptAssembler assembler,
                                           GroovyScriptEngine scriptEngine,
                                           ScriptCacheOps cacheOps,
                                           ScriptSensorQuery scriptSensorQuery) {
        this.registry = registry;
        this.assembler = assembler;
        this.scriptEngine = scriptEngine;
        this.cacheOps = cacheOps;
        this.scriptSensorQuery = scriptSensorQuery;
    }
```

c) 改 `testScript()` 第 79 行:

```java
        Map<String, Object> tools = new HashMap<>();
        tools.put("cache", cacheOps);
        tools.put("sensor", scriptSensorQuery);
        Map<String, Object> result = scriptEngine.executeComputed(script, curData, prevData, tools);
```

**Step 6.2: 编译验证**

```bash
cd server && mvn compile -pl zwei-iot-timeseries -am -q
```
Expected: BUILD SUCCESS

**Step 6.3: Commit**

```bash
git add server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/compute/controller/ComputedAttributeTestController.java
git commit -m "feat(compute): ComputedAttributeTestController 同步注入 cache/sensor wrapper"
```

---

## Task 7: 集成测试 ComputedAttributeIngestTest stub 调整

**Files:**
- Modify: `server/zwei-iot-timeseries/src/test/java/com/zwei/iot/timeseries/integration/ComputedAttributeIngestTest.java`

**Step 7.1: 调整 stub**

读文件，找到所有 `when(scriptEngine.executeComputed(...))` 与 `new ComputedAttributeEvaluator(...)` 构造点，按 Task 5 的模式调整：
- stub 改 4 参（多加一个 `any()` matcher）
- 构造器调用加 `cacheOps, scriptSensorQuery` 两个 mock 参数

**Step 7.2: 运行验证**

```bash
cd server && mvn test -pl zwei-iot-timeseries -Dtest=ComputedAttributeIngestTest -q
```
Expected: PASS (所有原有用例通过)

**Step 7.3: Commit** (如有改动)

```bash
git add server/zwei-iot-timeseries/src/test/java/com/zwei/iot/timeseries/integration/ComputedAttributeIngestTest.java
git commit -m "test(compute): 调整 ComputedAttributeIngestTest stub 适配 4 参 executeComputed"
```

---

## Task 8: 全量回归

**Step 8.1: 三个相关模块全测**

```bash
cd server && mvn test -pl zwei-iot-parser,zwei-iot-timeseries,zwei-common -q
```
Expected: BUILD SUCCESS + 0 failures

**Step 8.2: 检查所有提交**

```bash
git log --oneline -10
```
Expected: 看到 6-7 个新 commit (Task 1/2/3/4/5/6/(7))

---

## 执行完成检查清单

- [ ] `ScriptCacheOps` 21 方法委托测过 (Task 1+2)
- [ ] `ScriptSensorQuery` 异常吞噬测过 (Task 3)
- [ ] `executeComputed` 4 参重载 + 3 参向后兼容 (Task 4)
- [ ] `ComputedAttributeEvaluator` 注入两个 wrapper (Task 5)
- [ ] `ComputedAttributeTestController` 同步改造 (Task 6)
- [ ] 集成测试 `ComputedAttributeIngestTest` 通过 (Task 7)
- [ ] 三个模块 `mvn test` 全绿 (Task 8)

## 风险与缓解

| 风险 | 缓解 |
|---|---|
| `mockStatic` 需要 mockito-inline 依赖 | 先用 mockito-core 默认配置试；若报错，在 `zwei-iot-timeseries/pom.xml` mockito 依赖加 `<classifier>inline</classifier>` |
| Groovy 沙箱可能拒绝 wrapper 实例方法调用 | Task 4 测试 (`extraBindingsInjected`) 已覆盖该路径；若失败，检查 `SecureASTCustomizer.disallowedReceivers` 是否含 wrapper 全限定名（应无） |
| 字段命名冲突 (`sensorQuery`) | Task 5 已明确用 `scriptSensorQuery` 避开 |
| 现有 stub 改 4 参时漏改 | Task 8 全量回归捕获 |
