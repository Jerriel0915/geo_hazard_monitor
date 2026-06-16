<!-- components/AnalysisDataGrid.vue -->
<template>
  <div class="grid-mode">
    <div class="mode-header">
      <el-button text @click="emit('back')">&larr; 返回</el-button>
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
</template>

<script setup lang="ts">
import {computed, nextTick, onBeforeUnmount, onMounted, reactive, ref} from 'vue'
import {ElMessage} from 'element-plus'
import {showRequestErrorMessage} from '@/utils/errorHandler'
import echarts from '@/utils/echarts'
import {
  type DeviceOption,
  type DeviceTypeOption,
  getChartData,
  getDeviceOptions,
  getDeviceTypeOptions,
  getHazardPointOptions,
  type GridChartItem,
  type HazardPointOption,
} from '@/api/report'

const emit = defineEmits<{
  (e: 'back'): void
}>()

// State
const hazardPointOptions = ref<HazardPointOption[]>([])
const deviceTypeOptions = ref<DeviceTypeOption[]>([])
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
      (d) => !gridConfigForm.hazardPointId || d.boundHazardPointId === gridConfigForm.hazardPointId
  )
})

const gridAvailableAttrs = computed(() => {
  const allAttrs: { code: string; name: string; unit: string }[] = []
  deviceTypeOptions.value.forEach((dt) => allAttrs.push(...dt.attrs))
  return allAttrs
})

// Load options
const loadOptions = async () => {
  const [hps, dts] = await Promise.all([getHazardPointOptions(), getDeviceTypeOptions()])
  hazardPointOptions.value = hps
  deviceTypeOptions.value = dts
}

// Grid handlers
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
    hazardPointId: device.boundHazardPointId,
    deviceId: device.id,
    attrCode: gridConfigForm.attrCode,
    attrName: attr.name,
    unit: attr.unit,
  }
  gridConfigDialogVisible.value = false

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

// Resize handler
let resizeHandler: (() => void) | null = null

onMounted(() => {
  loadOptions()
  resizeHandler = () => {
    gridChartInstances.forEach((chart) => chart.resize())
  }
  window.addEventListener('resize', resizeHandler)
})

onBeforeUnmount(() => {
  gridChartInstances.forEach((chart) => chart.dispose())
  if (resizeHandler) {
    window.removeEventListener('resize', resizeHandler)
  }
})
</script>

<style scoped>
.grid-mode {
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
.grid-time-range {
  display: flex;
  align-items: center;
  margin-left: auto;
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
</style>