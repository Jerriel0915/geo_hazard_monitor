<!-- src/pages/alarm-handle.vue -->
<template>
  <view class="page-container">
    <!-- 渐变头部 -->
    <view class="header">
      <view class="header-bg">
        <view class="status-bar" :style="{ height: `${statusBarHeight + 65}px` }"></view>
        <view class="bg-circle bg-circle-1"></view>
        <view class="bg-circle bg-circle-2"></view>
      </view>
      <view class="header-content" :style="{ paddingTop: `${statusBarHeight}px` }">
        <view class="header-top">
          <text class="header-title">告警中心</text>
          <text class="header-subtitle">实时告警监控与处理</text>
        </view>
      </view>
    </view>

    <!-- 可滚动内容区域 -->
    <scroll-view
      class="content-scroll"
      scroll-y
      refresher-enabled
      :refresher-triggered="isRefreshing"
      @refresherrefresh="onRefresh"
      @scrolltolower="onLoadMore"
      lower-threshold="100"
    >
      <!-- 筛选器 -->
      <view class="filter-tabs">
        <view
          v-for="tab in filterTabs"
          :key="tab.value"
          class="filter-tab"
          :class="{ active: activeFilter === tab.value }"
          @click="changeFilter(tab.value)"
        >
          {{ tab.label }}
        </view>
      </view>

      <!-- 告警列表 -->
      <view class="alarm-list-content">
        <view v-if="loading && pageNum === 1" class="loading-wrapper">
          <text>加载中...</text>
        </view>

        <view v-else>
          <view
            v-for="alarm in alarmList"
            :key="alarm.id"
            class="alarm-card"
            @click="showAlarmDetail(alarm)"
          >
            <view class="alarm-header">
              <view class="alarm-level" :class="getAlarmLevelClass(alarm.alarmLevel)">
                {{ getAlarmLevelText(alarm.alarmLevel) }}
              </view>
              <text class="alarm-time">{{ formatTime(alarm.createTime) }}</text>
            </view>
            <view class="alarm-body">
              <text class="alarm-container">{{ alarm.containerNo || alarm.containerName || '未知集装箱' }}</text>
              <text class="alarm-device">设备: {{ alarm.deviceName || '未知' }}</text>
              <text class="alarm-rule">{{ alarm.alarmRuleDescription || alarm.alarmContent }}</text>
              <view v-if="alarm.alarmValue" class="alarm-value-row">
                <text class="alarm-attribute">{{ alarm.alarmAttribute || '' }}</text>
                <text class="alarm-value">{{ alarm.alarmValue }}</text>
              </view>
            </view>
            <view class="alarm-footer">
              <view class="alarm-status" :class="alarm.status === 1 ? 'handled' : 'pending'">
                {{ alarm.status === 1 ? '已处理' : '待处理' }}
              </view>
              <!-- 只有待处理状态才显示处理按钮 -->
              <view v-if="alarm.status !== 1" class="alarm-action" @click.stop="handleAlarm(alarm)">
                处理
              </view>
            </view>
          </view>

          <EmptyState
            v-if="alarmList.length === 0 && !loading"
            title="暂无告警"
            :description="activeFilter === 0 ? '暂无待处理的告警' : '暂无已处理的告警'"
          />

          <view v-if="!hasMore && alarmList.length > 0" class="no-more-wrapper">
            <text class="no-more-text">没有更多了</text>
          </view>

          <view v-if="loading && pageNum > 1" class="loading-more-wrapper">
            <text class="loading-more-text">加载中...</text>
          </view>
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useSafeArea } from '@/composables/useSafeArea'
import EmptyState from '@/components/EmptyState.vue'
import alarmApi from '@/utils/alarm'

const { statusBarHeight } = useSafeArea()

interface Alarm {
  id: number
  containerId: number
  containerNo?: string
  containerName?: string
  deviceId: number
  deviceName?: string
  alarmAttribute?: string
  alarmValue?: string
  alarmLevel: number
  alarmContent: string
  alarmRuleDescription?: string
  status: number
  createTime: string
}

const filterTabs = [
  { label: '待处理', value: 0 },
  { label: '已处理', value: 1 }
]

const loading = ref(false)
const isRefreshing = ref(false)
const alarmList = ref<Alarm[]>([])
const activeFilter = ref(0)  // 默认显示待处理

// 分页状态
const pageNum = ref(1)
const pageSize = 20
const hasMore = ref(true)

onMounted(() => {
  loadData(true)
})

const loadData = async (reset = false) => {
  if (reset) {
    pageNum.value = 1
    alarmList.value = []
    hasMore.value = true
  }

  if (loading.value || !hasMore.value) return

  try {
    loading.value = true
    const result = await alarmApi.getPage(pageNum.value, pageSize, activeFilter.value)

    if (result && result.list) {
      if (reset) {
        alarmList.value = result.list
      } else {
        alarmList.value.push(...result.list)
      }

      hasMore.value = result.list.length >= pageSize
    }
  } catch (error) {
    console.error('加载告警数据失败:', error)
    uni.showToast({ title: '加载数据失败', icon: 'none' })
  } finally {
    loading.value = false
    isRefreshing.value = false
  }
}

const onRefresh = async () => {
  isRefreshing.value = true
  await loadData(true)
}

const onLoadMore = () => {
  if (!loading.value && hasMore.value) {
    pageNum.value++
    loadData(false)
  }
}

const changeFilter = (value: number) => {
  activeFilter.value = value
  loadData(true)
}

const getAlarmLevelClass = (level: number) => {
  switch (level) {
    case 4: return 'danger'
    case 3: return 'danger'
    case 2: return 'warning'
    default: return 'info'
  }
}

const getAlarmLevelText = (level: number) => {
  switch (level) {
    case 4: return '紧急'
    case 3: return '严重'
    case 2: return '一般'
    default: return '提示'
  }
}

const formatTime = (time: string) => {
  if (!time) return '-'
  const iosTime = time.replace(' ', 'T').replace(/\.\\d+Z$/, '')
  const date = new Date(iosTime)
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${month}-${day} ${hours}:${minutes}`
}

const showAlarmDetail = (alarm: Alarm) => {
  uni.navigateTo({
    url: `/pages/alarm-detail?id=${alarm.id}&deviceId=${alarm.deviceId}`
  })
}

const handleAlarm = async (alarm: Alarm) => {
  // 先显示输入框让用户输入处理说明
  let handleRemark = ''

  // 使用 prompt 让用户输入处理说明
  uni.showModal({
    title: '处理告警',
    editable: true,
    placeholderText: '请输入处理说明...',
    success: async (res) => {
      if (res.confirm) {
        handleRemark = res.content || '已处理'
        try {
          await alarmApi.handle(alarm.id, handleRemark)
          uni.showToast({ title: '处理成功', icon: 'success' })
          await loadData(true)
        } catch (error) {
          console.error('处理告警失败:', error)
          uni.showToast({ title: '处理失败', icon: 'none' })
        }
      }
    }
  })
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

.status-bar {
  width: 100%;
}

.bg-circle {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
}

.bg-circle-1 {
  width: 300rpx;
  height: 300rpx;
  top: -80rpx;
  right: -60rpx;
}

.bg-circle-2 {
  width: 200rpx;
  height: 200rpx;
  top: 140rpx;
  left: -50rpx;
}

.header-content {
  position: relative;
  z-index: 1;
  padding: 0 32rpx 24rpx;
}

.header-top {
  margin-bottom: 24rpx;
}

.header-title {
  font-size: 40rpx;
  font-weight: bold;
  color: #ffffff;
  margin-bottom: 8rpx;
  display: block;
}

.header-subtitle {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.8);
}

.content-scroll {
  flex: 1;
  height: 0;
}

.filter-tabs {
  display: flex;
  gap: 16rpx;
  padding: 0 32rpx 20rpx;
}

.filter-tab {
  flex: 1;
  text-align: center;
  padding: 20rpx 0;
  background: #ffffff;
  border-radius: 16rpx;
  font-size: 26rpx;
  color: #6b7280;
  transition: all 0.3s ease;

  &.active {
    background: linear-gradient(135deg, #3068e4 0%, #1e5acc 100%);
    color: #ffffff;
  }
}

.alarm-list-content {
  padding: 0 32rpx 32rpx;
}

.loading-wrapper {
  display: flex;
  justify-content: center;
  padding: 80rpx 0;
  color: #9ca3af;
}

.alarm-card {
  background: #ffffff;
  border-radius: 24rpx;
  padding: 24rpx;
  margin-bottom: 16rpx;
  box-shadow: 0 8rpx 32rpx rgba(102, 126, 234, 0.15);
  border: 1rpx solid rgba(102, 126, 234, 0.08);
}

.alarm-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16rpx;
}

.alarm-level {
  padding: 6rpx 16rpx;
  border-radius: 8rpx;
  font-size: 22rpx;

  &.danger {
    background: rgba(245, 34, 45, 0.1);
    color: #f5222d;
  }

  &.warning {
    background: rgba(250, 173, 20, 0.1);
    color: #faad14;
  }

  &.info {
    background: rgba(24, 144, 255, 0.1);
    color: #1890ff;
  }
}

.alarm-time {
  font-size: 22rpx;
  color: #9ca3af;
}

.alarm-body {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
  margin-bottom: 16rpx;
}

.alarm-container {
  font-size: 26rpx;
  color: #3068e4;
  font-weight: 500;
}

.alarm-device {
  font-size: 24rpx;
  color: #6b7280;
  margin-bottom: 4rpx;
}

.alarm-rule {
  font-size: 26rpx;
  color: #1a1a2e;
  line-height: 1.5;
}

.alarm-value-row {
  display: flex;
  align-items: center;
  gap: 8rpx;
  padding: 12rpx 16rpx;
  background: linear-gradient(135deg, rgba(245, 34, 45, 0.08) 0%, rgba(245, 34, 45, 0.03) 100%);
  border-radius: 12rpx;
  border: 1rpx solid rgba(245, 34, 45, 0.15);
  margin-top: 4rpx;
}

.alarm-attribute {
  font-size: 24rpx;
  color: #6b7280;
}

.alarm-value {
  font-size: 28rpx;
  font-weight: 600;
  color: #f5222d;
}

.alarm-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.alarm-status {
  font-size: 22rpx;
  padding: 6rpx 16rpx;
  border-radius: 8rpx;

  &.handled {
    background: rgba(82, 196, 26, 0.1);
    color: #52c41a;
  }

  &.pending {
    background: rgba(250, 173, 20, 0.1);
    color: #faad14;
  }
}

.alarm-action {
  padding: 10rpx 24rpx;
  background: linear-gradient(135deg, #3068e4 0%, #1e5acc 100%);
  border-radius: 8rpx;
  font-size: 24rpx;
  color: #ffffff;
}

.no-more-wrapper {
  display: flex;
  justify-content: center;
  padding: 30rpx 0;
}

.no-more-text {
  font-size: 24rpx;
  color: #d1d5db;
}

.loading-more-wrapper {
  display: flex;
  justify-content: center;
  padding: 30rpx 0;
}

.loading-more-text {
  font-size: 24rpx;
  color: #9ca3af;
}
</style>
