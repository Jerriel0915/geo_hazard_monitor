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

onMounted(() => {
  const saved = localStorage.getItem(STORAGE_KEY)
  if (saved) {
    try {
      pos.value = clampToViewport(JSON.parse(saved))
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
  if (Math.abs(dx) > 3 || Math.abs(dy) > 3) moved.value = true
  pos.value = clampToViewport({ x: startPos.value.x + dx, y: startPos.value.y + dy })
}

function onMouseUp() {
  dragging.value = false
  document.removeEventListener('mousemove', onMouseMove)
  document.removeEventListener('mouseup', onMouseUp)
  if (!moved.value) chat.panelOpen.value = !chat.panelOpen.value
  localStorage.setItem(STORAGE_KEY, JSON.stringify(pos.value))
}

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
  if (!moved.value) chat.panelOpen.value = !chat.panelOpen.value
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
.terra-widget:active { cursor: grabbing; }
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
