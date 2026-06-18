// src/utils/alarm.ts
// 告警 API 封装 — 对齐 web/src/api/alarm.ts 与后端 /alarm/records/* 接口
import http from '@/utils/api'

// ==================== 类型定义 ====================

/** 告警等级: 1=红色(最严重) 2=橙色 3=黄色 4=蓝色 */
export type AlarmLevel = 1 | 2 | 3 | 4
/** 告警类型: THRESHOLD=阈值 COMPREHENSIVE=综合 */
export type AlarmType = 'THRESHOLD' | 'COMPREHENSIVE'
/** 警情状态: 1=待处理 2=处理中 3=已销警 4=误报 */
export type AlarmStatus = 1 | 2 | 3 | 4

/** 告警记录 (列表/详情) */
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
  alarmLevelText?: string
  alarmType: string
  alarmMessage: string
  criteriaId?: number
  strategyId?: number
  currentValue?: number
  thresholdValue?: number
  minAlarmLevel?: number
  maxAlarmLevel?: number
  firstTriggerTime: string
  lastTriggerTime: string
  triggerCount: number
  status: number
  statusName?: string
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
  alarmType?: string
  alarmLevels?: number[]
  alarmTypes?: string[]
  statusList?: number[]
  triggerTimeBegin?: string
  triggerTimeEnd?: string
}

export interface AlarmDisposePayload {
  status: number
  /** @deprecated 旧字段 */
  note?: string
  /** 描述 (FEEDBACK 时附带) */
  description?: string
  /** 附件 fileName (逗号分隔) */
  attachments?: string
  /** 备注/反馈内容 */
  remarks?: string
}

/** 告警动作日志 (处置记录 tab + 时间线) */
export interface AlarmRecordActionLog {
  id: number
  alarmRecordId: number
  /** CREATE/RE_TRIGGER/LEVEL_CHANGE/FEEDBACK/DISPOSE_CLOSE/DISPOSE_FALSE_ALARM/NOTIFY/CURRENT/ENDED */
  actionType: string
  fromValue?: string
  toValue?: string
  remarks?: string
  description?: string
  attachments?: string
  operator?: string
  createTime: string
}

/** 告警触发明细 (告警记录 tab) */
export interface AlarmRecordTriggerDetail {
  id: number
  alarmRecordId: number
  triggerTime: string
  alarmLevel?: number
  alarmType?: string
  alarmMessage?: string
  createTime: string
}

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
  /** 1=待发送 2=已发送 3=失败 4=接收人无效 5=渠道未配置 */
  status: number
  sendTime?: string
  errorMsg?: string
  createTime: string
}

/** 分页结果 */
export interface PageResult<T> {
  rows: T[]
  total: number
  code?: number
  msg?: string
  pageNum?: number
  pageSize?: number
}

/** 时间线节点 (buildTimeline 输出) */
export interface TimelineNode {
  time: string
  label: string
  description: string
  operator: string
  type: string
}

// ==================== 颜色配置 ====================

/** 告警等级颜色 (与 web ALARM_LEVEL_COLORS.solid 一致: 1=红 2=橙 3=黄 4=蓝) */
export const ALARM_LEVEL_COLORS: Record<number, string> = {
  1: '#F53F3F',
  2: '#FF7D00',
  3: '#FACC22',
  4: '#1890FF',
}

// ==================== 工具函数 ====================

/** 等级文案: "一级（警报）" / "二级（警戒）" / "三级（警示）" / "四级（注意）" */
export function getAlarmLevelText(level: number | string | undefined): string {
  const map: Record<number, string> = {
    1: '一级（警报）',
    2: '二级（警戒）',
    3: '三级（警示）',
    4: '四级（注意）',
  }
  return map[Number(level)] || '-'
}

/** 等级主色 (#F53F3F / #FF7D00 / #FACC22 / #1890FF) */
export function getAlarmLevelColor(level: number | string | undefined): string {
  return ALARM_LEVEL_COLORS[Number(level)] || '#909399'
}

/** 等级 CSS 钩子 (level-1 ~ level-4) */
export function getAlarmLevelClass(level: number | string | undefined): string {
  const n = Number(level)
  if (n >= 1 && n <= 4) return `level-${n}`
  return 'level-4'
}

/** 等级简短中文名 (红色/橙色/黄色/蓝色) */
export function getAlarmLevelName(level: number | string | undefined): string {
  const map: Record<number, string> = { 1: '红色', 2: '橙色', 3: '黄色', 4: '蓝色' }
  return map[Number(level)] || String(level ?? '')
}

/** 告警类型文案 */
export function getAlarmTypeText(type: string | undefined): string {
  const map: Record<string, string> = { THRESHOLD: '阈值预警', COMPREHENSIVE: '综合预警' }
  return map[type || ''] || type || '-'
}

/** 状态文案: 1=待处理 2=处理中 3=已销警 4=误报 */
export function getStatusText(status: number | string | undefined): string {
  const map: Record<number, string> = {
    1: '待处理',
    2: '处理中',
    3: '已销警',
    4: '误报',
  }
  return map[Number(status)] || '-'
}

/** 状态 tag 配色 (对应 uniapp 通用语义: danger/warning/success/info) */
export function getStatusType(status: number | string | undefined): string {
  const map: Record<number, string> = {
    1: 'danger',
    2: 'warning',
    3: 'success',
    4: 'info',
  }
  return map[Number(status)] || 'info'
}

/** 通知渠道文案 */
export function getChannelText(channel: string | undefined): string {
  const map: Record<string, string> = { SYSTEM: '系统', SMS: '短信', EMAIL: '邮件' }
  return map[channel || ''] || channel || '-'
}

/** 通知状态文案 */
export function getNotifyStatusText(status: number | undefined): string {
  const map: Record<number, string> = {
    1: '待发送',
    2: '已发送',
    3: '失败',
    4: '接收人无效',
    5: '渠道未配置',
  }
  return map[Number(status)] || '待发送'
}

/** 通知状态 tag 配色 */
export function getNotifyStatusType(status: number | undefined): string {
  const map: Record<number, string> = {
    1: 'info',
    2: 'success',
    3: 'danger',
    4: 'warning',
    5: 'warning',
  }
  return map[Number(status)] || 'info'
}

/** 反馈动作文案 */
export function getFeedbackActionText(actionType: string): string {
  const map: Record<string, string> = {
    FEEDBACK: '处置反馈',
    DISPOSE_CLOSE: '告警销警',
    DISPOSE_FALSE_ALARM: '标记误报',
  }
  return map[actionType] || actionType
}

/** 反馈动作 tag 配色 */
export function getFeedbackActionType(actionType: string): string {
  const map: Record<string, string> = {
    FEEDBACK: 'primary',
    DISPOSE_CLOSE: 'success',
    DISPOSE_FALSE_ALARM: 'warning',
  }
  return map[actionType] || 'info'
}

/**
 * 由动作日志构造时间线。
 * CURRENT/ENDED 当前状态节点始终置顶；其余按时间倒序。
 * 与 web H5Disposal.vue 的 buildTimeline 逻辑一致。
 */
export function buildTimeline(logs: AlarmRecordActionLog[]): TimelineNode[] {
  return [...logs].sort((a, b) => {
    if (a.actionType === 'CURRENT' || a.actionType === 'ENDED') return -1
    if (b.actionType === 'CURRENT' || b.actionType === 'ENDED') return 1
    return (b.createTime || '').localeCompare(a.createTime || '') || (a.id - b.id)
  }).map(log => {
    const typeMap: Record<string, string> = {
      CURRENT: 'current', ENDED: 'ended',
      CREATE: 'trigger', RE_TRIGGER: 'trigger', LEVEL_CHANGE: 'trigger',
      NOTIFY: 'notify',
      FEEDBACK: 'dispose', DISPOSE_CLOSE: 'dispose', DISPOSE_FALSE_ALARM: 'dispose',
    }
    const labelMap: Record<string, string> = {
      CURRENT: '当前', ENDED: '结束',
      CREATE: '告警创建', RE_TRIGGER: '告警触发', LEVEL_CHANGE: '等级变化',
      FEEDBACK: '处置反馈', DISPOSE_CLOSE: '告警销警',
      DISPOSE_FALSE_ALARM: '标记误报', NOTIFY: '通知发送',
    }
    const label = labelMap[log.actionType] || log.actionType
    const description = log.actionType === 'LEVEL_CHANGE'
      ? `${getAlarmLevelName(log.fromValue)}→${getAlarmLevelName(log.toValue)}`
      : (log.description || '')
    return {
      time: log.createTime || '',
      label,
      description,
      operator: log.operator || '',
      type: typeMap[log.actionType] || 'system',
    }
  })
}

// ==================== API 方法 ====================

/**
 * 解析分页接口返回。
 * 后端响应：{ code, msg, data: { rows, total } } 由 api.ts 取出 data。
 * 旧版兼容：可能直接返回数组或 { rows, total }。
 */
function asPage<T>(res: any): PageResult<T> {
  if (!res) return { rows: [], total: 0 }
  if (Array.isArray(res)) return { rows: res, total: res.length }
  if (Array.isArray(res.rows)) {
    return { rows: res.rows, total: Number(res.total ?? res.rows.length) }
  }
  if (Array.isArray(res.data)) {
    return { rows: res.data, total: Number(res.total ?? res.data.length) }
  }
  return { rows: [], total: 0 }
}

/** 列表接口可能直接返回数组 */
function asArray<T>(res: any): T[] {
  if (!res) return []
  if (Array.isArray(res)) return res
  if (Array.isArray(res.rows)) return res.rows
  if (Array.isArray(res.data)) return res.data
  return []
}

export const alarmApi = {
  /** 待办列表 (status IN 1,2) */
  async getPendingAlarms(params: AlarmRecordPageParams = {}): Promise<PageResult<AlarmRecordItem>> {
    const res = await http.get('/alarm/records/pending', {
      pageNum: params.pageNum ?? 1,
      pageSize: params.pageSize ?? 20,
      hazardPointId: params.hazardPointId,
      hazardPointName: params.hazardPointName,
      alarmLevel: params.alarmLevel,
      alarmType: params.alarmType,
      triggerTimeBegin: params.triggerTimeBegin,
      triggerTimeEnd: params.triggerTimeEnd,
    })
    return asPage<AlarmRecordItem>(res)
  },

  /** 历史列表 (status IN 3,4) */
  async getHistoryAlarms(params: AlarmRecordPageParams = {}): Promise<PageResult<AlarmRecordItem>> {
    const res = await http.get('/alarm/records/history', {
      pageNum: params.pageNum ?? 1,
      pageSize: params.pageSize ?? 20,
      hazardPointId: params.hazardPointId,
      hazardPointName: params.hazardPointName,
      alarmLevel: params.alarmLevel,
      alarmType: params.alarmType,
      triggerTimeBegin: params.triggerTimeBegin,
      triggerTimeEnd: params.triggerTimeEnd,
    })
    return asPage<AlarmRecordItem>(res)
  },

  /** 详情 */
  async getAlarmRecordDetail(id: number): Promise<AlarmRecordItem> {
    return await http.get(`/alarm/records/${id}`) as AlarmRecordItem
  },

  /** 处置 (状态流转) */
  async disposeAlarm(id: number, payload: AlarmDisposePayload): Promise<any> {
    return await http.put(`/alarm/records/${id}/dispose`, payload)
  },

  /** 触发明细 */
  async getTriggerDetails(id: number): Promise<AlarmRecordTriggerDetail[]> {
    const res = await http.get(`/alarm/records/${id}/trigger-details`)
    return asArray<AlarmRecordTriggerDetail>(res)
  },

  /** 动作日志 (处置记录 + 时间线) */
  async getActionLogs(id: number): Promise<AlarmRecordActionLog[]> {
    const res = await http.get(`/alarm/records/${id}/action-logs`)
    return asArray<AlarmRecordActionLog>(res)
  },

  /** 通知记录 */
  async getNotifications(id: number): Promise<AlarmNotificationItem[]> {
    const res = await http.get(`/alarm/records/${id}/notifications`)
    return asArray<AlarmNotificationItem>(res)
  },
}

export default alarmApi
