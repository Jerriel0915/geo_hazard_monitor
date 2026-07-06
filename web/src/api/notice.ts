import request from '@/utils/request'
import type { AjaxResult } from './system'

// ===================== 类型定义 =====================

/** 通知公告 */
export interface SysNotice {
  noticeId: number
  noticeTitle: string
  noticeType: '1' | '2'
  noticeContent: string
  status: '0' | '1'
  createBy: string
  createTime: string
  updateBy: string
  updateTime: string
  remark: string
  /** 当前用户是否已读（listTop 返回） */
  isRead?: boolean
}

/** listTop 响应结构（后端将 unreadCount/timestamp 平铺到顶层，data 为公告数组） */
export interface TopNoticeResponse extends AjaxResult<SysNotice[]> {
  /** 分页总数 */
  total: number
  /** 当前用户未读数（顶层平铺，非 data 内字段） */
  unreadCount: number
  /** 服务端时间戳 */
  timestamp: number
}

/** 已读用户 */
export interface ReadUser {
  userId: number
  userName: string
  nickName: string
  deptName: string
  phonenumber: string
  readTime: string
}

// ===================== API 函数 =====================

/** 获取首页顶部公告（分页，含已读状态与未读数）。返回 { data, total, unreadCount, timestamp } */
export function getTopNotices(pageNum = 1, pageSize = 10, status = '0', readFilter = 'all'): Promise<TopNoticeResponse> {
  return request.get('/system/notice/listTop', { params: { pageNum, pageSize, status, readFilter } })
}

/** 分页查询通知列表（管理后台）。后端返回 TableDataInfo，rows/total 在顶层 */
export function getNoticeList(params: { pageNum?: number; pageSize?: number; noticeTitle?: string; noticeType?: string }): Promise<{ rows: SysNotice[]; total: number }> {
  return request.get('/system/notice/list', { params })
}

/** 获取通知详情 */
export function getNoticeById(noticeId: number): Promise<AjaxResult<SysNotice>> {
  return request.get(`/system/notice/${noticeId}`)
}

/** 新增通知 */
export function createNotice(data: Partial<SysNotice>): Promise<AjaxResult> {
  return request.post('/system/notice', data)
}

/** 修改通知 */
export function updateNotice(data: Partial<SysNotice>): Promise<AjaxResult> {
  return request.put('/system/notice', data)
}

/** 删除通知 */
export function deleteNotices(ids: number[]): Promise<AjaxResult> {
  return request.delete(`/system/notice/${ids.join(',')}`)
}

/** 标记已读 */
export function markRead(noticeId: number): Promise<AjaxResult> {
  return request.post('/system/notice/markRead', null, { params: { noticeId } })
}

/** 批量标记已读（指定ID） */
export function markReadAll(ids: string): Promise<AjaxResult> {
  return request.post('/system/notice/markReadAll', null, { params: { ids } })
}

/** 一键已读：标记当前用户所有未读公告(status=0)为已读（无需参数，服务端全量） */
export function markAllNoticeRead(): Promise<AjaxResult> {
  return request.post('/system/notice/markAllRead')
}

/** 已读用户列表。后端返回 TableDataInfo，rows/total 在顶层 */
export function getReadUsers(noticeId: number, searchValue?: string): Promise<{ rows: ReadUser[]; total: number }> {
  return request.get('/system/notice/readUsers/list', { params: { noticeId, searchValue } })
}
