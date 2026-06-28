<template>
  <div class="page">
    <!-- 页头 -->
    <div class="header">
      <div class="header__left">
        <h2 class="header__title">综合告警</h2>
        <span class="header__subtitle">高阶多参数综合告警策略管理</span>
      </div>
      <div class="header__right">
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon> 新增策略
        </el-button>
      </div>
    </div>

    <!-- 搜索栏 -->
    <div class="search">
      <el-input v-model="searchName" placeholder="搜索策略名称或描述" class="search__input" clearable @clear="loadData" @keyup.enter="loadData">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="searchStatus" placeholder="状态" clearable class="search__select" @change="loadData">
        <el-option label="已启用" value="ENABLED" />
        <el-option label="已停用" value="DISABLED" />
      </el-select>
      <el-select v-model="searchTriggerMode" placeholder="触发方式" clearable class="search__select" @change="loadData">
        <el-option label="周期触发" value="PERIODIC" />
        <el-option label="实时触发" value="REALTIME" />
      </el-select>
      <el-button type="primary" @click="loadData">搜索</el-button>
      <el-button @click="handleResetSearch">重置</el-button>
    </div>

    <!-- 卡片列表 -->
    <div v-loading="loading" class="grid">
      <el-empty v-if="!loading && alarmList.length === 0" description="暂无综合告警策略" />

      <div v-for="item in alarmList" :key="item.id" class="card" :class="{ 'card--disabled': item.status === 'DISABLED' }">
        <div class="card__header">
          <div class="card__title-row">
            <h3 class="card__title">{{ item.name }}</h3>
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

        <div class="card__meta">
          <div class="card__meta-row">
            <el-tag :type="item.triggerMode === 'REALTIME' ? 'warning' : 'primary'" size="small" effect="plain">
              {{ item.triggerMode === 'REALTIME' ? '实时触发' : '周期触发' }}
            </el-tag>
            <span v-if="item.triggerMode === 'PERIODIC' && item.cronExpression" class="cron-text">{{ item.cronExpression }}</span>
          </div>
          <div class="card__meta-row">
            <span class="card__meta-label">静默:</span>
            <span class="card__meta-value">{{ item.silenceSeconds ? item.silenceSeconds + 'h' : '未设置' }}</span>
            <span class="card__meta-label" style="margin-left: 12px">持续:</span>
            <span class="card__meta-value">{{ item.sustainSeconds ? item.sustainSeconds + '次' : '未设置' }}</span>
          </div>
          <div class="card__meta-row">
            <span class="card__meta-label">等级变化提醒:</span>
            <span :class="['card__meta-value', item.levelChangeNotify ? 'text-success' : 'text-muted']">{{ item.levelChangeNotify ? '已开启' : '已关闭' }}</span>
          </div>
          <div class="card__meta-row">
            <span class="card__meta-label">应用范围:</span>
            <span class="card__meta-value">{{ item.scopeCount || 0 }} 个隐患点</span>
          </div>
          <div v-if="item.lastRunTime" class="card__meta-row">
            <span class="card__meta-label">最近运行:</span>
            <span :class="['card__meta-value', getRunStatusClass(item.lastRunStatus)]">{{ item.lastRunTime }}</span>
          </div>
        </div>

        <div class="card__footer">
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
        <el-form-item label="静默周期" prop="silenceSeconds">
          <el-input-number v-model="formData.silenceSeconds" :min="0" :max="720" :step="1" />
          <span class="form-hint">&nbsp;h</span>
        </el-form-item>
        <el-form-item label="持续时长" prop="sustainSeconds">
          <el-input-number v-model="formData.sustainSeconds" :min="0" :max="999" :step="1" />
          <span class="form-hint">&nbsp;次 (0表示不限制)</span>
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
  getStrategyList as getCompositeAlarmPage,
  createStrategy as createCompositeAlarm,
  updateStrategy as updateCompositeAlarm,
  deleteStrategy as deleteCompositeAlarm,
  toggleStrategy,
  type AlarmStrategyItem as CompositeAlarmItem
} from '@/api/alarm'
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
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '操作失败')
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
  const action = enabled ? '启用' : '停用'
  try {
    await ElMessageBox.confirm(`确定${action}策略「${item.name}」？`, `${action}确认`, { type: 'warning' })
    await toggleStrategy(item.id, enabled ? 1 : 0)
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
/* 页面特有覆盖 */
.page {
  background: #f0f2f5;
}

/* 组件特有：等宽字体 Cron 文本 */
.cron-text {
  font-family: 'Courier New', monospace;
  font-size: 12px;
  background: #f2f3f5;
  padding: 1px 6px;
  border-radius: 4px;
  color: #4e5969;
}
</style>
