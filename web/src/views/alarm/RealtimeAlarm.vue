<template>
  <div class="page-content">
    <div class="page-title">待办告警</div>
    <div class="page-body">
      <!-- 搜索和操作栏 -->
      <div class="search-bar">
        <div class="search-conditions">
          <el-form :inline="true" :model="queryParams" label-width="100px">
            <el-form-item label="隐患点名称">
              <el-input v-model="queryParams.hazardPointName" placeholder="请输入" clearable />
            </el-form-item>
            <el-form-item label="人员名称">
              <el-input v-model="queryParams.personName" placeholder="请输入" clearable />
            </el-form-item>
            <el-form-item label="告警时间">
              <el-date-picker
                v-model="queryParams.alarmTimeRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始时间"
                end-placeholder="结束时间"
                value-format="YYYY-MM-DD"
              />
            </el-form-item>
            <el-form-item label="告警次数">
              <el-input-number v-model="queryParams.alarmCountMin" :min="0" placeholder="最小" style="width: 120px" />
              <span style="margin: 0 8px">至</span>
              <el-input-number v-model="queryParams.alarmCountMax" :min="0" placeholder="最大" style="width: 120px" />
            </el-form-item>
            <el-form-item label="告警等级">
              <el-select v-model="queryParams.alarmLevel" placeholder="请选择" clearable multiple style="width: 120px">
                <el-option label="一级" value="1" />
                <el-option label="二级" value="2" />
                <el-option label="三级" value="3" />
                <el-option label="四级" value="4" />
              </el-select>
            </el-form-item>
            <el-form-item label="告警类型">
              <el-select v-model="queryParams.alarmType" placeholder="请选择" clearable multiple style="width: 120px">
                <el-option label="阈值预警" value="threshold" />
                <el-option label="综合预警" value="comprehensive" />
              </el-select>
            </el-form-item>
            <el-form-item label="警情状态">
              <el-select v-model="queryParams.status" placeholder="请选择" clearable multiple style="width: 120px">
                <el-option label="新警情" value="pending" />
                <el-option label="已响应" value="processing" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleQuery">查询</el-button>
              <el-button @click="handleReset">重置</el-button>
            </el-form-item>
          </el-form>
        </div>
        <div class="action-buttons">
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

      <!-- 数据表格 -->
      <div class="table-container">
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
              <span class="alarm-count" @click.stop="showAlarmList(row)">{{ row.alarmCount }}</span>
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
          <el-table-column prop="responseTime" label="响应时间" width="180" />
          <el-table-column label="操作" width="160" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click.stop="handleView(row)">
                <el-icon><View /></el-icon>
                查看
              </el-button>
              <el-button type="success" link size="small" @click.stop="handleFeedback(row)">
                <el-icon><ChatDotRound /></el-icon>
                处置
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 分页 -->
      <div class="pagination">
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
    <el-dialog v-model="detailDialogVisible" title="告警详情" width="800px">
      <div v-if="currentRow" class="detail-content">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="隐患点名称">{{ currentRow.hazardPointName }}</el-descriptions-item>
          <el-descriptions-item label="告警等级">
            <el-tag :type="getAlarmLevelType(currentRow.alarmLevel)">{{ getAlarmLevelText(currentRow.alarmLevel) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="告警类型">{{ getAlarmTypeText(currentRow.alarmType) }}</el-descriptions-item>
          <el-descriptions-item label="警情状态">
            <el-tag :type="getStatusType(currentRow.status)">{{ getStatusText(currentRow.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="首次告警时间">{{ currentRow.firstAlarmTime }}</el-descriptions-item>
          <el-descriptions-item label="最后告警时间">{{ currentRow.lastAlarmTime }}</el-descriptions-item>
          <el-descriptions-item label="告警次数">{{ currentRow.alarmCount }}</el-descriptions-item>
          <el-descriptions-item label="响应人员">{{ currentRow.responderName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="响应时间">{{ currentRow.responseTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="告警内容" :span="2">{{ currentRow.alarmContent }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 告警列表弹窗 -->
    <el-dialog v-model="alarmListDialogVisible" title="告警列表" width="900px">
      <div class="alarm-list-table">
        <el-table :data="currentAlarmList" style="width: 100%" border stripe>
          <el-table-column prop="alarmTime" label="告警时间" width="180" />
          <el-table-column prop="alarmLevel" label="告警等级" width="100">
            <template #default="{ row }">
              <el-tag :type="getAlarmLevelType(row.alarmLevel)">{{ getAlarmLevelText(row.alarmLevel) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="alarmContent" label="告警内容" />
        </el-table>
      </div>
      <template #footer>
        <el-button @click="alarmListDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 反馈弹窗组件 -->
    <FeedbackDialog 
      v-model="feedbackDialogVisible"
      :data="currentRow"
      @close="feedbackDialogVisible = false"
      @submit="submitFeedback"
      @view-detail="handleViewDetail"
      @quick-response="handleQuickResponse"
      @false-alarm="handleFalseAlarm"
      @close-alarm="handleCloseAlarm"
    />

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

    <!-- 批量反馈弹窗 -->
    <el-dialog 
      v-model="batchFeedbackDialogVisible" 
      :title="batchFeedbackTitle" 
      width="500px"
    >
      <el-form :model="batchFeedbackForm">
        <el-form-item label="反馈意见" label-width="80px">
          <el-input 
            v-model="batchFeedbackForm.remark" 
            type="textarea" 
            :rows="4" 
            placeholder="请输入批量反馈意见..."
            maxlength="500"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchFeedbackDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmBatchFeedback">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import {computed, onMounted, reactive, ref} from 'vue'
import {ElMessage} from 'element-plus'
import {ChatDotRound, CircleClose, Download, View, Warning} from '@element-plus/icons-vue'
import FeedbackDialog from '@/components/FeedbackDialog.vue'

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
const alarmListDialogVisible = ref(false)
const feedbackDialogVisible = ref(false)
const falseAlarmDialogVisible = ref(false)
const closeAlarmDialogVisible = ref(false)
const batchFeedbackDialogVisible = ref(false)

// 批量反馈表单
const batchFeedbackForm = reactive({
  remark: ''
})

const batchFeedbackTitle = computed(() => {
  return `批量反馈-${selectedRows.value.length}`
})

// 当前行
const currentRow = ref<any>(null)
const currentAlarmList = ref<any[]>([])
const feedbackForm = reactive({
  content: ''
})

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
    id: 4,
    hazardPointName: '泥石流监测点G-07',
    alarmLevel: '1',
    firstAlarmTime: '2024-06-03 15:00:00',
    lastAlarmTime: '2024-06-03 16:30:00',
    alarmCount: 6,
    alarmType: 'comprehensive',
    status: 'pending',
    responderName: '',
    responseTime: '',
    alarmContent: '综合预警：降雨量和土壤含水率同时超过阈值',
    alarmList: [
      { alarmTime: '2024-06-03 15:00:00', alarmLevel: '1', alarmContent: '降雨量达到阈值' },
      { alarmTime: '2024-06-03 16:30:00', alarmLevel: '1', alarmContent: '土壤含水率超限' }
    ]
  },
  {
    id: 5,
    hazardPointName: '地质灾害点H-09',
    alarmLevel: '2',
    firstAlarmTime: '2024-06-03 10:00:00',
    lastAlarmTime: '2024-06-03 11:00:00',
    alarmCount: 3,
    alarmType: 'threshold',
    status: 'processing',
    responderName: '李四',
    responseTime: '2024-06-03 10:30:00',
    alarmContent: '裂缝宽度超过阈值，正在处理中',
    alarmList: [
      { alarmTime: '2024-06-03 10:00:00', alarmLevel: '2', alarmContent: '裂缝宽度5mm' },
      { alarmTime: '2024-06-03 11:00:00', alarmLevel: '2', alarmContent: '裂缝宽度6mm' }
    ]
  },
  {
    id: 6,
    hazardPointName: '边坡监测点J-15',
    alarmLevel: '3',
    firstAlarmTime: '2024-06-03 09:30:00',
    lastAlarmTime: '2024-06-03 14:00:00',
    alarmCount: 10,
    alarmType: 'threshold',
    status: 'processing',
    responderName: '王五',
    responseTime: '2024-06-03 10:00:00',
    alarmContent: '地表位移超过阈值，已安排人员现场勘查',
    alarmList: [
      { alarmTime: '2024-06-03 09:30:00', alarmLevel: '3', alarmContent: '位移3mm' },
      { alarmTime: '2024-06-03 14:00:00', alarmLevel: '3', alarmContent: '位移4.5mm' }
    ]
  }
]

// 计算属性：过滤后的数据
const filteredData = computed(() => {
  let result = [...mockData]

  // 隐患点名称模糊查询
  if (queryParams.hazardPointName) {
    result = result.filter(item => 
      item.hazardPointName.includes(queryParams.hazardPointName)
    )
  }

  // 人员名称模糊查询
  if (queryParams.personName) {
    result = result.filter(item => 
      item.responderName && item.responderName.includes(queryParams.personName)
    )
  }

  // 告警等级筛选
  if (queryParams.alarmLevel.length > 0) {
    result = result.filter(item => queryParams.alarmLevel.includes(item.alarmLevel))
  }

  // 告警类型筛选
  if (queryParams.alarmType.length > 0) {
    result = result.filter(item => queryParams.alarmType.includes(item.alarmType))
  }

  // 警情状态筛选
  if (queryParams.status.length > 0) {
    result = result.filter(item => queryParams.status.includes(item.status))
  }

  // 告警次数范围筛选
  if (queryParams.alarmCountMin != null) {
    result = result.filter(item => item.alarmCount >= queryParams.alarmCountMin!)
  }
  if (queryParams.alarmCountMax != null) {
    result = result.filter(item => item.alarmCount <= queryParams.alarmCountMax!)
  }

  // 按最后告警时间倒序
  result.sort((a, b) => new Date(b.lastAlarmTime).getTime() - new Date(a.lastAlarmTime).getTime())

  return result
})

// 计算属性：分页数据
const paginatedData = computed(() => {
  const start = (pagination.currentPage - 1) * pagination.pageSize
  const end = start + pagination.pageSize
  return filteredData.value.slice(start, end)
})

// 初始化
onMounted(() => {
  pagination.total = mockData.length
  tableData.value = paginatedData.value
})

// 获取告警等级类型
const getAlarmLevelType = (level: string) => {
  const map: Record<string, string> = {
    '1': 'danger',
    '2': 'warning',
    '3': 'success',
    '4': 'info'
  }
  return map[level] || 'info'
}

// 获取告警等级文本
const getAlarmLevelText = (level: string) => {
  const map: Record<string, string> = {
    '1': '一级',
    '2': '二级',
    '3': '三级',
    '4': '四级'
  }
  return map[level] || level
}

// 获取告警类型文本
const getAlarmTypeText = (type: string) => {
  const map: Record<string, string> = {
    'threshold': '阈值预警',
    'comprehensive': '综合预警'
  }
  return map[type] || type
}

// 获取状态类型
const getStatusType = (status: string) => {
  const map: Record<string, string> = {
    'pending': 'danger',
    'processing': 'warning'
  }
  return map[status] || 'info'
}

// 获取状态文本
const getStatusText = (status: string) => {
  const map: Record<string, string> = {
    'pending': '新警情',
    'processing': '已响应'
  }
  return map[status] || status
}

// 查询
const handleQuery = () => {
  pagination.currentPage = 1
  pagination.total = filteredData.value.length
  tableData.value = paginatedData.value
  ElMessage.success('查询成功')
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
  handleQuery()
}

// 表格选择变化
const handleSelectionChange = (rows: any[]) => {
  selectedRows.value = rows
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

// 显示告警列表
const showAlarmList = (row: any) => {
  currentRow.value = row
  currentAlarmList.value = row.alarmList || []
  alarmListDialogVisible.value = true
}

// 反馈
const handleFeedback = (row: any) => {
  currentRow.value = row
  feedbackForm.content = ''
  feedbackDialogVisible.value = true
}

// 批量反馈
const handleBatchFeedback = () => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请先选择要反馈的记录')
    return
  }
  batchFeedbackForm.remark = ''
  batchFeedbackDialogVisible.value = true
}

// 确认批量反馈
const confirmBatchFeedback = () => {
  if (!batchFeedbackForm.remark.trim()) {
    ElMessage.warning('请输入反馈意见')
    return
  }
  
  selectedRows.value.forEach(row => {
    row.status = 'processing'
    row.responderName = '当前用户'
    row.responseTime = new Date().toLocaleString()
  })
  
  ElMessage.success(`成功反馈 ${selectedRows.value.length} 条告警记录`)
  batchFeedbackDialogVisible.value = false
  selectedRows.value = []
}

// 提交反馈
const submitFeedback = () => {
  if (!feedbackForm.content.trim()) {
    ElMessage.warning('请输入反馈内容')
    return
  }
  
  if (Array.isArray(currentRow.value)) {
    currentRow.value.forEach(row => {
      row.status = 'processing'
      row.responderName = '当前用户'
      row.responseTime = new Date().toLocaleString()
    })
  } else {
    currentRow.value.status = 'processing'
    currentRow.value.responderName = '当前用户'
    currentRow.value.responseTime = new Date().toLocaleString()
  }
  
  ElMessage.success('反馈成功')
  feedbackDialogVisible.value = false
  tableData.value = paginatedData.value
}

// 查看详情
const handleViewDetail = () => {
  ElMessage.info('查看详情功能已触发')
}

// 快速响应
const handleQuickResponse = (row: any) => {
  const targetRow = row || currentRow.value
  if (targetRow) {
    targetRow.status = 'processing'
    targetRow.responderName = '当前用户'
    targetRow.responseTime = new Date().toLocaleString()
    ElMessage.success('响应成功')
    tableData.value = paginatedData.value
  }
}

// 误报
const handleFalseAlarm = (row: any) => {
  currentRow.value = row
  falseAlarmDialogVisible.value = true
}

// 批量误报
const handleBatchFalseAlarm = () => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请先选择要标记为误报的记录')
    return
  }
  currentRow.value = selectedRows.value
  falseAlarmDialogVisible.value = true
}

// 确认误报
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

// 销警
const handleCloseAlarm = (row: any) => {
  currentRow.value = row
  closeAlarmDialogVisible.value = true
}

// 批量销警
const handleBatchCloseAlarm = () => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请先选择要销警的记录')
    return
  }
  currentRow.value = selectedRows.value
  closeAlarmDialogVisible.value = true
}

// 确认销警
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

// 导出
const handleExport = () => {
  ElMessage.success('导出功能已触发（模拟）')
}

// 分页大小变化
const handleSizeChange = (size: number) => {
  pagination.pageSize = size
  tableData.value = paginatedData.value
}

// 分页页码变化
const handleCurrentChange = (page: number) => {
  pagination.currentPage = page
  tableData.value = paginatedData.value
}
</script>

<style scoped>
.realtime-alarm-container {
  padding: 20px;
  background: #fff;
  border-radius: 4px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #e4e7ed;
}

.page-header h2 {
  margin: 0;
  font-size: 18px;
  font-weight: bold;
  color: #303133;
}

.search-bar {
  margin-bottom: 20px;
  padding: 15px;
  background: #f5f7fa;
  border-radius: 4px;
}

.search-form {
  margin: 0;
}

.table-container {
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 1px solid #e8e8e8;
}

.page-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.search-bar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 16px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
}

.search-conditions {
  flex: 1;
  min-width: 0;
}

.action-buttons {
  display: flex;
  gap: 8px;
  align-items: flex-end;
}

.table-container {
  flex: 1;
  overflow: auto;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
}

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

/* 反馈弹窗样式 */
.feedback-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.feedback-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background: linear-gradient(135deg, #0a1628 0%, #1a365d 100%);
  border-radius: 8px;
  color: #fff;
}

.header-left {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.header-left .label {
  color: #94a3b8;
  font-size: 12px;
}

.header-left .value {
  font-weight: 600;
  font-size: 14px;
}

.header-center {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.alarm-level-badge {
  padding: 8px 24px;
  border-radius: 20px;
  font-weight: bold;
  font-size: 16px;
}

.alarm-level-badge.danger {
  background: linear-gradient(135deg, #dc2626 0%, #ef4444 100%);
}

.alarm-level-badge.warning {
  background: linear-gradient(135deg, #ea580c 0%, #f97316 100%);
}

.alarm-level-badge.success {
  background: linear-gradient(135deg, #059669 0%, #10b981 100%);
}

.alarm-level-badge.info {
  background: linear-gradient(135deg, #2563eb 0%, #3b82f6 100%);
}

.alarm-desc {
  font-size: 14px;
  color: #e2e8f0;
  max-width: 400px;
  text-align: center;
}

.header-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 12px;
}

.timer-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #94a3b8;
}

.timer-info .icon {
  font-size: 18px;
}

.feedback-body {
  display: flex;
  gap: 16px;
}

.side-panel {
  width: 200px;
  background: #f8fafc;
  border-radius: 8px;
  padding: 16px;
}

.main-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 1px solid #e2e8f0;
}

.panel-title .el-icon {
  color: #3b82f6;
}

/* 事件生命周期 */
.lifecycle {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.lifecycle-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 8px 0;
}

.lifecycle-item.active .lifecycle-node {
  transform: scale(1.1);
  box-shadow: 0 0 20px rgba(59, 130, 246, 0.5);
}

.lifecycle-node {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 16px;
  position: relative;
  transition: all 0.3s ease;
}

.lifecycle-node .badge {
  position: absolute;
  top: -5px;
  right: -5px;
  background: #22c55e;
  color: #fff;
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 10px;
}

.device-node {
  background: #64748b;
}

.gateway-node {
  background: #0ea5e9;
}

.storage-node {
  background: #0ea5e9;
}

.alarm-node {
  background: #ef4444;
}

.situation-node {
  background: #64748b;
}

.verify-node {
  background: #64748b;
}

.close-node {
  background: #64748b;
}

.lifecycle-line {
  width: 2px;
  height: 24px;
  background: #cbd5e1;
  margin: 4px 0;
}

.lifecycle-label {
  font-size: 12px;
  color: #64748b;
  margin-top: 4px;
  text-align: center;
}

/* 告警资料 */
.info-section {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  border: 1px solid #e2e8f0;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 16px;
}

.section-title .el-icon {
  color: #3b82f6;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-item label {
  font-size: 12px;
  color: #64748b;
}

.info-item span {
  font-size: 14px;
  color: #1e293b;
}

.level-tag {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
  color: #fff;
}

.level-tag.danger {
  background: #ef4444;
}

.level-tag.warning {
  background: #f97316;
}

.level-tag.success {
  background: #10b981;
}

.level-tag.info {
  background: #3b82f6;
}

.alarm-desc-section {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #e2e8f0;
}

.alarm-desc-section label {
  display: block;
  font-size: 12px;
  color: #64748b;
  margin-bottom: 8px;
}

.alarm-desc-section p {
  font-size: 14px;
  color: #334155;
  line-height: 1.6;
  margin: 0;
}

/* 数据依据 */
.data-section {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  border: 1px solid #e2e8f0;
}

.sensor-selector {
  margin-bottom: 16px;
}

.chart-container {
  height: 250px;
  background: #f8fafc;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.chart-placeholder {
  text-align: center;
  color: #94a3b8;
}

.chart-placeholder .chart-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.chart-placeholder p {
  margin: 0 0 8px 0;
  font-size: 14px;
}

.chart-placeholder .hint {
  font-size: 12px;
  color: #cbd5e1;
}

/* 右侧面板 */
.right-panel {
  width: 240px;
}

.timeline-content {
  height: 150px;
  background: #fff;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
}

.timeline-empty {
  text-align: center;
  color: #94a3b8;
}

.timeline-empty .el-icon {
  font-size: 32px;
  margin-bottom: 8px;
}

.timeline-empty p {
  margin: 0;
  font-size: 12px;
}

.installation-content {
  height: 200px;
  background: #fff;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.img-placeholder {
  text-align: center;
  color: #94a3b8;
}

.img-placeholder .el-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.img-placeholder p {
  margin: 0;
  font-size: 12px;
}

.mt-3 {
  margin-top: 12px;
}

/* 底部操作面板 */
.feedback-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background: #f8fafc;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
}

.response-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.response-info .label {
  color: #64748b;
  font-size: 14px;
}

.response-info .value {
  color: #1e293b;
  font-weight: 500;
  font-size: 14px;
}

.response-info .ml-4 {
  margin-left: 16px;
}

.feedback-footer .action-buttons {
  display: flex;
  gap: 8px;
}

/* 响应式 */
@media (max-width: 1024px) {
  .info-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .side-panel {
    width: 150px;
  }
  
  .right-panel {
    width: 180px;
  }
}
</style>
