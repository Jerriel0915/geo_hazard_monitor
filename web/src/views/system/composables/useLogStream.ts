import { ref, shallowRef, onBeforeUnmount } from 'vue'

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

export type SseType = 'operation' | 'auth' | 'runtime'
export type StreamStatus = 'disconnected' | 'connecting' | 'connected' | 'error'

export interface TerminalLine {
  timestamp: string
  level: string
  logType: string
  message: string
  /** 原始行文本 — 用于文件追踪模式，直接渲染整个原始行 */
  raw?: string
}

// ---------------------------------------------------------------------------
// Constants
// ---------------------------------------------------------------------------

const MAX_LINES = 2000

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function formatTimestamp(occurredAt?: string): string {
  if (!occurredAt) return new Date().toTimeString().slice(0, 12)
  try {
    const d = new Date(occurredAt)
    if (isNaN(d.getTime())) return occurredAt
    return d.toTimeString().slice(0, 8) + '.' + String(d.getMilliseconds()).padStart(3, '0')
  } catch {
    return occurredAt
  }
}

function formatPayload(logType: string, payload: Record<string, any>): TerminalLine | null {
  if (logType === 'OPERATION') {
    const level = payload.execStatus === 'SUCCESS' ? 'INFO' : 'ERROR'
    const parts: string[] = []
    if (payload.title) parts.push(payload.title)
    if (payload.username) parts.push(payload.username)
    const detail = [payload.requestMethod, payload.apiPath, payload.costTimeMs != null ? `${payload.costTimeMs}ms` : '']
      .filter(Boolean)
      .join(' ')
    if (detail) parts.push(detail)
    return { timestamp: formatTimestamp(payload.occurredAt), level, logType: 'OPERATION', message: parts.join(' \u00b7 ') }
  }

  if (logType === 'AUTH') {
    const level = payload.resultStatus === 'SUCCESS' ? 'INFO' : 'WARN'
    const parts: string[] = []
    if (payload.authEventType) parts.push(payload.authEventType)
    if (payload.username) parts.push(payload.username)
    const detail = [payload.requestUri, payload.failureMessage].filter(Boolean).join(' ')
    if (detail) parts.push(detail)
    return { timestamp: formatTimestamp(payload.occurredAt), level, logType: 'AUTH', message: parts.join(' \u00b7 ') }
  }

  if (logType === 'RUNTIME') {
    const level = payload.level || 'INFO'
    const parts: string[] = []
    if (payload.loggerName) parts.push(payload.loggerName)
    const msg = payload.messageDigest || payload.message || ''
    if (msg) parts.push(msg)
    return { timestamp: formatTimestamp(payload.occurredAt), level, logType: 'RUNTIME', message: parts.join(' | ') }
  }

  return null
}

function parseEventBlock(block: string): { event: string; id: string; data: string } {
  const lines = block.split(/\r?\n/)
  let event = 'message'
  let id = ''
  const dataLines: string[] = []
  for (const rawLine of lines) {
    const line = rawLine.trimEnd()
    if (!line || line.startsWith(':')) continue
    const index = line.indexOf(':')
    const field = index >= 0 ? line.slice(0, index) : line
    const value = index >= 0 ? line.slice(index + 1).trimStart() : ''
    if (field === 'event') event = value
    else if (field === 'id') id = value
    else if (field === 'data') dataLines.push(value)
  }
  return { event, id, data: dataLines.join('\n') }
}

// ---------------------------------------------------------------------------
// Composable
// ---------------------------------------------------------------------------

export function useLogStream() {
  const lines = shallowRef<TerminalLine[]>([])
  const status = ref<StreamStatus>('disconnected')
  const activeTypes = ref<Set<SseType>>(new Set(['operation', 'auth', 'runtime']))
  const autoScroll = ref(true)

  let abortController: AbortController | null = null
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null
  let keepAlive = false
  let sessionId = 0

  const clearReconnectTimer = () => {
    if (reconnectTimer !== null) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
  }

  const pushLine = (line: TerminalLine) => {
    const current = lines.value
    if (current.length >= MAX_LINES) {
      lines.value = [...current.slice(current.length - MAX_LINES + 1), line]
    } else {
      lines.value = [...current, line]
    }
  }

  const scheduleReconnect = () => {
    if (!keepAlive) return
    clearReconnectTimer()
    status.value = 'disconnected'
    reconnectTimer = setTimeout(() => {
      start()
    }, 3000)
  }

  const stopInternal = () => {
    clearReconnectTimer()
    if (abortController) {
      abortController.abort()
      abortController = null
    }
  }

  const start = () => {
    if (status.value === 'connecting' || status.value === 'connected') return

    const token = localStorage.getItem('token')
    if (!token) return

    sessionId++
    const currentSessionId = sessionId
    keepAlive = false
    stopInternal()
    keepAlive = true
    abortController = new AbortController()
    status.value = 'connecting'

    const url = '/api/v1/logs/stream?types=operation,auth,runtime'

    fetch(url, {
      method: 'GET',
      headers: {
        Accept: 'text/event-stream',
        Authorization: `Bearer ${token}`,
      },
      signal: abortController.signal,
    })
      .then(async (response) => {
        if (!response.ok || !response.body) {
          throw new Error(`SSE connection failed: ${response.status}`)
        }

        if (!keepAlive || currentSessionId !== sessionId) return
        status.value = 'connected'

        const reader = response.body.getReader()
        const decoder = new TextDecoder('utf-8')
        let buffer = ''

        while (keepAlive && currentSessionId === sessionId) {
          const { value, done } = await reader.read()
          if (done) break
          buffer += decoder.decode(value, { stream: true })
          const segments = buffer.split(/\r?\n\r?\n/)
          buffer = segments.pop() || ''
          for (const segment of segments) {
            if (!segment.trim()) continue
            const parsed = parseEventBlock(segment)
            if (parsed.event === 'ready' || !parsed.data) continue
            let payload: Record<string, any> = {}
            try {
              payload = JSON.parse(parsed.data)
            } catch {
              continue
            }
            const logType = String(payload.logType || '').toUpperCase()
            const line = formatPayload(logType, payload)
            if (line) pushLine(line)
          }
        }

        if (keepAlive && currentSessionId === sessionId) {
          scheduleReconnect()
        }
      })
      .catch(() => {
        if (!keepAlive || currentSessionId !== sessionId) return
        status.value = 'error'
        scheduleReconnect()
      })
  }

  const stop = () => {
    keepAlive = false
    stopInternal()
    status.value = 'disconnected'
  }

  const clear = () => {
    lines.value = []
  }

  const toggleType = (type: SseType) => {
    const next = new Set(activeTypes.value)
    if (next.has(type)) {
      next.delete(type)
    } else {
      next.add(type)
    }
    activeTypes.value = next
  }

  onBeforeUnmount(() => {
    stop()
  })

  return {
    lines,
    status,
    activeTypes,
    autoScroll,
    start,
    stop,
    clear,
    toggleType,
  }
}

// ---------------------------------------------------------------------------
// Helpers for raw console log lines
// ---------------------------------------------------------------------------

const LEVEL_PATTERN = /(?:^|[\s\]])(DEBUG|INFO|WARN(?:ING)?|ERROR|CRITICAL|FATAL|TRACE)(?:[\s:\]]|$)/i

export function detectLevel(rawLine: string): string {
  const match = rawLine.match(LEVEL_PATTERN)
  if (!match) return 'INFO'
  const lv = match[1].toUpperCase()
  if (lv === 'WARNING') return 'WARN'
  if (lv === 'FATAL') return 'CRITICAL'
  return lv
}

// ---------------------------------------------------------------------------
// Console file stream composable
// ---------------------------------------------------------------------------

export function useConsoleStream() {
  const lines = shallowRef<TerminalLine[]>([])
  const status = ref<StreamStatus>('disconnected')
  const autoScroll = ref(true)

  let abortController: AbortController | null = null
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null
  let keepAlive = false
  let sessionId = 0

  const clearReconnectTimer = () => {
    if (reconnectTimer !== null) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
  }

  const pushLine = (raw: string) => {
    const level = detectLevel(raw)
    const now = new Date()
    const timestamp = now.toTimeString().slice(0, 8) + '.' + String(now.getMilliseconds()).padStart(3, '0')
    const line: TerminalLine = { timestamp, level, logType: 'CONSOLE', message: '', raw }
    const current = lines.value
    if (current.length >= MAX_LINES) {
      lines.value = [...current.slice(current.length - MAX_LINES + 1), line]
    } else {
      lines.value = [...current, line]
    }
  }

  const scheduleReconnect = () => {
    if (!keepAlive) return
    clearReconnectTimer()
    status.value = 'disconnected'
    reconnectTimer = setTimeout(() => {
      start()
    }, 3000)
  }

  const stopInternal = () => {
    clearReconnectTimer()
    if (abortController) {
      abortController.abort()
      abortController = null
    }
  }

  const start = () => {
    if (status.value === 'connecting' || status.value === 'connected') return

    const token = localStorage.getItem('token')
    if (!token) return

    sessionId++
    const currentSessionId = sessionId
    keepAlive = false
    stopInternal()
    keepAlive = true
    abortController = new AbortController()
    status.value = 'connecting'

    fetch('/api/v1/logs/console-stream', {
      method: 'GET',
      headers: {
        Accept: 'text/event-stream',
        Authorization: `Bearer ${token}`,
      },
      signal: abortController.signal,
    })
      .then(async (response) => {
        if (!response.ok || !response.body) {
          throw new Error(`Console stream failed: ${response.status}`)
        }

        if (!keepAlive || currentSessionId !== sessionId) return
        status.value = 'connected'

        const reader = response.body.getReader()
        const decoder = new TextDecoder('utf-8')
        let buffer = ''

        while (keepAlive && currentSessionId === sessionId) {
          const { value, done } = await reader.read()
          if (done) break
          buffer += decoder.decode(value, { stream: true })
          const segments = buffer.split(/\r?\n\r?\n/)
          buffer = segments.pop() || ''
          for (const segment of segments) {
            if (!segment.trim()) continue
            const parsed = parseEventBlock(segment)
            if (parsed.event === 'ready') continue
            if (parsed.data) {
              pushLine(parsed.data)
            }
          }
        }

        if (keepAlive && currentSessionId === sessionId) {
          scheduleReconnect()
        }
      })
      .catch(() => {
        if (!keepAlive || currentSessionId !== sessionId) return
        status.value = 'error'
        scheduleReconnect()
      })
  }

  const stop = () => {
    keepAlive = false
    stopInternal()
    status.value = 'disconnected'
  }

  const clear = () => {
    lines.value = []
  }

  onBeforeUnmount(() => {
    stop()
  })

  return {
    lines,
    status,
    autoScroll,
    start,
    stop,
    clear,
  }
}
