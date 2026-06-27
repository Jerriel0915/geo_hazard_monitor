<template>
  <div class="layout-config-overlay" v-if="visible" @click="close">
    <div class="layout-config-dialog" @click.stop>
      <div class="layout-config-header">
        <span class="layout-config-title">面板布局配置</span>
        <button class="layout-config-close" @click="close">
          <el-icon :size="16"><Close/></el-icon>
        </button>
      </div>
      <div class="layout-config-body">
        <p class="layout-config-desc">拖拽调整各模块在左右两侧面板的显示位置</p>
        <div class="layout-columns">
          <div class="layout-column">
            <div class="column-header left-header">左侧面板</div>
            <div class="column-drop-zone" @dragover.prevent @drop="onDrop($event, 'left')">
              <div
                  v-for="widget in leftWidgets"
                  :key="widget.key"
                  class="widget-chip"
                  draggable="true"
                  @dragstart="onDragStart($event, widget.key)"
              >
                <span class="widget-drag-handle">⠿</span>
                <span>{{ widget.label }}</span>
                <button class="widget-remove" @click="moveWidget(widget.key, null)" title="隐藏">
                  <el-icon :size="12"><Close/></el-icon>
                </button>
              </div>
              <div v-if="leftWidgets.length === 0" class="drop-hint">拖拽模块到此处</div>
            </div>
          </div>
          <div class="layout-column">
            <div class="column-header right-header">右侧面板</div>
            <div class="column-drop-zone" @dragover.prevent @drop="onDrop($event, 'right')">
              <div
                  v-for="widget in rightWidgets"
                  :key="widget.key"
                  class="widget-chip"
                  draggable="true"
                  @dragstart="onDragStart($event, widget.key)"
              >
                <span class="widget-drag-handle">⠿</span>
                <span>{{ widget.label }}</span>
                <button class="widget-remove" @click="moveWidget(widget.key, null)" title="隐藏">
                  <el-icon :size="12"><Close/></el-icon>
                </button>
              </div>
              <div v-if="rightWidgets.length === 0" class="drop-hint">拖拽模块到此处</div>
            </div>
          </div>
        </div>
        <div class="hidden-widgets" v-if="hiddenWidgets.length > 0">
          <div class="hidden-header">已隐藏的模块（点击恢复）</div>
          <div class="hidden-list">
            <span
                v-for="widget in hiddenWidgets"
                :key="widget.key"
                class="hidden-chip"
                @click="restoreWidget(widget.key)"
            >
              {{ widget.label }}
              <el-icon class="restore-icon"><RefreshLeft/></el-icon>
            </span>
          </div>
        </div>
      </div>
      <div class="layout-config-footer">
        <button class="reset-btn" @click="resetLayoutConfig">恢复默认</button>
        <button class="confirm-btn" @click="close">完成</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {computed, ref, watch} from 'vue'
import {Close, RefreshLeft} from '@element-plus/icons-vue'

export interface WidgetDef {
  key: string
  label: string
}

export interface LayoutConfig {
  left: string[]
  right: string[]
  hidden: string[]
}

const ALL_WIDGETS: WidgetDef[] = [
  {key: 'systemHealth', label: '系统健康度'},
  {key: 'assetInfo', label: '资产情况'},
  {key: 'alarmStatus', label: '告警态势'},
  {key: 'deviceStatus', label: '设备在线状态'}
]

const DEFAULT_LAYOUT: LayoutConfig = {
  left: ['systemHealth', 'assetInfo'],
  right: ['alarmStatus'],
  hidden: []
}

const WIDGET_DEFAULT_SIDE: Record<string, 'left' | 'right'> = {
  systemHealth: 'left',
  assetInfo: 'left',
  alarmStatus: 'right',
  deviceStatus: 'right'
}

const props = defineProps<{
  visible: boolean
  layout: LayoutConfig
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'update:layout', layout: LayoutConfig): void
}>()

const dragKey = ref<string | null>(null)

// Internal working copy
const layoutConfig = ref<LayoutConfig>({...DEFAULT_LAYOUT})

// Sync from parent prop
watch(() => props.layout, (val) => {
  layoutConfig.value = {...val}
}, {immediate: true, deep: true})

const leftWidgets = computed(() =>
    layoutConfig.value.left.map(k => ALL_WIDGETS.find(w => w.key === k)!).filter(Boolean)
)
const rightWidgets = computed(() =>
    layoutConfig.value.right.map(k => ALL_WIDGETS.find(w => w.key === k)!).filter(Boolean)
)
const hiddenWidgets = computed(() =>
    layoutConfig.value.hidden.map(k => ALL_WIDGETS.find(w => w.key === k)!).filter(Boolean)
)

const saveLayoutConfig = () => {
  localStorage.setItem('dashboard_layout', JSON.stringify(layoutConfig.value))
}

const loadLayoutConfig = () => {
  try {
    const saved = localStorage.getItem('dashboard_layout')
    if (saved) {
      const parsed = JSON.parse(saved)
      if (parsed.left && parsed.right && parsed.hidden) {
        const known = new Set([...parsed.left, ...parsed.right, ...parsed.hidden])
        for (const w of ALL_WIDGETS) {
          if (!known.has(w.key)) parsed.hidden.push(w.key)
        }
        layoutConfig.value = parsed
        emit('update:layout', {...parsed})
      }
    }
  } catch { /* keep defaults */
  }
}

const onDragStart = (e: DragEvent, key: string) => {
  dragKey.value = key
  e.dataTransfer!.effectAllowed = 'move'
}

const onDrop = (e: DragEvent, side: 'left' | 'right') => {
  const key = dragKey.value
  if (!key) return
  moveWidget(key, side)
  dragKey.value = null
}

const moveWidget = (key: string, side: 'left' | 'right' | null) => {
  layoutConfig.value.left = layoutConfig.value.left.filter(k => k !== key)
  layoutConfig.value.right = layoutConfig.value.right.filter(k => k !== key)
  layoutConfig.value.hidden = layoutConfig.value.hidden.filter(k => k !== key)
  if (side === 'left') layoutConfig.value.left.push(key)
  else if (side === 'right') layoutConfig.value.right.push(key)
  else layoutConfig.value.hidden.push(key)
  saveLayoutConfig()
  emit('update:layout', {...layoutConfig.value})
}

const restoreWidget = (key: string) => {
  const defaultSide = WIDGET_DEFAULT_SIDE[key] || 'right'
  moveWidget(key, defaultSide)
}

const resetLayoutConfig = () => {
  layoutConfig.value = {...DEFAULT_LAYOUT, hidden: [...DEFAULT_LAYOUT.hidden]}
  saveLayoutConfig()
  emit('update:layout', {...layoutConfig.value})
}

const close = () => {
  emit('update:visible', false)
}

// Load from localStorage on mount
loadLayoutConfig()

defineExpose({loadLayoutConfig})
</script>

<style scoped>
.layout-config-overlay {
  position: fixed;
  inset: 0;
  z-index: 2000;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
}

.layout-config-dialog {
  width: 580px;
  max-height: calc(100vh - 80px);
  background: #ffffff;
  border-radius: 10px;
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.15);
  overflow-y: auto;
}

.layout-config-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
}

.layout-config-title {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
}

.layout-config-close {
  width: 28px;
  height: 28px;
  border: none;
  background: none;
  font-size: 16px;
  color: #999;
  cursor: pointer;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.layout-config-close:hover {
  background: #f5f5f5;
  color: #333;
}

.layout-config-body {
  padding: 20px;
}

.layout-config-desc {
  margin: 0 0 16px;
  font-size: 13px;
  color: #999;
}

.layout-columns {
  display: flex;
  gap: 16px;
}

.layout-column {
  flex: 1;
}

.column-header {
  padding: 8px 12px;
  font-size: 13px;
  font-weight: 600;
  border-radius: 6px 6px 0 0;
  margin-bottom: 0;
}

.column-header.left-header {
  background: #e6f7ff;
  color: #1890ff;
}

.column-header.right-header {
  background: #fff7e6;
  color: #fa8c16;
}

.column-drop-zone {
  min-height: 120px;
  border: 2px dashed #e8e8e8;
  border-radius: 0 0 6px 6px;
  padding: 10px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  transition: border-color 0.2s;
}

.column-drop-zone:hover {
  border-color: #1890ff;
}

.drop-hint {
  color: #ccc;
  font-size: 12px;
  text-align: center;
  padding: 20px 0;
}

.widget-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  background: #fafafa;
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  font-size: 14px;
  color: #334155;
  cursor: grab;
  transition: all 0.15s;
}

.widget-chip:hover {
  background: #f0f5ff;
  border-color: #91caff;
}

.widget-chip:active {
  cursor: grabbing;
}

.widget-drag-handle {
  color: #bbb;
  font-size: 14px;
  line-height: 1;
}

.widget-remove {
  margin-left: auto;
  width: 20px;
  height: 20px;
  border: none;
  background: none;
  color: #ccc;
  cursor: pointer;
  border-radius: 4px;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.widget-remove:hover {
  background: #fff1f0;
  color: #ff4d4f;
}

.hidden-widgets {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.hidden-header {
  font-size: 13px;
  color: #999;
  margin-bottom: 8px;
}

.hidden-list {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.hidden-chip {
  padding: 6px 12px;
  background: #f5f5f5;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  font-size: 13px;
  color: #999;
  cursor: pointer;
  transition: all 0.15s;
}

.hidden-chip:hover {
  color: #1890ff;
  border-color: #91caff;
  background: #e6f7ff;
}

.restore-icon {
  margin-left: 4px;
  font-size: 12px;
}

.layout-config-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 14px 20px;
  border-top: 1px solid #f0f0f0;
  background: #fafafa;
}

.layout-config-footer .reset-btn {
  padding: 8px 16px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  background: #ffffff;
  color: #595959;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.layout-config-footer .reset-btn:hover {
  color: #1890ff;
  border-color: #1890ff;
}

.layout-config-footer .confirm-btn {
  padding: 8px 20px;
  border: none;
  border-radius: 6px;
  background: #1890ff;
  color: #ffffff;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.layout-config-footer .confirm-btn:hover {
  background: #40a9ff;
}
</style>
