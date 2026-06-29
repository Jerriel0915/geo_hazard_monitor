// ============================================
// 视频面板指令处理器
// ============================================

import { ref } from 'vue'

/**
 * 视频元素引用
 */
export function useVideoPanelCommands(videoElement: Ref<HTMLVideoElement | null>) {
  /**
   * 播放
   */
  async function play() {
    if (!videoElement.value) {
      throw new Error('Video not initialized')
    }

    await videoElement.value.play()
    return { success: true, playing: true }
  }

  /**
   * 暂停
   */
  async function pause() {
    if (!videoElement.value) {
      throw new Error('Video not initialized')
    }

    videoElement.value.pause()
    return { success: true, playing: false }
  }

  /**
   * 停止
   */
  async function stop() {
    if (!videoElement.value) {
      throw new Error('Video not initialized')
    }

    videoElement.value.pause()
    videoElement.value.currentTime = 0
    return { success: true, stopped: true }
  }

  /**
   * 跳转到指定时间
   */
  async function seek(params: { time: number }) {
    if (!videoElement.value) {
      throw new Error('Video not initialized')
    }

    videoElement.value.currentTime = params.time
    return { success: true, currentTime: params.time }
  }

  /**
   * 设置音量
   */
  async function setVolume(params: { volume: number }) {
    if (!videoElement.value) {
      throw new Error('Video not initialized')
    }

    videoElement.value.volume = Math.max(0, Math.min(1, params.volume))
    return { success: true, volume: videoElement.value.volume }
  }

  /**
   * 设置视频源
   */
  async function setSource(params: { url: string }) {
    if (!videoElement.value) {
      throw new Error('Video not initialized')
    }

    videoElement.value.src = params.url
    videoElement.value.load()
    return { success: true, url: params.url }
  }

  /**
   * 截图
   */
  async function screenshot() {
    if (!videoElement.value) {
      throw new Error('Video not initialized')
    }

    const canvas = document.createElement('canvas')
    canvas.width = videoElement.value.videoWidth
    canvas.height = videoElement.value.videoHeight
    const ctx = canvas.getContext('2d')

    if (ctx) {
      ctx.drawImage(videoElement.value, 0, 0, canvas.width, canvas.height)
      const dataUrl = canvas.toDataURL('image/png')
      return { success: true, dataUrl }
    }

    throw new Error('Failed to create canvas context')
  }

  return {
    // 生命周期
    lifecycle: {
      show: async () => ({ success: true }),
      hide: async () => ({ success: true })
    },

    // 布局
    layout: {
      maximize: async () => ({ success: true }),
      restore: async () => ({ success: true }),
      minimize: async () => ({ success: true })
    },

    // 数据
    data: {
      set: async (params: any) => ({ success: true }),
      update: async (params: any) => ({ success: true }),
      refresh: async () => ({ success: true })
    },

    // 视频特定指令
    video: {
      play,
      pause,
      stop,
      seek,
      setVolume,
      setSource,
      screenshot
    }
  }
}
