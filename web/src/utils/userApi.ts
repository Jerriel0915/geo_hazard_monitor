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
  sex?: string
  roleGroup?: string
  postGroup?: string
}

export interface UpdateUserParams {
  realName: string
  phone?: string
  email?: string
  sex?: string
}

export interface PasswordChangeParams {
  oldPassword: string
  newPassword: string
}

// 获取个人中心信息
export function getUserInfo(): Promise<UserInfo> {
  return request.get('/profile')
}

// 更新个人中心资料
export function updateUserInfo(data: UpdateUserParams): Promise<void> {
  return request.put('/profile', data)
}

// 修改个人中心密码
export function changePassword(data: PasswordChangeParams): Promise<void> {
  return request.put('/profile/password', data)
}

// 上传个人中心头像
export function uploadAvatar(file: File): Promise<{ imgUrl: string }> {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/profile/avatar', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
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
