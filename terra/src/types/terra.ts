// ============================================
// TerraMens 相关类型定义
// ============================================

/**
 * TerraMens 状态 - 5级状态
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
  sender?: 'user' | 'terramens'
  metadata?: Record<string, any>
}

/**
 * TerraMens 完整状态
 */
export interface TerraStatus {
  state: TerraState
  watching: string[]
  message?: string
  thinking?: {
    phase: 'analyzing' | 'comparing' | 'deciding' | 'acting'
    progress: number
    focus: string[]
  }
  alert?: AlertData
}

/**
 * TerraMens 状态标签映射
 */
export const TerraStateLabels: Record<TerraState, string> = {
  normal: '巡检中',
  info: '需要关注',
  caution: '需要注意',
  warning: '发出警告',
  critical: '紧急情况'
} as const

/**
 * TerraMens 状态颜色映射
 */
export const TerraStateColors: Record<TerraState, string> = {
  normal: '#22c55e',    // 绿色
  info: '#3b82f6',      // 蓝色
  caution: '#eab308',   // 黄色
  warning: '#f97316',   // 橙色
  critical: '#ef4444'   // 红色
} as const
