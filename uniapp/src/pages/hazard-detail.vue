<!-- src/pages/hazard-detail.vue -->
<template>
  <view class="page-container">
    <!-- 头部 -->
    <view class="header">
      <view class="header-bg" :style="{ height: `calc(${statusBarHeight}px + 155rpx)` }"></view>
      <view class="header-content" :style="{ marginTop: `${statusBarHeight}px` }">
        <view class="header-nav">
          <view class="back-btn" @click="goBack">←</view>
          <text class="header-title">{{ hazard.name }}</text>
        </view>
      </view>
    </view>

    <!-- 内容 -->
    <scroll-view class="content-scroll" scroll-y>
      <!-- 基本信息 -->
      <view class="section">
        <view class="info-card">
          <view class="info-row">
            <text class="info-label">位置</text>
            <text class="info-value">{{ hazard.location || '-' }}</text>
          </view>
          <view class="info-row">
            <text class="info-label">风险等级</text>
            <view class="level-tag" :style="{ background: getLevelColor(hazard.level) }">
              {{ hazard.level || '-' }}
            </view>
          </view>
          <view class="info-row">
            <text class="info-label">状态</text>
            <view class="status-badge" :class="hazard.status === '监测中' ? 'monitoring' : 'resolved'">
              {{ hazard.status || '-' }}
            </view>
          </view>
          <view class="info-row">
            <text class="info-label">设备数量</text>
            <text class="info-value">{{ hazard.deviceCount || 0 }}台</text>
          </view>
          <view class="info-row">
            <text class="info-label">描述</text>
            <text class="info-value desc-text">{{ hazard.description || '-' }}</text>
          </view>
          <view class="info-row">
            <text class="info-label">创建时间</text>
            <text class="info-value">{{ hazard.createTime || '-' }}</text>
          </view>
        </view>
      </view>

      <!-- 关联设备 -->
      <view class="section">
        <text class="section-title">关联设备</text>
        <view class="device-list">
          <view
            v-for="device in devices"
            :key="device.id"
            class="device-item"
            @click="goToDeviceDetail(device.id)"
          >
            <view class="device-left">
              <view class="device-main">
                <text class="device-name">{{ device.deviceName }}</text>
                <view class="device-type-tag">{{ device.deviceType }}</view>
              </view>
              <text class="device-time">最近上报：{{ device.lastReportTime }}</text>
            </view>
            <view class="device-status">
              <view
                class="status-dot"
                :class="device.status === '在线' ? 'online' : device.status === '故障' ? 'fault' : 'offline'"
              ></view>
              <text class="status-text">{{ device.status }}</text>
            </view>
          </view>

          <view v-if="devices.length === 0" class="empty-logs">
            <text class="empty-text">暂无关联设备</text>
          </view>
        </view>
      </view>

      <view class="bottom-spacer"></view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useSafeArea } from '@/composables/useSafeArea'
import { hazardApi } from '@/utils/hazard'
import { deviceApi } from '@/utils/device'
import type { Hazard } from '@/utils/hazard'
import type { DeviceInfo } from '@/utils/device'

const { statusBarHeight } = useSafeArea()

const hazardId = ref<number>(0)
const hazard = ref<Partial<Hazard>>({})
const devices = ref<DeviceInfo[]>([])

onLoad((options) => {
  if (options?.id) {
    hazardId.value = Number(options.id)
    loadData()
  }
})

const loadData = () => {
  const detail = hazardApi.getById(hazardId.value)
  if (detail) {
    hazard.value = detail
    devices.value = deviceApi.getByHazardId(hazardId.value)
  } else {
    uni.showToast({ title: '隐患点不存在', icon: 'none' })
    setTimeout(() => uni.navigateBack(), 1500)
  }
}

const getLevelColor = (level: string | undefined) => {
  const map: Record<string, string> = {
    '高风险': '#f5222d',
    '中风险': '#faad14',
    '低风险': '#52c41a'
  }
  return map[level || ''] || '#1890ff'
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
  margin-right: 16rpx;
}

.header-title {
  font-size: 34rpx;
  font-weight: 600;
  color: #ffffff;
  flex: 1;
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

  &:last-child {
    border-bottom: none;
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

  &.desc-text {
    max-width: 420rpx;
    line-height: 1.5;
    font-weight: 400;
  }
}

.level-tag {
  padding: 6rpx 20rpx;
  border-radius: 8rpx;
  font-size: 22rpx;
  color: #ffffff;
  font-weight: 500;
}

.status-badge {
  padding: 6rpx 16rpx;
  border-radius: 8rpx;
  font-size: 22rpx;

  &.monitoring {
    background: rgba(82, 196, 26, 0.1);
    color: #52c41a;
  }

  &.resolved {
    background: rgba(156, 163, 175, 0.1);
    color: #9ca3af;
  }
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

  &:last-child {
    border-bottom: none;
  }
}

.device-left {
  flex: 1;
  margin-right: 16rpx;
}

.device-main {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 8rpx;
}

.device-name {
  font-size: 28rpx;
  color: #1a1a2e;
  font-weight: 500;
}

.device-type-tag {
  font-size: 22rpx;
  color: #6b7280;
  background: #f5f5f5;
  padding: 4rpx 12rpx;
  border-radius: 6rpx;
}

.device-time {
  font-size: 22rpx;
  color: #9ca3af;
}

.device-status {
  display: flex;
  align-items: center;
  gap: 8rpx;
  flex-shrink: 0;
}

.status-dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;

  &.online {
    background: #52c41a;
  }

  &.offline {
    background: #d9d9d9;
  }

  &.fault {
    background: #f5222d;
  }
}

.status-text {
  font-size: 24rpx;
  color: #6b7280;
}

.empty-logs {
  padding: 40rpx 0;
  text-align: center;
}

.empty-text {
  font-size: 24rpx;
  color: #9ca3af;
}

.bottom-spacer {
  height: 32rpx;
}
</style>
