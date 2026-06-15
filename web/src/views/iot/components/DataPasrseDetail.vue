<!-- 数据解析查看详情 -->
<template>
  <el-dialog
      v-model="dialogVisible"
      :title="`解析策略详情 — ${currentData?.name || ''}`"
      width="1000px"
      :close-on-click-modal="false"
      destroy-on-close
      class="parse-detail-dialog"
  >
    <el-tabs v-model="activeTab" type="border-card">
      <el-tab-pane label="基本信息" name="basic">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="策略名称">{{ currentData?.name || '-' }}</el-descriptions-item>
          <el-descriptions-item label="服务地址">{{ currentData?.serverUrl || '-' }}</el-descriptions-item>
          <el-descriptions-item label="主题">{{ currentData?.topic || '-' }}</el-descriptions-item>
          <el-descriptions-item label="启用状态">
            <el-tag :type="currentData?.status === 1 ? 'success' : 'info'" size="small">
              {{ currentData?.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="应用范围">{{ appScopeText }}</el-descriptions-item>
          <el-descriptions-item label="最近运行">{{ currentData?.lastRunTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">{{ currentData?.description || '-' }}</el-descriptions-item>
        </el-descriptions>

        <!-- 指定厂商时显示厂商列表 -->
        <template v-if="currentData?.appScope === 'vendor' && currentData?.vendorNames?.length">
          <el-divider content-position="left">指定厂商</el-divider>
          <div class="scope-tags">
            <el-tag v-for="name in currentData.vendorNames" :key="name" class="scope-tag">{{ name }}</el-tag>
          </div>
        </template>

        <!-- 指定设备时显示设备列表 -->
        <template v-if="currentData?.appScope === 'device' && currentData?.deviceNames?.length">
          <el-divider content-position="left">指定设备</el-divider>
          <div class="scope-tags">
            <el-tag v-for="name in currentData.deviceNames" :key="name" class="scope-tag">{{ name }}</el-tag>
          </div>
        </template>
      </el-tab-pane>

      <el-tab-pane label="解析脚本" name="script">
        <div class="script-viewer">
          <div class="script-header">
            <span class="script-title">JavaScript 解析脚本</span>
            <el-button size="small" text type="primary" @click="copyScript">
              <el-icon><CopyDocument /></el-icon>
              复制代码
            </el-button>
          </div>
          <pre class="script-code">{{ currentData?.scriptCode || '暂无脚本代码' }}</pre>
        </div>
      </el-tab-pane>

      <el-tab-pane label="运行日志" name="log" lazy>
        <div class="log-filter">
          <el-date-picker
              v-model="logDateRange"
              type="datetimerange"
              range-separator="至"
              start-placeholder="开始时间"
              end-placeholder="结束时间"
              :disabled-date="disabledDate"
              value-format="YYYY-MM-DD HH:mm:ss"
              size="small"
          />
          <el-select v-model="logLevel" placeholder="日志级别" clearable size="small">
            <el-option label="全部" value="" />
            <el-option label="INFO" value="INFO" />
            <el-option label="WARN" value="WARN" />
            <el-option label="ERROR" value="ERROR" />
          </el-select>
          <el-button type="primary" size="small" @click="handleLogSearch">查询</el-button>
          <el-button size="small" @click="handleLogExport">导出</el-button>
        </div>

        <div class="log-table-container">
          <el-table :data="logList" border stripe size="small" height="350" v-loading="logLoading">
            <el-table-column prop="timestamp" label="时间" width="170" />
            <el-table-column prop="level" label="级别" width="90">
              <template #default="{ row }">
                <el-tag :type="getLogLevelType(row.level)" size="small">{{ row.level }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="message" label="日志内容" min-width="280" show-overflow-tooltip />
            <el-table-column prop="data" label="数据" width="100">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="showLogData(row)">查看详情</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div class="log-pagination">
          <el-pagination
              v-model:current-page="logCurrentPage"
              v-model:page-size="logPageSize"
              :total="logTotal"
              :page-sizes="[20, 50, 100]"
              layout="total, sizes, prev, pager, next, jumper"
              size="small"
              @size-change="handleLogSizeChange"
              @current-change="handleLogPageChange"
          />
        </div>
      </el-tab-pane>
    </el-tabs>

    <template #footer>
      <el-button @click="handleTest">测试脚本</el-button>
      <el-button type="primary" @click="dialogVisible = false">关闭</el-button>
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
import { ref, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CopyDocument } from '@element-plus/icons-vue'

interface Props {
  visible: boolean
  data: any
}

interface Emits {
  (e: 'update:visible', value: boolean): void
  (e: 'test', data: any): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const dialogVisible = ref(false)
const activeTab = ref('basic')
const currentData = ref<any>(null)

// 日志相关
const logLoading = ref(false)
const logDateRange = ref([] as string[])
const logLevel = ref('')
const logCurrentPage = ref(1)
const logPageSize = ref(20)
const logTotal = ref(0)
const logList = ref<any[]>([])

// 测试相关
const testDialogVisible = ref(false)
const testData = ref('')
const testResult = ref('')
const testRunning = ref(false)

// 应用范围文本
const appScopeText = computed(() => {
  const data = currentData.value
  if (!data) return '-'
  const scopeMap: Record<string, string> = {
    global: '全局',
    vendor: `指定厂商 (${data.vendorIds?.length || 0}个)`,
    device: `指定设备 (${data.deviceIds?.length || 0}个)`
  }
  return scopeMap[data.appScope] || '全局'
})

// 监听 visible 变化
watch(() => props.visible, (val) => {
  dialogVisible.value = val
  if (val && props.data) {
    currentData.value = props.data
    activeTab.value = 'basic'
    loadMockLogs()
  }
})

watch(() => dialogVisible.value, (val) => {
  if (!val) {
    activeTab.value = 'basic'
    currentData.value = null
    logList.value = []
  }
  emit('update:visible', val)
})

// 加载模拟日志数据
const loadMockLogs = () => {
  logLoading.value = true
  setTimeout(() => {
    logList.value = [
      { timestamp: '2026-06-08 14:30:25', level: 'INFO', message: '接收到MQTT消息，主题: ' + (currentData.value?.topic || '-'), data: '{"topic":"$dp","payload":"..."}' },
      { timestamp: '2026-06-08 14:30:26', level: 'INFO', message: '解析成功，策略: ' + (currentData.value?.name || '-'), data: '{"strategyId":1,"status":"success"}' },
      { timestamp: '2026-06-08 14:30:27', level: 'INFO', message: '数据已存储', data: '{}' },
    ]
    logTotal.value = logList.value.length
    logLoading.value = false
  }, 300)
}

// 复制脚本
const copyScript = async () => {
  if (currentData.value?.scriptCode) {
    try {
      await navigator.clipboard.writeText(currentData.value.scriptCode)
      ElMessage.success('脚本代码已复制到剪贴板')
    } catch {
      ElMessage.error('复制失败')
    }
  } else {
    ElMessage.warning('暂无脚本代码')
  }
}

// 测试脚本
const handleTest = () => {
  if (currentData.value) {
    testData.value = JSON.stringify({
      topic: currentData.value.topic || '$dp',
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
    emit('test', currentData.value)
  }
}

// 运行测试
const handleRunTest = () => {
  if (!testData.value) {
    ElMessage.warning('请输入测试数据')
    return
  }

  testRunning.value = true
  setTimeout(() => {
    try {
      const data = JSON.parse(testData.value)
      testResult.value = JSON.stringify({
        success: true,
        timestamp: new Date().toISOString(),
        input: data,
        output: {
          strategyName: currentData.value?.name,
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
}

// 日志相关方法
const getLogLevelType = (level: string) => {
  const typeMap: Record<string, string> = {
    INFO: 'info',
    WARN: 'warning',
    ERROR: 'danger'
  }
  return typeMap[level] || 'info'
}

const handleLogSearch = () => {
  loadMockLogs()
  ElMessage.info('查询日志')
}

const handleLogExport = () => {
  ElMessage.info('导出日志')
}

const showLogData = (row: any) => {
  ElMessageBox.alert(row.data || '-', '日志数据', {
    confirmButtonText: '关闭'
  })
}

const handleLogSizeChange = () => {
  loadMockLogs()
}

const handleLogPageChange = () => {
  loadMockLogs()
}

const disabledDate = (time: Date) => {
  return time.getTime() > Date.now()
}
</script>

<style scoped>
.parse-detail-dialog :deep(.el-dialog__body) {
  padding: 0 20px 20px;
}

.parse-detail-dialog :deep(.el-tabs--border-card) {
  border: none;
  box-shadow: none;
}

.parse-detail-dialog :deep(.el-tabs--border-card > .el-tabs__header) {
  background-color: #f5f7fa;
  margin: 0 0 16px;
}

.scope-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}

.scope-tag {
  margin: 0;
}

.script-viewer {
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  overflow: hidden;
}

.script-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 15px;
  background: #f5f7fa;
  border-bottom: 1px solid #e4e7ed;
}

.script-title {
  font-size: 13px;
  font-weight: 500;
  color: #303133;
}

.script-code {
  margin: 0;
  padding: 15px;
  background: #1e1e1e;
  color: #d4d4d4;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.6;
  overflow-x: auto;
  white-space: pre-wrap;
  word-wrap: break-word;
  max-height: 400px;
  overflow-y: auto;
}

.log-filter {
  display: flex;
  gap: 10px;
  margin-bottom: 15px;
  flex-wrap: wrap;
  align-items: center;
}

.log-table-container {
  margin-bottom: 15px;
}

.log-pagination {
  display: flex;
  justify-content: flex-end;
}
</style>