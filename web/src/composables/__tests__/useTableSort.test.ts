import {describe, expect, test} from 'vitest'
import {useTableSort} from '@/composables/useTableSort'

describe('useTableSort', () => {
  test('defaults to empty sort field and order', () => {
    const sort = useTableSort()
    expect(sort.sortField.value).toBe('')
    expect(sort.sortOrder.value).toBe('')
  })

  test('accepts initial field', () => {
    const sort = useTableSort('code')
    expect(sort.sortField.value).toBe('code')
    expect(sort.sortOrder.value).toBe('')
  })

  test('toggle cycles asc → desc → none', () => {
    const sort = useTableSort()
    sort.toggle('name')
    expect(sort.sortField.value).toBe('name')
    expect(sort.sortOrder.value).toBe('asc')
    sort.toggle('name')
    expect(sort.sortOrder.value).toBe('desc')
    sort.toggle('name')
    expect(sort.sortField.value).toBe('')
    expect(sort.sortOrder.value).toBe('')
  })

  test('toggle switches field on different field', () => {
    const sort = useTableSort()
    sort.toggle('name')
    sort.toggle('code')
    expect(sort.sortField.value).toBe('code')
    expect(sort.sortOrder.value).toBe('asc')
  })

  test('sorted returns array as-is when no sort', () => {
    const sort = useTableSort()
    const list = [{a: 3}, {a: 1}, {a: 2}]
    expect(sort.sorted(list)).toEqual(list)
  })

  test('sorted sorts numbers ascending', () => {
    const sort = useTableSort()
    sort.toggle('val')
    const result = sort.sorted([{val: 3}, {val: 1}, {val: 2}])
    expect(result).toEqual([{val: 1}, {val: 2}, {val: 3}])
  })

  test('sorted sorts numbers descending', () => {
    const sort = useTableSort()
    sort.toggle('val')
    sort.toggle('val') // now desc
    const result = sort.sorted([{val: 1}, {val: 3}, {val: 2}])
    expect(result).toEqual([{val: 3}, {val: 2}, {val: 1}])
  })

  test('sorted sorts strings (does not preserve original order)', () => {
    const sort = useTableSort()
    sort.toggle('name')
    const original = [{name: 'banana'}, {name: 'apple'}, {name: 'cherry'}]
    const result = sort.sorted(original)
    expect(result).not.toEqual(original)
    expect(result).toHaveLength(3)
  })

  test('sorted puts null/undefined at end', () => {
    const sort = useTableSort()
    sort.toggle('val')
    const list = [{val: null}, {val: 1}, {val: undefined}, {val: 2}]
    const result = sort.sorted(list)
    expect(result[0].val).toBe(1)
    expect(result[1].val).toBe(2)
    expect(result[2].val).toBeNull()
  })

  test('sorted does not mutate original array', () => {
    const sort = useTableSort()
    sort.toggle('val')
    const list = [{val: 2}, {val: 1}]
    const result = sort.sorted(list)
    expect(list).toEqual([{val: 2}, {val: 1}])
    expect(result).toEqual([{val: 1}, {val: 2}])
  })

  test('sortInfo provides current state', () => {
    const sort = useTableSort()
    expect(sort.sortInfo.value).toEqual({field: '', order: ''})
    sort.toggle('name')
    expect(sort.sortInfo.value).toEqual({field: 'name', order: 'asc'})
  })
})
