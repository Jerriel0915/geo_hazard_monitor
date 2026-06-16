<template>
  <div class="map-auxiliary-bar">
    <!-- 比例尺由 Leaflet 自带，样式在 Dashboard.vue 中覆盖 -->

    <!-- 右下角组：图例 + 底图切换 -->
    <div class="bottom-right-group" :style="{ right: props.rightPanelOffset + 'px' }">
      <!-- 图例 (多行，与底图切换等高) -->
      <div v-show="props.legendVisible" class="legend-bar">
        <div class="legend-group">
          <span class="legend-group-label">隐患点</span>
          <div class="legend-group-items">
            <div class="legend-item">
              <img src="/img/sy/auto_normal.png" class="legend-marker-icon" alt="正常" />
              <span>正常</span>
            </div>
            <div class="legend-item">
              <img src="/img/sy/auto_unnormal.png" class="legend-marker-icon" alt="预警" />
              <span>预警</span>
            </div>
          </div>
        </div>
        <div class="legend-divider"></div>
        <div class="legend-group">
          <span class="legend-group-label">设备状态</span>
          <div class="legend-group-items">
            <div class="legend-item">
              <div class="legend-dot" style="background: #52c41a;"></div>
              <span>在线</span>
            </div>
            <div class="legend-item">
              <div class="legend-dot" style="background: #c9cdd4;"></div>
              <span>离线</span>
            </div>
          </div>
        </div>
        <div v-if="monitorTypes.length" class="legend-divider"></div>
        <div v-if="monitorTypes.length" class="legend-group">
          <span class="legend-group-label">监测类型</span>
          <div class="legend-group-items legend-group-items-wrap">
            <div v-for="mt in monitorTypes" :key="mt.id" class="legend-item">
              <img v-if="mt.icon" :src="mt.icon" class="legend-type-icon" :alt="mt.name" />
              <div v-else class="legend-dot" style="background: #1890ff;"></div>
              <span>{{ mt.name }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 底图切换器 -->
      <div class="basemap-switcher" @mouseenter="showBasemapOptions = true" @mouseleave="showBasemapOptions = false">
        <div class="basemap-current">
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
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import type { MonitorTypeItem } from '@/api/monitorType'
import basemapNormal from '@/assets/img/basemap-normal.png'
import basemapSatellite from '@/assets/img/basemap-satellite.png'
import basemapTerrain from '@/assets/img/basemap-terrain.png'

const props = defineProps<{
  currentLayer: string
  monitorTypes: MonitorTypeItem[]
  rightPanelOffset: number
  legendVisible: boolean
}>()

const emit = defineEmits<{
  (e: 'update:currentLayer', layerId: string): void
}>()

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
}

/* 右下角组：图例 + 底图水平排列 */
.bottom-right-group {
  pointer-events: auto;
  position: absolute;
  bottom: 12px;
  display: flex;
  align-items: flex-end;
  gap: 8px;
}

/* 图例条 — 与底图切换器等高 */
.legend-bar {
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(12px);
  border-radius: 8px;
  border: 1px solid rgba(0, 0, 0, 0.06);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  display: flex;
  flex-direction: column;
  gap: 0;
  padding: 8px 12px;
}

.legend-group {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 4px 0;
}

.legend-group-label {
  font-size: 10px;
  color: #909399;
  font-weight: 600;
  letter-spacing: 0.5px;
  white-space: nowrap;
  min-width: 36px;
  padding-top: 2px;
}

.legend-group-items {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 监测类型：3列网格 */
.legend-group-items-wrap {
  display: grid;
  grid-template-columns: repeat(3, auto);
  gap: 4px 12px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: #4e5969;
  white-space: nowrap;
}

.legend-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.legend-marker-icon {
  width: 12px;
  height: 16px;
  flex-shrink: 0;
  object-fit: contain;
}

.legend-type-icon {
  width: 14px;
  height: 14px;
  flex-shrink: 0;
  object-fit: contain;
}

.legend-divider {
  height: 1px;
  background: rgba(0, 0, 0, 0.06);
  margin: 2px 0;
}

/* 底图切换器 */
.basemap-switcher {
  position: relative;
  z-index: 10;
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
  right: calc(100% - 1px);
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
