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
          v-model="selectedGroupId"
          placeholder="隐患分组"
          clearable
          @change="onGroupChange"
      >
        <el-option
            v-for="g in groupOptions"
            :key="g.id"
            :label="g.name"
            :value="g.id"
        />
      </el-select>

      <el-select
          v-model="selectedHazardPointId"
          placeholder="隐患点"
          clearable
          filterable
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
          filterable
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
            :span-method="tableSpanMethod"
            :cell-class-name="tableCellClassName"
            border
            stripe
            v-loading="loading"
        >
          <el-table-column prop="groupName" label="隐患分组" width="140" align="center" show-overflow-tooltip />
          <el-table-column prop="hazardPointName" label="隐患点" width="160" align="center" show-overflow-tooltip />
          <el-table-column prop="dataTime" label="时间" width="170" align="center" />
          <el-table-column prop="deviceName" label="设备名称" width="180" align="center" />
          <el-table-column prop="sensorName" label="传感器" width="180" align="center" />
          <el-table-column prop="attrName" label="监测指标" width="130" align="center" />
          <el-table-column label="监测值" min-width="140" align="center">
            <template #default="{ row }">
              {{ row.value != null ? row.value : '-' }}{{ row.unit ? ' ' + row.unit : '' }}
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
import { getHazardPointPage, getHazardPointGroups } from '@/api/hazardPoint'
import { getDevicePage, type DeviceItem } from '@/api/device'
import { getMonitorDataPage, type MonitorDataPageItem } from '@/api/monitorData'
import { getDeviceSensors } from '@/api/sensor'

// 类型定义
interface HazardPointOption {
  id: number
  name: string
  groupName: string
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

interface GroupOption {
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
const groupOptions = ref<GroupOption[]>([])
const allDevices = ref<DeviceItem[]>([])            // 全量设备（用于客户端级联过滤）
const deviceAttrsMap = ref<Map<number, DeviceAttr[]>>(new Map())

// 所有隐患点的完整映射（ID → 名称/分组/分组ID），一次性全量加载，不受分组过滤和分页限制影响
const allHazardPointMap = ref<Map<number, { name: string; groupName: string; groupId: number }>>(new Map())

// ── 级联下拉选项（全部 computed，响应式自动过滤）──

// 隐患点：根据选中分组过滤
const hazardPointOptions = computed<HazardPointOption[]>(() => {
  const result: HazardPointOption[] = []
  allHazardPointMap.value.forEach((info, id) => {
    if (!selectedGroupId.value || info.groupId === (selectedGroupId.value as number)) {
      result.push({ id, name: info.name, groupName: info.groupName })
    }
  })
  return result
})

// 设备：根据 分组/隐患点 级联过滤
const deviceOptions = computed<DeviceOption[]>(() => {
  let list = allDevices.value

  if (selectedHazardPointId.value) {
    // 选中具体隐患点 → 只显示已绑定的设备
    list = list.filter(d => d.boundHazardPointId === (selectedHazardPointId.value as number))
  } else if (selectedGroupId.value) {
    // 只选中分组 → 显示该分组下所有隐患点的设备
    const groupHpIds = new Set<number>()
    allHazardPointMap.value.forEach((info, id) => {
      if (info.groupId === (selectedGroupId.value as number)) groupHpIds.add(id)
    })
    list = list.filter(d => d.boundHazardPointId != null && groupHpIds.has(d.boundHazardPointId))
  }
  // 都没选 → 显示全部设备

  return list.map(d => ({ id: d.id!, name: d.name }))
})

// 选中的筛选条件 - 默认最近7天
const selectedGroupId = ref<number | ''>('')
const selectedHazardPointId = ref<number | ''>('')
const selectedDeviceId = ref<number | ''>('')
const selectedAttrCodes = ref<string[]>([])
const timeRange = ref<[string, string] | null>(getDefaultTimeRange())

// 可选的监测属性
const availableAttrs = computed(() => {
  if (!selectedDeviceId.value) return []
  return deviceAttrsMap.value.get(selectedDeviceId.value) || []
})

// 加载隐患分组选项
const loadGroupOptions = async () => {
  try {
    const response = await getHazardPointGroups()
    groupOptions.value = (response.data || []).map((g: any) => ({
      id: g.id,
      name: g.name
    }))
  } catch (error) {
    console.error('加载隐患分组失败:', error)
  }
}

// 加载全部隐患点（一次性全量，用于名称/分组查找，不受分组过滤和分页限制）
const loadAllHazardPoints = async () => {
  try {
    const res = await getHazardPointPage({ pageNum: 1, pageSize: 100000 })
    const rows = res.data?.rows || []
    const map = new Map<number, { name: string; groupName: string; groupId: number }>()
    rows.forEach((item: any) => {
      map.set(item.id, {
        name: item.name,
        groupName: item.groupName || '',
        groupId: item.groupId
      })
    })
    allHazardPointMap.value = map
  } catch (error) {
    console.error('加载隐患点列表失败:', error)
  }
}

// 加载全量设备（用于客户端级联过滤，仅首次加载）
const loadAllDevices = async () => {
  try {
    const res = await getDevicePage({ pageNum: 1, pageSize: 10000 })
    allDevices.value = res.rows || []
  } catch (error) {
    console.error('加载设备列表失败:', error)
    allDevices.value = []
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

// 设备变化时加载其属性（仅加载属性选项，不自动查询）
const onDeviceChange = async () => {
  selectedAttrCodes.value = []
  if (selectedDeviceId.value) {
    const attrs = await loadDeviceAttrs(selectedDeviceId.value)
    deviceAttrsMap.value.set(selectedDeviceId.value, attrs)
    selectedAttrCodes.value = attrs.map(a => a.code)
  }
}

// 合并的列（从左到右，越靠左越"父级"）
const SPAN_COLS = ['groupName', 'hazardPointName', 'dataTime', 'deviceName', 'sensorName']

// 转换 API 行为表格行：按时间降序排列，填充分组名
const buildFlatTableData = (rows: MonitorDataPageItem[]) => {
  return rows
    .map(item => {
      const hpInfo = allHazardPointMap.value.get(item.hazardPointId)
      return {
        groupName: hpInfo?.groupName || '',
        groupId: hpInfo?.groupId,
        hazardPointName: item.hazardPointName || hpInfo?.name || '',
        dataTime: item.dataTime,
        deviceName: item.deviceName,
        sensorName: item.sensorName,
        attrName: item.attrName,
        value: item.value,
        unit: item.unit,
      }
    })
    .sort((a, b) => {
      if (a.dataTime !== b.dataTime) return b.dataTime.localeCompare(a.dataTime)
      if (a.groupName !== b.groupName) return a.groupName.localeCompare(b.groupName)
      if (a.hazardPointName !== b.hazardPointName) return a.hazardPointName.localeCompare(b.hazardPointName)
      if (a.deviceName !== b.deviceName) return a.deviceName.localeCompare(b.deviceName)
      if (a.sensorName !== b.sensorName) return a.sensorName.localeCompare(b.sensorName)
      return (a.attrName || '').localeCompare(b.attrName || '')
    })
}

// span-method：即时计算，不预存 _rowspan
const tableSpanMethod = ({ row, column, rowIndex }: any) => {
  const prop = column.property as string
  const colIdx = SPAN_COLS.indexOf(prop)
  if (colIdx === -1) return { rowspan: 1, colspan: 1 }

  const data = tableData.value

  // 如果上一行所有父级列（含当前列）值都相同，则本行被合并到上一行
  if (rowIndex > 0) {
    const prev = data[rowIndex - 1]
    let same = true
    for (let c = 0; c <= colIdx; c++) {
      if (prev[SPAN_COLS[c]] !== row[SPAN_COLS[c]]) { same = false; break }
    }
    if (same) return { rowspan: 0, colspan: 0 }
  }

  // 本行是新分组的起点，向后数连续行数
  let span = 1
  for (let i = rowIndex + 1; i < data.length; i++) {
    let same = true
    for (let c = 0; c <= colIdx; c++) {
      if (data[i][SPAN_COLS[c]] !== row[SPAN_COLS[c]]) { same = false; break }
    }
    if (same) span++
    else break
  }

  return { rowspan: span, colspan: 1 }
}

// cell-class-name：合并组首行加底部粗线，组内续行弱化上边框
const tableCellClassName = ({ row, column, rowIndex }: any) => {
  const prop = column.property as string
  const colIdx = SPAN_COLS.indexOf(prop)
  if (colIdx === -1) return ''

  const data = tableData.value

  // 判断本行是否是该列的新分组起点（与上一行任何父级列不同即为新分组）
  if (rowIndex === 0) return 'cell-group-start'

  const prev = data[rowIndex - 1]
  for (let c = 0; c <= colIdx; c++) {
    if (prev[SPAN_COLS[c]] !== row[SPAN_COLS[c]]) return 'cell-group-start'
  }

  // 续行
  return 'cell-group-next'
}

// 查询监测数据
const handleQuery = async () => {
  loading.value = true
  try {
    const baseParams: any = {
      pageNum: currentPage.value,
      pageSize: pageSize.value
    }
    if (selectedDeviceId.value) {
      baseParams.deviceId = selectedDeviceId.value
    }
    if (timeRange.value) {
      baseParams.startTime = timeRange.value[0]
      baseParams.endTime = timeRange.value[1]
    }

    // 确定要查询的隐患点ID列表
    let hazardPointIds: number[] = []
    if (selectedHazardPointId.value) {
      // 选了具体隐患点 → 只查这一个
      hazardPointIds = [selectedHazardPointId.value as number]
    } else if (selectedGroupId.value) {
      // 只选了分组没选隐患点 → 查该分组下所有隐患点（上限避免过多 API 调用）
      const MAX_HP_PER_GROUP = 20
      allHazardPointMap.value.forEach((info, id) => {
        if (info.groupId === (selectedGroupId.value as number)) {
          hazardPointIds.push(id)
        }
      })
      if (hazardPointIds.length > MAX_HP_PER_GROUP) {
        hazardPointIds = hazardPointIds.slice(0, MAX_HP_PER_GROUP)
      }
    }
    // 都没选 → hazardPointIds 为空，走全局搜索（不传 hazardPointId）

    const attrCodes = selectedAttrCodes.value.length > 0 ? selectedAttrCodes.value : ['']

    const allRows: MonitorDataPageItem[] = []
    let serverTotal = 0

    // 对每个隐患点+属性组合分别查询并合并
    const hpIdsToQuery = hazardPointIds.length > 0 ? hazardPointIds : [undefined]
    for (const hpId of hpIdsToQuery) {
      const params = { ...baseParams }
      if (hpId !== undefined) {
        params.hazardPointId = hpId
      }
      for (const attrCode of attrCodes) {
        const res = await getMonitorDataPage({ ...params, attrCode: attrCode || undefined })
        allRows.push(...(res.rows || []))
        serverTotal += (res.total || 0)
      }
    }

    const flatData = buildFlatTableData(allRows)
    // 客户端二次过滤：按选中分组过滤
    if (selectedGroupId.value) {
      tableData.value = flatData.filter(row => row.groupId === (selectedGroupId.value as number))
    } else {
      tableData.value = flatData
    }
    // 分页总数：多属性/多隐患点查询时累计各次 API 返回的 total
    total.value = serverTotal
  } catch (error) {
    showRequestErrorMessage(error, '查询失败')
  } finally {
    loading.value = false
  }
}

// 分组变更：隐患点/设备下拉 computed 自动过滤，清除下游已失效的选择
const onGroupChange = () => {
  const prevHpId = selectedHazardPointId.value
  const prevDeviceId = selectedDeviceId.value

  if (!selectedGroupId.value) {
    // 分组已清除 → 重置下游（设备选项 computed 自动恢复全量）
    selectedHazardPointId.value = ''
    selectedDeviceId.value = ''
    selectedAttrCodes.value = []
    deviceAttrsMap.value.clear()
    return
  }

  // 检查当前隐患点是否仍在过滤后的选项中
  if (prevHpId && hazardPointOptions.value.some(hp => hp.id === prevHpId)) {
    // 隐患点仍有效，但设备列表可能因级联变化 → 检查当前设备是否仍有效
    if (prevDeviceId && !deviceOptions.value.some(d => d.id === prevDeviceId)) {
      selectedDeviceId.value = ''
      selectedAttrCodes.value = []
      deviceAttrsMap.value.clear()
    }
    return
  }

  // 隐患点已失效 → 清除隐患点及下游
  selectedHazardPointId.value = ''
  selectedDeviceId.value = ''
  selectedAttrCodes.value = []
  deviceAttrsMap.value.clear()
}

// 隐患点变更：设备选项 computed 自动过滤，清除下游已失效的选择（不自动查询）
const onHazardPointChange = async () => {
  const prevDeviceId = selectedDeviceId.value
  selectedDeviceId.value = ''
  selectedAttrCodes.value = []
  deviceAttrsMap.value.clear()

  if (!selectedHazardPointId.value) {
    return
  }

  // 如果之前选的设备仍在新列表中，恢复选择
  if (prevDeviceId && deviceOptions.value.some(d => d.id === prevDeviceId)) {
    selectedDeviceId.value = prevDeviceId
    const attrs = await loadDeviceAttrs(prevDeviceId)
    deviceAttrsMap.value.set(prevDeviceId, attrs)
    selectedAttrCodes.value = attrs.map(a => a.code)
  }
}

const handleReset = () => {
  selectedGroupId.value = ''
  selectedHazardPointId.value = ''
  selectedDeviceId.value = ''
  selectedAttrCodes.value = []
  timeRange.value = getDefaultTimeRange()
  currentPage.value = 1
  tableData.value = []
  total.value = 0
  deviceAttrsMap.value.clear()
}

const EXPORT_MAX = 20000

const handleExportCsv = async () => {
  if (tableData.value.length === 0) {
    ElMessage.warning('没有数据可导出')
    return
  }

  const allRows: MonitorDataPageItem[] = []
  const fetchPageSize = 1000
  const attrCodes = selectedAttrCodes.value.length > 0 ? selectedAttrCodes.value : ['']
  loading.value = true

  try {
    // 循环拉取直到数据耗尽或达到上限
    let page = 1
    let hasMore = true
    while (hasMore && allRows.length < EXPORT_MAX) {
      let maxPageRows = 0
      for (const attrCode of attrCodes) {
        const params: any = {
          pageNum: page,
          pageSize: fetchPageSize,
          attrCode: attrCode || undefined,
        }
        if (selectedHazardPointId.value) params.hazardPointId = selectedHazardPointId.value
        if (selectedDeviceId.value) params.deviceId = selectedDeviceId.value
        if (timeRange.value) {
          params.startTime = timeRange.value[0]
          params.endTime = timeRange.value[1]
        }

        const res = await getMonitorDataPage(params)
        const rows = res.rows || []
        allRows.push(...rows)
        maxPageRows = Math.max(maxPageRows, rows.length)
      }

      // 所有 attrCode 查询都不足一页 → 数据已耗尽
      if (maxPageRows < fetchPageSize) {
        hasMore = false
      } else {
        page++
      }
    }
  } catch (error) {
    showRequestErrorMessage(error, '导出数据拉取失败')
    loading.value = false
    return
  }
  loading.value = false

  if (allRows.length === 0) {
    ElMessage.warning('没有数据可导出')
    return
  }

  if (allRows.length >= EXPORT_MAX) {
    ElMessage.warning(`数据量已达导出上限（${EXPORT_MAX} 条），结果已截断`)
  }

  const exportRows = buildFlatTableData(allRows.slice(0, EXPORT_MAX))

  // CSV：隐患分组 | 隐患点 | 时间 | 设备名称 | 传感器 | 监测指标 | 监测值
  const headers = ['隐患分组', '隐患点', '时间', '设备名称', '传感器', '监测指标', '监测值']
  const rows = exportRows.map(row =>
    [row.groupName, row.hazardPointName, row.dataTime, row.deviceName, row.sensorName, row.attrName, `${row.value != null ? row.value : '-'}${row.unit ? ' ' + row.unit : ''}`]
  )

  const csv = '﻿' + [headers.join(','), ...rows.map(r => r.join(','))].join('\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `监测数据查询_${new Date().toISOString().slice(0, 10)}.csv`
  a.click()
  URL.revokeObjectURL(url)
  ElMessage.success(`已导出 ${rows.length} 条数据`)
}

onMounted(() => {
  loadGroupOptions()
  loadAllHazardPoints()
  loadAllDevices()
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

/* 合并单元格分组底部加粗分隔 */
:deep(.cell-group-start) {
  border-bottom: 3px solid #303133 !important;
}

/* 表格全局边框加粗 */
:deep(.el-table td) {
  border-right: 2px solid #dcdfe6 !important;
  border-bottom: 2px solid #dcdfe6 !important;
}

:deep(.el-table th) {
  border-right: 2px solid #dcdfe6 !important;
  border-bottom: 2px solid #dcdfe6 !important;
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