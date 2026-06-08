<!-- src/components/CascadeSelector.vue -->
<template>
  <view class="cascade-selector">
    <!-- 显示区域 -->
    <view class="selector-display" @click="showPopup = true">
      <text class="selector-text" :class="{ placeholder: !displayText }">
        {{ displayText || '请选择集装箱和设备' }}
      </text>
      <text class="selector-arrow">▼</text>
    </view>

    <!-- 弹出选择面板 -->
    <view v-if="showPopup" class="selector-popup" @click="showPopup = false">
      <view class="popup-content" @click.stop>
        <view class="popup-header">
          <text class="popup-title">选择设备</text>
          <view class="close-btn" @click="showPopup = false">
            <text>✕</text>
          </view>
        </view>

        <view class="popup-body">
          <!-- 左侧：集装箱列表 -->
          <view class="column-left">
            <scroll-view scroll-y class="list-scroll">
              <view
                v-for="container in containers"
                :key="container.id"
                class="list-item"
                :class="{ active: selectedContainer?.id === container.id }"
                @click="selectContainer(container)"
              >
                <text class="item-text">{{ container.containerName || container.containerNo }}</text>
              </view>
            </scroll-view>
          </view>

          <!-- 右侧：设备列表 -->
          <view class="column-right">
            <scroll-view scroll-y class="list-scroll">
              <view
                v-for="device in devices"
                :key="device.id"
                class="list-item"
                :class="{ active: selectedDevice?.id === device.id }"
                @click="selectDevice(device)"
              >
                <text class="item-text">{{ device.deviceName }}</text>
              </view>
              <view v-if="devices.length === 0 && selectedContainer" class="empty-hint">
                <text>该集装箱暂无设备</text>
              </view>
            </scroll-view>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'

interface Container {
  id: number
  containerNo: string
  containerName?: string
}

interface Device {
  id: number
  deviceName: string
  deviceCode?: string
}

interface Props {
  containers: Container[]
  devices: Device[]
  modelValue?: { container: Container; device: Device } | null
}

interface Emits {
  (e: 'update:modelValue', value: { container: Container; device: Device } | null): void
  (e: 'containerChange', container: Container): void
  (e: 'deviceChange', device: Device): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const showPopup = ref(false)
const selectedContainer = ref<Container | null>(null)
const selectedDevice = ref<Device | null>(null)

const displayText = computed(() => {
  if (selectedContainer.value && selectedDevice.value) {
    const containerName = selectedContainer.value.containerName || selectedContainer.value.containerNo
    return `${containerName} / ${selectedDevice.value.deviceName}`
  }
  return ''
})

watch(() => props.modelValue, (newValue) => {
  if (newValue) {
    selectedContainer.value = newValue.container
    selectedDevice.value = newValue.device
  }
}, { immediate: true })

const selectContainer = (container: Container) => {
  selectedContainer.value = container
  selectedDevice.value = null
  emit('containerChange', container)
}

const selectDevice = (device: Device) => {
  selectedDevice.value = device
  if (selectedContainer.value) {
    emit('update:modelValue', {
      container: selectedContainer.value,
      device: selectedDevice.value
    })
    emit('deviceChange', device)
    showPopup.value = false
  }
}
</script>

<style lang="scss" scoped>
.cascade-selector {
  position: relative;
}

.selector-display {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx;
  background: #ffffff;
  border-radius: 24rpx;
  margin: 24rpx 24rpx 20rpx;
  box-shadow: 0 8rpx 32rpx rgba(102, 126, 234, 0.15);
  border: 1rpx solid rgba(102, 126, 234, 0.08);
}

.selector-text {
  font-size: 30rpx;
  color: #1a1a2e;

  &.placeholder {
    color: #9ca3af;
  }
}

.selector-arrow {
  font-size: 24rpx;
  color: #9ca3af;
}

.selector-popup {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 999;
  display: flex;
  align-items: flex-end;
}

.popup-content {
  width: 100%;
  max-height: 70vh;
  background: #ffffff;
  border-radius: 24rpx 24rpx 0 0;
  display: flex;
  flex-direction: column;
}

.popup-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 32rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.popup-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #1a1a2e;
}

.close-btn {
  width: 48rpx;
  height: 48rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32rpx;
  color: #9ca3af;
}

.popup-body {
  display: flex;
  flex: 1;
  min-height: 400rpx;
}

.column-left,
.column-right {
  flex: 1;
}

.column-left {
  border-right: 1rpx solid #f0f0f0;
}

.list-scroll {
  height: 100%;
}

.list-item {
  padding: 32rpx 24rpx;
  border-bottom: 1rpx solid #f5f5f5;

  &.active {
    background: linear-gradient(135deg, rgba(102, 126, 234, 0.1) 0%, rgba(118, 75, 162, 0.1) 100%);

    .item-text {
      color: #3068e4;
      font-weight: 600;
    }
  }
}

.item-text {
  font-size: 28rpx;
  color: #1a1a2e;
}

.empty-hint {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 80rpx 0;
  font-size: 26rpx;
  color: #9ca3af;
}
</style>
