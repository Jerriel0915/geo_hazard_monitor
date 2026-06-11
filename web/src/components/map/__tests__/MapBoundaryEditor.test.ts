import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick, ref, computed } from 'vue'
import ElementPlus from 'element-plus'
import MapBoundaryEditor from '../MapBoundaryEditor.vue'
import type { BoundaryCoords } from '../../lib/boundaryCoords'

// Shared state — vi.mock factory returns refs that read/write these
const state = {
  mode: ref<'view' | 'edit'>('edit'),
  tool: ref<null | string>(null),
  canEdit: computed(() => true),
  canSave: computed(() => true),
  selectedId: ref<any>(null),
  manualCenterLocked: ref(false),
  polygon: ref<any[]>([]),
  strikeLine: ref<[any, any] | null>(null),
  auxiliaryLines: ref<any[][]>([]),
  center: ref<any>(null),
  toggleEdit: vi.fn(),
  removeSelected: vi.fn(),
  resetCenter: vi.fn(),
  clearAll: vi.fn(),
  invalidate: vi.fn()
}

vi.mock('@/composables/useMapEditor', () => ({
  useMapEditor: () => state
}))

const initial: BoundaryCoords = {
  polygon: [{ lat: 0, lng: 0 }, { lat: 0, lng: 1 }, { lat: 1, lng: 1 }],
  strikeLine: null,
  auxiliaryLines: []
}

describe('MapBoundaryEditor — delete button visibility (P2)', () => {
  let host: HTMLDivElement
  beforeEach(() => {
    host = document.createElement('div')
    document.body.appendChild(host)
    // Reset state
    state.mode.value = 'edit'
    state.selectedId.value = null
  })
  afterEach(() => { document.body.removeChild(host) })

  it('shows 删除选中 button in edit mode even when nothing is selected', async () => {
    state.mode.value = 'edit'
    state.selectedId.value = null
    const w = mount(MapBoundaryEditor, {
      attachTo: host,
      props: { initialValue: initial, height: 400 },
      global: { plugins: [ElementPlus] }
    })
    await nextTick()
    const deleteBtn = w.findAll('button').find(b => b.text().includes('删除选中'))
    expect(deleteBtn).toBeDefined()
    expect(deleteBtn!.attributes('disabled')).toBeDefined()
  })

  it('enables 删除选中 button when something is selected', async () => {
    state.mode.value = 'edit'
    state.selectedId.value = { kind: 'polygon-vertex', index: 0 }
    const w = mount(MapBoundaryEditor, {
      attachTo: host,
      props: { initialValue: initial, height: 400 },
      global: { plugins: [ElementPlus] }
    })
    await nextTick()
    const deleteBtn = w.findAll('button').find(b => b.text().includes('删除选中'))
    expect(deleteBtn).toBeDefined()
    expect(deleteBtn!.attributes('disabled')).toBeUndefined()
  })

  it('hides 删除选中 button in view mode', async () => {
    state.mode.value = 'view'
    state.selectedId.value = null
    const w = mount(MapBoundaryEditor, {
      attachTo: host,
      props: { initialValue: initial, height: 400 },
      global: { plugins: [ElementPlus] }
    })
    await nextTick()
    const deleteBtn = w.findAll('button').find(b => b.text().includes('删除选中'))
    expect(deleteBtn).toBeUndefined()
  })
})
