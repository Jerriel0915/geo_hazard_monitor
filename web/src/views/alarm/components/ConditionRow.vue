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
        :render-after-expand="false"
        node-key="value"
        :props="{ children: 'children', label: 'displayLabel', value: 'value', disabled: 'disabled' }"
        @update:model-value="onSubjectChange"
    >
      <template #default="{ data }">
        <span>{{ data.label }}</span>
      </template>
    </el-tree-select>
    <el-select
        :model-value="condition.operator"
        size="small"
        class="cond-field operator-field"
        @update:model-value="(v: string) => updateField('operator', v)"
    >
      <el-option v-for="op in operators" :key="op.value" :label="op.label" :value="op.value"/>
    </el-select>
    <el-input-number
        :model-value="condition.threshold"
        size="small"
        :precision="2"
        :step="0.1"
        controls-position="right"
        class="cond-field threshold-field"
        @update:model-value="(v: number | null) => updateField('threshold', v ?? 0)"
    />
    <span v-if="condition.unit" class="cond-unit">{{ condition.unit }}</span>
    <el-button size="small" type="danger" text @click="$emit('remove')">
      <el-icon><Delete/></el-icon>
    </el-button>
  </div>
</template>

<script setup lang="ts">
import {Delete} from '@element-plus/icons-vue'
import type {IndicatorTreeNode, Condition} from '../composables/useIndicatorTree'

const props = defineProps<{
  condition: Condition
  indicatorTree: IndicatorTreeNode[]
  nodeMap: Map<string, IndicatorTreeNode>
}>()

const emit = defineEmits<{
  'update:condition': [c: Condition]
  remove: []
}>()

const operators = [
  {value: 'GT', label: '>'},
  {value: 'GTE', label: '>='},
  {value: 'LT', label: '<'},
  {value: 'LTE', label: '<='},
  {value: 'EQ', label: '=='},
  {value: 'NEQ', label: '!='},
]

function onSubjectChange(val: string) {
  const node = props.nodeMap.get(val)
  const updated: Condition = {
    ...props.condition,
    subject: val,
    subjectType: (node?.meta?.subjectType as Condition['subjectType']) || undefined,
    unit: node?.unit || undefined,
  }
  emit('update:condition', updated)
}

function updateField(field: string, value: string | number) {
  emit('update:condition', {...props.condition, [field]: value})
}
</script>

<style scoped>
.condition-row {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}

.cond-field {
  flex-shrink: 0;
}

.subject-field {
  width: 160px;
}

.operator-field {
  width: 72px;
}

.threshold-field {
  width: 100px;
}

.cond-unit {
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
  min-width: 24px;
}
</style>
