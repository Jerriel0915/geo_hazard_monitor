[根目录](../../CLAUDE.md) > [server](../) > **zwei-datashare**

# zwei-datashare — 数据共享模块 (共享策略管理)

> 面包屑: [根目录](../../CLAUDE.md) > [server](../) > **zwei-datashare**

## 模块职责

数据共享策略管理系统, 负责"向谁共享什么数据、以什么方式共享":

- **共享策略 CRUD** (`ShareStrategyController` + `IShareStrategyService`) — 创建/更新/删除/查询共享策略, 控制数据外发的目标地址、协议、频次、范围
- **四种共享模式** (`ShareMethod`) — 统一化数据推送 (UNIFIED_PUSH) / 定制化数据推送 (CUSTOM_PUSH) / 统一化数据服务 (UNIFIED_SERVICE) / 定制化数据服务 (CUSTOM_SERVICE)
- **策略执行** (`ShareStrategyServiceImpl.execute()`) — 统一的执行入口, 区分统一化/定制化策略执行路径, 记录执行日志 (成功/失败/超时)
- **定制化脚本管理** (`ShareStrategyScript`) — 为 CUSTOM_PUSH 和 CUSTOM_SERVICE 模式提供自定义脚本存储 (Groovy 或 DSL), 支持变量定义
- **运行日志追踪** (`ShareStrategyLog`) — 每次执行记录运行时间、状态 (SUCCESS/ERROR/TIMEOUT)、消息、数据条数、耗时
- **范围控制** (`ScopeType`) — 策略可作用于 4 种范围: 隐患点分组 (HAZARD_POINT_GROUP) / 隐患点 (HAZARD_POINT) / 厂商 (VENDOR) / 设备 (DEVICE)

## 关键依赖

- `zwei-common` (BaseController / AjaxResult / 事件 + 基础)
- lombok + junit-jupiter + spring-boot-starter-test

## 主要子包

| 子包            | 职责                                                                                             |
|---------------|------------------------------------------------------------------------------------------------|
| `controller`  | `ShareStrategyController` — 共享策略 CRUD + 启停 + 执行 + 脚本管理 + 运行日志 REST 端点                          |
| `service`     | `IShareStrategyService` (接口) / `ShareStrategyServiceImpl` (实现)                                  |
| `domain`      | `ShareStrategy` (核心实体) / `ShareStrategyLog` (运行日志) / `ShareStrategyScript` (定制化脚本)               |
| `domain.dto`  | `ShareStrategyCreateRequest` / `ShareStrategyUpdateRequest` / `ShareStrategyVO` / `StatusChangeRequest` |
| `enums`       | `ShareMethod` (共享方式) / `ScopeType` (作用范围) / `StrategyStatus` (策略状态) / `RunStatus` (运行状态)         |
| `mapper`      | `ShareStrategyMapper` / `ShareStrategyLogMapper` / `ShareStrategyScriptMapper` (3 个 MyBatis Mapper) |

## 对外接口 (Controller)

| 路径                                           | 方法       | 权限                          | 职责                  |
|----------------------------------------------|----------|-----------------------------|---------------------|
| `/api/v1/datashare/strategy/page`            | GET      | `basic:device:list`         | 分页查询共享策略列表          |
| `/api/v1/datashare/strategy`                 | GET      | `datashare:strategy:list`   | 条件查询策略列表 (name/status/method) |
| `/api/v1/datashare/strategy`                 | POST     | `datashare:strategy:add`    | 创建共享策略 (新建默认 DISABLED) |
| `/api/v1/datashare/strategy/{id}`            | GET      | `datashare:strategy:query`  | 查询策略详情              |
| `/api/v1/datashare/strategy/{id}`            | PUT      | `datashare:strategy:edit`   | 更新共享策略              |
| `/api/v1/datashare/strategy/{id}`            | DELETE   | `datashare:strategy:remove` | 删除共享策略 (级联删除日志+脚本)   |
| `/api/v1/datashare/strategy/{id}/status`     | PATCH    | `datashare:strategy:edit`   | 切换策略启用/停用状态         |
| `/api/v1/datashare/strategy/{id}/execute`    | POST     | `datashare:strategy:execute` | 手动执行策略             |
| `/api/v1/datashare/strategy/{id}/logs`       | GET      | `datashare:strategy:query`  | 获取策略运行日志列表          |
| `/api/v1/datashare/strategy/{id}/script`     | GET      | `datashare:strategy:query`  | 获取定制化脚本            |
| `/api/v1/datashare/strategy/{id}/script`     | POST     | `datashare:strategy:edit`   | 保存定制化脚本 (script + variables) |

## 核心实现类索引

### Controller + Service 层

| 类                            | 文件                                                | 职责                                                     |
|------------------------------|---------------------------------------------------|--------------------------------------------------------|
| `ShareStrategyController`    | `controller/ShareStrategyController.java`         | 11 个 REST 端点: CRUD + 启停 + 执行 + 脚本 + 日志                  |
| `IShareStrategyService`      | `service/IShareStrategyService.java`              | 服务接口: create/update/delete/findById/findList/changeStatus/execute/findLogs/getScript/saveScript/selectPage |
| `ShareStrategyServiceImpl`   | `service/impl/ShareStrategyServiceImpl.java`      | 实现: `@Transactional` CRUD + 统一化/定制化执行分支 + 脚本 UPSERT       |

### 枚举体系

| 枚举               | 文件                            | 取值                                                                         |
|------------------|-------------------------------|----------------------------------------------------------------------------|
| `ShareMethod`    | `enums/ShareMethod.java`      | UNIFIED_PUSH (统一化数据推送) / CUSTOM_PUSH (定制化数据推送) / UNIFIED_SERVICE (统一化数据服务) / CUSTOM_SERVICE (定制化数据服务) |
| `ScopeType`      | `enums/ScopeType.java`        | HAZARD_POINT_GROUP (隐患点分组) / HAZARD_POINT (隐患点) / VENDOR (厂商) / DEVICE (设备)     |
| `StrategyStatus` | `enums/StrategyStatus.java`   | ENABLED (已启用) / DISABLED (已停用)                                              |
| `RunStatus`      | `enums/RunStatus.java`        | SUCCESS (成功) / ERROR (失败) / TIMEOUT (超时)                                    |

### 数据模型

| 类                     | 文件                                     | 关键字段                                                                                                                  |
|-----------------------|----------------------------------------|-----------------------------------------------------------------------------------------------------------------------|
| `ShareStrategy`       | `domain/ShareStrategy.java`            | id / code / name / description / method / address / topic / username / password / params / scopeType / scopeIds / cron / status / successCount / lastRunTime / lastRunStatus |
| `ShareStrategyLog`    | `domain/ShareStrategyLog.java`         | id / strategyId / runTime / status (RunStatus) / message / dataCount / duration / createTime                             |
| `ShareStrategyScript` | `domain/ShareStrategyScript.java`      | id / strategyId / script / variables / createTime / updateTime                                                           |

## 关键流程

### 策略执行 (ShareStrategyServiceImpl.execute)

1. 查询策略详情 (`shareStrategyMapper.selectById`)
2. 记录开始时间, 创建 `ShareStrategyLog` (初始 state)
3. 根据 `ShareMethod` 分流:
   - `UNIFIED_PUSH` / `UNIFIED_SERVICE` → `executeUnifiedStrategy()` (TODO: 统一化推送/服务逻辑)
   - `CUSTOM_PUSH` / `CUSTOM_SERVICE` → `executeCustomStrategy()` → 加载 `ShareStrategyScript` (TODO: 脚本执行逻辑)
4. 成功 → `log.setStatus(RunStatus.SUCCESS)` + `incrementSuccessCount()`
5. 失败 → `log.setStatus(RunStatus.ERROR)` + 记录异常消息
6. 写入日志 + 更新 `lastRunTime` / `lastRunStatus`

### 策略删除 (级联)

删除策略时 `@Transactional` 级联删除:
- `shareStrategyMapper.deleteById(id)` — 删除策略主记录
- `shareStrategyLogMapper.deleteByStrategyId(id)` — 删除关联运行日志
- `shareStrategyScriptMapper.deleteByStrategyId(id)` — 删除关联定制化脚本

## 测试与质量

- 运行: `mvn test -pl zwei-datashare`

## 相关文件清单

- `pom.xml`
- `src/main/java/com/zwei/datashare/controller/ShareStrategyController.java`
- `src/main/java/com/zwei/datashare/service/impl/ShareStrategyServiceImpl.java`
- `src/main/java/com/zwei/datashare/service/IShareStrategyService.java`
- `src/main/java/com/zwei/datashare/domain/ShareStrategy.java`
- `src/main/java/com/zwei/datashare/domain/ShareStrategyLog.java`
- `src/main/java/com/zwei/datashare/domain/ShareStrategyScript.java`
- `src/main/java/com/zwei/datashare/mapper/ShareStrategyMapper.java`
- `src/main/java/com/zwei/datashare/mapper/ShareStrategyLogMapper.java`
- `src/main/java/com/zwei/datashare/mapper/ShareStrategyScriptMapper.java`
- `src/main/java/com/zwei/datashare/enums/ShareMethod.java`
- `src/main/java/com/zwei/datashare/enums/ScopeType.java`
- `src/main/java/com/zwei/datashare/enums/StrategyStatus.java`
- `src/main/java/com/zwei/datashare/enums/RunStatus.java`
