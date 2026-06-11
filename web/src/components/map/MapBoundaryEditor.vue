<template>
  <div class="map-boundary-editor">
    <div v-if="!readonly" class="editor-toolbar">
      <el-button
        :type="editor.mode.value === 'edit' ? 'success' : 'default'"
        size="small"
        @click="editor.toggleEdit"
      >{{ editor.mode.value === 'edit' ? '● 编辑中' : '○ 编辑' }}</el-button>

      <el-button-group v-if="editor.mode.value === 'edit'">
        <el-button size="small"
          :type="editor.tool.value === 'polygon' ? 'primary' : 'default'"
          @click="onToolClick('polygon')">▢ 多边形</el-button>
        <el-button size="small"
          :type="editor.tool.value === 'strike' ? 'primary' : 'default'"
          @click="onToolClick('strike')">↗ 走向</el-button>
        <el-button size="small"
          :type="editor.tool.value === 'aux' ? 'primary' : 'default'"
          @click="onToolClick('aux')">⤴ 辅助线</el-button>
      </el-button-group>

      <el-button v-if="editor.mode.value === 'edit'"
        size="small" type="danger" plain
        :disabled="!editor.selectedId.value"
        @click="editor.removeSelected">× 删除选中</el-button>
      <el-button v-if="editor.mode.value === 'edit' && editor.manualCenterLocked.value"
        size="small" @click="editor.resetCenter">⌖ 重置中心</el-button>
      <el-button v-if="editor.mode.value === 'edit'"
        size="small" type="danger" plain @click="onClearAll">清空</el-button>
    </div>

    <div ref="containerRef" :style="{ height: heightStyle }" />

    <div v-if="hintText" class="editor-hint">{{ hintText }}</div>

    <el-drawer v-model="importOpen" title="批量导入 polygon 顶点" direction="rtl" size="400px">
      <MapCoordInput
        mode="multiline"
        @parse-success="onImportParsed"
        @replace="onImportReplace"
        @append="onImportAppend"
      />
    </el-drawer>

    <div class="editor-footer">
      <el-button @click="emitCancel" :disabled="readonly">取消</el-button>
      <el-button type="primary" @click="emitDone" :disabled="!editor.canSave.value">完成</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import { useMapEditor } from '@/composables/useMapEditor'
import type { BoundaryCoords, LatLng } from '@/lib/boundaryCoords'
import MapCoordInput from './MapCoordInput.vue'

const props = withDefaults(defineProps<{
  initialValue?: BoundaryCoords | null
  initialCenter?: LatLng | null
  readonly?: boolean
  defaultCenter?: LatLng
  defaultZoom?: number
  height?: string | number
}>(), {
  readonly: false,
  defaultZoom: 14,
  height: 500
})

const emit = defineEmits<{
  done: [value: BoundaryCoords, center: LatLng | null]
  cancel: []
}>()

const heightStyle = computed(() => typeof props.height === 'number' ? `${props.height}px` : props.height)
const containerRef = ref<HTMLElement | null>(null)
const importOpen = ref(false)
const lastImported = ref<LatLng[]>([])

function onImportParsed(value: LatLng | LatLng[]) {
  lastImported.value = Array.isArray(value) ? value : [value]
}

const editor = useMapEditor({
  container: containerRef,
  variant: 'boundary',
  initialBoundary: props.initialValue ?? null,
  initialCenter: props.initialCenter ?? null,
  defaultCenter: props.defaultCenter,
  defaultZoom: props.defaultZoom,
  readonly: props.readonly
})

// 重新打开 dialog 时, 父组件会传新的 initialValue, 这里重新同步到内部 state
watch(() => props.initialValue, (v) => {
  if (v) {
    editor.polygon.value = v.polygon?.slice() ?? []
    editor.strikeLine.value = v.strikeLine ? [...v.strikeLine] : null
    editor.auxiliaryLines.value = v.auxiliaryLines?.map(l => l.slice()) ?? []
    editor.manualCenterLocked.value = false
  }
}, { deep: true })
watch(() => props.initialCenter, (v) => {
  if (v) {
    editor.center.value = { ...v }
    editor.manualCenterLocked.value = true
  }
})

const hintText = computed(() => {
  if (props.readonly) return ''
  if (editor.tool.value === 'polygon') return '点击地图添加顶点 · 双击或回车闭合 · Esc 取消'
  if (editor.tool.value === 'strike') {
    // After 1st click, strikeLine exists (degenerate or not) — prompt for endpoint
    return '点击设置走向终点 (起点已固定)'
  }
  if (editor.tool.value === 'aux') {
    const last = editor.auxiliaryLines.value[editor.auxiliaryLines.value.length - 1]
    if (!last || last.length < 1) return '点击添加第一个顶点'
    return '点击添加下一个顶点 · 双击或回车结束 · Esc 取消'
  }
  if (editor.mode.value === 'edit') return '拖动顶点/端点/中心修改 · 点选后按 Delete 键删除'
  return '点击「编辑」开始'
})

function onToolClick(t: 'polygon' | 'strike' | 'aux') {
  editor.activateTool(editor.tool.value === t ? null : t)
}

function onClearAll() {
  ElMessageBox.confirm('将清除多边形、走向、辅助线和中心点。确定？', '清空', { type: 'warning' })
    .then(() => editor.clearAll()).catch(() => {})
}

function emitCancel() {
  // Reset internal state to the prop's initial value before closing,
  // so the next open (with the same or different data) starts clean.
  if (props.initialValue) {
    editor.polygon.value = props.initialValue.polygon?.slice() ?? []
    editor.strikeLine.value = props.initialValue.strikeLine ? [...props.initialValue.strikeLine] : null
    editor.auxiliaryLines.value = props.initialValue.auxiliaryLines?.map(l => l.slice()) ?? []
  } else {
    editor.polygon.value = []
    editor.strikeLine.value = null
    editor.auxiliaryLines.value = []
  }
  if (props.initialCenter) {
    editor.center.value = { ...props.initialCenter }
    editor.manualCenterLocked.value = true
  } else {
    editor.center.value = null
    editor.manualCenterLocked.value = false
  }
  editor.selectedId.value = null
  editor.tool.value = null
  editor.exitEdit()
  emit('cancel')
}

function emitDone() {
  if (!editor.canSave.value) {
    ElMessage.warning('请先完成多边形 (至少 3 个顶点)')
    return
  }
  const value: BoundaryCoords = {
    polygon: editor.polygon.value.slice(),
    strikeLine: editor.strikeLine.value ? [...editor.strikeLine.value] : null,
    auxiliaryLines: editor.auxiliaryLines.value.map(l => l.slice())
  }
  emit('done', value, editor.center.value ? { ...editor.center.value } : null)
}

function onImportReplace() {
  if (lastImported.value.length >= 3) {
    editor.polygon.value = lastImported.value.slice()
    importOpen.value = false
  }
}
function onImportAppend() {
  if (lastImported.value.length >= 1) {
    editor.polygon.value = [...editor.polygon.value, ...lastImported.value]
    importOpen.value = false
  }
}

defineExpose({ invalidate: editor.invalidate })
</script>

<style scoped>
.map-boundary-editor { display: flex; flex-direction: column; gap: 8px; }
.editor-toolbar { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
.editor-hint { font-size: 12px; color: #909399; height: 20px; line-height: 20px; }
.editor-footer { display: flex; justify-content: flex-end; gap: 8px; padding-top: 8px; border-top: 1px solid #ebeef5; }

/* P3: pulse animation for strike start marker during DRAW */
@keyframes ghost-pulse {
  0%, 100% { transform: scale(1); opacity: 0.6; }
  50%      { transform: scale(1.4); opacity: 0.2; }
}
</style>
