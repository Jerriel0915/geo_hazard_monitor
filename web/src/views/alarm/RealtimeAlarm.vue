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
        <el-option label="阈值预警" value="threshold" />
        <el-option label="综合预警" value="comprehensive" />
      </el-select>
      <el-select v-model="queryParams.status" placeholder="警情状态" clearable multiple class="search__select">
        <el-option label="待处理" value="pending"/>
        <el-option label="处理中" value="processing"/>
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
          <el-table-column prop="firstAlarmTime" label="首次告警时间" width="180" />
          <el-table-column prop="lastAlarmTime" label="最后告警时间" width="180" />
          <el-table-column prop="alarmCount" label="告警次数" width="100">
            <template #default="{ row }">
              <span class="alarm-count" @click.stop="handleView(row)">{{ row.alarmCount }}</span>
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
          <el-table-column prop="responderName" label="响应人员" width="120" />
          <el-table-column prop="responseTime" label="响应时间" min-width="180" />
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
import {computed, onMounted, reactive, ref} from 'vue'
import {ElMessage} from 'element-plus'
import {ChatDotRound, CircleClose, Download, View, Warning} from '@element-plus/icons-vue'
import FeedbackDialog from '@/components/FeedbackDialog.vue'
import AlarmDetailDialog from './components/AlarmDetailDialog.vue'
import FeedBack from '@/components/FeedBack.vue'

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

// Mock 数据
const mockData = [
  {
    id: 1,
    hazardPointName: '边坡监测点A-01',
    alarmLevel: '1',
    firstAlarmTime: '2024-06-01 08:30:00',
    lastAlarmTime: '2024-06-03 14:25:00',
    alarmCount: 15,
    alarmType: 'threshold',
    status: 'pending',
    responderName: '',
    responseTime: '',
    alarmContent: '边坡位移速率超过阈值12mm/h，当前值为15.2mm/h，请及时处理',
    alarmList: [
      { alarmTime: '2024-06-01 08:30:00', alarmLevel: '1', alarmContent: '边坡位移速率超过阈值12mm/h，当前值为12.5mm/h' },
      { alarmTime: '2024-06-01 12:45:00', alarmLevel: '1', alarmContent: '边坡位移速率超过阈值12mm/h，当前值为13.8mm/h' },
      { alarmTime: '2024-06-03 14:25:00', alarmLevel: '1', alarmContent: '边坡位移速率超过阈值12mm/h，当前值为15.2mm/h' }
    ]
  },
  {
    id: 2,
    hazardPointName: '地质灾害点B-05',
    alarmLevel: '2',
    firstAlarmTime: '2024-06-02 10:15:00',
    lastAlarmTime: '2024-06-03 10:15:00',
    alarmCount: 8,
    alarmType: 'comprehensive',
    status: 'processing',
    responderName: '张三',
    responseTime: '2024-06-03 09:00:00',
    alarmContent: '综合预警：降雨量和位移同时超过阈值，当前降雨量45mm/h，位移速率8.5mm/h',
    alarmList: [
      { alarmTime: '2024-06-02 10:15:00', alarmLevel: '2', alarmContent: '综合预警：降雨量35mm/h，位移速率7.2mm/h' },
      { alarmTime: '2024-06-03 10:15:00', alarmLevel: '2', alarmContent: '综合预警：降雨量45mm/h，位移速率8.5mm/h' }
    ]
  },
  {
    id: 3,
    hazardPointName: '山体滑坡点C-12',
    alarmLevel: '3',
    firstAlarmTime: '2024-05-28 16:20:00',
    lastAlarmTime: '2024-06-02 20:30:00',
    alarmCount: 25,
    alarmType: 'threshold',
    status: 'processing',
    responderName: '李四',
    responseTime: '2024-05-28 17:00:00',
    alarmContent: '山体倾斜角度超过阈值5度，当前值为6.2度',
    alarmList: [
      {alarmTime: '2024-05-28 16:20:00', alarmLevel: '3', alarmContent: '山体倾斜角度5.1度'},
      {alarmTime: '2024-06-02 20:30:00', alarmLevel: '3', alarmContent: '山体倾斜角度6.2度'}
    ]
  },
  {
    id: 4,
    hazardPointName: '桥梁监测点D-03',
    alarmLevel: '4',
    firstAlarmTime: '2024-06-01 00:00:00',
    lastAlarmTime: '2024-06-02 00:00:00',
    alarmCount: 3,
    alarmType: 'threshold',
    status: 'pending',
    responderName: '',
    responseTime: '',
    alarmContent: '桥墩沉降超过阈值2mm，当前值为2.3mm',
    alarmList: [
      {alarmTime: '2024-06-01 00:00:00', alarmLevel: '4', alarmContent: '桥墩沉降2.1mm'},
      {alarmTime: '2024-06-02 00:00:00', alarmLevel: '4', alarmContent: '桥墩沉降2.3mm'}
    ]
  },
  {
    id: 5,
    hazardPointName: '隧道监测点E-08',
    alarmLevel: '1',
    firstAlarmTime: '2024-06-03 08:00:00',
    lastAlarmTime: '2024-06-03 12:30:00',
    alarmCount: 5,
    alarmType: 'comprehensive',
    status: 'pending',
    responderName: '',
    responseTime: '',
    alarmContent: '隧道拱顶沉降和收敛同时异常，沉降值15mm，收敛值20mm',
    alarmList: [
      { alarmTime: '2024-06-03 08:00:00', alarmLevel: '1', alarmContent: '隧道拱顶沉降12mm' },
      { alarmTime: '2024-06-03 12:30:00', alarmLevel: '1', alarmContent: '隧道拱顶沉降15mm，收敛20mm' }
    ]
  },
  {
    id: 6,
    hazardPointName: '水库监测点F-02',
    alarmLevel: '2',
    firstAlarmTime: '2024-05-25 06:00:00',
    lastAlarmTime: '2024-06-01 18:00:00',
    alarmCount: 12,
    alarmType: 'threshold',
    status: 'processing',
    responderName: '赵六',
    responseTime: '2024-05-25 08:00:00',
    alarmContent: '水库水位超过警戒水位，当前水位超出安全范围',
    alarmList: [
      {alarmTime: '2024-05-25 06:00:00', alarmLevel: '2', alarmContent: '水库水位超限30cm'},
      {alarmTime: '2024-06-01 18:00:00', alarmLevel: '2', alarmContent: '水库水位超限50cm'}
    ]
  }
]

// 计算属性：过滤后的数据
const filteredData = computed(() => {
  let result = [...mockData]

  if (queryParams.hazardPointName) {
    result = result.filter(item => item.hazardPointName.includes(queryParams.hazardPointName))
  }
  if (queryParams.personName) {
    result = result.filter(item => item.responderName && item.responderName.includes(queryParams.personName))
  }
  if (queryParams.alarmLevel.length > 0) {
    result = result.filter(item => queryParams.alarmLevel.includes(item.alarmLevel))
  }
  if (queryParams.alarmType.length > 0) {
    result = result.filter(item => queryParams.alarmType.includes(item.alarmType))
  }
  if (queryParams.status.length > 0) {
    result = result.filter(item => queryParams.status.includes(item.status))
  }
  if (queryParams.alarmCountMin != null) {
    result = result.filter(item => item.alarmCount >= queryParams.alarmCountMin!)
  }
  if (queryParams.alarmCountMax != null) {
    result = result.filter(item => item.alarmCount <= queryParams.alarmCountMax!)
  }

  result.sort((a, b) => new Date(b.lastAlarmTime).getTime() - new Date(a.lastAlarmTime).getTime())
  return result
})

const paginatedData = computed(() => {
  const start = (pagination.currentPage - 1) * pagination.pageSize
  const end = start + pagination.pageSize
  return filteredData.value.slice(start, end)
})

onMounted(() => {
  pagination.total = mockData.length
  tableData.value = paginatedData.value
})

const getAlarmLevelType = (level: string) => {
  const map: Record<string, string> = {
    '1': 'danger',
    '2': 'warning',
    '3': 'success',
    '4': 'info'
  }
  return map[level] || 'info'
}

const getAlarmLevelText = (level: string) => {
  const map: Record<string, string> = {
    '1': '一级',
    '2': '二级',
    '3': '三级',
    '4': '四级'
  }
  return map[level] || level
}

const getAlarmTypeText = (type: string) => {
  const map: Record<string, string> = {
    'threshold': '阈值预警',
    'comprehensive': '综合预警'
  }
  return map[type] || type
}

const getStatusType = (status: string) => {
  const map: Record<string, string> = {
    'pending': 'danger',
    'processing': 'warning'
  }
  return map[status] || 'info'
}

const getStatusText = (status: string) => {
  const map: Record<string, string> = {
    'pending': '待处理',
    'processing': '处理中'
  }
  return map[status] || status
}

const handleQuery = () => {
  pagination.currentPage = 1
  pagination.total = filteredData.value.length
  tableData.value = paginatedData.value
  ElMessage.success('查询成功')
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
  pagination.total = filteredData.value.length
  tableData.value = paginatedData.value
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
  tableData.value = paginatedData.value
}

// 批量反馈 - 只改了这里
const handleBatchFeedback = () => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请先选择要反馈的记录')
    return
  }
  batchFeedbackVisible.value = true
}

const handleBatchFeedbackSubmit = (data: { content: string; files: File[] }) => {
  console.log('批量反馈内容:', data.content)
  console.log('批量反馈文件:', data.files)

  const now = new Date().toLocaleString()
  const currentUser = '当前用户'

  selectedRows.value.forEach(row => {
    if (row.status !== 'closed' && row.status !== 'false_alarm') {
      row.status = 'processing'
      row.responderName = currentUser
      row.responseTime = now
    }
  })

  ElMessage.success(`已对 ${selectedRows.value.length} 条告警提交反馈`)
  batchFeedbackVisible.value = false
  selectedRows.value = []
  tableData.value = paginatedData.value
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

const confirmFalseAlarm = () => {
  if (Array.isArray(currentRow.value)) {
    currentRow.value.forEach(row => {
      row.status = 'false_alarm'
      row.responderName = '当前用户'
      row.responseTime = new Date().toLocaleString()
    })
  } else {
    currentRow.value.status = 'false_alarm'
    currentRow.value.responderName = '当前用户'
    currentRow.value.responseTime = new Date().toLocaleString()
  }

  ElMessage.success('已标记为误报')
  falseAlarmDialogVisible.value = false
  selectedRows.value = []
  tableData.value = paginatedData.value
}

const handleBatchCloseAlarm = () => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请先选择要销警的记录')
    return
  }
  currentRow.value = selectedRows.value
  closeAlarmDialogVisible.value = true
}

const confirmCloseAlarm = () => {
  if (Array.isArray(currentRow.value)) {
    currentRow.value.forEach(row => {
      row.status = 'closed'
      row.responderName = '当前用户'
      row.responseTime = new Date().toLocaleString()
    })
  } else {
    currentRow.value.status = 'closed'
    currentRow.value.responderName = '当前用户'
    currentRow.value.responseTime = new Date().toLocaleString()
  }

  ElMessage.success('销警成功')
  closeAlarmDialogVisible.value = false
  selectedRows.value = []
  tableData.value = paginatedData.value
}

const handleSizeChange = (size: number) => {
  pagination.pageSize = size
  tableData.value = paginatedData.value
}

const handleCurrentChange = (page: number) => {
  pagination.currentPage = page
  tableData.value = paginatedData.value
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