<template>
  <div class="map-boundary-preview">
    <div ref="containerRef" :style="{ height: heightStyle }"/>
  </div>
</template>

<script setup lang="ts">
/**
 * 只读地图预览组件 — 直接照搬编辑器的渲染样式, 在地图上完整显示:
 *   - 多边形蓝色描边 + 绿色小圆点顶点标记（无序号, 缩小尺寸）
 *   - 走向红色虚线 + 红色编号端点标记
 *   - 辅助线橙色虚线 + 橙色小方形点标记（缩小以突出线本身）
 *   - 中心点蓝色小星形
 *   - 绑定设备 marker（只读, 颜色随 status/onlineStatus 联动）
 *
 * 不使用 useMapEditor 的状态管理, 避免 prop 初始化时序和 watch 触发问题。
 * 视觉效果与编辑器完全一致, 但无工具栏/底栏, 用于详情弹窗等只读场景。
 */
import {computed, nextTick, onMounted, onUnmounted, ref, shallowRef, watch} from 'vue'
import L from 'leaflet'
import {useLeafletMap} from '@/composables/useLeafletMap'
import {getDeviceMapIconPath} from '@/utils/deviceIcon'
import type {BoundaryCoords, LatLng} from '@/lib/boundaryCoords'
import {centroid} from '@/lib/boundaryCoords'
import type {BoundDevice} from '@/views/basic/composables/useHazardPointDeviceBind'

const props = withDefaults(defineProps<{
  initialValue?: BoundaryCoords | null
  initialCenter?: LatLng | null
  height?: string | number
  boundDevices?: BoundDevice[]
}>(), {
  height: 300,
  boundDevices: () => []
})

const renderLayer = shallowRef<L.LayerGroup | null>(null)

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
function vertexHtml(): string {
  return `<div style="background:#67C23A;width:10px;height:10px;border-radius:50%;border:1.5px solid #fff;"></div>`
}

const STRIKE_ENDPOINT_HTML = (idx: number) =>
    `<div style="background:#f56c6c;color:#fff;width:14px;height:14px;border-radius:50%;display:flex;align-items:center;justify-content:center;font-size:9px;font-weight:bold;border:1.5px solid #fff;">${idx + 1}</div>`

const AUX_POINT_HTML = () =>
    '<div style="background:#fa8c16;width:8px;height:8px;border-radius:2px;border:1.5px solid #fff;"></div>'

const CENTER_HTML = () =>
    '<div style="background:#1890ff;color:#fff;width:14px;height:14px;border-radius:50%;border:1.5px solid #fff;display:flex;align-items:center;justify-content:center;font-size:9px">★</div>'

/** 渲染所有边界要素到 layerGroup — 每次调用先清除旧图层再重绘 */
function renderBoundary(map: L.Map) {
  // 清除上一次渲染的所有图层
  if (renderLayer.value) {
    renderLayer.value.clearLayers()
  } else {
    renderLayer.value = L.layerGroup().addTo(map)
  }
  const lg = renderLayer.value

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
      fillOpacity: 0.10,
      weight: 2
    }).addTo(lg).bindPopup(
        `<div style="font-size:12px"><b>监测范围</b><br>顶点数: ${bc.polygon.length}</div>`
    )

    // 绿色顶点
    bc.polygon.forEach((p) => {
      L.marker([p.lat, p.lng], {
        icon: L.divIcon({
          className: 'preview-vertex-marker',
          html: vertexHtml(),
          iconSize: [16, 16],
          iconAnchor: [8, 8]
        })
      }).addTo(lg)
    })
  }

  // === 走向线 ===
  if (hasStrikeLine && bc.strikeLine) {
    const [a, b] = bc.strikeLine
    L.polyline([[a.lat, a.lng], [b.lat, b.lng]], {
      color: '#f56c6c',
      weight: 3,
      dashArray: '6 6'
    }).addTo(lg).bindPopup(`<div style="font-size:12px"><b>走向线</b></div>`)

    bc.strikeLine.forEach((p, idx) => {
      L.marker([p.lat, p.lng], {
        icon: L.divIcon({
          className: 'preview-strike-marker',
          html: STRIKE_ENDPOINT_HTML(idx),
          iconSize: [14, 14],
          iconAnchor: [7, 7]
        })
      }).addTo(lg)
    })
  }

  // === 辅助线 ===
  bc.auxiliaryLines.forEach((line, idx) => {
    if (line.length < 2) return
    L.polyline(line.map(p => [p.lat, p.lng] as L.LatLngTuple), {
      color: '#fa8c16',
      weight: 2,
      dashArray: '5 4'
    }).addTo(lg).bindPopup(
        `<div style="font-size:12px"><b>辅助线 ${idx + 1}</b><br>顶点数: ${line.length}</div>`
    )

    line.forEach(p => {
      L.marker([p.lat, p.lng], {
        icon: L.divIcon({
          className: 'preview-aux-marker',
          html: AUX_POINT_HTML(),
          iconSize: [12, 12],
          iconAnchor: [6, 6]
        })
      }).addTo(lg)
    })
  })

  // === 中心点 ===
  const centerIcon = L.divIcon({
    className: 'preview-center-marker',
    html: CENTER_HTML(),
    iconSize: [18, 18],
    iconAnchor: [9, 9]
  })
  L.marker([center.lat, center.lng], {icon: centerIcon, zIndexOffset: 1000})
      .addTo(lg)
      .bindPopup(
          `<div style="text-align:center;font-size:13px"><b>中心点</b><br>${center.lng.toFixed(6)}, ${center.lat.toFixed(6)}</div>`
      )

  // === 绑定设备 marker ===
  props.boundDevices.forEach(d => {
    if (d.installLongitude == null || d.installLatitude == null) return
    allPoints.push([d.installLatitude, d.installLongitude])
    const icon = L.icon({
      iconUrl: getDeviceMapIconPath(d),
      iconSize: [28, 28],
      iconAnchor: [14, 14]
    })
    L.marker([d.installLatitude, d.installLongitude], {icon, zIndexOffset: 500})
        .addTo(lg)
        .bindPopup(
            `<div style="font-size:12px;line-height:1.6"><b>${d.deviceName}</b><br>编号: ${d.deviceCode}<br>坐标: ${d.installLongitude!.toFixed(6)}, ${d.installLatitude!.toFixed(6)}</div>`
        )
  })

  // === 自适应边界 ===
  if (allPoints.length > 1) {
    map.fitBounds(L.latLngBounds(allPoints), {padding: [40, 40], maxZoom: 17})
  } else {
    map.setView([center.lat, center.lng], 15)
  }
}

/** 地图就绪 + 数据就绪 → 渲染 */
watch([leaflet.isReady, () => [props.initialValue, props.initialCenter, props.boundDevices]],
    ([ready]) => {
      if (!ready) return
      const map = leaflet.map.value
      if (!map) return
      nextTick(() => {
        renderBoundary(map)
        map.invalidateSize()
      })
    },
    {immediate: true, deep: true}
)

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
