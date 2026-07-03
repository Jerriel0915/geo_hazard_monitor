<template>
  <div class="map-point-picker">
    <div ref="containerRef" :style="{ height: heightStyle }" />
    <MapCoordInput
      v-if="!readonly && coordInputEnabled"
      mode="single"
      @parse-success="onCoordParsed"
    />
  </div>
</template>

<script setup lang="ts">
import {computed, ref, shallowRef, toRef, watch} from 'vue'
import L from 'leaflet'
import {useMapEditor} from '@/composables/useMapEditor'
import type {LatLng} from '@/lib/boundaryCoords'
import {getDeviceMapIconPath} from '@/utils/deviceIcon'
import type {BoundDevice} from '@/views/basic/composables/useHazardPointDeviceBind'
import MapCoordInput from './MapCoordInput.vue'

const props = withDefaults(defineProps<{
  modelValue: LatLng | null
  readonly?: boolean
  overlayPolygon?: LatLng[] | null
  /** 已绑定设备列表 — 在地图上叠加只读图钉, 与 MapBoundaryEditor 渲染一致 */
  boundDevices?: BoundDevice[]
  defaultCenter?: LatLng
  defaultZoom?: number
  coordInputEnabled?: boolean
  height?: string | number
}>(), {
  readonly: false,
  overlayPolygon: null,
  boundDevices: () => [],
  defaultZoom: 12,
  coordInputEnabled: true,
  height: 400
})

const emit = defineEmits<{
  'update:modelValue': [value: LatLng | null]
}>()

const heightStyle = computed(() => typeof props.height === 'number' ? `${props.height}px` : props.height)
const containerRef = ref<HTMLElement | null>(null)
const localPoint = ref<LatLng | null>(props.modelValue)

// overlayPolygon 用 toRef 包装成 Ref,让 useMapEditor 内的 watch 能响应 prop 变化
const overlayPolygonRef = toRef(props, 'overlayPolygon')

const editor = useMapEditor({
  container: containerRef,
  variant: 'point',
  initialPoint: props.modelValue,
  pointValue: localPoint,
  overlayPolygon: overlayPolygonRef,
  defaultCenter: props.defaultCenter,
  defaultZoom: props.defaultZoom,
  readonly: props.readonly
})

// 双向同步: 外部 modelValue → localPoint
watch(() => props.modelValue, v => {
  if (v && (!localPoint.value || v.lat !== localPoint.value.lat || v.lng !== localPoint.value.lng)) {
    localPoint.value = v
  }
}, { immediate: true })

// 双向同步: localPoint → emit('update:modelValue')
watch(localPoint, v => {
  if (v !== props.modelValue) emit('update:modelValue', v)
})

function onCoordParsed(result: LatLng | LatLng[]) {
  const pt = Array.isArray(result) ? result[0] : result
  if (pt) localPoint.value = pt
}

// ── 设备只读图层 (与 MapBoundaryEditor 渲染一致) ──
const deviceLayer = shallowRef<L.LayerGroup | null>(null)

function renderDeviceLayer(devices: BoundDevice[]) {
  const map = editor.mapRef.value
  if (!map) return
  if (!deviceLayer.value) {
    deviceLayer.value = L.layerGroup().addTo(map)
  }
  deviceLayer.value.clearLayers()
  devices.forEach(d => {
    if (d.installLongitude == null || d.installLatitude == null) return
    const icon = L.icon({
      iconUrl: getDeviceMapIconPath(d),
      iconSize: [28, 32],
      iconAnchor: [14, 16]
    })
    L.marker([d.installLatitude, d.installLongitude], {
      icon,
      interactive: true,
      zIndexOffset: 500
    }).addTo(deviceLayer.value!)
  })
}

watch(
  [() => props.boundDevices, editor.isReady],
  ([devices, ready]) => {
    if (!ready) return
    renderDeviceLayer(devices ?? [])
  },
  {immediate: true}
)

defineExpose({
  invalidate: editor.invalidate,
  focusToCoord: (lng: number, lat: number, zoom?: number) => editor.setView({ lat, lng }, zoom ?? 15)
})
</script>

<style scoped>
.map-point-picker { display: flex; flex-direction: column; gap: 8px; }
</style>
