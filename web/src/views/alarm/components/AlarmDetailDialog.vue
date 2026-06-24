<!-- 告警详情/处置 弹窗 - 查看 + 处置共用 -->
<template>
  <el-dialog
      :model-value="modelValue"
      @update:model-value="emit('update:modelValue', $event)"
      @opened="handleDialogOpened"
      :title="dialogTitle"
      width="80%"
      :close-on-click-modal="false"
      destroy-on-close
  >
    <div class="feedback-container" v-if="data">
      <div class="main-content">
        <!-- 左侧生命周期 - 按告警等级加载对应流程图 -->
        <div class="left-section">
          <div class="section-header">
            <span class="icon-wrapper"><Clock /></span>
            <span class="section-title">生命周期</span>
          </div>
          <div class="lifecycle-img-wrap">
            <img v-if="lifecycleImage" :src="lifecycleImage" :alt="`告警等级${alarmLevelText}流程图`" class="lifecycle-img" />
            <div v-else class="lifecycle-empty">暂无流程图</div>
          </div>
        </div>

        <!-- 右侧内容区 -->
        <div class="right-section">
          <!-- 头部卡片：左右结构一行 -->
          <div class="event-header">
            <div class="hd-item">
              <div class="hd-icon hazard"><MapLocation /></div>
              <div class="hd-text">
                <span class="hd-label">隐患点</span>
                <span class="hd-val">{{ data.hazardPointName }}</span>
              </div>
            </div>
            <div class="hd-item">
              <div class="hd-icon device"><Monitor /></div>
              <div class="hd-text">
                <span class="hd-label">设备名</span>
                <span class="hd-val">{{ data.deviceName || '未知设备' }}</span>
              </div>
            </div>
            <div class="hd-item">
              <div class="hd-icon level" :style="levelIconStyle"><WarnTriangleFilled /></div>
              <div class="hd-text">
                <span class="hd-label">告警等级</span>
                <el-tag :style="getAlarmLevelStyle(data.alarmLevel)" style="border: none; width: fit-content;">
                  {{ alarmLevelRange }}
                </el-tag>
              </div>
            </div>
            <div class="hd-item">
              <div class="hd-icon time"><Clock /></div>
              <div class="hd-text">
                <span class="hd-label">发生时间</span>
                <span class="hd-val">{{ formatDuration(data.firstTriggerTime) }}</span>
              </div>
            </div>
            <div class="hd-item alarm-count-item" @click="switchToAlarmTab">
              <div class="hd-icon count"><Bell /></div>
              <div class="hd-text">
                <span class="hd-label">告警次数</span>
                <span class="hd-count">{{ data.triggerCount || 0 }}</span>
              </div>
            </div>
          </div>

          <div class="event-body">
            <!-- 数据区域 - 页签 -->
            <div class="data-section">
              <!-- 基本资料 + 告警描述 + H5 二维码 (合并容器) -->
              <div class="info-summary">
                <div class="info-summary-main">
                  <div class="info-row">
                    <div class="info-item">
                      <span class="info-label">初次告警</span>
                      <span class="info-value">{{ data.firstTriggerTime || '-' }}</span>
                    </div>
                    <div class="info-item">
                      <span class="info-label">最后告警</span>
                      <span class="info-value">{{ data.lastTriggerTime || '-' }}</span>
                    </div>
                    <div class="info-item">
                      <span class="info-label">告警类型</span>
                      <span class="info-value">{{ getAlarmTypeText(data.alarmType) }}</span>
                    </div>
                    <div class="info-item">
                      <span class="info-label">告警次数</span>
                      <span class="info-value count-link" @click="switchToAlarmTab">{{ data.triggerCount || 0 }}</span>
                    </div>
                  </div>
                  <div class="desc-row">
                    <span class="detail-label">告警描述</span>
                    <p>{{ data.alarmMessage || '-' }}</p>
                  </div>
                </div>
                <div class="info-summary-side">
                  <div class="qr-card">
                    <div class="qr-title">H5 现场处置</div>
                    <img v-if="h5QrcodeDataUrl" :src="h5QrcodeDataUrl" alt="H5 告警处置二维码" class="qr-img" />
                    <div v-else class="qr-placeholder">生成中…</div>
                    <el-button size="small" type="primary" plain @click="handleCopyH5Url">
                      <el-icon><CopyDocument /></el-icon>&nbsp;复制链接
                    </el-button>
                  </div>
                </div>
              </div>

              <div class="data-tabs">
                <div class="tab" :class="{ active: activeTab === 'monitor' }" @click="switchToMonitorTab">监测数据</div>
                <div class="tab" :class="{ active: activeTab === 'alarm' }" @click="activeTab = 'alarm'">告警记录</div>
                <div class="tab" :class="{ active: activeTab === 'notify' }" @click="activeTab = 'notify'">通知记录</div>
                <div class="tab" :class="{ active: activeTab === 'disposal' }" @click="activeTab = 'disposal'">处置记录</div>
              </div>

              <!-- 监测数据 -->
              <div v-show="activeTab === 'monitor'" class="tab-content">
                <div ref="chartRef" class="chart-container"></div>
              </div>

              <!-- 告警记录 (API: getTriggerDetails) -->
              <div v-show="activeTab === 'alarm'" class="tab-content">
                <div class="tab-search">
                  <el-input v-model="alarmRecordSearch.description" placeholder="描述模糊查询" size="small" clearable class="tab-sch-inp" />
                  <el-date-picker v-model="alarmRecordSearch.timeRange" type="daterange" range-separator="至"
                                  start-placeholder="开始" end-placeholder="结束" value-format="YYYY-MM-DD HH:mm:ss" size="small" class="tab-sch-date" />
                  <el-button size="small" @click="resetAlarmRecords">重置</el-button>
                </div>
                <el-table :data="filteredAlarmRecords" border stripe size="small" :height="308" empty-text="暂无告警记录">
                  <el-table-column prop="triggerTime" label="告警时间" width="180" />
                  <el-table-column prop="alarmLevel" label="告警等级" width="100">
                    <template #default="{ row }">
                      <el-tag size="small" :style="getAlarmLevelStyle(row.alarmLevel)" style="border: none;">
                        {{ getAlarmLevelText(row.alarmLevel) }}
                      </el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column prop="alarmMessage" label="描述" min-width="220" show-overflow-tooltip />
                </el-table>
              </div>

              <!-- 通知记录 (NOTIFY 动作日志) -->
              <div v-show="activeTab === 'notify'" class="tab-content">
                <div class="tab-search">
                  <el-input v-model="notifyRecordSearch.account" placeholder="接收人/原因查询" size="small" clearable class="tab-sch-inp" />
                  <el-date-picker v-model="notifyRecordSearch.timeRange" type="daterange" range-separator="至"
                                  start-placeholder="开始" end-placeholder="结束" value-format="YYYY-MM-DD HH:mm:ss" size="small" class="tab-sch-date" />
                  <el-button size="small" @click="resetNotifyRecords">重置</el-button>
                </div>
                <el-table :data="filteredNotifyRecords" border stripe size="small" :height="308" empty-text="暂无通知记录">
                  <el-table-column prop="createTime" label="通知时间" width="180" />
                  <el-table-column prop="channel" label="渠道" width="90">
                    <template #default="{ row }">
                      <el-tag size="small" :type="row.channel === 'SMS' ? 'warning' : row.channel === 'EMAIL' ? 'success' : 'primary'">{{ row.channel }}</el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column prop="recipientName" label="接收人" width="120" show-overflow-tooltip />
                  <el-table-column prop="content" label="通知内容" min-width="200" show-overflow-tooltip />
                  <el-table-column prop="status" label="状态" width="80">
                    <template #default="{ row }">
                      <el-tag size="small" :type="row.status === 2 ? 'success' : row.status === 3 ? 'danger' : 'info'">{{ row.status === 2 ? '已发送' : row.status === 3 ? '失败' : '待发送' }}</el-tag>
                    </template>
                  </el-table-column>
                </el-table>
              </div>

              <!-- 处置记录 (API: getActionLogs 过滤 FEEDBACK/DISPOSE_*) -->
              <div v-show="activeTab === 'disposal'" class="tab-content">
                <el-table :data="disposalRecords" border stripe size="small" :height="350" empty-text="暂无处置记录">
                  <el-table-column prop="createTime" label="处置时间" width="180" />
                  <el-table-column prop="operator" label="处置人员" width="120" />
                  <el-table-column prop="description" label="描述" min-width="220" show-overflow-tooltip />
                </el-table>
              </div>
            </div>

            <!-- 处置时间线 (API: getActionLogs 全量) -->
            <div class="timeline-panel">
              <div class="timeline-header">
                <span class="timeline-title">时间线</span>
              </div>
              <div class="timeline-container">
                <div v-if="timelineData.length > 0" class="timeline">
                  <div
                    v-for="(item, index) in timelineData"
                    :key="index"
                    class="timeline-item"
                  >
                    <div class="timeline-dot" :class="item.type"></div>
                    <div v-if="index < timelineData.length - 1" class="timeline-line"></div>
                    <div class="timeline-content">
                      <div v-if="item.time" class="timeline-time">{{ item.time }}</div>
                      <div class="timeline-label">
                        {{ item.label }}
                        <span v-if="item.operator" class="timeline-operator">/ {{ item.operator }}</span>
                      </div>
                      <el-tooltip
                        v-if="item.description"
                        :content="item.description"
                        placement="top"
                        :show-after="300"
                        effect="dark"
                      >
                        <div class="timeline-desc">{{ item.description }}</div>
                      </el-tooltip>
                    </div>
                  </div>
                </div>
                <div v-else class="timeline-empty">
                  <span>暂无事件记录</span>
                </div>
              </div>
            </div>

          </div>
          <!-- /event-body -->

          <!-- 底部操作栏 - 固定在 right-section 底部，仅在待处理/处理中状态显示 -->
          <div v-if="showActions" class="action-right">
            <el-button type="primary" @click="feedbackVisible = true">
              <el-icon><ChatDotRound /></el-icon> 反馈
            </el-button>
            <el-button type="warning" @click="handleFalseAlarm">
              <el-icon><Warning /></el-icon> 误报
            </el-button>
            <el-button type="danger" @click="handleCloseAlarm">
              <el-icon><CircleClose /></el-icon> 消警
            </el-button>
            <el-button type="info" @click="notifyVisible = true">
              <el-icon><Bell /></el-icon> 通知
            </el-button>
            <el-button @click="handleClose">
              <el-icon><Close /></el-icon> 关闭
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <FeedBack v-model:visible="feedbackVisible" @submit="handleFeedbackSubmit" />
    <Notify v-model:visible="notifyVisible" @submit="handleNotifySubmit" />
  </el-dialog>
</template>

<script setup lang="ts">
import FeedBack from '@/components/FeedBack.vue'
import Notify from '@/components/Notify.vue'
import echarts from '@/utils/echarts'
import request from '@/utils/request'
import QRCode from 'qrcode'
import { getChartData, type ChartData } from '@/api/monitorData'
import {
  Bell, ChatDotRound, CircleClose, Clock, Close,
  CopyDocument, MapLocation, Monitor, Warning, WarnTriangleFilled,
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
// 生命周期流程图：按告警等级 1-4 分别对应 flowChart1-4.png
import {
  ALARM_LEVEL_COLORS,
  getActionLogs,
  getAlarmLevelStyle,
  getAlarmNotifications,
  getAlarmRecordDetail,
  getTriggerDetails,
  type AlarmNotificationItem,
  type AlarmRecordActionLog,
  type AlarmRecordItem,
  type AlarmRecordTriggerDetail,
} from '@/api/alarm'
import flowChart1 from '@/assets/images/alarm/flowChart1.png'
import flowChart2 from '@/assets/images/alarm/flowChart2.png'
import flowChart3 from '@/assets/images/alarm/flowChart3.png'
import flowChart4 from '@/assets/images/alarm/flowChart4.png'

// 等级 → 流程图映射
const FLOW_CHART_BY_LEVEL: Record<number, string> = {
  1: flowChart1,
  2: flowChart2,
  3: flowChart3,
  4: flowChart4,
}

const props = defineProps<{
  modelValue: boolean
  data: Record<string, any> | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'submit', payload: { description?: string; attachments?: string; remarks?: string }): void
  (e: 'false-alarm', data: any): void
  (e: 'close-alarm', data: any): void
  (e: 'notify', data: any): void
}>()

const activeTab = ref('monitor')
const chartRef = ref<HTMLDivElement | null>(null)
const feedbackVisible = ref(false)
const notifyVisible = ref(false)
let chartInstance: echarts.ECharts | null = null

const detail = ref<AlarmRecordItem | null>(null)
const triggerDetails = ref<AlarmRecordTriggerDetail[]>([])
const disposalRecords = ref<AlarmRecordActionLog[]>([])
const notifyRecords = ref<AlarmNotificationItem[]>([])
const chartSeriesData = ref<ChartData[]>([])

// H5 处置二维码（弹窗打开时按当前 data.id 生成 base64）
const h5QrcodeDataUrl = ref('')
const h5Url = computed(() => props.data?.id
  ? `${window.location.origin}/h5/disposal/${props.data.id}`
  : '')

interface TimelineNode { time: string; label: string; description: string; operator: string; type: string }
const timelineData = ref<TimelineNode[]>([])

const alarmRecordSearch = ref({ description: '', timeRange: [] as string[] })
const notifyRecordSearch = ref({ account: '', timeRange: [] as string[] })

// 通知记录：从 alarm_notification 表查询，按时间降序
const filteredNotifyRecords = computed(() => {
  let list = notifyRecords.value
  if (notifyRecordSearch.value.account) {
    const kw = notifyRecordSearch.value.account.toLowerCase()
    list = list.filter(r =>
      (r.recipientName || '').toLowerCase().includes(kw) ||
      (r.channel || '').toLowerCase().includes(kw) ||
      (r.content || '').toLowerCase().includes(kw))
  }
  if (notifyRecordSearch.value.timeRange.length === 2) {
    const [s, e] = notifyRecordSearch.value.timeRange
    list = list.filter(r => r.createTime >= s && r.createTime <= e)
  }
  return [...list].sort((a, b) => (b.createTime || '').localeCompare(a.createTime || ''))
})
const resetNotifyRecords = () => { notifyRecordSearch.value = { account: '', timeRange: [] } }

// 生命周期流程图：按告警等级加载对应图片 (1-4)
const lifecycleImage = computed(() => {
  const lv = Number(props.data?.alarmLevel)
  return FLOW_CHART_BY_LEVEL[lv] || ''
})
// 等级文本（用于 img alt）
const alarmLevelText = computed(() => getAlarmLevelText(props.data?.alarmLevel))

// 底部操作栏仅在 待处理(1)/处理中(2) 时显示
const showActions = computed(() => {
  const s = Number(props.data?.status)
  return s === 1 || s === 2
})

const dialogTitle = computed(() => {
  if (!props.data) return '告警详情'
  return `${props.data.hazardPointName || '-'}[${props.data.firstTriggerTime || ''}]`
})

// 告警等级 — 起止相同时只显示一个
const alarmLevelRange = computed(() => {
  const d = props.data
  if (!d) return '-'
  const minLevel = (d as any).minAlarmLevel ?? d.alarmLevel
  const maxLevel = (d as any).maxAlarmLevel ?? d.alarmLevel
  const minText = getAlarmLevelText(minLevel)
  const maxText = getAlarmLevelText(maxLevel)
  return minText === maxText ? minText : `${minText}-${maxText}`
})

// 告警等级颜色（严格遵循 一级红/二级橙/三级黄/四级蓝）
// 头部图标：light 背景 + dark 字色
const levelIconStyle = computed(() => {
  const c = ALARM_LEVEL_COLORS[Number(props.data?.alarmLevel)] || { light: '#f4f4f5', dark: '#909399' }
  return { backgroundColor: c.light, color: c.dark }
})

// 告警记录过滤
const filteredAlarmRecords = computed(() => {
  let list = triggerDetails.value
  if (alarmRecordSearch.value.description) {
    const kw = alarmRecordSearch.value.description.toLowerCase()
    list = list.filter(r => (r.alarmMessage || '').toLowerCase().includes(kw))
  }
  if (alarmRecordSearch.value.timeRange.length === 2) {
    const [s, e] = alarmRecordSearch.value.timeRange
    list = list.filter(r => r.triggerTime >= s && r.triggerTime <= e)
  }
  return [...list].sort((a, b) => (b.triggerTime || '').localeCompare(a.triggerTime || ''))
})

const resetAlarmRecords = () => { alarmRecordSearch.value = { description: '', timeRange: [] } }

const switchToAlarmTab = () => { activeTab.value = 'alarm' }

// 切换到监测数据 tab：确保图表已初始化，并 resize 以适应可见尺寸
const switchToMonitorTab = () => {
  activeTab.value = 'monitor'
  nextTick(() => {
    if (!chartInstance) {
      tryInitChart()
    } else {
      updateChart()
      chartInstance?.resize()
    }
  })
}

// 弹窗打开时并发拉取；图表初始化推迟到 @opened 后再尝试，避免容器尺寸为 0
const dialogOpened = ref(false)
const dataReady = ref(false)

const tryInitChart = () => {
  if (!dialogOpened.value || !dataReady.value) return
  nextTick(() => {
    initChart()
    // 第一次初始化后立即 resize 一次，确保宽度正确
    chartInstance?.resize()
  })
}

const handleDialogOpened = () => {
  dialogOpened.value = true
  tryInitChart()
}

// 弹窗打开时并发拉取
watch(() => props.modelValue, async (val) => {
  if (!val) {
    dialogOpened.value = false
    dataReady.value = false
    h5QrcodeDataUrl.value = ''
    return
  }
  if (!props.data?.id) return
  activeTab.value = 'monitor'
  alarmRecordSearch.value = { description: '', timeRange: [] }
  notifyRecordSearch.value = { account: '', timeRange: [] }
  const id = Number(props.data.id)

  // 并发生成 H5 二维码（不阻塞主流程）
  QRCode.toDataURL(h5Url.value, { width: 140, margin: 1 })
    .then(url => { h5QrcodeDataUrl.value = url })
    .catch(() => { h5QrcodeDataUrl.value = '' })

  try {
    const [d, t, l, n] = await Promise.all([
      getAlarmRecordDetail(id),
      getTriggerDetails(id),
      getActionLogs(id),
      getAlarmNotifications(id),
    ])
    const detailData = (d as any).data ?? d
    detail.value = detailData ?? null
    triggerDetails.value = (t as any).data ?? t ?? []
    const rawLogs: AlarmRecordActionLog[] = (l as any).data ?? l ?? []
    notifyRecords.value = (n as any).data ?? n ?? []
    // 在 action_log 头部插入"当前状态"元素，作为时间线的当前节点（无时间/描述，仅动作类型）
    // 已销警(3)/误报(4) → ENDED 灰色"结束"；待处理(1)/处理中(2) → CURRENT 蓝色"当前"
    const isEnded = [3, 4].includes(Number(props.data?.status))
    const currentLog = {
      id: 0,
      alarmRecordId: id,
      actionType: isEnded ? 'ENDED' : 'CURRENT',
      createTime: '',
      description: '',
      remarks: '',
      operator: '',
    } as AlarmRecordActionLog
    const logs: AlarmRecordActionLog[] = [currentLog, ...rawLogs]
    disposalRecords.value = logs.filter((x: AlarmRecordActionLog) =>
      ['FEEDBACK', 'DISPOSE_CLOSE', 'DISPOSE_FALSE_ALARM'].includes(x.actionType))
    timelineData.value = buildTimeline(logs)
    dataReady.value = true
    await loadChartData()
    tryInitChart()
  } catch (e) {
    detail.value = null
    triggerDetails.value = []
    disposalRecords.value = []
    notifyRecords.value = []
    chartSeriesData.value = []
    timelineData.value = []
    dataReady.value = false
  }
})

/** 复制 H5 链接到剪贴板 */
const handleCopyH5Url = async () => {
  if (!h5Url.value) return
  try {
    await navigator.clipboard.writeText(h5Url.value)
    ElMessage.success('H5 链接已复制')
  } catch {
    // 降级：使用临时 textarea 兼容旧浏览器/非 HTTPS 环境
    try {
      const ta = document.createElement('textarea')
      ta.value = h5Url.value
      ta.style.position = 'fixed'
      ta.style.opacity = '0'
      document.body.appendChild(ta)
      ta.select()
      document.execCommand('copy')
      document.body.removeChild(ta)
      ElMessage.success('H5 链接已复制')
    } catch {
      ElMessage.error('复制失败，请手动复制')
    }
  }
}

// 由动作日志构造时间线（按时间倒序；CURRENT/ENDED 当前状态节点始终置顶）
function buildTimeline(logs: AlarmRecordActionLog[]): TimelineNode[] {
  return [...logs].sort((a, b) => {
    // CURRENT(当前) / ENDED(结束) 元素始终排在最前
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
      CREATE: '告警创建', RE_TRIGGER: '告警触发',
      LEVEL_CHANGE: '等级变化',
      FEEDBACK: '处置反馈', DISPOSE_CLOSE: '告警销警',
      DISPOSE_FALSE_ALARM: '标记误报', NOTIFY: '通知发送',
    }
    // 动作类型标签：使用静态映射，无映射时回退到原始 actionType
    const label = labelMap[log.actionType] || log.actionType
    // 动作描述：LEVEL_CHANGE 展示等级转换，其余展示 description
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

// ---------- 图表（对接监测数据 API） ----------

const pad2 = (n: number) => String(n).padStart(2, '0')
const fmtDateTime = (d: Date) =>
  `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())} ${pad2(d.getHours())}:${pad2(d.getMinutes())}:${pad2(d.getSeconds())}`

/** 加载监测曲线数据：首次告警时间前3天 ~ 当天24点 */
const loadChartData = async () => {
  const rec = detail.value || (props.data as any)
  if (!rec) return
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
      title: { text: '暂无监测数据', left: 'center', top: 'center', textStyle: { color: '#909399', fontSize: 14 } },
    }, true)
    return
  }

  const main = series[0]
  const labels = main.labels || []
  const alarmIndices = findAlarmIndices(labels)

  chartInstance.setOption({
    title: { text: main.seriesName || main.attrName || '监测数据', left: 'left', textStyle: { fontSize: 13, color: '#606266' } },
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        const p = params[0]
        if (!p) return ''
        const isAlarm = alarmIndices.includes(p.dataIndex)
        return `<div style="padding:8px"><div style="font-weight:bold;margin-bottom:4px">${p.name}</div><div>${main.seriesName || main.attrName}: <span style="color:${isAlarm ? '#f56c6c' : '#409eff'}">${p.value}${main.unit || ''}</span></div>${isAlarm ? '<div style="color:#f56c6c;margin-top:4px">⚠️ 告警触发点</div>' : ''}</div>`
      }
    },
    grid: { left: '3%', right: '8%', bottom: '12%', top: '18%', containLabel: true },
    xAxis: {
      type: 'category', data: labels,
      name: '时间',
      nameLocation: 'end',
      nameGap: 10,
      nameTextStyle: { fontSize: 11, color: '#909399' },
      axisLabel: {
        rotate: 30,
        fontSize: 10,
        color: '#666',
        interval: Math.max(1, Math.floor((labels || []).length / 8)),
        formatter: (val: string) => {
          const t = val.replace('T', ' ')
          const parts = t.split(/[\s-:]/)
          if (parts.length >= 5) {
            return `${Number(parts[1])}月${Number(parts[2])}日 ${parts[3]}:${parts[4]}`
          }
          return t.slice(5, 16)
        },
      },
      axisLine: { lineStyle: { color: '#c0c4cc' } }
    },
    yAxis: {
      type: 'value', name: main.attrName && main.unit ? `${main.attrName}(${main.unit})` : (main.unit || '监测值'),
      axisLabel: { fontSize: 12, color: '#666' },
      nameTextStyle: { fontSize: 11, color: '#606266' },
      axisLine: { show: true, lineStyle: { color: '#c0c4cc' } },
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
      areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(64,158,255,.3)' }, { offset: 1, color: 'rgba(64,158,255,.05)' }]) },
      markPoint: {
        data: alarmIndices.map((idx, i) => ({
          name: `告警点${i + 1}`,
          coord: [idx, main.values[idx]],
          value: main.values[idx],
          itemStyle: { color: '#f56c6c' },
          symbol: 'pin', symbolSize: 40,
          label: { show: true, formatter: '⚠️', fontSize: 14 }
        }))
      }
    }],
    dataZoom: [{ type: 'inside', start: 0, end: 100 }, { type: 'slider', show: true, start: 0, end: 100, height: 20, bottom: 0 }]
  }, true)
}

const handleResize = () => { chartInstance?.resize() }

onMounted(() => { window.addEventListener('resize', handleResize) })
onUnmounted(() => { window.removeEventListener('resize', handleResize); chartInstance?.dispose() })

// ---------- 工具函数 ----------
const formatDuration = (startTime: string) => {
  if (!startTime) return '0小时0分0秒'
  const diff = Date.now() - new Date(startTime).getTime()
  const h = Math.floor(diff / 3600000)
  const m = Math.floor((diff % 3600000) / 60000)
  const s = Math.floor((diff % 60000) / 1000)
  return `${h}小时${m}分${s}秒前`
}

const getAlarmLevelText = (level: number | string | undefined) => {
  const n = Number(level)
  return ({ 1: '一级（警报）', 2: '二级（警戒）', 3: '三级（警示）', 4: '四级（注意）' } as Record<number, string>)[n] || String(level)
}
const getAlarmTypeText = (type: string) =>
  ({ THRESHOLD: '阈值预警', COMPREHENSIVE: '综合预警' } as Record<string, string>)[type] || type

/** 数值等级 → 中文颜色名称 (1=红色 2=橙色 3=黄色 4=蓝色) */
const getLevelName = (val: string | undefined) => {
  const n = Number(val)
  return ({ 1: '红色', 2: '橙色', 3: '黄色', 4: '蓝色' } as Record<number, string>)[n] || val || ''
}

// ---------- 附件上传 + 底部按钮事件 ----------
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

const handleFeedbackSubmit = async (data: { content: string; files: File[] }) => {
  const hasFiles = data.files && data.files.length > 0
  const attachments = await uploadAttachments(data.files || [])
  if (hasFiles && !attachments) {
    ElMessage.warning('附件上传失败，已提交纯文本反馈')
  }
  emit('submit', {
    description: data.content,
    attachments,
    remarks: data.content,
  })
  if (!hasFiles || attachments) {
    ElMessage.success('反馈提交成功')
  }
}

const handleNotifySubmit = (data: any) => {
  emit('notify', data)
}

const handleFalseAlarm = () => { emit('false-alarm', props.data) }
const handleCloseAlarm = () => { emit('close-alarm', props.data) }
const handleClose = () => { emit('update:modelValue', false) }
</script>

<style scoped>
.feedback-container { height: 70vh; display: flex; flex-direction: column; overflow: hidden; }
.main-content { display: flex; gap: 12px; flex: 1; min-height: 0; overflow: hidden; }

/* ====== 左侧生命周期（按等级加载流程图）====== */
.left-section {
  width: 340px;
  background: #f8f9fa;
  border-radius: 8px;
  padding: 10px 8px;
  border: 1px solid #e9ecef;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
}
.section-header {
  display: flex; align-items: center; gap: 4px;
  margin-bottom: 8px; padding-bottom: 6px;
  border-bottom: 1px solid #e9ecef;
}
.icon-wrapper {
  width: 20px; height: 20px;
  display: flex; align-items: center; justify-content: center;
  background: #e8f4fd; border-radius: 4px; color: #409eff;
}
.section-title { font-size: var(--el-font-size-base); font-weight: 600; color: #333; }
.lifecycle-img-wrap {
  flex: 1;
  min-height: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: auto;
}
.lifecycle-img {
  width: 100%;
  height: auto;
  display: block;
  object-fit: contain;
}
.lifecycle-empty {
  color: #909399;
  font-size: var(--el-font-size-base);
  padding: 20px 0;
  text-align: center;
}

/* ====== 右侧 ====== */
.right-section { flex: 1; display: flex; flex-direction: column; gap: 10px; min-width: 0; min-height: 0; }

/* ====== 头部卡片 - 一行显示 ====== */
.event-header {
  display: flex; align-items: center; gap: 0;
  background: #fff; border-radius: 8px; border: 1px solid #e9ecef;
  padding: 10px 14px;
}
.hd-item {
  display: flex; align-items: center; gap: 8px;
  flex: 1; min-width: 0;
  padding: 0 10px;
}
.hd-item + .hd-item { border-left: 1px solid #f0f0f0; }
.hd-icon {
  width: 34px; height: 34px; border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
  font-size: var(--el-font-size-large); flex-shrink: 0;
}
.hd-icon.hazard { background: #e8f5e9; color: #28a745; }
.hd-icon.device { background: #e3f2fd; color: #1976d2; }
.hd-icon.level { background: #fff3e0; color: #ff9800; }
.hd-icon.time { background: #f3e8ff; color: #6f42c1; }
.hd-icon.count { background: #fef0f0; color: #dc3545; }
.hd-text { display: flex; flex-direction: column; gap: 1px; min-width: 0; }
.hd-label { font-size: var(--el-font-size-extra-small); color: #909399; }
.hd-val { font-size: 15px; font-weight: 600; color: #303133; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.hd-val.lv { display: inline-block; padding: 1px 6px; border-radius: 3px; font-size: var(--el-font-size-small); width: fit-content; }

.hd-count {
  font-size: var(--el-font-size-extra-large); font-weight: 700; color: #f56c6c;
  cursor: pointer; transition: transform .15s;
}
.hd-count:hover { transform: scale(1.15); }
.alarm-count-item { cursor: pointer; }

/* ====== 中部：数据区 + 时间线（左右） ====== */
.event-body {
  display: flex;
  gap: 10px;
  flex: 1;
  min-height: 0;
}

/* ====== 数据区域 ====== */
.data-section {
  flex: 1;
  min-width: 0;
  background: #fff; border-radius: 8px; border: 1px solid #e9ecef;
  padding: 10px 12px;
  display: flex; flex-direction: column;
}

/* 合并容器：左侧基本资料 + 告警描述，右侧 H5 二维码 */
.info-summary {
  display: flex;
  gap: 10px;
  margin-bottom: 8px;
  align-items: stretch;
}
.info-summary-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.info-summary-side {
  flex-shrink: 0;
  width: 150px;
}
.info-row { display: flex; gap: 10px; }
.info-item {
  flex: 1;
  background: #f8f9fa; border-radius: 6px; padding: 6px 10px;
}
.info-label { font-size: var(--el-font-size-extra-small); color: #909399; display: block; margin-bottom: 2px; }
.info-value { font-size: var(--el-font-size-base); font-weight: 500; color: #303133; }
.count-link { color: #409eff; cursor: pointer; text-decoration: underline; }
.count-link:hover { color: #66b1ff; }

/* 告警描述（在合并容器主体内） */
.desc-row {
  flex: 1;
  background: #f8f9fa; border-radius: 6px; padding: 6px 10px;
}
.desc-row .detail-label { display: block; margin-bottom: 2px; font-size: var(--el-font-size-extra-small); color: #909399; }
.desc-row p { font-size: var(--el-font-size-base); color: #606266; line-height: 1.5; margin: 0; }

/* H5 二维码卡片 */
.qr-card {
  background: #f8f9fa;
  border: 1px solid #e9ecef;
  border-radius: 6px;
  padding: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  height: 100%;
  box-sizing: border-box;
}
.qr-title {
  font-size: var(--el-font-size-small);
  color: #606266;
  font-weight: 600;
}
.qr-img {
  width: 130px;
  height: 130px;
  display: block;
}
.qr-placeholder {
  width: 130px;
  height: 130px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
  font-size: var(--el-font-size-small);
  background: #fff;
  border: 1px dashed #dcdfe6;
  border-radius: 4px;
}

.data-tabs {
  display: flex; gap: 16px; margin-bottom: 8px;
  padding-bottom: 6px; border-bottom: 1px solid #f0f0f0;
}
.data-tabs .tab {
  font-size: var(--el-font-size-base); color: #606266; padding: 3px 0;
  cursor: pointer; border-bottom: 2px solid transparent;
  transition: all .2s;
}
.data-tabs .tab.active { color: #409eff; border-bottom-color: #409eff; font-weight: 600; }
.tab-content {
  height: 380px;
  display: flex;
  flex-direction: column;
}

/* 图表容器 */
.chart-container { width: 100%; flex: 1; min-height: 0; }

/* 页签内搜索 */
.tab-search { display: flex; gap: 8px; align-items: center; margin-bottom: 8px; flex-shrink: 0; }
.tab-sch-inp { width: 180px; }
.tab-sch-date { width: 240px; }

/* 表头恢复正常样式（不加粗、字号 12px），覆盖全局 .table-wrap 内的偏粗表头 */
:deep(.el-table th.el-table__cell) {
  background: #fafafa !important;
  color: #606266;
  font-size: var(--el-font-size-base);
  font-weight: 500;
  border-bottom: 1px solid #ebeef5;
}
:deep(.el-table .cell) {
  font-size: var(--el-font-size-base);
  color: #303133;
}

/* ====== 右侧时间线（参考原查看弹窗样式）====== */
.timeline-panel {
  width: 200px;
  flex-shrink: 0;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e9ecef;
  display: flex;
  flex-direction: column;
  max-height: 100%;
  overflow: hidden;
}

.timeline-header {
  padding: 12px 14px 8px;
  border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;
}

.timeline-title {
  font-size: var(--el-font-size-medium);
  font-weight: 600;
  color: #303133;
}

.timeline-container {
  flex: 1;
  padding: 12px 14px;
  overflow-y: auto;
  min-height: 60px;
}

.timeline { position: relative; padding-left: 16px; }
.timeline-item { position: relative; padding-bottom: 18px; }
.timeline-item:last-child { padding-bottom: 0; }

.timeline-dot {
  position: absolute; left: -16px; top: 3px;
  width: 10px; height: 10px; border-radius: 50%; background: #d9d9d9;
  z-index: 1;
}
.timeline-dot.trigger { background: #f56c6c; }
.timeline-dot.notify { background: #409eff; }
.timeline-dot.dispose { background: #67c23a; }
.timeline-dot.system { background: #909399; }

/* 当前状态节点：蓝色，保留呼吸光晕动画 */
.timeline-dot.current {
  background: #409eff;
  box-shadow: 0 0 0 4px rgba(64, 158, 255, 0.18);
  animation: timeline-pulse 1.8s ease-in-out infinite;
}
/* 历史警情终态节点：灰色（无动画，表达已结束） */
.timeline-dot.ended {
  background: #909399;
}
@keyframes timeline-pulse {
  0%, 100% { box-shadow: 0 0 0 4px rgba(64, 158, 255, 0.18); }
  50%      { box-shadow: 0 0 0 7px rgba(64, 158, 255, 0.08); }
}

.timeline-line {
  position: absolute; left: -12px; top: 14px;
  width: 2px; height: calc(100% - 4px); background: #e8e8e8;
}
.timeline-content { padding-left: 8px; }
.timeline-time {
  font-size: var(--el-font-size-small); color: #909399;
  margin-bottom: 2px; line-height: 1.4;
}
.timeline-label {
  font-size: var(--el-font-size-base); font-weight: 600;
  color: #303133; line-height: 1.5;
}
.timeline-operator {
  font-weight: 400;
  color: #909399;
}
.timeline-desc {
  font-size: var(--el-font-size-small); color: #909399; line-height: 1.5;
  margin-top: 2px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  word-break: break-all;
  cursor: default;
}
.timeline-empty {
  display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  height: 80px; color: #909399; font-size: var(--el-font-size-base);
}

/* ====== 底部操作栏 - 固定在 right-section 底部 ====== */
.action-right {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
  margin-top: auto;
  padding-top: 10px;
  border-top: 1px solid #f1f5f9;
}
</style>
