import type { AjaxResult, PageResult } from './system'

// ==================== 类型定义 ====================

export interface CompositeAlarmItem {
  id: number
  name: string
  description: string
  triggerMode: 'PERIODIC' | 'REALTIME'
  cronExpression?: string
  subscriptionConfig?: SubscriptionConfig
  scriptXml?: string
  scriptCode?: string
  silenceSeconds: number
  sustainSeconds: number
  levelChangeNotify: boolean
  status: 'ENABLED' | 'DISABLED'
  createTime: string
  updateTime?: string
  createBy?: string
  /** 应用范围内隐患点数量 */
  scopeCount?: number
  /** 最近运行状态 */
  lastRunStatus?: 'SUCCESS' | 'ERROR' | 'TIMEOUT'
  /** 最近运行时间 */
  lastRunTime?: string
}

export interface SubscriptionConfig {
  sourceType: 'ALARM' | 'SENSOR_DATA'
  deviceIds?: number[]
  alarmLevels?: number[]
  sensorCodes?: string[]
}

export interface CompositeAlarmLog {
  id: number
  alarmId: number
  triggerTime: string
  triggerMode: 'PERIODIC' | 'REALTIME'
  durationMs: number
  status: 'SUCCESS' | 'ERROR' | 'TIMEOUT'
  output?: string
  errorMsg?: string
}

export interface CompositeAlarmScope {
  id: number
  alarmId: number
  hazardPointId: number
  hazardPointName?: string
}

export interface CompositeAlarmPageParams {
  pageNum: number
  pageSize: number
  name?: string
  status?: '' | 'ENABLED' | 'DISABLED'
  triggerMode?: '' | 'PERIODIC' | 'REALTIME'
}

export interface HazardPointOption {
  id: number
  name: string
  parentId?: number
  children?: HazardPointOption[]
}

// ==================== 模拟数据 ====================

const mockAlarms: CompositeAlarmItem[] = [
  {
    id: 1,
    name: '暴雨滑坡综合预警',
    description: '结合降雨量、位移、水位等多参数进行滑坡综合风险评估，当降雨量超过阈值且位移加速时触发高级别预警。',
    triggerMode: 'REALTIME',
    subscriptionConfig: { sourceType: 'SENSOR_DATA', deviceIds: [1, 2, 3], sensorCodes: ['rainfall', 'displacement', 'water_level'] },
    scriptXml: '<xml><block type="subscription_trigger">...</block></xml>',
    scriptCode: `def run(TriggerMessage msg) {
    def deviceId = msg.payload.deviceId
    def rainfall = queryLatest(deviceId, 'rainfall')
    def displacement = queryLatest(deviceId, 'displacement')
    def waterLevel = queryLatest(deviceId, 'water_level')

    def score = 0
    if (rainfall.value > 50) score += 3
    if (rainfall.value > 30) score += 2
    if (displacement.value > 10) score += 3
    if (displacement.value > 5) score += 1
    if (waterLevel.value > 3) score += 2

    if (score >= 6) {
        return [level: 3, message: '暴雨滑坡高风险预警', detail: "降雨:\${rainfall.value}mm 位移:\${displacement.value}mm 水位:\${waterLevel.value}m"]
    } else if (score >= 4) {
        return [level: 2, message: '暴雨滑坡风险预警', detail: "降雨:\${rainfall.value}mm 位移:\${displacement.value}mm"]
    }
}`,
    silenceSeconds: 3600,
    sustainSeconds: 600,
    levelChangeNotify: true,
    status: 'ENABLED',
    createTime: '2026-05-15 10:30:00',
    scopeCount: 3,
    lastRunStatus: 'SUCCESS',
    lastRunTime: '2026-06-03 14:25:00'
  },
  {
    id: 2,
    name: '日变形趋势分析',
    description: '每日凌晨定时分析各监测点的变形趋势，通过位移速率变化率判断是否存在加速变形风险。',
    triggerMode: 'PERIODIC',
    cronExpression: '0 0 2 * * ?',
    scriptXml: '<xml><block type="periodic_run">...</block></xml>',
    scriptCode: `def run() {
    def points = getScopes()
    for (point in points) {
        def devices = queryDevices(point.id)
        for (device in devices) {
            def history = queryHistory(device.id, 'displacement', now() - 7, now())
            def velocity = calcVelocity(history)
            def acceleration = calcAcceleration(history)

            if (acceleration > 0.5) {
                return [level: 3, message: '变形加速预警', detail: "点位:\${point.name} 设备:\${device.name} 加速度:\${acceleration}"]
            } else if (velocity > 2.0) {
                return [level: 2, message: '变形速率预警', detail: "点位:\${point.name} 设备:\${device.name} 速率:\${velocity}"]
            }
        }
    }
}`,
    silenceSeconds: 86400,
    sustainSeconds: 0,
    levelChangeNotify: false,
    status: 'ENABLED',
    createTime: '2026-05-20 09:00:00',
    scopeCount: 5,
    lastRunStatus: 'SUCCESS',
    lastRunTime: '2026-06-03 02:00:00'
  },
  {
    id: 3,
    name: '多级联动告警升级',
    description: '当低级告警持续触发超过设定时长后自动升级告警级别，并联动周边监测点数据进行综合判断。',
    triggerMode: 'REALTIME',
    subscriptionConfig: { sourceType: 'ALARM', alarmLevels: [1, 2] },
    scriptXml: '<xml><block type="alarm_subscription">...</block></xml>',
    scriptCode: `def run(TriggerMessage msg) {
    def alarm = msg.payload
    def lastUpgrade = getData('last_upgrade_' + alarm.sourceId)
    if (lastUpgrade && (now() - lastUpgrade) < silenceSeconds * 1000) return null

    def nearbyAlarms = queryAlarms(alarm.level + 1, alarm.pointId, now() - 3600000)
    if (nearbyAlarms.size() >= 3) {
        storeData('last_upgrade_' + alarm.sourceId, now(), 3600)
        return [level: alarm.level + 1, message: '多点位联动告警升级', detail: "原始:\${alarm.level}级 升级至:\${alarm.level + 1}级 关联告警数:\${nearbyAlarms.size()}"]
    }
}`,
    silenceSeconds: 1800,
    sustainSeconds: 600,
    levelChangeNotify: true,
    status: 'DISABLED',
    createTime: '2026-06-01 15:00:00',
    scopeCount: 8,
    lastRunStatus: 'ERROR',
    lastRunTime: '2026-06-02 08:30:00'
  },
  {
    id: 4,
    name: '气象联动风险评估',
    description: '结合气象预报数据（降雨、温度）与实时监测数据，预测未来24小时风险等级变化趋势。',
    triggerMode: 'PERIODIC',
    cronExpression: '0 0 */6 * * ?',
    scriptXml: '<xml><block type="weather_check">...</block></xml>',
    scriptCode: `def run() {
    def points = getScopes()
    for (point in points) {
        def weather = queryWeather(point.id)
        if (weather.rainfallForecast > 100) {
            def soilMoisture = queryLatest(point.id, 'soil_moisture')
            if (soilMoisture.value > 80) {
                return [level: 3, message: '气象联动高风险', detail: "预报降雨:\${weather.rainfallForecast}mm 土壤湿度:\${soilMoisture.value}%"]
            }
        }
    }
}`,
    silenceSeconds: 21600,
    sustainSeconds: 0,
    levelChangeNotify: true,
    status: 'ENABLED',
    createTime: '2026-06-02 11:00:00',
    scopeCount: 2,
    lastRunStatus: 'SUCCESS',
    lastRunTime: '2026-06-03 06:00:00'
  }
]

const mockLogs: CompositeAlarmLog[] = [
  { id: 1, alarmId: 1, triggerTime: '2026-06-03 14:25:00', triggerMode: 'REALTIME', durationMs: 125, status: 'SUCCESS', output: 'null (无告警)' },
  { id: 2, alarmId: 1, triggerTime: '2026-06-03 13:50:00', triggerMode: 'REALTIME', durationMs: 230, status: 'SUCCESS', output: 'null (无告警)' },
  { id: 3, alarmId: 1, triggerTime: '2026-06-03 13:10:00', triggerMode: 'REALTIME', durationMs: 189, status: 'SUCCESS', output: '{"level":2,"message":"暴雨滑坡风险预警","detail":"降雨:35.2mm 位移:6.1mm"}' },
  { id: 4, alarmId: 1, triggerTime: '2026-06-03 12:30:00', triggerMode: 'REALTIME', durationMs: 3100, status: 'TIMEOUT', output: '', errorMsg: '脚本执行超时(30s)' },
  { id: 5, alarmId: 2, triggerTime: '2026-06-03 02:00:00', triggerMode: 'PERIODIC', durationMs: 1520, status: 'SUCCESS', output: 'null (无告警)' },
  { id: 6, alarmId: 2, triggerTime: '2026-06-02 02:00:00', triggerMode: 'PERIODIC', durationMs: 980, status: 'SUCCESS', output: 'null (无告警)' },
  { id: 7, alarmId: 3, triggerTime: '2026-06-02 08:30:00', triggerMode: 'REALTIME', durationMs: 45, status: 'ERROR', output: '', errorMsg: 'NullPointerException: Cannot invoke method value on null object' },
  { id: 8, alarmId: 4, triggerTime: '2026-06-03 06:00:00', triggerMode: 'PERIODIC', durationMs: 2100, status: 'SUCCESS', output: 'null (无告警)' }
]

const mockScopes: Record<number, CompositeAlarmScope[]> = {
  1: [
    { id: 1, alarmId: 1, hazardPointId: 101, hazardPointName: 'K15+200 右侧边坡' },
    { id: 2, alarmId: 1, hazardPointId: 102, hazardPointName: 'K23+100 左侧边坡' },
    { id: 3, alarmId: 1, hazardPointId: 103, hazardPointName: 'K31+050 右侧边坡' }
  ],
  2: [
    { id: 4, alarmId: 2, hazardPointId: 101, hazardPointName: 'K15+200 右侧边坡' },
    { id: 5, alarmId: 2, hazardPointId: 104, hazardPointName: 'K42+300 左侧边坡' },
    { id: 6, alarmId: 2, hazardPointId: 105, hazardPointName: 'K55+800 右侧边坡' },
    { id: 7, alarmId: 2, hazardPointId: 106, hazardPointName: 'K68+120 左侧边坡' },
    { id: 8, alarmId: 2, hazardPointId: 107, hazardPointName: 'K72+400 右侧边坡' }
  ],
  3: [
    { id: 9, alarmId: 3, hazardPointId: 101, hazardPointName: 'K15+200 右侧边坡' },
    { id: 10, alarmId: 3, hazardPointId: 102, hazardPointName: 'K23+100 左侧边坡' },
    { id: 11, alarmId: 3, hazardPointId: 104, hazardPointName: 'K42+300 左侧边坡' },
    { id: 12, alarmId: 3, hazardPointId: 108, hazardPointName: 'K80+050 右侧边坡' }
  ],
  4: [
    { id: 13, alarmId: 4, hazardPointId: 101, hazardPointName: 'K15+200 右侧边坡' },
    { id: 14, alarmId: 4, hazardPointId: 109, hazardPointName: 'K90+600 左侧边坡' }
  ]
}

const mockHazardPoints: HazardPointOption[] = [
  { id: 101, name: 'K15+200 右侧边坡' },
  { id: 102, name: 'K23+100 左侧边坡' },
  { id: 103, name: 'K31+050 右侧边坡' },
  { id: 104, name: 'K42+300 左侧边坡' },
  { id: 105, name: 'K55+800 右侧边坡' },
  { id: 106, name: 'K68+120 左侧边坡' },
  { id: 107, name: 'K72+400 右侧边坡' },
  { id: 108, name: 'K80+050 右侧边坡' },
  { id: 109, name: 'K90+600 左侧边坡' },
  { id: 110, name: 'K95+150 右侧边坡' },
  { id: 111, name: 'K102+300 左侧边坡' },
  { id: 112, name: 'K110+800 右侧边坡' }
]

// ==================== 模拟延迟工具 ====================

const delay = (ms: number) => new Promise(resolve => setTimeout(resolve, ms))

// ==================== API 函数 ====================

export const getCompositeAlarmPage = async (params: CompositeAlarmPageParams): Promise<PageResult<CompositeAlarmItem>> => {
  await delay(300)
  let filtered = [...mockAlarms]
  if (params.name) {
    const keyword = params.name.toLowerCase()
    filtered = filtered.filter(item => item.name.toLowerCase().includes(keyword) || item.description.toLowerCase().includes(keyword))
  }
  if (params.status) {
    filtered = filtered.filter(item => item.status === params.status)
  }
  if (params.triggerMode) {
    filtered = filtered.filter(item => item.triggerMode === params.triggerMode)
  }
  const start = (params.pageNum - 1) * params.pageSize
  const rows = filtered.slice(start, start + params.pageSize)
  return { rows, total: filtered.length, pageNum: params.pageNum, pageSize: params.pageSize }
}

export const getCompositeAlarmDetail = async (id: number): Promise<CompositeAlarmItem> => {
  await delay(200)
  const item = mockAlarms.find(a => a.id === id)
  if (!item) throw new Error('未找到该策略')
  return { ...item }
}

export const createCompositeAlarm = async (payload: Partial<CompositeAlarmItem>): Promise<CompositeAlarmItem> => {
  await delay(400)
  const newItem: CompositeAlarmItem = {
    id: Date.now(),
    name: payload.name || '',
    description: payload.description || '',
    triggerMode: payload.triggerMode || 'PERIODIC',
    cronExpression: payload.cronExpression,
    subscriptionConfig: payload.subscriptionConfig,
    scriptXml: payload.scriptXml,
    scriptCode: payload.scriptCode,
    silenceSeconds: payload.silenceSeconds || 0,
    sustainSeconds: payload.sustainSeconds || 0,
    levelChangeNotify: payload.levelChangeNotify || false,
    status: 'DISABLED',
    createTime: new Date().toISOString().replace('T', ' ').substring(0, 19),
    scopeCount: 0
  }
  mockAlarms.push(newItem)
  return newItem
}

export const updateCompositeAlarm = async (id: number, payload: Partial<CompositeAlarmItem>): Promise<CompositeAlarmItem> => {
  await delay(300)
  const idx = mockAlarms.findIndex(a => a.id === id)
  if (idx === -1) throw new Error('未找到该策略')
  Object.assign(mockAlarms[idx], payload, { updateTime: new Date().toISOString().replace('T', ' ').substring(0, 19) })
  return { ...mockAlarms[idx] }
}

export const deleteCompositeAlarm = async (id: number): Promise<void> => {
  await delay(300)
  const idx = mockAlarms.findIndex(a => a.id === id)
  if (idx === -1) throw new Error('未找到该策略')
  if (mockAlarms[idx].status === 'ENABLED') throw new Error('请先停用策略再删除')
  mockAlarms.splice(idx, 1)
}

export const changeCompositeAlarmStatus = async (id: number, status: 'ENABLED' | 'DISABLED'): Promise<void> => {
  await delay(300)
  const item = mockAlarms.find(a => a.id === id)
  if (!item) throw new Error('未找到该策略')
  if (status === 'ENABLED' && !item.scriptCode) throw new Error('请先编辑脚本再启用')
  item.status = status
}

export const testCompositeAlarm = async (id: number): Promise<CompositeAlarmLog> => {
  await delay(2000)
  const log: CompositeAlarmLog = {
    id: Date.now(),
    alarmId: id,
    triggerTime: new Date().toISOString().replace('T', ' ').substring(0, 19),
    triggerMode: 'PERIODIC',
    durationMs: Math.floor(Math.random() * 500) + 50,
    status: 'SUCCESS',
    output: 'null (无告警) — 测试运行'
  }
  return log
}

export const getCompositeAlarmLogs = async (alarmId: number, params: { pageNum: number; pageSize: number }): Promise<PageResult<CompositeAlarmLog>> => {
  await delay(300)
  const logs = mockLogs.filter(l => l.alarmId === alarmId)
  const start = (params.pageNum - 1) * params.pageSize
  return { rows: logs.slice(start, start + params.pageSize), total: logs.length, pageNum: params.pageNum, pageSize: params.pageSize }
}

export const getCompositeAlarmScopes = async (alarmId: number): Promise<CompositeAlarmScope[]> => {
  await delay(200)
  return mockScopes[alarmId] || []
}

export const updateCompositeAlarmScopes = async (alarmId: number, hazardPointIds: number[]): Promise<void> => {
  await delay(300)
  mockScopes[alarmId] = hazardPointIds.map((hpid, idx) => ({
    id: Date.now() + idx,
    alarmId,
    hazardPointId: hpid,
    hazardPointName: mockHazardPoints.find(hp => hp.id === hpid)?.name || `隐患点${hpid}`
  }))
  const alarm = mockAlarms.find(a => a.id === alarmId)
  if (alarm) alarm.scopeCount = hazardPointIds.length
}

export const getHazardPointOptions = async (): Promise<HazardPointOption[]> => {
  await delay(200)
  return [...mockHazardPoints]
}

export const updateScriptCode = async (id: number, scriptCode: string, scriptXml?: string): Promise<void> => {
  await delay(300)
  const item = mockAlarms.find(a => a.id === id)
  if (!item) throw new Error('未找到该策略')
  item.scriptCode = scriptCode
  if (scriptXml) item.scriptXml = scriptXml
}
