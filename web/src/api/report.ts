import request from '@/utils/request'
import type { PageResult } from './system'
import { getHazardPointPage } from './hazardPoint'
import { getSensorRange } from './monitorData'

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

// ====== 分析查询 API (真实接口) ======

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
  sensorCode: string
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

export interface GridChartItem {
  index: number
  sensorSeriesId?: string
  title?: string
  hazardPointId?: number
  deviceId?: number
  sensorId?: number
  sensorName?: string
  sensorCode?: string
  attrCode?: string
  attrName?: string
  unit?: string
}

/** Parse data from real sensor/range API into ChartDataItem */
async function fetchRealChartData(
  deviceId: number,
  sensorCode: string,
  attrCode: string,
  startTime: string,
  endTime: string,
): Promise<ChartDataItem | null> {
  try {
    const dataMap: Record<string, { dataTime: string; value: number }[]> = await getSensorRange({
      deviceId,
      sensorCode,
      attrCode,
      startTime,
      endTime,
    }) as any

    const rows = dataMap[attrCode] || Object.values(dataMap)[0]
    if (!rows || rows.length === 0) return null

    const sorted = [...rows].reverse()
    return {
      times: sorted.map((r: any) => r.dataTime ?? r.time ?? ''),
      values: sorted.map((r: any) => r.value),
    }
  } catch {
    return null
  }
}

// ---------------------------------------------------------------------------
// API functions
// ---------------------------------------------------------------------------

/** Fetch device options filtered by hazardPointId */
export async function getDeviceOptions(params: {
  hazardPointId?: number
  deviceType?: number
}): Promise<DeviceOption[]> {
  const queryParts: string[] = [`pageNum=1`, `pageSize=100`]
  if (params.hazardPointId) queryParts.push(`boundHazardPointId=${params.hazardPointId}`)
  const res = await request.get<any>(`/devices/page?${queryParts.join('&')}`)
  return (res.data?.rows ?? res.rows ?? []).map((d: any) => ({
    id: d.id,
    name: d.name,
    deviceType: d.deviceType,
    boundHazardPointId: d.boundHazardPointId,
  }))
}

/** Fetch chart data for a single device+attribute (real API, sensor-level) */
export async function getChartData(params: {
  deviceId: number
  sensorCode: string
  attrCode: string
  startTime: string
  endTime: string
}): Promise<ChartDataItem | null> {
  return fetchRealChartData(params.deviceId, params.sensorCode, params.attrCode, params.startTime, params.endTime)
}

/** Fetch chart data for multiple grid items (real API) */
export async function getGridChartData(
  items: GridChartItem[],
  startTime: string,
  endTime: string
): Promise<Map<number, ChartDataItem | null>> {
  const result = new Map<number, ChartDataItem | null>()
  for (const item of items) {
    if (item.deviceId && item.attrCode && item.sensorCode) {
      result.set(item.index, await fetchRealChartData(item.deviceId, item.sensorCode, item.attrCode, startTime, endTime))
    }
  }
  return result
}
