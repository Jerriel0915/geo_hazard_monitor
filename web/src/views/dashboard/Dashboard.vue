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
        ✕
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
            <span class="info-value">{{ currentHazardPoint?.latitude.toFixed(6) }}, {{ currentHazardPoint?.longitude.toFixed(6) }}</span>
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
            @click="selectDevice(device)"
          >
            <div class="device-info">
              <div class="device-type-icon">
                {{ device.type === 'GNSS' ? '📡' : device.type === 'RAIN' ? '🌧️' : device.type === 'PRESSURE' ? '💧' : '📏' }}
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
        
        <!-- 传感器列表 -->
        <div v-if="selectedDevice && sensorList.length" class="sensor-list-panel">
          <div class="sensor-title">传感器列表</div>
          <div class="sensor-list">
            <div 
              v-for="sensor in sensorList" 
              :key="sensor.id"
              class="sensor-item"
              :class="{ selected: selectedSensor?.id === sensor.id, warning: sensor.status === 'warning' }"
              @click="selectSensor(sensor)"
            >
              <div class="sensor-icon">
                {{ sensor.type === 'GNSS' ? '📍' : sensor.type === 'RAIN' ? '🌧️' : sensor.type === 'PRESSURE' ? '💧' : '📏' }}
              </div>
              <div class="sensor-info">
                <div class="sensor-name">{{ sensor.name }}</div>
                <div class="sensor-code">{{ sensor.code }}</div>
              </div>
              <div class="sensor-status" :class="sensor.status">
                <span class="status-dot"></span>
              </div>
            </div>
          </div>
          
          <!-- 数据曲线图 -->
          <div v-if="showSensorChart && selectedSensor" class="sensor-chart">
            <div class="chart-title">最近7天数据</div>
            <div class="chart-container">
              <svg class="chart-svg" viewBox="0 0 280 100">
                <polyline
                  :points="getChartPoints()"
                  fill="none"
                  stroke="#409eff"
                  stroke-width="2"
                />
                <circle 
                  v-for="(point, index) in sensorChartData" 
                  :key="index"
                  :cx="40 + index * 40"
                  :cy="100 - point * 1.8"
                  r="4"
                  fill="#409eff"
                />
              </svg>
              <div class="chart-labels">
                <span>7天前</span>
                <span>今天</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <div class="left-panel-wrapper" :class="{ collapsed: isPanelCollapsed || currentView === 'hazard' }">
      <div class="left-panel" v-if="currentView === 'system'">
        <div class="panel-content">
          <div class="panel-section health-section">
            <div class="section-header">
              <span class="section-title">系统健康度</span>
              <span class="health-question" @click="showAlgorithmDesc = true" title="健康度算法说明">
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
                  <circle cx="12" cy="12" r="10"/>
                  <path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"/>
                  <line x1="12" y1="17" x2="12.01" y2="17"/>
                </svg>
              </span>
            </div>
            <div class="health-content">
              <div class="health-ring-container">
                <svg class="health-ring" viewBox="0 0 120 120">
                  <circle class="ring-bg" cx="60" cy="60" r="50" />
                  <circle
                    v-for="(segment, index) in ringSegments"
                    :key="index"
                    class="ring-segment"
                    :class="{ active: activeSegment === index }"
                    cx="60"
                    cy="60"
                    r="50"
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
                  v-for="(item, index) in healthStats.items"
                  :key="item.name"
                  class="health-bar-item"
                  :class="{ active: activeSegment === index }"
                  @mouseenter="activeSegment = index"
                  @mouseleave="activeSegment = null"
                >
                  <div class="bar-info">
                    <span class="bar-name">{{ item.name }}</span>
                    <span class="bar-value" :style="{ color: item.color }">{{ item.value }}%</span>
                  </div>
                  <div class="bar-track">
                    <div
                      class="bar-progress"
                      :style="{ width: item.value + '%', backgroundColor: item.color }"
                    ></div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="panel-section resource-section">
            <div class="section-header">
              <span class="section-title">资产情况</span>
            </div>
            <div class="resource-compact">
              <div class="resource-main">
                <div class="resource-total">
                  <div class="total-circle">
                    <svg class="total-ring" viewBox="0 0 80 80">
                      <circle class="ring-bg" cx="40" cy="40" r="35" />
                      <circle class="ring-hazard" cx="40" cy="40" r="35" :stroke-dasharray="`113 170`" stroke-dashoffset="0" />
                      <circle class="ring-device" cx="40" cy="40" r="35" :stroke-dasharray="`142 141`" stroke-dashoffset="-113" />
                    </svg>
                    <div class="total-value">{{ resourceStats.totalResources }}</div>
                  </div>
                  <div class="total-label">资源总数</div>
                </div>
                <div class="resource-breakdown">
                  <div class="breakdown-item hazard">
                    <div class="breakdown-icon">
                      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="#faad14" stroke-width="2" width="14" height="14">
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
                      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="#52c41a" stroke-width="2" width="14" height="14">
                        <circle cx="12" cy="12" r="3"/>
                        <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"/>
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
      </div>
      <button class="panel-toggle-btn" @click="togglePanel">
        <span class="toggle-icon">{{ isPanelCollapsed ? '›' : '‹' }}</span>
      </button>
    </div>
    
    <div class="algorithm-modal" v-if="showAlgorithmDesc" @click="showAlgorithmDesc = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <span class="modal-title">健康度算法说明</span>
          <button class="modal-close" @click="showAlgorithmDesc = false">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="20" height="20">
              <path d="M18 6L6 18M6 6l12 12"/>
            </svg>
          </button>
        </div>
        <div class="modal-body">
          <p>系统健康度综合评估以下五个维度：</p>
          <ul>
            <li><strong>资料完善率</strong>：设备资料登记率与隐患点资料完善率的综合指标</li>
            <li><strong>设备在线率</strong>：在线设备数/隐患点关联设备总数 × 100%</li>
            <li><strong>设备正常率</strong>：状态正常设备数/设备总数 × 100%</li>
            <li><strong>告警及时响应率</strong>：首次告警1小时内响应的事件数/告警事件总数 × 100%</li>
            <li><strong>边坡稳定率</strong>：最近一个月未有效告警隐患点数/总隐患点数 × 100%</li>
          </ul>
          <p style="margin-top: 12px;">综合得分 = 各维度得分 × 权重之和（环形图分色展示各维度占比）</p>
        </div>
      </div>
    </div>
    
    <div class="right-panel-wrapper" :class="{ collapsed: isRightPanelCollapsed }">
      <div class="layer-switcher-wrapper">
        <button class="layer-toggle-btn" @click="toggleLayerList">
          <span class="btn-icon">🗺️</span>
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
        
        <div class="tool-buttons">
          <!-- 查询按钮 -->
          <div class="tool-button-wrapper">
            <div v-show="showSearchPanel" class="tool-panel search-panel">
              <div class="search-input-wrapper">
                <input 
                  v-model="searchQuery" 
                  type="text" 
                  class="search-input" 
                  placeholder="输入隐患点名称..."
                  @input="handleSearch"
                />
                <button class="search-btn" @click="handleSearch">🔍</button>
              </div>
              <div v-show="searchResults.length" class="search-dropdown">
                <div 
                  v-for="point in searchResults" 
                  :key="point.id"
                  class="search-result-item"
                  @click="selectSearchResult(point)"
                >
                  <span class="result-name">{{ point.name }}</span>
                  <span class="result-code">{{ point.code }}</span>
                </div>
              </div>
            </div>
            <button 
              class="tool-btn" 
              @click="toggleSearchPanel"
              :class="{ active: showSearchPanel }"
              title="查询隐患点"
            >
              🔍
            </button>
          </div>
          
          <!-- 图层管理按钮 -->
          <div class="tool-button-wrapper">
            <div v-show="showLayerPanel" class="tool-panel layer-panel">
              <div class="panel-title">图层管理</div>
              
              <div class="layer-group">
                <div class="layer-group-title">地图图层</div>
                <div class="layer-item">
                  <input type="checkbox" v-model="layerSettings.showLabels" @change="toggleLayer('showLabels')">
                  <span>名称标注</span>
                </div>
                <div class="layer-item">
                  <input type="checkbox" v-model="layerSettings.showWater" @change="toggleLayer('showWater')">
                  <span>水系图</span>
                </div>
                <div class="layer-item">
                  <input type="checkbox" v-model="layerSettings.showRoad" @change="toggleLayer('showRoad')">
                  <span>道路图</span>
                </div>
              </div>
              
              <div class="layer-group">
                <div class="layer-group-title">隐患点分组</div>
                <div class="layer-item">
                  <input type="checkbox" v-model="layerSettings.showGroup1" @change="toggleLayer('showGroup1')">
                  <span>第一监测组</span>
                </div>
                <div class="layer-item">
                  <input type="checkbox" v-model="layerSettings.showGroup2" @change="toggleLayer('showGroup2')">
                  <span>第二监测组</span>
                </div>
                <div class="layer-item">
                  <input type="checkbox" v-model="layerSettings.showGroup3" @change="toggleLayer('showGroup3')">
                  <span>第三监测组</span>
                </div>
              </div>
              
              <div class="layer-group">
                <div class="layer-group-title">隐患点状态</div>
                <div class="layer-item">
                  <input type="checkbox" v-model="layerSettings.showMonitoring" @change="toggleLayer('showMonitoring')">
                  <span>监测中</span>
                </div>
                <div class="layer-item">
                  <input type="checkbox" v-model="layerSettings.showStopped" @change="toggleLayer('showStopped')">
                  <span>停测</span>
                </div>
                <div class="layer-item">
                  <input type="checkbox" v-model="layerSettings.showCompleted" @change="toggleLayer('showCompleted')">
                  <span>完结</span>
                </div>
              </div>
            </div>
            <button 
              class="tool-btn" 
              @click="toggleLayerPanel"
              :class="{ active: showLayerPanel }"
              title="图层管理"
            >
              📊
            </button>
          </div>
          
          <!-- 图例说明按钮 -->
          <div class="tool-button-wrapper">
            <div v-show="showLegendPanel" class="tool-panel legend-panel">
              <div class="panel-title">图例说明</div>
              
              <div class="legend-group">
                <div class="legend-group-title">隐患点状态</div>
                <div class="legend-item">
                  <div class="legend-icon" style="background: #409eff;"></div>
                  <span>正常</span>
                </div>
                <div class="legend-item">
                  <div class="legend-icon" style="background: #faad14;"></div>
                  <span>预警</span>
                </div>
                <div class="legend-item">
                  <div class="legend-icon" style="background: #f5222d;"></div>
                  <span>告警</span>
                </div>
              </div>
              
              <div class="legend-group">
                <div class="legend-group-title">告警级别</div>
                <div class="legend-item">
                  <div class="legend-ripple" style="border-color: #f5222d;"></div>
                  <span>严重告警</span>
                </div>
                <div class="legend-item">
                  <div class="legend-ripple" style="border-color: #faad14;"></div>
                  <span>重要告警</span>
                </div>
                <div class="legend-item">
                  <div class="legend-ripple" style="border-color: #722ed1;"></div>
                  <span>一般告警</span>
                </div>
                <div class="legend-item">
                  <div class="legend-ripple" style="border-color: #1890ff;"></div>
                  <span>提示告警</span>
                </div>
              </div>
              
              <div class="legend-group">
                <div class="legend-group-title">其他图标</div>
                <div class="legend-item">
                  <span class="legend-text">📍</span>
                  <span>隐患点位置</span>
                </div>
                <div class="legend-item">
                  <span class="legend-text">⚡</span>
                  <span>有告警</span>
                </div>
              </div>
            </div>
            <button 
              class="tool-btn" 
              @click="toggleLegendPanel"
              :class="{ active: showLegendPanel }"
              title="图例说明"
            >
              📋
            </button>
          </div>
        </div>
      </div>
      
      <button class="right-panel-toggle-btn" @click="toggleRightPanel">
        <span class="toggle-icon">{{ isRightPanelCollapsed ? '‹' : '›' }}</span>
      </button>
      <div class="right-panel">
        <div class="panel-content">
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
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'

const mapContainer = ref<HTMLDivElement | null>(null)
let mapInstance: L.Map | null = null
let baseLayer: L.TileLayer | null = null
let labelLayer: L.TileLayer | null = null

const TIANDI_TU_KEY = '8dda07d4649c77efd0537a0ff0a1df13'

const layerOptions = [
  { id: 'image', name: '影像图', color: '#87CEEB', baseUrl: 'img_w', baseLayer: 'img', labelUrl: 'cia_w', labelLayer: 'cia' },
  { id: 'vector', name: '矢量图', color: '#90EE90', baseUrl: 'vec_w', baseLayer: 'vec', labelUrl: 'cva_w', labelLayer: 'cva' },
  { id: 'terrain', name: '地形图', color: '#DEB887', baseUrl: 'ter_w', baseLayer: 'ter', labelUrl: 'cta_w', labelLayer: 'cta' }
]

const currentLayer = ref('image')
const showLayerList = ref(false)
const isPanelCollapsed = ref(false)
const activeSegment = ref<number | null>(null)
const showAlgorithmDesc = ref(false)

const currentLayerName = ref('影像图')

const healthStats = ref({
  overallScore: 95,
  items: [
    { name: '资料完善率', value: 95, weight: 0.2, color: '#52c41a' },
    { name: '设备在线率', value: 96, weight: 0.15, color: '#1890ff' },
    { name: '设备正常率', value: 94, weight: 0.15, color: '#722ed1' },
    { name: '告警及时响应率', value: 90, weight: 0.2, color: '#fa8c16' },
    { name: '边坡稳定率', value: 97, weight: 0.3, color: '#eb2f96' }
  ]
})

const resourceStats = ref({
  totalResources: 156,
  deviceTotal: 98,
  hazardTotal: 45,
  deviceTypes: [
    { name: 'GNSS接收机', count: 25 },
    { name: '雨量计', count: 18 },
    { name: '渗压计', count: 15 },
    { name: '位移计', count: 20 },
    { name: '视频设备', count: 20 }
  ]
})

const isRightPanelCollapsed = ref(false)

// 视图模式
const currentView = ref<'system' | 'hazard'>('system')
const currentHazardPoint = ref<typeof hazardPoints.value[0] | null>(null)
const showHazardList = ref(false)

// 工具按钮状态
const showSearchPanel = ref(false)
const showLayerPanel = ref(false)
const showLegendPanel = ref(false)
const searchQuery = ref('')
const searchResults = ref<any[]>([])
const flashingPointId = ref<number | null>(null)

// 设备列表
const deviceList = ref([
  {
    id: 1,
    name: 'GNSS接收机-001',
    type: 'GNSS',
    typeName: 'GNSS接收机',
    status: 'online',
    sensorCount: 3,
    longitude: 104.085,
    latitude: 30.652
  },
  {
    id: 2,
    name: '雨量计-003',
    type: 'RAIN',
    typeName: '雨量计',
    status: 'online',
    sensorCount: 1,
    longitude: 104.088,
    latitude: 30.655
  },
  {
    id: 3,
    name: '渗压计-012',
    type: 'PRESSURE',
    typeName: '渗压计',
    status: 'warning',
    sensorCount: 2,
    longitude: 104.082,
    latitude: 30.658
  },
  {
    id: 4,
    name: '位移计-005',
    type: 'DISPLACEMENT',
    typeName: '位移计',
    status: 'offline',
    sensorCount: 4,
    longitude: 104.090,
    latitude: 30.653
  }
])

// 传感器列表
const sensorList = ref<any[]>([])
const selectedDevice = ref<typeof deviceList.value[0] | null>(null)
const selectedSensor = ref<any | null>(null)
const showSensorChart = ref(false)
const sensorChartData = ref<number[]>([])

// 生成最近7天的模拟数据
const generateSensorData = () => {
  const data: number[] = []
  for (let i = 0; i < 7; i++) {
    data.push(Math.random() * 50 + Math.sin(i * 0.5) * 10)
  }
  return data
}

// 图层设置
const layerSettings = ref({
  showLabels: true,
  showWater: false,
  showRoad: false,
  showGroup1: true,
  showGroup2: true,
  showGroup3: true,
  showMonitoring: true,
  showStopped: false,
  showCompleted: false
})

const focusAreaBounds = ref([
  [30.60, 104.00],
  [30.70, 104.15],
  [30.65, 104.25],
  [30.55, 104.20],
  [30.60, 104.00]
])

const hazardPoints = ref([
  {
    id: 1,
    name: 'K12+345 隐患点',
    code: 'HZD-001',
    longitude: 104.08,
    latitude: 30.65,
    groupId: 1,
    groupName: '第一监测组',
    description: '该区域边坡存在滑坡风险，需重点监测',
    deviceCount: 8,
    status: 'normal',
    alarmLevel: 'critical'
  },
  {
    id: 2,
    name: 'K15+678 隐患点',
    code: 'HZD-002',
    longitude: 104.12,
    latitude: 30.68,
    groupId: 1,
    groupName: '第一监测组',
    description: '道路边坡，雨季需加强监测',
    deviceCount: 6,
    status: 'warning',
    alarmLevel: 'major'
  },
  {
    id: 3,
    name: 'K18+123 隐患点',
    code: 'HZD-003',
    longitude: 104.18,
    latitude: 30.62,
    groupId: 2,
    groupName: '第二监测组',
    description: '小型滑坡隐患，定期监测',
    deviceCount: 4,
    status: 'normal',
    alarmLevel: null
  },
  {
    id: 4,
    name: 'K20+456 隐患点',
    code: 'HZD-004',
    longitude: 104.05,
    latitude: 30.67,
    groupId: 2,
    groupName: '第二监测组',
    description: '高边坡区域，重点防护',
    deviceCount: 10,
    status: 'warning',
    alarmLevel: 'minor'
  },
  {
    id: 5,
    name: 'K22+789 隐患点',
    code: 'HZD-005',
    longitude: 104.15,
    latitude: 30.60,
    groupId: 3,
    groupName: '第三监测组',
    description: '已治理区域，常规监测',
    deviceCount: 3,
    status: 'normal',
    alarmLevel: null
  },
  {
    id: 6,
    name: 'K25+012 隐患点',
    code: 'HZD-006',
    longitude: 104.20,
    latitude: 30.64,
    groupId: 3,
    groupName: '第三监测组',
    description: '新发现隐患，待评估',
    deviceCount: 5,
    status: 'danger',
    alarmLevel: 'info'
  }
])

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

const alarmStats = ref({
  pendingCount: 12,
  historyCount: 156,
  levelStats: [
    { key: 'critical', name: '严重', count: 3 },
    { key: 'major', name: '重要', count: 5 },
    { key: 'minor', name: '一般', count: 4 },
    { key: 'info', name: '提示', count: 8 }
  ],
  recentAlarms: [
    { id: 1, level: 'critical', title: '边坡位移超限告警', source: 'K12+345 隐患点', time: '2分钟前' },
    { id: 2, level: 'major', title: '设备离线告警', source: 'GNSS-001', time: '15分钟前' },
    { id: 3, level: 'minor', title: '雨量超标提醒', source: '雨量计-003', time: '30分钟前' },
    { id: 4, level: 'info', title: '数据上报延迟', source: '渗压计-012', time: '1小时前' }
  ]
})

const ringSegments = computed(() => {
  const circumference = 2 * Math.PI * 50
  let currentOffset = 0
  return healthStats.value.items.map((item, index) => {
    const segmentLength = (item.weight * circumference * item.value / 100)
    const gapLength = 3
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
    dashArray: '8,4',
    zIndex: 10
  }).addTo(mapInstance)
}

const addMaskLayer = () => {
  if (!mapInstance) return
  
  const southWest = L.latLng(30.40, 103.80)
  const northEast = L.latLng(30.90, 104.40)
  const mapBounds = L.latLngBounds(southWest, northEast)
  
  const maskGeoJson = {
    type: 'Polygon',
    coordinates: [
      [
        [mapBounds.getWest(), mapBounds.getSouth()],
        [mapBounds.getEast(), mapBounds.getSouth()],
        [mapBounds.getEast(), mapBounds.getNorth()],
        [mapBounds.getWest(), mapBounds.getNorth()],
        [mapBounds.getWest(), mapBounds.getSouth()]
      ],
      focusAreaBounds.value.map(point => [point[1], point[0]])
    ]
  }
  
  maskLayer = L.geoJSON(maskGeoJson, {
    style: {
      fillColor: '#000000',
      fillOpacity: 0.35,
      color: 'transparent',
      weight: 0,
      zIndex: 5
    }
  }).addTo(mapInstance)
}

const addHazardPoints = () => {
  if (!mapInstance) return
  
  hazardMarkerLayer = L.layerGroup().addTo(mapInstance)
  
  hazardPoints.value.forEach(point => {
    const marker = L.circleMarker([point.latitude, point.longitude], {
      radius: 10,
      fillColor: point.alarmLevel ? alarmColors[point.alarmLevel] : '#409eff',
      color: '#ffffff',
      weight: 2,
      opacity: 1,
      fillOpacity: 0.8,
      zIndex: 20
    }).addTo(hazardMarkerLayer)
    
    // 存储marker引用
    hazardMarkerMap.set(point.id, marker)
    
    if (point.alarmLevel) {
      startRipple(point)
    }
    
    const popupContent = `
      <div class="hazard-popup">
        <div class="popup-title">${point.name}</div>
        <div class="popup-code">${point.code}</div>
        <div class="popup-info">
          <div class="info-row">
            <span class="info-label">坐标位置:</span>
            <span class="info-value">${point.latitude.toFixed(6)}, ${point.longitude.toFixed(6)}</span>
          </div>
          <div class="info-row">
            <span class="info-label">所属分组:</span>
            <span class="info-value">${point.groupName}</span>
          </div>
          <div class="info-row">
            <span class="info-label">绑定设备:</span>
            <span class="info-value">${point.deviceCount}台</span>
          </div>
          <div class="info-row">
            <span class="info-label">描述:</span>
            <span class="info-value">${point.description}</span>
          </div>
        </div>
      </div>
    `
    
    marker.bindPopup(popupContent, {
      maxWidth: 280,
      closeButton: false,
      autoClose: true,
      offset: L.point(0, -15)
    })
    
    marker.on('click', () => {
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
  const center = [point.latitude, point.longitude]
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
        fillOpacity: 0.1,
        zIndex: 15
      }).addTo(mapInstance!)
      
      circles.push(ripple)
      
      ripple.setStyle({
        transition: 'all 2s ease-out'
      })
      
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

// 工具按钮切换函数
const toggleSearchPanel = () => {
  showSearchPanel.value = !showSearchPanel.value
  showLayerPanel.value = false
  showLegendPanel.value = false
}

const toggleLayerPanel = () => {
  showLayerPanel.value = !showLayerPanel.value
  showSearchPanel.value = false
  showLegendPanel.value = false
}

const toggleLegendPanel = () => {
  showLegendPanel.value = !showLegendPanel.value
  showSearchPanel.value = false
  showLayerPanel.value = false
}

// 搜索功能
const handleSearch = () => {
  if (!searchQuery.value.trim()) {
    searchResults.value = []
    return
  }
  
  const query = searchQuery.value.toLowerCase()
  searchResults.value = hazardPoints.value.filter(point => 
    point.name.toLowerCase().includes(query) || 
    point.code.toLowerCase().includes(query)
  )
}

const selectSearchResult = (point: any) => {
  // 进入隐患点视图
  enterHazardView(point)
  
  // 关闭搜索面板
  showSearchPanel.value = false
  searchQuery.value = ''
  searchResults.value = []
}

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
    const currentOpacity = marker.options.fillOpacity
    marker.setStyle({
      fillOpacity: currentOpacity > 0.5 ? 0.2 : 1
    })
    
    if (flashCount >= maxFlashes) {
      clearInterval(flashInterval)
      marker.setStyle({ fillOpacity: 0.8 })
      flashingPointId.value = null
    }
  }, 300)
}

// 图层控制
const toggleLayer = (layerKey: string) => {
  console.log('Toggle layer:', layerKey, layerSettings.value[layerKey as keyof typeof layerSettings.value])
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
  // 模拟当前隐患点的告警数据
  alarmStats.value.pendingCount = 3
  alarmStats.value.historyCount = 28
  alarmStats.value.levelStats = [
    { key: 'critical', name: '严重', count: 1 },
    { key: 'major', name: '重要', count: 1 },
    { key: 'minor', name: '一般', count: 1 },
    { key: 'info', name: '提示', count: 0 }
  ]
  alarmStats.value.recentAlarms = [
    { id: 1, level: 'critical', title: '位移超限告警', source: 'GNSS接收机-001', time: '5分钟前' },
    { id: 2, level: 'major', title: '数据异常', source: '渗压计-012', time: '30分钟前' },
    { id: 3, level: 'minor', title: '设备离线', source: '位移计-005', time: '2小时前' }
  ]
}

const resetAlarmStats = () => {
  alarmStats.value = {
    pendingCount: 12,
    historyCount: 156,
    levelStats: [
      { key: 'critical', name: '严重', count: 3 },
      { key: 'major', name: '重要', count: 5 },
      { key: 'minor', name: '一般', count: 4 },
      { key: 'info', name: '提示', count: 8 }
    ],
    recentAlarms: [
      { id: 1, level: 'critical', title: '位移超限告警', source: 'K12+345隐患点', time: '10分钟前' },
      { id: 2, level: 'major', title: '设备离线告警', source: 'GNSS-001', time: '15分钟前' },
      { id: 3, level: 'minor', title: '雨量超标提醒', source: '雨量计-003', time: '30分钟前' },
      { id: 4, level: 'info', title: '数据上报延迟', source: '渗压计-012', time: '1小时前' }
    ]
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

const addDeviceMarkers = (hazardId: number) => {
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
  
  // 添加设备标记
  deviceList.value.forEach(device => {
    const icon = createDeviceIcon(device.status)
    const marker = L.marker([device.latitude, device.longitude], { icon })
      .addTo(hazardMarkerLayer!)
      .bindPopup(`
        <div style="padding: 8px; min-width: 180px;">
          <div style="font-weight: 600; margin-bottom: 8px;">${device.name}</div>
          <div style="font-size: 12px; color: #666;">
            <div>类型: ${device.typeName}</div>
            <div>传感器: ${device.sensorCount}个</div>
            <div>状态: ${getStatusText(device.status)}</div>
          </div>
        </div>
      `)
  })
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

onMounted(() => {
  initMap()
  window.addEventListener('resize', handleResize)
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
  border-color: #409eff;
}

.btn-icon {
  font-size: 16px;
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
  top: calc(100% + 8px);
  right: 0;
  background: rgba(255, 255, 255, 0.95);
  border: 1px solid #e4e7ed;
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
  color: #409eff;
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
  border: 1px solid #e4e7ed;
}

.zoom-controls {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  display: flex;
  flex-direction: column;
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
  border-color: #409eff;
}

.zoom-in {
  color: #409eff;
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
  width: 240px;
  background: rgba(255, 255, 255, 0.55);
  backdrop-filter: blur(12px);
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
  overflow: hidden;
  transition: all 0.3s ease;
}

.left-panel-wrapper.collapsed .left-panel {
  width: 0;
  padding: 0;
  overflow: hidden;
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

.health-question {
  color: #409eff;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  transition: background 0.2s;
}

.health-question:hover {
  background: rgba(64, 158, 255, 0.1);
}

.health-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.health-ring-container {
  position: relative;
  width: 100px;
  height: 100px;
  margin: 0 auto;
}

.health-ring {
  width: 100%;
  height: 100%;
  transform: rotate(-90deg);
}

.health-ring .ring-bg {
  fill: none;
  stroke: rgba(0, 0, 0, 0.06);
  stroke-width: 8;
}

.ring-segment {
  fill: none;
  stroke-width: 8;
  stroke-linecap: round;
  transition: all 0.3s ease;
  cursor: pointer;
}

.ring-segment.active {
  stroke-width: 10;
  filter: drop-shadow(0 0 4px currentColor);
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
  font-size: 20px;
  font-weight: 700;
  color: #1f2937;
}

.ring-label {
  font-size: 10px;
  color: #6b7280;
}

.health-bars {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.health-bar-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 4px 6px;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.health-bar-item.active {
  background: rgba(64, 158, 255, 0.08);
}

.bar-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.bar-name {
  font-size: 11px;
  color: #4b5563;
}

.bar-value {
  font-size: 11px;
  font-weight: 600;
}

.bar-track {
  height: 4px;
  background: rgba(0, 0, 0, 0.06);
  border-radius: 2px;
  overflow: hidden;
}

.bar-progress {
  height: 100%;
  border-radius: 2px;
  transition: width 0.5s ease;
}

.resource-compact {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.resource-main {
  display: flex;
  align-items: center;
  gap: 12px;
}

.resource-total {
  flex-shrink: 0;
  text-align: center;
}

.total-circle {
  position: relative;
  width: 70px;
  height: 70px;
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
  margin-top: 4px;
  font-size: 10px;
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
  width: 26px;
  height: 26px;
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
  font-size: 15px;
  font-weight: 700;
  color: #1f2937;
}

.breakdown-label {
  font-size: 10px;
  color: #6b7280;
}

.device-type-section {
  border-top: 1px solid rgba(79, 172, 254, 0.2);
  padding-top: 12px;
}

.type-title {
  font-size: 11px;
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

.type-name {
  font-size: 11px;
  color: #6b7280;
}

.type-count {
  font-size: 11px;
  color: #3b82f6;
  font-weight: 600;
}

.panel-toggle-btn {
  width: 32px;
  height: 60px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-left: none;
  border-radius: 0 8px 8px 0;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  box-shadow: 2px 0 10px rgba(0, 0, 0, 0.05);
}

.panel-toggle-btn:hover {
  background: rgba(64, 158, 255, 0.1);
}

.toggle-icon {
  font-size: 18px;
  color: #409eff;
  font-weight: bold;
  transition: transform 0.2s ease;
}

.algorithm-modal {
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

.modal-content {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
  width: 90%;
  max-width: 480px;
  overflow: hidden;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
}

.modal-title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
}

.modal-close {
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

.modal-close:hover {
  background: rgba(0, 0, 0, 0.1);
  color: #374151;
}

.modal-body {
  padding: 20px;
  max-height: 400px;
  overflow-y: auto;
}

.modal-body p {
  font-size: 14px;
  color: #4b5563;
  line-height: 1.6;
  margin: 0 0 12px 0;
}

.modal-body ul {
  margin: 0;
  padding-left: 20px;
}

.modal-body li {
  font-size: 13px;
  color: #6b7280;
  line-height: 1.8;
  margin-bottom: 8px;
}

.modal-body li:last-child {
  margin-bottom: 0;
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

.right-panel-wrapper.collapsed .right-panel {
  width: 0;
  padding: 0;
  overflow: hidden;
  margin-left: 0;
}

.right-panel {
  width: 280px;
  background: rgba(255, 255, 255, 0.55);
  backdrop-filter: blur(12px);
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
  overflow: hidden;
  transition: all 0.3s ease;
  margin-left: 8px;
}

.right-panel-toggle-btn {
  width: 32px;
  height: 60px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-right: none;
  border-radius: 8px 0 0 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  box-shadow: -2px 0 10px rgba(0, 0, 0, 0.05);
}

.right-panel-toggle-btn:hover {
  background: rgba(64, 158, 255, 0.1);
}

.alarm-summary {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}

.alarm-summary-item {
  flex: 1;
  text-align: center;
  padding: 8px;
  background: rgba(245, 247, 250, 0.8);
  border-radius: 8px;
}

.summary-badge {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
  display: inline-block;
  margin-bottom: 4px;
}

.summary-badge.pending {
  background: rgba(250, 173, 20, 0.15);
  color: #faad14;
}

.summary-badge.history {
  background: rgba(144, 202, 249, 0.15);
  color: #1890ff;
}

.summary-count {
  font-size: 18px;
  font-weight: 700;
  color: #1f2937;
}

.alarm-level-stats {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 8px 0;
  border-top: 1px solid rgba(79, 172, 254, 0.2);
  border-bottom: 1px solid rgba(79, 172, 254, 0.2);
}

.level-stat {
  display: flex;
  align-items: center;
  gap: 8px;
}

.level-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.level-dot.critical {
  background: #f5222d;
}

.level-dot.major {
  background: #faad14;
}

.level-dot.minor {
  background: #722ed1;
}

.level-dot.info {
  background: #1890ff;
}

.level-name {
  flex: 1;
  font-size: 12px;
  color: #6b7280;
}

.level-count {
  font-size: 13px;
  font-weight: 600;
  color: #374151;
}

.alarm-list-section {
  margin-top: 12px;
}

.list-header {
  margin-bottom: 8px;
}

.list-title {
  font-size: 12px;
  font-weight: 600;
  color: #4b5563;
}

.alarm-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.alarm-item {
  display: flex;
  gap: 10px;
  padding: 8px;
  background: rgba(245, 247, 250, 0.6);
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.2s ease;
}

.alarm-item:hover {
  background: rgba(245, 247, 250, 1);
}

.alarm-level-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
  margin-top: 2px;
}

.alarm-level-dot.critical {
  background: #f5222d;
}

.alarm-level-dot.major {
  background: #faad14;
}

.alarm-level-dot.minor {
  background: #722ed1;
}

.alarm-level-dot.info {
  background: #1890ff;
}

.alarm-content {
  flex: 1;
  min-width: 0;
}

.alarm-title {
  font-size: 12px;
  color: #374151;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.alarm-meta {
  font-size: 11px;
  color: #9ca3af;
  margin-top: 2px;
}

.hazard-popup {
  padding: 12px;
  font-size: 13px;
}

.hazard-popup .popup-title {
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 4px;
}

.hazard-popup .popup-code {
  font-size: 11px;
  color: #6b7280;
  margin-bottom: 10px;
}

.hazard-popup .popup-info {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.hazard-popup .info-row {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.hazard-popup .info-label {
  font-size: 12px;
  color: #6b7280;
  font-weight: 500;
  flex-shrink: 0;
}

.hazard-popup .info-value {
  font-size: 12px;
  color: #374151;
}

:deep(.leaflet-popup-content-wrapper) {
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
  border: none;
}

:deep(.leaflet-popup-tip-container) {
  display: none;
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

/* 工具按钮样式 */
.tool-buttons {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 8px;
}

.tool-button-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
}

.tool-btn {
  width: 40px;
  height: 40px;
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(228, 231, 237, 0.7);
  border-radius: 8px;
  cursor: pointer;
  font-size: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.tool-btn:hover {
  background: #f0f7ff;
  border-color: #409eff;
  transform: scale(1.05);
}

.tool-btn.active {
  background: rgba(64, 158, 255, 0.1);
  border-color: #409eff;
  color: #409eff;
}

.tool-panel {
  background: rgba(255, 255, 255, 0.95);
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
  padding: 12px;
  min-width: 220px;
  max-width: 280px;
  max-height: 400px;
  overflow-y: auto;
  animation: slideInLeft 0.2s ease;
}

@keyframes slideInLeft {
  from {
    opacity: 0;
    transform: translateX(10px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

/* 搜索面板样式 */
.search-input-wrapper {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}

.search-input {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  font-size: 13px;
  outline: none;
  transition: all 0.2s ease;
}

.search-input:focus {
  border-color: #409eff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.1);
}

.search-btn {
  padding: 8px 12px;
  background: #409eff;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s ease;
}

.search-btn:hover {
  background: #66b1ff;
}

.search-dropdown {
  max-height: 200px;
  overflow-y: auto;
  border-top: 1px solid #f0f0f0;
  padding-top: 8px;
}

.search-result-item {
  padding: 8px 10px;
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.2s ease;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.search-result-item:hover {
  background: #f0f7ff;
}

.result-name {
  font-size: 13px;
  color: #303133;
  font-weight: 500;
}

.result-code {
  font-size: 11px;
  color: #909399;
}

/* 图层管理面板样式 */
.panel-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
}

.layer-group {
  margin-bottom: 12px;
}

.layer-group:last-child {
  margin-bottom: 0;
}

.layer-group-title {
  font-size: 12px;
  font-weight: 500;
  color: #606266;
  margin-bottom: 8px;
}

.layer-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
  font-size: 13px;
  color: #303133;
  cursor: pointer;
  transition: all 0.2s ease;
}

.layer-item:hover {
  color: #409eff;
}

.layer-item input[type="checkbox"] {
  width: 16px;
  height: 16px;
  cursor: pointer;
  accent-color: #409eff;
}

/* 图例面板样式 */
.legend-group {
  margin-bottom: 16px;
}

.legend-group:last-child {
  margin-bottom: 0;
}

.legend-group-title {
  font-size: 12px;
  font-weight: 500;
  color: #606266;
  margin-bottom: 8px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 0;
  font-size: 13px;
  color: #303133;
}

.legend-icon {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  border: 2px solid white;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.15);
}

.legend-ripple {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  border: 3px solid;
  position: relative;
}

.legend-ripple::after {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}

.legend-text {
  font-size: 16px;
}

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
  background: #e4e7ed;
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
  color: #409eff;
}

.close-hazard-view-btn {
  width: 36px;
  height: 36px;
  background: #f5f7fa;
  border: 1px solid #e4e7ed;
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
  border-color: #409eff;
}

.device-item.selected {
  background: #e6f7ff;
  border-color: #409eff;
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
  border-color: #409eff;
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
</style>