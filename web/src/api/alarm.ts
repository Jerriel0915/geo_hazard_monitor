import request from '@/utils/request'
import type {PageResult} from './system'

// ==================== 类型定义 ====================

/** 告警等级: 1=蓝色 2=黄色 3=橙色 4=红色 */
export type AlarmLevel = 1 | 2 | 3 | 4
/** 告警类型: THRESHOLD=阈值 COMPREHENSIVE=综合 */
export type AlarmType = 'THRESHOLD' | 'COMPREHENSIVE'
/** 警情状态: 1=待处理 2=处理中 3=已销警 4=误报 */
export type AlarmStatus = 1 | 2 | 3 | 4
/** 策略触发模式: REALTIME=实时 CRON=周期 */
export type TriggerMode = 'REALTIME' | 'CRON'

// ── 告警记录 ──

export interface AlarmRecordItem {
    id: number
    hazardPointId: number
    hazardPointName: string
    deviceId?: number
    sensorId?: number
    monitorContentId?: number
    alarmLevel: number
    alarmLevelText: string
    alarmType: string
    alarmMessage: string
    criteriaId?: number
    strategyId?: number
    currentValue?: number
    thresholdValue?: number
    firstTriggerTime: string
    lastTriggerTime: string
    triggerCount: number
    status: number
    statusName: string
    resolvedBy?: string
    resolvedAt?: string
    resolutionNote?: string
    createTime: string
}

export interface AlarmRecordPageParams {
    pageNum?: number
    pageSize?: number
    hazardPointId?: number
    hazardPointName?: string
    alarmLevel?: number
    alarmLevels?: string
    alarmType?: string
    alarmTypes?: string
    statusList?: string
    startTime?: string
    endTime?: string
    personName?: string
}

export interface AlarmDisposePayload {
    status: number
    note?: string
}

export interface AlarmBatchDisposePayload {
    ids: number[]
    status: number
    note?: string
}

export interface AlarmRecordLog {
    id: number
    alarmId: number
    fromStatus?: number
    toStatus: number
    /** 处置类型: 告警引擎自动创建/开始处置/已销警/标记误报/批量销警/批量标记误报 */
    disposalType?: string
    operator: string
    /** 处置结果描述 */
    disposalResult?: string
    note?: string
    createTime: string
}

// ── 告警判据 ──

// ── 单个条件 ──
export interface LevelCondition {
    subject: string
    subjectType?: 'CONTENT' | 'FUNCTION'
    function?: string
    functionParams?: Record<string, string>
    operator: string
    threshold: number
    thresholdMax?: number
    unit?: string
}

// ── 等级配置 ──
export interface LevelConfig {
    logicOperator?: string
    conditions: LevelCondition[]
    description?: string
}

export interface AlarmCriteriaItem {
    id: number
    name: string
    monitorTypeId?: number
    monitorTypeName?: string
    monitorContentId?: number
    monitorContentCode?: string
    hazardPointId?: number
    levelConfig?: string
    persistCount: number
    silencePeriod: number
    isEnabled: number
    version: number
    createTime: string
}

export interface AlarmCriteriaCreatePayload {
    name: string
    monitorTypeId?: number
    monitorTypeName?: string
    monitorContentId?: number
    monitorContentCode?: string
    hazardPointId?: number
    /** V3.0: 四级告警条件 JSON */
    levelConfig?: string
    persistCount?: number
    silencePeriod?: number
    isEnabled?: number
}

export interface AlarmCriteriaLog {
    id: number
    criteriaId: number
    version: number
    changeType: string
    oldValue?: string
    newValue?: string
    createBy?: string
    createTime: string
}

// ── 综合告警策略 ──

export interface AlarmStrategyItem {
    id: number
    name: string
    description?: string
    monitorTypeId?: number
    triggerMode: string
    cronExpression?: string
    scriptType?: string
    scriptContent?: string
    defaultAlarmLevel: number
    silenceMinutes: number
    escalationEnabled: number
    isEnabled: number
    lastRunTime?: string
    lastRunResult?: string
    createTime: string
    // ── 向后兼容旧视图字段 ──
    /** @deprecated 使用 isEnabled (1=ENABLED, 0=DISABLED) */
    status?: 'ENABLED' | 'DISABLED'
    /** @deprecated 使用 silenceMinutes */
    silenceSeconds?: number
    /** @deprecated 使用 silenceMinutes */
    sustainSeconds?: number
    /** @deprecated 使用 escalationEnabled */
    levelChangeNotify?: boolean
    /** @deprecated 通过 /scope 接口单独查询 */
    scopeCount?: number
    /** @deprecated 使用 lastRunResult */
    lastRunStatus?: 'SUCCESS' | 'ERROR' | 'TIMEOUT'
    /** @deprecated 使用 scriptContent */
    scriptCode?: string
    /** @deprecated 后续 Blockly 集成时使用 */
    scriptXml?: string
    /** @deprecated 后续版本 */
    subscriptionConfig?: any
}

export interface AlarmStrategyCreatePayload {
    name: string
    description?: string
    monitorTypeId?: number
    triggerMode: string
    cronExpression?: string
    scriptType?: string
    scriptContent?: string
    defaultAlarmLevel: number
    silenceMinutes?: number
    escalationEnabled?: number
    isEnabled?: number
    hazardPointIds?: number[]
}

// ── 告警分发规则 ──

export interface AlarmDispatchRuleItem {
    id: number
    name: string
    hazardPointId?: number
    alarmLevels?: string
    alarmTypes?: string
    recipientsJson?: string
    channels: string
    timeWindow?: string
    isEnabled: number
    createTime: string
}

export interface AlarmDispatchRuleCreatePayload {
    name: string
    hazardPointId?: number
    alarmLevels?: string
    alarmTypes?: string
    recipientsJson?: string
    channels?: string
    timeWindow?: string
    isEnabled?: number
}

// ==================== 告警记录 API ====================

/** 待办告警列表 (status IN 1,2) */
export const getPendingAlarms = (params: AlarmRecordPageParams) =>
    request.get<PageResult<AlarmRecordItem>>('/alarm/records/pending', {params})

/** 历史告警列表 (status IN 3,4) */
export const getHistoryAlarms = (params: AlarmRecordPageParams) =>
    request.get<PageResult<AlarmRecordItem>>('/alarm/records/history', {params})

/** 告警详情 */
export const getAlarmRecordDetail = (id: number) =>
    request.get<AlarmRecordItem>(`/alarm/records/${id}`)

/** 处置告警 (状态流转) */
export const disposeAlarm = (id: number, payload: AlarmDisposePayload) =>
    request.put(`/alarm/records/${id}/dispose`, payload)

/** 批量处置 */
export const batchDisposeAlarms = (payload: AlarmBatchDisposePayload) =>
    request.post('/alarm/records/batch', payload)

/** 告警状态变更日志 */
export const getAlarmRecordLogs = (id: number) =>
    request.get<AlarmRecordLog[]>(`/alarm/records/${id}/logs`)

// ── 告警反馈 ──

export interface AlarmFeedbackItem {
    id: number
    alarmId: number
    content: string
    files?: { name: string; url: string; size: number }[]
    operator: string
    createTime: string
}

/** 查询告警反馈列表 */
export const getAlarmFeedbacks = (id: number) =>
    request.get<AlarmFeedbackItem[]>(`/alarm/records/${id}/feedbacks`)

/** 添加告警反馈 */
export const addAlarmFeedback = (id: number, data: { content: string; files?: any[] }) =>
    request.post(`/alarm/records/${id}/feedback`, data)

// ==================== 告警判据 API ====================

/** 判据列表 */
export const getCriteriaList = (params?: Record<string, unknown>) =>
    request.get<PageResult<AlarmCriteriaItem>>('/alarm/criteria/list', {params})

/** 判据详情 */
export const getCriteriaDetail = (id: number) =>
    request.get<AlarmCriteriaItem>(`/alarm/criteria/${id}`)

/** 新增判据 */
export const createCriteria = (payload: AlarmCriteriaCreatePayload) =>
    request.post('/alarm/criteria', payload)

/** 修改判据 */
export const updateCriteria = (id: number, payload: AlarmCriteriaCreatePayload) =>
    request.put(`/alarm/criteria/${id}`, payload)

/** 删除判据 (软删除) */
export const deleteCriteria = (id: number) =>
    request.delete(`/alarm/criteria/${id}`)

/** 启用/停用判据 */
export const toggleCriteria = (id: number, isEnabled: number) =>
    request.put(`/alarm/criteria/${id}/toggle`, null, {params: {isEnabled}})

/** 判据变更日志 */
export const getCriteriaLogs = (id: number) =>
    request.get<AlarmCriteriaLog[]>(`/alarm/criteria/${id}/logs`)

// ==================== 综合告警策略 API ====================

/** 策略列表 */
export const getStrategyList = (params?: Record<string, unknown>) =>
    request.get<PageResult<AlarmStrategyItem>>('/alarm/strategies/list', {params})

/** 策略详情 */
export const getStrategyDetail = (id: number) =>
    request.get<AlarmStrategyItem>(`/alarm/strategies/${id}`)

/** 新增策略 */
export const createStrategy = (payload: AlarmStrategyCreatePayload) =>
    request.post('/alarm/strategies', payload)

/** 修改策略 */
export const updateStrategy = (id: number, payload: AlarmStrategyCreatePayload) =>
    request.put(`/alarm/strategies/${id}`, payload)

/** 删除策略 */
export const deleteStrategy = (id: number) =>
    request.delete(`/alarm/strategies/${id}`)

/** 启用/停用策略 */
export const toggleStrategy = (id: number, isEnabled: number) =>
    request.put(`/alarm/strategies/${id}/toggle`, null, {params: {isEnabled}})

/** 策略绑定的隐患点ID列表 */
export const getStrategyScope = (id: number) =>
    request.get<number[]>(`/alarm/strategies/${id}/scope`)

// ==================== 告警分发规则 API ====================

/** 分发规则列表 */
export const getDispatchRuleList = (params?: Record<string, unknown>) =>
    request.get<PageResult<AlarmDispatchRuleItem>>('/alarm/dispatch/list', {params})

/** 分发规则详情 */
export const getDispatchRuleDetail = (id: number) =>
    request.get<AlarmDispatchRuleItem>(`/alarm/dispatch/${id}`)

/** 新增分发规则 */
export const createDispatchRule = (payload: AlarmDispatchRuleCreatePayload) =>
    request.post('/alarm/dispatch', payload)

/** 修改分发规则 */
export const updateDispatchRule = (id: number, payload: AlarmDispatchRuleCreatePayload) =>
    request.put(`/alarm/dispatch/${id}`, payload)

/** 删除分发规则 */
export const deleteDispatchRule = (id: number) =>
    request.delete(`/alarm/dispatch/${id}`)
