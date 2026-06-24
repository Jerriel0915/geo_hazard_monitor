# 告警类型拆分设计 (阈值/综合)

## 背景

当前告警分发体系存在两个层面的"告警不分类"问题：

1. **分发规则** (`alarm_dispatch_rule.event_type`)：只有 `ALARM`/`OFFLINE` 两值，阈值告警和综合告警共用 `ALARM`，无法按类型分别配置接收人/渠道。
2. **通知记录** (`alarm_notification.source_type`)：只有 `alarm`/`offline`，前端无法区分通知来自阈值还是综合告警。

**目标**：将 `ALARM` 拆分为 `THRESHOLD`（阈值告警）和 `COMPREHENSIVE`（综合告警）两个独立类型，两者都支持四级（蓝/黄/橙/红）等级区分。用户可在分发规则中为阈值告警和综合告警分别配置通知策略。

## 范围边界

### 改动范围（本次）

| 层 | 文件 | 改动 |
|---|---|---|
| 后端 | `AlarmEventType.java` 枚举 | 新增 THRESHOLD / COMPREHENSIVE |
| 后端 | `AlarmDispatchRuleMapper.java` + XML | `matchAlarmRules` 增加 eventType 参数 |
| 后端 | `IAlarmRuleMatcher.java` + Impl | 接口签名增加 eventType |
| 后端 | `AlarmNotifier.java` | 传递 alarmType + 派生 sourceType |
| 后端 | `AlarmDispatchRuleServiceImpl.java` | saveRelations 支持 THRESHOLD/COMPREHENSIVE |
| 后端 | `AlarmDispatchRuleCreateRequest.java` | 验证消息更新 |
| 后端 | `AlarmNotifierTest.java` | mock 签名 + sourceType 断言 |
| 前端 | `alarmDispatch.ts` | `AlarmEventType` 类型扩展 |
| 前端 | `NotificationSetting.vue` | 搜索/表格/表单/验证 |
| DB | `v2026.06.24.001_alarm_type_split.sql` | event_type + source_type 回填 |

### 不改动（另一分支在改，避免冲突）

- `web/src/layout/index.vue` — 通知铃铛面板（消息中心前端）
- `web/src/api/alarmNotification.ts` — 通知中心 API
- `web/src/views/alarm/RealtimeAlarm.vue` — 告警详情页

> `layout/index.vue` 当前路由逻辑按 `sourceType === 'alarm'` 匹配，拆分后旧值已迁移为 `threshold`/`comprehensive`，该等值判断将不再命中 —— 但这正是另一分支要重构的部分，本分支不碰。

## 现状分析

### AlarmTriggeredEvent 已携带 alarmType

```java
public class AlarmTriggeredEvent {
    private Long alarmId;
    private Long hazardPointId;
    private Integer alarmLevel;        // 1-4
    private String alarmType;          // "THRESHOLD" 或 "COMPREHENSIVE"
    private String alarmMessage;
    private String triggerReason;
}
```

| 发布方 | alarmType 值 | 触发场景 |
|---|---|---|
| `AlarmEvaluationEngine` | `THRESHOLD` | 单指标阈值命中 |
| `ComprehensiveAlarmJob` | `COMPREHENSIVE` | Groovy 综合策略命中 |

### 分发规则匹配链路（当前）

```
AlarmNotifier.dispatchForAlarm(event)
  → ruleMatcher.matchAlarmRules(hazardPointId, alarmLevel)   // 不传 alarmType
  → AlarmRuleMatcherImpl.matchAlarmRules(hpId, level)
  → AlarmDispatchRuleMapper.matchAlarmRules(hpIdStr, level)
  → SQL: WHERE event_type = 'ALARM' AND FIND_IN_SET(level, alarm_levels) ...
```

关键问题：SQL 硬编码 `event_type = 'ALARM'`，不区分阈值/综合。

### AlarmEventType 枚举（当前）

```java
public enum AlarmEventType {
    ALARM("ALARM", "告警事件"),
    OFFLINE("OFFLINE", "设备离线");
}
```

### AlarmDispatchRuleServiceImpl.saveRelations（当前）

```java
// 隐患点（仅 ALARM）
if ("ALARM".equals(req.getEventType()) && req.getHazardPointIds() != null) { ... }
// 设备（仅 OFFLINE）
if ("OFFLINE".equals(req.getEventType()) && req.getDeviceIds() != null) { ... }
```

### 前端 NotificationSetting.vue（当前）

- 事件类型 radio：`告警事件(ALARM)` / `设备离线(OFFLINE)`
- `eventType === 'ALARM'` 时显示告警等级 + 隐患点
- `eventType === 'OFFLINE'` 时显示设备

### AlarmNotifier 当前硬编码 sourceType = "alarm"

```java
// line 99
Collection<AlarmNotification> notifications = buildAndDedup(
    rules, "alarm", event.getAlarmId(), title, content);
```

## 设计

### A. 分发规则 event_type 拆分

#### A1. AlarmEventType 枚举

**文件**：`dispatch/domain/enums/AlarmEventType.java`

```java
public enum AlarmEventType {
    THRESHOLD("THRESHOLD", "阈值告警"),
    COMPREHENSIVE("COMPREHENSIVE", "综合告警"),
    OFFLINE("OFFLINE", "设备离线");
    // ... code/label/fromCode 不变
}
```

移除 `ALARM`，新增 `THRESHOLD` + `COMPREHENSIVE`。

#### A2. Mapper — matchAlarmRules 增加 eventType 参数

**文件**：`dispatch/mapper/AlarmDispatchRuleMapper.java`

```java
// 当前
List<AlarmDispatchRule> matchAlarmRules(
    @Param("hazardPointIdStr") String hazardPointIdStr,
    @Param("alarmLevel") String alarmLevel);

// 改造后
List<AlarmDispatchRule> matchAlarmRules(
    @Param("hazardPointIdStr") String hazardPointIdStr,
    @Param("alarmLevel") String alarmLevel,
    @Param("eventType") String eventType);
```

**文件**：`mapper/alarm/AlarmDispatchRuleV2Mapper.xml`

```xml
<!-- 当前 (line 105) -->
AND r.event_type = 'ALARM'

<!-- 改造后 -->
AND r.event_type = #{eventType}
```

其余条件（`FIND_IN_SET`、隐患点通配）不变。

#### A3. IAlarmRuleMatcher + AlarmRuleMatcherImpl

**文件**：`dispatch/service/IAlarmRuleMatcher.java`

```java
// 当前
List<AlarmDispatchRule> matchAlarmRules(Long hazardPointId, String alarmLevel);

// 改造后
List<AlarmDispatchRule> matchAlarmRules(Long hazardPointId, String alarmLevel, String eventType);
```

**文件**：`dispatch/service/impl/AlarmRuleMatcherImpl.java`

```java
@Override
public List<AlarmDispatchRule> matchAlarmRules(Long hazardPointId, String alarmLevel, String eventType) {
    return ruleMapper.matchAlarmRules(
        hazardPointId == null ? null : String.valueOf(hazardPointId),
        alarmLevel,
        eventType);
}
```

#### A4. AlarmNotifier — 传递 alarmType + 派生 sourceType

**文件**：`service/notify/AlarmNotifier.java` `dispatchForAlarm()` (lines 83-101)

```java
private void dispatchForAlarm(AlarmTriggeredEvent event) {
    String alarmType = StringUtils.defaultIfBlank(event.getAlarmType(), "THRESHOLD");

    List<AlarmDispatchRule> rules = ruleMatcher.matchAlarmRules(
        event.getHazardPointId(),
        event.getAlarmLevel() == null ? null : String.valueOf(event.getAlarmLevel()),
        alarmType);                                          // ← NEW: 传 alarmType 作为 eventType

    if (rules == null || rules.isEmpty()) {
        log.debug("无匹配告警规则 alarmId={} type={}", event.getAlarmId(), alarmType);
        return;
    }

    boolean isComprehensive = "COMPREHENSIVE".equals(alarmType);
    String sourceType = isComprehensive ? "comprehensive" : "threshold";
    String typeName = isComprehensive ? "综合告警" : "阈值告警";
    String title = "[" + typeName + "] "
        + StringUtils.defaultString(event.getAlarmMessage(), "告警通知");
    String content = String.format("等级:%s | %s",
        event.getAlarmLevel(),
        StringUtils.defaultString(event.getAlarmMessage(), "-"));

    Collection<AlarmNotification> notifications = buildAndDedup(
        rules, sourceType, event.getAlarmId(), title, content);

    dispatch(notifications);
}
```

**改动点**：

1. `matchAlarmRules` 调用新增第 3 参数 `alarmType`（即 `THRESHOLD`/`COMPREHENSIVE`，直接作为规则的 event_type 匹配值）
2. `sourceType` 由 alarmType 派生（小写：`threshold`/`comprehensive`）
3. `title` 前缀加 `[综合告警]` / `[阈值告警]`
4. `alarmType` 为 null 时兜底为 `THRESHOLD`（理论上不会发生）

#### A5. AlarmDispatchRuleServiceImpl — saveRelations

**文件**：`dispatch/service/impl/AlarmDispatchRuleServiceImpl.java` (lines 250-263)

```java
// 当前
if ("ALARM".equals(req.getEventType()) && req.getHazardPointIds() != null) { ... }
if ("OFFLINE".equals(req.getEventType()) && req.getDeviceIds() != null) { ... }

// 改造后
if (("THRESHOLD".equals(req.getEventType()) || "COMPREHENSIVE".equals(req.getEventType()))
        && req.getHazardPointIds() != null) { ... }
if ("OFFLINE".equals(req.getEventType()) && req.getDeviceIds() != null) { ... }
```

阈值告警和综合告警都保存隐患点关联，逻辑完全一致。

#### A6. DTO 验证消息

**文件**：`dispatch/dto/AlarmDispatchRuleCreateRequest.java`

```java
// 当前 (line 21)
@NotBlank(message = "事件类型不能为空（ALARM/OFFLINE）")

// 改造后
@NotBlank(message = "事件类型不能为空（THRESHOLD/COMPREHENSIVE/OFFLINE）")
```

`AlarmDispatchRule.java` 实体字段注释更新：`ALARM / OFFLINE` → `THRESHOLD / COMPREHENSIVE / OFFLINE`。

#### A7. 前端 alarmDispatch.ts 类型

**文件**：`web/src/api/alarmDispatch.ts`

```typescript
// 当前 (line 3)
export type AlarmEventType = 'ALARM' | 'OFFLINE'

// 改造后
export type AlarmEventType = 'THRESHOLD' | 'COMPREHENSIVE' | 'OFFLINE'
```

#### A8. 前端 NotificationSetting.vue

**文件**：`web/src/views/alarm/NotificationSetting.vue`

改动 8 处：

| # | 位置 | 当前 | 改造后 |
|---|---|---|---|
| 1 | 搜索下拉 (L17-18) | `告警事件=ALARM` / `设备离线=OFFLINE` | `阈值告警=THRESHOLD` / `综合告警=COMPREHENSIVE` / `设备离线=OFFLINE` |
| 2 | 表格事件类型列 (L34-35) | `ALARM→'告警'(danger)` / else `'设备离线'(warning)` | `THRESHOLD→'阈值告警'(danger)` / `COMPREHENSIVE→'综合告警'(danger)` / `OFFLINE→'设备离线'(warning)` |
| 3 | 表格告警等级/隐患点条件 (L41,53) | `v-if="row.eventType === 'ALARM'"` | `v-if="row.eventType === 'THRESHOLD' \|\| row.eventType === 'COMPREHENSIVE'"` (或 `!== 'OFFLINE'`) |
| 4 | 表单 radio (L114-117) | 2 个 radio | 3 个 radio：阈值告警 / 综合告警 / 设备离线 |
| 5 | 表单告警等级条件 (L120) | `v-if="form.eventType === 'ALARM'"` | `v-if="form.eventType !== 'OFFLINE'"` |
| 6 | 表单隐患点条件 (L129) | `v-if="form.eventType === 'ALARM'"` | `v-if="form.eventType !== 'OFFLINE'"` |
| 7 | `FormState` 类型 (L218) | `eventType: 'ALARM' \| 'OFFLINE'` | `eventType: 'THRESHOLD' \| 'COMPREHENSIVE' \| 'OFFLINE'` |
| 8 | `defaultForm` 默认值 (L230) | `eventType: 'ALARM'` | `eventType: 'THRESHOLD'` |

**验证逻辑更新** (L246-266)：

```typescript
// alarmLevels 验证
validator: (_r, _v, cb) => {
  if (form.eventType !== 'OFFLINE' && form.alarmLevels.length === 0)
    cb(new Error('请选择告警等级'))
  else cb()
}

// hazardPointIds 验证
validator: (_r, _v, cb) => {
  if (form.eventType !== 'OFFLINE' && form.hazardPointIds.length === 0)
    cb(new Error('请选择隐患点'))
  else cb()
}
```

**`onEventTypeChange`** (L398-405)：

```typescript
function onEventTypeChange(v: string) {
  if (v === 'OFFLINE') {
    form.hazardPointIds = []
    form.alarmLevels = []
  } else {
    form.deviceIds = []
  }
}
```

**`handleSubmit` payload** (L361-365)：

```typescript
eventType: form.eventType,
alarmLevels: form.eventType !== 'OFFLINE' ? form.alarmLevels : undefined,
hazardPointIds: form.eventType !== 'OFFLINE' ? form.hazardPointIds : undefined,
deviceIds: form.eventType === 'OFFLINE' ? form.deviceIds : undefined,
```

**渠道提示** (L157)：

```html
<div class="form-help" v-if="form.eventType !== 'OFFLINE'">系统消息必选（确保站内可达）</div>
```

### B. 通知 source_type 拆分（已包含在 A4 中）

AlarmNotifier `dispatchForAlarm` 同时完成两件事：

1. 按 alarmType 匹配分发规则（event_type 维度）
2. 写入 source_type（`threshold`/`comprehensive`）

无需额外的 sourceType 代码，A4 一并覆盖。

**SSE 自动透传**：`SystemNotifyChannel.buildPayloadMap()` 已在 payload 中携带 `sourceType`，无需改。

**DB 唯一约束天然隔离**：`uk_notif_dedup (source_type, source_id, recipient_id, channel)` 保证去重。每条 `alarm_record` 要么是 THRESHOLD 要么是 COMPREHENSIVE，拆分后 `source_id` 与 `source_type` 一一对应，约束天然有效。

### C. DB 迁移脚本

**新文件**：`db/upgrade/v2026.06.24.001_alarm_type_split.sql`

```sql
-- =====================================================
-- 告警类型拆分: ALARM → THRESHOLD / COMPREHENSIVE
-- 影响: alarm_dispatch_rule.event_type + alarm_notification.source_type
-- 执行前请备份这两张表
-- =====================================================

-- 1. 分发规则: 原 ALARM → THRESHOLD
--    用户表述"原告警事件改为阈值告警"
UPDATE alarm_dispatch_rule
SET event_type = 'THRESHOLD'
WHERE event_type = 'ALARM';

-- 2. 通知记录: 原 alarm → threshold / comprehensive (按 alarm_record.alarm_type 判断)
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

- 分发规则：所有 `ALARM` 规则迁移为 `THRESHOLD`（保守迁移，用户可按需新增 COMPREHENSIVE 规则）
- 通知记录：通过 JOIN `alarm_record.alarm_type` 精确判断，非 `COMPREHENSIVE` 一律按 `threshold` 兜底
- `offline` / `notice` 类型不动
- 脚本幂等：`WHERE event_type='ALARM'` / `WHERE source_type='alarm'`，重复执行无副作用

### D. 测试更新

**文件**：`AlarmNotifierTest.java`

#### D1. mock 签名更新

所有 `when(ruleMatcher.matchAlarmRules(...))` 的 mock 需增加第 3 参数 `eventType`：

```java
// 当前
when(ruleMatcher.matchAlarmRules(eq(100L), eq("3"))).thenReturn(rules);

// 改造后
when(ruleMatcher.matchAlarmRules(eq(100L), eq("3"), eq("THRESHOLD"))).thenReturn(rules);
```

#### D2. 现有 THRESHOLD 测试补充 sourceType 断言

每个 alarm 测试增加：

```java
assertThat(saved.getSourceType()).isEqualTo("threshold");
assertThat(saved.getTitle()).startsWith("[阈值告警]");
```

#### D3. 新增 COMPREHENSIVE 测试 case

```java
@Test
void shouldDispatchComprehensiveAlarmWithCorrectSourceType() {
    AlarmTriggeredEvent event = new AlarmTriggeredEvent(
        9001L, 100L, 4, "COMPREHENSIVE",
        "小时雨量80mm+土壤含水率85%，泥石流风险极高", "综合策略命中");

    when(ruleMatcher.matchAlarmRules(eq(100L), eq("4"), eq("COMPREHENSIVE")))
        .thenReturn(List.of(buildRule(...)));
    // ... mock recipients ...

    notifier.onAlarmTriggered(event);

    AlarmNotification saved = captureSaved().iterator().next();
    assertThat(saved.getSourceType()).isEqualTo("comprehensive");
    assertThat(saved.getTitle()).startsWith("[综合告警]");
}
```

#### D4. 离线测试不动

`shouldDispatchOfflineEvent` 仍断言 `sourceType = "offline"`，行为不变。

## 不改动清单

| 文件 / 层 | 理由 |
|---|---|
| `web/src/layout/index.vue` | 另一分支在重构消息面板，避免冲突 |
| `web/src/api/alarmNotification.ts` | sourceType 本就是 string，无需改 |
| `AlarmTriggeredEvent.java` | 已有 alarmType 字段 |
| `AlarmEvaluationEngine.java` | 已传 `alarmType="THRESHOLD"` |
| `ComprehensiveAlarmJob.java` | 已传 `alarmType="COMPREHENSIVE"` |
| `AlarmNotifier.dispatchForOffline()` | 离线事件保持 `event_type=OFFLINE` + `source_type=offline` |
| `SystemNotifyChannel.java` | 已自动透传 sourceType |
| `AlarmStreamPublisher.java` | SSE 按 userId 路由，与 sourceType 无关 |

## 数据流验证

### 阈值告警路径（改造后）

```
AlarmEvaluationEngine 判定阈值命中
  → publish AlarmTriggeredEvent(alarmType="THRESHOLD", alarmLevel=3, ...)
  → AlarmNotifier.dispatchForAlarm
  → matchAlarmRules(hpId, "3", "THRESHOLD")
  → SQL: WHERE event_type='THRESHOLD' AND FIND_IN_SET('3', alarm_levels) ...
  → sourceType = "threshold", title = "[阈值告警] XXX"
  → INSERT alarm_notification (source_type='threshold', ...)
  → SSE data.sourceType = "threshold"
```

### 综合告警路径（改造后）

```
ComprehensiveAlarmJob Groovy 脚本命中
  → publish AlarmTriggeredEvent(alarmType="COMPREHENSIVE", alarmLevel=4, ...)
  → AlarmNotifier.dispatchForAlarm
  → matchAlarmRules(hpId, "4", "COMPREHENSIVE")
  → SQL: WHERE event_type='COMPREHENSIVE' AND FIND_IN_SET('4', alarm_levels) ...
  → sourceType = "comprehensive", title = "[综合告警] XXX"
  → INSERT alarm_notification (source_type='comprehensive', ...)
  → SSE data.sourceType = "comprehensive"
```

### 离线事件路径（不变）

```
DeviceOfflineEvent → matchOfflineRules → event_type='OFFLINE' → sourceType="offline"
```

## 风险与缓解

| 风险 | 缓解 |
|---|---|
| 前端 `layout/index.vue` 旧逻辑 `sourceType === 'alarm'` 失效 | 另一分支同步重构；DB 迁移已清空旧 `alarm` 值 |
| 历史 `alarm_record.alarm_type` 为 NULL | 迁移脚本 `ELSE 'threshold'` 兜底 |
| 现有 ALARM 分发规则迁移为 THRESHOLD 后，综合告警无规则匹配 | 用户需按需新增 COMPREHENSIVE 规则；文档提示 |
| 迁移期间新数据混入 | 脚本幂等，WHERE 子句确保只处理旧值 |
| `matchAlarmRules` 签名变更影响其他调用方 | 全局搜索确认仅 `AlarmNotifier` 调用 |

## 验收标准

1. `AlarmNotifierTest` 全部通过（含新增 COMPREHENSIVE case）
2. `mvn test -pl zwei-iot-alarm` BUILD SUCCESS
3. `npm run build` 前端类型检查通过
4. DB 迁移后 `SELECT DISTINCT event_type FROM alarm_dispatch_rule` 不再出现 `ALARM`
5. DB 迁移后 `SELECT DISTINCT source_type FROM alarm_notification` 不再出现 `alarm`
6. 前端分发规则表单可选择三种事件类型，THRESHOLD/COMPREHENSIVE 均显示等级+隐患点
7. 手动触发阈值/综合告警，`alarm_notification` 记录的 `source_type` 和 `title` 正确
