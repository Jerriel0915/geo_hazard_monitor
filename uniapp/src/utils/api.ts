/**
 * API请求工具类
 * @author linx
 */

// 环境配置
const ENV = {
  // 开发环境
  DEV: {
    BASE_URL: 'http://124.221.142.86/api/v1',
    MQTT_URL: 'ws://124.221.142.86:8083/mqtt',
  },
  // 生产环境
  PROD: {
    BASE_URL: 'http://124.221.142.86/api/v1',
    MQTT_URL: 'ws://124.221.142.86:8083/mqtt',
  }
}

// 当前环境
const currentEnv = ENV.PROD

const BASE_URL = currentEnv.BASE_URL
const MQTT_URL = currentEnv.MQTT_URL

/**
 * 获取存储的Token
 * @returns {string|null} Token
 */
const getToken = () => {
  return uni.getStorageSync('accessToken')
}

/**
 * 通用请求方法
 * @param {Object} options 请求配置
 * @returns {Promise} 请求Promise
 */
const request = (options) => {
  return new Promise((resolve, reject) => {
    const token = getToken()

    uni.request({
      url: BASE_URL + options.url,
      method: options.method || 'GET',
      data: options.data,
      header: {
        'Content-Type': 'application/json',
        'Authorization': token ? `Bearer ${token}` : '',
        ...options.header
      },
      success: (res) => {
        // 处理200状态码
        if (res.statusCode === 200) {
          // 检查是否为ApiResponse格式
          if (res.data && typeof res.data === 'object') {
            // 如果有code字段，检查是否成功
            if ('code' in res.data) {
              if (res.data.code === 200) {
                // 返回data字段或整个响应
                resolve(res.data.data !== undefined ? res.data.data : res.data)
              } else {
                // 业务错误，显示错误消息
                const message = res.data.message || '请求失败'
                uni.showToast({
                  title: message,
                  icon: 'none',
                  duration: 2000
                })
                reject(new Error(message))
              }
            } else {
              // 兼容旧格式，直接返回数据
              resolve(res.data)
            }
          } else {
            resolve(res.data)
          }
        }
        // 处理401未授权
        else if (res.statusCode === 401) {
          uni.removeStorageSync('accessToken')
          uni.removeStorageSync('refreshToken')
          uni.removeStorageSync('user')
          // 如果不是静默模式，跳转登录
          if (!options.silent) {
            uni.reLaunch({ url: '/pages/login' })
          }
          reject(new Error('登录已过期，请重新登录'))
        }
        // 处理403权限不足
        else if (res.statusCode === 403) {
          uni.showToast({
            title: '权限不足',
            icon: 'none',
            duration: 2000
          })
          reject(new Error('权限不足'))
        }
        // 处理其他HTTP错误
        else {
          const message = res.data?.message || res.data?.msg || `请求失败(${res.statusCode})`
          uni.showToast({
            title: message,
            icon: 'none',
            duration: 2000
          })
          reject(new Error(message))
        }
      },
      fail: (err) => {
        uni.showToast({
          title: '网络请求失败',
          icon: 'none',
          duration: 2000
        })
        reject(new Error('网络请求失败'))
      }
    })
  })
}

/**
 * GET请求
 * @param {string} url 请求路径
 * @param {Object} params 查询参数
 * @param {Object} options 额外选项
 * @returns {Promise} 请求Promise
 */
const get = (url, params = {}, options = {}) => {
  const queryString = Object.keys(params)
    .filter(key => params[key] !== undefined && params[key] !== null)
    .map(key => `${encodeURIComponent(key)}=${encodeURIComponent(params[key])}`)
    .join('&')

  return request({
    url: queryString ? `${url}?${queryString}` : url,
    method: 'GET',
    ...options
  })
}

/**
 * POST请求
 * @param {string} url 请求路径
 * @param {Object} data 请求数据
 * @returns {Promise} 请求Promise
 */
const post = (url, data = {}) => {
  return request({
    url,
    method: 'POST',
    data
  })
}

/**
 * PUT请求
 * @param {string} url 请求路径
 * @param {Object} data 请求数据
 * @param {Object} options 额外选项（支持 params 查询参数）
 * @returns {Promise} 请求Promise
 */
const put = (url, data = {}, options = {}) => {
  let finalUrl = url

  // 如果有 params，构建查询字符串
  if (options.params) {
    const queryString = Object.keys(options.params)
      .filter(key => options.params[key] !== undefined && options.params[key] !== null)
      .map(key => `${encodeURIComponent(key)}=${encodeURIComponent(options.params[key])}`)
      .join('&')
    if (queryString) {
      finalUrl = `${url}?${queryString}`
    }
  }

  return request({
    url: finalUrl,
    method: 'PUT',
    data,
    ...options
  })
}

/**
 * DELETE请求
 * @param {string} url 请求路径
 * @returns {Promise} 请求Promise
 */
const del = (url) => {
  return request({
    url,
    method: 'DELETE'
  })
}

export default {
  request,
  get,
  post,
  put,
  del,
  getToken,
  BASE_URL,
  MQTT_URL
}
