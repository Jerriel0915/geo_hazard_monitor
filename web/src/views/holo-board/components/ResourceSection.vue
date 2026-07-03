<template>
  <div class="panel-section resource-section">
    <div class="section-header"><span class="section-title">资源统计</span></div>
    <div class="resource-compact">
      <div class="resource-main">
        <div class="resource-total">
          <div class="total-circle">
            <svg class="total-ring" viewBox="0 0 100 100">
              <circle class="ring-bg" cx="50" cy="50" r="45"/>
              <circle class="ring-hazard" cx="50" cy="50" r="45" :stroke-dasharray="hazardDash" stroke-dashoffset="0"/>
              <circle class="ring-device" cx="50" cy="50" r="45" :stroke-dasharray="deviceDash" :stroke-dashoffset="deviceDashOffset"/>
            </svg>
            <div class="total-value">{{ stats.totalResources }}</div>
          </div>
          <div class="total-label">资源总数</div>
        </div>
        <div class="resource-breakdown">
          <div class="breakdown-item hazard">
            <div class="breakdown-icon">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="#faad14" stroke-width="2"
                   stroke-linecap="round" stroke-linejoin="round" width="16" height="16">
                <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"/>
              </svg>
            </div>
            <div class="breakdown-info"><span class="breakdown-value">{{ stats.hazardTotal }}</span><span
                class="breakdown-label">隐患点</span></div>
          </div>
          <div class="breakdown-item device">
            <div class="breakdown-icon">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="#52c41a" stroke-width="2"
                   stroke-linecap="round" stroke-linejoin="round" width="16" height="16">
                <circle cx="12" cy="12" r="3"/>
                <path
                    d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"/>
              </svg>
            </div>
            <div class="breakdown-info"><span class="breakdown-value">{{ stats.deviceTotal }}</span><span
                class="breakdown-label">设备</span></div>
          </div>
        </div>
      </div>
      <div class="device-type-section">
        <div class="type-title">设备分类</div>
        <div class="type-tags">
          <span v-for="type in stats.deviceTypes" :key="type.name" class="type-tag">{{ type.name }}: {{ type.count }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface DeviceTypeStat {
  name: string;
  count: number
}

const props = defineProps<{
  stats: { totalResources: number; hazardTotal: number; deviceTotal: number; deviceTypes: DeviceTypeStat[] }
}>()

const RING_R = 45
const RING_CIRCUMFERENCE = 2 * Math.PI * RING_R

const hazardDash = computed(() => {
  const total = props.stats.totalResources || 1
  const len = (props.stats.hazardTotal / total) * RING_CIRCUMFERENCE
  return `${len} ${RING_CIRCUMFERENCE - len}`
})

const deviceDashOffset = computed(() => {
  const total = props.stats.totalResources || 1
  const hazardLen = (props.stats.hazardTotal / total) * RING_CIRCUMFERENCE
  return -hazardLen
})

const deviceDash = computed(() => {
  const total = props.stats.totalResources || 1
  const len = (props.stats.deviceTotal / total) * RING_CIRCUMFERENCE
  return `${len} ${RING_CIRCUMFERENCE - len}`
})
</script>

<style scoped>
.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
}

.section-title {
  font-size: 17px;
  font-weight: 600;
  color: #1f2937;
}

.resource-compact {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.resource-main {
  display: flex;
  align-items: center;
  gap: 16px;
}

.resource-total {
  flex-shrink: 0;
  text-align: center;
}

.total-circle {
  position: relative;
  width: 80px;
  height: 80px;
}

.total-ring {
  width: 100%;
  height: 100%;
  transform: rotate(-90deg);
}

.ring-bg {
  fill: none;
  stroke: rgba(0, 0, 0, 0.08);
  stroke-width: 10;
}

.ring-hazard {
  fill: none;
  stroke: #faad14;
  stroke-width: 10;
  stroke-linecap: round;
}

.ring-device {
  fill: none;
  stroke: #52c41a;
  stroke-width: 10;
  stroke-linecap: round;
}

.total-value {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 16px;
  font-weight: 700;
  color: #1f2937;
}

.total-label {
  margin-top: 6px;
  font-size: 14px;
  color: #6b7280;
}

.resource-breakdown {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.breakdown-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  background: rgba(59, 130, 246, 0.08);
  border-radius: 6px;
}

.breakdown-icon {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(59, 130, 246, 0.15);
  border-radius: 6px;
}

.breakdown-info {
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.breakdown-value {
  font-size: 16px;
  font-weight: 700;
  color: #1f2937;
}

.breakdown-label {
  font-size: 14px;
  color: #6b7280;
}

.device-type-section {
  border-top: 1px solid rgba(79, 172, 254, 0.2);
  padding-top: 12px;
}

.type-title {
  font-size: 13px;
  color: #4b5563;
  margin-bottom: 6px;
}

.type-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.type-tag {
  display: inline-block;
  padding: 2px 8px;
  font-size: 12px;
  color: #3b82f6;
  background: rgba(59, 130, 246, 0.08);
  border: 1px solid rgba(59, 130, 246, 0.15);
  border-radius: 4px;
  font-weight: 500;
}
</style>
