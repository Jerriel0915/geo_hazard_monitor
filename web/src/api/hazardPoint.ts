import type {AjaxResult, PageResult} from './system'
import request from '@/utils/request'

export interface HazardPointListParams {
  pageNum?: number
  pageSize?: number
  code?: string
  name?: string
  groupId?: number
  status?: number
}

export interface HazardPointRaw {
  id: number
  code: string
  name: string
  groupId?: number
  groupName: string
  status: number
  statusName?: string
  longitude?: number
  latitude?: number
  boundaryCoords?: string
  description?: string
  deviceCount: number
  createTime?: string
  createBy?: string
  updateBy?: string
  updateTime?: string
}

export interface HazardPointPayload {
  code?: string
  name: string
  groupId: number | null
  longitude?: number
  latitude?: number
  strike?: number
    boundaryCoords?: string
  description?: string
}

export interface HazardPointExportPayload {
  ids?: number[]
  code?: string
  name?: string
  groupId?: number
  status?: number
}

export interface GroupPayload {
  code?: string
  name: string
  description: string
  sortOrder: number
  status: number
}

export function getHazardPointPage(params: HazardPointListParams): Promise<AjaxResult<PageResult<HazardPointRaw>>> {
  return request.get('/hazard-points/page', { params })
}

export function getHazardPointDetail(id: string): Promise<AjaxResult<HazardPointRaw>> {
  return request.get(`/hazard-points/${id}`)
}

export function createHazardPoint(data: HazardPointPayload): Promise<AjaxResult<HazardPointRaw>> {
  return request.post('/hazard-points', data)
}

export function updateHazardPoint(id: string, data: HazardPointPayload): Promise<AjaxResult<HazardPointRaw>> {
  return request.put(`/hazard-points/${id}`, data)
}

export function pauseHazardPoint(id: string, pause: boolean): Promise<AjaxResult<null>> {
  return request.put(`/hazard-points/${id}/pause`, { pause })
}

export function completeHazardPoint(id: string): Promise<AjaxResult<null>> {
  return request.put(`/hazard-points/${id}/complete`, {})
}

export function batchOperateHazardPoints(ids: number[], operation: 'pause' | 'resume' | 'complete'): Promise<AjaxResult<null>> {
  return request.put('/hazard-points/batch/operate', { ids, operation })
}

export function exportHazardPoints(data: HazardPointExportPayload) {
  return request.raw.post('/hazard-points/export', data, { responseType: 'blob' })
}

export function getHazardPointGroups(): Promise<AjaxResult<HazardPointGroupRaw[]>> {
  return request.get('/hazard-point-groups')
}

export interface HazardPointGroupRaw {
  id: number
  code: string
  name: string
  description: string
  sortOrder: number
  status: number
}

export function createHazardPointGroup(data: GroupPayload): Promise<AjaxResult<HazardPointGroupRaw>> {
  return request.post('/hazard-point-groups', data)
}

export function updateHazardPointGroup(id: string, data: Omit<GroupPayload, 'code'>): Promise<AjaxResult<HazardPointGroupRaw>> {
  return request.put(`/hazard-point-groups/${id}`, data)
}

export function deleteHazardPointGroup(id: string): Promise<AjaxResult<null>> {
  return request.delete(`/hazard-point-groups/${id}`)
}

// 获取隐患点已绑定的设备
export function getBoundDevices(hpId: string) {
  return request.get(`/hazard-points/${hpId}/bound-devices`)
}

// 获取未绑定设备列表
export function getUnboundDevices(hpId: string, keyword?: string) {
  return request.get(`/hazard-points/${hpId}/unbound-devices`, { params: { keyword } })
}

// 绑定设备到隐患点
export function bindDevicesToHazardPoint(hpId: string, data: {
  deviceIds: number[],
  installPositions?: Array<{ deviceId: number, installLongitude: number, installLatitude: number }>
}) {
  return request.post(`/hazard-points/${hpId}/bind-devices`, data)
}

// 从隐患点解绑设备
export function unbindDevicesFromHazardPoint(hpId: string, deviceIds: number[]) {
  return request.delete(`/hazard-points/${hpId}/unbind-devices`, { data: { deviceIds } })
}

// 获取隐患点已绑定的视频设备
export function getBoundVideoDevices(hpId: string) {
  return request.get(`/hazard-points/${hpId}/bound-video-devices`)
}

// 绑定视频设备到隐患点
export function bindVideoDevicesToHazardPoint(hpId: string, data: {
  videoDeviceIds: number[],
  installPositions?: Array<{ videoDeviceId: number, installLongitude: number, installLatitude: number }>
}) {
  return request.post(`/hazard-points/${hpId}/bind-video-devices`, data)
}

// 从隐患点解绑视频设备
export function unbindVideoDevicesFromHazardPoint(hpId: string, videoDeviceIds: number[]) {
  return request.delete(`/hazard-points/${hpId}/unbind-video-devices`, { data: { videoDeviceIds } })
}

// ==================== 大屏/视图看板聚合接口 ====================

/** 隐患点监测率 */
export interface HazardPointMonitorRate {
  hazardPointId: number
  hazardPointName: string
  totalDevices: number
  activeDevices: number
  monitorRate?: number
}

export function getMonitorRates(windowMinutes?: number) {
  return request.get<HazardPointMonitorRate[]>('/hazard-points/monitor-rates', { params: { windowMinutes } })
}

/** 地图标记点 — 设备摘要 */
export interface DeviceMapItem {
  name: string
  status: 'online' | 'offline' | 'warning'
}

/** 地图总览 — 隐患点 + 设备 + 告警 */
export interface HazardPointMapVO {
  id: number
  name: string
  code: string
  type: string
  description: string
  longitude: number
  latitude: number
  status: number
  hasAlarm: boolean
  deviceCount: number
  level: string
  devices: DeviceMapItem[]
}

export function getMapOverview() {
  return request.get<HazardPointMapVO[]>('/hazard-points/map-overview')
}