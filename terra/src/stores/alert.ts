// ============================================
// Alert Store - 警报管理
// ============================================

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { AlertData } from '@/types'

export const useAlertStore = defineStore('alert', () => {
  // 当前活跃的警报
  const currentAlert = ref<AlertData | null>(null)

  // 警报历史
  const alertHistory = ref<AlertData[]>([])

  // 警报是否可见
  const alertVisible = ref(false)

  /**
   * 是否有活跃警报
   */
  const hasAlert = computed(() => currentAlert.value !== null)

  /**
   * 警报级别
   */
  const alertLevel = computed(() => {
    return currentAlert.value?.level || null
  })

  /**
   * 显示警报
   */
  function showAlert(alert: AlertData) {
    currentAlert.value = alert
    alertVisible.value = true

    // 添加到历史
    alertHistory.value.unshift({ ...alert })
    if (alertHistory.value.length > 100) {
      alertHistory.value.pop()
    }

    console.log('[AlertStore] Alert shown:', alert.level, alert.title)
  }

  /**
   * 隐藏警报
   */
  function hideAlert() {
    alertVisible.value = false
    console.log('[AlertStore] Alert hidden')
  }

  /**
   * 清除当前警报
   */
  function clearAlert() {
    currentAlert.value = null
    alertVisible.value = false
    console.log('[AlertStore] Alert cleared')
  }

  /**
   * 处理警报操作
   */
  function handleAlertAction(actionId: string) {
    if (!currentAlert.value) return

    const action = currentAlert.value.actions?.find(a => a.id === actionId)
    if (action) {
      console.log('[AlertStore] Alert action:', actionId, action.label)
      // TODO: 发送操作到后端
    }

    // 隐藏警报
    hideAlert()
  }

  /**
   * 获取警报历史
   */
  function getAlertHistory(limit?: number): AlertData[] {
    if (limit) {
      return alertHistory.value.slice(0, limit)
    }
    return alertHistory.value
  }

  /**
   * 清空警报历史
   */
  function clearHistory() {
    alertHistory.value = []
  }

  return {
    // 状态
    currentAlert,
    alertHistory,
    alertVisible,

    // 计算属性
    hasAlert,
    alertLevel,

    // 方法
    showAlert,
    hideAlert,
    clearAlert,
    handleAlertAction,
    getAlertHistory,
    clearHistory
  }
})
