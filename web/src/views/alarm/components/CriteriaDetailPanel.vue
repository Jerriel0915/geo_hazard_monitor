<template>
  <div>
    <div class="criteria-header">
      <span class="sub-title">{{ title }}</span>
      <el-button v-if="criteriaList.length === 0" type="primary" size="small" @click="$emit('create')">新增判据
      </el-button>
    </div>

    <template v-if="activeCriteria">
      <div class="criteria-info-bar">
        <el-switch :model-value="activeCriteria.isEnabled === 1" @change="$emit('toggle', activeCriteria)"
                   size="small"/>
        <span class="criteria-name-text">{{ activeCriteria.name }}</span>
        <span v-if="activeCriteria.version" class="version-text">v{{ activeCriteria.version }}</span>
        <el-button size="small" @click="$emit('edit', activeCriteria)">重命名</el-button>
        <el-button size="small" type="danger" @click="$emit('delete', activeCriteria)">删除</el-button>
      </div>
      <div class="level-sections">
        <div v-for="lv in alarmLevels" :key="lv.value" class="level-section">
          <div class="level-header">
            <span class="level-title" :style="{ color: lv.color }">{{ lv.value }}、{{ lv.label }}</span>
          </div>
          <div class="level-divider" :style="{ backgroundColor: lv.color }"></div>
          <div class="level-form">
            <div class="form-row">
              <span class="form-label">多指标判据</span>
              <div class="form-field">
                <div class="expr-display" :class="{ 'expr-empty': !levelFormLocal[lv.key].expression }">
                  {{ levelFormLocal[lv.key].expression || '未设置' }}
                </div>
                <el-button type="primary" size="small" @click="$emit('expr-open', lv.key)">修改</el-button>
              </div>
            </div>
            <div class="form-row">
              <span class="form-label">告警持续时长</span>
              <div class="form-field">
                <el-input-number :model-value="levelFormLocal[lv.key].persistCount"
                                 @update:model-value="(v: number | null) => updateLevel(lv.key, 'persistCount', v)"
                                 :min="1" :max="100" size="small"/>
                <span class="unit-text">次</span>
              </div>
            </div>
            <div class="form-row">
              <span class="form-label">静默数据周期</span>
              <div class="form-field">
                <el-input-number :model-value="levelFormLocal[lv.key].silencePeriod"
                                 @update:model-value="(v: number | null) => updateLevel(lv.key, 'silencePeriod', v)"
                                 :min="0" :max="1000" size="small"/>
                <span class="unit-text">次</span>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div class="save-bar">
        <el-button type="primary" :loading="saving" @click="$emit('save-form')">保存判据</el-button>
      </div>
    </template>
    <div v-else class="empty-hint">暂未配置判据，请点击"新增判据"创建</div>
  </div>
</template>

<script setup lang="ts">
import {computed, watch} from 'vue'
import type {AlarmCriteriaItem} from '@/api/alarm'

interface LevelFormState {
  expression: string;
  persistCount: number;
  silencePeriod: number
}

const props = defineProps<{
  title: string
  criteriaList: AlarmCriteriaItem[]
  context: string
  alarmLevels: { value: number; label: string; key: string; color: string }[]
  monitorContents: any[]
  saving: boolean
  selectedId: number | null
  levelForm: Record<string, LevelFormState>
}>()

const emit = defineEmits<{
  'update:selectedId': [id: number | null]
  'update:levelForm': [form: Record<string, LevelFormState>]
  create: []; edit: [c: AlarmCriteriaItem]; delete: [c: AlarmCriteriaItem]; toggle: [c: AlarmCriteriaItem]
  'save-form': []; 'expr-open': [key: string]; 'expr-confirm': [p: { expression: string; description: string }]
}>()

const activeCriteria = computed(() => props.criteriaList.find(c => c.id === props.selectedId) || (props.criteriaList.length === 1 ? props.criteriaList[0] : null))

// 自动选中唯一的判据
watch(() => props.criteriaList, (list) => {
  if (list.length === 1 && props.selectedId !== list[0].id) {
    emit('update:selectedId', list[0].id)
  }
}, {immediate: true})

const levelFormLocal = computed(() => props.levelForm)

function updateLevel(key: string, field: string, v: number | null) {
  const updated = {
    ...props.levelForm,
    [key]: {...props.levelForm[key], [field]: v ?? (field === 'silencePeriod' ? 0 : 1)}
  }
  emit('update:levelForm', updated)
}
</script>

<style scoped>
.criteria-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.sub-title {
  font-size: 16px;
  font-weight: 500;
}

.criteria-info-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 20px;
  padding: 12px 16px;
  background: transparent;
  border-radius: 6px;
}

.criteria-name-text {
  font-weight: 500;
  flex: 1;
}

.version-text {
  font-size: 12px;
  color: #409eff;
}

.level-sections {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.level-section {
  background: transparent;
  border-radius: 8px;
  overflow: hidden;
}

.level-header {
  padding: 20px 24px 10px;
}

.level-title {
  font-size: 16px;
  font-weight: 600;
}

.level-divider {
  height: 2px;
  margin: 0 24px;
}

.level-form {
  padding: 20px 24px;
}

.form-row {
  display: flex;
  align-items: center;
  margin-bottom: 22px;
}

.form-row:last-child {
  margin-bottom: 0;
}

.form-label {
  width: 110px;
  font-size: 14px;
  color: #606266;
  flex-shrink: 0;
}

.form-field {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
}

.expr-display {
  flex: 1;
  padding: 8px 12px;
  font-size: 14px;
  font-family: 'Consolas', 'Courier New', monospace;
  background: rgba(0, 0, 0, .03);
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  color: #303133;
  min-height: 20px;
  line-height: 20px;
  word-break: break-all;
}

.expr-display.expr-empty {
  color: #c0c4cc;
  font-family: inherit;
}

.unit-text {
  font-size: 14px;
  color: #909399;
  white-space: nowrap;
}

.save-bar {
  display: flex;
  justify-content: flex-end;
  padding-top: 8px;
  border-top: 1px solid #ebeef5;
  margin-top: 4px;
}

.empty-hint {
  text-align: center;
  color: #999;
  padding: 60px 0;
  font-size: 14px;
}
</style>
