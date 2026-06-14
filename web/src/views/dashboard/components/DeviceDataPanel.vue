<template>
  <div class="device-data-panel" :style="{ left: leftOffset + 'px', right: rightOffset + 'px' }">
    <div class="panel-inner">
      <div class="panel-header">
        <div class="panel-title">
          <el-icon class="device-icon" :size="18"><DataAnalysis/></el-icon>
          <span class="title-name">{{ device?.name || '设备' }}</span>
          <span class="title-code">{{ device?.code || '' }}</span>
        </div>
        <div class="header-actions">
          <button class="close-btn" @click="$emit('close')">
            <el-icon :size="14"><Close/></el-icon>
          </button>
        </div>
      </div>

      <div class="panel-body">
        <MonitorDataExplorer
          v-if="device && hazardPointId"
          :hazard-point-id="hazardPointId"
          :hazard-point-name="hazardPointName"
          :show-device="false"
          :initial-device-id="device.id"
        />
        <div v-else class="empty-state">
          <el-icon :size="36" color="#c9cdd4"><DataAnalysis/></el-icon>
          <span class="empty-text">无法加载监测数据</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {Close, DataAnalysis} from '@element-plus/icons-vue'
import MonitorDataExplorer from '@/components/MonitorDataExplorer.vue'

const props = defineProps<{
  device: any
  hazardPointId?: number
  hazardPointName?: string
  leftOffset: number
  rightOffset: number
}>()

const emit = defineEmits<{
  (e: 'close'): void
}>()
</script>

<style scoped>
.device-data-panel {
  position: absolute;
  bottom: 12px;
  z-index: 1100;
}

.panel-inner {
  width: 100%;
  background: #ffffff;
  border-radius: 10px;
  box-shadow: 0 -2px 16px rgba(0, 0, 0, 0.1);
  border: 1px solid #e5e6eb;
  display: flex;
  flex-direction: column;
  height: 340px;
  overflow: hidden;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 16px;
  background: #f7f8fa;
  border-bottom: 1px solid #e5e6eb;
  flex-shrink: 0;
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.device-icon { color: #1677ff; }

.title-name {
  font-size: 14px;
  font-weight: 600;
  color: #1d2129;
}

.title-code {
  font-size: 12px;
  color: #6b7785;
  font-family: 'SFMono-Regular', Consolas, monospace;
  background: #f0f1f3;
  padding: 1px 8px;
  border-radius: 4px;
}

.close-btn {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: 1px solid #e5e6eb;
  border-radius: 6px;
  color: #6b7785;
  cursor: pointer;
  transition: all 0.2s;
}

.close-btn:hover {
  background: #f0f1f3;
  color: #1d2129;
  border-color: #c9cdd4;
}

.panel-body {
  display: flex;
  flex-direction: column;
  flex: 1;
  overflow: auto;
  padding: 8px 16px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex: 1;
  gap: 8px;
}

.empty-text {
  font-size: 13px;
  color: #86909c;
}
</style>
