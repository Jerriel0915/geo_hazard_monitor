<template>
  <div class="device-data-modal" @click="$emit('close')">
    <div class="modal-container" @click.stop>
      <div class="modal-header">
        <div class="modal-title">
          <el-icon class="device-icon">
            <DataAnalysis/>
          </el-icon>
          <!-- 三级面包屑：隐患点 → 设备 → 传感器（当前层级加粗） -->
          <div class="modal-breadcrumb">
              <span class="crumb crumb-clickable" @click="$emit('backToSystemView')" title="返回系统总览">
                {{ hazardPointName }}
              </span>
            <span class="crumb-sep">/</span>
            <span class="crumb crumb-clickable" @click="$emit('close')" title="返回设备列表">
                {{ device?.name || '设备' }}
              </span>
            <span v-if="selectedModalSensor" class="crumb-sep">/</span>
            <span v-if="selectedModalSensor" class="crumb crumb-current">{{ selectedModalSensor.name }}</span>
          </div>
        </div>
        <button class="modal-close-btn" @click="$emit('close')">
          <el-icon :size="16"><Close/></el-icon>
        </button>
      </div>

      <div class="modal-body">
        <!-- 左侧：传感器清单 -->
        <div class="sensor-list-sidebar">
          <div class="sidebar-header">
              <span class="sidebar-title">传感器清单</span>
              <span class="sensor-count">{{ modalSensorList.length }}个传感器</span>
          </div>
          <div class="sidebar-content">
            <div
                v-for="sensor in modalSensorList"
                :key="sensor.id"
                class="sensor-item"
                :class="{ selected: selectedModalSensor?.id === sensor.id, warning: sensor.status === 'warning' }"
                @click="selectModalSensor(sensor)"
            >
              <div class="sensor-icon-wrapper">
                <el-icon :size="24">
                  <component :is="getDeviceTypeIcon(sensor.type)"/>
                </el-icon>
              </div>
              <div class="sensor-details">
                <div class="sensor-name">{{ sensor.name }}</div>
                <div class="sensor-code">{{ sensor.code }}</div>
              </div>
              <div class="sensor-status-indicator" :class="sensor.status">
                <span class="status-dot"></span>
              </div>
            </div>
          </div>
        </div>

        <!-- 右侧：数据曲线面板 -->
        <div class="data-chart-panel">
          <div v-if="selectedModalSensor" class="chart-content">
            <!-- 查询条件栏 -->
            <div class="query-conditions">
              <div class="condition-group">
                <label class="condition-label">时间范围:</label>
                <select v-model="queryTimeRange" class="condition-select">
                  <option value="1">最近1天</option>
                  <option value="7">最近1周</option>
                  <option value="30">最近1月</option>
                </select>
              </div>

              <div class="condition-group">
                <label class="condition-label">值类型:</label>
                <select v-model="queryValueType" class="condition-select">
                  <option value="raw">采集值</option>
                  <option value="hourly">小时变化</option>
                  <option value="daily">24小时变化</option>
                  <option value="seventyTwo">72小时变化</option>
                </select>
              </div>

              <div class="condition-group">
                <label class="condition-label">方向:</label>
                <select v-model="queryDirection" class="condition-select">
                  <option value="all">全部</option>
                  <option value="x">X方向</option>
                  <option value="y">Y方向</option>
                  <option value="z">Z方向</option>
                  <option value="h">水平位移</option>
                  <option value="v">垂直位移</option>
                </select>
              </div>

              <button class="query-btn" @click="querySensorData">查询</button>
            </div>

            <!-- 数据展示切换和操作按钮 -->
            <div class="data-toolbar">
              <div class="view-toggle">
                <button
                    class="toggle-btn"
                    :class="{ active: dataViewMode === 'chart' }"
                    @click="dataViewMode = 'chart'"
                >
                  图表
                </button>
                <button
                    class="toggle-btn"
                    :class="{ active: dataViewMode === 'table' }"
                    @click="dataViewMode = 'table'"
                >
                  表格
                </button>
              </div>

              <div class="data-actions">
                <button class="action-btn" @click="handleImport">
                  <el-icon class="btn-icon">
                    <Download/>
                  </el-icon>
                  导入
                </button>
                <button class="action-btn" @click="handleExport">
                  <el-icon class="btn-icon">
                    <Upload/>
                  </el-icon>
                  导出
                </button>
              </div>
            </div>

            <!-- 图表视图 -->
            <div v-show="dataViewMode === 'chart'" class="chart-view">
              <div ref="chartContainer" class="echarts-container"></div>
            </div>

            <!-- 表格视图 -->
            <div v-show="dataViewMode === 'table'" class="table-view">
              <table class="data-table">
                <thead>
                <tr>
                  <th>时间</th>
                  <th v-for="s in chartDataResult" :key="s.seriesName">{{ s.seriesName }}({{ s.unit || '' }})</th>
                </tr>
                </thead>
                <tbody>
                <tr v-for="(row, index) in tableData" :key="index">
                  <td>{{ row.time }}</td>
                  <td v-for="s in chartDataResult" :key="s.seriesName">{{
                      row[s.seriesName]?.toFixed(3) ?? '--'
                    }}
                  </td>
                </tr>
                </tbody>
              </table>
            </div>
          </div>

          <div v-else class="empty-state">
            <el-icon size="64">
              <DataAnalysis/>
            </el-icon>
            <div class="empty-text">请从左侧选择一个传感器查看数据</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {nextTick, onUnmounted, ref} from 'vue'
import * as echarts from 'echarts'
import {Close, DataAnalysis, Download, Drizzling, Monitor, Odometer, Sunny, Upload} from '@element-plus/icons-vue'
import {getDeviceSensors} from '@/api/sensor'
import {type ChartData, getChartData} from '@/api/monitorData'
import {showRequestErrorMessage} from '@/utils/errorHandler'

// ---------- Props & Emits ----------
const props = defineProps<{
  device: any
  hazardPointId?: number
  hazardPointName?: string
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'backToSystemView'): void
}>()

// ---------- Helpers ----------
const getDeviceTypeIcon = (type: string) => {
  switch (type) {
    case 'GNSS':
      return Monitor
    case 'RAIN':
      return Sunny
    case 'PRESSURE':
      return Drizzling
    default:
      return Odometer
  }
}

// ---------- Modal state ----------
const modalSensorList = ref<any[]>([])
const selectedModalSensor = ref<any | null>(null)
const chartContainer = ref<HTMLDivElement | null>(null)
let chartInstance: echarts.ECharts | null = null

// 查询条件
const queryTimeRange = ref('7')
const queryValueType = ref('raw')
const queryDirection = ref('all')

// 数据展示模式
const dataViewMode = ref<'chart' | 'table'>('chart')

// 图表数据
const chartDataResult = ref<ChartData[]>([])
const tableData = ref<any[]>([])

// ---------- Init: load sensors on mount ----------
const loadSensors = async () => {
  if (!props.device?.id) return
  try {
    const sensors = await getDeviceSensors(props.device.id)
    modalSensorList.value = sensors.map((sensor: any) => ({
      id: sensor.id,
      name: sensor.sensorName,
      code: sensor.sensorCode,
      type: sensor.monitorTypeCode || 'UNKNOWN',
      status: sensor.status === 0 ? 'online' : sensor.status === 1 ? 'warning' : 'offline'
    }))
  } catch (error) {
    showRequestErrorMessage(error, '加载传感器列表失败')
    modalSensorList.value = []
  }

  // 初始化查询条件
  queryTimeRange.value = '7'
  queryValueType.value = 'raw'
  queryDirection.value = 'all'
  selectedModalSensor.value = null
  dataViewMode.value = 'chart'
}

loadSensors()

// ---------- Actions ----------
const selectModalSensor = (sensor: any) => {
  if (selectedModalSensor.value?.id === sensor.id) {
    return
  }
  selectedModalSensor.value = sensor
  querySensorData()
}

const querySensorData = () => {
  if (!selectedModalSensor.value) return

  const hpId = props.hazardPointId
  if (!hpId) return

  const now = new Date()
  const days = parseInt(queryTimeRange.value)
  const startTime = new Date(now.getTime() - days * 24 * 3600 * 1000).toISOString().slice(0, 19)
  const endTime = now.toISOString().slice(0, 19)

  getChartData({
    hazardPointId: hpId,
    deviceId: props.device?.id,
    sensorId: selectedModalSensor.value.id,
    attrCode: queryDirection.value !== 'all' ? queryDirection.value : undefined,
    valueType: queryValueType.value,
    startTime,
    endTime
  }).then(data => {
    chartDataResult.value = data || []

    // 生成表格数据
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
  }).catch(err => {
    showRequestErrorMessage(err, '查询传感器数据失败')
    tableData.value = []
    chartDataResult.value = []
  })
}

// ---------- ECharts ----------
const SERIES_COLORS = ['#1890ff', '#52c41a', '#faad14', '#f5222d', '#722ed1', '#eb2f96']

const renderChart = () => {
  if (!chartContainer.value) return
  const dataList = chartDataResult.value
  if (dataList.length === 0) return

  // 销毁旧实例
  if (chartInstance) {
    chartInstance.dispose()
  }

  // 创建新实例
  chartInstance = echarts.init(chartContainer.value)

  const labels = dataList[0].labels || []
  const legendData = dataList.map(s => s.seriesName)
  const yUnit = dataList[0].unit || ''

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {type: 'cross'}
    },
    legend: {
      data: legendData,
      textStyle: {fontSize: 12}
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: labels,
      axisLabel: {fontSize: 10, rotate: 45}
    },
    yAxis: {
      type: 'value',
      name: yUnit,
      axisLabel: {fontSize: 10}
    },
    series: dataList.map((s, index) => ({
      name: s.seriesName,
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 4,
      data: s.values || [],
      lineStyle: {color: SERIES_COLORS[index % SERIES_COLORS.length], width: 2},
      itemStyle: {color: SERIES_COLORS[index % SERIES_COLORS.length]}
    }))
  }

  chartInstance.setOption(option)
}

// ---------- Import / Export ----------
const handleImport = () => {
  console.log('导入监测数据')
  alert('数据导入功能开发中...')
}

const handleExport = () => {
  console.log('导出监测数据')
  const dataList = chartDataResult.value
  if (dataList.length === 0 || tableData.value.length === 0) {
    alert('暂无数据可导出')
    return
  }

  // 生成CSV数据
  const headers = ['时间', ...dataList.map(s => `${s.seriesName}(${s.unit || ''})`)]
  const labels = dataList[0].labels || []
  const rows = labels.map((label: string, i: number) => [
    label,
    ...dataList.map(s => s.values?.[i]?.toFixed(3) ?? '')
  ])

  const csvContent = [headers, ...rows].map(row => row.join(',')).join('\n')
  const blob = new Blob(['\ufeff' + csvContent], {type: 'text/csv;charset=utf-8;'})
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `传感器数据_${selectedModalSensor.value?.name || 'export'}_${new Date().toLocaleDateString()}.csv`
  link.click()
  URL.revokeObjectURL(link.href)
}

// ---------- Cleanup ----------
const disposeChart = () => {
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
}

onUnmounted(() => {
  disposeChart()
})
</script>

<style scoped>
.device-data-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
}

.modal-container {
  background: white;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  width: 95%;
  max-width: 1200px;
  height: 85vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  border-bottom: 1px solid #f0f0f0;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  flex-shrink: 0;
}

.modal-title {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.device-icon {
  font-size: 24px;
}

.modal-breadcrumb {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 16px;
  font-weight: 500;
}

.crumb {
  padding: 2px 8px;
  border-radius: 4px;
  transition: background 0.15s;
}

.crumb-clickable {
  cursor: pointer;
  color: #909399;
}

.crumb-clickable:hover {
  background: rgba(64, 158, 255, 0.1);
  color: #1890ff;
}

.crumb-current {
  color: #303133;
  font-weight: 600;
}

.crumb-sep {
  color: #c0c4cc;
  font-weight: 400;
}

.modal-close-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.06);
  border: none;
  border-radius: 8px;
  color: #909399;
  cursor: pointer;
  font-size: 18px;
  transition: all 0.2s;
}

.modal-close-btn:hover {
  background: rgba(0, 0, 0, 0.12);
  color: #303133;
}

.modal-body {
  display: flex;
  flex: 1;
  overflow: hidden;
}

/* 传感器清单侧边栏 */
.sensor-list-sidebar {
  width: 280px;
  background: #f5f7fa;
  border-right: 1px solid #e8e8e8;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  border-bottom: 1px solid #e8e8e8;
}

.sidebar-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.sensor-count {
  font-size: 12px;
  color: #909399;
  background: #f5f7fa;
  padding: 2px 8px;
  border-radius: 10px;
}

.sidebar-content {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}

.sensor-list-sidebar .sensor-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  background: white;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: 8px;
  border: 1px solid transparent;
}

.sensor-list-sidebar .sensor-item:hover {
  background: #f0f7ff;
  border-color: #1890ff;
}

.sensor-list-sidebar .sensor-item.selected {
  background: #e6f7ff;
  border-color: #1890ff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.15);
}

.sensor-list-sidebar .sensor-item.warning {
  background: #fffbf0;
}

.sensor-list-sidebar .sensor-item.warning.selected {
  background: #fff1f0;
  border-color: #fa541c;
}

.sensor-icon-wrapper {
  font-size: 24px;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  border-radius: 8px;
}

.sensor-details {
  flex: 1;
}

.sensor-list-sidebar .sensor-name {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 2px;
}

.sensor-list-sidebar .sensor-code {
  font-size: 11px;
  color: #909399;
}

.sensor-status-indicator {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}

.sensor-status-indicator.online {
  background: #52c41a;
}

.sensor-status-indicator.warning {
  background: #faad14;
}

/* 数据曲线面板 */
.data-chart-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: white;
}

.chart-content {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 16px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #909399;
}

.empty-text {
  font-size: 14px;
}

/* 查询条件栏 */
.query-conditions {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 16px;
  background: #f5f7fa;
  border-radius: 8px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.condition-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.condition-label {
  font-size: 13px;
  color: #909399;
  white-space: nowrap;
}

.condition-select {
  padding: 8px 12px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  font-size: 13px;
  color: #303133;
  background: white;
  cursor: pointer;
  transition: all 0.2s;
  min-width: 120px;
}

.condition-select:hover {
  border-color: #1890ff;
}

.condition-select:focus {
  outline: none;
  border-color: #1890ff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.1);
}

.query-btn {
  padding: 8px 20px;
  background: linear-gradient(135deg, #1890ff 0%, #66b1ff 100%);
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  margin-left: auto;
}

.query-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
}

/* 数据工具栏 */
.data-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.view-toggle {
  display: flex;
  gap: 8px;
  background: #f5f7fa;
  padding: 4px;
  border-radius: 8px;
}

.toggle-btn {
  padding: 6px 16px;
  background: transparent;
  border: none;
  border-radius: 6px;
  font-size: 13px;
  color: #909399;
  cursor: pointer;
  transition: all 0.2s;
}

.toggle-btn.active {
  background: white;
  color: #303133;
  font-weight: 500;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.toggle-btn:hover:not(.active) {
  color: #303133;
}

.data-actions {
  display: flex;
  gap: 8px;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: white;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  font-size: 13px;
  color: #303133;
  cursor: pointer;
  transition: all 0.2s;
}

.action-btn:hover {
  background: #f5f7fa;
  border-color: #1890ff;
  color: #1890ff;
}

.action-btn .btn-icon {
  font-size: 14px;
}

/* 图表视图 */
.chart-view {
  flex: 1;
  min-height: 0;
  background: #f5f7fa;
  border-radius: 8px;
  padding: 16px;
}

.echarts-container {
  width: 100%;
  height: 100%;
  min-height: 400px;
}

/* 表格视图 */
.table-view {
  flex: 1;
  overflow: auto;
  background: #f5f7fa;
  border-radius: 8px;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.data-table thead {
  background: white;
  position: sticky;
  top: 0;
  z-index: 1;
}

.data-table th {
  padding: 12px;
  text-align: left;
  font-weight: 600;
  color: #303133;
  border-bottom: 2px solid #e8e8e8;
  white-space: nowrap;
}

.data-table td {
  padding: 12px;
  border-bottom: 1px solid #f5f7fa;
  color: #909399;
}

.data-table tbody tr:hover {
  background: #f5f7fa;
}
</style>
