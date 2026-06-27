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
          streamingMessage.content += event.content
        }
      },
      onToolCall: async (event) => {
        if (streamingMessage) {
          if (!streamingMessage.toolCalls) streamingMessage.toolCalls = []
          streamingMessage.toolCalls.push(event)
        }
        if (event.execSide === 'frontend') {
          const result = await toolExecutor.execute(event.tool, event.params || {})
          await postToolResult(event.callId, result.success, result.result || result.error)
        }
      },
      onToolResult: (_event) => {
        // 后端工具结果（已由后端执行，仅展示）
      },
      onDone: (_event) => {
        if (streamingMessage) {
          streamingMessage.isStreaming = false
        }
        isStreaming.value = false
        streamingMessage = null
      },
      onError: (event) => {
        if (streamingMessage) {
          streamingMessage.content += `\n\n[错误] ${event.message}`
          streamingMessage.isStreaming = false
          streamingMessage.isError = true
        }
        isStreaming.value = false
        streamingMessage = null
      },
      onClose: () => {
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
