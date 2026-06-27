<template>
  <div class="terra-message" :class="message.role">
    <div v-if="message.role === 'user'" class="msg-bubble user-bubble">
      {{ message.content }}
    </div>

    <div v-else-if="message.role === 'assistant'" class="msg-bubble assistant-bubble">
      <div
        v-if="message.content"
        class="markdown-body"
        v-html="renderedContent"
      ></div>

      <div v-if="message.isStreaming && !message.content" class="typing-indicator">
        <span></span><span></span><span></span>
      </div>

      <div v-if="message.toolCalls && message.toolCalls.length" class="tool-calls">
        <div v-for="(tc, idx) in message.toolCalls" :key="idx" class="tool-call-item">
          <el-tag :type="tc.execSide === 'frontend' ? 'warning' : 'info'" size="small">
            {{ tc.execSide === 'frontend' ? '前端' : '后端' }}
          </el-tag>
          <span class="tool-name">{{ tc.tool }}</span>
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
import type { ChatMessage } from './useTerraChat'

const props = defineProps<{ message: ChatMessage }>()

const renderedContent = computed(() => {
  if (!props.message.content) return ''
  const raw = marked.parse(props.message.content, { async: false }) as string
  return DOMPurify.sanitize(raw)
})
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

.markdown-body :deep(p) { margin: 0 0 8px 0; }
.markdown-body :deep(p:last-child) { margin-bottom: 0; }
.markdown-body :deep(pre) { background: #f5f7fa; padding: 8px 12px; border-radius: 6px; overflow-x: auto; margin: 8px 0; }
.markdown-body :deep(code) { font-family: 'Consolas', monospace; font-size: 13px; }
.markdown-body :deep(ul), .markdown-body :deep(ol) { padding-left: 20px; margin: 8px 0; }
.markdown-body :deep(h1), .markdown-body :deep(h2), .markdown-body :deep(h3) { margin: 12px 0 8px; font-size: 15px; font-weight: 600; }
.markdown-body :deep(a) { color: #409EFF; }
.markdown-body :deep(table) { border-collapse: collapse; margin: 8px 0; }
.markdown-body :deep(th), .markdown-body :deep(td) { border: 1px solid #DCDFE6; padding: 4px 8px; }

.tool-calls { margin-top: 8px; display: flex; flex-wrap: wrap; gap: 4px; }
.tool-call-item { display: flex; align-items: center; gap: 4px; font-size: 12px; }
.tool-name { color: #909399; }

.typing-indicator { display: flex; gap: 4px; padding: 4px 0; }
.typing-indicator span { width: 8px; height: 8px; border-radius: 50%; background: #C0C4CC; animation: typing 1.4s infinite ease-in-out; }
.typing-indicator span:nth-child(2) { animation-delay: 0.2s; }
.typing-indicator span:nth-child(3) { animation-delay: 0.4s; }
@keyframes typing {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.4; }
  30% { transform: translateY(-6px); opacity: 1; }
}
</style>
