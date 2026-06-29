// ============================================
// WebSocket 消息类型定义
// ============================================

/**
 * 基础消息结构
 */
export interface BaseMessage {
  version: string           // 协议版本，如 "1.0"
  id: string               // 消息唯一 ID
  timestamp: number        // 时间戳
  type: MessageType        // 消息类型
  namespace?: string       // 命名空间（用于扩展）
  payload: any            // 消息负载
  meta?: Record<string, any>  // 元数据
}

/**
 * 消息类型
 */
export type MessageType =
  | 'command'              // 指令消息
  | 'event'                // 事件消息
  | 'query'                // 查询消息
  | 'response'             // 响应消息
  | 'error'                // 错误消息

/**
 * 标准命名空间
 */
export enum StandardNamespace {
  CORE = 'core',           // 核心功能
  PANEL = 'panel',         // 面板管理
  TERRA = 'terra',         // TerraMens 状态
  DATA = 'data',           // 数据更新
}

/**
 * 用户操作消息（前端 → 后端）
 */
export interface UserActionMessage extends BaseMessage {
  type: 'command'
  namespace: StandardNamespace.CORE
  payload: {
    action: string         // 操作类型
    target: string         // 目标对象
    params: Record<string, any>  // 参数
  }
}

/**
 * 状态查询消息（前端 → 后端）
 */
export interface QueryMessage extends BaseMessage {
  type: 'query'
  namespace: StandardNamespace.CORE
  payload: {
    query: string          // 查询类型
    params?: Record<string, any>
  }
}

/**
 * 指令执行结果反馈（前端 → 后端）
 */
export interface CommandResultMessage extends BaseMessage {
  type: 'response'
  namespace: StandardNamespace.CORE
  payload: {
    commandId: string      // 原始指令 ID
    success: boolean       // 执行是否成功
    result?: any          // 执行结果
    error?: string        // 错误信息
  }
}

/**
 * 通用指令消息（后端 → 前端）
 */
export interface CommandMessage extends BaseMessage {
  type: 'command'
  namespace?: StandardNamespace.CORE
  payload: {
    target: string         // 目标对象
    action: string         // 操作类型
    params: Record<string, any>  // 参数
  }
}

/**
 * 数据更新消息（后端 → 前端）
 */
export interface DataUpdateMessage extends BaseMessage {
  type: 'event'
  namespace: StandardNamespace.DATA
  payload: {
    source: string         // 数据源
    channel?: string       // 数据通道
    data: any             // 数据内容
  }
}

/**
 * 状态推送消息（后端 → 前端）
 */
export interface StatusPushMessage extends BaseMessage {
  type: 'event'
  namespace: StandardNamespace.TERRA
  payload: {
    terraState: TerraState
    watching: string[]
    alert?: AlertData
  }
}

/**
 * 思考动态消息（后端 → 前端）
 */
export interface ThinkingMessage extends BaseMessage {
  type: 'event'
  namespace: StandardNamespace.TERRA
  payload: {
    phase: 'analyzing' | 'comparing' | 'deciding' | 'acting'
    message: string
    progress?: number
    focus?: string[]
  }
}

/**
 * 面板指令消息（后端 → 前端）
 */
export interface PanelCommandMessage extends BaseMessage {
  type: 'command'
  namespace: StandardNamespace.PANEL
  payload: {
    panelId: string        // 目标面板 ID
    action: string         // 操作类型（命名空间格式：category:operation）
    params: Record<string, any>  // 参数
  }
}

/**
 * 批量指令消息
 */
export interface BatchCommandMessage extends BaseMessage {
  type: 'command'
  namespace: StandardNamespace.CORE
  payload: {
    commands: Array<Omit<BaseMessage, 'id' | 'timestamp' | 'type'>>
    mode?: 'sequential' | 'parallel'
    stopOnError?: boolean
  }
}

/**
 * 成功响应
 */
export interface SuccessResponse extends BaseMessage {
  type: 'response'
  payload: {
    success: true
    data: any
    message?: string
  }
}

/**
 * 错误响应
 */
export interface ErrorResponse extends BaseMessage {
  type: 'error'
  payload: {
    success: false
    error: {
      code: string
      message: string
      details?: any
    }
  }
}

// ============================================
// TerraMens 相关类型
// ============================================

/**
 * TerraMens 状态
 */
export type TerraState = 'normal' | 'info' | 'caution' | 'warning' | 'critical'

/**
 * 警报数据
 */
export interface AlertData {
  id: string
  level: 'attention' | 'warning' | 'critical'
  title: string
  description: string
  suggestion?: string
  actions?: Array<{
    id: string
    label: string
    primary?: boolean
  }>
  timestamp: number
}

/**
 * 时间线条目
 */
export interface TimelineItem {
  id: string
  type: 'thinking' | 'observation' | 'warning' | 'action'
  message: string
  timestamp: number
  sender?: 'user' | 'terramens'  // 发信人
  metadata?: Record<string, any>
}

// ============================================
// 面板相关类型
// ============================================

/**
 * 面板类型
 */
export type PanelType = 'map' | 'video' | 'image' | 'table' | 'chart' | 'iframe' | 'alert'

/**
 * 面板配置
 */
export interface PanelConfig {
  id: string
  type: PanelType
  title: string
  position: {
    x: number
    y: number
    w: number
    h: number
  }
  zIndex: number
  visible: boolean
  data: any
  metadata?: Record<string, any>
}

/**
 * 面板指令
 */
export interface PanelCommand {
  action: string
  params?: Record<string, any>
}

/**
 * 面板指令执行结果
 */
export interface PanelCommandResult {
  panelId: string
  command: PanelCommand
  success: boolean
  result?: any
  error?: string
  timestamp: number
}
