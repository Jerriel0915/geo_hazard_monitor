<template>
  <div class="page-content">
    <div class="page-title">组织管理</div>
    <div class="page-body">
      <div class="org-container">
        <div class="org-left">
          <div class="org-tree-header">
            <el-input
              v-model="filterText"
              placeholder="输入组织名称搜索"
              clearable
              size="small"
              :prefix-icon="SearchIcon"
            />
            <el-button type="primary" size="small" @click="handleAddRoot" :icon="PlusIcon">新增</el-button>
          </div>
          <el-tree
            ref="treeRef"
            :data="orgTreeData"
            :props="defaultProps"
            :filter-node-method="filterNode"
            node-key="id"
            highlight-current
            default-expand-all
            :expand-on-click-node="false"
            @node-click="handleNodeClick"
            class="org-tree"
          >
            <template #default="{ node, data }">
              <span class="custom-tree-node">
                <span class="node-label">{{ node.label }}</span>
                <span class="node-actions">
                  <el-button
                    v-if="getLevel(data) < 5"
                    link
                    type="primary"
                    size="small"
                    @click.stop="handleAdd(data)"
                    :icon="PlusIcon"
                    title="添加下级"
                  />
                  <el-button
                    link
                    type="primary"
                    size="small"
                    @click.stop="handleEdit(data)"
                    :icon="EditIcon"
                    title="编辑"
                  />
                  <el-button
                    link
                    type="danger"
                    size="small"
                    @click.stop="handleDelete(data)"
                    :icon="DeleteIcon"
                    title="删除"
                  />
                </span>
              </span>
            </template>
          </el-tree>
        </div>
        <div class="org-right">
          <div v-if="currentOrg" class="org-detail">
            <div class="detail-header">
              <h3>组织详情</h3>
              <el-tag :type="currentOrg.status === 1 ? 'success' : 'danger'">
                {{ currentOrg.status === 1 ? '启用' : '禁用' }}
              </el-tag>
            </div>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="组织名称">{{ currentOrg.name }}</el-descriptions-item>
              <el-descriptions-item label="负责人">{{ currentOrg.leader || '-' }}</el-descriptions-item>
              <el-descriptions-item label="联系电话">{{ currentOrg.phone || '-' }}</el-descriptions-item>
              <el-descriptions-item label="区域">{{ currentOrg.region || '-' }}</el-descriptions-item>
              <el-descriptions-item label="中心">{{ currentOrg.center || '-' }}</el-descriptions-item>
              <el-descriptions-item label="状态">
                <el-tag :type="currentOrg.status === 1 ? 'success' : 'danger'">
                  {{ currentOrg.status === 1 ? '启用' : '禁用' }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="地址" :span="2">{{ currentOrg.address || '-' }}</el-descriptions-item>
              <el-descriptions-item label="创建时间">{{ currentOrg.createTime || '-' }}</el-descriptions-item>
              <el-descriptions-item label="更新时间">{{ currentOrg.updateTime || '-' }}</el-descriptions-item>
            </el-descriptions>
            <div class="detail-actions">
              <el-button type="primary" @click="handleEdit(currentOrg)">编辑组织</el-button>
              <el-button v-if="getLevel(currentOrg) < 5" type="success" @click="handleAdd(currentOrg)">添加下级</el-button>
            </div>
          </div>
          <el-empty v-else description="请选择左侧组织查看详情" />
        </div>
      </div>
    </div>

    <!-- 组织表单弹窗 -->
    <el-dialog
      :title="dialogTitle"
      v-model="dialogVisible"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="100px"
      >
        <el-form-item label="上级组织" v-if="formData.parentId !== 0">
          <el-input v-model="parentName" disabled />
        </el-form-item>
        <el-form-item label="组织名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入组织名称" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="负责人" prop="leader">
          <el-input v-model="formData.leader" placeholder="请输入负责人姓名" maxlength="20" />
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="formData.phone" placeholder="请输入联系电话" maxlength="20" />
        </el-form-item>
        <el-form-item label="区域" prop="region">
          <el-input v-model="formData.region" placeholder="请输入区域" maxlength="50" />
        </el-form-item>
        <el-form-item label="中心" prop="center">
          <el-input v-model="formData.center" placeholder="请输入中心" maxlength="50" />
        </el-form-item>
        <el-form-item label="地址" prop="address">
          <el-input
            v-model="formData.address"
            type="textarea"
            :rows="2"
            placeholder="请输入地址"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="formData.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="formData.sortOrder" :min="0" :max="999" controls-position="right" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch, nextTick, h } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'

interface OrgNode {
  id: number
  name: string
  parentId: number
  leader?: string
  phone?: string
  address?: string
  region?: string
  center?: string
  status: number
  sortOrder: number
  createTime?: string
  updateTime?: string
  children?: OrgNode[]
}

const filterText = ref('')
const treeRef = ref<any>(null)
const currentOrg = ref<OrgNode | null>(null)
const dialogVisible = ref(false)
const dialogTitle = ref('新增组织')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()
const parentName = ref('')
const isEdit = ref(false)

const formData = reactive<OrgNode>({
  id: 0,
  name: '',
  parentId: 0,
  leader: '',
  phone: '',
  address: '',
  region: '',
  center: '',
  status: 1,
  sortOrder: 0
})

const formRules: FormRules = {
  name: [
    { required: true, message: '请输入组织名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$|^0\d{2,3}-?\d{7,8}$|^\d{7,8}$/, message: '请输入正确的联系电话', trigger: 'blur' }
  ],
  status: [
    { required: true, message: '请选择状态', trigger: 'change' }
  ]
}

const defaultProps = {
  children: 'children',
  label: 'name'
}

// 图标渲染函数
const PlusIcon = () => h('svg', { xmlns: 'http://www.w3.org/2000/svg', viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2', width: '14', height: '14' }, [
  h('line', { x1: '12', y1: '5', x2: '12', y2: '19' }),
  h('line', { x1: '5', y1: '12', x2: '19', y2: '12' })
])

const EditIcon = () => h('svg', { xmlns: 'http://www.w3.org/2000/svg', viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2', width: '14', height: '14' }, [
  h('path', { d: 'M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7' }),
  h('path', { d: 'M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z' })
])

const DeleteIcon = () => h('svg', { xmlns: 'http://www.w3.org/2000/svg', viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2', width: '14', height: '14' }, [
  h('polyline', { points: '3 6 5 6 21 6' }),
  h('path', { d: 'M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2' })
])

const SearchIcon = () => h('svg', { xmlns: 'http://www.w3.org/2000/svg', viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2', width: '16', height: '16' }, [
  h('circle', { cx: '11', cy: '11', r: '8' }),
  h('line', { x1: '21', y1: '21', x2: '16.65', y2: '16.65' })
])

// 模拟组织数据
const generateOrgData = (): OrgNode[] => {
  return [
    {
      id: 1,
      name: '地质灾害监测中心',
      parentId: 0,
      leader: '张主任',
      phone: '13800138001',
      address: '北京市海淀区中关村大街1号',
      region: '华北区',
      center: '总部',
      status: 1,
      sortOrder: 1,
      createTime: '2024-01-01 10:00:00',
      updateTime: '2024-03-15 14:30:00',
      children: [
        {
          id: 2,
          name: '监测一部',
          parentId: 1,
          leader: '李部长',
          phone: '13800138002',
          address: '北京市海淀区中关村大街1号A座',
          region: '华北区',
          center: '一部',
          status: 1,
          sortOrder: 1,
          createTime: '2024-01-05 09:00:00',
          updateTime: '2024-03-10 11:20:00',
          children: [
            {
              id: 4,
              name: '北京监测组',
              parentId: 2,
              leader: '王组长',
              phone: '13800138004',
              address: '北京市朝阳区建国路88号',
              region: '北京',
              center: '一组',
              status: 1,
              sortOrder: 1,
              createTime: '2024-01-10 08:30:00',
              updateTime: '2024-02-28 16:00:00'
            },
            {
              id: 5,
              name: '天津监测组',
              parentId: 2,
              leader: '赵组长',
              phone: '13800138005',
              address: '天津市滨海新区泰达大街1号',
              region: '天津',
              center: '二组',
              status: 1,
              sortOrder: 2,
              createTime: '2024-01-12 10:00:00',
              updateTime: '2024-03-01 09:30:00'
            }
          ]
        },
        {
          id: 3,
          name: '监测二部',
          parentId: 1,
          leader: '刘部长',
          phone: '13800138003',
          address: '北京市海淀区中关村大街1号B座',
          region: '华北区',
          center: '二部',
          status: 1,
          sortOrder: 2,
          createTime: '2024-01-06 14:00:00',
          updateTime: '2024-03-12 15:45:00',
          children: [
            {
              id: 6,
              name: '河北监测组',
              parentId: 3,
              leader: '陈组长',
              phone: '13800138006',
              address: '石家庄市长安区建设大街66号',
              region: '河北',
              center: '三组',
              status: 0,
              sortOrder: 1,
              createTime: '2024-01-15 11:00:00',
              updateTime: '2024-03-05 10:20:00'
            }
          ]
        }
      ]
    }
  ]
}

const orgTreeData = ref<OrgNode[]>(generateOrgData())

// 搜索过滤
watch(filterText, (val) => {
  treeRef.value?.filter(val)
})

const filterNode = (value: string, data: OrgNode) => {
  if (!value) return true
  return data.name.includes(value)
}

// 获取节点层级
const getLevel = (data: OrgNode): number => {
  let level = 1
  let parent = findParent(orgTreeData.value, data.parentId)
  while (parent) {
    level++
    parent = findParent(orgTreeData.value, parent.parentId)
  }
  return level
}

// 查找父节点
const findParent = (nodes: OrgNode[], parentId: number): OrgNode | null => {
  for (const node of nodes) {
    if (node.id === parentId) return node
    if (node.children) {
      const found = findParent(node.children, parentId)
      if (found) return found
    }
  }
  return null
}

// 查找节点
const findNode = (nodes: OrgNode[], id: number): OrgNode | null => {
  for (const node of nodes) {
    if (node.id === id) return node
    if (node.children) {
      const found = findNode(node.children, id)
      if (found) return found
    }
  }
  return null
}

// 生成ID
const generateId = (): number => {
  let maxId = 0
  const findMaxId = (nodes: OrgNode[]) => {
    for (const node of nodes) {
      if (node.id > maxId) maxId = node.id
      if (node.children) findMaxId(node.children)
    }
  }
  findMaxId(orgTreeData.value)
  return maxId + 1
}

// 点击节点
const handleNodeClick = (data: OrgNode) => {
  currentOrg.value = data
}

// 新增根组织
const handleAddRoot = () => {
  isEdit.value = false
  dialogTitle.value = '新增组织'
  resetForm()
  formData.parentId = 0
  parentName.value = '顶级组织'
  dialogVisible.value = true
}

// 新增子组织
const handleAdd = (data: OrgNode) => {
  const level = getLevel(data)
  if (level >= 5) {
    ElMessage.warning('组织层级最多支持5级')
    return
  }
  isEdit.value = false
  dialogTitle.value = '新增下级组织'
  resetForm()
  formData.parentId = data.id
  parentName.value = data.name
  dialogVisible.value = true
}

// 编辑组织
const handleEdit = (data: OrgNode) => {
  isEdit.value = true
  dialogTitle.value = '编辑组织'
  Object.assign(formData, { ...data })
  const parent = findParent(orgTreeData.value, data.parentId)
  parentName.value = parent ? parent.name : '顶级组织'
  dialogVisible.value = true
}

// 删除组织
const handleDelete = (data: OrgNode) => {
  if (data.children && data.children.length > 0) {
    ElMessage.warning('请先删除下级组织')
    return
  }
  ElMessageBox.confirm(
    `确定要删除组织 "${data.name}" 吗？`,
    '系统提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    deleteNode(orgTreeData.value, data.id)
    if (currentOrg.value?.id === data.id) {
      currentOrg.value = null
    }
    ElMessage.success('删除成功')
  }).catch(() => {})
}

// 删除节点
const deleteNode = (nodes: OrgNode[], id: number): boolean => {
  const index = nodes.findIndex(node => node.id === id)
  if (index !== -1) {
    nodes.splice(index, 1)
    return true
  }
  for (const node of nodes) {
    if (node.children && deleteNode(node.children, id)) {
      return true
    }
  }
  return false
}

// 重置表单
const resetForm = () => {
  formData.id = 0
  formData.name = ''
  formData.parentId = 0
  formData.leader = ''
  formData.phone = ''
  formData.address = ''
  formData.region = ''
  formData.center = ''
  formData.status = 1
  formData.sortOrder = 0
  nextTick(() => {
    formRef.value?.resetFields()
  })
}

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate((valid) => {
    if (valid) {
      submitLoading.value = true
      setTimeout(() => {
        const now = new Date().toLocaleString('zh-CN', { hour12: false })
        if (isEdit.value) {
          // 编辑
          const node = findNode(orgTreeData.value, formData.id)
          if (node) {
            Object.assign(node, {
              ...formData,
              updateTime: now
            })
            if (currentOrg.value?.id === node.id) {
              currentOrg.value = { ...node }
            }
          }
          ElMessage.success('修改成功')
        } else {
          // 新增
          const newNode: OrgNode = {
            ...formData,
            id: generateId(),
            createTime: now,
            updateTime: now
          }
          if (formData.parentId === 0) {
            orgTreeData.value.push(newNode)
          } else {
            const parent = findNode(orgTreeData.value, formData.parentId)
            if (parent) {
              if (!parent.children) parent.children = []
              parent.children.push(newNode)
            }
          }
          ElMessage.success('新增成功')
        }
        dialogVisible.value = false
        submitLoading.value = false
        // 刷新树
        nextTick(() => {
          treeRef.value?.filter(filterText.value)
        })
      }, 500)
    }
  })
}
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
  font-weight: bold;
  color: #303133;
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 1px solid #e4e7ed;
}

.page-body {
  padding: 0;
}

.org-container {
  display: flex;
  gap: 20px;
  height: calc(100vh - 220px);
}

.org-left {
  width: 380px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 16px;
  background: #fafafa;
  display: flex;
  flex-direction: column;
}

.org-tree-header {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
}

.org-tree-header .el-input {
  flex: 1;
}

.org-tree {
  flex: 1;
  overflow: auto;
  background: transparent;
}

.custom-tree-node {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex: 1;
  padding-right: 8px;
}

.node-label {
  font-size: 14px;
}

.node-actions {
  display: none;
  gap: 4px;
}

.custom-tree-node:hover .node-actions {
  display: flex;
}

.org-right {
  flex: 1;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 20px;
  background: #fff;
  overflow: auto;
}

.org-detail {
  height: 100%;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #e4e7ed;
}

.detail-header h3 {
  margin: 0;
  font-size: 16px;
  color: #303133;
}

.detail-actions {
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid #e4e7ed;
  display: flex;
  gap: 12px;
}

:deep(.el-tree-node__content) {
  height: 36px;
  border-radius: 4px;
  margin: 2px 0;
}

:deep(.el-tree-node__content:hover) {
  background-color: #f0f5ff;
}

:deep(.el-tree--highlight-current .el-tree-node.is-current > .el-tree-node__content) {
  background-color: #e6f7ff;
  color: #1890ff;
}
</style>
