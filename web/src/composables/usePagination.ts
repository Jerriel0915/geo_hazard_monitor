import {ref, type Ref} from 'vue'

export interface PaginationState<T> {
  /** 当前页码 */
  currentPage: Ref<number>
  /** 每页条数 */
  pageSize: Ref<number>
  /** 总条数 */
  total: Ref<number>
  /** 表格数据 */
  tableData: Ref<T[]>
  /** 加载中 */
  loading: Ref<boolean>
  /** 选中行 */
  selectedRows: Ref<T[]>
  /** 搜索关键词 */
  searchKeyword: Ref<string>
}

export interface UsePaginationOptions<T> {
  /** 初始每页条数 */
  pageSize?: number
  /** 数据加载函数 — 返回 { rows, total } */
  fetchFn: (params: { pageNum: number; pageSize: number; keyword: string }) => Promise<{ rows: T[]; total: number }>
}

/**
 * 通用分页 + 搜索 + 选中 composable
 *
 * 消除 16 个管理列表页中重复的 currentPage/pageSize/total/loading/searchKeyword/selectedRows 状态。
 * 每个页面只需提供 `fetchFn` 即可获得完整的分页搜索能力。
 */
export function usePagination<T>(opts: UsePaginationOptions<T>) {
  const currentPage = ref(1)
  const pageSize = ref(opts.pageSize ?? 10)
  const total = ref(0)
  const tableData = ref<T[]>([]) as Ref<T[]>
  const loading = ref(false)
  const selectedRows = ref<T[]>([]) as Ref<T[]>
  const searchKeyword = ref('')

  /** 加载数据 */
  const loadData = async () => {
    loading.value = true
    try {
      const result = await opts.fetchFn({
        pageNum: currentPage.value,
        pageSize: pageSize.value,
        keyword: searchKeyword.value,
      })
      tableData.value = result.rows
      total.value = result.total
    } finally {
      loading.value = false
    }
  }

  /** 搜索（重置到第一页） */
  const search = () => {
    currentPage.value = 1
    loadData()
  }

  /** 重置搜索 */
  const reset = () => {
    searchKeyword.value = ''
    currentPage.value = 1
    loadData()
  }

  /** pageSize 变化 */
  const onSizeChange = () => {
    currentPage.value = 1
    loadData()
  }

  /** currentPage 变化 */
  const onPageChange = () => {
    loadData()
  }

  /** 选中行变化 */
  const onSelectionChange = (rows: T[]) => {
    selectedRows.value = rows
  }

  return {
    // state
    currentPage,
    pageSize,
    total,
    tableData,
    loading,
    selectedRows,
    searchKeyword,
    // actions
    loadData,
    search,
    reset,
    onSizeChange,
    onPageChange,
    onSelectionChange,
  } as PaginationState<T> & {
    loadData: () => Promise<void>
    search: () => void
    reset: () => void
    onSizeChange: () => void
    onPageChange: () => void
    onSelectionChange: (rows: T[]) => void
  }
}
