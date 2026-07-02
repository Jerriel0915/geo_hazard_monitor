<!-- src/components/DataCard.vue — 通用数据卡片 (header + body + footer) -->
<script setup lang="ts">
defineProps<{
  /** 卡片标题 */
  title: string
  /** 标题下方副标题（可选） */
  subtitle?: string
}>()
</script>

<template>
  <view class="data-card" @click="$emit('click')">
    <view class="card-header">
      <view class="card-title-group">
        <text class="card-title">{{ title }}</text>
        <text v-if="subtitle" class="card-subtitle">{{ subtitle }}</text>
      </view>
      <slot name="tag" />
    </view>
    <view class="card-body">
      <slot />
    </view>
    <view v-if="$slots.footer" class="card-footer">
      <slot name="footer" />
    </view>
  </view>
</template>

<style lang="scss" scoped>
.data-card {
  background: #ffffff;
  border-radius: 24rpx;
  padding: 28rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 8rpx 32rpx rgba(102, 126, 234, 0.12);
  box-sizing: border-box;
  width: 100%;

  &:active {
    background: #f7f8fc;
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16rpx;
}

.card-title-group {
  display: flex;
  flex-direction: column;
  gap: 4rpx;
  flex: 1;
  margin-right: 16rpx;
  min-width: 0;
}

.card-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #1a1a2e;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-subtitle {
  font-size: 22rpx;
  color: #9ca3af;
}

.card-body {
  margin-bottom: 16rpx;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 16rpx;
  border-top: 1rpx solid #f5f5f5;
}
</style>
