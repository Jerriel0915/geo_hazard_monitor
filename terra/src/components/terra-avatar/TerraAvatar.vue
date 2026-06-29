<!-- ======================================== -->
<!-- TerraAvatar - TerraMens 3D 虚拟形象组件 -->
<!-- ======================================== -->

<template>
  <div class="terra-avatar" :class="[`state-${state}`, { breathing: breathing }]">
    <div ref="container" class="sphere-container"></div>
    <div class="avatar-label">{{ stateLabel }}</div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { QuantumSphere } from './QuantumSphere'
import type { SphereState } from './QuantumSphere'
import { TerraStateLabels } from '@/types'

/**
 * Props
 */
interface Props {
  state?: SphereState
  breathing?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  state: 'normal',
  breathing: true
})

/**
 * 状态标签
 */
const stateLabel = computed(() => TerraStateLabels[props.state])

/**
 * 容器引用
 */
const container = ref<HTMLElement>()

/**
 * 球体实例
 */
let sphere: QuantumSphere | null = null

/**
 * 组件挂载
 */
onMounted(async () => {
  if (container.value) {
    sphere = new QuantumSphere(container.value, {
      size: 280,
      state: props.state,
      breathing: props.breathing
    })
    // 等待 DOM 渲染后调整尺寸
    await nextTick()
    sphere?.resize()
  }
})

/**
 * 组件卸载
 */
onUnmounted(() => {
  sphere?.dispose()
  sphere = null
})

/**
 * 监听状态变化
 */
watch(() => props.state, (newState) => {
  sphere?.setState(newState)
})

/**
 * 监听呼吸开关
 */
watch(() => props.breathing, async () => {
  // 重新创建球体以应用呼吸设置
  if (sphere) {
    sphere.dispose()
  }
  if (container.value) {
    sphere = new QuantumSphere(container.value, {
      size: 280,
      state: props.state,
      breathing: props.breathing
    })
    await nextTick()
    sphere?.resize()
  }
})

/**
 * 心跳动画 - 收到 HEARTBEAT_OK 消息时触发
 */
function heartbeat() {
  sphere?.heartbeat()
}

// 暴露方法供外部调用
defineExpose({
  heartbeat
})
</script>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.terra-avatar {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  padding: 0;

  .sphere-container {
    width: 100%;
    height: 100%;
  }

  .avatar-label {
    position: absolute;
    bottom: 8px;
    left: 0;
    right: 0;
    text-align: center;
    font-size: 12px;
    font-weight: 500;
    color: $text-secondary;
    transition: color $transition-normal $ease-out;
  }

  // 状态样式
  &.state-normal .avatar-label {
    color: $terra-normal;
  }

  &.state-info .avatar-label {
    color: $terra-info;
  }

  &.state-caution .avatar-label {
    color: $terra-caution;
  }

  &.state-warning .avatar-label {
    color: $terra-warning;
  }

  &.state-critical .avatar-label {
    color: $terra-critical;
    animation: textPulse 1s ease-in-out infinite;
  }
}

@keyframes textPulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.6;
  }
}
</style>
