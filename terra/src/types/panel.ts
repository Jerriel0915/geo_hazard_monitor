// ============================================
// 面板相关类型定义
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
 * 批量面板配置请求
 */
export interface BatchPanelConfigRequest {
  panels: PanelConfig[]
  mode?: 'replace' | 'merge'  // replace: 替换所有面板, merge: 合并到现有面板
  clearExisting?: boolean  // 是否清除现有面板（仅 mode=replace 时有效）
}

/**
 * 批量面板配置响应
 */
export interface BatchPanelConfigResponse {
  success: boolean
  message: string
  results: Array<{
    panelId: string
    success: boolean
    error?: string
  }>
  summary?: {
    total: number
    success: number
    failed: number
  }
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

/**
 * 面板实例
 */
export interface PanelInstance {
  id: string
  type: PanelType
  config: PanelConfig
  element?: HTMLElement
  component?: any
}

/**
 * 面板渲染状态
 */
export interface PanelRenderState {
  visible: boolean
  loading: boolean
  error: string | null
}

// ============================================
// 地图面板类型
// ============================================

/**
 * 地图面板配置
 */
export interface MapPanelConfig extends PanelConfig {
  type: 'map'
  data: {
    center: [number, number]
    zoom: number
    markers?: Array<{
      id: string
      label?: string
      position: [number, number]
      state: 'normal' | 'watching' | 'warning'
      data: any
    }>
    polylines?: Array<{
      id: string
      label?: string
      points: Array<[number, number]>
      color?: string
      weight?: number
      opacity?: number
      data?: any
    }>
    polygons?: Array<{
      id: string
      label?: string
      points: Array<[number, number]>
      color?: string
      fillColor?: string
      fillOpacity?: number
      weight?: number
      data?: any
    }>
  }
}

/**
 * 地图绘制选项
 */
export interface CircleOptions {
  color?: string
  fillColor?: string
  fillOpacity?: number
  weight?: number
}

/**
 * 地图指令参数
 */
export interface MapCommandParams {
  drawCircle?: {
    center: [number, number]
    radius: number
    options?: CircleOptions
  }
  setView?: {
    center: [number, number]
    zoom?: number
  }
  fitBounds?: {
    bounds: [[number, number], [number, number]]
  }
}
