// src/utils/hazard.ts
import http from '@/utils/api'

export interface Hazard {
  id: number
  name: string
  code?: string
  groupName?: string
  longitude?: number
  latitude?: number
  location: string
  status: string
  deviceCount: number
  description?: string
  createTime: string
}

export interface HazardDetail extends Hazard {
  groupName?: string
  strike?: number
  boundaryCoords?: string
  updateBy?: string
  updateTime?: string
}

export interface HazardWithDevices extends HazardDetail {
  devices: any[]
}

interface HazardRawItem {
  id: number
  code?: string
  name: string
  groupName?: string
  longitude?: number
  latitude?: number
  strike?: number
  boundaryCoords?: string
  description?: string
  status?: number
  statusName?: string
  deviceCount?: number
  createTime?: string
  updateBy?: string
  updateTime?: string
}

function formatLocation(item: HazardRawItem): string {
  if (item.longitude != null && item.latitude != null) {
    return `${Number(item.longitude).toFixed(6)}, ${Number(item.latitude).toFixed(6)}`
  }
  return '-'
}

/** 防御性字符串转换，避免 [object Object] */
function safeString(val: any): string {
  if (val == null)
    return ''
  if (typeof val === 'string')
    return val
  if (typeof val === 'object')
    return val.text || val.content || val.value || ''
  return String(val)
}

function mapHazard(item: HazardRawItem): Hazard {
  return {
    id: item.id,
    name: item.name,
    code: item.code,
    groupName: item.groupName,
    longitude: item.longitude,
    latitude: item.latitude,
    location: formatLocation(item),
    status: item.statusName || (item.status === 1 ? '监测中' : item.status === 2 ? '停测中' : '已完结'),
    deviceCount: item.deviceCount || 0,
    description: safeString(item.description),
    createTime: item.createTime || '',
  }
}

export const hazardApi = {
  async getAll(): Promise<Hazard[]> {
    const res = await http.get('/hazard-points/page', {
      pageNum: 1,
      pageSize: 200,
    })
    const list = (res as any)?.rows || (res as any[]) || []
    return list.map(mapHazard)
  },

  async getById(id: number): Promise<HazardWithDevices | undefined> {
    try {
      const res = await http.get(`/hazard-points/${id}`)
      const item = res as HazardRawItem
      const base = mapHazard(item)
      return {
        ...base,
        groupName: item.groupName,
        strike: item.strike,
        boundaryCoords: item.boundaryCoords,
        updateBy: item.updateBy,
        updateTime: item.updateTime,
        devices: [],
      }
    }
    catch (error) {
      console.error('获取隐患点详情失败:', error)
      return undefined
    }
  },

  async getBoundDevices(hazardPointId: number): Promise<any[]> {
    try {
      const res = await http.get(`/hazard-points/${hazardPointId}/bound-devices`)
      if (Array.isArray(res))
        return res
      if (res && Array.isArray((res as any).rows))
        return (res as any).rows
      if (res && Array.isArray((res as any).data))
        return (res as any).data
      console.warn('getBoundDevices: 意外的响应格式', typeof res, res)
      return []
    }
    catch (error) {
      console.error('获取绑定设备失败:', error)
      return []
    }
  },
}

export default hazardApi
