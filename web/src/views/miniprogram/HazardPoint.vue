<template>
  <div class="page-content">
    <div class="page-title">隐患点</div>

    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-input v-model="searchName" placeholder="隐患点名称" clearable style="width: 200px"
                @keyup.enter="handleSearch"/>
      <el-select v-model="searchGroupId" placeholder="分组" clearable style="width: 160px; margin-left: 10px">
        <el-option v-for="g in groupList" :key="g.id" :label="g.name" :value="g.id"/>
      </el-select>
      <el-button type="primary" style="margin-left: 10px" @click="handleSearch">查询</el-button>
    </div>

    <!-- 隐患点列表 -->
    <el-table :data="list" v-loading="loading" border stripe style="margin-top: 16px">
      <el-table-column prop="code" label="编号" width="150" align="center"/>
      <el-table-column prop="name" label="名称" min-width="160"/>
      <el-table-column prop="groupName" label="分组" width="120" align="center"/>
      <el-table-column prop="statusName" label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.statusName }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="deviceCount" label="设备数" width="100" align="center"/>
      <el-table-column label="操作" width="120" align="center" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" size="small" link @click="openDetail(row)">查看详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrap">
      <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @change="loadList"
      />
    </div>

    <!-- 详情弹窗 -->
    <el-dialog
        v-model="detailVisible"
        :title="`隐患点详情 - ${currentRow?.name || ''}`"
        width="900px"
        :close-on-click-modal="false"
        destroy-on-close
    >
      <el-tabs v-model="detailTab" @tab-change="onTabChange">
        <!-- 已绑设备 -->
        <el-tab-pane label="已绑设备" name="devices">
          <div v-loading="deviceLoading">
            <div v-if="boundDevices.length === 0" class="empty-hint">暂无已绑定设备</div>
            <div v-for="dev in boundDevices" :key="dev.deviceId" class="device-card">
              <div class="device-header">
                <span class="device-name">{{ dev.deviceName }}</span>
                <el-tag :type="dev.deviceStatus === 1 ? 'success' : 'info'" size="small">
                  {{ dev.deviceStatus === 1 ? '在线' : '离线' }}
                </el-tag>
              </div>
              <div class="sensor-list">
                <el-tag v-for="s in dev.sensors" :key="s.id" size="small" type="warning" style="margin: 2px 4px">
                  {{ s.name }}
                </el-tag>
                <span v-if="!dev.sensors || dev.sensors.length === 0" class="no-sensor">暂无传感器</span>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <!-- 监测数据 -->
        <el-tab-pane label="监测数据" name="monitor">
          <div class="monitor-toolbar">
            <el-select v-model="monitorFilter.deviceId" placeholder="选择设备" clearable style="width: 180px"
                       @change="onDeviceChange">
              <el-option v-for="dev in boundDevices" :key="dev.deviceId" :label="dev.deviceName" :value="dev.deviceId"/>
            </el-select>
            <el-select v-model="monitorFilter.sensorId" placeholder="选择传感器" clearable
                       style="width: 180px; margin-left: 10px" @change="onSensorChange">
              <el-option v-for="s in monitorSensors" :key="s.id" :label="s.name" :value="s.id"/>
            </el-select>
            <el-select v-model="monitorFilter.attrCode" placeholder="选择指标" clearable
                       style="width: 160px; margin-left: 10px">
              <el-option v-for="a in monitorAttrs" :key="a.code" :label="a.label" :value="a.code"/>
            </el-select>
            <el-date-picker
                v-model="monitorTimeRange"
                type="datetimerange"
                range-separator="至"
                start-placeholder="开始"
                end-placeholder="结束"
                format="YYYY-MM-DD HH:mm"
                value-format="YYYY-MM-DD HH:mm:ss"
                style="margin-left: 10px; width: 300px"
            />
            <el-button type="primary" size="small" style="margin-left: 10px" @click="handleQueryMonitor">查询
            </el-button>
          </div>

          <el-tabs v-model="dataDisplay" style="margin-top: 12px">
            <!-- 最新值 -->
            <el-tab-pane label="最新值" name="latest">
              <div v-loading="latestLoading" class="latest-cards">
                <div v-if="latestList.length === 0" class="empty-hint">暂无数据</div>
                <div v-for="item in latestList" :key="item.attrCode" class="latest-card">
                  <div class="card-label">{{ item.attrName || item.attrCode }}</div>
                  <div class="card-value">{{ item.value }} <span class="card-unit">{{ item.unit }}</span></div>
                  <div class="card-sub">传感器: {{ item.sensorName }}</div>
                </div>
              </div>
            </el-tab-pane>

            <!-- 数据表格 -->
            <el-tab-pane label="数据表格" name="table">
              <el-table :data="monitorTableData" v-loading="tableLoading" border size="small">
                <el-table-column prop="dataTime" label="时间" width="170" align="center"/>
                <el-table-column prop="deviceName" label="设备" width="140" align="center"/>
                <el-table-column prop="sensorName" label="传感器" width="130" align="center"/>
                <el-table-column prop="value" label="数值" width="100" align="center"/>
                <el-table-column prop="unit" label="单位" width="80" align="center"/>
              </el-table>
              <div class="pagination-wrap">
                <el-pagination
                    v-model:current-page="monitorPageNum"
                    v-model:page-size="monitorPageSize"
                    :total="monitorTotal"
                    :page-sizes="[10, 20, 50]"
                    layout="total, sizes, prev, pager, next"
                    small
                    @change="loadMonitorTable"
                />
              </div>
            </el-tab-pane>

            <!-- 趋势图 -->
            <el-tab-pane label="趋势图" name="chart">
              <VueApexCharts
                    v-if="chartSeriesData.length > 0"
                    type="area"
                    height="100%"
                    :options="chartOptions"
                    :series="chartOptions.series"
                    class="chart-box"
                  />
                  <div v-else class="chart-empty">暂无图表数据</div>
            </el-tab-pane>
          </el-tabs>
        </el-tab-pane>
      </el-tabs>

      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import {nextTick, onMounted, reactive, ref} from 'vue'
import {ElMessage} from 'element-plus'
import VueApexCharts from 'vue3-apexcharts'
import {getBoundDevices, getHazardPointGroups, getHazardPointPage} from '@/api/hazardPoint'
import {
  type ChartData,
  getChartData,
  getLatestData,
  getMonitorDataPage,
  type LatestDataItem,
  type MonitorDataPageItem
} from '@/api/monitorData'
import {getDeviceSensors, type SensorAttrItem, type SensorItem} from '@/api/sensor'

// ----- 列表 -----
const list = ref<any[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const searchName = ref('')
const searchGroupId = ref<number | null>(null)
const groupList = ref<any[]>([])

const loadList = async () => {
  loading.value = true
  try {
    const res: any = await getHazardPointPage({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      name: searchName.value || undefined,
      groupId: searchGroupId.value ?? undefined
    })
    list.value = res.rows || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pageNum.value = 1;
  loadList()
}

const loadGroups = async () => {
  try {
    const res: any = await getHazardPointGroups()
    groupList.value = res.data || []
  } catch { /* ignore */
  }
}

// ----- 详情 -----
const detailVisible = ref(false)
const detailTab = ref('devices')
const currentRow = ref<any>(null)

// 设备
const boundDevices = ref<any[]>([])
const deviceLoading = ref(false)

// 监测
const monitorFilter = reactive({deviceId: null as number | null, sensorId: null as number | null, attrCode: ''})
const monitorTimeRange = ref<[string, string] | null>(null)
const monitorSensors = ref<any[]>([])
const monitorAttrs = ref<{ code: string; label: string }[]>([])
const dataDisplay = ref('latest')

const latestList = ref<LatestDataItem[]>([])
const latestLoading = ref(false)
const monitorTableData = ref<MonitorDataPageItem[]>([])
const monitorPageNum = ref(1)
const monitorPageSize = ref(10)
const monitorTotal = ref(0)
const tableLoading = ref(false)

// ----- 打开详情 -----
const openDetail = async (row: any) => {
  currentRow.value = row
  detailVisible.value = true
  detailTab.value = 'devices'
  await loadBoundDevices()
}

const loadBoundDevices = async () => {
  deviceLoading.value = true
  try {
    const res: any = await getBoundDevices(String(currentRow.value.id))
    boundDevices.value = (res.data || []).map((d: any) => ({
      ...d,
      sensors: d.sensors || []
    }))
  } finally {
    deviceLoading.value = false
  }
}

const monitorSensorMap = ref<Map<number, SensorItem>>(new Map())
const sensorLoading = ref(false)

// 设备选择 → 加载该设备下的传感器列表（含属性定义）
const onDeviceChange = async () => {
  monitorFilter.sensorId = null
  monitorFilter.attrCode = ''
  monitorSensors.value = []
  monitorAttrs.value = []
  if (!monitorFilter.deviceId) return

  sensorLoading.value = true
  try {
    const sensors = await getDeviceSensors(monitorFilter.deviceId)
    const map = new Map(monitorSensorMap.value)
    for (const s of sensors) {
      if (s.id != null) map.set(s.id, s)
    }
    monitorSensorMap.value = map
    monitorSensors.value = sensors.map(s => ({id: s.id, name: s.sensorName}))
  } catch {
    ElMessage.error('获取传感器列表失败')
  } finally {
    sensorLoading.value = false
  }
}

// 传感器选择 → 提取该传感器的属性作为指标选项
const onSensorChange = () => {
  monitorFilter.attrCode = ''
  if (!monitorFilter.sensorId) {
    monitorAttrs.value = []
    return
  }
  const sensor = monitorSensorMap.value.get(monitorFilter.sensorId)
  if (sensor?.attrList) {
    monitorAttrs.value = sensor.attrList.map((a: SensorAttrItem) => ({
      code: a.attrCode,
      label: `${a.attrName || a.attrCode}${a.unit ? ` (${a.unit})` : ''}`
    }))
  } else {
    monitorAttrs.value = []
  }
}

const onTabChange = (tab: string) => {
  if (tab === 'devices') loadBoundDevices()
  if (tab === 'monitor' && boundDevices.value.length === 0) loadBoundDevices()
}

// ----- 监测数据查询 -----
const buildMonitorParams = () => ({
  hazardPointId: Number(currentRow.value.id),
  deviceId: monitorFilter.deviceId ?? undefined,
  sensorId: monitorFilter.sensorId ?? undefined,
  attrCode: monitorFilter.attrCode || undefined,
  startTime: monitorTimeRange.value?.[0],
  endTime: monitorTimeRange.value?.[1]
})

const handleQueryMonitor = async () => {
  if (dataDisplay.value === 'latest') await loadLatest()
  else if (dataDisplay.value === 'table') {
    monitorPageNum.value = 1;
    await loadMonitorTable()
  } else if (dataDisplay.value === 'chart') await loadChart()
}

const loadLatest = async () => {
  latestLoading.value = true
  try {
    const res = await getLatestData(Number(currentRow.value.id))
    latestList.value = (res || []) as LatestDataItem[]
  } catch {
    ElMessage.error('获取最新数据失败')
  } finally {
    latestLoading.value = false
  }
}

const loadMonitorTable = async () => {
  tableLoading.value = true
  try {
    const res: any = await getMonitorDataPage({
      ...buildMonitorParams(),
      pageNum: monitorPageNum.value,
      pageSize: monitorPageSize.value
    })
    monitorTableData.value = res.rows || []
    monitorTotal.value = res.total || 0
  } catch {
    ElMessage.error('获取历史数据失败')
  } finally {
    tableLoading.value = false
  }
}

// ----- 趋势图（ApexCharts）-----
const chartSeriesData = ref<ChartData[]>([])
const chartOptions = ref<Record<string, any>>({})

const CHART_COLORS = [
  '#5470C6', '#91CC75', '#FAC858', '#EE6666', '#73C0DE',
  '#3BA272', '#FC8452', '#9A60B4', '#EA7CCC', '#909399'
]

const buildChartOptions = () => {
  const seriesData = chartSeriesData.value
  if (seriesData.length === 0) return

  const allLabels = new Set<string>()
  for (const s of seriesData) for (const l of s.labels) allLabels.add(l)
  const xCategories = Array.from(allLabels).sort()

  chartOptions.value = {
    chart: {
      type: 'area' as const,
      height: '100%',
      fontFamily: 'inherit',
      toolbar: { tools: { download: true, zoom: true, zoomin: true, zoomout: true, pan: true, reset: true } },
      zoom: { enabled: true, type: 'x' as const },
      animations: { enabled: true, easing: 'easeinout' as const, speed: 800 }
    },
    colors: CHART_COLORS,
    dataLabels: { enabled: false },
    stroke: { curve: 'smooth' as const, width: 2 },
    fill: { type: 'gradient', gradient: { shadeIntensity: 1, opacityFrom: 0.2, opacityTo: 0.02, stops: [0, 100] } },
    markers: { size: 0, hover: { size: 5 } },
    grid: { borderColor: '#e7e7e7', strokeDashArray: 4, padding: { top: 10, right: 10, bottom: 5, left: 10 } },
    legend: { position: 'top' as const, horizontalAlign: 'center' as const, fontSize: '13px', fontWeight: 500, itemMargin: { horizontal: 16 } },
    xaxis: { type: 'category' as const, categories: xCategories, labels: { rotate: -30, style: { fontSize: '11px' } }, tickAmount: Math.min(xCategories.length, 10) },
    yaxis: { title: { text: seriesData[0]?.unit || '', style: { fontSize: '12px' } } },
    tooltip: { shared: true, intersect: false },
    series: seriesData.map(s => {
      const points = s.labels.map((l, i) => ({ x: l, y: s.values[i] }))
      return { name: s.seriesName, data: points }
    })
  }
}

const loadChart = async () => {
  if (!monitorTimeRange.value || monitorTimeRange.value.length < 2) {
    ElMessage.warning('请选择时间范围')
    return
  }
  try {
    const series = await getChartData({
      ...buildMonitorParams(),
      startTime: monitorTimeRange.value![0],
      endTime: monitorTimeRange.value![1]
    })
    chartSeriesData.value = series
    await nextTick()
    buildChartOptions()
  } catch {
    ElMessage.error('获取图表数据失败')
  }
}

const renderChart = (data: ChartData) => {
  if (!chartRef.value) return
  if (!chartInstance) chartInstance = echarts.init(chartRef.value)
  chartInstance.setOption({
    tooltip: {trigger: 'axis'},
    xAxis: {type: 'category', data: data.labels || [], axisLabel: {rotate: 30, fontSize: 10}},
    yAxis: {type: 'value', name: data.unit || ''},
    dataZoom: [{type: 'inside'}, {type: 'slider'}],
    series: [{
      name: data.attrName || '',
      type: 'line', data: data.values || [], smooth: true,
      lineStyle: {color: '#1890ff', width: 2},
      areaStyle: {color: 'rgba(64,158,255,0.1)'}
    }],
    grid: {left: 50, right: 20, top: 20, bottom: 60}
  }, true)
}


const formatTime = (ts: number) => {
  if (!ts) return ''
  return new Date(ts).toISOString().replace('T', ' ').substring(0, 19)
}

// ----- 生命周期 -----
onMounted(() => {
  loadGroups()
  loadList()
})
</script>

<style scoped>
.page-content {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  min-height: calc(100% - 32px);
}
.page-title {
  font-size: 18px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 1px solid #e8e8e8;
}

.search-bar {
  display: flex;
  align-items: center;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.empty-hint {
  text-align: center;
  color: #909399;
  padding: 40px;
  font-size: 14px;
}

.no-sensor {
  color: #c0c4cc;
  font-size: 12px;
}

.device-card {
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 12px 16px;
  margin-bottom: 12px;
}

.device-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.device-name {
  font-weight: 600;
  font-size: 15px;
}

.sensor-list {
  display: flex;
  flex-wrap: wrap;
}

.monitor-toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
}

.latest-cards {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.latest-card {
  width: 180px;
  padding: 14px 16px;
  border-radius: 8px;
  background: #f5f7fa;
  text-align: center;
}

.card-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 6px;
}

.card-value {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
}

.card-unit {
  font-size: 14px;
  font-weight: normal;
  color: #909399;
  margin-left: 4px;
}

.card-sub {
  font-size: 11px;
  color: #c0c4cc;
  margin-top: 4px;
}

.chart-box {
  width: 100%;
  height: 360px;
}
</style>
