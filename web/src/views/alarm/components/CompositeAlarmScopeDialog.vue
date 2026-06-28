<template>
  <el-dialog :model-value="visible" title="应用范围" width="520px" destroy-on-close
    @close="emit('update:visible', false)">
    <template #header>
      <div>
        <h3 style="margin: 0; font-size: 16px;">应用范围</h3>
        <p style="margin: 4px 0 0; font-size: 13px; color: #86909c;">选择该策略应用的隐患点</p>
      </div>
    </template>

    <div v-loading="loading">
      <el-radio-group v-model="scopeMode" class="scope-mode-group">
        <el-radio value="all">全部隐患点</el-radio>

        <div v-show="scopeMode === 'group'" class="scope-section">
          <el-checkbox-group v-model="selectedGroups" class="scope-list">
            <el-checkbox v-for="g in groups" :key="g.id" :value="`group:${g.id}`" class="scope-item">
              {{ g.name }} ({{ g.code }})
            </el-checkbox>
          </el-checkbox-group>
        </div>

        <el-radio value="group">按分组选择</el-radio>

        <div v-show="scopeMode === 'specific'" class="scope-section">
          <el-checkbox-group v-model="selectedPoints" class="scope-list">
            <el-checkbox v-for="hp in hazardPoints" :key="hp.id" :value="String(hp.id)" class="scope-item">
              {{ hp.name }}
            </el-checkbox>
          </el-checkbox-group>
          <el-empty v-if="hazardPoints.length === 0 && !loading" description="暂无可选隐患点" />
        </div>

        <el-radio value="specific">指定隐患点</el-radio>
      </el-radio-group>
    </div>

    <template #footer>
      <el-button @click="emit('update:visible', false)">取消</el-button>
      <el-button type="primary" :loading="saving" @click="handleSave">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getStrategyScope, updateStrategy } from '@/api/alarm'
import { getHazardPointPage, getHazardPointGroups, type HazardPointRaw, type HazardPointGroupRaw } from '@/api/hazardPoint'

interface GroupItem { id: number; code: string; name: string }
interface PointItem { id: number; name: string }

const props = defineProps<{ visible: boolean; alarmId: number }>()
const emit = defineEmits<{ 'update:visible': [val: boolean] }>()

const loading = ref(false)
const saving = ref(false)
const scopeMode = ref<'all' | 'group' | 'specific'>('specific')
const groups = ref<GroupItem[]>([])
const hazardPoints = ref<PointItem[]>([])
const selectedGroups = ref<string[]>([])
const selectedPoints = ref<string[]>([])

watch(() => props.visible, async (val) => {
  if (!val) return
  loading.value = true
  try {
    const [groupRes, pointRes, scopeRes] = await Promise.all([
      getHazardPointGroups(),
      getHazardPointPage({ pageNum: 1, pageSize: 1000 }),
      getStrategyScope(props.alarmId)
    ])
    groups.value = ((groupRes as any)?.data || (groupRes as any) || []).map((g: HazardPointGroupRaw) => ({ id: g.id, code: g.code, name: g.name }))
    hazardPoints.value = ((pointRes as any)?.rows || []).map((hp: HazardPointRaw) => ({ id: hp.id, name: hp.name }))

    const scopes = Array.isArray(scopeRes) ? scopeRes as string[] : []
    if (scopes.includes('*')) {
      scopeMode.value = 'all'
    } else if (scopes.some(s => s.startsWith('group:'))) {
      scopeMode.value = 'group'
      selectedGroups.value = scopes.filter(s => s.startsWith('group:'))
      selectedPoints.value = scopes.filter(s => !s.startsWith('group:') && s !== '*')
    } else {
      scopeMode.value = 'specific'
      selectedPoints.value = scopes.filter(s => s !== '*')
    }
  } finally {
    loading.value = false
  }
}, { immediate: true })

async function handleSave() {
  saving.value = true
  try {
    let scopeValues: string[]
    if (scopeMode.value === 'all') {
      scopeValues = ['*']
    } else if (scopeMode.value === 'group') {
      scopeValues = [...selectedGroups.value]
    } else {
      scopeValues = [...selectedPoints.value]
    }
    await updateStrategy(props.alarmId, { hazardPointIds: scopeValues } as any)
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
.scope-mode-group { display: flex; flex-direction: column; gap: 8px; }
.scope-section { margin: 4px 0 4px 24px; }
.scope-list { display: flex; flex-direction: column; gap: 4px; max-height: 280px; overflow-y: auto; }
.scope-item { margin: 0; padding: 6px 10px; border-radius: 4px; }
.scope-item:hover { background: #f2f3f5; }
</style>
