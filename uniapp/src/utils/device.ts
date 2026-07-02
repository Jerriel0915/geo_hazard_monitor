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
  /** 安装位置（文字描述） */
  installLocation?: string
  /** 纬度 */
  latitude?: number
  /** 经度 */
  longitude?: number
  /** 传感器数量 */
  sensorCount?: number
  /** 绑定的隐患点ID（Service层富化） */
  boundHazardPointId?: number
  /** 绑定的隐患点名称 */
  boundHazardPointName?: string
  /** 监测类型列表（从传感器提取，如 GNSS/雨量计/测斜仪） */
  monitorTypes?: string[]
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
  installLocation?: string
  latitude?: number
  longitude?: number
  sensorCount?: number
  boundHazardPointId?: number
  boundHazardPointName?: string
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
  const status = item.status === 1 ? '正常' : item.status === 2 ? '维修' : item.status === 3 ? '停用' : (item.statusName || '正常')
  // 安装位置：优先使用文字描述，否则用经纬度拼接
  let installLocation = item.installLocation || ''
  if (!installLocation && item.latitude != null && item.longitude != null) {
    installLocation = `${Number(item.longitude).toFixed(6)}, ${Number(item.latitude).toFixed(6)}`
  }
  return {
    id: item.id,
    name,
    code,
    deviceTypeName,
    status,
    onlineStatus,
    lastReportTime: item.lastReportTime || '',
    createTime: item.createTime,
    installLocation,
    latitude: item.latitude,
    longitude: item.longitude,
    sensorCount: item.sensorCount,
    boundHazardPointId: item.boundHazardPointId,
    boundHazardPointName: item.boundHazardPointName,
    // 兼容字段
    deviceName: name,
    deviceCode: code,
    deviceType: deviceTypeName,
  }
}

export const deviceApi = {
  async getPage(pageNum: number, pageSize: number): Promise<{ rows: DeviceInfo[], total: number }> {
    try {
      const res = await http.get('/devices/page', { pageNum, pageSize })
      const rawList = (res as any)?.rows || []
      const total = (res as any)?.total || 0
      return { rows: rawList.map(mapDevice), total }
    }
    catch (error) {
      console.error('获取设备列表失败:', error)
      return { rows: [], total: 0 }
    }
  },

  /** 获取所有监测类型（用于筛选项） */
  async getMonitorTypes(): Promise<{ id: number, name: string, code: string }[]> {
    try {
      const res = await http.get('/monitor-types')
      const list = (res as any)?.rows || (res as any[]) || []
      return list
    }
    catch (error) {
      console.error('获取监测类型失败:', error)
      return []
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
        sensorNo: s.sensorCode || s.sensorNo,
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
