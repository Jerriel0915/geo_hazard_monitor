/**
 * 轮询服务
 * 用于定时获取最新数据
 */

let pollingTimer: ReturnType<typeof setInterval> | null = null

/**
 * 启动轮询
 * @param callback 轮询回调函数
 * @param interval 轮询间隔(毫秒)，默认10秒
 */
export const startPolling = (callback: () => void, interval = 10000): void => {
  stopPolling()
  pollingTimer = setInterval(callback, interval)
  // 立即执行一次
  callback()
}

/**
 * 停止轮询
 */
export const stopPolling = (): void => {
  if (pollingTimer) {
    clearInterval(pollingTimer)
    pollingTimer = null
  }
}

/**
 * 检查是否正在轮询
 */
export const isPolling = (): boolean => {
  return pollingTimer !== null
}

/**
 * 重启轮询
 * @param callback 轮询回调函数
 * @param interval 轮询间隔(毫秒)
 */
export const restartPolling = (callback: () => void, interval = 10000): void => {
  stopPolling()
  startPolling(callback, interval)
}
