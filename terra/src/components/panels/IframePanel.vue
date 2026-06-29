<!-- ======================================== -->
<!-- IframePanel - iframe 嵌入面板组件 -->
<!-- ======================================== -->

<template>
  <div
    class="iframe-panel"
    :style="panelStyle"
  >
    <div class="panel-header">
      <h3 class="panel-title">{{ config.title }}</h3>
      <div class="panel-actions">
        <button class="action-btn" @click="handleRefresh" title="刷新">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
            <path d="M11.534 7h3.932a.25.25 0 0 1 .192.41l-1.966 2.36a.25.25 0 0 1-.384 0l-1.966-2.36a.25.25 0 0 1 .192-.41zm-11 2h3.932a.25.25 0 0 0 .192-.41L2.692 6.23a.25.25 0 0 0-.384 0L.342 8.59A.25.25 0 0 0 .534 9z"/>
            <path fill-rule="evenodd" d="M8 3c-1.552 0-2.94.707-3.857 1.818a.5.5 0 1 1-.771-.636A6.002 6.002 0 0 1 13.917 7H12.9A5.002 5.002 0 0 0 8 3zM3.1 9a5.002 5.002 0 0 0 8.757 2.182.5.5 0 1 1 .771.636A6.002 6.002 0 0 1 2.083 9H3.1z"/>
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

    <div class="iframe-container">
      <iframe
        v-if="iframeUrl"
        :src="iframeUrl"
        :title="config.title"
        frameborder="0"
        allowfullscreen
        referrerpolicy="no-referrer"
      ></iframe>
      <div v-else class="no-content">
        <p>暂无内容</p>
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

/**
 * iframe URL
 */
const iframeUrl = computed(() => {
  return props.config.data?.url || props.config.data?.src || ''
})

/**
 * 面板样式
 */
const panelStyle = computed(() => {
  if (!props.config?.position) {
    console.warn('[IframePanel] config.position is missing:', props.config)
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

const iframeOperations = {
  setSource: (params: { url: string }) => {
    if (params.url) {
      props.config.data.url = params.url
      loading.value = true
      // 模拟加载完成
      setTimeout(() => {
        loading.value = false
      }, 1000)
      return { success: true, url: params.url }
    }
    return { success: false, error: 'Invalid URL' }
  },

  refresh: () => {
    loading.value = true
    // 触发 URL 更新来刷新 iframe
    const currentUrl = props.config.data.url
    props.config.data.url = ''
    setTimeout(() => {
      props.config.data.url = currentUrl
      loading.value = false
    }, 100)
    return { success: true }
  },

  postMessage: (params: { message: any; targetOrigin?: string }) => {
    // 向 iframe 发送消息（需要 iframe 支持）
    const iframe = document.querySelector(`.iframe-panel iframe`) as HTMLIFrameElement
    if (iframe && iframe.contentWindow) {
      iframe.contentWindow.postMessage(
        params.message,
        params.targetOrigin || '*'
      )
      return { success: true, message: params.message }
    }
    return { success: false, error: 'Iframe not found or not ready' }
  }
}

registerAutoHandler('iframe', 'setSource', (_, cmd) => iframeOperations.setSource(cmd.params || {}))
registerAutoHandler('iframe', 'refresh', () => iframeOperations.refresh())
registerAutoHandler('iframe', 'postMessage', (_, cmd) => iframeOperations.postMessage(cmd.params || {}))

/**
 * 刷新
 */
function handleRefresh() {
  iframeOperations.refresh()
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

.iframe-panel {
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

.iframe-container {
  flex: 1;
  min-height: 0;  // Allow flex item to shrink below content size
  position: relative;
  background-color: $bg-primary;
  overflow: hidden;  // Prevent iframe overflow

  iframe {
    width: 100%;
    height: 100%;
    border: none;
  }
}

.no-content {
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
  z-index: 10;

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
