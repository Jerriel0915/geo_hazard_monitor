// src/utils/device.ts
import http from '@/utils/api'

export interface DeviceInfo {
  id: number
  name: string
  code: string
  deviceTypeName: string
  status: string
  onlineStatus: number
  lastReportTime: string
  createTime?: string
  // 兼容字段（保留供现有 .vue 使用）
  deviceName: string
  deviceCode: string
  deviceType: string
}

export interface SensorAttr {
  attrCode: string
  attrName: string
  unit?: string
  rangeMin?: number | null
  rangeMax?: number | null
}

export interface DeviceSensor {
  id: number
  sensorNo?: string
  sensorName?: string
  monitorTypeName?: string
  attrs: SensorAttr[]
}

interface DeviceRawItem {
  id: number
  code?: string
  name?: string
  sn?: string
  deviceType?: number
  status?: number
  statusName?: string
  onlineStatus?: number
  lastReportTime?: string
  createTime?: string
}

const DEVICE_TYPE_MAP: Record<number, string> = {
  0: '单参数',
  1: '多参数',
  2: '本地组网',
}

function mapDevice(item: DeviceRawItem): DeviceInfo {
  const name = item.name || ''
  const code = item.code || ''
  const deviceTypeName = DEVICE_TYPE_MAP[item.deviceType ?? -1] || ''
  const onlineStatus = item.onlineStatus ?? 0
  const status = onlineStatus === 1 ? '在线' : '离线'
  return {
    id: item.id,
    name,
    code,
    deviceTypeName,
    status,
    onlineStatus,
    lastReportTime: item.lastReportTime || '',
    createTime: item.createTime,
    // 兼容字段
    deviceName: name,
    deviceCode: code,
    deviceType: deviceTypeName,
  }
}

export const deviceApi = {
  async getAll(): Promise<DeviceInfo[]> {
    try {
      // 优先尝试不分页接口
      const res = await http.get('/devices')
      const list = (res as any)?.rows || (res as any[]) || []
      return list.map(mapDevice)
    }
    catch (error) {
      console.error('获取设备列表失败，回退分页:', error)
      const res = await http.get('/devices/page', { pageNum: 1, pageSize: 200 })
      const list = (res as any)?.rows || []
      return list.map(mapDevice)
    }
  },

  async getById(id: number): Promise<DeviceInfo | undefined> {
    try {
      const res = await http.get(`/devices/${id}`)
      return mapDevice(res as DeviceRawItem)
    }
    catch (error) {
      console.error('获取设备详情失败:', error)
      return undefined
    }
  },

  async getSensors(deviceId: number): Promise<DeviceSensor[]> {
    try {
      const res = await http.get(`/devices/${deviceId}/sensors`)
      const list = (res as any)?.rows || (res as any[]) || []
      return list.map((s: any) => ({
        id: s.id,
        sensorNo: s.sensorNo,
        sensorName: s.sensorName,
        monitorTypeName: s.monitorTypeName,
        attrs: Array.isArray(s.attrList) ? s.attrList : (Array.isArray(s.attrs) ? s.attrs : []),
      }))
    }
    catch (error) {
      console.error('获取传感器列表失败:', error)
      return []
    }
  },

  /** @deprecated 未对接，返回空数组。alarm-detail.vue / container-detail.vue 暂用 */
  getByHazardId(_hazardId: number): DeviceInfo[] {
    return []
  },

  /** @deprecated 未对接，返回空数组。container-detail.vue 暂用 */
  getByContainerId(_containerId: number): DeviceInfo[] {
    return []
  },

  /** @deprecated 未对接，返回空数组。chart.vue 将在任务 6 中重写 */
  getHistoryData(_deviceId: number, _startTime: string, _endTime: string, _property?: string): any[] {
    return []
  },
}

export default deviceApi
