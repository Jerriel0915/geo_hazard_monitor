<template>
  <div class="criteria-detail-panel">
    <div class="criteria-header">
      <div class="header-left-area">
        <span class="sub-title">{{ title }}</span>
        <el-tag v-if="activeCriteria" :type="activeCriteria.isEnabled === 1 ? 'success' : 'info'" size="small">
          {{ activeCriteria.isEnabled === 1 ? '已启用' : '已停用' }}
        </el-tag>
      </div>
      <div class="header-actions">
        <el-switch
            v-if="activeCriteria"
            :model-value="activeCriteria.isEnabled === 1"
            @change="$emit('toggle', activeCriteria)"
            size="small" active-text="启用" inactive-text="停用"
        />
        <el-button size="small" @click="resetAllLevels">清空</el-button>
        <el-button size="small" type="primary" :loading="saving" @click="$emit('save-form')">保存</el-button>
      </div>
    </div>

    <div class="level-grid">
      <LevelCriteriaCard
          v-for="lv in alarmLevels"
          :key="lv.value"
          :level="lv"
          :level-data="props.levelForm[lv.key]"
          :indicator-tree="indicatorTree"
          :node-map="nodeMap"
          @update:level-data="d => updateLevel(lv.key, d)"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import {computed, watch} from 'vue'
import type {AlarmCriteriaItem} from '@/api/alarm'
import type {IndicatorTreeNode, LevelFormState} from '../composables/useIndicatorTree'
import LevelCriteriaCard from './LevelCriteriaCard.vue'

const props = defineProps<{
  title: string
  criteriaList: AlarmCriteriaItem[]
  context: string
  alarmLevels: { value: number; label: string; key: string; color: string }[]
  monitorContents: any[]
  saving: boolean
  selectedId: number | null
  levelForm: Record<string, LevelFormState>
  indicatorTree: IndicatorTreeNode[]
  nodeMap: Map<string, IndicatorTreeNode>
}>()

const emit = defineEmits<{
  'update:selectedId': [id: number | null]
  edit: [c: AlarmCriteriaItem]; delete: [c: AlarmCriteriaItem]; toggle: [c: AlarmCriteriaItem]
  'save-form': []
}>()

const activeCriteria = computed(() => {
  if (props.selectedId) return props.criteriaList.find(c => c.id === props.selectedId) || null
  return props.criteriaList.length > 0 ? props.criteriaList[0] : null
})

watch(() => props.criteriaList, (list) => {
  if (list.length > 0 && (!props.selectedId || !list.find(c => c.id === props.selectedId))) {
    emit('update:selectedId', list[0].id)
  }
}, {immediate: true})

function updateLevel(key: string, data: LevelFormState) {
  Object.assign(props.levelForm[key], data)
}

function resetAllLevels() {
  for (const key of ['blue', 'yellow', 'orange', 'red'] as const) {
    Object.assign(props.levelForm[key], {
      groups: [],
      groupLogic: 'AND',
      persistCount: 1,
      silencePeriod: 0,
      description: '',
    })
  }
}
</script>

<style scoped>
.criteria-detail-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.criteria-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-shrink: 0;
}

.header-left-area {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.sub-title {
  font-size: 16px;
  font-weight: 500;
}

.level-grid {
  display: flex;
  flex-direction: column;
  gap: 12px;
  flex: 1;
  min-height: 0;
  overflow-y: auto;
}
</style>
