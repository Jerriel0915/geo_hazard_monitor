<template>
  <div class="page">
    <div class="header">
      <div class="header__left">
        <h2 class="header__title">权限管理</h2>
        <span class="header__subtitle">角色管理与菜单权限配置</span>
      </div>
    </div>

    <el-tabs v-model="activeTab" class="perm-tabs">
      <el-tab-pane label="角色管理" name="role">
        <div class="perm-tab-content">
          <div class="search">
            <el-input v-model="roleSearchForm.code" placeholder="角色编码" clearable />
            <el-input v-model="roleSearchForm.name" placeholder="角色名称" clearable />
            <el-select v-model="roleSearchForm.status" placeholder="状态" clearable>
              <el-option label="正常" :value="0" />
              <el-option label="停用" :value="1" />
            </el-select>
            <el-button type="primary" @click="handleRoleSearch">查询</el-button>
            <el-button @click="handleRoleReset">重置</el-button>
            <el-button type="primary" @click="handleAddRole">新增角色</el-button>
          </div>

          <div class="table-wrap">
            <div class="table-wrap__scroll">
              <el-table :data="roleList" border stripe v-loading="roleLoading">
                <el-table-column type="index" label="序号" width="60" align="center" />
                <el-table-column prop="name" label="角色名称" min-width="140" />
                <el-table-column prop="code" label="角色编码" min-width="140" />
                <el-table-column prop="dataScope" label="数据范围" min-width="120">
                  <template #default="{ row }">
                    {{ getDataScopeLabel(row.dataScope) }}
                  </template>
                </el-table-column>
                <el-table-column prop="status" label="状态" width="90" align="center">
                  <template #default="{ row }">
                    <el-tag :type="getRoleStatusType(row.status)">{{ getRoleStatusLabel(row.status) }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="菜单权限数" width="100" align="center">
                  <template #default="{ row }">
                    {{ row.menuIds?.length ?? '-' }}
                  </template>
                </el-table-column>
                <el-table-column prop="description" label="角色说明" min-width="200" show-overflow-tooltip />
                <el-table-column prop="createTime" label="创建时间" min-width="170" />
                <el-table-column label="操作" width="220" fixed="right">
                  <template #default="{ row }">
                    <div v-if="row.id === 1" class="super-admin-tag">超级管理员</div>
                    <div v-else class="op-cell">
                      <el-button type="primary" text size="small" @click="handleEditRole(row)">编辑</el-button>
                      <el-button type="primary" text size="small" @click="handleConfigPermission(row)">权限配置</el-button>
                      <el-dropdown trigger="hover" @command="(cmd: string) => handleRoleMoreCommand(cmd, row)">
                        <el-button type="primary" text size="small">更多</el-button>
                        <template #dropdown>
                          <el-dropdown-menu>
                            <el-dropdown-item :command="'toggle_' + row.id">
                              {{ row.status === 0 ? '停用' : '启用' }}
                            </el-dropdown-item>
                            <el-dropdown-item command="delete" divided>
                              <span style="color: #f56c6c">删除</span>
                            </el-dropdown-item>
                          </el-dropdown-menu>
                        </template>
                      </el-dropdown>
                    </div>
                  </template>
                </el-table-column>
              </el-table>
            </div>

            <div class="table-wrap__pagination">
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
        </div>
      </el-tab-pane>

      <el-tab-pane label="菜单管理" name="menu">
        <div class="perm-tab-content">
          <div class="search">
            <el-button @click="loadMenus">刷新</el-button>
            <el-button type="primary" @click="handleAddMenu">新增菜单</el-button>
          </div>

          <div class="table-wrap">
            <div class="menu-tree" v-loading="menuLoading || menuSaving">
              <div class="menu-tree__header">
                <div class="menu-tree__cell menu-tree__cell--name">菜单名称</div>
                <div class="menu-tree__cell menu-tree__cell--code">菜单编码</div>
                <div class="menu-tree__cell menu-tree__cell--path">路由路径</div>
                <div class="menu-tree__cell menu-tree__cell--type">类型</div>
                <div class="menu-tree__cell menu-tree__cell--visible">显示</div>
                <div class="menu-tree__cell menu-tree__cell--sort">排序</div>
                <div class="menu-tree__cell menu-tree__cell--action">操作</div>
              </div>
              <el-tree
                ref="menuTreeRef"
                :data="menuList"
                node-key="id"
                :draggable="!menuSaving"
                :allow-drop="allowMenuDrop"
                @node-drop="handleNodeDrop"
                default-expand-all
                :props="{ label: 'name', children: 'children' }"
                :expand-on-click-node="false"
                empty-text="暂无菜单"
                class="menu-tree__body"
              >
                <template #default="{ node, data }">
                  <div class="menu-tree__row">
                    <div class="menu-tree__cell menu-tree__cell--name" :title="data.name">
                      <el-icon v-if="data.icon" class="menu-icon"><component :is="data.icon" /></el-icon>
                      <span class="menu-tree__name-text">{{ data.name }}</span>
                    </div>
                    <div class="menu-tree__cell menu-tree__cell--code" :title="data.code || ''">{{ data.code || '-' }}</div>
                    <div class="menu-tree__cell menu-tree__cell--path" :title="data.path || ''">{{ data.path || '-' }}</div>
                    <div class="menu-tree__cell menu-tree__cell--type">
                      <el-tag size="small" :type="getMenuTypeTag(data.type)">{{ getMenuTypeLabel(data.type) }}</el-tag>
                    </div>
                    <div class="menu-tree__cell menu-tree__cell--visible">
                      <el-tag size="small" :type="data.visible === 0 ? 'success' : 'info'">
                        {{ data.visible === 0 ? '显示' : '隐藏' }}
                      </el-tag>
                    </div>
                    <div class="menu-tree__cell menu-tree__cell--sort">{{ data.sortOrder ?? 0 }}</div>
                    <div class="menu-tree__cell menu-tree__cell--action">
                      <el-button type="primary" text size="small" @click="handleEditMenu(data)">编辑</el-button>
                      <el-button
                        type="danger"
                        text
                        size="small"
                        :disabled="hasChildren(data)"
                        :title="hasChildren(data) ? '请先删除子菜单' : ''"
                        @click="handleDeleteMenu(data)"
                      >删除</el-button>
                    </div>
                  </div>
                </template>
              </el-tree>
            </div>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

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
      destroy-on-close
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
              :default-checked-keys="checkedMenuKeys"
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
                  :default-checked-keys="checkedDeptKeys"
                  :props="{ label: 'label', children: 'children' }"
                />
              </el-form-item>
            </el-form>
          </el-tab-pane>
        </el-tabs>
      </div>
      <template #footer>
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
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="菜单类型" prop="type">
              <el-select v-model="menuFormData.type" placeholder="请选择" style="width: 100%">
                <el-option label="目录" :value="0" />
                <el-option label="菜单" :value="1" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序" prop="sortOrder">
              <el-input-number v-model="menuFormData.sortOrder" :min="0" :max="999" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="菜单名称" prop="name">
          <el-input v-model="menuFormData.name" placeholder="请输入菜单名称" maxlength="50" style="width: 100%" />
        </el-form-item>
        <el-form-item label="菜单编码" prop="code">
          <el-input v-model="menuFormData.code" placeholder="路由名称，与前端路由 name 一致，如 Device" maxlength="100" style="width: 100%" />
        </el-form-item>
        <el-form-item label="路由路径" prop="path">
          <el-input v-model="menuFormData.path" placeholder="如 basic/device" maxlength="200" style="width: 100%" />
        </el-form-item>
        <el-form-item v-if="menuFormData.type === 1" label="组件路径" prop="component">
          <el-input v-model="menuFormData.component" placeholder="如 basic/Device" maxlength="255" style="width: 100%" />
        </el-form-item>
        <el-form-item label="菜单图标" prop="icon">
          <div class="icon-picker">
            <el-select v-model="menuFormData.icon" placeholder="请选择图标" clearable filterable style="flex: 1">
              <el-option v-for="ic in availableIcons" :key="ic" :label="(iconLabelMap[ic] || ic) + ' ' + ic" :value="ic">
                <span class="icon-option">
                  <span class="icon-option__svg" v-html="getMenuIconSvg(ic)"></span>
                  <span>{{ iconLabelMap[ic] || ic }}</span>
                  <span class="icon-option__id">{{ ic }}</span>
                </span>
              </el-option>
            </el-select>
            <span v-if="menuFormData.icon" class="icon-preview" v-html="getMenuIconSvg(menuFormData.icon)"></span>
          </div>
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="显示状态" prop="visible">
              <el-radio-group v-model="menuFormData.visible">
                <el-radio :label="0">显示</el-radio>
                <el-radio :label="1">隐藏</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item v-if="menuFormData.type === 1" label="缓存" prop="isCache">
              <el-radio-group v-model="menuFormData.isCache">
                <el-radio :label="0">缓存</el-radio>
                <el-radio :label="1">不缓存</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="menuDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleMenuSubmit" :loading="menuSubmitLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import {computed, nextTick, onMounted, reactive, ref} from 'vue'
import type {FormInstance, FormRules} from 'element-plus'
import {ElMessage, ElMessageBox} from 'element-plus'
import {
  createMenu,
  createRole,
  deleteMenu,
  deleteRole,
  getMenuDetail,
  getMenuTree,
  getRoleDeptTree,
  getRoleDetail,
  getRolePage,
  type MenuItem,
  type MenuReorderItem,
  type RoleItem,
  reorderMenus,
  saveRoleDataScope,
  toggleRoleStatus,
  type TreeOption,
  updateMenu,
  updateRole
} from '@/api/system'
import { availableIcons, iconLabelMap, getMenuIconSvg } from '@/utils/menuIcon'

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

const collectTreeKeys = (treeRef: any) => {
  if (!treeRef) return []
  return ((treeRef.getCheckedKeys?.(false) || []) as number[])
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
    try {
      await saveRoleDataScope(currentRole.value.id, {
        dataScope: permDataScope.value,
        deptIds
      })
    } catch {
      ElMessage.warning('菜单权限已保存，但数据权限保存失败，请重试')
      return
    }
    permDialogVisible.value = false
    ElMessage.success('权限配置保存成功')
    await loadRoles()
  } catch {
    ElMessage.error('权限配置保存失败')
  } finally {
    permSubmitLoading.value = false
  }
}

const menuLoading = ref(false)
const menuSaving = ref(false)
const menuList = ref<MenuItem[]>([])
const menuTreeRef = ref<any>(null)

/** 递归过滤菜单树，仅保留目录(M=0)和菜单(C=1)类型 */
function filterMenuTree(menus: MenuItem[]): MenuItem[] {
  return menus
    .filter(item => item.type !== 2)
    .map(item => ({
      ...item,
      children: item.children?.length ? filterMenuTree(item.children) : []
    }))
}

const hasChildren = (row: MenuItem) =>
  Array.isArray(row.children) && row.children.length > 0

const loadMenus = async () => {
  menuLoading.value = true
  try {
    const raw = await getMenuTree()
    menuList.value = filterMenuTree(raw)
  } finally {
    menuLoading.value = false
    await nextTick()
    menuTreeRef.value?.expandAll?.(true)
  }
}

/** 从树中按 id 找到并移除该节点，返回被移除的节点引用 */
const findAndRemove = (list: MenuItem[], id: number): MenuItem | null => {
  for (let i = 0; i < list.length; i++) {
    if (list[i].id === id) {
      return list.splice(i, 1)[0] ?? null
    }
    const children = list[i].children
    if (children?.length) {
      const found = findAndRemove(children, id)
      if (found) return found
    }
  }
  return null
}

/** 取得某 parentId 下的子节点数组（root 时返回整个顶层列表） */
const findChildrenOf = (list: MenuItem[], parentId: number): MenuItem[] | null => {
  if (parentId === 0) return list
  for (const item of list) {
    if (item.id === parentId) return item.children ?? []
    const children = item.children
    if (children?.length) {
      const found = findChildrenOf(children, parentId)
      if (found) return found
    }
  }
  return null
}

/**
 * 在目标兄弟列表里找与 dragging 冲突的菜单（path 或 code 相同）
 * 对应后端 SysMenuServiceImpl.checkRouteConfigUnique 的两条规则：
 *   - path 同 parentId 唯一
 *   - code 全局唯一
 */
const findMenuConflict = (dragging: MenuItem, siblings: MenuItem[]): MenuItem | null =>
  siblings.find(s =>
    s.id !== dragging.id && (
      (s.path && dragging.path && s.path === dragging.path) ||
      (s.code && dragging.code && s.code === dragging.code)
    )
  ) ?? null

const describeConflict = (dragging: MenuItem, conflict: MenuItem): { field: string; value: string } => {
  if (conflict.path && dragging.path && conflict.path === dragging.path) {
    return { field: '路径', value: conflict.path }
  }
  return { field: '路由编码', value: conflict.code ?? '' }
}

/** 拖动前置校验：① 防成环；② 防 path/code 冲突 */
const allowMenuDrop = (draggingNode: any, dropNode: any, type: 'prev' | 'inner' | 'next') => {
  // 1) 防成环：不能把节点拖入自己的后代
  if (type === 'inner') {
    let cur = dropNode.parent
    while (cur) {
      if (cur.data?.id === draggingNode.data?.id) return false
      cur = cur.parent
    }
  }

  // 2) 防冲突：确定"目标位置的兄弟列表"和"目标位置名"
  const dragging = draggingNode.data as MenuItem
  let targetSiblings: MenuItem[]
  let targetLabel: string
  if (type === 'inner') {
    const drop = dropNode.data as MenuItem
    targetSiblings = drop.children ?? []
    targetLabel = drop.name
  } else {
    const parentNode = dropNode.parent
    if (parentNode) {
      targetSiblings = parentNode.data.children ?? []
      targetLabel = parentNode.data.name
    } else {
      targetSiblings = menuList.value
      targetLabel = '顶级'
    }
  }

  const conflict = findMenuConflict(dragging, targetSiblings)
  if (conflict) {
    const { field, value } = describeConflict(dragging, conflict)
    ElMessage.warning(
      `目标"${targetLabel}"下已存在${field}为"${value}"的菜单"${conflict.name}"，无法移动`
    )
    return false
  }
  return true
}

const handleNodeDrop = async (
  draggingNode: any,
  dropNode: any,
  dropType: 'before' | 'after' | 'inner'
) => {
  menuSaving.value = true
  try {
    const dragging = draggingNode.data as MenuItem
    const drop = dropNode.data as MenuItem
    const newParentId = dropType === 'inner' ? drop.id : drop.parentId
    const oldParentId = dragging.parentId

    // 1) 本地先 splice 到新位置，给用户即时反馈
    const removed = findAndRemove(menuList.value, dragging.id)
    if (!removed) return

    let newSiblingList: MenuItem[]
    let newIndex: number
    if (dropType === 'inner') {
      if (!drop.children) drop.children = []
      drop.children.push(removed)
      newSiblingList = drop.children
      newIndex = newSiblingList.length - 1
    } else {
      const siblings = findChildrenOf(menuList.value, newParentId) ?? []
      const dropIndex = siblings.findIndex(s => s.id === drop.id)
      newIndex = dropType === 'before' ? dropIndex : dropIndex + 1
      siblings.splice(newIndex, 0, removed)
      newSiblingList = siblings
    }
    removed.parentId = newParentId

    // 2) 防御兜底：allow-drop 漏判时，这里再查一次（命中即回滚）
    const conflict = findMenuConflict(removed, newSiblingList)
    if (conflict) {
      const { field, value } = describeConflict(removed, conflict)
      const parentName = dropType === 'inner'
        ? drop.name
        : (drop.parentId === 0 ? '顶级' : '当前层级')
      ElMessage.error(
        `目标"${parentName}"下已存在${field}为"${value}"的菜单"${conflict.name}"，移动已撤销`
      )
      return
    }

    // 3) 收集更新：只对 parentId 或 sortOrder 真正变化的节点发请求
    //    → 解决"同父内调位置"时频繁 N 次 PUT 的问题
    const updates: Array<{ menu: MenuItem; parentId: number; sortOrder: number }> = []
    if (oldParentId !== newParentId || (removed.sortOrder ?? 0) !== newIndex) {
      updates.push({ menu: removed, parentId: newParentId, sortOrder: newIndex })
    }
    newSiblingList.forEach((s, idx) => {
      if (s.id !== removed.id && (s.sortOrder ?? 0) !== idx) {
        updates.push({ menu: s, parentId: newParentId, sortOrder: idx })
      }
    })
    if (oldParentId !== newParentId) {
      const oldSiblings = findChildrenOf(menuList.value, oldParentId) ?? []
      oldSiblings.forEach((s, idx) => {
        if ((s.sortOrder ?? 0) !== idx) {
          updates.push({ menu: s, parentId: oldParentId, sortOrder: idx })
        }
      })
    }

    if (updates.length === 0) return

    // 4) 一次调用：后端单条 SQL 批量更新 + 事务内唯一性校验
    //    sortOrder (前端) → orderNum (后端) 字段名映射在请求边界完成
    const items: MenuReorderItem[] = updates.map(u => ({
      menuId: u.menu.id,
      parentId: u.parentId,
      orderNum: u.sortOrder
    }))
    await reorderMenus(items)
    ElMessage.success(`排序已保存（${items.length} 项）`)
  } catch (e) {
    const msg = (e as Error)?.message ?? '未知错误'
    if (msg.includes('路由名称或地址已存在')) {
      ElMessage.error('排序保存失败：目标位置存在路径或路由编码冲突')
    } else {
      ElMessage.error('排序保存失败：' + msg)
    }
  } finally {
    try {
      await loadMenus()
    } finally {
      menuSaving.value = false
    }
  }
}

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

const menuFormRules: FormRules = {
  parentId: [{ required: true, message: '请选择上级菜单', trigger: 'change' }],
  type: [{ required: true, message: '请选择菜单类型', trigger: 'change' }],
  name: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
  path: [{ validator: validatePath, trigger: 'blur' }],
  component: [{ validator: validateComponent, trigger: 'blur' }],
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
  ElMessage.success('删除成功，页面即将刷新...')
  await loadMenus()
  setTimeout(() => { window.location.reload() }, 600)
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
      path: menuFormData.path,
      component: menuFormData.type === 1 ? menuFormData.component : undefined,
      icon: menuFormData.icon || undefined,
      type: menuFormData.type,
      visible: menuFormData.visible,
      isCache: menuFormData.type === 1 ? menuFormData.isCache : undefined,
      sortOrder: menuFormData.sortOrder,
      status: menuFormData.status
    }
    if (isEditMenu.value && menuFormData.id) {
      await updateMenu(menuFormData.id, payload)
      ElMessage.success('修改成功，页面即将刷新...')
    } else {
      await createMenu(payload)
      ElMessage.success('新增成功，页面即将刷新...')
    }
    menuDialogVisible.value = false
    await loadMenus()
    setTimeout(() => { window.location.reload() }, 600)
  } finally {
    menuSubmitLoading.value = false
  }
}

onMounted(async () => {
  await Promise.all([loadRoles(), loadMenus()])
})
</script>

<style scoped>
.perm-tabs {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.perm-tabs :deep(.el-tabs__content) {
  flex: 1;
  min-height: 0;
}

.perm-tabs :deep(.el-tab-pane) {
  height: 100%;
}

.perm-tab-content {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.perm-tab-content .search {
  flex-shrink: 0;
}

/* ---------- 菜单管理：可拖动树 ---------- */
.menu-tree {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
  background: #fff;
}

.menu-tree__header,
.menu-tree__row {
  display: grid;
  grid-template-columns:
    minmax(180px, 1.2fr)
    minmax(120px, 0.8fr)
    minmax(180px, 1fr)
    80px
    80px
    60px
    150px;
  align-items: center;
  gap: 12px;
  padding: 0 16px;
}

.menu-tree__header {
  height: 44px;
  background: #fafbfc;
  border-bottom: 1px solid #ebeef5;
  font-weight: 600;
  font-size: 13px;
  color: #606266;
  padding-left: 40px; /* 留出 el-tree 展开图标区域，与内容对齐 */
  flex-shrink: 0;
}

.menu-tree__body {
  flex: 1;
  min-height: 0;
  overflow: auto;
}

.menu-tree__body :deep(.el-tree-node__content) {
  height: 48px;
  padding: 0 16px 0 0;
  border-bottom: 1px solid #f2f3f5;
  transition: background 0.15s;
}

.menu-tree__body :deep(.el-tree-node__content:hover) {
  background: #f5f7fa;
}

.menu-tree__body :deep(.el-tree-node.is-drop-inner > .el-tree-node__content) {
  background: #ecf5ff;
  outline: 2px dashed #409eff;
  outline-offset: -2px;
}

.menu-tree__body :deep(.el-tree-node.is-dragging > .el-tree-node__content) {
  opacity: 0.5;
}

.menu-tree__row {
  width: 100%;
  height: 48px;
  font-size: 13px;
  color: #303133;
  cursor: grab;
  user-select: none;
}

.menu-tree__row:active {
  cursor: grabbing;
}

.menu-tree__cell {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: flex;
  align-items: center;
  gap: 6px;
}

.menu-tree__name-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.menu-icon {
  color: #409eff;
  font-size: 14px;
  flex-shrink: 0;
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

.super-admin-tag {
  color: #909399;
  font-size: 13px;
}

.icon-picker {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
}

.icon-preview {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 6px;
  border: 1px solid #dcdfe6;
  background: #fff;
  color: #409eff;
}

.icon-preview :deep(svg) {
  width: 20px;
  height: 20px;
}

/* 图标下拉选项：左侧 SVG 预览 + 中文名 + 英文 ID */
.icon-option {
  display: flex;
  align-items: center;
  gap: 8px;
}

.icon-option__svg {
  display: flex;
  align-items: center;
  flex-shrink: 0;
  color: #409eff;
}

.icon-option__svg :deep(svg) {
  width: 16px;
  height: 16px;
}

.icon-option__id {
  margin-left: auto;
  color: #909399;
  font-size: 11px;
}
</style>
