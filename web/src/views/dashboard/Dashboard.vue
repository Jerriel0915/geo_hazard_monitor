<template>
  <div class="dashboard-container">
    <div ref="mapContainer" class="map-container" :style="{ '--scale-left': devicePanelLeftOffset + 12 + 'px' }"></div>

    <!-- 隐患点视图 / 系统视图选中隐患点 顶部标题栏 -->
    <div v-if="currentView === 'hazard' || (currentView === 'system' && currentHazardPoint)" class="hazard-view-header">
      <div class="hazard-title-wrapper">
        <div class="hazard-title" @click="showHazardList = !showHazardList">
          <span class="hazard-name">{{ currentHazardPoint?.name }}</span>
          <span class="hazard-dropdown-arrow">▼</span>
        </div>
        <div v-show="showHazardList" class="hazard-list-dropdown">
          <div
              v-for="point in hazardPoints"
              :key="point.id"
              class="hazard-list-item"
              :class="{ active: currentHazardPoint?.id === point.id }"
              @click="selectHazardPoint(point)"
          >
            {{ point.name }}
          </div>
        </div>
      </div>
      <button v-if="currentView === 'hazard'" class="close-hazard-view-btn" @click="exitHazardView" title="返回系统视图">
        <el-icon :size="18"><Close/></el-icon>
      </button>
      <button v-else class="close-hazard-view-btn" @click="clearHazardSelection" title="清除选择">
        <el-icon :size="18"><Close/></el-icon>
      </button>
    </div>

    <!-- 设备数据面板 (底部) -->
    <DeviceDataPanel
      v-if="selectedDevice"
      :device="selectedDevice"
      :hazard-point-id="currentHazardPoint?.id"
      :left-offset="devicePanelLeftOffset"
      :right-offset="devicePanelRightOffset"
      @close="closeDevicePanel"
    />

    <div class="left-panel-wrapper" :class="{ collapsed: isPanelCollapsed || !hasLeftContent }">
      <button class="panel-collapse-trigger-left" @click="togglePanel">
        <el-icon :size="12"><component :is="isPanelCollapsed ? ArrowRight : ArrowLeft"/></el-icon>
      </button>
      <div class="left-panel">
        <!-- 隐患点详情（hazard视图 或 system视图选中隐患点） -->
        <div v-if="currentView === 'hazard' || currentHazardPoint" class="panel-content">
          <HazardDetailWidget
            :hazard-point="currentHazardPoint"
            @device-click="openDeviceDataModal"
          />
        </div>
        <!-- 默认系统视图：Widget 布局 -->
        <div v-else class="panel-content">
          <HealthWidget v-if="isWidgetOnLeft('systemHealth')" :health-stats="healthStats" />
          <ResourceWidget v-if="isWidgetOnLeft('assetInfo')" :resource-stats="resourceStats" />
          <AlarmWidget v-if="isWidgetOnLeft('alarmStatus')" :alarm-stats="alarmStats" />
          <DeviceStatusWidget v-if="isWidgetOnLeft('deviceStatus')" :stats="deviceStatusStats" :trend-data="deviceOnlineTrend" />
        </div>
      </div>
    </div>


    <!-- 业务工具条 (右侧面板旁) -->
    <MapBusinessToolbar
      :hazard-points="hazardPoints"
      :mask-visible="showMaskLayer"
      :legend-visible="showLegend"
      :layout-dialog-visible="showLayoutDialog"
      :right-panel-collapsed="isRightPanelCollapsed || !hasRightContent"
      :groups="hazardPointGroups"
      @select-hazard-point="enterHazardView"
      @toggle-layers="handleLayerToggle"
      @open-layout-config="showLayoutDialog = true"
      @toggle-mask="toggleMaskLayer"
      @toggle-legend="showLegend = !showLegend"
      @reset-view="resetMapView"
    />

    <!-- 右侧面板 -->
    <div class="right-panel-wrapper" :class="{ collapsed: isRightPanelCollapsed || !hasRightContent }">
      <button class="panel-collapse-trigger-right" @click="toggleRightPanel">
        <el-icon :size="12"><component :is="isRightPanelCollapsed ? ArrowLeft : ArrowRight"/></el-icon>
      </button>
      <div class="right-panel">
        <!-- 隐患点视图：显示告警情况 + 实时警情 -->
        <HazardAlarmWidget v-if="currentView === 'hazard' || currentHazardPoint" class="panel-content" :hazard-point-id="currentHazardPoint?.id ?? null" />
        <!-- 默认系统视图：Widget 布局 -->
        <template v-else>
          <div class="panel-content panel-content-scroll">
            <HealthWidget v-if="isWidgetOnRight('systemHealth')" :health-stats="healthStats" />
            <ResourceWidget v-if="isWidgetOnRight('assetInfo')" :resource-stats="resourceStats" />
            <AlarmWidget v-if="isWidgetOnRight('alarmStatus')" :alarm-stats="alarmStats" />
            <DeviceStatusWidget v-if="isWidgetOnRight('deviceStatus')" :stats="deviceStatusStats" :trend-data="deviceOnlineTrend" />
          </div>
        </template>
      </div>
    </div>

    <!-- 面板布局配置弹窗 -->
    <LayoutConfigDialog
      v-model:visible="showLayoutDialog"
      :layout="layoutConfig"
      @update:layout="handleLayoutUpdate"
    />

    <!-- 地图辅助工具条 (底部: 比例尺+底图+图例) -->
    <MapAuxiliaryBar :current-layer="currentLayer" :monitor-types="monitorTypes" :right-panel-offset="devicePanelRightOffset" :legend-visible="showLegend" @update:current-layer="handleLayerSelect" />
  </div>
</template>

<script setup lang="ts">
import { getBoundDevices, getHazardPointGroups, getHazardPointPage } from '@/api/hazardPoint'
import { getDashboardFull } from '@/api/monitor'
import { getMonitorTypeList, type MonitorTypeItem } from '@/api/monitorType'
import { getRealtimeAlarmPage, type RealtimeAlarmDetail } from '@/api/realtimeAlarm'
import {
  ArrowLeft,
  ArrowRight,
  Close,
  Drizzling,
  Monitor,
  Odometer,
  Sunny
} from '@element-plus/icons-vue'
import 'cn-fontsource-ding-talk-jin-bu-ti-regular/font.css'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { computed, onMounted, onUnmounted, ref } from 'vue'
import AlarmWidget from './components/AlarmWidget.vue'
import DeviceDataPanel from './components/DeviceDataPanel.vue'
import HazardAlarmWidget from './components/HazardAlarmWidget.vue'
import HazardDetailWidget from './components/HazardDetailWidget.vue'
import DeviceStatusWidget from './components/DeviceStatusWidget.vue'
import HealthWidget from './components/HealthWidget.vue'
import LayoutConfigDialog from './components/LayoutConfigDialog.vue'
import MapAuxiliaryBar from './components/MapAuxiliaryBar.vue'
import MapBusinessToolbar from './components/MapBusinessToolbar.vue'
import ResourceWidget from './components/ResourceWidget.vue'

const getDeviceTypeIcon = (type: string) => {
  switch (type) {
    case 'GNSS':
      return Monitor
    case 'RAIN':
      return Sunny
    case 'PRESSURE':
      return Drizzling
    default:
      return Odometer
  }
}

const mapContainer = ref<HTMLDivElement | null>(null)
let mapInstance: L.Map | null = null
let baseLayer: L.TileLayer | null = null
let labelLayer: L.TileLayer | null = null
let isLabelVisible = true
const activeLayerKeys = ref<string[]>([])
const hazardPointDataMap = new Map<number, any>()

const TIANDI_TU_KEY = '8dda07d4649c77efd0537a0ff0a1df13'

const layerOptions = [
  {
    id: 'image',
    name: '影像图',
    color: '#87CEEB',
    baseUrl: 'img_w',
    baseLayer: 'img',
    labelUrl: 'cia_w',
    labelLayer: 'cia'
  },
  {
    id: 'vector',
    name: '矢量图',
    color: '#90EE90',
    baseUrl: 'vec_w',
    baseLayer: 'vec',
    labelUrl: 'cva_w',
    labelLayer: 'cva'
  },
  {
    id: 'terrain',
    name: '地形图',
    color: '#DEB887',
    baseUrl: 'ter_w',
    baseLayer: 'ter',
    labelUrl: 'cta_w',
    labelLayer: 'cta'
  }
]

const currentLayer = ref('image')
const showLayerList = ref(false)
const isPanelCollapsed = ref(false)
const currentLayerName = ref('影像图')

const healthStats = ref({
  overallScore: 0,
  items: [
    {name: '资料完善率', value: 0, weight: 0.2, color: '#52c41a'},
    {name: '设备在线率', value: 0, weight: 0.15, color: '#1890ff'},
    {name: '设备正常率', value: 0, weight: 0.15, color: '#722ed1'},
    {name: '告警及时响应率', value: 0, weight: 0.2, color: '#fa8c16'},
    {name: '边坡稳定率', value: 0, weight: 0.3, color: '#eb2f96'}
  ]
})

const resourceStats = ref({
  totalResources: 0,
  deviceTotal: 0,
  hazardTotal: 0,
  deviceTypes: [
    {name: 'GNSS接收机', count: 0},
    {name: '雨量计', count: 0},
    {name: '渗压计', count: 0},
    {name: '位移计', count: 0},
    {name: '视频设备', count: 0}
  ]
})

const deviceStatusStats = ref({
  onlineRate: 0,
  onlineCount: 0,
  totalCount: 0,
  typeStats: [] as { name: string; online: number; total: number }[]
})
const deviceOnlineTrend = ref<number[]>([])

const isRightPanelCollapsed = ref(false)

// ========== 面板布局配置 ==========
const DEFAULT_LAYOUT = {
  left: ['systemHealth', 'assetInfo'],
  right: ['alarmStatus'],
  hidden: []
}

const showLayoutDialog = ref(false)
const layoutConfig = ref<{ left: string[]; right: string[]; hidden: string[] }>(DEFAULT_LAYOUT)

const handleLayoutUpdate = (layout: { left: string[]; right: string[]; hidden: string[] }) => {
  layoutConfig.value = layout
}

const isWidgetOnLeft = (key: string) => layoutConfig.value.left.includes(key)
const isWidgetOnRight = (key: string) => layoutConfig.value.right.includes(key)

// 面板内容检测：当面板无可见部件时自动折叠
const hasLeftContent = computed(() => {
  return !!currentHazardPoint.value || isWidgetOnLeft('systemHealth') || isWidgetOnLeft('assetInfo') || isWidgetOnLeft('alarmStatus') || isWidgetOnLeft('deviceStatus')
})
const hasRightContent = computed(() => {
  return isWidgetOnRight('systemHealth') || isWidgetOnRight('assetInfo') || isWidgetOnRight('alarmStatus') || isWidgetOnRight('deviceStatus')
})

// ========== /面板布局配置 ==========

// 视图模式
const currentView = ref<'system' | 'hazard'>('system')
const currentHazardPoint = ref<typeof hazardPoints.value[0] | null>(null)
const showHazardList = ref(false)

let savedMapView: { center: [number, number]; zoom: number } | null = null
const widgetBoundDevices = ref<any[]>([])
const widgetDevicesLoading = ref(false)

// 工具按钮状态 (search/layer/legend panels moved to BottomToolbar component)
const flashingPointId = ref<number | null>(null)

// 设备列表
const deviceList = ref<any[]>([])

// 传感器列表
const sensorList = ref<any[]>([])
const selectedDevice = ref<typeof deviceList.value[0] | null>(null)
const selectedSensor = ref<any | null>(null)

// 设备数据面板偏移量：跟随左右面板折叠状态
const devicePanelLeftOffset = computed(() => {
  if (isPanelCollapsed.value || !hasLeftContent.value) return 24  // trigger
  return 12 + 320 + 24  // left + panel + trigger
})
const devicePanelRightOffset = computed(() => {
  if (isRightPanelCollapsed.value || !hasRightContent.value) return 24
  return 12 + 320 + 24
})
const showSensorChart = ref(false)
const sensorChartData = ref<number[]>([])

// 蒙层显示状态
const showMaskLayer = ref(false)
const showLegend = ref(true)

const generateSensorData = () => {
  return []
}

// 图层设置 (moved to BottomToolbar component)

const focusAreaBounds = ref<[number, number][]>([
  [30.60, 104.00],
  [30.70, 104.15],
  [30.65, 104.25],
  [30.55, 104.20],
  [30.60, 104.00]
])

// 隐患点列表（从API获取）
const hazardPoints = ref<any[]>([])
const hazardPointGroups = ref<any[]>([])
const monitorTypes = ref<MonitorTypeItem[]>([])

let maskLayer: L.GeoJSON | null = null
let boundaryLayer: L.Polyline | null = null
let hazardMarkerLayer: L.LayerGroup | null = null
let hazardMarkerMap: Map<number, L.Marker> = new Map()
let ripples: Map<number, L.Circle[]> = new Map()

const alarmColors: Record<string, string> = {
  critical: '#f5222d',
  major: '#faad14',
  minor: '#722ed1',
  info: '#1890ff'
}

const alarmStats = ref<{
  pendingCount: number
  historyCount: number
  levelStats: { key: string; name: string; count: number; icon: string }[]
  recentAlarms: { id: number; level: string; title: string; source: string; time: string }[]
}>({
  pendingCount: 0,
  historyCount: 0,
  levelStats: [
    {key: 'critical', name: '严重', count: 0, icon: '/img/alarm/level1.png'},
    {key: 'major', name: '重要', count: 0, icon: '/img/alarm/level2.png'},
    {key: 'minor', name: '一般', count: 0, icon: '/img/alarm/level3.png'},
    {key: 'info', name: '提示', count: 0, icon: '/img/alarm/level4.png'}
  ],
  recentAlarms: []
})

const togglePanel = () => {
  isPanelCollapsed.value = !isPanelCollapsed.value
}

const resetMapView = () => {
  if (currentHazardPoint.value && mapInstance) {
    const center: [number, number] = [currentHazardPoint.value.latitude, currentHazardPoint.value.longitude]
    const circleBounds = L.latLng(center).toBounds(1000) // radius=500 → diameter=1000m
    mapInstance.fitBounds(circleBounds, { padding: [40, 40], animate: true, maxZoom: 16 })
  } else {
    fitToFocusArea()
  }
}

const initMap = () => {
  if (!mapContainer.value) return

  mapInstance = L.map(mapContainer.value, {
    center: [30.67, 104.06],
    zoom: 10,
    zoomControl: false,
    attributionControl: false
  })

  addLayer('image')

  L.control.scale({
    maxWidth: 150,
    metric: true,
    imperial: false,
    position: 'bottomright'
  }).addTo(mapInstance)

  addFocusBoundary()
  addMaskLayer()
  // 默认关闭蒙层和边界线
  toggleMaskLayer()
  addHazardPoints()

  fitToFocusArea()
}

const fitToFocusArea = () => {
  if (!mapInstance || !focusAreaBounds.value.length) return

  const bounds = L.latLngBounds(focusAreaBounds.value)
  mapInstance.fitBounds(bounds, {
    padding: [20, 20],
    animate: false,
    maxZoom: 14
  })
}

const addFocusBoundary = () => {
  if (!mapInstance) return

  boundaryLayer = L.polyline(focusAreaBounds.value, {
    color: '#f5222d',
    weight: 3,
    opacity: 1,
    dashArray: '8,4'
  }).addTo(mapInstance)
}

const addMaskLayer = () => {
  if (!mapInstance) return

  const southWest = L.latLng(30.40, 103.80)
  const northEast = L.latLng(30.90, 104.40)
  const mapBounds = L.latLngBounds(southWest, northEast)
  const outerRing: [number, number][] = [
    [mapBounds.getWest(), mapBounds.getSouth()],
    [mapBounds.getEast(), mapBounds.getSouth()],
    [mapBounds.getEast(), mapBounds.getNorth()],
    [mapBounds.getWest(), mapBounds.getNorth()],
    [mapBounds.getWest(), mapBounds.getSouth()]
  ]
  const innerRing: [number, number][] = focusAreaBounds.value.map(([lat, lng]) => [lng, lat])

  const maskGeoJson = {
    type: 'Polygon' as const,
    coordinates: [outerRing, innerRing]
  }

  maskLayer = L.geoJSON(maskGeoJson, {
    style: {
      fillColor: '#000000',
      fillOpacity: 0.35,
      color: 'transparent',
      weight: 0
    }
  }).addTo(mapInstance)
}

const toggleMaskLayer = () => {
  showMaskLayer.value = !showMaskLayer.value
  if (maskLayer) {
    if (showMaskLayer.value) {
      maskLayer.setStyle({
        fillColor: '#000000',
        fillOpacity: 0.35,
        color: 'transparent',
        weight: 0
      })
    } else {
      maskLayer.setStyle({
        fillColor: 'transparent',
        fillOpacity: 0,
        color: 'transparent',
        weight: 0
      })
    }
  }
  if (boundaryLayer) {
    boundaryLayer.setStyle({ opacity: showMaskLayer.value ? 1 : 0 })
  }
}

const createHazardIcon = (hasAlarm: boolean) => {
  const iconUrl = hasAlarm ? '/img/sy/auto_unnormal.png' : '/img/sy/auto_normal.png'
  return L.icon({
    iconUrl,
    iconSize: [32, 40],
    iconAnchor: [16, 40],
    popupAnchor: [0, -40]
  })
}

const addHazardPoints = () => {
  if (!mapInstance) return

  const markerLayer = L.layerGroup().addTo(mapInstance)
  hazardMarkerLayer = markerLayer

  hazardPoints.value.forEach(point => {
    const hasAlarm = !!point.alarmLevel
    const marker = L.marker([point.latitude, point.longitude], {
      icon: createHazardIcon(hasAlarm)
    }).addTo(markerLayer)

    // 存储marker引用
    hazardMarkerMap.set(point.id, marker)

    // 存储数据用于过滤
    hazardPointDataMap.set(point.id, point)

    if (point.alarmLevel) {
      startRipple(point)
    }

    const alarmLvMap: Record<string, { text: string; bg: string; color: string }> = {
      critical: { text: '严重', bg: 'rgba(245,34,45,0.1)', color: '#f5222d' },
      major: { text: '重要', bg: 'rgba(250,173,20,0.1)', color: '#fa8c16' },
      minor: { text: '一般', bg: 'rgba(250,215,64,0.1)', color: '#d4a017' },
      info: { text: '提示', bg: 'rgba(82,196,26,0.1)', color: '#52c41a' }
    }
    const alv = alarmLvMap[point.alarmLevel] || { text: point.alarmLevel||'--', bg: 'rgba(24,144,255,0.1)', color: '#1890ff' }
    const desc = point.description ? `<div class="hpv2-dash"></div><div class="hpv2-row single"><div class="hpv2-cell full"><span class="hpv2-label">描述</span><span class="hpv2-val">${point.description}</span></div></div>` : ''

    const popupContent = `
      <div class="hpv2-card">
        <div class="hpv2-header"><span class="hpv2-title">${point.name}</span></div>
        <div class="hpv2-dash"></div>
        <div class="hpv2-body">
          <div class="hpv2-row">
            <div class="hpv2-cell"><span class="hpv2-label">编号</span><span class="hpv2-val">${point.code}</span></div>
            <div class="hpv2-cell"><span class="hpv2-label">分组</span><span class="hpv2-val">${point.groupName||'--'}</span></div>
          </div>
          <div class="hpv2-dash"></div>
          <div class="hpv2-row">
            <div class="hpv2-cell"><span class="hpv2-label">坐标</span><span class="hpv2-val">${point.latitude.toFixed(4)}, ${point.longitude.toFixed(4)}</span></div>
            <div class="hpv2-cell"><span class="hpv2-label">设备</span><span class="hpv2-val">${point.deviceCount} 台</span></div>
          </div>
          <div class="hpv2-dash"></div>
          <div class="hpv2-row single">
            <div class="hpv2-cell full">
              <span class="hpv2-label">预警等级</span>
              <span class="hpv2-level" style="background:${alv.bg};color:${alv.color}">${alv.text}</span>
            </div>
          </div>
          ${desc}
        </div>
      </div>
    `

    marker.bindPopup(popupContent, {
      maxWidth: 240,
      closeButton: false,
      autoClose: true
    })

    marker.on('click', () => {
      onHazardMarkerClick(point)
    })

    marker.on('dblclick', () => {
      enterHazardView(point)
    })

    marker.on('mouseover', () => {
      marker.openPopup()
    })

    marker.on('mouseout', () => {
      marker.closePopup()
    })
  })
}

const startRipple = (point: typeof hazardPoints.value[0]) => {
  if (!mapInstance || !point.alarmLevel) return

  const color = alarmColors[point.alarmLevel]
  const center: [number, number] = [point.latitude, point.longitude]
  const rippleCount = 3
  const rippleDelay = 1500

  const circles: L.Circle[] = []

  for (let i = 0; i < rippleCount; i++) {
    const delay = i * rippleDelay

    setTimeout(() => {
      const ripple = L.circle(center, {
        radius: 10,
        fillColor: color,
        color: color,
        weight: 2,
        opacity: 0.8,
        fillOpacity: 0.1
      }).addTo(mapInstance!)

      circles.push(ripple)

      const rippleElement = ripple.getElement()
      if (rippleElement instanceof HTMLElement || rippleElement instanceof SVGElement) {
        rippleElement.style.setProperty('transition', 'all 2s ease-out')
      }

      setTimeout(() => {
        ripple.setRadius(60)
        ripple.setStyle({
          opacity: 0,
          fillOpacity: 0
        })
      }, 50)

      setTimeout(() => {
        if (mapInstance && ripple) {
          mapInstance.removeLayer(ripple)
          const idx = circles.indexOf(ripple)
          if (idx > -1) circles.splice(idx, 1)
        }
      }, 2500)
    }, delay)
  }

  ripples.set(point.id, circles)

  const repeatRipple = () => {
    if (!ripples.has(point.id)) return

    setTimeout(() => {
      startRipple(point)
    }, rippleCount * rippleDelay)
  }

  repeatRipple()
}

// ========== 隐患点详情部件 - 软选择逻辑 ==========

const onHazardMarkerClick = async (point: typeof hazardPoints.value[0]) => {
  // Save current map view before flying to point
  if (mapInstance) {
    const center = mapInstance.getCenter()
    savedMapView = { center: [center.lat, center.lng], zoom: mapInstance.getZoom() }
  }

  currentView.value = 'hazard'

  currentHazardPoint.value = point

  // 地图缩放到隐患点
  if (mapInstance) {
    mapInstance.setView([point.latitude, point.longitude], 14)
  }

  // 地图上：隐藏所有隐患点，绘制区域+设备
  await showHazardOnMap(point)

  // 侧边栏：加载绑定设备
  await loadWidgetDevices(point.id)
}

const showHazardOnMap = async (point: typeof hazardPoints.value[0]) => {
  if (!mapInstance) return

  // 清除现有地图标记
  if (hazardMarkerLayer) {
    mapInstance.removeLayer(hazardMarkerLayer)
  }
  hazardMarkerLayer = L.layerGroup().addTo(mapInstance)

  // 绘制隐患点范围
  L.circle([point.latitude, point.longitude], {
    radius: 500,
    color: '#f5222d',
    fillColor: '#f5222d',
    fillOpacity: 0.1,
    weight: 2,
    dashArray: '8,4'
  }).addTo(hazardMarkerLayer)

  // 加载并绘制设备标记
  try {
    const response = await getBoundDevices(String(point.id))
    if (response.code === 200 && response.data) {
      const devices = (response.data as any[]).map((item: any) => ({
        id: item.deviceId,
        name: item.deviceName || '未知设备',
        type: (item.sensors?.[0]?.name || 'DEVICE').toUpperCase(),
        typeName: item.sensors?.[0]?.name || '设备',
        status: item.deviceStatus === 0 ? 'online' : item.deviceStatus === 1 ? 'warning' : 'offline',
        sensorCount: item.sensors?.length || 0,
        longitude: item.installLongitude ?? point.longitude,
        latitude: item.installLatitude ?? point.latitude
      }))

      devices.forEach(device => {
        const icon = createDeviceIcon(device.status)
        L.marker([device.latitude, device.longitude], {icon})
            .addTo(hazardMarkerLayer!)
            .bindPopup(`<div class="hpv2-card">
            <div class="hpv2-header"><span class="hpv2-title">${device.name}</span></div>
            <div class="hpv2-dash"></div>
            <div class="hpv2-body">
              <div class="hpv2-row single">
                <div class="hpv2-cell full"><span class="hpv2-label">类型</span><span class="hpv2-val">${device.typeName}</span></div>
              </div>
              <div class="hpv2-dash"></div>
              <div class="hpv2-row single">
                <div class="hpv2-cell full"><span class="hpv2-label">传感器</span><span class="hpv2-val">${device.sensorCount} 个</span></div>
              </div>
              <div class="hpv2-dash"></div>
              <div class="hpv2-row single">
                <div class="hpv2-cell full"><span class="hpv2-label">状态</span><span class="hpv2-val">${getStatusText(device.status)}</span></div>
              </div>
            </div>
          </div>`)
      })
    }
  } catch (error) {
    console.error('加载设备标记失败:', error)
  }
}

const restoreHazardMarkers = () => {
  refreshHazardMarkers()
  fitToFocusArea()
}

const loadWidgetDevices = async (hazardPointId: number) => {
  widgetDevicesLoading.value = true
  widgetBoundDevices.value = []
  try {
    const response = await getBoundDevices(String(hazardPointId))
    if (response.code === 200 && response.data) {
      widgetBoundDevices.value = (response.data as any[]).map((item: any) => ({
        id: item.deviceId,                                              // 设备ID (非绑定记录ID)
        name: item.deviceName || '未知设备',
        type: (item.sensors?.[0]?.name || 'DEVICE').toUpperCase(),      // 从传感器名推断类型
        typeName: item.sensors?.[0]?.name || '设备',
        status: item.deviceStatus === 0 ? 'online' : item.deviceStatus === 1 ? 'warning' : 'offline',
        sensorCount: item.sensors?.length || 0,
        longitude: item.installLongitude ?? 0,
        latitude: item.installLatitude ?? 0
      }))
    }
  } catch (error) {
    console.error('加载部件设备列表失败:', error)
    widgetBoundDevices.value = []
  } finally {
    widgetDevicesLoading.value = false
  }
}

const openHazardDetailFromWidget = () => {
  if (currentHazardPoint.value) {
    enterHazardView(currentHazardPoint.value)
  }
}

const clearHazardSelection = () => {
  currentHazardPoint.value = null
  widgetBoundDevices.value = []
  widgetDevicesLoading.value = false
  // Restore all markers
  refreshHazardMarkers()
  // Restore map view
  if (mapInstance && savedMapView) {
    mapInstance.setView(savedMapView.center, savedMapView.zoom)
    savedMapView = null
  }
}

// ========== /隐患点详情部件 ==========

const handleResize = () => {
  if (mapInstance) {
    mapInstance.invalidateSize()
  }
}

const toggleRightPanel = () => {
  isRightPanelCollapsed.value = !isRightPanelCollapsed.value
}

// 工具按钮切换函数 (moved to BottomToolbar component)

const startPointFlash = (pointId: number) => {
  if (!mapInstance) return

  const marker = hazardMarkerMap.get(pointId)
  if (!marker) return

  flashingPointId.value = pointId
  let flashCount = 0
  const maxFlashes = 6

  const flashInterval = setInterval(() => {
    flashCount++

    // Toggle marker opacity to simulate flash
    const el = marker.getElement()
    if (el) {
      el.style.opacity = flashCount % 2 === 0 ? '1' : '0.2'
    }

    if (flashCount >= maxFlashes) {
      clearInterval(flashInterval)
      const el2 = marker.getElement()
      if (el2) el2.style.opacity = '1'
      flashingPointId.value = null
    }
  }, 300)
}

// 图层控制
const handleLayerToggle = (activeKeys: string[]) => {
  const keySet = new Set(activeKeys)

  // 标注层
  if (keySet.has('showLabels')) {
    if (!labelLayer) addLabelOverlay()
    isLabelVisible = true
  } else {
    if (labelLayer && mapInstance) {
      mapInstance.removeLayer(labelLayer)
      labelLayer = null
    }
    isLabelVisible = false
  }

  // Marker 过滤
  filterMarkers(keySet)

  activeLayerKeys.value = activeKeys
}

const filterMarkers = (keySet: Set<string>) => {
  // 提取活跃分组 ID
  const activeGroups = new Set<number>()
  for (const key of keySet) {
    if (key.startsWith('group_')) {
      activeGroups.add(Number(key.slice(6)))
    }
  }

  // 提取活跃状态
  const activeStatuses = new Set<string>()
  if (keySet.has('showMonitoring')) activeStatuses.add('MONITORING')
  if (keySet.has('showStopped')) activeStatuses.add('PAUSED')
  if (keySet.has('showCompleted')) activeStatuses.add('COMPLETED')

  const hasGroups = activeGroups.size > 0
  const hasStatuses = activeStatuses.size > 0

  hazardMarkerMap.forEach((marker, pointId) => {
    const data = hazardPointDataMap.get(pointId)
    if (!data) return

    const groupOk = hasGroups && activeGroups.has(data.groupId)
    const statusOk = hasStatuses && activeStatuses.has(data.status)
    const visible = groupOk && statusOk

    const el = marker.getElement()
    if (el) el.style.display = visible ? '' : 'none'
  })
}

const refreshHazardMarkers = () => {
  if (mapInstance && hazardMarkerLayer) {
    hazardMarkerLayer.clearLayers()
    addHazardPoints()
    filterMarkers(new Set(activeLayerKeys.value))
  }
}

// 隐患点视图相关函数
const enterHazardView = (hazardPoint: typeof hazardPoints.value[0]) => {
  currentView.value = 'hazard'
  currentHazardPoint.value = hazardPoint
  showHazardList.value = false
  selectedDevice.value = null
  selectedSensor.value = null
  showSensorChart.value = false
  sensorList.value = []

  // 清除部件软选择状态（不恢复地图/标记，因为即将切换视图）
  widgetBoundDevices.value = []
  widgetDevicesLoading.value = false
  savedMapView = null

  // 更新告警统计（限定到当前隐患点）
  updateHazardAlarms(hazardPoint.id)

  // 地图聚焦到隐患点
  if (mapInstance) {
    mapInstance.setView([hazardPoint.latitude, hazardPoint.longitude], 14)
  }

  // 添加设备标记
  addDeviceMarkers(hazardPoint.id)
}

const exitHazardView = () => {
  currentView.value = 'system'
  currentHazardPoint.value = null
  selectedDevice.value = null
  selectedSensor.value = null
  showSensorChart.value = false
  sensorList.value = []

  // 恢复告警统计（全系统）
  resetAlarmStats()

  // 恢复隐患点显示
  refreshHazardMarkers()
  fitToFocusArea()
}

const updateHazardAlarms = (hazardId: number) => {
  // TODO: 从API获取当前隐患点的告警数据
  alarmStats.value.pendingCount = 0
  alarmStats.value.historyCount = 0
  alarmStats.value.levelStats = [
    {key: 'critical', name: '严重', count: 0, icon: '/img/alarm/level1.png'},
    {key: 'major', name: '重要', count: 0, icon: '/img/alarm/level2.png'},
    {key: 'minor', name: '一般', count: 0, icon: '/img/alarm/level3.png'},
    {key: 'info', name: '提示', count: 0, icon: '/img/alarm/level4.png'}
  ]
  alarmStats.value.recentAlarms = []
}

const resetAlarmStats = () => {
  loadDashboardData()
}

const selectHazardPoint = (hazardPoint: typeof hazardPoints.value[0]) => {
  showHazardList.value = false

  if (currentView.value === 'hazard') {
    currentHazardPoint.value = hazardPoint
    updateHazardAlarms(hazardPoint.id)

    if (mapInstance) {
      mapInstance.setView([hazardPoint.latitude, hazardPoint.longitude], 14)
    }

    addDeviceMarkers(hazardPoint.id)
  } else {
    // System view: treat as soft selection via top-center dropdown
    onHazardMarkerClick(hazardPoint)
  }
}

const addDeviceMarkers = async (hazardId: number) => {
  if (!mapInstance) return

  // 清除现有标记
  if (hazardMarkerLayer) {
    mapInstance.removeLayer(hazardMarkerLayer)
  }

  hazardMarkerLayer = L.layerGroup().addTo(mapInstance)

  // 绘制隐患点范围
  const hazardArea = L.circle([currentHazardPoint.value!.latitude, currentHazardPoint.value!.longitude], {
    radius: 500,
    color: '#f5222d',
    fillColor: '#f5222d',
    fillOpacity: 0.1,
    weight: 2,
    dashArray: '8,4'
  }).addTo(hazardMarkerLayer)

  // 从API获取绑定设备列表
  try {
    const request = await import('@/utils/request')
    const response = await request.default.get(`/hazard-points/${hazardId}/bound-devices`)
    if (response.code === 200 && response.data) {
      const devices = response.data
      // 更新设备列表（BoundDeviceVO 字段: deviceId, deviceName, deviceStatus, installLongitude, installLatitude, sensors）
      deviceList.value = devices.map((item: any) => ({
        id: item.deviceId,
        name: item.deviceName || '未知设备',
        type: (item.sensors?.[0]?.name || 'DEVICE').toUpperCase(),
        typeName: item.sensors?.[0]?.name || '设备',
        status: item.deviceStatus === 0 ? 'online' : item.deviceStatus === 1 ? 'warning' : 'offline',
        sensorCount: item.sensors?.length || 0,
        longitude: item.installLongitude || currentHazardPoint.value!.longitude,
        latitude: item.installLatitude || currentHazardPoint.value!.latitude
      }))
      
      // 添加设备标记
      deviceList.value.forEach(device => {
        const icon = createDeviceIcon(device.status)
        const marker = L.marker([device.latitude, device.longitude], {icon})
            .addTo(hazardMarkerLayer!)
            .bindPopup(`<div class="hpv2-card">
            <div class="hpv2-header"><span class="hpv2-title">${device.name}</span></div>
            <div class="hpv2-dash"></div>
            <div class="hpv2-body">
              <div class="hpv2-row single">
                <div class="hpv2-cell full"><span class="hpv2-label">类型</span><span class="hpv2-val">${device.typeName}</span></div>
              </div>
              <div class="hpv2-dash"></div>
              <div class="hpv2-row single">
                <div class="hpv2-cell full"><span class="hpv2-label">传感器</span><span class="hpv2-val">${device.sensorCount} 个</span></div>
              </div>
              <div class="hpv2-dash"></div>
              <div class="hpv2-row single">
                <div class="hpv2-cell full"><span class="hpv2-label">状态</span><span class="hpv2-val">${getStatusText(device.status)}</span></div>
              </div>
            </div>
          </div>`)
      })
    }
  } catch (error) {
    console.error('加载设备列表失败:', error)
  }
}

const createDeviceIcon = (status: string) => {
  const color = status === 'online' ? '#52c41a' : status === 'warning' ? '#faad14' : '#f5222d'
  return L.divIcon({
    className: 'device-marker',
    html: `<div style="
      width: 24px;
      height: 24px;
      background: ${color};
      border: 2px solid white;
      border-radius: 50%;
      box-shadow: 0 2px 6px rgba(0,0,0,0.3);
      display: flex;
      align-items: center;
      justify-content: center;
      color: white;
      font-size: 12px;
    ">📡</div>`,
    iconSize: [24, 24],
    iconAnchor: [12, 12]
  })
}

const getStatusText = (status: string) => {
  return status === 'online' ? '在线' : status === 'warning' ? '预警' : '离线'
}

const selectDevice = (device: typeof deviceList.value[0]) => {
  selectedDevice.value = device
  selectedSensor.value = null
  showSensorChart.value = false

  // 生成传感器列表
  sensorList.value = []
  for (let i = 1; i <= device.sensorCount; i++) {
    sensorList.value.push({
      id: device.id * 100 + i,
      name: `${device.typeName}-传感器${i}`,
      code: `S${device.id.toString().padStart(3, '0')}-${i.toString().padStart(2, '0')}`,
      type: device.type,
      status: i === 1 ? 'warning' : 'online'
    })
  }
}

const selectSensor = (sensor: any) => {
  if (selectedSensor.value?.id === sensor.id) {
    showSensorChart.value = !showSensorChart.value
  } else {
    selectedSensor.value = sensor
    showSensorChart.value = true
    sensorChartData.value = generateSensorData()
  }
}

const getChartPoints = () => {
  return sensorChartData.value.map((point, index) => {
    const x = 40 + index * 40
    const y = 100 - point * 1.8
    return `${x},${y}`
  }).join(' ')
}

// 打开设备数据面板
const openDeviceDataModal = (device: typeof deviceList.value[0]) => {
  // 如果点击同一设备则关闭面板
  if (selectedDevice.value?.id === device.id) {
    selectedDevice.value = null
  } else {
    selectedDevice.value = device
  }
}

// 关闭设备数据面板
const closeDevicePanel = () => {
  selectedDevice.value = null
}

// 从设备列表返回到系统总览
const backToSystemView = () => {
  selectedDevice.value = null
  exitHazardView()
}

const handleLayerSelect = (layerId: string) => {
  switchLayer(layerId)
  showLayerList.value = false
}

const addLayer = (layerId: string) => {
  if (!mapInstance) return

  const layer = layerOptions.find(l => l.id === layerId)
  if (!layer) return

  if (baseLayer) {
    mapInstance.removeLayer(baseLayer)
  }
  if (labelLayer) {
    mapInstance.removeLayer(labelLayer)
    labelLayer = null
  }

  baseLayer = L.tileLayer(`https://t0.tianditu.gov.cn/${layer.baseUrl}/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=${layer.baseLayer}&STYLE=default&TILEMATRIXSET=w&FORMAT=tiles&TILEMATRIX={z}&TILEROW={y}&TILECOL={x}&tk=${TIANDI_TU_KEY}`, {
    maxZoom: 18,
    minZoom: 1
  }).addTo(mapInstance)

  if (isLabelVisible) {
    addLabelOverlay()
  }
}

const addLabelOverlay = () => {
  if (!mapInstance) return
  const layer = layerOptions.find(l => l.id === currentLayer.value)
  if (!layer) return

  if (labelLayer) {
    mapInstance.removeLayer(labelLayer)
    labelLayer = null
  }

  labelLayer = L.tileLayer(`https://t0.tianditu.gov.cn/${layer.labelUrl}/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=${layer.labelLayer}&STYLE=default&TILEMATRIXSET=w&FORMAT=tiles&TILEMATRIX={z}&TILEROW={y}&TILECOL={x}&tk=${TIANDI_TU_KEY}`, {
    maxZoom: 18,
    minZoom: 1
  }).addTo(mapInstance)
}

const switchLayer = (layerId: string) => {
  if (currentLayer.value === layerId) return
  currentLayer.value = layerId
  const layer = layerOptions.find(l => l.id === layerId)
  if (layer) {
    currentLayerName.value = layer.name
  }
  addLayer(layerId)
}

// 加载隐患点列表
const loadHazardPoints = async () => {
  try {
    const response = await getHazardPointPage({ pageNum: 1, pageSize: 100 })
    if (response.code === 200 && response.data) {
      const list = response.data.rows || []
      // 转换数据格式以适配前端
      hazardPoints.value = list.map((item: any) => ({
        id: item.id,
        name: item.name,
        code: item.code,
        longitude: item.longitude,
        latitude: item.latitude,
        groupId: item.groupId,
        groupName: item.groupName || '',
        description: item.description || '',
        deviceCount: item.deviceCount || 0,
        status: item.status === 1 ? 'MONITORING' : item.status === 2 ? 'PAUSED' : 'COMPLETED',
        alarmLevel: item.alarmLevel || null
      }))
      
      // 重新渲染地图标记
      refreshHazardMarkers()
    }
  } catch (error) {
    console.error('加载隐患点列表失败:', error)
  }
}

// 加载隐患点分组
const loadHazardPointGroups = async () => {
  try {
    const response = await getHazardPointGroups()
    if (response.code === 200 && response.data) {
      hazardPointGroups.value = response.data
      // 同步默认图层选中状态（与 MapBusinessToolbar 的 defaultCheckedKeys 一致）
      if (activeLayerKeys.value.length === 0) {
        const keys = ['showLabels']
        response.data.forEach((g: any) => keys.push(`group_${g.id}`))
        keys.push('showMonitoring')
        activeLayerKeys.value = keys
      }
    }
  } catch (error) {
    console.error('加载隐患点分组失败:', error)
  }
}

const loadMonitorTypes = async () => {
  try {
    monitorTypes.value = await getMonitorTypeList()
  } catch {
    monitorTypes.value = []
  }
}

// 加载仪表盘全局统计（Health + Resource + Alarm）
const loadDashboardData = async () => {
  try {
    const [fullRes, alarmRes] = await Promise.all([
      getDashboardFull(),
      getRealtimeAlarmPage({ pageNum: 1, pageSize: 50 })
    ])

    // ---- HealthWidget ----
    if (fullRes.code === 200 && fullRes.data?.healthScore) {
      const hs = fullRes.data.healthScore
      healthStats.value.overallScore = hs.overallScore
      if (hs.items?.length) {
        healthStats.value.items = hs.items.map((item, i) => ({
          name: item.name,
          value: item.value,
          weight: item.weight,
          color: item.color || healthStats.value.items[i]?.color || '#1890ff'
        }))
      }
    }

    // ---- ResourceWidget ----
    if (fullRes.code === 200 && fullRes.data?.overview) {
      const ov = fullRes.data.overview
      const dist = fullRes.data.sensorDistribution
      resourceStats.value.deviceTotal = ov.device?.total || 0
      resourceStats.value.hazardTotal = ov.hazardPoint?.total || 0
      resourceStats.value.totalResources = resourceStats.value.deviceTotal + resourceStats.value.hazardTotal + (ov.videoDevice?.total || 0)

      if (dist?.list?.length) {
        resourceStats.value.deviceTypes = dist.list.map(d => ({
          name: d.monitorTypeName,
          count: d.sensorCount
        }))
      }
    }

    // ---- DeviceStatusWidget ----
    if (fullRes.code === 200 && fullRes.data?.deviceOnlineRate) {
      const dor = fullRes.data.deviceOnlineRate
      deviceStatusStats.value = {
        onlineRate: dor.onlineRate ?? 0,
        onlineCount: dor.online ?? 0,
        totalCount: dor.total ?? 0,
        typeStats: dor.byType?.map(t => ({
          name: t.monitorTypeName,
          online: t.online,
          total: t.total
        })) ?? []
      }
    }

    // ---- AlarmWidget (system-wide) ----
    if (alarmRes?.code === 200 && alarmRes.data?.rows) {
      const alarms: RealtimeAlarmDetail[] = alarmRes.data.rows
      const pending = alarms.filter(a => a.alarmStatus === 0)
      const levelMap: Record<string, string> = { '1': 'critical', '2': 'major', '3': 'minor', '4': 'info' }
      const nameMap: Record<string, string> = { '1': '严重', '2': '重要', '3': '一般', '4': '提示' }
      const levelCounts: Record<string, number> = { critical: 0, major: 0, minor: 0, info: 0 }
      alarms.forEach(a => {
        const key = levelMap[String(a.alarmLevel)] || 'info'
        levelCounts[key]++
      })

      alarmStats.value.pendingCount = pending.length
      alarmStats.value.historyCount = alarms.length
      alarmStats.value.levelStats = [
        { key: 'critical', name: '严重', count: levelCounts.critical, icon: '/img/alarm/level1.png' },
        { key: 'major', name: '重要', count: levelCounts.major, icon: '/img/alarm/level2.png' },
        { key: 'minor', name: '一般', count: levelCounts.minor, icon: '/img/alarm/level3.png' },
        { key: 'info', name: '提示', count: levelCounts.info, icon: '/img/alarm/level4.png' }
      ]
      alarmStats.value.recentAlarms = alarms.slice(0, 10).map(a => ({
        id: a.id,
        level: levelMap[String(a.alarmLevel)] || 'info',
        title: a.alarmDetail || a.alarmTypeName,
        source: a.hazardPointName,
        time: a.lastAlarmTime
      }))
    }
  } catch (e) {
    console.error('加载仪表盘统计失败:', e)
  }
}

onMounted(async () => {
  initMap()
  window.addEventListener('resize', handleResize)
  
  // 加载数据
  await loadHazardPointGroups()
  loadMonitorTypes()
  await loadHazardPoints()
  loadDashboardData()
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (mapInstance) {
    mapInstance.remove()
    mapInstance = null
  }
  if (baseLayer) {
    baseLayer.remove()
    baseLayer = null
  }
  if (labelLayer) {
    labelLayer.remove()
    labelLayer = null
  }
})
</script>

<style scoped>
:global(html), :global(body) {
  margin: 0;
  padding: 0;
  overflow: hidden;
}

:global(#app) {
  width: 100%;
  height: 100%;
  overflow: hidden;
}

.dashboard-container {
  width: 100%;
  height: 100%;
  position: relative;
  overflow: hidden;
  margin: 0;
  padding: 0;
  --font-display: 'DingTalk JinBuTi', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  --font-body: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

.map-container {
  width: 100%;
  height: 100%;
  margin: 0;
  padding: 0;
  overflow: hidden;
}

.left-panel-wrapper {
  position: absolute;
  top: 0;
  bottom: 0;
  left: 12px;
  z-index: 1000;
  overflow: visible;
  transition: left 0.3s ease;
  pointer-events: none;
}

.left-panel {
  width: 320px;
  height: 100%;
  overflow: hidden;
  background: transparent;
  backdrop-filter: none;
  border-radius: 0;
  border: none;
  box-shadow: none;
  transition: width 0.3s ease;
  padding: 12px 0;
}

.left-panel-wrapper.collapsed .left-panel .panel-content {
  display: none;
}

.left-panel-wrapper.collapsed {
  left: 0;
}

.left-panel-wrapper.collapsed .left-panel {
  width: 0;
  min-width: unset;
  padding: 0;
  overflow: hidden;
}

/* 面板折叠触发按钮 - 左侧面板 */
.panel-collapse-trigger-left {
  position: absolute;
  top: 50%;
  right: -24px;
  transform: translateY(-50%);
  z-index: 10;
  pointer-events: auto;
  width: 24px;
  height: 56px;
  background: linear-gradient(to right, rgba(255, 255, 255, 0.95), rgba(255, 255, 255, 0.8));
  backdrop-filter: blur(8px);
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-left: none;
  border-radius: 0 8px 8px 0;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #606266;
  transition: all 0.3s ease;
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.04);
}

.panel-collapse-trigger-left:hover {
  background: rgba(24, 144, 255, 0.1);
  border-color: rgba(24, 144, 255, 0.3);
  color: #1890ff;
  width: 28px;
  right: -28px;
}

.left-panel-wrapper.collapsed .panel-collapse-trigger-left {
  right: -24px;
  border-left: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: 0 8px 8px 0;
}

.panel-content {
  background: transparent;
  height: 100%;
  overflow-y: auto;
  pointer-events: auto;
}

.panel-section {
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(16px);
  border: 1px solid rgba(24, 144, 255, 0.08);
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.06), 0 1px 2px rgba(0, 0, 0, 0.04);
  padding: 16px 18px;
  margin-bottom: 12px;
  transition: box-shadow 0.2s, border-color 0.2s;
}

.panel-section:hover {
  border-color: rgba(24, 144, 255, 0.15);
  box-shadow: 0 4px 16px rgba(24, 144, 255, 0.08), 0 1px 2px rgba(0, 0, 0, 0.04);
}

.panel-section:last-child {
  margin-bottom: 0;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 14px 10px;
  background: linear-gradient(135deg, rgba(24, 144, 255, 0.12) 0%, rgba(24, 144, 255, 0.03) 100%);
  border-radius: 8px 8px 0 0;
  border-bottom: 1px solid rgba(24, 144, 255, 0.12);
  margin: -16px -18px 14px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #1d2129;
  font-family: var(--font-display);
}

/* 面板折叠触发按钮 - 右侧面板 */
.panel-collapse-trigger-right {
  position: absolute;
  top: 50%;
  left: -24px;
  transform: translateY(-50%);
  z-index: 10;
  pointer-events: auto;
  width: 24px;
  height: 56px;
  background: linear-gradient(to left, rgba(255, 255, 255, 0.95), rgba(255, 255, 255, 0.8));
  backdrop-filter: blur(8px);
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-right: none;
  border-radius: 8px 0 0 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #606266;
  transition: all 0.3s ease;
  box-shadow: -2px 0 8px rgba(0, 0, 0, 0.04);
}

.panel-collapse-trigger-right:hover {
  background: rgba(24, 144, 255, 0.1);
  border-color: rgba(24, 144, 255, 0.3);
  color: #1890ff;
  width: 28px;
  left: -28px;
}

.right-panel-wrapper.collapsed .panel-collapse-trigger-right {
  left: -24px;
  border-right: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: 8px 0 0 8px;
}

:deep(.leaflet-attribution) {
  background: rgba(255, 255, 255, 0.8) !important;
  padding: 4px 8px;
  font-size: 12px;
}

/* 比例尺 — 线段 + 两端竖线 + 居中文字 */
:deep(.leaflet-bottom.leaflet-right) {
  display: flex !important;
  right: auto;
  left: var(--scale-left, 12px);
  bottom: 28px;
}

:deep(.leaflet-control-scale-line) {
  background: transparent !important;
  border: none !important;
  border-bottom: 2px solid rgba(255, 255, 255, 0.85) !important;
  position: relative;
  text-align: center;
  padding: 0 0 2px 0;
  margin: 0;
  font-size: 11px;
  color: #fff;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.6), 0 0 4px rgba(0, 0, 0, 0.3);
  line-height: 1;
  white-space: nowrap;
}

:deep(.leaflet-control-scale-line)::before,
:deep(.leaflet-control-scale-line)::after {
  content: '';
  position: absolute;
  bottom: -2px;
  width: 2px;
  height: 8px;
  background: rgba(255, 255, 255, 0.85);
}

:deep(.leaflet-control-scale-line)::before {
  left: 0;
}

:deep(.leaflet-control-scale-line)::after {
  right: 0;
}

.right-panel-wrapper {
  position: absolute;
  top: 0;
  bottom: 0;
  right: 12px;
  z-index: 1000;
  overflow: visible;
  transition: right 0.3s ease;
  pointer-events: none;
}

.right-panel-wrapper.collapsed {
  right: 0;
}

.right-panel-wrapper.collapsed .right-panel .panel-content {
  display: none;
}

.right-panel-wrapper.collapsed .right-panel {
  width: 0;
  min-width: unset;
  padding: 0;
  overflow: hidden;
}

.right-panel {
  width: 320px;
  height: 100%;
  overflow: hidden;
  background: transparent;
  backdrop-filter: none;
  border-radius: 0;
  border: none;
  box-shadow: none;
  transition: width 0.3s ease;
  padding: 12px 0;
}
:deep(.leaflet-control-attribution) {
  display: none !important;
}

:deep(.tianditu-logo) {
  display: none !important;
}

/* 右下角工具栏样式已移至 BottomToolbar.vue */

/* 隐患点视图样式 */
.hazard-view-header {
  position: absolute;
  top: 20px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 1000;
  display: flex;
  align-items: center;
  gap: 16px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(12px);
  padding: 12px 20px;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

.hazard-title-wrapper {
  position: relative;
}

.hazard-title {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 8px 12px;
  background: #f5f7fa;
  border-radius: 8px;
  transition: all 0.2s;
}

.hazard-title:hover {
  background: #e8e8e8;
}

.hazard-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  font-family: var(--font-display);
}

.hazard-dropdown-arrow {
  font-size: 10px;
  color: #909399;
  transition: transform 0.2s;
}

.hazard-list-dropdown {
  position: absolute;
  top: calc(100% + 8px);
  left: 0;
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  min-width: 200px;
  max-height: 300px;
  overflow-y: auto;
  z-index: 1001;
}

.hazard-list-item {
  padding: 10px 16px;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 14px;
  color: #303133;
}

.hazard-list-item:hover {
  background: #f0f7ff;
}

.hazard-list-item.active {
  background: #e6f7ff;
  color: #1890ff;
}

.close-hazard-view-btn {
  width: 36px;
  height: 36px;
  background: #f5f7fa;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  cursor: pointer;
  font-size: 18px;
  color: #606266;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.close-hazard-view-btn:hover {
  background: #fff1f0;
  border-color: #ff7875;
  color: #f5222d;
}

/* 隐患点信息面板 */
/* 传感器列表 */
.sensor-list-panel {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.sensor-title {
  font-size: 13px;
  font-weight: 500;
  color: #606266;
  margin-bottom: 10px;
}

.sensor-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.sensor-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  background: #fafafa;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid transparent;
}

.sensor-item:hover {
  background: #f0f7ff;
}

.sensor-item.selected {
  background: #e6f7ff;
  border-color: #1890ff;
}

.sensor-item.warning {
  background: #fff7e6;
}

.sensor-item.warning.selected {
  background: #fff1f0;
  border-color: #fa541c;
}

.sensor-icon {
  font-size: 18px;
}

.sensor-info {
  flex: 1;
}

.sensor-name {
  font-size: 12px;
  color: #303133;
}

.sensor-code {
  font-size: 10px;
  color: #909399;
}

.sensor-status .status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.sensor-status.online .status-dot {
  background: #52c41a;
}

.sensor-status.warning .status-dot {
  background: #faad14;
}

/* 数据曲线图 */
.sensor-chart {
  margin-top: 16px;
  padding: 12px;
  background: #fafafa;
  border-radius: 8px;
}

.chart-title {
  font-size: 12px;
  font-weight: 500;
  color: #606266;
  margin-bottom: 10px;
}

.chart-container {
  position: relative;
}

.chart-svg {
  width: 100%;
  height: 100px;
  background: linear-gradient(to top, #f0f7ff 0%, transparent 100%);
  border-radius: 4px;
}

.chart-labels {
  display: flex;
  justify-content: space-between;
  font-size: 10px;
  color: #909399;
  margin-top: 4px;
}

.status-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 500;
}

.status-badge.normal {
  background: rgba(82, 196, 26, 0.1);
  color: #52c41a;
}

.status-badge.warning {
  background: rgba(250, 173, 20, 0.1);
  color: #faad14;
}

/* ========== 隐患点详情部件样式 ========== */

.hazard-detail-section .section-header {
  margin: -16px -18px 12px;
  padding: 8px 14px 10px;
  background: linear-gradient(135deg, rgba(24, 144, 255, 0.12) 0%, rgba(24, 144, 255, 0.03) 100%);
  border-radius: 8px 8px 0 0;
  border-bottom: 1px solid rgba(24, 144, 255, 0.12);
}

.clear-selection-btn {
  width: 22px;
  height: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: none;
  border: none;
  border-radius: 4px;
  color: #909399;
  cursor: pointer;
  transition: all 0.2s;
}

.clear-selection-btn:hover {
  background: #fff1f0;
  color: #f5222d;
}

.widget-empty-state {
  padding: 20px 12px;
  text-align: center;
}

.empty-hint {
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
}

.hazard-widget-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.widget-hazard-name {
  font-size: 16px;
  font-weight: 600;
  color: #1d2129;
  padding-bottom: 8px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  font-family: var(--font-display, inherit);
}

.hazard-detail-info {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 8px 12px;
  background: rgba(24, 144, 255, 0.04);
  border-radius: 8px;
}

.detail-row {
  display: flex;
  gap: 12px;
  font-size: 12px;
  line-height: 1.6;
}

.detail-label {
  color: #86909c;
  flex-shrink: 0;
  min-width: 40px;
  font-weight: 500;
}

.detail-value {
  color: #303133;
  word-break: break-all;
  font-weight: 500;
}

.detail-description {
  color: #606266;
  font-size: 11px;
}

/* 部件设备列表 */
.widget-device-list {
  border-top: 1px solid rgba(0, 0, 0, 0.06);
  padding-top: 10px;
}

.device-list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.device-list-title {
  font-size: 11px;
  font-weight: 500;
  color: #86909c;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.device-count {
  font-size: 12px;
  color: #1890ff;
  font-weight: 700;
  font-family: var(--font-display, inherit);
}

.device-loading {
  font-size: 12px;
  color: #909399;
  text-align: center;
  padding: 12px 0;
}

/* 设备卡片列表 */
.widget-device-cards {
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-height: 220px;
  overflow-y: auto;
}

.device-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  background: rgba(245, 247, 250, 0.6);
  border-radius: 8px;
  border: 1px solid transparent;
  cursor: pointer;
  transition: all 0.15s;
}

.device-card:hover {
  background: #f0f7ff;
  border-color: rgba(24, 144, 255, 0.2);
}

.device-card:active {
  background: #e6f7ff;
}

.device-card-icon {
  width: 34px;
  height: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(64, 158, 255, 0.08);
  border-radius: 8px;
  flex-shrink: 0;
  color: #1890ff;
}

.device-card-body {
  flex: 1;
  min-width: 0;
}

.device-card-name {
  font-size: 12px;
  color: #303133;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 2px;
}

.device-card-meta {
  display: flex;
  gap: 6px;
  font-size: 11px;
  color: #909399;
  white-space: nowrap;
}

.device-card-type {
  color: #606266;
}

.device-card-sensors {
  color: #909399;
}

.device-card-status {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
  font-size: 11px;
  white-space: nowrap;
}

.device-card-status .status-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
}

.device-card-status .status-text {
  font-size: 11px;
  font-weight: 500;
}

.device-card-status.online .status-dot {
  background: #52c41a;
}

.device-card-status.online .status-text {
  color: #52c41a;
}

.device-card-status.warning .status-dot {
  background: #faad14;
}

.device-card-status.warning .status-text {
  color: #faad14;
}

.device-card-status.offline .status-dot {
  background: #f5222d;
}

.device-card-status.offline .status-text {
  color: #f5222d;
}

.no-devices {
  font-size: 12px;
  color: #909399;
  text-align: center;
  padding: 8px 0;
}

.view-detail-btn {
  width: 100%;
  padding: 10px 0;
  background: linear-gradient(135deg, #1890ff 0%, #66b1ff 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.view-detail-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
}

/* ========== /隐患点详情部件样式 ========== */
</style>

<style>
/* ========== 隐患点悬浮窗 V2（全局样式，Leaflet popup 动态渲染） ========== */
.leaflet-popup-content-wrapper {
  border-radius: 12px !important;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15) !important;
  border: none !important;
  padding: 0 !important;
  overflow: visible !important;
}

.leaflet-popup-content {
  margin: 0 !important;
}

.hpv2-card{padding:0}
.hpv2-header{padding:8px 12px 6px}
.hpv2-title{font-size:13px;font-weight:700;color:#1677ff}
.hpv2-dash{margin:0 12px;border-bottom:1px dashed rgba(0,0,0,.18)}
.hpv2-body{padding:4px 12px 8px}
.hpv2-row{display:flex;padding:4px 0}
.hpv2-cell{flex:1;min-width:0;display:flex;flex-direction:column;gap:1px}
.hpv2-cell:not(:last-child){padding-right:12px}
.hpv2-cell.full{flex-direction:row;align-items:center;justify-content:space-between}
.hpv2-label{font-size:11px;color:#9ca3af;white-space:nowrap}
.hpv2-val{font-size:12px;color:#374151;font-weight:500}
.hpv2-badge{display:inline-block;font-size:11px;font-weight:500;padding:1px 8px;border-radius:3px;width:fit-content}
.hpv2-level{display:inline-block;font-size:11px;font-weight:600;padding:1px 8px;border-radius:3px;width:fit-content}
.hpv2-devices{margin-top:6px;padding-top:6px;border-top:1px dashed rgba(0,0,0,.18);max-height:100px;overflow-y:auto}
.hpv2-device{display:flex;justify-content:space-between;align-items:center;padding:3px 0}
.hpv2-device+.hpv2-device{border-top:1px solid rgba(0,0,0,.05)}
.hpv2-dn{font-size:11px;color:#4b5563}
.hpv2-ds{font-size:11px;font-weight:500}
</style>
