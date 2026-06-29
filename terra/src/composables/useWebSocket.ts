// ============================================
// WebSocket 组合式函数
// ============================================

import { onMounted, onUnmounted } from 'vue'
import { useWebSocketStore } from '@/stores/websocket'
import type { BaseMessage, MessageType } from '@/types'

/**
 * WebSocket 连接管理组合式函数
 */
export function useWebSocket(autoConnect = true) {
  const wsStore = useWebSocketStore()

  /**
   * 发送消息
   */
  function sendMessage<T extends BaseMessage = BaseMessage>(message: Omit<BaseMessage, 'id' | 'timestamp' | 'version'>) {
    const fullMessage: T = {
      ...message,
      version: '1.0',
      id: wsStore.generateMessageId(),
      timestamp: Date.now()
    } as T

    return wsStore.send(fullMessage)
  }

  /**
   * 订阅消息
   */
  function subscribe(type: MessageType, handler: (data: any) => void) {
    wsStore.on(type, handler)

    // 返回取消订阅函数
    return () => {
      wsStore.off(type, handler)
    }
  }

  /**
   * 自动连接和断开
   */
  if (autoConnect) {
    onMounted(() => {
      wsStore.connect()
    })

    onUnmounted(() => {
      wsStore.disconnect()
    })
  }

  return {
    // 状态
    connected: wsStore.connected,
    connecting: wsStore.connecting,

    // 方法
    connect: wsStore.connect,
    disconnect: wsStore.disconnect,
    send: sendMessage,
    on: subscribe,
    off: wsStore.off,
    generateId: wsStore.generateMessageId
  }
}
