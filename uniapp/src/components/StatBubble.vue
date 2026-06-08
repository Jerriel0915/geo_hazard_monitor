<!-- src/components/StatBubble.vue -->
<template>
  <view class="stat-bubble" :style="bubbleStyle">
    <view class="stat-value">
      <text class="stat-number" :class="{ 'animate': animate }">{{ displayValue }}</text>
    </view>
    <text class="stat-label">{{ label }}</text>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'

interface Props {
  value: number
  label: string
  gradient?: string
  animate?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  gradient: 'linear-gradient(135deg, #3068e4 0%, #1e5acc 100%)',
  animate: true
})

const displayValue = ref(0)

const bubbleStyle = computed(() => ({
  background: props.gradient
}))

// 数字滚动动画
watch(() => props.value, (newVal, oldVal) => {
  if (!props.animate) {
    displayValue.value = newVal
    return
  }

  const startVal = oldVal || 0
  const duration = 500
  const startTime = Date.now()

  const animateFrame = () => {
    const elapsed = Date.now() - startTime
    const progress = Math.min(elapsed / duration, 1)

    // easeOutQuad
    const easeProgress = 1 - (1 - progress) * (1 - progress)
    displayValue.value = Math.round(startVal + (newVal - startVal) * easeProgress)

    if (progress < 1) {
      requestAnimationFrame(animateFrame)
    }
  }

  animateFrame()
}, { immediate: true })
</script>

<style lang="scss" scoped>
.stat-bubble {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 24rpx 32rpx;
  border-radius: 16rpx;
  min-width: 140rpx;
}

.stat-value {
  margin-bottom: 8rpx;
}

.stat-number {
  font-size: 44rpx;
  font-weight: bold;
  color: #ffffff;

  &.animate {
    transition: all 0.3s ease;
  }
}

.stat-label {
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.85);
}
</style>
