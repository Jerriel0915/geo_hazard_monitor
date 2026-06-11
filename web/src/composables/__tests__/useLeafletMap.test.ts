import { describe, it, expect, beforeEach, afterEach } from 'vitest'
import { ref, nextTick } from 'vue'
import { useLeafletMap } from '../useLeafletMap'

describe('useLeafletMap', () => {
  let container: ReturnType<typeof ref<HTMLDivElement | null>>

  beforeEach(() => { container = ref<HTMLDivElement | null>(null) })
  afterEach(() => { container.value = null })

  it('keeps map null when container is null', () => {
    const { map, isReady } = useLeafletMap({ container })
    expect(map.value).toBeNull()
    expect(isReady.value).toBe(false)
  })

  it('creates map when container is set to a DOM element', async () => {
    const div = document.createElement('div')
    document.body.appendChild(div)
    container.value = div
    const { map } = useLeafletMap({ container })
    await nextTick()
    expect(map.value).not.toBeNull()
    document.body.removeChild(div)
  })

  it('destroys map when container is set back to null', async () => {
    const div = document.createElement('div')
    document.body.appendChild(div)
    container.value = div
    const { map } = useLeafletMap({ container })
    await nextTick()
    const instance = map.value
    expect(instance).not.toBeNull()

    container.value = null
    await nextTick()
    expect(map.value).toBeNull()
    document.body.removeChild(div)
  })

  it('recreates map when container is changed to a different element', async () => {
    const divA = document.createElement('div')
    const divB = document.createElement('div')
    document.body.appendChild(divA)
    document.body.appendChild(divB)

    container.value = divA
    const { map } = useLeafletMap({ container })
    await nextTick()
    const instanceA = map.value

    container.value = divB
    await nextTick()
    const instanceB = map.value
    expect(instanceB).not.toBeNull()
    expect(instanceB).not.toBe(instanceA)

    document.body.removeChild(divA)
    document.body.removeChild(divB)
  })

  it('calls onBeforeUnmount-style cleanup via destroy', async () => {
    const div = document.createElement('div')
    document.body.appendChild(div)
    container.value = div
    const { map, destroy } = useLeafletMap({ container })
    await nextTick()
    expect(map.value).not.toBeNull()
    destroy()
    expect(map.value).toBeNull()
    document.body.removeChild(div)
  })

  it('invalidate schedules nextTick invalidateSize without throwing', () => {
    const div = document.createElement('div')
    document.body.appendChild(div)
    container.value = div
    const { invalidate } = useLeafletMap({ container })
    expect(() => invalidate()).not.toThrow()
    document.body.removeChild(div)
  })
})
