<!-- src/pages/index.vue -->
<template>
  <view class="page-container">
    <!-- 渐变头部 -->
    <view class="header">
      <view class="header-bg">
        <view class="status-bar" :style="{ height: `${statusBarHeight + 322}rpx` }"></view>
        <view class="bg-circle bg-circle-1"></view>
        <view class="bg-circle bg-circle-2"></view>
      </view>
      <view class="header-content" :style="{ paddingTop: `${statusBarHeight}px` }">
        <view class="header-top">
          <text class="header-title">事件大厅</text>
          <text class="header-subtitle">边坡监测 · 智能预警</text>
        </view>
        <view class="stats-row">
          <view class="stat-item stat-red">
            <text class="stat-value">{{ stats.red }}</text>
            <text class="stat-label">红色</text>
          </view>
          <view class="stat-item stat-orange">
            <text class="stat-value">{{ stats.orange }}</text>
            <text class="stat-label">橙色</text>
          </view>
          <view class="stat-item stat-yellow">
            <text class="stat-value">{{ stats.yellow }}</text>
            <text class="stat-label">黄色</text>
          </view>
          <view class="stat-item stat-blue">
            <text class="stat-value">{{ stats.blue }}</text>
            <text class="stat-label">蓝色</text>
          </view>
        </view>
      </view>
    </view>

    <!-- Tab切换：待处理 / 历史事件 -->
    <view class="tab-bar">
      <view class="tab-item" :class="{ active: activeTab === 'pending' }" @click="activeTab = 'pending'">
        <text class="tab-text">待处理</text>
        <text v-if="pendingCount > 0" class="tab-badge">{{ pendingCount }}</text>
      </view>
      <view class="tab-item" :class="{ active: activeTab === 'history' }" @click="activeTab = 'history'">
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
    >
      <view class="list-inner">
        <!-- 骨架屏 -->
        <view v-if="loading" class="skeleton-wrapper">
          <Skeleton :rows="4" />
          <Skeleton :rows="4" />
          <Skeleton :rows="4" />
        </view>

        <!-- 列表内容 -->
        <view v-else>
          <view
            v-for="alarm in displayAlarms"
            :key="alarm.id"
            class="alarm-card"
            @click="goToDetail(alarm)"
            hover-class="alarm-card-hover"
          >
            <!-- 左侧颜色条 -->
            <view class="alarm-bar" :style="{ backgroundColor: getLevelColor(alarm.alarmLevel) }"></view>

            <view class="alarm-body">
              <view class="alarm-header">
                <text class="alarm-hazard-name">{{ alarm.hazardName }}</text>
                <view class="alarm-tag" :style="{ backgroundColor: getLevelColor(alarm.alarmLevel) + '15', color: getLevelColor(alarm.alarmLevel) }">
                  <text class="alarm-tag-text">{{ alarm.alarmType }}</text>
                </view>
              </view>
              <text class="alarm-content">{{ alarm.alarmContent }}</text>
              <view class="alarm-footer">
                <text class="alarm-device">{{ alarm.deviceName }}</text>
                <text class="alarm-time">{{ formatTime(alarm.createTime) }}</text>
              </view>
            </view>
          </view>

          <EmptyState
            v-if="displayAlarms.length === 0"
            :useImage="true"
            :title="activeTab === 'pending' ? '暂无告警事件' : '暂无历史事件'"
            :description="activeTab === 'pending' ? '当前没有未处理的告警事件' : '暂无已处理的告警事件'"
          />
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import EmptyState from '@/components/EmptyState.vue'
import Skeleton from '@/components/Skeleton.vue'
import { useSafeArea } from '@/composables/useSafeArea'
import { alarmApi } from '@/utils/alarm'
import type { Alarm } from '@/utils/alarm'
import { startPolling, stopPolling } from '@/utils/polling'
import { computed, onMounted, onUnmounted, ref } from 'vue'

const { statusBarHeight } = useSafeArea()

const loading = ref(true)
const isRefreshing = ref(false)
const activeTab = ref<'pending' | 'history'>('pending')
const allAlarms = ref<Alarm[]>([])
const stats = ref({ red: 0, orange: 0, yellow: 0, blue: 0 })

const pendingCount = computed(() => allAlarms.value.filter(a => a.status === 0).length)

const displayAlarms = computed(() => {
  if (activeTab.value === 'pending') {
    return allAlarms.value.filter(a => a.status === 0)
  }
  return allAlarms.value.filter(a => a.status === 1)
})

const getLevelColor = (level: number) => {
  const map: Record<number, string> = { 4: '#f5222d', 3: '#fa541c', 2: '#faad14', 1: '#1890ff' }
  return map[level] || '#1890ff'
}

const formatTime = (time: string) => {
  if (!time) return '-'
  const iosTime = time.replace(' ', 'T').replace(/\.\d+Z$/, '')
  const date = new Date(iosTime)
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${month}-${day} ${hours}:${minutes}`
}

const loadData = async () => {
  try {
    const accessToken = uni.getStorageSync('accessToken')
    if (!accessToken) {
      uni.redirectTo({ url: '/pages/login' })
      return
    }

    const [alarmStats, alarmList] = await Promise.all([
      alarmApi.getStats(),
      alarmApi.getUnprocessed()
    ])

    if (alarmStats) {
      stats.value = {
        red: alarmStats.red || 0,
        orange: alarmStats.orange || 0,
        yellow: alarmStats.yellow || 0,
        blue: alarmStats.blue || 0
      }
    }

    // 同时获取所有告警（含已处理）用于历史事件
    allAlarms.value = (alarmApi as any).getAll ? (alarmApi as any).getAll() : (alarmList || [])
  } catch (error) {
    console.error('加载数据失败:', error)
  } finally {
    loading.value = false
    isRefreshing.value = false
  }
}

const onRefresh = async () => {
  isRefreshing.value = true
  await loadData()
}

const goToDetail = (alarm: Alarm) => {
  uni.navigateTo({ url: `/pages/alarm-detail?id=${alarm.id}` })
}

onMounted(() => {
  loadData()
  startPolling(loadData, 30000)
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
.bg-circle-2 { width: 200rpx; height: 200rpx; top: 120rpx; left: -50rpx; }

.header-content {
  position: relative;
  z-index: 1;
  padding: 40rpx 32rpx 24rpx;
}

.header-top { margin-bottom: 24rpx; }

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

.stats-row {
  display: flex;
  gap: 24rpx;
}

.stat-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20rpx 0;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 16rpx;
  backdrop-filter: blur(10px);

  .stat-value {
    font-size: 36rpx;
    font-weight: bold;
    background: rgba(255, 255, 255, 0.9);
    width: 48rpx;
    height: 48rpx;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 8rpx;
  }

  .stat-label {
    font-size: 22rpx;
    color: rgba(255, 255, 255, 0.9);
  }

  &.stat-red .stat-value { color: #f5222d; }
  &.stat-orange .stat-value { color: #fa541c; }
  &.stat-yellow .stat-value { color: #faad14; }
  &.stat-blue .stat-value { color: #1890ff; }
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

.alarm-tag {
  flex-shrink: 0;
  padding: 4rpx 16rpx;
  border-radius: 8rpx;
}

.alarm-tag-text {
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
}

.alarm-device {
  font-size: 22rpx;
  color: #3068e4;
  font-weight: 500;
}

.alarm-time {
  font-size: 22rpx;
  color: #9ca3af;
}
</style>
