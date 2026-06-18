<!-- src/pages/index.vue -->
<template>
  <view class="page-container">
    <!-- 渐变头部（简化：标题 + 副标题） -->
    <view class="header">
      <view class="header-bg">
        <view class="status-bar" :style="{ height: `${statusBarHeight + 220}rpx` }"></view>
        <view class="bg-circle bg-circle-1"></view>
        <view class="bg-circle bg-circle-2"></view>
      </view>
      <view class="header-content" :style="{ paddingTop: `${statusBarHeight}px` }">
        <text class="header-title">事件大厅</text>
        <text class="header-subtitle">边坡监测 · 智能预警</text>
      </view>
    </view>

    <!-- Tab切换：待处理 / 历史事件 -->
    <view class="tab-bar">
      <view class="tab-item" :class="{ active: activeTab === 'pending' }" @click="switchTab('pending')">
        <text class="tab-text">待处理</text>
        <text v-if="pendingTotal > 0" class="tab-badge">{{ pendingTotal }}</text>
      </view>
      <view class="tab-item" :class="{ active: activeTab === 'history' }" @click="switchTab('history')">
        <text class="tab-text">历史事件</text>
      </view>
    </view>

    <!-- 告警事件列表 -->
    <scroll-view
      class="alarm-list"
      scroll-y
      refresher-enabled
      :refresher-triggered="isRefreshing"
      @refresherrefresh="onRefresh"
      @scrolltolower="onLoadMore"
      lower-threshold="100"
    >
      <view class="list-inner">
        <!-- 骨架屏 -->
        <view v-if="loading && pageNum === 1" class="skeleton-wrapper">
          <Skeleton :rows="4" />
          <Skeleton :rows="4" />
          <Skeleton :rows="4" />
        </view>

        <!-- 列表内容 -->
        <view v-else>
          <view
            v-for="alarm in alarms"
            :key="alarm.id"
            class="alarm-card"
            @click="goToDetail(alarm)"
            hover-class="alarm-card-hover"
          >
            <!-- 左侧颜色条 -->
            <view class="alarm-bar" :style="{ backgroundColor: getAlarmLevelColor(alarm.alarmLevel) }"></view>

            <view class="alarm-body">
              <view class="alarm-header">
                <text class="alarm-hazard-name">{{ alarm.hazardPointName || '-' }}</text>
                <view
                  class="alarm-level-tag"
                  :style="{
                    backgroundColor: getAlarmLevelColor(alarm.alarmLevel) + '20',
                    color: getAlarmLevelColor(alarm.alarmLevel),
                  }"
                >
                  <text class="alarm-level-text">{{ getAlarmLevelText(alarm.alarmLevel) }}</text>
                </view>
              </view>

              <text class="alarm-content">{{ alarm.alarmMessage || '-' }}</text>

              <view class="alarm-footer">
                <view class="alarm-meta-left">
                  <view
                    v-if="alarm.statusName"
                    class="status-tag"
                    :class="getStatusTypeClass(alarm.status)"
                  >
                    {{ alarm.statusName }}
                  </view>
                  <text class="alarm-device">{{ alarm.deviceName || '' }}</text>
                </view>
                <text class="alarm-time">{{ formatTime(alarm.firstTriggerTime) }}</text>
              </view>
            </view>
          </view>

          <EmptyState
            v-if="alarms.length === 0"
            :useImage="true"
            :title="activeTab === 'pending' ? '暂无待处理告警' : '暂无历史事件'"
            :description="activeTab === 'pending' ? '当前没有未处理的告警事件' : '暂无已处理的告警事件'"
          />

          <view v-if="!hasMore && alarms.length > 0" class="no-more-wrapper">
            <text class="no-more-text">没有更多了</text>
          </view>

          <view v-if="loadingMore" class="loading-more-wrapper">
            <text class="loading-more-text">加载中...</text>
          </view>
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import EmptyState from '@/components/EmptyState.vue'
import Skeleton from '@/components/Skeleton.vue'
import { useSafeArea } from '@/composables/useSafeArea'
import { alarmApi, getAlarmLevelColor, getAlarmLevelText, getStatusType } from '@/utils/alarm'
import type { AlarmRecordItem } from '@/utils/alarm'
import { startPolling, stopPolling } from '@/utils/polling'
import { ref, onMounted, onUnmounted } from 'vue'

const { statusBarHeight } = useSafeArea()

const loading = ref(true)
const loadingMore = ref(false)
const isRefreshing = ref(false)
const activeTab = ref<'pending' | 'history'>('pending')

const alarms = ref<AlarmRecordItem[]>([])
const pageNum = ref(1)
const pageSize = 20
const hasMore = ref(true)
/** 待处理总数（用于 tab badge） */
const pendingTotal = ref(0)
/** 当前 tab 的总数（用于判断 hasMore） */
const currentTotal = ref(0)

const getStatusTypeClass = (status: number) => {
  return `status-${getStatusType(status)}`
}

const formatTime = (time: string) => {
  if (!time) return '-'
  const iosTime = time.replace(/-/g, '/').replace(' ', 'T').replace(/\.\d+Z$/, '')
  const date = new Date(iosTime)
  if (isNaN(date.getTime())) return time
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${month}-${day} ${hours}:${minutes}`
}

const loadAlarms = async (reset = false) => {
  if (reset) {
    pageNum.value = 1
    alarms.value = []
    hasMore.value = true
  }

  if (loadingMore.value) return
  if (!reset && !hasMore.value) return

  try {
    if (reset) {
      loading.value = true
    } else {
      loadingMore.value = true
    }

    const params = { pageNum: pageNum.value, pageSize }
    const result = activeTab.value === 'pending'
      ? await alarmApi.getPendingAlarms(params)
      : await alarmApi.getHistoryAlarms(params)

    const rows = result.rows || []
    if (reset) {
      alarms.value = rows
    } else {
      alarms.value.push(...rows)
    }

    currentTotal.value = Number(result.total ?? 0)
    hasMore.value = alarms.value.length < currentTotal.value

    // 待处理 tab 的 total 同步到 badge
    if (activeTab.value === 'pending') {
      pendingTotal.value = currentTotal.value
    } else {
      // 切到历史 tab 时，并行刷新 pending total（不影响当前列表）
      alarmApi.getPendingAlarms({ pageNum: 1, pageSize: 1 })
        .then(r => { pendingTotal.value = Number(r.total ?? 0) })
        .catch(() => { /* ignore */ })
    }
  } catch (error) {
    console.error('加载告警数据失败:', error)
  } finally {
    loading.value = false
    loadingMore.value = false
    isRefreshing.value = false
  }
}

const switchTab = (tab: 'pending' | 'history') => {
  if (activeTab.value === tab) return
  activeTab.value = tab
  loadAlarms(true)
}

const onLoadMore = () => {
  if (loading.value || loadingMore.value || !hasMore.value) return
  pageNum.value++
  loadAlarms(false)
}

const onRefresh = async () => {
  isRefreshing.value = true
  await loadAlarms(true)
}

const goToDetail = (alarm: AlarmRecordItem) => {
  uni.navigateTo({ url: `/pages/alarm-detail?id=${alarm.id}` })
}

onMounted(() => {
  const accessToken = uni.getStorageSync('accessToken')
  if (!accessToken) {
    uni.redirectTo({ url: '/pages/login' })
    return
  }
  loadAlarms(true)
  startPolling(() => loadAlarms(true), 30000)
})

onUnmounted(() => {
  stopPolling()
})
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
  padding-bottom: 20rpx;
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

.status-bar { width: 100%; }

.bg-circle {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
}

.bg-circle-1 { width: 300rpx; height: 300rpx; top: -80rpx; right: -60rpx; }
.bg-circle-2 { width: 200rpx; height: 200rpx; top: 60rpx; left: -50rpx; }

.header-content {
  position: relative;
  z-index: 1;
  padding: 40rpx 32rpx 24rpx;
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

/* Tab 切换栏 */
.tab-bar {
  display: flex;
  padding: 16rpx 32rpx;
  gap: 0;
  background: #ffffff;
  flex-shrink: 0;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.04);
}

.tab-item {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  padding: 16rpx 0;
  position: relative;
  border-bottom: 4rpx solid transparent;

  &.active {
    border-bottom-color: #3068e4;
  }

  &.active .tab-text {
    color: #3068e4;
    font-weight: 600;
  }
}

.tab-text {
  font-size: 28rpx;
  color: #6b7280;
}

.tab-badge {
  font-size: 20rpx;
  background: #f5222d;
  color: #ffffff;
  padding: 2rpx 10rpx;
  border-radius: 16rpx;
  min-width: 28rpx;
  text-align: center;
}

/* 列表 */
.alarm-list {
  flex: 1;
  height: 0;
}

.list-inner {
  padding: 20rpx 32rpx;
}

.skeleton-wrapper {
  background: #ffffff;
  border-radius: 24rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 4rpx 20rpx rgba(102, 126, 234, 0.1);
}

.alarm-card {
  display: flex;
  background: #ffffff;
  border-radius: 24rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 8rpx 32rpx rgba(102, 126, 234, 0.12);
  border: 1rpx solid rgba(102, 126, 234, 0.08);
  overflow: hidden;
  box-sizing: border-box;
  width: 100%;
}

.alarm-card-hover {
  transform: scale(0.98);
  box-shadow: 0 4rpx 16rpx rgba(102, 126, 234, 0.2);
}

.alarm-bar {
  width: 8rpx;
  min-height: 100%;
  border-radius: 4rpx;
  flex-shrink: 0;
  margin-right: 20rpx;
}

.alarm-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12rpx;
  min-width: 0;
  overflow: hidden;
}

.alarm-header {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.alarm-hazard-name {
  font-size: 30rpx;
  font-weight: 600;
  color: #1a1a2e;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.alarm-level-tag {
  flex-shrink: 0;
  padding: 4rpx 14rpx;
  border-radius: 8rpx;
}

.alarm-level-text {
  font-size: 22rpx;
  font-weight: 500;
}

.alarm-content {
  font-size: 26rpx;
  color: #4b5563;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}

.alarm-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 4rpx;
  gap: 8rpx;
}

.alarm-meta-left {
  display: flex;
  align-items: center;
  gap: 12rpx;
  flex: 1;
  min-width: 0;
  overflow: hidden;
}

.status-tag {
  font-size: 20rpx;
  padding: 4rpx 12rpx;
  border-radius: 6rpx;
  flex-shrink: 0;

  &.status-danger { background: rgba(245, 63, 63, 0.1); color: #f53f3f; }
  &.status-warning { background: rgba(255, 125, 0, 0.1); color: #ff7d00; }
  &.status-success { background: rgba(82, 196, 26, 0.1); color: #52c41a; }
  &.status-info { background: rgba(144, 147, 153, 0.1); color: #909399; }
}

.alarm-device {
  font-size: 22rpx;
  color: #3068e4;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.alarm-time {
  font-size: 22rpx;
  color: #9ca3af;
  flex-shrink: 0;
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
