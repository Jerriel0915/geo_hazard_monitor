/**
 * 仪表板相关API
 * @author linx
 */

import api from './api'

export default {
  /**
   * 获取统计数据
   * @returns {Promise} 统计数据
   */
  getStats() {
    return api.get('/dashboard/stats')
  }
}
