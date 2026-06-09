<template>
  <div class="map-business-toolbar" :class="{ 'panel-open': !rightPanelCollapsed }">
    <!-- 归心 -->
    <button class="tool-btn" @click="$emit('resetView')" title="归心 - 复原视角">
      <el-icon><Aim/></el-icon>
    </button>

    <!-- 搜索 -->
    <div class="tool-button-wrapper">
      <button class="tool-btn" @click="toggleSearchPanel" :class="{ active: showSearchPanel }" title="搜索隐患点">
        <el-icon><Search/></el-icon>
      </button>
      <div v-show="showSearchPanel" class="tool-panel search-panel">
        <div class="search-input-wrapper">
          <input v-model="searchQuery" type="text" class="search-input" placeholder="输入隐患点名称..."
                 @input="handleSearch"/>
          <button class="search-btn" @click="handleSearch">
            <el-icon><Search/></el-icon>
          </button>
        </div>
        <div v-show="searchResults.length" class="search-dropdown">
          <div v-for="point in searchResults" :key="point.id" class="search-result-item"
               @click="onSelectSearchResult(point)">
            <span class="result-name">{{ point.name }}</span>
            <span class="result-code">{{ point.code }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 图层控制 -->
    <div class="tool-button-wrapper">
      <button class="tool-btn" @click="toggleLayerPanel" :class="{ active: showLayerPanel }" title="图层控制">
        <el-icon><DataAnalysis/></el-icon>
      </button>
      <div v-show="showLayerPanel" class="tool-panel layer-panel">
        <div class="panel-title">图层控制</div>
        <div class="layer-group">
          <div class="layer-group-title">Tile 图层</div>
          <div class="layer-item">
            <input type="checkbox" v-model="layerSettings.showLabels" @change="onToggleLayer('showLabels')">
            <span>名称标注</span>
          </div>
          <div class="layer-item">
            <input type="checkbox" v-model="layerSettings.showWater" @change="onToggleLayer('showWater')">
            <span>水系图</span>
          </div>
          <div class="layer-item">
            <input type="checkbox" v-model="layerSettings.showRoad" @change="onToggleLayer('showRoad')">
            <span>道路图</span>
          </div>
        </div>
        <div class="layer-group">
          <div class="layer-group-title">隐患点分组</div>
          <div class="layer-item">
            <input type="checkbox" v-model="layerSettings.showGroup1" @change="onToggleLayer('showGroup1')">
            <span>第一监测组</span>
          </div>
          <div class="layer-item">
            <input type="checkbox" v-model="layerSettings.showGroup2" @change="onToggleLayer('showGroup2')">
            <span>第二监测组</span>
          </div>
          <div class="layer-item">
            <input type="checkbox" v-model="layerSettings.showGroup3" @change="onToggleLayer('showGroup3')">
            <span>第三监测组</span>
          </div>
        </div>
        <div class="layer-group">
          <div class="layer-group-title">隐患点状态</div>
          <div class="layer-item">
            <input type="checkbox" v-model="layerSettings.showMonitoring" @change="onToggleLayer('showMonitoring')">
            <span>监测中</span>
          </div>
          <div class="layer-item">
            <input type="checkbox" v-model="layerSettings.showStopped" @change="onToggleLayer('showStopped')">
            <span>停测</span>
          </div>
          <div class="layer-item">
            <input type="checkbox" v-model="layerSettings.showCompleted" @change="onToggleLayer('showCompleted')">
            <span>完结</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 分隔线 -->
    <div class="toolbar-divider"></div>

    <!-- 面板设置 -->
    <button class="tool-btn" @click="$emit('openLayoutConfig')" :class="{ active: layoutDialogVisible }" title="面板设置">
      <el-icon><Setting/></el-icon>
    </button>

    <!-- 蒙层开关 -->
    <button class="tool-btn" @click="$emit('toggleMask')" :class="{ active: !maskVisible }"
            :title="maskVisible ? '隐藏蒙层' : '显示蒙层'">
      <el-icon><component :is="maskVisible ? View : Hide"/></el-icon>
    </button>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Aim, DataAnalysis, Hide, Search, Setting, View } from '@element-plus/icons-vue'

const props = defineProps<{
  hazardPoints: any[]
  maskVisible: boolean
  layoutDialogVisible: boolean
  rightPanelCollapsed: boolean
}>()

const emit = defineEmits<{
  (e: 'selectHazardPoint', point: any): void
  (e: 'toggleLayer', layerKey: string): void
  (e: 'openLayoutConfig'): void
  (e: 'toggleMask'): void
  (e: 'resetView'): void
}>()

const showSearchPanel = ref(false)
const showLayerPanel = ref(false)
const searchQuery = ref('')
const searchResults = ref<any[]>([])

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

const toggleSearchPanel = () => {
  showSearchPanel.value = !showSearchPanel.value
  showLayerPanel.value = false
}

const toggleLayerPanel = () => {
  showLayerPanel.value = !showLayerPanel.value
  showSearchPanel.value = false
}

const handleSearch = () => {
  if (!searchQuery.value.trim()) {
    searchResults.value = []
    return
  }
  const query = searchQuery.value.toLowerCase()
  searchResults.value = props.hazardPoints.filter(point =>
    point.name.toLowerCase().includes(query) || point.code.toLowerCase().includes(query)
  )
}

const onSelectSearchResult = (point: any) => {
  emit('selectHazardPoint', point)
  showSearchPanel.value = false
  searchQuery.value = ''
  searchResults.value = []
}

const onToggleLayer = (layerKey: string) => {
  emit('toggleLayer', layerKey)
}
</script>

<style scoped>
.map-business-toolbar {
  position: absolute;
  top: 16px;
  right: 0;
  z-index: 999;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
  padding: 8px 0;
  transition: right 0.3s ease;
}

.map-business-toolbar.panel-open {
  right: 352px;
}

.toolbar-divider {
  width: 24px;
  height: 1px;
  background: rgba(0, 0, 0, 0.08);
  margin: 2px 6px;
}

.tool-button-wrapper {
  position: relative;
}

.tool-btn {
  width: 36px;
  height: 36px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: 8px;
  cursor: pointer;
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #606266;
  transition: all 0.2s ease;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.tool-btn:hover {
  background: rgba(24, 144, 255, 0.08);
  border-color: #1890ff;
  color: #1890ff;
}

.tool-btn.active {
  background: rgba(24, 144, 255, 0.1);
  border-color: #1890ff;
  color: #1890ff;
}

/* 面板 — 从按钮左侧展开 */
.tool-panel {
  position: absolute;
  right: calc(100% + 8px);
  top: 50%;
  transform: translateY(-50%);
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: 10px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  padding: 12px;
  min-width: 220px;
  max-width: 280px;
  max-height: 400px;
  overflow-y: auto;
}

/* 搜索面板 */
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
  border-color: #1890ff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.1);
}

.search-btn {
  padding: 8px 12px;
  background: #1890ff;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
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

/* 图层面板 */
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
}

.layer-item:hover {
  color: #1890ff;
}

.layer-item input[type="checkbox"] {
  width: 16px;
  height: 16px;
  cursor: pointer;
  accent-color: #1890ff;
}
</style>
