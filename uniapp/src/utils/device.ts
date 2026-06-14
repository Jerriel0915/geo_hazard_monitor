// src/utils/device.ts

export interface DeviceInfo {
  id: number
  deviceName: string
  deviceCode: string
  deviceType: string
  status: string
  lastReportTime: string
  hazardId: number
  hazardName: string
  installDate: string
  attributes: DeviceAttribute[]
}

export interface DeviceAttribute {
  property: string
  displayName: string
  unit: string
  currentValue?: number | null
}

const mockDevices: DeviceInfo[] = [
  {
    id: 1, deviceName: 'GNSS-001', deviceCode: 'GNSS-BP001-01',
    deviceType: 'GNSS', status: '在线',
    lastReportTime: '2026-06-08 10:28:00',
    hazardId: 1, hazardName: 'K15+200 右侧边坡',
    installDate: '2025-03-20',
    attributes: [
      { property: 'displacement_x', displayName: 'X方向位移', unit: 'mm', currentValue: 5.2 },
      { property: 'displacement_y', displayName: 'Y方向位移', unit: 'mm', currentValue: 3.8 },
      { property: 'displacement_z', displayName: 'Z方向位移', unit: 'mm', currentValue: 12.5 },
    ]
  },
  {
    id: 2, deviceName: 'GNSS-002', deviceCode: 'GNSS-BP001-02',
    deviceType: 'GNSS', status: '在线',
    lastReportTime: '2026-06-08 10:25:00',
    hazardId: 1, hazardName: 'K15+200 右侧边坡',
    installDate: '2025-03-20',
    attributes: [
      { property: 'surface_displacement', displayName: '表面位移', unit: 'mm', currentValue: 3.2 },
      { property: 'velocity', displayName: '位移速率', unit: 'mm/d', currentValue: 0.15 },
    ]
  },
  {
    id: 3, deviceName: 'CRACK-001', deviceCode: 'CRK-BP001-01',
    deviceType: '裂缝计', status: '在线',
    lastReportTime: '2026-06-08 10:26:00',
    hazardId: 1, hazardName: 'K15+200 右侧边坡',
    installDate: '2025-03-22',
    attributes: [
      { property: 'crack_width', displayName: '裂缝宽度', unit: 'mm', currentValue: 2.1 },
    ]
  },
  {
    id: 4, deviceName: 'GNSS-004', deviceCode: 'GNSS-BP002-01',
    deviceType: 'GNSS', status: '在线',
    lastReportTime: '2026-06-08 09:12:00',
    hazardId: 2, hazardName: 'K23+500 左侧边坡',
    installDate: '2025-04-25',
    attributes: [
      { property: 'displacement_x', displayName: 'X方向位移', unit: 'mm', currentValue: 4.1 },
      { property: 'displacement_y', displayName: 'Y方向位移', unit: 'mm', currentValue: 6.3 },
      { property: 'displacement_z', displayName: 'Z方向位移', unit: 'mm', currentValue: 7.8 },
    ]
  },
  {
    id: 5, deviceName: 'INCLINE-001', deviceCode: 'INC-BP002-01',
    deviceType: '测斜仪', status: '在线',
    lastReportTime: '2026-06-08 09:10:00',
    hazardId: 2, hazardName: 'K23+500 左侧边坡',
    installDate: '2025-04-28',
    attributes: [
      { property: 'angle', displayName: '倾斜角度', unit: '°', currentValue: 0.15 },
    ]
  },
  {
    id: 6, deviceName: 'RAIN-002', deviceCode: 'RAN-BP002-01',
    deviceType: '雨量计', status: '在线',
    lastReportTime: '2026-06-08 09:08:00',
    hazardId: 2, hazardName: 'K23+500 左侧边坡',
    installDate: '2025-04-25',
    attributes: [
      { property: 'hourly_rain', displayName: '小时雨量', unit: 'mm/h', currentValue: 15.2 },
      { property: 'daily_rain', displayName: '日累计雨量', unit: 'mm', currentValue: 45.6 },
    ]
  },
  {
    id: 7, deviceName: 'RAIN-001', deviceCode: 'RAN-BP003-01',
    deviceType: '雨量计', status: '在线',
    lastReportTime: '2026-06-08 08:40:00',
    hazardId: 3, hazardName: 'K31+100 右侧边坡',
    installDate: '2025-05-15',
    attributes: [
      { property: 'hourly_rain', displayName: '小时雨量', unit: 'mm/h', currentValue: 28.0 },
      { property: 'daily_rain', displayName: '日累计雨量', unit: 'mm', currentValue: 62.3 },
    ]
  },
  {
    id: 8, deviceName: 'GNSS-005', deviceCode: 'GNSS-BP003-01',
    deviceType: 'GNSS', status: '离线',
    lastReportTime: '2026-06-07 18:30:00',
    hazardId: 3, hazardName: 'K31+100 右侧边坡',
    installDate: '2025-05-15',
    attributes: [
      { property: 'displacement_x', displayName: 'X方向位移', unit: 'mm', currentValue: 1.2 },
      { property: 'displacement_z', displayName: 'Z方向位移', unit: 'mm', currentValue: 2.1 },
    ]
  },
  {
    id: 9, deviceName: 'GNSS-003', deviceCode: 'GNSS-BP004-01',
    deviceType: 'GNSS', status: '在线',
    lastReportTime: '2026-06-08 10:20:00',
    hazardId: 4, hazardName: 'K42+800 左侧边坡',
    installDate: '2025-03-05',
    attributes: [
      { property: 'displacement_x', displayName: 'X方向位移', unit: 'mm', currentValue: 6.7 },
      { property: 'displacement_z', displayName: 'Z方向位移', unit: 'mm', currentValue: 9.5 },
    ]
  },
  {
    id: 10, deviceName: 'GNSS-006', deviceCode: 'GNSS-BP004-02',
    deviceType: 'GNSS', status: '在线',
    lastReportTime: '2026-06-08 06:48:00',
    hazardId: 4, hazardName: 'K42+800 左侧边坡',
    installDate: '2025-03-05',
    attributes: [
      { property: 'deep_displacement', displayName: '深埋位移', unit: 'mm', currentValue: 8.3 },
      { property: 'velocity', displayName: '位移速率', unit: 'mm/d', currentValue: 0.35 },
    ]
  },
  {
    id: 11, deviceName: 'WATER-001', deviceCode: 'WAT-BP004-01',
    deviceType: '水位计', status: '维修',
    lastReportTime: '2026-06-06 14:00:00',
    hazardId: 4, hazardName: 'K42+800 左侧边坡',
    installDate: '2025-03-08',
    attributes: [
      { property: 'water_level', displayName: '地下水位', unit: 'm', currentValue: null },
    ]
  },
  {
    id: 12, deviceName: 'GNSS-007', deviceCode: 'GNSS-BP005-01',
    deviceType: 'GNSS', status: '在线',
    lastReportTime: '2026-06-08 10:15:00',
    hazardId: 5, hazardName: 'K56+300 右侧边坡',
    installDate: '2025-01-20',
    attributes: [
      { property: 'displacement_x', displayName: 'X方向位移', unit: 'mm', currentValue: 0.8 },
      { property: 'displacement_z', displayName: 'Z方向位移', unit: 'mm', currentValue: 1.2 },
    ]
  },
  {
    id: 13, deviceName: 'RAIN-003', deviceCode: 'RAN-BP005-01',
    deviceType: '雨量计', status: '离线',
    lastReportTime: '2026-06-07 22:00:00',
    hazardId: 5, hazardName: 'K56+300 右侧边坡',
    installDate: '2025-01-20',
    attributes: [
      { property: 'hourly_rain', displayName: '小时雨量', unit: 'mm/h', currentValue: null },
      { property: 'daily_rain', displayName: '日累计雨量', unit: 'mm', currentValue: 12.5 },
    ]
  },
  {
    id: 14, deviceName: 'CAM-001', deviceCode: 'CAM-BP001-01',
    deviceType: '视频设备', status: '在线',
    lastReportTime: '2026-06-08 10:29:00',
    hazardId: 1, hazardName: 'K15+200 右侧边坡',
    installDate: '2025-03-25',
    attributes: [
      { property: 'video_status', displayName: '视频状态', unit: '', currentValue: 1 },
    ]
  },
  {
    id: 15, deviceName: 'CAM-002', deviceCode: 'CAM-BP002-01',
    deviceType: '视频设备', status: '在线',
    lastReportTime: '2026-06-08 10:25:00',
    hazardId: 2, hazardName: 'K23+500 左侧边坡',
    installDate: '2025-04-30',
    attributes: [
      { property: 'video_status', displayName: '视频状态', unit: '', currentValue: 1 },
    ]
  },
  {
    id: 16, deviceName: 'CAM-003', deviceCode: 'CAM-BP004-01',
    deviceType: '视频设备', status: '离线',
    lastReportTime: '2026-06-07 16:00:00',
    hazardId: 4, hazardName: 'K42+800 左侧边坡',
    installDate: '2025-03-10',
    attributes: [
      { property: 'video_status', displayName: '视频状态', unit: '', currentValue: 0 },
    ]
  },
]

export const deviceApi = {
  getAll(): DeviceInfo[] {
    return mockDevices
  },

  getById(id: number): DeviceInfo | undefined {
    return mockDevices.find(d => d.id === id)
  },

  getByHazardId(hazardId: number): DeviceInfo[] {
    return mockDevices.filter(d => d.hazardId === hazardId)
  },

  getHistoryData(deviceId: number, startTime: string, endTime: string, property?: string) {
    const hours = Math.round((new Date(endTime).getTime() - new Date(startTime).getTime()) / 3600000)
    const data = []
    const device = mockDevices.find(d => d.id === deviceId)
    const attr = device?.attributes.find(a => a.property === property)
    const baseValue = attr?.currentValue || 5

    for (let i = 0; i < Math.min(hours, 72); i++) {
      const time = new Date(new Date(startTime).getTime() + i * 3600000)
      data.push({
        time: `${time.getFullYear()}-${String(time.getMonth()+1).padStart(2,'0')}-${String(time.getDate()).padStart(2,'0')} ${String(time.getHours()).padStart(2,'0')}:${String(time.getMinutes()).padStart(2,'0')}:00`,
        value: (baseValue + (Math.random() - 0.5) * baseValue * 0.4).toFixed(2)
      })
    }
    return data
  }
}

export default deviceApi
