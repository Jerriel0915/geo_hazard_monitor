<!-- src/pages/chart.vue - 监测数据 -->
<template>
  <view class="page-container">
    <!-- 渐变头部 -->
    <view class="header">
      <view class="header-bg">
        <view class="status-bar" :style="{ height: `${statusBarHeight + 65}px` }"></view>
        <view class="bg-circle bg-circle-1"></view>
        <view class="bg-circle bg-circle-2"></view>
      </view>
      <view class="header-content" :style="{ paddingTop: `${statusBarHeight}px` }">
        <view class="header-top">
          <text class="header-title">监测数据</text>
          <text class="header-subtitle">多设备数据可视化分析</text>
        </view>
      </view>
    </view>

    <!-- 已选设备标签 -->
    <view class="selected-devices">
      <scroll-view scroll-x class="tags-scroll">
        <view class="tags-container">
          <view
            v-for="device in selectedDevices"
            :key="device.id"
            class="device-tag"
          >
            <text class="tag-text">{{ device.deviceName }}</text>
            <text class="tag-close" @click="removeDevice(device.id)">×</text>
          </view>
          <view v-if="selectedDevices.length === 0" class="hint-tag">
            <text class="hint-text">点击右下角按钮添加设备</text>
          </view>
        </view>
      </scroll-view>
    </view>

    <!-- 时间范围选择 -->
    <scroll-view class="content-scroll" scroll-y>
      <view class="time-tabs">
        <view
          v-for="tab in timeTabs"
          :key="tab.value"
          class="time-tab"
          :class="{ active: activeTimeTab === tab.value }"
          @click="changeTimeTab(tab.value)"
        >
          {{ tab.label }}
        </view>
      </view>

      <!-- 图表区域 -->
      <view class="charts-container">
        <view v-if="loading && chartGroups.length === 0" class="loading-wrapper">
          <text>加载中...</text>
        </view>

        <view v-else>
          <view
            v-for="group in chartGroups"
            :key="group.deviceId"
            class="device-chart-group"
          >
            <view class="chart-card">
              <view class="card-header">
                <view class="card-header-left">
                  <text class="group-title">{{ group.deviceName }}</text>
                  <text class="group-sub">{{ group.deviceType }}</text>
                </view>
              </view>
              <view class="chart-container">
                <EchartsComponent
                  v-if="group.option"
                  :onInit="(canvas, width, height) => initChart(canvas, width, height, group.deviceId)"
                  :canvasId="`chart-${group.deviceId}`"
                  width="100%"
                  height="500rpx"
                />
                <view v-else class="chart-empty">
                  <text class="empty-text">暂无数据</text>
                </view>
              </view>
            </view>
          </view>

          <view v-if="selectedDevices.length === 0" class="empty-hint">
            <text class="empty-text">请添加设备查看监测数据</text>
          </view>
        </view>
      </view>

      <view class="bottom-spacer" />
    </scroll-view>

    <!-- FAB 添加设备按钮 -->
    <view class="fab-btn" @click="showDevicePicker = true">
      <text class="fab-icon">+</text>
      <view v-if="selectedDevices.length > 0" class="fab-badge">
        <text class="fab-badge-text">{{ selectedDevices.length }}</text>
      </view>
    </view>

    <!-- 设备选择弹窗 -->
    <view v-if="showDevicePicker" class="picker-mask" @click="showDevicePicker = false">
      <view class="picker-panel" @click.stop>
        <view class="picker-header">
          <text class="picker-title">选择设备</text>
          <text class="picker-close" @click="showDevicePicker = false">×</text>
        </view>
        <scroll-view class="picker-list" scroll-y>
          <view
            v-for="device in availableDevices"
            :key="device.id"
            class="picker-item"
            :class="{ selected: isSelected(device.id) }"
            @click="toggleDevice(device)"
          >
            <view class="picker-device-info">
              <text class="picker-device-name">{{ device.deviceName }}</text>
              <text class="picker-device-type">{{ device.deviceType }} · {{ device.hazardName }}</text>
            </view>
            <view class="picker-check" v-if="isSelected(device.id)">✓</view>
          </view>
        </scroll-view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useSafeArea } from '@/composables/useSafeArea'
import * as echartsLib from '@/components/echarts.esm.min.js'
import EchartsComponent from '@/components/echarts.vue'
import { deviceApi } from '@/utils/device'
import type { DeviceInfo } from '@/utils/device'

const { statusBarHeight } = useSafeArea()

const timeTabs = [
  { label: '24小时', value: '24h' },
  { label: '7天', value: '7d' },
  { label: '30天', value: '30d' }
]

const loading = ref(false)
const allDevices = ref<DeviceInfo[]>([])
const selectedDevices = ref<DeviceInfo[]>([])
const activeTimeTab = ref('24h')
const showDevicePicker = ref(false)

interface ChartGroup {
  deviceId: number
  deviceName: string
  deviceType: string
  option: any
}

const chartGroups = ref<ChartGroup[]>([])

const availableDevices = computed(() => allDevices.value)

const isSelected = (id: number) => selectedDevices.value.some(d => d.id === id)

onMounted(() => {
  allDevices.value = deviceApi.getAll()
  // 检查URL参数，预选设备
  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1]
  const options = (currentPage as any).options || (currentPage as any).$page?.options
  if (options?.deviceId) {
    const device = deviceApi.getById(Number(options.deviceId))
    if (device) {
      selectedDevices.value = [device]
      loadAllCharts()
    }
  }
})

const toggleDevice = (device: DeviceInfo) => {
  const idx = selectedDevices.value.findIndex(d => d.id === device.id)
  if (idx >= 0) {
    selectedDevices.value.splice(idx, 1)
  } else {
    selectedDevices.value.push(device)
  }
  loadAllCharts()
}

const removeDevice = (id: number) => {
  selectedDevices.value = selectedDevices.value.filter(d => d.id !== id)
  chartGroups.value = chartGroups.value.filter(g => g.deviceId !== id)
}

const changeTimeTab = (value: string) => {
  activeTimeTab.value = value
  loadAllCharts()
}

const loadAllCharts = async () => {
  if (selectedDevices.value.length === 0) {
    chartGroups.value = []
    return
  }

  loading.value = true
  try {
    const groups: ChartGroup[] = []

    for (const device of selectedDevices.value) {
      let hours = 24
      switch (activeTimeTab.value) {
        case '7d': hours = 168; break
        case '30d': hours = 720; break
      }

      const endTime = new Date().toISOString()
      const startTime = new Date(Date.now() - hours * 3600000).toISOString()

      const seriesList: any[] = []
      const allCategories: Set<string> = new Set()
      const attrDataMap: { attr: any; data: any[]; isRain: boolean }[] = []

      for (const attr of device.attributes) {
        const historyData = deviceApi.getHistoryData(device.id, startTime, endTime, attr.property)
        if (historyData && historyData.length > 0) {
          const isRain = attr.property.includes('rain')
          attrDataMap.push({ attr, data: historyData, isRain })
          historyData.forEach((item: any) => {
            const date = new Date(item.time.replace(' ', 'T'))
            const label = activeTimeTab.value === '24h'
              ? `${date.getHours()}:00`
              : `${date.getMonth() + 1}/${date.getDate()}`
            allCategories.add(label)
          })
        }
      }

      if (attrDataMap.length === 0) {
        groups.push({
          deviceId: device.id,
          deviceName: device.deviceName,
          deviceType: device.deviceType,
          option: null
        })
        continue
      }

      const categories = Array.from(allCategories)
      const seriesColors = ['#3068e4', '#52c41a', '#fa8c16', '#722ed1', '#13c2c2', '#eb2f96']

      for (let i = 0; i < attrDataMap.length; i++) {
        const { attr, data, isRain } = attrDataMap[i]
        const color = seriesColors[i % seriesColors.length]
        const valueMap = new Map<string, number>()
        data.forEach((item: any) => {
          const date = new Date(item.time.replace(' ', 'T'))
          const label = activeTimeTab.value === '24h'
            ? `${date.getHours()}:00`
            : `${date.getMonth() + 1}/${date.getDate()}`
          valueMap.set(label, parseFloat(item.value) || 0)
        })

        const values = categories.map(c => valueMap.get(c) ?? null)

        if (isRain) {
          seriesList.push({
            name: attr.displayName,
            type: 'bar',
            data: values,
            yAxisIndex: attrDataMap.some((d, j) => j < i && !d.isRain) ? 1 : 0,
            itemStyle: {
              color: {
                type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
                colorStops: [
                  { offset: 0, color: color },
                  { offset: 1, color: hexToRgba(color, 0.4) }
                ]
              },
              borderRadius: [4, 4, 0, 0]
            },
            barWidth: '30%'
          })
        } else {
          seriesList.push({
            name: attr.displayName,
            type: 'line',
            data: values,
            smooth: true,
            symbol: 'circle',
            symbolSize: 4,
            showSymbol: categories.length <= 24,
            lineStyle: { color, width: 2.5 },
            itemStyle: { color: '#fff', borderColor: color, borderWidth: 2 },
            areaStyle: {
              color: {
                type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
                colorStops: [
                  { offset: 0, color: hexToRgba(color, 0.2) },
                  { offset: 1, color: hexToRgba(color, 0.02) }
                ]
              }
            }
          })
        }
      }

      const hasBarAndLine = attrDataMap.some(d => d.isRain) && attrDataMap.some(d => !d.isRain)

      const option = {
        animation: true,
        legend: {
          data: attrDataMap.map(d => d.attr.displayName),
          bottom: 0,
          textStyle: { color: '#6b7280', fontSize: 10 },
          itemWidth: 16,
          itemHeight: 10
        },
        grid: { left: '5%', right: hasBarAndLine ? '8%' : '5%', bottom: '15%', top: '10%', containLabel: true },
        xAxis: {
          type: 'category',
          data: categories,
          boundaryGap: attrDataMap.some(d => d.isRain),
          axisLabel: { color: '#9ca3af', fontSize: 9, margin: 8 },
          axisLine: { lineStyle: { color: '#e5e7eb' } },
          axisTick: { show: false },
          splitLine: { show: false }
        },
        yAxis: hasBarAndLine ? [
          {
            type: 'value',
            axisLabel: { color: '#9ca3af', fontSize: 9 },
            axisLine: { show: false },
            axisTick: { show: false },
            splitLine: { lineStyle: { color: '#f3f4f6', type: 'dashed' } }
          },
          {
            type: 'value',
            axisLabel: { color: '#9ca3af', fontSize: 9 },
            axisLine: { show: false },
            axisTick: { show: false },
            splitLine: { show: false }
          }
        ] : {
          type: 'value',
          axisLabel: { color: '#9ca3af', fontSize: 9 },
          axisLine: { show: false },
          axisTick: { show: false },
          splitLine: { lineStyle: { color: '#f3f4f6', type: 'dashed' } }
        },
        series: seriesList,
        tooltip: {
          trigger: 'axis',
          backgroundColor: 'rgba(0,0,0,0.8)',
          borderColor: 'rgba(0,0,0,0.8)',
          textStyle: { color: '#fff', fontSize: 11 }
        }
      }

      groups.push({
        deviceId: device.id,
        deviceName: device.deviceName,
        deviceType: device.deviceType,
        option
      })
    }

    chartGroups.value = groups
  } finally {
    loading.value = false
  }
}

const initChart = (canvas: any, width: number, height: number, deviceId: number) => {
  const group = chartGroups.value.find(g => g.deviceId === deviceId)
  if (!group?.option) return null

  const chart = echartsLib.init(canvas, null, { width, height })
  canvas.setChart(chart)
  chart.setOption(group.option)
  return chart
}

const hexToRgba = (hex: string, alpha: number) => {
  const r = parseInt(hex.slice(1, 3), 16)
  const g = parseInt(hex.slice(3, 5), 16)
  const b = parseInt(hex.slice(5, 7), 16)
  return `rgba(${r}, ${g}, ${b}, ${alpha})`
}
</script>

<style lang="scss" scoped>
.page-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, #eef1f8 0%, #e8ecf4 100%);
}

.header {
  position: relative;
  flex-shrink: 0;
}

.header-bg {
  position: absolute;
  top: 0; left: 0; right: 0;
  background: linear-gradient(135deg, #3068e4 0%, #1e5acc 100%);
  border-radius: 0 0 15rpx 15rpx;
  overflow: hidden;
}

.status-bar { width: 100%; }

.bg-circle {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
}

.bg-circle-1 { width: 300rpx; height: 300rpx; top: -80rpx; right: -60rpx; }
.bg-circle-2 { width: 200rpx; height: 200rpx; top: 80rpx; left: -50rpx; }

.header-content {
  position: relative;
  z-index: 1;
  padding: 40rpx 32rpx 24rpx;
}

.header-title {
  font-size: 40rpx;
  font-weight: bold;
  color: #ffffff;
  margin-bottom: 8rpx;
  display: block;
}

.header-subtitle {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.8);
}

/* 已选设备标签 */
.selected-devices {
  padding: 16rpx 32rpx;
  flex-shrink: 0;
}

.tags-scroll {
  white-space: nowrap;
}

.tags-container {
  display: flex;
  gap: 12rpx;
  align-items: center;
}

.device-tag {
  display: flex;
  align-items: center;
  gap: 8rpx;
  padding: 10rpx 16rpx;
  background: #ffffff;
  border-radius: 24rpx;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.06);
  flex-shrink: 0;
}

.tag-text {
  font-size: 24rpx;
  color: #3068e4;
}

.tag-close {
  font-size: 28rpx;
  color: #9ca3af;
  padding-left: 4rpx;
}

.hint-tag {
  padding: 10rpx 20rpx;
}

.hint-text {
  font-size: 24rpx;
  color: #9ca3af;
}

.content-scroll {
  flex: 1;
  height: 0;
}

.time-tabs {
  display: flex;
  gap: 16rpx;
  margin: 0 24rpx 20rpx;
}

.time-tab {
  flex: 1;
  text-align: center;
  padding: 20rpx 0;
  background: #ffffff;
  border-radius: 16rpx;
  font-size: 26rpx;
  color: #6b7280;

  &.active {
    background: linear-gradient(135deg, #3068e4 0%, #1e5acc 100%);
    color: #ffffff;
  }
}

.charts-container {
  padding: 0 24rpx 24rpx;
}

.device-chart-group {
  margin-bottom: 32rpx;
}

.chart-card {
  background: #ffffff;
  border-radius: 24rpx;
  padding: 24rpx;
  box-shadow: 0 8rpx 32rpx rgba(102, 126, 234, 0.12);
}

.card-header {
  display: flex;
  align-items: baseline;
  gap: 12rpx;
  margin-bottom: 16rpx;
}

.card-header-left {
  display: flex;
  align-items: baseline;
  gap: 12rpx;
}

.group-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #1a1a2e;
}

.group-sub {
  font-size: 24rpx;
  color: #9ca3af;
}

.chart-container {
  width: 100%;
  height: 500rpx;
  position: relative;
}

.chart-empty, .empty-hint {
  height: 400rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.empty-hint {
  height: 200rpx;
}

.empty-text {
  font-size: 26rpx;
  color: #9ca3af;
}

.loading-wrapper {
  display: flex;
  justify-content: center;
  padding: 80rpx 0;
  color: #9ca3af;
}

.bottom-spacer { height: 120rpx; }

/* FAB 按钮 */
.fab-btn {
  position: fixed;
  right: 40rpx;
  bottom: 60rpx;
  width: 100rpx;
  height: 100rpx;
  background: linear-gradient(135deg, #3068e4 0%, #1e5acc 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8rpx 32rpx rgba(48, 104, 228, 0.4);
  z-index: 100;

  &:active {
    opacity: 0.9;
    transform: scale(0.95);
  }
}

.fab-icon {
  font-size: 48rpx;
  color: #ffffff;
  font-weight: 300;
}

.fab-badge {
  position: absolute;
  top: -8rpx;
  right: -8rpx;
  min-width: 36rpx;
  height: 36rpx;
  background: #f5222d;
  border-radius: 18rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 8rpx;
  border: 2rpx solid #ffffff;
}

.fab-badge-text {
  font-size: 20rpx;
  color: #ffffff;
  font-weight: 600;
}

/* 设备选择弹窗 */
.picker-mask {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 999;
  display: flex;
  align-items: flex-end;
}

.picker-panel {
  width: 100%;
  max-height: 70vh;
  background: #ffffff;
  border-radius: 24rpx 24rpx 0 0;
  display: flex;
  flex-direction: column;
}

.picker-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 28rpx 32rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.picker-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #1a1a2e;
}

.picker-close {
  font-size: 40rpx;
  color: #9ca3af;
}

.picker-list {
  max-height: 60vh;
}

.picker-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24rpx 32rpx;
  border-bottom: 1rpx solid #f5f5f5;

  &.selected {
    background: rgba(48, 104, 228, 0.05);
  }
}

.picker-device-info {
  display: flex;
  flex-direction: column;
  gap: 4rpx;
}

.picker-device-name {
  font-size: 28rpx;
  color: #1a1a2e;
  font-weight: 500;
}

.picker-device-type {
  font-size: 22rpx;
  color: #9ca3af;
}

.picker-check {
  font-size: 32rpx;
  color: #3068e4;
  font-weight: bold;
}
</style>
