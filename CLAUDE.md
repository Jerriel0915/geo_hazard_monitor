# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**知微——地质灾害监测预警系统** (Geo Hazard Monitor) is a multi-tenant management system for geological disaster monitoring and early warning. Built on RuoYi framework with Spring Boot 3 + Vue 3.

## Build Commands

```bash
# Backend (server/)
cd server
mvn clean install -DskipTests              # Build all modules
mvn clean install -Pdev -DskipTests        # Build with dev profile (default)
mvn clean install -Pprod -DskipTests        # Build with prod profile
mvn spring-boot:run -pl ghm-admin           # Run ghm-admin module directly

# Frontend (web/)
cd web
npm install                                  # Install dependencies
npm run dev                                  # Development server
npm run build                                # Production build
```

## Architecture

```
geo_hazard_monitor/
├── server/                    # Spring Boot backend (Maven multi-module)
│   ├── ghm-admin/             # Entry layer: auth, captcha, login
│   ├── ghm-common/            # Shared components (25 submodules)
│   │   ├── ghm-common-core/   # Core: constants, exceptions, utils
│   │   ├── ghm-common-mybatis/# MyBatis-Plus config, pagination, data permission
│   │   ├── ghm-common-redis/  # Redisson distributed lock, cache
│   │   ├── ghm-common-satoken/# Sa-Token authentication
│   │   ├── ghm-common-sse/    # Server-Sent Events for real-time push
│   │   ├── ghm-common-tenant/ # Multi-tenant support
│   │   └── ...
│   ├── ghm-extend/            # Optional extensions
│   │   ├── ghm-monitor-admin/ # Spring Boot Admin monitoring (port 9090)
│   │   └── ghm-snailjob-server/# SnailJob distributed job (port 8800)
│   └── ghm-modules/           # Business modules
│       ├── ghm-system/         # System management (user, role, menu, dept, config, etc.)
│       ├── ghm-workflow/       # Warm-Flow workflow engine
│       ├── ghm-job/            # SnailJob task scheduling
│       ├── ghm-generator/     # Code generation
│       └── ghm-demo/           # Demo examples
├── web/                       # Vue 3 frontend (Vite)
└── db/                        # Database SQL scripts

ghm-admin is the main Spring Boot application. ghm-extend modules (monitor-admin, snailjob-server) are optional and must be started separately if needed.
```

## Key Technologies

- **Auth**: Sa-Token (JWT) with multi-grant-type support (password, sms, email, social)
- **Database**: MyBatis-Plus with dynamic-datasource (multi-tenancy)
- **Cache/Lock**: Redisson + lock4j
- **Job**: SnailJob (port 17888 for client connections)
- **Workflow**: Warm-Flow (warm-flow-mybatis-plus-sb3-starter)
- **SSE**: Real-time notification push (e.g., system notices)
- **OSS**: AWS S3 compatible (minio, aliyun OSS)
- **SMS**: sms4j (alibaba, tencent, etc.)

## Important Conventions

### Package Structure
Controllers in `web/controller/` handle HTTP requests. Business logic lives in `service/impl/`. Domain objects follow: `Bo` (business object for input), `Vo` (view object for output), `Dto` (data transfer object).

### API Response Format
```java
R.ok(data)           // Success with data
R.fail(message)      // Failure with message
// Returns: { code: 200, msg: "success", data: {...}, timestamp: ... }
```

### Multi-Tenant
Use `TenantHelper` for tenant context. Data access automatically filtered by tenant ID via MyBatis-Plus data permission handler.

### Profile-Based Configuration
- `application-dev.yml` - Development (default active)
- `application-prod.yml` - Production
- `application-local.yml` - Local

### Testing Tags
Unit tests use JUnit 5 tags matching profile: `@Tag("dev")`, `@Tag("prod")`. Tests run via `mvn test` use profile-specific groups from pom.xml configuration.

## Database

SQL scripts in `db/`:
- `geo_hazard_monitor.sql` - Main schema
- `jclx.sql` - Monitoring types (监测类型)
- `yw_devices.sql` - Device records
- `yw_projects.sql` - Project/hazard point records
- `yw_videos.sql` - Video device records

## Common Tasks

```bash
# Run specific test
mvn test -Dtest=AssertUnitTest -pl ghm-admin

# Run with specific profile
mvn test -Pdev

# Disable optional services (avoid warnings)
# In application-dev.yml:
spring.boot.admin.client.enabled: false
snail-job.enabled: false
```

## Module Dependencies

```
ghm-admin
  └── ghm-common (core, mybatis, redis, satoken, sse, tenant, web, ...)
  └── ghm-modules/ghm-system
  └── ghm-modules/ghm-workflow
  └── ghm-modules/ghm-job
```