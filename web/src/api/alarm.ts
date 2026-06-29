import request from '@/utils/request';
import type { AjaxResult, PageResult } from './system';

// ==================== 类型定义 ====================

/** 告警等级: 1=红色 2=橙色 3=黄色 4=蓝色 (1=最严重) */
export type AlarmLevel = 1 | 2 | 3 | 4

/**
 * 告警等级颜色配置 — 严格遵循 一级红/二级橙/三级黄/四级蓝
 * 不使用 UI 主题色，每个等级独立颜色
 *  - solid: 主色（tag 背景）
 *  - light: 浅色（icon 背景）
 *  - dark: 深色（icon 字色）
 *  - fg: tag 文字色（黄底用深字保证对比度）
 */
export const ALARM_LEVEL_COLORS: Record<number, { solid: string; light: string; dark: string; fg: string }> = {
    1: { solid: '#F53F3F', light: '#fee2e2', dark: '#dc3545', fg: '#ffffff' }, // 红
    2: { solid: '#FF7D00', light: '#fff3e0', dark: '#e67e22', fg: '#ffffff' }, // 橙
    3: { solid: '#e1ff00', light: '#fff9e6', dark: '#f5e856', fg: '#1d2129' }, // 黄 (深字保对比度)
    4: { solid: '#1890FF', light: '#e6f4ff', dark: '#0958d9', fg: '#ffffff' }, // 蓝
}

/** 告警等级 tag 的 inline style (背景 + 边框 + 文字色) */
export const getAlarmLevelStyle = (level: number | string | undefined): Record<string, string> => {
    const c = ALARM_LEVEL_COLORS[Number(level)] || { solid: '#909399', fg: '#ffffff' }
    return {
        backgroundColor: c.solid,
        borderColor: c.solid,
        color: c.fg,
    }
}

/**
 * 告警等级配置（文字 + 样式）
 * 返回: { text: string; style: Record<string, string> }
 */
export const getAlarmLevelConfig = (level: number | string | undefined) => {
    const n = Number(level)
    const configMap: Record<number, { text: string; style: Record<string, string> }> = {
        1: {
            text: '一级（警报）',
            style: { backgroundColor: '#F53F3F', borderColor: '#F53F3F', color: '#ffffff' }
        },
        2: {
            text: '二级（警戒）',
            style: { backgroundColor: '#FF7D00', borderColor: '#FF7D00', color: '#ffffff' }
        },
        3: {
            text: '三级（警示）',
            style: { backgroundColor: '#e1ff00', borderColor: '#e1ff00', color: '#1d2129' }
        },
        4: {
            text: '四级（注意）',
            style: { backgroundColor: '#1890FF', borderColor: '#1890FF', color: '#ffffff' }
        }
    }
    return configMap[n] || {
        text: String(level),
        style: { backgroundColor: '#909399', borderColor: '#909399', color: '#ffffff' }
    }
}

/**
 * 告警类型文本映射
 * THRESHOLD → 阈值告警，COMPREHENSIVE → 综合预警
 */
export const getAlarmTypeText = (type: string): string => {
    const map: Record<string, string> = {
        'THRESHOLD': '阈值告警',
        'COMPREHENSIVE': '综合告警'
    }
    return map[type] || type
}

/**
 * 告警类型对应的标签类型
 * 综合预警 → warning（橙色），阈值预警 → danger（红色）
 */
export const getAlarmTypeTagType = (type: string): string => {
    const typeMap: Record<string, string> = {
        'COMPREHENSIVE': 'warning',
        'THRESHOLD': 'danger'
    }
    return typeMap[type] || 'info'
}

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
    deviceName?: string
    sensorId?: number
    sensorName?: string
    monitorContentId?: number
    alarmLevel: number
    alarmLevelText: string
    alarmType: string
    alarmMessage: string
    criteriaId?: number
    strategyId?: number
    currentValue?: number
    thresholdValue?: number
    /** 历史告警中曾出现的最低等级（用于"X-Y级"区间展示） */
    minAlarmLevel?: number
    /** 历史告警中曾出现的最高等级 */
    maxAlarmLevel?: number
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
    /** 原有单选 */
    alarmLevel?: number
    alarmType?: string
    /** 新增多选筛选 */
    alarmLevels?: number[]
    alarmTypes?: string[]
    statusList?: number[]
    /** 触发时间范围 */
    triggerTimeBegin?: string
    triggerTimeEnd?: string
}

export interface AlarmDisposePayload {
    status: number
    /** @deprecated 旧字段，保留向后兼容 */
    note?: string
    /** 描述 (FEEDBACK 时附带) */
    description?: string
    /** 附件 fileName (逗号分隔) */
    attachments?: string
    /** 备注/反馈内容 */
    remarks?: string
}

export interface AlarmBatchDisposePayload {
    ids: number[]
    status: number
    note?: string
    description?: string
    attachments?: string
    remarks?: string
}

/** 告警动作日志（处置记录 tab + 时间线） */
export interface AlarmRecordActionLog {
    id: number
    alarmRecordId: number
    /** 动作类型: CREATE/RE_TRIGGER/LEVEL_CHANGE/FEEDBACK/DISPOSE_CLOSE/DISPOSE_FALSE_ALARM/NOTIFY */
    actionType: string
    fromValue?: string
    toValue?: string
    remarks?: string
    description?: string
    attachments?: string
    operator?: string
    createTime: string
}

/** 告警触发明细（告警记录 tab） */
export interface AlarmRecordTriggerDetail {
    id: number
    alarmRecordId: number
    triggerTime: string
    alarmLevel?: number
    alarmType?: string
    alarmMessage?: string
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
    sustainSeconds?: number
    isEnabled?: number
    hazardPointIds?: string[]
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
    request.get<AjaxResult<PageResult<AlarmRecordItem>>>('/alarm/records/pending', {params})

/** 历史告警列表 (status IN 3,4) */
export const getHistoryAlarms = (params: AlarmRecordPageParams) =>
    request.get<PageResult<AlarmRecordItem>>('/alarm/records/history', {params})

/** 告警详情 */
export const getAlarmRecordDetail = (id: number) =>
    request.get<AjaxResult<AlarmRecordItem>>(`/alarm/records/${id}`)

/** 处置告警 (状态流转) */
export const disposeAlarm = (id: number, payload: AlarmDisposePayload) =>
    request.put(`/alarm/records/${id}/dispose`, payload)

/** 批量处置 */
export const batchDisposeAlarms = (payload: AlarmBatchDisposePayload) =>
    request.post('/alarm/records/batch', payload)

/** 告警触发明细列表 */
export const getTriggerDetails = (id: number) =>
    request.get<AjaxResult<AlarmRecordTriggerDetail[]>>(`/alarm/records/${id}/trigger-details`)

/** 告警动作日志列表（处置记录 + 时间线） */
export const getActionLogs = (id: number) =>
    request.get<AjaxResult<AlarmRecordActionLog[]>>(`/alarm/records/${id}/action-logs`)

/** 告警通知记录 (alarm_notification 表) */
export interface AlarmNotificationItem {
    id: number
    alarmId: number
    dispatchRuleId?: number
    recipientId?: number
    recipientName?: string
    recipientPhone?: string
    channel: string
    title?: string
    content?: string
    status: number   // 1=待发送 2=已发送 3=发送失败
    sendTime?: string
    errorMsg?: string
    createTime: string
}

/** 通知记录列表 */
export const getAlarmNotifications = (id: number) =>
    request.get<AjaxResult<AlarmNotificationItem[]>>(`/alarm/records/${id}/notifications`)

// ==================== 告警统计 API ====================

/** 待处理告警等级统计: { 1: count, 2: count, 3: count, 4: count } */
export const getAlarmLevelStats = () =>
    request.get<Record<number, number>>('/alarm/records/level-stats')

/** 告警趋势数据 (近N个月+2个月预测) */
export interface AlarmTrendVO {
    months: string[]
    level1: number[]
    level2: number[]
    level3: number[]
    level4: number[]
    total: number[]
    forecastMonths: string[]
    forecastLevel1: number[]
    forecastLevel2: number[]
    forecastLevel3: number[]
    forecastLevel4: number[]
    forecastTotal: number[]
}

export const getAlarmTrend = (months?: number) =>
    request.get<AlarmTrendVO>('/alarm/records/trend', { params: { months } })

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
    request.get<any>(`/alarm/strategies/${id}`).then(res => res.data as AlarmStrategyItem)

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
    request.get<any>(`/alarm/strategies/${id}/scope`).then(res => res.data as string[])

/** 仅更新策略的应用范围 */
export const updateStrategyScope = (id: number, hazardPointIds: string[]) =>
    request.put(`/alarm/strategies/${id}/scope`, { hazardPointIds })

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

// ==================== 从 compositeAlarm.ts 迁移的类型 ====================

/** 综合告警策略执行日志 */
export interface CompositeAlarmLog {
  id: number
  alarmId: number
  triggerTime: string
  triggerMode: string
  durationMs: number
  status: string
  output?: string
  errorMsg?: string
}

/** 隐患点选项（供告警策略范围选择器使用） */
export interface HazardPointOption {
  id: number
  name: string
  parentId?: number
  hazardPointId?: number
  children?: HazardPointOption[]
}

// ==================== 综合告警策略测试运行 ====================

/** 综合告警策略测试运行结果 */
export interface StrategyTestRunResult {
  level: number | null
  levelText: string | null
  durationMs: number
  error: string | null
}

/** 测试运行综合告警策略脚本 */
export const testStrategyRun = (id: number, payload?: {
  mockSensorCode?: string
  mockDataTime?: number
}) =>
  request.post<any>(`/alarm/strategies/${id}/test-run`, payload || {}).then(res => res.data as StrategyTestRunResult)

// ==================== 策略执行日志 ====================

/** 执行日志项 */
export interface ExecutionLogItem {
  id: number
  strategyId: number
  triggerType: 'CRON' | 'DATA_INGEST' | 'ALARM_TRIGGER'
  triggerSource: string | null
  hazardPointIds: string | null
  resultLevel: number | null
  resultStatus: 'SUCCESS' | 'NO_ALARM' | 'FAIL' | 'TIMEOUT'
  durationMs: number
  scriptLogs: string | null
  errorMessage: string | null
  triggeredCount: number
  createTime: string
}

/** 查询策略执行日志 */
export const getExecutionLogs = (strategyId: number, params?: { pageNum?: number; pageSize?: number }) =>
  request.get<{ rows: ExecutionLogItem[]; total: number }>(
    `/alarm/strategies/${strategyId}/execution-logs`, { params }
  )
