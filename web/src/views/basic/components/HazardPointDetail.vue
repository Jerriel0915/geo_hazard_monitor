<!-- src/views/basic/components/HazardPointDetail.vue -->
<template>
  <el-dialog
      v-model="dialogVisible"
      title="隐患点详情"
      width="1000px"
      :close-on-click-modal="false"
      destroy-on-close
  >
    <el-tabs v-model="activeTab">
      <el-tab-pane label="基本信息" name="basic">
        <div class="basic-info-container">
          <div class="info-section">
            <h3 class="section-title">隐患点信息</h3>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="隐患点编号">{{ currentRow?.code }}</el-descriptions-item>
              <el-descriptions-item label="隐患点名称">{{ currentRow?.name }}</el-descriptions-item>
              <el-descriptions-item label="分组">{{ currentRow?.groupName || '未分组' }}</el-descriptions-item>
              <el-descriptions-item label="状态">
                <el-tag :type="getStatusType(currentRow?.status || '')" size="small">{{ currentRow?.statusName }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="中心坐标" :span="2">
                {{ currentRow?.longitude?.toFixed(6) }}, {{ currentRow?.latitude?.toFixed(6) }}
              </el-descriptions-item>
              <el-descriptions-item label="走向">{{ currentRow?.strike }}°</el-descriptions-item>
              <el-descriptions-item label="描述" :span="2">{{ currentRow?.description || '-' }}</el-descriptions-item>
            </el-descriptions>
          </div>

          <div class="map-section">
            <h3 class="section-title">隐患点区域展示</h3>
            <MapBoundaryPreview
                v-if="currentRow"
                :initial-value="parsedBoundary"
                :initial-center="previewCenter"
                height="300px"
            />
          </div>

          <div class="system-info-section">
            <h3 class="section-title">系统信息</h3>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="创建人">{{ currentRow?.createBy || '-' }}</el-descriptions-item>
              <el-descriptions-item label="创建时间">{{ currentRow?.createTime || '-' }}</el-descriptions-item>
              <el-descriptions-item label="更新人">{{ currentRow?.updateBy || '-' }}</el-descriptions-item>
              <el-descriptions-item label="更新时间">{{ currentRow?.updateTime || '-' }}</el-descriptions-item>
            </el-descriptions>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="绑定设备" name="devices">
        <div class="table-wrap">
          <div class="table-wrap__scroll">
            <el-table :data="boundDevices" border size="small">
              <el-table-column prop="deviceCode" label="设备编号" width="150" align="center" />
              <el-table-column prop="deviceName" label="设备名称" min-width="150" align="center" />
              <el-table-column prop="sensorNames" label="传感器" min-width="150" align="center">
                <template #default="{ row }">
                  <span v-for="sensor in row.sensors" :key="sensor.id" class="sensor-tag">
                    {{ sensor.name }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column prop="bindTime" label="绑定时间" width="180" align="center" />
              <el-table-column prop="deviceStatus" label="设备状态" width="100" align="center">
                <template #default="{ row }">
                  <el-tag :type="row.deviceStatus === 'NORMAL' ? 'success' : row.deviceStatus === 'FAULT' ? 'danger' : 'warning'" size="small">
                    {{ row.deviceStatus === 'NORMAL' ? '正常' : row.deviceStatus === 'FAULT' ? '故障' : '离线' }}
                  </el-tag>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="告警配置" name="alarmConfig">
        <div class="alarm-config-view">
          <div class="config-section">
            <h3 class="section-title">告警判据</h3>
            <div class="table-wrap">
              <div class="table-wrap__scroll">
                <el-table :data="alarmCriteriaList" border size="small">
                  <el-table-column prop="name" label="判据名称" width="150" align="center" />
                  <el-table-column prop="monitorTypeName" label="监测类型" width="150" align="center" />
                  <el-table-column prop="expression" label="表达式" min-width="250" align="center" />
                  <el-table-column prop="alarmLevel" label="告警等级" width="100" align="center">
                    <template #default="{ row }">
                      <el-tag :type="getAlarmLevelType(row.alarmLevel)" size="small">{{ row.alarmLevelText }}</el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column prop="isEnabled" label="状态" width="80" align="center">
                    <template #default="{ row }">
                      <el-tag :type="row.isEnabled ? 'success' : 'info'" size="small">{{ row.isEnabled ? '启用' : '禁用' }}</el-tag>
                    </template>
                  </el-table-column>
                </el-table>
              </div>
            </div>
          </div>

          <div class="config-section">
            <h3 class="section-title">告警分发</h3>
            <div class="table-wrap">
              <div class="table-wrap__scroll">
                <el-table :data="dispatchRules" border size="small">
                  <el-table-column prop="name" label="规则名称" width="150" align="center" />
                  <el-table-column label="类型" width="100" align="center">
                    <template #default="{ row }">
                      <el-tag :type="row.type === 'ALARM' ? 'warning' : 'info'" size="small">
                        {{ row.type === 'ALARM' ? '告警分发' : '状态通知' }}
                      </el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column prop="alarmLevel" label="告警等级" width="120" align="center">
                    <template #default="{ row }">
                      <span v-if="row.type === 'ALARM'">{{ row.alarmLevel }}</span>
                      <span v-else class="empty-text">-</span>
                    </template>
                  </el-table-column>
                  <el-table-column prop="recipientName" label="接收人" width="120" align="center" />
                  <el-table-column label="通知渠道" min-width="150" align="center">
                    <template #default="{ row }">
                      <span v-for="ch in row.channel.split(',')" :key="ch" class="channel-tag">{{ getChannelLabel(ch) }}</span>
                    </template>
                  </el-table-column>
                  <el-table-column prop="isEnabled" label="状态" width="80" align="center">
                    <template #default="{ row }">
                      <el-tag :type="row.isEnabled ? 'success' : 'info'" size="small">{{ row.isEnabled ? '启用' : '禁用' }}</el-tag>
                    </template>
                  </el-table-column>
                </el-table>
              </div>
            </div>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="监测数据" name="monitorData">
        <MonitorDataExplorer
          v-if="currentRow"
          :hazard-point-id="Number(currentRow.id)"
          :hazard-point-name="currentRow.name"
          :initial-device-id="boundDevices[0]?.deviceId ? Number(boundDevices[0].deviceId) : undefined"
          @device-change="(id: number) => emit('deviceChange', String(id))"
          @sensor-change="(id: number) => emit('sensorChange', String(id))"
        />
      </el-tab-pane>
    </el-tabs>

    <template #footer>
      <el-button @click="dialogVisible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import MapBoundaryPreview from '@/components/map/MapBoundaryPreview.vue'
import MonitorDataExplorer from '@/components/MonitorDataExplorer.vue'
import { deserialize, type LatLng } from '@/lib/boundaryCoords'
import { getAlarmLevelType, getChannelLabel, getStatusType, type HazardPointItem } from '../composables/useHazardPointCrud'
import type { BoundDevice } from '../composables/useHazardPointDeviceBind'
import type { AlarmCriteria, DispatchRule } from '../composables/useHazardPointAlarm'

interface Props {
  visible: boolean
  hazardPoint: HazardPointItem | null
  boundDevices: BoundDevice[]
  alarmCriteriaList: AlarmCriteria[]
  dispatchRules: DispatchRule[]
}

interface Emits {
  (e: 'update:visible', value: boolean): void
  (e: 'deviceChange', deviceId: string): void
  (e: 'sensorChange', sensorId: string): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const dialogVisible = ref(false)
const activeTab = ref('basic')

const currentRow = computed(() => props.hazardPoint)

const parsedBoundary = computed(() => {
  if (!currentRow.value) return null
  return deserialize((currentRow.value as any).boundaryCoords)
})

const previewCenter = computed<LatLng | null>(() => {
  const r = currentRow.value
  if (!r || r.latitude == null || r.longitude == null) return null
  return { lat: r.latitude, lng: r.longitude }
})

watch(() => props.visible, (val) => {
  dialogVisible.value = val
  if (val && props.hazardPoint) {
    activeTab.value = 'basic'
  }
})

watch(() => dialogVisible.value, (val) => {
  emit('update:visible', val)
})
</script>

<style scoped>
/* 样式从原文件复制过来 */
.basic-info-container {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.info-section,
.map-section,
.system-info-section {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 16px;
}

.section-title {
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 8px;
  display: block;
  font-size: 13px;
}

.info-section .section-title,
.map-section .section-title,
.system-info-section .section-title {
  font-size: 13px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #e8e8e8;
}

.sensor-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-right: 6px;
  margin-bottom: 4px;
  padding: 2px 8px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 4px;
  font-size: 11px;
  color: #475569;
}

.channel-tag {
  display: inline-block;
  padding: 2px 8px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 4px;
  font-size: 11px;
  margin-right: 4px;
  color: #475569;
}

.alarm-config-view {
  padding: 8px;
}

.config-section {
  margin-bottom: 24px;
}

.config-section .section-title {
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 14px;
  padding-bottom: 10px;
  border-bottom: 2px solid #1890ff;
}

.empty-text {
  color: #909399;
}

.table-wrap {
  flex: 1;
  width: 100%;
  min-height: 0;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.table-wrap__scroll {
  flex: 1;
  min-height: 0;
  overflow: auto;
}
</style>