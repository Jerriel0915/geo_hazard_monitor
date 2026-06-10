<template>
  <div class="condition-group" :style="{'--group-color': groupColor}">
    <div class="group-header">
      <span class="group-index">条件组 {{ groupIndex + 1 }}</span>
      <el-button size="small" text type="danger" @click="$emit('remove')">
        <el-icon><Delete/></el-icon>
      </el-button>
    </div>

    <div v-if="group.conditions.length === 0" class="group-empty">
      <span class="empty-text">暂无条件</span>
    </div>

    <div v-else class="conditions-flow">
      <template v-for="(cond, idx) in group.conditions" :key="idx">
        <el-button
            v-if="idx > 0"
            size="small"
            :type="group.logicOperator === 'AND' ? 'primary' : 'default'"
            round
            class="logic-chip"
            @click="toggleLogic"
        >
          {{ group.logicOperator === 'AND' ? '且' : '或' }}
        </el-button>
        <ConditionRow
            :condition="cond"
            :indicator-tree="indicatorTree"
            :node-map="nodeMap"
            @update:condition="c => onUpdateCondition(idx, c)"
            @remove="onRemoveCondition(idx)"
        />
      </template>
    </div>

    <el-button
        v-if="group.conditions.length < 5"
        size="small"
        type="primary"
        text
        class="add-cond-btn"
        @click="onAddCondition"
    >
      + 添加条件
    </el-button>
  </div>
</template>

<script setup lang="ts">
import {Delete} from '@element-plus/icons-vue'
import ConditionRow from './ConditionRow.vue'
import type {IndicatorTreeNode, Condition, ConditionGroup} from '../composables/useIndicatorTree'

const props = defineProps<{
  group: ConditionGroup
  groupIndex: number
  indicatorTree: IndicatorTreeNode[]
  nodeMap: Map<string, IndicatorTreeNode>
  groupColor?: string
}>()

const emit = defineEmits<{
  'update:group': [g: ConditionGroup]
  remove: []
}>()

function onAddCondition() {
  emit('update:group', {
    ...props.group,
    conditions: [...props.group.conditions, {subject: '', operator: 'GT', threshold: 0}],
  })
}

function onUpdateCondition(idx: number, c: Condition) {
  const updated = [...props.group.conditions]
  updated[idx] = c
  emit('update:group', {...props.group, conditions: updated})
}

function onRemoveCondition(idx: number) {
  const updated = props.group.conditions.filter((_, i) => i !== idx)
  emit('update:group', {...props.group, conditions: updated})
}

function toggleLogic() {
  emit('update:group', {
    ...props.group,
    logicOperator: props.group.logicOperator === 'AND' ? 'OR' : 'AND',
  })
}
</script>

<style scoped>
.condition-group {
  background: #fff;
  border: 1px solid #e5e6eb;
  border-left: 3px solid var(--group-color, #409eff);
  border-radius: 6px;
  padding: 10px 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.group-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.group-index {
  font-size: 12px;
  font-weight: 600;
  color: #606266;
}

.group-empty {
  padding: 4px 0;
}

.empty-text {
  font-size: 13px;
  color: #c0c4cc;
}

.conditions-flow {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
}

.logic-chip {
  font-size: 12px;
  padding: 2px 10px;
  flex-shrink: 0;
}

.add-cond-btn {
  align-self: flex-start;
  font-size: 12px;
}
</style>
