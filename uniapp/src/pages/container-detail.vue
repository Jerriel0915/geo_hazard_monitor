<!-- src/pages/container-detail.vue -->
<template>
  <view class="page-container">
    <!-- 头部信息 -->
    <PageHeader show-back>
      <view class="container-info-top">
        <text class="container-no">{{ container.containerNo }}</text>
        <text class="container-name">{{ container.containerName || '未命名' }}</text>
      </view>
      <view class="header-right status-tag" :class="statusClass">
        <view class="status-dot" />
        <text>{{ statusText }}</text>
      </view>
    </PageHeader>

    <!-- 可滚动内容区域 -->
    <scroll-view class="page-body" scroll-y>
      <!-- 实时数据 -->
      <view class="section">
        <text class="section-title">实时数据</text>
        <view class="data-cards" :style="{ gridTemplateColumns: `repeat(${Math.min(realtimeData.length, 4)}, 1fr)` }">
          <view v-for="item in realtimeData" :key="`${item.property}-${item.deviceId}`" class="data-item">
            <zui-svg-icon :icon="getIcon(item.property)" width="36rpx" />
            <view class="data-value-row">
              <text class="data-value" :class="getValueClass(item.property, item.value, item.hasAlarm)">
                {{ formatValue(item.value) }}
              </text>
              <text v-if="item.unit" class="data-unit-inline">{{ item.unit }}</text>
            </view>
            <text class="data-label">{{ item.displayName }}</text>
            <view v-if="item.hasAlarm" class="alarm-badge">
              异常
            </view>
          </view>
          <view v-if="realtimeData.length === 0" class="data-empty">
            <text class="empty-text">暂无数据</text>
          </view>
        </view>
      </view>

      <!-- 最近告警 -->
      <view class="section">
        <view class="section-header">
          <text class="section-title">最近告警</text>
          <navigator url="/pages/alarm-handle" open-type="switchTab" class="more-link">
            查看全部 →
          </navigator>
        </view>
        <view class="alarm-list">
          <view v-for="alarm in recentAlarms" :key="alarm.id" class="alarm-item">
            <view class="alarm-level" :class="getAlarmLevelClass(alarm.alarmLevel)">
              {{ getAlarmLevelText(alarm.alarmLevel) }}
            </view>
            <view class="alarm-content">
              <text class="alarm-message">{{ alarm.alarmContent }}</text>
              <text class="alarm-time">{{ formatTime(alarm.createTime) }}</text>
            </view>
          </view>
          <EmptyState
            v-if="recentAlarms.length === 0"
            iconName="bell"
            :iconSize="120"
            title="暂无告警"
          />
        </view>
      </view>

      <!-- 设备列表 -->
      <view class="section">
        <text class="section-title">关联设备</text>
        <view class="device-list">
          <view v-for="(device, deviceIdx) in devices" :key="device.id" class="device-item-full">
            <view class="device-header">
              <view class="device-info">
                <text class="device-name">{{ device.deviceName }}</text>
                <text class="device-code">{{ device.deviceCode }}</text>
              </view>
              <view class="device-header-right">
                <view class="device-status" :class="device.status === 1 ? 'online' : 'offline'">
                  {{ device.status === 1 ? '在线' : '离线' }}
                </view>
                <view class="device-more-link" @click="goToDeviceChart(device)">
                  查看更多 →
                </view>
              </view>
            </view>
            <!-- 设备的属性图表卡片 -->
            <view class="device-attributes">
              <view v-if="!device.defaultAttributes || device.defaultAttributes.length === 0" class="no-attrs-hint">
                <text class="hint-text">该设备暂无默认展示的属性</text>
              </view>
              <view
                v-for="(attr, attrIdx) in device.defaultAttributes"
                :key="`${device.id}-${attr.property}`"
                class="attribute-chart-card"
              >
                <view class="attr-chart-header">
                  <text class="attr-chart-title">【{{ device.deviceName }}:{{ attr.displayName }}】</text>
                  <view class="time-range-tabs">
                    <view
                      v-for="range in timeRanges"
                      :key="range.value"
                      class="time-tab"
                      :class="{ active: attr.timeRange === range.value }"
                      @click.stop="switchAttributeTimeRange(deviceIdx, attrIdx, range.value)"
                    >
                      {{ range.label }}
                    </view>
                  </view>
                </view>
                <view class="chart-container">
                  <EchartsComponent
                    v-if="attr.chartData && attr.chartData.series && attr.chartData.series.length > 0"
                    :onInit="(canvas, width, height) => initChart(canvas, width, height, deviceIdx, attrIdx)"
                    :canvasId="`chart-${device.id}-${attr.property}`"
                    width="100%"
                    height="400rpx"
                  />
                  <view v-else class="chart-empty">
                    <zui-svg-icon icon="chart-line" width="80rpx" />
                    <text class="empty-text">暂无{{ attr.displayName }}数据</text>
                    <text class="empty-hint">该属性暂未采集到历史数据</text>
                  </view>
                </view>
              </view>
            </view>
          </view>
          <EmptyState
            v-if="devices.length === 0"
            iconName="device"
            :iconSize="120"
            title="暂无关联设备"
          />
        </view>
      </view>

      <!-- 底部留白 -->
      <view class="bottom-spacer" />
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import * as echartsLib from '@/components/echarts.esm.min.js'
import EchartsComponent from '@/components/echarts.vue'
import EmptyState from '@/components/EmptyState.vue'
import PageHeader from '@/components/PageHeader.vue'
import alarmApi from '@/utils/alarm'
import containerApi from '@/utils/container'
import deviceApi from '@/utils/device'
import { startPolling, stopPolling } from '@/utils/polling'
import { computed, onMounted, onUnmounted, ref } from 'vue'

const containerId = ref(0)
const container = ref<any>({})
const devices = ref<any[]>([])
const recentAlarms = ref<any[]>([])
const realtimeData = ref<any[]>([])
const timeRanges = [
  { label: '近3天', value: 3 },
  { label: '近7天', value: 7 },
  { label: '近30天', value: 30 },
]

const statusClass = computed(() => {
  return container.value.status === 1 ? 'success' : 'inactive'
})

const statusText = computed(() => {
  return container.value.status === 1 ? '运行中' : '已停用'
})

onMounted(() => {
  console.log('[container-detail.vue] mounted')

  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1]
  const options = (currentPage as any).options || (currentPage as any).$page?.options
  if (options?.id) {
    containerId.value = Number.parseInt(options.id)
    loadData()
    // 启动轮询刷新实时数据
    startPolling(loadRealtimeData, 10000)
  }
})

onUnmounted(() => {
  stopPolling()
})
const loadData = async () => {
  try {
    uni.showLoading({ title: '加载中...' })

    const [containerData, deviceList, alarms] = await Promise.all([
      containerApi.getById(containerId.value),
      deviceApi.getByContainerId(containerId.value),
      alarmApi.getByContainerId(containerId.value, 5),
    ])

    container.value = containerData

    // 为每个设备获取default属性并初始化
    devices.value = await Promise.all((deviceList || []).map(async (d: any) => {
      // 获取设备详情，包含属性信息
      const deviceDetail = await deviceApi.getById(d.id)
      const defaultAttrs = (deviceDetail?.attributeMetas || deviceDetail?.AttributeMetas || [])
        .filter((attr: any) => attr.default === true)
        .map((attr: any) => ({
          ...attr,
          timeRange: 3,
          chartData: null,
          loading: false,
        }))

      return {
        ...d,
        defaultAttributes: defaultAttrs,
      }
    }))

    recentAlarms.value = alarms || []

    // 加载实时数据
    await loadRealtimeData()

    // 加载每个设备每个属性的数据曲线
    for (let deviceIdx = 0; deviceIdx < devices.value.length; deviceIdx++) {
      const device = devices.value[deviceIdx]
      for (let attrIdx = 0; attrIdx < device.defaultAttributes.length; attrIdx++) {
        await loadAttributeChartData(deviceIdx, attrIdx)
      }
    }
  } catch (error) {
    console.error('加载数据失败:', error)
    uni.showToast({ title: '加载数据失败', icon: 'none' })
  } finally {
    uni.hideLoading()
  }
}

// 加载实时数据（使用独立接口）
const loadRealtimeData = async () => {
  try {
    const realtime = await containerApi.getRealtime(containerId.value)
    if (realtime && realtime.realtime) {
      realtimeData.value = realtime.realtime.map((item: any) => ({
        ...item,
        numericValue: Number.parseFloat(item.value) || 0,
        hasAlarm: item.hasAlarm || false,
      }))
    } else {
      realtimeData.value = []
    }
  } catch (error) {
    console.error('加载实时数据失败:', error)
  }
}

// 获取图标
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

// 获取数值样式类（根据后端返回的hasAlarm字段判断状态）
const getValueClass = (property: string, value: string, hasAlarm?: boolean) => {
  // 如果后端返回了hasAlarm字段，优先使用
  if (hasAlarm === true) {
    return 'danger' // 有激活告警，显示异常状态
  } else if (hasAlarm === false) {
    return 'normal' // 无激活告警，显示正常状态
  }

  // 兼容旧逻辑：如果没有hasAlarm字段，根据阈值判断
  const numValue = Number.parseFloat(value)
  if (isNaN(numValue))
    return ''

  // 根据不同属性判断状态
  if (property === 'temperature') {
    if (numValue >= 35 || numValue <= 0)
      return 'danger'
    if (numValue >= 30 || numValue <= 5)
      return 'warning'
  } else if (property === 'humidity') {
    if (numValue >= 80 || numValue <= 20)
      return 'danger'
    if (numValue >= 70 || numValue <= 30)
      return 'warning'
  } else if (property === 'smoke' || property === 'smoke_level') {
    if (numValue > 0.5)
      return 'danger'
  }

  return 'normal'
}

// 格式化数值
const formatValue = (val: string) => {
  if (!val)
    return '-'
  const num = Number.parseFloat(val)
  if (!isNaN(num)) {
    return num.toFixed(1)
  }
  return val
}

const formatTime = (time: string) => {
  if (!time)
    return '-'
  // iOS 兼容：将时间格式转换为支持的格式
  const iosTime = time.replace(' ', 'T').replace(/\.\d+Z$/, '')
  const date = new Date(iosTime)
  return `${date.getMonth() + 1}-${date.getDate()} ${date.getHours()}:${String(date.getMinutes()).padStart(2, '0')}`
}

// 切换属性的时间范围
const switchAttributeTimeRange = async (deviceIdx: number, attrIdx: number, days: number) => {
  devices.value[deviceIdx].defaultAttributes[attrIdx].timeRange = days
  await loadAttributeChartData(deviceIdx, attrIdx)
}

// 加载单个属性的曲线数据
const loadAttributeChartData = async (deviceIdx: number, attrIdx: number) => {
  const device = devices.value[deviceIdx]
  const attr = device.defaultAttributes[attrIdx]

  try {
    console.log('[container-detail.vue] loadAttributeChartData called', { device, attr })

    attr.loading = true
    const endTime = new Date().toISOString()
    const startTime = new Date(Date.now() - attr.timeRange * 24 * 60 * 60 * 1000).toISOString()

    // 传入属性参数，后端直接过滤
    const historyData = await deviceApi.getHistoryData(device.id, startTime, endTime, attr.property)

    console.log('[container-detail.vue] historyData:', historyData)

    // 处理数据转换为图表格式
    const categories: string[] = []
    const values: number[] = []

    if (historyData && historyData.length > 0) {
      // 后端已按属性过滤，直接使用返回数据
      // 按时间排序
      const sortedData = historyData.sort((a: any, b: any) => {
        const timeA = a.time.replace(' ', 'T').replace(/\.\d+Z$/, '')
        const timeB = b.time.replace(' ', 'T').replace(/\.\d+Z$/, '')
        return new Date(timeA).getTime() - new Date(timeB).getTime()
      })

      console.log('[container-detail.vue] sortedData:', sortedData)

      // 采样最多50个点
      const step = Math.max(1, Math.floor(sortedData.length / 50))
      for (let i = 0; i < sortedData.length; i += step) {
        const item = sortedData[i]
        const iosTime = item.time.replace(' ', 'T').replace(/\.\d+Z$/, '')
        const date = new Date(iosTime)
        categories.push(`${date.getMonth() + 1}/${date.getDate()} ${date.getHours()}:${String(date.getMinutes()).padStart(2, '0')}`)
        // 使用 value 字段获取数据值（后端已简化返回格式）
        values.push(Number.parseFloat(item.value) || 0)
      }
    }

    console.log('[container-detail.vue] categories:', categories)
    console.log('[container-detail.vue] values:', values)

    // 生成 echarts option
    attr.chartData = generateEChartsOption(categories, values, attr)
    console.log('[container-detail.vue] chartData:', attr.chartData)
  } catch (error) {
    console.error('加载属性曲线数据失败:', error)
    attr.chartData = null
  } finally {
    attr.loading = false
  }
}

// 初始化图表（onInit回调）
const initChart = (canvas: any, width: any, height: any, deviceIdx: number, attrIdx: number) => {
  const device = devices.value[deviceIdx]
  const attr = device.defaultAttributes[attrIdx]

  console.log('[container-detail.vue] initChart called', {
    width,
    height,
    deviceIdx,
    attrIdx,
    chartData: attr.chartData
  })

  const chart = echartsLib.init(canvas, null, {
    width: width,
    height: height
  })
  canvas.setChart(chart)
  chart.setOption(attr.chartData)
  return chart
}

// 生成 echarts 图表配置
const generateEChartsOption = (categories: string[], values: number[], attr: any) => {
  if (!categories || categories.length === 0) {
    return null
  }

  // 根据属性类型选择颜色
  const colorMap: Record<string, string> = {
    temperature: '#f5222d',
    humidity: '#52c41a',
    smoke: '#faad14',
    smoke_level: '#faad14',
  }
  const color = colorMap[attr.property] || '#3068e4'

  // 美化的折线图配置
  return {
    animation: true,
    animationDuration: 1000,
    animationEasing: 'cubicOut',
    grid: {
      left: '5%',
      right: '5%',
      bottom: '20%',
      top: '10%',
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      data: categories,
      boundaryGap: false,
      axisLabel: {
        color: '#9ca3af',
        fontSize: 10,
        margin: 8,
        rotate: categories.length > 10 ? 30 : 0
      },
      axisLine: {
        show: true,
        lineStyle: {
          color: '#e5e7eb',
          width: 1
        }
      },
      axisTick: {
        show: false
      },
      splitLine: {
        show: false
      }
    },
    yAxis: {
      type: 'value',
      axisLabel: {
        color: '#9ca3af',
        fontSize: 10,
        formatter: (value: number) => value.toFixed(1)
      },
      axisLine: {
        show: false
      },
      axisTick: {
        show: false
      },
      splitLine: {
        show: true,
        lineStyle: {
          color: '#f3f4f6',
          width: 1,
          type: 'dashed'
        }
      }
    },
    series: [{
      name: attr.displayName,
      type: 'line',
      data: values,
      smooth: true,
      smoothMonotone: 'x',
      symbol: 'circle',
      symbolSize: 6,
      showSymbol: values.length <= 20,
      hoverAnimation: true,
      lineStyle: {
        color: color,
        width: 2.5,
        shadowColor: color,
        shadowBlur: 8,
        shadowOffsetY: 4
      },
      itemStyle: {
        color: '#ffffff',
        borderColor: color,
        borderWidth: 2,
        shadowColor: 'rgba(0, 0, 0, 0.1)',
        shadowBlur: 3,
        shadowOffsetY: 1
      },
      emphasis: {
        itemStyle: {
          color: color,
          borderColor: '#ffffff',
          borderWidth: 2.5,
          shadowColor: color,
          shadowBlur: 8
        },
        scale: true,
        scaleSize: 8
      },
      areaStyle: {
        color: {
          type: 'linear',
          x: 0,
          y: 0,
          x2: 0,
          y2: 1,
          colorStops: [{
            offset: 0,
            color: hexToRgba(color, 0.25)
          }, {
            offset: 0.5,
            color: hexToRgba(color, 0.08)
          }, {
            offset: 1,
            color: hexToRgba(color, 0.01)
          }]
        }
      }
    }],
    dataZoom: [
      { type: 'slider', xAxisIndex: 0, height: 18, bottom: 4, borderColor: '#e5e7eb', fillerColor: 'rgba(48,104,228,0.15)', handleSize: '60%', textStyle: { fontSize: 9, color: '#9ca3af' } },
      { type: 'inside', xAxisIndex: 0 },
    ],
    tooltip: {
      trigger: 'axis',
      triggerOn: 'mousemove|click',
      backgroundColor: 'rgba(0, 0, 0, 0.8)',
      borderColor: 'rgba(0, 0, 0, 0.8)',
      borderWidth: 0,
      padding: [6, 10],
      textStyle: {
        color: '#ffffff',
        fontSize: 11
      },
      formatter: (params: any) => {
        const param = params[0]
        return `${param.name}<br/>${param.seriesName}: <b>${param.value}</b>`
      }
    }
  }
}

// 辅助函数：将hex颜色转换为rgba
const hexToRgba = (hex: string, alpha: number) => {
  const r = parseInt(hex.slice(1, 3), 16)
  const g = parseInt(hex.slice(3, 5), 16)
  const b = parseInt(hex.slice(5, 7), 16)
  return `rgba(${r}, ${g}, ${b}, ${alpha})`
}

// 跳转到图表页面
const goToChart = (device: any, attr: any) => {
  // 发送预选择事件
  uni.$emit('chartPreselect', {
    containerId: containerId.value,
    deviceId: device.id
  })
  // 跳转到图表tab
  uni.switchTab({
    url: '/pages/chart'
  })
}

// 跳转到设备图表（从"查看更多"链接）
const goToDeviceChart = (device: any) => {
  console.log('[container-detail.vue] goToDeviceChart 调用', {
    containerId: containerId.value,
    device: device
  })
  // 使用全局变量传递预选择参数（更可靠）
  ;(globalThis as any).__chartPreselect__ = {
    containerId: containerId.value,
    deviceId: device.id,
    timestamp: Date.now()
  }
  console.log('[container-detail.vue] 已设置全局预选择参数')
  // 跳转到图表tab
  uni.switchTab({
    url: '/pages/chart'
  })
}


const getAlarmLevelClass = (level: number) => {
  switch (level) {
    case 4: return 'danger'
    case 3: return 'danger'
    case 2: return 'warning'
    default: return 'info'
  }
}

const getAlarmLevelText = (level: number) => {
  switch (level) {
    case 4: return '紧急'
    case 3: return '严重'
    case 2: return '一般'
    default: return '提示'
  }
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

.container-info-top {
  margin-bottom: 16rpx;
}

.status-tag {
  display: flex;
  align-items: center;
  gap: 8rpx;
  padding: 12rpx 20rpx;
  border-radius: 8rpx;
  font-size: 24rpx;

  &.success {
    background: rgba(82, 196, 26, 0.2);
    color: #52c41a;

    .status-dot {
      background: #52c41a;
    }
  }

  &.inactive {
    background: rgba(156, 163, 175, 0.2);
    color: #9ca3af;

    .status-dot {
      background: #9ca3af;
    }
  }
}

.status-dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
}

.container-no {
  display: block;
  font-size: 44rpx;
  font-weight: bold;
  color: #ffffff;
  margin-bottom: 8rpx;
}

.container-name {
  font-size: 28rpx;
  color: rgba(255, 255, 255, 0.8);
}

.section {
  margin: 0 32rpx 24rpx;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16rpx;
}

.section-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #1a1a2e;
  margin-bottom: 16rpx;
  display: block;
}

.more-link {
  font-size: 24rpx;
  color: #3068e4;
  margin-bottom: 16rpx;
}

.data-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16rpx;
  background: #ffffff;
  border-radius: 24rpx;
  padding: 24rpx 16rpx;
  box-shadow: 0 8rpx 32rpx rgba(102, 126, 234, 0.12);
}

.data-item {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8rpx;
  padding: 16rpx 8rpx;
  border-radius: 16rpx;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.05) 0%, rgba(118, 75, 162, 0.05) 100%);
  min-width: 0;
}

.alarm-badge {
  position: absolute;
  top: 8rpx;
  right: 8rpx;
  padding: 4rpx 8rpx;
  background: rgba(245, 34, 45, 0.9);
  color: #ffffff;
  font-size: 18rpx;
  border-radius: 8rpx;
  line-height: 1;
}

.data-value {
  font-size: 32rpx;
  font-weight: 700;
  color: #1a1a2e;
  text-align: center;

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

.data-value-row {
  display: flex;
  align-items: baseline;
  gap: 4rpx;
}

.data-unit-inline {
  font-size: 20rpx;
  color: #6b7280;
  font-weight: 500;
}

.data-label {
  font-size: 22rpx;
  color: #6b7280;
  text-align: center;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100%;
}

.data-empty {
  grid-column: 1 / -1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60rpx 0;
}

.empty-text {
  font-size: 24rpx;
  color: #9ca3af;
}

.device-list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.device-item-full {
  background: #ffffff;
  border-radius: 24rpx;
  overflow: hidden;
  box-shadow: 0 8rpx 32rpx rgba(102, 126, 234, 0.12);
  margin-bottom: 24rpx;
}

.device-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.device-info {
  display: flex;
  flex-direction: column;
  gap: 4rpx;
}

.device-name {
  font-size: 28rpx;
  color: #1a1a2e;
  font-weight: 600;
}

.device-code {
  font-size: 24rpx;
  color: #6b7280;
}

.device-status {
  padding: 8rpx 16rpx;
  border-radius: 8rpx;
  font-size: 22rpx;
  flex-shrink: 0;

  &.online {
    background: rgba(82, 196, 26, 0.1);
    color: #52c41a;
  }

  &.offline {
    background: rgba(156, 163, 175, 0.1);
    color: #9ca3af;
  }
}

.device-header-right {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.device-more-link {
  font-size: 24rpx;
  color: #3068e4;
  padding: 8rpx 16rpx;
  border-radius: 8rpx;
  background: rgba(102, 126, 234, 0.08);
  flex-shrink: 0;
}

.device-attributes {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
  padding: 16rpx;
}

.no-attrs-hint {
  padding: 40rpx;
  text-align: center;
  background: rgba(102, 126, 234, 0.03);
  border-radius: 16rpx;
}

.hint-text {
  font-size: 24rpx;
  color: #9ca3af;
}

.attribute-chart-card {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.03) 0%, rgba(118, 75, 162, 0.03) 100%);
  border-radius: 16rpx;
  padding: 16rpx;
  border: 1rpx solid rgba(102, 126, 234, 0.1);
  position: relative;
}

.attr-chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16rpx;
}

.attr-chart-title {
  font-size: 24rpx;
  font-weight: 600;
  color: #1a1a2e;
}

.chart-container {
  width: 100%;
  height: 400rpx;
  position: relative;
  overflow: hidden;
}

.chart-empty {
  height: 400rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: rgba(102, 126, 234, 0.03);
  border-radius: 16rpx;
  gap: 16rpx;

  .app-icon {
    opacity: 0.3;
  }
}

.empty-text {
  font-size: 26rpx;
  color: #6b7280;
  font-weight: 500;
}

.empty-hint {
  font-size: 22rpx;
  color: #9ca3af;
}

.time-range-tabs {
  display: flex;
  gap: 8rpx;
  background: #f5f5f5;
  border-radius: 8rpx;
  padding: 4rpx;
}

.time-tab {
  padding: 8rpx 16rpx;
  font-size: 22rpx;
  color: #666;
  border-radius: 6rpx;
  transition: all 0.3s;

  &.active {
    background: #3068e4;
    color: #ffffff;
  }
}

.alarm-list {
  background: #ffffff;
  border-radius: 24rpx;
  overflow: hidden;
  box-shadow: 0 8rpx 32rpx rgba(102, 126, 234, 0.12);
}

.alarm-item {
  display: flex;
  padding: 20rpx 24rpx;
  border-bottom: 1rpx solid #f0f0f0;

  &:last-child {
    border-bottom: none;
  }
}

.alarm-level {
  padding: 6rpx 12rpx;
  border-radius: 8rpx;
  font-size: 20rpx;
  margin-right: 16rpx;
  flex-shrink: 0;

  &.danger {
    background: rgba(245, 34, 45, 0.1);
    color: #f5222d;
  }

  &.warning {
    background: rgba(250, 173, 20, 0.1);
    color: #faad14;
  }

  &.info {
    background: rgba(102, 126, 234, 0.1);
    color: #3068e4;
  }
}

.alarm-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4rpx;
}

.alarm-message {
  font-size: 26rpx;
  color: #1a1a2e;
}

.alarm-time {
  font-size: 22rpx;
  color: #9ca3af;
}

.page-body {
  flex: 1;
  height: 0;
}

.bottom-spacer {
  height: 32rpx;
}
</style>
