# CLAUDE.md

> 编码规范: `.claude/skills/coding-standards.md` | 模块级文档: `server/*/CLAUDE.md` + `web/CLAUDE.md` | 扫描元数据:
`.claude/index.json`

## Project Overview

**知微 (Zwei)** — 地质灾害监测管理系统. IoT platform: field sensors → MQTT → IoTDB + MySQL → alarm evaluation →
visualization dashboards.

## Tech Stack

| Layer          | Technology                                                      |
|----------------|-----------------------------------------------------------------|
| Backend        | Java 17, Spring Boot 4.0.3, MyBatis, Maven                      |
| Frontend       | Vue 3 + TypeScript + Vite, Element Plus 2.6, ECharts 6, Leaflet |
| Relational DB  | MySQL 8.0 (business data)                                       |
| Time-series DB | Apache IoTDB 2.0.2 (sensor measurements)                        |
| Cache          | Redis 7 (sessions, auth, runtime cache)                         |
| Messaging      | MQTT (mica-mqtt broker) for device communication                |
| Deployment     | Docker Compose, Nginx reverse proxy                             |

## Development Commands

```bash
# Frontend (web/)
cd web && npm run dev       # Vite dev server :5173, proxies /api → :8080
cd web && npm run build     # Type-check + production build

# Backend (server/)
cd server && mvn clean compile                    # Compile only
cd server && mvn clean package -DskipTests        # Build without tests
cd server && mvn test -pl <module> -Dtest=Class   # Single test class
```

## Module Index

| 模块                    | 路径                           | 职责                         |
|-----------------------|------------------------------|----------------------------|
| `zwei-admin`          | `server/zwei-admin`          | 启动入口 + REST 控制器            |
| `zwei-common`         | `server/zwei-common`         | BaseController/事件契约/工具类    |
| `zwei-framework`      | `server/zwei-framework`      | 认证/安全/权限/AOP/MyBatis/Redis |
| `zwei-system`         | `server/zwei-system`         | RBAC + 通知公告 (含 SSE)        |
| `zwei-quartz`         | `server/zwei-quartz`         | Quartz 定时任务                |
| `zwei-log`            | `server/zwei-log`            | 审计/操作日志 + SSE + MQTT 消息日志  |
| `zwei-monitor`        | `server/zwei-monitor`        | 系统监控: 服务器/Redis/MQTT/仪表盘   |
| `zwei-iot-monitor`    | `server/zwei-iot-monitor`    | 监测字典 (叶子模块)                |
| `zwei-iot-device`     | `server/zwei-iot-device`     | 设备/传感器/注册 + 跨模块接口定义        |
| `zwei-iot-timeseries` | `server/zwei-iot-timeseries` | IoTDB 读写 + 监测数据查询          |
| `zwei-iot-broker`     | `server/zwei-iot-broker`     | MQTT 鉴权/会话/ACL             |
| `zwei-iot-hazard`     | `server/zwei-iot-hazard`     | 隐患点/分组/设备绑定                |
| `zwei-iot-video`      | `server/zwei-iot-video`      | 视频设备管理                     |
| `zwei-iot-alarm`      | `server/zwei-iot-alarm`      | 告警引擎 + 算法库 (Groovy/Python) |
| `zwei-iot-parser`     | `server/zwei-iot-parser`     | 数据解析引擎 (Groovy 脚本 + 编译缓存)  |
| `zwei-iot-report`     | `server/zwei-iot-report`     | 报告模板 + 报告记录生成              |
| `zwei-datashare`      | `server/zwei-datashare`      | 数据共享策略 (4 种模式)             |
| `zwei-terra`          | `server/zwei-terra`          | Terra AI 助手 (core + agent) |
| 前端                    | `web`                        | Vue 3 + TS + Vite          |
| Terra 独立前端            | `terra`                      | Vue 3 独立应用 — AI 值守大屏       |
| 数据库                   | `db`                         | MySQL 全量脚本 + 升级 (59 张表)    |

## Backend Module Map

```
server/
├── zwei-admin/            Entry point — Spring Boot app, REST controllers
├── zwei-common/           BaseController, BaseEntity, AjaxResult, event contracts, annotations
├── zwei-framework/        JWT auth, Spring Security RBAC, MyBatis/Redis/Druid config, AOP
├── zwei-system/           RBAC (users/roles/menus/departments/dicts) + notice/ (SSE + multi-channel)
├── zwei-iot-monitor/      Monitor category/type/content CRUD — leaf, no IoT deps
├── zwei-iot-device/       Device & sensor lifecycle + cross-module service interfaces
├── zwei-iot-timeseries/   IoTDB read/write + MQTT payload parsing + monitor data query API
├── zwei-iot-broker/       MQTT CONNECT auth + session registry + pub/sub ACL
├── zwei-iot-hazard/       Hazard point & group CRUD + device/video binding
├── zwei-iot-video/        Video device CRUD + hazard point association
├── zwei-iot-alarm/        Alarm engine: criteria/strategy/evaluation/notification + algolib
├── zwei-iot-parser/       Groovy script parse engine + compile cache + thread pool
├── zwei-iot-report/       Report template CRUD + record generation
├── zwei-datashare/        Data sharing strategy (UNIFIED/CUSTOM × PUSH/SERVICE)
├── zwei-terra/            AI assistant: core (interfaces) + agent (chat/duty/tools)
├── zwei-monitor/          System monitoring: MQTT stats, dashboard, health score
├── zwei-quartz/           Scheduled job framework
└── zwei-log/              Audit/operation log + SSE + MQTT message/exception log
```

**Cross-module rule:** `zwei-iot-device` defines all cross-module service interfaces. Other IoT modules depend on it via
interfaces only, never via Mapper directly.

## Data Flow

```
Field sensors → MQTT (mica-mqtt) → MqttServerMessageListener
  → MqttDeviceAuthService (鉴权) → MonitorIngestFacade.ingest()
  → Redis Stream (stream:monitor:ingest)
  → MonitorIngestConsumerService (4-stage: 幂等去重 → IoTDB write → 指标回写 → 失败重试/死信)

Alarm evaluation (on MonitorDataIngestedEvent):
  ├─ AlarmEvaluationEngine → 阈值判据 → AlarmRecord → AlarmNotifier + SSE push
  └─ ComprehensiveAlarmEventListener → 综合策略 (REALTIME: 并行评估; CRON: Quartz 动态调度)
       → Groovy (ScriptAlgoOps) / Python (PythonAlgoExecutor) → StrategyExecutionLog

Parser: MQTT payload → DataParseStrategy (Groovy, 编译缓存) → IoTDB
Terra AI: TerraChatController (SSE + ReAct loop) → AnthropicChatModel → ToolManager
Data Share: ShareStrategy (Groovy) → external push / HTTP service
```

## Frontend Structure

```
web/src/
├── api/          # 17 domain API modules
├── views/        # dashboard, holo-board, basic, alarm, report, iot, terra, bigscreen, miniprogram, system, user
├── components/   # terra/ (AI chat + widget), map/ (editor/preview/picker), MonitorDataExplorer, EChartsWrapper
├── composables/  # useLeafletMap, useMapEditor, useMonitorData, usePagination, useTableSort
├── lib/          # boundaryCoords, coordParser, mapGeometry
├── layout/       # Main layout shell + sidebar
├── router/       # Vue Router — all routes except /login require token auth
└── utils/        # request.ts (Axios + Bearer token + 401 handling), auth.ts
```

## API Convention

- All REST endpoints under `/api/v1/`, response envelope `{ code, msg, data }`
- JWT Bearer token auth (`Authorization` header), validated by `JwtAuthenticationTokenFilter`
- `@Anonymous` for public endpoints; Vite dev proxies `/api` → `http://127.0.0.1:8080`

## Key Config Files

| File                             | Purpose                                                   |
|----------------------------------|-----------------------------------------------------------|
| `docker-compose.yml`             | Full stack with health checks + log rotation              |
| `web/nginx.conf`                 | Production Nginx — SPA fallback, API proxy, SSE/WebSocket |
| `server/pom.xml`                 | Parent POM with dependency versions + module declarations |
| `.env.example`                   | Environment variables template                            |
| `db/geo_hazard_monitor_v2.0.sql` | MySQL full dump (59 tables)                               |
| `db/upgrade/*.sql`               | 23+ incremental upgrade scripts                           |

## Database Notes

- MySQL 8.0, `utf8mb4_0900_ai_ci`, all InnoDB, 59 tables in 9 domains
- 物理外键仅 `device_hazard_point` / `video_device_hazard_point`；其余应用层维护
- 逻辑删除: `del_flag` (0-正常 1-删除)
- IoTDB paths: `root.{database}.d{deviceId}.s{sensorNo}`, aligned timeseries (DOUBLE+GORILLA / INT32+RLE)

## Shared Events (`zwei-common`)

| Event                                      | Publisher           | Consumer                                        |
|--------------------------------------------|---------------------|-------------------------------------------------|
| `MqttMessageReceivedEvent`                 | zwei-iot-broker     | zwei-log (message log)                          |
| `MqttMessageRejectEvent`                   | zwei-iot-broker     | zwei-log (exception log)                        |
| `DeviceOnlineEvent` / `DeviceOfflineEvent` | zwei-iot-broker     | zwei-iot-device (online status)                 |
| `NoticeCreatedEvent`                       | zwei-system         | zwei-system (SSE push)                          |
| `AlarmTriggeredEvent`                      | zwei-iot-alarm      | zwei-iot-alarm (notifier + SSE + comprehensive) |
| `MonitorDataIngestedEvent`                 | zwei-iot-timeseries | zwei-iot-alarm (alarm evaluation)               |
| `MonitorContentChangedEvent`               | zwei-iot-monitor    | zwei-iot-device (sensor_attribute + TSL sync)   |

## Security Model

- RBAC: menu/button/data-scope/API 4-level permissions; `@DataScope` filters by department hierarchy
- `@RateLimiter` / `@RepeatSubmit` for abuse prevention; `@Sensitive` for data masking
- Admin (user_id=1): `*:*:*` wildcard; others derive from `sys_role_menu` + `sys_menu.perms`
- Permission pattern: `module:entity:action` (e.g. `system:user:list`, `monitor:mqtt:kick`)

> 完整模块级文档见 `server/*/CLAUDE.md` 与 `web/CLAUDE.md`，数据库文档见 `db/CLAUDE.md`。
