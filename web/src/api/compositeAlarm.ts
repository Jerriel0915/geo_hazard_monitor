/**
 * @deprecated 请使用 @/api/alarm 模块。此文件保留向后兼容。
 */
import * as alarm from './alarm'

// ── 策略 CRUD ──
export const getCompositeAlarmPage = alarm.getStrategyList
export const getCompositeAlarmDetail = alarm.getStrategyDetail
export const createCompositeAlarm = alarm.createStrategy
export const updateCompositeAlarm = alarm.updateStrategy
export const deleteCompositeAlarm = alarm.deleteStrategy

export const changeCompositeAlarmStatus = (id: number, status: 'ENABLED' | 'DISABLED') =>
    alarm.toggleStrategy(id, status === 'ENABLED' ? 1 : 0)

// ── 范围管理 ──
export const getCompositeAlarmScopes = async (alarmId: number) => {
    const ids: number[] = await alarm.getStrategyScope(alarmId) as any
    return (Array.isArray(ids) ? ids : []).map((hpId: number) => ({id: hpId, alarmId, hazardPointId: hpId}))
}

export const updateCompositeAlarmScopes = async (alarmId: number, hazardPointIds: number[]) => {
    return alarm.updateStrategy(alarmId, {hazardPointIds} as any)
}

// ── 隐患点选项 ──
import {getHazardPointPage} from './hazardPoint'

export const getHazardPointOptions = async () => {
    const res: any = await getHazardPointPage({pageNum: 1, pageSize: 1000})
    return ((res && res.rows) || []).map((hp: any) => ({id: hp.id, name: hp.name}))
}

// ── 日志 / 测试（后端暂未实现，预留占位） ──
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

export const getCompositeAlarmLogs = async (_alarmId: number, _params: any) => {
    console.warn('getCompositeAlarmLogs: 后端暂未实现策略执行日志接口')
    return {rows: [], total: 0}
}

export const testCompositeAlarm = async (_id: number) => {
    console.warn('testCompositeAlarm: 后端暂未实现策略测试接口')
    return null
}

export const updateScriptCode = async (id: number, scriptContent: string, _scriptXml?: string) => {
    return alarm.updateStrategy(id, {scriptContent} as any)
}

// ── 隐患点选项类型 ──
export interface HazardPointOption {
  id: number
  name: string
  parentId?: number
  children?: HazardPointOption[]
}

// 类型重导出（保持旧名，含兼容字段）
export type CompositeAlarmItem = alarm.AlarmStrategyItem & {
    status?: 'ENABLED' | 'DISABLED'
    silenceSeconds?: number
    sustainSeconds?: number
    levelChangeNotify?: boolean
    scopeCount?: number
    lastRunStatus?: 'SUCCESS' | 'ERROR' | 'TIMEOUT'
    scriptCode?: string
    scriptXml?: string
    subscriptionConfig?: any
    triggerMode?: string
}
