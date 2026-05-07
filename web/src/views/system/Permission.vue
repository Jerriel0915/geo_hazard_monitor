<template>
  <div class="page-content">
    <div class="page-title">权限管理</div>
    <div class="page-body">
      <el-tabs v-model="activeTab" type="border-card">
        <!-- 角色管理 -->
        <el-tab-pane label="角色管理" name="role">
          <div class="tab-content">
            <div class="search-bar">
              <el-form :model="roleSearchForm" inline>
                <el-form-item label="角色名称">
                  <el-input v-model="roleSearchForm.name" placeholder="请输入角色名称" clearable />
                </el-form-item>
                <el-form-item label="状态">
                  <el-select v-model="roleSearchForm.status" placeholder="全部状态" clearable style="width: 120px">
                    <el-option label="启用" :value="1" />
                    <el-option label="禁用" :value="0" />
                  </el-select>
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="handleRoleSearch">查询</el-button>
                  <el-button @click="handleRoleReset">重置</el-button>
                </el-form-item>
              </el-form>
              <el-button type="primary" @click="handleAddRole">新增角色</el-button>
            </div>

            <el-table :data="roleList" border stripe>
              <el-table-column type="index" label="序号" width="60" align="center" />
              <el-table-column prop="name" label="角色名称" width="150" />
              <el-table-column prop="code" label="角色编码" width="150" />
              <el-table-column prop="dataScope" label="数据范围" width="150">
                <template #default="{ row }">
                  {{ getDataScopeLabel(row.dataScope) }}
                </template>
              </el-table-column>
              <el-table-column prop="status" label="状态" width="90" align="center">
                <template #default="{ row }">
                  <el-tag :type="row.status === 1 ? 'success' : 'danger'">
                    {{ row.status === 1 ? '启用' : '禁用' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="remark" label="备注" min-width="200" show-overflow-tooltip />
              <el-table-column prop="createTime" label="创建时间" width="160" />
              <el-table-column label="操作" width="220" fixed="right">
                <template #default="{ row }">
                  <span class="action-link" @click="handleEditRole(row)">编辑</span>
                  <span class="action-link" @click="handleConfigPermission(row)">权限配置</span>
                  <span :class="['action-link', row.status === 1 ? 'action-warning' : 'action-success']" @click="handleToggleRoleStatus(row)">
                    {{ row.status === 1 ? '禁用' : '启用' }}
                  </span>
                  <span v-if="row.id !== 1" class="action-link action-danger" @click="handleDeleteRole(row)">删除</span>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-tab-pane>

        <!-- 菜单管理 -->
        <el-tab-pane label="菜单管理" name="menu">
          <div class="tab-content">
            <div class="search-bar">
              <el-button type="primary" @click="handleAddMenu">新增菜单</el-button>
            </div>
            <el-table
              :data="menuList"
              border
              stripe
              row-key="id"
              default-expand-all
              :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
            >
              <el-table-column prop="name" label="菜单名称" min-width="180">
                <template #default="{ row }">
                  <span :style="{ paddingLeft: row.parentId === 0 ? '0' : '20px' }">{{ row.name }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="icon" label="图标" width="80" align="center">
                <template #default="{ row }">
                  <span v-if="row.icon" v-html="row.icon" class="menu-icon-cell"></span>
                  <span v-else>-</span>
                </template>
              </el-table-column>
              <el-table-column prop="path" label="路由路径" width="180" />
              <el-table-column prop="type" label="类型" width="90" align="center">
                <template #default="{ row }">
                  <el-tag :type="row.type === 'menu' ? 'primary' : 'warning'" size="small">
                    {{ row.type === 'menu' ? '菜单' : '按钮' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="sort" label="排序" width="80" align="center" />
              <el-table-column prop="status" label="状态" width="90" align="center">
                <template #default="{ row }">
                  <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
                    {{ row.status === 1 ? '启用' : '禁用' }}
                  </el-tag>
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

    <!-- 角色表单弹窗 -->
    <el-dialog
      :title="roleDialogTitle"
      v-model="roleDialogVisible"
      width="550px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="roleFormRef"
        :model="roleFormData"
        :rules="roleFormRules"
        label-width="100px"
      >
        <el-form-item label="角色名称" prop="name">
          <el-input v-model="roleFormData.name" placeholder="请输入角色名称" maxlength="50" />
        </el-form-item>
        <el-form-item label="角色编码" prop="code">
          <el-input v-model="roleFormData.code" placeholder="请输入角色编码" :disabled="isEditRole" maxlength="50" />
        </el-form-item>
        <el-form-item label="数据范围" prop="dataScope">
          <el-select v-model="roleFormData.dataScope" placeholder="请选择数据范围" style="width: 100%">
            <el-option label="全部数据" value="all" />
            <el-option label="本组织及下级" value="org_and_child" />
            <el-option label="仅本组织" value="org_only" />
            <el-option label="本人数据" value="self" />
            <el-option label="自定义" value="custom" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="roleFormData.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="roleFormData.remark" type="textarea" :rows="2" placeholder="请输入备注" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleRoleSubmit" :loading="roleSubmitLoading">确定</el-button>
      </template>
    </el-dialog>

    <!-- 权限配置弹窗 -->
    <el-dialog
      :title="`权限配置[${currentRole?.name || ''}]`"
      v-model="permDialogVisible"
      width="600px"
      :close-on-click-modal="false"
    >
      <div class="perm-config">
        <div class="perm-role-info">
          <span>当前角色：<strong>{{ currentRole?.name }}</strong></span>
        </div>
        <el-tabs v-model="permActiveTab">
          <el-tab-pane label="菜单权限" name="menu">
            <el-tree
              ref="permTreeRef"
              :data="menuTreeData"
              show-checkbox
              node-key="id"
              :props="{ label: 'name', children: 'children' }"
              :default-checked-keys="checkedMenuKeys"
              :check-strictly="false"
            />
          </el-tab-pane>
          <el-tab-pane label="数据权限" name="data">
            <el-form label-width="120px">
              <el-form-item label="数据范围">
                <el-select v-model="permDataScope" placeholder="请选择数据范围" style="width: 100%">
                  <el-option label="全部数据" value="all" />
                  <el-option label="本组织及下级" value="org_and_child" />
                  <el-option label="仅本组织" value="org_only" />
                  <el-option label="本人数据" value="self" />
                  <el-option label="自定义" value="custom" />
                </el-select>
              </el-form-item>
              <el-form-item label="自定义组织" v-if="permDataScope === 'custom'">
                <el-tree
                  :data="orgTreeData"
                  show-checkbox
                  node-key="id"
                  :props="{ label: 'name', children: 'children' }"
                  :default-checked-keys="checkedOrgKeys"
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

    <!-- 菜单表单弹窗 -->
    <el-dialog
      :title="menuDialogTitle"
      v-model="menuDialogVisible"
      width="550px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="menuFormRef"
        :model="menuFormData"
        :rules="menuFormRules"
        label-width="100px"
      >
        <el-form-item label="上级菜单" v-if="menuFormData.parentId !== 0">
          <el-input v-model="parentMenuName" disabled />
        </el-form-item>
        <el-form-item label="菜单类型" prop="type">
          <el-radio-group v-model="menuFormData.type">
            <el-radio label="menu">菜单</el-radio>
            <el-radio label="button">按钮</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="菜单名称" prop="name">
          <el-input v-model="menuFormData.name" placeholder="请输入菜单名称" maxlength="50" />
        </el-form-item>
        <el-form-item label="路由路径" prop="path" v-if="menuFormData.type === 'menu'">
          <el-input v-model="menuFormData.path" placeholder="请输入路由路径" maxlength="100" />
        </el-form-item>
        <el-form-item label="权限标识" prop="perms" v-if="menuFormData.type === 'button'">
          <el-input v-model="menuFormData.perms" placeholder="如: system:user:add" maxlength="100" />
        </el-form-item>
        <el-form-item label="图标" prop="icon" v-if="menuFormData.type === 'menu'">
          <el-input v-model="menuFormData.icon" placeholder="请输入SVG图标代码" maxlength="500" />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="menuFormData.sort" :min="0" :max="999" controls-position="right" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="menuFormData.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
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
import { ref, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'

interface Role {
  id: number
  name: string
  code: string
  dataScope: string
  status: number
  remark: string
  createTime: string
}

interface Menu {
  id: number
  name: string
  parentId: number
  path?: string
  icon?: string
  type: 'menu' | 'button'
  perms?: string
  sort: number
  status: number
  children?: Menu[]
}

interface OrgNode {
  id: number
  name: string
  children?: OrgNode[]
}

const activeTab = ref('role')
const permActiveTab = ref('menu')

// 角色相关
const roleSearchForm = reactive({ name: '', status: undefined as number | undefined })
const roleDialogVisible = ref(false)
const roleDialogTitle = ref('新增角色')
const roleSubmitLoading = ref(false)
const roleFormRef = ref<FormInstance>()
const isEditRole = ref(false)

const roleFormData = reactive({
  id: 0,
  name: '',
  code: '',
  dataScope: 'org_and_child',
  status: 1,
  remark: ''
})

const roleFormRules: FormRules = {
  name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入角色编码', trigger: 'blur' }],
  dataScope: [{ required: true, message: '请选择数据范围', trigger: 'change' }]
}

const allRoleList = ref<Role[]>([
  { id: 1, name: '超级管理员', code: 'super_admin', dataScope: 'all', status: 1, remark: '系统最高权限', createTime: '2024-01-01 10:00:00' },
  { id: 2, name: '管理员', code: 'admin', dataScope: 'org_and_child', status: 1, remark: '组织管理员', createTime: '2024-01-05 09:00:00' },
  { id: 3, name: '值班员', code: 'duty', dataScope: 'org_only', status: 1, remark: '值班监控人员', createTime: '2024-01-10 08:30:00' },
  { id: 4, name: '巡检员', code: 'inspector', dataScope: 'self', status: 1, remark: '现场巡检人员', createTime: '2024-01-12 10:00:00' },
  { id: 5, name: '只读用户', code: 'readonly', dataScope: 'self', status: 1, remark: '仅查看权限', createTime: '2024-01-15 11:00:00' }
])

const roleList = computed(() => {
  let result = allRoleList.value
  if (roleSearchForm.name) {
    result = result.filter(r => r.name.includes(roleSearchForm.name))
  }
  if (roleSearchForm.status !== undefined) {
    result = result.filter(r => r.status === roleSearchForm.status)
  }
  return result
})

const getDataScopeLabel = (scope: string) => {
  const map: Record<string, string> = {
    all: '全部数据',
    org_and_child: '本组织及下级',
    org_only: '仅本组织',
    self: '本人数据',
    custom: '自定义'
  }
  return map[scope] || scope
}

const handleRoleSearch = () => {}
const handleRoleReset = () => {
  roleSearchForm.name = ''
  roleSearchForm.status = undefined
}

const handleAddRole = () => {
  isEditRole.value = false
  roleDialogTitle.value = '新增角色'
  resetRoleForm()
  roleDialogVisible.value = true
}

const handleEditRole = (row: Role) => {
  isEditRole.value = true
  roleDialogTitle.value = '编辑角色'
  Object.assign(roleFormData, { ...row })
  roleDialogVisible.value = true
}

const handleDeleteRole = (row: Role) => {
  ElMessageBox.confirm(`确定要删除角色 "${row.name}" 吗？`, '系统提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    const index = allRoleList.value.findIndex(r => r.id === row.id)
    if (index !== -1) allRoleList.value.splice(index, 1)
    ElMessage.success('删除成功')
  }).catch(() => {})
}

const handleToggleRoleStatus = (row: Role) => {
  row.status = row.status === 1 ? 0 : 1
  ElMessage.success(row.status === 1 ? '启用成功' : '禁用成功')
}

const resetRoleForm = () => {
  roleFormData.id = 0
  roleFormData.name = ''
  roleFormData.code = ''
  roleFormData.dataScope = 'org_and_child'
  roleFormData.status = 1
  roleFormData.remark = ''
}

const handleRoleSubmit = async () => {
  if (!roleFormRef.value) return
  await roleFormRef.value.validate((valid) => {
    if (valid) {
      roleSubmitLoading.value = true
      setTimeout(() => {
        if (isEditRole.value) {
          const role = allRoleList.value.find(r => r.id === roleFormData.id)
          if (role) Object.assign(role, { ...roleFormData })
          ElMessage.success('修改成功')
        } else {
          allRoleList.value.push({
            ...roleFormData,
            id: allRoleList.value.length + 1,
            createTime: new Date().toLocaleString('zh-CN', { hour12: false })
          })
          ElMessage.success('新增成功')
        }
        roleDialogVisible.value = false
        roleSubmitLoading.value = false
      }, 500)
    }
  })
}

// 权限配置
const permDialogVisible = ref(false)
const permSubmitLoading = ref(false)
const currentRole = ref<Role | null>(null)
const permTreeRef = ref<any>(null)
const permDataScope = ref('org_and_child')
const checkedMenuKeys = ref<number[]>([])
const checkedOrgKeys = ref<number[]>([])

const menuTreeData = ref<Menu[]>([
  {
    id: 1, name: '全息看板', parentId: 0, type: 'menu', path: '/dashboard', sort: 1, status: 1,
    children: [
      { id: 11, name: '综合视图', parentId: 1, type: 'menu', path: '/holo-board/comprehensive', sort: 1, status: 1 },
      { id: 12, name: '告警视图', parentId: 1, type: 'menu', path: '/holo-board/alarm', sort: 2, status: 1 },
      { id: 13, name: '运营视图', parentId: 1, type: 'menu', path: '/holo-board/operation', sort: 3, status: 1 },
      { id: 14, name: '自定义视图', parentId: 1, type: 'menu', path: '/holo-board/custom', sort: 4, status: 1 }
    ]
  },
  {
    id: 2, name: '基础管理', parentId: 0, type: 'menu', path: '/basic', sort: 2, status: 1,
    children: [
      {
        id: 21, name: '隐患点管理', parentId: 2, type: 'menu', path: '/basic/hazard-point', sort: 1, status: 1,
        children: [
          { id: 211, name: '新增隐患点', parentId: 21, type: 'button', perms: 'basic:hazard:add', sort: 1, status: 1 },
          { id: 212, name: '编辑隐患点', parentId: 21, type: 'button', perms: 'basic:hazard:edit', sort: 2, status: 1 },
          { id: 213, name: '删除隐患点', parentId: 21, type: 'button', perms: 'basic:hazard:delete', sort: 3, status: 1 },
          { id: 214, name: '查看隐患点', parentId: 21, type: 'button', perms: 'basic:hazard:view', sort: 4, status: 1 },
          { id: 215, name: '批量停测', parentId: 21, type: 'button', perms: 'basic:hazard:pause', sort: 5, status: 1 },
          { id: 216, name: '批量完结', parentId: 21, type: 'button', perms: 'basic:hazard:complete', sort: 6, status: 1 },
          { id: 217, name: '导出隐患点', parentId: 21, type: 'button', perms: 'basic:hazard:export', sort: 7, status: 1 }
        ]
      },
      {
        id: 22, name: '监测类型', parentId: 2, type: 'menu', path: '/basic/monitor-type', sort: 2, status: 1,
        children: [
          { id: 221, name: '新增监测类型', parentId: 22, type: 'button', perms: 'basic:monitor:add', sort: 1, status: 1 },
          { id: 222, name: '编辑监测类型', parentId: 22, type: 'button', perms: 'basic:monitor:edit', sort: 2, status: 1 },
          { id: 223, name: '删除监测类型', parentId: 22, type: 'button', perms: 'basic:monitor:delete', sort: 3, status: 1 },
          { id: 224, name: '导入监测类型', parentId: 22, type: 'button', perms: 'basic:monitor:import', sort: 4, status: 1 },
          { id: 225, name: '导出监测类型', parentId: 22, type: 'button', perms: 'basic:monitor:export', sort: 5, status: 1 }
        ]
      },
      {
        id: 23, name: '设备管理', parentId: 2, type: 'menu', path: '/basic/device', sort: 3, status: 1,
        children: [
          { id: 231, name: '新增设备', parentId: 23, type: 'button', perms: 'basic:device:add', sort: 1, status: 1 },
          { id: 232, name: '编辑设备', parentId: 23, type: 'button', perms: 'basic:device:edit', sort: 2, status: 1 },
          { id: 233, name: '删除设备', parentId: 23, type: 'button', perms: 'basic:device:delete', sort: 3, status: 1 },
          { id: 234, name: '导入设备', parentId: 23, type: 'button', perms: 'basic:device:import', sort: 4, status: 1 },
          { id: 235, name: '导出设备', parentId: 23, type: 'button', perms: 'basic:device:export', sort: 5, status: 1 },
          { id: 236, name: '绑定设备', parentId: 23, type: 'button', perms: 'basic:device:bind', sort: 6, status: 1 }
        ]
      },
      {
        id: 24, name: '视频设备管理', parentId: 2, type: 'menu', path: '/basic/video-device', sort: 4, status: 1,
        children: [
          { id: 241, name: '新增视频设备', parentId: 24, type: 'button', perms: 'basic:video:add', sort: 1, status: 1 },
          { id: 242, name: '编辑视频设备', parentId: 24, type: 'button', perms: 'basic:video:edit', sort: 2, status: 1 },
          { id: 243, name: '删除视频设备', parentId: 24, type: 'button', perms: 'basic:video:delete', sort: 3, status: 1 },
          { id: 244, name: '导入视频设备', parentId: 24, type: 'button', perms: 'basic:video:import', sort: 4, status: 1 },
          { id: 245, name: '导出视频设备', parentId: 24, type: 'button', perms: 'basic:video:export', sort: 5, status: 1 }
        ]
      }
    ]
  },
  {
    id: 3, name: '告警中心', parentId: 0, type: 'menu', path: '/alarm', sort: 3, status: 1,
    children: [
      {
        id: 31, name: '实时告警', parentId: 3, type: 'menu', path: '/alarm/realtime', sort: 1, status: 1,
        children: [
          { id: 311, name: '查看告警', parentId: 31, type: 'button', perms: 'alarm:realtime:view', sort: 1, status: 1 },
          { id: 312, name: '确认告警', parentId: 31, type: 'button', perms: 'alarm:realtime:confirm', sort: 2, status: 1 },
          { id: 313, name: '导出告警', parentId: 31, type: 'button', perms: 'alarm:realtime:export', sort: 3, status: 1 }
        ]
      },
      {
        id: 32, name: '告警判据管理', parentId: 3, type: 'menu', path: '/alarm/criteria', sort: 2, status: 1,
        children: [
          { id: 321, name: '新增判据', parentId: 32, type: 'button', perms: 'alarm:criteria:add', sort: 1, status: 1 },
          { id: 322, name: '编辑判据', parentId: 32, type: 'button', perms: 'alarm:criteria:edit', sort: 2, status: 1 },
          { id: 323, name: '删除判据', parentId: 32, type: 'button', perms: 'alarm:criteria:delete', sort: 3, status: 1 }
        ]
      },
      {
        id: 33, name: '告警查看和通知', parentId: 3, type: 'menu', path: '/alarm/notification', sort: 3, status: 1,
        children: [
          { id: 331, name: '查看通知', parentId: 33, type: 'button', perms: 'alarm:notification:view', sort: 1, status: 1 },
          { id: 332, name: '配置通知', parentId: 33, type: 'button', perms: 'alarm:notification:config', sort: 2, status: 1 }
        ]
      },
      {
        id: 34, name: '告警处置', parentId: 3, type: 'menu', path: '/alarm/disposal', sort: 4, status: 1,
        children: [
          { id: 341, name: '处置告警', parentId: 34, type: 'button', perms: 'alarm:disposal:handle', sort: 1, status: 1 },
          { id: 342, name: '查看处置记录', parentId: 34, type: 'button', perms: 'alarm:disposal:view', sort: 2, status: 1 }
        ]
      }
    ]
  },
  {
    id: 4, name: '系统管理', parentId: 0, type: 'menu', path: '/system', sort: 4, status: 1,
    children: [
      {
        id: 41, name: '组织管理', parentId: 4, type: 'menu', path: '/system/organization', sort: 1, status: 1,
        children: [
          { id: 411, name: '新增组织', parentId: 41, type: 'button', perms: 'system:org:add', sort: 1, status: 1 },
          { id: 412, name: '编辑组织', parentId: 41, type: 'button', perms: 'system:org:edit', sort: 2, status: 1 },
          { id: 413, name: '删除组织', parentId: 41, type: 'button', perms: 'system:org:delete', sort: 3, status: 1 },
          { id: 414, name: '调整层级', parentId: 41, type: 'button', perms: 'system:org:adjust', sort: 4, status: 1 }
        ]
      },
      {
        id: 42, name: '身份管理', parentId: 4, type: 'menu', path: '/system/identity', sort: 2, status: 1,
        children: [
          { id: 421, name: '新增用户', parentId: 42, type: 'button', perms: 'system:user:add', sort: 1, status: 1 },
          { id: 422, name: '编辑用户', parentId: 42, type: 'button', perms: 'system:user:edit', sort: 2, status: 1 },
          { id: 423, name: '删除用户', parentId: 42, type: 'button', perms: 'system:user:delete', sort: 3, status: 1 },
          { id: 424, name: '重置密码', parentId: 42, type: 'button', perms: 'system:user:resetPwd', sort: 4, status: 1 },
          { id: 425, name: '启用禁用', parentId: 42, type: 'button', perms: 'system:user:toggleStatus', sort: 5, status: 1 }
        ]
      },
      {
        id: 43, name: '权限管理', parentId: 4, type: 'menu', path: '/system/permission', sort: 3, status: 1,
        children: [
          { id: 431, name: '新增角色', parentId: 43, type: 'button', perms: 'system:role:add', sort: 1, status: 1 },
          { id: 432, name: '编辑角色', parentId: 43, type: 'button', perms: 'system:role:edit', sort: 2, status: 1 },
          { id: 433, name: '删除角色', parentId: 43, type: 'button', perms: 'system:role:delete', sort: 3, status: 1 },
          { id: 434, name: '配置权限', parentId: 43, type: 'button', perms: 'system:role:config', sort: 4, status: 1 },
          { id: 435, name: '新增菜单', parentId: 43, type: 'button', perms: 'system:menu:add', sort: 5, status: 1 },
          { id: 436, name: '编辑菜单', parentId: 43, type: 'button', perms: 'system:menu:edit', sort: 6, status: 1 },
          { id: 437, name: '删除菜单', parentId: 43, type: 'button', perms: 'system:menu:delete', sort: 7, status: 1 }
        ]
      },
      {
        id: 44, name: '日志管理', parentId: 4, type: 'menu', path: '/system/log', sort: 4, status: 1,
        children: [
          { id: 441, name: '查看日志', parentId: 44, type: 'button', perms: 'system:log:view', sort: 1, status: 1 },
          { id: 442, name: '导出日志', parentId: 44, type: 'button', perms: 'system:log:export', sort: 2, status: 1 }
        ]
      },
      {
        id: 45, name: '系统设置', parentId: 4, type: 'menu', path: '/system/settings', sort: 5, status: 1,
        children: [
          { id: 451, name: '修改参数', parentId: 45, type: 'button', perms: 'system:setting:edit', sort: 1, status: 1 },
          { id: 452, name: '配置告警分发', parentId: 45, type: 'button', perms: 'system:setting:alarm', sort: 2, status: 1 },
          { id: 453, name: '导入告警规则', parentId: 45, type: 'button', perms: 'system:setting:import', sort: 3, status: 1 },
          { id: 454, name: '导出告警规则', parentId: 45, type: 'button', perms: 'system:setting:export', sort: 4, status: 1 }
        ]
      }
    ]
  }
])

const orgTreeData = ref<OrgNode[]>([
  {
    id: 1, name: '地质灾害监测中心',
    children: [
      {
        id: 2, name: '监测一部',
        children: [
          { id: 4, name: '北京监测组' },
          { id: 5, name: '天津监测组' }
        ]
      },
      {
        id: 3, name: '监测二部',
        children: [
          { id: 6, name: '河北监测组' }
        ]
      }
    ]
  }
])

const handleConfigPermission = (row: Role) => {
  currentRole.value = row
  permDataScope.value = row.dataScope
  checkedMenuKeys.value = [1, 11, 12, 2, 21, 3, 31]
  checkedOrgKeys.value = [1, 2, 4]
  permDialogVisible.value = true
}

const handlePermSubmit = () => {
  permSubmitLoading.value = true
  setTimeout(() => {
    permSubmitLoading.value = false
    permDialogVisible.value = false
    ElMessage.success('权限配置保存成功')
  }, 500)
}

// 菜单管理
const menuDialogVisible = ref(false)
const menuDialogTitle = ref('新增菜单')
const menuSubmitLoading = ref(false)
const menuFormRef = ref<FormInstance>()
const isEditMenu = ref(false)
const parentMenuName = ref('')

const menuFormData = reactive({
  id: 0,
  name: '',
  parentId: 0,
  path: '',
  icon: '',
  type: 'menu' as 'menu' | 'button',
  perms: '',
  sort: 0,
  status: 1
})

const menuFormRules: FormRules = {
  name: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择菜单类型', trigger: 'change' }],
  path: [{ required: true, message: '请输入路由路径', trigger: 'blur' }],
  perms: [{ required: true, message: '请输入权限标识', trigger: 'blur' }]
}

const menuList = computed(() => menuTreeData.value)

const handleAddMenu = () => {
  isEditMenu.value = false
  menuDialogTitle.value = '新增菜单'
  resetMenuForm()
  menuFormData.parentId = 0
  parentMenuName.value = '顶级菜单'
  menuDialogVisible.value = true
}

const handleEditMenu = (row: Menu) => {
  isEditMenu.value = true
  menuDialogTitle.value = '编辑菜单'
  Object.assign(menuFormData, { ...row })
  const parent = findMenuParent(menuTreeData.value, row.parentId)
  parentMenuName.value = parent ? parent.name : '顶级菜单'
  menuDialogVisible.value = true
}

const handleDeleteMenu = (row: Menu) => {
  if (row.children && row.children.length > 0) {
    ElMessage.warning('请先删除子菜单')
    return
  }
  ElMessageBox.confirm(`确定要删除菜单 "${row.name}" 吗？`, '系统提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    deleteMenuNode(menuTreeData.value, row.id)
    ElMessage.success('删除成功')
  }).catch(() => {})
}

const findMenuParent = (nodes: Menu[], parentId: number): Menu | null => {
  for (const node of nodes) {
    if (node.id === parentId) return node
    if (node.children) {
      const found = findMenuParent(node.children, parentId)
      if (found) return found
    }
  }
  return null
}

const deleteMenuNode = (nodes: Menu[], id: number): boolean => {
  const index = nodes.findIndex(n => n.id === id)
  if (index !== -1) {
    nodes.splice(index, 1)
    return true
  }
  for (const node of nodes) {
    if (node.children && deleteMenuNode(node.children, id)) return true
  }
  return false
}

const resetMenuForm = () => {
  menuFormData.id = 0
  menuFormData.name = ''
  menuFormData.parentId = 0
  menuFormData.path = ''
  menuFormData.icon = ''
  menuFormData.type = 'menu'
  menuFormData.perms = ''
  menuFormData.sort = 0
  menuFormData.status = 1
}

const handleMenuSubmit = async () => {
  if (!menuFormRef.value) return
  await menuFormRef.value.validate((valid) => {
    if (valid) {
      menuSubmitLoading.value = true
      setTimeout(() => {
        if (isEditMenu.value) {
          const updateNode = (nodes: Menu[]): boolean => {
            for (const node of nodes) {
              if (node.id === menuFormData.id) {
                Object.assign(node, { ...menuFormData })
                return true
              }
              if (node.children && updateNode(node.children)) return true
            }
            return false
          }
          updateNode(menuTreeData.value)
          ElMessage.success('修改成功')
        } else {
          const newMenu: Menu = {
            ...menuFormData,
            id: Date.now()
          }
          if (menuFormData.parentId === 0) {
            menuTreeData.value.push(newMenu)
          } else {
            const addToParent = (nodes: Menu[]): boolean => {
              for (const node of nodes) {
                if (node.id === menuFormData.parentId) {
                  if (!node.children) node.children = []
                  node.children.push(newMenu)
                  return true
                }
                if (node.children && addToParent(node.children)) return true
              }
              return false
            }
            addToParent(menuTreeData.value)
          }
          ElMessage.success('新增成功')
        }
        menuDialogVisible.value = false
        menuSubmitLoading.value = false
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

.perm-config {
  min-height: 400px;
}

.perm-role-info {
  margin-bottom: 16px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 4px;
}

.menu-icon-cell :deep(svg) {
  width: 18px;
  height: 18px;
}

:deep(.el-form--inline .el-form-item) {
  margin-right: 16px;
  margin-bottom: 10px;
}

:deep(.el-table .el-button--text) {
  padding: 4px 8px;
  margin: 0 4px;
  border-radius: 4px;
  font-size: 13px;
  transition: all 0.2s ease;
}

:deep(.el-table .el-button--text:hover) {
  background: #e6f7ff;
  color: #1890ff;
}

:deep(.el-table .el-button--text:primary) {
  color: #1890ff;
}

:deep(.el-table .el-button--text:primary:hover) {
  background: #e6f7ff;
}

:deep(.el-table .el-button--text:warning) {
  color: #faad14;
}

:deep(.el-table .el-button--text:warning:hover) {
  background: #fff7e6;
}

:deep(.el-table .el-button--text:danger) {
  color: #f5222d;
}

:deep(.el-table .el-button--text:danger:hover) {
  background: #fff1f0;
}

:deep(.el-table .el-button--text:success) {
  color: #52c41a;
}

:deep(.el-table .el-button--text:success:hover) {
  background: #f6ffed;
}

.action-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  border: none;
}

.action-btn-primary {
  background: #e6f7ff;
  color: #1890ff;
}

.action-btn-primary:hover {
  background: #91d5ff;
  color: #096dd9;
}

.action-btn-warning {
  background: #fff7e6;
  color: #faad14;
}

.action-btn-warning:hover {
  background: #ffe58f;
  color: #d48806;
}

.action-btn-danger {
  background: #fff1f0;
  color: #f5222d;
}

.action-btn-danger:hover {
  background: #ffccc7;
  color: #cf1322;
}

.action-btn-success {
  background: #f6ffed;
  color: #52c41a;
}

.action-btn-success:hover {
  background: #b7eb8f;
  color: #389e0d;
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
