import request from '@/utils/request'
import type { PageResult } from './system'
import { getHazardPointPage } from './hazardPoint'

// ====== 报告管理 API (真实接口) ======

export type ReportType = 'weekly' | 'monthly' | 'quarterly'

export interface ReportItem {
  id: number
  type: ReportType
  typeDesc: string
  periodStart: string
  periodEnd: string
  hazardPointId: number
  hazardPointCode: string
  hazardPointName: string
  reportName: string
  status: 1 | 2 | 3
  statusDesc: string
  errorMsg: string | null
  createTime: string
  content?: string
}

export interface ReportPageParams {
  pageNum: number
  pageSize: number
  type?: ReportType | ''
  hazardPointId?: number
  periodStart?: string
  periodEnd?: string
  status?: number
  keyword?: string
}

export interface ReportGenerateParams {
  type: ReportType
  hazardPointId: number
  periodStart: string
  periodEnd: string
}

export interface HazardPointOption {
  id: number
  name: string
}

// 后端类型码 -> 前端字符串
const TYPE_CODE_TO_STR: Record<number, ReportType> = { 2: 'weekly', 3: 'monthly', 4: 'quarterly' }
const TYPE_STR_TO_CODE: Record<ReportType, number> = { weekly: 2, monthly: 3, quarterly: 4 }

function mapRecord(raw: any): ReportItem {
  return {
    id: raw.id,
    type: TYPE_CODE_TO_STR[raw.type] ?? 'weekly',
    typeDesc: raw.typeDesc ?? '',
    periodStart: raw.periodStart,
    periodEnd: raw.periodEnd,
    hazardPointId: raw.hazardPointId,
    hazardPointCode: raw.hazardPointCode ?? '',
    hazardPointName: raw.hazardPointName ?? '',
    reportName: raw.reportName ?? '',
    status: raw.status,
    statusDesc: raw.statusDesc ?? '',
    errorMsg: raw.errorMsg,
    createTime: raw.createTime,
    content: raw.content,
  }
}

export async function getReportPage(params: ReportPageParams): Promise<PageResult<ReportItem>> {
  const payload: any = { ...params }
  if (params.type) payload.type = TYPE_STR_TO_CODE[params.type]
  const res: any = await request.get('/report/records/page', { params: payload })
  const data = res.data ?? res
  return {
    rows: (data.rows ?? []).map(mapRecord),
    total: data.total ?? 0,
    pageNum: data.pageNum ?? params.pageNum,
    pageSize: data.pageSize ?? params.pageSize,
  }
}

export async function getReportDetail(id: number): Promise<ReportItem> {
  const res: any = await request.get(`/report/records/${id}`)
  return mapRecord(res.data ?? res)
}

export async function deleteReport(id: number): Promise<void> {
  await request.delete(`/report/records/${id}`)
}

export async function generateReport(data: ReportGenerateParams): Promise<{ reportId: number; existed: boolean }> {
  const payload = {
    type: TYPE_STR_TO_CODE[data.type],
    hazardPointId: data.hazardPointId,
    periodStart: data.periodStart,
    periodEnd: data.periodEnd,
  }
  try {
    const res: any = await request.post('/report/records/generate', payload)
    return { reportId: res.reportId ?? -1, existed: false }
  } catch (err: any) {
    const respData = err?.response?.data
    if (respData?.code === 409) {
      return { reportId: respData.reportId ?? -1, existed: true }
    }
    throw err
  }
}

/** 批量一键生成: 对全部监测中隐患点调用 generateAll */
export async function generateAllReports(
  type: ReportType,
  referenceDate?: string
): Promise<void> {
  const payload: any = { type: TYPE_STR_TO_CODE[type] }
  if (referenceDate) payload.referenceDate = referenceDate
  await request.post('/report/records/generate-all', payload)
}

// 隐患点选项 (调用真实 API)
export async function getHazardPointOptions(): Promise<HazardPointOption[]> {
  try {
    const res = await getHazardPointPage({ pageNum: 1, pageSize: 500 })
    return ((res as any).data?.rows ?? (res as any).rows ?? []).map((hp: any) => ({
      id: hp.id,
      name: hp.name ?? hp.hazardPointName ?? '',
    }))
  } catch {
    return []
  }
}

// ====== Query / Analysis (保留原 mock, 不在本次改动范围) ======

// --- Query ---
export interface MonitorQueryParams {
  hazardPointId?: number | ''
  deviceType?: number | ''
  deviceId?: number | ''
  attrCodes?: string[]
  startTime?: string
  endTime?: string
  pageNum: number
  pageSize: number
}

// --- Analysis ---
export interface SensorSeriesItem {
  id: string
  hazardPointId: number
  hazardPointName: string
  deviceId: number
  deviceName: string
  sensorId: number
  sensorName: string
  attrCode: string
  attrName: string
  unit: string
  color: string
}

export interface ChartDataItem {
  times: string[]
  values: number[]
}

// --- Options for dropdowns ---
export interface DeviceOption {
  id: number
  name: string
  deviceType: number
  boundHazardPointId: number
}

export interface DeviceTypeOption {
  value: number
  label: string
  attrs: { code: string; name: string; unit: string }[]
}

export interface GridChartItem {
  index: number
  sensorSeriesId?: string
  title?: string
  hazardPointId?: number
  deviceId?: number
  sensorId?: number
  sensorName?: string
  attrCode?: string
  attrName?: string
  unit?: string
}

// ---------------------------------------------------------------------------
// Deterministic pseudo-random helpers (seeded, no Math.random)
// ---------------------------------------------------------------------------

/** Simple seeded PRNG — returns a float in [0, 1) */
function seededRandom(seed: number): () => number {
  let s = seed
  return () => {
    s = (s * 16807 + 0) % 2147483647
    return (s - 1) / 2147483646
  }
}

/** Return a deterministic float in [min, max) using the given rng */
function randRange(rng: () => number, min: number, max: number): number {
  return min + rng() * (max - min)
}

/** Round to fixed decimals */
function toFixed(value: number, decimals: number): number {
  const factor = Math.pow(10, decimals)
  return Math.round(value * factor) / factor
}

// ---------------------------------------------------------------------------
// Mock data generators
// ---------------------------------------------------------------------------

const HAZARD_POINTS: HazardPointOption[] = [
  { id: 1, name: '王家坪滑坡' },
  { id: 2, name: '李家沟泥石流' },
  { id: 3, name: '赵家坡危岩体' },
  { id: 4, name: '张家湾崩塌' },
  { id: 5, name: '刘家坳滑坡' },
]

const DEVICE_TYPES: DeviceTypeOption[] = [
  {
    value: 1,
    label: '位移计',
    attrs: [
      { code: 'disp_x', name: 'X方向位移', unit: 'mm' },
      { code: 'disp_y', name: 'Y方向位移', unit: 'mm' },
      { code: 'disp_z', name: 'Z方向位移', unit: 'mm' },
      { code: 'disp_result', name: '成果值', unit: 'mm' },
    ],
  },
  {
    value: 2,
    label: '雨量计',
    attrs: [
      { code: 'rainfall', name: '降雨量', unit: 'mm' },
      { code: 'rainfall_daily', name: '日累计雨量', unit: 'mm' },
    ],
  },
  {
    value: 3,
    label: '倾角传感器',
    attrs: [
      { code: 'tilt_x', name: 'X倾角', unit: '°' },
      { code: 'tilt_y', name: 'Y倾角', unit: '°' },
    ],
  },
  {
    value: 4,
    label: '土压力计',
    attrs: [
      { code: 'earth_pressure', name: '土压力', unit: 'kPa' },
    ],
  },
]

// 12 devices spread across hazard points (3-4 per hazard point)
const DEVICES: DeviceOption[] = [
  { id: 101, name: 'WJP-WY-01', deviceType: 1, boundHazardPointId: 1 },
  { id: 102, name: 'WJP-YL-01', deviceType: 2, boundHazardPointId: 1 },
  { id: 103, name: 'WJP-QJ-01', deviceType: 3, boundHazardPointId: 1 },
  { id: 201, name: 'LJG-YL-01', deviceType: 2, boundHazardPointId: 2 },
  { id: 202, name: 'LJG-WY-01', deviceType: 1, boundHazardPointId: 2 },
  { id: 203, name: 'LJG-QJ-01', deviceType: 3, boundHazardPointId: 2 },
  { id: 204, name: 'LJG-TY-01', deviceType: 4, boundHazardPointId: 2 },
  { id: 301, name: 'ZJP-WY-01', deviceType: 1, boundHazardPointId: 3 },
  { id: 302, name: 'ZJP-QJ-01', deviceType: 3, boundHazardPointId: 3 },
  { id: 303, name: 'ZJP-TY-01', deviceType: 4, boundHazardPointId: 3 },
  { id: 401, name: 'ZJW-WY-01', deviceType: 1, boundHazardPointId: 4 },
  { id: 402, name: 'ZJW-YL-01', deviceType: 2, boundHazardPointId: 4 },
  { id: 403, name: 'ZJW-TY-01', deviceType: 4, boundHazardPointId: 4 },
  { id: 501, name: 'LJA-WY-01', deviceType: 1, boundHazardPointId: 5 },
  { id: 502, name: 'LJA-YL-01', deviceType: 2, boundHazardPointId: 5 },
]

/** Format a Date to YYYY-MM-DD */
function formatDate(d: Date): string {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

/** Format a Date to YYYY-MM-DD HH:mm:ss */
function formatDateTime(d: Date): string {
  const date = formatDate(d)
  const h = String(d.getHours()).padStart(2, '0')
  const min = String(d.getMinutes()).padStart(2, '0')
  const s = String(d.getSeconds()).padStart(2, '0')
  return `${date} ${h}:${min}:${s}`
}

/** Generate mock query data (time-series rows) */
function getMockQueryData(params: MonitorQueryParams): PageResult<Record<string, any>> {
  const { deviceType = 1, pageNum = 1, pageSize = 20 } = params
  const rng = seededRandom(Number(deviceType) * 100 + pageNum * 7 + pageSize)

  const devType = DEVICE_TYPES.find((t) => t.value === Number(deviceType)) || DEVICE_TYPES[0]
  const filteredDevices = DEVICES.filter((d) => d.deviceType === Number(deviceType))
  if (filteredDevices.length === 0) return { rows: [], total: 0, pageNum, pageSize }

  const totalRows = 60
  const rows: Record<string, any>[] = []

  const startIdx = (pageNum - 1) * pageSize
  const endIdx = Math.min(startIdx + pageSize, totalRows)

  for (let i = startIdx; i < endIdx; i++) {
    const device = filteredDevices[i % filteredDevices.length]
    const timeOffset = (totalRows - i) * 2 // hours ago
    const time = new Date()
    time.setHours(time.getHours() - timeOffset)

    const row: Record<string, any> = {
      time: formatDateTime(time),
      deviceName: device.name,
      hazardPointName: HAZARD_POINTS.find((h) => h.id === device.boundHazardPointId)?.name || '',
    }

    for (const attr of devType.attrs) {
      let val: number
      switch (attr.code) {
        case 'disp_x':
        case 'disp_y':
        case 'disp_z':
          val = toFixed(randRange(rng, 0.1, 5.0), 2)
          break
        case 'rainfall':
          val = toFixed(randRange(rng, 0, 50), 1)
          break
        case 'rainfall_daily':
          val = toFixed(randRange(rng, 0, 80), 1)
          break
        case 'tilt_x':
        case 'tilt_y':
          val = toFixed(randRange(rng, -2.0, 2.0), 3)
          break
        case 'earth_pressure':
          val = toFixed(randRange(rng, 10, 120), 2)
          break
        default:
          val = toFixed(randRange(rng, 0, 100), 2)
      }
      row[attr.code] = val
    }

    rows.push(row)
  }

  return { rows, total: totalRows, pageNum, pageSize }
}

/** Generate mock chart data (100 points over 7 days) */
function getMockChartData(
  deviceId: number,
  attrCode: string,
  startTime: string,
  _endTime: string
): ChartDataItem {
  const rng = seededRandom(deviceId * 31 + attrCode.charCodeAt(0) * 17 + startTime.charCodeAt(5) * 3)

  // Parse startTime safely — replace space with 'T' for ISO 8601 compatibility
  const start = new Date(startTime.replace(' ', 'T'))
  const times: string[] = []
  const values: number[] = []

  // 100 points over 7 days
  const totalMinutes = 7 * 24 * 60
  const interval = totalMinutes / 100

  for (let i = 0; i < 100; i++) {
    const t = new Date(start.getTime() + i * interval * 60 * 1000)
    times.push(formatDateTime(t))

    let val: number
    switch (attrCode) {
      case 'disp_x':
      case 'disp_y':
      case 'disp_z':
        val = toFixed(randRange(rng, 0.1, 5.0), 2)
        break
      case 'rainfall':
        val = toFixed(randRange(rng, 0, 50), 1)
        break
      case 'rainfall_daily':
        val = toFixed(randRange(rng, 0, 80), 1)
        break
      case 'tilt_x':
      case 'tilt_y':
        val = toFixed(randRange(rng, -2.0, 2.0), 3)
        break
      case 'earth_pressure':
        val = toFixed(randRange(rng, 10, 120), 2)
        break
      default:
        val = toFixed(randRange(rng, 0, 100), 2)
    }
    values.push(val)
  }

  return { times, values }
}

// ---------------------------------------------------------------------------
// API functions (try real API, fall back to mock)
// ---------------------------------------------------------------------------

/** Fetch device type options with attributes (mock) */
export async function getDeviceTypeOptions(): Promise<DeviceTypeOption[]> {
  return DEVICE_TYPES
}

/** Fetch device options, optionally filtered (mock) */
export async function getDeviceOptions(params: {
  hazardPointId?: number
  deviceType?: number
}): Promise<DeviceOption[]> {
  // let filtered = [...DEVICES]
  const devices = await request.get<any>(`/devices/page?pageNum=1&pageSize=20&boundHazardPointId=${params.hazardPointId}`);
    let filtered = devices.data?.rows ?? [];
  // if (params.hazardPointId) {
  //   filtered = filtered.filter((d) => d.hazardPointId === params.hazardPointId)
  // }
  // if (params.deviceType) {
  //   filtered = filtered.filter((d) => d.deviceType === params.deviceType)
  // }



  return filtered
}

/** Fetch paginated monitor query data (mock) */
export async function getMonitorQueryData(
  params: MonitorQueryParams
): Promise<PageResult<Record<string, any>>> {
  return getMockQueryData(params)
}

/** Fetch chart data for a single device+attribute (mock) */
export async function getChartData(params: {
  deviceId: number
  attrCode: string
  startTime: string
  endTime: string
}): Promise<ChartDataItem> {
  return getMockChartData(params.deviceId, params.attrCode, params.startTime, params.endTime)
}

/** Fetch chart data for multiple grid items (mock) */
export async function getGridChartData(
  items: GridChartItem[],
  startTime: string,
  endTime: string
): Promise<Map<number, ChartDataItem>> {
  const result = new Map<number, ChartDataItem>()
  for (const item of items) {
    if (item.deviceId && item.attrCode) {
      result.set(item.index, getMockChartData(item.deviceId, item.attrCode, startTime, endTime))
    }
  }
  return result
}
