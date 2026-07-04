<template>
  <div class="page">
    <div class="header">
      <div class="header__left">
        <h2 class="header__title">日志管理</h2>
        <span class="header__subtitle">操作日志、认证日志、运行日志与实时日志</span>
      </div>
    </div>

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
                    type="daterange"
                    range-separator="至"
                    start-placeholder="开始日期"
                    end-placeholder="结束日期"
                    value-format="YYYY-MM-DD HH:mm:ss"
                    :default-time="[new Date(2000, 1, 1, 0, 0, 0), new Date(2000, 1, 1, 23, 59, 59)]"
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
                    type="daterange"
                    range-separator="至"
                    start-placeholder="开始日期"
                    end-placeholder="结束日期"
                    value-format="YYYY-MM-DD HH:mm:ss"
                    :default-time="[new Date(2000, 1, 1, 0, 0, 0), new Date(2000, 1, 1, 23, 59, 59)]"
                    :shortcuts="shortcuts"
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
                    type="daterange"
                    range-separator="至"
                    start-placeholder="开始日期"
                    end-placeholder="结束日期"
                    value-format="YYYY-MM-DD HH:mm:ss"
                    :default-time="[new Date(2000, 1, 1, 0, 0, 0), new Date(2000, 1, 1, 23, 59, 59)]"
                    :shortcuts="shortcuts"
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

          <el-tab-pane name="realtime">
            <template #label>
              <span>实时日志
                <span class="realtime-dot" :style="{ background: sseStatusColor }"></span>
              </span>
            </template>
            <div class="log-tab-content realtime-tab">
              <div class="realtime-toolbar">
                <div class="filter-pills">
                  <button
                    v-for="lv in levelOptions"
                    :key="lv.value"
                    class="filter-pill"
                    :class="{ active: levelFilter.has(lv.value) }"
                    :style="levelFilter.has(lv.value) ? { background: lv.color, borderColor: lv.color } : {}"
                    @click="toggleLevel(lv.value)"
                  >
                    <span v-if="levelFilter.has(lv.value)" class="pill-check">&#10003;</span>
                    {{ lv.label }}
                  </button>
                </div>
                <div class="realtime-controls">
                  <el-tag :type="sseStatusTagType" size="small" effect="dark">{{ sseStatusText }}</el-tag>
                  <span class="line-count">{{ visibleLineCount }} 行</span>
                  <el-switch v-model="stream.autoScroll.value" size="small" active-text="自动滚动" />
                  <el-button size="small" text @click="stream.clear()">清空</el-button>
                </div>
              </div>
              <LogTerminal
                :lines="stream.lines.value"
                :level-filter="levelFilter"
                :auto-scroll="stream.autoScroll.value"
              />
            </div>
          </el-tab-pane>
        </el-tabs>
      </section>
  </div>
</template>

<script setup lang="ts">
import {computed, onMounted, ref, watch} from 'vue'
import {ElMessage} from 'element-plus'
import {
  getAuthEventTag,
  getBusinessTypeTag,
  getLevelTag,
  getRequestMethodTag
} from '@/utils/logTags'
import {useLogQuery} from './composables/useLogQuery'
import {useConsoleStream} from './composables/useLogStream'
import LogTerminal from './components/LogTerminal.vue'
import request from '@/utils/request'

type TabKey = 'operation' | 'auth' | 'runtime' | 'realtime'

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

const tabLabelMap: Record<TabKey, string> = {
  operation: '操作日志',
  auth: '认证日志',
  runtime: '运行日志',
  realtime: '实时日志'
}

const authEventOptions = ['LOGIN_SUCCESS', 'LOGIN_FAIL', 'LOGOUT', 'UNAUTHORIZED', 'TOKEN_INVALID']

// 快捷选项：最近7天
const shortcuts = [
  {
    text: '最近7天',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setTime(start.getTime() - 3600 * 1000 * 24 * 7)
      return [start, end]
    }
  },
  {
    text: '最近30天',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setTime(start.getTime() - 3600 * 1000 * 24 * 30)
      return [start, end]
    }
  }
]

// 获取默认时间范围：最近7天
const getDefaultTimeRange = (): [string, string] => {
  const end = new Date()
  const start = new Date()
  start.setTime(start.getTime() - 3600 * 1000 * 24 * 7)
  const format = (d: Date) => {
    const year = d.getFullYear()
    const month = String(d.getMonth() + 1).padStart(2, '0')
    const day = String(d.getDate()).padStart(2, '0')
    return `${year}-${month}-${day} 00:00:00`
  }
  const endStr = `${end.getFullYear()}-${String(end.getMonth() + 1).padStart(2, '0')}-${String(end.getDate()).padStart(2, '0')} 23:59:59`
  return [format(start), endStr]
}

const activeTab = ref<TabKey>('operation')

// ── Tab queries (operation / auth / runtime) ──
const opQuery = useLogQuery<{
  username: string;
  title: string;
  apiPath: string;
  execStatus: string;
  timeRange: string[]
}, OperationLogRecord>({
  endpoint: '/logs/operations/page',
  initialForm: {username: '', title: '', apiPath: '', execStatus: '', timeRange: getDefaultTimeRange()}
})
const authQuery = useLogQuery<{
  username: string;
  authEventType: string;
  resultStatus: string;
  timeRange: string[]
}, AuthLogRecord>({
  endpoint: '/logs/auth/page',
  initialForm: {username: '', authEventType: '', resultStatus: '', timeRange: getDefaultTimeRange()}
})
const runtimeQuery = useLogQuery<{
  level: string;
  loggerName: string;
  keyword: string;
  timeRange: string[]
}, RuntimeLogRecord>({
  endpoint: '/logs/runtime/page',
  initialForm: {level: '', loggerName: '', keyword: '', timeRange: getDefaultTimeRange()}
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

// ── Console log stream composable ──
const replayWindow = ref(180)
const stream = useConsoleStream(replayWindow)

const sseStatusText = computed(() => {
  const map: Record<string, string> = {
    disconnected: '未连接',
    connecting: '连接中',
    connected: '已连接',
    error: '异常'
  }
  return map[stream.status.value] || stream.status.value
})

const sseStatusTagType = computed(() => {
  const map: Record<string, '' | 'success' | 'warning' | 'danger' | 'info'> = {
    disconnected: 'info',
    connecting: 'warning',
    connected: 'success',
    error: 'danger'
  }
  return map[stream.status.value] || 'info'
})

const sseStatusColor = computed(() => {
  const map: Record<string, string> = {
    disconnected: '#909399',
    connecting: '#E6A23C',
    connected: '#67C23A',
    error: '#F56C6C'
  }
  return map[stream.status.value] || '#909399'
})

const levelFilter = ref<Set<string>>(new Set(['DEBUG', 'INFO', 'WARN', 'ERROR', 'CRITICAL']))

const levelOptions: { label: string; value: string; color: string }[] = [
  { label: 'DEBUG', value: 'DEBUG', color: '#6e7681' },
  { label: 'INFO', value: 'INFO', color: '#56d364' },
  { label: 'WARN', value: 'WARN', color: '#e3b341' },
  { label: 'ERROR', value: 'ERROR', color: '#f85149' },
  { label: 'CRIT', value: 'CRITICAL', color: '#ff6b9d' },
]

const toggleLevel = (level: string) => {
  const next = new Set(levelFilter.value)
  if (next.has(level)) next.delete(level)
  else next.add(level)
  levelFilter.value = next
}

const visibleLineCount = computed(() => {
  return stream.lines.value.filter((l) => levelFilter.value.has(l.level)).length
})

const refreshActiveTab = () => {
  if (activeTab.value === 'realtime') return
  const q = activeTab.value === 'operation' ? opQuery : activeTab.value === 'auth' ? authQuery : runtimeQuery
  q.fetch()
}

const handleTabChange = (tabName: string | number) => {
  activeTab.value = tabName as TabKey
  refreshActiveTab()
}

// keep SSE alive in background — start once, buffer keeps accumulating
watch(activeTab, (tab) => {
  if (tab === 'realtime') {
    stream.start()
  }
})

onMounted(async () => {
  try {
    const res: any = await request.get('/system/config/configKey/console_replay_window')
    const val = res?.data
    if (val != null) {
      const n = Number(val)
      if (!Number.isNaN(n) && n > 0) replayWindow.value = n
    }
  } catch { /* 使用默认值 180 */ }
  refreshActiveTab()
  if (activeTab.value === 'realtime') {
    stream.start()
  }
})
</script>

<style scoped>
.main-panel {
  min-height: 0;
  overflow: hidden;
  background: #ffffff;
  border-radius: 22px;
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.08);
  flex: 1;
}

.log-tab-content {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.log-tab-content .search {
  flex-shrink: 0;
}

/* ── Realtime log tab ── */

.realtime-tab {
  gap: 0;
}

.realtime-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 8px 0 10px;
  flex-shrink: 0;
  flex-wrap: wrap;
}

.filter-pills {
  display: flex;
  gap: 8px;
}

.filter-pill {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 4px 14px;
  border-radius: 20px;
  border: 1.5px solid #d0d5dd;
  background: #f9fafb;
  color: #6b7280;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  font-family: 'Fira Code', 'Consolas', monospace;
}

.filter-pill:hover {
  border-color: #9ca3af;
  color: #374151;
}

.filter-pill.active {
  color: #fff;
  border-color: transparent;
  font-weight: 600;
}

.pill-check {
  font-size: 11px;
  font-weight: 700;
}

.realtime-controls {
  display: flex;
  align-items: center;
  gap: 12px;
}

.line-count {
  font-size: 12px;
  color: #94a3b8;
  font-family: 'Fira Code', 'Consolas', monospace;
  min-width: 50px;
}

.console-label {
  font-family: 'Fira Code', 'Consolas', monospace;
  font-size: 12px;
  color: #64748b;
  letter-spacing: 1px;
}

.realtime-dot {
  display: inline-block;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  margin-left: 6px;
  vertical-align: middle;
}

/* ── Deep overrides ── */

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

/* 搜索框样式 */
.search {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 16px;
  align-items: center;
}

.search .el-input,
.search .el-select,
.search .el-date-editor {
  width: 200px;
}

.table-wrap {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.table-wrap__scroll {
  flex: 1;
  overflow: auto;
}

.table-wrap__pagination {
  flex-shrink: 0;
  padding: 12px 0 4px;
  display: flex;
  justify-content: flex-end;
}
</style>