# MonitorDataIngestedEvent 携带完整 ParsedMessage 契约重构设计

> 将 `MonitorDataIngestedEvent` 从**单属性**（attrCode + value）重构为**完整数据包**（ParsedMessage 全部字段），让一次 MQTT 报文对应一次事件，下游告警引擎获得完整报文上下文。

## 一、概述

### 1.1 背景

当前 `MonitorIngestConsumerService.processParsedMessage()`（PARSED_MESSAGE 路径）在 IoTDB 落库成功后，会把 `ParsedMessage` adapt 成多个 `StandardMeasurementPoint`，**逐点循环发布** `MonitorDataIngestedEvent`：

```java
// 问题代码（MonitorIngestConsumerService.java:267-270）
for (StandardMeasurementPoint pt : points) {
    publishIngestedEvent(pt);  // 一次报文 N 个属性 → 发 N 次事件
}
```

这导致：

- 一次完整报文被拆分为 N 个独立事件，下游告警引擎**丢失报文上下文**
- 一次上报触发 N 次评估循环，**效率低**
- 未来若要做多属性综合判据（如"雨量 > X 且 水位 > Y"），**无法实现**

事件契约 `MonitorDataIngestedEvent` 本身也只携带 `attrCode + value + dataTime` 三个单属性字段。

### 1.2 目标

- **契约层**：`MonitorDataIngestedEvent` 持有完整报文数据（`ParsedMessage` 所有字段），不再有单属性语义
- **发布层**：一次 MQTT 报文（ParsedMessage）落库成功后**只发布一次**事件
- **消费层**：`AlarmEvaluationEngine` 一次性把报文所有数值属性放入 `subjectValues`，单属性判据继续工作，为未来多属性综合判据铺路
- **统一性**：STANDARD_POINT 旧路径（已无生产调用方）也改造为包装单属性 `ParsedMessage` 后走同一发布逻辑，契约统一

### 1.3 范围外

- **嵌套 `subjectValues` 结构升级**（`{sensorCode: {设备信息, 数据包, props}}`）— 属独立架构改造，另开 spec
- `CriteriaEvaluator` 评估器逻辑重写 — 同上
- 前端判据配置页面改造 — 同上
- 现有判据 `level_config` 数据迁移 — 不需要（向后兼容）

## 二、契约设计

### 2.1 `MonitorDataIngestedEvent` 字段对比

| 字段 | 旧 | 新 | 来源 |
|---|---|---|---|
| `deviceId` (Long) | ✓ | ✓ 保留 | consumer adapt 阶段解析 |
| `sensorId` (Long) | ✓ | ✓ 保留 | consumer adapt 阶段解析 |
| `sensorCode` (String) | ✓ | ✓ 保留 | ParsedMessage.sensorCode |
| `sourceType` (String) | ✓ | ✓ 保留 | ParsedMessage.sourceType |
| `attrCode` (String) | ✓ | ✗ 删除 | 由 properties 列表取代 |
| `value` (Double) | ✓ | ✗ 删除 | 由 properties 列表取代 |
| `dataTime` (Long) | ✓ | ✗ 删除 | 由 properties 列表取代 |
| `deviceCode` (String) | ✗ | ✓ 新增 | ParsedMessage.deviceCode |
| `receiveTime` (long) | ✗ | ✓ 新增 | ParsedMessage.receiveTime |
| `payloadHash` (String) | ✗ | ✓ 新增 | ParsedMessage.payloadHash |
| `properties` (List\<PropertyValue\>) | ✗ | ✓ 新增 | ParsedMessage.properties |

### 2.2 新构造器

```java
public MonitorDataIngestedEvent(Long deviceId, Long sensorId, String deviceCode,
                                String sensorCode, String sourceType, long receiveTime,
                                String payloadHash, List<PropertyValue> properties)
```

访问器与字段同名（`getDeviceId()` / `getProperties()` / …）。

### 2.3 向后兼容性

- `MonitorDataIngestedEvent` 旧构造器 `(deviceId, sensorId, sensorCode, attrCode, value, dataTime, sourceType)` **删除**（无第三方调用方）
- 生产代码中只有 `MonitorIngestConsumerService` 发布、`AlarmEvaluationEngine` 消费该事件
- 测试代码无（grep `*Test*.java` 未找到引用）

## 三、改动清单（3 个文件）

### 3.1 `server/zwei-common/src/main/java/com/zwei/common/event/MonitorDataIngestedEvent.java`

- 删除字段：`attrCode`、`value`、`dataTime`
- 新增字段：`deviceCode`、`receiveTime`、`payloadHash`、`properties`
- 新增 import：`com.zwei.common.domain.PropertyValue`、`java.util.List`
- 删除旧构造器，新增上述新构造器
- 删除旧访问器 `getAttrCode/getValue/getDataTime`，新增 `getDeviceCode/getReceiveTime/getPayloadHash/getProperties`

### 3.2 `server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/service/MonitorIngestConsumerService.java`

**`processParsedMessage()`（PARSED_MESSAGE 路径）**：

删除循环：

```java
// 删除（267-270 行）
for (StandardMeasurementPoint pt : points) {
    publishIngestedEvent(pt);
}
```

替换为单次发布（携带完整 ParsedMessage + adapt 时已解析的 deviceId/sensorId）：

```java
publishIngestedEvent(parsed.deviceCode(), points.get(0).deviceId(),
                     points.get(0).sensorId(), parsed);
```

**`processRecord()` 默认分支（STANDARD_POINT 路径，已无生产调用方）**：

删除：

```java
// 删除（214 行）
publishIngestedEvent(point);
```

替换为：包装 `StandardMeasurementPoint` 为单属性 `ParsedMessage`，走同一发布方法。需要反查 deviceCode（通过 `deviceMapper.selectDeviceById(point.deviceId()).getCode()`）。

**统一发布方法（新增）**：

```java
private void publishIngestedEvent(String deviceCode, Long deviceId, Long sensorId,
                                  com.zwei.common.domain.ParsedMessage msg) {
    try {
        eventPublisher.publishEvent(new MonitorDataIngestedEvent(
                deviceId, sensorId, deviceCode, msg.sensorCode(), msg.sourceType(),
                msg.receiveTime(), msg.payloadHash(), msg.properties()));
        log.info("发布 MonitorDataIngestedEvent: deviceCode={} sensorCode={} properties={}",
                deviceCode, msg.sensorCode(), msg.properties().size());
    } catch (Exception e) {
        log.warn("发布 MonitorDataIngestedEvent 失败 deviceCode={} sensorCode={}: {}",
                deviceCode, msg.sensorCode(), e.getMessage());
    }
}
```

**删除旧方法**：`publishIngestedEvent(StandardMeasurementPoint point)`。

### 3.3 `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/engine/AlarmEvaluationEngine.java`

**`evaluate(event)`**：

删除：

```java
// 删除（101-104 行）
if (event.getValue() == null) {
    log.debug("[Alarm][Skip] value=null ...");
    return;
}
```

新增：从 `event.getProperties()` 一次性构建 `subjectValues`：

```java
Map<String, Double> subjectValues = new HashMap<>();
for (PropertyValue pv : event.getProperties()) {
    if (pv.value() instanceof Number n) {
        subjectValues.put(pv.identifier(), n.doubleValue());
    }
}
if (subjectValues.isEmpty()) {
    log.debug("[Alarm][Skip] 报文无数值属性 deviceId={} sensorCode={}",
            event.getDeviceId(), event.getSensorCode());
    return;
}
```

**属性元数据匹配（114-142 行）**：原逻辑按单 `event.getAttrCode()` 匹配 → 改为遍历 `event.getProperties()`，对每个 `PropertyValue.identifier` 在 `SensorMetadata.attributes` 中查找匹配，**取第一个匹配且 `monitorContentId != null` 的**作为本事件的 `monitorContentId`（用于兜底判据路径）。

**`evaluateCriteria` 签名**：

```java
private boolean evaluateCriteria(MonitorDataIngestedEvent event,
                                 Map<String, Double> subjectValues,
                                 List<AlarmCriteria> criteriaList,
                                 List<Long> hazardPointIds,
                                 Long monitorContentId)
```

`subjectValues` 由 `evaluate()` 调用方构建并传入。

**AlarmRecord 字段取值策略**：

当 winner 候选判据确定后，通过其 `levelConfig.groups[0].conditions[0].subject`（或旧格式 `conditions[0].subject`）拿到 `winnerSubject`，然后：

- `currentValue` = `subjectValues.get(winnerSubject)` → `new BigDecimal(...)`
- `attrName` = 从 `event.getProperties()` 中按 `identifier == winnerSubject` 查 `PropertyValue.name`，查不到时回退为 `winnerSubject`
- `alarmMessage` = `buildAlarmMessage(attrName, currentValue, winner.levelConfig, winner.level)`
- `sensorId` / `deviceId` / `monitorContentId`：用事件级字段（不再随候选变化）

**内部循环日志**：原 `event.getAttrCode()` / `event.getValue()` → 改为 `subjectValues.keySet()` / `subjectValues`。

**`buildAlarmMessage()`**：签名不变（已是 `attrName, double currentValue, LevelConfig, int level`），只是调用方传值方式改变（不再用 `event.getValue()`）。

## 四、数据流对比

### 4.1 改造前

```
ParsedMessage(3 个属性) → adapt → [pt1, pt2, pt3]
  → IoTDB.writePoints([pt1, pt2, pt3])
  → for pt in [pt1, pt2, pt3]:
      publishEvent(MonitorDataIngestedEvent(attrCode=pt.attrCode, value=pt.value))
      ↓
      AlarmEvaluationEngine.evaluate(event)
        → subjectValues = {pt.attrCode: pt.value}   // 一次只有一个属性
        → evaluateCriteria(...)
```

### 4.2 改造后

```
ParsedMessage(3 个属性) → adapt → [pt1, pt2, pt3]
  → IoTDB.writePoints([pt1, pt2, pt3])
  → publishEvent(MonitorDataIngestedEvent(properties=[pv1, pv2, pv3]))   // 一次发布
      ↓
      AlarmEvaluationEngine.evaluate(event)
        → subjectValues = {pv1.id: pv1.val, pv2.id: pv2.val, pv3.id: pv3.val}   // 一次性全部
        → evaluateCriteria(...)   // 单属性判据按 subject 取值，多属性判据未来可工作
```

## 五、风险与处置

| 风险 | 处置 |
|---|---|
| STANDARD_POINT 路径 deviceCode 反查增加一次 DB 调用 | 该路径无生产调用方，性能影响可忽略；反查失败时 deviceCode=null（事件容错） |
| 多属性触发多条判据时，AlarmRecord.currentValue 取哪个 | 取 winner 判据 `level_config[0].conditions[0].subject` 对应的值（判据的"主属性"） |
| 日志变长（多属性打印） | 控制单行长度，只打印 keySet 和 size，value 详情走 DEBUG 级别 |
| 前端 SSE 推送格式变化 | AlarmTriggeredEvent 字段不变，前端无感知 |

## 六、验收标准

1. `mvn clean compile -pl zwei-common,zwei-iot-timeseries,zwei-iot-alarm -am` 通过
2. `mvn test -pl zwei-iot-alarm` 通过（现有 60 case 不破坏）
3. 代码 review：
   - `MonitorDataIngestedEvent` 无 `attrCode`/`value`/`dataTime` 字段
   - `processParsedMessage()` 内无 `for (pt : points) publishIngestedEvent(pt)` 循环
   - `AlarmEvaluationEngine.evaluate()` 一次性构建 subjectValues
4. 端到端：MQTT PUBLISH 一个多属性报文 → 仅触发一次 `AlarmEvaluationEngine.onMonitorDataIngested`，subjectValues 包含全部数值属性

## 七、后续工作（独立 spec）

- Q3 嵌套 `subjectValues` 结构升级：`{sensorCode: {设备信息, 数据包, props}}`
- `CriteriaEvaluator` 评估器重写：支持 subject 路径 `sensorCode.props.xxx`
- 前端判据配置页面：subject 选择器适配
