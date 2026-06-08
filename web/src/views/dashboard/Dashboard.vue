<template>
  <div class="dashboard-container">
    <div ref="mapContainer" class="map-container"></div>

    <!-- 隐患点视图顶部标题栏 -->
    <div v-if="currentView === 'hazard'" class="hazard-view-header">
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
      <button class="close-hazard-view-btn" @click="exitHazardView" title="返回系统视图">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
             stroke-linecap="round" stroke-linejoin="round" width="18" height="18">
          <line x1="18" y1="6" x2="6" y2="18"/>
          <line x1="6" y1="6" x2="18" y2="18"/>
        </svg>
      </button>
    </div>

    <!-- 隐患点视图左侧面板 -->
    <div v-if="currentView === 'hazard'" class="hazard-info-panel">
      <!-- 隐患点基本信息 -->
      <div class="hazard-basic-info">
        <div class="info-title">隐患点基本信息</div>
        <div class="info-content">
          <div class="info-row">
            <span class="info-label">隐患点名称:</span>
            <span class="info-value">{{ currentHazardPoint?.name }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">隐患点编号:</span>
            <span class="info-value">{{ currentHazardPoint?.code }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">坐标位置:</span>
            <span class="info-value">{{
                currentHazardPoint?.latitude.toFixed(6)
              }}, {{ currentHazardPoint?.longitude.toFixed(6) }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">所属分组:</span>
            <span class="info-value">{{ currentHazardPoint?.groupName }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">隐患点备注:</span>
            <span class="info-value">{{ currentHazardPoint?.description }}</span>
          </div>
        </div>
      </div>

      <!-- 设备列表 -->
      <div class="device-list-panel">
        <div class="info-title">绑定设备列表</div>
        <div class="device-list">
          <div
              v-for="device in deviceList"
              :key="device.id"
              class="device-item"
              :class="{ selected: selectedDevice?.id === device.id }"
              @click="openDeviceDataModal(device)"
          >
            <div class="device-info">
              <div class="device-type-icon">
                <el-icon :size="24">
                  <component :is="getDeviceTypeIcon(device.type)"/>
                </el-icon>
              </div>
              <div class="device-details">
                <div class="device-name">{{ device.name }}</div>
                <div class="device-meta">
                  <span class="device-type">{{ device.typeName }}</span>
                  <span class="device-sensors">{{ device.sensorCount }}个传感器</span>
                </div>
              </div>
            </div>
            <div class="device-status" :class="device.status">
              <span class="status-dot"></span>
              <span class="status-text">{{ getStatusText(device.status) }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 设备数据弹窗 -->
    <DeviceDataModal
      v-if="showDeviceDataModal"
      :device="selectedDevice"
      :hazard-point-id="currentHazardPoint?.id"
      :hazard-point-name="currentHazardPoint?.name || '系统总览'"
      @close="closeDeviceDataModal"
      @back-to-system-view="backToSystemView"
    />

    <div class="left-panel-wrapper" :class="{ collapsed: isPanelCollapsed || currentView === 'hazard' }">
      <div class="left-panel" v-if="currentView === 'system'">
        <div class="panel-header">
          <button class="panel-collapse-btn" @click="togglePanel">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                 stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="16" height="16">
              <polyline v-if="isPanelCollapsed" points="9 18 15 12 9 6"/>
              <polyline v-else points="15 18 9 12 15 6"/>
            </svg>
          </button>
        </div>
        <div class="panel-content">
          <HealthWidget v-if="isWidgetOnLeft('systemHealth')" :health-stats="healthStats" />

          <ResourceWidget v-if="isWidgetOnLeft('assetInfo')" :resource-stats="resourceStats" />

          <AlarmWidget v-if="isWidgetOnLeft('alarmStatus')" :alarm-stats="alarmStats" />

          <!-- 隐患点详情部件 -->
          <div v-if="isWidgetOnLeft('hazardPointDetail') && currentView === 'system'"
               class="panel-section hazard-detail-section">
            <div class="section-header">
              <span class="section-title">隐患点详情</span>
              <button v-if="selectedHazardPoint" class="clear-selection-btn" @click="clearWidgetSelection"
                      title="清除选择">
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                     stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="14" height="14">
                  <line x1="18" y1="6" x2="6" y2="18"/>
                  <line x1="6" y1="6" x2="18" y2="18"/>
                </svg>
              </button>
            </div>

            <!-- 空状态 -->
            <div v-if="!selectedHazardPoint" class="widget-empty-state">
              <span class="empty-hint">点击地图标记查看隐患点详情</span>
            </div>

            <!-- 已选择隐患点 -->
            <div v-else class="hazard-widget-content">
              <div class="widget-hazard-name">{{ selectedHazardPoint.name }}</div>

              <div class="hazard-detail-info">
                <div class="detail-row">
                  <span class="detail-label">编号</span>
                  <span class="detail-value">{{ selectedHazardPoint.code }}</span>
                </div>
                <div class="detail-row">
                  <span class="detail-label">坐标</span>
                  <span class="detail-value">{{
                      selectedHazardPoint.latitude.toFixed(6)
                    }}, {{ selectedHazardPoint.longitude.toFixed(6) }}</span>
                </div>
                <div class="detail-row">
                  <span class="detail-label">分组</span>
                  <span class="detail-value">{{ selectedHazardPoint.groupName || '--' }}</span>
                </div>
                <div class="detail-row">
                  <span class="detail-label">备注</span>
                  <span class="detail-value detail-description">{{ selectedHazardPoint.description || '--' }}</span>
                </div>
              </div>

              <!-- 绑定设备列表 -->
              <div class="widget-device-list">
                <div class="device-list-header">
                  <span class="device-list-title">绑定设备</span>
                  <span class="device-count">{{ widgetBoundDevices.length }}台</span>
                </div>
                <div v-if="widgetDevicesLoading" class="device-loading">加载中...</div>
                <div v-else class="widget-device-cards">
                  <div
                      v-for="device in widgetBoundDevices"
                      :key="device.id"
                      class="device-card"
                      @click="openDeviceDataModal(device)"
                  >
                    <div class="device-card-icon">
                      <el-icon :size="22">
                        <component :is="getDeviceTypeIcon(device.type)"/>
                      </el-icon>
                    </div>
                    <div class="device-card-body">
                      <div class="device-card-name">{{ device.name }}</div>
                      <div class="device-card-meta">
                        <span class="device-card-type">{{ device.typeName }}</span>
                        <span class="device-card-sensors">{{ device.sensorCount }}个传感器</span>
                      </div>
                    </div>
                    <div class="device-card-status" :class="device.status">
                      <span class="status-dot"></span>
                      <span class="status-text">{{
                          device.status === 'online' ? '在线' : device.status === 'warning' ? '预警' : '离线'
                        }}</span>
                    </div>
                  </div>
                  <div v-if="widgetBoundDevices.length === 0" class="no-devices">暂无绑定设备</div>
                </div>
              </div>

              <button class="view-detail-btn" @click="openHazardDetailFromWidget">
                查看详情
              </button>
            </div>
          </div>
          <!-- /隐患点详情部件 -->

        </div>
      </div>
    </div>


    <div class="right-panel-wrapper" :class="{ collapsed: isRightPanelCollapsed }">
      <div class="layer-switcher-wrapper">
        <button class="layer-toggle-btn" @click="toggleLayerList">
          <el-icon>
            <MapLocation/>
          </el-icon>
          <span>{{ currentLayerName }}</span>
          <span class="arrow" :class="{ expanded: showLayerList }">▼</span>
        </button>

        <div v-show="showLayerList" class="layer-dropdown">
          <div
              v-for="layer in layerOptions"
              :key="layer.id"
              class="layer-item"
              :class="{ active: currentLayer === layer.id }"
              @click="handleLayerSelect(layer.id)"
          >
            <div class="layer-preview" :style="{ background: layer.color }"></div>
            <span>{{ layer.name }}</span>
          </div>
        </div>

        <div class="zoom-controls">
          <button class="zoom-btn zoom-in" @click="handleZoomIn">+</button>
          <button class="zoom-btn zoom-out" @click="handleZoomOut">−</button>
        </div>
      </div>

      <div class="right-panel">
        <div class="panel-header right-panel-header">
          <button class="panel-collapse-btn" @click="toggleRightPanel">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                 stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="16" height="16">
              <polyline v-if="isRightPanelCollapsed" points="15 18 9 12 15 6"/>
              <polyline v-else points="9 18 15 12 9 6"/>
            </svg>
          </button>
        </div>
        <div class="panel-content">
          <HealthWidget v-if="isWidgetOnRight('systemHealth')" :health-stats="healthStats" />

          <ResourceWidget v-if="isWidgetOnRight('assetInfo')" :resource-stats="resourceStats" />

          <AlarmWidget v-if="isWidgetOnRight('alarmStatus')" :alarm-stats="alarmStats" />

          <!-- 隐患点详情部件 -->
          <div v-if="isWidgetOnRight('hazardPointDetail') && currentView === 'system'"
               class="panel-section hazard-detail-section">
            <div class="section-header">
              <span class="section-title">隐患点详情</span>
              <button v-if="selectedHazardPoint" class="clear-selection-btn" @click="clearWidgetSelection"
                      title="清除选择">
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                     stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="14" height="14">
                  <line x1="18" y1="6" x2="6" y2="18"/>
                  <line x1="6" y1="6" x2="18" y2="18"/>
                </svg>
              </button>
            </div>

            <div v-if="!selectedHazardPoint" class="widget-empty-state">
              <span class="empty-hint">点击地图标记查看隐患点详情</span>
            </div>

            <div v-else class="hazard-widget-content">
              <div class="widget-hazard-name">{{ selectedHazardPoint.name }}</div>

              <div class="hazard-detail-info">
                <div class="detail-row">
                  <span class="detail-label">编号</span>
                  <span class="detail-value">{{ selectedHazardPoint.code }}</span>
                </div>
                <div class="detail-row">
                  <span class="detail-label">坐标</span>
                  <span class="detail-value">{{
                      selectedHazardPoint.latitude.toFixed(6)
                    }}, {{ selectedHazardPoint.longitude.toFixed(6) }}</span>
                </div>
                <div class="detail-row">
                  <span class="detail-label">分组</span>
                  <span class="detail-value">{{ selectedHazardPoint.groupName || '--' }}</span>
                </div>
                <div class="detail-row">
                  <span class="detail-label">备注</span>
                  <span class="detail-value detail-description">{{ selectedHazardPoint.description || '--' }}</span>
                </div>
              </div>

              <div class="widget-device-list">
                <div class="device-list-header">
                  <span class="device-list-title">绑定设备</span>
                  <span class="device-count">{{ widgetBoundDevices.length }}台</span>
                </div>
                <div v-if="widgetDevicesLoading" class="device-loading">加载中...</div>
                <div v-else class="widget-device-cards">
                  <div
                      v-for="device in widgetBoundDevices"
                      :key="device.id"
                      class="device-card"
                      @click="openDeviceDataModal(device)"
                  >
                    <div class="device-card-icon">
                      <el-icon :size="22">
                        <component :is="getDeviceTypeIcon(device.type)"/>
                      </el-icon>
                    </div>
                    <div class="device-card-body">
                      <div class="device-card-name">{{ device.name }}</div>
                      <div class="device-card-meta">
                        <span class="device-card-type">{{ device.typeName }}</span>
                        <span class="device-card-sensors">{{ device.sensorCount }}个传感器</span>
                      </div>
                    </div>
                    <div class="device-card-status" :class="device.status">
                      <span class="status-dot"></span>
                      <span class="status-text">{{
                          device.status === 'online' ? '在线' : device.status === 'warning' ? '预警' : '离线'
                        }}</span>
                    </div>
                  </div>
                  <div v-if="widgetBoundDevices.length === 0" class="no-devices">暂无绑定设备</div>
                </div>
              </div>

              <button class="view-detail-btn" @click="openHazardDetailFromWidget">
                查看详情
              </button>
            </div>
          </div>
          <!-- /隐患点详情部件 -->

        </div>
      </div>
    </div>

    <!-- 面板布局配置弹窗 -->
    <LayoutConfigDialog
      v-model:visible="showLayoutDialog"
      :layout="layoutConfig"
      @update:layout="handleLayoutUpdate"
    />

    <!-- 右下角工具栏 -->
    <BottomToolbar
        :hazard-points="hazardPoints"
        :mask-visible="showMaskLayer"
        :layout-dialog-visible="showLayoutDialog"
        @select-hazard-point="enterHazardView"
        @toggle-layer="toggleLayer"
        @open-layout-config="showLayoutDialog = true"
        @toggle-mask="toggleMaskLayer"
    />
  </div>
</template>

<script setup lang="ts">
import {onMounted, onUnmounted, ref} from 'vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import {
  Drizzling,
  MapLocation,
  Monitor,
  Odometer,
  Sunny
} from '@element-plus/icons-vue'
import {getBoundDevices, getHazardPointGroups, getHazardPointPage} from '@/api/hazardPoint'
import AlarmWidget from './components/AlarmWidget.vue'
import BottomToolbar from './components/BottomToolbar.vue'
import DeviceDataModal from './components/DeviceDataModal.vue'
import HealthWidget from './components/HealthWidget.vue'
import LayoutConfigDialog from './components/LayoutConfigDialog.vue'
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

const isRightPanelCollapsed = ref(false)

// ========== 面板布局配置 ==========
const DEFAULT_LAYOUT = {
  left: ['hazardPointDetail'],
  right: ['alarmStatus'],
  hidden: ['systemHealth', 'assetInfo']
}

const showLayoutDialog = ref(false)
const layoutConfig = ref<{ left: string[]; right: string[]; hidden: string[] }>(DEFAULT_LAYOUT)

const handleLayoutUpdate = (layout: { left: string[]; right: string[]; hidden: string[] }) => {
  layoutConfig.value = layout
}

const isWidgetOnLeft = (key: string) => layoutConfig.value.left.includes(key)
const isWidgetOnRight = (key: string) => layoutConfig.value.right.includes(key)

// ========== /面板布局配置 ==========

// 视图模式
const currentView = ref<'system' | 'hazard'>('system')
const currentHazardPoint = ref<typeof hazardPoints.value[0] | null>(null)
const showHazardList = ref(false)

// 隐患点详情部件 - 软选择状态
const selectedHazardPoint = ref<typeof hazardPoints.value[0] | null>(null)
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
const showSensorChart = ref(false)
const sensorChartData = ref<number[]>([])

// 蒙层显示状态
const showMaskLayer = ref(true)

// 设备数据弹窗
const showDeviceDataModal = ref(false)

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

let maskLayer: L.GeoJSON | null = null
let boundaryLayer: L.Polyline | null = null
let hazardMarkerLayer: L.LayerGroup | null = null
let hazardMarkerMap: Map<number, L.CircleMarker> = new Map()
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
  levelStats: { key: string; name: string; count: number }[]
  recentAlarms: { id: number; level: string; title: string; source: string; time: string }[]
}>({
  pendingCount: 0,
  historyCount: 0,
  levelStats: [
    {key: 'critical', name: '严重', count: 0},
    {key: 'major', name: '重要', count: 0},
    {key: 'minor', name: '一般', count: 0},
    {key: 'info', name: '提示', count: 0}
  ],
  recentAlarms: []
})

const togglePanel = () => {
  isPanelCollapsed.value = !isPanelCollapsed.value
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
    position: 'bottomleft'
  }).addTo(mapInstance)

  addFocusBoundary()
  addMaskLayer()
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
  if (!maskLayer) return
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

const addHazardPoints = () => {
  if (!mapInstance) return

  const markerLayer = L.layerGroup().addTo(mapInstance)
  hazardMarkerLayer = markerLayer

  hazardPoints.value.forEach(point => {
    const marker = L.circleMarker([point.latitude, point.longitude], {
      radius: 10,
      fillColor: point.alarmLevel ? alarmColors[point.alarmLevel] : '#1890ff',
      color: '#ffffff',
      weight: 2,
      opacity: 1,
      fillOpacity: 0.8
    }).addTo(markerLayer)

    // 存储marker引用
    hazardMarkerMap.set(point.id, marker)

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
      autoClose: true,
      offset: L.point(0, -15)
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
  selectedHazardPoint.value = point

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
  if (mapInstance && hazardMarkerLayer) {
    hazardMarkerLayer.clearLayers()
    addHazardPoints()
    fitToFocusArea()
  }
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
  if (selectedHazardPoint.value) {
    enterHazardView(selectedHazardPoint.value)
  }
}

const clearWidgetSelection = () => {
  selectedHazardPoint.value = null
  widgetBoundDevices.value = []
  widgetDevicesLoading.value = false
}

// ========== /隐患点详情部件 ==========

const handleResize = () => {
  if (mapInstance) {
    mapInstance.invalidateSize()
  }
}

const handleZoomIn = () => {
  if (mapInstance) {
    mapInstance.zoomIn()
  }
}

const handleZoomOut = () => {
  if (mapInstance) {
    mapInstance.zoomOut()
  }
}

const toggleLayerList = () => {
  showLayerList.value = !showLayerList.value
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

    // 切换marker样式来模拟闪烁
    const currentOpacity = marker.options.fillOpacity ?? 0.8
    marker.setStyle({
      fillOpacity: currentOpacity > 0.5 ? 0.2 : 1
    })

    if (flashCount >= maxFlashes) {
      clearInterval(flashInterval)
      marker.setStyle({fillOpacity: 0.8})
      flashingPointId.value = null
    }
  }, 300)
}

// 图层控制
const toggleLayer = (layerKey: string) => {
  console.log('Toggle layer:', layerKey)
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

  // 清除部件软选择状态
  clearWidgetSelection()

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
  if (mapInstance && hazardMarkerLayer) {
    hazardMarkerLayer.clearLayers()
    addHazardPoints()
    fitToFocusArea()
  }
}

const updateHazardAlarms = (hazardId: number) => {
  // TODO: 从API获取当前隐患点的告警数据
  alarmStats.value.pendingCount = 0
  alarmStats.value.historyCount = 0
  alarmStats.value.levelStats = [
    {key: 'critical', name: '严重', count: 0},
    {key: 'major', name: '重要', count: 0},
    {key: 'minor', name: '一般', count: 0},
    {key: 'info', name: '提示', count: 0}
  ]
  alarmStats.value.recentAlarms = []
}

const resetAlarmStats = () => {
  alarmStats.value = {
    pendingCount: 0,
    historyCount: 0,
    levelStats: [
      {key: 'critical', name: '严重', count: 0},
      {key: 'major', name: '重要', count: 0},
      {key: 'minor', name: '一般', count: 0},
      {key: 'info', name: '提示', count: 0}
    ],
    recentAlarms: []
  }
}

const selectHazardPoint = (hazardPoint: typeof hazardPoints.value[0]) => {
  currentHazardPoint.value = hazardPoint
  showHazardList.value = false
  updateHazardAlarms(hazardPoint.id)

  if (mapInstance) {
    mapInstance.setView([hazardPoint.latitude, hazardPoint.longitude], 14)
  }

  addDeviceMarkers(hazardPoint.id)
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

// 打开设备数据弹窗
const openDeviceDataModal = (device: typeof deviceList.value[0]) => {
  selectedDevice.value = device
  showDeviceDataModal.value = true
}

// 关闭设备数据弹窗
const closeDeviceDataModal = () => {
  showDeviceDataModal.value = false
  selectedDevice.value = null
}

// 从设备列表返回到系统总览
const backToSystemView = () => {
  showDeviceDataModal.value = false
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
  }

  baseLayer = L.tileLayer(`https://t0.tianditu.gov.cn/${layer.baseUrl}/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=${layer.baseLayer}&STYLE=default&TILEMATRIXSET=w&FORMAT=tiles&TILEMATRIX={z}&TILEROW={y}&TILECOL={x}&tk=${TIANDI_TU_KEY}`, {
    maxZoom: 18,
    minZoom: 1
  }).addTo(mapInstance)

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
        status: item.status === 0 ? 'normal' : item.status === 1 ? 'warning' : 'danger',
        alarmLevel: item.alarmLevel || null
      }))
      
      // 重新渲染地图标记
      if (mapInstance && hazardMarkerLayer) {
        hazardMarkerLayer.clearLayers()
        addHazardPoints()
      }
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
    }
  } catch (error) {
    console.error('加载隐患点分组失败:', error)
  }
}

onMounted(async () => {
  initMap()
  window.addEventListener('resize', handleResize)
  
  // 加载数据
  await loadHazardPointGroups()
  await loadHazardPoints()
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
}

.map-container {
  width: 100%;
  height: 100%;
  margin: 0;
  padding: 0;
  overflow: hidden;
}

.layer-switcher-wrapper {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
  padding: 0px 0px;
  backdrop-filter: blur(12px);
  border-radius: 12px;
  border: 0px solid rgba(255, 255, 255, 0.2);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.04);
  transition: all 0.3s ease;
}

.layer-toggle-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(228, 231, 237, 0.7);
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  cursor: pointer;
  font-size: 13px;
  color: #303133;
  transition: all 0.2s ease;
}

.layer-toggle-btn:hover {
  background: #f5f7fa;
  border-color: #1890ff;
}

.arrow {
  font-size: 10px;
  transition: transform 0.2s ease;
}

.arrow.expanded {
  transform: rotate(180deg);
}

.layer-dropdown {
  position: absolute;
  top: 0;
  right: calc(100% + 8px);
  background: rgba(255, 255, 255, 0.95);
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  min-width: 140px;
  overflow: hidden;
}

.layer-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 13px;
  color: #303133;
}

.layer-item:hover {
  background: #f5f7fa;
}

.layer-item.active {
  background: #e8f4ff;
  color: #1890ff;
}

.layer-item.active::after {
  content: '✓';
  margin-left: auto;
  font-size: 12px;
}

.layer-preview {
  width: 20px;
  height: 14px;
  border-radius: 3px;
  border: 1px solid #e8e8e8;
}

.zoom-controls {
  display: flex;
  flex-direction: row;
  gap: 4px;
}

.zoom-btn {
  width: 36px;
  height: 36px;
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(228, 231, 237, 0.7);
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  cursor: pointer;
  font-size: 18px;
  font-weight: bold;
  color: #303133;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.zoom-btn:hover {
  background: #f5f7fa;
  border-color: #1890ff;
}

.zoom-in {
  color: #1890ff;
}

.zoom-out {
  color: #67c23a;
}

.left-panel-wrapper {
  position: absolute;
  top: 20px;
  left: 20px;
  z-index: 1000;
  display: flex;
  align-items: stretch;
  transition: all 0.3s ease;
}

.left-panel {
  width: 280px;
  max-height: calc(100vh - 40px);
  overflow-y: auto;
  background: rgba(255, 255, 255, 0.55);
  backdrop-filter: blur(12px);
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
  transition: all 0.3s ease;
}

.left-panel-wrapper.collapsed .left-panel .panel-content {
  display: none;
}

.left-panel-wrapper.collapsed .left-panel {
  width: auto;
  min-width: unset;
}

.panel-content {
  padding: 0;
  background: transparent;
}

.panel-section {
  background: rgba(255, 255, 255, 0.6);
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 12px;
}

.panel-section:last-child {
  margin-bottom: 0;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.section-title {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}

.panel-header {
  display: flex;
  align-items: center;
  padding: 8px 10px 4px;
}

.right-panel-header {
  justify-content: flex-end;
}

.panel-collapse-btn {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 6px;
  color: #606266;
  cursor: pointer;
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.panel-collapse-btn:hover {
  background: rgba(64, 158, 255, 0.1);
  border-color: #1890ff;
  color: #1890ff;
}

:deep(.leaflet-attribution) {
  background: rgba(255, 255, 255, 0.8) !important;
  padding: 4px 8px;
  font-size: 12px;
}

.right-panel-wrapper {
  position: absolute;
  top: 20px;
  right: 20px;
  z-index: 1000;
  display: flex;
  align-items: flex-start;
  gap: 8px;
  transition: all 0.3s ease;
}

.right-panel-wrapper.collapsed {
  right: 20px;
}

.right-panel-wrapper.collapsed .right-panel .panel-content {
  display: none;
}

.right-panel-wrapper.collapsed .right-panel {
  width: auto;
  min-width: unset;
}

.right-panel {
  width: 280px;
  max-height: calc(100vh - 40px);
  overflow-y: auto;
  background: rgba(255, 255, 255, 0.55);
  backdrop-filter: blur(12px);
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
  transition: all 0.3s ease;
  margin-left: 8px;
}
:deep(.leaflet-control-attribution) {
  display: none !important;
}

:deep(.leaflet-bottom.leaflet-right) {
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
.hazard-info-panel {
  position: absolute;
  top: 80px;
  left: 20px;
  width: 300px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(12px);
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  max-height: calc(100vh - 160px);
  overflow-y: auto;
  z-index: 999;
}

.hazard-basic-info {
  padding: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.info-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
}

.info-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.info-row {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.info-label {
  font-size: 12px;
  color: #909399;
  flex-shrink: 0;
}

.info-value {
  font-size: 12px;
  color: #303133;
}

/* 设备列表 */
.device-list-panel {
  padding: 16px;
}

.device-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.device-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  background: #fafafa;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid transparent;
}

.device-item:hover {
  background: #f0f7ff;
  border-color: #1890ff;
}

.device-item.selected {
  background: #e6f7ff;
  border-color: #1890ff;
}

.device-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.device-type-icon {
  font-size: 24px;
}

.device-details {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.device-name {
  font-size: 13px;
  font-weight: 500;
  color: #303133;
}

.device-meta {
  display: flex;
  gap: 8px;
  font-size: 11px;
  color: #909399;
}

.device-status {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
}

.device-status .status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.device-status.online .status-dot {
  background: #52c41a;
}

.device-status.online .status-text {
  color: #52c41a;
}

.device-status.warning .status-dot {
  background: #faad14;
}

.device-status.warning .status-text {
  color: #faad14;
}

.device-status.offline .status-dot {
  background: #f5222d;
}

.device-status.offline .status-text {
  color: #f5222d;
}

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
  margin-bottom: 8px;
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
  gap: 10px;
}

.widget-hazard-name {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  padding-bottom: 6px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.hazard-detail-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.detail-row {
  display: flex;
  gap: 8px;
  font-size: 12px;
  line-height: 1.6;
}

.detail-label {
  color: #909399;
  flex-shrink: 0;
  min-width: 36px;
}

.detail-value {
  color: #303133;
  word-break: break-all;
}

.detail-description {
  color: #606266;
  font-size: 11px;
}

/* 部件设备列表 */
.widget-device-list {
  border-top: 1px solid rgba(0, 0, 0, 0.06);
  padding-top: 8px;
}

.device-list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.device-list-title {
  font-size: 12px;
  font-weight: 600;
  color: #606266;
}

.device-count {
  font-size: 11px;
  color: #909399;
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
  gap: 5px;
  max-height: 220px;
  overflow-y: auto;
}

.device-card {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  background: #fafafa;
  border-radius: 8px;
  border: 1px solid transparent;
  cursor: pointer;
  transition: all 0.15s;
}

.device-card:hover {
  background: #f0f7ff;
  border-color: #bae0ff;
}

.device-card:active {
  background: #e6f7ff;
}

.device-card-icon {
  width: 32px;
  height: 32px;
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
  font-weight: 500;
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
  gap: 3px;
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
  padding: 8px 0;
  background: linear-gradient(135deg, #1890ff 0%, #66b1ff 100%);
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
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
