import request from '@/utils/request'

export type AlarmEventType = 'ALARM' | 'OFFLINE'
export type AlarmRecipientType = 'ROLE' | 'DEPT' | 'USER'
export type NotifyChannel = 'SYSTEM' | 'SMS' | 'EMAIL'

export interface RecipientSelection {
  roleIds?: string[]
  deptIds?: string[]
  userIds?: string[]
}

export interface AlarmDispatchRuleCreateRequest {
  id?: number
  name: string
  eventType: AlarmEventType
  alarmLevels?: string[]
  channels: NotifyChannel[]
  hazardPointIds?: string[]
  deviceIds?: string[]
  recipients: RecipientSelection
  isEnabled: number
  remark?: string
}

export interface RoleOption { id: string; name: string }
export interface DeptOption { id: string; name: string }
export interface UserOption { id: string; name: string; deptName?: string }

export interface RecipientOptions {
  roles: RoleOption[]
  depts: DeptOption[]
  users: UserOption[]
}

export interface AlarmDispatchRuleItemVO {
  id: number
  name: string
  eventType: AlarmEventType
  alarmLevels: string[]
  channels: NotifyChannel[]
  hazardPointAll: boolean
  hazardPointNames?: string[]
  deviceAll: boolean
  deviceNames?: string[]
  recipientAll: boolean
  recipientSummary?: string
  isEnabled: number
  createTime: string
  createBy: string
  remark?: string
}

export interface AlarmDispatchRuleDetailVO {
  id: number
  name: string
  eventType: AlarmEventType
  alarmLevels: string[]
  channels: NotifyChannel[]
  hazardPointIds: string[]
  hazardPointOptions?: Array<{ id: string; name: string }>
  deviceIds: string[]
  deviceOptions?: Array<{ id: string; name: string; code: string }>
  recipients: {
    roles: RoleOption[]
    depts: DeptOption[]
    users: UserOption[]
    hasWildcardRole: boolean
    hasWildcardDept: boolean
    hasWildcardUser: boolean
  }
  isEnabled: number
  remark?: string
  createTime: string
  createBy: string
}

export interface AlarmDispatchRuleQuery {
  name?: string
  eventType?: AlarmEventType
  isEnabled?: number
  pageNum?: number
  pageSize?: number
}

// ===== API =====

export const getDispatchRuleList = (params: AlarmDispatchRuleQuery) =>
  request.get<{ rows: AlarmDispatchRuleItemVO[]; total: number }>(
    '/alarm/dispatch/list', { params }
  )

export const getDispatchRuleDetail = (id: number) =>
  request.get<{ data: AlarmDispatchRuleDetailVO }>(`/alarm/dispatch/${id}`)

export const createDispatchRule = (payload: AlarmDispatchRuleCreateRequest) =>
  request.post('/alarm/dispatch', payload)

export const updateDispatchRule = (id: number, payload: AlarmDispatchRuleCreateRequest) =>
  request.put(`/alarm/dispatch/${id}`, payload)

export const deleteDispatchRule = (id: number) =>
  request.delete(`/alarm/dispatch/${id}`)

export const toggleDispatchRuleEnabled = (id: number, isEnabled: number) =>
  request.put(`/alarm/dispatch/${id}/enabled`, { isEnabled })

export const getRecipientOptions = () =>
  request.get<{ data: RecipientOptions }>('/alarm/dispatch/recipient-options')
