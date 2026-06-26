<template>
  <div class="map-boundary-preview">
    <div ref="containerRef" :style="{ height: heightStyle }"/>
  </div>
</template>

<script setup lang="ts">
/**
 * 只读地图预览组件 — 纯线轮廓展示，与首页隐患点悬浮窗风格一致:
 *   - 多边形蓝色描边（轻量填充）
 *   - 走向红色虚线
 *   - 辅助线橙色虚线
 *
 * 不显示任何顶点/端点/中心点标记, 不使用 useMapEditor 的状态管理。
 */
import {computed, nextTick, onMounted, ref, shallowRef, watch} from 'vue'
import L from 'leaflet'
import {useLeafletMap} from '@/composables/useLeafletMap'
import type {BoundaryCoords, LatLng} from '@/lib/boundaryCoords'
import {centroid} from '@/lib/boundaryCoords'

export interface VideoDeviceMarker {
  videoDeviceId: string
  deviceCode: string
  deviceName: string
  installLongitude?: number | null
  installLatitude?: number | null
}

const props = withDefaults(defineProps<{
  initialValue?: BoundaryCoords | null
  initialCenter?: LatLng | null
  height?: string | number
  videoDevices?: VideoDeviceMarker[]
}>(), {
  height: 300,
  videoDevices: () => []
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
  const allPoints: L.LatLngTuple[] = []
  if (hasPolygon) bc.polygon.forEach(p => allPoints.push([p.lat, p.lng]))
  if (hasStrikeLine && bc.strikeLine) bc.strikeLine.forEach(p => allPoints.push([p.lat, p.lng]))
  bc.auxiliaryLines.forEach(line => line.forEach(p => allPoints.push([p.lat, p.lng])))

  // === 多边形边界（纯轮廓, 无顶点标记） ===
  if (hasPolygon) {
    L.polygon(bc.polygon.map(p => [p.lat, p.lng] as L.LatLngTuple), {
      color: '#1890ff',
      fillColor: '#1890ff',
      fillOpacity: 0.06,
      weight: 1.5
    }).addTo(lg).bindPopup(
        `<div style="font-size:12px"><b>监测范围</b><br>顶点数: ${bc.polygon.length}</div>`
    )
  }

  // === 走向线（纯虚线, 无端点圆点标记） ===
  if (hasStrikeLine && bc.strikeLine) {
    const [a, b] = bc.strikeLine
    L.polyline([[a.lat, a.lng], [b.lat, b.lng]], {
      color: '#f56c6c',
      weight: 3,
      dashArray: '6 6'
    }).addTo(lg).bindPopup(`<div style="font-size:12px"><b>走向线</b></div>`)
  }

  // === 辅助线（纯虚线, 无方形端点标记） ===
  bc.auxiliaryLines.forEach((line, idx) => {
    if (line.length < 2) return
    L.polyline(line.map(p => [p.lat, p.lng] as L.LatLngTuple), {
      color: '#fa8c16',
      weight: 2,
      dashArray: '5 4'
    }).addTo(lg).bindPopup(
        `<div style="font-size:12px"><b>辅助线 ${idx + 1}</b><br>顶点数: ${line.length}</div>`
    )
  })

  // === 视频设备 marker（蓝色摄像机图标） ===
  props.videoDevices.forEach(d => {
    if (d.installLongitude == null || d.installLatitude == null) return
    allPoints.push([d.installLatitude, d.installLongitude])
    const videoIcon = L.divIcon({
      className: 'preview-video-marker',
      html: `<div style="background:#1677ff;color:#fff;width:16px;height:16px;border-radius:50%;display:flex;align-items:center;justify-content:center;font-size:10px;border:2px solid #fff;box-shadow:0 1px 3px rgba(0,0,0,.3)">📹</div>`,
      iconSize: [20, 20],
      iconAnchor: [10, 10]
    })
    L.marker([d.installLatitude, d.installLongitude], {icon: videoIcon, zIndexOffset: 600})
        .addTo(lg)
        .bindPopup(
            `<div class="hpv2-card">
              <div class="hpv2-header"><span class="hpv2-title">📹 ${d.deviceName}</span></div>
              <div class="hpv2-dash"></div>
              <div class="hpv2-body">
                <div class="hpv2-row single">
                  <div class="hpv2-cell full"><span class="hpv2-label">编号</span><span class="hpv2-val">${d.deviceCode}</span></div>
                </div>
                <div class="hpv2-dash"></div>
                <div class="hpv2-row single">
                  <div class="hpv2-cell full"><span class="hpv2-label">坐标</span><span class="hpv2-val">${d.installLongitude!.toFixed(6)}, ${d.installLatitude!.toFixed(6)}</span></div>
                </div>
              </div>
            </div>`
        )
  })

  // === 自适应边界：撑满显示区域 ===
  if (allPoints.length > 1) {
    map.fitBounds(L.latLngBounds(allPoints), {padding: [20, 20], maxZoom: 19})
  } else {
    map.setView([center.lat, center.lng], 15)
  }
}

/** 地图就绪 + 数据就绪 → 渲染 */
watch([leaflet.isReady, () => [props.initialValue, props.initialCenter, props.videoDevices]],
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
