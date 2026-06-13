import {computed, reactive, ref, type Ref} from 'vue'
import {ElMessage, ElMessageBox} from 'element-plus'
import type {HazardPointItem} from './useHazardPointCrud'

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

export interface AlarmCriteria {
    id: string
    name: string
    deviceId: string
    deviceName: string
    monitorTypeId: string
    monitorTypeName: string
    monitorContentCode: string
    monitorContentName: string
    expression: string
    alarmLevel: string
    alarmLevelText: string
    isEnabled: boolean
}

export interface DispatchRule {
    id: string
    type: 'alarm' | 'offline'
    level: string[]
    deviceIds: string[]
    deviceNames?: string[]
    persons: string[]
    channels: string[]
    execTime: string
    status: number
    remark: string
}

// ---------------------------------------------------------------------------
// Composable
// ---------------------------------------------------------------------------

export interface UseHazardPointAlarmOptions {
    /** 当前选中的隐患点 */
    currentRow: Ref<HazardPointItem | null>
    /** 已绑定的设备列表（用于告警判据设备选择） */
    boundDevices: Ref<{ deviceId: string; deviceName: string }[]>
}

export function useHazardPointAlarm(opts: UseHazardPointAlarmOptions) {
    // ── Alarm criteria dialog state ──
    const alarmConfigDialogVisible = ref(false)
    const alarmDialogVisible = ref(false)
    const isEditAlarm = ref(false)
    const alarmFormRef = ref()
    const alarmCriteriaList = ref<AlarmCriteria[]>([])
    const currentEditingAlarmLevel = ref('')

    const alarmFormData = reactive({
        id: '',
        name: '',
        deviceId: '',
        deviceName: '',
        monitorTypeId: '',
        monitorTypeName: '',
        monitorContentCode: '',
        monitorContentName: '',
        unit: '',
        blueExpression: '',
        blueDescription: '',
        yellowExpression: '',
        yellowDescription: '',
        orangeExpression: '',
        orangeDescription: '',
        redExpression: '',
        redDescription: '',
    })

    const alarmFormRules = {
        name: [{required: true, message: '请输入判据名称', trigger: 'blur'}],
        deviceId: [{required: true, message: '请选择设备', trigger: 'blur'}],
        monitorTypeId: [{required: true, message: '请选择监测类型', trigger: 'blur'}],
        monitorContentCode: [{required: true, message: '请选择监测内容', trigger: 'blur'}],
    }

    // ── Mock monitor type list ──
    const monitorTypeList = ref<
        { id: string; name: string; code: string; contents: { value: string; label: string; unit: string }[] }[]
    >([
        {
            id: '1', name: '地表位移监测', code: 'DISPLACEMENT', contents: [
                {value: 'displacement_x', label: 'X方向位移', unit: 'mm'},
                {value: 'displacement_y', label: 'Y方向位移', unit: 'mm'},
                {value: 'displacement_z', label: 'Z方向位移', unit: 'mm'},
                {value: 'total_displacement', label: '总位移', unit: 'mm'},
            ],
        },
        {
            id: '2', name: '裂缝监测', code: 'CRACK', contents: [
                {value: 'crack_width', label: '裂缝宽度', unit: 'mm'},
                {value: 'crack_length', label: '裂缝长度', unit: 'm'},
                {value: 'crack_depth', label: '裂缝深度', unit: 'm'},
            ],
        },
        {
            id: '3', name: '雨量监测', code: 'RAINFALL', contents: [
                {value: 'rainfall_hour', label: '小时雨量', unit: 'mm'},
                {value: 'rainfall_day', label: '日雨量', unit: 'mm'},
                {value: 'rainfall_week', label: '周雨量', unit: 'mm'},
                {value: 'rainfall_month', label: '月雨量', unit: 'mm'},
            ],
        },
        {
            id: '4', name: '水位监测', code: 'WATER_LEVEL', contents: [
                {value: 'water_level', label: '水位', unit: 'm'},
                {value: 'water_temp', label: '水温', unit: '℃'},
                {value: 'water_pressure', label: '水压', unit: 'kPa'},
            ],
        },
        {
            id: '5', name: '地温监测', code: 'SOIL_TEMP', contents: [
                {value: 'soil_temp_10cm', label: '10cm地温', unit: '℃'},
                {value: 'soil_temp_30cm', label: '30cm地温', unit: '℃'},
                {value: 'soil_temp_50cm', label: '50cm地温', unit: '℃'},
            ],
        },
        {
            id: '6', name: '含水率监测', code: 'MOISTURE', contents: [
                {value: 'soil_moisture', label: '土壤含水率', unit: '%'},
                {value: 'volumetric_water', label: '体积含水率', unit: '%'},
            ],
        },
        {
            id: '7', name: '倾斜监测', code: 'INCLINATION', contents: [
                {value: 'inclination_x', label: 'X方向倾角', unit: '°'},
                {value: 'inclination_y', label: 'Y方向倾角', unit: '°'},
                {value: 'total_inclination', label: '总倾角', unit: '°'},
            ],
        },
        {
            id: '8', name: '应力应变监测', code: 'STRESS', contents: [
                {value: 'axial_stress', label: '轴向应力', unit: 'MPa'},
                {value: 'radial_stress', label: '径向应力', unit: 'MPa'},
                {value: 'strain', label: '应变', unit: 'με'},
            ],
        },
    ])

    const filteredMonitorContent = computed(() => {
        if (!alarmFormData.monitorTypeId) return []
        const mt = monitorTypeList.value.find((t) => t.id === alarmFormData.monitorTypeId)
        return mt ? mt.contents : []
    })

    // ── Dispatch rule dialog state ──
    const dispatchRules = ref<DispatchRule[]>([])
    const dispatchDialogVisible = ref(false)
    const isEditDispatch = ref(false)
    const dispatchFormRef = ref()

    const dispatchFormData = reactive({
        id: '',
        hazardPointId: '',
        type: 'alarm' as 'alarm' | 'offline',
        level: [] as string[],
        deviceIds: [] as string[],
        persons: [] as string[],
        channels: ['system'] as string[],
        execTime: '',
        execType: 'realtime' as 'realtime' | 'timed',
        execFrequencyNum: 1,
        execFrequencyUnit: 'hour' as 'minute' | 'hour' | 'day' | 'week' | 'month' | 'year',
        execTimePoints: '',
        status: 1 as 0 | 1,
        remark: '',
    })

    const dispatchFormRules = {
        type: [{required: true, message: '请选择类型', trigger: 'change'}],
        level: [{required: true, type: 'array', min: 1, message: '请选择告警等级', trigger: 'change'}],
        deviceIds: [{required: true, type: 'array', min: 1, message: '请选择设备', trigger: 'change'}],
        persons: [{required: true, type: 'array', min: 1, message: '请选择通知人员', trigger: 'change'}],
        channels: [{required: true, type: 'array', min: 1, message: '请选择通知渠道', trigger: 'change'}],
    }

    // ── Mock user list ──
    const userList = ref<{ id: string; name: string; phone: string }[]>([
        {id: '1', name: '张三', phone: '13923755477'},
        {id: '2', name: '李四', phone: '13558981389'},
        {id: '3', name: '王强', phone: '13889771288'},
        {id: '4', name: '陈经理', phone: '13900001111'},
    ])

    // ── Init (stubs, pending real API) ──
    const initAlarmCriteria = (_hazardPointId: string) => {
        alarmCriteriaList.value = []
    }

    const initDispatchRules = (_hazardPointId: string) => {
        dispatchRules.value = []
    }

    // ── Alarm criteria CRUD ──
    const handleConfigAlarm = (row: HazardPointItem) => {
        opts.currentRow.value = row
        initAlarmCriteria(row.id)
        initDispatchRules(row.id)
        alarmConfigDialogVisible.value = true
    }

    const handleAddAlarmCriteria = () => {
        isEditAlarm.value = false
        Object.assign(alarmFormData, {
            id: '',
            name: '',
            deviceId: '',
            deviceName: '',
            monitorContentCode: '',
            monitorContentName: '',
            blueExpression: '',
            blueDescription: '',
            yellowExpression: '',
            yellowDescription: '',
            orangeExpression: '',
            orangeDescription: '',
            redExpression: '',
            redDescription: '',
        })
        alarmDialogVisible.value = true
    }

    const handleEditAlarm = (row: AlarmCriteria) => {
        isEditAlarm.value = true
        Object.assign(alarmFormData, {
            id: row.id,
            name: row.name,
            deviceId: row.deviceId,
            deviceName: row.deviceName,
            monitorTypeId: row.monitorTypeId,
            monitorTypeName: row.monitorTypeName,
            monitorContentCode: row.monitorContentCode,
            monitorContentName: row.monitorContentName,
            unit: '',
        })
        alarmDialogVisible.value = true
    }

    const handleAlarmDeviceChange = (val: string) => {
        const device = opts.boundDevices.value.find((d) => d.deviceId === val)
        if (device) alarmFormData.deviceName = device.deviceName
    }

    const handleMonitorTypeChange = (val: string) => {
        const mt = monitorTypeList.value.find((t) => t.id === val)
        if (mt) {
            alarmFormData.monitorTypeName = mt.name
            alarmFormData.monitorContentCode = ''
            alarmFormData.monitorContentName = ''
            alarmFormData.unit = ''
        }
    }

    const handleMonitorContentChange = (val: string) => {
        const mt = monitorTypeList.value.find((t) => t.id === alarmFormData.monitorTypeId)
        if (mt) {
            const content = mt.contents.find((c) => c.value === val)
            if (content) {
                alarmFormData.monitorContentName = content.label
                alarmFormData.unit = content.unit
            }
        }
    }

    const insertExpression = (text: string) => {
        if (currentEditingAlarmLevel.value) {
            const field = currentEditingAlarmLevel.value + 'Expression'
            ;(alarmFormData as any)[field] += text
        }
    }

    const handleAlarmSubmit = () => {
        alarmFormRef.value.validate((valid: boolean) => {
            if (valid) {
                ElMessage.success(isEditAlarm.value ? '判据修改成功' : '判据添加成功')
                alarmDialogVisible.value = false
                if (opts.currentRow.value) initAlarmCriteria(opts.currentRow.value.id)
            }
        })
    }

    const handleToggleAlarm = (row: AlarmCriteria) => {
        ElMessage.success(`判据${row.isEnabled ? '启用' : '停用'}成功`)
    }

    const handleDeleteAlarm = (row: AlarmCriteria) => {
        ElMessageBox.confirm(`确定要删除判据"${row.name}"吗?`, '删除确认', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning',
        })
            .then(() => {
                ElMessage.success('删除成功')
                if (opts.currentRow.value) initAlarmCriteria(opts.currentRow.value.id)
            })
            .catch(() => {
            })
    }

    // ── Dispatch rule CRUD ──
    const handleAddDispatchRule = () => {
        isEditDispatch.value = false
        Object.assign(dispatchFormData, {
            id: '',
            hazardPointId: opts.currentRow.value?.id || '',
            type: 'alarm',
            level: [],
            deviceIds: [],
            persons: [],
            channels: ['system'],
            execTime: '',
            status: 1,
            remark: '',
        })
        dispatchDialogVisible.value = true
    }

    const handleEditDispatchRule = (row: DispatchRule) => {
        isEditDispatch.value = true
        const execTime = row.execTime || ''
        let execType: 'realtime' | 'timed' = 'realtime'
        let execFrequencyNum = 1
        let execFrequencyUnit: 'minute' | 'hour' | 'day' | 'week' | 'month' | 'year' = 'hour'
        let execTimePoints = ''
        if (execTime) {
            const parts = execTime.split('|')
            if (parts.length === 2) {
                execType = 'timed'
                execFrequencyUnit = parts[0] as typeof execFrequencyUnit
                execTimePoints = parts[1]
            }
        }
        Object.assign(dispatchFormData, {
            id: row.id,
            hazardPointId: opts.currentRow.value?.id || '',
            type: row.type,
            level: row.level || [],
            deviceIds: row.deviceIds || [],
            persons: row.persons || [],
            channels: row.channels || ['system'],
            execTime,
            execType,
            execFrequencyNum,
            execFrequencyUnit,
            execTimePoints,
            status: row.status || 1,
            remark: row.remark || '',
        })
        dispatchDialogVisible.value = true
    }

    const handleDispatchSubmit = () => {
        dispatchFormRef.value.validate((valid: boolean) => {
            if (!valid) return
            let execTimeValue = ''
            if (dispatchFormData.execType === 'timed' && dispatchFormData.execTimePoints) {
                execTimeValue = `${dispatchFormData.execFrequencyUnit}|${dispatchFormData.execTimePoints}`
            }
            dispatchFormData.execTime = execTimeValue
            ElMessage.success(isEditDispatch.value ? '规则修改成功' : '规则添加成功')
            dispatchDialogVisible.value = false
            if (opts.currentRow.value) initDispatchRules(opts.currentRow.value.id)
        })
    }

    const handleDeleteDispatchRule = (row: DispatchRule) => {
        const ruleDesc = row.remark || (row.type === 'alarm' ? '监测告警规则' : '设备离线通知规则')
        ElMessageBox.confirm(`确定要删除规则"${ruleDesc}"吗?`, '删除确认', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning',
        })
            .then(() => {
                ElMessage.success('删除成功')
                if (opts.currentRow.value) initDispatchRules(opts.currentRow.value.id)
            })
            .catch(() => {
            })
    }

    return {
        // alarm criteria state
        alarmConfigDialogVisible,
        alarmDialogVisible,
        isEditAlarm,
        alarmFormRef,
        alarmFormData,
        alarmFormRules,
        alarmCriteriaList,
        currentEditingAlarmLevel,
        // monitor type
        monitorTypeList,
        filteredMonitorContent,
        // dispatch state
        dispatchRules,
        dispatchDialogVisible,
        isEditDispatch,
        dispatchFormRef,
        dispatchFormData,
        dispatchFormRules,
        // mock user list
        userList,
        // init
        initAlarmCriteria,
        initDispatchRules,
        // alarm criteria actions
        handleConfigAlarm,
        handleAddAlarmCriteria,
        handleEditAlarm,
        handleAlarmDeviceChange,
        handleMonitorTypeChange,
        handleMonitorContentChange,
        insertExpression,
        handleAlarmSubmit,
        handleToggleAlarm,
        handleDeleteAlarm,
        // dispatch actions
        handleAddDispatchRule,
        handleEditDispatchRule,
        handleDispatchSubmit,
        handleDeleteDispatchRule,
    }
}
