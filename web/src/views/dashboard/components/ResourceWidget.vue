<template>
  <div class="panel-section resource-section">
    <div class="section-header">
      <span class="section-title-group">
        <el-icon class="section-icon" :size="18"><Box/></el-icon>
        <span class="section-title">资产情况</span>
      </span>
    </div>
    <div class="resource-compact">
      <div class="resource-main">
        <div class="resource-total">
          <div class="total-circle">
            <svg class="total-ring" viewBox="0 0 80 80">
              <circle class="ring-bg" cx="40" cy="40" r="35"/>
              <circle class="ring-hazard" cx="40" cy="40" r="35" :stroke-dasharray="`113 170`"
                      stroke-dashoffset="0"/>
              <circle class="ring-device" cx="40" cy="40" r="35" :stroke-dasharray="`142 141`"
                      stroke-dashoffset="-113"/>
            </svg>
            <div class="total-value">{{ resourceStats.totalResources }}</div>
          </div>
          <div class="total-label">资源总数</div>
        </div>
        <div class="resource-breakdown">
          <div class="breakdown-item hazard">
            <div class="breakdown-icon">
              <el-icon :size="14" color="#faad14"><Location/></el-icon>
            </div>
            <div class="breakdown-info">
              <span class="breakdown-value">{{ resourceStats.hazardTotal }}</span>
              <span class="breakdown-label">隐患点</span>
            </div>
          </div>
          <div class="breakdown-item device">
            <div class="breakdown-icon">
              <el-icon :size="14" color="#52c41a"><Setting/></el-icon>
            </div>
            <div class="breakdown-info">
              <span class="breakdown-value">{{ resourceStats.deviceTotal }}</span>
              <span class="breakdown-label">设备</span>
            </div>
          </div>
        </div>
      </div>
      <div class="device-type-section">
        <div class="type-title">设备分类</div>
        <div class="type-bars">
          <div v-for="type in resourceStats.deviceTypes" :key="type.name" class="type-bar-row">
            <span class="type-name">{{ type.name }}</span>
            <span class="type-count">{{ type.count }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Box, Location, Setting } from '@element-plus/icons-vue'
defineProps<{
  resourceStats: {
    totalResources: number
    deviceTotal: number
    hazardTotal: number
    deviceTypes: { name: string; count: number }[]
  }
}>()
</script>

<style scoped>
.panel-section {
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(16px);
  border: 1px solid rgba(24, 144, 255, 0.08);
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.06);
  padding: 16px 18px;
  margin-bottom: 12px;
  transition: box-shadow 0.2s, border-color 0.2s;
}

.panel-section:hover {
  border-color: rgba(24, 144, 255, 0.15);
  box-shadow: 0 4px 16px rgba(24, 144, 255, 0.08);
}

.panel-section:last-child {
  margin-bottom: 0;
}

.section-header {
  padding-left: 10px;
  border-left: 3px solid #52c41a;
  margin-bottom: 14px;
}

.section-title {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}

.section-title-group {
  display: flex;
  align-items: center;
  gap: 6px;
}

.section-icon {
  color: #52c41a;
}

.resource-compact {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.resource-main {
  display: flex;
  align-items: center;
  gap: 12px;
}

.resource-total {
  flex-shrink: 0;
  text-align: center;
}

.total-circle {
  position: relative;
  width: 70px;
  height: 70px;
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
  color: #303133;
}

.total-label {
  margin-top: 4px;
  font-size: 10px;
  color: #909399;
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
  width: 26px;
  height: 26px;
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
  font-size: 15px;
  font-weight: 700;
  color: #303133;
}

.breakdown-label {
  font-size: 10px;
  color: #909399;
}

.device-type-section {
  border-top: 1px solid rgba(79, 172, 254, 0.2);
  padding-top: 12px;
}

.type-title {
  font-size: 11px;
  color: #606266;
  margin-bottom: 8px;
}

.type-bars {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.type-bar-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 0;
}

.type-name {
  font-size: 11px;
  color: #909399;
}

.type-count {
  font-size: 11px;
  color: #1890ff;
  font-weight: 600;
}
</style>
