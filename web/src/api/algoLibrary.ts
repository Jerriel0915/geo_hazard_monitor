import request from '@/utils/request'

// ===== 类型 =====
export interface AlgoInfoPageParams {
  pageNum?: number
  pageSize?: number
  name?: string
  status?: 0 | 1
  code?: string
}

export interface AlgoInfoPayload {
  code?: string
  name: string
  description?: string
  remark?: string
}

export interface AlgoVersionUploadPayload {
  file: File
  versionNo: string
  remark?: string
}

export interface AlgoInfo {
  id: number
  code: string
  name: string
  description?: string
  status: 0 | 1
  delFlag?: 0 | 1
  versionCount?: number
  latestVersionNo?: string
  latestUploadTime?: string
  createTime?: string
  updateTime?: string
  remark?: string
  versions?: AlgoVersion[]
}

export interface AlgoVersion {
  id: number
  algoId: number
  versionNo: string
  fileName: string
  originalName: string
  fileSize: number
  sha256?: string
  createBy?: string
  createTime?: string
  remark?: string
}

export interface PageResult<T> {
  code: number
  msg: string
  rows: T[]
  total: number
}

// ===== 算法 =====
export function getAlgoLibraryPage(params: AlgoInfoPageParams) {
  return request.get<PageResult<AlgoInfo>>('/algo-lib/page', { params })
}

export function getAlgoLibraryDetail(id: number | string) {
  return request.get<{ code: number; msg: string; data: AlgoInfo }>(`/algo-lib/${id}`)
}

export function createAlgoLibrary(data: AlgoInfoPayload) {
  return request.post('/algo-lib', data)
}

export function updateAlgoLibrary(id: number | string, data: AlgoInfoPayload) {
  return request.put(`/algo-lib/${id}`, data)
}

export function updateAlgoLibraryStatus(id: number | string, status: 0 | 1) {
  return request.put(`/algo-lib/${id}/status`, null, { params: { status } })
}

export function deleteAlgoLibrary(id: number | string) {
  return request.delete(`/algo-lib/${id}`)
}

// ===== 版本 =====
export function getAlgoVersionList(algoId: number | string) {
  return request.get<{ code: number; msg: string; data: AlgoVersion[] }>(`/algo-lib/${algoId}/versions`)
}

export function uploadAlgoVersion(
  algoId: number | string,
  payload: AlgoVersionUploadPayload,
  onProgress?: (percent: number) => void
) {
  const formData = new FormData()
  formData.append('file', payload.file)
  formData.append('versionNo', payload.versionNo)
  if (payload.remark) formData.append('remark', payload.remark)
  return request.post(`/algo-lib/${algoId}/versions/upload`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: (e) => {
      if (onProgress && e.total) onProgress(Math.round((e.loaded * 100) / e.total))
    }
  })
}

export function deleteAlgoVersion(id: number | string) {
  return request.delete(`/algo-lib/versions/${id}`)
}

export function downloadAlgoVersion(id: number | string) {
  return request.raw.get(`/algo-lib/versions/${id}/download`, { responseType: 'blob' })
}
