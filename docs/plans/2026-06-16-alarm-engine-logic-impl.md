# 告警评估引擎逻辑调整 — 实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 让 `AlarmEvaluationEngine` 实现隐患点判据优先级排他、多判据候选合并为最高等级单条告警、每等级独立计数与"不连续即重置"语义。

**Architecture:** 仅修改 `AlarmEvaluationEngine.java`：删除综合策略实时调用与三个不再使用的依赖；重构 `evaluateCriteria` 为"逐判据+逐等级收集候选 → 取最高 → 单条入库"。`AlarmDedupService` 与 `CriteriaEvaluator` 不变。

**Tech Stack:** Java 17 + Spring Boot 4 + JUnit 5 + Mockito + MyBatis + Redis + MySQL + MQTT(mica-mqtt)

**Design Doc:** `docs/plans/2026-06-16-alarm-engine-logic-design.md`

---

## Task 1: 修改 AlarmEvaluationEngine（构造器精简 + evaluate 调整 + evaluateCriteria 重构 + 删除 evaluateRealtimeStrategies）

**Files:**
- Modify: `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/engine/AlarmEvaluationEngine.java`

### Step 1: 备份查看现有文件

Run: `git diff HEAD -- server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/engine/AlarmEvaluationEngine.java`
Expected: 空输出（还未修改）

### Step 2: 修改 imports + 字段 + 构造器（删除三个依赖）

删除以下 imports：
```java
import com.zwei.iot.alarm.mapper.AlarmStrategyHazardPointMapper;
import com.zwei.iot.alarm.mapper.AlarmStrategyMapper;
```

字段区删除：
```java
private final AlarmStrategyMapper strategyMapper;
private final AlarmStrategyHazardPointMapper bindingMapper;
private final GroovyScriptExecutor groovyScriptExecutor;
```

构造器签名删除三个参数，构造器体删除三行赋值。注意保留 `monitorContentMapper`（`resolveMonitorTypeId` 仍需它）。

### Step 3: 重写 `evaluate` 方法体

将原 `evaluate` 方法替换为：

```java
private void evaluate(MonitorDataIngestedEvent event) {
    if (event.getValue() == null) {
        log.debug("null value, skip");
        return;
    }

    List<Long> hazardPointIds = hazardRelationService.getHazardPointIdsByDeviceIds(
            Collections.singletonList(event.getDeviceId()));
    if (hazardPointIds.isEmpty()) {
        log.debug("no hazard point for deviceId={}", event.getDeviceId());
        return;
    }

    // 查询传感器属性
    Long monitorContentId = null;
    String sensorAttrCode = event.getAttrCode();
    try {
        SensorMetadata metadata = sensorQueryService.requireSensorMetadata(event.getDeviceId(), event.getSensorCode());
        for (SensorAttribute attr : metadata.attributes()) {
            if (sensorAttrCode.equals(attr.getAttrCode())) {
                monitorContentId = attr.getMonitorContentId();
                break;
            }
        }
    } catch (Exception e) {
        log.debug("sensor metadata fail: {}", e.getMessage());
        return;
    }

    // ── 优先级 1: 隐患点专属判据（存在则只评估它，不再走监测类型兜底） ──
    List<AlarmCriteria> hpCriteria = new ArrayList<>();
    for (Long hpId : hazardPointIds) hpCriteria.addAll(criteriaCache.getByHazardPointId(hpId));

    if (!hpCriteria.isEmpty()) {
        evaluateCriteria(event, hpCriteria, hazardPointIds, monitorContentId);
        return;
    }

    // ── 优先级 2: 仅当无隐患点判据时，使用监测类型兜底判据 ──
    if (monitorContentId != null) {
        Long monitorTypeId = resolveMonitorTypeId(monitorContentId);
        if (monitorTypeId != null) {
            List<AlarmCriteria> mtCriteria = criteriaCache.getByMonitorTypeId(monitorTypeId);
            if (!mtCriteria.isEmpty()) {
                evaluateCriteria(event, mtCriteria, hazardPointIds, monitorContentId);
            }
        }
    }
}
```

### Step 4: 重写 `evaluateCriteria` 方法

将原 `evaluateCriteria` 整体替换为以下实现。在类顶部增加常量与 record：

```java
private static final Map<String, Integer> LEVEL_VALUES = Map.of(
        "red", 4, "orange", 3, "yellow", 2, "blue", 1);

private record Candidate(AlarmCriteria criteria, int level, Long effectiveHpId) {}
```

新方法体：

```java
/**
 * 逐判据、逐等级独立评估；候选等级合并为最高级单条告警。
 *
 * @return true 如果至少产生了一条候选告警
 */
private boolean evaluateCriteria(MonitorDataIngestedEvent event, List<AlarmCriteria> criteriaList,
                                 List<Long> hazardPointIds, Long monitorContentId) {
    List<Candidate> candidates = new ArrayList<>();

    for (AlarmCriteria criteria : criteriaList) {
        Long effectiveHpId = criteria.getHazardPointId();
        if (effectiveHpId == null && !hazardPointIds.isEmpty()) effectiveHpId = hazardPointIds.get(0);
        if (effectiveHpId == null) continue;

        Map<String, Double> subjectValues = new HashMap<>();
        subjectValues.put(event.getAttrCode(), event.getValue());

        Map<String, LevelConfig> configMap = criteriaEvaluator.parseLevelConfig(criteria.getLevelConfig());
        int persistCount  = criteria.getPersistCount()  != null ? criteria.getPersistCount()  : 1;
        int silencePeriod = criteria.getSilencePeriod() != null ? criteria.getSilencePeriod() : 0;

        // 逐等级独立评估：满足累加，未满足仅重置当前等级
        for (Map.Entry<String, LevelConfig> entry : configMap.entrySet()) {
            int level = LEVEL_VALUES.getOrDefault(entry.getKey(), 0);
            if (level <= 0) continue;

            boolean satisfied = criteriaEvaluator.evaluateLevel(entry.getValue(), subjectValues);
            if (!satisfied) {
                dedupService.clearPreTrigger(criteria.getId(), effectiveHpId, level);
                continue;
            }
            if (dedupService.shouldTriggerAlarm(criteria.getId(), effectiveHpId, level,
                                                persistCount, silencePeriod)) {
                candidates.add(new Candidate(criteria, level, effectiveHpId));
            }
        }
    }

    if (candidates.isEmpty()) return false;

    // 候选合并：取最高等级；同等级取首个
    Candidate winner = candidates.stream()
            .max(Comparator.comparingInt(Candidate::level))
            .orElseThrow();
    String hpName = getHazardPointName(winner.effectiveHpId);
    AlarmRecord record = AlarmRecord.builder()
            .hazardPointId(winner.effectiveHpId).hazardPointName(hpName)
            .deviceId(event.getDeviceId()).sensorId(event.getSensorId())
            .monitorContentId(monitorContentId)
            .alarmLevel(winner.level).alarmLevelText(AlarmConstants.resolveLevelText(winner.level))
            .alarmType("THRESHOLD").alarmMessage("阈值告警: " + winner.criteria.getName())
            .criteriaId(winner.criteria.getId())
            .currentValue(event.getValue() != null ? new BigDecimal(event.getValue()) : null)
            .createBy(AlarmConstants.SYSTEM_OPERATOR).createTime(new Date())
            .build();
    AlarmRecord saved = alarmRecordService.createOrUpdateAlarm(record);
    eventPublisher.publishEvent(new AlarmTriggeredEvent(saved.getId(), saved.getHazardPointId(),
            saved.getAlarmLevel(), saved.getAlarmType(), saved.getAlarmMessage()));
    log.info("告警触发 id={} level={} criteria={} (candidates={})",
            saved.getId(), winner.level, winner.criteria.getId(), candidates.size());
    return true;
}
```

### Step 5: 删除 `evaluateRealtimeStrategies` 整个方法

整个方法块（约 38 行）从 `private void evaluateRealtimeStrategies(...)` 到对应 `}` 全部删除。

### Step 6: 添加 `Comparator` 与 `LevelConfig` 的 import

```java
import com.zwei.iot.alarm.domain.LevelConfig;
import java.util.Comparator;
```

`AlarmStrategy` import 如果未再使用也可删除（确认仅 `evaluateRealtimeStrategies` 用到）。

### Step 7: 编译验证

Run: `cd server && mvn -pl zwei-iot-alarm -am compile -q`
Expected: BUILD SUCCESS

如有 unused import / 字段未消除的编译错误，按编译器提示修正。

### Step 8: Commit

```bash
git add server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/engine/AlarmEvaluationEngine.java
git commit -m "refactor(alarm): 重构评估引擎—隐患点判据排他+候选合并+等级独立计数"
```

---

## Task 2: 运行已有单元测试，验证无回归

**Files:**
- Test: `server/zwei-iot-alarm/src/test/java/com/zwei/iot/alarm/service/engine/AlarmDedupServiceTest.java`
- Test: `server/zwei-iot-alarm/src/test/java/com/zwei/iot/alarm/service/engine/CriteriaEvaluatorTest.java`
- Test: `server/zwei-iot-alarm/src/test/java/com/zwei/iot/alarm/service/impl/AlarmRecordServiceImplTest.java`

### Step 1: 跑全部 alarm 模块测试

Run: `cd server && mvn -pl zwei-iot-alarm -am test -q`
Expected: BUILD SUCCESS, 0 failures

### Step 2: 如失败，记录失败用例与原因

如出现 `AlarmDedupServiceTest` 失败 → 说明触动了既有 dedup 行为，回看 Task 1 Step 4 是否误改了 `AlarmDedupService`。
如出现编译错误 → 回到 Task 1 Step 6-7 检查 imports。

修复后回到 Step 1 重跑。

---

## Task 3: 准备数据库测试数据

**Files:**
- 无新文件，直接在 MySQL 执行 SQL

### Step 1: 查询现有设备与隐患点 ID

```sql
SELECT d.id AS device_id, d.device_name, dhp.hazard_point_id, hp.name AS hp_name
FROM device d
JOIN device_hazard_point dhp ON dhp.device_id = d.id
JOIN hazard_point hp ON hp.id = dhp.hazard_point_id
WHERE d.del_flag = 0
LIMIT 5;
```

记下：`device_id` / `hazard_point_id`（HP1）→ 在后续 SQL 中替换 `:device_id` / `:hp_id` 占位符。

### Step 2: 查询雨量传感器与监测内容

```sql
SELECT ds.id AS sensor_id, ds.sensor_code, sa.attr_code, sa.monitor_content_id, mc.monitor_type_id
FROM device_sensor ds
JOIN sensor_attribute sa ON sa.sensor_id = ds.id
JOIN monitor_content mc ON mc.id = sa.monitor_content_id
WHERE ds.device_id = :device_id
  AND sa.attr_code = 'rainfall'
LIMIT 1;
```

记下：`sensor_id` / `sensor_code` / `monitor_content_id` / `monitor_type_id`。

> 如未找到 `attr_code='rainfall'`：把 `rainfall` 替换为该设备实际的雨量属性 code，后续 MQTT payload 也用同一 code。

### Step 3: 清理历史告警记录（避免干扰）

```sql
DELETE FROM alarm_record WHERE device_id = :device_id;
DELETE FROM alarm_record_trigger_detail WHERE alarm_record_id NOT IN (SELECT id FROM alarm_record);
-- 如有 alarm_record_action_log 外键约束，按需清理
```

### Step 4: 清理 Redis 计数 key（避免历史计数干扰）

```bash
redis-cli --scan --pattern 'alarm:pre-trigger:*' | xargs -r redis-cli del
redis-cli --scan --pattern 'alarm:last-trigger:*' | xargs -r redis-cli del
```

### Step 5: 插入三条测试判据

> 把下面 SQL 中的 `:hp_id` / `:monitor_type_id` / `:monitor_content_id` 替换为前面查到的实际值。

```sql
-- CR_HP_A: 隐患点判据，rainfall>10 → 蓝色，persistCount=3
INSERT INTO alarm_criteria
  (name, monitor_type_id, monitor_content_id, hazard_point_id,
   level_config, persist_count, silence_period, is_enabled, version, del_flag, create_time, create_by)
VALUES
  ('CR_HP_A_rain10_blue', :monitor_type_id, :monitor_content_id, :hp_id,
   '{"blue":{"logicOperator":"AND","conditions":[{"subject":"rainfall","subjectType":"CONTENT","operator":"GT","threshold":10}],"description":"雨量>10mm"}}',
   3, 0, 1, 1, 0, NOW(), 'system');

-- CR_HP_B: 隐患点判据，rainfall>50 → 红色，persistCount=2
INSERT INTO alarm_criteria
  (name, monitor_type_id, monitor_content_id, hazard_point_id,
   level_config, persist_count, silence_period, is_enabled, version, del_flag, create_time, create_by)
VALUES
  ('CR_HP_B_rain50_red', :monitor_type_id, :monitor_content_id, :hp_id,
   '{"red":{"logicOperator":"AND","conditions":[{"subject":"rainfall","subjectType":"CONTENT","operator":"GT","threshold":50}],"description":"雨量>50mm"}}',
   2, 0, 1, 1, 0, NOW(), 'system');

-- CR_MT_C: 监测类型判据（hazard_point_id IS NULL），rainfall>5 → 蓝色，persistCount=1
-- 期望被 CR_HP_A/B 排他，永不触发
INSERT INTO alarm_criteria
  (name, monitor_type_id, monitor_content_id, hazard_point_id,
   level_config, persist_count, silence_period, is_enabled, version, del_flag, create_time, create_by)
VALUES
  ('CR_MT_C_rain5_blue', :monitor_type_id, :monitor_content_id, NULL,
   '{"blue":{"logicOperator":"AND","conditions":[{"subject":"rainfall","subjectType":"CONTENT","operator":"GT","threshold":5}],"description":"雨量>5mm"}}',
   1, 0, 1, 1, 0, NOW(), 'system');
```

### Step 6: 验证插入成功 + 触发判据缓存刷新

```sql
SELECT id, name, hazard_point_id, persist_count, is_enabled
FROM alarm_criteria
WHERE name LIKE 'CR_%' AND del_flag = 0;
```
Expected: 3 行（CR_HP_A、CR_HP_B、CR_MT_C）

> 判据缓存 `CriteriaCacheService` 默认 5 分钟 TTL。为立即生效，**重启后端** 或调用 `POST /api/v1/iot/alarm/criteria/{id}/toggle` 两次（disable→enable）触发缓存失效。最稳妥是重启后端。

### Step 7: Commit 测试 SQL（可选）

如希望保留测试数据脚本，可保存到 `db/upgrade/2026-06-16-alarm-test-data.sql`。否则跳过。

---

## Task 4: MQTT 模拟测试 — 用例 1（优先级排他）

### Step 1: 确认后端服务已启动并加载新判据

启动 `com.zwei.RuoYiApplication`（profile=local），观察日志：
- `Started RuoYiApplication in ...`
- 无 `CriteriaCacheService` 报错

### Step 2: 查询设备 MQTT 凭据

```sql
SELECT mqtt_username, mqtt_password, client_id
FROM device
WHERE id = :device_id;
```

或使用设备的 `deviceCode` 与 `secretKey`（参考 `MqttDeviceAuthService`）。

### Step 3: 发送 value=6（仅满足 CR_MT_C，HP 判据都不满足）

使用 mosquitto_pub 或 mica-mqtt 测试客户端：

```bash
mosquitto_pub -h 127.0.0.1 -p 1883 \
  -u <device_mqtt_username> -P <device_mqtt_password> \
  -t 'sys/<deviceCode>/<sensorCode>/rainfall' \
  -m '{"v":6,"t":"2026-06-16T10:00:00"}'
```

> 实际 topic 与 payload 格式以项目 `MonitorTopicParser` 为准（参考 `zwei-iot-timeseries` 的 sys/gb 解析器）。如不确定，先抓一条真实设备上报报文做模板。

### Step 4: 验证 0 条告警

```sql
SELECT COUNT(*) FROM alarm_record WHERE device_id = :device_id;
```
Expected: `0`

应用日志期望：`no hazard point criteria matched` 或 `null value, skip` 不应出现；应是 dedup 计数累加但未达 persistCount（CR_HP_A 计数 0 因为 6<10）。

### Step 5: 记录结果到 `docs/plans/test-results.md`

新建文件，记录：用例名、输入、SQL 输出、应用日志关键行、是否通过。

---

## Task 5: MQTT 模拟测试 — 用例 2（计数器独立累加）

### Step 1: 连续发送三次 value=11

每条间隔 5 秒（避免被设备上报频率限流）：

```bash
for v in 11 11 11; do
  mosquitto_pub -h 127.0.0.1 -p 1883 \
    -u <user> -P <pass> \
    -t 'sys/<deviceCode>/<sensorCode>/rainfall' \
    -m "{\"v\":$v,\"t\":\"2026-06-16T10:0${v}:00\"}"
  sleep 5
done
```

### Step 2: 验证 1 条 level=1 告警

```sql
SELECT id, alarm_level, alarm_level_text, criteria_id, trigger_count, alarm_message, create_time
FROM alarm_record
WHERE device_id = :device_id
ORDER BY id DESC LIMIT 1;
```
Expected:
- 1 行
- `alarm_level=1`, `alarm_level_text='蓝色'`
- `criteria_id` = CR_HP_A.id
- `trigger_count=1`
- `alarm_message='阈值告警: CR_HP_A_rain10_blue'`

应用日志：`告警触发 id=... level=1 criteria=<CR_HP_A.id> (candidates=1)`

### Step 3: 验证 CR_HP_B 计数独立

```bash
redis-cli GET alarm:pre-trigger:<CR_HP_B.id>:<hp_id>:4
```
Expected: `(nil)` 或不存在（11 不满足 red>50，从未累加；且每次评估都被 clearPreTrigger 重置）

### Step 4: 记录结果

---

## Task 6: MQTT 模拟测试 — 用例 3（等级独立重置）

### Step 1: 清理上一用例的告警，重置 Redis 计数

```sql
DELETE FROM alarm_record WHERE device_id = :device_id;
```

```bash
redis-cli --scan --pattern 'alarm:pre-trigger:*' | xargs -r redis-cli del
redis-cli --scan --pattern 'alarm:last-trigger:*' | xargs -r redis-cli del
```

### Step 2: 发送序列 `60, 20, 60, 60`，每条间隔 5 秒

```bash
for v in 60 20 60 60; do
  mosquitto_pub ... -m "{\"v\":$v,...}"
  sleep 5
done
```

### Step 3: 预期计数变化

| 步骤 | value | blue 计数(>10) | red 计数(>50) | 备注 |
|---|---|---|---|---|
| 1 | 60 | 1 | 1 | blue+red 均满足 |
| 2 | 20 | 2 | **0（重置）** | blue 满足，red 不满足→重置 |
| 3 | 60 | 3 ✅ | 1 | blue 达到 persistCount=3 → 候选 level=1 |
| 4 | 60 | （已 trigger，重新计数为 1，但 silence 接下来控制）| 2 ✅ | red 达到 persistCount=2 → 候选 level=4 |

### Step 4: 验证最终告警

```sql
SELECT alarm_level, alarm_level_text, criteria_id, trigger_count
FROM alarm_record WHERE device_id = :device_id ORDER BY id DESC;
```
Expected:
- 第 3 步产生 level=1 告警（CR_HP_A 触发）
- 第 4 步再次评估时，CR_HP_B 达到 red persistCount=2，触发 level=4 告警，winner=red
- 第 4 步的告警**应该升级**：因为 `createOrUpdateAlarm` 同 `criteriaId` 已存在记录 → 更新为 level=4，trigger_count=2

实际期望：
- 唯一一条告警记录，`alarm_level=4`，`trigger_count=2`，但 `criteria_id` 不同（第 3 步是 CR_HP_A，第 4 步是 CR_HP_B → 新建一条）

> 注意：`createOrUpdateAlarm` 按 `criteriaId` 去重；不同 criteriaId 会建两条记录。所以最终会有 2 条 alarm_record：1 条 level=1 (CR_HP_A)，1 条 level=4 (CR_HP_B)。这是正确行为。

### Step 5: 验证日志中 `(candidates=N)` 数字

第 3 步日志：`告警触发 ... level=1 criteria=<CR_HP_A.id> (candidates=1)`
第 4 步日志：`告警触发 ... level=4 criteria=<CR_HP_B.id> (candidates=1)`

### Step 6: 记录结果

---

## Task 7: MQTT 模拟测试 — 用例 4（最高等级胜出）

### Step 1: 清理

```sql
DELETE FROM alarm_record WHERE device_id = :device_id;
```
```bash
redis-cli --scan --pattern 'alarm:pre-trigger:*' | xargs -r redis-cli del
redis-cli --scan --pattern 'alarm:last-trigger:*' | xargs -r redis-cli del
```

### Step 2: 连续发送 value=60 两次（间隔 5 秒）

第 1 次：blue 计数 1，red 计数 1，均未达阈值（persistCount=3 和 2）
第 2 次：blue 计数 2，red 计数 2 → **red 达到 persistCount=2 触发**，blue 未达 persistCount=3

候选集合：`[red(level=4)]`
winner=red → 写一条 level=4 告警，criteriaId=CR_HP_B.id

### Step 3: 验证

```sql
SELECT alarm_level, alarm_level_text, criteria_id, trigger_count
FROM alarm_record WHERE device_id = :device_id;
```
Expected:
- 1 行
- `alarm_level=4`, `alarm_level_text='红色'`
- `criteria_id` = CR_HP_B.id
- `trigger_count=1`

应用日志：`告警触发 ... level=4 criteria=<CR_HP_B.id> (candidates=1)`

### Step 4: 记录结果

---

## Task 8: MQTT 模拟测试 — 用例 5（多判据候选合并）

### Step 1: 清理

同 Task 7 Step 1。

### Step 2: 连续发送 value=51 三次（间隔 5 秒）

预期累加：
- CR_HP_A blue（>10）：1 → 2 → 3 ✅ 第 3 次触发，候选 level=1
- CR_HP_B red（>50）：1 → 2 ✅ 第 2 次已触发，候选 level=4；第 3 次重新计数 1

第 2 次发送：候选 = `[red]`，winner=red → 写 level=4 告警
第 3 次发送：候选 = `[blue, ]`（red 重新计数未达阈值）→ 写 CR_HP_A level=1 告警

最终 alarm_record 表：2 条记录（一条 level=4 from CR_HP_B，一条 level=1 from CR_HP_A）

### Step 3: 验证

```sql
SELECT id, alarm_level, alarm_level_text, criteria_id, trigger_count, create_time
FROM alarm_record WHERE device_id = :device_id ORDER BY id;
```
Expected: 2 行
- 第一条（较早 create_time）：level=4, criteria_id=CR_HP_B.id, trigger_count=1
- 第二条：level=1, criteria_id=CR_HP_A.id, trigger_count=1

应用日志：两次 `告警触发`，分别 `level=4 (candidates=1)` 与 `level=1 (candidates=1)`

### Step 4: 记录结果

---

## Task 9: 整理测试结果并提交

### Step 1: 汇总 `docs/plans/test-results.md`

按以下结构：

```markdown
# 告警引擎逻辑调整 — 测试结果

> 日期：2026-06-16
> 环境：local profile

## 用例汇总

| # | 用例 | 期望 | 实际 | 状态 |
|---|---|---|---|---|
| 1 | 优先级排他 | 0 条告警 | ... | ✅/❌ |
| 2 | 计数器独立 | 1 条 level=1 | ... | ✅/❌ |
| 3 | 等级独立重置 | red 计数正确清零 | ... | ✅/❌ |
| 4 | 最高等级胜出 | 1 条 level=4 | ... | ✅/❌ |
| 5 | 多判据候选 | 2 条告警 | ... | ✅/❌ |

## 关键日志摘录

...

## 异常与遗留问题

...
```

### Step 2: Commit

```bash
git add docs/plans/test-results.md
git commit -m "test(alarm): 端到端验证引擎逻辑调整"
```

### Step 3: 调用 superpowers:verification-before-completion

按 skill 要求运行验证命令并确认输出，然后用证据支撑结论。

---

## 全部完成的标准

- [ ] Task 1: AlarmEvaluationEngine 修改完成，编译通过
- [ ] Task 2: 既有单测全部通过
- [ ] Task 3: DB 三条判据已插入，缓存已刷新
- [ ] Task 4-8: 五个 MQTT 用例全部 ✅
- [ ] Task 9: 测试结果文档已 commit
- [ ] Git 状态干净（除非保留测试数据 SQL）

## 退路与回滚

如任意一步失败且无法快速修复：
```bash
git reset --hard 49e8b8c  # 设计文档 commit 之前的 HEAD
```
（请先 `git log --oneline -3` 确认 commit hash）

实施过程中产生的 DB 测试数据可保留供下次回归，无需主动清理。
