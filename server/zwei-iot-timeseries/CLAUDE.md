[根目录](../../CLAUDE.md) > [server](../) > **zwei-iot-timeseries**

# zwei-iot-timeseries — IoTDB 读写 + MQTT 解析 + 监测数据查询

> 面包屑: [根目录](../../CLAUDE.md) > [server](../) > **zwei-iot-timeseries**

## 模块职责

时序数据层:

- **IoTDB JDBC 客户端** (`IotdbJdbcClient`) — 读写时序数据 (IoTDB 2.0+)
- **MQTT Payload 解析器** — 支持 `sys` 协议与 `gb` (国标) 协议
- **数据接入消费者** (`MonitorIngestConsumerService`) — 消费 Redis Stream 异步入库
- **监测数据查询 API** (`MonitorDataQueryService`) — 最新值/分页/图表数据

## 关键依赖

- `zwei-common`
- `zwei-iot-device` (字典 + 设备信息 + `IDeviceSensorService`)
- `zwei-iot-monitor` (字典)
- `zwei-iot-hazard` (隐患点)
- `iotdb-jdbc` (1.3.4, 数据库实际为 IoTDB 2.0+)
- lombok

## 主要子包

| 子包           | 职责                                                                                                                                                                                                                                    |
|--------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `controller` | `MonitorDataController` (hazardPointId 入口) + `MonitorDataSensorController` (sensorCode 入口,5 端点)                                                                                                                                   |
| `service`    | `IotdbTimeSeriesService` (核心) / `IotdbJdbcClient` / `MonitorIngestFacade` / `MonitorIngestStreamService` / `MonitorIngestConsumerService` / `MonitorMetadataService` / `MonitorDataQueryService` / `impl/TimeSeriesSchemaServiceImpl` |
| `parser`     | `MonitorPayloadParser` (接口) / `SysMonitorPayloadParser` / `GbMonitorPayloadParser`                                                                                                                                                    |
| `support`    | `MonitorTopic` / `MonitorTopicParser` / `IotdbPathResolver`                                                                                                                                                                           |
| `config`     | `IotdbProperties` / `MonitorIngestProperties`                                                                                                                                                                                         |
| `domain`     | `StandardMeasurementPoint` (record) / `IotdbQueryRow` / `MonitorDataVO` / `ChartDataVO` / `ValueType`                                                                                                                                 |

## 数据流

```
Field sensors → MQTT (mica-mqtt) → MqttServerMessageListener
    → MonitorIngestFacade.ingest(topic, payload, deviceId)
        → MonitorTopicParser (解析协议/设备/传感器)
        → MonitorMetadataService (查询设备传感器元数据)
        → MonitorPayloadParser (sys/gb)
        → MonitorIngestStreamService.enqueue(points)  → Redis Stream (stream:monitor:ingest)
    → MonitorIngestConsumerService (单线程 daemon)
        ─ 阶段1: 幂等去重 (Redis SETNX, dedupe-key 拼接 deviceId:sensorNo:attrCode:dataTime:payloadHash)
        ─ 阶段2: IotdbTimeSeriesService.writePoints (懒建 aligned timeseries, 质量码 INT32 RLE)
        ─ 阶段3: 运维指标回写
            ├─ DeviceOnlineStatusService → device_online_status.last_report_at
            ├─ DeviceSensorService → device_sensor.last_report_time
            └─ DeviceMapper → device.lastReportTime (兼容保留)
    → 失败重试: 三段退避 (3s/9s/27s) → 死信队列
```

## 对外接口 (Controller)

| 路径                                | 方法  | 职责            |
|-----------------------------------|-----|---------------|
| `/api/v1/iot/monitor-data/latest` | GET | 某设备/隐患点最新监测值  |
| `/api/v1/iot/monitor-data/page`   | GET | 历史分页          |
| `/api/v1/iot/monitor-data/chart`  | GET | 图表数据 (时间窗口聚合) |
| `/api/v1/iot/monitor-data/export` | GET | 导出 Excel      |

## 协议解析

- **sys 协议** — 系统自定义 (二进制/JSON, 视设备厂商)
- **gb 协议** — 国标 SL651 / SL427 (水文/地质灾害)

每种协议对应一个 `*Parser` 实现, 通过 `MonitorPayloadParser.supports(topic)` 路由。`MonitorIngestFacade` 注入
`List<MonitorPayloadParser>` 自动发现。

**Topic 格式** (来自 `MonitorTopicParser`):

```
^(sys|gb)/v1/(?<deviceCode>[A-Za-z0-9_-]{1,64})/(?<sensorNo>[A-Za-z0-9_-]{1,64})/updata$
```

## IoTDB 路径模型

- 数据库: `root.zwei` (在 `IotdbProperties.database` 配置)
- 时序路径: `root.{database}.d{deviceId}.s{sensorNo}`
- 业务指标列: `DOUBLE + GORILLA` (懒建)
- 质量码列: `INT32 + RLE` (懒建)
- 压缩器: `SNAPPY`
- 首次写入触发 `CREATE TIMESERIES ... WITH DATATYPE=DOUBLE, ENCODING=GORILLA, COMPRESSOR=SNAPPY`
- 路径缓存: `ConcurrentHashMap<String, Boolean> createdMeasurements` 避免重复 DDL

## 异步接入

- 消费线程: `monitor-ingest-consumer` (单线程 daemon, 启动于 `ApplicationReadyEvent`)
- 优雅停机: `@PreDestroy` 设置 `running=false` → `shutdownNow()` → `awaitTermination(5s)`
- 消费组: `createGroup(ReadOffset.latest())`, BUSYGROUP 视为已存在直接成功
- 幂等去重: Redis SETNX `dedupe-key-prefix` + TTL
- 三段退避: 配置化 `retryDelaysSeconds` (默认 3/9/27 秒)
- 死信: `MonitorIngestStreamService.enqueueDeadLetter(point, errorMsg)`

## 核心实现类索引 (P0)

| 类                              | 文件                                              | 关键方法 / 责任                                                                                                                                |
|--------------------------------|-------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------|
| `IotdbTimeSeriesService`       | `service/IotdbTimeSeriesService.java`           | `writePoints` / `queryLatest` / `queryRange` / `queryRangePaged` / `countRange` / `createSensorSchema` / `queryAggregate` / `queryDelta` / `queryCompleteness` / `queryTrend` / `queryLatestBySensor` / `queryRangeBySensor` |
| `MonitorDataAggregationService` | `service/MonitorDataAggregationService.java`    | `aggregate` / `aggregateAllAttrs` / `delta` — 表达式驱动聚合 |
| `MonitorDataAnalysisService`    | `service/MonitorDataAnalysisService.java`       | `completeness` / `trend` — 派生统计 |
| `MonitorIngestFacade`          | `service/MonitorIngestFacade.java`              | `ingest(topic, message, deviceId)` — 串联 topic 解析+元数据+解析器+入队                                                                              |
| `MonitorIngestStreamService`   | `service/MonitorIngestStreamService.java`       | `enqueue(points)` / `enqueueDeadLetter` — Redis Stream 写入/死信                                                                             |
| `MonitorIngestConsumerService` | `service/MonitorIngestConsumerService.java`     | 入口 `@EventListener(ApplicationReadyEvent)`，循环 `consume()` + `processRecord()` (4 阶段)                                                     |
| `MonitorDataQueryService`      | `service/MonitorDataQueryService.java`          | `latest` / `page` / `chart` — 业务查询 (注: **接口 + 实现合一**，无 I 前缀)                                                                             |
| `MonitorMetadataService`       | `service/MonitorMetadataService.java`           | `requireSensorMetadata(deviceId, sensorNo)`                                                                                              |
| `IotdbJdbcClient`              | `service/IotdbJdbcClient.java`                  | `getConnection()` / `executeBatch()` / `executeSilent()`                                                                                 |
| `IotdbPathResolver`            | `support/IotdbPathResolver.java`                | `buildSensorPath()` / `buildMeasurementPath()`                                                                                           |
| `MonitorTopicParser`           | `support/MonitorTopicParser.java`               | `parse(topic)` 提取 (protocol, deviceCode, sensorNo)                                                                                       |
| `TimeSeriesSchemaServiceImpl`  | `service/impl/TimeSeriesSchemaServiceImpl.java` | `createSensorSchema()` (注册冷路径预建)                                                                                                         |

## 查询能力矩阵

| 维度 | hazardPointId 入口(保留) | sensorCode 入口(新增) |
|---|---|---|
| 最新值 | `/api/v1/monitor-data/latest` | `/api/v1/monitor-data/sensor/latest` |
| 区间数据 | `/api/v1/monitor-data/page` | `/api/v1/monitor-data/sensor/range` (支持 minValue/maxValue) |
| 聚合 | `/api/v1/monitor-data/chart` | `/api/v1/monitor-data/sensor/aggregate` (多表达式 + 数值范围) |
| delta | — | `/api/v1/monitor-data/sensor/aggregate` (传 LAST-FIRST 表达式) |
| 完整度 | — | `/api/v1/monitor-data/sensor/completeness` |
| 趋势 | — | `/api/v1/monitor-data/sensor/trend` |

## ExpressionSpec DSL

`AggregationFunction` 白名单枚举: `AVG/MAX/MIN/SUM/COUNT/FIRST_VALUE/LAST_VALUE/EXTREME/STDDEV/P50/P95/P99`。

表达式组合通过 sealed `ExpressionSpec`:
- `FunctionCall(func)` — 单函数
- `BinaryOp(left, op, right)` — 二元运算,op ∈ {+, -, *, /}
- `Constant(value)` — 标量

常见组合:
- `MAX - MIN` → 极差(别名 `MAX-MIN`)
- `LAST_VALUE - FIRST_VALUE` → **delta**(别名 `DELTA`,自动映射)
- `(MAX - MIN) / AVG` → 变异系数

## 安全边界

- SQL 拼接仅从 `AggregationFunction` 枚举取值,无法注入任意函数
- `ExpressionSpec` 是 sealed interface,子类型编译期固定
- 表达式嵌套深度 ≤ 5,别名长度 ≤ 64
- `attrCode`/`sensorCode` 走 `IotdbPathResolver`,不直接拼 SQL

## Redis Key / Stream 模式

| Key                                                                          | 类型     | 用途                 |
|------------------------------------------------------------------------------|--------|--------------------|
| `stream:monitor:ingest`                                                      | Stream | 监测数据缓冲队列           |
| `{dedupeKeyPrefix}{deviceId}:{sensorNo}:{attrCode}:{dataTime}:{payloadHash}` | String | 幂等去重 (SETNX + TTL) |

Stream 配置 (`MonitorIngestProperties`):

- `streamKey` (默认 `stream:monitor:ingest`)
- `consumerGroup` / `consumerName` / `pollBatchSize` / `pollBlockMs`
- `dedupeKeyPrefix` / `dedupeTtlSeconds`
- `retryDelaysSeconds` (List<Long>)

## 查询 API 详细

`MonitorDataQueryService`:

- `latest(hazardPointId)` → `List<MonitorDataVO>` (按传感器+属性解析, 调 `IotdbTimeSeriesService.queryLatest`)
- `page(hazardPointId, deviceId?, sensorId?, attrCode?, valueType?, startTime?, endTime?, pageNum, pageSize)` →
  `Map{total, rows, pageNum, pageSize}`
    - 单测点: IoTDB 原生 LIMIT/OFFSET
    - 多测点: 每个测点取 `pageNum * pageSize`, 合并排序后截取
- `chart(...)` → `List<ChartDataVO>` (含 max/min/avg 统计)
- `ValueType` 控制聚合: `current` (原始) / `hour` / `24h` / `72h` (走 IoTDB `GROUP BY` + 聚合函数)

## 测试与质量

- 单元测试: 各协议解析器 (`SysMonitorPayloadParser` / `GbMonitorPayloadParser`)
- 集成测试: IoTDB 测试容器 (`testcontainers-iotdb`)
- 模拟 MQTT 上行 → 端到端验证数据入库

## 常见问题 (FAQ)

**Q: 新增一种协议如何扩展?**
A: 实现 `MonitorPayloadParser` 子接口 (`supports(topic)` + `parse(topic, message, metadata)`)，由 Spring 自动注入到
`MonitorIngestFacade.parsers` List。

**Q: 数据查询慢怎么办?**
A: 1) 走 `ITimeSeriesSchemaService` 确认有索引; 2) 缩短时间窗口; 3) 用 `valueType=hour/24h` 走 IoTDB `GROUP BY` 降采样;

4) 启用 IoTDB 查询缓存。

**Q: Redis Stream 堆积怎么办?**
A: 1) 检查消费者是否抛异常未 ACK; 2) 增加 batchSize (注意单线程串行); 3) 横向扩展实例 (不同 consumerName)。

**Q: 为什么 alarm 引擎需要 `MonitorDataIngestedEvent`?**
A: `IotdbTimeSeriesService` 写入成功后由 `MonitorIngestConsumerService` 内部触发，alarm 模块通过 `@EventListener`
异步消费，避免在 IoTDB 写路径上做重逻辑。

## 相关文件清单

- `pom.xml`
- `src/main/java/com/zwei/iot/timeseries/controller/MonitorDataController.java`
- `src/main/java/com/zwei/iot/timeseries/service/IotdbTimeSeriesService.java` (P0)
- `src/main/java/com/zwei/iot/timeseries/service/MonitorIngestFacade.java` (P0)
- `src/main/java/com/zwei/iot/timeseries/service/MonitorIngestStreamService.java` (P0)
- `src/main/java/com/zwei/iot/timeseries/service/MonitorIngestConsumerService.java` (P0)
- `src/main/java/com/zwei/iot/timeseries/service/MonitorDataQueryService.java` (P0)
- `src/main/java/com/zwei/iot/timeseries/service/MonitorMetadataService.java` (P0)
- `src/main/java/com/zwei/iot/timeseries/service/IotdbJdbcClient.java`
- `src/main/java/com/zwei/iot/timeseries/service/impl/TimeSeriesSchemaServiceImpl.java`
- `src/main/java/com/zwei/iot/timeseries/parser/SysMonitorPayloadParser.java`
- `src/main/java/com/zwei/iot/timeseries/parser/GbMonitorPayloadParser.java`
- `src/main/java/com/zwei/iot/timeseries/support/MonitorTopicParser.java`
- `src/main/java/com/zwei/iot/timeseries/support/IotdbPathResolver.java`

## 变更记录 (Changelog)

| 时间               | 变更                                                                                                                 |
|------------------|--------------------------------------------------------------------------------------------------------------------|
| 2026-06-10 18:52 | 首次生成模块级 CLAUDE.md (架构师自动扫描)                                                                                        |
| 2026-06-10 19:08 | 增量补扫: 修正路径 `ingest/` → `service/`，`query/` → 实际为 `MonitorDataQueryService`；新增核心实现类索引、Redis Key 模式、四阶段处理流程、三段退避重试说明 |
| 2026-06-14 | 新增查询能力增强: ExpressionSpec DSL + 数值范围 + 完整度/趋势 — 详见 specs/2026-06-14-timeseries-query-enhancement-design |
| 2026-06-25 | PEL 泄漏修复: parseObject 移入 try 块 (解析失败→死信+ACK) + 启动时 XAUTOCLAIM 兜底回收超时 PEL (含 XPENDING+XCLAIM 降级) + MonitorIngestProperties 新增 pelRecoverIdleMs 配置 |
