<template>
  <!-- 打字阶段：显示纯文本带光标 -->
  <span v-if="isTyping" class="typewriter-message" :class="{ 'is-cursor': showCursor }">
    {{ displayText }}
  </span>
  <!-- 完成阶段：渲染 Markdown -->
  <span v-else class="markdown-content" v-html="renderedMarkdown"></span>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted, computed } from 'vue'
import { marked } from 'marked'

// 配置 marked
marked.use({
  breaks: true,
  gfm: true,
  headerIds: false,
  mangle: false
})

interface Props {
  text: string
  speed?: number  // 每个字符的打字间隔（毫秒）
  enabled?: boolean  // 是否启用打字机效果
  onComplete?: () => void  // 打字完成回调
  useMarkdown?: boolean  // 是否使用 Markdown 渲染
}

const props = withDefaults(defineProps<Props>(), {
  speed: 15,
  enabled: true,
  useMarkdown: true
})

const displayText = ref('')
const showCursor = ref(true)
const isTyping = ref(true)  // 是否正在打字
let targetText = ''
let currentIndex = 0
let typingInterval: number | null = null
let cursorInterval: number | null = null
let isComplete = false
let hasCalledOnComplete = false  // 确保回调只执行一次

/**
 * 渲染 Markdown
 */
const renderedMarkdown = computed(() => {
  if (!props.useMarkdown) return targetText
  return marked.parse(targetText) as string
})

/**
 * 继续打字（从当前位置开始）
 */
function continueTyping() {
  if (isComplete) return

  isTyping.value = true

  // 计算需要继续打的字符数
  const charsToAdd = targetText.length - displayText.value.length

  if (charsToAdd <= 0) {
    // 已经打完了
    if (typingInterval !== null) {
      clearInterval(typingInterval)
      typingInterval = null
    }
    finishTyping()
    return
  }

  // 继续打字
  typingInterval = window.setInterval(() => {
    if (currentIndex < targetText.length) {
      displayText.value += targetText[currentIndex]
      currentIndex++
    } else {
      // 打字完成
      if (typingInterval !== null) {
        clearInterval(typingInterval)
        typingInterval = null
      }
      finishTyping()
    }
  }, props.speed)
}

/**
 * 完成打字
 */
function finishTyping() {
  if (isComplete || hasCalledOnComplete) {
    return  // 已经完成或已调用过回调，不再执行
  }

  isComplete = true
  showCursor.value = false

  // 延迟一下再切换到 Markdown，让用户看到完整的文本
  setTimeout(() => {
    isTyping.value = false
    if (props.onComplete && !hasCalledOnComplete) {
      hasCalledOnComplete = true
      props.onComplete()
    }
  }, 100)
}

/**
 * 停止打字机效果
 */
function stopTyping() {
  if (typingInterval !== null) {
    clearInterval(typingInterval)
    typingInterval = null
  }
}

/**
 * 重置并开始新的打字
 */
function resetAndStart() {
  stopTyping()
  targetText = props.text
  currentIndex = 0
  displayText.value = ''
  isComplete = false
  showCursor.value = true
  isTyping.value = true
  hasCalledOnComplete = false  // 重置完成回调标志

  if (!props.enabled) {
    displayText.value = targetText
    isComplete = true
    showCursor.value = false
    isTyping.value = false
    hasCalledOnComplete = true
    return
  }

  continueTyping()
}

/**
 * 开始光标闪烁
 */
function startCursorBlink() {
  cursorInterval = window.setInterval(() => {
    if (!isComplete) {
      showCursor.value = !showCursor.value
    }
  }, 500)
}

/**
 * 停止光标闪烁
 */
function stopCursorBlink() {
  if (cursorInterval !== null) {
    clearInterval(cursorInterval)
    cursorInterval = null
  }
}

/**
 * 监听文本变化，继续打字
 */
watch(() => props.text, (newText) => {
  const oldLength = displayText.value.length
  const newLength = newText.length

  // 如果新文本比当前显示的短很多，重新开始
  if (newLength < oldLength - 50) {
    resetAndStart()
  } else if (newText !== displayText.value) {
    // 文本变化，更新目标并继续打字
    targetText = newText

    // 如果当前已经停止打字，重新开始
    if (typingInterval === null && !isComplete) {
      continueTyping()
    }
  }
}, { immediate: true })

/**
 * 监听启用状态变化
 */
watch(() => props.enabled, (enabled) => {
  if (!enabled) {
    stopTyping()
    displayText.value = targetText
    isComplete = true
    showCursor.value = false
    finishTyping()
  }
})

onMounted(() => {
  targetText = props.text
  if (props.enabled && props.text) {
    continueTyping()
  }
  startCursorBlink()
})

onUnmounted(() => {
  stopTyping()
  stopCursorBlink()
})

// 暴露方法给父组件
defineExpose({
  complete: () => {
    stopTyping()
    displayText.value = targetText
    isComplete = true
    showCursor.value = false
    isTyping.value = false
    hasCalledOnComplete = true  // 重置标志
    finishTyping()
  }
})
</script>

<style scoped lang="scss">
.typewriter-message {
  white-space: pre-wrap;  // 保留换行和空格
  word-wrap: break-word;

  &.is-cursor::after {
    content: '|';
    animation: blink 1s step-end infinite;
    margin-left: 2px;
    color: inherit;
  }
}

@keyframes blink {
  0%, 50% {
    opacity: 1;
  }
  51%, 100% {
    opacity: 0;
  }
}

// Markdown 内容样式（继承全局样式）
.markdown-content {
  :deep(p) {
    margin: 6px 0;
    line-height: 1.6;

    &:empty {
      display: none;
    }
  }

  :deep(pre) {
    background: rgba(0, 12, 28, 0.9);
    border: 1px solid rgba(0, 212, 255, 0.2);
    border-radius: 4px;
    padding: 12px;
    margin: 12px 0;
    overflow-x: auto;
    font-size: 11px;
    line-height: 1.5;

    code {
      background: none;
      padding: 0;
      font-family: 'Monaco', 'Menlo', 'Consolas', monospace;
      color: #a5d6ff;
    }
  }

  :deep(code) {
    background: rgba(0, 12, 28, 0.7);
    border: 1px solid rgba(0, 212, 255, 0.2);
    padding: 3px 8px;
    border-radius: 4px;
    font-family: 'Monaco', 'Menlo', 'Consolas', monospace;
    font-size: 11px;
    color: #7ee787;
  }

  :deep(strong) {
    font-weight: bold;
    color: #fff;
  }

  :deep(em) {
    font-style: italic;
  }

  :deep(ul), :deep(ol) {
    margin: 10px 0;
    padding-left: 24px;

    li {
      margin: 4px 0;
    }
  }

  :deep(h1), :deep(h2), :deep(h3), :deep(h4) {
    margin: 12px 0 8px 0;
    font-weight: bold;
  }

  :deep(h1) {
    font-size: 16px;
  }

  :deep(h2) {
    font-size: 14px;
  }

  :deep(h3) {
    font-size: 13px;
  }

  :deep(a) {
    color: #00d4ff;
    text-decoration: none;

    &:hover {
      text-decoration: underline;
    }
  }

  :deep(blockquote) {
    margin: 12px 0;
    padding-left: 12px;
    border-left: 3px solid #00d4ff;
    color: #ccc;
  }

  :deep(table) {
    width: 100%;
    border-collapse: collapse;
    margin: 12px 0;
    font-size: 12px;

    th, td {
      padding: 8px;
      border: 1px solid rgba(0, 212, 255, 0.2);
      text-align: left;
    }

    th {
      background: rgba(0, 212, 255, 0.1);
      font-weight: bold;
    }
  }

  :deep(hr) {
    border: none;
    border-top: 1px solid rgba(0, 212, 255, 0.2);
    margin: 16px 0;
  }
}
</style>
