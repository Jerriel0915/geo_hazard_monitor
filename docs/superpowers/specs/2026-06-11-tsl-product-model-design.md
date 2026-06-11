# TSL 物模型 — Product 概念引入设计

日期：2026-06-11
状态：已确认

## 目标

在设备管理系统中引入 TSL（Thing Specification Language）物模型的 Product 概念，使设备模型对齐 IoT 标准。

## 核心决策

| # | 决策 | 选择 |
|---|------|------|
| 1 | Product 与 Device 关系 | **1:1 映射**起步，数据模型预留 1:N 模板复用 |
| 2 | TSL 存储方式 | **JSON 文档 + 现有表并存**，TSL 是定义层，现表是实例层 |
| 3 | 监测字典 | **保留**，与 TSL 互补，TSL property 的 identifier 引用字典 attr_code |
| 4 | 现有数据迁移 | **全量迁移**，为所有现有 Device 生成 Product + TSL |
| 5 | 实现策略 | **方案 A — TSL 作为只读投影**，现有表为数据主源，TSL 单向生成 |
| 6 | TSL 对外暴露 | **不对外暴露 REST API**，仅通过内部 Service 接口在模块间流转 |

## 数据库变更

### 新增 `product` 表

```sql
CREATE TABLE `product` (
  `id`            bigint       NOT NULL AUTO_INCREMENT,
  `product_key`   varchar(64)  NOT NULL COMMENT '产品唯一标识，由device.code哈希生成',
  `device_id`     bigint       NOT NULL COMMENT '关联设备ID，当前1:1',
  `tsl_json`      json         NOT NULL COMMENT '完整TSL JSON（properties/events/services）',
  `tsl_version`   varchar(32)  DEFAULT '1.0' COMMENT 'TSL版本号',
  `create_by`     varchar(64)  DEFAULT NULL,
  `create_time`   datetime     DEFAULT CURRENT_TIMESTAMP,
  `update_by`     varchar(64)  DEFAULT NULL,
  `update_time`   datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag`      tinyint      DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_product_key` (`product_key`),
  UNIQUE KEY `uk_device_id` (`device_id`),
  KEY `idx_product_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='产品物模型表';
```

- `product_key` 由 `device.code` MD5 取前 12 位 + `p_` 前缀生成
- `device_id` UNIQUE 约束保证 1:1；未来放宽为 1:N 只需移除此约束
- `tsl_version` 预留版本管理，当前 "1.0"

## TSL JSON 结构

```json
{
  "schema": "https://iot.example.com/tsl/v1",
  "profile": {
    "productKey": "p_a1b2c3d4e5f6"
  },
  "properties": [
    {
      "identifier": "rainfall_hour",
      "name": "小时降雨量",
      "accessMode": "r",
      "required": true,
      "dataType": {
        "type": "double",
        "specs": {
          "min": "0",
          "max": "500.00",
          "unit": "mm",
          "unitName": "毫米",
          "step": "0.1"
        }
      }
    }
  ],
  "events": [],
  "services": []
}
```

### 映射关系

| TSL 字段 | 数据来源 |
|-----------|----------|
| `profile.productKey` | `device.code` → MD5 → `"p_" + prefix` |
| `properties[].identifier` | `sensor_attribute.attr_code` |
| `properties[].name` | `sensor_attribute.attr_name` |
| `properties[].accessMode` | 当前固定 `"r"`（只读），后续可扩展 |
| `properties[].dataType.type` | 从监测字典推断，默认 `"double"` |
| `properties[].dataType.specs.min/max` | `sensor_attribute.range_min` / `range_max` |
| `properties[].dataType.specs.unit` | `sensor_attribute.unit` |

## TSL 生成与同步

### ProductTslService

```java
public interface IProductTslService {
    ProductTsl getByProductKey(String productKey);
    ProductTsl getByDeviceId(Long deviceId);
    void regenerate(Long deviceId);  // 重建 TSL JSON 并 UPSERT product 表
}
```

位于 `zwei-iot-device` 模块，其他模块通过 Maven 依赖调用。

### 触发时机

| 操作 | 触发位置 |
|------|----------|
| 创建/更新/删除传感器 | `DeviceSensorServiceImpl` |
| 创建/更新/删除属性 | SensorAttribute 对应 Service |
| 设备自注册 | `DeviceRegistryServiceImpl.register` |
| 设备复制 | `DeviceServiceImpl.copyDevice` |

所有触发与业务操作在 **同一事务** 中，通过 `@Transactional` 保证一致性。

### 同步保证

```
写操作 ──事务──┬── device_sensor / sensor_attribute 写入
               └── product.tsl_json UPSERT (regenerate)
```

失败时全部回滚。

## 内部消费场景

| 消费模块 | 用途 |
|----------|------|
| `zwei-iot-timeseries` | 数据解析时，通过 TSL dataType.specs 做值域校验 |
| `zwei-iot-device` | 自注册时生成初始 TSL，用于 IoTDB schema 预创建 |
| `zwei-iot-alarm` | 告警判据引用 TSL property identifier 和阈值 |
| 未来扩展 | events/services 的消费 |

## Events 和 Services 预留

- `ProductTsl` 领域对象包含完整的 `TslEvent` 和 `TslService` 字段定义
- TSL JSON 中 `events` 和 `services` 当前序列化为空数组 `[]`
- `tsl_version` 字段预留，后续扩展时升至 `"2.0"`

## API 层

- **不新增**任何 TSL 相关 REST 端点
- 现有 Device/Sensor API **完全不变**
- TSL 仅在 Java 模块间通过 `IProductTslService` 内部流转

## 迁移计划

### 数据库迁移

`db/upgrade/v2.1_add_product_tsl.sql`：创建 `product` 表（DDL）

Java 迁移组件 `ProductTslMigrationRunner`（实现 `ApplicationRunner`）：
1. 启动时检查 `product` 表是否为空
2. 遍历所有 `del_flag=0` 的 Device，调用 `TslBuilder` 生成 TSL JSON
3. 批量 INSERT 到 `product` 表
4. 迁移完成后标记完成，后续启动跳过

### 代码变更范围

| 模块 | 变更 |
|------|------|
| `zwei-iot-device` | 新增 Product 实体、ProductMapper、IProductTslService/ProductTslServiceImpl、TslBuilder；DeviceSensorServiceImpl 等写操作中注入 regenerate 调用 |
| `zwei-iot-timeseries` | 可选：值域校验时调用 IProductTslService |
| `zwei-iot-alarm` | 可选：告警判据引用 TSL property |
| `db/` | 新增升级脚本 |

## 不纳入本期

- TSL 的 REST API 暴露
- events/services 的生成逻辑
- TSL 文件的导入/导出
- 监测字典变更触发 TSL 重建（影响面太大，后续再议）
- 1:N 产品模板复用（仅数据模型预留）
