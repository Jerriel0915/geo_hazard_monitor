// ============================================
// 地图面板指令处理器
// ============================================

import { ref, type Ref } from 'vue'
import type L from 'leaflet'
import type { PanelCommand } from '@/types'

/**
 * 地图面板指令处理
 */
export function useMapPanelCommands(mapInstance: Ref<L.Map | null>) {
  /**
   * 绘制圆形
   */
  async function drawCircle(params: {
    center: [number, number]
    radius: number
    options?: any
  }) {
    if (!mapInstance.value) {
      throw new Error('Map not initialized')
    }

    // @ts-ignore - Leaflet 类型定义问题
    const circle = L.circle(params.center, {
      radius: params.radius,
      ...params.options
    })
    circle.addTo(mapInstance.value)

    return {
      shapeId: `circle-${Date.now()}`,
      bounds: circle.getBounds()
    }
  }

  /**
   * 设置视图
   */
  async function setView(params: { center: [number, number]; zoom?: number }) {
    if (!mapInstance.value) {
      throw new Error('Map not initialized')
    }

    mapInstance.value.setView(params.center, params.zoom)
    return { success: true }
  }

  /**
   * 适应边界
   */
  async function fitBounds(params: { bounds: [[number, number], [number, number]] }) {
    if (!mapInstance.value) {
      throw new Error('Map not initialized')
    }

    mapInstance.value.fitBounds(params.bounds)
    return { success: true }
  }

  /**
   * 放大
   */
  async function zoomIn() {
    if (!mapInstance.value) {
      throw new Error('Map not initialized')
    }

    mapInstance.value.zoomIn()
    return { success: true }
  }

  /**
   * 缩小
   */
  async function zoomOut() {
    if (!mapInstance.value) {
      throw new Error('Map not initialized')
    }

    mapInstance.value.zoomOut()
    return { success: true }
  }

  /**
   * 设置缩放级别
   */
  async function setZoom(params: { zoom: number }) {
    if (!mapInstance.value) {
      throw new Error('Map not initialized')
    }

    mapInstance.value.setZoom(params.zoom)
    return { success: true }
  }

  /**
   * 设置可视区域
   * 支持多种方式设置视野：
   * 1. 通过边界坐标设置
   * 2. 通过一组点自动计算边界
   * 3. 通过中心点和缩放级别设置
   */
  async function setViewport(params: {
    bounds?: [[number, number], [number, number]]
    points?: Array<[number, number]>
    center?: [number, number]
    zoom?: number
    padding?: [number, number, number, number]  // top, right, bottom, left
    animate?: boolean
    maxZoom?: number  // 可选的最大缩放级别限制
  }) {
    if (!mapInstance.value) {
      throw new Error('Map not initialized')
    }

    // 处理 padding：支持 4 元素数组 [top, right, bottom, left]
    let padding: L.PointTuple = [50, 50]  // 默认值
    if (params.padding) {
      if (params.padding.length === 4) {
        // 4 元素数组：取最大值作为水平和垂直 padding
        // 这样可以确保各个方向都有足够的边距
        const paddingVertical = Math.max(params.padding[0], params.padding[2])
        const paddingHorizontal = Math.max(params.padding[1], params.padding[3])
        padding = [paddingVertical, paddingHorizontal]
      } else if (params.padding.length === 2) {
        padding = [params.padding[0], params.padding[1]]
      }
    }

    const options: L.FitBoundsOptions = {
      animate: params.animate !== false,
      padding: padding
    }

    // 只有明确指定 maxZoom 时才设置限制，否则让地图自由缩放到最大
    if (params.maxZoom !== undefined) {
      options.maxZoom = params.maxZoom
    }

    // 优先级1: 使用明确的边界
    if (params.bounds) {
      mapInstance.value.fitBounds(params.bounds, options)
      return { success: true, method: 'bounds' }
    }

    // 优先级2: 从点集计算边界
    if (params.points && params.points.length > 0) {
      const bounds = L.latLngBounds(params.points)
      mapInstance.value.fitBounds(bounds, options)
      return { success: true, method: 'points', pointCount: params.points.length }
    }

    // 优先级3: 使用中心点和缩放级别
    if (params.center) {
      mapInstance.value.setView(params.center, params.zoom || 10, {
        animate: options.animate
      })
      return { success: true, method: 'center' }
    }

    throw new Error('setViewport requires bounds, points, or center')
  }

  /**
   * 清除形状
   */
  async function clearShapes() {
    if (!mapInstance.value) {
      throw new Error('Map not initialized')
    }

    mapInstance.value.eachLayer((layer: any) => {
      if (layer instanceof L.Circle || layer instanceof L.Polygon) {
        mapInstance.value?.removeLayer(layer)
      }
    })

    return { cleared: true }
  }

  /**
   * 绘制标记
   */
  async function drawMarker(params: { position: [number, number]; options?: any }) {
    if (!mapInstance.value) {
      throw new Error('Map not initialized')
    }

    // @ts-ignore
    const marker = L.marker(params.position, params.options)
    marker.addTo(mapInstance.value)

    return { markerId: `marker-${Date.now()}` }
  }

  /**
   * 绘制多边形
   */
  async function drawPolygon(params: { points: Array<[number, number]>; options?: any }) {
    if (!mapInstance.value) {
      throw new Error('Map not initialized')
    }

    // @ts-ignore
    const polygon = L.polygon(params.points, params.options)
    polygon.addTo(mapInstance.value)

    return { shapeId: `polygon-${Date.now()}` }
  }

  /**
   * 绘制折线
   */
  async function drawPolyline(params: { points: Array<[number, number]>; options?: any }) {
    if (!mapInstance.value) {
      throw new Error('Map not initialized')
    }

    // @ts-ignore
    const polyline = L.polyline(params.points, params.options)
    polyline.addTo(mapInstance.value)

    return { shapeId: `polyline-${Date.now()}` }
  }

  /**
   * 设置数据
   */
  async function setData(params: { data: any }) {
    // 这里可以处理批量添加标记等操作
    console.log('[MapPanel] Setting data:', params.data)
    return { success: true }
  }

  return {
    // 生命周期
    lifecycle: {
      show: async () => ({ success: true }),
      hide: async () => ({ success: true })
    },

    // 布局
    layout: {
      maximize: async () => ({ success: true }),
      restore: async () => ({ success: true }),
      minimize: async () => ({ success: true })
    },

    // 数据
    data: {
      set: setData,
      update: async (params: any) => ({ success: true }),
      refresh: async () => ({ success: true })
    },

    // 地图特定指令
    map: {
      drawCircle,
      setView,
      fitBounds,
      zoomIn,
      zoomOut,
      setZoom,
      clearShapes,
      drawMarker,
      drawPolygon,
      drawPolyline
    }
  }
}
