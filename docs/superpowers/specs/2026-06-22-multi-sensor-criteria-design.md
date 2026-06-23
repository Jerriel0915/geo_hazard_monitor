# 告警判据多传感器支持设计

> 修复前端"多传感器下选中一个属性后所有传感器同名属性都被选中"的 bug；定义新的判据 subject 路径格式以支持传感器隔离与 prev（上一值）语义；打通 LastMessageStore 与告警评估引擎的数据通路。

## 一、概述

### 1.1 背景

当前判据配置存在两个问题：

**问题 1（前端 bug）**：传感器模式下，选中某个传感器的属性后，UI 上其他传感器下的同名属性也会被高亮选中。

根因在 `web/src/views/alarm/composables/useIndicatorTree.ts` 第 76-89 行 `prefixDisplayLabels` 函数：传感器模式下只修改叶子节点的 `displayLabel`，**没有修改 `value`**。结果不同传感器下相同 attrCode 的叶子节点 `value` 完全相同（都是 `payload.current.waterLevel`），`buildNodeMap` 用 value 做 key 导致后者覆盖前者；`el-tree-select` 用 `node-key=value` 寻找节点，选中任何一个都会让所有同名节点被高亮。

**问题 2（契约缺失）**：判据 subject 当前的格式无法区分传感器，也不支持 prev 语义：

- 监测类型模式：`payload.current.waterLevel` / `payload.previous.waterLevel`
- 传感器模式（理论值）：`sensor_123.payload.current.waterLevel`（但因 bug 实际丢掉了 sensorId 前缀）

后端 `CriteriaEvaluator.normalizeSubject` 只识别 `payload.current.` / `payload.` 前缀，剥离后剩 attrCode，**无法支持**：
- 多传感器同名属性隔离
- prev（上一值）语义
- 跨数据维度（device/packet）扩展

### 1.2 目标

1. **修复前端 bug**：`prefixDisplayLabels` 同步修改 value，保证传感器模式下叶子节点 value 唯一
2. **定义新 subject 路径格式**：`[sensorCode.] {kind}.{dimension}.{attrCode}`，kind ∈ {current, prev}，dimension ∈ {payload, device, packet}
3. **打通 prev 数据通路**：`MonitorDataIngestedEvent` 携带 `prevSnapshot`，由 `MonitorIngestConsumerService` 在发布事件前从 `LastMessageStore` 获取
4. **store 写入时机迁移**：从 ingest 阶段（`ComputedAttributeEvaluator`）后移到 consume 阶段（`MonitorIngestConsumerService`），保证 AlarmEvaluationEngine 接到事件时 store 已推进到当前条
5. **AlarmEvaluationEngine 评估流程改造**：构建双 key（传感器模式 + 监测类型模式）的 subjectValues
6. **不兼容老格式**：清空 `alarm_criteria.level_config` 历史数据，用户重新配置等级条件

### 1.3 范围外

- **跨传感器判据**：判据 subject 引用其他 sensorCode（非本次上报的 sensorCode）暂不支持。判据 subject 的 sensorCode 段必须与本次事件的 sensorCode 一致，否则 subjectValues 中找不到对应 key，判据不触发。跨传感器综合判据请用 Groovy 综合策略（`alarm_strategy`）实现。
- `ParsedMessageSnapshot` 扩展 quality 字段：packet.quality 暂不暴露给前端
- `alarm_criteria` 表结构变更：无新增列（subject 格式由 level_config JSON 内容决定）
- 前端判据条件构建器 UI 重设计：仅重组数据结构，组件交互保持不变

## 二、Subject 路径格式

### 2.1 四层结构

```
[sensorCode.] {kind} {dimension} {attrCode}
```

| 模式 | 格式 | 示例 |
|---|---|---|
| 传感器模式 | `sensorCode.{kind}.{dimension}.{attrCode}` | `DEV001.current.payload.water_level` |
| 监测类型模式 | `{kind}.{dimension}.{attrCode}` | `current.payload.water_level` |

**字段语义**：

- `{kind}` ∈ `{current, prev}` — 时间维度
  - `current` = 本次报文
  - `prev` = 上一条已完整处理的报文（来自 LastMessageStore）
- `{dimension}` ∈ `{payload, device, packet}` — 数据维度
  - `payload` — 传感器业务属性（water_level、rainfall_hour 等，来自报文 properties）
  - `device` — 设备基础信息（onlineStatus、lastReportTime，**无视 kind** 直接查 device 表，因为基本信息不会变化）
  - `packet` — 数据包元信息（仅 `dataTime`；packet.quality 暂不支持）
- `{attrCode}` — 具体属性编码（来自 monitor_content.code 或 device 表字段）

### 2.2 段数识别规则

后端 `CriteriaEvaluator.normalizeSubject(String subject)` 按 `.` 分割后按段数识别：

- **4 段** `a.b.c.d` → 传感器模式
  - a = sensorCode（不校验内容，由调用方保证）
  - b ∈ {current, prev}
  - c ∈ {payload, device, packet}
  - d = attrCode
- **3 段** `b.c.d` → 监测类型模式
  - b ∈ {current, prev}
  - c ∈ {payload, device, packet}
  - d = attrCode
- 其他段数 → 返回 null（日志 warn，判据不触发）

### 2.3 device / packet 维度的特殊语义

**device 维度**：
- `sensorCode.current.device.X` 与 `sensorCode.prev.device.X` **返回相同值**（查同一张 device 表）
- 前端在 current 和 prev 分支下都展示 device 子节点（格式对称），但后端解析时无视 kind
- `device.onlineStatus` → 查 `device_online_status` 表（或 IDeviceOnlineStatusService）
- `device.lastReportTime` → 查 `device.last_report_time` 字段（或 IDeviceQueryService）

**packet 维度**：
- `current.packet.dataTime` = 本次报文的 dataTime（从 `MonitorDataIngestedEvent` 透传）
- `prev.packet.dataTime` = `prevSnapshot.dataTime`
- `packet.quality` 暂不支持（`ParsedMessageSnapshot.properties` 只存 value 不存 quality；前端树不展示该节点）

## 三、事件契约改造

### 3.1 MonitorDataIngestedEvent 新增字段

```java
public class MonitorDataIngestedEvent {
    // 已有字段（保留）
    private final Long deviceId;
    private final Long sensorId;
    private final String deviceCode;
    private final String sensorCode;
    private final String sourceType;
    private final long receiveTime;
    private final String payloadHash;
    private final List<PropertyValue> properties;

    // 新增
    private final ParsedMessageSnapshot prevSnapshot;  // 可为 null
}
```

`ParsedMessageSnapshot`（已存在于 `zwei-iot-timeseries/compute/`）字段：
- `deviceCode` / `sensorCode` / `dataTime` — 用于 `*.packet.dataTime`
- `properties: Map<String, Object>` — 用于 `*.prev.payload.{attrCode}`

**注意**：`ParsedMessageSnapshot` 当前位于 `zwei-iot-timeseries` 模块，而 `MonitorDataIngestedEvent` 在 `zwei-common`。`zwei-common` 不能依赖 `zwei-iot-timeseries`。需要把 `ParsedMessageSnapshot` **下沉到 `zwei-common`**（作为共享契约），或新建 `zwei-common/domain/ParsedMessageSnapshot.java` 并在 timeseries 模块 re-export 或删除原类。

**推荐方案**：把 `ParsedMessageSnapshot` 从 `zwei-iot-timeseries/compute/` 移动到 `zwei-common/domain/`，包名改为 `com.zwei.common.domain.ParsedMessageSnapshot`。timeseries 模块的引用同步更新。

### 3.2 prevSnapshot = null 的场景

- 首次上报（LastMessageStore 无记录）
- 已过期（TTL 7 天之外）
- Redis 异常（LastMessageStore.get 失败返回 null）
- STANDARD_POINT 路径（已无生产调用方，不维护 prev）

`prevSnapshot = null` 时，判据中所有 `*.prev.*.*` 的 subject 解析到的值为 null，`evaluateCondition` 返回 false，判据不触发。

## 四、Store 写入时机迁移

### 4.1 现状

`ComputedAttributeEvaluator.evaluate()`（ingest 阶段，MonitorIngestFacade 第 92-107 行）：
1. `prev = lastMessageStore.get(deviceId, sensorCode)` — 读上一条作为计算属性脚本的 prevData
2. 执行 Groovy 脚本计算属性
3. `lastMessageStore.put(deviceId, sensorCode, currentSnapshot)` — **写入当前条**（包含原始 + 计算属性）

### 4.2 问题

当 `AlarmEvaluationEngine` 接到 `MonitorDataIngestedEvent`（consume 阶段后）时：
- LastMessageStore 已被 ingest 阶段的 put 更新为当前条
- 无法获取真正的 prev（上一条）

### 4.3 改造后

**ingest 阶段**（`ComputedAttributeEvaluator`）：
- 保留 get（读 prev 给计算属性脚本用）
- **删除 put**（第 87-95 行）

**consume 阶段**（`MonitorIngestConsumerService.processParsedMessage()`）：
在 IoTDB 写入成功 + 运维指标回写后，**发布事件之前**：

```java
// 1. 读 prev（此时 store 里还是 ingest 之前的上一条）
ParsedMessageSnapshot prevSnapshot = lastMessageStore.get(deviceId, sensorCode);

// 2. 构造当前 snapshot（msg.properties 已包含计算属性，ingest 阶段已 merge）
Map<String, Object> currentProps = new LinkedHashMap<>();
for (PropertyValue pv : msg.properties()) {
    if (pv.value() != null) currentProps.put(pv.identifier(), pv.value());
}
ParsedMessageSnapshot currentSnapshot = new ParsedMessageSnapshot(
    msg.deviceCode(), msg.sensorCode(), msg.dataTime(), currentProps);

// 3. 推进 store：当前条覆盖上一条
lastMessageStore.put(deviceId, sensorCode, currentSnapshot);

// 4. 发布携带 prev 的事件
publishIngestedEvent(deviceId, sensorId, msg, prevSnapshot);
```

### 4.4 语义保证

- LastMessageStore 始终存储"上一条已完整处理的报文"
- ingest 阶段读、不写
- consume 阶段读一次（作为事件携带的 prevSnapshot）→ put 当前条 → 发布事件
- consume 失败重试时：dedupe 拦截重复消息；若消息重试，get 拿到的还是同一 prev，put 的还是同一 current，语义一致

### 4.5 STANDARD_POINT 路径

包装 `ParsedMessage` 时 `prevSnapshot` 传 null（该路径已无生产调用方，不引入额外 get/put 复杂度）。

## 五、AlarmEvaluationEngine 评估流程

### 5.1 双 key 写入策略

事件无法预知判据是传感器模式还是监测类型模式，所以构建 subjectValues 时**两个 key 都写入**：

```java
Map<String, Double> subjectValues = new HashMap<>();

// 1. 本 sensorCode 的 current payload
for (PropertyValue pv : event.getProperties()) {
    if (pv.value() instanceof Number n) {
        double v = n.doubleValue();
        subjectValues.put(event.getSensorCode() + ".current.payload." + pv.identifier(), v);
        subjectValues.put("current.payload." + pv.identifier(), v);
    }
}

// 2. 本 sensorCode 的 prev payload
if (event.getPrevSnapshot() != null) {
    for (Map.Entry<String, Object> e : event.getPrevSnapshot().properties().entrySet()) {
        if (e.getValue() instanceof Number n) {
            double v = n.doubleValue();
            subjectValues.put(event.getSensorCode() + ".prev.payload." + e.getKey(), v);
            subjectValues.put("prev.payload." + e.getKey(), v);
        }
    }
}

// 3. packet.dataTime
long currentDataTime = resolveCurrentDataTime(event);  // 从事件或 msg 透传
subjectValues.put(event.getSensorCode() + ".current.packet.dataTime", (double) currentDataTime);
subjectValues.put("current.packet.dataTime", (double) currentDataTime);
if (event.getPrevSnapshot() != null) {
    subjectValues.put(event.getSensorCode() + ".prev.packet.dataTime", (double) event.getPrevSnapshot().dataTime());
    subjectValues.put("prev.packet.dataTime", (double) event.getPrevSnapshot().dataTime());
}

// 4. device.* — 无视 kind，查 device 表
Device dev = deviceQueryService.getById(event.getDeviceId());
if (dev != null) {
    double online = dev.isOnline() ? 1.0 : 0.0;
    double lastReport = dev.getLastReportTime() != null ? parseToEpochSeconds(dev.getLastReportTime()) : 0.0;
    for (String kind : List.of("current", "prev")) {
        subjectValues.put(event.getSensorCode() + "." + kind + ".device.onlineStatus", online);
        subjectValues.put(event.getSensorCode() + "." + kind + ".device.lastReportTime", lastReport);
        subjectValues.put(kind + ".device.onlineStatus", online);
        subjectValues.put(kind + ".device.lastReportTime", lastReport);
    }
}
```

### 5.2 currentDataTime 的来源

当前 `MonitorDataIngestedEvent` 只有 `receiveTime`（MQTT 接收时间），没有报文业务 dataTime。需要从 `ParsedMessage.dataTime()` 透传：

- `MonitorDataIngestedEvent` 新增 `dataTime: long` 字段（可选，或在现有字段里复用）
- 或从 `properties` 里反查（如果有特殊属性标识 dataTime）

**推荐方案**：`MonitorDataIngestedEvent` 新增 `dataTime: long` 字段，consume 阶段从 `msg.dataTime()` 透传。

### 5.3 device 查询接口扩展

`IDeviceQueryService` 现有方法只有 `getDeviceBriefByAuthUsername(String)`，没有按 deviceId 查的方法。需要扩展：

```java
// zwei-iot-device/service/IDeviceQueryService.java 新增
/**
 * 按设备 ID 查询设备基础信息（供告警引擎解析 device 维度 subject）。
 * 返回 null 表示设备不存在或已删除。
 */
DeviceBasicInfo getBasicInfoById(Long deviceId);
```

`DeviceBasicInfo` 是新增 DTO（或直接复用 `Device` 实体的子集），含字段：
- `online: boolean` — 来自 `device_online_status.status`
- `lastReportAt: Long` — epoch seconds，来自 `device.last_report_time`
- `status: int` — 设备业务状态（1-正常 / 2-维修 / 3-停用）

实现 `DeviceQueryServiceImpl.getBasicInfoById`：
- 查 `device` 表（现有 `DeviceMapper.selectDeviceById`）
- 查 `device_online_status` 表（现有 `DeviceOnlineStatusService`）

**不直接注入 `DeviceMapper`**（跨模块规则要求只通过 Service 接口），AlarmEvaluationEngine 只依赖 `IDeviceQueryService`。

**关于 `device.lastReportTime` 格式**：现有字段是 `String` 类型（如 "2026-06-22 10:30:00"），subjectValues 是 `Map<String, Double>`，需要把时间字符串转为 epoch seconds（或 epoch millis）。转换工具复用 `com.zwei.common.utils.DateUtils.parsePatterns`。

## 六、CriteriaEvaluator 改造

### 6.1 normalizeSubject 重写

```java
private static final Set<String> KINDS = Set.of("current", "prev");
private static final Set<String> DIMENSIONS = Set.of("payload", "device", "packet");

String normalizeSubject(String subject) {
    if (subject == null) return null;
    String s = subject.trim();
    String[] parts = s.split("\\.");
    if (parts.length == 4) {
        // sensorCode.kind.dimension.attrCode
        if (!KINDS.contains(parts[1])) return null;
        if (!DIMENSIONS.contains(parts[2])) return null;
        return s;
    } else if (parts.length == 3) {
        // kind.dimension.attrCode
        if (!KINDS.contains(parts[0])) return null;
        if (!DIMENSIONS.contains(parts[1])) return null;
        return s;
    }
    log.warn("[Alarm][Criteria] 不支持的 subject 格式: {} (段数={})", subject, parts.length);
    return null;
}
```

**关键变化**：
- 不再剥离前缀
- 直接返回原 subject 作为 subjectValues 的 key（双 key 已在引擎端写入）
- 段数 + 枚举值校验

### 6.2 resolveSubjectValue

```java
Double resolveSubjectValue(LevelCondition cond, Map<String, Double> subjectValues) {
    if (cond == null || cond.getSubject() == null) return null;
    String key = normalizeSubject(cond.getSubject());
    if (key == null) return null;
    Double value = subjectValues.get(key);
    if (value == null) {
        log.debug("[Alarm][Criteria][Subject] 未找到对应值 subject={} available={}", key, subjectValues.keySet());
    }
    return value;
}
```

### 6.3 老格式不兼容

`normalizeSubject` 不再识别 `payload.current.x` / `payload.x`。老判据 subject 直接被段数校验拒绝（4 段以下返回 null），判据不触发。配合数据库迁移（第七节）清空 level_config。

## 七、前端改造

### 7.1 IndicatorTree 结构重组

`web/src/views/alarm/composables/useIndicatorTree.ts`：

**现状**（监测类型模式）：
```
├── device (disabled)
│   ├── onlineStatus    value="device.onlineStatus"
│   └── lastReportTime  value="device.lastReportTime"
├── packet (disabled)
│   ├── dataTime        value="packet.dataTime"
│   └── quality         value="packet.quality"
└── payload (disabled)
    ├── current (disabled)
    │   └── {attrCode}  value="payload.current.{attrCode}"
    └── previous (disabled)
        └── {attrCode}  value="payload.previous.{attrCode}"
```

**改造后**（监测类型模式）：
```
├── current (disabled)
│   ├── payload (disabled)
│   │   └── {attrCode}  value="current.payload.{attrCode}"
│   ├── device (disabled)
│   │   ├── onlineStatus    value="current.device.onlineStatus"
│   │   └── lastReportTime  value="current.device.lastReportTime"
│   └── packet (disabled)
│       └── dataTime        value="current.packet.dataTime"
└── prev (disabled)
    ├── payload (disabled)
    │   └── {attrCode}  value="prev.payload.{attrCode}"
    ├── device (disabled)
    │   ├── onlineStatus    value="prev.device.onlineStatus"
    │   └── lastReportTime  value="prev.device.lastReportTime"
    └── packet (disabled)
        └── dataTime        value="prev.packet.dataTime"
```

**传感器模式**：每个 sensor 作为根节点（disabled），children 为上述结构，所有叶子 value 加 `{sensorCode}.` 前缀。

### 7.2 prefixDisplayLabels bug 修复

```typescript
function prefixDisplayLabels(nodes: IndicatorTreeNode[], prefix: string): IndicatorTreeNode[] {
  return nodes.map(n => {
    const copy: IndicatorTreeNode = {...n}
    if (!n.disabled) {
      // 可选叶子：displayLabel 和 value 都加前缀
      copy.displayLabel = `${prefix}.${n.label}`
      copy.value = `${prefix}.${n.value}`   // ← 新增（bug 修复）
    } else if (n.children) {
      copy.children = prefixDisplayLabels(n.children, prefix)
    }
    return copy
  })
}
```

### 7.3 buildFromSensors 入参调整

```typescript
// 现状
async function buildFromSensors(sensors: { sensorId: number; sensorName: string; monitorTypeId: number }[])

// 改造后
async function buildFromSensors(sensors: { sensorCode: string; sensorName: string; monitorTypeId: number }[])
```

**调用方确认**：全局只有 `AlarmCriteria.vue:295` 一处调用。改造点：

```typescript
// AlarmCriteria.vue 第 295-301 行
await buildFromSensors(
  device.sensors.map((s: any) => ({
    sensorCode: s.sensorCode,    // ← 替代 s.id || s.sensorId
    sensorName: s.monitorTypeName || s.name || s.sensorName || '',
    monitorTypeId: s.monitorTypeId,
  }))
)
```

**前置假设**：`device.sensors` 数组的元素含 `sensorCode` 字段。若 API 返回未包含该字段，需要扩展后端 `DeviceController` 的设备详情接口（返回 sensors 数组时带上 sensorCode）。实现时先验证前端 API 响应，若缺字段再补后端。

传感器根节点的 value 改为 `{sensorCode}`（替代 `sensor_${sensorId}`），与判据存储格式对齐。

### 7.4 packet.quality 移除

前端 IndicatorTree 的 packet 分支下**移除 quality 子节点**（保留 dataTime）。理由：`ParsedMessageSnapshot` 不存整体 quality；报文属性级 quality 语义在数据包级别模糊。

### 7.5 AlarmCriteria.vue migrateSubject 删除

`migrateSubject()` 函数（第 214-218 行）处理老格式迁移，改造后直接删除。保存的 level_config 里 subject 始终是新格式。

## 八、数据库迁移

### 8.1 清空历史判据 level_config

老判据的 subject 格式与新评估器不兼容。直接删除 level_config 让用户重新配置：

```sql
-- 清空所有启用判据的 level_config（保留判据元数据：名称、绑定关系等）
UPDATE alarm_criteria
SET level_config = NULL,
    version = version + 1,
    update_time = NOW(),
    update_by = 'system-migration'
WHERE del_flag = 0;
```

### 8.2 不执行物理 DELETE

保留判据元数据（name、hazard_point_id、persist_count、silence_period、is_enabled 等），用户在前端看到判据列表，仅 level_config 为空需要重新配置等级条件。

### 8.3 变更日志

迁移脚本本身不写 `alarm_criteria_log`（避免日志噪声）。用户重新配置后由 `AlarmCriteriaServiceImpl` 正常写入变更日志。

## 九、改动清单总览

### 前端（3 文件）

| 文件 | 改动 |
|---|---|
| `web/src/views/alarm/composables/useIndicatorTree.ts` | 重组树结构；`prefixDisplayLabels` 同步改 value；`buildFromSensors` 入参改 sensorCode；移除 packet.quality 节点 |
| `web/src/views/alarm/AlarmCriteria.vue` | 删除 migrateSubject 函数；调用 buildFromSensors 时传 sensorCode |
| 可能涉及 `CriteriaDetailPanel.vue` 等 | 若 buildFromSensors 调用方传 sensorId，改为传 sensorCode |

### 后端 — common 模块

| 文件 | 改动 |
|---|---|
| `zwei-common/domain/ParsedMessageSnapshot.java` | **新增**（从 timeseries 下沉，包名改 com.zwei.common.domain） |
| `zwei-common/event/MonitorDataIngestedEvent.java` | 新增 `prevSnapshot: ParsedMessageSnapshot` 和 `dataTime: long` 字段 |

### 后端 — timeseries 模块

| 文件 | 改动 |
|---|---|
| `compute/ParsedMessageSnapshot.java` | **删除**（已下沉到 common） |
| `compute/LastMessageStore.java` | import 改为 common 包 |
| `compute/ComputedAttributeEvaluator.java` | import 改为 common 包；**删除第 87-95 行 put** |
| `service/MonitorIngestConsumerService.java` | processParsedMessage 新增 get prev → put 当前 → 构造携带 prevSnapshot 的事件；publishIngestedEvent 签名扩展 |

### 后端 — device 模块

| 文件 | 改动 |
|---|---|
| `service/IDeviceQueryService.java` | 新增 `getBasicInfoById(Long deviceId)` 方法 |
| `service/impl/DeviceQueryServiceImpl.java` | 实现 getBasicInfoById：查 DeviceMapper + DeviceOnlineStatusService |
| `domain/dto/DeviceBasicInfo.java` | **新增** DTO（online / lastReportAt / status） |

### 后端 — alarm 模块

| 文件 | 改动 |
|---|---|
| `service/engine/AlarmEvaluationEngine.java` | evaluate 重写 subjectValues 构建（双 key）；注入 device 查询服务 |
| `service/engine/CriteriaEvaluator.java` | normalizeSubject 重写（段数 + 枚举校验）；resolveSubjectValue 适配 |

### 数据库

| 迁移 | 内容 |
|---|---|
| 单条 UPDATE | `UPDATE alarm_criteria SET level_config = NULL WHERE del_flag = 0;` |

## 十、数据流对比

### 10.1 改造前

```
MQTT 报文
  → MonitorIngestFacade.ingest()
    → ComputedAttributeEvaluator.evaluate()
      → lastMessageStore.get(prev)
      → 计算属性脚本
      → lastMessageStore.put(当前)    ← ingest 阶段写入
  → Redis Stream
  → MonitorIngestConsumerService
    → IoTDB 写入
    → 发布 MonitorDataIngestedEvent (properties only)
  → AlarmEvaluationEngine
    → subjectValues = {attrCode: value}   ← 单层 key，无 sensorCode 隔离，无 prev
    → CriteriaEvaluator.evaluateLevel
      → normalizeSubject 剥离 payload.current. 前缀
```

### 10.2 改造后

```
MQTT 报文
  → MonitorIngestFacade.ingest()
    → ComputedAttributeEvaluator.evaluate()
      → lastMessageStore.get(prev)
      → 计算属性脚本
      (不再 put)
  → Redis Stream
  → MonitorIngestConsumerService
    → IoTDB 写入
    → lastMessageStore.get(prevSnapshot)    ← consume 阶段读 prev
    → lastMessageStore.put(当前)            ← consume 阶段写入
    → 发布 MonitorDataIngestedEvent (properties + prevSnapshot + dataTime)
  → AlarmEvaluationEngine
    → 构建 4 个 bucket 的双 key subjectValues:
        - current.payload.* / sensorCode.current.payload.*
        - prev.payload.* / sensorCode.prev.payload.*
        - current.packet.dataTime / sensorCode.current.packet.dataTime
        - device.* / sensorCode.device.* (查 device 表)
    → CriteriaEvaluator.evaluateLevel
      → normalizeSubject 段数校验 + 直接精确匹配
```

## 十一、风险与处置

| 风险 | 处置 |
|---|---|
| ComputedAttributeEvaluator 删除 put 后，下次脚本看到的 prev 是"上一条+计算属性" | 消费阶段 put 时 msg.properties 已 merge 计算属性（ingest 阶段 merge），snapshot 包含计算属性 — **语义等价** |
| 双 key 写入导致 subjectValues 体积翻倍 | 单次事件 properties 数通常 <20，双 key 后 <80，HashMap 内存可忽略 |
| 跨传感器判据 | 本次完全不支持。判据 subject 的 sensorCode 段必须匹配本次事件的 sensorCode；跨传感器综合判据请用 Groovy 综合策略 |
| `MonitorDataIngestedEvent` 新增字段破坏序列化兼容 | 事件是 Spring ApplicationEvent，进程内同步消费，无跨进程序列化；新字段对现有消费方透明 |
| `ParsedMessageSnapshot` 下沉到 common 影响 timeseries 模块 | 仅包名变更 + import 调整；LastMessageStore / ComputedAttributeEvaluator 同步更新 |
| 清空 level_config 后用户判据失效 | 迁移后用户在前端看到判据等级配置为空，需要重新配置；可接受（用户已确认） |

## 十二、验收标准

1. **编译通过**：`mvn clean compile -pl zwei-common,zwei-iot-timeseries,zwei-iot-alarm -am` BUILD SUCCESS
2. **测试通过**：`mvn test -pl zwei-iot-alarm` 全部 PASS（现有 60 case 不破坏）
3. **代码 review 验证**：
   - `MonitorDataIngestedEvent` 含 `prevSnapshot` / `dataTime` 字段
   - `ComputedAttributeEvaluator` 第 87-95 行的 put 已删除
   - `MonitorIngestConsumerService.processParsedMessage()` 包含 get prev → put 当前 → 构造事件
   - `CriteriaEvaluator.normalizeSubject` 按段数 + 枚举值校验，不剥离前缀
   - `AlarmEvaluationEngine.evaluate()` 构建 4 bucket 双 key subjectValues
   - `useIndicatorTree.ts` 中 `prefixDisplayLabels` 修改了 value 字段
   - `useIndicatorTree.ts` 中 `buildFromSensors` 入参为 sensorCode
   - `useIndicatorTree.ts` 中 packet 节点下只有 dataTime
4. **数据库迁移**：`alarm_criteria.level_config` 全部为 NULL
5. **端到端**：
   - 前端传感器模式下选传感器 A 的"水位"属性 → 只有 A 下的该节点被高亮（B 的同名节点不高亮）
   - 配置判据 `DEV001.prev.payload.water_level GT 5` → 上报第一条（store 为空）→ 不触发；上报第二条（water_level > 5）→ 触发（prev 为第一条的值）
   - 配置判据 `current.device.onlineStatus EQ 0` → 设备离线时上报 → 触发

## 十三、后续工作（独立 spec）

- 跨传感器判据：`LastMessageStore` 维护双份（current + prev）或 AlarmEvaluationEngine 在评估前批量预取本隐患点下所有 sensorCode 的快照
- `packet.quality` 支持：`ParsedMessageSnapshot` 扩展 quality 字段
- 嵌套 subject 路径：`sensorCode.current.payload.obj.subfield` 5 段路径（支持对象类型属性）
