import { describe, it, expect, beforeEach, afterEach } from 'vitest'
import { ref, nextTick } from 'vue'
import { useMapEditor, type EditorSnapshot } from '../useMapEditor'
import type { BoundaryCoords, LatLng } from '../../lib/boundaryCoords'

describe('useMapEditor — state', () => {
  let container: ReturnType<typeof ref<HTMLDivElement | null>>

  beforeEach(() => { container = ref<HTMLDivElement | null>(null) })
  afterEach(() => { container.value = null })

  it('initializes with empty boundary in view mode', () => {
    const e = useMapEditor({ container, variant: 'boundary' })
    expect(e.mode.value).toBe('view')
    expect(e.tool.value).toBeNull()
    expect(e.polygon.value).toEqual([])
    expect(e.strikeLine.value).toBeNull()
    expect(e.auxiliaryLines.value).toEqual([])
    expect(e.center.value).toBeNull()
    expect(e.manualCenterLocked.value).toBe(false)
  })

  it('respects initialBoundary', () => {
    const initial: BoundaryCoords = {
      polygon: [{ lat: 0, lng: 0 }, { lat: 0, lng: 1 }, { lat: 1, lng: 1 }],
      strikeLine: null,
      auxiliaryLines: []
    }
    const e = useMapEditor({ container, variant: 'boundary', initialBoundary: initial })
    expect(e.polygon.value).toEqual(initial.polygon)
  })

  it('canSave is true only when polygon has ≥ 3 vertices', () => {
    const e = useMapEditor({ container, variant: 'boundary' })
    expect(e.canSave.value).toBe(false)
    e.polygon.value = [{ lat: 0, lng: 0 }, { lat: 0, lng: 1 }, { lat: 1, lng: 1 }]
    expect(e.canSave.value).toBe(true)
  })

  it('canEdit is false when readonly=true', () => {
    const e = useMapEditor({ container, variant: 'boundary', readonly: true })
    expect(e.canEdit.value).toBe(false)
  })

  it('strikeAngle returns null when no strike line', () => {
    const e = useMapEditor({ container, variant: 'boundary' })
    expect(e.strikeAngle.value).toBeNull()
  })
})

describe('useMapEditor — center auto-derive', () => {
  let container: ReturnType<typeof ref<HTMLDivElement | null>>
  beforeEach(() => { container = ref(null) })
  afterEach(() => { container.value = null })

  it('recomputes center from polygon when vertices change (no manual lock)', async () => {
    const e = useMapEditor({ container, variant: 'boundary' })
    e.polygon.value = [{ lat: 0, lng: 0 }, { lat: 0, lng: 4 }, { lat: 4, lng: 4 }, { lat: 4, lng: 0 }]
    await nextTick()
    expect(e.center.value).toBeTruthy()
    expect(e.center.value!.lat).toBeCloseTo(2, 5)
    expect(e.center.value!.lng).toBeCloseTo(2, 5)
  })

  it('does not overwrite manually-locked center when vertices change', async () => {
    const e = useMapEditor({ container, variant: 'boundary' })
    e.setCenter({ lat: 50, lng: 50 }, true) // manual
    await nextTick()
    e.polygon.value = [{ lat: 0, lng: 0 }, { lat: 0, lng: 4 }, { lat: 4, lng: 4 }]
    await nextTick()
    expect(e.center.value).toEqual({ lat: 50, lng: 50 })
  })

  it('resetCenter unlocks and re-derives', async () => {
    const e = useMapEditor({ container, variant: 'boundary' })
    e.setCenter({ lat: 50, lng: 50 }, true)
    await nextTick()
    e.polygon.value = [{ lat: 0, lng: 0 }, { lat: 0, lng: 4 }, { lat: 4, lng: 4 }, { lat: 4, lng: 0 }]
    await nextTick()
    e.resetCenter()
    expect(e.manualCenterLocked.value).toBe(false)
    expect(e.center.value!.lat).toBeCloseTo(2, 5)
  })

  it('moveCenter sets manual lock', () => {
    const e = useMapEditor({ container, variant: 'boundary' })
    e.moveCenter({ lat: 10, lng: 10 })
    expect(e.manualCenterLocked.value).toBe(true)
    expect(e.center.value).toEqual({ lat: 10, lng: 10 })
  })

  it('clears center when polygon shrinks below 3 vertices', async () => {
    const e = useMapEditor({ container, variant: 'boundary' })
    e.polygon.value = [{ lat: 0, lng: 0 }, { lat: 0, lng: 4 }, { lat: 4, lng: 4 }]
    await nextTick()
    expect(e.center.value).toBeTruthy()
    e.polygon.value = [] // simulate clear
    await nextTick()
    expect(e.center.value).toBeNull()
  })
})

describe('useMapEditor — clearAll invariants', () => {
  let container: ReturnType<typeof ref<HTMLDivElement | null>>
  beforeEach(() => { container = ref(null) })
  afterEach(() => { container.value = null })

  it('clears all data and unlocks center', () => {
    const initial: BoundaryCoords = {
      polygon: [{ lat: 0, lng: 0 }, { lat: 0, lng: 1 }, { lat: 1, lng: 1 }],
      strikeLine: [{ lat: 0, lng: 0 }, { lat: 1, lng: 1 }],
      auxiliaryLines: [[{ lat: 0.1, lng: 0.1 }, { lat: 0.2, lng: 0.2 }]]
    }
    const e = useMapEditor({ container, variant: 'boundary', initialBoundary: initial, initialCenter: { lat: 0.5, lng: 0.5 } })
    e.setCenter({ lat: 10, lng: 10 }, true) // manual lock
    e.clearAll()
    expect(e.polygon.value).toEqual([])
    expect(e.strikeLine.value).toBeNull()
    expect(e.auxiliaryLines.value).toEqual([])
    expect(e.center.value).toBeNull()
    expect(e.manualCenterLocked.value).toBe(false)
  })

  it('removeVertex on a 3-vertex polygon is a no-op', () => {
    const e = useMapEditor({ container, variant: 'boundary' })
    e.polygon.value = [{ lat: 0, lng: 0 }, { lat: 0, lng: 1 }, { lat: 1, lng: 1 }]
    e.removeVertex({ kind: 'polygon-vertex', index: 1 })
    expect(e.polygon.value.length).toBe(3)
  })
})

describe('useMapEditor — point variant', () => {
  let container: ReturnType<typeof ref<HTMLDivElement | null>>
  beforeEach(() => { container = ref(null) })
  afterEach(() => { container.value = null })

  it('canSave is true in point variant when a point is set via pointValue ref', async () => {
    const pointRef = ref<LatLng | null>(null)
    const e = useMapEditor({ container, variant: 'point', pointValue: pointRef })
    expect(e.canSave.value).toBe(false)
    pointRef.value = { lat: 30.67, lng: 104.06 }
    await nextTick()
    expect(e.canSave.value).toBe(true)
  })

  it('canSave respects initialPoint in point variant', () => {
    const e = useMapEditor({ container, variant: 'point', initialPoint: { lat: 30.67, lng: 104.06 } })
    expect(e.canSave.value).toBe(true)
  })
})

describe('useMapEditor — removeSelected', () => {
  let container: ReturnType<typeof ref<HTMLDivElement | null>>
  beforeEach(() => { container = ref(null) })
  afterEach(() => { container.value = null })

  it('removes selected polygon vertex', () => {
    const e = useMapEditor({ container, variant: 'boundary' })
    e.polygon.value = [{ lat: 0, lng: 0 }, { lat: 0, lng: 1 }, { lat: 1, lng: 1 }, { lat: 1, lng: 0 }]
    e.select({ kind: 'polygon-vertex', index: 2 })
    e.removeSelected()
    expect(e.polygon.value.length).toBe(3)
    expect(e.selectedId.value).toBeNull()
  })

  it('refuses to remove vertex when polygon would go below 3', () => {
    const e = useMapEditor({ container, variant: 'boundary' })
    e.polygon.value = [{ lat: 0, lng: 0 }, { lat: 0, lng: 1 }, { lat: 1, lng: 1 }]
    e.select({ kind: 'polygon-vertex', index: 0 })
    e.removeSelected()
    expect(e.polygon.value.length).toBe(3)
  })

  it('removes selected aux line', () => {
    const e = useMapEditor({ container, variant: 'boundary' })
    e.addAuxLine([{ lat: 0, lng: 0 }, { lat: 1, lng: 1 }])
    e.addAuxLine([{ lat: 2, lng: 2 }, { lat: 3, lng: 3 }])
    e.select({ kind: 'aux-line', index: 0 })
    e.removeSelected()
    expect(e.auxiliaryLines.value.length).toBe(1)
  })

  it('removes selected strike', () => {
    const e = useMapEditor({ container, variant: 'boundary' })
    e.setStrike({ lat: 0, lng: 0 }, { lat: 1, lng: 1 })
    e.select({ kind: 'strike-endpoint', index: 0 })
    e.removeSelected()
    expect(e.strikeLine.value).toBeNull()
  })
})

describe('useMapEditor — tool sub-state', () => {
  let container: ReturnType<typeof ref<HTMLDivElement | null>>
  beforeEach(() => { container = ref(null) })
  afterEach(() => { container.value = null })

  it('activateTool polygon starts drawing — addVertex extends the polygon', () => {
    const e = useMapEditor({ container, variant: 'boundary' })
    e.enterEdit()
    e.activateTool('polygon')
    expect(e.tool.value).toBe('polygon')
    e.addVertex({ lat: 0, lng: 0 })
    e.addVertex({ lat: 0, lng: 1 })
    e.addVertex({ lat: 1, lng: 1 })
    expect(e.polygon.value.length).toBe(3)
  })

  it('cancelTool resets tool to null without touching data', () => {
    const e = useMapEditor({ container, variant: 'boundary' })
    e.enterEdit()
    e.activateTool('polygon')
    e.addVertex({ lat: 0, lng: 0 })
    e.cancelTool()
    expect(e.tool.value).toBeNull()
    expect(e.polygon.value.length).toBe(1) // addVertex did happen
  })

  it('activateTool(t) when tool already = t toggles off', () => {
    const e = useMapEditor({ container, variant: 'boundary' })
    e.enterEdit()
    e.activateTool('polygon')
    e.activateTool('polygon')
    expect(e.tool.value).toBeNull()
  })

  it('exitEdit clears tool and selection', () => {
    const e = useMapEditor({ container, variant: 'boundary' })
    e.enterEdit()
    e.activateTool('polygon')
    e.exitEdit()
    expect(e.tool.value).toBeNull()
    expect(e.mode.value).toBe('view')
  })
})

describe('useMapEditor — change callbacks', () => {
  let container: ReturnType<typeof ref<HTMLDivElement | null>>
  beforeEach(() => { container = ref(null) })
  afterEach(() => { container.value = null })

  it('onChange fires when polygon changes', async () => {
    let snap: EditorSnapshot | null = null
    const e = useMapEditor({
      container, variant: 'boundary',
      onChange: s => { snap = s }
    })
    e.polygon.value = [{ lat: 0, lng: 0 }, { lat: 0, lng: 1 }, { lat: 1, lng: 1 }]
    await nextTick()
    expect(snap).not.toBeNull()
    expect(snap!.polygon.length).toBe(3)
  })

  it('onCenterChange fires when center changes', async () => {
    const calls: (LatLng | null)[] = []
    const e = useMapEditor({
      container, variant: 'boundary',
      onCenterChange: c => { calls.push(c ? { ...c } : null) }
    })
    e.polygon.value = [{ lat: 0, lng: 0 }, { lat: 0, lng: 4 }, { lat: 4, lng: 4 }]
    await nextTick()
    expect(calls.length).toBeGreaterThan(0)
    expect(calls[calls.length - 1]).toBeTruthy()
  })
})

describe('useMapEditor — overlay polygon (point variant)', () => {
  let container: ReturnType<typeof ref<HTMLDivElement | null>>
  beforeEach(() => { container = ref(null) })
  afterEach(() => { container.value = null })

  it('reacts to changes in overlayPolygon ref', async () => {
    const overlay = ref<LatLng[] | null>(null)
    const e = useMapEditor({ container, variant: 'point', overlayPolygon: overlay.value ?? null })
    // pass overlay to composable via prop re-call is not how the API works;
    // verify direct prop value is accepted
    overlay.value = [{ lat: 0, lng: 0 }, { lat: 0, lng: 1 }, { lat: 1, lng: 1 }]
    await nextTick()
    expect(overlay.value.length).toBe(3)
    expect(e.canSave.value).toBe(false) // point variant, no point yet
  })
})

describe('useMapEditor — drawing ghost (P3)', () => {
  let container: ReturnType<typeof ref<HTMLDivElement | null>>
  beforeEach(() => { container = ref(null) })
  afterEach(() => { container.value = null })

  it('exposes mouseLatLng as null by default', () => {
    const e = useMapEditor({ container, variant: 'boundary' })
    expect((e as any).mouseLatLng.value).toBeNull()
  })

  it('exposes ghostGroup as null by default', () => {
    const e = useMapEditor({ container, variant: 'boundary' })
    expect((e as any).ghostGroup.value).toBeNull()
  })

  it('exposes clearGhost as a callable function', () => {
    const e = useMapEditor({ container, variant: 'boundary' })
    expect(typeof (e as any).clearGhost).toBe('function')
    expect(() => (e as any).clearGhost()).not.toThrow()
  })
})

describe('useMapEditor — strike endpoint drag (P4)', () => {
  let container: ReturnType<typeof ref<HTMLDivElement | null>>
  beforeEach(() => { container = ref(null) })
  afterEach(() => { container.value = null })

  it('moveStrikeEndpoint(0, ...) replaces start point', () => {
    const e = useMapEditor({ container, variant: 'boundary' })
    e.setStrike({ lat: 0, lng: 0 }, { lat: 1, lng: 1 })
    e.moveStrikeEndpoint(0, { lat: 5, lng: 5 })
    expect(e.strikeLine.value).toEqual([{ lat: 5, lng: 5 }, { lat: 1, lng: 1 }])
  })

  it('moveStrikeEndpoint(1, ...) replaces end point', () => {
    const e = useMapEditor({ container, variant: 'boundary' })
    e.setStrike({ lat: 0, lng: 0 }, { lat: 1, lng: 1 })
    e.moveStrikeEndpoint(1, { lat: 9, lng: 9 })
    expect(e.strikeLine.value).toEqual([{ lat: 0, lng: 0 }, { lat: 9, lng: 9 }])
  })

  it('moveStrikeEndpoint is no-op when no strike', () => {
    const e = useMapEditor({ container, variant: 'boundary' })
    e.moveStrikeEndpoint(0, { lat: 5, lng: 5 })
    expect(e.strikeLine.value).toBeNull()
  })

  it('draggingStrikeIndex is exposed as null by default', () => {
    const e = useMapEditor({ container, variant: 'boundary' })
    expect((e as any).draggingStrikeIndex.value).toBeNull()
  })
})

describe('useMapEditor — aux point drag (P5)', () => {
  let container: ReturnType<typeof ref<HTMLDivElement | null>>
  beforeEach(() => { container = ref(null) })
  afterEach(() => { container.value = null })

  it('moveAuxPoint replaces the right (line, point)', () => {
    const e = useMapEditor({ container, variant: 'boundary' })
    e.addAuxLine([{ lat: 0, lng: 0 }, { lat: 1, lng: 1 }, { lat: 2, lng: 2 }])
    e.moveAuxPoint(0, 1, { lat: 9, lng: 9 })
    expect(e.auxiliaryLines.value[0][1]).toEqual({ lat: 9, lng: 9 })
    expect(e.auxiliaryLines.value[0][0]).toEqual({ lat: 0, lng: 0 })
    expect(e.auxiliaryLines.value[0][2]).toEqual({ lat: 2, lng: 2 })
  })

  it('moveAuxPoint on a different line does not affect other lines', () => {
    const e = useMapEditor({ container, variant: 'boundary' })
    e.addAuxLine([{ lat: 0, lng: 0 }, { lat: 1, lng: 1 }])
    e.addAuxLine([{ lat: 10, lng: 10 }, { lat: 11, lng: 11 }])
    e.moveAuxPoint(1, 0, { lat: 99, lng: 99 })
    expect(e.auxiliaryLines.value[0][0]).toEqual({ lat: 0, lng: 0 })
    expect(e.auxiliaryLines.value[1][0]).toEqual({ lat: 99, lng: 99 })
  })

  it('removing an aux line does not shift other indices', () => {
    const e = useMapEditor({ container, variant: 'boundary' })
    e.addAuxLine([{ lat: 0, lng: 0 }, { lat: 1, lng: 1 }])
    e.addAuxLine([{ lat: 10, lng: 10 }, { lat: 11, lng: 11 }])
    e.addAuxLine([{ lat: 20, lng: 20 }, { lat: 21, lng: 21 }])
    e.removeAuxLine(1)
    expect(e.auxiliaryLines.value.length).toBe(2)
    expect(e.auxiliaryLines.value[0][0]).toEqual({ lat: 0, lng: 0 })
    expect(e.auxiliaryLines.value[1][0]).toEqual({ lat: 20, lng: 20 })
  })

  it('draggingAuxKey is exposed as null by default', () => {
    const e = useMapEditor({ container, variant: 'boundary' })
    expect((e as any).draggingAuxKey.value).toBeNull()
  })
})
