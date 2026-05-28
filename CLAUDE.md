# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working in this repository.

## Project Overview

Geo-disaster Monitor (地质灾害监测预警系统) is a Vue 3 + Java Spring Boot system for geological hazard monitoring, device management, alarm handling, reports, and real-time log streaming.

The repository currently has two main applications:

- `web/` - Vue 3 frontend
- `server/` - Java Spring Boot backend (RuoYi v3.9.2 multi-module project)

There are also supporting assets at the repo root:

- `db/` - full schema, API docs, and upgrade SQL scripts
- `docs/` - product and UI documentation
- `docker-compose.yml` - local or deployment orchestration baseline

## Tech Stack

### Frontend

- **Framework**: Vue 3 + TypeScript
- **Build**: Vite
- **UI Library**: Element Plus
- **Charts**: ECharts
- **Maps**: Leaflet + Leaflet Draw
- **HTTP Client**: Axios wrapper in `web/src/utils/userApi.ts`

### Backend

- **Framework**: Spring Boot 4.0.3
- **Architecture**: Maven multi-module
- **ORM / Pagination**: MyBatis + PageHelper
- **Database**: MySQL + Druid
- **Cache**: Redis + Spring Cache annotations
- **Auth**: JWT
- **API Docs**: SpringDoc OpenAPI
- **Scheduling**: Quartz
- **MQTT**: `mica-mqtt`
- **Java Version**: 17

## Repository Structure

```text
geo_hazard_monitor/
├── db/                         # Schema, API docs, upgrade scripts
├── docs/                       # Product docs and screenshots
├── server/                     # Spring Boot backend
├── web/                        # Vue 3 frontend
├── docker-compose.yml
├── .env.example
└── CLAUDE.md
```

## Backend Modules (`server/`)

Current backend modules defined in `server/pom.xml`:

```text
server/
├── pom.xml
├── sql/
│   ├── quartz.sql
│   └── ry_20260417.sql
├── zwei-admin/                 # Boot entry, web controllers, app resources
├── zwei-common/                # Shared annotations, constants, utils, base classes
├── zwei-framework/             # Security, MVC, datasource, exception handling, aspects
├── zwei-log/                   # Unified log query / SSE / storage / cleanup module
├── zwei-system/                # System management domain and services
├── zwei-quartz/                # Scheduled task management
├── zwei-generator/             # Code generator
└── zwei-iot/                   # IoT and hazard monitoring business modules
```

### `zwei-admin`

```text
zwei-admin/src/main/
├── java/com/zwei/
│   ├── RuoYiApplication.java
│   ├── RuoYiServletInitializer.java
│   └── web/
│       ├── controller/
│       │   ├── common/         # Captcha, upload, shared endpoints
│       │   ├── monitor/        # Cache, server info, online users
│       │   ├── system/         # User/role/menu/config/notice/profile/login
│       │   └── tool/           # TestController
│       └── core/config/
│           └── SwaggerConfig.java
└── resources/
    ├── application.yml
    ├── application-prod.yml
    ├── application-druid.yml
    ├── logback-spring.xml
    └── mybatis/mybatis-config.xml
```

Notes:

- Logging now uses `logback-spring.xml`, not `logback.xml`.
- Production logging path is configured in `logback-spring.xml` via Spring profiles.

### `zwei-common`

Shared cross-module utilities and base abstractions:

- `annotation/` - `@Log`, `@RateLimiter`, `@DataScope`, `@Anonymous`, `@Sensitive`
- `config/` - `RuoYiConfig`, `LogSseConfig`, serializer helpers
- `constant/` - shared constants such as `ScheduleConstants`, `CacheConstants`
- `core/` - `BaseController`, `AjaxResult`, pagination, tree/domain helpers, Redis cache wrapper
- `exception/` - common, file, job, and user exceptions
- `utils/` - grouped helper packages for bean, file, http, ip, poi, reflect, spring, sql, uuid

### `zwei-framework`

Core framework layer for security and infrastructure:

```text
zwei-framework/src/main/java/com/zwei/framework/
├── aspectj/                    # DataScope, Log, RateLimiter, DataSource aspects
├── config/                     # Security, Redis, MyBatis, Druid, MVC, thread pool
│   └── properties/             # DruidProperties, PermitAllUrlProperties
├── datasource/                 # Dynamic data source
├── interceptor/                # Repeat submit interceptor and impl/
├── manager/                    # AsyncManager, ShutdownManager
├── security/
│   ├── context/
│   ├── filter/                 # JwtAuthenticationTokenFilter
│   └── handle/                 # Auth entry/logout handlers
└── web/
    ├── domain/
    ├── exception/              # GlobalExceptionHandler
    └── service/                # TokenService, login/register/password/permission services
```

### `zwei-log`

This module is the most important recent structural change.

```text
zwei-log/src/main/java/com/zwei/log/
├── api/
│   ├── controller/             # LogQueryController, LogStreamController
│   └── dto/                    # AuthLogQuery, OperationLogQuery, RuntimeLogQuery
├── application/service/        # LogCenterService, replay/router services
├── domain/
│   ├── enums/                  # LogType, AuthEventType, LogExecutionStatus
│   ├── model/                  # Operation/Auth/Runtime records + checkpoint
│   └── sink/                   # LogSink, FileLogWriter, storage router abstractions
├── infrastructure/
│   ├── appender/               # RuntimeLogAppender
│   ├── collector/http/         # AccessLogFilter
│   ├── config/                 # LogModuleAutoConfiguration, LogModuleProperties
│   ├── persistence/mysql/      # MyBatis mappers + MysqlLogSink
│   ├── push/sse/               # LogStreamPublisher, LogSubscription
│   └── sequence/               # EventIdGenerator
└── task/
    └── LogCleanupTask.java     # Periodic cleanup task bean
```

Current log endpoints:

- `/api/v1/logs/operations/page`
- `/api/v1/logs/auth/page`
- `/api/v1/logs/runtime/page`
- `/api/v1/logs/stream`
- `/api/v1/logs/stream/connections`

Current log-related configuration is under `zwei.log.*` and `log-sse.*`:

- runtime log persistence levels
- SSE timeout / retry
- checkpoint flush interval
- cleanup retention days / batch size

### `zwei-system`

System-domain module focused on mappers and services for:

- users / roles / posts / menus / departments
- dictionaries
- config
- notice + notice read
- online users

Notable structure additions compared with older versions:

- `domain/vo/MetaVo.java`
- `domain/vo/RouterVo.java`

### `zwei-quartz`

Quartz scheduling module:

```text
zwei-quartz/src/main/java/com/zwei/quartz/
├── controller/                 # SysJobController, SysJobLogController
├── domain/                     # SysJob, SysJobLog
├── mapper/                     # SysJobMapper, SysJobLogMapper
├── service/
│   ├── impl/                   # SysJobServiceImpl, SysJobLogServiceImpl
│   └── interfaces
├── task/                       # RyTask
└── util/                       # ScheduleUtils, JobInvokeUtil, CronUtils, Quartz job wrappers
```

Important behavior:

- Jobs are initialized from `sys_job` at startup by `SysJobServiceImpl`.
- Custom task methods can be invoked through Quartz using `invokeTarget`, such as `logCleanupTask.cleanExpiredLogs()`.

### `zwei-iot`

Current IoT business structure:

```text
zwei-iot/src/main/java/com/zwei/iot/
├── broker/
│   ├── component/
│   ├── handler/
│   └── service/
├── device/
│   ├── controller/
│   ├── domain/
│   ├── mapper/
│   └── service/
├── hazardpoint/
│   ├── controller/
│   ├── domain/
│   ├── mapper/
│   └── service/
├── monitor/
│   ├── controller/
│   ├── domain/
│   ├── mapper/
│   └── service/
└── video/
    ├── controller/
    ├── domain/
    ├── mapper/
    └── service/
```

There is no longer a dedicated `domain/dto/` subtree under `hazardpoint` in the current code layout.

## Frontend Structure

Current `web/src/` layout:

```text
web/src/
├── App.vue
├── main.ts
├── style.css
├── layout/
│   └── index.vue
├── router/
│   └── index.ts
├── utils/
│   └── userApi.ts
└── views/
    ├── Login.vue
    ├── alarm/                  # RealtimeAlarm, AlarmCriteria, AlarmNotification, AlarmDisposal
    ├── basic/                  # HazardPoint, MonitorType, Device, VideoDevice
    ├── dashboard/              # Dashboard.vue
    ├── holo-board/             # Comprehensive, Alarm, Operation, Custom
    ├── iot/                    # AlarmEngine, DataParse
    ├── miniprogram/            # Device, Event, HazardPoint, MonitorData
    ├── report/                 # Report, Query, Analysis, Screen
    ├── system/                 # Organization, Identity, Permission, Log, Settings
    └── user/                   # UserProfile
```

Recent frontend structure changes compared with older docs:

- Added `views/holo-board/`
- Added `views/miniprogram/`
- `views/system/Log.vue` is now the real-time log console page
- Route file still uses simple auth guarding based on `localStorage.getItem('token')`

## Logging and SSE

The project now has a dedicated unified log module instead of only relying on legacy system operation logs.

### File Logging

Backend file logging is configured by `zwei-admin/src/main/resources/logback-spring.xml`:

- non-prod writes to `./logs`
- prod writes to `/app/logs`
- output files:
  - `sys-info.log`
  - `sys-error.log`
  - `sys-user.log`

### Runtime Log Persistence

`RuntimeLogAppender` forwards selected runtime logs into the log module and persists them based on `zwei.log.runtime-levels`.

Default configuration:

- dev: `WARN`, `ERROR`
- prod: `WARN`, `ERROR`

### SSE Streaming

The real-time log streaming implementation is centered on:

- `LogStreamController`
- `LogStreamPublisher`
- `LogReplayService`
- `LogStreamCheckpoint`

SSE behavior now supports:

- multiple log types (`operation`, `auth`, `runtime`)
- replay from `Last-Event-ID`
- subscriber checkpoints
- configurable checkpoint flush interval
- cleanup task for old logs and checkpoints

## Cache Mechanism

The project uses Spring Cache annotations with Redis, not a custom warmup-only cache layer.

Typical pattern:

```java
@Cacheable(value = "hazardPoint", key = "#id")
public HazardPoint selectHazardPointById(Long id)

@Caching(evict = {
    @CacheEvict(value = "hazardPoint", key = "#hazardPoint.id"),
    @CacheEvict(value = "hazardPointList", allEntries = true)
})
public int insertHazardPoint(HazardPoint hazardPoint)
```

Related cache configuration lives mainly in:

- `zwei-framework/.../RedisConfig.java`
- `application.yml`

## API Conventions

All backend APIs follow REST-style conventions:

- **Base path**: `/api/v1`
- **Auth**: `Authorization: Bearer {token}`
- **Response wrapper**: `AjaxResult`
- **Pagination**: `pageNum`, `pageSize`

Current major route groups:

| Prefix | Description |
|--------|-------------|
| `/api/v1/system/` | system management |
| `/api/v1/basic/` | hazard points, devices, monitor types, video devices |
| `/api/v1/alarm/` | alarm center |
| `/api/v1/reports/` | reports and analysis |
| `/api/v1/iot/` | IoT management |
| `/api/v1/logs/` | operation/auth/runtime log query and SSE stream |
| `/api/v1/common/` | common upload/captcha utilities |

## Common Commands

### Backend

```bash
cd server

# Build all modules
mvn clean package

# Build a module with dependencies
mvn clean install -pl zwei-log -am

# Run tests for the log module
mvn test -pl zwei-log -am

# Run tests for the IoT module
mvn test -pl zwei-iot -am

# Start the admin app
cd zwei-admin && mvn spring-boot:run
```

### Frontend

```bash
cd web

npm install
npm run dev
npm run build
npm run preview
```

## Configuration

Backend configuration files are in `server/zwei-admin/src/main/resources/`:

- `application.yml` - base and local/dev-oriented configuration
- `application-prod.yml` - production profile overrides
- `application-druid.yml` - Druid datasource settings
- `logback-spring.xml` - file logging configuration with profile-based paths

Important config groups now in use:

- `zwei.log.*` - unified log module settings
- `log-sse.*` - SSE log streaming settings
- `mqtt.server.*` - MQTT broker settings
- `springdoc.*` - OpenAPI / Swagger settings

Frontend config:

- `web/vite.config.ts`
- `web/tsconfig.json`

## Database Schema

Current main schema file:

- `db/geo_hazard_monitor_v1.5.sql`

Important upgrade scripts:

- `db/upgrade/v1.3_log_module_schema.sql`
- `db/upgrade/v1.3_log_module_migration.sql`
- `db/upgrade/v1.3_log_module_indexes.sql`
- `db/upgrade/v1.3_log_module_rollback.sql`

Key log-related tables introduced or used by the new log module:

- `log_operation_record`
- `log_auth_record`
- `log_runtime_record`
- `log_stream_checkpoint`

Existing core business tables still include:

- `hazard_point`
- `hazard_point_group`
- `monitor_type`
- `monitor_content`
- `device`
- `device_sensor`
- `sensor_attribute`
- `video_device`
- `alarm_criteria`
- `alarm_record`
- `report_template`
- `report_record`
- `sys_job`
- `sys_job_log`

## Logical Delete and Unique Keys

For tables that use logical delete (`del_flag`) and also have unique fields or unique indexes, deleting a record must not leave the original unique value permanently occupied.

Required handling:

- On logical delete, rewrite the unique field value before or while setting `del_flag`.
- Prefer the existing project pattern: `CONCAT(LEFT(original_value, 75), '#DEL#', id)` for `varchar(100)` code fields.
- Also update `update_time` during logical delete when the table has that column.
- Guard the delete SQL with the current active flag condition when appropriate (for example `AND del_flag = 0` or `AND del_flag = '0'`) to avoid repeated rewrites.

Current in-repo reference implementation:

- `zwei-iot/.../hazardPoint/HazardPointMapper.xml` rewrites `hazard_point.code` on logical delete.
- `zwei-iot/.../monitor/MonitorTypeMapper.xml` rewrites `monitor_type.code` on logical delete.

When adding or reviewing delete logic for tables such as `device`, `device_sensor`, `monitor_content`, `video_device`, `hazard_point_group`, `sys_dept`, `sys_role`, or `sys_user`, always verify whether any unique field also needs this rewrite strategy.

## Key Backend Patterns

### BaseController

`zwei-common/core/controller/BaseController.java`

- shared controller helpers
- pagination bootstrap
- current user lookup helpers

### AjaxResult

`zwei-common/core/domain/AjaxResult.java`

- standard JSON response wrapper

### Service Pattern

- interface + implementation is still the dominant pattern
- constructor injection is used across new code
- MyBatis mapper + XML remains the standard persistence style

### Security

Security-related code is centered in:

- `zwei-framework/config/SecurityConfig.java`
- `zwei-framework/security/filter/JwtAuthenticationTokenFilter.java`
- `zwei-framework/security/handle/*`
- `zwei-framework/web/service/TokenService.java`

### Scheduling

Quartz jobs are managed through:

- `sys_job`
- `SysJobServiceImpl`
- `ScheduleUtils`
- `JobInvokeUtil`

The new log cleanup task can be scheduled using:

- `logCleanupTask.cleanExpiredLogs()`

## Routing

Frontend routes are defined in `web/src/router/index.ts` and are lazy-loaded.

Current major route groups include:

- `/dashboard`
- `/holo-board/*`
- `/basic/*`
- `/alarm/*`
- `/report/*`
- `/iot/*`
- `/system/*`
- `/user/profile`

Authentication guard remains simple:

- any route except `/login` requires `localStorage.getItem('token')`

## Commit Message Format

Follow `.trae/rules/git-commit-message.md` - conventional commits with type/scope/subject structure.
