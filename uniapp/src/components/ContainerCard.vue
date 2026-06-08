<!-- src/components/ContainerCard.vue -->
<template>
  <view class="container-card" @click="handleClick">
    <view class="card-header">
      <view class="container-info">
        <text class="container-name">{{ container.containerName || container.containerNo || '未命名' }}</text>
        <text v-if="container.containerName" class="container-no">{{ container.containerNo }}</text>
      </view>
      <view class="status-tag" :class="statusClass">
        <view class="status-dot"></view>
        <text>{{ statusText }}</text>
      </view>
    </view>

    <view class="card-data">
      <view v-for="item in container.realtime" :key="`${item.property}-${item.deviceId}`" class="data-item">
        <zui-svg-icon :icon="getIcon(item.property)" width="32rpx" />
        <view class="data-value-row">
          <text class="data-value" :class="getValueClass(item)">{{ formatValue(item.value) }}</text>
          <text v-if="item.unit" class="data-unit">{{ item.unit }}</text>
        </view>
        <text class="data-label">{{ item.displayName }}</text>
        <view v-if="item.hasAlarm" class="alarm-badge">!</view>
      </view>
      <view v-if="!container.realtime || container.realtime.length === 0" class="data-empty">
        <text class="empty-text">暂无数据</text>
      </view>
    </view>

    <view class="card-footer">
      <text class="location">{{ container.location || '未知位置' }}</text>
      <text class="update-time">更新于 {{ formatTime(container.updateTime) }}</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, defineOptions } from 'vue'

// 显式设置组件名
defineOptions({
  name: 'ContainerCard'
})

interface RealtimeData {
  property: string
  displayName: string
  value: string
  unit: string
  deviceId: number
  hasAlarm?: boolean
}

interface ContainerData {
  id: number
  containerNo: string
  containerName?: string
  status: number
  realtime?: RealtimeData[]
  location?: string
  updateTime?: string
}

interface Props {
  container: ContainerData
}

const props = defineProps<Props>()
const emit = defineEmits<{
  click: [container: ContainerData]
}>()

const statusClass = computed(() => {
  return props.container.status === 1 ? 'success' : 'inactive'
})

const statusText = computed(() => {
  return props.container.status === 1 ? '运行中' : '已停用'
})

const getIcon = (property: string): string => {
  const iconMap: Record<string, string> = {
    temperature: 'temperature',
    humidity: 'humidity',
    smoke: 'smoke',
    smoke_level: 'smoke',
    door_status: 'door',
    latitude: 'location',
    longitude: 'location',
    battery: 'battery',
    chart_bar: 'chart-bar'
  }
  return iconMap[property] || 'chart-bar'
}

const formatValue = (val: string) => {
  if (!val) return '-'
  // 尝试格式化为小数
  const num = parseFloat(val)
  if (!isNaN(num)) {
    return num.toFixed(1)
  }
  return val
}

const getValueClass = (item: RealtimeData) => {
  // 如果后端返回了hasAlarm字段，优先使用
  if (item.hasAlarm === true) {
    return 'danger' // 有激活告警，显示异常状态
  } else if (item.hasAlarm === false) {
    return 'normal' // 无激活告警，显示正常状态
  }
  return '' // 默认状态
}

const formatTime = (time?: string) => {
  if (!time) return '-'
  // iOS 兼容：将 "2026-03-08 19:35:37" 格式转换为支持的格式
  // 替换空格为 "T" 或转换为 "yyyy/MM/dd HH:mm:ss" 格式
  const iosTime = time.replace(' ', 'T').replace(/\.\d+Z$/, '')
  const date = new Date(iosTime)
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${hours}:${minutes}`
}

const handleClick = () => {
  emit('click', props.container)
}
</script>

<style lang="scss" scoped>
.container-card {
  background: linear-gradient(135deg, #ffffff 0%, #f8fafc 100%);
  border-radius: 24rpx;
  padding: 24rpx;
  margin: 5rpx 32rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 8rpx 32rpx rgba(102, 126, 234, 0.15);
  border: 1rpx solid rgba(102, 126, 234, 0.1);
  transition: transform 0.2s ease, box-shadow 0.2s ease;

  &:active {
    transform: scale(0.98);
    box-shadow: 0 4rpx 16rpx rgba(102, 126, 234, 0.2);
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20rpx;
}

.container-info {
  display: flex;
  flex-direction: column;
  gap: 4rpx;
}

.container-name {
  font-size: 32rpx;
  font-weight: 600;
  color: #1a1a2e;
}

.container-no {
  font-size: 24rpx;
  color: #9ca3af;
}

.status-tag {
  display: flex;
  align-items: center;
  gap: 8rpx;
  padding: 8rpx 16rpx;
  border-radius: 20rpx;
  font-size: 22rpx;

  &.success {
    background: linear-gradient(135deg, rgba(82, 196, 26, 0.15) 0%, rgba(82, 196, 26, 0.05) 100%);

    .status-dot {
      background-color: #52c41a;
      box-shadow: 0 0 8rpx rgba(82, 196, 26, 0.5);
    }

    text {
      color: #52c41a;
    }
  }

  &.inactive {
    background-color: rgba(156, 163, 175, 0.15);

    .status-dot {
      background-color: #9ca3af;
    }

    text {
      color: #9ca3af;
    }
  }
}

.status-dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.card-data {
  display: flex;
  justify-content: space-around;
  padding: 20rpx 0;
  background: linear-gradient(90deg, rgba(102, 126, 234, 0.03) 0%, rgba(118, 75, 162, 0.03) 100%);
  border-radius: 16rpx;
  margin: 0 -8rpx;
}

.data-item {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6rpx;
  padding: 0 16rpx;
}

.alarm-badge {
  position: absolute;
  top: -4rpx;
  right: 8rpx;
  width: 28rpx;
  height: 28rpx;
  background: rgba(245, 34, 45, 0.9);
  color: #ffffff;
  font-size: 18rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
  font-weight: bold;
}

.data-value-row {
  display: flex;
  align-items: baseline;
  gap: 2rpx;
}

.data-value {
  font-size: 32rpx;
  font-weight: 700;
  color: #1a1a2e;

  &.normal {
    color: #52c41a;
  }

  &.warning {
    color: #faad14;
  }

  &.danger {
    color: #f5222d;
  }
}

.data-unit {
  font-size: 20rpx;
  color: #6b7280;
  font-weight: 500;
}

.data-label {
  font-size: 22rpx;
  color: #6b7280;
}

.data-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40rpx 0;
  width: 100%;
}

.empty-text {
  font-size: 24rpx;
  color: #9ca3af;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 16rpx;
  padding-top: 16rpx;
  border-top: 1rpx dashed rgba(102, 126, 234, 0.15);
}

.location {
  font-size: 24rpx;
  color: #3068e4;
  font-weight: 500;
}

.update-time {
  font-size: 22rpx;
  color: #9ca3af;
}
</style>
