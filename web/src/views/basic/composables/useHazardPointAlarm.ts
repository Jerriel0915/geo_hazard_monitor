import {computed, reactive, ref, type Ref} from 'vue'
import {ElMessage, ElMessageBox} from 'element-plus'
import type {HazardPointItem} from './useHazardPointCrud'
import {getMonitorTypeListWithContents, type MonitorTypeItem} from '@/api/monitorType'
import {getUserPage} from '@/api/system'
import {
  createCriteria, updateCriteria, toggleCriteria, deleteCriteria,
  createDispatchRule, updateDispatchRule, deleteDispatchRule,
  getCriteriaList, getDispatchRuleList
} from '@/api/alarm'

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
    levelConfig: string
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

    // ── Monitor type list (from real API) ──
    const monitorTypeList = ref<MonitorTypeItem[]>([])
    const monitorTypeLoadError = ref(false)

    const loadMonitorTypes = async () => {
      try {
        const res: any = await getMonitorTypeListWithContents()
        monitorTypeList.value = res?.data ?? res ?? []
        monitorTypeLoadError.value = false
      } catch {
        monitorTypeList.value = []
        monitorTypeLoadError.value = true
      }
    }
    // preload on module init
    loadMonitorTypes()

    const filteredMonitorContent = computed(() => {
        if (!alarmFormData.monitorTypeId) return []
        const mt = monitorTypeList.value.find((t) => String(t.id) === alarmFormData.monitorTypeId)
        if (!mt?.contents) return []
        return mt.contents.map(c => ({
          value: c.code,
          label: c.name,
          unit: c.unit
        }))
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

    // ── User list (from real API) ──
    const userList = ref<{ id: string; name: string; phone: string }[]>([])

    const loadUsers = async () => {
      try {
        const res: any = await getUserPage({ pageNum: 1, pageSize: 200 })
        if (res?.code === 200 && res.data?.rows) {
          userList.value = res.data.rows.map((u: any) => ({
            id: String(u.userId),
            name: u.userName,
            phone: u.phonenumber || ''
          }))
        }
      } catch { userList.value = [] }
    }
    // preload on module init
    loadUsers()

    // ── Init (real API) ──
    const initAlarmCriteria = async (hazardPointId: string) => {
      if (!hazardPointId) { alarmCriteriaList.value = []; return }
      try {
        const res: any = await getCriteriaList({ hazardPointId })
        if (res?.code === 200 && res.data?.rows) {
          alarmCriteriaList.value = res.data.rows.map((r: any) => ({
            id: String(r.id),
            name: r.name,
            deviceId: '',
            deviceName: '',
            monitorTypeId: String(r.monitorTypeId ?? ''),
            monitorTypeName: r.monitorTypeName ?? '',
            monitorContentCode: r.monitorContentCode ?? '',
            monitorContentName: '',
            levelConfig: r.levelConfig || '',
            isEnabled: r.isEnabled === 1
          }))
        } else { alarmCriteriaList.value = [] }
      } catch { alarmCriteriaList.value = [] }
    }

    const initDispatchRules = async (hazardPointId: string) => {
      if (!hazardPointId) { dispatchRules.value = []; return }
      try {
        const res: any = await getDispatchRuleList({ hazardPointId })
        if (res?.code === 200 && res.data?.rows) {
          dispatchRules.value = res.data.rows.map((r: any) => ({
            id: String(r.id),
            type: 'alarm' as const,
            level: r.alarmLevels ? String(r.alarmLevels).split(',') : [],
            deviceIds: [],
            persons: r.recipientsJson ? JSON.parse(r.recipientsJson) : [],
            channels: r.channels ? r.channels.split(',') : ['system'],
            execTime: r.timeWindow ?? '',
            status: r.isEnabled ?? 1,
            remark: r.name ?? ''
          }))
        } else { dispatchRules.value = [] }
      } catch { dispatchRules.value = [] }
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
            monitorTypeId: '',
            monitorTypeName: '',
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
        let lc: Record<string, { expression?: string; description?: string }> = {}
        try { if (row.levelConfig) lc = JSON.parse(row.levelConfig) } catch { /* keep empty */ }
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
            redExpression: lc['1']?.expression || '',
            redDescription: lc['1']?.description || '',
            orangeExpression: lc['2']?.expression || '',
            orangeDescription: lc['2']?.description || '',
            yellowExpression: lc['3']?.expression || '',
            yellowDescription: lc['3']?.description || '',
            blueExpression: lc['4']?.expression || '',
            blueDescription: lc['4']?.description || '',
        })
        alarmDialogVisible.value = true
    }

    const handleAlarmDeviceChange = (val: string) => {
        const device = opts.boundDevices.value.find((d) => d.deviceId === val)
        if (device) alarmFormData.deviceName = device.deviceName
    }

    const handleMonitorTypeChange = (val: string) => {
        const mt = monitorTypeList.value.find((t) => String(t.id) === val)
        if (mt) {
            alarmFormData.monitorTypeName = mt.name
            alarmFormData.monitorContentCode = ''
            alarmFormData.monitorContentName = ''
            alarmFormData.unit = ''
        }
    }

    const handleMonitorContentChange = (val: string) => {
        const mt = monitorTypeList.value.find((t) => String(t.id) === alarmFormData.monitorTypeId)
        if (mt?.contents) {
            const content = mt.contents.find((c) => c.code === val)
            if (content) {
                alarmFormData.monitorContentName = content.name
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

    const handleAlarmSubmit = async () => {
        alarmFormRef.value.validate(async (valid: boolean) => {
            if (!valid) return
            const hpId = opts.currentRow.value?.id ?? ''
            const levelConfig = JSON.stringify({
              '1': { expression: alarmFormData.redExpression, description: alarmFormData.redDescription },
              '2': { expression: alarmFormData.orangeExpression, description: alarmFormData.orangeDescription },
              '3': { expression: alarmFormData.yellowExpression, description: alarmFormData.yellowDescription },
              '4': { expression: alarmFormData.blueExpression, description: alarmFormData.blueDescription }
            })
            const payload = {
              name: alarmFormData.name,
              monitorTypeId: alarmFormData.monitorTypeId ? Number(alarmFormData.monitorTypeId) : undefined,
              monitorContentCode: alarmFormData.monitorContentCode || undefined,
              hazardPointId: hpId ? Number(hpId) : undefined,
              levelConfig,
              isEnabled: 1
            }
            try {
              const res: any = isEditAlarm.value
                ? await updateCriteria(Number(alarmFormData.id), payload)
                : await createCriteria(payload)
              if (res?.code === 200) {
                ElMessage.success(isEditAlarm.value ? '判据修改成功' : '判据添加成功')
                alarmDialogVisible.value = false
              }
            } catch { ElMessage.error('操作失败') }
        })
    }

    const handleToggleAlarm = async (row: AlarmCriteria) => {
      try {
        const res: any = await toggleCriteria(Number(row.id), row.isEnabled ? 0 : 1)
        if (res?.code === 200) {
          ElMessage.success(`判据${row.isEnabled ? '停用' : '启用'}成功`)
        }
      } catch { ElMessage.error('操作失败') }
    }

    const handleDeleteAlarm = (row: AlarmCriteria) => {
        ElMessageBox.confirm(`确定要删除判据"${row.name}"吗?`, '删除确认', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning',
        })
            .then(async () => {
                try {
                  const res: any = await deleteCriteria(Number(row.id))
                  if (res?.code === 200) {
                    ElMessage.success('删除成功')
                    if (opts.currentRow.value) initAlarmCriteria(opts.currentRow.value.id)
                  }
                } catch { ElMessage.error('删除失败') }
            })
            .catch(() => {})
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

    const handleDispatchSubmit = async () => {
        dispatchFormRef.value.validate(async (valid: boolean) => {
            if (!valid) return
            let execTimeValue = ''
            if (dispatchFormData.execType === 'timed' && dispatchFormData.execTimePoints) {
                execTimeValue = `${dispatchFormData.execFrequencyUnit}|${dispatchFormData.execTimePoints}`
            }
            dispatchFormData.execTime = execTimeValue
            const hpId = opts.currentRow.value?.id ?? ''
            const payload = {
              name: dispatchFormData.remark || (dispatchFormData.type === 'alarm' ? '监测告警规则' : '设备离线通知规则'),
              hazardPointId: hpId ? Number(hpId) : undefined,
              alarmLevels: dispatchFormData.level.join(','),
              recipientsJson: JSON.stringify(dispatchFormData.persons),
              channels: dispatchFormData.channels.join(','),
              timeWindow: execTimeValue || undefined,
              isEnabled: dispatchFormData.status as number
            }
            try {
              const res: any = isEditDispatch.value
                ? await updateDispatchRule(Number(dispatchFormData.id), payload)
                : await createDispatchRule(payload)
              if (res?.code === 200) {
                ElMessage.success(isEditDispatch.value ? '规则修改成功' : '规则添加成功')
                dispatchDialogVisible.value = false
                if (opts.currentRow.value) initDispatchRules(opts.currentRow.value.id)
              }
            } catch { ElMessage.error('操作失败') }
        })
    }

    const handleDeleteDispatchRule = (row: DispatchRule) => {
        const ruleDesc = row.remark || (row.type === 'alarm' ? '监测告警规则' : '设备离线通知规则')
        ElMessageBox.confirm(`确定要删除规则"${ruleDesc}"吗?`, '删除确认', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning',
        })
            .then(async () => {
                try {
                  const res: any = await deleteDispatchRule(Number(row.id))
                  if (res?.code === 200) {
                    ElMessage.success('删除成功')
                    if (opts.currentRow.value) initDispatchRules(opts.currentRow.value.id)
                  }
                } catch { ElMessage.error('删除失败') }
            })
            .catch(() => {})
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
