<!-- src/components/EmptyState.vue -->
<template>
  <view class="empty-state">
    <view class="empty-icon">
      <image v-if="useImage" src="/static/empty.png" class="empty-image" mode="aspectFit" />
      <zui-svg-icon v-else-if="isVNodeIcon" :icon="iconName" :width="iconSize + 'rpx'" />
      <text v-else>{{ icon }}</text>
    </view>
    <text class="empty-title">{{ title }}</text>
    <text class="empty-description" v-if="description">{{ description }}</text>
    <view class="empty-action" v-if="actionText" @click.stop="handleAction" @tap.stop="handleAction" hover-class="empty-action-hover">
      <text>{{ actionText }}</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, defineOptions } from 'vue'

// 显式设置组件名
defineOptions({
  name: 'EmptyState'
})

interface Props {
  icon?: string
  iconName?: string
  iconSize?: number
  title: string
  description?: string
  actionText?: string
  useImage?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  icon: '',
  iconName: 'empty',
  iconSize: 120,
  useImage: true
})

const emit = defineEmits<{
  action: []
}>()

// 判断使用图标还是emoji
const isVNodeIcon = computed(() => {
  return !!props.iconName && !props.useImage
})

const handleAction = () => {
  emit('action')
}
</script>

<style lang="scss" scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80rpx 40rpx;
}

.empty-icon {
  margin-bottom: 24rpx;
  opacity: 0.5;

  .empty-image {
    width: 200rpx;
    height: 200rpx;
  }

  :deep(.app-icon) {
    color: #9ca3af;
  }

  text {
    font-size: 120rpx;
  }
}

.empty-title {
  font-size: 28rpx;
  color: #1a1a2e;
  margin-bottom: 12rpx;
}

.empty-description {
  font-size: 24rpx;
  color: #6b7280;
  text-align: center;
  margin-bottom: 24rpx;
}

.empty-action {
  padding: 16rpx 48rpx;
  background: linear-gradient(135deg, #3068e4 0%, #1e5acc 100%);
  border-radius: 12rpx;
  cursor: pointer;

  text {
    font-size: 26rpx;
    color: #ffffff;
  }
}

.empty-action-hover {
  opacity: 0.8;
  transform: scale(0.98);
}
</style>
