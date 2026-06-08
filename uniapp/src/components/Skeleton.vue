<!-- src/components/Skeleton.vue -->
<template>
  <view class="skeleton" :class="{ animate: animate }">
    <view
      v-for="(item, index) in rows"
      :key="index"
      class="skeleton-item"
      :style="getItemStyle(item)"
    ></view>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface SkeletonRow {
  width?: string
  height?: string
  borderRadius?: string
  margin?: string
}

interface Props {
  rows?: number | SkeletonRow[]
  animate?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  rows: 3,
  animate: true
})

const defaultRow: SkeletonRow = {
  width: '100%',
  height: '32rpx',
  borderRadius: '8rpx',
  margin: '0 0 20rpx 0'
}

const rows = computed(() => {
  if (typeof props.rows === 'number') {
    return Array(props.rows).fill(defaultRow)
  }
  return props.rows
})

const getItemStyle = (item: SkeletonRow) => ({
  width: item.width || defaultRow.width,
  height: item.height || defaultRow.height,
  borderRadius: item.borderRadius || defaultRow.borderRadius,
  margin: item.margin || defaultRow.margin
})
</script>

<style lang="scss" scoped>
.skeleton {
  &.animate .skeleton-item {
    animation: skeleton-loading 1.4s ease infinite;
  }
}

.skeleton-item {
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
}

@keyframes skeleton-loading {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}
</style>
