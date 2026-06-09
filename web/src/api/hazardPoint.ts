import request from '@/utils/request'

export interface HazardPointListParams {
  pageNum?: number
  pageSize?: number
  code?: string
  name?: string
  groupId?: number
  status?: number
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

export function getHazardPointPage(params: HazardPointListParams) {
  return request.get('/hazard-points/page', { params })
}

export function getHazardPointDetail(id: string) {
  return request.get(`/hazard-points/${id}`)
}

export function createHazardPoint(data: HazardPointPayload) {
  return request.post('/hazard-points', data)
}

export function updateHazardPoint(id: string, data: HazardPointPayload) {
  return request.put(`/hazard-points/${id}`, data)
}

export function deleteHazardPoint(id: string) {
  return request.delete(`/hazard-points/${id}`)
}

export function deleteHazardPoints(ids: number[]) {
  return request.delete('/hazard-points/batch', { data: { ids } })
}

export function pauseHazardPoint(id: string, pause: boolean) {
  return request.put(`/hazard-points/${id}/pause`, { pause })
}

export function completeHazardPoint(id: string) {
  return request.put(`/hazard-points/${id}/complete`, {})
}

export function batchOperateHazardPoints(ids: number[], operation: 'pause' | 'resume' | 'complete') {
  return request.put('/hazard-points/batch/operate', { ids, operation })
}

export function exportHazardPoints(data: HazardPointExportPayload) {
  return request.raw.post('/hazard-points/export', data, { responseType: 'blob' })
}

export function getHazardPointGroups() {
  return request.get('/hazard-point-groups')
}

export function createHazardPointGroup(data: GroupPayload) {
  return request.post('/hazard-point-groups', data)
}

export function updateHazardPointGroup(id: string, data: Omit<GroupPayload, 'code'>) {
  return request.put(`/hazard-point-groups/${id}`, data)
}

export function deleteHazardPointGroup(id: string) {
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