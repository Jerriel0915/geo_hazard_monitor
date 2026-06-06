<template>
  <div class="video-player-container">
    <div class="video-wrapper" ref="wrapperRef">
      <video
          ref="videoRef"
          class="video-el"
          muted
          autoplay
          playsinline
          @loadedmetadata="onLoaded"
          @playing="onPlaying"
          @pause="onPause"
          @waiting="onWaiting"
          @stalled="onStalled"
          @error="onError"
      ></video>

      <!-- 加载中 -->
      <div v-if="playerState === 'connecting'" class="overlay">
        <div class="overlay-content">
          <div class="spinner"></div>
          <span class="overlay-text">连接视频流...</span>
        </div>
      </div>

      <!-- 错误 -->
      <div v-if="playerState === 'error'" class="overlay overlay-error">
        <div class="overlay-content">
          <span class="overlay-text">{{ errorMsg || '视频流加载失败' }}</span>
          <button class="btn-retry" @click="reconnect">重新连接</button>
        </div>
      </div>

      <!-- 状态栏 -->
      <div v-if="playerState === 'playing'" class="status-bar">
        <span class="status-dot" :class="latencyClass"></span>
        <span class="status-text">{{ playerTypeLabel }}</span>
        <span v-if="latencyMs !== null" class="status-latency">{{ latencyMs }}ms</span>
      </div>
    </div>

    <!-- 控制栏 -->
    <div class="controls">
      <button class="ctrl-btn" @click="togglePlay" :title="paused ? '播放' : '暂停'">
        {{ paused ? '▶' : '⏸' }}
      </button>
      <button class="ctrl-btn" @click="takeScreenshot" title="截图">📷</button>
      <button class="ctrl-btn" @click="toggleFullscreen" title="全屏">⛶</button>
      <button class="ctrl-btn" @click="togglePip" title="画中画">🖼</button>
      <button class="ctrl-btn" @click="reconnect" title="刷新">↻</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import {computed, nextTick, onBeforeUnmount, ref, watch} from 'vue'
import mpegts from 'mpegts.js'
import Hls from 'hls.js'

interface Props {
  streamUrl: string
  autoplay?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  autoplay: true
})

const emit = defineEmits<{
  (e: 'stateChange', state: string): void
  (e: 'error', msg: string): void
}>()

// ==================== 状态 ====================

type PlayerState = 'idle' | 'connecting' | 'playing' | 'stalled' | 'error'
const playerState = ref<PlayerState>('idle')
const latencyMs = ref<number | null>(null)
const errorMsg = ref('')
const paused = ref(false)
const playerType = ref<'flv' | 'hls' | 'native' | ''>('')

const videoRef = ref<HTMLVideoElement | null>(null)
const wrapperRef = ref<HTMLDivElement | null>(null)

let mpegtsPlayer: mpegts.Player | null = null
let hlsInstance: Hls | null = null

// ==================== 计算属性 ====================

const playerTypeLabel = computed(() => {
  const map: Record<string, string> = {flv: 'HTTP-FLV', hls: 'HLS', native: '原生'}
  return map[playerType.value] || ''
})

const latencyClass = computed(() => {
  if (latencyMs.value === null) return ''
  if (latencyMs.value < 3000) return 'latency-good'
  if (latencyMs.value < 6000) return 'latency-warn'
  return 'latency-bad'
})

// ==================== 播放器核心 ====================

function detectType(url: string): 'flv' | 'hls' | 'native' {
  const lower = url.toLowerCase()
  if (lower.includes('.flv') || lower.includes('/flv')) return 'flv'
  if (lower.includes('.m3u8') || lower.includes('.m3u') || lower.includes('/hls')) return 'hls'
  return 'native'
}

function createFlvPlayer(url: string) {
  if (!videoRef.value) return
  playerType.value = 'flv'

  mpegtsPlayer = mpegts.createPlayer(
      {
        type: 'flv',
        url,
        isLive: true
      },
      {
        enableWorker: true,
        enableStashBuffer: false,
        stashInitialSize: 128,
        isLive: true,
        liveBufferLatencyChasing: true,
        liveBufferLatencyMaxLatency: 3,
        liveBufferLatencyMinRemain: 0.5
      }
  )

  mpegtsPlayer.attachMediaElement(videoRef.value)
  mpegtsPlayer.load()

  playerState.value = 'connecting'

  mpegtsPlayer.on(mpegts.Events.ERROR, (_type, _info) => {
    playerState.value = 'error'
    errorMsg.value = 'FLV 流加载失败'
    emit('error', 'FLV stream error')
    destroyPlayer()
  })

  mpegtsPlayer.on(mpegts.Events.STATISTICS_INFO, (info) => {
    // info.endToEndLatency is in ms
    if (typeof info.endToEndLatency === 'number' && info.endToEndLatency > 0) {
      latencyMs.value = info.endToEndLatency
    }
  })

  mpegtsPlayer.on(mpegts.Events.LOADING_COMPLETE, () => {
    // live stream never completes loading, this indicates buffer full
  })

  mpegtsPlayer.play()
}

function createHlsPlayer(url: string) {
  if (!videoRef.value) return
  playerType.value = 'hls'
  playerState.value = 'connecting'

  const hls = new Hls({
    lowLatencyMode: true,
    enableWorker: true,
    liveSyncDurationCount: 2,
    liveMaxLatencyDurationCount: 5,
    liveDurationInfinity: true,
    highBufferWatchdogPeriod: 1,
    nudgeOffset: 0.1,
    nudgeMaxRetry: 3,
    maxBufferLength: 10,
    maxMaxBufferLength: 30
  })

  hlsInstance = hls
  hls.attachMedia(videoRef.value)
  hls.on(Hls.Events.MEDIA_ATTACHED, () => {
    hls.loadSource(url)
  })

  hls.on(Hls.Events.ERROR, (_event, data) => {
    if (data.fatal) {
      playerState.value = 'error'
      errorMsg.value = 'HLS 流加载失败'
      emit('error', 'HLS fatal error')
      destroyPlayer()
    }
  })

  hls.on(Hls.Events.MANIFEST_PARSED, () => {
    videoRef.value?.play().catch(() => {
    })
  })
}

function createNativePlayer(url: string) {
  if (!videoRef.value) return
  playerType.value = 'native'
  playerState.value = 'connecting'
  videoRef.value.src = url
  videoRef.value.load()
  videoRef.value.play().catch(() => {
  })
}

function createPlayer() {
  destroyPlayer()

  if (!props.streamUrl) {
    playerState.value = 'error'
    errorMsg.value = '未配置视频流地址'
    return
  }

  const type = detectType(props.streamUrl)
  if (type === 'flv') {
    createFlvPlayer(props.streamUrl)
  } else if (type === 'hls') {
    createHlsPlayer(props.streamUrl)
  } else {
    createNativePlayer(props.streamUrl)
  }
}

function destroyPlayer() {
  if (mpegtsPlayer) {
    try {
      mpegtsPlayer.detachMediaElement()
      mpegtsPlayer.destroy()
    } catch { /* ignore cleanup errors */
    }
    mpegtsPlayer = null
  }

  if (hlsInstance) {
    try {
      hlsInstance.detachMedia()
      hlsInstance.destroy()
    } catch { /* ignore */
    }
    hlsInstance = null
  }

  if (videoRef.value) {
    videoRef.value.src = ''
    videoRef.value.load()
  }

  latencyMs.value = null
  playerType.value = ''
}

function reconnect() {
  destroyPlayer()
  playerState.value = 'idle'
  errorMsg.value = ''
  paused.value = false
  nextTick(() => createPlayer())
}

// ==================== 控制方法 ====================

function togglePlay() {
  if (!videoRef.value) return
  if (videoRef.value.paused) {
    videoRef.value.play().catch(() => {
    })
    paused.value = false
  } else {
    videoRef.value.pause()
    paused.value = true
  }
}

function toggleFullscreen() {
  if (!wrapperRef.value) return
  if (document.fullscreenElement) {
    document.exitFullscreen()
  } else {
    wrapperRef.value.requestFullscreen()
  }
}

function togglePip() {
  if (!videoRef.value) return
  if (document.pictureInPictureElement) {
    document.exitPictureInPicture()
  } else {
    videoRef.value.requestPictureInPicture().catch(() => {
    })
  }
}

function takeScreenshot() {
  const video = videoRef.value
  if (!video || video.videoWidth === 0 || video.videoHeight === 0) {
    return
  }
  try {
    const canvas = document.createElement('canvas')
    canvas.width = video.videoWidth
    canvas.height = video.videoHeight
    const ctx = canvas.getContext('2d')
    if (!ctx) return
    ctx.drawImage(video, 0, 0, canvas.width, canvas.height)
    try {
      const dataUrl = canvas.toDataURL('image/png')
      const link = document.createElement('a')
      link.download = `screenshot_${Date.now()}.png`
      link.href = dataUrl
      link.click()
    } catch {
      // cross-origin screenshots are blocked by browser security policy
    }
  } catch {
    // canvas tainted by cross-origin content
  }
}

// ==================== 事件处理 ====================

function onLoaded() {
  // native video metadata loaded
}

function onPlaying() {
  playerState.value = 'playing'
  paused.value = false
}

function onPause() {
  paused.value = true
}

function onWaiting() {
  if (playerState.value === 'playing') {
    playerState.value = 'stalled'
  }
}

function onStalled() {
  playerState.value = 'stalled'
}

function onError() {
  playerState.value = 'error'
  errorMsg.value = '播放失败'
  emit('error', 'Video element error')
}

// ==================== 生命周期 ====================

watch(
    () => props.streamUrl,
    (newUrl) => {
      if (newUrl) {
        reconnect()
      }
    }
)

// 延迟初始化，等 DOM 挂载完成
nextTick(() => {
  if (props.streamUrl) {
    createPlayer()
  }
})

onBeforeUnmount(() => {
  destroyPlayer()
})
</script>

<style scoped>
.video-player-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
}

.video-wrapper {
  width: 100%;
  height: 480px;
  background: #000;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}

.video-el {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

/* 覆盖层 */
.overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.7);
}

.overlay-error {
  background: rgba(0, 0, 0, 0.85);
}

.overlay-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  color: #fff;
}

.spinner {
  width: 32px;
  height: 32px;
  border: 3px solid rgba(255, 255, 255, 0.2);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.overlay-text {
  font-size: 14px;
}

.btn-retry {
  padding: 6px 20px;
  border: none;
  border-radius: 4px;
  background: #1890ff;
  color: #fff;
  font-size: 13px;
  cursor: pointer;
}

.btn-retry:hover {
  background: #40a9ff;
}

/* 状态栏 */
.status-bar {
  position: absolute;
  top: 8px;
  left: 8px;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  background: rgba(0, 0, 0, 0.55);
  border-radius: 4px;
  font-size: 12px;
  color: #fff;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #52c41a;
}

.latency-good {
  background: #52c41a;
}

.latency-warn {
  background: #faad14;
}

.latency-bad {
  background: #ff4d4f;
}

.status-text {
  opacity: 0.7;
}

.status-latency {
  margin-left: 4px;
  font-variant-numeric: tabular-nums;
}

/* 控制栏 */
.controls {
  display: flex;
  gap: 4px;
  margin-top: 10px;
}

.ctrl-btn {
  width: 34px;
  height: 30px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  background: #fff;
  font-size: 14px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.ctrl-btn:hover {
  border-color: #1890ff;
  color: #1890ff;
}
</style>
