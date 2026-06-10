<template>
  <div class="grouped-rule-builder">
    <template v-if="groups.length === 0">
      <div class="empty-state">
        <span class="empty-text">暂无条件组，点击下方按钮添加</span>
      </div>
    </template>

    <template v-for="(group, idx) in groups" :key="idx">
      <div v-if="idx > 0" class="group-connector">
        <el-button
            size="small"
            :type="groupLogic === 'AND' ? 'primary' : 'default'"
            round
            class="connector-btn"
            @click="toggleGroupLogic"
        >
          {{ groupLogic === 'AND' ? '且 (组间)' : '或 (组间)' }}
        </el-button>
      </div>
      <ConditionGroup
          :group="group"
          :group-index="idx"
          :indicator-tree="indicatorTree"
          :node-map="nodeMap"
          :group-color="color"
          @update:group="g => onUpdateGroup(idx, g)"
          @remove="onRemoveGroup(idx)"
      />
    </template>

    <el-button
        v-if="groups.length < 5"
        size="small"
        type="primary"
        text
        class="add-group-btn"
        @click="onAddGroup"
    >
      + 添加条件组
    </el-button>
  </div>
</template>

<script setup lang="ts">
import ConditionGroup from './ConditionGroup.vue'
import type {IndicatorTreeNode, ConditionGroup as CG} from '../composables/useIndicatorTree'

const props = defineProps<{
  groups: CG[]
  groupLogic: 'AND' | 'OR'
  indicatorTree: IndicatorTreeNode[]
  nodeMap: Map<string, IndicatorTreeNode>
  color?: string
}>()

const emit = defineEmits<{
  'update:groups': [groups: CG[]]
  'update:groupLogic': [logic: 'AND' | 'OR']
}>()

function onAddGroup() {
  emit('update:groups', [...props.groups, {conditions: [], logicOperator: 'AND'}])
}

function onUpdateGroup(idx: number, g: CG) {
  const updated = [...props.groups]
  updated[idx] = g
  emit('update:groups', updated)
}

function onRemoveGroup(idx: number) {
  emit('update:groups', props.groups.filter((_, i) => i !== idx))
}

function toggleGroupLogic() {
  emit('update:groupLogic', props.groupLogic === 'AND' ? 'OR' : 'AND')
}
</script>

<style scoped>
.grouped-rule-builder {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.empty-state {
  text-align: center;
  padding: 16px 0;
}

.empty-text {
  font-size: 13px;
  color: #c0c4cc;
}

.group-connector {
  display: flex;
  justify-content: center;
}

.connector-btn {
  font-size: 12px;
  padding: 2px 14px;
  border-style: dashed;
}

.add-group-btn {
  font-size: 12px;
}
</style>
