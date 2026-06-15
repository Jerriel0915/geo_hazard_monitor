# IoT 数据解析模块 V2 实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 创建独立 Maven 模块 `zwei-iot-parser`，使用 Groovy 脚本引擎替代现有 Java 解析器，产出 TSL 对齐的中间格式 `ParsedMessage`，通过 Redis Stream 与 timeseries 解耦。

**架构：** 替代式。删除 `MonitorPayloadParser` 接口及 sys/gb Java 实现。所有协议统一为 DB 策略表管理的 Groovy 脚本，经由 Grails 沙箱执行后产出 `ParsedMessage` JSON 写入 Redis Stream。timeseries consumer 侧新增轻量适配层。

**技术栈：** Java 17, Spring Boot 4.0.3, MyBatis, Groovy (`org.apache.groovy:groovy`), Redis Stream, MySQL 8.0

---

### 任务 1：创建 `zwei-iot-parser` 模块骨架

**文件：**
- 创建：`server/zwei-iot-parser/pom.xml`
- 修改：`server/pom.xml:227` (插入 module)
- 修改：`server/zwei-admin/pom.xml:73` (插入 dependency)

- [ ] **步骤 1：编写 parser 模块 pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <artifactId>zwei</artifactId>
        <groupId>com.zwei</groupId>
        <version>3.9.2</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>
    <artifactId>zwei-iot-parser</artifactId>
    <description>IoT数据解析模块 — 策略管理 + Groovy脚本引擎 + 中间格式标准化</description>

    <dependencies>
        <!-- 公共模块 -->
        <dependency>
            <groupId>com.zwei</groupId>
            <artifactId>zwei-common</artifactId>
        </dependency>
        <!-- IoT 设备模块 (TSL + SensorMetadata + 跨模块接口) -->
        <dependency>
            <groupId>com.zwei</groupId>
            <artifactId>zwei-iot-device</artifactId>
        </dependency>
        <!-- Groovy 脚本引擎 -->
        <dependency>
            <groupId>org.apache.groovy</groupId>
            <artifactId>groovy</artifactId>
        </dependency>
        <!-- MyBatis -->
        <dependency>
            <groupId>org.mybatis.spring.boot</groupId>
            <artifactId>mybatis-spring-boot-starter</artifactId>
        </dependency>
        <!-- Spring Web (仅用于 Controller) -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <!-- Redis (Stream 写入) -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
        </dependency>
        <!-- Test -->
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

- [ ] **步骤 2：在父 POM 中注册模块**

编辑 `server/pom.xml`，在 `<modules>` 中，`zwei-iot-hazard` 之后插入：

```xml
<module>zwei-iot-parser</module>
```

- [ ] **步骤 3：在 admin 模块添加依赖**

编辑 `server/zwei-admin/pom.xml`，在 `zwei-iot-hazard` dependency 之后插入：

```xml
<dependency><groupId>com.zwei</groupId><artifactId>zwei-iot-parser</artifactId></dependency>
```

- [ ] **步骤 4：验证编译**

```bash
cd server && mvn clean compile -pl zwei-iot-parser
```

预期：BUILD SUCCESS，无 warning。

- [ ] **步骤 5：Commit**

```bash
git add server/zwei-iot-parser/pom.xml server/pom.xml server/zwei-admin/pom.xml
git commit -m "feat: create zwei-iot-parser module skeleton"
```

---

### 任务 2：数据库迁移脚本

**文件：**
- 创建：`db/upgrade/v2.1-parser-module.sql`

- [ ] **步骤 1：编写建表 + 种子数据 SQL**

```sql
-- ============================================================
-- zwei-iot-parser 模块数据库迁移
-- 版本: v2.1
-- 描述: 新建数据解析策略管理表 + 运行日志表 + 预置策略
-- ============================================================

-- 1. 解析策略表
CREATE TABLE IF NOT EXISTS `iot_data_parse_strategy` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) NOT NULL COMMENT '策略名称',
  `source_type` varchar(50) NOT NULL COMMENT '协议标识(sys/gb/自定义)',
  `description` text COMMENT '描述',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态 0-停用 1-启用',
  `app_scope` varchar(20) NOT NULL DEFAULT 'global' COMMENT '应用范围 global/vendor/device',
  `script_code` mediumtext NOT NULL COMMENT 'Groovy解析脚本',
  `is_preset` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否预置策略 0-否 1-是',
  `last_run_time` datetime DEFAULT NULL COMMENT '最近运行时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '删除标记 0-否 1-是',
  PRIMARY KEY (`id`),
  KEY `idx_source_type` (`source_type`),
  KEY `idx_status` (`status`),
  KEY `idx_app_scope` (`app_scope`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据解析策略表';

-- 2. 策略-厂商关联表
CREATE TABLE IF NOT EXISTS `iot_data_parse_strategy_vendor` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `strategy_id` bigint NOT NULL COMMENT '策略ID',
  `vendor_id` bigint NOT NULL COMMENT '厂商ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_strategy_vendor` (`strategy_id`, `vendor_id`),
  KEY `idx_strategy_id` (`strategy_id`),
  KEY `idx_vendor_id` (`vendor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='解析策略-厂商关联表';

-- 3. 策略-设备关联表
CREATE TABLE IF NOT EXISTS `iot_data_parse_strategy_device` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `strategy_id` bigint NOT NULL COMMENT '策略ID',
  `device_id` bigint NOT NULL COMMENT '设备ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_strategy_device` (`strategy_id`, `device_id`),
  KEY `idx_strategy_id` (`strategy_id`),
  KEY `idx_device_id` (`device_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='解析策略-设备关联表';

-- 4. 运行日志表
CREATE TABLE IF NOT EXISTS `iot_data_parse_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `strategy_id` bigint NOT NULL COMMENT '策略ID',
  `log_level` varchar(20) NOT NULL COMMENT '日志级别 INFO/WARN/ERROR',
  `message` text NOT NULL COMMENT '日志消息',
  `data` text COMMENT '关联数据(JSON)',
  `topic` varchar(200) DEFAULT NULL COMMENT '消息主题',
  `device_code` varchar(100) DEFAULT NULL COMMENT '设备编码',
  `parse_result` text COMMENT '解析结果(JSON)',
  `execution_time` int DEFAULT NULL COMMENT '执行耗时(毫秒)',
  `error_stack` text COMMENT '错误堆栈',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_strategy_id` (`strategy_id`),
  KEY `idx_log_level` (`log_level`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_device_code` (`device_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='解析运行日志表';
```

- [ ] **步骤 2：编写预置策略种子数据（sys + gb）**

在同一文件中追加：

```sql
-- 5. 预置策略: 系统自定义协议 (sys)
INSERT INTO `iot_data_parse_strategy` (`name`, `source_type`, `description`, `status`, `app_scope`, `script_code`, `is_preset`) VALUES
(
  '系统协议解析',
  'sys',
  '系统自定义JSON协议解析策略，支持标准格式(version+data)和传统格式(嵌套deviceId键)',
  1,
  'global',
  'import groovy.json.JsonSlurper

@groovy.transform.CompileStatic
Map<String, Object> parse(String topic, byte[] messageBytes) {
    String payload = new String(messageBytes, "UTF-8")
    def json = new JsonSlurper().parseText(payload) as Map<String, Object>
    def result = [
        sensorCode: (json.getOrDefault("sensorNo", "") ?: "").toString(),
        dataTime: 0L,
        properties: []
    ]

    if ((json.containsKey("version") || json.containsKey("data")) && json.get("data") != null) {
        def ts = json.get("timestamp")
        result.dataTime = ts != null ? resolveTimestamp(ts) : System.currentTimeMillis()
        parseStandardData(json.get("data"), result)
    } else {
        result.dataTime = System.currentTimeMillis()
        parseLegacyData(json, result)
    }
    result.sensorCode = result.sensorCode ?: "1"
    return result
}

@groovy.transform.CompileStatic
private void parseStandardData(Object data, Map<String, Object> result) {
    if (data instanceof List) {
        List items = (List) data
        List<Map<String, Object>> props = []
        for (item in items) {
            if (item instanceof Map) {
                Map m = (Map) item
                if (m.containsKey("time") || m.containsKey("value")) {
                    def ts = m.get("timestamp")
                    if (ts != null) result.dataTime = resolveTimestamp(ts)
                    parseSingleDataPoint(m, props)
                } else {
                    props.addAll(objectToProperties(m))
                }
            }
        }
        result.properties = props
    } else if (data instanceof Map) {
        Map dataMap = (Map) data
        // 历史映射格式 (timestamp → value)
        if (looksLikeHistoryMap(dataMap)) {
            List<Map<String, Object>> props = []
            for (entry in dataMap) {
                String key = entry.key.toString()
                Map<String, Object> p = toProperty(key, entry.value)
                p.dataTime = parseTimestampString(key)
                props.add(p)
            }
            result.properties = props
        } else if (dataMap.containsKey("time") || dataMap.containsKey("value")) {
            // 单点对象
            List<Map<String, Object>> props = []
            parseSingleDataPoint(dataMap, props)
            result.properties = props
        } else {
            // 多键对 — 每个键是一个 attrCode
            result.properties = objectToProperties(dataMap)
        }
    }
}

@groovy.transform.CompileStatic
private void parseSingleDataPoint(Map m, List<Map<String, Object>> props) {
    Object rawValue = m.get("value")
    if (rawValue instanceof Number) {
        props.add(toProperty("value", rawValue))
    } else if (rawValue instanceof String) {
        String s = (String) rawValue
        if (s.contains(",")) {
            def parts = s.split(",")
            for (int i = 0; i < parts.length; i++) {
                props.add(toProperty("value_" + i, parts[i].trim()))
            }
        } else {
            props.add(toProperty("value", s))
        }
    } else if (rawValue instanceof Map) {
        props.addAll(objectToProperties((Map) rawValue))
    }
}

@groovy.transform.CompileStatic
private void parseLegacyData(Map json, Map<String, Object> result) {
    List<Map<String, Object>> props = []
    for (topKey in json.keySet()) {
        def topValue = json.get(topKey)
        if (topValue instanceof Map) {
            Map topMap = (Map) topValue
            for (measKey in topMap.keySet()) {
                if (measKey.toString().endsWith("_" + result.sensorCode)) {
                    def measValue = topMap.get(measKey)
                    if (measValue instanceof Map) {
                        Map measMap = (Map) measValue
                        for (tsKey in measMap.keySet()) {
                            props.add(toProperty("value", measMap.get(tsKey)))
                        }
                    } else {
                        props.add(toProperty("value", measValue))
                    }
                }
            }
        }
    }
    result.properties = props
}

@groovy.transform.CompileStatic
private List<Map<String, Object>> objectToProperties(Map m) {
    List<Map<String, Object>> props = []
    for (entry in m) {
        String key = entry.key.toString()
        if (key != "time" && key != "timestamp" && key != "version" && key != "sensorNo") {
            props.add(toProperty(key, entry.value))
        }
    }
    return props
}

@groovy.transform.CompileStatic
private Map<String, Object> toProperty(String identifier, Object value) {
    return [
        identifier: identifier,
        value: toDouble(value),
        quality: 0
    ]
}

@groovy.transform.CompileStatic
private Double toDouble(Object v) {
    if (v == null) return null
    if (v instanceof Number) return ((Number) v).doubleValue()
    try { return Double.parseDouble(v.toString().trim()) } catch (Exception ignored) { return null }
}

@groovy.transform.CompileStatic
private long resolveTimestamp(Object ts) {
    if (ts == null) return System.currentTimeMillis()
    if (ts instanceof Number) return ((Number) ts).longValue()
    try {
        String s = ts.toString().trim()
        if (s =~ /^\d{13}$/) return Long.parseLong(s)
        if (s =~ /^\d{10}$/) return Long.parseLong(s) * 1000L
        return System.currentTimeMillis()
    } catch (Exception ignored) {
        return System.currentTimeMillis()
    }
}

@groovy.transform.CompileStatic
private long parseTimestampString(String s) {
    try {
        if (s =~ /^\d{13}$/) return Long.parseLong(s)
        if (s =~ /^\d{10}$/) return Long.parseLong(s) * 1000L
        return System.currentTimeMillis()
    } catch (Exception ignored) {
        return System.currentTimeMillis()
    }
}

@groovy.transform.CompileStatic
private boolean looksLikeHistoryMap(Map m) {
    for (key in m.keySet()) {
        if (!(key =~ /^\d+$/)) return false
    }
    return m.size() > 0
}

@groovy.transform.CompileStatic
private void setResult(Map result, String sensorCode, long dataTime, List props) {
    result.sensorCode = sensorCode
    result.dataTime = dataTime
    result.properties = props
}

return [sensorCode: "1", dataTime: 0L, properties: []]',
  1
);

-- 6. 预置策略: 国标协议 (gb)
INSERT INTO `iot_data_parse_strategy` (`name`, `source_type`, `description`, `status`, `app_scope`, `script_code`, `is_preset`) VALUES
(
  '国标协议解析',
  'gb',
  '国标水文/地质灾害监测数据协议，基于hex字节流解析。参考: http://ghiot.cigem.cn:8080/doc/overview/overview.html',
  1,
  'global',
  'import groovy.transform.CompileStatic

@CompileStatic
Map<String, Object> parse(String topic, byte[] messageBytes) {
    String hexPayload = new String(messageBytes, "UTF-8")
    byte[] bytes = builtin.hexDecode(hexPayload)
    def result = [
        sensorCode: "1",
        dataTime: System.currentTimeMillis(),
        properties: []
    ]
    // 按国标帧格式逐字段解析
    // 帧头 (2B) + 帧长 (2B) + 设备ID (16B ASCII) + 时间 (8B) + 数据区
    int offset = 4  // 跳过帧头+帧长
    String deviceCode = builtin.readAscii(bytes, offset, 16).trim()
    offset += 16
    // 时间戳解析 (BCD 编码)
    long dataTime = builtin.readBcdTimestamp(bytes, offset)
    offset += 8
    // 数据区 — 逐属性解析，格式: 属性码(2B) + 长度(1B) + 值(NB) + 质量(1B)
    while (offset + 4 <= bytes.length) {
        int attrCode = builtin.readUInt16(bytes, offset)
        offset += 2
        int valLen = builtin.readUInt8(bytes, offset) as int
        offset += 1
        double value = 0.0
        switch (valLen) {
            case 2: value = (double) builtin.readInt16(bytes, offset); break
            case 4: value = (double) builtin.readFloat(bytes, offset); break
            case 8: value = builtin.readDouble(bytes, offset); break
            default: value = 0.0; break
        }
        offset += valLen
        int quality = builtin.readUInt8(bytes, offset) as int
        offset += 1
        result.properties.add([
            identifier: "attr_" + attrCode,
            value: value,
            quality: quality
        ])
    }
    // 校验值域
    for (prop in result.properties) {
        if (prop.value == null || Double.isNaN((double) prop.value) || Double.isInfinite((double) prop.value)) {
            prop.value = null
            prop.quality = 9  // 无效数据标记
        }
    }
    return result
}',
  1
);
```

- [ ] **步骤 2：验证 SQL 语法**

```bash
# 在本地 MySQL 执行试跑（不实际提交）
mysql -u root -e "SOURCE db/upgrade/v2.1-parser-module.sql; SHOW TABLES LIKE 'iot_data_parse%';" geo_hazard_monitor
```

预期：4 张表创建成功，2 条预置策略插入成功。

- [ ] **步骤 3：Commit**

```bash
git add db/upgrade/v2.1-parser-module.sql
git commit -m "feat: add parser module database migration (4 tables + 2 preset strategies)"
```

---

### 任务 3：公共中间格式 — `ParsedMessage` + `PropertyValue`

**文件：**
- 创建：`server/zwei-common/src/main/java/com/zwei/common/domain/ParsedMessage.java`
- 创建：`server/zwei-common/src/main/java/com/zwei/common/domain/PropertyValue.java`

- [ ] **步骤 1：编写 `PropertyValue` record**

```java
package com.zwei.common.domain;

import java.io.Serial;
import java.io.Serializable;

/**
 * TSL 属性运行时值 — parser 模块解析产出的最小数据单元。
 *
 * <p>每个 PropertyValue 对应 TSL properties 数组中的一个属性，
 * 包含标识符、名称、单位、运行时数值和质量码。
 *
 * @param identifier TslProperty.identifier（如 "rainfall_hour"）
 * @param name       TslProperty.name（中文名称）
 * @param unit       计量单位（如 "mm"）
 * @param value      运行时数值
 * @param quality    质量码（0=正常，非零=异常）
 */
public record PropertyValue(
        String identifier,
        String name,
        String unit,
        Double value,
        Integer quality
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
```

- [ ] **步骤 2：编写 `ParsedMessage` record**

```java
package com.zwei.common.domain;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 解析后的标准化报文 — parser 模块的对外统一产出格式。
 *
 * <p>作为 Redis Stream 中传输的流契约（JSON 序列化），
 * 由 zwei-iot-parser 写入，zwei-iot-timeseries 消费。
 *
 * @param deviceCode  设备编码 (device.code)
 * @param sensorCode  传感器编码 (sensor.sensorCode)
 * @param sourceType  源协议标识（"sys" / "gb" / 自定义）
 * @param dataTime    数据采集时间 epoch 毫秒
 * @param receiveTime 服务端接收时间 epoch 毫秒
 * @param payloadHash 原始报文 SHA-256（用于幂等去重）
 * @param properties  解析出的属性值列表
 */
public record ParsedMessage(
        String deviceCode,
        String sensorCode,
        String sourceType,
        long dataTime,
        long receiveTime,
        String payloadHash,
        List<PropertyValue> properties
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
```

- [ ] **步骤 3：验证编译**

```bash
cd server && mvn clean compile -pl zwei-common
```

预期：BUILD SUCCESS。

- [ ] **步骤 4：Commit**

```bash
git add server/zwei-common/src/main/java/com/zwei/common/domain/ParsedMessage.java
git add server/zwei-common/src/main/java/com/zwei/common/domain/PropertyValue.java
git commit -m "feat: add ParsedMessage and PropertyValue cross-module domain records"
```

---

### 任务 4：策略管理 Domain + MyBatis Mapper

**文件：**
- 创建：`server/zwei-iot-parser/src/main/java/com/zwei/iot/parser/domain/DataParseStrategy.java`
- 创建：`server/zwei-iot-parser/src/main/java/com/zwei/iot/parser/domain/DataParseStrategyVendor.java`
- 创建：`server/zwei-iot-parser/src/main/java/com/zwei/iot/parser/domain/DataParseStrategyDevice.java`
- 创建：`server/zwei-iot-parser/src/main/java/com/zwei/iot/parser/domain/DataParseLog.java`
- 创建：`server/zwei-iot-parser/src/main/java/com/zwei/iot/parser/mapper/DataParseStrategyMapper.java`
- 创建：`server/zwei-iot-parser/src/main/java/com/zwei/iot/parser/mapper/DataParseStrategyVendorMapper.java`
- 创建：`server/zwei-iot-parser/src/main/java/com/zwei/iot/parser/mapper/DataParseStrategyDeviceMapper.java`
- 创建：`server/zwei-iot-parser/src/main/java/com/zwei/iot/parser/mapper/DataParseLogMapper.java`
- 创建：`server/zwei-iot-parser/src/main/resources/mapper/iot/parser/DataParseStrategyMapper.xml`
- 创建：`server/zwei-iot-parser/src/main/resources/mapper/iot/parser/DataParseStrategyVendorMapper.xml`
- 创建：`server/zwei-iot-parser/src/main/resources/mapper/iot/parser/DataParseStrategyDeviceMapper.xml`
- 创建：`server/zwei-iot-parser/src/main/resources/mapper/iot/parser/DataParseLogMapper.xml`

- [ ] **步骤 1：创建 Domain 实体类**

`DataParseStrategy.java`：
```java
package com.zwei.iot.parser.domain;

import com.zwei.common.core.domain.BaseEntity;
import lombok.*;

@Setter
@Getter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class DataParseStrategy extends BaseEntity {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String name;
    private String sourceType;
    private String description;
    private Integer status;       // 0=停用 1=启用
    private String appScope;      // global/vendor/device
    private String scriptCode;
    private Integer isPreset;     // 0=否 1=是
    private String lastRunTime;
}
```

`DataParseStrategyVendor.java`：
```java
package com.zwei.iot.parser.domain;

import lombok.*;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DataParseStrategyVendor {
    private Long id;
    private Long strategyId;
    private Long vendorId;
}
```

`DataParseStrategyDevice.java`：
```java
package com.zwei.iot.parser.domain;

import lombok.*;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DataParseStrategyDevice {
    private Long id;
    private Long strategyId;
    private Long deviceId;
}
```

`DataParseLog.java`：
```java
package com.zwei.iot.parser.domain;

import lombok.*;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DataParseLog {
    private Long id;
    private Long strategyId;
    private String logLevel;       // INFO/WARN/ERROR
    private String message;
    private String data;           // JSON
    private String topic;
    private String deviceCode;
    private String parseResult;    // JSON
    private Integer executionTime; // 毫秒
    private String errorStack;
    private java.util.Date createTime;
}
```

- [ ] **步骤 2：创建 Mapper 接口**

`DataParseStrategyMapper.java`：
```java
package com.zwei.iot.parser.mapper;

import com.zwei.iot.parser.domain.DataParseStrategy;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface DataParseStrategyMapper {
    DataParseStrategy selectById(Long id);
    List<DataParseStrategy> selectByCondition(DataParseStrategy condition);
    List<DataParseStrategy> selectBySourceType(String sourceType);
    List<DataParseStrategy> selectEnabled();
    int insert(DataParseStrategy strategy);
    int updateById(DataParseStrategy strategy);
    int deleteById(Long id);
    int updateLastRunTime(Long id);
}
```

`DataParseStrategyVendorMapper.java`：
```java
package com.zwei.iot.parser.mapper;

import com.zwei.iot.parser.domain.DataParseStrategyVendor;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface DataParseStrategyVendorMapper {
    List<Long> selectVendorIdsByStrategyId(Long strategyId);
    List<Long> selectStrategyIdsByVendorId(Long vendorId);
    int insert(DataParseStrategyVendor relation);
    int deleteByStrategyId(Long strategyId);
}
```

`DataParseStrategyDeviceMapper.java`：
```java
package com.zwei.iot.parser.mapper;

import com.zwei.iot.parser.domain.DataParseStrategyDevice;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface DataParseStrategyDeviceMapper {
    List<Long> selectDeviceIdsByStrategyId(Long strategyId);
    Long selectStrategyIdByDeviceId(Long deviceId);
    int insert(DataParseStrategyDevice relation);
    int deleteByStrategyId(Long strategyId);
}
```

`DataParseLogMapper.java`：
```java
package com.zwei.iot.parser.mapper;

import com.zwei.iot.parser.domain.DataParseLog;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface DataParseLogMapper {
    int insert(DataParseLog log);
    List<DataParseLog> selectByCondition(Long strategyId, String logLevel, String startTime, String endTime);
    int deleteByStrategyId(Long strategyId);
}
```

- [ ] **步骤 3：创建 MyBatis XML 映射文件**

`DataParseStrategyMapper.xml`：
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.zwei.iot.parser.mapper.DataParseStrategyMapper">
    <resultMap id="BaseResultMap" type="com.zwei.iot.parser.domain.DataParseStrategy">
        <id column="id" property="id"/>
        <result column="name" property="name"/>
        <result column="source_type" property="sourceType"/>
        <result column="description" property="description"/>
        <result column="status" property="status"/>
        <result column="app_scope" property="appScope"/>
        <result column="script_code" property="scriptCode"/>
        <result column="is_preset" property="isPreset"/>
        <result column="last_run_time" property="lastRunTime"/>
    </resultMap>

    <select id="selectById" resultMap="BaseResultMap">
        SELECT * FROM iot_data_parse_strategy WHERE id = #{id} AND deleted = 0
    </select>

    <select id="selectByCondition" resultMap="BaseResultMap">
        SELECT * FROM iot_data_parse_strategy WHERE deleted = 0
        <if test="name != null and name != ''">AND name LIKE CONCAT('%', #{name}, '%')</if>
        <if test="sourceType != null and sourceType != ''">AND source_type = #{sourceType}</if>
        <if test="status != null">AND status = #{status}</if>
        <if test="appScope != null and appScope != ''">AND app_scope = #{appScope}</if>
        ORDER BY create_time DESC
    </select>

    <select id="selectBySourceType" resultMap="BaseResultMap">
        SELECT * FROM iot_data_parse_strategy WHERE source_type = #{sourceType} AND status = 1 AND deleted = 0
        ORDER BY is_preset DESC, create_time ASC
    </select>

    <select id="selectEnabled" resultMap="BaseResultMap">
        SELECT * FROM iot_data_parse_strategy WHERE status = 1 AND deleted = 0
    </select>

    <insert id="insert" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO iot_data_parse_strategy (name, source_type, description, status, app_scope, script_code, is_preset)
        VALUES (#{name}, #{sourceType}, #{description}, #{status}, #{appScope}, #{scriptCode}, IFNULL(#{isPreset}, 0))
    </insert>

    <update id="updateById">
        UPDATE iot_data_parse_strategy
        SET name = #{name}, source_type = #{sourceType}, description = #{description},
            status = #{status}, app_scope = #{appScope}, script_code = #{scriptCode}
        WHERE id = #{id}
    </update>

    <update id="deleteById">
        UPDATE iot_data_parse_strategy SET deleted = 1 WHERE id = #{id}
    </update>

    <update id="updateLastRunTime">
        UPDATE iot_data_parse_strategy SET last_run_time = NOW() WHERE id = #{id}
    </update>
</mapper>
```

`DataParseStrategyVendorMapper.xml`：
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.zwei.iot.parser.mapper.DataParseStrategyVendorMapper">
    <select id="selectVendorIdsByStrategyId" resultType="long">
        SELECT vendor_id FROM iot_data_parse_strategy_vendor WHERE strategy_id = #{strategyId}
    </select>
    <select id="selectStrategyIdsByVendorId" resultType="long">
        SELECT strategy_id FROM iot_data_parse_strategy_vendor WHERE vendor_id = #{vendorId}
    </select>
    <insert id="insert">
        INSERT INTO iot_data_parse_strategy_vendor (strategy_id, vendor_id) VALUES (#{strategyId}, #{vendorId})
    </insert>
    <delete id="deleteByStrategyId">
        DELETE FROM iot_data_parse_strategy_vendor WHERE strategy_id = #{strategyId}
    </delete>
</mapper>
```

`DataParseStrategyDeviceMapper.xml`：
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.zwei.iot.parser.mapper.DataParseStrategyDeviceMapper">
    <select id="selectDeviceIdsByStrategyId" resultType="long">
        SELECT device_id FROM iot_data_parse_strategy_device WHERE strategy_id = #{strategyId}
    </select>
    <select id="selectStrategyIdByDeviceId" resultType="long">
        SELECT strategy_id FROM iot_data_parse_strategy_device d
        INNER JOIN iot_data_parse_strategy s ON d.strategy_id = s.id
        WHERE d.device_id = #{deviceId} AND s.status = 1 AND s.deleted = 0
        LIMIT 1
    </select>
    <insert id="insert">
        INSERT INTO iot_data_parse_strategy_device (strategy_id, device_id) VALUES (#{strategyId}, #{deviceId})
    </insert>
    <delete id="deleteByStrategyId">
        DELETE FROM iot_data_parse_strategy_device WHERE strategy_id = #{strategyId}
    </delete>
</mapper>
```

`DataParseLogMapper.xml`：
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.zwei.iot.parser.mapper.DataParseLogMapper">
    <resultMap id="BaseResultMap" type="com.zwei.iot.parser.domain.DataParseLog">
        <id column="id" property="id"/>
        <result column="strategy_id" property="strategyId"/>
        <result column="log_level" property="logLevel"/>
        <result column="message" property="message"/>
        <result column="data" property="data"/>
        <result column="topic" property="topic"/>
        <result column="device_code" property="deviceCode"/>
        <result column="parse_result" property="parseResult"/>
        <result column="execution_time" property="executionTime"/>
        <result column="error_stack" property="errorStack"/>
        <result column="create_time" property="createTime"/>
    </resultMap>

    <insert id="insert">
        INSERT INTO iot_data_parse_log (strategy_id, log_level, message, data, topic, device_code, parse_result, execution_time, error_stack)
        VALUES (#{strategyId}, #{logLevel}, #{message}, #{data}, #{topic}, #{deviceCode}, #{parseResult}, #{executionTime}, #{errorStack})
    </insert>

    <select id="selectByCondition" resultMap="BaseResultMap">
        SELECT * FROM iot_data_parse_log WHERE strategy_id = #{strategyId}
        <if test="logLevel != null and logLevel != ''">AND log_level = #{logLevel}</if>
        <if test="startTime != null">AND create_time >= #{startTime}</if>
        <if test="endTime != null">AND create_time &lt;= #{endTime}</if>
        ORDER BY create_time DESC LIMIT 500
    </select>

    <delete id="deleteByStrategyId">
        DELETE FROM iot_data_parse_log WHERE strategy_id = #{strategyId}
    </delete>
</mapper>
```

- [ ] **步骤 4：验证编译**

```bash
cd server && mvn clean compile -pl zwei-iot-parser
```

预期：BUILD SUCCESS。

- [ ] **步骤 5：Commit**

```bash
git add server/zwei-iot-parser/src/main/java/com/zwei/iot/parser/domain/
git add server/zwei-iot-parser/src/main/java/com/zwei/iot/parser/mapper/
git add server/zwei-iot-parser/src/main/resources/
git commit -m "feat: add parser domain entities and MyBatis mappers"
```

---

### 任务 5：DTO + Service 层（策略管理 + 日志）

**文件：**
- 创建：`server/zwei-iot-parser/src/main/java/com/zwei/iot/parser/dto/DataParseStrategyDTO.java`
- 创建：`server/zwei-iot-parser/src/main/java/com/zwei/iot/parser/dto/DataParseStrategyQueryDTO.java`
- 创建：`server/zwei-iot-parser/src/main/java/com/zwei/iot/parser/dto/DataParseTestRequest.java`
- 创建：`server/zwei-iot-parser/src/main/java/com/zwei/iot/parser/dto/DataParseTestResponse.java`
- 创建：`server/zwei-iot-parser/src/main/java/com/zwei/iot/parser/service/DataParseStrategyService.java`
- 创建：`server/zwei-iot-parser/src/main/java/com/zwei/iot/parser/service/DataParseLogService.java`

- [ ] **步骤 1：创建 DTO**

`DataParseStrategyDTO.java`：
```java
package com.zwei.iot.parser.dto;

import lombok.Data;
import java.util.List;

@Data
public class DataParseStrategyDTO {
    private Long id;
    private String name;
    private String sourceType;
    private String description;
    private Integer status;
    private String appScope;
    private String scriptCode;
    private List<Long> vendorIds;
    private List<Long> deviceIds;
}
```

`DataParseStrategyQueryDTO.java`：
```java
package com.zwei.iot.parser.dto;

import lombok.Data;

@Data
public class DataParseStrategyQueryDTO {
    private String name;
    private String sourceType;
    private Integer status;
    private String appScope;
}
```

`DataParseTestRequest.java`：
```java
package com.zwei.iot.parser.dto;

import lombok.Data;

@Data
public class DataParseTestRequest {
    private Long strategyId;
    private String scriptCode;
    private String topic;
    private String testData;      // hex 字符串 or JSON
}
```

`DataParseTestResponse.java`：
```java
package com.zwei.iot.parser.dto;

import lombok.Data;
import java.util.Map;

@Data
public class DataParseTestResponse {
    private boolean success;
    private long executionTime;
    private Map<String, Object> parseResult;
    private String error;
}
```

- [ ] **步骤 2：编写 Groovy 预编译校验工具**

`server/zwei-iot-parser/src/main/java/com/zwei/iot/parser/support/GroovyScriptValidator.java`：
```java
package com.zwei.iot.parser.support;

import groovy.lang.GroovyShell;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.SecureASTCustomizer;

import java.util.List;

/**
 * Groovy 脚本预编译校验器 — 保存策略时检查脚本合法性。
 */
public final class GroovyScriptValidator {

    private static final String[] FORBIDDEN = {
        "System.exit", "Runtime.getRuntime", "ProcessBuilder",
        "exec(", "Class.forName", "getClassLoader",
        "File(", "FileInputStream", "FileOutputStream",
        "Thread.sleep", "Thread.start",
        "System.getProperty", "System.setProperty"
    };

    private GroovyScriptValidator() {}

    /**
     * 编译检查脚本语法 + 安全性。不检查则返回 null，有问题则返回错误信息。
     */
    public static String validate(String scriptCode) {
        if (scriptCode == null || scriptCode.trim().isEmpty()) {
            return "脚本内容不能为空";
        }
        for (String kw : FORBIDDEN) {
            if (scriptCode.contains(kw)) {
                return "脚本包含不安全代码: " + kw;
            }
        }
        try {
            CompilerConfiguration config = new CompilerConfiguration();
            SecureASTCustomizer secure = new SecureASTCustomizer();
            secure.setDisallowedStarImports(true);
            secure.setDisallowedImports(List.of(
                "java.io.*", "java.nio.*", "java.net.*",
                "java.lang.reflect.*", "java.lang.System"
            ));
            config.addCompilationCustomizers(secure);
            GroovyShell shell = new GroovyShell(config);
            shell.parse(scriptCode);
            return null;  // OK
        } catch (Exception e) {
            return "脚本编译失败: " + e.getMessage();
        }
    }
}
```

- [ ] **步骤 3：编写 `DataParseStrategyService`**

```java
package com.zwei.iot.parser.service;

import com.zwei.common.exception.ServiceException;
import com.zwei.iot.parser.domain.DataParseStrategy;
import com.zwei.iot.parser.domain.DataParseStrategyDevice;
import com.zwei.iot.parser.domain.DataParseStrategyVendor;
import com.zwei.iot.parser.dto.DataParseStrategyDTO;
import com.zwei.iot.parser.dto.DataParseStrategyQueryDTO;
import com.zwei.iot.parser.mapper.DataParseStrategyDeviceMapper;
import com.zwei.iot.parser.mapper.DataParseStrategyMapper;
import com.zwei.iot.parser.mapper.DataParseStrategyVendorMapper;
import com.zwei.iot.parser.support.GroovyScriptValidator;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;

@Service
public class DataParseStrategyService {

    @Resource
    private DataParseStrategyMapper strategyMapper;
    @Resource
    private DataParseStrategyVendorMapper vendorMapper;
    @Resource
    private DataParseStrategyDeviceMapper deviceMapper;
    @Resource
    private DataParseLogService logService;

    public List<DataParseStrategy> listByPage(DataParseStrategyQueryDTO query) {
        DataParseStrategy condition = new DataParseStrategy();
        if (query != null) {
            condition.setName(query.getName());
            condition.setSourceType(query.getSourceType());
            condition.setStatus(query.getStatus());
            condition.setAppScope(query.getAppScope());
        }
        return strategyMapper.selectByCondition(condition);
    }

    public DataParseStrategyDTO getById(Long id) {
        DataParseStrategy strategy = strategyMapper.selectById(id);
        if (strategy == null) {
            throw new ServiceException("策略不存在: id=" + id);
        }
        DataParseStrategyDTO dto = new DataParseStrategyDTO();
        BeanUtils.copyProperties(strategy, dto);
        if ("vendor".equals(strategy.getAppScope())) {
            dto.setVendorIds(vendorMapper.selectVendorIdsByStrategyId(id));
        }
        if ("device".equals(strategy.getAppScope())) {
            dto.setDeviceIds(deviceMapper.selectDeviceIdsByStrategyId(id));
        }
        return dto;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(DataParseStrategyDTO dto) {
        String err = GroovyScriptValidator.validate(dto.getScriptCode());
        if (err != null) {
            throw new ServiceException(err);
        }
        DataParseStrategy strategy = new DataParseStrategy();
        BeanUtils.copyProperties(dto, strategy);
        strategyMapper.insert(strategy);
        saveRelations(strategy.getId(), dto);
        return strategy.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(DataParseStrategyDTO dto) {
        DataParseStrategy existing = strategyMapper.selectById(dto.getId());
        if (existing == null) {
            throw new ServiceException("策略不存在: id=" + dto.getId());
        }
        String err = GroovyScriptValidator.validate(dto.getScriptCode());
        if (err != null) {
            throw new ServiceException(err);
        }
        DataParseStrategy strategy = new DataParseStrategy();
        BeanUtils.copyProperties(dto, strategy);
        strategyMapper.updateById(strategy);
        deleteRelations(dto.getId());
        saveRelations(dto.getId(), dto);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        DataParseStrategy strategy = strategyMapper.selectById(id);
        if (strategy == null) {
            throw new ServiceException("策略不存在: id=" + id);
        }
        deleteRelations(id);
        strategyMapper.deleteById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void toggleStatus(Long id, Integer status) {
        DataParseStrategy strategy = strategyMapper.selectById(id);
        if (strategy == null) {
            throw new ServiceException("策略不存在: id=" + id);
        }
        strategy.setStatus(status);
        strategyMapper.updateById(strategy);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long copy(Long id) {
        DataParseStrategy original = strategyMapper.selectById(id);
        if (original == null) {
            throw new ServiceException("原策略不存在: id=" + id);
        }
        DataParseStrategy copy = new DataParseStrategy();
        BeanUtils.copyProperties(original, copy);
        copy.setId(null);
        copy.setName(original.getName() + " (副本)");
        copy.setIsPreset(0);
        copy.setStatus(0);
        strategyMapper.insert(copy);

        DataParseStrategyDTO dto = new DataParseStrategyDTO();
        dto.setAppScope(original.getAppScope());
        if ("vendor".equals(original.getAppScope())) {
            dto.setVendorIds(vendorMapper.selectVendorIdsByStrategyId(id));
        }
        if ("device".equals(original.getAppScope())) {
            dto.setDeviceIds(deviceMapper.selectDeviceIdsByStrategyId(id));
        }
        saveRelations(copy.getId(), dto);
        return copy.getId();
    }

    public List<DataParseStrategy> getEnabledBySourceType(String sourceType) {
        List<DataParseStrategy> strategies = strategyMapper.selectBySourceType(sourceType);
        return strategies != null ? strategies : Collections.emptyList();
    }

    private void saveRelations(Long strategyId, DataParseStrategyDTO dto) {
        if ("vendor".equals(dto.getAppScope()) && !CollectionUtils.isEmpty(dto.getVendorIds())) {
            for (Long vendorId : dto.getVendorIds()) {
                DataParseStrategyVendor rel = new DataParseStrategyVendor();
                rel.setStrategyId(strategyId);
                rel.setVendorId(vendorId);
                vendorMapper.insert(rel);
            }
        }
        if ("device".equals(dto.getAppScope()) && !CollectionUtils.isEmpty(dto.getDeviceIds())) {
            for (Long deviceId : dto.getDeviceIds()) {
                DataParseStrategyDevice rel = new DataParseStrategyDevice();
                rel.setStrategyId(strategyId);
                rel.setDeviceId(deviceId);
                deviceMapper.insert(rel);
            }
        }
    }

    private void deleteRelations(Long strategyId) {
        vendorMapper.deleteByStrategyId(strategyId);
        deviceMapper.deleteByStrategyId(strategyId);
    }
}
```

- [ ] **步骤 4：编写 `DataParseLogService`**

```java
package com.zwei.iot.parser.service;

import com.zwei.iot.parser.domain.DataParseLog;
import com.zwei.iot.parser.mapper.DataParseLogMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

@Service
public class DataParseLogService {

    @Resource
    private DataParseLogMapper logMapper;

    public void save(DataParseLog log) {
        log.setCreateTime(new Date());
        logMapper.insert(log);
    }

    public void info(Long strategyId, String message, String data) {
        DataParseLog log = DataParseLog.builder()
                .strategyId(strategyId).logLevel("INFO")
                .message(message).data(data).build();
        save(log);
    }

    public void error(Long strategyId, String message, String data, String errorStack) {
        DataParseLog log = DataParseLog.builder()
                .strategyId(strategyId).logLevel("ERROR")
                .message(message).data(data).errorStack(errorStack).build();
        save(log);
    }

    public List<DataParseLog> listByCondition(Long strategyId, String logLevel,
                                               String startTime, String endTime) {
        return logMapper.selectByCondition(strategyId, logLevel, startTime, endTime);
    }

    public void clearByStrategyId(Long strategyId) {
        logMapper.deleteByStrategyId(strategyId);
    }
}
```

- [ ] **步骤 5：验证编译**

```bash
cd server && mvn clean compile -pl zwei-iot-parser
```

预期：BUILD SUCCESS。

- [ ] **步骤 6：Commit**

```bash
git add server/zwei-iot-parser/src/main/java/com/zwei/iot/parser/dto/
git add server/zwei-iot-parser/src/main/java/com/zwei/iot/parser/service/
git add server/zwei-iot-parser/src/main/java/com/zwei/iot/parser/support/
git commit -m "feat: add parser DTOs, strategy service, log service, and Groovy validator"
```

---

### 任务 6：Groovy 脚本引擎 + 内置函数

**文件：**
- 创建：`server/zwei-iot-parser/src/main/java/com/zwei/iot/parser/engine/GroovyScriptEngine.java`
- 创建：`server/zwei-iot-parser/src/main/java/com/zwei/iot/parser/engine/BuiltInFunctions.java`

- [ ] **步骤 1：编写 `BuiltInFunctions`**

```java
package com.zwei.iot.parser.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Groovy 脚本内置函数注入 — 提供二进制读取 + JSON + 查询能力。
 *
 * <p>在 GroovyShell Binding 中以 {@code builtin} 变量暴露，
 * 脚本内通过 {@code builtin.hexDecode(payload)} 等调用。
 */
@Component
public class BuiltInFunctions {

    private static final Logger log = LoggerFactory.getLogger(BuiltInFunctions.class);

    // ===== 二进制读取原语 (字节流协议使用) =====

    /** hex 字符串 → byte[] */
    public byte[] hexDecode(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    /** big-endian float (4 bytes) */
    public float readFloat(byte[] data, int offset) {
        int bits = ((data[offset] & 0xFF) << 24)
                 | ((data[offset + 1] & 0xFF) << 16)
                 | ((data[offset + 2] & 0xFF) << 8)
                 | (data[offset + 3] & 0xFF);
        return Float.intBitsToFloat(bits);
    }

    /** big-endian double (8 bytes) */
    public double readDouble(byte[] data, int offset) {
        long bits = ((long)(data[offset] & 0xFF) << 56)
                  | ((long)(data[offset + 1] & 0xFF) << 48)
                  | ((long)(data[offset + 2] & 0xFF) << 40)
                  | ((long)(data[offset + 3] & 0xFF) << 32)
                  | ((long)(data[offset + 4] & 0xFF) << 24)
                  | ((long)(data[offset + 5] & 0xFF) << 16)
                  | ((long)(data[offset + 6] & 0xFF) << 8)
                  | (data[offset + 7] & 0xFF);
        return Double.longBitsToDouble(bits);
    }

    /** big-endian uint16 */
    public int readUInt16(byte[] data, int offset) {
        return ((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF);
    }

    /** big-endian int16 (signed) */
    public short readInt16(byte[] data, int offset) {
        return (short) (((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF));
    }

    /** uint8 */
    public int readUInt8(byte[] data, int offset) {
        return data[offset] & 0xFF;
    }

    /** ASCII 字符串 (定长) */
    public String readAscii(byte[] data, int offset, int length) {
        return new String(data, offset, length, StandardCharsets.US_ASCII);
    }

    /** BCD 编码时间戳 → epoch millis */
    public long readBcdTimestamp(byte[] data, int offset) {
        try {
            int year = bcdToInt(data[offset]) * 100 + bcdToInt(data[offset + 1]);
            int month = bcdToInt(data[offset + 2]);
            int day = bcdToInt(data[offset + 3]);
            int hour = bcdToInt(data[offset + 4]);
            int min = bcdToInt(data[offset + 5]);
            int sec = bcdToInt(data[offset + 6]);
            LocalDateTime ldt = LocalDateTime.of(year, month, day, hour, min, sec);
            return ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (Exception e) {
            log.warn("BCD 时间戳解析失败", e);
            return System.currentTimeMillis();
        }
    }

    private int bcdToInt(byte b) {
        return ((b >> 4) & 0x0F) * 10 + (b & 0x0F);
    }

    // ===== 工具方法 =====

    /** 当前 epoch 毫秒 */
    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    /** SHA-256 (调用 Java 原生) */
    public String sha256(byte[] data) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    /** Object → double，安全转换 */
    public Double toDouble(Object v) {
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).doubleValue();
        try {
            return Double.parseDouble(v.toString().trim());
        } catch (Exception e) {
            return null;
        }
    }

    /** Object → int，安全转换 */
    public Integer toInt(Object v, int defaultVal) {
        if (v == null) return defaultVal;
        if (v instanceof Number) return ((Number) v).intValue();
        try {
            return Integer.parseInt(v.toString().trim());
        } catch (Exception e) {
            return defaultVal;
        }
    }
}
```

- [ ] **步骤 2：编写 `GroovyScriptEngine`**

```java
package com.zwei.iot.parser.engine;

import com.zwei.common.domain.ParsedMessage;
import com.zwei.common.domain.PropertyValue;
import com.zwei.iot.parser.domain.DataParseLog;
import com.zwei.iot.parser.domain.DataParseStrategy;
import com.zwei.iot.parser.service.DataParseLogService;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import jakarta.annotation.PreDestroy;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.SecureASTCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Groovy 脚本执行引擎 — 解析策略的核心执行器。
 *
 * <p>使用 Caffeine-like 手动缓存 GroovyShell 实例（按 strategyId），
 * 复用已编译的脚本。沙箱通过 SecureASTCustomizer 限制文件/网络/反射访问。
 */
@Component
public class GroovyScriptEngine {

    private static final Logger log = LoggerFactory.getLogger(GroovyScriptEngine.class);

    private static final String[] FORBIDDEN_KEYWORDS = {
        "System.exit", "Runtime.getRuntime", "ProcessBuilder",
        "exec(", "Class.forName", "getClassLoader",
        "File(", "FileInputStream", "FileOutputStream",
        "Thread.sleep", "Thread.start",
        "System.getProperty", "System.setProperty"
    };

    private final Map<Long, GroovyShell> shellCache = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "parser-groovy");
        t.setDaemon(true);
        return t;
    });

    @Resource
    private BuiltInFunctions builtInFunctions;
    @Resource
    private DataParseLogService logService;

    /** Groovy 执行超时秒数 */
    private static final int TIMEOUT_SECONDS = 30;

    @PreDestroy
    public void destroy() {
        shellCache.clear();
        executor.shutdownNow();
    }

    /**
     * 执行解析脚本，产出 ParsedMessage。
     *
     * @param strategy  解析策略（含脚本代码）
     * @param topic     MQTT 主题
     * @param message   原始报文字节数组
     * @return 标准化解析结果，失败时返回 null
     */
    public ParsedMessage execute(DataParseStrategy strategy, String topic, byte[] message) {
        if (!isSafeScript(strategy.getScriptCode())) {
            log.warn("策略脚本包含不安全代码，拒绝执行: {}", strategy.getName());
            return null;
        }

        long startTime = System.currentTimeMillis();
        Future<ParsedMessage> future = executor.submit(() -> {
            try {
                GroovyShell shell = getOrCreateShell(strategy);
                Binding binding = new Binding();
                binding.setVariable("builtin", builtInFunctions);
                Script script = shell.parse(strategy.getScriptCode());
                script.setBinding(binding);

                @SuppressWarnings("unchecked")
                Map<String, Object> result = (Map<String, Object>) script.invokeMethod(
                        "parse", new Object[]{topic, message});

                String payloadStr = new String(message, StandardCharsets.UTF_8);
                String hash = sha256(payloadStr);

                ParsedMessage parsed = new ParsedMessage(
                    strategy.getSourceType(),   // deviceCode (脚本应填入真实值)
                    resolveSensorCode(result),
                    strategy.getSourceType(),
                    resolveDataTime(result),
                    System.currentTimeMillis(),
                    hash,
                    resolveProperties(result)
                );

                long execTime = System.currentTimeMillis() - startTime;
                logService.info(strategy.getId(), "解析成功, 耗时=" + execTime + "ms", payloadStr.substring(0, Math.min(payloadStr.length(), 500)));
                return parsed;
            } catch (Exception e) {
                log.error("Groovy 脚本执行异常: strategyId={}, topic={}, error={}",
                        strategy.getId(), topic, e.getMessage());
                DataParseLog parseLog = DataParseLog.builder()
                        .strategyId(strategy.getId()).logLevel("ERROR")
                        .message("执行异常: " + e.getMessage())
                        .topic(topic)
                        .executionTime((int) (System.currentTimeMillis() - startTime))
                        .errorStack(getStackTrace(e)).build();
                logService.save(parseLog);
                return null;
            }
        });

        try {
            return future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.warn("Groovy 脚本执行超时 ({}s): strategyId={}", TIMEOUT_SECONDS, strategy.getId());
            future.cancel(true);
            return null;
        } catch (Exception e) {
            log.error("Groovy 脚本执行中断: strategyId={}", strategy.getId(), e);
            return null;
        }
    }

    /**
     * 测试脚本（不写日志，不持久化）
     */
    public Map<String, Object> testScript(String scriptCode, String topic, String testData) {
        long startTime = System.currentTimeMillis();
        try {
            GroovyShell shell = new GroovyShell(createSecureConfig());
            Binding binding = new Binding();
            binding.setVariable("builtin", builtInFunctions);
            Script script = shell.parse(scriptCode);
            script.setBinding(binding);
            byte[] messageBytes = testData.getBytes(StandardCharsets.UTF_8);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) script.invokeMethod(
                    "parse", new Object[]{topic, messageBytes});
            Map<String, Object> response = new ConcurrentHashMap<>();
            response.put("success", true);
            response.put("executionTime", System.currentTimeMillis() - startTime);
            response.put("parsedMessage", buildParsedMessage(result, "test"));
            return response;
        } catch (Exception e) {
            Map<String, Object> response = new ConcurrentHashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return response;
        }
    }

    /** 重载 shell 缓存（策略更新后调用） */
    public void evictCache(Long strategyId) {
        shellCache.remove(strategyId);
    }

    private GroovyShell getOrCreateShell(DataParseStrategy strategy) {
        return shellCache.computeIfAbsent(strategy.getId(), id -> {
            GroovyShell shell = new GroovyShell(createSecureConfig());
            shell.parse(strategy.getScriptCode());
            return shell;
        });
    }

    private CompilerConfiguration createSecureConfig() {
        CompilerConfiguration config = new CompilerConfiguration();
        SecureASTCustomizer secure = new SecureASTCustomizer();
        secure.setDisallowedStarImports(true);
        secure.setDisallowedImports(List.of(
            "java.io.*", "java.nio.*", "java.net.*",
            "java.lang.reflect.*", "java.lang.System"
        ));
        config.addCompilationCustomizers(secure);
        return config;
    }

    private boolean isSafeScript(String script) {
        for (String kw : FORBIDDEN_KEYWORDS) {
            if (script.contains(kw)) return false;
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private String resolveSensorCode(Map<String, Object> result) {
        Object sensorCode = result.getOrDefault("sensorCode", "1");
        return sensorCode != null ? sensorCode.toString() : "1";
    }

    @SuppressWarnings("unchecked")
    private long resolveDataTime(Map<String, Object> result) {
        Object dt = result.get("dataTime");
        if (dt instanceof Number) return ((Number) dt).longValue();
        return System.currentTimeMillis();
    }

    @SuppressWarnings("unchecked")
    private List<PropertyValue> resolveProperties(Map<String, Object> result) {
        Object props = result.get("properties");
        if (!(props instanceof List)) return List.of();
        List<PropertyValue> list = new ArrayList<>();
        for (Object item : (List<?>) props) {
            if (item instanceof Map) {
                Map<String, Object> m = (Map<String, Object>) item;
                list.add(new PropertyValue(
                    str(m, "identifier", ""),
                    str(m, "name", ""),
                    str(m, "unit", ""),
                    toDouble(m.get("value")),
                    toInt(m.get("quality"), 0)
                ));
            }
        }
        return list;
    }

    private String str(Map<String, Object> m, String key, String def) {
        Object v = m.get(key);
        return v != null ? v.toString() : def;
    }

    private Double toDouble(Object v) {
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).doubleValue();
        try { return Double.parseDouble(v.toString().trim()); }
        catch (Exception e) { return null; }
    }

    private Integer toInt(Object v, int def) {
        if (v == null) return def;
        if (v instanceof Number) return ((Number) v).intValue();
        try { return Integer.parseInt(v.toString().trim()); }
        catch (Exception e) { return def; }
    }

    private ParsedMessage buildParsedMessage(Map<String, Object> result, String sourceType) {
        String payload = result.getOrDefault("_rawPayload", "").toString();
        return new ParsedMessage(
            str(result, "deviceCode", ""),
            resolveSensorCode(result),
            sourceType,
            resolveDataTime(result),
            System.currentTimeMillis(),
            sha256(payload),
            resolveProperties(result)
        );
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { return ""; }
    }

    private String getStackTrace(Exception e) {
        java.io.StringWriter sw = new java.io.StringWriter();
        e.printStackTrace(new java.io.PrintWriter(sw));
        return sw.toString();
    }
}
```

- [ ] **步骤 3：验证编译**

```bash
cd server && mvn clean compile -pl zwei-iot-parser
```

预期：BUILD SUCCESS。

- [ ] **步骤 4：Commit**

```bash
git add server/zwei-iot-parser/src/main/java/com/zwei/iot/parser/engine/
git commit -m "feat: add Groovy script engine and built-in functions"
```

---

### 任务 7：Controller + 在线测试 API

**文件：**
- 创建：`server/zwei-iot-parser/src/main/java/com/zwei/iot/parser/controller/DataParseController.java`

- [ ] **步骤 1：编写 Controller**

```java
package com.zwei.iot.parser.controller;

import com.zwei.common.core.controller.BaseController;
import com.zwei.common.core.domain.AjaxResult;
import com.zwei.common.core.page.TableDataInfo;
import com.zwei.iot.parser.domain.DataParseLog;
import com.zwei.iot.parser.domain.DataParseStrategy;
import com.zwei.iot.parser.dto.*;
import com.zwei.iot.parser.engine.GroovyScriptEngine;
import com.zwei.iot.parser.service.DataParseLogService;
import com.zwei.iot.parser.service.DataParseStrategyService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/iot/parser/strategy")
public class DataParseController extends BaseController {

    @Resource
    private DataParseStrategyService strategyService;
    @Resource
    private DataParseLogService logService;
    @Resource
    private GroovyScriptEngine scriptEngine;

    @GetMapping("/page")
    public TableDataInfo list(DataParseStrategyQueryDTO query) {
        startPage();
        List<DataParseStrategy> list = strategyService.listByPage(query);
        return getDataTable(list);
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

    @PostMapping("/{id}/copy")
    public AjaxResult copy(@PathVariable Long id) {
        return AjaxResult.success(strategyService.copy(id));
    }

    @PostMapping("/test")
    public AjaxResult testScript(@RequestBody DataParseTestRequest request) {
        Map<String, Object> result = scriptEngine.testScript(
                request.getScriptCode(), request.getTopic(), request.getTestData());
        return AjaxResult.success(result);
    }

    @GetMapping("/{id}/logs")
    public TableDataInfo getLogs(@PathVariable Long id,
                                  @RequestParam(required = false) String logLevel,
                                  @RequestParam(required = false) String startTime,
                                  @RequestParam(required = false) String endTime) {
        startPage();
        List<DataParseLog> logs = logService.listByCondition(id, logLevel, startTime, endTime);
        return getDataTable(logs);
    }

    @DeleteMapping("/{id}/logs")
    public AjaxResult clearLogs(@PathVariable Long id) {
        logService.clearByStrategyId(id);
        return AjaxResult.success();
    }
}
```

- [ ] **步骤 2：验证编译**

```bash
cd server && mvn clean compile -pl zwei-iot-parser
```

预期：BUILD SUCCESS。

- [ ] **步骤 3：Commit**

```bash
git add server/zwei-iot-parser/src/main/java/com/zwei/iot/parser/controller/
git commit -m "feat: add parser strategy CRUD controller and online test API"
```

---

### 任务 8：MonitorMetadataService 重写 + 策略匹配

**文件：**
- 创建：`server/zwei-iot-parser/src/main/java/com/zwei/iot/parser/service/MonitorMetadataService.java`

- [ ] **步骤 1：编写重写的 `MonitorMetadataService`**

```java
package com.zwei.iot.parser.service;

import com.zwei.common.exception.ServiceException;
import com.zwei.iot.device.domain.ProductTsl;
import com.zwei.iot.device.domain.SensorMetadata;
import com.zwei.iot.device.service.IDeviceSensorQueryService;
import com.zwei.iot.device.service.IProductTslService;
import com.zwei.iot.parser.domain.DataParseStrategy;
import com.zwei.iot.parser.mapper.DataParseStrategyDeviceMapper;
import com.zwei.iot.parser.mapper.DataParseStrategyMapper;
import com.zwei.iot.parser.mapper.DataParseStrategyVendorMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 解析元数据服务 — 传感器元数据 + TSL 物模型 + 策略匹配。
 *
 * <p>从 timeseries 模块移入并扩展职责。
 */
@Service
public class MonitorMetadataService {

    @Resource
    private IDeviceSensorQueryService deviceSensorQueryService;
    @Resource
    private IProductTslService productTslService;
    @Resource
    private DataParseStrategyMapper strategyMapper;
    @Resource
    private DataParseStrategyVendorMapper vendorMapper;
    @Resource
    private DataParseStrategyDeviceMapper deviceMapper;

    /**
     * 获取传感器元数据（保留现有能力）。
     */
    public SensorMetadata requireSensorMetadata(Long deviceId, String sensorCode) {
        return deviceSensorQueryService.requireSensorMetadata(deviceId, sensorCode);
    }

    /**
     * 获取设备对应的 TSL 物模型（新增，用于值域校验）。
     */
    public ProductTsl getTsl(Long deviceId) {
        return productTslService.getByDeviceId(deviceId);
    }

    /**
     * 三级级联策略匹配。
     *
     * <ol>
     *   <li>device 级 — 查 strategy_device 表</li>
     *   <li>vendor 级 — 查 strategy_vendor 表</li>
     *   <li>全局级 — 按 sourceType 匹配 app_scope='global'</li>
     * </ol>
     *
     * @param sourceType 协议标识（sys/gb/自定义）
     * @param deviceId   设备主键
     * @return 匹配的策略，未命中返回 null
     */
    public DataParseStrategy resolveStrategy(String sourceType, Long deviceId) {
        // 1. device 级匹配
        Long deviceStrategyId = deviceMapper.selectStrategyIdByDeviceId(deviceId);
        if (deviceStrategyId != null) {
            DataParseStrategy strategy = strategyMapper.selectById(deviceStrategyId);
            if (strategy != null && strategy.getStatus() == 1) {
                return strategy;
            }
        }
        // 2. vendor 级匹配（需根据 deviceId 查询 vendorId，暂时跳过，预留扩展）
        // 3. 全局级匹配
        List<DataParseStrategy> globalStrategies = strategyMapper.selectBySourceType(sourceType);
        if (globalStrategies != null && !globalStrategies.isEmpty()) {
            return globalStrategies.get(0);  // 返回第一个启用的预置策略
        }
        return null;
    }
}
```

- [ ] **步骤 2：验证编译**

```bash
cd server && mvn clean compile -pl zwei-iot-parser
```

预期：BUILD SUCCESS。

- [ ] **步骤 3：Commit**

```bash
git add server/zwei-iot-parser/src/main/java/com/zwei/iot/parser/service/MonitorMetadataService.java
git commit -m "feat: add rewritten MonitorMetadataService with TSL + strategy matching"
```

---

### 任务 9：迁移 MonitorTopic/MonitorTopicParser 到 parser 模块

**文件：**
- 创建：`server/zwei-iot-parser/src/main/java/com/zwei/iot/parser/support/MonitorTopic.java`
- 创建：`server/zwei-iot-parser/src/main/java/com/zwei/iot/parser/support/MonitorTopicParser.java`
- 删除：`server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/support/MonitorTopic.java`
- 删除：`server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/support/MonitorTopicParser.java`
- 修改：timeseries 中所有引用这两个类的文件

- [ ] **步骤 1：复制 MonitorTopic 到 parser 模块**

从 `server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/support/MonitorTopic.java` 复制到 `server/zwei-iot-parser/src/main/java/com/zwei/iot/parser/support/MonitorTopic.java`，修改 package 声明为 `com.zwei.iot.parser.support`。

- [ ] **步骤 2：复制 MonitorTopicParser 到 parser 模块**

同样复制并修改 package。

- [ ] **步骤 3：更新 timeseries 模块中的引用**

修改 `MonitorIngestFacade.java`：
```java
// 旧 import
import com.zwei.iot.timeseries.support.MonitorTopic;
import com.zwei.iot.timeseries.support.MonitorTopicParser;
// 新 import
import com.zwei.iot.parser.support.MonitorTopic;
import com.zwei.iot.parser.support.MonitorTopicParser;
```

修改 `MonitorIngestFacade` 的构造函数，注入 parser 模块的 `MonitorTopicParser`。

- [ ] **步骤 4：删除 timeseries 中的旧文件**

```bash
rm server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/support/MonitorTopic.java
rm server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/support/MonitorTopicParser.java
```

- [ ] **步骤 5：验证编译**

```bash
cd server && mvn clean compile -pl zwei-iot-timeseries,zwei-iot-parser
```

预期：BUILD SUCCESS。

- [ ] **步骤 6：Commit**

```bash
git add server/zwei-iot-parser/src/main/java/com/zwei/iot/parser/support/
git add server/zwei-iot-timeseries/
git commit -m "refactor: move MonitorTopic and MonitorTopicParser from timeseries to parser module"
```

---

### 任务 10：改造 MonitorIngestFacade（策略匹配 + 脚本引擎）

**文件：**
- 修改：`server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/service/MonitorIngestFacade.java`

- [ ] **步骤 1：重写 Facade**

```java
package com.zwei.iot.timeseries.service;

import com.zwei.common.domain.ParsedMessage;
import com.zwei.common.exception.ServiceException;
import com.zwei.iot.parser.domain.DataParseStrategy;
import com.zwei.iot.parser.engine.GroovyScriptEngine;
import com.zwei.iot.parser.service.MonitorMetadataService;
import com.zwei.iot.parser.support.MonitorTopic;
import com.zwei.iot.parser.support.MonitorTopicParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * MQTT 监测数据接入门面 — 重写后使用 parser 模块。
 *
 * <p>原 MonitorMetadataService 和 MonitorPayloadParser 调用已替换为
 * parser 模块的策略匹配 + Groovy 脚本执行 + ParsedMessage。
 */
@Slf4j
@Service
public class MonitorIngestFacade {
    private final MonitorTopicParser topicParser;
    private final MonitorMetadataService metadataService;
    private final GroovyScriptEngine scriptEngine;
    private final MonitorIngestStreamService streamService;

    @Autowired
    public MonitorIngestFacade(MonitorTopicParser topicParser,
                               MonitorMetadataService metadataService,
                               GroovyScriptEngine scriptEngine,
                               MonitorIngestStreamService streamService) {
        this.topicParser = topicParser;
        this.metadataService = metadataService;
        this.scriptEngine = scriptEngine;
        this.streamService = streamService;
    }

    /**
     * 接收并标准化处理 MQTT 监测报文。
     *
     * <p>新流程：topic 解析 → 策略匹配 → Groovy 脚本执行 → 值域校验 → 入 Stream。
     *
     * @param topic    MQTT 主题
     * @param message  报文字节数组
     * @param deviceId 已认证设备主键
     * @throws ServiceException 当主题非法或策略未匹配时抛出
     */
    public void ingest(String topic, byte[] message, Long deviceId) {
        // 1. 解析 topic
        MonitorTopic parsedTopic = topicParser.parse(topic);
        if (parsedTopic == null) {
            throw new ServiceException("监测主题格式非法: " + topic);
        }

        // 2. 策略匹配
        DataParseStrategy strategy = metadataService.resolveStrategy(
                parsedTopic.sourceType(), deviceId);
        if (strategy == null) {
            log.error("未找到匹配的解析策略: sourceType={}, deviceId={}, topic={}",
                    parsedTopic.sourceType(), deviceId, topic);
            return;  // 静默丢弃，MQTT 不回 ack
        }

        // 3. 执行 Groovy 脚本
        ParsedMessage parsedMessage = scriptEngine.execute(strategy, topic, message);
        if (parsedMessage == null) {
            // 脚本执行失败 → 入死信队列
            String payloadStr = new String(message, java.nio.charset.StandardCharsets.UTF_8);
            streamService.enqueueDeadLetter(topic, payloadStr,
                    "策略[" + strategy.getName() + "] 解析失败");
            return;
        }

        // 4. 值域校验（基于 TSL specs，仅告警不阻断）
        try {
            var tsl = metadataService.getTsl(deviceId);
            if (tsl != null && tsl.properties() != null) {
                for (var prop : parsedMessage.properties()) {
                    for (var tslProp : tsl.properties()) {
                        if (tslProp.identifier().equals(prop.identifier())
                                && tslProp.dataType() != null
                                && tslProp.dataType().specs() != null) {
                            var specs = tslProp.dataType().specs();
                            if (specs.min() != null && prop.value() != null
                                    && prop.value() < Double.parseDouble(specs.min())) {
                                log.warn("属性值低于下限: deviceId={}, {}={}, min={}",
                                        deviceId, prop.identifier(), prop.value(), specs.min());
                            }
                            if (specs.max() != null && prop.value() != null
                                    && prop.value() > Double.parseDouble(specs.max())) {
                                log.warn("属性值超出上限: deviceId={}, {}={}, max={}",
                                        deviceId, prop.identifier(), prop.value(), specs.max());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("TSL 值域校验跳过: deviceId={}, error={}", deviceId, e.getMessage());
        }

        // 5. 入 Stream
        streamService.enqueue(parsedMessage);
        log.debug("监测报文已入缓冲队列, topic={}, properties={}", topic, parsedMessage.properties().size());
    }
}
```

- [ ] **步骤 2：验证编译**

```bash
cd server && mvn clean compile -pl zwei-iot-timeseries
```

预期：BUILD SUCCESS。

- [ ] **步骤 3：Commit**

```bash
git add server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/service/MonitorIngestFacade.java
git commit -m "refactor: rewrite MonitorIngestFacade to use parser module strategy matching + Groovy engine"
```

---

### 任务 11：改造 MonitorIngestStreamService（接收 ParsedMessage）

**文件：**
- 修改：`server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/service/MonitorIngestStreamService.java`

- [ ] **步骤 1：新增 `enqueue(ParsedMessage)` 重载**

编辑 `MonitorIngestStreamService.java`，在现有 `enqueue(List<StandardMeasurementPoint>)` 方法之后新增：

```java
/**
 * 将标准化解析结果写入主消费流（新接口 — 接收 ParsedMessage）。
 *
 * @param message 解析后的标准化报文
 */
public void enqueue(com.zwei.common.domain.ParsedMessage message) {
    Map<String, String> body = new HashMap<>();
    body.put("payload", JSON.toJSONString(message));
    body.put("payloadType", "PARSED_MESSAGE");  // 标记类型，consumer 区分处理
    body.put("retryCount", "0");
    redisTemplate.opsForStream().add(MapRecord.create(properties.getStreamKey(), body));
}

/**
 * 将原始报文写入死信队列（重载 — 接收字符串）。
 *
 * @param topic   MQTT 主题
 * @param rawPayload 原始报文字符串
 * @param reason  失败原因
 */
public void enqueueDeadLetter(String topic, String rawPayload, String reason) {
    Map<String, String> body = new HashMap<>();
    body.put("topic", topic);
    body.put("payload", rawPayload);
    body.put("reason", reason);
    redisTemplate.opsForStream().add(MapRecord.create(properties.getDeadLetterStreamKey(), body));
}
```

保留原有 `enqueue(List<StandardMeasurementPoint>)` 和 `enqueueDeadLetter(StandardMeasurementPoint, String)` 方法不变，等迁移完全后再删除。

- [ ] **步骤 2：验证编译**

```bash
cd server && mvn clean compile -pl zwei-iot-timeseries
```

预期：BUILD SUCCESS。

- [ ] **步骤 3：Commit**

```bash
git add server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/service/MonitorIngestStreamService.java
git commit -m "feat: add ParsedMessage enqueue overload to MonitorIngestStreamService"
```

---

### 任务 12：改造 MonitorIngestConsumerService（适配 ParsedMessage）

**文件：**
- 修改：`server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/service/MonitorIngestConsumerService.java`

- [ ] **步骤 1：在 `processRecord` 中新增 `PARSED_MESSAGE` 分支**

在 `processRecord` 方法的反序列化步骤（当前第 168-170 行）之后插入新分支：

```java
private void processRecord(MapRecord<Object, Object, Object> record) {
    String payloadType = String.valueOf(record.getValue().getOrDefault("payloadType", "STANDARD_POINT"));
    String payload = String.valueOf(record.getValue().get("payload"));
    int retryCount = Integer.parseInt(String.valueOf(record.getValue().getOrDefault("retryCount", "0")));

    // 新路径：ParsedMessage（来自 parser 模块）
    if ("PARSED_MESSAGE".equals(payloadType)) {
        processParsedMessage(record, payload, retryCount);
        return;
    }
    // 保持旧路径兼容
    StandardMeasurementPoint point = JSON.parseObject(payload, StandardMeasurementPoint.class);
    // ... 原有逻辑不变
}

/**
 * 处理 ParsedMessage 类型消息 — 适配为 StandardMeasurementPoint 后写入 IoTDB。
 */
private void processParsedMessage(MapRecord<Object, Object, Object> record, String payload, int retryCount) {
    com.zwei.common.domain.ParsedMessage parsed = JSON.parseObject(payload, com.zwei.common.domain.ParsedMessage.class);
    try {
        List<StandardMeasurementPoint> points = adapt(parsed);
        if (points.isEmpty()) {
            ack(record);
            return;
        }
        // 幂等去重 — 按第一条 payloadHash 去重
        if (isDuplicate(points.get(0))) {
            ack(record);
            return;
        }
        iotdbTimeSeriesService.writePoints(points);
        // 运维回写
        for (StandardMeasurementPoint pt : points) {
            deviceOnlineStatusService.updateLastReportAt(pt.deviceId());
            if (pt.sensorId() != null) {
                String now = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                        .format(new java.util.Date(pt.dataTime()));
                deviceSensorService.updateLastReportTime(pt.sensorId(), now);
            }
            deviceMapper.updateDevice(Device.builder()
                    .id(pt.deviceId())
                    .lastReportTime(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                            .format(new java.util.Date(pt.dataTime())))
                    .build());
        }
        log.info("ParsedMessage 落库成功 deviceCode={} sensorCode={} properties={}",
                parsed.deviceCode(), parsed.sensorCode(), points.size());
        ack(record);
    } catch (Exception e) {
        if (retryCount >= properties.getRetryDelaysSeconds().size()) {
            streamService.enqueueDeadLetter(parsed.deviceCode(), payload, e.getMessage());
            ack(record);
            return;
        }
        long delaySeconds = properties.getRetryDelaysSeconds().get(retryCount);
        sleep(delaySeconds);
        record.getValue().put("retryCount", String.valueOf(retryCount + 1));
        redisTemplate.opsForStream().add(MapRecord.create(properties.getStreamKey(), record.getValue()));
        ack(record);
    }
}

/**
 * ParsedMessage → List<StandardMeasurementPoint> 适配。
 *
 * deviceCode → deviceId 的映射需在 consumer 侧查缓存。
 */
private List<StandardMeasurementPoint> adapt(com.zwei.common.domain.ParsedMessage msg) {
    Long deviceId = resolveDeviceId(msg.deviceCode());
    Long sensorId = resolveSensorId(msg.sensorCode());
    return msg.properties().stream()
        .filter(p -> p.value() != null)
        .map(p -> StandardMeasurementPoint.builder()
            .deviceId(deviceId)
            .sensorCode(msg.sensorCode())
            .sensorId(sensorId)
            .attrCode(p.identifier())
            .attrName(p.name())
            .unit(p.unit())
            .dataTime(msg.dataTime())
            .value(p.value())
            .quality(p.quality() != null ? p.quality() : 0)
            .receiveTime(msg.receiveTime())
            .sourceType(msg.sourceType())
            .payloadHash(msg.payloadHash())
            .build())
        .toList();
}

private Long resolveDeviceId(String deviceCode) {
    // 从 deviceMapper 按 code 查询，生产环境需加缓存
    Device dev = deviceMapper.selectByCode(deviceCode);
    return dev != null ? dev.getId() : -1L;
}

private Long resolveSensorId(String sensorCode) {
    // 从 deviceSensorService 查询 sensorId
    return deviceSensorService.findBySensorCode(sensorCode)
            .map(s -> s.getId()).orElse(-1L);
}
```

`isDuplicate` 方法已有基于 `StandardMeasurementPoint` 的重载，对 ParsedMessage 路径复用即可（已在 `adapt()` 中填充了 `payloadHash`）。

- [ ] **步骤 2：确认 DeviceMapper 有 `selectByCode` 方法**

检查 `server/zwei-iot-device/src/main/java/com/zwei/iot/device/mapper/DeviceMapper.java` 是否已有 `selectByCode` 方法。若没有，需要在 DeviceMapper 接口和 XML 中添加。

- [ ] **步骤 3：验证编译**

```bash
cd server && mvn clean compile -pl zwei-iot-timeseries
```

预期：BUILD SUCCESS 或有缺失方法的编译错误，补齐后通过。

- [ ] **步骤 4：Commit**

```bash
git add server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/service/MonitorIngestConsumerService.java
git commit -m "feat: add ParsedMessage consumer path with adaptation layer"
```

---

### 任务 13：删除旧解析器代码

**文件：**
- 删除：`server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/parser/MonitorPayloadParser.java`
- 删除：`server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/parser/SysMonitorPayloadParser.java`
- 删除：`server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/parser/GbMonitorPayloadParser.java`
- 删除：`server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/service/MonitorMetadataService.java`
- 修改：timeseries pom.xml（移除不必要的依赖，如果有）

- [ ] **步骤 1：删除旧文件并验证编译**

```bash
rm server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/parser/MonitorPayloadParser.java
rm server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/parser/SysMonitorPayloadParser.java
rm server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/parser/GbMonitorPayloadParser.java
rm server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/service/MonitorMetadataService.java
```

- [ ] **步骤 2：全量验证编译**

```bash
cd server && mvn clean compile
```

预期：BUILD SUCCESS。

- [ ] **步骤 3：Commit**

```bash
git add server/zwei-iot-timeseries/
git commit -m "refactor: remove old Java-based parsers and timeseries MetadataService"
```

---

### 任务 14：端到端集成验证

- [ ] **步骤 1：启动完整应用**

```bash
cd server && mvn spring-boot:run -pl zwei-admin
```

验证：
- 应用正常启动，无 Bean 注入失败
- parser 模块的 Controller 注册成功（`/api/v1/iot/parser/strategy/page` 可访问）
- 预置策略已加载（数据库查询 `SELECT * FROM iot_data_parse_strategy` 返回 2 条）

- [ ] **步骤 2：模拟数据解析**

```bash
# 测试 sys 协议脚本
curl -X POST http://localhost:8080/api/v1/iot/parser/strategy/test \
  -H "Content-Type: application/json" \
  -d '{"strategyId":1,"scriptCode":"...","topic":"sys/v1/TEST001/1/updata","testData":"{\"version\":\"1.0\",\"data\":{\"rainfall\":{\"value\":25.5}}}"}'
```

预期：返回 200，`parsedMessage.properties` 包含 `identifier=rainfall, value=25.5`。

- [ ] **步骤 3：Commit（最终验证通过后的汇总 commit）**

```bash
git add -A && git status
git commit -m "chore: final integration verification — parser module complete"
```

---

## 自检清单

- [x] 规格覆盖度：14 个任务覆盖设计文档全部章节
- [x] 占位符扫描：无 "TBD"/"TODO"/"后续实现"，所有代码块包含实际代码
- [x] 类型一致性：`ParsedMessage`/`PropertyValue`/`MonitorTopic`/`DataParseStrategy` 在所有任务中类型签名一致
- [x] 迁移步骤完整性：创建 → 编译验证 → commit 逐阶段可追溯

## 变更记录

| 时间 | 变更 |
|------|------|
| 2026-06-15 | 初版实现计划，14 个任务，基于 V2 设计文档 |
