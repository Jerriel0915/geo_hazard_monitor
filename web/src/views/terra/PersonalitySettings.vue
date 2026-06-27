<template>
  <div class="personality-settings">
    <el-card v-loading="loading">
      <template #header><span>人格配置</span></template>

      <div v-for="p in coreList" :key="p.id" class="personality-block">
        <div class="block-header">
          <el-tag type="danger" size="small">核心</el-tag>
          <span class="block-name">{{ p.name }}</span>
          <el-tag type="success" size="small">启用</el-tag>
        </div>
        <el-input type="textarea" :rows="8" :model-value="p.content" readonly resize="none" />
      </div>

      <div v-for="p in roleList" :key="p.id" class="personality-block">
        <div class="block-header">
          <el-tag type="info" size="small">角色</el-tag>
          <span class="block-name">{{ p.name }}</span>
          <el-switch :model-value="p.isActive === 1" @change="onToggle(p)" size="small" />
        </div>
        <el-input type="textarea" :rows="6" v-model="editCache[p.id]" resize="none" placeholder="输入角色定义内容..." />
        <div class="block-footer">
          <el-input v-model="p.name" size="small" style="width: 200px" placeholder="角色名称" />
          <el-button type="primary" size="small" @click="onSave(p)">保存</el-button>
        </div>
      </div>

      <el-button type="primary" plain @click="addRole" :icon="Plus">添加角色层</el-button>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, reactive } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getPersonalities, updatePersonality, togglePersonality } from '@/api/terra'
import type { TerraPersonality } from '@/components/terra/types'

const loading = ref(false)
const list = ref<TerraPersonality[]>([])
const editCache = reactive<Record<number, string>>({})

const coreList = computed(() => list.value.filter(p => p.layerType === 'core'))
const roleList = computed(() => list.value.filter(p => p.layerType === 'role'))

async function loadData() {
  loading.value = true
  try {
    list.value = await getPersonalities()
    list.value.forEach(p => { editCache[p.id] = p.content })
  } catch (e: any) { ElMessage.error(e.message || '加载失败') }
  finally { loading.value = false }
}

async function onSave(p: TerraPersonality) {
  try {
    p.content = editCache[p.id]
    await updatePersonality(p)
    ElMessage.success('保存成功')
  } catch (e: any) { ElMessage.error(e.message || '保存失败') }
}

async function onToggle(p: TerraPersonality) {
  try {
    await togglePersonality(p.id)
    p.isActive = p.isActive === 1 ? 0 : 1
    ElMessage.success(p.isActive === 1 ? '已启用' : '已停用')
  } catch (e: any) { ElMessage.error(e.message || '操作失败') }
}

function addRole() {
  const newRole: TerraPersonality = {
    id: Date.now(), layerType: 'role', name: '新角色', content: '',
    isActive: 1, isPreset: 0, sortOrder: roleList.value.length, createTime: '',
  }
  list.value.push(newRole)
  editCache[newRole.id] = ''
}

onMounted(loadData)
</script>

<style scoped>
.personality-block { margin-bottom: 20px; }
.block-header { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.block-name { font-weight: 500; flex: 1; }
.block-footer { display: flex; justify-content: space-between; margin-top: 8px; }
</style>
