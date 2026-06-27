<template>
  <div class="model-config-list">
    <el-card v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>模型配置</span>
          <el-button type="primary" size="small" @click="openDialog()" :icon="Plus">新增</el-button>
        </div>
      </template>

      <el-table :data="list" stripe>
        <el-table-column prop="name" label="名称" width="140" />
        <el-table-column prop="baseUrl" label="Base URL" min-width="200" show-overflow-tooltip />
        <el-table-column prop="modelName" label="模型" width="180" />
        <el-table-column prop="maxTokens" label="Max Tokens" width="100" />
        <el-table-column prop="temperature" label="Temperature" width="100" />
        <el-table-column label="API Key" width="140">
          <template #default="{ row }"><span class="api-key-mask">{{ row.apiKey }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.isActive === 1" type="success" size="small">激活</el-tag>
            <el-tag v-else type="info" size="small">未激活</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button text size="small" type="primary" @click="openDialog(row)">编辑</el-button>
            <el-button v-if="row.isActive !== 1" text size="small" type="success" @click="onActivate(row)">激活</el-button>
            <el-button text size="small" type="danger" @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑模型配置' : '新增模型配置'" width="500px">
      <el-form :model="formData" label-width="100px">
        <el-form-item label="名称"><el-input v-model="formData.name" placeholder="如：生产环境" /></el-form-item>
        <el-form-item label="Base URL"><el-input v-model="formData.baseUrl" placeholder="https://api.anthropic.com" /></el-form-item>
        <el-form-item label="API Key"><el-input v-model="formData.apiKey" type="password" show-password :placeholder="editingId ? '不修改请留空' : '输入 API Key'" /></el-form-item>
        <el-form-item label="模型名称"><el-input v-model="formData.modelName" placeholder="claude-sonnet-4-20250514" /></el-form-item>
        <el-form-item label="Max Tokens"><el-input-number v-model="formData.maxTokens" :min="256" :max="32768" /></el-form-item>
        <el-form-item label="Temperature"><el-input-number v-model="formData.temperature" :min="0" :max="2" :step="0.1" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="onSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getModelConfigs, createModelConfig, updateModelConfig, deleteModelConfig, activateModelConfig } from '@/api/terra'
import type { TerraModelConfig } from '@/components/terra/types'

const loading = ref(false)
const list = ref<TerraModelConfig[]>([])
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const formData = reactive<Partial<TerraModelConfig>>({
  name: '', baseUrl: 'https://api.anthropic.com', apiKey: '',
  modelName: 'claude-sonnet-4-20250514', maxTokens: 4096, temperature: 0.7,
})

async function loadData() {
  loading.value = true
  try { list.value = await getModelConfigs() }
  catch (e: any) { ElMessage.error(e.message || '加载失败') }
  finally { loading.value = false }
}

function openDialog(row?: TerraModelConfig) {
  if (row) {
    editingId.value = row.id
    Object.assign(formData, row)
    formData.apiKey = ''
  } else {
    editingId.value = null
    Object.assign(formData, { name: '', baseUrl: 'https://api.anthropic.com', apiKey: '', modelName: 'claude-sonnet-4-20250514', maxTokens: 4096, temperature: 0.7 })
  }
  dialogVisible.value = true
}

async function onSave() {
  try {
    if (editingId.value) {
      const data = { ...formData, id: editingId.value }
      if (!data.apiKey) delete data.apiKey
      await updateModelConfig(data)
    } else {
      await createModelConfig(formData)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadData()
  } catch (e: any) { ElMessage.error(e.message || '保存失败') }
}

async function onActivate(row: TerraModelConfig) {
  try { await activateModelConfig(row.id); ElMessage.success('激活成功'); loadData() }
  catch (e: any) { ElMessage.error(e.message || '激活失败') }
}

async function onDelete(row: TerraModelConfig) {
  try {
    await ElMessageBox.confirm(`确定删除「${row.name}」吗？`, '提示', { type: 'warning' })
    await deleteModelConfig(row.id); ElMessage.success('删除成功'); loadData()
  } catch { /* cancel */ }
}

onMounted(loadData)
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.api-key-mask { font-family: monospace; color: #909399; }
</style>
