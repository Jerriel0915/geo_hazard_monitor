<template>
  <div class="device-data-panel" :style="{ left: leftOffset + 'px', right: rightOffset + 'px' }">
    <div class="panel-inner">
      <!-- 标题栏 -->
      <div class="panel-header">
        <div class="panel-title">
          <el-icon class="device-icon" :size="18"><DataAnalysis/></el-icon>
          <span class="title-name">{{ device?.name || '设备' }}</span>
          <span class="title-code">{{ device?.code || '' }}</span>
        </div>
        <div class="header-actions">
          <button class="close-btn" @click="$emit('close')">
            <el-icon :size="14"><Close/></el-icon>
          </button>
        </div>
      </div>

      <div class="panel-body">
        <!-- 查询条件栏 -->
        <div class="query-bar">
          <div class="condition-group">
            <label class="condition-label">传感器</label>
            <select v-model="selectedSensorId" class="condition-select condition-select--sensor" @change="onSensorChange">
              <option value="">请选择</option>
              <option v-for="s in sensorList" :key="s.id" :value="s.id">{{ s.name }}</option>
            </select>
          </div>
          <div class="condition-group">
            <label class="condition-label">时间</label>
            <select v-model="queryTimeRange" class="condition-select">
              <option value="1">1天</option>
              <option value="7">1周</option>
              <option value="30">1月</option>
            </select>
          </div>
          <div class="condition-group">
            <label class="condition-label">值类型</label>
            <select v-model="queryValueType" class="condition-select">
              <option value="raw">采集值</option>
              <option value="hourly">小时变化</option>
              <option value="daily">24h变化</option>
              <option value="seventyTwo">72h变化</option>
            </select>
          </div>
          <div class="condition-group">
            <label class="condition-label">方向</label>
            <select v-model="queryDirection" class="condition-select">
              <option value="all">全部</option>
              <option value="x">X</option>
              <option value="y">Y</option>
              <option value="z">Z</option>
              <option value="h">水平</option>
              <option value="v">垂直</option>
            </select>
          </div>
          <button class="query-btn" @click="querySensorData">查询</button>
          <div class="toolbar-right">
            <div class="view-toggle">
              <button class="toggle-btn" :class="{ active: dataViewMode === 'chart' }" @click="dataViewMode = 'chart'">图表</button>
              <button class="toggle-btn" :class="{ active: dataViewMode === 'table' }" @click="dataViewMode = 'table'">表格</button>
            </div>
            <button class="export-btn" @click="handleExport">
              <el-icon :size="14"><Upload/></el-icon>
              导出
            </button>
          </div>
        </div>

        <!-- 图表 -->
        <div v-if="selectedSensor" class="data-area">
          <div v-show="dataViewMode === 'chart'" class="chart-area">
            <div ref="chartContainer" class="chart-container"></div>
          </div>
          <div v-show="dataViewMode === 'table'" class="table-area">
            <table class="data-table">
              <thead>
                <tr>
                  <th>时间</th>
                  <th v-for="s in chartDataResult" :key="s.seriesName">{{ s.seriesName }}({{ s.unit || '' }})</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(row, i) in tableData" :key="i">
                  <td>{{ row.time }}</td>
                  <td v-for="s in chartDataResult" :key="s.seriesName">{{ row[s.seriesName]?.toFixed(3) ?? '--' }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <div v-else class="empty-state">
          <el-icon :size="36" color="#c9cdd4"><DataAnalysis/></el-icon>
          <span class="empty-text">选择传感器查看数据</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onUnmounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import { Close, DataAnalysis, Upload } from '@element-plus/icons-vue'
import { getDeviceSensors } from '@/api/sensor'
import { type ChartData, getChartData } from '@/api/monitorData'

const props = defineProps<{
  device: any
  hazardPointId?: number
  leftOffset: number
  rightOffset: number
}>()

const emit = defineEmits<{
  (e: 'close'): void
}>()

// State
const sensorList = ref<any[]>([])
const selectedSensorId = ref<number | string>('')
const selectedSensor = ref<any | null>(null)
const chartContainer = ref<HTMLDivElement | null>(null)
let chartInstance: echarts.ECharts | null = null

const queryTimeRange = ref('7')
const queryValueType = ref('raw')
const queryDirection = ref('all')
const dataViewMode = ref<'chart' | 'table'>('chart')
const chartDataResult = ref<ChartData[]>([])
const tableData = ref<any[]>([])

// Load sensors
const loadSensors = async () => {
  if (!props.device?.id) return
  try {
    const sensors = await getDeviceSensors(props.device.id)
    sensorList.value = sensors.map((sensor: any) => ({
      id: sensor.id,
      name: sensor.sensorName,
      code: sensor.sensorCode,
      type: sensor.monitorTypeCode || 'UNKNOWN'
    }))
  } catch {
    sensorList.value = []
  }
  selectedSensorId.value = ''
  selectedSensor.value = null
  chartDataResult.value = []
  tableData.value = []
}

watch(() => props.device?.id, (newId) => {
  if (newId) loadSensors()
}, { immediate: true })

const onSensorChange = () => {
  const id = Number(selectedSensorId.value)
  if (!id) {
    selectedSensor.value = null
    chartDataResult.value = []
    tableData.value = []
    return
  }
  selectedSensor.value = sensorList.value.find(s => s.id === id) || null
  if (selectedSensor.value) querySensorData()
}

const querySensorData = () => {
  if (!selectedSensor.value || !props.hazardPointId) return

  const now = new Date()
  const days = parseInt(queryTimeRange.value)
  const startTime = new Date(now.getTime() - days * 24 * 3600 * 1000).toISOString().slice(0, 19)
  const endTime = now.toISOString().slice(0, 19)

  getChartData({
    hazardPointId: props.hazardPointId,
    deviceId: props.device?.id,
    sensorId: selectedSensor.value.id,
    attrCode: queryDirection.value !== 'all' ? queryDirection.value : undefined,
    valueType: queryValueType.value,
    startTime,
    endTime
  }).then(data => {
    chartDataResult.value = data || []
    if (data && data.length > 0) {
      const labels = data[0].labels || []
      tableData.value = labels.map((label: string, i: number) => ({
        time: label,
        ...Object.fromEntries(data.map(s => [s.seriesName, s.values?.[i] ?? null]))
      }))
    } else {
      tableData.value = []
    }
    nextTick(() => renderChart())
  }).catch(() => {
    tableData.value = []
    chartDataResult.value = []
  })
}

const SERIES_COLORS = ['#1677ff', '#52c41a', '#faad14', '#f5222d', '#722ed1', '#eb2f96']

const renderChart = () => {
  if (!chartContainer.value) return
  const dataList = chartDataResult.value
  if (dataList.length === 0) return

  if (chartInstance) chartInstance.dispose()
  chartInstance = echarts.init(chartContainer.value)

  const labels = dataList[0].labels || []
  const legendData = dataList.map(s => s.seriesName)
  const yUnit = dataList[0].unit || ''

  chartInstance.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'cross' } },
    legend: { data: legendData, textStyle: { fontSize: 12 } },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '12%', containLabel: true },
    xAxis: { type: 'category', boundaryGap: false, data: labels, axisLabel: { fontSize: 11, rotate: 30 } },
    yAxis: { type: 'value', name: yUnit, axisLabel: { fontSize: 11 } },
    series: dataList.map((s, index) => ({
      name: s.seriesName,
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 3,
      data: s.values || [],
      lineStyle: { color: SERIES_COLORS[index % SERIES_COLORS.length], width: 2 },
      itemStyle: { color: SERIES_COLORS[index % SERIES_COLORS.length] }
    }))
  })
}

const handleExport = () => {
  const dataList = chartDataResult.value
  if (dataList.length === 0 || tableData.value.length === 0) return

  const headers = ['时间', ...dataList.map(s => `${s.seriesName}(${s.unit || ''})`)]
  const labels = dataList[0].labels || []
  const rows = labels.map((label: string, i: number) => [
    label, ...dataList.map(s => s.values?.[i]?.toFixed(3) ?? '')
  ])

  const csvContent = [headers, ...rows].map(row => row.join(',')).join('\n')
  const blob = new Blob(['\ufeff' + csvContent], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `传感器数据_${selectedSensor.value?.name || 'export'}_${new Date().toLocaleDateString()}.csv`
  link.click()
  URL.revokeObjectURL(link.href)
}

onUnmounted(() => {
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
})
</script>

<style scoped>
.device-data-panel {
  position: absolute;
  bottom: 12px;
  z-index: 900;
}

.panel-inner {
  width: 100%;
  background: #ffffff;
  border-radius: 10px;
  box-shadow: 0 -2px 16px rgba(0, 0, 0, 0.1);
  border: 1px solid #e5e6eb;
  display: flex;
  flex-direction: column;
  height: 340px;
  overflow: hidden;
}

/* 标题栏 */
.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 16px;
  background: #f7f8fa;
  border-bottom: 1px solid #e5e6eb;
  flex-shrink: 0;
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.device-icon { color: #1677ff; }

.title-name {
  font-size: 14px;
  font-weight: 600;
  color: #1d2129;
}

.title-code {
  font-size: 12px;
  color: #6b7785;
  font-family: 'SFMono-Regular', Consolas, monospace;
  background: #f0f1f3;
  padding: 1px 8px;
  border-radius: 4px;
}

.close-btn {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: 1px solid #e5e6eb;
  border-radius: 6px;
  color: #6b7785;
  cursor: pointer;
  transition: all 0.2s;
}

.close-btn:hover {
  background: #f0f1f3;
  color: #1d2129;
  border-color: #c9cdd4;
}

/* 主体 */
.panel-body {
  display: flex;
  flex-direction: column;
  flex: 1;
  overflow: hidden;
}

/* 查询栏 */
.query-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 16px;
  border-bottom: 1px solid #e5e6eb;
  flex-shrink: 0;
}

.condition-group {
  display: flex;
  align-items: center;
  gap: 4px;
}

.condition-label {
  font-size: 12px;
  color: #6b7785;
  white-space: nowrap;
}

.condition-select {
  padding: 4px 8px;
  border: 1px solid #e5e6eb;
  border-radius: 4px;
  font-size: 12px;
  color: #1d2129;
  background: #fff;
  cursor: pointer;
  outline: none;
}

.condition-select:focus {
  border-color: #1677ff;
}

.condition-select--sensor {
  min-width: 140px;
}

.query-btn {
  padding: 4px 14px;
  background: #1677ff;
  color: #fff;
  border: none;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s;
}

.query-btn:hover { background: #4096ff; }

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;
}

.view-toggle {
  display: flex;
  gap: 0;
  background: #f0f1f3;
  padding: 2px;
  border-radius: 6px;
}

.toggle-btn {
  padding: 3px 12px;
  background: transparent;
  border: none;
  border-radius: 4px;
  font-size: 12px;
  color: #6b7785;
  cursor: pointer;
  transition: all 0.15s;
}

.toggle-btn.active {
  background: #fff;
  color: #1d2129;
  font-weight: 500;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.08);
}

.export-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  background: #fff;
  border: 1px solid #e5e6eb;
  border-radius: 4px;
  font-size: 12px;
  color: #4e5969;
  cursor: pointer;
  transition: all 0.15s;
}

.export-btn:hover {
  border-color: #1677ff;
  color: #1677ff;
}

/* 数据区 */
.data-area {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chart-area {
  flex: 1;
  min-height: 0;
  padding: 8px 12px;
}

.chart-container {
  width: 100%;
  height: 100%;
  min-height: 180px;
}

.table-area {
  flex: 1;
  overflow: auto;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
}

.data-table thead {
  background: #f7f8fa;
  position: sticky;
  top: 0;
  z-index: 1;
}

.data-table th {
  padding: 8px 12px;
  text-align: left;
  font-weight: 600;
  color: #1d2129;
  border-bottom: 1px solid #e5e6eb;
  white-space: nowrap;
}

.data-table td {
  padding: 6px 12px;
  border-bottom: 1px solid #f0f1f3;
  color: #4e5969;
}

.data-table tbody tr:hover {
  background: #f7f8fa;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex: 1;
  gap: 8px;
}

.empty-text {
  font-size: 13px;
  color: #86909c;
}
</style>
