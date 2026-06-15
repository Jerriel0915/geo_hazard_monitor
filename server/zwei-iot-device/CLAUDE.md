[根目录](../../CLAUDE.md) > [server](../) > **zwei-iot-device**

# zwei-iot-device — 设备全生命周期 + 跨模块接口中心 + TSL 物模型

> 面包屑: [根目录](../../CLAUDE.md) > [server](../) > **zwei-iot-device**

## 模块职责

IoT 设备域的核心枢纽:

- 设备/传感器 CRUD 与状态管理（含设备自注册 API 流程）
- 设备注册中心 (clientId ↔ deviceId 映射 + 注册幂等日志)
- MQTT 设备鉴权账号 (authUsername/authPassword 生成)
- 设备状态机 (正常 ↔ 故障 ↔ 停用，维修单)
- 设备在线状态 (UPSERT 实时状态表 + 历史事件日志)
- **TSL 物模型 (Product)** — 设备→物模型 JSON 投影，为其他模块提供 IoT 标准语义
- **所有跨模块 Service 接口的定义** — 其他 IoT 子模块 (hazard/video) 通过实现本模块的接口, 供本模块与 `zwei-monitor` 使用

## 关键依赖

- `zwei-common` (BaseEntity, 事件契约, IpUtils, AjaxResult)
- `zwei-iot-monitor` (字典: MonitorType/MonitorContent, 用于注册冷路径)
- `org.projectlombok:lombok`
- MyBatis / Jackson (ObjectMapper)
- (测试) `spring-boot-starter-test`, `junit-jupiter`, AssertJ, Mockito

## 主要子包

| 子包                  | 职责                                                                                                                                                                                                                                        |
|---------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `controller`        | `DeviceController` / `SensorController` / `DeviceRegistryController`                                                                                                                                                                      |
| `service` (本模块实现接口) | `IDeviceService` / `IDeviceSensorService` / `IDeviceRegistryService` / `IDeviceStatusLogService` / `IDeviceSessionService` / `DeviceAuthLogService` / `DeviceOnlineStatusService` / `DeviceRegistrationLogService` / `IProductTslService` |
| `service` (跨模块接口)   | `IDeviceAuthQueryService` / `IDeviceSensorQueryService` / `IDeviceHazardRelationService` / `IDeviceStatService` / `IDeviceQueryService` / `IVideoDeviceStatService` / `ITimeSeriesSchemaService`                                          |
| `service.impl`      | 上述 9 个本模块 Service 的实现 + 7 个跨模块接口的查询/统计实现 (`DeviceAuthQueryServiceImpl` / `DeviceSensorQueryServiceImpl` / `DeviceQueryServiceImpl` / `DeviceStatServiceImpl` / `ProductTslServiceImpl` 等)                                                 |
| `domain`            | `Device` / `DeviceSensor` / `SensorAttribute` / `DeviceAuthLog` / `DeviceOnlineStatus` / `DeviceOnlineEventLog` / `DeviceRegistrationLog` / `DeviceStatusLog` / `SensorMetadata` (record) / `Product` (TSL)                               |
| `domain.dto`        | 设备/传感器 CRUD + 注册请求 DTO (13 个) + `DeviceBriefDTO`                                                                                                                                                                                          |
| `domain.tsl`        | **TSL 物模型记录**: `ProductTsl` / `TslProfile` / `TslProperty` / `TslEvent` / `TslService` / `TslDataType` / `TslDataSpecs`                                                                                                                   |
| `domain.vo`         | `DeviceRegistryResult`                                                                                                                                                                                                                    |
| `mapper`            | MyBatis 数据访问 (9 个 Mapper, 含 ProductMapper)                                                                                                                                                                                                |
| `migration`         | `ProductTslMigrationRunner` — `ApplicationRunner` (@Order(1))，启动时为现有设备全量生成 Product + TSL JSON                                                                                                                                             |
| `tsl`               | `TslBuilder` — 由 deviceCode + 属性列表生成 `ProductTsl` 的纯计算组件 (MD5(productKey) + properties 映射)                                                                                                                                                |
| `support`           | `DeviceAuthAccountGenerator` (6 位大写字母数字 + 8 位字母数字组合)                                                                                                                                                                                      |

## 对外接口 — 业务 Controller

| Controller                 | 路径前缀                                                     | 职责                                                          |
|----------------------------|----------------------------------------------------------|-------------------------------------------------------------|
| `DeviceController`         | `/api/v1/devices`                                        | 设备 CRUD + 复制 + 启停 + 维修 + **账号管理 (查看/重置/启停)** + 维修记录 + 上下线事件 |
| `SensorController`         | `/api/v1/sensors` + `/api/v1/devices/{deviceId}/sensors` | 传感器 CRUD + 属性显式删除 + 下一个 sensorCode 预测                       |
| `DeviceRegistryController` | `/api/v1/device-registry`                                | 设备自注册 API (`@Anonymous`，POST `/register`，幂等校验)              |

### DeviceController 端点清单

| Method | Path                                       | 权限                         | 职责                     |
|--------|--------------------------------------------|----------------------------|------------------------|
| GET    | `/api/v1/devices/page`                     | `basic:device:list`        | 分页查询                   |
| GET    | `/api/v1/devices`                          | `basic:device:list`        | 全量查询                   |
| GET    | `/api/v1/devices/{id}`                     | `basic:device:query`       | 设备详情                   |
| POST   | `/api/v1/devices`                          | `basic:device:add`         | 新增 (返回账号)              |
| PUT    | `/api/v1/devices/{id}`                     | `basic:device:edit`        | 修改                     |
| DELETE | `/api/v1/devices/{id}`                     | `basic:device:remove`      | 逻辑删除                   |
| POST   | `/api/v1/devices/{id}/copy`                | `basic:device:add`         | 复制设备                   |
| GET    | `/api/v1/devices/{deviceId}/sensors`       | `basic:device:query`       | 设备的传感器列表               |
| GET    | `/api/v1/devices/{id}/auth-account`        | `basic:device:auth:view`   | 查看接入账号                 |
| POST   | `/api/v1/devices/{id}/auth-password/reset` | `basic:device:auth:reset`  | 重置密码 (支持 forceOffline) |
| PUT    | `/api/v1/devices/{id}/auth-status`         | `basic:device:auth:status` | 启停账号                   |
| POST   | `/api/v1/devices/{id}/maintenance`         | `basic:device:edit`        | 维修操作 (报修/修复/停用/恢复)     |
| GET    | `/api/v1/devices/{id}/maintenance-logs`    | `basic:device:query`       | 维修记录                   |
| GET    | `/api/v1/devices/{id}/online-logs`         | `basic:device:query`       | 上下线事件                  |

### SensorController 端点清单

| Method | Path                                             | 权限                    | 职责                 |
|--------|--------------------------------------------------|-----------------------|--------------------|
| GET    | `/api/v1/sensors/{id}`                           | `basic:sensor:query`  | 传感器详情              |
| GET    | `/api/v1/sensors/next-code?deviceId={deviceId}`  | `basic:sensor:query`  | 预测下一个可用 sensorCode |
| PUT    | `/api/v1/sensors/{id}`                           | `basic:sensor:edit`   | 修改 (含 attrList)    |
| DELETE | `/api/v1/sensors/{id}`                           | `basic:sensor:remove` | 逻辑删除               |
| DELETE | `/api/v1/sensors/{sensorId}/attributes/{attrId}` | `basic:sensor:edit`   | 显式删除单个属性           |
| POST   | `/api/v1/devices/{deviceId}/sensors`             | `basic:sensor:add`    | 为设备新增传感器           |

## 对外接口 — 跨模块 Service 接口 (供其他模块实现/消费)

| 接口                             | 实现方                                                                               | 消费方                          |
|--------------------------------|-----------------------------------------------------------------------------------|------------------------------|
| `IDeviceAuthQueryService`      | 本模块 (`DeviceAuthQueryServiceImpl`)                                                | MQTT 鉴权时校验                   |
| `IDeviceSensorQueryService`    | 本模块 (`DeviceSensorQueryServiceImpl`，返回 `SensorMetadata` record)                   | 时序模块解析 (设备+传感器+属性聚合)         |
| `IDeviceHazardRelationService` | **zwei-iot-hazard**                                                               | `zwei-monitor` 仪表盘 + 隐患点模块本身 |
| `IDeviceStatService`           | 本模块 (`DeviceStatServiceImpl`，16 个统计方法含完整度/在线/活跃窗口)                                | `zwei-monitor` 仪表盘           |
| `IDeviceQueryService`          | 本模块 (`DeviceQueryServiceImpl`，按 username 返回 `DeviceBriefDTO` 含隐患点名)               | `zwei-monitor` 富化 MQTT 客户端   |
| `IVideoDeviceStatService`      | **zwei-iot-video**                                                                | `zwei-monitor`               |
| `ITimeSeriesSchemaService`     | 本模块 (实现于 `zwei-iot-timeseries.service.impl.TimeSeriesSchemaServiceImpl`，**反向实现**) | 时序模块创建 IoTDB 表               |

> **架构准则**: 本模块**只定义接口**, 不依赖 hazard/video 模块的 Mapper; 真正实现放业务侧, 通过 Spring 自动注入。
>
> **特殊**: `ITimeSeriesSchemaService` 接口在本模块，但实现位于
`zwei-iot-timeseries.service.impl.TimeSeriesSchemaServiceImpl` (因 IoTDB 细节只在 timeseries 模块中可见)。

## 核心实现类索引 (P1)

| 类                                  | 文件                                                   | 关键方法 / 责任                                                                                                                           |
|------------------------------------|------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------|
| `DeviceServiceImpl`                | `service/impl/DeviceServiceImpl.java`                | 设备 CRUD + 状态机 (报修/修复/停用/恢复) + 维修单日志 + 设备复制 + 密码重置 (`@Transactional`) + `copyDevice` 触发 TSL 重建                                       |
| `DeviceSensorServiceImpl`          | `service/impl/DeviceSensorServiceImpl.java`          | 传感器 CRUD + 属性校验 + IoTDB schema 预创建 + **写操作触发 `productTslService.regenerate()`** (`@Transactional`)                                  |
| `DeviceRegistryServiceImpl`        | `service/impl/DeviceRegistryServiceImpl.java`        | 设备自注册 API 流程：幂等校验 (requestId) + SN 一致性 + 子设备递归 + 注册冷路径预建 IoTDB schema + **TSL 初始生成**                                                |
| `ProductTslServiceImpl`            | `service/impl/ProductTslServiceImpl.java`            | Product/TSL 持久化：`getByProductKey` / `getByDeviceId` / `regenerate(deviceId)` (`@Transactional`，重建 JSON 并 UPSERT)                    |
| `ProductTslMigrationRunner`        | `migration/ProductTslMigrationRunner.java`           | `ApplicationRunner` (@Order(1))，启动时检查 `product` 表为空时遍历全设备生成 TSL                                                                     |
| `DeviceOnlineStatusService`        | `service/DeviceOnlineStatusService.java`             | 监听 `DeviceOnlineEvent`/`DeviceOfflineEvent`，UPSERT `device_online_status` + INSERT `device_online_event_log` + `updateLastReportAt` |
| `DeviceAuthLogService`             | `service/DeviceAuthLogService.java`                  | 设备鉴权审计 (成功/失败 + 原因)                                                                                                                 |
| `TslBuilder`                       | `tsl/TslBuilder.java`                                | 由 `deviceCode` (MD5 前 12 位 + `p_` 前缀生成 productKey) + `SensorAttribute[]` 生成 `ProductTsl` (`accessMode=r`, `type=double`)            |
| `DeviceAuthQueryServiceImpl`       | `service/impl/DeviceAuthQueryServiceImpl.java`       | 实现 `IDeviceAuthQueryService`：按 username 查设备 + 更新 lastAuthTime/lastAuthIp                                                            |
| `DeviceSensorQueryServiceImpl`     | `service/impl/DeviceSensorQueryServiceImpl.java`     | 实现 `IDeviceSensorQueryService` (`requireSensorMetadata` 返回 `SensorMetadata` record)                                                 |
| `DeviceQueryServiceImpl`           | `service/impl/DeviceQueryServiceImpl.java`           | 实现 `IDeviceQueryService`：按 username 拿 `DeviceBriefDTO` (含隐患点名称)，批量版本避免 N+1                                                          |
| `DeviceStatServiceImpl`            | `service/impl/DeviceStatServiceImpl.java`            | 实现 `IDeviceStatService`：16 个统计查询 (设备/传感器/隐患点/视频/监测类型 + 运维指标)                                                                        |
| `RegistryServiceImpl`              | `service/impl/DeviceRegistryServiceImpl.java`        | (已在上面)                                                                                                                              |
| `DeviceAuthLogServiceImpl`         | `service/impl/DeviceAuthLogServiceImpl.java`         | 鉴权日志保存                                                                                                                              |
| `DeviceStatusLogServiceImpl`       | `service/impl/DeviceStatusLogServiceImpl.java`       | 设备状态日志保存                                                                                                                            |
| `DeviceRegistrationLogServiceImpl` | `service/impl/DeviceRegistrationLogServiceImpl.java` | 注册日志保存 + 按 requestId 查询                                                                                                             |

## 设备状态机 (来自 `DeviceServiceImpl.maintenanceDevice`)

| operationType | 含义 | oldStatus → newStatus     |
|---------------|----|---------------------------|
| 1             | 报修 | 1 (正常) → 2 (维修)           |
| 2             | 修复 | 2 (维修) → 1 (正常)           |
| 3             | 停用 | 1 (正常) \| 2 (维修) → 3 (停用) |
| 4             | 恢复 | 3 (停用) → 1 (正常)           |

非法转换抛 `ServiceException`。每次操作写 `device_status_log`。

## 账号生成规则 (DeviceAuthAccountGenerator)

- **username**: 6 位大写字母或数字 (`^[A-Z0-9]{6}$`)，例 `NZMX40`
- **password**: 8 位字母数字组合 (`^[A-Za-z0-9]{8}$`)，例 `FSg4n5Z2`
- 创建后不可改 username；密码可通过 `resetDeviceAuthPassword` 强制重置 + 可选 `forceOffline` 主动断连 (
  `IDeviceSessionService.disconnectDevice`)

## 设备自注册流程 (`DeviceRegistryServiceImpl.register`)

1. **幂等校验**: `requestId` 查 `device_registration_log`，已成功返回原设备；已失败抛 409
2. **注册码验证**: `registerCode` 必须在 `zwei.iot.device-registry.register-codes` 允许列表中
3. **SN 冲突检测**: 同 SN 已存在 → 校验请求一致性 + 补齐账号 → 返回已有设备
4. **设备创建**: 6 位 username + 8 位 password，状态正常，registerSource=API
5. **传感器创建**: 按 `monitorTypes` 为每个监测类型建一个传感器 + 属性；子设备递归 (
   `sensorNo = childSn + "_" + baseSensorNo`)
6. **IoTDB Schema 预创建**: 调 `ITimeSeriesSchemaService.createSensorSchema()` (注册冷路径)
7. **TSL 初始生成**: 调 `IProductTslService.regenerate(deviceId)` 生成 Product 物模型
8. **失败回滚**: 抛异常时写 `device_registration_log` (result_status=FAIL) + 向上抛

## TSL 物模型 (Product)

为对齐 IoT 标准物模型 (Thing Specification Language)，引入 Product 概念作为 Device 的语义投影层。

### Product 与 Device 关系

- 当前 **1:1** 映射（`product.device_id` UNIQUE），数据模型预留 1:N 模板复用
- Product 是**定义层** (TSL JSON)，现有 `device`/`device_sensor`/`sensor_attribute` 表是**实例层** (数据主源)
- TSL 是**只读投影**，由现有表单向生成，业务写入操作在同一事务中触发 regenerate

### product 表 (新增)

| 字段            | 类型                   | 说明                                       |
|---------------|----------------------|------------------------------------------|
| `id`          | `bigint` PK          | 主键                                       |
| `product_key` | `varchar(64)` UNIQUE | 设备 `code` MD5 前 12 位 + `p_` 前缀           |
| `device_id`   | `bigint` UNIQUE      | 关联设备ID (1:1)                             |
| `tsl_json`    | `json` NOT NULL      | 完整 TSL JSON (properties/events/services) |
| `tsl_version` | `varchar(32)`        | TSL 版本号 (当前 `1.0`)                       |
| `del_flag`    | `tinyint`            | 逻辑删除                                     |

### TSL JSON 结构 (示例)

```json
{
  "schema": "https://iot.example.com/tsl/v1",
  "profile": { "productKey": "p_a1b2c3d4e5f6" },
  "properties": [
    {
      "identifier": "rainfall_hour",
      "name": "小时降雨量",
      "accessMode": "r",
      "required": true,
      "dataType": {
        "type": "double",
        "specs": {
          "min": "0", "max": "500.00",
          "unit": "mm", "unitName": "毫米",
          "step": "0.1"
        }
      }
    }
  ],
  "events": [],
  "services": []
}
```

### TSL 字段映射

| TSL 字段                                | 数据来源                                       |
|---------------------------------------|--------------------------------------------|
| `profile.productKey`                  | `device.code` → MD5 → `"p_" + 12位`         |
| `properties[].identifier`             | `sensor_attribute.attr_code`               |
| `properties[].name`                   | `sensor_attribute.attr_name`               |
| `properties[].accessMode`             | 固定 `"r"`（只读）                               |
| `properties[].dataType.type`          | 固定 `"double"`                              |
| `properties[].dataType.specs.min/max` | `sensor_attribute.range_min` / `range_max` |
| `properties[].dataType.specs.unit`    | `sensor_attribute.unit`                    |

### TSL 触发时机 (同一事务)

| 操作      | 触发位置                                              |
|---------|---------------------------------------------------|
| 设备自注册   | `DeviceRegistryServiceImpl.register()`            |
| 设备复制    | `DeviceServiceImpl.copyDevice()`                  |
| 新增传感器   | `DeviceSensorServiceImpl.insertSensor()`          |
| 修改传感器   | `DeviceSensorServiceImpl.updateSensor()`          |
| 删除传感器   | `DeviceSensorServiceImpl.deleteSensorById()`      |
| 删除传感器属性 | `DeviceSensorServiceImpl.deleteSensorAttribute()` |

失败时全部回滚 (Product/TSL 与业务表共享 `@Transactional` 边界)。

### 启动迁移 (`ProductTslMigrationRunner`)

`ApplicationRunner` (`@Order(1)`) 在 Spring Boot 启动时执行:

1. `productMapper.countAll()` > 0 → 已迁移完成，跳过
2. 否则遍历 `deviceMapper.selectDeviceAll()`，逐个调 `productTslService.regenerate(deviceId)`
3. 记录 `migrated` / `failed` / total 数量日志

### TSL 内部消费场景 (Java 模块间流转)

| 消费模块                   | 用途                                      |
|------------------------|-----------------------------------------|
| `zwei-iot-timeseries`  | 数据解析时通过 TSL `dataType.specs` 做值域校验 (可选) |
| `zwei-iot-device` (自身) | 自注册时生成初始 TSL 用于 IoTDB schema 预创建        |
| `zwei-iot-alarm`       | 告警判据引用 TSL property identifier 与阈值 (可选) |
| 未来扩展                   | events / services 字段已预留，当前序列化为空数组 `[]`  |

> **API 层**: **不新增**任何 TSL 相关 REST 端点。TSL 仅在 Java 模块间通过 `IProductTslService` 内部流转。
> 设计决策见 `docs/superpowers/specs/2026-06-11-tsl-product-model-design.md`。

## 缓存使用

- `DeviceServiceImpl.resetDeviceAuthPassword` 通过 `ObjectProvider<IDeviceSessionService>` 可选注入 (broker
  模块可能不存在)
- `HazardPointServiceImpl` 在 `bindDevices`/`unbindDevices` 时 `@CacheEvict(value="hazardPoint", key="#hazardPointId")`
  触发隐患点缓存失效

## 数据模型

- `device` — 设备主表 (id / code UNIQUE / sn / name / deviceType / networkType / protocolType: MQTT|HTTP|COAP /
  registerSource: MANUAL|API|IMPORT / authUsername UNIQUE char(6) / authPassword varchar(32) 明文 / authStatus / icon /
  iconPath / status: 1-正常 2-维修 3-停用 / lastReportTime / lastAuthTime / lastAuthIp / longitude / latitude)
- `device_sensor` — 传感器 (id / deviceId / deviceCode / sensorCode UNIQUE / sensorNo UNIQUE(deviceId,sensorNo) /
  sensorName / monitorTypeId / monitorTypeCode / monitorTypeName / status / lastReportTime)
- `sensor_attribute` — 传感器属性 (id / sensorId / monitorContentId / attrCode / attrName / initialValue / unit /
  rangeMin / rangeMax / icon)
- `device_auth_log` — 鉴权日志 (deviceId / authUsername / authResult: 1/0 / clientId / clientIp / failureReason)
- `device_registration_log` — 注册日志 (requestId UNIQUE / registerCode / registerSource / deviceId / sn / resultStatus:
  SUCCESS/FAIL / failureReason / requestBody JSON)
- `device_online_status` — 实时状态 (deviceId UNIQUE / clientId / status 0=离线 1=在线 / onlineAt / offlineAt /
  lastReportAt / sessionDurationSec)
- `device_online_event_log` — 上下线历史 (deviceId / eventType: ONLINE|OFFLINE|HEARTBEAT / clientId / clientIp /
  eventTime / reason)
- `device_status_log` — 状态变更历史 (deviceId / deviceCode / oldStatus / newStatus / statusText / operatorName /
  operatorPhone / operationDate / description)
- **`product`** — 产品物模型 (id / productKey UNIQUE / deviceId UNIQUE / tslJson / tslVersion / delFlag) — TSL JSON 存储

## 测试与质量

| 测试类                              | 行数  | 覆盖                                        |
|----------------------------------|-----|-------------------------------------------|
| `DeviceRegistryServiceImplTest`  | 173 | 注册流程 (幂等/SN冲突/注册码/传感器创建/IoTDB schema/TSL) |
| `DeviceSensorServiceImplTest`    | 259 | 传感器 CRUD + attrList + TSL 触发              |
| `DeviceServiceImplTest`          | 199 | 设备 CRUD + 状态机 + 维修 + 复制 + 密码重置            |
| `ProductTslServiceImplTest`      | 146 | Product/TSL 持久化 + regenerate              |
| `DeviceAuthAccountGeneratorTest` | 38  | 账号生成规则                                    |
| `TslBuilderTest`                 | 116 | TSL 构建 (productKey 稳定性/属性映射/边界)           |

- 单测: 接口契约 (mock hazard/video 实现)
- 集成测试: 启动时验证所有跨模块接口都有实现 (否则启动失败) + `ProductTslMigrationRunner` 全量迁移
- 覆盖率目标 80%

## 常见问题 (FAQ)

**Q: 新增设备如何获取 MQTT clientId?**
A: 调 `POST /api/v1/device-registry/register` 创建注册记录, 返回 `authUsername` (即 MQTT username) + `authPassword` + 初始
deviceCode。

**Q: 设备心跳怎么更新?**
A: 三条链路: 1) `zwei-iot-timeseries.MonitorIngestConsumerService` 写入 IoTDB 成功后调
`deviceOnlineStatusService.updateLastReportAt`; 2) 同链路 `deviceSensorService.updateLastReportTime`; 3) 兼容保留
`device.lastReportTime`。

**Q: 跨模块接口的实现类放哪里?**
A: 实现类放**业务侧** (如 hazard 的 `IDeviceHazardRelationServiceImpl` 在 `zwei-iot-hazard`), 本模块只放接口 +
自己的实现 (`DeviceXxxServiceImpl` + `DeviceXxxQueryServiceImpl` + `DeviceXxxStatServiceImpl`)。

**Q: 密码重置 + forceOffline 怎么实现?**
A: `DeviceServiceImpl.resetDeviceAuthPassword` → `IDeviceSessionService.disconnectDevice(deviceId)` (通过
`ObjectProvider` 可选注入, broker 模块可能不存在时降级为日志告警)。

**Q: TSL 物模型有什么用?**
A: TSL 是设备语义的 IoT 标准投影，对内供 timeseries/alarm 模块做值域校验和判据引用；对外**暂不暴露** REST API。
当前版本固定 `accessMode=r` 与 `dataType.type=double`，未来可扩展 events/services。

**Q: 改了传感器/属性为什么 Product 表会自动更新?**
A: `DeviceSensorServiceImpl` 的 insert/update/delete + attr delete 四个方法都在 `@Transactional` 中调
`productTslService.regenerate(deviceId)`，业务表与 Product 表在同一个事务内原子提交。

**Q: product 表为空会影响功能吗?**
A: 启动时 `ProductTslMigrationRunner` 自动为所有现有设备生成 Product，不会影响业务运行。运行中无 Product 的设备读 TSL 时
`getByProductKey` / `getByDeviceId` 会抛 `ServiceException`，调用方需注意。

## 相关文件清单

- `pom.xml`
- `src/main/java/com/zwei/iot/device/controller/DeviceController.java`
- `src/main/java/com/zwei/iot/device/controller/SensorController.java`
- `src/main/java/com/zwei/iot/device/controller/DeviceRegistryController.java`
- `src/main/java/com/zwei/iot/device/service/impl/DeviceServiceImpl.java` (P1)
- `src/main/java/com/zwei/iot/device/service/impl/DeviceSensorServiceImpl.java` (P1)
- `src/main/java/com/zwei/iot/device/service/impl/DeviceRegistryServiceImpl.java` (P1)
- `src/main/java/com/zwei/iot/device/service/impl/ProductTslServiceImpl.java` (P1)
- `src/main/java/com/zwei/iot/device/migration/ProductTslMigrationRunner.java` (P1)
- `src/main/java/com/zwei/iot/device/tsl/TslBuilder.java` (P1)
- `src/main/java/com/zwei/iot/device/service/DeviceOnlineStatusService.java` (P1)
- `src/main/java/com/zwei/iot/device/service/IProductTslService.java`
- `src/main/java/com/zwei/iot/device/service/IDevice*Service.java` (本模块接口)
- `src/main/java/com/zwei/iot/device/service/I*QueryService.java` / `I*StatService.java` (跨模块接口)
- `src/main/java/com/zwei/iot/device/domain/Product.java`
- `src/main/java/com/zwei/iot/device/domain/tsl/*.java` (7 个 record)
- `src/main/java/com/zwei/iot/device/domain/SensorMetadata.java`
- `src/main/java/com/zwei/iot/device/support/DeviceAuthAccountGenerator.java`

## 变更记录 (Changelog)

| 时间               | 变更                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
|------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 2026-06-10 18:52 | 首次生成模块级 CLAUDE.md (架构师自动扫描)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
| 2026-06-10 19:08 | 增量补扫: 新增核心实现类索引、设备状态机表、账号生成规则、设备自注册 7 步流程、缓存/ObjectProvider 使用说明、跨模块接口完整方法列表                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
| 2026-06-14 16:21 | **TSL 物模型(Product)接入**: 新增 `domain/Product.java` + `domain/tsl/` (7 record) + `mapper/ProductMapper.java` + `service/IProductTslService.java` + `service/impl/ProductTslServiceImpl.java` + `tsl/TslBuilder.java` + `migration/ProductTslMigrationRunner.java`; `DeviceSensorServiceImpl` 四个写方法 + `DeviceServiceImpl.copyDevice` + `DeviceRegistryServiceImpl.register` 触发 `regenerate`; 新增 `domain/SensorMetadata` record + `domain/dto/DeviceBriefDTO`; `DeviceController` 扩展 6 个端点 (auth-account/auth-password/reset/auth-status/maintenance/maintenance-logs/online-logs); `SensorController` 新增 next-code + 显式 attribute 删除; 6 个单测类覆盖注册流程/传感器/设备/TSL/账号生成/TSL构建 |