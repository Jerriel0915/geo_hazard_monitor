<!-- components/AnalysisCorrelation.vue -->
<template>
  <div class="correlation-mode">
    <div class="mode-header">
      <el-button text @click="emit('back')">&larr; 返回</el-button>
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
</template>

<script setup lang="ts">
import {computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch} from 'vue'
import {ElMessage} from 'element-plus'
import {showRequestErrorMessage} from '@/utils/errorHandler'
import echarts from '@/utils/echarts'
import {
  type ChartDataItem,
  type DeviceOption,
  type DeviceTypeOption,
  getChartData,
  getDeviceOptions,
  getDeviceTypeOptions,
  getHazardPointOptions,
  type HazardPointOption,
  type SensorSeriesItem,
} from '@/api/report'

const emit = defineEmits<{
  (e: 'back'): void
}>()

// Props
const COLORS = ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272', '#fc8452', '#9a60b4']

// State
const hazardPointOptions = ref<HazardPointOption[]>([])
const deviceTypeOptions = ref<DeviceTypeOption[]>([])
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

// Load data
const loadOptions = async () => {
  const [hps, dts] = await Promise.all([getHazardPointOptions(), getDeviceTypeOptions()])
  hazardPointOptions.value = hps
  deviceTypeOptions.value = dts
}

// Dialog handlers
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
    hazardPointId: device.hazardPointId || 0,
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

// Chart generation
const generateCorrelationChart = async () => {
  if (!selectedSensors.value.length) return
  chartLoading.value = true
  try {
    const now = new Date()
    const sevenDaysAgo = new Date(now.getTime() - 7 * 24 * 3600 * 1000)
    const pad = (n: number) => String(n).padStart(2, '0')
    const toIsoStr = (d: Date) =>
        `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
    const startTime = correlationTimeRange.value?.[0]?.replace(' ', 'T') || toIsoStr(sevenDaysAgo)
    const endTime = correlationTimeRange.value?.[1]?.replace(' ', 'T') || toIsoStr(now)

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

    const container = correlationChartRef.value
    if (!container) {
      ElMessage.error('图表容器未就绪，请重试')
      return
    }
    if (correlationChartInstance.value) {
      correlationChartInstance.value.dispose()
      correlationChartInstance.value = null
    }
    if (container.clientHeight < 10) {
      container.style.height = '450px'
    }
    const chart = echarts.init(container)
    correlationChartInstance.value = chart

    const allTimes = new Set<string>()
    allSeriesData.forEach(({ chartData }) => chartData.times.forEach((t) => allTimes.add(t)))
    const sortedTimes = [...allTimes].sort()

    const series: any[] = []
    const yAxesArray: any[] = []
    const statisticsRows: { name: string; max: string; min: string; avg: string; std: string }[] = []

    allSeriesData.forEach(({ sensor, chartData }, idx) => {
      yAxesArray.push({
        type: 'value',
        name: `${sensor.attrName}(${sensor.unit})`,
        position: idx % 2 === 0 ? 'left' : 'right',
        offset: Math.floor(idx / 2) * 60,
        axisLabel: { fontSize: 11 },
        nameTextStyle: { fontSize: 11 },
      })

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

// Resize handler
let resizeHandler: (() => void) | null = null

onMounted(() => {
  loadOptions()
  resizeHandler = () => {
    correlationChartInstance.value?.resize()
  }
  window.addEventListener('resize', resizeHandler)
})

onBeforeUnmount(() => {
  correlationChartInstance.value?.dispose()
  if (resizeHandler) {
    window.removeEventListener('resize', resizeHandler)
  }
})
</script>

<style scoped>
.correlation-mode {
  height: 100%;
}
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
</style>