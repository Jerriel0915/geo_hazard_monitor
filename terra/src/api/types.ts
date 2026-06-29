// ============================================
// API 类型定义
// ============================================

/**
 * 健康检查响应
 */
export interface HealthCheckResponse {
  status: 'ok' | 'error'
  version: string
  timestamp: number
}

/**
 * 配置响应
 */
export interface ConfigResponse {
  [key: string]: any
}

/**
 * Agent 消息响应
 */
export interface AgentMessageResponse {
  id: string
  role: 'user' | 'assistant' | 'system'
  content: string
  timestamp: number
}

/**
 * 会话信息
 */
export interface SessionInfo {
  id: string
  agentId: string
  createdAt: number
  messageCount: number
}

/**
 * 初始化状态
 */
export interface InitializerStatus {
  initialized: boolean
  steps: Array<{
    name: string
    completed: boolean
    error?: string
  }>
}
