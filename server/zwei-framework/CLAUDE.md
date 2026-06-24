[根目录](../../CLAUDE.md) > [server](../) > **zwei-framework**

# zwei-framework — 框架核心 (认证/安全/权限/AOP/配置)

> 面包屑: [根目录](../../CLAUDE.md) > [server](../) > **zwei-framework**

## 模块职责

提供 Spring Boot 应用的横切基础设施:

- **Spring Security 配置 + JWT 鉴权过滤器** (`JwtAuthenticationTokenFilter`)
- **RBAC 权限模型** (`@PreAuthorize` + `sys_menu.perms`)
- **MyBatis / Druid / Redis 配置**
- **AOP 切面** (操作日志/限流/防重/数据权限/脱敏)
- **全局异常处理** (`@RestControllerAdvice`)
- **系统监控 (OSHI) 与验证码 (kaptcha)**

## 关键依赖

- spring-boot-starter-webmvc
- spring-boot-starter-aspectj (AOP)
- druid-spring-boot-4-starter
- kaptcha (验证码)
- oshi-core (系统信息采集)
- zwei-system (引入 RBAC Service 接口)
- zwei-log (引入日志写入能力)

## 主要子包

| 子包                 | 职责                                                                                 |
|--------------------|------------------------------------------------------------------------------------|
| `config`           | MyBatis/Redis/Security/Swagger/线程池等 Bean 配置                                        |
| `security.filter`  | JwtAuthenticationTokenFilter, AnonymousFilter 等                                    |
| `security.handler` | AuthenticationEntryPoint / AccessDeniedHandler                                     |
| `aspectj`          | DataScopeAspect, RateLimiterAspect, LogAspect, RepeatSubmitAspect, SensitiveAspect |
| `manager`          | AsyncManager, ShutdownManager, ThreadPoolConfig                                    |
| `interceptor`      | RepeatSubmitInterceptor 等                                                          |
| `web.exception`    | GlobalExceptionHandler (统一异常转 AjaxResult)                                          |
| `captcha`          | 验证码生成 (CaptchaImage 控制器配合)                                                         |

## 对外接口

- Spring Security Filter Chain (通过 `@Bean SecurityFilterChain` 暴露)
- AOP 切面 (通过 `@Log`/`@DataScope`/`@RateLimiter` 等注解触发)
- `RedisUtils` / `CacheUtils` (Redis 操作工具)
- `SecurityUtils.getLoginUser()` (业务侧统一获取当前用户)

## 关键 Service 接口 (来自 zwei-system / zwei-log)

本模块不强依赖 Service 实现, 但通过依赖 `zwei-system`/`zwei-log` 引入:

- `ISysMenuService` — 权限菜单查询
- `ISysUserService` — 用户认证
- `ISysRoleService` — 角色权限
- `IOperLogService` — 操作日志

## 切面说明

| 注解                                 | 切面                        | 行为                           |
|------------------------------------|---------------------------|------------------------------|
| `@Log(title, businessType)`        | `LogAspect`               | 自动记录操作日志到 `sys_oper_log`     |
| `@DataScope(deptAlias, userAlias)` | `DataScopeAspect`         | 自动追加 `dept_id IN (...)` 数据权限 |
| `@RateLimiter(key, time, count)`   | `RateLimiterAspect`       | 基于 Redis 的限流                 |
| `@RepeatSubmit(interval)`          | `RepeatSubmitInterceptor` | 防重复提交                        |
| `@Sensitive(strategy)`             | `SensitiveAspect`         | JSON 序列化时脱敏                  |

## 测试与质量

- 集成测试由 `zwei-admin` 启动类覆盖
- 单元测试建议覆盖: 切面逻辑 (AOP)、`GlobalExceptionHandler` 异常映射
- 切面顺序在 `application.yml` 中通过 `spring.aop.order` 控制

## 常见问题 (FAQ)

**Q: 业务 Controller 怎样获得当前登录用户?**
A: 继承 `BaseController`, 调用 `getLoginUser()` 或在任意位置调用 `SecurityUtils.getLoginUser()`。

**Q: 新增 `@PreAuthorize("xxx:yyy:list")` 后, 前端没看到菜单怎么办?**
A: 调 `GET /api/v1/menus/permission-coverage` 比对 `codePerms` 与 `dbPerms`, 调 `POST /api/v1/menus/batch-register`
自动入库。

**Q: AOP 切面不生效?**
A: 检查: 1) 目标类是否在 `zwei-admin` 的 `@SpringBootApplication` 扫描包内 (`com.zwei`); 2) 是否被同类方法内部调用绕过代理;

3) 注解是否被 `@Within` 限定范围。

## 相关文件清单

- `pom.xml`
- `src/main/java/com/zwei/framework/config/SecurityConfig.java`
- `src/main/java/com/zwei/framework/security/filter/JwtAuthenticationTokenFilter.java`
- `src/main/java/com/zwei/framework/aspectj/*Aspect.java`
- `src/main/java/com/zwei/framework/web/exception/GlobalExceptionHandler.java`

## 变更记录 (Changelog)

| 时间               | 变更                          |
|------------------|-----------------------------|
| 2026-06-10 18:52 | 首次生成模块级 CLAUDE.md (架构师自动扫描) |
| 2026-06-25 10:00 | SSE 订阅泄漏修复: ThreadPoolConfig 新增 ThreadPoolTaskScheduler Bean (poolSize=4) 防止 @Scheduled 心跳饥饿 |
