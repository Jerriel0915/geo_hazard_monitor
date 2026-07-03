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
import {computed, ref, toRef, watch, onBeforeUnmount} from 'vue'
import L from 'leaflet'
import {useMapEditor} from '@/composables/useMapEditor'
import type {LatLng} from '@/lib/boundaryCoords'
import MapCoordInput from './MapCoordInput.vue'

export interface DeviceMarker {
  lng: number
  lat: number
  name: string
  code: string
}

const props = withDefaults(defineProps<{
  modelValue: LatLng | null
  readonly?: boolean
  overlayPolygon?: LatLng[] | null
  deviceMarkers?: DeviceMarker[]
  defaultCenter?: LatLng
  defaultZoom?: number
  coordInputEnabled?: boolean
  height?: string | number
}>(), {
  readonly: false,
  overlayPolygon: null,
  deviceMarkers: () => [],
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

// ── 设备位置标记渲染 ──
let deviceLayer: L.LayerGroup | null = null

watch([() => props.deviceMarkers, editor.mapRef], ([markers, map]) => {
  deviceLayer?.remove()
  if (!map || !markers || markers.length === 0) return
  deviceLayer = L.layerGroup().addTo(map)
  markers.forEach(m => {
    const marker = L.circleMarker([m.lat, m.lng], {
      radius: 7,
      fillColor: '#3b82f6',
      fillOpacity: 0.7,
      color: '#ffffff',
      weight: 2,
      opacity: 1
    })
    marker.bindTooltip(`${m.name} (${m.code})`, {
      direction: 'top',
      offset: [0, -8]
    })
    deviceLayer!.addLayer(marker)
  })
}, {immediate: true})

onBeforeUnmount(() => {
  deviceLayer?.remove()
})

defineExpose({
  invalidate: editor.invalidate,
  focusToCoord: (lng: number, lat: number, zoom?: number) => editor.setView({ lat, lng }, zoom ?? 15)
})
</script>

<style scoped>
.map-point-picker { display: flex; flex-direction: column; gap: 8px; }
</style>
