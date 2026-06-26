<template>
  <div class="test-panel">
    <div class="panel-header">▶ 在线测试</div>

    <div class="panel-body">
      <div class="form-row">
        <label class="row-label">curData</label>
        <el-input
          v-model="curDataJson"
          type="textarea"
          :rows="4"
          placeholder='{"props":{"attrCode":12.5}}'
          data-test="cur-data-input"
        />
      </div>

      <div class="form-row">
        <label class="row-label">prevData</label>
        <el-input
          v-model="prevDataJson"
          type="textarea"
          :rows="4"
          placeholder='{"props":{"attrCode":10.0},"dataTime":1700000000000}'
          data-test="prev-data-input"
        />
      </div>

      <div v-if="jsonError" class="json-error">{{ jsonError }}</div>

      <div class="actions">
        <el-button
          type="primary"
          :loading="testing"
          :disabled="testing"
          data-test="run-btn"
          @click="handleRun"
        >运行测试</el-button>
        <el-button data-test="clear-btn" @click="clearInputs">清空输入</el-button>
      </div>

      <el-alert
        v-if="result?.success"
        type="success"
        :closable="false"
        class="result-alert"
        data-test="result-success"
      >
        <template #title>
          ✅ 成功
          <span v-if="result.executionTime !== undefined">· 耗时 {{ result.executionTime }}ms</span>
          <pre>{{ JSON.stringify(result.result, null, 2) }}</pre>
        </template>
      </el-alert>

      <el-alert
        v-else-if="result && !result.success"
        type="error"
        :closable="false"
        class="result-alert"
        data-test="result-error"
      >
        <template #title>
          ❌ 失败
          <span v-if="result.executionTime !== undefined">· 耗时 {{ result.executionTime }}ms</span>
          <pre>{{ result.error }}</pre>
        </template>
      </el-alert>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { CalcScriptTestResult } from '@/api/monitorType'

defineProps<{
  result: CalcScriptTestResult | null
  testing: boolean
}>()

const emit = defineEmits<{
  runTest: [payload: { curData: Record<string, any>; prevData: Record<string, any> | undefined }]
}>()

const curDataJson = ref('{\n  "props": {}\n}')
const prevDataJson = ref('')
const jsonError = ref('')

function handleRun() {
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
  curDataJson.value = ''
  prevDataJson.value = ''
  jsonError.value = ''
}
</script>

<style scoped>
.test-panel {
  border-top: 1px solid #ebeef5;
  background: #f5f7fa;
}

.panel-header {
  padding: 8px 12px;
  font-size: 12px;
  font-weight: 600;
  color: #606266;
}

.panel-body {
  padding: 8px 12px 12px;
  background: white;
}

.form-row {
  display: flex;
  margin-bottom: 8px;
}

.row-label {
  width: 80px;
  font-size: 12px;
  color: #606266;
  padding-top: 6px;
  flex-shrink: 0;
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

.result-alert pre {
  margin: 4px 0 0;
  font-family: 'Consolas', monospace;
  font-size: 11px;
  white-space: pre-wrap;
}
</style>
