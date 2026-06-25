<template>
  <div class="page service-status-view">
    <!-- 无权限 -->
    <div v-if="!canViewMqtt" class="no-permission">
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="#ef4444" stroke-width="2"
           width="48" height="48">
        <circle cx="12" cy="12" r="10"/>
        <line x1="4.93" y1="4.93" x2="19.07" y2="19.07"/>
      </svg>
      <p>无权限访问此页面</p>
    </div>

    <!-- 管理员 -->
    <template v-if="canViewMqtt">
      <div class="header">
        <div class="header__left">
          <h2 class="header__title">MQTT监控</h2>
          <span class="header__subtitle">Broker 运行状态、数据日志与在线客户端管理</span>
        </div>
      </div>

      <div class="tabs-row">
        <span class="tab-item" :class="{ active: activeTab === 'status' }" @click="switchTab('status')">MQTT状态</span>
        <span class="tab-item" :class="{ active: activeTab === 'log' }" @click="switchTab('log')">数据日志</span>
        <span class="tab-item" :class="{ active: activeTab === 'clients' }" @click="switchTab('clients')">
          在线客户端（{{ clientTotal }}）
        </span>
      </div>

      <!-- MQTT状态 -->
      <div v-show="activeTab === 'status'" class="tab-content table-wrap">
        <div class="table-wrap__scroll">
          <div class="status-grid">
          <div class="status-cell">
            <span class="sc-label">Broker 状态</span>
            <span class="sc-value">
              <span class="status-dot" :class="{ running: stats?.upstream, stopped: !stats?.upstream }"></span>
              {{ stats?.upstream ? '运行中' : '不可达' }}
            </span>
          </div>
          <div class="status-cell">
            <span class="sc-label">运行时长</span>
            <span class="sc-value">{{ uptimeText }}</span>
          </div>
          <div class="status-cell">
            <span class="sc-label">当前连接数</span>
            <span class="sc-value">{{ stats?.connections?.size ?? '-' }}</span>
          </div>
          <div class="status-cell">
            <span class="sc-label">用户数</span>
            <span class="sc-value">{{ stats?.nodes?.users ?? '-' }}</span>
          </div>
          <div class="status-cell">
            <span class="sc-label">历史接受连接</span>
            <span class="sc-value">{{ fmtNumber(stats?.connections?.accepted) }}</span>
          </div>
          <div class="status-cell">
            <span class="sc-label">历史关闭连接</span>
            <span class="sc-value">{{ fmtNumber(stats?.connections?.closed) }}</span>
          </div>
          <div class="status-cell">
            <span class="sc-label">已处理消息包</span>
            <span class="sc-value">{{ fmtNumber(stats?.messages?.handledPackets) }}</span>
          </div>
          <div class="status-cell">
            <span class="sc-label">已处理消息字节</span>
            <span class="sc-value">{{ fmtBytes(stats?.messages?.handledBytes) }}</span>
          </div>
          <div class="status-cell">
            <span class="sc-label">已接收消息包</span>
            <span class="sc-value">{{ fmtNumber(stats?.messages?.receivedPackets) }}</span>
          </div>
          <div class="status-cell">
            <span class="sc-label">已发送消息包</span>
            <span class="sc-value">{{ fmtNumber(stats?.messages?.sendPackets) }}</span>
          </div>
          <div class="status-cell">
            <span class="sc-label">TCP接收速率</span>
            <span class="sc-value">{{
                stats?.messages?.packetsPerTcpReceive ?? '-'
              }} 包/s · {{ stats?.messages?.bytesPerTcpReceive ?? '-' }} B/s</span>
          </div>
          <div class="status-cell">
            <span class="sc-label">认证状态</span>
            <span class="sc-value">{{ config?.authEnabled ? '已启用' : '未启用' }}</span>
          </div>
          <div class="status-cell">
            <span class="sc-label">心跳超时</span>
            <span class="sc-value">{{ config ? (config.heartbeatTimeout / 1000) + 's' : '-' }}</span>
          </div>
          <div class="status-cell">
            <span class="sc-label">消息大小限制</span>
            <span class="sc-value">{{ config?.maxBytesInMessage ?? '-' }}</span>
          </div>
          <div class="status-cell">
            <span class="sc-label">监听器</span>
            <span class="sc-value">
              <span v-if="enabledListeners.length">{{ enabledListeners.join(' · ') }}</span>
              <span v-else class="text-muted">加载中...</span>
            </span>
          </div>
        </div>
      </div>
      </div>

      <!-- 数据日志 -->
      <div v-show="activeTab === 'log'" class="tab-content table-wrap">
        <div class="table-wrap__scroll">
          <div class="query-bar">
          <div class="query-item">
            <label>Client ID</label>
            <input v-model="logQuery.clientId" type="text" placeholder="请输入 Client ID"/>
          </div>
          <div class="query-item">
            <label>主题</label>
            <input v-model="logQuery.topic" type="text" placeholder="请输入主题"/>
          </div>
          <div class="query-actions">
            <el-button type="primary" @click="handleLogSearch">查询</el-button>
            <el-button @click="handleLogReset">重置</el-button>
          </div>
        </div>
        <el-table :data="logs" border stripe v-loading="logsLoading">
          <el-table-column type="index" label="序号" width="60" align="center" />
          <el-table-column label="接收时间" min-width="170" align="center">
            <template #default="{ row }">{{ formatTimestamp(row.receiveTime) }}</template>
          </el-table-column>
          <el-table-column label="Client ID" min-width="150" align="center">
            <template #default="{ row }"><code>{{ row.clientId || '-' }}</code></template>
          </el-table-column>
          <el-table-column prop="username" label="用户名" width="110" align="center" />
          <el-table-column label="主题" min-width="170" align="center">
            <template #default="{ row }"><code>{{ row.topic }}</code></template>
          </el-table-column>
          <el-table-column label="消息内容" min-width="200">
            <template #default="{ row }"><span class="log-message-cell" :title="row.payload">{{ row.payload }}</span></template>
          </el-table-column>
          <el-table-column label="大小" width="80" align="center">
            <template #default="{ row }">{{ fmtBytes(row.payloadSize) }}</template>
          </el-table-column>
        </el-table>
      </div>
      <div class="table-wrap__pagination" v-if="logTotal > 0">
        <el-pagination
          v-model:current-page="logPage"
          v-model:page-size="logPageSize"
          :total="logTotal"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="fetchLogs"
          @current-change="fetchLogs"
        />
      </div>
    </div>

      <!-- 在线客户端 -->
      <div v-show="activeTab === 'clients'" class="tab-content table-wrap">
        <div class="table-wrap__scroll">
          <div class="client-toolbar" v-if="clients.length > 0">
          <el-button
              type="danger"
              :disabled="selectedClientIds.length === 0"
              @click="handleBatchKick"
              size="small"
          >
            批量踢出 {{ selectedClientIds.length ? '(' + selectedClientIds.length + ')' : '' }}
          </el-button>
        </div>
        <el-table
          :data="clients"
          border
          stripe
          v-loading="clientsLoading"
          @selection-change="handleClientSelectionChange"
        >
          <el-table-column type="selection" width="50" align="center" />
          <el-table-column type="index" label="序号" width="60" align="center" />
          <el-table-column label="Client ID" min-width="170" align="center">
            <template #default="{ row }"><code>{{ row.clientId }}</code></template>
          </el-table-column>
          <el-table-column prop="username" label="用户名" width="110" align="center" />
          <el-table-column label="IP 地址" min-width="130" align="center">
            <template #default="{ row }">{{ row.ipAddress }}:{{ row.port }}</template>
          </el-table-column>
          <el-table-column label="连接时间" min-width="170" align="center">
            <template #default="{ row }">{{ formatTimestamp(row.connectedAt) }}</template>
          </el-table-column>
          <el-table-column label="设备名称" width="130" align="center">
            <template #default="{ row }">{{ row.deviceName || '-' }}</template>
          </el-table-column>
          <el-table-column label="隐患点" width="130" align="center">
            <template #default="{ row }">{{ row.hazardPointName || '-' }}</template>
          </el-table-column>
          <el-table-column label="操作" width="200" fixed="right" align="center">
            <template #default="{ row }">
              <div class="op-cell">
                <el-button type="primary" text size="small" @click="openClientDetail(row)">详情</el-button>
                <el-button type="danger" text size="small" @click="handleKickClient(row)">踢出</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
        </div>
      <div class="table-wrap__pagination" v-if="clientTotal > 0">
        <el-pagination
          v-model:current-page="clientPage"
          v-model:page-size="clientPageSize"
          :total="clientTotal"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="fetchClients"
          @current-change="fetchClients"
        />
      </div>
      </div>
    </template>

    <!-- 客户端详情弹窗 -->
    <el-dialog v-model="detailVisible" title="客户端详情" width="640px" destroy-on-close>
      <div v-if="detailLoading" style="text-align:center;padding:40px 0;color:#94a3b8">加载中...</div>
      <div v-else-if="detailInfo" class="table-wrap">
        <div class="table-wrap__scroll">
          <table class="info-table">
            <tbody>
            <tr>
              <td class="info-label">Client ID</td>
              <td class="info-value"><code>{{ detailInfo.clientId }}</code></td>
            </tr>
            <tr>
              <td class="info-label">用户名</td>
              <td class="info-value">{{ detailInfo.username }}</td>
            </tr>
            <tr>
              <td class="info-label">IP 地址</td>
              <td class="info-value">{{ detailInfo.ipAddress }}:{{ detailInfo.port }}</td>
            </tr>
            <tr>
              <td class="info-label">连接时间</td>
              <td class="info-value">{{ formatTimestamp(detailInfo.connectedAt) }}</td>
            </tr>
            <tr>
              <td class="info-label">协议</td>
              <td class="info-value">{{ detailInfo.protoName }} v{{ detailInfo.protoVer }}</td>
            </tr>
            <tr>
              <td class="info-label">设备名称</td>
              <td class="info-value">{{ detailInfo.deviceName || '-' }}</td>
            </tr>
            <tr>
              <td class="info-label">设备编号</td>
              <td class="info-value">{{ detailInfo.deviceCode || '-' }}</td>
            </tr>
            <tr>
              <td class="info-label">隐患点</td>
              <td class="info-value">{{ detailInfo.hazardPointName || '-' }}</td>
            </tr>
            <tr>
              <td class="info-label">最后认证IP</td>
              <td class="info-value">{{ detailInfo.lastAuthIp }}</td>
            </tr>
            <tr>
              <td class="info-label">最后认证时间</td>
              <td class="info-value">{{ detailInfo.lastAuthTime }}</td>
            </tr>
            <tr>
              <td class="info-label">订阅主题</td>
              <td class="info-value">
                <div class="topic-list" v-if="detailSubscriptions.length">
                  <div v-for="sub in detailSubscriptions" :key="sub.topicFilter" class="topic-tag-row">
                    <code>{{ sub.topicFilter }}</code>
                    <span class="qos-tag">QoS {{ sub.mqttQoS }}</span>
                  </div>
                </div>
                <span v-else class="text-muted">无订阅</span>
              </td>
            </tr>
            </tbody>
          </table>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import {computed, onMounted, onUnmounted, ref} from 'vue'
import { ElMessage } from 'element-plus'
import {hasPermission} from '@/utils/permission'
import {
  getMqttClientDetail,
  getMqttClients,
  getMqttConfig,
  getMqttListeners,
  getMqttMessages,
  getMqttStats,
  kickMqttClient,
  kickMqttClients,
  type MqttClientItem,
  type MqttConfig,
  type MqttListener,
  type MqttMessageLogItem,
  type MqttStats,
  type MqttSubscription
} from '@/api/monitor'

// ===== 权限 =====
const canViewMqtt = computed(() => hasPermission('monitor:mqtt:list'))

onMounted(() => {
  if (canViewMqtt.value) {
    fetchStats()
    fetchListeners()
    fetchConfig()
  }
})

// ===== 标签页 =====
const activeTab = ref<'status' | 'log' | 'clients'>('status')

const switchTab = (tab: typeof activeTab.value) => {
  activeTab.value = tab
  if (tab === 'status') {
    fetchStats()
    fetchListeners()
    fetchConfig()
  } else if (tab === 'clients' && clients.value.length === 0) {
    fetchClients()
  }
}

// ===== MQTT状态 =====
const stats = ref<MqttStats | null>(null)
const listeners = ref<MqttListener[]>([])
const config = ref<MqttConfig | null>(null)
let uptimeTimer: ReturnType<typeof setInterval> | null = null

const uptimeText = ref('')

const calcUptime = () => {
  if (stats.value?.startTime) {
    const diff = Date.now() - stats.value.startTime
    const days = Math.floor(diff / 86400000)
    const hours = Math.floor((diff % 86400000) / 3600000)
    const mins = Math.floor((diff % 3600000) / 60000)
    uptimeText.value = `${days}天 ${hours}小时 ${mins}分钟`
  } else {
    uptimeText.value = '-'
  }
}

const fetchStats = async () => {
  try {
    const res = await getMqttStats()
    stats.value = res.data
    calcUptime()
    if (!uptimeTimer) {
      uptimeTimer = setInterval(calcUptime, 60000)
    }
  } catch { /* ignore */
  }
}

const fetchListeners = async () => {
  try {
    const res = await getMqttListeners()
    listeners.value = res.data ?? []
  } catch { /* ignore */
  }
}

const fetchConfig = async () => {
  try {
    const res = await getMqttConfig()
    config.value = res.data
  } catch { /* ignore */
  }
}

const enabledListeners = computed(() =>
    listeners.value.filter(l => l.enabled).map(l => `${l.type}://${l.ip}:${l.port}`)
)

const fmtNumber = (v?: number) => v != null ? v.toLocaleString() : '-'
const fmtBytes = (v?: number) => {
  if (v == null) return '-'
  if (v < 1024) return v + ' B'
  if (v < 1048576) return (v / 1024).toFixed(1) + ' KB'
  return (v / 1048576).toFixed(1) + ' MB'
}

const formatTimestamp = (ts?: number) => {
  if (!ts) return '-'
  const d = new Date(ts)
  const pad = (n: number) => n.toString().padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

onUnmounted(() => {
  if (uptimeTimer) clearInterval(uptimeTimer)
})

// ===== 数据日志 =====
const logQuery = ref({clientId: '', topic: ''})
const logPage = ref(1)
const logPageSize = ref(10)
const logTotal = ref(0)
const logs = ref<MqttMessageLogItem[]>([])
const logsLoading = ref(false)

const fetchLogs = async () => {
  logsLoading.value = true
  try {
    const res = await getMqttMessages({
      page: logPage.value,
      pageSize: logPageSize.value,
      clientId: logQuery.value.clientId || undefined,
      topic: logQuery.value.topic || undefined
    })
    const data = res.data
    logs.value = data?.list ?? []
    logTotal.value = data?.totalRow ?? 0
  } catch {
    logs.value = []
    logTotal.value = 0
  } finally {
    logsLoading.value = false
  }
}

const handleLogSearch = () => {
  logPage.value = 1
  fetchLogs()
}

const handleLogReset = () => {
  logQuery.value = {clientId: '', topic: ''}
  logPage.value = 1
  fetchLogs()
}

// ===== 在线客户端 =====
const clients = ref<MqttClientItem[]>([])
const clientPage = ref(1)
const clientPageSize = ref(20)
const clientTotal = ref(0)
const clientsLoading = ref(false)

const fetchClients = async () => {
  clientsLoading.value = true
  try {
    const res = await getMqttClients({page: clientPage.value, limit: clientPageSize.value})
    const data = res.data
    clients.value = data?.list ?? []
    clientTotal.value = data?.totalRow ?? 0
  } catch {
    clients.value = []
  } finally {
    clientsLoading.value = false
  }
}

const selectedClientIds = ref<string[]>([])

const handleClientSelectionChange = (rows: MqttClientItem[]) => {
  selectedClientIds.value = rows.map(c => c.clientId)
}

const handleBatchKick = async () => {
  if (selectedClientIds.value.length === 0) return
  try {
    const res = await kickMqttClients(selectedClientIds.value)
    const data = res.data
    ElMessage.success(`批量踢出完成：成功 ${data?.success ?? selectedClientIds.value.length}，失败 ${data?.fail ?? 0}`)
    selectedClientIds.value = []
    fetchClients()
  } catch {
    ElMessage.error('批量踢出失败')
  }
}

const handleKickClient = async (client: MqttClientItem) => {
  try {
    await kickMqttClient(client.clientId)
    selectedClientIds.value = selectedClientIds.value.filter(id => id !== client.clientId)
    fetchClients()
  } catch {
    ElMessage.error('踢出失败，请确认 MQTT HTTP API 已启用且客户端在线')
  }
}

// ===== 客户端详情弹窗 =====
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailInfo = ref<MqttClientItem | null>(null)
const detailSubscriptions = ref<MqttSubscription[]>([])

const openClientDetail = async (client: MqttClientItem) => {
  detailVisible.value = true
  detailLoading.value = true
  detailInfo.value = null
  detailSubscriptions.value = []
  try {
    const res = await getMqttClientDetail(client.clientId)
    detailInfo.value = res.data.info
    detailSubscriptions.value = res.data.subscriptions ?? []
  } catch {
    detailInfo.value = client
  } finally {
    detailLoading.value = false
  }
}
</script>

<style scoped>
.service-status-view {
  background: transparent;
}

.no-permission {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  gap: 16px;
  color: #ef4444;
  font-size: 16px;
}

/* 标签页 */
.tabs-row {
  display: flex;
  gap: 0;
  border-bottom: 2px solid #e2e8f0;
  margin-bottom: 12px;
  flex-shrink: 0;
}

.tab-item {
  padding: 8px 20px;
  font-size: 14px;
  color: #64748b;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  margin-bottom: -2px;
  transition: all 0.2s;
  user-select: none;
}

.tab-item:hover {
  color: #3b82f6;
}

.tab-item.active {
  color: #3b82f6;
  font-weight: 600;
  border-bottom-color: #3b82f6;
}

.tab-content {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
}

/* MQTT状态表 */
.info-table {
  width: 100%;
  border-collapse: collapse;
}

.info-table td {
  padding: 12px 24px;
  font-size: 14px;
  color: #334155;
  border-bottom: 1px solid #f1f5f9;
}

.info-table tr:last-child td {
  border-bottom: none;
}

.info-label {
  width: 160px;
  font-weight: 600;
  color: #64748b;
  background: #f8fafc;
}

.info-value code {
  font-family: 'SF Mono', 'Fira Code', monospace;
  font-size: 13px;
  background: #f1f5f9;
  padding: 2px 8px;
  border-radius: 4px;
  color: #6366f1;
}

.status-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 6px;
  vertical-align: middle;
}

.status-dot.running {
  background: #10b981;
  box-shadow: 0 0 6px rgba(16, 185, 129, 0.5);
  animation: dotPulse 2s infinite;
}

.status-dot.stopped {
  background: #ef4444;
}

@keyframes dotPulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.3;
  }
}

.text-muted {
  color: #94a3b8;
}

/* 状态网格 */
.status-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
}

.status-cell {
  display: flex;
  align-items: center;
  padding: 10px 24px;
  border-bottom: 1px solid #f1f5f9;
  gap: 12px;
}

.sc-label {
  font-size: 14px;
  font-weight: 600;
  color: #64748b;
  white-space: nowrap;
  min-width: 100px;
}

.sc-value {
  font-size: 14px;
  color: #334155;
}

.sc-value code {
  font-family: 'SF Mono', 'Fira Code', monospace;
  font-size: 13px;
  background: #f1f5f9;
  padding: 2px 8px;
  border-radius: 4px;
  color: #6366f1;
}

.listener-remark {
  font-size: 13px;
  color: #94a3b8;
}

/* 查询栏 */
.query-bar {
  display: flex;
  align-items: flex-end;
  gap: 16px;
  padding: 14px 24px;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
  flex-wrap: wrap;
}

.query-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.query-item label {
  font-size: 13px;
  color: #64748b;
  font-weight: 500;
}

.query-item input[type="text"] {
  width: 180px;
  height: 32px;
  padding: 0 10px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 14px;
  color: #334155;
  background: #fff;
  outline: none;
  transition: border-color 0.2s;
}

.query-item input[type="text"]:focus {
  border-color: #3b82f6;
}

.query-item input[type="datetime-local"] {
  width: 175px;
  height: 32px;
  padding: 0 8px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 13px;
  color: #334155;
  background: #fff;
  outline: none;
}

.query-item input[type="datetime-local"]:focus {
  border-color: #3b82f6;
}

.query-actions {
  display: flex;
  gap: 8px;
  align-items: flex-end;
  padding-bottom: 0;
}

.log-message-cell {
  max-width: 260px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
}

/* 客户端工具栏 */
.client-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 20px;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
}

/* 分页 */
.topic-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.topic-tag-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.qos-tag {
  font-size: 11px;
  background: #f1f5f9;
  color: #64748b;
  padding: 1px 6px;
  border-radius: 4px;
}
</style>
