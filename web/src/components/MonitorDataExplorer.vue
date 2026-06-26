<!-- web/src/components/MonitorDataExplorer.vue -->
<template>
  <div class="monitor-data-explorer" :class="{ 'mde-fill': fillContainer }">
    <!-- 筛选栏（含视图切换，切换靠右） -->
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

      <!-- <el-select
        v-if="showValueType"
        v-model="filter.valueType"
        placeholder="聚合粒度"
        style="width: 120px"
      >
        <el-option label="原始值" value="current" />
        <el-option label="小时均值" value="hour" />
        <el-option label="日均值" value="24h" />
        <el-option label="3日均值" value="72h" />
      </el-select> -->

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

      <el-popover placement="bottom" :width="220" trigger="click">
        <template #reference>
          <el-button size="default" :type="downsampleEnabled ? 'primary' : 'default'" plain>
            降采样<el-icon class="el-icon--right"><CaretBottom /></el-icon>
          </el-button>
        </template>
        <div class="mde-downsample-popover">
          <div class="mde-downsample-row">
            <span>启用降采样</span>
            <el-switch v-model="downsampleEnabled" size="small" />
          </div>
          <div class="mde-downsample-row" v-if="downsampleEnabled">
            <span>粒度</span>
            <el-select v-model="downsampleGranularity" size="small" style="width: 100px">
              <el-option label="自动" value="auto" />
              <el-option label="1分钟" value="1m" />
              <el-option label="5分钟" value="5m" />
              <el-option label="10分钟" value="10m" />
              <el-option label="30分钟" value="30m" />
              <el-option label="1小时" value="1h" />
              <el-option label="6小时" value="6h" />
            </el-select>
          </div>
        </div>
      </el-popover>

      <el-button type="primary" :loading="loading" @click="query">查询</el-button>
      <el-button @click="reset">重置</el-button>

      <template v-if="showImportExport">
        <el-button @click="onImport">导入数据</el-button>
        <el-button @click="onExport">导出数据</el-button>
      </template>

      <!-- 图表/表格切换 -->
      <div class="mde-toolbar">
        <el-button-group>
          <el-button
            :type="mode === 'chart' ? 'primary' : 'default'"
            size="small"
            title="图表展示"
            @click="mode = 'chart'"
          ><el-icon><TrendCharts/></el-icon></el-button>
          <el-button
            :type="mode === 'table' ? 'primary' : 'default'"
            size="small"
            title="表格展示"
            @click="mode = 'table'"
          ><el-icon><Grid/></el-icon></el-button>
        </el-button-group>
      </div>
    </div>

    <!-- 降采样 / 大数据量提示 -->
    <div v-if="downsampleInfo" class="mde-info">
      已启用大数据优化 · 降采样间隔 {{ downsampleInfo.interval }}，展示 {{ downsampleInfo.pointCount }} 个数据点
    </div>
    <div v-else-if="dataPointWarning" class="mde-warning">
      数据点较多（{{ totalDataPoints }} 点），建议缩小时间范围或启用降采样以提升性能
    </div>

    <!-- 图表视图 -->
    <div v-show="mode === 'chart'" class="mde-chart-area">
      <div v-if="loading" class="mde-skeleton" />
      <EChartsWrapper
        v-else-if="chartSeries.length > 0"
        :option="chartOptions"
        :height="fillContainer ? '100%' : '500px'"
        :loading="loading"
        :show-fullscreen="true"
      />
      <div v-else class="mde-empty">
        <span>暂无数据，请选择条件后点击查询</span>
      </div>
    </div>

    <!-- 表格视图 -->
    <div v-show="mode === 'table'" class="mde-table-area">
      <el-table
        v-loading="loading"
        :data="tableData"
        border
        stripe
        size="small"
        :height="fillContainer ? '100%' : undefined"
        :max-height="fillContainer ? undefined : 400"
      >
        <el-table-column prop="dataTime" label="时间" min-width="170" align="center" sortable />
        <el-table-column prop="deviceName" label="设备" width="140" align="center" sortable />
        <el-table-column prop="sensorName" label="传感器" width="120" align="center" sortable />
        <el-table-column prop="attrName" label="指标" width="100" align="center" sortable />
        <el-table-column prop="unit" label="单位" width="80" align="center" sortable>
          <template #default="{ row }">
            {{ row.unit || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="value" label="数值" width="120" align="center" sortable />
      </el-table>
      <div v-if="!loading && tableData.length > 0" class="mde-table-pagination">
        <el-pagination
          v-model:current-page="tablePageNum"
          v-model:page-size="tablePageSize"
          :page-sizes="[20, 50, 100]"
          :total="tableTotal"
          layout="total, sizes, prev, pager, next, jumper"
          size="small"
          @size-change="handleTableSizeChange"
          @current-change="handleTablePageChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { ChartData, MonitorDataPageItem } from '@/api/monitorData'
import { useMonitorData } from '@/composables/useMonitorData'
import { ElMessage } from 'element-plus'
import { CaretBottom, Grid, TrendCharts } from '@element-plus/icons-vue'
import { computed, ref, watch } from 'vue'
import EChartsWrapper from '@/components/EChartsWrapper.vue'

const props = withDefaults(defineProps<{
  hazardPointId?: number | null
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
  /**
   * 弹性填充容器高度 — 适用于父级有确定高度的浮层/面板 (如 DeviceDataPanel)。
   * 开启后根元素 flex 撑满父级，图表/表格区随可用高度伸缩，避免固定 400px 导致溢出滚动。
   * 默认 false：保留 400px 固定高度，兼容 el-dialog/标签页 等无确定高度的滚动容器。
   */
  fillContainer?: boolean
}>(), {
  showDevice: true,
  showSensor: true,
  showAttr: true,
  showValueType: true,
  showImportExport: false,
  enableCompleteness: false,
  enableTrend: false,
  initialMode: 'chart',
  fillContainer: false,
})

const emit = defineEmits<{
  (e: 'data-loaded', data: { series: ChartData[]; list: MonitorDataPageItem[] }): void
  (e: 'device-change', deviceId: number): void
  (e: 'sensor-change', sensorId: number): void
}>()

const {
  devices,
  sensors,
  attrs,
  chartSeries,
  tableData,
  loading,
  mode,
  downsampleInfo,
  downsampleEnabled,
  downsampleGranularity,
  tablePageNum,
  tablePageSize,
  tableTotal,
  filter,
  selectDevice,
  selectSensor,
  query,
  queryPage,
  reset,
  buildChartOptions,
} = useMonitorData({
  hazardPointId: computed(() => props.hazardPointId ?? null),
  initialDeviceId: computed(() => props.initialDeviceId ?? null),
})

const chartOptions = computed(() => buildChartOptions(chartSeries.value))

const totalDataPoints = computed(() =>
  chartSeries.value.reduce((sum, s) => sum + s.labels.length, 0)
)
const dataPointWarning = computed(() => totalDataPoints.value > 2000)

const onDeviceChange = async (deviceId: string | number) => {
  await selectDevice(deviceId)
  if (deviceId) emit('device-change', Number(deviceId))
}

const onSensorChange = (sensorId: string | number) => {
  selectSensor(sensorId)
  if (sensorId) emit('sensor-change', Number(sensorId))
}

const onImport = () => ElMessage.info('导入功能开发中')
const onExport = () => ElMessage.info('导出功能开发中')

// 自动查询标记
const autoQueried = ref(false)

const handleTableSizeChange = () => {
  queryPage(1, tablePageSize.value)
}
const handleTablePageChange = (page: number) => {
  queryPage(page, tablePageSize.value)
}

const tryAutoQuery = () => {
  if (autoQueried.value) return
  autoQueried.value = true
  if (sensors.value.length > 0) {
    query()
  }
}

watch(() => props.initialDeviceId, async (id) => {
  if (id != null) {
    filter.deviceId = id
    await selectDevice(id)
    tryAutoQuery()
  }
}, { immediate: true })

// 无 initialDeviceId 时，设备列表加载后自动选择第一个有传感器的设备并查询
watch(devices, async (list) => {
  if (!autoQueried.value && list.length > 0 && !filter.deviceId) {
    // 找第一个有传感器的设备
    const firstWithSensors = list.find((d: any) => (d.sensors?.length || 0) > 0)
    if (!firstWithSensors) return
    autoQueried.value = true
    filter.deviceId = firstWithSensors.deviceId
    await selectDevice(firstWithSensors.deviceId)
    query()
  }
})

mode.value = props.initialMode

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
  margin-left: auto;
  flex-shrink: 0;
  display: flex;
  align-items: center;
}

.mde-warning {
  padding: 6px 12px;
  background: #fef3c7;
  color: #92400e;
  border-radius: 6px;
  font-size: 12px;
}

.mde-info {
  padding: 6px 12px;
  background: #ecfdf5;
  color: #065f46;
  border-radius: 6px;
  font-size: 12px;
}

.mde-chart-area,
.mde-table-area {
  min-height: 500px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  overflow: hidden;
  position: relative;
  display: flex;
  flex-direction: column;
}

.mde-table-pagination {
  display: flex;
  justify-content: flex-end;
  padding: 8px 12px;
  background: #fafafa;
  border-top: 1px solid #ebeef5;
  margin-top: auto;
}

.mde-skeleton {
  height: 500px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

.mde-empty {
  height: 500px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
  font-size: 14px;
}

/* ===== 弹性填充模式 (fillContainer) ===== */
/* 父级需为确定高度的 flex 列容器；根节点撑满，图表/表格区伸缩，skeleton/empty 跟随。 */
.monitor-data-explorer.mde-fill {
  flex: 1 1 0;
  min-height: 0;
}

.monitor-data-explorer.mde-fill .mde-chart-area {
  flex: 1 1 0;
  min-height: 0;
}

.monitor-data-explorer.mde-fill .mde-table-area {
  flex: 1 1 0;
  min-height: 0;
  overflow: hidden;
}

.monitor-data-explorer.mde-fill .mde-skeleton,
.monitor-data-explorer.mde-fill .mde-empty {
  height: 100%;
}

.mde-downsample-popover {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.mde-downsample-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
  color: #303133;
}
</style>
