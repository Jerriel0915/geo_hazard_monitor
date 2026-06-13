import request from '@/utils/request'
import type {AjaxResult} from './system'
import type {MonitorTypeItem} from './monitorType'

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
    if (response && typeof response.code === 'number' && response.code !== 200) {
        throw new Error(response.msg || '操作失败')
    }
  return response.data
}

export const getDeviceSensors = (deviceId: number) =>
  unwrap<SensorItem[]>(request.get(`/devices/${deviceId}/sensors`))

export const getSensorDetail = (id: number) =>
  unwrap<SensorItem>(request.get(`/sensors/${id}`))

export const createSensor = (deviceId: number, payload: SensorCreatePayload) =>
    unwrap<{ id: number }>(request.post(`/devices/${deviceId}/sensors`, payload))

export const updateSensor = (id: number, payload: SensorUpdatePayload) =>
  unwrap<null>(request.put(`/sensors/${id}`, payload))

export const deleteSensor = (id: number) =>
  unwrap<null>(request.delete(`/sensors/${id}`))

export const deleteSensorAttribute = (sensorId: number, attrId: number) =>
  unwrap<null>(request.delete(`/sensors/${sensorId}/attributes/${attrId}`))

/**
 * 预测指定设备下一个可用的传感器序号。
 * <p>
 * 用于前端在"新增传感器"表单中按规则 {@code {indicator_type(大写)}_{序号}} 预填 sensorCode 占位。
 * 序号 = 该设备下未删除传感器数 +1。
 */
export const getNextSensorCode = (deviceId: number) =>
    unwrap<{ nextNo: number }>(request.get('/sensors/next-code', {params: {deviceId}}))

export const getSensorMonitorTypes = async () => {
  const list = await unwrap<MonitorTypeItem[]>(request.get('/monitor-types'))
  return list
}
