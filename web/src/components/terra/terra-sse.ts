import type {
  TokenEvent,
  ToolCallEvent,
  ToolResultEvent,
  DoneEvent,
  ErrorEvent,
} from './types'

export interface SseCallbacks {
  onToken?: (event: TokenEvent) => void
  onToolCall?: (event: ToolCallEvent) => void
  onToolResult?: (event: ToolResultEvent) => void
  onDone?: (event: DoneEvent) => void
  onError?: (event: ErrorEvent) => void
  onClose?: () => void
}

const MAX_RETRIES = 5
const BASE_DELAY = 3000
const MAX_DELAY = 30000

export function createTerraSSE(
  url: string,
  body: Record<string, unknown>,
  callbacks: SseCallbacks
): AbortController {
  const controller = new AbortController()
  let retryCount = 0

  const connect = async () => {
    try {
      const token = localStorage.getItem('token')
      const response = await fetch(url, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`,
        },
        body: JSON.stringify(body),
        signal: controller.signal,
      })

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`)
      }

      const reader = response.body!.getReader()
      const decoder = new TextDecoder()
      let buffer = ''
      let currentEventName = ''  // 必须在 while 循环外声明，跨 chunk 保持
      retryCount = 0

      while (true) {
        const { done, value } = await reader.read()
        if (done) break

        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split('\n')
        buffer = lines.pop() || ''

        for (const line of lines) {
          if (line.startsWith('event:')) {
            currentEventName = line.slice(6).trim()
          } else if (line.startsWith('data:')) {
            const dataStr = line.slice(5).trim()
            if (!dataStr) continue
            try {
              const data = JSON.parse(dataStr)
              dispatchByEventName(currentEventName || 'message', data, callbacks)
            } catch {
              // 非 JSON data 行，跳过
            }
            currentEventName = ''
          }
        }
      }

      callbacks.onClose?.()
    } catch (err: any) {
      if (err.name === 'AbortError') return
      if (retryCount < MAX_RETRIES && !controller.signal.aborted) {
        retryCount++
        const delay = Math.min(BASE_DELAY * Math.pow(2, retryCount - 1), MAX_DELAY)
        setTimeout(connect, delay)
      } else {
        callbacks.onError?.({ message: err.message || '连接失败' })
      }
    }
  }

  connect()
  return controller
}

function dispatchByEventName(eventName: string, data: any, callbacks: SseCallbacks) {
  switch (eventName) {
    case 'token':
      callbacks.onToken?.(data)
      break
    case 'tool_call':
      callbacks.onToolCall?.(data)
      break
    case 'tool_result':
      callbacks.onToolResult?.(data)
      break
    case 'done':
      callbacks.onDone?.(data)
      break
    case 'error':
      callbacks.onError?.(data)
      break
    default:
      break
  }
}
