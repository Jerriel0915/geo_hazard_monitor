// src/utils/video.ts
import http from '@/utils/api'

export interface VideoDevice {
  id: number
  deviceName: string
  deviceCode: string
  streamUrl: string
  status: number
  onlineStatus?: number
  manufacturer?: string
  protocolCode?: string
  protocolName?: string
  iconPath?: string
  longitude?: number
  latitude?: number
  hazardPointIds?: string
  createTime?: string
}

interface VideoRawItem {
  id: number
  name?: string
  code?: string
  streamUrl?: string
  status?: number
  onlineStatus?: number
  manufacturer?: string
  protocolCode?: string
  protocolName?: string
  icon?: string
  iconPath?: string
  longitude?: number
  latitude?: number
  hazardPointIds?: string
  createTime?: string
}

function mapVideo(item: VideoRawItem): VideoDevice {
  return {
    id: item.id,
    deviceName: item.name || '',
    deviceCode: item.code || '',
    streamUrl: item.streamUrl || '',
    status: item.status ?? 0,
    onlineStatus: item.onlineStatus,
    manufacturer: item.manufacturer,
    protocolCode: item.protocolCode,
    protocolName: item.protocolName,
    iconPath: item.iconPath,
    longitude: item.longitude,
    latitude: item.latitude,
    hazardPointIds: item.hazardPointIds,
    createTime: item.createTime,
  }
}

export const videoApi = {
  async getPage(params?: { pageNum?: number; pageSize?: number }): Promise<{ rows: VideoDevice[], total: number }> {
    try {
      const res = await http.get('/video-devices/page', {
        pageNum: params?.pageNum || 1,
        pageSize: params?.pageSize || 200,
      })
      const rawList = (res as any)?.rows || []
      const total = (res as any)?.total || rawList.length
      return { rows: rawList.map(mapVideo), total }
    } catch (error) {
      console.error('获取视频设备列表失败:', error)
      return { rows: [], total: 0 }
    }
  },

  async getById(id: number): Promise<VideoDevice | undefined> {
    try {
      const res = await http.get(`/video-devices/${id}`)
      return mapVideo(res as VideoRawItem)
    } catch (error) {
      console.error('获取视频设备详情失败:', error)
      return undefined
    }
  },
}

export default videoApi
