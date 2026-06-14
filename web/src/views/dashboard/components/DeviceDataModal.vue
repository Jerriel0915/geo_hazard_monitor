<template>
  <div class="device-data-modal" @click="$emit('close')">
    <div class="modal-container" @click.stop>
      <div class="modal-header">
        <div class="modal-title">
          <el-icon class="device-icon">
            <DataAnalysis/>
          </el-icon>
          <div class="modal-breadcrumb">
            <span class="crumb crumb-clickable" @click="$emit('backToSystemView')" title="返回系统总览">
              {{ hazardPointName }}
            </span>
            <span class="crumb-sep">/</span>
            <span class="crumb crumb-clickable" @click="$emit('close')" title="返回设备列表">
              {{ device?.name || '设备' }}
            </span>
          </div>
        </div>
        <button class="modal-close-btn" @click="$emit('close')">
          <el-icon :size="16"><Close/></el-icon>
        </button>
      </div>

      <div class="modal-body" style="padding: 16px">
        <MonitorDataExplorer
          v-if="device && hazardPointId"
          :hazard-point-id="hazardPointId"
          :hazard-point-name="hazardPointName"
          :show-device="false"
          :initial-device-id="device.id"
        />
        <div v-else class="empty-state">无法加载监测数据</div>
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
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'backToSystemView'): void
}>()
</script>

<style scoped>
.device-data-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
}

.modal-container {
  background: white;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  width: 95%;
  max-width: 1200px;
  height: 85vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  border-bottom: 1px solid #f0f0f0;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  flex-shrink: 0;
}

.modal-title {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.device-icon {
  font-size: 24px;
}

.modal-breadcrumb {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 16px;
  font-weight: 500;
}

.crumb {
  padding: 2px 8px;
  border-radius: 4px;
  transition: background 0.15s;
}

.crumb-clickable {
  cursor: pointer;
  color: #909399;
}

.crumb-clickable:hover {
  background: rgba(64, 158, 255, 0.1);
  color: #1890ff;
}

.modal-close-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.06);
  border: none;
  border-radius: 8px;
  color: #909399;
  cursor: pointer;
  font-size: 18px;
  transition: all 0.2s;
}

.modal-close-btn:hover {
  background: rgba(0, 0, 0, 0.12);
  color: #303133;
}

.modal-body {
  flex: 1;
  overflow: auto;
}

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #909399;
  font-size: 14px;
}
</style>
