<!-- ======================================== -->
<!-- MapPanel - 地图面板组件 -->
<!-- ======================================== -->

<template>
  <div
    class="map-panel"
    :class="{ [`state-${markerState}`]: true }"
    :style="panelStyle"
  >
    <div class="panel-header">
      <h3 class="panel-title">{{ config.title }}</h3>
      <div class="panel-actions">
        <button class="action-btn" @click="handleMaximize" title="最大化">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
            <path d="M2 2h5v2H4v3H2V2zm7 0h5v5h-2V4H9V2zM2 9h2v3h3v2H2V9zm12 0h-2v3h-3v2h5V9z"/>
          </svg>
        </button>
        <button class="action-btn close" @click="handleClose" title="关闭">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
            <path d="M4.646 4.646a.5.5 0 0 1 .708 0L8 7.293l2.646-2.647a.5.5 0 0 1 .708.708L8.707 8l2.647 2.646a.5.5 0 0 1-.708.708L8 8.707l-2.646 2.647a.5.5 0 0 1-.708-.708L7.293 8 4.646 5.354a.5.5 0 0 1 0-.708z"/>
          </svg>
        </button>
      </div>
    </div>

    <div ref="mapContainer" class="map-container"></div>

    <div v-if="loading" class="panel-loading">
      <div class="spinner"></div>
      <span>加载中...</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, watch, nextTick } from 'vue'
import L from 'leaflet'
import { usePanelCommand } from '@/composables/usePanelCommand'
import { useMapPanelCommands } from '@/composables/useMapPanelCommands'
import type { MapPanelConfig } from '@/types'

/**
 * Props
 */
interface Props {
  id: string
  config: MapPanelConfig
}

const props = defineProps<Props>()

/**
 * Emits
 */
interface Emits {
  (e: 'focus', panelId: string): void
  (e: 'close', panelId: string): void
}

const emit = defineEmits<Emits>()

// 监听props变化以进行调试
watch(() => props.config, (newConfig) => {
  console.log('[MapPanel] Config changed:', {
    id: props.id,
    hasConfig: !!newConfig,
    configKeys: newConfig ? Object.keys(newConfig) : 'no config',
    position: newConfig?.position,
    fullConfig: newConfig
  })
}, { immediate: true, deep: true })

/**
 * 状态
 */
const mapContainer = ref<HTMLElement>()
const mapInstance = ref<L.Map | null>(null)
const loading = ref(true)
const isMaximized = ref(false)
const drawnLayers = ref<{
  markers: L.Marker[]
  polylines: L.Polyline[]
  polygons: L.Polygon[]
}>({
  markers: [],
  polylines: [],
  polygons: []
})

/**
 * 面板样式
 */
const panelStyle = computed(() => {
  if (!props.config?.position) {
    console.warn('[MapPanel] config.position is missing:', props.config)
    return {}
  }

  const { x, y, w, h } = props.config.position
  return {
    gridColumn: `${x} / span ${w}`,
    gridRow: `${y} / span ${isMaximized.value ? 999 : h}`
  }
})

/**
 * 标记状态（根据 markers 的最高风险等级）
 */
const markerState = computed(() => {
  if (!props.config.data.markers || props.config.data.markers.length === 0) {
    return 'normal'
  }
  const states = props.config.data.markers.map(m => m.state)
  if (states.includes('warning')) return 'warning'
  if (states.includes('watching')) return 'watching'
  return 'normal'
})

/**
 * 初始化地图
 */
function initMap() {
  if (!mapContainer.value) {
    console.warn('[MapPanel] Container not available, cannot initialize map')
    loading.value = false
    return
  }

  // 天地图 API Key
  const TDT_TOKEN = '8037710b2c683f6f31e5260d8f7e174d'

  // 天地图 WMTS 服务 URL 模板
  const VEC_C = `http://{s}.tianditu.com/img_c/wmts?layer=img&style=default&tilematrixset=c&Service=WMTS&Request=GetTile&Version=1.0.0&Format=tiles&TileMatrix={z}&TileCol={x}&TileRow={y}&tk=${TDT_TOKEN}`
  const CIA_C = `http://{s}.tianditu.com/cia_c/wmts?layer=cia&style=default&tilematrixset=c&Service=WMTS&Request=GetTile&Version=1.0.0&Format=tiles&TileMatrix={z}&TileCol={x}&TileRow={y}&tk=${TDT_TOKEN}`

  // 创建地图实例 - 使用 EPSG4326 (WGS84经纬度) 坐标系
  mapInstance.value = L.map(mapContainer.value, {
    zoomControl: false,
    attributionControl: false,
    crs: L.CRS.EPSG4326,
    minZoom: 3,
    maxZoom: 17,
    zoomSnap: 0.1
  }).setView(props.config.data.center, props.config.data.zoom)

  // 添加天地图影像底图
  const imgLayer = L.tileLayer(VEC_C, {
    maxZoom: 20,
    tileSize: 256,
    zoomOffset: 1,
    subdomains: ['t0', 't1', 't2', 't3', 't4', 't5', 't6', 't7']
  })

  // 添加天地图影像注记
  const ciaLayer = L.tileLayer(CIA_C, {
    maxZoom: 20,
    tileSize: 256,
    zoomOffset: 1,
    subdomains: ['t0', 't1', 't2', 't3', 't4', 't5', 't6', 't7']
  })

  // 先添加注记，再添加底图（底图在上层）
  ciaLayer.addTo(mapInstance.value)
  imgLayer.addTo(mapInstance.value)

  // 添加缩放控件（右下角）
  L.control.zoom({
    position: 'bottomright'
  }).addTo(mapInstance.value)

  // 添加比例尺
  L.control.scale({
    position: 'bottomleft',
    imperial: false
  }).addTo(mapInstance.value)

  // 添加标记
  if (props.config.data.markers) {
    props.config.data.markers.forEach(marker => {
      addMarker(marker)
    })
  }

  // 添加折线
  if (props.config.data.polylines) {
    props.config.data.polylines.forEach(polyline => {
      addPolyline(polyline)
    })
  }

  // 添加多边形
  if (props.config.data.polygons) {
    props.config.data.polygons.forEach(polygon => {
      addPolygon(polygon)
    })
  }

  // 强制刷新地图
  setTimeout(() => {
    mapInstance.value?.invalidateSize()
  }, 200)

  loading.value = false
}

/**
 * 状态翻译映射
 */
const stateLabels: Record<string, string> = {
  normal: '正常',
  watching: '观测中',
  warning: '预警'
}

/**
 * 添加标记
 */
function addMarker(marker: { id: string; label?: string; position: [number, number]; state: string; data: any }) {
  if (!mapInstance.value) return

  const colorMap: Record<string, string> = {
    normal: '#10b981',
    watching: '#f59e0b',
    warning: '#ef4444'
  }

  const color = colorMap[marker.state] || colorMap.normal
  const displayName = marker.label || marker.id
  const stateLabel = stateLabels[marker.state] || marker.state

  // 自定义图标
  const icon = L.divIcon({
    className: 'custom-marker',
    html: `<div style="
      width: 16px;
      height: 16px;
      background: ${color};
      border: 2px solid white;
      border-radius: 50%;
      box-shadow: 0 2px 8px rgba(0,0,0,0.3);
    "></div>`,
    iconSize: [16, 16],
    iconAnchor: [8, 8]
  })

  const markerInstance = L.marker(marker.position, { icon })
    .addTo(mapInstance.value)
    .bindPopup(`<b>${displayName}</b><br>状态: ${stateLabel}`)

  // 保存引用以便后续清理
  drawnLayers.value.markers.push(markerInstance)
}

/**
 * 添加折线
 */
function addPolyline(polyline: { id: string; label?: string; points: Array<[number, number]>; color?: string; weight?: number; opacity?: number; data?: any }) {
  if (!mapInstance.value) return

  const displayName = polyline.label || polyline.id

  const polylineInstance = L.polyline(polyline.points, {
    color: polyline.color || '#3b82f6',
    weight: polyline.weight || 3,
    opacity: polyline.opacity !== undefined ? polyline.opacity : 0.8
  })

  polylineInstance.addTo(mapInstance.value)

  // 如果有标签，添加弹出窗口
  if (polyline.label || polyline.data) {
    let popupContent = `<b>${displayName}</b>`
    if (polyline.data) {
      popupContent += `<br>${JSON.stringify(polyline.data)}`
    }
    polylineInstance.bindPopup(popupContent)
  }

  // 保存引用以便后续清理
  drawnLayers.value.polylines.push(polylineInstance)
}

/**
 * 添加多边形
 */
function addPolygon(polygon: { id: string; label?: string; points: Array<[number, number]>; color?: string; fillColor?: string; fillOpacity?: number; weight?: number; data?: any }) {
  if (!mapInstance.value) return

  const displayName = polygon.label || polygon.id

  const polygonInstance = L.polygon(polygon.points, {
    color: polygon.color || '#ef4444',
    fillColor: polygon.fillColor || '#ef4444',
    fillOpacity: polygon.fillOpacity !== undefined ? polygon.fillOpacity : 0.2,
    weight: polygon.weight || 2
  })

  polygonInstance.addTo(mapInstance.value)

  // 如果有标签，添加弹出窗口
  if (polygon.label || polygon.data) {
    let popupContent = `<b>${displayName}</b>`
    if (polygon.data) {
      popupContent += `<br>${JSON.stringify(polygon.data)}`
    }
    polygonInstance.bindPopup(popupContent)
  }

  // 保存引用以便后续清理
  drawnLayers.value.polygons.push(polygonInstance)
}

/**
 * 注册面板指令处理器
 */
const { registerAutoHandler } = usePanelCommand()
const { executeCommand } = useMapPanelCommands(mapInstance)

// 注册所有地图指令
registerAutoHandler('map', 'drawCircle', executeCommand)
registerAutoHandler('map', 'setView', executeCommand)
registerAutoHandler('map', 'setViewport', executeCommand)
registerAutoHandler('map', 'fitBounds', executeCommand)
registerAutoHandler('map', 'zoomIn', executeCommand)
registerAutoHandler('map', 'zoomOut', executeCommand)
registerAutoHandler('map', 'setZoom', executeCommand)
registerAutoHandler('map', 'clearShapes', executeCommand)
registerAutoHandler('map', 'drawMarker', executeCommand)
registerAutoHandler('map', 'drawPolygon', executeCommand)
registerAutoHandler('map', 'drawPolyline', executeCommand)
registerAutoHandler('map', 'setData', executeCommand)

/**
 * 最大化面板
 */
function handleMaximize() {
  isMaximized.value = !isMaximized.value
  // 重新调整地图尺寸
  setTimeout(() => {
    mapInstance.value?.invalidateSize()
  }, 300)
}

/**
 * 关闭面板
 */
function handleClose() {
  emit('close', props.id)
}

/**
 * 监听配置数据变化
 */
watch(() => props.config.data, (newData, oldData) => {
  if (!mapInstance.value) return

  // 处理中心点变化
  if (newData.center && (!oldData || newData.center[0] !== oldData.center?.[0] || newData.center[1] !== oldData.center?.[1])) {
    mapInstance.value.setView(newData.center, newData.zoom || mapInstance.value.getZoom())
  }

  // 处理缩放级别变化
  if (newData.zoom !== undefined && newData.zoom !== oldData?.zoom) {
    mapInstance.value.setZoom(newData.zoom)
  }

  // 处理标记变化
  if (newData.markers) {
    // 清除现有标记
    drawnLayers.value.markers.forEach(marker => {
      mapInstance.value?.removeLayer(marker)
    })
    drawnLayers.value.markers = []
    // 添加新标记
    newData.markers.forEach(marker => {
      addMarker(marker)
    })
  }

  // 处理折线变化
  if (newData.polylines) {
    // 清除现有折线
    drawnLayers.value.polylines.forEach(polyline => {
      mapInstance.value?.removeLayer(polyline)
    })
    drawnLayers.value.polylines = []
    // 添加新折线
    newData.polylines.forEach(polyline => {
      addPolyline(polyline)
    })
  }

  // 处理多边形变化
  if (newData.polygons) {
    // 清除现有多边形
    drawnLayers.value.polygons.forEach(polygon => {
      mapInstance.value?.removeLayer(polygon)
    })
    drawnLayers.value.polygons = []
    // 添加新多边形
    newData.polygons.forEach(polygon => {
      addPolygon(polygon)
    })
  }

  // 强制刷新地图尺寸
  setTimeout(() => {
    mapInstance.value?.invalidateSize()
  }, 100)
}, { deep: true })

/**
 * 组件挂载
 */
onMounted(async () => {
  // 等待 DOM 完全渲染后再初始化地图
  await nextTick()

  // 检查容器是否有尺寸，如果没有则等待（最多等待 5 秒）
  const checkContainerSize = () => {
    return new Promise<void>((resolve) => {
      let attempts = 0
      const maxAttempts = 300 // 300 * 16ms ≈ 5 秒

      const check = () => {
        if (mapContainer.value && mapContainer.value.offsetWidth > 0 && mapContainer.value.offsetHeight > 0) {
          resolve()
        } else if (attempts < maxAttempts) {
          attempts++
          requestAnimationFrame(check)
        } else {
          console.warn('[MapPanel] Container size check timeout, proceeding anyway')
          resolve() // 超时后继续执行
        }
      }
      check()
    })
  }

  await checkContainerSize()

  // 初始化地图
  initMap()

  // 地图初始化后再次刷新尺寸
  await nextTick()
  setTimeout(() => {
    mapInstance.value?.invalidateSize()
  }, 300)
})

/**
 * 组件卸载
 */
onUnmounted(() => {
  mapInstance.value?.remove()
  mapInstance.value = null
})
</script>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.map-panel {
  background: $bg-secondary;
  backdrop-filter: $backdrop-blur;
  border: 1px solid $border-default;
  border-radius: $radius-sm;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  position: relative;
  transition: all $transition-fast $ease-out;
  box-shadow: $shadow-sm;
  height: 100%;

  &:hover {
    border-color: $border-medium;
    box-shadow: $shadow-md;
  }

  // 移除状态边框效果，保持统一的青色线框风格
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: rgba(0, 8, 20, 0.8);
  border-bottom: 1px solid $border-default;
  position: relative;

  // 线框装饰
  &::after {
    content: '';
    position: absolute;
    bottom: -1px;
    left: 0;
    width: 30%;
    height: 1px;
    background: linear-gradient(
      to right,
      $border-accent,
      transparent
    );
  }

  .panel-title {
    font-family: $font-family-ui;
    font-size: 11px;
    font-weight: $font-weight-semibold;
    color: $color-primary;
    text-transform: uppercase;
    letter-spacing: 1px;
    text-shadow: $text-shadow-sm;
  }

  .panel-actions {
    display: flex;
    gap: 4px;

    .action-btn {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 24px;
      height: 24px;
      border: 1px solid $border-subtle;
      background: transparent;
      color: $text-secondary;
      border-radius: $radius-sm;
      cursor: pointer;
      transition: all $transition-fast $ease-out;

      &:hover {
        border-color: $border-accent;
        background: rgba($color-primary, 0.1);
        color: $color-primary;
        box-shadow: $glow-primary;
      }

      &.close:hover {
        border-color: $border-warning;
        background: rgba($terra-warning, 0.1);
        color: $terra-warning;
        box-shadow: $glow-warning;
      }
    }
  }
}

.map-container {
  flex: 1;
  min-height: 0;
  position: relative;
  overflow: hidden;

  :deep(.leaflet-container) {
    background: $bg-primary;
  }

  :deep(.custom-marker) {
    background: transparent !important;
  }
}

.panel-loading {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  background: rgba($bg-secondary, 0.95);
  backdrop-filter: $backdrop-blur;
  color: $color-primary;
  font-family: $font-family-ui;
  font-size: $font-size-small;
  letter-spacing: 1px;
  text-transform: uppercase;

  .spinner {
    width: 28px;
    height: 28px;
    border: 2px solid $border-default;
    border-top-color: $border-accent;
    border-radius: 50%;
    animation: spin 1s linear infinite;
  }
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
