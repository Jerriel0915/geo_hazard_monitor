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
          <el-table-column label="操作" width="280" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click.stop="handleView(row)">
                <el-icon><View /></el-icon>
                查看
              </el-button>
              <el-button type="success" link size="small" @click.stop="handleFeedback(row)">
                <el-icon><ChatDotRound /></el-icon>
                反馈
              </el-button>
              <el-button type="warning" link size="small" @click.stop="handleFalseAlarm(row)">
                <el-icon><Warning /></el-icon>
                误报
              </el-button>
              <el-button type="danger" link size="small" @click.stop="handleCloseAlarm(row)">
                <el-icon><CircleClose /></el-icon>
                销警
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

    <!-- 反馈弹窗 -->
    <el-dialog v-model="feedbackDialogVisible" title="告警反馈" width="850px">
      <div v-if="currentRow && !Array.isArray(currentRow)" class="feedback-container">
        <!-- 告警基本信息 -->
        <div class="feedback-header">
          <div class="header-info">
            <div class="info-item">
              <span class="label">告警编号：</span>
              <span class="value">{{ currentRow.id }}</span>
            </div>
            <div class="info-item">
              <span class="label">隐患点名称：</span>
              <span class="value">{{ currentRow.hazardPointName }}</span>
            </div>
            <div class="info-item">
              <span class="label">告警等级：</span>
              <el-tag :type="getAlarmLevelType(currentRow.alarmLevel)" size="large">{{ getAlarmLevelText(currentRow.alarmLevel) }}({{ getAlarmLevelDesc(currentRow.alarmLevel) }})</el-tag>
            </div>
            <div class="info-item">
              <span class="label">告警类型：</span>
              <span class="value">{{ getAlarmTypeText(currentRow.alarmType) }}</span>
            </div>
          </div>
        </div>

        <!-- 事件生命周期 -->
        <div class="feedback-section">
          <div class="section-title">
            <el-icon><Clock /></el-icon>
            事件生命周期
          </div>
          <div class="life-cycle">
            <div class="cycle-step completed">
              <div class="step-icon">
                <el-icon><Server /></el-icon>
              </div>
              <div class="step-label">设备接入</div>
            </div>
            <div class="cycle-line completed"></div>
            <div class="cycle-step completed">
              <div class="step-icon">
                <el-icon><Database /></el-icon>
              </div>
              <div class="step-label">数据存储</div>
            </div>
            <div class="cycle-line completed"></div>
            <div class="cycle-step completed active">
              <div class="step-icon alarm">
                <el-icon><Bell /></el-icon>
              </div>
              <div class="step-label">警报级</div>
            </div>
            <div class="cycle-line" :class="{ completed: currentRow.status === 'processing' }"></div>
            <div class="cycle-step" :class="{ completed: currentRow.status === 'processing' }">
              <div class="step-icon">
                <el-icon><Search /></el-icon>
              </div>
              <div class="step-label">情况核查</div>
            </div>
            <div class="cycle-line"></div>
            <div class="cycle-step">
              <div class="step-icon">
                <el-icon><CheckCircle /></el-icon>
              </div>
              <div class="step-label">核查完成</div>
            </div>
            <div class="cycle-line"></div>
            <div class="cycle-step">
              <div class="step-icon">
                <el-icon><XCircle /></el-icon>
              </div>
              <div class="step-label">关闭事件</div>
            </div>
          </div>
        </div>

        <!-- 基本信息 -->
        <div class="feedback-section">
          <div class="section-title">
            <el-icon><FileText /></el-icon>
            基本信息
          </div>
          <el-descriptions :column="3" border size="small">
            <el-descriptions-item label="首次告警时间">{{ currentRow.firstAlarmTime }}</el-descriptions-item>
            <el-descriptions-item label="最后告警时间">{{ currentRow.lastAlarmTime }}</el-descriptions-item>
            <el-descriptions-item label="告警次数">{{ currentRow.alarmCount }} 次</el-descriptions-item>
            <el-descriptions-item label="当前状态">
              <el-tag :type="getStatusType(currentRow.status)">{{ getStatusText(currentRow.status) }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="响应人员">{{ currentRow.responderName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="响应时间">{{ currentRow.responseTime || '-' }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 告警内容 -->
        <div class="feedback-section">
          <div class="section-title">
            <el-icon><AlertTriangle /></el-icon>
            告警描述
          </div>
          <div class="alarm-content">{{ currentRow.alarmContent }}</div>
        </div>

        <!-- 反馈表单 -->
        <div class="feedback-section">
          <div class="section-title">
            <el-icon><MessageSquare /></el-icon>
            处理反馈
          </div>
          <el-form :model="feedbackForm" label-width="100px">
            <el-form-item label="处理方式">
              <el-select v-model="feedbackForm.handleType" placeholder="请选择处理方式">
                <el-option label="现场勘查" value="on_site" />
                <el-option label="远程监控" value="remote" />
                <el-option label="技术支持" value="technical" />
                <el-option label="专家会诊" value="expert" />
                <el-option label="其他" value="other" />
              </el-select>
            </el-form-item>
            <el-form-item label="处理状态">
              <el-select v-model="feedbackForm.status" placeholder="请选择处理状态">
                <el-option label="已响应，正在处理" value="processing" />
                <el-option label="已到达现场" value="on_site" />
                <el-option label="已处理完成" value="completed" />
                <el-option label="需要进一步评估" value="pending" />
              </el-select>
            </el-form-item>
            <el-form-item label="处理人员">
              <el-input v-model="feedbackForm.personnel" placeholder="请输入处理人员" />
            </el-form-item>
            <el-form-item label="反馈内容">
              <el-input v-model="feedbackForm.content" type="textarea" :rows="4" placeholder="请输入处理反馈内容，包括现场情况、采取的措施、后续计划等" />
            </el-form-item>
            <el-form-item label="附件">
              <el-upload
                class="upload-demo"
                action="#"
                :auto-upload="false"
                :file-list="feedbackForm.attachments"
                multiple
                accept=".jpg,.png,.pdf,.doc,.docx"
              >
                <el-button size="small" type="primary">点击上传附件</el-button>
                <template #tip>
                  <div class="el-upload__tip">支持 jpg、png、pdf、doc、docx 格式</div>
                </template>
              </el-upload>
            </el-form-item>
          </el-form>
        </div>
      </div>
      
      <!-- 批量反馈 -->
      <div v-else-if="Array.isArray(currentRow)" class="batch-feedback">
        <div class="batch-info">
          <el-icon><List /></el-icon>
          <span>已选择 {{ currentRow.length }} 条告警记录进行批量反馈</span>
        </div>
        <el-form :model="feedbackForm" label-width="100px">
          <el-form-item label="处理方式">
            <el-select v-model="feedbackForm.handleType" placeholder="请选择处理方式">
              <el-option label="现场勘查" value="on_site" />
              <el-option label="远程监控" value="remote" />
              <el-option label="技术支持" value="technical" />
              <el-option label="专家会诊" value="expert" />
              <el-option label="其他" value="other" />
            </el-select>
          </el-form-item>
          <el-form-item label="处理人员">
            <el-input v-model="feedbackForm.personnel" placeholder="请输入处理人员" />
          </el-form-item>
          <el-form-item label="反馈内容">
            <el-input v-model="feedbackForm.content" type="textarea" :rows="5" placeholder="请输入批量处理反馈内容" />
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <el-button @click="feedbackDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitFeedback">提交反馈</el-button>
      </template>
    </el-dialog>

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
import {ChatDotRound, CircleClose, Clock, Database, Download, List, MessageSquare, Search, Server, View, Warning, AlertTriangle, Bell, CheckCircle, FileText, XCircle} from '@element-plus/icons-vue'

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

// 当前行
const currentRow = ref<any>(null)
const currentAlarmList = ref<any[]>([])
const feedbackForm = reactive({
  content: '',
  handleType: '',
  status: '',
  personnel: '',
  attachments: [] as any[]
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

// 获取告警等级描述
const getAlarmLevelDesc = (level: string) => {
  const map: Record<string, string> = {
    '1': '特别严重',
    '2': '严重',
    '3': '较严重',
    '4': '一般'
  }
  return map[level] || ''
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
  feedbackForm.handleType = ''
  feedbackForm.status = ''
  feedbackForm.personnel = ''
  feedbackForm.attachments = []
  feedbackDialogVisible.value = true
}

// 批量反馈
const handleBatchFeedback = () => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请先选择要反馈的记录')
    return
  }
  currentRow.value = selectedRows.value
  feedbackForm.content = ''
  feedbackForm.handleType = ''
  feedbackForm.personnel = ''
  feedbackDialogVisible.value = true
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
  padding: 16px;
}

.feedback-header {
  background: linear-gradient(135deg, #1e3a5f 0%, #2d5a87 100%);
  border-radius: 8px;
  padding: 16px 20px;
  margin-bottom: 16px;
}

.header-info {
  display: flex;
  flex-wrap: wrap;
  gap: 24px;
  align-items: center;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.info-item .label {
  color: rgba(255, 255, 255, 0.8);
  font-size: 14px;
}

.info-item .value {
  color: #fff;
  font-size: 14px;
  font-weight: 500;
}

.feedback-section {
  margin-bottom: 20px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: bold;
  color: #1e3a5f;
  margin-bottom: 12px;
  padding-left: 8px;
  border-left: 3px solid #2d5a87;
}

/* 事件生命周期 */
.life-cycle {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px;
  background: #f8fafc;
  border-radius: 8px;
  flex-wrap: wrap;
}

.cycle-step {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 80px;
}

.step-icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: #e2e8f0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #64748b;
  font-size: 20px;
  transition: all 0.3s;
}

.step-icon.alarm {
  background: #ef4444;
  color: #fff;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0% {
    box-shadow: 0 0 0 0 rgba(239, 68, 68, 0.7);
  }
  70% {
    box-shadow: 0 0 0 10px rgba(239, 68, 68, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(239, 68, 68, 0);
  }
}

.cycle-step.completed .step-icon {
  background: #10b981;
  color: #fff;
}

.cycle-step.active .step-icon {
  background: #f59e0b;
  color: #fff;
}

.step-label {
  font-size: 12px;
  color: #64748b;
  text-align: center;
}

.cycle-step.completed .step-label {
  color: #10b981;
}

.cycle-step.active .step-label {
  color: #f59e0b;
  font-weight: bold;
}

.cycle-line {
  flex: 1;
  height: 3px;
  background: #e2e8f0;
  margin: 0 8px;
  min-width: 20px;
}

.cycle-line.completed {
  background: linear-gradient(90deg, #10b981, #34d399);
}

/* 告警内容 */
.alarm-content {
  background: #fff3cd;
  border: 1px solid #ffeeba;
  border-radius: 8px;
  padding: 16px;
  font-size: 14px;
  color: #856404;
  line-height: 1.6;
}

/* 批量反馈 */
.batch-feedback {
  padding: 16px;
}

.batch-info {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #64748b;
  margin-bottom: 16px;
  padding: 12px;
  background: #f8fafc;
  border-radius: 8px;
}

.batch-info el-icon {
  color: #3b82f6;
}

/* 表单样式 */
.upload-demo {
  margin-top: 8px;
}

.el-upload__tip {
  margin-top: 8px;
}
</style>
