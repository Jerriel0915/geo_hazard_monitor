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
        <el-icon><Operation/></el-icon>
      </button>
      <div v-show="showLayerPanel" class="tool-panel layer-panel">
        <div class="panel-title">图层控制</div>
        <el-tree
          ref="treeRef"
          :data="treeData"
          show-checkbox
          node-key="id"
          :default-expanded-keys="expandedKeys"
          :default-checked-keys="defaultCheckedKeys"
          :props="{ label: 'label', children: 'children' }"
          @check="onTreeCheck"
          class="layer-tree"
        />
      </div>
    </div>

    <!-- 地理工具箱 -->
    <MapDrawToolbar :map-instance="mapInstance" />

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

    <!-- 图例开关 -->
    <button class="tool-btn" @click="$emit('toggleLegend')" :class="{ active: legendVisible }"
            :title="legendVisible ? '隐藏图例' : '显示图例'">
      <el-icon><PieChart/></el-icon>
    </button>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import L from 'leaflet'
import { Aim, Hide, Operation, PieChart, Search, Setting, View } from '@element-plus/icons-vue'
import type { ElTree } from 'element-plus'
import MapDrawToolbar from './MapDrawToolbar.vue'

const props = defineProps<{
  hazardPoints: any[]
  mapInstance: L.Map | null
  maskVisible: boolean
  legendVisible: boolean
  layoutDialogVisible: boolean
  rightPanelCollapsed: boolean
  groups: { id: number; name: string }[]
  hazardPointStatuses: { key: string; label: string }[]
  deviceStatuses: { key: string; label: string }[]
}>()

const emit = defineEmits<{
  (e: 'selectHazardPoint', point: any): void
  (e: 'toggleLayers', activeKeys: string[]): void
  (e: 'openLayoutConfig'): void
  (e: 'toggleMask'): void
  (e: 'toggleLegend'): void
  (e: 'resetView'): void
}>()

const treeRef = ref<InstanceType<typeof ElTree> | null>(null)
const showSearchPanel = ref(false)
const showLayerPanel = ref(false)
const searchQuery = ref('')
const searchResults = ref<any[]>([])
let debounceTimer: ReturnType<typeof setTimeout> | null = null

const treeData = computed(() => [
  {
    id: 'tile-layers', label: '图层叠加', children: [
      { id: 'showLabels', label: '文字标注' },
    ]
  },
  {
    id: 'hazard-status', label: '隐患点状态', children: props.hazardPointStatuses.map(hs => ({ id: `hstatus_${hs.key}`, label: hs.label }))
  },
  {
    id: 'device-status', label: '设备状态', children: props.deviceStatuses.map(ds => ({ id: `dstatus_${ds.key}`, label: ds.label }))
  }
])

const expandedKeys = ['tile-layers', 'hazard-status', 'device-status']

const defaultCheckedKeys = computed(() => {
  const keys = ['showLabels']
  // 隐患点：默认仅勾选"监测中"
  props.hazardPointStatuses
    .filter(hs => hs.key !== 'stopped' && hs.key !== 'completed')
    .forEach(hs => keys.push(`hstatus_${hs.key}`))
  // 设备：默认取消"停用"
  props.deviceStatuses
    .filter(ds => ds.key !== 'stopped')
    .forEach(ds => keys.push(`dstatus_${ds.key}`))
  return keys
})

const toggleSearchPanel = () => {
  showSearchPanel.value = !showSearchPanel.value
  if (showSearchPanel.value) showLayerPanel.value = false
}

const toggleLayerPanel = () => {
  showLayerPanel.value = !showLayerPanel.value
  if (showLayerPanel.value) showSearchPanel.value = false
}

const closeAllPanels = () => {
  showSearchPanel.value = false
  showLayerPanel.value = false
}

const handleSearch = () => {
  if (debounceTimer) clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => {
    if (!searchQuery.value.trim()) {
      searchResults.value = []
      return
    }
    const query = searchQuery.value.toLowerCase()
    searchResults.value = props.hazardPoints.filter(point =>
      point.name.toLowerCase().includes(query) || point.code.toLowerCase().includes(query)
    )
  }, 300)
}

const onSelectSearchResult = (point: any) => {
  emit('selectHazardPoint', point)
  showSearchPanel.value = false
  searchQuery.value = ''
  searchResults.value = []
}

const onTreeCheck = () => {
  // Only emit leaf keys — parent IDs are irrelevant for layer logic
  const keys = (treeRef.value?.getCheckedKeys(true) || []) as string[]
  emit('toggleLayers', keys)
}

// Sync tree checked state when data loads asynchronously
watch([() => props.hazardPointStatuses, () => props.deviceStatuses], () => {
  if (treeRef.value) {
    nextTick(() => {
      const keys = ['showLabels']
      props.hazardPointStatuses
        .filter(hs => hs.key !== 'stopped' && hs.key !== 'completed')
        .forEach(hs => keys.push(`hstatus_${hs.key}`))
      props.deviceStatuses
        .filter(ds => ds.key !== 'stopped')
        .forEach(ds => keys.push(`dstatus_${ds.key}`))
      treeRef.value!.setCheckedKeys(keys, true)
    })
  }
}, { deep: true })

// Click outside to close panels
const handleClickOutside = (event: MouseEvent) => {
  const target = event.target as HTMLElement
  if (!target.closest('.tool-button-wrapper') && !target.closest('.tool-panel')) {
    closeAllPanels()
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
  if (debounceTimer) clearTimeout(debounceTimer)
})
</script>

<style scoped>
.map-business-toolbar {
  position: absolute;
  top: 16px;
  right: 16px;
  z-index: 999;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
  padding: 8px 0;
  transition: right 0.3s ease;
}

.map-business-toolbar.panel-open {
  right: 368px;
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
  top: 0;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: 10px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  padding: 12px;
  min-width: 220px;
  max-width: 280px;
  max-height: calc(100vh - 80px);
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

.layer-tree {
  --el-tree-node-content-height: 28px;
  font-size: 13px;
}

.layer-tree :deep(.el-tree-node__content) {
  height: 28px;
}

/* 根节点取消缩进 */
.layer-tree > :deep(.el-tree-node > .el-tree-node__content) {
  padding-left: 4px !important;
}

.layer-tree :deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
  background-color: #1890ff;
  border-color: #1890ff;
}
</style>
