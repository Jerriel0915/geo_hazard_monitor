import { ref, reactive } from 'vue'
import { createTerraSSE, type SseCallbacks } from './terra-sse'
import { TerraToolExecutor } from './TerraToolExecutor'
import {
  getConversations,
  getConversationMessages,
  createConversation,
  deleteConversation,
  postToolResult,
} from '@/api/terra'
import type {
  TerraConversation,
  ToolCallEvent,
} from './types'

export interface ChatMessage {
  role: 'user' | 'assistant' | 'tool'
  content: string
  toolCalls?: ToolCallEvent[]
  isStreaming?: boolean
  isError?: boolean
}

// ============ 模块级共享状态（单例）============
const conversations = ref<TerraConversation[]>([])
const currentConversationId = ref<number | null>(null)
const messages = ref<ChatMessage[]>([])
const isLoading = ref(false)
const isStreaming = ref(false)
const panelOpen = ref(false)
const toolExecutor = new TerraToolExecutor()

let currentController: AbortController | null = null
let streamingMessage: ChatMessage | null = null

// ---- 打字机效果 ----
let typewriterTimer: ReturnType<typeof setInterval> | null = null
let pendingBuffer = ''

// ---- 前端导航回调（由 TerraChatPanel 设置）----
let navigateCallback: ((routeName: string, query?: Record<string, string>) => void) | null = null

export function setNavigateCallback(fn: typeof navigateCallback) {
  navigateCallback = fn
}

function startTypewriter() {
  if (typewriterTimer) return
  typewriterTimer = setInterval(() => {
    if (!streamingMessage || pendingBuffer.length === 0) {
      stopTypewriter()
      return
    }
    // 自适应速率：缓冲越长每次输出越多字符，保持流畅不堆积
    const charsPerTick = Math.max(2, Math.ceil(pendingBuffer.length / 40))
    const chunk = pendingBuffer.slice(0, charsPerTick)
    pendingBuffer = pendingBuffer.slice(charsPerTick)
    streamingMessage.content += chunk
  }, 20) // 20ms/tick ≈ 50fps
}

function stopTypewriter() {
  if (typewriterTimer) {
    clearInterval(typewriterTimer)
    typewriterTimer = null
  }
}

function flushBuffer() {
  if (streamingMessage && pendingBuffer) {
    streamingMessage.content += pendingBuffer
  }
  pendingBuffer = ''
  stopTypewriter()
}

export function useTerraChat() {

  async function loadConversations() {
    try {
      conversations.value = await getConversations()
    } catch (e) {
      console.error('加载会话列表失败', e)
    }
  }

  async function selectConversation(id: number) {
    currentConversationId.value = id
    messages.value = []
    try {
      const history = await getConversationMessages(id)
      messages.value = history.map(m => ({
        role: m.role as 'user' | 'assistant' | 'tool',
        content: m.content || '',
      }))
    } catch (e) {
      console.error('加载消息历史失败', e)
    }
  }

  async function sendMessage(text: string) {
    if (!text.trim() || isStreaming.value) return

    if (!currentConversationId.value) {
      try {
        const conv = await createConversation(text.slice(0, 30))
        currentConversationId.value = conv.id
        conversations.value.unshift(conv)
      } catch (e) {
        console.error('创建会话失败', e)
        return
      }
    }

    messages.value.push({ role: 'user', content: text })
    isStreaming.value = true

    streamingMessage = reactive({ role: 'assistant' as const, content: '', isStreaming: true })
    messages.value.push(streamingMessage)

    const url = '/api/v1/terra/chat'
    const body = {
      conversationId: currentConversationId.value,
      message: text,
    }

    const callbacks: SseCallbacks = {
      onToken: (event) => {
        if (streamingMessage) {
          pendingBuffer += event.content
          startTypewriter()
        }
      },
      onToolCall: (event) => {
        if (streamingMessage) {
          if (!streamingMessage.toolCalls) streamingMessage.toolCalls = []
          streamingMessage.toolCalls.push(event)
        }
        if (event.execSide === 'frontend') {
          const toolName = event.tool
          const params = event.params || {}

          if (toolName === 'frontend.navigate') {
            // 直接处理导航工具
            const routeName = params.routeName as string
            const query: Record<string, string> = {}
            for (const [k, v] of Object.entries(params)) {
              if (k !== 'routeName' && typeof v === 'string') query[k] = v
            }
            try {
              if (navigateCallback) {
                navigateCallback(routeName, Object.keys(query).length ? query : undefined)
                postToolResult(event.callId, true, `已打开页面: ${routeName}`).catch(() => {})
              } else {
                postToolResult(event.callId, false, '导航回调未就绪').catch(() => {})
              }
            } catch (err) {
              console.error('[Terra] 导航失败:', err)
              postToolResult(event.callId, false, '导航执行异常').catch(() => {})
            }
          } else {
            // 其他前端工具走 toolExecutor
            toolExecutor.execute(toolName, params)
              .then(result => postToolResult(event.callId, result.success, result.result || result.error))
              .catch(err => {
                console.error('[Terra] 前端工具执行异常:', toolName, err)
                postToolResult(event.callId, false, err?.message || '执行异常').catch(() => {})
              })
          }
        }
      },
      onToolResult: (_event) => {
        // 后端工具结果（已由后端执行，仅展示）
      },
      onDone: (_event) => {
        flushBuffer()
        if (streamingMessage) {
          streamingMessage.isStreaming = false
        }
        isStreaming.value = false
        streamingMessage = null
      },
      onError: (event) => {
        flushBuffer()
        if (streamingMessage) {
          streamingMessage.content += `\n\n[错误] ${event.message}`
          streamingMessage.isStreaming = false
          streamingMessage.isError = true
        }
        isStreaming.value = false
        streamingMessage = null
      },
      onClose: () => {
        flushBuffer()
        if (streamingMessage) {
          streamingMessage.isStreaming = false
        }
        isStreaming.value = false
      },
    }

    currentController = createTerraSSE(url, body, callbacks)
  }

  function stopStreaming() {
    if (currentController) {
      currentController.abort()
      currentController = null
    }
    flushBuffer()
    isStreaming.value = false
    if (streamingMessage) {
      streamingMessage.isStreaming = false
      streamingMessage = null
    }
  }

  function newConversation() {
    currentConversationId.value = null
    messages.value = []
  }

  async function removeConversation(id: number) {
    try {
      await deleteConversation(id)
      conversations.value = conversations.value.filter(c => c.id !== id)
      if (currentConversationId.value === id) {
        newConversation()
      }
    } catch (e) {
      console.error('删除会话失败', e)
    }
  }

  return {
    conversations,
    currentConversationId,
    messages,
    isLoading,
    isStreaming,
    panelOpen,
    toolExecutor,
    loadConversations,
    selectConversation,
    sendMessage,
    stopStreaming,
    newConversation,
    removeConversation,
  }
}
