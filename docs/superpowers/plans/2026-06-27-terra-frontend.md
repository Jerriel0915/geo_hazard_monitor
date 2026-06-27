# Terra 智能助手前端实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 构建 Terra 智能助手前端 — 全局悬浮球 + 透明对话面板 + 设置中心 + 前端工具执行框架。

**架构：** Vue 3 Composition API + TypeScript。悬浮球挂载在全局 Layout，对话面板使用 SSE 流式接收后端响应。设置中心复用项目现有 Element Plus 表单模式。前端工具执行器通过 SSE 接收 tool_call 事件，本地执行后通过 HTTP POST 回调结果。

**技术栈：** Vue 3.4 + TypeScript + Element Plus 2.6 + marked（Markdown 渲染）+ DOMPurify（XSS 防护）

**规格文档：** `docs/superpowers/specs/2026-06-27-terra-ai-assistant-design.md`（第 7 节 前端架构）

---

## 文件结构

### 创建的文件

```
web/src/
├── api/
│   └── terra.ts                              # Terra API 封装（人格/模型/技能/工具/对话/会话）
├── components/
│   └── terra/
│       ├── types.ts                           # TypeScript 类型定义
│       ├── terra-sse.ts                       # SSE 连接管理（对话流 + 指数退避重连）
│       ├── useTerraChat.ts                    # 对话 Composable（状态管理 + 发消息 + 工具回调）
│       ├── TerraToolExecutor.ts               # 前端工具执行器（注册 + 派发 + 回调）
│       ├── TerraWidget.vue                    # 悬浮球入口（蓝色圆圈，可拖动）
│       ├── TerraMessage.vue                   # 单条消息渲染（Markdown + 工具结果卡片）
│       └── TerraChatPanel.vue                 # 对话面板（右侧悬浮，透明背景）
└── views/
    └── terra/
        ├── SettingsLayout.vue                 # 设置页布局（左侧 Tab 导航）
        ├── PersonalitySettings.vue            # 人格配置编辑
        ├── ModelConfigList.vue                # 模型配置管理（CRUD + 激活）
        ├── SkillManager.vue                   # 技能管理（查看 + 启停 + 卸载）
        └── ToolManager.vue                    # 工具管理（CRUD + 启停）
```

### 修改的文件

```
web/package.json                               # 添加 marked + dompurify 依赖
web/src/layout/index.vue                       # 挂载 <TerraWidget />
web/src/router/index.ts                        # 添加 /terra/settings 路由
```

---

## 任务 1：安装依赖 + 创建 API 模块 + 类型定义

**文件：**
- 修改：`web/package.json`
- 创建：`web/src/api/terra.ts`
- 创建：`web/src/components/terra/types.ts`

- [ ] **步骤 1：安装 Markdown 渲染依赖**

```bash
cd web && npm install marked dompurify
```

- [ ] **步骤 2：创建类型定义 `web/src/components/terra/types.ts`**

```typescript
/** Terra 人格配置 */
export interface TerraPersonality {
  id: number
  layerType: string         // core | role
  name: string
  content: string
  isActive: number          // 0 | 1
  isPreset: number          // 0 | 1
  sortOrder: number
  createTime: string
}

/** Terra 模型配置 */
export interface TerraModelConfig {
  id: number
  name: string
  baseUrl: string
  apiKey: string             // 列表接口返回脱敏值
  modelName: string
  maxTokens: number
  temperature: number
  isActive: number
  sortOrder: number
}

/** Terra 技能 */
export interface TerraSkill {
  id: number
  name: string
  displayName: string
  description: string
  category: string
  sourceType: string         // preset | custom
  isEnabled: number
  version: string
  skillPath: string
  createTime: string
}

/** Terra 工具 */
export interface TerraTool {
  id: number
  toolKey: string
  name: string
  description: string
  execSide: string           // backend | frontend
  toolType: string           // code | config
  category: string
  config: string             // JSON string (config 类型工具的 HTTP 端点配置)
  isEnabled: number
  sortOrder: number
}

/** Terra 会话 */
export interface TerraConversation {
  id: number
  userId: number
  title: string
  status: string             // active | archived
  lastMessageTime: string
  messageCount: number
}

/** Terra 消息 */
export interface TerraMessageData {
  id: number
  conversationId: number
  role: string               // user | assistant | tool
  content: string
  toolCalls: string | null
  toolCallId: string | null
  tokensUsed: number | null
  createTime: string
}

/** SSE 事件数据类型 */
export interface TokenEvent {
  content: string
}

export interface ToolCallEvent {
  callId: string
  tool: string
  execSide: string
  params?: Record<string, unknown>
}

export interface ToolResultEvent {
  callId: string
  success: boolean
  result: unknown
}

export interface DoneEvent {
  messageId: number
  tokensUsed: number
}

export interface ErrorEvent {
  message: string
}

/** 工具执行结果 */
export interface ToolResult {
  success: boolean
  result?: unknown
  error?: string
}
```

- [ ] **步骤 3：创建 API 模块 `web/src/api/terra.ts`**

```typescript
import request from '@/utils/request'
import type {
  TerraPersonality,
  TerraModelConfig,
  TerraSkill,
  TerraTool,
  TerraConversation,
  TerraMessageData,
} from '@/components/terra/types'

/**
 * 项目 request.ts 的 get/post/put/delete 已返回 response.data（即 AjaxResult 对象）。
 * unwrap 检查 code 并提取 data 字段。
 */
interface AjaxResult<T = any> {
  code: number
  msg: string
  data: T
}

const unwrap = async <T>(promise: Promise<AjaxResult<T>>): Promise<T> => {
  const res = await promise
  if (res && res.code !== 200) {
    throw new Error(res.msg || '操作失败')
  }
  return res.data
}

// ============ 人格配置 ============

export const getPersonalities = () =>
  unwrap<TerraPersonality[]>(request.get('/terra/personality'))

export const updatePersonality = (data: Partial<TerraPersonality>) =>
  unwrap<void>(request.put('/terra/personality', data))

export const togglePersonality = (id: number) =>
  unwrap<void>(request.put(`/terra/personality/${id}/toggle`))

// ============ 模型配置 ============

export const getModelConfigs = () =>
  unwrap<TerraModelConfig[]>(request.get('/terra/model-configs'))

export const getModelConfig = (id: number) =>
  unwrap<TerraModelConfig>(request.get(`/terra/model-configs/${id}`))

export const createModelConfig = (data: Partial<TerraModelConfig>) =>
  unwrap<void>(request.post('/terra/model-configs', data))

export const updateModelConfig = (data: Partial<TerraModelConfig>) =>
  unwrap<void>(request.put('/terra/model-configs', data))

export const deleteModelConfig = (id: number) =>
  unwrap<void>(request.delete(`/terra/model-configs/${id}`))

export const activateModelConfig = (id: number) =>
  unwrap<void>(request.put(`/terra/model-configs/${id}/activate`))

// ============ 技能管理 ============

export const getSkills = () =>
  unwrap<TerraSkill[]>(request.get('/terra/skills'))

export const getSkillDetail = (id: number) =>
  unwrap<TerraSkill>(request.get(`/terra/skills/${id}`))

export const deleteSkill = (id: number) =>
  unwrap<void>(request.delete(`/terra/skills/${id}`))

export const toggleSkill = (id: number) =>
  unwrap<void>(request.put(`/terra/skills/${id}/toggle`))

// ============ 工具管理 ============

export const getTools = () =>
  unwrap<TerraTool[]>(request.get('/terra/tools'))

export const createTool = (data: Partial<TerraTool>) =>
  unwrap<void>(request.post('/terra/tools', data))

export const updateTool = (data: Partial<TerraTool>) =>
  unwrap<void>(request.put('/terra/tools', data))

export const deleteTool = (id: number) =>
  unwrap<void>(request.delete(`/terra/tools/${id}`))

export const toggleTool = (id: number) =>
  unwrap<void>(request.put(`/terra/tools/${id}/toggle`))

// ============ 对话 & 会话 ============

export const getConversations = () =>
  unwrap<TerraConversation[]>(request.get('/terra/conversations'))

export const getConversationMessages = (id: number) =>
  unwrap<TerraMessageData[]>(request.get(`/terra/conversations/${id}/messages`))

export const createConversation = (title: string) =>
  unwrap<TerraConversation>(request.post('/terra/conversations', { title }))

export const deleteConversation = (id: number) =>
  unwrap<void>(request.delete(`/terra/conversations/${id}`))

/** 前端工具执行结果回调 */
export const postToolResult = (callId: string, success: boolean, result: unknown) =>
  unwrap<void>(request.post('/terra/chat/tool-result', { callId, success, result }))
```

> **注意：** 项目 `request.ts` 的 `get/post/put/delete` 已返回 `response.data`（即 AjaxResult 对象），所以 `unwrap` 直接检查 `.code` 并提取 `.data`。

- [ ] **步骤 4：验证类型编译**

```bash
cd web && npx vue-tsc --noEmit 2>&1 | head -20
```

如有类型错误修正后继续。

- [ ] **步骤 5：Commit**

```bash
git add web/package.json web/package-lock.json web/src/api/terra.ts web/src/components/terra/types.ts
git commit -m "feat(terra-fe): 安装依赖 + API 模块 + 类型定义"
```

---

## 任务 2：SSE 连接管理 + 对话 Composable

**文件：**
- 创建：`web/src/components/terra/terra-sse.ts`
- 创建：`web/src/components/terra/useTerraChat.ts`

- [ ] **步骤 1：创建 SSE 连接管理 `web/src/components/terra/terra-sse.ts`**

```typescript
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

/**
 * 创建 Terra 对话 SSE 连接。
 * 使用 fetch + ReadableStream 而非 EventSource，因为需要 POST 请求体。
 */
export function createTerraSSE(
  url: string,
  body: Record<string, unknown>,
  callbacks: SseCallbacks
): AbortController {
  const controller = new AbortController()
  let retryCount = 0

  const connect = async () => {
    try      | const token = localStorage.getItem('token')
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
      retryCount = 0

      while (true) {
        const { done, value } = await reader.read()
        if (done) break

        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split('\n')
        buffer = lines.pop() || ''

        for (const line of lines) {
          if (line.startsWith('data:')) {
            const data = line.slice(5).trim()
            if (!data) continue
            try {
              const parsed = JSON.parse(data)
              dispatchEvent(parsed, callbacks)
            } catch {
              // 非 JSON data 行，跳过
            }
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

function dispatchEvent(data: any, callbacks: SseCallbacks) {
  // 根据 SSE event name 分发
  // Terra SSE 事件格式: event:name\ndata:json
  // 由于 fetch 流解析，这里 data 已经是 JSON 对象
  // 后端 SseEmitter 发送的 event name 在 data 之前的 event: 行
  // 但我们在上面只解析了 data: 行，event name 通过 data 内部字段区分
  // 实际后端 TerraSseEmitter 的 data 格式:
  //   token: {"content":"..."}
  //   tool_call: {"callId":"...","tool":"...","execSide":"..."}
  //   tool_result: {"callId":"...","success":...}
  //   done: {"messageId":...,"tokensUsed":...}
  //   error: {"message":"..."}

  if (data.content !== undefined && typeof data.content === 'string') {
    callbacks.onToken?.(data)
  } else if (data.callId && data.tool && data.execSide) {
    callbacks.onToolCall?.(data)
  } else if (data.callId && data.success !== undefined && !data.tool) {
    callbacks.onToolResult?.(data)
  } else if (data.messageId !== undefined) {
    callbacks.onDone?.(data)
  } else if (data.message && typeof data.message === 'string') {
    callbacks.onError?.(data)
  }
}
```

> **注意：** 后端 SseEmitter 使用 `event().name("token").data(...)` 发送。fetch ReadableStream 解析时需同时处理 `event:` 行和 `data:` 行。上面代码需要完善：在解析行时跟踪当前 event name，然后在 data 行中根据 event name 分发。修正版见步骤 1b。

- [ ] **步骤 1b：修正 SSE 解析（处理 event:name 行）**

替换 `createTerraSSE` 中的流解析逻辑：

```typescript
      while (true) {
        const { done, value } = await reader.read()
        if (done) break

        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split('\n')
        buffer = lines.pop() || ''

        let currentEventName = ''
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
```

替换 `dispatchEvent` 为：

```typescript
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
```

- [ ] **步骤 2：创建对话 Composable `web/src/components/terra/useTerraChat.ts`**

```typescript
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
// TerraWidget 和 TerraChatPanel 都调用 useTerraChat()，必须共享同一份状态。
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

  /** 加载会话列表 */
  async function loadConversations() {
    try {
      conversations.value = await getConversations()
    } catch (e) {
      console.error('加载会话列表失败', e)
    }
  }

  /** 切换到指定会话 */
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

  /** 发送消息 */
  async function sendMessage(text: string) {
    if (!text.trim() || isStreaming.value) return

    // 如果没有会话，先创建（标题取前 30 字）
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

    // 添加用户消息到 UI
    messages.value.push({ role: 'user', content: text })
    isStreaming.value = true

    // 准备流式 assistant 消息
    streamingMessage = reactive({ role: 'assistant' as const, content: '', isStreaming: true })
    messages.value.push(streamingMessage)

    // SSE 连接
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
        // 工具调用事件
        if (streamingMessage) {
          if (!streamingMessage.toolCalls) streamingMessage.toolCalls = []
          streamingMessage.toolCalls.push(event)
        }
        // 前端工具：执行并回调
        if (event.execSide === 'frontend') {
          const result = await toolExecutor.execute(event.tool, event.params || {})
          await postToolResult(event.callId, result.success, result.result || result.error)
        }
      },
      onToolResult: (event) => {
        // 后端工具结果（已由后端执行，仅展示）
      },
      onDone: (event) => {
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

  /** 停止当前流式对话 */
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

  /** 新建会话 */
  function newConversation() {
    currentConversationId.value = null
    messages.value = []
  }

  /** 删除会话 */
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
```

- [ ] **步骤 3：验证编译**

```bash
cd web && npx vue-tsc --noEmit 2>&1 | grep -i "terra" | head -20
```

> **注意：** `TerraToolExecutor` 在任务 3 中创建。为避免编译错误，可以先创建一个空壳文件 `web/src/components/terra/TerraToolExecutor.ts`，只包含类定义和 execute 方法签名。完整实现在任务 3 中补充。

- [ ] **步骤 4：Commit**

```bash
git add web/src/components/terra/terra-sse.ts web/src/components/terra/useTerraChat.ts
git commit -m "feat(terra-fe): SSE 连接管理 + 对话 Composable"
```

---

## 任务 3：前端工具执行器 + TerraWidget 悬浮球

**文件：**
- 创建：`web/src/components/terra/TerraToolExecutor.ts`
- 创建：`web/src/components/terra/TerraWidget.vue`

- [ ] **步骤 1：创建 TerraToolExecutor `web/src/components/terra/TerraToolExecutor.ts`**

```typescript
import type { ToolResult } from './types'

type ToolHandler = (params: Record<string, unknown>) => Promise<ToolResult>

/**
 * 前端工具执行器。
 * 业务模块通过 register() 注册前端工具处理器，
 * SSE 收到 tool_call(execSide=frontend) 时自动派发。
 */
export class TerraToolExecutor {
  private handlers = new Map<string, ToolHandler>()

  /** 注册前端工具处理器 */
  register(toolName: string, handler: ToolHandler) {
    this.handlers.set(toolName, handler)
  }

  /** 注销工具处理器 */
  unregister(toolName: string) {
    this.handlers.delete(toolName)
  }

  /** 执行工具 */
  async execute(toolName: string, params: Record<string, unknown>): Promise<ToolResult> {
    const handler = this.handlers.get(toolName)
    if (!handler) {
      return { success: false, error: `未注册的前端工具: ${toolName}` }
    }
    try {
      return await handler(params)
    } catch (e: any) {
      return { success: false, error: e.message || '工具执行异常' }
    }
  }

  /** 检查工具是否已注册 */
  has(toolName: string): boolean {
    return this.handlers.has(toolName)
  }
}
```

- [ ] **步骤 2：创建 TerraWidget.vue `web/src/components/terra/TerraWidget.vue`**

```vue
<template>
  <div
    class="terra-widget"
    :style="{ left: pos.x + 'px', top: pos.y + 'px' }"
    @mousedown="onMouseDown"
    @touchstart.passive="onTouchStart"
  >
    <div class="terra-orb" :class="{ active: chat.panelOpen.value }">
      <span class="terra-orb-icon">T</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useTerraChat } from './useTerraChat'

const STORAGE_KEY = 'terra-widget-pos'

const chat = useTerraChat()

const pos = ref({ x: window.innerWidth - 72, y: window.innerHeight - 72 })
const dragging = ref(false)
const moved = ref(false)
const startPos = ref({ x: 0, y: 0 })
const startMouse = ref({ x: 0, y: 0 })

// 加载保存的位置
onMounted(() => {
  const saved = localStorage.getItem(STORAGE_KEY)
  if (saved) {
    try {
      const p = JSON.parse(saved)
      pos.value = clampToViewport(p)
    } catch { /* ignore */ }
  }
  chat.loadConversations()
})

function clampToViewport(p: { x: number; y: number }) {
  return {
    x: Math.max(0, Math.min(p.x, window.innerWidth - 48)),
    y: Math.max(0, Math.min(p.y, window.innerHeight - 48)),
  }
}

function onMouseDown(e: MouseEvent) {
  dragging.value = true
  moved.value = false
  startPos.value = { ...pos.value }
  startMouse.value = { x: e.clientX, y: e.clientY }
  document.addEventListener('mousemove', onMouseMove)
  document.addEventListener('mouseup', onMouseUp)
}

function onMouseMove(e: MouseEvent) {
  if (!dragging.value) return
  const dx = e.clientX - startMouse.value.x
  const dy = e.clientY - startMouse.value.y
  if (Math.abs(dx) > 3 || Math.abs(dy) > 3) {
    moved.value = true
  }
  pos.value = clampToViewport({
    x: startPos.value.x + dx,
    y: startPos.value.y + dy,
  })
}

function onMouseUp() {
  dragging.value = false
  document.removeEventListener('mousemove', onMouseMove)
  document.removeEventListener('mouseup', onMouseUp)
  if (!moved.value) {
    // 点击（非拖动）→ 切换面板
    chat.panelOpen.value = !chat.panelOpen.value
  }
  localStorage.setItem(STORAGE_KEY, JSON.stringify(pos.value))
}

// 触摸支持
function onTouchStart(e: TouchEvent) {
  const touch = e.touches[0]
  dragging.value = true
  moved.value = false
  startPos.value = { ...pos.value }
  startMouse.value = { x: touch.clientX, y: touch.clientY }
  document.addEventListener('touchmove', onTouchMove, { passive: false })
  document.addEventListener('touchend', onTouchEnd)
}

function onTouchMove(e: TouchEvent) {
  if (!dragging.value) return
  e.preventDefault()
  const touch = e.touches[0]
  const dx = touch.clientX - startMouse.value.x
  const dy = touch.clientY - startMouse.value.y
  if (Math.abs(dx) > 3 || Math.abs(dy) > 3) moved.value = true
  pos.value = clampToViewport({ x: startPos.value.x + dx, y: startPos.value.y + dy })
}

function onTouchEnd() {
  dragging.value = false
  document.removeEventListener('touchmove', onTouchMove)
  document.removeEventListener('touchend', onTouchEnd)
  if (!moved.value) {
    chat.panelOpen.value = !chat.panelOpen.value
  }
  localStorage.setItem(STORAGE_KEY, JSON.stringify(pos.value))
}

onUnmounted(() => {
  document.removeEventListener('mousemove', onMouseMove)
  document.removeEventListener('mouseup', onMouseUp)
  document.removeEventListener('touchmove', onTouchMove)
  document.removeEventListener('touchend', onTouchEnd)
})
</script>

<style scoped>
.terra-widget {
  position: fixed;
  z-index: 9999;
  cursor: grab;
  user-select: none;
  touch-action: none;
}
.terra-widget:active {
  cursor: grabbing;
}
.terra-orb {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: linear-gradient(135deg, #409EFF, #337ECC);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.4);
  transition: transform 0.2s, box-shadow 0.2s;
}
.terra-orb:hover {
  transform: scale(1.1);
  box-shadow: 0 6px 20px rgba(64, 158, 255, 0.5);
}
.terra-orb.active {
  background: linear-gradient(135deg, #67C23A, #529B2E);
}
.terra-orb-icon {
  color: white;
  font-size: 22px;
  font-weight: bold;
  font-family: 'Segoe UI', sans-serif;
}
</style>
```

- [ ] **步骤 3：验证编译并 Commit**

```bash
cd web && npx vue-tsc --noEmit 2>&1 | grep -i "terra" | head -10
git add web/src/components/terra/TerraToolExecutor.ts web/src/components/terra/TerraWidget.vue
git commit -m "feat(terra-fe): 前端工具执行器 + 悬浮球组件"
```

---

## 任务 4：TerraMessage 消息渲染

**文件：**
- 创建：`web/src/components/terra/TerraMessage.vue`

- [ ] **步骤 1：创建 TerraMessage.vue**

```vue
<template>
  <div class="terra-message" :class="message.role">
    <!-- 用户消息 -->
    <div v-if="message.role === 'user'" class="msg-bubble user-bubble">
      {{ message.content }}
    </div>

    <!-- Assistant 消息 -->
    <div v-else-if="message.role === 'assistant'" class="msg-bubble assistant-bubble">
      <!-- Markdown 渲染 -->
      <div
        v-if="message.content"
        class="markdown-body"
        v-html="renderedContent"
      ></div>

      <!-- 流式加载指示器 -->
      <div v-if="message.isStreaming && !message.content" class="typing-indicator">
        <span></span><span></span><span></span>
      </div>

      <!-- 工具调用列表 -->
      <div v-if="message.toolCalls && message.toolCalls.length" class="tool-calls">
        <div v-for="(tc, idx) in message.toolCalls" :key="idx" class="tool-call-item">
          <el-tag :type="tc.execSide === 'frontend' ? 'warning' : 'info'" size="small">
            {{ tc.execSide === 'frontend' ? '前端' : '后端' }}
          </el-tag>
          <span class="tool-name">{{ tc.tool }}</span>
        </div>
      </div>
    </div>

    <!-- 错误消息 -->
    <div v-else-if="message.isError" class="msg-bubble error-bubble">
      {{ message.content }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import type { ChatMessage } from './useTerraChat'

const props = defineProps<{
  message: ChatMessage
}>()

const renderedContent = computed(() => {
  if (!props.message.content) return ''
  const raw = marked.parse(props.message.content, { async: false }) as string
  return DOMPurify.sanitize(raw)
})
</script>

<style scoped>
.terra-message {
  margin-bottom: 12px;
  display: flex;
}
.terra-message.user {
  justify-content: flex-end;
}
.terra-message.assistant,
.terra-message.tool {
  justify-content: flex-start;
}

.msg-bubble {
  max-width: 85%;
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
}
.user-bubble {
  background: #409EFF;
  color: white;
  border-bottom-right-radius: 4px;
}
.assistant-bubble {
  background: rgba(255, 255, 255, 0.9);
  color: #303133;
  border-bottom-left-radius: 4px;
}
.error-bubble {
  background: #FEF0F0;
  color: #F56C6C;
  border: 1px solid #FBC4C4;
}

/* Markdown 样式 */
.markdown-body :deep(p) { margin: 0 0 8px 0; }
.markdown-body :deep(p:last-child) { margin-bottom: 0; }
.markdown-body :deep(pre) {
  background: #f5f7fa;
  padding: 8px 12px;
  border-radius: 6px;
  overflow-x: auto;
  margin: 8px 0;
}
.markdown-body :deep(code) {
  font-family: 'Consolas', monospace;
  font-size: 13px;
}
.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  padding-left: 20px;
  margin: 8px 0;
}
.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3) {
  margin: 12px 0 8px;
  font-size: 15px;
  font-weight: 600;
}
.markdown-body :deep(a) { color: #409EFF; }
.markdown-body :deep(table) {
  border-collapse: collapse;
  margin: 8px 0;
}
.markdown-body :deep(th),
.markdown-body :deep(td) {
  border: 1px solid #DCDFE6;
  padding: 4px 8px;
}

/* 工具调用样式 */
.tool-calls {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}
.tool-call-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
}
.tool-name {
  color: #909399;
}

/* 打字指示器 */
.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 4px 0;
}
.typing-indicator span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #C0C4CC;
  animation: typing 1.4s infinite ease-in-out;
}
.typing-indicator span:nth-child(2) { animation-delay: 0.2s; }
.typing-indicator span:nth-child(3) { animation-delay: 0.4s; }
@keyframes typing {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.4; }
  30% { transform: translateY(-6px); opacity: 1; }
}
</style>
```

- [ ] **步骤 2：验证编译并 Commit**

```bash
cd web && npx vue-tsc --noEmit 2>&1 | grep -i "terra" | head -10
git add web/src/components/terra/TerraMessage.vue
git commit -m "feat(terra-fe): 消息渲染组件（Markdown + 工具调用卡片）"
```

---

## 任务 5：TerraChatPanel 对话面板

**文件：**
- 创建：`web/src/components/terra/TerraChatPanel.vue`

- [ ] **步骤 1：创建 TerraChatPanel.vue**

```vue
<template>
  <Teleport to="body">
    <Transition name="terra-slide">
      <div v-if="chat.panelOpen.value" class="terra-chat-panel">
        <!-- 头部 -->
        <div class="panel-header">
          <div class="header-left">
            <el-select
              v-model="chat.currentConversationId.value"
              placeholder="新对话"
              size="small"
              filterable
              @change="onConversationChange"
              style="width: 180px"
            >
              <el-option
                v-for="conv in chat.conversations.value"
                :key="conv.id"
                :label="conv.title"
                :value="conv.id"
              />
            </el-select>
          </div>
          <div class="header-right">
            <el-button text size="small" @click="onNewConversation" title="新对话">
              <el-icon><Plus /></el-icon>
            </el-button>
            <el-button
              text
              size="small"
              @click="chat.panelOpen.value = false"
              title="关闭"
            >
              <el-icon><Close /></el-icon>
            </el-button>
          </div>
        </div>

        <!-- 消息列表 -->
        <div ref="messagesContainer" class="panel-messages">
          <div v-if="!chat.messages.value.length" class="empty-hint">
            <el-icon size="32" color="#C0C4CC"><ChatDotRound /></el-icon>
            <p>你好，我是 Terra，有什么可以帮你的吗？</p>
          </div>
          <TerraMessage
            v-for="(msg, idx) in chat.messages.value"
            :key="idx"
            :message="msg"
          />
        </div>

        <!-- 输入区域 -->
        <div class="panel-input">
          <el-input
            v-model="inputText"
            type="textarea"
            :rows="2"
            placeholder="输入消息..."
            resize="none"
            @keydown.enter.exact.prevent="onSend"
            :disabled="chat.isStreaming.value"
          />
          <div class="input-actions">
            <el-button
              v-if="chat.isStreaming.value"
              type="danger"
              size="small"
              circle
              @click="chat.stopStreaming()"
            >
              <el-icon><VideoPause /></el-icon>
            </el-button>
            <el-button
              v-else
              type="primary"
              size="small"
              circle
              @click="onSend"
              :disabled="!inputText.trim()"
            >
              <el-icon><Promotion /></el-icon>
            </el-button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, watch, nextTick } from 'vue'
import { Plus, Close, Promotion, VideoPause, ChatDotRound } from '@element-plus/icons-vue'
import { useTerraChat } from './useTerraChat'
import TerraMessage from './TerraMessage.vue'

const chat = useTerraChat()
const inputText = ref('')
const messagesContainer = ref<HTMLElement>()

function onSend() {
  const text = inputText.value.trim()
  if (!text || chat.isStreaming.value) return
  inputText.value = ''
  chat.sendMessage(text)
}

function onNewConversation() {
  chat.newConversation()
}

async function onConversationChange(id: number) {
  if (id) {
    await chat.selectConversation(id)
  }
}

// 自动滚动到底部
watch(
  () => chat.messages.value.length,
  () => {
    nextTick(() => {
      if (messagesContainer.value) {
        messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
      }
    })
  }
)

// 流式内容更新时也滚动
watch(
  () => chat.messages.value[chat.messages.value.length - 1]?.content,
  () => {
    nextTick(() => {
      if (messagesContainer.value) {
        messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
      }
    })
  }
)
</script>

<style scoped>
.terra-chat-panel {
  position: fixed;
  right: 24px;
  top: 80px;
  bottom: 80px;
  width: 380px;
  z-index: 9998;
  display: flex;
  flex-direction: column;
  background: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.4);
  overflow: hidden;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  border-bottom: 1px solid rgba(220, 223, 230, 0.5);
  flex-shrink: 0;
}
.header-left { flex: 1; }
.header-right { display: flex; gap: 4px; }

.panel-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}
.panel-messages::-webkit-scrollbar { width: 4px; }
.panel-messages::-webkit-scrollbar-thumb {
  background: rgba(192, 196, 204, 0.4);
  border-radius: 2px;
}

.empty-hint {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  gap: 12px;
  color: #909399;
  font-size: 14px;
}

.panel-input {
  padding: 10px 12px;
  border-top: 1px solid rgba(220, 223, 230, 0.5);
  flex-shrink: 0;
  display: flex;
  gap: 8px;
  align-items: flex-end;
}
.panel-input :deep(.el-textarea__inner) {
  background: rgba(255, 255, 255, 0.8);
  border-radius: 8px;
}
.input-actions {
  flex-shrink: 0;
}

/* 滑入/滑出动画 */
.terra-slide-enter-active,
.terra-slide-leave-active {
  transition: transform 0.3s ease, opacity 0.3s ease;
}
.terra-slide-enter-from,
.terra-slide-leave-to {
  transform: translateX(20px);
  opacity: 0;
}
</style>
```

- [ ] **步骤 2：验证编译并 Commit**

```bash
cd web && npx vue-tsc --noEmit 2>&1 | grep -i "terra" | head -10
git add web/src/components/terra/TerraChatPanel.vue
git commit -m "feat(terra-fe): 对话面板（透明悬浮 + 消息流 + 输入框）"
```

---

## 任务 6：Layout 集成

**文件：**
- 修改：`web/src/layout/index.vue`

- [ ] **步骤 1：在 Layout 中挂载 TerraWidget 和 TerraChatPanel**

在 `web/src/layout/index.vue` 中：

1. 在 `<script setup>` 部分添加导入（与其他 import 一起）：

```typescript
import TerraWidget from '@/components/terra/TerraWidget.vue'
import TerraChatPanel from '@/components/terra/TerraChatPanel.vue'
```

2. 在模板的根 `<div>` 内部、`</template>` 之前添加：

```html
    <!-- Terra 智能助手 -->
    <TerraWidget />
    <TerraChatPanel />
```

> **位置注意：** 放在 `<main>` 标签之后、根 `</div>` 之前。悬浮球和面板使用 `position: fixed`，不干扰正常布局流。

- [ ] **步骤 2：验证开发服务器**

```bash
cd web && npm run build 2>&1 | tail -20
```

预期：Build 成功（vue-tsc 类型检查 + Vite 构建）。

- [ ] **步骤 3：Commit**

```bash
git add web/src/layout/index.vue
git commit -m "feat(terra-fe): Layout 集成 — 挂载悬浮球 + 对话面板"
```

---

## 任务 7：设置页面 — 人格配置

**文件：**
- 创建：`web/src/views/terra/SettingsLayout.vue`
- 创建：`web/src/views/terra/PersonalitySettings.vue`

- [ ] **步骤 1：创建 SettingsLayout.vue**

```vue
<template>
  <div class="terra-settings">
    <div class="settings-sidebar">
      <h3 class="settings-title">Terra 设置</h3>
      <div
        v-for="tab in tabs"
        :key="tab.path"
        class="settings-tab"
        :class="{ active: currentTab === tab.path }"
        @click="$router.push(`/terra/settings/${tab.path}`)"
      >
        <el-icon><component :is="tab.icon" /></el-icon>
        <span>{{ tab.label }}</span>
      </div>
    </div>
    <div class="settings-content">
      <router-view />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { User, Setting, MagicStick, Tools } from '@element-plus/icons-vue'

const route = useRoute()
const tabs = [
  { path: 'personality', label: '人格配置', icon: User },
  { path: 'models', label: '模型配置', icon: Setting },
  { path: 'skills', label: '技能管理', icon: MagicStick },
  { path: 'tools', label: '工具管理', icon: Tools },
]

const currentTab = computed(() => {
  const seg = route.path.split('/').pop()
  return seg || 'personality'
})
</script>

<style scoped>
.terra-settings {
  display: flex;
  height: calc(100vh - 90px);
  background: #f5f7fa;
}
.settings-sidebar {
  width: 200px;
  background: white;
  border-right: 1px solid #e4e7ed;
  padding: 16px 0;
  flex-shrink: 0;
}
.settings-title {
  padding: 0 20px 16px;
  margin: 0;
  font-size: 16px;
  color: #303133;
  border-bottom: 1px solid #f0f0f0;
}
.settings-tab {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  cursor: pointer;
  color: #606266;
  font-size: 14px;
  transition: all 0.2s;
}
.settings-tab:hover {
  background: #f5f7fa;
  color: #409EFF;
}
.settings-tab.active {
  background: #ecf5ff;
  color: #409EFF;
  border-right: 3px solid #409EFF;
}
.settings-content {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
}
</style>
```

- [ ] **步骤 2：创建 PersonalitySettings.vue**

```vue
<template>
  <div class="personality-settings">
    <el-card v-loading="loading">
      <template #header>
        <span>人格配置</span>
      </template>

      <!-- 核心灵魂（只读展示） -->
      <div v-for="p in coreList" :key="p.id" class="personality-block">
        <div class="block-header">
          <el-tag type="danger" size="small">核心</el-tag>
          <span class="block-name">{{ p.name }}</span>
          <el-tag type="success" size="small">启用</el-tag>
        </div>
        <el-input
          type="textarea"
          :rows="8"
          :model-value="p.content"
          readonly
          resize="none"
        />
      </div>

      <!-- 角色层（可编辑） -->
      <div v-for="p in roleList" :key="p.id" class="personality-block">
        <div class="block-header">
          <el-tag type="info" size="small">角色</el-tag>
          <span class="block-name">{{ p.name }}</span>
          <el-switch
            :model-value="p.isActive === 1"
            @change="onToggle(p)"
            active-text="启用"
            inactive-text=""
            size="small"
          />
        </div>
        <el-input
          type="textarea"
          :rows="6"
          v-model="editCache[p.id]"
          resize="none"
          placeholder="输入角色定义内容..."
        />
        <div class="block-footer">
          <el-input v-model="p.name" size="small" style="width: 200px" placeholder="角色名称" />
          <el-button type="primary" size="small" @click="onSave(p)">保存</el-button>
        </div>
      </div>

      <!-- 添加新角色 -->
      <el-button type="primary" plain @click="addRole" :icon="Plus">
        添加角色层
      </el-button>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, reactive } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
  getPersonalities,
  updatePersonality,
  togglePersonality,
} from '@/api/terra'
import type { TerraPersonality } from '@/components/terra/types'

const loading = ref(false)
const list = ref<TerraPersonality[]>([])
const editCache = reactive<Record<number, string>>({})

const coreList = computed(() => list.value.filter(p => p.layerType === 'core'))
const roleList = computed(() => list.value.filter(p => p.layerType === 'role'))

async function loadData() {
  loading.value = true
  try {
    list.value = await getPersonalities()
    list.value.forEach(p => {
      editCache[p.id] = p.content
    })
  } catch (e: any) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

async function onSave(p: TerraPersonality) {
  try {
    p.content = editCache[p.id]
    await updatePersonality(p)
    ElMessage.success('保存成功')
  } catch (e: any) {
    ElMessage.error(e.message || '保存失败')
  }
}

async function onToggle(p: TerraPersonality) {
  try {
    await togglePersonality(p.id)
    p.isActive = p.isActive === 1 ? 0 : 1
    ElMessage.success(p.isActive === 1 ? '已启用' : '已停用')
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  }
}

function addRole() {
  const newRole: TerraPersonality = {
    id: Date.now(), // 临时 ID
    layerType: 'role',
    name: '新角色',
    content: '',
    isActive: 1,
    isPreset: 0,
    sortOrder: roleList.value.length,
    createTime: '',
  }
  list.value.push(newRole)
  editCache[newRole.id] = ''
}

onMounted(loadData)
</script>

<style scoped>
.personality-block {
  margin-bottom: 20px;
}
.block-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.block-name {
  font-weight: 500;
  flex: 1;
}
.block-footer {
  display: flex;
  justify-content: space-between;
  margin-top: 8px;
}
</style>
```

- [ ] **步骤 3：验证编译并 Commit**

```bash
cd web && npx vue-tsc --noEmit 2>&1 | grep -i "terra" | head -10
git add web/src/views/terra/
git commit -m "feat(terra-fe): 设置布局 + 人格配置页面"
```

---

## 任务 8：设置页面 — 模型配置

**文件：**
- 创建：`web/src/views/terra/ModelConfigList.vue`

- [ ] **步骤 1：创建 ModelConfigList.vue**

```vue
<template>
  <div class="model-config-list">
    <el-card v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>模型配置</span>
          <el-button type="primary" size="small" @click="openDialog()" :icon="Plus">新增</el-button>
        </div>
      </template>

      <el-table :data="list" stripe>
        <el-table-column prop="name" label="名称" width="140" />
        <el-table-column prop="baseUrl" label="Base URL" min-width="200" show-overflow-tooltip />
        <el-table-column prop="modelName" label="模型" width="180" />
        <el-table-column prop="maxTokens" label="Max Tokens" width="100" />
        <el-table-column prop="temperature" label="Temperature" width="100" />
        <el-table-column label="API Key" width="140">
          <template #default="{ row }">
            <span class="api-key-mask">{{ row.apiKey }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.isActive === 1" type="success" size="small">激活</el-tag>
            <el-tag v-else type="info" size="small">未激活</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button text size="small" type="primary" @click="openDialog(row)">编辑</el-button>
            <el-button
              v-if="row.isActive !== 1"
              text
              size="small"
              type="success"
              @click="onActivate(row)"
            >激活</el-button>
            <el-button text size="small" type="danger" @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑模型配置' : '新增模型配置'"
      width="500px"
    >
      <el-form :model="formData" label-width="100px">
        <el-form-item label="名称">
          <el-input v-model="formData.name" placeholder="如：生产环境" />
        </el-form-item>
        <el-form-item label="Base URL">
          <el-input v-model="formData.baseUrl" placeholder="https://api.anthropic.com" />
        </el-form-item>
        <el-form-item label="API Key">
          <el-input
            v-model="formData.apiKey"
            type="password"
            show-password
            :placeholder="editingId ? '不修改请留空' : '输入 API Key'"
          />
        </el-form-item>
        <el-form-item label="模型名称">
          <el-input v-model="formData.modelName" placeholder="claude-sonnet-4-20250514" />
        </el-form-item>
        <el-form-item label="Max Tokens">
          <el-input-number v-model="formData.maxTokens" :min="256" :max="32768" />
        </el-form-item>
        <el-form-item label="Temperature">
          <el-input-number v-model="formData.temperature" :min="0" :max="2" :step="0.1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="onSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getModelConfigs,
  createModelConfig,
  updateModelConfig,
  deleteModelConfig,
  activateModelConfig,
} from '@/api/terra'
import type { TerraModelConfig } from '@/components/terra/types'

const loading = ref(false)
const list = ref<TerraModelConfig[]>([])
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const formData = reactive<Partial<TerraModelConfig>>({
  name: '',
  baseUrl: 'https://api.anthropic.com',
  apiKey: '',
  modelName: 'claude-sonnet-4-20250514',
  maxTokens: 4096,
  temperature: 0.7,
})

async function loadData() {
  loading.value = true
  try {
    list.value = await getModelConfigs()
  } catch (e: any) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function openDialog(row?: TerraModelConfig) {
  if (row) {
    editingId.value = row.id
    Object.assign(formData, row)
    formData.apiKey = '' // 编辑时不回显密钥
  } else {
    editingId.value = null
    Object.assign(formData, {
      name: '',
      baseUrl: 'https://api.anthropic.com',
      apiKey: '',
      modelName: 'claude-sonnet-4-20250514',
      maxTokens: 4096,
      temperature: 0.7,
    })
  }
  dialogVisible.value = true
}

async function onSave() {
  try {
    if (editingId.value) {
      const data = { ...formData, id: editingId.value }
      // apiKey 为空时不传
      if (!data.apiKey) delete data.apiKey
      await updateModelConfig(data)
    } else {
      await createModelConfig(formData)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadData()
  } catch (e: any) {
    ElMessage.error(e.message || '保存失败')
  }
}

async function onActivate(row: TerraModelConfig) {
  try {
    await activateModelConfig(row.id)
    ElMessage.success('激活成功')
    loadData()
  } catch (e: any) {
    ElMessage.error(e.message || '激活失败')
  }
}

async function onDelete(row: TerraModelConfig) {
  try {
    await ElMessageBox.confirm(`确定删除「${row.name}」吗？`, '提示', { type: 'warning' })
    await deleteModelConfig(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch { /* cancel */ }
}

onMounted(loadData)
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.api-key-mask {
  font-family: monospace;
  color: #909399;
}
</style>
```

- [ ] **步骤 2：验证编译并 Commit**

```bash
cd web && npx vue-tsc --noEmit 2>&1 | grep -i "terra" | head -10
git add web/src/views/terra/ModelConfigList.vue
git commit -m "feat(terra-fe): 模型配置管理页面（CRUD + 激活）"
```

---

## 任务 9：设置页面 — 技能管理 + 工具管理

**文件：**
- 创建：`web/src/views/terra/SkillManager.vue`
- 创建：`web/src/views/terra/ToolManager.vue`

- [ ] **步骤 1：创建 SkillManager.vue**

```vue
<template>
  <div class="skill-manager">
    <el-card v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>技能管理</span>
          <el-tag type="info" size="small">共 {{ list.length }} 个</el-tag>
        </div>
      </template>

      <div class="skill-grid">
        <div v-for="skill in list" :key="skill.id" class="skill-card">
          <div class="skill-card-header">
            <el-icon size="20" color="#409EFF"><MagicStick /></el-icon>
            <span class="skill-name">{{ skill.displayName || skill.name }}</span>
            <el-tag v-if="skill.sourceType === 'preset'" type="warning" size="small">预置</el-tag>
          </div>
          <p class="skill-desc">{{ skill.description || '暂无描述' }}</p>
          <div class="skill-meta">
            <span v-if="skill.category" class="meta-item">
              <el-tag size="small" effect="plain">{{ skill.category }}</el-tag>
            </span>
            <span v-if="skill.version" class="meta-item">v{{ skill.version }}</span>
          </div>
          <div class="skill-actions">
            <el-switch
              :model-value="skill.isEnabled === 1"
              @change="onToggle(skill)"
              :disabled="skill.sourceType === 'preset'"
              active-text="启用"
              size="small"
            />
            <el-button
              v-if="skill.sourceType !== 'preset'"
              text
              size="small"
              type="danger"
              @click="onDelete(skill)"
            >卸载</el-button>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { MagicStick } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getSkills, deleteSkill, toggleSkill } from '@/api/terra'
import type { TerraSkill } from '@/components/terra/types'

const loading = ref(false)
const list = ref<TerraSkill[]>([])

async function loadData() {
  loading.value = true
  try {
    list.value = await getSkills()
  } catch (e: any) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

async function onToggle(skill: TerraSkill) {
  try {
    await toggleSkill(skill.id)
    skill.isEnabled = skill.isEnabled === 1 ? 0 : 1
    ElMessage.success(skill.isEnabled === 1 ? '已启用' : '已停用')
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  }
}

async function onDelete(skill: TerraSkill) {
  try {
    await ElMessageBox.confirm(`确定卸载「${skill.displayName || skill.name}」吗？`, '提示', { type: 'warning' })
    await deleteSkill(skill.id)
    ElMessage.success('卸载成功')
    loadData()
  } catch { /* cancel */ }
}

onMounted(loadData)
</script>

<style scoped>
.card-header { display: flex; align-items: center; gap: 8px; }
.skill-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}
.skill-card {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 14px;
  transition: box-shadow 0.2s;
}
.skill-card:hover { box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08); }
.skill-card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.skill-name { font-weight: 500; flex: 1; }
.skill-desc {
  color: #606266;
  font-size: 13px;
  margin: 0 0 8px;
  line-height: 1.5;
  min-height: 20px;
}
.skill-meta {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 12px;
  color: #909399;
}
.skill-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid #f5f7fa;
}
</style>
```

- [ ] **步骤 2：创建 ToolManager.vue**

```vue
<template>
  <div class="tool-manager">
    <el-card v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>工具管理</span>
          <el-button type="primary" size="small" @click="openDialog()" :icon="Plus">新增工具</el-button>
        </div>
      </template>

      <el-table :data="list" stripe>
        <el-table-column prop="toolKey" label="工具标识" width="160" />
        <el-table-column prop="name" label="名称" width="140" />
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column label="执行侧" width="80">
          <template #default="{ row }">
            <el-tag :type="row.execSide === 'frontend' ? 'warning' : 'info'" size="small">
              {{ row.execSide === 'frontend' ? '前端' : '后端' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="80">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.toolType === 'code' ? '代码' : '配置' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.isEnabled === 1 ? 'success' : 'info'" size="small">
              {{ row.isEnabled === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button text size="small" type="primary" @click="openDialog(row)">编辑</el-button>
            <el-button text size="small" @click="onToggle(row)">
              {{ row.isEnabled === 1 ? '停用' : '启用' }}
            </el-button>
            <el-button text size="small" type="danger" @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑工具' : '新增工具'"
      width="520px"
    >
      <el-form :model="formData" label-width="100px">
        <el-form-item label="工具标识">
          <el-input v-model="formData.toolKey" placeholder="如: query_device" :disabled="!!editingId" />
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="formData.name" placeholder="显示名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="formData.description" type="textarea" :rows="2" placeholder="工具描述（会发给 LLM）" />
        </el-form-item>
        <el-form-item label="执行侧">
          <el-radio-group v-model="formData.execSide">
            <el-radio value="backend">后端</el-radio>
            <el-radio value="frontend">前端</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="类型">
          <el-radio-group v-model="formData.toolType">
            <el-radio value="code">代码注册</el-radio>
            <el-radio value="config">配置(HTTP)</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="分类">
          <el-input v-model="formData.category" placeholder="如: device" />
        </el-form-item>
        <el-form-item v-if="formData.toolType === 'config'" label="配置 JSON">
          <el-input
            v-model="formData.config"
            type="textarea"
            :rows="4"
            placeholder='{"method":"GET","url":"http://...","headers":{}}'
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="onSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getTools,
  createTool,
  updateTool,
  deleteTool,
  toggleTool,
} from '@/api/terra'
import type { TerraTool } from '@/components/terra/types'

const loading = ref(false)
const list = ref<TerraTool[]>([])
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const formData = reactive<Partial<TerraTool>>({
  toolKey: '',
  name: '',
  description: '',
  execSide: 'backend',
  toolType: 'config',
  category: '',
  config: '',
})

async function loadData() {
  loading.value = true
  try {
    list.value = await getTools()
  } catch (e: any) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function openDialog(row?: TerraTool) {
  if (row) {
    editingId.value = row.id
    Object.assign(formData, row)
  } else {
    editingId.value = null
    Object.assign(formData, {
      toolKey: '', name: '', description: '',
      execSide: 'backend', toolType: 'config', category: '', config: '',
    })
  }
  dialogVisible.value = true
}

async function onSave() {
  try {
    if (editingId.value) {
      await updateTool({ ...formData, id: editingId.value })
    } else {
      await createTool(formData)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadData()
  } catch (e: any) {
    ElMessage.error(e.message || '保存失败')
  }
}

async function onToggle(row: TerraTool) {
  try {
    await toggleTool(row.id)
    row.isEnabled = row.isEnabled === 1 ? 0 : 1
    ElMessage.success(row.isEnabled === 1 ? '已启用' : '已停用')
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  }
}

async function onDelete(row: TerraTool) {
  try {
    await ElMessageBox.confirm(`确定删除工具「${row.name}」吗？`, '提示', { type: 'warning' })
    await deleteTool(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch { /* cancel */ }
}

onMounted(loadData)
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>
```

- [ ] **步骤 3：验证编译并 Commit**

```bash
cd web && npx vue-tsc --noEmit 2>&1 | grep -i "terra" | head -10
git add web/src/views/terra/SkillManager.vue web/src/views/terra/ToolManager.vue
git commit -m "feat(terra-fe): 技能管理 + 工具管理页面"
```

---

## 任务 10：路由配置 + 构建验证

**文件：**
- 修改：`web/src/router/index.ts`

- [ ] **步骤 1：添加 Terra 路由**

在 `web/src/router/index.ts` 中，找到 Layout 的 children 数组，添加 terra 路由：

```typescript
        // === Terra 智能助手 ===
        {
          path: '/terra/settings',
          component: () => import('@/views/terra/SettingsLayout.vue'),
          children: [
            {
              path: '',
              redirect: '/terra/settings/personality',
            },
            {
              path: 'personality',
              name: 'TerraPersonality',
              component: () => import('@/views/terra/PersonalitySettings.vue'),
            },
            {
              path: 'models',
              name: 'TerraModelConfigs',
              component: () => import('@/views/terra/ModelConfigList.vue'),
            },
            {
              path: 'skills',
              name: 'TerraSkills',
              component: () => import('@/views/terra/SkillManager.vue'),
            },
            {
              path: 'tools',
              name: 'TerraTools',
              component: () => import('@/views/terra/ToolManager.vue'),
            },
          ],
        },
```

> **位置：** 在 children 数组末尾（其他路由之后），确保与现有路由格式一致。

- [ ] **步骤 2：完整构建验证**

```bash
cd web && npm run build
```

预期：vue-tsc 类型检查通过 + Vite 构建成功，输出到 `dist/`。

如有类型错误：
1. 检查 `marked` 和 `dompurify` 是否有 TypeScript 类型定义（`@types/dompurify` 可能需要安装）
2. 检查 `AjaxResult` 类型是否在 `web/src/types` 中定义；如果不存在，在 `terra.ts` 中内联定义

- [ ] **步骤 3：Commit**

```bash
git add web/src/router/index.ts
git commit -m "feat(terra-fe): 路由配置 + 构建验证通过"
```

---

## 自检

### 规格覆盖度

| 规格章节 | 对应任务 | 状态 |
|---------|---------|------|
| 7.1 组件结构 — TerraWidget | 任务 3 | 覆盖 |
| 7.1 组件结构 — TerraChatPanel | 任务 5 | 覆盖 |
| 7.1 组件结构 — TerraMessage | 任务 4 | 覆盖 |
| 7.1 组件结构 — TerraToolExecutor | 任务 3 | 覆盖 |
| 7.1 组件结构 — terra-sse.ts | 任务 2 | 覆盖 |
| 7.1 组件结构 — api/terra.ts | 任务 1 | 覆盖 |
| 7.1 组件结构 — 设置页面 (4 个) | 任务 7-9 | 覆盖 |
| 7.2 悬浮球交互（拖动 + 点击 + localStorage） | 任务 3 | 覆盖 |
| 7.3 对话面板（透明 + 悬浮 + 会话切换） | 任务 5 | 覆盖 |
| 7.4 前端工具执行器 | 任务 3 | 覆盖 |
| 7.5 Layout 挂载 | 任务 6 | 覆盖 |
| 7.6 设置页面路由 | 任务 10 | 覆盖 |

### 修正项（已在计划中修复）

1. **Composable 单例问题**：状态变量已移至模块级（函数外部），确保 TerraWidget 和 TerraChatPanel 共享同一份状态。
2. **AjaxResult 类型**：已在 `terra.ts` 中内联定义 `AjaxResult<T>` 接口，不依赖外部类型模块。
3. **request.ts 响应格式**：已确认 `request.get/post/put/delete` 返回 `response.data`（即 AjaxResult），unwrap 直接检查 `.code`。

### 剩余注意事项

- `marked` v9+ 自带 TypeScript 类型，无需额外 `@types`。
- `dompurify` v3+ 自带 TypeScript 类型，无需额外 `@types`。
- 后端 SSE 端点 `POST /api/v1/terra/chat` 使用 `SseEmitter`，Vite dev server 需配置代理不缓冲 SSE（已有 notice/alarm SSE 先例，复用 Nginx/Vite 配置）。

---

## 执行交接

计划已完成并保存到 `docs/superpowers/plans/2026-06-27-terra-frontend.md`。两种执行方式：

1. **子代理驱动（推荐）** - 每个任务调度一个新的子代理，任务间进行审查，快速迭代
2. **内联执行** - 在当前会话中使用 executing-plans 执行任务，批量执行并设有检查点
