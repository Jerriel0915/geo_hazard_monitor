<template>
  <div class="condition-row">
    <el-tree-select
        :model-value="condition.subject"
        :data="indicatorTree"
        placeholder="选择指标"
        filterable
        check-strictly
        size="small"
        class="cond-field subject-field"
        :style="{width: subjectWidth}"
        :render-after-expand="false"
        node-key="value"
        :props="{ children: 'children', label: 'displayLabel', value: 'value', disabled: 'disabled' }"
        @update:model-value="onSubjectChange"
    >
      <template #default="{ data }">{{ data.label }}</template>
    </el-tree-select>

    <el-select
        :model-value="condition.operator"
        size="small"
        class="cond-field operator-field"
        @update:model-value="(v: string) => updateField('operator', v)"
    >
      <el-option v-for="op in currentOperators" :key="op.value" :label="op.label" :value="op.value"/>
    </el-select>

    <!-- NUMBER 单值 -->
    <el-input-number
        v-if="valueType === 'NUMBER' && condition.operator !== 'BETWEEN'"
        :model-value="Number(condition.threshold) || 0"
        size="small"
        :precision="2"
        :step="0.1"
        controls-position="right"
        class="cond-field threshold-field"
        @update:model-value="(v: number | null) => updateField('threshold', v ?? 0)"
    />
    <!-- NUMBER BETWEEN: 两个 input-number -->
    <template v-else-if="valueType === 'NUMBER' && condition.operator === 'BETWEEN'">
      <el-input-number
          :model-value="Number(condition.threshold) || 0"
          size="small"
          :precision="2"
          :step="0.1"
          controls-position="right"
          class="cond-field threshold-field"
          @update:model-value="(v: number | null) => updateField('threshold', v ?? 0)"
      />
      <span class="cond-tilde">~</span>
      <el-input-number
          :model-value="Number(condition.thresholdMax) || 0"
          size="small"
          :precision="2"
          :step="0.1"
          controls-position="right"
          class="cond-field threshold-field"
          @update:model-value="(v: number | null) => updateField('thresholdMax', v ?? 0)"
      />
    </template>

    <!-- DATETIME -->
    <DateTimeConditionPicker
        v-else-if="valueType === 'DATETIME'"
        :condition="condition"
        :range="condition.operator === 'BETWEEN'"
        @update:condition="(c: Condition) => emit('update:condition', c)"
    />

    <!-- STRING -->
    <el-input
        v-else-if="valueType === 'STRING'"
        :model-value="String(condition.threshold || '')"
        size="small"
        class="cond-field threshold-field"
        placeholder="输入字符串"
        @update:model-value="(v: string) => updateField('threshold', v)"
    />

    <!-- BOOLEAN -->
    <el-select
        v-else-if="valueType === 'BOOLEAN'"
        :model-value="Number(condition.threshold) || 0"
        size="small"
        class="cond-field threshold-field"
        @update:model-value="(v: number) => updateField('threshold', v)"
    >
      <el-option :label="booleanTrueLabel" :value="1"/>
      <el-option :label="booleanFalseLabel" :value="0"/>
    </el-select>

    <!-- 兜底: NUMBER 单值（valueType 为空时） -->
    <el-input-number
        v-else
        :model-value="Number(condition.threshold) || 0"
        size="small"
        :precision="2"
        :step="0.1"
        controls-position="right"
        class="cond-field threshold-field"
        @update:model-value="(v: number | null) => updateField('threshold', v ?? 0)"
    />

    <span v-if="condition.unit && valueType === 'NUMBER'" class="cond-unit">{{ condition.unit }}</span>
    <el-button size="small" type="danger" text @click="$emit('remove')">
      <el-icon><Delete/></el-icon>
    </el-button>
  </div>
</template>

<script setup lang="ts">
import {computed} from 'vue'
import {Delete} from '@element-plus/icons-vue'
import DateTimeConditionPicker from './DateTimeConditionPicker.vue'
import type {IndicatorTreeNode, Condition} from '../composables/useIndicatorTree'
import type {ValueType} from '@/utils/indicatorType'

const props = defineProps<{
  condition: Condition
  indicatorTree: IndicatorTreeNode[]
  nodeMap: Map<string, IndicatorTreeNode>
}>()

const emit = defineEmits<{
  'update:condition': [c: Condition]
  remove: []
}>()

interface Op { value: string; label: string }

const OPERATOR_SETS: Record<ValueType, Op[]> = {
  NUMBER: [
    {value: 'GT', label: '>'},
    {value: 'GTE', label: '>='},
    {value: 'LT', label: '<'},
    {value: 'LTE', label: '<='},
    {value: 'EQ', label: '=='},
    {value: 'NEQ', label: '!='},
    {value: 'BETWEEN', label: '介于'},
  ],
  DATETIME: [
    {value: 'GT', label: '晚于'},
    {value: 'LT', label: '早于'},
    {value: 'BETWEEN', label: '介于时段'},
  ],
  STRING: [
    {value: 'CONTAINS', label: '包含'},
    {value: 'EQ', label: '等于'},
    {value: 'NEQ', label: '不等于'},
    {value: 'GT', label: '大于(ASCII)'},
    {value: 'LT', label: '小于(ASCII)'},
  ],
  BOOLEAN: [
    {value: 'EQ', label: '等于'},
    {value: 'NEQ', label: '不等于'},
  ],
}

const currentNode = computed(() => props.nodeMap.get(props.condition.subject))
const valueType = computed<ValueType>(() => currentNode.value?.meta?.valueType || 'NUMBER')

const currentOperators = computed(() => OPERATOR_SETS[valueType.value] || OPERATOR_SETS.NUMBER)

const subjectWidth = computed(() => {
  const label = currentNode.value?.displayLabel || ''
  return `${Math.max(200, Math.min(500, label.length * 14 + 40))}px`
})

const booleanTrueLabel = computed(() => {
  const s = props.condition.subject || ''
  return s.endsWith('onlineStatus') ? '在线' : '是'
})
const booleanFalseLabel = computed(() => {
  const s = props.condition.subject || ''
  return s.endsWith('onlineStatus') ? '离线' : '否'
})

function onSubjectChange(val: string) {
  const node = props.nodeMap.get(val)
  const vt = (node?.meta?.valueType as ValueType) || 'NUMBER'
  const updated: Condition = {
    ...props.condition,
    subject: val,
    subjectType: (node?.meta?.subjectType as Condition['subjectType']) || undefined,
    valueType: vt,
    unit: node?.unit || undefined,
    // 切换 subject 时重置 operator 到该类型允许的第一个
    operator: OPERATOR_SETS[vt]?.[0]?.value || 'GT',
    // DATETIME 默认绝对模式
    thresholdMode: vt === 'DATETIME' ? 'ABSOLUTE' : undefined,
    threshold: vt === 'NUMBER' ? 0 : vt === 'BOOLEAN' ? 1 : '',
    // 显式清除 stale thresholdMax（不同类型切换时数据卫生）
    thresholdMax: undefined,
    // 显式清除 stale 相对模式字段（仅 DATETIME 用得到，且会在 updateField 中重新初始化）
    relDirection: undefined,
    relValue: undefined,
    relUnit: undefined,
    relDirectionMax: undefined,
    relValueMax: undefined,
    relUnitMax: undefined,
  }
  emit('update:condition', updated)
}

function updateField(field: string, value: any) {
  const updated: Condition = {...props.condition, [field]: value}
  // 切换 operator 到 BETWEEN 时初始化 thresholdMax
  if (field === 'operator' && value === 'BETWEEN' && updated.thresholdMax === undefined) {
    if (updated.valueType === 'NUMBER') updated.thresholdMax = 0
    else if (updated.valueType === 'DATETIME') {
      updated.thresholdMax = ''
      updated.relDirectionMax = '-'
      updated.relValueMax = 0
      updated.relUnitMax = 'h'
    }
  }
  emit('update:condition', updated)
}
</script>

<style scoped>
.condition-row {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
  flex-wrap: wrap;
}

.cond-field { flex-shrink: 0; }
.subject-field { min-width: 200px; }
.operator-field { width: 96px; }
.threshold-field { width: 180px; }

.cond-unit {
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
  min-width: 24px;
}

.cond-tilde {
  font-size: 13px;
  color: #606266;
  padding: 0 2px;
}
</style>
