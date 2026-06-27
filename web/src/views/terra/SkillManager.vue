<template>
  <div class="skill-manager">
    <el-card v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>技能管理</span>
          <el-tag type="info" size="small">共 {{ list.length }} 个</el-tag>
        </div>
      </template>

      <div class="skill-grid">
        <div v-for="skill in list" :key="skill.id" class="skill-card">
          <div class="skill-card-header">
            <el-icon size="20" color="#409EFF"><MagicStick /></el-icon>
            <span class="skill-name">{{ skill.displayName || skill.name }}</span>
            <el-tag v-if="skill.sourceType === 'preset'" type="warning" size="small">预置</el-tag>
          </div>
          <p class="skill-desc">{{ skill.description || '暂无描述' }}</p>
          <div class="skill-meta">
            <span v-if="skill.category" class="meta-item"><el-tag size="small" effect="plain">{{ skill.category }}</el-tag></span>
            <span v-if="skill.version" class="meta-item">v{{ skill.version }}</span>
          </div>
          <div class="skill-actions">
            <el-switch :model-value="skill.isEnabled === 1" @change="onToggle(skill)" :disabled="skill.sourceType === 'preset'" size="small" />
            <el-button v-if="skill.sourceType !== 'preset'" text size="small" type="danger" @click="onDelete(skill)">卸载</el-button>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { MagicStick } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getSkills, deleteSkill, toggleSkill } from '@/api/terra'
import type { TerraSkill } from '@/components/terra/types'

const loading = ref(false)
const list = ref<TerraSkill[]>([])

async function loadData() {
  loading.value = true
  try { list.value = await getSkills() }
  catch (e: any) { ElMessage.error(e.message || '加载失败') }
  finally { loading.value = false }
}

async function onToggle(skill: TerraSkill) {
  try { await toggleSkill(skill.id); skill.isEnabled = skill.isEnabled === 1 ? 0 : 1; ElMessage.success(skill.isEnabled === 1 ? '已启用' : '已停用') }
  catch (e: any) { ElMessage.error(e.message || '操作失败') }
}

async function onDelete(skill: TerraSkill) {
  try {
    await ElMessageBox.confirm(`确定卸载「${skill.displayName || skill.name}」吗？`, '提示', { type: 'warning' })
    await deleteSkill(skill.id); ElMessage.success('卸载成功'); loadData()
  } catch { /* cancel */ }
}

onMounted(loadData)
</script>

<style scoped>
.card-header { display: flex; align-items: center; gap: 8px; }
.skill-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 16px; }
.skill-card { border: 1px solid #e4e7ed; border-radius: 8px; padding: 14px; transition: box-shadow 0.2s; }
.skill-card:hover { box-shadow: 0 2px 12px rgba(0,0,0,0.08); }
.skill-card-header { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.skill-name { font-weight: 500; flex: 1; }
.skill-desc { color: #606266; font-size: 13px; margin: 0 0 8px; line-height: 1.5; min-height: 20px; }
.skill-meta { display: flex; gap: 8px; margin-bottom: 8px; font-size: 12px; color: #909399; }
.skill-actions { display: flex; justify-content: space-between; align-items: center; margin-top: 8px; padding-top: 8px; border-top: 1px solid #f5f7fa; }
</style>
