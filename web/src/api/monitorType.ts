import request from '@/utils/request'
import type { AjaxResult, PageResult } from './system'

export interface MonitorContentItem {
  id?: number
  monitorTypeId?: number
  code: string
  name: string
  indicatorType: string
  unit: string
  icon?: string
  rangeMin?: number | null
  rangeMax?: number | null
}

export interface MonitorTypeItem {
  id: number
  code: string
  name: string
  deviceType: number | null
  deviceTypeName?: string
  icon: string
  description: string
  sortOrder: number
  status: number
  createTime: string
  contents?: MonitorContentItem[]
}

export interface MonitorTypeQueryParams {
  pageNum?: number
  pageSize?: number
  code?: string
  name?: string
  deviceType?: number
  status?: number
}

export interface MonitorTypeCreatePayload {
  code: string
  name: string
  deviceType: number | null
  icon?: string
  description?: string
  sortOrder?: number
  status?: number
}

export interface MonitorTypeUpdatePayload {
  name: string
  deviceType: number | null
  icon?: string
  description?: string
  sortOrder?: number
}

export interface MonitorContentCreatePayload {
  monitorTypeId: number
  code: string
  name: string
  indicatorType: string
  unit?: string
  icon?: string
  rangeMin?: number | null
  rangeMax?: number | null
}

export interface MonitorContentUpdatePayload {
  name?: string
  unit?: string
  icon?: string
  rangeMin?: number | null
  rangeMax?: number | null
}

const unwrap = async <T>(promise: Promise<AjaxResult<T>>): Promise<T> => {
  const response = await promise
  return response.data
}

export const getMonitorTypePage = (params: MonitorTypeQueryParams) =>
  unwrap<PageResult<MonitorTypeItem>>(request.get('/monitor-types/page', { params }))

export const getMonitorTypeDetail = (id: number) =>
  unwrap<MonitorTypeItem>(request.get(`/monitor-types/${id}`))

export const getMonitorTypeList = () =>
  unwrap<MonitorTypeItem[]>(request.get('/monitor-types'))

export const createMonitorType = async (payload: MonitorTypeCreatePayload) => {
  const response = await request.post<AjaxResult<{ id: number }>>('/monitor-types', payload)
  return response.data
}

export const updateMonitorType = (id: number, payload: MonitorTypeUpdatePayload) =>
  unwrap<null>(request.put(`/monitor-types/${id}`, payload))

export const removeMonitorType = (id: number) =>
  unwrap<null>(request.delete(`/monitor-types/${id}`))

export const createMonitorContent = async (payload: MonitorContentCreatePayload) => {
  const response = await request.post<AjaxResult<{ id: number }>>('/monitor-contents', payload)
  return response.data
}

export const updateMonitorContent = (id: number, payload: MonitorContentUpdatePayload) =>
  unwrap<null>(request.put(`/monitor-contents/${id}`, payload))

export const removeMonitorContent = (id: number) =>
  unwrap<null>(request.delete(`/monitor-contents/${id}`))
