[根目录](../../CLAUDE.md) > [server](../) > **zwei-iot-alarm**

# zwei-iot-alarm — 告警中心 (判据/综合策略/引擎/分发)

> 面包屑: [根目录](../../CLAUDE.md) > [server](../) > **zwei-iot-alarm**

## 模块职责

告警引擎, 负责"如何判断是否告警"与"告警后做什么":

- **告警判据** (`alarm_criteria`) — 单指标阈值 (如 "雨量 > 50 mm/h")，四级 (蓝/黄/橙/红) JSON 条件
- **综合告警策略** (`alarm_strategy`) — 多指标组合 (Groovy 脚本 + 条件组)，REALTIME/CRON 两种触发模式
- **告警评估引擎** (`AlarmEvaluationEngine`) — 消费 `MonitorDataIngestedEvent`，命中判据/策略后生成告警
- **告警记录** (`alarm_record`) — 告警生命周期 (待处理/处理中/已销警/误报)
- **告警通知分发** — 多通道 (SYSTEM/SMS/EMAIL)，监听 `AlarmTriggeredEvent` 自动建通知
- **告警 SSE 推送** — `AlarmStreamPublisher` 实时推送到前端

## 关键依赖

- `zwei-common` (事件 + 基础)
- `zwei-iot-device` (设备/传感器查询 Service 接口)
- `zwei-iot-monitor` (字典: monitor_content)
- `zwei-iot-timeseries` (监测数据)
- `zwei-iot-hazard` (隐患点)
- MyBatis + spring-boot-starter-web (SSE)
- **groovy** (综合策略脚本引擎)
- lombok + junit + spring-boot-starter-test

## 主要子包

| 子包               | 职责                                                                                                                                                     |
|------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------|
| `controller`     | `AlarmCriteriaController` / `AlarmStrategyController` / `AlarmRecordController` / `AlarmDispatchController` / `AlarmStreamController` (SSE)            |
| `service`        | `IAlarmCriteriaService` / `IAlarmStrategyService` / `IAlarmRecordService` / `IAlarmDispatchService` / `IAlarmNotificationService`                      |
| `service.impl`   | 上述接口实现 (5 个)                                                                                                                                           |
| `service.engine` | `AlarmEvaluationEngine` / `CriteriaEvaluator` / `CriteriaCacheService` / `AlarmDedupService` / `GroovyScriptExecutor`                                  |
| `service.notify` | `AlarmNotifier` (分发) / `AlarmStreamPublisher` (SSE)                                                                                                    |
| `domain`         | `AlarmCriteria` / `AlarmStrategy` / `AlarmRecord` / `AlarmDispatchRule` / `AlarmNotification` / `LevelConfig` / `LevelCondition` / `CriteriaCondition` |
| `domain.dto`     | `CriteriaCreateRequest` / `StrategyCreateRequest` / `DispatchRuleCreateRequest` / `AlarmRecordDisposeRequest` / `BatchDisposeRequest`                  |
| `job`            | `ComprehensiveAlarmJob` (周期综合策略调度)                                                                                                                     |
| `config`         | `AlarmProperties` (Groovy 超时/预触发 TTL)                                                                                                                  |
| `mapper`         | MyBatis 5 个 Mapper + XML                                                                                                                               |

## 对外接口 (Controller)

| 路径                                 | 职责                     |
|------------------------------------|------------------------|
| `/api/v1/iot/alarm/criteria/*`     | 判据 CRUD + 启停 + 变更日志    |
| `/api/v1/iot/alarm/strategy/*`     | 综合策略 CRUD + 启停 + 隐患点绑定 |
| `/api/v1/iot/alarm/record/*`       | 告警记录分页/详情/处置/批量销警      |
| `/api/v1/iot/alarm/dispatch/*`     | 告警分发规则 CRUD            |
| `/api/v1/iot/alarm/notification/*` | 通知记录查询/重发              |
| `/api/v1/iot/alarm/stream`         | SSE 实时推送               |

## 关键流程

### 告警触发 (V3.0 引擎)

1. `zwei-iot-timeseries.MonitorIngestConsumerService` 写入 IoTDB 后发布 `MonitorDataIngestedEvent`
2. `AlarmEvaluationEngine.@EventListener` 接收 (`AlarmEvaluationEngine.java:79`)
3. 解析设备→隐患点关系 (`IDeviceHazardRelationService`)
4. 匹配**隐患点专属判据** (优先级 1) — `criteriaCache.getByHazardPointId()`
5. 未触发 → 匹配**监测类型兜底判据** (`hazard_point_id IS NULL`) — 优先级 2
6. 匹配**综合策略** (REALTIME 模式) — `strategyMapper.selectEnabledByTriggerMode("REALTIME")`
7. 触发后写 `alarm_record` → 发布 `AlarmTriggeredEvent` → SSE 推送 + 通知分发

### 判据评估 (CriteriaEvaluator)

- 输入: `AlarmCriteria` + `subject→value` 映射
- 评估顺序: **red → orange → yellow → blue** (高到低)，返回最高命中等级
- 单等级评估: AND/OR 逻辑组合
- 支持算子: `GT/GTE/LT/LTE/EQ/NEQ/BETWEEN`

### 告警去重 (AlarmDedupService)

- **persistCount=1** → 立即触发 (无预触发)
- **persistCount>1** → Redis 累加计数 (`alarm:pre-trigger:{cid}:{hpId}:{level}`)，达阈值才触发
- **silence_period** → `alarm:last-trigger:{cid}:{hpId}` 静默期内不再触发，仅累加 `trigger_count`

### 告警生命周期

| 状态码 | 状态名 | 含义         |
|-----|-----|------------|
| 1   | 待处理 | 引擎自动创建初始状态 |
| 2   | 处理中 | 操作员开始处置    |
| 3   | 已销警 | 现场确认解除     |
| 4   | 误报  | 标记为非真实告警   |

每次状态变更写入 `alarm_record_log` (含 fromStatus/toStatus/disposalType/operator/note)

## 核心实现类索引

### Engine 层 (P0)

| 类                       | 文件                                          | 职责                                      |
|-------------------------|---------------------------------------------|-----------------------------------------|
| `AlarmEvaluationEngine` | `service/engine/AlarmEvaluationEngine.java` | 入口监听器，串联判据匹配+综合策略+事件发布                  |
| `CriteriaEvaluator`     | `service/engine/CriteriaEvaluator.java`     | 单判据评估 (red→blue)，JSON 解析 `level_config` |
| `CriteriaCacheService`  | `service/engine/CriteriaCacheService.java`  | 判据缓存 (Redis + 本地 ConcurrentHashMap)     |
| `AlarmDedupService`     | `service/engine/AlarmDedupService.java`     | 预触发计数 + 静默期管理 (Redis)                   |
| `GroovyScriptExecutor`  | `service/engine/GroovyScriptExecutor.java`  | 沙箱化 Groovy 脚本执行 (单线程 + 超时)              |

### Service 实现 (P0)

| 类                              | 文件                                               | 关键方法                                                        |
|--------------------------------|--------------------------------------------------|-------------------------------------------------------------|
| `AlarmCriteriaServiceImpl`     | `service/impl/AlarmCriteriaServiceImpl.java`     | CRUD + 缓存失效 + 变更日志 (`alarm_criteria_log`)                   |
| `AlarmStrategyServiceImpl`     | `service/impl/AlarmStrategyServiceImpl.java`     | 策略 CRUD + `@Transactional` 绑定 `alarm_strategy_hazard_point` |
| `AlarmRecordServiceImpl`       | `service/impl/AlarmRecordServiceImpl.java`       | `createOrUpdateAlarm` 主动去重 + 状态机                            |
| `AlarmDispatchServiceImpl`     | `service/impl/AlarmDispatchServiceImpl.java`     | 分发规则 CRUD + 唯一性校验                                           |
| `AlarmNotificationServiceImpl` | `service/impl/AlarmNotificationServiceImpl.java` | 通知批量创建/状态更新                                                 |

### 通知层

| 类                      | 文件                                         | 职责                                           |
|------------------------|--------------------------------------------|----------------------------------------------|
| `AlarmNotifier`        | `service/notify/AlarmNotifier.java`        | 监听 `AlarmTriggeredEvent` → 匹配规则 → 批量创建通知     |
| `AlarmStreamPublisher` | `service/notify/AlarmStreamPublisher.java` | SSE 推送到前端 (CopyOnWriteArrayList<SseEmitter>) |

## Redis Key 模式

| Key 模式                                                   | 用途        | TTL                    |
|----------------------------------------------------------|-----------|------------------------|
| `alarm:pre-trigger:{criteriaId}:{hazardPointId}:{level}` | 预触发计数     | `preTriggerTtlSeconds` |
| `alarm:last-trigger:{criteriaId}:{hazardPointId}`        | 最近触发时间    | `preTriggerTtlSeconds` |
| `alarm:criteria:enabled`                                 | 启用的判据列表缓存 | 5 分钟                   |

## Groovy 沙箱 (GroovyScriptExecutor)

**禁止关键词** (出现即拒绝执行):
`System.exit` / `Runtime.getRuntime` / `ProcessBuilder` / `exec(` / `Class.forName` / `getClassLoader` / `File(` /
`FileInputStream` / `FileOutputStream` / `Thread.sleep` / `Thread.start` / `System.getProperty` / `System.setProperty`

**执行控制**:

- 单线程 executor (`groovy-eval` daemon thread)
- 超时由 `alarm.groovy-timeout-seconds` 配置控制
- 返回值: `Number` (1-4) = 告警等级，其他/null/异常 = 无告警

## 数据模型

- `alarm_criteria` — 判据 (id / name / monitorTypeId / monitorContentId / levelConfig JSON / hazardPointId /
  persistCount / silencePeriod / isEnabled / version)
- `alarm_criteria_log` — 判据变更日志 (criteriaId / version / changeType / oldValue JSON / newValue JSON)
- `alarm_strategy` — 综合策略 (id / name / monitorTypeId / triggerMode: REALTIME|CRON / cronExpression / scriptType:
  GROOVY / scriptContent / defaultAlarmLevel / silenceMinutes)
- `alarm_strategy_hazard_point` — 策略-隐患点绑定 (UNIQUE `uk_strategy_hp`)
- `alarm_record` — 告警记录 (id / hazardPointId / deviceId / sensorId / monitorContentId / alarmLevel 1-4 / alarmType:
  THRESHOLD|COMPREHENSIVE / criteriaId / strategyId / currentValue / thresholdValue / triggerConditions JSON /
  triggerCount / status / statusName)
- `alarm_record_log` — 告警状态变更日志
- `alarm_dispatch_rule` — 分发规则 (hazardPointId / alarmLevels / alarmTypes / recipientsJson / channels / timeWindow)
- `alarm_notification` — 通知记录 (alarmId / dispatchRuleId / recipientId / channel / status 1=待发送 2=已发送 3=失败)

## 综合策略脚本 (Groovy 实际示例)

数据库初始化包含 3 条策略 (来自 `alarm_strategy` 表):

```groovy
// 策略1: 清溪乡暴雨泥石流综合预警 (REALTIME)
def hourRain = getLatestValue("rainfall_hour", hazardPointId)
def dayRain = getLatestValue("rainfall_day", hazardPointId)
def soilMoisture = getLatestValue("soil_moisture", hazardPointId)

if (hourRain > 50 && dayRain > 120 && soilMoisture > 80) {
    return AlarmResult.red("小时雨量${hourRain}mm + 日雨量${dayRain}mm + 土壤含水率${soilMoisture}%，泥石流风险极高")
} else if (hourRain > 30 && dayRain > 80 && soilMoisture > 70) {
    return AlarmResult.orange("...")
}
// ...
```

注: 实际脚本会引用 `groovyScriptExecutor.execute()` 中注入的 vars:
`deviceId/sensorId/sensorNo/attrCode/value/hazardPointIds/dataTime` (`AlarmEvaluationEngine.java:209-217`)

## 测试与质量

- 单元测试: 阈值判据 / Groovy 脚本 (`GroovyShell` 解析)
- 集成测试: 启动后注入模拟数据 → 验证告警记录入库 + SSE 推送
- 覆盖率目标 80%+ (告警引擎是核心)

## 常见问题 (FAQ)

**Q: 告警去重策略?**
A: `AlarmDedupService.shouldTriggerAlarm()` — persistCount 控制预触发计数，silencePeriod 控制静默期 (默认 60s ×
静默周期数)。

**Q: Groovy 脚本如何隔离?**
A: `GroovyScriptExecutor` — 沙箱关键字过滤 + 单线程执行 + Future.get(timeout) 强制中断 + Future.cancel(true) 兜底。

**Q: 告警 SSE 推送给谁?**
A: 当前**全量推送** (未按 data_scope 过滤)，由前端按用户权限二次过滤展示。

**Q: 告警等级数值映射?**
A: `AlarmConstants.resolveLevelText()` — 1=蓝/blue, 2=黄/yellow, 3=橙/orange, 4=红/red。

## 相关文件清单

- `pom.xml`
- `src/main/java/com/zwei/iot/alarm/service/engine/AlarmEvaluationEngine.java` (P0)
- `src/main/java/com/zwei/iot/alarm/service/engine/CriteriaEvaluator.java` (P0)
- `src/main/java/com/zwei/iot/alarm/service/engine/AlarmDedupService.java` (P0)
- `src/main/java/com/zwei/iot/alarm/service/engine/CriteriaCacheService.java` (P0)
- `src/main/java/com/zwei/iot/alarm/service/engine/GroovyScriptExecutor.java` (P0)
- `src/main/java/com/zwei/iot/alarm/service/impl/AlarmCriteriaServiceImpl.java` (P0)
- `src/main/java/com/zwei/iot/alarm/service/impl/AlarmStrategyServiceImpl.java` (P0)
- `src/main/java/com/zwei/iot/alarm/service/impl/AlarmRecordServiceImpl.java` (P0)
- `src/main/java/com/zwei/iot/alarm/service/impl/AlarmDispatchServiceImpl.java` (P0)
- `src/main/java/com/zwei/iot/alarm/service/impl/AlarmNotificationServiceImpl.java` (P0)
- `src/main/java/com/zwei/iot/alarm/service/notify/AlarmNotifier.java` (P0)
- `src/main/java/com/zwei/iot/alarm/service/notify/AlarmStreamPublisher.java` (P0)
- `src/main/java/com/zwei/iot/alarm/job/ComprehensiveAlarmJob.java` (P2, CRON 调度)

## 变更记录 (Changelog)

| 时间               | 变更                                                                                                                                                           |
|------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 2026-06-10 18:52 | 首次生成模块级 CLAUDE.md (架构师自动扫描)                                                                                                                                  |
| 2026-06-10 19:08 | 增量补扫: 修正路径 `engine/` → `service/engine/`、`strategy/` → `service/engine/`、`notify/` → `service/notify/`; 新增核心实现类索引、Redis Key 模式、Groovy 沙箱、Engine/Service 完整清单 |
