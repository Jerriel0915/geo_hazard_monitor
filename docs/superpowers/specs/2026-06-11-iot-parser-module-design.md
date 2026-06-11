# zwei-iot-parser 模块设计

> 状态: 已确认 | 日期: 2026-06-11

## 一、目标

为所有设备上报报文增加一层**可脚本化的数据解析转化层**。用户通过 Groovy 脚本，将不同厂商设备的异构报文统一输出为系统标准元数据。后续所有数据消费围绕统一元数据格式展开。

### 一期范围

- 策略驱动解析（按 MQTT topic 路由到 Groovy 脚本）
- 预置 sys/gb 两条解析策略
- 策略 CRUD + 在线测试接口
- 运行日志（仅 ERROR）

### 一期不做

- Blockly 可视化编程
- 厂商/设备级策略绑定（仅按 topic 路由）
- Groovy 安全沙箱（脚本仅管理员录入）
- 策略版本管理与回滚
- 前端管理页面

## 二、模块定位与依赖

```
zwei-iot-device ────┐
(跨模块接口)         │
                    ▼
              zwei-iot-parser  ←── zwei-common
                    │
                    ▼
              zwei-iot-timeseries
```

| 依赖 | 原因 |
|------|------|
| `zwei-common` | `AjaxResult`, `BaseController`, `BusinessException`, `StandardMeasurementPoint` |
| `zwei-iot-device` | `IDeviceSensorQueryService` 获取传感器元数据 |
| `zwei-iot-timeseries` | **不依赖** |

`StandardMeasurementPoint` 从 `zwei-iot-timeseries/domain/` 下沉到 `zwei-common/domain/`。

## 三、包结构

```
server/zwei-iot-parser/src/main/java/com/zwei/iot/parser/
├── engine/
│   ├── ParseEngine.java              # 入口接口：策略匹配 → 脚本执行 → 标准化输出
│   ├── ParseEngineImpl.java          # 实现
│   ├── GroovyScriptExecutor.java     # Groovy 编译、缓存、执行
│   └── StrategyMatcher.java          # 按 topic_pattern LIKE 匹配策略（Caffeine 缓存）
├── strategy/
│   ├── domain/
│   │   ├── DataParseStrategy.java    # 策略实体
│   │   └── DataParseLog.java         # 运行日志实体（仅 ERROR）
│   ├── mapper/
│   │   ├── DataParseStrategyMapper.java
│   │   └── DataParseLogMapper.java
│   └── service/
│       ├── DataParseStrategyService.java
│       └── DataParseLogService.java
├── dto/
│   ├── DataParseStrategyDTO.java
│   ├── DataParseTestRequest.java
│   └── DataParseTestResponse.java
├── controller/
│   └── DataParseController.java
├── domain/
│   └── ParseResultItem.java          # 脚本返回的强类型结果项
├── metadata/
│   ├── SensorMetadataView.java       # 传感器属性视图（已有）
│   └── SensorAttributeView.java      # 属性代码/名称/单位（已有）
└── config/
    └── DataParseAutoConfiguration.java
```

## 四、数据库

### iot_data_parse_strategy

```sql
CREATE TABLE `iot_data_parse_strategy` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(100) NOT NULL COMMENT '策略名称',
  `topic_pattern` varchar(200) NOT NULL COMMENT '订阅主题（支持通配符 *，仅末尾）',
  `script_code` mediumtext NOT NULL COMMENT 'Groovy 解析脚本',
  `description` text COMMENT '描述',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '0-停用 1-启用',
  `is_preset` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否预置',
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
```

### iot_data_parse_log

```sql
CREATE TABLE `iot_data_parse_log` (
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

### 预置策略

| 策略 | topic_pattern | 说明 |
|------|--------------|------|
| 系统协议解析 | `sys/v1/*` | 翻译现有 SysMonitorPayloadParser 到 Groovy |
| 国标协议解析 | `gb/v1/*` | 占位，和现有 GbMonitorPayloadParser 行为一致 |

`is_preset=1` 不可删除、不可编辑脚本内容，仅允许启停和改名。

## 五、核心接口

### ParseEngine

```java
public interface ParseEngine {
    List<StandardMeasurementPoint> parse(String topic, byte[] message, Long deviceId)
            throws ParseException;
}
```

### ParseResultItem

```java
package com.zwei.iot.parser.domain;

public class ParseResultItem {
    private String sensorNo;   // 必填
    private String attrCode;   // 必填
    private Double value;      // 必填
    private Long dataTime;     // 默认当前时间
    private Integer quality;   // 默认 0

    public ParseResultItem() {}
    public ParseResultItem(String sensorNo, String attrCode, Double value,
                           Long dataTime, Integer quality) {
        this.sensorNo = sensorNo;
        this.attrCode = attrCode;
        this.value = value;
        this.dataTime = dataTime != null ? dataTime : System.currentTimeMillis();
        this.quality = quality != null ? quality : 0;
    }
}
```

### Groovy 脚本契约

脚本必须定义 `parse(Map ctx)` 方法，返回 `List<ParseResultItem>`：

- `ctx.topic` — MQTT 主题
- `ctx.payload` — 报文内容（String, UTF-8）
- `ctx.deviceId` — 设备 ID
- `ctx.deviceCode` — 设备编码
- `ctx.sensorNo` — 传感器编号
- `ctx.attributes` — `List<SensorAttributeView>` 传感器属性列表
- `ctx.log` — `IParseLogger` 脚本内日志（可选）

示例：

```groovy
import com.alibaba.fastjson2.JSON
import com.zwei.iot.parser.domain.ParseResultItem

def parse(Map ctx) {
    def json = JSON.parseObject(ctx.payload as String)
    return ctx.attributes
        .findAll { json.containsKey(it.getAttrCode()) }
        .collect { attr ->
            new ParseResultItem(
                ctx.sensorNo,
                attr.getAttrCode(),
                json.getDouble(attr.getAttrCode()),
                System.currentTimeMillis(),
                0
            )
        }
}
```

## 六、调用链

```
ParseEngineImpl.parse(topic, message, deviceId)
  │
  ├─ 1. Topic 解析（正则提取 deviceCode, sensorNo, sourceType）
  │      → 格式不匹配 → 返回空列表（静默跳过）
  │
  ├─ 2. StrategyMatcher.match(sourceType, topic)
  │      → DB LIKE 匹配，priority 降序，Caffeine 缓存 TTL=60s
  │      → 无匹配 → 返回空列表
  │
  ├─ 3. IDeviceSensorQueryService.requireSensorMetadata(deviceId, sensorNo)
  │      → 传感器不存在/停用 → 抛出 ParseException
  │
  ├─ 4. GroovyScriptExecutor.execute(script, context)
  │      → 编译缓存: MD5(scriptCode) 为 key
  │      → 编译失败/超时/运行异常 → ERROR 日志 + ParseException
  │
  ├─ 5. ResultValidator + ResultMapper
  │      → 校验必填字段 → 映射为 List<StandardMeasurementPoint>
  │      → 校验失败 → ERROR 日志 + ParseException
  │
  └─ 6. 更新策略 last_run_time
```

## 七、数据流（与 timeseries 集成）

```
设备 → MQTT → MqttServerMessageListener (broker)
                  │
                  ▼
            MonitorIngestFacade (timeseries, 薄化)
              │ parseEngine.parse(topic, message, deviceId)
              ▼
            ParseEngineImpl (parser)
              ├─ Topic 解析 + 策略匹配 + 元数据解析
              ├─ Groovy 脚本执行
              └─ 结果校验 + 映射
                  │
            成功 → List<StandardMeasurementPoint>
                  → MonitorIngestStreamService.enqueue()
                  → Redis Stream → IoTDB（现有链路不变）
            失败 → ParseException
                  → Facade catch + log.warn
                  → 消息丢弃（不重试，不入 DLQ）
```

### MonitorIngestFacade 改造

```java
// 改造后：只做编排，解析逻辑全在 parser
private ParseEngine parseEngine;

public void ingest(String topic, byte[] message, Long deviceId) {
    List<StandardMeasurementPoint> points;
    try {
        points = parseEngine.parse(topic, message, deviceId);
    } catch (ParseException e) {
        log.warn("解析失败: topic={}, deviceId={}", topic, deviceId, e);
        return;
    }
    if (!points.isEmpty()) {
        streamService.enqueue(points);
    }
}
```

### timeseries 模块删除清单

| 文件 | 原因 |
|------|------|
| `MonitorPayloadParser.java` | 接口被 ParseEngine 替代 |
| `SysMonitorPayloadParser.java` | 转 Groovy 预置脚本 |
| `GbMonitorPayloadParser.java` | 转 Groovy 预置脚本 |
| `MonitorMetadataService.java` | parser 直接调 IDeviceSensorQueryService |
| `MonitorIngestFacade` 中解析器选择逻辑 | 简化为一行 |

## 八、API 接口

所有接口前缀: `/api/v1/iot/dataParse`

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/list` | 分页查询策略列表 |
| `GET` | `/{id}` | 获取策略详情 |
| `POST` | | 新增策略 |
| `PUT` | | 更新策略 |
| `DELETE` | `/{id}` | 删除（预置禁止） |
| `PUT` | `/{id}/status` | 启用/停用 |
| `POST` | `/test` | 测试脚本（不退库） |

### 校验规则

| 字段 | 规则 |
|------|------|
| `name` | 必填，2-100 字符 |
| `topic_pattern` | 必填，`*` 仅末尾（如 `sys/v1/*`） |
| `script_code` | 必填，保存时 Groovy 编译预检 |
| 预置策略 | DELETE 和 PUT 脚本内容拒绝 |
| 启停 | 停用即时 evict 缓存，启用触发编译预检 |

### 测试接口

```
POST /api/v1/iot/dataParse/test
Request:  { topic, deviceId, scriptCode, testPayload }
Response: { success, executionTime, points, logs[] }
```

## 九、日志与异常

### 日志策略

| 结果 | 动作 |
|------|------|
| 成功 | 更新策略 `last_run_time` |
| 失败 | 写入 `iot_data_parse_log`（仅 ERROR 级别） |

### 异常传播

```
ParseException → 上游 MonitorIngestFacade catch + log.warn
→ 消息丢弃，不进入重试或 DLQ（脚本逻辑错误，重试无意义）
```

### Groovy 安全

- 编译预检：保存/启用时 `GroovyClassLoader.parseClass()` 验证语法
- 执行超时：默认 30 秒
- 一期不做沙箱（脚本仅管理员录入）

## 十、实现顺序

1. 下沉 `StandardMeasurementPoint` → `zwei-common`
2. 建表 + `zwei-iot-parser` 注册到父 POM + `zwei-admin` 引入依赖
3. 实现 parser 核心链路：ParseEngine → StrategyMatcher → GroovyScriptExecutor → ResultValidator
4. 预置 sys 策略脚本（翻译 SysMonitorPayloadParser）
5. 改造 `MonitorIngestFacade`（薄化）
6. 清理 timeseries 解析器残留
7. 实现 REST 控制器
8. Maven 编译验证 + 全链路测试
