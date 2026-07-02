// src/utils/monitor.ts
import http from '@/utils/api'

export interface ChartQuery {
  hazardPointId: number
  deviceId?: number
  sensorId?: number
  attrCode?: string
  valueType?: 'current' | 'hour' | '24h' | '72h'
  startTime: string
  endTime: string
}

export interface ChartSeries {
  seriesName: string
  deviceName: string
  sensorName: string
  labels: string[]
  values: number[]
  unit: string
  attrName: string
  maxValue?: number
  minValue?: number
  avgValue?: number
}

export interface LatestMonitorData {
  hazardPointId: number
  hazardPointName: string
  deviceId: number
  deviceName: string
  sensorId: number
  sensorName: string
  attrCode: string
  attrName: string
  value: number | null
  unit: string
  dataTime: string
  quality: number
  qualityText: string
}

export interface SensorLatestRow {
  time: number
  value: number | null
  quality: number
}

export const monitorApi = {
  async getLatest(hazardPointId: number): Promise<LatestMonitorData[]> {
    try {
      const res = await http.get('/monitor-data/latest', { hazardPointId })
      const list = (res as any)?.rows || (res as any[]) || []
      return list
    } catch (error) {
      console.error('获取最新监测数据失败:', error)
      return []
    }
  },

  /** 按传感器获取各属性最新一条数据，返回 Map<attrCode, SensorLatestRow> */
  async getSensorLatest(deviceId: number, sensorCode: string): Promise<Record<string, SensorLatestRow>> {
    try {
      const res = await http.get('/monitor-data/sensor/latest', { deviceId, sensorCode })
      return (res as any) || {}
    } catch (error) {
      console.error('获取传感器最新数据失败:', error)
      return {}
    }
  },

  async getChart(query: ChartQuery): Promise<ChartSeries[]> {
    try {
      const res = await http.get('/monitor-data/chart', {
        hazardPointId: query.hazardPointId,
        deviceId: query.deviceId,
        sensorId: query.sensorId,
        attrCode: query.attrCode,
        valueType: query.valueType || 'current',
        startTime: query.startTime,
        endTime: query.endTime
      })
      const list = (res as any)?.rows || (res as any[]) || []
      return list
    } catch (error) {
      console.error('获取图表数据失败:', error)
      return []
    }
  }
}

export default monitorApi
