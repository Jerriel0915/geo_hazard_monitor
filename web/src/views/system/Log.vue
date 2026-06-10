<template>
  <div class="page-content">
    <div class="dashboard-grid">
      <section class="main-panel">
        <el-tabs v-model="activeTab" type="border-card" @tab-change="handleTabChange">
          <el-tab-pane label="操作日志" name="operation">
            <div class="tab-content">
              <div class="toolbar">
                <el-form :model="opSearchForm" inline class="search-form">
                  <el-form-item label="操作用户">
                    <el-input v-model="opSearchForm.username" placeholder="请输入用户名" clearable/>
                  </el-form-item>
                  <el-form-item label="业务标题">
                    <el-input v-model="opSearchForm.title" placeholder="请输入业务标题" clearable/>
                  </el-form-item>
                  <el-form-item label="接口路径">
                    <el-input v-model="opSearchForm.apiPath" placeholder="请输入接口路径" clearable/>
                  </el-form-item>
                  <el-form-item label="执行状态">
                    <el-select v-model="opSearchForm.execStatus" placeholder="全部状态" clearable style="width: 140px">
                      <el-option label="成功" value="SUCCESS"/>
                      <el-option label="失败" value="FAIL"/>
                    </el-select>
                  </el-form-item>
                  <el-form-item label="操作时间">
                    <el-date-picker
                        v-model="opSearchForm.timeRange"
                        type="datetimerange"
                        range-separator="至"
                        start-placeholder="开始时间"
                        end-placeholder="结束时间"
                        value-format="YYYY-MM-DD HH:mm:ss"
                    />
                  </el-form-item>
                </el-form>
                <div class="toolbar-actions">
                  <el-button type="primary" @click="handleOperationSearch">查询</el-button>
                  <el-button @click="handleOperationReset">重置</el-button>
                  <el-button @click="refreshActiveTab">刷新</el-button>
                </div>
              </div>

              <el-table :data="operationLogs" border stripe v-loading="operationLoading">
                <el-table-column type="index" label="序号" width="60" align="center"/>
                <el-table-column prop="title" label="业务标题" min-width="140" show-overflow-tooltip/>
                <el-table-column prop="businessType" label="业务类型" width="120" align="center">
                  <template #default="{ row }">
                    <el-tag :type="getBusinessTypeTag(row.businessType)" size="small">
                      {{ row.businessType || 'UNKNOWN' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="username" label="操作用户" width="120"/>
                <el-table-column prop="apiPath" label="接口路径" min-width="220" show-overflow-tooltip/>
                <el-table-column prop="requestMethod" label="请求方式" width="100" align="center">
                  <template #default="{ row }">
                    <el-tag :type="getRequestMethodTag(row.requestMethod)" size="small">
                      {{ row.requestMethod || '--' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="clientIp" label="IP地址" width="130"/>
                <el-table-column prop="execStatus" label="状态" width="90" align="center">
                  <template #default="{ row }">
                    <el-tag :type="row.execStatus === 'SUCCESS' ? 'success' : 'danger'" size="small">
                      {{ row.execStatus === 'SUCCESS' ? '成功' : '失败' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="costTimeMs" label="耗时(ms)" width="100" align="center"/>
                <el-table-column prop="occurredAt" label="操作时间" width="180"/>
              </el-table>

              <div class="pagination">
                <el-pagination
                    v-model:current-page="operationPagination.page"
                    v-model:page-size="operationPagination.size"
                    :page-sizes="[10, 20, 50, 100]"
                    :total="operationPagination.total"
                    layout="total, sizes, prev, pager, next, jumper"
                    @size-change="handleOperationSizeChange"
                    @current-change="handleOperationPageChange"
                />
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane label="认证日志" name="auth">
            <div class="tab-content">
              <div class="toolbar">
                <el-form :model="authSearchForm" inline class="search-form">
                  <el-form-item label="用户名">
                    <el-input v-model="authSearchForm.username" placeholder="请输入用户名" clearable/>
                  </el-form-item>
                  <el-form-item label="事件类型">
                    <el-select v-model="authSearchForm.authEventType" placeholder="全部类型" clearable
                               style="width: 160px">
                      <el-option v-for="item in authEventOptions" :key="item" :label="item" :value="item"/>
                    </el-select>
                  </el-form-item>
                  <el-form-item label="结果状态">
                    <el-select v-model="authSearchForm.resultStatus" placeholder="全部状态" clearable
                               style="width: 140px">
                      <el-option label="成功" value="SUCCESS"/>
                      <el-option label="失败" value="FAIL"/>
                    </el-select>
                  </el-form-item>
                  <el-form-item label="认证时间">
                    <el-date-picker
                        v-model="authSearchForm.timeRange"
                        type="datetimerange"
                        range-separator="至"
                        start-placeholder="开始时间"
                        end-placeholder="结束时间"
                        value-format="YYYY-MM-DD HH:mm:ss"
                    />
                  </el-form-item>
                </el-form>
                <div class="toolbar-actions">
                  <el-button type="primary" @click="handleAuthSearch">查询</el-button>
                  <el-button @click="handleAuthReset">重置</el-button>
                  <el-button @click="refreshActiveTab">刷新</el-button>
                </div>
              </div>

              <el-table :data="authLogs" border stripe v-loading="authLoading">
                <el-table-column type="index" label="序号" width="60" align="center"/>
                <el-table-column prop="username" label="用户名" width="120"/>
                <el-table-column prop="authEventType" label="认证事件" width="160" align="center">
                  <template #default="{ row }">
                    <el-tag :type="getAuthEventTag(row.authEventType)" size="small">
                      {{ row.authEventType || '--' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="authChannel" label="认证通道" width="120" align="center"/>
                <el-table-column prop="requestUri" label="请求URI" min-width="220" show-overflow-tooltip/>
                <el-table-column prop="clientIp" label="IP地址" width="130"/>
                <el-table-column prop="resultStatus" label="结果" width="90" align="center">
                  <template #default="{ row }">
                    <el-tag :type="row.resultStatus === 'SUCCESS' ? 'success' : 'danger'" size="small">
                      {{ row.resultStatus === 'SUCCESS' ? '成功' : '失败' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="failureMessage" label="消息" min-width="200" show-overflow-tooltip/>
                <el-table-column prop="occurredAt" label="认证时间" width="180"/>
              </el-table>

              <div class="pagination">
                <el-pagination
                    v-model:current-page="authPagination.page"
                    v-model:page-size="authPagination.size"
                    :page-sizes="[10, 20, 50, 100]"
                    :total="authPagination.total"
                    layout="total, sizes, prev, pager, next, jumper"
                    @size-change="handleAuthSizeChange"
                    @current-change="handleAuthPageChange"
                />
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane label="运行日志" name="runtime">
            <div class="tab-content">
              <div class="toolbar">
                <el-form :model="runtimeSearchForm" inline class="search-form">
                  <el-form-item label="日志级别">
                    <el-select v-model="runtimeSearchForm.level" placeholder="全部级别" clearable style="width: 140px">
                      <el-option label="INFO" value="INFO"/>
                      <el-option label="WARN" value="WARN"/>
                      <el-option label="ERROR" value="ERROR"/>
                    </el-select>
                  </el-form-item>
                  <el-form-item label="Logger">
                    <el-input v-model="runtimeSearchForm.loggerName" placeholder="请输入 Logger 名称" clearable/>
                  </el-form-item>
                  <el-form-item label="关键词">
                    <el-input v-model="runtimeSearchForm.keyword" placeholder="请输入日志内容关键词" clearable/>
                  </el-form-item>
                  <el-form-item label="发生时间">
                    <el-date-picker
                        v-model="runtimeSearchForm.timeRange"
                        type="datetimerange"
                        range-separator="至"
                        start-placeholder="开始时间"
                        end-placeholder="结束时间"
                        value-format="YYYY-MM-DD HH:mm:ss"
                    />
                  </el-form-item>
                </el-form>
                <div class="toolbar-actions">
                  <el-button type="primary" @click="handleRuntimeSearch">查询</el-button>
                  <el-button @click="handleRuntimeReset">重置</el-button>
                  <el-button @click="refreshActiveTab">刷新</el-button>
                </div>
              </div>

              <el-table :data="runtimeLogs" border stripe v-loading="runtimeLoading">
                <el-table-column type="index" label="序号" width="60" align="center"/>
                <el-table-column prop="level" label="级别" width="100" align="center">
                  <template #default="{ row }">
                    <el-tag :type="getLevelTag(row.level)" effect="dark" size="small">{{ row.level }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="loggerName" label="Logger" min-width="250" show-overflow-tooltip/>
                <el-table-column prop="threadName" label="线程" width="160" show-overflow-tooltip/>
                <el-table-column prop="sourceApp" label="来源应用" width="120"/>
                <el-table-column prop="messageDigest" label="日志摘要" min-width="280" show-overflow-tooltip/>
                <el-table-column prop="occurredAt" label="发生时间" width="180"/>
              </el-table>

              <div class="pagination">
                <el-pagination
                    v-model:current-page="runtimePagination.page"
                    v-model:page-size="runtimePagination.size"
                    :page-sizes="[10, 20, 50, 100]"
                    :total="runtimePagination.total"
                    layout="total, sizes, prev, pager, next, jumper"
                    @size-change="handleRuntimeSizeChange"
                    @current-change="handleRuntimePageChange"
                />
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </section>

      <aside class="stream-panel">
        <div class="stream-card">
          <div class="panel-head">
            <div>
              <div class="panel-title">实时日志流</div>
              <div class="panel-desc">基于 SSE 订阅 `auth / runtime / operation` 三类实时日志。</div>
            </div>
            <el-tag :type="sseStatusTagType" effect="dark">{{ sseStatusText }}</el-tag>
          </div>

          <el-form label-position="top" class="stream-form">
            <el-form-item label="订阅类型">
              <el-checkbox-group v-model="sseTypes">
                <el-checkbox label="operation">操作</el-checkbox>
                <el-checkbox label="auth">认证</el-checkbox>
                <el-checkbox label="runtime">运行</el-checkbox>
              </el-checkbox-group>
            </el-form-item>
            <el-form-item label="订阅者标识">
              <el-input v-model="subscriberKey" placeholder="例如 web-log-console" clearable/>
            </el-form-item>
          </el-form>

          <div class="stream-meta">
            <div class="meta-item">
              <span>恢复起点</span>
              <strong>{{ resumeEventId || '--' }}</strong>
            </div>
            <div class="meta-item">
              <span>最后事件</span>
              <strong>{{ lastEventId || '--' }}</strong>
            </div>
            <div class="meta-item">
              <span>事件缓存</span>
              <strong>{{ liveEvents.length }}</strong>
            </div>
          </div>

          <div class="stream-actions">
            <el-button type="primary" :loading="sseStatus === 'connecting' || sseStatus === 'reconnecting'"
                       :disabled="isStreamAlive"
                       @click="startStream">
              开始订阅
            </el-button>
            <el-button :disabled="!isStreamAlive" @click="stopStream">停止订阅</el-button>
            <el-button @click="clearLiveEvents">清空缓存</el-button>
          </div>

          <div class="stream-list">
            <div v-if="!liveEvents.length" class="stream-empty">
              暂无实时事件，建立连接后将展示实时日志与断线补发记录。
            </div>
            <div v-for="item in liveEvents" :key="`${item.eventId}-${item.event}-${item.timestamp}`"
                 class="stream-item">
              <div class="stream-item-head">
                <div class="stream-item-tags">
                  <el-tag size="small" :type="getStreamEventTag(item.event)">{{ item.event.toUpperCase() }}</el-tag>
                  <el-tag v-if="item.subType" size="small" effect="plain" :type="getLiveSubtypeTag(item.logType)">
                    {{ item.subType }}
                  </el-tag>
                  <el-tag v-else size="small" effect="plain">{{ item.logType }}</el-tag>
                </div>
                <span class="stream-time">{{ item.timestamp }}</span>
              </div>
              <div class="stream-title">{{ item.title }}</div>
              <div class="stream-detail">{{ item.detail }}</div>
              <div class="stream-id">eventId: {{ item.eventId }}</div>
            </div>
          </div>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import axios from 'axios'
import {computed, onBeforeUnmount, onMounted, reactive, ref} from 'vue'
import {ElMessage} from 'element-plus'
import {handleAuthFailure} from '@/utils/auth'
import {getRequestErrorMessage} from '@/utils/errorHandler'

type TabKey = 'operation' | 'auth' | 'runtime'
type StreamStatus = 'disconnected' | 'connecting' | 'connected' | 'reconnecting' | 'error'
type SseType = 'operation' | 'auth' | 'runtime'

interface PageResult<T> {
  pageNum: number
  pageSize: number
  total: number
  rows: T[]
}

interface ApiResponse<T> {
  code: number
  msg: string
  data: T
  timestamp: number
}

interface OperationLogRecord {
  id: number
  eventId: number
  logType: string
  title: string
  businessType: string
  apiPath: string
  requestMethod: string
  controllerMethod: string
  username: string
  userId: number | null
  clientIp: string
  httpStatus: number | null
  execStatus: string
  costTimeMs: number | null
  occurredAt: string
}

interface AuthLogRecord {
  id: number
  eventId: number
  logType: string
  username: string
  authEventType: string
  authChannel: string
  requestUri: string
  requestMethod: string
  clientIp: string
  resultStatus: string
  failureMessage: string
  occurredAt: string
}

interface RuntimeLogRecord {
  id: number
  eventId: number
  logType: string
  level: string
  loggerName: string
  threadName: string
  sourceApp: string
  message: string
  messageDigest: string
  occurredAt: string
}

interface LiveEventItem {
  event: string
  logType: string
  subType: string
  eventId: string
  title: string
  detail: string
  timestamp: string
}

const tabLabelMap: Record<TabKey, string> = {
  operation: '操作日志',
  auth: '认证日志',
  runtime: '运行日志'
}

const authEventOptions = ['LOGIN_SUCCESS', 'LOGIN_FAIL', 'LOGOUT', 'UNAUTHORIZED', 'TOKEN_INVALID']

const activeTab = ref<TabKey>('operation')

const operationLoading = ref(false)
const authLoading = ref(false)
const runtimeLoading = ref(false)

const operationLogs = ref<OperationLogRecord[]>([])
const authLogs = ref<AuthLogRecord[]>([])
const runtimeLogs = ref<RuntimeLogRecord[]>([])

const operationPagination = reactive({page: 1, size: 10, total: 0})
const authPagination = reactive({page: 1, size: 10, total: 0})
const runtimePagination = reactive({page: 1, size: 10, total: 0})

const opSearchForm = reactive({
  username: '',
  title: '',
  apiPath: '',
  execStatus: '',
  timeRange: [] as string[]
})

const authSearchForm = reactive({
  username: '',
  authEventType: '',
  resultStatus: '',
  timeRange: [] as string[]
})

const runtimeSearchForm = reactive({
  level: '',
  loggerName: '',
  keyword: '',
  timeRange: [] as string[]
})

const sseTypes = ref<SseType[]>(['auth', 'runtime'])
const subscriberKey = ref('web-log-console')
const sseStatus = ref<StreamStatus>('disconnected')
const resumeEventId = ref('')
const lastEventId = ref('')
const liveEvents = ref<LiveEventItem[]>([])

let streamAbortController: AbortController | null = null
let reconnectTimer: number | null = null
let keepStreamAlive = false
let refreshTimer: number | null = null
let streamSessionId = 0

const sseStatusText = computed(() => {
  const map: Record<StreamStatus, string> = {
    disconnected: '未连接',
    connecting: '连接中',
    connected: '已连接',
    reconnecting: '重连中',
    error: '异常'
  }
  return map[sseStatus.value]
})

const sseStatusTagType = computed(() => {
  const map: Record<StreamStatus, '' | 'success' | 'warning' | 'danger' | 'info'> = {
    disconnected: 'info',
    connecting: 'warning',
    connected: 'success',
    reconnecting: 'warning',
    error: 'danger'
  }
  return map[sseStatus.value]
})

const isStreamAlive = computed(() => ['connecting', 'connected', 'reconnecting'].includes(sseStatus.value))

const requestHeaders = () => {
  const token = localStorage.getItem('token')
  return {
    Authorization: token ? `Bearer ${token}` : ''
  }
}

const buildTimeParams = (range: string[]) => ({
  startTime: range?.length === 2 ? range[0] : undefined,
  endTime: range?.length === 2 ? range[1] : undefined
})

const fetchOperationLogs = async () => {
  operationLoading.value = true
  try {
    const response = await axios.get<ApiResponse<PageResult<OperationLogRecord>>>('/api/v1/logs/operations/page', {
      params: {
        pageNum: operationPagination.page,
        pageSize: operationPagination.size,
        username: opSearchForm.username || undefined,
        title: opSearchForm.title || undefined,
        apiPath: opSearchForm.apiPath || undefined,
        execStatus: opSearchForm.execStatus || undefined,
        ...buildTimeParams(opSearchForm.timeRange)
      },
      headers: requestHeaders()
    })
    if (response.data.code === 200) {
      operationLogs.value = response.data.data.rows || []
      operationPagination.total = response.data.data.total || 0
    } else {
      ElMessage.error(response.data.msg || '获取操作日志失败')
    }
  } catch (error) {
    console.error('获取操作日志失败:', error)
    ElMessage.error(getRequestErrorMessage(error, '获取操作日志失败'))
  } finally {
    operationLoading.value = false
  }
}

const fetchAuthLogs = async () => {
  authLoading.value = true
  try {
    const response = await axios.get<ApiResponse<PageResult<AuthLogRecord>>>('/api/v1/logs/auth/page', {
      params: {
        pageNum: authPagination.page,
        pageSize: authPagination.size,
        username: authSearchForm.username || undefined,
        authEventType: authSearchForm.authEventType || undefined,
        resultStatus: authSearchForm.resultStatus || undefined,
        ...buildTimeParams(authSearchForm.timeRange)
      },
      headers: requestHeaders()
    })
    if (response.data.code === 200) {
      authLogs.value = response.data.data.rows || []
      authPagination.total = response.data.data.total || 0
    } else {
      ElMessage.error(response.data.msg || '获取认证日志失败')
    }
  } catch (error) {
    console.error('获取认证日志失败:', error)
    ElMessage.error(getRequestErrorMessage(error, '获取认证日志失败'))
  } finally {
    authLoading.value = false
  }
}

const fetchRuntimeLogs = async () => {
  runtimeLoading.value = true
  try {
    const response = await axios.get<ApiResponse<PageResult<RuntimeLogRecord>>>('/api/v1/logs/runtime/page', {
      params: {
        pageNum: runtimePagination.page,
        pageSize: runtimePagination.size,
        level: runtimeSearchForm.level || undefined,
        loggerName: runtimeSearchForm.loggerName || undefined,
        keyword: runtimeSearchForm.keyword || undefined,
        ...buildTimeParams(runtimeSearchForm.timeRange)
      },
      headers: requestHeaders()
    })
    if (response.data.code === 200) {
      runtimeLogs.value = response.data.data.rows || []
      runtimePagination.total = response.data.data.total || 0
    } else {
      ElMessage.error(response.data.msg || '获取运行日志失败')
    }
  } catch (error) {
    console.error('获取运行日志失败:', error)
    ElMessage.error(getRequestErrorMessage(error, '获取运行日志失败'))
  } finally {
    runtimeLoading.value = false
  }
}

const refreshActiveTab = () => {
  if (activeTab.value === 'operation') {
    fetchOperationLogs()
  } else if (activeTab.value === 'auth') {
    fetchAuthLogs()
  } else {
    fetchRuntimeLogs()
  }
}

const handleTabChange = (tabName: string | number) => {
  activeTab.value = tabName as TabKey
  refreshActiveTab()
}

const handleOperationSearch = () => {
  operationPagination.page = 1
  fetchOperationLogs()
}

const handleOperationReset = () => {
  opSearchForm.username = ''
  opSearchForm.title = ''
  opSearchForm.apiPath = ''
  opSearchForm.execStatus = ''
  opSearchForm.timeRange = []
  operationPagination.page = 1
  fetchOperationLogs()
}

const handleOperationSizeChange = (value: number) => {
  operationPagination.size = value
  operationPagination.page = 1
  fetchOperationLogs()
}

const handleOperationPageChange = (value: number) => {
  operationPagination.page = value
  fetchOperationLogs()
}

const handleAuthSearch = () => {
  authPagination.page = 1
  fetchAuthLogs()
}

const handleAuthReset = () => {
  authSearchForm.username = ''
  authSearchForm.authEventType = ''
  authSearchForm.resultStatus = ''
  authSearchForm.timeRange = []
  authPagination.page = 1
  fetchAuthLogs()
}

const handleAuthSizeChange = (value: number) => {
  authPagination.size = value
  authPagination.page = 1
  fetchAuthLogs()
}

const handleAuthPageChange = (value: number) => {
  authPagination.page = value
  fetchAuthLogs()
}

const handleRuntimeSearch = () => {
  runtimePagination.page = 1
  fetchRuntimeLogs()
}

const handleRuntimeReset = () => {
  runtimeSearchForm.level = ''
  runtimeSearchForm.loggerName = ''
  runtimeSearchForm.keyword = ''
  runtimeSearchForm.timeRange = []
  runtimePagination.page = 1
  fetchRuntimeLogs()
}

const handleRuntimeSizeChange = (value: number) => {
  runtimePagination.size = value
  runtimePagination.page = 1
  fetchRuntimeLogs()
}

const handleRuntimePageChange = (value: number) => {
  runtimePagination.page = value
  fetchRuntimeLogs()
}

const clearReconnectTimer = () => {
  if (reconnectTimer !== null) {
    window.clearTimeout(reconnectTimer)
    reconnectTimer = null
  }
}

const clearRefreshTimer = () => {
  if (refreshTimer !== null) {
    window.clearTimeout(refreshTimer)
    refreshTimer = null
  }
}

const stopStream = (manual = true) => {
  if (manual) {
    keepStreamAlive = false
  }
  clearReconnectTimer()
  if (streamAbortController) {
    streamAbortController.abort()
    streamAbortController = null
  }
  if (manual) {
    sseStatus.value = 'disconnected'
  }
}

const scheduleReconnect = () => {
  if (!keepStreamAlive) {
    return
  }
  clearReconnectTimer()
  sseStatus.value = 'reconnecting'
  reconnectTimer = window.setTimeout(() => {
    startStream(true)
  }, 3000)
}

const createStreamUrl = () => {
  const params = new URLSearchParams()
  const types = sseTypes.value.length ? sseTypes.value.join(',') : 'auth,runtime'
  params.set('types', types)
  if (subscriberKey.value.trim()) {
    params.set('subscriberKey', subscriberKey.value.trim())
  }
  return `/api/v1/logs/stream?${params.toString()}`
}

const pushLiveEvent = (event: string, eventId: string, payload: Record<string, any>) => {
  const logType = String(payload.logType || event).toUpperCase()
  const subType = resolveLiveSubtype(logType, payload)
  const title = buildLiveTitle(event, payload)
  const detail = buildLiveDetail(payload)
  liveEvents.value = [
    {
      event,
      logType,
      subType,
      eventId,
      title,
      detail,
      timestamp: payload.occurredAt || new Date().toLocaleString()
    },
    ...liveEvents.value
  ].slice(0, 16)
}

const buildLiveTitle = (event: string, payload: Record<string, any>) => {
  if (event === 'ready') {
    return 'SSE 连接建立成功'
  }
  if (payload.logType === 'AUTH') {
    return `${payload.authEventType || 'AUTH'} · ${payload.username || '匿名用户'}`
  }
  if (payload.logType === 'OPERATION') {
    return `${payload.title || '操作日志'} · ${payload.username || '未知用户'}`
  }
  if (payload.logType === 'RUNTIME') {
    return `${payload.level || 'LOG'} · ${payload.loggerName || 'runtime'}`
  }
  return event
}

const buildLiveDetail = (payload: Record<string, any>) => {
  if (payload.logType === 'AUTH') {
    return payload.failureMessage || payload.requestUri || '--'
  }
  if (payload.logType === 'OPERATION') {
    return `${payload.apiPath || '--'} / ${payload.requestMethod || '--'} / ${payload.execStatus || '--'}`
  }
  if (payload.logType === 'RUNTIME') {
    return payload.messageDigest || payload.message || '--'
  }
  return JSON.stringify(payload)
}

const resolveLiveSubtype = (logType: string, payload: Record<string, any>) => {
  if (logType === 'OPERATION') {
    return String(payload.businessType || '')
  }
  if (logType === 'AUTH') {
    return String(payload.authEventType || '')
  }
  if (logType === 'RUNTIME') {
    return String(payload.level || '')
  }
  return ''
}

const scheduleTabRefresh = (payload: Record<string, any>) => {
  const tabMap: Record<string, TabKey> = {
    OPERATION: 'operation',
    AUTH: 'auth',
    RUNTIME: 'runtime'
  }
  const targetTab = tabMap[String(payload.logType || '').toUpperCase()]
  if (!targetTab || targetTab !== activeTab.value) {
    return
  }
  clearRefreshTimer()
  refreshTimer = window.setTimeout(() => {
    refreshActiveTab()
  }, 800)
}

const parseEventBlock = (block: string) => {
  const lines = block.split(/\r?\n/)
  let event = 'message'
  let id = ''
  const dataLines: string[] = []
  for (const rawLine of lines) {
    const line = rawLine.trimEnd()
    if (!line || line.startsWith(':')) {
      continue
    }
    const index = line.indexOf(':')
    const field = index >= 0 ? line.slice(0, index) : line
    const value = index >= 0 ? line.slice(index + 1).trimStart() : ''
    if (field === 'event') {
      event = value
    } else if (field === 'id') {
      id = value
    } else if (field === 'data') {
      dataLines.push(value)
    }
  }
  return {event, id, data: dataLines.join('\n')}
}

const processSseEvent = (parsedEvent: { event: string; id: string; data: string }) => {
  if (!parsedEvent.data) {
    return
  }
  if (parsedEvent.id) {
    lastEventId.value = parsedEvent.id
  }
  let payload: Record<string, any> = {}
  try {
    payload = JSON.parse(parsedEvent.data)
  } catch (error) {
    return
  }
  if (parsedEvent.event === 'ready') {
    resumeEventId.value = payload.resumeEventId ? String(payload.resumeEventId) : ''
    sseStatus.value = 'connected'
    return
  }
  pushLiveEvent(parsedEvent.event, parsedEvent.id || String(payload.eventId || ''), payload)
  scheduleTabRefresh(payload)
}

const startStream = async (isReconnect = false) => {
  if (!sseTypes.value.length) {
    ElMessage.warning('请至少选择一种订阅类型')
    return
  }
  if (!isReconnect && isStreamAlive.value) {
    return
  }
  const token = localStorage.getItem('token')
  if (!token) {
    ElMessage.error('登录状态已失效，请重新登录')
    return
  }

  const sessionId = ++streamSessionId
  keepStreamAlive = false
  clearReconnectTimer()
  stopStream(false)
  keepStreamAlive = true
  streamAbortController = new AbortController()
  sseStatus.value = isReconnect ? 'reconnecting' : 'connecting'

  try {
    const response = await fetch(createStreamUrl(), {
      method: 'GET',
      headers: {
        Accept: 'text/event-stream',
        Authorization: `Bearer ${token}`,
        ...(lastEventId.value ? {'Last-Event-ID': lastEventId.value} : {})
      },
      signal: streamAbortController.signal
    })

    if (handleAuthFailure(undefined, response.status)) {
      return
    }

    if (!response.ok || !response.body) {
      throw new Error(`SSE连接失败: ${response.status}`)
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buffer = ''

    while (keepStreamAlive) {
      const {value, done} = await reader.read()
      if (done) {
        break
      }
      buffer += decoder.decode(value, {stream: true})
      const segments = buffer.split(/\r?\n\r?\n/)
      buffer = segments.pop() || ''
      for (const segment of segments) {
        if (segment.trim()) {
          processSseEvent(parseEventBlock(segment))
        }
      }
    }

    if (keepStreamAlive && sessionId === streamSessionId) {
      scheduleReconnect()
    }
  } catch (error) {
    if (!keepStreamAlive || sessionId !== streamSessionId) {
      return
    }
    sseStatus.value = 'error'
    ElMessage.warning('实时日志流连接中断，正在尝试重连')
    scheduleReconnect()
  }
}

const clearLiveEvents = () => {
  liveEvents.value = []
}

const getRequestMethodTag = (method?: string) => {
  const map: Record<string, '' | 'success' | 'warning' | 'danger' | 'info'> = {
    GET: 'success',
    POST: 'warning',
    PUT: 'info',
    DELETE: 'danger'
  }
  return map[String(method || '').toUpperCase()] || 'info'
}

const getBusinessTypeTag = (type?: string) => {
  const map: Record<string, '' | 'success' | 'warning' | 'danger' | 'info'> = {
    INSERT: 'success',
    UPDATE: 'warning',
    DELETE: 'danger',
    EXPORT: 'info',
    OTHER: 'info'
  }
  return map[String(type || '').toUpperCase()] || 'info'
}

const getAuthEventTag = (type?: string) => {
  const key = String(type || '').toUpperCase()
  if (key.includes('SUCCESS') || key === 'LOGOUT') {
    return 'success'
  }
  if (key.includes('UNAUTHORIZED') || key.includes('INVALID')) {
    return 'warning'
  }
  return 'danger'
}

const getLevelTag = (level?: string) => {
  const map: Record<string, '' | 'success' | 'warning' | 'danger' | 'info'> = {
    INFO: 'info',
    WARN: 'warning',
    ERROR: 'danger'
  }
  return map[String(level || '').toUpperCase()] || 'info'
}

const getStreamEventTag = (event: string) => {
  const map: Record<string, '' | 'success' | 'warning' | 'danger' | 'info'> = {
    ready: 'success',
    replay: 'warning',
    operation: 'info',
    auth: 'danger',
    runtime: 'warning'
  }
  return map[event] || 'info'
}

const getLiveSubtypeTag = (logType: string) => {
  if (logType === 'OPERATION') {
    return 'warning'
  }
  if (logType === 'AUTH') {
    return 'danger'
  }
  if (logType === 'RUNTIME') {
    return 'info'
  }
  return ''
}

onMounted(() => {
  refreshActiveTab()
})

onBeforeUnmount(() => {
  stopStream()
  clearRefreshTimer()
})
</script>

<style scoped>
.page-content {
  min-height: calc(100% - 32px);
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 20px;
}

.main-panel,
.stream-card {
  background: #ffffff;
  border-radius: 22px;
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.08);
  overflow: hidden;
}

.tab-content {
  padding: 10px 0 0;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 18px;
  flex-wrap: wrap;
}

.search-form {
  flex: 1;
}

.toolbar-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.pagination {
  margin-top: 18px;
  display: flex;
  justify-content: flex-end;
}

.stream-card {
  padding: 22px 20px;
  position: sticky;
  top: 20px;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.panel-title {
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
}

.panel-desc {
  margin-top: 6px;
  font-size: 13px;
  line-height: 1.6;
  color: #64748b;
}

.stream-form {
  margin-top: 18px;
}

.stream-meta {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-top: 6px;
}

.meta-item {
  padding: 12px;
  border-radius: 16px;
  background: linear-gradient(180deg, #f8fafc 0%, #f1f5f9 100%);
}

.meta-item span {
  display: block;
  font-size: 12px;
  color: #64748b;
}

.meta-item strong {
  display: block;
  margin-top: 8px;
  font-size: 13px;
  color: #0f172a;
  word-break: break-all;
}

.stream-actions {
  display: flex;
  gap: 10px;
  margin-top: 16px;
  flex-wrap: wrap;
}

.stream-list {
  margin-top: 18px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 780px;
  overflow: auto;
  padding-right: 4px;
}

.stream-empty {
  padding: 22px 16px;
  border: 1px dashed #cbd5e1;
  border-radius: 18px;
  text-align: center;
  color: #94a3b8;
  line-height: 1.7;
}

.stream-item {
  padding: 14px;
  border-radius: 18px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
  border: 1px solid #e2e8f0;
}

.stream-item-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.stream-item-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.stream-time {
  font-size: 12px;
  color: #94a3b8;
}

.stream-title {
  margin-top: 10px;
  font-weight: 600;
  color: #0f172a;
}

.stream-detail {
  margin-top: 8px;
  font-size: 13px;
  line-height: 1.6;
  color: #475569;
  word-break: break-word;
}

.stream-id {
  margin-top: 10px;
  font-size: 12px;
  color: #94a3b8;
  word-break: break-all;
}

:deep(.el-tabs--border-card) {
  border: none;
  box-shadow: none;
  border-radius: 22px;
}

:deep(.el-tabs--border-card > .el-tabs__header) {
  background: linear-gradient(180deg, #f8fafc 0%, #eef2ff 100%);
  border-bottom: 1px solid #e2e8f0;
  padding: 8px 14px 0;
}

:deep(.el-tabs--border-card > .el-tabs__content) {
  padding: 18px 18px 20px;
}

:deep(.el-form--inline .el-form-item) {
  margin-right: 14px;
  margin-bottom: 12px;
}

@media (max-width: 1400px) {
  .dashboard-grid {
    grid-template-columns: 1fr;
  }

  .stream-card {
    position: static;
  }
}

@media (max-width: 900px) {
  .stream-meta {
    grid-template-columns: 1fr;
  }
}
</style>
