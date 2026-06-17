# 计算属性 (Computed Attribute) 设计规格

| 项 | 值 |
|---|---|
| 创建日期 | 2026-06-17 |
| 状态 | 待审查 |
| 关联模块 | `zwei-iot-monitor` / `zwei-iot-timeseries` / `zwei-iot-parser` / `web` |
| 关联文件 | `MonitorIngestFacade.java` / `MonitorContent.java` / `MonitorType.vue` |

## 1. 背景与目标

### 1.1 现状

`monitor_content` 表定义监测类型下的具体监测指标(如小时雨量、X 轴位移)。所有属性都由设备直接上报,经 MQTT → `MonitorIngestFacade.ingest()` → Redis Stream → IoTDB 的链路入库。

部分派生指标(如变化速率、累积值、差分等)目前只能在业务查询层临时计算,无法落库、无法参与告警判据、无法在仪表盘实时展示。

### 1.2 目标

为监测属性引入"字段类型"维度:

- **固有属性 (inherent)**:设备直接上报的数据(默认类型,行为不变)。
- **计算属性 (computed)**:通过 Groovy 脚本对固有属性计算得出的派生属性,在数据标准化解析后、入队前生成,合并到 `parsedMessage.properties`,后续与固有属性同链路写入 IoTDB、参与告警。

### 1.3 非目标

- 不支持设备级脚本覆盖(本次脚本定义在监测类型层级)。
- 不引入新的代码编辑器前端依赖(CodeMirror/Monaco)。
- 不修改 `IoTDB` schema 创建路径(sensor_attribute 创建逻辑不变)。
- 不修改 TSL 模型(保持 TSL 纯粹,不感知 fieldType/calcScript)。

## 2. 核心决策

| # | 决策点 | 结果 |
|---|---|---|
| 1 | `prevData` 来源 | Redis Hash,key = `monitor:last:{deviceId}:{sensorCode}`,TTL 7 天 |
| 2 | `curData` / `prevData` 结构 | `{ deviceCode, sensorCode, dataTime, properties: Map<attrCode, value> }` |
| 3 | 计算属性间依赖 | 允许互相引用,按 `sort_order` 求值,后算可见先算 |
| 4 | 脚本拼装 | 函数拼接 + 主入口 `compute(curData, prevData)`,返回 `Map<attrCode, Object>` |
| 5 | 失败处理 | 单个跳过 + warn 日志,其他照常;外层 try-catch 兜底,绝不影响主链路 |
| 6 | IoTDB 落库 | append 到 `parsedMessage.properties`,与固有属性同链路 |
| 7 | 总体方案 | 方案 A:`monitor_content` 加 2 列 + 独立 `ComputedAttributeEvaluator` 等组件 |

## 3. 架构与组件

### 3.1 模块归属

| 模块 | 改动 |
|---|---|
| `zwei-iot-monitor` | `monitor_content` 表加 2 列;实体/DTO/Mapper/Controller 配套 |
| `zwei-iot-timeseries` | 新增 `compute/` 子包(`Registry` + `Evaluator` + `Assembler` + `LastMessageStore`);修改 `MonitorIngestFacade.ingest()` |
| `zwei-iot-parser` | `GroovyScriptEngine` 新增 `executeComputed(...)` 入口(不改动现有 `execute`) |
| `web` | `MonitorType.vue` 表格列扩展 + 新组件 `CalcScriptEditor.vue` + API 扩展 |

不引入新 Maven 依赖,不修改 `pom.xml`。

### 3.2 新增/修改文件清单

```
zwei-iot-monitor
├── domain/MonitorContent.java                 [改] 加 fieldType + calcScript
├── domain/dto/MonitorContentCreateRequest.java [改] 加 fieldType + calcScript + Pattern
├── domain/dto/MonitorContentUpdateRequest.java [改] 加 calcScript
├── mapper/MonitorContentMapper.java           [改] 加 selectComputedByTypeId
├── resources/mapper/iot/monitor/MonitorContentMapper.xml [改] 加列映射 + selectComputedByTypeId
├── service/IMonitorContentService.java        [改] 加 selectComputedByTypeId 接口
├── service/impl/MonitorContentServiceImpl.java [改] 实现 + @CacheEvict 联动
└── controller/MonitorContentController.java   [改] 加 calcScript 校验 + test-script 端点

zwei-iot-timeseries
└── compute/                                    [新包]
    ├── ComputedAttribute.java                  [新] record
    ├── ComputedAttributeRegistry.java          [新] @Cacheable 缓存
    ├── ComputedAttributeEvaluator.java         [新] 主入口
    ├── ComputedScriptAssembler.java            [新] 脚本拼装 + 缓存
    ├── LastMessageStore.java                   [新] Redis Hash 读写
    └── ParsedMessageSnapshot.java              [新] 精简快照 record
└── service/MonitorIngestFacade.java            [改] 第 4.5 环节插入求值

zwei-iot-parser
└── engine/GroovyScriptEngine.java              [改] 加 executeComputed 方法

web
├── api/monitorType.ts                          [改] 类型 + testCalcScript
├── views/basic/MonitorType.vue                 [改] 字段类型列 + 操作列扩展 + 校验
└── views/basic/components/CalcScriptEditor.vue [新] 计算脚本编辑弹窗

db/upgrade/v2.2-computed-attribute.sql          [新] DDL 升级脚本
```

### 3.3 调用序列

```
MonitorIngestFacade.ingest()
  ├─ ① topicParser.parse(topic)                  → MonitorTopic
  ├─ ② metadataService.resolveStrategy(...)      → DataParseStrategy
  ├─ ③ scriptEngine.execute(strategy, ...)       → ParsedMessage (固有属性)
  ├─ ④ enrichProperties(parsedMessage, tsl)      → ParsedMessage (位置标识符映射)
  ├─ ④.5 computedAttrEvaluator.evaluate(deviceId, sensorCode, parsedMessage)
  │       │
  │       ├─ sensorQuery.requireSensorMetadata(deviceId, sensorCode) → SensorMetadata
  │       ├─ registry.getByMonitorTypeId(meta.monitorTypeId)         → List<ComputedAttribute>
  │       │      (空列表 → 直接 return,fast path)
  │       ├─ lastMessageStore.get(deviceId, sensorCode)              → prevData (可 null)
  │       ├─ 构建 curData (ParsedMessage → Map 形式)
  │       ├─ assembler.assemble(attrs)                               → Groovy 源码 (带缓存)
  │       ├─ scriptEngine.executeComputed(script, curData, prevData) → Map<attrCode, Object>
  │       │      (失败属性按 warn-only 跳过)
  │       ├─ 结果 → List<PropertyValue> + append 到 parsedMessage.properties
  │       └─ lastMessageStore.put(deviceId, sensorCode, curData 含计算属性)
  │              (失败仅 warn,不影响主链路)
  └─ ⑤ streamService.enqueue(parsedMessage)
```

## 4. 数据库变更

### 4.1 表结构变更(`monitor_content`)

```sql
-- db/upgrade/v2.2-computed-attribute.sql

ALTER TABLE monitor_content
    ADD COLUMN field_type VARCHAR(16) NOT NULL DEFAULT 'inherent'
        COMMENT '字段类型: inherent-固有属性, computed-计算属性'
        AFTER indicator_type,
    ADD COLUMN calc_script MEDIUMTEXT NULL
        COMMENT '计算属性脚本(Groovy 代码块,仅 field_type=computed 时必填)'
        AFTER field_type;

ALTER TABLE monitor_content
    ADD INDEX idx_monitor_content_field_type (monitor_type_id, field_type);
```

### 4.2 字段语义

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `field_type` | `VARCHAR(16)` | 是,默认 `inherent` | `inherent` 或 `computed`,**创建后不可改** |
| `calc_script` | `MEDIUMTEXT` | `field_type=computed` 时必填 | Groovy 代码块,最大 64 KB |

### 4.3 字段类型不变性

一旦创建,`field_type` 不可修改。理由:固有属性与计算属性的 IoTDB 数据来源不同,中途切换会导致历史数据语义错乱。前端在编辑模式下字段类型选择器禁用(只读)。

### 4.4 升级脚本位置

按 CLAUDE.md 约定,本次新建 `db/upgrade/v2.2-computed-attribute.sql`,只含上述 DDL,无 DML。存量 12 条 `monitor_content` 数据 `field_type` 自动为 `inherent`,`calc_script` 为 NULL,行为与改动前一致。

### 4.5 `attrCode` 命名约束加强

`MonitorContentCreateRequest.code` 现有 `@Size(max = 100)` 但无 Pattern,本次追加:

```java
@Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]*$",
         message = "监测内容编码必须以字母开头,只能包含字母数字下划线")
```

理由:计算属性的 `code` 会被 `ComputedScriptAssembler` 用作 Groovy 函数名 `calc_{code}`,必须合法 Java 标识符。同时该约束对固有属性无副作用。

## 5. 后端实现细节

### 5.1 `MonitorContent` 实体扩展

```java
public class MonitorContent extends BaseEntity {
    // ... 现有字段
    private String fieldType;      // "inherent" / "computed",默认 "inherent"
    private String calcScript;     // nullable
}
```

### 5.2 DTO 扩展

**Create**:

```java
@Pattern(regexp = "inherent|computed", message = "字段类型必须是 inherent 或 computed")
private String fieldType;  // 默认 "inherent" (Controller 层 null → "inherent")

@Size(max = 65535, message = "计算脚本长度不能超过 64KB")
private String calcScript;
```

Controller 显式校验:`fieldType=computed` 时 `calcScript` 不能为空。

**Update**(只加 calcScript,fieldType 不可改):

```java
@Size(max = 65535, message = "计算脚本长度不能超过 64KB")
private String calcScript;
```

`hasUpdatableField()` 追加 `calcScript != null`。

### 5.3 `IMonitorContentService` 新增方法

```java
/**
 * 查询指定监测类型下的所有计算属性(按 sort_order 排序)。
 * 由 ComputedAttributeRegistry 调用,带 @Cacheable。
 */
List<MonitorContent> selectComputedByTypeId(Long monitorTypeId);
```

Mapper SQL:

```xml
<select id="selectComputedByTypeId" resultMap="MonitorContentResult">
    SELECT id, monitor_type_id, code, name, unit, indicator_type,
           field_type, calc_script, sort_order
    FROM monitor_content
    WHERE monitor_type_id = #{monitorTypeId}
      AND field_type = 'computed'
      AND del_flag = 0
    ORDER BY sort_order ASC
</select>
```

### 5.4 `MonitorContentServiceImpl` 缓存联动

所有 `@CacheEvict` 注解追加 `@CacheEvict(value = "computedAttrs", allEntries = true)`,确保新增/修改/删除计算属性时 `ComputedAttributeRegistry` 缓存失效:

```java
@Caching(evict = {
        @CacheEvict(value = "monitorContent", key = "#monitorContent.id"),
        @CacheEvict(value = "monitorContentList", allEntries = true),
        @CacheEvict(value = "monitorType", allEntries = true),
        @CacheEvict(value = "computedAttrs", allEntries = true)  // 新增
})
public int insertMonitorContent(MonitorContent monitorContent) { ... }
```

(insert / update / delete 全部追加)

### 5.5 `ComputedAttribute` (record)

```java
public record ComputedAttribute(
        Long id,
        Long monitorTypeId,
        String code,         // 必须合法 Java 标识符
        String name,
        String unit,
        String calcScript,   // 非空
        Integer sortOrder
) {
    public static ComputedAttribute from(MonitorContent mc) {
        return new ComputedAttribute(
                mc.getId(), mc.getMonitorTypeId(), mc.getCode(), mc.getName(),
                mc.getUnit(), mc.getCalcScript(), mc.getSortOrder());
    }
}
```

### 5.6 `ComputedAttributeRegistry`

```java
@Service
public class ComputedAttributeRegistry {
    private final IMonitorContentService monitorContentService;

    @Cacheable(value = "computedAttrs", key = "#monitorTypeId")
    public List<ComputedAttribute> getByMonitorTypeId(Long monitorTypeId) {
        return monitorContentService.selectComputedByTypeId(monitorTypeId).stream()
                .map(ComputedAttribute::from)
                .toList();
    }
}
```

### 5.7 `ParsedMessageSnapshot` (record)

```java
public record ParsedMessageSnapshot(
        String deviceCode,
        String sensorCode,
        long dataTime,
        Map<String, Double> properties  // attrCode → value,含计算属性
) { }
```

### 5.8 `LastMessageStore`

```java
@Service
@Slf4j
public class LastMessageStore {
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private static final String KEY_PREFIX = "monitor:last:";
    private static final Duration TTL = Duration.ofDays(7);

    /** 取上一条精简消息,null 表示首次上报或已过期 */
    public ParsedMessageSnapshot get(Long deviceId, String sensorCode) {
        try {
            String json = redis.opsForValue().get(buildKey(deviceId, sensorCode));
            return json == null ? null : objectMapper.readValue(json, ParsedMessageSnapshot.class);
        } catch (Exception e) {
            log.warn("LastMessageStore.get failed: deviceId={}, sensorCode={}",
                    deviceId, sensorCode, e);
            return null;
        }
    }

    /** 写当前条作为下次的 prevData。失败仅 warn 不抛 */
    public void put(Long deviceId, String sensorCode, ParsedMessageSnapshot snapshot) {
        try {
            String json = objectMapper.writeValueAsString(snapshot);
            redis.opsForValue().set(buildKey(deviceId, sensorCode), json, TTL);
        } catch (Exception e) {
            log.warn("LastMessageStore.put failed: deviceId={}, sensorCode={}",
                    deviceId, sensorCode, e);
        }
    }

    private String buildKey(Long deviceId, String sensorCode) {
        return KEY_PREFIX + deviceId + ":" + sensorCode;
    }
}
```

**Redis Key**: `monitor:last:{deviceId}:{sensorCode}`,JSON 序列化,TTL 7 天。
**序列化**: 用项目已有 Jackson 配置(不引入 fastjson)。

### 5.9 `ComputedScriptAssembler`

把多个计算属性脚本片段拼成一个 Groovy 源文件。

**模板**:

```groovy
// === Auto-generated by ComputedScriptAssembler ===
// Do not edit manually.

def calc_attr1(curData, prevData) {
    // === user code: attr1 ===
    {attr1 用户脚本}
}

def calc_attr2(curData, prevData) {
    // === user code: attr2 ===
    {attr2 用户脚本}
}

def compute(curData, prevData) {
    def out = new LinkedHashMap<String, Object>()
    try { out.attr1 = calc_attr1(curData, prevData) }
         catch (Exception e) { /* warn-only */ }
    try {
        out.attr2 = calc_attr2(curData, prevData)
        // 把已计算结果回填到 curData.properties,供后续函数引用
        curData.properties.putAll(out)
    } catch (Exception e) { /* warn-only */ }
    return out
}
```

**函数命名**: `calc_{attrCode}`,要求 attrCode 是合法 Java 标识符子集(由 `MonitorContentCreateRequest.code` 的 Pattern 校验保证)。

**缓存策略**: 用 `ConcurrentHashMap<String, String>`,key 为 `monitorTypeId + ":" + SHA-256(所有 calc_script 拼接)`,内容变化自动失效。

### 5.10 `GroovyScriptEngine.executeComputed` (新增入口)

**不改动**现有 `execute(strategy, topic, message)`。

```java
/**
 * 执行合并后的计算属性脚本。
 * @param scriptCode  ComputedScriptAssembler.assemble() 产物
 * @param curData     当前精简消息 Map
 * @param prevData    上一条精简消息 Map(可 null)
 * @return 计算结果 Map<attrCode, Object>;失败时返回空 Map
 */
public Map<String, Object> executeComputed(String scriptCode,
                                            Map<String, Object> curData,
                                            Map<String, Object> prevData) {
    Future<Map<String, Object>> future = executor.submit(() -> {
        try {
            GroovyShell shell = new GroovyShell(createSecureConfig());
            Binding binding = new Binding();
            binding.setVariable("builtin", builtInFunctions);
            Script script = shell.parse(scriptCode);
            script.setBinding(binding);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) script.invokeMethod(
                    "compute", new Object[]{curData, prevData});
            return result == null ? Map.of() : result;
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

**为什么单独入口**:
- `execute` 接收 `DataParseStrategy` + topic + bytes,语义完全不同
- 缓存 key 不同(strategyId vs monitorTypeId+sha)
- 调用约定不同(parse 函数 vs compute 函数)
- 共享沙箱/超时/线程池 — 不需要新 executor

### 5.11 `ComputedAttributeEvaluator` (主入口)

```java
@Service
@Slf4j
public class ComputedAttributeEvaluator {
    private final IDeviceSensorQueryService sensorQuery;
    private final ComputedAttributeRegistry registry;
    private final ComputedScriptAssembler assembler;
    private final LastMessageStore lastMessageStore;
    private final GroovyScriptEngine scriptEngine;

    public List<PropertyValue> evaluate(Long deviceId, String sensorCode,
                                         ParsedMessage message) {
        try {
            // 1. 通过 sensorCode 拿 monitorTypeId
            SensorMetadata meta = sensorQuery.requireSensorMetadata(deviceId, sensorCode);
            Long monitorTypeId = meta.monitorTypeId();
            if (monitorTypeId == null) return List.of();

            // 2. 取该监测类型的计算属性列表(fast path)
            List<ComputedAttribute> attrs = registry.getByMonitorTypeId(monitorTypeId);
            if (attrs.isEmpty()) return List.of();

            // 3. 读 prevData
            ParsedMessageSnapshot prev = lastMessageStore.get(deviceId, sensorCode);

            // 4. 构建 curData (Map 形式)
            Map<String, Object> curData = buildCurData(message);
            Map<String, Object> prevData = (prev == null) ? null : buildPrevData(prev);

            // 5. 拼装脚本(带缓存)
            String script = assembler.assemble(attrs);

            // 6. 执行
            Map<String, Object> results = scriptEngine.executeComputed(script, curData, prevData);

            // 7. 转 PropertyValue
            List<PropertyValue> computedProps = new ArrayList<>();
            for (ComputedAttribute attr : attrs) {
                Object val = results.get(attr.code());
                if (val == null) continue;
                Double dv = toDouble(val);
                if (dv == null) continue;
                computedProps.add(new PropertyValue(
                        attr.code(), attr.name(), attr.unit(), dv, 0));
            }

            // 8. 总是写回 prevData(含固有属性 + 成功计算的计算属性)
            //    只要进入求值阶段就更新,保证下次脚本看到的 prevData 是当前条,
            //    避免因计算失败导致 prevData 永远停留在更旧的一条
            Map<String, Double> merged = new HashMap<>(buildPropertyMap(message));
            computedProps.forEach(p -> merged.put(p.identifier(), p.value()));
            lastMessageStore.put(deviceId, sensorCode,
                    new ParsedMessageSnapshot(message.deviceCode(), message.sensorCode(),
                            message.dataTime(), merged));

            return computedProps;
        } catch (Exception e) {
            log.warn("ComputedAttributeEvaluator failed: deviceId={}, sensorCode={}",
                    deviceId, sensorCode, e);
            return List.of();
        }
    }

    private Map<String, Object> buildCurData(ParsedMessage msg) {
        Map<String, Object> props = new LinkedHashMap<>();
        for (var p : msg.properties()) {
            if (p.value() != null) props.put(p.identifier(), p.value());
        }
        return Map.of(
                "deviceCode", msg.deviceCode(),
                "sensorCode", msg.sensorCode(),
                "dataTime", msg.dataTime(),
                "properties", props);
    }

    private Map<String, Object> buildPrevData(ParsedMessageSnapshot snap) {
        return Map.of(
                "deviceCode", snap.deviceCode(),
                "sensorCode", snap.sensorCode(),
                "dataTime", snap.dataTime(),
                "properties", snap.properties());
    }
}
```

### 5.12 `MonitorIngestFacade.ingest()` 修改

在 ④ `enrichProperties` 之后、⑤ `enqueue` 之前插入:

```java
// 4.5 Computed attributes evaluation
try {
    List<PropertyValue> computed = computedAttrEvaluator.evaluate(
            deviceId, parsedMessage.sensorCode(), parsedMessage);
    if (!computed.isEmpty()) {
        List<PropertyValue> merged = new ArrayList<>(parsedMessage.properties());
        merged.addAll(computed);
        parsedMessage = new ParsedMessage(
                parsedMessage.deviceCode(), parsedMessage.sensorCode(),
                parsedMessage.sourceType(), parsedMessage.dataTime(),
                parsedMessage.receiveTime(), parsedMessage.payloadHash(), merged);
    }
} catch (Exception e) {
    log.warn("Computed attribute evaluation failed, skip: deviceId={}, sensorCode={}",
            deviceId, parsedMessage.sensorCode(), e);
}
```

构造器追加注入 `ComputedAttributeEvaluator`。

### 5.13 在线测试端点

`MonitorContentController` 新增:

```java
@PreAuthorize("@ss.hasPermi('basic:monitorContent:test')")
@PostMapping("/test-script")
public AjaxResult testScript(@Validated @RequestBody CalcScriptTestRequest request) {
    // 在同监测类型下,以 request.calcScript 替换目标属性的脚本,与其他计算属性一起拼装执行
    // 返回 { success, result, error, executionTime }
}
```

`CalcScriptTestRequest` / `CalcScriptTestResult` DTO 见前端 4.1 节。

### 5.14 权限注册

`sys_menu` 表插入新权限:

```sql
-- 在 monitorContent 父菜单下注册 test 权限
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, perms, menu_type, ...)
VALUES ('监测内容脚本测试', {monitorContent_parent_id}, 5, '', '', 'basic:monitorContent:test', 'F', ...);
```

具体 `parent_id` 在实现时通过 `SELECT menu_id FROM sys_menu WHERE perms = 'basic:monitorContent:list'` 查到。

## 6. 前端 UI 实现

### 6.1 `monitorType.ts` API 扩展

```typescript
export interface MonitorContentItem {
  // ... 现有字段
  fieldType?: 'inherent' | 'computed'
  calcScript?: string
}

export interface MonitorContentCreatePayload {
  // ... 现有字段
  fieldType?: 'inherent' | 'computed'
  calcScript?: string
}

export interface MonitorContentUpdatePayload {
  // ... 现有字段
  calcScript?: string
}

export interface CalcScriptTestRequest {
  monitorTypeId: number
  attrCode: string
  calcScript: string
  curData: Record<string, any>
  prevData?: Record<string, any>
}

export interface CalcScriptTestResult {
  success: boolean
  result?: Record<string, any>
  error?: string
  executionTime?: number
}

export const testCalcScript = (payload: CalcScriptTestRequest) =>
  unwrap<CalcScriptTestResult>(
    request.post('/monitor-contents/test-script', payload)
  )
```

### 6.2 `MonitorType.vue` 表格列扩展

在"指标类型"列后插入"字段类型"列:

```vue
<el-table-column label="字段类型" width="110" align="center">
  <template #default="{ row }">
    <template v-if="isView">
      <el-tag v-if="row.fieldType === 'computed'" type="warning" size="small">计算属性</el-tag>
      <el-tag v-else type="info" size="small">固有属性</el-tag>
    </template>
    <el-select
      v-else
      v-model="row.fieldType"
      :disabled="Boolean(row.id)"
      @change="handleFieldTypeChange(row)"
      placeholder="请选择"
    >
      <el-option label="固有属性" value="inherent" />
      <el-option label="计算属性" value="computed" />
    </el-select>
  </template>
</el-table-column>
```

`handleFieldTypeChange`: 切换为 `computed` 时若 calcScript 为空,自动填入默认模板(占位,提示用户去脚本编辑器完善)。

### 6.3 操作列扩展

操作列宽度 80 → 140,加"脚本"按钮(仅 `fieldType === 'computed'` 显示):

```vue
<el-table-column label="操作" width="140" fixed="right" align="center" v-if="!isView">
  <template #default="{ row, $index }">
    <div class="op-cell">
      <el-button
        v-if="row.fieldType === 'computed'"
        type="primary" text size="small"
        @click="handleEditScript(row, $index)"
      >脚本</el-button>
      <el-button type="text" size="small" class="danger-text"
        @click="handleRemoveModelAttr($index)"
      >删除</el-button>
    </div>
  </template>
</el-table-column>
```

### 6.4 `CalcScriptEditor.vue` 新组件

**布局**:

```
┌────────────────────────────────────────────────────────────┐
│  计算脚本 - {attrName}                            [X]        │
├────────────────────────────────────────────────────────────┤
│  脚本编辑区 (el-input textarea, 14 行,等宽字体)              │
│                                                            │
│  ─── 在线测试 (折叠面板, 默认展开) ───────────────────────── │
│  curData (JSON 文本框)                                     │
│  prevData (JSON 文本框, 可选)                               │
│  [测试运行]                                                 │
│                                                            │
│  测试结果: ✅ 成功   耗时: 3ms                              │
│  返回值: 2.3                                                │
├────────────────────────────────────────────────────────────┤
│                              [取消] [清空] [确定]            │
└────────────────────────────────────────────────────────────┘
```

**Props / Emits**:

```typescript
props: {
  modelValue: boolean
  attrCode: string
  attrName: string
  script: string
  unit?: string
  monitorTypeId: number
}
emits: {
  'update:modelValue': (v: boolean) => void
  'update:script': (v: string) => void
}
```

**编辑器实现**: `el-input type="textarea" :rows="14"` + 等宽字体(复用 `.code-textarea` 样式,与 `DataParseForm.vue` 一致)。不引入 CodeMirror/Monaco。

**模板默认值**(切换为计算属性时自动填入):

```groovy
// 计算属性: {attrCode}
// 可用变量:
//   curData.properties.{attrCode}  当前固有属性值
//   prevData?.properties.{attrCode}  上一条数据包属性值
// 返回: 计算结果 (Number)

return curData.properties.{attrCode}
```

### 6.5 校验逻辑调整

`validateModelAttrs()` 追加:

```typescript
if (!row.fieldType) {
  ElMessage.warning(`第 ${index + 1} 行字段类型不能为空`)
  return false
}
if (row.fieldType === 'computed' && !row.calcScript?.trim()) {
  ElMessage.warning(`第 ${index + 1} 行(${row.name || row.code})为计算属性,必须设置计算脚本`)
  return false
}
```

### 6.6 `syncMonitorContents` 同步逻辑

- Create payload 追加 `fieldType` + `calcScript`
- Update payload 追加 `calcScript`(fieldType 由前端禁用保证不可改)
- 判断是否需要 update:追加 `oldItem.calcScript !== item.calcScript`

## 7. 错误处理矩阵

| 错误场景 | 处理策略 | 影响 |
|---|---|---|
| `monitor_type_id` 解析失败(sensor 元数据缺失) | warn 日志 + 返回空 list | 主链路继续,无计算属性 |
| `ComputedAttributeRegistry` 查询失败(DB 异常) | warn + 返回空 list | 主链路继续 |
| `LastMessageStore.get` 失败(Redis 异常) | warn + prevData=null | 脚本里 prevData 为 null,逻辑需 nullable |
| `LastMessageStore.put` 失败 | warn 日志,不影响主链路 | 下次 prevData 还是更旧的一条 |
| `scriptEngine.executeComputed` 超时(>30s) | warn + 返回空 Map | 所有计算属性丢弃,主链路继续 |
| 单个计算属性脚本异常 | assembler 内 try-catch,跳过该属性 | 其他属性正常 |
| 计算属性返回非数值 | `toDouble` 转换失败 → null → 跳过 | 该属性不入 properties |
| MonitorIngestFacade 外层兜底 | 不允许任何异常上抛 | 数据接入可用性优先 |

**核心契约**: 计算属性求值的任何失败**绝不影响数据接入主链路**。失败永远只是"丢掉计算属性",绝不"丢掉原始数据"。

## 8. 可观察性

### 8.1 日志

所有失败点 `log.warn(...)` + 必要上下文:

```java
log.warn("Computed attribute calc failed: monitorTypeId={}, attrCode={}, deviceId={}, error={}",
        monitorTypeId, attrCode, deviceId, e.getMessage());
```

避免 `log.error` 防止告警风暴(失败是预期的可恢复场景)。

### 8.2 Metrics

本次不引入 Micrometer 额外埋点。为未来扩展预留:
- `monitor.computed.eval.duration` (Timer)
- `monitor.computed.eval.failures` (Counter, by attrCode)

### 8.3 数据库表

不在 `iot_data_parse_log`(parse 阶段日志)中记录计算属性失败。失败是临时的,日志可追溯即可。

## 9. 安全

### 9.1 沙箱继承

`executeComputed` 直接复用 `GroovyScriptEngine.createSecureConfig()` 的 `SecureASTCustomizer`:

- 禁所有 wildcard imports
- 禁危险 imports(System/Runtime/ProcessBuilder/Thread/Class/ClassLoader/java.io.*/java.net.*/java.lang.reflect.*/...)
- 禁危险 receivers(System/Runtime/ProcessBuilder/Class/Thread/File/GroovyShell/GroovyClassLoader/Script/Closure/InvokerHelper)
- 禁静态 imports

`curData`/`prevData` 是 `LinkedHashMap`,本身不在黑名单,正常可访问;已有的 receiver 限制覆盖所有已知逃逸路径。

### 9.2 输入校验

- `calcScript` 长度: 后端 `@Size(max = 65535)`
- `attrCode` 必须匹配 `^[a-zA-Z][a-zA-Z0-9_]*$`(校验函数命名安全),由 `MonitorContentCreateRequest.code` 的 Pattern 保证
- 脚本预编译校验: 保存时通过 `GroovyScriptValidator.validate()` 预编译,失败直接拒绝写入

### 9.3 权限

| 操作 | 权限 |
|---|---|
| 新增/修改 calcScript | 复用 `basic:monitorContent:add` / `basic:monitorContent:edit` |
| 测试脚本 | 新增 `basic:monitorContent:test` |

## 10. 测试策略

### 10.1 后端单测(P0)

| 测试类 | 用例数 | 验证点 |
|---|---|---|
| `ComputedScriptAssemblerTest` | 5 | 函数拼接正确、attrCode 非法抛异常、空列表产出空脚本、sort_order 排序、缓存命中 |
| `LastMessageStoreTest` | 4 | 正常读写、TTL 过期、Redis 异常返回 null、序列化失败不抛 |
| `ComputedAttributeEvaluatorTest` | 8 | 无计算属性 fast path、首次上报(prevData=null)、脚本成功合并 properties、单属性失败跳过、全部失败返回空、monitorTypeId 缺失、prevData 写回、脚本超时 |
| `GroovyScriptEngineComputedTest` | 4 | executeComputed 正常执行、沙箱拒绝危险代码、超时返回空 Map、curData/prevData 注入正确 |

### 10.2 后端集成测试(P1)

- `MonitorIngestFacadeIT`: 端到端 — MQTT 消息 → 含计算属性的 ParsedMessage 入队 → Stream 消费者收到包含计算属性的 properties
- `MonitorContentControllerIT`: CRUD + test-script 端点

### 10.3 前端(P2)

暂无单测框架,通过手动测试覆盖:

1. 创建固有属性 → 不显示脚本按钮
2. 创建计算属性 → 显示脚本按钮,默认模板填入
3. 编辑计算属性 → fieldType 禁用,脚本可改
4. 在线测试 → 输入 curData + prevData → 看到结果
5. 校验拦截 → 计算属性无脚本时不允许保存

### 10.4 回归测试

- 存量 12 条 `monitor_content` 自动 `field_type=inherent`,行为零变化
- `IoTDB` schema 创建路径不变,计算属性也会创建对应 `sensor_attribute` 条目
- TSL 模型不变,ProductTsl 重建时计算属性与固有属性同结构(只是来源不同)

## 11. 边界与依赖

### 11.1 `sensor_attribute` 与计算属性的关系

`DeviceSensorServiceImpl.insertSensor` 的实际语义是:**用户在前端创建传感器时主动勾选监测内容**(`attrList[i].monitorContentId`),系统按勾选调用 `populateFromContent(attrList)` 回填属性详情,生成 `sensor_attribute` 条目并预建 IoTDB schema。计算属性与固有属性在该流程中无差异对待 — 用户勾选了它,就会自动生成条目;没勾选则不生成。这意味着:

- 若用户在传感器创建时勾选了计算属性,IoTDB 路径 `root.zwei.d{deviceId}.s{sensorNo}.{attrCode}` 会在传感器创建时预建,ProductTsl 包含该属性
- 若用户**没勾选**该计算属性,计算求值仍会产出 `PropertyValue`,但 Consumer 在 IoTDB 写入时若该路径不存在会触发懒建(`IotdbTimeSeriesService` 已有 aligned timeseries 懒建机制,无需额外处理)
- 前端传感器创建页面无需改动 — 计算属性会作为可选项出现在监测内容选择列表中(后续可在 UI 上加"计算属性"标记区分,本次不强制)
- ProductTsl 包含计算属性(仅当用户勾选时;与固有属性同结构,只是来源不同)
- **TSL 不感知 fieldType/calcScript**(保持 TSL 模型纯净)

### 11.2 计算属性参与告警

`AlarmEvaluationEngine` 通过 `MonitorDataIngestedEvent` 消费 `parsedMessage.properties`(含计算属性),所以计算属性可被告警判据引用 — 这是**设计内的免费红利**,无需额外工作。

### 11.3 监测类型层级 vs 设备层级

计算脚本定义在监测类型层级(`monitor_content`),意味着同一监测类型下所有设备的所有传感器共用同一份脚本。如果未来需要设备级差异,扩展方案 A 加一个 `monitor_content_device_override` 表即可 — 不在本次范围。

### 11.4 prevData 时序保证

`MonitorIngestFacade` 在单线程 MQTT listener 中执行,但同一设备的多条消息可能并发到达(不同传感器)。`LastMessageStore` 按 `deviceId + sensorCode` 粒度缓存,保证每个传感器有独立的 prevData 序列。

**不保证强一致**: 如果同一传感器的两条消息几乎同时到达,可能读到稍旧的 prevData。这是可接受的(计算属性是派生指标,不需要严格时序)。

## 12. 实施计划(高层)

详细实施计划由后续 `writing-plans` 阶段产出。高层步骤:

1. DB 升级脚本 + MonitorContent 实体/DTO/Mapper/Service 扩展
2. 后端新组件(Registry + Evaluator + Assembler + LastMessageStore + Snapshot)
3. GroovyScriptEngine.executeComputed + MonitorIngestFacade 接入
4. CalcScriptTestRequest/Result + test-script 端点 + 权限注册
5. 后端单测 + 集成测试
6. 前端 API + MonitorType.vue 改造
7. CalcScriptEditor.vue 新组件
8. 前端手动测试 + 回归

## 13. 开放问题

无。所有关键决策已确认。
