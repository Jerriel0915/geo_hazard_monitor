<!-- ======================================== -->
<!-- PanelContainer - 动态面板容器 -->
<!-- ======================================== -->

<template>
  <div class="panel-container">
    <TransitionGroup name="panel">
      <component
        v-for="panel in visiblePanels"
        :key="panel.id"
        :is="getPanelComponent(panel.type)"
        :id="panel.id"
        :config="panel"
        @focus="handlePanelFocus"
        @close="handlePanelClose"
      />
    </TransitionGroup>

    <div v-if="visiblePanels.length === 0" class="empty-state">
      <p class="empty-text">暂无活跃面板</p>
      <p class="empty-hint">TerraMens 关注的内容会在此显示</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, defineAsyncComponent } from 'vue'
import type { PanelConfig } from '@/types'

/**
 * Props
 */
interface Props {
  panels: PanelConfig[]
}

const props = defineProps<Props>()

/**
 * Emits
 */
interface Emits {
  (e: 'focus', panelId: string): void
  (e: 'close', panelId: string): void
}

const emit = defineEmits<Emits>()

/**
 * 面板组件映射（在模块级别定义，避免重复创建）
 */
const panelComponents = {
  map: defineAsyncComponent(() => import('./MapPanel.vue')),
  video: defineAsyncComponent(() => import('./VideoPanel.vue')),
  image: defineAsyncComponent(() => import('./ImagePanel.vue')),
  table: defineAsyncComponent(() => import('./TablePanel.vue')),
  chart: defineAsyncComponent(() => import('./ChartPanel.vue')),
  iframe: defineAsyncComponent(() => import('./IframePanel.vue'))
} as const

/**
 * 可见面板列表（按 zIndex 排序）
 */
const visiblePanels = computed(() => {
  return props.panels
    .filter(p => p.visible)
    .sort((a, b) => a.zIndex - b.zIndex)
})

/**
 * 获取面板组件
 */
function getPanelComponent(type: string) {
  return panelComponents[type as keyof typeof panelComponents] || null
}

/**
 * 处理面板聚焦
 */
function handlePanelFocus(panelId: string) {
  emit('focus', panelId)
}

/**
 * 处理面板关闭
 */
function handlePanelClose(panelId: string) {
  emit('close', panelId)
}
</script>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.panel-container {
  width: 100%;
  height: 100%;
  padding: 16px;
  display: grid;
  grid-template-columns: repeat(12, 1fr);
  grid-template-rows: repeat(12, 60px);
  gap: 16px;
  overflow-y: auto;
  position: relative;

  // 网格背景效果
  background-image:
    linear-gradient(to right, rgba(0, 212, 255, 0.03) 1px, transparent 1px),
    linear-gradient(to bottom, rgba(0, 212, 255, 0.03) 1px, transparent 1px);
  background-size: 40px 40px;
  background-position: -1px -1px;

  // 自定义滚动条（线框风格）
  &::-webkit-scrollbar {
    width: 6px;
  }

  &::-webkit-scrollbar-track {
    background: rgba(0, 8, 20, 0.5);
    border: 1px solid $border-subtle;
    border-radius: $radius-sm;
  }

  &::-webkit-scrollbar-thumb {
    background: rgba(0, 212, 255, 0.3);
    border: 1px solid $border-default;
    border-radius: $radius-sm;

    &:hover {
      background: rgba(0, 212, 255, 0.5);
    }
  }
}

.empty-state {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;

  .empty-text {
    font-family: $font-family-ui;
    font-size: 14px;
    color: $color-primary;
    text-transform: uppercase;
    letter-spacing: 2px;
    text-shadow: $text-shadow-sm;
    margin-bottom: 8px;
  }

  .empty-hint {
    font-family: $font-family-ui;
    font-size: 11px;
    color: $text-dim;
    letter-spacing: 1px;
  }
}

// 面板过渡动画
.panel-enter-active {
  animation: panelFadeIn 0.3s $ease-out;
}

.panel-leave-active {
  animation: panelFadeOut 0.3s $ease-in;
}

@keyframes panelFadeIn {
  from {
    opacity: 0;
    transform: scale(0.98);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

@keyframes panelFadeOut {
  from {
    opacity: 1;
    transform: scale(1);
  }
  to {
    opacity: 0;
    transform: scale(0.98);
  }
}
</style>
