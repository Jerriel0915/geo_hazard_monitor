<script setup lang="ts">
/**
 * 统一页面 Header 组件
 * 参考 hazard-detail header 布局，支持 slot 自定义内容
 *
 * @example 带返回按钮的详情页
 * <PageHeader show-back title="隐患点详情" />
 *
 * @example 标题 + 副标题的主页面
 * <PageHeader title="事件大厅" subtitle="边坡监测 · 智能预警" />
 *
 * @example 完全自定义内容
 * <PageHeader>
 *   <view class="user-card">...</view>
 * </PageHeader>
 */
import { useSafeArea } from '@/composables/useSafeArea'

withDefaults(defineProps<{
  /** 主标题 */
  title?: string
  /** 副标题（仅 showBack=false 时生效） */
  subtitle?: string
  /** 是否显示返回按钮 */
  showBack?: boolean
}>(), {
  title: '',
  subtitle: '',
  showBack: false,
})

const { statusBarHeight } = useSafeArea()

function handleBack() {
  uni.navigateBack()
}
</script>

<template>
  <view class="page-header">
    <!-- 渐变背景 -->
    <view class="page-header__bg">
      <view class="page-header__circle page-header__circle--1" />
      <view class="page-header__circle page-header__circle--2" />
    </view>

    <!-- 内容区域 -->
    <view class="page-header__content" :style="{ paddingTop: `${statusBarHeight}px` }">
      <!-- 返回按钮行：showBack=true 时始终渲染 -->
      <view v-if="showBack" class="page-header__nav">
        <view class="page-header__back" @click="handleBack">←</view>
        <text v-if="title" class="page-header__title">{{ title }}</text>
      </view>

      <!-- slot 内容：有外部传入时使用，否则按 props 渲染 -->
      <slot>
        <template v-if="!showBack">
          <text v-if="title" class="page-header__title--large">{{ title }}</text>
          <text v-if="subtitle" class="page-header__subtitle">{{ subtitle }}</text>
        </template>
      </slot>
    </view>
  </view>
</template>

<style lang="scss" scoped>
.page-header {
  position: relative;
  flex-shrink: 0;
}

.page-header__bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, #3068e4 0%, #1e5acc 100%);
  border-radius: 0 0 15rpx 15rpx;
  overflow: hidden;
}

.page-header__circle {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
}

.page-header__circle--1 {
  width: 300rpx;
  height: 300rpx;
  top: -80rpx;
  right: -60rpx;
}

.page-header__circle--2 {
  width: 200rpx;
  height: 200rpx;
  top: 80rpx;
  left: -50rpx;
}

.page-header__content {
  position: relative;
  z-index: 1;
  padding: 0 32rpx 72rpx;
}

/* 返回按钮行 */
.page-header__nav {
  display: flex;
  align-items: center;
  padding-top: 24rpx;
  margin-bottom: 24rpx;
}

.page-header__back {
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

/* 紧凑标题（与返回按钮同行） */
.page-header__title {
  font-size: 34rpx;
  font-weight: 600;
  color: #ffffff;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 大标题（无返回按钮时） */
.page-header__title--large {
  font-size: 40rpx;
  font-weight: bold;
  color: #ffffff;
  margin-bottom: 8rpx;
  display: block;
  padding-top: 40rpx;
}

.page-header__subtitle {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.8);
}
</style>
