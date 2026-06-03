<template>
<<<<<<< Updated upstream
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
                <el-option label="待处理" value="pending" />
                <el-option label="处理中" value="processing" />
                <el-option label="已处理" value="handled" />
                <el-option label="误报" value="false_alarm" />
                <el-option label="已销警" value="closed" />
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
=======
  <div class="realtime-alarm-container">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-left">
        <h2>实时告警</h2>
      </div>
      <div class="header-right">
        <el-button type="primary" @click="handleExport">导出</el-button>
      </div>
    </div>

    <!-- 查询条件 -->
    <div class="search-bar">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="隐患点名称">
          <el-input
            v-model="searchForm.hazardPointName"
            placeholder="请输入隐患点名称"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="响应人员">
          <el-input
            v-model="searchForm.responseUserName"
            placeholder="请输入响应人员"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="告警时间">
          <el-date-picker
            v-model="alarmTimeRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item label="告警次数">
          <el-input-number
            v-model="searchForm.alarmCountMin"
            :min="0"
            placeholder="最小"
            style="width: 120px"
          />
          <span style="margin: 0 5px">-</span>
          <el-input-number
            v-model="searchForm.alarmCountMax"
            :min="0"
            placeholder="最大"
            style="width: 120px"
          />
        </el-form-item>
        <el-form-item label="告警等级">
          <el-select v-model="searchForm.alarmLevel" placeholder="请选择" clearable style="width: 150px">
            <el-option label="一级(红色)" :value="1" />
            <el-option label="二级(橙色)" :value="2" />
            <el-option label="三级(黄色)" :value="3" />
            <el-option label="四级(蓝色)" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="告警类型">
          <el-select v-model="searchForm.alarmType" placeholder="请选择" clearable style="width: 150px">
            <el-option label="阈值预警" :value="1" />
            <el-option label="综合预警" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="警情状态">
          <el-select v-model="searchForm.alarmStatus" placeholder="请选择" clearable style="width: 150px">
            <el-option label="待处理" :value="0" />
            <el-option label="处理中" :value="1" />
            <el-option label="已处理" :value="2" />
            <el-option label="误报" :value="3" />
            <el-option label="已销警" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 表格 -->
    <div class="table-container">
      <el-table
        :data="tableData"
        border
        stripe
        highlight-current-row
        @row-click="handleRowClick"
        v-loading="loading"
      >
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="hazardPointName" label="隐患点名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="alarmLevelName" label="告警等级" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getAlarmLevelType(row.alarmLevel)" effect="dark">
              {{ row.alarmLevelName }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="firstAlarmTime" label="首次告警时间" width="170" align="center">
          <template #default="{ row }">
            {{ formatDateTime(row.firstAlarmTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="lastAlarmTime" label="最后告警时间" width="170" align="center">
          <template #default="{ row }">
            {{ formatDateTime(row.lastAlarmTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="alarmCount" label="告警次数" width="100" align="center">
          <template #default="{ row }">
            <el-link type="primary" @click.stop="handleShowAlarmList(row)">
              {{ row.alarmCount }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column prop="alarmTypeName" label="告警类型" width="120" align="center" />
        <el-table-column prop="alarmStatusName" label="警情状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getAlarmStatusType(row.alarmStatus)">
              {{ row.alarmStatusName }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="responseUserName" label="响应人员" width="100" align="center" show-overflow-tooltip />
        <el-table-column prop="responseTime" label="响应时间" width="170" align="center">
          <template #default="{ row }">
            {{ formatDateTime(row.responseTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click.stop="handleView(row)">查看</el-button>
            <el-button link type="success" size="small" @click.stop="handleFeedback(row)">反馈</el-button>
            <el-button link type="warning" size="small" @click.stop="handleFalseAlarm(row)">误报</el-button>
            <el-button link type="danger" size="small" @click.stop="handleClearAlarm(row)">销警</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 分页 -->
    <div class="pagination-container">
      <el-pagination
        v-model:current-page="pagination.pageNum"
        v-model:page-size="pagination.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
      />
    </div>

    <!-- 告警详情弹窗 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="告警详情"
      width="700px"
      destroy-on-close
    >
      <el-descriptions :column="2" border v-if="currentAlarm">
        <el-descriptions-item label="隐患点名称">{{ currentAlarm.hazardPointName }}</el-descriptions-item>
        <el-descriptions-item label="告警等级">
          <el-tag :type="getAlarmLevelType(currentAlarm.alarmLevel)" effect="dark">
            {{ currentAlarm.alarmLevelName }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="告警类型">{{ currentAlarm.alarmTypeName }}</el-descriptions-item>
        <el-descriptions-item label="警情状态">
          <el-tag :type="getAlarmStatusType(currentAlarm.alarmStatus)">
            {{ currentAlarm.alarmStatusName }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="首次告警时间">{{ formatDateTime(currentAlarm.firstAlarmTime) }}</el-descriptions-item>
        <el-descriptions-item label="最后告警时间">{{ formatDateTime(currentAlarm.lastAlarmTime) }}</el-descriptions-item>
        <el-descriptions-item label="告警次数">{{ currentAlarm.alarmCount }}</el-descriptions-item>
        <el-descriptions-item label="响应人员">{{ currentAlarm.responseUserName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="响应时间" :span="2">{{ formatDateTime(currentAlarm.responseTime) }}</el-descriptions-item>
        <el-descriptions-item label="告警详情" :span="2">{{ currentAlarm.alarmDetail || '-' }}</el-descriptions-item>
      </el-descriptions>
>>>>>>> Stashed changes
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

<<<<<<< Updated upstream
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
    <el-dialog v-model="feedbackDialogVisible" title="告警反馈" width="600px">
      <el-form :model="feedbackForm" label-width="100px">
        <el-form-item label="反馈内容">
          <el-input v-model="feedbackForm.content" type="textarea" :rows="5" placeholder="请输入反馈内容" />
=======
    <!-- 反馈弹窗 -->
    <el-dialog
      v-model="feedbackDialogVisible"
      title="告警反馈"
      width="500px"
      destroy-on-close
    >
      <el-form :model="feedbackForm" label-width="100px">
        <el-form-item label="告警信息">
          <span>{{ currentAlarm?.hazardPointName }} - {{ currentAlarm?.alarmLevelName }}</span>
        </el-form-item>
        <el-form-item label="反馈内容" required>
          <el-input
            v-model="feedbackForm.feedback"
            type="textarea"
            :rows="4"
            placeholder="请输入反馈内容"
          />
>>>>>>> Stashed changes
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="feedbackDialogVisible = false">取消</el-button>
<<<<<<< Updated upstream
        <el-button type="primary" @click="submitFeedback">确定</el-button>
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
=======
        <el-button type="primary" @click="submitFeedback">提交</el-button>
      </template>
    </el-dialog>

    <!-- 告警列表弹窗 -->
    <el-dialog
      v-model="alarmListDialogVisible"
      title="告警记录"
      width="800px"
      destroy-on-close
    >
      <el-table :data="alarmListData" border stripe max-height="400">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="alarmLevelName" label="告警等级" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getAlarmLevelType(row.alarmLevel)" effect="dark" size="small">
              {{ row.alarmLevelName }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="alarmTypeName" label="告警类型" width="100" align="center" />
        <el-table-column prop="firstAlarmTime" label="告警时间" width="170" align="center">
          <template #default="{ row }">
            {{ formatDateTime(row.firstAlarmTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="alarmStatusName" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="getAlarmStatusType(row.alarmStatus)" size="small">
              {{ row.alarmStatusName }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="responseUserName" label="响应人员" width="100" align="center" />
      </el-table>
>>>>>>> Stashed changes
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
<<<<<<< Updated upstream
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { View, ChatDotRound, Warning, CircleClose, Download } from '@element-plus/icons-vue'

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
=======
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getRealtimeAlarmPage,
  getRealtimeAlarmDetail,
  getRealtimeAlarmByHazardPoint,
  feedbackAlarm,
  markAsFalseAlarm,
  clearAlarm,
  exportRealtimeAlarms,
  type RealtimeAlarmListParams,
  type RealtimeAlarmDetail
} from '@/api/realtimeAlarm'

const loading = ref(false)
const tableData = ref<RealtimeAlarmDetail[]>([])
const currentAlarm = ref<RealtimeAlarmDetail | null>(null)
const alarmListData = ref<RealtimeAlarmDetail[]>([])

const searchForm = reactive<Partial<RealtimeAlarmListParams>>({
  hazardPointName: '',
  responseUserName: '',
  alarmCountMin: undefined,
  alarmCountMax: undefined,
  alarmLevel: undefined,
  alarmType: undefined,
  alarmStatus: undefined
})

const alarmTimeRange = ref<[string, string] | null>(null)

const pagination = reactive({
  pageNum: 1,
>>>>>>> Stashed changes
  pageSize: 10,
  total: 0
})

<<<<<<< Updated upstream
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
    hazardPointName: '山体滑坡点C-12',
    alarmLevel: '3',
    firstAlarmTime: '2024-05-28 16:20:00',
    lastAlarmTime: '2024-06-02 20:30:00',
    alarmCount: 25,
    alarmType: 'threshold',
    status: 'handled',
    responderName: '李四',
    responseTime: '2024-05-28 17:00:00',
    alarmContent: '山体倾斜角度超过阈值5度，当前值为6.2度',
    alarmList: [
      { alarmTime: '2024-05-28 16:20:00', alarmLevel: '3', alarmContent: '山体倾斜角度5.1度' },
      { alarmTime: '2024-06-02 20:30:00', alarmLevel: '3', alarmContent: '山体倾斜角度6.2度' }
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
    status: 'false_alarm',
    responderName: '王五',
    responseTime: '2024-06-01 08:00:00',
    alarmContent: '桥墩沉降超过阈值2mm，经核查为传感器故障',
    alarmList: [
      { alarmTime: '2024-06-01 00:00:00', alarmLevel: '4', alarmContent: '桥墩沉降2.1mm' },
      { alarmTime: '2024-06-02 00:00:00', alarmLevel: '4', alarmContent: '桥墩沉降2.3mm' }
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
    status: 'closed',
    responderName: '赵六',
    responseTime: '2024-06-01 20:00:00',
    alarmContent: '水库水位超过警戒水位50cm，当前已降至安全范围',
    alarmList: [
      { alarmTime: '2024-05-25 06:00:00', alarmLevel: '2', alarmContent: '水库水位超限30cm' },
      { alarmTime: '2024-06-01 18:00:00', alarmLevel: '2', alarmContent: '水库水位超限50cm' }
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
  if (queryParams.alarmCountMin !== null) {
    result = result.filter(item => item.alarmCount >= queryParams.alarmCountMin)
  }
  if (queryParams.alarmCountMax !== null) {
    result = result.filter(item => item.alarmCount <= queryParams.alarmCountMax)
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
    'processing': 'warning',
    'handled': 'success',
    'false_alarm': 'info',
    'closed': 'info'
  }
  return map[status] || 'info'
}

// 获取状态文本
const getStatusText = (status: string) => {
  const map: Record<string, string> = {
    'pending': '待处理',
    'processing': '处理中',
    'handled': '已处理',
    'false_alarm': '误报',
    'closed': '已销警'
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
  currentRow.value = selectedRows.value
  feedbackForm.content = ''
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
=======
const detailDialogVisible = ref(false)
const feedbackDialogVisible = ref(false)
const alarmListDialogVisible = ref(false)

const feedbackForm = reactive({
  feedback: ''
})

const formatDateTime = (dateTime: string | null) => {
  if (!dateTime) return '-'
  return dateTime
}

const getAlarmLevelType = (level: number) => {
  const types: Record<number, string> = {
    1: 'danger',
    2: 'warning',
    3: 'warning',
    4: 'info'
  }
  return types[level] || 'info'
}

const getAlarmStatusType = (status: number) => {
  const types: Record<number, string> = {
    0: 'danger',
    1: 'warning',
    2: 'success',
    3: 'info',
    4: 'success'
  }
  return types[status] || 'info'
}

const loadData = async () => {
  loading.value = true
  try {
    const params: RealtimeAlarmListParams = {
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      ...searchForm
    }

    if (alarmTimeRange.value && alarmTimeRange.value.length === 2) {
      params.alarmStartTime = alarmTimeRange.value[0]
      params.alarmEndTime = alarmTimeRange.value[1]
    }

    const response: any = await getRealtimeAlarmPage(params)
    if (response.code === 200) {
      tableData.value = response.data.rows || []
      pagination.total = response.data.total || 0
    } else {
      ElMessage.error(response.msg || '加载数据失败')
    }
  } catch (error) {
    console.error('加载数据失败:', error)
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.pageNum = 1
  loadData()
}

const handleReset = () => {
  Object.keys(searchForm).forEach(key => {
    (searchForm as any)[key] = undefined
  })
  alarmTimeRange.value = null
  pagination.pageNum = 1
  loadData()
}

const handleSizeChange = (size: number) => {
  pagination.pageSize = size
  pagination.pageNum = 1
  loadData()
}

const handlePageChange = (page: number) => {
  pagination.pageNum = page
  loadData()
}

const handleRowClick = (row: RealtimeAlarmDetail) => {
  handleView(row)
}

const handleView = async (row: RealtimeAlarmDetail) => {
  try {
    const response: any = await getRealtimeAlarmDetail(row.id.toString())
    if (response.code === 200) {
      currentAlarm.value = response.data
      detailDialogVisible.value = true
    } else {
      ElMessage.error(response.msg || '获取详情失败')
    }
  } catch (error) {
    console.error('获取详情失败:', error)
    ElMessage.error('获取详情失败')
  }
}

const handleFeedback = (row: RealtimeAlarmDetail) => {
  currentAlarm.value = row
  feedbackForm.feedback = ''
  feedbackDialogVisible.value = true
}

const submitFeedback = async () => {
  if (!feedbackForm.feedback.trim()) {
    ElMessage.warning('请输入反馈内容')
    return
  }

  try {
    const response: any = await feedbackAlarm({
      alarmId: currentAlarm.value!.id,
      feedback: feedbackForm.feedback,
      responseUserId: 1,
      responseUserName: '当前用户'
    })

    if (response.code === 200) {
      ElMessage.success('反馈成功')
      feedbackDialogVisible.value = false
      loadData()
    } else {
      ElMessage.error(response.msg || '反馈失败')
    }
  } catch (error) {
    console.error('反馈失败:', error)
    ElMessage.error('反馈失败')
  }
}

const handleFalseAlarm = async (row: RealtimeAlarmDetail) => {
  try {
    await ElMessageBox.confirm(
      `确定要将"${row.hazardPointName}"的告警标记为误报吗？`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const response: any = await markAsFalseAlarm(row.id.toString())
    if (response.code === 200) {
      ElMessage.success('标记误报成功')
      loadData()
    } else {
      ElMessage.error(response.msg || '操作失败')
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('操作失败:', error)
      ElMessage.error('操作失败')
    }
  }
}

const handleClearAlarm = async (row: RealtimeAlarmDetail) => {
  try {
    await ElMessageBox.confirm(
      `确定要销除"${row.hazardPointName}"的告警吗？`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const response: any = await clearAlarm(row.id.toString())
    if (response.code === 200) {
      ElMessage.success('销警成功')
      loadData()
    } else {
      ElMessage.error(response.msg || '操作失败')
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('操作失败:', error)
      ElMessage.error('操作失败')
    }
  }
}

const handleShowAlarmList = async (row: RealtimeAlarmDetail) => {
  try {
    const response: any = await getRealtimeAlarmByHazardPoint(row.hazardPointId.toString())
    if (response.code === 200) {
      alarmListData.value = response.data || []
      alarmListDialogVisible.value = true
    } else {
      ElMessage.error(response.msg || '获取告警列表失败')
    }
  } catch (error) {
    console.error('获取告警列表失败:', error)
    ElMessage.error('获取告警列表失败')
  }
}

const handleExport = async () => {
  try {
    await ElMessageBox.confirm('确定要导出告警数据吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info'
    })

    const params: any = { ...searchForm }
    if (alarmTimeRange.value && alarmTimeRange.value.length === 2) {
      params.alarmStartTime = alarmTimeRange.value[0]
      params.alarmEndTime = alarmTimeRange.value[1]
    }

    const blob: any = await exportRealtimeAlarms(params)
    const url = window.URL.createObjectURL(new Blob([blob]))
    const link = document.createElement('a')
    link.href = url
    link.download = `实时告警_${new Date().toISOString().slice(0, 10)}.xlsx`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)

    ElMessage.success('导出成功')
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('导出失败:', error)
      ElMessage.error('导出失败')
    }
  }
}

onMounted(() => {
  loadData()
})
>>>>>>> Stashed changes
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
<<<<<<< Updated upstream
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
=======
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
>>>>>>> Stashed changes
}
</style>
