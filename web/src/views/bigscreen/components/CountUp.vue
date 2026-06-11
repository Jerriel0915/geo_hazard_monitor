<template>
  <span class="count-up">{{ displayValue }}</span>
</template>

<script setup lang="ts">
import { ref, watch, onUnmounted } from 'vue'

const props = withDefaults(defineProps<{
  end: number
  duration?: number
  decimals?: number
}>(), {
  duration: 2,
  decimals: 0
})

const displayValue = ref('0')
let rafId: number | null = null

function animateTo(target: number) {
  if (rafId !== null) cancelAnimationFrame(rafId)

  const start = parseFloat(displayValue.value) || 0
  const diff = target - start
  if (diff === 0) {
    displayValue.value = target.toFixed(props.decimals)
    return
  }

  const startTime = performance.now()
  const durationMs = props.duration * 1000

  function step(now: number) {
    const elapsed = now - startTime
    const progress = Math.min(elapsed / durationMs, 1)
    // easeOutCubic
    const eased = 1 - Math.pow(1 - progress, 3)
    const current = start + diff * eased
    displayValue.value = current.toFixed(props.decimals)

    if (progress < 1) {
      rafId = requestAnimationFrame(step)
    } else {
      displayValue.value = target.toFixed(props.decimals)
      rafId = null
    }
  }

  rafId = requestAnimationFrame(step)
}

watch(() => props.end, (newVal) => {
  animateTo(newVal)
}, { immediate: true })

onUnmounted(() => {
  if (rafId !== null) cancelAnimationFrame(rafId)
})
</script>
