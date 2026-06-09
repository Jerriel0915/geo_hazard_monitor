<template>
  <div class="page-content">
    <div class="page-title">告警判据</div>
    <div class="page-body">
      <el-tabs v-model="activeTab" class="criteria-tabs" @tab-change="handleTabChange">

        <!-- ========== 监测类型 ========== -->
        <el-tab-pane label="监测类型" name="monitorType">
          <div class="criteria-container">
            <div class="left-panel">
              <div class="type-list">
                <div v-if="monitorTypes.length === 0" class="empty-hint">加载中...</div>
                <div
                  v-for="item in monitorTypes"
                  :key="item.id"
                  class="type-item"
                  :class="{ active: selectedMonitorType === item.id }"
                  @click="selectMonitorType(item.id)"
                >
                  <span class="type-name">{{ item.name }}</span>
                  <span class="type-count">{{ getTypeCriteriaCount(item.id) }}</span>
                </div>
              </div>
            </div>
            <div class="right-panel">
              <template v-if="!selectedMonitorType">
                <div class="empty-hint">请选择左侧监测类型</div>
              </template>
              <template v-else>
                <div class="criteria-header">
                  <span class="sub-title">{{ selectedMonitorTypeName }} — 告警判据</span>
                  <el-button type="primary" size="small" @click="openCreateDialog('monitor')">新增判据</el-button>
                </div>
                <div v-if="currentTypeCriteria.length > 1" class="criteria-selector">
                  <el-select v-model="selectedTypeCriteriaId" placeholder="选择判据" @change="onTypeCriteriaSelect">
                    <el-option v-for="c in currentTypeCriteria" :key="c.id" :label="c.name" :value="c.id"/>
                  </el-select>
                </div>
                <template v-if="activeTypeCriteria">
                  <div class="criteria-info-bar">
                    <el-switch :model-value="activeTypeCriteria.isEnabled === 1"
                               @change="handleToggle(activeTypeCriteria)" size="small"/>
                    <span class="criteria-name-text">{{ activeTypeCriteria.name }}</span>
                    <span v-if="activeTypeCriteria.version" class="version-text">v{{
                        activeTypeCriteria.version
                      }}</span>
                    <el-button size="small" type="danger" @click="handleDelete(activeTypeCriteria)">删除</el-button>
                  </div>
                  <div class="level-sections">
                    <div v-for="lv in alarmLevels" :key="lv.value" class="level-section">
                      <div class="level-header">
                        <span class="level-title" :style="{ color: lv.color }">{{ lv.value }}、{{ lv.label }}</span>
                      </div>
                      <div class="level-divider" :style="{ backgroundColor: lv.color }"></div>
                      <div class="level-form">
                        <div class="form-row">
                          <span class="form-label">多指标判据</span>
                          <div class="form-field">
                            <div class="expr-display" :class="{ 'expr-empty': !levelForm[lv.key].expression }">
                              {{ levelForm[lv.key].expression || '未设置' }}
                            </div>
                            <el-button type="primary" size="small" @click="openExprDialog(lv.key)">修改</el-button>
                          </div>
                        </div>
                        <div class="form-row">
                          <span class="form-label">告警持续时长</span>
                          <div class="form-field">
                            <el-input-number v-model="levelForm[lv.key].persistCount" :min="1" :max="100" size="small"/>
                            <span class="unit-text">次</span>
                          </div>
                        </div>
                        <div class="form-row">
                          <span class="form-label">静默数据周期</span>
                          <div class="form-field">
                            <el-input-number v-model="levelForm[lv.key].silencePeriod" :min="0" :max="1000"
                                             size="small"/>
                            <span class="unit-text">次</span>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                  <div class="save-bar">
                    <el-button type="primary" :loading="saving" @click="handleSaveForm('monitor')">保存判据</el-button>
                  </div>
                </template>
                <div v-else class="empty-hint">暂未配置判据，请点击"新增判据"创建</div>
              </template>
            </div>
          </div>
        </el-tab-pane>

        <!-- ========== 隐患点 ========== -->
        <el-tab-pane label="隐患点" name="hazardPoint">
          <div class="criteria-container">
            <div class="left-panel">
              <el-tree
                  :data="hazardPointTree"
                  :props="{ children: 'children', label: 'name' }"
                node-key="id"
                  highlight-current
                default-expand-all
                  @node-click="handleHazardPointNodeClick"
                  :expand-on-click-node="true"
              />
            </div>
            <div class="right-panel">
              <template v-if="!selectedHazardPointId">
                <div class="empty-hint">请选择左侧隐患点</div>
              </template>
              <template v-else>
                <div class="criteria-header">
                  <span class="sub-title">{{ selectedHazardPointName }} — 告警判据</span>
                  <el-button type="primary" size="small" @click="openCreateDialog('hazard')">新增判据</el-button>
                </div>
                <div v-if="currentHpCriteria.length > 1" class="criteria-selector">
                  <el-select v-model="selectedHpCriteriaId" placeholder="选择判据" @change="onHpCriteriaSelect">
                    <el-option v-for="c in currentHpCriteria" :key="c.id" :label="c.name" :value="c.id"/>
                  </el-select>
                </div>
                <template v-if="activeHpCriteria">
                  <div class="criteria-info-bar">
                    <el-switch :model-value="activeHpCriteria.isEnabled === 1" @change="handleToggle(activeHpCriteria)"
                               size="small"/>
                    <span class="criteria-name-text">{{ activeHpCriteria.name }}</span>
                    <span v-if="activeHpCriteria.version" class="version-text">v{{ activeHpCriteria.version }}</span>
                    <el-button size="small" type="danger" @click="handleDelete(activeHpCriteria)">删除</el-button>
                  </div>
                  <div class="level-sections">
                    <div v-for="lv in alarmLevels" :key="lv.value" class="level-section">
                      <div class="level-header">
                        <span class="level-title" :style="{ color: lv.color }">{{ lv.value }}、{{ lv.label }}</span>
                      </div>
                      <div class="level-divider" :style="{ backgroundColor: lv.color }"></div>
                      <div class="level-form">
                        <div class="form-row">
                          <span class="form-label">多指标判据</span>
                          <div class="form-field">
                            <div class="expr-display" :class="{ 'expr-empty': !levelForm[lv.key].expression }">
                              {{ levelForm[lv.key].expression || '未设置' }}
                            </div>
                            <el-button type="primary" size="small" @click="openExprDialog(lv.key)">修改</el-button>
                          </div>
                        </div>
                        <div class="form-row">
                          <span class="form-label">告警持续时长</span>
                          <div class="form-field">
                            <el-input-number v-model="levelForm[lv.key].persistCount" :min="1" :max="100" size="small"/>
                            <span class="unit-text">次</span>
                          </div>
                        </div>
                        <div class="form-row">
                          <span class="form-label">静默数据周期</span>
                          <div class="form-field">
                            <el-input-number v-model="levelForm[lv.key].silencePeriod" :min="0" :max="1000"
                                             size="small"/>
                            <span class="unit-text">次</span>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                  <div class="save-bar">
                    <el-button type="primary" :loading="saving" @click="handleSaveForm('hazard')">保存判据</el-button>
                  </div>
                </template>
                <div v-else class="empty-hint">暂未配置判据，请点击"新增判据"创建</div>
              </template>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- ========== 新增/编辑判据弹窗 ========== -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="700px" destroy-on-close>
      <el-form :model="formData" label-width="100px">
        <el-form-item label="判据名称" required>
          <el-input v-model="formData.name" placeholder="如：雨量蓝色预警"/>
        </el-form-item>
        <el-form-item label="监测内容" v-if="dialogContext === 'monitor'">
          <el-select v-model="formData.monitorContentId" placeholder="选择监测指标" clearable style="width: 100%">
            <el-option v-for="mc in monitorContents" :key="mc.id" :label="`${mc.name} (${mc.code})`" :value="mc.id"/>
          </el-select>
        </el-form-item>
        <el-form-item label="逻辑运算符">
          <el-radio-group v-model="formData.logicOperator">
            <el-radio value="AND">且 (AND)</el-radio>
            <el-radio value="OR">或 (OR)</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-divider content-position="left">四级告警阈值</el-divider>
        <el-form-item v-for="lv in alarmLevels" :key="lv.value" :label="lv.label">
          <el-row :gutter="8">
            <el-col :span="14">
              <el-input v-model="(formData as any)[`${lv.key}Expression`]" placeholder="阈值 (如 10 或 GT 10)"/>
            </el-col>
            <el-col :span="10">
              <el-input v-model="(formData as any)[`${lv.key}Description`]" placeholder="描述"/>
            </el-col>
          </el-row>
        </el-form-item>
        <el-divider content-position="left">防抖配置</el-divider>
        <el-form-item label="持续触发">
          <el-input-number v-model="formData.persistCount" :min="1" :max="100"/>
          <span class="form-hint">连续触发 N 次后才生成告警</span>
        </el-form-item>
        <el-form-item label="静默周期">
          <el-input-number v-model="formData.silencePeriod" :min="0" :max="1000"/>
          <span class="form-hint">周期内重复触发仅累加次数，0=不禁用</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- ========== 表达式编辑弹窗 (组件化) ========== -->
    <ExpressionEditDialog
        v-model="exprDialogVisible"
        :expression="exprDialogExpression"
        :description="exprDialogDescription"
        :indicators="exprDialogIndicators"
        @confirm="handleExprConfirm"
    />
  </div>
</template>

<script setup lang="ts">
import {computed, onMounted, reactive, ref} from 'vue'
import {ElMessage, ElMessageBox} from 'element-plus'
import {getMonitorTypeList, type MonitorTypeItem} from '@/api/monitorType'
import {getHazardPointGroups, getHazardPointPage} from '@/api/hazardPoint'
import {
  type AlarmCriteriaCreatePayload,
  type AlarmCriteriaItem,
  createCriteria,
  deleteCriteria,
  getCriteriaList,
  toggleCriteria as apiToggleCriteria,
  updateCriteria
} from '@/api/alarm'
import ExpressionEditDialog from './components/ExpressionEditDialog.vue'

// ── 常量 ──
const alarmLevels = [
  {value: 1, label: '蓝色告警（注意级）', key: 'blue', color: '#409eff'},
  {value: 2, label: '黄色告警（警示级）', key: 'yellow', color: '#e6a23c'},
  {value: 3, label: '橙色告警（警戒级）', key: 'orange', color: '#e6902c'},
  {value: 4, label: '红色告警（严重级）', key: 'red', color: '#f56c6c'},
]

// ── 状态 ──
const activeTab = ref('monitorType')
const selectedMonitorType = ref<number | null>(null)
const selectedMonitorTypeName = computed(() => {
  const t = monitorTypes.value.find(m => m.id === selectedMonitorType.value)
  return t?.name || ''
})
const selectedHazardPointId = ref<number | null>(null)
const selectedHazardPointName = ref('')

const monitorTypes = ref<MonitorTypeItem[]>([])
const monitorContents = ref<any[]>([])
const hazardPointTree = ref<any[]>([])

const allCriteria = ref<AlarmCriteriaItem[]>([])
const currentTypeCriteria = computed(() =>
    allCriteria.value.filter(c => c.monitorTypeId === selectedMonitorType.value && !c.hazardPointId)
)
const currentHpCriteria = computed(() =>
    allCriteria.value.filter(c => c.hazardPointId === selectedHazardPointId.value)
)

const selectedTypeCriteriaId = ref<number | null>(null)
const selectedHpCriteriaId = ref<number | null>(null)

const activeTypeCriteria = computed(() =>
    currentTypeCriteria.value.find(c => c.id === selectedTypeCriteriaId.value) || null
)
const activeHpCriteria = computed(() =>
    currentHpCriteria.value.find(c => c.id === selectedHpCriteriaId.value) || null
)

// ── 级别表单状态 (四级告警表达式 + 防抖) ──
interface LevelFormState {
  expression: string;
  persistCount: number;
  silencePeriod: number
}

function makeLevelFormState(): LevelFormState {
  return {expression: '', persistCount: 1, silencePeriod: 0}
}

const levelForm = reactive<Record<string, LevelFormState>>({
  blue: makeLevelFormState(), yellow: makeLevelFormState(),
  orange: makeLevelFormState(), red: makeLevelFormState(),
})

function initLevelForm(c: AlarmCriteriaItem) {
  levelForm.blue.expression = c.blueExpression || ''
  levelForm.yellow.expression = c.yellowExpression || ''
  levelForm.orange.expression = c.orangeExpression || ''
  levelForm.red.expression = c.redExpression || ''
  levelForm.blue.persistCount = c.persistCount ?? 1
  levelForm.yellow.persistCount = c.persistCount ?? 1
  levelForm.orange.persistCount = c.persistCount ?? 1
  levelForm.red.persistCount = c.persistCount ?? 1
  levelForm.blue.silencePeriod = c.silencePeriod ?? 0
  levelForm.yellow.silencePeriod = c.silencePeriod ?? 0
  levelForm.orange.silencePeriod = c.silencePeriod ?? 0
  levelForm.red.silencePeriod = c.silencePeriod ?? 0
}

// ── 表达式编辑弹窗 ──
const exprDialogVisible = ref(false)
const exprDialogLevelKey = ref('')
const exprDialogExpression = ref('')
const exprDialogDescription = ref('')

const exprDialogIndicators = computed(() =>
    monitorContents.value.map(mc => ({code: mc.code, name: mc.name, unit: mc.unit}))
)

function openExprDialog(key: string) {
  exprDialogLevelKey.value = key
  exprDialogExpression.value = levelForm[key].expression
  const c = activeTab.value === 'monitorType' ? activeTypeCriteria.value : activeHpCriteria.value
  const descKey = (key + 'Description') as keyof AlarmCriteriaItem
  exprDialogDescription.value = (c?.[descKey] as string) || ''
  exprDialogVisible.value = true
}

function handleExprConfirm(payload: { expression: string; description: string }) {
  const key = exprDialogLevelKey.value
  if (!key) return
  levelForm[key].expression = payload.expression
  // description 暂存到 levelForm 的 computed 来源；实际保存时由 handleSaveForm 写回
}

// ── 判据切换 ──
function onTypeCriteriaSelect(id: number) {
  const c = currentTypeCriteria.value.find(c => c.id === id);
  if (c) initLevelForm(c)
}

function onHpCriteriaSelect(id: number) {
  const c = currentHpCriteria.value.find(c => c.id === id);
  if (c) initLevelForm(c)
}

// ── 判据 CRUD 弹窗 ──
const dialogVisible = ref(false)
const dialogTitle = ref('')
const dialogContext = ref<'monitor' | 'hazard'>('monitor')
const editingId = ref<number | null>(null)
const saving = ref(false)

const formData = reactive<AlarmCriteriaCreatePayload & { id?: number }>({
  name: '',
  monitorTypeId: undefined, monitorContentId: undefined, hazardPointId: undefined,
  conditionsJson: '', logicOperator: 'AND',
  blueExpression: '', blueDescription: '',
  yellowExpression: '', yellowDescription: '',
  orangeExpression: '', orangeDescription: '',
  redExpression: '', redDescription: '',
  persistCount: 1, silencePeriod: 0, isEnabled: 1,
})

function resetForm() {
  Object.assign(formData, {
    name: '', monitorTypeId: undefined, monitorContentId: undefined, hazardPointId: undefined,
    conditionsJson: '', logicOperator: 'AND',
    blueExpression: '', blueDescription: '',
    yellowExpression: '', yellowDescription: '',
    orangeExpression: '', orangeDescription: '',
    redExpression: '', redDescription: '',
    persistCount: 1, silencePeriod: 0, isEnabled: 1,
  })
  editingId.value = null
}

function openCreateDialog(context: 'monitor' | 'hazard') {
  resetForm()
  dialogContext.value = context;
  dialogTitle.value = '新增告警判据'
  formData.monitorTypeId = context === 'monitor' ? selectedMonitorType.value! : undefined
  formData.hazardPointId = context === 'hazard' ? selectedHazardPointId.value! : undefined
  dialogVisible.value = true
}

function openEditDialog(c: AlarmCriteriaItem) {
  resetForm()
  dialogContext.value = c.hazardPointId ? 'hazard' : 'monitor'
  dialogTitle.value = '编辑告警判据'
  editingId.value = c.id
  formData.name = c.name;
  formData.monitorTypeId = c.monitorTypeId
  formData.monitorContentId = c.monitorContentId;
  formData.hazardPointId = c.hazardPointId
  formData.conditionsJson = c.conditionsJson || '';
  formData.logicOperator = c.logicOperator || 'AND'
  formData.blueExpression = c.blueExpression || '';
  formData.blueDescription = c.blueDescription || ''
  formData.yellowExpression = c.yellowExpression || '';
  formData.yellowDescription = c.yellowDescription || ''
  formData.orangeExpression = c.orangeExpression || '';
  formData.orangeDescription = c.orangeDescription || ''
  formData.redExpression = c.redExpression || '';
  formData.redDescription = c.redDescription || ''
  formData.persistCount = c.persistCount || 1;
  formData.silencePeriod = c.silencePeriod || 0
  formData.isEnabled = c.isEnabled
  dialogVisible.value = true
}

// ── 数据加载 ──
async function loadMonitorTypes() {
  try {
    const res: any = await getMonitorTypeList()
    monitorTypes.value = (res && res.rows) || (Array.isArray(res) ? res : res?.data) || []
  } catch { /* ok */
  }
}

async function loadHazardPointTree() {
  try {
    const groups: any = await getHazardPointGroups()
    const gList = groups?.data || groups?.rows || groups || []
    const hps: any = await getHazardPointPage({pageNum: 1, pageSize: 1000})
    const hpList = hps?.data?.rows || hps?.rows || hps || []
    hazardPointTree.value = gList.map((g: any) => ({
      id: 'g_' + g.id, name: g.name,
      children: hpList.filter((h: any) => h.groupId === g.id).map((h: any) => ({id: h.id, name: h.name})),
    }))
  } catch { /* ok */
  }
}

async function loadMonitorContents(typeId: number) {
  try {
    const api = await import('@/api/monitorType')
    const res: any = await api.getMonitorTypeDetail(typeId)
    monitorContents.value = res?.contents || res?.data?.contents || []
  } catch {
    monitorContents.value = []
  }
}

async function loadAllCriteria() {
  try {
    const res: any = await getCriteriaList({pageSize: 1000})
    allCriteria.value = res?.rows || res?.data?.rows || []
  } catch {
    allCriteria.value = []
  }
}

function getTypeCriteriaCount(typeId: number) {
  return allCriteria.value.filter(c => c.monitorTypeId === typeId && !c.hazardPointId).length
}

// ── 选择 ──
async function selectMonitorType(id: number) {
  selectedMonitorType.value = id;
  selectedHazardPointId.value = null
  await loadMonitorContents(id)
  const list = currentTypeCriteria.value
  selectedTypeCriteriaId.value = list.length > 0 ? list[0].id : null
  if (selectedTypeCriteriaId.value && list[0]) initLevelForm(list[0])
}

async function handleHazardPointNodeClick(node: any) {
  if (node.children && node.children.length > 0) return
  selectedHazardPointId.value = node.id;
  selectedHazardPointName.value = node.name
  selectedMonitorType.value = null
  const list = currentHpCriteria.value
  selectedHpCriteriaId.value = list.length > 0 ? list[0].id : null
  if (selectedHpCriteriaId.value && list[0]) initLevelForm(list[0])
}

async function handleTabChange() {
  selectedMonitorType.value = null;
  selectedHazardPointId.value = null
  selectedTypeCriteriaId.value = null;
  selectedHpCriteriaId.value = null
  await loadAllCriteria()
}

// ── 保存 ──
async function handleSave() {
  if (!formData.name) {
    ElMessage.warning('请输入判据名称');
    return
  }
  saving.value = true
  try {
    let targetId: number | null = null
    if (editingId.value) {
      await updateCriteria(editingId.value, formData)
      targetId = editingId.value;
      ElMessage.success('判据已更新')
    } else {
      const res: any = await createCriteria(formData)
      targetId = res?.id ?? res?.data?.id ?? null;
      ElMessage.success('判据已创建')
    }
    dialogVisible.value = false
    await loadAllCriteria()
    if (targetId) {
      if (dialogContext.value === 'monitor') selectedTypeCriteriaId.value = targetId
      else selectedHpCriteriaId.value = targetId
      const active = dialogContext.value === 'monitor' ? activeTypeCriteria.value : activeHpCriteria.value
      if (active) initLevelForm(active)
    }
  } catch (e: any) {
    ElMessage.error(e?.message || '操作失败')
  } finally {
    saving.value = false
  }
}

async function handleSaveForm(context: 'monitor' | 'hazard') {
  const active = context === 'monitor' ? activeTypeCriteria.value : activeHpCriteria.value
  if (!active) {
    ElMessage.warning('请先选择判据');
    return
  }
  saving.value = true
  try {
    const payload: AlarmCriteriaCreatePayload = {
      name: active.name,
      monitorTypeId: active.monitorTypeId, monitorContentId: active.monitorContentId,
      hazardPointId: active.hazardPointId,
      conditionsJson: active.conditionsJson, logicOperator: active.logicOperator,
      blueExpression: levelForm.blue.expression, yellowExpression: levelForm.yellow.expression,
      orangeExpression: levelForm.orange.expression, redExpression: levelForm.red.expression,
      persistCount: levelForm.blue.persistCount, silencePeriod: levelForm.blue.silencePeriod,
      isEnabled: active.isEnabled,
    }
    await updateCriteria(active.id, payload)
    ElMessage.success('判据已保存')
    await loadAllCriteria()
  } catch (e: any) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// ── 启用/删除 ──
async function handleToggle(c: AlarmCriteriaItem) {
  try {
    await apiToggleCriteria(c.id, c.isEnabled === 1 ? 0 : 1)
    ElMessage.success(c.isEnabled === 1 ? '已停用' : '已启用')
    await loadAllCriteria()
  } catch (e: any) {
    ElMessage.error(e?.message || '操作失败')
  }
}

async function handleDelete(c: AlarmCriteriaItem) {
  try {
    await ElMessageBox.confirm(`确定删除判据"${c.name}"？`, '确认删除', {type: 'warning'})
    await deleteCriteria(c.id)
    ElMessage.success('已删除')
    if (activeTab.value === 'monitorType') selectedTypeCriteriaId.value = null
    else selectedHpCriteriaId.value = null
    await loadAllCriteria()
  } catch { /* cancelled */
  }
}

// ── 初始化 ──
onMounted(async () => {
  await Promise.all([loadMonitorTypes(), loadHazardPointTree(), loadAllCriteria()])
})
</script>

<style scoped>
.page-body {
  padding: 0;
}

.criteria-tabs :deep(.el-tabs__header) {
  margin: 0 0 16px 0;
}

.criteria-container {
  display: flex;
  gap: 20px;
  min-height: 500px;
}
.left-panel {
  width: 260px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 8px;
  max-height: 620px;
  overflow-y: auto;
}

.type-list {
  display: flex;
  flex-direction: column;
}
.type-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  cursor: pointer;
  border-radius: 4px;
  margin-bottom: 2px;
  transition: all .2s;
}

.type-item:hover {
  background: #f5f7fa;
}

.type-item.active {
  background: #409eff;
  color: #fff;
}

.type-item.active .type-count {
  color: #fff;
}

.type-name {
  font-size: 14px;
}

.type-count {
  font-size: 12px;
  color: #999;
  background: #f0f0f0;
  border-radius: 10px;
  padding: 0 8px;
  min-width: 20px;
  text-align: center;
}

.right-panel {
  flex: 1;
  border: none;
  border-radius: 4px;
  padding: 20px;
  overflow-y: auto;
  max-height: 620px;
}

.empty-hint {
  text-align: center;
  color: #999;
  padding: 60px 0;
  font-size: 14px;
}

.criteria-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.sub-title {
  font-size: 16px;
  font-weight: 500;
}

.criteria-selector {
  margin-bottom: 12px;
}

.criteria-selector .el-select {
  width: 100%;
}

.criteria-info-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 20px;
  padding: 12px 16px;
  background: transparent;
  border-radius: 6px;
}

.criteria-name-text {
  font-weight: 500;
  flex: 1;
}

.version-text {
  font-size: 12px;
  color: #409eff;
}

.level-sections {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.level-section {
  background: transparent;
  border-radius: 8px;
  overflow: hidden;
}

.level-header {
  padding: 20px 24px 10px;
}

.level-title {
  font-size: 16px;
  font-weight: 600;
}

.level-divider {
  height: 2px;
  margin: 0 24px;
}

.level-form {
  padding: 20px 24px;
}

.form-row {
  display: flex;
  align-items: center;
  margin-bottom: 22px;
}

.form-row:last-child {
  margin-bottom: 0;
}

.form-label {
  width: 110px;
  font-size: 14px;
  color: #606266;
  flex-shrink: 0;
}

.form-field {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
}

.expr-display {
  flex: 1;
  padding: 8px 12px;
  font-size: 14px;
  font-family: 'Consolas', 'Courier New', monospace;
  background: rgba(0, 0, 0, 0.03);
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  color: #303133;
  min-height: 20px;
  line-height: 20px;
  word-break: break-all;
}

.expr-display.expr-empty {
  color: #c0c4cc;
  font-family: inherit;
}

.unit-text {
  font-size: 14px;
  color: #909399;
  white-space: nowrap;
}

.save-bar {
  display: flex;
  justify-content: flex-end;
  padding-top: 8px;
  border-top: 1px solid #ebeef5;
  margin-top: 4px;
}

.form-hint {
  margin-left: 10px;
  font-size: 12px;
  color: #999;
}
</style>
