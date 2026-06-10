[根目录](../../CLAUDE.md) > [server](../) > **zwei-iot-broker**

# zwei-iot-broker — MQTT Broker 鉴权/会话/ACL

> 面包屑: [根目录](../../CLAUDE.md) > [server](../) > **zwei-iot-broker**

## 模块职责

基于 mica-mqtt 嵌入式 Broker 实现:

- **CONNECT 鉴权** (`MqttDeviceAuthService` + `MqttServerAuthHandler`) — 校验 clientId/username/password + 协议白名单 +
  失败封禁 + 审计日志 + 单设备单连接挤占
- **会话注册** (`MqttDeviceSessionRegistry`) — 双 ConcurrentHashMap 索引 (deviceId / clientId)
- **发布订阅 ACL** (`MqttServerPublishPermission` / `MqttServerSubscribeValidator`) — topic 访问控制 (设备只允许发布到自身
  topic)
- **连接/断开监听** (`MqttConnectStatusListener` / `MqttSessionListener`) — 发布 `DeviceOnlineEvent`/
  `DeviceOfflineEvent`
- **消息监听** (`MqttServerMessageListener`) — 接收上行消息 → 调 `MonitorIngestFacade.ingest()`
- **异常体系** — `MqttBusinessException` / `MqttConnectionException` / `MqttCommunicationException` /
  `MqttProtocolException`

## 关键依赖

- `zwei-common` (事件契约)
- `zwei-iot-device` (鉴权 Service + 设备信息)
- `zwei-iot-timeseries` (上行消息触发接入，**注意: broker 模块依赖 timeseries 模块的 `MonitorIngestFacade`**
  ，存在反向依赖)
- `mica-mqtt-server-spring-boot-starter` (2.6.3)
- lombok

## 主要子包

| 子包          | 职责                                                                                                                                                                                                     |
|-------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `handler`   | `MqttServerAuthHandler` — Broker 协议层 CONNECT 鉴权入口 (薄封装委托)                                                                                                                                              |
| `service`   | `MqttDeviceAuthService` (核心鉴权) / `DeviceSessionServiceImpl` (跨模块接口 `IDeviceSessionService` 实现) / `MqttConnectStatusListener` / `MqttSessionListener` / `MqttServerMessageListener`                     |
| `component` | `MqttDeviceSessionRegistry` (内存索引) / `MqttAuthFailureGuard` (失败封禁) / `MqttServerPublishPermission` (发布 ACL) / `MqttServerSubscribeValidator` (订阅 ACL)                                                  |
| `config`    | `MqttAuthCenterProperties`                                                                                                                                                                             |
| `exception` | `MqttBusinessException` / `MqttConnectionException` / `MqttCommunicationException` / `MqttProtocolException` / `MqttErrorCode` / `MqttErrorContext` / `MqttExceptionReporter` / `MqttServiceException` |
| `model`     | `MqttDeviceSession` (record: deviceId, deviceCode, authUsername, clientId, clientIp, connectedAt)                                                                                                      |

## 关键流程

### CONNECT 鉴权 (`MqttDeviceAuthService.authenticate`)

1. 设备发起 MQTT CONNECT, 携带 clientId/username/password
2. 归一化 clientId/username/password
3. **clientId 空** → 拒绝 (`MalformedPacket`)
4. **username 格式非法** (`^[A-Z0-9]{6}$`) → 拒绝 (`AuthenticationFailed`)
5. **password 格式非法** (`^[A-Za-z0-9]{8}$`) → 拒绝并 `failureGuard.recordFailure` (避免无意义访问)
6. **被临时封禁** → 返回 `AUTH_TEMP_BLOCKED_Ns`
7. **设备不存在** → 拒绝
8. **协议不允许** (`device.protocolType != MQTT`，可通过 `mqtt.auth.enforce-mqtt-protocol` 配置关闭) → 拒绝
9. **账号已禁用** (`authStatus=2`) → 拒绝
10. **密码错误** → 拒绝
11. **通过**：回写 `lastAuthTime`/`lastAuthIp` + `MqttDeviceSessionRegistry.register()` + 单设备单连接挤占 (
    `disconnectPreviousClient`) + 鉴权审计 + 发布 `DeviceOnlineEvent`

### 消息上行

1. 设备 publish 到上行 topic (`sys/v1/...` 或 `gb/v1/...`)
2. `MqttServerMessageListener` 接收
3. 发布 `MqttMessageReceivedEvent` (被 `zwei-log.MqttMessageLogService` 消费)
4. 同时 `MqttDeviceAuthService.hasPublishPermission()` 校验 (deviceCode 一致性)
5. 通过后入 `MonitorIngestFacade.parse()`, 解析后入 Redis Stream

### 连接/断开

1. `MqttConnectStatusListener.onConnect()` → 发布 `DeviceOnlineEvent`
2. `MqttConnectStatusListener.onDisconnect()` → 发布 `DeviceOfflineEvent`
3. `zwei-iot-device.DeviceOnlineStatusService` 监听并 UPSERT `device_online_status` / INSERT `device_online_event_log`

## 核心实现类索引 (P2)

| 类                              | 文件                                            | 关键方法 / 责任                                                                               |
|--------------------------------|-----------------------------------------------|-----------------------------------------------------------------------------------------|
| `MqttServerAuthHandler`        | `handler/MqttServerAuthHandler.java`          | Broker 协议层入口，try-catch 包 `MqttDeviceAuthService.authenticate()`，异常转 `BrokerUnavailable` |
| `MqttDeviceAuthService`        | `service/MqttDeviceAuthService.java`          | 10 步鉴权流程 + 单连接挤占 + 审计 + 事件发布                                                            |
| `MqttDeviceSessionRegistry`    | `component/MqttDeviceSessionRegistry.java`    | 双 ConcurrentHashMap 索引 (deviceId / clientId)，`synchronized` 写，无锁读                       |
| `MqttAuthFailureGuard`         | `component/MqttAuthFailureGuard.java`         | 失败计数 + 临时封禁 (Redis)                                                                     |
| `MqttServerPublishPermission`  | `component/MqttServerPublishPermission.java`  | 发布 ACL (设备→自身 deviceCode)                                                               |
| `MqttServerSubscribeValidator` | `component/MqttServerSubscribeValidator.java` | 订阅 ACL                                                                                  |
| `MqttConnectStatusListener`    | `service/MqttConnectStatusListener.java`      | connect/disconnect 事件 → `DeviceOnlineEvent`/`DeviceOfflineEvent`                        |
| `MqttServerMessageListener`    | `service/MqttServerMessageListener.java`      | 上行消息 → `MqttMessageReceivedEvent` + `MonitorIngestFacade.ingest()`                      |
| `MqttSessionListener`          | `service/MqttSessionListener.java`            | 会话事件 (mica-mqtt 回调)                                                                     |
| `DeviceSessionServiceImpl`     | `service/DeviceSessionServiceImpl.java`       | 实现 `IDeviceSessionService`：密码重置时 `disconnectDevice(deviceId)` 主动断连                      |

## mica-mqtt 配置

```yaml
mqtt:
  server:
    port: 1883                  # MQTT 端口
    ssl:                        # 可选
    http-listener:              # mica-mqtt 内置 HTTP API
      enable: true
      port: 18083
    node: zwei
```

## 对外接口

无 Controller (Broker 是服务端组件)。HTTP API 通过 mica-mqtt 内置 HTTP (端口 18083) 暴露, 由 `zwei-monitor` 的
`MqttHttpApiClient` 调用。

## 异常体系 (broker 专属)

```
MqttServiceException (基类, RuntimeException)
  ├── MqttBusinessException (业务/参数错误)
  │     ├── InvalidTopic
  │     └── PermissionDenied
  ├── MqttConnectionException (CONNECT/DISCONNECT)
  │     ├── AuthenticationFailed
  │     └── BrokerUnavailable
  ├── MqttCommunicationException (网络/IO)
  └── MqttProtocolException (协议层)
        └── MalformedPacket
```

每个异常带 `MqttErrorContext` (clientId, topic, qos, attributes) + `MqttExceptionReporter` 统一日志。

## 测试与质量

- 单元测试: 鉴权逻辑 (mock `IDeviceAuthQueryService`)
- 集成测试: 启动 mica-mqtt 本地端口, 用 mqtt-client 模拟连接

## 常见问题 (FAQ)

**Q: 设备连接被拒绝, 怎么排查?**
A: 1) 检查 clientId/username/password 是不是 `device.auth_username` + `device.auth_password` (明文)；2) 查
`device_online_event_log` 的 reason 字段；3) 查 `MqttExceptionReporter` 日志（含 clientId + reason 编码）。

**Q: ACL 规则在哪里配置?**
A: `MqttServerPublishPermission` (发布) / `MqttServerSubscribeValidator` (订阅) — 设备只能发布到
`sys|gb/v1/{自己的deviceCode}/...`，不允许的 topic 直接拒绝。

**Q: SSL/TLS 怎么配?**
A: 在 `application.yml` 的 `mqtt.server.ssl.*` 配置 keystore, mica-mqtt 自动启用。

**Q: 鉴权失败封禁的策略?**
A: `MqttAuthFailureGuard` (基于 Redis) — 连续失败 N 次后封禁 M 分钟，由 `MqttAuthCenterProperties` 配置阈值。

## 相关文件清单

- `pom.xml`
- `src/main/java/com/zwei/iot/broker/handler/MqttServerAuthHandler.java` (P2)
- `src/main/java/com/zwei/iot/broker/service/MqttDeviceAuthService.java` (P2)
- `src/main/java/com/zwei/iot/broker/service/MqttConnectStatusListener.java` (P2)
- `src/main/java/com/zwei/iot/broker/service/MqttServerMessageListener.java` (P2)
- `src/main/java/com/zwei/iot/broker/service/DeviceSessionServiceImpl.java` (P2)
- `src/main/java/com/zwei/iot/broker/component/MqttDeviceSessionRegistry.java` (P2)
- `src/main/java/com/zwei/iot/broker/component/MqttAuthFailureGuard.java` (P2)
- `src/main/java/com/zwei/iot/broker/component/MqttServerPublishPermission.java` (P2)
- `src/main/java/com/zwei/iot/broker/component/MqttServerSubscribeValidator.java` (P2)
- `src/main/java/com/zwei/iot/broker/exception/MqttErrorCode.java`

## 变更记录 (Changelog)

| 时间               | 变更                                                                                                     |
|------------------|--------------------------------------------------------------------------------------------------------|
| 2026-06-10 18:52 | 首次生成模块级 CLAUDE.md (架构师自动扫描)                                                                            |
| 2026-06-10 19:08 | 增量补扫: 修正路径 `auth/` → `service/MqttDeviceAuthService.java`；新增 10 步鉴权流程、异常体系类图、核心实现类索引、Redis 封禁策略、密码格式正则 |
