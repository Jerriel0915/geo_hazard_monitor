import {ElMessage} from 'element-plus'

/**
 * 统一提取请求错误的用户可读消息。
 *
 * 兼容三种错误来源：
 * 1. api/*.ts 的 unwrap 抛出的 Error（error.message = 后端 msg）
 * 2. axios 拦截器 reject 的网络/HTTP 错误（error.response.data.msg = 后端 msg）
 * 3. 业务兜底文案（fallback）
 */
export function getRequestErrorMessage(error: any, fallback: string): string {
    return error?.message || error?.response?.data?.msg || fallback
}

/**
 * 控制台记录 + ElMessage 弹窗统一封装。
 */
export function showRequestErrorMessage(error: any, fallback: string): void {
    console.error(`${fallback}:`, error)
    ElMessage.error(getRequestErrorMessage(error, fallback))
}
