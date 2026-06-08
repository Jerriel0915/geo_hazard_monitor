/**
 * 系统配置相关API
 * @author linx
 */
import api from './api'

/**
 * 获取公开的系统配置
 * @returns {Promise} 系统配置数据
 */
export const getPublicConfig = () => {
  return api.get('/system-config/public')
}

export default {
  getPublicConfig
}
