<template>
  <div class="comprehensive-view">
    <div class="left-panel">
      <div class="panel-section health-section">
        <div class="section-header">
          <span class="section-title">系统健康度</span>
          <span ref="healthTriggerRef" class="health-question" @mouseenter="showPopover" @mouseleave="hidePopover">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                 stroke-width="2" width="16" height="16">
              <circle cx="12" cy="12" r="10"/>
              <path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"/>
              <line x1="12" y1="17" x2="12.01" y2="17"/>
            </svg>
          </span>
          <Teleport to="body">
            <div
              class="algorithm-popover"
              v-if="showAlgorithmDesc"
              :style="popoverStyle"
              @mouseenter="cancelHide"
              @mouseleave="hidePopover"
            >
              <div class="popover-arrow"></div>
              <p>系统健康度综合评估以下五个维度：</p>
              <ul>
                <li><strong>资料完善率</strong>：设备资料登记率与隐患点资料完善率的综合指标</li>
                <li><strong>设备在线率</strong>：在线设备数/隐患点关联设备总数 × 100%</li>
                <li><strong>设备正常率</strong>：状态正常设备数/设备总数 × 100%</li>
                <li><strong>告警及时响应率</strong>：首次告警1小时内响应的事件数/告警事件总数 × 100%</li>
                <li><strong>边坡稳定率</strong>：最近一个月未有效告警隐患点数/总隐患点数 × 100%</li>
              </ul>
              <p style="margin-top: 8px;">综合得分 = 各维度得分 × 权重之和（环形图分色展示各维度占比）</p>
            </div>
          </Teleport>
        </div>
        <div class="health-content">
          <div class="health-ring-container">
            <svg class="health-ring" viewBox="0 0 200 200">
              <circle class="ring-bg" cx="100" cy="100" r="85"/>
              <circle
                  v-for="(segment, index) in ringSegments"
                  :key="index"
                  class="ring-segment"
                  :class="['segment-' + (index + 1), { active: activeSegment === index }]"
                  cx="100"
                  cy="100"
                  r="85"
                  :stroke="segment.color"
                  :stroke-dasharray="segment.dashArray"
                  :stroke-dashoffset="segment.dashOffset"
                  :style="{ transform: 'rotate(' + segment.rotate + 'deg)', transformOrigin: 'center' }"
                  @mouseenter="activeSegment = index"
                  @mouseleave="activeSegment = null"
              />
            </svg>
            <div class="ring-center">
              <div class="ring-score">{{ healthStats.overallScore }}%</div>
              <div class="ring-label">综合健康度</div>
            </div>
          </div>
          <div class="health-bars">
            <div
                class="health-bar-item"
                v-for="(item, index) in healthStats.items"
                :key="item.name"
                :class="{ active: activeSegment === index }"
                @mouseenter="activeSegment = index"
                @mouseleave="activeSegment = null"
                @click="activeSegment = activeSegment === index ? null : index"
            >
              <div class="bar-info">
                <span class="bar-name">{{ item.name }}</span>
                <span class="bar-value" :style="{ color: item.color }">{{ item.value }}%</span>
              </div>
              <div class="bar-track">
                <div
                    class="bar-progress"
                    :style="{
                    width: item.value + '%',
                    backgroundColor: item.color
                  }"
                ></div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="panel-section resource-section">
        <div class="section-header">
          <span class="section-title">资源统计</span>
        </div>
        <div class="resource-compact">
          <div class="resource-main">
            <div class="resource-total">
              <div class="total-circle">
                <svg class="total-ring" viewBox="0 0 100 100">
                  <circle class="ring-bg" cx="50" cy="50" r="45"/>
                  <circle class="ring-hazard" cx="50" cy="50" r="45" :stroke-dasharray="`113 170`"
                          stroke-dashoffset="0"/>
                  <circle class="ring-device" cx="50" cy="50" r="45" :stroke-dasharray="`142 141`"
                          stroke-dashoffset="-113"/>
                </svg>
                <div class="total-value">{{ resourceStats.totalResources }}</div>
              </div>
              <div class="total-label">资源总数</div>
            </div>
            <div class="resource-breakdown">
              <div class="breakdown-item hazard">
                <div class="breakdown-icon">
                  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="#faad14"
                       stroke-width="2" width="16" height="16">
                    <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"/>
                  </svg>
                </div>
                <div class="breakdown-info">
                  <span class="breakdown-value">{{ resourceStats.hazardTotal }}</span>
                  <span class="breakdown-label">隐患点</span>
                </div>
              </div>
              <div class="breakdown-item device">
                <div class="breakdown-icon">
                  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="#52c41a"
                       stroke-width="2" width="16" height="16">
                    <circle cx="12" cy="12" r="3"/>
                    <path
                        d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"/>
                  </svg>
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
    </div>

    <div class="center-panel">
      <div class="charts-container">
        <div class="stats-bar">
          <div class="stat-item">
            <div class="stat-icon-wrapper alarm">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                   stroke-width="2" width="24" height="24">
                <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/>
                <path d="M13.73 21a2 2 0 0 1-3.46 0"/>
              </svg>
            </div>
            <div class="stat-content">
              <div class="stat-value-wrapper">
                <span class="stat-value" :class="{ animate: animateStats }">{{ summaryStats.recentThreeMonthsAlarms }}</span>
              </div>
              <div class="stat-info">
                <span class="stat-label">近三月告警次数</span>
                <span class="stat-desc">需及时处理</span>
              </div>
            </div>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <div class="stat-icon-wrapper warning">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                   stroke-width="2" width="24" height="24">
                <circle cx="12" cy="12" r="10"/>
                <line x1="12" y1="8" x2="12" y2="12"/>
                <line x1="12" y1="16" x2="12.01" y2="16"/>
              </svg>
            </div>
            <div class="stat-content">
              <div class="stat-value-wrapper">
                <span class="stat-value highlight" :class="{ animate: animateStats }">{{ summaryStats.totalAlarms }}</span>
              </div>
              <div class="stat-info">
                <span class="stat-label">累计告警次数</span>
                <span class="stat-desc">历史记录</span>
              </div>
            </div>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <div class="stat-icon-wrapper monitor">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                   stroke-width="2" width="24" height="24">
                <polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/>
              </svg>
            </div>
            <div class="stat-content">
              <div class="stat-value-wrapper">
                <span class="stat-value" :class="{ animate: animateStats }">{{ summaryStats.totalMonitorCount }}</span>
              </div>
              <div class="stat-info">
                <span class="stat-label">累计监测次数</span>
                <span class="stat-desc">数据汇总</span>
              </div>
            </div>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <div class="stat-icon-wrapper hazard">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                   stroke-width="2" width="24" height="24">
                <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
              </svg>
            </div>
            <div class="stat-content">
              <div class="stat-value-wrapper">
                <span class="stat-value" :class="{ animate: animateStats }">{{ summaryStats.hazardPointCount }}</span>
              </div>
              <div class="stat-info">
                <span class="stat-label">隐患点数量</span>
                <span class="stat-desc">监测区域</span>
              </div>
            </div>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <div class="stat-icon-wrapper device">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                   stroke-width="2" width="24" height="24">
                <rect x="2" y="3" width="20" height="14" rx="2" ry="2"/>
                <line x1="8" y1="21" x2="16" y2="21"/>
                <line x1="12" y1="17" x2="12" y2="21"/>
              </svg>
            </div>
            <div class="stat-content">
              <div class="stat-value-wrapper">
                <span class="stat-value" :class="{ animate: animateStats }">{{ summaryStats.deviceCount }}</span>
              </div>
              <div class="stat-info">
                <span class="stat-label">设备数量</span>
                <span class="stat-desc">在线部署</span>
              </div>
            </div>
          </div>
        </div>

        <div class="analysis-charts">
          <div class="chart-panel">
            <div class="panel-header">
              <span class="panel-title">隐患点增长趋势分析</span>
              <span class="panel-subtitle">近12个月新增隐患点统计</span>
            </div>
            <div class="chart-body">
              <div ref="hazardTrendChart" class="echarts-container"></div>
            </div>
          </div>

          <div class="chart-panel">
            <div class="panel-header">
              <span class="panel-title">告警趋势分析</span>
              <span class="panel-subtitle">近12个月告警统计及未来预测</span>
            </div>
            <div class="chart-body">
              <div ref="alarmTrendChart" class="echarts-container"></div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="right-panel">
      <div class="panel-section online-section">
        <div class="section-header">
          <span class="section-title">设备在线状态</span>
        </div>
        <div class="online-overview">
          <div class="online-rate-section">
            <div class="online-rate">
              <span class="rate-value">{{ onlineStats.onlineRate }}</span>
              <span class="rate-unit">%</span>
            </div>
            <div class="online-text">设备在线率</div>
          </div>
          <div class="online-count-section">
            <div class="online-numbers">
              <span class="online-count">{{ onlineStats.onlineCount }}</span>
              <span class="online-separator">/</span>
              <span class="total-count">{{ onlineStats.totalCount }}</span>
            </div>
            <div class="online-label">台设备在线</div>
          </div>
        </div>
        <div class="online-chart">
          <div class="chart-title">分类型在线统计</div>
          <div class="type-bars">
            <div v-for="type in onlineStats.typeStats" :key="type.name" class="type-bar-item">
              <div class="bar-label">{{ type.name }}</div>
              <div class="bar-container">
                <div class="bar-fill" :style="{ width: (type.online / type.total * 100) + '%' }"></div>
              </div>
              <div class="bar-count">{{ type.online }}/{{ type.total }}</div>
            </div>
          </div>
        </div>
        <div class="online-trend">
          <div class="trend-header">
            <span class="trend-title">历史在线趋势</span>
            <span class="trend-subtitle">最近7天</span>
          </div>
          <div class="trend-chart">
            <div class="trend-y-axis">
              <span v-for="label in trendYLabels" :key="label">{{ label }}</span>
            </div>
            <div class="trend-area">
              <svg class="trend-svg" viewBox="0 0 280 100" preserveAspectRatio="none">
                <defs>
                  <linearGradient id="trendGradient" x1="0%" y1="0%" x2="0%" y2="100%">
                    <stop offset="0%" stop-color="rgba(82, 196, 26, 0.4)"/>
                    <stop offset="100%" stop-color="rgba(82, 196, 26, 0)"/>
                  </linearGradient>
                </defs>
                <path :d="trendAreaPath" fill="url(#trendGradient)"/>
                <path :d="trendLinePath" fill="none" stroke="#52c41a" stroke-width="2"/>
                <circle
                    v-for="(point, index) in trendDataPoints"
                    :key="index"
                    :cx="point.x"
                    :cy="point.y"
                    r="4"
                    fill="#52c41a"
                    stroke="#fff"
                    stroke-width="2"
                />
              </svg>
              <div class="trend-x-axis">
                <span v-for="label in trendXLabels" :key="label">{{ label }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="panel-section alarm-section">
        <div class="section-header">
          <span class="section-title">告警态势</span>
        </div>
        <div class="alarm-summary">
          <div class="alarm-summary-item">
            <div class="summary-badge pending">待办告警</div>
            <div class="summary-count">{{ alarmStats.pendingCount }}</div>
          </div>
          <div class="alarm-summary-item">
            <div class="summary-badge history">历史告警</div>
            <div class="summary-count">{{ alarmStats.historyCount }}</div>
          </div>
        </div>
        <div class="alarm-level-stats">
          <div class="level-stat" v-for="level in alarmStats.levelStats" :key="level.name">
            <div class="level-dot" :class="level.key"></div>
            <span class="level-name">{{ level.name }}</span>
            <span class="level-count">{{ level.count }}</span>
          </div>
        </div>
        <div class="alarm-list-section">
          <div class="list-header">
            <span class="list-title">实时告警事件</span>
          </div>
          <div class="alarm-list">
            <div v-for="alarm in alarmStats.recentAlarms" :key="alarm.id" class="alarm-item">
              <div class="alarm-level-dot" :class="alarm.level"></div>
              <div class="alarm-content">
                <div class="alarm-title">{{ alarm.title }}</div>
                <div class="alarm-meta">{{ alarm.source }} · {{ alarm.time }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {computed, onMounted, onUnmounted, ref} from 'vue'
import * as echarts from 'echarts'
import L from 'leaflet'

const TIANDITU_KEY = '8dda07d4649c77efd0537a0ff0a1df13'

interface DeviceInfo {
  name: string
  status: 'online' | 'offline' | 'warning'
}

interface HazardPoint {
  id: number
  name: string
  code: string
  type: string
  location: string
  level: string
  x: number
  y: number
  hasAlarm: boolean
  displacement: number
  rainfall: number
  waterLevel: number
  inclination: number
  deviceCount: number
  devices: DeviceInfo[]
}

interface ChartPoint {
  x: number
  y: number
}

const showAlgorithmDesc = ref(false)
const healthTriggerRef = ref<HTMLElement | null>(null)
const popoverStyle = ref<Record<string, string>>({})
let hideTimer: ReturnType<typeof setTimeout> | null = null

const showPopover = () => {
  if (hideTimer) { clearTimeout(hideTimer); hideTimer = null }
  if (healthTriggerRef.value) {
    const rect = healthTriggerRef.value.getBoundingClientRect()
    popoverStyle.value = {
      position: 'fixed',
      top: rect.bottom + 8 + 'px',
      left: Math.min(rect.left, window.innerWidth - 320) + 'px',
    }
  }
  showAlgorithmDesc.value = true
}

const hidePopover = () => {
  hideTimer = setTimeout(() => {
    showAlgorithmDesc.value = false
  }, 150)
}

const cancelHide = () => {
  if (hideTimer) { clearTimeout(hideTimer); hideTimer = null }
}
const animateStats = ref(false)
const activeSegment = ref<number | null>(null)
const dataFilter = ref('all')
const selectedPoint = ref<HazardPoint | null>(null)

const hazardTrendChart = ref<HTMLDivElement | null>(null)
const alarmTrendChart = ref<HTMLDivElement | null>(null)
let hazardTrendChartInstance: echarts.ECharts | null = null
let alarmTrendChartInstance: echarts.ECharts | null = null

const summaryStats = ref({
  recentThreeMonthsAlarms: 156,
  totalAlarms: 892,
  totalMonitorCount: 125680,
  hazardPointCount: 28,
  deviceCount: 156
})

interface SensorInfo {
  id: string
  name: string
  type: string
}

const hazardTrendData = ref({
  months: ['2025-06', '2025-07', '2025-08', '2025-09', '2025-10', '2025-11', '2025-12', '2026-01', '2026-02', '2026-03', '2026-04', '2026-05'],
  values: [2, 3, 5, 4, 6, 3, 4, 5, 3, 4, 2, 3]
})

const alarmTrendData = ref({
  months: ['2025-06', '2025-07', '2025-08', '2025-09', '2025-10', '2025-11', '2025-12', '2026-01', '2026-02', '2026-03', '2026-04', '2026-05'],
  level1: [5, 8, 12, 10, 15, 12, 14, 16, 12, 14, 10, 12],
  level2: [8, 12, 15, 14, 18, 15, 17, 20, 16, 18, 14, 15],
  level3: [3, 5, 7, 6, 8, 6, 7, 8, 6, 7, 5, 6],
  level4: [2, 3, 4, 3, 5, 4, 4, 5, 4, 4, 3, 4],
  total: [18, 28, 38, 33, 46, 37, 42, 49, 38, 43, 32, 37],
  forecastTotal: [35, 38],
  forecastLevel1: [11, 12],
  forecastLevel2: [16, 17],
  forecastLevel3: [6, 6],
  forecastLevel4: [4, 4]
})

const monitorChartLinePath = computed(() => {
  const points = monitorDataPoints.value
  if (points.length === 0) return ''
  return points.map((p: ChartPoint, i: number) => `${i === 0 ? 'M' : 'L'} ${p.x} ${p.y}`).join(' ')
})

const monitorChartAreaPath = computed(() => {
  const linePath = monitorChartLinePath.value
  if (!linePath) return ''
  const points = monitorDataPoints.value
  const lastX = points[points.length - 1]?.x || 500
  return `${linePath} L ${lastX} 180 L 25 180 Z`
})

const currentChartYLabels = computed(() => ['30', '25', '20', '15', '10', '5', '0'])

const currentChartXLabels = computed(() => ['0时', '4时', '8时', '12时', '16时', '20时'])

const monitorDataTable = ref([
  {
    time: '2024-01-15 00:00:00',
    deviceName: 'GNSS接收机-A1',
    sensorName: '节点1',
    value: '12.3',
    unit: 'mm',
    direction: 'X'
  },
  {
    time: '2024-01-15 04:00:00',
    deviceName: 'GNSS接收机-A1',
    sensorName: '节点1',
    value: '15.1',
    unit: 'mm',
    direction: 'X'
  },
  {
    time: '2024-01-15 08:00:00',
    deviceName: 'GNSS接收机-A1',
    sensorName: '节点1',
    value: '18.2',
    unit: 'mm',
    direction: 'X'
  },
  {
    time: '2024-01-15 12:00:00',
    deviceName: 'GNSS接收机-A1',
    sensorName: '节点1',
    value: '22.0',
    unit: 'mm',
    direction: 'X'
  },
  {
    time: '2024-01-15 16:00:00',
    deviceName: 'GNSS接收机-A1',
    sensorName: '节点1',
    value: '19.5',
    unit: 'mm',
    direction: 'X'
  },
  {
    time: '2024-01-15 20:00:00',
    deviceName: 'GNSS接收机-A1',
    sensorName: '节点1',
    value: '16.8',
    unit: 'mm',
    direction: 'X'
  }
])

const monitorDataPoints = computed<ChartPoint[]>(() => {
  const values = monitorDataTable.value.map(item => Number(item.value))
  const maxValue = Math.max(...values, 30)
  const chartWidth = 475
  const chartHeight = 180
  const startX = 25
  const stepX = values.length > 1 ? chartWidth / (values.length - 1) : 0

  return values.map((value, index) => ({
    x: startX + index * stepX,
    y: chartHeight - (value / maxValue) * chartHeight
  }))
})

const handleQueryData = () => {
  console.log('查询监测数据:', dataFilter.value)
}
const mapContainer = ref<HTMLElement | null>(null)
let map: L.Map | null = null

const initMap = () => {
  if (!mapContainer.value) return

  map = L.map(mapContainer.value, {
    center: [30.65, 104.10],
    zoom: 12,
    zoomControl: false
  })

  L.tileLayer(`https://t0.tianditu.gov.cn/img_w/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=img&STYLE=default&TILEMATRIXSET=w&FORMAT=tiles&TILEMATRIX={z}&TILEROW={y}&TILECOL={x}&tk=${TIANDITU_KEY}`, {
    maxZoom: 18,
    minZoom: 1
  }).addTo(map)

  L.control.zoom({position: 'bottomright'}).addTo(map)
  const leafletMap = map

  hazardPoints.value.forEach(point => {
    const color = point.hasAlarm ? '#f5222d' : '#52c41a'

    const iconHtml = `
      <div class="hazard-marker-container" style="position: relative;">
        <div class="hazard-icon" style="width: 24px; height: 24px; border-radius: 50%; background: ${color}; border: 2px solid #fff; display: flex; align-items: center; justify-content: center; box-shadow: 0 2px 8px rgba(0,0,0,0.3);">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="white" width="14" height="14">
            <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"/>
          </svg>
        </div>
        ${point.hasAlarm ? '<div class="alarm-ring"></div>' : ''}
        <div class="device-count-badge" style="position: absolute; top: -8px; right: -8px; background: #1890ff; color: #fff; border-radius: 50%; width: 20px; height: 20px; display: flex; align-items: center; justify-content: center; font-size: 11px; font-weight: bold; border: 2px solid #fff; box-shadow: 0 2px 4px rgba(0,0,0,0.2);">
          ${point.deviceCount}
        </div>
      </div>
    `

    const customIcon = L.divIcon({
      html: iconHtml,
      className: 'custom-hazard-marker',
      iconSize: [32, 32],
      iconAnchor: [16, 16]
    })

    const marker = L.marker([point.y / 100 * 0.1 + 30.55, point.x / 100 * 0.2 + 104.0], {
      icon: customIcon
    }).addTo(leafletMap)

    const statusText = point.hasAlarm ? '待办告警' : '正常监测'
    const devicesHtml = point.devices.map(device => {
      const statusColor = device.status === 'online' ? '#52c41a' : device.status === 'warning' ? '#faad14' : '#f5222d'
      const statusText = device.status === 'online' ? '在线' : device.status === 'warning' ? '异常' : '离线'
      return `
        <div class="device-item">
          <span class="device-name">${device.name}</span>
          <span class="device-status" style="color: ${statusColor};">${statusText}</span>
        </div>
      `
    }).join('')

    const popupContent = `
      <div class="hazard-popup">
        <div class="popup-header">
          <span class="popup-title">${point.name}</span>
          <span class="popup-status" style="color: ${color};">${statusText}</span>
        </div>
        <div class="popup-device-info">
          <span class="device-count-label">设备总数：</span>
          <span class="device-count-value">${point.deviceCount}台（含视频设备）</span>
        </div>
        <div class="popup-device-list">
          <div class="list-header">设备清单</div>
          ${devicesHtml}
        </div>
      </div>
    `

    marker.bindPopup(popupContent, {
      maxWidth: 280,
      className: 'hazard-popup-container'
    })

    marker.on('click', () => {
      selectedPoint.value = point
    })
  })
}

const handleHazardClick = (point: HazardPoint) => {
  selectedPoint.value = point
  if (map) {
    map.flyTo([point.y / 100 * 0.1 + 30.55, point.x / 100 * 0.2 + 104.0], 14)
  }
}

const zoomIn = () => {
  if (map) map.zoomIn()
}

const zoomOut = () => {
  if (map) map.zoomOut()
}

onMounted(() => {
  initHazardTrendChart()
  initAlarmTrendChart()
  setTimeout(() => {
    animateStats.value = true
  }, 500)
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  if (hazardTrendChartInstance) {
    hazardTrendChartInstance.dispose()
    hazardTrendChartInstance = null
  }
  if (alarmTrendChartInstance) {
    alarmTrendChartInstance.dispose()
    alarmTrendChartInstance = null
  }
  window.removeEventListener('resize', handleResize)
})

const handleResize = () => {
  if (hazardTrendChartInstance) {
    hazardTrendChartInstance.resize()
  }
  if (alarmTrendChartInstance) {
    alarmTrendChartInstance.resize()
  }
}

const initHazardTrendChart = () => {
  if (!hazardTrendChart.value) return

  hazardTrendChartInstance = echarts.init(hazardTrendChart.value)

  const option = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#e2e8f0',
      borderWidth: 1,
      textStyle: {
        color: '#374151'
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: hazardTrendData.value.months,
      axisLine: {
        lineStyle: {
          color: '#e2e8f0'
        }
      },
      axisLabel: {
        color: '#6b7280',
        fontSize: 14
      }
    },
    yAxis: {
      type: 'value',
      name: '新增隐患点',
      nameTextStyle: {
        color: '#6b7280',
        fontSize: 14
      },
      axisLine: {
        show: false
      },
      axisTick: {
        show: false
      },
      axisLabel: {
        color: '#6b7280',
        fontSize: 14
      },
      splitLine: {
        lineStyle: {
          color: '#f3f4f6'
        }
      }
    },
    series: [
      {
        name: '新增隐患点',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 8,
        lineStyle: {
          width: 3,
          color: '#6366f1'
        },
        itemStyle: {
          color: '#6366f1',
          borderColor: '#fff',
          borderWidth: 2
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            {offset: 0, color: 'rgba(99, 102, 241, 0.3)'},
            {offset: 1, color: 'rgba(99, 102, 241, 0.05)'}
          ])
        },
        data: hazardTrendData.value.values
      }
    ]
  }

  hazardTrendChartInstance.setOption(option)
}

const initAlarmTrendChart = () => {
  if (!alarmTrendChart.value) return

  alarmTrendChartInstance = echarts.init(alarmTrendChart.value)

  const option = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#e2e8f0',
      borderWidth: 1,
      textStyle: {
        color: '#374151'
      }
    },
    legend: {
      data: ['一级告警', '二级告警', '三级告警', '四级告警', '合计', '预测一级', '预测二级', '预测三级', '预测四级', '预测合计'],
      bottom: 0,
      textStyle: {
        color: '#6b7280',
        fontSize: 12
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '18%',
      top: '5%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: [...alarmTrendData.value.months, '2026-06', '2026-07'],
      axisLine: {
        lineStyle: {
          color: '#e2e8f0'
        }
      },
      axisLabel: {
        color: '#6b7280',
        fontSize: 12
      }
    },
    yAxis: {
      type: 'value',
      name: '告警次数',
      nameTextStyle: {
        color: '#6b7280',
        fontSize: 12
      },
      axisLine: {
        show: false
      },
      axisTick: {
        show: false
      },
      axisLabel: {
        color: '#6b7280',
        fontSize: 12
      },
      splitLine: {
        lineStyle: {
          color: '#f3f4f6'
        }
      }
    },
    series: [
      {
        name: '一级告警',
        type: 'line',
        smooth: true,
        color: '#ef4444',
        data: [...alarmTrendData.value.level1, null, null]
      },
      {
        name: '二级告警',
        type: 'line',
        smooth: true,
        color: '#f97316',
        data: [...alarmTrendData.value.level2, null, null]
      },
      {
        name: '三级告警',
        type: 'line',
        smooth: true,
        color: '#eab308',
        data: [...alarmTrendData.value.level3, null, null]
      },
      {
        name: '四级告警',
        type: 'line',
        smooth: true,
        color: '#22c55e',
        data: [...alarmTrendData.value.level4, null, null]
      },
      {
        name: '合计',
        type: 'line',
        smooth: true,
        color: '#3b82f6',
        lineStyle: {
          width: 3
        },
        data: [...alarmTrendData.value.total, null, null]
      },
      {
        name: '预测一级',
        type: 'line',
        smooth: true,
        color: '#ef4444',
        lineStyle: {
          width: 2,
          type: 'dashed'
        },
        symbol: 'diamond',
        symbolSize: 6,
        data: [null, null, null, null, null, null, null, null, null, null, null, alarmTrendData.value.level1[11], ...alarmTrendData.value.forecastLevel1]
      },
      {
        name: '预测二级',
        type: 'line',
        smooth: true,
        color: '#f97316',
        lineStyle: {
          width: 2,
          type: 'dashed'
        },
        symbol: 'diamond',
        symbolSize: 6,
        data: [null, null, null, null, null, null, null, null, null, null, null, alarmTrendData.value.level2[11], ...alarmTrendData.value.forecastLevel2]
      },
      {
        name: '预测三级',
        type: 'line',
        smooth: true,
        color: '#eab308',
        lineStyle: {
          width: 2,
          type: 'dashed'
        },
        symbol: 'diamond',
        symbolSize: 6,
        data: [null, null, null, null, null, null, null, null, null, null, null, alarmTrendData.value.level3[11], ...alarmTrendData.value.forecastLevel3]
      },
      {
        name: '预测四级',
        type: 'line',
        smooth: true,
        color: '#22c55e',
        lineStyle: {
          width: 2,
          type: 'dashed'
        },
        symbol: 'diamond',
        symbolSize: 6,
        data: [null, null, null, null, null, null, null, null, null, null, null, alarmTrendData.value.level4[11], ...alarmTrendData.value.forecastLevel4]
      },
      {
        name: '预测合计',
        type: 'line',
        smooth: true,
        color: '#3b82f6',
        lineStyle: {
          width: 2,
          type: 'dashed'
        },
        symbol: 'diamond',
        symbolSize: 6,
        data: [null, null, null, null, null, null, null, null, null, null, null, alarmTrendData.value.total[11], ...alarmTrendData.value.forecastTotal]
      }
    ]
  }

  alarmTrendChartInstance.setOption(option)
}

const chartTabs = [
  {key: 'displacement', label: '位移监测'},
  {key: 'rainfall', label: '雨量监测'},
  {key: 'waterLevel', label: '水位监测'},
  {key: 'inclination', label: '倾斜监测'}
]

const healthStats = ref({
  overallScore: 95,
  items: [
    {name: '资料完善率', value: 95, weight: 0.2, color: '#52c41a'},
    {name: '设备在线率', value: 96, weight: 0.15, color: '#1890ff'},
    {name: '设备正常率', value: 94, weight: 0.15, color: '#722ed1'},
    {name: '告警及时响应率', value: 90, weight: 0.2, color: '#fa8c16'},
    {name: '边坡稳定率', value: 97, weight: 0.3, color: '#eb2f96'}
  ]
})

const ringSegments = computed(() => {
  const circumference = 2 * Math.PI * 85
  let currentOffset = 0
  return healthStats.value.items.map((item, index) => {
    const segmentLength = (item.weight * circumference * item.value / 100)
    const gapLength = 4
    const segment = {
      color: item.color,
      dashArray: `${segmentLength} ${circumference - segmentLength}`,
      dashOffset: -currentOffset,
      rotate: (index * 72) - 90
    }
    currentOffset += segmentLength + gapLength
    return segment
  })
})

const getHealthColor = (value: number) => {
  if (value >= 90) return '#52c41a'
  if (value >= 70) return '#faad14'
  return '#f5222d'
}

const resourceStats = ref({
  totalResources: 156,
  deviceTotal: 98,
  hazardTotal: 45,
  deviceTypes: [
    {name: 'GNSS接收机', count: 25},
    {name: '雨量计', count: 18},
    {name: '渗压计', count: 15},
    {name: '位移计', count: 20},
    {name: '视频设备', count: 20}
  ]
})

const onlineStats = ref({
  onlineRate: 91,
  onlineCount: 89,
  totalCount: 98,
  offlineCount: 9,
  typeStats: [
    {name: 'GNSS', online: 24, total: 25},
    {name: '雨量计', online: 17, total: 18},
    {name: '渗压计', online: 14, total: 15},
    {name: '位移计', online: 19, total: 20},
    {name: '视频', online: 15, total: 20}
  ]
})

const alarmStats = ref({
  pendingCount: 8,
  historyCount: 156,
  levelStats: [
    {name: '红色告警', key: 'red', count: 2},
    {name: '橙色告警', key: 'orange', count: 3},
    {name: '黄色告警', key: 'yellow', count: 2},
    {name: '绿色提示', key: 'green', count: 1}
  ],
  recentAlarms: [
    {id: 1, title: '龙潭寺滑坡点位移超过警戒值', source: 'GNSS-A1', time: '14:32', level: 'red'},
    {id: 2, title: '地声异常波动预警', source: '地声传感器', time: '13:45', level: 'orange'},
    {id: 3, title: 'ZZ水库水位接近警戒水位', source: '渗压计-B1', time: '12:20', level: 'orange'},
    {id: 4, title: '设备应变计-B2离线告警', source: '系统监测', time: '11:15', level: 'yellow'},
    {id: 5, title: '日降雨量达到预警阈值', source: '雨量计-A1', time: '10:30', level: 'yellow'}
  ]
})

const hazardPoints = ref<HazardPoint[]>([
  {
    id: 1,
    name: '龙潭寺滑坡点',
    code: 'HZ-001',
    type: '滑坡',
    location: '龙潭寺镇西北侧',
    level: 'high',
    x: 35,
    y: 28,
    hasAlarm: true,
    displacement: 15.6,
    rainfall: 128.5,
    waterLevel: 2.3,
    inclination: 12.5,
    deviceCount: 5,
    devices: [{name: 'GNSS接收机-A1', status: 'online'}, {name: '位移计-B1', status: 'online'}, {
      name: '雨量计-C1',
      status: 'online'
    }, {name: '视频监控-D1', status: 'online'}, {name: '裂缝计-E1', status: 'warning'}]
  },
  {
    id: 2,
    name: '大坝监测点',
    code: 'HZ-002',
    type: '坝体',
    location: 'ZZ水库大坝',
    level: 'medium',
    x: 65,
    y: 45,
    hasAlarm: false,
    displacement: 5.2,
    rainfall: 98.3,
    waterLevel: 15.8,
    inclination: 3.2,
    deviceCount: 4,
    devices: [{name: '渗压计-A2', status: 'online'}, {name: '水位计-B2', status: 'online'}, {
      name: '应变计-C2',
      status: 'online'
    }, {name: '视频监控-D2', status: 'online'}]
  },
  {
    id: 3,
    name: '边坡监测点',
    code: 'HZ-003',
    type: '边坡',
    location: '高速公路K120段',
    level: 'low',
    x: 45,
    y: 68,
    hasAlarm: false,
    displacement: 2.1,
    rainfall: 76.2,
    waterLevel: 0,
    inclination: 1.8,
    deviceCount: 3,
    devices: [{name: 'GNSS接收机-A3', status: 'online'}, {name: '倾角仪-B3', status: 'online'}, {
      name: '视频监控-C3',
      status: 'offline'
    }]
  },
  {
    id: 4,
    name: '泥石流隐患点',
    code: 'HZ-004',
    type: '泥石流',
    location: '山区公路沿线',
    level: 'high',
    x: 78,
    y: 35,
    hasAlarm: true,
    displacement: 28.5,
    rainfall: 156.8,
    waterLevel: 1.2,
    inclination: 8.5,
    deviceCount: 6,
    devices: [{name: '雨量计-A4', status: 'online'}, {name: '地声传感器-B4', status: 'online'}, {
      name: '位移计-C4',
      status: 'online'
    }, {name: '视频监控-D4', status: 'online'}, {name: '振动传感器-E4', status: 'warning'}, {
      name: '裂缝计-F4',
      status: 'online'
    }]
  },
  {
    id: 5,
    name: '地面沉降点',
    code: 'HZ-005',
    type: '沉降',
    location: '工业园区A区',
    level: 'medium',
    x: 22,
    y: 55,
    hasAlarm: false,
    displacement: 8.3,
    rainfall: 65.4,
    waterLevel: 4.5,
    inclination: 0.5,
    deviceCount: 3,
    devices: [{name: '沉降计-A5', status: 'online'}, {name: '水位计-B5', status: 'online'}, {
      name: '视频监控-C5',
      status: 'online'
    }]
  },
  {
    id: 6,
    name: '桥梁监测点',
    code: 'HZ-006',
    type: '桥梁',
    location: 'XX大桥',
    level: 'low',
    x: 55,
    y: 35,
    hasAlarm: false,
    displacement: 1.2,
    rainfall: 88.6,
    waterLevel: 0,
    inclination: 0.3,
    deviceCount: 4,
    devices: [{name: '应变计-A6', status: 'online'}, {name: '位移计-B6', status: 'online'}, {
      name: '倾角仪-C6',
      status: 'online'
    }, {name: '视频监控-D6', status: 'online'}]
  }
])

const chartYLabels = ['30', '25', '20', '15', '10', '5', '0']
const chartXLabels = ['0时', '4时', '8时', '12时', '16时', '20时']

const chartDataPoints = computed(() => {
  const data = [12, 15, 18, 22, 19, 16]
  return data.map((value, index) => ({
    x: index * 80 + 20,
    y: 150 - (value / 30) * 150
  }))
})

const chartLinePath = computed(() => {
  const points = chartDataPoints.value
  if (points.length === 0) return ''
  return points.map((p, i) => `${i === 0 ? 'M' : 'L'} ${p.x} ${p.y}`).join(' ')
})

const chartAreaPath = computed(() => {
  const linePath = chartLinePath.value
  if (!linePath) return ''
  const points = chartDataPoints.value
  const lastX = points[points.length - 1]?.x || 400
  return `${linePath} L ${lastX} 150 L 20 150 Z`
})

const getHealthClass = (value: number) => {
  if (value >= 90) return 'healthy'
  if (value >= 70) return 'warning'
  return 'danger'
}

const handleMapZoom = () => {
}

const trendYLabels = ['100', '80', '60', '40', '20', '0']
const trendXLabels = ['5-11', '5-12', '5-13', '5-14', '5-15', '5-16', '5-17']

const trendDataPoints = computed(() => {
  const data = [85, 88, 82, 90, 91, 89, 91]
  return data.map((value, index) => ({
    x: index * 46 + 23,
    y: 100 - value
  }))
})

const trendLinePath = computed(() => {
  const points = trendDataPoints.value
  if (points.length === 0) return ''
  return points.map((p, i) => `${i === 0 ? 'M' : 'L'} ${p.x} ${p.y}`).join(' ')
})

const trendAreaPath = computed(() => {
  const linePath = trendLinePath.value
  if (!linePath) return ''
  const points = trendDataPoints.value
  const lastX = points[points.length - 1]?.x || 280
  return `${linePath} L ${lastX} 100 L 23 100 Z`
})
</script>

<style scoped>
.comprehensive-view {
  display: flex;
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8ec 100%);
  position: relative;
}

.left-panel, .right-panel {
  width: 320px;
  padding: 16px;
  overflow-y: auto;
  background: rgba(255, 255, 255, 0.6);
  box-shadow: none;
}

.right-panel {
  border-right: none;
  border-left: none;
}

.center-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: transparent;
}

.charts-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: rgba(255, 255, 255, 0.7);
  border-radius: 12px;
  margin: 16px;
  overflow: hidden;
  box-shadow: none;
  border: 1px solid rgba(255, 255, 255, 0.5);
}

.analysis-charts {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 16px;
  overflow-y: auto;
}

.chart-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 8px;
  overflow: hidden;
  min-height: 320px;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
}

.panel-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}

.panel-subtitle {
  font-size: 14px;
  color: #6b7280;
}

.chart-body {
  flex: 1;
  padding: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.echarts-container {
  width: 100%;
  height: 100%;
  min-height: 250px;
}

.panel-section {
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 16px;
  box-shadow: none;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}

.health-question {
  color: #3b82f6;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  transition: background 0.2s;
  position: relative;
}

.health-question:hover {
  background: rgba(79, 172, 254, 0.2);
}

.health-overview {
  margin-bottom: 16px;
}

.health-score {
  display: flex;
  align-items: baseline;
  margin-bottom: 12px;
}

.score-value {
  font-size: 48px;
  font-weight: 700;
  color: #52c41a;
}

.score-unit {
  font-size: 24px;
  color: rgba(255, 255, 255, 0.6);
  margin-left: 4px;
}

.health-bar {
  height: 8px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 4px;
  overflow: hidden;
}

.health-bar-fill {
  height: 100%;
  background: linear-gradient(90deg, #52c41a, #4facfe);
  border-radius: 4px;
  transition: width 0.5s ease;
}

.health-items {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.health-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.health-label {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.6);
}

.health-value {
  font-size: 13px;
  font-weight: 600;
}

.health-value.healthy {
  color: #52c41a;
}

.health-value.warning {
  color: #faad14;
}

.health-value.danger {
  color: #f5222d;
}

.health-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.health-ring-container {
  position: relative;
  width: 140px;
  height: 140px;
  margin: 0 auto;
}

.health-ring {
  width: 100%;
  height: 100%;
  transform: rotate(-90deg);
}

.ring-bg {
  fill: none;
  stroke: rgba(255, 255, 255, 0.1);
  stroke-width: 12;
}

.ring-segment {
  fill: none;
  stroke-width: 12;
  stroke-linecap: round;
  transition: all 0.3s ease;
  cursor: pointer;
}

.ring-segment.active {
  stroke-width: 16;
  filter: drop-shadow(0 0 6px currentColor);
}

.ring-segment:not(.active) {
  opacity: 0.5;
}

.ring-center {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
}

.ring-score {
  font-size: 28px;
  font-weight: 700;
  color: #1f2937;
}

.ring-label {
  font-size: 14px;
  color: #6b7280;
}

.health-bars {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.health-bar-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.bar-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.bar-name {
  font-size: 14px;
  color: #4b5563;
}

.bar-value {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}

.bar-track {
  height: 6px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 3px;
  overflow: hidden;
}

.bar-progress {
  height: 100%;
  border-radius: 3px;
  transition: width 0.5s ease;
}

.health-bar-item {
  padding: 6px 8px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.health-bar-item.active {
  background: rgba(59, 130, 246, 0.1);
  transform: translateX(4px);
}

.health-bar-item:not(.active) {
  opacity: 0.5;
}

.health-bar-item:hover {
  background: rgba(59, 130, 246, 0.05);
}

.resource-cards {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 16px;
}

.resource-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
}

.resource-icon {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 8px;
}

.resource-info {
  flex: 1;
}

.resource-value {
  font-size: 20px;
  font-weight: 700;
  color: #fff;
}

.resource-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
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

.resource-section .ring-bg {
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
  font-size: 14px;
  color: #4b5563;
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

.type-bar-row .type-name {
  font-size: 14px;
  color: #6b7280;
}

.type-bar-row .type-count {
  font-size: 14px;
  color: #3b82f6;
  font-weight: 600;
}

.stats-bar {
  display: flex;
  align-items: center;
  justify-content: space-around;
  padding: 24px 32px;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon-wrapper {
  width: 52px;
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  flex-shrink: 0;
}

.stat-icon-wrapper.alarm {
  background: linear-gradient(135deg, #fee2e2 0%, #fecaca 100%);
  color: #ef4444;
}

.stat-icon-wrapper.warning {
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
  color: #f59e0b;
}

.stat-icon-wrapper.monitor {
  background: linear-gradient(135deg, #dbeafe 0%, #bfdbfe 100%);
  color: #3b82f6;
}

.stat-icon-wrapper.hazard {
  background: linear-gradient(135deg, #e0e7ff 0%, #c7d2fe 100%);
  color: #6366f1;
}

.stat-icon-wrapper.device {
  background: linear-gradient(135deg, #d1fae5 0%, #a7f3d0 100%);
  color: #10b981;
}

.stat-content {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.stat-value-wrapper {
  position: relative;
}

.stat-value {
  font-size: 44px;
  font-weight: 900;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  color: #111827;
  letter-spacing: -1px;
  line-height: 1;
  opacity: 0;
  transform: translateY(20px);
  transition: all 0.6s cubic-bezier(0.34, 1.56, 0.64, 1);
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.stat-value.animate {
  opacity: 1;
  transform: translateY(0);
}

.stat-value.highlight {
  background: linear-gradient(135deg, #1f2937 0%, #374151 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.stat-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.stat-label {
  font-size: 14px;
  color: #374151;
  font-weight: 700;
  letter-spacing: 0.3px;
  text-transform: none;
}

.stat-desc {
  font-size: 14px;
  color: #9ca3af;
  font-weight: 500;
}

.stat-divider {
  width: 1px;
  height: 64px;
  background: linear-gradient(to bottom, transparent, rgba(156, 163, 175, 0.3), transparent);
}

.map-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
}

.map-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}

.map-legend {
  display: flex;
  gap: 16px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.legend-dot.alarm {
  background: #f5222d;
}

.legend-dot.normal {
  background: #52c41a;
}

.legend-text {
  font-size: 14px;
  color: #6b7280;
}

.map-content {
  flex: 1;
  position: relative;
  overflow: hidden;
}

.map-content :deep(.leaflet-container) {
  width: 100%;
  height: 100%;
}

.map-content:active {
  cursor: grabbing;
}

.map-grid {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
}

.grid-line-h {
  position: absolute;
  left: 0;
  right: 0;
  height: 1px;
  background: rgba(79, 172, 254, 0.1);
}

.grid-line-v {
  position: absolute;
  top: 0;
  bottom: 0;
  width: 1px;
  background: rgba(79, 172, 254, 0.1);
}

.grid-line-h:nth-child(-n+20) {
  top: calc(var(--i, 0) * 5%);
}

.grid-line-v:nth-child(n+21) {
  left: calc((var(--i, 0) - 20) * 5%);
}

.map-regions {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
}

.region {
  position: absolute;
  border: 1px solid rgba(79, 172, 254, 0.3);
  border-radius: 8px;
  background: rgba(79, 172, 254, 0.05);
}

.region-name {
  position: absolute;
  top: 8px;
  left: 12px;
  font-size: 12px;
  color: rgba(79, 172, 254, 0.7);
}

.hazard-markers {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
}

.hazard-marker {
  position: absolute;
  transform: translate(-50%, -50%);
  cursor: pointer;
  z-index: 10;
  transition: transform 0.2s;
}

.hazard-marker:hover, .hazard-marker.active {
  transform: translate(-50%, -50%) scale(1.2);
}

.marker-ring {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 32px;
  height: 32px;
  transform: translate(-50%, -50%);
  border-radius: 50%;
  border: 2px solid #f5222d;
  animation: ring-pulse 1.5s infinite;
}

@keyframes ring-pulse {
  0% {
    transform: translate(-50%, -50%) scale(1);
    opacity: 1;
  }
  100% {
    transform: translate(-50%, -50%) scale(1.5);
    opacity: 0;
  }
}

.marker-icon {
  position: relative;
  width: 20px;
  height: 20px;
  color: #52c41a;
  z-index: 1;
}

.hazard-marker.has-alarm .marker-icon {
  color: #f5222d;
}

.map-zoom-controls {
  position: absolute;
  bottom: 20px;
  right: 20px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.zoom-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.95);
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.zoom-btn:hover {
  background: rgba(59, 130, 246, 0.1);
  border-color: rgba(59, 130, 246, 0.3);
}

.monitor-chart-panel {
  height: 320px;
  background: #ffffff;
  border-top: 1px solid rgba(79, 172, 254, 0.3);
  box-shadow: 0 -4px 20px rgba(0, 0, 0, 0.08);
  display: flex;
  flex-direction: column;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
}

.chart-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}

.close-chart {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.06);
  border: none;
  border-radius: 6px;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.2s;
}

.close-chart:hover {
  background: rgba(0, 0, 0, 0.1);
  color: #374151;
}

.chart-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 12px 20px;
  overflow: hidden;
}

.data-filters {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  flex-wrap: wrap;
}

.data-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.data-content {
  flex: 1;
  overflow: hidden;
}

.chart-container {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.chart-area-wrapper {
  flex: 1;
  display: flex;
  gap: 10px;
  min-height: 120px;
}

.chart-main-area {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.chart-stats {
  display: flex;
  justify-content: space-around;
  padding: 10px;
  background: rgba(59, 130, 246, 0.05);
  border-radius: 8px;
  margin-top: 10px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-label {
  font-size: 11px;
  color: #6b7280;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 14px;
  font-weight: 600;
  color: #374151;
}

.stat-value.current {
  color: #3b82f6;
}

.stat-value.max {
  color: #ef4444;
}

.stat-value.min {
  color: #10b981;
}

.stat-value.avg {
  color: #6b7280;
}

.table-container {
  height: 100%;
  overflow: auto;
}

.chart-tabs {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}

.chart-tab {
  font-size: 12px;
  color: #6b7280;
  padding: 4px 12px;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
}

.chart-tab.active {
  background: rgba(59, 130, 246, 0.15);
  color: #3b82f6;
}

.line-chart {
  flex: 1;
  display: flex;
  gap: 10px;
}

.chart-y-axis {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 0 4px;
}

.chart-y-axis span {
  font-size: 12px;
  color: #9ca3af;
}

.chart-area {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.chart-svg {
  flex: 1;
  width: 100%;
}

.chart-x-axis {
  display: flex;
  justify-content: space-between;
  padding-top: 8px;
}

.chart-x-axis span {
  font-size: 12px;
  color: #9ca3af;
}

.chart-summary {
  display: flex;
  gap: 20px;
  padding-left: 20px;
  border-left: 1px solid rgba(79, 172, 254, 0.2);
}

.summary-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.summary-label {
  font-size: 11px;
  color: #9ca3af;
}

.summary-value {
  font-size: 14px;
  font-weight: 600;
  color: #3b82f6;
}

.online-overview {
  display: flex;
  justify-content: space-around;
  align-items: center;
  margin-bottom: 20px;
  padding: 12px;
  background: rgba(82, 196, 26, 0.08);
  border-radius: 8px;
}

.online-rate-section {
  text-align: center;
}

.online-rate {
  display: flex;
  align-items: baseline;
  justify-content: center;
  margin-bottom: 4px;
}

.rate-value {
  font-size: 42px;
  font-weight: 700;
  color: #52c41a;
}

.rate-unit {
  font-size: 20px;
  color: #9ca3af;
}

.online-text {
  font-size: 14px;
  color: #6b7280;
}

.online-count-section {
  text-align: center;
}

.online-numbers {
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 4px;
  margin-bottom: 4px;
}

.online-count {
  font-size: 24px;
  font-weight: 700;
  color: #1f2937;
}

.online-separator {
  color: #9ca3af;
}

.total-count {
  font-size: 16px;
  color: #6b7280;
}

.online-label {
  font-size: 14px;
  color: #6b7280;
}

.online-chart {
  margin-bottom: 16px;
}

.online-chart .chart-title {
  font-size: 14px;
  color: #4b5563;
  margin-bottom: 12px;
}

.type-bars {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.type-bar-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.bar-label {
  width: 50px;
  font-size: 14px;
  color: #6b7280;
}

.bar-container {
  flex: 1;
  height: 12px;
  background: rgba(0, 0, 0, 0.06);
  border-radius: 6px;
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  background: linear-gradient(90deg, #52c41a, #4facfe);
  border-radius: 6px;
  transition: width 0.5s ease;
}

.bar-count {
  width: 50px;
  font-size: 14px;
  color: #374151;
  text-align: right;
}

.online-trend {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid rgba(79, 172, 254, 0.2);
}

.trend-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.trend-title {
  font-size: 14px;
  font-weight: 600;
  color: #374151;
}

.trend-subtitle {
  font-size: 14px;
  color: #9ca3af;
}

.trend-chart {
  display: flex;
  gap: 8px;
}

.trend-y-axis {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 0 2px;
}

.trend-y-axis span {
  font-size: 12px;
  color: #9ca3af;
}

.trend-area {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.trend-svg {
  flex: 1;
  width: 100%;
  min-height: 80px;
}

.trend-x-axis {
  display: flex;
  justify-content: space-between;
  padding-top: 4px;
}

.trend-x-axis span {
  font-size: 12px;
  color: #9ca3af;
}

.alarm-summary {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
}

.alarm-summary-item {
  flex: 1;
  text-align: center;
  padding: 12px;
  background: rgba(245, 34, 45, 0.06);
  border-radius: 8px;
}

.summary-badge {
  font-size: 14px;
  padding: 2px 8px;
  border-radius: 4px;
  margin-bottom: 8px;
  display: inline-block;
}

.summary-badge.pending {
  background: rgba(245, 34, 45, 0.15);
  color: #dc2626;
}

.summary-badge.history {
  background: rgba(0, 0, 0, 0.06);
  color: #6b7280;
}

.summary-count {
  font-size: 28px;
  font-weight: 700;
  color: #1f2937;
}

.alarm-level-stats {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
  margin-bottom: 16px;
}

.level-stat {
  display: flex;
  align-items: center;
  gap: 8px;
}

.level-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.level-dot.red {
  background: #dc2626;
}

.level-dot.orange {
  background: #ea580c;
}

.level-dot.yellow {
  background: #ca8a04;
}

.level-dot.green {
  background: #16a34a;
}

.level-name {
  flex: 1;
  font-size: 14px;
  color: #4b5563;
}

.level-count {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}

.alarm-list-section {
  border-top: 1px solid rgba(79, 172, 254, 0.2);
  padding-top: 12px;
}

.list-header {
  margin-bottom: 12px;
}

.list-title {
  font-size: 14px;
  color: #4b5563;
}

.alarm-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.alarm-item {
  display: flex;
  gap: 10px;
  padding: 10px;
  background: rgba(0, 0, 0, 0.04);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.alarm-item:hover {
  background: rgba(59, 130, 246, 0.08);
}

.alarm-level-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-top: 4px;
}

.alarm-level-dot.red {
  background: #dc2626;
}

.alarm-level-dot.orange {
  background: #ea580c;
}

.alarm-level-dot.yellow {
  background: #ca8a04;
}

.alarm-level-dot.green {
  background: #16a34a;
}

.alarm-content {
  flex: 1;
  min-width: 0;
}

.alarm-title {
  font-size: 14px;
  color: #374151;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.alarm-meta {
  font-size: 14px;
  color: #9ca3af;
  margin-top: 4px;
}

.algorithm-popover {
  position: fixed;
  width: 300px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
  padding: 14px 16px;
  z-index: 1000;
  cursor: default;
  pointer-events: auto;
}

.popover-arrow {
  position: absolute;
  top: -6px;
  left: 8px;
  width: 12px;
  height: 12px;
  background: #fff;
  border-left: 1px solid #e2e8f0;
  border-top: 1px solid #e2e8f0;
  transform: rotate(45deg);
}

.algorithm-popover p {
  font-size: 12px;
  color: #374151;
  line-height: 1.5;
  margin: 0;
}

.algorithm-popover ul {
  margin: 6px 0 0 0;
  padding-left: 16px;
}

.algorithm-popover li {
  font-size: 12px;
  color: #4b5563;
  margin-bottom: 3px;
  line-height: 1.5;
}

.algorithm-popover li strong {
  color: #3b82f6;
}

/* 隐患点标记样式 */
.custom-hazard-marker {
  cursor: pointer;
  transition: transform 0.2s ease;
}

.custom-hazard-marker:hover {
  transform: scale(1.1);
}

.hazard-marker-container {
  position: relative;
}

.hazard-icon {
  transition: transform 0.2s ease;
}

.hazard-marker-container:hover .hazard-icon {
  transform: scale(1.1);
}

.alarm-ring {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 32px;
  height: 32px;
  transform: translate(-50%, -50%);
  border: 2px solid #f5222d;
  border-radius: 50%;
  animation: alarm-pulse 1.5s infinite;
}

@keyframes alarm-pulse {
  0% {
    transform: translate(-50%, -50%) scale(1);
    opacity: 1;
  }
  100% {
    transform: translate(-50%, -50%) scale(1.4);
    opacity: 0;
  }
}

.device-count-badge {
  cursor: pointer;
  transition: all 0.2s ease;
}

.device-count-badge:hover {
  transform: scale(1.1);
  background: #0ea5e9;
}

/* 隐患点弹窗样式 */
.hazard-popup-container .leaflet-popup-content-wrapper {
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
  border: 1px solid rgba(79, 172, 254, 0.2);
  padding: 0;
  overflow: hidden;
}

.hazard-popup-container .leaflet-popup-tip {
  background: #ffffff;
  border-color: rgba(79, 172, 254, 0.2);
}

.hazard-popup {
  padding: 16px;
  min-width: 260px;
}

.popup-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
}

.popup-title {
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
}

.popup-status {
  font-size: 14px;
  font-weight: 500;
  padding: 2px 8px;
  border-radius: 4px;
  background: rgba(0, 0, 0, 0.06);
}

.popup-device-info {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
  font-size: 14px;
}

.device-count-label {
  color: #6b7280;
}

.device-count-value {
  color: #374151;
  font-weight: 500;
}

.popup-device-list {
  background: rgba(0, 0, 0, 0.04);
  border-radius: 8px;
  padding: 10px;
}

.popup-device-list .list-header {
  font-size: 14px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 8px;
  padding-bottom: 6px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
}

.device-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 0;
  font-size: 14px;
}

.device-name {
  color: #4b5563;
}

.device-status {
  font-weight: 500;
  font-size: 14px;
}
</style>
