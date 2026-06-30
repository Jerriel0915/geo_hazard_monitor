// ============================================
// WebSocket Store
// ============================================

import type { BaseMessage, MessageType } from '@/types'
import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

/**
 * 获取 JWT token — 优先从 localStorage 读取（与主前端共享），
 * 回退到 URL hash 参数（直接访问 /terra#token=xxx 时）。
 */
function getToken(): string | null {
  // 从 localStorage 获取（主前端登录后存储）
  const stored = localStorage.getItem('token')
  if (stored) return stored

  // 从 URL hash 获取
  const hash = window.location.hash
  if (hash) {
    const match = hash.match(/[#&]token=([^&]+)/)
    if (match) return decodeURIComponent(match[1])
  }

  return null
}

export const useWebSocketStore = defineStore('websocket', () => {
  // 状态
  const ws = ref<WebSocket | null>(null)
  const connected = ref(false)
  const connecting = ref(false)
  const url = ref('')
  const reconnectAttempts = ref(0)
  const maxReconnectAttempts = 5
  const reconnectDelay = 3000

  // 消息处理器
  const messageHandlers = ref<Map<MessageType, Set<(data: any) => void>>>(new Map())

  // WebSocket URL — 值守模式端点，携带 JWT token
  const wsUrl = computed(() => {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const host = import.meta.env.VITE_WS_URL || window.location.host
    // 从 localStorage 或 URL 参数获取 token
    const token = getToken()
    const tokenParam = token ? `?token=${encodeURIComponent(token)}` : ''
    return `${protocol}//${host}/ws/terramens/duty${tokenParam}`
  })

  /**
   * 连接 WebSocket
   */
  function connect() {
    if (connected.value || connecting.value) return

    connecting.value = true
    url.value = wsUrl.value

    try {
      ws.value = new WebSocket(url.value)

      ws.value.onopen = () => {
        console.log('[WebSocket] Connected to', url.value)
        connected.value = true
        connecting.value = false
        reconnectAttempts.value = 0
      }

      ws.value.onmessage = (event) => {
        try {
          const message: BaseMessage = JSON.parse(event.data)
          handleMessage(message)
        } catch (error) {
          console.error('[WebSocket] Failed to parse message:', error)
        }
      }

      ws.value.onclose = (event) => {
        console.log('[WebSocket] Disconnected', event.code, event.reason)
        connected.value = false
        connecting.value = false
        ws.value = null

        // 尝试重连
        if (reconnectAttempts.value < maxReconnectAttempts) {
          reconnectAttempts.value++
          console.log(`[WebSocket] Reconnecting... (${reconnectAttempts.value}/${maxReconnectAttempts})`)
          setTimeout(connect, reconnectDelay)
        }
      }

      ws.value.onerror = (error) => {
        console.error('[WebSocket] Error:', error)
        connecting.value = false
      }
    } catch (error) {
      console.error('[WebSocket] Failed to connect:', error)
      connecting.value = false
    }
  }

  /**
   * 断开连接
   */
  function disconnect() {
    if (ws.value) {
      ws.value.close()
      ws.value = null
    }
    connected.value = false
    reconnectAttempts.value = maxReconnectAttempts // 防止自动重连
  }

  /**
   * 发送消息
   */
  function send(message: BaseMessage) {
    if (!connected.value || !ws.value) {
      console.warn('[WebSocket] Not connected, message not sent:', message)
      return false
    }

    try {
      ws.value.send(JSON.stringify(message))
      return true
    } catch (error) {
      console.error('[WebSocket] Failed to send message:', error)
      return false
    }
  }

  /**
   * 注册消息处理器
   */
  function on(type: MessageType, handler: (data: any) => void) {
    if (!messageHandlers.value.has(type)) {
      messageHandlers.value.set(type, new Set())
    }
    messageHandlers.value.get(type)!.add(handler)
  }

  /**
   * 注销消息处理器
   */
  function off(type: MessageType, handler: (data: any) => void) {
    const handlers = messageHandlers.value.get(type)
    if (handlers) {
      handlers.delete(handler)
      if (handlers.size === 0) {
        messageHandlers.value.delete(type)
      }
    }
  }

  /**
   * 处理接收到的消息
   */
  function handleMessage(message: BaseMessage) {
    // 打印完整的 JSON 格式消息
    console.log('[WebSocket] ========================================')
    console.log('[WebSocket] Received JSON:')
    console.log(JSON.stringify(message, null, 2))
    console.log('[WebSocket] ========================================')

    // 特殊处理查询消息
    if (message.type === 'query') {
      console.log('[WebSocket] Processing query message:', message)
      const payload = message.payload as any
      if (payload.query === 'get_state') {
        console.log('[WebSocket] Got get_state query, preparing response...')
        // 发送状态响应
        const stateResponse = {
          version: '1.0',
          id: message.id,
          timestamp: Date.now(),
          type: 'response',
          namespace: 'core',
          payload: {
            success: true,
            state: (window as any).__getDashboardState?.() || { panels: [], terra: {} }
          }
        }
        console.log('[WebSocket] Sending state response:', JSON.stringify(stateResponse).substring(0, 200) + '...')
        // 直接发送原始 JSON 字符串
        try {
          ws.value?.send(JSON.stringify(stateResponse))
          console.log('[WebSocket] Sent state response for query:', message.id)
        } catch (error) {
          console.error('[WebSocket] Failed to send state response:', error)
        }
        return
      }
    }

    const handlers = messageHandlers.value.get(message.type)
    if (handlers) {
      console.log(`[WebSocket] Found ${handlers.size} handlers for type: ${message.type}`)
      handlers.forEach(handler => {
        try {
          handler(message.payload)
        } catch (error) {
          console.error('[WebSocket] Handler error:', error)
        }
      })
    } else {
      console.log(`[WebSocket] No handlers found for type: ${message.type}`)
    }
  }

  /**
   * 生成消息 ID
   */
  function generateMessageId(): string {
    return `msg-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`
  }

  return {
    // 状态
    connected,
    connecting,
    url,
    reconnectAttempts,

    // 方法
    connect,
    disconnect,
    send,
    on,
    off,
    generateMessageId
  }
})
