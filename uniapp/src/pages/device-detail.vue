<!-- src/pages/device-detail.vue -->
<script setup lang="ts">
import type { DeviceInfo, DeviceSensor, SensorAttr } from '@/utils/device'
import type { ChartSeries } from '@/utils/monitor'
import PageHeader from '@/components/PageHeader.vue'
import { onLoad } from '@dcloudio/uni-app'
import { computed, nextTick, ref } from 'vue'
import { deviceApi } from '@/utils/device'
import { monitorApi } from '@/utils/monitor'
import { wgs84ToGcj02 } from '@/utils/coordTransform'
import * as echartsLib from '@/components/echarts.esm.min.js'
import EchartsComponent from '@/components/echarts.vue'

// === 设备数据 ===
const deviceId = ref(0)
const device = ref<DeviceInfo | undefined>(undefined)
const sensorGroups = ref<SensorGroup[]>([])
const loading = ref(true)

// === 回到顶部 ===
const showBackTop = ref(false)
const scrollTop = ref(0)

function onScroll(e: { detail: { scrollTop: number } }) {
  showBackTop.value = e.detail.scrollTop > 400
}

function scrollToTop() {
  scrollTop.value = 1
  nextTick(() => { scrollTop.value = 0 })
  showBackTop.value = false
}

// === 内联图表状态 ===
const expandedKey = ref('')
const inlineTimeTab = ref<'24h' | '7d'>('24h')
const inlineChartOption = ref<any>(null)
const inlineLoading = ref(false)
const inlineChartVersion = ref(0)

interface AttrWithData extends SensorAttr {
  sensorId: number
  latestValue?: number | null
  latestTime?: string
  quality?: number
}

interface SensorGroup {
  sensor: DeviceSensor
  attrs: AttrWithData[]
}

onLoad(async (options) => {
  if (options?.id) {
    deviceId.value = Number(options.id)
    await loadDevice()
  }
})

async function loadDevice() {
  try {
    device.value = await deviceApi.getById(deviceId.value)
    if (!device.value) {
      uni.showToast({ title: '设备不存在', icon: 'none' })
      setTimeout(() => uni.navigateBack(), 1500)
      return
    }

    const sensors = await deviceApi.getSensors(deviceId.value)

    // 为每个传感器获取最新数据
    const groups: SensorGroup[] = []
    for (const sensor of sensors) {
      const latestMap = await monitorApi.getSensorLatest(deviceId.value, sensor.sensorNo || '')
      const attrs: AttrWithData[] = sensor.attrs.map(a => {
        const latest = latestMap[a.attrCode]
        return {
          ...a,
          sensorId: sensor.id,
          latestValue: latest?.value ?? null,
          latestTime: latest?.time ? formatTimestamp(latest.time) : '',
          quality: latest?.quality,
        }
      })
      groups.push({ sensor, attrs })
    }
    sensorGroups.value = groups
  }
  catch (error) {
    console.error('加载设备详情失败:', error)
  }
  finally {
    loading.value = false
  }
}

// === 内联图表 ===
async function toggleAttr(group: SensorGroup, attr: AttrWithData) {
  const key = `${group.sensor.id}-${attr.attrCode}`
  if (expandedKey.value === key) {
    // 收起
    expandedKey.value = ''
    inlineChartOption.value = null
    return
  }

  expandedKey.value = key
  inlineChartOption.value = null

  const hazardId = device.value?.boundHazardPointId
  if (!hazardId) return

  await loadInlineChart(group.sensor.id, attr.attrCode)
}

async function switchInlineTime(tab: '24h' | '7d') {
  if (inlineTimeTab.value === tab) return
  inlineTimeTab.value = tab

  if (!expandedKey.value) return
  const [sensorIdStr, attrCode] = expandedKey.value.split('-')
  const sensorId = Number(sensorIdStr)
  await loadInlineChart(sensorId, attrCode)
}

async function loadInlineChart(sensorId: number, attrCode: string) {
  const hazardId = device.value?.boundHazardPointId
  if (!hazardId) return

  inlineLoading.value = true
  try {
    const hours = inlineTimeTab.value === '24h' ? 24 : 168
    const endTime = new Date()
    const startTime = new Date(endTime.getTime() - hours * 3600000)
    const fmt = (d: Date) => {
      const pad = (n: number) => String(n).padStart(2, '0')
      return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
    }

    const seriesList = await monitorApi.getChart({
      hazardPointId: hazardId,
      deviceId: deviceId.value,
      sensorId,
      attrCode,
      valueType: 'current',
      startTime: fmt(startTime),
      endTime: fmt(endTime),
    })

    inlineChartOption.value = buildInlineOption(seriesList)
    inlineChartVersion.value++
  }
  catch (error) {
    console.error('加载内联图表失败:', error)
  }
  finally {
    inlineLoading.value = false
  }
}

function buildInlineOption(series: ChartSeries[]): any {
  if (!series || series.length === 0) return null

  const allLabels = new Set<string>()
  series.forEach(s => s.labels.forEach(l => allLabels.add(l)))
  const categories = Array.from(allLabels)

  const colors = ['#3068e4', '#52c41a', '#fa8c16']
  const seriesList = series.map((s, i) => {
    const color = colors[i % colors.length]
    const valueMap = new Map<string, number>()
    s.labels.forEach((l, idx) => valueMap.set(l, s.values[idx]))
    const data = categories.map(c => valueMap.get(c) ?? null)
    return {
      name: s.seriesName || s.attrName,
      type: 'line',
      data,
      smooth: true,
      symbol: 'circle',
      symbolSize: 3,
      showSymbol: categories.length <= 20,
      lineStyle: { color, width: 2 },
      itemStyle: { color: '#fff', borderColor: color, borderWidth: 2 },
      areaStyle: {
        color: {
          type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: hexToRgba(color, 0.2) },
            { offset: 1, color: hexToRgba(color, 0.02) },
          ],
        },
      },
    }
  })

  return {
    animation: true,
    grid: { left: '3%', right: '3%', bottom: '5%', top: '8%', containLabel: true },
    xAxis: {
      type: 'category',
      data: categories,
      boundaryGap: false,
      axisLabel: { color: '#9ca3af', fontSize: 9, margin: 8 },
      axisLine: { lineStyle: { color: '#e5e7eb' } },
      axisTick: { show: false },
      splitLine: { show: false },
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: '#9ca3af', fontSize: 9 },
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { lineStyle: { color: '#f3f4f6', type: 'dashed' } },
    },
    series: seriesList,
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(0,0,0,0.8)',
      borderColor: 'rgba(0,0,0,0.8)',
      textStyle: { color: '#fff', fontSize: 11 },
    },
  }
}

function initInlineChart(canvas: any, width: number, height: number) {
  if (!inlineChartOption.value) return null
  const chart = echartsLib.init(canvas, null, { width, height })
  canvas.setChart(chart)
  chart.setOption(inlineChartOption.value)
  return chart
}

// === 安装位置 ===
const hasLocation = computed(() => {
  return device.value?.latitude != null && device.value?.longitude != null
})

const locationText = computed(() => {
  if (!hasLocation.value) return device.value?.installLocation || '-'
  const lat = Number(device.value!.latitude!).toFixed(6)
  const lng = Number(device.value!.longitude!).toFixed(6)
  return `${lng}, ${lat}`
})

function showLocation() {
  if (!hasLocation.value) return
  const gc = wgs84ToGcj02(device.value!.latitude!, device.value!.longitude!)
  uni.openLocation({
    latitude: gc.latitude,
    longitude: gc.longitude,
    name: device.value?.deviceName || '设备位置',
    address: device.value?.installLocation || '',
  })
}

// === 跳转 chart 页面 ===
function goToChart() {
  const hazardId = device.value?.boundHazardPointId
  if (!hazardId) {
    uni.showToast({ title: '该设备未绑定隐患点，无法查看趋势数据', icon: 'none' })
    return
  }
  uni.navigateTo({ url: `/pages/chart?deviceId=${deviceId.value}&hazardPointId=${hazardId}` })
}

// === 工具函数 ===
function formatTimestamp(ts: number): string {
  const d = new Date(ts)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function formatValue(val: number | null | undefined): string {
  if (val === null || val === undefined) return '-'
  return Number.isInteger(val) ? String(val) : val.toFixed(3)
}

function hexToRgba(hex: string, alpha: number) {
  const r = parseInt(hex.slice(1, 3), 16)
  const g = parseInt(hex.slice(3, 5), 16)
  const b = parseInt(hex.slice(5, 7), 16)
  return `rgba(${r}, ${g}, ${b}, ${alpha})`
}

function getTypeColor(type: string): string {
  const colorMap: Record<string, string> = {
    GNSS: '#3068e4',
    雨量计: '#1890ff',
    测斜仪: '#722ed1',
    裂缝计: '#fa8c16',
    水位计: '#13c2c2',
  }
  return colorMap[type] || '#3068e4'
}

function getStatusClass(status: string): string {
  switch (status) {
    case '正常': return 'online'
    case '维修': return 'fault'
    case '停用': return 'offline'
    default: return 'offline'
  }
}

function getOnlineStatusClass(onlineStatus: number | undefined): string {
  return onlineStatus === 1 ? 'online' : 'offline'
}

function getOnlineStatusText(onlineStatus: number | undefined): string {
  return onlineStatus === 1 ? '在线' : '离线'
}

function getDeviceStatusText(status: string | undefined): string {
  return status || '-'
}
</script>

<template>
  <view class="page-container">
    <!-- 头部 -->
    <PageHeader show-back :title="device?.deviceName || '设备详情'" />

    <!-- 可滚动内容 -->
    <scroll-view class="page-body" scroll-y :scroll-top="scrollTop" @scroll="onScroll">
      <!-- 状态卡片 -->
      <view class="section">
        <view class="status-card">
          <view class="status-main">
            <view class="status-big-badge" :class="getOnlineStatusClass(device?.onlineStatus)">
              <view class="status-big-dot" :class="getOnlineStatusClass(device?.onlineStatus)" />
              <text class="status-big-text">{{ getOnlineStatusText(device?.onlineStatus) }}</text>
            </view>
            <view class="status-info">
              <text class="status-label">连接状态</text>
              <text class="status-time">{{ device?.lastReportTime ? `最近上报: ${device.lastReportTime}` : '暂无上报记录' }}</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 配置信息卡片 -->
      <view class="section">
        <text class="section-title">设备信息</text>
        <view class="info-card">
          <view class="info-row">
            <text class="info-label">设备编号</text>
            <text class="info-value">{{ device?.deviceCode || '-' }}</text>
          </view>
          <view v-if="device?.boundHazardPointName" class="info-row">
            <text class="info-label">所属隐患点</text>
            <text class="info-value">{{ device.boundHazardPointName }}</text>
          </view>
          <view
            class="info-row"
            :class="{ clickable: hasLocation }"
            @click="showLocation"
          >
            <text class="info-label">安装位置</text>
            <text class="info-value" :class="{ 'link-text': hasLocation }">
              {{ locationText }}
              <text v-if="hasLocation" class="link-arrow">></text>
            </text>
          </view>
          <view class="info-row">
            <text class="info-label">设备状态</text>
            <view class="device-status-row">
              <view class="device-status-dot" :class="getStatusClass(device?.status || '')" />
              <text class="device-status-text">{{ getDeviceStatusText(device?.status) }}</text>
            </view>
          </view>
          <view class="info-row">
            <text class="info-label">创建时间</text>
            <text class="info-value">{{ device?.createTime || '-' }}</text>
          </view>
        </view>
      </view>

      <!-- 安装传感器（按传感器分组） -->
      <view class="section">
        <text class="section-title">安装传感器</text>

        <view v-if="sensorGroups.length > 0" class="sensor-groups">
          <view
            v-for="group in sensorGroups"
            :key="group.sensor.id"
            class="sensor-group"
          >
            <!-- 传感器标题 -->
            <view class="sensor-header">
              <view class="sensor-title-row">
                <text class="sensor-name">{{ group.sensor.sensorName || '传感器' }}</text>
                <text v-if="group.sensor.sensorNo" class="sensor-code-text">编号: {{ group.sensor.sensorNo }}</text>
              </view>
              <view v-if="group.sensor.monitorTypeName" class="sensor-type-tag">
                <text class="sensor-type-text">{{ group.sensor.monitorTypeName }}</text>
              </view>
            </view>

            <!-- 属性列表 -->
            <view class="attr-list">
              <template v-for="attr in group.attrs" :key="`${group.sensor.id}-${attr.attrCode}`">
                <!-- 属性卡片 -->
                <view
                  class="attr-card"
                  :class="{ expanded: expandedKey === `${group.sensor.id}-${attr.attrCode}` }"
                  @click="toggleAttr(group, attr)"
                >
                  <view class="attr-info">
                    <text class="attr-name">{{ attr.attrName }}</text>
                    <text v-if="attr.latestTime" class="attr-time">{{ attr.latestTime }}</text>
                  </view>
                  <view class="attr-value-wrap">
                    <text class="attr-value" :class="{ placeholder: attr.latestValue === null || attr.latestValue === undefined }">
                      {{ formatValue(attr.latestValue) }}
                    </text>
                    <text class="attr-unit">{{ attr.unit || '' }}</text>
                    <text class="attr-arrow" :class="{ rotated: expandedKey === `${group.sensor.id}-${attr.attrCode}` }">›</text>
                  </view>
                </view>

                <!-- 内联图表区域 -->
                <view
                  v-if="expandedKey === `${group.sensor.id}-${attr.attrCode}`"
                  class="inline-chart-area"
                >
                  <!-- 时间 Tab -->
                  <view class="inline-tabs">
                    <view
                      class="inline-tab"
                      :class="{ active: inlineTimeTab === '24h' }"
                      @click.stop="switchInlineTime('24h')"
                    >24小时</view>
                    <view
                      class="inline-tab"
                      :class="{ active: inlineTimeTab === '7d' }"
                      @click.stop="switchInlineTime('7d')"
                    >7天</view>
                  </view>

                  <!-- 图表 / 提示 -->
                  <view v-if="!device?.boundHazardPointId" class="inline-empty">
                    <text class="inline-empty-text">该设备未绑定隐患点，无法查看趋势图</text>
                  </view>
                  <view v-else-if="inlineLoading" class="inline-loading">
                    <text>加载中...</text>
                  </view>
                  <view v-else-if="!inlineChartOption" class="inline-empty">
                    <text class="inline-empty-text">暂无数据</text>
                  </view>
                  <view v-else class="inline-chart-container">
                    <EchartsComponent
                      :key="`inline-${inlineChartVersion}`"
                      :onInit="initInlineChart"
                      :canvasId="`inline-chart-${inlineChartVersion}`"
                      width="100%"
                      height="400rpx"
                    />
                  </view>
                </view>
              </template>
            </view>
          </view>
        </view>

        <view v-else-if="!loading" class="empty-attrs">
          <text class="empty-attrs-text">暂无监测参数</text>
        </view>
      </view>

      <!-- 查看监测数据按钮 -->
      <view class="section">
        <view class="action-btn" @click="goToChart">
          <text class="action-btn-text">查看监测数据</text>
        </view>
      </view>

      <!-- 底部留白 -->
      <view class="bottom-spacer" />
    </scroll-view>

    <!-- 回到顶部 -->
    <view v-if="showBackTop" class="back-top-btn" @click="scrollToTop">
      <zui-svg-icon icon="up" :width="20" color="#ffffff" />
    </view>
  </view>
</template>

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

/* 内容滚动区域 */
.page-body {
  flex: 1;
  height: 0;
}

.section {
  margin: 0 32rpx 24rpx;
  box-sizing: border-box;
}

.section-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #1a1a2e;
  margin-bottom: 16rpx;
  display: block;
}

/* 状态卡片 */
.status-card {
  background: #ffffff;
  border-radius: 24rpx;
  padding: 32rpx 24rpx;
  box-shadow: 0 8rpx 32rpx rgba(102, 126, 234, 0.12);
}

.status-main {
  display: flex;
  align-items: center;
  gap: 24rpx;
}

.status-big-badge {
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 16rpx 28rpx;
  border-radius: 16rpx;

  &.online { background: rgba(82, 196, 26, 0.1); }
  &.offline { background: rgba(156, 163, 175, 0.15); }
  &.fault { background: rgba(245, 34, 45, 0.1); }
}

.status-big-dot {
  width: 20rpx;
  height: 20rpx;
  border-radius: 50%;

  &.online { background: #52c41a; box-shadow: 0 0 12rpx rgba(82, 196, 26, 0.5); }
  &.offline { background: #9ca3af; }
  &.fault { background: #f5222d; box-shadow: 0 0 12rpx rgba(245, 34, 45, 0.5); }
}

.status-big-text {
  font-size: 30rpx;
  font-weight: 700;

  .status-big-badge.online & { color: #52c41a; }
  .status-big-badge.offline & { color: #9ca3af; }
  .status-big-badge.fault & { color: #f5222d; }
}

.status-info {
  display: flex;
  flex-direction: column;
  gap: 6rpx;
}

.status-label {
  font-size: 24rpx;
  color: #9ca3af;
}

.status-time {
  font-size: 24rpx;
  color: #6b7280;
}

/* 信息卡片 */
.info-card {
  background: #ffffff;
  border-radius: 24rpx;
  padding: 24rpx;
  box-shadow: 0 8rpx 32rpx rgba(102, 126, 234, 0.12);
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.info-label {
  font-size: 26rpx;
  color: #6b7280;
}

.info-value {
  font-size: 26rpx;
  color: #1a1a2e;
  font-weight: 500;

  &.link-text {
    color: #3068e4;
  }
}

.link-arrow {
  font-size: 26rpx;
  color: #3068e4;
  margin-left: 4rpx;
}

.info-row.clickable {
  cursor: pointer;

  &:active {
    background: #f7f8fc;
    border-radius: 8rpx;
  }
}

.device-status-row {
  display: flex;
  align-items: center;
  gap: 8rpx;
}

.device-status-dot {
  width: 14rpx;
  height: 14rpx;
  border-radius: 50%;

  &.online { background: #52c41a; }
  &.offline { background: #9ca3af; }
  &.fault { background: #f5222d; }
}

.device-status-text {
  font-size: 26rpx;
  color: #1a1a2e;
  font-weight: 500;
}

/* 传感器分组 */
.sensor-groups {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.sensor-group {
  display: flex;
  flex-direction: column;
}

.sensor-header {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 16rpx;
  padding-left: 4rpx;
}

.sensor-title-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
  flex: 1;
}

.sensor-code-text {
  font-size: 22rpx;
  color: #9ca3af;
}

.sensor-name {
  font-size: 28rpx;
  font-weight: 600;
  color: #1a1a2e;
}

.sensor-type-tag {
  padding: 4rpx 12rpx;
  background: rgba(48, 104, 228, 0.1);
  border-radius: 8rpx;
}

.sensor-type-text {
  font-size: 20rpx;
  color: #3068e4;
}

/* 属性列表 */
.attr-list {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}

.attr-card {
  background: #ffffff;
  border-radius: 20rpx;
  padding: 24rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 4rpx 16rpx rgba(102, 126, 234, 0.08);
  transition: all 0.2s;

  &.expanded {
    border-bottom-left-radius: 0;
    border-bottom-right-radius: 0;
    box-shadow: 0 4rpx 16rpx rgba(102, 126, 234, 0.12);
  }

  &:active {
    background: #f7f8fc;
  }
}

.attr-info {
  display: flex;
  flex-direction: column;
  gap: 4rpx;
  flex: 1;
  min-width: 0;
}

.attr-name {
  font-size: 28rpx;
  color: #4b5563;
  font-weight: 500;
}

.attr-time {
  font-size: 22rpx;
  color: #9ca3af;
}

.attr-value-wrap {
  display: flex;
  align-items: baseline;
  gap: 6rpx;
  flex-shrink: 0;
}

.attr-value {
  font-size: 36rpx;
  font-weight: 700;
  color: #3068e4;

  &.placeholder {
    color: #d1d5db;
    font-weight: 400;
  }
}

.attr-unit {
  font-size: 22rpx;
  color: #9ca3af;
}

.attr-arrow {
  font-size: 32rpx;
  color: #d1d5db;
  transition: transform 0.2s;
  margin-left: 8rpx;

  &.rotated {
    transform: rotate(90deg);
    color: #3068e4;
  }
}

/* 内联图表区域 */
.inline-chart-area {
  background: #ffffff;
  border-radius: 0 0 20rpx 20rpx;
  padding: 16rpx 16rpx 20rpx;
  box-shadow: 0 4rpx 16rpx rgba(102, 126, 234, 0.08);
  margin-top: -12rpx;
}

.inline-tabs {
  display: flex;
  gap: 12rpx;
  margin-bottom: 12rpx;
}

.inline-tab {
  padding: 8rpx 20rpx;
  border-radius: 20rpx;
  font-size: 22rpx;
  color: #6b7280;
  background: #f7f8fc;

  &.active {
    background: linear-gradient(135deg, #3068e4 0%, #1e5acc 100%);
    color: #ffffff;
  }
}

.inline-chart-container {
  width: 100%;
  height: 400rpx;
}

.inline-loading {
  height: 400rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #9ca3af;
  font-size: 26rpx;
}

.inline-empty {
  height: 300rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.inline-empty-text {
  font-size: 24rpx;
  color: #9ca3af;
}

.empty-attrs {
  background: #ffffff;
  border-radius: 24rpx;
  padding: 60rpx 24rpx;
  text-align: center;
  box-shadow: 0 8rpx 32rpx rgba(102, 126, 234, 0.12);
}

.empty-attrs-text {
  font-size: 26rpx;
  color: #9ca3af;
}

/* 操作按钮 */
.action-btn {
  padding: 28rpx;
  background: linear-gradient(135deg, #3068e4 0%, #1e5acc 100%);
  border-radius: 24rpx;
  text-align: center;
  box-shadow: 0 8rpx 32rpx rgba(102, 126, 234, 0.3);
  box-sizing: border-box;

  &:active { opacity: 0.9; }
}

.action-btn-text {
  font-size: 30rpx;
  font-weight: 600;
  color: #ffffff;
}

.bottom-spacer {
  height: 32rpx;
}

/* 回到顶部 */
.back-top-btn {
  position: fixed;
  right: 32rpx;
  bottom: 200rpx;
  width: 88rpx;
  height: 88rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #3068e4 0%, #1e5acc 100%);
  box-shadow: 0 8rpx 24rpx rgba(48, 104, 228, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10;

  &:active {
    transform: scale(0.9);
    opacity: 0.85;
  }
}
</style>
