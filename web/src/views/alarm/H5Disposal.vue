<template>
  <div class="h5-disposal-page">
    <!-- 顶部导航 -->
    <div class="page-header">
      <div class="header-title">{{ alarmData?.hazardPointName || '告警处置' }}</div>
    </div>

    <!-- 内容区域 -->
    <div class="page-content" v-if="alarmData">
      <!-- 告警信息卡片 -->
      <div class="info-card alarm-info">
        <div class="card-header compact">
          <div class="card-icon alarm">
            <el-icon><WarnTriangleFilled /></el-icon>
          </div>
          <span class="card-title">告警信息</span>
        </div>
        <div class="card-body">
          <div class="info-row">
            <div class="info-item">
              <span class="info-label">告警等级</span>
              <span class="info-value level-badge" :class="getLevelClass(alarmData.alarmLevel)">
                {{ alarmLevelRange }}
              </span>
            </div>
            <div class="info-item">
              <span class="info-label">告警类型</span>
              <span class="info-value">{{ getAlarmTypeText(alarmData.alarmType) }}</span>
            </div>
          </div>
          <div class="info-row">
            <div class="info-item full">
              <span class="info-label">告警时间</span>
              <span class="info-value">{{ alarmData.firstTriggerTime }} ~ {{ alarmData.lastTriggerTime }}</span>
            </div>
          </div>
          <div class="info-row">
            <div class="info-item full">
              <span class="info-label">警情来源</span>
              <div class="source-wrapper">
                <span class="info-value source">
                  <el-icon><MapLocation /></el-icon>
                  {{ alarmData.hazardPointName }}
                  <el-icon><Monitor /></el-icon>
                  {{ alarmData.deviceName || '未知设备' }}
                </span>
                <div class="map-icon-btn" @click="handleOpenMap">
                  <el-icon><MapLocation /></el-icon>
                </div>
              </div>
            </div>
          </div>
          <div class="info-row">
            <div class="info-item full">
              <span class="info-label">告警描述</span>
              <p class="info-desc">{{ alarmData.alarmMessage }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 项目信息卡片 -->
      <div class="info-card project-info">
        <div class="card-header compact">
          <div class="card-icon project">
            <el-icon><Location /></el-icon>
          </div>
          <span class="card-title">项目信息</span>
        </div>
        <div class="card-body">
          <div class="info-row">
            <div class="info-item full">
              <span class="info-label">地理位置</span>
              <span class="info-value">{{ hazardPointLocation }}</span>
            </div>
          </div>
          <div class="info-row">
            <div class="info-item full">
              <span class="info-label">所属分组</span>
              <span class="info-value">{{ hazardPointData?.groupName || '未分组' }}</span>
            </div>
          </div>
          <div class="info-row">
            <div class="info-item full">
              <span class="info-label">隐患点描述</span>
              <p class="info-desc">{{ hazardPointData?.description || '暂无描述' }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 支撑数据卡片 -->
      <div class="info-card support-data">
        <!-- Tab切换 -->
        <div class="data-tabs">
          <div 
            class="tab-item" 
            :class="{ active: activeTab === 'monitor' }" 
            @click="switchTab('monitor')"
          >
            <el-icon><TrendCharts /></el-icon>
            监测数据
          </div>
          <div
            class="tab-item"
            :class="{ active: activeTab === 'alarm' }"
            @click="switchTab('alarm')"
          >
            <el-icon><Bell /></el-icon>
            告警次数
            <span class="tab-badge">{{ alarmData.triggerCount || 0 }}</span>
          </div>
          <div
            class="tab-item"
            :class="{ active: activeTab === 'notify' }"
            @click="switchTab('notify')"
          >
            <el-icon><ChatDotRound /></el-icon>
            通知记录
          </div>
          <div
            class="tab-item"
            :class="{ active: activeTab === 'feedback' }"
            @click="switchTab('feedback')"
          >
            <el-icon><ChatLineRound /></el-icon>
            反馈记录
          </div>
        </div>

        <div class="card-body">
          <!-- 监测数据 -->
          <div v-show="activeTab === 'monitor'" class="tab-content">
            <div ref="chartRef" class="chart-container"></div>
          </div>

          <!-- 告警次数 -->
          <div v-show="activeTab === 'alarm'" class="tab-content">
            <div class="search-bar">
              <el-input
                v-model="alarmFilter.desc"
                placeholder="描述模糊查询"
                clearable
                class="search-input"
              />
              <el-date-picker
                v-model="alarmFilter.timeRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始"
                end-placeholder="结束"
                value-format="YYYY-MM-DD"
                class="search-date"
              />
            </div>
            <div class="table-wrapper">
              <div class="mobile-table">
                <div
                  class="table-row"
                  v-for="(item, index) in paginatedAlarmList"
                  :key="index"
                >
                  <div class="row-header">
                    <span class="row-time">{{ item.triggerTime }}</span>
                    <el-tag :type="getAlarmLevelType(item.alarmLevel)" size="small">
                      {{ getAlarmLevelText(item.alarmLevel) }}
                    </el-tag>
                  </div>
                  <div class="row-content">{{ item.alarmMessage || '-' }}</div>
                </div>
                <div v-if="filteredAlarmList.length === 0" class="empty-tip">暂无告警记录</div>
              </div>
            </div>
            <div class="pagination-wrapper" v-if="filteredAlarmList.length > alarmPageSize">
              <el-pagination
                v-model:current-page="alarmCurrentPage"
                :page-size="alarmPageSize"
                :total="filteredAlarmList.length"
                layout="prev, pager, next"
                small
              />
            </div>
          </div>

          <!-- 通知记录 -->
          <div v-show="activeTab === 'notify'" class="tab-content">
            <div class="search-bar">
              <el-input
                v-model="notifyFilter.account"
                placeholder="接收人/渠道查询"
                clearable
                class="search-input"
              />
              <el-date-picker
                v-model="notifyFilter.timeRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始"
                end-placeholder="结束"
                value-format="YYYY-MM-DD"
                class="search-date"
              />
            </div>
            <div class="table-wrapper">
              <div class="mobile-table">
                <div
                  class="table-row notify-row"
                  v-for="(item, index) in paginatedNotifyList"
                  :key="index"
                >
                  <div class="row-header">
                    <span class="row-time">{{ item.createTime }}</span>
                    <el-tag :type="getNotifyStatusType(item.status)" size="small">
                      {{ getNotifyStatusText(item.status) }}
                    </el-tag>
                  </div>
                  <div class="row-info">
                    <span class="channel-tag">{{ getChannelText(item.channel) }}</span>
                    <span class="target-text">{{ item.recipientName || item.recipientPhone || '-' }}</span>
                  </div>
                  <div class="row-content">{{ item.content || '-' }}</div>
                </div>
                <div v-if="filteredNotifyList.length === 0" class="empty-tip">暂无通知记录</div>
              </div>
            </div>
            <div class="pagination-wrapper" v-if="filteredNotifyList.length > notifyPageSize">
              <el-pagination
                v-model:current-page="notifyCurrentPage"
                :page-size="notifyPageSize"
                :total="filteredNotifyList.length"
                layout="prev, pager, next"
                small
              />
            </div>
          </div>

          <!-- 反馈记录 -->
          <div v-show="activeTab === 'feedback'" class="tab-content">
            <div class="search-bar">
              <el-input
                v-model="feedbackFilter.text"
                placeholder="操作人/描述查询"
                clearable
                class="search-input"
              />
              <el-date-picker
                v-model="feedbackFilter.timeRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始"
                end-placeholder="结束"
                value-format="YYYY-MM-DD"
                class="search-date"
              />
            </div>
            <div class="table-wrapper">
              <div class="mobile-table">
                <div
                  class="table-row feedback-row"
                  v-for="(item, index) in paginatedFeedbackList"
                  :key="index"
                >
                  <div class="row-header">
                    <span class="row-time">{{ item.createTime }}</span>
                    <el-tag :type="getFeedbackActionType(item.actionType)" size="small">
                      {{ getFeedbackActionText(item.actionType) }}
                    </el-tag>
                  </div>
                  <div class="row-info">
                    <span class="channel-tag operator">操作人</span>
                    <span class="target-text">{{ item.operator || '-' }}</span>
                  </div>
                  <div class="row-content">{{ item.description || '-' }}</div>
                </div>
                <div v-if="filteredFeedbackList.length === 0" class="empty-tip">暂无反馈记录</div>
              </div>
            </div>
            <div class="pagination-wrapper" v-if="filteredFeedbackList.length > feedbackPageSize">
              <el-pagination
                v-model:current-page="feedbackCurrentPage"
                :page-size="feedbackPageSize"
                :total="filteredFeedbackList.length"
                layout="prev, pager, next"
                small
              />
            </div>
          </div>
        </div>
      </div>

      <!-- 时间线 -->
      <div class="info-card timeline-card">
        <div class="card-header">
          <div class="card-icon timeline">
            <el-icon><Clock /></el-icon>
          </div>
          <span class="card-title">时间线</span>
        </div>
        <div class="card-body">
          <div class="timeline" v-if="timelineData.length > 0">
            <div class="timeline-item" v-for="(item, index) in visibleTimeline" :key="index">
              <div class="timeline-dot" :class="item.type"></div>
              <div class="timeline-line" v-if="index < visibleTimeline.length - 1"></div>
              <div class="timeline-content">
                <div class="timeline-time" v-if="item.time">{{ item.time }}</div>
                <div class="timeline-label">
                  {{ item.label }}
                  <span v-if="item.operator" class="timeline-operator">/ {{ item.operator }}</span>
                </div>
                <div class="timeline-desc" v-if="item.description">{{ item.description }}</div>
              </div>
            </div>
            <div v-if="timelineData.length > timelineCollapsedLimit" class="timeline-toggle" @click="timelineExpanded = !timelineExpanded">
              {{ timelineExpanded ? '收起' : `展开全部 (${timelineData.length})` }}
              <el-icon><ArrowDown v-if="!timelineExpanded" /><ArrowUp v-else /></el-icon>
            </div>
          </div>
          <div class="timeline-empty" v-else>
            <el-icon><Clock /></el-icon>
            <p>暂无事件记录</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 地图弹窗 -->
    <el-dialog
      v-model="mapDialogVisible"
      title="位置信息"
      width="90%"
      :close-on-click-modal="false"
      class="h5-map-dialog"
    >
      <div class="map-container">
        <div class="map-placeholder">
          <el-icon><MapLocation /></el-icon>
          <div class="map-location">{{ hazardPointLocation }}</div>
          <div class="map-coords">
            <span>经度: {{ hazardPointData?.longitude ?? '-' }}°</span>
            <span>纬度: {{ hazardPointData?.latitude ?? '-' }}°</span>
          </div>
          <div class="map-marker">
            <div class="marker-pin"></div>
            <div class="marker-label">{{ alarmData?.hazardPointName }}</div>
          </div>
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" size="small" @click="handleNavigate">导航</el-button>
          <el-button size="small" @click="mapDialogVisible = false">关闭</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 反馈弹窗 -->
    <el-dialog
      v-model="feedbackDialogVisible"
      title="反馈"
      width="90%"
      :close-on-click-modal="false"
      class="h5-feedback-dialog"
    >
      <div class="feedback-form">
        <div class="form-item">
          <div class="form-label">反馈内容</div>
          <el-input
            v-model="feedbackForm.content"
            type="textarea"
            :rows="3"
            placeholder="请输入反馈内容（非必填）..."
            resize="none"
          />
        </div>
        <div class="form-item">
          <div class="form-label">反馈文件</div>
          <div class="upload-area">
            <div 
              v-for="(file, index) in feedbackForm.files" 
              :key="index" 
              class="uploaded-file"
            >
              <div class="file-icon" :class="getFileClass(file)">
                <el-icon>{{ getFileIcon(file) }}</el-icon>
              </div>
              <span class="file-name">{{ file.name }}</span>
              <div class="file-remove" @click="removeFile(index)">
                <el-icon><CircleClose /></el-icon>
              </div>
            </div>
            <div 
              v-if="feedbackForm.files.length < maxFiles" 
              class="upload-btn"
              @click="triggerFileInput"
            >
              <el-icon><Plus /></el-icon>
              <span>添加文件</span>
            </div>
            <input 
              type="file" 
              multiple 
              accept="image/*,.pdf,.doc,.docx,.xls,.xlsx"
              class="file-input"
              ref="fileInputRef"
              @change="handleFileSelect"
            />
          </div>
          <span class="form-hint">支持图片、PDF、Word、Excel格式，最多上传{{ maxFiles }}个文件</span>
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button size="small" @click="feedbackDialogVisible = false">取消</el-button>
          <el-button type="primary" size="small" @click="handleSubmitFeedback">提交</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 底部操作栏 -->
    <div class="page-footer" v-if="showActions">
      <div class="action-buttons">
        <el-button type="primary" size="small" @click="handleFeedback">
          <el-icon><ChatDotRound /></el-icon> 反馈
        </el-button>
        <el-button type="warning" size="small" @click="handleFalseAlarm">
          <el-icon><Warning /></el-icon> 误报
        </el-button>
        <el-button type="danger" size="small" @click="handleCloseAlarm">
          <el-icon><CircleClose /></el-icon> 消警
        </el-button>
      </div>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="page-loading">
      <el-icon class="is-loading"><Loading /></el-icon>
      <span>加载中...</span>
    </div>

    <!-- 加载失败 -->
    <div v-if="loadError" class="page-error">
      <el-icon><WarningFilled /></el-icon>
      <p>{{ loadError }}</p>
      <el-button type="primary" size="small" @click="loadAll">重试</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import {computed, nextTick, onMounted, onUnmounted, reactive, ref} from 'vue'
import {useRoute} from 'vue-router'
import {ElMessage, ElMessageBox} from 'element-plus'
import {
  ArrowDown,
  ArrowUp,
  Bell,
  ChatDotRound,
  ChatLineRound,
  CircleClose,
  Clock,
  Document,
  Files,
  Loading,
  Location,
  MapLocation,
  Monitor,
  Picture,
  Plus,
  TrendCharts,
  Warning,
  WarningFilled,
  WarnTriangleFilled
} from '@element-plus/icons-vue'
import echarts from '@/utils/echarts'
import request from '@/utils/request'
import {
  getAlarmRecordDetail,
  getTriggerDetails,
  getActionLogs,
  getAlarmNotifications,
  disposeAlarm,
  type AlarmRecordItem,
  type AlarmRecordTriggerDetail,
  type AlarmRecordActionLog,
  type AlarmNotificationItem,
} from '@/api/alarm'
import {getChartData, type ChartData} from '@/api/monitorData'
import {getHazardPointDetail} from '@/api/hazardPoint'

const route = useRoute()

// 告警数据
const alarmData = ref<AlarmRecordItem | null>(null)
const hazardPointData = ref<any>(null)
const triggerDetails = ref<AlarmRecordTriggerDetail[]>([])
const notifyRecords = ref<AlarmNotificationItem[]>([])
const actionLogs = ref<AlarmRecordActionLog[]>([])
const chartSeriesData = ref<ChartData[]>([])
const timelineData = ref<TimelineNode[]>([])
const loading = ref(false)
const loadError = ref('')

// 时间线默认折叠：仅展示最近 5 条，点击「展开全部」查看完整
const timelineCollapsedLimit = 5
const timelineExpanded = ref(false)
const visibleTimeline = computed(() =>
  timelineExpanded.value || timelineData.value.length <= timelineCollapsedLimit
    ? timelineData.value
    : timelineData.value.slice(0, timelineCollapsedLimit)
)

interface TimelineNode {
  time: string
  label: string
  description: string
  operator: string
  type: string
}

const activeTab = ref('monitor')
const chartRef = ref<HTMLDivElement | null>(null)
let chartInstance: echarts.ECharts | null = null

// 告警次数筛选
const alarmFilter = reactive({ desc: '', timeRange: [] as string[] })
const alarmCurrentPage = ref(1)
const alarmPageSize = 5

// 通知记录筛选
const notifyFilter = reactive({ account: '', timeRange: [] as string[] })
const notifyCurrentPage = ref(1)
const notifyPageSize = 5

// 反馈记录筛选
const feedbackFilter = reactive({ text: '', timeRange: [] as string[] })
const feedbackCurrentPage = ref(1)
const feedbackPageSize = 5

// 地图弹窗
const mapDialogVisible = ref(false)

// 反馈弹窗
const feedbackDialogVisible = ref(false)
const feedbackForm = reactive({
  content: '',
  files: [] as File[]
})
const feedbackSubmitting = ref(false)

const maxFiles = 3
const fileInputRef = ref<HTMLInputElement | null>(null)

// 计算属性
const alarmId = computed(() => {
  const id = route.params.id || route.query.id
  return id ? Number(id) : null
})

const alarmLevelRange = computed(() => {
  if (!alarmData.value) return '-'
  const d: any = alarmData.value
  const minLevel = d.minAlarmLevel ?? d.alarmLevel
  const maxLevel = d.maxAlarmLevel ?? d.alarmLevel
  const minText = getAlarmLevelText(minLevel)
  const maxText = getAlarmLevelText(maxLevel)
  return minText === maxText ? minText : `${minText}-${maxText}`
})

// 底部操作栏仅在 待处理(1)/处理中(2) 时显示
const showActions = computed(() => {
  const s = Number(alarmData.value?.status)
  return s === 1 || s === 2
})

// 隐患点位置文本
const hazardPointLocation = computed(() => {
  const hp = hazardPointData.value
  if (!hp) return '暂无位置信息'
  if (hp.longitude && hp.latitude) return `经度 ${hp.longitude}, 纬度 ${hp.latitude}`
  return hp.address || '暂无位置信息'
})

// 告警次数过滤
const filteredAlarmList = computed(() => {
  let list = [...triggerDetails.value]
  if (alarmFilter.desc) {
    const kw = alarmFilter.desc.toLowerCase()
    list = list.filter(a => (a.alarmMessage || '').toLowerCase().includes(kw))
  }
  if (alarmFilter.timeRange?.length === 2) {
    const [s, e] = alarmFilter.timeRange
    list = list.filter(a => (a.triggerTime || '') >= s && (a.triggerTime || '') <= e + ' 23:59:59')
  }
  return [...list].sort((a, b) => (b.triggerTime || '').localeCompare(a.triggerTime || ''))
})

const paginatedAlarmList = computed(() => {
  const start = (alarmCurrentPage.value - 1) * alarmPageSize
  return filteredAlarmList.value.slice(start, start + alarmPageSize)
})

const filteredNotifyList = computed(() => {
  let list = [...notifyRecords.value]
  if (notifyFilter.account) {
    const kw = notifyFilter.account.toLowerCase()
    list = list.filter(n =>
      (n.recipientName || '').toLowerCase().includes(kw) ||
      (n.channel || '').toLowerCase().includes(kw) ||
      (n.content || '').toLowerCase().includes(kw))
  }
  if (notifyFilter.timeRange?.length === 2) {
    const [s, e] = notifyFilter.timeRange
    list = list.filter(n => (n.createTime || '') >= s && (n.createTime || '') <= e + ' 23:59:59')
  }
  return [...list].sort((a, b) => (b.createTime || '').localeCompare(a.createTime || ''))
})

const paginatedNotifyList = computed(() => {
  const start = (notifyCurrentPage.value - 1) * notifyPageSize
  return filteredNotifyList.value.slice(start, start + notifyPageSize)
})

// 反馈记录：过滤 FEEDBACK / DISPOSE_CLOSE / DISPOSE_FALSE_ALARM
const filteredFeedbackList = computed(() => {
  let list = actionLogs.value.filter(x =>
    ['FEEDBACK', 'DISPOSE_CLOSE', 'DISPOSE_FALSE_ALARM'].includes(x.actionType))
  if (feedbackFilter.text) {
    const kw = feedbackFilter.text.toLowerCase()
    list = list.filter(x =>
      (x.operator || '').toLowerCase().includes(kw) ||
      (x.description || '').toLowerCase().includes(kw))
  }
  if (feedbackFilter.timeRange?.length === 2) {
    const [s, e] = feedbackFilter.timeRange
    list = list.filter(x => (x.createTime || '') >= s && (x.createTime || '') <= e + ' 23:59:59')
  }
  return [...list].sort((a, b) => (b.createTime || '').localeCompare(a.createTime || ''))
})

const paginatedFeedbackList = computed(() => {
  const start = (feedbackCurrentPage.value - 1) * feedbackPageSize
  return filteredFeedbackList.value.slice(start, start + feedbackPageSize)
})

// 方法
const switchTab = (tab: string) => {
  activeTab.value = tab
  if (tab === 'monitor') {
    nextTick(() => initChart())
  }
}

// 图表相关 — 对接 getChartData
const pad2 = (n: number) => String(n).padStart(2, '0')
const fmtDateTime = (d: Date) =>
  `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())} ${pad2(d.getHours())}:${pad2(d.getMinutes())}:${pad2(d.getSeconds())}`

/** 加载监测曲线：首次告警时间前3天 ~ 当天24点 */
const loadChartData = async () => {
  const rec: any = alarmData.value
  if (!rec) { chartSeriesData.value = []; return }
  const hpId = Number(rec.hazardPointId)
  if (!hpId) { chartSeriesData.value = []; return }

  const firstTime = new Date(rec.firstTriggerTime)
  if (isNaN(firstTime.getTime())) { chartSeriesData.value = []; return }

  const startTime = new Date(firstTime)
  startTime.setDate(startTime.getDate() - 3)
  const endTime = new Date()
  endTime.setHours(23, 59, 59, 0)

  const params: {
    hazardPointId: number; startTime: string; endTime: string
    deviceId?: number; sensorId?: number
  } = {
    hazardPointId: hpId,
    startTime: fmtDateTime(startTime),
    endTime: fmtDateTime(endTime),
  }
  if (rec.deviceId) params.deviceId = Number(rec.deviceId)
  if (rec.sensorId) params.sensorId = Number(rec.sensorId)

  try {
    const data = await getChartData(params)
    chartSeriesData.value = Array.isArray(data) ? data : []
  } catch {
    chartSeriesData.value = []
  }
}

/** 将触发明细时间匹配到图表 x 轴最近的数据点索引 */
const findAlarmIndices = (labels: string[]): number[] => {
  const indices: number[] = []
  for (const td of triggerDetails.value) {
    if (!td.triggerTime) continue
    const triggerTs = new Date(td.triggerTime.replace(/-/g, '/')).getTime()
    if (isNaN(triggerTs)) continue
    let bestIdx = -1
    let bestDiff = Infinity
    labels.forEach((label, idx) => {
      const labelTs = new Date(label.replace(/-/g, '/')).getTime()
      if (isNaN(labelTs)) return
      const diff = Math.abs(labelTs - triggerTs)
      if (diff < bestDiff) { bestDiff = diff; bestIdx = idx }
    })
    if (bestIdx >= 0 && !indices.includes(bestIdx)) indices.push(bestIdx)
  }
  return indices
}

const initChart = () => {
  if (!chartRef.value) return
  if (chartInstance) chartInstance.dispose()
  chartInstance = echarts.init(chartRef.value)
  updateChart()
}

const updateChart = () => {
  if (!chartInstance) return
  const series = chartSeriesData.value
  if (!series || series.length === 0) {
    chartInstance.setOption({
      title: { text: '暂无监测数据', left: 'center', top: 'center', textStyle: { color: '#909399', fontSize: 13 } },
    } as echarts.EChartsCoreOption, true)
    return
  }

  const main = series[0]
  const labels = main.labels || []
  const alarmIndices = findAlarmIndices(labels)

  chartInstance.setOption({
    title: { text: main.seriesName || main.attrName || '监测数据', left: 'left', textStyle: { fontSize: 12, color: '#606266' } },
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        const p = params[0]
        if (!p) return ''
        const isAlarm = alarmIndices.includes(p.dataIndex)
        return `<div style="padding:6px"><div style="font-weight:bold;margin-bottom:2px">${p.name}</div><div>${main.seriesName || main.attrName}: <span style="color:${isAlarm ? '#f56c6c' : '#409eff'}">${p.value}${main.unit || ''}</span></div>${isAlarm ? '<div style="color:#f56c6c;margin-top:2px">⚠️ 告警触发点</div>' : ''}</div>`
      }
    },
    grid: { left: '12%', right: '5%', bottom: '15%', top: '12%' },
    xAxis: {
      type: 'category', data: labels,
      axisLabel: { rotate: 45, fontSize: 10, color: '#666' },
      axisLine: { lineStyle: { color: '#ddd' } }
    },
    yAxis: {
      type: 'value', name: main.unit || '监测值',
      nameTextStyle: { fontSize: 10 },
      axisLabel: { fontSize: 10, color: '#666' },
      axisLine: { lineStyle: { color: '#ddd' } },
      splitLine: { lineStyle: { color: '#f0f0f0' } }
    },
    series: [{
      name: main.seriesName || main.attrName,
      type: 'line',
      data: main.values,
      smooth: true, symbol: 'circle',
      symbolSize: (_v: number, params: any) => alarmIndices.includes(params.dataIndex) ? 10 : 5,
      itemStyle: { color: (params: any) => alarmIndices.includes(params.dataIndex) ? '#f56c6c' : '#409eff' },
      lineStyle: { width: 2, color: '#409eff' },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(64,158,255,.3)' },
          { offset: 1, color: 'rgba(64,158,255,.05)' }
        ])
      },
      markPoint: {
        data: alarmIndices.map((idx, i) => ({
          name: `告警点${i + 1}`,
          coord: [idx, main.values[idx]],
          value: main.values[idx],
          itemStyle: { color: '#f56c6c' },
          symbol: 'pin', symbolSize: 30,
          label: { show: true, formatter: '⚠', fontSize: 10 }
        }))
      }
    }]
  } as echarts.EChartsCoreOption, true)
}

const handleResize = () => { chartInstance?.resize() }

// 工具函数
const getLevelClass = (level: number | string | undefined) => {
  const map: Record<string, string> = {
    '1': 'level-1',
    '2': 'level-2',
    '3': 'level-3',
    '4': 'level-4'
  }
  return map[String(level)] || 'level-4'
}

const getAlarmLevelType = (level: number | string | undefined) => {
  const map: Record<string, string> = {
    '1': 'danger',
    '2': 'warning',
    '3': 'success',
    '4': 'info'
  }
  return map[String(level)] || 'info'
}

const getAlarmLevelText = (level: number | string | undefined) => {
  const map: Record<string, string> = {
    '1': '一级',
    '2': '二级',
    '3': '三级',
    '4': '四级'
  }
  return map[String(level)] || String(level ?? '-')
}

const getAlarmTypeText = (type: string) => {
  const map: Record<string, string> = {
    'THRESHOLD': '阈值预警',
    'COMPREHENSIVE': '综合预警'
  }
  return map[type] || type
}

// 通知状态文案/类型
const getNotifyStatusText = (status: number | undefined) => {
  const n = Number(status)
  return ({ 1: '待发送', 2: '已发送', 3: '失败', 4: '接收人无效', 5: '渠道未配置' } as Record<number, string>)[n] || '待发送'
}
const getNotifyStatusType = (status: number | undefined) => {
  const n = Number(status)
  return ({ 1: 'info', 2: 'success', 3: 'danger', 4: 'warning', 5: 'warning' } as Record<number, string>)[n] || 'info'
}
const getChannelText = (channel: string | undefined) => {
  const map: Record<string, string> = { SYSTEM: '系统', SMS: '短信', EMAIL: '邮件' }
  return map[channel || ''] || channel || '-'
}

// 反馈动作类型文案/颜色（FEEDBACK/DISPOSE_CLOSE/DISPOSE_FALSE_ALARM）
const getFeedbackActionText = (actionType: string) => {
  const map: Record<string, string> = {
    FEEDBACK: '处置反馈',
    DISPOSE_CLOSE: '告警销警',
    DISPOSE_FALSE_ALARM: '标记误报',
  }
  return map[actionType] || actionType
}
const getFeedbackActionType = (actionType: string) => {
  const map: Record<string, string> = {
    FEEDBACK: 'primary',
    DISPOSE_CLOSE: 'success',
    DISPOSE_FALSE_ALARM: 'warning',
  }
  return map[actionType] || 'info'
}

/** 数值等级 → 中文颜色名称 (1=红色 2=橙色 3=黄色 4=蓝色) */
const getLevelName = (val: string | undefined) => {
  const n = Number(val)
  return ({ 1: '红色', 2: '橙色', 3: '黄色', 4: '蓝色' } as Record<number, string>)[n] || val || ''
}

/** 由动作日志构造时间线（CURRENT/ENDED 当前状态节点始终置顶，其余按时间倒序） */
function buildTimeline(logs: AlarmRecordActionLog[]): TimelineNode[] {
  return [...logs].sort((a, b) => {
    if (a.actionType === 'CURRENT' || a.actionType === 'ENDED') return -1
    if (b.actionType === 'CURRENT' || b.actionType === 'ENDED') return 1
    return (b.createTime || '').localeCompare(a.createTime || '') || ((a.id ?? 0) - (b.id ?? 0))
  }).map(log => {
    const typeMap: Record<string, string> = {
      CURRENT: 'current', ENDED: 'ended',
      CREATE: 'trigger', RE_TRIGGER: 'trigger', LEVEL_CHANGE: 'trigger',
      NOTIFY: 'notify',
      FEEDBACK: 'dispose', DISPOSE_CLOSE: 'dispose', DISPOSE_FALSE_ALARM: 'dispose',
    }
    const labelMap: Record<string, string> = {
      CURRENT: '当前', ENDED: '结束',
      CREATE: '告警创建', RE_TRIGGER: '告警触发', LEVEL_CHANGE: '等级变化',
      FEEDBACK: '处置反馈', DISPOSE_CLOSE: '告警销警',
      DISPOSE_FALSE_ALARM: '标记误报', NOTIFY: '通知发送',
    }
    const label = labelMap[log.actionType] || log.actionType
    const description = log.actionType === 'LEVEL_CHANGE'
      ? `${getLevelName(log.fromValue)}→${getLevelName(log.toValue)}`
      : (log.description || '')
    return {
      time: log.createTime || '',
      label,
      description,
      operator: log.operator || '',
      type: typeMap[log.actionType] || 'system',
    }
  })
}

// ───────── 数据加载 ─────────

const loadAll = async () => {
  if (!alarmId.value) {
    loadError.value = '缺少告警 ID'
    return
  }
  loading.value = true
  loadError.value = ''
  try {
    const id = alarmId.value
    const [d, t, l, n] = await Promise.all([
      getAlarmRecordDetail(id),
      getTriggerDetails(id),
      getActionLogs(id),
      getAlarmNotifications(id),
    ])
    const detailData: any = (d as any).data ?? d
    alarmData.value = detailData ?? null
    triggerDetails.value = (t as any).data ?? t ?? []
    const rawLogs: AlarmRecordActionLog[] = (l as any).data ?? l ?? []
    notifyRecords.value = (n as any).data ?? n ?? []

    // 在 action_log 头部插入"当前状态"节点
    const isEnded = [3, 4].includes(Number(detailData?.status))
    const currentLog = {
      id: 0,
      alarmRecordId: id,
      actionType: isEnded ? 'ENDED' : 'CURRENT',
      createTime: '',
      description: '',
      remarks: '',
      operator: '',
    } as AlarmRecordActionLog
    actionLogs.value = rawLogs
    timelineData.value = buildTimeline([currentLog, ...rawLogs])

    // 拉取隐患点详情
    if (detailData?.hazardPointId) {
      try {
        const hpRes: any = await getHazardPointDetail(String(detailData.hazardPointId))
        hazardPointData.value = hpRes?.data ?? hpRes ?? null
      } catch {
        hazardPointData.value = null
      }
    }

    // 加载监测曲线
    await loadChartData()

    // 初始化图表
    nextTick(() => initChart())
  } catch (e: any) {
    console.error('加载告警详情失败:', e)
    loadError.value = e?.message || '加载失败，请重试'
    alarmData.value = null
  } finally {
    loading.value = false
  }
}

// ───────── 处置操作 ─────────

const handleFeedback = () => {
  feedbackForm.content = ''
  feedbackForm.files = []
  feedbackDialogVisible.value = true
}

const triggerFileInput = () => {
  fileInputRef.value?.click()
}

const handleFileSelect = (event: Event) => {
  const target = event.target as HTMLInputElement
  const files = target.files
  if (!files) return

  const remaining = maxFiles - feedbackForm.files.length
  for (let i = 0; i < Math.min(files.length, remaining); i++) {
    feedbackForm.files.push(files[i])
  }
  target.value = ''
}

const getFileClass = (file: File) => {
  const ext = file.name.split('.').pop()?.toLowerCase()
  if (['jpg', 'jpeg', 'png', 'gif', 'webp'].includes(ext || '')) return 'image'
  if (['pdf'].includes(ext || '')) return 'pdf'
  if (['doc', 'docx'].includes(ext || '')) return 'word'
  if (['xls', 'xlsx'].includes(ext || '')) return 'excel'
  return 'other'
}

const getFileIcon = (file: File) => {
  const ext = file.name.split('.').pop()?.toLowerCase()
  if (['jpg', 'jpeg', 'png', 'gif', 'webp'].includes(ext || '')) return Picture
  if (['pdf'].includes(ext || '')) return Document
  return Files
}

const removeFile = (index: number) => {
  feedbackForm.files.splice(index, 1)
}

/** 上传附件到 /common/upload，返回逗号分隔的 fileName 列表 */
async function uploadAttachments(files: File[]): Promise<string | undefined> {
  if (!files || files.length === 0) return undefined
  const fileNames: string[] = []
  for (const f of files) {
    const fd = new FormData()
    fd.append('file', f)
    try {
      const res = await request.post('/common/upload', fd, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
      const resData = (res as any).data
      if (resData?.fileName) fileNames.push(resData.fileName)
    } catch (e) {
      console.error('附件上传失败:', e)
    }
  }
  return fileNames.length > 0 ? fileNames.join(',') : undefined
}

const handleSubmitFeedback = async () => {
  if (!alarmData.value) return
  if (feedbackSubmitting.value) return
  feedbackSubmitting.value = true
  try {
    const hasFiles = feedbackForm.files.length > 0
    const attachments = await uploadAttachments(feedbackForm.files)
    if (hasFiles && !attachments) {
      ElMessage.warning('附件上传失败，请重试')
      return
    }
    await disposeAlarm(alarmData.value.id, {
      status: 2,
      description: feedbackForm.content,
      attachments,
      remarks: feedbackForm.content,
    })
    ElMessage.success('反馈提交成功')
    feedbackDialogVisible.value = false
    await loadAll()
  } catch (e: any) {
    ElMessage.error(e?.message || '反馈提交失败')
  } finally {
    feedbackSubmitting.value = false
  }
}

const handleFalseAlarm = async () => {
  if (!alarmData.value) return
  try {
    await ElMessageBox.confirm('确定将此告警标记为误报吗？', '误报确认', { type: 'warning' })
    await disposeAlarm(alarmData.value.id, { status: 4 })
    ElMessage.success('已标记为误报')
    await loadAll()
  } catch (e: any) {
    if (e !== 'cancel' && e !== 'close') {
      ElMessage.error(e?.message || '标记误报失败')
    }
  }
}

const handleCloseAlarm = async () => {
  if (!alarmData.value) return
  try {
    await ElMessageBox.confirm('确定要销警此告警吗？', '销警确认', { type: 'warning' })
    await disposeAlarm(alarmData.value.id, { status: 3 })
    ElMessage.success('消警成功')
    await loadAll()
  } catch (e: any) {
    if (e !== 'cancel' && e !== 'close') {
      ElMessage.error(e?.message || '销警失败')
    }
  }
}

const handleOpenMap = () => {
  mapDialogVisible.value = true
}

const handleNavigate = () => {
  const lat = hazardPointData.value?.latitude || ''
  const lng = hazardPointData.value?.longitude || ''
  const name = alarmData.value?.hazardPointName || '目的地'
  if (lat && lng) {
    // 尝试唤醒地图 APP（高德/百度通用 scheme）
    window.open(`https://uri.amap.com/navigation?to=${lng},${lat},${encodeURIComponent(name)}&mode=car`, '_blank')
  } else {
    ElMessage.info(`正在打开导航到 ${name}`)
  }
  mapDialogVisible.value = false
}

// 生命周期
onMounted(() => {
  loadAll()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  chartInstance?.dispose()
})
</script>

<style scoped>
.h5-disposal-page {
  min-height: 100vh;
  background: #f5f7fa;
  display: flex;
  flex-direction: column;
}

/* 顶部导航 */
.page-header {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 50px;
  background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);
  color: #fff;
  padding: 0 16px;
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-title {
  font-size: 18px;
  font-weight: 600;
}

/* 内容区域 */
.page-content {
  flex: 1;
  padding: 12px;
  padding-bottom: 180px;
  overflow-y: auto;
}

/* 信息卡片 */
.info-card {
  background: #fff;
  border-radius: 12px;
  margin-bottom: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  background: #fafafa;
}

.card-header.compact {
  padding-top: 7px;
  padding-bottom: 7px;
}

.card-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
}

.card-icon.alarm {
  background: #fef0f0;
  color: #f56c6c;
}

.card-icon.project {
  background: #e8f5e9;
  color: #67c23a;
}

.card-icon.data {
  background: #e3f2fd;
  color: #409eff;
}

.card-icon.timeline {
  background: #fff3e0;
  color: #e6a23c;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.card-body {
  padding: 12px 16px;
}

/* 信息行 */
.info-row {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}

.info-row:last-child {
  margin-bottom: 0;
}

.info-item {
  flex: 1;
  min-width: 0;
}

.info-item.full {
  flex: none;
  width: 100%;
}

.info-label {
  font-size: 12px;
  color: #909399;
  display: block;
  margin-bottom: 4px;
}

.info-value {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
}

.info-value.source {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
}

.info-value.source .el-icon {
  color: #909399;
  font-size: 14px;
}

.source-wrapper {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.map-icon-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #67c23a;
  border-radius: 50%;
  cursor: pointer;
  color: #fff;
  font-size: 16px;
  transition: all 0.2s;
}

.map-icon-btn:active {
  transform: scale(0.95);
}

.info-desc {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  margin: 0;
  background: #f8f9fa;
  padding: 8px 12px;
  border-radius: 6px;
}

/* 等级标签 */
.level-badge {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 4px;
  color: #fff;
  font-size: 12px;
}

.level-badge.level-1 {
  background: #f56c6c;
}

.level-badge.level-2 {
  background: #e6a23c;
}

.level-badge.level-3 {
  background: #67c23a;
}

.level-badge.level-4 {
  background: #909399;
}

/* Tab切换 */
.data-tabs {
  display: flex;
  border-bottom: 1px solid #f0f0f0;
  background: #fafafa;
}

.tab-item {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 12px 0;
  font-size: 13px;
  color: #606266;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  transition: all 0.2s;
}

.tab-item.active {
  color: #409eff;
  border-bottom-color: #409eff;
  font-weight: 600;
  background: #fff;
}

.tab-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  background: #f56c6c;
  color: #fff;
  font-size: 11px;
  border-radius: 9px;
  margin-left: 4px;
  font-weight: 500;
}

/* Tab 内空提示 */
.empty-tip {
  text-align: center;
  color: #909399;
  font-size: 13px;
  padding: 24px 0;
}

/* 反馈记录行 */
.feedback-row .row-info {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.channel-tag.operator {
  color: #67c23a;
  background: #f0f9eb;
}

/* 加载/失败占位 */
.page-loading,
.page-error {
  position: fixed;
  inset: 50px 0 0 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  background: #f5f7fa;
  color: #909399;
  font-size: 14px;
  z-index: 50;
}

.page-loading .el-icon {
  font-size: 32px;
  color: #409eff;
}

.page-error .el-icon {
  font-size: 40px;
  color: #f56c6c;
}

.page-error p {
  margin: 0;
}

.chart-container {
  width: 100%;
  height: 220px;
}

/* 搜索栏 */
.search-bar {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 12px;
}

.search-input {
  width: 100%;
}

.search-date {
  width: 100%;
}

/* 移动端表格 */
.table-wrapper {
  max-height: 300px;
  overflow-y: auto;
}

.mobile-table {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.table-row {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 10px 12px;
}

.row-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.row-time {
  font-size: 12px;
  color: #909399;
}

.row-content {
  font-size: 13px;
  color: #606266;
  line-height: 1.5;
}

.notify-row .row-info {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.channel-tag {
  font-size: 11px;
  color: #409eff;
  background: #e3f2fd;
  padding: 2px 6px;
  border-radius: 3px;
}

.target-text {
  font-size: 12px;
  color: #606266;
}

/* 分页 */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 12px;
}

/* 时间线 */
.timeline {
  position: relative;
  padding-left: 20px;
}

.timeline-item {
  position: relative;
  padding-bottom: 16px;
}

.timeline-item:last-child {
  padding-bottom: 0;
}

.timeline-dot {
  position: absolute;
  left: -20px;
  top: 4px;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #d9d9d9;
}

.timeline-dot.trigger {
  background: #f56c6c;
}

.timeline-dot.system {
  background: #409eff;
}

.timeline-dot.confirm {
  background: #67c23a;
}

.timeline-dot.notify {
  background: #409eff;
}

.timeline-dot.dispose {
  background: #67c23a;
}

.timeline-dot.current {
  background: #409eff;
  box-shadow: 0 0 0 4px rgba(64, 158, 255, 0.18);
  animation: timeline-pulse 1.8s ease-in-out infinite;
}

.timeline-dot.ended {
  background: #909399;
}

@keyframes timeline-pulse {
  0%, 100% { box-shadow: 0 0 0 4px rgba(64, 158, 255, 0.18); }
  50%      { box-shadow: 0 0 0 7px rgba(64, 158, 255, 0.08); }
}

.timeline-line {
  position: absolute;
  left: -15px;
  top: 16px;
  width: 2px;
  height: calc(100% - 8px);
  background: #e4e7ed;
}

.timeline-content {
  padding-left: 8px;
}

.timeline-time {
  font-size: 12px;
  color: #909399;
  margin-bottom: 2px;
}

.timeline-label {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  line-height: 1.5;
}

.timeline-operator {
  font-weight: 400;
  color: #909399;
}

.timeline-desc {
  font-size: 12px;
  color: #606266;
  margin-top: 2px;
  line-height: 1.5;
}

.timeline-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 24px 0;
  color: #909399;
}

/* 折叠状态下的展开/收起按钮 */
.timeline-toggle {
  margin-top: 8px;
  padding-left: 20px;
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #409eff;
  cursor: pointer;
  user-select: none;
}

.timeline-toggle:hover {
  opacity: 0.85;
}

.timeline-empty .el-icon {
  font-size: 32px;
  margin-bottom: 8px;
}

.timeline-empty p {
  font-size: 13px;
  margin: 0;
}

/* 底部操作栏 */
.page-footer {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: #fff;
  padding: 12px 16px;
  box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.08);
  z-index: 100;
}

.action-buttons {
  display: flex;
  gap: 8px;
  justify-content: space-between;
}

.action-buttons .el-button {
  flex: 1;
}

/* 通知弹窗 */
.notify-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-label {
  font-size: 13px;
  color: #303133;
  font-weight: 500;
}

.channel-group {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.channel-group .el-checkbox {
  margin-right: 0;
}

.user-select {
  width: 100%;
}

.user-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.user-name {
  font-weight: 500;
}

.user-role {
  font-size: 12px;
  color: #909399;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

/* 地图弹窗 */
.map-container {
  width: 100%;
  height: 280px;
  background: linear-gradient(180deg, #e8f5f3 0%, #d0e6e3 100%);
  border-radius: 8px;
  overflow: hidden;
  position: relative;
}

.map-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  position: relative;
}

.map-placeholder .el-icon {
  font-size: 48px;
  color: #67c23a;
  margin-bottom: 12px;
}

.map-location {
  font-size: 15px;
  color: #303133;
  font-weight: 500;
  margin-bottom: 8px;
  text-align: center;
  padding: 0 16px;
}

.map-coords {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: #606266;
}

.map-marker {
  position: absolute;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  flex-direction: column;
  align-items: center;
}

.marker-pin {
  width: 24px;
  height: 24px;
  background: #67c23a;
  border-radius: 50% 50% 50% 0;
  transform: rotate(-45deg);
  position: relative;
}

.marker-pin::after {
  content: '';
  width: 10px;
  height: 10px;
  background: #fff;
  border-radius: 50%;
  position: absolute;
  top: 5px;
  left: 5px;
}

.marker-label {
  font-size: 12px;
  color: #303133;
  margin-top: 8px;
  white-space: nowrap;
  background: rgba(255, 255, 255, 0.9);
  padding: 4px 8px;
  border-radius: 4px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

/* 反馈弹窗 */
.feedback-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.upload-area {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  min-height: 48px;
}

.uploaded-file {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  background: #f8f9fa;
  border-radius: 6px;
  max-width: calc(100% - 100px);
}

.file-icon {
  width: 28px;
  height: 28px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
}

.file-icon.image {
  background: #e8f5e9;
  color: #67c23a;
}

.file-icon.pdf {
  background: #fef0f0;
  color: #f56c6c;
}

.file-icon.word {
  background: #e3f2fd;
  color: #409eff;
}

.file-icon.excel {
  background: #fff3e0;
  color: #e6a23c;
}

.file-icon.other {
  background: #f5f7fa;
  color: #909399;
}

.file-name {
  font-size: 12px;
  color: #606266;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-remove {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
  cursor: pointer;
  border-radius: 50%;
  transition: all 0.2s;
}

.file-remove:hover {
  background: #f5f7fa;
  color: #f56c6c;
}

.upload-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 72px;
  height: 72px;
  border: 2px dashed #d9d9d9;
  border-radius: 8px;
  cursor: pointer;
  color: #909399;
  transition: all 0.2s;
}

.upload-btn:hover {
  border-color: #409eff;
  color: #409eff;
}

.upload-btn .el-icon {
  font-size: 20px;
  margin-bottom: 4px;
}

.upload-btn span {
  font-size: 12px;
}

.file-input {
  display: none;
}

.form-hint {
  font-size: 12px;
  color: #909399;
  margin-top: 8px;
  display: block;
}
</style>
