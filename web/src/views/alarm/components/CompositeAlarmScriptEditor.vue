<template>
  <div class="composite-editor">
    <el-alert
      v-if="statusBar"
      :type="statusBar.type"
      :closable="false"
      class="status-bar"
      data-test="status-bar"
    >
      <template #title>{{ statusBar.text }}</template>
    </el-alert>

    <div class="editor-area" ref="editorAreaRef">
      <div class="editor-main">
        <div class="editor-tag">Groovy</div>
        <CodeMirrorGroovy
          :model-value="localScript"
          @update:model-value="onScriptChange"
          class="cm-wrapper"
        />
      </div>
      <div class="editor-resizer" @mousedown="startResize" />
      <div class="editor-side" :style="{ width: sidebarWidth + 'px' }">
        <ApiDocsSidebar mode="alarm" />
      </div>
    </div>

    <TestPanel
      mode="alarm"
      :result="testResult as any"
      :testing="testing"
      @run-test="onRunTest"
    />

    <div class="editor-footer">
      <el-button data-test="reset-btn" @click="onReset">重置为模板</el-button>
      <el-button
        type="primary"
        :disabled="!canSave"
        :class="{ 'save-ready': canSave && dirty }"
        data-test="save-btn"
        :loading="saving"
        @click="onSave"
      >保存</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import { getStrategyDetail, updateStrategy, testStrategyRun, type StrategyTestRunResult } from '@/api/alarm'
import CodeMirrorGroovy from '../../basic/components/script-editor/CodeMirrorGroovy.vue'
import ApiDocsSidebar from '../../basic/components/script-editor/ApiDocsSidebar.vue'
import TestPanel from '../../basic/components/script-editor/TestPanel.vue'

const props = defineProps<{
  alarmId: number
  triggerMode: 'PERIODIC' | 'REALTIME'
}>()

const emit = defineEmits<{
  saved: []
}>()

const initialScript = ref('')
const localScript = ref('')
const testedPassed = ref(false)
const testing = ref(false)
const saving = ref(false)
const testResult = ref<StrategyTestRunResult | null>(null)

const dirty = computed(() => localScript.value !== initialScript.value)
const canSave = computed(() => !dirty.value || testedPassed.value)

const statusBar = computed(() => {
  if (!dirty.value) return null
  if (testedPassed.value) return { type: 'success' as const, text: '✅ 测试通过, 可以保存' }
  if (testResult.value && testResult.value.error)
    return { type: 'error' as const, text: `❌ 测试失败: ${testResult.value.error}` }
  return { type: 'warning' as const, text: '⚠️ 修改后必须通过测试才能保存' }
})

const defaultTemplate = computed(() =>
  '// 综合告警脚本 — 返回 1-4 表示告警等级 (red/orange/yellow/blue), 0 或 null = 无告警\n' +
  '// 可用变量:\n' +
  '//   hazardPointIds  绑定的隐患点 ID 列表 (List<Long>)\n' +
  '//   currentTime     当前时间 (String, yyyy-MM-dd HH:mm:ss)\n' +
  '// 工具:\n' +
  '//   cache.getInt(key, default)   Redis 读取\n' +
  '//   sensor.query(deviceCode, sensorCode, time, attrCode)  IoTDB 查询\n' +
  '//   sensor.listDeviceSensorsByHazardPoint(hpId)  查询隐患点设备列表\n' +
  '//   algo.executeLatest(algoCode, method, params)  调用Python算法\n' +
  '//   log.info(msg) / log.warn(msg)  日志记录\n\n' +
  'def level = 0\n\n' +
  '// 遍历隐患点设备示例:\n' +
  '// for (hpId in hazardPointIds) {\n' +
  '//     def devices = sensor.listDeviceSensorsByHazardPoint(hpId)\n' +
  '//     for (d in devices) {\n' +
  '//         // sensor.query(d.deviceCode, d.sensorCode, ...)\n' +
  '//         // algo.executeLatest("GJQXJ", "calc_speed", [...])\n' +
  '//     }\n' +
  '// }\n\n' +
  'return level\n'
)

watch(() => props.alarmId, async (id) => {
  if (!id) return
  try {
    const detail = await getStrategyDetail(id)
    initialScript.value = detail.scriptContent || ''
    localScript.value = detail.scriptContent || defaultTemplate.value
  } catch {
    localScript.value = defaultTemplate.value
    initialScript.value = ''
  }
  testedPassed.value = false
  testResult.value = null
}, { immediate: true })

function onScriptChange(newVal: string) {
  localScript.value = newVal
  testedPassed.value = false
}

function onReset() {
  localScript.value = defaultTemplate.value
  testedPassed.value = false
  testResult.value = null
}

async function onRunTest(_payload: Record<string, any>) {
  if (!props.alarmId) {
    ElMessage.warning('请先保存策略, 再测试脚本')
    return
  }
  testing.value = true
  try {
    const result = await testStrategyRun(props.alarmId)
    testResult.value = result
    testedPassed.value = result.error == null
  } catch (e: any) {
    testResult.value = {
      level: null, levelText: null, durationMs: 0,
      error: e?.message || '请求失败'
    }
    testedPassed.value = false
  } finally {
    testing.value = false
  }
}

async function onSave() {
  if (!canSave.value) return
  if (!localScript.value.trim()) {
    ElMessage.warning('脚本不能为空')
    return
  }
  saving.value = true
  try {
    await updateStrategy(props.alarmId, { scriptContent: localScript.value } as any)
    initialScript.value = localScript.value
    ElMessage.success('脚本已保存')
    emit('saved')
  } catch (e: any) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// ── 侧栏拖拽调整宽度 ──
const editorAreaRef = ref<HTMLElement>()
const sidebarWidth = ref(240)
let resizing = false

function startResize(e: MouseEvent) {
  e.preventDefault()
  resizing = true
  document.body.style.cursor = 'col-resize'
  document.body.style.userSelect = 'none'
  window.addEventListener('mousemove', onMouseMove)
  window.addEventListener('mouseup', stopResize)
}

function onMouseMove(e: MouseEvent) {
  if (!resizing || !editorAreaRef.value) return
  const rect = editorAreaRef.value.getBoundingClientRect()
  const newWidth = rect.right - e.clientX
  sidebarWidth.value = Math.max(160, Math.min(500, newWidth))
}

function stopResize() {
  resizing = false
  document.body.style.cursor = ''
  document.body.style.userSelect = ''
  window.removeEventListener('mousemove', onMouseMove)
  window.removeEventListener('mouseup', stopResize)
}

onBeforeUnmount(stopResize)
</script>

<style scoped>
.composite-editor {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.status-bar {
  margin-bottom: 12px;
  flex-shrink: 0;
}

.editor-area {
  display: flex;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  overflow: hidden;
  flex: 1;
  min-height: 0;
}

.editor-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  position: relative;
}

.editor-tag {
  position: absolute;
  top: 6px;
  right: 8px;
  z-index: 2;
  background: #264f78;
  color: white;
  padding: 1px 6px;
  font-size: 10px;
  border-radius: 2px;
  font-family: 'Consolas', monospace;
}

.cm-wrapper {
  flex: 1;
  overflow: hidden;
}

.editor-resizer {
  flex-shrink: 0;
  width: 4px;
  cursor: col-resize;
  background: #ebeef5;
  transition: background 0.15s;
}

.editor-resizer:hover {
  background: #c0ccda;
}

.editor-side {
  flex-shrink: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.editor-side :deep(.api-docs-sidebar) {
  width: 100%;
  flex: 1;
  min-height: 0;
}

.editor-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 12px 0;
  flex-shrink: 0;
}

.save-ready {
  background: #67c23a !important;
  border-color: #67c23a !important;
}
</style>

<style>
.el-drawer__body {
  display: flex;
  flex-direction: column;
  padding: 16px;
}
</style>
