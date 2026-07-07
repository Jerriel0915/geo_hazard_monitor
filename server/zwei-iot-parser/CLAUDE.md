[根目录](../../CLAUDE.md) > [server](../) > **zwei-iot-parser**

# zwei-iot-parser — 数据解析中间层 策略管理 + Groovy 脚本引擎

> 面包屑: [根目录](../../CLAUDE.md) > [server](../) > **zwei-iot-parser**

## 模块职责

独立于存储层的报文解析模块:

- **策略管理** (`DataParseStrategyService`) — 解析策略 CRUD + 厂商/设备级绑定 + 预置策略
- **Groovy 脚本引擎** (`GroovyScriptEngine`) — 沙箱化执行用户/预置解析脚本, 产出 `ParsedMessage`
- **元数据服务** (`MonitorMetadataService`) — 传感器元数据查询 + TSL 物模型 + 三级策略匹配
- **内置函数库** (`BuiltInFunctions`) — 二进制读取原语 (hex/float/double/BCD) + 工具方法
- **在线测试** (`DataParseController`) — 策略管理的 REST API + 脚本在线测试

## 关键依赖

- `zwei-common` (`ParsedMessage` / `PropertyValue` — 本模块的对外产出格式)
- `zwei-iot-device` (`SensorMetadata` / `ProductTsl` / `IDeviceSensorQueryService`)
- `org.apache.groovy:groovy` + `groovy-json` (`JsonSlurper`)
- MyBatis (策略表/日志表数据访问)
- lombok + junit + AssertJ + Mockito

> **不依赖 `zwei-iot-timeseries`** — 完全不感知 IoTDB 和 Redis Stream 消费逻辑。

## 主要子包

| 子包 | 职责 |
|------|------|
| `controller` | `DataParseController` — 策略 CRUD + 脚本测试 + 运行日志查询, 10 个端点 |
| `service` | `DataParseStrategyService` / `DataParseLogService` / `MonitorMetadataService` |
| `engine` | `GroovyScriptEngine` (沙箱执行) / `BuiltInFunctions` (二进制原语注入) |
| `domain` | `DataParseStrategy` / `DataParseStrategyVendor` / `DataParseStrategyDevice` / `DataParseLog` |
| `dto` | `DataParseStrategyDTO` / `DataParseStrategyQueryDTO` / `DataParseTestRequest` |
| `mapper` | MyBatis Mapper 接口 + XML (4 个 Mapper) |
| `support` | `GroovyScriptValidator` (预编译校验) / `MonitorTopic` (record) / `MonitorTopicParser` (正则) |

## 数据流

```
MQTT raw message
  │
  ▼
MonitorIngestFacade.ingest(topic, message, deviceId)  [timeseries]
  │
  ├─ MonitorTopicParser.parse(topic) → MonitorTopic  [parser]
  ├─ MonitorMetadataService.resolveStrategy(sourceType, deviceId)  [parser]
  │     ├─ 1st: strategy_device (device 级)
  │     ├─ 2nd: strategy_vendor (vendor 级, 预留)
  │     └─ 3rd: sourceType 全局匹配
  ├─ GroovyScriptEngine.execute(strategy, topic, message)  [parser]
  │     └─ 注入 builtin.* 函数 → 沙箱执行 → 产出 ParsedMessage
  ├─ Facade.enrichProperties()  [timeseries]
  │     └─ value_N → SensorMetadata.attributes[id] 映射
  └─ MonitorIngestStreamService.enqueue() → Redis Stream  [timeseries]
```

## 策略匹配 (三级级联)

1. **device 级** — `strategy_device` 表按 `device_id` 精确匹配
2. **vendor 级** — `strategy_vendor` 表按厂商匹配 (预留)
3. **sourceType 级** — 全局策略, 按 `source_type` 字段匹配 (预置 sys/gb 脚本)

## Groovy 脚本约定

### 入口函数签名

```groovy
Map<String, Object> parse(String topic, byte[] messageBytes)
```

### 返回值结构

```groovy
[
    sensorCode: "WY_1",     // 可选, 默认从 topic 提取
    deviceCode: "DEV001",   // 可选, 默认从 topic 提取
    dataTime: 1700000000000, // 可选, 默认取当前时间
    properties: [
        [identifier: "rainfall_hour", value: 25.5, quality: 0],
        [identifier: "rainfall_day",  value: 120.0, quality: 0]
    ]
]
```

### CSV 格式数据处理

脚本解析 `"value": "16.7,24.1,9.7"` 时产出 positional 标识符 (`value_0`/`value_1`/`value_2`)。
`MonitorIngestFacade.enrichProperties()` 在 Java 侧查询 `SensorMetadata.attributes` (按 id 排序)
将 positional 标识符重映射为真实 `attrCode`。脚本无需感知业务属性名。

### 注入对象

| 变量 | 类型 | 说明 |
|------|------|------|
| `builtin` | `BuiltInFunctions` | 二进制读取 + 工具方法 (见下方 API) |

### BuiltInFunctions API

| 方法 | 说明 |
|------|------|
| `builtin.hexDecode(String hex)` | hex 字符串 → byte[] |
| `builtin.readFloat(byte[] data, int offset)` | 大端序 float (4 bytes) |
| `builtin.readDouble(byte[] data, int offset)` | 大端序 double (8 bytes) |
| `builtin.readUInt16(byte[] data, int offset)` | 大端序 uint16 |
| `builtin.readInt16(byte[] data, int offset)` | 大端序 int16 (signed) |
| `builtin.readUInt8(byte[] data, int offset)` | uint8 |
| `builtin.readAscii(byte[] data, int offset, int length)` | 定长 ASCII 字符串 |
| `builtin.readBcdTimestamp(byte[] data, int offset)` | BCD 编码时间 (7 bytes) → epoch millis |
| `builtin.currentTimeMillis()` | 当前 epoch 毫秒 |
| `builtin.sha256(byte[] data)` | SHA-256 十六进制摘要 |
| `builtin.toDouble(Object v)` | 安全转换为 Double |
| `builtin.toInt(Object v, int defaultVal)` | 安全转换为 Integer |

## 沙箱安全

### SecureASTCustomizer 配置

- **禁止 import 的包**: `java.lang.System`, `java.lang.Runtime`, `java.lang.ProcessBuilder`, `java.lang.Thread`, `java.lang.Class`, `java.lang.ClassLoader`, `java.io.*`, `java.io.File*`, `java.nio.*`, `java.net.*`, `java.lang.reflect.*`, `java.lang.invoke.*`, `javax.script.*`, `groovy.lang.*`, `org.codehaus.groovy.*`
- **禁止 receiver 的方法调用**: `System`, `Runtime`, `ProcessBuilder`, `Class`, `Thread`, `File`, `GroovyShell`, `GroovyClassLoader`, `Script`, `Closure`, `InvokerHelper`
- **禁止静态导入**: 所有 (`*`)
- 脚本保存时通过 `GroovyScriptValidator.validate()` 预编译校验
- 运行时超时: 30 秒 `Future.get(timeout)`

### Groovy 脚本编写注意事项

- 禁止使用 `@CompileStatic` — `builtin` 通过 Binding 注入, 静态编译无法解析
- 禁止使用 `Map.property = value` 赋值 — 用 `map.put("key", value)` 代替 (Groovy 会将 `.property =` 
  解析为 setter 调用而非 Map.put)
- `import groovy.json.JsonSlurper` 已放行 (用于 JSON 解析)

## 核心实现类索引

| 类 | 文件 | 关键方法 / 责任 |
|----|------|----------------|
| `GroovyScriptEngine` | `engine/GroovyScriptEngine.java` | `execute()` — 沙箱执行, `testScript()` — 在线测试, `getOrCreateShell()` — shell 缓存, `resolveDeviceCode()` — 从 topic 提取 deviceCode |
| `BuiltInFunctions` | `engine/BuiltInFunctions.java` | 10+ 个二进制读取/工具方法, 注入到 Groovy Binding 的 `builtin` 变量 |
| `GroovyScriptValidator` | `support/GroovyScriptValidator.java` | `validate(scriptCode)` — 预编译校验 + 安全扫描 |
| `DataParseStrategyService` | `service/DataParseStrategyService.java` | 策略 CRUD + 复制 + 启停 + 关联表管理 |
| `MonitorMetadataService` | `service/MonitorMetadataService.java` | `requireSensorMetadata()` / `getTsl()` / `resolveStrategy()` |
| `MonitorTopicParser` | `support/MonitorTopicParser.java` | `parse(topic)` — 正则提取 (sourceType, deviceCode, sensorCode) |
| `MonitorTopic` | `support/MonitorTopic.java` | record(sourceType, deviceCode, sensorCode) |

## 解析策略预置脚本

| 策略 | sourceType | 脚本 | 说明 |
|------|-----------|------|------|
| 系统协议解析 | `sys` | JSON (Groovy) | 标准格式 (version+data) + 传统格式 (嵌套 deviceId 键), 来源于原 `SysMonitorPayloadParser` (570 行 Java) |
| 国标协议解析 | `gb` | 字节流 (Groovy + builtin) | 基于 hex 解码 + `builtin.read*/readBcdTimestamp` 解析国标帧, 替代原 `GbMonitorPayloadParser` 占位 |

## DB 表

| 表 | 说明 |
|----|------|
| `iot_data_parse_strategy` | 策略主表 (source_type, script_code, app_scope, etc.) |
| `iot_data_parse_strategy_vendor` | 策略-厂商关联 |
| `iot_data_parse_strategy_device` | 策略-设备关联 |
| `iot_data_parse_log` | 运行日志 (策略 ID + 日志级别 + 执行耗时 + 错误堆栈) |

迁移脚本: `db/upgrade/v2.1-parser-module.sql`

## Controller API (`/api/v1/iot/parser/strategy`)

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| GET | `/page` | `monitor:parser:list` | 分页查询策略列表 |
| GET | `/{id}` | `monitor:parser:list` | 策略详情 (含关联厂商/设备) |
| POST | `/` | `monitor:parser:edit` | 新增策略 (预编译校验) |
| PUT | `/` | `monitor:parser:edit` | 更新策略 |
| DELETE | `/{id}` | `monitor:parser:edit` | 逻辑删除 |
| PUT | `/{id}/status` | `monitor:parser:edit` | 启停策略 |
| POST | `/{id}/copy` | `monitor:parser:edit` | 复制策略 |
| POST | `/test` | `monitor:parser:test` | 在线测试脚本 |
| GET | `/{id}/logs` | `monitor:parser:list` | 运行日志 |
| DELETE | `/{id}/logs` | `monitor:parser:edit` | 清空日志 |

## 测试覆盖

| 测试类 | 用例数 | 覆盖内容 |
|--------|--------|---------|
| `GroovyScriptEngineTest` | 10 | sys 标准 JSON/legacy/gb 解析 + 边界 |
| `BuiltInFunctionsTest` | 15 | 二进制读取 + 越界校验 + sha256 + 类型转换 |
| `GroovySecurityTest` | 9 | 沙箱: 危险 import 拒绝 + 超大脚本 + 空脚本 |
| `ParsedMessageJsonTest` | 3 | fastjson2 序列化往返 |
| `MonitorTopicParserTest` | 3 | topic 正则解析 |

## 常见问题

**Q: 如何新增一种协议?**
A: 在 `iot_data_parse_strategy` 表中插入新策略, 指定 `source_type` 和 Groovy 脚本。
全局策略直接生效; device/vendor 级策略需在关联表中绑定目标设备/厂商。

**Q: 脚本执行失败如何排查?**
A: 查看 `iot_data_parse_log` 表 (按 strategy_id + create_time 查询), 或调 `POST /test` 在线测试。

**Q: 为什么不用 @CompileStatic?**
A: `builtin` 对象通过 Groovy Binding 注入, 静态编译无法解析动态注入的变量, 会导致编译错误。

**Q: Groovy 脚本中 Map 赋值为什么必须用 .put()?**
A: `result.properties = xxx` 在 Groovy 中被解析为 `result.setProperty("properties", xxx)` 而非
`result.put("properties", xxx)`, 会抛出 `ReadOnlyPropertyException`。使用 `result.put("key", value)` 替代。

## 相关文件清单

- `pom.xml`
- `src/main/java/com/zwei/iot/parser/controller/DataParseController.java`
- `src/main/java/com/zwei/iot/parser/engine/GroovyScriptEngine.java` (P0)
- `src/main/java/com/zwei/iot/parser/engine/BuiltInFunctions.java` (P0)
- `src/main/java/com/zwei/iot/parser/service/DataParseStrategyService.java` (P0)
- `src/main/java/com/zwei/iot/parser/service/MonitorMetadataService.java` (P0)
- `src/main/java/com/zwei/iot/parser/service/DataParseLogService.java`
- `src/main/java/com/zwei/iot/parser/support/GroovyScriptValidator.java` (P0)
- `src/main/java/com/zwei/iot/parser/support/MonitorTopicParser.java`
- `src/main/java/com/zwei/iot/parser/support/MonitorTopic.java`
- `src/main/java/com/zwei/iot/parser/domain/DataParseStrategy.java`
- `src/main/java/com/zwei/iot/parser/domain/DataParseLog.java`
- `src/main/resources/mapper/iot/parser/*.xml`

## 变更记录

| 时间 | 变更 |
|------|------|
| 2026-06-15 | 模块首次创建 — 策略 CRUD + Groovy 沙箱引擎 + 预置 sys/gb 脚本 + 40 个单测 |
| 2026-06-15 | 修复: 沙箱加固 (AST 级 receiver 黑名单) + @PreAuthorize 授权 + bean 名称冲突 |
| 2026-06-15 | 修复: groovy-json 依赖 + System → builtin.currentTimeMillis() + Map.put() 替代属性赋值 |
| 2026-06-15 | 重构: enrichProperties() 后置属性富化 — Groovy 脚本产出 value_N → Java 侧映射为真实 attrCode |
| 2026-07-07 | **动态 topic 前缀注册**: 新增 `ITopicPatternService` 接口 + `TopicPatternServiceImpl` 实现，从 `DataParseStrategy.sourceType` 动态派生 topic 正则；`MonitorTopicParser` 委托给该服务；策略变更自动 reload |
