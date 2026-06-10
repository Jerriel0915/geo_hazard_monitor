<template>
  <div class="criteria-detail-panel">
    <div class="criteria-header">
      <div class="header-left-area">
        <span class="sub-title">{{ title }}</span>
        <el-button v-if="criteriaList.length === 0" type="primary" size="small" @click="$emit('create')">新增判据
        </el-button>
      </div>
      <div v-if="activeCriteria" class="header-actions">
        <el-switch :model-value="activeCriteria.isEnabled === 1" @change="$emit('toggle', activeCriteria)"
                   size="small" active-text="启用" inactive-text="停用"/>
        <el-button size="small" @click="resetAllLevels">清空</el-button>
        <el-button size="small" type="primary" :loading="saving" @click="$emit('save-form')">保存</el-button>
      </div>
    </div>

    <template v-if="activeCriteria">
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
    </template>

    <el-empty
        v-else
        description="暂未配置判据，请点击上方「新增判据」按钮创建"
        :image-size="100"
    />
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
  create: []; edit: [c: AlarmCriteriaItem]; delete: [c: AlarmCriteriaItem]; toggle: [c: AlarmCriteriaItem]
  'save-form': []
}>()

const activeCriteria = computed(() => props.criteriaList.find(c => c.id === props.selectedId) || (props.criteriaList.length === 1 ? props.criteriaList[0] : null))

watch(() => props.criteriaList, (list) => {
  if (list.length === 1 && props.selectedId !== list[0].id) {
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
  gap: 12px;
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
