# 告警判据多传感器支持 — 实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 修复前端多传感器属性选择 bug；将判据 subject 格式从 `payload.current.attrCode` 迁移到 `[sensorCode.] {current|prev} {payload|device|packet} {attrCode}`；打通 LastMessageStore → MonitorDataIngestedEvent → AlarmEvaluationEngine 的 prev 数据通路。

**架构：** 把 `ParsedMessageSnapshot` 下沉到 zwei-common 作为共享契约；事件契约扩展 `prevSnapshot` 和 `dataTime`；LastMessageStore 的 put 时机从 ingest 阶段后移到 consume 阶段（get prev → put 当前 → 发布携带 prev 的事件）；AlarmEvaluationEngine 构建双 key（传感器模式 + 监测类型模式）的 subjectValues；CriteriaEvaluator normalizeSubject 按段数 + 枚举值校验。

**技术栈：** Java 17 / Spring Boot 4 / MyBatis / JUnit 5 / Mockito / AssertJ；Vue 3 / TypeScript / Element Plus / Vite。

**规格文档：** `docs/superpowers/specs/2026-06-22-multi-sensor-criteria-design.md`

**工作目录约定：** 本计划假定 `cd "E:/work/PMO/4.其他项目/sys-交通边坡监测预警/zwei"`（项目根）。Maven 命令在 `server/` 下执行。

---

## 文件清单

### 创建
- `server/zwei-common/src/main/java/com/zwei/common/domain/ParsedMessageSnapshot.java` — 从 timeseries 下沉的共享契约
- `server/zwei-iot-device/src/main/java/com/zwei/iot/device/domain/dto/DeviceBasicInfo.java` — 设备基础信息 DTO
- `server/zwei-iot-alarm/src/test/java/com/zwei/iot/alarm/service/engine/CriteriaEvaluatorTest.java` — 新增 normalizeSubject 段数校验测试
- `db/upgrade/v2026.06.22.001_clear_criteria_level_config.sql` — 迁移脚本

### 修改
- `server/zwei-common/src/main/java/com/zwei/common/event/MonitorDataIngestedEvent.java` — 新增 prevSnapshot + dataTime
- `server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/compute/ParsedMessageSnapshot.java` — 删除（已下沉）
- `server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/compute/LastMessageStore.java` — import 改 common 包
- `server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/compute/ComputedAttributeEvaluator.java` — import 改 common 包 + 删除第 87-95 行 put
- `server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/service/MonitorIngestConsumerService.java` — consume 阶段 get/put + 事件携带 prevSnapshot
- `server/zwei-iot-timeseries/src/test/java/com/zwei/iot/timeseries/compute/ComputedAttributeEvaluatorTest.java` — 删除 put 断言
- `server/zwei-iot-device/src/main/java/com/zwei/iot/device/service/IDeviceQueryService.java` — 新增 getBasicInfoById
- `server/zwei-iot-device/src/main/java/com/zwei/iot/device/service/impl/DeviceQueryServiceImpl.java` — 实现 getBasicInfoById
- `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/engine/CriteriaEvaluator.java` — normalizeSubject 重写
- `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/engine/AlarmEvaluationEngine.java` — 注入 IDeviceQueryService + 双 key subjectValues
- `web/src/views/alarm/composables/useIndicatorTree.ts` — 树结构重组 + bug 修复 + buildFromSensors 入参调整
- `web/src/views/alarm/AlarmCriteria.vue` — 删除 migrateSubject + 传 sensorCode

---

## 任务 1：ParsedMessageSnapshot 下沉到 zwei-common

**目的：** `MonitorDataIngestedEvent` 在 zwei-common 需要引用 `ParsedMessageSnapshot`，但 common 不能依赖 timeseries。把类型下沉到 common 作为共享契约。

**文件：**
- 创建：`server/zwei-common/src/main/java/com/zwei/common/domain/ParsedMessageSnapshot.java`
- 删除：`server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/compute/ParsedMessageSnapshot.java`
- 修改：
  - `server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/compute/LastMessageStore.java`
  - `server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/compute/ComputedAttributeEvaluator.java`
  - `server/zwei-iot-timeseries/src/test/java/com/zwei/iot/timeseries/compute/ComputedAttributeEvaluatorTest.java`
  - `server/zwei-iot-timeseries/src/test/java/com/zwei/iot/timeseries/compute/LastMessageStoreTest.java`
  - `server/zwei-iot-timeseries/src/test/java/com/zwei/iot/timeseries/integration/ComputedAttributeIngestTest.java`

- [ ] **步骤 1.1：在 common 模块创建新的 ParsedMessageSnapshot**

创建 `server/zwei-common/src/main/java/com/zwei/common/domain/ParsedMessageSnapshot.java`：

```java
package com.zwei.common.domain;

import java.util.Map;

/**
 * ParsedMessage 的精简快照, 用于 Redis 缓存上一条消息 (prevData)。
 *
 * <p>由 {@code zwei-iot-timeseries.LastMessageStore} 维护,
 * 被 {@code AlarmEvaluationEngine} 用作 prev 维度数据源。
 *
 * @param deviceCode 设备编码
 * @param sensorCode 传感器编码
 * @param dataTime   数据采集时间 (epoch ms)
 * @param properties 属性值映射 (attrCode -> value), 含固有属性 + 计算属性结果
 */
public record ParsedMessageSnapshot(
        String deviceCode,
        String sensorCode,
        long dataTime,
        Map<String, Object> properties
) {
}
```

- [ ] **步骤 1.2：删除 timeseries 模块的旧文件**

删除 `server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/compute/ParsedMessageSnapshot.java`。

- [ ] **步骤 1.3：批量更新 import**

在以下文件中把 `import com.zwei.iot.timeseries.compute.ParsedMessageSnapshot;` 替换为 `import com.zwei.common.domain.ParsedMessageSnapshot;`：

- `server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/compute/LastMessageStore.java`
- `server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/compute/ComputedAttributeEvaluator.java`
- `server/zwei-iot-timeseries/src/test/java/com/zwei/iot/timeseries/compute/ComputedAttributeEvaluatorTest.java`
- `server/zwei-iot-timeseries/src/test/java/com/zwei/iot/timeseries/compute/LastMessageStoreTest.java`
- `server/zwei-iot-timeseries/src/test/java/com/zwei/iot/timeseries/integration/ComputedAttributeIngestTest.java`

推荐用 `Grep` 确认所有引用点已更新：

```
Grep pattern="com\.zwei\.iot\.timeseries\.compute\.ParsedMessageSnapshot"
```

预期：返回 0 个匹配。

- [ ] **步骤 1.4：编译 + 运行 timeseries 测试验证**

运行：
```bash
cd server
mvn clean compile -pl zwei-common,zwei-iot-timeseries -am
mvn test -pl zwei-iot-timeseries -Dtest="ComputedAttributeEvaluatorTest,LastMessageStoreTest"
```

预期：BUILD SUCCESS + 测试全部 PASS（10 个 ComputedAttributeEvaluator + LastMessageStoreTest 用例不变）。

- [ ] **步骤 1.5：Commit**

```bash
git add server/zwei-common/src/main/java/com/zwei/common/domain/ParsedMessageSnapshot.java
git add server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/compute/ParsedMessageSnapshot.java
git add server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/compute/LastMessageStore.java
git add server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/compute/ComputedAttributeEvaluator.java
git add server/zwei-iot-timeseries/src/test/java/com/zwei/iot/timeseries/compute/ComputedAttributeEvaluatorTest.java
git add server/zwei-iot-timeseries/src/test/java/com/zwei/iot/timeseries/compute/LastMessageStoreTest.java
git add server/zwei-iot-timeseries/src/test/java/com/zwei/iot/timeseries/integration/ComputedAttributeIngestTest.java
git commit -m "refactor(common): ParsedMessageSnapshot 下沉到 common 模块"
```

---

## 任务 2：扩展 IDeviceQueryService（device 模块）

**目的：** `AlarmEvaluationEngine` 需要查询 device 表填充 `*.device.*` 维度 subjectValues。跨模块规则要求通过 Service 接口，不能直接注入 `DeviceMapper`。

**文件：**
- 创建：`server/zwei-iot-device/src/main/java/com/zwei/iot/device/domain/dto/DeviceBasicInfo.java`
- 修改：`server/zwei-iot-device/src/main/java/com/zwei/iot/device/service/IDeviceQueryService.java`
- 修改：`server/zwei-iot-device/src/main/java/com/zwei/iot/device/service/impl/DeviceQueryServiceImpl.java`

- [ ] **步骤 2.1：创建 DeviceBasicInfo DTO**

创建 `server/zwei-iot-device/src/main/java/com/zwei/iot/device/domain/dto/DeviceBasicInfo.java`：

```java
package com.zwei.iot.device.domain.dto;

/**
 * 设备基础信息精简 DTO, 供告警引擎解析 device 维度 subject。
 *
 * <p>由 {@code DeviceQueryServiceImpl.getBasicInfoById} 装配, 字段来源:
 * <ul>
 *   <li>{@code online} — {@code device_online_status.status}</li>
 *   <li>{@code lastReportAt} — {@code device.last_report_time} (epoch seconds)</li>
 *   <li>{@code status} — {@code device.status} (1-正常 2-维修 3-停用)</li>
 * </ul>
 */
public record DeviceBasicInfo(
        boolean online,
        long lastReportAt,
        int status
) {
}
```

- [ ] **步骤 2.2：扩展 IDeviceQueryService 接口**

在 `server/zwei-iot-device/src/main/java/com/zwei/iot/device/service/IDeviceQueryService.java` 末尾（第 27 行 `}` 前）插入：

```java
    /**
     * 按设备 ID 查询基础信息 (供告警引擎解析 device 维度 subject)。
     *
     * @param deviceId 设备主键
     * @return 基础信息; null 表示设备不存在或已逻辑删除
     */
    DeviceBasicInfo getBasicInfoById(Long deviceId);
```

- [ ] **步骤 2.3：实现 DeviceQueryServiceImpl.getBasicInfoById**

在 `server/zwei-iot-device/src/main/java/com/zwei/iot/device/service/impl/DeviceQueryServiceImpl.java` 中：

1. 先读完整文件，确认现有注入（DeviceMapper / DeviceOnlineStatusService 等）
2. 注入 `DeviceMapper`（若未注入）和 `DeviceOnlineStatusService`（若未注入）
3. 在类末尾添加方法：

```java
@Override
public DeviceBasicInfo getBasicInfoById(Long deviceId) {
    if (deviceId == null) return null;
    Device device = deviceMapper.selectDeviceById(deviceId);
    if (device == null) return null;

    boolean online = false;
    long lastReportAt = 0L;
    try {
        DeviceOnlineStatus status = deviceOnlineStatusService.getById(deviceId);
        if (status != null) {
            online = Integer.valueOf(1).equals(status.getStatus());
            if (status.getLastReportAt() != null) {
                lastReportAt = status.getLastReportAt().getTime() / 1000;
            }
        }
    } catch (Exception ignored) {
        // device_online_status 查询失败时降级为 online=false
    }

    long devLastReport = 0L;
    if (device.getLastReportTime() != null) {
        try {
            devLastReport = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                    .parse(device.getLastReportTime()).getTime() / 1000;
        } catch (Exception ignored) {
        }
    }
    if (lastReportAt == 0L) lastReportAt = devLastReport;

    int status = device.getStatus() != null ? device.getStatus() : 1;
    return new DeviceBasicInfo(online, lastReportAt, status);
}
```

注：实现时若发现 `DeviceOnlineStatusService` 没有 `getById(Long)` 方法，改用 `deviceOnlineStatusMapper.selectById(deviceId)`（若该 mapper 已被同类其他方法使用）或注入新 mapper。

- [ ] **步骤 2.4：编译验证**

```bash
cd server
mvn clean compile -pl zwei-iot-device -am
```

预期：BUILD SUCCESS。

- [ ] **步骤 2.5：Commit**

```bash
git add server/zwei-iot-device/src/main/java/com/zwei/iot/device/domain/dto/DeviceBasicInfo.java
git add server/zwei-iot-device/src/main/java/com/zwei/iot/device/service/IDeviceQueryService.java
git add server/zwei-iot-device/src/main/java/com/zwei/iot/device/service/impl/DeviceQueryServiceImpl.java
git commit -m "feat(device): IDeviceQueryService 新增 getBasicInfoById 供告警引擎使用"
```

---

## 任务 3：MonitorDataIngestedEvent 扩展 prevSnapshot + dataTime

**目的：** 事件契约扩展，携带 prev 快照和当前报文业务 dataTime。AlarmEvaluationEngine 从事件直接获取 prev 数据，不查 Redis。

**文件：**
- 修改：`server/zwei-common/src/main/java/com/zwei/common/event/MonitorDataIngestedEvent.java`
- 修改：`server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/service/MonitorIngestConsumerService.java`（调用方临时适配）

- [ ] **步骤 3.1：扩展事件字段**

读 `server/zwei-common/src/main/java/com/zwei/common/event/MonitorDataIngestedEvent.java` 当前实现。

在现有字段基础上新增：

```java
import com.zwei.common.domain.ParsedMessageSnapshot;

// 新增字段（放在 properties 下方）
private final long dataTime;
private final ParsedMessageSnapshot prevSnapshot;
```

新构造器（替换原构造器签名）：

```java
public MonitorDataIngestedEvent(Long deviceId, Long sensorId, String deviceCode,
                                String sensorCode, String sourceType, long receiveTime,
                                String payloadHash, List<PropertyValue> properties,
                                long dataTime, ParsedMessageSnapshot prevSnapshot) {
    this.deviceId = deviceId;
    this.sensorId = sensorId;
    this.deviceCode = deviceCode;
    this.sensorCode = sensorCode;
    this.sourceType = sourceType;
    this.receiveTime = receiveTime;
    this.payloadHash = payloadHash;
    this.properties = properties;
    this.dataTime = dataTime;
    this.prevSnapshot = prevSnapshot;
}
```

新增 getter：

```java
public long getDataTime() {
    return dataTime;
}

public ParsedMessageSnapshot getPrevSnapshot() {
    return prevSnapshot;
}
```

- [ ] **步骤 3.2：更新 MonitorIngestConsumerService 调用方（临时传占位值）**

在 `server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/service/MonitorIngestConsumerService.java` 中：

1. import 新增：`import com.zwei.common.domain.ParsedMessageSnapshot;`
2. 修改 `publishIngestedEvent` 方法（第 298 行附近）：

```java
private void publishIngestedEvent(Long deviceId, Long sensorId, ParsedMessage msg) {
    try {
        eventPublisher.publishEvent(new MonitorDataIngestedEvent(
                deviceId, sensorId, msg.deviceCode(), msg.sensorCode(), msg.sourceType(),
                msg.receiveTime(), msg.payloadHash(), msg.properties(),
                msg.dataTime(), null));   // ← 临时：prevSnapshot=null，任务 5 填实
        log.info("发布 MonitorDataIngestedEvent: deviceCode={} sensorCode={} properties={}",
                msg.deviceCode(), msg.sensorCode(), msg.properties().size());
    } catch (Exception e) {
        log.warn("发布 MonitorDataIngestedEvent 失败 deviceCode={} sensorCode={}: {}",
                msg.deviceCode(), msg.sensorCode(), e.getMessage());
    }
}
```

- [ ] **步骤 3.3：编译验证**

```bash
cd server
mvn clean compile -pl zwei-common,zwei-iot-timeseries,zwei-iot-alarm -am
```

预期：BUILD SUCCESS。alarm 模块没有直接构造 MonitorDataIngestedEvent（只消费），不受影响。

- [ ] **步骤 3.4：运行全量 alarm 测试确认不破坏**

```bash
cd server
mvn test -pl zwei-iot-alarm
```

预期：60 个 case 全部 PASS。

- [ ] **步骤 3.5：Commit**

```bash
git add server/zwei-common/src/main/java/com/zwei/common/event/MonitorDataIngestedEvent.java
git add server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/service/MonitorIngestConsumerService.java
git commit -m "feat(common): MonitorDataIngestedEvent 新增 prevSnapshot + dataTime 字段"
```

---

## 任务 4：Store 时机迁移（删除 ComputedAttributeEvaluator 的 put）

**目的：** 让 LastMessageStore 始终存储"上一条已完整处理的报文"。consume 阶段（任务 5）接管 get/put，AlarmEvaluationEngine 接到事件时 store 还未被推进，prev 可以正确取到。

**文件：**
- 修改：`server/zwei-iot-timeseries/src/test/java/com/zwei/iot/timeseries/compute/ComputedAttributeEvaluatorTest.java`（先更新测试）
- 修改：`server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/compute/ComputedAttributeEvaluator.java`

- [ ] **步骤 4.1：更新 ComputedAttributeEvaluatorTest（移除 put 断言）**

在 `ComputedAttributeEvaluatorTest.java` 中：

1. 第 65 行：`verify(lastMessageStore, never()).put(any(), any(), any());` — **保留**（无计算属性时不 put 仍然成立，因为 fast path 不进入求值阶段）
2. 第 96-98 行（firstReport 测试）：删除整个 `verify(lastMessageStore).put(...)` 断言块
3. 第 102-118 行（allScriptsFail 测试）：
   - 修改 `@DisplayName` 为 `"全部脚本失败(返回空 Map): 返回空 list"`
   - 删除第 115-117 行的注释和 `verify(lastMessageStore).put(...)` 断言
   - 修改后的测试体只保留 `assertThat(out).isEmpty();`

更新后的 allScriptsFail 测试：

```java
@Test
@DisplayName("全部脚本失败(返回空 Map): 返回空 list")
void allScriptsFail() {
    stubSensor();
    when(registry.getByMonitorTypeId(100L)).thenReturn(List.of(
            new ComputedAttribute(1L, 100L, "velocity", "速率", "mm/s",
                    "return 1/0", 1)));
    when(lastMessageStore.get(1L, "S1")).thenReturn(null);
    when(scriptEngine.executeComputed(anyString(), any(), any()))
            .thenReturn(Map.of());

    List<PropertyValue> out = evaluator.evaluate(1L, "S1", msg(12.0));

    assertThat(out).isEmpty();
    verify(lastMessageStore, never()).put(any(), any(), any());
}
```

更新后的 firstReport 测试（删除最后 verify 块）：

```java
@Test
@DisplayName("首次上报(prevData=null): 脚本可执行, 结果合并 properties")
void firstReport() {
    stubSensor();
    when(registry.getByMonitorTypeId(100L)).thenReturn(List.of(
            new ComputedAttribute(1L, 100L, "velocity", "速率", "mm/s",
                    "return curData.properties.displacement * 2", 1)));
    when(lastMessageStore.get(1L, "S1")).thenReturn(null);
    when(scriptEngine.executeComputed(anyString(), any(), isNull()))
            .thenReturn(Map.of("velocity", 24.0));

    List<PropertyValue> out = evaluator.evaluate(1L, "S1", msg(12.0));

    assertThat(out).hasSize(1);
    assertThat(out.get(0).identifier()).isEqualTo("velocity");
    assertThat(out.get(0).value()).isEqualTo(24.0);
}
```

- [ ] **步骤 4.2：运行测试确认测试更新正确（此时主代码未改，put 仍存在）**

```bash
cd server
mvn test -pl zwei-iot-timeseries -Dtest="ComputedAttributeEvaluatorTest"
```

预期：7 个 case 全部 PASS（put 断言已删除，现有 put 实现不影响）。

- [ ] **步骤 4.3：修改 ComputedAttributeEvaluator 删除 put**

在 `server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/compute/ComputedAttributeEvaluator.java` 中：

删除第 87-95 行（注释 `// 8. 总是写回 prevData...` 到 `lastMessageStore.put(...);` 结束的整块）：

```java
// 删除以下代码块（87-95 行）：
// 8. 总是写回 prevData(避免下次脚本看到更旧的 prev)
Map<String, Object> mergedProps = new LinkedHashMap<>();
for (PropertyValue p : message.properties()) {
    if (p.value() != null) mergedProps.put(p.identifier(), p.value());
}
for (PropertyValue p : computed) mergedProps.put(p.identifier(), p.value());
lastMessageStore.put(deviceId, sensorCode,
        new ParsedMessageSnapshot(message.deviceCode(), message.sensorCode(),
                message.dataTime(), mergedProps));
```

保留前面的 `return computed;`。

若删除后产生未使用 import（如 `ParsedMessageSnapshot` / `LinkedHashMap`），同步删除。

- [ ] **步骤 4.4：运行测试确认通过**

```bash
cd server
mvn test -pl zwei-iot-timeseries -Dtest="ComputedAttributeEvaluatorTest"
```

预期：7 个 case 全部 PASS。

- [ ] **步骤 4.5：Commit**

```bash
git add server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/compute/ComputedAttributeEvaluator.java
git add server/zwei-iot-timeseries/src/test/java/com/zwei/iot/timeseries/compute/ComputedAttributeEvaluatorTest.java
git commit -m "refactor(timeseries): ComputedAttributeEvaluator 删除 put, 时机后移到 consume 阶段"
```

---

## 任务 5：MonitorIngestConsumerService consume 阶段接管 get/put + 事件携带 prevSnapshot

**目的：** 把 LastMessageStore 的 get/put 收敛到 consume 阶段。consume IoTDB 写入成功后，先 get prev（此时 store 还是上一条）→ put 当前条 → 发布携带 prevSnapshot 的事件。

**文件：**
- 修改：`server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/service/MonitorIngestConsumerService.java`

- [ ] **步骤 5.1：注入 LastMessageStore**

读 `MonitorIngestConsumerService.java` 构造器（第 77-99 行）。在构造器参数列表末尾新增 `LastMessageStore lastMessageStore`，并新增 `private final LastMessageStore lastMessageStore;` 字段。

注意：构造器参数变更会影响所有调用方，但 Spring 自动注入，无需改其他代码。

- [ ] **步骤 5.2：在 processParsedMessage 中添加 get/put + 构造携带 prevSnapshot 的事件**

定位 `processParsedMessage()` 方法（第 238 行起）。在 `publishIngestedEvent(...)` 调用前（第 270 行附近）插入 get/put 逻辑：

```java
// ── prev snapshot: get 必须在 put 之前, 此时 store 里是上一条 ──
ParsedMessageSnapshot prevSnapshot = lastMessageStore.get(deviceId, sensorId != null ? sensorId : 0L) == null
        ? null : lastMessageStore.get(msg.deviceCode(), msg.sensorCode());
// 简化版本（推荐用 deviceId + sensorCode 复合 key, 与 LastMessageStore 契约一致）:
prevSnapshot = lastMessageStore.get(deviceId, msg.sensorCode());

// ── 推进 store: 当前条覆盖上一条 ──
Map<String, Object> currentProps = new LinkedHashMap<>();
for (PropertyValue pv : msg.properties()) {
    if (pv.value() != null) currentProps.put(pv.identifier(), pv.value());
}
ParsedMessageSnapshot currentSnapshot = new ParsedMessageSnapshot(
        msg.deviceCode(), msg.sensorCode(), msg.dataTime(), currentProps);
lastMessageStore.put(deviceId, msg.sensorCode(), currentSnapshot);

// ── 发布携带 prevSnapshot 的事件 ──
publishIngestedEvent(deviceId, sensorId, msg, prevSnapshot);
```

清理：上面第一个 `prevSnapshot` 赋值的"简化版本"注释是说明，实际只保留 `prevSnapshot = lastMessageStore.get(deviceId, msg.sensorCode());` 一行。

最终插入位置示意（伪代码，实际请读现有代码确认变量名）：

```java
// 现有: IoTDB 写入 + 运维指标回写
iotdbTimeSeriesService.writePoints(points);
// ... deviceOnlineStatusService / deviceSensorService / deviceMapper ...

// 新增 ↓
ParsedMessageSnapshot prevSnapshot = lastMessageStore.get(deviceId, msg.sensorCode());

Map<String, Object> currentProps = new LinkedHashMap<>();
for (PropertyValue pv : msg.properties()) {
    if (pv.value() != null) currentProps.put(pv.identifier(), pv.value());
}
lastMessageStore.put(deviceId, msg.sensorCode(),
        new ParsedMessageSnapshot(msg.deviceCode(), msg.sensorCode(),
                msg.dataTime(), currentProps));

publishIngestedEvent(deviceId, sensorId, msg, prevSnapshot);
// 现有: ack(record);
```

新增 import：
```java
import com.zwei.common.domain.ParsedMessageSnapshot;
import com.zwei.iot.timeseries.compute.LastMessageStore;
import java.util.LinkedHashMap;
import java.util.Map;
```

- [ ] **步骤 5.3：修改 publishIngestedEvent 签名 + 使用新事件构造器**

把 `publishIngestedEvent(Long deviceId, Long sensorId, ParsedMessage msg)` 改为 `publishIngestedEvent(Long deviceId, Long sensorId, ParsedMessage msg, ParsedMessageSnapshot prevSnapshot)`：

```java
private void publishIngestedEvent(Long deviceId, Long sensorId, ParsedMessage msg,
                                   ParsedMessageSnapshot prevSnapshot) {
    try {
        eventPublisher.publishEvent(new MonitorDataIngestedEvent(
                deviceId, sensorId, msg.deviceCode(), msg.sensorCode(), msg.sourceType(),
                msg.receiveTime(), msg.payloadHash(), msg.properties(),
                msg.dataTime(), prevSnapshot));
        log.info("发布 MonitorDataIngestedEvent: deviceCode={} sensorCode={} properties={} hasPrev={}",
                msg.deviceCode(), msg.sensorCode(), msg.properties().size(),
                prevSnapshot != null);
    } catch (Exception e) {
        log.warn("发布 MonitorDataIngestedEvent 失败 deviceCode={} sensorCode={}: {}",
                msg.deviceCode(), msg.sensorCode(), e.getMessage());
    }
}
```

- [ ] **步骤 5.4：STANDARD_POINT 路径 prevSnapshot 传 null**

定位 `processRecord()` 中 STANDARD_POINT 分支（第 185 行 `StandardMeasurementPoint point = JSON.parseObject(...)` 之后）。`wrapPointAsParsedMessage` 调用之后的 `publishIngestedEvent(point.deviceId(), point.sensorId(), wrapPointAsParsedMessage(point))` 改为：

```java
publishIngestedEvent(point.deviceId(), point.sensorId(),
        wrapPointAsParsedMessage(point), null);
```

- [ ] **步骤 5.5：编译 + 运行测试验证**

```bash
cd server
mvn clean compile -pl zwei-iot-timeseries -am
mvn test -pl zwei-iot-timeseries -Dtest="ComputedAttributeEvaluatorTest,LastMessageStoreTest"
```

预期：BUILD SUCCESS + 测试 PASS。

- [ ] **步骤 5.6：Commit**

```bash
git add server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/service/MonitorIngestConsumerService.java
git commit -m "feat(timeseries): consume 阶段接管 LastMessageStore get/put, 事件携带 prevSnapshot"
```

---

## 任务 6：CriteriaEvaluator normalizeSubject 重写（TDD）

**目的：** normalizeSubject 按段数 + 枚举值校验，识别新格式 `[sensorCode.] {current|prev} {payload|device|packet} {attrCode}`。老格式 `payload.current.x` 不再兼容。

**文件：**
- 创建：`server/zwei-iot-alarm/src/test/java/com/zwei/iot/alarm/service/engine/CriteriaEvaluatorTest.java`
- 修改：`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/engine/CriteriaEvaluator.java`

- [ ] **步骤 6.1：编写失败的测试**

创建 `server/zwei-iot-alarm/src/test/java/com/zwei/iot/alarm/service/engine/CriteriaEvaluatorTest.java`：

```java
package com.zwei.iot.alarm.service.engine;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Method;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CriteriaEvaluator.normalizeSubject 段数校验")
class CriteriaEvaluatorTest {

    private final CriteriaEvaluator evaluator = new CriteriaEvaluator();

    private String normalize(String subject) throws Exception {
        Method m = CriteriaEvaluator.class.getDeclaredMethod("normalizeSubject", String.class);
        m.setAccessible(true);
        return (String) m.invoke(evaluator, subject);
    }

    @Test
    @DisplayName("传感器模式 4 段格式: 原样返回")
    void sensorModeFourParts() throws Exception {
        assertThat(normalize("DEV001.current.payload.water_level")).isEqualTo("DEV001.current.payload.water_level");
        assertThat(normalize("DEV001.prev.payload.rainfall_hour")).isEqualTo("DEV001.prev.payload.rainfall_hour");
    }

    @Test
    @DisplayName("监测类型模式 3 段格式: 原样返回")
    void monitorTypeModeThreeParts() throws Exception {
        assertThat(normalize("current.payload.water_level")).isEqualTo("current.payload.water_level");
        assertThat(normalize("prev.device.onlineStatus")).isEqualTo("prev.device.onlineStatus");
    }

    @Test
    @DisplayName("device 维度: 无视 current/prev 都接受")
    void deviceDimensionAcceptsBothKinds() throws Exception {
        assertThat(normalize("current.device.lastReportTime")).isEqualTo("current.device.lastReportTime");
        assertThat(normalize("sensorA.prev.device.onlineStatus")).isEqualTo("sensorA.prev.device.onlineStatus");
    }

    @Test
    @DisplayName("packet 维度: 仅 attrCode=dataTime 合法 (但 normalizeSubject 不校验 attrCode)")
    void packetDimensionAcceptsAnyAttrCode() throws Exception {
        assertThat(normalize("current.packet.dataTime")).isEqualTo("current.packet.dataTime");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "current.unknown.water_level",         // 非法 dimension
            "sensorA.future.payload.water_level",  // 非法 kind
            "current.payload",                     // 2 段, 太短
            "a.b.c.d.e",                           // 5 段, 太长
            "payload.current.water_level",         // 老格式不兼容
            ""                                     // 空
    })
    @DisplayName("非法格式: 返回 null")
    void invalidFormatsReturnNull(String subject) throws Exception {
        assertThat(normalize(subject)).isNull();
    }

    @Test
    @DisplayName("null / 空白: 返回 null")
    void nullAndBlank() throws Exception {
        assertThat(normalize(null)).isNull();
        assertThat(normalize("   ")).isNull();
    }
}
```

- [ ] **步骤 6.2：运行测试确认失败**

```bash
cd server
mvn test -pl zwei-iot-alarm -Dtest="CriteriaEvaluatorTest"
```

预期：FAIL，错误信息类似 "expected DEV001.current.payload.water_level but null"（现有 normalizeSubject 剥离 `payload.` 前缀返回 `current.water_level`，与预期不符）。

- [ ] **步骤 6.3：重写 normalizeSubject**

在 `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/engine/CriteriaEvaluator.java` 中替换 `normalizeSubject` 方法（第 172-183 行）：

```java
private static final java.util.Set<String> KINDS = java.util.Set.of("current", "prev");
private static final java.util.Set<String> DIMENSIONS = java.util.Set.of("payload", "device", "packet");

/**
 * 标准化 subject — 按段数 + 枚举值校验新格式。
 *
 * <p>合法格式:
 * <ul>
 *   <li>4 段: {@code sensorCode.{kind}.{dimension}.{attrCode}}</li>
 *   <li>3 段: {@code {kind}.{dimension}.{attrCode}}</li>
 * </ul>
 * 老格式 {@code payload.current.x} / {@code payload.x} 不再兼容。
 */
private String normalizeSubject(String subject) {
    if (subject == null) return null;
    String s = subject.trim();
    if (s.isEmpty()) return null;
    String[] parts = s.split("\\.");
    if (parts.length == 4) {
        if (!KINDS.contains(parts[1])) {
            log.warn("[Alarm][Criteria] 非法 kind: {} (subject={})", parts[1], subject);
            return null;
        }
        if (!DIMENSIONS.contains(parts[2])) {
            log.warn("[Alarm][Criteria] 非法 dimension: {} (subject={})", parts[2], subject);
            return null;
        }
        return s;
    } else if (parts.length == 3) {
        if (!KINDS.contains(parts[0])) {
            log.warn("[Alarm][Criteria] 非法 kind: {} (subject={})", parts[0], subject);
            return null;
        }
        if (!DIMENSIONS.contains(parts[1])) {
            log.warn("[Alarm][Criteria] 非法 dimension: {} (subject={})", parts[1], subject);
            return null;
        }
        return s;
    }
    log.warn("[Alarm][Criteria] 不支持的 subject 段数: {} (subject={})", parts.length, subject);
    return null;
}
```

- [ ] **步骤 6.4：运行测试确认通过**

```bash
cd server
mvn test -pl zwei-iot-alarm -Dtest="CriteriaEvaluatorTest"
```

预期：8 个参数化 + 5 个独立 case 全部 PASS。

- [ ] **步骤 6.5：运行 alarm 全量测试确认未破坏现有 case**

```bash
cd server
mvn test -pl zwei-iot-alarm
```

预期：现有 60 case + 新增 CriteriaEvaluatorTest 全部 PASS。

注：如果 `extractSubjects` 方法（第 243 行）仍调用 `LevelCondition::getSubject` 直接返回，不需要改（返回的是原始 subject，下游评估时再 normalize）。如果有破坏，按相同规则适配。

- [ ] **步骤 6.6：Commit**

```bash
git add server/zwei-iot-alarm/src/test/java/com/zwei/iot/alarm/service/engine/CriteriaEvaluatorTest.java
git add server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/engine/CriteriaEvaluator.java
git commit -m "refactor(alarm): CriteriaEvaluator.normalizeSubject 按段数+枚举校验新格式"
```

---

## 任务 7：AlarmEvaluationEngine 双 key subjectValues

**目的：** 评估时构建 4 个 bucket 的双 key subjectValues（传感器模式 + 监测类型模式），让 CriteriaEvaluator 无需感知判据模式即可精确匹配。

**文件：**
- 修改：`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/engine/AlarmEvaluationEngine.java`

- [ ] **步骤 7.1：注入 IDeviceQueryService**

读 `AlarmEvaluationEngine.java` 构造器（第 64-81 行）。在构造器参数末尾新增 `IDeviceQueryService deviceQueryService`，新增字段 `private final IDeviceQueryService deviceQueryService;`。

新增 import：
```java
import com.zwei.common.domain.ParsedMessageSnapshot;
import com.zwei.iot.device.domain.dto.DeviceBasicInfo;
import com.zwei.iot.device.service.IDeviceQueryService;
import java.util.Set;
```

- [ ] **步骤 7.2：重写 evaluate 方法的 subjectValues 构建段**

定位 `evaluate()` 方法第 102-115 行（现有构建 subjectValues 的代码）。替换为：

```java
Map<String, Double> subjectValues = new HashMap<>();
String sensorCode = event.getSensorCode();
ParsedMessageSnapshot prev = event.getPrevSnapshot();
long currentDataTime = event.getDataTime();

// ── bucket 1: 本 sensorCode 的 current payload ──
if (event.getProperties() != null) {
    for (PropertyValue pv : event.getProperties()) {
        if (pv.value() instanceof Number n) {
            double v = n.doubleValue();
            subjectValues.put(sensorCode + ".current.payload." + pv.identifier(), v);
            subjectValues.put("current.payload." + pv.identifier(), v);
        }
    }
}

// ── bucket 2: 本 sensorCode 的 prev payload ──
if (prev != null && prev.properties() != null) {
    for (Map.Entry<String, Object> e : prev.properties().entrySet()) {
        if (e.getValue() instanceof Number n) {
            double v = n.doubleValue();
            subjectValues.put(sensorCode + ".prev.payload." + e.getKey(), v);
            subjectValues.put("prev.payload." + e.getKey(), v);
        }
    }
}

// ── bucket 3: packet.dataTime ──
subjectValues.put(sensorCode + ".current.packet.dataTime", (double) currentDataTime);
subjectValues.put("current.packet.dataTime", (double) currentDataTime);
if (prev != null) {
    subjectValues.put(sensorCode + ".prev.packet.dataTime", (double) prev.dataTime());
    subjectValues.put("prev.packet.dataTime", (double) prev.dataTime());
}

// ── bucket 4: device.* — 无视 current/prev, 查 device 表 ──
DeviceBasicInfo dev = deviceQueryService.getBasicInfoById(event.getDeviceId());
if (dev != null) {
    double online = dev.online() ? 1.0 : 0.0;
    double lastReport = (double) dev.lastReportAt();
    for (String kind : new String[]{"current", "prev"}) {
        subjectValues.put(sensorCode + "." + kind + ".device.onlineStatus", online);
        subjectValues.put(sensorCode + "." + kind + ".device.lastReportTime", lastReport);
        subjectValues.put(kind + ".device.onlineStatus", online);
        subjectValues.put(kind + ".device.lastReportTime", lastReport);
    }
}

if (subjectValues.isEmpty()) {
    log.debug("[Alarm][Skip] 报文无数值属性 deviceId={} sensorCode={}",
            event.getDeviceId(), event.getSensorCode());
    return;
}
```

- [ ] **步骤 7.3：保留后续逻辑（hazardPointIds / monitorContentId / evaluateCriteria）**

后续第 117 行起的 hazardPointIds 查询、monitorContentId 匹配、判据评估逻辑保持不变。注意 `evaluateCriteria` 接收的 `subjectValues` 参数现在已经是新结构（key 是完整路径）。

- [ ] **步骤 7.4：编译验证**

```bash
cd server
mvn clean compile -pl zwei-iot-alarm -am
```

预期：BUILD SUCCESS。

- [ ] **步骤 7.5：运行 alarm 全量测试确认未破坏**

```bash
cd server
mvn test -pl zwei-iot-alarm
```

预期：现有 case + CriteriaEvaluatorTest 全部 PASS。

注：若 AlarmEvaluationEngine 没有直接单测（确认目前没有），此步骤只能间接验证 CriteriaEvaluator + 其他模块测试通过。

- [ ] **步骤 7.6：Commit**

```bash
git add server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/engine/AlarmEvaluationEngine.java
git commit -m "feat(alarm): AlarmEvaluationEngine 构建双 key subjectValues 支持 4 维度"
```

---

## 任务 8：数据库迁移脚本

**目的：** 老判据 level_config 中的 subject 格式与新 normalizeSubject 不兼容，直接清空让用户重新配置。

**文件：**
- 创建：`db/upgrade/v2026.06.22.001_clear_criteria_level_config.sql`

- [ ] **步骤 8.1：创建迁移脚本**

创建 `db/upgrade/v2026.06.22.001_clear_criteria_level_config.sql`：

```sql
-- v2026.06.22.001_clear_criteria_level_config.sql
-- 告警判据 subject 格式迁移: 老格式 payload.current.attrCode 不再兼容
-- 新格式: [sensorCode.] {current|prev} {payload|device|packet} {attrCode}
-- 清空所有启用判据的 level_config, 用户在前端重新配置等级条件

UPDATE alarm_criteria
SET level_config = NULL,
    version = version + 1,
    update_time = NOW(),
    update_by = 'system-migration-20260622'
WHERE del_flag = 0
  AND level_config IS NOT NULL;
```

- [ ] **步骤 8.2：在本地 MySQL 执行验证**

```bash
mysql -uroot -pwodepassword geo_hazard_monitor < db/upgrade/v2026.06.22.001_clear_criteria_level_config.sql
```

或通过 MySQL CLI：

```sql
USE geo_hazard_monitor;
SOURCE E:/work/PMO/4.其他项目/sys-交通边坡监测预警/zwei/db/upgrade/v2026.06.22.001_clear_criteria_level_config.sql;
SELECT COUNT(*) FROM alarm_criteria WHERE level_config IS NOT NULL AND del_flag = 0;
```

预期：第二条查询返回 0。

- [ ] **步骤 8.3：Commit**

```bash
git add db/upgrade/v2026.06.22.001_clear_criteria_level_config.sql
git commit -m "chore(db): 清空 alarm_criteria.level_config 为新 subject 格式让路"
```

---

## 任务 9：前端 useIndicatorTree 重组 + bug 修复

**目的：** 重组 IndicatorTree 结构（kind 在外、dimension 在中、attrCode 在内）；修复 `prefixDisplayLabels` 同步修改 value 的 bug；`buildFromSensors` 入参改为 sensorCode；移除 packet.quality 节点。

**文件：**
- 修改：`web/src/views/alarm/composables/useIndicatorTree.ts`

- [ ] **步骤 9.1：重组 DEVICE_NODES 和 buildPayloadNode 为新结构**

读 `useIndicatorTree.ts` 完整文件。替换 `DEVICE_NODES` 常量（第 35-50 行）和 `buildPayloadNode` / `buildPayloadLeaves` 函数（第 52-73 行）。

**新结构**：把 `device` / `packet` / `payload` 三个维度都放在 `current` 和 `prev` 两个 kind 分支下。

替换 `DEVICE_NODES`（顶层常量删除，改为按 kind 构建函数）：

```typescript
// 删除原 DEVICE_NODES 常量

function buildDimensionChildren(contents: MonitorContentItem[]): IndicatorTreeNode[] {
  const payloadChildren: IndicatorTreeNode[] = (contents || []).map(c => {
    const shortLabel = `${c.name}${c.unit ? ` (${c.unit})` : ''}`
    return {
      value: `payload.${c.code}`,
      label: shortLabel,
      displayLabel: shortLabel,
      unit: c.unit || undefined,
      meta: {subjectType: 'CONTENT' as const, valueKind: 'current'},
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
        {value: 'device.onlineStatus', label: '在线状态', displayLabel: '在线状态', meta: {subjectType: 'DEVICE' as const}},
        {value: 'device.lastReportTime', label: '最后上报时间', displayLabel: '最后上报时间', meta: {subjectType: 'DEVICE' as const}},
      ],
    },
    {
      value: 'packet', label: '数据包信息', displayLabel: '数据包信息', disabled: true,
      children: [
        {value: 'packet.dataTime', label: '数据时间', displayLabel: '数据时间', meta: {subjectType: 'PACKET' as const}},
      ],
    },
  ]
}
```

- [ ] **步骤 9.2：替换 buildFromMonitorType 的树结构**

替换 `buildFromMonitorType` 函数（第 109-121 行）：

```typescript
async function buildFromMonitorType(typeId: number) {
  try {
    const detail = await getMonitorTypeDetail(typeId)
    const contents = detail.contents || []
    const tree: IndicatorTreeNode[] = [
      {
        value: 'current', label: '当前值', displayLabel: '当前值', disabled: true,
        children: buildDimensionChildren(contents),
      },
      {
        value: 'prev', label: '上一值', displayLabel: '上一值', disabled: true,
        children: buildDimensionChildren(contents),
      },
    ]
    setTree(tree)
  } catch {
    setTree([])
  }
}
```

- [ ] **步骤 9.3：修复 prefixDisplayLabels 同步修改 value（bug 核心修复）**

替换 `prefixDisplayLabels` 函数（第 76-89 行）：

```typescript
/** 深拷贝节点树, 为所有 disabled=false 的节点 value 和 displayLabel 都加上 sensorCode 前缀 */
function prefixDisplayLabels(nodes: IndicatorTreeNode[], prefix: string): IndicatorTreeNode[] {
  return nodes.map(n => {
    const copy: IndicatorTreeNode = {...n}
    if (!n.disabled) {
      // 可选叶子 / 可选节点: value 和 displayLabel 都加前缀（修复多传感器 value 重复 bug）
      copy.value = `${prefix}.${n.value}`
      copy.displayLabel = `${prefix}.${n.label}`
    } else if (n.children) {
      // 分组节点: 保持自身 label, 递归处理子节点
      copy.children = prefixDisplayLabels(n.children, prefix)
    }
    return copy
  })
}
```

**关键改动**：第 80 行 `copy.value = \`${prefix}.${n.value}\`` 是 bug 修复核心。

- [ ] **步骤 9.4：buildFromSensors 入参改 sensorCode**

替换 `buildFromSensors` 函数（第 123-150 行）：

```typescript
async function buildFromSensors(sensors: { sensorCode: string; sensorName: string; monitorTypeId: number }[]) {
  const seenTypes = new Map<number, MonitorContentItem[]>()
  for (const s of sensors) {
    if (!seenTypes.has(s.monitorTypeId)) {
      try {
        const detail = await getMonitorTypeDetail(s.monitorTypeId)
        seenTypes.set(s.monitorTypeId, detail.contents || [])
      } catch {
        seenTypes.set(s.monitorTypeId, [])
      }
    }
  }

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
          children: buildDimensionChildren(contents).map(n => ({
            ...n,
            // 注意: 不在 sensor 子节点上直接 prefix, 而是让 prefixDisplayLabels 递归处理
            // 这里只标记当前层级, prefix 由下面调用完成
          })),
        },
        {
          value: 'prev', label: '上一值', displayLabel: '上一值', disabled: true,
          children: buildDimensionChildren(contents),
        },
      ],
    } satisfies IndicatorTreeNode
  })

  // 对每个 sensor 子树应用 prefixDisplayLabels, 加上 sensorCode 前缀
  const prefixedTree = tree.map(sensorNode => ({
    ...sensorNode,
    children: [
      ...prefixDisplayLabels(sensorNode.children!, sensorNode.value),  // sensorCode 作为 prefix
    ],
  }))

  setTree(prefixedTree)
}
```

**说明**：sensor 节点本身 value=sensorCode（不前缀），children 经过 prefixDisplayLabels 后所有可选叶子 value 变为 `{sensorCode}.current.payload.{attrCode}` 等四段格式。

- [ ] **步骤 9.5：删除残留的 buildPayloadNode / buildPayloadLeaves 函数**

确认旧函数已删除（被 `buildDimensionChildren` 替代）。删除残留 import（如有）。

- [ ] **步骤 9.6：TypeScript 编译验证**

```bash
cd web
npx vue-tsc --noEmit
```

预期：无错误。如果提示 `AlarmCriteria.vue` 传 sensorId 类型不匹配，预期会在任务 10 修复——先记下错误，继续到任务 10。

- [ ] **步骤 9.7：Commit**

```bash
git add web/src/views/alarm/composables/useIndicatorTree.ts
git commit -m "fix(alarm-web): 重组 IndicatorTree + 修复多传感器 subject value 重复 bug"
```

---

## 任务 10：前端 AlarmCriteria.vue 适配

**目的：** `buildFromSensors` 调用方传 sensorCode；删除老格式迁移函数 migrateSubject。

**文件：**
- 修改：`web/src/views/alarm/AlarmCriteria.vue`

- [ ] **步骤 10.1：调用 buildFromSensors 传 sensorCode**

读 `AlarmCriteria.vue` 第 285-301 行。替换 `device.sensors.map(...)` 映射：

```typescript
await buildFromSensors(
  device.sensors.map((s: any) => ({
    sensorCode: s.sensorCode,    // 替代原 s.id || s.sensorId
    sensorName: s.monitorTypeName || s.name || s.sensorName || '',
    monitorTypeId: s.monitorTypeId,
  }))
)
```

**前置假设**：`device.sensors` 数组元素含 `sensorCode` 字段。若 API 响应未返回该字段，需要在后端 `DeviceController` 的设备详情响应 DTO 中加入 sensorCode（独立后续任务，本计划不覆盖）。

- [ ] **步骤 10.2：删除 migrateSubject 函数**

定位 `migrateSubject` 函数（第 214-218 行附近）。整个函数删除，并删除所有调用点（在 levelForm 初始化处）。

如果 migrateSubject 在 `initLevelForm` 中被调用：

```typescript
// 删除前: groups = rawGroups.map(g => ({...g, conditions: g.conditions.map(c => ({...c, subject: migrateSubject(c.subject)}))}))
// 删除后: groups = rawGroups.map(g => ({...g}))  // subject 保持原样
```

- [ ] **步骤 10.3：TypeScript 编译 + Vite 启动验证**

```bash
cd web
npx vue-tsc --noEmit
npm run dev
```

预期：vue-tsc 无错误；Vite 启动成功，浏览器访问 http://localhost:5173/ 能打开判据配置页面。

- [ ] **步骤 10.4：Commit**

```bash
git add web/src/views/alarm/AlarmCriteria.vue
git commit -m "refactor(alarm-web): AlarmCriteria 适配 sensorCode + 删除老格式迁移"
```

---

## 任务 11：端到端验证

**目的：** 启动全栈验证多传感器判据配置 + prev 语义触发。

- [ ] **步骤 11.1：启动依赖容器**

```bash
docker start redis geo_iotdb
```

- [ ] **步骤 11.2：编译 + 启动后端**

```bash
cd server
mvn clean package -DskipTests -pl zwei-admin -am
# 启动（在 server/ 目录）
java -jar -Dspring.profiles.active=local zwei-admin/target/zwei-admin.jar
```

预期：约 22 秒后看到 `Started RuoYiApplication`。

- [ ] **步骤 11.3：启动前端**

```bash
cd web
npm run dev
```

- [ ] **步骤 11.4：前端功能验证**

访问 http://localhost:5173/ 登录后进入判据配置页面：

1. 选择一个绑定多传感器的隐患点
2. 配置某个等级条件，选择传感器 A 的 payload 下某属性
3. **验证**：只有传感器 A 下的该属性被高亮选中，传感器 B/C 下的同名属性**不会**被选中（bug 已修复）
4. 查看保存后的 level_config JSON：subject 字段应为 `sensorA.current.payload.attrCode` 四段格式

- [ ] **步骤 11.5：后端日志验证 prev 通路**

上报两条报文（同设备同传感器，属性值递增）：

1. 第一条上报 → 后端日志：
   - `发布 MonitorDataIngestedEvent: ... hasPrev=false`（首次上报）
2. 第二条上报 → 后端日志：
   - `发布 MonitorDataIngestedEvent: ... hasPrev=true`（store 已推进）
   - `[Alarm][In] 接收监测数据 ...`（事件被评估引擎接收）
   - `[Alarm][Eval]` 相关日志显示 subjectValues 含 `*.prev.payload.*` key

- [ ] **步骤 11.6：判据触发验证**

配置判据：`DEV001.prev.payload.water_level GT 5`（大于 5 触发）

1. 上报第一条 water_level=3 → 不触发（store 为空，prev=null）
2. 上报第二条 water_level=10 → 检查是否触发（prev=3，不满足 GT 5）
3. 上报第三条 water_level=8 → 检查是否触发（prev=10，满足 GT 5）

注：本验证依赖具体传感器配置；如无测试设备可用 MQTT 工具模拟上报。

---

## 自检清单

### 规格覆盖度

| 规格章节 | 对应任务 |
|---|---|
| §2 Subject 路径格式 | 任务 6（normalizeSubject 校验）+ 任务 9（前端生成） |
| §3 MonitorDataIngestedEvent 契约 | 任务 3 |
| §3 ParsedMessageSnapshot 下沉 | 任务 1 |
| §4 Store 时机迁移 | 任务 4 + 任务 5 |
| §5 AlarmEvaluationEngine 双 key | 任务 7 |
| §5 IDeviceQueryService 扩展 | 任务 2 |
| §6 CriteriaEvaluator 改造 | 任务 6 |
| §7 前端 IndicatorTree 重组 | 任务 9 |
| §7 prefixDisplayLabels bug 修复 | 任务 9 步骤 9.3 |
| §7 buildFromSensors 入参调整 | 任务 9 步骤 9.4 + 任务 10 步骤 10.1 |
| §7 packet.quality 移除 | 任务 9 步骤 9.1（buildDimensionChildren 不含 quality） |
| §7 migrateSubject 删除 | 任务 10 步骤 10.2 |
| §8 数据库迁移 | 任务 8 |

### 占位符扫描

- 无"TODO"/"待定"字样 ✓
- 所有代码步骤含完整代码块 ✓
- 测试步骤含可执行命令和预期输出 ✓

### 类型一致性

- `ParsedMessageSnapshot` 全程使用 `com.zwei.common.domain.ParsedMessageSnapshot`（任务 1 下沉后统一）
- `DeviceBasicInfo` 在任务 2 定义，任务 7 使用 — 字段名 `online / lastReportAt / status` 一致
- `MonitorDataIngestedEvent` 构造器在任务 3 扩展 10 个参数，任务 5 调用方使用相同顺序
- `normalizeSubject` 返回 String（任务 6），`subjectValues.put(normalizeSubject(...))` key 类型一致（任务 7）
- `buildFromSensors` 入参类型从 `{sensorId, sensorName, monitorTypeId}` 改为 `{sensorCode, sensorName, monitorTypeId}`（任务 9 定义，任务 10 调用方匹配）

---

## 执行风险

| 风险 | 处置 |
|---|---|
| `DeviceOnlineStatusService` 没有 `getById(Long)` 方法 | 任务 2 步骤 2.3 注释里说明降级方案；实现时读 DeviceOnlineStatusService 源码确认 |
| `device.sensors` API 响应不含 sensorCode | 任务 10 步骤 10.1 注释里标记为前置假设；必要时扩展后端 DeviceController 响应 |
| `MonitorIngestConsumerService` 的 processParsedMessage 中 `deviceId` / `sensorId` 变量名与计划伪代码不一致 | 步骤 5.2 说明"实际请读现有代码确认变量名" |
| `IDeviceQueryService` 扩展影响 `DeviceQueryServiceImpl` 现有注入 | 任务 2 步骤 2.3 要求先读完整文件确认注入；保持现有依赖不变 |
