<template>
  <div class="page">
    <div class="header">
      <div class="header__left">
        <h2 class="header__title">历史告警</h2>
        <span class="header__subtitle">已处置告警记录归档与追溯</span>
      </div>
      <div class="header__right">
        <el-button type="info" @click="handleExport">
          <el-icon><Download /></el-icon>
          导出
        </el-button>
      </div>
    </div>

    <div class="search">
      <el-input v-model="queryParams.hazardPointName" placeholder="隐患点名称" clearable />
      <el-input v-model="queryParams.personName" placeholder="人员名称" clearable />
      <el-date-picker
        v-model="queryParams.alarmTimeRange"
        type="daterange"
        range-separator=""
        start-placeholder="告警时间:开始"
        end-placeholder="告警时间:结束"
        value-format="YYYY-MM-DD"
      />
      <el-select v-model="queryParams.alarmLevel" placeholder="告警等级" clearable multiple collapse-tags collapse-tags-tooltip>
        <el-option label="一级" value="1" />
        <el-option label="二级" value="2" />
        <el-option label="三级" value="3" />
        <el-option label="四级" value="4" />
      </el-select>
      <el-select v-model="queryParams.alarmType" placeholder="告警类型" clearable multiple collapse-tags collapse-tags-tooltip>
        <el-option label="阈值预警" value="THRESHOLD" />
        <el-option label="综合预警" value="COMPREHENSIVE" />
      </el-select>
      <el-select v-model="queryParams.status" placeholder="警情状态" clearable multiple collapse-tags collapse-tags-tooltip>
        <el-option label="误报" value="4" />
        <el-option label="已销警" value="3" />
      </el-select>
      <el-button type="primary" @click="handleQuery">查询</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>

    <div class="table-wrap">
      <div class="table-wrap__scroll">
        <el-table
          :data="tableData"
          style="width: 100%"
          @row-click="handleRowClick"
          border
          stripe
        >
          <el-table-column prop="hazardPointName" label="隐患点名称" min-width="160" />
          <el-table-column prop="alarmLevel" label="告警等级" width="90">
            <template #default="{ row }">
              <el-tag :style="getAlarmLevelStyle(row.alarmLevel)">{{ getAlarmLevelText(row.alarmLevel) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="firstTriggerTime" label="首次告警时间" min-width="160" />
          <el-table-column prop="lastTriggerTime" label="最后告警时间" min-width="160" />
          <el-table-column prop="triggerCount" label="告警次数" width="90">
            <template #default="{ row }">
              <span class="alarm-count" @click.stop="handleView(row)">{{ row.triggerCount }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="alarmType" label="告警类型" width="100">
            <template #default="{ row }">
              {{ getAlarmTypeText(row.alarmType) }}
            </template>
          </el-table-column>
          <el-table-column prop="status" label="警情状态" width="90">
            <template #default="{ row }">
              <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="resolvedBy" label="响应人员" width="100" />
          <el-table-column prop="resolvedAt" label="响应时间" min-width="160" />
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" text size="small" @click.stop="handleView(row)">
                <el-icon><View /></el-icon>
                查看
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="table-wrap__pagination">
        <el-pagination
          v-model:current-page="pagination.currentPage"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>

    <!-- 告警详情弹窗 -->
    <AlarmDetailDialog v-model="detailDialogVisible" :data="currentRow" />
  </div>
</template>

<script setup lang="ts">
import {onMounted, reactive, ref} from 'vue'
import {ElMessage} from 'element-plus'
import {Download, View} from '@element-plus/icons-vue'
import AlarmDetailDialog from './components/AlarmDetailDialog.vue'
import {getHistoryAlarms, getAlarmLevelStyle} from '@/api/alarm'

// 查询参数
const queryParams = reactive({
  hazardPointName: '',
  personName: '',
  alarmTimeRange: [] as string[],
  alarmCountMin: null as number | null,
  alarmCountMax: null as number | null,
  alarmLevel: [] as string[],
  alarmType: [] as string[],
  status: [] as string[]
})

// 分页
const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0
})

// 表格数据
const tableData = ref<any[]>([])

// 弹窗
const detailDialogVisible = ref(false)

// 当前行
const currentRow = ref<any>(null)

const loading = ref(false)

const fetchData = async () => {
  loading.value = true
  try {
    const params: Record<string, unknown> = {
      pageNum: pagination.currentPage,
      pageSize: pagination.pageSize
    }
    if (queryParams.hazardPointName) params.hazardPointName = queryParams.hazardPointName
    if (queryParams.personName) params.personName = queryParams.personName
    if (queryParams.alarmLevel.length > 0) params.alarmLevels = queryParams.alarmLevel.join(',')
    if (queryParams.alarmType.length > 0) params.alarmTypes = queryParams.alarmType.join(',')
    if (queryParams.status.length > 0) params.statusList = queryParams.status.join(',')
    if (queryParams.alarmTimeRange.length === 2) {
      params.startTime = queryParams.alarmTimeRange[0]
      params.endTime = queryParams.alarmTimeRange[1]
    }
    const res = await getHistoryAlarms(params as any)
    tableData.value = res.rows || []
    pagination.total = res.total || 0
  } finally {
    loading.value = false
  }
}

// 原始 mock 数据 — 已替换为 API

// 初始化
onMounted(() => {
  fetchData()
})

// 获取告警等级文本
const getAlarmLevelText = (level: number | string) => {
  const map: Record<string, string> = {
    '1': '一级',
    '2': '二级',
    '3': '三级',
    '4': '四级'
  }
  return map[String(level)] || String(level)
}

// 获取告警类型文本
const getAlarmTypeText = (type: string) => {
  const map: Record<string, string> = {
    'THRESHOLD': '阈值预警',
    'COMPREHENSIVE': '综合预警'
  }
  return map[type] || type
}

// 获取状态类型
const getStatusType = (status: number) => {
  const map: Record<number, string> = {
    3: 'info',
    4: 'info'
  }
  return map[status] || 'info'
}

// 获取状态文本
const getStatusText = (status: number) => {
  const map: Record<number, string> = {
    3: '已销警',
    4: '误报'
  }
  return map[status] || String(status)
}

// 查询
const handleQuery = () => {
  pagination.currentPage = 1
  fetchData()
}

// 重置
const handleReset = () => {
  queryParams.hazardPointName = ''
  queryParams.personName = ''
  queryParams.alarmTimeRange = []
  queryParams.alarmCountMin = null
  queryParams.alarmCountMax = null
  queryParams.alarmLevel = []
  queryParams.alarmType = []
  queryParams.status = []
  pagination.currentPage = 1
  fetchData()
}

// 行点击 - 查看详情
const handleRowClick = (row: any) => {
  currentRow.value = row
  detailDialogVisible.value = true
}

// 查看详情
const handleView = (row: any) => {
  currentRow.value = row
  detailDialogVisible.value = true
}

// 导出
const handleExport = () => {
  ElMessage.success('导出功能已触发（模拟）')
}

// 分页大小变化
const handleSizeChange = (size: number) => {
  pagination.pageSize = size
  pagination.currentPage = 1
  fetchData()
}

// 分页页码变化
const handleCurrentChange = (page: number) => {
  pagination.currentPage = page
  fetchData()
}
</script>

<style scoped>
.alarm-count {
  color: #409eff;
  cursor: pointer;
  text-decoration: underline;
}

.alarm-count:hover {
  color: #66b1ff;
}
</style>