# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

> **编码规范 Skill**: 本项目提供 `.claude/skills/coding-standards.md`，包含注释格式、模块边界、错误处理、数据库设计约定等详细编码规范。Agent
> 在处理本项目代码时自动加载。

## Project Overview

**知微 (Zwei)** — 地质灾害监测管理系统 (Geo-Hazard Monitoring & Management System). An IoT platform that collects
real-time sensor data from field devices via MQTT, stores time-series measurements in Apache IoTDB, applies alarm models
for hazard early warning, and provides visualization dashboards with Leaflet maps.

## Tech Stack

| Layer          | Technology                                                       |
|----------------|------------------------------------------------------------------|
| Backend        | Java 17, Spring Boot 4.0.3, MyBatis, Maven                       |
| Frontend       | Vue 3 + TypeScript + Vite, Element Plus 2.6, ECharts 6, Leaflet  |
| Relational DB  | MySQL 8.0 (business data: users, devices, hazard points, alarms) |
| Time-series DB | Apache IoTDB 2.0.2 (sensor measurement data)                     |
| Cache          | Redis 7 (sessions, auth tokens, runtime cache)                   |
| Messaging      | MQTT (embedded mica-mqtt broker) for device communication        |
| Deployment     | Docker Compose, multi-stage Docker builds, Nginx reverse proxy   |

## Development Commands

### Frontend (`web/`)

```bash
cd web
npm run dev       # Start Vite dev server on :5173, proxies /api to :8080
npm run build     # Type-check then production build (outputs to dist/)
npm run preview   # Preview production build locally
```

### Backend (`server/`)

```bash
cd server
mvn clean compile                          # Compile only
mvn clean package -DskipTests              # Build without tests
mvn test                                   # Run all tests
mvn test -pl zwei-iot -Dtest=ClassName     # Run a single test class
```

### Docker (full stack)

```bash
cp .env.example .env    # Then fill in real passwords and secrets
docker compose up -d --build
```

## Architecture

### Backend Module Map (Maven multi-module)

```
server/
├── zwei-admin/            Entry point — Spring Boot app, REST controllers
├── zwei-common/           Shared: domain models, annotations (@Log, @RateLimiter, @DataScope),
│                          base classes (BaseController, BaseEntity), AJAX response envelope
├── zwei-framework/        Cross-cutting: JWT auth filter, RBAC security (Spring Security),
│                          MyBatis/Redis/Druid config, AOP aspects, global exception handler
├── zwei-system/           RBAC implementation: users, roles, menus, departments, dicts
│                          └── notice/  通知公告子包（包级隔离，含SSE推送+多通道架构预留）
├── zwei-iot-monitor/      IoT — 监测字典: 监测大类(category) + 监测类型(type) + 监测内容(content)
├── zwei-iot-device/       IoT — 设备全生命周期 + 传感器 + 注册中心 + 跨模块 Service 接口定义
├── zwei-iot-timeseries/   IoT — IoTDB 读写 + MQTT 数据解析 + 监测数据查询
├── zwei-iot-broker/       IoT — MQTT 设备鉴权 + 会话管理 + 发布订阅 ACL
├── zwei-iot-hazard/       IoT — 隐患点管理 + 分组 + 设备/视频设备绑定
├── zwei-iot-video/        IoT — 视频设备管理 + 隐患点关联
├── zwei-iot/              (空壳，保留兼容旧依赖)
├── zwei-monitor/          System monitoring — unified monitoring API & MQTT broker status
├── zwei-quartz/           Scheduled tasks (quartz job framework)
└── zwei-log/              Audit/operation logging, SSE streaming, MQTT message logs
```

### IoT Modules — Core Business Logic (拆分后)

Previously a single `zwei-iot` module. Now split into 6 independent Maven modules:

| Module                | Package                     | Responsibility                                                                              |
|-----------------------|-----------------------------|---------------------------------------------------------------------------------------------|
| `zwei-iot-monitor`    | `com.zwei.iot.monitor`      | Monitor category/type/content CRUD. Pure dictionary, no IoT dependencies.                   |
| `zwei-iot-device`     | `com.zwei.iot.device`       | Device & sensor lifecycle, registration, MQTT auth accounts, cross-module service interfaces |
| `zwei-iot-timeseries` | `com.zwei.iot.timeseries`   | IoTDB read/write, MQTT payload parsing (sys/gb), monitor data query API (latest/page/chart) |
| `zwei-iot-broker`     | `com.zwei.iot.broker`       | MQTT CONNECT auth, session registry, publish/subscribe ACL, connect/disconnect listeners    |
| `zwei-iot-hazard`     | `com.zwei.iot.hazardpoint`  | Hazard point & group CRUD, device/video binding, implements IDeviceHazardRelationService    |
| `zwei-iot-video`      | `com.zwei.iot.video`        | Video device CRUD, hazard point association, implements IVideoDeviceStatService             |

**Cross-module dependency rules:**
- `zwei-iot-device` defines all cross-module service interfaces (IDeviceAuthQueryService, IDeviceSensorQueryService, etc.)
- All other IoT modules depend on `zwei-iot-device` only through its service interfaces, never through Mapper directly
- `zwei-iot-monitor` is the leaf module — no IoT dependencies

### Monitor Module (`zwei-monitor/`) — System & MQTT Monitoring

Unified monitoring layer that wraps the mica-mqtt HTTP API (port 18083) and aggregates server health data:

| Controller                                 | Path Prefix                     | Responsibility                                                                                                |
|--------------------------------------------|---------------------------------|---------------------------------------------------------------------------------------------------------------|
| `MqttStatsController`                      | `/api/v1/monitor/mqtt`          | MQTT server stats, listener config, runtime parameters                                                        |
| `MqttClientController`                     | `/api/v1/monitor/mqtt/clients`  | Connected client list (enriched with device/hazard-point names), client detail (with subscriptions), kick/ban |
| `MonitorOverviewController`                | `/api/v1/monitor`               | Aggregated overview: server health + Redis + online users + MQTT + uptime                                     |
| `DashboardStatController`                  | `/api/v1/monitor/dashboard`     | Dashboard metrics: health score, online/active rates, trend, distribution + /full aggregation                 |
| `MqttMessageLogController` *(in zwei-log)* | `/api/v1/monitor/mqtt/messages` | Real-time device message log query (receive time, clientId, topic, payload, size)                             |

Key infrastructure:

- `MqttHttpApiClient` — wraps mica-mqtt HTTP API calls (stats, clients, subscriptions, kick)
- `MqttSessionEnrichService` — enriches raw MQTT client data with Device/HazardPoint names via `IDeviceQueryService` (
  not via Mapper)
- `MqttHttpApiProperties` — binds `mqtt.server.http-listener.*` config for internal HTTP calls
- `DashboardStatService` — aggregates dashboard metrics via `IDeviceStatService` (Service interface, not Mapper)

**Cross-module dependency rules:**

- `zwei-monitor` depends on `zwei-iot-device` **only through Service interfaces** (`IDeviceStatService`,
  `IDeviceQueryService`), never through Mapper or Domain classes directly.
- IoT modules publish events to `zwei-common` event classes (`MqttMessageReceivedEvent`, `DeviceOnlineEvent`,
  `DeviceOfflineEvent`) — no direct Maven dependency on `zwei-log` for event consumption.
- `zwei-log` listens to common events via `@EventListener`, fully decoupled from source modules.

**Device online status infrastructure:**

- `device_online_status` table — separate from `device` business table, stores real-time online/offline/last_report_at
  as a fast lookup
- `device_online_event_log` table — append-only history of every connect/disconnect with reason
- `DeviceOnlineStatusService` — `@EventListener` on `DeviceOnlineEvent`/`DeviceOfflineEvent`, UPSERTs status + INSERTs
  event log
- `device_sensor.last_report_time` — sensor-level reporting timestamp, updated in
  `MonitorIngestConsumerService.processRecord()` after IoTDB write succeeds

> **Deprecation note:** The legacy monitoring endpoints under `/sys/v1/monitor/*` (ServerController, CacheController,
> SysUserOnlineController) remain operational but are superseded by `/api/v1/monitor/overview`. New development should use
> the `/api/v1/monitor/*` paths.

### Data Flow

```
Field sensors → MQTT (mica-mqtt) → MonitorIngestFacade
    → MonitorTopicParser → MonitorMetadataService → payload parser (sys/gb)
    → Redis Stream (stream:monitor:ingest)
    → MonitorIngestConsumerService (async)
        → IotdbTimeSeriesService → IoTDB (time-series storage)
        → DeviceOnlineStatusService → device_online_status.last_report_at  (运维指标)
        → DeviceSensorService → device_sensor.last_report_time            (传感器活跃率)
        → DeviceMapper → device.lastReportTime (兼容保留)

MQTT Connect/Disconnect:
    → MqttDeviceAuthService.authenticate() → publishEvent(DeviceOnlineEvent)
    → MqttConnectStatusListener → publishEvent(DeviceOnlineEvent / DeviceOfflineEvent)
    → DeviceOnlineStatusService (EventListener) → device_online_status 表 (UPSERT)
    → device_online_event_log 表 (INSERT 历史明细)
```

### Frontend Structure

```
web/src/
├── api/          # Per-domain API modules (device, hazardPoint, monitorData, sensor, etc.)
├── views/        # Page components organized by feature area:
│   ├── dashboard/       # Home dashboard
│   ├── holo-board/      # Comprehensive views (map overlay, alarm, operation, custom)
│   ├── basic/           # Hazard points, monitor types, device/video management
│   ├── alarm/           # Real-time alarms, criteria, notification, disposal
│   ├── report/          # Reports, query center, data analysis, large screen
│   ├── iot/             # Alarm engine, data parsing
│   ├── miniprogram/     # Mini-program facing views
│   ├── system/          # Organization, identity, permission, logs, settings, notice
│   └── user/            # User profile
├── layout/       # Main layout shell with sidebar navigation
├── router/       # Vue Router config — all routes except /login require token auth
└── utils/
    ├── request.ts  # Axios wrapper — base URL /api/v1, auto-attaches Bearer token, handles 401
    └── auth.ts     # Auth failure handler
```

### API Convention

- All REST endpoints are under `/api/v1/`
- Standard response envelope: `{ code, msg, data }` pattern from BaseController/AjaxResult
- JWT Bearer token auth (via `Authorization` header), validated by `JwtAuthenticationTokenFilter`
- Anonymous access controlled by `@Anonymous` annotation on controller methods
- Vite dev server proxies `/api` → `http://127.0.0.1:8080`

### Key Config Files

| File                 | Purpose                                                                      |
|----------------------|------------------------------------------------------------------------------|
| `docker-compose.yml` | Full stack orchestration with health checks and log rotation                 |
| `web/nginx.conf`     | Production Nginx — SPA fallback, API proxy to backend, SSE/WebSocket support |
| `server/pom.xml`     | Parent POM with all dependency versions and module declarations              |
| `.env.example`       | Required environment variables template                                      |

## Database Notes

- MySQL schema initializes from `db/geo_hazard_monitor_v1.10.sql` on first container start
- Upgrade scripts live in `db/upgrade/`
- IoTDB stores time-series data — tables (sequences) created dynamically on first write per device

## Shared Events (`zwei-common`)

Event classes in `com.zwei.common.event` serve as contracts between modules without direct Maven dependencies:

| Event                      | Publisher                                                   | Consumer                                  |
|----------------------------|-------------------------------------------------------------|-------------------------------------------|
| `MqttMessageReceivedEvent` | zwei-iot (MqttServerMessageListener)                        | zwei-log (MqttMessageLogService)          |
| `DeviceOnlineEvent`        | zwei-iot (MqttDeviceAuthService, MqttConnectStatusListener) | zwei-iot (DeviceOnlineStatusService)      |
| `DeviceOfflineEvent`       | zwei-iot (MqttConnectStatusListener)                        | zwei-iot (DeviceOnlineStatusService)      |
| `NoticeCreatedEvent`       | zwei-system (SysNoticeServiceImpl)                          | zwei-system (NoticeStreamPublisher → SSE) |

## Notification Module (`zwei-system/notice/`)

Package-isolated at `com.zwei.system.notice.*` (16 files renamed from `zwei-system`, zero business code changes):

| Subpackage      | Contents                                                                                |
|-----------------|-----------------------------------------------------------------------------------------|
| `domain/`       | SysNotice, SysNoticeRead                                                                |
| `mapper/`       | SysNoticeMapper, SysNoticeReadMapper                                                    |
| `service/`      | ISysNoticeService, ISysNoticeReadService, NoticeStreamPublisher (SSE)                   |
| `service/impl/` | SysNoticeServiceImpl, SysNoticeReadServiceImpl                                          |
| `notify/`       | INotifyChannel, NotifyChannelDispatcher, NotifySendRequest (multi-channel architecture) |

Controllers in `zwei-admin/web/controller/system/notice/`:

- `SysNoticeController` — CRUD + read/unread status at `/api/v1/system/notice/*`
- `NoticeStreamController` — SSE endpoint at `/api/v1/system/notice/stream`

Frontend: `web/src/api/notice.ts` + `web/src/layout/index.vue` (notification bell with real data + SSE real-time push).

## Security Model

- RBAC with 4 permission levels: menu, button, data scope, API
- `@DataScope` annotation filters SQL queries by user's department hierarchy (up to 5 levels)
- `@RateLimiter` / `@RepeatSubmit` for abuse prevention
- `@Sensitive` annotation for automatic data masking in JSON serialization
- Admin user (user_id=1) gets `*:*:*` wildcard permission; all others derive permissions from `sys_role_menu` + `sys_menu.perms`

### Permission Convention

All `@PreAuthorize` annotations follow the pattern `module:entity:action` (e.g. `system:user:list`).

**Permissions added by zwei-monitor / log hardening:**

| String | Purpose |
|--------|---------|
| `monitor:overview:list` | System monitoring overview dashboard |
| `monitor:mqtt:list` | MQTT broker stats, clients, listeners, message logs |
| `monitor:mqtt:kick` | Kick/ban MQTT clients |
| `monitor:operlog:list` | Query operation/auth/runtime logs and SSE stream |
| `common:file:upload` | File upload (single + batch) |
| `common:file:query` | File download |

**Permission management endpoints (SysMenuController):**

| Method | Path | Purpose |
|--------|------|---------|
| `GET` | `/api/v1/menus/permission-coverage` | Compare `@PreAuthorize` annotations vs `sys_menu.perms` — returns `{ codePerms, dbPerms, missingInDb }` |
| `POST` | `/api/v1/menus/batch-register` | Bulk register missing permissions as menu entries under parent_id=2 (系统监控) |

Use these to identify and fix permission gaps after adding new `@PreAuthorize` annotations.
