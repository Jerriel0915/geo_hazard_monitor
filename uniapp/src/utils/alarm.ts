// src/utils/alarm.ts

export interface Alarm {
  id: number
  hazardId: number
  hazardName: string
  deviceId: number
  deviceName: string
  alarmType: string
  alarmLevel: number
  alarmContent: string
  alarmValue: string
  status: number
  createTime: string
  handleTime?: string
  handleRemark?: string
  alarmCount: number
  dispatchLogs: DispatchLog[]
}

export interface DispatchLog {
  id: number
  action: string
  operator: string
  time: string
}

const mockAlarms: Alarm[] = [
  {
    id: 1, hazardId: 1, hazardName: 'K15+200 右侧边坡',
    deviceId: 1, deviceName: 'GNSS-001',
    alarmType: '阈值预警', alarmLevel: 4,
    alarmContent: '深埋位移超过红色预警阈值，当前值 12.5mm，阈值 10mm',
    alarmValue: '12.5mm', status: 0,
    createTime: '2026-06-08 10:30:00',
    alarmCount: 5,
    dispatchLogs: [
      { id: 1, action: '系统自动告警', operator: '系统', time: '2026-06-08 10:30:00' },
      { id: 2, action: '短信通知责任人', operator: '系统', time: '2026-06-08 10:30:05' },
      { id: 3, action: 'App推送通知', operator: '系统', time: '2026-06-08 10:30:10' },
    ]
  },
  {
    id: 2, hazardId: 2, hazardName: 'K23+500 左侧边坡',
    deviceId: 4, deviceName: 'GNSS-004',
    alarmType: '综合预警', alarmLevel: 3,
    alarmContent: '综合分析：位移速率加快且雨量持续增大，综合评估为橙色预警',
    alarmValue: '-', status: 0,
    createTime: '2026-06-08 09:15:00',
    alarmCount: 3,
    dispatchLogs: [
      { id: 1, action: '综合评估触发', operator: '系统', time: '2026-06-08 09:15:00' },
      { id: 2, action: '短信通知责任人', operator: '系统', time: '2026-06-08 09:15:05' },
    ]
  },
  {
    id: 3, hazardId: 3, hazardName: 'K31+100 右侧边坡',
    deviceId: 7, deviceName: 'RAIN-001',
    alarmType: '阈值预警', alarmLevel: 2,
    alarmContent: '小时雨量超过黄色预警阈值，当前值 28mm/h，阈值 25mm/h',
    alarmValue: '28mm/h', status: 0,
    createTime: '2026-06-08 08:45:00',
    alarmCount: 2,
    dispatchLogs: [
      { id: 1, action: '系统自动告警', operator: '系统', time: '2026-06-08 08:45:00' },
      { id: 2, action: 'App推送通知', operator: '系统', time: '2026-06-08 08:45:05' },
    ]
  },
  {
    id: 4, hazardId: 1, hazardName: 'K15+200 右侧边坡',
    deviceId: 2, deviceName: 'GNSS-002',
    alarmType: '阈值预警', alarmLevel: 1,
    alarmContent: '表面位移变化量超过蓝色预警阈值，当前值 3.2mm，阈值 3mm',
    alarmValue: '3.2mm', status: 0,
    createTime: '2026-06-08 07:20:00',
    alarmCount: 1,
    dispatchLogs: [
      { id: 1, action: '系统自动告警', operator: '系统', time: '2026-06-08 07:20:00' },
    ]
  },
  {
    id: 5, hazardId: 4, hazardName: 'K42+800 左侧边坡',
    deviceId: 10, deviceName: 'GNSS-006',
    alarmType: '阈值预警', alarmLevel: 3,
    alarmContent: '深埋位移超过橙色预警阈值，当前值 8.3mm，阈值 7mm',
    alarmValue: '8.3mm', status: 0,
    createTime: '2026-06-08 06:50:00',
    alarmCount: 4,
    dispatchLogs: [
      { id: 1, action: '系统自动告警', operator: '系统', time: '2026-06-08 06:50:00' },
      { id: 2, action: '短信通知责任人', operator: '系统', time: '2026-06-08 06:50:05' },
    ]
  },
  {
    id: 6, hazardId: 2, hazardName: 'K23+500 左侧边坡',
    deviceId: 5, deviceName: 'INCLINE-001',
    alarmType: '阈值预警', alarmLevel: 2,
    alarmContent: '倾斜角度超过黄色预警阈值，当前值 0.15°，阈值 0.1°',
    alarmValue: '0.15°', status: 1,
    createTime: '2026-06-07 14:30:00',
    handleTime: '2026-06-07 15:00:00',
    handleRemark: '已现场核实，设备基座轻微松动，已加固处理',
    alarmCount: 1,
    dispatchLogs: [
      { id: 1, action: '系统自动告警', operator: '系统', time: '2026-06-07 14:30:00' },
      { id: 2, action: 'App推送通知', operator: '系统', time: '2026-06-07 14:30:05' },
      { id: 3, action: '处理完成', operator: '张工', time: '2026-06-07 15:00:00' },
    ]
  },
]

export const alarmApi = {
  getAll(): Alarm[] {
    return mockAlarms
  },

  getUnprocessed(): Alarm[] {
    return mockAlarms.filter(a => a.status === 0)
  },

  getStats(): { red: number; orange: number; yellow: number; blue: number } {
    const unprocessed = mockAlarms.filter(a => a.status === 0)
    return {
      red: unprocessed.filter(a => a.alarmLevel === 4).length,
      orange: unprocessed.filter(a => a.alarmLevel === 3).length,
      yellow: unprocessed.filter(a => a.alarmLevel === 2).length,
      blue: unprocessed.filter(a => a.alarmLevel === 1).length,
    }
  },

  getById(id: number): Alarm | undefined {
    return mockAlarms.find(a => a.id === id)
  },

  handle(alarmId: number, remark: string): boolean {
    const alarm = mockAlarms.find(a => a.id === alarmId)
    if (alarm) {
      alarm.status = 1
      alarm.handleTime = new Date().toISOString()
      alarm.handleRemark = remark
    }
    return true
  }
}

export default alarmApi
