<template>
  <div class="map-auxiliary-bar">
    <!-- 比例尺由 Leaflet 自带，样式在 Dashboard.vue 中覆盖 -->

    <!-- 图例 (默认展开) -->
    <div class="legend-panel">
      <div class="legend-header" @click="legendExpanded = !legendExpanded">
        <span class="legend-title">图例</span>
        <el-icon :size="12"><component :is="legendExpanded ? ArrowDown : ArrowUp"/></el-icon>
      </div>
      <div v-show="legendExpanded" class="legend-body">
        <div class="legend-group">
          <div class="legend-group-title">隐患点状态</div>
          <div class="legend-item">
            <img src="/img/sy/auto_normal.png" class="legend-marker-icon" alt="正常" />
            <span>正常</span>
          </div>
          <div class="legend-item">
            <img src="/img/sy/auto_unnormal.png" class="legend-marker-icon" alt="预警" />
            <span>预警/告警</span>
          </div>
        </div>
        <div class="legend-group">
          <div class="legend-group-title">告警级别</div>
          <div class="legend-item">
            <div class="legend-dot" style="background: #f5222d;"></div>
            <span>严重告警</span>
          </div>
          <div class="legend-item">
            <div class="legend-dot" style="background: #faad14;"></div>
            <span>重要告警</span>
          </div>
          <div class="legend-item">
            <div class="legend-dot" style="background: #722ed1;"></div>
            <span>一般告警</span>
          </div>
          <div class="legend-item">
            <div class="legend-dot" style="background: #1890ff;"></div>
            <span>提示告警</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 底图切换器 -->
    <div class="basemap-switcher">
      <div class="basemap-current" @click="showBasemapOptions = !showBasemapOptions">
        <img :src="currentThumb" class="basemap-thumb" />
        <span class="basemap-label">{{ currentName }}</span>
      </div>
      <div v-show="showBasemapOptions" class="basemap-options">
        <div
          v-for="bm in basemapList"
          :key="bm.id"
          class="basemap-option"
          :class="{ active: currentLayer === bm.id }"
          @click="switchBasemap(bm.id)"
        >
          <img :src="bm.thumb" class="basemap-option-thumb" />
          <span class="basemap-option-label">{{ bm.name }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { ArrowDown, ArrowUp } from '@element-plus/icons-vue'
import basemapNormal from '@/assets/img/basemap-normal.png'
import basemapSatellite from '@/assets/img/basemap-satellite.png'
import basemapTerrain from '@/assets/img/basemap-terrain.png'

const props = defineProps<{
  currentLayer: string
}>()

const emit = defineEmits<{
  (e: 'update:currentLayer', layerId: string): void
}>()

const legendExpanded = ref(true)
const showBasemapOptions = ref(false)

const basemapList = [
  { id: 'image', name: '影像', thumb: basemapSatellite },
  { id: 'vector', name: '矢量', thumb: basemapNormal },
  { id: 'terrain', name: '地形', thumb: basemapTerrain }
]

const currentThumb = computed(() => {
  return basemapList.find(b => b.id === props.currentLayer)?.thumb || basemapSatellite
})

const currentName = computed(() => {
  return basemapList.find(b => b.id === props.currentLayer)?.name || '影像'
})

const switchBasemap = (id: string) => {
  emit('update:currentLayer', id)
  showBasemapOptions.value = false
}
</script>

<style scoped>
.map-auxiliary-bar {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 999;
  pointer-events: none;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  padding: 0 12px 12px;
}

/* 图例面板 */
.legend-panel {
  pointer-events: auto;
  position: absolute;
  right: 12px;
  bottom: 100px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(12px);
  border-radius: 8px;
  border: 1px solid rgba(0, 0, 0, 0.06);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  min-width: 140px;
}

.legend-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  cursor: pointer;
  user-select: none;
}

.legend-title {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}

.legend-body {
  padding: 4px 12px 10px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
}

.legend-group {
  margin-bottom: 8px;
}

.legend-group:last-child {
  margin-bottom: 0;
}

.legend-group-title {
  font-size: 11px;
  font-weight: 500;
  color: #909399;
  margin-bottom: 4px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 3px 0;
  font-size: 12px;
  color: #303133;
}

.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}

.legend-marker-icon {
  width: 14px;
  height: 18px;
  flex-shrink: 0;
  object-fit: contain;
}

/* 底图切换器 */
.basemap-switcher {
  pointer-events: auto;
  position: absolute;
  right: 12px;
  bottom: 12px;
}

.basemap-current {
  display: flex;
  flex-direction: column;
  align-items: center;
  cursor: pointer;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(12px);
  border-radius: 8px;
  border: 1px solid rgba(0, 0, 0, 0.06);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  transition: box-shadow 0.2s;
}

.basemap-current:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
}

.basemap-thumb {
  width: 60px;
  height: 60px;
  object-fit: cover;
  display: block;
}

.basemap-label {
  font-size: 11px;
  color: #303133;
  padding: 3px 0;
  font-weight: 500;
}

.basemap-options {
  position: absolute;
  bottom: 0;
  right: calc(100% + 6px);
  display: flex;
  gap: 6px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(12px);
  border-radius: 8px;
  border: 1px solid rgba(0, 0, 0, 0.06);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  padding: 8px;
}

.basemap-option {
  display: flex;
  flex-direction: column;
  align-items: center;
  cursor: pointer;
  border-radius: 6px;
  overflow: hidden;
  border: 2px solid transparent;
  transition: border-color 0.2s;
}

.basemap-option:hover {
  border-color: #1890ff;
}

.basemap-option.active {
  border-color: #1890ff;
}

.basemap-option-thumb {
  width: 60px;
  height: 60px;
  object-fit: cover;
  display: block;
}

.basemap-option-label {
  font-size: 11px;
  color: #303133;
  padding: 2px 0;
  white-space: nowrap;
}
</style>
