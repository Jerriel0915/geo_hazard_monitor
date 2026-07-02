import request from '@/utils/request'
import type { AjaxResult } from './system'

// ==================== 类型定义 ====================

export interface DataParseStrategy {
  id: number
  name: string
  sourceType: string
  description?: string
  status: number
  appScope: 'global' | 'vendor' | 'device'
  /** MQTT 服务地址（仅描述展示用，不参与策略匹配） */
  serverUrl?: string
  /** 订阅主题（仅描述展示用，不参与策略匹配） */
  topic?: string
  scriptCode?: string
  isPreset?: number
  lastRunTime?: string
  vendorIds?: number[]
  deviceIds?: number[]
  createTime?: string
}

export interface DataParseStrategyPageParams {
  pageNum: number
  pageSize: number
  /** 关键字（name 或 topic 模糊匹配） */
  keyword?: string
  sourceType?: string
  status?: number | ''
  appScope?: string
}

export interface DataParseLog {
  id?: number
  strategyId?: number
  logLevel: 'INFO' | 'WARN' | 'ERROR' | string
  message: string
  data?: string
  topic?: string
  deviceCode?: string
  parseResult?: string
  executionTime?: number
  errorStack?: string
  createTime?: string
}

export interface DataParseLogPageParams {
  pageNum: number
  pageSize: number
  logLevel?: string
  startTime?: string
  endTime?: string
}

export interface DataParseTestPayload {
  scriptCode: string
  topic: string
  testData: string
}

export interface DataParseTestResult {
  success: boolean
  executionTime?: number
  parsedMessage?: {
    deviceCode: string
    sensorCode: string
    dataTime: number
    properties: Array<{
      identifier: string
      name: string
      unit: string
      value: number | string | boolean | null
      quality: number
    }>
  }
  error?: string
}

// ==================== 响应处理 ====================

/**
 * TableDataInfo 分页响应（{ code, msg, rows, total }）。
 * 后端 DataParseController 的分页接口返回该结构。
 */
interface TableDataInfo<T> {
  code: number
  msg: string
  rows: T[]
  total: number
}

const unwrap = async <T>(promise: Promise<AjaxResult<T>>): Promise<T> => {
  const response = await promise
  if (response && typeof response.code === 'number' && response.code !== 200) {
    throw new Error(response.msg || '操作失败')
  }
  return response.data
}

const unwrapPage = async <T>(promise: Promise<TableDataInfo<T>>): Promise<{ rows: T[]; total: number }> => {
  const response = await promise
  if (response && typeof response.code === 'number' && response.code !== 200) {
    throw new Error(response.msg || '查询失败')
  }
  return { rows: response.rows ?? [], total: response.total ?? 0 }
}

// ==================== API 方法 ====================

const BASE = '/iot/parser/strategy'

/** 分页查询策略列表 */
export const getStrategyPage = (params: DataParseStrategyPageParams) =>
  unwrapPage<DataParseStrategy>(request.get(`${BASE}/page`, { params }))

/** 策略详情（含关联厂商/设备 ID） */
export const getStrategyDetail = (id: number) =>
  unwrap<DataParseStrategy>(request.get(`${BASE}/${id}`))

/** 新增策略 */
export const createStrategy = (payload: Partial<DataParseStrategy>) =>
  unwrap<number>(request.post(BASE, payload))

/** 更新策略 */
export const updateStrategy = (payload: Partial<DataParseStrategy>) =>
  unwrap<null>(request.put(BASE, payload))

/** 逻辑删除策略 */
export const deleteStrategy = (id: number) =>
  unwrap<null>(request.delete(`${BASE}/${id}`))

/** 启停策略 */
export const toggleStrategyStatus = (id: number, status: number) =>
  unwrap<null>(request.put(`${BASE}/${id}/status`, null, { params: { status } }))

/** 复制策略 */
export const copyStrategy = (id: number) =>
  unwrap<number>(request.post(`${BASE}/${id}/copy`))

/** 在线测试脚本 */
export const testScript = (payload: DataParseTestPayload) =>
  unwrap<DataParseTestResult>(request.post(`${BASE}/test`, payload))

/** 查询策略运行日志（分页） */
export const getStrategyLogs = (id: number, params: DataParseLogPageParams) =>
  unwrapPage<DataParseLog>(request.get(`${BASE}/${id}/logs`, { params }))

/** 清空策略运行日志 */
export const clearStrategyLogs = (id: number) =>
  unwrap<null>(request.delete(`${BASE}/${id}/logs`))
