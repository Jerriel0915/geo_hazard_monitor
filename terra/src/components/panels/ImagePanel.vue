<!-- ======================================== -->
<!-- ImagePanel - 图片预览面板组件 -->
<!-- ======================================== -->

<template>
  <div
    class="image-panel"
    :style="panelStyle"
  >
    <div class="panel-header">
      <h3 class="panel-title">{{ config.title }}</h3>
      <div class="panel-actions">
        <button class="action-btn" @click="handleZoomIn" title="放大">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
            <path d="M8 4a.5.5 0 0 1 .5.5v3h3a.5.5 0 0 1 0 1h-3v3a.5.5 0 0 1-1 0v-3h-3a.5.5 0 0 1 0-1h3v-3A.5.5 0 0 1 8 4z"/>
          </svg>
        </button>
        <button class="action-btn" @click="handleZoomOut" title="缩小">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
            <path d="M4 8a.5.5 0 0 1 .5-.5h7a.5.5 0 0 1 0 1h-7A.5.5 0 0 1 4 8z"/>
          </svg>
        </button>
        <button class="action-btn" @click="handleReset" title="重置">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
            <path d="M8 3a5 5 0 1 0 4.546 2.914.5.5 0 0 1 .908-.417A6 6 0 1 1 8 2v1z"/>
            <path d="M8 4.466V.534a.25.25 0 0 1 .41-.192l2.36 1.966c.12.1.12.284 0 .384L8.41 4.658A.25.25 0 0 1 8 4.466z"/>
          </svg>
        </button>
        <button class="action-btn" @click="handleMaximize" title="最大化">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
            <path d="M2 2h5v2H4v3H2V2zm7 0h5v5h-2V4H9V2zM2 9h2v3h3v2H2V9zm12 0h-2v3h-3v2h5V9z"/>
          </svg>
        </button>
        <button class="action-btn close" @click="handleClose" title="关闭">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
            <path d="M4.646 4.646a.5.5 0 0 1 .708 0L8 7.293l2.646-2.647a.5.5 0 0 1 .708.708L8.707 8l2.647 2.646a.5.5 0 0 1-.708.708L8 8.707l-2.646 2.647a.5.5 0 0 1-.708-.708L7.293 8 4.646 5.354a.5.5 0 0 1 0-.708z"/>
          </svg>
        </button>
      </div>
    </div>

    <div class="image-container" @wheel="handleWheel" @mousedown="handleMouseDown">
      <div
        class="image-wrapper"
        :style="imageStyle"
      >
        <img
          v-if="imageUrl"
          :src="imageUrl"
          :alt="config.title"
          @dragstart.prevent
        />
        <div v-else class="no-image">
          <p>暂无图片</p>
        </div>
      </div>
    </div>

    <div v-if="loading" class="panel-loading">
      <div class="spinner"></div>
      <span>加载中...</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { usePanelCommand } from '@/composables/usePanelCommand'
import type { PanelConfig } from '@/types'

/**
 * Props
 */
interface Props {
  id: string
  config: PanelConfig
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
 * 状态
 */
const loading = ref(false)
const isMaximized = ref(false)
const zoom = ref(1)
const panX = ref(0)
const panY = ref(0)
const isDragging = ref(false)
const dragStart = ref({ x: 0, y: 0 })

/**
 * 图片URL
 */
const imageUrl = computed(() => {
  return props.config.data?.url || props.config.data?.src || ''
})

/**
 * 面板样式
 */
const panelStyle = computed(() => {
  if (!props.config?.position) {
    console.warn('[ImagePanel] config.position is missing:', props.config)
    return {}
  }
  const { x, y, w, h } = props.config.position
  return {
    gridColumn: `${x} / span ${w}`,
    gridRow: `${y} / span ${isMaximized.value ? 999 : h}`
  }
})

/**
 * 图片容器样式
 */
const imageStyle = computed(() => ({
  transform: `scale(${zoom.value}) translate(${panX.value}px, ${panY.value}px)`
}))

/**
 * 注册面板指令处理器
 */
const { registerAutoHandler } = usePanelCommand()

const imageOperations = {
  setSource: (params: { url: string }) => {
    if (params.url) {
      props.config.data.url = params.url
      return { success: true, url: params.url }
    }
    return { success: false, error: 'Invalid URL' }
  },

  zoomIn: (params?: { amount?: number }) => {
    const amount = params?.amount || 0.1
    zoom.value = Math.min(zoom.value + amount, 5)
    return { success: true, zoom: zoom.value }
  },

  zoomOut: (params?: { amount?: number }) => {
    const amount = params?.amount || 0.1
    zoom.value = Math.max(zoom.value - amount, 0.1)
    return { success: true, zoom: zoom.value }
  },

  reset: () => {
    zoom.value = 1
    panX.value = 0
    panY.value = 0
    return { success: true, zoom: 1, panX: 0, panY: 0 }
  },

  setZoom: (params: { zoom: number }) => {
    if (params.zoom !== undefined) {
      zoom.value = Math.max(0.1, Math.min(5, params.zoom))
      return { success: true, zoom: zoom.value }
    }
    return { success: false, error: 'Invalid zoom value' }
  }
}

registerAutoHandler('image', 'setSource', (_, cmd) => imageOperations.setSource(cmd.params || {}))
registerAutoHandler('image', 'zoomIn', (_, cmd) => imageOperations.zoomIn(cmd.params))
registerAutoHandler('image', 'zoomOut', (_, cmd) => imageOperations.zoomOut(cmd.params))
registerAutoHandler('image', 'reset', () => imageOperations.reset())
registerAutoHandler('image', 'setZoom', (_, cmd) => imageOperations.setZoom(cmd.params || {}))

/**
 * 放大
 */
function handleZoomIn() {
  imageOperations.zoomIn()
}

/**
 * 缩小
 */
function handleZoomOut() {
  imageOperations.zoomOut()
}

/**
 * 重置
 */
function handleReset() {
  imageOperations.reset()
}

/**
 * 滚轮缩放
 */
function handleWheel(e: WheelEvent) {
  e.preventDefault()
  if (e.deltaY < 0) {
    handleZoomIn()
  } else {
    handleZoomOut()
  }
}

/**
 * 鼠标按下
 */
function handleMouseDown(e: MouseEvent) {
  if (e.button === 0) {
    isDragging.value = true
    dragStart.value = { x: e.clientX - panX.value, y: e.clientY - panY.value }

    const handleMouseMove = (e: MouseEvent) => {
      if (isDragging.value) {
        panX.value = e.clientX - dragStart.value.x
        panY.value = e.clientY - dragStart.value.y
      }
    }

    const handleMouseUp = () => {
      isDragging.value = false
      document.removeEventListener('mousemove', handleMouseMove)
      document.removeEventListener('mouseup', handleMouseUp)
    }

    document.addEventListener('mousemove', handleMouseMove)
    document.addEventListener('mouseup', handleMouseUp)
  }
}

/**
 * 最大化面板
 */
function handleMaximize() {
  isMaximized.value = !isMaximized.value
}

/**
 * 关闭面板
 */
function handleClose() {
  emit('close', props.id)
}
</script>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.image-panel {
  background-color: $bg-secondary;
  border: 1px solid $border-subtle;
  border-radius: $radius-md;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  height: 100%;  // 确保 grid 单元格高度生效
  min-height: 240px;  // 确保面板有最小高度
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background-color: rgba($bg-primary, 0.5);
  border-bottom: 1px solid $border-subtle;

  .panel-title {
    font-size: 14px;
    font-weight: 600;
    color: $text-primary;
  }

  .panel-actions {
    display: flex;
    gap: 8px;

    .action-btn {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 28px;
      height: 28px;
      border: none;
      background: transparent;
      color: $text-secondary;
      border-radius: $radius-sm;
      cursor: pointer;
      transition: all 0.2s $ease-default;

      &:hover {
        background-color: rgba($color-primary, 0.1);
        color: $text-primary;
      }

      &.close:hover {
        background-color: rgba($terra-warning, 0.1);
        color: $terra-warning;
      }
    }
  }
}

.image-container {
  flex: 1;
  min-height: 0;  // Allow flex item to shrink below content size
  position: relative;
  overflow: hidden;
  background-color: $bg-primary;
  cursor: grab;

  &:active {
    cursor: grabbing;
  }
}

.image-wrapper {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  transform-origin: center center;
  transition: transform 0.1s ease-out;

  img {
    max-width: 100%;
    max-height: 100%;
    object-fit: contain;
    user-select: none;
    pointer-events: none;
  }
}

.no-image {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;

  p {
    color: $text-dim;
    font-size: 14px;
  }
}

.panel-loading {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  background-color: rgba($bg-secondary, 0.9);
  color: $text-secondary;
  font-size: 14px;

  .spinner {
    width: 32px;
    height: 32px;
    border: 3px solid $border-subtle;
    border-top-color: $color-primary;
    border-radius: 50%;
    animation: spin 1s linear infinite;
  }
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
