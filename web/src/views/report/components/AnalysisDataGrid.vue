<!-- components/AnalysisDataGrid.vue -->
<template>
  <div class="grid-mode">
    <div class="mode-header">
      <el-button text @click="emit('back')">&larr; 返回</el-button>
      <span class="mode-label">数据宫格</span>
      <el-button-group class="grid-layout-switcher">
        <el-button
          v-for="opt in layoutOptions"
          :key="opt.cols + 'x' + opt.rows"
          :type="gridLayout.cols === opt.cols && gridLayout.rows === opt.rows ? 'primary' : 'default'"
          size="small"
          @click="switchLayout(opt.cols, opt.rows)"
        >{{ opt.label }}</el-button>
      </el-button-group>
      <div class="grid-time-range">
        <span style="margin-right: 8px; color: #606266; font-size: 13px">统一时间范围：</span>
        <el-date-picker
            v-model="gridTimeRange"
            type="daterange"
            range-separator="至"
            start-placeholder="起始日期"
            end-placeholder="截止日期"
            value-format="YYYY-MM-DD"
            :default-time="[new Date(2000, 1, 1, 0, 0, 0), new Date(2000, 1, 1, 23, 59, 59)]"
            style="width: 300px"
        />
        <el-button type="primary" size="small" @click="loadAllGridCharts" style="margin-left: 10px">应用</el-button>
      </div>
    </div>
    <div class="grid-container" :style="{ gridTemplateColumns: `repeat(${gridLayout.cols}, 1fr)` }">
      <div
        v-for="(cell, idx) in visibleGridCells"
        :key="cell.index"
        class="grid-cell"
        :class="{ 'grid-cell--active': cell.sensorSeriesId, 'grid-cell--dragging': dragIdx === idx, 'grid-cell--dragover': dragOverIdx === idx }"
        draggable="true"
        @dragstart="onDragStart($event, idx)"
        @dragover.prevent="onDragOver($event, idx)"
        @dragleave="onDragLeave"
        @drop="onDrop($event, idx)"
        @dragend="onDragEnd"
      >
        <template v-if="cell.sensorSeriesId">
          <div class="grid-cell-header">
            <span class="grid-cell-title">{{ cell.title }}</span>
            <el-dropdown trigger="click" @command="(cmd: string) => handleGridCommand(cmd, idx)">
              <span class="grid-cell-more">···</span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="refresh">刷新</el-dropdown-item>
                  <el-dropdown-item command="fullscreen">全屏查看</el-dropdown-item>
                  <el-dropdown-item command="edit" divided>修改</el-dropdown-item>
                  <el-dropdown-item command="clear">清除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
          <div v-if="gridCellLoading.has(idx)" class="grid-cell-skeleton" />
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

    <!-- Grid Fullscreen Dialog -->
    <el-dialog
      v-model="gridFullscreenVisible"
      :title="gridFullscreenCell?.cell?.title || '图表全屏'"
      width="90%"
      top="3vh"
      destroy-on-close
      @closed="onGridFullscreenClosed"
    >
      <div ref="gridFullscreenChartRef" style="width: 100%; height: 75vh" />
    </el-dialog>

    <!-- Grid Config Dialog -->
    <el-dialog v-model="gridConfigDialogVisible" title="配置图表" width="480px" destroy-on-close>
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
        <el-form-item label="传感器">
          <el-select
              v-model="gridConfigForm.sensorId"
              placeholder="选择传感器（可选）"
              clearable
              style="width: 100%"
          >
            <el-option v-for="s in gridDialogSensors" :key="s.id" :label="s.sensorName" :value="s.id" />
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
</template>

<script setup lang="ts">
import {computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch} from 'vue'
import {ElMessage} from 'element-plus'
import {showRequestErrorMessage} from '@/utils/errorHandler'
import echarts from '@/utils/echarts'
import { getSensorRange } from '@/api/monitorData'
import { getHazardPointPage } from '@/api/hazardPoint'
import { getDevicePage, type DeviceItem } from '@/api/device'
import { getDeviceSensors, type SensorItem } from '@/api/sensor'
import { formatDateWithTime, formatTimestamp, formatXAxisLabel, getDefaultTimeRange } from './analysisUtils'

const emit = defineEmits<{
  (e: 'back'): void
}>()

// Types
interface HazardPointOption {
  id: number
  name: string
}

interface DeviceOption {
  id: number
  name: string
  deviceType: number
  boundHazardPointId: number
}

interface DeviceAttr {
  code: string
  name: string
  unit: string
  sensorCode: string
}

interface GridChartItem {
  index: number
  sensorSeriesId?: string
  title?: string
  hazardPointId?: number
  deviceId?: number
  sensorId?: number
  sensorName?: string
  sensorCode?: string
  attrCode?: string
  attrName?: string
  unit?: string
}

// Layout
const layoutOptions = [
  { cols: 3, rows: 3, label: '3×3' },
  { cols: 2, rows: 2, label: '2×2' },
  { cols: 1, rows: 2, label: '1×2' },
  { cols: 2, rows: 4, label: '2×4' },
]
const gridLayout = reactive({ cols: 3, rows: 3 })

const switchLayout = (cols: number, rows: number) => {
  // Dispose ALL existing chart instances — old DOM refs become stale after layout change
  gridChartInstances.forEach((inst) => inst.dispose())
  gridChartInstances.clear()
  gridChartRefs.clear()

  gridLayout.cols = cols
  gridLayout.rows = rows
  const total = cols * rows

  // Preserve existing configured cells, pad with empty slots
  const configured = gridCells.value.filter((c) => c.sensorSeriesId)
  const newCells: GridChartItem[] = []
  for (let i = 0; i < total; i++) {
    if (i < configured.length) {
      newCells.push(configured[i])
    } else {
      newCells.push({ index: i })
    }
  }
  gridCells.value = newCells

  // Re-render all configured cells in the new layout
  nextTick(() => {
    setTimeout(() => loadAllGridCharts(), 50)
  })
}

// Visible cells (computed from gridCells, with index = position)
const visibleGridCells = computed(() => gridCells.value)

// Drag and drop
const dragIdx = ref(-1)
const dragOverIdx = ref(-1)

const onDragStart = (e: DragEvent, idx: number) => {
  if (!e.dataTransfer) return
  dragIdx.value = idx
  e.dataTransfer.effectAllowed = 'move'
  e.dataTransfer.setData('text/plain', String(idx))
}

const onDragOver = (e: DragEvent, idx: number) => {
  if (dragIdx.value === -1 || dragIdx.value === idx) return
  dragOverIdx.value = idx
  if (e.dataTransfer) e.dataTransfer.dropEffect = 'move'
}

const onDragLeave = () => {
  dragOverIdx.value = -1
}

const onDrop = (e: DragEvent, targetIdx: number) => {
  const sourceIdx = dragIdx.value
  dragIdx.value = -1
  dragOverIdx.value = -1
  if (sourceIdx === -1 || sourceIdx === targetIdx) return

  // Swap cells
  const source = gridCells.value[sourceIdx]
  const target = gridCells.value[targetIdx]
  gridCells.value[sourceIdx] = target
  gridCells.value[targetIdx] = source

  // Dispose chart instances for swapped positions
  const srcInst = gridChartInstances.get(sourceIdx)
  const tgtInst = gridChartInstances.get(targetIdx)
  if (srcInst) { srcInst.dispose(); gridChartInstances.delete(sourceIdx) }
  if (tgtInst) { tgtInst.dispose(); gridChartInstances.delete(targetIdx) }

  // Re-render both cells
  nextTick(() => {
    if (gridCells.value[sourceIdx].sensorSeriesId) loadGridCellChart(sourceIdx)
    if (gridCells.value[targetIdx].sensorSeriesId) loadGridCellChart(targetIdx)
  })
}

const onDragEnd = () => {
  dragIdx.value = -1
  dragOverIdx.value = -1
}

// State
const hazardPointOptions = ref<HazardPointOption[]>([])
const gridCells = ref<GridChartItem[]>(Array.from({ length: gridLayout.cols * gridLayout.rows }, (_, i) => ({ index: i })))
const gridTimeRange = ref<[string, string] | null>(null)
const gridChartRefs = new Map<number, HTMLElement>()
const gridChartInstances = new Map<number, echarts.ECharts>()

const gridConfigDialogVisible = ref(false)
const gridConfigTargetIdx = ref(0)
const gridConfigForm = reactive({ hazardPointId: '' as number | '', deviceId: '' as number | '', sensorId: '' as number | '', attrCode: '' })
const gridDialogDevices = ref<DeviceOption[]>([])
const gridDialogSensors = ref<SensorItem[]>([])
const gridCellLoading = ref(new Set<number>())
const gridFullscreenVisible = ref(false)
const gridFullscreenCell = ref<{ idx: number; cell: GridChartItem } | null>(null)
const gridFullscreenChartRef = ref<HTMLElement>()
const gridFullscreenChartInstance = ref<echarts.ECharts | null>(null)

const gridFilteredDevices = computed(() => {
  return gridDialogDevices.value.filter(
      (d) => !gridConfigForm.hazardPointId || d.boundHazardPointId === gridConfigForm.hazardPointId
  )
})

const gridAvailableAttrs = computed(() => {
  if (!gridConfigForm.deviceId || gridDialogSensors.value.length === 0) return []
  const seen = new Set<string>()
  const attrs: DeviceAttr[] = []
  for (const sensor of gridDialogSensors.value) {
    for (const attr of sensor.attrList) {
      if (!seen.has(attr.attrCode)) {
        seen.add(attr.attrCode)
        attrs.push({
          code: attr.attrCode,
          name: attr.attrName || attr.attrCode,
          unit: attr.unit || '',
          sensorCode: sensor.sensorCode,
        })
      }
    }
  }
  return attrs.length > 0 ? attrs : [{ code: 'value', name: '监测值', unit: '', sensorCode: '1' }]
})

// 初始化默认时间范围
const initDefaultTimeRange = () => {
  const [start, end] = getDefaultTimeRange()
  gridTimeRange.value = [start, end]
}

// Load options
const loadOptions = async () => {
  try {
    const res = await getHazardPointPage({ pageNum: 1, pageSize: 500 })
    const rows = (res.data as any)?.rows ?? (res as any).rows ?? []
    hazardPointOptions.value = rows.map((item: any) => ({
      id: item.id,
      name: item.name
    }))
  } catch {
    hazardPointOptions.value = []
  }
}

// Grid handlers
const setGridChartRef = (idx: number, el: any) => {
  if (el) gridChartRefs.set(idx, el as HTMLElement)
}

const openGridConfig = (idx: number) => {
  gridConfigTargetIdx.value = idx
  gridConfigForm.hazardPointId = ''
  gridConfigForm.deviceId = ''
  gridConfigForm.sensorId = ''
  gridConfigForm.attrCode = ''
  gridDialogSensors.value = []
  gridConfigDialogVisible.value = true
}

const onGridConfigHpChange = async () => {
  gridConfigForm.deviceId = ''
  gridConfigForm.sensorId = ''
  gridConfigForm.attrCode = ''
  gridDialogSensors.value = []
  try {
    const params: any = { pageNum: 1, pageSize: 100 }
    if (gridConfigForm.hazardPointId) params.boundHazardPointId = gridConfigForm.hazardPointId
    const res = await getDevicePage(params)
    const rows = res.rows || []
    gridDialogDevices.value = rows.map((item: DeviceItem) => ({
      id: item.id!,
      name: item.name,
      deviceType: item.deviceType ?? 0,
      boundHazardPointId: item.boundHazardPointId ?? 0,
    }))
  } catch {
    gridDialogDevices.value = []
  }
}

const onGridConfigDeviceChange = async () => {
  gridConfigForm.sensorId = ''
  gridConfigForm.attrCode = ''
  gridDialogSensors.value = []
  if (gridConfigForm.deviceId) {
    try {
      gridDialogSensors.value = await getDeviceSensors(gridConfigForm.deviceId as number)
    } catch {
      gridDialogSensors.value = []
    }
  }
}

const confirmGridConfig = async () => {
  const device = gridDialogDevices.value.find((d) => d.id === gridConfigForm.deviceId)
  const attr = gridAvailableAttrs.value.find((a) => a.code === gridConfigForm.attrCode)
  if (!device || !attr) return

  const sensor = gridDialogSensors.value.find((s) => s.id === gridConfigForm.sensorId)

  const idx = gridConfigTargetIdx.value
  gridCells.value[idx] = {
    index: idx,
    sensorSeriesId: `${device.id}_${gridConfigForm.attrCode}_${Date.now()}`,
    title: `${device.name}-${attr.name}`,
    hazardPointId: device.boundHazardPointId,
    deviceId: device.id,
    sensorId: sensor?.id,
    sensorName: sensor?.sensorName,
    sensorCode: attr.sensorCode,
    attrCode: gridConfigForm.attrCode,
    attrName: attr.name,
    unit: attr.unit,
  }
  gridConfigDialogVisible.value = false

  await nextTick()
  loadGridCellChart(idx)
}

const loadGridCellChart = async (idx: number, fullscreen = false) => {
  const cell = gridCells.value[idx]
  const el = fullscreen ? gridFullscreenChartRef.value : gridChartRefs.get(idx)
  if (!cell.sensorSeriesId || !el || !cell.deviceId || !cell.attrCode || !cell.sensorCode) return

  if (!fullscreen) gridCellLoading.value.add(idx)

  // 获取时间范围，如果没有则使用默认的最近7天
  let startTime: string
  let endTime: string
  if (gridTimeRange.value && gridTimeRange.value.length === 2) {
    startTime = formatDateWithTime(gridTimeRange.value[0], false)
    endTime = formatDateWithTime(gridTimeRange.value[1], true)
  } else {
    const [defaultStart, defaultEnd] = getDefaultTimeRange()
    startTime = formatDateWithTime(defaultStart, false)
    endTime = formatDateWithTime(defaultEnd, true)
  }

  try {
    const dataMap: Record<string, { dataTime: string; value: number }[]> = await getSensorRange({
      deviceId: cell.deviceId,
      sensorCode: cell.sensorCode,
      attrCode: cell.attrCode,
      startTime,
      endTime,
    }) as any

    const rows = dataMap[cell.attrCode] || Object.values(dataMap)[0]
    if (!rows || rows.length === 0) {
      ElMessage.warning(`宫格 ${idx + 1} "${cell.title}" 在当前时间范围内无可用数据`)
      return
    }

    const sorted = [...rows].reverse()
    const times = sorted.map((r: any) => formatTimestamp(r.dataTime ?? r.time))
    const values = sorted.map((r: any) => r.value)

    const existing = gridChartInstances.get(idx)
    if (existing && !fullscreen) {
      existing.dispose()
      gridChartInstances.delete(idx)
    }

    const chart = echarts.init(el)
    if (fullscreen) {
      gridFullscreenChartInstance.value = chart
    } else {
      gridChartInstances.set(idx, chart)
    }

    const isRainfall = /^rainfall/.test(cell.attrCode)

    chart.setOption({
      grid: { left: 60, right: 25, top: fullscreen ? 35 : 25, bottom: 40 },
      xAxis: {
        type: 'category',
        name: '时间',
        nameLocation: 'end',
        nameGap: 2,
        nameTextStyle: { fontSize: 10, color: '#909399' },
        data: times,
        axisLabel: {
          fontSize: 9,
          rotate: 30,
          interval: Math.max(1, Math.floor(times.length / 6)),
          formatter: formatXAxisLabel,
        },
      },
      yAxis: {
        type: 'value',
        name: cell.attrName && cell.unit ? `${cell.attrName}(${cell.unit})` : (cell.unit || '监测值'),
        nameGap: 2,
        axisLabel: { fontSize: 10 },
        nameTextStyle: { fontSize: 10, color: '#606266' },
        axisLine: { show: true, lineStyle: { color: '#c0c4cc' } },
        splitLine: { show: true, lineStyle: { type: 'dashed', color: '#e8e8e8' } },
      },
      series: [isRainfall ? {
        type: 'bar',
        data: values,
        barWidth: '60%',
        itemStyle: {
          color: '#67c23a',
          borderRadius: [2, 2, 0, 0],
        },
      } : {
        type: 'line',
        data: values,
        smooth: true,
        symbol: 'none',
        lineStyle: { width: 1.5 },
      }],
      tooltip: { trigger: 'axis' },
      dataZoom: [{ type: 'inside' }, ...(fullscreen ? [{ type: 'slider', bottom: 10 }] : [])],
    })
  } catch (error) {
    showRequestErrorMessage(error, `加载宫格图表 ${idx + 1} 失败`)
  } finally {
    if (!fullscreen) {
      gridCellLoading.value.delete(idx)
      gridCellLoading.value = new Set(gridCellLoading.value)
    }
  }
}

const loadAllGridCharts = async () => {
  for (let i = 0; i < gridCells.value.length; i++) {
    if (gridCells.value[i].sensorSeriesId) {
      await loadGridCellChart(i)
    }
  }
}

const handleGridCommand = (cmd: string, idx: number) => {
  if (cmd === 'refresh') {
    loadGridCellChart(idx)
  } else if (cmd === 'fullscreen') {
    openGridFullscreen(idx)
  } else if (cmd === 'edit') {
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

const openGridFullscreen = (idx: number) => {
  gridFullscreenCell.value = { idx, cell: gridCells.value[idx] }
  gridFullscreenVisible.value = true
  nextTick(() => {
    setTimeout(() => loadGridCellChart(idx, true), 100)
  })
}

const onGridFullscreenClosed = () => {
  if (gridFullscreenChartInstance.value) {
    gridFullscreenChartInstance.value.dispose()
    gridFullscreenChartInstance.value = null
  }
}

// Resize handler
let resizeHandler: (() => void) | null = null

onMounted(() => {
  // 初始化默认时间范围（最近7天）
  initDefaultTimeRange()
  
  loadOptions()
  
  // 延迟加载图表，确保DOM渲染完成
  setTimeout(() => {
    loadAllGridCharts()
  }, 300)
  
  resizeHandler = () => {
    gridChartInstances.forEach((chart) => chart.resize())
  }
  window.addEventListener('resize', resizeHandler)
})

onBeforeUnmount(() => {
  gridChartInstances.forEach((chart) => chart.dispose())
  gridChartInstances.clear()
  gridChartRefs.clear()
  if (resizeHandler) {
    window.removeEventListener('resize', resizeHandler)
  }
})

// 监听时间范围变化，自动刷新图表
watch(gridTimeRange, (newVal) => {
  if (newVal && newVal.length === 2) {
    loadAllGridCharts()
  }
})
</script>

<style scoped>
.grid-mode {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.mode-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e8e8e8;
  flex-shrink: 0;
}
.mode-label {
  font-size: 16px;
  font-weight: bold;
  color: #303133;
}
.grid-layout-switcher {
  margin-left: 16px;
  flex-shrink: 0;
}
.grid-time-range {
  display: flex;
  align-items: center;
  margin-left: auto;
}
.grid-container {
  display: grid;
  gap: 16px;
  padding: 16px;
  flex: 1;
  min-height: 0;
  grid-auto-rows: 1fr;
}
.grid-cell {
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  position: relative;
  transition: border-color 0.3s, box-shadow 0.3s, opacity 0.2s;
  cursor: grab;
}
.grid-cell:active {
  cursor: grabbing;
}
.grid-cell--dragging {
  opacity: 0.45;
}
.grid-cell--dragover {
  border-color: #409eff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.25);
}
.grid-cell--active {
  border-color: #d9d9d9;
}
.grid-cell--active:hover {
  border-color: #409eff;
  box-shadow: 0 2px 12px rgba(64, 158, 255, 0.1);
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
  line-height: 1;
}
.grid-cell-hint {
  font-size: 11px;
  color: #c0c4cc;
}
.grid-cell-skeleton {
  position: absolute;
  inset: 36px 0 0 0;
  background: linear-gradient(90deg, #f5f7fa 25%, #e8ecf1 50%, #f5f7fa 75%);
  background-size: 200% 100%;
  animation: grid-shimmer 1.8s infinite;
  z-index: 1;
  pointer-events: none;
}
@keyframes grid-shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
</style>