import {computed, ref, watch, type Ref} from 'vue'
import {ElMessage, ElMessageBox} from 'element-plus'
import {
    bindDevicesToHazardPoint,
    getBoundDevices,
    getUnboundDevices,
    unbindDevicesFromHazardPoint,
} from '@/api/hazardPoint'
import {getDeviceIconPath} from '@/utils/deviceIcon'
import {showRequestErrorMessage} from '@/utils/errorHandler'
import type {HazardPointItem} from './useHazardPointCrud'

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

export interface BoundDevice {
    deviceId: string
    deviceCode: string
    deviceName: string
    bindTime: string
    deviceStatus: string
    onlineStatus?: number
    icon?: string
    iconPath?: string
    /**
     * 设备在隐患点内的安装坐标 (用于地图上叠加 marker)
     * - 由 getBoundDevices API 返回的 installLongitude / installLatitude 填充
     * - 缺失时地图回退到隐患点中心点
     */
    installLongitude?: number | null
    installLatitude?: number | null
    /**
     * 设备业务状态数值 (用于 deviceIcon.getDeviceIconColor 推导颜色档位)
     * - 1=正常, 2=维修, 3=停用
     */
    status?: number | null
    sensors: { id: string; name: string; iconPath: string }[]
}

export interface TreeNode {
    id: string
    key: string
    label: string
    icon?: string
    iconPath?: string
    status?: string
    statusText?: string
    bindCount?: number
    children?: TreeNode[]

    [key: string]: unknown
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

const extractDeviceIds = (checkedKeys: Array<string | number>): number[] => {
    const ids: number[] = []
    for (const k of checkedKeys) {
        const s = String(k)
        if (s.startsWith('dev_')) {
            const n = Number(s.slice(4))
            if (!Number.isNaN(n)) ids.push(n)
        }
    }
    return ids
}

// ---------------------------------------------------------------------------
// Composable
// ---------------------------------------------------------------------------

export interface UseHazardPointDeviceBindOptions {
    /** 当前选中的隐患点 */
    currentRow: Ref<HazardPointItem | null>
    /** 已绑定设备（由外部 initBoundDevices 填充） */
    boundDevices: Ref<BoundDevice[]>
    /** 保存后刷新表格 */
    onSaved: () => void
}

export function useHazardPointDeviceBind(opts: UseHazardPointDeviceBindOptions) {
    // ── Dialog state ──
    const bindDeviceDialogVisible = ref(false)
    const bindLoading = ref(false)

    // ── Transfer panel state ──
    const leftSearchText = ref('')
    const rightSearchText = ref('')
    const leftDeviceTree = ref<TreeNode[]>([])
    const rightDeviceTree = ref<TreeNode[]>([])
    const leftTreeRef = ref()
    const rightTreeRef = ref()
    // 设备勾选状态(以 device key = "dev_<id>" 为单位,不再用 el-tree 自带的 show-checkbox)
    const selectedLeftKeys = ref<Set<string>>(new Set())
    const selectedRightKeys = ref<Set<string>>(new Set())
    const initialBoundDeviceIds = ref<Set<number>>(new Set())

    // ── Filter ──
    const filterLeftNode = (value: string, data: TreeNode) => {
        if (!value) return true
        return data.label.toLowerCase().includes(value.toLowerCase())
    }

    const filterRightNode = (value: string, data: TreeNode) => {
        if (!value) return true
        return data.label.toLowerCase().includes(value.toLowerCase())
    }

    // ── 勾选状态辅助函数 ──
    // 传感器节点(data.disabled === true)永远不视为已勾选
    const isLeftNodeChecked = (data: TreeNode): boolean =>
        !data.disabled && selectedLeftKeys.value.has(data.key)

    const isRightNodeChecked = (data: TreeNode): boolean =>
        !data.disabled && selectedRightKeys.value.has(data.key)

    const toggleLeftNode = (data: TreeNode, checked: boolean): void => {
        if (data.disabled) return
        const next = new Set(selectedLeftKeys.value)
        if (checked) next.add(data.key)
        else next.delete(data.key)
        selectedLeftKeys.value = next
    }

    const toggleRightNode = (data: TreeNode, checked: boolean): void => {
        if (data.disabled) return
        const next = new Set(selectedRightKeys.value)
        if (checked) next.add(data.key)
        else next.delete(data.key)
        selectedRightKeys.value = next
    }

    // ── Load data ──
    const loadUnboundDevices = async (keyword?: string) => {
        if (!opts.currentRow.value) return []
        try {
            type UnboundDeviceItem = { id: number; label: string; bindCount: number; status: number; onlineStatus: number; icon: string; iconPath: string; children?: { id: number; label: string; iconPath: string; status: number }[] }
            const response = await getUnboundDevices(opts.currentRow.value.id, keyword)
            if (response.code === 200) {
                return (response.data as UnboundDeviceItem[])
                    .filter((item) => !item.bindCount || item.bindCount === 0)
                    .map((item) => ({
                    id: String(item.id),
                    key: `dev_${item.id}`,
                    label: item.label,
                    bindCount: item.bindCount,
                    status: String(item.status),
                    onlineStatus: item.onlineStatus,
                    iconPath: getDeviceIconPath({icon: item.icon, iconPath: item.iconPath, status: item.status, onlineStatus: item.onlineStatus}),
                    children:
                        item.children?.map((child) => ({
                            id: String(child.id),
                            key: `sen_${item.id}_${child.id}`,
                            label: child.label,
                            iconPath: child.iconPath,
                            status: String(child.status),
                            disabled: true,
                        })) || [],
                }))
            }
            return []
        } catch {
            return []
        }
    }

    const handleSearchUnboundDevices = async () => {
        if (!opts.currentRow.value) return
        leftDeviceTree.value = await loadUnboundDevices(leftSearchText.value)
    }

    const initBoundDevices = async (hazardPointId: string) => {
        try {
            type BoundDeviceRaw = { deviceId?: number; id?: number; deviceCode: string; deviceName: string; bindTime: string; deviceStatus: number; onlineStatus: number; icon: string; iconPath: string; installLongitude?: number; installLatitude?: number; sensors: { id: number; name: string; iconPath: string }[] }
            const response = await getBoundDevices(hazardPointId)
            if (response.code === 200) {
                opts.boundDevices.value = (response.data as BoundDeviceRaw[]).map((item) => ({
                    deviceId: String(item.deviceId || item.id),
                    deviceCode: item.deviceCode,
                    deviceName: item.deviceName,
                    bindTime: item.bindTime,
                    deviceStatus: item.deviceStatus === 1 ? 'NORMAL' : item.deviceStatus === 2 ? 'FAULT' : 'DISABLED',
                    onlineStatus: item.onlineStatus ?? 0,
                    icon: item.icon || 'device',
                    iconPath: item.iconPath,
                    installLongitude: item.installLongitude ?? null,
                    installLatitude: item.installLatitude ?? null,
                    status: item.deviceStatus ?? null,
                    sensors: (item.sensors || []).map((s) => ({ id: String(s.id), name: s.name, iconPath: s.iconPath })),
                }))
            } else {
                opts.boundDevices.value = []
            }
        } catch {
            opts.boundDevices.value = []
        }
    }

    const refreshDeviceLists = async () => {
        if (!opts.currentRow.value) return
        await initBoundDevices(opts.currentRow.value.id)
        const unbound = await loadUnboundDevices()
        leftDeviceTree.value = unbound
        rightDeviceTree.value = opts.boundDevices.value.map((device) => {
            const statusCode = device.deviceStatus === 'NORMAL' ? 1 : device.deviceStatus === 'FAULT' ? 2 : 3
            return {
                id: String(device.deviceId),
                key: `dev_${device.deviceId}`,
                label: `${device.deviceCode} - ${device.deviceName}`,
                iconPath: getDeviceIconPath({icon: device.icon, iconPath: device.iconPath, status: statusCode, onlineStatus: device.onlineStatus}),
                status: String(statusCode),
                sensorCount: device.sensors.length,
                children: device.sensors.map((sensor) => ({
                    id: String(sensor.id),
                    key: `sen_${device.deviceId}_${sensor.id}`,
                    label: sensor.name,
                    iconPath: sensor.iconPath,
                    disabled: true,
                })),
            }
        })
    }

    // ── Open dialog ──
    const handleBindDevice = async (row: HazardPointItem) => {
        opts.currentRow.value = row
        bindDeviceDialogVisible.value = true
        await refreshDeviceLists()
        initialBoundDeviceIds.value = new Set(
            opts.boundDevices.value.map((d) => Number(d.deviceId)).filter((id) => !Number.isNaN(id)),
        )
        selectedLeftKeys.value = new Set()
        selectedRightKeys.value = new Set()
    }

    // ── Transfer operations ──
    const transferToRight = () => {
        const deviceIds = extractDeviceIds(Array.from(selectedLeftKeys.value))
        if (deviceIds.length === 0) {
            ElMessage.warning('请选择要绑定的设备')
            return
        }
        const movedIds = new Set(deviceIds.map(String))
        const moved = leftDeviceTree.value.filter((node) => movedIds.has(node.id))
        leftDeviceTree.value = leftDeviceTree.value.filter((node) => !movedIds.has(node.id))
        rightDeviceTree.value = [...rightDeviceTree.value, ...moved]
        selectedLeftKeys.value = new Set()
    }

    const transferToLeft = () => {
        const deviceIds = extractDeviceIds(Array.from(selectedRightKeys.value))
        if (deviceIds.length === 0) {
            ElMessage.warning('请选择要解绑的设备')
            return
        }
        const movedIds = new Set(deviceIds.map(String))
        const moved = rightDeviceTree.value.filter((node) => movedIds.has(node.id))
        rightDeviceTree.value = rightDeviceTree.value.filter((node) => !movedIds.has(node.id))
        leftDeviceTree.value = [...leftDeviceTree.value, ...moved]
        selectedRightKeys.value = new Set()
    }

    const transferAllToRight = async () => {
        if (leftDeviceTree.value.length === 0) {
            ElMessage.warning('没有可绑定的设备')
            return
        }
        try {
            await ElMessageBox.confirm(
                `确定要将左侧 ${leftDeviceTree.value.length} 台设备全部加入待绑定列表吗？（点击"确定"后生效）`,
                '批量绑定确认',
                {type: 'warning', confirmButtonText: '全部加入', cancelButtonText: '取消'},
            )
        } catch {
            return
        }
        rightDeviceTree.value = [...rightDeviceTree.value, ...leftDeviceTree.value]
        leftDeviceTree.value = []
        selectedLeftKeys.value = new Set()
    }

    const transferAllToLeft = async () => {
        if (rightDeviceTree.value.length === 0) {
            ElMessage.warning('没有可解绑的设备')
            return
        }
        try {
            await ElMessageBox.confirm(
                `确定要将当前隐患点的 ${rightDeviceTree.value.length} 台设备全部移到待绑定列表吗？（点击"确定"后生效）`,
                '批量解绑确认',
                {type: 'warning', confirmButtonText: '全部移除', cancelButtonText: '取消'},
            )
        } catch {
            return
        }
        leftDeviceTree.value = [...leftDeviceTree.value, ...rightDeviceTree.value]
        rightDeviceTree.value = []
        selectedRightKeys.value = new Set()
    }

    // ── Submit ──
    const handleBindDeviceSubmit = async () => {
        if (!opts.currentRow.value) {
            bindDeviceDialogVisible.value = false
            return
        }

        const finalIds = new Set<number>()
        for (const node of rightDeviceTree.value) {
            const n = Number(node.id)
            if (!Number.isNaN(n)) finalIds.add(n)
        }

        const toBind: number[] = []
        for (const id of finalIds) {
            if (!initialBoundDeviceIds.value.has(id)) toBind.push(id)
        }
        const toUnbind: number[] = []
        for (const id of initialBoundDeviceIds.value) {
            if (!finalIds.has(id)) toUnbind.push(id)
        }

        if (toBind.length === 0 && toUnbind.length === 0) {
            bindDeviceDialogVisible.value = false
            return
        }

        bindLoading.value = true
        try {
            if (toUnbind.length > 0) {
                const unbindResp = await unbindDevicesFromHazardPoint(opts.currentRow.value.id, toUnbind)
                if (unbindResp.code !== 200) {
                    ElMessage.error(unbindResp.msg || '解绑失败')
                    return
                }
            }
            if (toBind.length > 0) {
                const bindResp = await bindDevicesToHazardPoint(opts.currentRow.value.id, {deviceIds: toBind})
                if (bindResp.code !== 200) {
                    ElMessage.error(bindResp.msg || '绑定失败')
                    return
                }
            }
            ElMessage.success(`保存成功（新增绑定 ${toBind.length}，解绑 ${toUnbind.length}）`)
            bindDeviceDialogVisible.value = false
            opts.onSaved()
        } catch (error) {
            showRequestErrorMessage(error, '保存失败')
        } finally {
            bindLoading.value = false
        }
    }

    // 已完结或停测状态下，只能查看已绑定关系，禁止修改
    const isBindReadonly = computed(() => {
        const s = opts.currentRow.value?.status
        return s === 'COMPLETED' || s === 'PAUSED'
    })

    // 搜索过滤：监听输入文本触发 el-tree 过滤
    watch(leftSearchText, (v) => leftTreeRef.value?.filter(v))
    watch(rightSearchText, (v) => rightTreeRef.value?.filter(v))

    return {
        // state
        bindDeviceDialogVisible,
        isBindReadonly,
        bindLoading,
        leftSearchText,
        rightSearchText,
        leftDeviceTree,
        rightDeviceTree,
        leftTreeRef,
        rightTreeRef,
        selectedLeftKeys,
        selectedRightKeys,
        // actions
        filterLeftNode,
        filterRightNode,
        isLeftNodeChecked,
        isRightNodeChecked,
        toggleLeftNode,
        toggleRightNode,
        handleSearchUnboundDevices,
        initBoundDevices,
        refreshDeviceLists,
        handleBindDevice,
        transferToRight,
        transferToLeft,
        transferAllToRight,
        transferAllToLeft,
        handleBindDeviceSubmit,
    }
}
