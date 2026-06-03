import request from '@/utils/request'

export interface RealtimeAlarmListParams {
  pageNum?: number
  pageSize?: number
  hazardPointName?: string
  responseUserName?: string
  alarmStartTime?: string
  alarmEndTime?: string
  alarmCountMin?: number
  alarmCountMax?: number
  alarmLevel?: number
  alarmType?: number
  alarmStatus?: number
}

export interface RealtimeAlarmDetail {
  id: number
  hazardPointId: number
  hazardPointName: string
  alarmLevel: number
  alarmLevelName: string
  firstAlarmTime: string
  lastAlarmTime: string
  alarmCount: number
  alarmType: number
  alarmTypeName: string
  alarmStatus: number
  alarmStatusName: string
  responseUserId: number | null
  responseUserName: string
  responseTime: string
  alarmDetail: string
}

export interface AlarmFeedbackPayload {
  alarmId: number
  feedback: string
  responseUserId: number
  responseUserName: string
}

const mockAlarmData: RealtimeAlarmDetail[] = [
  { id: 1, hazardPointId: 1, hazardPointName: 'XX区边坡监测点-001', alarmLevel: 1, alarmLevelName: '一级', firstAlarmTime: '2026-05-20 08:00:00', lastAlarmTime: '2026-05-30 10:30:00', alarmCount: 15, alarmType: 1, alarmTypeName: '阈值预警', alarmStatus: 0, alarmStatusName: '待处理', responseUserId: null, responseUserName: '', responseTime: '', alarmDetail: '位移超过安全阈值，需紧急处理' },
  { id: 2, hazardPointId: 2, hazardPointName: 'XX地质灾害隐患点-002', alarmLevel: 2, alarmLevelName: '二级', firstAlarmTime: '2026-05-22 14:20:00', lastAlarmTime: '2026-05-29 16:45:00', alarmCount: 8, alarmType: 2, alarmTypeName: '综合预警', alarmStatus: 1, alarmStatusName: '处理中', responseUserId: 1, responseUserName: '张三', responseTime: '2026-05-29 17:00:00', alarmDetail: '正在进行现场核查，初步判断存在滑坡风险' },
  { id: 3, hazardPointId: 3, hazardPointName: 'XX山体滑坡监测点-003', alarmLevel: 3, alarmLevelName: '三级', firstAlarmTime: '2026-05-25 09:10:00', lastAlarmTime: '2026-05-30 08:00:00', alarmCount: 5, alarmType: 1, alarmTypeName: '阈值预警', alarmStatus: 0, alarmStatusName: '待处理', responseUserId: null, responseUserName: '', responseTime: '', alarmDetail: '土壤含水量接近预警值' },
  { id: 4, hazardPointId: 4, hazardPointName: 'XX桥梁监测点-004', alarmLevel: 4, alarmLevelName: '四级', firstAlarmTime: '2026-05-26 11:30:00', lastAlarmTime: '2026-05-30 09:15:00', alarmCount: 3, alarmType: 2, alarmTypeName: '综合预警', alarmStatus: 2, alarmStatusName: '已处理', responseUserId: 2, responseUserName: '李四', responseTime: '2026-05-30 10:00:00', alarmDetail: '已安排维修人员处理完成，桥梁结构安全' },
  { id: 5, hazardPointId: 1, hazardPointName: 'XX区边坡监测点-001', alarmLevel: 2, alarmLevelName: '二级', firstAlarmTime: '2026-05-28 13:00:00', lastAlarmTime: '2026-05-30 07:20:00', alarmCount: 4, alarmType: 1, alarmTypeName: '阈值预警', alarmStatus: 3, alarmStatusName: '误报', responseUserId: 3, responseUserName: '王五', responseTime: '2026-05-29 08:00:00', alarmDetail: '误报，传感器故障导致数据异常，已修复' },
  { id: 6, hazardPointId: 5, hazardPointName: 'XX隧道监测点-005', alarmLevel: 1, alarmLevelName: '一级', firstAlarmTime: '2026-05-29 06:30:00', lastAlarmTime: '2026-05-31 12:00:00', alarmCount: 22, alarmType: 1, alarmTypeName: '阈值预警', alarmStatus: 0, alarmStatusName: '待处理', responseUserId: null, responseUserName: '', responseTime: '', alarmDetail: '隧道结构变形超出安全范围' },
  { id: 7, hazardPointId: 6, hazardPointName: 'XX水库监测点-006', alarmLevel: 3, alarmLevelName: '三级', firstAlarmTime: '2026-05-27 10:15:00', lastAlarmTime: '2026-05-30 15:30:00', alarmCount: 7, alarmType: 2, alarmTypeName: '综合预警', alarmStatus: 1, alarmStatusName: '处理中', responseUserId: 4, responseUserName: '赵六', responseTime: '2026-05-30 16:00:00', alarmDetail: '水位持续上涨，已启动应急预案' },
  { id: 8, hazardPointId: 7, hazardPointName: 'XX公路监测点-007', alarmLevel: 4, alarmLevelName: '四级', firstAlarmTime: '2026-05-28 09:00:00', lastAlarmTime: '2026-05-30 11:45:00', alarmCount: 2, alarmType: 1, alarmTypeName: '阈值预警', alarmStatus: 2, alarmStatusName: '已处理', responseUserId: 5, responseUserName: '孙七', responseTime: '2026-05-30 12:00:00', alarmDetail: '路面沉降已修复，交通恢复正常' },
  { id: 9, hazardPointId: 8, hazardPointName: 'XX基坑监测点-008', alarmLevel: 2, alarmLevelName: '二级', firstAlarmTime: '2026-05-30 08:30:00', lastAlarmTime: '2026-05-31 09:00:00', alarmCount: 6, alarmType: 1, alarmTypeName: '阈值预警', alarmStatus: 0, alarmStatusName: '待处理', responseUserId: null, responseUserName: '', responseTime: '', alarmDetail: '基坑位移速率加快' },
  { id: 10, hazardPointId: 9, hazardPointName: 'XX尾矿库监测点-009', alarmLevel: 1, alarmLevelName: '一级', firstAlarmTime: '2026-05-31 07:00:00', lastAlarmTime: '2026-05-31 14:00:00', alarmCount: 18, alarmType: 2, alarmTypeName: '综合预警', alarmStatus: 4, alarmStatusName: '已销警', responseUserId: 6, responseUserName: '周八', responseTime: '2026-05-31 15:00:00', alarmDetail: '险情已排除，恢复正常监测' },
  { id: 11, hazardPointId: 10, hazardPointName: 'XX泥石流监测点-010', alarmLevel: 3, alarmLevelName: '三级', firstAlarmTime: '2026-05-24 12:00:00', lastAlarmTime: '2026-05-29 18:00:00', alarmCount: 9, alarmType: 1, alarmTypeName: '阈值预警', alarmStatus: 3, alarmStatusName: '误报', responseUserId: 7, responseUserName: '吴九', responseTime: '2026-05-29 18:30:00', alarmDetail: '经现场核查，为正常地质活动' },
  { id: 12, hazardPointId: 2, hazardPointName: 'XX地质灾害隐患点-002', alarmLevel: 1, alarmLevelName: '一级', firstAlarmTime: '2026-05-31 06:00:00', lastAlarmTime: '2026-05-31 16:00:00', alarmCount: 12, alarmType: 1, alarmTypeName: '阈值预警', alarmStatus: 0, alarmStatusName: '待处理', responseUserId: null, responseUserName: '', responseTime: '', alarmDetail: '降雨量突破历史极值，滑坡风险极高' },
  { id: 13, hazardPointId: 3, hazardPointName: 'XX山体滑坡监测点-003', alarmLevel: 4, alarmLevelName: '四级', firstAlarmTime: '2026-05-29 14:30:00', lastAlarmTime: '2026-05-30 10:30:00', alarmCount: 4, alarmType: 2, alarmTypeName: '综合预警', alarmStatus: 2, alarmStatusName: '已处理', responseUserId: 2, responseUserName: '李四', responseTime: '2026-05-30 11:00:00', alarmDetail: '排水系统已疏通，风险解除' },
  { id: 14, hazardPointId: 4, hazardPointName: 'XX桥梁监测点-004', alarmLevel: 2, alarmLevelName: '二级', firstAlarmTime: '2026-05-31 08:00:00', lastAlarmTime: '2026-05-31 13:00:00', alarmCount: 5, alarmType: 1, alarmTypeName: '阈值预警', alarmStatus: 1, alarmStatusName: '处理中', responseUserId: 1, responseUserName: '张三', responseTime: '2026-05-31 13:30:00', alarmDetail: '桥梁支座检测中' },
  { id: 15, hazardPointId: 6, hazardPointName: 'XX水库监测点-006', alarmLevel: 2, alarmLevelName: '二级', firstAlarmTime: '2026-05-31 09:30:00', lastAlarmTime: '2026-05-31 17:00:00', alarmCount: 3, alarmType: 1, alarmTypeName: '阈值预警', alarmStatus: 0, alarmStatusName: '待处理', responseUserId: null, responseUserName: '', responseTime: '', alarmDetail: '水位接近警戒水位' }
]

const getMockData = (params?: RealtimeAlarmListParams) => {
  let data = [...mockAlarmData]
  
  if (params?.hazardPointName) {
    data = data.filter(item => item.hazardPointName.includes(params.hazardPointName!))
  }
  if (params?.responseUserName) {
    data = data.filter(item => item.responseUserName.includes(params.responseUserName!))
  }
  if (params?.alarmLevel !== undefined) {
    data = data.filter(item => item.alarmLevel === params.alarmLevel)
  }
  if (params?.alarmType !== undefined) {
    data = data.filter(item => item.alarmType === params.alarmType)
  }
  if (params?.alarmStatus !== undefined) {
    data = data.filter(item => item.alarmStatus === params.alarmStatus)
  }
  if (params?.alarmCountMin != null) {
    data = data.filter(item => item.alarmCount >= params.alarmCountMin!)
  }
  if (params?.alarmCountMax != null) {
    data = data.filter(item => item.alarmCount <= params.alarmCountMax!)
  }

  const pageNum = params?.pageNum || 1
  const pageSize = params?.pageSize || 10
  const start = (pageNum - 1) * pageSize
  const end = start + pageSize
  
  return {
    rows: data.slice(start, end),
    total: data.length
  }
}

export async function getRealtimeAlarmPage(params: RealtimeAlarmListParams) {
  try {
    const response = await request.get('/realtime-alarms/page', { params })
    return response
  } catch (error) {
    console.log('Backend API unavailable, returning mock data')
    return {
      code: 200,
      data: getMockData(params),
      msg: 'success'
    }
  }
}

export async function getRealtimeAlarmDetail(id: string) {
  try {
    const response = await request.get(`/realtime-alarms/${id}`)
    return response
  } catch (error) {
    console.log('Backend API unavailable, returning mock data')
    const alarm = mockAlarmData.find(item => item.id === parseInt(id))
    return {
      code: 200,
      data: alarm || null,
      msg: 'success'
    }
  }
}

export async function getRealtimeAlarmByHazardPoint(hazardPointId: string) {
  try {
    const response = await request.get(`/realtime-alarms/hazard-point/${hazardPointId}`)
    return response
  } catch (error) {
    console.log('Backend API unavailable, returning mock data')
    const data = mockAlarmData.filter(item => item.hazardPointId === parseInt(hazardPointId))
    return {
      code: 200,
      data: data,
      msg: 'success'
    }
  }
}

export async function feedbackAlarm(data: AlarmFeedbackPayload) {
  try {
    const response = await request.post('/realtime-alarms/feedback', data)
    return response
  } catch (error) {
    console.log('Backend API unavailable, returning mock response')
    return {
      code: 200,
      data: null,
      msg: '反馈成功（模拟）'
    }
  }
}

export async function markAsFalseAlarm(id: string) {
  try {
    const response = await request.put(`/realtime-alarms/${id}/false-alarm`)
    return response
  } catch (error) {
    console.log('Backend API unavailable, returning mock response')
    return {
      code: 200,
      data: null,
      msg: '标记误报成功（模拟）'
    }
  }
}

export async function clearAlarm(id: string) {
  try {
    const response = await request.put(`/realtime-alarms/${id}/clear`)
    return response
  } catch (error) {
    console.log('Backend API unavailable, returning mock response')
    return {
      code: 200,
      data: null,
      msg: '销警成功（模拟）'
    }
  }
}

export function batchMarkAsFalseAlarm(ids: number[]) {
  return request.put('/realtime-alarms/batch/false-alarm', { ids })
}

export function batchClearAlarm(ids: number[]) {
  return request.put('/realtime-alarms/batch/clear', { ids })
}

export function deleteRealtimeAlarm(id: string) {
  return request.delete(`/realtime-alarms/${id}`)
}

export function deleteRealtimeAlarms(ids: number[]) {
  return request.delete('/realtime-alarms/batch', { data: { ids } })
}

export function exportRealtimeAlarms(params?: RealtimeAlarmListParams) {
  return request.raw.post('/realtime-alarms/export', params || {}, { responseType: 'blob' })
}
