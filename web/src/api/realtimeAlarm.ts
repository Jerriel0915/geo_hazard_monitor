/**
 * @deprecated 请使用 @/api/alarm 模块。此文件保留向后兼容。
 */
// 类型重导出
import type {AlarmRecordItem as _AlarmRecordItem} from './alarm'
import * as alarm from './alarm'

export type {AlarmRecordItem, AlarmRecordPageParams, AlarmDisposePayload, AlarmBatchDisposePayload} from './alarm'
export type RealtimeAlarmDetail = _AlarmRecordItem

// ── 待办 / 历史 ──
export const getRealtimeAlarmPage = alarm.getPendingAlarms
export const getRealtimeAlarmDetail = (id: string) => alarm.getAlarmRecordDetail(Number(id))

// ── 处置 ──
export const feedbackAlarm = (data: {
    alarmId: number;
    feedback: string;
    responseUserId: number;
    responseUserName: string
}) =>
    alarm.disposeAlarm(data.alarmId, {status: 2, note: data.feedback})

export const markAsFalseAlarm = (id: string) =>
    alarm.disposeAlarm(Number(id), {status: 4, note: '标记误报'})

export const clearAlarm = (id: string) =>
    alarm.disposeAlarm(Number(id), {status: 3, note: '销警'})

// ── 批量 ──
export const batchMarkAsFalseAlarm = (ids: number[]) =>
    alarm.batchDisposeAlarms({ids, status: 4, note: '批量误报'})

export const batchClearAlarm = (ids: number[]) =>
    alarm.batchDisposeAlarms({ids, status: 3, note: '批量销警'})

// ── 导出（暂用原路径直到后端实现） ──
export const exportRealtimeAlarms = (params?: Record<string, unknown>) =>
    alarm.getPendingAlarms(params as any)  // TODO: 后端实现 /alarm/records/export 后替换

// ── 废弃 ──
export const deleteRealtimeAlarm = (_id: string) => {
    console.warn('deleteRealtimeAlarm 已废弃');
    return Promise.resolve()
}
export const deleteRealtimeAlarms = (_ids: number[]) => {
    console.warn('deleteRealtimeAlarms 已废弃');
    return Promise.resolve()
}
export const getRealtimeAlarmByHazardPoint = (_hazardPointId: string) => {
    console.warn('getRealtimeAlarmByHazardPoint 已废弃, 使用待办列表 hazardPointId 筛选');
    return Promise.resolve({code: 200, data: []})
}
