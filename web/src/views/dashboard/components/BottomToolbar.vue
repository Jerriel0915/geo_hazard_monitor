<template>
  <div class="bottom-toolbar">
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
            <button class="search-btn" @click="handleSearch">
              <el-icon>
                <Search/>
              </el-icon>
            </button>
          </div>
          <div v-show="searchResults.length" class="search-dropdown">
            <div
                v-for="point in searchResults"
                :key="point.id"
                class="search-result-item"
                @click="onSelectSearchResult(point)"
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
          <el-icon>
            <Search/>
          </el-icon>
        </button>
      </div>

      <!-- 图层管理按钮 -->
      <div class="tool-button-wrapper">
        <div v-show="showLayerPanel" class="tool-panel layer-panel">
          <div class="panel-title">图层管理</div>
          <div class="layer-group">
            <div class="layer-group-title">地图图层</div>
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
        <button
            class="tool-btn"
            @click="toggleLayerPanel"
            :class="{ active: showLayerPanel }"
            title="图层管理"
        >
          <el-icon>
            <DataAnalysis/>
          </el-icon>
        </button>
      </div>

      <!-- 图例说明按钮 -->
      <div class="tool-button-wrapper">
        <div v-show="showLegendPanel" class="tool-panel legend-panel">
          <div class="panel-title">图例说明</div>
          <div class="legend-group">
            <div class="legend-group-title">隐患点状态</div>
            <div class="legend-item">
              <div class="legend-icon" style="background: #1890ff;"></div>
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
              <el-icon class="legend-text">
                <Location/>
              </el-icon>
              <span>隐患点位置</span>
            </div>
            <div class="legend-item">
              <el-icon class="legend-text">
                <Lightning/>
              </el-icon>
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
          <el-icon>
            <List/>
          </el-icon>
        </button>
      </div>

      <!-- 面板布局修改按钮 -->
      <button
          class="tool-btn edit-btn"
          @click="$emit('openLayoutConfig')"
          :class="{ active: layoutDialogVisible }"
          title="修改面板布局"
      >
        <el-icon>
          <Setting/>
        </el-icon>
      </button>

      <!-- 蒙层开关按钮 -->
      <button
          class="tool-btn mask-btn"
          @click="$emit('toggleMask')"
          :class="{ active: !maskVisible }"
          :title="maskVisible ? '隐藏蒙层' : '显示蒙层'"
      >
        <svg v-if="maskVisible" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="16" height="16">
          <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
          <circle cx="12" cy="12" r="3"/>
        </svg>
        <svg v-else xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="16" height="16">
          <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94"/>
          <path d="M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19"/>
          <path d="M14.12 14.12a3 3 0 1 1-4.24-4.24"/>
          <line x1="1" y1="1" x2="23" y2="23"/>
        </svg>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import {ref} from 'vue'
import {
  DataAnalysis,
  Lightning,
  List,
  Location,
  Search,
  Setting
} from '@element-plus/icons-vue'

const props = defineProps<{
  hazardPoints: any[]
  maskVisible: boolean
  layoutDialogVisible: boolean
}>()

const emit = defineEmits<{
  (e: 'selectHazardPoint', point: any): void
  (e: 'toggleLayer', layerKey: string): void
  (e: 'openLayoutConfig'): void
  (e: 'toggleMask'): void
}>()

// --- internal state ---
const showSearchPanel = ref(false)
const showLayerPanel = ref(false)
const showLegendPanel = ref(false)
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

// --- toggle functions ---
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

// --- search ---
const handleSearch = () => {
  if (!searchQuery.value.trim()) {
    searchResults.value = []
    return
  }
  const query = searchQuery.value.toLowerCase()
  searchResults.value = props.hazardPoints.filter(point =>
      point.name.toLowerCase().includes(query) ||
      point.code.toLowerCase().includes(query)
  )
}

const onSelectSearchResult = (point: any) => {
  emit('selectHazardPoint', point)
  showSearchPanel.value = false
  searchQuery.value = ''
  searchResults.value = []
}

// --- layer ---
const onToggleLayer = (layerKey: string) => {
  emit('toggleLayer', layerKey)
}
</script>

<style scoped>
/* 右下角工具栏 */
.bottom-toolbar {
  position: absolute;
  bottom: 20px;
  right: 20px;
  z-index: 1000;
  display: flex;
  align-items: flex-end;
  gap: 8px;
}

/* 工具按钮样式 */
.tool-buttons {
  display: flex;
  flex-direction: row;
  gap: 8px;
}

.tool-button-wrapper {
  position: relative;
  display: flex;
  flex-direction: column-reverse;
  align-items: flex-end;
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
  border-color: #1890ff;
  transform: scale(1.05);
}

.tool-btn.active {
  background: rgba(64, 158, 255, 0.1);
  border-color: #1890ff;
  color: #1890ff;
}

.tool-panel {
  position: absolute;
  bottom: calc(100% + 8px);
  right: 0;
  background: rgba(255, 255, 255, 0.95);
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
  padding: 12px;
  min-width: 220px;
  max-width: 280px;
  max-height: 400px;
  overflow-y: auto;
  animation: slideUp 0.2s ease;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
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
  color: #1890ff;
}

.layer-item input[type="checkbox"] {
  width: 16px;
  height: 16px;
  cursor: pointer;
  accent-color: #1890ff;
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

/* 面板布局修改按钮 */
.edit-btn {
  font-size: 16px;
}
</style>
