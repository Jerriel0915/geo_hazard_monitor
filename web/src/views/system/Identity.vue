<template>
  <div class="page">
    <div class="header">
      <div class="header__left">
        <h2 class="header__title">身份管理</h2>
        <span class="header__subtitle">系统用户与角色权限配置</span>
      </div>
      <div class="header__right">
        <el-button type="primary" @click="handleAdd">新增用户</el-button>
      </div>
    </div>

    <div class="search">
      <el-input v-model="searchForm.username" placeholder="用户名" clearable />
      <el-input v-model="searchForm.realName" placeholder="真实姓名" clearable />
      <el-select v-model="searchForm.status" placeholder="状态" clearable>
        <el-option label="正常" :value="0" />
        <el-option label="禁用" :value="1" />
      </el-select>
      <el-tree-select
        v-model="searchForm.orgId"
        :data="orgTreeData"
        :props="{ label: 'name', value: 'id', children: 'children' }"
        placeholder="所属组织"
        clearable
        check-strictly
        :render-after-expand="false"
      />
      <el-button type="primary" @click="handleSearch">查询</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>

    <div class="table-wrap">
      <div class="table-wrap__scroll">
        <el-table :data="userList" border stripe v-loading="loading">
          <el-table-column type="index" label="序号" width="60" align="center" />
          <el-table-column prop="username" label="用户名" min-width="120" />
          <el-table-column prop="realName" label="真实姓名" min-width="120" />
          <el-table-column prop="orgName" label="所属组织" min-width="140" />
          <el-table-column prop="phone" label="联系电话" width="130" />
          <el-table-column prop="email" label="邮箱" min-width="160" />
          <el-table-column prop="status" label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="getStatusType(row.status)">{{ getStatusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="lastLoginTime" label="最后登录" min-width="160" />
          <el-table-column prop="createTime" label="创建时间" min-width="160" />
          <el-table-column label="操作" width="220" fixed="right">
            <template #default="{ row }">
              <div class="op-cell">
                <el-button type="primary" text size="small" @click="handleEdit(row)">编辑</el-button>
                <el-button type="primary" text size="small" @click="handleChangePwd(row)">修改密码</el-button>
                <el-button type="danger" text size="small" @click="handleDelete(row)">删除</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="table-wrap__pagination">
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
      width="640px"
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
      :title="`修改密码[${currentUser?.realName || ''}(${currentUser?.username || ''})]`"
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
        <el-form-item label="旧密码" prop="oldPwd">
          <el-input v-model="pwdForm.oldPwd" type="password" placeholder="请输入旧密码" show-password />
        </el-form-item>
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
import {onMounted, reactive, ref} from 'vue'
import type {FormInstance, FormRules} from 'element-plus'
import {ElMessage, ElMessageBox} from 'element-plus'
import {
  changeUserPassword,
  createUser,
  deleteUser,
  getOrganizationTree,
  getRoleOptions,
  getUserDetail,
  getUserPage,
  type OrganizationItem,
  type RoleItem,
  updateUser,
  type UserItem
} from '@/api/system'

const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增用户')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()
const isEdit = ref(false)
const editingUserId = ref<number>()

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
      validator: (_rule: any, value: string, callback: (error?: Error) => void) => {
        if (value !== formData.password) {
          callback(new Error('两次输入的密码不一致'))
          return
        }
        callback()
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

const orgTreeData = ref<OrganizationItem[]>([])
const roleList = ref<RoleItem[]>([])
const userList = ref<UserItem[]>([])

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const getStatusType = (status: number) => {
  const map: Record<number, string> = { 0: 'success', 1: 'danger' }
  return map[status] || 'info'
}

const getStatusLabel = (status: number) => {
  const map: Record<number, string> = { 0: '正常', 1: '禁用' }
  return map[status] || '未知'
}

const loadBaseOptions = async () => {
  const [orgs, roles] = await Promise.all([getOrganizationTree(), getRoleOptions()])
  orgTreeData.value = orgs
  roleList.value = roles
}

const loadUsers = async () => {
  loading.value = true
  try {
    const data = await getUserPage({
      pageNum: pagination.page,
      pageSize: pagination.size,
      username: searchForm.username || undefined,
      realName: searchForm.realName || undefined,
      orgId: searchForm.orgId,
      status: searchForm.status
    })
    userList.value = data.rows
    pagination.total = data.total
  } finally {
    loading.value = false
  }
}

const handleSearch = async () => {
  pagination.page = 1
  await loadUsers()
}

const handleReset = async () => {
  searchForm.username = ''
  searchForm.realName = ''
  searchForm.status = undefined
  searchForm.orgId = undefined
  pagination.page = 1
  await loadUsers()
}

const handleAdd = () => {
  isEdit.value = false
  dialogTitle.value = '新增用户'
  resetForm()
  dialogVisible.value = true
}

const handleEdit = async (row: UserItem) => {
  isEdit.value = true
  dialogTitle.value = '编辑用户'
  const detail = await getUserDetail(row.id)
  editingUserId.value = row.id
  Object.assign(formData, {
    id: detail.id,
    username: detail.username,
    realName: detail.realName,
    phone: detail.phone || '',
    email: detail.email || '',
    orgId: detail.orgId,
    status: detail.status ?? 0,
    roleIds: detail.roleIds || [],
    remark: detail.remark || '',
    password: '',
    confirmPwd: ''
  })
  dialogVisible.value = true
}

const handleDelete = async (row: UserItem) => {
  if (row.username === 'admin') {
    ElMessage.warning('系统内置账号不可删除')
    return
  }
  await ElMessageBox.confirm(`确定要删除用户 "${row.username}" 吗？`, '系统提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteUser(row.id)
  ElMessage.success('删除成功')
  await loadUsers()
}

// 重置密码
const pwdDialogVisible = ref(false)
const pwdLoading = ref(false)
const pwdFormRef = ref<FormInstance>()
const currentUserId = ref<number>()
const currentUser = ref<UserItem | null>(null)

const pwdForm = reactive({
  oldPwd: '',
  newPwd: '',
  confirmPwd: ''
})

const pwdRules: FormRules = {
  oldPwd: [
    { required: true, message: '请输入旧密码', trigger: 'blur' }
  ],
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

const handleChangePwd = (row: UserItem) => {
  currentUserId.value = row.id
  currentUser.value = row
  pwdForm.oldPwd = ''
  pwdForm.newPwd = ''
  pwdForm.confirmPwd = ''
  pwdDialogVisible.value = true
}

const handlePwdSubmit = async () => {
  if (!pwdFormRef.value) return
  await pwdFormRef.value.validate()
  if (!currentUserId.value) return
  pwdLoading.value = true
  try {
    await changeUserPassword(currentUserId.value, {
      oldPassword: pwdForm.oldPwd,
      newPassword: pwdForm.newPwd
    })
    pwdDialogVisible.value = false
    ElMessage.success('密码修改成功')
  } finally {
    pwdLoading.value = false
  }
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
  editingUserId.value = undefined
  formRef.value?.clearValidate()
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate()
  submitLoading.value = true
  try {
    const payload = {
      username: formData.username,
      password: formData.password || undefined,
      realName: formData.realName,
      phone: formData.phone,
      email: formData.email,
      orgId: formData.orgId,
      status: formData.status,
      roleIds: formData.roleIds,
      remark: formData.remark
    }
    if (isEdit.value && editingUserId.value) {
      await updateUser(editingUserId.value, payload)
      ElMessage.success('修改成功')
    } else {
      if (!formData.password) {
        ElMessage.warning('新增用户时必须填写密码')
        return
      }
      await createUser(payload)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    await loadUsers()
  } finally {
    submitLoading.value = false
  }
}

const handleSizeChange = async (val: number) => {
  pagination.size = val
  pagination.page = 1
  await loadUsers()
}

const handlePageChange = async (val: number) => {
  pagination.page = val
  await loadUsers()
}

onMounted(async () => {
  await Promise.all([loadBaseOptions(), loadUsers()])
})
</script>



