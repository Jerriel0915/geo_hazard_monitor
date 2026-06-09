<template>
  <div class="composite-alarm-page">
    <!-- 页头 -->
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">综合告警</h2>
        <span class="page-subtitle">高阶多参数综合告警策略管理</span>
      </div>
      <div class="header-right">
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon> 新增策略
        </el-button>
      </div>
    </div>

    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-input v-model="searchName" placeholder="搜索策略名称或描述" class="search-input" clearable @clear="loadData" @keyup.enter="loadData">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="searchStatus" placeholder="状态" clearable class="filter-select" @change="loadData">
        <el-option label="已启用" value="ENABLED" />
        <el-option label="已停用" value="DISABLED" />
      </el-select>
      <el-select v-model="searchTriggerMode" placeholder="触发方式" clearable class="filter-select" @change="loadData">
        <el-option label="周期触发" value="PERIODIC" />
        <el-option label="实时触发" value="REALTIME" />
      </el-select>
      <el-button type="primary" @click="loadData">搜索</el-button>
      <el-button @click="handleResetSearch">重置</el-button>
    </div>

    <!-- 卡片列表 -->
    <div v-loading="loading" class="card-grid">
      <el-empty v-if="!loading && alarmList.length === 0" description="暂无综合告警策略" />

      <div v-for="item in alarmList" :key="item.id" class="alarm-card" :class="{ 'is-disabled': item.status === 'DISABLED' }">
        <div class="card-header">
          <div class="card-title-row">
            <h3 class="card-title">{{ item.name }}</h3>
            <el-switch
              :model-value="item.status === 'ENABLED'"
              size="small"
              active-text="启用"
              inactive-text="停用"
              @change="(val: boolean) => handleToggleStatus(item, val)"
            />
          </div>
          <p class="card-desc">{{ item.description }}</p>
        </div>

        <div class="card-meta">
          <div class="meta-row">
            <el-tag :type="item.triggerMode === 'REALTIME' ? 'warning' : 'primary'" size="small" effect="plain">
              {{ item.triggerMode === 'REALTIME' ? '实时触发' : '周期触发' }}
            </el-tag>
            <span v-if="item.triggerMode === 'PERIODIC' && item.cronExpression" class="cron-text">{{ item.cronExpression }}</span>
          </div>
          <div class="meta-row">
            <span class="meta-label">静默:</span>
            <span class="meta-value">{{ formatDuration(item.silenceSeconds || 0) }}</span>
            <span class="meta-label" style="margin-left: 12px">持续:</span>
            <span class="meta-value">{{ item.sustainSeconds ? formatDuration(item.sustainSeconds) : '未设置' }}</span>
          </div>
          <div class="meta-row">
            <span class="meta-label">等级变化提醒:</span>
            <span :class="['meta-value', item.levelChangeNotify ? 'text-success' : 'text-muted']">{{ item.levelChangeNotify ? '已开启' : '已关闭' }}</span>
          </div>
          <div class="meta-row">
            <span class="meta-label">应用范围:</span>
            <span class="meta-value">{{ item.scopeCount || 0 }} 个隐患点</span>
          </div>
          <div v-if="item.lastRunTime" class="meta-row">
            <span class="meta-label">最近运行:</span>
            <span :class="['meta-value', getRunStatusClass(item.lastRunStatus)]">{{ item.lastRunTime }}</span>
          </div>
        </div>

        <div class="card-footer">
          <el-button type="primary" text size="small" @click="handleEditScript(item)">
            <el-icon><Edit /></el-icon> 脚本
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

    <!-- 分页 -->
    <div v-if="total > 0" class="pagination-wrap">
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

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="editingItem ? '编辑策略' : '新增策略'" width="640px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="120px">
        <el-form-item label="策略名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入策略名称" maxlength="100" />
        </el-form-item>
        <el-form-item label="策略描述" prop="description">
          <el-input v-model="formData.description" type="textarea" :rows="3" placeholder="请输入策略描述" maxlength="500" />
        </el-form-item>
        <el-form-item label="触发方式" prop="triggerMode">
          <el-radio-group v-model="formData.triggerMode">
            <el-radio value="PERIODIC">周期触发</el-radio>
            <el-radio value="REALTIME">实时触发</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="formData.triggerMode === 'PERIODIC'" label="Cron 表达式" prop="cronExpression">
          <el-input v-model="formData.cronExpression" placeholder="例: 0 0/30 * * * ? (每30分钟)">
            <template #append>
              <el-tooltip content="常用: 每分钟(0 * * * * ?) 每5分钟(0 0/5 * * * ?) 每小时(0 0 * * * ?) 每天2点(0 0 2 * * ?)">
                <el-icon><QuestionFilled /></el-icon>
              </el-tooltip>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item v-if="formData.triggerMode === 'REALTIME'" label="订阅类型">
          <el-select v-model="formData.subscriptionSourceType" placeholder="选择订阅数据源">
            <el-option label="传感器数据" value="SENSOR_DATA" />
            <el-option label="告警信息" value="ALARM" />
          </el-select>
        </el-form-item>
        <el-form-item label="静默时长(秒)" prop="silenceSeconds">
          <el-input-number v-model="formData.silenceSeconds" :min="0" :max="864000" :step="60" />
        </el-form-item>
        <el-form-item label="持续时长(秒)" prop="sustainSeconds">
          <el-input-number v-model="formData.sustainSeconds" :min="0" :max="864000" :step="60" />
          <span class="form-hint">为0表示不限制</span>
        </el-form-item>
        <el-form-item label="等级变化提醒">
          <el-switch v-model="formData.levelChangeNotify" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 运行日志弹窗 -->
    <CompositeAlarmLogDrawer v-if="logDrawerVisible" v-model:visible="logDrawerVisible" :alarm-id="currentAlarmId" :alarm-name="currentAlarmName" />

    <!-- 应用范围弹窗 -->
    <CompositeAlarmScopeDialog v-if="scopeDialogVisible" v-model:visible="scopeDialogVisible" :alarm-id="currentAlarmId" />

    <!-- 脚本编辑抽屉 -->
    <CompositeAlarmScriptDrawer
      v-if="scriptDrawerVisible"
      v-model:visible="scriptDrawerVisible"
      :alarm-id="currentAlarmId"
      :trigger-mode="(currentTriggerMode as any)"
      @saved="loadData"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Plus, Search, Edit, Delete, Document, MapLocation, Setting, QuestionFilled } from '@element-plus/icons-vue'
import {
  getCompositeAlarmPage,
  createCompositeAlarm,
  updateCompositeAlarm,
  deleteCompositeAlarm,
  changeCompositeAlarmStatus,
  type CompositeAlarmItem
} from '@/api/compositeAlarm'
import CompositeAlarmLogDrawer from './components/CompositeAlarmLogDrawer.vue'
import CompositeAlarmScopeDialog from './components/CompositeAlarmScopeDialog.vue'
import CompositeAlarmScriptDrawer from './components/CompositeAlarmScriptDrawer.vue'

// ==================== 列表状态 ====================
const loading = ref(false)
const alarmList = ref<CompositeAlarmItem[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(12)

const searchName = ref('')
const searchStatus = ref<'' | 'ENABLED' | 'DISABLED'>('')
const searchTriggerMode = ref<'' | 'PERIODIC' | 'REALTIME'>('')

// ==================== 弹窗状态 ====================
const dialogVisible = ref(false)
const editingItem = ref<CompositeAlarmItem | null>(null)
const submitting = ref(false)
const formRef = ref<FormInstance>()

const logDrawerVisible = ref(false)
const scopeDialogVisible = ref(false)
const scriptDrawerVisible = ref(false)
const currentAlarmId = ref(0)
const currentAlarmName = ref('')
const currentTriggerMode = ref<string>('CRON')

const formData = reactive({
  name: '',
  description: '',
  triggerMode: 'PERIODIC' as 'PERIODIC' | 'REALTIME',
  cronExpression: '',
  subscriptionSourceType: 'SENSOR_DATA' as 'ALARM' | 'SENSOR_DATA',
  silenceSeconds: 0,
  sustainSeconds: 0,
  levelChangeNotify: false
})

const formRules: FormRules = {
  name: [{ required: true, message: '请输入策略名称', trigger: 'blur' }],
  triggerMode: [{ required: true, message: '请选择触发方式', trigger: 'change' }],
  cronExpression: [{ required: true, message: '请输入 Cron 表达式', trigger: 'blur' }]
}

// ==================== 数据加载 ====================
async function loadData() {
  loading.value = true
  try {
    const res = await getCompositeAlarmPage({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      name: searchName.value || undefined,
      status: searchStatus.value || undefined,
      triggerMode: searchTriggerMode.value || undefined
    })
    alarmList.value = res.rows
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function handleResetSearch() {
  searchName.value = ''
  searchStatus.value = ''
  searchTriggerMode.value = ''
  pageNum.value = 1
  loadData()
}

// ==================== CRUD ====================
function handleAdd() {
  editingItem.value = null
  Object.assign(formData, {
    name: '', description: '', triggerMode: 'PERIODIC', cronExpression: '',
    subscriptionSourceType: 'SENSOR_DATA', silenceSeconds: 0, sustainSeconds: 0, levelChangeNotify: false
  })
  dialogVisible.value = true
}

function handleEdit(item: CompositeAlarmItem) {
  editingItem.value = item
  Object.assign(formData, {
    name: item.name, description: item.description, triggerMode: item.triggerMode,
    cronExpression: item.cronExpression || '',
    subscriptionSourceType: item.subscriptionConfig?.sourceType || 'SENSOR_DATA',
    silenceSeconds: item.silenceSeconds, sustainSeconds: item.sustainSeconds,
    levelChangeNotify: item.levelChangeNotify
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    const payload: Partial<CompositeAlarmItem> = {
      name: formData.name, description: formData.description, triggerMode: formData.triggerMode,
      silenceSeconds: formData.silenceSeconds, sustainSeconds: formData.sustainSeconds,
      levelChangeNotify: formData.levelChangeNotify
    }
    if (formData.triggerMode === 'PERIODIC') {
      payload.cronExpression = formData.cronExpression
    } else {
      payload.subscriptionConfig = { sourceType: formData.subscriptionSourceType }
    }
    if (editingItem.value) {
      await updateCompositeAlarm(editingItem.value.id, payload as any)
      ElMessage.success('更新成功')
    } else {
      await createCompositeAlarm(payload as any)
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

async function handleDelete(item: CompositeAlarmItem) {
  try {
    await ElMessageBox.confirm(`确定删除策略「${item.name}」？删除后不可恢复。`, '删除确认', { type: 'warning' })
    await deleteCompositeAlarm(item.id)
    ElMessage.success('删除成功')
    loadData()
  } catch { /* cancelled */ }
}

async function handleToggleStatus(item: CompositeAlarmItem, enabled: boolean) {
  const newStatus = enabled ? 'ENABLED' : 'DISABLED' as const
  const action = enabled ? '启用' : '停用'
  try {
    await ElMessageBox.confirm(`确定${action}策略「${item.name}」？`, `${action}确认`, { type: 'warning' })
    await changeCompositeAlarmStatus(item.id, newStatus)
    ElMessage.success(`${action}成功`)
    loadData()
  } catch { /* cancelled */ }
}

// ==================== 子功能入口 ====================
function handleEditScript(item: CompositeAlarmItem) {
  currentAlarmId.value = item.id
  currentTriggerMode.value = item.triggerMode as string
  scriptDrawerVisible.value = true
}

function handleViewLogs(item: CompositeAlarmItem) {
  currentAlarmId.value = item.id
  currentAlarmName.value = item.name
  logDrawerVisible.value = true
}

function handleEditScope(item: CompositeAlarmItem) {
  currentAlarmId.value = item.id
  scopeDialogVisible.value = true
}

// ==================== 工具方法 ====================
function formatDuration(seconds: number): string {
  if (seconds <= 0) return '未设置'
  if (seconds < 60) return `${seconds}秒`
  if (seconds < 3600) return `${Math.floor(seconds / 60)}分钟`
  if (seconds < 86400) return `${Math.floor(seconds / 3600)}小时`
  return `${Math.floor(seconds / 86400)}天`
}

function getRunStatusClass(status?: string): string {
  if (status === 'SUCCESS') return 'text-success'
  if (status === 'ERROR') return 'text-danger'
  if (status === 'TIMEOUT') return 'text-warning'
  return ''
}

onMounted(() => loadData())
</script>

<style scoped>
.composite-alarm-page {
  padding: 20px;
  background: #f0f2f5;
  min-height: calc(100vh - 60px);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header-left {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.page-title {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  color: #1d2129;
}

.page-subtitle {
  font-size: 13px;
  color: #86909c;
}

.search-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
  background: #fff;
  padding: 16px 20px;
  border-radius: 8px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.06);
}

.search-input {
  width: 260px;
}

.filter-select {
  width: 140px;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(420px, 1fr));
  gap: 16px;
  min-height: 200px;
}

.alarm-card {
  background: #fff;
  border-radius: 10px;
  padding: 20px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  transition: box-shadow 0.2s, transform 0.15s;
  display: flex;
  flex-direction: column;
  border: 1px solid #e5e6eb;
}

.alarm-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
  transform: translateY(-2px);
}

.alarm-card.is-disabled {
  opacity: 0.65;
  background: #fafafa;
}

.card-header {
  margin-bottom: 14px;
}

.card-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.card-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #1d2129;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
  margin-right: 12px;
}

.card-desc {
  margin: 0;
  font-size: 13px;
  color: #86909c;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-meta {
  flex: 1;
  padding: 12px 0;
  border-top: 1px solid #f2f3f5;
  border-bottom: 1px solid #f2f3f5;
}

.meta-row {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 6px;
  font-size: 13px;
  line-height: 1.6;
}

.meta-row:last-child {
  margin-bottom: 0;
}

.meta-label {
  color: #86909c;
  flex-shrink: 0;
}

.meta-value {
  color: #4e5969;
}

.cron-text {
  font-family: 'Courier New', monospace;
  font-size: 12px;
  background: #f2f3f5;
  padding: 1px 6px;
  border-radius: 4px;
  color: #4e5969;
}

.text-success { color: #00b42a; }
.text-danger { color: #f53f3f; }
.text-warning { color: #ff7d00; }
.text-muted { color: #c9cdd4; }

.card-footer {
  display: flex;
  align-items: center;
  gap: 4px;
  padding-top: 12px;
  flex-wrap: wrap;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
  padding: 12px 0;
}

.form-hint {
  margin-left: 8px;
  font-size: 12px;
  color: #86909c;
}
</style>
