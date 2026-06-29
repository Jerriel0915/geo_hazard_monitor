// ============================================
// HTTP API 客户端 — 知微值守模式
// ============================================

import axios from 'axios'

const apiClient = axios.create({
  baseURL: '/api/v1/terra',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

/**
 * 获取 JWT token（与主前端共享 localStorage）
 */
function getToken(): string | null {
  return localStorage.getItem('token')
}

// 请求拦截器 — 自动附加 JWT token
apiClient.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token && !config.headers?.Authorization) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    console.error('[HTTP] Request error:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器 — 处理认证失败
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Token 过期，跳转到主前端登录页
      window.location.href = '/'
    }
    return Promise.reject(error)
  }
)

/**
 * API 接口
 */
export const api = {
  /**
   * 发送值守消息（HTTP 备用通道，主要走 WebSocket）
   */
  sendDutyMessage(message: string) {
    return apiClient.post('/duty/message', { message })
  },

  /**
   * 获取值守模式状态
   */
  getDutyState() {
    return apiClient.get('/duty/state')
  }
}

export default apiClient
