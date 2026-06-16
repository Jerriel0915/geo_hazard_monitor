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
        <el-tooltip content="暂未开放" placement="top">
          <el-button type="info" disabled>
            <el-icon><Download /></el-icon>
            导出
          </el-button>
        </el-tooltip>
      </div>
    </div>

    <div class="search">
      <el-input v-model="queryParams.hazardPointName" placeholder="隐患点名称" clearable class="search__input" />
      <el-date-picker
          v-model="queryParams.alarmTimeRange"
          type="daterange"
          range-separator=""
          start-placeholder="告警时间:开始"
          end-placeholder="告警时间:结束"
          value-format="YYYY-MM-DD"
      />
      <el-select v-model="queryParams.alarmLevel" placeholder="告警等级" clearable multiple collapse-tags collapse-tags-tooltip class="search__select">
        <el-option label="一级" :value="1" />
        <el-option label="二级" :value="2" />
        <el-option label="三级" :value="3" />
        <el-option label="四级" :value="4" />
      </el-select>
      <el-select v-model="queryParams.alarmType" placeholder="告警类型" clearable multiple collapse-tags collapse-tags-tooltip class="search__select">
        <el-option label="阈值预警" value="THRESHOLD" />
        <el-option label="综合预警" value="COMPREHENSIVE" />
      </el-select>
      <el-select v-model="queryParams.status" placeholder="警情状态" clearable multiple collapse-tags collapse-tags-tooltip class="search__select">
        <el-option label="待处理" :value="1" />
        <el-option label="处理中" :value="2" />
        <el-option label="已销警" :value="3" />
        <el-option label="误报" :value="4" />
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
              <el-tag :style="getAlarmLevelStyle(row.alarmLevel)">{{ getAlarmLevelText(row.alarmLevel) }}</el-tag>
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
            <template #default="{ row }">{{ getAlarmTypeText(row.alarmType) }}</template>
          </el-table-column>
          <el-table-column prop="status" label="警情状态" width="100">
            <template #default="{ row }">
              <el-tag :type="getStatusType(row.status)">{{ row.statusName || getStatusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="resolvedBy" label="响应人员" width="120" />
          <el-table-column prop="resolvedAt" label="响应时间" min-width="180" />
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
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

    <!-- 告警详情/处置弹窗 - 查看 + 处置共用 -->
    <AlarmDetailDialog
        v-model="detailDialogVisible"
        :data="currentRow"
        @submit="handleFeedbackSubmit"
        @false-alarm="handleDetailFalseAlarm"
        @close-alarm="handleDetailCloseAlarm"
    />

    <!-- 批量反馈弹窗 - FeedBack组件 -->
    <FeedBack v-model:visible="batchFeedbackVisible" @submit="handleBatchFeedbackSubmit" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ChatDotRound, CircleClose, Download, Warning } from '@element-plus/icons-vue'
import AlarmDetailDialog from './components/AlarmDetailDialog.vue'
import FeedBack from '@/components/FeedBack.vue'
import {
  getPendingAlarms,
  disposeAlarm,
  batchDisposeAlarms,
  getAlarmLevelStyle,
  type AlarmRecordItem,
  type AlarmRecordPageParams,
} from '@/api/alarm'

// 查询参数（已移除人员名称）
const queryParams = reactive({
  hazardPointName: '',
  alarmTimeRange: [] as string[],
  alarmLevel: [] as number[],
  alarmType: [] as string[],
  status: [] as number[],
})

const pagination = reactive({ currentPage: 1, pageSize: 10, total: 0 })

const tableData = ref<AlarmRecordItem[]>([])
const selectedRows = ref<AlarmRecordItem[]>([])

const detailDialogVisible = ref(false)
const batchFeedbackVisible = ref(false)
const currentRow = ref<AlarmRecordItem | null>(null)

// ── 数据加载 ──
async function loadList() {
  const params: AlarmRecordPageParams = {
    pageNum: pagination.currentPage,
    pageSize: pagination.pageSize,
    hazardPointName: queryParams.hazardPointName || undefined,
    alarmLevels: queryParams.alarmLevel.length > 0 ? queryParams.alarmLevel : undefined,
    alarmTypes: queryParams.alarmType.length > 0 ? queryParams.alarmType : undefined,
    statusList: queryParams.status.length > 0 ? queryParams.status : undefined,
  }
  if (queryParams.alarmTimeRange?.length === 2) {
    params.triggerTimeBegin = queryParams.alarmTimeRange[0]
    params.triggerTimeEnd = queryParams.alarmTimeRange[1]
  }
  const res = await getPendingAlarms(params)
  tableData.value = res.rows || []
  pagination.total = res.total || 0
}

onMounted(() => { loadList() })

// ── 枚举映射（数字/大写）──
const getAlarmLevelText = (level: number | string) => {
  const n = Number(level)
  return ({ 1: '一级', 2: '二级', 3: '三级', 4: '四级' } as Record<number, string>)[n] || String(level)
}
const getAlarmTypeText = (type: string) =>
  ({ THRESHOLD: '阈值预警', COMPREHENSIVE: '综合预警' } as Record<string, string>)[type] || type
const getStatusType = (status: number | string) => {
  const n = Number(status)
  return ({ 1: 'danger', 2: 'warning', 3: 'success', 4: 'info' } as Record<number, string>)[n] || 'info'
}
const getStatusText = (status: number | string) => {
  const n = Number(status)
  return ({ 1: '待处理', 2: '处理中', 3: '已销警', 4: '误报' } as Record<number, string>)[n] || String(status)
}

// ── 查询 / 重置 / 分页 ──
const handleQuery = () => { pagination.currentPage = 1; loadList() }
const handleReset = () => {
  queryParams.hazardPointName = ''
  queryParams.alarmTimeRange = []
  queryParams.alarmLevel = []
  queryParams.alarmType = []
  queryParams.status = []
  pagination.currentPage = 1
  loadList()
}
const handleSizeChange = (size: number) => { pagination.pageSize = size; loadList() }
const handleCurrentChange = (page: number) => { pagination.currentPage = page; loadList() }

const handleSelectionChange = (rows: AlarmRecordItem[]) => { selectedRows.value = rows }

// ── 查看 ──
const handleView = (row: AlarmRecordItem) => { currentRow.value = row; detailDialogVisible.value = true }
const handleRowClick = (row: AlarmRecordItem) => { currentRow.value = row; detailDialogVisible.value = true }

// ── 处置反馈 ──
const handleFeedback = (row: AlarmRecordItem) => { currentRow.value = row; detailDialogVisible.value = true }

const handleFeedbackSubmit = async (payload: { description?: string; attachments?: string; remarks?: string }) => {
  if (!currentRow.value) return
  try {
    await disposeAlarm(currentRow.value.id, {
      status: 2,
      description: payload.description,
      attachments: payload.attachments,
      remarks: payload.remarks,
    })
    ElMessage.success('处置成功')
    detailDialogVisible.value = false
    loadList()
  } catch (e) {
    ElMessage.error('处置失败')
  }
}

// ── 详情内单条误报 ──
const handleDetailFalseAlarm = async (row: AlarmRecordItem | null) => {
  if (!row) return
  try {
    await ElMessageBox.confirm(`确定将此告警标记为误报吗？`, '误报确认', { type: 'warning' })
    await disposeAlarm(row.id, { status: 4 })
    ElMessage.success('已标记为误报')
    detailDialogVisible.value = false
    loadList()
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') {
      ElMessage.error('标记误报失败')
    }
  }
}

// ── 详情内单条销警 ──
const handleDetailCloseAlarm = async (row: AlarmRecordItem | null) => {
  if (!row) return
  try {
    await ElMessageBox.confirm(`确定要销警此告警吗？`, '销警确认', { type: 'warning' })
    await disposeAlarm(row.id, { status: 3 })
    ElMessage.success('销警成功')
    detailDialogVisible.value = false
    loadList()
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') {
      ElMessage.error('销警失败')
    }
  }
}

// ── 批量反馈 ──
const handleBatchFeedback = () => {
  if (selectedRows.value.length === 0) { ElMessage.warning('请先选择要反馈的记录'); return }
  batchFeedbackVisible.value = true
}

const handleBatchFeedbackSubmit = async (payload: { content: string; files: File[] }) => {
  try {
    await batchDisposeAlarms({
      ids: selectedRows.value.map(r => r.id),
      status: 2,
      remarks: payload.content,
    })
    ElMessage.success(`已对 ${selectedRows.value.length} 条告警提交反馈`)
    batchFeedbackVisible.value = false
    selectedRows.value = []
    loadList()
  } catch (e) {
    ElMessage.error('批量反馈失败')
  }
}

// ── 批量误报 ──
const handleBatchFalseAlarm = async () => {
  if (selectedRows.value.length === 0) { ElMessage.warning('请先选择要标记为误报的记录'); return }
  try {
    await ElMessageBox.confirm(`确定将选中的 ${selectedRows.value.length} 条告警标记为误报吗？`, '误报确认', { type: 'warning' })
    await batchDisposeAlarms({ ids: selectedRows.value.map(r => r.id), status: 4 })
    ElMessage.success('已标记为误报')
    selectedRows.value = []
    loadList()
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') {
      ElMessage.error('标记误报失败')
    }
  }
}

// ── 批量销警 ──
const handleBatchCloseAlarm = async () => {
  if (selectedRows.value.length === 0) { ElMessage.warning('请先选择要销警的记录'); return }
  try {
    await ElMessageBox.confirm(`确定要销警 ${selectedRows.value.length} 条告警吗？`, '销警确认', { type: 'warning' })
    await batchDisposeAlarms({ ids: selectedRows.value.map(r => r.id), status: 3 })
    ElMessage.success('销警成功')
    selectedRows.value = []
    loadList()
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') {
      ElMessage.error('销警失败')
    }
  }
}

// 导出：按钮已 disabled，函数保留为空
const handleExport = () => { ElMessage.info('暂未开放') }
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