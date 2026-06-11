import { ref, computed, watch, shallowRef, onBeforeUnmount, type Ref, type ShallowRef, type ComputedRef } from 'vue'
import L, { type Map as LMap } from 'leaflet'
import type { LatLng, BoundaryCoords } from '@/lib/boundaryCoords'
import { centroid, strikeAngle as computeStrikeAngle } from '@/lib/boundaryCoords'
import { useLeafletMap } from './useLeafletMap'

export type EditorMode = 'view' | 'edit'
export type EditorTool = null | 'polygon' | 'strike' | 'aux'
export type MapEditorVariant = 'boundary' | 'point'

export interface VertexId { kind: 'polygon-vertex'; index: number }
export interface StrikeId { kind: 'strike-endpoint'; index: 0 | 1 }
export interface AuxId { kind: 'aux-line'; index: number }
export type SelectableId = VertexId | StrikeId | AuxId

export interface EditorSnapshot {
  mode: EditorMode
  polygon: LatLng[]
  strikeLine: [LatLng, LatLng] | null
  auxiliaryLines: LatLng[][]
  center: LatLng | null
  manualCenterLocked: boolean
  strikeAngle: number | null
}

export interface VertexHandle {
  id: SelectableId
  position: LatLng
}

export interface UseMapEditorOptions {
  container: Ref<HTMLElement | null | undefined>
  variant: MapEditorVariant
  initialBoundary?: BoundaryCoords | null
  initialCenter?: LatLng | null
  initialPoint?: LatLng | null
  pointValue?: Ref<LatLng | null>
  overlayPolygon?: LatLng[] | null
  defaultCenter?: LatLng
  defaultZoom?: number
  readonly?: boolean
  tianditu?: boolean
  onChange?: (snapshot: EditorSnapshot) => void
  onCenterChange?: (center: LatLng | null) => void
}

export interface UseMapEditorReturn {
  mapRef: ShallowRef<LMap | null>
  isReady: Ref<boolean>
  containerRef: Ref<HTMLElement | null | undefined>
  invalidate: () => void
  setView: (p: LatLng, zoom?: number) => void
  destroy: () => void

  mode: Ref<EditorMode>
  tool: Ref<EditorTool>
  canEdit: ComputedRef<boolean>
  canSave: ComputedRef<boolean>

  polygon: Ref<LatLng[]>
  strikeLine: Ref<[LatLng, LatLng] | null>
  auxiliaryLines: Ref<LatLng[][]>
  center: Ref<LatLng | null>
  manualCenterLocked: Ref<boolean>
  strikeAngle: ComputedRef<number | null>

  selectedId: Ref<SelectableId | null>
  selectedVertex: ComputedRef<VertexHandle | null>

  enterEdit: () => void
  exitEdit: () => void
  toggleEdit: () => void
  activateTool: (t: EditorTool) => void
  cancelTool: () => void

  addVertex: (p: LatLng) => void
  moveVertex: (id: VertexId, p: LatLng) => void
  removeVertex: (id: VertexId) => void
  insertVertexAfter: (afterId: VertexId, p: LatLng) => void

  setStrike: (a: LatLng, b: LatLng) => void
  moveStrikeEndpoint: (idx: 0 | 1, p: LatLng) => void
  removeStrike: () => void

  addAuxLine: (points: LatLng[]) => void
  moveAuxPoint: (lineId: number, pointId: number, p: LatLng) => void
  removeAuxLine: (lineId: number) => void

  setCenter: (p: LatLng, manual?: boolean) => void
  moveCenter: (p: LatLng) => void
  resetCenter: () => void

  select: (id: SelectableId | null) => void
  clearSelection: () => void
  removeSelected: () => void

  clearAll: () => void
  snapshot: () => EditorSnapshot

  // P3: drawing visual feedback
  mouseLatLng: Ref<LatLng | null>
  ghostGroup: ShallowRef<L.LayerGroup | null>
  clearGhost: () => void
  // P4: strike endpoint drag protection
  draggingStrikeIndex: Ref<0 | 1 | null>
  // P5: aux point drag protection
  draggingAuxKey: Ref<{ line: number; point: number } | null>
}

export function useMapEditor(options: UseMapEditorOptions): UseMapEditorReturn {
  // ── 状态 ──
  const mode = ref<EditorMode>('view')
  const tool = ref<EditorTool>(null)
  const polygon = ref<LatLng[]>(options.initialBoundary?.polygon?.slice() ?? [])
  const strikeLine = ref<[LatLng, LatLng] | null>(options.initialBoundary?.strikeLine ?? null)
  const auxiliaryLines = ref<LatLng[][]>(options.initialBoundary?.auxiliaryLines?.map(l => l.slice()) ?? [])
  const center = ref<LatLng | null>(options.initialCenter ? { ...options.initialCenter } : null)
  const manualCenterLocked = ref<boolean>(false)
  const selectedId = ref<SelectableId | null>(null)

  // ── Point variant v-model bridge ──
  const localPoint = ref<LatLng | null>(options.initialPoint ?? options.pointValue?.value ?? null)
  if (options.pointValue) {
    watch(options.pointValue, v => { if (v) localPoint.value = v }, { immediate: true })
  }

  // ── 派生 ──
  const canEdit = computed(() => !options.readonly)
  const canSave = computed(() => {
    if (options.variant === 'point') return !!localPoint.value
    return polygon.value.length >= 3
  })
  const strikeAngle = computed<number | null>(() =>
    strikeLine.value ? computeStrikeAngle(strikeLine.value) : null
  )
  const selectedVertex = computed<VertexHandle | null>(() => {
    if (!selectedId.value) return null
    const id = selectedId.value
    if (id.kind === 'polygon-vertex') {
      const p = polygon.value[id.index]
      return p ? { id, position: p } : null
    }
    if (id.kind === 'strike-endpoint') {
      const p = strikeLine.value?.[id.index]
      return p ? { id, position: p } : null
    }
    return null
  })

  // ── 自动派生: polygon 变化 → 中心点 (除非手动锁定) ──
  watch(polygon, () => {
    if (polygon.value.length < 3) {
      if (!manualCenterLocked.value) center.value = null
    } else {
      if (!manualCenterLocked.value) {
        const c = centroid(polygon.value)
        if (c) center.value = c
      }
    }
  }, { deep: true })

  // ── 变更通知 ──
  if (options.onChange) {
    const cb = options.onChange
    watch(
      [() => polygon.value, () => strikeLine.value, () => auxiliaryLines.value, () => mode.value, () => tool.value, () => manualCenterLocked.value],
      () => cb(snapshot()),
      { deep: true }
    )
  }
  if (options.onCenterChange) {
    const cb = options.onCenterChange
    watch(() => center.value, c => cb(c ? { ...c } : null), { immediate: true })
  }

  // ── 地图底层 ──
  const leaflet = useLeafletMap({
    container: options.container,
    center: options.initialCenter ?? options.initialPoint ?? options.defaultCenter ?? { lat: 30.67, lng: 104.06 },
    zoom: options.defaultZoom ?? 14,
    tianditu: options.tianditu
  })

  // ── Leaflet layers (managed by composable) ──
  const polygonLayer = shallowRef<L.Polygon | null>(null)
  const strikeLayer = shallowRef<L.Polyline | null>(null)
  const auxLayers = shallowRef<L.Polyline[]>([])
  const vertexMarkers = shallowRef<L.Marker[]>([])
  const centerMarker = shallowRef<L.Marker | null>(null)
  const overlayLayer = shallowRef<L.GeoJSON | null>(null)
  const pointMarker = shallowRef<L.Marker | null>(null)
  // index of the vertex currently being dragged; render skips it
  const draggingVertexIndex = ref<number | null>(null)
  const draggingCenter = ref<boolean>(false)
  const draggingPoint = ref<boolean>(false)
  // P3: drawing visual feedback
  const mouseLatLng = ref<LatLng | null>(null)
  const ghostGroup = shallowRef<L.LayerGroup | null>(null)
  // P4: strike endpoint markers (draggable in edit mode)
  const strikeEndpointMarkers = shallowRef<(L.Marker | null)[]>([])
  const draggingStrikeIndex = ref<0 | 1 | null>(null)
  // P5: aux point markers (draggable in edit mode)
  const auxPointMarkers = shallowRef<(L.Marker | null)[][]>([])
  const draggingAuxKey = ref<{ line: number; point: number } | null>(null)

  // ── Render effect: redraw layers on data change ──
  const renderTimer = { id: null as number | null }
  function scheduleRender() {
    if (renderTimer.id !== null) cancelAnimationFrame(renderTimer.id)
    renderTimer.id = requestAnimationFrame(() => {
      renderTimer.id = null
      render()
    })
  }

  function vertexHtml(num: number, selected: boolean, editable: boolean): string {
    const ring = selected ? 'box-shadow:0 0 0 4px #ef4444aa;' :
                  editable ? 'box-shadow:0 0 0 3px #f59e0b80;' : ''
    return `<div style="background:#67C23A;color:#fff;width:28px;height:28px;border-radius:50%;display:flex;align-items:center;justify-content:center;font-size:13px;font-weight:bold;border:2px solid white;${ring}">${num}</div>`
  }

  function render() {
    const map = leaflet.map.value
    if (!map) return

    // polygon
    if (polygon.value.length >= 3) {
      const latlngs: L.LatLngExpression[] = polygon.value.map(p => [p.lat, p.lng])
      if (polygonLayer.value) {
        polygonLayer.value.setLatLngs(latlngs)
      } else {
        polygonLayer.value = L.polygon(latlngs, {
          color: '#1890ff', fillColor: '#1890ff', fillOpacity: 0.15, weight: 2,
          dashArray: mode.value === 'edit' ? '4 2' : undefined
        }).addTo(map)
      }
    } else if (polygonLayer.value) {
      polygonLayer.value.remove()
      polygonLayer.value = null
    }

    // vertex markers — incremental update so we don't kill the one being dragged
    // 1. Remove excess markers (when polygon shrinks)
    while (vertexMarkers.value.length > polygon.value.length) {
      const m = vertexMarkers.value.pop()!
      m.remove()
    }
    // 2. For each polygon vertex, ensure a marker exists at the right position
    vertexMarkers.value = polygon.value.map((p, i) => {
      // Skip the marker that's currently being dragged — its position is
      // managed by Leaflet's drag handler. Removing/recreating it
      // would interrupt the drag.
      if (i === draggingVertexIndex.value) return vertexMarkers.value[i]

      const existing = vertexMarkers.value[i]
      const isSelected = selectedId.value?.kind === 'polygon-vertex' && selectedId.value.index === i
      const newIcon = L.divIcon({
        className: 'vertex-marker',
        html: vertexHtml(i + 1, isSelected, mode.value === 'edit'),
        iconSize: [28, 28],
        iconAnchor: [14, 14]
      })
      if (existing) {
        // Update in place: position + icon (icon for selection/mode halo)
        existing.setLatLng([p.lat, p.lng])
        existing.setIcon(newIcon)
        return existing
      }
      // Create new
      const marker = L.marker([p.lat, p.lng], {
        icon: newIcon,
        // Always draggable; drag handler checks mode
        draggable: true
      }).addTo(map)
      marker.on('dragstart', () => { draggingVertexIndex.value = i })
      marker.on('drag', (e: any) => {
        if (mode.value !== 'edit' || !canEdit.value) {
          e.target.setLatLng([polygon.value[i].lat, polygon.value[i].lng])
          return
        }
        const ll = e.target.getLatLng()
        moveVertex({ kind: 'polygon-vertex', index: i }, { lat: ll.lat, lng: ll.lng })
      })
      marker.on('dragend', () => {
        draggingVertexIndex.value = null
      })
      marker.on('click', (e: any) => {
        L.DomEvent.stopPropagation(e)
        if (mode.value === 'edit') select({ kind: 'polygon-vertex', index: i })
      })
      return marker
    })

    // strike line + endpoint markers
    if (strikeLine.value) {
      const [a, b] = strikeLine.value
      const isLineSelected = selectedId.value?.kind === 'strike-endpoint'
      if (strikeLayer.value) {
        strikeLayer.value.setLatLngs([[a.lat, a.lng], [b.lat, b.lng]])
        strikeLayer.value.setStyle({ weight: isLineSelected ? 5 : 3 })
      } else {
        strikeLayer.value = L.polyline([[a.lat, a.lng], [b.lat, b.lng]], {
          color: '#f56c6c', weight: isLineSelected ? 5 : 3
        }).addTo(map)
        strikeLayer.value.on('click', () => select({ kind: 'strike-endpoint', index: 0 }))
      }
      // P4: draggable endpoint markers
      strikeEndpointMarkers.value.forEach(m => { if (m) m.remove() })
      strikeEndpointMarkers.value = strikeLine.value.map((pt, i) => {
        if (i === draggingStrikeIndex.value) return strikeEndpointMarkers.value[i] ?? null
        const isSelected = isLineSelected && selectedId.value!.index === i
        return L.marker([pt.lat, pt.lng], {
          icon: L.divIcon({
            className: 'strike-endpoint-marker',
            html: `<div style="background:#f56c6c;color:#fff;width:18px;height:18px;border-radius:50%;display:flex;align-items:center;justify-content:center;font-size:10px;font-weight:bold;border:2px solid #fff;${isSelected ? 'box-shadow:0 0 0 4px #ef4444aa;' : 'box-shadow:0 0 0 3px #f59e0b80;'}">${i + 1}</div>`,
            iconSize: [18, 18],
            iconAnchor: [9, 9]
          }),
          draggable: mode.value === 'edit' && canEdit.value
        }).addTo(map)
      })
      strikeEndpointMarkers.value.forEach((m, i) => {
        if (!m) return
        m.on('dragstart', () => { draggingStrikeIndex.value = i as 0 | 1 })
        m.on('drag', (e: any) => {
          if (mode.value !== 'edit' || !canEdit.value) {
            e.target.setLatLng([strikeLine.value![i].lat, strikeLine.value![i].lng])
            return
          }
          const ll = e.target.getLatLng()
          moveStrikeEndpoint(i as 0 | 1, { lat: ll.lat, lng: ll.lng })
        })
        m.on('dragend', () => { draggingStrikeIndex.value = null })
        m.on('click', (e: any) => {
          L.DomEvent.stopPropagation(e)
          if (mode.value === 'edit') select({ kind: 'strike-endpoint', index: i as 0 | 1 })
        })
      })
    } else if (strikeLayer.value) {
      strikeLayer.value.remove()
      strikeLayer.value = null
      strikeEndpointMarkers.value.forEach(m => { if (m) m.remove() })
      strikeEndpointMarkers.value = []
    }

    // aux lines + per-vertex markers
    auxLayers.value.forEach(l => l.remove())
    auxPointMarkers.value.forEach(row => row.forEach(m => { if (m) m.remove() }))
    auxLayers.value = []
    auxPointMarkers.value = []
    auxiliaryLines.value.forEach((line, lineIdx) => {
      const isLineSelected = selectedId.value?.kind === 'aux-line' && selectedId.value.index === lineIdx
      const pl = L.polyline(line.map(p => [p.lat, p.lng] as L.LatLngExpression), {
        color: '#fa8c16', weight: isLineSelected ? 3 : 2, dashArray: '5 4'
      }).addTo(map)
      pl.on('click', () => select({ kind: 'aux-line', index: lineIdx }))
      auxLayers.value.push(pl)
      // P5: per-vertex markers (drag in edit mode)
      const row: (L.Marker | null)[] = line.map((pt, ptIdx) => {
        if (draggingAuxKey.value &&
            draggingAuxKey.value.line === lineIdx &&
            draggingAuxKey.value.point === ptIdx) {
          return auxPointMarkers.value[lineIdx]?.[ptIdx] ?? null
        }
        return L.marker([pt.lat, pt.lng], {
          icon: L.divIcon({
            className: 'aux-point-marker',
            html: `<div style="background:#fa8c16;color:#fff;width:16px;height:16px;border-radius:2px;display:flex;align-items:center;justify-content:center;font-size:9px;font-weight:bold;border:2px solid #fff;${isLineSelected ? 'box-shadow:0 0 0 4px #ef4444aa;' : 'box-shadow:0 0 0 3px #f59e0b80;'}"></div>`,
            iconSize: [16, 16],
            iconAnchor: [8, 8]
          }),
          draggable: mode.value === 'edit' && canEdit.value
        }).addTo(map)
      })
      row.forEach((m, ptIdx) => {
        if (!m) return
        m.on('dragstart', () => { draggingAuxKey.value = { line: lineIdx, point: ptIdx } })
        m.on('drag', (e: any) => {
          if (mode.value !== 'edit' || !canEdit.value) {
            const cur = auxiliaryLines.value[lineIdx][ptIdx]
            e.target.setLatLng([cur.lat, cur.lng])
            return
          }
          const ll = e.target.getLatLng()
          moveAuxPoint(lineIdx, ptIdx, { lat: ll.lat, lng: ll.lng })
        })
        m.on('dragend', () => { draggingAuxKey.value = null })
        m.on('click', (e: any) => {
          L.DomEvent.stopPropagation(e)
          if (mode.value === 'edit') select({ kind: 'aux-line', index: lineIdx })
        })
      })
      auxPointMarkers.value.push(row)
    })

    // center marker — update in place; skip when being dragged
    if (center.value && !draggingCenter.value) {
      if (centerMarker.value) {
        centerMarker.value.setLatLng([center.value.lat, center.value.lng])
      } else {
        centerMarker.value = L.marker([center.value.lat, center.value.lng], {
          icon: L.divIcon({
            className: '',
            html: '<div style="background:#1890ff;color:#fff;padding:4px 8px;border-radius:50%;font-size:12px;width:30px;height:30px;display:flex;align-items:center;justify-content:center">★</div>',
            iconSize: [30, 30], iconAnchor: [15, 15]
          }),
          draggable: true
        }).addTo(map)
        centerMarker.value.on('dragstart', () => { draggingCenter.value = true })
        centerMarker.value.on('drag', (e: any) => {
          if (mode.value !== 'edit' || !canEdit.value) {
            e.target.setLatLng([center.value!.lat, center.value!.lng])
            return
          }
          const ll = e.target.getLatLng()
          moveCenter({ lat: ll.lat, lng: ll.lng })
        })
        centerMarker.value.on('dragend', () => { draggingCenter.value = false })
        centerMarker.value.on('click', (e: any) => L.DomEvent.stopPropagation(e))
      }
    } else if (!center.value && centerMarker.value) {
      centerMarker.value.remove()
      centerMarker.value = null
    }

    // point variant marker (for MapPointPicker) — update in place
    if (options.variant === 'point' && !draggingPoint.value) {
      if (localPoint.value) {
        if (pointMarker.value) {
          pointMarker.value.setLatLng([localPoint.value.lat, localPoint.value.lng])
        } else {
          pointMarker.value = L.marker([localPoint.value.lat, localPoint.value.lng], {
            icon: L.icon({
              iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
              iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
              shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
              iconSize: [25, 41],
              iconAnchor: [12, 41]
            }),
            draggable: !options.readonly
          }).addTo(map)
          pointMarker.value.on('dragstart', () => { draggingPoint.value = true })
          pointMarker.value.on('dragend', (e: any) => {
            draggingPoint.value = false
            const ll = e.target.getLatLng()
            localPoint.value = { lat: ll.lat, lng: ll.lng }
          })
        }
      } else if (pointMarker.value) {
        pointMarker.value.remove()
        pointMarker.value = null
      }
    }
  }

  watch(
    [leaflet.isReady, () => polygon.value, () => strikeLine.value, () => auxiliaryLines.value, () => center.value, () => mode.value, () => selectedId.value, () => localPoint.value],
    () => scheduleRender(),
    { deep: true }
  )

  // ── P3: drawing ghost (mouse follower + preview line) ──
  function clearGhost() {
    if (ghostGroup.value) {
      ghostGroup.value.remove()
      ghostGroup.value = null
    }
  }

  function renderGhost() {
    const map = leaflet.map.value
    if (!map) return
    clearGhost()
    if (mode.value !== 'edit' || !tool.value || !mouseLatLng.value) return
    const ml = mouseLatLng.value
    const t = tool.value
    const layers: L.Layer[] = []

    // Cursor ghost dot — blue ring, white fill
    layers.push(L.circleMarker([ml.lat, ml.lng], {
      radius: 6, color: '#3b82f6', fillColor: '#fff', fillOpacity: 1, weight: 2
    }))

    if (t === 'strike' && strikeLine.value) {
      // 1 point placed: dashed line from start to cursor
      const start = strikeLine.value[0]
      layers.push(L.polyline([[start.lat, start.lng], [ml.lat, ml.lng]], {
        color: '#f56c6c', weight: 2, dashArray: '3 3', opacity: 0.6
      }))
      // Pulse marker at start with "起点 ✓" label
      layers.push(L.marker([start.lat, start.lng], {
        icon: L.divIcon({
          className: 'strike-start-pulse',
          html: '<div style="position:relative"><div style="position:absolute;inset:-10px;background:#f56c6c33;border-radius:50%;animation:ghost-pulse 1.4s infinite"></div><div style="background:#f56c6c;width:18px;height:18px;border-radius:50%;border:3px solid #fff;box-shadow:0 2px 4px rgba(0,0,0,0.5)"></div></div><div style="position:absolute;top:50%;left:130%;transform:translate(0,-50%);background:#1f2937;color:#fbbf24;padding:3px 8px;border-radius:4px;font-size:11px;font-weight:bold;white-space:nowrap;border:1px solid #f59e0b">起点 ✓</div>',
          iconSize: [80, 20],
          iconAnchor: [9, 10]
        })
      }))
    } else if (t === 'aux' && auxiliaryLines.value.length > 0) {
      // Dashed polyline through all placed vertices → cursor
      const last = auxiliaryLines.value[auxiliaryLines.value.length - 1]
      if (last && (last as any).__drawing) {
        const pts: L.LatLngExpression[] = [...last.map(p => [p.lat, p.lng] as L.LatLngExpression), [ml.lat, ml.lng]]
        layers.push(L.polyline(pts, {
          color: '#fa8c16', weight: 2, dashArray: '3 3', opacity: 0.6
        }))
      }
    }

    ghostGroup.value = L.layerGroup(layers).addTo(map)
  }

  watch([mouseLatLng, () => tool.value, () => strikeLine.value, () => auxiliaryLines.value, () => mode.value],
    () => renderGhost(),
    { deep: true }
  )

  // ── Map click handler for DRAW-* sub-states + point variant ──
  let mapClickHandler: ((e: L.LeafletMouseEvent) => void) | null = null
  let mapMoveHandler: ((e: L.LeafletMouseEvent) => void) | null = null
  let mapOutHandler: (() => void) | null = null
  watch([leaflet.map, () => tool.value, () => mode.value, () => options.variant], ([map, t, m, variant]) => {
    if (!map) return
    if (mapClickHandler) { map.off('click', mapClickHandler); mapClickHandler = null }
    if (mapMoveHandler) { map.off('mousemove', mapMoveHandler); mapMoveHandler = null }
    if (mapOutHandler) { map.off('mouseout', mapOutHandler); mapOutHandler = null }
    // point variant: map click sets the point (when not readonly and not mid-draw)
    if (variant === 'point' && !options.readonly && m !== 'edit') {
      if ((map as any).dragging) (map as any).dragging.enable()
      mapClickHandler = (e: L.LeafletMouseEvent) => {
        localPoint.value = { lat: e.latlng.lat, lng: e.latlng.lng }
      }
      map.on('click', mapClickHandler)
      return
    }
    if (m === 'edit' && t) {
      // disable map drag while drawing so accidental drags don't pan
      if ((map as any).dragging) (map as any).dragging.disable()
      mapClickHandler = (e: L.LeafletMouseEvent) => {
        const p: LatLng = { lat: e.latlng.lat, lng: e.latlng.lng }
        if (t === 'polygon') {
          addVertex(p)
        } else if (t === 'strike') {
          if (!strikeLine.value) {
            strikeLine.value = [p, p]
          } else {
            setStrike(strikeLine.value[0], p)
            tool.value = null
          }
        } else if (t === 'aux') {
          // append to last aux line if mid-draw, else create new
          const lastIdx = auxiliaryLines.value.length - 1
          const last = lastIdx >= 0 ? auxiliaryLines.value[lastIdx] : null
          if (last && (last as any).__drawing) {
            auxiliaryLines.value = auxiliaryLines.value.map((l, i) =>
              i === lastIdx ? [...l, p] : l
            )
          } else {
            const newLine = [p]
            ;(newLine as any).__drawing = true
            auxiliaryLines.value = [...auxiliaryLines.value, newLine]
          }
        }
      }
      map.on('click', mapClickHandler)
    } else {
      if ((map as any).dragging) (map as any).dragging.enable()
    }
    // P3: register mousemove/mouseout for the ghost cursor
    mapMoveHandler = (e: L.LeafletMouseEvent) => {
      mouseLatLng.value = { lat: e.latlng.lat, lng: e.latlng.lng }
    }
    mapOutHandler = () => { mouseLatLng.value = null }
    map.on('mousemove', mapMoveHandler)
    map.on('mouseout', mapOutHandler)
  }, { immediate: true })

  // ── Keyboard handler ──
  function onKeyDown(e: KeyboardEvent) {
    if (mode.value !== 'edit') return
    // Don't interfere with form inputs
    const target = e.target as HTMLElement
    if (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA' || target.isContentEditable) return

    if (e.key === 'Delete' || e.key === 'Backspace') {
      if (selectedId.value) {
        e.preventDefault()
        removeSelected()
      }
    } else if (e.key === 'Escape') {
      if (tool.value) { cancelTool(); e.preventDefault() }
      else if (mode.value === 'edit') { exitEdit(); e.preventDefault() }
    } else if (e.key === 'Enter') {
      if (tool.value === 'polygon' && polygon.value.length >= 3) {
        tool.value = null
        e.preventDefault()
      } else if (tool.value === 'aux' && auxiliaryLines.value.length > 0) {
        const lastIdx = auxiliaryLines.value.length - 1
        const last = auxiliaryLines.value[lastIdx]
        if (last && last.length >= 2) {
          delete (last as any).__drawing
          tool.value = null
          e.preventDefault()
        }
      }
    }
  }
  if (typeof document !== 'undefined') {
    document.addEventListener('keydown', onKeyDown)
    onBeforeUnmount(() => document.removeEventListener('keydown', onKeyDown))
  }

  // ── Overlay polygon (point variant 用: 显示所属隐患点范围) ──
  watch([leaflet.map, () => options.overlayPolygon], ([map, overlay]) => {
    if (!map) return
    if (overlayLayer.value) { overlayLayer.value.remove(); overlayLayer.value = null }
    if (!overlay || overlay.length < 3) return
    const geojson: GeoJSON.Feature = {
      type: 'Feature',
      properties: {},
      geometry: { type: 'Polygon', coordinates: [overlay.map(p => [p.lng, p.lat])] }
    }
    overlayLayer.value = L.geoJSON(geojson, {
      style: { color: '#1890ff', fillColor: '#1890ff', fillOpacity: 0.08, weight: 2, dashArray: '6 3' }
    }).addTo(map)
  }, { immediate: true })

  // ── 占位 actions (实现留在后续 task) ──
  const enterEdit = () => { mode.value = 'edit' }
  const exitEdit = () => { mode.value = 'view'; tool.value = null; selectedId.value = null }
  const toggleEdit = () => { mode.value === 'edit' ? exitEdit() : enterEdit() }
  const activateTool = (t: EditorTool) => { tool.value = tool.value === t ? null : t }
  const cancelTool = () => { tool.value = null }
  const clearAll = () => {
    polygon.value = []
    strikeLine.value = null
    auxiliaryLines.value = []
    center.value = null
    manualCenterLocked.value = false
    selectedId.value = null
  }
  const select = (id: SelectableId | null) => { selectedId.value = id }
  const clearSelection = () => { selectedId.value = null }
  const removeSelected = () => {
    const id = selectedId.value
    if (!id) return
    if (id.kind === 'polygon-vertex') {
      if (polygon.value.length <= 3) return
      removeVertex(id)
    } else if (id.kind === 'strike-endpoint') {
      removeStrike()
    } else if (id.kind === 'aux-line') {
      removeAuxLine(id.index)
    }
    selectedId.value = null
  }
  const addVertex = (p: LatLng) => { polygon.value = [...polygon.value, p] }
  const moveVertex = (id: VertexId, p: LatLng) => {
    polygon.value = polygon.value.map((v, i) => i === id.index ? p : v)
  }
  const removeVertex = (id: VertexId) => {
    if (polygon.value.length <= 3) return // 不变量
    polygon.value = [...polygon.value.slice(0, id.index), ...polygon.value.slice(id.index + 1)]
  }
  const insertVertexAfter = (afterId: VertexId, p: LatLng) => {
    const insertAt = afterId.index + 1
    polygon.value = [...polygon.value.slice(0, insertAt), p, ...polygon.value.slice(insertAt)]
  }
  const setStrike = (a: LatLng, b: LatLng) => { strikeLine.value = [a, b] }
  const moveStrikeEndpoint = (idx: 0 | 1, p: LatLng) => {
    if (!strikeLine.value) return
    strikeLine.value = idx === 0 ? [p, strikeLine.value[1]] : [strikeLine.value[0], p]
  }
  const removeStrike = () => { strikeLine.value = null }
  const addAuxLine = (points: LatLng[]) => {
    if (points.length < 2) return
    auxiliaryLines.value = [...auxiliaryLines.value, points.slice()]
  }
  const moveAuxPoint = (lineId: number, pointId: number, p: LatLng) => {
    auxiliaryLines.value = auxiliaryLines.value.map((line, i) =>
      i !== lineId ? line : line.map((pt, j) => j === pointId ? p : pt)
    )
  }
  const removeAuxLine = (lineId: number) => {
    auxiliaryLines.value = auxiliaryLines.value.filter((_, i) => i !== lineId)
  }
  const setCenter = (p: LatLng, manual = false) => {
    center.value = p
    if (manual) manualCenterLocked.value = true
  }
  const moveCenter = (p: LatLng) => { center.value = p; manualCenterLocked.value = true }
  const resetCenter = () => { manualCenterLocked.value = false; autoCenter() }
  const autoCenter = () => {
    if (manualCenterLocked.value) return
    if (polygon.value.length >= 3) {
      const c = centroid(polygon.value)
      if (c) center.value = c
    }
  }

  // ── 同步 (留作占位) ──
  const snapshot = (): EditorSnapshot => ({
    mode: mode.value,
    polygon: polygon.value.slice(),
    strikeLine: strikeLine.value ? [...strikeLine.value] : null,
    auxiliaryLines: auxiliaryLines.value.map(l => l.slice()),
    center: center.value ? { ...center.value } : null,
    manualCenterLocked: manualCenterLocked.value,
    strikeAngle: strikeAngle.value
  })

  return {
    mapRef: leaflet.map,
    isReady: leaflet.isReady,
    containerRef: options.container,
    invalidate: leaflet.invalidate,
    setView: leaflet.setView,
    destroy: leaflet.destroy,

    mode, tool, canEdit, canSave,
    polygon, strikeLine, auxiliaryLines, center, manualCenterLocked, strikeAngle,
    selectedId, selectedVertex,

    enterEdit, exitEdit, toggleEdit, activateTool, cancelTool,
    addVertex, moveVertex, removeVertex, insertVertexAfter,
    setStrike, moveStrikeEndpoint, removeStrike,
    addAuxLine, moveAuxPoint, removeAuxLine,
    setCenter, moveCenter, resetCenter,
    select, clearSelection, removeSelected,
    clearAll, snapshot,
    // P3
    mouseLatLng, ghostGroup, clearGhost,
    // P4
    draggingStrikeIndex,
    // P5
    draggingAuxKey
  }
}
