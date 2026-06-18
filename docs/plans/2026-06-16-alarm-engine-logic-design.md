# 告警评估引擎逻辑调整设计

> 状态：已通过 brainstorming，进入实施
> 日期：2026-06-16
> 范围：`server/zwei-iot-alarm` 模块
> 入口文件：`AlarmEvaluationEngine.java`

## 1. 背景与问题

当前 `AlarmEvaluationEngine` 存在三个不符合产品语义的问题：

1. **判据优先级不互斥**：隐患点判据未触发时仍会兜底走监测类型判据，导致同一份数据可能被两套判据重复评估
2. **每条判据独立产生告警**：`evaluateCriteria` 中每条命中判据直接 `createOrUpdateAlarm`，多条同时命中会产生多条 `alarm_record`，而业务期望"取最高等级合并为一条"
3. **计数器仅累加不重置**：`AlarmDedupService` 只在判据被满足时累加 Redis 计数，未提供"未满足即重置"的语义，导致 persistCount>1 时实际上只需在 TTL 窗口内累计满足 N 次即可触发，与"连续触发"的语义不一致
4. **综合策略实时调用被废弃**：`evaluateRealtimeStrategies`（Groovy 实时综合策略）在本次需求中不再调用

## 2. 目标

- 隐患点判据存在时，**仅**评估隐患点判据；只有不存在时才回退到监测类型判据
- `evaluateCriteria` 改为：遍历所有判据、所有等级 → 独立计数 → 收集候选 → 取最高等级合并为一条 `alarm_record`
- 实现"连续触发"语义：每个等级独立计数，本等级未被满足则重置该等级计数器（不影响其他等级）
- 移除 `evaluateRealtimeStrategies` 调用与方法本身

## 3. 非目标

- 不修改 `CriteriaEvaluator` / `AlarmDedupService` 既有方法签名（保持现有单测通过）
- 不修改 `AlarmRecord` 表结构与 `createOrUpdateAlarm` 行为
- 不动 `ComprehensiveAlarmJob`（CRON 综合策略调度）— 它独立调用 `dedupService.shouldTriggerAlarm`，不受影响

## 4. 改动清单

### 4.1 `AlarmEvaluationEngine.java`

**字段与构造器删减**：删除 `strategyMapper` / `bindingMapper` / `groovyScriptExecutor` 三个依赖（仅服务于被删除的 `evaluateRealtimeStrategies`）

**`evaluate` 方法调整**：

```java
if (!hpCriteria.isEmpty()) {
    evaluateCriteria(event, hpCriteria, hazardPointIds, monitorContentId);
} else if (monitorContentId != null) {
    Long monitorTypeId = resolveMonitorTypeId(monitorContentId);
    if (monitorTypeId != null) {
        List<AlarmCriteria> mtCriteria = criteriaCache.getByMonitorTypeId(monitorTypeId);
        if (!mtCriteria.isEmpty()) {
            evaluateCriteria(event, mtCriteria, hazardPointIds, monitorContentId);
        }
    }
}
// evaluateRealtimeStrategies 调用删除
```

**`evaluateCriteria` 重构**（逐等级独立评估 + 候选合并）：

```java
private static final Map<String, Integer> LEVEL_VALUES = Map.of(
        "red", 4, "orange", 3, "yellow", 2, "blue", 1);

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

    Candidate winner = candidates.stream()
            .max(Comparator.comparingInt(c -> c.level))
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

private record Candidate(AlarmCriteria criteria, int level, Long effectiveHpId) {}
```

**删除 `evaluateRealtimeStrategies` 整个方法**

### 4.2 `AlarmDedupService.java`

**不修改**。既有 `clearPreTrigger(criteriaId, hazardPointId, level)` 已支持"仅清当前 level"语义，满足本设计需要。

### 4.3 `CriteriaEvaluator.java`

**不修改**。`parseLevelConfig` 与 `evaluateLevel` 均为 package-private 可见，足够本设计调用。

## 5. 数据流

```
MonitorDataIngestedEvent
   │
   ▼
AlarmEvaluationEngine.evaluate
   │
   ├── 查隐患点 → 查 hpCriteria
   │
   ├── [hpCriteria 非空] → evaluateCriteria(hpCriteria)
   │        │
   │        ├── 逐判据：
   │        │     逐等级（blue/yellow/orange/red）：
   │        │        满足 → dedupService.shouldTriggerAlarm(level) → 命中则入候选
   │        │        未满足 → dedupService.clearPreTrigger(level) 仅重置该等级
   │        │
   │        └── 候选非空 → 取最高等级 → createOrUpdateAlarm(1 条) → publishEvent(1 次)
   │
   └── [hpCriteria 空 && monitorContentId 非空] → resolveMonitorTypeId → mtCriteria → 同上
```

## 6. 测试方案

### 6.1 现有单元测试

- `AlarmDedupServiceTest`：保持 100% 通过（不动 `AlarmDedupService`）
- `CriteriaEvaluatorTest`（若存在）：保持通过

### 6.2 新增单元测试（如时间允许）

为 `AlarmEvaluationEngine.evaluateCriteria` 加 mock 单测，验证：
- 候选合并：多条候选 → 单条 `createOrUpdateAlarm` 调用，level 为最高
- 等级独立重置：等级未满足时调用 `clearPreTrigger(cid, hpId, level)`，且仅清该 level

### 6.3 端到端测试（DB + MQTT 模拟）

#### 测试数据准备

- 设备 D1（已有，关联隐患点 HP1）
- 传感器：雨量（attrCode=`rainfall`，关联 monitorContentId 已有）
- 判据三条（按以下顺序 INSERT 到 `alarm_criteria`）：

| name | hazard_point_id | monitor_type_id | monitor_content_id | level_config | persist_count | silence_period | is_enabled |
|---|---|---|---|---|---|---|---|
| CR_HP_A | HP1 | (null 或对应) | 雨量 content_id | `{"blue":{"logicOperator":"AND","conditions":[{"subject":"rainfall","operator":"GT","threshold":10}]}}` | 3 | 0 | 1 |
| CR_HP_B | HP1 | (同) | 雨量 content_id | `{"red":{"logicOperator":"AND","conditions":[{"subject":"rainfall","operator":"GT","threshold":50}]}}` | 2 | 0 | 1 |
| CR_MT_C | null | 雨量 type_id | 雨量 content_id | `{"blue":{"logicOperator":"AND","conditions":[{"subject":"rainfall","operator":"GT","threshold":5}]}}` | 1 | 0 | 1 |

#### 测试用例

| # | 用例名 | 数据序列（attrCode=rainfall 的 value）| 期望结果 |
|---|---|---|---|
| 1 | 优先级排他 | `6`（仅满足 CR_MT_C） | 0 条告警（HP 判据存在，MT 不评估） |
| 2 | 计数器独立累加 | `11, 11, 11` | 1 条 level=1 告警（CR_HP_A 第 3 次触发，CR_HP_B 仅 3 次但未达 level，不计）|
| 3 | 等级独立重置 | `60, 60, 20, 60, 60` | red 计数 1→2→**0**→1→2，**不触发**（因 persistCount=2 已被重置后从 0 开始，到 2 仍未达 2 次连续且第 5 次刚好到 2，需观察） |

> 用例 3 备注：`shouldTriggerAlarm` 在 count>=persistCount 时触发，第二次 `60` 已 count=2 触发并清零，因此用例 3 应改为：`60, 20, 60, 60` → red 计数 1→**0**→1→2，第 4 步触发；blue 始终满足，3 次累加到 3 触发 level=1。最终 winner=red(4)。

| 4 | 最高等级胜出 | `60, 60`（同时满足 blue+red，red persistCount=2 触发，blue persistCount=3 未触发） | 1 条 level=4 告警 |
| 5 | 多判据候选合并 | `51, 51, 51` → CR_HP_B red 触发(level=4)，CR_HP_A blue 触发(level=1) | 1 条 level=4 告警（winner=red） |

#### 验证手段

- `SELECT * FROM alarm_record WHERE device_id=D1 ORDER BY id DESC LIMIT 5;` 看记录数与等级
- Redis CLI：`KEYS alarm:pre-trigger:*` 查看计数 key 状态
- 应用日志：`告警触发 id=... level=... criteria=...` INFO 行

## 7. 风险与回滚

| 风险 | 缓解 |
|---|---|
| 删除 `evaluateRealtimeStrategies` 影响其他模块 | 已 grep 全仓确认无外部调用；`ComprehensiveAlarmJob` 走独立调度，不依赖此方法 |
| 等级独立重置带来更多 Redis 操作 | 每判据每次评估最多 4 次额外 DEL，QPS 影响可忽略 |
| 候选合并后丢失多判据触发明细 | `alarm_record.criteriaId` 记录胜出判据；其他判据计数已在 dedup 中累加，下次评估仍可触发 |

回滚：单文件 `git revert` 即可，无 schema 变更。

## 8. 实施顺序

1. 修改 `AlarmEvaluationEngine.java`（字段/构造器删减、`evaluate` 调整、`evaluateCriteria` 重构、`evaluateRealtimeStrategies` 删除）
2. 编译验证 `mvn -pl zwei-iot-alarm compile`
3. 准备 DB 测试数据（INSERT 三条判据）
4. 启动后端 + 通过 MQTT 发送模拟数据，按用例 1-5 验证
5. 整理验证结果，commit
