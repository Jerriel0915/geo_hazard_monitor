<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    :title="`计算脚本 - ${attrName}`"
    width="800px"
    :close-on-click-modal="false"
    destroy-on-close
  >
    <el-alert
      type="info"
      :closable="false"
      class="form-alert"
    >
      <template #title>
        可用变量:
        <code>curData?.props?.{{ attrCode }}</code>
        (当前数据包) ·
        <code>prevData?.props?.{{ attrCode }}</code>
        (上一条数据包, 可空)
        <br />
        返回: 数值(Number)
      </template>
    </el-alert>

    <el-input
      v-model="localScript"
      type="textarea"
      :rows="14"
      class="code-textarea"
      placeholder="// 输入计算脚本"
    />

    <el-collapse v-model="testPanelActive" class="test-panel">
      <el-collapse-item title="在线测试" name="test">
        <el-form label-width="100px">
          <el-form-item label="curData">
            <el-input
              v-model="curDataJson"
              type="textarea"
              :rows="4"
              placeholder='{"props":{"attrCode":12.5}}'
            />
          </el-form-item>
          <el-form-item label="prevData">
            <el-input
              v-model="prevDataJson"
              type="textarea"
              :rows="4"
              placeholder='{"props":{"attrCode":10.0},"dataTime":1700000000000}'
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="runTest" :loading="testing">运行测试</el-button>
            <el-button @click="curDataJson = ''; prevDataJson = ''">清空输入</el-button>
          </el-form-item>
        </el-form>

        <el-alert
          v-if="testResult"
          :type="testResult.success ? 'success' : 'error'"
          :closable="false"
          class="form-alert"
        >
          <template #title>
            {{ testResult.success ? '✅ 成功' : '❌ 失败' }}
            <span v-if="testResult.executionTime !== undefined">
              · 耗时 {{ testResult.executionTime }}ms
            </span>
            <br />
            <pre v-if="testResult.success">{{ JSON.stringify(testResult.result, null, 2) }}</pre>
            <span v-else>{{ testResult.error }}</span>
          </template>
        </el-alert>
      </el-collapse-item>
    </el-collapse>

    <template #footer>
      <el-button @click="$emit('update:modelValue', false)">取消</el-button>
      <el-button @click="localScript = defaultTemplate">重置为模板</el-button>
      <el-button type="primary" @click="handleSave">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { testCalcScript, type CalcScriptTestResult } from '@/api/monitorType'

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

const localScript = ref(props.script)
const testPanelActive = ref<string[]>(['test'])
const curDataJson = ref('{\n  "props": {}\n}')
const prevDataJson = ref('')
const testing = ref(false)
const testResult = ref<CalcScriptTestResult | null>(null)

// key 用 'props': Groovy 中 map.properties 会触发 GDK Object.getProperties(), 用 .props 规避
const defaultTemplate = computed(() =>
  `// 计算属性: ${props.attrCode}\n` +
  '// 可用变量:\n' +
  `//   curData?.props?.${props.attrCode}  当前数据包属性值\n` +
  `//   prevData?.props?.${props.attrCode}  上一条数据包属性值(可空)\n` +
  '// 返回: 数值 (Number)\n\n' +
  `return curData?.props?.${props.attrCode}\n`
)

// 每次打开重置本地状态
watch(() => props.modelValue, (open) => {
  if (open) {
    localScript.value = props.script || defaultTemplate.value
    testResult.value = null
  }
})

const handleSave = () => {
  if (!localScript.value.trim()) {
    ElMessage.warning('脚本不能为空')
    return
  }
  emit('save', localScript.value)
}

const runTest = async () => {
  if (!localScript.value.trim()) {
    ElMessage.warning('请先输入脚本')
    return
  }
  if (!props.monitorTypeId) {
    ElMessage.warning('请先保存监测类型, 再测试脚本')
    return
  }
  let curData: Record<string, any>
  try {
    curData = curDataJson.value.trim() ? JSON.parse(curDataJson.value) : {}
  } catch (e) {
    ElMessage.error('curData 不是合法 JSON')
    return
  }
  let prevData: Record<string, any> | undefined
  if (prevDataJson.value.trim()) {
    try {
      prevData = JSON.parse(prevDataJson.value)
    } catch (e) {
      ElMessage.error('prevData 不是合法 JSON')
      return
    }
  }
  testing.value = true
  try {
    const result = await testCalcScript({
      monitorTypeId: props.monitorTypeId,
      attrCode: props.attrCode,
      calcScript: localScript.value,
      curData,
      prevData
    })
    testResult.value = result
  } catch (e: any) {
    testResult.value = { success: false, error: e?.message || '请求失败' }
  } finally {
    testing.value = false
  }
}
</script>

<style scoped>
.form-alert {
  margin-bottom: 12px;
}

.code-textarea :deep(textarea) {
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.6;
}

.test-panel {
  margin-top: 16px;
}

pre {
  margin: 4px 0 0;
  font-family: 'Consolas', monospace;
  font-size: 12px;
  white-space: pre-wrap;
}
</style>
