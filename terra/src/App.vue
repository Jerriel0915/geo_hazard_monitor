<template>
  <div class="dashboard-app" :class="{ [`state-${terraState}`]: true }">
    <!-- 左侧固定区域 -->
    <aside class="left-sidebar">
      <!-- 系统标题栏 -->
      <header class="system-header">
        <div class="header-title">Terra 值守</div>
      </header>

      <!-- Terra 3D 形象 -->
      <div class="terra-avatar-section">
        <TerraAvatar ref="terraAvatarRef" :state="terraState" />
      </div>

      <!-- 消息对话框区域 -->
      <div class="chat-section">
        <!-- 固定标题 -->
        <div class="chat-header">最新动态</div>

        <!-- 消息列表 -->
        <div class="chat-messages" ref="messagesContainer">
          <div
            v-for="item in sortedTimelineItems"
            :key="item.id"
            class="chat-message"
            :class="[
              `type-${item.type}`,
              { 'is-user': item.sender === 'user', 'is-terramens': item.sender !== 'user' }
            ]"
          >
            <div class="message-header">
              <span class="message-time">{{ formatTime(item.timestamp) }}</span>
              <span class="message-sender">{{ item.sender === 'user' ? '你' : 'Terra' }}</span>
            </div>
            <div class="message-content markdown-content">
              <!-- 使用 TypewriterMessage 组件实现打字机效果 -->
              <TypewriterMessage
                v-if="(item as any).isStreaming"
                :text="item.message"
                :enabled="true"
                :speed="15"
                :on-complete="() => handleStreamingComplete(item)"
              />
              <!-- 非流式消息使用 Markdown 渲染 -->
              <div v-else v-html="renderMarkdown(item.message)"></div>
            </div>
          </div>
          <div v-if="timelineItems.length === 0" class="chat-empty">
            暂无消息
          </div>
        </div>

        <!-- 用户输入区域 -->
        <div class="chat-input-area">
          <textarea
            v-model="userInput"
            class="chat-textarea"
            placeholder="输入消息... (Shift+Enter 发送)"
            @keydown="handleKeydown"
            ref="textareaRef"
          ></textarea>
          <div class="chat-actions">
            <button class="send-btn" @click="sendMessage" :disabled="!userInput.trim()">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
                <path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z"/>
              </svg>
              <span>发送</span>
            </button>
          </div>
        </div>
      </div>
    </aside>

    <!-- 右侧展示区域 -->
    <main class="display-area">
      <PanelContainer
        :panels="demoPanels"
        @focus="handlePanelFocus"
        @close="handlePanelClose"
      />
    </main>

    <!-- 底部版权栏 -->
    <footer class="copyright-bar">
      <span class="copyright-text">© 2026 知微 Zwei · Terra 值守模式</span>
    </footer>

    <!-- 警报弹窗 -->
    <AlertPanel v-if="currentAlert" :alert="currentAlert" />
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { marked } from 'marked'
import { TerraAvatar } from '@/components/terra-avatar'
import { Timeline } from '@/components/timeline'
import { AlertPanel, PanelContainer } from '@/components/panels'
import TypewriterMessage from '@/components/TypewriterMessage.vue'
import { useTerraStore } from '@/stores/terra'
import { createWebSocketClient } from '@/api/websocket'
import type { TerraState, PanelConfig } from '@/types'

// 配置 marked
marked.use({
  breaks: true,  // 支持 \n 换行
  gfm: true,     // 支持 GitHub Flavored Markdown (表格、删除线等)
  headerIds: false,
  mangle: false
})

// Pinia Store
const terraStore = useTerraStore()

// Terra 状态
const terraState = computed<TerraState>(() => terraStore.state)
const timelineItems = computed(() => terraStore.timelineItems)
const currentAlert = computed(() => terraStore.currentAlert)

// 按时间降序排列的 timeline（最新的在顶部）
const sortedTimelineItems = computed(() => {
  return [...timelineItems.value].sort((a, b) => b.timestamp - a.timestamp)
})

// 用户输入
const userInput = ref('')
const textareaRef = ref<HTMLTextAreaElement>()
const messagesContainer = ref<HTMLElement>()

// TerraAvatar 引用
const terraAvatarRef = ref<InstanceType<typeof import('@/components/terra-avatar')['TerraAvatar']>>()

// WebSocket 客户端
const wsClient = createWebSocketClient()

// 面板数据
const demoPanels = ref<PanelConfig[]>([])

// 初始化标志（防止重复初始化）
const isInitialized = ref(false)

// 追踪已处理的流式消息 runId（防止重复处理）
const processedRunIds = ref<Set<string>>(new Set())

// 限制 processedRunIds 的大小
const MAX_PROCESSED_RUN_IDS = 100

/**
 * 格式化时间
 */
function formatTime(timestamp: number): string {
  const date = new Date(timestamp)
  const hours = date.getHours().toString().padStart(2, '0')
  const minutes = date.getMinutes().toString().padStart(2, '0')
  return `${hours}:${minutes}`
}

/**
 * 处理流式消息完成
 */
function handleStreamingComplete(item: any) {
  console.log('[App] Streaming message completed:', item.runId)
  // 处理队列中的下一个消息
  terraStore.processStreamingQueue()

  // 滚动到顶部
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = 0
    }
  })
}

/**
 * Markdown 渲染（使用 marked 库）
 */
function renderMarkdown(text: string): string {
  if (!text) return ''

  // 使用 marked 库解析 Markdown
  return marked.parse(text) as string
}

/**
 * 处理键盘事件
 */
function handleKeydown(event: KeyboardEvent) {
  if (event.key === 'Enter' && event.shiftKey) {
    event.preventDefault()
    sendMessage()
  }
}

/**
 * 发送消息
 */
function sendMessage() {
  const message = userInput.value.trim()
  if (!message) return

  // 发送到后端（后端会添加用户消息和回复到时间线）
  if (wsClient.isConnected()) {
    wsClient.sendUserAction('chat_message', 'dashboard', { message })
  } else {
    // 未连接时显示错误
    terraStore.addTimelineItem({
      type: 'warning',
      message: '未连接到服务器，无法发送消息'
    })
  }

  // 清空输入
  userInput.value = ''

  // 滚动到顶部（最新的消息）
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = 0
    }
  })
}

/**
 * 处理面板聚焦
 */
function handlePanelFocus(panelId: string) {
  console.log('Panel focused:', panelId)
}

/**
 * 处理面板关闭
 */
function handlePanelClose(panelId: string) {
  const panel = demoPanels.value.find(p => p.id === panelId)
  if (panel) {
    panel.visible = false
  }
}

/**
 * 组件挂载
 */
onMounted(() => {
  // 注册全局状态获取函数（供后端查询使用）
  ;(window as any).__getDashboardState = () => {
    return {
      panels: demoPanels.value.map(panel => ({
        id: panel.id,
        type: panel.type,
        title: panel.title,
        position: panel.position,
        zIndex: panel.zIndex,
        visible: panel.visible,
        data: panel.data
      })),
      terra: {
        state: terraStore.state,
        watching: terraStore.watching,
        currentMessage: terraStore.currentMessage,
        timelineItems: terraStore.timelineItems.map(item => ({
          id: item.id,
          type: item.type,
          message: item.message,
          sender: item.sender,
          timestamp: item.timestamp
        }))
      },
      timestamp: Date.now()
    }
  }

  // 先注册 WebSocket 消息处理器（在连接之前）
  wsClient.on('event', handleEventMessage)
  wsClient.on('command', handleCommandMessage)
  wsClient.on('response', handleResponseMessage)

  // 然后连接 WebSocket
  wsClient.connect()

  // 监听连接状态，连接成功后等待一段时间再请求更新看板
  // 确保握手完成且消息处理器完全就绪
  const checkConnection = setInterval(() => {
    if (wsClient.isConnected() && !isInitialized.value) {
      clearInterval(checkConnection)
      isInitialized.value = true

      // 等待 500ms 确保一切就绪后再发送消息
      setTimeout(() => {
        // 先添加一条 Terra 的消息，让用户知道正在处理
        terraStore.addTimelineItem({
          type: 'thinking',
          message: '正在初始化值守看板...',
          sender: 'terra',
          timestamp: Date.now()
        })

        wsClient.sendUserAction('chat_message', 'dashboard', {
           message: '请分析当前系统状态，使用工具查询设备统计和告警信息，然后创建看板面板来展示系统概况。',
           skipTimeline: true  // 不在 timeline 中显示这条自动消息
        })
      }, 500)
    }
  }, 500)

  // 10秒后停止检查（避免无限循环）
  setTimeout(() => {
    clearInterval(checkConnection)
  }, 10000)
})

/**
 * 组件卸载
 */
onUnmounted(() => {
  // 清理全局函数
  delete (window as any).__getDashboardState

  // 取消订阅
  wsClient.off('event', handleEventMessage)
  wsClient.off('command', handleCommandMessage)
  wsClient.off('response', handleResponseMessage)

  // 断开连接
  wsClient.disconnect()
})


/**
 * 清理过期的 runId 记录（每5分钟执行一次）
 */
setInterval(() => {
  // 限制 Set 的大小，避免无限增长
  if (processedRunIds.value.size > MAX_PROCESSED_RUN_IDS) {
    // 保留最近的一半
    const entries = Array.from(processedRunIds.value)
    processedRunIds.value.clear()
    const keepCount = Math.floor(entries.length / 2)
    entries.slice(-keepCount).forEach(id => processedRunIds.value.add(id))
    console.log(`[App] Cleaned up old runId records, kept last ${keepCount}`)
  }
}, 5 * 60 * 1000)  // 每5分钟执行一次

/**
 * 处理事件消息
 */

/**
 * 处理事件消息
 */
function handleEventMessage(payload: any) {
  console.log('[App] ========================================')
  console.log('[App] Event Message JSON:')
  console.log(JSON.stringify(payload, null, 2))
  console.log('[App] ========================================')
  console.log('[App] Event message:', payload)

  // 处理心跳动画触发事件
  if (payload.heartbeatTrigger) {
    console.log('[App] Triggering heartbeat animation from backend event')
    nextTick(() => {
      if (terraAvatarRef.value && typeof (terraAvatarRef.value as any).heartbeat === 'function') {
        (terraAvatarRef.value as any).heartbeat()
      }
    })
    // 不需要处理其他内容
    return
  }

  // 处理 Terra 状态更新
  if (payload.terraState) {
    terraStore.updateState(payload.terraState)
  }
  if (payload.watching) {
    terraStore.updateWatching(payload.watching)
  }
  if (payload.alert) {
    terraStore.setAlert(payload.alert)
  }
  if (payload.message) {
    terraStore.updateMessage(payload.message)
  }

  // 处理时间线条目
  if (payload.timelineItem) {
    let timelineMessage = payload.timelineItem.message || ''
    const isStreaming = payload.timelineItem.isStreaming
    const runId = payload.timelineItem.runId

    // 检查是否包含 HEARTBEAT_OK
    const hasHeartbeatOk = timelineMessage.includes('HEARTBEAT_OK')

    if (hasHeartbeatOk) {
      // 触发 TerraAvatar 的心跳动画
      console.log('[App] Triggering heartbeat animation')
      nextTick(() => {
        if (terraAvatarRef.value && typeof (terraAvatarRef.value as any).heartbeat === 'function') {
          (terraAvatarRef.value as any).heartbeat()
        }
      })

      // 移除 HEARTBEAT_OK 字符串
      timelineMessage = timelineMessage.replace(/HEARTBEAT_OK/g, '').trim()

      // 如果移除后为空，则不显示该消息
      if (!timelineMessage) {
        console.log('[App] Filtered HEARTBEAT_OK only message')
        return
      }

      // 否则显示剩余内容
      console.log('[App] Showing message after filtering HEARTBEAT_OK:', timelineMessage)
      payload.timelineItem.message = timelineMessage
    }

    // 处理流式消息完成
    if (!isStreaming && runId) {
      // 检查是否已处理过此 runId
      if (processedRunIds.value.has(runId)) {
        console.log('[App] Duplicate completion message, already processed:', runId)
        return
      }

      // 标记为已处理
      processedRunIds.value.add(runId)

      // 这是一个非流式消息但有 runId，说明流式输出已完成
      // 查找并更新现有的流式消息，将其标记为已完成
      const existingItem = timelineItems.value.find((t: any) => t.runId === runId)
      if (existingItem) {
        // 更新消息内容并标记为非流式
        ;(existingItem as any).isStreaming = false
        existingItem.message = timelineMessage
        console.log('[App] Streaming message completed:', runId)
        // 滚动到顶部并返回，不继续处理
        nextTick(() => {
          if (messagesContainer.value) {
            messagesContainer.value.scrollTop = 0
          }
        })
        return
      } else {
        // 没有找到现有的流式消息，这是一个孤立的完成消息
        // 可能是因为页面刷新导致的，直接添加为新消息
        console.log('[App] Orphan completion message, adding as new:', runId)
        payload.timelineItem.message = timelineMessage
        terraStore.addTimelineItem(payload.timelineItem)
        // 滚动到顶部并返回
        nextTick(() => {
          if (messagesContainer.value) {
            messagesContainer.value.scrollTop = 0
          }
        })
        return
      }
    } else if (isStreaming && runId) {
      // 清理已处理的 runId（新的流式消息开始）
      processedRunIds.value.delete(runId)

      // 这是流式消息，传递给 Store 处理（Store 会处理队列）
      terraStore.addTimelineItem(payload.timelineItem)
      // 滚动到顶部并返回
      nextTick(() => {
        if (messagesContainer.value) {
          messagesContainer.value.scrollTop = 0
        }
      })
      return
    }

    // 普通消息，直接添加
    console.log('[App] Adding timeline item:', payload.timelineItem)
    terraStore.addTimelineItem(payload.timelineItem)
    // 滚动到顶部（最新的消息）
    nextTick(() => {
      if (messagesContainer.value) {
        messagesContainer.value.scrollTop = 0
      }
    })
  }
}

/**
 * 处理指令消息
 */
function handleCommandMessage(payload: any) {
  console.log('[App] ========================================')
  console.log('[App] Command Message JSON:')
  console.log(JSON.stringify(payload, null, 2))
  console.log('[App] ========================================')
  console.log('[App] Command message:', payload)

  // payload 格式: { target: string, action: string, params: any }
  // 或面板格式: { panelId: string, action: string, params: any }
  // 或全局命令格式: { action: string, params: any }
  const params = payload.params
  const target = payload.target || payload.panelId || (params && params.panelId)
  const action = payload.action

  // 处理全局命令（无 target）
  if (!target) {
    if (action === 'clearAll') {
      // 清空所有面板
      demoPanels.value = []
      console.log('[App] All panels cleared')
      return
    }
    console.warn('[App] Command missing target and action:', action)
    return
  }

  // 处理面板命令（支持 target 和 panelId 两种格式）
  if (payload.panelId || payload.target) {
    const panelId = payload.panelId || payload.target

    if (action === 'lifecycle:create') {
      // 创建新面板，支持 zIndex 和 visible 参数
      // 支持 panelType 和 type 两种参数名
      const panelType = params.panelType || params.type
      const newPanel: PanelConfig = {
        id: panelId,
        type: panelType,
        title: params.title,
        position: params.position || { x: 1, y: 1, w: 6, h: 4 },
        zIndex: params.zIndex ?? demoPanels.value.length + 1,
        visible: params.visible ?? true,
        data: params.data || {}
      }
      demoPanels.value.push(newPanel)
      console.log('[App] Panel created:', panelId, 'with position:', newPanel.position, 'full panel:', newPanel)
      return
    }

    if (action === 'lifecycle:destroy') {
      // 销毁面板
      const index = demoPanels.value.findIndex(p => p.id === panelId)
      if (index > -1) {
        demoPanels.value.splice(index, 1)
        console.log('[App] Panel destroyed:', panelId)
      }
      return
    }

    // 其他面板命令（setData, show, hide 等）
    const panel = demoPanels.value.find(p => p.id === panelId)
    if (panel) {
      if (action === 'show') {
        panel.visible = true
      } else if (action === 'hide') {
        panel.visible = false
      } else if (action === 'setData') {
        // 智能合并数据而不是完全替换
        panel.data = { ...panel.data, ...params }
      } else if (action === 'update') {
        // 通用更新命令，支持更新标题、数据等
        if (params.title !== undefined) {
          panel.title = params.title
        }
        if (params.data !== undefined) {
          // 深度合并数据
          panel.data = { ...panel.data, ...params.data }
        }
        if (params.visible !== undefined) {
          panel.visible = params.visible
        }
        if (params.position !== undefined) {
          panel.position = params.position
        }
        if (params.zIndex !== undefined) {
          panel.zIndex = params.zIndex
        }
      }
      console.log('[App] Panel updated:', panelId, action)
    }
    return
  }

  // 处理 Terra 状态更新
  if (target === 'terra' && action === 'update') {
    if (params.terraState) {
      terraStore.updateState(params.terraState)
    }
    if (params.message) {
      terraStore.updateMessage(params.message)
    }
    if (params.watching) {
      terraStore.updateWatching(params.watching)
    }
    if (params.alert) {
      terraStore.setAlert(params.alert)
    }
    return
  }

  // 处理面板指令
  // 可以根据需要添加更多面板类型的处理
  console.log('[App] Unhandled command:', { target, action, params })
}

/**
 * 处理响应消息
 */
function handleResponseMessage(payload: any) {
  console.log('[App] ========================================')
  console.log('[App] Response Message JSON:')
  console.log(JSON.stringify(payload, null, 2))
  console.log('[App] ========================================')
  console.log('[App] Response message:', payload)
}
</script>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.dashboard-app {
  width: 100vw;
  height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: $bg-primary;
  color: $text-primary;
  overflow: hidden;

  // 左侧固定区域
  .left-sidebar {
    position: fixed;
    left: 0;
    top: 0;
    bottom: $statusbar-height;
    width: 360px;
    background: $bg-secondary;
    backdrop-filter: $backdrop-blur;
    border-right: 1px solid $border-default;
    display: flex;
    flex-direction: column;
    z-index: 10;
    box-shadow: $shadow-md;

    // 系统标题栏
    .system-header {
      flex-shrink: 0;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 12px;
      background: rgba(0, 8, 20, 0.8);
      border-bottom: 1px solid $border-default;
      position: relative;

      // 线框风格装饰线
      &::after {
        content: '';
        position: absolute;
        bottom: -1px;
        left: 0;
        right: 0;
        height: 1px;
        background: linear-gradient(
          to right,
          transparent,
          $border-accent 50%,
          transparent
        );
      }

      .header-title {
        font-family: $font-family-ui;
        font-size: 18px;
        font-weight: $font-weight-bold;
        color: $color-primary;
        letter-spacing: 3px;
        text-shadow: $text-shadow-md;
      }
    }

    .terra-avatar-section {
      flex-shrink: 0;
      padding: 0;
      border-bottom: 1px solid $border-default;
      background: rgba($bg-tertiary, 0.3);
      position: relative;

      // 线框风格装饰线
      &::after {
        content: '';
        position: absolute;
        bottom: 0;
        left: 0;
        right: 0;
        height: 1px;
        background: linear-gradient(
          to right,
          transparent,
          $border-accent,
          transparent
        );
      }
    }

    // 消息对话框区域
    .chat-section {
      flex: 1;
      display: flex;
      flex-direction: column;
      overflow: hidden;

      .chat-header {
        flex-shrink: 0;
        padding: 12px 16px;
        font-family: $font-family-ui;
        font-size: 11px;
        font-weight: $font-weight-semibold;
        color: $color-primary;
        text-transform: uppercase;
        letter-spacing: 2px;
        background: rgba(0, 8, 20, 0.8);
        border-bottom: 1px solid $border-default;
        text-shadow: $text-shadow-sm;
        position: relative;

        // 线框风格装饰线
        &::after {
          content: '';
          position: absolute;
          bottom: -1px;
          left: 0;
          width: 30%;
          height: 1px;
          background: linear-gradient(
            to right,
            $border-accent,
            transparent
          );
        }
      }

      .chat-messages {
        flex: 1;
        overflow-y: scroll;  /* 改为 scroll 而不是 auto，确保滚动条始终可见 */
        overflow-x: hidden;
        display: flex;
        flex-direction: column;
        gap: 8px;
        padding: 16px;
        scroll-behavior: smooth;
        position: relative;  /* 添加相对定位 */

        // 自定义滚动条样式
        &::-webkit-scrollbar {
          width: 8px;  /* 增加宽度 */
        }

        &::-webkit-scrollbar-track {
          background: rgba(0, 8, 20, 0.3);
          border-radius: 4px;
        }

        &::-webkit-scrollbar-thumb {
          background: rgba(0, 212, 255, 0.4);
          border-radius: 4px;
          transition: background 0.3s;

          &:hover {
            background: rgba(0, 212, 255, 0.6);
          }

          &:active {
            background: rgba(0, 212, 255, 0.8);
          }
        }

        // 确保滚动条可交互
        &::-webkit-scrollbar-thumb:window-inactive {
          background: rgba(0, 212, 255, 0.3);
        }

        // Firefox 滚动条
        scrollbar-width: thin;
        scrollbar-color: rgba(0, 212, 255, 0.4) rgba(0, 8, 20, 0.3);

        .chat-message {
          display: flex;
          flex-direction: column;
          gap: 6px;
          padding: 0;
          border: none;
          border-radius: 0;
          animation: chatSlideIn 0.3s $ease-out;
          background: transparent;
          position: relative;

          // 左侧装饰线
          &::before {
            content: '';
            position: absolute;
            left: -10px;
            top: 0;
            bottom: 0;
            width: 3px;
            background: $border-default;
          }

          &.type-observation::before {
            background: $terra-info;
          }

          &.type-thinking::before {
            background: $color-primary;
          }

          &.type-warning::before {
            background: $terra-warning;
          }

          &.type-action::before {
            background: $terra-normal;
          }

          &.is-user {
            background: rgba($color-secondary, 0.05);

            &::before {
              background: $color-secondary;
            }
          }

          .message-header {
            display: flex;
            align-items: center;
            gap: 10px;
            padding: 6px 0 8px 0;
            border-bottom: 1px solid $border-subtle;
          }

          .message-sender {
            font-family: $font-family-ui;
            font-size: 11px;
            font-weight: $font-weight-semibold;
            color: $color-primary;
            text-transform: uppercase;
            letter-spacing: 1px;
          }

          .message-time {
            font-family: $font-family-ui;
            font-size: 10px;
            color: $text-dim;
          }

          .message-content {
            font-family: $font-family-base;
            font-size: $font-size-small;
            color: $text-secondary;
            line-height: 1.5;
            word-wrap: break-word;
          }
        }

        .chat-empty {
          display: flex;
          align-items: center;
          justify-content: center;
          flex: 1;
          color: $text-dim;
          font-family: $font-family-ui;
          font-size: $font-size-small;
          letter-spacing: 1px;
        }
      }

      // 用户输入区域
      .chat-input-area {
        flex-shrink: 0;
        border-top: 1px solid $border-default;
        background: $bg-tertiary;
        backdrop-filter: $backdrop-blur-sm;

        .chat-textarea {
          width: 100%;
          min-height: 60px;
          max-height: 120px;
          padding: 12px;
          font-family: $font-family-base;
          font-size: $font-size-body;
          color: $text-primary;
          background: transparent;
          border: none;
          outline: none;
          resize: none;

          &::placeholder {
            color: $text-muted;
          }

          &:focus {
            background: rgba(0, 12, 28, 0.5);
          }
        }

        .chat-actions {
          display: flex;
          justify-content: flex-end;
          padding: 8px 12px;
          gap: 8px;

          .send-btn {
            display: flex;
            align-items: center;
            gap: 6px;
            padding: 6px 16px;
            font-family: $font-family-ui;
            font-size: $font-size-small;
            font-weight: $font-weight-medium;
            color: $color-primary;
            background: transparent;
            border: 1px solid $border-accent;
            border-radius: $radius-sm;
            cursor: pointer;
            transition: all $transition-fast $ease-out;
            text-transform: uppercase;
            letter-spacing: 1px;
            box-shadow: $glow-primary;

            &:hover:not(:disabled) {
              background: rgba($color-primary, 0.15);
              box-shadow: $shadow-md;
            }

            &:active:not(:disabled) {
              transform: scale(0.98);
            }

            &:disabled {
              opacity: 0.4;
              cursor: not-allowed;
              box-shadow: none;
            }

            svg {
              flex-shrink: 0;
            }
          }
        }
      }
    }
  }

  // 右侧展示区域
  .display-area {
    margin-left: 360px;
    height: calc(100vh - $statusbar-height);
    overflow: hidden;
    border-left: 1px solid $border-subtle;
    background: rgba(0, 8, 20, 0.3);
  }

  // 底部版权栏
  .copyright-bar {
    position: fixed;
    bottom: 0;
    left: 0;
    right: 0;
    height: $statusbar-height;
    display: flex;
    align-items: center;
    justify-content: center;
    background: $bg-secondary;
    backdrop-filter: $backdrop-blur;
    border-top: 1px solid $border-default;
    z-index: 20;

    .copyright-text {
      font-family: $font-family-ui;
      font-size: 10px;
      color: $text-dim;
      font-weight: $font-weight-normal;
      letter-spacing: 1px;
      text-transform: uppercase;
    }
  }
}

// 聊天消息滑入动画
@keyframes chatSlideIn {
  from {
    opacity: 0;
    transform: translateX(-10px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}
</style>

<!-- 非 scoped 样式：用于 v-html 渲染的 Markdown 内容 -->
<style lang="scss">
@use '@/styles/variables' as *;

// Markdown 内容样式（全局，用于 v-html 渲染）
.markdown-content {
  // 段落样式
  p {
    margin: 6px 0;
    line-height: 1.6;

    &:empty {
      display: none;
    }
  }

  // 代码块样式
  pre {
    background: linear-gradient(135deg, rgba(0, 12, 28, 0.9), rgba(0, 8, 20, 0.95));
    border: 1px solid $border-default;
    border-radius: $radius-sm;
    padding: 12px;
    margin: 12px 0;
    overflow-x: auto;
    font-size: 11px;
    line-height: 1.5;
    box-shadow: inset 0 1px 3px rgba(0, 0, 0, 0.3);
    position: relative;

    &::before {
      content: 'CODE';
      position: absolute;
      top: 4px;
      right: 8px;
      font-size: 8px;
      color: $text-dim;
      letter-spacing: 1px;
      opacity: 0.5;
    }

    code {
      background: none;
      padding: 0;
      border: none;
      font-family: 'Monaco', 'Menlo', 'Consolas', monospace;
      color: #a5d6ff;
      text-shadow: 0 0 10px rgba(0, 212, 255, 0.3);
    }
  }

  // 行内代码样式
  code {
    background: linear-gradient(135deg, rgba(0, 12, 28, 0.7), rgba(0, 8, 20, 0.8));
    border: 1px solid rgba(0, 212, 255, 0.2);
    padding: 3px 8px;
    border-radius: 4px;
    font-family: 'Monaco', 'Menlo', 'Consolas', monospace;
    font-size: 11px;
    color: #7ee787;
    box-shadow: 0 0 8px rgba(126, 231, 135, 0.15);
  }

  // 粗体样式
  strong {
    font-weight: $font-weight-bold;
    color: $text-primary;
    text-shadow: 0 0 8px rgba(255, 255, 255, 0.2);
  }

  // 斜体样式
  em {
    font-style: italic;
    color: $text-secondary;
  }

  // 链接样式
  a {
    color: $color-primary;
    text-decoration: none;
    border-bottom: 1px solid transparent;
    text-shadow: $text-shadow-sm;
    transition: all $transition-fast $ease-out;
    position: relative;

    &::after {
      content: '';
      position: absolute;
      bottom: -1px;
      left: 0;
      width: 0;
      height: 1px;
      background: linear-gradient(90deg, $border-accent, $color-primary);
      transition: width $transition-normal $ease-out;
    }

    &:hover {
      color: #fff;

      &::after {
        width: 100%;
      }
    }
  }

  // Markdown 表格样式（增强版）
  table {
    width: 100%;
    border-collapse: separate;
    border-spacing: 0;
    margin: 12px 0;
    font-size: $font-size-small;
    font-family: $font-family-ui;
    background: rgba(0, 8, 20, 0.4);
    border: 1px solid $border-default;
    border-radius: $radius-sm;
    overflow: hidden;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);

    thead {
      tr {
        background: linear-gradient(180deg, rgba(0, 12, 28, 0.95), rgba(0, 8, 20, 0.9));
        border-bottom: 2px solid $border-accent;
        box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
      }

      th {
        padding: 10px 14px;
        text-align: left;
        font-weight: $font-weight-bold;
        color: $color-primary;
        text-transform: uppercase;
        letter-spacing: 1px;
        font-size: 10px;
        border-right: 1px solid $border-subtle;
        position: relative;
        text-shadow: $text-shadow-sm;

        &:last-child {
          border-right: none;
        }

        // 装饰线
        &::after {
          content: '';
          position: absolute;
          bottom: 0;
          left: 0;
          right: 0;
          height: 2px;
          background: linear-gradient(90deg, transparent, $border-accent, transparent);
          opacity: 0.5;
        }
      }
    }

    tbody {
      tr {
        border-bottom: 1px solid $border-subtle;
        transition: all $transition-fast $ease-out;
        background: transparent;

        // 条纹效果
        &:nth-child(even) {
          background: rgba(0, 212, 255, 0.02);
        }

        &:hover {
          background: rgba($color-primary, 0.08);
          box-shadow: inset 0 0 0 1px rgba($color-primary, 0.2);

          td {
            color: $text-primary;
          }
        }

        &:last-child {
          border-bottom: none;
        }

        td {
          padding: 10px 14px;
          color: $text-secondary;
          border-right: 1px solid $border-subtle;
          transition: color $transition-fast $ease-out;

          &:last-child {
            border-right: none;
          }

          // 表格中的代码
          code {
            font-size: 10px;
            padding: 2px 5px;
          }
        }
      }
    }
  }

  // 列表样式（增强版）
  ul, ol {
    margin: 10px 0;
    padding-left: 24px;

    li {
      margin: 6px 0;
      line-height: 1.6;
      color: $text-secondary;
      position: relative;

      // 列表项标记颜色
      &::marker {
        color: $color-primary;
      }
    }
  }

  // 无序列表自定义样式
  ul {
    list-style: none;
    padding-left: 16px;

    li {
      position: relative;
      padding-left: 16px;

      &::before {
        content: '▸';
        position: absolute;
        left: 0;
        color: $color-primary;
        font-size: 12px;
        text-shadow: $text-shadow-sm;
      }
    }

    // 嵌套列表
    ul {
      padding-left: 20px;

      li::before {
        content: '◦';
        font-size: 10px;
      }
    }
  }

  // 有序列表样式
  ol {
    li {
      padding-left: 6px;

      &::marker {
        font-weight: $font-weight-semibold;
        color: $border-accent;
      }
    }
  }

  // 标题样式（增强版）
  h1, h2, h3, h4, h5, h6 {
    margin: 14px 0 8px 0;
    font-weight: $font-weight-bold;
    color: $text-primary;
    line-height: 1.3;
    position: relative;
    padding-left: 12px;

    &:first-child {
      margin-top: 4px;
    }

    // 左侧装饰线
    &::before {
      content: '';
      position: absolute;
      left: 0;
      top: 4px;
      bottom: 4px;
      width: 3px;
      background: linear-gradient(180deg, $border-accent, $color-primary);
      border-radius: 2px;
    }
  }

  h1 {
    font-size: 16px;
    text-transform: uppercase;
    letter-spacing: 1px;
    padding-bottom: 4px;
    border-bottom: 1px solid $border-medium;

    &::before {
      height: calc(100% - 8px);
    }
  }

  h2 {
    font-size: 14px;
    letter-spacing: 0.5px;
  }

  h3 {
    font-size: 13px;
    color: #a5d6ff;

    &::before {
      background: linear-gradient(180deg, $border-accent, #7ee787);
    }
  }

  h4, h5, h6 {
    font-size: $font-size-small;
    color: $text-secondary;

    &::before {
      background: $border-default;
    }
  }

  // 引用块样式（增强版）
  blockquote {
    margin: 12px 0;
    padding: 12px 16px;
    border-left: 4px solid $border-accent;
    background: linear-gradient(90deg, rgba(0, 12, 28, 0.6), rgba(0, 8, 20, 0.3));
    color: $text-secondary;
    font-style: italic;
    border-radius: 0 $radius-sm $radius-sm 0;
    position: relative;
    box-shadow: inset 0 1px 3px rgba(0, 0, 0, 0.2);

    // 引用符号装饰
    &::before {
      content: '"';
      position: absolute;
      top: 4px;
      left: 8px;
      font-size: 24px;
      color: $border-accent;
      opacity: 0.3;
      font-family: Georgia, serif;
    }

    p {
      margin: 0;
      padding-left: 16px;
    }
  }

  // 水平线样式（增强版）
  hr {
    margin: 16px 0;
    border: none;
    height: 1px;
    background: linear-gradient(90deg,
      transparent,
      $border-medium 20%,
      $border-accent 50%,
      $border-medium 80%,
      transparent
    );
    position: relative;

    // 中间装饰点
    &::after {
      content: '◆';
      position: absolute;
      left: 50%;
      top: 50%;
      transform: translate(-50%, -50%);
      background: rgba(0, 8, 20, 0.9);
      padding: 0 8px;
      color: $border-accent;
      font-size: 10px;
    }
  }

  // 删除线样式
  del {
    color: $text-dim;
    text-decoration: line-through;
    opacity: 0.7;
  }

  // 键盘快捷键样式
  kbd {
    background: linear-gradient(180deg, rgba(255, 255, 255, 0.1), rgba(255, 255, 255, 0.05));
    border: 1px solid $border-default;
    border-radius: 4px;
    padding: 2px 6px;
    font-family: $font-family-ui;
    font-size: 10px;
    color: $color-primary;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2), inset 0 1px 1px rgba(255, 255, 255, 0.1);
  }

  // 图片样式
  img {
    max-width: 100%;
    height: auto;
    border-radius: $radius-sm;
    border: 1px solid $border-default;
    margin: 8px 0;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
  }

  // 任务列表样式
  input[type="checkbox"] {
    margin-right: 8px;
    accent-color: $color-primary;
  }
}
</style>
