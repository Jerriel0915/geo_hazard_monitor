<!-- src/views/basic/components/HazardPointDetail.vue -->
<template>
  <el-dialog
      v-model="dialogVisible"
      title="隐患点详情"
      width="1000px"
      :close-on-click-modal="false"
      destroy-on-close
  >
    <el-tabs v-model="activeTab">
      <el-tab-pane label="基本信息" name="basic">
        <div class="basic-info-container">
          <div class="info-section">
            <h3 class="section-title">隐患点信息</h3>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="隐患点编号">{{ currentRow?.code }}</el-descriptions-item>
              <el-descriptions-item label="隐患点名称">{{ currentRow?.name }}</el-descriptions-item>
              <el-descriptions-item label="分组">{{ currentRow?.groupName || '未分组' }}</el-descriptions-item>
              <el-descriptions-item label="状态">
                <el-tag :type="getStatusType(currentRow?.status || '')" size="small">{{ currentRow?.statusName }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="中心坐标" :span="2">
                {{ currentRow?.longitude?.toFixed(6) }}, {{ currentRow?.latitude?.toFixed(6) }}
              </el-descriptions-item>
              <el-descriptions-item label="走向">{{ currentRow?.strike }}°</el-descriptions-item>
              <el-descriptions-item label="描述" :span="2">{{ currentRow?.description || '-' }}</el-descriptions-item>
            </el-descriptions>
          </div>

          <div class="map-section">
            <h3 class="section-title">隐患点区域展示</h3>
            <MapBoundaryPreview
                v-if="currentRow"
                :initial-value="parsedBoundary"
                :initial-center="previewCenter"
                height="300px"
            />
          </div>

          <div class="system-info-section">
            <h3 class="section-title">系统信息</h3>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="创建人">{{ currentRow?.createBy || '-' }}</el-descriptions-item>
              <el-descriptions-item label="创建时间">{{ currentRow?.createTime || '-' }}</el-descriptions-item>
              <el-descriptions-item label="更新人">{{ currentRow?.updateBy || '-' }}</el-descriptions-item>
              <el-descriptions-item label="更新时间">{{ currentRow?.updateTime || '-' }}</el-descriptions-item>
            </el-descriptions>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="绑定设备" name="devices">
        <div class="table-wrap">
          <div class="table-wrap__scroll">
            <el-table :data="boundDevices" border size="small">
              <el-table-column prop="deviceCode" label="设备编号" width="150" align="center" />
              <el-table-column prop="deviceName" label="设备名称" min-width="150" align="center" />
              <el-table-column prop="sensorNames" label="传感器" min-width="150" align="center">
                <template #default="{ row }">
                  <span v-for="sensor in row.sensors" :key="sensor.id" class="sensor-tag">
                    {{ sensor.name }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column prop="bindTime" label="绑定时间" width="180" align="center" />
              <el-table-column prop="deviceStatus" label="设备状态" width="100" align="center">
                <template #default="{ row }">
                  <el-tag :type="row.deviceStatus === 'NORMAL' ? 'success' : row.deviceStatus === 'FAULT' ? 'danger' : 'warning'" size="small">
                    {{ row.deviceStatus === 'NORMAL' ? '正常' : row.deviceStatus === 'FAULT' ? '故障' : '离线' }}
                  </el-tag>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="告警配置" name="alarmConfig">
        <div class="alarm-config-view">
          <div class="config-section">
            <h3 class="section-title">告警判据</h3>
            <div class="table-wrap">
              <div class="table-wrap__scroll">
                <el-table :data="alarmCriteriaList" border size="small">
                  <el-table-column prop="name" label="判据名称" width="150" align="center" />
                  <el-table-column prop="monitorTypeName" label="监测类型" width="150" align="center" />
                  <el-table-column prop="expression" label="表达式" min-width="250" align="center" />
                  <el-table-column prop="alarmLevel" label="告警等级" width="100" align="center">
                    <template #default="{ row }">
                      <el-tag :type="getAlarmLevelType(row.alarmLevel)" size="small">{{ row.alarmLevelText }}</el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column prop="isEnabled" label="状态" width="80" align="center">
                    <template #default="{ row }">
                      <el-tag :type="row.isEnabled ? 'success' : 'info'" size="small">{{ row.isEnabled ? '启用' : '禁用' }}</el-tag>
                    </template>
                  </el-table-column>
                </el-table>
              </div>
            </div>
          </div>

          <div class="config-section">
            <h3 class="section-title">告警分发</h3>
            <div class="table-wrap">
              <div class="table-wrap__scroll">
                <el-table :data="dispatchRules" border size="small">
                  <el-table-column prop="name" label="规则名称" width="150" align="center" />
                  <el-table-column label="类型" width="100" align="center">
                    <template #default="{ row }">
                      <el-tag :type="row.type === 'ALARM' ? 'warning' : 'info'" size="small">
                        {{ row.type === 'ALARM' ? '告警分发' : '状态通知' }}
                      </el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column prop="alarmLevel" label="告警等级" width="120" align="center">
                    <template #default="{ row }">
                      <span v-if="row.type === 'ALARM'">{{ row.alarmLevel }}</span>
                      <span v-else class="empty-text">-</span>
                    </template>
                  </el-table-column>
                  <el-table-column prop="recipientName" label="接收人" width="120" align="center" />
                  <el-table-column label="通知渠道" min-width="150" align="center">
                    <template #default="{ row }">
                      <span v-for="ch in row.channel.split(',')" :key="ch" class="channel-tag">{{ getChannelLabel(ch) }}</span>
                    </template>
                  </el-table-column>
                  <el-table-column prop="isEnabled" label="状态" width="80" align="center">
                    <template #default="{ row }">
                      <el-tag :type="row.isEnabled ? 'success' : 'info'" size="small">{{ row.isEnabled ? '启用' : '禁用' }}</el-tag>
                    </template>
                  </el-table-column>
                </el-table>
              </div>
            </div>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="监测数据" name="monitorData">
        <div class="monitor-data-panel">
          <div class="data-filters">
            <el-select v-model="localDataFilter.deviceId" placeholder="选择设备" clearable style="width: 150px"
                       @change="onDataDeviceChange">
              <el-option v-for="d in boundDevices" :key="d.deviceId" :label="d.deviceName" :value="d.deviceId" />
            </el-select>
            <el-select v-model="localDataFilter.sensorId" placeholder="选择传感器" clearable style="width: 150px"
                       @change="onDataSensorChange">
              <el-option v-for="s in monitorSensors" :key="s.id" :label="s.name" :value="s.id"/>
            </el-select>
            <el-select v-model="localDataFilter.attrCode" placeholder="选择指标" clearable style="width: 160px">
              <el-option v-for="a in monitorAttrs" :key="a.code" :label="a.label" :value="a.code"/>
            </el-select>
            <el-select v-model="localDataFilter.valueType" placeholder="聚合粒度" style="width: 120px">
              <el-option label="原始值" value="current" />
              <el-option label="小时均值" value="hour" />
              <el-option label="日均值" value="24h" />
              <el-option label="3日均值" value="72h" />
            </el-select>
            <el-date-picker
                v-model="localDataFilter.timeRange"
                type="datetimerange"
                range-separator="至"
                start-placeholder="开始"
                end-placeholder="结束"
                format="YYYY-MM-DD HH:mm:ss"
                value-format="YYYY-MM-DD HH:mm:ss"
                style="width: 360px"
            />
            <el-button type="primary" size="small" @click="handleQueryData">查询</el-button>
            <el-button size="small" @click="handleResetData">重置</el-button>
          </div>

          <div class="data-toolbar">
            <el-button-group>
              <el-button :type="dataDisplayMode === 'chart' ? 'primary' : 'default'" size="small" @click="dataDisplayMode = 'chart'">图表展示</el-button>
              <el-button :type="dataDisplayMode === 'table' ? 'primary' : 'default'" size="small" @click="dataDisplayMode = 'table'">表格展示</el-button>
            </el-button-group>
            <div class="data-actions">
              <el-button size="small" @click="handleImportData">导入数据</el-button>
              <el-button size="small" @click="handleExportData">导出数据</el-button>
            </div>
          </div>

          <div class="data-content">
            <div v-if="dataDisplayMode === 'chart'" class="chart-container">
              <VueApexCharts
                  v-if="chartSeriesData.length > 0"
                  type="area"
                  height="100%"
                  :options="chartOptions"
                  :series="chartOptions.series"
              />
              <div v-if="chartSeriesData.length === 0" class="chart-empty-tip">暂无数据，选择条件后将自动加载近3天数据</div>
            </div>
            <div v-else class="table-wrap">
              <div class="table-wrap__scroll">
                <el-table :data="monitorDataList" border size="small">
                  <el-table-column prop="dataTime" label="时间" min-width="180" align="center" />
                  <el-table-column prop="deviceName" label="设备" width="150" align="center" />
                  <el-table-column prop="sensorName" label="传感器" width="120" align="center" />
                  <el-table-column prop="attrName" label="指标" width="100" align="center"/>
                  <el-table-column prop="value" label="数值" width="100" align="center" />
                  <el-table-column prop="unit" label="单位" width="80" align="center" />
                  <el-table-column prop="qualityText" label="质量" width="80" align="center" />
                </el-table>
              </div>
            </div>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <template #footer>
      <el-button @click="dialogVisible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import MapBoundaryPreview from '@/components/map/MapBoundaryPreview.vue'
import VueApexCharts from 'vue3-apexcharts'
import { deserialize, type LatLng } from '@/lib/boundaryCoords'
import { getAlarmLevelType, getChannelLabel, getStatusType, type HazardPointItem } from '../composables/useHazardPointCrud'
import type { BoundDevice } from '../composables/useHazardPointDeviceBind'
import type { AlarmCriteria, DispatchRule } from '../composables/useHazardPointAlarm'

interface Props {
  visible: boolean
  hazardPoint: HazardPointItem | null
  boundDevices: BoundDevice[]
  alarmCriteriaList: AlarmCriteria[]
  dispatchRules: DispatchRule[]
}

interface Emits {
  (e: 'update:visible', value: boolean): void
  (e: 'queryData', params: any): void
  (e: 'importData'): void
  (e: 'exportData'): void
  (e: 'deviceChange', deviceId: string): void
  (e: 'sensorChange', sensorId: string): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const dialogVisible = ref(false)
const activeTab = ref('basic')
const dataDisplayMode = ref('chart')
const monitorDataList = ref<any[]>([])
const chartSeriesData = ref<any[]>([])
const chartOptions = ref<any>({})
const monitorSensors = ref<any[]>([])
const monitorAttrs = ref<any[]>([])
const localDataFilter = ref({
  deviceId: '',
  sensorId: '',
  attrCode: '',
  valueType: 'current',
  timeRange: [] as string[]
})

const currentRow = computed(() => props.hazardPoint)

const parsedBoundary = computed(() => {
  if (!currentRow.value) return null
  return deserialize((currentRow.value as any).boundaryCoords)
})

const previewCenter = computed<LatLng | null>(() => {
  const r = currentRow.value
  if (!r || r.latitude == null || r.longitude == null) return null
  return { lat: r.latitude, lng: r.longitude }
})

watch(() => props.visible, (val) => {
  dialogVisible.value = val
  if (val && props.hazardPoint) {
    activeTab.value = 'basic'
  }
})

watch(() => dialogVisible.value, (val) => {
  emit('update:visible', val)
})

// 重置筛选条件
const handleResetData = () => {
  localDataFilter.value = {
    deviceId: '',
    sensorId: '',
    attrCode: '',
    valueType: 'current',
    timeRange: []
  }
  // 清空传感器和指标列表
  monitorSensors.value = []
  monitorAttrs.value = []
  // 清空数据
  monitorDataList.value = []
  chartSeriesData.value = []

  // 可选：重新加载设备列表（如果需要）
  // 注意：boundDevices 是从 props 传入的，不需要重新加载
}

const onDataDeviceChange = (deviceId: string) => {
  emit('deviceChange', deviceId)
}

const onDataSensorChange = (sensorId: string) => {
  emit('sensorChange', sensorId)
}

const handleQueryData = () => {
  emit('queryData', localDataFilter.value)
}

const handleImportData = () => {
  emit('importData')
}

const handleExportData = () => {
  emit('exportData')
}

// 暴露方法给父组件更新数据
const updateMonitorData = (data: any) => {
  monitorDataList.value = data.list || []
  chartSeriesData.value = data.series || []
  chartOptions.value = data.options || {}
}

const updateSensors = (sensors: any[]) => {
  monitorSensors.value = sensors
}

const updateAttrs = (attrs: any[]) => {
  monitorAttrs.value = attrs
}

defineExpose({
  updateMonitorData,
  updateSensors,
  updateAttrs,
  localDataFilter
})
</script>

<style scoped>
/* 样式从原文件复制过来 */
.basic-info-container {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.info-section,
.map-section,
.system-info-section {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 16px;
}

.section-title {
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 8px;
  display: block;
  font-size: 13px;
}

.info-section .section-title,
.map-section .section-title,
.system-info-section .section-title {
  font-size: 13px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #e8e8e8;
}

.sensor-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-right: 6px;
  margin-bottom: 4px;
  padding: 2px 8px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 4px;
  font-size: 11px;
  color: #475569;
}

.channel-tag {
  display: inline-block;
  padding: 2px 8px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 4px;
  font-size: 11px;
  margin-right: 4px;
  color: #475569;
}

.alarm-config-view {
  padding: 8px;
}

.config-section {
  margin-bottom: 24px;
}

.config-section .section-title {
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 14px;
  padding-bottom: 10px;
  border-bottom: 2px solid #1890ff;
}

.monitor-data-panel {
  padding: 8px 0;
}

.data-filters {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
  padding: 14px 16px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  flex-wrap: nowrap;
}

.data-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.data-actions {
  display: flex;
  gap: 8px;
}

.data-content {
  height: 400px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  overflow: hidden;
}

.chart-container {
  width: 100%;
  height: 100%;
  position: relative;
}

.chart-empty-tip {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: #94a3b8;
  font-size: 13px;
  pointer-events: none;
}

.empty-text {
  color: #909399;
}

.table-wrap {
  flex: 1;
  width: 100%;
  min-height: 0;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.table-wrap__scroll {
  flex: 1;
  min-height: 0;
  overflow: auto;
}
</style>