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
  return request.get('/auth/getInfo').then(res => {
    // 真实返回结构: { code, msg, user, roles, permissions, ... }
    const user = res.user || {}
    const dept = user.dept || {}

    return {
      id: user.userId || 0,
      username: user.userName || '',      // userName -> username
      realName: user.nickName || '',
      phone: user.phonenumber || '',
      email: user.email || '',
      orgId: user.deptId || 0,
      orgName: dept.deptName || '',
      avatar: user.avatar || '',
      status: user.status ? parseInt(user.status) : 0,
      createTime: user.createTime
    }
  })
}

// 更新用户信息 - 调用 /system/user/profile
export function updateUserInfo(_id: number, data: UpdateUserParams): Promise<void> {
  // 转换参数格式给后端
  const requestData: any = {}
  if (data.realName !== undefined) requestData.nickName = data.realName
  if (data.phone !== undefined) requestData.phonenumber = data.phone
  if (data.email !== undefined) requestData.email = data.email
  if (data.orgId !== undefined) requestData.deptId = data.orgId

  return request.put('/system/user/profile', requestData)
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