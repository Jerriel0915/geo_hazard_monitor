<!-- src/pages/device-detail.vue -->
<template>
  <view class="page-container">
    <!-- 头部 -->
    <view class="header">
      <view class="header-bg" :style="{ height: `calc(${statusBarHeight}px + 155rpx)` }"></view>
      <view class="header-content" :style="{ marginTop: `${statusBarHeight}px` }">
        <view class="header-nav">
          <view class="back-btn" @click="goBack">
            <text class="back-arrow">←</text>
          </view>
          <text class="header-device-name">{{ device?.deviceName || '设备详情' }}</text>
          <view class="nav-placeholder"></view>
        </view>
      </view>
    </view>

    <!-- 可滚动内容 -->
    <scroll-view class="content-scroll" scroll-y>
      <!-- 状态卡片 -->
      <view class="section">
        <view class="status-card">
          <view class="status-main">
            <view class="status-big-badge" :class="getStatusClass(device?.status || '')">
              <view class="status-big-dot" :class="getStatusClass(device?.status || '')"></view>
              <text class="status-big-text">{{ device?.status || '-' }}</text>
            </view>
            <view class="status-info">
              <text class="status-label">连接状态</text>
              <text class="status-time">{{ device?.lastReportTime ? `最近上报: ${device.lastReportTime}` : '暂无上报记录' }}</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 配置信息卡片 -->
      <view class="section">
        <text class="section-title">设备信息</text>
        <view class="info-card">
          <view class="info-row">
            <text class="info-label">设备编号</text>
            <text class="info-value">{{ device?.deviceCode || '-' }}</text>
          </view>
          <view class="info-row">
            <text class="info-label">设备类型</text>
            <view class="type-tag" :style="{ background: getTypeColor(device?.deviceType || '') }">
              <text class="type-tag-text">{{ device?.deviceType || '-' }}</text>
            </view>
          </view>
          <view class="info-row">
            <text class="info-label">所属隐患点</text>
            <text class="info-value hazard-name">{{ device?.hazardName || '-' }}</text>
          </view>
          <view class="info-row">
            <text class="info-label">安装日期</text>
            <text class="info-value">{{ device?.installDate || '-' }}</text>
          </view>
        </view>
      </view>

      <!-- 监测参数 -->
      <view class="section">
        <text class="section-title">监测参数</text>
        <view v-if="device?.attributes && device.attributes.length > 0" class="attr-list">
          <view
            v-for="attr in device.attributes"
            :key="attr.property"
            class="attr-card"
          >
            <view class="attr-left">
              <text class="attr-name">{{ attr.displayName }}</text>
            </view>
            <view class="attr-right">
              <text class="attr-value">{{ attr.currentValue != null ? attr.currentValue : '--' }}</text>
              <text class="attr-unit">{{ attr.unit }}</text>
            </view>
          </view>
        </view>
        <view v-else class="empty-attrs">
          <text class="empty-attrs-text">暂无监测参数</text>
        </view>
      </view>

      <!-- 查看监测数据按钮 -->
      <view class="section">
        <view class="action-btn" @click="goToChart">
          <text class="action-btn-text">查看监测数据</text>
        </view>
      </view>

      <!-- 底部留白 -->
      <view class="bottom-spacer"></view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { deviceApi } from '@/utils/device'
import type { DeviceInfo } from '@/utils/device'
import { useSafeArea } from '@/composables/useSafeArea'

const { statusBarHeight } = useSafeArea()

const deviceId = ref(0)
const device = ref<DeviceInfo | undefined>(undefined)

onLoad((options) => {
  if (options?.id) {
    deviceId.value = Number(options.id)
    loadDevice()
  }
})

const loadDevice = () => {
  device.value = deviceApi.getById(deviceId.value)
  if (!device.value) {
    uni.showToast({ title: '设备不存在', icon: 'none' })
    setTimeout(() => uni.navigateBack(), 1500)
  }
}

const goBack = () => {
  uni.navigateBack()
}

const goToChart = () => {
  uni.navigateTo({ url: `/pages/chart?deviceId=${deviceId.value}` })
}

const getTypeColor = (type: string): string => {
  const colorMap: Record<string, string> = {
    'GNSS': '#3068e4',
    '雨量计': '#1890ff',
    '测斜仪': '#722ed1',
    '裂缝计': '#fa8c16',
    '水位计': '#13c2c2',
  }
  return colorMap[type] || '#3068e4'
}

const getStatusClass = (status: string): string => {
  switch (status) {
    case '在线': return 'online'
    case '离线': return 'offline'
    case '故障': return 'fault'
    default: return 'offline'
  }
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

  &::before {
    content: '';
    position: absolute;
    width: 300rpx;
    height: 280rpx;
    background: rgba(255, 255, 255, 0.1);
    border-radius: 50%;
    top: -80rpx;
    right: -60rpx;
  }
}

.header-content {
  position: relative;
  z-index: 1;
  padding: 0 32rpx 24rpx;
}

.header-nav {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.back-btn {
  width: 64rpx;
  height: 64rpx;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.back-arrow {
  font-size: 36rpx;
  color: #ffffff;
}

.header-device-name {
  font-size: 34rpx;
  font-weight: 600;
  color: #ffffff;
}

.nav-placeholder {
  width: 64rpx;
}

/* 内容滚动区域 */
.content-scroll {
  flex: 1;
  height: 0;
}

.section {
  margin: 0 32rpx 24rpx;
  box-sizing: border-box;
}

.section-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #1a1a2e;
  margin-bottom: 16rpx;
  display: block;
}

/* 状态卡片 */
.status-card {
  background: #ffffff;
  border-radius: 24rpx;
  padding: 32rpx 24rpx;
  box-shadow: 0 8rpx 32rpx rgba(102, 126, 234, 0.12);
}

.status-main {
  display: flex;
  align-items: center;
  gap: 24rpx;
}

.status-big-badge {
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 16rpx 28rpx;
  border-radius: 16rpx;

  &.online {
    background: rgba(82, 196, 26, 0.1);
  }

  &.offline {
    background: rgba(156, 163, 175, 0.15);
  }

  &.fault {
    background: rgba(245, 34, 45, 0.1);
  }
}

.status-big-dot {
  width: 20rpx;
  height: 20rpx;
  border-radius: 50%;

  &.online {
    background: #52c41a;
    box-shadow: 0 0 12rpx rgba(82, 196, 26, 0.5);
  }

  &.offline {
    background: #9ca3af;
  }

  &.fault {
    background: #f5222d;
    box-shadow: 0 0 12rpx rgba(245, 34, 45, 0.5);
  }
}

.status-big-text {
  font-size: 30rpx;
  font-weight: 700;

  .status-big-badge.online & {
    color: #52c41a;
  }

  .status-big-badge.offline & {
    color: #9ca3af;
  }

  .status-big-badge.fault & {
    color: #f5222d;
  }
}

.status-info {
  display: flex;
  flex-direction: column;
  gap: 6rpx;
}

.status-label {
  font-size: 24rpx;
  color: #9ca3af;
}

.status-time {
  font-size: 24rpx;
  color: #6b7280;
}

/* 信息卡片 */
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

  &.hazard-name {
    color: #3068e4;
  }
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

/* 监测参数列表 */
.attr-list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.attr-card {
  background: #ffffff;
  border-radius: 20rpx;
  padding: 24rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 8rpx 32rpx rgba(102, 126, 234, 0.12);
}

.attr-left {
  display: flex;
  align-items: center;
}

.attr-name {
  font-size: 28rpx;
  color: #4b5563;
  font-weight: 500;
}

.attr-right {
  display: flex;
  align-items: baseline;
  gap: 6rpx;
}

.attr-value {
  font-size: 36rpx;
  font-weight: 700;
  color: #1a1a2e;
}

.attr-unit {
  font-size: 22rpx;
  color: #9ca3af;
}

.empty-attrs {
  background: #ffffff;
  border-radius: 24rpx;
  padding: 60rpx 24rpx;
  text-align: center;
  box-shadow: 0 8rpx 32rpx rgba(102, 126, 234, 0.12);
}

.empty-attrs-text {
  font-size: 26rpx;
  color: #9ca3af;
}

/* 操作按钮 */
.action-btn {
  padding: 28rpx;
  background: linear-gradient(135deg, #3068e4 0%, #1e5acc 100%);
  border-radius: 24rpx;
  text-align: center;
  box-shadow: 0 8rpx 32rpx rgba(102, 126, 234, 0.3);
  box-sizing: border-box;

  &:active {
    opacity: 0.9;
  }
}

.action-btn-text {
  font-size: 30rpx;
  font-weight: 600;
  color: #ffffff;
}

.bottom-spacer {
  height: 32rpx;
}
</style>
