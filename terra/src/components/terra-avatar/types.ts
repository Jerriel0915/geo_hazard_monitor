// ============================================
// TerraAvatar 组件类型定义
// ============================================

/**
 * 球体状态 - 5级状态
 */
export type SphereState = 'normal' | 'info' | 'caution' | 'warning' | 'critical'

/**
 * 量子球体配置
 */
export interface QuantumSphereConfig {
  size?: number
  color?: {
    core?: string
    glow?: string
  }
  state?: SphereState
  breathing?: boolean
  breathingSpeed?: number
}
