# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

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
├── zwei-admin/       Entry point — Spring Boot app, REST controllers
├── zwei-common/      Shared: domain models, annotations (@Log, @RateLimiter, @DataScope),
│                     base classes (BaseController, BaseEntity), AJAX response envelope
├── zwei-framework/   Cross-cutting: JWT auth filter, RBAC security (Spring Security),
│                     MyBatis/Redis/Druid config, AOP aspects (logging, rate limiting, data scope),
│                     global exception handler, server monitoring
├── zwei-system/      RBAC implementation: users, roles, menus, departments, dicts, notices
├── zwei-iot/         **Core business module** — see below
├── zwei-monitor/     System monitoring — unified monitoring API & MQTT broker status
├── zwei-quartz/      Scheduled tasks (quartz job framework)
└── zwei-log/         Audit/operation logging, SSE streaming, MQTT message logs
```

### IoT Module (`zwei-iot/`) — Core Business Logic

This is where the domain-specific logic lives, organized into sub-packages:

| Package        | Responsibility                                                                                                      |
|----------------|---------------------------------------------------------------------------------------------------------------------|
| `broker/`      | MQTT broker integration — device auth, session registry, publish/subscribe ACL, connect status listener             |
| `device/`      | Device lifecycle — CRUD, sensor attributes, device registration/activation with auto-generated MQTT credentials     |
| `hazardpoint/` | Hazard point management — CRUD, device binding, map positioning (lat/lng), groups                                   |
| `monitor/`     | Monitor type & content definitions — standardized sensor measurement specs                                          |
| `timeseries/`  | IoTDB integration — data ingestion (MQTT → IoTDB), query service, payload parsers (GB/SYS protocols), topic routing |
| `video/`       | Video device management — CRUD, hazard point association                                                            |

### Monitor Module (`zwei-monitor/`) — System & MQTT Monitoring

Unified monitoring layer that wraps the mica-mqtt HTTP API (port 18083) and aggregates server health data:

| Controller                                 | Path Prefix                     | Responsibility                                                                                                |
|--------------------------------------------|---------------------------------|---------------------------------------------------------------------------------------------------------------|
| `MqttStatsController`                      | `/api/v1/monitor/mqtt`          | MQTT server stats, listener config, runtime parameters                                                        |
| `MqttClientController`                     | `/api/v1/monitor/mqtt/clients`  | Connected client list (enriched with device/hazard-point names), client detail (with subscriptions), kick/ban |
| `MonitorOverviewController`                | `/api/v1/monitor`               | Aggregated overview: server health + Redis + online users + MQTT + uptime                                     |
| `MqttMessageLogController` *(in zwei-log)* | `/api/v1/monitor/mqtt/messages` | Real-time device message log query (receive time, clientId, topic, payload, size)                             |

Key infrastructure:

- `MqttHttpApiClient` — wraps mica-mqtt HTTP API calls (stats, clients, subscriptions, kick)
- `MqttSessionEnrichService` — enriches raw client data with Device/HazardPoint names from the IoT module
- `MqttHttpApiProperties` — binds `mqtt.server.http-listener.*` config for internal HTTP calls

> **Deprecation note:** The legacy monitoring endpoints under `/sys/v1/monitor/*` (ServerController, CacheController,
> SysUserOnlineController) remain operational but are superseded by `/api/v1/monitor/overview`. New development should use
> the `/api/v1/monitor/*` paths.

### Data Flow

```
Field sensors → MQTT (mica-mqtt) → MonitorIngestStreamService
    → payload parser (GbMonitorPayloadParser / SysMonitorPayloadParser)
    → IotdbJdbcClient → Apache IoTDB (time-series storage)
    → Alarm engine evaluates thresholds → generates alarm records → notifications (SMS/H5)
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
│   ├── system/          # Organization, identity, permission, logs, settings
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

- MySQL schema initializes from `db/geo_hazard_monitor_v1.5.sql` on first container start
- Upgrade scripts live in `db/upgrade/`
- IoTDB stores time-series data — tables (sequences) created dynamically on first write per device

## Security Model

- RBAC with 4 permission levels: menu, button, data scope, API
- `@DataScope` annotation filters SQL queries by user's department hierarchy (up to 5 levels)
- `@RateLimiter` / `@RepeatSubmit` for abuse prevention
- `@Sensitive` annotation for automatic data masking in JSON serialization
- Admin user (user_id=1) gets `*:*:*` wildcard permission; all others derive permissions from `sys_role_menu` + `sys_menu.perms`

### Permission Convention

All `@PreAuthorize` annotations follow the pattern `module:entity:action` (e.g. `system:user:list`).
New permissions are registered via `db/upgrade/upgrade_v1.6_permissions.sql` as `sys_menu` rows.

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
