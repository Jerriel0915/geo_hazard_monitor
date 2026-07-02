<template>
  <div class="test-panel">
    <div class="panel-header">
      <span class="panel-title">
        <el-icon><VideoPlay /></el-icon>
        <span>在线测试</span>
      </span>
      <el-button
        class="theme-toggle"
        size="small"
        text
        @click="editorTheme = editorTheme === 'dark' ? 'light' : 'dark'"
      >
        <el-icon><Sunny v-if="editorTheme === 'dark'" /><Moon v-else /></el-icon>
        {{ editorTheme === 'dark' ? '亮色' : '暗色' }}
      </el-button>
    </div>

    <div class="panel-body">
      <!-- calc 模式: curData + prevData -->
      <div v-if="mode === 'calc'" class="form-row">
        <div class="row-header">
          <label class="row-label">curData</label>
          <el-button size="small" text type="primary" @click="formatJson('curData')">格式化</el-button>
        </div>
        <div class="cm-row-wrapper">
          <CodeMirrorJson
            v-model="curDataJson"
            :theme="editorTheme"
            :min-height="80"
            data-test="cur-data-input"
            @blur="formatJson('curData', true)"
          />
        </div>
      </div>

      <div v-if="mode === 'calc'" class="form-row">
        <div class="row-header">
          <label class="row-label">prevData</label>
          <el-button size="small" text type="primary" @click="formatJson('prevData')">格式化</el-button>
        </div>
        <div class="cm-row-wrapper">
          <CodeMirrorJson
            v-model="prevDataJson"
            :theme="editorTheme"
            :min-height="80"
            data-test="prev-data-input"
            @blur="formatJson('prevData', true)"
          />
        </div>
      </div>

      <!-- parse 模式: topic + 测试数据 -->
      <div v-if="mode === 'parse'" class="form-row">
        <div class="row-header">
          <label class="row-label">MQTT 主题</label>
        </div>
        <el-input
          v-model="parseTopic"
          placeholder="sys/v1/{deviceCode}/{sensorCode}/updata"
          size="small"
        />
      </div>

      <div v-if="mode === 'parse'" class="form-row">
        <div class="row-header">
          <label class="row-label">测试数据</label>
          <el-button size="small" text type="primary" @click="formatParseData()">格式化</el-button>
        </div>
        <div class="cm-row-wrapper">
          <CodeMirrorJson
            v-model="parseDataJson"
            :theme="editorTheme"
            :min-height="100"
            @blur="formatParseData(true)"
          />
        </div>
      </div>

      <div v-if="(mode === 'calc' || mode === 'parse') && jsonError" class="json-error">{{ jsonError }}</div>

      <div class="actions">
        <el-button
          type="primary"
          :loading="testing"
          :disabled="testing"
          data-test="run-btn"
          @click="handleRun"
        >
          <el-icon><CaretRight /></el-icon>
          运行测试
        </el-button>
        <el-button v-if="mode !== 'alarm'" data-test="clear-btn" @click="clearInputs">清空输入</el-button>
      </div>

      <!-- calc 结果 -->
      <el-alert
        v-if="mode === 'calc' && result?.success"
        type="success"
        :closable="false"
        class="result-alert"
        data-test="result-success"
      >
        <template #title>
          <el-icon class="result-icon"><SuccessFilled /></el-icon>
          成功
          <span v-if="result.executionTime !== undefined">· 耗时 {{ result.executionTime }}ms</span>
          <pre>{{ JSON.stringify(result.result, null, 2) }}</pre>
        </template>
      </el-alert>

      <el-alert
        v-else-if="mode === 'calc' && result && !result.success"
        type="error"
        :closable="false"
        class="result-alert"
        data-test="result-error"
      >
        <template #title>
          <el-icon class="result-icon"><CircleCloseFilled /></el-icon>
          失败
          <span v-if="result.executionTime !== undefined">· 耗时 {{ result.executionTime }}ms</span>
          <pre>{{ result.error }}</pre>
        </template>
      </el-alert>

      <!-- alarm 结果 -->
      <el-alert
        v-if="mode === 'alarm' && result"
        :type="(result as StrategyTestResult).error ? 'error' : 'success'"
        :closable="false"
        class="result-alert"
        data-test="result-alarm"
      >
        <template #title>
          <span v-if="(result as StrategyTestResult).error">
            <el-icon class="result-icon"><CircleCloseFilled /></el-icon>
            错误: {{ (result as StrategyTestResult).error }}
          </span>
          <span v-else>
            <el-icon class="result-icon"><SuccessFilled /></el-icon>
            告警等级: {{ (result as StrategyTestResult).levelText || '无告警' }}
            · 耗时 {{ (result as StrategyTestResult).durationMs }}ms
          </span>
        </template>
      </el-alert>

      <!-- parse 结果 (由父组件传入) -->
      <el-alert
        v-if="mode === 'parse' && parseResult"
        :type="parseResultType"
        :closable="false"
        class="result-alert"
      >
        <template #title>
          <el-icon class="result-icon">
            <SuccessFilled v-if="parseResultType === 'success'" />
            <CircleCloseFilled v-else />
          </el-icon>
          {{ parseResult }}
        </template>
      </el-alert>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import {
  VideoPlay, Sunny, Moon, CaretRight,
  SuccessFilled, CircleCloseFilled
} from '@element-plus/icons-vue'
import type { CalcScriptTestResult } from '@/api/monitorType'
import CodeMirrorJson from './CodeMirrorJson.vue'

/** 综合告警策略测试结果 */
export interface StrategyTestResult {
  level: number | null
  levelText: string | null
  durationMs: number
  error: string | null
}

/** 数据解析测试结果 */
export interface ParseTestPayload {
  topic: string
  testData: string
}

const props = withDefaults(defineProps<{
  result?: CalcScriptTestResult | StrategyTestResult | null
  testing: boolean
  mode?: 'calc' | 'alarm' | 'parse'
  /** parse 模式: 默认 topic */
  defaultTopic?: string
  /** parse 模式: 测试结果文本 */
  parseResult?: string
  /** parse 模式: 结果类型 */
  parseResultType?: 'success' | 'error'
}>(), {
  result: null,
  mode: 'calc',
  parseResultType: 'success'
})

const emit = defineEmits<{
  runTest: [payload: any]
}>()

const curDataJson = ref('{\n  "props": {}\n}')
const prevDataJson = ref('')
const parseTopic = ref('')
const parseDataJson = ref('')
const jsonError = ref('')
const editorTheme = ref<'light' | 'dark'>('dark')

// parse 模式初始化
if (props.mode === 'parse') {
  parseTopic.value = props.defaultTopic || 'sys/v1/DEV001/S001/updata'
}

function formatJson(field: 'curData' | 'prevData', silent = false) {
  const raw = field === 'curData' ? curDataJson.value : prevDataJson.value
  if (!raw.trim()) return
  try {
    const parsed = JSON.parse(raw)
    const formatted = JSON.stringify(parsed, null, 2)
    if (formatted === raw) return
    if (field === 'curData') {
      curDataJson.value = formatted
    } else {
      prevDataJson.value = formatted
    }
    jsonError.value = ''
    if (!silent) ElMessage.success('格式化完成')
  } catch (e: any) {
    if (!silent) {
      jsonError.value = `JSON 格式错误: ${e.message}`
      ElMessage.error('格式化失败: JSON 格式不合法')
    }
  }
}

function formatParseData(silent = false) {
  if (!parseDataJson.value.trim()) return
  try {
    const parsed = JSON.parse(parseDataJson.value)
    const formatted = JSON.stringify(parsed, null, 2)
    if (formatted === parseDataJson.value) return
    parseDataJson.value = formatted
    jsonError.value = ''
    if (!silent) ElMessage.success('格式化完成')
  } catch (e: any) {
    if (!silent) {
      jsonError.value = `JSON 格式错误: ${e.message}`
      ElMessage.error('格式化失败: JSON 格式不合法')
    }
  }
}

function handleRun() {
  if (props.mode === 'alarm') {
    emit('runTest', {})
    return
  }
  if (props.mode === 'parse') {
    emit('runTest', {
      topic: parseTopic.value,
      testData: parseDataJson.value
    } as ParseTestPayload)
    return
  }
  // calc mode
  jsonError.value = ''
  let curData: Record<string, any>
  try {
    curData = curDataJson.value.trim() ? JSON.parse(curDataJson.value) : {}
  } catch (e) {
    jsonError.value = 'curData 不是合法 JSON, 请检查格式'
    return
  }
  let prevData: Record<string, any> | undefined
  if (prevDataJson.value.trim()) {
    try {
      prevData = JSON.parse(prevDataJson.value)
    } catch (e) {
      jsonError.value = 'prevData 不是合法 JSON, 请检查格式'
      return
    }
  }
  emit('runTest', { curData, prevData })
}

function clearInputs() {
  if (props.mode === 'parse') {
    parseDataJson.value = ''
  } else {
    curDataJson.value = ''
    prevDataJson.value = ''
  }
  jsonError.value = ''
}
</script>

<style scoped>
.test-panel {
  border-top: 1px solid #ebeef5;
  background: #f5f7fa;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 12px;
  font-size: 12px;
  font-weight: 600;
  color: #606266;
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 4px;
}

.theme-toggle {
  font-size: 11px;
}

.panel-body {
  padding: 8px 12px 12px;
  background: white;
}

.form-row {
  margin-bottom: 8px;
}

.row-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.row-label {
  font-size: 12px;
  color: #606266;
}

.cm-row-wrapper {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
}

.json-error {
  color: #f56c6c;
  font-size: 11px;
  margin-bottom: 6px;
}

.actions {
  display: flex;
  gap: 8px;
  margin-top: 4px;
}

.result-alert {
  margin-top: 10px;
}

.result-icon {
  vertical-align: middle;
  margin-right: 2px;
}

.result-alert pre {
  margin: 4px 0 0;
  font-family: 'Consolas', monospace;
  font-size: 11px;
  white-space: pre-wrap;
}
</style>
