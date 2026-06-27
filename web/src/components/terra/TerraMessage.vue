<template>
  <div class="terra-message" :class="message.role">
    <div v-if="message.role === 'user'" class="msg-bubble user-bubble">
      {{ message.content }}
    </div>

    <div v-else-if="message.role === 'assistant'" class="msg-bubble assistant-bubble">
      <!-- 思考状态 -->
      <div v-if="message.isStreaming && !message.content" class="thinking-state">
        <div class="thinking-icon">
          <span class="pulse-ring"></span>
          <span class="pulse-core"></span>
        </div>
        <span class="thinking-text">{{ thinkingText }}</span>
      </div>

      <!-- 工具调用状态（流式输出前的工具调用） -->
      <div
        v-if="message.isStreaming && message.toolCalls?.length && !message.content"
        class="tool-status"
      >
        <div v-for="(tc, idx) in message.toolCalls" :key="idx" class="tool-status-item">
          <el-icon class="tool-spinner"><Loading /></el-icon>
          <span class="tool-label">{{ toolDisplayName(tc.tool) }}</span>
        </div>
      </div>

      <!-- 消息内容 -->
      <div
        v-if="message.content"
        class="markdown-body"
        v-html="renderedContent"
        @click="handleClick"
      ></div>

      <!-- 流式输出时的光标 -->
      <span v-if="message.isStreaming && message.content" class="streaming-cursor"></span>

      <!-- 工具调用标签（有内容后显示在底部） -->
      <div v-if="message.toolCalls?.length && message.content && !message.isStreaming" class="tool-calls">
        <div v-for="(tc, idx) in message.toolCalls" :key="idx" class="tool-call-item">
          <el-tag :type="tc.execSide === 'frontend' ? 'warning' : 'info'" size="small">
            {{ tc.execSide === 'frontend' ? '前端' : '后端' }}
          </el-tag>
          <span class="tool-name">{{ toolDisplayName(tc.tool) }}</span>
        </div>
      </div>
    </div>

    <div v-else-if="message.isError" class="msg-bubble error-bubble">
      {{ message.content }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import { Loading } from '@element-plus/icons-vue'
import type { ChatMessage } from './useTerraChat'

const props = defineProps<{ message: ChatMessage }>()
const emit = defineEmits<{ navigate: [routeName: string, query?: Record<string, string>] }>()

/** 思考状态文案 */
const thinkingText = computed(() => {
  if (props.message.toolCalls?.length) return '正在查询系统数据…'
  return 'Terra 正在思考…'
})

/** 工具友好名称映射 */
const toolDisplayName = (toolKey: string): string => {
  const names: Record<string, string> = {
    'system.query.overview': '系统总览',
    'system.query.deviceStat': '设备统计',
    'system.query.deviceList': '设备查询',
    'system.query.hazardPointStat': '隐患点统计',
    'system.query.hazardPointList': '隐患点查询',
    'system.query.alarmStat': '告警统计',
    'system.query.sensorList': '传感器查询',
  }
  return names[toolKey] || toolKey
}

/** 渲染 markdown + 导航链接处理 */
const renderedContent = computed(() => {
  if (!props.message.content) return ''
  const raw = marked.parse(props.message.content, { async: false }) as string
  let html = DOMPurify.sanitize(raw)
  // 给 #page:xxx 或 #page:xxx?key=val 链接添加 data-navigate 属性和样式
  html = html.replace(
    /href="#page:([^"?]+)(\?[^"]*)?"/g,
    (_match, routeName: string, queryStr?: string) => {
      const q = queryStr ? queryStr.slice(1) : ''
      return `href="#page:${routeName}${queryStr || ''}" data-navigate="${routeName}" data-query="${q}" class="nav-link"`
    }
  )
  return html
})

/** 点击事件委托 — 处理导航链接 */
function handleClick(e: MouseEvent) {
  const target = (e.target as HTMLElement)?.closest('a[data-navigate]')
  if (!target) return
  e.preventDefault()
  const routeName = target.getAttribute('data-navigate')
  const queryStr = target.getAttribute('data-query') || ''
  if (routeName) {
    const query: Record<string, string> = {}
    if (queryStr) {
      new URLSearchParams(queryStr).forEach((v, k) => { query[k] = v })
    }
    emit('navigate', routeName, Object.keys(query).length ? query : undefined)
  }
}
</script>

<style scoped>
.terra-message { margin-bottom: 12px; display: flex; }
.terra-message.user { justify-content: flex-end; }
.terra-message.assistant, .terra-message.tool { justify-content: flex-start; }

.msg-bubble {
  max-width: 85%;
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
}
.user-bubble { background: #409EFF; color: white; border-bottom-right-radius: 4px; }
.assistant-bubble { background: rgba(255, 255, 255, 0.9); color: #303133; border-bottom-left-radius: 4px; }
.error-bubble { background: #FEF0F0; color: #F56C6C; border: 1px solid #FBC4C4; }

/* ---- 思考状态 ---- */
.thinking-state {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
}
.thinking-icon {
  position: relative;
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.pulse-core {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: linear-gradient(135deg, #409EFF, #7B68EE);
  animation: pulse-core 1.8s ease-in-out infinite;
}
.pulse-ring {
  position: absolute;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  border: 2px solid #409EFF;
  animation: pulse-ring 1.8s ease-out infinite;
}
@keyframes pulse-core {
  0%, 100% { transform: scale(0.8); opacity: 0.8; }
  50% { transform: scale(1.1); opacity: 1; }
}
@keyframes pulse-ring {
  0% { transform: scale(0.6); opacity: 0.8; }
  100% { transform: scale(1.4); opacity: 0; }
}
.thinking-text {
  color: #909399;
  font-size: 13px;
  animation: thinking-fade 2s ease-in-out infinite;
}
@keyframes thinking-fade {
  0%, 100% { opacity: 0.6; }
  50% { opacity: 1; }
}

/* ---- 工具状态 ---- */
.tool-status {
  margin-top: 6px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.tool-status-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #909399;
}
.tool-spinner {
  animation: spin 1s linear infinite;
  font-size: 14px;
  color: #409EFF;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}
.tool-label {
  font-weight: 500;
}

/* ---- 流式光标 ---- */
.streaming-cursor {
  display: inline-block;
  width: 2px;
  height: 16px;
  background: #409EFF;
  margin-left: 2px;
  vertical-align: text-bottom;
  animation: cursor-blink 1s step-end infinite;
}
@keyframes cursor-blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}

/* ---- Markdown ---- */
.markdown-body :deep(p) { margin: 0 0 8px 0; }
.markdown-body :deep(p:last-child) { margin-bottom: 0; }
.markdown-body :deep(pre) { background: #f5f7fa; padding: 8px 12px; border-radius: 6px; overflow-x: auto; margin: 8px 0; }
.markdown-body :deep(code) { font-family: 'Consolas', monospace; font-size: 13px; }
.markdown-body :deep(ul), .markdown-body :deep(ol) { padding-left: 20px; margin: 8px 0; }
.markdown-body :deep(h1), .markdown-body :deep(h2), .markdown-body :deep(h3) { margin: 12px 0 8px; font-size: 15px; font-weight: 600; }
.markdown-body :deep(a) { color: #409EFF; }
.markdown-body :deep(table) { border-collapse: collapse; margin: 8px 0; }
.markdown-body :deep(th), .markdown-body :deep(td) { border: 1px solid #DCDFE6; padding: 4px 8px; }

/* 导航链接特殊样式 */
.markdown-body :deep(.nav-link) {
  color: #409EFF !important;
  text-decoration: none;
  border-bottom: 1px dashed #409EFF;
  cursor: pointer;
}
.markdown-body :deep(.nav-link:hover) {
  background: rgba(64, 158, 255, 0.1);
  border-radius: 3px;
}

/* ---- 工具调用标签 ---- */
.tool-calls { margin-top: 8px; display: flex; flex-wrap: wrap; gap: 4px; }
.tool-call-item { display: flex; align-items: center; gap: 4px; font-size: 12px; }
.tool-name { color: #909399; }
</style>
