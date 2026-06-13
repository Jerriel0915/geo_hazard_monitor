import axios from 'axios'
import {ElMessage} from 'element-plus'
import {reactive, ref, type Ref} from 'vue'
import {showRequestErrorMessage} from '@/utils/errorHandler'

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

export interface LogPagination {
    page: number
    size: number
    total: number
}

export interface LogQueryOptions<T extends Record<string, any>> {
    /** API endpoint path, e.g. '/api/v1/logs/operations/page' */
    endpoint: string
    /** Initial search form values */
    initialForm: T
    /** Optional extra query params to merge into every request */
    extraParams?: () => Record<string, any>
}

export interface LogQueryReturn<T extends Record<string, any>, R> {
    loading: Ref<boolean>
    records: Ref<R[]>
    pagination: LogPagination
    searchForm: T
    fetch: () => Promise<void>
    search: () => void
    reset: () => void
    handleSizeChange: (v: number) => void
    handlePageChange: (v: number) => void
}

// ---------------------------------------------------------------------------
// Composable
// ---------------------------------------------------------------------------

function requestHeaders() {
    const token = localStorage.getItem('token')
    return {Authorization: token ? `Bearer ${token}` : ''}
}

function buildTimeParams(range: string[]) {
    return {
        startTime: range?.length === 2 ? range[0] : undefined,
        endTime: range?.length === 2 ? range[1] : undefined,
    }
}

/** Generic composable for one log tab (operation / auth / runtime) */
export function useLogQuery<T extends Record<string, any>, R>(
    opts: LogQueryOptions<T>,
): LogQueryReturn<T, R> {
    const loading = ref(false)
    const records = ref<R[]>([]) as Ref<R[]>
    const pagination = reactive<LogPagination>({page: 1, size: 10, total: 0})
    const searchForm = reactive<T>({...opts.initialForm}) as T

    const fetch = async () => {
        loading.value = true
        try {
            const response = await axios.get<{
                code: number;
                msg: string;
                data: { rows: R[]; total: number }
            }>(opts.endpoint, {
                params: {
                    pageNum: pagination.page,
                    pageSize: pagination.size,
                    ...searchForm,
                    ...buildTimeParams((searchForm as any).timeRange || []),
                    ...(opts.extraParams?.() || {}),
                },
                headers: requestHeaders(),
            })
            if (response.data.code === 200) {
                records.value = response.data.data.rows || []
                pagination.total = response.data.data.total || 0
            } else {
                ElMessage.error(response.data.msg || '获取日志失败')
            }
        } catch (error) {
            showRequestErrorMessage(error, '获取日志失败')
        } finally {
            loading.value = false
        }
    }

    const search = () => {
        pagination.page = 1
        fetch()
    }

    const reset = () => {
        Object.assign(searchForm, opts.initialForm)
        pagination.page = 1
        fetch()
    }

    const handleSizeChange = (value: number) => {
        pagination.size = value
        pagination.page = 1
        fetch()
    }

    const handlePageChange = (value: number) => {
        pagination.page = value
        fetch()
    }

    return {loading, records, pagination, searchForm, fetch, search, reset, handleSizeChange, handlePageChange}
}
