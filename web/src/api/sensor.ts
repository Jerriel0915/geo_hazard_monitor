import request from '@/utils/request'
import type { AjaxResult } from './system'
import type { MonitorTypeItem } from './monitorType'

export interface SensorAttrItem {
  id?: number
  monitorContentId?: number
  attrCode: string
  attrName: string
  initialValue?: number | null
  unit?: string
  rangeMin?: number | null
  rangeMax?: number | null
  icon?: string
}

export interface SensorItem {
  id?: number
  sensorCode: string
  sensorName: string
  monitorTypeId: number
  monitorTypeCode?: string
  monitorTypeName?: string
  status: number
  attrList: SensorAttrItem[]
  createTime?: string
  updateTime?: string
}

export interface SensorCreatePayload {
  sensorCode: string
  sensorName: string
  monitorTypeId: number
  status: number
  attrList: SensorAttrItem[]
}

export interface SensorUpdatePayload {
  sensorName: string
  status: number
  attrList: SensorAttrItem[]
}

const unwrap = async <T>(promise: Promise<AjaxResult<T>>): Promise<T> => {
  const response = await promise
  return response.data
}

export const getDeviceSensors = (deviceId: number) =>
  unwrap<SensorItem[]>(request.get(`/devices/${deviceId}/sensors`))

export const getSensorDetail = (id: number) =>
  unwrap<SensorItem>(request.get(`/sensors/${id}`))

export const createSensor = async (deviceId: number, payload: SensorCreatePayload) => {
  const response = await request.post<AjaxResult<{ id: number }>>(`/devices/${deviceId}/sensors`, payload)
  return response.data
}

export const updateSensor = (id: number, payload: SensorUpdatePayload) =>
  unwrap<null>(request.put(`/sensors/${id}`, payload))

export const deleteSensor = (id: number) =>
  unwrap<null>(request.delete(`/sensors/${id}`))

export const getSensorMonitorTypes = async () => {
  const list = await unwrap<MonitorTypeItem[]>(request.get('/monitor-types'))
  return list
}
