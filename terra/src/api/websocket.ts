// ============================================
// WebSocket API 客户端
// ============================================

import { useWebSocketStore } from '@/stores/websocket'
import type {
  BaseMessage,
  UserActionMessage,
  QueryMessage,
  CommandResultMessage
} from '@/types'
import { StandardNamespace } from '@/types'

/**
 * Dashboard WebSocket 客户端类
 */
export class DashboardWebSocket {
  private wsStore = useWebSocketStore()

  /**
   * 连接 WebSocket
   */
  connect(): void {
    this.wsStore.connect()
  }

  /**
   * 断开连接
   */
  disconnect(): void {
    this.wsStore.disconnect()
  }

  /**
   * 发送消息
   */
  send<T extends BaseMessage = BaseMessage>(message: Omit<BaseMessage, 'id' | 'timestamp' | 'version'>): boolean {
    const fullMessage: T = {
      ...message,
      version: '1.0',
      id: this.wsStore.generateMessageId(),
      timestamp: Date.now()
    } as T

    return this.wsStore.send(fullMessage)
  }

  /**
   * 发送用户操作
   */
  sendUserAction(action: string, target: string, params: Record<string, any> = {}): boolean {
    const message: Omit<UserActionMessage, 'id' | 'timestamp' | 'version'> = {
      type: 'command',
      namespace: StandardNamespace.CORE,
      payload: { action, target, params }
    }
    return this.send(message)
  }

  /**
   * 发送查询
   */
  sendQuery(query: string, params: Record<string, any> = {}): boolean {
    const message: Omit<QueryMessage, 'id' | 'timestamp' | 'version'> = {
      type: 'query',
      namespace: StandardNamespace.CORE,
      payload: { query, params }
    }
    return this.send(message)
  }

  /**
   * 发送指令执行结果
   */
  sendCommandResult(commandId: string, success: boolean, result?: any, error?: string): boolean {
    const message: Omit<CommandResultMessage, 'id' | 'timestamp' | 'version'> = {
      type: 'response',
      namespace: StandardNamespace.CORE,
      payload: { commandId, success, result, error }
    }
    return this.send(message)
  }

  /**
   * 订阅消息
   */
  on(type: string, handler: (data: any) => void): void {
    this.wsStore.on(type as any, handler)
  }

  /**
   * 取消订阅
   */
  off(type: string, handler: (data: any) => void): void {
    this.wsStore.off(type as any, handler)
  }

  /**
   * 获取连接状态
   */
  isConnected(): boolean {
    return this.wsStore.connected
  }

  /**
   * 获取连接中状态
   */
  isConnecting(): boolean {
    return this.wsStore.connecting
  }
}

// 创建实例的函数（必须在 Pinia 安装后调用）
export function createWebSocketClient(): DashboardWebSocket {
  return new DashboardWebSocket()
}
