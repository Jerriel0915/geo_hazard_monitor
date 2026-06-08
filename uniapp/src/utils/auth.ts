/**
 * 认证相关 - 模拟数据
 * @author linx
 */

const mockUser = {
  id: 1,
  username: 'admin',
  nickname: '管理员',
  phone: '13800138000',
  avatar: ''
}

const mockAuth = {
  login(phone: string, password: string) {
    return Promise.resolve({
      accessToken: 'mock-access-token-' + Date.now(),
      refreshToken: 'mock-refresh-token-' + Date.now(),
      user: { ...mockUser, phone }
    })
  },

  wechatLogin(code: string, phoneCode: string) {
    return Promise.resolve({
      accessToken: 'mock-access-token-' + Date.now(),
      refreshToken: 'mock-refresh-token-' + Date.now(),
      user: mockUser
    })
  },

  refreshToken(refreshToken: string) {
    return Promise.resolve({
      accessToken: 'mock-access-token-' + Date.now(),
      refreshToken: 'mock-refresh-token-' + Date.now(),
      user: mockUser
    })
  },

  logout() {
    return Promise.resolve({ success: true })
  },

  checkToken() {
    return Promise.resolve({ valid: true })
  }
}

export default mockAuth
