<template>
  <div class="dashboard-container">
    <div class="dashboard-header">
      <div class="header-left">
        <el-radio-group v-model="viewMode" size="default">
          <el-radio-button label="overview">总览层</el-radio-button>
          <el-radio-button label="hazard">隐患点层</el-radio-button>
        </el-radio-group>
      </div>
      <div class="header-right">
        <el-input
          v-model="searchKeyword"
          placeholder="隐患点/设备模糊查找"
          class="search-input"
          clearable
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </div>
    </div>

    <div class="dashboard-main">
      <div class="left-panel" v-if="viewMode === 'overview'">
        <div class="stat-card">
          <div class="stat-header">
            <span class="stat-title">系统健康度</span>
          </div>
          <div class="stat-body">
            <div class="health-ring">
              <div class="ring-chart" ref="healthRingRef"></div>
              <div class="ring-value">{{ healthScore }}%</div>
            </div>
            <div class="health-detail">
              <div class="detail-item">
                <span class="label">隐患点完整度</span>
                <el-progress :percentage="hazardIntegrity" :stroke-width="6" />
              </div>
              <div class="detail-item">
                <span class="label">设备完整度</span>
                <el-progress :percentage="deviceIntegrity" :stroke-width="6" />
              </div>
              <div class="detail-item">
                <span class="label">关联完成度</span>
                <el-progress :percentage="relationComplete" :stroke-width="6" />
              </div>
            </div>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-header">
            <span class="stat-title">隐患点监测统计</span>
          </div>
          <div class="stat-body">
            <div class="pie-chart" ref="hazardPieRef"></div>
            <div class="pie-legend">
              <div class="legend-item">
                <span class="dot monitoring"></span>
                <span class="text">监测中</span>
                <span class="value">{{ hazardStats.monitoring }}</span>
              </div>
              <div class="legend-item">
                <span class="dot paused"></span>
                <span class="text">停测</span>
                <span class="value">{{ hazardStats.paused }}</span>
              </div>
              <div class="legend-item">
                <span class="dot completed"></span>
                <span class="text">完结</span>
                <span class="value">{{ hazardStats.completed }}</span>
              </div>
            </div>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-header">
            <span class="stat-title">设备在线情况</span>
          </div>
          <div class="stat-body">
            <div class="device-online">
              <div class="online-rate">{{ deviceOnlineRate }}%</div>
              <div class="online-text">平均在线率</div>
            </div>
            <div class="online-detail">
              <div class="online-stat">
                <el-icon color="#52c41a"><CircleCheck /></el-icon>
                <span>在线 {{ onlineDevices }}</span>
              </div>
              <div class="online-stat">
                <el-icon color="#ff4d4f"><CircleClose /></el-icon>
                <span>离线 {{ offlineDevices }}</span>
              </div>
            </div>
            <div class="online-total">共 {{ totalDevices }} 台设备</div>
          </div>
        </div>

        <div class="stat-card alarm-card">
          <div class="stat-header">
            <span class="stat-title">告警态势</span>
          </div>
          <div class="stat-body">
            <div class="alarm-summary">
              <div class="alarm-stat">
                <span class="alarm-num pending">{{ pendingAlarms }}</span>
                <span class="alarm-label">待办告警</span>
              </div>
              <div class="alarm-stat">
                <span class="alarm-num history">{{ historyAlarms }}</span>
                <span class="alarm-label">历史告警</span>
              </div>
            </div>
            <div class="alarm-levels">
              <div class="level-item level-red">
                <span class="level-count">{{ alarmLevels.red }}</span>
                <span class="level-label">红色</span>
              </div>
              <div class="level-item level-orange">
                <span class="level-count">{{ alarmLevels.orange }}</span>
                <span class="level-label">橙色</span>
              </div>
              <div class="level-item level-yellow">
                <span class="level-count">{{ alarmLevels.yellow }}</span>
                <span class="level-label">黄色</span>
              </div>
              <div class="level-item level-blue">
                <span class="level-count">{{ alarmLevels.blue }}</span>
                <span class="level-label">蓝色</span>
              </div>
            </div>
            <div class="alarm-list">
              <div class="alarm-list-header">待办告警列表</div>
              <div class="alarm-list-body" ref="alarmListRef">
                <div
                  v-for="alarm in alarmList"
                  :key="alarm.id"
                  class="alarm-item"
                  :class="'level-' + alarm.level"
                  @mouseenter="showAlarmBubble(alarm)"
                  @mouseleave="hideBubble"
                >
                  <span class="alarm-icon" :class="'icon-' + alarm.level"></span>
                  <span class="alarm-name">{{ alarm.hazardName }}</span>
                  <span class="alarm-time">{{ alarm.time }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="left-panel" v-else>
        <div class="stat-card">
          <div class="stat-header">
            <span class="stat-title">隐患点资料</span>
          </div>
          <div class="stat-body hazard-info">
            <div class="info-row">
              <span class="label">编号</span>
              <span class="value">{{ currentHazard.code }}</span>
            </div>
            <div class="info-row">
              <span class="label">名称</span>
              <span class="value">{{ currentHazard.name }}</span>
            </div>
            <div class="info-row">
              <span class="label">分组</span>
              <span class="value">{{ currentHazard.group }}</span>
            </div>
            <div class="info-row">
              <span class="label">状态</span>
              <span class="value status-tag" :class="currentHazard.status">{{ currentHazard.statusText }}</span>
            </div>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-header">
            <span class="stat-title">设备列表</span>
            <span class="stat-count">{{ deviceList.length }} 台</span>
          </div>
          <div class="stat-body">
            <div class="device-filter">
              <el-select v-model="deviceFilterStatus" placeholder="设备状态" size="small" clearable>
                <el-option label="监测中" value="monitoring" />
                <el-option label="停测" value="paused" />
                <el-option label="完结" value="completed" />
              </el-select>
            </div>
            <div class="device-list">
              <div
                v-for="device in filteredDevices"
                :key="device.id"
                class="device-item"
                @click="selectDevice(device)"
              >
                <div class="device-info">
                  <span class="device-name">{{ device.name }}</span>
                  <span class="device-type">{{ device.type }}</span>
                </div>
                <span class="device-status" :class="device.status">{{ device.statusText }}</span>
              </div>
            </div>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-header">
            <span class="stat-title">设备在线情况</span>
          </div>
          <div class="stat-body">
            <div class="device-online">
              <div class="online-rate">{{ hazardDeviceOnlineRate }}%</div>
              <div class="online-text">在线率</div>
            </div>
            <div class="online-detail">
              <div class="online-stat">
                <el-icon color="#52c41a"><CircleCheck /></el-icon>
                <span>在线 {{ hazardOnlineDevices }}</span>
              </div>
              <div class="online-stat">
                <el-icon color="#ff4d4f"><CircleClose /></el-icon>
                <span>离线 {{ hazardOfflineDevices }}</span>
              </div>
            </div>
            <div class="online-total">共 {{ hazardTotalDevices }} 台设备</div>
          </div>
        </div>

        <div class="stat-card alarm-card">
          <div class="stat-header">
            <span class="stat-title">告警态势</span>
          </div>
          <div class="stat-body">
            <div class="alarm-summary">
              <div class="alarm-stat">
                <span class="alarm-num pending">{{ hazardPendingAlarms }}</span>
                <span class="alarm-label">待办告警</span>
              </div>
              <div class="alarm-stat">
                <span class="alarm-num history">{{ hazardHistoryAlarms }}</span>
                <span class="alarm-label">历史告警</span>
              </div>
            </div>
            <div class="alarm-levels">
              <div class="level-item level-red">
                <span class="level-count">{{ hazardAlarmLevels.red }}</span>
                <span class="level-label">红色</span>
              </div>
              <div class="level-item level-orange">
                <span class="level-count">{{ hazardAlarmLevels.orange }}</span>
                <span class="level-label">橙色</span>
              </div>
              <div class="level-item level-yellow">
                <span class="level-count">{{ hazardAlarmLevels.yellow }}</span>
                <span class="level-label">黄色</span>
              </div>
              <div class="level-item level-blue">
                <span class="level-count">{{ hazardAlarmLevels.blue }}</span>
                <span class="level-label">蓝色</span>
              </div>
            </div>
            <div class="alarm-list">
              <div class="alarm-list-header">设备告警列表</div>
              <div class="alarm-list-body">
                <div
                  v-for="alarm in hazardAlarmList"
                  :key="alarm.id"
                  class="alarm-item"
                  :class="'level-' + alarm.level"
                  @mouseenter="showDeviceAlarmBubble(alarm)"
                  @mouseleave="hideBubble"
                >
                  <span class="alarm-icon" :class="'icon-' + alarm.level"></span>
                  <span class="alarm-name">{{ alarm.deviceName }}</span>
                  <span class="alarm-time">{{ alarm.time }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="map-container" ref="mapContainerRef"></div>

      <div class="right-panel">
        <div class="layer-control">
          <div class="layer-title">图层管理</div>
          <div class="layer-list">
            <div class="layer-item">
              <el-switch v-model="layers.hazard" size="small" />
              <span>隐患点图层</span>
            </div>
            <div class="layer-item">
              <el-switch v-model="layers.device" size="small" />
              <span>设备图层</span>
            </div>
            <div class="layer-item">
              <el-switch v-model="layers.video" size="small" />
              <span>视频设备图层</span>
            </div>
          </div>
        </div>

        <div class="map-tools">
          <div class="tools-title">地图工具</div>
          <div class="tools-list">
            <el-tooltip content="缩放" placement="left">
              <div class="tool-btn" @click="toggleZoom">
                <el-icon><ZoomIn /></el-icon>
              </div>
            </el-tooltip>
            <el-tooltip content="定位" placement="left">
              <div class="tool-btn" @click="toggleLocate">
                <el-icon><Aim /></el-icon>
              </div>
            </el-tooltip>
            <el-tooltip content="测量" placement="left">
              <div class="tool-btn" @click="toggleMeasure">
                <el-icon><FullScreen /></el-icon>
              </div>
            </el-tooltip>
            <el-tooltip content="图例" placement="left">
              <div class="tool-btn" @click="toggleLegend">
                <el-icon><List /></el-icon>
              </div>
            </el-tooltip>
          </div>
        </div>
      </div>
    </div>

    <div class="map-legend" v-if="showLegend">
      <div class="legend-title">告警等级</div>
      <div class="legend-items">
        <div class="legend-item">
          <span class="legend-color level-red"></span>
          <span>红色告警</span>
        </div>
        <div class="legend-item">
          <span class="legend-color level-orange"></span>
          <span>橙色告警</span>
        </div>
        <div class="legend-item">
          <span class="legend-color level-yellow"></span>
          <span>黄色告警</span>
        </div>
        <div class="legend-item">
          <span class="legend-color level-blue"></span>
          <span>蓝色告警</span>
        </div>
      </div>
    </div>

    <div class="bubble-container" ref="bubbleRef" v-show="showBubble">
      <div class="bubble-content">
        <div class="bubble-title">{{ bubbleData.title }}</div>
        <div class="bubble-body" v-html="bubbleData.content"></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, computed, watch, nextTick } from 'vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import * as echarts from 'echarts'
import { Search, ZoomIn, Aim, FullScreen, List, CircleCheck, CircleClose } from '@element-plus/icons-vue'

const TIANDITU_KEY = '8dda07d4649c77efd0537a0ff0a1df13'

const viewMode = ref('overview')
const searchKeyword = ref('')
const mapContainerRef = ref<HTMLElement | null>(null)
const healthRingRef = ref<HTMLElement | null>(null)
const hazardPieRef = ref<HTMLElement | null>(null)
const alarmListRef = ref<HTMLElement | null>(null)
const bubbleRef = ref<HTMLElement | null>(null)
const showBubble = ref(false)

let map: L.Map | null = null
let healthChart: echarts.ECharts | null = null
let hazardPieChart: echarts.ECharts | null = null

const layers = reactive({
  hazard: true,
  device: true,
  video: true
})

const showLegend = ref(false)

const healthScore = ref(85)
const hazardIntegrity = ref(90)
const deviceIntegrity = ref(82)
const relationComplete = ref(83)

const hazardStats = reactive({
  monitoring: 12,
  paused: 3,
  completed: 5
})

const deviceOnlineRate = ref(94)
const onlineDevices = ref(156)
const offlineDevices = ref(10)
const totalDevices = ref(166)

const pendingAlarms = ref(23)
const historyAlarms = ref(156)
const alarmLevels = reactive({
  red: 2,
  orange: 5,
  yellow: 8,
  blue: 8
})

const alarmList = ref([
  { id: 1, hazardName: '成都理工大学隐患点1', level: 'red', time: '10:23' },
  { id: 2, hazardName: '龙潭寺隐患点2', level: 'orange', time: '10:15' },
  { id: 3, hazardName: '成华区隐患点3', level: 'yellow', time: '09:45' },
  { id: 4, hazardName: '锦江区隐患点4', level: 'blue', time: '09:30' },
  { id: 5, hazardName: '武侯区隐患点5', level: 'yellow', time: '09:20' },
  { id: 6, hazardName: '金牛区隐患点6', level: 'blue', time: '08:55' }
])

const currentHazard = reactive({
  code: 'HD-2024-001',
  name: '成都理工大学隐患点',
  group: '教育组',
  status: 'monitoring',
  statusText: '监测中'
})

const deviceFilterStatus = ref('')

const deviceList = ref([
  { id: 1, name: 'GNSS位移传感器-01', type: '位移监测', status: 'monitoring', statusText: '监测中', sensors: 3 },
  { id: 2, name: '雨量计-01', type: '雨量监测', status: 'monitoring', statusText: '监测中', sensors: 1 },
  { id: 3, name: '深部位移传感器-01', type: '位移监测', status: 'paused', statusText: '停测', sensors: 5 },
  { id: 4, name: '水位传感器-01', type: '水位监测', status: 'monitoring', statusText: '监测中', sensors: 2 },
  { id: 5, name: '视频监控-01', type: '视频监控', status: 'monitoring', statusText: '监测中', sensors: 1 }
])

const filteredDevices = computed(() => {
  if (!deviceFilterStatus.value) return deviceList.value
  return deviceList.value.filter(d => d.status === deviceFilterStatus.value)
})

const hazardDeviceOnlineRate = ref(100)
const hazardOnlineDevices = ref(4)
const hazardOfflineDevices = ref(0)
const hazardTotalDevices = ref(5)

const hazardPendingAlarms = ref(8)
const hazardHistoryAlarms = ref(42)
const hazardAlarmLevels = reactive({
  red: 1,
  orange: 2,
  yellow: 3,
  blue: 2
})

const hazardAlarmList = ref([
  { id: 1, deviceName: 'GNSS位移传感器-01', level: 'red', time: '10:23' },
  { id: 2, deviceName: '深部位移传感器-01', level: 'orange', time: '10:15' },
  { id: 3, deviceName: '雨量计-01', level: 'yellow', time: '09:45' },
  { id: 4, deviceName: '水位传感器-01', level: 'blue', time: '09:30' }
])

const bubbleData = reactive({
  title: '',
  content: ''
})

let hazardMarkers: L.LayerGroup | null = null
let deviceMarkers: L.LayerGroup | null = null

const hazardPoints = [
  { id: 1, name: '成都理工大学隐患点1', lat: 30.63, lng: 104.12, level: 'red', alarms: 3 },
  { id: 2, name: '龙潭寺隐患点2', lat: 30.65, lng: 104.14, level: 'orange', alarms: 2 },
  { id: 3, name: '成华区隐患点3', lat: 30.68, lng: 104.10, level: 'yellow', alarms: 1 },
  { id: 4, name: '锦江区隐患点4', lat: 30.62, lng: 104.08, level: 'blue', alarms: 1 },
  { id: 5, name: '武侯区隐患点5', lat: 30.64, lng: 104.05, level: 'yellow', alarms: 2 }
]

const devices = [
  { id: 1, name: '设备A', lat: 30.631, lng: 104.121, level: 'red', hazardId: 1 },
  { id: 2, name: '设备B', lat: 30.632, lng: 104.122, level: 'orange', hazardId: 1 },
  { id: 3, name: '设备C', lat: 30.651, lng: 104.141, level: 'normal', hazardId: 2 },
  { id: 4, name: '设备D', lat: 30.681, lng: 104.101, level: 'normal', hazardId: 3 }
]

const initMap = () => {
  if (!mapContainerRef.value || map) return

  map = L.map(mapContainerRef.value, {
    center: [30.65, 104.10],
    zoom: 12,
    zoomControl: false
  })

  L.tileLayer(`https://t0.tianditu.gov.cn/img_w/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=img&STYLE=default&TILEMATRIXSET=w&FORMAT=tiles&TILEMATRIX={z}&TILEROW={y}&TILECOL={x}&tk=${TIANDITU_KEY}`, {
    maxZoom: 18,
    minZoom: 1
  }).addTo(map)

  L.control.zoom({ position: 'bottomright' }).addTo(map)
  L.control.scale({ position: 'bottomleft' }).addTo(map)

  hazardMarkers = L.layerGroup().addTo(map)
  deviceMarkers = L.layerGroup().addTo(map)

  updateMarkers()
}

const getMarkerIcon = (type: string, level: string) => {
  const colors: Record<string, string> = {
    red: '#f5222d',
    orange: '#fa8c16',
    yellow: '#fadb14',
    blue: '#1890ff',
    normal: '#52c41a'
  }
  const color = colors[level] || colors.normal

  if (type === 'hazard') {
    return L.divIcon({
      className: 'custom-marker hazard-marker',
      html: `<div style="background: ${color}; width: 32px; height: 32px; border-radius: 50%; border: 3px solid white; box-shadow: 0 2px 8px rgba(0,0,0,0.3); display: flex; align-items: center; justify-content: center; color: white; font-weight: bold; font-size: 12px;">${level === 'red' ? '!' : level === 'orange' ? '!!' : level === 'yellow' ? '!' : level === 'blue' ? '!' : '●'}</div>`,
      iconSize: [32, 32],
      iconAnchor: [16, 16]
    })
  } else {
    return L.divIcon({
      className: 'custom-marker device-marker',
      html: `<div style="background: ${color}; width: 20px; height: 20px; border-radius: 50%; border: 2px solid white; box-shadow: 0 2px 6px rgba(0,0,0,0.3);"></div>`,
      iconSize: [20, 20],
      iconAnchor: [10, 10]
    })
  }
}

const updateMarkers = () => {
  if (!hazardMarkers || !deviceMarkers || !map) return

  hazardMarkers.clearLayers()
  deviceMarkers.clearLayers()

  if (layers.hazard) {
    hazardPoints.forEach(point => {
      const marker = L.marker([point.lat, point.lng], {
        icon: getMarkerIcon('hazard', point.level)
      })
      marker.on('click', () => {
        viewMode.value = 'hazard'
        map?.setView([point.lat, point.lng], 14)
      })
      marker.on('mouseover', () => {
        showHazardBubble(point)
      })
      marker.on('mouseout', () => {
        hideBubble()
      })
      marker.addTo(hazardMarkers!)
    })
  }

  if (layers.device) {
    devices.forEach(device => {
      const marker = L.marker([device.lat, device.lng], {
        icon: getMarkerIcon('device', device.level)
      })
      marker.on('click', () => {
        const d = deviceList.value.find(d => d.name.includes('GNSS') && device.name === '设备A' ? true : false)
        if (d) selectDevice(d)
      })
      marker.on('mouseover', () => {
        showDeviceBubble(device)
      })
      marker.on('mouseout', () => {
        hideBubble()
      })
      marker.addTo(deviceMarkers!)
    })
  }
}

const showHazardBubble = (point: typeof hazardPoints[0]) => {
  if (!bubbleRef.value || !map) return
  const pointPos = map.latLngToContainerPoint([point.lat, point.lng])

  bubbleData.title = point.name
  bubbleData.content = `
    <div style="font-size: 12px; color: #666;">
      <p style="margin: 4px 0;">告警级别: <span style="color: ${
        point.level === 'red' ? '#f5222d' :
        point.level === 'orange' ? '#fa8c16' :
        point.level === 'yellow' ? '#fadb14' : '#1890ff'
      }; font-weight: bold;">${
        point.level === 'red' ? '红色' :
        point.level === 'orange' ? '橙色' :
        point.level === 'yellow' ? '黄色' : '蓝色'
      }</span></p>
      <p style="margin: 4px 0;">当前告警数: ${point.alarms}</p>
      <p style="margin: 4px 0; color: #1890ff;">点击查看详情 >></p>
    </div>
  `

  bubbleRef.value.style.left = `${pointPos.x + 20}px`
  bubbleRef.value.style.top = `${pointPos.y - 60}px`
  showBubble.value = true
}

const showDeviceBubble = (device: typeof devices[0]) => {
  if (!bubbleRef.value || !map) return
  const pointPos = map.latLngToContainerPoint([device.lat, device.lng])

  bubbleData.title = device.name
  bubbleData.content = `
    <div style="font-size: 12px; color: #666;">
      <p style="margin: 4px 0;">设备状态: <span style="color: ${
        device.level === 'normal' ? '#52c41a' : '#f5222d'
      }; font-weight: bold;">${device.level === 'normal' ? '正常' : '告警'}</span></p>
      <p style="margin: 4px 0; color: #1890ff;">点击查看详情 >></p>
    </div>
  `

  bubbleRef.value.style.left = `${pointPos.x + 20}px`
  bubbleRef.value.style.top = `${pointPos.y - 40}px`
  showBubble.value = true
}

const showAlarmBubble = (alarm: typeof alarmList.value[0]) => {
  if (!bubbleRef.value || !map) return
  const point = hazardPoints.find(h => h.name === alarm.hazardName)
  if (!point) return

  const pointPos = map.latLngToContainerPoint([point.lat, point.lng])
  bubbleData.title = alarm.hazardName
  bubbleData.content = `
    <div style="font-size: 12px; color: #666;">
      <p style="margin: 4px 0;">告警级别: <span style="color: ${
        alarm.level === 'red' ? '#f5222d' :
        alarm.level === 'orange' ? '#fa8c16' :
        alarm.level === 'yellow' ? '#fadb14' : '#1890ff'
      }; font-weight: bold;">${
        alarm.level === 'red' ? '红色' :
        alarm.level === 'orange' ? '橙色' :
        alarm.level === 'yellow' ? '黄色' : '蓝色'
      }</span></p>
      <p style="margin: 4px 0;">告警时间: ${alarm.time}</p>
    </div>
  `

  bubbleRef.value.style.left = `${pointPos.x + 20}px`
  bubbleRef.value.style.top = `${pointPos.y - 60}px`
  showBubble.value = true
}

const showDeviceAlarmBubble = (alarm: typeof hazardAlarmList.value[0]) => {
  if (!bubbleRef.value || !map) return
  const device = devices.find(d => d.name === alarm.deviceName)
  if (!device) return

  const pointPos = map.latLngToContainerPoint([device.lat, device.lng])
  bubbleData.title = alarm.deviceName
  bubbleData.content = `
    <div style="font-size: 12px; color: #666;">
      <p style="margin: 4px 0;">告警级别: <span style="color: ${
        alarm.level === 'red' ? '#f5222d' :
        alarm.level === 'orange' ? '#fa8c16' :
        alarm.level === 'yellow' ? '#fadb14' : '#1890ff'
      }; font-weight: bold;">${
        alarm.level === 'red' ? '红色' :
        alarm.level === 'orange' ? '橙色' :
        alarm.level === 'yellow' ? '黄色' : '蓝色'
      }</span></p>
      <p style="margin: 4px 0;">告警时间: ${alarm.time}</p>
    </div>
  `

  bubbleRef.value.style.left = `${pointPos.x + 20}px`
  bubbleRef.value.style.top = `${pointPos.y - 40}px`
  showBubble.value = true
}

const hideBubble = () => {
  showBubble.value = false
}

const selectDevice = (device: typeof deviceList.value[0]) => {
  console.log('Selected device:', device)
}

const handleSearch = () => {
  console.log('Search:', searchKeyword.value)
}

const toggleLegend = () => {
  showLegend.value = !showLegend.value
}

const toggleZoom = () => {
  if (map) map.zoomIn()
}

const toggleLocate = () => {
  if (map) map.setView([30.65, 104.10], 12)
}

const toggleMeasure = () => {
  console.log('Measure tool toggled')
}

const initHealthChart = () => {
  if (!healthRingRef.value) return

  healthChart = echarts.init(healthRingRef.value)
  healthChart.setOption({
    series: [{
      type: 'gauge',
      startAngle: 90,
      endAngle: -270,
      radius: '90%',
      pointer: { show: false },
      progress: {
        show: true,
        overlap: false,
        roundCap: true,
        clipAngle: 100,
        itemStyle: {
          color: {
            type: 'linear',
            x: 0, y: 0, x2: 1, y2: 0,
            colorStops: [
              { offset: 0, color: '#1890ff' },
              { offset: 1, color: '#52c41a' }
            ]
          }
        }
      },
      axisLine: { lineStyle: { width: 10, color: [[1, '#e8e8e8']] } },
      splitLine: { show: false },
      axisTick: { show: false },
      axisLabel: { show: false },
      data: [{ value: healthScore.value, name: '健康度', title: { offsetCenter: ['0%', '30%'] } }],
      title: { fontSize: 12, color: '#666' },
      detail: { fontSize: 24, fontWeight: 'bold', offsetCenter: ['0', '0'], formatter: '{value}%', color: '#1f1f1f' }
    }]
  })
}

const initHazardPieChart = () => {
  if (!hazardPieRef.value) return

  hazardPieChart = echarts.init(hazardPieRef.value)
  hazardPieChart.setOption({
    series: [{
      type: 'pie',
      radius: ['50%', '70%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      emphasis: { label: { show: false } },
      data: [
        { value: hazardStats.monitoring, itemStyle: { color: '#52c41a' } },
        { value: hazardStats.paused, itemStyle: { color: '#faad14' } },
        { value: hazardStats.completed, itemStyle: { color: '#1890ff' } }
      ]
    }]
  })
}

watch(() => layers, () => {
  updateMarkers()
}, { deep: true })

watch(viewMode, (newMode) => {
  if (newMode === 'overview') {
    map?.setView([30.65, 104.10], 12)
  }
})

onMounted(() => {
  nextTick(() => {
    initMap()
    initHealthChart()
    initHazardPieChart()
  })

  window.addEventListener('resize', () => {
    healthChart?.resize()
    hazardPieChart?.resize()
  })
})

onUnmounted(() => {
  if (map) {
    map.remove()
    map = null
  }
  healthChart?.dispose()
  hazardPieChart?.dispose()
})
</script>

<style scoped>
.dashboard-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #f0f2f5;
  position: relative;
}

.dashboard-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
}

.search-input {
  width: 240px;
}

.dashboard-main {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.left-panel {
  width: 320px;
  padding: 12px;
  overflow-y: auto;
  background: #f5f7fa;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.stat-card {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  overflow: hidden;
}

.stat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  background: linear-gradient(135deg, #fafafa 0%, #f5f5f5 100%);
}

.stat-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f1f1f;
}

.stat-count {
  font-size: 12px;
  color: #999;
}

.stat-body {
  padding: 16px;
}

.health-ring {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-bottom: 12px;
}

.ring-chart {
  width: 80px;
  height: 80px;
}

.ring-value {
  font-size: 28px;
  font-weight: bold;
  color: #1890ff;
}

.health-detail {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.detail-item .label {
  font-size: 12px;
  color: #666;
}

.pie-chart {
  width: 100%;
  height: 120px;
}

.pie-legend {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-top: 8px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #666;
}

.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.dot.monitoring { background: #52c41a; }
.dot.paused { background: #faad14; }
.dot.completed { background: #1890ff; }

.legend-item .value {
  font-weight: 600;
  color: #333;
}

.device-online {
  text-align: center;
  margin-bottom: 12px;
}

.online-rate {
  font-size: 32px;
  font-weight: bold;
  color: #52c41a;
}

.online-text {
  font-size: 12px;
  color: #999;
}

.online-detail {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-bottom: 8px;
}

.online-stat {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #666;
}

.online-total {
  text-align: center;
  font-size: 12px;
  color: #999;
}

.alarm-card .stat-body {
  padding: 12px 16px;
}

.alarm-summary {
  display: flex;
  justify-content: space-around;
  margin-bottom: 12px;
}

.alarm-stat {
  text-align: center;
}

.alarm-num {
  display: block;
  font-size: 24px;
  font-weight: bold;
}

.alarm-num.pending { color: #f5222d; }
.alarm-num.history { color: #666; }

.alarm-label {
  font-size: 12px;
  color: #999;
}

.alarm-levels {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
  padding: 8px 12px;
  background: #fafafa;
  border-radius: 6px;
}

.level-item {
  text-align: center;
}

.level-count {
  display: block;
  font-size: 16px;
  font-weight: bold;
}

.level-label {
  font-size: 11px;
  color: #999;
}

.level-red .level-count { color: #f5222d; }
.level-orange .level-count { color: #fa8c16; }
.level-yellow .level-count { color: #fadb14; }
.level-blue .level-count { color: #1890ff; }

.alarm-list {
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  overflow: hidden;
}

.alarm-list-header {
  padding: 8px 12px;
  background: #fafafa;
  font-size: 12px;
  font-weight: 600;
  color: #333;
  border-bottom: 1px solid #f0f0f0;
}

.alarm-list-body {
  max-height: 160px;
  overflow-y: auto;
}

.alarm-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-bottom: 1px solid #f5f5f5;
  cursor: pointer;
  transition: background 0.2s;
}

.alarm-item:last-child {
  border-bottom: none;
}

.alarm-item:hover {
  background: #f5f7fa;
}

.alarm-icon {
  width: 12px;
  height: 12px;
  border-radius: 50%;
}

.alarm-icon.icon-red { background: #f5222d; }
.alarm-icon.icon-orange { background: #fa8c16; }
.alarm-icon.icon-yellow { background: #fadb14; }
.alarm-icon.icon-blue { background: #1890ff; }

.alarm-name {
  flex: 1;
  font-size: 12px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.alarm-time {
  font-size: 11px;
  color: #999;
}

.hazard-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
}

.info-row .label {
  color: #999;
}

.info-row .value {
  color: #333;
}

.status-tag {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.status-tag.monitoring {
  background: #f6ffed;
  color: #52c41a;
  border: 1px solid #b7eb8f;
}

.status-tag.paused {
  background: #fffbe6;
  color: #faad14;
  border: 1px solid #ffe58f;
}

.status-tag.completed {
  background: #e6f7ff;
  color: #1890ff;
  border: 1px solid #91d5ff;
}

.device-filter {
  margin-bottom: 12px;
}

.device-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 180px;
  overflow-y: auto;
}

.device-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  background: #fafafa;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.device-item:hover {
  background: #e6f7ff;
  transform: translateX(4px);
}

.device-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.device-name {
  font-size: 13px;
  color: #333;
  font-weight: 500;
}

.device-type {
  font-size: 11px;
  color: #999;
}

.device-status {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
}

.device-status.monitoring {
  background: #f6ffed;
  color: #52c41a;
}

.device-status.paused {
  background: #fffbe6;
  color: #faad14;
}

.device-status.completed {
  background: #e6f7ff;
  color: #1890ff;
}

.map-container {
  flex: 1;
  position: relative;
}

.right-panel {
  position: absolute;
  right: 16px;
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  flex-direction: column;
  gap: 12px;
  z-index: 1000;
}

.layer-control,
.map-tools {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  padding: 12px;
}

.layer-title,
.tools-title {
  font-size: 13px;
  font-weight: 600;
  color: #333;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
}

.layer-list,
.tools-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.layer-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #666;
}

.tool-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 18px;
  color: #666;
}

.tool-btn:hover {
  background: #1890ff;
  color: #fff;
}

.map-legend {
  position: absolute;
  bottom: 24px;
  left: 50%;
  transform: translateX(-50%);
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.15);
  padding: 12px 20px;
  z-index: 1000;
}

.legend-title {
  font-size: 13px;
  font-weight: 600;
  color: #333;
  margin-bottom: 10px;
  text-align: center;
}

.legend-items {
  display: flex;
  gap: 20px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #666;
}

.legend-color {
  width: 14px;
  height: 14px;
  border-radius: 50%;
}

.legend-color.level-red { background: #f5222d; }
.legend-color.level-orange { background: #fa8c16; }
.legend-color.level-yellow { background: #fadb14; }
.legend-color.level-blue { background: #1890ff; }

.bubble-container {
  position: absolute;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
  padding: 12px 16px;
  z-index: 10000;
  min-width: 180px;
  pointer-events: none;
}

.bubble-title {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin-bottom: 8px;
  padding-bottom: 6px;
  border-bottom: 1px solid #f0f0f0;
}

.bubble-content {
  font-size: 13px;
}
</style>

<style>
.leaflet-container {
  height: 100%;
  width: 100%;
}
</style>