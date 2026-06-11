[根目录](../../CLAUDE.md) > [server](../) > **zwei-admin**

# zwei-admin — 启动入口 + REST 控制器总成

> 面包屑: [根目录](../../CLAUDE.md) > [server](../) > **zwei-admin**

## 模块职责

Spring Boot 应用启动入口, 聚合所有业务 Controller:

- **启动类** `com.zwei.RuoYiApplication` (基于 RuoYi 改造)
- **配置文件** `application.yml` / `application-druid.yml` / `application-redis.yml` / 各业务 `application-iotdb.yml` 等
- **REST 控制器** — 通过 `@ComponentScan("com.zwei")` 自动扫描所有子模块的 Controller

## 关键依赖

`zwei-admin` 是聚合模块, 依赖几乎所有其他模块:

- `zwei-framework` (启动 Security/AOP)
- `zwei-system` (RBAC)
- `zwei-quartz` (定时任务)
- `zwei-log` (日志)
- `zwei-monitor` (系统监控)
- `zwei-iot-monitor` / `zwei-iot-device` / `zwei-iot-timeseries` / `zwei-iot-broker` / `zwei-iot-hazard` /
  `zwei-iot-video` / `zwei-iot-alarm`
- `zwei-common` (基础)
- spring-boot-starter-web / validation / aop
- druid / mysql-connector / mica-mqtt / iotdb-jdbc

## 入口

- 主类: `com.zwei.RuoYiApplication`
- 端口: 8080 (默认, 可通过 `server.port` 改)
- 启动 banner: 经典 RuoYi ASCII 艺术

## 主要 Controller (按业务域)

| 包                                   | 控制器示例                                                                                                                                                                                                                                                                                                            | 路径前缀                                   |
|-------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------|
| `web.controller.common`             | `CaptchaController` / `CommonController` (文件上传)                                                                                                                                                                                                                                                                  | `/api/v1/common/*`                     |
| `web.controller.system`             | `SysLoginController` / `SysUserController` / `SysRoleController` / `SysMenuController` / `SysDeptController` / `SysPostController` / `SysDictTypeController` / `SysDictDataController` / `SysConfigController` / `SysRegisterController` / `OrganizationController` / `ProfileController` / `SysIndexController` | `/api/v1/system/*` / `/api/v1/menus/*` |
| `web.controller.system.notice`      | `SysNoticeController` / `NoticeStreamController`                                                                                                                                                                                                                                                                 | `/api/v1/system/notice/*`              |
| `web.controller.monitor` *(legacy)* | `ServerController` / `CacheController` / `SysUserOnlineController`                                                                                                                                                                                                                                               | `/sys/v1/monitor/*` (废弃)               |
| `web.controller.tool`               | `TestController`                                                                                                                                                                                                                                                                                                 | `/api/v1/tool/*`                       |

> 业务模块的 Controller (如 `zwei-iot-alarm.AlarmRecordController`) 不在本模块, 但被 `@ComponentScan` 自动注册。

## 关键配置 (application.yml 摘要)

```yaml
spring:
  profiles:
    active: druid, redis, iotdb, mqtt
  application:
    name: zwei
  datasource: { type: com.alibaba.druid... }
  redis: { host: 127.0.0.1, port: 6379 }
mqtt:
  server:
    port: 1883
    http-listener: { enable: true, port: 18083 }
iotdb:
  jdbc: { url: jdbc:iotdb://127.0.0.1:6667/, user: root, password: root }
server:
  port: 8080
```

## API 约定

- 所有路径以 `/api/v1/` 开头
- 响应信封: `{ code: 200, msg: "操作成功", data: ... }`
- 鉴权: JWT Bearer Token (Header `Authorization: Bearer xxx`)
- 匿名访问: 方法级 `@Anonymous` 注解
- 跨域: `CorsConfig` 允许 `*` (生产应严格)

## 测试与质量

- 集成测试: `SpringBootTest` + `MockMvc`
- 业务测试: 借助各业务模块的 Service 测试
- 启动验证: `mvn spring-boot:run` 验证 8080 端口监听

## 常见问题 (FAQ)

**Q: 启动报错 "No qualifying bean of type XXX"?**
A: 通常是缺 `@ComponentScan` 扫描包, 或某 Service 接口没有实现类。检查: 1) 业务模块是否在 `zwei-admin` 的依赖列表; 2)
实现类是否标注 `@Service`; 3) 是否在 `com.zwei` 扫描包下。

**Q: 启动报 DataSource 错误?**
A: 检查 `application.yml` 中 `spring.datasource.*` 配置, MySQL 是否可达。

**Q: 启动时 IoTDB 连接失败?**
A: 检查 `iotdb.jdbc.*` 配置, IoTDB 是否启动, 端口是否正确 (默认 6667)。

## 相关文件清单

- `pom.xml`
- `src/main/java/com/zwei/RuoYiApplication.java`
- `src/main/resources/application.yml`
- `src/main/java/com/zwei/web/controller/**/*.java` (本模块自带)
- `src/main/java/com/zwei/web/controller/system/notice/*.java`

## 变更记录 (Changelog)

| 时间               | 变更                          |
|------------------|-----------------------------|
| 2026-06-10 18:52 | 首次生成模块级 CLAUDE.md (架构师自动扫描) |
