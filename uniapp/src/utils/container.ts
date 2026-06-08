/**
 * 集装箱相关API
 * @author linx
 */

import api from './api'

export default {
  /**
   * 获取所有集装箱
   * @returns {Promise} 集装箱列表
   */
  getAll() {
    return api.get('/containers/all')
  },

  /**
   * 获取当前用户绑定的集装箱
   * @returns {Promise} 集装箱列表
   */
  getBound() {
    return api.get('/containers/bound')
  },

  /**
   * 绑定集装箱到当前用户
   * @param {number} containerId 集装箱ID
   * @returns {Promise} 绑定结果
   */
  bind(containerId) {
    return api.post(`/containers/${containerId}/bind`)
  },

  /**
   * 解绑集装箱
   * @param {number} containerId 集装箱ID
   * @returns {Promise} 解绑结果
   */
  unbind(containerId) {
    return api.post(`/containers/${containerId}/unbind`)
  },

  /**
   * 根据ID获取集装箱
   * @param {number} id 集装箱ID
   * @returns {Promise} 集装箱信息
   */
  getById(id) {
    return api.get(`/containers/${id}`)
  },

  /**
   * 获取集装箱实时数据
   * @param {number} containerId 集装箱ID
   * @returns {Promise} 集装箱实时数据
   */
  getRealtime(containerId) {
    return api.get(`/containers/${containerId}/realtime`)
  },

  /**
   * 根据编号获取集装箱
   * @param {string} containerNo 集装箱编号
   * @returns {Promise} 集装箱信息
   */
  getByNo(containerNo) {
    return api.get(`/containers/no/${containerNo}`)
  },

  /**
   * 创建集装箱
   * @param {Object} data 集装箱数据
   * @returns {Promise} 创建的集装箱
   */
  create(data) {
    return api.post('/containers', data)
  },

  /**
   * 更新集装箱
   * @param {number} id 集装箱ID
   * @param {Object} data 集装箱数据
   * @returns {Promise} 更新后的集装箱
   */
  update(id, data) {
    return api.put(`/containers/${id}`, data)
  },

  /**
   * 删除集装箱
   * @param {number} id 集装箱ID
   * @returns {Promise} 删除响应
   */
  delete(id) {
    return api.del(`/containers/${id}`)
  },

  /**
   * 获取集装箱二维码
   * @param {number} containerId 集装箱ID
   * @returns {Promise} 二维码URL
   */
  getQrCode(containerId) {
    return api.get(`/containers/${containerId}/qrcode`)
  }
}
