# 综合告警脚本编辑器改造 — 注入 sensor/cache 工具

> **日期**: 2026-06-27
> **状态**: 设计已批准，待实现
> **范围**: 后端 GroovyScriptExecutor 增强 + 前端 CompositeAlarmScriptDrawer 全量迁移（废弃 Blockly）

## 1. 背景与目标

综合告警的 Groovy 脚本编辑器当前使用 Blockly 可视化编程 + textarea 双模式（`CompositeAlarmScriptDrawer.vue`，1183 行），脚本仅能访问 `hazardPointIds` 和 `currentTime` 两个变量。

计算属性模块已建成成熟的脚本编辑器架构（`CalcScriptEditor.vue` + `CodeMirrorGroovy` + `ApiDocsSidebar` + `TestPanel`），且已实现 `ScriptCacheOps`（Redis 封装）和 `ScriptSensorQuery`（IoTDB 查询）两个工具 bean 的注入。

**目标**：复用计算属性的脚本编辑器架构，为综合告警脚本注入 `cache` + `sensor` 工具，让综合告警策略能在 Groovy 脚本中直接查询 Redis 和 IoTDB。

## 2. 设计决策

| 决策项 | 选择 | 理由 |
|--------|------|------|
| 前端范围 | 全量迁移（废弃 Blockly） | 用户明确要求"参考计算属性的脚本编辑器" |
| 后端引擎 | 增强现有 GroovyScriptExecutor | 不迁移到 GroovyScriptEngine，保持 alarm 模块独立 |
| 运行模式 | CRON 保持，REALTIME 不动 | 仅追加 tools 注入，不改变现有 variables 传递 |
| API 文档 | 多模式扩展 script-api-docs.ts | 共享 cache/sensor 分组，隔离 curData/hazardPointIds |

## 3. 文件变更总览

### 后端（4 文件修改，0 新建）

| 文件 | 变更 |
|------|------|
| `GroovyScriptExecutor.java` | 新增 `executeWithTools(script, variables, tools)` 重载 |
| `ComprehensiveAlarmJob.java` | 追加 tools 注入，variables 不变 |
| `AlarmStrategyController.java` | 新增 `POST /{id}/test-run` 端点 |
| `AlarmStrategyServiceImpl.java` | 新增 `testRun()` 方法 |

### 前端（2 修改，1 新建，1 重写）

| 文件 | 变更 |
|------|------|
| `script-api-docs.ts` | 多模式扩展：`getApiDocs(mode)` 工厂函数 |
| `ApiDocsSidebar.vue` | 新增 `mode` prop |
| `CompositeAlarmScriptEditor.vue` | **新建** — 对照 CalcScriptEditor 改造 |
| `CompositeAlarmScriptDrawer.vue` | **重写** — 1183 行退化为 ~70 行壳 |

## 4. 后端设计

### 4.1 GroovyScriptExecutor 增强

新增重载方法，旧方法委托给新方法（DRY）：

```java
public Integer execute(String scriptContent, Map<String, Object> variables) {
    return executeWithTools(scriptContent, variables, null);
}

public Integer executeWithTools(String scriptContent, Map<String, Object> variables, Map<String, Object> tools) {
    // 复用 isSafeScript + Future + timeout
    // Binding 阶段: variables.forEach(binding::setVariable)
    //               tools.forEach(binding::setVariable)   ← 新增
}
```

**沙箱策略**：保留现有 `FORBIDDEN_KEYWORDS` 关键字过滤，不引入 AST 沙箱。

### 4.2 ComprehensiveAlarmJob 改造

仅改 line 132-137，variables 原样不变：

```java
Map<String, Object> variables = new HashMap<>();
variables.put("hazardPointIds", hazardPointIds);    // 不变
variables.put("currentTime", now);                   // 不变

Map<String, Object> tools = Map.of("cache", cacheOps, "sensor", scriptSensorQuery);  // 新增

Integer alarmLevel = scriptExecutor.executeWithTools(strategy.getScriptContent(), variables, tools);
```

构造函数追加注入 `ScriptCacheOps` + `ScriptSensorQuery`。

### 4.3 测试端点

`POST /api/v1/alarm/strategies/{id}/test-run`

请求体：
```json
{ "mockSensorCode": "RAIN-001", "mockDataTime": 1719400000000 }
```

响应：
```json
{ "code": 200, "data": { "level": 3, "levelText": "黄色", "durationMs": 234, "error": null } }
```

**关键约束**：不写库、不发事件、不写 Redis 去重 key。仅返回脚本执行结果。

### 4.4 依赖注入

`zwei-iot-alarm` 已在 pom.xml 依赖 `zwei-iot-timeseries`，可直接 `@Autowired` 注入 `ScriptCacheOps` + `ScriptSensorQuery`，无需新增 Maven 依赖。

## 5. 前端设计

### 5.1 script-api-docs.ts 多模式扩展

```typescript
export type ScriptMode = 'calc' | 'alarm'

export function getApiDocs(mode: ScriptMode): ApiGroup[] {
  if (mode === 'alarm') return [...ALARM_SPECIFIC_GROUPS, ...SHARED_GROUPS]
  return [...CALC_SPECIFIC_GROUPS, ...SHARED_GROUPS]
}
```

| 分组 | calc | alarm |
|------|------|-------|
| curData / prevData | ✅ | ❌ |
| cache / sensor | ✅ | ✅ |
| hazardPointIds / currentTime | ❌ | ✅ |

向后兼容：`export const API_DOCS = getApiDocs('calc')`（deprecated 别名）。

### 5.2 ApiDocsSidebar.vue

新增 `mode?: ScriptMode` prop（默认 `'calc'`），内部 `computed(() => getApiDocs(props.mode))`。

### 5.3 CompositeAlarmScriptEditor.vue（新建）

对照 `CalcScriptEditor.vue` 架构，差异：

| 维度 | CalcScriptEditor | CompositeAlarmScriptEditor |
|------|------------------|---------------------------|
| API docs mode | calc | alarm |
| 模板 | `return curData?.props?.xxx` | `return 0  // 1-4 告警等级` |
| 测试 API | `testCalcScript` | `testStrategyRun` |
| 保存 | emit('save') → 父组件 | 内部调 `updateStrategy(alarmId, ...)` |
| 测试结果 | success/error/value | level/levelText/durationMs/error |

布局：CodeMirrorGroovy（编辑区）+ ApiDocsSidebar mode=alarm（右侧）+ TestPanel（底部）。

### 5.4 CompositeAlarmScriptDrawer.vue 退化

删除全部 Blockly 逻辑、自定义块、代码生成器、工具面板。退化为：

```vue
<el-drawer :model-value="visible" size="80%" @close="$emit('update:visible', false)">
  <template #header><h3>综合告警脚本编辑器</h3></template>
  <CompositeAlarmScriptEditor :alarm-id="alarmId" :trigger-mode="triggerMode" @saved="$emit('saved')" />
</el-drawer>
```

### 5.5 Blockly 依赖清理

`package.json` 移除 `blockly` 依赖（需全局 Grep 确认无其他文件引用）。

## 6. 数据流

### 路径 A — CRON 定时触发

```
@Scheduled(60s)
  → ComprehensiveAlarmJob.executeStrategy()
    → variables = {hazardPointIds, currentTime}     // 不变
    → tools = {cache, sensor}                        // 新增
    → groovyScriptExecutor.executeWithTools(script, variables, tools)
    → AlarmDedupService → AlarmRecord → AlarmTriggeredEvent
```

### 路径 B — 前端测试运行

```
TestPanel @run-test
  → POST /api/v1/alarm/strategies/{id}/test-run
    → variables = {hazardPointIds(从DB), currentTime}
    → tools = {cache, sensor}
    → groovyScriptExecutor.executeWithTools(script, variables, tools)
  → 返回 {level, levelText, durationMs, error}
  → 不写库 / 不发事件 / 不写 Redis
```

## 7. 错误处理

| 层 | 错误类型 | 处理方式 |
|----|----------|----------|
| ScriptCacheOps | Redis 异常 | 透传给脚本 |
| ScriptSensorQuery | IoTDB/deviceCode 异常 | 吞异常返回 null |
| GroovyScriptExecutor | 语法/运行时异常 | catch → return null |
| GroovyScriptExecutor | 超时 | future.cancel → return null |
| GroovyScriptExecutor | 不安全关键字 | isSafeScript → return null |
| ComprehensiveAlarmJob | 单策略异常 | catch → updateResult(FAIL) |
| AlarmStrategyService.testRun | 任意异常 | 封装到 error 字段 |

## 8. 测试矩阵

### 后端（12 用例）

| 测试类 | 用例 |
|--------|------|
| GroovyScriptExecutorTest (5) | execute 向后兼容 / executeWithTools_nullTools / cache 注入可访问 / sensor 注入可访问 / 工具异常返回 null |
| AlarmStrategyServiceImplTest (5) | testRun 返回 level / 返回 null / 脚本异常 / 不创建 record / 不发 event |
| ComprehensiveAlarmJobTest (2) | tools 注入不影响 variables / 现有流程回归 |

### 前端（14 用例）

| 测试文件 | 用例 |
|----------|------|
| script-api-docs.test.ts (5) | calc 4 分组 / alarm 4 分组 / alarm 有 hazardPointIds / alarm 无 curData / API_DOCS 别名兼容 |
| ApiDocsSidebar.test.ts (2) | mode=alarm 渲染 / mode=calc 渲染 |
| CompositeAlarmScriptEditor.test.ts (5) | 渲染 / dirty 标记 / 测试通过可保存 / save 事件 / 数据加载 |
| CompositeAlarmScriptDrawer.test.ts (2) | props 透传 / close 事件 |

**总计：26 个测试用例**

## 9. 验证命令

```bash
# 后端
cd server && mvn test -pl zwei-iot-alarm -Dtest=GroovyScriptExecutorTest,AlarmStrategyServiceImplTest,ComprehensiveAlarmJobTest

# 前端
cd web && npx vitest run src/views/basic/components/script-editor
cd web && npx vitest run src/views/alarm/components/CompositeAlarm
```
