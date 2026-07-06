<template>
  <!-- 地图上的取消浮层（工具激活时显示） -->
  <div v-if="activeTool" class="draw-active-overlay">
    <span class="draw-active-label">{{ currentToolLabel }}</span>
    <span class="draw-active-hint">在地图上操作，完成后自动退出</span>
    <button class="cancel-on-map-btn" @click="cancelTool">取消</button>
  </div>

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

      <!-- 坐标跳转 -->
      <div class="coord-jump">
        <div class="coord-row">
          <input v-model="jumpLng" type="text" class="coord-input" placeholder="经度"
                 @keyup.enter="jumpToCoord" />
          <span class="coord-sep">,</span>
          <input v-model="jumpLat" type="text" class="coord-input" placeholder="纬度"
                 @keyup.enter="jumpToCoord" />
          <button class="coord-jump-btn" @click="jumpToCoord" title="跳转到坐标">跳转</button>
        </div>
      </div>

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

      <!-- 颜色预设 -->
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
import { ref, computed, watch, onMounted, onUnmounted, type Component } from 'vue'
import L from 'leaflet'
import 'leaflet-draw/dist/leaflet.draw.css'
import 'leaflet-draw'
import { DataLine, Grid, Location, Crop, Delete, TakeawayBox, Back } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

// ── Leaflet Draw 中文化 ──
if ((L as any).drawLocal) {
  const dl = (L as any).drawLocal
  dl.draw = {
    toolbar: {
      actions: { title: '取消绘制', text: '取消' },
      finish: { title: '完成绘制', text: '完成' },
      undo: { title: '删除上一个点', text: '撤销上一个点' },
      buttons: { polyline: '绘制折线', polygon: '绘制多边形', rectangle: '绘制矩形', circle: '绘制圆形', marker: '放置标记', circlemarker: '放置圆形标记' },
    },
    handlers: {
      circle: { tooltip: { start: '点击并拖拽绘制圆形' }, radius: '半径' },
      circlemarker: { tooltip: { start: '点击放置圆形标记' } },
      marker: { tooltip: { start: '点击放置标记' } },
      polygon: { tooltip: { start: '点击开始绘制形状', cont: '点击继续绘制', end: '点击首个点闭合形状' } },
      polyline: { error: '<strong>错误:</strong> 边不能交叉!', tooltip: { start: '点击开始画线', cont: '点击继续画线', end: '点击最后一个点完成' } },
      rectangle: { tooltip: { start: '点击并拖拽绘制矩形' } },
      simpleshape: { tooltip: { end: '松开鼠标完成绘制' } },
    },
  }
  dl.edit = {
    toolbar: {
      actions: { save: { title: '保存更改', text: '保存' }, cancel: { title: '取消编辑，丢弃更改', text: '取消' }, clearAll: { title: '清除所有图层', text: '全部清除' } },
      buttons: { edit: '编辑图层', editDisabled: '没有可编辑的图层', remove: '删除图层', removeDisabled: '没有可删除的图层' },
    },
    handlers: {
      edit: { tooltip: { text: '拖拽控制点或标记来编辑', subtext: '点击"取消"丢弃更改' } },
      remove: { tooltip: { text: '点击要删除的元素' } },
    },
  }
}

const Draw = (L as any).Draw

interface ToolDef {
  key: string
  label: string
  icon: Component
}

const tools: ToolDef[] = [
  { key: 'distance', label: '测距', icon: DataLine },
  { key: 'area', label: '测面积', icon: Grid },
  { key: 'marker', label: '标注点', icon: Location },
  { key: 'polygon', label: '标注多边形', icon: Crop },
  { key: 'cancel', label: '取消', icon: Back },
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

const currentToolLabel = computed(() => {
  const t = tools.find(t => t.key === activeTool.value)
  return t?.label ?? ''
})

// ── 坐标跳转 ──
const jumpLng = ref('')
const jumpLat = ref('')

const jumpToCoord = () => {
  const map = props.mapInstance
  if (!map) return
  const lng = parseFloat(jumpLng.value.trim())
  const lat = parseFloat(jumpLat.value.trim())
  if (isNaN(lng) || isNaN(lat)) { ElMessage.warning('请输入有效的经纬度坐标'); return }
  if (lng < -180 || lng > 180 || lat < -90 || lat > 90) { ElMessage.warning('经纬度范围无效（经度 -180~180，纬度 -90~90）'); return }
  map.setView([lat, lng], 17, { animate: true })
  const marker = L.circleMarker([lat, lng], { radius: 6, color: '#f5222d', fillColor: '#f5222d', fillOpacity: 0.6, weight: 2 }).addTo(map)
  marker.bindTooltip(`${lng.toFixed(6)}, ${lat.toFixed(6)}`, { permanent: true, direction: 'top', className: 'coord-jump-tooltip' }).openTooltip()
  setTimeout(() => { marker.closeTooltip(); map.removeLayer(marker) }, 3000)
  showPanel.value = false
}

// ── 颜色 ──
const shapeOptions = ref({ color: '#1890ff', fillColor: '#1890ff', fillOpacity: 0.2, weight: 3 })
const colorPresets = [
  { label: '蓝色', color: '#1890ff', fill: 'rgba(24,144,255,0.2)' },
  { label: '红色', color: '#f5222d', fill: 'rgba(245,34,45,0.2)' },
  { label: '绿色', color: '#52c41a', fill: 'rgba(82,196,26,0.2)' },
  { label: '橙色', color: '#fa8c16', fill: 'rgba(250,140,22,0.2)' },
]

const togglePanel = () => { showPanel.value = !showPanel.value }
const applyPreset = (preset: typeof colorPresets[0]) => {
  shapeOptions.value.color = preset.color
  shapeOptions.value.fillColor = preset.color
}

const cancelTool = () => { deactivateTool(); ElMessage.info('已取消当前工具') }

const deactivateTool = () => {
  const map = props.mapInstance
  if (currentHandler) { currentHandler.disable(); currentHandler = null; currentCreatedHandler = null }
  if (map) { map.off('draw:created', handleDrawCreated) }
  activeTool.value = ''
  vertexCount = 0
}

let vertexCount = 0

const setTool = (tool: string) => {
  const map = props.mapInstance
  if (!map) return
  deactivateTool()
  if (tool === 'cancel') { showPanel.value = false; return }
  if (tool === 'clear') {
    if (drawLayer) drawLayer.clearLayers()
    ElMessage.success('已清空所有标注')
    showPanel.value = false
    return
  }

  activeTool.value = tool
  showPanel.value = false

  const opts = { color: shapeOptions.value.color, fillColor: shapeOptions.value.fillColor, fillOpacity: shapeOptions.value.fillOpacity, weight: shapeOptions.value.weight }

  switch (tool) {
    case 'distance':
      currentHandler = new Draw.Polyline(map, { shapeOptions: opts, metric: true, allowIntersection: false, zIndexOffset: 1000 })
      map.on('draw:created', handleDrawCreated)
      map.on('draw:drawvertex', handleDistVertex)
      break
    case 'area':
      currentHandler = new Draw.Polygon(map, { shapeOptions: { ...opts, fillOpacity: opts.fillOpacity * 0.5 }, metric: true, allowIntersection: false, zIndexOffset: 999 })
      map.on('draw:created', handleDrawCreated)
      break
    case 'marker':
      currentHandler = new Draw.Marker(map)
      map.on('draw:created', handleDrawCreated)
      break
    case 'polygon':
      currentHandler = new Draw.Polygon(map, { shapeOptions: opts, allowIntersection: false, zIndexOffset: 999 })
      map.on('draw:created', handleDrawCreated)
      break
  }
  if (currentHandler) currentHandler.enable()
}

// ── 测距：两点自动完成 ──
const handleDistVertex = () => {
  vertexCount++
  if (vertexCount >= 2 && currentHandler) {
    props.mapInstance?.off('draw:drawvertex', handleDistVertex)
    try { currentHandler.completeShape() } catch { /* ok */ }
    vertexCount = 0
  }
}

// ── 统一 draw:created 处理 ──
// eslint-disable-next-line @typescript-eslint/no-explicit-any
const handleDrawCreated = (e: any) => {
  const map = props.mapInstance
  if (!map) return
  map.off('draw:drawvertex', handleDistVertex)
  vertexCount = 0

  const layer = e.layer
  if (!drawLayer) return
  const type = e.layerType

  if (type === 'marker') {
    const defaultName = `标注 ${drawLayer.getLayers().length + 1}`
    const marker = e.layer as L.Marker
    marker.options.draggable = true
    marker.bindTooltip(deleteBtnHtml(defaultName), {
      permanent: false, direction: 'top', className: 'draw-marker-tooltip', opacity: 1,
    })
    // 拖动更新位置
    marker.on('dragend', () => {
      const ll = marker.getLatLng()
      marker.setTooltipContent(deleteBtnHtml(`标注 (${ll.lat.toFixed(5)}, ${ll.lng.toFixed(5)})`))
    })
    drawLayer.addLayer(marker)
  } else if (type === 'polyline') {
    drawLayer.addLayer(layer)
    const latlngs = layer.getLatLngs() as L.LatLng[]
    if (latlngs.length < 2) return
    let total = 0
    for (let i = 1; i < latlngs.length; i++) total += latlngs[i - 1].distanceTo(latlngs[i])
    const label = total > 1000 ? `${(total / 1000).toFixed(2)} km` : `${total.toFixed(1)} m`
    // 端点 - 淡色小圆
    latlngs.forEach(ll => {
      L.circleMarker(ll, { radius: 3, color: opts().color, fillColor: opts().color, fillOpacity: 0.4, weight: 1, interactive: true })
        .addTo(drawLayer!).bindTooltip(label, { direction: 'top', className: 'draw-measure-tooltip' })
    })
    const mid = latlngs[latlngs.length - 1]
    L.tooltip({ permanent: true, direction: 'top', className: 'draw-measure-tooltip' }).setLatLng(mid).setContent(label).addTo(drawLayer)
    // 可拖动端点
    makeVerticesDraggable(layer, latlngs, 'distance')
  } else if (type === 'polygon') {
    drawLayer.addLayer(layer)
    if (activeTool.value === 'area') {
      const rings = layer.getLatLngs()[0] as L.LatLng[]
      if (rings.length >= 3) {
        const areaM2 = L.GeometryUtil.geodesicArea(rings)
        const label = areaM2 > 1_000_000 ? `${(areaM2 / 1_000_000).toFixed(2)} km²` : `${areaM2.toFixed(1)} m²`
        const c = computeCentroid(rings)
        L.tooltip({ permanent: true, direction: 'center', className: 'draw-measure-tooltip' }).setLatLng(c).setContent(label).addTo(drawLayer)
        makeVerticesDraggable(layer, rings, 'area')
      }
    } else {
      // 标注多边形 - 可拖动顶点
      const rings = (layer.getLatLngs() as L.LatLng[][])[0]
      makeVerticesDraggable(layer, rings, 'polygon')
    }
  }
  deactivateTool()
}

const opts = () => ({ color: shapeOptions.value.color, fillColor: shapeOptions.value.fillColor, fillOpacity: shapeOptions.value.fillOpacity, weight: shapeOptions.value.weight })

// ── 顶点可拖动 ──
const shapeVertMarkers = new WeakMap<L.Polyline | L.Polygon, L.CircleMarker[]>()
let activeDrag: { vm: L.CircleMarker; shape: L.Polyline | L.Polygon; latlngs: L.LatLng[]; idx: number; mode: string } | null = null

function makeVerticesDraggable(shape: L.Polyline | L.Polygon, latlngs: L.LatLng[], mode: string) {
  const map = props.mapInstance
  if (!drawLayer || !map) return
  const markers: L.CircleMarker[] = []
  latlngs.forEach((ll, i) => {
    const vm = L.circleMarker(ll, { radius: 5, color: '#f5222d', fillColor: '#fff', fillOpacity: 1, weight: 2, interactive: true, bubblingMouseEvents: false }).addTo(drawLayer!)
    markers.push(vm)
    vm.on('mousedown', () => {
      activeDrag = { vm, shape, latlngs, idx: i, mode }
      map.dragging.disable()
      L.DomEvent.stopPropagation
    })
  })
  shapeVertMarkers.set(shape, markers)
}

// 全局拖拽事件（只绑定一次）
const onMapMouseMove = (e: L.LeafletMouseEvent) => {
  if (!activeDrag) return
  activeDrag.vm.setLatLng(e.latlng)
}

const onMapMouseUp = () => {
  if (!activeDrag) return
  syncShape(activeDrag.shape, activeDrag.latlngs, activeDrag.idx, activeDrag.vm.getLatLng(), activeDrag.mode)
  activeDrag = null
  props.mapInstance?.dragging.enable()
}

function syncShape(shape: L.Polyline | L.Polygon, latlngs: L.LatLng[], idx: number, newLl: L.LatLng, mode: string) {
  latlngs[idx] = newLl
  shape.setLatLngs(mode === 'polygon' || mode === 'area' ? [latlngs] : latlngs)

  // 更新顶点 CircleMarker 位置
  const vertMarkers = shapeVertMarkers.get(shape)
  if (vertMarkers) {
    latlngs.forEach((ll, i) => {
      if (vertMarkers[i]) vertMarkers[i].setLatLng(ll)
    })
  }

  // 清除旧 tooltip（避免残留）
  clearMeasureTooltips()

  if (mode === 'distance') {
    let total = 0
    for (let i = 1; i < latlngs.length; i++) total += latlngs[i - 1].distanceTo(latlngs[i])
    const label = total > 1000 ? `${(total / 1000).toFixed(2)} km` : `${total.toFixed(1)} m`
    shape.unbindTooltip()
    const mid = latlngs[latlngs.length - 1]
    L.tooltip({ permanent: true, direction: 'top', className: 'draw-measure-tooltip' }).setLatLng(mid).setContent(label).addTo(drawLayer!)
    // 更新 CircleMarker 端点 tooltip
    if (drawLayer) {
      const endpoints = latlngs.map(ll => ll)
      drawLayer.eachLayer(l => {
        if (l instanceof L.CircleMarker && l.options.radius === 3) {
          l.unbindTooltip()
          l.bindTooltip(label, { direction: 'top', className: 'draw-measure-tooltip' })
        }
      })
    }
  } else if (mode === 'area') {
    const rings = (shape.getLatLngs() as L.LatLng[][])[0]
    const areaM2 = L.GeometryUtil.geodesicArea(rings)
    const label = areaM2 > 1_000_000 ? `${(areaM2 / 1_000_000).toFixed(2)} km²` : `${areaM2.toFixed(1)} m²`
    shape.unbindTooltip()
    const c = computeCentroid(rings)
    L.tooltip({ permanent: true, direction: 'center', className: 'draw-measure-tooltip' }).setLatLng(c).setContent(label).addTo(drawLayer!)
  }
}

/** 移除 drawLayer 中所有旧的测量 tooltip */
function clearMeasureTooltips() {
  if (!drawLayer) return
  drawLayer.eachLayer(l => {
    if (l instanceof L.Tooltip && (l.options as any).permanent) {
      drawLayer!.removeLayer(l)
    }
  })
}

function computeCentroid(rings: L.LatLng[]): L.LatLng {
  return L.latLng(
    rings.reduce((s, p) => s + p.lat, 0) / rings.length,
    rings.reduce((s, p) => s + p.lng, 0) / rings.length,
  )
}

// ── 标注点删除按钮 HTML ──
function deleteBtnHtml(name: string) {
  return `<span style="display:inline-flex;align-items:center;gap:4px">${name}<span class="marker-delete-x" style="cursor:pointer;color:#ff4d4f;font-weight:bold;font-size:14px;line-height:1" data-action="delete">×</span></span>`
}

// 全局删除委托
const onTooltipClick = (e: MouseEvent) => {
  const target = e.target as HTMLElement
  if (target.classList.contains('marker-delete-x') || (target as any).dataset?.action === 'delete') {
    if (!drawLayer || !props.mapInstance) return
    // find marker by traversing up to tooltip's latlng
    drawLayer.eachLayer(layer => {
      if (layer instanceof L.Marker) {
        const tip = layer.getTooltip()
        if (tip && (tip as any)._content?.includes?.('marker-delete-x')) {
          const el = tip.getElement()
          if (el && el.contains(target)) {
            props.mapInstance!.closeTooltip()
            drawLayer!.removeLayer(layer)
            ElMessage.success('已删除标注点')
          }
        }
      }
    })
  }
}

onMounted(() => { document.addEventListener('click', onTooltipClick) })
onUnmounted(() => { document.removeEventListener('click', onTooltipClick) })

// ── ESC 取消 ──
const handleKeyDown = (e: KeyboardEvent) => {
  if (e.key === 'Escape' && activeTool.value) { e.preventDefault(); cancelTool() }
}

onMounted(() => { document.addEventListener('keydown', handleKeyDown) })
onUnmounted(() => { document.removeEventListener('keydown', handleKeyDown) })

// Click outside to close panel
const handleClickOutside = (event: MouseEvent) => {
  const target = event.target as HTMLElement
  if (!target.closest('.draw-panel') && !target.closest('.tool-button-wrapper')) { showPanel.value = false }
}

onMounted(() => { document.addEventListener('click', handleClickOutside) })
onUnmounted(() => { document.removeEventListener('click', handleClickOutside) })

// Watch map — 创建 drawLayer + 绑定全局拖拽
watch(() => props.mapInstance, (map, _old, onCleanup) => {
  if (map && !drawLayer) drawLayer = L.featureGroup().addTo(map)
  if (map) { map.on('mousemove', onMapMouseMove); map.on('mouseup', onMapMouseUp) }
  onCleanup(() => {
    deactivateTool()
    if (map) { map.off('mousemove', onMapMouseMove); map.off('mouseup', onMapMouseUp) }
  })
}, { immediate: true })

onUnmounted(() => {
  deactivateTool()
  activeDrag = null
  if (drawLayer && props.mapInstance) {
    props.mapInstance.off('mousemove', onMapMouseMove)
    props.mapInstance.off('mouseup', onMapMouseUp)
    props.mapInstance.removeLayer(drawLayer)
    drawLayer = null
  }
})
</script>

<style scoped>
.tool-button-wrapper { position: relative; }

.tool-btn {
  width: 36px; height: 36px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: 8px;
  cursor: pointer; font-size: 16px;
  display: flex; align-items: center; justify-content: center;
  color: #606266; transition: all 0.2s ease;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.tool-btn:hover { background: rgba(24, 144, 255, 0.08); border-color: #1890ff; color: #1890ff; }
.tool-btn.active { background: rgba(24, 144, 255, 0.1); border-color: #1890ff; color: #1890ff; }

.tool-panel {
  position: absolute; right: calc(100% + 8px); top: 0;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: 10px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  padding: 12px; min-width: 260px; max-width: 320px;
}

.panel-title { font-size: 14px; font-weight: 600; color: #303133; margin-bottom: 10px; padding-bottom: 8px; border-bottom: 1px solid #f0f0f0; }

.coord-jump { margin-bottom: 10px; padding-bottom: 10px; border-bottom: 1px solid #f0f0f0; }
.coord-row { display: flex; align-items: center; gap: 6px; }
.coord-input { width: 100px; padding: 5px 8px; border: 1px solid #dcdfe6; border-radius: 4px; font-size: 12px; outline: none; font-family: monospace; }
.coord-input:focus { border-color: #1890ff; box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.1); }
.coord-sep { color: #909399; font-size: 14px; }
.coord-jump-btn { padding: 5px 12px; background: #1890ff; color: white; border: none; border-radius: 4px; cursor: pointer; font-size: 12px; white-space: nowrap; }
.coord-jump-btn:hover { background: #40a9ff; }

.draw-tools { display: flex; gap: 4px; flex-wrap: wrap; }
.draw-tool-btn { display: flex; flex-direction: column; align-items: center; gap: 2px; padding: 6px 8px; border: 1px solid transparent; border-radius: 6px; background: transparent; cursor: pointer; color: #606266; font-size: 12px; transition: all 0.2s ease; }
.draw-tool-btn:hover { background: rgba(24, 144, 255, 0.08); color: #1890ff; }
.draw-tool-btn.active { background: rgba(24, 144, 255, 0.1); border-color: #1890ff; color: #1890ff; }
.draw-tool-btn .el-icon { font-size: 18px; }
.draw-tool-label { font-size: 11px; white-space: nowrap; }

.color-settings { margin-top: 10px; padding-top: 10px; border-top: 1px solid #f0f0f0; }
.color-row { display: flex; align-items: center; gap: 8px; }
.color-label { font-size: 12px; color: #606266; }
.color-input { width: 32px; height: 24px; padding: 1px; border: 1px solid #dcdfe6; border-radius: 4px; cursor: pointer; }
.color-presets { display: flex; gap: 8px; margin-top: 8px; }
.color-preset { width: 24px; height: 24px; border-radius: 4px; border-width: 2px; border-style: solid; cursor: pointer; transition: transform 0.15s ease; }
.color-preset:hover { transform: scale(1.2); }
.color-preset.selected { box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.4); }

.draw-active-overlay {
  position: fixed; top: 90px; left: 50%; transform: translateX(-50%); z-index: 1001;
  display: flex; align-items: center; gap: 10px;
  padding: 8px 16px;
  background: rgba(255, 255, 255, 0.95); backdrop-filter: blur(8px);
  border: 1px solid #91d5ff; border-radius: 8px;
  box-shadow: 0 2px 12px rgba(24, 144, 255, 0.15);
  font-size: 13px;
}

.draw-active-label { font-weight: 600; color: #0050b3; }
.draw-active-hint { color: #606266; font-size: 12px; }
.cancel-on-map-btn { padding: 4px 14px; background: #fff; color: #f5222d; border: 1px solid #ffa39e; border-radius: 4px; cursor: pointer; font-size: 12px; font-weight: 500; }
.cancel-on-map-btn:hover { background: #fff1f0; }
</style>

<style>
.draw-measure-tooltip {
  background: rgba(24, 144, 255, 0.88) !important; color: white !important;
  border: none !important; border-radius: 4px !important;
  padding: 3px 8px !important; font-size: 12px !important; font-weight: 500 !important;
}
.draw-measure-tooltip::before { border-top-color: rgba(24, 144, 255, 0.88) !important; }

.draw-marker-tooltip {
  background: rgba(255, 255, 255, 0.95) !important; color: #303133 !important;
  border: 1px solid #e8e8e8 !important; border-radius: 6px !important;
  padding: 4px 10px !important; font-size: 12px !important;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1) !important;
}
.draw-marker-tooltip::before { border-top-color: #e8e8e8 !important; }

.coord-jump-tooltip {
  background: rgba(245, 34, 45, 0.9) !important; color: white !important;
  border: none !important; border-radius: 4px !important;
  padding: 4px 8px !important; font-size: 12px !important;
}
.coord-jump-tooltip::before { border-top-color: rgba(245, 34, 45, 0.9) !important; }

.marker-delete-x:hover { color: #ff0000 !important; }
</style>
