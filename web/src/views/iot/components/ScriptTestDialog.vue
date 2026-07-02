<!-- 数据解析 — 脚本测试共享弹窗 -->
<template>
  <el-dialog
      v-model="dialogVisible"
      title="脚本测试"
      width="800px"
      :close-on-click-modal="false"
      destroy-on-close
  >
    <el-form label-width="100px">
      <el-form-item label="MQTT 主题">
        <el-input
            v-model="testTopic"
            placeholder="格式: sys/v1/{deviceCode}/{sensorCode}/updata"
        />
      </el-form-item>
      <el-form-item label="测试数据">
        <div class="json-input-area">
          <div class="json-input-header">
            <span class="header-hint">原始 payload（脚本以 byte[] 接收）</span>
            <div class="header-actions">
              <el-button size="small" text type="primary" @click="formatTestData()">格式化</el-button>
              <el-button size="small" text @click="toggleTheme">
                <el-icon><Sunny v-if="editorTheme === 'dark'" /><Moon v-else /></el-icon>
              </el-button>
            </div>
          </div>
          <div class="cm-wrapper">
            <CodeMirrorJson
              v-model="testData"
              :theme="editorTheme"
              :min-height="140"
              @blur="formatTestData(true)"
            />
          </div>
        </div>
      </el-form-item>
      <el-form-item label="测试结果">
        <div class="json-input-area">
          <div class="cm-wrapper">
            <CodeMirrorJson
              v-model="testResult"
              :theme="editorTheme"
              :min-height="180"
              readonly
            />
          </div>
        </div>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="handleRunTest" :loading="testRunning">运行测试</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { testScript, type DataParseTestResult } from '@/api/dataParse'
import { Sunny, Moon } from '@element-plus/icons-vue'
import CodeMirrorJson from '@/views/basic/components/script-editor/CodeMirrorJson.vue'

interface Props {
  visible: boolean
  /** 要测试的 Groovy 脚本代码 */
  scriptCode: string
  /** 默认填入的主题 */
  defaultTopic?: string
}

interface Emits {
  (e: 'update:visible', value: boolean): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const dialogVisible = ref(false)
const testTopic = ref('')
const testData = ref('')
const testResult = ref('')
const testRunning = ref(false)
const editorTheme = ref<'light' | 'dark'>('dark')

function toggleTheme() {
  editorTheme.value = editorTheme.value === 'dark' ? 'light' : 'dark'
}

function formatTestData(silent = false) {
  if (!testData.value.trim()) return
  try {
    const parsed = JSON.parse(testData.value)
    const formatted = JSON.stringify(parsed, null, 2)
    if (formatted === testData.value) return
    testData.value = formatted
    if (!silent) ElMessage.success('格式化完成')
  } catch (e: any) {
    if (!silent) ElMessage.error(`格式化失败: JSON 格式不合法 — ${e.message}`)
  }
}

// 弹窗打开时重置状态 + 填入默认主题
watch(() => props.visible, (val) => {
  dialogVisible.value = val
  if (val) {
    testTopic.value = props.defaultTopic || 'sys/v1/DEV001/S001/updata'
    testData.value = ''
    testResult.value = ''
  }
})

watch(() => dialogVisible.value, (val) => {
  if (!val) emit('update:visible', val)
})

const handleRunTest = async () => {
  if (!testData.value) {
    ElMessage.warning('请输入测试数据')
    return
  }
  if (!props.scriptCode) {
    ElMessage.warning('无脚本代码，无法测试')
    return
  }
  testRunning.value = true
  testResult.value = ''
  try {
    const result: DataParseTestResult = await testScript({
      scriptCode: props.scriptCode,
      topic: testTopic.value,
      testData: testData.value
    })
    testResult.value = JSON.stringify(result, null, 2)
    if (result.success) {
      ElMessage.success('测试运行成功')
    } else {
      ElMessage.error('测试失败: ' + (result.error || '未知错误'))
    }
  } catch (e: any) {
    testResult.value = JSON.stringify({ success: false, error: e.message }, null, 2)
    ElMessage.error('测试失败')
  } finally {
    testRunning.value = false
  }
}
</script>

<style scoped>
.json-input-area {
  width: 100%;
}

.json-input-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.header-hint {
  font-size: 11px;
  color: #909399;
}

.header-actions {
  display: flex;
  gap: 4px;
}

.cm-wrapper {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
}
</style>
