<!-- 数据解析编辑表单 -->
<template>
  <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="1000px"
      :close-on-click-modal="false"
      destroy-on-close
      class="parse-form-dialog"
  >
    <el-tabs v-model="activeTab" type="border-card">
      <el-tab-pane label="基本信息" name="basic">
        <el-form ref="formRef" :model="formData" :rules="formRules" label-width="120px">
          <el-form-item label="策略名称" prop="name">
            <el-input v-model="formData.name" placeholder="请输入策略名称" :disabled="isView" />
          </el-form-item>
          <el-form-item label="服务地址" prop="serverUrl">
            <el-input v-model="formData.serverUrl" placeholder="请输入MQTT服务地址" :disabled="isView" />
          </el-form-item>
          <el-form-item label="主题" prop="topic">
            <el-input v-model="formData.topic" placeholder="请输入订阅主题，如：$dp" :disabled="isView" />
          </el-form-item>
          <el-form-item label="描述" prop="description">
            <el-input
                v-model="formData.description"
                type="textarea"
                :rows="3"
                placeholder="请输入策略描述"
                :disabled="isView"
            />
          </el-form-item>
          <el-form-item label="启用状态" prop="status">
            <el-radio-group v-model="formData.status" :disabled="isView">
              <el-radio :label="1">启用</el-radio>
              <el-radio :label="0">停用</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="应用范围" prop="appScope">
            <el-radio-group v-model="formData.appScope" :disabled="isView" @change="handleAppScopeChange">
              <el-radio value="global">全局</el-radio>
              <el-radio value="vendor">指定厂商</el-radio>
              <el-radio value="device">指定设备</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="厂商选择" prop="vendorIds" v-if="formData.appScope === 'vendor'">
            <el-select v-model="formData.vendorIds" multiple placeholder="请选择厂商" :disabled="isView" style="width: 100%">
              <el-option v-for="vendor in vendorList" :key="vendor.id" :label="vendor.name" :value="vendor.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="设备选择" prop="deviceIds" v-if="formData.appScope === 'device'">
            <el-select v-model="formData.deviceIds" multiple placeholder="请选择设备" :disabled="isView" style="width: 100%">
              <el-option v-for="device in deviceList" :key="device.id" :label="device.name" :value="device.id" />
            </el-select>
          </el-form-item>
        </el-form>
      </el-tab-pane>

      <el-tab-pane label="脚本编辑" name="script">
        <div class="script-editor-container">
          <div class="editor-tabs" v-if="!isView">
            <el-radio-group v-model="scriptMode" size="small">
<!--              <el-radio-button label="visual">可视化编程</el-radio-button>-->
              <el-radio-button label="code">代码编辑</el-radio-button>
            </el-radio-group>
          </div>

          <!-- 可视化编程区域 -->
          <div v-if="scriptMode === 'visual'" class="blockly-container">
            <div id="blocklyDiv" class="blockly-workspace">
              <div class="blockly-placeholder">Blockly 可视化编程区域</div>
            </div>
            <div class="toolbox-container">
              <div class="toolbox-title">工具模块</div>
              <div class="toolbox-items">
                <div class="toolbox-category">
                  <div class="category-title">数据监听</div>
                  <div class="tool-item">监听MQTT消息</div>
                  <div class="tool-item">监听策略解析结果</div>
                </div>
                <div class="toolbox-category">
                  <div class="category-title">数据查询</div>
                  <div class="tool-item">查询设备信息</div>
                  <div class="tool-item">查询厂商信息</div>
                  <div class="tool-item">查询隐患点信息</div>
                </div>
                <div class="toolbox-category">
                  <div class="category-title">算法调用</div>
                  <div class="tool-item">数据清洗算法</div>
                  <div class="tool-item">数据格式转换</div>
                  <div class="tool-item">数据异常检测</div>
                  <div class="tool-item">数据聚合计算</div>
                  <div class="tool-item">数据趋势分析</div>
                </div>
                <div class="toolbox-category">
                  <div class="category-title">数据存储</div>
                  <div class="tool-item">存储监测数据</div>
                  <div class="tool-item">存储设备状态</div>
                  <div class="tool-item">存储告警事件</div>
                </div>
                <div class="toolbox-category">
                  <div class="category-title">数据输出</div>
                  <div class="tool-item">输出到其他策略</div>
                  <div class="tool-item">输出到HTTP接口</div>
                  <div class="tool-item">输出到消息队列</div>
                </div>
                <div class="toolbox-category">
                  <div class="category-title">控制逻辑</div>
                  <div class="tool-item">条件判断</div>
                  <div class="tool-item">循环执行</div>
                  <div class="tool-item">日志输出</div>
                </div>
              </div>
            </div>
          </div>

          <!-- 代码编辑区域 -->
          <div v-else class="code-editor-container">
            <el-input
                v-model="formData.scriptCode"
                type="textarea"
                :rows="20"
                placeholder="// 请输入解析脚本代码
// 示例：解析国标协议数据
function parse(data) {
  const result = {};
  result.timestamp = Date.now();
  result.deviceId = data.deviceId;
  result.data = data.payload;
  return result;
}"
                :disabled="isView"
                class="code-textarea"
            />
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <template #footer v-if="!isView">
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="handleTest" :disabled="!formData.scriptCode">测试脚本</el-button>
      <el-button type="primary" @click="handleSubmit" :loading="submitLoading">保存</el-button>
    </template>
    <template #footer v-else>
      <el-button @click="dialogVisible = false">关闭</el-button>
    </template>
  </el-dialog>

  <!-- 测试弹窗 -->
  <el-dialog
      v-model="testDialogVisible"
      title="脚本测试"
      width="800px"
      :close-on-click-modal="false"
      destroy-on-close
  >
    <el-form label-width="100px">
      <el-form-item label="测试数据">
        <el-input
            v-model="testData"
            type="textarea"
            :rows="8"
            placeholder='请输入测试数据，JSON格式：
{
  "topic": "$dp",
  "payload": {
    "deviceId": "dev001",
    "data": "..."
  }
}'
        />
      </el-form-item>
      <el-form-item label="测试结果">
        <el-input
            v-model="testResult"
            type="textarea"
            :rows="8"
            readonly
            placeholder="测试结果将显示在这里"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="testDialogVisible = false">取消</el-button>
      <el-button type="primary" @click="handleRunTest" :loading="testRunning">运行测试</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

interface Props {
  visible: boolean
  data: any
  mode: 'add' | 'edit' | 'view'
}

interface Emits {
  (e: 'update:visible', value: boolean): void
  (e: 'submit', data: any): void
  (e: 'test', data: any): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const dialogVisible = ref(false)
const activeTab = ref('basic')
const scriptMode = ref('code')
const submitLoading = ref(false)
const testRunning = ref(false)
const formRef = ref()

// 测试相关
const testDialogVisible = ref(false)
const testData = ref('')
const testResult = ref('')

// setTimeout 清理
const formTimers: ReturnType<typeof setTimeout>[] = []

onUnmounted(() => {
  formTimers.forEach(id => clearTimeout(id))
  formTimers.length = 0
})

// 表单数据
const formData = reactive({
  id: null as number | null,
  name: '',
  serverUrl: '',
  topic: '',
  description: '',
  status: 1,
  appScope: 'global' as 'global' | 'vendor' | 'device',
  vendorIds: [] as number[],
  deviceIds: [] as number[],
  scriptCode: ''
})

// 厂商和设备列表
const vendorList = ref([
  { id: 1, name: '北京国信华源科技有限公司' },
  { id: 2, name: '深圳北斗智联科技有限公司' },
  { id: 3, name: '上海物联网科技有限公司' }
])

const deviceList = ref([
  { id: 1, name: 'GNSS监测站-001' },
  { id: 2, name: '雨量计-001' },
  { id: 3, name: '裂缝监测-001' }
])

// 表单验证规则
const formRules = {
  name: [{ required: true, message: '请输入策略名称', trigger: 'blur' }],
  serverUrl: [{ required: true, message: '请输入服务地址', trigger: 'blur' }],
  topic: [{ required: true, message: '请输入主题', trigger: 'blur' }],
  status: [{ required: true, message: '请选择启用状态', trigger: 'change' }]
}

// 弹窗标题
const dialogTitle = computed(() => {
  if (props.mode === 'add') return '新增解析策略'
  if (props.mode === 'edit') return '编辑解析策略'
  return '查看解析策略'
})

// 是否只读
const isView = computed(() => props.mode === 'view')

// 监听 visible 变化
watch(() => props.visible, (val) => {
  dialogVisible.value = val
  if (val && props.data) {
    fillForm(props.data)
    activeTab.value = 'basic'
    scriptMode.value = 'code'
  } else if (val && props.mode === 'add') {
    resetForm()
    activeTab.value = 'basic'
    scriptMode.value = 'code'
  }
})

watch(() => dialogVisible.value, (val) => {
  if (!val) {
    emit('update:visible', val)
  }
})

// 重置表单
const resetForm = () => {
  formData.id = null
  formData.name = ''
  formData.serverUrl = ''
  formData.topic = ''
  formData.description = ''
  formData.status = 1
  formData.appScope = 'global'
  formData.vendorIds = []
  formData.deviceIds = []
  formData.scriptCode = ''
  formRef.value?.clearValidate()
}

// 填充表单
const fillForm = (row: any) => {
  formData.id = row.id
  formData.name = row.name
  formData.serverUrl = row.serverUrl
  formData.topic = row.topic
  formData.description = row.description || ''
  formData.status = row.status
  formData.appScope = row.appScope || 'global'
  formData.vendorIds = [...(row.vendorIds || [])]
  formData.deviceIds = [...(row.deviceIds || [])]
  formData.scriptCode = row.scriptCode || ''
}

// 应用范围变化
const handleAppScopeChange = () => {
  formData.vendorIds = []
  formData.deviceIds = []
}

// 测试脚本
const handleTest = () => {
  testData.value = JSON.stringify({
    topic: formData.topic || '$dp',
    payload: {
      deviceId: 'test001',
      timestamp: Date.now(),
      data: {
        temperature: 25.5,
        humidity: 60
      }
    }
  }, null, 2)
  testResult.value = ''
  testDialogVisible.value = true
  emit('test', formData)
}

// 运行测试
const handleRunTest = () => {
  if (!testData.value) {
    ElMessage.warning('请输入测试数据')
    return
  }

  testRunning.value = true
  const t1 = setTimeout(() => {
    try {
      const data = JSON.parse(testData.value)
      testResult.value = JSON.stringify({
        success: true,
        timestamp: new Date().toISOString(),
        input: data,
        output: {
          strategyName: formData.name,
          parsedData: data.payload?.data || {},
          status: 'parsed'
        }
      }, null, 2)
      ElMessage.success('测试运行成功')
    } catch (e) {
      testResult.value = JSON.stringify({
        success: false,
        error: '测试数据格式错误，请输入有效的JSON'
      }, null, 2)
      ElMessage.error('测试失败')
    }
    testRunning.value = false
  }, 1000)
  formTimers.push(t1)
}

// 提交表单
const handleSubmit = () => {
  formRef.value?.validate(async (valid: boolean) => {
    if (!valid) return

    submitLoading.value = true
    const t2 = setTimeout(() => {
      const submitData = { ...formData }
      emit('submit', submitData)
      dialogVisible.value = false
      submitLoading.value = false
      ElMessage.success(props.mode === 'add' ? '新增成功' : '保存成功')
    }, 500)
    formTimers.push(t2)
  })
}
</script>

<style scoped>
.parse-form-dialog :deep(.el-dialog__body) {
  padding: 0 20px 20px;
}

.parse-form-dialog :deep(.el-tabs--border-card) {
  border: none;
  box-shadow: none;
}

.parse-form-dialog :deep(.el-tabs--border-card > .el-tabs__header) {
  background-color: #f5f7fa;
  margin: 0 0 16px;
}

.script-editor-container {
  min-height: 450px;
}

.editor-tabs {
  margin-bottom: 15px;
  text-align: right;
}

.blockly-container {
  display: flex;
  gap: 15px;
  height: 400px;
}

.blockly-workspace {
  flex: 1;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}

.blockly-placeholder {
  color: rgba(255, 255, 255, 0.7);
  font-size: 16px;
}

.toolbox-container {
  width: 280px;
  background: #f5f7fa;
  border-radius: 8px;
  padding: 15px;
  overflow-y: auto;
}

.toolbox-title {
  font-size: 14px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 15px;
  padding-bottom: 10px;
  border-bottom: 2px solid #409eff;
}

.toolbox-items {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.toolbox-category {
  background: #fff;
  border-radius: 6px;
  padding: 10px;
}

.category-title {
  font-size: 13px;
  font-weight: bold;
  color: #409eff;
  margin-bottom: 8px;
}

.tool-item {
  font-size: 12px;
  color: #606266;
  padding: 6px 10px;
  margin: 4px 0;
  background: #f5f7fa;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
}

.tool-item:hover {
  background: #ecf5ff;
  color: #409eff;
}

.code-editor-container {
  height: 400px;
}

.code-textarea :deep(textarea) {
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.6;
}
</style>