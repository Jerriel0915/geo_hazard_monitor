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
        v-model="selectedDeviceType"
        placeholder="设备类型"
        clearable
        @change="onDeviceTypeChange"
      >
        <el-option
          v-for="dt in deviceTypeOptions"
          :key="dt.value"
          :label="dt.label"
          :value="dt.value"
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
        type="datetimerange"
        range-separator=""
        start-placeholder="开始时间"
        end-placeholder="结束时间"
        value-format="YYYY-MM-DD HH:mm:ss"
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
          <el-table-column prop="time" label="时间" min-width="180" align="center" />
          <el-table-column prop="deviceName" label="设备名称" width="150" align="center" />
          <el-table-column
            v-for="col in dynamicColumns"
            :key="col.code"
            :prop="col.code"
            :label="`${col.name}(${col.unit})`"
            min-width="140"
            align="center"
          />
          <el-table-column
            v-if="dynamicColumns.length === 0"
            label="监测数据"
            align="center"
          >
            <template #default>
              <span class="empty-text">请选择设备类型</span>
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
import {computed, onMounted, ref} from 'vue'
import {ElMessage} from 'element-plus'
import {showRequestErrorMessage} from '@/utils/errorHandler'
import {
  type DeviceOption,
  type DeviceTypeOption,
  getDeviceOptions,
  getDeviceTypeOptions,
  getHazardPointOptions,
  getMonitorQueryData,
  type HazardPointOption
} from '@/api/report'

const loading = ref(false)
const tableData = ref<Record<string, any>[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// Filter options
const hazardPointOptions = ref<HazardPointOption[]>([])
const deviceTypeOptions = ref<DeviceTypeOption[]>([])
const deviceOptions = ref<DeviceOption[]>([])

// Selected filters
const selectedHazardPointId = ref<number | ''>('')
const selectedDeviceType = ref<number | ''>('')
const selectedDeviceId = ref<number | ''>('')
const selectedAttrCodes = ref<string[]>([])
const timeRange = ref<[string, string] | null>(null)

// Dynamic columns computed from selected device type
const dynamicColumns = computed(() => {
  if (!selectedDeviceType.value) return []
  const dt = deviceTypeOptions.value.find(d => d.value === selectedDeviceType.value)
  return dt?.attrs || []
})

// Available attrs for the multi-select (filtered by selectedAttrCodes)
const availableAttrs = computed(() => {
  if (!selectedDeviceType.value) return []
  const dt = deviceTypeOptions.value.find(d => d.value === selectedDeviceType.value)
  return dt?.attrs || []
})

// Cascading logic
const onHazardPointChange = async () => {
  selectedDeviceType.value = ''
  selectedDeviceId.value = ''
  selectedAttrCodes.value = []
  deviceOptions.value = []
  await loadDeviceOptions()
}

const onDeviceTypeChange = async () => {
  selectedDeviceId.value = ''
  selectedAttrCodes.value = []
  // Auto-select all attrs for this device type
  if (selectedDeviceType.value) {
    const dt = deviceTypeOptions.value.find(d => d.value === selectedDeviceType.value)
    selectedAttrCodes.value = dt?.attrs.map(a => a.code) || []
  }
  await loadDeviceOptions()
}

const onDeviceChange = () => {
  // attrs stay based on device type, not individual device
}

// Load options
const loadOptions = async () => {
  try {
    const [hps, dts] = await Promise.all([
      getHazardPointOptions(),
      getDeviceTypeOptions()
    ])
    hazardPointOptions.value = hps
    deviceTypeOptions.value = dts
  } catch (error) {
    showRequestErrorMessage(error, '加载选项失败')
  }
}

const loadDeviceOptions = async () => {
  try {
    const devices = await getDeviceOptions({
      hazardPointId: selectedHazardPointId.value || undefined,
      deviceType: selectedDeviceType.value || undefined
    })
    deviceOptions.value = devices
  } catch (error) {
    console.error('加载设备选项失败:', error)
    deviceOptions.value = []
  }
}

const handleQuery = async () => {
  if (!selectedDeviceType.value) {
    ElMessage.warning('请至少选择设备类型')
    return
  }
  loading.value = true
  try {
    const data = await getMonitorQueryData({
      hazardPointId: selectedHazardPointId.value,
      deviceType: selectedDeviceType.value,
      deviceId: selectedDeviceId.value,
      attrCodes: selectedAttrCodes.value,
      startTime: timeRange.value?.[0] || undefined,
      endTime: timeRange.value?.[1] || undefined,
      pageNum: currentPage.value,
      pageSize: pageSize.value
    })
    tableData.value = data.rows || []
    total.value = data.total || 0
  } catch (error) {
    showRequestErrorMessage(error, '查询失败')
  } finally {
    loading.value = false
  }
}

const handleReset = () => {
  selectedHazardPointId.value = ''
  selectedDeviceType.value = ''
  selectedDeviceId.value = ''
  selectedAttrCodes.value = []
  timeRange.value = null
  currentPage.value = 1
  tableData.value = []
  total.value = 0
  deviceOptions.value = []
}

const handleExportCsv = () => {
  if (!tableData.value.length) {
    ElMessage.warning('没有数据可导出')
    return
  }
  // Build header
  const headers = ['时间', '设备名称', ...dynamicColumns.value.map(c => `${c.name}(${c.unit})`)]
  // Build rows
  const rows = tableData.value.map(row => [
    row.time,
    row.deviceName,
    ...dynamicColumns.value.map(c => row[c.code] ?? '')
  ])
  // Join with comma, add BOM for Chinese
  const csv = '\uFEFF' + [headers.join(','), ...rows.map(r => r.join(','))].join('\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `监测数据查询_${new Date().toISOString().slice(0, 10)}.csv`
  a.click()
  URL.revokeObjectURL(url)
}

onMounted(() => {
  loadOptions()
})
</script>

