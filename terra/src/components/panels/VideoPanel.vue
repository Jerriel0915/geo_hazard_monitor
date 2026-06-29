<!-- ======================================== -->
<!-- VideoPanel - 视频面板组件 -->
<!-- ======================================== -->

<template>
  <div
    class="video-panel"
    :style="panelStyle"
  >
    <div class="panel-header">
      <h3 class="panel-title">{{ config.title }}</h3>
      <div class="panel-actions">
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

    <div class="video-container">
      <video
        ref="videoElement"
        class="video-player"
        :src="videoSource"
        controls
        @play="isPlaying = true"
        @pause="isPlaying = false"
        @ended="isPlaying = false"
      ></video>
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
const videoElement = ref<HTMLVideoElement>()
const loading = ref(false)
const isMaximized = ref(false)
const isPlaying = ref(false)

/**
 * 视频源
 */
const videoSource = computed(() => {
  return props.config.data?.url || props.config.data?.src || ''
})

/**
 * 面板样式
 */
const panelStyle = computed(() => {
  if (!props.config?.position) {
    console.warn('[VideoPanel] config.position is missing:', props.config)
    return {}
  }
  const { x, y, w, h } = props.config.position
  return {
    gridColumn: `${x} / span ${w}`,
    gridRow: `${y} / span ${isMaximized.value ? 999 : h}`
  }
})

/**
 * 注册面板指令处理器
 */
const { registerAutoHandler } = usePanelCommand()

const videoOperations = {
  play: () => {
    videoElement.value?.play()
    return { success: true, playing: true }
  },

  pause: () => {
    videoElement.value?.pause()
    return { success: true, playing: false }
  },

  stop: () => {
    if (videoElement.value) {
      videoElement.value.pause()
      videoElement.value.currentTime = 0
    }
    return { success: true, playing: false }
  },

  seek: (params: { time: number }) => {
    if (videoElement.value && params.time !== undefined) {
      videoElement.value.currentTime = params.time
      return { success: true, currentTime: params.time }
    }
    return { success: false, error: 'Invalid seek time' }
  },

  setVolume: (params: { volume: number }) => {
    if (videoElement.value && params.volume !== undefined) {
      const vol = Math.max(0, Math.min(1, params.volume))
      videoElement.value.volume = vol
      return { success: true, volume: vol }
    }
    return { success: false, error: 'Invalid volume value' }
  },

  setSource: (params: { url: string }) => {
    if (params.url) {
      props.config.data.url = params.url
      return { success: true, url: params.url }
    }
    return { success: false, error: 'Invalid URL' }
  },

  screenshot: () => {
    if (!videoElement.value) return { success: false, error: 'Video not available' }

    const canvas = document.createElement('canvas')
    canvas.width = videoElement.value.videoWidth
    canvas.height = videoElement.value.videoHeight
    const ctx = canvas.getContext('2d')

    if (ctx) {
      ctx.drawImage(videoElement.value, 0, 0, canvas.width, canvas.height)
      const dataUrl = canvas.toDataURL('image/png')
      return { success: true, screenshot: dataUrl }
    }

    return { success: false, error: 'Failed to capture screenshot' }
  }
}

registerAutoHandler('video', 'play', () => videoOperations.play())
registerAutoHandler('video', 'pause', () => videoOperations.pause())
registerAutoHandler('video', 'stop', () => videoOperations.stop())
registerAutoHandler('video', 'seek', (_, cmd) => videoOperations.seek(cmd.params || {}))
registerAutoHandler('video', 'setVolume', (_, cmd) => videoOperations.setVolume(cmd.params || {}))
registerAutoHandler('video', 'setSource', (_, cmd) => videoOperations.setSource(cmd.params || {}))
registerAutoHandler('video', 'screenshot', () => videoOperations.screenshot())

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

.video-panel {
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

.video-container {
  flex: 1;
  min-height: 0;  // Allow flex item to shrink below content size
  position: relative;
  background-color: #000;
  overflow: hidden;  // Prevent video overflow

  .video-player {
    width: 100%;
    height: 100%;
    object-fit: contain;
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
