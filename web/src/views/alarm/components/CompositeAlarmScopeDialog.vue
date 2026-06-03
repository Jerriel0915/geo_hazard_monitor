<template>
  <el-dialog :model-value="visible" title="应用范围" width="540px" destroy-on-close @close="emit('update:visible', false)">
    <template #header>
      <div>
        <h3 style="margin: 0; font-size: 16px;">应用范围</h3>
        <p style="margin: 4px 0 0; font-size: 13px; color: #86909c;">选择该策略应用的隐患点</p>
      </div>
    </template>

    <div v-loading="loading">
      <el-checkbox-group v-model="selectedIds">
        <div class="scope-list">
          <el-checkbox v-for="hp in hazardPoints" :key="hp.id" :value="hp.id" class="scope-item">
            {{ hp.name }}
          </el-checkbox>
        </div>
      </el-checkbox-group>
      <el-empty v-if="hazardPoints.length === 0 && !loading" description="暂无可选隐患点" />
    </div>

    <template #footer>
      <el-button @click="emit('update:visible', false)">取消</el-button>
      <el-button type="primary" :loading="saving" @click="handleSave">
        确定 ({{ selectedIds.length }} 个隐患点)
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getHazardPointOptions, getCompositeAlarmScopes, updateCompositeAlarmScopes, type HazardPointOption } from '@/api/compositeAlarm'

const props = defineProps<{
  visible: boolean
  alarmId: number
}>()

const emit = defineEmits<{
  'update:visible': [val: boolean]
}>()

const loading = ref(false)
const saving = ref(false)
const hazardPoints = ref<HazardPointOption[]>([])
const selectedIds = ref<number[]>([])

watch(() => props.visible, async (val) => {
  if (val) {
    loading.value = true
    try {
      const [points, scopes] = await Promise.all([getHazardPointOptions(), getCompositeAlarmScopes(props.alarmId)])
      hazardPoints.value = points
      selectedIds.value = scopes.map(s => s.hazardPointId)
    } finally {
      loading.value = false
    }
  }
}, { immediate: true })

async function handleSave() {
  saving.value = true
  try {
    await updateCompositeAlarmScopes(props.alarmId, selectedIds.value)
    ElMessage.success('应用范围已更新')
    emit('update:visible', false)
  } catch (e: any) {
    ElMessage.error(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.scope-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-height: 400px;
  overflow-y: auto;
}

.scope-item {
  margin: 0;
  padding: 8px 12px;
  border-radius: 6px;
  transition: background 0.15s;
}

.scope-item:hover {
  background: #f2f3f5;
}
</style>
