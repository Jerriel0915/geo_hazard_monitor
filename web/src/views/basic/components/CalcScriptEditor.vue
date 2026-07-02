<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    :title="`计算脚本 - ${attrName}`"
    width="900px"
    :close-on-click-modal="false"
    destroy-on-close
  >
    <el-alert
      v-if="statusBar"
      :type="statusBar.type"
      :closable="false"
      class="status-bar"
      data-test="status-bar"
    >
      <template #title>
        <el-icon v-if="statusBar.icon" class="status-icon"><component :is="statusBar.icon" /></el-icon>
        {{ statusBar.text }}
      </template>
    </el-alert>

    <div class="editor-area">
      <div class="editor-main">
        <div class="editor-tag">Groovy</div>
        <div class="editor-toolbar">
          <el-button size="small" text type="primary" @click="handleFormatGroovy">格式化</el-button>
          <el-button size="small" text @click="editorTheme = editorTheme === 'dark' ? 'light' : 'dark'">
            <el-icon><Sunny v-if="editorTheme === 'dark'" /><Moon v-else /></el-icon>
            {{ editorTheme === 'dark' ? '亮色' : '暗色' }}
          </el-button>
        </div>
        <CodeMirrorGroovy
          :model-value="localScript"
          @update:model-value="onScriptChange"
          :theme="editorTheme"
          class="cm-wrapper"
        />
      </div>
      <ApiDocsSidebar class="editor-side" />
    </div>

    <TestPanel
      :result="testResult"
      :testing="testing"
      @run-test="onRunTest"
    />

    <template #footer>
      <el-button data-test="cancel-btn" @click="$emit('update:modelValue', false)">取消</el-button>
      <el-button data-test="reset-btn" @click="onReset">重置为模板</el-button>
      <el-button
        type="primary"
        :disabled="!canSave"
        :class="{ 'save-ready': canSave && dirty }"
        data-test="save-btn"
        @click="onSave"
      >保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch, markRaw } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Sunny, Moon, SuccessFilled, CircleCloseFilled, WarningFilled
} from '@element-plus/icons-vue'
import { testCalcScript, type CalcScriptTestResult } from '@/api/monitorType'
import { formatGroovyCode } from '@/utils/groovyFormat'
import CodeMirrorGroovy from './script-editor/CodeMirrorGroovy.vue'
import ApiDocsSidebar from './script-editor/ApiDocsSidebar.vue'
import TestPanel from './script-editor/TestPanel.vue'

const props = defineProps<{
  modelValue: boolean
  attrCode: string
  attrName: string
  unit?: string
  script: string
  monitorTypeId: number
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  save: [script: string]
}>()

const initialScript = ref(props.script)
const localScript = ref(props.script)
const testedPassed = ref(false)
const testing = ref(false)
const testResult = ref<CalcScriptTestResult | null>(null)
const editorTheme = ref<'light' | 'dark'>('dark')

const dirty = computed(() => localScript.value !== initialScript.value)
const canSave = computed(() => !dirty.value || testedPassed.value)

const statusBar = computed(() => {
  if (!dirty.value) {
    return null
  }
  if (testedPassed.value) {
    return { type: 'success' as const, icon: markRaw(SuccessFilled), text: '测试通过, 可以保存' }
  }
  if (testResult.value && !testResult.value.success) {
    return { type: 'error' as const, icon: markRaw(CircleCloseFilled), text: `测试失败: ${testResult.value.error || '未知错误'}` }
  }
  return { type: 'warning' as const, icon: markRaw(WarningFilled), text: '修改后必须通过测试才能保存' }
})

const defaultTemplate = computed(() =>
  `// 计算属性: ${props.attrCode}\n` +
  '// 可用变量:\n' +
  `//   curData?.props?.${props.attrCode}  当前数据包属性值\n` +
  `//   prevData?.props?.${props.attrCode}  上一条数据包属性值(可空)\n` +
  '// 工具:\n' +
  '//   cache.getInt(key, default)  Redis 读取 (异常吞噬)\n' +
  '//   sensor.query(deviceCode, sensorCode, time, attrCode)  IoTDB 查询 (异常返回 null)\n' +
  '// 返回: 数值 (Number)\n\n' +
  `return curData?.props?.${props.attrCode}\n`
)

watch(() => props.modelValue, (open) => {
  if (open) {
    initialScript.value = props.script
    localScript.value = props.script || defaultTemplate.value
    testedPassed.value = false
    testResult.value = null
  }
})

function onScriptChange(newVal: string) {
  localScript.value = newVal
  testedPassed.value = false
}

function onReset() {
  localScript.value = defaultTemplate.value
  testedPassed.value = false
  testResult.value = null
}

function handleFormatGroovy() {
  if (!localScript.value.trim()) {
    ElMessage.warning('无代码可格式化')
    return
  }
  const formatted = formatGroovyCode(localScript.value)
  if (formatted === localScript.value) return
  localScript.value = formatted
  testedPassed.value = false
  ElMessage.success('格式化完成')
}

async function onRunTest(payload: { curData: Record<string, any>; prevData: Record<string, any> | undefined }) {
  if (!localScript.value.trim()) {
    ElMessage.warning('脚本不能为空，无法测试')
    return
  }
  testing.value = true
  try {
    const result = await testCalcScript({
      monitorTypeId: props.monitorTypeId || 0,
      attrCode: props.attrCode,
      calcScript: localScript.value,
      curData: payload.curData,
      prevData: payload.prevData
    })
    testResult.value = result
    testedPassed.value = result.success === true
  } catch (e: any) {
    testResult.value = { success: false, error: e?.message || '请求失败' }
    testedPassed.value = false
  } finally {
    testing.value = false
  }
}

function onSave() {
  if (!canSave.value) return
  if (!localScript.value.trim()) {
    ElMessage.warning('脚本不能为空')
    return
  }
  emit('save', localScript.value)
}
</script>

<style scoped>
.status-bar {
  margin-bottom: 12px;
}

.status-icon {
  vertical-align: middle;
  margin-right: 4px;
}

.editor-area {
  display: flex;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  overflow: hidden;
  height: 320px;
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

.editor-toolbar {
  display: flex;
  justify-content: flex-end;
  gap: 4px;
  padding: 2px 4px;
}

.cm-wrapper {
  flex: 1;
  overflow: hidden;
}

.editor-side {
  flex-shrink: 0;
}

.save-ready {
  background: #67c23a !important;
  border-color: #67c23a !important;
}
</style>
