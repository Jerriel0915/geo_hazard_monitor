import {
  computed,
  type ComputedRef,
  onBeforeUnmount,
  type Ref,
  ref,
  type ShallowRef,
  shallowRef,
  unref,
  watch
} from 'vue'
import L, {type Map as LMap} from 'leaflet'
import type {BoundaryCoords, LatLng} from '@/lib/boundaryCoords'
import {centroid, strikeAngle as computeStrikeAngle} from '@/lib/boundaryCoords'
import {useLeafletMap} from './useLeafletMap'

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

/**
 * Unified "draggable vertex" helper. Used by polygon vertices, strike
 * endpoints, AND aux points — all three go through the same logic
 * so the drag-during-render protection, click-to-select, and
 * snap-back-if-not-in-edit-mode behavior are guaranteed identical.
 *
 * Pattern:
 *   - If `isDragging` is true, return `existing` untouched (its
 *     position is managed by Leaflet's drag handler; we'd interrupt
 *     the drag if we re-touched it).
 *   - If `existing` is set, update its position in place.
 *   - Otherwise create a new marker with `draggable: true` and wire
 *     dragstart/drag/dragend/click handlers.
 *
 * The drag handler always allows the drag (Leaflet) but calls
 * `canMove()` to decide whether to commit the new position. If not,
 * it snaps back to `snapBackTo()`.
 */
interface DraggableVertexOpts {
  position: LatLng
  existing: L.Marker | null | undefined
  isDragging: boolean
  iconHtml: string
  iconSize: [number, number]
  iconAnchor: [number, number]
  iconClass?: string
  onDragStart: () => void
  onDrag: (pos: LatLng) => void
  onDragEnd: () => void
  onClick: () => void
  canMove: () => boolean
  snapBackTo: () => LatLng
}

function ensureDraggableVertex(
    map: L.Map,
    opts: DraggableVertexOpts
): L.Marker {
  if (opts.isDragging) return opts.existing ?? createNew()
  if (opts.existing) {
    opts.existing.setLatLng([opts.position.lat, opts.position.lng])
    return opts.existing
  }
  return createNew()

  function createNew(): L.Marker {
    const m = L.marker([opts.position.lat, opts.position.lng], {
      icon: L.divIcon({
        className: opts.iconClass ?? 'draggable-vertex-marker',
        html: opts.iconHtml,
        iconSize: opts.iconSize,
        iconAnchor: opts.iconAnchor
      }),
      // Always draggable; the drag handler enforces canMove() and
      // snaps back if not allowed. Creating as draggable:true is
      // essential so markers made in view mode become draggable
      // the moment the user enters edit mode (without recreating).
      draggable: true
    }).addTo(map)
    m.on('dragstart', () => opts.onDragStart())
    m.on('drag', (e: any) => {
      if (!opts.canMove()) {
        const sb = opts.snapBackTo()
        e.target.setLatLng([sb.lat, sb.lng])
        return
      }
      const ll = e.target.getLatLng()
      opts.onDrag({lat: ll.lat, lng: ll.lng})
    })
    m.on('dragend', () => opts.onDragEnd())
    m.on('click', (e: any) => {
      L.DomEvent.stopPropagation(e)
      opts.onClick()
    })
    return m
  }
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
    // 既可传 Ref(响应式),也可传普通值(非响应式,初始一次性)
    overlayPolygon?: Ref<LatLng[] | null> | LatLng[] | null
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
      // 外部 → 内部:prop 变化时同步到编辑器内部
    watch(options.pointValue, v => { if (v) localPoint.value = v }, { immediate: true })
      // 内部 → 外部:地图点击/拖拽图钉时反向推回 prop,让父组件 v-model 收到更新
      //(不加这个反向,父组件 watch(pickerLngLat) 永远触发不了,坐标输入栏也不会跟随)
      watch(localPoint, v => {
          const curr = options.pointValue!.value
          if (v && (!curr || v.lat !== curr.lat || v.lng !== curr.lng)) {
              options.pointValue!.value = v
          }
      })
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
      tianditu: options.tianditu,
      // Disable double-click zoom to avoid Leaflet's 300ms click delay;
      // this can swallow click events in Edge.
      doubleClickZoom: false
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

  function vertexHtml(selected: boolean, editable: boolean): string {
    const ring = selected ? 'box-shadow:0 0 0 3px #ef4444aa;' :
                  editable ? 'box-shadow:0 0 0 2px #f59e0b80;' : ''
    return `<div style="background:#67C23A;width:10px;height:10px;border-radius:50%;border:1.5px solid #fff;${ring}"></div>`
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
      const isSelected = selectedId.value?.kind === 'polygon-vertex' && selectedId.value.index === i
      return ensureDraggableVertex(map, {
        position: p,
        existing: vertexMarkers.value[i],
        isDragging: i === draggingVertexIndex.value,
        iconHtml: vertexHtml(isSelected, mode.value === 'edit'),
        iconSize: [16, 16],
        iconAnchor: [8, 8],
        iconClass: 'vertex-marker',
        onDragStart: () => {
          draggingVertexIndex.value = i
        },
        onDrag: pos => moveVertex({kind: 'polygon-vertex', index: i}, pos),
        onDragEnd: () => {
          draggingVertexIndex.value = null
        },
        onClick: () => {
          if (mode.value === 'edit') select({kind: 'polygon-vertex', index: i})
        },
        canMove: () => mode.value === 'edit' && canEdit.value,
        snapBackTo: () => polygon.value[i]
      })
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
        // P4: draggable endpoint markers (incremental — don't kill the dragging one)
        // 1. Remove only the non-dragging markers (in case of length change)
        while (strikeEndpointMarkers.value.length > 2) {
            const m = strikeEndpointMarkers.value.pop()
            if (m) m.remove()
        }
        while (strikeEndpointMarkers.value.length < 2) {
            strikeEndpointMarkers.value.push(null)
        }
        // 2. For each endpoint, ensure a marker exists at the right position
      strikeEndpointMarkers.value = strikeLine.value.map((pt, i) => {
        const isSelected = isLineSelected && selectedId.value!.index === i
        return ensureDraggableVertex(map, {
          position: pt,
          existing: strikeEndpointMarkers.value[i],
          isDragging: i === draggingStrikeIndex.value,
          iconHtml: `<div style="background:#f56c6c;color:#fff;width:14px;height:14px;border-radius:50%;display:flex;align-items:center;justify-content:center;font-size:9px;font-weight:bold;border:1.5px solid #fff;${isSelected ? 'box-shadow:0 0 0 3px #ef4444aa;' : 'box-shadow:0 0 0 2px #f59e0b80;'}">${i + 1}</div>`,
          iconSize: [18, 18],
          iconAnchor: [9, 9],
          iconClass: 'strike-endpoint-marker',
          onDragStart: () => {
            draggingStrikeIndex.value = i as 0 | 1
          },
          onDrag: pos => moveStrikeEndpoint(i as 0 | 1, pos),
          onDragEnd: () => {
            draggingStrikeIndex.value = null
          },
          onClick: () => {
            if (mode.value === 'edit') select({kind: 'strike-endpoint', index: i as 0 | 1})
          },
          canMove: () => mode.value === 'edit' && canEdit.value,
          snapBackTo: () => strikeLine.value![i]
        })
      })
    } else if (strikeLayer.value) {
      strikeLayer.value.remove()
      strikeLayer.value = null
      strikeEndpointMarkers.value.forEach(m => { if (m) m.remove() })
      strikeEndpointMarkers.value = []
    }

      // aux lines + per-vertex markers (incremental — don't kill dragging one)
    auxLayers.value.forEach(l => l.remove())
    auxLayers.value = []
      // Trim auxPointMarkers if there are fewer lines now
      while (auxPointMarkers.value.length > auxiliaryLines.value.length) {
          const row = auxPointMarkers.value.pop()
          if (row) row.forEach(m => {
              if (m) m.remove()
          })
      }
    auxiliaryLines.value.forEach((line, lineIdx) => {
      const isLineSelected = selectedId.value?.kind === 'aux-line' && selectedId.value.index === lineIdx
      const pl = L.polyline(line.map(p => [p.lat, p.lng] as L.LatLngExpression), {
        color: '#fa8c16', weight: isLineSelected ? 3 : 2, dashArray: '5 4'
      }).addTo(map)
      pl.on('click', () => select({ kind: 'aux-line', index: lineIdx }))
      auxLayers.value.push(pl)
        // P5: per-vertex markers (incremental — don't kill the dragging one)
        const existingRow = auxPointMarkers.value[lineIdx] || []
        // Trim if line is shorter
        while (auxPointMarkers.value.length <= lineIdx) auxPointMarkers.value.push([])
        while (auxPointMarkers.value[lineIdx].length > line.length) {
            const m = auxPointMarkers.value[lineIdx].pop()
            if (m) m.remove()
        }
        // Ensure row is at least as long as line
        while (auxPointMarkers.value[lineIdx].length < line.length) {
            auxPointMarkers.value[lineIdx].push(null)
        }
      const row: L.Marker[] = line.map((pt, ptIdx) => {
        return ensureDraggableVertex(map, {
          position: pt,
          existing: auxPointMarkers.value[lineIdx][ptIdx],
          isDragging: !!(draggingAuxKey.value &&
            draggingAuxKey.value.line === lineIdx &&
              draggingAuxKey.value.point === ptIdx),
          iconHtml: `<div style="background:#fa8c16;width:8px;height:8px;border-radius:2px;border:1.5px solid #fff;${isLineSelected ? 'box-shadow:0 0 0 3px #ef4444aa;' : 'box-shadow:0 0 0 2px #f59e0b80;'}"></div>`,
          iconSize: [12, 12],
          iconAnchor: [6, 6],
          iconClass: 'aux-point-marker',
          onDragStart: () => {
            draggingAuxKey.value = {line: lineIdx, point: ptIdx}
          },
          onDrag: pos => moveAuxPoint(lineIdx, ptIdx, pos),
          onDragEnd: () => {
            draggingAuxKey.value = null
          },
          onClick: () => {
            if (mode.value === 'edit') select({kind: 'aux-line', index: lineIdx})
          },
          canMove: () => mode.value === 'edit' && canEdit.value,
          snapBackTo: () => auxiliaryLines.value[lineIdx][ptIdx]
        })
      })
      auxPointMarkers.value[lineIdx] = row
    })

    // center marker — update in place; skip when being dragged
    if (center.value && !draggingCenter.value) {
      if (centerMarker.value) {
        centerMarker.value.setLatLng([center.value.lat, center.value.lng])
      } else {
        centerMarker.value = L.marker([center.value.lat, center.value.lng], {
          icon: L.divIcon({
            className: '',
            html: '<div style="background:#1890ff;color:#fff;width:14px;height:14px;border-radius:50%;border:1.5px solid #fff;display:flex;align-items:center;justify-content:center;font-size:9px">★</div>',
            iconSize: [18, 18], iconAnchor: [9, 9]
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
      // 1 point placed: dashed line from start to cursor.
      // 起点本身已由 strikeEndpointMarkers 渲染为红点, 无需额外叠加徽章;
      // 用户可以通过拖动该红点调整起点位置。
      const start = strikeLine.value[0]
      layers.push(L.polyline([[start.lat, start.lng], [ml.lat, ml.lng]], {
        color: '#f56c6c', weight: 2, dashArray: '3 3', opacity: 0.6
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
    // Prevent accidental panning during draw mode without disabling the
    // drag handler (map.dragging.disable() breaks click events in Edge).
    let drawPanGuard: (() => void) | null = null
  watch([leaflet.map, () => tool.value, () => mode.value, () => options.variant], ([map, t, m, variant]) => {
    if (!map) return
    if (mapClickHandler) { map.off('click', mapClickHandler); mapClickHandler = null }
    if (mapMoveHandler) { map.off('mousemove', mapMoveHandler); mapMoveHandler = null }
    if (mapOutHandler) { map.off('mouseout', mapOutHandler); mapOutHandler = null }
      if (drawPanGuard) {
          map.off('move', drawPanGuard);
          drawPanGuard = null
      }
    // point variant: map click sets the point (when not readonly and not mid-draw)
    if (variant === 'point' && !options.readonly && m !== 'edit') {
      mapClickHandler = (e: L.LeafletMouseEvent) => {
        localPoint.value = { lat: e.latlng.lat, lng: e.latlng.lng }
      }
      map.on('click', mapClickHandler)
      return
    }
    if (m === 'edit' && t) {
        // Lock map center during draw so accidental drags don't pan.
        // Uses a move-event guard instead of dragging.disable() because
        // disabling the drag handler breaks click propagation in Edge.
        // The guard temporarily detaches itself before calling setView
        // to prevent infinite recursion (setView always fires move).
        const drawCenter = map.getCenter()
        drawPanGuard = () => {
            map.off('move', drawPanGuard!)
            // Only snap back if the map actually moved (not our own setView).
            const cc = map.getCenter()
            if (Math.abs(cc.lat - drawCenter.lat) > 1e-7 || Math.abs(cc.lng - drawCenter.lng) > 1e-7) {
                map.setView(drawCenter, map.getZoom(), {animate: false})
            }
            map.on('move', drawPanGuard!)
        }
        map.on('move', drawPanGuard)

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
    // 注意:用 computed(unref(...)) 包装,而非 () => options.overlayPolygon。
    // 后者读到的是 setup 时的快照,prop 后续变化不会触发 watch 重绘。
    // computed 会追踪 unref 内部对 Ref.value 的访问,从而真正响应 prop 变化。
    const overlayPolygonSource = computed(() => unref(options.overlayPolygon))
    watch([leaflet.map, overlayPolygonSource], ([map, overlay]) => {
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
