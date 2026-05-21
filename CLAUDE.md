# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Geo-disaster Monitor (地质灾害监测预警系统) - A Vue 3 + Java Spring Boot system for monitoring geological hazards. The project has two main components:

- `web/` - Vue 3 frontend (Element Plus, ECharts, Leaflet)
- `server/` - Java Spring Boot backend (RuoYi v3.9.2 framework)

## Tech Stack

### Frontend

- **Framework**: Vue 3 + TypeScript
- **Build**: Vite
- **UI Library**: Element Plus
- **Charts**: ECharts
- **Maps**: Leaflet + Leaflet Draw
- **HTTP Client**: Axios (base URL: `/api/v1`)

### Backend

- **Framework**: Spring Boot 4.0.3 (RuoYi v3.9.2)
- **Build**: Maven multi-module
- **Database**: MySQL with Druid connection pool
- **Cache**: Redis
- **ORM**: MyBatis + PageHelper
- **Auth**: JWT (jjwt 0.9.1)
- **Java Version**: 17

## Module Structure (server/)

```
server/
├── pom.xml                    # Parent POM, defines all modules
├── sql/
│   ├── ry_20260417.sql        # Core RuoYi tables
│   └── quartz.sql             # Quartz scheduler tables
├── zwei-admin/                # Web entry point & controllers
│   └── src/main/java/com/zwei/
│       ├── RuoYiApplication.java
│       └── web/controller/
│           ├── common/        # Upload, captcha
│           ├── monitor/       # Cache, Server, Logs, Online users
│           ├── system/        # SysIndex, SysUser, SysRole, SysMenu, etc.
│           └── tool/          # Test tools
├── zwei-common/               # Shared utilities & base classes
│   └── src/main/java/com/zwei/common/
│       ├── annotation/       # @Log, @RateLimiter, @DataScope, @Sensitive
│       ├── constant/          # CacheConstants, Constants, UserConstants
│       ├── core/              # BaseController, AjaxResult, R, TableDataInfo
│       ├── enums/             # BusinessType, UserStatus, HttpMethod
│       ├── exception/         # GlobalException, ServiceException, user/*
│       ├── filter/            # XssFilter, RepeatableFilter
│       └── utils/             # DateUtils, BeanUtils, DesensitizedUtil
├── zwei-framework/            # Core framework components
│   └── src/main/java/com/zwei/framework/
│       ├── aspectj/           # DataScopeAspect, LogAspect, RateLimiterAspect
│       ├── config/            # DruidConfig, RedisConfig, SecurityConfig, MyBatisConfig
│       ├── datasource/        # DynamicDataSource
│       ├── interceptor/       # RepeatSubmitInterceptor
│       ├── manager/           # AsyncManager, AsyncFactory
│       ├── security/          # JwtAuthenticationTokenFilter, AuthenticationEntryPointImpl
│       └── web/               # TokenService, SysLoginService, GlobalExceptionHandler
├── zwei-system/               # System management (users, roles, menus, depts)
├── zwei-quartz/               # Scheduled tasks
├── zwei-generator/            # Code generation
└── zwei-iot/                  # IoT business logic (device, hazardPoint, monitor, video, cache)
```

### zwei-iot Module Structure

```
zwei-iot/src/main/java/com/zwei/iot/
├── device/                    # Device & sensor management
│   ├── controller/            # DeviceController, SensorController
│   ├── domain/                # Device, DeviceSensor, SensorAttribute
│   ├── mapper/                # MyBatis mappers
│   └── service/              # IDeviceService, IDeviceSensorService
├── hazardpoint/               # Hazard point & group management
│   ├── controller/            # HazardPointController, HazardPointGroupController
│   ├── domain/                # HazardPoint, HazardPointGroup
│   ├── mapper/
│   └── service/
├── monitor/                   # Monitor types & content
│   ├── controller/            # MonitorTypeController, MonitorContentController
│   ├── domain/                # MonitorType, MonitorContent
│   ├── mapper/
│   └── service/
├── video/                     # Video device management
│   ├── controller/            # VideoDeviceController
│   ├── domain/                # VideoDevice
│   ├── mapper/
│   └── service/
└── cache/                     # Cache warmup on startup
    ├── config/                # CacheWarmupRunner, CacheWarmupTaskRegistry
    ├── service/               # IotCacheService
    └── warmup/                # HazardPointWarmupTask, MonitorTypeWarmupTask, etc.
```

## Frontend Structure

```
web/src/
├── App.vue
├── main.ts
├── router/index.ts            # Vue Router with auth guard
├── views/                     # Page components (lazy loaded)
│   ├── Login.vue
│   ├── dashboard/
│   ├── basic/                 # HazardPoint, Device, VideoDevice, MonitorType
│   ├── alarm/                 # RealtimeAlarm, AlarmCriteria, etc.
│   ├── report/                # Report, Query, Analysis, Screen
│   ├── iot/                   # AlarmEngine, DataParse
│   ├── system/               # Organization, Identity, Permission, Log
│   └── user/
├── layout/index.vue
└── utils/userApi.ts           # Axios instance with auth interceptors
```

## API Conventions

All backend APIs follow REST conventions (see `db/api_20260505.md` for full docs):

- **Base path**: `/api/v1`
- **Response format**: `{ code, msg, data, timestamp }`
- **Pagination**: `pageNum`, `pageSize` parameters with response metadata
- **Auth**: `Authorization: Bearer {token}` header
- **Status codes**: 200=成功, 400=参数错误, 401=未登录, 403=无权限, 500=服务器异常

### API Modules
| Prefix               | Description                                                      |
|----------------------|------------------------------------------------------------------|
| `/api/v1/system/`    | System management (auth, users, roles, menus, orgs)              |
| `/api/v1/basic/`     | Base data (hazard-points, monitor-types, devices, video-devices) |
| `/api/v1/alarm/`     | Alarm center (realtime, criteria, records, notifications)        |
| `/api/v1/monitor/`   | Monitoring data                                                  |
| `/api/v1/reports/`   | Reports & analysis                                               |
| `/api/v1/dashboard/` | Dashboard overview                                               |
| `/api/v1/iot/`       | IoT management (alarm-engine, data-parse)                        |
| `/api/v1/common/`    | Common (file upload)                                             |

## Key Backend Patterns

**BaseController** (`zwei-common/core/controller/BaseController.java`)
- Provides `getCurrentUserId()`, `getLoginUser()`, `getSysUser()`
- All controllers extend this for shared functionality

**AjaxResult** (`zwei-common/core/domain/AjaxResult.java`)
- Standard response wrapper with `success()`, `error()`, `warn()` static factories

**Service Layer**

- Interface + Implementation pattern in `zwei-iot/*/service/`
- Constructor injection (not field injection)

**DataScope** (`@DataScope` annotation + `DataScopeAspect`)
- Row-level data permission filtering based on org hierarchy

## Common Commands

### Backend (server/)

```bash
cd server

# Build all modules
mvn clean package

# Build specific module with dependencies
mvn clean install -pl zwei-iot -am

# Run tests for a specific module
mvn test -pl zwei-iot

# Run (from zwei-admin directory)
cd zwei-admin && mvn spring-boot:run
```

### Frontend (web/)

```bash
cd web

npm install          # Install dependencies
npm run dev          # Start dev server
npm run build        # Type check + production build
npm run preview      # Preview production build
```

## Configuration

Backend configuration is in `zwei-admin/src/main/resources/`:
- `application.yml` - Main Spring Boot config
- `application-druid.yml` - Database connection pool settings
- `logback.xml` - Logging configuration

Frontend config: `web/vite.config.ts` and `web/tsconfig.json`

## Database Schema (db/geo_hazard_monitor_v1.1.sql)

### Key Business Tables

| Table                       | Description                                                                                          |
|-----------------------------|------------------------------------------------------------------------------------------------------|
| `sys_organization`          | Organization hierarchy (id, code, name, parent_id, level)                                            |
| `hazard_point_group`        | Hazard point groups (id, code, name, sort_order, status)                                             |
| `hazard_point`              | Hazard points with geo coordinates (id, code, name, group_id, longitude, latitude, strike)           |
| `monitor_type`              | Monitor types - rain, water level, displacement, etc. (id, code, name, device_type)                  |
| `monitor_content`           | Monitor content metrics (id, monitor_type_id, code, name, unit, indicator_type)                      |
| `device`                    | Physical devices (id, code, name, icon, status, run_status, last_report_time)                        |
| `device_sensor`             | Device sensors (id, device_id, sensor_code, sensor_name, monitor_type_id)                            |
| `sensor_attribute`          | Sensor attributes/ranges (id, sensor_id, attr_code, attr_name, unit, range_min, range_max)           |
| `video_device`              | Video devices with stream URLs (id, code, name, protocol_code, stream_url, status)                   |
| `device_hazard_point`       | Device-to-hazard binding (device_id, hazard_point_id, install_longitude/latitude)                    |
| `video_device_hazard_point` | Video device-to-hazard binding                                                                       |
| `alarm_criteria`            | Alarm rules per hazard point (id, hazard_point_id, device_id, blue/yellow/orange/red_expression)     |
| `alarm_dispatch_rule`       | Notification dispatch rules (id, hazard_point_id, type, recipient_ids, channel)                      |
| `alarm_record`              | Alarm history (id, hazard_point_id, alarm_level, status, current_value, handle_time)                 |
| `monitor_data`              | Time-series monitoring data (id, hazard_point_id, device_id, sensor_id, attr_code, value, data_time) |
| `report_template`           | Report templates (id, code, name, type, content)                                                     |
| `report_record`             | Generated report records (id, template_id, hazard_point_id, report_date, file_path, status)          |

### System Tables (RuoYi-based)

- `sys_user`, `sys_role`, `sys_menu`, `sys_dept`, `sys_post`
- `sys_oper_log`, `sys_logininfor`, `sys_dict_type`, `sys_dict_data`
- `sys_config`, `sys_notice`, `sys_job`, `sys_job_log`

## Routing

Frontend routes are in `web/src/router/index.ts` with lazy loading. Authentication check via `localStorage.getItem('token')` - routes other than `/login` redirect if token missing.

## Commit Message Format

Follow `.trae/rules/git-commit-message.md` - conventional commits with type/scope/subject structure.