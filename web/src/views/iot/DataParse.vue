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
        <el-empty v-if="filteredList.length === 0" description="暂无解析策略" />
        <el-row :gutter="20">
          <el-col :xs="24" :sm="12" :md="8" :lg="6" v-for="item in filteredList" :key="item.id">
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
                  <span class="info-label">服务地址:</span>
                  <span class="info-value">{{ item.serverUrl || '-' }}</span>
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
                  <span class="info-value">{{ item.lastRunTime || '-' }}</span>
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
        title="运行日志"
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
        <el-select v-model="logLevel" placeholder="日志级别" clearable>
          <el-option label="全部" value="" />
          <el-option label="INFO" value="INFO" />
          <el-option label="WARN" value="WARN" />
          <el-option label="ERROR" value="ERROR" />
        </el-select>
        <el-button type="primary" @click="handleLogSearch">查询</el-button>
        <el-button @click="handleLogExport">导出</el-button>
      </div>

      <div class="log-table-container">
        <el-table :data="logList" border stripe height="400">
          <el-table-column prop="timestamp" label="时间" width="180" />
          <el-table-column prop="level" label="级别" width="100">
            <template #default="{ row }">
              <el-tag :type="getLogLevelType(row.level)" size="small">{{ row.level }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="message" label="日志内容" min-width="300" />
          <el-table-column prop="data" label="数据" min-width="200">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click="showLogData(row)">查看</el-button>
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
        <el-button @click="testDialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="handleRunTest" :loading="testRunning">运行测试</el-button>
      </template>
    </el-dialog>

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
        @submit="handleFormSubmit"
        @test="handleTestFromForm"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { MoreFilled } from '@element-plus/icons-vue'
import DataParseDetail from './components/DataPasrseDetail.vue'
import DataParseForm from './components/DataParseForm.vue'

// ============ 弹窗状态 ============
const detailDialogVisible = ref(false)
const currentDetailData = ref<any>(null)
const formDialogVisible = ref(false)
const formMode = ref<'add' | 'edit' | 'view'>('add')
const currentFormData = ref<any>(null)

// ============ 列表状态 ============
const loading = ref(false)
const refreshing = ref(false)
const testRunning = ref(false)

const searchKeyword = ref('')
const searchStatus = ref<number | null>(null)
const currentPage = ref(1)
const pageSize = ref(12)
const total = ref(0)

// ============ 日志弹窗状态 ============
const logDialogVisible = ref(false)
const logDateRange = ref([] as string[])
const logLevel = ref('')
const logCurrentPage = ref(1)
const logPageSize = ref(20)
const logTotal = ref(0)
const currentLogStrategy = ref<any>(null)

// ============ 测试弹窗状态 ============
const testDialogVisible = ref(false)
const testData = ref('')
const testResult = ref('')

// ============ Mock 数据 ============
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

const tableData = ref([
  {
    id: 1,
    name: '国标协议解析',
    serverUrl: 'tcp://mqtt.server:1883',
    topic: '$dp',
    description: '国标协议数据解析策略，支持多厂商设备',
    status: 1,
    appScope: 'global',
    vendorIds: [],
    deviceIds: [],
    lastRunTime: '2026-06-08 14:30:25',
    scriptCode: `// 国标协议解析脚本
function parse(message) {
  const result = {};
  result.timestamp = Date.now();
  result.sourceTopic = message.topic;
  result.payload = message.payload;

  if (message.topic === '$dp') {
    result.type = 'dataPoint';
    result.deviceId = message.payload.deviceId;
    result.data = parseDataPoint(message.payload);
  }

  return result;
}

function parseDataPoint(payload) {
  const data = {};
  data.timestamp = payload.timestamp;
  data.values = payload.values || {};
  return data;
}`
  },
  {
    id: 2,
    name: '北斗智联协议解析',
    serverUrl: 'tcp://mqtt.server:1883',
    topic: '/beidou/+/data',
    description: '北斗智联设备数据解析',
    status: 1,
    appScope: 'vendor',
    vendorIds: [2],
    deviceIds: [],
    lastRunTime: '2026-06-08 14:25:10',
    scriptCode: '// 北斗智联协议解析\nfunction parse(message) {\n  return { deviceId: message.deviceId, data: message.data };\n}'
  },
  {
    id: 3,
    name: '雨量计专用解析',
    serverUrl: 'tcp://mqtt.server:1883',
    topic: '/rainfall/+/data',
    description: '雨量计设备专用解析策略',
    status: 0,
    appScope: 'device',
    vendorIds: [],
    deviceIds: [2],
    lastRunTime: '2026-06-08 12:15:30',
    scriptCode: '// 雨量计解析\nfunction parse(message) {\n  return { deviceId: message.deviceId, rainfall: message.value };\n}'
  }
])

const logList = ref([
  { timestamp: '2026-06-08 14:30:25', level: 'INFO', message: '接收到MQTT消息，主题: $dp', data: '{"topic":"$dp","payload":"..."}' },
  { timestamp: '2026-06-08 14:30:26', level: 'INFO', message: '解析成功，设备ID: dev001', data: '{"deviceId":"dev001","data":"..."}' },
  { timestamp: '2026-06-08 14:30:27', level: 'INFO', message: '数据已存储', data: '{}' },
  { timestamp: '2026-06-08 14:25:10', level: 'WARN', message: '数据格式异常，使用默认值', data: '{"error":"format error"}' },
  { timestamp: '2026-06-08 12:15:30', level: 'ERROR', message: '解析脚本执行失败', data: '{"error":"script error"}' }
])

// ============ 计算属性 ============
const filteredList = computed(() => {
  let list = [...tableData.value]

  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    list = list.filter(item =>
        item.name.toLowerCase().includes(keyword) ||
        item.topic.toLowerCase().includes(keyword)
    )
  }

  if (searchStatus.value !== null) {
    list = list.filter(item => item.status === searchStatus.value)
  }

  return list
})

// ============ 工具函数 ============
const getAppScopeText = (item: any) => {
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

const disabledDate = (time: Date) => {
  return time.getTime() > Date.now()
}

// ============ 列表操作方法 ============
const loadData = () => {
  loading.value = true
  setTimeout(() => {
    total.value = filteredList.value.length
    loading.value = false
  }, 300)
}

const handleSearch = () => {
  currentPage.value = 1
  loadData()
}

const handleReset = () => {
  searchKeyword.value = ''
  searchStatus.value = null
  currentPage.value = 1
  loadData()
}

const handleRefresh = () => {
  refreshing.value = true
  setTimeout(() => {
    loadData()
    refreshing.value = false
  }, 500)
}

const handleSizeChange = () => {
  loadData()
}

const handlePageChange = () => {
  loadData()
}

// ============ CRUD 操作 ============
const handleAdd = () => {
  formMode.value = 'add'
  currentFormData.value = null
  formDialogVisible.value = true
}

const handleView = (row: any) => {
  currentDetailData.value = row
  detailDialogVisible.value = true
}

const handleEdit = (row: any) => {
  formMode.value = 'edit'
  currentFormData.value = row
  formDialogVisible.value = true
}

const handleFormSubmit = (data: any) => {
  if (formMode.value === 'add') {
    tableData.value.unshift({
      ...data,
      id: Date.now(),
      lastRunTime: ''
    })
  } else if (formMode.value === 'edit') {
    const index = tableData.value.findIndex(item => item.id === data.id)
    if (index !== -1) {
      tableData.value[index] = { ...data, lastRunTime: tableData.value[index].lastRunTime }
    }
  }
  ElMessage.success(formMode.value === 'add' ? '新增成功' : '编辑成功')
}

const handleDuplicate = (row: any) => {
  ElMessageBox.confirm('确定要复制此解析策略吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'info'
  }).then(() => {
    const newItem = { ...row, id: Date.now(), name: row.name + ' (副本)', lastRunTime: '' }
    tableData.value.push(newItem)
    ElMessage.success('复制成功')
  }).catch(() => {})
}

const handleToggle = (row: any) => {
  const newStatus = row.status === 1 ? 0 : 1
  const action = newStatus === 1 ? '启用' : '停用'
  ElMessageBox.confirm(`确定要${action}此解析策略吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    row.status = newStatus
    ElMessage.success(`${action}成功`)
  }).catch(() => {})
}

const handleDelete = (row: any) => {
  ElMessageBox.confirm('确定要删除此解析策略吗？删除后不可恢复！', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    const index = tableData.value.findIndex(item => item.id === row.id)
    if (index !== -1) {
      tableData.value.splice(index, 1)
    }
    ElMessage.success('删除成功')
  }).catch(() => {})
}

const handleCardCommand = (cmd: string, row: any) => {
  switch (cmd) {
    case 'view':
      handleView(row)
      break
    case 'edit':
      handleEdit(row)
      break
    case 'log':
      handleLog(row)
      break
    case 'duplicate':
      handleDuplicate(row)
      break
    case 'toggle':
      handleToggle(row)
      break
    case 'delete':
      handleDelete(row)
      break
  }
}

// ============ 日志相关 ============
const handleLog = (row: any) => {
  currentLogStrategy.value = row
  logDialogVisible.value = true
}

const handleLogSearch = () => {
  ElMessage.info('查询日志')
}

const handleLogExport = () => {
  ElMessage.info('导出日志')
}

const handleLogClear = () => {
  ElMessageBox.confirm('确定要清空运行日志吗？', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    logList.value = []
    ElMessage.success('日志已清空')
  }).catch(() => {})
}

const showLogData = (row: any) => {
  ElMessageBox.alert(row.data || '-', '日志数据', {
    confirmButtonText: '关闭'
  })
}

const handleLogSizeChange = () => {}
const handleLogPageChange = () => {}

// ============ 测试相关 ============
const handleTest = (row?: any) => {
  if (row) {
    testData.value = JSON.stringify({
      topic: row.topic || '$dp',
      payload: {
        deviceId: 'test001',
        timestamp: Date.now(),
        data: {
          temperature: 25.5,
          humidity: 60
        }
      }
    }, null, 2)
  }
  testResult.value = ''
  testDialogVisible.value = true
}

const handleTestFromDetail = (data: any) => {
  handleTest(data)
}

const handleTestFromForm = (data: any) => {
  handleTest(data)
}

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
          deviceId: data.payload?.deviceId || 'unknown',
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

// ============ 生命周期 ============
onMounted(() => {
  loadData()
})
</script>

<style scoped>
.status-select {
  width: 150px;
}

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