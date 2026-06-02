<template>
  <div class="page-content">
    <div class="page-title">组织管理</div>
    <div class="toolbar">
      <el-form :model="searchForm" inline>
        <el-form-item label="组织编码">
          <el-input v-model="searchForm.code" placeholder="请输入组织编码" clearable />
        </el-form-item>
        <el-form-item label="组织名称">
          <el-input v-model="searchForm.name" placeholder="请输入组织名称" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部状态" clearable style="width: 140px">
            <el-option label="启用" :value="0" />
            <el-option label="禁用" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
      <el-button type="primary" @click="handleAddRoot">新增组织</el-button>
    </div>

    <div class="content">
      <div class="tree-panel">
        <div class="panel-title">组织树</div>
        <el-input v-model="treeKeyword" placeholder="输入组织名称过滤" clearable />
        <el-tree
          ref="treeRef"
          class="tree-body"
          node-key="id"
          :data="treeData"
          :props="{ label: 'name', children: 'children' }"
          default-expand-all
          highlight-current
          :filter-node-method="filterTreeNode"
          @node-click="handleTreeClick"
        />
      </div>

      <div class="list-panel">
        <div class="panel-title">组织列表</div>
        <el-table :data="listData" border stripe v-loading="loading" @row-click="handleRowClick">
          <el-table-column prop="code" label="组织编码" width="140" />
          <el-table-column prop="name" label="组织名称" min-width="180" />
          <el-table-column prop="level" label="层级" width="80" align="center" />
          <el-table-column prop="leader" label="负责人" width="120" />
          <el-table-column prop="phone" label="联系电话" width="150" />
          <el-table-column prop="status" label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 0 ? 'success' : 'danger'">
                {{ row.status === 0 ? '启用' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="180" />
          <el-table-column label="操作" width="220" fixed="right">
            <template #default="{ row }">
              <span class="action-link" @click.stop="handleAddChild(row)">新增下级</span>
              <span class="action-link" @click.stop="handleEdit(row)">编辑</span>
              <span class="action-link action-danger" @click.stop="handleDelete(row)">删除</span>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination">
          <el-pagination
            v-model:current-page="pagination.pageNum"
            v-model:page-size="pagination.pageSize"
            :total="pagination.total"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            @current-change="loadList"
            @size-change="handleSizeChange"
          />
        </div>

        <div class="detail-card">
          <div class="detail-header">
            <span>组织详情</span>
            <el-button v-if="currentOrg" link type="primary" @click="handleEdit(currentOrg)">编辑</el-button>
          </div>
          <el-descriptions v-if="currentOrg" :column="2" border>
            <el-descriptions-item label="组织编码">{{ currentOrg.code || '-' }}</el-descriptions-item>
            <el-descriptions-item label="组织名称">{{ currentOrg.name || '-' }}</el-descriptions-item>
            <el-descriptions-item label="父级路径">{{ currentOrg.parentIds || '-' }}</el-descriptions-item>
            <el-descriptions-item label="层级">{{ currentOrg.level ?? '-' }}</el-descriptions-item>
            <el-descriptions-item label="负责人">{{ currentOrg.leader || '-' }}</el-descriptions-item>
            <el-descriptions-item label="联系电话">{{ currentOrg.phone || '-' }}</el-descriptions-item>
            <el-descriptions-item label="邮箱">{{ currentOrg.email || '-' }}</el-descriptions-item>
            <el-descriptions-item label="区域">{{ currentOrg.region || '-' }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="currentOrg.status === 0 ? 'success' : 'danger'">
                {{ currentOrg.status === 0 ? '启用' : '禁用' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="排序">{{ currentOrg.sortOrder ?? '-' }}</el-descriptions-item>
            <el-descriptions-item label="地址" :span="2">{{ currentOrg.address || '-' }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ currentOrg.createTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="更新时间">{{ currentOrg.updateTime || '-' }}</el-descriptions-item>
          </el-descriptions>
          <el-empty v-else description="请选择组织节点或列表数据" />
        </div>
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="620px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="上级组织">
          <el-input :model-value="parentName" disabled />
        </el-form-item>
        <el-form-item label="组织编码" prop="code">
          <el-input v-model="formData.code" placeholder="请输入组织编码" />
        </el-form-item>
        <el-form-item label="组织名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入组织名称" />
        </el-form-item>
        <el-form-item label="负责人" prop="leader">
          <el-input v-model="formData.leader" placeholder="请输入负责人" />
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="formData.phone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="formData.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="区域" prop="region">
          <el-input v-model="formData.region" placeholder="请输入区域" />
        </el-form-item>
        <el-form-item label="地址" prop="address">
          <el-input v-model="formData.address" type="textarea" :rows="2" placeholder="请输入地址" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="formData.status">
            <el-radio :label="0">启用</el-radio>
            <el-radio :label="1">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="formData.sortOrder" :min="0" :max="999" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  createOrganization,
  deleteOrganization,
  getOrganizationDetail,
  getOrganizationPage,
  getOrganizationTree,
  updateOrganization,
  type OrganizationItem
} from '@/api/system'

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增组织')
const currentOrg = ref<OrganizationItem | null>(null)
const treeData = ref<OrganizationItem[]>([])
const listData = ref<OrganizationItem[]>([])
const treeKeyword = ref('')
const treeRef = ref()
const formRef = ref<FormInstance>()
const isEdit = ref(false)
const editingId = ref<number>()
const parentName = ref('顶级组织')

const searchForm = reactive({
  code: '',
  name: '',
  status: undefined as number | undefined
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const formData = reactive({
  code: '',
  name: '',
  parentId: 0,
  leader: '',
  phone: '',
  email: '',
  region: '',
  address: '',
  status: 0,
  sortOrder: 0
})

const formRules: FormRules = {
  code: [{ required: true, message: '请输入组织编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入组织名称', trigger: 'blur' }],
  parentId: [{ required: true, message: '请选择上级组织', trigger: 'change' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }]
}

watch(treeKeyword, (value) => {
  treeRef.value?.filter(value)
})

const filterTreeNode = (value: string, data: OrganizationItem) => {
  if (!value) return true
  return data.name.includes(value) || data.code?.includes(value)
}

const loadTree = async () => {
  treeData.value = await getOrganizationTree()
}

const loadList = async () => {
  loading.value = true
  try {
    const data = await getOrganizationPage({
      ...searchForm,
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    })
    listData.value = data.rows
    pagination.total = data.total
  } finally {
    loading.value = false
  }
}

const loadDetail = async (id: number) => {
  currentOrg.value = await getOrganizationDetail(id)
}

const handleSearch = async () => {
  pagination.pageNum = 1
  await Promise.all([loadTree(), loadList()])
}

const handleReset = async () => {
  searchForm.code = ''
  searchForm.name = ''
  searchForm.status = undefined
  pagination.pageNum = 1
  await Promise.all([loadTree(), loadList()])
}

const handleTreeClick = async (node: OrganizationItem) => {
  await loadDetail(node.id)
}

const handleRowClick = async (row: OrganizationItem) => {
  await loadDetail(row.id)
}

const handleSizeChange = async (size: number) => {
  pagination.pageSize = size
  pagination.pageNum = 1
  await loadList()
}

const resetForm = () => {
  formData.code = ''
  formData.name = ''
  formData.parentId = 0
  formData.leader = ''
  formData.phone = ''
  formData.email = ''
  formData.region = ''
  formData.address = ''
  formData.status = 0
  formData.sortOrder = 0
  editingId.value = undefined
  formRef.value?.clearValidate()
}

const handleAddRoot = () => {
  isEdit.value = false
  dialogTitle.value = '新增组织'
  resetForm()
  parentName.value = '顶级组织'
  dialogVisible.value = true
}

const handleAddChild = (row: OrganizationItem) => {
  isEdit.value = false
  dialogTitle.value = '新增下级组织'
  resetForm()
  formData.parentId = row.id
  parentName.value = row.name
  dialogVisible.value = true
}

const handleEdit = async (row: OrganizationItem) => {
  isEdit.value = true
  dialogTitle.value = '编辑组织'
  resetForm()
  const detail = await getOrganizationDetail(row.id)
  editingId.value = detail.id
  formData.code = detail.code
  formData.name = detail.name
  formData.parentId = detail.parentId
  formData.leader = detail.leader || ''
  formData.phone = detail.phone || ''
  formData.email = detail.email || ''
  formData.region = detail.region || ''
  formData.address = detail.address || ''
  formData.status = detail.status ?? 0
  formData.sortOrder = detail.sortOrder ?? 0
  parentName.value = findOrganizationName(treeData.value, detail.parentId) || '顶级组织'
  dialogVisible.value = true
}

const handleDelete = async (row: OrganizationItem) => {
  if (row.children?.length) {
    ElMessage.warning('存在下级组织，无法删除')
    return
  }
  await ElMessageBox.confirm(`确定删除组织“${row.name}”吗？`, '系统提示', {
    type: 'warning'
  })
  await deleteOrganization(row.id)
  ElMessage.success('删除成功')
  if (currentOrg.value?.id === row.id) {
    currentOrg.value = null
  }
  await Promise.all([loadTree(), loadList()])
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate()
  submitLoading.value = true
  try {
    const payload = {
      code: formData.code,
      name: formData.name,
      parentId: formData.parentId,
      leader: formData.leader,
      phone: formData.phone,
      email: formData.email,
      region: formData.region,
      address: formData.address,
      status: formData.status,
      sortOrder: formData.sortOrder
    }
    if (isEdit.value && editingId.value) {
      await updateOrganization(editingId.value, payload)
      ElMessage.success('修改成功')
      await loadDetail(editingId.value)
    } else {
      const data = await createOrganization(payload)
      ElMessage.success('新增成功')
      await loadDetail(data.id)
    }
    dialogVisible.value = false
    await Promise.all([loadTree(), loadList()])
  } finally {
    submitLoading.value = false
  }
}

const findOrganizationName = (nodes: OrganizationItem[], id?: number): string => {
  if (!id) return ''
  for (const node of nodes) {
    if (node.id === id) return node.name
    if (node.children?.length) {
      const match = findOrganizationName(node.children, id)
      if (match) return match
    }
  }
  return ''
}

onMounted(async () => {
  await Promise.all([loadTree(), loadList()])
})
</script>

<style scoped>
.page-content {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  min-height: calc(100% - 32px);
}

.page-title {
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 1px solid #e8e8e8;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.content {
  display: flex;
  gap: 16px;
}

.tree-panel {
  width: 320px;
  padding: 16px;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  background: #fafafa;
}

.tree-body {
  margin-top: 12px;
  max-height: 720px;
  overflow: auto;
}

.list-panel {
  flex: 1;
  padding: 16px;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
}

.panel-title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 12px;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.detail-card {
  margin-top: 20px;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.action-link {
  display: inline-block;
  margin-right: 8px;
  color: #1890ff;
  cursor: pointer;
}

.action-danger {
  color: #f56c6c;
}
</style>
