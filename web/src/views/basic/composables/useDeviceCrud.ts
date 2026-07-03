import {reactive, ref} from 'vue'
import {ElMessage, ElMessageBox} from 'element-plus'
import {showRequestErrorMessage} from '@/utils/errorHandler'
import {
    createDevice as createDeviceApi,
    deleteDevice as deleteDeviceApi,
    exportDevices,
    type DeviceItem,
    type DevicePageParams,
    getDeviceDetail,
    getDevicePage,
    updateDevice as updateDeviceApi,
} from '@/api/device'
import {type SensorItem} from '@/api/sensor'

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

export type {DeviceItem}

// ---------------------------------------------------------------------------
// Composable
// ---------------------------------------------------------------------------

export function useDeviceCrud() {
    // ── Search / Filter ──
    const searchKeyword = ref('')
    const searchStatus = ref<number | ''>('')
    const searchHazardPointId = ref<number | ''>('')

    // ── Table state ──
    const loading = ref(false)
    const refreshing = ref(false)
    const submitLoading = ref(false)
    const tableData = ref<DeviceItem[]>([])
    const currentPage = ref(1)
    const pageSize = ref(10)
    const total = ref(0)

    // ── Form dialog ──
    const dialogVisible = ref(false)
    const dialogTitle = ref('')
    const isEdit = ref(false)
    const isView = ref(false)
    const isCopyMode = ref(false)
    const formRef = ref()
    const currentRow = ref<DeviceItem | null>(null)

    const formData = reactive<{
        id?: number
        code: string
        name: string
        sn: string
        deviceType: number | null
        networkType: number | null
        protocolType: string
        vendorName: string
        icon: string
        iconPath: string
        longitude: number | null
        latitude: number | null
        status: number
        sensorList: SensorItem[]
        boundHazardPointId: number | null
    }>({
        code: '',
        name: '',
        sn: '',
        deviceType: 0,
        networkType: 0,
        protocolType: 'MQTT',
        vendorName: '',
        icon: '',
        iconPath: '',
        boundHazardPointId: null,
        longitude: null,
        latitude: null,
        status: 1,
        sensorList: [],
    })

    const formRules = {
        code: [{required: true, message: '请输入设备编号', trigger: 'blur'}],
        name: [{required: true, message: '请输入设备名称', trigger: 'blur'}],
    }

    // ── Detail dialog ──
    const detailDialogVisible = ref(false)
    const detailPwdVisible = ref(false)
    const detailTab = ref('info')

    // ── Helpers ──
    const getStatusType = (status: number) => ({1: 'success', 2: 'danger', 3: 'info'}[status] || 'default')
    const getStatusLabel = (status: number) => ({1: '正常', 2: '维修', 3: '停用'}[status] || '未知')

    const copyPwd = async (pwd: string) => {
        try {
            await navigator.clipboard.writeText(pwd)
            ElMessage.success('密码已复制')
        } catch {
            ElMessage.warning('复制失败，请手动复制')
        }
    }

    const formatCoord = (lng?: number | null, lat?: number | null) => {
        if (lng == null || lat == null) return '-'
        return `${lng.toFixed(6)}, ${lat.toFixed(6)}`
    }

    const nowString = () => {
        const d = new Date()
        const pad = (n: number) => n.toString().padStart(2, '0')
        return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
    }

    // ── Data loading ──
    const loadTableData = async () => {
        loading.value = true
        try {
            const params: DevicePageParams = {pageNum: currentPage.value, pageSize: pageSize.value}
            if (searchKeyword.value) params.code = searchKeyword.value
            if (searchStatus.value !== '') params.status = searchStatus.value as number
            if (searchHazardPointId.value !== '') params.hazardPointId = searchHazardPointId.value as number
            const data = await getDevicePage(params)
            tableData.value = data.rows || []
            total.value = data.total || 0
        } catch (error) {
            showRequestErrorMessage(error, '加载设备列表失败')
        } finally {
            loading.value = false
        }
    }

    const fetchDetail = async (id: number) => {
        loading.value = true
        try {
            return await getDeviceDetail(id)
        } catch (error) {
            showRequestErrorMessage(error, '获取设备详情失败')
            return null
        } finally {
            loading.value = false
        }
    }

    // ── Search / Pagination ──
    const handleSearch = () => {
        currentPage.value = 1;
        loadTableData()
    }
    const handleReset = () => {
        searchKeyword.value = '';
        searchStatus.value = '';
        searchHazardPointId.value = '';
        currentPage.value = 1;
        loadTableData()
    }
    const handleRefresh = async () => {
        refreshing.value = true
        try {
            await loadTableData();
            ElMessage.success('刷新成功')
        } catch (error) {
            showRequestErrorMessage(error, '刷新失败')
        } finally {
            refreshing.value = false
        }
    }
    const handleSizeChange = () => loadTableData()
    const handlePageChange = () => loadTableData()

    // ── Add / Edit ──
    const handleAdd = () => {
        dialogTitle.value = '新增设备';
        isEdit.value = false;
        isView.value = false
        isCopyMode.value = false
        Object.assign(formData, {
            id: undefined,
            code: '',
            name: '',
            sn: '',
            deviceType: 0,
            networkType: 0,
            protocolType: 'MQTT',
            vendorName: '',
            icon: '',
            iconPath: '',
            longitude: null,
            latitude: null,
            status: 1,
            boundHazardPointId: null,
            sensorList: []
        })
        dialogVisible.value = true
    }

    const handleEdit = async (row: DeviceItem) => {
        dialogTitle.value = '编辑设备';
        isEdit.value = true;
        isView.value = false
        isCopyMode.value = false
        Object.assign(formData, {
            id: row.id,
            code: row.code,
            name: row.name,
            sn: row.sn || '',
            deviceType: row.deviceType ?? 0,
            networkType: row.networkType ?? 0,
            protocolType: row.protocolType || 'MQTT',
            vendorName: row.vendorName || '',
            icon: row.icon || '',
            iconPath: row.iconPath || '',
            longitude: row.longitude ?? null,
            latitude: row.latitude ?? null,
            boundHazardPointId: row.boundHazardPointId ?? null,
            status: row.status,
            sensorList: []
        })
        dialogVisible.value = true
    }

    /** 计算复制后缀：原值末尾 _数字 递增，否则追加 _1 */
    const computeCopySuffix = (original: string): string => {
        const match = original.match(/^(.+)_(\d+)$/)
        if (match) {
            const base = match[1]
            const num = parseInt(match[2]) + 1
            return `${base}_${num}`
        }
        return `${original}_1`
    }

    /** 打开复制弹窗（复用新增/编辑弹窗，预填原设备全部字段） */
    const openCopyDialog = (row: DeviceItem) => {
        dialogTitle.value = '复制设备'
        isEdit.value = false
        isView.value = false
        isCopyMode.value = true
        currentRow.value = row // 记住源设备，handleSubmit 需要其 ID
        const suggestedCode = computeCopySuffix(row.code || '')
        const suggestedName = computeCopySuffix(row.name || '')
        Object.assign(formData, {
            id: undefined,
            code: suggestedCode,
            name: suggestedName,
            sn: row.sn || '',
            deviceType: row.deviceType ?? 0,
            networkType: row.networkType ?? 0,
            protocolType: row.protocolType || 'MQTT',
            vendorName: row.vendorName || '',
            icon: row.icon || '',
            iconPath: row.iconPath || '',
            longitude: row.longitude ?? null,
            latitude: row.latitude ?? null,
            boundHazardPointId: row.boundHazardPointId ?? null,
            status: row.status,
            sensorList: [],
        })
        dialogVisible.value = true
    }

    const validateDeviceIdentity = () => {
        const code = formData.code?.trim()
        const sn = formData.sn?.trim()
        const excludeId = formData.id
        // 复制模式下同时排除源设备，允许 SN 保持一致
        const sourceId = isCopyMode.value ? currentRow.value?.id : undefined
        if (code && tableData.value.find((d) => d.id !== excludeId && d.id !== sourceId && d.code === code)) {
            ElMessage.warning(`设备编号 ${code} 已被占用`);
            return false
        }
        if (sn && tableData.value.find((d) => d.id !== excludeId && d.id !== sourceId && d.sn === sn)) {
            ElMessage.warning(`设备 SN ${sn} 已被占用`);
            return false
        }
        return true
    }

    const createDevice = async () => {
        submitLoading.value = true
        try {
            const result = await createDeviceApi({
                code: formData.code,
                name: formData.name,
                sn: formData.sn || undefined,
                deviceType: formData.deviceType!,
                networkType: formData.networkType!,
                protocolType: formData.protocolType,
                vendorName: formData.vendorName || undefined,
                icon: formData.icon,
                iconPath: formData.iconPath,
                longitude: formData.longitude,
                latitude: formData.latitude,
                status: formData.status,
                boundHazardPointId: formData.boundHazardPointId ?? undefined,
            })
            ElMessage.success('新增成功');
            dialogVisible.value = false;
            await loadTableData()
            return result
        } catch (error) {
            showRequestErrorMessage(error, '新增设备失败');
            return null
        } finally {
            submitLoading.value = false
        }
    }

    const updateDevice = async () => {
        submitLoading.value = true
        try {
            await updateDeviceApi(Number(formData.id), {
                name: formData.name,
                sn: formData.sn || undefined,
                deviceType: formData.deviceType!,
                networkType: formData.networkType!,
                protocolType: formData.protocolType,
                vendorName: formData.vendorName || undefined,
                icon: formData.icon,
                iconPath: formData.iconPath,
                longitude: formData.longitude,
                latitude: formData.latitude,
                status: formData.status,
                boundHazardPointId: formData.boundHazardPointId ?? undefined,
            })
            ElMessage.success('修改成功');
            dialogVisible.value = false;
            await loadTableData()
        } catch (error) {
            showRequestErrorMessage(error, '修改设备失败')
        } finally {
            submitLoading.value = false
        }
    }

    // ── Draft state（新增/复制模式下缓存设备信息，待传感器配置完成后统一提交）──
    const draftMode = ref<'add' | 'copy' | null>(null)
    const draftSourceId = ref<number | null>(null)
    const draftCopySensors = ref(false)

    const clearDraft = () => {
        draftMode.value = null
        draftSourceId.value = null
        draftCopySensors.value = false
    }

    const handleSubmit = (copySensors?: boolean) => {
        formRef.value.validate((valid: boolean) => {
            if (!valid || !validateDeviceIdentity()) return

            // 编辑模式：直接提交（保持现有逻辑）
            if (isEdit.value) {
                updateDevice()
                return
            }

            // 复制模式：缓存草稿，通知父组件进入传感器配置阶段
            if (isCopyMode.value) {
                draftSourceId.value = currentRow.value?.id ?? null
                draftCopySensors.value = copySensors ?? true
                draftMode.value = 'copy'
                dialogVisible.value = false
                return
            }

            // 新增模式：缓存草稿，通知父组件进入传感器配置阶段
            draftMode.value = 'add'
            dialogVisible.value = false
        })
    }

    const handleView = async (row: DeviceItem) => {
        detailPwdVisible.value = false;
        currentRow.value = row
        const detail = await fetchDetail(Number(row.id))
        if (detail) currentRow.value = detail
        detailDialogVisible.value = true
    }

    // ── Delete / Copy ──
    const handleDelete = (row: DeviceItem) => {
        ElMessageBox.confirm(`确定要删除设备"${row.name}"吗?`, '删除确认', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
        })
            .then(async () => {
                await deleteDeviceApi(Number(row.id));
                ElMessage.success('删除成功');
                await loadTableData()
            })
            .catch(() => {
            })
    }

    const handleExport = async () => {
        try {
            const response = await exportDevices()
            const disposition = String(response.headers['content-disposition'] || '')
            const fileName = disposition
                ? decodeURIComponent(disposition.split("filename*=UTF-8''")[1] || disposition.split('filename=')[1]?.replace(/"/g, '') || '设备数据.xlsx')
                : '设备数据.xlsx'
            const blob = response.data instanceof Blob ? response.data : new Blob([response.data])
            const url = URL.createObjectURL(blob)
            const a = document.createElement('a')
            a.href = url
            a.download = fileName
            a.click()
            URL.revokeObjectURL(url)
            ElMessage.success('导出成功')
        } catch (error) {
            showRequestErrorMessage(error, '导出失败')
        }
    }

    const handleMoreCommand = (command: string, row: DeviceItem) => {
        // handled by parent with local callbacks
        return {command, row}
    }

    return {
        searchKeyword, searchStatus, searchHazardPointId,
        loading, refreshing, submitLoading, tableData, currentPage, pageSize, total,
        dialogVisible, dialogTitle, isEdit, isView, isCopyMode, formRef, formData, formRules,
        detailDialogVisible, detailPwdVisible, detailTab, currentRow,
        getStatusType, getStatusLabel, copyPwd, formatCoord, nowString,
        loadTableData, fetchDetail,
        handleSearch, handleReset, handleRefresh, handleSizeChange, handlePageChange,
        handleAdd, handleEdit, handleView, handleSubmit, handleDelete, handleExport,
        handleMoreCommand, createDevice, openCopyDialog,
        draftMode, draftSourceId, draftCopySensors, clearDraft,
    }
}
