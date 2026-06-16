<!--代办告警 -->
<template>
  <div class="page">
    <div class="header">
      <div class="header__left">
        <h2 class="header__title">待办告警</h2>
        <span class="header__subtitle">实时告警工单处理与处置追踪</span>
      </div>
      <div class="header__right">
        <el-button type="success" :disabled="selectedRows.length === 0" @click="handleBatchFeedback">
          <el-icon><ChatDotRound /></el-icon>
          批量反馈
        </el-button>
        <el-button type="warning" :disabled="selectedRows.length === 0" @click="handleBatchFalseAlarm">
          <el-icon><Warning /></el-icon>
          批量误报
        </el-button>
        <el-button type="danger" :disabled="selectedRows.length === 0" @click="handleBatchCloseAlarm">
          <el-icon><CircleClose /></el-icon>
          批量销警
        </el-button>
        <el-button type="info" @click="handleExport">
          <el-icon><Download /></el-icon>
          导出
        </el-button>
      </div>
    </div>

    <div class="search">
      <el-input v-model="queryParams.hazardPointName" placeholder="隐患点名称" clearable class="search__input" />
      <el-input v-model="queryParams.personName" placeholder="人员名称" clearable class="search__input" />
      <el-date-picker
          v-model="queryParams.alarmTimeRange"
          type="daterange"
          range-separator=""
          start-placeholder="告警时间:开始"
          end-placeholder="告警时间:结束"
          value-format="YYYY-MM-DD"
      />
      <el-select v-model="queryParams.alarmLevel" placeholder="告警等级" clearable multiple class="search__select">
        <el-option label="一级" value="1" />
        <el-option label="二级" value="2" />
        <el-option label="三级" value="3" />
        <el-option label="四级" value="4" />
      </el-select>
      <el-select v-model="queryParams.alarmType" placeholder="告警类型" clearable multiple class="search__select">
        <el-option label="阈值预警" value="THRESHOLD" />
        <el-option label="综合预警" value="COMPREHENSIVE" />
      </el-select>
      <el-select v-model="queryParams.status" placeholder="警情状态" clearable multiple class="search__select">
        <el-option label="待处理" value="1"/>
        <el-option label="处理中" value="2"/>
      </el-select>
      <el-button type="primary" @click="handleQuery">查询</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>

    <div class="table-wrap">
      <div class="table-wrap__scroll">
        <el-table
            :data="tableData"
            style="width: 100%"
            @selection-change="handleSelectionChange"
            @row-click="handleRowClick"
            border
            stripe
        >
          <el-table-column type="selection" width="55" />
          <el-table-column prop="hazardPointName" label="隐患点名称" min-width="180" />
          <el-table-column prop="alarmLevel" label="告警等级" width="100">
            <template #default="{ row }">
              <el-tag :type="getAlarmLevelType(row.alarmLevel)">{{ getAlarmLevelText(row.alarmLevel) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="firstTriggerTime" label="首次告警时间" width="180" />
          <el-table-column prop="lastTriggerTime" label="最后告警时间" width="180" />
          <el-table-column prop="triggerCount" label="告警次数" width="100">
            <template #default="{ row }">
              <span class="alarm-count" @click.stop="handleView(row)">{{ row.triggerCount }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="alarmType" label="告警类型" width="120">
            <template #default="{ row }">
              {{ getAlarmTypeText(row.alarmType) }}
            </template>
          </el-table-column>
          <el-table-column prop="status" label="警情状态" width="100">
            <template #default="{ row }">
              <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="resolvedBy" label="响应人员" width="120" />
          <el-table-column prop="resolvedAt" label="响应时间" min-width="180" />
          <el-table-column label="操作" width="160" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" text size="small" @click.stop="handleView(row)">
                <el-icon><View /></el-icon>
                查看
              </el-button>
              <el-button type="success" text size="small" @click.stop="handleFeedback(row)">
                <el-icon><ChatDotRound /></el-icon>
                处置
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

    <!-- 反馈弹窗 -->
    <FeedbackDialog
        v-model="feedbackDialogVisible"
        :data="currentRow"
        @submit="handleFeedbackSubmit"
        @update:model-value="feedbackDialogVisible = $event"
    />

    <!-- 批量反馈弹窗 - FeedBack组件 -->
    <FeedBack v-model:visible="batchFeedbackVisible" @submit="handleBatchFeedbackSubmit" />

    <!-- 误报确认弹窗 -->
    <el-dialog v-model="falseAlarmDialogVisible" title="误报确认" width="500px">
      <p>确定将选中的告警标记为误报吗？</p>
      <template #footer>
        <el-button @click="falseAlarmDialogVisible = false">取消</el-button>
        <el-button type="warning" @click="confirmFalseAlarm">确定</el-button>
      </template>
    </el-dialog>

    <!-- 销警确认弹窗 -->
    <el-dialog v-model="closeAlarmDialogVisible" title="销警确认" width="500px">
      <p>确定要销警吗？</p>
      <template #footer>
        <el-button @click="closeAlarmDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmCloseAlarm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import {onMounted, reactive, ref} from 'vue'
import {ElMessage} from 'element-plus'
import {ChatDotRound, CircleClose, Download, View, Warning} from '@element-plus/icons-vue'
import FeedbackDialog from '@/components/FeedbackDialog.vue'
import AlarmDetailDialog from './components/AlarmDetailDialog.vue'
import FeedBack from '@/components/FeedBack.vue'
import {getPendingAlarms, disposeAlarm, batchDisposeAlarms} from '@/api/alarm'

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
const selectedRows = ref<any[]>([])

// 弹窗
const detailDialogVisible = ref(false)
const feedbackDialogVisible = ref(false)
const batchFeedbackVisible = ref(false)
const falseAlarmDialogVisible = ref(false)
const closeAlarmDialogVisible = ref(false)

// 当前行
const currentRow = ref<any>(null)

const loading = ref(false)

// 加载数据
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
    const res = await getPendingAlarms(params as any)
    tableData.value = res.rows || []
    pagination.total = res.total || 0
  } finally {
    loading.value = false
  }
}


onMounted(() => {
  fetchData()
})

const getAlarmLevelType = (level: number | string) => {
  const map: Record<string, string> = {
    '1': 'danger',
    '2': 'warning',
    '3': 'success',
    '4': 'info'
  }
  return map[String(level)] || 'info'
}

const getAlarmLevelText = (level: number | string) => {
  const map: Record<string, string> = {
    '1': '一级',
    '2': '二级',
    '3': '三级',
    '4': '四级'
  }
  return map[String(level)] || String(level)
}

const getAlarmTypeText = (type: string) => {
  const map: Record<string, string> = {
    'THRESHOLD': '阈值预警',
    'COMPREHENSIVE': '综合预警'
  }
  return map[type] || type
}

const getStatusType = (status: number) => {
  const map: Record<number, string> = {
    1: 'danger',
    2: 'warning'
  }
  return map[status] || 'info'
}

const getStatusText = (status: number) => {
  const map: Record<number, string> = {
    1: '待处理',
    2: '处理中'
  }
  return map[status] || String(status)
}

const handleQuery = () => {
  pagination.currentPage = 1
  fetchData()
}

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

const handleSelectionChange = (rows: any[]) => {
  selectedRows.value = rows
}

const handleRowClick = (row: any) => {
  currentRow.value = row
  detailDialogVisible.value = true
}

const handleView = (row: any) => {
  currentRow.value = row
  detailDialogVisible.value = true
}

// 处置反馈 - 原封不动
const handleFeedback = (row: any) => {
  currentRow.value = row
  feedbackDialogVisible.value = true
}

const handleFeedbackSubmit = () => {
  ElMessage.success('处置成功')
  feedbackDialogVisible.value = false
  fetchData()
}

// 批量反馈 - 只改了这里
const handleBatchFeedback = () => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请先选择要反馈的记录')
    return
  }
  batchFeedbackVisible.value = true
}

const handleBatchFeedbackSubmit = async (data: { content: string; files: File[] }) => {
  const ids = selectedRows.value.map(r => r.id)
  try {
    await batchDisposeAlarms({ ids, status: 2, note: data.content })
    ElMessage.success(`已对 ${ids.length} 条告警提交反馈`)
    batchFeedbackVisible.value = false
    selectedRows.value = []
    fetchData()
  } catch {
    ElMessage.error('批量反馈失败')
  }
}

const handleExport = () => {
  ElMessage.success('导出功能已触发（模拟）')
}

const handleBatchFalseAlarm = () => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请先选择要标记为误报的记录')
    return
  }
  currentRow.value = selectedRows.value
  falseAlarmDialogVisible.value = true
}

const confirmFalseAlarm = async () => {
  const ids = Array.isArray(currentRow.value)
    ? currentRow.value.map((r: any) => r.id)
    : [currentRow.value.id]
  try {
    await batchDisposeAlarms({ ids, status: 4 })
    ElMessage.success('已标记为误报')
  } catch {
    ElMessage.error('操作失败')
  }
  falseAlarmDialogVisible.value = false
  selectedRows.value = []
  fetchData()
}

const handleBatchCloseAlarm = () => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请先选择要销警的记录')
    return
  }
  currentRow.value = selectedRows.value
  closeAlarmDialogVisible.value = true
}

const confirmCloseAlarm = async () => {
  const ids = Array.isArray(currentRow.value)
    ? currentRow.value.map((r: any) => r.id)
    : [currentRow.value.id]
  try {
    await batchDisposeAlarms({ ids, status: 3 })
    ElMessage.success('销警成功')
  } catch {
    ElMessage.error('操作失败')
  }
  closeAlarmDialogVisible.value = false
  selectedRows.value = []
  fetchData()
}

const handleSizeChange = (size: number) => {
  pagination.pageSize = size
  pagination.currentPage = 1
  fetchData()
}

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

.detail-content {
  padding: 16px 0;
}

.alarm-list-table {
  max-height: 400px;
  overflow: auto;
}
</style>