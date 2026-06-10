import request from '@/utils/request'
import type {AjaxResult, PageResult} from './system'

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
  categoryId?: number
  categoryName?: string
  name: string
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
  categoryId?: number
  status?: number
}

export interface MonitorTypeCreatePayload {
  code: string
  categoryId: number
  name: string
  icon?: string
  description?: string
  sortOrder?: number
  status?: number
}

export interface MonitorTypeUpdatePayload {
  name: string
  categoryId?: number
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
    if (response && typeof response.code === 'number' && response.code !== 200) {
        throw new Error(response.msg || '操作失败')
    }
  return response.data
}

export const getMonitorTypePage = (params: MonitorTypeQueryParams) =>
  unwrap<PageResult<MonitorTypeItem>>(request.get('/monitor-types/page', { params }))

export const getMonitorTypeDetail = (id: number) =>
  unwrap<MonitorTypeItem>(request.get(`/monitor-types/${id}`))

export const getMonitorTypeList = () =>
  unwrap<MonitorTypeItem[]>(request.get('/monitor-types'))

/** 批量获取所有监测类型及其内容（单次请求，避免 N+1） */
export const getMonitorTypeListWithContents = () =>
    unwrap<MonitorTypeItem[]>(request.get('/monitor-types/with-contents'))

export const createMonitorType = (payload: MonitorTypeCreatePayload) =>
    unwrap<{ id: number }>(request.post('/monitor-types', payload))

export const updateMonitorType = (id: number, payload: MonitorTypeUpdatePayload) =>
  unwrap<null>(request.put(`/monitor-types/${id}`, payload))

export const removeMonitorType = (id: number) =>
  unwrap<null>(request.delete(`/monitor-types/${id}`))

export const createMonitorContent = (payload: MonitorContentCreatePayload) =>
    unwrap<{ id: number }>(request.post('/monitor-contents', payload))

export const updateMonitorContent = (id: number, payload: MonitorContentUpdatePayload) =>
  unwrap<null>(request.put(`/monitor-contents/${id}`, payload))

export const removeMonitorContent = (id: number) =>
  unwrap<null>(request.delete(`/monitor-contents/${id}`))
