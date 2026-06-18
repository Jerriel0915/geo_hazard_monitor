<template>
  <div class="level-card">
    <div class="level-card__band" :style="{backgroundColor: level.color}"></div>
    <div class="level-card__body">
      <div class="level-card__title" :style="{color: level.color}">{{ level.label }}</div>

      <div class="level-card__section">
        <div class="section-label">判据规则</div>
        <GroupedRuleBuilder
            :groups="levelData.groups"
            :group-logic="levelData.groupLogic"
            :indicator-tree="indicatorTree"
            :node-map="nodeMap"
            :color="level.color"
            @update:groups="g => updateField('groups', g)"
            @update:group-logic="l => updateField('groupLogic', l)"
        />
      </div>

      <div class="level-card__row">
        <div class="level-card__field">
          <span class="field-label">持续时长</span>
          <el-input-number
              :model-value="levelData.persistCount"
              @update:model-value="(v: number | null) => updateField('persistCount', v ?? 1)"
              :min="1" :max="100" size="small" controls-position="right"
          />
          <span class="field-unit">次</span>
        </div>
        <div class="level-card__field">
          <span class="field-label">静默周期</span>
          <el-input-number
              :model-value="levelData.silencePeriod"
              @update:model-value="(v: number | null) => updateField('silencePeriod', v ?? 0)"
              :min="0" :max="1000" size="small" controls-position="right"
          />
          <span class="field-unit">h</span>
        </div>
      </div>

      <div class="level-card__section level-card__section--last">
        <div class="section-label">说明</div>
        <el-input
            :model-value="levelData.description"
            @update:model-value="(v: string) => updateField('description', v)"
            type="textarea"
            :rows="2"
            placeholder="告警条件的业务描述（非必填）"
            resize="none"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { IndicatorTreeNode, LevelFormState } from '../composables/useIndicatorTree';
import GroupedRuleBuilder from './GroupedRuleBuilder.vue';

const props = defineProps<{
  level: { value: number; label: string; key: string; color: string }
  levelData: LevelFormState
  indicatorTree: IndicatorTreeNode[]
  nodeMap: Map<string, IndicatorTreeNode>
}>()

const emit = defineEmits<{
  'update:levelData': [data: LevelFormState]
}>()

function updateField(field: string, value: any) {
  emit('update:levelData', {...props.levelData, [field]: value})
}
</script>

<style scoped>
.level-card {
  background: #fff;
  border: 1px solid #e5e6eb;
  border-radius: 8px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
  display: flex;
  flex-direction: column;
}

.level-card__band {
  height: 4px;
  flex-shrink: 0;
}

.level-card__body {
  padding: 12px 16px 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.level-card__title {
  font-size: 14px;
  font-weight: 600;
}

.level-card__section {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.level-card__section--last {
  margin-top: 4px;
}

.section-label {
  font-size: 12px;
  color: #909399;
  font-weight: 500;
}

.level-card__row {
  display: flex;
  gap: 16px;
}

.level-card__field {
  display: flex;
  align-items: center;
  gap: 6px;
}

.field-label {
  font-size: 12px;
  color: #606266;
  white-space: nowrap;
}

.field-unit {
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
}
</style>
