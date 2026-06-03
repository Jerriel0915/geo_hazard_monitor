<template>
  <el-drawer :model-value="visible" size="90%" @close="emit('update:visible', false)">
    <template #header>
      <div style="display: flex; align-items: center; justify-content: space-between; width: 100%;">
        <div>
          <h3 style="margin: 0; font-size: 16px;">脚本编辑器</h3>
          <p style="margin: 4px 0 0; font-size: 13px; color: #86909c;">
            {{ triggerMode === 'REALTIME' ? '实时触发 — run(TriggerMessage msg)' : '周期触发 — run()' }}
          </p>
        </div>
        <div style="display: flex; gap: 8px;">
          <el-button @click="handleTest" :loading="testing">
            <el-icon><VideoPlay /></el-icon> 测试运行
          </el-button>
          <el-button type="primary" @click="handleSave" :loading="saving">
            <el-icon><Check /></el-icon> 保存
          </el-button>
        </div>
      </div>
    </template>

    <div class="script-editor-layout">
      <!-- 左侧：工具面板 -->
      <div class="tool-panel">
        <h4 class="panel-title">预置工具</h4>
        <el-collapse v-model="expandedTools">
          <el-collapse-item v-for="group in toolGroups" :key="group.name" :title="group.name" :name="group.name">
            <div v-for="tool in group.tools" :key="tool.sign" class="tool-item" @click="insertSnippet(tool.snippet)">
              <div class="tool-name">{{ tool.name }}</div>
              <code class="tool-sign">{{ tool.sign }}</code>
              <p class="tool-desc">{{ tool.desc }}</p>
            </div>
          </el-collapse-item>
        </el-collapse>
      </div>

      <!-- 中间：Blockly 工作区 -->
      <div class="blockly-area">
        <div class="area-header">
          <span class="area-label">可视化编程</span>
          <el-radio-group v-model="editMode" size="small">
            <el-radio-button value="blockly">拼图模式</el-radio-button>
            <el-radio-button value="code">代码模式</el-radio-button>
          </el-radio-group>
        </div>

        <!-- Blockly 占位 -->
        <div v-show="editMode === 'blockly'" class="blockly-workspace" ref="blocklyContainer">
          <div class="blockly-placeholder">
            <div class="placeholder-icon">
              <svg width="48" height="48" viewBox="0 0 48 48" fill="none">
                <rect x="4" y="8" width="20" height="12" rx="4" fill="#5b8def" opacity="0.8"/>
                <rect x="12" y="20" width="28" height="12" rx="4" fill="#67c23a" opacity="0.8"/>
                <rect x="8" y="32" width="24" height="12" rx="4" fill="#e6a23c" opacity="0.8"/>
              </svg>
            </div>
            <p>Blockly 可视化编程工作区</p>
            <p class="placeholder-hint">需要安装 blockly 依赖后启用（npm install blockly）</p>
            <el-button type="primary" size="small" @click="editMode = 'code'">切换到代码模式</el-button>
          </div>
        </div>

        <!-- 代码编辑区 -->
        <div v-show="editMode === 'code'" class="code-area">
          <div class="code-template" v-if="!codeContent">
            <el-button @click="applyTemplate">加载脚本模板</el-button>
          </div>
          <textarea
            ref="codeTextarea"
            v-model="codeContent"
            class="code-editor"
            spellcheck="false"
            placeholder="在此编辑 Groovy 脚本..."
          />
        </div>
      </div>

      <!-- 右侧：代码预览 -->
      <div class="preview-panel" v-show="editMode === 'blockly'">
        <div class="area-header">
          <span class="area-label">生成代码预览</span>
        </div>
        <pre class="code-preview"><code>{{ codeContent || '// 从 Blockly 工作区生成的代码将在此显示' }}</code></pre>
      </div>
    </div>

    <!-- 测试结果 -->
    <el-dialog v-model="testResultVisible" title="测试运行结果" width="520px" append-to-body>
      <el-descriptions :column="1" border size="small">
        <el-descriptions-item label="状态">
          <el-tag v-if="testResult" :type="testResult.status === 'SUCCESS' ? 'success' : 'danger'" effect="dark">
            {{ testResult.status === 'SUCCESS' ? '成功' : '失败' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item v-if="testResult" label="耗时">{{ testResult.durationMs }}ms</el-descriptions-item>
        <el-descriptions-item v-if="testResult" label="输出">
          <pre style="margin: 0; font-size: 12px; white-space: pre-wrap;">{{ testResult.output }}</pre>
        </el-descriptions-item>
        <el-descriptions-item v-if="testResult?.errorMsg" label="错误">
          <pre style="margin: 0; font-size: 12px; color: #f53f3f; white-space: pre-wrap;">{{ testResult.errorMsg }}</pre>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { VideoPlay, Check } from '@element-plus/icons-vue'
import { getCompositeAlarmDetail, updateScriptCode, testCompositeAlarm, type CompositeAlarmLog } from '@/api/compositeAlarm'

const props = defineProps<{
  visible: boolean
  alarmId: number
  triggerMode: 'PERIODIC' | 'REALTIME'
}>()

const emit = defineEmits<{
  'update:visible': [val: boolean]
  saved: []
}>()

const editMode = ref<'blockly' | 'code'>('code')
const codeContent = ref('')
const saving = ref(false)
const testing = ref(false)
const testResultVisible = ref(false)
const testResult = ref<CompositeAlarmLog | null>(null)
const expandedTools = ref(['告警查询', '设备与数据', '工具方法'])

const toolGroups = [
  {
    name: '告警查询',
    tools: [
      { name: '查询告警记录', sign: 'queryAlarms(level, pointId, since)', desc: '查询阈值告警或综合告警记录', snippet: 'def alarms = queryAlarms(2, pointId, since)' }
    ]
  },
  {
    name: '设备与数据',
    tools: [
      { name: '查询设备列表', sign: 'queryDevices(pointId)', desc: '查询隐患点下所有设备', snippet: 'def devices = queryDevices(pointId)' },
      { name: '查询历史数据', sign: 'queryHistory(deviceId, sensorCode, start, end)', desc: '从 IoTDB 查询历史时序数据', snippet: 'def history = queryHistory(deviceId, \'displacement\', start, end)' },
      { name: '查询最新数据', sign: 'queryLatest(deviceId, sensorCode)', desc: '查询设备最新一条数据', snippet: 'def latest = queryLatest(deviceId, \'displacement\')' },
      { name: '查询天气数据', sign: 'queryWeather(pointId)', desc: '查询隐患点位置天气信息', snippet: 'def weather = queryWeather(pointId)' }
    ]
  },
  {
    name: '工具方法',
    tools: [
      { name: '调用算法', sign: 'invokeAlgorithm(name, params)', desc: '调用预置 Python 算法', snippet: 'def result = invokeAlgorithm(\'slope_stability\', [angle: 45, cohesion: 20])' },
      { name: '存储数据', sign: 'storeData(key, value, ttl)', desc: '策略级暂存数据，跨次执行持久化', snippet: 'storeData(\'last_check\', now(), 3600)' },
      { name: '读取数据', sign: 'getData(key)', desc: '读取暂存数据', snippet: 'def lastCheck = getData(\'last_check\')' },
      { name: '获取应用范围', sign: 'getScopes()', desc: '获取策略绑定的所有隐患点', snippet: 'def points = getScopes()' }
    ]
  },
  {
    name: '日志输出',
    tools: [
      { name: 'INFO 日志', sign: 'logInfo(msg)', desc: '记录信息级别日志', snippet: 'logInfo(\'检查完成\')' },
      { name: 'WARN 日志', sign: 'logWarn(msg)', desc: '记录警告级别日志', snippet: 'logWarn(\'数据异常\')' },
      { name: 'ERROR 日志', sign: 'logError(msg)', desc: '记录错误级别日志', snippet: 'logError(\'处理失败\')' }
    ]
  }
]

watch(() => props.visible, async (val) => {
  if (val) {
    editMode.value = 'code'
    try {
      const detail = await getCompositeAlarmDetail(props.alarmId)
      codeContent.value = detail.scriptCode || ''
    } catch {
      codeContent.value = ''
    }
  }
}, { immediate: true })

function applyTemplate() {
  if (props.triggerMode === 'REALTIME') {
    codeContent.value = `// 实时触发模式 — 订阅数据到达时执行
// msg: TriggerMessage { sourceType, sourceId, payload }
def run(TriggerMessage msg) {
    def deviceId = msg.payload.deviceId
    def value = msg.payload.value

    logInfo("收到触发: sourceType=" + msg.sourceType + " deviceId=" + deviceId)

    // 在此编写判断逻辑
    def data = queryLatest(deviceId, 'displacement')
    if (data.value > 10) {
        return [level: 2, message: '监测值超限', detail: "设备\${deviceId} 当前值: \${data.value}"]
    }
    return null
}`
  } else {
    codeContent.value = `// 周期触发模式 — 按 Cron 表达式定时执行
def run() {
    def points = getScopes()

    for (point in points) {
        def devices = queryDevices(point.id)
        for (device in devices) {
            def data = queryLatest(device.id, 'displacement')
            logInfo("点位: \${point.name} 设备: \${device.name} 值: \${data.value}")

            if (data.value > 10) {
                return [level: 2, message: '位移超限', detail: "点位:\${point.name} 值:\${data.value}"]
            }
        }
    }
    return null
}`
  }
}

function insertSnippet(snippet: string) {
  if (editMode.value === 'code') {
    codeContent.value = codeContent.value ? codeContent.value + '\n' + snippet : snippet
  }
}

async function handleSave() {
  if (!codeContent.value.trim()) {
    ElMessage.warning('脚本内容不能为空')
    return
  }
  saving.value = true
  try {
    await updateScriptCode(props.alarmId, codeContent.value)
    ElMessage.success('脚本已保存')
    emit('saved')
  } catch (e: any) {
    ElMessage.error(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function handleTest() {
  if (!codeContent.value.trim()) {
    ElMessage.warning('请先编写脚本')
    return
  }
  testing.value = true
  try {
    testResult.value = await testCompositeAlarm(props.alarmId)
    testResultVisible.value = true
  } catch (e: any) {
    ElMessage.error(e.message || '测试失败')
  } finally {
    testing.value = false
  }
}
</script>

<style scoped>
.script-editor-layout {
  display: flex;
  gap: 0;
  height: calc(100vh - 80px);
  background: #fff;
}

.tool-panel {
  width: 280px;
  border-right: 1px solid #e5e6eb;
  overflow-y: auto;
  flex-shrink: 0;
  background: #fafbfc;
}

.panel-title {
  margin: 0;
  padding: 12px 16px;
  font-size: 14px;
  font-weight: 600;
  color: #1d2129;
  border-bottom: 1px solid #e5e6eb;
}

.tool-item {
  padding: 8px 16px;
  cursor: pointer;
  transition: background 0.15s;
  border-bottom: 1px solid #f2f3f5;
}

.tool-item:hover {
  background: #e8f3ff;
}

.tool-name {
  font-size: 13px;
  font-weight: 500;
  color: #1d2129;
  margin-bottom: 2px;
}

.tool-sign {
  display: block;
  font-size: 11px;
  color: #5b8def;
  font-family: 'Courier New', monospace;
  margin-bottom: 4px;
}

.tool-desc {
  margin: 0;
  font-size: 12px;
  color: #86909c;
}

.blockly-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.area-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 16px;
  border-bottom: 1px solid #e5e6eb;
  background: #fafbfc;
}

.area-label {
  font-size: 13px;
  font-weight: 600;
  color: #4e5969;
}

.blockly-workspace {
  flex: 1;
  position: relative;
}

.blockly-placeholder {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #f7f8fa;
  color: #86909c;
}

.placeholder-icon {
  margin-bottom: 12px;
}

.placeholder-hint {
  font-size: 12px;
  color: #c9cdd4;
  margin-bottom: 16px;
}

.code-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  position: relative;
}

.code-template {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(247, 248, 250, 0.9);
  z-index: 1;
}

.code-editor {
  flex: 1;
  border: none;
  outline: none;
  resize: none;
  padding: 16px;
  font-family: 'JetBrains Mono', 'Fira Code', 'Courier New', monospace;
  font-size: 14px;
  line-height: 1.7;
  color: #1d2129;
  background: #fff;
  tab-size: 4;
}

.code-editor:focus {
  background: #fefefe;
}

.preview-panel {
  width: 380px;
  border-left: 1px solid #e5e6eb;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.code-preview {
  flex: 1;
  margin: 0;
  padding: 16px;
  font-family: 'JetBrains Mono', 'Fira Code', 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.7;
  color: #4e5969;
  background: #fafbfc;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-all;
}

:deep(.el-collapse-item__header) {
  font-size: 13px;
  font-weight: 500;
}

:deep(.el-collapse-item__content) {
  padding: 0;
}
</style>
