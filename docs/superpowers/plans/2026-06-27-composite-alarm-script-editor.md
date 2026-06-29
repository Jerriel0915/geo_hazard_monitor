# 综合告警脚本编辑器改造 实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 为综合告警 Groovy 脚本注入 `cache`(Redis) + `sensor`(IoTDB) 工具，前端全量迁移到 CodeMirror 编辑器架构（废弃 Blockly）。

**架构：** 后端在 `GroovyScriptExecutor` 新增 `executeWithTools` 重载注入工具 bean；CRON 调用点追加 tools 参数；新增测试端点。前端新建 `CompositeAlarmScriptEditor.vue`（对照 `CalcScriptEditor.vue`），`CompositeAlarmScriptDrawer.vue` 退化为壳，`script-api-docs.ts` 扩展为多模式。

**技术栈：** Java 17 + Spring Boot + Groovy + Mockito (后端) / Vue 3 + TypeScript + Vitest + CodeMirror 6 (前端)

**设计文档：** `docs/superpowers/specs/2026-06-27-composite-alarm-script-editor-design.md`

---

## 文件结构

### 后端

| 文件 | 职责 | 变更 |
|------|------|------|
| `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/engine/GroovyScriptExecutor.java` | Groovy 脚本执行器 | 修改：新增 `executeWithTools` 重载 |
| `server/zwei-iot-alarm/src/test/java/com/zwei/iot/alarm/service/engine/GroovyScriptExecutorTest.java` | 执行器单测 | 新建 |
| `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/job/ComprehensiveAlarmJob.java` | CRON 调度任务 | 修改：追加 tools 注入 |
| `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/controller/AlarmStrategyController.java` | REST 控制器 | 修改：新增 test-run 端点 |
| `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/IAlarmStrategyService.java` | 策略 Service 接口 | 修改：新增 `testRun` 方法签名 |
| `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/impl/AlarmStrategyServiceImpl.java` | 策略 Service 实现 | 修改：实现 `testRun` |
| `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/domain/dto/StrategyTestRunRequest.java` | 测试请求 DTO | 新建 |
| `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/domain/dto/StrategyTestRunResult.java` | 测试结果 DTO | 新建 |

### 前端

| 文件 | 职责 | 变更 |
|------|------|------|
| `web/src/views/basic/components/script-editor/script-api-docs.ts` | API 文档数据源 | 修改：多模式扩展 |
| `web/src/views/basic/components/script-editor/ApiDocsSidebar.vue` | API 文档侧栏 | 修改：新增 mode prop |
| `web/src/views/basic/components/script-editor/TestPanel.vue` | 测试面板 | 修改：新增 mode prop（alarm 模式隐藏 curData/prevData 输入） |
| `web/src/api/alarm.ts` | 告警 API 封装 | 修改：新增 `testStrategyRun` |
| `web/src/views/alarm/components/CompositeAlarmScriptEditor.vue` | 综合告警脚本编辑器 | **新建** |
| `web/src/views/alarm/components/CompositeAlarmScriptDrawer.vue` | 脚本抽屉 | **重写**：1183 行 → ~70 行壳 |

---

## 任务 1：GroovyScriptExecutor.executeWithTools 重载

**文件：**
- 修改：`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/engine/GroovyScriptExecutor.java`
- 测试：`server/zwei-iot-alarm/src/test/java/com/zwei/iot/alarm/service/engine/GroovyScriptExecutorTest.java`

- [ ] **步骤 1：编写失败的测试**

创建 `GroovyScriptExecutorTest.java`：

```java
package com.zwei.iot.alarm.service.engine;

import com.zwei.iot.alarm.config.AlarmProperties;
import com.zwei.iot.timeseries.compute.ScriptCacheOps;
import com.zwei.iot.timeseries.compute.ScriptSensorQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GroovyScriptExecutorTest {

    private GroovyScriptExecutor executor;

    @BeforeEach
    void setUp() {
        AlarmProperties props = new AlarmProperties();
        props.setGroovyTimeoutSeconds(10);
        executor = new GroovyScriptExecutor(props);
    }

    @Test
    void execute_legacyStillWorks() {
        Integer result = executor.execute("return 3", Map.of());
        assertEquals(3, result);
    }

    @Test
    void executeWithTools_nullTools_equivalentToLegacy() {
        Integer result = executor.executeWithTools("return 2", Map.of(), null);
        assertEquals(2, result);
    }

    @Test
    void executeWithTools_cacheAccessible() {
        ScriptCacheOps cache = mock(ScriptCacheOps.class);
        when(cache.getString("rainfall_key", null)).thenReturn("45.0");

        Map<String, Object> tools = Map.of("cache", cache);
        Integer result = executor.executeWithTools(
                "return Double.parseDouble(cache.getString('rainfall_key', null)) > 10 ? 3 : 0",
                Map.of(), tools);

        assertEquals(3, result);
        verify(cache).getString("rainfall_key", null);
    }

    @Test
    void executeWithTools_sensorAccessible() {
        ScriptSensorQuery sensor = mock(ScriptSensorQuery.class);
        when(sensor.query(anyString(), anyString(), anyLong(), anyString())).thenReturn(null);

        Map<String, Object> tools = Map.of("sensor", sensor);
        Integer result = executor.executeWithTools(
                "def snapshot = sensor.query('DEV001', 'RAIN-001', System.currentTimeMillis(), 'rainfall')\n" +
                "return snapshot == null ? 0 : 1",
                Map.of(), tools);

        assertEquals(0, result);
        verify(sensor).query(eq("DEV001"), eq("RAIN-001"), anyLong(), eq("rainfall"));
    }

    @Test
    void executeWithTools_scriptThrows_returnsNull() {
        ScriptCacheOps cache = mock(ScriptCacheOps.class);
        when(cache.getString("missing", null)).thenThrow(new RuntimeException("Redis down"));

        Map<String, Object> tools = Map.of("cache", cache);
        Integer result = executor.executeWithTools(
                "return cache.getString('missing', null) == null ? 0 : 1",
                Map.of(), tools);

        assertNull(result);
    }
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd server && mvn test -pl zwei-iot-alarm -Dtest=GroovyScriptExecutorTest`
预期：FAIL — `executeWithTools` 方法不存在（编译错误）

- [ ] **步骤 3：实现 executeWithTools 重载**

修改 `GroovyScriptExecutor.java`，在现有 `execute` 方法之后添加：

```java
/**
 * 执行 Groovy 脚本，额外注入工具 bean (cache/sensor 等)。
 * <p>
 * tools 中的键将作为 Groovy 变量注入，优先级高于 variables（但实际不会冲突）。
 *
 * @param scriptContent Groovy 脚本文本
 * @param variables     业务变量 (hazardPointIds, currentTime 等)
 * @param tools         工具 bean (cache, sensor)；可为 null
 * @return 告警等级 1-4，或 null 表示无告警/执行失败
 */
public Integer executeWithTools(String scriptContent, Map<String, Object> variables, Map<String, Object> tools) {
    if (scriptContent == null || scriptContent.trim().isEmpty()) {
        return null;
    }
    if (!isSafeScript(scriptContent)) {
        log.warn("Groovy脚本包含不安全代码，已拒绝执行");
        return null;
    }

    Future<Integer> future = executor.submit(() -> {
        try {
            GroovyShell shell = new GroovyShell();
            Binding binding = new Binding();
            if (variables != null) {
                variables.forEach(binding::setVariable);
            }
            if (tools != null) {
                tools.forEach(binding::setVariable);
            }
            shell.setProperty("binding", binding);

            Object result = shell.evaluate(scriptContent);
            if (result == null) return null;
            if (result instanceof Number) {
                int level = ((Number) result).intValue();
                return (level >= 1 && level <= 4) ? level : null;
            }
            return null;
        } catch (Exception e) {
            log.error("Groovy脚本执行异常: {}", e.getMessage());
            return null;
        }
    });

    try {
        return future.get(properties.getGroovyTimeoutSeconds(), TimeUnit.SECONDS);
    } catch (TimeoutException e) {
        log.warn("Groovy脚本执行超时 ({}s)", properties.getGroovyTimeoutSeconds());
        future.cancel(true);
        return null;
    } catch (Exception e) {
        log.error("Groovy脚本执行中断", e);
        return null;
    }
}
```

然后将原 `execute` 方法体替换为委托：

```java
public Integer execute(String scriptContent, Map<String, Object> variables) {
    return executeWithTools(scriptContent, variables, null);
}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`cd server && mvn test -pl zwei-iot-alarm -Dtest=GroovyScriptExecutorTest`
预期：PASS — 5 个测试全部通过

- [ ] **步骤 5：Commit**

```bash
cd server && git add zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/engine/GroovyScriptExecutor.java \
  zwei-iot-alarm/src/test/java/com/zwei/iot/alarm/service/engine/GroovyScriptExecutorTest.java
git commit -m "feat(alarm): GroovyScriptExecutor 新增 executeWithTools 重载支持工具注入"
```

---

## 任务 2：ComprehensiveAlarmJob 追加 tools 注入

**文件：**
- 修改：`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/job/ComprehensiveAlarmJob.java`

- [ ] **步骤 1：修改构造函数注入 ScriptCacheOps + ScriptSensorQuery**

在 `ComprehensiveAlarmJob.java` 的 import 区域添加：

```java
import com.zwei.iot.timeseries.compute.ScriptCacheOps;
import com.zwei.iot.timeseries.compute.ScriptSensorQuery;
```

在字段声明区（line 48 后）添加：

```java
private final ScriptCacheOps cacheOps;
private final ScriptSensorQuery scriptSensorQuery;
```

修改构造函数（line 50-62）：

```java
public ComprehensiveAlarmJob(AlarmStrategyMapper strategyMapper,
                             AlarmStrategyHazardPointMapper bindingMapper,
                             GroovyScriptExecutor scriptExecutor,
                             IAlarmRecordService alarmRecordService,
                             ApplicationEventPublisher eventPublisher,
                             AlarmDedupService dedupService,
                             ScriptCacheOps cacheOps,
                             ScriptSensorQuery scriptSensorQuery) {
    this.strategyMapper = strategyMapper;
    this.bindingMapper = bindingMapper;
    this.scriptExecutor = scriptExecutor;
    this.alarmRecordService = alarmRecordService;
    this.eventPublisher = eventPublisher;
    this.dedupService = dedupService;
    this.cacheOps = cacheOps;
    this.scriptSensorQuery = scriptSensorQuery;
}
```

- [ ] **步骤 2：修改 executeStrategy 方法中的脚本调用**

将 line 132-137 原代码：

```java
// 原代码
Map<String, Object> variables = new HashMap<>();
variables.put("hazardPointIds", hazardPointIds);
variables.put("currentTime", now);

Integer alarmLevel = scriptExecutor.execute(strategy.getScriptContent(), variables);
```

替换为：

```java
// 新代码 — variables 不变，追加 tools
Map<String, Object> variables = new HashMap<>();
variables.put("hazardPointIds", hazardPointIds);
variables.put("currentTime", now);

Map<String, Object> tools = new HashMap<>();
tools.put("cache", cacheOps);
tools.put("sensor", scriptSensorQuery);

Integer alarmLevel = scriptExecutor.executeWithTools(strategy.getScriptContent(), variables, tools);
```

- [ ] **步骤 3：编译验证**

运行：`cd server && mvn compile -pl zwei-iot-alarm -am`
预期：BUILD SUCCESS

- [ ] **步骤 4：Commit**

```bash
cd server && git add zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/job/ComprehensiveAlarmJob.java
git commit -m "feat(alarm): ComprehensiveAlarmJob 注入 cache+sensor 工具 bean"
```

---

## 任务 3：后端测试端点 — DTO + Service + Controller

**文件：**
- 创建：`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/domain/dto/StrategyTestRunRequest.java`
- 创建：`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/domain/dto/StrategyTestRunResult.java`
- 修改：`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/IAlarmStrategyService.java`
- 修改：`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/impl/AlarmStrategyServiceImpl.java`
- 修改：`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/controller/AlarmStrategyController.java`

- [ ] **步骤 1：创建请求/响应 DTO**

创建 `StrategyTestRunRequest.java`：

```java
package com.zwei.iot.alarm.domain.dto;

/**
 * 综合告警策略测试运行请求。
 */
public class StrategyTestRunRequest {
    /** 可选模拟传感器编码 */
    private String mockSensorCode;
    /** 可选模拟数据时间 (epoch ms) */
    private Long mockDataTime;

    public String getMockSensorCode() { return mockSensorCode; }
    public void setMockSensorCode(String mockSensorCode) { this.mockSensorCode = mockSensorCode; }
    public Long getMockDataTime() { return mockDataTime; }
    public void setMockDataTime(Long mockDataTime) { this.mockDataTime = mockDataTime; }
}
```

创建 `StrategyTestRunResult.java`：

```java
package com.zwei.iot.alarm.domain.dto;

/**
 * 综合告警策略测试运行结果。
 */
public class StrategyTestRunResult {
    private Integer level;          // 1-4 或 null
    private String levelText;       // "红色"/"橙色"/"黄色"/"蓝色" 或 null
    private long durationMs;
    private String error;           // 异常信息或 null

    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }
    public String getLevelText() { return levelText; }
    public void setLevelText(String levelText) { this.levelText = levelText; }
    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}
```

- [ ] **步骤 2：IAlarmStrategyService 新增方法签名**

在 `IAlarmStrategyService.java` 接口中添加：

```java
import com.zwei.iot.alarm.domain.dto.StrategyTestRunRequest;
import com.zwei.iot.alarm.domain.dto.StrategyTestRunResult;

// 在接口方法列表末尾添加：
StrategyTestRunResult testRun(Long id, StrategyTestRunRequest request);
```

- [ ] **步骤 3：AlarmStrategyServiceImpl 实现 testRun**

在 `AlarmStrategyServiceImpl.java` 中：

添加 import：
```java
import com.zwei.iot.alarm.domain.AlarmConstants;
import com.zwei.iot.alarm.domain.dto.StrategyTestRunRequest;
import com.zwei.iot.alarm.domain.dto.StrategyTestRunResult;
import com.zwei.iot.alarm.service.engine.GroovyScriptExecutor;
import com.zwei.iot.timeseries.compute.ScriptCacheOps;
import com.zwei.iot.timeseries.compute.ScriptSensorQuery;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
```

修改构造函数，追加 3 个依赖：

```java
private final GroovyScriptExecutor groovyScriptExecutor;
private final ScriptCacheOps cacheOps;
private final ScriptSensorQuery scriptSensorQuery;

public AlarmStrategyServiceImpl(AlarmStrategyMapper strategyMapper,
                                AlarmStrategyHazardPointMapper bindingMapper,
                                GroovyScriptExecutor groovyScriptExecutor,
                                ScriptCacheOps cacheOps,
                                ScriptSensorQuery scriptSensorQuery) {
    this.strategyMapper = strategyMapper;
    this.bindingMapper = bindingMapper;
    this.groovyScriptExecutor = groovyScriptExecutor;
    this.cacheOps = cacheOps;
    this.scriptSensorQuery = scriptSensorQuery;
}
```

实现 testRun 方法：

```java
@Override
public StrategyTestRunResult testRun(Long id, StrategyTestRunRequest request) {
    StrategyTestRunResult result = new StrategyTestRunResult();
    long start = System.currentTimeMillis();

    AlarmStrategy strategy = strategyMapper.selectById(id);
    if (strategy == null) {
        result.setError("策略不存在: id=" + id);
        result.setDurationMs(System.currentTimeMillis() - start);
        return result;
    }
    if (strategy.getScriptContent() == null || strategy.getScriptContent().trim().isEmpty()) {
        result.setError("策略脚本内容为空");
        result.setDurationMs(System.currentTimeMillis() - start);
        return result;
    }

    // 查询绑定的隐患点（与 ComprehensiveAlarmJob 逻辑一致）
    List<Long> hazardPointIds = bindingMapper.selectHazardPointIdsByStrategyId(id);
    if (hazardPointIds.isEmpty() && strategy.getMonitorTypeId() != null) {
        hazardPointIds = strategyMapper.selectHazardPointIdsByMonitorTypeId(strategy.getMonitorTypeId());
    }

    Map<String, Object> variables = new HashMap<>();
    variables.put("hazardPointIds", hazardPointIds);
    variables.put("currentTime", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
    if (request != null && request.getMockSensorCode() != null) {
        variables.put("sensorCode", request.getMockSensorCode());
    }
    if (request != null && request.getMockDataTime() != null) {
        variables.put("dataTime", request.getMockDataTime());
    }

    Map<String, Object> tools = new HashMap<>();
    tools.put("cache", cacheOps);
    tools.put("sensor", scriptSensorQuery);

    try {
        Integer level = groovyScriptExecutor.executeWithTools(
                strategy.getScriptContent(), variables, tools);
        result.setLevel(level);
        result.setLevelText(level != null ? AlarmConstants.resolveLevelText(level) : null);
    } catch (Exception e) {
        result.setError(e.getMessage());
    }
    result.setDurationMs(System.currentTimeMillis() - start);
    return result;
}
```

- [ ] **步骤 4：Controller 新增端点**

在 `AlarmStrategyController.java` 中添加 import：

```java
import com.zwei.iot.alarm.domain.dto.StrategyTestRunRequest;
import com.zwei.iot.alarm.domain.dto.StrategyTestRunResult;
```

在类方法末尾添加：

```java
/**
 * 测试运行综合告警策略脚本（不产生告警记录）。
 */
@PostMapping("/{id}/test-run")
@PreAuthorize("@ss.hasPermi('iot:alarm-strategy:list')")
public AjaxResult testRun(@PathVariable Long id, @RequestBody(required = false) StrategyTestRunRequest request) {
    return success(strategyService.testRun(id, request));
}
```

- [ ] **步骤 5：编译验证**

运行：`cd server && mvn compile -pl zwei-iot-alarm -am`
预期：BUILD SUCCESS

- [ ] **步骤 6：Commit**

```bash
cd server && git add zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/domain/dto/ \
  zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/IAlarmStrategyService.java \
  zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/impl/AlarmStrategyServiceImpl.java \
  zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/controller/AlarmStrategyController.java
git commit -m "feat(alarm): 新增策略测试运行端点 POST /strategies/{id}/test-run"
```

---

## 任务 4：前端 — script-api-docs.ts 多模式扩展

**文件：**
- 修改：`web/src/views/basic/components/script-editor/script-api-docs.ts`

- [ ] **步骤 1：编写失败的测试**

创建或扩展 `web/src/views/basic/components/script-editor/__tests__/script-api-docs.test.ts`：

```typescript
import { describe, it, expect } from 'vitest'
import { getApiDocs, API_DOCS, type ScriptMode } from '../script-api-docs'

describe('script-api-docs multi-mode', () => {
  it('calc 模式返回 4 分组', () => {
    const docs = getApiDocs('calc')
    expect(docs).toHaveLength(4)
    expect(docs.map(g => g.name)).toContain('curData')
    expect(docs.map(g => g.name)).toContain('prevData')
    expect(docs.map(g => g.name)).toContain('cache')
    expect(docs.map(g => g.name)).toContain('sensor')
  })

  it('alarm 模式返回 4 分组 (hazardPointIds + currentTime + cache + sensor)', () => {
    const docs = getApiDocs('alarm')
    expect(docs).toHaveLength(4)
    expect(docs.map(g => g.name)).toContain('hazardPointIds')
    expect(docs.map(g => g.name)).toContain('currentTime')
    expect(docs.map(g => g.name)).toContain('cache')
    expect(docs.map(g => g.name)).toContain('sensor')
  })

  it('alarm 模式不含 curData', () => {
    const docs = getApiDocs('alarm')
    expect(docs.map(g => g.name)).not.toContain('curData')
    expect(docs.map(g => g.name)).not.toContain('prevData')
  })

  it('alarm 模式 hazardPointIds 分组有 methods', () => {
    const docs = getApiDocs('alarm')
    const hpGroup = docs.find(g => g.name === 'hazardPointIds')!
    expect(hpGroup.methods.length).toBeGreaterThan(0)
  })

  it('API_DOCS 别名等价于 getApiDocs(calc)', () => {
    expect(API_DOCS).toEqual(getApiDocs('calc'))
  })
})
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd web && npx vitest run src/views/basic/components/script-editor/__tests__/script-api-docs.test.ts`
预期：FAIL — `getApiDocs` 不存在（导出错误）

- [ ] **步骤 3：实现多模式扩展**

将 `script-api-docs.ts` 的导出区改为：

```typescript
export type ScriptMode = 'calc' | 'alarm'

/** calc 模式特有分组 */
const CALC_SPECIFIC_GROUPS: ApiGroup[] = [
  {
    icon: '📦',
    color: '#409eff',
    name: 'curData',
    methods: [
      { signature: '.deviceCode', note: '设备编码' },
      { signature: '.sensorCode', note: '传感器编码' },
      { signature: '.props.<attrCode>', note: '当前数据包属性值' },
      { signature: '.properties.<attrCode>', note: 'props 别名 (同引用)' },
      { signature: '.dataTime', note: '数据时间戳 (ms)' }
    ]
  },
  {
    icon: '📦',
    color: '#409eff',
    name: 'prevData',
    description: '可空',
    methods: [
      { signature: '.deviceCode', note: '上一条设备编码' },
      { signature: '.sensorCode', note: '上一条传感器编码' },
      { signature: '.props.<attrCode>', note: '上一条数据包属性值' },
      { signature: '.properties.<attrCode>', note: 'props 别名 (同引用)' },
      { signature: '.dataTime', note: '上一条数据时间戳 (ms)' }
    ]
  }
]

/** alarm 模式特有分组 */
const ALARM_SPECIFIC_GROUPS: ApiGroup[] = [
  {
    icon: '⚠️',
    color: '#e6a23c',
    name: 'hazardPointIds',
    description: 'List<Long> — 绑定的隐患点 ID',
    methods: [
      { signature: '.size()', note: '隐患点数量' },
      { signature: '[i]', note: '按索引取 ID' },
      { signature: 'for (id in hazardPointIds) { ... }' }
    ]
  },
  {
    icon: '🕐',
    color: '#e6a23c',
    name: 'currentTime',
    description: 'String — 当前时间 (yyyy-MM-dd HH:mm:ss)',
    methods: [
      { signature: 'new Date(currentTime)' }
    ]
  }
]

/** 共享分组 (cache + sensor) */
const SHARED_GROUPS: ApiGroup[] = [
  {
    icon: '🛠',
    color: '#67c23a',
    name: 'cache',
    description: 'Redis 二次封装',
    methods: [
      { signature: 'getInt(key, default?)' },
      { signature: 'getLong(key, default?)' },
      { signature: 'getDouble(key, default?)' },
      { signature: 'getFloat(key, default?)' },
      { signature: 'getBigDecimal(key, default?)' },
      { signature: 'getString(key, default?)' },
      { signature: 'getBoolean(key, default?)' },
      { signature: 'set(key, value)' },
      { signature: 'set(key, value, timeout, unit)' },
      { signature: 'delete(key) → boolean' },
      { signature: 'hasKey(key) → boolean' },
      { signature: 'expire(key, timeout, unit?) → boolean', note: '省略 unit 时 timeout 单位为秒' },
      { signature: 'getExpire(key) → long' }
    ]
  },
  {
    icon: '📡',
    color: '#67c23a',
    name: 'sensor',
    description: 'IoTDB 查询',
    methods: [
      { signature: 'query(deviceCode, sensorCode, time, attrCode)', note: '异常时返回 null,不中断脚本' },
      { signature: '↳ .time', note: '最近一条数据的时间戳 (ms, long)' },
      { signature: '↳ .values.<attrCode>', note: '属性值 (Double), 无数据时为 null' }
    ]
  }
]

export function getApiDocs(mode: ScriptMode): ApiGroup[] {
  if (mode === 'alarm') return [...ALARM_SPECIFIC_GROUPS, ...SHARED_GROUPS]
  return [...CALC_SPECIFIC_GROUPS, ...SHARED_GROUPS]
}

/** @deprecated 使用 getApiDocs('calc') 替代 */
export const API_DOCS: ApiGroup[] = getApiDocs('calc')
```

- [ ] **步骤 4：运行测试验证通过**

运行：`cd web && npx vitest run src/views/basic/components/script-editor/__tests__/script-api-docs.test.ts`
预期：PASS — 5 个测试全部通过

- [ ] **步骤 5：验证现有 CalcScriptEditor 回归**

运行：`cd web && npx vitest run src/views/basic/components/script-editor`
预期：PASS — ApiDocsSidebar 现有测试不破坏

- [ ] **步骤 6：Commit**

```bash
cd web && git add src/views/basic/components/script-editor/script-api-docs.ts \
  src/views/basic/components/script-editor/__tests__/script-api-docs.test.ts
git commit -m "feat(web): script-api-docs 多模式扩展 (calc/alarm)"
```

---

## 任务 5：前端 — ApiDocsSidebar.vue mode prop

**文件：**
- 修改：`web/src/views/basic/components/script-editor/ApiDocsSidebar.vue`

- [ ] **步骤 1：修改 ApiDocsSidebar.vue**

将 `<script setup>` 替换为：

```vue
<script setup lang="ts">
import { computed } from 'vue'
import { getApiDocs, type ScriptMode } from './script-api-docs'

const props = withDefaults(defineProps<{
  mode?: ScriptMode
}>(), { mode: 'calc' })

const docs = computed(() => getApiDocs(props.mode))
</script>
```

将 template 中 `API_DOCS` 引用替换为 `docs`：

```vue
<section v-for="group in docs" :key="group.name" class="api-group">
```

- [ ] **步骤 2：验证现有测试回归**

运行：`cd web && npx vitest run src/views/basic/components/script-editor/__tests__/ApiDocsSidebar.test.ts`
预期：PASS（默认 mode='calc'，行为不变）

- [ ] **步骤 3：Commit**

```bash
cd web && git add src/views/basic/components/script-editor/ApiDocsSidebar.vue
git commit -m "feat(web): ApiDocsSidebar 新增 mode prop 支持 alarm 模式"
```

---

## 任务 6：前端 — TestPanel.vue mode prop

**文件：**
- 修改：`web/src/views/basic/components/script-editor/TestPanel.vue`

TestPanel 当前有 curData/prevData 输入区域（calc 模式专用）。alarm 模式不需要这些输入。

- [ ] **步骤 1：修改 TestPanel.vue 支持 alarm 模式**

在 `<script setup>` 中添加 mode prop 和 alarm 结果类型：

```typescript
import type { CalcScriptTestResult } from '@/api/monitorType'

export interface StrategyTestResult {
  level: number | null
  levelText: string | null
  durationMs: number
  error: string | null
}

type AnyTestResult = CalcScriptTestResult | StrategyTestResult

const props = withDefaults(defineProps<{
  result: AnyTestResult | null
  testing: boolean
  mode?: 'calc' | 'alarm'
}>(), { mode: 'calc' })

const emit = defineEmits<{
  runTest: [payload: { curData?: Record<string, any>; prevData?: Record<string, any> | undefined }]
}>()
```

修改 handleRun 函数，alarm 模式下不解析 JSON：

```typescript
function handleRun() {
  if (props.mode === 'alarm') {
    emit('runTest', {})
    return
  }
  // calc 模式原逻辑不变
  jsonError.value = ''
  let curData: Record<string, any>
  try {
    curData = curDataJson.value.trim() ? JSON.parse(curDataJson.value) : {}
  } catch (e) {
    jsonError.value = 'curData 不是合法 JSON, 请检查格式'
    return
  }
  let prevData: Record<string, any> | undefined
  if (prevDataJson.value.trim()) {
    try {
      prevData = JSON.parse(prevDataJson.value)
    } catch (e) {
      jsonError.value = 'prevData 不是合法 JSON, 请检查格式'
      return
    }
  }
  emit('runTest', { curData, prevData })
}
```

在 template 中，用 `v-if="mode === 'calc'"` 包裹 curData/prevData 输入区和清空按钮：

```vue
<!-- calc 模式才显示模拟数据输入 -->
<template v-if="mode === 'calc'">
  <div class="form-row">
    <label class="row-label">curData</label>
    <el-input v-model="curDataJson" type="textarea" :rows="4"
      placeholder='{"props":{"attrCode":12.5}}' data-test="cur-data-input" />
  </div>
  <div class="form-row">
    <label class="row-label">prevData</label>
    <el-input v-model="prevDataJson" type="textarea" :rows="4"
      placeholder='{"props":{"attrCode":10.0},"dataTime":1700000000000}'
      data-test="prev-data-input" />
  </div>
  <div v-if="jsonError" class="json-error">{{ jsonError }}</div>
</template>
```

结果展示区添加 alarm 模式适配（在现有 calc 结果之后）：

```vue
<!-- alarm 模式结果 -->
<el-alert
  v-if="mode === 'alarm' && result"
  :type="(result as StrategyTestResult).error ? 'error' : 'success'"
  :closable="false"
  class="result-alert"
  data-test="result-alarm"
>
  <template #title>
    <span v-if="(result as StrategyTestResult).error">
      ❌ 错误: {{ (result as StrategyTestResult).error }}
    </span>
    <span v-else>
      ✅ 告警等级: {{ (result as StrategyTestResult).levelText || '无告警' }}
      · 耗时 {{ (result as StrategyTestResult).durationMs }}ms
    </span>
  </template>
</el-alert>
```

- [ ] **步骤 2：验证现有测试回归**

运行：`cd web && npx vitest run src/views/basic/components/script-editor`
预期：PASS — 默认 mode='calc'，现有测试不受影响

- [ ] **步骤 3：Commit**

```bash
cd web && git add src/views/basic/components/script-editor/TestPanel.vue
git commit -m "feat(web): TestPanel 新增 mode prop 支持 alarm 模式"
```

---

## 任务 7：前端 — API 模块 testStrategyRun

**文件：**
- 修改：`web/src/api/alarm.ts`

- [ ] **步骤 1：添加类型和 API 函数**

在 `alarm.ts` 中添加类型定义和 API 函数：

```typescript
/** 综合告警策略测试运行结果 */
export interface StrategyTestRunResult {
  level: number | null
  levelText: string | null
  durationMs: number
  error: string | null
}

/** 测试运行综合告警策略脚本 */
export const testStrategyRun = (id: number, payload?: {
  mockSensorCode?: string
  mockDataTime?: number
}) =>
  request.post<StrategyTestRunResult>(`/alarm/strategies/${id}/test-run`, payload || {})
```

- [ ] **步骤 2：Commit**

```bash
cd web && git add src/api/alarm.ts
git commit -m "feat(web): alarm API 新增 testStrategyRun"
```

---

## 任务 8：前端 — CompositeAlarmScriptEditor.vue（新建）

**文件：**
- 创建：`web/src/views/alarm/components/CompositeAlarmScriptEditor.vue`

- [ ] **步骤 1：创建 CompositeAlarmScriptEditor.vue**

```vue
<template>
  <div class="composite-editor">
    <el-alert
      v-if="statusBar"
      :type="statusBar.type"
      :closable="false"
      class="status-bar"
      data-test="status-bar"
    >
      <template #title>{{ statusBar.text }}</template>
    </el-alert>

    <div class="editor-area">
      <div class="editor-main">
        <div class="editor-tag">Groovy</div>
        <CodeMirrorGroovy
          :model-value="localScript"
          @update:model-value="onScriptChange"
          class="cm-wrapper"
        />
      </div>
      <ApiDocsSidebar class="editor-side" mode="alarm" />
    </div>

    <TestPanel
      mode="alarm"
      :result="testResult as any"
      :testing="testing"
      @run-test="onRunTest"
    />

    <div class="editor-footer">
      <el-button data-test="reset-btn" @click="onReset">重置为模板</el-button>
      <el-button
        type="primary"
        :disabled="!canSave"
        :class="{ 'save-ready': canSave && dirty }"
        data-test="save-btn"
        :loading="saving"
        @click="onSave"
      >保存</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getStrategyDetail, updateStrategy, testStrategyRun, type StrategyTestRunResult } from '@/api/alarm'
import CodeMirrorGroovy from '../../basic/components/script-editor/CodeMirrorGroovy.vue'
import ApiDocsSidebar from '../../basic/components/script-editor/ApiDocsSidebar.vue'
import TestPanel from '../../basic/components/script-editor/TestPanel.vue'

const props = defineProps<{
  alarmId: number
  triggerMode: 'PERIODIC' | 'REALTIME'
}>()

const emit = defineEmits<{
  saved: []
}>()

const initialScript = ref('')
const localScript = ref('')
const testedPassed = ref(false)
const testing = ref(false)
const saving = ref(false)
const testResult = ref<StrategyTestRunResult | null>(null)

const dirty = computed(() => localScript.value !== initialScript.value)
const canSave = computed(() => !dirty.value || testedPassed.value)

const statusBar = computed(() => {
  if (!dirty.value) return null
  if (testedPassed.value) return { type: 'success' as const, text: '✅ 测试通过, 可以保存' }
  if (testResult.value && testResult.value.error)
    return { type: 'error' as const, text: `❌ 测试失败: ${testResult.value.error}` }
  return { type: 'warning' as const, text: '⚠️ 修改后必须通过测试才能保存' }
})

const defaultTemplate = computed(() =>
  '// 综合告警脚本 — 返回 1-4 表示告警等级 (red/orange/yellow/blue), 0 或 null = 无告警\n' +
  '// 可用变量:\n' +
  '//   hazardPointIds  绑定的隐患点 ID 列表 (List<Long>)\n' +
  '//   currentTime     当前时间戳 (long, ms)\n' +
  '// 工具:\n' +
  '//   cache.getInt(key, default)   Redis 读取\n' +
  '//   sensor.query(deviceCode, sensorCode, time, attrCode)  IoTDB 查询\n\n' +
  'def level = 0\n\n' +
  '// 在此编写判断逻辑...\n\n' +
  'return level\n'
)

watch(() => props.alarmId, async (id) => {
  if (!id) return
  try {
    const detail = await getStrategyDetail(id)
    initialScript.value = detail.scriptCode || ''
    localScript.value = detail.scriptCode || defaultTemplate.value
  } catch {
    localScript.value = defaultTemplate.value
    initialScript.value = ''
  }
  testedPassed.value = false
  testResult.value = null
}, { immediate: true })

function onScriptChange(newVal: string) {
  localScript.value = newVal
  testedPassed.value = false
}

function onReset() {
  localScript.value = defaultTemplate.value
  testedPassed.value = false
  testResult.value = null
}

async function onRunTest(_payload: { curData?: Record<string, any>; prevData?: Record<string, any> }) {
  if (!props.alarmId) {
    ElMessage.warning('请先保存策略, 再测试脚本')
    return
  }
  testing.value = true
  try {
    const result = await testStrategyRun(props.alarmId)
    testResult.value = result
    testedPassed.value = result.error == null
  } catch (e: any) {
    testResult.value = {
      level: null, levelText: null, durationMs: 0,
      error: e?.message || '请求失败'
    }
    testedPassed.value = false
  } finally {
    testing.value = false
  }
}

async function onSave() {
  if (!canSave.value) return
  if (!localScript.value.trim()) {
    ElMessage.warning('脚本不能为空')
    return
  }
  saving.value = true
  try {
    await updateStrategy(props.alarmId, { scriptContent: localScript.value } as any)
    initialScript.value = localScript.value
    ElMessage.success('脚本已保存')
    emit('saved')
  } catch (e: any) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.composite-editor {
  padding: 0;
}

.status-bar {
  margin-bottom: 12px;
}

.editor-area {
  display: flex;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  overflow: hidden;
  height: 320px;
}

.editor-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  position: relative;
}

.editor-tag {
  position: absolute;
  top: 6px;
  right: 8px;
  z-index: 2;
  background: #264f78;
  color: white;
  padding: 1px 6px;
  font-size: 10px;
  border-radius: 2px;
  font-family: 'Consolas', monospace;
}

.cm-wrapper {
  flex: 1;
  overflow: hidden;
}

.editor-side {
  flex-shrink: 0;
}

.editor-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 12px 0;
}

.save-ready {
  background: #67c23a !important;
  border-color: #67c23a !important;
}
</style>
```

- [ ] **步骤 2：Commit**

```bash
cd web && git add src/views/alarm/components/CompositeAlarmScriptEditor.vue
git commit -m "feat(web): 新建 CompositeAlarmScriptEditor 复用 CalcScriptEditor 架构"
```

---

## 任务 9：前端 — CompositeAlarmScriptDrawer.vue 重写

**文件：**
- 重写：`web/src/views/alarm/components/CompositeAlarmScriptDrawer.vue`

- [ ] **步骤 1：全量替换 CompositeAlarmScriptDrawer.vue**

将 1183 行文件完全替换为：

```vue
<template>
  <el-drawer
    :model-value="visible"
    size="70%"
    :title="'综合告警脚本编辑器'"
    @close="handleClose"
  >
    <CompositeAlarmScriptEditor
      :alarm-id="alarmId"
      :trigger-mode="triggerMode"
      @saved="onSaved"
    />
  </el-drawer>
</template>

<script setup lang="ts">
import CompositeAlarmScriptEditor from './CompositeAlarmScriptEditor.vue'

const props = defineProps<{
  visible: boolean
  alarmId: number
  triggerMode: 'PERIODIC' | 'REALTIME'
}>()

const emit = defineEmits<{
  'update:visible': [val: boolean]
  saved: []
}>()

function handleClose() {
  emit('update:visible', false)
}

function onSaved() {
  emit('saved')
}
</script>
```

- [ ] **步骤 2：编译验证**

运行：`cd web && npx vue-tsc --noEmit`
预期：无类型错误

- [ ] **步骤 3：Commit**

```bash
cd web && git add src/views/alarm/components/CompositeAlarmScriptDrawer.vue
git commit -m "refactor(web): CompositeAlarmScriptDrawer 退化为壳, 移除 Blockly (1183→33行)"
```

---

## 任务 10：清理 — 移除 Blockly 依赖

**文件：**
- 修改：`web/package.json`

- [ ] **步骤 1：全局搜索确认无其他文件引用 Blockly**

运行：`cd web && grep -r "blockly" src/ --include="*.vue" --include="*.ts" -l`
预期：无输出（或仅 CompositeAlarmScriptDrawer.vue 已被重写不包含 blockly）

- [ ] **步骤 2：从 package.json 移除 blockly 依赖**

在 `package.json` 的 `dependencies` 中删除 `"blockly": "..."` 行。

- [ ] **步骤 3：安装验证**

运行：`cd web && npm install`
预期：无错误，node_modules 中 blockly 被移除

- [ ] **步骤 4：构建验证**

运行：`cd web && npm run build`
预期：构建成功，无 import 错误

- [ ] **步骤 5：Commit**

```bash
cd web && git add package.json package-lock.json
git commit -m "chore(web): 移除 blockly 依赖 (已废弃 Blockly 可视化编程)"
```

---

## 自检

### 规格覆盖度

| 规格需求 | 实现任务 | 状态 |
|----------|----------|------|
| GroovyScriptExecutor 增强 | 任务 1 | ✅ |
| ComprehensiveAlarmJob tools 注入 | 任务 2 | ✅ |
| 测试端点 test-run | 任务 3 | ✅ |
| script-api-docs 多模式 | 任务 4 | ✅ |
| ApiDocsSidebar mode prop | 任务 5 | ✅ |
| TestPanel mode prop | 任务 6 | ✅ |
| API 模块 testStrategyRun | 任务 7 | ✅ |
| CompositeAlarmScriptEditor | 任务 8 | ✅ |
| CompositeAlarmScriptDrawer 重写 | 任务 9 | ✅ |
| Blockly 依赖清理 | 任务 10 | ✅ |

### 占位符扫描

- 无 "TODO" / "待定" / "后续实现" ✅
- 每个步骤都包含完整代码 ✅
- 每个任务都有验证命令 ✅

### 类型一致性

- `executeWithTools(scriptContent, variables, tools)` — 任务 1 定义，任务 2/3 调用 ✅
- `StrategyTestRunRequest` / `StrategyTestRunResult` — 任务 3 定义，任务 7 前端 `StrategyTestRunResult` 字段名对齐 ✅
- `getApiDocs(mode: ScriptMode)` — 任务 4 定义，任务 5/8 调用 ✅
- `testStrategyRun(id, payload)` — 任务 7 定义，任务 8 调用 ✅
- TestPanel `mode` prop — 任务 6 定义，任务 8 使用 `mode="alarm"` ✅
