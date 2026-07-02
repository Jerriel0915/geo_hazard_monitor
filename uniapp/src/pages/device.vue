<!-- src/pages/device.vue -->
<script setup lang="ts">
import type { DeviceInfo } from '@/utils/device'
import type { VideoDevice } from '@/utils/video'
import PageHeader from '@/components/PageHeader.vue'
import { computed, nextTick, onMounted, ref } from 'vue'
import { deviceApi } from '@/utils/device'
import { videoApi } from '@/utils/video'

const TIME_SUFFIX_RE = /:\d{2}$/

const activeTab = ref<'monitor' | 'video'>('monitor')
const keyword = ref('')
const isRefreshing = ref(false)
const devices = ref<DeviceInfo[]>([])
const videoDevices = ref<VideoDevice[]>([])

// 分页
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loadingMore = ref(false)
const hasMore = computed(() => devices.value.length < total.value)

// 回到顶部
const showBackTop = ref(false)
const scrollTop = ref(0)

const deviceTotal = computed(() => total.value)
const videoDeviceTotal = computed(() => videoDevices.value.length)

const filteredDevices = computed(() => {
  if (!keyword.value.trim()) return devices.value
  const kw = keyword.value.trim().toLowerCase()
  return devices.value.filter(d =>
    d.deviceName.toLowerCase().includes(kw)
    || d.deviceCode.toLowerCase().includes(kw),
  )
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
    pageNum.value = 1
    const res = await deviceApi.getPage(pageNum.value, pageSize.value)
    devices.value = res.rows
    total.value = res.total
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

async function loadMoreDevices() {
  if (loadingMore.value || !hasMore.value) return
  loadingMore.value = true
  try {
    pageNum.value++
    const res = await deviceApi.getPage(pageNum.value, pageSize.value)
    devices.value.push(...res.rows)
    total.value = res.total
  }
  catch (error) {
    console.error('加载更多设备失败:', error)
    pageNum.value--
  }
  finally {
    loadingMore.value = false
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

function switchTab(tab: 'monitor' | 'video') {
  activeTab.value = tab
  keyword.value = ''
}

function goToDetail(device: DeviceInfo) {
  uni.navigateTo({ url: `/pages/device-detail?id=${device.id}` })
}

function goToVideoPlayer(video: VideoDevice) {
  uni.navigateTo({ url: `/pages/video-player?id=${video.id}` })
}

function getStatusClass(status: string): string {
  switch (status) {
    case '正常': return 'online'
    case '维修': return 'fault'
    case '停用': return 'offline'
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

function onScrollToLower() {
  if (activeTab.value === 'monitor') {
    loadMoreDevices()
  }
}

function onScroll(e: { detail: { scrollTop: number } }) {
  showBackTop.value = e.detail.scrollTop > 400
}

function scrollToTop() {
  scrollTop.value = 1
  nextTick(() => { scrollTop.value = 0 })
  showBackTop.value = false
}
</script>

<template>
  <view class="page-container">
    <!-- 渐变头部 -->
    <PageHeader title="设备库" subtitle="设备运维 · 状态监控" />

    <!-- 设备列表 -->
    <view class="page-body">
      <!-- Tab 胶囊切换 -->
      <view class="tab-capsule">
        <view
          class="tab-capsule-item"
          :class="{ active: activeTab === 'monitor' }"
          @click="switchTab('monitor')"
        >
          <text class="tab-capsule-text">监测设备({{ deviceTotal }})</text>
        </view>
        <view
          class="tab-capsule-item"
          :class="{ active: activeTab === 'video' }"
          @click="switchTab('video')"
        >
          <text class="tab-capsule-text">视频设备({{ videoDeviceTotal }})</text>
        </view>
      </view>

      <!-- 搜索框 -->
      <view class="search-bar-wrapper">
        <view class="search-bar">
          <view class="search-icon-wrapper">
            <zui-svg-icon icon="search" :width="16" color="#9ca3af" />
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

      <!-- 列表滚动区 -->
      <scroll-view
        class="list-scroll"
        scroll-y
        :scroll-top="scrollTop"
        refresher-enabled
        :refresher-triggered="isRefreshing"
        @refresherrefresh="onRefresh"
        @scrolltolower="onScrollToLower"
        @scroll="onScroll"
      >
        <view class="list-inner">
          <!-- 监测设备列表 -->
          <template v-if="activeTab === 'monitor'">
            <DataCard
              v-for="item in filteredDevices"
              :key="item.id"
              :title="item.deviceName"
              :subtitle="item.deviceCode"
              @click="goToDetail(item)"
            >
              <template #tag>
                <view class="status-badge" :class="getVideoStatusClass(item.onlineStatus)">
                  <view class="status-dot" :class="getVideoStatusClass(item.onlineStatus)" />
                  <text class="status-text">{{ getVideoStatusText(item.onlineStatus) }}</text>
                </view>
              </template>

              <!-- body: 信息行 -->
              <view class="info-line">
                <text class="info-label">关联隐患点</text>
                <text class="info-val">{{ item.boundHazardPointName || '-' }}</text>
              </view>
              <view class="info-line">
                <text class="info-label">安装位置</text>
                <text class="info-val">{{ item.installLocation || '-' }}</text>
              </view>

              <!-- footer -->
              <template #footer>
                <text class="footer-left">📡 {{ item.sensorCount ?? 0 }}个传感器</text>
                <text class="footer-time">最近上报: {{ formatTime(item.lastReportTime) }}</text>
              </template>
            </DataCard>

            <EmptyState
              v-if="filteredDevices.length === 0"
              :use-image="true"
              title="暂无设备"
              :description="keyword ? '未找到匹配的设备' : '暂无设备数据'"
            />
          </template>

          <!-- 视频设备列表 -->
          <template v-else>
            <DataCard
              v-for="item in filteredVideoDevices"
              :key="item.id"
              :title="item.deviceName"
              :subtitle="item.deviceCode"
              @click="goToVideoPlayer(item)"
            >
              <template #tag>
                <view class="status-badge" :class="getVideoStatusClass(item.onlineStatus)">
                  <view class="status-dot" :class="getVideoStatusClass(item.onlineStatus)" />
                  <text class="status-text">{{ getVideoStatusText(item.onlineStatus) }}</text>
                </view>
              </template>

              <view class="info-line">
                <text class="info-label">SN</text>
                <text class="info-val">{{ item.deviceCode || '-' }}</text>
              </view>
              <view v-if="item.protocolName" class="info-line">
                <text class="info-label">协议</text>
                <text class="info-val">{{ item.protocolName }}</text>
              </view>
              <view v-if="item.longitude && item.latitude" class="info-line">
                <text class="info-label">坐标</text>
                <text class="info-val">{{ item.longitude.toFixed(6) }}, {{ item.latitude.toFixed(6) }}</text>
              </view>
              <view v-if="item.hazardPointIds" class="info-line">
                <text class="info-label">关联隐患点</text>
                <text class="info-val">ID: {{ item.hazardPointIds }}</text>
              </view>
            </DataCard>

            <EmptyState
              v-if="filteredVideoDevices.length === 0"
              :use-image="true"
              title="暂无视频设备"
              :description="keyword ? '未找到匹配的视频设备' : '暂无视频设备数据'"
            />
          </template>

          <!-- 加载更多提示 -->
          <view v-if="activeTab === 'monitor' && devices.length > 0" class="load-more-tip">
            <text v-if="loadingMore">加载中...</text>
            <text v-else-if="!hasMore">没有更多了</text>
          </view>

          <view class="bottom-spacer" />
        </view>
      </scroll-view>
    </view>

    <!-- 回到顶部 -->
    <view v-if="showBackTop" class="back-top-btn" @click="scrollToTop">
      <zui-svg-icon icon="up" :width="20" color="#ffffff" />
    </view>
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

/* 页面主体 */
.page-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

/* Tab 胶囊卡片 */
.tab-capsule {
  display: flex;
  gap: 20rpx;
  padding: 16rpx;
  margin: 0 32rpx;
  background: #ffffff;
  border-radius: 24rpx;
  box-shadow: 0 4rpx 16rpx rgba(102, 126, 234, 0.1);
  flex-shrink: 0;
}

.tab-capsule-item {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20rpx 0;
  background: #ffffff;
  border-radius: 24rpx;
  box-shadow: 0 4rpx 16rpx rgba(102, 126, 234, 0.4);
  border: 2rpx solid transparent;
  transition: all 0.2s;

  &.active {
    background: linear-gradient(135deg, #3068e4 0%, #1e5acc 100%);
    box-shadow: 0 8rpx 24rpx rgba(48, 104, 228, 0.3);
  }

  &.active .tab-capsule-text {
    color: #ffffff;
    font-weight: 600;
  }
}

.tab-capsule-text {
  font-size: 28rpx;
  color: #6b7280;
}

/* 搜索框 */
.search-bar-wrapper {
  flex-shrink: 0;
  padding: 16rpx 32rpx;
}

.search-bar {
  display: flex;
  align-items: center;
  height: 72rpx;
  background: #ffffff;
  border-radius: 36rpx;
  padding: 0 24rpx;
  box-shadow: 0 2rpx 12rpx rgba(102, 126, 234, 0.08);
}

.search-icon-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12rpx;
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

/* 列表滚动区 */
.list-scroll {
  flex: 1;
  height: 0;
}

.list-inner {
  padding: 4rpx 32rpx 0;
}

/* 卡片内信息行 */
.info-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6rpx 0;
}

.info-label {
  font-size: 24rpx;
  color: #9ca3af;
  flex-shrink: 0;
}

.info-val {
  font-size: 24rpx;
  color: #4b5563;
  flex: 1;
  text-align: right;
  margin-left: 16rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 状态徽章 */
.status-badge {
  display: flex;
  align-items: center;
  gap: 6rpx;
  padding: 6rpx 14rpx;
  border-radius: 8rpx;
  flex-shrink: 0;

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

/* Footer */
.footer-left {
  font-size: 24rpx;
  color: #4b5563;
}

.footer-time {
  font-size: 22rpx;
  color: #9ca3af;
  flex-shrink: 0;
}

/* 加载更多 */
.load-more-tip {
  text-align: center;
  padding: 24rpx 0;
  font-size: 24rpx;
  color: #9ca3af;
}

/* 空状态 */
.bottom-spacer { height: 40rpx; }

/* 回到顶部 */
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
