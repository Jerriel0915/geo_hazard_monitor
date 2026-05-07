<template>
  <div class="page-content">
    <div class="page-title">身份管理</div>
    <div class="page-body">
      <div class="search-bar">
        <el-form :model="searchForm" inline>
          <el-form-item label="用户名">
            <el-input v-model="searchForm.username" placeholder="请输入用户名" clearable />
          </el-form-item>
          <el-form-item label="真实姓名">
            <el-input v-model="searchForm.realName" placeholder="请输入真实姓名" clearable />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="searchForm.status" placeholder="全部状态" clearable>
              <el-option label="正常" :value="0" />
              <el-option label="禁用" :value="1" />
              <el-option label="锁定" :value="2" />
              <el-option label="过期" :value="3" />
            </el-select>
          </el-form-item>
          <el-form-item label="所属组织">
            <el-tree-select
              v-model="searchForm.orgId"
              :data="orgTreeData"
              :props="{ label: 'name', value: 'id', children: 'children' }"
              placeholder="请选择组织"
              clearable
              check-strictly
              :render-after-expand="false"
              style="width: 200px"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
        <el-button type="primary" @click="handleAdd">新增用户</el-button>
      </div>

      <el-table :data="userList" border stripe v-loading="loading">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="realName" label="真实姓名" width="120" />
        <el-table-column prop="orgName" label="所属组织" width="150" />
        <el-table-column prop="phone" label="联系电话" width="140" />
        <el-table-column prop="email" label="邮箱" width="180" />
        <el-table-column prop="roles" label="角色" min-width="150">
          <template #default="{ row }">
            <el-tag v-for="role in row.roles" :key="role" size="small" style="margin-right: 4px;">{{ role }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastLoginTime" label="最后登录" width="160" />
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <span class="action-link" @click="handleEdit(row)">编辑</span>
            <span class="action-link" @click="handleResetPwd(row)">重置密码</span>
            <span :class="['action-link', row.status === 1 ? 'action-warning' : 'action-success']" @click="handleToggleStatus(row)">
              {{ row.status === 1 ? '禁用' : '启用' }}
            </span>
            <span class="action-link action-danger" @click="handleDelete(row)">删除</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          prev-text="上一页"
          next-text="下一页"
          :disabled="pagination.total === 0"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <!-- 用户表单弹窗 -->
    <el-dialog
      :title="dialogTitle"
      v-model="dialogVisible"
      width="650px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="100px"
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="formData.username" placeholder="请输入用户名" :disabled="isEdit" maxlength="30" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="真实姓名" prop="realName">
              <el-input v-model="formData.realName" placeholder="请输入真实姓名" maxlength="20" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="密码" prop="password" v-if="!isEdit">
              <el-input v-model="formData.password" type="password" placeholder="请输入密码" maxlength="50" show-password />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="确认密码" prop="confirmPwd" v-if="!isEdit">
              <el-input v-model="formData.confirmPwd" type="password" placeholder="请确认密码" maxlength="50" show-password />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="联系电话" prop="phone">
              <el-input v-model="formData.phone" placeholder="请输入联系电话" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="formData.email" placeholder="请输入邮箱" maxlength="50" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="所属组织" prop="orgId">
              <el-tree-select
                v-model="formData.orgId"
                :data="orgTreeData"
                :props="{ label: 'name', value: 'id', children: 'children' }"
                placeholder="请选择组织"
                check-strictly
                :render-after-expand="false"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-select v-model="formData.status" placeholder="请选择状态" style="width: 100%">
                <el-option label="正常" :value="0" />
                <el-option label="禁用" :value="1" />
                <el-option label="锁定" :value="2" />
                <el-option label="过期" :value="3" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="角色" prop="roleIds">
          <el-select v-model="formData.roleIds" multiple placeholder="请选择角色" style="width: 100%">
            <el-option v-for="role in roleList" :key="role.id" :label="role.name" :value="role.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="formData.remark" type="textarea" :rows="2" placeholder="请输入备注" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码弹窗 -->
    <el-dialog
      :title="`重置密码[${currentUser?.realName || ''}(${currentUser?.username || ''})]`"
      v-model="pwdDialogVisible"
      width="400px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="pwdFormRef"
        :model="pwdForm"
        :rules="pwdRules"
        label-width="100px"
      >
        <el-form-item label="新密码" prop="newPwd">
          <el-input v-model="pwdForm.newPwd" type="password" placeholder="请输入新密码" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPwd">
          <el-input v-model="pwdForm.confirmPwd" type="password" placeholder="请确认新密码" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handlePwdSubmit" :loading="pwdLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'

interface User {
  id: number
  username: string
  realName: string
  orgId: number
  orgName: string
  phone: string
  email: string
  roles: string[]
  roleIds: number[]
  status: number
  lastLoginTime: string
  createTime: string
  remark?: string
}

interface Role {
  id: number
  name: string
}

interface OrgNode {
  id: number
  name: string
  children?: OrgNode[]
}

const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增用户')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()
const isEdit = ref(false)

const searchForm = reactive({
  username: '',
  realName: '',
  status: undefined as number | undefined,
  orgId: undefined as number | undefined
})

const formData = reactive({
  id: 0,
  username: '',
  realName: '',
  password: '',
  confirmPwd: '',
  phone: '',
  email: '',
  orgId: undefined as number | undefined,
  status: 0,
  roleIds: [] as number[],
  remark: ''
})

const formRules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 30, message: '长度在 3 到 30 个字符', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]+$/, message: '只能包含字母、数字、下划线', trigger: 'blur' }
  ],
  realName: [
    { required: true, message: '请输入真实姓名', trigger: 'blur' },
    { min: 2, max: 20, message: '长度在 2 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 50, message: '长度在 6 到 50 个字符', trigger: 'blur' }
  ],
  confirmPwd: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (_rule: any, value: string, callback: Function) => {
        if (value !== formData.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  orgId: [
    { required: true, message: '请选择所属组织', trigger: 'change' }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  roleIds: [
    { required: true, message: '请选择角色', trigger: 'change', type: 'array' }
  ]
}

// 模拟组织树数据
const orgTreeData = ref<OrgNode[]>([
  {
    id: 1,
    name: '地质灾害监测中心',
    children: [
      {
        id: 2,
        name: '监测一部',
        children: [
          { id: 4, name: '北京监测组' },
          { id: 5, name: '天津监测组' }
        ]
      },
      {
        id: 3,
        name: '监测二部',
        children: [
          { id: 6, name: '河北监测组' }
        ]
      }
    ]
  }
])

// 模拟角色列表
const roleList = ref<Role[]>([
  { id: 1, name: '超级管理员' },
  { id: 2, name: '管理员' },
  { id: 3, name: '值班员' },
  { id: 4, name: '巡检员' },
  { id: 5, name: '只读用户' }
])

// 模拟用户数据
const allUserList = ref<User[]>([
  {
    id: 1,
    username: 'admin',
    realName: '系统管理员',
    orgId: 1,
    orgName: '地质灾害监测中心',
    phone: '13800138001',
    email: 'admin@example.com',
    roles: ['超级管理员'],
    roleIds: [1],
    status: 0,
    lastLoginTime: '2024-03-20 09:30:00',
    createTime: '2024-01-01 10:00:00',
    remark: '系统内置账号'
  },
  {
    id: 2,
    username: 'zhangsan',
    realName: '张三',
    orgId: 2,
    orgName: '监测一部',
    phone: '13800138002',
    email: 'zhangsan@example.com',
    roles: ['管理员'],
    roleIds: [2],
    status: 0,
    lastLoginTime: '2024-03-19 16:45:00',
    createTime: '2024-01-05 09:00:00'
  },
  {
    id: 3,
    username: 'lisi',
    realName: '李四',
    orgId: 4,
    orgName: '北京监测组',
    phone: '13800138003',
    email: 'lisi@example.com',
    roles: ['值班员'],
    roleIds: [3],
    status: 0,
    lastLoginTime: '2024-03-20 08:15:00',
    createTime: '2024-01-10 08:30:00'
  },
  {
    id: 4,
    username: 'wangwu',
    realName: '王五',
    orgId: 5,
    orgName: '天津监测组',
    phone: '13800138004',
    email: 'wangwu@example.com',
    roles: ['巡检员'],
    roleIds: [4],
    status: 1,
    lastLoginTime: '2024-03-15 14:20:00',
    createTime: '2024-01-12 10:00:00'
  },
  {
    id: 5,
    username: 'zhaoliu',
    realName: '赵六',
    orgId: 6,
    orgName: '河北监测组',
    phone: '13800138005',
    email: 'zhaoliu@example.com',
    roles: ['只读用户'],
    roleIds: [5],
    status: 2,
    lastLoginTime: '2024-03-10 11:00:00',
    createTime: '2024-01-15 11:00:00'
  }
])

const pagination = reactive({
  page: 1,
  size: 10,
  total: 5
})

// 过滤后的用户列表
const userList = computed(() => {
  let result = allUserList.value

  if (searchForm.username) {
    result = result.filter(u => u.username.includes(searchForm.username))
  }
  if (searchForm.realName) {
    result = result.filter(u => u.realName.includes(searchForm.realName))
  }
  if (searchForm.status !== undefined) {
    result = result.filter(u => u.status === searchForm.status)
  }
  if (searchForm.orgId !== undefined) {
    result = result.filter(u => u.orgId === searchForm.orgId)
  }

  pagination.total = result.length
  const start = (pagination.page - 1) * pagination.size
  const end = start + pagination.size
  return result.slice(start, end)
})

const getStatusType = (status: number) => {
  const map: Record<number, string> = { 0: 'success', 1: 'danger', 2: 'warning', 3: 'info' }
  return map[status] || 'info'
}

const getStatusLabel = (status: number) => {
  const map: Record<number, string> = { 0: '正常', 1: '禁用', 2: '锁定', 3: '过期' }
  return map[status] || '未知'
}

const handleSearch = () => {
  pagination.page = 1
}

const handleReset = () => {
  searchForm.username = ''
  searchForm.realName = ''
  searchForm.status = undefined
  searchForm.orgId = undefined
  pagination.page = 1
}

const handleAdd = () => {
  isEdit.value = false
  dialogTitle.value = '新增用户'
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (row: User) => {
  isEdit.value = true
  dialogTitle.value = '编辑用户'
  Object.assign(formData, {
    id: row.id,
    username: row.username,
    realName: row.realName,
    phone: row.phone,
    email: row.email,
    orgId: row.orgId,
    status: row.status,
    roleIds: [...row.roleIds],
    remark: row.remark || '',
    password: '',
    confirmPwd: ''
  })
  dialogVisible.value = true
}

const handleDelete = (row: User) => {
  if (row.username === 'admin') {
    ElMessage.warning('系统内置账号不可删除')
    return
  }
  ElMessageBox.confirm(`确定要删除用户 "${row.username}" 吗？`, '系统提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    const index = allUserList.value.findIndex(u => u.id === row.id)
    if (index !== -1) {
      allUserList.value.splice(index, 1)
    }
    ElMessage.success('删除成功')
  }).catch(() => {})
}

const handleToggleStatus = (row: User) => {
  const newStatus = row.status === 1 ? 0 : 1
  const action = newStatus === 0 ? '启用' : '禁用'
  ElMessageBox.confirm(`确定要${action}用户 "${row.username}" 吗？`, '系统提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    row.status = newStatus
    ElMessage.success(`${action}成功`)
  }).catch(() => {})
}

// 重置密码
const pwdDialogVisible = ref(false)
const pwdLoading = ref(false)
const pwdFormRef = ref<FormInstance>()
const currentUserId = ref(0)
const currentUser = ref<User | null>(null)

const pwdForm = reactive({
  newPwd: '',
  confirmPwd: ''
})

const pwdRules: FormRules = {
  newPwd: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 50, message: '长度在 6 到 50 个字符', trigger: 'blur' }
  ],
  confirmPwd: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (_rule: any, value: string, callback: Function) => {
        if (value !== pwdForm.newPwd) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

const handleResetPwd = (row: User) => {
  currentUserId.value = row.id
  currentUser.value = row
  pwdForm.newPwd = ''
  pwdForm.confirmPwd = ''
  pwdDialogVisible.value = true
}

const handlePwdSubmit = async () => {
  if (!pwdFormRef.value) return
  await pwdFormRef.value.validate((valid) => {
    if (valid) {
      pwdLoading.value = true
      setTimeout(() => {
        pwdLoading.value = false
        pwdDialogVisible.value = false
        ElMessage.success('密码重置成功')
      }, 500)
    }
  })
}

const resetForm = () => {
  formData.id = 0
  formData.username = ''
  formData.realName = ''
  formData.password = ''
  formData.confirmPwd = ''
  formData.phone = ''
  formData.email = ''
  formData.orgId = undefined
  formData.status = 0
  formData.roleIds = []
  formData.remark = ''
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate((valid) => {
    if (valid) {
      submitLoading.value = true
      setTimeout(() => {
        const now = new Date().toLocaleString('zh-CN', { hour12: false })
        if (isEdit.value) {
          const user = allUserList.value.find(u => u.id === formData.id)
          if (user) {
            Object.assign(user, {
              realName: formData.realName,
              phone: formData.phone,
              email: formData.email,
              orgId: formData.orgId,
              orgName: getOrgName(formData.orgId!),
              status: formData.status,
              roleIds: [...formData.roleIds],
              roles: formData.roleIds.map(id => roleList.value.find(r => r.id === id)?.name || ''),
              remark: formData.remark
            })
          }
          ElMessage.success('修改成功')
        } else {
          const newUser: User = {
            id: allUserList.value.length + 1,
            username: formData.username,
            realName: formData.realName,
            phone: formData.phone,
            email: formData.email,
            orgId: formData.orgId!,
            orgName: getOrgName(formData.orgId!),
            status: formData.status,
            roleIds: [...formData.roleIds],
            roles: formData.roleIds.map(id => roleList.value.find(r => r.id === id)?.name || ''),
            lastLoginTime: '-',
            createTime: now,
            remark: formData.remark
          }
          allUserList.value.push(newUser)
          ElMessage.success('新增成功')
        }
        dialogVisible.value = false
        submitLoading.value = false
      }, 500)
    }
  })
}

const getOrgName = (orgId: number): string => {
  const findName = (nodes: OrgNode[]): string | null => {
    for (const node of nodes) {
      if (node.id === orgId) return node.name
      if (node.children) {
        const found = findName(node.children)
        if (found) return found
      }
    }
    return null
  }
  return findName(orgTreeData.value) || ''
}

const handleSizeChange = (val: number) => {
  pagination.size = val
  pagination.page = 1
}

const handlePageChange = (val: number) => {
  pagination.page = val
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

.search-bar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 10px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
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
