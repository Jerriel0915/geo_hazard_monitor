[根目录](../../CLAUDE.md) > [server](../) > **zwei-log**

# zwei-log — 审计/操作日志 + SSE 实时流 + MQTT 消息日志

> 面包屑: [根目录](../../CLAUDE.md) > [server](../) > **zwei-log**

## 模块职责

提供三类日志/流式能力:

1. **操作日志** (`sys_oper_log`): 自动记录 Controller 方法 (由 `@Log` 切面写入)
2. **登录日志** (`sys_logininfor`): 登录成功/失败
3. **SSE 实时流**: 操作日志/运行时异常的实时推送
4. **MQTT 消息日志**: 通过 `MqttMessageReceivedEvent` 订阅, 记录所有 MQTT 消息

## 关键依赖

- `zwei-common` (基础 + 事件契约)
- `zwei-system` (SysUser, LoginUser 等)
- `spring-boot-starter-web` (SSE)
- MyBatis (操作/登录日志写入)
- lombok

## 主要子包

| 子包                              | 路径                                                                          | 职责                            |
|---------------------------------|-----------------------------------------------------------------------------|-------------------------------|
| `api.controller`                | `LogQueryController` / `LogStreamController` / `LogCleanupConfigController` | 日志查询/清理配置/SSE 推送              |
| `api.service` / `service.impl`  | `IOperLogService` / `ILogininforService`                                    | 日志查询                          |
| `api.mapper` / `mapper`         | MyBatis Mapper                                                              | DB 访问                         |
| `api.domain` / `domain`         | `SysOperLog` / `SysLogininfor`                                              | 实体                            |
| `mqtt.controller`               | `MqttMessageLogController`                                                  | MQTT 消息日志查询                   |
| `mqtt.service` / `service.impl` | `IMqttMessageLogService`                                                    | MQTT 消息持久化                    |
| `mqtt.event`                    | `MqttMessageLogService.@EventListener`                                      | 订阅 `MqttMessageReceivedEvent` |
| `sse`                           | `LogSsePublisher`                                                           | 推送给前端                         |

## 对外接口 (Controller)

| 控制器                               | 路径                               | 职责              |
|-----------------------------------|----------------------------------|-----------------|
| `LogQueryController`              | `/api/v1/monitor/operlog`        | 操作日志分页/详情/清空/删除 |
| `LogQueryController` (logininfor) | `/api/v1/monitor/logininfor`     | 登录日志            |
| `LogStreamController`             | `/api/v1/monitor/operlog/stream` | SSE 实时推送        |
| `LogCleanupConfigController`      | `/api/v1/monitor/log/cleanup`    | 日志清理策略配置        |
| `MqttMessageLogController`        | `/api/v1/monitor/mqtt/messages`  | MQTT 消息日志查询     |

## 事件消费

| 事件                         | 消费方式                             | 写入表                |
|----------------------------|----------------------------------|--------------------|
| `MqttMessageReceivedEvent` | `@EventListener`                 | `mqtt_message_log` |
| `NoticeCreatedEvent`       | `@EventListener` (在 zwei-system) | `sys_notice`       |

> **解耦设计**: 本模块**不**直接依赖 `zwei-iot-broker` (MQTT 来源), 通过 `zwei-common` 事件契约异步消费。

## 数据模型

- `sys_oper_log` — 操作日志 (title/businessType/method/requestParam/jsonResult/status/errorMsg/costTime)
- `sys_logininfor` — 登录日志 (userName/ip/status/msg)
- `mqtt_message_log` — MQTT 消息日志 (clientId/topic/payload/payloadSize/receiveTime)

## 测试与质量

- 单元测试建议覆盖: `LogSsePublisher` 多连接管理、`@EventListener` 触发
- 集成测试: 通过 `LogQueryController` 分页查询

## 常见问题 (FAQ)

**Q: 操作日志不写怎么办?**
A: 检查 1) Controller 方法是否标注 `@Log(title, businessType)`; 2) `LogAspect` 是否生效 (包在 `com.zwei` 扫描范围内); 3)
`sys_oper_log` 表是否存在。

**Q: MQTT 消息日志不写怎么办?**
A: 检查 1) `zwei-iot-broker` 是否发布 `MqttMessageReceivedEvent`; 2) `MqttMessageLogService` 的 `@EventListener` 是否生效;

3) 是否启用了 `@EnableAsync`。

**Q: SSE 连接断开怎么重连?**
A: 前端用 `EventSource` 自带重连机制; 服务端应记录活跃连接, 在 `complete/timeout/error` 时移除。

## 相关文件清单

- `pom.xml`
- `src/main/java/com/zwei/log/api/controller/LogQueryController.java`
- `src/main/java/com/zwei/log/api/controller/LogStreamController.java`
- `src/main/java/com/zwei/log/mqtt/controller/MqttMessageLogController.java`
- `src/main/java/com/zwei/log/mqtt/service/IMqttMessageLogService.java`

## 变更记录 (Changelog)

| 时间               | 变更                          |
|------------------|-----------------------------|
| 2026-06-10 18:52 | 首次生成模块级 CLAUDE.md (架构师自动扫描) |
