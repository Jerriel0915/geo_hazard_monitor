/**
 * 认证相关 API
 * @author linx
 */
import http from '@/utils/api'

export interface CaptchaInfo {
  captchaEnabled: boolean
  captchaKey: string
  captchaImage: string
}

export interface LoginResult {
  token: string
  expiresIn?: number
}

export interface UserInfo {
  userId: number
  userName: string
  nickName: string
  phonenumber: string
  avatar: string
  deptId?: number
  email?: string
}

export interface GetInfoResult {
  user: UserInfo
  roles: string[]
  permissions: string[]
}

const authApi = {
  /**
   * 获取图形验证码
   */
  getCaptcha(): Promise<CaptchaInfo> {
    return http.get('/auth/captcha', {}, { silent: true }) as Promise<CaptchaInfo>
  },

  /**
   * 账号密码登录
   */
  login(username: string, password: string, code: string, uuid: string): Promise<LoginResult> {
    return http.post('/auth/login', {
      username,
      password,
      code,
      uuid,
      rememberMe: false,
    }) as Promise<LoginResult>
  },

  /**
   * 获取当前登录用户信息
   */
  getUserInfo(): Promise<GetInfoResult> {
    return http.get('/auth/getInfo') as Promise<GetInfoResult>
  },

  /**
   * 登出
   */
  logout(): Promise<void> {
    return http.post('/system/auth/logout', {}) as Promise<void>
  },
}

export default authApi
