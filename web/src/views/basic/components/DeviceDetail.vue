<!-- 设备管理下的查看页面  -->
<template>
  <el-dialog
      v-model="dialogVisible"
      :title="`设备详情 — ${currentRow?.name || ''}`"
      width="960px"
      :close-on-click-modal="false"
      destroy-on-close
  >
    <el-tabs v-model="activeTab">
      <el-tab-pane label="设备详情" name="info">
        <!-- 原有设备详情内容保持不变 -->
        <el-descriptions :column="2" border>
          <el-descriptions-item label="设备编号">{{ currentRow?.code }}</el-descriptions-item>
          <el-descriptions-item label="设备名称">{{ currentRow?.name }}</el-descriptions-item>
          <el-descriptions-item label="设备SN">{{ currentRow?.sn || '-' }}</el-descriptions-item>
          <el-descriptions-item label="接入协议">{{ currentRow?.protocolType || '-' }}</el-descriptions-item>
          <el-descriptions-item label="注册来源">{{ currentRow?.registerSource || '-' }}</el-descriptions-item>
          <el-descriptions-item label="接入账号">{{ currentRow?.authUsername || '-' }}</el-descriptions-item>
          <el-descriptions-item label="接入密码">
            <template v-if="currentRow?.authPassword">
              <span class="pwd-masked">{{ pwdVisible ? currentRow.authPassword : '••••••••' }}</span>
              <el-button size="small" text type="primary" @click="pwdVisible = !pwdVisible">
                {{ pwdVisible ? '隐藏' : '查看' }}
              </el-button>
              <el-button size="small" text type="primary" @click="copyPwd(currentRow.authPassword)">复制</el-button>
            </template>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="安装位置">
            {{ formatCoord(currentRow?.longitude, currentRow?.latitude) }}
            <el-button v-if="currentRow?.longitude != null" size="small" text type="primary" @click="emit('viewOnMap', currentRow)">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                   stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="14" height="14">
                <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/>
                <circle cx="12" cy="10" r="3"/>
              </svg>
              查看
            </el-button>
          </el-descriptions-item>
          <el-descriptions-item label="设备状态">
            <el-tag :type="getStatusType(currentRow?.status || 0)" size="small">{{ currentRow?.statusName }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="在线状态">
            <el-tag :type="currentRow?.onlineStatus === 1 ? 'success' : 'info'" size="small">
              {{ currentRow?.onlineStatus === 1 ? '在线' : '离线' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="最近上报时间">{{ currentRow?.lastReportTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ currentRow?.createTime || '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">传感器列表</el-divider>
        <el-table :data="sensorList" border size="small" v-loading="sensorLoading">
          <el-table-column label="图标" width="60" align="center">
            <template #default="{ row }">
              <img v-if="getSensorIconPath(row)" :src="getSensorIconPath(row)" class="table-icon" alt="icon"/>
              <span v-else class="empty-text">-</span>
            </template>
          </el-table-column>
          <el-table-column prop="sensorCode" label="传感器编号" width="150" align="center"/>
          <el-table-column prop="sensorName" label="传感器名称" width="150" align="center"/>
          <el-table-column prop="monitorTypeName" label="监测类型" width="120" align="center"/>
          <el-table-column label="埋深(米)" width="90" align="center">
            <template #default="{ row }">
              <span v-if="row.burialDepth != null">{{ row.burialDepth }}</span>
              <span v-else class="empty-text">-</span>
            </template>
          </el-table-column>
          <el-table-column label="属性配置" min-width="250" align="center">
            <template #default="{ row }">
              <div v-for="attr in row.attrList" :key="attr.attrCode" class="attr-item">
                {{ attr.attrName }}: {{ attr.initialValue }}{{ attr.unit }}
              </div>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="运行状态变更" name="online">
        <el-table :data="onlineLogs" border size="small" max-height="360">
          <el-table-column prop="eventTime" label="变更时间" width="170" align="center"/>
          <el-table-column label="变更类型" width="90" align="center">
            <template #default="{row}">
              <el-tag :type="row.eventType==='ONLINE'?'success':'danger'" size="small" effect="plain">
                {{ row.eventType === 'ONLINE' ? '🟢 上线' : '🔴 离线' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态变化" width="160" align="center">
            <template #default="{row}">
              <span class="status-change">
                <el-tag size="small" type="info" effect="plain">{{ row.fromStatus || '离线' }}</el-tag>
                <span class="arrow">→</span>
                <el-tag 
                  size="small" 
                  :type="row.eventType === 'ONLINE' ? 'success' : 'danger'" 
                  effect="plain"
                >
                  {{ row.eventType === 'ONLINE' ? '在线' : '离线' }}
                </el-tag>
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="clientId" label="客户端ID（Client ID）" min-width="160" align="center"/>
          <el-table-column prop="clientIp" label="IP地址（IP）" width="140" align="center"/>
          <el-table-column prop="reason" label="原因" min-width="120" align="center"/>
        </el-table>
        <div v-if="onlineLogs.length === 0" class="mde-empty">
          <span>暂无状态变更记录</span>
        </div>
      </el-tab-pane>

      <el-tab-pane label="维修记录" name="maintenance">
        <el-table :data="maintenanceLogs" border size="small" max-height="400">
          <el-table-column label="操作" width="80">
            <template #default="{row}">
              <el-tag :type="row.newStatus === 1 ? 'success' : row.newStatus === 2 ? 'danger' : 'info'" size="small">
                {{ row.statusText }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态变化" width="110">
            <template #default="{row}">{{ getStatusLabel(row.oldStatus) }}→{{ getStatusLabel(row.newStatus) }}</template>
          </el-table-column>
          <el-table-column prop="operatorName" label="操作人" width="90"/>
          <el-table-column prop="operatorPhone" label="电话" width="120"/>
          <el-table-column prop="operationDate" label="操作日期" width="160"/>
          <el-table-column prop="createTime" label="记录时间" width="160"/>
          <el-table-column prop="description" label="描述" min-width="120"/>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="监测数据" name="monitorData" lazy>
        <MonitorDataExplorer
          :hazard-point-id="currentRow?.boundHazardPointId ?? null"
          :hazard-point-name="currentRow?.name"
          :show-device="false"
          :initial-device-id="currentRow?.id"
        />
      </el-tab-pane>
    </el-tabs>

    <template #footer>
      <el-button @click="dialogVisible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import request from '@/utils/request'
import { getSensorIconPath } from '@/utils/deviceIcon'
import { type DeviceItem } from '../composables/useDeviceCrud'
import MonitorDataExplorer from '@/components/MonitorDataExplorer.vue'

interface Props {
  visible: boolean
  device: DeviceItem | null
}

interface Emits {
  (e: 'update:visible', value: boolean): void
  (e: 'viewOnMap', device: DeviceItem): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const dialogVisible = ref(false)
const activeTab = ref('info')
const pwdVisible = ref(false)
const sensorList = ref<any[]>([])
const onlineLogs = ref<any[]>([])
const maintenanceLogs = ref<any[]>([])
const sensorLoading = ref(false)

const currentRow = ref<DeviceItem | null>(null)

// 监听 visible 变化
watch(() => props.visible, (val) => {
  dialogVisible.value = val
  if (val && props.device) {
    currentRow.value = props.device
    loadSensorList(props.device.id!)
    loadOpsLogs(props.device.id!)
    activeTab.value = 'info'
    pwdVisible.value = false
  }
})

watch(() => dialogVisible.value, (val) => {
  if (!val) {
    activeTab.value = 'info'
    pwdVisible.value = false
    sensorList.value = []
    onlineLogs.value = []
    maintenanceLogs.value = []
  }
  emit('update:visible', val)
})

// 加载传感器列表
const loadSensorList = async (deviceId: number) => {
  sensorLoading.value = true
  try {
    const res = await request.get(`/devices/${deviceId}/sensors`)
    sensorList.value = res.data || []
  } catch {
    sensorList.value = []
  } finally {
    sensorLoading.value = false
  }
}

// 加载运行日志和维修记录
const loadOpsLogs = async (deviceId: number) => {
  try {
    const [online, maint] = await Promise.all([
      request.get(`/devices/${deviceId}/online-logs`),
      request.get(`/devices/${deviceId}/maintenance-logs`)
    ])
    onlineLogs.value = online.data || []
    maintenanceLogs.value = maint.data || []
  } catch {
    onlineLogs.value = []
    maintenanceLogs.value = []
  }
}

// 工具函数
const getStatusType = (status: number) => {
  const map: Record<number, string> = { 1: 'success', 2: 'danger', 3: 'info' }
  return map[status] || 'info'
}

const getStatusLabel = (status: number) => {
  const map: Record<number, string> = { 1: '正常', 2: '维修', 3: '停用' }
  return map[status] || '-'
}

const formatCoord = (lng?: number | null, lat?: number | null) => {
  if (lng == null || lat == null) return '未设置'
  return `${lng.toFixed(6)}, ${lat.toFixed(6)}`
}

const copyPwd = (pwd: string) => {
  navigator.clipboard.writeText(pwd)
}
</script>

<style scoped>
.table-icon {
  width: 28px;
  height: 28px;
  object-fit: contain;
}

.empty-text {
  color: #909399;
}

.pwd-masked {
  font-family: monospace;
  letter-spacing: 2px;
  margin-right: 8px;
}

.attr-item {
  font-size: 13px;
  color: #606266;
  padding: 2px 0;
}

.status-change {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.status-change .arrow {
  color: #c0c4cc;
  font-size: 12px;
}

.mde-empty {
  padding: 20px 0;
  text-align: center;
  color: #94a3b8;
  font-size: 14px;
}
</style>