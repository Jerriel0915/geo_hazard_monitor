# 综合告警功能迭代完善 — 设计规格

> **日期**: 2026-06-28
> **状态**: 设计已批准，待实现
> **范围**: 综合告警系统全面迭代 — 事件监听、Scope 选择、Quartz 调度、脚本日志、共用告警逻辑
> **前置**: `2026-06-27-composite-alarm-script-editor-design.md`（脚本编辑器 + cache/sensor 注入）

## 1. 背景与目标

综合告警系统当前存在以下不足：

1. **触发模式不完整**：REALTIME 模式没有实际实现事件监听，仅 CRON 模式可用
2. **范围选择不可用**：前端无法正常加载隐患点列表，不支持分组选择
3. **脚本无事件上下文**：脚本只能访问 `hazardPointIds` 和 `currentTime`，无法感知触发事件
4. **定时调度硬编码**：所有 CRON 策略统一每 60 秒执行，无法按 cron 表达式独立调度
5. **无执行日志**：脚本执行过程无记录，无法排查问题
6. **告警逻辑分散**：ComprehensiveAlarmJob 内嵌了告警创建逻辑，无法被 REALTIME 路径复用

**目标**：构建统一的 `ComprehensiveAlarmExecutionService`，三条触发路径（CRON/事件/告警）统一入口，共用告警去重→记录→分发逻辑。

## 2. 设计决策

| 决策项 | 选择 | 理由 |
|--------|------|------|
| 执行架构 | 集中式 ExecutionService | 单一入口，三种触发源统一调用，逻辑内聚 |
| Scope 存储 | `hazard_point_id` 列改 VARCHAR，前缀区分 | `*`=全部 / `group:{id}`=分组 / `{数字}`=指定 |
| Quartz 调度 | 每个策略独立 JobDetail + Trigger | 支持不同 cron 频率，启用/停用动态管理 |
| 事件传入脚本 | 直接传 Java 对象 | 脚本可访问有类型方法/字段 |
| 执行日志 | 独立 `alarm_strategy_execution_log` 表 | 每次执行一条记录，含脚本内 log 工具收集的日志 |
| 循环防护 | `alarmType=COMPREHENSIVE` 过滤 | 综合策略产生的 AlarmTriggeredEvent 不再触发其他综合策略 |

## 3. 架构总图

```
┌──────────────────────────────────────────────────────────────────┐
│                    触发源 (3 条路径)                              │
├────────────────┬──────────────────────┬──────────────────────────┤
│  Quartz Job    │ DataIngest Listener  │ AlarmTrigger Listener    │
│  (CRON 策略)   │ (REALTIME 策略)      │ (REALTIME 策略)          │
│  按 cronExpression│ 监听               │ 监听                      │
│  独立调度       │ MonitorDataIngested │ AlarmTriggeredEvent      │
│                │ Event                │ (过滤 COMPREHENSIVE 类型) │
└───────┬────────┴──────────┬───────────┴────────────┬─────────────┘
        │                   │                          │
        └───────────────────┼──────────────────────────┘
                            ▼
        ┌───────────────────────────────────────────┐
        │    ComprehensiveAlarmExecutionService      │
        │    (统一执行入口)                           │
        ├───────────────────────────────────────────┤
        │ 1. Scope 解析 (StrategyScopeResolver)      │
        │    展开 *→全部 / group:xxx→组成员 / ID    │
        │ 2. Groovy 脚本执行                         │
        │    注入: event + tools(cache/sensor)      │
        │         + log(ScriptLogger)                │
        │ 3. AlarmDedupService 去重                  │
        │    (按 strategyId + hazardPointId)         │
        │ 4. AlarmRecordService.createOrUpdateAlarm  │
        │    (每个匹配隐患点各一条)                   │
        │ 5. 发布 AlarmTriggeredEvent                │
        │    → AlarmNotifier (通知分发)              │
        │    → AlarmStreamPublisher (SSE推送)        │
        │ 6. 写 execution_log                        │
        └───────────────────────────────────────────┘
```

## 4. 数据库变更

### 4.1 alarm_strategy_hazard_point — 列类型扩展

`hazard_point_id` 从 BIGINT 改为 VARCHAR(100)，支持前缀语义：

```sql
ALTER TABLE alarm_strategy_hazard_point
  MODIFY COLUMN hazard_point_id VARCHAR(100) NOT NULL
  COMMENT '范围值: *=全部隐患点 / group:{id}=按分组 / {数字}=指定隐患点ID';
```

**语义**：
- `*` — 该策略应用于全部隐患点
- `group:3` — 该策略应用于分组 ID=3 下所有隐患点
- `123` — 该策略应用于指定隐患点 ID=123（与现有数据兼容，值不变）

### 4.2 alarm_strategy_execution_log — 新建表

```sql
CREATE TABLE alarm_strategy_execution_log (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    strategy_id      BIGINT       NOT NULL COMMENT '策略ID',
    trigger_type     VARCHAR(20)  NOT NULL COMMENT 'CRON/DATA_INGEST/ALARM_TRIGGER',
    trigger_source   TEXT         NULL     COMMENT '触发事件摘要 JSON',
    hazard_point_ids VARCHAR(500) NULL     COMMENT '解析后的隐患点ID列表 (逗号分隔)',
    result_level     INT          NULL     COMMENT '脚本返回等级 1-4',
    result_status    VARCHAR(20)  NOT NULL COMMENT 'SUCCESS/NO_ALARM/FAIL/TIMEOUT',
    duration_ms      BIGINT       NOT NULL DEFAULT 0,
    script_logs      TEXT         NULL     COMMENT '脚本内 log 工具收集的日志 (JSON数组)',
    error_message    TEXT         NULL     COMMENT '异常信息',
    triggered_count  INT          NOT NULL DEFAULT 0 COMMENT '触发告警记录数',
    create_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_strategy_create (strategy_id, create_time DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='综合告警策略执行日志';
```

## 5. 后端设计

### 5.1 新建类清单

| 类名 | 包路径 | 职责 |
|------|--------|------|
| `ComprehensiveAlarmExecutionService` | `service.engine` | 统一执行入口 |
| `StrategyScopeResolver` | `service.engine` | 范围解析 (展开 *、group:xxx) |
| `StrategyScopeResolverTest` | `service.engine` (test) | 范围解析单测 |
| `ScriptLogger` | `service.engine` | 脚本日志工具 (注入 Groovy) |
| `ComprehensiveAlarmEventListener` | `service.engine` | REALTIME 事件监听器 |
| `StrategyQuartzScheduler` | `service.engine` | Quartz 动态注册/注销 |
| `ComprehensiveAlarmQuartzJob` | `job` | Quartz Job 类 |
| `ExecutionResult` | `domain.dto` | 执行结果 DTO |
| `StrategyExecutionLog` | `domain` | 执行日志 domain |
| `StrategyExecutionLogMapper` | `mapper` | 执行日志 Mapper |
| `AlarmStrategyExecutionLogController` 或合入现有 Controller | — | 执行日志查询 API |

### 5.2 ComprehensiveAlarmExecutionService

```java
@Service
@Slf4j
public class ComprehensiveAlarmExecutionService {

    private final AlarmStrategyMapper strategyMapper;
    private final AlarmStrategyHazardPointMapper bindingMapper;
    private final GroovyScriptExecutor groovyScriptExecutor;
    private final IAlarmRecordService alarmRecordService;
    private final AlarmDedupService dedupService;
    private final ApplicationEventPublisher eventPublisher;
    private final ScriptCacheOps cacheOps;
    private final ScriptSensorQuery scriptSensorQuery;
    private final StrategyExecutionLogMapper executionLogMapper;
    private final StrategyScopeResolver scopeResolver;

    /**
     * 执行综合告警策略（三种触发源的统一入口）。
     *
     * @param strategy     策略
     * @param triggerEvent 触发事件 (MonitorDataIngestedEvent / AlarmTriggeredEvent / null=CRON)
     * @param triggerType  CRON / DATA_INGEST / ALARM_TRIGGER
     * @return 执行结果
     */
    public ExecutionResult execute(AlarmStrategy strategy, Object triggerEvent, String triggerType) {
        long start = System.currentTimeMillis();

        // 1. Scope 解析
        List<Long> hazardPointIds = scopeResolver.resolveScope(strategy.getId());
        if (hazardPointIds.isEmpty()) {
            return saveAndReturn(strategy, triggerType, triggerEvent, hazardPointIds,
                null, "NO_ALARM", start, null, null, 0);
        }

        // 2. 构建 variables + tools + log
        ScriptLogger scriptLogger = new ScriptLogger(strategy.getId());
        Map<String, Object> variables = new HashMap<>();
        variables.put("hazardPointIds", hazardPointIds);
        variables.put("currentTime", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        if (triggerEvent != null) {
            variables.put("event", triggerEvent);
        }

        Map<String, Object> tools = new HashMap<>();
        tools.put("cache", cacheOps);
        tools.put("sensor", scriptSensorQuery);
        tools.put("log", scriptLogger);

        // 3. 执行脚本
        Integer alarmLevel;
        String errorMessage = null;
        try {
            alarmLevel = groovyScriptExecutor.executeWithTools(
                strategy.getScriptContent(), variables, tools);
        } catch (Exception e) {
            errorMessage = e.getMessage();
            alarmLevel = null;
        }
        long durationMs = System.currentTimeMillis() - start;

        // 4. 告警触发
        int triggeredCount = 0;
        if (alarmLevel != null && alarmLevel > 0) {
            triggeredCount = triggerAlarms(strategy, hazardPointIds, alarmLevel);
        }

        // 5. 确定状态
        String resultStatus = errorMessage != null ? "FAIL"
            : (alarmLevel != null && alarmLevel > 0 ? "SUCCESS" : "NO_ALARM");

        // 6. 写执行日志
        return saveAndReturn(strategy, triggerType, triggerEvent, hazardPointIds,
            alarmLevel, resultStatus, start, scriptLogger, errorMessage, triggeredCount);
    }

    /**
     * 共用的告警触发逻辑（从 ComprehensiveAlarmJob 提取）。
     * 对每个隐患点：去重 → 创建/更新 AlarmRecord → 发布 AlarmTriggeredEvent
     */
    private int triggerAlarms(AlarmStrategy strategy, List<Long> hazardPointIds,
                              int alarmLevel) {
        int silenceMinutes = strategy.getSilenceMinutes() != null
                             ? strategy.getSilenceMinutes() : 0;
        int triggeredCount = 0;

        for (Long hpId : hazardPointIds) {
            boolean shouldTrigger = dedupService.shouldTriggerAlarm(
                strategy.getId(), hpId, alarmLevel, 1, silenceMinutes);
            if (!shouldTrigger) continue;

            AlarmRecord record = AlarmRecord.builder()
                .hazardPointId(hpId)
                .alarmLevel(alarmLevel)
                .alarmLevelText(AlarmConstants.resolveLevelText(alarmLevel))
                .alarmType("COMPREHENSIVE")
                .alarmMessage("综合策略告警: " + strategy.getName())
                .strategyId(strategy.getId())
                .currentValue(BigDecimal.ZERO)
                .createBy(AlarmConstants.SYSTEM_OPERATOR)
                .createTime(new Date())
                .build();

            AlarmRecord saved = alarmRecordService.createOrUpdateAlarm(record);
            triggeredCount++;

            eventPublisher.publishEvent(new AlarmTriggeredEvent(
                saved.getId(), saved.getHazardPointId(),
                saved.getAlarmLevel(), saved.getAlarmType(),
                saved.getAlarmMessage(), saved.getTriggerReason()));
        }
        return triggeredCount;
    }

    private ExecutionResult saveAndReturn(/* 参数省略 */) {
        // 写 execution_log 表
        // 更新 strategy.last_run_time / last_run_result
        // 返回 ExecutionResult
    }
}
```

### 5.3 StrategyScopeResolver

```java
@Service
public class StrategyScopeResolver {

    private final AlarmStrategyHazardPointMapper bindingMapper;
    private final HazardPointMapper hazardPointMapper;  // 跨模块通过接口调用

    /**
     * 将策略的 scope 配置展开为实际隐患点 ID 列表。
     */
    public List<Long> resolveScope(Long strategyId) {
        List<String> scopeValues = bindingMapper.selectScopeValuesByStrategyId(strategyId);
        Set<Long> result = new LinkedHashSet<>();

        for (String scope : scopeValues) {
            if ("*".equals(scope)) {
                result.addAll(hazardPointMapper.selectAllHazardPointIds());
            } else if (scope.startsWith("group:")) {
                Long groupId = Long.parseLong(scope.substring(6));
                result.addAll(hazardPointMapper.selectIdsByGroupId(groupId));
            } else {
                result.add(Long.parseLong(scope));
            }
        }
        return new ArrayList<>(result);
    }

    /**
     * 反向匹配：给定隐患点 ID，判断策略 scope 是否包含它。
     * 用于事件监听器快速过滤。
     */
    public boolean isHazardPointInScope(Long strategyId, Long hazardPointId) {
        List<String> scopeValues = bindingMapper.selectScopeValuesByStrategyId(strategyId);
        for (String scope : scopeValues) {
            if ("*".equals(scope)) return true;
            if (scope.startsWith("group:")) {
                Long groupId = Long.parseLong(scope.substring(6));
                if (hazardPointMapper.selectIdsByGroupId(groupId).contains(hazardPointId))
                    return true;
            } else if (scope.equals(String.valueOf(hazardPointId))) {
                return true;
            }
        }
        return false;
    }
}
```

**跨模块依赖**：`StrategyScopeResolver` 通过已有的 Service 接口查询隐患点数据：
- `IHazardPointQueryService.listMonitoring()` — 获取全部监测中隐患点（用于 `*` 展开）
- `IDeviceHazardRelationService.getHazardPointIdsByDeviceIds()` — 设备反查隐患点（已有）
- 分组成员查询：需在 `IHazardPointQueryService` 新增 `listByGroupId(Long groupId)` 方法，返回该分组下隐患点 ID 列表

### 5.4 ScriptLogger

```java
public class ScriptLogger {
    private final Long strategyId;
    private final List<LogEntry> entries = new ArrayList<>();
    private final long startTime = System.currentTimeMillis();

    public void info(String msg)  { add("INFO", msg); }
    public void warn(String msg)  { add("WARN", msg); }
    public void error(String msg) { add("ERROR", msg); }

    private void add(String level, String msg) {
        entries.add(new LogEntry(level, msg, System.currentTimeMillis() - startTime));
    }

    /** 序列化为 JSON 数组字符串，存入 execution_log.script_logs */
    public String toJson() {
        if (entries.isEmpty()) return null;
        // [{"level":"INFO","msg":"开始检查","ts":12}, ...]
        return JSON.toJSONString(entries);
    }

    @Data @AllArgsConstructor
    public static class LogEntry {
        private String level;
        private String msg;
        private long ts;  // 相对脚本开始的毫秒数
    }
}
```

脚本内使用方式：
```groovy
log.info("开始检查 ${hazardPointIds.size()} 个隐患点")
def rainfall = sensor.query('DEV001', 'RAIN-001', System.currentTimeMillis(), 'rainfall')
if (rainfall?.values?.rainfall > 50) {
    log.warn("雨量值异常: ${rainfall.values.rainfall}mm")
    return 2  // 橙色告警
}
log.info("数据正常")
return 0
```

### 5.5 ComprehensiveAlarmEventListener

```java
@Service
@Slf4j
public class ComprehensiveAlarmEventListener {

    private final AlarmStrategyMapper strategyMapper;
    private final StrategyScopeResolver scopeResolver;
    private final ComprehensiveAlarmExecutionService executionService;
    private final IDeviceHazardRelationService hazardRelationService;

    /**
     * 监听数据入库事件 — REALTIME 策略。
     * 过滤：event 的 deviceId → 反查隐患点 → 匹配策略 scope
     */
    @Async("alarmEvalExecutor")
    @EventListener
    public void onDataIngested(MonitorDataIngestedEvent event) {
        List<AlarmStrategy> strategies = strategyMapper.selectEnabledByTriggerMode("REALTIME");
        if (strategies.isEmpty()) return;

        List<Long> eventHazardPointIds = hazardRelationService
            .getHazardPointIdsByDeviceIds(Collections.singletonList(event.getDeviceId()));
        if (eventHazardPointIds.isEmpty()) return;

        for (AlarmStrategy strategy : strategies) {
            boolean matched = eventHazardPointIds.stream()
                .anyMatch(hpId -> scopeResolver.isHazardPointInScope(strategy.getId(), hpId));
            if (!matched) continue;

            try {
                executionService.execute(strategy, event, "DATA_INGEST");
            } catch (Exception e) {
                log.error("REALTIME策略执行失败 strategyId={}", strategy.getId(), e);
            }
        }
    }

    /**
     * 监听告警触发事件 — REALTIME 策略。
     * 过滤：跳过 COMPREHENSIVE 类型（防循环）；event.hazardPointId 匹配策略 scope
     */
    @Async("alarmEvalExecutor")
    @EventListener
    public void onAlarmTriggered(AlarmTriggeredEvent event) {
        // 循环防护：综合策略产生的告警不再触发其他综合策略
        if ("COMPREHENSIVE".equals(event.getAlarmType())) return;

        List<AlarmStrategy> strategies = strategyMapper.selectEnabledByTriggerMode("REALTIME");
        if (strategies.isEmpty()) return;

        for (AlarmStrategy strategy : strategies) {
            if (!scopeResolver.isHazardPointInScope(strategy.getId(), event.getHazardPointId()))
                continue;

            try {
                executionService.execute(strategy, event, "ALARM_TRIGGER");
            } catch (Exception e) {
                log.error("REALTIME策略执行失败 strategyId={}", strategy.getId(), e);
            }
        }
    }
}
```

**循环防护说明**：

- 综合策略执行后产生的 `AlarmTriggeredEvent` 携带 `alarmType="COMPREHENSIVE"`
- `onAlarmTriggered` 开头过滤掉 `COMPREHENSIVE` 类型，避免无限递归
- 阈值判据产生的 `AlarmTriggeredEvent`（`alarmType="THRESHOLD"`）仍会触发综合策略

### 5.6 StrategyQuartzScheduler

```java
@Service
@Slf4j
public class StrategyQuartzScheduler {

    private final Scheduler scheduler;
    private static final String JOB_GROUP = "ALARM_STRATEGY";
    private static final String DATA_STRATEGY_ID = "strategyId";

    public void scheduleOrUpdate(AlarmStrategy strategy) {
        if (!"CRON".equals(strategy.getTriggerMode())) return;
        if (strategy.getCronExpression() == null || strategy.getCronExpression().isBlank()) return;

        JobKey jobKey = new JobKey("strategy_" + strategy.getId(), JOB_GROUP);
        try {
            scheduler.deleteJob(jobKey);  // 先删旧

            JobDetail jobDetail = JobBuilder.newJob(ComprehensiveAlarmQuartzJob.class)
                .withIdentity(jobKey)
                .usingJobData(DATA_STRATEGY_ID, strategy.getId())
                .storeDurably()
                .build();

            CronTrigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("trigger_" + strategy.getId(), JOB_GROUP)
                .withSchedule(CronScheduleBuilder.cronSchedule(strategy.getCronExpression())
                    .withMisfireHandlingInstructionDoNothing())
                .build();

            scheduler.scheduleJob(jobDetail, trigger);

            if (strategy.getIsEnabled() != null && strategy.getIsEnabled() == 0) {
                scheduler.pauseJob(jobKey);
            }
        } catch (SchedulerException e) {
            log.error("注册策略定时任务失败 strategyId={}", strategy.getId(), e);
        }
    }

    public void pause(Long strategyId) {
        try { scheduler.pauseJob(new JobKey("strategy_" + strategyId, JOB_GROUP)); }
        catch (SchedulerException e) { log.error("暂停任务失败", e); }
    }

    public void resume(Long strategyId) {
        try { scheduler.resumeJob(new JobKey("strategy_" + strategyId, JOB_GROUP)); }
        catch (SchedulerException e) { log.error("恢复任务失败", e); }
    }

    public void unschedule(Long strategyId) {
        try { scheduler.deleteJob(new JobKey("strategy_" + strategyId, JOB_GROUP)); }
        catch (SchedulerException e) { log.error("删除任务失败", e); }
    }
}
```

### 5.7 ComprehensiveAlarmQuartzJob

```java
import com.zwei.common.utils.spring.SpringUtils;

@DisallowConcurrentExecution
public class ComprehensiveAlarmQuartzJob implements Job {

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        Long strategyId = context.getJobDetail().getJobDataMap().getLong("strategyId");

        AlarmStrategyMapper mapper = SpringUtils.getBean(AlarmStrategyMapper.class);
        ComprehensiveAlarmExecutionService execService =
            SpringUtils.getBean(ComprehensiveAlarmExecutionService.class);

        AlarmStrategy strategy = mapper.selectStrategyById(strategyId);
        if (strategy == null || strategy.getIsEnabled() == null || strategy.getIsEnabled() == 0) return;

        try {
            execService.execute(strategy, null, "CRON");
        } catch (Exception e) {
            throw new JobExecutionException("策略执行失败: " + strategyId, e);
        }
    }
}
```

**SpringUtils**：使用项目已有的 `com.zwei.common.utils.spring.SpringUtils.getBean()` 在非 Spring 管理的 Quartz Job 中获取 Bean。

### 5.8 策略 CRUD 集成 Quartz

在 `AlarmStrategyServiceImpl` 中注入 `StrategyQuartzScheduler`：

```java
// insert() 末尾
if ("CRON".equals(strategy.getTriggerMode()) && strategy.getIsEnabled() == 1) {
    quartzScheduler.scheduleOrUpdate(strategy);
}

// update()
if ("CRON".equals(strategy.getTriggerMode())) {
    quartzScheduler.scheduleOrUpdate(strategy);  // 先删后建
} else {
    quartzScheduler.unschedule(strategy.getId());  // 改为 REALTIME
}

// delete()
quartzScheduler.unschedule(id);

// toggle()
if (isEnabled == 1) quartzScheduler.resume(id);
else quartzScheduler.pause(id);
```

### 5.9 应用启动时批量注册

在 `AlarmStrategyServiceImpl` 或新类中添加：

```java
@PostConstruct
public void initCronStrategies() {
    List<AlarmStrategy> cronStrategies = strategyMapper.selectEnabledByTriggerMode("CRON");
    for (AlarmStrategy s : cronStrategies) {
        if (s.getIsEnabled() != null && s.getIsEnabled() == 1) {
            quartzScheduler.scheduleOrUpdate(s);
        }
    }
    log.info("已注册 {} 个 CRON 策略到 Quartz", cronStrategies.size());
}
```

### 5.10 ComprehensiveAlarmJob 退役

删除原有 `ComprehensiveAlarmJob.java` 的全部内容：
- 删除 `@Scheduled(fixedDelay = 60_000)` 注解和方法
- 删除线程池 `strategyExecutor`、`@PreDestroy`、`CountDownLatch`
- 删除 `executeStrategy()` 全部业务逻辑（已迁移到 ExecutionService）
- 可以删除该类，或保留为空壳兼容（建议删除）

### 5.11 Mapper 变更

**AlarmStrategyHazardPointMapper** 新增方法：

```java
// 原 selectHazardPointIdsByStrategyId → 改名为
List<String> selectScopeValuesByStrategyId(Long strategyId);

// 新增：插入 scope 值（兼容原 insertBinding，但 hazard_point_id 参数类型改 String）
int insertBinding(@Param("strategyId") Long strategyId, @Param("scopeValue") String scopeValue);
```

**IHazardPointQueryService（跨模块接口）** 新增方法：

```java
/** 查询指定分组下的隐患点 ID 列表 */
List<Long> listIdsByGroupId(Long groupId);
```

`*` 展开使用已有 `listMonitoring()` 方法。

### 5.12 执行日志 API

新增端点（在现有 `AlarmStrategyController` 中追加）：

```
GET /api/v1/alarm/strategies/{id}/execution-logs?pageNum=1&pageSize=20
```

响应：
```json
{
  "code": 200,
  "rows": [
    {
      "id": 1,
      "strategyId": 10,
      "triggerType": "CRON",
      "triggerSource": null,
      "hazardPointIds": "1,2,3",
      "resultLevel": 3,
      "resultStatus": "SUCCESS",
      "durationMs": 234,
      "scriptLogs": "[{\"level\":\"INFO\",\"msg\":\"开始检查\",\"ts\":5}]",
      "errorMessage": null,
      "triggeredCount": 2,
      "createTime": "2026-06-28 10:23:15"
    }
  ],
  "total": 42
}
```

## 6. 前端设计

### 6.1 CompositeAlarmScopeDialog.vue — 重写

支持三种范围模式：

```
┌─────────────────────────────────────────────┐
│  应用范围                          [×]       │
├─────────────────────────────────────────────┤
│  ○ 全部隐患点                                │
│  ○ 按分组选择                                │
│    ├ ☑ 崩塌监测组 (G001)                    │
│    ├ ☐ 滑坡监测组 (G002)                    │
│    └ ☐ 边坡监测组 (G005)                    │
│  ○ 指定隐患点                                │
│    ┌───────────────────────────────────┐    │
│    │ ☑ K1+100 边坡点                   │    │
│    │ ☑ K2+050 边坡点                   │    │
│    │ ☐ K3+200 边坡点                   │    │
│    └───────────────────────────────────┘    │
├─────────────────────────────────────────────┤
│              [取消]  [确定]                  │
└─────────────────────────────────────────────┘
```

**数据流**：
- 加载时：读取 `alarm_strategy_hazard_point` 记录，按前缀分类
  - `*` → 选中「全部隐患点」单选
  - `group:{id}` → 选中「按分组选择」+ 勾选对应分组
  - `{数字}` → 选中「指定隐患点」+ 勾选对应隐患点
- 保存时：根据选择的模式写入对应 scope 值
  - 需要 `getHazardPointGroupList` API 加载分组列表（已有 `hazardPointGroupList` API）
  - 需要 `getHazardPointPage` API 加载隐患点列表（已有）

**Props/Emits**：保持与现有接口兼容。

### 6.2 CompositeAlarmLogDrawer.vue — 修改

改为从 `alarm_strategy_execution_log` 表读取数据：

```typescript
// API 调用
const res = await getExecutionLogs(props.alarmId, { pageNum, pageSize })

// 展示
interface ExecutionLogItem {
  id: number
  strategyId: number
  triggerType: 'CRON' | 'DATA_INGEST' | 'ALARM_TRIGGER'
  triggerSource: string | null
  hazardPointIds: string | null
  resultLevel: number | null
  resultStatus: 'SUCCESS' | 'NO_ALARM' | 'FAIL' | 'TIMEOUT'
  durationMs: number
  scriptLogs: string | null  // JSON 数组
  errorMessage: string | null
  triggeredCount: number
  createTime: string
}
```

UI 展示：
- 时间轴样式，按时间倒序
- 每条记录可展开查看脚本日志详情
- triggerType 用不同颜色标签区分
- resultStatus 用不同颜色（SUCCESS=绿/NO_ALARM=灰/FAIL=红/TIMEOUT=橙）

### 6.3 script-api-docs.ts — alarm 模式新增分组

alarm 模式从 4 组扩展到 6 组：

| 分组 | 说明 | 新增 |
|------|------|------|
| `event` | 触发事件对象 | ✅ 新增 |
| `hazardPointIds` | 隐患点 ID 列表 | 已有 |
| `currentTime` | 当前时间字符串 | 已有 |
| `cache` | Redis 缓存工具 | 已有 |
| `sensor` | IoTDB 查询工具 | 已有 |
| `log` | 脚本日志工具 | ✅ 新增 |

**event 分组文档**（两种事件类型）：

```typescript
{
  name: 'event',
  description: '触发事件对象 (CRON 模式下为 null)',
  methods: [
    { signature: '.deviceId', note: '设备 ID (DataIngest)' },
    { signature: '.deviceCode', note: '设备编码 (DataIngest)' },
    { signature: '.sensorCode', note: '传感器编码 (DataIngest)' },
    { signature: '.properties', note: '属性值列表 (DataIngest)' },
    { signature: '.dataTime', note: '数据时间戳 (DataIngest)' },
    { signature: '.alarmId', note: '告警记录 ID (AlarmTrigger)' },
    { signature: '.hazardPointId', note: '隐患点 ID (AlarmTrigger)' },
    { signature: '.alarmLevel', note: '告警等级 (AlarmTrigger)' },
    { signature: '.alarmMessage', note: '告警消息 (AlarmTrigger)' }
  ]
}
```

**log 分组文档**：

```typescript
{
  name: 'log',
  description: '脚本日志工具',
  methods: [
    { signature: '.info(msg)', note: '记录 INFO 日志' },
    { signature: '.warn(msg)', note: '记录 WARN 日志' },
    { signature: '.error(msg)', note: '记录 ERROR 日志' }
  ]
}
```

### 6.4 alarm.ts — 新增 API

```typescript
/** 执行日志项 */
export interface ExecutionLogItem {
  id: number
  strategyId: number
  triggerType: 'CRON' | 'DATA_INGEST' | 'ALARM_TRIGGER'
  triggerSource: string | null
  hazardPointIds: string | null
  resultLevel: number | null
  resultStatus: 'SUCCESS' | 'NO_ALARM' | 'FAIL' | 'TIMEOUT'
  durationMs: number
  scriptLogs: string | null
  errorMessage: string | null
  triggeredCount: number
  createTime: string
}

/** 查询策略执行日志 */
export const getExecutionLogs = (strategyId: number, params?: { pageNum?: number; pageSize?: number }) =>
  request.get<{ rows: ExecutionLogItem[]; total: number }>(
    `/alarm/strategies/${strategyId}/execution-logs`, { params }
  )

/** 更新策略 scope (scope 值为字符串数组: ["*", "group:3", "123"]) */
// updateStrategy 已有，hazardPointIds 参数类型从 number[] 改为 string[]
```

## 7. 文件变更总览

### 后端（新建 11 文件，修改 6 文件）

| 文件 | 变更类型 | 说明 |
|------|----------|------|
| `ComprehensiveAlarmExecutionService.java` | **新建** | 统一执行入口 |
| `StrategyScopeResolver.java` | **新建** | 范围解析 |
| `ScriptLogger.java` | **新建** | 脚本日志工具 |
| `ComprehensiveAlarmEventListener.java` | **新建** | REALTIME 事件监听 |
| `StrategyQuartzScheduler.java` | **新建** | Quartz 动态注册 |
| `ComprehensiveAlarmQuartzJob.java` | **新建** | Quartz Job 类 |
| `ExecutionResult.java` | **新建** | 执行结果 DTO |
| `StrategyExecutionLog.java` | **新建** | 执行日志 domain |
| `StrategyExecutionLogMapper.java` + XML | **新建** | 执行日志 Mapper |
| `StrategyExecutionLogMapper.xml` | **新建** | Mapper XML |
| `ComprehensiveAlarmJob.java` | **删除** | 业务逻辑已迁移 |
| `GroovyScriptExecutor.java` | **修改** | executeWithTools 已在前置设计中实现 |
| `AlarmStrategyServiceImpl.java` | **修改** | CRUD 集成 Quartz + scope 类型适配 |
| `AlarmStrategyController.java` | **修改** | 新增 execution-logs 端点 |
| `AlarmStrategyHazardPointMapper.java` + XML | **修改** | 方法签名适配 VARCHAR |
| `AlarmStrategyMapper.xml` | **修改** | scope 查询适配 |

### 前端（新建 0，修改 4 文件）

| 文件 | 变更 |
|------|------|
| `CompositeAlarmScopeDialog.vue` | **重写**：支持全部/分组/指定三种模式 |
| `CompositeAlarmLogDrawer.vue` | **修改**：改为展示 execution_log 数据 |
| `script-api-docs.ts` | **修改**：alarm 模式新增 event + log 分组 |
| `alarm.ts` | **修改**：新增 getExecutionLogs，hazardPointIds 类型适配 |

### 数据库（1 改 + 1 新建）

| 变更 | 说明 |
|------|------|
| `alarm_strategy_hazard_point` ALTER | `hazard_point_id` BIGINT → VARCHAR(100) |
| `alarm_strategy_execution_log` CREATE | 新建执行日志表 |

## 8. 数据流

### 路径 A — CRON 定时触发

```
Quartz Scheduler (按策略 cronExpression 触发)
  → ComprehensiveAlarmQuartzJob.execute()
    → executionService.execute(strategy, null, "CRON")
      → scopeResolver.resolveScope(strategyId)
      → variables = {hazardPointIds, currentTime}  // event=null
      → tools = {cache, sensor, log}
      → groovyScriptExecutor.executeWithTools(script, variables, tools)
      → triggerAlarms(strategy, hazardPointIds, level)
        → dedupService.shouldTriggerAlarm() per hazardPoint
        → alarmRecordService.createOrUpdateAlarm()
        → eventPublisher.publishEvent(AlarmTriggeredEvent)
      → saveExecutionLog()
```

### 路径 B — DataIngest 事件触发

```
MonitorIngestConsumerService (IoTDB 写入成功)
  → publishEvent(MonitorDataIngestedEvent)
  → ComprehensiveAlarmEventListener.onDataIngested()
    → 查询 REALTIME 策略列表
    → 反查 deviceId → hazardPointIds
    → 过滤：strategy scope ∩ event hazardPoints
    → executionService.execute(strategy, event, "DATA_INGEST")
      → variables = {hazardPointIds, currentTime, event=<MonitorDataIngestedEvent>}
      → ... (同路径 A 后续)
```

### 路径 C — AlarmTrigger 事件触发

```
AlarmEvaluationEngine (阈值判据触发告警)
  → publishEvent(AlarmTriggeredEvent, alarmType="THRESHOLD")
  → ComprehensiveAlarmEventListener.onAlarmTriggered()
    → 过滤：跳过 alarmType="COMPREHENSIVE" (防循环)
    → 查询 REALTIME 策略列表
    → 过滤：strategy scope 包含 event.hazardPointId
    → executionService.execute(strategy, event, "ALARM_TRIGGER")
      → variables = {hazardPointIds, currentTime, event=<AlarmTriggeredEvent>}
      → ... (同路径 A 后续)
```

## 9. 错误处理

| 层 | 错误类型 | 处理方式 |
|----|----------|----------|
| ComprehensiveAlarmQuartzJob | 策略不存在/已停用 | 静默跳过 |
| ComprehensiveAlarmQuartzJob | 脚本执行异常 | 抛 JobExecutionException，Quartz 记录 |
| EventListener.onDataIngested | 单策略异常 | catch → log.error，不影响其他策略 |
| EventListener.onAlarmTriggered | 单策略异常 | catch → log.error，不影响其他策略 |
| ExecutionService.execute | 脚本异常 | 记录到 execution_log.error_message |
| ExecutionService.execute | 脚本超时 | GroovyScriptExecutor 已有 Future+timeout 处理 |
| StrategyQuartzScheduler | Quartz 注册失败 | log.error，不影响策略保存 |
| StrategyScopeResolver | scope 值解析失败 | 跳过该条 scope，继续处理其他 |

## 10. 测试矩阵

### 后端（18 用例）

| 测试类 | 用例数 | 覆盖场景 |
|--------|--------|----------|
| `ComprehensiveAlarmExecutionServiceTest` | 6 | CRON 执行成功 / DataIngest 事件传入 / AlarmTrigger 事件传入 / 脚本返回 0 / 脚本异常 / scope 为空跳过 |
| `StrategyScopeResolverTest` | 4 | 展开全部 / 展开分组 / 展开指定 ID / isHazardPointInScope 反向匹配 |
| `ScriptLoggerTest` | 2 | info/warn/error 收集 / toJson 序列化 |
| `ComprehensiveAlarmEventListenerTest` | 3 | DataIngest 过滤匹配 / AlarmTrigger 防循环 / 策略 scope 不匹配时跳过 |
| `StrategyQuartzSchedulerTest` | 3 | 注册成功 / 暂停恢复 / 删除 |

### 前端（8 用例）

| 测试文件 | 用例数 | 覆盖场景 |
|----------|--------|----------|
| `CompositeAlarmScopeDialog.test.ts` | 4 | 加载分组模式 / 加载指定模式 / 保存全部模式 / 切换模式 |
| `CompositeAlarmLogDrawer.test.ts` | 2 | 加载日志列表 / 展开脚本日志 |
| `script-api-docs.test.ts` | 2 | alarm 模式有 event 分组 / alarm 模式有 log 分组 |

**总计：26 个测试用例**

## 11. 验证命令

```bash
# 后端编译
cd server && mvn compile -pl zwei-iot-alarm -am

# 后端测试
cd server && mvn test -pl zwei-iot-alarm \
  -Dtest=ComprehensiveAlarmExecutionServiceTest,StrategyScopeResolverTest,ScriptLoggerTest,ComprehensiveAlarmEventListenerTest,StrategyQuartzSchedulerTest

# 前端编译
cd web && npx vue-tsc --noEmit

# 前端测试
cd web && npx vitest run src/views/alarm/components
cd web && npx vitest run src/views/basic/components/script-editor/__tests__/script-api-docs.test.ts
```
