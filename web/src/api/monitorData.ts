import request from '@/utils/request'
import type {AjaxResult, PageResult} from './system'

export interface LatestDataItem {
    deviceId: number
    deviceName: string
    sensorId: number
    sensorNo: string
    sensorName: string
    attrCode: string
    attrName: string
    value: number
    unit: string
    dataTime: number
}

export interface ChartData {
    labels: string[]
    values: number[]
    unit: string
    attrName: string
    maxValue: number | null
    minValue: number | null
    avgValue: number | null
}

export interface MonitorDataPageItem {
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
}

export interface MonitorDataPageQuery {
    hazardPointId: number
    deviceId?: number
    sensorId?: number
    attrCode?: string
    startTime?: string
    endTime?: string
    pageNum?: number
    pageSize?: number
}

const unwrap = async <T>(promise: Promise<AjaxResult<T>>): Promise<T> => {
    const response = await promise
    return response.data
}

/** 查询隐患点下所有测点的最新监测值 */
export const getLatestData = (hazardPointId: number) =>
    unwrap<LatestDataItem[]>(request.get('/monitor-data/latest', {params: {hazardPointId}}))

/** 分页查询隐患点下的历史监测数据 */
export const getMonitorDataPage = (params: MonitorDataPageQuery) =>
    unwrap<PageResult<MonitorDataPageItem>>(request.get('/monitor-data/page', {params}))

/** 查询监测指标的图表（曲线）数据 */
export const getChartData = (params: {
    hazardPointId: number
    deviceId?: number
    sensorId?: number
    attrCode?: string
    startTime: string
    endTime: string
}) =>
    unwrap<ChartData>(request.get('/monitor-data/chart', {params}))
