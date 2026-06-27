<template>
  <Teleport to="body">
    <Transition name="terra-slide">
      <div v-if="chat.panelOpen.value" class="terra-chat-panel">
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
            <el-button text size="small" @click="chat.panelOpen.value = false" title="关闭">
              <el-icon><Close /></el-icon>
            </el-button>
          </div>
        </div>

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
  if (id) await chat.selectConversation(id)
}

watch(() => chat.messages.value.length, () => {
  nextTick(() => {
    if (messagesContainer.value) messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  })
})

watch(() => chat.messages.value[chat.messages.value.length - 1]?.content, () => {
  nextTick(() => {
    if (messagesContainer.value) messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  })
})
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

.panel-messages { flex: 1; overflow-y: auto; padding: 16px; }
.panel-messages::-webkit-scrollbar { width: 4px; }
.panel-messages::-webkit-scrollbar-thumb { background: rgba(192, 196, 204, 0.4); border-radius: 2px; }

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
.panel-input :deep(.el-textarea__inner) { background: rgba(255, 255, 255, 0.8); border-radius: 8px; }
.input-actions { flex-shrink: 0; }

.terra-slide-enter-active, .terra-slide-leave-active { transition: transform 0.3s ease, opacity 0.3s ease; }
.terra-slide-enter-from, .terra-slide-leave-to { transform: translateX(20px); opacity: 0; }
</style>
