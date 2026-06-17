import request from '@/utils/request'

export interface ShareStrategyItem {
  id: number
  code: string
  name: string
  description: string
  method: 'UNIFIED_PUSH' | 'CUSTOM_PUSH' | 'UNIFIED_SERVICE' | 'CUSTOM_SERVICE'
  address: string
  topic: string
  username: string
  password: string
  params: Record<string, any>
  scopeType: 'HAZARD_POINT_GROUP' | 'HAZARD_POINT' | 'VENDOR' | 'DEVICE'
  scopeIds: number[]
  frequency: string
  status: 'ENABLED' | 'DISABLED'
  successCount: number
  createTime: string
  updateTime: string
  lastRunTime?: string
  lastRunStatus?: 'SUCCESS' | 'ERROR' | 'TIMEOUT'
}

export interface ShareStrategyLog {
  id: number
  strategyId: number
  runTime: string
  status: 'SUCCESS' | 'ERROR' | 'TIMEOUT'
  message: string
  dataCount: number
  duration: number
}

export interface ShareStrategyPageRequest {
  pageNum: number
  pageSize: number
  name?: string
  status?: 'ENABLED' | 'DISABLED'
  method?: ShareStrategyItem['method']
}

export interface ShareStrategyPageResponse {
  rows: ShareStrategyItem[]
  total: number
}

export async function getShareStrategyPage(params: ShareStrategyPageRequest): Promise<ShareStrategyPageResponse> {
  const res = await request.get('/share-strategies', { params })
  return res.data
}

export async function getShareStrategyDetail(id: number): Promise<ShareStrategyItem> {
  const res = await request.get(`/share-strategies/${id}`)
  return res.data
}

export async function createShareStrategy(data: Omit<ShareStrategyItem, 'id' | 'successCount' | 'createTime' | 'updateTime' | 'lastRunTime' | 'lastRunStatus'>): Promise<ShareStrategyItem> {
  const res = await request.post('/share-strategies', data)
  return res.data
}

export async function updateShareStrategy(id: number, data: Partial<ShareStrategyItem>): Promise<ShareStrategyItem> {
  const res = await request.put(`/share-strategies/${id}`, data)
  return res.data
}

export async function deleteShareStrategy(id: number): Promise<void> {
  await request.delete(`/share-strategies/${id}`)
}

export async function changeShareStrategyStatus(id: number, status: 'ENABLED' | 'DISABLED'): Promise<ShareStrategyItem> {
  const res = await request.patch(`/share-strategies/${id}/status`, { status })
  return res.data
}

export async function getShareStrategyLogs(params: {
  strategyId: number
  pageNum: number
  pageSize: number
}): Promise<{ rows: ShareStrategyLog[]; total: number }> {
  const res = await request.get(`/share-strategies/${params.strategyId}/logs`, {
    params: { pageNum: params.pageNum, pageSize: params.pageSize }
  })
  return res.data
}

export async function runShareStrategy(id: number): Promise<void> {
  await request.post(`/share-strategies/${id}/run`)
}

export async function getShareStrategyScript(id: number): Promise<{ script: string; variables: Record<string, any> }> {
  const res = await request.get(`/share-strategies/${id}/script`)
  return res.data
}

export async function saveShareStrategyScript(id: number, data: { script: string; variables: Record<string, any> }): Promise<void> {
  await request.put(`/share-strategies/${id}/script`, data)
}

export const METHOD_LABELS: Record<ShareStrategyItem['method'], string> = {
  UNIFIED_PUSH: '统一化数据推送',
  CUSTOM_PUSH: '定制化数据推送',
  UNIFIED_SERVICE: '统一化数据服务',
  CUSTOM_SERVICE: '定制化数据服务'
}

export const SCOPE_TYPE_LABELS: Record<ShareStrategyItem['scopeType'], string> = {
  HAZARD_POINT_GROUP: '隐患点分组',
  HAZARD_POINT: '隐患点',
  VENDOR: '厂商',
  DEVICE: '设备'
}