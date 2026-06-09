<template>
  <div class="hazard-detail-card">
    <!-- 卡片标题 -->
    <div class="section-header">
      <span class="section-title-group">
        <el-icon class="section-icon" :size="18"><MapLocation/></el-icon>
        <span class="section-title">{{ hazardPoint?.name || '--' }}</span>
      </span>
    </div>

    <!-- 基本信息 -->
    <div class="card-info">
      <div class="info-row">
        <span class="info-label">编号</span>
        <span class="info-value">{{ hazardPoint?.code || '--' }}</span>
      </div>
      <div class="info-row">
        <span class="info-label">坐标</span>
        <span class="info-value">{{ hazardPoint ? `${hazardPoint.latitude.toFixed(4)}, ${hazardPoint.longitude.toFixed(4)}` : '--' }}</span>
      </div>
      <div class="info-row">
        <span class="info-label">分组</span>
        <span class="info-value">{{ hazardPoint?.groupName || '--' }}</span>
      </div>
      <div v-if="hazardPoint?.description" class="info-row">
        <span class="info-label">备注</span>
        <span class="info-value info-desc">{{ hazardPoint.description }}</span>
      </div>
    </div>

    <!-- 设备列表 -->
    <div class="device-section">
      <div class="device-section-header">
        <span class="section-title-group">
          <el-icon class="section-icon section-icon--green" :size="16"><Monitor/></el-icon>
          <span class="device-section-title">设备列表</span>
        </span>
        <span class="device-section-count">{{ devices.length }}</span>
      </div>
      <div v-if="loading" class="device-loading">加载中...</div>
      <div v-else-if="devices.length === 0" class="device-empty">暂无设备</div>
      <div v-else class="device-scroll">
        <div
          v-for="device in devices"
          :key="device.id"
          class="device-capsule"
          :class="{ selected: selectedDeviceId === device.id }"
          @click="onDeviceClick(device)"
        >
          <div class="capsule-main">
            <div class="capsule-info">
              <div class="capsule-name-row">
                <span class="capsule-name">{{ device.name }}</span>
                <span class="capsule-status" :class="device.onlineStatus">
                  <span class="capsule-status-dot"></span>
                  {{ device.onlineStatus === 'online' ? '在线' : '离线' }}
                </span>
              </div>
              <div class="capsule-meta">
                <span class="capsule-code">{{ device.code || '--' }}</span>
                <span class="capsule-state-tag">{{ device.deviceState || '--' }}</span>
              </div>
            </div>
          </div>
          <!-- 传感器列表 -->
          <div v-if="device.sensors && device.sensors.length" class="capsule-sensors">
            <div v-for="sensor in device.sensors" :key="sensor.id || sensor.sensorCode" class="sensor-chip">
              <el-icon :size="12">
                <component :is="getSensorIcon(sensor.monitorTypeCode || sensor.name)" />
              </el-icon>
              <span class="sensor-chip-name">{{ sensor.monitorTypeName || sensor.name }}</span>
            </div>
          </div>
          <!-- 绑定时间 -->
          <div v-if="device.bindTime" class="capsule-footer">
            绑定时间: {{ formatTime(device.bindTime) }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { Drizzling, MapLocation, Monitor, Odometer, Sunny } from '@element-plus/icons-vue'
import { getBoundDevices } from '@/api/hazardPoint'

const props = defineProps<{
  hazardPoint: any
}>()

const emit = defineEmits<{
  (e: 'deviceClick', device: any): void
}>()

const loading = ref(false)
const devices = ref<any[]>([])
const selectedDeviceId = ref<number | null>(null)

const onDeviceClick = (device: any) => {
  selectedDeviceId.value = selectedDeviceId.value === device.id ? null : device.id
  emit('deviceClick', device)
}
const getSensorIcon = (code: string) => {
  const c = code?.toUpperCase() || ''
  if (c.includes('GNSS')) return Monitor
  if (c.includes('RAIN') || c.includes('雨')) return Sunny
  if (c.includes('PRESSURE') || c.includes('渗压') || c.includes('压力')) return Drizzling
  return Odometer
}

const formatTime = (time: string) => {
  if (!time) return '--'
  return time.replace(/:\d{2}$/, '')
}

const fetchDevices = async () => {
  if (!props.hazardPoint?.id) {
    devices.value = []
    return
  }
  loading.value = true
  try {
    const response = await getBoundDevices(String(props.hazardPoint.id))
    if (response.code === 200 && response.data) {
      devices.value = (response.data as any[]).map((item: any) => ({
        id: item.deviceId,
        name: item.deviceName || '未知设备',
        code: item.deviceCode || '',
        onlineStatus: item.deviceStatus === 0 ? 'online' : 'offline',
        deviceState: item.deviceState || item.state || '',
        bindTime: item.bindTime || item.createTime || '',
        sensors: (item.sensors || []).map((s: any) => ({
          id: s.id,
          sensorCode: s.sensorCode || '',
          name: s.name || s.sensorName || '',
          monitorTypeCode: s.monitorTypeCode || s.monitorContentCode || '',
          monitorTypeName: s.monitorTypeName || s.monitorContentName || s.name || ''
        })),
        longitude: item.installLongitude || props.hazardPoint.longitude,
        latitude: item.installLatitude || props.hazardPoint.latitude
      }))
    }
  } catch {
    devices.value = []
  } finally {
    loading.value = false
  }
}

watch(() => props.hazardPoint?.id, fetchDevices, { immediate: true })
</script>

<style scoped>
/* ===== 设计 Token ===== */
/* 色阶: 主文字 #1d2129 / 次文字 #4e5969 / 辅助文字 #6b7785 / 占位 #86909c */

.hazard-detail-card {
  background: #ffffff;
  border: 1px solid #e5e6eb;
  border-radius: 12px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08), 0 4px 12px rgba(0, 0, 0, 0.04);
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

/* 卡片标题 */
.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 14px 10px;
  background: linear-gradient(135deg, rgba(24, 144, 255, 0.12) 0%, rgba(24, 144, 255, 0.03) 100%);
  border-bottom: 1px solid rgba(24, 144, 255, 0.12);
  flex-shrink: 0;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #1d2129;
  font-family: var(--font-display, inherit);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.section-title-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.section-icon {
  color: #1890ff;
}

/* 基本信息 */
.card-info {
  padding: 14px 18px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  border-bottom: 1px solid #e5e6eb;
  flex-shrink: 0;
}

.info-row {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.info-label {
  font-size: 13px;
  color: #6b7785;
  white-space: nowrap;
  min-width: 32px;
  font-weight: 500;
}

.info-value {
  font-size: 14px;
  color: #1d2129;
  word-break: break-all;
  line-height: 1.5;
}

.info-desc {
  color: #4e5969;
  font-size: 13px;
  line-height: 1.5;
}

/* 设备区 */
.device-section {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
  padding: 0 14px 14px;
}

.device-section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 4px 8px;
  flex-shrink: 0;
}

.section-icon--green {
  color: #52c41a;
}

.device-section-title {
  font-size: 14px;
  font-weight: 600;
  color: #1d2129;
}

.device-section-count {
  font-size: 13px;
  color: #6b7785;
  font-weight: 500;
}

.device-loading,
.device-empty {
  text-align: center;
  padding: 32px 0;
  font-size: 14px;
  color: #6b7785;
}

.device-scroll {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-right: 4px;
}

.device-scroll::-webkit-scrollbar { width: 4px; }
.device-scroll::-webkit-scrollbar-thumb { background: rgba(0, 0, 0, 0.12); border-radius: 2px; }

/* 设备卡片 */
.device-capsule {
  display: flex;
  flex-direction: column;
  padding: 12px 14px;
  background: #f7f8fa;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid #e5e6eb;
}

.device-capsule:hover {
  background: #e8f4ff;
  border-color: #91caff;
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.1);
}

.device-capsule.selected {
  background: #e8f4ff;
  border-color: #1677ff;
  box-shadow: 0 2px 8px rgba(22, 119, 255, 0.15);
}

.capsule-main {
  display: flex;
  align-items: flex-start;
}

.capsule-info {
  flex: 1;
  min-width: 0;
}

.capsule-name-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.capsule-name {
  font-size: 14px;
  font-weight: 600;
  color: #1d2129;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
  min-width: 0;
  line-height: 1.5;
}

.capsule-meta {
  display: flex;
  gap: 8px;
  margin-top: 4px;
  align-items: center;
  flex-wrap: wrap;
}

.capsule-code {
  font-size: 12px;
  color: #6b7785;
  font-family: 'SFMono-Regular', Consolas, monospace;
}

.capsule-state-tag {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
  background: #f0f1f3;
  color: #4e5969;
  font-weight: 500;
}

/* 在线状态 */
.capsule-status {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
  flex-shrink: 0;
  font-weight: 500;
}

.capsule-status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

.capsule-status.online { background: #f0ffe6; color: #237804; }
.capsule-status.online .capsule-status-dot { background: #52c41a; }
.capsule-status.offline { background: #f5f5f5; color: #86909c; }
.capsule-status.offline .capsule-status-dot { background: #c9cdd4; }

/* 绑定时间 */
.capsule-footer {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid #e5e6eb;
  font-size: 12px;
  color: #6b7785;
}

/* 传感器芯片 */
.capsule-sensors {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid #e5e6eb;
}

.sensor-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 10px;
  background: #ffffff;
  border: 1px solid #e5e6eb;
  border-radius: 6px;
  font-size: 12px;
  color: #4e5969;
  transition: border-color 0.2s;
}

.sensor-chip:hover {
  border-color: #91caff;
  color: #1677ff;
}

.sensor-chip-name {
  white-space: nowrap;
}
</style>
