<template>
  <div class="page">
    <div class="header">
      <div class="header__left">
        <h2 class="header__title">共享策略</h2>
        <span class="header__subtitle">管理系统成果数据向第三方系统、上下级单位等进行分享</span>
      </div>
      <div class="header__right">
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon> 新增策略
        </el-button>
      </div>
    </div>

    <div class="search">
      <el-input v-model="searchName" placeholder="搜索策略名称或编号" class="search__input" clearable @clear="loadData" @keyup.enter="loadData">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="searchStatus" placeholder="状态" clearable class="search__select" @change="loadData">
        <el-option label="已启用" value="ENABLED" />
        <el-option label="已停用" value="DISABLED" />
      </el-select>
      <el-select v-model="searchMethod" placeholder="分享方式" clearable class="search__select" @change="loadData">
        <el-option label="统一化数据推送" value="UNIFIED_PUSH" />
        <el-option label="定制化数据推送" value="CUSTOM_PUSH" />
        <el-option label="统一化数据服务" value="UNIFIED_SERVICE" />
        <el-option label="定制化数据服务" value="CUSTOM_SERVICE" />
      </el-select>
      <el-button type="primary" @click="loadData">搜索</el-button>
      <el-button @click="handleResetSearch">重置</el-button>
    </div>

    <div v-loading="loading" class="grid">
      <el-empty v-if="!loading && strategyList.length === 0" description="暂无共享策略" />

      <div v-for="item in strategyList" :key="item.id" class="card" :class="{ 'card--disabled': item.status === 'DISABLED' }">
        <div class="card__header">
          <div class="card__title-row">
            <div class="card__title-info">
              <h3 class="card__title">{{ item.name }}</h3>
              <span class="card__code">{{ item.code }}</span>
            </div>
            <el-switch
              :model-value="item.status === 'ENABLED'"
              size="small"
              active-text="启用"
              inactive-text="停用"
              @change="(val: boolean) => handleToggleStatus(item, val)"
            />
          </div>
          <p class="card__desc">{{ item.description }}</p>
        </div>

        <div class="card__body">
          <div class="card__method-tag">
            <el-tag :type="getMethodTagType(item.method)" size="small">
              {{ METHOD_LABELS[item.method] }}
            </el-tag>
          </div>

          <div class="card__info-grid">
            <div class="card__info-item">
              <span class="card__info-label">地址</span>
              <span class="card__info-value">{{ item.address }}</span>
            </div>
            <div class="card__info-item">
              <span class="card__info-label">主题</span>
              <span class="card__info-value">{{ item.topic || '-' }}</span>
            </div>
            <div class="card__info-item">
              <span class="card__info-label">账号</span>
              <span class="card__info-value">{{ item.username || '-' }}</span>
            </div>
            <div class="card__info-item">
              <span class="card__info-label">范围</span>
              <span class="card__info-value">{{ SCOPE_TYPE_LABELS[item.scopeType] }} ({{ item.scopeIds.length }}个)</span>
            </div>
            <div class="card__info-item">
              <span class="card__info-label">频率</span>
              <span class="card__info-value">{{ item.frequency }}</span>
            </div>
            <div class="card__info-item">
              <span class="card__info-label">成功分享</span>
              <span class="card__info-value text-success">{{ item.successCount }}次</span>
            </div>
          </div>

          <div v-if="item.lastRunTime" class="card__last-run">
            <span class="card__last-run-label">最近运行:</span>
            <span :class="['card__last-run-value', getRunStatusClass(item.lastRunStatus)]">{{ item.lastRunTime }}</span>
            <span v-if="item.lastRunStatus" :class="['card__last-run-status', getRunStatusClass(item.lastRunStatus)]">
              {{ getRunStatusLabel(item.lastRunStatus) }}
            </span>
          </div>
        </div>

        <div class="card__footer">
          <el-button type="primary" text size="small" @click="handleRun(item)" :disabled="item.status !== 'ENABLED'">
            <el-icon><ArrowRight /></el-icon> 执行
          </el-button>
          <el-button v-if="item.method === 'CUSTOM_PUSH' || item.method === 'CUSTOM_SERVICE'" type="primary" text size="small" @click="handleEditScript(item)">
            <el-icon><Files /></el-icon> 脚本
          </el-button>
          <el-button type="primary" text size="small" @click="handleViewLogs(item)">
            <el-icon><Document /></el-icon> 日志
          </el-button>
          <el-button type="primary" text size="small" @click="handleEditScope(item)">
            <el-icon><MapLocation /></el-icon> 范围
          </el-button>
          <el-button type="primary" text size="small" @click="handleEdit(item)">
            <el-icon><Setting /></el-icon> 编辑
          </el-button>
          <el-button type="danger" text size="small" @click="handleDelete(item)">
            <el-icon><Delete /></el-icon> 删除
          </el-button>
        </div>
      </div>
    </div>

    <div v-if="total > 0" class="pagination">
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[12, 24, 48]"
        layout="total, sizes, prev, pager, next"
        @size-change="loadData"
        @current-change="loadData"
      />
    </div>

    <el-dialog v-model="dialogVisible" :title="editingItem ? '编辑策略' : '新增策略'" width="720px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="120px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="策略编号" prop="code">
              <el-input v-model="formData.code" placeholder="请输入策略编号" :disabled="isEdit" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="策略名称" prop="name">
              <el-input v-model="formData.name" placeholder="请输入策略名称" maxlength="100" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="策略描述" prop="description">
          <el-input v-model="formData.description" type="textarea" :rows="3" placeholder="请输入策略描述" maxlength="500" />
        </el-form-item>

        <el-form-item label="分享方式" prop="method">
          <el-select v-model="formData.method" placeholder="请选择分享方式">
            <el-option label="统一化数据推送" value="UNIFIED_PUSH" />
            <el-option label="定制化数据推送" value="CUSTOM_PUSH" />
            <el-option label="统一化数据服务" value="UNIFIED_SERVICE" />
            <el-option label="定制化数据服务" value="CUSTOM_SERVICE" />
          </el-select>
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="目标地址" prop="address">
              <el-input v-model="formData.address" placeholder="IP地址或IP:端口" maxlength="200" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="主题/路径" prop="topic">
              <el-input v-model="formData.topic" placeholder="MQTT主题或API路径" maxlength="200" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="账号" prop="username">
              <el-input v-model="formData.username" placeholder="认证账号" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="密码" prop="password">
              <el-input v-model="formData.password" type="password" placeholder="认证密码" maxlength="100" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="其他参数">
          <el-input v-model="paramsJson" type="textarea" :rows="3" placeholder='{"key": "value"}' maxlength="500" />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="数据范围类型" prop="scopeType">
              <el-select v-model="formData.scopeType" placeholder="请选择范围类型">
                <el-option label="隐患点分组" value="HAZARD_POINT_GROUP" />
                <el-option label="隐患点" value="HAZARD_POINT" />
                <el-option label="厂商" value="VENDOR" />
                <el-option label="设备" value="DEVICE" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="执行频率" prop="frequency">
              <el-input v-model="formData.frequency" placeholder="Cron表达式，如: 0 0/30 * * * ?" maxlength="50" />
              <el-tooltip content="常用: 每分钟(0 * * * * ?) 每5分钟(0 0/5 * * * ?) 每小时(0 0 * * * ?) 每天2点(0 0 2 * * ?)">
                <el-icon class="form-hint-icon"><QuestionFilled /></el-icon>
              </el-tooltip>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="scopeDialogVisible" :title="`设置数据范围 - ${editingScopeItem?.name || ''}`" width="640px" destroy-on-close>
      <div class="scope-dialog">
        <el-select
          v-model="scopeForm.scopeType"
          placeholder="选择范围类型"
          class="scope-type-select"
          @change="loadScopeOptions"
        >
          <el-option label="隐患点分组" value="HAZARD_POINT_GROUP" />
          <el-option label="隐患点" value="HAZARD_POINT" />
          <el-option label="厂商" value="VENDOR" />
          <el-option label="设备" value="DEVICE" />
        </el-select>

        <div v-if="scopeOptions.length > 0" class="scope-options">
          <el-checkbox-group v-model="scopeForm.scopeIds">
            <el-checkbox
              v-for="option in scopeOptions"
              :key="option.id"
              :label="option.id"
            >
              {{ option.name }}
            </el-checkbox>
          </el-checkbox-group>
        </div>
        <el-empty v-else-if="scopeForm.scopeType" description="暂无可选数据" />
      </div>
      <template #footer>
        <el-button @click="scopeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleScopeSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="logDialogVisible" :title="`运行日志 - ${currentStrategyName}`" width="800px" destroy-on-close>
      <el-table :data="logList" border size="small" v-loading="logLoading">
        <el-table-column prop="runTime" label="运行时间" width="180" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'SUCCESS' ? 'success' : row.status === 'ERROR' ? 'danger' : 'warning'">
              {{ row.status === 'SUCCESS' ? '成功' : row.status === 'ERROR' ? '失败' : '超时' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="dataCount" label="数据条数" width="100" />
        <el-table-column prop="duration" label="耗时(ms)" width="100" />
        <el-table-column prop="message" label="信息" />
      </el-table>
      <div v-if="logTotal > 0" class="log-pagination">
        <el-pagination
          v-model:current-page="logPageNum"
          v-model:page-size="logPageSize"
          :total="logTotal"
          layout="total, sizes, prev, pager, next"
          @size-change="loadLogs"
          @current-change="loadLogs"
        />
      </div>
      <template #footer>
        <el-button @click="logDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <ShareStrategyScriptDrawer
      v-if="scriptDrawerVisible"
      v-model:visible="scriptDrawerVisible"
      :strategy-id="currentStrategyId"
      :strategy-name="currentStrategyName"
      :method="currentStrategyMethod"
      @saved="loadData"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Plus, Search, Edit, Delete, Document, MapLocation, Setting, ArrowRight, Files, QuestionFilled } from '@element-plus/icons-vue'
import {
  getShareStrategyPage,
  createShareStrategy,
  updateShareStrategy,
  deleteShareStrategy,
  changeShareStrategyStatus,
  runShareStrategy,
  getShareStrategyLogs,
  type ShareStrategyItem,
  type ShareStrategyLog,
  METHOD_LABELS,
  SCOPE_TYPE_LABELS
} from '@/api/shareStrategy'

const loading = ref(false)
const strategyList = ref<ShareStrategyItem[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(12)

const searchName = ref('')
const searchStatus = ref<'' | 'ENABLED' | 'DISABLED'>('')
const searchMethod = ref<'' | ShareStrategyItem['method']>('')

const dialogVisible = ref(false)
const editingItem = ref<ShareStrategyItem | null>(null)
const submitting = ref(false)
const formRef = ref<FormInstance>()

const scopeDialogVisible = ref(false)
const editingScopeItem = ref<ShareStrategyItem | null>(null)

const logDialogVisible = ref(false)
const logLoading = ref(false)
const logList = ref<ShareStrategyLog[]>([])
const logTotal = ref(0)
const logPageNum = ref(1)
const logPageSize = ref(20)

const scriptDrawerVisible = ref(false)
const currentStrategyId = ref(0)
const currentStrategyName = ref('')
const currentStrategyMethod = ref<ShareStrategyItem['method']>('UNIFIED_PUSH')

const isEdit = computed(() => !!editingItem.value)

const formData = reactive({
  code: '',
  name: '',
  description: '',
  method: 'UNIFIED_PUSH' as ShareStrategyItem['method'],
  address: '',
  topic: '',
  username: '',
  password: '',
  params: {} as Record<string, any>,
  scopeType: 'HAZARD_POINT' as ShareStrategyItem['scopeType'],
  scopeIds: [] as number[],
  frequency: ''
})

const scopeForm = reactive({
  scopeType: 'HAZARD_POINT' as ShareStrategyItem['scopeType'],
  scopeIds: [] as number[]
})

const scopeOptions = ref<{ id: number; name: string }[]>([])

const paramsJson = computed({
  get: () => JSON.stringify(formData.params, null, 2),
  set: (val) => {
    try {
      formData.params = val ? JSON.parse(val) : {}
    } catch {
      ElMessage.error('JSON格式错误')
    }
  }
})

const formRules: FormRules = {
  code: [{ required: true, message: '请输入策略编号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入策略名称', trigger: 'blur' }],
  method: [{ required: true, message: '请选择分享方式', trigger: 'change' }],
  address: [{ required: true, message: '请输入目标地址', trigger: 'blur' }],
  frequency: [{ required: true, message: '请输入执行频率', trigger: 'blur' }],
  scopeType: [{ required: true, message: '请选择数据范围类型', trigger: 'change' }]
}

async function loadData() {
  loading.value = true
  try {
    const res = await getShareStrategyPage({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      name: searchName.value || undefined,
      status: searchStatus.value || undefined,
      method: searchMethod.value || undefined
    })
    strategyList.value = res.rows
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function handleResetSearch() {
  searchName.value = ''
  searchStatus.value = ''
  searchMethod.value = ''
  pageNum.value = 1
  loadData()
}

function handleAdd() {
  editingItem.value = null
  Object.assign(formData, {
    code: '',
    name: '',
    description: '',
    method: 'UNIFIED_PUSH',
    address: '',
    topic: '',
    username: '',
    password: '',
    params: {},
    scopeType: 'HAZARD_POINT',
    scopeIds: [],
    frequency: ''
  })
  dialogVisible.value = true
}

function handleEdit(item: ShareStrategyItem) {
  editingItem.value = item
  Object.assign(formData, {
    code: item.code,
    name: item.name,
    description: item.description,
    method: item.method,
    address: item.address,
    topic: item.topic || '',
    username: item.username || '',
    password: item.password || '',
    params: item.params || {},
    scopeType: item.scopeType,
    scopeIds: [...item.scopeIds],
    frequency: item.frequency
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    const payload = {
      code: formData.code,
      name: formData.name,
      description: formData.description,
      method: formData.method,
      address: formData.address,
      topic: formData.topic || undefined,
      username: formData.username || undefined,
      password: formData.password || undefined,
      params: formData.params,
      scopeType: formData.scopeType,
      scopeIds: formData.scopeIds,
      frequency: formData.frequency,
      status: 'DISABLED' as const
    }
    if (editingItem.value) {
      await updateShareStrategy(editingItem.value.id, payload)
      ElMessage.success('更新成功')
    } else {
      await createShareStrategy(payload)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadData()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

async function handleDelete(item: ShareStrategyItem) {
  try {
    await ElMessageBox.confirm(`确定删除策略「${item.name}」？删除后不可恢复。`, '删除确认', { type: 'warning' })
    await deleteShareStrategy(item.id)
    ElMessage.success('删除成功')
    loadData()
  } catch { }
}

async function handleToggleStatus(item: ShareStrategyItem, enabled: boolean) {
  const newStatus = enabled ? 'ENABLED' : 'DISABLED' as const
  const action = enabled ? '启用' : '停用'
  try {
    await ElMessageBox.confirm(`确定${action}策略「${item.name}」？`, `${action}确认`, { type: 'warning' })
    await changeShareStrategyStatus(item.id, newStatus)
    ElMessage.success(`${action}成功`)
    loadData()
  } catch { }
}

async function handleRun(item: ShareStrategyItem) {
  try {
    await runShareStrategy(item.id)
    ElMessage.success('策略已触发执行')
    loadData()
  } catch (e: any) {
    ElMessage.error(e.message || '执行失败')
  }
}

function handleEditScript(item: ShareStrategyItem) {
  currentStrategyId.value = item.id
  currentStrategyName.value = item.name
  currentStrategyMethod.value = item.method
  scriptDrawerVisible.value = true
}

function handleViewLogs(item: ShareStrategyItem) {
  currentStrategyId.value = item.id
  currentStrategyName.value = item.name
  logPageNum.value = 1
  logDialogVisible.value = true
  loadLogs()
}

async function loadLogs() {
  logLoading.value = true
  try {
    const res = await getShareStrategyLogs({
      strategyId: currentStrategyId.value,
      pageNum: logPageNum.value,
      pageSize: logPageSize.value
    })
    logList.value = res.rows
    logTotal.value = res.total
  } finally {
    logLoading.value = false
  }
}

function handleEditScope(item: ShareStrategyItem) {
  editingScopeItem.value = item
  scopeForm.scopeType = item.scopeType
  scopeForm.scopeIds = [...item.scopeIds]
  scopeDialogVisible.value = true
  loadScopeOptions()
}

async function loadScopeOptions() {
  scopeOptions.value = []
  try {
    const type = scopeForm.scopeType
    let res: any[] = []
    if (type === 'HAZARD_POINT') {
      const hazardRes = await import('@/api/hazardPoint').then(m => m.getHazardPointPage({ pageNum: 1, pageSize: 1000 }))
      res = (hazardRes.data?.rows || []).map((item: any) => ({ id: item.id, name: item.name }))
    } else if (type === 'DEVICE') {
      const deviceRes = await import('@/api/device').then(m => m.getDevicePage({ pageNum: 1, pageSize: 1000 }))
      res = (deviceRes.data?.rows || []).map((item: any) => ({ id: item.id, name: item.name }))
    }
    scopeOptions.value = res
  } catch {
    scopeOptions.value = []
  }
}

async function handleScopeSubmit() {
  if (!editingScopeItem.value) return
  try {
    await updateShareStrategy(editingScopeItem.value.id, {
      scopeType: scopeForm.scopeType,
      scopeIds: scopeForm.scopeIds
    })
    ElMessage.success('更新成功')
    scopeDialogVisible.value = false
    loadData()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  }
}

function getMethodTagType(method: ShareStrategyItem['method']): string {
  switch (method) {
    case 'UNIFIED_PUSH': return 'primary'
    case 'CUSTOM_PUSH': return 'warning'
    case 'UNIFIED_SERVICE': return 'success'
    case 'CUSTOM_SERVICE': return 'danger'
    default: return 'default'
  }
}

function getRunStatusClass(status?: string): string {
  if (status === 'SUCCESS') return 'text-success'
  if (status === 'ERROR') return 'text-danger'
  if (status === 'TIMEOUT') return 'text-warning'
  return ''
}

function getRunStatusLabel(status?: string): string {
  if (status === 'SUCCESS') return '成功'
  if (status === 'ERROR') return '失败'
  if (status === 'TIMEOUT') return '超时'
  return ''
}

onMounted(() => loadData())
</script>

<style scoped>
.page {
  background: #f0f2f5;
}

.search {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 16px;
  background: #fff;
  margin-bottom: 16px;
  border-radius: 8px;
}

.search__input {
  width: 240px;
}

.search__select {
  width: 160px;
}

.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
  gap: 16px;
  padding-bottom: 20px;
}

.card {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  transition: all 0.3s;
}

.card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
}

.card--disabled {
  opacity: 0.7;
}

.card__header {
  padding: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.card__title-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 8px;
}

.card__title-info {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.card__title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.card__code {
  font-size: 12px;
  color: #909399;
  font-family: monospace;
}

.card__desc {
  font-size: 13px;
  color: #606266;
  margin: 0;
  line-height: 1.5;
}

.card__body {
  padding: 16px;
}

.card__method-tag {
  margin-bottom: 12px;
}

.card__info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}

.card__info-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.card__info-label {
  font-size: 12px;
  color: #909399;
}

.card__info-value {
  font-size: 13px;
  color: #303133;
}

.card__last-run {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed #e4e7ed;
}

.card__last-run-label {
  font-size: 12px;
  color: #909399;
}

.card__last-run-value {
  font-size: 13px;
  color: #606266;
}

.card__last-run-status {
  font-size: 12px;
  padding: 2px 6px;
  border-radius: 4px;
}

.card__footer {
  display: flex;
  gap: 8px;
  padding: 12px 16px;
  background: #fafafa;
  border-top: 1px solid #f0f0f0;
}

.text-success {
  color: #67c23a;
}

.text-danger {
  color: #f56c6c;
}

.text-warning {
  color: #e6a23c;
}

.form-hint-icon {
  margin-left: 8px;
  color: #909399;
}

.scope-dialog {
  padding: 16px;
}

.scope-type-select {
  width: 100%;
  margin-bottom: 16px;
}

.scope-options {
  max-height: 300px;
  overflow-y: auto;
}

.log-pagination {
  margin-top: 16px;
  text-align: right;
}

.pagination {
  display: flex;
  justify-content: center;
  padding: 20px 0;
}
</style>