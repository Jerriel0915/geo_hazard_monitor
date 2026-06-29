<!-- ======================================== -->
<!-- TimelineItem - 单条时间线条目 -->
<!-- ======================================== -->

<template>
  <div class="timeline-item" :class="`type-${item.type}`">
    <div class="item-icon">
      <component :is="iconComponent" />
    </div>
    <div class="item-content">
      <div class="item-header">
        <span class="item-time">{{ formatTime(item.timestamp) }}</span>
      </div>
      <div class="item-message">{{ item.message }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { TimelineItem as TimelineItemType } from '@/types'

/**
 * Props
 */
interface Props {
  item: TimelineItemType
}

const props = defineProps<Props>()

/**
 * 图标组件映射
 */
const iconComponent = computed(() => {
  const icons: Record<TimelineItemType['type'], string> = {
    thinking: '💭',
    observation: '👁️',
    warning: '⚠️',
    action: '✓'
  }
  return icons[props.item.type] || '📌'
})

/**
 * 格式化时间
 */
function formatTime(timestamp: number): string {
  const date = new Date(timestamp)
  const now = new Date()
  const diffMs = now.getTime() - date.getTime()
  const diffMins = Math.floor(diffMs / 60000)

  if (diffMins < 1) {
    return '刚刚'
  } else if (diffMins < 60) {
    return `${diffMins} 分钟前`
  } else if (diffMins < 1440) {
    const hours = Math.floor(diffMs / 60)
    return `${hours} 小时前`
  } else {
    const days = Math.floor(diffMs / 1440)
    return `${days} 天前`
  }
}
</script>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.timeline-item {
  display: flex;
  gap: 12px;
  padding: 10px 16px;
  margin: 0 8px 4px;
  background-color: rgba($bg-tertiary, 0.4);
  border-radius: $radius-sm;
  border: 1px solid $border-subtle;
  border-left: 4px solid transparent;
  transition: all $transition-fast $ease-out;

  &:hover {
    background-color: rgba($bg-tertiary, 0.6);
    border-color: $border-default;
  }

  // 类型样式
  &.type-thinking {
    border-left-color: $color-secondary;
    background-color: rgba($color-secondary, 0.08);
  }

  &.type-observation {
    border-left-color: $color-primary;
    background-color: rgba($color-primary, 0.08);
  }

  &.type-warning {
    border-left-color: $terra-warning;
    background-color: rgba($terra-warning, 0.1);
  }

  &.type-action {
    border-left-color: $terra-normal;
    background-color: rgba($terra-normal, 0.08);
  }

  .item-icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 24px;
    height: 24px;
    font-size: 16px;
    flex-shrink: 0;
  }

  .item-content {
    flex: 1;
    min-width: 0;

    .item-header {
      margin-bottom: 4px;

      .item-time {
        font-size: 11px;
        font-weight: 500;
        color: $text-dim;
      }
    }

    .item-message {
      font-size: 13px;
      font-weight: 400;
      color: $text-secondary;
      line-height: 1.4;
      word-break: break-all;
    }
  }
}
</style>
