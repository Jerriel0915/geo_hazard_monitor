<template>
  <div class="panel-section resource-section">
    <div class="section-header"><span class="section-title">资源统计</span></div>
    <div class="resource-compact">
      <div class="resource-main">
        <div class="resource-total">
          <div class="total-circle">
            <svg class="total-ring" viewBox="0 0 100 100">
              <circle class="ring-bg" cx="50" cy="50" r="45"/>
              <circle class="ring-hazard" cx="50" cy="50" r="45" stroke-dasharray="113 170" stroke-dashoffset="0"/>
              <circle class="ring-device" cx="50" cy="50" r="45" stroke-dasharray="142 141" stroke-dashoffset="-113"/>
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
        <div class="type-bars">
          <div v-for="type in stats.deviceTypes" :key="type.name" class="type-bar-row">
            <span class="type-name">{{ type.name }}</span><span class="type-count">{{ type.count }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
interface DeviceTypeStat {
  name: string;
  count: number
}

defineProps<{
  stats: { totalResources: number; hazardTotal: number; deviceTotal: number; deviceTypes: DeviceTypeStat[] }
}>()
</script>
