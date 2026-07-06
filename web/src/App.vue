<template>
  <el-config-provider :locale="zhCn" size="default" :z-index="3000" :message="{ max: 3 }">
    <div v-if="appError" class="global-error-fallback">
      <div class="global-error-card">
        <el-icon :size="48"><WarningFilled /></el-icon>
        <h3>页面异常</h3>
        <p>{{ appError }}</p>
        <el-button type="primary" @click="appError = null">关闭</el-button>
      </div>
    </div>
    <router-view v-else />
  </el-config-provider>
</template>

<script setup lang="ts">
import {onErrorCaptured, onMounted, onUnmounted, ref} from 'vue'
import {WarningFilled} from '@element-plus/icons-vue'
import zhCn from 'element-plus/es/locale/lang/zh-cn'

const appError = ref<string | null>(null)

onErrorCaptured((err, _instance, info) => {
  console.error('[Component Error]', err, `\n  source: ${info}`)
  appError.value = import.meta.env.DEV
    ? `页面发生错误: ${(err as Error).message || String(err)}`
    : '页面加载异常，请刷新重试'
  return false
})

// ── 全局：表格列宽拖拽调整 ──
onMounted(() => {
  document.addEventListener('mousedown', onTableColResize)
})

function onTableColResize(e: MouseEvent) {
  const target = e.target as HTMLElement
  if (target.tagName !== 'TH' || !target.classList.contains('el-table__cell')) return
  const rect = target.getBoundingClientRect()
  if (e.clientX < rect.right - 6 || e.clientX > rect.right + 2) return
  e.preventDefault()
  const startX = e.clientX
  const startW = target.offsetWidth
  const onMove = (ev: MouseEvent) => {
    const newW = Math.max(40, startW + (ev.clientX - startX))
    target.style.width = newW + 'px'
    target.style.minWidth = newW + 'px'
  }
  const onUp = () => { document.removeEventListener('mousemove', onMove); document.removeEventListener('mouseup', onUp) }
  document.addEventListener('mousemove', onMove)
  document.addEventListener('mouseup', onUp)
}

// ── 全局：el-dialog 全屏切换 ──
let dialogObserver: MutationObserver | null = null

function injectDialogFullscreenBtn(header: HTMLElement) {
  if (header.querySelector('.dialog-fs-btn')) return
  const btn = document.createElement('button')
  btn.className = 'dialog-fs-btn'
  btn.title = '最大化'
  btn.innerHTML = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14"><path d="M8 3H5a2 2 0 0 0-2 2v3m18 0V5a2 2 0 0 0-2-2h-3m0 18h3a2 2 0 0 0 2-2v-3M3 16v3a2 2 0 0 0 2 2h3"/></svg>'
  btn.addEventListener('click', (e) => {
    e.stopPropagation()
    const overlay = header.closest('.el-overlay-dialog') as HTMLElement
    if (!overlay) return
    overlay.classList.toggle('dialog-fullscreen')
    btn.title = overlay.classList.contains('dialog-fullscreen') ? '还原' : '最大化'
  })
  header.appendChild(btn)
}

onMounted(() => {
  dialogObserver = new MutationObserver(() => {
    document.querySelectorAll('.el-dialog__header:not([data-fs])').forEach(h => {
      h.setAttribute('data-fs', '1')
      injectDialogFullscreenBtn(h as HTMLElement)
    })
  })
  dialogObserver.observe(document.body, { childList: true, subtree: true })
})

onUnmounted(() => {
  dialogObserver?.disconnect()
  document.removeEventListener('mousedown', onTableColResize)
})
</script>

<style scoped>
.global-error-fallback {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: #f5f7fa;
}
.global-error-card {
  text-align: center;
  padding: 48px 64px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}
.global-error-card h3 {
  margin: 16px 0 8px;
  color: #303133;
}
.global-error-card p {
  margin: 0 0 24px;
  color: #909399;
  font-size: 14px;
  max-width: 400px;
}
</style>

<style>
/* 全局：el-table 列宽按内容自适应 */
.el-table { table-layout: auto !important; }

/* 全局：el-table 表头可拖拽调整列宽 */
.el-table__header th.el-table__cell {
  position: relative; overflow: visible;
}

.el-table__header th.el-table__cell::after {
  content: '';
  position: absolute; right: 0; top: 0; bottom: 0;
  width: 5px; cursor: col-resize; z-index: 1;
}

.el-table__header th.el-table__cell:hover::after {
  background: rgba(24, 144, 255, 0.15);
}

/* 全局：el-dialog 全屏按钮 */
.el-dialog__header { display: flex; align-items: center; }

.dialog-fs-btn {
  display: inline-flex; align-items: center; justify-content: center;
  width: 28px; height: 28px; margin-left: auto; margin-right: 4px;
  border: 1px solid #dcdfe6; border-radius: 4px;
  background: #fff; color: #606266; cursor: pointer;
  transition: all 0.2s; flex-shrink: 0;
}

.dialog-fs-btn:hover { border-color: #1890ff; color: #1890ff; background: #ecf5ff; }

/* 全局：el-dialog 全屏状态 */
.dialog-fullscreen .el-overlay-dialog { display: flex !important; align-items: stretch !important; }

.dialog-fullscreen .el-dialog {
  width: 100vw !important; height: 100vh !important;
  max-width: 100vw !important; margin: 0 !important;
  border-radius: 0 !important; display: flex; flex-direction: column;
}

.dialog-fullscreen .el-dialog__header {
  flex-shrink: 0; padding: 12px 20px;
  border-bottom: 1px solid #e5e7eb;
}

.dialog-fullscreen .el-dialog__body {
  flex: 1 1 0; overflow: auto; padding: 16px 20px; min-height: 0;
}

.dialog-fullscreen .el-dialog__footer {
  flex-shrink: 0; padding: 12px 20px;
  border-top: 1px solid #e5e7eb;
}
</style>
