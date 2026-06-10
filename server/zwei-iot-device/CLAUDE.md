[根目录](../../CLAUDE.md) > [server](../) > **zwei-iot-device**

# zwei-iot-device — 设备全生命周期 + 跨模块接口中心

> 面包屑: [根目录](../../CLAUDE.md) > [server](../) > **zwei-iot-device**

## 模块职责

IoT 设备域的核心枢纽:

- 设备/传感器 CRUD 与状态管理（含设备自注册 API 流程）
- 设备注册中心 (clientId ↔ deviceId 映射 + 注册幂等日志)
- MQTT 设备鉴权账号 (authUsername/authPassword 生成)
- 设备状态机 (正常 ↔ 故障 ↔ 停用，维修单)
- 设备在线状态 (UPSERT 实时状态表 + 历史事件日志)
- **所有跨模块 Service 接口的定义** — 其他 IoT 子模块 (hazard/video) 通过实现本模块的接口, 供本模块与 `zwei-monitor` 使用

## 关键依赖

- `zwei-common`
- `zwei-iot-monitor` (字典: MonitorType/MonitorContent)
- MyBatis / lombok
- (测试) spring-boot-starter-test, junit-jupiter

## 主要子包

| 子包                | 职责                                                                                                                                                                                                                 |
|-------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `controller`      | `DeviceController` / `SensorController` / `DeviceRegistryController`                                                                                                                                               |
| `service` (本模块实现) | `IDeviceService` / `IDeviceSensorService` / `IDeviceRegistryService` / `IDeviceStatusLogService` / `IDeviceSessionService` / `DeviceAuthLogService` / `DeviceOnlineStatusService` / `DeviceRegistrationLogService` |
| `service` (跨模块接口) | `IDeviceAuthQueryService` / `IDeviceSensorQueryService` / `IDeviceHazardRelationService` / `IDeviceStatService` / `IDeviceQueryService` / `IVideoDeviceStatService` / `ITimeSeriesSchemaService`                   |
| `service.impl`    | 上述 8 个本模块 Service 的实现 + 6 个跨模块接口的查询/统计实现 (`DeviceAuthQueryServiceImpl` / `DeviceSensorQueryServiceImpl` / `DeviceQueryServiceImpl` / `DeviceStatServiceImpl` 等)                                                    |
| `domain`          | `Device` / `DeviceSensor` / `SensorAttribute` / `DeviceAuthLog` / `DeviceOnlineStatus` / `DeviceOnlineEventLog` / `DeviceRegistrationLog` / `DeviceStatusLog` / `SensorMetadata` (record)                          |
| `domain.dto`      | 设备/传感器 CRUD + 注册请求 DTO (13 个)                                                                                                                                                                                      |
| `domain.vo`       | `DeviceRegistryResult`                                                                                                                                                                                             |
| `mapper`          | MyBatis 数据访问 (8 个 Mapper)                                                                                                                                                                                          |
| `support`         | `DeviceAuthAccountGenerator` (6 位大写字母数字 + 8 位字母数字组合)                                                                                                                                                               |

## 对外接口 — 业务 Controller

| 路径                              | 职责                        |
|---------------------------------|---------------------------|
| `/api/v1/iot/device/*`          | 设备 CRUD + 启停/删除/导入/维修     |
| `/api/v1/iot/sensor/*`          | 传感器 CRUD + 启停             |
| `/api/v1/iot/device-registry/*` | 设备自注册 API (POST: 设备侧主动注册) |

## 对外接口 — 跨模块 Service 接口 (供其他模块实现/消费)

| 接口                             | 实现方                                                                               | 消费方                        |
|--------------------------------|-----------------------------------------------------------------------------------|----------------------------|
| `IDeviceAuthQueryService`      | 本模块 (`DeviceAuthQueryServiceImpl`)                                                | MQTT 鉴权时校验                 |
| `IDeviceSensorQueryService`    | 本模块 (`DeviceSensorQueryServiceImpl`)                                              | 时序模块/告警                    |
| `IDeviceHazardRelationService` | **zwei-iot-hazard**                                                               | `zwei-monitor` 仪表盘         |
| `IDeviceStatService`           | 本模块 (`DeviceStatServiceImpl`)                                                     | `zwei-monitor` 仪表盘         |
| `IDeviceQueryService`          | 本模块 (`DeviceQueryServiceImpl`)                                                    | `zwei-monitor` 富化 MQTT 客户端 |
| `IVideoDeviceStatService`      | **zwei-iot-video**                                                                | `zwei-monitor`             |
| `ITimeSeriesSchemaService`     | 本模块 (实现于 `zwei-iot-timeseries.service.impl.TimeSeriesSchemaServiceImpl`，**反向实现**) | 时序模块创建 IoTDB 表             |

> **架构准则**: 本模块**只定义接口**, 不依赖 hazard/video 模块的 Mapper; 真正实现放业务侧, 通过 Spring 自动注入。
>
> **特殊**: `ITimeSeriesSchemaService` 接口在本模块，但实现位于
`zwei-iot-timeseries.service.impl.TimeSeriesSchemaServiceImpl` (因 IoTDB 细节只在 timeseries 模块中可见)。

## 核心实现类索引 (P1)

| 类                                  | 文件                                                   | 关键方法 / 责任                                                                                                                           |
|------------------------------------|------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------|
| `DeviceServiceImpl`                | `service/impl/DeviceServiceImpl.java`                | 设备 CRUD + 状态机 (报修/修复/停用/恢复) + 维修单日志 + 设备复制 + 密码重置 (`@Transactional`)                                                                |
| `DeviceSensorServiceImpl`          | `service/impl/DeviceSensorServiceImpl.java`          | 传感器 CRUD + 属性校验 + IoTDB schema 预创建 (`@Transactional`)                                                                               |
| `DeviceRegistryServiceImpl`        | `service/impl/DeviceRegistryServiceImpl.java`        | 设备自注册 API 流程：幂等校验 (requestId) + SN 一致性 + 子设备递归 + 注册冷路径预建 IoTDB schema                                                               |
| `DeviceOnlineStatusService`        | `service/DeviceOnlineStatusService.java`             | 监听 `DeviceOnlineEvent`/`DeviceOfflineEvent`，UPSERT `device_online_status` + INSERT `device_online_event_log` + `updateLastReportAt` |
| `DeviceAuthLogService`             | `service/DeviceAuthLogService.java`                  | 设备鉴权审计 (成功/失败 + 原因)                                                                                                                 |
| `DeviceAuthQueryServiceImpl`       | `service/impl/DeviceAuthQueryServiceImpl.java`       | 实现 `IDeviceAuthQueryService`：按 username 查设备 + 更新 lastAuthTime/lastAuthIp                                                            |
| `DeviceSensorQueryServiceImpl`     | `service/impl/DeviceSensorQueryServiceImpl.java`     | 实现 `IDeviceSensorQueryService`                                                                                                      |
| `DeviceQueryServiceImpl`           | `service/impl/DeviceQueryServiceImpl.java`           | 实现 `IDeviceQueryService`：批量按 username 拿设备简要信息 (`getDeviceBriefsByAuthUsernames`)                                                    |
| `DeviceStatServiceImpl`            | `service/impl/DeviceStatServiceImpl.java`            | 实现 `IDeviceStatService`：所有设备/传感器/隐患点/视频统计查询                                                                                         |
| `DeviceRegistryServiceImpl`        | `service/impl/DeviceRegistryServiceImpl.java`        | (已在上面)                                                                                                                              |
| `DeviceAuthLogServiceImpl`         | `service/impl/DeviceAuthLogServiceImpl.java`         | 鉴权日志保存                                                                                                                              |
| `DeviceStatusLogServiceImpl`       | `service/impl/DeviceStatusLogServiceImpl.java`       | 设备状态日志保存                                                                                                                            |
| `DeviceRegistrationLogServiceImpl` | `service/impl/DeviceRegistrationLogServiceImpl.java` | 注册日志保存                                                                                                                              |

## 设备状态机 (来自 `DeviceServiceImpl.maintenanceDevice`)

| operationType | 含义 | oldStatus → newStatus     |
|---------------|----|---------------------------|
| 1             | 报修 | 1 (正常) → 2 (故障)           |
| 2             | 修复 | 2 (故障) → 1 (正常)           |
| 3             | 停用 | 1 (正常) \| 2 (故障) → 3 (停用) |
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
7. **失败回滚**: 抛异常时写 `device_registration_log` (result_status=FAIL) + 向上抛

## 缓存使用

- `DeviceServiceImpl.resetDeviceAuthPassword` 通过 `ObjectProvider<IDeviceSessionService>` 可选注入 (broker
  模块可能不存在)
- `HazardPointServiceImpl` 在 `bindDevices`/`unbindDevices` 时 `@CacheEvict(value="hazardPoint", key="#hazardPointId")`
  触发隐患点缓存失效

## 数据模型

- `device` — 设备主表 (id / code UNIQUE / sn / name / deviceType / networkType / protocolType: MQTT|HTTP|COAP /
  registerSource: MANUAL|API|IMPORT / authUsername UNIQUE char(6) / authPassword varchar(32) 明文 / authStatus / icon /
  iconPath / status: 1-正常 2-故障 3-离线 / lastReportTime / lastAuthTime / lastAuthIp / longitude / latitude)
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

## 测试与质量

- 单测: 接口契约 (mock hazard/video 实现)
- 集成测试: 启动时验证所有跨模块接口都有实现 (否则启动失败)
- 覆盖率目标 80%

## 常见问题 (FAQ)

**Q: 新增设备如何获取 MQTT clientId?**
A: 调 `POST /api/v1/iot/device-registry` 创建注册记录, 返回 `authUsername` (即 MQTT username) + `authPassword` + 初始
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

## 相关文件清单

- `pom.xml`
- `src/main/java/com/zwei/iot/device/controller/DeviceController.java`
- `src/main/java/com/zwei/iot/device/controller/SensorController.java`
- `src/main/java/com/zwei/iot/device/controller/DeviceRegistryController.java`
- `src/main/java/com/zwei/iot/device/service/impl/DeviceServiceImpl.java` (P1)
- `src/main/java/com/zwei/iot/device/service/impl/DeviceSensorServiceImpl.java` (P1)
- `src/main/java/com/zwei/iot/device/service/impl/DeviceRegistryServiceImpl.java` (P1)
- `src/main/java/com/zwei/iot/device/service/DeviceOnlineStatusService.java` (P1)
- `src/main/java/com/zwei/iot/device/service/IDevice*Service.java` (本模块接口)
- `src/main/java/com/zwei/iot/device/service/I*QueryService.java` / `I*StatService.java` (跨模块接口)
- `src/main/java/com/zwei/iot/device/support/DeviceAuthAccountGenerator.java`

## 变更记录 (Changelog)

| 时间               | 变更                                                                           |
|------------------|------------------------------------------------------------------------------|
| 2026-06-10 18:52 | 首次生成模块级 CLAUDE.md (架构师自动扫描)                                                  |
| 2026-06-10 19:08 | 增量补扫: 新增核心实现类索引、设备状态机表、账号生成规则、设备自注册 7 步流程、缓存/ObjectProvider 使用说明、跨模块接口完整方法列表 |
