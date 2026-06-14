# MonitorDataExplorer 实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 将监测数据查询能力抽离为公共 composable `useMonitorData` + 公共组件 `<MonitorDataExplorer>`，替换 6 处分散的重复实现。

**架构：** 分层结构 — composable 负责所有数据逻辑（级联查询、AbortController 防重复、错误隔离），组件负责筛选栏 + ApexCharts 图表 + el-table 表格的 UI 渲染。组件通过 props 控制筛选栏各控件显隐。

**技术栈：** Vue 3.4 + TypeScript + ApexCharts (vue3-apexcharts) + Element Plus 2.6

---

## 文件结构

| 操作 | 文件 | 职责 |
|------|------|------|
| 创建 | `web/src/composables/useMonitorData.ts` | 公共 composable：设备/传感器/指标级联、图表/表格查询、高级 sensor API |
| 创建 | `web/src/components/MonitorDataExplorer.vue` | 公共 UI 组件：筛选栏 + 图表 + 表格 |
| 修改 | `web/src/api/monitorData.ts` | 追加 5 个 sensor 端点封装 |
| 修改 | `web/src/App.vue` | 包裹 `<el-config-provider :locale="zhCn">` |
| 修改 | `web/src/views/basic/HazardPoint.vue` | 删除 composable 桥接代码，改为透传 props |
| 修改 | `web/src/views/basic/components/HazardPointDetail.vue` | 监测 tab 替换为新组件，删除本地状态（~8 refs + 对应模板） |
| 修改 | `web/src/views/dashboard/components/DeviceDataModal.vue` | 替换为新组件 |
| 修改 | `web/src/views/dashboard/components/DeviceDataPanel.vue` | 替换为新组件 |
| 修改 | `web/src/views/miniprogram/HazardPoint.vue` | 监测区域替换为新组件 |
| 删除 | `web/src/views/basic/composables/useHazardPointMonitor.ts` | 功能已被 useMonitorData 覆盖 |

---

### 任务 1：追加 sensor API 封装

**文件：**
- 修改：`web/src/api/monitorData.ts`（在文件末尾追加，第 88 行之后）

- [ ] **步骤 1：追加类型定义和 5 个 API 函数**

在文件末尾 `export const getChartData = ...` 之后追加以下代码：

```ts
// ── 传感器维度接口（MonitorDataSensorController） ──

export interface ExpressionSpec {
  alias?: string
  function?: string  // AVG | MAX | MIN | SUM | COUNT | FIRST_VALUE | LAST_VALUE | EXTREME | STDDEV | P50 | P95 | P99
  left?: ExpressionSpec
  op?: '+' | '-' | '*' | '/'
  right?: ExpressionSpec
  value?: number
}

export interface SensorAggregateVO {
  intervals: { startTime: string; endTime: string }[]
  columns: { alias: string; label: string; unit: string }[]
  rows: Record<string, number | null>[]
}

export interface SensorCompletenessVO {
  expected: number
  actual: number
  rate: number
  gaps: { start: string; end: string }[]
}

export interface SensorTrendVO {
  slope: number
  intercept: number
  direction: 'up' | 'down' | 'stable'
  confidence: number
}

/** 传感器维度 — 最新值 */
export const getSensorLatest = (deviceId: number, sensorCode: string, attrCode?: string) =>
  unwrap<LatestDataItem[]>(request.get('/monitor-data/sensor/latest', {
    params: { deviceId, sensorCode, attrCode }
  }))

/** 传感器维度 — 区间数据 */
export const getSensorRange = (params: {
  deviceId: number
  sensorCode: string
  attrCode?: string
  startTime: string
  endTime: string
  minValue?: number
  maxValue?: number
  limit?: number
  offset?: number
}) =>
  unwrap(request.get('/monitor-data/sensor/range', { params }))

/** 传感器维度 — 多表达式聚合 */
export const getSensorAggregate = (
  params: {
    deviceId: number
    sensorCode: string
    startTime: string
    endTime: string
    granularity?: string
    minValue?: number
    maxValue?: number
  },
  expressions: ExpressionSpec[]
) =>
  unwrap<SensorAggregateVO>(
    request.post('/monitor-data/sensor/aggregate', expressions, { params })
  )

/** 传感器维度 — 数据完整度 */
export const getSensorCompleteness = (params: {
  deviceId: number
  sensorCode: string
  attrCode: string
  startTime: string
  endTime: string
  expectedIntervalMs?: number
}) =>
  unwrap<SensorCompletenessVO>(request.get('/monitor-data/sensor/completeness', { params }))

/** 传感器维度 — 趋势分析 */
export const getSensorTrend = (params: {
  deviceId: number
  sensorCode: string
  attrCode: string
  startTime: string
  endTime: string
}) =>
  unwrap<SensorTrendVO>(request.get('/monitor-data/sensor/trend', { params }))
```

- [ ] **步骤 2：TypeCheck 验证**

```bash
cd web && npx vue-tsc --noEmit 2>&1
```

预期：PASS，无类型错误

- [ ] **步骤 3：Commit**

```bash
git add web/src/api/monitorData.ts
git commit -m "feat: add sensor-dimension monitor data API wrappers"
```

---

### 任务 2：创建 useMonitorData composable

**文件：**
- 创建：`web/src/composables/useMonitorData.ts`

- [ ] **步骤 1：创建 composable 文件**

```ts
// web/src/composables/useMonitorData.ts
import { ref, reactive, watch, type Ref, type MaybeRef, toValue } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getChartData,
  getMonitorDataPage,
  getLatestData,
  getSensorLatest,
  getSensorAggregate,
  getSensorCompleteness,
  getSensorTrend,
  type ChartData,
  type MonitorDataPageItem,
  type LatestDataItem,
  type ExpressionSpec,
  type SensorAggregateVO,
  type SensorCompletenessVO,
  type SensorTrendVO,
} from '@/api/monitorData'
import { getDeviceSensors, type SensorItem, type SensorAttrItem } from '@/api/sensor'
import { getBoundDevices } from '@/api/hazardPoint'
import { showRequestErrorMessage } from '@/utils/errorHandler'

export interface BoundDeviceItem {
  deviceId: number
  deviceName: string
  deviceCode: string
  sensors?: { id: number; name: string }[]
}

export interface AttrItem {
  code: string
  label: string
}

const CHART_COLORS = [
  '#3b82f6', '#ef4444', '#10b981', '#f59e0b', '#8b5cf6',
  '#06b6d4', '#ec4899', '#84cc16', '#f97316', '#6366f1',
]

export interface UseMonitorDataOptions {
  hazardPointId: MaybeRef<number | null>
}

export function useMonitorData(opts: UseMonitorDataOptions) {
  // ── 数据状态 ──
  const devices = ref<BoundDeviceItem[]>([])
  const sensors = ref<SensorItem[]>([])
  const attrs = ref<AttrItem[]>([])
  const chartSeries = ref<ChartData[]>([])
  const tableData = ref<MonitorDataPageItem[]>([])
  const loading = ref(false)
  const mode = ref<'chart' | 'table'>('chart')

  // ── 筛选状态 ──
  const filter = reactive({
    deviceId: '' as string | number,
    sensorId: '' as string | number,
    attrCode: '',
    valueType: 'current',
    timeRange: null as [string, string] | null,
  })

  // ── 缓存 ──
  const sensorMap = new Map<number, SensorItem>()

  // ── AbortController ──
  let abortController: AbortController | null = null

  // ── 加载设备列表 ──
  const loadDevices = async () => {
    const hpId = toValue(opts.hazardPointId)
    if (!hpId) {
      devices.value = []
      return
    }
    try {
      const res: any = await getBoundDevices(String(hpId))
      devices.value = (res.data || res || []).map((d: any) => ({
        deviceId: d.deviceId,
        deviceName: d.deviceName,
        deviceCode: d.deviceCode,
        sensors: d.sensors || [],
      }))
    } catch {
      devices.value = []
    }
  }

  // ── 选择设备 → 加载传感器 ──
  const selectDevice = async (deviceId: number | string) => {
    // 取消前一个未完成的请求
    abortController?.abort()
    abortController = new AbortController()

    filter.sensorId = ''
    filter.attrCode = ''
    sensors.value = []
    attrs.value = []

    if (!deviceId) return

    try {
      const list = await getDeviceSensors(Number(deviceId))
      sensorMap.clear()
      for (const s of list) {
        if (s.id != null) sensorMap.set(s.id, s)
      }
      sensors.value = list
    } catch (error: any) {
      if (error?.name !== 'AbortError' && error?.code !== 'ERR_CANCELED') {
        showRequestErrorMessage(error, '获取传感器列表失败')
      }
    }
  }

  // ── 选择传感器 → 提取指标 ──
  const selectSensor = (sensorId: number | string) => {
    filter.attrCode = ''
    if (!sensorId) {
      attrs.value = []
      return
    }
    const sensor = sensorMap.get(Number(sensorId))
    attrs.value = (sensor?.attrList || []).map((a: SensorAttrItem) => ({
      code: a.attrCode,
      label: `${a.attrName || a.attrCode}${a.unit ? ` (${a.unit})` : ''}`,
    }))
  }

  // ── 查询 ──
  const query = async () => {
    const hpId = toValue(opts.hazardPointId)
    if (!hpId) {
      ElMessage.warning('请先选择隐患点')
      return
    }

    // 默认时间范围：最近 3 天
    let startTime: string
    let endTime: string
    if (filter.timeRange && filter.timeRange[0] && filter.timeRange[1]) {
      startTime = filter.timeRange[0]
      endTime = filter.timeRange[1]
    } else {
      const end = new Date()
      const start = new Date(end.getTime() - 3 * 24 * 60 * 60 * 1000)
      const fmt = (d: Date) => {
        const pad = (n: number) => String(n).padStart(2, '0')
        return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
      }
      startTime = fmt(start)
      endTime = fmt(end)
    }

    const baseParams = {
      hazardPointId: hpId,
      deviceId: filter.deviceId ? Number(filter.deviceId) : undefined,
      sensorId: filter.sensorId ? Number(filter.sensorId) : undefined,
      attrCode: filter.attrCode || undefined,
      valueType: filter.valueType || undefined,
      startTime,
      endTime,
    }

    loading.value = true
    try {
      if (mode.value === 'chart') {
        const series = await getChartData(baseParams as any)
        chartSeries.value = series || []
        ElMessage.success(
          `加载 ${series.length} 条曲线，共 ${series[0]?.labels.length || 0} 个数据点`
        )
      } else {
        const res = await getMonitorDataPage({ ...baseParams, pageNum: 1, pageSize: 100 })
        tableData.value = (res as any).rows || []
        ElMessage.success(`加载 ${tableData.value.length} 条数据`)
      }
    } catch (error) {
      showRequestErrorMessage(error, '获取监测数据失败')
    } finally {
      loading.value = false
    }
  }

  // ── 高级方法 ──
  const querySensorLatest = async (deviceId: number, sensorCode: string, attrCode?: string) => {
    return getSensorLatest(deviceId, sensorCode, attrCode)
  }

  const querySensorAggregate = async (
    deviceId: number, sensorCode: string,
    startTime: string, endTime: string,
    expressions: ExpressionSpec[],
    granularity?: string,
  ) => {
    return getSensorAggregate(
      { deviceId, sensorCode, startTime, endTime, granularity },
      expressions,
    )
  }

  const querySensorCompleteness = async (
    deviceId: number, sensorCode: string, attrCode: string,
    startTime: string, endTime: string, expectedIntervalMs?: number,
  ) => {
    return getSensorCompleteness({ deviceId, sensorCode, attrCode, startTime, endTime, expectedIntervalMs })
  }

  const querySensorTrend = async (
    deviceId: number, sensorCode: string, attrCode: string,
    startTime: string, endTime: string,
  ) => {
    return getSensorTrend({ deviceId, sensorCode, attrCode, startTime, endTime })
  }

  // ── 重置 ──
  const reset = () => {
    filter.deviceId = ''
    filter.sensorId = ''
    filter.attrCode = ''
    filter.valueType = 'current'
    filter.timeRange = null
    sensors.value = []
    attrs.value = []
    chartSeries.value = []
    tableData.value = []
  }

  // ── 构建图表配置 ──
  const buildChartOptions = (seriesData: ChartData[]) => {
    if (seriesData.length === 0) return {}
    const allLabels = new Set<string>()
    for (const s of seriesData) for (const l of s.labels) allLabels.add(l)
    const xCategories = Array.from(allLabels).sort()

    return {
      chart: {
        type: 'area' as const,
        height: '100%',
        fontFamily: 'inherit',
        toolbar: {
          tools: {
            download: true, selection: true, zoom: true,
            zoomin: true, zoomout: true, pan: true, reset: true,
          },
        },
        zoom: { enabled: true, type: 'x' as const },
        animations: { enabled: true, easing: 'easeinout' as const, speed: 800 },
      },
      colors: CHART_COLORS,
      dataLabels: { enabled: false },
      stroke: { curve: 'smooth' as const, width: 2 },
      fill: {
        type: 'gradient',
        gradient: { shadeIntensity: 1, opacityFrom: 0.2, opacityTo: 0.02, stops: [0, 100] },
      },
      markers: { size: 0, hover: { size: 5 } },
      grid: {
        borderColor: '#e7e7e7',
        strokeDashArray: 4,
        padding: { top: 10, right: 10, bottom: 5, left: 10 },
      },
      legend: {
        position: 'top' as const,
        horizontalAlign: 'center' as const,
        fontSize: '13px',
        fontWeight: 500,
        markers: { width: 12, height: 12, radius: 6, offsetX: -4 },
        itemMargin: { horizontal: 16, vertical: 4 },
        offsetY: -4,
      },
      xaxis: {
        type: 'category' as const,
        categories: xCategories,
        labels: { rotate: -30, style: { fontSize: '11px', colors: '#666' } },
        tickAmount: Math.min(xCategories.length, 10),
        tooltip: { enabled: false },
      },
      yaxis: {
        title: {
          text: seriesData[0]?.unit || '',
          style: { fontSize: '12px', color: '#888' },
        },
        labels: {
          formatter: (val: number) => val != null ? Number(val.toFixed(2)).toString() : '',
        },
      },
      tooltip: { shared: true, intersect: false },
      series: seriesData.map((s) => {
        const points = s.labels.map((l, i) => ({ x: l, y: s.values[i] }))
        return { name: s.seriesName, data: points }
      }),
    }
  }

  // ── 监听 hazardPointId 变化自动加载 ──
  watch(() => toValue(opts.hazardPointId), () => {
    reset()
    loadDevices()
  }, { immediate: true })

  return {
    // 状态
    devices,
    sensors,
    attrs,
    chartSeries,
    tableData,
    loading,
    mode,
    // 筛选
    filter,
    // 方法
    selectDevice,
    selectSensor,
    query,
    reset,
    buildChartOptions,
    // 高级
    querySensorLatest,
    querySensorAggregate,
    querySensorCompleteness,
    querySensorTrend,
  }
}
```

- [ ] **步骤 2：TypeCheck 验证**

```bash
cd web && npx vue-tsc --noEmit 2>&1
```

预期：PASS

- [ ] **步骤 3：Commit**

```bash
git add web/src/composables/useMonitorData.ts
git commit -m "feat: add useMonitorData composable for unified monitor data query"
```

---

### 任务 3：创建 MonitorDataExplorer 组件

**文件：**
- 创建：`web/src/components/MonitorDataExplorer.vue`

- [ ] **步骤 1：创建组件文件**

```vue
<!-- web/src/components/MonitorDataExplorer.vue -->
<template>
  <div class="monitor-data-explorer">
    <!-- 筛选栏 -->
    <div class="mde-filters">
      <el-select
        v-if="showDevice"
        v-model="filter.deviceId"
        placeholder="选择设备"
        clearable
        style="width: 140px"
        @change="onDeviceChange"
      >
        <el-option
          v-for="d in devices"
          :key="d.deviceId"
          :label="d.deviceName"
          :value="d.deviceId"
        />
      </el-select>

      <el-select
        v-if="showSensor"
        v-model="filter.sensorId"
        placeholder="选择传感器"
        clearable
        style="width: 140px"
        @change="onSensorChange"
      >
        <el-option
          v-for="s in sensors"
          :key="s.id"
          :label="s.sensorName"
          :value="s.id"
        />
      </el-select>

      <el-select
        v-if="showAttr"
        v-model="filter.attrCode"
        placeholder="选择指标"
        clearable
        style="width: 160px"
      >
        <el-option
          v-for="a in attrs"
          :key="a.code"
          :label="a.label"
          :value="a.code"
        />
      </el-select>

      <el-select
        v-if="showValueType"
        v-model="filter.valueType"
        placeholder="聚合粒度"
        style="width: 120px"
      >
        <el-option label="原始值" value="current" />
        <el-option label="小时均值" value="hour" />
        <el-option label="日均值" value="24h" />
        <el-option label="3日均值" value="72h" />
      </el-select>

      <el-date-picker
        v-model="filter.timeRange"
        type="datetimerange"
        range-separator="至"
        start-placeholder="开始时间"
        end-placeholder="结束时间"
        format="YYYY-MM-DD HH:mm:ss"
        value-format="YYYY-MM-DD HH:mm:ss"
        style="width: 360px"
      />

      <el-button type="primary" :loading="loading" @click="query">查询</el-button>
      <el-button @click="reset">重置</el-button>

      <template v-if="showImportExport">
        <el-button @click="onImport">导入数据</el-button>
        <el-button @click="onExport">导出数据</el-button>
      </template>
    </div>

    <!-- 图表/表格切换 -->
    <div class="mde-toolbar">
      <el-button-group>
        <el-button
          :type="mode === 'chart' ? 'primary' : 'default'"
          size="small"
          @click="mode = 'chart'"
        >图表展示</el-button>
        <el-button
          :type="mode === 'table' ? 'primary' : 'default'"
          size="small"
          @click="mode = 'table'"
        >表格展示</el-button>
      </el-button-group>
    </div>

    <!-- 数据点过多提示 -->
    <div v-if="dataPointWarning" class="mde-warning">
      数据点较多（{{ totalDataPoints }} 点），建议缩小时间范围以提升性能
    </div>

    <!-- 图表视图 -->
    <div v-show="mode === 'chart'" class="mde-chart-area">
      <div v-if="loading" class="mde-skeleton"></div>
      <VueApexCharts
        v-else-if="chartSeries.length > 0"
        type="area"
        height="400"
        :options="chartOptions"
        :series="chartOptions.series"
      />
      <div v-else class="mde-empty">
        <span>暂无数据，请选择条件后点击查询</span>
      </div>
    </div>

    <!-- 表格视图 -->
    <div v-show="mode === 'table'" class="mde-table-area">
      <el-table
        v-if="!loading"
        :data="tableData"
        border
        stripe
        size="small"
        max-height="400"
      >
        <el-table-column prop="dataTime" label="时间" min-width="180" align="center" />
        <el-table-column prop="deviceName" label="设备" width="150" align="center" />
        <el-table-column prop="sensorName" label="传感器" width="120" align="center" />
        <el-table-column prop="attrName" label="指标" width="100" align="center" />
        <el-table-column prop="value" label="数值" width="100" align="center" />
        <el-table-column prop="unit" label="单位" width="80" align="center" />
        <el-table-column prop="qualityText" label="质量" width="80" align="center" />
      </el-table>
      <div v-else class="mde-skeleton"></div>
      <div v-if="!loading && tableData.length === 0" class="mde-empty">
        <span>暂无数据，请选择条件后点击查询</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import VueApexCharts from 'vue3-apexcharts'
import { useMonitorData, type BoundDeviceItem } from '@/composables/useMonitorData'
import type { ChartData, MonitorDataPageItem } from '@/api/monitorData'

// ── Props ──
const props = withDefaults(defineProps<{
  hazardPointId: number
  hazardPointName?: string
  showDevice?: boolean
  showSensor?: boolean
  showAttr?: boolean
  showValueType?: boolean
  showImportExport?: boolean
  enableCompleteness?: boolean
  enableTrend?: boolean
  initialDeviceId?: number
  initialMode?: 'chart' | 'table'
}>(), {
  showDevice: true,
  showSensor: true,
  showAttr: true,
  showValueType: true,
  showImportExport: false,
  enableCompleteness: false,
  enableTrend: false,
  initialMode: 'chart',
})

// ── Emits ──
const emit = defineEmits<{
  (e: 'data-loaded', data: { series: ChartData[]; list: MonitorDataPageItem[] }): void
  (e: 'device-change', deviceId: number): void
  (e: 'sensor-change', sensorId: number): void
}>()

// ── Composable ──
const {
  devices,
  sensors,
  attrs,
  chartSeries,
  tableData,
  loading,
  mode,
  filter,
  selectDevice,
  selectSensor,
  query,
  reset,
  buildChartOptions,
} = useMonitorData({
  hazardPointId: computed(() => props.hazardPointId),
})

// ── 图表配置 ──
const chartOptions = computed(() => buildChartOptions(chartSeries.value))

// ── 数据点警告 ──
const totalDataPoints = computed(() =>
  chartSeries.value.reduce((sum, s) => sum + s.labels.length, 0)
)
const dataPointWarning = computed(() => totalDataPoints.value > 500)

// ── 设备变更 ──
const onDeviceChange = async (deviceId: string | number) => {
  await selectDevice(deviceId)
  if (deviceId) emit('device-change', Number(deviceId))
}

// ── 传感器变更 ──
const onSensorChange = (sensorId: string | number) => {
  selectSensor(sensorId)
  if (sensorId) emit('sensor-change', Number(sensorId))
}

// ── 导入/导出 ──
const onImport = () => ElMessage.info('导入功能开发中')
const onExport = () => ElMessage.info('导出功能开发中')

// ── 初始设备 ──
watch(() => props.initialDeviceId, (id) => {
  if (id != null) {
    filter.deviceId = id
    selectDevice(id)
  }
}, { immediate: true })

// ── 初始模式 ──
mode.value = props.initialMode

// ── 数据加载后事件 ──
watch([chartSeries, tableData], () => {
  emit('data-loaded', {
    series: chartSeries.value,
    list: tableData.value,
  })
})
</script>

<style scoped>
.monitor-data-explorer {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.mde-filters {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
  padding: 12px 14px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
}

.mde-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.mde-warning {
  padding: 6px 12px;
  background: #fef3c7;
  color: #92400e;
  border-radius: 6px;
  font-size: 12px;
}

.mde-chart-area,
.mde-table-area {
  min-height: 400px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  overflow: hidden;
  position: relative;
}

.mde-skeleton {
  height: 400px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

.mde-empty {
  height: 400px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
  font-size: 14px;
}
</style>
```

- [ ] **步骤 2：TypeCheck 验证**

```bash
cd web && npx vue-tsc --noEmit 2>&1
```

预期：PASS

- [ ] **步骤 3：Commit**

```bash
git add web/src/components/MonitorDataExplorer.vue
git commit -m "feat: add MonitorDataExplorer shared component"
```

---

### 任务 4：全局 Element Plus 中文 locale

**文件：**
- 修改：`web/src/App.vue`

- [ ] **步骤 1：包裹 el-config-provider**

将 App.vue 的模板根元素包裹在 `<el-config-provider>` 中：

```vue
<template>
  <el-config-provider :locale="zhCn">
    <router-view />
  </el-config-provider>
</template>

<script setup lang="ts">
import zhCn from 'element-plus/es/locale/lang/zh-cn'
</script>
```

（注：如果 App.vue 已有 `<script setup>`，只需在现有 script 中追加 `import zhCn`，并将 `<router-view />` 包裹起来）

- [ ] **步骤 2：TypeCheck 验证**

```bash
cd web && npx vue-tsc --noEmit 2>&1
```

预期：PASS

- [ ] **步骤 3：Commit**

```bash
git add web/src/App.vue
git commit -m "feat: add Element Plus Chinese locale globally"
```

---

### 任务 5：迁移 HazardPointDetail 监测 tab

**文件：**
- 修改：`web/src/views/basic/components/HazardPointDetail.vue`
- 修改：`web/src/views/basic/HazardPoint.vue`

- [ ] **步骤 1：HazardPointDetail 删除本地监测状态和模板**

在 `HazardPointDetail.vue` 中：

**删除模板**（第 140-211 行，`<el-tab-pane label="监测数据" name="monitorData">` 整个 tab-pane）：

移除：
```html
<el-tab-pane label="监测数据" name="monitorData">
  <div class="monitor-data-panel">
    <!-- 所有 data-filters / data-toolbar / data-content 内容 -->
  </div>
</el-tab-pane>
```

替换为：
```html
<el-tab-pane label="监测数据" name="monitorData">
  <MonitorDataExplorer
    v-if="currentRow"
    :hazard-point-id="Number(currentRow.id)"
    :hazard-point-name="currentRow.name"
    :initial-device-id="boundDevices[0]?.deviceId"
    @device-change="(id) => emit('deviceChange', String(id))"
    @sensor-change="(id) => emit('sensorChange', String(id))"
  />
</el-tab-pane>
```

**删除 script 中的本地 ref**（第 251-263 行）：
移除 `dataDisplayMode`, `monitorDataList`, `chartSeriesData`, `chartOptions`, `monitorSensors`, `monitorAttrs`, `localDataFilter` 这 7 个 ref。

**删除方法**（第 290-349 行）：
移除 `handleResetData`, `onDataDeviceChange`, `onDataSensorChange`, `handleQueryData`, `handleImportData`, `handleExportData`, `updateMonitorData`, `updateSensors`, `updateAttrs` 这些方法。

**修改 defineExpose**（第 344-349 行）：
`defineExpose` 删除 `updateMonitorData`, `updateSensors`, `updateAttrs`, `localDataFilter`, `dataDisplayMode`，只保留可能被其他功能引用的内容。

**追加 import**（在 `<script setup>` 顶部）：
```ts
import MonitorDataExplorer from '@/components/MonitorDataExplorer.vue'
```

- [ ] **步骤 2：HazardPoint.vue 清理桥接代码**

**删除 composable 引入**（第 964-982 行）：
移除 `useHazardPointMonitor` 的 destructure 调用，包括：
```ts
const {
  dataDisplayMode,
  monitorDataList,
  chartSeriesData,
  chartOptions,
  latestDataList,
  monitorSensors,
  monitorAttrs,
  dataFilter,
  initLatestData,
  onDataDeviceChange,
  onDataSensorChange,
  handleQueryData,
  handleImportData,
  handleExportData,
} = useHazardPointMonitor({...})
```

**删除事件处理桥接**（第 819-850 行）：
移除 `handleDetailQueryData`, `handleDetailImportData`, `handleDetailExportData`, `handleDetailDeviceChange`, `handleDetailSensorChange` 这 5 个函数。

**更新 HazardPointDetail 的 template 绑定**（第 317-329 行）：
移除 `@query-data`, `@import-data`, `@export-data`, `@device-change`, `@sensor-change` 这些事件绑定。

**删除 monitor 相关 import**：
```ts
// 移除：
import {useHazardPointMonitor} from './composables/useHazardPointMonitor'
```

- [ ] **步骤 3：TypeCheck 验证**

```bash
cd web && npx vue-tsc --noEmit 2>&1
```

预期：PASS，确认无未引用变量

- [ ] **步骤 4：Commit**

```bash
git add web/src/views/basic/components/HazardPointDetail.vue web/src/views/basic/HazardPoint.vue
git commit -m "refactor: migrate HazardPoint monitor tab to MonitorDataExplorer"
```

---

### 任务 6：迁移 DeviceDataModal 和 DeviceDataPanel

**文件：**
- 修改：`web/src/views/dashboard/components/DeviceDataModal.vue`
- 修改：`web/src/views/dashboard/components/DeviceDataPanel.vue`

- [ ] **步骤 1：迁移 DeviceDataModal**

DeviceDataModal 当前是传感器侧边栏 + 图表面板的布局。替换为 `<MonitorDataExplorer>`：

```vue
<MonitorDataExplorer
  v-if="device"
  :hazard-point-id="hazardPointId"
  :hazard-point-name="hazardPointName"
  :show-device="false"
  :initial-device-id="device.id"
/>
```

保留 modal 的外壳（el-dialog 容器），替换 body 内容。删除：
- `modalSensorList`, `selectedModalSensor`, `chartContainer` 等本地 ref
- `selectModalSensor`, `querySensorData` 等方法
- `getDeviceSensors` / `getChartData` import（不再直接使用）
- ECharts 相关代码（`chartInstance` 等）

- [ ] **步骤 2：迁移 DeviceDataPanel**

同理，替换为：
```vue
<MonitorDataExplorer
  :hazard-point-id="hazardPointId"
  :hazard-point-name="hazardPointName"
  :show-device="false"
  :initial-device-id="device?.id"
/>
```

删除本地传感器/图表相关代码。

- [ ] **步骤 3：TypeCheck 验证**

```bash
cd web && npx vue-tsc --noEmit 2>&1
```

预期：PASS

- [ ] **步骤 4：Commit**

```bash
git add web/src/views/dashboard/components/DeviceDataModal.vue web/src/views/dashboard/components/DeviceDataPanel.vue
git commit -m "refactor: migrate dashboard device data panels to MonitorDataExplorer"
```

---

### 任务 7：迁移 miniprogram HazardPoint

**文件：**
- 修改：`web/src/views/miniprogram/HazardPoint.vue`

- [ ] **步骤 1：替换监测区域**

将监测 tab 内的手动筛选栏 + 图表 + 表格替换为：

```vue
<MonitorDataExplorer
  v-if="currentRow"
  :hazard-point-id="Number(currentRow.id)"
  :hazard-point-name="currentRow.name"
/>
```

删除：
- `monitorFilter`, `monitorTimeRange`, `monitorSensors`, `monitorAttrs` 等本地状态
- `onDeviceChange`, `onSensorChange`, `handleQueryMonitor` 等方法
- `getDeviceSensors`, `getChartData`, `getMonitorDataPage` 等直接 import

- [ ] **步骤 2：TypeCheck 验证**

```bash
cd web && npx vue-tsc --noEmit 2>&1
```

预期：PASS

- [ ] **步骤 3：Commit**

```bash
git add web/src/views/miniprogram/HazardPoint.vue
git commit -m "refactor: migrate miniprogram hazard point monitor to MonitorDataExplorer"
```

---

### 任务 8：删除 useHazardPointMonitor

**文件：**
- 删除：`web/src/views/basic/composables/useHazardPointMonitor.ts`

- [ ] **步骤 1：确认无残留引用**

```bash
cd web && grep -r "useHazardPointMonitor" src/ --include="*.ts" --include="*.vue"
```

预期：无输出（确认已全部迁移）

- [ ] **步骤 2：删除文件并 TypeCheck**

```bash
rm web/src/views/basic/composables/useHazardPointMonitor.ts
cd web && npx vue-tsc --noEmit 2>&1
```

预期：PASS

- [ ] **步骤 3：Commit**

```bash
git rm web/src/views/basic/composables/useHazardPointMonitor.ts
git commit -m "refactor: remove replaced useHazardPointMonitor composable"
```

---

### 任务 9：全量验证

- [ ] **步骤 1：TypeCheck**

```bash
cd web && npx vue-tsc --noEmit 2>&1
```

预期：PASS，零错误

- [ ] **步骤 2：检查 diff 统计**

```bash
git diff --stat develop
```

确认：新增约 2 个文件（composable + 组件），修改约 7 个文件，删除 1 个文件。净代码行数减少（删除了 6 份重复实现）。

- [ ] **步骤 3：手动冒烟测试清单**

| 页面 | 测试点 |
|------|--------|
| 隐患点管理 → 查看 → 监测数据 tab | 选择设备 → 传感器下拉更新 → 选择传感器 → 指标下拉更新 → 点查询 → 图表展示数据 |
| 同上 | 切换到表格模式 → 表格展示数据 |
| 同上 | 切换值类型 → 查询 → 数据变化 |
| 同上 | 修改时间范围 → 查询 |
| Dashboard → 设备数据弹窗 | 点击设备 → 弹窗中监测数据区域正常加载 |
| Dashboard → 设备数据面板 | 面板中监测数据正常展示 |
| 小程序侧 → 隐患点详情 → 监测数据 | 查询功能正常 |
| 全局 | 所有 el-date-picker 显示中文（年月日时秒均为中文） |

- [ ] **步骤 4：Commit（如有最后修正）**

```bash
git add -A
git commit -m "chore: final verification fixes"
```
