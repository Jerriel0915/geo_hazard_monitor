# 计算属性脚本工具注入 设计

> 日期: 2026-06-25
> 模块: `zwei-iot-timeseries` + `zwei-iot-parser`
> 状态: 已批准

## 1. 目标

让计算属性 Groovy 脚本通过 `cache.*` 与 `sensor.*` 调用 `CacheUtil` (Redis 二次封装) 与
`SensorDataQueryUtil` (IoTDB 传感器快照查询) 两个静态工具类，用于跨属性累积、跨传感器引用等场景。

不破坏模块边界 (`zwei-iot-parser` 不感知 `zwei-iot-timeseries`)、不削弱 Groovy 沙箱安全性。

## 2. 背景与约束

### 2.1 现状

- 计算属性求值入口: `ComputedAttributeEvaluator.evaluate()` (`zwei-iot-timeseries`)
- 脚本执行: `GroovyScriptEngine.executeComputed(script, curData, prevData)` (`zwei-iot-parser`)
- 沙箱 (`SecureASTCustomizer`) 已禁 `Class`/`System`/`Runtime` 等 receiver，禁止所有静态导入
- 脚本 Binding 当前仅注入 `builtin` (一个 `BuiltInFunctions` 实例)

### 2.2 关键约束

| 约束 | 含义 |
|---|---|
| 沙箱禁 `Class` receiver | 无法直接绑定 `CacheUtil.class` 让脚本调静态方法 — 必须用实例 |
| parser 不依赖 timeseries | `SensorDataQueryUtil` 不可见，wrapper 必须在 timeseries 侧实例化后传入 |
| `executeComputed` 现有 3 个调用点 | 改签名需保持向后兼容 (3 参重载保留) |
| 计算属性求值失败绝不影响主链路 | wrapper 内部异常必须捕获，返回 null / 空结果 |

### 2.3 既有先例

`BuiltInFunctions` 已通过 `binding.setVariable("builtin", builtInFunctions)` 注入到 Groovy 脚本，
脚本以 `builtin.readFloat(...)` 形式调用实例方法。本设计沿用同一通道。

## 3. 设计

### 3.1 两个 wrapper (`zwei-iot-timeseries/compute/`)

**`ScriptCacheOps`** — `@Component`、无状态、委托 `CacheUtil` 静态方法:

```java
@Component
public class ScriptCacheOps {
    public Integer getInt(String key)                          { return CacheUtil.getInt(key); }
    public int     getInt(String key, int defaultValue)        { return CacheUtil.getInt(key, defaultValue); }
    public Long    getLong(String key)                         { return CacheUtil.getLong(key); }
    public long    getLong(String key, long defaultValue)      { return CacheUtil.getLong(key, defaultValue); }
    public Double  getDouble(String key)                       { return CacheUtil.getDouble(key); }
    public double  getDouble(String key, double def)           { return CacheUtil.getDouble(key, def); }
    public Float   getFloat(String key)                        { return CacheUtil.getFloat(key); }
    public float   getFloat(String key, float def)             { return CacheUtil.getFloat(key, def); }
    public BigDecimal getBigDecimal(String key)                { return CacheUtil.getBigDecimal(key); }
    public BigDecimal getBigDecimal(String key, BigDecimal def){ return CacheUtil.getBigDecimal(key, def); }
    public String  getString(String key)                       { return CacheUtil.getString(key); }
    public String  getString(String key, String def)           { return CacheUtil.getString(key, def); }
    public Boolean getBoolean(String key)                      { return CacheUtil.getBoolean(key); }
    public boolean getBoolean(String key, boolean def)         { return CacheUtil.getBoolean(key, def); }

    public void set(String key, Object value)                                   { CacheUtil.set(key, value); }
    public void set(String key, Object value, long timeout, TimeUnit unit)      { CacheUtil.set(key, value, timeout, unit); }
    public boolean delete(String key)                          { return CacheUtil.delete(key); }
    public boolean hasKey(String key)                          { return CacheUtil.hasKey(key); }
    public boolean expire(String key, long timeout)            { return CacheUtil.expire(key, timeout); }
    public boolean expire(String key, long timeout, TimeUnit u){ return CacheUtil.expire(key, timeout, u); }
    public long    getExpire(String key)                       { return CacheUtil.getExpire(key); }
}
```

> 14 个 getter (7 类 × 2 重载) + 7 个通用操作 = **21 个公共方法**，全部一对一委托。
> 不做参数转换、不做语义改造 — 仅"实例外壳"。

**`ScriptSensorQuery`** — `@Component`、委托 `SensorDataQueryUtil`:

```java
@Component
public class ScriptSensorQuery {
    public SensorSnapshot query(long deviceId, String sensorCode,
                                 long time, String attrCode) {
        try {
            return SensorDataQueryUtil.query(deviceId, sensorCode, time, attrCode);
        } catch (Exception e) {
            return null;   // 主链路保护: 求值期不抛
        }
    }
}
```

> 调用 `SensorDataQueryUtil.query` 时 `deviceId` 为 `long` (基本类型) —
> 适配既有签名 `Long deviceId` (包装) 的 auto-box 不会失败，但脚本侧传入的 `curData.deviceId` 多为 `Long`，
> Groovy 会自动拆箱为 `long`。工具内部仍按既有逻辑校验 `null`。
>
> **异常吞噬**: 工具内部已对 SQL 失败返回 null；wrapper 再包一层 catch 兜底所有 RuntimeException，
> 保证脚本里的 sensor 调用永远不会让 compute() 主流程失败。

### 3.2 `GroovyScriptEngine` 新增重载 (`zwei-iot-parser`)

```java
/** 新增: 带 extraBindings 的重载 */
public Map<String, Object> executeComputed(String scriptCode,
                                            Map<String, Object> curData,
                                            Map<String, Object> prevData,
                                            Map<String, Object> extraBindings) {
    // 与现有 3 参版本逻辑一致，仅 Binding 阶段追加:
    //   if (extraBindings != null) extraBindings.forEach(binding::setVariable)
}

/** 旧 3 参版本改为委托，保持向后兼容 */
public Map<String, Object> executeComputed(String scriptCode,
                                            Map<String, Object> curData,
                                            Map<String, Object> prevData) {
    return executeComputed(scriptCode, curData, prevData, Map.of());
}
```

> parser 仅看到 `Map<String, Object>`，**不引入新依赖**。
> 现有 `GroovyScriptEngineComputedTest` 继续走 3 参版本，无需改动。

### 3.3 调用方修改

#### 3.3.1 `ComputedAttributeEvaluator`

```java
// 构造器注入新增:
private final ScriptCacheOps cacheOps;
private final ScriptSensorQuery sensorQuery;

// evaluate() 第 78 行原:
//   Map<String,Object> results = scriptEngine.executeComputed(script, curData, prevData);
// 改为:
Map<String, Object> tools = Map.of(
    "cache",  cacheOps,
    "sensor", sensorQuery
);
Map<String,Object> results = scriptEngine.executeComputed(script, curData, prevData, tools);
```

#### 3.3.2 `ComputedAttributeTestController`

同步改造: 注入两个 wrapper，在调用 `executeComputed` 时构建 tools Map 透传，
让在线测试与生产环境行为一致。

### 3.4 沙箱安全性

- 不修改 `SecureASTCustomizer` — wrapper 是普通 POJO，走 `builtin` 同款实例方法通道，已被允许
- 脚本无法获取 `Class` 对象 (沙箱已禁 receiver)，因此**无法绕过 wrapper 直接调静态方法**
- `CacheUtil` / `SensorDataQueryUtil` 内部仍走 `SpringUtils.getBean()` — wrapper 不承担依赖查找

### 3.5 性能

- wrapper 为 `@Component` 单例，无创建开销
- `Map.of("cache", ..., "sensor", ...)` 每次求值构建一次 (100ns 级)，与 `curData` 构建同级，可忽略
- 不做单次 compute() 内的查询结果缓存 (YAGNI) — 若脚本多次查同一传感器，由调用方在脚本里 `def x = sensor.query(...)` 自行 memo

## 4. 测试计划

### 4.1 新增测试

**`ScriptCacheOpsTest`** (zwei-iot-timeseries/test):
- 21 个委托测试，分 3 组:
  - getter 委托 (mock `CacheUtil.getInt(...)` 等静态方法 — 用 `Mockito.mockStatic`)
  - 通用操作委托 (`set`/`delete`/`expire`/...)
  - 默认值重载
- 1 个异常透传测试: `CacheUtil.getInt` 抛 RuntimeException 时 wrapper 不吞 (与 `ScriptSensorQuery` 相反 — cache 失败应让脚本感知)

**`ScriptSensorQueryTest`** (zwei-iot-timeseries/test):
- 委托测试 (mock `SensorDataQueryUtil.query`)
- **异常吞噬测试** (关键): 工具抛 RuntimeException → wrapper 返回 null，不传播

**`GroovyScriptEngineComputedTest` 新增用例** (zwei-iot-parser/test):
- 调用 4 参重载, extraBindings 里塞一个 stub `Map.of("counter", new TestCounter())`
- 脚本: `def compute(c, p) { return [doubled: counter.double(21)] }`
- 验证返回 `{doubled: 42}` — 证明 extraBindings 进入 Binding 且实例方法可调
- 验证 3 参重载仍工作 (回归)

### 4.2 受影响测试

- `GroovyScriptEngineComputedTest` 现有 4 个用例 — 走 3 参重载委托路径，预期**无需改动**
- `ComputedAttributeEvaluatorTest` — mock `scriptEngine.executeComputed(...)` 的 stub 签名变化:
  改为 `when(scriptEngine.executeComputed(anyString(), any(), any(), any()))` (4 个 matcher)
- `ComputedAttributeIngestTest` (集成测试) — 同上调整 stub

## 5. 使用示例

### 5.1 跨属性累积

```groovy
def calc_rain_hourly(curData, prevData) {
    def cur = curData.props.rainfall ?: 0
    def acc = cache.getInt('rain:hourly:acc', 0) + cur
    cache.set('rain:hourly:acc', acc, 1, java.util.concurrent.TimeUnit.HOURS)
    return acc
}
```

### 5.2 跨传感器引用

```groovy
def calc_temp_corrected(curData, prevData) {
    def raw = curData.props.temperature ?: 0
    def ref = sensor.query(curData.deviceId, 'TEMP_REF_1', curData.dataTime, 'temperature')
    if (ref == null) return raw
    return raw - ref.values.temperature   // 校准补偿
}
```

### 5.3 时序对比

```groovy
def calc_velocity(curData, prevData) {
    if (prevData == null) return 0
    def cur  = curData.props.displacement ?: 0
    def prev = prevData.props.displacement ?: 0
    def dt   = curData.dataTime - prevData.dataTime
    if (dt == 0) return 0
    cache.set('vel:last', (cur - prev) / dt)
    return (cur - prev) / dt
}
```

## 6. 范围之外 (YAGNI)

- 不在 wrapper 内做 TTL 单位字符串解析 (`'MINUTES'` → `TimeUnit.MINUTES`) — 脚本直接传 `TimeUnit.MINUTES`
  常量；若后续需要，再单独讨论 DSL 简化
- 不引入单次 compute() 内的查询缓存层
- 不扩展到 `execute()` (parse 脚本) — 仅 `executeComputed` (compute 脚本) 需要这两个工具
- 不暴露 IoTDB 写入接口 — 仅只读查询

## 7. 实施步骤 (粗粒度)

1. (前置已完成) `CacheUtil` 已实现并测过 (31 单测)
2. (前置已完成) `SensorDataQueryUtil` 已实现并测过 (7 单测)
3. 新建 `ScriptCacheOps` + `ScriptCacheOpsTest` (TDD)
4. 新建 `ScriptSensorQuery` + `ScriptSensorQueryTest` (TDD)
5. `GroovyScriptEngine` 新增 4 参 `executeComputed` 重载 + 旧 3 参委托
6. `GroovyScriptEngineComputedTest` 新增 4 参重载用例
7. `ComputedAttributeEvaluator` 注入两个 wrapper + 改调用
8. `ComputedAttributeEvaluatorTest` 调整 stub
9. `ComputedAttributeTestController` 同步改造
10. `ComputedAttributeIngestTest` 集成测试回归
11. 全模块 `mvn test` 通过

## 8. 变更清单

| 文件 | 类型 | 动作 |
|---|---|---|
| `zwei-iot-timeseries/.../compute/ScriptCacheOps.java` | main | 新增 |
| `zwei-iot-timeseries/.../compute/ScriptSensorQuery.java` | main | 新增 |
| `zwei-iot-timeseries/.../compute/ComputedAttributeEvaluator.java` | main | 改造: 注入 + 调用 |
| `zwei-iot-timeseries/.../compute/controller/ComputedAttributeTestController.java` | main | 改造: 同步 |
| `zwei-iot-parser/.../engine/GroovyScriptEngine.java` | main | 新增 4 参重载 + 旧重载委托 |
| `zwei-iot-timeseries/.../compute/ScriptCacheOpsTest.java` | test | 新增 |
| `zwei-iot-timeseries/.../compute/ScriptSensorQueryTest.java` | test | 新增 |
| `zwei-iot-parser/.../engine/GroovyScriptEngineComputedTest.java` | test | 新增 4 参重载用例 |
| `zwei-iot-timeseries/.../compute/ComputedAttributeEvaluatorTest.java` | test | 调整 stub 签名 |
| `zwei-iot-timeseries/.../integration/ComputedAttributeIngestTest.java` | test | 调整 stub 签名 |
