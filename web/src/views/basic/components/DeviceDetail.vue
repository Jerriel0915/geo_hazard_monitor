<!-- src/views/device/components/DeviceDetail.vue -->
<template>
  <el-dialog
      v-model="dialogVisible"
      :title="`设备详情 — ${currentRow?.name || ''}`"
      width="960px"
      :close-on-click-modal="false"
      destroy-on-close
  >
    <el-tabs v-model="activeTab">
      <el-tab-pane label="设备详情" name="info">
        <!-- 原有设备详情内容保持不变 -->
        <el-descriptions :column="2" border>
          <el-descriptions-item label="设备编号">{{ currentRow?.code }}</el-descriptions-item>
          <el-descriptions-item label="设备名称">{{ currentRow?.name }}</el-descriptions-item>
          <el-descriptions-item label="设备SN">{{ currentRow?.sn || '-' }}</el-descriptions-item>
          <el-descriptions-item label="接入协议">{{ currentRow?.protocolType || '-' }}</el-descriptions-item>
          <el-descriptions-item label="注册来源">{{ currentRow?.registerSource || '-' }}</el-descriptions-item>
          <el-descriptions-item label="接入账号">{{ currentRow?.authUsername || '-' }}</el-descriptions-item>
          <el-descriptions-item label="接入密码">
            <template v-if="currentRow?.authPassword">
              <span class="pwd-masked">{{ pwdVisible ? currentRow.authPassword : '••••••••' }}</span>
              <el-button size="small" text type="primary" @click="pwdVisible = !pwdVisible">
                {{ pwdVisible ? '隐藏' : '查看' }}
              </el-button>
              <el-button size="small" text type="primary" @click="copyPwd(currentRow.authPassword)">复制</el-button>
            </template>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="安装位置">
            {{ formatCoord(currentRow?.longitude, currentRow?.latitude) }}
            <el-button v-if="currentRow?.longitude != null" size="small" text type="primary" @click="emit('viewOnMap', currentRow)">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                   stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="14" height="14">
                <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/>
                <circle cx="12" cy="10" r="3"/>
              </svg>
              查看
            </el-button>
          </el-descriptions-item>
          <el-descriptions-item label="设备状态">
            <el-tag :type="getStatusType(currentRow?.status || 0)" size="small">{{ currentRow?.statusName }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="在线状态">
            <el-tag :type="currentRow?.onlineStatus === 1 ? 'success' : 'info'" size="small">
              {{ currentRow?.onlineStatus === 1 ? '在线' : '离线' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="最近上报时间">{{ currentRow?.lastReportTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ currentRow?.createTime || '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">传感器列表</el-divider>
        <el-table :data="sensorList" border size="small" v-loading="sensorLoading">
          <el-table-column label="图标" width="60" align="center">
            <template #default="{ row }">
              <img v-if="getSensorIconPath(row)" :src="getSensorIconPath(row)" class="table-icon" alt="icon"/>
              <span v-else class="empty-text">-</span>
            </template>
          </el-table-column>
          <el-table-column prop="sensorCode" label="传感器编号" width="150" align="center"/>
          <el-table-column prop="sensorName" label="传感器名称" width="150" align="center"/>
          <el-table-column prop="monitorTypeName" label="监测类型" width="150" align="center"/>
          <el-table-column label="属性配置" min-width="250" align="center">
            <template #default="{ row }">
              <div v-for="attr in row.attrList" :key="attr.attrCode" class="attr-item">
                {{ attr.attrName }}: {{ attr.initialValue }}{{ attr.unit }}
              </div>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="运行状态变更" name="online">
        <el-table :data="onlineLogs" border size="small" max-height="400">
          <el-table-column prop="eventTime" label="时间" width="170"/>
          <el-table-column label="类型" width="80">
            <template #default="{row}">
              <el-tag :type="row.eventType==='ONLINE'?'success':'danger'" size="small">{{ row.eventType }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="clientId" label="Client ID" min-width="160"/>
          <el-table-column prop="clientIp" label="IP" width="140"/>
          <el-table-column prop="reason" label="原因" min-width="120"/>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="维修记录" name="maintenance">
        <el-table :data="maintenanceLogs" border size="small" max-height="400">
          <el-table-column label="操作" width="80">
            <template #default="{row}">
              <el-tag :type="row.newStatus === 1 ? 'success' : row.newStatus === 2 ? 'danger' : 'info'" size="small">
                {{ row.statusText }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态变化" width="110">
            <template #default="{row}">{{ getStatusLabel(row.oldStatus) }}→{{ getStatusLabel(row.newStatus) }}</template>
          </el-table-column>
          <el-table-column prop="operatorName" label="操作人" width="90"/>
          <el-table-column prop="operatorPhone" label="电话" width="120"/>
          <el-table-column prop="operationDate" label="操作日期" width="160"/>
          <el-table-column prop="createTime" label="记录时间" width="160"/>
          <el-table-column prop="description" label="描述" min-width="120"/>
        </el-table>
      </el-tab-pane>

      <!-- 新增：监测数据 Tab -->
      <el-tab-pane label="监测数据" name="monitorData">
        <div class="monitor-data-panel">
          <div class="data-filters">
            <!-- 去掉设备选择，直接显示传感器选择 -->
            <el-select v-model="localDataFilter.sensorId" placeholder="选择传感器" clearable style="width: 180px"
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
              <div v-if="chartSeriesData.length === 0" class="chart-empty-tip">暂无数据，请选择条件后查询</div>
            </div>
            <div v-else class="table-wrap">
              <div class="table-wrap__scroll">
                <el-table :data="monitorDataList" border size="small">
                  <el-table-column prop="dataTime" label="时间" min-width="180" align="center" />
                  <el-table-column prop="sensorName" label="传感器" width="150" align="center" />
                  <el-table-column prop="attrName" label="指标" width="120" align="center"/>
                  <el-table-column prop="value" label="数值" width="120" align="center" />
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
import { ref, watch } from 'vue'
import request from '@/utils/request'
import { getSensorIconPath } from '@/utils/deviceIcon'
import { type DeviceItem } from '../composables/useDeviceCrud'
import VueApexCharts from 'vue3-apexcharts'

interface Props {
  visible: boolean
  device: DeviceItem | null
}

interface Emits {
  (e: 'update:visible', value: boolean): void
  (e: 'viewOnMap', device: DeviceItem): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const dialogVisible = ref(false)
const activeTab = ref('info')
const pwdVisible = ref(false)
const sensorList = ref<any[]>([])
const onlineLogs = ref<any[]>([])
const maintenanceLogs = ref<any[]>([])
const sensorLoading = ref(false)

const currentRow = ref<DeviceItem | null>(null)

// 监测数据相关
const dataDisplayMode = ref('chart')
const monitorDataList = ref<any[]>([])
const chartSeriesData = ref<any[]>([])
const chartOptions = ref<any>({
  chart: { type: 'area', height: '100%', toolbar: { show: false } },
  xaxis: { type: 'datetime' },
  dataLabels: { enabled: false },
  stroke: { curve: 'smooth' }
})
const monitorSensors = ref<any[]>([])
const monitorAttrs = ref<any[]>([])
const localDataFilter = ref({
  sensorId: '',
  attrCode: '',
  valueType: 'current',
  timeRange: [] as string[]
})

// 监听 visible 变化
watch(() => props.visible, (val) => {
  dialogVisible.value = val
  if (val && props.device) {
    currentRow.value = props.device
    loadSensorList(props.device.id!)
    loadOpsLogs(props.device.id!)
    loadMonitorSensors(props.device.id!)
    // 重置监测数据相关
    activeTab.value = 'info'
    pwdVisible.value = false
    monitorDataList.value = []
    chartSeriesData.value = []
    localDataFilter.value = {
      sensorId: '',
      attrCode: '',
      valueType: 'current',
      timeRange: []
    }
  }
})

watch(() => dialogVisible.value, (val) => {
  if (!val) {
    activeTab.value = 'info'
    pwdVisible.value = false
    sensorList.value = []
    onlineLogs.value = []
    maintenanceLogs.value = []
    monitorDataList.value = []
    chartSeriesData.value = []
  }
  emit('update:visible', val)
})

// 加载传感器列表
const loadSensorList = async (deviceId: number) => {
  sensorLoading.value = true
  try {
    const res = await request.get(`/devices/${deviceId}/sensors`)
    sensorList.value = res.data || []
  } catch {
    sensorList.value = []
  } finally {
    sensorLoading.value = false
  }
}

// 加载运行日志和维修记录
const loadOpsLogs = async (deviceId: number) => {
  try {
    const [online, maint] = await Promise.all([
      request.get(`/devices/${deviceId}/online-logs`),
      request.get(`/devices/${deviceId}/maintenance-logs`)
    ])
    onlineLogs.value = online.data || []
    maintenanceLogs.value = maint.data || []
  } catch {
    onlineLogs.value = []
    maintenanceLogs.value = []
  }
}

// 重置筛选条件
const handleResetData = () => {
  localDataFilter.value = {
    sensorId: '',
    attrCode: '',
    valueType: 'current',
    timeRange: []
  }
  // 清空指标列表
  monitorAttrs.value = []
  // 清空数据
  monitorDataList.value = []
  chartSeriesData.value = []
  // 可选：重新加载传感器列表
  if (currentRow.value?.id) {
    loadMonitorSensors(currentRow.value.id)
  }
}

// 加载监测传感器列表（获取该设备下的传感器）
const loadMonitorSensors = async (deviceId: number) => {
  try {
    const res = await request.get(`/devices/${deviceId}/sensors`)
    monitorSensors.value = res.data || []
  } catch {
    monitorSensors.value = []
  }
}

// 传感器变化时加载指标
const onDataSensorChange = async (sensorId: string) => {
  if (!sensorId) {
    monitorAttrs.value = []
    localDataFilter.value.attrCode = ''
    return
  }
  try {
    const res = await request.get(`/sensors/${sensorId}/attributes`)
    monitorAttrs.value = res.data || []
  } catch {
    monitorAttrs.value = []
  }
}

// 查询监测数据
const handleQueryData = async () => {
  if (!localDataFilter.value.sensorId) {
    // ElMessage.warning('请选择传感器')
    return
  }
  try {
    const params = {
      sensorId: localDataFilter.value.sensorId,
      attrCode: localDataFilter.value.attrCode,
      valueType: localDataFilter.value.valueType,
      startTime: localDataFilter.value.timeRange?.[0],
      endTime: localDataFilter.value.timeRange?.[1]
    }
    const res = await request.post('/monitor/data/query', params)
    monitorDataList.value = res.data?.list || []
    // 处理图表数据
    if (res.data?.chartData) {
      chartSeriesData.value = res.data.chartData.series || []
      chartOptions.value = {
        ...chartOptions.value,
        series: chartSeriesData.value,
        xaxis: { ...chartOptions.value.xaxis, categories: res.data.chartData.categories || [] }
      }
    }
  } catch (error) {
    console.error('查询监测数据失败', error)
  }
}

const handleImportData = () => {
  // 导入数据逻辑
}

const handleExportData = () => {
  // 导出数据逻辑
}

// 工具函数
const getStatusType = (status: number) => {
  const map: Record<number, string> = { 1: 'success', 2: 'danger', 3: 'info' }
  return map[status] || 'info'
}

const getStatusLabel = (status: number) => {
  const map: Record<number, string> = { 1: '正常', 2: '故障', 3: '维修' }
  return map[status] || '-'
}

const formatCoord = (lng?: number | null, lat?: number | null) => {
  if (lng == null || lat == null) return '未设置'
  return `${lng}, ${lat}`
}

const copyPwd = (pwd: string) => {
  navigator.clipboard.writeText(pwd)
}
</script>

<style scoped>
.table-icon {
  width: 28px;
  height: 28px;
  object-fit: contain;
}

.empty-text {
  color: #909399;
}

.pwd-masked {
  font-family: monospace;
  letter-spacing: 2px;
  margin-right: 8px;
}

.attr-item {
  font-size: 13px;
  color: #606266;
  padding: 2px 0;
}

/* 监测数据样式 */
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
  align-items: center;
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