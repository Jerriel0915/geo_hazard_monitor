<!-- src/pages/video-player.vue -->
<script setup lang="ts">
import type { VideoDevice } from '@/utils/video'
import PageHeader from '@/components/PageHeader.vue'
import { onLoad } from '@dcloudio/uni-app'
import { ref } from 'vue'
import { videoApi } from '@/utils/video'

const videoDeviceId = ref(0)
const videoDevice = ref<VideoDevice | undefined>(undefined)
const loading = ref(true)
const error = ref(false)
const videoContext = ref<UniApp.VideoContext | null>(null)
const isFullscreen = ref(false)

onLoad(async (options) => {
  if (options?.id) {
    videoDeviceId.value = Number(options.id)
    await loadVideoDevice()
  }
})

async function loadVideoDevice() {
  loading.value = true
  error.value = false
  try {
    videoDevice.value = await videoApi.getById(videoDeviceId.value)
    if (!videoDevice.value) {
      error.value = true
      uni.showToast({ title: '视频设备不存在', icon: 'none' })
      return
    }
    if (!videoDevice.value.streamUrl) {
      error.value = true
      return
    }
    // 等 DOM 渲染后获取 video context
    setTimeout(() => {
      videoContext.value = uni.createVideoContext('liveVideo')
    }, 200)
  }
  catch (e) {
    console.error('加载视频设备失败:', e)
    error.value = true
  }
  finally {
    loading.value = false
  }
}

function toggleFullscreen() {
  if (!videoContext.value) return
  if (isFullscreen.value) {
    videoContext.value.exitFullScreen()
  } else {
    videoContext.value.enterFullScreen()
  }
}

function onFullscreenChange(e: any) {
  isFullscreen.value = e.detail.fullScreen
}

function onVideoError(e: any) {
  console.error('视频播放错误:', e)
  error.value = true
}

function retry() {
  loadVideoDevice()
}
</script>

<template>
  <view class="page-container">
    <!-- 头部 -->
    <PageHeader show-back :title="videoDevice?.deviceName || '视频监控'" />

    <scroll-view class="page-body" scroll-y>
      <!-- 视频播放区域 -->
      <view class="video-section">
      <view v-if="loading" class="video-loading">
        <text>加载中...</text>
      </view>

      <view v-else-if="error || !videoDevice?.streamUrl" class="video-error">
        <text class="error-icon">&#x1F6A7;</text>
        <text class="error-title">{{ videoDevice && !videoDevice.streamUrl ? '该设备未配置视频流地址' : '视频加载失败' }}</text>
        <view class="retry-btn" @click="retry">
          <text class="retry-text">重试</text>
        </view>
      </view>

      <view v-else class="video-wrapper">
        <video
          id="liveVideo"
          class="video-player"
          :src="videoDevice.streamUrl"
          :autoplay="true"
          :controls="true"
          :show-fullscreen-btn="true"
          :show-play-btn="true"
          :enable-progress-gesture="false"
          object-fit="cover"
          @fullscreenchange="onFullscreenChange"
          @error="onVideoError"
        />
      </view>
    </view>

    <!-- 设备信息卡片 -->
    <view v-if="videoDevice" class="info-section">
      <view class="info-card">
        <view class="info-row">
          <text class="info-label">设备编号</text>
          <text class="info-value">{{ videoDevice.deviceCode || '-' }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">设备类型</text>
          <text class="info-value">{{ videoDevice.deviceType || '-' }}</text>
        </view>
        <view v-if="videoDevice.manufacturer" class="info-row">
          <text class="info-label">厂商</text>
          <text class="info-value">{{ videoDevice.manufacturer }}</text>
        </view>
        <view v-if="videoDevice.location" class="info-row">
          <text class="info-label">安装位置</text>
          <text class="info-value">{{ videoDevice.location }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">状态</text>
          <view class="status-badge" :class="videoDevice.onlineStatus === 1 ? 'online' : 'offline'">
            <view class="status-dot" :class="videoDevice.onlineStatus === 1 ? 'online' : 'offline'" />
            <text class="status-text" :class="videoDevice.onlineStatus === 1 ? 'online' : 'offline'">{{ videoDevice.onlineStatus === 1 ? '在线' : '离线' }}</text>
          </view>
        </view>
      </view>
    </view>

      <!-- 底部留白 -->
      <view class="bottom-spacer" />
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

.page-body {
  flex: 1;
  height: 0;
}

/* 视频播放区域 */
.video-section {
  margin: 0 32rpx 24rpx;
}

.video-wrapper {
  background: #000;
  border-radius: 24rpx;
  overflow: hidden;
  box-shadow: 0 8rpx 32rpx rgba(0, 0, 0, 0.2);
}

.video-player {
  width: 100%;
  height: 420rpx;
}

.video-loading {
  background: #000;
  border-radius: 24rpx;
  height: 420rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #9ca3af;
  font-size: 28rpx;
}

.video-error {
  background: #f7f8fc;
  border-radius: 24rpx;
  height: 420rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16rpx;
}

.error-icon {
  font-size: 64rpx;
}

.error-title {
  font-size: 26rpx;
  color: #6b7280;
}

.retry-btn {
  padding: 16rpx 40rpx;
  background: linear-gradient(135deg, #3068e4 0%, #1e5acc 100%);
  border-radius: 32rpx;

  &:active { opacity: 0.9; }
}

.retry-text {
  font-size: 26rpx;
  color: #ffffff;
}

/* 设备信息 */
.info-section {
  margin: 0 32rpx;
}

.info-card {
  background: #ffffff;
  border-radius: 24rpx;
  padding: 24rpx;
  box-shadow: 0 8rpx 32rpx rgba(102, 126, 234, 0.12);
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.info-label {
  font-size: 26rpx;
  color: #6b7280;
}

.info-value {
  font-size: 26rpx;
  color: #1a1a2e;
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
}

.status-dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;

  &.online { background: #52c41a; }
  &.offline { background: #9ca3af; }
}

.status-text {
  font-size: 22rpx;

  &.online { color: #52c41a; }
  &.offline { color: #9ca3af; }
}

.bottom-spacer {
  height: 32rpx;
}
</style>
