<template>
  <el-popover
      trigger="click"
      placement="bottom-start"
      :width="range ? 360 : 260"
      :hide-after="0"
  >
    <template #reference>
      <div class="dt-picker-input" :class="{ 'is-range': range }">
        <span class="dt-picker-text" :class="{ 'is-placeholder': !displayText }">
          {{ displayText || placeholder }}
        </span>
        <el-icon class="dt-picker-icon"><Clock /></el-icon>
      </div>
    </template>

    <div class="dt-panel">
      <!-- 模式切换 -->
      <el-radio-group
          :model-value="mode"
          size="small"
          @update:model-value="onModeChange"
      >
        <el-radio-button value="ABSOLUTE">绝对</el-radio-button>
        <el-radio-button value="RELATIVE">相对</el-radio-button>
      </el-radio-group>

      <!-- 绝对模式 -->
      <template v-if="mode === 'ABSOLUTE'">
        <el-date-picker
            v-if="!range"
            :model-value="(condition.threshold as string) || undefined"
            type="datetime"
            size="small"
            value-format="YYYY-MM-DDTHH:mm:ss"
            placeholder="选择时间"
            style="width: 100%"
            @update:model-value="onAbsoluteSingleChange"
        />
        <el-date-picker
            v-else
            :model-value="absoluteRange"
            type="datetimerange"
            size="small"
            value-format="YYYY-MM-DDTHH:mm:ss"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            style="width: 100%"
            @update:model-value="onAbsoluteRangeChange"
        />
      </template>

      <!-- 相对模式 -->
      <div v-else class="rel-section">
        <div v-if="range" class="rel-label">起点</div>
        <div class="rel-row">
          <span class="rel-now">当前时间</span>
          <el-select
              :model-value="condition.relDirection || '-'"
              size="small"
              style="width: 56px"
              @update:model-value="(v: string) => updateRelField('relDirection', v)"
          >
            <el-option label="-" value="-" />
            <el-option label="+" value="+" />
          </el-select>
          <el-input-number
              :model-value="condition.relValue || 0"
              size="small"
              :min="0"
              :step="1"
              controls-position="right"
              style="width: 100px"
              @update:model-value="(v: number | null) => updateRelField('relValue', v ?? 0)"
          />
          <el-select
              :model-value="condition.relUnit || 'h'"
              size="small"
              style="width: 64px"
              @update:model-value="(v: string) => updateRelField('relUnit', v)"
          >
            <el-option label="秒" value="s" />
            <el-option label="分" value="m" />
            <el-option label="时" value="h" />
            <el-option label="天" value="d" />
          </el-select>
        </div>

        <template v-if="range">
          <span class="rel-tilde">~</span>
          <div class="rel-label">终点</div>
          <div class="rel-row">
            <span class="rel-now">当前时间</span>
            <el-select
                :model-value="condition.relDirectionMax || '-'"
                size="small"
                style="width: 56px"
                @update:model-value="(v: string) => updateRelField('relDirectionMax', v)"
            >
              <el-option label="-" value="-" />
              <el-option label="+" value="+" />
            </el-select>
            <el-input-number
                :model-value="condition.relValueMax || 0"
                size="small"
                :min="0"
                :step="1"
                controls-position="right"
                style="width: 100px"
                @update:model-value="(v: number | null) => updateRelField('relValueMax', v ?? 0)"
            />
            <el-select
                :model-value="condition.relUnitMax || 'h'"
                size="small"
                style="width: 64px"
                @update:model-value="(v: string) => updateRelField('relUnitMax', v)"
            >
              <el-option label="秒" value="s" />
              <el-option label="分" value="m" />
              <el-option label="时" value="h" />
              <el-option label="天" value="d" />
            </el-select>
          </div>
        </template>
      </div>
    </div>
  </el-popover>
</template>

<script setup lang="ts">
import {computed} from 'vue'
import {Clock} from '@element-plus/icons-vue'
import type {Condition} from '../composables/useIndicatorTree'

const props = defineProps<{
  condition: Condition
  range: boolean
}>()

const emit = defineEmits<{
  'update:condition': [c: Condition]
}>()

const mode = computed(() => props.condition.thresholdMode || 'ABSOLUTE')

const placeholder = computed(() => props.range ? '选择时间范围' : '选择时间')

const UNIT_TEXT: Record<string, string> = {s: '秒', m: '分', h: '时', d: '天'}

function formatRelative(dir?: string, val?: number, unit?: string): string {
  if (!val || val === 0) return '当前时间'
  return `当前时间 ${dir || '-'} ${val} ${UNIT_TEXT[unit || 'h'] || '时'}`
}

function formatCompact(dt: string): string {
  if (!dt) return ''
  const d = new Date(dt)
  if (isNaN(d.getTime())) return dt
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  const hh = String(d.getHours()).padStart(2, '0')
  const mi = String(d.getMinutes()).padStart(2, '0')
  return `${mm}-${dd} ${hh}:${mi}`
}

const displayText = computed(() => {
  if (mode.value === 'ABSOLUTE') {
    if (props.range) {
      const s = props.condition.threshold as string
      const e = props.condition.thresholdMax as string
      return s && e ? `${formatCompact(s)} ~ ${formatCompact(e)}` : ''
    }
    const t = props.condition.threshold as string
    return t ? formatCompact(t) : ''
  }
  // RELATIVE
  if (props.range) {
    const s = formatRelative(props.condition.relDirection, props.condition.relValue, props.condition.relUnit)
    const e = formatRelative(props.condition.relDirectionMax, props.condition.relValueMax, props.condition.relUnitMax)
    return `${s} ~ ${e}`
  }
  return formatRelative(props.condition.relDirection, props.condition.relValue, props.condition.relUnit)
})

const absoluteRange = computed<[string, string] | null>(() => {
  const a = props.condition.threshold as string
  const b = props.condition.thresholdMax as string
  return a && b ? [a, b] : null
})

function serializeRelative(dir: string, val: number, unit: string): string {
  return val > 0 ? `now${dir}${val}${unit}` : 'now'
}

function onModeChange(m: string) {
  const updated: Condition = {...props.condition, thresholdMode: m as 'ABSOLUTE' | 'RELATIVE'}
  if (m === 'ABSOLUTE') {
    if (typeof updated.threshold === 'string' && updated.threshold.startsWith('now')) {
      updated.threshold = ''
    }
    if (typeof updated.thresholdMax === 'string' && updated.thresholdMax.startsWith('now')) {
      updated.thresholdMax = ''
    }
  } else {
    if (updated.relDirection === undefined) updated.relDirection = '-'
    if (updated.relValue === undefined) updated.relValue = 0
    if (updated.relUnit === undefined) updated.relUnit = 'h'
    updated.threshold = serializeRelative(updated.relDirection, updated.relValue, updated.relUnit)
    if (props.range) {
      if (updated.relDirectionMax === undefined) updated.relDirectionMax = '-'
      if (updated.relValueMax === undefined) updated.relValueMax = 0
      if (updated.relUnitMax === undefined) updated.relUnitMax = 'h'
      updated.thresholdMax = serializeRelative(updated.relDirectionMax, updated.relValueMax, updated.relUnitMax)
    }
  }
  emit('update:condition', updated)
}

function updateRelField(field: string, value: any) {
  const updated: Condition = {...props.condition, [field]: value}
  if (field === 'relDirection' || field === 'relValue' || field === 'relUnit') {
    const dir = updated.relDirection || '-'
    const n = updated.relValue || 0
    const unit = updated.relUnit || 'h'
    updated.threshold = serializeRelative(dir, n, unit)
  }
  if (field === 'relDirectionMax' || field === 'relValueMax' || field === 'relUnitMax') {
    const dir = updated.relDirectionMax || '-'
    const n = updated.relValueMax || 0
    const unit = updated.relUnitMax || 'h'
    updated.thresholdMax = serializeRelative(dir, n, unit)
  }
  emit('update:condition', updated)
}

function onAbsoluteSingleChange(v: string | null) {
  emit('update:condition', {...props.condition, threshold: v ?? ''})
}

function onAbsoluteRangeChange(v: [string, string] | null) {
  emit('update:condition', {
    ...props.condition,
    threshold: v?.[0] ?? '',
    thresholdMax: v?.[1] ?? '',
  })
}
</script>

<style scoped>
.dt-picker-input {
  display: inline-flex;
  align-items: center;
  justify-content: space-between;
  gap: 4px;
  height: 24px;
  padding: 0 8px;
  border: 1px solid var(--el-border-color, #dcdfe6);
  border-radius: 4px;
  cursor: pointer;
  background: var(--el-fill-color-blank, #fff);
  transition: border-color 0.2s;
  box-sizing: border-box;
}
.dt-picker-input:hover {
  border-color: var(--el-color-primary, #409eff);
}
.dt-picker-input.is-range {
  width: 360px;
}
.dt-picker-input:not(.is-range) {
  width: 180px;
}
.dt-picker-text {
  font-size: 12px;
  color: var(--el-text-color-primary, #303133);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.dt-picker-text.is-placeholder {
  color: var(--el-text-color-placeholder, #a8abb2);
}
.dt-picker-icon {
  color: var(--el-text-color-placeholder, #a8abb2);
  font-size: 14px;
  flex-shrink: 0;
}

.dt-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 4px 0;
}
.rel-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.rel-row {
  display: flex;
  align-items: center;
  gap: 6px;
}
.rel-now {
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
}
.rel-label {
  font-size: 12px;
  color: #606266;
  font-weight: 500;
}
.rel-tilde {
  text-align: center;
  font-size: 13px;
  color: #606266;
  margin: -4px 0;
}
</style>
