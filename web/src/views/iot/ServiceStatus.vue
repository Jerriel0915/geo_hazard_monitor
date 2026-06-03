<template>
  <div class="service-status-view">
    <!-- 无权限 -->
    <div v-if="!isAdmin" class="no-permission">
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="#ef4444" stroke-width="2" width="48" height="48">
        <circle cx="12" cy="12" r="10"/>
        <line x1="4.93" y1="4.93" x2="19.07" y2="19.07"/>
      </svg>
      <p>无权限访问此页面</p>
    </div>

    <!-- 管理员 -->
    <template v-if="isAdmin">
      <div class="page-title">MQTT监控</div>

      <div class="tabs-row">
        <span class="tab-item" :class="{ active: activeTab === 'status' }" @click="switchTab('status')">MQTT状态</span>
        <span class="tab-item" :class="{ active: activeTab === 'log' }" @click="switchTab('log')">数据日志</span>
        <span class="tab-item" :class="{ active: activeTab === 'clients' }" @click="switchTab('clients')">
          在线客户端（{{ clientTotal }}）
        </span>
      </div>

      <!-- MQTT状态 -->
      <div v-show="activeTab === 'status'" class="tab-content">
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
            <span class="sc-value">{{ stats?.messages?.packetsPerTcpReceive ?? '-' }} 包/s · {{ stats?.messages?.bytesPerTcpReceive ?? '-' }} B/s</span>
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

      <!-- 数据日志（暂无接口，保留mock） -->
      <div v-show="activeTab === 'log'" class="tab-content">
        <div class="query-bar">
          <div class="query-item">
            <label>Client ID</label>
            <input v-model="logQuery.clientId" type="text" placeholder="请输入 Client ID" />
          </div>
          <div class="query-item">
            <label>主题</label>
            <input v-model="logQuery.topic" type="text" placeholder="请输入主题" />
          </div>
          <div class="query-item query-item-time">
            <label>时间范围</label>
            <div class="time-inputs">
              <input v-model="logQuery.startTime" type="datetime-local" />
              <span class="time-separator">至</span>
              <input v-model="logQuery.endTime" type="datetime-local" />
            </div>
          </div>
          <div class="query-actions">
            <button class="btn-query" @click="handleLogSearch">查询</button>
            <button class="btn-reset" @click="handleLogReset">重置</button>
          </div>
        </div>
        <table class="data-table">
          <thead>
            <tr>
              <th style="width:60px;">序号</th>
              <th style="width:160px;">接收时间</th>
              <th style="width:140px;">Client ID</th>
              <th style="width:100px;">用户名</th>
              <th style="width:160px;">主题</th>
              <th>消息内容</th>
              <th style="width:80px;">大小</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(log, index) in logs" :key="log.id">
              <td>{{ (logPage - 1) * logPageSize + index + 1 }}</td>
              <td>{{ log.receiveTime }}</td>
              <td><code>{{ log.clientId || '-' }}</code></td>
              <td>{{ log.username }}</td>
              <td><code>{{ log.topic }}</code></td>
              <td class="log-message-cell">{{ log.message }}</td>
              <td>{{ log.size }}</td>
            </tr>
            <tr v-if="logs.length === 0">
              <td colspan="7" class="empty-row">暂无数据</td>
            </tr>
          </tbody>
        </table>
        <div class="pagination-row" v-if="logTotal > logPageSize">
          <span class="page-info">共 {{ logTotal }} 条</span>
          <div class="page-btns">
            <button :disabled="logPage <= 1" @click="logPage--; handleLogSearch()">上一页</button>
            <span class="page-num">{{ logPage }} / {{ Math.ceil(logTotal / logPageSize) }}</span>
            <button :disabled="logPage >= Math.ceil(logTotal / logPageSize)" @click="logPage++; handleLogSearch()">下一页</button>
          </div>
        </div>
      </div>

      <!-- 在线客户端 -->
      <div v-show="activeTab === 'clients'" class="tab-content">
        <div class="client-toolbar" v-if="clients.length > 0">
          <label class="select-all">
            <input type="checkbox" :checked="isAllSelected" @change="toggleSelectAll" />
            <span>全选</span>
          </label>
          <button
            class="btn-batch-kick"
            :disabled="selectedClientIds.length === 0"
            @click="handleBatchKick"
          >
            批量踢出 {{ selectedClientIds.length ? '(' + selectedClientIds.length + ')' : '' }}
          </button>
        </div>
        <table class="data-table">
          <thead>
            <tr>
              <th style="width:40px;">
                <input type="checkbox" :checked="isAllSelected" @change="toggleSelectAll" />
              </th>
              <th style="width:55px;">序号</th>
              <th style="width:170px;">Client ID</th>
              <th style="width:100px;">用户名</th>
              <th style="width:130px;">IP 地址</th>
              <th style="width:160px;">连接时间</th>
              <th style="width:130px;">设备名称</th>
              <th style="width:120px;">隐患点</th>
              <th style="width:120px;">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(client, index) in clients" :key="client.clientId">
              <td>
                <input type="checkbox" :checked="selectedClientIds.includes(client.clientId)" @change="toggleSelect(client.clientId)" />
              </td>
              <td>{{ (clientPage - 1) * clientPageSize + index + 1 }}</td>
              <td><code>{{ client.clientId }}</code></td>
              <td>{{ client.username }}</td>
              <td>{{ client.ipAddress }}:{{ client.port }}</td>
              <td>{{ formatTimestamp(client.connectedAt) }}</td>
              <td>{{ client.deviceName || '-' }}</td>
              <td>{{ client.hazardPointName || '-' }}</td>
              <td>
                <div class="row-actions">
                  <button class="btn-detail" @click="openClientDetail(client)">详情</button>
                  <button class="btn-kick" @click="handleKickClient(client)">踢出</button>
                </div>
              </td>
            </tr>
            <tr v-if="clients.length === 0">
              <td colspan="9" class="empty-row">{{ clientsLoading ? '加载中...' : '暂无在线客户端' }}</td>
            </tr>
          </tbody>
        </table>
        <div class="pagination-row" v-if="clientTotal > clientPageSize">
          <span class="page-info">共 {{ clientTotal }} 条</span>
          <div class="page-btns">
            <button :disabled="clientPage <= 1" @click="clientPage--; fetchClients()">上一页</button>
            <span class="page-num">{{ clientPage }} / {{ Math.ceil(clientTotal / clientPageSize) }}</span>
            <button :disabled="clientPage >= Math.ceil(clientTotal / clientPageSize)" @click="clientPage++; fetchClients()">下一页</button>
          </div>
        </div>
      </div>
    </template>

    <!-- 客户端详情弹窗 -->
    <div v-if="detailVisible" class="dialog-overlay" @click.self="detailVisible = false">
      <div class="dialog-panel">
        <div class="dialog-header">
          <span class="dialog-title">客户端详情</span>
          <button class="dialog-close" @click="detailVisible = false">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="20" height="20">
              <path d="M18 6L6 18M6 6l12 12"/>
            </svg>
          </button>
        </div>
        <div class="dialog-body" v-if="detailLoading">加载中...</div>
        <div class="dialog-body" v-else-if="detailInfo">
          <table class="info-table">
            <tbody>
              <tr><td class="info-label">Client ID</td><td class="info-value"><code>{{ detailInfo.clientId }}</code></td></tr>
              <tr><td class="info-label">用户名</td><td class="info-value">{{ detailInfo.username }}</td></tr>
              <tr><td class="info-label">IP 地址</td><td class="info-value">{{ detailInfo.ipAddress }}:{{ detailInfo.port }}</td></tr>
              <tr><td class="info-label">连接时间</td><td class="info-value">{{ formatTimestamp(detailInfo.connectedAt) }}</td></tr>
              <tr><td class="info-label">协议</td><td class="info-value">{{ detailInfo.protoName }} v{{ detailInfo.protoVer }}</td></tr>
              <tr><td class="info-label">设备名称</td><td class="info-value">{{ detailInfo.deviceName || '-' }}</td></tr>
              <tr><td class="info-label">设备编号</td><td class="info-value">{{ detailInfo.deviceCode || '-' }}</td></tr>
              <tr><td class="info-label">隐患点</td><td class="info-value">{{ detailInfo.hazardPointName || '-' }}</td></tr>
              <tr><td class="info-label">最后认证IP</td><td class="info-value">{{ detailInfo.lastAuthIp }}</td></tr>
              <tr><td class="info-label">最后认证时间</td><td class="info-value">{{ detailInfo.lastAuthTime }}</td></tr>
              <tr><td class="info-label">订阅主题</td>
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
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { getAuthInfo } from '@/utils/userApi'
import {
  getMqttStats,
  getMqttListeners,
  getMqttConfig,
  getMqttClients,
  getMqttClientDetail,
  kickMqttClient,
  kickMqttClients,
  type MqttClientItem,
  type MqttStats,
  type MqttConfig,
  type MqttListener,
  type MqttSubscription
} from '@/api/monitor'

// ===== 权限 =====
const isAdmin = ref(false)

onMounted(async () => {
  try {
    const auth = await getAuthInfo()
    isAdmin.value = auth.roles.includes('admin') || auth.roles.includes('ROOT')
  } catch {
    isAdmin.value = false
  }
  if (isAdmin.value) {
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
  } catch { /* ignore */ }
}

const fetchListeners = async () => {
  try {
    const res = await getMqttListeners()
    listeners.value = res.data ?? []
  } catch { /* ignore */ }
}

const fetchConfig = async () => {
  try {
    const res = await getMqttConfig()
    config.value = res.data
  } catch { /* ignore */ }
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

// ===== 数据日志（mock） =====
const logQuery = ref({ clientId: '', topic: '', startTime: '', endTime: '' })
const logPage = ref(1)
const logPageSize = 10
const logTotal = ref(22)

const allLogs = [
  { id: 1,  receiveTime: '2026-06-03 09:15:32', clientId: 'device/gnss/a1-001', username: 'gnss_a1', topic: 'sensor/gnss/displacement', message: '{"x":12.3,"y":5.1,"z":-0.2,"unit":"mm"}', size: '78 B' },
  { id: 2,  receiveTime: '2026-06-03 09:15:28', clientId: 'device/rain/b3-002', username: 'rain_b3', topic: 'sensor/rainfall/current', message: '{"value":0.5,"unit":"mm/h"}', size: '42 B' },
  { id: 3,  receiveTime: '2026-06-03 09:15:15', clientId: 'device/press/c5-003', username: 'press_c5', topic: 'sensor/pressure/level', message: '{"value":2.3,"unit":"m"}', size: '38 B' },
  { id: 4,  receiveTime: '2026-06-03 09:14:58', clientId: 'device/crack/d2-004', username: 'crack_d2', topic: 'sensor/crack/width', message: '{"value":0.08,"unit":"mm"}', size: '40 B' },
  { id: 5,  receiveTime: '2026-06-03 09:14:42', clientId: 'device/water/e7-005', username: 'water_e7', topic: 'sensor/water/level', message: '{"value":4.7,"unit":"m"}', size: '37 B' },
  { id: 6,  receiveTime: '2026-06-03 09:14:10', clientId: 'device/gnss/a2-006', username: 'gnss_a2', topic: 'sensor/gnss/position', message: '{"lat":30.6512,"lng":104.1015}', size: '52 B' },
  { id: 7,  receiveTime: '2026-06-03 09:13:35', clientId: 'device/tilt/f8-007', username: 'tilt_f8', topic: 'sensor/tilt/angle', message: '{"x":0.12,"y":-0.05,"unit":"deg"}', size: '55 B' },
  { id: 8,  receiveTime: '2026-06-03 09:12:50', clientId: 'device/rain/c8-008', username: 'rain_c8', topic: 'sensor/rainfall/daily', message: '{"total":12.8,"unit":"mm"}', size: '45 B' },
  { id: 9,  receiveTime: '2026-06-03 09:12:20', clientId: 'device/gnss/a1-001', username: 'gnss_a1', topic: 'sensor/gnss/displacement', message: '{"x":12.4,"y":5.0,"z":-0.1,"unit":"mm"}', size: '78 B' },
  { id: 10, receiveTime: '2026-06-03 09:11:45', clientId: 'monitor/edge/gw01', username: 'edge_gw1', topic: 'status/gateway/heartbeat', message: '{"cpu":45,"mem":62,"disk":38,"uptime":86400}', size: '68 B' },
  { id: 11, receiveTime: '2026-06-03 09:11:10', clientId: 'device/press/c5-003', username: 'press_c5', topic: 'sensor/pressure/level', message: '{"value":2.4,"unit":"m"}', size: '38 B' },
  { id: 12, receiveTime: '2026-06-03 09:10:33', clientId: 'device/crack/d2-004', username: 'crack_d2', topic: 'cmd/crack/d2', message: '{"action":"calibrate","timestamp":1717402200}', size: '62 B' }
]

const logs = ref(allLogs)

const handleLogSearch = () => {
  let filtered = [...allLogs]
  const q = logQuery.value
  if (q.clientId) filtered = filtered.filter(l => l.clientId.toLowerCase().includes(q.clientId.toLowerCase()))
  if (q.topic) filtered = filtered.filter(l => l.topic.toLowerCase().includes(q.topic.toLowerCase()))
  if (q.startTime) filtered = filtered.filter(l => l.receiveTime >= q.startTime.replace('T', ' '))
  if (q.endTime) filtered = filtered.filter(l => l.receiveTime <= q.endTime.replace('T', ' '))
  logs.value = filtered
  logTotal.value = filtered.length
  logPage.value = 1
}

const handleLogReset = () => {
  logQuery.value = { clientId: '', topic: '', startTime: '', endTime: '' }
  logs.value = allLogs
  logTotal.value = allLogs.length
  logPage.value = 1
}

// ===== 在线客户端 =====
const clients = ref<MqttClientItem[]>([])
const clientPage = ref(1)
const clientPageSize = 20
const clientTotal = ref(0)
const clientsLoading = ref(false)

const fetchClients = async () => {
  clientsLoading.value = true
  try {
    const res = await getMqttClients({ page: clientPage.value, limit: clientPageSize })
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

const isAllSelected = computed(() => {
  return clients.value.length > 0 && selectedClientIds.value.length === clients.value.length
})

const toggleSelectAll = () => {
  if (isAllSelected.value) {
    selectedClientIds.value = []
  } else {
    selectedClientIds.value = clients.value.map(c => c.clientId)
  }
}

const toggleSelect = (clientId: string) => {
  const idx = selectedClientIds.value.indexOf(clientId)
  if (idx > -1) {
    selectedClientIds.value.splice(idx, 1)
  } else {
    selectedClientIds.value.push(clientId)
  }
}

const handleBatchKick = async () => {
  if (selectedClientIds.value.length === 0) return
  try {
    const res = await kickMqttClients(selectedClientIds.value)
    const data = res.data
    alert(`批量踢出完成：成功 ${data?.success ?? selectedClientIds.value.length}，失败 ${data?.fail ?? 0}`)
    selectedClientIds.value = []
    fetchClients()
  } catch {
    alert('批量踢出失败')
  }
}

const handleKickClient = async (client: MqttClientItem) => {
  try {
    await kickMqttClient(client.clientId)
    selectedClientIds.value = selectedClientIds.value.filter(id => id !== client.clientId)
    fetchClients()
  } catch {
    alert('踢出失败，请确认 MQTT HTTP API 已启用且客户端在线')
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
  min-height: 100%;
  background: transparent;
  padding: 20px 0 0 0;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

/* 无权限 */
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

/* 标题 */
.page-title {
  font-size: 18px;
  font-weight: 700;
  color: #1e293b;
  padding: 4px 0 16px 12px;
  border-bottom: 1px solid #e2e8f0;
  margin-bottom: 16px;
}

/* 标签页 */
.tabs-row {
  display: flex;
  gap: 0;
  border-bottom: 2px solid #e2e8f0;
  margin-bottom: 16px;
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

.tab-item:hover { color: #3b82f6; }
.tab-item.active { color: #3b82f6; font-weight: 600; border-bottom-color: #3b82f6; }

.tab-content {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
}

/* MQTT状态表 */
.info-table { width: 100%; border-collapse: collapse; }

.info-table td {
  padding: 12px 24px;
  font-size: 14px;
  color: #334155;
  border-bottom: 1px solid #f1f5f9;
}

.info-table tr:last-child td { border-bottom: none; }

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

.status-dot.running { background: #10b981; box-shadow: 0 0 6px rgba(16, 185, 129, 0.5); animation: dotPulse 2s infinite; }
.status-dot.stopped { background: #ef4444; }

@keyframes dotPulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}

.text-muted { color: #94a3b8; }

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

.listener-remark { font-size: 13px; color: #94a3b8; }

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

.query-item { display: flex; flex-direction: column; gap: 4px; }
.query-item label { font-size: 13px; color: #64748b; font-weight: 500; }

.query-item input[type="text"] {
  width: 180px; height: 32px; padding: 0 10px;
  border: 1px solid #e2e8f0; border-radius: 6px; font-size: 14px; color: #334155;
  background: #fff; outline: none; transition: border-color 0.2s;
}

.query-item input[type="text"]:focus { border-color: #3b82f6; }

.query-item input[type="datetime-local"] {
  width: 175px; height: 32px; padding: 0 8px;
  border: 1px solid #e2e8f0; border-radius: 6px; font-size: 13px; color: #334155;
  background: #fff; outline: none;
}

.query-item input[type="datetime-local"]:focus { border-color: #3b82f6; }

.time-separator { font-size: 13px; color: #94a3b8; flex-shrink: 0; }
.time-inputs { display: flex; align-items: center; gap: 6px; }
.query-item-time .time-inputs input[type="datetime-local"] { width: 175px; }

.query-actions { display: flex; gap: 8px; align-items: flex-end; padding-bottom: 0; }

.btn-query {
  height: 32px; padding: 0 16px; background: #3b82f6; color: #fff;
  border: none; border-radius: 6px; font-size: 14px; cursor: pointer; transition: background 0.2s;
}

.btn-query:hover { background: #2563eb; }

.btn-reset {
  height: 32px; padding: 0 16px; background: #fff; color: #64748b;
  border: 1px solid #e2e8f0; border-radius: 6px; font-size: 14px; cursor: pointer; transition: all 0.2s;
}

.btn-reset:hover { background: #f1f5f9; color: #334155; }

/* 数据表格 */
.data-table { width: 100%; border-collapse: collapse; }

.data-table th {
  padding: 10px 20px; text-align: left; font-size: 14px; color: #64748b;
  font-weight: 600; background: #f8fafc; border-bottom: 2px solid #e2e8f0;
  white-space: nowrap; position: sticky; top: 0; z-index: 1;
}

.data-table td {
  padding: 10px 20px; font-size: 14px; color: #334155;
  border-bottom: 1px solid #f1f5f9; vertical-align: middle;
}

.data-table tbody tr:hover { background: #f8fafc; }

.data-table code {
  font-family: 'SF Mono', 'Fira Code', monospace; font-size: 12px;
  background: #f1f5f9; padding: 2px 6px; border-radius: 4px; color: #6366f1;
  word-break: break-all;
}

.empty-row { text-align: center; color: #94a3b8; padding: 40px 0 !important; }
.log-message-cell { max-width: 260px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 13px; }

/* 客户端工具栏 */
.client-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 20px;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
}

.select-all {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #64748b;
  cursor: pointer;
  user-select: none;
}

.select-all input[type="checkbox"],
.data-table input[type="checkbox"] {
  width: 16px;
  height: 16px;
  cursor: pointer;
  accent-color: #3b82f6;
}

.btn-batch-kick {
  height: 32px;
  padding: 0 16px;
  background: #fff;
  color: #ef4444;
  border: 1px solid #fecaca;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-batch-kick:hover:not(:disabled) {
  background: #fef2f2;
  border-color: #ef4444;
}

.btn-batch-kick:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.row-actions { display: flex; gap: 6px; }

.btn-detail {
  height: 28px; padding: 0 12px; background: #fff; color: #3b82f6;
  border: 1px solid #bfdbfe; border-radius: 6px; font-size: 13px;
  cursor: pointer; transition: all 0.2s; white-space: nowrap;
}

.btn-detail:hover { background: #eff6ff; border-color: #3b82f6; }

.btn-kick {
  height: 28px; padding: 0 12px; background: #fff; color: #ef4444;
  border: 1px solid #fecaca; border-radius: 6px; font-size: 13px;
  cursor: pointer; transition: all 0.2s; white-space: nowrap;
}

.btn-kick:hover { background: #fef2f2; border-color: #ef4444; }

/* 分页 */
.pagination-row {
  display: flex; justify-content: flex-end; align-items: center;
  gap: 12px; padding: 10px 20px; border-top: 1px solid #f1f5f9;
}

.page-info { font-size: 13px; color: #94a3b8; }
.page-btns { display: flex; align-items: center; gap: 8px; }

.page-btns button {
  height: 28px; padding: 0 10px; background: #fff; color: #64748b;
  border: 1px solid #e2e8f0; border-radius: 6px; font-size: 13px;
  cursor: pointer; transition: all 0.2s;
}

.page-btns button:hover:not(:disabled) { background: #f1f5f9; color: #334155; }
.page-btns button:disabled { opacity: 0.4; cursor: not-allowed; }
.page-num { font-size: 13px; color: #64748b; }

/* 详情弹窗 */
.dialog-overlay {
  position: fixed; top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0, 0, 0, 0.4); display: flex;
  align-items: center; justify-content: center; z-index: 2000;
}

.dialog-panel {
  width: 600px; max-height: 70vh; background: #ffffff;
  border-radius: 12px; box-shadow: 0 12px 48px rgba(0, 0, 0, 0.15); overflow: hidden;
}

.dialog-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 16px 20px; background: #f8fafc; border-bottom: 1px solid #e2e8f0;
}

.dialog-title { font-size: 16px; font-weight: 600; color: #1e293b; }

.dialog-close {
  width: 32px; height: 32px; display: flex; align-items: center; justify-content: center;
  background: rgba(0, 0, 0, 0.05); border: none; border-radius: 8px;
  color: #6b7280; cursor: pointer; transition: all 0.2s;
}

.dialog-close:hover { background: rgba(0, 0, 0, 0.1); color: #374151; }
.dialog-body { padding: 20px; overflow-y: auto; }

.topic-list { display: flex; flex-direction: column; gap: 4px; }

.topic-tag-row {
  display: flex; align-items: center; gap: 8px;
}

.qos-tag { font-size: 11px; background: #f1f5f9; color: #64748b; padding: 1px 6px; border-radius: 4px; }
</style>
