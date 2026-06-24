# 告警通知 sourceType 拆分设计

## 背景

告警通知中心（`alarm_notification` 表）通过 `source_type` 字段区分事件来源。当前取值：

| sourceType | 含义 | 发布方 |
|---|---|---|
| `alarm` | 告警事件（不区分类型） | `AlarmNotifier.dispatchForAlarm()` |
| `offline` | 设备离线事件 | `AlarmNotifier.dispatchForOffline()` |
| `notice` | 系统公告 | `SysNoticeServiceImpl` |

**问题**：告警实际存在两种类型 —— `THRESHOLD`（阈值告警，来自 `AlarmEvaluationEngine`）和 `COMPREHENSIVE`（综合告警，来自 `ComprehensiveAlarmJob`），但通知记录里都记为 `alarm`，前端无法按类型筛选或区分展示。

**目标**：将 `alarm` 拆分为 `threshold`（阈值告警）和 `comprehensive`（综合告警），两者都支持四级（蓝/黄/橙/红）等级区分。

## 范围边界（关键）

**本次只改后端**。以下文件**不改动**（另一分支正在重构消息中心前端，避免冲突）：

- `web/src/layout/index.vue` — 通知铃铛面板
- `web/src/api/alarmNotification.ts` — 通知中心 API
- `web/src/views/alarm/RealtimeAlarm.vue` — 告警详情
- `web/src/views/alarm/components/*` — 告警组件
- 其他所有 `web/**` 文件

前端现状（不改）：`layout/index.vue` 的路由逻辑按 `sourceType === 'alarm'` 匹配，拆分后旧记录迁移为 `threshold`/`comprehensive`，该等值判断将不再命中 —— 但这正是另一分支要重构的部分，本分支不碰。

## 现状分析

### AlarmTriggeredEvent 已携带 alarmType

`server/zwei-common/src/main/java/com/zwei/common/event/AlarmTriggeredEvent.java`：

```java
public class AlarmTriggeredEvent {
    private Long alarmId;
    private Long hazardPointId;
    private Integer alarmLevel;        // 1-4
    private String alarmType;          // "THRESHOLD" 或 "COMPREHENSIVE"
    private String alarmMessage;
    private String triggerReason;
    // ...
}
```

两个发布方：

| 发布方 | alarmType 值 | 触发场景 |
|---|---|---|
| `AlarmEvaluationEngine` | `THRESHOLD` | 单指标阈值命中 |
| `ComprehensiveAlarmJob` | `COMPREHENSIVE` | Groovy 综合策略命中 |

### AlarmNotifier 当前硬编码 "alarm"

`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/notify/AlarmNotifier.java:83-101`：

```java
private void dispatchForAlarm(AlarmTriggeredEvent event) {
    // ... 匹配分发规则 ...
    String title = "告警通知";
    String content = StringUtils.defaultString(event.getAlarmMessage(), "");
    Collection<AlarmNotification> notifications = buildAndDedup(
        rules, "alarm", event.getAlarmId(), title, content);  // ← line 99
    // ...
}
```

`dispatchForOffline()`（lines 104-123）使用 `sourceType = "offline"`，**本次不改**。

### SSE 自动透传

`SystemNotifyChannel.buildPayloadMap()` 已在 payload 中携带 `sourceType`（line 57: `data.put("sourceType", n.getSourceType())`），后端改完值前端 SSE 事件自动更新，无需改 SystemNotifyChannel。

### DB 唯一约束天然隔离

`alarm_notification` 表的 `uk_notif_dedup (source_type, source_id, recipient_id, channel)` 保证去重。由于每条 `alarm_record` 要么是 THRESHOLD 要么是 COMPREHENSIVE，拆分后 `source_id`（alarm_record.id）与 `source_type` 一一对应，不会出现"同一条告警既算 threshold 又算 comprehensive"的情况，约束天然有效。

## 设计

### 第 1 节：AlarmNotifier.dispatchForAlarm() 改造

**文件**：`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/notify/AlarmNotifier.java`

**当前**（lines 94-99）：

```java
String title = "告警通知";
String content = StringUtils.defaultString(event.getAlarmMessage(), "");
// ...
Collection<AlarmNotification> notifications = buildAndDedup(
    rules, "alarm", event.getAlarmId(), title, content);
```

**改造后**：

```java
boolean isComprehensive = "COMPREHENSIVE".equals(event.getAlarmType());
String sourceType = isComprehensive ? "comprehensive" : "threshold";
String typeName = isComprehensive ? "综合告警" : "阈值告警";
String title = "[" + typeName + "] "
    + StringUtils.defaultString(event.getAlarmMessage(), "告警通知");
String content = StringUtils.defaultString(event.getAlarmMessage(), "");
// ...
Collection<AlarmNotification> notifications = buildAndDedup(
    rules, sourceType, event.getAlarmId(), title, content);
```

**改动点**：

1. `sourceType` 由 `event.getAlarmType()` 派生（`COMPREHENSIVE` → `comprehensive`，其他 → `threshold`）
2. `title` 前缀加 `[综合告警]` / `[阈值告警]`，便于通知展示和短信/邮件场景区分
3. `content` 不变（仍是 `alarmMessage`）

**边界**：

- `event.getAlarmType()` 为 `null` 或其他未知值时，按 `threshold` 兜底（向后兼容，理论上不会发生）
- `dispatchForOffline()` 完全不动

### 第 2 节：DB 迁移脚本

**新文件**：`db/upgrade/v2026.06.24.001_source_type_split.sql`

**脚本内容**：

```sql
-- 告警通知 source_type 拆分: alarm → threshold / comprehensive
-- 配套 AlarmNotifier.dispatchForAlarm() 改造
-- 执行前请备份 alarm_notification 表

UPDATE alarm_notification an
    JOIN alarm_record ar ON an.source_id = ar.id
SET an.source_type =
    CASE ar.alarm_type
        WHEN 'COMPREHENSIVE' THEN 'comprehensive'
        ELSE 'threshold'
    END
WHERE an.source_type = 'alarm';
```

**逻辑说明**：

- 仅处理 `source_type = 'alarm'` 的历史记录
- `offline` / `notice` 类型不动
- 通过 JOIN `alarm_record.alarm_type` 判断具体子类型
- `alarm_type` 非 `COMPREHENSIVE` 的一律按 `threshold` 处理（包含历史 NULL/空值数据）

**回滚脚本**（如需）：

```sql
UPDATE alarm_notification
SET source_type = 'alarm'
WHERE source_type IN ('threshold', 'comprehensive');
```

**注意**：回滚前需确认 `AlarmNotifier` 是否已回退到旧版本，否则新产生的记录又会被写成 `threshold`/`comprehensive`。

### 第 3 节：测试更新

**文件**：`server/zwei-iot-alarm/src/test/java/com/zwei/iot/alarm/service/notify/AlarmNotifierTest.java`

**现状**：6 个 alarm 相关测试，构造事件时全部传 `alarmType = "THRESHOLD"`，但未断言 `sourceType`。

**改动**：

#### 3.1 现有 THRESHOLD 测试补充 sourceType 断言

每个 alarm 测试在最后增加：

```java
assertThat(saved.getSourceType()).isEqualTo("threshold");
```

涉及 case（所有构造 `AlarmTriggeredEvent(... "THRESHOLD", ...)` 的测试）：
- `shouldDispatchAlarmWhenRuleMatches`
- `shouldNotDispatchWhenLevelNotMatch`
- `shouldNotDispatchWhenNoRulesForHazardPoint`
- `shouldDedupByRecipientAndChannel`
- `shouldFilterRecipientsByAlarmLevel`
- `shouldUseDefaultLevelWhenRuleLevelMissing`

#### 3.2 新增 COMPREHENSIVE 测试 case

```java
@Test
void shouldDispatchComprehensiveAlarmWithCorrectSourceType() {
    AlarmTriggeredEvent event = new AlarmTriggeredEvent(
        9001L, 100L, 4, "COMPREHENSIVE",
        "小时雨量80mm + 土壤含水率85%，泥石流风险极高", "综合策略命中");

    when(ruleMapper.selectEnabledByHazardPointId(100L))
        .thenReturn(List.of(buildRule(100L, "[4]", List.of(200L), List.of("SYSTEM"))));
    when(userService.selectUserIdsByHazardPointId(100L)).thenReturn(Set.of(200L));

    notifier.onAlarmTriggered(event);

    ArgumentCaptor<Collection<AlarmNotification>> captor = captureSaved();
    AlarmNotification saved = captor.getValue().iterator().next();
    assertThat(saved.getSourceType()).isEqualTo("comprehensive");
    assertThat(saved.getSourceId()).isEqualTo(9001L);
    assertThat(saved.getAlarmLevel()).isEqualTo(4);
    assertThat(saved.getTitle()).startsWith("[综合告警]");
}
```

#### 3.3 现有离线测试不动

`shouldDispatchOfflineEvent` 仍断言 `sourceType = "offline"`，行为不变。

## 不改动的清单（明确记录）

| 文件 / 层 | 理由 |
|---|---|
| `web/src/layout/index.vue` | 另一分支在重构消息面板，避免冲突 |
| `web/src/api/alarmNotification.ts` | 类型定义无需变（sourceType 本就是 string） |
| `AlarmTriggeredEvent.java` | 已有 alarmType 字段，无需扩展 |
| `AlarmEvaluationEngine.java` | 发布事件时已传 `alarmType="THRESHOLD"` |
| `ComprehensiveAlarmJob.java` | 发布事件时已传 `alarmType="COMPREHENSIVE"` |
| `AlarmNotifier.dispatchForOffline()` | 离线事件 sourceType 保持 `offline` |
| `SystemNotifyChannel.java` | 已自动透传 sourceType，无需改 |
| `AlarmStreamPublisher.java` | SSE 路由按 userId，与 sourceType 无关 |
| `alarm_dispatch_rule` 表 / 实体 | 分发规则不关心 alarmType，按 hazardPointId/level 匹配 |

## 数据流验证

### 阈值告警路径（改造后）

```
AlarmEvaluationEngine 判定阈值命中
  → publish AlarmTriggeredEvent(alarmType="THRESHOLD", alarmLevel=3, ...)
  → AlarmNotifier.dispatchForAlarm
  → sourceType = "threshold"
  → buildAndDedup(rules, "threshold", alarmId, "[阈值告警] XXX", content)
  → INSERT alarm_notification (source_type='threshold', source_id=alarmId, ...)
  → AlarmNotifier 继续走 SystemNotifyChannel
  → buildPayloadMap() → SSE data.sourceType = "threshold"
```

### 综合告警路径（改造后）

```
ComprehensiveAlarmJob 执行 Groovy 脚本命中
  → publish AlarmTriggeredEvent(alarmType="COMPREHENSIVE", alarmLevel=4, ...)
  → AlarmNotifier.dispatchForAlarm
  → sourceType = "comprehensive"
  → buildAndDedup(rules, "comprehensive", alarmId, "[综合告警] XXX", content)
  → INSERT alarm_notification (source_type='comprehensive', source_id=alarmId, ...)
  → SSE data.sourceType = "comprehensive"
```

### 离线事件路径（不变）

```
DeviceOfflineEvent → dispatchForOffline → sourceType = "offline"
```

## 风险与缓解

| 风险 | 缓解 |
|---|---|
| 前端旧逻辑 `sourceType === 'alarm'` 失效 | 另一分支同步重构前端，不在本分支处理；DB 迁移已清空旧 `alarm` 值 |
| 历史数据中 `alarm_record.alarm_type` 为 NULL | 迁移脚本 `ELSE 'threshold'` 兜底 |
| 同一条告警误判类型 | `alarm_record.alarm_type` 在写入时已由引擎确定，不会变更 |
| 迁移期间新数据混入 | 迁移脚本幂等（WHERE source_type='alarm'），重复执行无副作用 |

## 验收标准

1. `AlarmNotifierTest` 全部通过（含新增 COMPREHENSIVE case）
2. `mvn test -pl zwei-iot-alarm` BUILD SUCCESS
3. DB 执行迁移后 `SELECT DISTINCT source_type FROM alarm_notification` 不再出现 `alarm`
4. 手动触发阈值/综合告警，`alarm_notification` 表对应记录 `source_type` 正确
5. SSE 推送的 payload 中 `sourceType` 字段为 `threshold` 或 `comprehensive`
