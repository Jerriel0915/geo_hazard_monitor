[根目录](../CLAUDE.md) > **web** (前端总览)

# web — 前端总览 (Vue 3 + TypeScript + Vite)

> 面包屑: [根目录](../CLAUDE.md) > **web**

## 模块职责

Vue 3 单页应用, 为地质灾害监测平台提供:

- 控制台首页 (含地图/隐患详情/告警/设备状态/健康/资源部件 + Terra AI 悬浮球)
- 视图看板 (综合/告警/运营/自定义)
- 基础数据管理 (隐患点/监测类型/设备/视频设备)
- 告警 (实时/判据/通知/综合/处置/H5 处置/分发规则/通知设置)
- 报表/查询/分析/大屏/共享策略
- IoT (数据解析 + 服务状态 + 异常报文)
- Terra AI 设置 (人格/模型/技能/工具管理)
- 小程序侧视图
- 系统管理 (组织/身份/权限/日志/设置/通知公告)
- 大屏展示 (报表大屏 + 灾害大屏 with Three.js 3D 地图)
- 个人资料

## 技术栈

- Vue 3.4 + TypeScript 5.3
- Vite 5
- Vue Router 4
- Element Plus 2.6
- ECharts 6 + echarts-gl 2.1 / ApexCharts
- Leaflet 1.9 + leaflet-draw
- Hls.js (HLS 播放) / mpegts.js (FLV 播放)
- Blockly (告警策略脚本编辑器)
- CodeMirror 6 (Groovy/JSON 脚本编辑器)
- Three.js 0.184 (3D 灾害大屏)
- html2canvas + jspdf (截图/导出 PDF)
- marked (Markdown 渲染)
- DOMPurify (XSS 防御)
- qrcode (二维码)
- Axios 1.16
- Vitest 4.1 (单元测试)
- @tweenjs/tween.js (动画补间)

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
    ├── api/              # 17 个业务域 API 模块
    ├── views/            # 页面组件
    │   ├── Login.vue
    │   ├── dashboard/
    │   ├── holo-board/
    │   ├── basic/
    │   ├── alarm/
    │   ├── report/
    │   ├── iot/
    │   ├── terra/        # Terra AI 设置 (人格/模型/技能/工具)
    │   ├── bigscreen/    # 大屏 (报表 + 灾害 3D)
    │   ├── miniprogram/
    │   ├── system/
    │   └── user/
    ├── composables/      # 5 个共享 composables
    │   ├── useLeafletMap.ts
    │   ├── useMapEditor.ts
    │   ├── useMonitorData.ts
    │   ├── usePagination.ts
    │   └── useTableSort.ts
    ├── lib/              # 3 个地理空间工具库 (均有测试)
    │   ├── boundaryCoords.ts
    │   ├── coordParser.ts
    │   └── mapGeometry.ts
    ├── constants/        # monitorIcons.ts
    ├── components/
    │   ├── terra/        # Terra AI 聊天面板 + 悬浮球 + SSE + 工具执行
    │   ├── map/          # 地图编辑器 (边界/坐标/定位)
    │   ├── MonitorDataExplorer.vue
    │   └── EChartsWrapper.vue
    ├── layout/
    │   └── index.vue     # 主布局 (侧边栏/顶部/通知铃铛 + 双 SSE)
    ├── router/
    │   └── index.ts      # 路由 + 登录守卫
    └── utils/
        ├── request.ts    # Axios 封装
        ├── auth.ts       # 401 处理
        ├── errorHandler.ts
        ├── permission.ts
        ├── deviceIcon.ts
        ├── groovyFormat.ts
        └── echarts.ts
```

## 业务模块 → 页面映射

| 业务域      | 前端路径                                                                                         | 入口页面                               |
|----------|----------------------------------------------------------------------------------------------|------------------------------------|
| 控制台      | `/dashboard`                                                                                 | `views/dashboard/Dashboard.vue`    |
| 视图看板     | `/holo-board/{comprehensive\|alarm\|operation\|custom}`                                      | `views/holo-board/*.vue`           |
| 基础数据     | `/basic/{hazard-point\|monitor-type\|device\|video-device}`                                  | `views/basic/*.vue`                |
| 告警       | `/alarm/{realtime\|criteria\|notification\|disposal\|composite\|notification-setting}`       | `views/alarm/*.vue`                |
| H5 处置    | `/h5/disposal/:id?`                                                                          | `views/alarm/H5Disposal.vue` (免登录) |
| 报表       | `/report/{report\|query\|analysis\|screen\|share-strategy}`                                  | `views/report/*.vue`               |
| IoT 内部   | `/iot/{data-parse\|service-status}`                                                          | `views/iot/*.vue`                  |
| Terra AI | `/terra/settings` (子路由: personality/models/skills/tools)                                   | `views/terra/SettingsLayout.vue`   |
| 大屏       | `/bigscreen/disaster`                                                                        | `views/bigscreen/DisasterScreen.vue` |
| 小程序      | `/miniprogram/{hazard-point\|device\|monitor-data}`                                          | `views/miniprogram/*.vue`          |
| 系统       | `/system/{organization\|identity\|permission\|log\|settings\|notice}`                        | `views/system/*.vue`               |
| 个人       | `/user/profile`                                                                              | `views/user/UserProfile.vue`       |
| 登录       | `/login`                                                                                     | `views/Login.vue`                  |

## API 模块 (`src/api/`) — 17 个模块

| 文件                    | 业务域     | 说明                       |
|-----------------------|---------|--------------------------|
| `device.ts`           | 设备      | 设备 CRUD + 状态管理           |
| `sensor.ts`           | 传感器     | 传感器 CRUD                 |
| `hazardPoint.ts`      | 隐患点     | 隐患点 + 分组管理               |
| `video.ts`            | 视频设备    | 视频设备 CRUD                |
| `monitor.ts`          | 系统监控    | 服务器/Redis/MQTT 监控       |
| `monitorData.ts`      | 监测数据    | 最新/分页/图表 + 传感器维度 (latest/range/aggregate/completeness/trend) |
| `monitorType.ts`      | 监测类型    | 监测类型 + 监测内容字典            |
| `alarm.ts`            | 告警      | 判据/策略/记录/处置 (合并了 realtimeAlarm + compositeAlarm) |
| `alarmDispatch.ts`    | 告警分发规则  | 分发规则 CRUD + 收件人选项        |
| `alarmNotification.ts` | 告警通知中心  | 事件 Tab: 已读/未读/分页          |
| `algoLibrary.ts`      | 算法库     | 算法信息 + 版本管理 (文件上传/下载)    |
| `dataParse.ts`        | 数据解析    | 解析策略 CRUD + 在线测试 + 执行日志   |
| `shareStrategy.ts`    | 共享策略    | 数据共享策略 CRUD + 脚本管理       |
| `terra.ts`            | Terra AI | 人格/模型/技能/工具/会话管理         |
| `report.ts`           | 报表      | 报表模板 + 生成记录              |
| `notice.ts`           | 通知公告    | 系统公告 + 已读追踪              |
| `system.ts`           | 系统管理    | 用户/角色/菜单/组织/字典/日志        |

> **注意**: `realtimeAlarm.ts` 和 `compositeAlarm.ts` 已合并入 `alarm.ts`，不再作为独立文件存在。

## 关键工具

- `request.ts` — Axios 封装, baseURL=`/api/v1`, 超时 10s, 自动 Bearer Token, 401 自动跳登录
- `auth.ts` — `handleAuthFailure()` 业务码 401/403 处理
- `permission.ts` — `hasPerm(perm)`, `hasAnyPerm(perms)` 前端权限指令
- `errorHandler.ts` — 全局错误捕获 (Vue errorHandler, unhandledrejection)
- `deviceIcon.ts` — 设备图标 (按 type 映射)
- `groovyFormat.ts` — Groovy 脚本代码格式化
- `echarts.ts` — ECharts 按需导入工具

## 共享 Composables (`src/composables/`)

| 文件                  | 用途                                              |
|----------------------|--------------------------------------------------|
| `useLeafletMap.ts`   | Leaflet 地图生命周期 (天地图瓦片, init/destroy/invalidate)   |
| `useMapEditor.ts`    | 地图编辑器 (多边形绘制/编辑, 走向线, 顶点拖拽, 键盘快捷键)            |
| `useMonitorData.ts`  | 监测数据加载 (设备/传感器级联, 图表/表格模式, 降采样, 传感器 API)     |
| `usePagination.ts`   | 通用分页 + 搜索 + 选择 (消除 16+ 页面重复代码)                 |
| `useTableSort.ts`    | 通用表格字段排序 (asc/desc/none 循环)                     |

## 共享工具库 (`src/lib/`) — 均含测试

| 文件                  | 用途                                              |
|----------------------|--------------------------------------------------|
| `boundaryCoords.ts`  | LatLng/BoundaryCoords 类型, 序列化, 质心, 走向角计算     |
| `coordParser.ts`     | 单行/多行坐标解析 (decimal + DMS), 智能经纬度顺序检测        |
| `mapGeometry.ts`     | Haversine 距离, 中点, 边中点, 命中检测, 自相交检测           |

## SSE 实时推送 — 3 种实现

| 位置                      | 方式                        | 端点                                | 说明                       |
|--------------------------|---------------------------|------------------------------------|--------------------------|
| `layout/index.vue`       | 原生 `EventSource` (GET)    | `/api/v1/alarm/stream` + `/api/v1/system/notice/stream` | 双 SSE 流, 指数退避重连 (最多 10 次) |
| `components/terra/terra-sse.ts` | 自定义 `fetch()` + `ReadableStream` (POST) | `/api/v1/terra/chat`              | Terra AI 聊天流, 手动 SSE 行解析 |
| `views/system/composables/useLogStream.ts` | 自定义 `fetch()` + `ReadableStream` (GET) | `/api/v1/logs/stream` + `/api/v1/logs/console-stream` | 日志实时尾巴, 类型过滤          |

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

- **单元测试**: Vitest 4.1 + @vitest/coverage-v8, 13 个测试文件分布在 `__tests__/` 目录
  - `composables/__tests__/` — useLeafletMap, useMapEditor, usePagination, useTableSort
  - `lib/__tests__/` — boundaryCoords, coordParser, mapGeometry
  - `utils/__tests__/` — errorHandler, indicatorType, logTags
  - `components/map/__tests__/` — MapBoundaryEditor
  - `views/basic/components/__tests__/` — CalcScriptEditor
  - `views/basic/components/script-editor/__tests__/` — 4 个测试文件
- **E2E**: Playwright MCP 支持 (`web/.playwright-mcp/`)
- **类型检查**: `vue-tsc` 在 `npm run build` 时执行
- **Lint**: Husky + commitlint (`commitlint.config.js`)

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
| 2026-06-18 01:45 | 通知中心 v1: alarmNotification API + layout 双 Tab + Settings 通知配置分类 |
| 2026-06-24 18:00 | 通知中心 v2: 双 Tab 分页 + NoticeDetail.vue + 告警跳转失败提示 |
| 2026-06-25 10:00 | SSE 订阅泄漏修复: 重连定时器生命周期管理 |
| 2026-07-01 | Terra AI 集成: TerraWidget 悬浮球 + TerraChatPanel + TerraMessage + terra-sse.ts + useTerraChat + TerraToolExecutor + 4 个设置页 |
| 2026-07-01 | IoT 数据解析: DataParse 策略管理页 + 详情/表单/测试对话框 + ServiceStatus 页 |
| 2026-07-01 | 告警分发规则: alarmDispatch API + 分发规则管理 |
| 2026-07-01 | 算法库: algoLibrary API + 算法信息/版本管理 |
| 2026-07-01 | 共享策略: shareStrategy API + ShareStrategy 页面 + 脚本抽屉 |
| 2026-07-01 | 监测数据增强: useMonitorData composable + MonitorDataExplorer 组件 + 传感器维度 API |
| 2026-07-01 | 数据分析: Analysis 页面 (相关性分析 + 数据网格模式) |
| 2026-07-01 | 共享基础设施: 5 个 composables + 3 个 lib 工具 + 13 个测试文件 |
| 2026-07-01 | 地图编辑器: 6 个 map 组件 + useMapEditor composable |
| 2026-07-01 | 灾害大屏: DisasterScreen + ThreeMap (Three.js 3D 地图) |
| 2026-07-01 | 代码编辑器: Groovy 格式化 + CodeMirror JSON 编辑器 + ScriptTestDialog |
| 2026-07-01 | 依赖更新: CodeMirror 6, Three.js, marked, DOMPurify, qrcode, Vitest |
| 2026-07-01 | 代码质量: Husky + commitlint 配置 |
| 2026-07-03 | 全面文档更新: API 模块 13→17, 路由补充, SSE 三系统, composables/lib 文档化 |
