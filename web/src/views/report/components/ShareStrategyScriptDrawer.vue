<template>
  <el-drawer
    v-model="visible"
    title="可视化脚本配置"
    direction="rtl"
    size="800px"
    :destroy-on-close="true"
  >
    <div class="script-drawer">
      <div class="drawer-header">
        <h3>{{ strategyName }}</h3>
        <span class="method-tag">{{ METHOD_LABELS[method] }}</span>
      </div>

      <div class="editor-section">
        <div class="section-header">
          <span class="section-title">Blockly 可视化编辑器</span>
          <div class="section-actions">
            <el-button size="small" @click="handleClearBlocks">清空</el-button>
            <el-button size="small" @click="handleLoadTemplate">加载模板</el-button>
          </div>
        </div>
        <div ref="blocklyContainer" class="blockly-container"></div>
      </div>

      <div class="variables-section">
        <div class="section-header">
          <span class="section-title">变量配置</span>
        </div>
        <el-form :model="variablesForm" label-width="100px">
          <el-form-item label="策略属性">
            <el-checkbox-group v-model="selectedProps">
              <el-checkbox label="策略编号" value="code" />
              <el-checkbox label="策略名称" value="name" />
              <el-checkbox label="目标地址" value="address" />
              <el-checkbox label="执行频率" value="frequency" />
            </el-checkbox-group>
          </el-form-item>
        </el-form>
      </div>

      <div class="tools-section">
        <div class="section-header">
          <span class="section-title">可用工具方法</span>
        </div>
        <div class="tools-grid">
          <div
            v-for="tool in availableTools"
            :key="tool.id"
            class="tool-item"
            @click="handleAddTool(tool)"
          >
            <el-icon :component="tool.icon" class="tool-icon" />
            <span class="tool-name">{{ tool.name }}</span>
            <span class="tool-desc">{{ tool.description }}</span>
          </div>
        </div>
      </div>

      <div class="output-section">
        <div class="section-header">
          <span class="section-title">生成代码</span>
          <el-button size="small" type="primary" @click="handleExportCode">导出代码</el-button>
        </div>
        <pre class="code-output">{{ generatedCode }}</pre>
      </div>
    </div>

    <template #footer>
      <el-button @click="emit('update:visible', false)">取消</el-button>
      <el-button type="primary" :loading="saving" @click="handleSave">保存脚本</el-button>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { DataLine, Aim, Cpu, Files, ArrowRight, AlarmClock, Box, Bell } from '@element-plus/icons-vue'
import { getShareStrategyScript, saveShareStrategyScript, METHOD_LABELS, type ShareStrategyItem } from '@/api/shareStrategy'

const props = defineProps<{
  visible: boolean
  strategyId: number
  strategyName: string
  method: ShareStrategyItem['method']
}>()

const emit = defineEmits<{
  (e: 'update:visible', val: boolean): void
  (e: 'saved'): void
}>()

const blocklyContainer = ref<HTMLElement | null>(null)
const saving = ref(false)
const selectedProps = ref<string[]>([])
const generatedCode = ref('')

interface ToolItem {
  id: string
  name: string
  description: string
  icon: any
  category: string
}

const availableTools = ref<ToolItem[]>([
  { id: 'subscribe', name: '订阅数据包', description: '监听指定数据源的实时数据', icon: Bell, category: 'data' },
  { id: 'parseStrategy', name: '解析策略', description: '解析其他共享策略的输出', icon: Box, category: 'data' },
  { id: 'queryDevice', name: '查询设备', description: '根据条件查询设备信息', icon: Aim, category: 'query' },
  { id: 'queryVendor', name: '查询厂商', description: '查询厂商详细信息', icon: Aim, category: 'query' },
  { id: 'queryHazardPoint', name: '查询隐患点', description: '查询隐患点信息', icon: Aim, category: 'query' },
  { id: 'getProperty', name: '获取属性', description: '获取策略或设备的属性值', icon: Cpu, category: 'property' },
  { id: 'algorithm', name: '执行算法', description: '调用Python算法处理数据', icon: Cpu, category: 'algorithm' },
  { id: 'storeData', name: '存储数据', description: '将数据存储到数据库', icon: DataLine, category: 'storage' },
  { id: 'output', name: '数据输出', description: '输出数据到目标地址', icon: ArrowRight, category: 'output' },
  { id: 'delay', name: '延时等待', description: '暂停执行指定时间', icon: AlarmClock, category: 'control' },
  { id: 'log', name: '记录日志', description: '记录执行日志', icon: Files, category: 'utility' }
])

const variablesForm = computed(() => ({
  selectedProps: selectedProps.value
}))

async function loadScript() {
  if (!props.strategyId) return
  try {
    const res = await getShareStrategyScript(props.strategyId)
    selectedProps.value = Object.keys(res.variables || {})
    generatedCode.value = res.script || ''
  } catch {
    selectedProps.value = []
    generatedCode.value = ''
  }
}

async function handleSave() {
  saving.value = true
  try {
    await saveShareStrategyScript(props.strategyId, {
      script: generatedCode.value,
      variables: selectedProps.value.reduce((acc, prop) => ({ ...acc, [prop]: true }), {})
    })
    ElMessage.success('保存成功')
    emit('saved')
    emit('update:visible', false)
  } catch (e: any) {
    ElMessage.error(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}

function handleClearBlocks() {
  generatedCode.value = ''
  selectedProps.value = []
}

function handleLoadTemplate() {
  generatedCode.value = `// ${METHOD_LABELS[props.method]} 模板代码\nasync function executeShare(data) {\n  // 获取策略配置\n  const config = getStrategyConfig();\n  \n  // 查询数据\n  const devices = await queryDevices({ scopeIds: config.scopeIds });\n  \n  // 处理数据\n  const result = devices.map(device => ({\n    id: device.id,\n    name: device.name,\n    status: device.status,\n    data: device.lastData\n  }));\n  \n  // 输出数据\n  await outputData(result, {\n    address: config.address,\n    topic: config.topic\n  });\n  \n  return result.length;\n}`
}

function handleAddTool(tool: ToolItem) {
  const toolCode = getToolCode(tool)
  if (generatedCode.value) {
    generatedCode.value += '\n\n' + toolCode
  } else {
    generatedCode.value = toolCode
  }
}

function getToolCode(tool: ToolItem): string {
  const templates: Record<string, string> = {
    subscribe: '// 订阅实时数据\nconst subscription = subscribeData({\n  type: "SENSOR_DATA",\n  filters: { deviceIds: [] }\n});\nsubscription.on("data", (data) => {\n  // 处理数据\n});',
    parseStrategy: '// 解析其他策略输出\nconst otherStrategy = parseStrategyOutput("STRATEGY_CODE");',
    queryDevice: '// 查询设备\nconst devices = await queryDevices({\n  ids: [],\n  status: 1\n});',
    queryVendor: '// 查询厂商\nconst vendors = await queryVendors({\n  ids: []\n});',
    queryHazardPoint: '// 查询隐患点\nconst hazardPoints = await queryHazardPoints({\n  ids: []\n});',
    getProperty: '// 获取属性\nconst value = getProperty("propertyName");',
    algorithm: '// 执行算法\nconst result = await runAlgorithm({\n  name: "算法名称",\n  params: {}\n});',
    storeData: '// 存储数据\nawait storeData("collectionName", data);',
    output: '// 数据输出\nawait outputData(data, {\n  address: config.address,\n  format: "json"\n});',
    delay: '// 延时等待\nawait delay(1000); // 毫秒',
    log: '// 记录日志\nlog("info", "消息内容");'
  }
  return templates[tool.id] || `// ${tool.name}\n// ${tool.description}`
}

function handleExportCode() {
  const blob = new Blob([generatedCode.value], { type: 'text/javascript' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `share-strategy-${props.strategyId}.js`
  a.click()
  URL.revokeObjectURL(url)
}

watch(() => props.visible, (val) => {
  if (val) {
    loadScript()
  }
})

onMounted(() => {
  if (props.visible) {
    loadScript()
  }
})

onUnmounted(() => {
})
</script>

<style scoped>
.script-drawer {
  height: calc(100% - 80px);
  overflow-y: auto;
  padding: 16px;
}

.drawer-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 16px;
}

.drawer-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.method-tag {
  font-size: 12px;
  padding: 2px 8px;
  background: #e6f7ff;
  color: #1890ff;
  border-radius: 4px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.section-actions {
  display: flex;
  gap: 8px;
}

.editor-section {
  margin-bottom: 20px;
}

.blockly-container {
  height: 300px;
  background: #fafafa;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.blockly-container::before {
  content: 'Blockly 编辑器区域';
  color: #909399;
  font-size: 14px;
}

.variables-section {
  margin-bottom: 20px;
  padding: 12px;
  background: #fafafa;
  border-radius: 4px;
}

.tools-section {
  margin-bottom: 20px;
}

.tools-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.tool-item {
  display: flex;
  flex-direction: column;
  padding: 12px;
  background: #fafafa;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
}

.tool-item:hover {
  background: #e6f7ff;
  border-color: #91d5ff;
}

.tool-icon {
  font-size: 20px;
  color: #1890ff;
  margin-bottom: 6px;
}

.tool-name {
  font-size: 13px;
  font-weight: 500;
  color: #303133;
}

.tool-desc {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

.output-section {
  margin-bottom: 20px;
}

.code-output {
  background: #1f1f1f;
  color: #d4d4d4;
  padding: 12px;
  border-radius: 4px;
  font-family: 'Monaco', 'Menlo', monospace;
  font-size: 13px;
  line-height: 1.5;
  max-height: 200px;
  overflow-y: auto;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>