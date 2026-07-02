<!-- src/pages/alarm-detail.vue -->
<template>
  <view class="page-container">
    <!-- 头部 -->
    <PageHeader show-back :title="alarmData?.hazardPointName || '告警详情'" />

    <!-- 内容 -->
    <view class="page-body">
      <scroll-view
        class="page-scroll"
        scroll-y
        refresher-enabled
        :refresher-triggered="isRefreshing"
        :scroll-top="scrollTop"
        :style="{ marginTop: `${capsuleShift}px`, height: `calc(100% - ${capsuleShift}px)` }"
        @refresherrefresh="loadAll"
        @scroll="onScroll"
      >
      <view v-if="loading && !alarmData" class="loading-wrapper">
        <text class="loading-text">加载中...</text>
      </view>

      <view v-else-if="!alarmData" class="loading-wrapper">
        <text class="loading-text">{{ loadError || '告警不存在' }}</text>
      </view>

      <template v-else>
        <!-- 告警信息卡 -->
        <view class="section">
          <view class="info-card">
            <view class="info-row">
              <text class="info-label">告警等级</text>
              <view class="level-badge" :style="{ background: getAlarmLevelColor(alarmData.alarmLevel) }">
                {{ getAlarmLevelText(alarmData.alarmLevel) }}
              </view>
            </view>
            <view class="info-row">
              <text class="info-label">告警类型</text>
              <text class="info-value">{{ getAlarmTypeText(alarmData.alarmType) }}</text>
            </view>
            <view class="info-row">
              <text class="info-label">告警时间</text>
              <text class="info-value time-range">{{ formatTime(alarmData.firstTriggerTime) }} ~ {{ formatTime(alarmData.lastTriggerTime) }}</text>
            </view>
            <view class="info-row">
              <text class="info-label">触发设备</text>
              <text class="info-value">{{ alarmData.deviceName || '-' }}</text>
            </view>
            <view class="info-row column">
              <text class="info-label">告警描述</text>
              <text class="info-value content-text">{{ alarmData.alarmMessage || '-' }}</text>
            </view>
            <view class="info-row">
              <text class="info-label">触发次数</text>
              <text class="info-value">{{ alarmData.triggerCount || 0 }} 次</text>
            </view>
            <view class="info-row">
              <text class="info-label">状态</text>
              <view class="status-badge" :class="`status-${getStatusType(alarmData.status)}`">
                {{ alarmData.statusName || getStatusText(alarmData.status) }}
              </view>
            </view>
          </view>
        </view>

        <!-- 项目信息卡 -->
        <view class="section" v-if="hazardPointData">
          <text class="section-title">项目信息</text>
          <view class="info-card">
            <view class="info-row">
              <text class="info-label">隐患点</text>
              <text class="info-value">{{ alarmData.hazardPointName || '-' }}</text>
            </view>
            <view class="info-row" @click="openMap">
              <text class="info-label">地理位置</text>
              <text class="info-value" style="color: #3068e4;">{{ formatLocation(hazardPointData) }} ></text>
            </view>
            <view class="info-row">
              <text class="info-label">所属分组</text>
              <text class="info-value">{{ hazardPointData.groupName || '-' }}</text>
            </view>
            <view class="info-row column">
              <text class="info-label">隐患点描述</text>
              <text class="info-value content-text">{{ hazardPointData.description || '-' }}</text>
            </view>
          </view>
        </view>

        <!-- 支撑数据卡 (4 Tab) -->
        <view class="section">
          <text class="section-title">支撑数据</text>
          <view class="support-card">
            <view class="sub-tabs">
              <view
                v-for="tab in supportTabs"
                :key="tab.value"
                class="sub-tab"
                :class="{ active: activeSupportTab === tab.value }"
                @click="switchSupportTab(tab.value)"
              >
                {{ tab.label }}
              </view>
            </view>

            <!-- 监测数据 -->
            <view v-if="activeSupportTab === 'monitor'" class="sub-content">
              <MonitorChart
                v-if="chartSeries.length > 0 && chartReady"
                :key="`alarm-chart-${alarmId}`"
                :series="chartSeries"
                :alarmLabels="alarmTimeLabels"
                height="420rpx"
              />
              <view v-else class="empty-block">
                <text class="empty-block-text">暂无监测数据</text>
              </view>
            </view>

            <!-- 告警次数 (触发明细) -->
            <view v-else-if="activeSupportTab === 'trigger'" class="sub-content">
              <view v-if="triggerDetails.length === 0" class="empty-block">
                <text class="empty-block-text">暂无触发记录</text>
              </view>
              <view v-else>
                <view
                  v-for="t in triggerDetails"
                  :key="t.id"
                  class="trigger-item"
                >
                  <view class="trigger-dot" :style="{ background: getAlarmLevelColor(t.alarmLevel) }"></view>
                  <view class="trigger-info">
                    <view class="trigger-top">
                      <text class="trigger-level" :style="{ color: getAlarmLevelColor(t.alarmLevel) }">
                        {{ getAlarmLevelText(t.alarmLevel) }}
                      </text>
                      <text class="trigger-time">{{ formatTime(t.triggerTime) }}</text>
                    </view>
                    <text v-if="t.alarmMessage" class="trigger-message">{{ t.alarmMessage }}</text>
                  </view>
                </view>
              </view>
            </view>

            <!-- 通知记录 -->
            <view v-else-if="activeSupportTab === 'notify'" class="sub-content">
              <view v-if="notifyRecords.length === 0" class="empty-block">
                <text class="empty-block-text">暂无通知记录</text>
              </view>
              <view v-else>
                <view
                  v-for="n in notifyRecords"
                  :key="n.id"
                  class="notify-item"
                >
                  <view class="notify-top">
                    <view class="notify-channel" :class="`channel-${n.channel?.toLowerCase()}`">
                      {{ getChannelText(n.channel) }}
                    </view>
                    <text class="notify-label">接收人：</text>
                    <text class="notify-value">{{ n.recipientName || n.recipientPhone || '-' }}</text>
                    <view class="notify-status" :class="`notify-status-${getNotifyStatusType(n.status)}`">
                      {{ getNotifyStatusText(n.status) }}
                    </view>
                  </view>
                  <view v-if="n.content" class="notify-content-row">
                    <text class="notify-content">{{ n.content }}</text>
                  </view>
                  <text v-if="n.sendTime" class="notify-time">{{ formatTime(n.sendTime) }}</text>
                </view>
              </view>
            </view>

            <!-- 反馈记录 -->
            <view v-else-if="activeSupportTab === 'feedback'" class="sub-content">
              <view v-if="feedbackLogs.length === 0" class="empty-block">
                <text class="empty-block-text">暂无反馈记录</text>
              </view>
              <view v-else>
                <view
                  v-for="f in feedbackLogs"
                  :key="f.id"
                  class="feedback-item"
                >
                  <view class="feedback-top">
                    <view class="feedback-action" :class="`feedback-action-${getFeedbackActionType(f.actionType)}`">
                      {{ getFeedbackActionText(f.actionType) }}
                    </view>
                    <text class="feedback-time">{{ formatTime(f.createTime) }}</text>
                  </view>
                  <view v-if="f.operator" class="feedback-operator">
                    <text class="feedback-label">操作人：</text>
                    <text class="feedback-value">{{ f.operator }}</text>
                  </view>
                  <text v-if="f.description" class="feedback-content">{{ f.description }}</text>
                </view>
              </view>
            </view>
          </view>
        </view>

        <!-- 时间线 -->
        <view class="section">
          <view class="timeline-header">
            <text class="section-title">时间线</text>
            <text v-if="timeline.length > 5" class="timeline-toggle" @click="toggleTimeline">
              {{ showAllTimeline ? '收起' : '展开全部' }}
            </text>
          </view>
          <view class="timeline-card">
            <view
              v-for="(node, idx) in displayedTimeline"
              :key="idx"
              class="timeline-item"
            >
              <view class="timeline-dot" :class="`type-${node.type}`"></view>
              <view v-if="idx < displayedTimeline.length - 1" class="timeline-line"></view>
              <view class="timeline-content">
                <view class="timeline-row">
                  <text class="timeline-label">{{ node.label }}</text>
                  <text class="timeline-time">{{ formatTime(node.time) }}</text>
                </view>
                <text v-if="node.description" class="timeline-desc">{{ node.description }}</text>
                <text v-if="node.operator" class="timeline-operator">{{ node.operator }}</text>
              </view>
            </view>
            <view v-if="timeline.length === 0" class="empty-block">
              <text class="empty-block-text">暂无时间线</text>
            </view>
          </view>
        </view>

        <!-- 底部留白 -->
        <view class="bottom-spacer" />
      </template>
      </scroll-view>

      <!-- 粘性胶囊：绝对固定在 page-body 可视区顶部，不随滚动消失 -->
      <view
        v-if="alarmData"
        class="sticky-capsule"
        :style="{ opacity: capsuleOpacity, transform: `translateY(${capsuleTranslateY})` }"
      >
        <view class="capsule-badge" :style="{ background: getAlarmLevelColor(alarmData.alarmLevel) }">
          {{ getAlarmLevelText(alarmData.alarmLevel) }}
        </view>
        <view class="capsule-divider" />
        <view class="capsule-field">
          <text class="capsule-label">类型</text>
          <text class="capsule-val">{{ getAlarmTypeText(alarmData.alarmType) }}</text>
        </view>
        <view class="capsule-divider" />
        <view class="capsule-field">
          <text class="capsule-label">触发</text>
          <text class="capsule-val">{{ alarmData.triggerCount || 0 }} 次</text>
        </view>
      </view>
    </view>

    <!-- 回到顶部 -->
    <view v-if="showBackTop" class="back-top-btn" @click="scrollToTop">
      <zui-svg-icon icon="up" :width="20" color="#ffffff" />
    </view>

    <!-- 底部操作栏 -->
    <view v-if="showActions" class="action-bar" :style="{ paddingBottom: `${safeAreaBottom + 16}rpx` }">
      <view class="action-btn feedback-btn" @click="goFeedback">反馈</view>
      <view class="action-btn false-btn" @click="handleFalseAlarm">误报</view>
      <view class="action-btn clear-btn" @click="handleClear">消警</view>
    </view>
  </view>
</template>

<script setup lang="ts">
import MonitorChart from '@/components/MonitorChart.vue'
import PageHeader from '@/components/PageHeader.vue'
import { useSafeArea } from '@/composables/useSafeArea'
import type {
  AlarmNotificationItem,
  AlarmRecordActionLog,
  AlarmRecordItem,
  AlarmRecordTriggerDetail,
  TimelineNode,
} from '@/utils/alarm'
import {
  alarmApi,
  buildTimeline,
  getAlarmLevelColor,
  getAlarmLevelText,
  getAlarmTypeText,
  getChannelText,
  getFeedbackActionText,
  getFeedbackActionType,
  getNotifyStatusText,
  getNotifyStatusType,
  getStatusText,
  getStatusType,
} from '@/utils/alarm'
import type { HazardWithDevices } from '@/utils/hazard'
import { hazardApi } from '@/utils/hazard'
import type { ChartSeries } from '@/utils/monitor'
import { monitorApi, calcGranularity } from '@/utils/monitor'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { computed, nextTick, ref } from 'vue'

const { safeAreaBottom } = useSafeArea()

const alarmId = ref<number>(0)
const loading = ref(false)
const loadError = ref('')
const isRefreshing = ref(false)

// 回到顶部
const showBackTop = ref(false)
const scrollTop = ref(0)

// 粘性胶囊：滚动时元素逐渐聚拢并吸顶
const scrollY = ref(0)

const capsuleOpacity = computed(() => {
  // 从 60px 开始出现，140px 完全可见
  return Math.min(1, Math.max(0, (scrollY.value - 60) / 80))
})

const capsuleTranslateY = computed(() => {
  const progress = capsuleOpacity.value
  return `${(1 - progress) * 12}rpx`
})

// 胶囊出现时，scroll-view 逐渐下移让出空间
// 实际高度 = padding 20rpx×2 + 内容 ~44rpx ≈ 84rpx ≈ 42px，加 5px 间距 = 47px
const capsuleShift = computed(() => {
  return Math.round(capsuleOpacity.value * 55)
})

const onScroll = (e: { detail: { scrollTop: number } }) => {
  const y = e.detail.scrollTop
  scrollY.value = y
  showBackTop.value = y > 400
}

const scrollToTop = () => {
  scrollTop.value = 1
  nextTick(() => { scrollTop.value = 0 })
  showBackTop.value = false
}

const alarmData = ref<AlarmRecordItem | null>(null)
const triggerDetails = ref<AlarmRecordTriggerDetail[]>([])
const actionLogs = ref<AlarmRecordActionLog[]>([])
const notifyRecords = ref<AlarmNotificationItem[]>([])
const hazardPointData = ref<HazardWithDevices | null>(null)

const timeline = ref<TimelineNode[]>([])
const showAllTimeline = ref(false)

// 支撑数据 Tab
type SupportTab = 'monitor' | 'trigger' | 'notify' | 'feedback'
const supportTabs: { label: string; value: SupportTab }[] = [
  { label: '监测数据', value: 'monitor' },
  { label: '告警记录', value: 'trigger' },
  { label: '通知记录', value: 'notify' },
  { label: '反馈记录', value: 'feedback' },
]
const activeSupportTab = ref<SupportTab>('monitor')

// 图表
const chartSeries = ref<ChartSeries[]>([])
const chartReady = ref(false)

// 告警时间标签（用于 MonitorChart markPoint 标注）
const alarmTimeLabels = computed(() =>
  triggerDetails.value
    .map(td => td.triggerTime)
    .filter((t): t is string => !!t)
)

const feedbackLogs = computed(() =>
  actionLogs.value.filter(x =>
    ['FEEDBACK', 'DISPOSE_CLOSE', 'DISPOSE_FALSE_ALARM'].includes(x.actionType),
  ),
)

const showActions = computed(() => {
  const s = Number(alarmData.value?.status)
  return s === 1 || s === 2
})

const displayedTimeline = computed(() =>
  showAllTimeline.value ? timeline.value : timeline.value.slice(0, 5),
)

const toggleTimeline = () => {
  showAllTimeline.value = !showAllTimeline.value
}

onLoad((options) => {
  if (options?.id) {
    alarmId.value = Number(options.id)
  } else {
    loadError.value = '缺少告警 ID'
  }
})

onShow(() => {
  // 从 alarm-handle 返回时触发刷新
  if (alarmId.value && !loading.value) {
    loadAll()
  }
})

const formatTime = (time: string) => {
  if (!time) return '-'
  const iosTime = time.replace(/-/g, '/').replace(' ', 'T').replace(/\.\d+Z$/, '')
  const date = new Date(iosTime)
  if (isNaN(date.getTime())) return time
  const pad = (n: number) => String(n).padStart(2, '0')
  const yyyy = date.getFullYear()
  const mm = pad(date.getMonth() + 1)
  const dd = pad(date.getDate())
  const hh = pad(date.getHours())
  const mi = pad(date.getMinutes())
  const ss = pad(date.getSeconds())
  return `${yyyy}-${mm}-${dd} ${hh}:${mi}:${ss}`
}

const formatLocation = (hp: HazardWithDevices | null) => {
  if (!hp) return '-'
  if (hp.longitude != null && hp.latitude != null) {
    return `${Number(hp.longitude).toFixed(6)}, ${Number(hp.latitude).toFixed(6)}`
  }
  return hp.location || '-'
}

const switchSupportTab = (tab: SupportTab) => {
  activeSupportTab.value = tab
  if (tab === 'monitor') {
    nextTick(() => {
      chartReady.value = false
      nextTick(() => {
        chartReady.value = true
      })
    })
  }
}

const loadAll = async () => {
  if (!alarmId.value) {
    loadError.value = '缺少告警 ID'
    return
  }
  loading.value = true
  loadError.value = ''
  try {
    const id = alarmId.value
    const [detail, triggers, logs, notifies] = await Promise.all([
      alarmApi.getAlarmRecordDetail(id),
      alarmApi.getTriggerDetails(id),
      alarmApi.getActionLogs(id),
      alarmApi.getNotifications(id),
    ])

    alarmData.value = detail as AlarmRecordItem
    triggerDetails.value = triggers
    actionLogs.value = logs
    notifyRecords.value = notifies

    // 构造时间线（含 CURRENT/ENDED 虚拟节点）
    const isEnded = [3, 4].includes(Number(detail?.status))
    const virtualLog: AlarmRecordActionLog = {
      id: 0,
      alarmRecordId: id,
      actionType: isEnded ? 'ENDED' : 'CURRENT',
      createTime: '',
      description: '',
      remarks: '',
      operator: '',
    } as AlarmRecordActionLog
    timeline.value = buildTimeline([virtualLog, ...logs])

    // 加载隐患点详情
    if (detail?.hazardPointId) {
      try {
        hazardPointData.value = await hazardApi.getById(Number(detail.hazardPointId)) || null
      } catch {
        hazardPointData.value = null
      }
    }

    // 加载监测曲线
    await loadChartData()
    chartReady.value = true
  } catch (e: any) {
    console.error('加载告警详情失败:', e)
    loadError.value = e?.message || '加载失败，请重试'
    alarmData.value = null
  } finally {
    loading.value = false
    isRefreshing.value = false
  }
}

// ─── 图表 ───

const pad2 = (n: number) => String(n).padStart(2, '0')
const fmtDateTime = (d: Date) =>
  `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())} ${pad2(d.getHours())}:${pad2(d.getMinutes())}:${pad2(d.getSeconds())}`

/** 加载监测曲线：首次告警时间前3天 ~ 当天24点 */
const loadChartData = async () => {
  const rec: AlarmRecordItem | null = alarmData.value
  chartSeries.value = []
  if (!rec) return

  const hpId = Number(rec.hazardPointId)
  if (!hpId) return

  const firstTime = new Date(String(rec.firstTriggerTime).replace(/-/g, '/'))
  if (isNaN(firstTime.getTime())) return

  const startTime = new Date(firstTime)
  startTime.setDate(startTime.getDate() - 3)
  const endTime = new Date()
  endTime.setHours(23, 59, 59, 0)

  const startTimeStr = fmtDateTime(startTime)
  const endTimeStr = fmtDateTime(endTime)

  const params: any = {
    hazardPointId: hpId,
    startTime: startTimeStr,
    endTime: endTimeStr,
    granularity: calcGranularity(startTimeStr, endTimeStr),
  }
  if (rec.deviceId) params.deviceId = Number(rec.deviceId)
  if (rec.sensorId) params.sensorId = Number(rec.sensorId)

  try {
    const list = await monitorApi.getChart(params)
    chartSeries.value = Array.isArray(list) ? list : []
  } catch (e) {
    chartSeries.value = []
  }
}

// ─── 处置操作 ───

const goFeedback = () => {
  if (!alarmId.value) return
  uni.navigateTo({ url: `/pages/alarm-handle?alarmId=${alarmId.value}` })
}

const handleFalseAlarm = () => {
  uni.showModal({
    title: '确认误报',
    content: '确定将此告警标记为误报吗？',
    success: async (res) => {
      if (!res.confirm) return
      try {
        await alarmApi.disposeAlarm(alarmId.value, { status: 4 })
        uni.showToast({ title: '已标记为误报', icon: 'success' })
        await loadAll()
      } catch (e: any) {
        uni.showToast({ title: e?.message || '操作失败', icon: 'none' })
      }
    },
  })
}

const handleClear = () => {
  uni.showModal({
    title: '确认消警',
    content: '确定将此告警销警吗？',
    success: async (res) => {
      if (!res.confirm) return
      try {
        await alarmApi.disposeAlarm(alarmId.value, { status: 3 })
        uni.showToast({ title: '已销警', icon: 'success' })
        await loadAll()
      } catch (e: any) {
        uni.showToast({ title: e?.message || '操作失败', icon: 'none' })
      }
    },
  })
}

const openMap = () => {
  const hpId = alarmData.value?.hazardPointId
  if (!hpId) {
    uni.showToast({ title: '暂无隐患点信息', icon: 'none' })
    return
  }
  uni.navigateTo({ url: `/pages/hazard-detail?id=${hpId}` })
}
</script>

<style lang="scss" scoped>
.page-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, #eef1f8 0%, #e8ecf4 100%);
}

.header {
  position: relative;
  flex-shrink: 0;
}

.loading-wrapper {
  display: flex;
  justify-content: center;
  padding: 80rpx 0;
}

.loading-text {
  font-size: 26rpx;
  color: #9ca3af;
}

/* page-body wrapper：继承全局 flex 布局，作为胶囊定位上下文 */
.page-body {
  position: relative;
}

.page-scroll {
  height: 100%;
}

/* 粘性胶囊卡片：absolute 固定在 page-body 可视区顶部 */
.sticky-capsule {
  position: absolute;
  top: 0;
  left: 32rpx;
  right: 32rpx;
  display: flex;
  align-items: center;
  background: #ffffff;
  border-radius: 24rpx;
  padding: 20rpx 8rpx;
  box-shadow: 0 8rpx 32rpx rgba(102, 126, 234, 0.15);
  z-index: 10;
  pointer-events: none;
}

.capsule-badge {
  font-size: 20rpx;
  color: #ffffff;
  padding: 6rpx 18rpx;
  border-radius: 12rpx;
  flex-shrink: 0;
  margin: 0 8rpx;
}

.capsule-field {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2rpx;
}

.capsule-label {
  font-size: 18rpx;
  color: #9ca3af;
  line-height: 1;
}

.capsule-val {
  font-size: 22rpx;
  color: #1a1a2e;
  font-weight: 600;
  line-height: 1.2;
}

.capsule-divider {
  width: 1rpx;
  height: 36rpx;
  background: #e5e7eb;
  flex-shrink: 0;
}

.section {
  margin: 0 32rpx 24rpx;
}

.section-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #1a1a2e;
  margin-bottom: 16rpx;
  display: block;
}

.info-card {
  background: #ffffff;
  border-radius: 24rpx;
  padding: 24rpx;
  box-shadow: 0 8rpx 32rpx rgba(102, 126, 234, 0.12);
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12rpx 0;
  border-bottom: 1rpx solid #f5f5f5;
  gap: 16rpx;

  &:last-child { border-bottom: none; }

  &.column {
    flex-direction: column;
    align-items: flex-start;
    gap: 8rpx;
  }
}

.info-label {
  font-size: 26rpx;
  color: #6b7280;
  flex-shrink: 0;
}

.info-value {
  font-size: 26rpx;
  color: #1a1a2e;
  font-weight: 500;
  text-align: right;

  &.time-range {
    font-size: 24rpx;
  }

  &.content-text {
    max-width: 100%;
    line-height: 1.5;
    text-align: left;
    word-break: break-all;
  }
}

.level-badge {
  padding: 6rpx 16rpx;
  border-radius: 8rpx;
  font-size: 22rpx;
  color: #ffffff;
}

.status-badge {
  padding: 6rpx 16rpx;
  border-radius: 8rpx;
  font-size: 22rpx;

  &.status-danger { background: rgba(245, 63, 63, 0.1); color: #f53f3f; }
  &.status-warning { background: rgba(255, 125, 0, 0.1); color: #ff7d00; }
  &.status-success { background: rgba(82, 196, 26, 0.1); color: #52c41a; }
  &.status-info { background: rgba(144, 147, 153, 0.1); color: #909399; }
}

/* 支撑数据 Tab */
.support-card {
  background: #ffffff;
  border-radius: 24rpx;
  padding: 0;
  box-shadow: 0 8rpx 32rpx rgba(102, 126, 234, 0.12);
  overflow: hidden;
}

.sub-tabs {
  display: flex;
  border-bottom: 1rpx solid #f0f0f0;
}

.sub-tab {
  flex: 1;
  padding: 20rpx 0;
  text-align: center;
  font-size: 24rpx;
  color: #6b7280;
  position: relative;

  &.active {
    color: #3068e4;
    font-weight: 600;

    &::after {
      content: '';
      position: absolute;
      bottom: 0;
      left: 50%;
      transform: translateX(-50%);
      width: 40rpx;
      height: 4rpx;
      background: #3068e4;
      border-radius: 2rpx;
    }
  }
}

.sub-content {
  padding: 24rpx;
  min-height: 240rpx;
}

.empty-block {
  padding: 60rpx 0;
  text-align: center;
}

.empty-block-text {
  font-size: 24rpx;
  color: #9ca3af;
}

/* 触发明细 */
.trigger-item {
  display: flex;
  gap: 16rpx;
  padding: 16rpx 0;
  border-bottom: 1rpx solid #f5f5f5;

  &:last-child { border-bottom: none; }
}

.trigger-dot {
  width: 14rpx;
  height: 14rpx;
  border-radius: 50%;
  margin-top: 10rpx;
  flex-shrink: 0;
}

.trigger-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4rpx;
  min-width: 0;
}

.trigger-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8rpx;
}

.trigger-level {
  font-size: 24rpx;
  font-weight: 500;
}

.trigger-time {
  font-size: 22rpx;
  color: #9ca3af;
}

.trigger-message {
  font-size: 24rpx;
  color: #4b5563;
  line-height: 1.5;
}

/* 通知记录 */
.notify-item {
  padding: 16rpx 0;
  border-bottom: 1rpx solid #f5f5f5;

  &:last-child { border-bottom: none; }
}

.notify-top {
  display: flex;
  align-items: center;
  gap: 8rpx;
  margin-bottom: 8rpx;
}

.notify-channel {
  font-size: 22rpx;
  padding: 4rpx 12rpx;
  border-radius: 6rpx;

  &.channel-system { background: rgba(48, 104, 228, 0.1); color: #3068e4; }
  &.channel-sms { background: rgba(82, 196, 26, 0.1); color: #52c41a; }
  &.channel-email { background: rgba(250, 140, 22, 0.1); color: #fa8c16; }
}

.notify-status {
  font-size: 22rpx;
  padding: 4rpx 12rpx;
  border-radius: 6rpx;
  margin-left: auto;
  flex-shrink: 0;

  &.notify-status-info { background: rgba(144, 147, 153, 0.1); color: #909399; }
  &.notify-status-success { background: rgba(82, 196, 26, 0.1); color: #52c41a; }
  &.notify-status-danger { background: rgba(245, 63, 63, 0.1); color: #f53f3f; }
  &.notify-status-warning { background: rgba(255, 125, 0, 0.1); color: #ff7d00; }
}

.notify-content-row {
  display: flex;
  gap: 8rpx;
  margin-bottom: 4rpx;
}

.notify-label,
.feedback-label {
  font-size: 22rpx;
  color: #9ca3af;
}

.notify-value {
  font-size: 22rpx;
  color: #1a1a2e;
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.feedback-value {
  font-size: 22rpx;
  color: #1a1a2e;
}

.notify-content {
  font-size: 22rpx;
  color: #4b5563;
  line-height: 1.5;
}

.notify-time {
  font-size: 20rpx;
  color: #9ca3af;
  display: block;
  margin-top: 4rpx;
}

/* 反馈记录 */
.feedback-item {
  padding: 16rpx 0;
  border-bottom: 1rpx solid #f5f5f5;

  &:last-child { border-bottom: none; }
}

.feedback-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8rpx;
}

.feedback-action {
  font-size: 22rpx;
  padding: 4rpx 12rpx;
  border-radius: 6rpx;

  &.feedback-action-primary { background: rgba(48, 104, 228, 0.1); color: #3068e4; }
  &.feedback-action-success { background: rgba(82, 196, 26, 0.1); color: #52c41a; }
  &.feedback-action-warning { background: rgba(255, 125, 0, 0.1); color: #ff7d00; }
  &.feedback-action-info { background: rgba(144, 147, 153, 0.1); color: #909399; }
}

.feedback-time {
  font-size: 22rpx;
  color: #9ca3af;
}

.feedback-operator {
  margin-bottom: 4rpx;
}

.feedback-content {
  font-size: 24rpx;
  color: #4b5563;
  line-height: 1.5;
  display: block;
}

/* 时间线 */
.timeline-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16rpx;
}

.timeline-toggle {
  font-size: 24rpx;
  color: #3068e4;
}

.timeline-card {
  background: #ffffff;
  border-radius: 24rpx;
  padding: 24rpx;
  box-shadow: 0 8rpx 32rpx rgba(102, 126, 234, 0.12);
}

.timeline-item {
  position: relative;
  padding-left: 40rpx;
  padding-bottom: 32rpx;

  &:last-child { padding-bottom: 0; }
}

.timeline-dot {
  position: absolute;
  left: 0;
  top: 8rpx;
  width: 16rpx;
  height: 16rpx;
  border-radius: 50%;
  background: #d9d9d9;

  &.type-current { background: #f53f3f; box-shadow: 0 0 0 6rpx rgba(245, 63, 63, 0.15); }
  &.type-ended { background: #909399; }
  &.type-trigger { background: #3068e4; }
  &.type-notify { background: #fa8c16; }
  &.type-dispose { background: #52c41a; }
}

.timeline-line {
  position: absolute;
  left: 7rpx;
  top: 28rpx;
  width: 2rpx;
  height: calc(100% - 28rpx);
  background: #f0f0f0;
}

.timeline-content {
  display: flex;
  flex-direction: column;
  gap: 4rpx;
}

.timeline-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8rpx;
}

.timeline-label {
  font-size: 26rpx;
  color: #1a1a2e;
  font-weight: 500;
}

.timeline-time {
  font-size: 22rpx;
  color: #9ca3af;
}

.timeline-desc {
  font-size: 24rpx;
  color: #4b5563;
  line-height: 1.5;
}

.timeline-operator {
  font-size: 22rpx;
  color: #9ca3af;
}

/* 底部操作栏 */
.action-bar {
  display: flex;
  gap: 16rpx;
  padding: 16rpx 32rpx;
  background: #ffffff;
  box-shadow: 0 -4rpx 16rpx rgba(0, 0, 0, 0.04);
  flex-shrink: 0;
}

.action-btn {
  flex: 1;
  padding: 24rpx 0;
  border-radius: 16rpx;
  text-align: center;
  font-size: 28rpx;
  font-weight: 600;
  color: #ffffff;
  box-sizing: border-box;

  &:active {
    opacity: 0.9;
  }
}

.feedback-btn {
  background: linear-gradient(135deg, #3068e4 0%, #1e5acc 100%);
}

.false-btn {
  background: linear-gradient(135deg, #fa8c16 0%, #d87a04 100%);
}

.clear-btn {
  background: linear-gradient(135deg, #f5222d 0%, #cf1322 100%);
}

.bottom-spacer {
  height: 60rpx;
}

/* 回到顶部按钮 */
.back-top-btn {
  position: fixed;
  right: 32rpx;
  bottom: 200rpx;
  width: 88rpx;
  height: 88rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #3068e4 0%, #1e5acc 100%);
  box-shadow: 0 8rpx 24rpx rgba(48, 104, 228, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10;

  &:active {
    transform: scale(0.9);
    opacity: 0.85;
  }
}

</style>
