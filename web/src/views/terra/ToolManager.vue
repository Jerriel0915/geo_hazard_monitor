<template>
  <div class="tool-manager">
    <el-card v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>工具管理</span>
          <el-button type="primary" size="small" @click="openDialog()" :icon="Plus">新增工具</el-button>
        </div>
      </template>

      <el-table :data="list" stripe>
        <el-table-column prop="toolKey" label="工具标识" width="160" />
        <el-table-column prop="name" label="名称" width="140" />
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column label="执行侧" width="80">
          <template #default="{ row }">
            <el-tag :type="row.execSide === 'frontend' ? 'warning' : 'info'" size="small">{{ row.execSide === 'frontend' ? '前端' : '后端' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="80">
          <template #default="{ row }"><el-tag size="small" effect="plain">{{ row.toolType === 'code' ? '代码' : '配置' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.isEnabled === 1 ? 'success' : 'info'" size="small">{{ row.isEnabled === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button text size="small" type="primary" @click="openDialog(row)">编辑</el-button>
            <el-button text size="small" @click="onToggle(row)">{{ row.isEnabled === 1 ? '停用' : '启用' }}</el-button>
            <el-button text size="small" type="danger" @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑工具' : '新增工具'" width="520px">
      <el-form :model="formData" label-width="100px">
        <el-form-item label="工具标识"><el-input v-model="formData.toolKey" placeholder="如: query_device" :disabled="!!editingId" /></el-form-item>
        <el-form-item label="名称"><el-input v-model="formData.name" placeholder="显示名称" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="formData.description" type="textarea" :rows="2" placeholder="工具描述（会发给 LLM）" /></el-form-item>
        <el-form-item label="执行侧"><el-radio-group v-model="formData.execSide"><el-radio value="backend">后端</el-radio><el-radio value="frontend">前端</el-radio></el-radio-group></el-form-item>
        <el-form-item label="类型"><el-radio-group v-model="formData.toolType"><el-radio value="code">代码注册</el-radio><el-radio value="config">配置(HTTP)</el-radio></el-radio-group></el-form-item>
        <el-form-item label="分类"><el-input v-model="formData.category" placeholder="如: device" /></el-form-item>
        <el-form-item v-if="formData.toolType === 'config'" label="配置 JSON"><el-input v-model="formData.config" type="textarea" :rows="4" placeholder='{"method":"GET","url":"http://...","headers":{}}' /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="onSave">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTools, createTool, updateTool, deleteTool, toggleTool } from '@/api/terra'
import type { TerraTool } from '@/components/terra/types'

const loading = ref(false)
const list = ref<TerraTool[]>([])
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const formData = reactive<Partial<TerraTool>>({
  toolKey: '', name: '', description: '', execSide: 'backend', toolType: 'config', category: '', config: '',
})

async function loadData() {
  loading.value = true
  try { list.value = await getTools() }
  catch (e: any) { ElMessage.error(e.message || '加载失败') }
  finally { loading.value = false }
}

function openDialog(row?: TerraTool) {
  if (row) { editingId.value = row.id; Object.assign(formData, row) }
  else { editingId.value = null; Object.assign(formData, { toolKey: '', name: '', description: '', execSide: 'backend', toolType: 'config', category: '', config: '' }) }
  dialogVisible.value = true
}

async function onSave() {
  try {
    if (editingId.value) await updateTool({ ...formData, id: editingId.value })
    else await createTool(formData)
    ElMessage.success('保存成功'); dialogVisible.value = false; loadData()
  } catch (e: any) { ElMessage.error(e.message || '保存失败') }
}

async function onToggle(row: TerraTool) {
  try { await toggleTool(row.id); row.isEnabled = row.isEnabled === 1 ? 0 : 1; ElMessage.success(row.isEnabled === 1 ? '已启用' : '已停用') }
  catch (e: any) { ElMessage.error(e.message || '操作失败') }
}

async function onDelete(row: TerraTool) {
  try {
    await ElMessageBox.confirm(`确定删除工具「${row.name}」吗？`, '提示', { type: 'warning' })
    await deleteTool(row.id); ElMessage.success('删除成功'); loadData()
  } catch { /* cancel */ }
}

onMounted(loadData)
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>
