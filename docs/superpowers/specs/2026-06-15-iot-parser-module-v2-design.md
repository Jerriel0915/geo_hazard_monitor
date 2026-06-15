# IoT 数据解析模块 V2 设计文档

> 基于 2026-06-11 初版设计讨论的演进方案，对齐现有 timeseries 架构的 Redis Stream 缓冲层。

## 一、概述

### 1.1 背景

当前系统解析链路紧耦合在 `zwei-iot-timeseries` 模块中：

- `SysMonitorPayloadParser`（570 行）和 `GbMonitorPayloadParser`（占位）实现 `MonitorPayloadParser` 接口
- 解析器产出 `StandardMeasurementPoint`（IoTDB 写入格式），无独立中间格式
- 新增厂商协议需修改 timeseries 模块并编写 Java 代码
- 无脚本化扩展能力，无法热更新解析逻辑

### 1.2 目标

将解析层抽离为独立 Maven 模块 `zwei-iot-parser`，引入 Groovy 脚本引擎，产出 TSL 对齐的标准化中间格式 `ParsedMessage`，通过 Redis Stream 与下游解耦。

### 1.3 核心决策

| 决策项 | 选择 | 理由 |
|--------|------|------|
| 模块定位 | 独立 Maven 模块 | 最大化解耦，可独立测试部署 |
| 架构关系 | 替代式 | 删除 `MonitorPayloadParser` 接口，所有协议统一为 Groovy 脚本 |
| 脚本引擎 | Groovy (`@CompileStatic`) | alarm 模块已用，运维熟悉；语法接近 Java；调用 Java 零开销 |
| 中间格式 | `ParsedMessage` + `PropertyValue` record | 对齐 TSL properties 语义，下沉至 common |
| MQTT 接入 | 保持现状 | `MonitorIngestFacade` 仍为入口，内部调用 parser 模块 |

## 二、模块架构

### 2.1 模块依赖

```
zwei-iot-parser (新)
  ├─ zwei-common       → ParsedMessage/PropertyValue 定义
  ├─ zwei-iot-device   → SensorMetadata / ProductTsl / IDeviceSensorQueryService
  ├─ Groovy (已存在)    → 脚本执行引擎
  └─ Redis (已存在)     → Stream 写入

zwei-iot-timeseries
  ├─ zwei-iot-parser   → ScriptExecutionService / MonitorMetadataService
  └─ 自身: MonitorIngestFacade / MonitorIngestStreamService / Consumer
```

### 2.2 包结构

```
zwei-iot-parser/
├── pom.xml
└── src/main/java/com/zwei/iot/parser/
    ├── controller/
    │   └── DataParseController.java
    ├── service/
    │   ├── DataParseStrategyService.java
    │   ├── DataParseLogService.java
    │   ├── MonitorMetadataService.java
    │   └── ScriptExecutionService.java
    ├── engine/
    │   ├── GroovyScriptEngine.java
    │   └── BuiltInFunctions.java
    ├── domain/
    │   ├── DataParseStrategy.java
    │   ├── DataParseStrategyVendor.java
    │   ├── DataParseStrategyDevice.java
    │   └── DataParseLog.java
    ├── dto/
    │   ├── DataParseStrategyDTO.java
    │   ├── DataParseStrategyQueryDTO.java
    │   ├── DataParseTestRequest.java
    │   └── DataParseTestResponse.java
    ├── mapper/
    │   ├── DataParseStrategyMapper.java
    │   ├── DataParseStrategyVendorMapper.java
    │   ├── DataParseStrategyDeviceMapper.java
    │   └── DataParseLogMapper.java
    └── config/
        └── DataParseAutoConfiguration.java
```

### 2.3 中间格式（下沉至 zwei-common）

```java
// com.zwei.common.domain.ParsedMessage
public record ParsedMessage(
    String deviceCode,          // device.code
    String sensorCode,          // sensor.sensorCode
    String sourceType,          // "sys" | "gb" | 自定义
    long dataTime,              // 采集时间 epoch ms
    long receiveTime,           // 服务端接收时间
    String payloadHash,         // SHA-256
    List<PropertyValue> properties
) implements Serializable {}

// com.zwei.common.domain.PropertyValue
public record PropertyValue(
    String identifier,   // TslProperty.identifier
    String name,         // TslProperty.name
    String unit,         // TslProperty.dataType.specs.unit
    Double value,        // 运行时数值
    Integer quality      // 质量码
) implements Serializable {}
```

### 2.4 模型归属汇总

| 模型 | 归属模块 | 说明 |
|------|---------|------|
| `ParsedMessage` | zwei-common | 跨模块流契约 |
| `PropertyValue` | zwei-common | ParsedMessage 组成部分 |
| `SensorMetadata` | zwei-iot-device | 不变，device 模块聚合查询 |
| `ProductTsl` 系列 | zwei-iot-device | 不变，TSL 定义层 |
| `StandardMeasurementPoint` | zwei-iot-timeseries | 不变，IoTDB 内部格式 |
| `MonitorTopic` | zwei-iot-parser | 从 timeseries 移入 |
| `MonitorTopicParser` | zwei-iot-parser | 从 timeseries 移入 |

## 三、数据流

### 3.1 完整链路

```
MQTT raw message
  │
  ▼
MqttServerMessageListener (zwei-iot-broker)
  │ deviceId from auth session
  ▼
MonitorIngestFacade.ingest(topic, message, deviceId)  [timeseries]
  │
  ├─ MonitorTopicParser.parse(topic) → MonitorTopic  [parser]
  │
  ├─ MonitorMetadataService.resolveStrategy(sourceType, deviceId)  [parser]
  │     ├─ 1st: strategy_device (device 级)
  │     ├─ 2nd: strategy_vendor (vendor 级)
  │     └─ 3rd: sourceType 全局匹配
  │
  ├─ ScriptExecutionService.execute(strategy, topic, message)  [parser]
  │     ├─ GroovyShell + @CompileStatic
  │     ├─ builtin.* 函数注入 (二进制读取/数据查询)
  │     └─ 产出 ParsedMessage
  │
  ├─ Validator.validate(message, tsl)  [parser]
  │     └─ 基于 TSL property specs 的值域校验，不阻断
  │
  └─ MonitorIngestStreamService.enqueue(parsedMessage)  [timeseries]
       │  JSON 序列化后写入 Redis Stream
       ▼  (异步边界)
MonitorIngestConsumerService  [timeseries]
  ├─ 读取 Stream 中的 ParsedMessage JSON
  ├─ adapt(ParsedMessage) → List<StandardMeasurementPoint>
  ├─ 幂等去重
  └─ IotdbTimeSeriesService.writePoints() → IoTDB
```

### 3.2 timeseries 侧适配层

```java
// MonitorIngestConsumerService
private List<StandardMeasurementPoint> adapt(ParsedMessage msg) {
    return msg.properties().stream()
        .map(p -> StandardMeasurementPoint.builder()
            .deviceId(resolveDeviceId(msg.deviceCode()))
            .sensorCode(msg.sensorCode())
            .attrCode(p.identifier())
            .attrName(p.name())
            .unit(p.unit())
            .dataTime(msg.dataTime())
            .value(p.value())
            .quality(p.quality())
            .receiveTime(msg.receiveTime())
            .sourceType(msg.sourceType())
            .payloadHash(msg.payloadHash())
            .build())
        .toList();
}
```

## 四、Groovy 脚本引擎

### 4.1 上下文管理

```java
@Component
public class GroovyScriptEngine {
    private final Map<Long, GroovyShell> shellCache = new ConcurrentHashMap<>();

    public ParsedMessage execute(DataParseStrategy strategy, String topic, byte[] message) {
        GroovyShell shell = shellCache.computeIfAbsent(strategy.getId(), id -> {
            CompilerConfiguration config = new CompilerConfiguration();
            config.setScriptBaseClass(ParseScript.class.getName());
            // 沙箱：禁止反射、文件IO、系统调用
            SecureASTCustomizer secure = new SecureASTCustomizer();
            secure.setDisallowedStarImports(true);
            secure.setDisallowedImports(List.of(
                "java.io.*", "java.nio.*", "java.net.*",
                "java.lang.reflect.*", "java.lang.System"
            ));
            config.addCompilationCustomizers(secure);

            GroovyClassLoader classLoader = new GroovyClassLoader();
            return new GroovyShell(classLoader, new Binding(), config);
        });
        // 注入内置函数
        Binding binding = new Binding();
        binding.setVariable("builtin", builtInFunctions);
        binding.setVariable("log", loggerFunctions);
        shell.setBinding(binding);

        Script script = shell.parse(strategy.getScriptCode());
        Object result = script.invokeMethod("parse", new Object[]{topic, new String(message)});
        return mapToParsedMessage(result, strategy.getSourceType());
    }
}
```

### 4.2 内置函数

```java
@Component
public class BuiltInFunctions {
    // 二进制读取原语 (供字节流协议使用)
    public byte[] hexDecode(String hex) { ... }
    public float readFloat(byte[] data, int offset) { ... }
    public int readUInt16(byte[] data, int offset) { ... }
    public int readUInt8(byte[] data, int offset) { ... }
    public String readAscii(byte[] data, int offset, int length) { ... }
    public String readBCD(byte[] data, int offset, int length) { ... }

    // JSON 解析 (系统协议可用)
    public Map<String, Object> jsonParse(String json) { ... }

    // 设备信息查询
    public Map<String, Object> getDeviceInfo(String deviceCode) { ... }
    public Map<String, Object> getSensorInfo(String sensorCode) { ... }
}
```

### 4.3 预置脚本模板

**sys 协议脚本**（从现有 `SysMonitorPayloadParser` 翻译）：

```groovy
@groovy.transform.CompileStatic
Map<String, Object> parse(String topic, String payload) {
    def json = builtin.jsonParse(payload)
    def result = [
        sensorCode: json.getOrDefault("sensorNo", "").toString(),
        properties: []
    ]

    if (json.containsKey("version") || json.containsKey("data")) {
        parseStandard(json, result)
    } else {
        parseLegacy(json, result)
    }
    return result
}

@groovy.transform.CompileStatic
private void parseStandard(Map json, Map result) {
    def data = json.get("data")
    if (data instanceof List) {
        result.properties = data.collect { item ->
            [
                identifier: item.getOrDefault("code", "value").toString(),
                value: builtin.toDouble(item.get("value")),
                quality: builtin.toInt(item.get("quality"), 0)
            ]
        }
    } else if (data instanceof Map) {
        result.properties = data.collect { k, v ->
            [identifier: k.toString(), value: builtin.toDouble(v), quality: 0]
        }
    }
}

@groovy.transform.CompileStatic
private void parseLegacy(Map json, Map result) { ... }
```

**gb 协议脚本**（占位 → 正式实现）：

```groovy
@groovy.transform.CompileStatic
Map<String, Object> parse(String topic, String payload) {
    byte[] bytes = builtin.hexDecode(payload)
    def result = [
        sensorCode: "1",
        properties: []
    ]
    // 按国标帧格式逐字段解析
    result.properties.add([
        identifier: "water_level",
        value: (double) builtin.readFloat(bytes, 20),
        quality: builtin.readUInt8(bytes, 24)
    ])
    return result
}
```

## 五、数据库设计

### 5.1 解析策略表 (iot_data_parse_strategy)

```sql
CREATE TABLE `iot_data_parse_strategy` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) NOT NULL COMMENT '策略名称',
  `source_type` varchar(50) NOT NULL COMMENT '协议标识 (sys/gb/自定义)',
  `description` text COMMENT '描述',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态 0-停用 1-启用',
  `app_scope` varchar(20) NOT NULL DEFAULT 'global' COMMENT '应用范围 global/vendor/device',
  `script_code` mediumtext NOT NULL COMMENT 'Groovy解析脚本',
  `is_preset` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否预置策略 0-否 1-是',
  `last_run_time` datetime DEFAULT NULL COMMENT '最近运行时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_source_type` (`source_type`),
  KEY `idx_status` (`status`),
  KEY `idx_app_scope` (`app_scope`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据解析策略表';
```

### 5.2 策略-厂商关联表 (iot_data_parse_strategy_vendor)

```sql
CREATE TABLE `iot_data_parse_strategy_vendor` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `strategy_id` bigint NOT NULL,
  `vendor_id` bigint NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_strategy_vendor` (`strategy_id`, `vendor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 5.3 策略-设备关联表 (iot_data_parse_strategy_device)

```sql
CREATE TABLE `iot_data_parse_strategy_device` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `strategy_id` bigint NOT NULL,
  `device_id` bigint NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_strategy_device` (`strategy_id`, `device_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 5.4 运行日志表 (iot_data_parse_log)

```sql
CREATE TABLE `iot_data_parse_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `strategy_id` bigint NOT NULL,
  `log_level` varchar(20) NOT NULL COMMENT 'INFO/WARN/ERROR',
  `message` text NOT NULL,
  `data` text COMMENT '关联数据(JSON)',
  `topic` varchar(200) DEFAULT NULL,
  `device_code` varchar(100) DEFAULT NULL,
  `parse_result` text COMMENT '解析结果(JSON)',
  `execution_time` int DEFAULT NULL COMMENT '执行耗时(毫秒)',
  `error_stack` text COMMENT '错误堆栈',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_strategy_id` (`strategy_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 5.5 预置策略数据

```sql
INSERT INTO `iot_data_parse_strategy` (`name`, `source_type`, `description`, `status`, `app_scope`, `script_code`, `is_preset`) VALUES
('系统协议解析', 'sys', '系统自定义JSON协议解析策略', 1, 'global', '<sys-groovy-script>', 1),
('国标协议解析', 'gb', '国标字节流协议解析策略', 1, 'global', '<gb-groovy-script>', 1);
```

## 六、MonitorMetadataService 重写

从 timeseries 移至 parser 模块，职责扩展：

```java
@Service
public class MonitorMetadataService {
    // 1. 传感器元数据（保留现有能力）
    public SensorMetadata requireSensorMetadata(Long deviceId, String sensorCode);

    // 2. TSL 物模型查询（新增，用于值域校验）
    public ProductTsl getTsl(Long deviceId);

    // 3. 策略匹配（新增，三级级联）
    public DataParseStrategy resolveStrategy(String sourceType, Long deviceId);
    //    Priority: device > vendor > global (by sourceType)
}
```

## 七、异常处理

| 场景 | 处置 | 影响 |
|------|------|------|
| 策略未匹配 | 静默丢弃 + ERROR 日志，不抛异常 | MQTT 不回 ack，等待重发 |
| 脚本编译失败 | 策略保存时预编译校验，手动拒绝 | 不写入 Stream |
| 脚本运行异常 | 原始报文入 DLQ (`stream:monitor:dlq`) + 日志表 ERROR | 不阻塞其他策略 |
| 脚本超时 (30s) | `TimedInterrupt` 中断，入 DLQ | 释放线程 |
| 值域校验失败 | WARN 日志 + 日志表，数据仍入 Stream | 不阻断链路 |
| Redis Stream 满 | `MonitorIngestStreamService` 内部失败重试 | 由 Redis 集群保障 |

```java
// MonitorIngestFacade.ingest() 中的异常处理骨架
try {
    strategy = metadataService.resolveStrategy(sourceType, deviceId);
    if (strategy == null) {
        log.error("策略未匹配: topic={}, sourceType={}", topic, sourceType);
        return;  // 静默丢弃
    }
    parsedMessage = scriptEngine.execute(strategy, topic, message);
    validator.validate(parsedMessage, metadataService.getTsl(deviceId));
    streamService.enqueue(parsedMessage);
} catch (StrategyNotFoundException e) {
    log.error("策略未匹配: topic={}", topic);
} catch (ScriptExecutionException e) {
    streamService.enqueueDeadLetter(topic, message, e.getMessage());
    logService.save(ParseLog.error(strategy.getId(), e));
} catch (Exception e) {
    log.error("数据解析异常: topic={}", topic, e);
}
```

## 八、API 接口

基础路径 `/api/v1/iot/parser/strategy`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/page` | 分页查询策略列表 |
| GET | `/{id}` | 获取策略详情（含关联厂商/设备） |
| POST | `/` | 新增策略（预编译校验） |
| PUT | `/` | 更新策略（预编译校验） |
| DELETE | `/{id}` | 逻辑删除策略 |
| PUT | `/{id}/status` | 启用/停用策略 |
| POST | `/{id}/copy` | 复制策略 |
| POST | `/test` | 在线测试脚本执行 |
| GET | `/{id}/logs` | 查询策略运行日志 |
| DELETE | `/{id}/logs` | 清空日志 |
| GET | `/{id}/logs/export` | 导出日志 |

## 九、测试策略

### 9.1 策略管理 CRUD

- 预编译校验：非法 Groovy 脚本保存时直接拒绝，返回编译错误信息
- 关联表级联：vendor/device 关联变更后正确持久化
- 状态切换：停用后策略不再匹配到执行路径

### 9.2 脚本执行正确性

- 复用 `SysMonitorPayloadParser` 的现有测试用例覆盖 sys 预置脚本
  - 标准格式：单值/多值对象/数组/CSV
  - Legacy 格式：嵌套 deviceId 键
  - 边界：空 payload、异常编码、超长字段
- 国标脚本：提供样本 hex 报文 → 断言 `ParsedMessage.properties`
- 测试用 `GroovyShell` 独立执行脚本，验证产出结构与值

### 9.3 集成测试

- 模拟 MQTT 消息 → `MonitorIngestFacade.ingest()` → 读 Redis Stream 验证 `ParsedMessage` JSON
- timeseries 的 `adapt(ParsedMessage)` 转换逻辑覆盖

### 9.4 回归测试

- timeseries 模块现有测试全量回归
- `MonitorIngestFacade` 改造后调用链路不变（仅内部实现切换）

## 十、迁移计划

| Phase | 内容 | 风险 | 可验证 |
|-------|------|------|--------|
| 1 | 新建 `zwei-iot-parser` 模块，建表，实现策略 CRUD + Groovy 引擎 | 低 | 单元测试 |
| 2 | 将 `SysMonitorPayloadParser` 翻译为预置 Groovy 脚本，复用现有测试用例验证 | 中 | 对比测试 |
| 3 | 实现国标字节流脚本（调用 `builtin.read*`），替代 `GbMonitorPayloadParser` 占位 | 中 | 样本报文测试 |
| 4 | 改造 `MonitorIngestFacade`：策略匹配 → Groovy 执行 → 入 Stream | 中 | 集成测试 |
| 5 | 改造 `MonitorIngestConsumerService`：新增 `adapt(ParsedMessage)` | 低 | 单元测试 |
| 6 | 删除旧代码：`MonitorPayloadParser` 接口、`SysMonitorPayloadParser`、`GbMonitorPayloadParser` | 低 | 编译通过 |
| 7 | 移除 `MonitorTopicParser`、`MonitorTopic` 从 timeseries，确认 parser 模块引用正确 | 低 | 编译通过 |

每阶段独立可验证，Phase 1-2 不影响现有链路。

## 十一、待删除代码清单

| 文件 | 模块 | 阶段 |
|------|------|------|
| `parser/MonitorPayloadParser.java` | timeseries | 6 |
| `parser/SysMonitorPayloadParser.java` | timeseries | 6 |
| `parser/GbMonitorPayloadParser.java` | timeseries | 6 |
| `support/MonitorTopicParser.java` | timeseries | 7 |
| `support/MonitorTopic.java` | timeseries | 7 |
| `service/MonitorMetadataService.java` | timeseries | 7 (移至 parser) |
| `StandardMeasurementPoint` 的 `sensorId` 字段 | timeseries | 5 (适配时填充) |

## 十二、安全边界

- Groovy 沙箱：`SecureASTCustomizer` 禁止 `java.io.*`、`java.nio.*`、`java.net.*`、`java.lang.reflect.*`、`java.lang.System`
- 脚本超时：`TimedInterrupt` 30s 硬超时
- 预编译校验：CRUD API 保存时编译检查，拒绝非法脚本
- `builtin.*` 函数为唯一对外交互面，不开放任意 Java 调用
- `source_type` 白名单：`sys`、`gb` 及管理员定义的自定义标识

## 变更记录

| 时间 | 变更 |
|------|------|
| 2026-06-15 | 初版 V2 设计，替代 2026-06-11 初版 |
