import request from '@/utils/request'
import type {AjaxResult} from './system'

// ===================== 类型定义 =====================

/** 视频设备 */
export interface VideoDeviceItem {
    id: string
    code: string
    name: string
    icon: string
    iconPath: string
    protocolCode: string
    protocolName: string | null   // ← 允许 null
    streamUrl: string
    hazardPointIds?: string | null  // ← 后端返回的是 hazardPointIds
    // hazardPointNames: string     // ← 删除，后端没有这个字段
    status: number
    installTime: string | null    // ← 允许 null
    lastOnlineTime?: string | null
    longitude?: number | null
    latitude?: number | null
}

/** 视频设备分页查询参数 */
export interface VideoDevicePageParams {
    pageNum: number
    pageSize: number
    code?: string
    name?: string
    protocolCode?: string
}

/** 分页结果 */
export interface PageResult<T> {
    rows: T[]
    total: number
    pageNum: number
    pageSize: number
}

/** 新增/编辑视频设备参数 */
export interface VideoDeviceFormData {
    id?: string
    code: string
    name: string
    icon: string
    iconPath: string
    protocolCode: string
    streamUrl: string
    longitude?: number | null
    latitude?: number | null
    status?: number
}

// ===================== API 函数 =====================

/** 分页查询视频设备 */
export function getVideoDevicePage(params: VideoDevicePageParams): Promise<AjaxResult<PageResult<VideoDeviceItem>>> {
    return request.get('/video-devices/page', {params})
}

/** 查询所有视频设备（不分页） */
export function getVideoDeviceAll(): Promise<AjaxResult<VideoDeviceItem[]>> {
    return request.get('/video-devices')
}

/** 获取视频设备详情 */
export function getVideoDeviceDetail(id: string): Promise<AjaxResult<VideoDeviceItem>> {
    return request.get(`/video-devices/${id}`)
}

/** 新增视频设备 */
export function createVideoDevice(data: VideoDeviceFormData): Promise<AjaxResult<number>> {
    return request.post('/video-devices', data)
}

/** 修改视频设备 */
export function updateVideoDevice(id: string, data: Partial<VideoDeviceFormData>): Promise<AjaxResult<null>> {
    return request.put(`/video-devices/${id}`, data)
}

/** 删除视频设备 */
export function deleteVideoDevice(id: string): Promise<AjaxResult<null>> {
    return request.delete(`/video-devices/${id}`)
}

/** 导出视频设备 */
export function exportVideoDevices() {
    return request.raw.post('/video-devices/export', {}, { responseType: 'blob' })
}