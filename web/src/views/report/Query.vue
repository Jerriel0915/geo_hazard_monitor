<template>
  <div class="page">
    <div class="header">
      <div class="header__left">
        <h2 class="header__title">查询中心</h2>
        <span class="header__subtitle">多维度监测数据灵活查询与导出</span>
      </div>
      <div class="header__right">
        <el-button @click="handleExportCsv">导出CSV</el-button>
      </div>
    </div>

    <div class="search">
      <el-select
          v-model="selectedHazardPointId"
          placeholder="隐患点"
          clearable
          @change="onHazardPointChange"
      >
        <el-option
            v-for="hp in hazardPointOptions"
            :key="hp.id"
            :label="hp.name"
            :value="hp.id"
        />
      </el-select>

      <el-select
          v-model="selectedDeviceId"
          placeholder="设备"
          clearable
          @change="onDeviceChange"
      >
        <el-option
            v-for="dev in deviceOptions"
            :key="dev.id"
            :label="dev.name"
            :value="dev.id"
        />
      </el-select>

      <el-select
          v-model="selectedAttrCodes"
          placeholder="监测属性"
          multiple
          collapse-tags
          collapse-tags-tooltip
          clearable
      >
        <el-option
            v-for="attr in availableAttrs"
            :key="attr.code"
            :label="`${attr.name}(${attr.unit})`"
            :value="attr.code"
        />
      </el-select>

      <el-date-picker
          v-model="timeRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          value-format="YYYY-MM-DD HH:mm:ss"
          :default-time="[new Date(2000, 1, 1, 0, 0, 0), new Date(2000, 1, 1, 23, 59, 59)]"
      />

      <el-button type="primary" @click="handleQuery">查询</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>

    <div class="table-wrap">
      
      <div class="table-wrap__scroll">
        <el-table
            :data="tableData"
            border
            stripe
            v-loading="loading"
        >
          <el-table-column prop="dataTime" label="时间" width="170" align="center" />
          <el-table-column prop="deviceName" label="设备名称" width="180" align="center" />
          <el-table-column prop="sensorName" label="传感器" width="180" align="center" />
          <el-table-column label="监测数据" min-width="300" align="center">
            <template #default="{ row }">
              <span class="monitor-data-item">
                {{ row.dataList.map((d: any) => `${d.attrName}: ${d.value} ${d.unit}`).join('; ') }}
              </span>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="table-wrap__pagination">
        <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :page-sizes="[10, 20, 50, 100]"
            :total="total"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleQuery"
            @current-change="handleQuery"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { showRequestErrorMessage } from '@/utils/errorHandler'
import { getHazardPointPage } from '@/api/hazardPoint'
import { getDevicePage, type DeviceItem } from '@/api/device'
import { getMonitorDataPage, type MonitorDataPageItem, type MonitorDataPageQuery } from '@/api/monitorData'
import { getDeviceSensors } from '@/api/sensor'

// 类型定义
interface HazardPointOption {
  id: number
  name: string
}

interface DeviceAttr {
  code: string
  name: string
  unit: string
}

interface DeviceOption {
  id: number
  name: string
}

// 获取默认时间范围：最近7天
const getDefaultTimeRange = (): [string, string] => {
  const end = new Date()
  const start = new Date()
  start.setTime(start.getTime() - 3600 * 1000 * 24 * 7)
  const format = (d: Date) => {
    const year = d.getFullYear()
    const month = String(d.getMonth() + 1).padStart(2, '0')
    const day = String(d.getDate()).padStart(2, '0')
    return `${year}-${month}-${day} 00:00:00`
  }
  const endStr = `${end.getFullYear()}-${String(end.getMonth() + 1).padStart(2, '0')}-${String(end.getDate()).padStart(2, '0')} 23:59:59`
  return [format(start), endStr]
}

// 状态
const loading = ref(false)
const tableData = ref<any[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 选项数据
const hazardPointOptions = ref<HazardPointOption[]>([])
const deviceOptions = ref<DeviceOption[]>([])
const deviceAttrsMap = ref<Map<number, DeviceAttr[]>>(new Map())

// 选中的筛选条件 - 默认最近7天
const selectedHazardPointId = ref<number | ''>('')
const selectedDeviceId = ref<number | ''>('')
const selectedAttrCodes = ref<string[]>([])
const timeRange = ref<[string, string] | null>(getDefaultTimeRange())

// 可选的监测属性
const availableAttrs = computed(() => {
  if (!selectedDeviceId.value) return []
  return deviceAttrsMap.value.get(selectedDeviceId.value) || []
})

// 加载隐患点选项
const loadHazardPointOptions = async () => {
  try {
    const res = await getHazardPointPage({ pageNum: 1, pageSize: 1000 })
    const rows = res.data?.rows || []
    hazardPointOptions.value = rows.map((item: any) => ({
      id: item.id,
      name: item.name
    }))
  } catch (error) {
    showRequestErrorMessage(error, '加载隐患点列表失败')
  }
}

// 加载设备选项
const loadDeviceOptions = async () => {
  try {
    const params: any = { pageNum: 1, pageSize: 1000 }
    if (selectedHazardPointId.value) {
      params.boundHazardPointId = selectedHazardPointId.value
    }
    const res = await getDevicePage(params)
    const rows = res.rows || []

    deviceOptions.value = rows.map((item: DeviceItem) => ({
      id: item.id!,
      name: item.name
    }))
  } catch (error) {
    console.error('加载设备选项失败:', error)
    deviceOptions.value = []
  }
}

// 加载设备的监测属性（从该设备的传感器属性列表中提取去重）
const loadDeviceAttrs = async (deviceId: number): Promise<DeviceAttr[]> => {
  try {
    const sensors = await getDeviceSensors(deviceId)
    const seen = new Set<string>()
    const attrs: DeviceAttr[] = []
    for (const sensor of sensors) {
      for (const attr of sensor.attrList) {
        if (!seen.has(attr.attrCode)) {
          seen.add(attr.attrCode)
          attrs.push({
            code: attr.attrCode,
            name: attr.attrName || attr.attrCode,
            unit: attr.unit || ''
          })
        }
      }
    }
    return attrs.length > 0 ? attrs : [{ code: 'value', name: '监测值', unit: '' }]
  } catch {
    return [{ code: 'value', name: '监测值', unit: '' }]
  }
}

// 设备变化时加载其属性
const onDeviceChange = async () => {
  selectedAttrCodes.value = []
  if (selectedDeviceId.value) {
    const attrs = await loadDeviceAttrs(selectedDeviceId.value)
    deviceAttrsMap.value.set(selectedDeviceId.value, attrs)
    // 自动全选所有属性
    selectedAttrCodes.value = attrs.map(a => a.code)
  }
  // 自动查询
  await handleQuery()
}

// 转换监测数据为表格行（将同一时间点的多条属性聚合）
const transformMonitorData = (rows: MonitorDataPageItem[]) => {
  const grouped: Record<string, any> = {}

  rows.forEach(item => {
    const key = `${item.dataTime}_${item.deviceId}_${item.sensorId}`
    if (!grouped[key]) {
      grouped[key] = {
        dataTime: item.dataTime,
        deviceId: item.deviceId,
        deviceName: item.deviceName,
        sensorId: item.sensorId,
        sensorName: item.sensorName,
        dataList: []
      }
    }
    grouped[key].dataList.push({
      attrCode: item.attrCode,
      attrName: item.attrName,
      value: item.value,
      unit: item.unit
    })
  })

  return Object.values(grouped)
}

// 查询监测数据
const handleQuery = async () => {
  if (!selectedDeviceId.value) {
    ElMessage.warning('请选择设备')
    return
  }
  if (!selectedHazardPointId.value) {
    ElMessage.warning('请选择隐患点')
    return
  }

  loading.value = true
  try {
    const baseParams: MonitorDataPageQuery = {
      hazardPointId: selectedHazardPointId.value,
      deviceId: selectedDeviceId.value,
      pageNum: currentPage.value,
      pageSize: pageSize.value
    }
    if (timeRange.value) {
      baseParams.startTime = timeRange.value[0]
      baseParams.endTime = timeRange.value[1]
    }

    const attrCodes = selectedAttrCodes.value.length > 0 ? selectedAttrCodes.value : ['']

    // 逐个属性查询，合并所有结果
    const allRows: MonitorDataPageItem[] = []
    let mergedTotal = 0
    for (const attrCode of attrCodes) {
      const res = await getMonitorDataPage({ ...baseParams, attrCode: attrCode || undefined })
      allRows.push(...(res.rows || []))
      mergedTotal = Math.max(mergedTotal, res.total || 0)
    }

    const transformedData = transformMonitorData(allRows)
    tableData.value = transformedData
    total.value = mergedTotal
  } catch (error) {
    showRequestErrorMessage(error, '查询失败')
  } finally {
    loading.value = false
  }
}

// 事件处理
const onHazardPointChange = async () => {
  selectedDeviceId.value = ''
  selectedAttrCodes.value = []
  deviceOptions.value = []
  deviceAttrsMap.value.clear()
  tableData.value = []
  total.value = 0
  // 重置时保留默认时间范围
  timeRange.value = getDefaultTimeRange()
  await loadDeviceOptions()
}

const handleReset = () => {
  selectedHazardPointId.value = ''
  selectedDeviceId.value = ''
  selectedAttrCodes.value = []
  timeRange.value = getDefaultTimeRange()
  currentPage.value = 1
  tableData.value = []
  total.value = 0
  deviceOptions.value = []
  deviceAttrsMap.value.clear()
}

const EXPORT_MAX = 20000

const handleExportCsv = async () => {
  if (!selectedDeviceId.value || !selectedHazardPointId.value) {
    ElMessage.warning('请先选择隐患点和设备并查询')
    return
  }
  if (total.value === 0) {
    ElMessage.warning('没有数据可导出')
    return
  }
  if (total.value > EXPORT_MAX) {
    ElMessage.warning(`数据量过大（${total.value} 条，上限 ${EXPORT_MAX} 条），请缩小查询范围后重试`)
    return
  }

  // 拉取全部数据（分页循环，多选属性时分别请求合并）
  const allRows: MonitorDataPageItem[] = []
  const fetchPageSize = 500
  const totalPages = Math.ceil(total.value / fetchPageSize)
  const attrCodes = selectedAttrCodes.value.length > 0 ? selectedAttrCodes.value : ['']
  loading.value = true
  try {
    for (let p = 1; p <= totalPages; p++) {
      for (const attrCode of attrCodes) {
      const params: MonitorDataPageQuery = {
        hazardPointId: selectedHazardPointId.value,
        deviceId: selectedDeviceId.value,
        pageNum: p,
          pageSize: fetchPageSize,
          attrCode: attrCode || undefined
      }
      if (timeRange.value) {
        params.startTime = timeRange.value[0]
        params.endTime = timeRange.value[1]
      }
      const res = await getMonitorDataPage(params)
      allRows.push(...(res.rows || []))
    }
    }
  } catch (error) {
    showRequestErrorMessage(error, '导出数据拉取失败')
    loading.value = false
    return
  }
  loading.value = false

  const exportRows = transformMonitorData(allRows)

  // 构建CSV数据（与查询表格一致：时间 | 设备名称 | 传感器 | 监测数据）
  const headers = ['时间', '设备名称', '传感器', '监测数据']
  const rows = exportRows.map(row => {
    const monitorDataStr = row.dataList.map((item: any) =>
      `${item.attrName}: ${item.value}${item.unit}`
    ).join('; ')
    return [row.dataTime, row.deviceName, row.sensorName, monitorDataStr]
  })

  const csv = '﻿' + [headers.join(','), ...rows.map(r => r.join(','))].join('\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `监测数据查询_${new Date().toISOString().slice(0, 10)}.csv`
  a.click()
  URL.revokeObjectURL(url)
}

onMounted(() => {
  loadHazardPointOptions()
  loadDeviceOptions()
})
</script>

<style scoped>
.empty-text {
  color: #909399;
}

.monitor-data-item {
  line-height: 1.8;
  font-size: 13px;
  padding: 2px 0;
}

.table-wrap {
  width: 100%;
  overflow-x: auto;
}

:deep(.el-table) {
  width: 100%;
  table-layout: auto;
}

:deep(.el-table__header-wrapper) {
  overflow: visible;
}

:deep(.el-table__body-wrapper) {
  overflow-x: auto;
}

/* 监测数据属性分割线 */
.monitor-data-item {
  border-bottom: 1px dashed #e8e8e8;
  padding: 6px 0;
}

.monitor-data-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.monitor-data-item:first-child {
  padding-top: 0;
}
</style>