[根目录](../../CLAUDE.md) > [server](../) > **zwei-iot-device**

# zwei-monitor — 系统监控 (服务器健康 + Redis + MQTT Broker + 仪表盘)

> 面包屑: [根目录](../../CLAUDE.md) > [server](../) > **zwei-iot-device**

## 模块职责

统一对外暴露**系统级**监控指标与 **MQTT Broker 运行状态**, 包含:

- 仪表盘统计 (健康分/在线率/活跃率/趋势/分布) — 一体化 `getFull()` 聚合
- MQTT Broker 状态 + 客户端连接 + 踢人/封禁
- 设备在线状态聚合
- 与 mica-mqtt 内置 HTTP API (端口 18083) 集成

## 关键依赖

- `zwei-common` (基础)
- `zwei-framework` (安全/权限)
- `zwei-iot-device` (**仅通过 Service 接口** `IDeviceStatService`/`IDeviceQueryService`)
- `spring-boot-starter-web` (提供 RestTemplate)
- lombok (provided)

## 主要子包

| 子包                 | 路径                                                                                                                                 | 职责                                                      |
|--------------------|------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------|
| `controller`       | 4 个 Controller                                                                                                                     | REST API 入口                                             |
| `service`          | `DashboardStatService` / `MqttSessionEnrichService`                                                                                | 仪表盘聚合 + MQTT 富化                                         |
| `client`           | `MqttHttpApiClient`                                                                                                                | mica-mqtt HTTP API 客户端 (RestTemplate)                   |
| `config`           | `MqttHttpApiProperties` / `RestTemplateConfig`                                                                                     | 读取 `mqtt.server.http-listener.*` 配置 + RestTemplate Bean |
| `domain`           | `MqttClientInfo` / `MqttClientPageResponse` / `MqttConfigInfo` / `MqttListenerInfo` / `MqttStatsResponse` / `MqttSubscriptionInfo` | mica-mqtt API 反序列化 VO                                   |
| `domain.dashboard` | `DashboardFullVO` / `DashboardOverviewVO` / `HealthScoreVO` / `HazardPointTrendVO` / `RateByTypeVO` / `SensorDistributionVO`       | 仪表盘返回 VO                                                |

## 对外接口 (Controller)

| 控制器                         | 路径前缀                           | 职责                               |
|-----------------------------|--------------------------------|----------------------------------|
| `MqttStatsController`       | `/api/v1/monitor/mqtt`         | MQTT Broker 状态/监听器/运行时参数         |
| `MqttClientController`      | `/api/v1/monitor/mqtt/clients` | 已连接客户端列表 (含设备/隐患点名) + 详情 + 踢人/封禁 |
| `MonitorOverviewController` | `/api/v1/monitor`              | 总览聚合                             |
| `DashboardStatController`   | `/api/v1/monitor/dashboard`    | 仪表盘指标 + `/full` 完整聚合             |

## 关键 Service 接口 (来自 zwei-iot-device, 仅接口)

- `IDeviceStatService` — 设备统计 (在线/总数/活跃率/按状态/按监测类型)
- `IDeviceQueryService` — 设备查询 (按 username 批量取设备简要信息，避免 N+1)

> **重要**: 本模块**禁止**直接依赖 `zwei-iot-device` 的 Mapper / Domain, 必须通过 Service 接口。

## 核心实现类索引 (P2)

| 类                          | 文件                                      | 关键方法 / 责任                                                                                                                                                                                                     |
|----------------------------|-----------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `DashboardStatService`     | `service/DashboardStatService.java`     | 8 个统计维度: `getOverview` / `getDeviceOnlineRate` / `getDeviceActiveRate` / `getSensorOnlineRate` / `getSensorActiveRate` / `getHazardPointTrend` / `getSensorDistribution` / `getHealthScore` + `getFull` 一体化聚合 |
| `MqttSessionEnrichService` | `service/MqttSessionEnrichService.java` | `enrichBatch` 通过 `IDeviceQueryService.getDeviceBriefsByAuthUsernames` 一次查询补全所有 clientId 信息 (避免 N+1)                                                                                                           |
| `MqttHttpApiClient`        | `client/MqttHttpApiClient.java`         | 包装 mica-mqtt HTTP API (stats/clients/subscriptions/kick)                                                                                                                                                      |
| `MqttHttpApiProperties`    | `config/MqttHttpApiProperties.java`     | 绑定 `mqtt.server.http-listener.*` 配置                                                                                                                                                                           |
| `RestTemplateConfig`       | `config/RestTemplateConfig.java`        | RestTemplate Bean + 鉴权头注入                                                                                                                                                                                     |

## 关键基础设施

- `MqttHttpApiClient` — 包装 mica-mqtt HTTP API 调用 (stats/clients/subscriptions/kick)
- `MqttSessionEnrichService` — 用 `IDeviceQueryService` 给原始 clientId 添加设备名/隐患点名
- `MqttHttpApiProperties` — 绑定 `mqtt.server.http-listener.*` 配置
- `DashboardStatService` — 通过 `IDeviceStatService` 聚合仪表盘指标

## 设备在线状态基础设施

- `device_online_status` 表 — 实时在线/离线/最后上报时间, 用于快速查询
- `device_online_event_log` 表 — 每次 connect/disconnect 追加历史 (含 reason)
- `DeviceOnlineStatusService` (在 `zwei-iot-device`) — `@EventListener` 监听 `DeviceOnlineEvent`/`DeviceOfflineEvent`,
  UPSERT 状态 + INSERT 历史
- `device_sensor.last_report_time` — 传感器级上报时间, 由 `MonitorIngestConsumerService.processRecord()` 在 IoTDB
  写入成功后更新

## 健康分计算 (DashboardStatService.getHealthScore)

| 因子      | 权重   | 类型          | 状态                 |
|---------|------|-------------|--------------------|
| 资料完善率   | 0.20 | computed    | 已实现                |
| 设备在线率   | 0.15 | computed    | 已实现                |
| 设备正常率   | 0.15 | computed    | 已实现                |
| 告警及时响应率 | 0.20 | placeholder | 占位 100.0 (需告警模块集成) |
| 边坡稳定率   | 0.30 | placeholder | 占位 100.0 (需边坡模块集成) |

总分 = 加权求和 (round to 0.1)

## 测试与质量

- 单元测试建议覆盖: `DashboardStatService` 聚合逻辑 (使用 mock `IDeviceStatService`)
- 集成测试: mica-mqtt HTTP API 可用性

## 常见问题 (FAQ)

**Q: 设备富化时如何避免循环依赖?**
A: 本模块只依赖 `zwei-iot-device` 的 Service 接口 (`IDeviceQueryService`); `zwei-iot-device` 不反向依赖 `zwei-monitor`。

**Q: 旧 `/sys/v1/monitor/*` 还能用吗?**
A: 可以, 但已被 `/api/v1/monitor/overview` 取代。新功能应使用 `/api/v1/monitor/*`。

**Q: 仪表盘的健康分怎么算?**
A: 见上方"健康分计算"表 — 因子包括 CPU/内存/磁盘/在线率/告警量, 加权求和。当前告警/边坡两项为占位值，待集成后自动激活。

**Q: 按监测类型分组的在线/活跃率如何计算?**
A: `DashboardStatService.buildTypeStats` — 当前没有按类型单独统计在线/活跃数，按总体比率等比例分摊到各类型 (避免显示 100%
假数据)。

## 相关文件清单

- `pom.xml`
- `src/main/java/com/zwei/monitor/controller/MqttStatsController.java`
- `src/main/java/com/zwei/monitor/controller/MqttClientController.java`
- `src/main/java/com/zwei/monitor/controller/MonitorOverviewController.java`
- `src/main/java/com/zwei/monitor/controller/DashboardStatController.java`
- `src/main/java/com/zwei/monitor/client/MqttHttpApiClient.java`
- `src/main/java/com/zwei/monitor/service/MqttSessionEnrichService.java` (P2)
- `src/main/java/com/zwei/monitor/service/DashboardStatService.java` (P2)
- `src/main/java/com/zwei/monitor/config/MqttHttpApiProperties.java`
- `src/main/java/com/zwei/monitor/config/RestTemplateConfig.java`

## 变更记录 (Changelog)

| 时间               | 变更                                                                                                                                   |
|------------------|--------------------------------------------------------------------------------------------------------------------------------------|
| 2026-06-10 18:52 | 首次生成模块级 CLAUDE.md (架构师自动扫描)                                                                                                          |
| 2026-06-10 19:08 | 增量补扫: 修正 `mqtt/session/MqttSessionEnrichService.java` → 实际在 `service/MqttSessionEnrichService.java`; 新增核心实现类索引、健康分计算表、按监测类型等比例分摊算法说明 |
| 2026-06-10 21:20 | 移除对 `zwei-iot` 兼容空壳的依赖 (本模块已不再依赖此模块)                                                                                                 |
