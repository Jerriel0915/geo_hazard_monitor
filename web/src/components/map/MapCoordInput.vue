<template>
  <div class="map-coord-input">
    <template v-if="mode === 'single'">
      <div class="coord-input-row">
        <el-input
          v-model="text"
          :placeholder="placeholder || '输入坐标 (lat,lng)'"
          size="small"
          style="flex:1"
          @keyup.enter="handleSubmit"
          @blur="handleSubmit"
        />
        <el-button size="small" type="primary" :disabled="!text.trim()" @click="handleSubmit">
          使用
        </el-button>
      </div>
      <p class="input-hint">支持 "lat,lng" 或 "lng,lat" 智能识别</p>
    </template>

    <template v-else>
      <p class="textarea-label">每行一个 "lat,lng"，支持表头自动跳过：</p>
      <el-input
        v-model="text"
        type="textarea"
        :rows="6"
        :placeholder="placeholder || '30.6712,104.0631\n30.6720,104.0640\n...'"
      />
      <p v-if="previewText" :class="['parse-preview', parseOk ? '' : 'parse-preview--err']">
        {{ previewText }}
      </p>
      <div class="coord-actions">
        <el-button size="small" type="primary" @click="handleParse">解析预览</el-button>
        <el-button size="small" @click="$emit('replace')">替换现有</el-button>
        <el-button size="small" @click="$emit('append')">追加到现有</el-button>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import type { LatLng } from '@/lib/boundaryCoords'
import { parseSingle, parseMultiline } from '@/lib/coordParser'

const props = defineProps<{
  mode: 'single' | 'multiline'
  placeholder?: string
}>()

const emit = defineEmits<{
  'parse-success': [value: LatLng | LatLng[]]
  'parse-error': [reason: string, lineNumber?: number]
  'replace': []
  'append': []
}>()

const text = ref('')
const lastResult = ref<LatLng | LatLng[] | null>(null)
const parseError = ref('')

const previewText = computed(() => {
  if (props.mode !== 'multiline') return ''
  const t = text.value.trim()
  if (!t) return ''
  const result = parseMultiline(t)
  if (result.errors.length) return `警告：第 ${result.errors.map(e => e.line).join(',')} 行无法解析`
  return `解析预览：${result.coords.length} 个顶点 ✓`
})

const parseOk = computed(() => !parseError.value)

function handleSubmit() {
  const result = parseSingle(text.value)
  if (result) {
    parseError.value = ''
    lastResult.value = result
    emit('parse-success', result)
  } else {
    parseError.value = '无法解析（格式：lat,lng）'
    emit('parse-error', parseError.value)
  }
}

function handleParse() {
  const result = parseMultiline(text.value)
  if (result.coords.length > 0) {
    parseError.value = ''
    lastResult.value = result.coords
    emit('parse-success', result.coords)
  } else {
    parseError.value = '未能从输入中提取有效坐标'
    emit('parse-error', parseError.value)
  }
}
</script>

<style scoped>
.map-coord-input { padding: 8px 0; }
.coord-input-row { display: flex; gap: 8px; align-items: center; }
.input-hint { margin: 4px 0 0; font-size: 11px; color: #909399; }
.textarea-label { font-size: 12px; color: #606266; margin: 0 0 6px; }
.parse-preview { font-size: 12px; margin: 6px 0; color: #67c23a; }
.parse-preview--err { color: #f56c6c; }
.coord-actions { display: flex; gap: 8px; margin-top: 8px; }
</style>
