import {computed, reactive, ref, type Ref, watch} from 'vue'
import {ElMessage, ElMessageBox} from 'element-plus'
import {
    batchOperateHazardPoints,
    completeHazardPoint,
    createHazardPoint,
    exportHazardPoints,
    getHazardPointDetail,
    getHazardPointPage,
    pauseHazardPoint,
    updateHazardPoint,
} from '@/api/hazardPoint'
import {showRequestErrorMessage} from '@/utils/errorHandler'
import {type BoundaryCoords, deserialize, serialize} from '@/lib/boundaryCoords'

// ---------------------------------------------------------------------------
// Types (moved from HazardPoint.vue — shared with parent via return)
// ---------------------------------------------------------------------------

export interface HazardPointItem {
    id: string
    code: string
    name: string
    groupId?: string
    groupName: string
    status: string
    statusName: string
    statusColor?: string
    longitude?: number
    latitude?: number
    boundaryCoords?: string
    description?: string
    deviceCount: number
    createTime?: string
    createBy?: string
    updateBy?: string
    updateTime?: string
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

const round6 = (n: number): number => Math.round(n * 1e6) / 1e6

const normalizeHazardPoint = (item: any): HazardPointItem => ({
    id: String(item.id),
    code: item.code || '',
    name: item.name || '',
    groupId: item.groupId ? String(item.groupId) : '',
    groupName: item.groupName || '',
    status: item.status == 1 ? 'MONITORING' : item.status == 2 ? 'PAUSED' : 'COMPLETED',
    statusName: item.statusName || '',
    longitude: item.longitude,
    latitude: item.latitude,
    boundaryCoords: item.boundaryCoords,
    description: item.description,
    deviceCount: item.deviceCount || 0,
    createTime: item.createTime,
    createBy: item.createBy,
    updateBy: item.updateBy,
    updateTime: item.updateTime,
})

/** 供外部使用的枚举映射 */
export const getStatusType = (status: string): string => {
    const types: Record<string, string> = {
        MONITORING: 'success',
        PAUSED: 'warning',
        COMPLETED: 'info',
    }
    return types[status] || 'default'
}

export const getStatusTagType = (status: string): string => {
    const types: Record<string, string> = {
        NORMAL: 'success',
        FAULT: 'danger',
    }
    return types[status] || 'info'
}

export const getAlarmLevelType = (level: string): string => {
    const types: Record<string, string> = {
        '蓝色预警': 'primary',
        '黄色预警': 'warning',
        '橙色预警': 'warning',
        '红色预警': 'danger',
        '四级(注意)': 'primary',
        '三级(警示)': 'warning',
        '二级(警戒)': 'warning',
        '一级(警报)': 'danger',
    }
    return types[level] || 'default'
}

export const getChannelLabel = (channel: string): string => {
    const labels: Record<string, string> = {
        SYSTEM: '系统消息',
        SMS: '短信',
        WECHAT: '微信',
        EMAIL: '邮件',
        system: '系统消息',
        sms: '短信',
        email: '邮件',
    }
    return labels[channel] || channel
}

// ---------------------------------------------------------------------------
// Composable
// ---------------------------------------------------------------------------

export interface UseHazardPointCrudOptions {
    /** 当前选中的分组ID（由外部 useHazardPointGroups 控制） */
    groupId: Ref<string | null>
    /** 刷新分组列表的回调（增删改后统一刷新） */
    onRefreshGroups: () => void
    /** 额外操作列命令（bindDevice, alarmConfig 等），返回 false 表示未处理 */
    onExtraCommand?: (command: string, row: HazardPointItem) => boolean
}

export function useHazardPointCrud(opts: UseHazardPointCrudOptions) {
    // ── Search / Filter state ──
    const searchKeyword = ref('')
    const searchStatus = ref('')
    const searchType = ref<'name' | 'code'>('name')

    // ── Table state ──
    const loading = ref(false)
    const refreshing = ref(false)
    const tableData = ref<HazardPointItem[]>([])
    const selectedRows = ref<HazardPointItem[]>([])
    const currentPage = ref(1)
    const pageSize = ref(10)
    const total = ref(0)

    // ── Dialog state ──
    const dialogVisible = ref(false)
    const dialogTitle = ref('')
    const isEdit = ref(false)
    const formRef = ref()
    const currentRow = ref<HazardPointItem | null>(null)

    const formData = reactive({
        code: '',
        name: '',
        groupId: '',
        longitude: 104.06,
        latitude: 30.67,
        description: '',
        boundaryCoords: deserialize(null) as BoundaryCoords,
    })

    // Round lat/lng to 6 decimal places to match DB DECIMAL(10,6)
    watch(() => formData.longitude, (v) => {
        formData.longitude = round6(v)
    })
    watch(() => formData.latitude, (v) => {
        formData.latitude = round6(v)
    })

    const formRules = {
        code: [{required: true, message: '请输入隐患点编号', trigger: 'blur'}],
        name: [{required: true, message: '请输入隐患点名称', trigger: 'blur'}],
    }

    // ── Computed stats ──
    const statsTotal = computed(() => total.value)
    const statsMonitoring = computed(() => tableData.value.filter((r) => r.status === 'MONITORING').length)
    const statsDeviceTotal = computed(() => tableData.value.reduce((sum, r) => sum + (r.deviceCount || 0), 0))
    const statsGroupCount = computed(() => /* caller sets externally, placeholder */ 0)

    // ── Helpers ──
    const getStatusValue = () => {
        if (!searchStatus.value) return undefined
        const map: Record<string, number> = {MONITORING: 1, PAUSED: 2, COMPLETED: 3}
        return map[searchStatus.value]
    }

    const buildQueryParams = () => {
        const params: Record<string, any> = {
            pageNum: currentPage.value,
            pageSize: pageSize.value,
        }
        if (searchKeyword.value) {
            if (searchType.value === 'name') {
                params.name = searchKeyword.value
            } else {
                params.code = searchKeyword.value
            }
        }
        const status = getStatusValue()
        if (status !== undefined) params.status = status
        if (opts.groupId.value) params.groupId = parseInt(opts.groupId.value)
        return params
    }

    const buildPayload = () => ({
        code: formData.code,
        name: formData.name,
        groupId: formData.groupId ? Number(formData.groupId) : null,
        longitude: formData.longitude,
        latitude: formData.latitude,
        description: formData.description,
        boundaryCoords: serialize(formData.boundaryCoords),
    })

    const downloadBlobFile = (blob: Blob, fileName: string) => {
        const url = URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url
        link.download = fileName
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
        URL.revokeObjectURL(url)
    }

    const getExportFileName = (contentDisposition?: string) => {
        if (!contentDisposition) return `hazard-points-${Date.now()}.xlsx`
        const utf8Match = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i)
        if (utf8Match?.[1]) return decodeURIComponent(utf8Match[1])
        const normalMatch = contentDisposition.match(/filename="?([^";]+)"?/i)
        if (normalMatch?.[1]) return decodeURIComponent(normalMatch[1])
        return `hazard-points-${Date.now()}.xlsx`
    }

    // ── Data loading ──
    const loadTableData = async () => {
        loading.value = true
        try {
            const response: any = await getHazardPointPage(buildQueryParams())
            if (response.code === 200) {
                const data = response.data
                tableData.value = data.rows.map((item: any) => normalizeHazardPoint(item))
                total.value = data.total
            } else {
                ElMessage.error(response.msg || '获取数据失败')
            }
        } catch (error) {
            showRequestErrorMessage(error, '加载隐患点失败')
        } finally {
            loading.value = false
        }
    }

    const fetchDetail = async (id: string): Promise<HazardPointItem> => {
        const response: any = await getHazardPointDetail(id)
        if (response.code !== 200) throw new Error(response.msg || '获取详情失败')
        return normalizeHazardPoint(response.data)
    }

    // ── Search / Reset / Refresh ──
    const handleSearch = () => {
        currentPage.value = 1
        loadTableData()
    }

    const handleReset = () => {
        searchKeyword.value = ''
        searchStatus.value = ''
        searchType.value = 'name'
        opts.groupId.value = null
        currentPage.value = 1
        loadTableData()
    }

    const handleRefresh = async () => {
        refreshing.value = true
        try {
            await Promise.all([loadTableData(), opts.onRefreshGroups()])
            ElMessage.success('刷新成功')
        } catch (error) {
            showRequestErrorMessage(error, '刷新失败')
        } finally {
            refreshing.value = false
        }
    }

    // ── Pagination ──
    const handleSizeChange = () => loadTableData()
    const handlePageChange = () => loadTableData()
    const handleSelectionChange = (val: HazardPointItem[]) => {
        selectedRows.value = val
    }

    // ── Add / Edit dialog ──
    const handleAdd = () => {
        dialogTitle.value = '新增隐患点'
        isEdit.value = false
        Object.assign(formData, {
            code: '',
            name: '',
            groupId: '',
            longitude: 104.06,
            latitude: 30.67,
            description: '',
        })
        formData.boundaryCoords = deserialize(null)
        dialogVisible.value = true
    }

    const handleEdit = (row: HazardPointItem) => {
        currentRow.value = row
        dialogTitle.value = '编辑隐患点'
        isEdit.value = true
        Object.assign(formData, {
            code: row.code,
            name: row.name,
            groupId: row.groupId || '',
            longitude: row.longitude || 104.06,
            latitude: row.latitude || 30.67,
            description: row.description || '',
        })
        formData.boundaryCoords = deserialize((row as any).boundaryCoords)
        dialogVisible.value = true
    }

    const handleSubmit = async () => {
        formRef.value.validate(async (valid: boolean) => {
            if (!valid) return
            loading.value = true
            try {
                let res: any
                const payload = buildPayload()
                if (isEdit.value && currentRow.value?.id) {
                    res = await updateHazardPoint(currentRow.value.id, payload)
                } else {
                    res = await createHazardPoint(payload)
                }
                if (res.code === 200) {
                    ElMessage.success(isEdit.value ? '修改成功' : '新增成功')
                    dialogVisible.value = false
                    loadTableData()
                    opts.onRefreshGroups()
                } else {
                    ElMessage.error(res.msg || '操作失败')
                }
            } catch (error: any) {
                showRequestErrorMessage(error, '提交失败')
            } finally {
                loading.value = false
            }
        })
    }

    // ── Single row operations ──
    const handleView = async (row: HazardPointItem) => {
        loading.value = true
        try {
            currentRow.value = await fetchDetail(row.id)
            // detail dialog is opened by parent via reactive currentRow
        } catch (error) {
            showRequestErrorMessage(error, '获取详情失败')
        } finally {
            loading.value = false
        }
    }

    const handleMoreCommand = (command: string, row: HazardPointItem) => {
        // 先尝试外部回调（bindDevice, alarmConfig 等本地逻辑）
        if (opts.onExtraCommand?.(command, row)) return

        const map: Record<string, () => void> = {
            togglePause: () => handleTogglePause(row),
            complete: () => handleComplete(row),
        }
        map[command]?.()
    }

    const handleTogglePause = async (row: HazardPointItem) => {
        const pause = row.status !== 'PAUSED'
        const actionText = pause ? '停测' : '恢复'
        try {
            await ElMessageBox.confirm(`确定要${actionText}隐患点"${row.name}"吗？`, `${actionText}确认`, {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: pause ? 'warning' : 'info',
            })
            loading.value = true
            const res: any = await pauseHazardPoint(row.id, pause)
            if (res.code === 200) {
                ElMessage.success(`${actionText}成功`)
                loadTableData()
            } else {
                ElMessage.error(res.msg || `${actionText}失败`)
            }
        } catch (error: any) {
            if (error === 'cancel' || error === 'close') return
            showRequestErrorMessage(error, `${actionText}失败`)
        } finally {
            loading.value = false
        }
    }

    const handleComplete = async (row: HazardPointItem) => {
        try {
            await ElMessageBox.confirm(`确定要完结隐患点"${row.name}"吗？完结后将停止监测。`, '完结确认', {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning',
            })
            loading.value = true
            const res: any = await completeHazardPoint(row.id)
            if (res.code === 200) {
                ElMessage.success('完结成功')
                loadTableData()
            } else {
                ElMessage.error(res.msg || '完结失败')
            }
        } catch (error: any) {
            if (error === 'cancel' || error === 'close') return
            showRequestErrorMessage(error, '完结失败')
        } finally {
            loading.value = false
        }
    }

    // ── Batch operations ──
    const checkSelection = (action: string): number[] | null => {
        if (selectedRows.value.length === 0) {
            ElMessage.warning(`请先选择要${action}的隐患点`)
            return null
        }
        return selectedRows.value.map((row) => parseInt(row.id))
    }

    const handleBatchPause = async () => {
        const ids = checkSelection('停测')
        if (!ids) return
        try {
            await ElMessageBox.confirm('确定要暂停选中的隐患点监测吗？', '批量停测确认', {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning',
            })
            const res: any = await batchOperateHazardPoints(ids, 'pause')
            if (res.code === 200) {
                ElMessage.success('批量停测成功')
                loadTableData()
            } else {
                ElMessage.error(res.msg || '批量停测失败')
            }
        } catch (error: any) {
            if (error === 'cancel' || error === 'close') return
            showRequestErrorMessage(error, '批量停测失败')
        }
    }

    const handleBatchResume = async () => {
        const ids = checkSelection('恢复')
        if (!ids) return
        try {
            await ElMessageBox.confirm('确定要恢复选中的隐患点监测吗？', '批量恢复确认', {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'info',
            })
            const res: any = await batchOperateHazardPoints(ids, 'resume')
            if (res.code === 200) {
                ElMessage.success('批量恢复成功')
                loadTableData()
            } else {
                ElMessage.error(res.msg || '批量恢复失败')
            }
        } catch (error: any) {
            if (error === 'cancel' || error === 'close') return
            showRequestErrorMessage(error, '批量恢复失败')
        }
    }

    const handleBatchComplete = async () => {
        const ids = checkSelection('完结')
        if (!ids) return
        try {
            await ElMessageBox.confirm('确定要完结选中的隐患点吗？完结后将停止监测。', '批量完结确认', {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning',
            })
            const res: any = await batchOperateHazardPoints(ids, 'complete')
            if (res.code === 200) {
                ElMessage.success('批量完结成功')
                loadTableData()
            } else {
                ElMessage.error(res.msg || '批量完结失败')
            }
        } catch (error: any) {
            if (error === 'cancel' || error === 'close') return
            showRequestErrorMessage(error, '批量完结失败')
        }
    }

    // ── Export ──
    const handleExport = async () => {
        try {
            const exportPayload: Record<string, any> = {}
            const selectedIds = selectedRows.value.map((row) => parseInt(row.id))
            if (selectedIds.length > 0) {
                exportPayload.ids = selectedIds
            } else {
                const params = buildQueryParams()
                exportPayload.code = params.code
                exportPayload.name = params.name
                exportPayload.groupId = params.groupId
                exportPayload.status = params.status
            }
            const response = await exportHazardPoints(exportPayload)
            const contentType = String(response.headers['content-type'] || '')
            if (contentType.includes('application/json')) {
                const text = await response.data.text()
                const result = JSON.parse(text)
                throw new Error(result.msg || '导出失败')
            }
            const fileName = getExportFileName(response.headers['content-disposition'])
            downloadBlobFile(response.data, fileName)
            ElMessage.success(selectedIds.length > 0 ? '已按选中隐患点导出' : '已按当前筛选条件导出')
        } catch (error: any) {
            showRequestErrorMessage(error, '导出失败')
        }
    }

    return {
        // state
        searchKeyword,
        searchStatus,
        searchType,
        loading,
        refreshing,
        tableData,
        selectedRows,
        currentPage,
        pageSize,
        total,
        dialogVisible,
        dialogTitle,
        isEdit,
        formRef,
        formData,
        formRules,
        currentRow,
        // computed
        statsTotal,
        statsMonitoring,
        statsDeviceTotal,
        statsGroupCount,
        // actions
        loadTableData,
        fetchDetail,
        handleSearch,
        handleReset,
        handleRefresh,
        handleSizeChange,
        handlePageChange,
        handleSelectionChange,
        handleAdd,
        handleEdit,
        handleSubmit,
        handleView,
        handleMoreCommand,
        handleTogglePause,
        handleComplete,
        handleBatchPause,
        handleBatchResume,
        handleBatchComplete,
        handleExport,
    }
}
