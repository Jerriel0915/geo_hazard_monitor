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
  sex?: string
  status?: number
  roleGroup?: string
  postGroup?: string
}

export interface UpdateUserParams {
  realName?: string
  phone?: string
  email?: string
  sex?: string
}

export interface PasswordChangeParams {
  oldPassword: string
  newPassword: string
}

export interface AuthInfo {
  roles: string[]
  permissions: string[]
  loginIp: string
  loginDate: string
  isDefaultModifyPwd: boolean
  isPasswordExpired: boolean
  pwdChrtype: string
}

// 获取个人中心信息
export function getUserInfo(): Promise<UserInfo> {
  return request.get('/profile').then(res => {
    const profile = res.data || {}
    return {
      id: profile.id || 0,
      username: profile.username || '',
      realName: profile.realName || '',
      phone: profile.phone || '',
      email: profile.email || '',
      orgId: profile.orgId || 0,
      orgName: profile.orgName || '',
      avatar: profile.avatar || '',
      sex: profile.sex || '',
      status: typeof profile.status === 'number' ? profile.status : 0,
      roleGroup: profile.roleGroup || '',
      postGroup: profile.postGroup || ''
    }
  })
}

// 获取认证扩展信息
export function getAuthInfo(): Promise<AuthInfo> {
  return request.get('/auth/getInfo').then(res => {
    const payload = res.data || res
    const user = payload.user || {}
    return {
      roles: payload.roles || [],
      permissions: payload.permissions || [],
      loginIp: user.loginIp || '',
      loginDate: user.loginDate || '',
      isDefaultModifyPwd: payload.isDefaultModifyPwd || false,
      isPasswordExpired: payload.isPasswordExpired || false,
      pwdChrtype: payload.pwdChrtype || ''
    }
  })
}

// 更新个人中心资料
export function updateUserInfo(_id: number, data: UpdateUserParams): Promise<void> {
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
  }).then(res => ({
    imgUrl: res.imgUrl || ''
  }))
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
