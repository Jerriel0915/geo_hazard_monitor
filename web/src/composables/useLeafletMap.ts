import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import {nextTick, onBeforeUnmount, ref, type Ref, shallowRef, type ShallowRef, watch} from 'vue'
import type {LatLng} from '@/lib/boundaryCoords'

export let TIANDITU_KEY = '8dda07d4649c77efd0537a0ff0a1df13'

/** 从 sys_config 加载天地图 API Key，失败时保留旧值 */
export async function loadTiandituKey(): Promise<string> {
  try {
    const {default: request} = await import('@/utils/request')
    const res: any = await request.get('/system/config/configKey/tianditu_key')
    const val = res?.data ?? res?.msg ?? res
    if (val && typeof val === 'string' && val.trim()) {
      TIANDITU_KEY = val.trim()
    }
  } catch { /* 未配置时使用默认值 */ }
  return TIANDITU_KEY
}

export function buildTiandituUrl(layer: string, style: string): string {
  return `https://t0.tianditu.gov.cn/${layer}/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=${style}&STYLE=default&TILEMATRIXSET=w&FORMAT=tiles&TILEMATRIX={z}&TILEROW={y}&TILECOL={x}&tk=${TIANDITU_KEY}`
}

function addTiandituLayers(map: L.Map) {
  L.tileLayer(buildTiandituUrl('img_w', 'img'), {
    maxZoom: 18, minZoom: 3, attribution: '天地图'
  }).addTo(map)
  L.tileLayer(buildTiandituUrl('cia_w', 'cia'), {
    maxZoom: 18, minZoom: 3
  }).addTo(map)
}

export interface UseLeafletMapOptions {
  container: Ref<HTMLElement | null | undefined>
  center?: LatLng
  zoom?: number
  tianditu?: boolean
    doubleClickZoom?: boolean
}

export interface UseLeafletMapReturn {
  map: ShallowRef<L.Map | null>
  isReady: Ref<boolean>
  invalidate: () => void
  setView: (latLng: LatLng, zoom?: number) => void
  destroy: () => void
}

export function useLeafletMap(opts: UseLeafletMapOptions): UseLeafletMapReturn {
  const map = shallowRef<L.Map | null>(null)
  const isReady = ref(false)

  function initMap(el: HTMLElement) {
    const instance = L.map(el, {
      center: (opts.center ? [opts.center.lat, opts.center.lng] : [30.65, 104.10]) as L.LatLngExpression,
      zoom: opts.zoom ?? 12,
        zoomControl: true,
        doubleClickZoom: opts.doubleClickZoom ?? true
    })
    if (opts.tianditu !== false) addTiandituLayers(instance)
    map.value = instance
    isReady.value = true
  }

  function destroyMap() {
    if (!map.value) return
    map.value.off()
    map.value.remove()
    map.value = null
    isReady.value = false
  }

  // watch container DOM ref — auto init/destroy
  watch(opts.container, (el) => {
    if (el) {
      // destroy old instance before creating a new one on DOM swap
      if (map.value) destroyMap()
      initMap(el)
    } else if (map.value) {
      destroyMap()
    }
  }, { immediate: true, flush: 'post' })

  onBeforeUnmount(destroyMap)

  function invalidate() {
    if (!map.value) return
    nextTick(() => map.value?.invalidateSize())
  }

  function setView(p: LatLng, zoom?: number) {
    map.value?.setView([p.lat, p.lng], zoom ?? map.value.getZoom())
  }

  return { map, isReady, invalidate, setView, destroy: destroyMap }
}
