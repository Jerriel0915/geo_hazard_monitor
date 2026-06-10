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
                    <el-icon><MoreFilled/></el-icon>
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

    <el-dialog
        v-model="dialogVisible"
        :title="dialogTitle"
        width="1000px"
        :close-on-click-modal="false"
        destroy-on-close
        class="parse-dialog"
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
                <el-radio label="global">全局</el-radio>
                <el-radio label="vendor">指定厂商</el-radio>
                <el-radio label="device">指定设备</el-radio>
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
            <div class="editor-tabs">
              <el-radio-group v-model="scriptMode" size="small" :disabled="isView">
                <el-radio-button label="visual">可视化编程</el-radio-button>
                <el-radio-button label="code">代码编辑</el-radio-button>
              </el-radio-group>
            </div>
            
            <div v-if="scriptMode === 'visual'" class="blockly-container">
              <div id="blocklyDiv" class="blockly-workspace"></div>
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, MoreFilled } from '@element-plus/icons-vue'

const loading = ref(false)
const refreshing = ref(false)
const submitLoading = ref(false)
const testRunning = ref(false)

const searchKeyword = ref('')
const searchStatus = ref<number | null>(null)
const currentPage = ref(1)
const pageSize = ref(12)
const total = ref(0)

const dialogVisible = ref(false)
const dialogTitle = ref('新增解析策略')
const isEdit = ref(false)
const isView = ref(false)
const activeTab = ref('basic')
const scriptMode = ref('visual')

const formRef = ref()
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

const formRules = {
  name: [{ required: true, message: '请输入策略名称', trigger: 'blur' }],
  serverUrl: [{ required: true, message: '请输入服务地址', trigger: 'blur' }],
  topic: [{ required: true, message: '请输入主题', trigger: 'blur' }],
  status: [{ required: true, message: '请选择启用状态', trigger: 'change' }]
}

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
    appScope: 'global' as string,
    vendorIds: [] as number[],
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
    appScope: 'vendor' as string,
    vendorIds: [2],
    deviceIds: [] as number[],
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
    appScope: 'device' as string,
    vendorIds: [] as number[],
    deviceIds: [2],
    lastRunTime: '2026-06-08 12:15:30',
    scriptCode: '// 雨量计解析\nfunction parse(message) {\n  return { deviceId: message.deviceId, rainfall: message.value };\n}'
  }
])

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

const logDialogVisible = ref(false)
const logDateRange = ref([] as string[])
const logLevel = ref('')
const logCurrentPage = ref(1)
const logPageSize = ref(20)
const logTotal = ref(0)

const logList = ref([
  { timestamp: '2026-06-08 14:30:25', level: 'INFO', message: '接收到MQTT消息，主题: $dp', data: '{"topic":"$dp","payload":"..."}' },
  { timestamp: '2026-06-08 14:30:26', level: 'INFO', message: '解析成功，设备ID: dev001', data: '{"deviceId":"dev001","data":"..."}' },
  { timestamp: '2026-06-08 14:30:27', level: 'INFO', message: '数据已存储', data: '{}' },
  { timestamp: '2026-06-08 14:25:10', level: 'WARN', message: '数据格式异常，使用默认值', data: '{"error":"format error"}' },
  { timestamp: '2026-06-08 12:15:30', level: 'ERROR', message: '解析脚本执行失败', data: '{"error":"script error"}' }
])

const testDialogVisible = ref(false)
const testData = ref('')
const testResult = ref('')

const currentLogStrategy = ref<any>(null)

const getAppScopeText = (item: any) => {
  const scopeMap = {
    global: '全局',
    vendor: `指定厂商 (${item.vendorIds?.length || 0}个)`,
    device: `指定设备 (${item.deviceIds?.length || 0}个)`
  }
  return scopeMap[item.appScope as keyof typeof scopeMap] || '全局'
}

const getLogLevelType = (level: string) => {
  const typeMap = {
    INFO: 'info',
    WARN: 'warning',
    ERROR: 'danger'
  }
  return typeMap[level as keyof typeof typeMap] || 'info'
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

const loadData = () => {
  loading.value = true
  setTimeout(() => {
    total.value = filteredList.value.length
    loading.value = false
  }, 300)
}

const handleAdd = () => {
  resetForm()
  isEdit.value = false
  isView.value = false
  dialogTitle.value = '新增解析策略'
  activeTab.value = 'basic'
  dialogVisible.value = true
}

const handleView = (row: any) => {
  fillForm(row)
  isEdit.value = false
  isView.value = true
  dialogTitle.value = '查看解析策略'
  activeTab.value = 'basic'
  dialogVisible.value = true
}

const handleEdit = (row: any) => {
  fillForm(row)
  isEdit.value = true
  isView.value = false
  dialogTitle.value = '编辑解析策略'
  activeTab.value = 'basic'
  dialogVisible.value = true
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

const handleLog = (row: any) => {
  currentLogStrategy.value = row
  logDialogVisible.value = true
}

const handleTest = (row?: any) => {
  if (row) {
    formData.scriptCode = row.scriptCode
  }
  testData.value = JSON.stringify({
    topic: '$dp',
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

const handleAppScopeChange = () => {
  formData.vendorIds = []
  formData.deviceIds = []
}

const handleSubmit = () => {
  formRef.value.validate((valid: boolean) => {
    if (!valid) return
    
    submitLoading.value = true
    setTimeout(() => {
      if (isEdit.value && formData.id) {
        const index = tableData.value.findIndex(item => item.id === formData.id)
        if (index !== -1) {
          tableData.value[index] = { ...formData, id: formData.id!, lastRunTime: tableData.value[index].lastRunTime }
        }
      } else {
        tableData.value.push({
          ...formData,
          id: Date.now(),
          lastRunTime: ''
        })
      }
      dialogVisible.value = false
      submitLoading.value = false
      ElMessage.success('保存成功')
    }, 500)
  })
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

const handleSizeChange = () => {}
const handlePageChange = () => {}

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
}

const fillForm = (row: any) => {
  formData.id = row.id
  formData.name = row.name
  formData.serverUrl = row.serverUrl
  formData.topic = row.topic
  formData.description = row.description
  formData.status = row.status
  formData.appScope = row.appScope
  formData.vendorIds = [...(row.vendorIds || [])]
  formData.deviceIds = [...(row.deviceIds || [])]
  formData.scriptCode = row.scriptCode || ''
}

const disabledDate = (time: Date) => {
  return time.getTime() > Date.now()
}

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


.parse-dialog .el-tabs {
  min-height: 500px;
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
  color: #fff;
  font-size: 16px;
  position: relative;
}

.blockly-workspace::before {
  content: 'Blockly 可视化编程区域';
  position: absolute;
  text-align: center;
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

.log-dialog {
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
