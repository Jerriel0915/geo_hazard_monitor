import request from '@/utils/request'
import type {AjaxResult, PageResult} from './system'

export interface DeviceItem {
  id?: number
  code: string
  name: string
  sn?: string
  deviceType?: number | null
  networkType?: number | null
  protocolType?: string
  registerSource?: string
  vendorName?: string
  authUsername?: string
  authPassword?: string
  authStatus?: number
  icon?: string
  iconPath?: string
  status: number
  statusName?: string
    onlineStatus?: number
  lastReportTime?: string
  registeredAt?: string
  lastAuthTime?: string
  lastAuthIp?: string
  longitude?: number | null
  latitude?: number | null
  createTime?: string
  sensors?: any[]
}

export interface DeviceCreatePayload {
  code: string
  name: string
  sn?: string
  deviceType?: number | null
  networkType?: number | null
  protocolType?: string
  vendorName?: string
  icon?: string
  iconPath?: string
  longitude?: number | null
  latitude?: number | null
  status: number
}

export interface DeviceUpdatePayload {
  name: string
  sn?: string
  deviceType?: number | null
  networkType?: number | null
  protocolType?: string
  vendorName?: string
  icon?: string
  iconPath?: string
  longitude?: number | null
  latitude?: number | null
  status: number
}

export interface DeviceAuthAccount {
  deviceId: number
  username: string
  password: string
  authStatus: number
  registeredAt?: string
  lastAuthTime?: string
  lastAuthIp?: string
}

export interface DeviceCreateResult {
  id: number
  username: string
  password: string
}

export interface DevicePageParams {
  pageNum: number
  pageSize: number
  code?: string
  name?: string
  sn?: string
  status?: number | ''
}

const unwrap = async <T>(promise: Promise<AjaxResult<T>>): Promise<T> => {
  const response = await promise
    if (response && typeof response.code === 'number' && response.code !== 200) {
        throw new Error(response.msg || '操作失败')
    }
  return response.data
}

export const getDevicePage = (params: DevicePageParams) =>
  unwrap<PageResult<DeviceItem>>(request.get('/devices/page', { params }))

export const getDeviceDetail = (id: number) =>
  unwrap<DeviceItem>(request.get(`/devices/${id}`))

export const createDevice = (payload: DeviceCreatePayload) =>
  unwrap<DeviceCreateResult>(request.post('/devices', payload))

export const updateDevice = (id: number, payload: DeviceUpdatePayload) =>
  unwrap<{ id: number }>(request.put(`/devices/${id}`, payload))

export const deleteDevice = (id: number) =>
  unwrap<null>(request.delete(`/devices/${id}`))

export const copyDevice = (id: number) =>
  unwrap<number>(request.post(`/devices/${id}/copy`, {}))

export const getDeviceAuthAccount = (id: number) =>
  unwrap<DeviceAuthAccount>(request.get(`/devices/${id}/auth-account`))

export const resetDevicePassword = (id: number, reason?: string) =>
  unwrap<{ username: string; password: string }>(request.post(`/devices/${id}/auth-password/reset`, { reason }))

export const changeDeviceAuthStatus = (id: number, authStatus: number, reason?: string) =>
  unwrap<DeviceAuthAccount>(request.put(`/devices/${id}/auth-status`, { authStatus, reason }))
