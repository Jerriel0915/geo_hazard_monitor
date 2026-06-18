[根目录](../CLAUDE.md) > **web** (前端总览)

# web — 前端总览 (Vue 3 + TypeScript + Vite)

> 面包屑: [根目录](../CLAUDE.md) > **web**

## 模块职责

Vue 3 单页应用, 为地质灾害监测平台提供:

- 控制台首页 (含地图/隐患详情/告警/设备状态/健康/资源部件)
- 视图看板 (综合/告警/运营/自定义)
- 基础数据管理 (隐患点/监测类型/设备/视频设备)
- 告警 (实时/判据/通知/综合/处置/H5 处置)
- 报表/查询/分析/大屏
- IoT (数据解析 + 服务状态)
- 小程序侧视图
- 系统管理 (组织/身份/权限/日志/设置/通知公告)
- 个人资料

## 技术栈

- Vue 3.4 + TypeScript 5.3
- Vite 5
- Vue Router 4
- Element Plus 2.6
- ECharts 6 / ApexCharts
- Leaflet 1.9 + leaflet-draw
- Hls.js (HLS 播放) / mpegts.js (FLV 播放)
- Blockly (告警策略脚本编辑器)
- html2canvas + jspdf (截图/导出 PDF)
- Axios 1.16

## 目录结构

```
web/
├── index.html
├── package.json          # dev/build/preview 脚本
├── tsconfig.json
├── vite.config.ts        # Vite 配置, 代理 /api → 8080
├── nginx.conf            # 生产 Nginx 配置
├── public/
└── src/
    ├── main.ts
    ├── App.vue
    ├── api/              # 各业务域 API 封装
    ├── views/            # 页面组件
    │   ├── Login.vue
    │   ├── dashboard/
    │   ├── holo-board/
    │   ├── basic/
    │   ├── alarm/
    │   ├── report/
    │   ├── iot/
    │   ├── miniprogram/
    │   ├── system/
    │   └── user/
    ├── layout/
    │   └── index.vue     # 主布局 (侧边栏/顶部/通知铃铛)
    ├── router/
    │   └── index.ts      # 路由 + 登录守卫
    ├── utils/
    │   ├── request.ts    # Axios 封装
    │   ├── auth.ts       # 401 处理
    │   ├── errorHandler.ts
    │   ├── permission.ts
    │   ├── deviceIcon.ts
    │   └── userApi.ts
    └── components/       # 公共组件 (需进一步扫描)
```

## 业务模块 → 页面映射

| 业务域    | 前端路径                                                                                   | 入口页面                               |
|--------|----------------------------------------------------------------------------------------|------------------------------------|
| 控制台    | `/dashboard`                                                                           | `views/dashboard/Dashboard.vue`    |
| 视图看板   | `/holo-board/{comprehensive\|alarm\|operation\|custom}`                                | `views/holo-board/*.vue`           |
| 基础数据   | `/basic/{hazard-point\|monitor-type\|device\|video-device}`                            | `views/basic/*.vue`                |
| 告警     | `/alarm/{realtime\|criteria\|notification\|disposal\|composite\|notification-setting}` | `views/alarm/*.vue`                |
| H5 处置  | `/h5/disposal/:id?`                                                                    | `views/alarm/H5Disposal.vue` (免登录) |
| 报表     | `/report/{report\|query\|analysis\|screen}`                                            | `views/report/*.vue`               |
| IoT 内部 | `/iot/{data-parse\|service-status}`                                                    | `views/iot/*.vue`                  |
| 小程序    | `/miniprogram/{hazard-point\|device\|monitor-data\|event}`                             | `views/miniprogram/*.vue`          |
| 系统     | `/system/{organization\|identity\|permission\|log\|settings\|notice}`                  | `views/system/*.vue`               |
| 个人     | `/user/profile`                                                                        | `views/user/UserProfile.vue`       |
| 登录     | `/login`                                                                               | `views/Login.vue`                  |

## API 模块 (`src/api/`)

| 文件                  | 业务域  |
|---------------------|------|
| `device.ts`         | 设备   |
| `sensor.ts`         | 传感器  |
| `hazardPoint.ts`    | 隐患点  |
| `video.ts`          | 视频设备 |
| `monitor.ts`        | 系统监控 |
| `monitorData.ts`    | 监测数据 |
| `monitorType.ts`    | 监测类型 |
| `alarm.ts`          | 告警   |
| `realtimeAlarm.ts`  | 实时告警 |
| `compositeAlarm.ts` | 综合告警 |
| `alarmNotification.ts` | 告警通知中心 (事件 Tab) |
| `report.ts`         | 报表   |
| `notice.ts`         | 通知公告 |
| `system.ts`         | 系统管理 |

## 关键工具

- `request.ts` — Axios 封装, baseURL=`/api/v1`, 超时 10s, 自动 Bearer Token, 401 自动跳登录
- `auth.ts` — `handleAuthFailure()` 业务码 401/403 处理
- `permission.ts` — `hasPerm(perm)`, `hasAnyPerm(perms)` 前端权限指令
- `errorHandler.ts` — 全局错误捕获 (Vue errorHandler, unhandledrejection)
- `deviceIcon.ts` — 设备图标 (按 type 映射)

## 路由守卫

`router/index.ts` 中 `beforeEach`:

- 非 `/login` 路由, 无 `localStorage.token` → 重定向 `/login`
- 不在白名单的路由检查 token

## 开发命令

```bash
cd web
npm run dev       # 启动 Vite :5173, 代理 /api → 8080
npm run build     # vue-tsc 类型检查 + vite build, 输出 dist/
npm run preview   # 预览生产构建
```

## 性能与优化

- **路由懒加载**: 所有 `import('@/views/...')`
- **大屏组件**: `holo-board/*` 用 ECharts 大量图表, 建议开启 `chart.dispose()` 销毁
- **地图**: Leaflet 大数据点 (>1000) 用 `L.canvas()` 渲染

## 关键视图组件说明

- `views/dashboard/Dashboard.vue` — 综合控制台, 包含地图 + 多个部件 (告警/设备状态/健康/资源/隐患详情)
- `views/dashboard/components/` — 11 个部件组件 (MapBusinessToolbar / MapDrawToolbar / MapAuxiliaryBar /
  HazardDetailWidget / HazardAlarmWidget / AlarmWidget / DeviceStatusWidget / HealthWidget / ResourceWidget /
  DeviceDataPanel / DeviceDataModal / LayoutConfigDialog)
- `views/alarm/components/` — 14 个组件, 包括 Blockly 风格的条件构建器 (
  ConditionGroup/ConditionRow/GroupedRuleBuilder/LevelCriteriaCard/CriteriaDetailPanel/ExpressionEditDialog)
  与综合告警脚本 (CompositeAlarm*)
- `views/report/Screen.vue` — 大屏 (独立路由, 全屏展示)
- `views/alarm/H5Disposal.vue` — H5 现场处置 (免登录, 单独路由)

## 测试与质量

- **E2E**: 暂未集成, 建议引入 Playwright (见 `web/.playwright-mcp/` 已存在的痕迹)
- **单元**: 暂未集成
- **类型检查**: `vue-tsc` 在 `npm run build` 时执行
- **Lint**: 项目无 ESLint 配置, 建议补全

## 常见问题 (FAQ)

**Q: 路由加载慢?**
A: 检查: 1) 路由是否懒加载 (必须); 2) 是否有大组件未拆; 3) 是否在 `main.ts` 同步引入了大依赖 (ECharts 应按需引入)。

**Q: 401 后没跳登录?**
A: 检查 `utils/auth.ts` 的 `handleAuthFailure` 与 `router/index.ts` 的守卫。

**Q: SSE 通知没收到?**
A: 检查: 1) `EventSource` URL; 2) 后端是否启用 SSE (`@RestController` + `produces=text/event-stream`); 3) Nginx
是否缓冲 (需 `proxy_buffering off`)。

## 相关文件清单

- `package.json` / `vite.config.ts` / `nginx.conf`
- `src/main.ts` / `src/App.vue`
- `src/router/index.ts`
- `src/layout/index.vue`
- `src/api/*.ts` (13 个)
- `src/utils/*.ts` (6 个)
- `src/views/**/*.vue` (~60 个页面/组件)

## 通知中心 (2026-06 新增)

### 入口与布局

- 顶部铃铛 → 下拉面板双 Tab：「事件」(默认) / 「公告」
- 角标 = 事件未读 + 公告未读 (computed)
- SSE 双流：`/api/v1/alarm/stream` (告警事件，按 userId 路由) + `/api/v1/system/notice/stream` (公告)

### 关键文件

| 文件 | 职责 |
|---|---|
| `src/api/alarmNotification.ts` | 通知中心 API 封装 (4 接口 + Item/Summary 类型) |
| `src/layout/index.vue` | 铃铛 + 双 Tab + SSE 订阅 + 跳转逻辑 |
| `src/views/system/Settings.vue` | 「通知配置」分类 (11 个 sys_config 参数) |
| `src/views/alarm/RealtimeAlarm.vue` | 支持 `?alarmId=` query 自动打开详情 |
| `src/views/basic/Device.vue` | 支持 `?deviceId=` query 自动打开详情 |

### 跳转协议

| 通知 sourceType | 跳转 |
|---|---|
| `alarm` | `/alarm/realtime?alarmId={sourceId}` |
| `offline` | `/basic/device?deviceId={sourceId}` |

### 通道参数 (sys_config)

「系统设置 > 通知配置」分类下 11 个参数：
`notify.sms.{access-key-id, access-key-secret, sign-name, template.alarm, template.offline}` +
`notify.mail.{host, port, username, password, from, ssl}`

读取 `GET /system/config/configKey/{key}`，保存 `PUT /system/config/configKey/{key}` body `{configValue}`。

详见 `docs/通知中心使用手册.md`。

## 变更记录 (Changelog)

| 时间               | 变更                          |
|------------------|-----------------------------|
| 2026-06-10 18:52 | 首次生成模块级 CLAUDE.md (架构师自动扫描) |
| 2026-06-18 01:45 | 通知中心 v1: alarmNotification API + layout 双 Tab + Settings 通知配置分类 + 路由 query 兼容 |
