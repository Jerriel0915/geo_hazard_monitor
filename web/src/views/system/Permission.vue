<template>
  <div class="page-content">
    <div class="page-title">权限管理</div>
    <div class="page-body">
      <el-tabs v-model="activeTab" type="border-card">
        <el-tab-pane label="角色管理" name="role">
          <div class="tab-content">
            <div class="search-bar">
              <el-form :model="roleSearchForm" inline>
                <el-form-item label="角色编码">
                  <el-input v-model="roleSearchForm.code" placeholder="请输入角色编码" clearable />
                </el-form-item>
                <el-form-item label="角色名称">
                  <el-input v-model="roleSearchForm.name" placeholder="请输入角色名称" clearable />
                </el-form-item>
                <el-form-item label="状态">
                  <el-select v-model="roleSearchForm.status" placeholder="全部状态" clearable style="width: 140px">
                    <el-option label="正常" :value="0" />
                    <el-option label="停用" :value="1" />
                  </el-select>
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="handleRoleSearch">查询</el-button>
                  <el-button @click="handleRoleReset">重置</el-button>
                </el-form-item>
              </el-form>
              <div class="toolbar">
                <el-button @click="loadRoles">刷新</el-button>
                <el-button type="primary" @click="handleAddRole">新增角色</el-button>
              </div>
            </div>

            <el-table :data="roleList" border stripe v-loading="roleLoading">
              <el-table-column type="index" label="序号" width="60" align="center" />
              <el-table-column prop="name" label="角色名称" width="160" />
              <el-table-column prop="code" label="角色编码" width="160" />
              <el-table-column prop="dataScope" label="数据范围" width="160">
                <template #default="{ row }">
                  {{ getDataScopeLabel(row.dataScope) }}
                </template>
              </el-table-column>
              <el-table-column prop="status" label="状态" width="90" align="center">
                <template #default="{ row }">
                  <el-tag :type="getRoleStatusType(row.status)">{{ getRoleStatusLabel(row.status) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="菜单权限数" width="110" align="center">
                <template #default="{ row }">
                  {{ row.menuIds?.length ?? '-' }}
                </template>
              </el-table-column>
              <el-table-column prop="description" label="角色说明" min-width="220" show-overflow-tooltip />
              <el-table-column prop="createTime" label="创建时间" width="180" />
              <el-table-column label="操作" width="220" fixed="right">
                <template #default="{ row }">
                  <div class="op-cell">
                    <el-button type="primary" text size="small" @click="handleEditRole(row)">编辑</el-button>
                    <el-button type="primary" text size="small" @click="handleConfigPermission(row)">权限配置</el-button>
                    <el-dropdown trigger="hover" @command="(cmd: string) => handleRoleMoreCommand(cmd, row)">
                      <el-button type="primary" text size="small">更多</el-button>
                      <template #dropdown>
                        <el-dropdown-menu>
                          <el-dropdown-item :command="'toggle_' + row.id">
                            {{ row.status === 0 ? '停用' : '启用' }}
                          </el-dropdown-item>
                          <el-dropdown-item v-if="row.id !== 1" command="delete" divided>
                            <span style="color: #f56c6c">删除</span>
                          </el-dropdown-item>
                        </el-dropdown-menu>
                      </template>
                    </el-dropdown>
                  </div>
                </template>
              </el-table-column>
            </el-table>

            <div class="pagination">
              <el-pagination
                v-model:current-page="rolePagination.pageNum"
                v-model:page-size="rolePagination.pageSize"
                :page-sizes="[10, 20, 50, 100]"
                :total="rolePagination.total"
                layout="total, sizes, prev, pager, next, jumper"
                prev-text="上一页"
                next-text="下一页"
                @size-change="handleRoleSizeChange"
                @current-change="handleRolePageChange"
              />
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="菜单管理" name="menu">
          <div class="tab-content">
            <div class="search-bar">
              <div />
              <div class="toolbar">
                <el-button @click="loadMenus">刷新</el-button>
                <el-button type="primary" @click="handleAddMenu">新增菜单</el-button>
              </div>
            </div>

            <el-table
              :data="menuList"
              border
              stripe
              row-key="id"
              default-expand-all
              v-loading="menuLoading"
              :tree-props="{ children: 'children' }"
            >
              <el-table-column prop="name" label="菜单名称" min-width="180" />
              <el-table-column prop="code" label="菜单编码" width="160" />
              <el-table-column prop="path" label="路由路径" width="220" show-overflow-tooltip />
              <el-table-column prop="type" label="类型" width="90" align="center">
                <template #default="{ row }">
                  <el-tag size="small" :type="getMenuTypeTag(row.type)">{{ getMenuTypeLabel(row.type) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="visible" label="显示状态" width="100" align="center">
                <template #default="{ row }">
                  <el-tag size="small" :type="row.visible === 0 ? 'success' : 'info'">
                    {{ row.visible === 0 ? '显示' : '隐藏' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
              <el-table-column prop="perms" label="权限标识" min-width="220" show-overflow-tooltip />
              <el-table-column label="覆盖状态" width="100" align="center">
                <template #default="{ row }">
                  <el-tag v-if="row.perms" size="small" :type="isPermMissingFromDb(row.perms) ? 'warning' : 'success'">
                    {{ isPermMissingFromDb(row.perms) ? '仅代码' : '已注册' }}
                  </el-tag>
                  <span v-else>-</span>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="150" fixed="right">
                <template #default="{ row }">
                  <span class="action-link" @click="handleEditMenu(row)">编辑</span>
                  <span v-if="row.id !== 1" class="action-link action-danger" @click="handleDeleteMenu(row)">删除</span>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <el-dialog
      :title="roleDialogTitle"
      v-model="roleDialogVisible"
      width="560px"
      :close-on-click-modal="false"
    >
      <el-form ref="roleFormRef" :model="roleFormData" :rules="roleFormRules" label-width="100px">
        <el-form-item label="角色编码" prop="code">
          <el-input v-model="roleFormData.code" placeholder="请输入角色编码" :disabled="isEditRole" maxlength="100" />
        </el-form-item>
        <el-form-item label="角色名称" prop="name">
          <el-input v-model="roleFormData.name" placeholder="请输入角色名称" maxlength="30" />
        </el-form-item>
        <el-form-item label="数据范围" prop="dataScope">
          <el-select v-model="roleFormData.dataScope" placeholder="请选择数据范围" style="width: 100%">
            <el-option v-for="item in dataScopeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="roleFormData.sortOrder" :min="0" :max="999" controls-position="right" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="roleFormData.status">
            <el-radio :label="0">正常</el-radio>
            <el-radio :label="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="角色说明" prop="description">
          <el-input
            v-model="roleFormData.description"
            type="textarea"
            :rows="3"
            placeholder="请输入角色说明"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleRoleSubmit" :loading="roleSubmitLoading">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog
      :title="`权限配置[${currentRole?.name || ''}]`"
      v-model="permDialogVisible"
      width="760px"
      :close-on-click-modal="false"
    >
      <div class="perm-config">
        <div class="perm-role-info">当前角色：{{ currentRole?.name || '-' }}</div>
        <el-tabs v-model="permActiveTab">
          <el-tab-pane label="菜单权限" name="menu">
            <el-tree
              ref="permMenuTreeRef"
              :data="permMenuTreeData"
              node-key="id"
              show-checkbox
              default-expand-all
              :props="{ label: 'name', children: 'children' }"
            />
          </el-tab-pane>
          <el-tab-pane label="数据权限" name="data">
            <el-form label-width="100px">
              <el-form-item label="数据范围">
                <el-select v-model="permDataScope" placeholder="请选择数据范围" style="width: 100%">
                  <el-option v-for="item in dataScopeOptions" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </el-form-item>
              <el-form-item v-if="permDataScope === 2" label="自定义组织">
                <el-tree
                  ref="permDeptTreeRef"
                  :data="permDeptTreeData"
                  node-key="id"
                  show-checkbox
                  default-expand-all
                  :props="{ label: 'label', children: 'children' }"
                />
              </el-form-item>
            </el-form>
          </el-tab-pane>
        </el-tabs>
      </div>
      <template #footer>
        <el-button v-if="coverage?.missingInDb.length" type="warning" @click="handleBatchRegister" :loading="registerLoading">
          注册缺失权限 ({{ coverage.missingInDb.length }})
        </el-button>
        <el-button @click="permDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handlePermSubmit" :loading="permSubmitLoading">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog
      :title="menuDialogTitle"
      v-model="menuDialogVisible"
      width="620px"
      :close-on-click-modal="false"
    >
      <el-form ref="menuFormRef" :model="menuFormData" :rules="menuFormRules" label-width="100px">
        <el-form-item label="上级菜单" prop="parentId">
          <el-tree-select
            v-model="menuFormData.parentId"
            :data="menuParentOptions"
            :props="{ label: 'name', value: 'id', children: 'children' }"
            check-strictly
            :render-after-expand="false"
            placeholder="请选择上级菜单"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="菜单类型" prop="type">
          <el-select v-model="menuFormData.type" placeholder="请选择菜单类型" style="width: 100%">
            <el-option label="目录" :value="0" />
            <el-option label="菜单" :value="1" />
            <el-option label="按钮" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="菜单名称" prop="name">
          <el-input v-model="menuFormData.name" placeholder="请输入菜单名称" maxlength="50" />
        </el-form-item>
        <el-form-item label="菜单编码" prop="code">
          <el-input v-model="menuFormData.code" placeholder="请输入菜单编码" maxlength="100" />
        </el-form-item>
        <el-form-item v-if="menuFormData.type !== 2" label="路由路径" prop="path">
          <el-input v-model="menuFormData.path" placeholder="请输入路由路径" maxlength="200" />
        </el-form-item>
        <el-form-item v-if="menuFormData.type === 1" label="组件路径" prop="component">
          <el-input v-model="menuFormData.component" placeholder="请输入组件路径" maxlength="255" />
        </el-form-item>
        <el-form-item v-if="menuFormData.type === 2" label="权限标识" prop="perms">
          <el-input v-model="menuFormData.perms" placeholder="如 system:user:add" maxlength="100" />
        </el-form-item>
        <el-form-item v-if="menuFormData.type !== 2" label="菜单图标" prop="icon">
          <el-input v-model="menuFormData.icon" placeholder="请输入图标名称" maxlength="100" />
        </el-form-item>
        <el-form-item label="显示状态" prop="visible">
          <el-radio-group v-model="menuFormData.visible">
            <el-radio :label="0">显示</el-radio>
            <el-radio :label="1">隐藏</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="menuFormData.type === 1" label="缓存" prop="isCache">
          <el-radio-group v-model="menuFormData.isCache">
            <el-radio :label="0">缓存</el-radio>
            <el-radio :label="1">不缓存</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="menuFormData.sortOrder" :min="0" :max="999" controls-position="right" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="menuDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleMenuSubmit" :loading="menuSubmitLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  batchRegisterPermissions,
  createMenu,
  createRole,
  deleteMenu,
  deleteRole,
  getMenuDetail,
  getMenuTree,
  getPermissionCoverage,
  getRoleDeptTree,
  getRoleDetail,
  getRolePage,
  saveRoleDataScope,
  toggleRoleStatus,
  updateMenu,
  updateRole,
  type MenuItem,
  type PermissionCoverage,
  type RoleItem,
  type TreeOption
} from '@/api/system'

const activeTab = ref('role')
const permActiveTab = ref('menu')

const dataScopeOptions = [
  { label: '全部数据', value: 1 },
  { label: '自定义数据', value: 2 },
  { label: '本组织数据', value: 3 },
  { label: '本组织及下级', value: 4 },
  { label: '仅本人数据', value: 5 }
]

const getDataScopeLabel = (scope?: number) =>
  dataScopeOptions.find((item) => item.value === scope)?.label || '-'

const getRoleStatusLabel = (status?: number) => (status === 1 ? '停用' : '正常')
const getRoleStatusType = (status?: number) => (status === 1 ? 'danger' : 'success')

const getMenuTypeLabel = (type?: number) => {
  const map: Record<number, string> = { 0: '目录', 1: '菜单', 2: '按钮' }
  return type === undefined ? '-' : map[type] || '-'
}

const getMenuTypeTag = (type?: number) => {
  const map: Record<number, string> = { 0: 'warning', 1: 'primary', 2: 'success' }
  return type === undefined ? 'info' : map[type] || 'info'
}

const roleLoading = ref(false)
const roleList = ref<RoleItem[]>([])
const roleSearchForm = reactive({
  code: '',
  name: '',
  status: undefined as number | undefined
})
const rolePagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const loadRoles = async () => {
  roleLoading.value = true
  try {
    const data = await getRolePage({
      pageNum: rolePagination.pageNum,
      pageSize: rolePagination.pageSize,
      code: roleSearchForm.code || undefined,
      name: roleSearchForm.name || undefined,
      status: roleSearchForm.status
    })
    roleList.value = data.rows
    rolePagination.total = data.total
  } finally {
    roleLoading.value = false
  }
}

const handleRoleSearch = async () => {
  rolePagination.pageNum = 1
  await loadRoles()
}

const handleRoleReset = async () => {
  roleSearchForm.code = ''
  roleSearchForm.name = ''
  roleSearchForm.status = undefined
  rolePagination.pageNum = 1
  await loadRoles()
}

const handleRoleSizeChange = async (size: number) => {
  rolePagination.pageSize = size
  rolePagination.pageNum = 1
  await loadRoles()
}

const handleRolePageChange = async (page: number) => {
  rolePagination.pageNum = page
  await loadRoles()
}

const roleDialogVisible = ref(false)
const roleDialogTitle = ref('新增角色')
const roleSubmitLoading = ref(false)
const roleFormRef = ref<FormInstance>()
const isEditRole = ref(false)
const roleFormData = reactive({
  id: 0,
  code: '',
  name: '',
  description: '',
  dataScope: 4,
  sortOrder: 0,
  status: 0,
  menuIds: [] as number[]
})

const roleFormRules: FormRules = {
  code: [{ required: true, message: '请输入角色编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  dataScope: [{ required: true, message: '请选择数据范围', trigger: 'change' }],
  sortOrder: [{ required: true, message: '请输入排序', trigger: 'change' }]
}

const resetRoleForm = () => {
  roleFormData.id = 0
  roleFormData.code = ''
  roleFormData.name = ''
  roleFormData.description = ''
  roleFormData.dataScope = 4
  roleFormData.sortOrder = 0
  roleFormData.status = 0
  roleFormData.menuIds = []
  roleFormRef.value?.clearValidate()
}

const handleAddRole = () => {
  isEditRole.value = false
  roleDialogTitle.value = '新增角色'
  resetRoleForm()
  roleDialogVisible.value = true
}

const handleEditRole = async (row: RoleItem) => {
  isEditRole.value = true
  roleDialogTitle.value = '编辑角色'
  resetRoleForm()
  const detail = await getRoleDetail(row.id)
  Object.assign(roleFormData, {
    id: detail.id,
    code: detail.code,
    name: detail.name,
    description: detail.description || '',
    dataScope: detail.dataScope ?? 4,
    sortOrder: detail.sortOrder ?? 0,
    status: detail.status ?? 0,
    menuIds: detail.menuIds || []
  })
  roleDialogVisible.value = true
}

const handleRoleSubmit = async () => {
  if (!roleFormRef.value) return
  await roleFormRef.value.validate()
  roleSubmitLoading.value = true
  try {
    const payload = {
      code: roleFormData.code,
      name: roleFormData.name,
      description: roleFormData.description,
      dataScope: roleFormData.dataScope,
      sortOrder: roleFormData.sortOrder,
      status: roleFormData.status,
      menuIds: roleFormData.menuIds
    }
    if (isEditRole.value && roleFormData.id) {
      await updateRole(roleFormData.id, payload)
      ElMessage.success('修改成功')
    } else {
      await createRole(payload)
      ElMessage.success('新增成功')
    }
    roleDialogVisible.value = false
    await loadRoles()
  } finally {
    roleSubmitLoading.value = false
  }
}

const handleToggleRoleStatus = async (row: RoleItem) => {
  const nextStatus = row.status === 1 ? 0 : 1
  await toggleRoleStatus(row.id, { status: String(nextStatus) })
  ElMessage.success(nextStatus === 1 ? '停用成功' : '启用成功')
  await loadRoles()
}

const handleRoleMoreCommand = (command: string, row: RoleItem) => {
  if (command === 'delete') {
    handleDeleteRole(row)
  } else if (command.startsWith('toggle_')) {
    handleToggleRoleStatus(row)
  }
}

const handleDeleteRole = async (row: RoleItem) => {
  await ElMessageBox.confirm(`确定要删除角色 "${row.name}" 吗？`, '系统提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteRole(row.id)
  ElMessage.success('删除成功')
  await loadRoles()
}

const permDialogVisible = ref(false)
const permSubmitLoading = ref(false)
const currentRole = ref<RoleItem | null>(null)
const permRoleDetail = ref<RoleItem | null>(null)
const permDataScope = ref(4)
const permMenuTreeData = ref<MenuItem[]>([])
const permDeptTreeData = ref<TreeOption[]>([])
const checkedMenuKeys = ref<number[]>([])
const checkedDeptKeys = ref<number[]>([])
const permMenuTreeRef = ref<any>()
const permDeptTreeRef = ref<any>()

const setTreeCheckedKeys = (treeRef: any, keys: number[]) => {
  if (!treeRef?.setCheckedKeys) return
  treeRef.setCheckedKeys(keys)
}

const collectTreeKeys = (treeRef: any) => {
  if (!treeRef) return []
  return Array.from(
    new Set<number>([
      ...((treeRef.getCheckedKeys?.(false) || []) as number[]),
      ...((treeRef.getHalfCheckedKeys?.() || []) as number[])
    ])
  )
}

const handleConfigPermission = async (row: RoleItem) => {
  currentRole.value = row
  permActiveTab.value = 'menu'
  const [detail, menuTree, deptTree] = await Promise.all([
    getRoleDetail(row.id),
    getMenuTree(),
    getRoleDeptTree(row.id)
  ])
  permRoleDetail.value = detail
  permDataScope.value = detail.dataScope ?? 4
  permMenuTreeData.value = menuTree
  permDeptTreeData.value = deptTree.depts
  checkedMenuKeys.value = detail.menuIds || []
  checkedDeptKeys.value = deptTree.checkedKeys || []
  permDialogVisible.value = true
  await nextTick()
  setTreeCheckedKeys(permMenuTreeRef.value, checkedMenuKeys.value)
  setTreeCheckedKeys(permDeptTreeRef.value, checkedDeptKeys.value)
}

const registerLoading = ref(false)

const handleBatchRegister = async () => {
  if (!coverage.value?.missingInDb.length) return
  registerLoading.value = true
  try {
    const res = await batchRegisterPermissions(coverage.value.missingInDb)
    ElMessage.success(res.data ?? res.msg ?? '注册成功')
    await loadMenus()
  } catch {
    ElMessage.error('注册失败')
  } finally {
    registerLoading.value = false
  }
}

const handlePermSubmit = async () => {
  if (!currentRole.value || !permRoleDetail.value) return
  permSubmitLoading.value = true
  try {
    const menuIds = collectTreeKeys(permMenuTreeRef.value)
    const deptIds = permDataScope.value === 2 ? collectTreeKeys(permDeptTreeRef.value) : []
    await updateRole(currentRole.value.id, {
      name: permRoleDetail.value.name,
      description: permRoleDetail.value.description,
      dataScope: permDataScope.value,
      sortOrder: permRoleDetail.value.sortOrder,
      status: permRoleDetail.value.status,
      menuIds
    })
    await saveRoleDataScope(currentRole.value.id, {
      dataScope: permDataScope.value,
      deptIds
    })
    permDialogVisible.value = false
    ElMessage.success('权限配置保存成功')
    await loadRoles()
  } finally {
    permSubmitLoading.value = false
  }
}

const menuLoading = ref(false)
const menuList = ref<MenuItem[]>([])
const coverage = ref<PermissionCoverage | null>(null)

const loadMenus = async () => {
  menuLoading.value = true
  try {
    const [tree, cov] = await Promise.all([getMenuTree(), getPermissionCoverage()])
    menuList.value = tree
    coverage.value = cov
  } finally {
    menuLoading.value = false
  }
}

const isPermMissingFromDb = (perms?: string) =>
  perms && coverage.value ? coverage.value.missingInDb.includes(perms) : false

const menuParentOptions = computed(() => [
  {
    id: 0,
    name: '顶级菜单',
    children: menuList.value
  }
])

const menuDialogVisible = ref(false)
const menuDialogTitle = ref('新增菜单')
const menuSubmitLoading = ref(false)
const menuFormRef = ref<FormInstance>()
const isEditMenu = ref(false)
const menuFormData = reactive({
  id: 0,
  parentId: 0,
  name: '',
  code: '',
  path: '',
  component: '',
  icon: '',
  type: 1,
  visible: 0,
  isCache: 0,
  sortOrder: 0,
  perms: '',
  status: 0
})

const validatePath = (_rule: any, value: string, callback: (error?: Error) => void) => {
  if (menuFormData.type !== 2 && !value) {
    callback(new Error('请输入路由路径'))
    return
  }
  callback()
}

const validateComponent = (_rule: any, value: string, callback: (error?: Error) => void) => {
  if (menuFormData.type === 1 && !value) {
    callback(new Error('请输入组件路径'))
    return
  }
  callback()
}

const validatePerms = (_rule: any, value: string, callback: (error?: Error) => void) => {
  if (menuFormData.type === 2 && !value) {
    callback(new Error('请输入权限标识'))
    return
  }
  callback()
}

const menuFormRules: FormRules = {
  parentId: [{ required: true, message: '请选择上级菜单', trigger: 'change' }],
  type: [{ required: true, message: '请选择菜单类型', trigger: 'change' }],
  name: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
  path: [{ validator: validatePath, trigger: 'blur' }],
  component: [{ validator: validateComponent, trigger: 'blur' }],
  perms: [{ validator: validatePerms, trigger: 'blur' }],
  sortOrder: [{ required: true, message: '请输入排序', trigger: 'change' }]
}

const resetMenuForm = () => {
  menuFormData.id = 0
  menuFormData.parentId = 0
  menuFormData.name = ''
  menuFormData.code = ''
  menuFormData.path = ''
  menuFormData.component = ''
  menuFormData.icon = ''
  menuFormData.type = 1
  menuFormData.visible = 0
  menuFormData.isCache = 0
  menuFormData.sortOrder = 0
  menuFormData.perms = ''
  menuFormData.status = 0
  menuFormRef.value?.clearValidate()
}

const handleAddMenu = () => {
  isEditMenu.value = false
  menuDialogTitle.value = '新增菜单'
  resetMenuForm()
  menuDialogVisible.value = true
}

const handleEditMenu = async (row: MenuItem) => {
  isEditMenu.value = true
  menuDialogTitle.value = '编辑菜单'
  resetMenuForm()
  const detail = await getMenuDetail(row.id)
  Object.assign(menuFormData, {
    id: detail.id,
    parentId: detail.parentId ?? 0,
    name: detail.name,
    code: detail.code || '',
    path: detail.path || '',
    component: detail.component || '',
    icon: detail.icon || '',
    type: detail.type ?? 1,
    visible: detail.visible ?? 0,
    isCache: detail.isCache ?? 1,
    sortOrder: detail.sortOrder ?? 0,
    perms: detail.perms || '',
    status: detail.status ?? 0
  })
  menuDialogVisible.value = true
}

const handleDeleteMenu = async (row: MenuItem) => {
  await ElMessageBox.confirm(`确定要删除菜单 "${row.name}" 吗？`, '系统提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteMenu(row.id)
  ElMessage.success('删除成功')
  await loadMenus()
}

const handleMenuSubmit = async () => {
  if (!menuFormRef.value) return
  await menuFormRef.value.validate()
  menuSubmitLoading.value = true
  try {
    const payload = {
      parentId: menuFormData.parentId,
      name: menuFormData.name,
      code: menuFormData.code || undefined,
      path: menuFormData.type === 2 ? undefined : menuFormData.path,
      component: menuFormData.type === 1 ? menuFormData.component : undefined,
      icon: menuFormData.type === 2 ? undefined : menuFormData.icon || undefined,
      type: menuFormData.type,
      visible: menuFormData.visible,
      isCache: menuFormData.type === 1 ? menuFormData.isCache : undefined,
      sortOrder: menuFormData.sortOrder,
      perms: menuFormData.type === 2 ? menuFormData.perms : menuFormData.perms || undefined,
      status: menuFormData.status
    }
    if (isEditMenu.value && menuFormData.id) {
      await updateMenu(menuFormData.id, payload)
      ElMessage.success('修改成功')
    } else {
      await createMenu(payload)
      ElMessage.success('新增成功')
    }
    menuDialogVisible.value = false
    await loadMenus()
  } finally {
    menuSubmitLoading.value = false
  }
}

onMounted(async () => {
  await Promise.all([loadRoles(), loadMenus()])
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
  font-weight: bold;
  color: #303133;
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 1px solid #e8e8e8;
}

.page-body {
  padding: 0;
}

.tab-content {
  padding: 16px 0;
}

.search-bar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 10px;
}

.toolbar {
  display: flex;
  gap: 10px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.perm-config {
  min-height: 420px;
}

.perm-role-info {
  margin-bottom: 16px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 4px;
  color: #606266;
}

:deep(.el-form--inline .el-form-item) {
  margin-right: 16px;
  margin-bottom: 10px;
}

.action-link {
  display: inline-block;
  padding: 4px 10px;
  margin: 0 4px;
  color: #303133;
  font-size: 13px;
  cursor: pointer;
  transition: color 0.2s ease;
}

.action-link:hover {
  color: #1890ff;
}

.action-link.action-warning {
  color: #faad14;
}

.action-link.action-warning:hover {
  color: #d48806;
}

.action-link.action-success {
  color: #52c41a;
}

.action-link.action-success:hover {
  color: #389e0d;
}

.action-link.action-danger {
  color: #f5222d;
}

.action-link.action-danger:hover {
  color: #cf1322;
}
</style>
