const AUTH_FAILURE_PATTERN = /认证失败|登录状态已过期|重新登录|token已过期|令牌已过期|无效token|unauthorized/i

let isRedirectingToLogin = false

export const clearAuthStorage = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
}

export const isAuthFailure = (payload?: unknown, status?: number) => {
  if (status === 401) {
    return true
  }

  if (!payload || typeof payload !== 'object') {
    return false
  }

  const data = payload as Record<string, unknown>
  const code = typeof data.code === 'number' ? data.code : Number(data.code)
  const message = String(data.msg ?? data.message ?? '')

  return code === 401 || AUTH_FAILURE_PATTERN.test(message)
}

export const redirectToLogin = () => {
  clearAuthStorage()

  if (typeof window === 'undefined' || window.location.pathname === '/login' || isRedirectingToLogin) {
    return
  }

  isRedirectingToLogin = true
  const currentPath = `${window.location.pathname}${window.location.search}${window.location.hash}`
  const loginUrl = `/login?redirect=${encodeURIComponent(currentPath)}`
  window.location.replace(loginUrl)
}

export const handleAuthFailure = (payload?: unknown, status?: number) => {
  if (!isAuthFailure(payload, status)) {
    return false
  }

  redirectToLogin()
  return true
}
