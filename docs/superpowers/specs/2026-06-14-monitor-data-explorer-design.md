# MonitorDataExplorer — 公共监测数据查询组件

> 状态: 设计完成，待实现
> 日期: 2026-06-14

## 背景

前端系统中至少 6 处独立实现了相似的监测数据查询能力：设备→传感器→指标级联选择 + 图表/表格双模展示。每个实现都有各自的本地状态、重复的 API 调用逻辑，且查询结果与 UI 之间的数据同步反复出 bug。

目标：将上述能力抽离为一个公共 composable + 一个公共 UI 组件，统一替换所有分散的实现。

## 参考

- 后端增强接口: `MonitorDataSensorController` (`/api/v1/monitor-data/sensor/*`) — 5 个传感器维度端点（latest / range / aggregate / completeness / trend）
- 现有 composable: `useHazardPointMonitor.ts`（将被删除）
- 现有组件: `HazardPointDetail.vue`（监测 tab 部分将被替换）

---

## 架构

```
src/
├── api/monitorData.ts              # 追加 5 个 sensor 端点封装
├── composables/
│   └── useMonitorData.ts           # 公共 composable（新建）
└── components/
    └── MonitorDataExplorer.vue     # 公共 UI 组件（新建）
```

### 数据流

```
调用方
  │  props: hazardPointId, showDevice, showAttr, ...
  ▼
<MonitorDataExplorer>
  │  useMonitorData({ hazardPointId })
  │    ├─ 加载设备列表 (getBoundDevices)
  │    ├─ 设备选择 → getDeviceSensors → 传感器下拉
  │    ├─ 传感器选择 → attrList → 指标下拉
  │    ├─ 查询 → getChartData / getMonitorDataPage
  │    └─ [可选] getSensorLatest / getSensorAggregate / getSensorCompleteness / getSensorTrend
  │
  ▼  渲染
  ├─ 筛选栏（设备/传感器/指标/值类型/时间范围 + 查询/重置）
  ├─ 图表/表格切换
  ├─ ApexCharts 图表（空状态占位、骨架屏 loading）
  └─ el-table 表格（空状态占位）
```

---

## `<MonitorDataExplorer>` Props

```ts
interface MonitorDataExplorerProps {
  /** 隐患点 ID（必填，组件入口） */
  hazardPointId: number
  /** 隐患点名称（用于标题/面包屑显示） */
  hazardPointName?: string

  // 筛选栏显隐控制
  showDevice?: boolean        // 默认 true
  showSensor?: boolean        // 默认 true
  showAttr?: boolean          // 默认 true
  showValueType?: boolean     // 默认 true
  showImportExport?: boolean  // 默认 false

  // 高级功能
  enableCompleteness?: boolean // 默认 false
  enableTrend?: boolean        // 默认 false

  // 初始值
  initialDeviceId?: number    // 预设设备 ID
  initialMode?: 'chart' | 'table' // 默认 'chart'
}
```

## Events

```ts
interface MonitorDataExplorerEmits {
  (e: 'data-loaded', data: { series: ChartData[]; list: MonitorDataPageItem[] }): void
  (e: 'device-change', deviceId: number): void
  (e: 'sensor-change', sensorId: number): void
}
```

---

## `useMonitorData` Composable

```ts
function useMonitorData(options: {
  hazardPointId: MaybeRef<number | null>
}) => {
  // 状态
  devices: Ref<BoundDevice[]>
  sensors: Ref<SensorItem[]>
  attrs: Ref<AttrItem[]>
  chartSeries: Ref<ChartData[]>
  tableData: Ref<MonitorDataPageItem[]>
  loading: Ref<boolean>
  mode: Ref<'chart' | 'table'>

  // 筛选
  filter: Reactive<{
    deviceId: string | number
    sensorId: string | number
    attrCode: string
    valueType: string
    timeRange: [string, string] | null
  }>

  // 方法
  selectDevice(id: number): Promise<void>
  selectSensor(id: number): void
  query(): Promise<void>
  querySensorLatest(deviceId: number, sensorCode: string): Promise
  querySensorAggregate(deviceId: number, sensorCode: string, expressions: ExpressionSpec[]): Promise
  querySensorCompleteness(deviceId: number, sensorCode: string, attrCode: string): Promise
  querySensorTrend(deviceId: number, sensorCode: string, attrCode: string): Promise
  reset(): void
}
```

### 内部流程

```
watch(hazardPointId) → loadDevices()
  └─ getBoundDevices(hazardPointId) → devices[]

selectDevice(deviceId)
  ├─ 取消前一个未完成的 AbortController
  ├─ filter.sensorId = ''; sensors = []; attrs = []
  ├─ getDeviceSensors(deviceId) → sensorMap.set(...)
  └─ sensors.value = [...]

selectSensor(sensorId)
  ├─ filter.attrCode = ''; attrs = []
  └─ sensorMap.get(sensorId)?.attrList → attrs.value

query()
  ├─ 默认时间范围: 最近 3 天
  ├─ chart mode → getChartData(...) → chartSeries
  └─ table mode → getMonitorDataPage(...) → tableData
```

### 关键细节

- **防重复请求**: `selectDevice` / `query` 在调用前 abort 上一个未完成的 `AbortController`
- **sensorMap**: `Map<number, SensorItem>` 缓存，selectSensor 时直接取 attrList，零请求
- **错误隔离**: 查询失败保留上次数据，toast 错误但不破坏展示
- **数据超过 500 点**: 组件顶部显示降采样提示条

---

## API 层新增

在 `api/monitorData.ts` 追加 5 个 sensor 维度端点：

| 函数 | 端点 | 用途 |
|---|---|---|
| `getSensorLatest` | `GET /monitor-data/sensor/latest` | 传感器最新值 |
| `getSensorRange` | `GET /monitor-data/sensor/range` | 区间数据（支持数值范围过滤） |
| `getSensorAggregate` | `POST /monitor-data/sensor/aggregate` | 多表达式聚合 |
| `getSensorCompleteness` | `GET /monitor-data/sensor/completeness` | 数据完整度 |
| `getSensorTrend` | `GET /monitor-data/sensor/trend` | 趋势分析 |

---

## UI 布局

### 筛选栏

```
┌──────────────────────────────────────────────────────────┐
│ [设备 ▼] [传感器 ▼] [指标 ▼] [值类型 ▼] [时间范围 ▾] [查询] [重置] │
└──────────────────────────────────────────────────────────┘
```

- 时间范围: `<el-date-picker type="datetimerange">`，全局中文 locale
- 查询按钮: loading 态 + disabled 防重复点击
- 各控件按 props 显隐

### 图表区域

- ApexCharts `<VueApexCharts>`，默认高度 400px（CSS var `--chart-height` 可覆盖）
- 空数据: 空状态占位（不是空白区域）
- 加载中: 骨架屏
- 用 `v-show` 切换图表/表格，避免 DOM 重建

### 表格区域

- `el-table`，列头动态生成（按 seriesName）
- 时间列固定左侧
- 空数据: `el-empty`

---

## 中文日期

`main.ts` 中加全局 Element Plus 中文 locale：

```ts
import zhCn from 'element-plus/es/locale/lang/zh-cn'
app.use(ElementPlus, { locale: zhCn })
```

`<el-date-picker>` 的 format 使用 `YYYY-MM-DD HH:mm:ss`，不出现英文月份。

---

## 交互优化

| 问题 | 优化 |
|---|---|
| 查询时无反馈 | 查询按钮 loading + 图表区域骨架屏 |
| 切换设备后传感器残留 | 传感器 + 指标自动清空重置 |
| 时间范围未选 | 默认最近 3 天，placeholder 提示 |
| 网络错误 | toast 错误 + 保留上次数据不清空 |
| 图表/表格切换闪烁 | `v-show` 替代 `v-if`，保持 DOM |
| 数据点过多 | 超 500 点时顶部提示降采样 |

---

## 迁移路径

### 新增文件

- `web/src/composables/useMonitorData.ts`
- `web/src/components/MonitorDataExplorer.vue`

### 修改文件

| 文件 | 变更 |
|---|---|
| `web/src/main.ts` | 加 `ElementPlus({ locale: zhCn })` |
| `web/src/api/monitorData.ts` | 追加 5 个 sensor 端点 |
| `views/basic/HazardPoint.vue` | 删除 composable 桥接代码，监测 tab 改用 `<MonitorDataExplorer>` |
| `views/basic/components/HazardPointDetail.vue` | 删除监测 tab 本地状态和模板（约 70 行），只保留基本信息+设备+告警三个 tab |
| `views/dashboard/components/DeviceDataModal.vue` | 改为 `<MonitorDataExplorer :show-device="false">` |
| `views/dashboard/components/DeviceDataPanel.vue` | 同上 |
| `views/miniprogram/HazardPoint.vue` | 监测 tab 改为 `<MonitorDataExplorer>` |

### 删除内容

- `useHazardPointMonitor.ts`（功能被 `useMonitorData.ts` 覆盖）
- `HazardPointDetail.vue` 中本地 ref: monitorSensors, monitorAttrs, chartSeriesData, chartOptions, monitorDataList, dataDisplayMode, localDataFilter 及对应模板

### 实施步骤

1. 新建 `useMonitorData` composable — 独立可测
2. 追加 API 封装 — `api/monitorData.ts`
3. 新建 `<MonitorDataExplorer>` 组件
4. `main.ts` 加中文 locale — 一行改动
5. 改造 `HazardPointDetail` — 监测 tab 替换为新组件
6. 改造 `DeviceDataModal` / `DeviceDataPanel`
7. 改造 `miniprogram/HazardPoint`
8. 删除 `useHazardPointMonitor.ts`
9. 全量 typecheck + 冒烟测试

---

## 不包含

- 导入/导出功能（当前各处都是 `ElMessage.info('开发中')`，不在此次范围）
- ECharts 支持（统一用 ApexCharts）
- 最新数据模式（可通过 composable 的 `querySensorLatest` 在调用方自行实现）
