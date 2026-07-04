<template>
  <div class="tool-button-wrapper">
    <button
      class="tool-btn"
      :class="{ active: showPanel || activeTool }"
      @click="togglePanel"
      title="地理工具箱"
    >
      <el-icon><TakeawayBox /></el-icon>
    </button>
    <div v-show="showPanel" class="tool-panel draw-panel">
      <div class="panel-title">地理工具箱</div>
      <div class="draw-tools">
        <button
          v-for="tool in tools"
          :key="tool.key"
          class="draw-tool-btn"
          :class="{ active: activeTool === tool.key }"
          :title="tool.label"
          @click="setTool(tool.key)"
        >
          <el-icon><component :is="tool.icon" /></el-icon>
          <span class="draw-tool-label">{{ tool.label }}</span>
        </button>
      </div>
      <!-- 颜色预设 — 始终可见，点击标注多边形时使用当前配色 -->
      <div class="color-settings">
        <div class="color-row">
          <span class="color-label">边框色</span>
          <input type="color" v-model="shapeOptions.color" class="color-input" />
          <span class="color-label" style="margin-left:12px">填充色</span>
          <input type="color" v-model="shapeOptions.fillColor" class="color-input" />
        </div>
        <div class="color-presets">
          <div
            v-for="(preset, i) in colorPresets"
            :key="i"
            class="color-preset"
            :class="{ selected: shapeOptions.color === preset.color }"
            :style="{ borderColor: preset.color, backgroundColor: preset.fill }"
            :title="preset.label"
            @click="applyPreset(preset)"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted, type Component } from 'vue'
import L from 'leaflet'
import 'leaflet-draw/dist/leaflet.draw.css'
import 'leaflet-draw'
import { DataLine, Grid, Location, Crop, Delete, TakeawayBox } from '@element-plus/icons-vue'

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const Draw = (L as any).Draw

interface ToolDef {
  key: string
  label: string
  icon: Component
}

const tools: ToolDef[] = [
  { key: 'distance', label: '测距', icon: DataLine },
  { key: 'area', label: '测面', icon: Grid },
  { key: 'marker', label: '标注点', icon: Location },
  { key: 'polygon', label: '标注多边形', icon: Crop },
  { key: 'clear', label: '清空', icon: Delete },
]

const props = defineProps<{
  mapInstance: L.Map | null
}>()

const showPanel = ref(false)
const activeTool = ref('')
let drawLayer: L.FeatureGroup | null = null
// eslint-disable-next-line @typescript-eslint/no-explicit-any
let currentHandler: any = null
// eslint-disable-next-line @typescript-eslint/no-explicit-any
let currentCreatedHandler: ((e: any) => void) | null = null

const shapeOptions = ref({
  color: '#1890ff',
  fillColor: '#1890ff',
  fillOpacity: 0.2,
  weight: 3
})

const colorPresets = [
  { label: '蓝色', color: '#1890ff', fill: 'rgba(24,144,255,0.2)' },
  { label: '红色', color: '#f5222d', fill: 'rgba(245,34,45,0.2)' },
  { label: '绿色', color: '#52c41a', fill: 'rgba(82,196,26,0.2)' },
  { label: '橙色', color: '#fa8c16', fill: 'rgba(250,140,22,0.2)' },
]

const togglePanel = () => {
  showPanel.value = !showPanel.value
  if (!showPanel.value) {
    // Closing panel without selecting a tool — keep current mode if any
  }
}

const applyPreset = (preset: typeof colorPresets[0]) => {
  shapeOptions.value.color = preset.color
  shapeOptions.value.fillColor = preset.color
}

const deactivateTool = () => {
  const map = props.mapInstance
  if (currentHandler) {
    currentHandler.disable()
    currentHandler = null
  }
  if (map && currentCreatedHandler) {
    map.off('draw:created', currentCreatedHandler)
    currentCreatedHandler = null
  }
  activeTool.value = ''
}

const setTool = (tool: string) => {
  const map = props.mapInstance
  if (!map) return

  // Clean up previous handler
  deactivateTool()

  // Clear tool
  if (tool === 'clear') {
    if (drawLayer) drawLayer.clearLayers()
    showPanel.value = false
    return
  }

  // Activate tool and close panel
  activeTool.value = tool
  showPanel.value = false

  const opts = {
    color: shapeOptions.value.color,
    fillColor: shapeOptions.value.fillColor,
    fillOpacity: shapeOptions.value.fillOpacity,
    weight: shapeOptions.value.weight,
  }

  switch (tool) {
    case 'distance':
      currentHandler = new Draw.Polyline(map, {
        shapeOptions: opts,
        showLength: true,
      })
      currentCreatedHandler = handleDistanceCreated
      map.on('draw:created', currentCreatedHandler)
      break
    case 'area':
      currentHandler = new Draw.Polygon(map, {
        shapeOptions: opts,
        showArea: true,
      })
      currentCreatedHandler = handleAreaCreated
      map.on('draw:created', currentCreatedHandler)
      break
    case 'marker':
      currentHandler = new Draw.Marker(map)
      currentCreatedHandler = handleMarkerCreated
      map.on('draw:created', currentCreatedHandler)
      break
    case 'polygon':
      currentHandler = new Draw.Polygon(map, {
        shapeOptions: opts,
      })
      currentCreatedHandler = handlePolygonCreated
      map.on('draw:created', currentCreatedHandler)
      break
  }

  if (currentHandler) currentHandler.enable()
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const handleDistanceCreated = (e: any) => {
  const layer = e.layer
  if (!drawLayer) return
  drawLayer.addLayer(layer)

  const latlngs = layer.getLatLngs() as L.LatLng[]
  if (latlngs.length < 2) return

  let totalDistance = 0
  for (let i = 1; i < latlngs.length; i++) {
    totalDistance += latlngs[i - 1].distanceTo(latlngs[i])
  }

  const lastPoint = latlngs[latlngs.length - 1]
  const label = totalDistance > 1000
    ? `总距离: ${(totalDistance / 1000).toFixed(2)} km`
    : `总距离: ${totalDistance.toFixed(1)} m`

  L.tooltip({ permanent: true, direction: 'top', className: 'draw-distance-tooltip' })
    .setLatLng(lastPoint)
    .setContent(label)
    .addTo(drawLayer)

  activeTool.value = ''
  currentHandler = null
  currentCreatedHandler = null
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const handleAreaCreated = (e: any) => {
  const layer = e.layer
  if (!drawLayer) return
  drawLayer.addLayer(layer)

  const latlngs = layer.getLatLngs()[0] as L.LatLng[]
  if (latlngs.length < 3) return

  // Calculate area using spherical geometry
  const areaM2 = L.GeometryUtil.geodesicArea(latlngs)
  const label = areaM2 > 1_000_000
    ? `面积: ${(areaM2 / 1_000_000).toFixed(2)} km²`
    : `面积: ${areaM2.toFixed(1)} m²`

  // Place tooltip at polygon centroid
  const centroidLat = latlngs.reduce((s, p) => s + p.lat, 0) / latlngs.length
  const centroidLng = latlngs.reduce((s, p) => s + p.lng, 0) / latlngs.length
  L.tooltip({ permanent: true, direction: 'center', className: 'draw-area-tooltip' })
    .setLatLng([centroidLat, centroidLng])
    .setContent(label)
    .addTo(drawLayer)

  activeTool.value = ''
  currentHandler = null
  currentCreatedHandler = null
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const handleMarkerCreated = (e: any) => {
  if (!drawLayer) return
  const marker = e.layer
  const defaultName = `标注 ${drawLayer.getLayers().length + 1}`
  marker.bindTooltip(defaultName, { permanent: true, direction: 'top' })

  // Editable popup
  const popup = document.createElement('div')
  popup.className = 'draw-marker-popup'
  const input = document.createElement('input')
  input.type = 'text'
  input.value = defaultName
  input.className = 'draw-marker-input'
  const btn = document.createElement('button')
  btn.textContent = '确定'
  btn.className = 'draw-marker-confirm'
  btn.addEventListener('click', () => {
    const name = input.value.trim() || defaultName
    marker.setTooltipContent(name)
    marker.closePopup()
  })
  popup.appendChild(input)
  popup.appendChild(btn)
  marker.bindPopup(popup as any)

  drawLayer.addLayer(marker)
  marker.openPopup()
  activeTool.value = ''
  currentHandler = null
  currentCreatedHandler = null
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const handlePolygonCreated = (e: any) => {
  if (!drawLayer) return
  drawLayer.addLayer(e.layer)
  activeTool.value = ''
  currentHandler = null
  currentCreatedHandler = null
}

// Watch mapInstance to create the draw layer
watch(() => props.mapInstance, (map) => {
  if (map && !drawLayer) {
    drawLayer = L.featureGroup().addTo(map)
  }
}, { immediate: true })

// Click outside to close draw panel
const handleClickOutside = (event: MouseEvent) => {
  const target = event.target as HTMLElement
  if (!target.closest('.draw-panel') && !target.closest('.tool-button-wrapper')) {
    showPanel.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
  deactivateTool()
  if (drawLayer && props.mapInstance) {
    props.mapInstance.removeLayer(drawLayer)
    drawLayer = null
  }
})
</script>

<style scoped>
/* 复用父级工具栏基础样式 */
.tool-button-wrapper {
  position: relative;
}

.tool-btn {
  width: 36px;
  height: 36px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: 8px;
  cursor: pointer;
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #606266;
  transition: all 0.2s ease;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.tool-btn:hover {
  background: rgba(24, 144, 255, 0.08);
  border-color: #1890ff;
  color: #1890ff;
}

.tool-btn.active {
  background: rgba(24, 144, 255, 0.1);
  border-color: #1890ff;
  color: #1890ff;
}

.tool-panel {
  position: absolute;
  right: calc(100% + 8px);
  top: 0;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: 10px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  padding: 12px;
  min-width: 220px;
  max-width: 320px;
}

.panel-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
}

/* 工具按钮网格 */
.draw-tools {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}

.draw-tool-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 6px 8px;
  border: 1px solid transparent;
  border-radius: 6px;
  background: transparent;
  cursor: pointer;
  color: #606266;
  font-size: 12px;
  transition: all 0.2s ease;
}

.draw-tool-btn:hover {
  background: rgba(24, 144, 255, 0.08);
  color: #1890ff;
}

.draw-tool-btn.active {
  background: rgba(24, 144, 255, 0.1);
  border-color: #1890ff;
  color: #1890ff;
}

.draw-tool-btn .el-icon {
  font-size: 18px;
}

.draw-tool-label {
  font-size: 11px;
  white-space: nowrap;
}

/* 颜色设置 */
.color-settings {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid #f0f0f0;
}

.color-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.color-label {
  font-size: 12px;
  color: #606266;
}

.color-input {
  width: 32px;
  height: 24px;
  padding: 1px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  cursor: pointer;
}

.color-presets {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}

.color-preset {
  width: 24px;
  height: 24px;
  border-radius: 4px;
  border-width: 2px;
  border-style: solid;
  cursor: pointer;
  transition: transform 0.15s ease;
}

.color-preset:hover {
  transform: scale(1.2);
}

.color-preset.selected {
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.4);
}
</style>

<style>
/* 全局样式 — leaflet 绘制提示框/弹窗 */
.draw-distance-tooltip {
  background: rgba(24, 144, 255, 0.9) !important;
  color: white !important;
  border: none !important;
  border-radius: 4px !important;
  padding: 4px 8px !important;
  font-size: 12px !important;
  font-weight: 500 !important;
}

.draw-distance-tooltip::before {
  border-top-color: rgba(24, 144, 255, 0.9) !important;
}

.draw-area-tooltip {
  background: rgba(82, 196, 26, 0.9) !important;
  color: white !important;
  border: none !important;
  border-radius: 4px !important;
  padding: 4px 8px !important;
  font-size: 12px !important;
  font-weight: 500 !important;
}

.draw-area-tooltip::before {
  border-top-color: rgba(82, 196, 26, 0.9) !important;
}

.draw-marker-popup {
  display: flex;
  gap: 6px;
  align-items: center;
}

.draw-marker-input {
  width: 120px;
  padding: 4px 8px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-size: 13px;
  outline: none;
}

.draw-marker-input:focus {
  border-color: #1890ff;
}

.draw-marker-confirm {
  padding: 4px 10px;
  background: #1890ff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
}

.draw-marker-confirm:hover {
  background: #40a9ff;
}
</style>
