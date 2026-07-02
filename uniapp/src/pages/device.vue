<!-- src/pages/device.vue -->
<script setup lang="ts">
import type { DeviceInfo } from '@/utils/device'
import type { VideoDevice } from '@/utils/video'
import PageHeader from '@/components/PageHeader.vue'
import { computed, onMounted, ref } from 'vue'
import { deviceApi } from '@/utils/device'
import { videoApi } from '@/utils/video'

const TIME_SUFFIX_RE = /:\d{2}$/

const activeTab = ref<'monitor' | 'video'>('monitor')
const keyword = ref('')
const activeType = ref('')
const showTypeFilter = ref(false)
const isRefreshing = ref(false)
const devices = ref<DeviceInfo[]>([])
const videoDevices = ref<VideoDevice[]>([])

const deviceTypes = computed(() => {
  const types = new Set<string>()
  devices.value.forEach((d) => {
    d.monitorTypes?.forEach(t => types.add(t))
  })
  return [...types]
})

const activeTypeLabel = computed(() => activeType.value || '监测类型')

const filteredDevices = computed(() => {
  let list = devices.value
  if (activeType.value) {
    list = list.filter(d => d.monitorTypes?.includes(activeType.value))
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

const filteredVideoDevices = computed(() => {
  if (!keyword.value.trim()) return videoDevices.value
  const kw = keyword.value.trim().toLowerCase()
  return videoDevices.value.filter(d =>
    d.deviceName.toLowerCase().includes(kw)
    || d.deviceCode.toLowerCase().includes(kw),
  )
})

onMounted(() => {
  loadDevices()
  loadVideoDevices()
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

async function loadVideoDevices() {
  try {
    const res = await videoApi.getPage()
    videoDevices.value = res.rows
  }
  catch (error) {
    console.error('加载视频设备列表失败:', error)
  }
}

function onRefresh() {
  isRefreshing.value = true
  if (activeTab.value === 'monitor') {
    loadDevices()
  } else {
    loadVideoDevices()
  }
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

function switchTab(tab: 'monitor' | 'video') {
  activeTab.value = tab
  keyword.value = ''
  activeType.value = ''
}

function goToDetail(device: DeviceInfo) {
  uni.navigateTo({ url: `/pages/device-detail?id=${device.id}` })
}

function goToVideoPlayer(video: VideoDevice) {
  uni.navigateTo({ url: `/pages/video-player?id=${video.id}` })
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

function getVideoStatusClass(online: number | undefined): string {
  return online === 1 ? 'online' : 'offline'
}

function getVideoStatusText(online: number | undefined): string {
  return online === 1 ? '在线' : '离线'
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
    <PageHeader title="设备库" subtitle="设备运维 · 状态监控" />

    <!-- 设备列表 -->
    <scroll-view
      class="page-body"
      scroll-y
      refresher-enabled
      :refresher-triggered="isRefreshing"
      @refresherrefresh="onRefresh"
    >
      <!-- Tab 切换 -->
      <view class="tab-bar">
        <view
          class="tab-item"
          :class="{ active: activeTab === 'monitor' }"
          @click="switchTab('monitor')"
        >
          <text class="tab-text">监测设备</text>
        </view>
        <view
          class="tab-item"
          :class="{ active: activeTab === 'video' }"
          @click="switchTab('video')"
        >
          <text class="tab-text">视频设备</text>
        </view>
      </view>

      <!-- 搜索 + 类型筛选 -->
      <view class="filter-bar">
        <view class="filter-row">
          <view v-if="activeTab === 'monitor'" class="type-filter-btn" @click="showTypeFilter = !showTypeFilter">
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
              :placeholder="activeTab === 'video' ? '搜索视频设备名称 / 编号' : '搜索设备名称 / 编号'"
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
        <!-- 类型下拉列表 (仅监测设备) -->
        <view v-if="showTypeFilter && activeTab === 'monitor'" class="type-dropdown">
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

      <view class="list-inner">
        <!-- 监测设备列表 -->
        <template v-if="activeTab === 'monitor'">
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
                <view class="type-tag" :style="{ background: getTypeColor(item.monitorTypes?.[0] || '') }">
                  <text class="type-tag-text">{{ item.monitorTypes?.[0] || '未分类' }}</text>
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
        </template>

        <!-- 视频设备列表 -->
        <template v-else>
          <view
            v-for="item in filteredVideoDevices"
            :key="item.id"
            class="device-card"
            @click="goToVideoPlayer(item)"
          >
            <view class="card-top">
              <view class="card-left">
                <view class="video-name-row">
                  <image
                    v-if="item.iconPath"
                    class="video-card-icon"
                    :src="item.iconPath"
                    mode="aspectFit"
                  />
                  <text v-else class="video-card-icon-fallback">&#x1F4F7;</text>
                  <text class="device-name">{{ item.deviceName }}</text>
                </view>
                <text class="device-code">{{ item.deviceCode }}</text>
              </view>
              <view class="card-right">
                <view v-if="item.protocolName" class="type-tag" style="background: #eb2f96">
                  <text class="type-tag-text">{{ item.protocolName }}</text>
                </view>
                <view class="status-badge" :class="getVideoStatusClass(item.onlineStatus)">
                  <view class="status-dot" :class="getVideoStatusClass(item.onlineStatus)" />
                  <text class="status-text">{{ getVideoStatusText(item.onlineStatus) }}</text>
                </view>
              </view>
            </view>
            <view v-if="(item.longitude && item.latitude) || item.hazardPointIds" class="card-bottom">
              <view v-if="item.longitude && item.latitude" class="card-info-item">
                <text class="info-label">地址</text>
                <text class="info-value">{{ item.longitude.toFixed(6) }}, {{ item.latitude.toFixed(6) }}</text>
              </view>
              <view v-if="item.hazardPointIds" class="card-info-item">
                <text class="info-label">关联隐患点</text>
                <text class="info-value">ID: {{ item.hazardPointIds }}</text>
              </view>
            </view>
          </view>

          <view v-if="filteredVideoDevices.length === 0" class="empty-state">
            <text class="empty-icon">&#x1F4F7;</text>
            <text class="empty-title">暂无视频设备</text>
            <text class="empty-desc">{{ keyword ? '未找到匹配的视频设备' : '暂无视频设备数据' }}</text>
          </view>
        </template>

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

/* Tab 切换 */
.tab-bar {
  display: flex;
  background: #ffffff;
  padding: 0 32rpx;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.03);
}

.tab-item {
  flex: 1;
  text-align: center;
  padding: 20rpx 0;
  position: relative;

  &.active::after {
    content: '';
    position: absolute;
    bottom: 0;
    left: 25%;
    right: 25%;
    height: 4rpx;
    background: linear-gradient(90deg, #3068e4, #1e5acc);
    border-radius: 2rpx;
  }
}

.tab-text {
  font-size: 28rpx;
  color: #6b7280;

  .tab-item.active & {
    color: #3068e4;
    font-weight: 600;
  }
}

/* 搜索 + 类型筛选 */
.filter-bar {
  padding: 20rpx 32rpx;
  background: #ffffff;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.04);
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
.page-body {
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
  gap: 24rpx;
}

.video-name-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.video-card-icon {
  width: 40rpx;
  height: 40rpx;
  border-radius: 8rpx;
}

.video-card-icon-fallback {
  font-size: 32rpx;
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
