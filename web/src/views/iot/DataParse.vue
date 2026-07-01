<template>
  <div class="page data-parse-page">
    <div class="header">
      <div class="header__left">
        <h2 class="header__title">数据解析</h2>
        <span class="header__subtitle">MQTT 消息解析策略配置与管理</span>
      </div>
      <div class="header__right">
        <el-button type="primary" @click="handleAdd">+ 新增解析策略</el-button>
        <el-button @click="handleRefresh" :loading="refreshing">刷新</el-button>
      </div>
    </div>

    <div class="search">
      <el-input
          v-model="searchKeyword"
          placeholder="搜索策略名称或主题"
          clearable
          @clear="handleSearch"
          @keyup.enter="handleSearch"
      />
      <el-select v-model="searchStatus" placeholder="启用状态" clearable>
        <el-option label="启用" :value="1" />
        <el-option label="停用" :value="0" />
      </el-select>
      <el-button type="primary" @click="handleSearch">搜索</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>

    <div class="main-card">
      <div class="cards-container" v-loading="loading">
        <el-empty v-if="tableData.length === 0" description="暂无解析策略" />
        <el-row :gutter="20">
          <el-col :xs="24" :sm="12" :md="8" :lg="6" v-for="item in tableData" :key="item.id">
            <div class="parse-card">
              <div class="card-header">
                <div class="card-title">
                  <span class="title-text">{{ item.name }}</span>
                  <el-tag :type="item.status === 1 ? 'success' : 'info'" size="small" class="status-tag">
                    {{ item.status === 1 ? '启用' : '停用' }}
                  </el-tag>
                </div>
                <div class="card-actions">
                  <el-dropdown trigger="hover" @command="(cmd: string) => handleCardCommand(cmd, item)">
                    <el-button type="primary" link size="small">
                      <el-icon><MoreFilled /></el-icon>
                    </el-button>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item command="view">查看详情</el-dropdown-item>
                        <el-dropdown-item command="edit">编辑</el-dropdown-item>
                        <el-dropdown-item command="log">运行日志</el-dropdown-item>
                        <el-dropdown-item command="duplicate">复制</el-dropdown-item>
                        <el-dropdown-item command="toggle" v-if="item.status === 1">
                          <span style="color: #e6a23c">停用</span>
                        </el-dropdown-item>
                        <el-dropdown-item command="toggle" v-else>
                          <span style="color: #67c23a">启用</span>
                        </el-dropdown-item>
                        <el-dropdown-item command="delete" divided>
                          <span style="color: #f56c6c">删除</span>
                        </el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </div>
              </div>

              <div class="card-body">
                <div class="info-row">
                  <span class="info-label">协议:</span>
                  <span class="info-value">
                    <el-tag size="small" type="warning">{{ item.sourceType || '-' }}</el-tag>
                  </span>
                </div>
                <div class="info-row">
                  <span class="info-label">主题:</span>
                  <span class="info-value topic">{{ item.topic || '-' }}</span>
                </div>
                <div class="info-row">
                  <span class="info-label">描述:</span>
                  <span class="info-value desc">{{ item.description || '-' }}</span>
                </div>
                <div class="info-row">
                  <span class="info-label">应用范围:</span>
                  <span class="info-value">{{ getAppScopeText(item) }}</span>
                </div>
                <div class="info-row">
                  <span class="info-label">最近运行:</span>
                  <span class="info-value">{{ formatTime(item.lastRunTime) }}</span>
                </div>
              </div>

              <div class="card-footer">
                <el-button type="primary" link size="small" @click="handleView(item)">查看</el-button>
                <el-button type="primary" link size="small" @click="handleEdit(item)">编辑</el-button>
                <el-button type="primary" link size="small" @click="handleLog(item)">日志</el-button>
                <el-button type="primary" link size="small" @click="handleTest(item)">测试</el-button>
              </div>
            </div>
          </el-col>
        </el-row>
      </div>

      <div class="pagination-bar">
        <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :total="total"
            :page-sizes="[12, 24, 48, 96]"
            layout="total, sizes, prev, pager, next, jumper"
            prev-text="上一页"
            next-text="下一页"
            :disabled="total === 0"
            @size-change="handleSizeChange"
            @current-change="handlePageChange"
        />
      </div>
    </div>

    <!-- 日志弹窗 -->
    <el-dialog
        v-model="logDialogVisible"
        :title="`运行日志 — ${currentLogStrategy?.name || ''}`"
        width="90%"
        :close-on-click-modal="false"
        destroy-on-close
        class="log-dialog"
    >
      <div class="log-filter">
        <el-date-picker
            v-model="logDateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            :disabled-date="disabledDate"
            value-format="YYYY-MM-DD HH:mm:ss"
        />
        <el-select v-model="logLevel" placeholder="日志级别" clearable style="width: 120px;">
          <el-option label="全部" value="" />
          <el-option label="INFO" value="INFO" />
          <el-option label="WARN" value="WARN" />
          <el-option label="ERROR" value="ERROR" />
        </el-select>
        <el-button type="primary" @click="handleLogSearch">查询</el-button>
        <el-button @click="handleLogExport">导出</el-button>
      </div>

      <div class="log-table-container">
        <el-table :data="logList" border stripe height="400" v-loading="logLoading">
          <el-table-column prop="createTime" label="时间" width="180" />
          <el-table-column prop="logLevel" label="级别" width="100">
            <template #default="{ row }">
              <el-tag :type="getLogLevelType(row.logLevel)" size="small">{{ row.logLevel }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="message" label="日志内容" min-width="300" show-overflow-tooltip />
          <el-table-column prop="executionTime" label="耗时(ms)" width="100" />
          <el-table-column prop="data" label="数据" min-width="120">
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
            @size-change="handleLogSizeChange"
            @current-change="handleLogPageChange"
        />
      </div>

      <template #footer>
        <el-button @click="logDialogVisible = false">关闭</el-button>
        <el-button @click="handleLogClear" type="danger">清空日志</el-button>
      </template>
    </el-dialog>

    <!-- 测试弹窗 -->
    <ScriptTestDialog
        v-model:visible="testDialogVisible"
        :script-code="testStrategyScript"
        :default-topic="testDefaultTopic"
    />

    <!-- 查看详情组件 -->
    <DataParseDetail
        v-model:visible="detailDialogVisible"
        :data="currentDetailData"
        @test="handleTestFromDetail"
    />

    <!-- 新增/编辑表单组件 -->
    <DataParseForm
        v-model:visible="formDialogVisible"
        :data="currentFormData"
        :mode="formMode"
        @saved="handleFormSaved"
        @test="handleTestFromForm"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { MoreFilled } from '@element-plus/icons-vue'
import DataParseDetail from './components/DataParseDetail.vue'
import DataParseForm from './components/DataParseForm.vue'
import ScriptTestDialog from './components/ScriptTestDialog.vue'
import {
  getStrategyPage, getStrategyDetail, createStrategy, updateStrategy,
  deleteStrategy, toggleStrategyStatus, copyStrategy,
  getStrategyLogs, clearStrategyLogs,
  type DataParseStrategy, type DataParseLog
} from '@/api/dataParse'

// ============ 弹窗状态 ============
const detailDialogVisible = ref(false)
const currentDetailData = ref<DataParseStrategy | null>(null)
const formDialogVisible = ref(false)
const formMode = ref<'add' | 'edit' | 'view'>('add')
const currentFormData = ref<DataParseStrategy | null>(null)

// ============ 列表状态 ============
const loading = ref(false)
const refreshing = ref(false)

const searchKeyword = ref('')
const searchStatus = ref<number | ''>('')
const currentPage = ref(1)
const pageSize = ref(12)
const total = ref(0)
const tableData = ref<DataParseStrategy[]>([])

// ============ 日志弹窗状态 ============
const logDialogVisible = ref(false)
const logDateRange = ref([] as string[])
const logLevel = ref('')
const logCurrentPage = ref(1)
const logPageSize = ref(20)
const logTotal = ref(0)
const logList = ref<DataParseLog[]>([])
const logLoading = ref(false)
const currentLogStrategy = ref<DataParseStrategy | null>(null)

// ============ 测试弹窗状态 ============
const testDialogVisible = ref(false)
const testStrategyScript = ref('')
const testDefaultTopic = ref('')

// ============ 工具函数 ============
const getAppScopeText = (item: DataParseStrategy) => {
  const scopeMap: Record<string, string> = {
    global: '全局',
    vendor: `指定厂商 (${item.vendorIds?.length || 0}个)`,
    device: `指定设备 (${item.deviceIds?.length || 0}个)`
  }
  return scopeMap[item.appScope] || '全局'
}

const getLogLevelType = (level: string) => {
  const typeMap: Record<string, string> = {
    INFO: 'info',
    WARN: 'warning',
    ERROR: 'danger'
  }
  return typeMap[level] || 'info'
}

const formatTime = (t?: string) => {
  if (!t) return '-'
  // 后端 datetime 字符串已是 'YYYY-MM-DD HH:mm:ss'
  return t.replace('T', ' ').substring(0, 19)
}

const disabledDate = (time: Date) => time.getTime() > Date.now()

// ============ 列表加载 ============
const loadData = async () => {
  loading.value = true
  try {
    const result = await getStrategyPage({
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      keyword: searchKeyword.value || undefined,
      status: searchStatus.value === '' ? undefined : searchStatus.value
    })
    tableData.value = result.rows
    total.value = result.total
  } catch (e: any) {
    ElMessage.error(e.message || '加载策略列表失败')
    tableData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  loadData()
}

const handleReset = () => {
  searchKeyword.value = ''
  searchStatus.value = ''
  currentPage.value = 1
  loadData()
}

const handleRefresh = async () => {
  refreshing.value = true
  await loadData()
  refreshing.value = false
}

const handleSizeChange = () => loadData()
const handlePageChange = () => loadData()

// ============ CRUD 操作 ============
const handleAdd = () => {
  formMode.value = 'add'
  currentFormData.value = null
  formDialogVisible.value = true
}

const handleView = async (row: DataParseStrategy) => {
  try {
    // 详情接口回填 vendorIds/deviceIds
    const detail = await getStrategyDetail(row.id)
    currentDetailData.value = detail
  } catch {
    currentDetailData.value = row
  }
  detailDialogVisible.value = true
}

const handleEdit = async (row: DataParseStrategy) => {
  try {
    const detail = await getStrategyDetail(row.id)
    currentFormData.value = detail
  } catch {
    currentFormData.value = row
  }
  formMode.value = 'edit'
  formDialogVisible.value = true
}

const handleFormSaved = () => {
  formDialogVisible.value = false
  loadData()
}

const handleDuplicate = (row: DataParseStrategy) => {
  ElMessageBox.confirm('确定要复制此解析策略吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'info'
  }).then(async () => {
    try {
      await copyStrategy(row.id)
      ElMessage.success('复制成功')
      loadData()
    } catch (e: any) {
      ElMessage.error(e.message || '复制失败')
    }
  }).catch(() => {})
}

const handleToggle = (row: DataParseStrategy) => {
  const newStatus = row.status === 1 ? 0 : 1
  const action = newStatus === 1 ? '启用' : '停用'
  ElMessageBox.confirm(`确定要${action}此解析策略吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await toggleStrategyStatus(row.id, newStatus)
      ElMessage.success(`${action}成功`)
      loadData()
    } catch (e: any) {
      ElMessage.error(e.message || `${action}失败`)
    }
  }).catch(() => {})
}

const handleDelete = (row: DataParseStrategy) => {
  ElMessageBox.confirm('确定要删除此解析策略吗？删除后不可恢复！', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteStrategy(row.id)
      ElMessage.success('删除成功')
      loadData()
    } catch (e: any) {
      ElMessage.error(e.message || '删除失败')
    }
  }).catch(() => {})
}

const handleCardCommand = (cmd: string, row: DataParseStrategy) => {
  switch (cmd) {
    case 'view': handleView(row); break
    case 'edit': handleEdit(row); break
    case 'log': handleLog(row); break
    case 'duplicate': handleDuplicate(row); break
    case 'toggle': handleToggle(row); break
    case 'delete': handleDelete(row); break
  }
}

// ============ 日志相关 ============
const handleLog = (row: DataParseStrategy) => {
  currentLogStrategy.value = row
  logDialogVisible.value = true
  logDateRange.value = []
  logLevel.value = ''
  logCurrentPage.value = 1
  loadLogs()
}

const loadLogs = async () => {
  if (!currentLogStrategy.value) return
  logLoading.value = true
  try {
    const [startTime, endTime] = logDateRange.value.length === 2 ? logDateRange.value : ['', '']
    const result = await getStrategyLogs(currentLogStrategy.value.id, {
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

const handleLogExport = () => {
  // 导出当前日志为 JSON 文件
  if (!logList.value.length) {
    ElMessage.warning('暂无日志可导出')
    return
  }
  const blob = new Blob([JSON.stringify(logList.value, null, 2)], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `parse-logs-${currentLogStrategy.value?.id || 'unknown'}-${Date.now()}.json`
  a.click()
  URL.revokeObjectURL(url)
  ElMessage.success('导出成功')
}

const handleLogClear = () => {
  if (!currentLogStrategy.value) return
  ElMessageBox.confirm('确定要清空此策略的运行日志吗？', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await clearStrategyLogs(currentLogStrategy.value!.id)
      ElMessage.success('日志已清空')
      loadLogs()
    } catch (e: any) {
      ElMessage.error(e.message || '清空日志失败')
    }
  }).catch(() => {})
}

const showLogData = (row: DataParseLog) => {
  ElMessageBox.alert(row.data || '-', '日志数据', {
    confirmButtonText: '关闭',
    customClass: 'log-data-dialog'
  })
}

const handleLogSizeChange = () => loadLogs()
const handleLogPageChange = () => loadLogs()

// ============ 测试相关 ============
const handleTest = (row?: DataParseStrategy) => {
  if (row) {
    testDefaultTopic.value = row.topic || 'sys/v1/DEV001/S001/updata'
    testStrategyScript.value = row.scriptCode || ''
  } else {
    testStrategyScript.value = ''
    testDefaultTopic.value = ''
  }
  testDialogVisible.value = true
}

const handleTestFromDetail = (data: DataParseStrategy) => handleTest(data)
const handleTestFromForm = (data: DataParseStrategy) => handleTest(data)

// ============ 生命周期 ============
onMounted(() => {
  loadData()
})
</script>

<style scoped>
.main-card {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.cards-container {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 15px 20px;
}

.parse-card {
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  margin-bottom: 20px;
  transition: all 0.3s;
  overflow: hidden;
}

.parse-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  min-width: 0;
}

.title-text {
  font-size: 16px;
  font-weight: bold;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.status-tag {
  flex-shrink: 0;
}

.card-actions {
  flex-shrink: 0;
}

.card-body {
  padding: 15px;
}

.info-row {
  display: flex;
  margin-bottom: 10px;
  font-size: 13px;
}

.info-label {
  color: #909399;
  width: 80px;
  flex-shrink: 0;
}

.info-value {
  color: #303133;
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.info-value.topic {
  color: #409eff;
  font-family: monospace;
}

.info-value.desc {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.card-footer {
  display: flex;
  gap: 5px;
  padding: 10px 15px;
  border-top: 1px solid #f0f0f0;
  background: #fafafa;
}

.card-footer .el-button {
  flex: 1;
}

.log-filter {
  display: flex;
  gap: 12px;
  margin-bottom: 15px;
  flex-wrap: wrap;
}

.log-table-container {
  margin-bottom: 15px;
}

.log-pagination {
  display: flex;
  justify-content: flex-end;
}
</style>
