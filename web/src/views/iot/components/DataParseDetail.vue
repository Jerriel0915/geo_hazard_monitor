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
          <el-descriptions-item label="协议类型">
            <el-tag size="small" type="warning">{{ currentData?.sourceType || '-' }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="服务地址">{{ currentData?.serverUrl || '-' }}</el-descriptions-item>
          <el-descriptions-item label="订阅主题">{{ currentData?.topic || '-' }}</el-descriptions-item>
          <el-descriptions-item label="启用状态">
            <el-tag :type="currentData?.status === 1 ? 'success' : 'info'" size="small">
              {{ currentData?.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="应用范围">{{ appScopeText }}</el-descriptions-item>
          <el-descriptions-item label="最近运行">{{ formatTime(currentData?.lastRunTime) }}</el-descriptions-item>
          <el-descriptions-item label="预置策略">
            <el-tag :type="currentData?.isPreset === 1 ? 'success' : 'info'" size="small">
              {{ currentData?.isPreset === 1 ? '是' : '否' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">{{ currentData?.description || '-' }}</el-descriptions-item>
        </el-descriptions>

        <!-- 指定设备时显示设备 ID 列表 -->
        <template v-if="currentData?.appScope === 'device' && currentData?.deviceIds?.length">
          <el-divider content-position="left">指定设备 ({{ currentData.deviceIds.length }} 个)</el-divider>
          <div class="scope-tags">
            <el-tag v-for="did in currentData.deviceIds" :key="did" class="scope-tag">设备 #{{ did }}</el-tag>
          </div>
        </template>
      </el-tab-pane>

      <el-tab-pane label="解析脚本" name="script">
        <div class="script-viewer">
          <div class="script-header">
            <span class="script-title">Groovy 解析脚本</span>
            <el-button size="small" text type="primary" @click="copyScript">
              <el-icon><CopyDocument /></el-icon>
              复制代码
            </el-button>
          </div>
          <div class="script-code-wrapper">
            <CodeMirrorGroovy
                v-model="scriptDisplay"
                readonly
                :min-height="280"
            />
          </div>
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
          <el-select v-model="logLevel" placeholder="日志级别" clearable size="small" style="width: 110px;">
            <el-option label="全部" value="" />
            <el-option label="INFO" value="INFO" />
            <el-option label="WARN" value="WARN" />
            <el-option label="ERROR" value="ERROR" />
          </el-select>
          <el-button type="primary" size="small" @click="handleLogSearch">查询</el-button>
        </div>

        <div class="log-table-container">
          <el-table :data="logList" border stripe size="small" height="350" v-loading="logLoading">
            <el-table-column prop="createTime" label="时间" width="170" />
            <el-table-column prop="logLevel" label="级别" width="90">
              <template #default="{ row }">
                <el-tag :type="getLogLevelType(row.logLevel)" size="small">{{ row.logLevel }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="message" label="日志内容" min-width="280" show-overflow-tooltip />
            <el-table-column prop="executionTime" label="耗时(ms)" width="90" />
            <el-table-column label="数据" width="90">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="showLogData(row)" v-if="row.data">查看</el-button>
                <span v-else>-</span>
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
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CopyDocument } from '@element-plus/icons-vue'
import CodeMirrorGroovy from '@/views/basic/components/script-editor/CodeMirrorGroovy.vue'
import { getStrategyLogs, type DataParseStrategy, type DataParseLog } from '@/api/dataParse'

interface Props {
  visible: boolean
  data: DataParseStrategy | null
}

interface Emits {
  (e: 'update:visible', value: boolean): void
  (e: 'test', data: DataParseStrategy): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const dialogVisible = ref(false)
const activeTab = ref('basic')
const currentData = ref<DataParseStrategy | null>(null)

// 脚本只读展示（CodeMirrorGroovy 需要 v-model）
const scriptDisplay = computed(() => currentData.value?.scriptCode || '')

// 日志相关
const logLoading = ref(false)
const logDateRange = ref([] as string[])
const logLevel = ref('')
const logCurrentPage = ref(1)
const logPageSize = ref(20)
const logTotal = ref(0)
const logList = ref<DataParseLog[]>([])

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

const formatTime = (t?: string) => {
  if (!t) return '-'
  return t.replace('T', ' ').substring(0, 19)
}

const getLogLevelType = (level: string) => {
  const typeMap: Record<string, string> = {
    INFO: 'info',
    WARN: 'warning',
    ERROR: 'danger'
  }
  return typeMap[level] || 'info'
}

const disabledDate = (time: Date) => time.getTime() > Date.now()

watch(() => props.visible, (val) => {
  dialogVisible.value = val
  if (val && props.data) {
    currentData.value = props.data
    activeTab.value = 'basic'
    logList.value = []
    logTotal.value = 0
    logCurrentPage.value = 1
  }
})

watch(() => dialogVisible.value, (val) => {
  if (!val) {
    activeTab.value = 'basic'
    currentData.value = null
    logList.value = []
    emit('update:visible', val)
  }
})

// 切到日志 Tab 时自动加载
watch(() => activeTab.value, (val) => {
  if (val === 'log' && currentData.value && logList.value.length === 0) {
    loadLogs()
  }
})

const loadLogs = async () => {
  if (!currentData.value?.id) return
  logLoading.value = true
  try {
    const [startTime, endTime] = logDateRange.value.length === 2 ? logDateRange.value : ['', '']
    const result = await getStrategyLogs(currentData.value.id, {
      pageNum: logCurrentPage.value,
      pageSize: logPageSize.value,
      logLevel: logLevel.value || undefined,
      startTime: startTime || undefined,
      endTime: endTime || undefined
    })
    logList.value = result.rows
    logTotal.value = result.total
  } catch (e: any) {
    ElMessage.error(e.message || '加载日志失败')
    logList.value = []
    logTotal.value = 0
  } finally {
    logLoading.value = false
  }
}

const handleLogSearch = () => {
  logCurrentPage.value = 1
  loadLogs()
}

const handleLogSizeChange = () => loadLogs()
const handleLogPageChange = () => loadLogs()

const showLogData = (row: DataParseLog) => {
  ElMessageBox.alert(row.data || '-', '日志数据', {
    confirmButtonText: '关闭'
  })
}

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

const handleTest = () => {
  if (currentData.value) {
    emit('test', currentData.value)
  }
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

.script-code-wrapper {
  border-radius: 0 0 4px 4px;
  overflow: hidden;
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
