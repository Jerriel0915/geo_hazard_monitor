<template>
  <div class="page">
    <div class="header">
      <div class="header__left">
        <h2 class="header__title">通知公告管理</h2>
        <span class="header__subtitle">系统通知与公告的发布与管理</span>
      </div>
      <div class="header__right">
        <el-button type="primary" @click="handleAdd">新增公告</el-button>
      </div>
    </div>

    <div class="search">
      <el-input v-model="queryParams.noticeTitle" placeholder="标题" clearable @clear="handleQuery" />
      <el-select v-model="queryParams.noticeType" placeholder="类型" clearable @clear="handleQuery">
        <el-option label="通知" value="1" />
        <el-option label="公告" value="2" />
      </el-select>
      <el-button type="primary" @click="handleQuery">查询</el-button>
      <el-button @click="resetQuery">重置</el-button>
    </div>

    <div class="table-wrap">
      <div class="table-wrap__scroll">
        <el-table :data="noticeList" v-loading="loading" border stripe>
          <el-table-column prop="noticeId" label="ID" width="80" />
          <el-table-column prop="noticeTitle" label="标题" min-width="200" show-overflow-tooltip />
          <el-table-column label="类型" width="80">
            <template #default="{ row }">
              <el-tag :type="row.noticeType === '1' ? 'warning' : 'success'" size="small">
                {{ row.noticeType === '1' ? '通知' : '公告' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="80">
            <template #default="{ row }">
              <el-tag :type="row.status === '0' ? '' : 'danger'" size="small">
                {{ row.status === '0' ? '正常' : '关闭' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createBy" label="创建人" width="100" />
          <el-table-column prop="createTime" label="创建时间" min-width="160" />
          <el-table-column label="操作" width="220" fixed="right">
            <template #default="{ row }">
              <div class="op-cell">
                <el-button type="primary" text size="small" @click="handleView(row)">查看</el-button>
                <el-button type="primary" text size="small" @click="handleEdit(row)">修改</el-button>
                <el-button type="primary" text size="small" @click="handleReadUsers(row)">已读</el-button>
                <el-button type="danger" text size="small" @click="handleDelete(row)">删除</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <div class="table-wrap__pagination">
        <el-pagination
          v-model:current-page="queryParams.pageNum"
          v-model:page-size="queryParams.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="handleQuery"
          @current-change="handleQuery"
        />
      </div>
    </div>

    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="640px" destroy-on-close>
      <el-form :model="form" label-width="80px" :rules="rules" ref="formRef">
        <el-form-item label="标题" prop="noticeTitle">
          <el-input v-model="form.noticeTitle" placeholder="请输入标题" maxlength="50" />
        </el-form-item>
        <el-form-item label="类型" prop="noticeType">
          <el-radio-group v-model="form.noticeType">
            <el-radio value="1">通知</el-radio>
            <el-radio value="2">公告</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="内容" prop="noticeContent">
          <el-input v-model="form.noticeContent" type="textarea" :rows="8" placeholder="请输入内容（支持HTML）" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio value="0">正常</el-radio>
            <el-radio value="1">关闭</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="备注信息" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog title="通知详情" v-model="viewVisible" width="640px">
      <div class="notice-detail">
        <h3>{{ viewData.noticeTitle }}</h3>
        <div class="notice-meta">
          <span>类型：<el-tag :type="viewData.noticeType === '1' ? 'warning' : 'success'" size="small">{{ viewData.noticeType === '1' ? '通知' : '公告' }}</el-tag></span>
          <span>创建人：{{ viewData.createBy }}</span>
          <span>创建时间：{{ viewData.createTime }}</span>
        </div>
        <div class="notice-content" v-html="viewData.noticeContent" />
      </div>
    </el-dialog>

    <el-dialog title="已读人员" v-model="readUsersVisible" width="800px">
      <div class="table-wrap">
        <div class="table-wrap__scroll">
          <el-table :data="readUsers" v-loading="readUsersLoading" border stripe>
            <el-table-column prop="userName" label="用户名" width="120" />
            <el-table-column prop="nickName" label="昵称" width="120" />
            <el-table-column prop="deptName" label="部门" min-width="150" />
            <el-table-column prop="phonenumber" label="手机号" width="140" />
            <el-table-column prop="readTime" label="阅读时间" min-width="160" />
          </el-table>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { getNoticeList, getNoticeById, createNotice, updateNotice, deleteNotices, getReadUsers, type SysNotice, type ReadUser } from '@/api/notice'

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const viewVisible = ref(false)
const readUsersVisible = ref(false)
const readUsersLoading = ref(false)
const formRef = ref<FormInstance>()
const isEdit = ref(false)

const queryParams = reactive({ pageNum: 1, pageSize: 10, noticeTitle: '', noticeType: '' })
const noticeList = ref<SysNotice[]>([])
const total = ref(0)
const readUsers = ref<ReadUser[]>([])

const form = reactive<Partial<SysNotice>>({ noticeTitle: '', noticeType: '1', noticeContent: '', status: '0', remark: '' })
const viewData = reactive<Partial<SysNotice>>({})

const rules = {
  noticeTitle: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  noticeType: [{ required: true, message: '请选择类型', trigger: 'change' }],
  noticeContent: [{ required: true, message: '请输入内容', trigger: 'blur' }]
}

const dialogTitle = computed(() => isEdit.value ? '修改公告' : '新增公告')

async function handleQuery() {
  loading.value = true
  try {
    const res = await getNoticeList(queryParams)
    noticeList.value = res.rows ?? []
    total.value = res.total ?? 0
  } catch {
    // ignore
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  queryParams.noticeTitle = ''
  queryParams.noticeType = ''
  queryParams.pageNum = 1
  handleQuery()
}

function handleAdd() {
  isEdit.value = false
  Object.assign(form, { noticeId: undefined, noticeTitle: '', noticeType: '1', noticeContent: '', status: '0', remark: '' })
  dialogVisible.value = true
}

async function handleEdit(row: SysNotice) {
  isEdit.value = true
  try {
    const res = await getNoticeById(row.noticeId)
    const d = res.data as any
    Object.assign(form, d.data ?? d)
    dialogVisible.value = true
  } catch {
    ElMessage.error('获取详情失败')
  }
}

function handleView(row: SysNotice) {
  Object.assign(viewData, row)
  viewVisible.value = true
}

async function handleDelete(row: SysNotice) {
  try {
    await ElMessageBox.confirm('确认删除该公告？', '提示', { type: 'warning' })
    await deleteNotices([row.noticeId])
    ElMessage.success('删除成功')
    handleQuery()
  } catch { /* cancelled */ }
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    if (isEdit.value) {
      await updateNotice(form)
      ElMessage.success('修改成功')
    } else {
      await createNotice(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    handleQuery()
  } catch {
    ElMessage.error('操作失败')
  } finally {
    submitLoading.value = false
  }
}

async function handleReadUsers(row: SysNotice) {
  readUsersVisible.value = true
  readUsersLoading.value = true
  try {
    const res = await getReadUsers(row.noticeId)
    readUsers.value = res.rows ?? []
  } catch {
    readUsers.value = []
  } finally {
    readUsersLoading.value = false
  }
}

handleQuery()
</script>

<style scoped>
.notice-detail h3 { margin: 0 0 12px; }
.notice-meta { display: flex; gap: 16px; color: #909399; font-size: 13px; margin-bottom: 16px; padding-bottom: 12px; border-bottom: 1px solid #ebeef5; }
.notice-content { line-height: 1.8; }
</style>
