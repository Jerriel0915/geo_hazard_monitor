/** Terra 人格配置 */
export interface TerraPersonality {
  id: number
  layerType: string
  name: string
  content: string
  isActive: number
  isPreset: number
  sortOrder: number
  createTime: string
}

/** Terra 模型配置 */
export interface TerraModelConfig {
  id: number
  name: string
  baseUrl: string
  apiKey: string
  modelName: string
  maxTokens: number
  temperature: number
  isActive: number
  sortOrder: number
}

/** Terra 技能 */
export interface TerraSkill {
  id: number
  name: string
  displayName: string
  description: string
  category: string
  sourceType: string
  isEnabled: number
  version: string
  skillPath: string
  createTime: string
}

/** Terra 工具 */
export interface TerraTool {
  id: number
  toolKey: string
  name: string
  description: string
  execSide: string
  toolType: string
  category: string
  config: string
  isEnabled: number
  sortOrder: number
}

/** Terra 会话 */
export interface TerraConversation {
  id: number
  userId: number
  title: string
  status: string
  lastMessageTime: string
  messageCount: number
}

/** Terra 消息 */
export interface TerraMessageData {
  id: number
  conversationId: number
  role: string
  content: string
  toolCalls: string | null
  toolCallId: string | null
  tokensUsed: number | null
  createTime: string
}

/** SSE 事件数据类型 */
export interface TokenEvent { content: string }
export interface ToolCallEvent { callId: string; tool: string; execSide: string; params?: Record<string, unknown> }
export interface ToolResultEvent { callId: string; success: boolean; result: unknown }
export interface DoneEvent { messageId: number; tokensUsed: number }
export interface ErrorEvent { message: string }

/** 工具执行结果 */
export interface ToolResult {
  success: boolean
  result?: unknown
  error?: string
}
