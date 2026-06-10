<template>
  <div class="analysis-page">
    <!-- Mode Selection -->
    <div v-if="currentMode === ''" class="mode-selection">
      <h2 class="mode-title">数据分析</h2>
      <p class="mode-subtitle">请选择分析模式</p>
      <div class="mode-cards">
        <div class="mode-card" @click="currentMode = 'correlation'">
          <div class="mode-card-icon">📈</div>
          <div class="mode-card-title">关联分析</div>
          <div class="mode-card-desc">将多个传感器属性叠加在同一坐标系中，对比分析不同指标的变化趋势与关联关系</div>
        </div>
        <div class="mode-card" @click="currentMode = 'grid'">
          <div class="mode-card-icon">📊</div>
          <div class="mode-card-title">数据宫格</div>
          <div class="mode-card-desc">以九宫格模式同时查看多个传感器的独立数据图表，快速纵览整体监测态势</div>
        </div>
      </div>
    </div>

    <!-- Correlation Analysis Mode -->
    <div v-if="currentMode === 'correlation'" class="correlation-mode">
      <div class="mode-header">
        <el-button text @click="exitMode">&larr; 返回</el-button>
        <span class="mode-label">关联分析</span>
      </div>
      <div class="correlation-layout">
        <!-- Left Panel -->
        <div class="correlation-panel">
          <!-- Time Range -->
          <div class="panel-section">
            <div class="panel-title">时间范围</div>
            <el-date-picker
              v-model="correlationTimeRange"
              type="datetimerange"
              start-placeholder="开始"
              end-placeholder="结束"
              value-format="YYYY-MM-DD HH:mm:ss"
              style="width: 100%"
            />
          </div>
          <!-- Sensor List -->
          <div class="panel-section">
            <div class="panel-title">传感器列表</div>
            <el-button
              type="primary"
              size="small"
              @click="openAddSensorDialog"
              style="width: 100%; margin-bottom: 10px"
            >
              + 添加传感器
            </el-button>
            <div class="sensor-tags">
              <div v-for="(s, idx) in selectedSensors" :key="s.id" class="sensor-tag-item">
                <el-tag :color="s.color" effect="dark" closable @close="removeSensor(idx)" style="width: 100%">
                  {{ s.deviceName }} - {{ s.attrName }}
                </el-tag>
              </div>
            </div>
            <div v-if="!selectedSensors.length" class="empty-hint">请添加至少一个传感器</div>
          </div>
          <!-- Analysis Tools -->
          <div class="panel-section">
            <div class="panel-title">分析工具</div>
            <el-checkbox-group v-model="activeTools">
              <div class="tool-checkbox"><el-checkbox label="statistics">统计指标</el-checkbox></div>
              <div class="tool-checkbox"><el-checkbox label="trend">趋势分析</el-checkbox></div>
              <div class="tool-checkbox"><el-checkbox label="changeRate">变化率</el-checkbox></div>
            </el-checkbox-group>
          </div>
          <el-button
            type="primary"
            @click="generateCorrelationChart"
            :loading="chartLoading"
            :disabled="!selectedSensors.length"
            style="width: 100%"
          >
            生成图表
          </el-button>
        </div>

        <!-- Right Chart Area -->
        <div class="correlation-chart-area">
          <div class="chart-toolbar">
            <el-button size="small" @click="exportChartImage" :disabled="!correlationChartInstance">导出图片</el-button>
          </div>
          <div ref="correlationChartRef" class="chart-main" />
          <!-- Statistics Panel -->
          <div v-if="activeTools.includes('statistics') && statisticsData.length" class="statistics-panel">
            <div class="panel-title">统计指标</div>
            <el-table :data="statisticsData" border size="small">
              <el-table-column prop="name" label="系列" />
              <el-table-column prop="max" label="最大值" width="100" />
              <el-table-column prop="min" label="最小值" width="100" />
              <el-table-column prop="avg" label="平均值" width="100" />
              <el-table-column prop="std" label="标准差" width="100" />
            </el-table>
          </div>
        </div>
      </div>

      <!-- Add Sensor Dialog -->
      <el-dialog v-model="addSensorDialogVisible" title="添加传感器" width="500px" destroy-on-close>
        <el-form label-width="80px">
          <el-form-item label="隐患点">
            <el-select
              v-model="addSensorForm.hazardPointId"
              placeholder="选择隐患点"
              @change="onAddSensorHpChange"
              style="width: 100%"
            >
              <el-option v-for="hp in hazardPointOptions" :key="hp.id" :label="hp.name" :value="hp.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="设备">
            <el-select
              v-model="addSensorForm.deviceId"
              placeholder="选择设备"
              @change="onAddSensorDeviceChange"
              style="width: 100%"
            >
              <el-option v-for="d in filteredDevices" :key="d.id" :label="d.name" :value="d.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="属性">
            <el-select v-model="addSensorForm.attrCode" placeholder="选择属性" style="width: 100%">
              <el-option
                v-for="a in availableAttrs"
                :key="a.code"
                :label="a.name + '(' + a.unit + ')'"
                :value="a.code"
              />
            </el-select>
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="addSensorDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmAddSensor" :disabled="!addSensorForm.attrCode">确定</el-button>
        </template>
      </el-dialog>
    </div>

    <!-- Data Grid Mode -->
    <div v-if="currentMode === 'grid'" class="grid-mode">
      <div class="mode-header">
        <el-button text @click="exitMode">&larr; 返回</el-button>
        <span class="mode-label">数据宫格</span>
        <div class="grid-time-range">
          <span style="margin-right: 8px; color: #606266; font-size: 13px">统一时间范围：</span>
          <el-date-picker
            v-model="gridTimeRange"
            type="datetimerange"
            start-placeholder="开始"
            end-placeholder="结束"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 340px"
          />
          <el-button type="primary" size="small" @click="loadAllGridCharts" style="margin-left: 10px">应用</el-button>
        </div>
      </div>
      <div class="grid-container">
        <div v-for="(cell, idx) in gridCells" :key="idx" class="grid-cell">
          <template v-if="cell.sensorSeriesId">
            <div class="grid-cell-header">
              <span class="grid-cell-title">{{ cell.title }}</span>
              <el-dropdown trigger="click" @command="(cmd: string) => handleGridCommand(cmd, idx)">
                <span class="grid-cell-more">···</span>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="edit">修改</el-dropdown-item>
                    <el-dropdown-item command="clear">清除</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
            <div :ref="(el: any) => setGridChartRef(idx, el)" class="grid-chart" />
          </template>
          <template v-else>
            <div class="grid-cell-empty" @click="openGridConfig(idx)">
              <span class="grid-cell-add-icon">+</span>
              <span>添加图表</span>
            </div>
          </template>
        </div>
      </div>

      <!-- Grid Config Dialog -->
      <el-dialog v-model="gridConfigDialogVisible" title="配置图表" width="450px" destroy-on-close>
        <el-form label-width="80px">
          <el-form-item label="隐患点">
            <el-select
              v-model="gridConfigForm.hazardPointId"
              placeholder="选择隐患点"
              @change="onGridConfigHpChange"
              style="width: 100%"
            >
              <el-option v-for="hp in hazardPointOptions" :key="hp.id" :label="hp.name" :value="hp.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="设备">
            <el-select
              v-model="gridConfigForm.deviceId"
              placeholder="选择设备"
              @change="onGridConfigDeviceChange"
              style="width: 100%"
            >
              <el-option v-for="d in gridFilteredDevices" :key="d.id" :label="d.name" :value="d.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="属性">
            <el-select v-model="gridConfigForm.attrCode" placeholder="选择属性" style="width: 100%">
              <el-option
                v-for="a in gridAvailableAttrs"
                :key="a.code"
                :label="a.name + '(' + a.unit + ')'"
                :value="a.code"
              />
            </el-select>
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="gridConfigDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmGridConfig" :disabled="!gridConfigForm.attrCode">确定</el-button>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup lang="ts">
import {computed, nextTick, onBeforeUnmount, onMounted, reactive, ref} from 'vue'
import {ElMessage} from 'element-plus'
import {showRequestErrorMessage} from '@/utils/errorHandler'
import * as echarts from 'echarts'
import {
  type ChartDataItem,
  type DeviceOption,
  type DeviceTypeOption,
  getChartData,
  getDeviceOptions,
  getDeviceTypeOptions,
  getHazardPointOptions,
  type GridChartItem,
  type HazardPointOption,
  type SensorSeriesItem,
} from '@/api/report'

// ---------------------------------------------------------------------------
// Mode state
// ---------------------------------------------------------------------------

const currentMode = ref<string>('') // '' | 'correlation' | 'grid'
const exitMode = () => {
  currentMode.value = ''
}

// ---------------------------------------------------------------------------
// Shared option data
// ---------------------------------------------------------------------------

const hazardPointOptions = ref<HazardPointOption[]>([])
const deviceTypeOptions = ref<DeviceTypeOption[]>([])

// ---------------------------------------------------------------------------
// Correlation mode state
// ---------------------------------------------------------------------------

const COLORS = ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272', '#fc8452', '#9a60b4']
const selectedSensors = ref<SensorSeriesItem[]>([])
const correlationTimeRange = ref<[string, string] | null>(null)
const activeTools = ref<string[]>([])
const chartLoading = ref(false)
const correlationChartRef = ref<HTMLElement>()
const correlationChartInstance = ref<echarts.ECharts | null>(null)
const statisticsData = ref<{ name: string; max: string; min: string; avg: string; std: string }[]>([])

// Add sensor dialog
const addSensorDialogVisible = ref(false)
const addSensorForm = reactive({ hazardPointId: '' as number | '', deviceId: '' as number | '', attrCode: '' })
const dialogDevices = ref<DeviceOption[]>([])

const filteredDevices = computed(() => {
  return dialogDevices.value.filter(
    (d) => !addSensorForm.hazardPointId || d.hazardPointId === addSensorForm.hazardPointId
  )
})

const availableAttrs = computed(() => {
  const allAttrs: { code: string; name: string; unit: string }[] = []
  deviceTypeOptions.value.forEach((dt) => allAttrs.push(...dt.attrs))
  return allAttrs
})

// ---------------------------------------------------------------------------
// Correlation dialog handlers
// ---------------------------------------------------------------------------

const openAddSensorDialog = () => {
  addSensorForm.hazardPointId = ''
  addSensorForm.deviceId = ''
  addSensorForm.attrCode = ''
  addSensorDialogVisible.value = true
}

const onAddSensorHpChange = async () => {
  addSensorForm.deviceId = ''
  addSensorForm.attrCode = ''
  const devices = await getDeviceOptions({ hazardPointId: addSensorForm.hazardPointId || undefined })
  dialogDevices.value = devices
}

const onAddSensorDeviceChange = () => {
  addSensorForm.attrCode = ''
}

const confirmAddSensor = () => {
  const device = dialogDevices.value.find((d) => d.id === addSensorForm.deviceId)
  const dt = deviceTypeOptions.value.find((dt) => dt.attrs.some((a) => a.code === addSensorForm.attrCode))
  const attr = dt?.attrs.find((a) => a.code === addSensorForm.attrCode)
  if (!device || !attr) return

  const hp = hazardPointOptions.value.find((h) => h.id === device.hazardPointId)
  const id = `${device.id}_${addSensorForm.attrCode}_${Date.now()}`
  const colorIndex = selectedSensors.value.length % COLORS.length

  selectedSensors.value.push({
    id,
    hazardPointId: device.hazardPointId,
    hazardPointName: hp?.name || '',
    deviceId: device.id,
    deviceName: device.name,
    sensorId: device.id,
    sensorName: device.name,
    attrCode: addSensorForm.attrCode,
    attrName: attr.name,
    unit: attr.unit,
    color: COLORS[colorIndex],
  })
  addSensorDialogVisible.value = false
}

const removeSensor = (idx: number) => {
  selectedSensors.value.splice(idx, 1)
}

// ---------------------------------------------------------------------------
// Correlation chart generation
// ---------------------------------------------------------------------------

const generateCorrelationChart = async () => {
  if (!selectedSensors.value.length) return
  chartLoading.value = true
  try {
    // Build start/end time as ISO string with 'T' separator for safe Date parsing
    const now = new Date()
    const sevenDaysAgo = new Date(now.getTime() - 7 * 24 * 3600 * 1000)
    const pad = (n: number) => String(n).padStart(2, '0')
    const toIsoStr = (d: Date) =>
      `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
    const startTime = correlationTimeRange.value?.[0]?.replace(' ', 'T') || toIsoStr(sevenDaysAgo)
    const endTime = correlationTimeRange.value?.[1]?.replace(' ', 'T') || toIsoStr(now)

    // Fetch data for each sensor
    const allSeriesData: { sensor: SensorSeriesItem; chartData: ChartDataItem }[] = []
    for (const sensor of selectedSensors.value) {
      const data = await getChartData({
        deviceId: sensor.deviceId,
        attrCode: sensor.attrCode,
        startTime,
        endTime,
      })
      allSeriesData.push({ sensor, chartData: data })
    }

    // Ensure chart container exists and has dimensions
    const container = correlationChartRef.value
    if (!container) {
      ElMessage.error('图表容器未就绪，请重试')
      return
    }
    if (correlationChartInstance.value) {
      correlationChartInstance.value.dispose()
      correlationChartInstance.value = null
    }
    // Force container to have height if flex didn't resolve
    if (container.clientHeight < 10) {
      container.style.height = '450px'
    }
    const chart = echarts.init(container)
    correlationChartInstance.value = chart

    // Merge all time points
    const allTimes = new Set<string>()
    allSeriesData.forEach(({ chartData }) => chartData.times.forEach((t) => allTimes.add(t)))
    const sortedTimes = [...allTimes].sort()

    // Build series and y-axes — MUST be array for ECharts multi-axis
    const series: any[] = []
    const yAxesArray: any[] = []
    const statisticsRows: (typeof statisticsData.value)[number][] = []

    allSeriesData.forEach(({ sensor, chartData }, idx) => {
      yAxesArray.push({
        type: 'value',
        name: `${sensor.attrName}(${sensor.unit})`,
        position: idx % 2 === 0 ? 'left' : 'right',
        offset: Math.floor(idx / 2) * 60,
        axisLabel: { fontSize: 11 },
        nameTextStyle: { fontSize: 11 },
      })

      // Map data
      const dataMap = new Map(chartData.times.map((t, i) => [t, chartData.values[i]]))
      const seriesData = sortedTimes.map((t) => dataMap.get(t) ?? null)

      series.push({
        name: `${sensor.deviceName}-${sensor.attrName}`,
        type: 'line',
        yAxisIndex: idx,
        data: seriesData,
        itemStyle: { color: sensor.color },
        lineStyle: { width: 2 },
        symbolSize: 4,
        connectNulls: true,
      })

      // Trend line (if enabled)
      if (activeTools.value.includes('trend')) {
        const validData = seriesData.filter((v) => v !== null) as number[]
        if (validData.length >= 2) {
          const n = validData.length
          const xArr = validData.map((_, i) => i)
          const sumX = xArr.reduce((a, b) => a + b, 0)
          const sumY = validData.reduce((a, b) => a + b, 0)
          const sumXY = xArr.reduce((a, x, i) => a + x * validData[i], 0)
          const sumXX = xArr.reduce((a, x) => a + x * x, 0)
          const denom = n * sumXX - sumX * sumX
          const slope = denom !== 0 ? (n * sumXY - sumX * sumY) / denom : 0
          const intercept = (sumY - slope * sumX) / n
          const trendData = seriesData.map((v, i) =>
            v !== null ? Number((slope * i + intercept).toFixed(4)) : null
          )
          series.push({
            name: `${sensor.attrName}-趋势`,
            type: 'line',
            yAxisIndex: idx,
            data: trendData,
            lineStyle: { width: 1.5, type: 'dashed', color: sensor.color },
            itemStyle: { color: sensor.color, opacity: 0.5 },
            symbol: 'none',
            connectNulls: true,
          })
        }
      }

      // Statistics
      if (activeTools.value.includes('statistics')) {
        const valid = seriesData.filter((v) => v !== null) as number[]
        if (valid.length) {
          const max = Math.max(...valid)
          const min = Math.min(...valid)
          const avg = valid.reduce((a, b) => a + b, 0) / valid.length
          const variance = valid.reduce((a, v) => a + (v - avg) ** 2, 0) / valid.length
          const std = Math.sqrt(variance)
          statisticsRows.push({
            name: `${sensor.deviceName}-${sensor.attrName}`,
            max: max.toFixed(3),
            min: min.toFixed(3),
            avg: avg.toFixed(3),
            std: std.toFixed(3),
          })
        }
      }
    })

    statisticsData.value = statisticsRows

    // Change rate series
    if (activeTools.value.includes('changeRate')) {
      allSeriesData.forEach(({ sensor, chartData }, idx) => {
        const dataMap = new Map(chartData.times.map((t, i) => [t, chartData.values[i]]))
        const seriesData = sortedTimes.map((t) => dataMap.get(t) ?? null)
        const rates = seriesData.map((v, i) => {
          if (i === 0 || v === null || seriesData[i - 1] === null) return null
          const prev = seriesData[i - 1] as number
          return prev !== 0 ? Number(((v - prev) / prev) * 100).toFixed(2) : 0
        })
        const rateAxisIdx = yAxesArray.length
        yAxesArray.push({
          type: 'value',
          name: '变化率(%)',
          position: 'right',
          offset: (Math.floor(idx / 2) + allSeriesData.length) * 60,
          axisLabel: { fontSize: 11, formatter: '{value}%' },
          nameTextStyle: { fontSize: 11 },
        })
        series.push({
          name: `${sensor.attrName}-变化率`,
          type: 'line',
          yAxisIndex: rateAxisIdx,
          data: rates,
          lineStyle: { width: 1, type: 'dotted', color: sensor.color },
          itemStyle: { color: sensor.color, opacity: 0.6 },
          symbol: 'none',
          connectNulls: true,
        })
      })
    }

    const option = {
      tooltip: { trigger: 'axis' },
      legend: { type: 'scroll', bottom: 0 },
      grid: { left: 80, right: 80, top: 30, bottom: 60, containLabel: true },
      xAxis: { type: 'category', data: sortedTimes, axisLabel: { fontSize: 11, rotate: 30 } },
      yAxis: yAxesArray,
      series,
      dataZoom: [{ type: 'inside' }, { type: 'slider' }],
    }

    chart.setOption(option)
  } catch (error) {
    showRequestErrorMessage(error, '生成图表失败')
  } finally {
    chartLoading.value = false
  }
}

const exportChartImage = () => {
  if (!correlationChartInstance.value) return
  const url = correlationChartInstance.value.getDataURL({ type: 'png', pixelRatio: 2 })
  const a = document.createElement('a')
  a.href = url
  a.download = `关联分析_${new Date().toISOString().slice(0, 10)}.png`
  a.click()
}

// ---------------------------------------------------------------------------
// Grid mode state
// ---------------------------------------------------------------------------

const gridCells = ref<GridChartItem[]>(Array.from({ length: 9 }, (_, i) => ({ index: i })))
const gridTimeRange = ref<[string, string] | null>(null)
const gridChartRefs = new Map<number, HTMLElement>()
const gridChartInstances = new Map<number, echarts.ECharts>()

const gridConfigDialogVisible = ref(false)
const gridConfigTargetIdx = ref(0)
const gridConfigForm = reactive({ hazardPointId: '' as number | '', deviceId: '' as number | '', attrCode: '' })
const gridDialogDevices = ref<DeviceOption[]>([])

const gridFilteredDevices = computed(() => {
  return gridDialogDevices.value.filter(
    (d) => !gridConfigForm.hazardPointId || d.hazardPointId === gridConfigForm.hazardPointId
  )
})

const gridAvailableAttrs = computed(() => {
  const allAttrs: { code: string; name: string; unit: string }[] = []
  deviceTypeOptions.value.forEach((dt) => allAttrs.push(...dt.attrs))
  return allAttrs
})

// ---------------------------------------------------------------------------
// Grid handlers
// ---------------------------------------------------------------------------

const setGridChartRef = (idx: number, el: any) => {
  if (el) gridChartRefs.set(idx, el as HTMLElement)
}

const openGridConfig = (idx: number) => {
  gridConfigTargetIdx.value = idx
  gridConfigForm.hazardPointId = ''
  gridConfigForm.deviceId = ''
  gridConfigForm.attrCode = ''
  gridConfigDialogVisible.value = true
}

const onGridConfigHpChange = async () => {
  gridConfigForm.deviceId = ''
  gridConfigForm.attrCode = ''
  const devices = await getDeviceOptions({ hazardPointId: gridConfigForm.hazardPointId || undefined })
  gridDialogDevices.value = devices
}

const onGridConfigDeviceChange = () => {
  gridConfigForm.attrCode = ''
}

const confirmGridConfig = async () => {
  const device = gridDialogDevices.value.find((d) => d.id === gridConfigForm.deviceId)
  const dt = deviceTypeOptions.value.find((dt) => dt.attrs.some((a) => a.code === gridConfigForm.attrCode))
  const attr = dt?.attrs.find((a) => a.code === gridConfigForm.attrCode)
  if (!device || !attr) return

  const idx = gridConfigTargetIdx.value
  gridCells.value[idx] = {
    index: idx,
    sensorSeriesId: `${device.id}_${gridConfigForm.attrCode}_${Date.now()}`,
    title: `${device.name}-${attr.name}`,
    hazardPointId: device.hazardPointId,
    deviceId: device.id,
    attrCode: gridConfigForm.attrCode,
    attrName: attr.name,
    unit: attr.unit,
  }
  gridConfigDialogVisible.value = false

  // Wait for DOM update then load chart
  await nextTick()
  loadGridCellChart(idx)
}

const loadGridCellChart = async (idx: number) => {
  const cell = gridCells.value[idx]
  const el = gridChartRefs.get(idx)
  if (!cell.sensorSeriesId || !el || !cell.deviceId || !cell.attrCode) return

  const startTime =
    gridTimeRange.value?.[0] ||
    new Date(Date.now() - 7 * 24 * 3600 * 1000).toISOString().slice(0, 19).replace('T', ' ')
  const endTime =
    gridTimeRange.value?.[1] || new Date().toISOString().slice(0, 19).replace('T', ' ')

  try {
    const data = await getChartData({
      deviceId: cell.deviceId,
      attrCode: cell.attrCode,
      startTime,
      endTime,
    })

    // Dispose existing
    const existing = gridChartInstances.get(idx)
    if (existing) {
      existing.dispose()
      gridChartInstances.delete(idx)
    }

    const chart = echarts.init(el)
    gridChartInstances.set(idx, chart)

    chart.setOption({
      grid: { left: 50, right: 15, top: 15, bottom: 30 },
      xAxis: { type: 'category', data: data.times, axisLabel: { fontSize: 10, rotate: 30 } },
      yAxis: { type: 'value', name: cell.unit, axisLabel: { fontSize: 10 }, nameTextStyle: { fontSize: 10 } },
      series: [{ type: 'line', data: data.values, symbolSize: 2, lineStyle: { width: 1.5 } }],
      tooltip: { trigger: 'axis' },
      dataZoom: [{ type: 'inside' }],
    })
  } catch (error) {
    showRequestErrorMessage(error, `加载宫格图表 ${idx} 失败`)
  }
}

const loadAllGridCharts = async () => {
  for (let i = 0; i < 9; i++) {
    if (gridCells.value[i].sensorSeriesId) {
      await loadGridCellChart(i)
    }
  }
}

const handleGridCommand = (cmd: string, idx: number) => {
  if (cmd === 'edit') {
    openGridConfig(idx)
  } else if (cmd === 'clear') {
    const existing = gridChartInstances.get(idx)
    if (existing) {
      existing.dispose()
      gridChartInstances.delete(idx)
    }
    gridCells.value[idx] = { index: idx }
  }
}

// ---------------------------------------------------------------------------
// Lifecycle
// ---------------------------------------------------------------------------

onMounted(async () => {
  const [hps, dts] = await Promise.all([getHazardPointOptions(), getDeviceTypeOptions()])
  hazardPointOptions.value = hps
  deviceTypeOptions.value = dts
})

// Resize handler — registered and cleaned up properly
let resizeHandler: (() => void) | null = null

onMounted(() => {
  resizeHandler = () => {
    correlationChartInstance.value?.resize()
    gridChartInstances.forEach((chart) => chart.resize())
  }
  window.addEventListener('resize', resizeHandler)
})

onBeforeUnmount(() => {
  correlationChartInstance.value?.dispose()
  gridChartInstances.forEach((chart) => chart.dispose())
  if (resizeHandler) {
    window.removeEventListener('resize', resizeHandler)
  }
})
</script>

<style scoped>
/* Page shell */
.analysis-page {
  padding: 20px;
  background: #fff;
  border-radius: 8px;
  min-height: calc(100% - 40px);
}

/* Mode selection */
.mode-selection {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
}
.mode-title {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 8px;
}
.mode-subtitle {
  color: #909399;
  margin-bottom: 40px;
  font-size: 14px;
}
.mode-cards {
  display: flex;
  gap: 30px;
}
.mode-card {
  width: 300px;
  padding: 40px 30px;
  border: 2px solid #e8e8e8;
  border-radius: 12px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
}
.mode-card:hover {
  border-color: #409eff;
  box-shadow: 0 4px 20px rgba(64, 158, 255, 0.15);
  transform: translateY(-4px);
}
.mode-card-icon {
  font-size: 48px;
  margin-bottom: 16px;
}
.mode-card-title {
  font-size: 18px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 12px;
}
.mode-card-desc {
  font-size: 13px;
  color: #909399;
  line-height: 1.6;
}

/* Mode header (shared by correlation & grid) */
.mode-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e8e8e8;
}
.mode-label {
  font-size: 16px;
  font-weight: bold;
  color: #303133;
}

/* Correlation layout */
.correlation-layout {
  display: flex;
  gap: 16px;
  height: calc(100% - 60px);
}
.correlation-panel {
  width: 280px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.panel-section {
  margin-bottom: 12px;
}
.panel-title {
  font-size: 13px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 8px;
}
.correlation-chart-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.chart-main {
  flex: 1;
  min-height: 400px;
  height: 450px;
}
.chart-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 8px;
}
.sensor-tags {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.sensor-tag-item {
  width: 100%;
}
.empty-hint {
  color: #909399;
  font-size: 12px;
  text-align: center;
  padding: 10px 0;
}
.statistics-panel {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #e8e8e8;
}
.tool-checkbox {
  margin-bottom: 4px;
}

/* Grid mode */
.grid-mode {
  height: 100%;
}
.grid-container {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  padding: 16px;
}
.grid-cell {
  min-height: 220px;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
.grid-cell-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: #f5f7fa;
  border-bottom: 1px solid #e8e8e8;
}
.grid-cell-title {
  font-size: 12px;
  font-weight: bold;
  color: #303133;
}
.grid-cell-more {
  cursor: pointer;
  font-weight: bold;
  color: #909399;
}
.grid-cell-more:hover {
  color: #409eff;
}
.grid-chart {
  flex: 1;
  min-height: 0;
}
.grid-cell-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  cursor: pointer;
  color: #909399;
  transition: all 0.3s;
}
.grid-cell-empty:hover {
  color: #409eff;
  background: #f5f7fa;
}
.grid-cell-add-icon {
  font-size: 32px;
  margin-bottom: 8px;
}
.grid-time-range {
  display: flex;
  align-items: center;
  margin-left: auto;
}
</style>
