// ============================================
// 面板类型扩展定义
// ============================================

/**
 * 表格面板数据
 */
export interface TablePanelData {
  columns: Array<{
    key: string
    label: string
    width?: number
    sortable?: boolean
  }>
  rows: Array<Record<string, any>>
  highlightedRows?: Map<number, string>
}

/**
 * 图表面板数据
 */
export interface ChartPanelData {
  chartType: 'line' | 'bar' | 'pie' | 'scatter'
  datasets: Array<{
    label: string
    data: number[]
    color?: string
  }>
  labels: string[]
}

/**
 * 视频面板数据
 */
export interface VideoPanelData {
  url: string
  autoplay?: boolean
  controls?: boolean
  loop?: boolean
  muted?: boolean
}

/**
 * 图片面板数据
 */
export interface ImagePanelData {
  url: string
  alt?: string
  zoom?: number
}

/**
 * Iframe 面板数据
 */
export interface IframePanelData {
  url: string
  sandbox?: string
}
