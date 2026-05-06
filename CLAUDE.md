# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Geo-disaster Monitor (地质灾害监测预警系统) - A Vue 3 + Java Spring Boot system for monitoring geological hazards. The project has two main components:
- `web/` - Vue 3 frontend
- `server/` - Java Spring Boot backend (RuoYi-based)

## Server Backend Architecture

### Tech Stack
- **Framework**: Spring Boot 4.0.3 (RuoYi v3.9.2 framework)
- **Build**: Maven multi-module
- **Database**: MySQL with Druid connection pool
- **Cache**: Redis
- **ORM**: MyBatis + PageHelper
- **Auth**: JWT (jjwt 0.9.1)
- **API Docs**: SpringDoc OpenAPI (Knife4j-style)
- **Java Version**: 17

### Module Structure (server/)

```
server/
├── pom.xml                    # Parent POM, defines all modules
├── sql/
│   ├── ry_20260417.sql        # Core RuoYi tables
│   └── quartz.sql             # Quartz scheduler tables
├── zwei-admin/                # Web entry point & controllers
│   └── src/main/java/com/zwei/
│       ├── RuoYiApplication.java     # Main application
│       └── web/controller/
│           ├── common/               # Common (upload, captcha)
│           ├── monitor/             # Cache, Server, Logs, Online users
│           ├── system/               # SysIndex, SysUser, SysRole, SysMenu, etc.
│           └── tool/                 # Test tools
├── zwei-common/                # Shared utilities & base classes
│   └── src/main/java/com/zwei/common/
│       ├── annotation/         # @Log, @RateLimiter, @DataScope, @Sensitive
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
│       ├── interceptor/      # RepeatSubmitInterceptor
│       ├── manager/          # AsyncManager, AsyncFactory (async task processing)
│       ├── security/         # JwtAuthenticationTokenFilter, AuthenticationEntryPointImpl
│       └── web/              # TokenService, SysLoginService, GlobalExceptionHandler
├── zwei-system/              # System management business logic
│   └── src/main/java/com/zwei/system/
│       ├── domain/           # SysUser, SysRole, SysMenu, SysDept, SysConfig
│       ├── mapper/           # MyBatis mappers
│       └── service/         # Service interfaces & implementations
├── zwei-quartz/              # Scheduled tasks
└── zwei-generator/           # Code generation
```

### API Conventions

All backend APIs follow REST conventions documented in `db/接口设计文档.md`:

- **Base path**: `/api/v1`
- **Response format**: `{ code, msg, data, timestamp }`
- **Pagination**: `pageNum`, `pageSize` parameters with `TableDataInfo` response
- **Auth**: `Authorization: Bearer {token}` header
- **Status codes**: 200=成功, 400=参数错误, 401=未登录, 403=无权限, 500=服务器异常

### API Modules (from `db/接口设计文档.md`)
| Prefix | Description |
|--------|-------------|
| `/api/v1/system/` | System management (auth, users, roles, menus, orgs) |
| `/api/v1/basic/` | Base data (hazard-points, monitor-types, devices, video-devices) |
| `/api/v1/alarm/` | Alarm center (realtime, criteria, records, notifications) |
| `/api/v1/monitor/` | Monitoring data |
| `/api/v1/reports/` | Reports & analysis |
| `/api/v1/dashboard/` | Dashboard overview |
| `/api/v1/iot/` | IoT management (alarm-engine, data-parse) |
| `/api/v1/common/` | Common (file upload) |

### Key Backend Patterns

**BaseController** (`zwei-common/core/controller/BaseController.java`)
- Provides `getCurrentUserId()`, `getLoginUser()`, `getSysUser()`
- All controllers extend this for shared functionality

**AjaxResult** (`zwei-common/core/domain/AjaxResult.java`)
- Standard response wrapper with `success()`, `error()`, `warn()` static factories

**Service Layer**
- Interface + Implementation pattern in `zwei-system/service/`
- Implementations use `@Autowired` injected mappers

**DataScope** (`@DataScope` annotation + `DataScopeAspect`)
- Row-level data permission filtering based on org hierarchy

## Common Commands

### Backend (server/)

```bash
cd server

# Build all modules
mvn clean package

# Run (from zwei-admin directory)
cd zwei-admin
mvn spring-boot:run
# Or with: java -jar zwei-admin/target/zwei-admin.jar

# Build specific module
mvn clean install -pl zwei-admin -am
```

### Frontend (web/)

```bash
cd web

npm install          # Install dependencies
npm run dev          # Start dev server
npm run build        # Build for production (runs vue-tsc type check)
npm run preview      # Preview production build
```

## Configuration

Backend configuration is in `zwei-admin/src/main/resources/`:
- `application.yml` - Main Spring Boot config
- `application-druid.yml` - Database connection pool settings
- `logback.xml` - Logging configuration

Frontend config: `web/vite.config.ts` and `web/tsconfig.json`

## Routing

Frontend routes are in `web/src/router/index.ts` with lazy loading. Authentication check via `localStorage.getItem('token')` - routes other than `/login` redirect if token missing.

## Commit Message Format

Follow `.trae/rules/git-commit-message.md` - conventional commits with type/scope/subject structure.