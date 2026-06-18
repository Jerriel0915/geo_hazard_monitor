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
  cron: string
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

// 转换函数：从后端数据转换为前端格式
function convertFromBackend(data: any): ShareStrategyItem {
  return {
    ...data,
    // 将 params 字符串转换为对象
    params: data.params ? (typeof data.params === 'string' ? JSON.parse(data.params) : data.params) : {},
    // 将 scopeIds 字符串转换为数组
    scopeIds: data.scopeIds ? (typeof data.scopeIds === 'string' ? JSON.parse(data.scopeIds) : data.scopeIds) : []
  }
}

// 转换函数：从前端数据转换为后端格式
function convertToBackend(data: any): any {
  return {
    ...data,
    // 将 params 对象转换为字符串
    params: data.params ? (typeof data.params === 'object' ? JSON.stringify(data.params) : data.params) : null,
    // 将 scopeIds 数组转换为字符串
    scopeIds: data.scopeIds ? (Array.isArray(data.scopeIds) ? JSON.stringify(data.scopeIds) : data.scopeIds) : null
  }
}

export async function getShareStrategyPage(params: ShareStrategyPageRequest): Promise<ShareStrategyPageResponse> {
  const res = await request.get('/datashare/strategy/page', { params })
  return {
    ...res.data,
    rows: res.data.rows.map(convertFromBackend)
  }
}

export async function getShareStrategyDetail(id: number): Promise<ShareStrategyItem> {
  const res = await request.get(`/datashare/strategy/${id}`)
  return convertFromBackend(res.data)
}

export async function createShareStrategy(data: Omit<ShareStrategyItem, 'id' | 'successCount' | 'createTime' | 'updateTime' | 'lastRunTime' | 'lastRunStatus'>): Promise<ShareStrategyItem> {
  const res = await request.post('/datashare/strategy', convertToBackend(data))
  return convertFromBackend(res.data)
}

export async function updateShareStrategy(id: number, data: Partial<ShareStrategyItem>): Promise<ShareStrategyItem> {
  const res = await request.put(`/datashare/strategy/${id}`, convertToBackend(data))
  return convertFromBackend(res.data)
}

export async function deleteShareStrategy(id: number): Promise<void> {
  await request.delete(`/datashare/strategy/${id}`)
}

export async function changeShareStrategyStatus(id: number, status: 'ENABLED' | 'DISABLED'): Promise<ShareStrategyItem> {
  const res = await request.patch(`/datashare/strategy/${id}/status`, { status })
  return res.data
}

export async function getShareStrategyLogs(params: {
  strategyId: number
  pageNum: number
  pageSize: number
}): Promise<{ rows: ShareStrategyLog[]; total: number }> {
  const res = await request.get(`/datashare/strategy/${params.strategyId}/logs`, {
    params: { pageNum: params.pageNum, pageSize: params.pageSize }
  })
  return {
    rows: res.data.rows || [],
    total: res.data.total || 0
  }
}

export async function runShareStrategy(id: number): Promise<void> {
  await request.post(`/datashare/strategy/${id}/run`)
}

export async function getShareStrategyScript(id: number): Promise<{ script: string; variables: Record<string, any> }> {
  const res = await request.get(`/datashare/strategy/${id}/script`)
  // 将 variables 字符串转换为对象
  return {
    script: res.data.script || '',
    variables: res.data.variables ? (typeof res.data.variables === 'string' ? JSON.parse(res.data.variables) : res.data.variables) : {}
  }
}

export async function saveShareStrategyScript(id: number, data: { script: string; variables: Record<string, any> }): Promise<void> {
  // 将 variables 对象转换为字符串
  await request.post(`/datashare/strategy/${id}/script`, {
    script: data.script,
    variables: typeof data.variables === 'object' ? JSON.stringify(data.variables) : data.variables
  })
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