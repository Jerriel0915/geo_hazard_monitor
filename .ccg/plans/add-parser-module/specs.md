# Spec: 数据解析转化层 — 独立 parser 模块

## 需求概述

为所有设备 MQTT 上报报文增加统一数据解析转化层。不同 MQTT 主题路由到不同解析器，解析器统一输出 `StandardMeasurementPoint` 作为系统内部流通数据格式。

## 目标

1. 创建独立 Maven 模块 `zwei-iot-parser`，集中管理所有报文解析逻辑
2. 打破 `zwei-iot-broker` → `zwei-iot-timeseries` 反向依赖（通过 Spring Event 解耦）
3. 从 `zwei-iot-timeseries` 移除 parser 相关代码
4. 为未来策略表 + Blockly 引擎预留 SPI 扩展点

## 非目标 (本次不实现)

- DB 策略表 (`iot_data_parse_strategy` 等 4 张表)
- Blockly 可视化脚本引擎
- JavaScript/GraalVM 脚本执行引擎
- 解析策略 CRUD API

## 约束

- **Java 17**, Spring Boot 4.0.3, Maven 多模块
- 新模块为纯函数库风格，**零 IoT 依赖**（不依赖 iotdb-jdbc / mica-mqtt / Redis）
- 仅依赖 `zwei-common`（工具类 + 异常）
- 新模块不依赖 `zwei-iot-device` — `SensorMetadata` 通过接口抽象解耦
- 所有现有 MQTT 消息流不可中断
- Maven 编译必须 100% 通过（14→15 模块）

## 架构目标

### Before (problematic)
```
zwei-iot-broker ──depends on──▶ zwei-iot-timeseries (reverse!)
    └─ calls MonitorIngestFacade.ingest() directly
```

### After
```
zwei-iot-parser (new leaf module)
    ↑ depends on           ↑ depends on
zwei-iot-broker       zwei-iot-timeseries
    │                       │
    └── MqttMessageReceivedEvent ──▶ @EventListener in timeseries
         (Spring event, no compile-time dep)
```

## PBT Properties

| # | Property | Falsification Strategy |
|---|----------|----------------------|
| P1 | **解析幂等性**: 同一报文两次解析结果一致 | 修改 payloadHash / receiveTime 等时间敏感字段 |
| P2 | **路由确定性**: 同一 topic 始终路由到同一 parser | 注入 supports() 重叠的解析器 |
| P3 | **Topic 解析一致性**: `parse("sys/v1/X/Y/updata").sourceType() == "sys"` | 畸形 topic 输入 |
| P4 | **输出完整性**: 每个 StandardMeasurementPoint 必须有 deviceId/sensorNo/attrCode/value/dataTime | null/缺失字段的边界 payload |
| P5 | **模块边界**: parser 模块不依赖 timeseries/broker/iotdb | `mvn dependency:tree -pl zwei-iot-parser` |
| P6 | **事件解耦**: broker 不 import MonitorIngestFacade | `rg "MonitorIngestFacade" server/zwei-iot-broker/` |
