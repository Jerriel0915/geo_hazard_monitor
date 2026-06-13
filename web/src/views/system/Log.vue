<template>
  <div class="page">
    <div class="header">
      <div class="header__left">
        <h2 class="header__title">日志管理</h2>
        <span class="header__subtitle">操作日志、认证日志、运行日志与实时流监控</span>
      </div>
    </div>

    <div class="dashboard-grid">
      <section class="main-panel">
        <el-tabs v-model="activeTab" type="border-card" @tab-change="handleTabChange">
          <el-tab-pane label="操作日志" name="operation">
            <div class="log-tab-content">
              <div class="search">
                <el-input v-model="opSearchForm.username" placeholder="操作用户" clearable />
                <el-input v-model="opSearchForm.title" placeholder="业务标题" clearable />
                <el-input v-model="opSearchForm.apiPath" placeholder="接口路径" clearable />
                <el-select v-model="opSearchForm.execStatus" placeholder="执行状态" clearable>
                  <el-option label="成功" value="SUCCESS"/>
                  <el-option label="失败" value="FAIL"/>
                </el-select>
                <el-date-picker
                    v-model="opSearchForm.timeRange"
                    type="datetimerange"
                    range-separator=""
                    start-placeholder="开始"
                    end-placeholder="结束"
                    value-format="YYYY-MM-DD HH:mm:ss"
                />
                <el-button type="primary" @click="handleOperationSearch">查询</el-button>
                <el-button @click="handleOperationReset">重置</el-button>
                <el-button @click="refreshActiveTab">刷新</el-button>
              </div>

              <div class="table-wrap">
                <div class="table-wrap__scroll">
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
                    <el-table-column prop="username" label="操作用户" width="110"/>
                    <el-table-column prop="apiPath" label="接口路径" min-width="200" show-overflow-tooltip/>
                    <el-table-column prop="requestMethod" label="请求方式" width="90" align="center">
                      <template #default="{ row }">
                        <el-tag :type="getRequestMethodTag(row.requestMethod)" size="small">
                          {{ row.requestMethod || '--' }}
                        </el-tag>
                      </template>
                    </el-table-column>
                    <el-table-column prop="clientIp" label="IP地址" width="120"/>
                    <el-table-column prop="execStatus" label="状态" width="80" align="center">
                      <template #default="{ row }">
                        <el-tag :type="row.execStatus === 'SUCCESS' ? 'success' : 'danger'" size="small">
                          {{ row.execStatus === 'SUCCESS' ? '成功' : '失败' }}
                        </el-tag>
                      </template>
                    </el-table-column>
                    <el-table-column prop="costTimeMs" label="耗时(ms)" width="90" align="center"/>
                    <el-table-column prop="occurredAt" label="操作时间" min-width="170"/>
                  </el-table>
                </div>

                <div class="table-wrap__pagination">
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
            </div>
          </el-tab-pane>

          <el-tab-pane label="认证日志" name="auth">
            <div class="log-tab-content">
              <div class="search">
                <el-input v-model="authSearchForm.username" placeholder="用户名" clearable />
                <el-select v-model="authSearchForm.authEventType" placeholder="事件类型" clearable>
                  <el-option v-for="item in authEventOptions" :key="item" :label="item" :value="item"/>
                </el-select>
                <el-select v-model="authSearchForm.resultStatus" placeholder="结果状态" clearable>
                  <el-option label="成功" value="SUCCESS"/>
                  <el-option label="失败" value="FAIL"/>
                </el-select>
                <el-date-picker
                    v-model="authSearchForm.timeRange"
                    type="datetimerange"
                    range-separator=""
                    start-placeholder="认证时间:开始"
                    end-placeholder="认证时间:结束"
                    value-format="YYYY-MM-DD HH:mm:ss"
                />
                <el-button type="primary" @click="handleAuthSearch">查询</el-button>
                <el-button @click="handleAuthReset">重置</el-button>
                <el-button @click="refreshActiveTab">刷新</el-button>
              </div>

              <div class="table-wrap">
                <div class="table-wrap__scroll">
                  <el-table :data="authLogs" border stripe v-loading="authLoading">
                    <el-table-column type="index" label="序号" width="60" align="center"/>
                    <el-table-column prop="username" label="用户名" width="110"/>
                    <el-table-column prop="authEventType" label="认证事件" width="140" align="center">
                      <template #default="{ row }">
                        <el-tag :type="getAuthEventTag(row.authEventType)" size="small">
                          {{ row.authEventType || '--' }}
                        </el-tag>
                      </template>
                    </el-table-column>
                    <el-table-column prop="authChannel" label="认证通道" width="110" align="center"/>
                    <el-table-column prop="requestUri" label="请求URI" min-width="200" show-overflow-tooltip/>
                    <el-table-column prop="clientIp" label="IP地址" width="120"/>
                    <el-table-column prop="resultStatus" label="结果" width="80" align="center">
                      <template #default="{ row }">
                        <el-tag :type="row.resultStatus === 'SUCCESS' ? 'success' : 'danger'" size="small">
                          {{ row.resultStatus === 'SUCCESS' ? '成功' : '失败' }}
                        </el-tag>
                      </template>
                    </el-table-column>
                    <el-table-column prop="failureMessage" label="消息" min-width="180" show-overflow-tooltip/>
                    <el-table-column prop="occurredAt" label="认证时间" min-width="170"/>
                  </el-table>
                </div>

                <div class="table-wrap__pagination">
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
            </div>
          </el-tab-pane>

          <el-tab-pane label="运行日志" name="runtime">
            <div class="log-tab-content">
              <div class="search">
                <el-select v-model="runtimeSearchForm.level" placeholder="日志级别" clearable>
                  <el-option label="INFO" value="INFO"/>
                  <el-option label="WARN" value="WARN"/>
                  <el-option label="ERROR" value="ERROR"/>
                </el-select>
                <el-input v-model="runtimeSearchForm.loggerName" placeholder="Logger 名称" clearable />
                <el-input v-model="runtimeSearchForm.keyword" placeholder="日志内容关键词" clearable />
                <el-date-picker
                    v-model="runtimeSearchForm.timeRange"
                    type="datetimerange"
                    range-separator=""
                    start-placeholder="发生时间:开始"
                    end-placeholder="发生时间:结束"
                    value-format="YYYY-MM-DD HH:mm:ss"
                />
                <el-button type="primary" @click="handleRuntimeSearch">查询</el-button>
                <el-button @click="handleRuntimeReset">重置</el-button>
                <el-button @click="refreshActiveTab">刷新</el-button>
              </div>

              <div class="table-wrap">
                <div class="table-wrap__scroll">
                  <el-table :data="runtimeLogs" border stripe v-loading="runtimeLoading">
                    <el-table-column type="index" label="序号" width="60" align="center"/>
                    <el-table-column prop="level" label="级别" width="90" align="center">
                      <template #default="{ row }">
                        <el-tag :type="getLevelTag(row.level)" effect="dark" size="small">{{ row.level }}</el-tag>
                      </template>
                    </el-table-column>
                    <el-table-column prop="loggerName" label="Logger" min-width="240" show-overflow-tooltip/>
                    <el-table-column prop="threadName" label="线程" min-width="150" show-overflow-tooltip/>
                    <el-table-column prop="sourceApp" label="来源应用" width="110"/>
                    <el-table-column prop="messageDigest" label="日志摘要" min-width="260" show-overflow-tooltip/>
                    <el-table-column prop="occurredAt" label="发生时间" min-width="170"/>
                  </el-table>
                </div>

                <div class="table-wrap__pagination">
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
import {computed, onBeforeUnmount, onMounted, ref} from 'vue'
import {ElMessage} from 'element-plus'
import {handleAuthFailure} from '@/utils/auth'
import {
  getAuthEventTag,
  getBusinessTypeTag,
  getLevelTag,
  getLiveSubtypeTag,
  getRequestMethodTag,
  getStreamEventTag
} from '@/utils/logTags'
import {useLogQuery} from './composables/useLogQuery'

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

// ── Tab queries (operation / auth / runtime) ──
const opQuery = useLogQuery<{
  username: string;
  title: string;
  apiPath: string;
  execStatus: string;
  timeRange: string[]
}, OperationLogRecord>({
  endpoint: '/api/v1/logs/operations/page',
  initialForm: {username: '', title: '', apiPath: '', execStatus: '', timeRange: []}
})
const authQuery = useLogQuery<{
  username: string;
  authEventType: string;
  resultStatus: string;
  timeRange: string[]
}, AuthLogRecord>({
  endpoint: '/api/v1/logs/auth/page',
  initialForm: {username: '', authEventType: '', resultStatus: '', timeRange: []}
})
const runtimeQuery = useLogQuery<{
  level: string;
  loggerName: string;
  keyword: string;
  timeRange: string[]
}, RuntimeLogRecord>({
  endpoint: '/api/v1/logs/runtime/page',
  initialForm: {level: '', loggerName: '', keyword: '', timeRange: []}
})

const {
  loading: operationLoading,
  records: operationLogs,
  pagination: operationPagination,
  searchForm: opSearchForm,
  search: handleOperationSearch,
  reset: handleOperationReset,
  handleSizeChange: handleOperationSizeChange,
  handlePageChange: handleOperationPageChange
} = opQuery
const {
  loading: authLoading,
  records: authLogs,
  pagination: authPagination,
  searchForm: authSearchForm,
  search: handleAuthSearch,
  reset: handleAuthReset,
  handleSizeChange: handleAuthSizeChange,
  handlePageChange: handleAuthPageChange
} = authQuery
const {
  loading: runtimeLoading,
  records: runtimeLogs,
  pagination: runtimePagination,
  searchForm: runtimeSearchForm,
  search: handleRuntimeSearch,
  reset: handleRuntimeReset,
  handleSizeChange: handleRuntimeSizeChange,
  handlePageChange: handleRuntimePageChange
} = runtimeQuery

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

const refreshActiveTab = () => {
  const q = activeTab.value === 'operation' ? opQuery : activeTab.value === 'auth' ? authQuery : runtimeQuery
  q.fetch()
}

const handleTabChange = (tabName: string | number) => {
  activeTab.value = tabName as TabKey
  refreshActiveTab()
}

// ========== SSE stream logic (unchanged) ==========

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
      keepStreamAlive = false
      streamAbortController = null
      sseStatus.value = 'disconnected'
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
    if (!isReconnect) {
      ElMessage.warning('实时日志流连接中断，正在尝试重连')
    }
    scheduleReconnect()
  }
}

const clearLiveEvents = () => {
  liveEvents.value = []
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
.dashboard-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  grid-template-rows: 1fr;
  gap: 12px;
  flex: 1;
  min-height: 0;
}

.main-panel {
  min-height: 0;
  overflow: hidden;
  background: #ffffff;
  border-radius: 22px;
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.08);
}

.log-tab-content {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.log-tab-content .search {
  flex-shrink: 0;
}

.stream-panel {
  min-height: 0;
}

.stream-card {
  height: 100%;
  background: #ffffff;
  border-radius: 22px;
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.08);
  padding: 22px 20px;
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

.stream-card > .stream-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding-right: 4px;
  margin-top: 18px;
  display: flex;
  flex-direction: column;
  gap: 12px;
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
  height: 100%;
  display: flex;
  flex-direction: column;
}

:deep(.el-tabs--border-card > .el-tabs__header) {
  background: linear-gradient(180deg, #f8fafc 0%, #eef2ff 100%);
  border-bottom: 1px solid #e2e8f0;
  padding: 8px 14px 0;
  flex-shrink: 0;
}

:deep(.el-tabs--border-card > .el-tabs__content) {
  padding: 14px 18px 16px;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

:deep(.el-tab-pane) {
  height: 100%;
}

@media (max-width: 1400px) {
  .dashboard-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 900px) {
  .stream-meta {
    grid-template-columns: 1fr;
  }
}
</style>
