<template>
  <i :class="iconClass" :style="iconStyle"></i>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  name: string
  size?: number | string
  color?: string
  className?: string
}>(), {
  size: 16,
  color: '',
  className: ''
})

const iconClass = computed(() => {
  const classes = ['iconfont', `icon-${props.name}`]
  if (props.className) {
    classes.push(props.className)
  }
  return classes.join(' ')
})

const iconStyle = computed(() => {
  const style: Record<string, string> = {}
  if (props.size) {
    style.fontSize = typeof props.size === 'number' ? `${props.size}px` : props.size
  }
  if (props.color) {
    style.color = props.color
  }
  return style
})
</script>