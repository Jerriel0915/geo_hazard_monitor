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
          <el-form-item label="协议类型" prop="sourceType">
            <el-select v-model="formData.sourceType" placeholder="请选择协议" :disabled="isView" style="width: 100%">
              <el-option label="sys (系统自定义协议)" value="sys" />
              <el-option label="gb (国标协议)" value="gb" />
              <el-option label="自定义协议" value="custom" />
            </el-select>
          </el-form-item>
          <el-form-item label="服务地址" prop="serverUrl">
            <el-input v-model="formData.serverUrl" placeholder="如 tcp://mqtt.server:1883 (描述用)" :disabled="isView" />
          </el-form-item>
          <el-form-item label="订阅主题" prop="topic">
            <el-input v-model="formData.topic" placeholder="如 sys/v1/{deviceCode}/{sensorCode}/updata (描述用)" :disabled="isView" />
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
              <el-radio value="device">指定设备</el-radio>
              <!-- 厂商主表暂未建立，隐藏 -->
            </el-radio-group>
          </el-form-item>
          <el-form-item label="设备选择" prop="deviceIds" v-if="formData.appScope === 'device'">
            <el-select
                v-model="formData.deviceIds"
                multiple
                filterable
                remote
                remote-show-suffix
                :remote-method="searchDevices"
                placeholder="输入设备名称搜索"
                :disabled="isView"
                style="width: 100%"
                :loading="deviceLoading"
            >
              <el-option
                  v-for="device in deviceList"
                  :key="device.id"
                  :label="`${device.name} (${device.code})`"
                  :value="device.id!"
              />
            </el-select>
          </el-form-item>
        </el-form>
      </el-tab-pane>

      <el-tab-pane label="脚本编辑" name="script">
        <div class="script-editor-container">
          <div class="editor-hint">
            <el-alert type="info" :closable="false" show-icon>
              <template #title>
                Groovy 脚本约定：入口函数 <code>Map&lt;String, Object&gt; parse(String topic, byte[] messageBytes)</code>；
                通过 <code>builtin.*</code> 调用内置函数（hexDecode/readFloat/readBcdTimestamp 等）；
                禁用 <code>@CompileStatic</code>；Map 赋值用 <code>result.put("key", value)</code>。
              </template>
            </el-alert>
          </div>
          <div class="code-editor-container">
            <CodeMirrorGroovy
                v-model="formData.scriptCode"
                :readonly="isView"
                :min-height="400"
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
  <ScriptTestDialog
      v-model:visible="testDialogVisible"
      :script-code="formData.scriptCode"
      :default-topic="formData.topic"
  />
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  createStrategy, updateStrategy,
  type DataParseStrategy
} from '@/api/dataParse'
import { getDevicePage, getDeviceDetail, type DeviceItem } from '@/api/device'
import ScriptTestDialog from './ScriptTestDialog.vue'
import CodeMirrorGroovy from '@/views/basic/components/script-editor/CodeMirrorGroovy.vue'

interface Props {
  visible: boolean
  data: DataParseStrategy | null
  mode: 'add' | 'edit' | 'view'
}

interface Emits {
  (e: 'update:visible', value: boolean): void
  (e: 'saved'): void
  (e: 'test', data: DataParseStrategy): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const dialogVisible = ref(false)
const activeTab = ref('basic')
const submitLoading = ref(false)
const formRef = ref()

// 设备下拉（远程搜索）
const deviceList = ref<DeviceItem[]>([])
const deviceLoading = ref(false)

// 测试弹窗
const testDialogVisible = ref(false)

// 表单数据
const formData = reactive({
  id: null as number | null,
  name: '',
  sourceType: 'sys',
  serverUrl: '',
  topic: '',
  description: '',
  status: 1,
  appScope: 'global' as 'global' | 'vendor' | 'device',
  vendorIds: [] as number[],
  deviceIds: [] as number[],
  scriptCode: ''
})

// 表单验证规则
const formRules = {
  name: [{ required: true, message: '请输入策略名称', trigger: 'blur' }],
  sourceType: [{ required: true, message: '请选择协议类型', trigger: 'change' }],
  status: [{ required: true, message: '请选择启用状态', trigger: 'change' }]
}


// 弹窗标题
const dialogTitle = computed(() => {
  if (props.mode === 'add') return '新增解析策略'
  if (props.mode === 'edit') return '编辑解析策略'
  return '查看解析策略'
})

const isView = computed(() => props.mode === 'view')

// 监听 visible 变化
watch(() => props.visible, (val) => {
  dialogVisible.value = val
  if (val) {
    if (props.data) {
      fillForm(props.data)
      // 编辑/查看模式：回填已选中设备的名称
      if (props.data.deviceIds?.length) {
        loadSelectedDevices(props.data.deviceIds)
      } else {
        deviceList.value = []
      }
    } else {
      resetForm()
      deviceList.value = []
    }
    activeTab.value = 'basic'
  }
})

watch(() => dialogVisible.value, (val) => {
  if (!val) {
    emit('update:visible', val)
  }
})

// 加载已选中设备（编辑模式回填名称）
const loadSelectedDevices = async (deviceIds: number[]) => {
  if (!deviceIds.length) {
    deviceList.value = []
    return
  }
  deviceLoading.value = true
  try {
    const results = await Promise.allSettled(deviceIds.map(id => getDeviceDetail(id)))
    const devices: DeviceItem[] = []
    results.forEach(r => {
      if (r.status === 'fulfilled') devices.push(r.value)
    })
    deviceList.value = devices
  } catch {
    deviceList.value = []
  } finally {
    deviceLoading.value = false
  }
}

// 远程搜索设备（按名称过滤，分页取前 50 条）
let searchTimer: ReturnType<typeof setTimeout> | null = null
const searchDevices = (query: string) => {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(async () => {
    const trimmed = query.trim()
    if (!trimmed) {
      // 空查询时保留已选中设备的选项
      return
    }
    deviceLoading.value = true
    try {
      const result = await getDevicePage({ pageNum: 1, pageSize: 50, name: trimmed })
      // 合并：保留已选中但不在搜索结果中的设备，避免选中项丢失标签
      const existingIds = new Set(deviceList.value.map(d => d.id))
      const newDevices = result.rows.filter(d => !existingIds.has(d.id))
      deviceList.value = [...deviceList.value, ...newDevices]
    } catch {
      // 搜索失败时保留现有列表
    } finally {
      deviceLoading.value = false
    }
  }, 300)
}

const resetForm = () => {
  formData.id = null
  formData.name = ''
  formData.sourceType = 'sys'
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

const fillForm = (row: DataParseStrategy) => {
  formData.id = row.id ?? null
  formData.name = row.name || ''
  formData.sourceType = row.sourceType || 'sys'
  formData.serverUrl = row.serverUrl || ''
  formData.topic = row.topic || ''
  formData.description = row.description || ''
  formData.status = row.status ?? 1
  formData.appScope = row.appScope || 'global'
  formData.vendorIds = [...(row.vendorIds || [])]
  formData.deviceIds = [...(row.deviceIds || [])]
  formData.scriptCode = row.scriptCode || ''
}

const handleAppScopeChange = () => {
  formData.vendorIds = []
  formData.deviceIds = []
}

// 测试脚本（打开共享测试弹窗）
const handleTest = () => {
  testDialogVisible.value = true
  emit('test', { ...formData } as DataParseStrategy)
}

// 提交表单
const handleSubmit = () => {
  formRef.value?.validate(async (valid: boolean) => {
    if (!valid) return
    submitLoading.value = true
    try {
      const payload: Partial<DataParseStrategy> = {
        name: formData.name,
        sourceType: formData.sourceType,
        serverUrl: formData.serverUrl || undefined,
        topic: formData.topic || undefined,
        description: formData.description || undefined,
        status: formData.status,
        appScope: formData.appScope,
        scriptCode: formData.scriptCode,
        deviceIds: formData.appScope === 'device' ? formData.deviceIds : [],
        vendorIds: formData.appScope === 'vendor' ? formData.vendorIds : []
      }
      if (formData.id != null) {
        payload.id = formData.id
        await updateStrategy(payload)
        ElMessage.success('保存成功')
      } else {
        await createStrategy(payload)
        ElMessage.success('新增成功')
      }
      dialogVisible.value = false
      emit('saved')
    } catch (e: any) {
      ElMessage.error(e.message || '保存失败')
    } finally {
      submitLoading.value = false
    }
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
  min-height: 480px;
}

.editor-hint {
  margin-bottom: 12px;
}

.editor-hint code {
  background: rgba(0, 0, 0, 0.06);
  padding: 1px 4px;
  border-radius: 3px;
  font-family: 'Consolas', monospace;
  font-size: 12px;
}

.code-editor-container {
  height: 460px;
  border-radius: 4px;
  overflow: hidden;
}
</style>
