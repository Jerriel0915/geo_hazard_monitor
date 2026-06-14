import request from '@/utils/request'
import type {AjaxResult, PageResult} from './system'

export interface LatestDataItem {
    hazardPointId: number
    hazardPointName: string
    deviceId: number
    deviceName: string
    sensorId: number
    sensorName: string
    attrCode: string
    attrName: string
    value: number
    unit: string
    dataTime: string
    quality: number
    qualityText: string
}

export interface ChartData {
    seriesName: string
    deviceName: string
    sensorName: string
    labels: string[]
    values: number[]
    unit: string
    attrName: string
    maxValue: number | null
    minValue: number | null
    avgValue: number | null
}

export interface MonitorDataPageItem {
    hazardPointId: number
    hazardPointName: string
    dataTime: string
    deviceId: number
    deviceName: string
    sensorId: number
    sensorName: string
    attrCode: string
    attrName: string
    value: number
    unit: string
    quality: number
    qualityText: string
}

export interface MonitorDataPageQuery {
    hazardPointId: number
    deviceId?: number
    sensorId?: number
    attrCode?: string
    valueType?: string
    startTime?: string
    endTime?: string
    pageNum?: number
    pageSize?: number
}

const unwrap = async <T>(promise: Promise<AjaxResult<T>>): Promise<T> => {
    const response = await promise
    if (response && typeof response.code === 'number' && response.code !== 200) {
        throw new Error(response.msg || '操作失败')
    }
    return response.data
}

/** 查询隐患点下所有测点的最新监测值 */
export const getLatestData = (hazardPointId: number) =>
    unwrap<LatestDataItem[]>(request.get('/monitor-data/latest', {params: {hazardPointId}}))

/** 分页查询隐患点下的历史监测数据 */
export const getMonitorDataPage = (params: MonitorDataPageQuery) =>
    unwrap<PageResult<MonitorDataPageItem>>(request.get('/monitor-data/page', {params}))

/** 查询监测指标的图表（曲线）数据（支持多测点多序列） */
export const getChartData = (params: {
    hazardPointId: number
    deviceId?: number
    sensorId?: number
    attrCode?: string
    valueType?: string
    startTime: string
    endTime: string
}) =>
    unwrap<ChartData[]>(request.get('/monitor-data/chart', {params}))

// ── 传感器维度接口（MonitorDataSensorController） ──

export interface ExpressionSpec {
  alias?: string
  function?: string
  left?: ExpressionSpec
  op?: '+' | '-' | '*' | '/'
  right?: ExpressionSpec
  value?: number
}

export interface SensorAggregateVO {
  intervals: { startTime: string; endTime: string }[]
  columns: { alias: string; label: string; unit: string }[]
  rows: Record<string, number | null>[]
}

export interface SensorCompletenessVO {
  expected: number
  actual: number
  rate: number
  gaps: { start: string; end: string }[]
}

export interface SensorTrendVO {
  slope: number
  intercept: number
  direction: 'up' | 'down' | 'stable'
  confidence: number
}

/** 传感器维度 — 最新值 */
export const getSensorLatest = (deviceId: number, sensorCode: string, attrCode?: string) =>
  unwrap<LatestDataItem[]>(request.get('/monitor-data/sensor/latest', {
    params: { deviceId, sensorCode, attrCode }
  }))

/** 传感器维度 — 区间数据（支持数值范围过滤） */
export const getSensorRange = (params: {
  deviceId: number; sensorCode: string; attrCode?: string
  startTime: string; endTime: string
  minValue?: number; maxValue?: number; limit?: number; offset?: number
}) =>
  unwrap(request.get('/monitor-data/sensor/range', { params }))

/** 传感器维度 — 多表达式聚合 */
export const getSensorAggregate = (
  params: {
    deviceId: number; sensorCode: string
    startTime: string; endTime: string; granularity?: string
    minValue?: number; maxValue?: number
  },
  expressions: ExpressionSpec[]
) =>
  unwrap<SensorAggregateVO>(
    request.post('/monitor-data/sensor/aggregate', expressions, { params })
  )

/** 传感器维度 — 数据完整度 */
export const getSensorCompleteness = (params: {
  deviceId: number; sensorCode: string; attrCode: string
  startTime: string; endTime: string; expectedIntervalMs?: number
}) =>
  unwrap<SensorCompletenessVO>(request.get('/monitor-data/sensor/completeness', { params }))

/** 传感器维度 — 趋势分析 */
export const getSensorTrend = (params: {
  deviceId: number; sensorCode: string; attrCode: string
  startTime: string; endTime: string
}) =>
  unwrap<SensorTrendVO>(request.get('/monitor-data/sensor/trend', { params }))
