# 告警类型拆分实现计划 (阈值/综合)

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 将告警分发规则的 `event_type` 从 `ALARM/OFFLINE` 拆分为 `THRESHOLD/COMPREHENSIVE/OFFLINE`，同时将通知记录的 `source_type` 从 `alarm` 拆分为 `threshold/comprehensive`。

**架构：** `AlarmTriggeredEvent.alarmType`（THRESHOLD/COMPREHENSIVE）直接作为 dispatch rule 的 `event_type` 匹配值。AlarmNotifier 在调用 `matchAlarmRules` 时传入 alarmType，在写入 `alarm_notification` 时派生小写 sourceType。前端表单从 2 个 radio 变为 3 个，THRESHOLD/COMPREHENSIVE 都显示等级+隐患点。

**技术栈：** Java 17 + Spring Boot + MyBatis (后端) / Vue 3 + TypeScript + Element Plus (前端) / MySQL 8.0 (DB)

**设计文档：** `docs/superpowers/specs/2026-06-24-alarm-source-type-split-design.md`

---

## 文件结构

| # | 文件 | 职责 | 操作 |
|---|---|---|---|
| 1 | `db/upgrade/v2026.06.24.001_alarm_type_split.sql` | DB 迁移: event_type + source_type 回填 | 创建 |
| 2 | `dispatch/domain/enums/AlarmEventType.java` | 事件类型枚举 | 修改 |
| 3 | `dispatch/domain/AlarmDispatchRule.java` | 实体字段注释 | 修改 |
| 4 | `dispatch/dto/AlarmDispatchRuleCreateRequest.java` | DTO 验证消息+注释 | 修改 |
| 5 | `dispatch/mapper/AlarmDispatchRuleMapper.java` | matchAlarmRules 签名 | 修改 |
| 6 | `mapper/alarm/AlarmDispatchRuleV2Mapper.xml` | SQL: `='ALARM'` → `=#{eventType}` | 修改 |
| 7 | `dispatch/service/IAlarmRuleMatcher.java` | 接口签名 | 修改 |
| 8 | `dispatch/service/impl/AlarmRuleMatcherImpl.java` | 实现签名 | 修改 |
| 9 | `service/notify/AlarmNotifier.java` | 传 alarmType + 派生 sourceType | 修改 |
| 10 | `test/.../AlarmNotifierTest.java` | mock 签名 + sourceType 断言 + COMPREHENSIVE case | 修改 |
| 11 | `dispatch/service/impl/AlarmDispatchRuleServiceImpl.java` | saveRelations 支持新类型 | 修改 |
| 12 | `web/src/api/alarmDispatch.ts` | AlarmEventType 类型 | 修改 |
| 13 | `web/src/views/alarm/NotificationSetting.vue` | 搜索/表格/表单/验证 | 修改 |

> **后端文件路径前缀：** `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/`
> **后端测试路径前缀：** `server/zwei-iot-alarm/src/test/java/com/zwei/iot/alarm/`
> **Mapper XML 路径前缀：** `server/zwei-iot-alarm/src/main/resources/`

---

### 任务 1：DB 迁移脚本

**文件：**
- 创建：`db/upgrade/v2026.06.24.001_alarm_type_split.sql`

- [ ] **步骤 1：创建迁移脚本**

创建 `db/upgrade/v2026.06.24.001_alarm_type_split.sql`：

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

- [ ] **步骤 2：验证脚本语法**

运行：
```bash
cd "E:/work/PMO/4.其他项目/sys-交通边坡监测预警/zwei"
# 仅检查文件存在且非空
test -s db/upgrade/v2026.06.24.001_alarm_type_split.sql && echo "OK"
```
预期：输出 `OK`

- [ ] **步骤 3：Commit**

```bash
git add db/upgrade/v2026.06.24.001_alarm_type_split.sql
git commit -m "feat(alarm): DB 迁移脚本 alarm 类型拆分为 threshold/comprehensive"
```

---

### 任务 2：AlarmEventType 枚举 + 实体/DTO 元数据

**文件：**
- 修改：`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/dispatch/domain/enums/AlarmEventType.java`
- 修改：`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/dispatch/domain/AlarmDispatchRule.java`
- 修改：`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/dispatch/dto/AlarmDispatchRuleCreateRequest.java`

- [ ] **步骤 1：更新 AlarmEventType 枚举**

将 `AlarmEventType.java` 的枚举值替换（L8-9）：

```java
// 替换前
    ALARM("ALARM", "告警事件"),
    OFFLINE("OFFLINE", "设备离线");

// 替换后
    THRESHOLD("THRESHOLD", "阈值告警"),
    COMPREHENSIVE("COMPREHENSIVE", "综合告警"),
    OFFLINE("OFFLINE", "设备离线");
```

- [ ] **步骤 2：更新 AlarmDispatchRule 实体注释**

将 `AlarmDispatchRule.java` L22 的注释替换：

```java
// 替换前
    /** 事件类型: ALARM / OFFLINE */

// 替换后
    /** 事件类型: THRESHOLD / COMPREHENSIVE / OFFLINE */
```

- [ ] **步骤 3：更新 DTO 验证消息和注释**

将 `AlarmDispatchRuleCreateRequest.java` 的 3 处替换：

L21 验证消息：
```java
// 替换前
    @NotBlank(message = "事件类型不能为空（ALARM/OFFLINE）")

// 替换后
    @NotBlank(message = "事件类型不能为空（THRESHOLD/COMPREHENSIVE/OFFLINE）")
```

L24 注释：
```java
// 替换前
    /** ALARM 必填；OFFLINE 时为 null */

// 替换后
    /** THRESHOLD/COMPREHENSIVE 必填；OFFLINE 时为 null */
```

L30 注释：
```java
// 替换前
    /** ALARM 必填；元素可为 "*" */

// 替换后
    /** THRESHOLD/COMPREHENSIVE 必填；元素可为 "*" */
```

- [ ] **步骤 4：编译验证**

运行：
```bash
cd "E:/work/PMO/4.其他项目/sys-交通边坡监测预警/zwei/server"
mvn compile -pl zwei-iot-alarm -am -q
```
预期：BUILD SUCCESS（枚举只是添加值+改注释，不破坏编译）

- [ ] **步骤 5：Commit**

```bash
cd "E:/work/PMO/4.其他项目/sys-交通边坡监测预警/zwei"
git add server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/dispatch/domain/enums/AlarmEventType.java \
        server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/dispatch/domain/AlarmDispatchRule.java \
        server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/dispatch/dto/AlarmDispatchRuleCreateRequest.java
git commit -m "refactor(alarm): AlarmEventType 枚举 ALARM → THRESHOLD/COMPREHENSIVE"
```

---

### 任务 3：匹配链路 + AlarmNotifier sourceType（TDD）

**文件：**
- 修改：`server/zwei-iot-alarm/src/test/java/com/zwei/iot/alarm/service/notify/AlarmNotifierTest.java`
- 修改：`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/dispatch/mapper/AlarmDispatchRuleMapper.java`
- 修改：`server/zwei-iot-alarm/src/main/resources/mapper/alarm/AlarmDispatchRuleV2Mapper.xml`
- 修改：`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/dispatch/service/IAlarmRuleMatcher.java`
- 修改：`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/dispatch/service/impl/AlarmRuleMatcherImpl.java`
- 修改：`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/notify/AlarmNotifier.java`

> **注意：** Java 接口签名变更会级联影响所有调用方。步骤 1 先改测试（红灯=编译失败），步骤 2-6 改实现，步骤 7 验证全绿。

- [ ] **步骤 1：更新测试 — mock 签名 + sourceType 断言 + 新增 COMPREHENSIVE case**

修改 `AlarmNotifierTest.java`：

**1a. 所有 `matchAlarmRules` mock 调用增加第 3 参数 `"THRESHOLD"`（共 6 处）：**

L64:
```java
// 替换前
when(ruleMatcher.matchAlarmRules(1L, "4")).thenReturn(List.of(rule));
// 替换后
when(ruleMatcher.matchAlarmRules(1L, "4", "THRESHOLD")).thenReturn(List.of(rule));
```

L84:
```java
// 替换前
when(ruleMatcher.matchAlarmRules(2L, "3")).thenReturn(Collections.emptyList());
// 替换后
when(ruleMatcher.matchAlarmRules(2L, "3", "THRESHOLD")).thenReturn(Collections.emptyList());
```

L98:
```java
// 替换前
when(ruleMatcher.matchAlarmRules(3L, "4")).thenReturn(List.of(rule));
// 替换后
when(ruleMatcher.matchAlarmRules(3L, "4", "THRESHOLD")).thenReturn(List.of(rule));
```

L117:
```java
// 替换前
when(ruleMatcher.matchAlarmRules(4L, "4")).thenReturn(Arrays.asList(ruleA, ruleB));
// 替换后
when(ruleMatcher.matchAlarmRules(4L, "4", "THRESHOLD")).thenReturn(Arrays.asList(ruleA, ruleB));
```

L142:
```java
// 替换前
when(ruleMatcher.matchAlarmRules(6L, "4")).thenReturn(List.of(rule));
// 替换后
when(ruleMatcher.matchAlarmRules(6L, "4", "THRESHOLD")).thenReturn(List.of(rule));
```

L201:
```java
// 替换前
when(ruleMatcher.matchAlarmRules(5L, "4")).thenReturn(List.of(rule));
// 替换后
when(ruleMatcher.matchAlarmRules(5L, "4", "THRESHOLD")).thenReturn(List.of(rule));
```

**1b. 在 `onAlarmTriggered_system_channel_defaults_to_sent_sms_pending` 测试末尾（L165 后）追加 sourceType 断言：**

```java
        // then — sourceType 派生正确
        assertThat(saved).allMatch(n -> "threshold".equals(n.getSourceType()));
        assertThat(saved.get(0).getTitle()).startsWith("[阈值告警]");
```

**1c. 在离线事件 section（L168 注释行）之前，新增 COMPREHENSIVE 测试：**

在 `onAlarmTriggered_system_channel_defaults_to_sent_sms_pending` 方法结束后（L166 `}` 之后），插入：

```java

    @Test
    void onAlarmTriggered_comprehensive_uses_correct_sourceType_and_title() {
        AlarmTriggeredEvent event = new AlarmTriggeredEvent(
            906L, 7L, 4, "COMPREHENSIVE",
            "小时雨量80mm+土壤含水率85%，泥石流风险极高", "综合策略命中");

        AlarmDispatchRule rule = buildRule(15L, "SYSTEM");
        when(ruleMatcher.matchAlarmRules(7L, "4", "COMPREHENSIVE")).thenReturn(List.of(rule));
        when(recipientResolver.resolveUserIds(15L)).thenReturn(new HashSet<>(List.of(80L)));
        when(userService.selectUserById(80L)).thenReturn(buildUser(80L, "0", "user80"));
        when(notificationService.batchCreate(anyList())).thenReturn(1);

        notifier.onAlarmTriggered(event);

        ArgumentCaptor<List<AlarmNotification>> captor =
            ArgumentCaptor.forClass(List.class);
        verify(notificationService, times(1)).batchCreate(captor.capture());
        AlarmNotification saved = captor.getValue().get(0);
        assertThat(saved.getSourceType()).isEqualTo("comprehensive");
        assertThat(saved.getTitle()).startsWith("[综合告警]");
        assertThat(saved.getSourceId()).isEqualTo(906L);
    }
```

- [ ] **步骤 2：运行测试验证编译失败（红灯）**

运行：
```bash
cd "E:/work/PMO/4.其他项目/sys-交通边坡监测预警/zwei/server"
mvn test -pl zwei-iot-alarm -Dtest=AlarmNotifierTest -q 2>&1 | head -30
```
预期：COMPILATION ERROR — `matchAlarmRules` 方法签名不匹配（3 参数 vs 2 参数定义）

- [ ] **步骤 3：更新 Mapper 接口签名**

修改 `AlarmDispatchRuleMapper.java` L33-35：

```java
// 替换前
    /** 告警规则匹配：event_type=ALARM + 等级命中 + 隐患点匹配（含 '*'） */
    List<AlarmDispatchRule> matchAlarmRules(@Param("hazardPointIdStr") String hazardPointIdStr,
                                             @Param("alarmLevel") String alarmLevel);

// 替换后
    /** 告警规则匹配：event_type=#{eventType} + 等级命中 + 隐患点匹配（含 '*'） */
    List<AlarmDispatchRule> matchAlarmRules(@Param("hazardPointIdStr") String hazardPointIdStr,
                                             @Param("alarmLevel") String alarmLevel,
                                             @Param("eventType") String eventType);
```

- [ ] **步骤 4：更新 Mapper XML — SQL 用参数替代硬编码**

修改 `AlarmDispatchRuleV2Mapper.xml` 两处：

L96 注释：
```xml
<!-- 替换前 -->
    <!-- 告警规则匹配：event_type=ALARM + 等级命中 + 隐患点匹配（含 '*'） -->

<!-- 替换后 -->
    <!-- 告警规则匹配：event_type=#{eventType} + 等级命中 + 隐患点匹配（含 '*'） -->
```

L105 条件：
```xml
<!-- 替换前 -->
          AND r.event_type = 'ALARM'

<!-- 替换后 -->
          AND r.event_type = #{eventType}
```

- [ ] **步骤 5：更新 IAlarmRuleMatcher 接口 + Impl**

修改 `IAlarmRuleMatcher.java` L9-12：

```java
// 替换前
    /**
     * 告警事件：匹配 ALARM 类型 + 等级匹配 + 隐患点匹配（含 '*'）
     */
    List<AlarmDispatchRule> matchAlarmRules(Long hazardPointId, String alarmLevel);

// 替换后
    /**
     * 告警事件：匹配 THRESHOLD/COMPREHENSIVE 类型 + 等级匹配 + 隐患点匹配（含 '*'）
     */
    List<AlarmDispatchRule> matchAlarmRules(Long hazardPointId, String alarmLevel, String eventType);
```

修改 `AlarmRuleMatcherImpl.java` L17-22：

```java
// 替换前
    @Override
    public List<AlarmDispatchRule> matchAlarmRules(Long hazardPointId, String alarmLevel) {
        return ruleMapper.matchAlarmRules(
            hazardPointId == null ? null : String.valueOf(hazardPointId),
            alarmLevel);
    }

// 替换后
    @Override
    public List<AlarmDispatchRule> matchAlarmRules(Long hazardPointId, String alarmLevel, String eventType) {
        return ruleMapper.matchAlarmRules(
            hazardPointId == null ? null : String.valueOf(hazardPointId),
            alarmLevel,
            eventType);
    }
```

- [ ] **步骤 6：更新 AlarmNotifier.dispatchForAlarm**

修改 `AlarmNotifier.java` L83-101，替换整个 `dispatchForAlarm` 方法体：

```java
// 替换前 (L83-102)
    private void dispatchForAlarm(AlarmTriggeredEvent event) {
        List<AlarmDispatchRule> rules = ruleMatcher.matchAlarmRules(
            event.getHazardPointId(),
            event.getAlarmLevel() == null ? null : String.valueOf(event.getAlarmLevel()));

        if (rules == null || rules.isEmpty()) {
            log.debug("无匹配告警规则 alarmId={}", event.getAlarmId());
            return;
        }

        String title = "[告警] " + StringUtils.defaultString(event.getAlarmType(), "告警通知");
        String content = String.format("等级:%s | %s",
            event.getAlarmLevel(),
            StringUtils.defaultString(event.getAlarmMessage(), "-"));

        Collection<AlarmNotification> notifications = buildAndDedup(
            rules, "alarm", event.getAlarmId(), title, content);

        dispatch(notifications);
    }

// 替换后
    private void dispatchForAlarm(AlarmTriggeredEvent event) {
        String alarmType = StringUtils.defaultIfBlank(event.getAlarmType(), "THRESHOLD");

        List<AlarmDispatchRule> rules = ruleMatcher.matchAlarmRules(
            event.getHazardPointId(),
            event.getAlarmLevel() == null ? null : String.valueOf(event.getAlarmLevel()),
            alarmType);

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

- [ ] **步骤 7：运行测试验证通过（绿灯）**

运行：
```bash
cd "E:/work/PMO/4.其他项目/sys-交通边坡监测预警/zwei/server"
mvn test -pl zwei-iot-alarm -Dtest=AlarmNotifierTest
```
预期：Tests run: 8, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS

- [ ] **步骤 8：Commit**

```bash
cd "E:/work/PMO/4.其他项目/sys-交通边坡监测预警/zwei"
git add server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/dispatch/mapper/AlarmDispatchRuleMapper.java \
        server/zwei-iot-alarm/src/main/resources/mapper/alarm/AlarmDispatchRuleV2Mapper.xml \
        server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/dispatch/service/IAlarmRuleMatcher.java \
        server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/dispatch/service/impl/AlarmRuleMatcherImpl.java \
        server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/notify/AlarmNotifier.java \
        server/zwei-iot-alarm/src/test/java/com/zwei/iot/alarm/service/notify/AlarmNotifierTest.java
git commit -m "feat(alarm): 匹配链路按 eventType 过滤 + sourceType 派生 threshold/comprehensive"
```

---

### 任务 4：ServiceImpl saveRelations 支持新类型

**文件：**
- 修改：`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/dispatch/service/impl/AlarmDispatchRuleServiceImpl.java`

- [ ] **步骤 1：更新 saveRelations 条件**

修改 `AlarmDispatchRuleServiceImpl.java` L250-251：

```java
// 替换前
        // 隐患点（仅 ALARM）
        if ("ALARM".equals(req.getEventType()) && req.getHazardPointIds() != null) {

// 替换后
        // 隐患点（THRESHOLD / COMPREHENSIVE）
        boolean isAlarmType = "THRESHOLD".equals(req.getEventType())
            || "COMPREHENSIVE".equals(req.getEventType());
        if (isAlarmType && req.getHazardPointIds() != null) {
```

- [ ] **步骤 2：编译验证**

运行：
```bash
cd "E:/work/PMO/4.其他项目/sys-交通边坡监测预警/zwei/server"
mvn compile -pl zwei-iot-alarm -am -q
```
预期：BUILD SUCCESS

- [ ] **步骤 3：Commit**

```bash
cd "E:/work/PMO/4.其他项目/sys-交通边坡监测预警/zwei"
git add server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/dispatch/service/impl/AlarmDispatchRuleServiceImpl.java
git commit -m "fix(alarm): saveRelations 支持 THRESHOLD/COMPREHENSIVE 保存隐患点"
```

---

### 任务 5：前端 alarmDispatch.ts 类型

**文件：**
- 修改：`web/src/api/alarmDispatch.ts`

- [ ] **步骤 1：更新 AlarmEventType 类型**

修改 `alarmDispatch.ts` L3：

```typescript
// 替换前
export type AlarmEventType = 'ALARM' | 'OFFLINE'

// 替换后
export type AlarmEventType = 'THRESHOLD' | 'COMPREHENSIVE' | 'OFFLINE'
```

- [ ] **步骤 2：验证前端类型检查**

运行：
```bash
cd "E:/work/PMO/4.其他项目/sys-交通边坡监测预警/zwei/web"
npx vue-tsc --noEmit 2>&1 | grep -E "alarmDispatch|error" | head -20
```
预期：`alarmDispatch.ts` 本身无错误（但 `NotificationSetting.vue` 会报 `'ALARM'` 不在类型范围内的错误 — 下一个任务修复）

- [ ] **步骤 3：Commit**

```bash
cd "E:/work/PMO/4.其他项目/sys-交通边坡监测预警/zwei"
git add web/src/api/alarmDispatch.ts
git commit -m "refactor(web): AlarmEventType 类型 ALARM → THRESHOLD/COMPREHENSIVE"
```

---

### 任务 6：前端 NotificationSetting.vue

**文件：**
- 修改：`web/src/views/alarm/NotificationSetting.vue`

> 8 处改动：搜索下拉 / 表格列 / 表格条件 ×2 / 表单 radio / 表单条件 ×2 / 脚本逻辑 ×4

- [ ] **步骤 1：搜索下拉增加选项**

修改 L17-18：

```html
<!-- 替换前 -->
        <el-option label="告警事件" value="ALARM" />
        <el-option label="设备离线" value="OFFLINE" />

<!-- 替换后 -->
        <el-option label="阈值告警" value="THRESHOLD" />
        <el-option label="综合告警" value="COMPREHENSIVE" />
        <el-option label="设备离线" value="OFFLINE" />
```

- [ ] **步骤 2：表格事件类型列改造**

修改 L32-38（整个 `el-table-column`）：

```html
<!-- 替换前 -->
          <el-table-column label="事件类型" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="row.eventType === 'ALARM' ? 'danger' : 'warning'" size="small">
                {{ row.eventType === 'ALARM' ? '告警' : '设备离线' }}
              </el-tag>
            </template>
          </el-table-column>

<!-- 替换后 -->
          <el-table-column label="事件类型" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="row.eventType === 'OFFLINE' ? 'warning' : 'danger'" size="small">
                {{ eventTypeLabel(row.eventType) }}
              </el-tag>
            </template>
          </el-table-column>
```

- [ ] **步骤 3：表格条件显示 — 告警等级列**

修改 L41：

```html
<!-- 替换前 -->
              <template v-if="row.eventType === 'ALARM'">

<!-- 替换后 -->
              <template v-if="row.eventType !== 'OFFLINE'">
```

- [ ] **步骤 4：表格条件显示 — 隐患点/设备列**

修改 L53：

```html
<!-- 替换前 -->
              <template v-if="row.eventType === 'ALARM'">

<!-- 替换后 -->
              <template v-if="row.eventType !== 'OFFLINE'">
```

- [ ] **步骤 5：表单 radio 改为 3 选项**

修改 L114-117：

```html
<!-- 替换前 -->
          <el-radio-group v-model="form.eventType" @change="onEventTypeChange">
            <el-radio label="ALARM">告警事件</el-radio>
            <el-radio label="OFFLINE">设备离线</el-radio>
          </el-radio-group>

<!-- 替换后 -->
          <el-radio-group v-model="form.eventType" @change="onEventTypeChange">
            <el-radio label="THRESHOLD">阈值告警</el-radio>
            <el-radio label="COMPREHENSIVE">综合告警</el-radio>
            <el-radio label="OFFLINE">设备离线</el-radio>
          </el-radio-group>
```

- [ ] **步骤 6：表单条件显示 — 告警等级 + 隐患点 + 渠道提示**

修改 L120（告警等级 v-if）：

```html
<!-- 替换前 -->
        <el-form-item label="告警等级" prop="alarmLevels" v-if="form.eventType === 'ALARM'">

<!-- 替换后 -->
        <el-form-item label="告警等级" prop="alarmLevels" v-if="form.eventType !== 'OFFLINE'">
```

修改 L129（隐患点 v-if）：

```html
<!-- 替换前 -->
        <el-form-item label="隐患点" prop="hazardPointIds" v-if="form.eventType === 'ALARM'">

<!-- 替换后 -->
        <el-form-item label="隐患点" prop="hazardPointIds" v-if="form.eventType !== 'OFFLINE'">
```

修改 L157（渠道提示 v-if）：

```html
<!-- 替换前 -->
          <div class="form-help" v-if="form.eventType === 'ALARM'">系统消息必选（确保站内可达）</div>

<!-- 替换后 -->
          <div class="form-help" v-if="form.eventType !== 'OFFLINE'">系统消息必选（确保站内可达）</div>
```

- [ ] **步骤 7：脚本逻辑 — 类型/默认值/验证/onChange/submit**

**7a. FormState 类型** (L218)：

```typescript
// 替换前
  eventType: 'ALARM' | 'OFFLINE'

// 替换后
  eventType: 'THRESHOLD' | 'COMPREHENSIVE' | 'OFFLINE'
```

**7b. defaultForm 默认值** (L230)：

```typescript
// 替换前
  eventType: 'ALARM',

// 替换后
  eventType: 'THRESHOLD',
```

**7c. alarmLevels 验证** (L248)：

```typescript
// 替换前
      if (form.eventType === 'ALARM' && form.alarmLevels.length === 0)

// 替换后
      if (form.eventType !== 'OFFLINE' && form.alarmLevels.length === 0)
```

**7d. hazardPointIds 验证** (L255)：

```typescript
// 替换前
      if (form.eventType === 'ALARM' && form.hazardPointIds.length === 0)

// 替换后
      if (form.eventType !== 'OFFLINE' && form.hazardPointIds.length === 0)
```

**7e. onEventTypeChange** (L398-405)：

```typescript
// 替换前
function onEventTypeChange(v: string) {
  if (v === 'ALARM') {
    form.deviceIds = []
  } else {
    form.hazardPointIds = []
    form.alarmLevels = []
  }
}

// 替换后
function onEventTypeChange(v: string) {
  if (v === 'OFFLINE') {
    form.hazardPointIds = []
    form.alarmLevels = []
  } else {
    form.deviceIds = []
  }
}
```

**7f. handleSubmit payload** (L361-365)：

```typescript
// 替换前
      eventType: form.eventType,
      alarmLevels: form.eventType === 'ALARM' ? form.alarmLevels : undefined,
      channels: form.channels,
      hazardPointIds: form.eventType === 'ALARM' ? form.hazardPointIds : undefined,
      deviceIds: form.eventType === 'OFFLINE' ? form.deviceIds : undefined,

// 替换后
      eventType: form.eventType,
      alarmLevels: form.eventType !== 'OFFLINE' ? form.alarmLevels : undefined,
      channels: form.channels,
      hazardPointIds: form.eventType !== 'OFFLINE' ? form.hazardPointIds : undefined,
      deviceIds: form.eventType === 'OFFLINE' ? form.deviceIds : undefined,
```

**7g. 新增 eventTypeLabel 辅助函数**（在 `levelLabel` 函数前，L412 之前插入）：

```typescript
function eventTypeLabel(et: string) {
  return ({
    THRESHOLD: '阈值告警',
    COMPREHENSIVE: '综合告警',
    OFFLINE: '设备离线'
  } as Record<string, string>)[et] || et
}
```

- [ ] **步骤 8：验证前端编译**

运行：
```bash
cd "E:/work/PMO/4.其他项目/sys-交通边坡监测预警/zwei/web"
npx vue-tsc --noEmit 2>&1 | grep -i "NotificationSetting\|error TS" | head -20
```
预期：无 `error TS` 输出（或仅有与本次改动无关的已有警告）

- [ ] **步骤 9：Commit**

```bash
cd "E:/work/PMO/4.其他项目/sys-交通边坡监测预警/zwei"
git add web/src/views/alarm/NotificationSetting.vue
git commit -m "feat(web): 通知规则表单事件类型拆分为阈值/综合/离线三类"
```

---

### 任务 7：全量验证

- [ ] **步骤 1：后端全量编译 + 测试**

运行：
```bash
cd "E:/work/PMO/4.其他项目/sys-交通边坡监测预警/zwei/server"
mvn clean test -pl zwei-iot-alarm -am
```
预期：BUILD SUCCESS，Tests run: 60+（含新增 COMPREHENSIVE case），0 Failures

- [ ] **步骤 2：前端构建**

运行：
```bash
cd "E:/work/PMO/4.其他项目/sys-交通边坡监测预警/zwei/web"
npm run build
```
预期：vue-tsc 类型检查通过，vite build 输出 `dist/`

- [ ] **步骤 3：手动验证（可选，需后端运行）**

1. 执行 DB 迁移脚本
2. 打开通知规则页面，新建规则 — 确认事件类型有 3 个 radio
3. 选"阈值告警" — 确认等级+隐患点显示
4. 选"综合告警" — 确认等级+隐患点显示
5. 选"设备离线" — 确认设备显示
6. 保存后重新打开编辑 — 确认值回显正确

- [ ] **步骤 4：更新任务状态**

所有任务完成后，确认 git 工作区干净：
```bash
cd "E:/work/PMO/4.其他项目/sys-交通边坡监测预警/zwei"
git status
```
预期：nothing to commit, working tree clean
