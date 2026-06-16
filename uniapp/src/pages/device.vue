<!-- src/pages/device.vue -->
<script setup lang="ts">
import type { DeviceInfo } from '@/utils/device'
import { computed, onMounted, ref } from 'vue'
import { useSafeArea } from '@/composables/useSafeArea'
import { deviceApi } from '@/utils/device'

const { statusBarHeight } = useSafeArea()

const TIME_SUFFIX_RE = /:\d{2}$/

const keyword = ref('')
const activeType = ref('')
const showTypeFilter = ref(false)
const isRefreshing = ref(false)
const devices = ref<DeviceInfo[]>([])

const deviceTypes = computed(() => {
  const types = new Set(devices.value.map(d => d.deviceType))
  return [...types]
})

const activeTypeLabel = computed(() => activeType.value || '类型')

const filteredDevices = computed(() => {
  let list = devices.value
  if (activeType.value) {
    list = list.filter(d => d.deviceType === activeType.value)
  }
  if (keyword.value.trim()) {
    const kw = keyword.value.trim().toLowerCase()
    list = list.filter(d =>
      d.deviceName.toLowerCase().includes(kw)
      || d.deviceCode.toLowerCase().includes(kw),
    )
  }
  return list
})

onMounted(() => {
  loadDevices()
})

async function loadDevices() {
  try {
    devices.value = await deviceApi.getAll()
  }
  catch (error) {
    console.error('加载设备列表失败:', error)
  }
  finally {
    setTimeout(() => {
      isRefreshing.value = false
    }, 400)
  }
}

function onRefresh() {
  isRefreshing.value = true
  loadDevices()
}

function onSearch(e: any) {
  keyword.value = e.detail.value || ''
}

function clearSearch() {
  keyword.value = ''
}

function selectType(type: string) {
  activeType.value = type
  showTypeFilter.value = false
}

function goToDetail(device: DeviceInfo) {
  uni.navigateTo({ url: `/pages/device-detail?id=${device.id}` })
}

function getTypeColor(type: string): string {
  const colorMap: Record<string, string> = {
    GNSS: '#3068e4',
    雨量计: '#1890ff',
    测斜仪: '#722ed1',
    裂缝计: '#fa8c16',
    水位计: '#13c2c2',
    视频设备: '#eb2f96',
  }
  return colorMap[type] || '#3068e4'
}

function getStatusClass(status: string): string {
  switch (status) {
    case '在线': return 'online'
    case '离线': return 'offline'
    case '维修': return 'fault'
    default: return 'offline'
  }
}

function formatTime(time: string): string {
  if (!time)
    return '-'
  return time.replace(TIME_SUFFIX_RE, '')
}
</script>

<template>
  <view class="page-container">
    <!-- 渐变头部 -->
    <view class="header">
      <view class="header-bg">
        <view class="status-bar" :style="{ height: `${statusBarHeight + 65}px` }" />
        <view class="bg-circle bg-circle-1" />
        <view class="bg-circle bg-circle-2" />
      </view>
      <view class="header-content" :style="{ paddingTop: `${statusBarHeight}px` }">
        <view class="header-top">
          <text class="header-title">设备库</text>
          <text class="header-subtitle">设备运维 · 状态监控</text>
        </view>
      </view>
    </view>

    <!-- 搜索 + 类型筛选 -->
    <view class="filter-bar">
      <view class="filter-row">
        <view class="type-filter-btn" @click="showTypeFilter = !showTypeFilter">
          <text class="filter-text">{{ activeTypeLabel }}</text>
          <text class="filter-arrow" :class="{ open: showTypeFilter }">&#x25BC;</text>
        </view>
        <view class="search-bar">
          <view class="search-icon-wrapper">
            <text class="search-icon">&#x1F50D;</text>
          </view>
          <input
            class="search-input"
            type="text"
            placeholder="搜索设备名称 / 编号"
            placeholder-class="search-placeholder"
            :value="keyword"
            confirm-type="search"
            @input="onSearch"
          >
          <view v-if="keyword" class="search-clear" @click="clearSearch">
            <text class="clear-icon">x</text>
          </view>
        </view>
      </view>
      <!-- 类型下拉列表 -->
      <view v-if="showTypeFilter" class="type-dropdown">
        <view
          class="type-option"
          :class="{ active: activeType === '' }"
          @click="selectType('')"
        >
          全部类型
        </view>
        <view
          v-for="t in deviceTypes"
          :key="t"
          class="type-option"
          :class="{ active: activeType === t }"
          @click="selectType(t)"
        >
          {{ t }}
        </view>
      </view>
    </view>

    <!-- 设备列表 -->
    <scroll-view
      class="device-list"
      scroll-y
      refresher-enabled
      :refresher-triggered="isRefreshing"
      @refresherrefresh="onRefresh"
    >
      <view class="list-inner">
        <view
          v-for="item in filteredDevices"
          :key="item.id"
          class="device-card"
          @click="goToDetail(item)"
        >
          <view class="card-top">
            <view class="card-left">
              <text class="device-name">{{ item.deviceName }}</text>
              <text class="device-code">{{ item.deviceCode }}</text>
            </view>
            <view class="card-right">
              <view class="type-tag" :style="{ background: getTypeColor(item.deviceType) }">
                <text class="type-tag-text">{{ item.deviceType }}</text>
              </view>
              <view class="status-badge" :class="getStatusClass(item.status)">
                <view class="status-dot" :class="getStatusClass(item.status)" />
                <text class="status-text">{{ item.status }}</text>
              </view>
            </view>
          </view>
          <view class="card-bottom">
            <view class="card-info-item">
              <text class="info-label">最近上报</text>
              <text class="info-value">{{ formatTime(item.lastReportTime) }}</text>
            </view>
          </view>
        </view>

        <view v-if="filteredDevices.length === 0" class="empty-state">
          <text class="empty-icon">&#x1F4E1;</text>
          <text class="empty-title">暂无设备</text>
          <text class="empty-desc">{{ keyword || activeType ? '未找到匹配的设备' : '暂无设备数据' }}</text>
        </view>

        <view class="bottom-spacer" />
      </view>
    </scroll-view>
  </view>
</template>

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

/* 搜索 + 类型筛选 */
.filter-bar {
  padding: 20rpx 32rpx;
  background: #ffffff;
  flex-shrink: 0;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.04);
  position: relative;
  z-index: 10;
}

.filter-row {
  display: flex;
  gap: 16rpx;
  align-items: center;
}

.type-filter-btn {
  display: flex;
  align-items: center;
  gap: 6rpx;
  height: 72rpx;
  padding: 0 20rpx;
  background: #f0f5ff;
  border-radius: 36rpx;
  flex-shrink: 0;
  border: 1rpx solid #d6e4ff;
}

.filter-text {
  font-size: 26rpx;
  color: #3068e4;
}

.filter-arrow {
  font-size: 18rpx;
  color: #3068e4;
  transition: transform 0.2s;

  &.open { transform: rotate(180deg); }
}

.search-bar {
  flex: 1;
  display: flex;
  align-items: center;
  height: 72rpx;
  background: #f7f8fc;
  border-radius: 36rpx;
  padding: 0 24rpx;
}

.search-icon-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12rpx;
}

.search-icon {
  font-size: 28rpx;
  opacity: 0.6;
}

.search-input {
  flex: 1;
  height: 72rpx;
  font-size: 26rpx;
  color: #1a1a2e;
}

.search-placeholder {
  color: #9ca3af;
  font-size: 26rpx;
}

.search-clear {
  width: 40rpx;
  height: 40rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #e5e7eb;
  border-radius: 50%;
}

.clear-icon {
  font-size: 22rpx;
  color: #6b7280;
}

/* 类型下拉 */
.type-dropdown {
  margin-top: 16rpx;
  background: #f7f8fc;
  border-radius: 16rpx;
  padding: 8rpx 0;
  border: 1rpx solid #e5e7eb;
}

.type-option {
  padding: 20rpx 24rpx;
  font-size: 26rpx;
  color: #4b5563;

  &.active {
    color: #3068e4;
    font-weight: 600;
  }
}

/* 设备列表 */
.device-list {
  flex: 1;
  height: 0;
}

.list-inner {
  padding: 20rpx 32rpx 0;
}

.device-card {
  background: #ffffff;
  border-radius: 24rpx;
  padding: 28rpx 24rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 8rpx 32rpx rgba(102, 126, 234, 0.12);
  box-sizing: border-box;
  width: 100%;

  &:active { background: #f7f8fc; }
}

.card-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20rpx;
}

.card-left {
  display: flex;
  flex-direction: column;
  gap: 6rpx;
  flex: 1;
  min-width: 0;
}

.device-name {
  font-size: 32rpx;
  font-weight: 600;
  color: #1a1a2e;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.device-code {
  font-size: 24rpx;
  color: #9ca3af;
}

.card-right {
  display: flex;
  align-items: center;
  gap: 12rpx;
  flex-shrink: 0;
}

.type-tag {
  padding: 6rpx 16rpx;
  border-radius: 8rpx;
}

.type-tag-text {
  font-size: 22rpx;
  color: #ffffff;
  font-weight: 500;
}

.status-badge {
  display: flex;
  align-items: center;
  gap: 6rpx;
  padding: 6rpx 14rpx;
  border-radius: 8rpx;

  &.online { background: rgba(82, 196, 26, 0.1); }
  &.offline { background: rgba(156, 163, 175, 0.15); }
  &.fault { background: rgba(245, 34, 45, 0.1); }
}

.status-dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;

  &.online { background: #52c41a; }
  &.offline { background: #9ca3af; }
  &.fault { background: #f5222d; }
}

.status-text {
  font-size: 22rpx;

  .status-badge.online & { color: #52c41a; }
  .status-badge.offline & { color: #9ca3af; }
  .status-badge.fault & { color: #f5222d; }
}

.card-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 20rpx;
  border-top: 1rpx solid #f0f2f5;
}

.card-info-item {
  display: flex;
  flex-direction: column;
  gap: 4rpx;
}

.info-label {
  font-size: 22rpx;
  color: #9ca3af;
}

.info-value {
  font-size: 24rpx;
  color: #4b5563;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 120rpx 0;
}

.empty-icon { font-size: 80rpx; margin-bottom: 24rpx; opacity: 0.6; }
.empty-title { font-size: 32rpx; font-weight: 600; color: #6b7280; margin-bottom: 8rpx; }
.empty-desc { font-size: 24rpx; color: #9ca3af; }

.bottom-spacer { height: 40rpx; }
</style>
