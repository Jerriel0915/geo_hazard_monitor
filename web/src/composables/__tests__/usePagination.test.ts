import {describe, expect, test, vi} from 'vitest'
import {usePagination} from '@/composables/usePagination'

interface TestRow {
  id: number
  name: string
}

describe('usePagination', () => {
  test('initial state', () => {
    const p = usePagination<TestRow>({
      fetchFn: async () => ({rows: [], total: 0}),
    })
    expect(p.currentPage.value).toBe(1)
    expect(p.pageSize.value).toBe(10)
    expect(p.total.value).toBe(0)
    expect(p.tableData.value).toEqual([])
    expect(p.loading.value).toBe(false)
    expect(p.searchKeyword.value).toBe('')
  })

  test('accepts custom pageSize', () => {
    const p = usePagination<TestRow>({
      pageSize: 20,
      fetchFn: async () => ({rows: [], total: 0}),
    })
    expect(p.pageSize.value).toBe(20)
  })

  test('loadData calls fetchFn with correct params', async () => {
    const fetchFn = vi.fn(async () => ({rows: [{id: 1, name: 'a'}], total: 1}))
    const p = usePagination<TestRow>({fetchFn})

    await p.loadData()
    expect(fetchFn).toHaveBeenCalledWith({pageNum: 1, pageSize: 10, keyword: ''})
    expect(p.tableData.value).toEqual([{id: 1, name: 'a'}])
    expect(p.total.value).toBe(1)
  })

  test('search resets to page 1', async () => {
    const fetchFn = vi.fn(async () => ({rows: [], total: 0}))
    const p = usePagination<TestRow>({fetchFn})

    p.currentPage.value = 5
    p.search()
    expect(p.currentPage.value).toBe(1)
  })

  test('reset clears keyword and page', async () => {
    const fetchFn = vi.fn(async () => ({rows: [], total: 0}))
    const p = usePagination<TestRow>({fetchFn})

    p.searchKeyword.value = 'test'
    p.currentPage.value = 3
    p.reset()
    expect(p.searchKeyword.value).toBe('')
    expect(p.currentPage.value).toBe(1)
  })

  test('onSizeChange resets to page 1', async () => {
    const p = usePagination<TestRow>({
      fetchFn: async () => ({rows: [], total: 0}),
    })

    p.currentPage.value = 5
    p.pageSize.value = 20
    p.onSizeChange()
    expect(p.currentPage.value).toBe(1)
  })

  test('onSelectionChange updates selectedRows', () => {
    const p = usePagination<TestRow>({
      fetchFn: async () => ({rows: [], total: 0}),
    })

    const rows = [{id: 1, name: 'a'}, {id: 2, name: 'b'}]
    p.onSelectionChange(rows)
    expect(p.selectedRows.value).toEqual(rows)
  })
})
