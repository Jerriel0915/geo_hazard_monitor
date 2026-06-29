// ============================================
// TerraMens Store
// ============================================

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { TerraState, AlertData, TimelineItem, TerraStatus, TerraStateLabels, TerraStateColors } from '@/types'

export const useTerraStore = defineStore('terra', () => {
  // TerraMens 状态
  const state = ref<TerraState>('normal')
  const watching = ref<string[]>([])
  const currentAlert = ref<AlertData | null>(null)
  const timelineItems = ref<TimelineItem[]>([])

  // 流式消息控制
  const activeStreamingRunId = ref<string | null>(null)
  const streamingMessageQueue: Array<Omit<TimelineItem, 'id'>> = []

  // 思考状态
  const thinking = ref<{
    phase: 'analyzing' | 'comparing' | 'deciding' | 'acting' | null
    progress: number
    focus: string[]
    message: string
  }>({
    phase: null,
    progress: 0,
    focus: [],
    message: ''
  })

  // 当前消息
  const currentMessage = ref<string>('')

  /**
   * 状态标签
   */
  const stateLabel = computed(() => {
    return TerraStateLabels[state.value]
  })

  /**
   * 状态颜色
   */
  const stateColor = computed(() => {
    return TerraStateColors[state.value]
  })

  /**
   * 是否有活跃警报
   */
  const hasAlert = computed(() => currentAlert.value !== null)

  /**
   * 更新 TerraMens 状态
   */
  function updateState(newState: TerraState) {
    state.value = newState
  }

  /**
   * 更新关注点列表
   */
  function updateWatching(points: string[]) {
    watching.value = points
  }

  /**
   * 添加关注点
   */
  function addWatching(point: string) {
    if (!watching.value.includes(point)) {
      watching.value.push(point)
    }
  }

  /**
   * 移除关注点
   */
  function removeWatching(point: string) {
    const index = watching.value.indexOf(point)
    if (index > -1) {
      watching.value.splice(index, 1)
    }
  }

  /**
   * 设置当前警报
   */
  function setAlert(alert: AlertData | null) {
    currentAlert.value = alert
  }

  /**
   * 清除警报
   */
  function clearAlert() {
    currentAlert.value = null
  }

  /**
   * 更新思考状态
   */
  function updateThinking(data: {
    phase?: 'analyzing' | 'comparing' | 'deciding' | 'acting' | null
    progress?: number
    focus?: string[]
    message?: string
  }) {
    if (data.phase !== undefined) thinking.value.phase = data.phase
    if (data.progress !== undefined) thinking.value.progress = data.progress
    if (data.focus !== undefined) thinking.value.focus = data.focus
    if (data.message !== undefined) thinking.value.message = data.message
  }

  /**
   * 重置思考状态
   */
  function resetThinking() {
    thinking.value = {
      phase: null,
      progress: 0,
      focus: [],
      message: ''
    }
  }

  /**
   * 添加时间线条目
   * 支持流式更新（通过 runId 识别同一个消息的更新）
   */
  function addTimelineItem(item: Omit<TimelineItem, 'id'>) {
    // 如果是流式消息且有 runId，更新现有消息
    if ((item as any).isStreaming && (item as any).runId) {
      const runId = (item as any).runId

      // 如果有其他活动的流式消息，将新消息加入队列
      if (activeStreamingRunId.value && activeStreamingRunId.value !== runId) {
        console.log('[TerraStore] Another message is streaming, queueing:', runId)
        streamingMessageQueue.push(item)
        return
      }

      // 设置为当前活动的流式消息
      activeStreamingRunId.value = runId

      const existingItem = timelineItems.value.find(t => (t as any).runId === runId)

      if (existingItem) {
        // 更新现有消息的内容
        existingItem.message = item.message || ''
        existingItem.timestamp = item.timestamp || Date.now()
        return
      }

      // 创建新的流式消息
      const newItem: TimelineItem = {
        id: `timeline-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
        timestamp: item.timestamp || Date.now(),
        ...item
      }
      ;(newItem as any).runId = runId
      ;(newItem as any).isStreaming = true
      timelineItems.value.push(newItem)
      return
    }

    // 如果是带有 runId 的非流式消息，标记流式消息为完成
    if ((item as any).runId && !(item as any).isStreaming) {
      const runId = (item as any).runId

      // 只有当前活动的流式消息才处理
      if (activeStreamingRunId.value === runId) {
        const existingItem = timelineItems.value.find(t => (t as any).runId === runId)
        if (existingItem) {
          // 更新消息内容并标记为非流式
          ;(existingItem as any).isStreaming = false
          existingItem.message = item.message || ''
          existingItem.timestamp = item.timestamp || Date.now()
        }

        // 清除活动流式消息
        activeStreamingRunId.value = null

        // 处理队列中的下一个消息
        processStreamingQueue()
        return
      }
    }

    // 普通消息，直接添加
    const newItem: TimelineItem = {
      id: `timeline-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
      timestamp: item.timestamp || Date.now(),
      ...item
    }
    timelineItems.value.push(newItem)

    // 限制时间线条目数量（最多保留 100 条）
    if (timelineItems.value.length > 100) {
      timelineItems.value.shift()
    }
  }

  /**
   * 处理流式消息队列
   */
  function processStreamingQueue() {
    if (streamingMessageQueue.length > 0 && !activeStreamingRunId.value) {
      const nextItem = streamingMessageQueue.shift()
      if (nextItem) {
        console.log('[TerraStore] Processing next streaming message from queue')
        addTimelineItem(nextItem)
      }
    }
  }

  /**
   * 清空时间线
   */
  function clearTimeline() {
    timelineItems.value = []
  }

  /**
   * 更新当前消息
   */
  function updateMessage(message: string) {
    currentMessage.value = message
  }

  /**
   * 获取完整状态
   */
  function getStatus(): TerraStatus {
    return {
      state: state.value,
      watching: watching.value,
      message: currentMessage.value,
      thinking: thinking.value.phase ? {
        phase: thinking.value.phase!,
        progress: thinking.value.progress,
        focus: thinking.value.focus
      } : undefined,
      alert: currentAlert.value || undefined
    }
  }

  return {
    // 状态
    state,
    watching,
    currentAlert,
    timelineItems,
    thinking,
    currentMessage,
    activeStreamingRunId,

    // 计算属性
    stateLabel,
    stateColor,
    hasAlert,

    // 方法
    updateState,
    updateWatching,
    addWatching,
    removeWatching,
    setAlert,
    clearAlert,
    updateThinking,
    resetThinking,
    addTimelineItem,
    clearTimeline,
    updateMessage,
    getStatus,
    processStreamingQueue
  }
})
