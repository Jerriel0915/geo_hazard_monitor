# zwei-iot-parser 模块实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现策略驱动的 Groovy 脚本数据解析引擎，按 MQTT topic 路由解析，输出统一 StandardMeasurementPoint 到 timeseries 链路。

**Architecture:** zwei-iot-parser 模块独立于 timeseries，通过 ParseEngine 接口对外暴露。内部串联 topic 解析→策略匹配→Groovy 脚本执行→结果校验→标准化输出。MonitorIngestFacade 薄化为仅调用 parseEngine.parse() + 入队。

**Tech Stack:** Java 17, Spring Boot 4.0.3, MyBatis, Groovy (org.apache.groovy), Caffeine, fastjson2, Lombok

---

### Task 1: 下沉 StandardMeasurementPoint 到 zwei-common

**Files:**
- Create: `server/zwei-common/src/main/java/com/zwei/common/domain/StandardMeasurementPoint.java`
- Modify: `server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/domain/StandardMeasurementPoint.java` → **删除**
- Modify: 所有 import `com.zwei.iot.timeseries.domain.StandardMeasurementPoint` → `com.zwei.common.domain.StandardMeasurementPoint`

- [ ] **Step 1: 在 zwei-common 中创建 StandardMeasurementPoint**

```java
package com.zwei.common.domain;

import lombok.Builder;

import java.io.Serial;
import java.io.Serializable;

/**
 * 统一标准化时序点。系统内流通的元数据格式。
 *
 * <p>由 parser 模块的 Groovy 脚本解析输出，经 timeseries 模块写入 Redis Stream 并最终持久化至 IoTDB。</p>
 */
@Builder
public record StandardMeasurementPoint(
        Long deviceId,
        String sensorNo,
        Long sensorId,
        String attrCode,
        String attrName,
        String unit,
        long dataTime,
        Double value,
        Integer quality,
        long reportTime,
        long receiveTime,
        String sourceType,
        String payloadHash
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
```

- [ ] **Step 2: 删除 timeseries 中的旧文件**

```bash
rm server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/domain/StandardMeasurementPoint.java
```

- [ ] **Step 3: 更新所有 import**

涉及的 Java 文件（旧 import `com.zwei.iot.timeseries.domain.StandardMeasurementPoint` → 新 import `com.zwei.common.domain.StandardMeasurementPoint`）：

| 文件 |
|------|
| `server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/parser/SysMonitorPayloadParser.java` |
| `server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/parser/MonitorPayloadParser.java` |
| `server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/service/MonitorIngestFacade.java` |
| `server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/service/MonitorIngestStreamService.java` |
| `server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/service/MonitorIngestConsumerService.java` |
| `server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/service/IotdbTimeSeriesService.java` |

每个文件将 import 行改为 `import com.zwei.common.domain.StandardMeasurementPoint;`。

- [ ] **Step 4: 编译验证**

Run: `cd server && mvn clean compile -pl zwei-common,zwei-iot-timeseries,zwei-iot-alarm -am`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add server/zwei-common/src/main/java/com/zwei/common/domain/StandardMeasurementPoint.java
git add -u server/zwei-iot-timeseries/
git commit -m "refactor: 下沉 StandardMeasurementPoint 到 zwei-common

parser/timeseries/alarm 模块共用统一元数据格式"
```

---

### Task 2: 数据库建表

**Files:**
- Create: `db/upgrade/v3.9.3-parser-module.sql`

- [ ] **Step 1: 创建 DDL 脚本**

```sql
-- zwei-iot-parser 模块初始化
-- 数据库升级脚本 v3.9.3

CREATE TABLE IF NOT EXISTS `iot_data_parse_strategy` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(100) NOT NULL COMMENT '策略名称',
  `topic_pattern` varchar(200) NOT NULL COMMENT '订阅主题（支持通配符 *，仅末尾）',
  `script_code` mediumtext NOT NULL COMMENT 'Groovy 解析脚本',
  `description` text COMMENT '描述',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '0-停用 1-启用',
  `is_preset` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否预置 0-否 1-是',
  `priority` int NOT NULL DEFAULT '0' COMMENT '匹配优先级（值越大越优先）',
  `last_run_time` datetime DEFAULT NULL COMMENT '最近运行时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` varchar(64) DEFAULT NULL,
  `update_by` varchar(64) DEFAULT NULL,
  `deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_topic_pattern` (`topic_pattern`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据解析策略表';

CREATE TABLE IF NOT EXISTS `iot_data_parse_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `strategy_id` bigint NOT NULL COMMENT '策略ID',
  `message` text NOT NULL COMMENT '错误消息',
  `topic` varchar(200) DEFAULT NULL,
  `device_code` varchar(100) DEFAULT NULL,
  `sensor_no` varchar(100) DEFAULT NULL,
  `execution_time` int DEFAULT NULL COMMENT '执行耗时(毫秒)',
  `error_stack` text COMMENT '错误堆栈',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_strategy_id` (`strategy_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='解析运行日志表（仅ERROR）';
```

- [ ] **Step 2: Commit**

```bash
mkdir -p db/upgrade
git add db/upgrade/v3.9.3-parser-module.sql
git commit -m "feat: 新增 parser 模块数据库表

iot_data_parse_strategy + iot_data_parse_log"
```

---

### Task 3: 注册 parser 模块到 Maven 构建

**Files:**
- Modify: `server/pom.xml`
- Modify: `server/zwei-admin/pom.xml`
- Modify: `server/zwei-iot-parser/pom.xml`

- [ ] **Step 1: 父 POM 添加 module 和 dependencyManagement**

在 `server/pom.xml` 的 `<modules>` 中添加：

```xml
<module>zwei-iot-parser</module>
```

在 `<dependencyManagement>` 的 `<dependencies>` 中，IOT 模块区添加：

```xml
<!-- IOT数据解析模块 -->
<dependency>
    <groupId>com.zwei</groupId>
    <artifactId>zwei-iot-parser</artifactId>
    <version>${zwei.version}</version>
</dependency>
```

- [ ] **Step 2: zwei-admin 添加依赖**

在 `server/zwei-admin/pom.xml` 的 IOT 模块区添加：

```xml
<dependency><groupId>com.zwei</groupId><artifactId>zwei-iot-parser</artifactId></dependency>
```

- [ ] **Step 3: 更新 parser pom.xml（添加完整依赖）**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <artifactId>zwei</artifactId>
        <groupId>com.zwei</groupId>
        <version>3.9.2</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>
    <artifactId>zwei-iot-parser</artifactId>
    <description>IoT 数据解析模块 — 策略驱动 Groovy 脚本解析 + 主题路由 + 标准化输出</description>
    <dependencies>
        <dependency>
            <groupId>com.zwei</groupId>
            <artifactId>zwei-common</artifactId>
        </dependency>
        <dependency>
            <groupId>com.zwei</groupId>
            <artifactId>zwei-iot-device</artifactId>
        </dependency>
        <dependency>
            <groupId>org.apache.groovy</groupId>
            <artifactId>groovy</artifactId>
        </dependency>
        <dependency>
            <groupId>com.github.ben-manes.caffeine</groupId>
            <artifactId>caffeine</artifactId>
        </dependency>
        <dependency>
            <groupId>com.alibaba.fastjson2</groupId>
            <artifactId>fastjson2</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 4: 编译验证**

Run: `cd server && mvn clean compile -pl zwei-iot-parser -am`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add server/pom.xml server/zwei-admin/pom.xml server/zwei-iot-parser/pom.xml
git commit -m "feat: 注册 zwei-iot-parser 模块到 Maven 构建

添加 module + dependencyManagement + zwei-admin 依赖"
```

---

### Task 4: ParseResultItem — 脚本返回强类型

**Files:**
- Create: `server/zwei-iot-parser/src/main/java/com/zwei/iot/parser/domain/ParseResultItem.java`
- Delete: `server/zwei-iot-parser/src/main/java/com/zwei/iot/parser/metadata/SensorMetadataView.java`
- Delete: `server/zwei-iot-parser/src/main/java/com/zwei/iot/parser/metadata/SensorAttributeView.java`

- [ ] **Step 1: 创建 ParseResultItem**

```java
package com.zwei.iot.parser.domain;

/**
 * 脚本解析结果项。Groovy 脚本的 parse() 方法返回此类型的 List。
 * 各字段由 ResultValidator 校验后，映射为 StandardMeasurementPoint。
 */
public class ParseResultItem {

    /** 传感器编号（必填） */
    private String sensorNo;

    /** 属性代码（必填，需匹配传感器已注册的 attrCode） */
    private String attrCode;

    /** 测量值（必填） */
    private Double value;

    /** 数据时间戳（毫秒），默认当前时间 */
    private Long dataTime;

    /** 质量码，0=正常，默认 0 */
    private Integer quality;

    public ParseResultItem() {}

    public ParseResultItem(String sensorNo, String attrCode, Double value,
                           Long dataTime, Integer quality) {
        this.sensorNo = sensorNo;
        this.attrCode = attrCode;
        this.value = value;
        this.dataTime = dataTime != null ? dataTime : System.currentTimeMillis();
        this.quality = quality != null ? quality : 0;
    }

    public String getSensorNo() { return sensorNo; }
    public void setSensorNo(String sensorNo) { this.sensorNo = sensorNo; }
    public String getAttrCode() { return attrCode; }
    public void setAttrCode(String attrCode) { this.attrCode = attrCode; }
    public Double getValue() { return value; }
    public void setValue(Double value) { this.value = value; }
    public Long getDataTime() { return dataTime; }
    public void setDataTime(Long dataTime) { this.dataTime = dataTime; }
    public Integer getQuality() { return quality; }
    public void setQuality(Integer quality) { this.quality = quality; }
}
```

- [ ] **Step 2: 删除旧的两个 metadata view 接口**

```bash
rm server/zwei-iot-parser/src/main/java/com/zwei/iot/parser/metadata/SensorMetadataView.java
rm server/zwei-iot-parser/src/main/java/com/zwei/iot/parser/metadata/SensorAttributeView.java
```

- [ ] **Step 3: Commit**

```bash
git add server/zwei-iot-parser/src/main/java/
git add -u server/zwei-iot-parser/
git commit -m "feat: ParseResultItem — Groovy 脚本返回强类型

替换旧的 metadata view 接口，脚本直接返回 List<ParseResultItem>"
```

---

### Task 5: Entity 类 — DataParseStrategy + DataParseLog

**Files:**
- Create: `server/zwei-iot-parser/src/main/java/com/zwei/iot/parser/strategy/domain/DataParseStrategy.java`
- Create: `server/zwei-iot-parser/src/main/java/com/zwei/iot/parser/strategy/domain/DataParseLog.java`

- [ ] **Step 1: 创建 DataParseStrategy**

```java
package com.zwei.iot.parser.strategy.domain;

import java.time.LocalDateTime;

/**
 * 数据解析策略实体。
 */
public class DataParseStrategy {
    private Long id;
    private String name;
    private String topicPattern;
    private String scriptCode;
    private String description;
    private Integer status;       // 0-停用 1-启用
    private Integer isPreset;     // 0-否 1-是（预置不可删）
    private Integer priority;     // 匹配优先级
    private LocalDateTime lastRunTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String createBy;
    private String updateBy;
    private Integer deleted;      // 0-否 1-是

    // getters/setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getTopicPattern() { return topicPattern; }
    public void setTopicPattern(String topicPattern) { this.topicPattern = topicPattern; }
    public String getScriptCode() { return scriptCode; }
    public void setScriptCode(String scriptCode) { this.scriptCode = scriptCode; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Integer getIsPreset() { return isPreset; }
    public void setIsPreset(Integer isPreset) { this.isPreset = isPreset; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public LocalDateTime getLastRunTime() { return lastRunTime; }
    public void setLastRunTime(LocalDateTime lastRunTime) { this.lastRunTime = lastRunTime; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }
    public String getUpdateBy() { return updateBy; }
    public void setUpdateBy(String updateBy) { this.updateBy = updateBy; }
    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
```

- [ ] **Step 2: 创建 DataParseLog**

```java
package com.zwei.iot.parser.strategy.domain;

import java.time.LocalDateTime;

/**
 * 解析运行日志实体（仅记录 ERROR）。
 */
public class DataParseLog {
    private Long id;
    private Long strategyId;
    private String message;
    private String topic;
    private String deviceCode;
    private String sensorNo;
    private Integer executionTime;
    private String errorStack;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getStrategyId() { return strategyId; }
    public void setStrategyId(Long strategyId) { this.strategyId = strategyId; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    public String getDeviceCode() { return deviceCode; }
    public void setDeviceCode(String deviceCode) { this.deviceCode = deviceCode; }
    public String getSensorNo() { return sensorNo; }
    public void setSensorNo(String sensorNo) { this.sensorNo = sensorNo; }
    public Integer getExecutionTime() { return executionTime; }
    public void setExecutionTime(Integer executionTime) { this.executionTime = executionTime; }
    public String getErrorStack() { return errorStack; }
    public void setErrorStack(String errorStack) { this.errorStack = errorStack; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
```

- [ ] **Step 3: Commit**

```bash
git add server/zwei-iot-parser/src/main/java/com/zwei/iot/parser/strategy/domain/
git commit -m "feat: 解析策略 + 日志实体类"
```

---

### Task 6: Mapper 接口

**Files:**
- Create: `server/zwei-iot-parser/src/main/java/com/zwei/iot/parser/strategy/mapper/DataParseStrategyMapper.java`
- Create: `server/zwei-iot-parser/src/main/java/com/zwei/iot/parser/strategy/mapper/DataParseLogMapper.java`

- [ ] **Step 1: 创建 DataParseStrategyMapper**

```java
package com.zwei.iot.parser.strategy.mapper;

import com.zwei.iot.parser.strategy.domain.DataParseStrategy;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface DataParseStrategyMapper {

    @Select("SELECT * FROM iot_data_parse_strategy WHERE deleted = 0 ORDER BY priority DESC, id ASC")
    List<DataParseStrategy> selectAllEnabled();

    @Select("SELECT * FROM iot_data_parse_strategy WHERE id = #{id} AND deleted = 0")
    DataParseStrategy selectById(Long id);

    @Select("SELECT * FROM iot_data_parse_strategy WHERE topic_pattern = #{topicPattern} AND deleted = 0")
    List<DataParseStrategy> selectByTopicPattern(String topicPattern);

    @Insert("INSERT INTO iot_data_parse_strategy (name, topic_pattern, script_code, description, status, is_preset, priority, create_time, update_time, create_by, update_by) " +
            "VALUES (#{name}, #{topicPattern}, #{scriptCode}, #{description}, #{status}, #{isPreset}, #{priority}, NOW(), NOW(), #{createBy}, #{updateBy})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(DataParseStrategy strategy);

    @Update("UPDATE iot_data_parse_strategy SET name=#{name}, topic_pattern=#{topicPattern}, script_code=#{scriptCode}, description=#{description}, status=#{status}, priority=#{priority}, update_time=NOW(), update_by=#{updateBy} WHERE id=#{id}")
    int update(DataParseStrategy strategy);

    @Update("UPDATE iot_data_parse_strategy SET status=#{status}, update_time=NOW() WHERE id=#{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    @Update("UPDATE iot_data_parse_strategy SET last_run_time=NOW() WHERE id=#{id}")
    int updateLastRunTime(Long id);

    @Update("UPDATE iot_data_parse_strategy SET deleted=1, update_time=NOW() WHERE id=#{id}")
    int deleteById(Long id);
}
```

- [ ] **Step 2: 创建 DataParseLogMapper**

```java
package com.zwei.iot.parser.strategy.mapper;

import com.zwei.iot.parser.strategy.domain.DataParseLog;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface DataParseLogMapper {

    @Insert("INSERT INTO iot_data_parse_log (strategy_id, message, topic, device_code, sensor_no, execution_time, error_stack, create_time) " +
            "VALUES (#{strategyId}, #{message}, #{topic}, #{deviceCode}, #{sensorNo}, #{executionTime}, #{errorStack}, NOW())")
    int insert(DataParseLog log);

    @Select("SELECT * FROM iot_data_parse_log WHERE strategy_id = #{strategyId} ORDER BY create_time DESC")
    List<DataParseLog> selectByStrategyId(Long strategyId);

    @Delete("DELETE FROM iot_data_parse_log WHERE strategy_id = #{strategyId}")
    int deleteByStrategyId(Long strategyId);
}
```

- [ ] **Step 3: Commit**

```bash
git add server/zwei-iot-parser/src/main/java/com/zwei/iot/parser/strategy/mapper/
git commit -m "feat: 解析策略 + 日志 Mapper 接口（注解式 SQL）"
```

---

### Task 7: ParseEngine 接口

**Files:**
- Create: `server/zwei-iot-parser/src/main/java/com/zwei/iot/parser/engine/ParseEngine.java`

- [ ] **Step 1: 创建 ParseEngine**

```java
package com.zwei.iot.parser.engine;

import com.zwei.common.domain.StandardMeasurementPoint;
import com.zwei.common.exception.ServiceException;

import java.util.List;

/**
 * 数据解析引擎入口。timeseries 模块通过此接口替代原有 MonitorPayloadParser SPI。
 */
public interface ParseEngine {

    /**
     * 解析原始 MQTT 报文为标准化监测数据点。
     *
     * @param topic    MQTT 主题（如 sys/v1/DEV001/SN01/updata）
     * @param message  原始报文字节
     * @param deviceId 已认证设备 ID（由 broker 层传入）
     * @return 标准化数据点列表；无匹配策略时返回空列表
     * @throws ServiceException 解析失败时抛出
     */
    List<StandardMeasurementPoint> parse(String topic, byte[] message, Long deviceId);
}
```

- [ ] **Step 2: Commit**

```bash
git add server/zwei-iot-parser/src/main/java/com/zwei/iot/parser/engine/ParseEngine.java
git commit -m "feat: ParseEngine 入口接口"
```

---

### Task 8: StrategyMatcher — 策略匹配器

**Files:**
- Create: `server/zwei-iot-parser/src/main/java/com/zwei/iot/parser/engine/StrategyMatcher.java`

- [ ] **Step 1: 创建 StrategyMatcher**

```java
package com.zwei.iot.parser.engine;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.zwei.iot.parser.strategy.domain.DataParseStrategy;
import com.zwei.iot.parser.strategy.mapper.DataParseStrategyMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 策略匹配器 — 按 topic 匹配启用的解析策略。
 *
 * <p>使用 Caffeine 缓存全量启用策略列表，TTL=60s。topicPattern 中的 * 替换为 SQL LIKE 的 % 进行模糊匹配。
 */
@Component
public class StrategyMatcher {

    private static final Logger log = LoggerFactory.getLogger(StrategyMatcher.class);

    private final DataParseStrategyMapper strategyMapper;
    private volatile List<DataParseStrategy> cache;
    private volatile long lastRefreshTime;

    public StrategyMatcher(DataParseStrategyMapper strategyMapper) {
        this.strategyMapper = strategyMapper;
    }

    @PostConstruct
    void init() {
        refreshCache();
    }

    /**
     * 按 topic 匹配策略。
     *
     * @param topic MQTT 主题（已通过格式校验）
     * @return 匹配到的启用策略；无匹配返回 null
     */
    public DataParseStrategy match(String topic) {
        List<DataParseStrategy> strategies = getCachedStrategies();
        for (DataParseStrategy s : strategies) {
            String likePattern = s.getTopicPattern().replace("*", "%");
            if (likePattern.equals(topic) || matchesLike(topic, likePattern)) {
                return s;
            }
        }
        return null;
    }

    /**
     * 主动刷新缓存（策略 CRUD 后调用）。
     */
    public void evict() {
        refreshCache();
    }

    private List<DataParseStrategy> getCachedStrategies() {
        if (cache == null || System.currentTimeMillis() - lastRefreshTime > 60_000) {
            refreshCache();
        }
        return cache;
    }

    private synchronized void refreshCache() {
        if (cache != null && System.currentTimeMillis() - lastRefreshTime <= 60_000) {
            return;
        }
        cache = strategyMapper.selectAllEnabled();
        lastRefreshTime = System.currentTimeMillis();
        log.debug("策略缓存已刷新, count={}", cache.size());
    }

    private boolean matchesLike(String topic, String likePattern) {
        // 将 Like 通配符转为正则
        String regex = likePattern
                .replaceAll("%", ".*")
                .replaceAll("_", ".");
        return topic.matches(regex);
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add server/zwei-iot-parser/src/main/java/com/zwei/iot/parser/engine/StrategyMatcher.java
git commit -m "feat: StrategyMatcher — 按 topic LIKE 匹配策略（Caffeine 缓存）"
```

---

### Task 9: GroovyScriptExecutor — 脚本执行器

**Files:**
- Create: `server/zwei-iot-parser/src/main/java/com/zwei/iot/parser/engine/GroovyScriptExecutor.java`

- [ ] **Step 1: 创建 GroovyScriptExecutor**

参考告警模块 `GroovyScriptExecutor` 实现，适配解析场景：

```java
package com.zwei.iot.parser.engine;

import com.zwei.iot.parser.domain.ParseResultItem;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Groovy 脚本执行器。
 *
 * <p>编译缓存键 = MD5(scriptCode)。单个脚本多次执行复用同一个 GroovyObject 实例。
 * 执行超时 30 秒，由 Future.get(timeout) 控制。
 */
@Component
public class GroovyScriptExecutor {

    private static final Logger log = LoggerFactory.getLogger(GroovyScriptExecutor.class);
    private static final int EXECUTION_TIMEOUT_SECONDS = 30;

    private final ConcurrentHashMap<String, GroovyObject> compileCache = new ConcurrentHashMap<>();
    private final ExecutorService executor =
            Executors.newSingleThreadExecutor(r -> new Thread(r, "parser-groovy"));

    /**
     * 执行解析脚本。
     *
     * @param scriptCode Groovy 脚本源码
     * @param context    脚本上下文（topic/payload/deviceId/deviceCode/sensorNo/attributes/log）
     * @return 解析结果列表
     * @throws ParseScriptException 编译或执行失败
     */
    @SuppressWarnings("unchecked")
    public List<ParseResultItem> execute(String scriptCode, Map<String, Object> context)
            throws ParseScriptException {
        Future<List<ParseResultItem>> future = executor.submit(() -> {
            GroovyObject obj = compileOrGet(scriptCode);
            return (List<ParseResultItem>) obj.invokeMethod("parse", new Object[]{context});
        });

        try {
            return future.get(EXECUTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new ParseScriptException("脚本执行超时 (" + EXECUTION_TIMEOUT_SECONDS + "s)");
        } catch (ExecutionException e) {
            throw new ParseScriptException("脚本执行异常: " + e.getCause().getMessage(), e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ParseScriptException("脚本执行被中断", e);
        }
    }

    /**
     * 预编译脚本（不执行），用于保存/启用时的语法校验。
     */
    public void validateSyntax(String scriptCode) throws ParseScriptException {
        try {
            CompilerConfiguration config = new CompilerConfiguration();
            config.setTargetBytecode(17);
            GroovyClassLoader classLoader = new GroovyClassLoader(
                    Thread.currentThread().getContextClassLoader(), config);
            classLoader.parseClass(scriptCode);
            classLoader.close();
        } catch (Exception e) {
            throw new ParseScriptException("脚本编译失败: " + e.getMessage(), e);
        }
    }

    private GroovyObject compileOrGet(String scriptCode) throws ParseScriptException {
        String key = md5(scriptCode);
        return compileCache.computeIfAbsent(key, k -> {
            try {
                CompilerConfiguration config = new CompilerConfiguration();
                config.setTargetBytecode(17);
                GroovyClassLoader classLoader = new GroovyClassLoader(
                        Thread.currentThread().getContextClassLoader(), config);
                Class<?> clazz = classLoader.parseClass(scriptCode);
                return (GroovyObject) clazz.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException(new ParseScriptException("脚本编译失败: " + e.getMessage(), e));
            }
        });
    }

    public void clearCache() {
        compileCache.clear();
    }

    private static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return HexFormat.of().formatHex(md.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /** 脚本执行异常 */
    public static class ParseScriptException extends Exception {
        public ParseScriptException(String message) { super(message); }
        public ParseScriptException(String message, Throwable cause) { super(message, cause); }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add server/zwei-iot-parser/src/main/java/com/zwei/iot/parser/engine/GroovyScriptExecutor.java
git commit -m "feat: GroovyScriptExecutor — MD5 编译缓存 + 30s 超时 + 语法预检"
```

---

### Task 10: ParseEngineImpl — 核心引擎实现

**Files:**
- Create: `server/zwei-iot-parser/src/main/java/com/zwei/iot/parser/engine/ParseEngineImpl.java`

- [ ] **Step 1: 创建 ParseEngineImpl**

```java
package com.zwei.iot.parser.engine;

import com.alibaba.fastjson2.JSON;
import com.zwei.common.domain.StandardMeasurementPoint;
import com.zwei.common.exception.ServiceException;
import com.zwei.iot.device.domain.SensorAttribute;
import com.zwei.iot.device.domain.SensorMetadata;
import com.zwei.iot.device.service.IDeviceSensorQueryService;
import com.zwei.iot.parser.domain.ParseResultItem;
import com.zwei.iot.parser.strategy.domain.DataParseLog;
import com.zwei.iot.parser.strategy.domain.DataParseStrategy;
import com.zwei.iot.parser.strategy.mapper.DataParseLogMapper;
import com.zwei.iot.parser.strategy.mapper.DataParseStrategyMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ParseEngineImpl implements ParseEngine {

    private static final Logger log = LoggerFactory.getLogger(ParseEngineImpl.class);
    private static final Pattern TOPIC_PATTERN =
            Pattern.compile("^(sys|gb)/v1/(?<deviceCode>[A-Za-z0-9_-]{1,64})/(?<sensorNo>[A-Za-z0-9_-]{1,64})/updata$");

    private final StrategyMatcher strategyMatcher;
    private final GroovyScriptExecutor scriptExecutor;
    private final IDeviceSensorQueryService sensorQueryService;
    private final DataParseStrategyMapper strategyMapper;
    private final DataParseLogMapper logMapper;

    public ParseEngineImpl(StrategyMatcher strategyMatcher,
                           GroovyScriptExecutor scriptExecutor,
                           IDeviceSensorQueryService sensorQueryService,
                           DataParseStrategyMapper strategyMapper,
                           DataParseLogMapper logMapper) {
        this.strategyMatcher = strategyMatcher;
        this.scriptExecutor = scriptExecutor;
        this.sensorQueryService = sensorQueryService;
        this.strategyMapper = strategyMapper;
        this.logMapper = logMapper;
    }

    @Override
    public List<StandardMeasurementPoint> parse(String topic, byte[] message, Long deviceId) {
        // 1. Topic 解析
        Matcher m = TOPIC_PATTERN.matcher(topic == null ? "" : topic);
        if (!m.matches()) {
            return Collections.emptyList();
        }
        String sourceType = m.group(1);
        String deviceCode = m.group("deviceCode");
        String sensorNo = m.group("sensorNo");

        // 2. 策略匹配
        DataParseStrategy strategy = strategyMatcher.match(topic);
        if (strategy == null) {
            return Collections.emptyList();
        }

        long startTime = System.currentTimeMillis();

        try {
            // 3. 传感器元数据
            SensorMetadata metadata = sensorQueryService.requireSensorMetadata(deviceId, sensorNo);

            // 4. 构建脚本上下文
            Map<String, Object> ctx = buildContext(topic, message, deviceId, deviceCode, sensorNo, metadata);

            // 5. 执行脚本
            List<ParseResultItem> items = scriptExecutor.execute(strategy.getScriptCode(), ctx);

            // 6. 校验 + 映射
            List<StandardMeasurementPoint> points = validateAndMap(items, metadata, sourceType, message, Instant.now().toEpochMilli());

            // 7. 更新 lastRunTime
            strategyMapper.updateLastRunTime(strategy.getId());

            return points;

        } catch (GroovyScriptExecutor.ParseScriptException e) {
            logError(strategy, topic, deviceCode, sensorNo, startTime, e);
            throw new ServiceException("解析脚本执行失败: " + e.getMessage());
        } catch (ServiceException e) {
            logError(strategy, topic, deviceCode, sensorNo, startTime, e);
            throw e;
        } catch (Exception e) {
            logError(strategy, topic, deviceCode, sensorNo, startTime, e);
            throw new ServiceException("解析引擎未知异常: " + e.getMessage());
        }
    }

    private Map<String, Object> buildContext(String topic, byte[] message, Long deviceId,
                                              String deviceCode, String sensorNo,
                                              SensorMetadata metadata) {
        Map<String, Object> ctx = new LinkedHashMap<>();
        ctx.put("topic", topic);
        ctx.put("payload", new String(message, StandardCharsets.UTF_8));
        ctx.put("deviceId", deviceId);
        ctx.put("deviceCode", deviceCode);
        ctx.put("sensorNo", sensorNo);
        ctx.put("attributes", metadata.attributes());
        return ctx;
    }

    private List<StandardMeasurementPoint> validateAndMap(List<ParseResultItem> items,
                                                           SensorMetadata metadata,
                                                           String sourceType,
                                                           byte[] rawMessage,
                                                           long receiveTime) {
        if (items == null || items.isEmpty()) {
            throw new ServiceException("脚本未返回有效解析结果");
        }

        List<StandardMeasurementPoint> points = new ArrayList<>();
        String payloadHash = buildHash(rawMessage);

        for (ParseResultItem item : items) {
            if (item.getSensorNo() == null || item.getSensorNo().isBlank()) {
                throw new ServiceException("解析结果缺少 sensorNo");
            }
            if (item.getAttrCode() == null || item.getAttrCode().isBlank()) {
                throw new ServiceException("解析结果缺少 attrCode");
            }
            if (item.getValue() == null) {
                throw new ServiceException("解析结果缺少 value (attrCode=" + item.getAttrCode() + ")");
            }

            long dataTime = item.getDataTime() != null ? item.getDataTime() : receiveTime;
            int quality = item.getQuality() != null ? item.getQuality() : 0;

            // 从元数据查找 attrName 和 unit
            String attrName = item.getAttrCode();
            String unit = null;
            if (metadata.attributes() != null) {
                for (SensorAttribute attr : metadata.attributes()) {
                    if (Objects.equals(attr.getAttrCode(), item.getAttrCode())) {
                        attrName = attr.getAttrName() != null ? attr.getAttrName() : attrName;
                        unit = attr.getUnit();
                        break;
                    }
                }
            }

            points.add(StandardMeasurementPoint.builder()
                    .deviceId(metadata.deviceId())
                    .sensorNo(item.getSensorNo())
                    .sensorId(metadata.sensorId())
                    .attrCode(item.getAttrCode())
                    .attrName(attrName)
                    .unit(unit)
                    .dataTime(dataTime)
                    .value(item.getValue())
                    .quality(quality)
                    .reportTime(dataTime)
                    .receiveTime(receiveTime)
                    .sourceType(sourceType)
                    .payloadHash(payloadHash)
                    .build());
        }
        return points;
    }

    private void logError(DataParseStrategy strategy, String topic, String deviceCode,
                          String sensorNo, long startTime, Exception e) {
        DataParseLog logEntry = new DataParseLog();
        logEntry.setStrategyId(strategy.getId());
        logEntry.setMessage(e.getMessage());
        logEntry.setTopic(topic);
        logEntry.setDeviceCode(deviceCode);
        logEntry.setSensorNo(sensorNo);
        logEntry.setExecutionTime((int) (System.currentTimeMillis() - startTime));
        logEntry.setErrorStack(stackTraceToString(e));
        try {
            logMapper.insert(logEntry);
        } catch (Exception ex) {
            log.error("写入解析错误日志失败", ex);
        }
    }

    private String stackTraceToString(Throwable t) {
        StringBuilder sb = new StringBuilder();
        sb.append(t.toString()).append("\n");
        for (StackTraceElement ste : t.getStackTrace()) {
            sb.append("    at ").append(ste.toString()).append("\n");
        }
        return sb.toString();
    }

    private String buildHash(byte[] payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(payload));
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add server/zwei-iot-parser/src/main/java/com/zwei/iot/parser/engine/ParseEngineImpl.java
git commit -m "feat: ParseEngineImpl — 策略匹配→脚本执行→校验映射全链路"
```

---

### Task 11: DataParseLogService — 运行日志服务

**Files:**
- Create: `server/zwei-iot-parser/src/main/java/com/zwei/iot/parser/strategy/service/DataParseLogService.java`

- [ ] **Step 1: 创建 DataParseLogService**

```java
package com.zwei.iot.parser.strategy.service;

import com.zwei.iot.parser.strategy.domain.DataParseLog;
import com.zwei.iot.parser.strategy.mapper.DataParseLogMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DataParseLogService {

    private final DataParseLogMapper logMapper;

    public DataParseLogService(DataParseLogMapper logMapper) {
        this.logMapper = logMapper;
    }

    public List<DataParseLog> listByStrategyId(Long strategyId) {
        return logMapper.selectByStrategyId(strategyId);
    }

    public void clearByStrategyId(Long strategyId) {
        logMapper.deleteByStrategyId(strategyId);
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add server/zwei-iot-parser/src/main/java/com/zwei/iot/parser/strategy/service/DataParseLogService.java
git commit -m "feat: DataParseLogService — 解析日志查询与清空"
```

---

### Task 12: DataParseStrategyService — 策略 CRUD

**Files:**
- Create: `server/zwei-iot-parser/src/main/java/com/zwei/iot/parser/strategy/service/DataParseStrategyService.java`

- [ ] **Step 1: 创建 DataParseStrategyService**

```java
package com.zwei.iot.parser.strategy.service;

import com.zwei.common.exception.ServiceException;
import com.zwei.iot.parser.dto.DataParseStrategyDTO;
import com.zwei.iot.parser.engine.GroovyScriptExecutor;
import com.zwei.iot.parser.engine.StrategyMatcher;
import com.zwei.iot.parser.strategy.domain.DataParseStrategy;
import com.zwei.iot.parser.strategy.mapper.DataParseStrategyMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DataParseStrategyService {

    private static final Logger log = LoggerFactory.getLogger(DataParseStrategyService.class);

    private final DataParseStrategyMapper strategyMapper;
    private final StrategyMatcher strategyMatcher;
    private final GroovyScriptExecutor scriptExecutor;

    public DataParseStrategyService(DataParseStrategyMapper strategyMapper,
                                     StrategyMatcher strategyMatcher,
                                     GroovyScriptExecutor scriptExecutor) {
        this.strategyMapper = strategyMapper;
        this.strategyMatcher = strategyMatcher;
        this.scriptExecutor = scriptExecutor;
    }

    public List<DataParseStrategy> list() {
        return strategyMapper.selectAllEnabled();
    }

    public DataParseStrategy getById(Long id) {
        DataParseStrategy s = strategyMapper.selectById(id);
        if (s == null) {
            throw new ServiceException("策略不存在");
        }
        return s;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(DataParseStrategyDTO dto) {
        validateSyntax(dto.getScriptCode());
        DataParseStrategy s = fromDTO(dto);
        strategyMapper.insert(s);
        strategyMatcher.evict();
        return s.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(DataParseStrategyDTO dto) {
        DataParseStrategy existing = getById(dto.getId());
        if (existing.getIsPreset() == 1 && dto.getScriptCode() != null
                && !dto.getScriptCode().equals(existing.getScriptCode())) {
            throw new ServiceException("预置策略不可修改脚本代码");
        }
        validateSyntax(dto.getScriptCode());
        DataParseStrategy s = fromDTO(dto);
        s.setId(dto.getId());
        strategyMapper.update(s);
        strategyMatcher.evict();
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        DataParseStrategy existing = getById(id);
        if (existing.getIsPreset() == 1) {
            throw new ServiceException("预置策略不可删除");
        }
        strategyMapper.deleteById(id);
        strategyMatcher.evict();
    }

    @Transactional(rollbackFor = Exception.class)
    public void toggleStatus(Long id, Integer status) {
        DataParseStrategy existing = getById(id);
        if (status == 1) {
            validateSyntax(existing.getScriptCode());
        }
        strategyMapper.updateStatus(id, status);
        strategyMatcher.evict();
    }

    private void validateSyntax(String scriptCode) {
        try {
            scriptExecutor.validateSyntax(scriptCode);
        } catch (GroovyScriptExecutor.ParseScriptException e) {
            throw new ServiceException("脚本语法错误: " + e.getMessage());
        }
    }

    private DataParseStrategy fromDTO(DataParseStrategyDTO dto) {
        DataParseStrategy s = new DataParseStrategy();
        s.setName(dto.getName());
        s.setTopicPattern(dto.getTopicPattern());
        s.setScriptCode(dto.getScriptCode());
        s.setDescription(dto.getDescription());
        s.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        s.setIsPreset(dto.getIsPreset() != null ? dto.getIsPreset() : 0);
        s.setPriority(dto.getPriority() != null ? dto.getPriority() : 0);
        s.setCreateBy(dto.getCreateBy());
        s.setUpdateBy(dto.getUpdateBy());
        return s;
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add server/zwei-iot-parser/src/main/java/com/zwei/iot/parser/strategy/service/DataParseStrategyService.java
git commit -m "feat: DataParseStrategyService — 策略 CRUD + 编译预检 + 缓存失效"
```

---

### Task 13: DTO 类

**Files:**
- Create: `server/zwei-iot-parser/src/main/java/com/zwei/iot/parser/dto/DataParseStrategyDTO.java`
- Create: `server/zwei-iot-parser/src/main/java/com/zwei/iot/parser/dto/DataParseTestRequest.java`
- Create: `server/zwei-iot-parser/src/main/java/com/zwei/iot/parser/dto/DataParseTestResponse.java`

- [ ] **Step 1: 创建 DataParseStrategyDTO**

```java
package com.zwei.iot.parser.dto;

public class DataParseStrategyDTO {
    private Long id;
    private String name;
    private String topicPattern;
    private String scriptCode;
    private String description;
    private Integer status;
    private Integer isPreset;
    private Integer priority;
    private String createBy;
    private String updateBy;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getTopicPattern() { return topicPattern; }
    public void setTopicPattern(String topicPattern) { this.topicPattern = topicPattern; }
    public String getScriptCode() { return scriptCode; }
    public void setScriptCode(String scriptCode) { this.scriptCode = scriptCode; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Integer getIsPreset() { return isPreset; }
    public void setIsPreset(Integer isPreset) { this.isPreset = isPreset; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }
    public String getUpdateBy() { return updateBy; }
    public void setUpdateBy(String updateBy) { this.updateBy = updateBy; }
}
```

- [ ] **Step 2: 创建 DataParseTestRequest**

```java
package com.zwei.iot.parser.dto;

public class DataParseTestRequest {
    private String topic;
    private Long deviceId;
    private String scriptCode;
    private String testPayload;

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    public Long getDeviceId() { return deviceId; }
    public void setDeviceId(Long deviceId) { this.deviceId = deviceId; }
    public String getScriptCode() { return scriptCode; }
    public void setScriptCode(String scriptCode) { this.scriptCode = scriptCode; }
    public String getTestPayload() { return testPayload; }
    public void setTestPayload(String testPayload) { this.testPayload = testPayload; }
}
```

- [ ] **Step 3: 创建 DataParseTestResponse**

```java
package com.zwei.iot.parser.dto;

import com.zwei.common.domain.StandardMeasurementPoint;

import java.util.List;

public class DataParseTestResponse {
    private boolean success;
    private long executionTime;
    private List<StandardMeasurementPoint> points;
    private List<TestLogEntry> logs;

    public static DataParseTestResponse ok(long executionTime, List<StandardMeasurementPoint> points) {
        DataParseTestResponse r = new DataParseTestResponse();
        r.success = true;
        r.executionTime = executionTime;
        r.points = points;
        return r;
    }

    public static DataParseTestResponse error(long executionTime, String errorMessage) {
        DataParseTestResponse r = new DataParseTestResponse();
        r.success = false;
        r.executionTime = executionTime;
        r.logs = List.of(new TestLogEntry("ERROR", errorMessage));
        return r;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public long getExecutionTime() { return executionTime; }
    public void setExecutionTime(long executionTime) { this.executionTime = executionTime; }
    public List<StandardMeasurementPoint> getPoints() { return points; }
    public void setPoints(List<StandardMeasurementPoint> points) { this.points = points; }
    public List<TestLogEntry> getLogs() { return logs; }
    public void setLogs(List<TestLogEntry> logs) { this.logs = logs; }

    public static class TestLogEntry {
        private String level;
        private String message;
        public TestLogEntry() {}
        public TestLogEntry(String level, String message) { this.level = level; this.message = message; }
        public String getLevel() { return level; }
        public void setLevel(String level) { this.level = level; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add server/zwei-iot-parser/src/main/java/com/zwei/iot/parser/dto/
git commit -m "feat: parser DTO — 策略请求/响应 + 测试请求/响应"
```

---

### Task 14: DataParseController — REST API

**Files:**
- Create: `server/zwei-iot-parser/src/main/java/com/zwei/iot/parser/controller/DataParseController.java`

- [ ] **Step 1: 创建 DataParseController**

测试接口直接调 `GroovyScriptExecutor` + 自行构建上下文，绕开 DB 策略匹配：

```java
package com.zwei.iot.parser.controller;

import com.zwei.common.core.domain.AjaxResult;
import com.zwei.common.core.page.TableDataInfo;
import com.zwei.common.domain.StandardMeasurementPoint;
import com.zwei.common.exception.ServiceException;
import com.zwei.iot.device.domain.SensorAttribute;
import com.zwei.iot.device.domain.SensorMetadata;
import com.zwei.iot.device.service.IDeviceSensorQueryService;
import com.zwei.iot.parser.domain.ParseResultItem;
import com.zwei.iot.parser.dto.DataParseStrategyDTO;
import com.zwei.iot.parser.dto.DataParseTestRequest;
import com.zwei.iot.parser.dto.DataParseTestResponse;
import com.zwei.iot.parser.engine.GroovyScriptExecutor;
import com.zwei.iot.parser.engine.ParseEngine;
import com.zwei.iot.parser.strategy.domain.DataParseLog;
import com.zwei.iot.parser.strategy.domain.DataParseStrategy;
import com.zwei.iot.parser.strategy.service.DataParseLogService;
import com.zwei.iot.parser.strategy.service.DataParseStrategyService;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1/iot/dataParse")
public class DataParseController {

    private static final Pattern TOPIC_PATTERN =
            Pattern.compile("^(sys|gb)/v1/(?<deviceCode>[A-Za-z0-9_-]{1,64})/(?<sensorNo>[A-Za-z0-9_-]{1,64})/updata$");

    private final DataParseStrategyService strategyService;
    private final DataParseLogService logService;
    private final GroovyScriptExecutor scriptExecutor;
    private final IDeviceSensorQueryService sensorQueryService;
    private final ParseEngine parseEngine;

    public DataParseController(DataParseStrategyService strategyService,
                                DataParseLogService logService,
                                GroovyScriptExecutor scriptExecutor,
                                IDeviceSensorQueryService sensorQueryService,
                                ParseEngine parseEngine) {
        this.strategyService = strategyService;
        this.logService = logService;
        this.scriptExecutor = scriptExecutor;
        this.sensorQueryService = sensorQueryService;
        this.parseEngine = parseEngine;
    }

    @GetMapping("/list")
    public TableDataInfo list() {
        List<DataParseStrategy> list = strategyService.list();
        return TableDataInfo.success(list, list.size());
    }

    @GetMapping("/{id}")
    public AjaxResult getDetail(@PathVariable Long id) {
        return AjaxResult.success(strategyService.getById(id));
    }

    @PostMapping
    public AjaxResult create(@RequestBody DataParseStrategyDTO dto) {
        return AjaxResult.success(strategyService.create(dto));
    }

    @PutMapping
    public AjaxResult update(@RequestBody DataParseStrategyDTO dto) {
        strategyService.update(dto);
        return AjaxResult.success();
    }

    @DeleteMapping("/{id}")
    public AjaxResult delete(@PathVariable Long id) {
        strategyService.delete(id);
        return AjaxResult.success();
    }

    @PutMapping("/{id}/status")
    public AjaxResult toggleStatus(@PathVariable Long id, @RequestParam Integer status) {
        strategyService.toggleStatus(id, status);
        return AjaxResult.success();
    }

    @PostMapping("/test")
    public AjaxResult testScript(@RequestBody DataParseTestRequest request) {
        long startTime = System.currentTimeMillis();
        try {
            // 1. 语法预检
            scriptExecutor.validateSyntax(request.getScriptCode());

            // 2. Topic 解析
            Matcher m = TOPIC_PATTERN.matcher(request.getTopic() == null ? "" : request.getTopic());
            if (!m.matches()) {
                return AjaxResult.success(DataParseTestResponse.error(
                        System.currentTimeMillis() - startTime, "Topic 格式非法"));
            }
            String sourceType = m.group(1);
            String deviceCode = m.group("deviceCode");
            String sensorNo = m.group("sensorNo");

            // 3. 元数据解析
            SensorMetadata metadata;
            try {
                metadata = sensorQueryService.requireSensorMetadata(request.getDeviceId(), sensorNo);
            } catch (ServiceException e) {
                return AjaxResult.success(DataParseTestResponse.error(
                        System.currentTimeMillis() - startTime, e.getMessage()));
            }

            // 4. 构建上下文
            Map<String, Object> ctx = new LinkedHashMap<>();
            ctx.put("topic", request.getTopic());
            ctx.put("payload", request.getTestPayload());
            ctx.put("deviceId", request.getDeviceId());
            ctx.put("deviceCode", deviceCode);
            ctx.put("sensorNo", sensorNo);
            ctx.put("attributes", metadata.attributes());

            // 5. 执行脚本
            List<ParseResultItem> items = scriptExecutor.execute(request.getScriptCode(), ctx);

            // 6. 校验 + 映射
            List<StandardMeasurementPoint> points = validateAndMap(items, metadata, sourceType,
                    request.getTestPayload().getBytes(StandardCharsets.UTF_8));

            long elapsed = System.currentTimeMillis() - startTime;
            return AjaxResult.success(DataParseTestResponse.ok(elapsed, points));
        } catch (GroovyScriptExecutor.ParseScriptException e) {
            long elapsed = System.currentTimeMillis() - startTime;
            return AjaxResult.success(DataParseTestResponse.error(elapsed, e.getMessage()));
        }
    }

    private List<StandardMeasurementPoint> validateAndMap(List<ParseResultItem> items,
                                                           SensorMetadata metadata,
                                                           String sourceType,
                                                           byte[] rawMessage) {
        if (items == null || items.isEmpty()) {
            throw new ServiceException("脚本未返回有效解析结果");
        }
        List<StandardMeasurementPoint> points = new ArrayList<>();
        long receiveTime = Instant.now().toEpochMilli();
        String payloadHash = buildHash(rawMessage);

        for (ParseResultItem item : items) {
            if (item.getSensorNo() == null || item.getSensorNo().isBlank())
                throw new ServiceException("解析结果缺少 sensorNo");
            if (item.getAttrCode() == null || item.getAttrCode().isBlank())
                throw new ServiceException("解析结果缺少 attrCode");
            if (item.getValue() == null)
                throw new ServiceException("解析结果缺少 value (attrCode=" + item.getAttrCode() + ")");

            String attrName = item.getAttrCode();
            String unit = null;
            if (metadata.attributes() != null) {
                for (SensorAttribute attr : metadata.attributes()) {
                    if (Objects.equals(attr.getAttrCode(), item.getAttrCode())) {
                        attrName = attr.getAttrName() != null ? attr.getAttrName() : attrName;
                        unit = attr.getUnit();
                        break;
                    }
                }
            }
            long dataTime = item.getDataTime() != null ? item.getDataTime() : receiveTime;
            int quality = item.getQuality() != null ? item.getQuality() : 0;

            points.add(StandardMeasurementPoint.builder()
                    .deviceId(metadata.deviceId()).sensorNo(item.getSensorNo())
                    .sensorId(metadata.sensorId()).attrCode(item.getAttrCode())
                    .attrName(attrName).unit(unit).dataTime(dataTime).value(item.getValue())
                    .quality(quality).reportTime(dataTime).receiveTime(receiveTime)
                    .sourceType(sourceType).payloadHash(payloadHash).build());
        }
        return points;
    }

    private String buildHash(byte[] payload) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(payload));
        } catch (NoSuchAlgorithmException e) { return ""; }
    }

    @GetMapping("/{id}/logs")
    public TableDataInfo getLogs(@PathVariable Long id) {
        List<DataParseLog> logs = logService.listByStrategyId(id);
        return TableDataInfo.success(logs, logs.size());
    }

    @DeleteMapping("/{id}/logs")
    public AjaxResult clearLogs(@PathVariable Long id) {
        logService.clearByStrategyId(id);
        return AjaxResult.success();
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add server/zwei-iot-parser/src/main/java/com/zwei/iot/parser/controller/DataParseController.java
git commit -m "feat: DataParseController — 策略 CRUD + 测试 + 日志 REST API"
```

---

### Task 15: 预置策略 SQL

**Files:**
- Modify: `db/upgrade/v3.9.3-parser-module.sql`

- [ ] **Step 1: 追加预置策略 INSERT**

在 DDL 脚本末尾追加：

```sql
-- 预置策略: 系统协议解析（翻译自 SysMonitorPayloadParser）
INSERT INTO `iot_data_parse_strategy` (`name`, `topic_pattern`, `script_code`, `description`, `status`, `is_preset`, `priority`) VALUES
(
  '系统协议解析',
  'sys/v1/*',
  'import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import com.zwei.iot.parser.domain.ParseResultItem

def parse(Map ctx) {
    String payload = ctx.payload as String
    JSONObject root = JSON.parseObject(payload)
    if (root == null || root.isEmpty()) {
        throw new RuntimeException("报文为空")
    }
    if (root.containsKey("version") || root.containsKey("data")) {
        return parseStandard(ctx, root, payload)
    }
    return parseLegacy(ctx, root, payload)
}

def parseStandard(Map ctx, JSONObject root, String payload) {
    String payloadSensorNo = root.getString("sensorNo")
    if (payloadSensorNo != null && !payloadSensorNo.isBlank() && payloadSensorNo != ctx.sensorNo) {
        throw new RuntimeException("payload 中 sensorNo 与 topic 不一致")
    }
    def items = []
    Object dataNode = root.get("data")
    if (dataNode instanceof JSONObject) {
        JSONObject dataObj = (JSONObject) dataNode
        if (dataObj.containsKey("time") || dataObj.containsKey("value")) {
            items.addAll(parseDataPoint(ctx, dataObj.get("time"), dataObj.get("value")))
        } else {
            dataObj.forEach { k, v -> items.addAll(parseDataPoint(ctx, k, v)) }
        }
    } else if (dataNode instanceof JSONArray) {
        ((JSONArray) dataNode).forEach { item ->
            if (item instanceof JSONObject) {
                items.addAll(parseDataPoint(ctx, ((JSONObject)item).get("time"), ((JSONObject)item).get("value")))
            }
        }
    } else {
        throw new RuntimeException("data 节点格式不支持")
    }
    return items
}

def parseLegacy(Map ctx, JSONObject root, String payload) {
    JSONObject deviceObject = root.getJSONObject(String.valueOf(ctx.deviceId))
    if (deviceObject == null || deviceObject.isEmpty()) {
        throw new RuntimeException("未识别的历史/兼容报文结构")
    }
    def items = []
    deviceObject.forEach { key, rawValue ->
        if (!key.endsWith("_" + ctx.sensorNo)) return
        if (rawValue instanceof JSONObject && isHistoryMap((JSONObject)rawValue)) {
            ((JSONObject)rawValue).forEach { timeK, timeV ->
                items.addAll(parseDataPoint(ctx, timeK, timeV))
            }
        } else {
            items.addAll(parseDataPoint(ctx, System.currentTimeMillis(), rawValue))
        }
    }
    return items
}

def parseDataPoint(Map ctx, Object timeValue, Object rawValue) {
    long dataTime = resolveTimestamp(timeValue, System.currentTimeMillis())
    def items = []
    def attrs = ctx.attributes as List

    if (rawValue instanceof Number) {
        def attr = findAttr(attrs, "value")
        items << new ParseResultItem(ctx.sensorNo, attr.getAttrCode(), ((Number)rawValue).doubleValue(), dataTime, 0)
    } else if (rawValue instanceof JSONObject) {
        ((JSONObject)rawValue).forEach { k, v ->
            Double dv = toDouble(v)
            if (dv != null) {
                def attr = findAttr(attrs, k)
                items << new ParseResultItem(ctx.sensorNo, attr.getAttrCode(), dv, dataTime, 0)
            }
        }
    } else if (rawValue instanceof String) {
        String sv = (String)rawValue
        if (!sv.contains(",")) {
            def attr = findAttr(attrs, "value")
            Double dv = toDouble(sv)
            if (dv != null) items << new ParseResultItem(ctx.sensorNo, attr.getAttrCode(), dv, dataTime, 0)
        } else if (!attrs.isEmpty()) {
            String[] vals = sv.split(",")
            for (int i = 0; i < vals.length && i < attrs.size(); i++) {
                Double dv = toDouble(vals[i])
                if (dv != null) {
                    def attr = attrs.get(i)
                    items << new ParseResultItem(ctx.sensorNo, attr.getAttrCode(), dv, dataTime, 0)
                }
            }
        }
    }
    return items
}

private def findAttr(List attrs, String candidate) {
    def found = attrs.find { it.getAttrCode() == candidate }
    return found ?: (attrs.isEmpty() ? new DummyAttr(candidate) : attrs.get(0))
}

private Double toDouble(Object v) {
    if (v == null) return null
    if (v instanceof Number) return ((Number)v).doubleValue()
    String s = v.toString().trim()
    if (s.isEmpty()) return null
    try { return Double.parseDouble(s) } catch (Exception e) { return null }
}

private long resolveTimestamp(Object v, long defaultVal) {
    if (v == null) return defaultVal
    if (v instanceof Number) return ((Number)v).longValue()
    String s = v.toString()
    if (s.chars().allMatch(Character.&isDigit)) return Long.parseLong(s)
    try { return java.time.Instant.parse(s).toEpochMilli() } catch (Exception e) { return defaultVal }
}

private boolean isHistoryMap(JSONObject obj) {
    return obj.keySet().stream().allMatch { k ->
        try { resolveTimestamp(k, -1L); return true } catch (Exception e) { return false }
    }
}

// 兜底属性
class DummyAttr {
    private String attrCode
    DummyAttr(String code) { this.attrCode = code }
    String getAttrCode() { return attrCode }
    String getAttrName() { return attrCode }
    String getUnit() { return null }
}',
  '标准 JSON 报文解析，支持单值/多值/CSV/历史兼容格式',
  1, 1, 10
);

-- 预置策略: 国标协议解析（占位）
INSERT INTO `iot_data_parse_strategy` (`name`, `topic_pattern`, `script_code`, `description`, `status`, `is_preset`, `priority`) VALUES
(
  '国标协议解析',
  'gb/v1/*',
  'import com.zwei.iot.parser.domain.ParseResultItem

def parse(Map ctx) {
    throw new RuntimeException("国标协议解析暂不支持")
}',
  '国标协议数据解析策略（占位）',
  1, 1, 10
);
```

- [ ] **Step 2: Commit**

```bash
git add db/upgrade/v3.9.3-parser-module.sql
git commit -m "feat: 预置 sys/gb 解析策略

sys 翻译自 SysMonitorPayloadParser 完整逻辑，gb 保持占位"
```

---

### Task 16: 改造 MonitorIngestFacade

**Files:**
- Modify: `server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/service/MonitorIngestFacade.java`

- [ ] **Step 1: 改造 MonitorIngestFacade**

```java
package com.zwei.iot.timeseries.service;

import com.zwei.common.domain.StandardMeasurementPoint;
import com.zwei.common.exception.ServiceException;
import com.zwei.iot.parser.engine.ParseEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * MQTT 监测数据接入门面（薄化）。
 */
@Slf4j
@Service
public class MonitorIngestFacade {
    private final ParseEngine parseEngine;
    private final MonitorIngestStreamService streamService;

    @Autowired
    public MonitorIngestFacade(ParseEngine parseEngine,
                               MonitorIngestStreamService streamService) {
        this.parseEngine = parseEngine;
        this.streamService = streamService;
    }

    public void ingest(String topic, byte[] message, Long deviceId) {
        List<StandardMeasurementPoint> points;
        try {
            points = parseEngine.parse(topic, message, deviceId);
        } catch (ServiceException e) {
            log.warn("解析失败: topic={}, deviceId={}, error={}", topic, deviceId, e.getMessage());
            return;
        }
        if (points != null && !points.isEmpty()) {
            streamService.enqueue(points);
            log.debug("监测报文已入缓冲队列, topic={}, points={}", topic, points.size());
        }
    }
}
```

- [ ] **Step 2: 编译验证**

Run: `cd server && mvn clean compile -pl zwei-iot-timeseries -am`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/service/MonitorIngestFacade.java
git commit -m "refactor: MonitorIngestFacade 薄化，委托 ParseEngine 完成解析"
```

---

### Task 17: 清理 timeseries 遗留代码

**Files:**
- Delete: `server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/parser/MonitorPayloadParser.java`
- Delete: `server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/parser/SysMonitorPayloadParser.java`
- Delete: `server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/parser/GbMonitorPayloadParser.java`
- Delete: `server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/service/MonitorMetadataService.java`

- [ ] **Step 1: 删除遗留文件**

```bash
rm server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/parser/MonitorPayloadParser.java
rm server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/parser/SysMonitorPayloadParser.java
rm server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/parser/GbMonitorPayloadParser.java
rm server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/service/MonitorMetadataService.java
```

- [ ] **Step 2: 清理空目录**

```bash
rmdir server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/parser 2>/dev/null || true
```

- [ ] **Step 3: 编译验证**

Run: `cd server && mvn clean compile`
Expected: BUILD SUCCESS (all 15 modules)

- [ ] **Step 4: Commit**

```bash
git add -u server/zwei-iot-timeseries/
git commit -m "refactor: 删除 timeseries 遗留解析器代码

MonitorPayloadParser/Sys/Gb/MonitorMetadataService 已被 parser 模块替代"
```

---

### Task 18: 全链路编译 + 启动验证

- [ ] **Step 1: 全模块编译**

Run: `cd server && mvn clean compile`
Expected: BUILD SUCCESS

- [ ] **Step 2: 检查启动（如基础设施就绪）**

Run: `cd server && mvn spring-boot:run -pl zwei-admin`
Expected: 应用正常启动，无 Bean 注入失败。如果本地 MySQL/Redis 不可用则跳过此步。

---

### 完成检查清单

- [ ] `StandardMeasurementPoint` 在 `zwei-common` 下，旧引用全部更新
- [ ] 数据库升级脚本 `db/upgrade/v3.9.3-parser-module.sql` 已创建
- [ ] `zwei-iot-parser` 在父 POM modules 和 dependencyManagement 中注册
- [ ] `zwei-admin` POM 已添加 `zwei-iot-parser` 依赖
- [ ] ParseEngine → StrategyMatcher → GroovyScriptExecutor 全链路可编译
- [ ] 预置 sys 策略完整保留了原有解析逻辑（单值/多值/CSV/历史/兼容）
- [ ] MonitorIngestFacade 简化为 20 行
- [ ] timeseries 遗留解析器代码已清理
- [ ] `mvn clean compile` 全模块通过
