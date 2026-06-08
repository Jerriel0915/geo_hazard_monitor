<!-- src/pages/alarm-detail.vue -->
<template>
  <view class="page-container">
    <!-- 头部 -->
    <view class="header">
      <view class="header-bg" :style="{ height: `calc(${statusBarHeight}px + 155rpx)` }"></view>
      <view class="header-content" :style="{ marginTop: `${statusBarHeight}px` }">
        <view class="header-nav">
          <view class="back-btn" @click="goBack">←</view>
        </view>
        <view class="alarm-info-top">
          <view class="alarm-level-badge" :style="{ background: getLevelColor(alarm.alarmLevel) }">
            {{ getLevelText(alarm.alarmLevel) }}预警
          </view>
          <text class="alarm-time-text">{{ alarm.createTime }}</text>
        </view>
      </view>
    </view>

    <!-- 内容 -->
    <scroll-view class="content-scroll" scroll-y>
      <!-- 告警信息 -->
      <view class="section">
        <text class="section-title">告警信息</text>
        <view class="info-card">
          <view class="info-row">
            <text class="info-label">隐患点</text>
            <text class="info-value">{{ alarm.hazardName || '-' }}</text>
          </view>
          <view class="info-row">
            <text class="info-label">告警类型</text>
            <view class="type-tag" :class="alarm.alarmType === '综合预警' ? 'comprehensive' : 'threshold'">
              {{ alarm.alarmType }}
            </view>
          </view>
          <view class="info-row">
            <text class="info-label">告警内容</text>
            <text class="info-value content-text">{{ alarm.alarmContent || '-' }}</text>
          </view>
          <view class="info-row" v-if="alarm.alarmValue && alarm.alarmValue !== '-'">
            <text class="info-label">告警数值</text>
            <text class="info-value alarm-value">{{ alarm.alarmValue }}</text>
          </view>
          <view class="info-row">
            <text class="info-label">告警次数</text>
            <text class="info-value">{{ alarm.alarmCount }}次</text>
          </view>
          <view class="info-row">
            <text class="info-label">状态</text>
            <view class="status-badge" :class="alarm.status === 1 ? 'handled' : 'pending'">
              {{ alarm.status === 1 ? '已处理' : '待处理' }}
            </view>
          </view>
          <view v-if="alarm.handleTime" class="info-row">
            <text class="info-label">处理时间</text>
            <text class="info-value">{{ alarm.handleTime }}</text>
          </view>
          <view v-if="alarm.handleRemark" class="info-row">
            <text class="info-label">处理备注</text>
            <text class="info-value">{{ alarm.handleRemark }}</text>
          </view>
        </view>
      </view>

      <!-- 分发日志 -->
      <view class="section">
        <text class="section-title">分发日志</text>
        <view class="timeline-card">
          <view
            v-for="(log, index) in alarm.dispatchLogs"
            :key="log.id"
            class="timeline-item"
          >
            <view class="timeline-dot" :class="{ active: index === 0 }"></view>
            <view class="timeline-line" v-if="index < alarm.dispatchLogs.length - 1"></view>
            <view class="timeline-content">
              <text class="timeline-action">{{ log.action }}</text>
              <view class="timeline-meta">
                <text class="timeline-operator">{{ log.operator }}</text>
                <text class="timeline-time">{{ log.time }}</text>
              </view>
            </view>
          </view>
          <view v-if="!alarm.dispatchLogs || alarm.dispatchLogs.length === 0" class="empty-logs">
            <text class="empty-text">暂无分发日志</text>
          </view>
        </view>
      </view>

      <!-- 关联设备 -->
      <view class="section">
        <text class="section-title">关联设备</text>
        <view class="device-list">
          <view
            v-for="device in relatedDevices"
            :key="device.id"
            class="device-item"
            @click="goToDeviceDetail(device.id)"
          >
            <view class="device-info">
              <text class="device-name">{{ device.deviceName }}</text>
              <text class="device-type">{{ device.deviceType }}</text>
            </view>
            <view class="device-status">
              <view class="status-dot" :class="device.status === '在线' ? 'online' : device.status === '故障' ? 'fault' : 'offline'"></view>
              <text class="status-text">{{ device.status }}</text>
            </view>
          </view>
          <view v-if="relatedDevices.length === 0" class="empty-logs">
            <text class="empty-text">暂无关联设备</text>
          </view>
        </view>
      </view>

      <!-- 底部操作按钮 -->
      <view v-if="alarm.status !== 1" class="section">
        <view class="action-row">
          <view class="action-btn feedback-btn" @click="handleFeedback">反馈</view>
          <view class="action-btn false-btn" @click="handleFalseAlarm">误报</view>
          <view class="action-btn clear-btn" @click="handleClear">消警</view>
          <view class="action-btn notify-btn" @click="handleNotify">通知</view>
        </view>
      </view>

      <view class="bottom-spacer" />
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useSafeArea } from '@/composables/useSafeArea'
import { alarmApi } from '@/utils/alarm'
import { deviceApi } from '@/utils/device'
import type { Alarm } from '@/utils/alarm'
import type { DeviceInfo } from '@/utils/device'

const { statusBarHeight } = useSafeArea()

const alarm = ref<Partial<Alarm>>({
  dispatchLogs: []
})
const relatedDevices = ref<DeviceInfo[]>([])

onLoad((options) => {
  if (options?.id) {
    loadAlarmDetail(Number(options.id))
  }
})

const loadAlarmDetail = (id: number) => {
  const detail = alarmApi.getById(id)
  if (detail) {
    alarm.value = detail
    loadRelatedDevices(detail)
  } else {
    uni.showToast({ title: '告警不存在', icon: 'none' })
    setTimeout(() => uni.navigateBack(), 1500)
  }
}

const loadRelatedDevices = (alarmData: Alarm) => {
  if (alarmData.alarmType === '综合预警') {
    // 综合预警：该隐患点下所有设备
    relatedDevices.value = deviceApi.getByHazardId(alarmData.hazardId)
  } else {
    // 阈值预警：单设备
    const device = deviceApi.getById(alarmData.deviceId)
    relatedDevices.value = device ? [device] : []
  }
}

const getLevelColor = (level: number) => {
  const map: Record<number, string> = { 4: '#f5222d', 3: '#fa541c', 2: '#faad14', 1: '#1890ff' }
  return map[level] || '#1890ff'
}

const getLevelText = (level: number) => {
  const map: Record<number, string> = { 4: '红色', 3: '橙色', 2: '黄色', 1: '蓝色' }
  return map[level] || '蓝色'
}

const handleFeedback = () => {
  uni.showModal({
    title: '反馈',
    editable: true,
    placeholderText: '请输入反馈说明...',
    success: async (res) => {
      if (res.confirm) {
        const remark = res.content || '已反馈'
        alarmApi.handle(alarm.value.id!, remark)
        uni.showToast({ title: '反馈成功', icon: 'success' })
        alarm.value.handleTime = new Date().toISOString()
        alarm.value.handleRemark = remark
      }
    }
  })
}

const handleFalseAlarm = () => {
  uni.showModal({
    title: '确认误报',
    content: '确定将此告警标记为误报吗？',
    success: (res) => {
      if (res.confirm) {
        alarmApi.handle(alarm.value.id!, '误报')
        uni.showToast({ title: '已标记误报', icon: 'success' })
        alarm.value.status = 1
        alarm.value.handleTime = new Date().toISOString()
        alarm.value.handleRemark = '误报'
        setTimeout(() => uni.navigateBack(), 1500)
      }
    }
  })
}

const handleClear = () => {
  uni.showModal({
    title: '消警',
    editable: true,
    placeholderText: '请输入消警说明...',
    success: (res) => {
      if (res.confirm) {
        const remark = res.content || '已消警'
        alarmApi.handle(alarm.value.id!, remark)
        uni.showToast({ title: '已消警', icon: 'success' })
        alarm.value.status = 1
        alarm.value.handleTime = new Date().toISOString()
        alarm.value.handleRemark = remark
        setTimeout(() => uni.navigateBack(), 1500)
      }
    }
  })
}

const handleNotify = () => {
  uni.showModal({
    title: '通知',
    editable: true,
    placeholderText: '请输入通知内容...',
    success: (res) => {
      if (res.confirm) {
        const content = res.content || '请关注告警信息'
        uni.showToast({ title: '通知已发送', icon: 'success' })
        alarm.value.dispatchLogs = [
          { id: Date.now(), action: `手动通知: ${content}`, operator: '当前用户', time: new Date().toLocaleString() },
          ...(alarm.value.dispatchLogs || [])
        ]
      }
    }
  })
}

const goToDeviceDetail = (id: number) => {
  uni.navigateTo({ url: `/pages/device-detail?id=${id}` })
}

const goBack = () => {
  uni.navigateBack()
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

.header-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  background: linear-gradient(135deg, #3068e4 0%, #1e5acc 100%);
  border-radius: 0 0 15rpx 15rpx;
  overflow: hidden;
}

.header-content {
  position: relative;
  z-index: 1;
  padding: 0 32rpx 24rpx;
}

.header-nav {
  display: flex;
  align-items: center;
  margin-bottom: 24rpx;
}

.back-btn {
  width: 64rpx;
  height: 64rpx;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36rpx;
  color: #ffffff;
}

.alarm-info-top {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.alarm-level-badge {
  padding: 8rpx 20rpx;
  border-radius: 8rpx;
  font-size: 24rpx;
  color: #ffffff;
}

.alarm-time-text {
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.85);
}

.content-scroll {
  flex: 1;
  height: 0;
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

  &:last-child { border-bottom: none; }
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

  &.content-text {
    max-width: 420rpx;
    line-height: 1.5;
  }

  &.alarm-value {
    color: #f5222d;
    font-weight: 600;
  }
}

.type-tag {
  padding: 6rpx 16rpx;
  border-radius: 8rpx;
  font-size: 22rpx;

  &.threshold {
    background: rgba(48, 104, 228, 0.1);
    color: #3068e4;
  }

  &.comprehensive {
    background: rgba(250, 173, 20, 0.1);
    color: #faad14;
  }
}

.status-badge {
  padding: 6rpx 16rpx;
  border-radius: 8rpx;
  font-size: 22rpx;

  &.handled {
    background: rgba(82, 196, 26, 0.1);
    color: #52c41a;
  }

  &.pending {
    background: rgba(250, 173, 20, 0.1);
    color: #faad14;
  }
}

/* 时间线 */
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

  &.active { background: #3068e4; }
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

.timeline-action {
  font-size: 26rpx;
  color: #1a1a2e;
  font-weight: 500;
}

.timeline-meta {
  display: flex;
  gap: 16rpx;
}

.timeline-operator {
  font-size: 22rpx;
  color: #9ca3af;
}

.timeline-time {
  font-size: 22rpx;
  color: #9ca3af;
}

.empty-logs {
  padding: 40rpx 0;
  text-align: center;
}

.empty-text {
  font-size: 24rpx;
  color: #9ca3af;
}

/* 关联设备 */
.device-list {
  background: #ffffff;
  border-radius: 24rpx;
  padding: 8rpx 24rpx;
  box-shadow: 0 8rpx 32rpx rgba(102, 126, 234, 0.12);
}

.device-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20rpx 0;
  border-bottom: 1rpx solid #f5f5f5;

  &:last-child { border-bottom: none; }
}

.device-info {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.device-name {
  font-size: 28rpx;
  color: #1a1a2e;
  font-weight: 500;
}

.device-type {
  font-size: 22rpx;
  color: #6b7280;
  background: #f5f5f5;
  padding: 4rpx 12rpx;
  border-radius: 6rpx;
}

.device-status {
  display: flex;
  align-items: center;
  gap: 8rpx;
}

.status-dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;

  &.online { background: #52c41a; }
  &.offline { background: #d9d9d9; }
  &.fault { background: #f5222d; }
}

.status-text {
  font-size: 24rpx;
  color: #6b7280;
}

/* 操作按钮 */
.action-row {
  display: flex;
  gap: 16rpx;
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

.notify-btn {
  background: linear-gradient(135deg, #52c41a 0%, #389e0d 100%);
}

.bottom-spacer {
  height: 32rpx;
}
</style>
