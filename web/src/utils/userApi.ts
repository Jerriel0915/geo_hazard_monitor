import request from './request'

export interface UserInfo {
  id: number
  username: string
  realName: string
  phone: string
  email: string
  orgId: number
  orgName: string
  avatar?: string
  status?: number
  createTime?: string
}

export interface UpdateUserParams {
  realName?: string
  phone?: string
  email?: string
  orgId?: number
}

export interface PasswordChangeParams {
  oldPassword: string
  newPassword: string
}

// 获取当前用户详情 - 调用 /api/v1/auth/getInfo 获取用户信息
export function getUserInfo(): Promise<UserInfo> {
  return request.get('/auth/getInfo')
}

// 更新用户信息 - 调用 /system/user/profile (id参数已废弃，接口从token获取用户)
export function updateUserInfo(_id: number, data: UpdateUserParams): Promise<void> {
  return request.put('/system/user/profile', data)
}

// 修改密码 - 调用 /system/user/profile/updatePwd
export function changePassword(data: PasswordChangeParams): Promise<void> {
  return request.put('/system/user/profile/updatePwd', data)
}

// 获取当前登录用户ID（从 localStorage）
export function getCurrentUserId(): number | null {
  const userInfo = localStorage.getItem('userInfo')
  if (userInfo) {
    try {
      return JSON.parse(userInfo).id
    } catch {
      return null
    }
  }
  return null
}

// 保存用户信息到 localStorage
export function saveUserInfo(user: { id: number; username: string; realName: string; orgId: number; orgName: string }): void {
  localStorage.setItem('userInfo', JSON.stringify(user))
}
