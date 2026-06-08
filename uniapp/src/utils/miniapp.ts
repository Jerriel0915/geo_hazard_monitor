/**
 * 小程序专用API
 * @author linx
 */
import api from './api'

/**
 * 获取最近告警列表
 * @param limit 限制数量，默认10条
 */
export const getRecentAlarms = (limit = 10) => {
  return api.get('/miniapp/alarms/recent', { limit })
}

/**
 * 处理告警
 * @param alarmId 告警ID
 * @param remark 处理备注
 */
export const handleAlarm = (alarmId: number, remark: string) => {
  return api.post(`/miniapp/alarms/${alarmId}/handle`, { remark })
}

/**
 * 忽略告警
 * @param alarmId 告警ID
 * @param remark 忽略备注
 */
export const ignoreAlarm = (alarmId: number, remark?: string) => {
  return api.post(`/miniapp/alarms/${alarmId}/ignore`, { remark })
}

/**
 * 保存订阅状态
 * @param subscribeType 订阅类型(ALARM/DATA)
 * @param openid 微信openid
 * @param templateId 模板ID
 */
export const saveSubscription = (subscribeType: string, openid: string, templateId?: string) => {
  return api.post('/miniapp/subscription', {
    subscribeType,
    openid,
    templateId
  })
}

/**
 * 取消订阅
 * @param subscribeType 订阅类型
 */
export const cancelSubscription = (subscribeType: string) => {
  return api.del(`/miniapp/subscription?type=${subscribeType}`)
}

export default {
  getRecentAlarms,
  handleAlarm,
  ignoreAlarm,
  saveSubscription,
  cancelSubscription
}
