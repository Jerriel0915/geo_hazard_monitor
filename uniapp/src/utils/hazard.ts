// src/utils/hazard.ts
import http from '@/utils/api'

export interface Hazard {
  id: number
  name: string
  code?: string
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

function mapHazard(item: HazardRawItem): Hazard {
  return {
    id: item.id,
    name: item.name,
    code: item.code,
    longitude: item.longitude,
    latitude: item.latitude,
    location: formatLocation(item),
    status: item.statusName || (item.status === 1 ? '监测中' : '已停测'),
    deviceCount: item.deviceCount || 0,
    description: item.description || '',
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
      const list = (res as any)?.rows || (res as any[]) || []
      return list
    }
    catch (error) {
      console.error('获取绑定设备失败:', error)
      return []
    }
  },
}

export default hazardApi
