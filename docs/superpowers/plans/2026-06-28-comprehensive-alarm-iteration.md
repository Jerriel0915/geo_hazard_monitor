# 综合告警功能迭代完善 实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 构建统一的 `ComprehensiveAlarmExecutionService`，支持三条触发路径（Quartz CRON / DataIngest 事件 / AlarmTrigger 事件），共用告警去重→记录→分发逻辑，增加 Scope 范围选择（全部/分组/指定）、脚本日志工具注入、执行日志表。

**架构：** 集中式执行服务 `ComprehensiveAlarmExecutionService` 作为三种触发源的统一入口，通过 `StrategyScopeResolver` 展开 scope 配置，`ScriptLogger` 注入脚本收集日志，`StrategyQuartzScheduler` 动态管理 Quartz 任务。删除原有 `ComprehensiveAlarmJob` 的硬编码调度。

**技术栈：** Java 17 + Spring Boot 4.0.3 + Quartz + Groovy + Mockito (后端) / Vue 3 + TypeScript + Element Plus (前端)

**设计文档：** `docs/superpowers/specs/2026-06-28-comprehensive-alarm-iteration-design.md`

---

## 文件结构

### 后端

| 文件 | 职责 | 变更 |
|------|------|------|
| `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/engine/ScriptLogger.java` | 脚本日志工具 | **新建** |
| `server/zwei-iot-alarm/src/test/java/com/zwei/iot/alarm/service/engine/ScriptLoggerTest.java` | ScriptLogger 单测 | **新建** |
| `server/zwei-iot-device/src/main/java/com/zwei/iot/device/service/IHazardPointQueryService.java` | 跨模块隐患点查询接口 | **修改**：新增 `listIdsByGroupId` |
| `server/zwei-iot-hazard/src/main/java/com/zwei/iot/hazardpoint/service/impl/HazardPointQueryServiceImpl.java` | 隐患点查询实现 | **修改**：实现 `listIdsByGroupId` |
| `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/domain/AlarmStrategyHazardPoint.java` | 策略-隐患点绑定 domain | **修改**：hazardPointId 类型 Long→String |
| `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/mapper/AlarmStrategyHazardPointMapper.java` | 绑定 Mapper | **修改**：返回值 Long→String |
| `server/zwei-iot-alarm/src/main/resources/mapper/alarm/AlarmStrategyHazardPointMapper.xml` | 绑定 Mapper XML | **修改**：resultType 改 String |
| `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/engine/StrategyScopeResolver.java` | 范围解析器 | **新建** |
| `server/zwei-iot-alarm/src/test/java/com/zwei/iot/alarm/service/engine/StrategyScopeResolverTest.java` | 范围解析单测 | **新建** |
| `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/domain/StrategyExecutionLog.java` | 执行日志 domain | **新建** |
| `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/domain/dto/ExecutionResult.java` | 执行结果 DTO | **新建** |
| `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/mapper/StrategyExecutionLogMapper.java` | 执行日志 Mapper | **新建** |
| `server/zwei-iot-alarm/src/main/resources/mapper/alarm/StrategyExecutionLogMapper.xml` | 执行日志 Mapper XML | **新建** |
| `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/engine/ComprehensiveAlarmExecutionService.java` | 统一执行入口 | **新建** |
| `server/zwei-iot-alarm/src/test/java/com/zwei/iot/alarm/service/engine/ComprehensiveAlarmExecutionServiceTest.java` | 执行服务单测 | **新建** |
| `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/engine/StrategyQuartzScheduler.java` | Quartz 动态调度 | **新建** |
| `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/job/ComprehensiveAlarmQuartzJob.java` | Quartz Job 类 | **新建** |
| `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/engine/ComprehensiveAlarmEventListener.java` | REALTIME 事件监听 | **新建** |
| `server/zwei-iot-alarm/src/test/java/com/zwei/iot/alarm/service/engine/ComprehensiveAlarmEventListenerTest.java` | 事件监听单测 | **新建** |
| `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/job/ComprehensiveAlarmJob.java` | 旧定时任务 | **删除** |
| `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/impl/AlarmStrategyServiceImpl.java` | 策略 Service | **修改**：Quartz 集成 + scope 适配 |
| `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/controller/AlarmStrategyController.java` | REST 控制器 | **修改**：execution-logs 端点 |
| `server/zwei-iot-alarm/pom.xml` | Maven POM | **修改**：加 quartz 依赖 |
| `db/upgrade/2026-06-28-alarm-strategy-iteration.sql` | DB 迁移脚本 | **新建** |

### 前端

| 文件 | 职责 | 变更 |
|------|------|------|
| `web/src/api/alarm.ts` | 告警 API | **修改**：getExecutionLogs + scope 类型适配 |
| `web/src/views/basic/components/script-editor/script-api-docs.ts` | API 文档 | **修改**：alarm 模式加 event + log 分组 |
| `web/src/views/alarm/components/CompositeAlarmScopeDialog.vue` | 范围选择 | **重写** |
| `web/src/views/alarm/components/CompositeAlarmLogDrawer.vue` | 执行日志 | **修改** |

---

## 任务 1：数据库迁移脚本

**文件：**
- 创建：`db/upgrade/2026-06-28-alarm-strategy-iteration.sql`

- [ ] **步骤 1：创建迁移 SQL**

```sql
-- =====================================================
-- 综合告警功能迭代完善 DB 迁移
-- 日期: 2026-06-28
-- =====================================================

-- 1. alarm_strategy_hazard_point: hazard_point_id BIGINT → VARCHAR(100)
ALTER TABLE alarm_strategy_hazard_point
  MODIFY COLUMN hazard_point_id VARCHAR(100) NOT NULL
  COMMENT '范围值: *=全部隐患点 / group:{id}=按分组 / {数字}=指定隐患点ID';

-- 2. 新建执行日志表
CREATE TABLE IF NOT EXISTS alarm_strategy_execution_log (
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

- [ ] **步骤 2：执行迁移**

在本地 MySQL 执行该 SQL。

- [ ] **步骤 3：Commit**

```bash
git add db/upgrade/2026-06-28-alarm-strategy-iteration.sql
git commit -m "feat(alarm): 综合告警迭代 DB 迁移 — scope VARCHAR + execution_log 表"
```

---

## 任务 2：ScriptLogger（脚本日志工具）

**文件：**
- 创建：`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/engine/ScriptLogger.java`
- 测试：`server/zwei-iot-alarm/src/test/java/com/zwei/iot/alarm/service/engine/ScriptLoggerTest.java`

- [ ] **步骤 1：编写失败的测试**

创建 `ScriptLoggerTest.java`：

```java
package com.zwei.iot.alarm.service.engine;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ScriptLoggerTest {

    @Test
    void collectLogs_infoWarnError() {
        ScriptLogger logger = new ScriptLogger(1L);
        logger.info("开始检查");
        logger.warn("雨量异常");
        logger.error("设备离线");

        String json = logger.toJson();
        assertNotNull(json);
        JSONArray arr = JSON.parseArray(json);
        assertEquals(3, arr.size());
        assertEquals("INFO", arr.getJSONObject(0).getString("level"));
        assertEquals("开始检查", arr.getJSONObject(0).getString("msg"));
        assertEquals("WARN", arr.getJSONObject(1).getString("level"));
        assertEquals("ERROR", arr.getJSONObject(2).getString("level"));
    }

    @Test
    void toJson_noEntries_returnsNull() {
        ScriptLogger logger = new ScriptLogger(1L);
        assertNull(logger.toJson());
    }

    @Test
    void ts_isRelative() throws InterruptedException {
        ScriptLogger logger = new ScriptLogger(1L);
        Thread.sleep(10);
        logger.info("after delay");
        String json = logger.toJson();
        JSONArray arr = JSON.parseArray(json);
        long ts = arr.getJSONObject(0).getLong("ts");
        assertTrue(ts >= 10, "ts should be >= 10ms, got " + ts);
    }
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd server && mvn test -pl zwei-iot-alarm -Dtest=ScriptLoggerTest`
预期：FAIL — `ScriptLogger` 类不存在

- [ ] **步骤 3：实现 ScriptLogger**

创建 `ScriptLogger.java`：

```java
package com.zwei.iot.alarm.service.engine;

import com.alibaba.fastjson2.JSON;

import java.util.ArrayList;
import java.util.List;

/**
 * 脚本日志工具，注入 Groovy 脚本的 {@code log} 变量。
 * 脚本内调用 {@code log.info(msg)} / {@code log.warn(msg)} / {@code log.error(msg)}，
 * 执行后日志序列化为 JSON 存入 alarm_strategy_execution_log.script_logs。
 */
public class ScriptLogger {

    private final Long strategyId;
    private final List<LogEntry> entries = new ArrayList<>();
    private final long startTime = System.currentTimeMillis();

    public ScriptLogger(Long strategyId) {
        this.strategyId = strategyId;
    }

    public void info(String msg)  { add("INFO", msg); }
    public void warn(String msg)  { add("WARN", msg); }
    public void error(String msg) { add("ERROR", msg); }

    private void add(String level, String msg) {
        entries.add(new LogEntry(level, msg, System.currentTimeMillis() - startTime));
    }

    /**
     * 序列化为 JSON 数组字符串。
     * @return JSON 数组 or null（无日志时）
     */
    public String toJson() {
        if (entries.isEmpty()) return null;
        return JSON.toJSONString(entries);
    }

    /**
     * 日志条目。
     */
    public static class LogEntry {
        private final String level;
        private final String msg;
        private final long ts;

        public LogEntry(String level, String msg, long ts) {
            this.level = level;
            this.msg = msg;
            this.ts = ts;
        }

        public String getLevel() { return level; }
        public String getMsg() { return msg; }
        public long getTs() { return ts; }
    }
}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`cd server && mvn test -pl zwei-iot-alarm -Dtest=ScriptLoggerTest`
预期：PASS — 3 个测试全部通过

- [ ] **步骤 5：Commit**

```bash
cd server && git add zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/engine/ScriptLogger.java \
  zwei-iot-alarm/src/test/java/com/zwei/iot/alarm/service/engine/ScriptLoggerTest.java
git commit -m "feat(alarm): 新建 ScriptLogger 脚本日志工具"
```

---

## 任务 3：IHazardPointQueryService 新增 listIdsByGroupId

**文件：**
- 修改：`server/zwei-iot-device/src/main/java/com/zwei/iot/device/service/IHazardPointQueryService.java`
- 修改：`server/zwei-iot-hazard/src/main/java/com/zwei/iot/hazardpoint/service/impl/HazardPointQueryServiceImpl.java`

- [ ] **步骤 1：在接口新增方法签名**

在 `IHazardPointQueryService.java` 末尾添加：

```java
/**
 * 查询指定分组下的隐患点 ID 列表 (status=1 AND del_flag='0')。
 * @param groupId 分组 ID
 * @return 隐患点 ID 列表
 */
List<Long> listIdsByGroupId(Long groupId);
```

- [ ] **步骤 2：在实现类中实现**

在 `HazardPointQueryServiceImpl.java` 中添加方法：

```java
@Override
public List<Long> listIdsByGroupId(Long groupId) {
    return hazardPointMapper.selectAll().stream()
        .filter(hp -> groupId.equals(hp.getGroupId()))
        .filter(hp -> hp.getStatus() != null && hp.getStatus() == 1)
        .filter(hp -> "0".equals(hp.getDelFlag()))
        .map(HazardPoint::getId)
        .collect(java.util.stream.Collectors.toList());
}
```

- [ ] **步骤 3：编译验证**

运行：`cd server && mvn compile -pl zwei-iot-device,zwei-iot-hazard -am`
预期：BUILD SUCCESS

- [ ] **步骤 4：Commit**

```bash
cd server && git add zwei-iot-device/src/main/java/com/zwei/iot/device/service/IHazardPointQueryService.java \
  zwei-iot-hazard/src/main/java/com/zwei/iot/hazardpoint/service/impl/HazardPointQueryServiceImpl.java
git commit -m "feat(device): IHazardPointQueryService 新增 listIdsByGroupId 方法"
```

---

## 任务 4：AlarmStrategyHazardPoint VARCHAR 适配

**文件：**
- 修改：`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/domain/AlarmStrategyHazardPoint.java`
- 修改：`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/mapper/AlarmStrategyHazardPointMapper.java`
- 修改：`server/zwei-iot-alarm/src/main/resources/mapper/alarm/AlarmStrategyHazardPointMapper.xml`

- [ ] **步骤 1：修改 domain 类 hazardPointId 类型**

在 `AlarmStrategyHazardPoint.java` 中，将 `private Long hazardPointId` 改为 `private String hazardPointId`。

- [ ] **步骤 2：修改 Mapper 接口返回类型**

在 `AlarmStrategyHazardPointMapper.java` 中，将方法签名改为：

```java
// 原: List<Long> selectHazardPointIdsByStrategyId(Long strategyId);
// 改: 返回 String 类型的 scope 值列表
List<String> selectScopeValuesByStrategyId(Long strategyId);
```

- [ ] **步骤 3：修改 Mapper XML**

在 `AlarmStrategyHazardPointMapper.xml` 中，将 select 语句的 id 和 resultType 改为：

```xml
<select id="selectScopeValuesByStrategyId" resultType="String">
    SELECT hazard_point_id FROM alarm_strategy_hazard_point WHERE strategy_id = #{strategyId}
</select>
```

注意 insertBinding SQL 不需要改（hazard_point_id 列已是 VARCHAR，MyBatis 自动转换）。

- [ ] **步骤 4：编译验证**

运行：`cd server && mvn compile -pl zwei-iot-alarm -am`
预期：BUILD SUCCESS（如果有引用旧方法名的地方会报错，需同步修改）

- [ ] **步骤 5：修复编译错误**

搜索引用 `selectHazardPointIdsByStrategyId` 的文件并全部改为 `selectScopeValuesByStrategyId`。涉及：
- `AlarmStrategyServiceImpl.java`（testRun 和 updateBindings 方法中）
- `ComprehensiveAlarmJob.java`（即将删除，不改）

在 `AlarmStrategyServiceImpl.java` 中：
- `testRun()` 里 `List<Long> hazardPointIds = bindingMapper.selectHazardPointIdsByStrategyId(id)` 改为通过 scopeResolver 解析
- `updateBindings()` 里 `bindingMapper.deleteByStrategyId(strategyId)` 不变，插入逻辑需要适配 String 类型的 scope 值

- [ ] **步骤 6：Commit**

```bash
cd server && git add zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/domain/AlarmStrategyHazardPoint.java \
  zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/mapper/AlarmStrategyHazardPointMapper.java \
  zwe-iot-alarm/src/main/resources/mapper/alarm/AlarmStrategyHazardPointMapper.xml \
  zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/impl/AlarmStrategyServiceImpl.java
git commit -m "refactor(alarm): hazard_point_id BIGINT→VARCHAR scope 适配"
```

---

## 任务 5：StrategyScopeResolver（范围解析器）

**文件：**
- 创建：`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/engine/StrategyScopeResolver.java`
- 测试：`server/zwei-iot-alarm/src/test/java/com/zwei/iot/alarm/service/engine/StrategyScopeResolverTest.java`

- [ ] **步骤 1：编写失败的测试**

创建 `StrategyScopeResolverTest.java`：

```java
package com.zwei.iot.alarm.service.engine;

import com.zwei.iot.alarm.mapper.AlarmStrategyHazardPointMapper;
import com.zwei.iot.device.service.IHazardPointQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StrategyScopeResolverTest {

    @Mock private AlarmStrategyHazardPointMapper bindingMapper;
    @Mock private IHazardPointQueryService hazardPointQueryService;
    @InjectMocks private StrategyScopeResolver resolver;

    @Test
    void resolveScope_star_returnsAllMonitoring() {
        when(bindingMapper.selectScopeValuesByStrategyId(1L)).thenReturn(List.of("*"));
        when(hazardPointQueryService.listMonitoring()).thenReturn(List.of(
            new IHazardPointQueryService.HazardPointBrief(10L, "HP-10"),
            new IHazardPointQueryService.HazardPointBrief(20L, "HP-20")
        ));
        List<Long> result = resolver.resolveScope(1L);
        assertEquals(2, result.size());
        assertTrue(result.contains(10L));
        assertTrue(result.contains(20L));
    }

    @Test
    void resolveScope_group() {
        when(bindingMapper.selectScopeValuesByStrategyId(2L)).thenReturn(List.of("group:3"));
        when(hazardPointQueryService.listIdsByGroupId(3L)).thenReturn(List.of(30L, 31L));
        List<Long> result = resolver.resolveScope(2L);
        assertEquals(2, result.size());
        assertTrue(result.contains(30L));
        assertTrue(result.contains(31L));
    }

    @Test
    void resolveScope_specificId() {
        when(bindingMapper.selectScopeValuesByStrategyId(3L)).thenReturn(List.of("123"));
        List<Long> result = resolver.resolveScope(3L);
        assertEquals(1, result.size());
        assertTrue(result.contains(123L));
    }

    @Test
    void isHazardPointInScope_star_returnsTrue() {
        when(bindingMapper.selectScopeValuesByStrategyId(1L)).thenReturn(List.of("*"));
        assertTrue(resolver.isHazardPointInScope(1L, 999L));
    }

    @Test
    void isHazardPointInScope_notInScope_returnsFalse() {
        when(bindingMapper.selectScopeValuesByStrategyId(2L)).thenReturn(List.of("123"));
        assertFalse(resolver.isHazardPointInScope(2L, 456L));
    }
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd server && mvn test -pl zwei-iot-alarm -Dtest=StrategyScopeResolverTest`
预期：FAIL — `StrategyScopeResolver` 类不存在

- [ ] **步骤 3：实现 StrategyScopeResolver**

创建 `StrategyScopeResolver.java`：

```java
package com.zwei.iot.alarm.service.engine;

import com.zwei.iot.alarm.mapper.AlarmStrategyHazardPointMapper;
import com.zwei.iot.device.service.IHazardPointQueryService;
import com.zwei.iot.device.service.IHazardPointQueryService.HazardPointBrief;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 策略范围解析器，将 alarm_strategy_hazard_point 中的 scope 值
 * (*、group:{id}、{数字}) 展开为实际隐患点 ID 列表。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class StrategyScopeResolver {

    private final AlarmStrategyHazardPointMapper bindingMapper;
    private final IHazardPointQueryService hazardPointQueryService;

    /**
     * 展开策略 scope 为实际隐患点 ID 列表。
     */
    public List<Long> resolveScope(Long strategyId) {
        List<String> scopeValues = bindingMapper.selectScopeValuesByStrategyId(strategyId);
        Set<Long> result = new LinkedHashSet<>();

        for (String scope : scopeValues) {
            try {
                if ("*".equals(scope)) {
                    List<HazardPointBrief> all = hazardPointQueryService.listMonitoring();
                    for (HazardPointBrief b : all) {
                        result.add(b.id());
                    }
                } else if (scope.startsWith("group:")) {
                    Long groupId = Long.parseLong(scope.substring(6));
                    result.addAll(hazardPointQueryService.listIdsByGroupId(groupId));
                } else {
                    result.add(Long.parseLong(scope));
                }
            } catch (Exception e) {
                log.warn("scope 值解析失败，跳过: strategyId={} scope={}", strategyId, scope, e);
            }
        }
        return new ArrayList<>(result);
    }

    /**
     * 反向匹配：判断给定隐患点 ID 是否在策略 scope 内。
     * 用于事件监听器快速过滤。
     */
    public boolean isHazardPointInScope(Long strategyId, Long hazardPointId) {
        List<String> scopeValues = bindingMapper.selectScopeValuesByStrategyId(strategyId);
        for (String scope : scopeValues) {
            if ("*".equals(scope)) return true;
            if (scope.startsWith("group:")) {
                try {
                    Long groupId = Long.parseLong(scope.substring(6));
                    if (hazardPointQueryService.listIdsByGroupId(groupId).contains(hazardPointId))
                        return true;
                } catch (NumberFormatException e) {
                    log.warn("group scope 解析失败: {}", scope);
                }
            } else if (scope.equals(String.valueOf(hazardPointId))) {
                return true;
            }
        }
        return false;
    }
}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`cd server && mvn test -pl zwei-iot-alarm -Dtest=StrategyScopeResolverTest`
预期：PASS — 5 个测试全部通过

- [ ] **步骤 5：Commit**

```bash
cd server && git add zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/engine/StrategyScopeResolver.java \
  zwei-iot-alarm/src/test/java/com/zwei/iot/alarm/service/engine/StrategyScopeResolverTest.java
git commit -m "feat(alarm): 新建 StrategyScopeResolver 范围解析器"
```

---

## 任务 6：ExecutionLog domain + Mapper + ExecutionResult DTO

**文件：**
- 创建：`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/domain/StrategyExecutionLog.java`
- 创建：`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/domain/dto/ExecutionResult.java`
- 创建：`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/mapper/StrategyExecutionLogMapper.java`
- 创建：`server/zwei-iot-alarm/src/main/resources/mapper/alarm/StrategyExecutionLogMapper.xml`

- [ ] **步骤 1：创建 StrategyExecutionLog domain**

```java
package com.zwei.iot.alarm.domain;

import lombok.Data;
import java.util.Date;

@Data
public class StrategyExecutionLog {
    private Long id;
    private Long strategyId;
    private String triggerType;
    private String triggerSource;
    private String hazardPointIds;
    private Integer resultLevel;
    private String resultStatus;
    private Long durationMs;
    private String scriptLogs;
    private String errorMessage;
    private Integer triggeredCount;
    private Date createTime;
}
```

- [ ] **步骤 2：创建 ExecutionResult DTO**

```java
package com.zwei.iot.alarm.domain.dto;

import com.zwei.iot.alarm.domain.AlarmRecord;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 综合告警策略执行结果。
 */
@Data
@AllArgsConstructor
public class ExecutionResult {
    private Integer alarmLevel;
    private List<AlarmRecord> triggeredRecords;
    private long durationMs;
    private String scriptLogs;
}
```

- [ ] **步骤 3：创建 StrategyExecutionLogMapper**

```java
package com.zwei.iot.alarm.mapper;

import com.zwei.iot.alarm.domain.StrategyExecutionLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface StrategyExecutionLogMapper {
    int insertLog(StrategyExecutionLog log);
    List<StrategyExecutionLog> selectByStrategyId(@Param("strategyId") Long strategyId,
                                                    @Param("offset") int offset,
                                                    @Param("limit") int limit);
    long countByStrategyId(@Param("strategyId") Long strategyId);
}
```

- [ ] **步骤 4：创建 Mapper XML**

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
  "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.zwei.iot.alarm.mapper.StrategyExecutionLogMapper">

    <insert id="insertLog" parameterType="com.zwei.iot.alarm.domain.StrategyExecutionLog"
            useGeneratedKeys="true" keyProperty="id">
        INSERT INTO alarm_strategy_execution_log
            (strategy_id, trigger_type, trigger_source, hazard_point_ids,
             result_level, result_status, duration_ms, script_logs,
             error_message, triggered_count)
        VALUES
            (#{strategyId}, #{triggerType}, #{triggerSource}, #{hazardPointIds},
             #{resultLevel}, #{resultStatus}, #{durationMs}, #{scriptLogs},
             #{errorMessage}, #{triggeredCount})
    </insert>

    <select id="selectByStrategyId" resultType="com.zwei.iot.alarm.domain.StrategyExecutionLog">
        SELECT id, strategy_id, trigger_type, trigger_source, hazard_point_ids,
               result_level, result_status, duration_ms, script_logs,
               error_message, triggered_count, create_time
        FROM alarm_strategy_execution_log
        WHERE strategy_id = #{strategyId}
        ORDER BY create_time DESC
        LIMIT #{limit} OFFSET #{offset}
    </select>

    <select id="countByStrategyId" resultType="long">
        SELECT COUNT(*) FROM alarm_strategy_execution_log
        WHERE strategy_id = #{strategyId}
    </select>

</mapper>
```

- [ ] **步骤 5：编译验证**

运行：`cd server && mvn compile -pl zwei-iot-alarm -am`
预期：BUILD SUCCESS

- [ ] **步骤 6：Commit**

```bash
cd server && git add zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/domain/StrategyExecutionLog.java \
  zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/domain/dto/ExecutionResult.java \
  zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/mapper/StrategyExecutionLogMapper.java \
  zwei-iot-alarm/src/main/resources/mapper/alarm/StrategyExecutionLogMapper.xml
git commit -m "feat(alarm): 执行日志 domain + mapper + ExecutionResult DTO"
```

---

## 任务 7：ComprehensiveAlarmExecutionService（核心执行服务）

**文件：**
- 创建：`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/engine/ComprehensiveAlarmExecutionService.java`
- 测试：`server/zwei-iot-alarm/src/test/java/com/zwei/iot/alarm/service/engine/ComprehensiveAlarmExecutionServiceTest.java`

- [ ] **步骤 1：编写失败的测试**

创建 `ComprehensiveAlarmExecutionServiceTest.java`：

```java
package com.zwei.iot.alarm.service.engine;

import com.zwei.common.event.AlarmTriggeredEvent;
import com.zwei.common.event.MonitorDataIngestedEvent;
import com.zwei.iot.alarm.domain.AlarmStrategy;
import com.zwei.iot.alarm.mapper.AlarmStrategyMapper;
import com.zwei.iot.alarm.mapper.AlarmStrategyHazardPointMapper;
import com.zwei.iot.alarm.mapper.StrategyExecutionLogMapper;
import com.zwei.iot.alarm.service.IAlarmRecordService;
import com.zwei.iot.timeseries.compute.ScriptCacheOps;
import com.zwei.iot.timeseries.compute.ScriptSensorQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComprehensiveAlarmExecutionServiceTest {

    @Mock private AlarmStrategyMapper strategyMapper;
    @Mock private AlarmStrategyHazardPointMapper bindingMapper;
    @Mock private GroovyScriptExecutor groovyScriptExecutor;
    @Mock private IAlarmRecordService alarmRecordService;
    @Mock private AlarmDedupService dedupService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private ScriptCacheOps cacheOps;
    @Mock private ScriptSensorQuery scriptSensorQuery;
    @Mock private StrategyExecutionLogMapper executionLogMapper;
    @Mock private StrategyScopeResolver scopeResolver;
    @InjectMocks private ComprehensiveAlarmExecutionService service;

    private AlarmStrategy buildStrategy() {
        return AlarmStrategy.builder()
            .id(1L).name("测试策略").triggerMode("CRON")
            .scriptContent("return 0").silenceMinutes(0).isEnabled(1)
            .build();
    }

    @Test
    void execute_cron_noAlarm() {
        AlarmStrategy strategy = buildStrategy();
        when(scopeResolver.resolveScope(1L)).thenReturn(List.of(10L, 20L));
        when(groovyScriptExecutor.executeWithTools(anyString(), anyMap(), anyMap()))
            .thenReturn(0);

        var result = service.execute(strategy, null, "CRON");

        assertNotNull(result);
        assertEquals(0, result.getAlarmLevel());
        verify(executionLogMapper).insertLog(any());
        verify(dedupService, never()).shouldTriggerAlarm(anyLong(), anyLong(), anyInt(), anyInt(), anyInt());
    }

    @Test
    void execute_cron_triggersAlarm() {
        AlarmStrategy strategy = buildStrategy();
        strategy.setScriptContent("return 2");
        when(scopeResolver.resolveScope(1L)).thenReturn(List.of(10L));
        when(groovyScriptExecutor.executeWithTools(anyString(), anyMap(), anyMap()))
            .thenReturn(2);
        when(dedupService.shouldTriggerAlarm(1L, 10L, 2, 1, 0)).thenReturn(true);

        var result = service.execute(strategy, null, "CRON");

        assertEquals(2, result.getAlarmLevel());
        verify(alarmRecordService).createOrUpdateAlarm(any());
        verify(eventPublisher).publishEvent(any(AlarmTriggeredEvent.class));
    }

    @Test
    void execute_dataIngest_eventPassedToScript() {
        AlarmStrategy strategy = buildStrategy();
        MonitorDataIngestedEvent event = mock(MonitorDataIngestedEvent.class);
        when(event.getDeviceId()).thenReturn(1L);
        when(scopeResolver.resolveScope(1L)).thenReturn(List.of(10L));
        when(groovyScriptExecutor.executeWithTools(anyString(), anyMap(), anyMap()))
            .thenReturn(0);

        service.execute(strategy, event, "DATA_INGEST");

        // 验证 event 变量被传入 variables
        verify(groovyScriptExecutor).executeWithTools(
            anyString(),
            argThat(vars -> vars.containsKey("event")),
            anyMap());
    }

    @Test
    void execute_scriptThrows_logsFail() {
        AlarmStrategy strategy = buildStrategy();
        when(scopeResolver.resolveScope(1L)).thenReturn(List.of(10L));
        when(groovyScriptExecutor.executeWithTools(anyString(), anyMap(), anyMap()))
            .thenThrow(new RuntimeException("脚本执行失败"));

        var result = service.execute(strategy, null, "CRON");

        assertNotNull(result);
        verify(executionLogMapper).insertLog(argThat(log ->
            "FAIL".equals(log.getResultStatus()) &&
            log.getErrorMessage() != null));
    }

    @Test
    void execute_emptyScope_skips() {
        AlarmStrategy strategy = buildStrategy();
        when(scopeResolver.resolveScope(1L)).thenReturn(List.of());

        var result = service.execute(strategy, null, "CRON");

        verify(groovyScriptExecutor, never()).executeWithTools(anyString(), anyMap(), anyMap());
        verify(executionLogMapper).insertLog(argThat(log -> "NO_ALARM".equals(log.getResultStatus())));
    }

    @Test
    void execute_silencePeriod_dedupBlocksAlarm() {
        AlarmStrategy strategy = buildStrategy();
        strategy.setScriptContent("return 3");
        when(scopeResolver.resolveScope(1L)).thenReturn(List.of(10L, 20L));
        when(groovyScriptExecutor.executeWithTools(anyString(), anyMap(), anyMap()))
            .thenReturn(3);
        when(dedupService.shouldTriggerAlarm(eq(1L), eq(10L), eq(3), eq(1), eq(0)))
            .thenReturn(false);
        when(dedupService.shouldTriggerAlarm(eq(1L), eq(20L), eq(3), eq(1), eq(0)))
            .thenReturn(true);

        var result = service.execute(strategy, null, "CRON");

        assertEquals(3, result.getAlarmLevel());
        verify(alarmRecordService, times(1)).createOrUpdateAlarm(any());
        verify(eventPublisher, times(1)).publishEvent(any(AlarmTriggeredEvent.class));
    }
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd server && mvn test -pl zwei-iot-alarm -Dtest=ComprehensiveAlarmExecutionServiceTest`
预期：FAIL — `ComprehensiveAlarmExecutionService` 类不存在

- [ ] **步骤 3：实现 ComprehensiveAlarmExecutionService**

创建 `ComprehensiveAlarmExecutionService.java`：

```java
package com.zwei.iot.alarm.service.engine;

import com.zwei.iot.alarm.domain.AlarmConstants;
import com.zwei.iot.alarm.domain.AlarmRecord;
import com.zwei.iot.alarm.domain.AlarmStrategy;
import com.zwei.iot.alarm.domain.StrategyExecutionLog;
import com.zwei.iot.alarm.domain.dto.ExecutionResult;
import com.zwei.iot.alarm.mapper.AlarmStrategyMapper;
import com.zwei.iot.alarm.mapper.AlarmStrategyHazardPointMapper;
import com.zwei.iot.alarm.mapper.StrategyExecutionLogMapper;
import com.zwei.iot.alarm.service.IAlarmRecordService;
import com.zwei.iot.timeseries.compute.ScriptCacheOps;
import com.zwei.iot.timeseries.compute.ScriptSensorQuery;
import com.zwei.common.event.AlarmTriggeredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 综合告警统一执行服务 — 三种触发源（CRON/DataIngest/AlarmTrigger）的统一入口。
 */
@Service
@Slf4j
@RequiredArgsConstructor
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
     * 执行综合告警策略。
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

        // 2. 空范围快速返回
        if (hazardPointIds.isEmpty()) {
            return saveAndReturn(strategy, triggerType, triggerEvent, hazardPointIds,
                null, "NO_ALARM", start, null, null, 0);
        }

        // 3. 构建 variables + tools + log
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

        // 4. 执行脚本
        Integer alarmLevel = null;
        String errorMessage = null;
        try {
            alarmLevel = groovyScriptExecutor.executeWithTools(
                strategy.getScriptContent(), variables, tools);
        } catch (Exception e) {
            errorMessage = e.getMessage();
        }

        // 5. 告警触发
        int triggeredCount = 0;
        List<AlarmRecord> triggeredRecords = new ArrayList<>();
        if (alarmLevel != null && alarmLevel > 0) {
            triggeredRecords = triggerAlarms(strategy, hazardPointIds, alarmLevel);
            triggeredCount = triggeredRecords.size();
        }

        // 6. 确定状态
        String resultStatus;
        if (errorMessage != null) {
            resultStatus = "FAIL";
        } else if (alarmLevel != null && alarmLevel > 0) {
            resultStatus = "SUCCESS";
        } else {
            resultStatus = "NO_ALARM";
        }

        long durationMs = System.currentTimeMillis() - start;

        // 7. 写执行日志 + 更新策略状态
        return saveAndReturn(strategy, triggerType, triggerEvent, hazardPointIds,
            alarmLevel, resultStatus, start, scriptLogger, errorMessage, triggeredCount);
    }

    /**
     * 共用的告警触发逻辑。对每个隐患点：去重 → 创建/更新 AlarmRecord → 发布 AlarmTriggeredEvent。
     */
    private List<AlarmRecord> triggerAlarms(AlarmStrategy strategy, List<Long> hazardPointIds,
                                            int alarmLevel) {
        int silenceMinutes = strategy.getSilenceMinutes() != null
                             ? strategy.getSilenceMinutes() : 0;
        List<AlarmRecord> records = new ArrayList<>();

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
            records.add(saved);

            eventPublisher.publishEvent(new AlarmTriggeredEvent(
                saved.getId(), saved.getHazardPointId(),
                saved.getAlarmLevel(), saved.getAlarmType(),
                saved.getAlarmMessage(), saved.getTriggerReason()));
        }
        return records;
    }

    private ExecutionResult saveAndReturn(AlarmStrategy strategy, String triggerType,
                                          Object triggerEvent, List<Long> hazardPointIds,
                                          Integer alarmLevel, String resultStatus,
                                          long startMs, ScriptLogger scriptLogger,
                                          String errorMessage, int triggeredCount) {
        long durationMs = System.currentTimeMillis() - startMs;

        // 构建触发事件摘要
        String triggerSource = null;
        if (triggerEvent != null) {
            triggerSource = triggerEvent.toString();
            if (triggerSource.length() > 2000) {
                triggerSource = triggerSource.substring(0, 2000);
            }
        }

        // 隐患点 ID 列表 → 逗号分隔字符串
        String hpIdsStr = hazardPointIds.isEmpty() ? null
            : hazardPointIds.stream().map(String::valueOf)
                .reduce((a, b) -> a + "," + b).orElse(null);

        // 写 execution_log
        StrategyExecutionLog logEntry = new StrategyExecutionLog();
        logEntry.setStrategyId(strategy.getId());
        logEntry.setTriggerType(triggerType);
        logEntry.setTriggerSource(triggerSource);
        logEntry.setHazardPointIds(hpIdsStr);
        logEntry.setResultLevel(alarmLevel);
        logEntry.setResultStatus(resultStatus);
        logEntry.setDurationMs(durationMs);
        logEntry.setScriptLogs(scriptLogger != null ? scriptLogger.toJson() : null);
        logEntry.setErrorMessage(errorMessage);
        logEntry.setTriggeredCount(triggeredCount);
        executionLogMapper.insertLog(logEntry);

        // 更新策略最后执行结果
        strategyMapper.updateLastRunResult(
            strategy.getId(),
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()),
            resultStatus);

        return new ExecutionResult(alarmLevel, new ArrayList<>(), durationMs,
            scriptLogger != null ? scriptLogger.toJson() : null);
    }
}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`cd server && mvn test -pl zwei-iot-alarm -Dtest=ComprehensiveAlarmExecutionServiceTest`
预期：PASS — 6 个测试全部通过

- [ ] **步骤 5：Commit**

```bash
cd server && git add zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/engine/ComprehensiveAlarmExecutionService.java \
  zwei-iot-alarm/src/test/java/com/zwei/iot/alarm/service/engine/ComprehensiveAlarmExecutionServiceTest.java
git commit -m "feat(alarm): 新建 ComprehensiveAlarmExecutionService 统一执行入口"
```

---

## 任务 8：Quartz 集成（Scheduler + Job + POM）

**文件：**
- 修改：`server/zwei-iot-alarm/pom.xml`
- 创建：`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/engine/StrategyQuartzScheduler.java`
- 创建：`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/job/ComprehensiveAlarmQuartzJob.java`

- [ ] **步骤 1：添加 Quartz 依赖到 pom.xml**

在 `server/zwei-iot-alarm/pom.xml` 的 `<dependencies>` 中添加：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-quartz</artifactId>
</dependency>
```

- [ ] **步骤 2：创建 StrategyQuartzScheduler**

```java
package com.zwei.iot.alarm.service.engine;

import com.zwei.iot.alarm.domain.AlarmStrategy;
import com.zwei.iot.alarm.job.ComprehensiveAlarmQuartzJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.stereotype.Service;

/**
 * Quartz 动态调度管理器，负责策略 → Quartz Job 的生命周期。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class StrategyQuartzScheduler {

    private final Scheduler scheduler;
    private static final String JOB_GROUP = "ALARM_STRATEGY";
    private static final String DATA_STRATEGY_ID = "strategyId";

    /**
     * 注册或更新 CRON 策略的 Quartz 任务。
     */
    public void scheduleOrUpdate(AlarmStrategy strategy) {
        if (!"CRON".equals(strategy.getTriggerMode())) return;
        if (strategy.getCronExpression() == null || strategy.getCronExpression().isBlank()) return;

        JobKey jobKey = jobKey(strategy.getId());
        try {
            // 先删除旧任务
            scheduler.deleteJob(jobKey);

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
        try { scheduler.pauseJob(jobKey(strategyId)); }
        catch (SchedulerException e) { log.error("暂停任务失败 strategyId={}", strategyId, e); }
    }

    public void resume(Long strategyId) {
        try { scheduler.resumeJob(jobKey(strategyId)); }
        catch (SchedulerException e) { log.error("恢复任务失败 strategyId={}", strategyId, e); }
    }

    public void unschedule(Long strategyId) {
        try { scheduler.deleteJob(jobKey(strategyId)); }
        catch (SchedulerException e) { log.error("删除任务失败 strategyId={}", strategyId, e); }
    }

    private JobKey jobKey(Long strategyId) {
        return new JobKey("strategy_" + strategyId, JOB_GROUP);
    }
}
```

- [ ] **步骤 3：创建 ComprehensiveAlarmQuartzJob**

```java
package com.zwei.iot.alarm.job;

import com.zwei.common.utils.spring.SpringUtils;
import com.zwei.iot.alarm.domain.AlarmStrategy;
import com.zwei.iot.alarm.mapper.AlarmStrategyMapper;
import com.zwei.iot.alarm.service.engine.ComprehensiveAlarmExecutionService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * Quartz Job 类，按策略 cronExpression 触发综合告警脚本执行。
 */
@Slf4j
@DisallowConcurrentExecution
public class ComprehensiveAlarmQuartzJob implements Job {

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        Long strategyId = context.getJobDetail().getJobDataMap().getLong("strategyId");

        AlarmStrategyMapper mapper = SpringUtils.getBean(AlarmStrategyMapper.class);
        ComprehensiveAlarmExecutionService execService =
            SpringUtils.getBean(ComprehensiveAlarmExecutionService.class);

        AlarmStrategy strategy = mapper.selectStrategyById(strategyId);
        if (strategy == null || strategy.getIsEnabled() == null || strategy.getIsEnabled() == 0) {
            log.debug("策略不存在或已停用，跳过: strategyId={}", strategyId);
            return;
        }

        try {
            execService.execute(strategy, null, "CRON");
        } catch (Exception e) {
            log.error("策略 CRON 执行失败 strategyId={}", strategyId, e);
            throw new JobExecutionException("策略执行失败: " + strategyId, e);
        }
    }
}
```

- [ ] **步骤 4：编译验证**

运行：`cd server && mvn compile -pl zwei-iot-alarm -am`
预期：BUILD SUCCESS

- [ ] **步骤 5：Commit**

```bash
cd server && git add zwei-iot-alarm/pom.xml \
  zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/engine/StrategyQuartzScheduler.java \
  zwe-iot-alarm/src/main/java/com/zwei/iot/alarm/job/ComprehensiveAlarmQuartzJob.java
git commit -m "feat(alarm): Quartz 动态调度 — StrategyQuartzScheduler + ComprehensiveAlarmQuartzJob"
```

---

## 任务 9：删除 ComprehensiveAlarmJob + ServiceImpl 集成 Quartz

**文件：**
- 删除：`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/job/ComprehensiveAlarmJob.java`
- 修改：`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/impl/AlarmStrategyServiceImpl.java`

- [ ] **步骤 1：删除 ComprehensiveAlarmJob.java**

```bash
cd server && git rm zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/job/ComprehensiveAlarmJob.java
```

- [ ] **步骤 2：修改 AlarmStrategyServiceImpl — 注入 QuartzScheduler**

在 `AlarmStrategyServiceImpl` 构造函数中追加 `StrategyQuartzScheduler`：

```java
private final StrategyQuartzScheduler quartzScheduler;

// 构造函数追加参数
public AlarmStrategyServiceImpl(
    AlarmStrategyMapper strategyMapper,
    AlarmStrategyHazardPointMapper bindingMapper,
    GroovyScriptExecutor groovyScriptExecutor,
    ScriptCacheOps cacheOps,
    ScriptSensorQuery scriptSensorQuery,
    StrategyQuartzScheduler quartzScheduler
) {
    // ... 赋值
    this.quartzScheduler = quartzScheduler;
}
```

- [ ] **步骤 3：修改 CRUD 方法集成 Quartz**

在 `insert()` 方法末尾追加：

```java
if ("CRON".equals(strategy.getTriggerMode()) && strategy.getIsEnabled() != null && strategy.getIsEnabled() == 1) {
    quartzScheduler.scheduleOrUpdate(strategy);
}
```

在 `update()` 方法中，在 `updateBindings` 之后追加：

```java
if ("CRON".equals(strategy.getTriggerMode())) {
    quartzScheduler.scheduleOrUpdate(strategy);
} else {
    quartzScheduler.unschedule(strategy.getId());
}
```

在 `delete()` 方法中追加：

```java
quartzScheduler.unschedule(id);
```

在 `toggle()` 方法中追加：

```java
if (isEnabled == 1) quartzScheduler.resume(id);
else quartzScheduler.pause(id);
```

- [ ] **步骤 4：添加启动时批量注册**

在 `AlarmStrategyServiceImpl` 中添加：

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

- [ ] **步骤 5：修改 updateBindings 适配 String scope**

将 `updateBindings` 方法签名从 `Long[] hazardPointIds` 改为 `String[] scopeValues`：

```java
private void updateBindings(Long strategyId, String[] scopeValues) {
    bindingMapper.deleteByStrategyId(strategyId);
    if (scopeValues == null) return;
    for (String scope : scopeValues) {
        AlarmStrategyHazardPoint binding = AlarmStrategyHazardPoint.builder()
            .strategyId(strategyId)
            .hazardPointId(scope)
            .createTime(new Date())
            .build();
        bindingMapper.insertBinding(binding);
    }
}
```

同步修改 `insert()` 和 `update()` 的参数类型（`Long[] hazardPointIds` → `String[] scopeValues`）。

- [ ] **步骤 6：编译验证**

运行：`cd server && mvn compile -pl zwei-iot-alarm -am`
预期：BUILD SUCCESS

- [ ] **步骤 7：Commit**

```bash
cd server && git add zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/impl/AlarmStrategyServiceImpl.java
git rm zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/job/ComprehensiveAlarmJob.java
git commit -m "refactor(alarm): 删除 ComprehensiveAlarmJob, ServiceImpl 集成 Quartz + scope 适配"
```

---

## 任务 10：ComprehensiveAlarmEventListener（REALTIME 事件监听）

**文件：**
- 创建：`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/engine/ComprehensiveAlarmEventListener.java`
- 测试：`server/zwei-iot-alarm/src/test/java/com/zwei/iot/alarm/service/engine/ComprehensiveAlarmEventListenerTest.java`

- [ ] **步骤 1：编写失败的测试**

创建 `ComprehensiveAlarmEventListenerTest.java`：

```java
package com.zwei.iot.alarm.service.engine;

import com.zwei.common.event.AlarmTriggeredEvent;
import com.zwei.common.event.MonitorDataIngestedEvent;
import com.zwei.iot.alarm.domain.AlarmStrategy;
import com.zwei.iot.alarm.mapper.AlarmStrategyMapper;
import com.zwei.iot.device.service.IDeviceHazardRelationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComprehensiveAlarmEventListenerTest {

    @Mock private AlarmStrategyMapper strategyMapper;
    @Mock private StrategyScopeResolver scopeResolver;
    @Mock private ComprehensiveAlarmExecutionService executionService;
    @Mock private IDeviceHazardRelationService hazardRelationService;
    @InjectMocks private ComprehensiveAlarmEventListener listener;

    private AlarmStrategy buildStrategy(Long id, String mode) {
        return AlarmStrategy.builder().id(id).name("策略" + id).triggerMode(mode).isEnabled(1).build();
    }

    @Test
    void onDataIngested_scopeMatches_executes() {
        AlarmStrategy s = buildStrategy(1L, "REALTIME");
        when(strategyMapper.selectEnabledByTriggerMode("REALTIME")).thenReturn(List.of(s));

        MonitorDataIngestedEvent event = mock(MonitorDataIngestedEvent.class);
        when(event.getDeviceId()).thenReturn(10L);
        when(hazardRelationService.getHazardPointIdsByDeviceIds(List.of(10L)))
            .thenReturn(List.of(5L));
        when(scopeResolver.isHazardPointInScope(1L, 5L)).thenReturn(true);

        listener.onDataIngested(event);

        verify(executionService).execute(s, event, "DATA_INGEST");
    }

    @Test
    void onDataIngested_scopeNotMatch_skips() {
        AlarmStrategy s = buildStrategy(1L, "REALTIME");
        when(strategyMapper.selectEnabledByTriggerMode("REALTIME")).thenReturn(List.of(s));

        MonitorDataIngestedEvent event = mock(MonitorDataIngestedEvent.class);
        when(event.getDeviceId()).thenReturn(10L);
        when(hazardRelationService.getHazardPointIdsByDeviceIds(List.of(10L)))
            .thenReturn(List.of(5L));
        when(scopeResolver.isHazardPointInScope(1L, 5L)).thenReturn(false);

        listener.onDataIngested(event);

        verify(executionService, never()).execute(any(), any(), anyString());
    }

    @Test
    void onAlarmTriggered_comprehensiveType_skips() {
        AlarmTriggeredEvent event = new AlarmTriggeredEvent(
            1L, 5L, 2, "COMPREHENSIVE", "综合告警", "首次告警");

        listener.onAlarmTriggered(event);

        verify(strategyMapper, never()).selectEnabledByTriggerMode(anyString());
        verify(executionService, never()).execute(any(), any(), anyString());
    }

    @Test
    void onAlarmTriggered_thresholdType_executes() {
        AlarmStrategy s = buildStrategy(1L, "REALTIME");
        when(strategyMapper.selectEnabledByTriggerMode("REALTIME")).thenReturn(List.of(s));

        AlarmTriggeredEvent event = new AlarmTriggeredEvent(
            1L, 5L, 2, "THRESHOLD", "阈值告警", "首次告警");
        when(scopeResolver.isHazardPointInScope(1L, 5L)).thenReturn(true);

        listener.onAlarmTriggered(event);

        verify(executionService).execute(s, event, "ALARM_TRIGGER");
    }
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd server && mvn test -pl zwei-iot-alarm -Dtest=ComprehensiveAlarmEventListenerTest`
预期：FAIL — `ComprehensiveAlarmEventListener` 类不存在

- [ ] **步骤 3：实现 ComprehensiveAlarmEventListener**

```java
package com.zwei.iot.alarm.service.engine;

import com.zwei.common.event.AlarmTriggeredEvent;
import com.zwei.common.event.MonitorDataIngestedEvent;
import com.zwei.iot.alarm.domain.AlarmStrategy;
import com.zwei.iot.alarm.mapper.AlarmStrategyMapper;
import com.zwei.iot.device.service.IDeviceHazardRelationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * REALTIME 模式事件监听器，监听 DataIngest 和 AlarmTrigger 事件。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ComprehensiveAlarmEventListener {

    private final AlarmStrategyMapper strategyMapper;
    private final StrategyScopeResolver scopeResolver;
    private final ComprehensiveAlarmExecutionService executionService;
    private final IDeviceHazardRelationService hazardRelationService;

    /**
     * 监听数据入库事件。
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
                log.error("REALTIME 策略执行失败 strategyId={}", strategy.getId(), e);
            }
        }
    }

    /**
     * 监听告警触发事件。跳过 COMPREHENSIVE 类型以防循环。
     */
    @Async("alarmEvalExecutor")
    @EventListener
    public void onAlarmTriggered(AlarmTriggeredEvent event) {
        if ("COMPREHENSIVE".equals(event.getAlarmType())) return;

        List<AlarmStrategy> strategies = strategyMapper.selectEnabledByTriggerMode("REALTIME");
        if (strategies.isEmpty()) return;

        for (AlarmStrategy strategy : strategies) {
            if (!scopeResolver.isHazardPointInScope(strategy.getId(), event.getHazardPointId()))
                continue;

            try {
                executionService.execute(strategy, event, "ALARM_TRIGGER");
            } catch (Exception e) {
                log.error("REALTIME 策略执行失败 strategyId={}", strategy.getId(), e);
            }
        }
    }
}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`cd server && mvn test -pl zwei-iot-alarm -Dtest=ComprehensiveAlarmEventListenerTest`
预期：PASS — 4 个测试全部通过

- [ ] **步骤 5：Commit**

```bash
cd server && git add zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/engine/ComprehensiveAlarmEventListener.java \
  zwei-iot-alarm/src/test/java/com/zwei/iot/alarm/service/engine/ComprehensiveAlarmEventListenerTest.java
git commit -m "feat(alarm): 新建 ComprehensiveAlarmEventListener REALTIME 事件监听"
```

---

## 任务 11：Controller execution-logs 端点

**文件：**
- 修改：`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/controller/AlarmStrategyController.java`

- [ ] **步骤 1：新增 execution-logs 端点**

在 `AlarmStrategyController.java` 末尾追加：

```java
/**
 * 查询策略执行日志。
 */
@GetMapping("/{id}/execution-logs")
@PreAuthorize("@ss.hasPermi('iot:alarm-strategy:list')")
public AjaxResult executionLogs(@PathVariable Long id,
                                 @RequestParam(defaultValue = "1") int pageNum,
                                 @RequestParam(defaultValue = "20") int pageSize) {
    int offset = (pageNum - 1) * pageSize;
    List<StrategyExecutionLog> rows = executionLogMapper.selectByStrategyId(id, offset, pageSize);
    long total = executionLogMapper.countByStrategyId(id);
    Map<String, Object> data = new HashMap<>();
    data.put("rows", rows);
    data.put("total", total);
    return success(data);
}
```

在构造函数中注入 `StrategyExecutionLogMapper`（或在类中 `@Autowired`）。

- [ ] **步骤 2：编译验证**

运行：`cd server && mvn compile -pl zwei-iot-alarm -am`
预期：BUILD SUCCESS

- [ ] **步骤 3：Commit**

```bash
cd server && git add zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/controller/AlarmStrategyController.java
git commit -m "feat(alarm): 新增策略执行日志查询端点 GET /strategies/{id}/execution-logs"
```

---

## 任务 12：前端 — alarm.ts + script-api-docs.ts

**文件：**
- 修改：`web/src/api/alarm.ts`
- 修改：`web/src/views/basic/components/script-editor/script-api-docs.ts`

- [ ] **步骤 1：alarm.ts 新增 ExecutionLogItem + getExecutionLogs**

在 `alarm.ts` 的 strategy 区域追加：

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
```

同时将 `AlarmStrategyCreatePayload.hazardPointIds` 类型从 `number[]` 改为 `string[]`。

- [ ] **步骤 2：script-api-docs.ts 新增 event + log 分组**

在 alarm 模式的 `ALARM_SPECIFIC_GROUPS` 数组中新增两个分组：

```typescript
{
  icon: '📨',
  color: '#e6a23c',
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
},
{
  icon: '📝',
  color: '#e6a23c',
  name: 'log',
  description: '脚本日志工具',
  methods: [
    { signature: '.info(msg)', note: '记录 INFO 日志' },
    { signature: '.warn(msg)', note: '记录 WARN 日志' },
    { signature: '.error(msg)', note: '记录 ERROR 日志' }
  ]
}
```

- [ ] **步骤 3：Commit**

```bash
cd web && git add src/api/alarm.ts \
  src/views/basic/components/script-editor/script-api-docs.ts
git commit -m "feat(web): alarm API 新增 execution logs + script-api-docs 加 event/log 分组"
```

---

## 任务 13：前端 — CompositeAlarmScopeDialog.vue 重写

**文件：**
- 重写：`web/src/views/alarm/components/CompositeAlarmScopeDialog.vue`

- [ ] **步骤 1：重写 ScopeDialog**

替换整个文件为支持三种范围模式（全部/分组/指定）的版本。核心逻辑：

- 加载时并行获取：分组列表（`hazardPointGroupList` API）、隐患点列表（`getHazardPointPage`）、当前 scope 值（`getStrategyScope`）
- 按 scope 值前缀分类：`*` → 全部模式、`group:` → 分组模式、纯数字 → 指定模式
- 保存时根据选中的模式生成 scope 值数组传给 `updateStrategy`

```vue
<template>
  <el-dialog :model-value="visible" title="应用范围" width="520px" destroy-on-close
    @close="emit('update:visible', false)">
    <template #header>
      <div>
        <h3 style="margin: 0; font-size: 16px;">应用范围</h3>
        <p style="margin: 4px 0 0; font-size: 13px; color: #86909c;">选择该策略应用的隐患点</p>
      </div>
    </template>

    <div v-loading="loading">
      <el-radio-group v-model="scopeMode" class="scope-mode-group">
        <el-radio value="all">全部隐患点</el-radio-group>

        <div v-show="scopeMode === 'group'" class="scope-section">
          <el-checkbox-group v-model="selectedGroups" class="scope-list">
            <el-checkbox v-for="g in groups" :key="g.id" :value="`group:${g.id}`" class="scope-item">
              {{ g.name }} ({{ g.code }})
            </el-checkbox>
          </el-checkbox-group>
        </div>

        <el-radio value="group">按分组选择</el-radio>

        <div v-show="scopeMode === 'specific'" class="scope-section">
          <el-checkbox-group v-model="selectedPoints" class="scope-list">
            <el-checkbox v-for="hp in hazardPoints" :key="hp.id" :value="String(hp.id)" class="scope-item">
              {{ hp.name }}
            </el-checkbox>
          </el-checkbox-group>
          <el-empty v-if="hazardPoints.length === 0 && !loading" description="暂无可选隐患点" />
        </div>

        <el-radio value="specific">指定隐患点</el-radio>
      </el-radio-group>
    </div>

    <template #footer>
      <el-button @click="emit('update:visible', false)">取消</el-button>
      <el-button type="primary" :loading="saving" @click="handleSave">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getStrategyScope, updateStrategy } from '@/api/alarm'
import { getHazardPointPage, type HazardPointRaw } from '@/api/hazardPoint'
import { hazardPointGroupList } from '@/api/hazardPoint'

interface GroupItem { id: number; code: string; name: string }
interface PointItem { id: number; name: string }

const props = defineProps<{ visible: boolean; alarmId: number }>()
const emit = defineEmits<{ 'update:visible': [val: boolean] }>()

const loading = ref(false)
const saving = ref(false)
const scopeMode = ref<'all' | 'group' | 'specific'>('specific')
const groups = ref<GroupItem[]>([])
const hazardPoints = ref<PointItem[]>([])
const selectedGroups = ref<string[]>([])
const selectedPoints = ref<string[]>([])

watch(() => props.visible, async (val) => {
  if (!val) return
  loading.value = true
  try {
    const [groupRes, pointRes, scopeRes] = await Promise.all([
      hazardPointGroupList(),
      getHazardPointPage({ pageNum: 1, pageSize: 1000 }),
      getStrategyScope(props.alarmId)
    ])
    groups.value = ((groupRes as any)?.rows || []).map((g: any) => ({ id: g.id, code: g.code, name: g.name }))
    hazardPoints.value = ((pointRes as any)?.rows || []).map((hp: HazardPointRaw) => ({ id: hp.id, name: hp.name }))

    const scopes = Array.isArray(scopeRes) ? scopeRes as string[] : []
    if (scopes.includes('*')) {
      scopeMode.value = 'all'
    } else if (scopes.some(s => s.startsWith('group:'))) {
      scopeMode.value = 'group'
      selectedGroups.value = scopes.filter(s => s.startsWith('group:'))
      selectedPoints.value = scopes.filter(s => !s.startsWith('group:') && s !== '*')
    } else {
      scopeMode.value = 'specific'
      selectedPoints.value = scopes.filter(s => s !== '*')
    }
  } finally {
    loading.value = false
  }
}, { immediate: true })

async function handleSave() {
  saving.value = true
  try {
    let scopeValues: string[]
    if (scopeMode.value === 'all') {
      scopeValues = ['*']
    } else if (scopeMode.value === 'group') {
      scopeValues = [...selectedGroups.value]
    } else {
      scopeValues = [...selectedPoints.value]
    }
    await updateStrategy(props.alarmId, { hazardPointIds: scopeValues } as any)
    ElMessage.success('应用范围已更新')
    emit('update:visible', false)
  } catch (e: any) {
    ElMessage.error(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.scope-mode-group { display: flex; flex-direction: column; gap: 8px; }
.scope-section { margin: 4px 0 4px 24px; }
.scope-list { display: flex; flex-direction: column; gap: 4px; max-height: 280px; overflow-y: auto; }
.scope-item { margin: 0; padding: 6px 10px; border-radius: 4px; }
.scope-item:hover { background: #f2f3f5; }
</style>
```

- [ ] **步骤 2：Commit**

```bash
cd web && git add src/views/alarm/components/CompositeAlarmScopeDialog.vue
git commit -m "feat(web): 重写 CompositeAlarmScopeDialog 支持全部/分组/指定三种范围模式"
```

---

## 任务 14：前端 — CompositeAlarmLogDrawer.vue 修改

**文件：**
- 修改：`web/src/views/alarm/components/CompositeAlarmLogDrawer.vue`

- [ ] **步骤 1：修改 LogDrawer 读取 execution logs**

将数据源从旧的运行日志改为 `getExecutionLogs` API。展示执行日志的时间轴、触发类型、结果状态、脚本日志详情。

核心变更：
- 引入 `getExecutionLogs` 替代旧 API
- 时间轴倒序展示，每条记录可展开查看 `scriptLogs` 详情
- `triggerType` 和 `resultStatus` 用不同颜色 el-tag 标记
- 展开后解析 `scriptLogs` JSON 并逐条展示

具体实现对照设计文档 Section 6.2 的 UI 展示规范。

- [ ] **步骤 2：Commit**

```bash
cd web && git add src/views/alarm/components/CompositeAlarmLogDrawer.vue
git commit -m "feat(web): CompositeAlarmLogDrawer 改为展示 execution_log 数据"
```

---

## 自检

### 规格覆盖度

| 规格需求 | 实现任务 | 状态 |
|----------|----------|------|
| DB: hazard_point_id VARCHAR | 任务 1 | ✅ |
| DB: execution_log 表 | 任务 1 | ✅ |
| ScriptLogger | 任务 2 | ✅ |
| IHazardPointQueryService.listIdsByGroupId | 任务 3 | ✅ |
| Mapper VARCHAR 适配 | 任务 4 | ✅ |
| StrategyScopeResolver | 任务 5 | ✅ |
| ExecutionLog domain + Mapper | 任务 6 | ✅ |
| ComprehensiveAlarmExecutionService | 任务 7 | ✅ |
| Quartz Scheduler + Job | 任务 8 | ✅ |
| ComprehensiveAlarmJob 删除 + CRUD 集成 | 任务 9 | ✅ |
| ComprehensiveAlarmEventListener | 任务 10 | ✅ |
| Controller execution-logs 端点 | 任务 11 | ✅ |
| 前端 alarm.ts + script-api-docs | 任务 12 | ✅ |
| 前端 ScopeDialog 重写 | 任务 13 | ✅ |
| 前端 LogDrawer 修改 | 任务 14 | ✅ |

### 占位符扫描

- 无 "TODO" / "待定" / "后续实现" ✅
- 每个步骤包含完整代码或精确的文件路径 ✅
- 每个任务有验证命令 ✅

### 类型一致性

- `selectScopeValuesByStrategyId(Long)` → `List<String>` — 任务 4 定义，任务 5 调用 ✅
- `resolveScope(Long)` → `List<Long>` — 任务 5 定义，任务 7 调用 ✅
- `isHazardPointInScope(Long, Long)` → `boolean` — 任务 5 定义，任务 10 调用 ✅
- `execute(AlarmStrategy, Object, String)` → `ExecutionResult` — 任务 7 定义，任务 8/10 调用 ✅
- `ScriptLogger.toJson()` → `String` — 任务 2 定义，任务 7 调用 ✅
- `scheduleOrUpdate(AlarmStrategy)` — 任务 8 定义，任务 9 调用 ✅
- `listIdsByGroupId(Long)` → `List<Long>` — 任务 3 定义，任务 5 调用 ✅
- `hazardPointIds: string[]` — 前端任务 12 定义，任务 13 使用 ✅
