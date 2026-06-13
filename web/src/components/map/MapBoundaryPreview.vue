<template>
  <div class="map-boundary-preview">
    <div ref="containerRef" :style="{ height: heightStyle }"/>
  </div>
</template>

<script setup lang="ts">
/**
 * 只读地图预览组件 — 直接照搬编辑器的渲染样式, 在地图上完整显示:
 *   - 多边形蓝色描边 + 绿色编号顶点标记
 *   - 走向红色虚线 + 红色编号端点标记
 *   - 辅助线橙色虚线 + 橙色方形点标记
 *   - 中心点蓝色星形
 *
 * 不使用 useMapEditor 的状态管理, 避免 prop 初始化时序和 watch 触发问题。
 * 视觉效果与编辑器完全一致, 但无工具栏/底栏, 用于详情弹窗等只读场景。
 */
import {computed, nextTick, onMounted, ref, watch} from 'vue'
import L from 'leaflet'
import {useLeafletMap} from '@/composables/useLeafletMap'
import type {BoundaryCoords, LatLng} from '@/lib/boundaryCoords'
import {centroid} from '@/lib/boundaryCoords'

const props = withDefaults(defineProps<{
  initialValue?: BoundaryCoords | null
  initialCenter?: LatLng | null
  height?: string | number
}>(), {
  height: 300
})

const heightStyle = computed(() =>
    typeof props.height === 'number' ? `${props.height}px` : props.height
)
const containerRef = ref<HTMLElement | null>(null)

const leaflet = useLeafletMap({
  container: containerRef,
  center: props.initialCenter ?? {lat: 30.67, lng: 104.06},
  zoom: 14
})

/** 多边形顶点图标 (与 useMapEditor 中 vertexHtml 保持一致) */
function vertexHtml(num: number): string {
  return `<div style="background:#67C23A;color:#fff;width:28px;height:28px;border-radius:50%;display:flex;align-items:center;justify-content:center;font-size:13px;font-weight:bold;border:2px solid white;">${num}</div>`
}

const STRIKE_ENDPOINT_HTML = (idx: number) =>
    `<div style="background:#f56c6c;color:#fff;width:18px;height:18px;border-radius:50%;display:flex;align-items:center;justify-content:center;font-size:10px;font-weight:bold;border:2px solid #fff;">${idx + 1}</div>`

const AUX_POINT_HTML = () =>
    '<div style="background:#fa8c16;width:16px;height:16px;border-radius:2px;border:2px solid #fff;"></div>'

const CENTER_HTML = () =>
    '<div style="background:#1890ff;color:#fff;width:30px;height:30px;border-radius:50%;display:flex;align-items:center;justify-content:center;font-size:15px;font-weight:bold;border:3px solid #fff;box-shadow:0 2px 6px rgba(0,0,0,0.4)">★</div>'

/** 渲染所有边界要素 — 一次性绘制, 不需要 watch 增量更新 */
function renderBoundary(map: L.Map) {
  const bc = props.initialValue ?? {polygon: [], strikeLine: null, auxiliaryLines: []}
  const hasPolygon = bc.polygon.length >= 3
  const hasStrikeLine = !!bc.strikeLine
  const center = props.initialCenter
      ?? (hasPolygon ? centroid(bc.polygon) : null)
      ?? {lat: 30.67, lng: 104.06}

  // 收集所有点用于 fitBounds
  const allPoints: L.LatLngTuple[] = [[center.lat, center.lng]]
  if (hasPolygon) bc.polygon.forEach(p => allPoints.push([p.lat, p.lng]))
  if (hasStrikeLine && bc.strikeLine) bc.strikeLine.forEach(p => allPoints.push([p.lat, p.lng]))
  bc.auxiliaryLines.forEach(line => line.forEach(p => allPoints.push([p.lat, p.lng])))

  // === 多边形边界 ===
  if (hasPolygon) {
    L.polygon(bc.polygon.map(p => [p.lat, p.lng] as L.LatLngTuple), {
      color: '#1890ff',
      fillColor: '#1890ff',
      fillOpacity: 0.15,
      weight: 2
    }).addTo(map).bindPopup(
        `<div style="font-size:12px"><b>监测范围</b><br>顶点数: ${bc.polygon.length}</div>`
    )

    // 绿色编号顶点
    bc.polygon.forEach((p, idx) => {
      L.marker([p.lat, p.lng], {
        icon: L.divIcon({
          className: 'preview-vertex-marker',
          html: vertexHtml(idx + 1),
          iconSize: [28, 28],
          iconAnchor: [14, 14]
        })
      }).addTo(map)
    })
  }

  // === 走向线 ===
  if (hasStrikeLine && bc.strikeLine) {
    const [a, b] = bc.strikeLine
    L.polyline([[a.lat, a.lng], [b.lat, b.lng]], {
      color: '#f56c6c',
      weight: 3,
      dashArray: '6 6'
    }).addTo(map).bindPopup(`<div style="font-size:12px"><b>走向线</b></div>`)

    // 红色编号端点
    bc.strikeLine.forEach((p, idx) => {
      L.marker([p.lat, p.lng], {
        icon: L.divIcon({
          className: 'preview-strike-marker',
          html: STRIKE_ENDPOINT_HTML(idx),
          iconSize: [18, 18],
          iconAnchor: [9, 9]
        })
      }).addTo(map)
    })
  }

  // === 辅助线 ===
  bc.auxiliaryLines.forEach((line, idx) => {
    if (line.length < 2) return
    L.polyline(line.map(p => [p.lat, p.lng] as L.LatLngTuple), {
      color: '#fa8c16',
      weight: 2,
      dashArray: '5 4'
    }).addTo(map).bindPopup(
        `<div style="font-size:12px"><b>辅助线 ${idx + 1}</b><br>顶点数: ${line.length}</div>`
    )

    // 橙色方形点
    line.forEach(p => {
      L.marker([p.lat, p.lng], {
        icon: L.divIcon({
          className: 'preview-aux-marker',
          html: AUX_POINT_HTML(),
          iconSize: [16, 16],
          iconAnchor: [8, 8]
        })
      }).addTo(map)
    })
  })

  // === 中心点 ===
  const centerIcon = L.divIcon({
    className: 'preview-center-marker',
    html: CENTER_HTML(),
    iconSize: [30, 30],
    iconAnchor: [15, 15]
  })
  L.marker([center.lat, center.lng], {icon: centerIcon, zIndexOffset: 1000})
      .addTo(map)
      .bindPopup(
          `<div style="text-align:center;font-size:13px"><b>中心点</b><br>${center.lng.toFixed(6)}, ${center.lat.toFixed(6)}</div>`
      )

  // === 自适应边界 ===
  if (allPoints.length > 1) {
    map.fitBounds(L.latLngBounds(allPoints), {padding: [40, 40], maxZoom: 17})
  } else {
    map.setView([center.lat, center.lng], 15)
  }
}

/** 当地图就绪时渲染; 初次 + props 变化时都重新渲染 */
watch(leaflet.isReady, (ready) => {
  if (!ready) return
  const map = leaflet.map.value
  if (!map) return
  nextTick(() => {
    renderBoundary(map)
    map.invalidateSize()
  })
}, {immediate: true})

// 兜底: dialog 内首次挂载时容器尺寸可能未就绪, 强制重算一次
onMounted(() => {
  setTimeout(() => {
    const map = leaflet.map.value
    if (map) {
      map.invalidateSize()
    }
  }, 200)
})
</script>

<style scoped>
.map-boundary-preview {
  display: flex;
  flex-direction: column;
}
</style>

<style>
/* 只读场景下, 顶点/端点/辅助点标记的鼠标光标保持默认, 避免看起来可拖动 */
.map-boundary-preview .leaflet-marker-icon {
  cursor: default !important;
}
</style>
