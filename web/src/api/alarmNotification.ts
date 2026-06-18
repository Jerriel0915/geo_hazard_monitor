import request from '@/utils/request'
import type { AjaxResult } from './system'

// ===================== 类型定义 =====================

/**
 * 通知中心事件列表项（前端展示用）。
 */
export interface AlarmNotificationItem {
  /** 通知记录 ID */
  id: number
  /** 来源类型：alarm / offline */
  sourceType: 'alarm' | 'offline'
  /** 来源 ID（alarm_record.id 或 device.id） */
  sourceId: number
  /** 通知标题 */
  title: string
  /** 通知正文 */
  content: string
  /** 接收人名称 */
  recipientName?: string
  /** 已读时间（NULL=未读） */
  readTime: string | null
  /** 创建时间（事件时间） */
  createTime: string
}

/**
 * 未读汇总。
 */
export interface AlarmNotificationSummary {
  unreadCount: number
  timestamp: number
}

// ===================== API 函数 =====================

/**
 * 查询当前用户最近事件通知（默认 10 条，事件 Tab 数据源）。
 */
export function getRecentAlarmNotifications(limit = 10): Promise<AjaxResult<AlarmNotificationItem[]>> {
  return request.get('/alarm/notifications/recent', { params: { limit } })
}

/**
 * 当前用户未读事件数。
 */
export function getAlarmNotificationUnreadCount(): Promise<AjaxResult<AlarmNotificationSummary>> {
  return request.get('/alarm/notifications/unread-count')
}

/**
 * 标记单条事件通知已读。
 */
export function markAlarmNotificationRead(id: number): Promise<AjaxResult> {
  return request.post(`/alarm/notifications/${id}/read`)
}

/**
 * 全部事件通知标记已读。
 */
export function markAllAlarmNotificationsRead(): Promise<AjaxResult> {
  return request.post('/alarm/notifications/read-all')
}
