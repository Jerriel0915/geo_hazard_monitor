# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Geo-disaster Monitor (地质灾害监测预警系统) - A Vue 3 + TypeScript web application for monitoring geological hazards. The project root contains the `web/` subdirectory with the frontend code.

## Common Commands

All commands run from the `web/` directory:

```bash
cd web
npm install          # Install dependencies
npm run dev          # Start dev server
npm run build        # Build for production (runs vue-tsc type check)
npm run preview      # Preview production build
```

No test framework is currently configured.

## Architecture

### Tech Stack
- **Framework**: Vue 3 (Composition API) + TypeScript
- **Build**: Vite 5
- **Router**: Vue Router 4 (lazy-loaded routes)
- **UI**: Element Plus 2
- **Charts**: ECharts 6
- **Map**: Leaflet + Leaflet Draw

### Path Alias
- `@/` maps to `web/src/`

### Module Structure (web/src/views/)
- `dashboard/` - Dashboard homepage
- `holo-board/` - Holographic board views (Comprehensive, Alarm, Operation, Custom)
- `basic/` - Base data management (HazardPoint, MonitorType, Device, VideoDevice)
- `alarm/` - Alarm center (RealtimeAlarm, AlarmCriteria, AlarmNotification, AlarmDisposal)
- `report/` - Reports (Report, Query, Analysis, Screen)
- `iot/` - IoT management (AlarmEngine, DataParse)
- `system/` - System management (Organization, Identity, Permission, Log, Settings)
- `miniprogram/` - WeChat mini-program views

### Routing
Routes are defined in `web/src/router/index.ts` with lazy loading. The layout wrapper (`layout/index.vue`) provides the header navigation and tab-based page management. Authentication is handled via `localStorage.getItem('token')` - routes other than `/login` redirect to login if token is missing.

### API Conventions
Backend API design is documented in `db/接口设计文档.md`:
- Base path: `/api/v1`
- REST methods: GET (query), POST (create), PUT (update), DELETE (remove)
- Unified response: `{ code, msg, data, timestamp }`
- Pagination: `pageNum`, `pageSize`, `orderBy`, `order`

### Commit Message Format
Follow `.trae/rules/git-commit-message.md` - conventional commits with type/scope/subject structure.