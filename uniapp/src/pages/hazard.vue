<!-- src/pages/hazard.vue -->
<script setup lang="ts">
import type { Hazard } from '@/utils/hazard'
import { onMounted, ref } from 'vue'
import { useSafeArea } from '@/composables/useSafeArea'
import { hazardApi } from '@/utils/hazard'

const { statusBarHeight } = useSafeArea()

const loading = ref(true)
const isRefreshing = ref(false)
const hazards = ref<Hazard[]>([])

async function loadData() {
  try {
    hazards.value = await hazardApi.getAll()
  }
  catch (error) {
    console.error('加载隐患点数据失败:', error)
  }
  finally {
    loading.value = false
    isRefreshing.value = false
  }
}

function onRefresh() {
  isRefreshing.value = true
  loadData()
}

function goToDetail(hazard: Hazard) {
  uni.navigateTo({ url: `/pages/hazard-detail?id=${hazard.id}` })
}

onMounted(() => {
  loadData()
})
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
          <text class="header-title">隐患点</text>
          <text class="header-subtitle">风险管控 · 安全监测</text>
        </view>
      </view>
    </view>

    <!-- 隐患点列表 -->
    <scroll-view
      class="hazard-list"
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
        </view>

        <!-- 列表内容 -->
        <view v-else>
          <view
            v-for="item in hazards"
            :key="item.id"
            class="hazard-card"
            @click="goToDetail(item)"
          >
            <view class="card-header">
              <text class="hazard-name">{{ item.name }}</text>
              <view class="status-tag" :class="item.status === '监测中' ? 'monitoring' : 'resolved'">
                {{ item.status }}
              </view>
            </view>
            <view class="card-body">
              <view class="card-info-row">
                <text class="card-info-icon">📍</text>
                <text class="card-info-text">{{ item.location }}</text>
              </view>
              <view class="card-info-row">
                <text class="card-info-icon">📡</text>
                <text class="card-info-text">设备数量：{{ item.deviceCount }}台</text>
              </view>
            </view>
            <view class="card-footer">
              <text v-if="item.description" class="card-desc">{{ item.description }}</text>
              <text class="card-time">{{ item.createTime }}</text>
            </view>
          </view>

          <EmptyState
            v-if="hazards.length === 0"
            :use-image="true"
            title="暂无隐患点"
            description="当前没有任何隐患点数据"
          />
        </view>
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
.bg-circle-2 { width: 200rpx; height: 200rpx; top: 80rpx; left: -50rpx; }

.header-content {
  position: relative;
  z-index: 1;
  padding: 40rpx 32rpx 24rpx;
}

.header-top { margin-bottom: 0; }

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

/* 列表 */
.hazard-list {
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

.hazard-card {
  background: #ffffff;
  border-radius: 24rpx;
  padding: 28rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 8rpx 32rpx rgba(102, 126, 234, 0.12);
  box-sizing: border-box;
  width: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;
}

.hazard-name {
  font-size: 32rpx;
  font-weight: 600;
  color: #1a1a2e;
  flex: 1;
  margin-right: 16rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status-tag {
  padding: 6rpx 16rpx;
  border-radius: 8rpx;
  font-size: 22rpx;
  flex-shrink: 0;

  &.monitoring {
    background: rgba(82, 196, 26, 0.1);
    color: #52c41a;
  }

  &.resolved {
    background: rgba(156, 163, 175, 0.1);
    color: #9ca3af;
  }
}

.card-body {
  margin-bottom: 20rpx;
}

.card-info-row {
  display: flex;
  align-items: center;
  margin-bottom: 12rpx;

  &:last-child { margin-bottom: 0; }
}

.card-info-icon {
  font-size: 24rpx;
  margin-right: 8rpx;
}

.card-info-text {
  font-size: 26rpx;
  color: #6b7280;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 20rpx;
  border-top: 1rpx solid #f5f5f5;
}

.card-desc {
  font-size: 24rpx;
  color: #9ca3af;
  flex: 1;
  margin-right: 16rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-time {
  font-size: 22rpx;
  color: #9ca3af;
  flex-shrink: 0;
}
</style>
