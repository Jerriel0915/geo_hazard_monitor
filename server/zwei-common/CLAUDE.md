[根目录](../../CLAUDE.md) > [server](../) > **zwei-common**

# zwei-common — 公共基础模块

> 面包屑: [根目录](../../CLAUDE.md) > [server](../) > **zwei-common**

## 模块职责

存放全项目共享的基础类、领域模型、工具方法、注解、常量、异常与**事件契约**。是所有其他 Maven 模块的依赖基础（叶子节点，不依赖任何业务模块）。

## 关键依赖

- Spring Context Support / Spring Web
- Spring Security / Validation
- pagehelper-spring-boot-starter (分页)
- fastjson2 / jackson-databind
- commons-io / commons-lang3 / commons-pool2
- jjwt (token 工具)
- poi-ooxml (Excel 导入导出)
- yauaa (User-Agent 解析)
- spring-boot-starter-data-redis / cache (Redis 与缓存)
- jakarta.servlet-api
- lombok (provided)

## 主要子包

| 子包                  | 路径                                                                                                                                        | 职责                            |
|---------------------|-------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------|
| `core.controller`   | `BaseController`                                                                                                                          | Web 层通用数据处理 (日期转换、分页、AJAX 响应) |
| `core.domain`       | `AjaxResult`/`BaseEntity`/`LoginUser`                                                                                                     | 统一响应/实体基类/登录用户                |
| `core.page`         | `TableDataInfo`/`PageDomain`/`TableSupport`                                                                                               | 分页基础设施                        |
| `core.domain.model` | `LoginUser`                                                                                                                               | 登录态模型                         |
| `annotation`        | `@Log`/`@RateLimiter`/`@RepeatSubmit`/`@DataScope`/`@Anonymous`/`@Sensitive`/`@PreAuthorize`                                              | 横切注解                          |
| `event`             | `MqttMessageReceivedEvent`/`DeviceOnlineEvent`/`DeviceOfflineEvent`/`NoticeCreatedEvent`/`AlarmTriggeredEvent`/`MonitorDataIngestedEvent` | 模块间事件契约 (解耦)                  |
| `constant`          | `HttpStatus`/`Constants`                                                                                                                  | 常量                            |
| `exception`         | `ServiceException`/`BaseException`                                                                                                        | 业务异常                          |
| `utils`             | `DateUtils`/`StringUtils`/`SecurityUtils`/`PageUtils`/`SqlUtil`                                                                           | 工具方法                          |
| `enums`             | 业务枚举                                                                                                                                      | —                             |

## 共享事件契约 (zwei-common/event)

事件类是模块解耦的核心：发布者不直接依赖订阅者的 Maven 模块。

| 事件                         | 发布者                                                   | 订阅者                                           |
|----------------------------|-------------------------------------------------------|-----------------------------------------------|
| `MqttMessageReceivedEvent` | `MqttServerMessageListener` (zwei-iot-broker)         | `MqttMessageLogService` (zwei-log)            |
| `DeviceOnlineEvent`        | `MqttDeviceAuthService` / `MqttConnectStatusListener` | `DeviceOnlineStatusService` (zwei-iot-device) |
| `DeviceOfflineEvent`       | `MqttConnectStatusListener`                           | `DeviceOnlineStatusService` (zwei-iot-device) |
| `NoticeCreatedEvent`       | `SysNoticeServiceImpl` (zwei-system)                  | `NoticeStreamPublisher` → SSE                 |
| `AlarmTriggeredEvent`      | `zwei-iot-alarm`                                      | 前端 SSE 实时推送                                   |
| `MonitorDataIngestedEvent` | `zwei-iot-timeseries`                                 | 下游分析模块                                        |

## 共享基础类

- `BaseController` (`com.zwei.common.core.controller`) — 所有 Controller 基类
- `AjaxResult` (`com.zwei.common.core.domain`) — `{code, msg, data}` 信封
- `BaseEntity` — 实体基类 (含 createTime/updateTime/createBy/updateBy)
- `TableDataInfo` — 表格分页响应
- `LoginUser` — Spring Security 上下文中的当前登录用户
- `SecurityUtils` — 取当前用户/角色/权限的工具

## 测试与质量

- 模块无 `src/test/java` (纯基础模块, 不含业务逻辑)
- 工具类应通过 zwei-admin / zwei-system 的集成测试间接覆盖
- 建议: 引入单元测试覆盖 `StringUtils` / `DateUtils` / `SqlUtil` 等关键工具

## 常见问题 (FAQ)

**Q: 新增跨模块事件应该放在哪里?**
A: 在 `com.zwei.common.event` 下新建 Event 类, 字段保持简单 (基本类型/枚举/小对象), 不要引用业务实体。

**Q: 业务异常如何抛?**
A: 抛 `ServiceException` 或继承自 `BaseException`, 由 `zwei-framework` 的全局异常处理器统一转换为 `AjaxResult.error()`。

## 相关文件清单

- `pom.xml` — 无业务依赖
- `src/main/java/com/zwei/common/event/*.java` — 6 个事件类
- `src/main/java/com/zwei/common/core/controller/BaseController.java`
- `src/main/java/com/zwei/common/annotation/*.java`

## 变更记录 (Changelog)

| 时间               | 变更                          |
|------------------|-----------------------------|
| 2026-06-10 18:52 | 首次生成模块级 CLAUDE.md (架构师自动扫描) |
