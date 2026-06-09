<template>
  <div class="page-content">
    <div class="page-title">告警判据</div>
    <div class="page-body">
      <el-tabs v-model="activeTab" class="criteria-tabs" @tab-change="handleTabChange">

        <!-- ========== 监测类型（兜底判据） ========== -->
        <el-tab-pane label="监测类型" name="monitorType">
          <div class="criteria-container">
            <div class="left-panel">
              <div class="type-list">
                <div v-if="monitorTypes.length === 0" class="empty-hint">加载中...</div>
                <div v-for="item in monitorTypes" :key="item.id" class="type-item"
                     :class="{ active: selectedMonitorType === item.id }" @click="selectMonitorType(item.id)">
                  <span class="type-name">{{ item.name }}</span>
                  <span class="type-count">{{ getTypeCriteriaCount(item.id) }}</span>
                </div>
              </div>
            </div>
            <div class="right-panel">
              <CriteriaDetailPanel
                  v-if="selectedMonitorType"
                  :title="selectedMonitorTypeName + ' — 通用判据'"
                  :criteria-list="currentTypeCriteria"
                  :context="'monitor'"
                  :alarm-levels="alarmLevels"
                  :monitor-contents="monitorContents"
                  :saving="saving"
                  @create="openCreateDialog('monitor')"
                  @edit="openEditDialog"
                  @delete="handleDelete"
                  @toggle="handleToggle"
                  @save-form="handleSaveForm('monitor')"
                  @expr-open="openExprDialog"
                  @expr-confirm="handleExprConfirm"
                  v-model:selected-id="selectedTypeCriteriaId"
                  v-model:level-form="levelForm"
              />
              <div v-else class="empty-hint">请选择左侧监测类型</div>
            </div>
          </div>
        </el-tab-pane>

        <!-- ========== 隐患点（设备判据） ========== -->
        <el-tab-pane label="隐患点" name="hazardPoint">
          <div class="criteria-container">
            <div class="left-panel cascade-panel">
              <div class="cascade-row">
                <span class="cascade-label">隐患点</span>
                <el-tree-select
                    v-model="cascade.hazardPointId"
                  :data="hazardPointTree"
                    :props="{ children: 'children', label: 'name', value: 'id', disabled: 'isGroup' }"
                    placeholder="选择隐患点"
                    filterable
                    check-strictly
                    style="width: 100%"
                    @update:model-value="onHpSelect"
                />
              </div>
              <div class="cascade-row">
                <span class="cascade-label">设备</span>
                <el-select v-model="cascade.deviceId" placeholder="选择设备" filterable style="width: 100%"
                           :disabled="!cascade.hazardPointId" @change="onDeviceSelect">
                  <el-option v-for="d in cascadeDevices" :key="d.deviceId" :label="d.deviceName || d.deviceCode"
                             :value="d.deviceId"/>
                </el-select>
              </div>
              <div class="cascade-row">
                <span class="cascade-label">传感器</span>
                <el-select v-model="cascade.sensorId" placeholder="选择传感器" filterable style="width: 100%"
                           :disabled="!cascade.deviceId" @change="onSensorSelect">
                  <el-option v-for="s in cascadeSensors" :key="s.id" :label="s.name || s.sensorName || s.sensorCode"
                             :value="s.id"/>
                </el-select>
              </div>
              <div v-if="cascade.sensorMeta" class="cascade-meta">
                监测类型: {{ cascade.sensorMeta.contentName || '未知' }}
              </div>
            </div>
            <div class="right-panel">
              <CriteriaDetailPanel
                  v-if="cascade.sensorId && cascade.hazardPointId"
                  :title="(cascade.sensorMeta?.sensorName || '传感器') + ' — 设备判据'"
                  :criteria-list="currentHpCriteria"
                  :context="'hazard'"
                  :alarm-levels="alarmLevels"
                  :monitor-contents="monitorContents"
                  :saving="saving"
                  @create="openCreateDialog('hazard')"
                  @edit="openEditDialog"
                  @delete="handleDelete"
                  @toggle="handleToggle"
                  @save-form="handleSaveForm('hazard')"
                  @expr-open="openExprDialog"
                  @expr-confirm="handleExprConfirm"
                  v-model:selected-id="selectedHpCriteriaId"
                  v-model:level-form="levelForm"
              />
              <div v-else class="empty-hint">请依次选择隐患点 → 设备 → 传感器</div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- ========== 新增弹窗（仅名称） ========== -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="450px" destroy-on-close>
      <el-form :model="formData" label-width="100px">
        <el-form-item label="判据名称" required>
          <el-input v-model="formData.name" placeholder="如：雨量蓝色预警" @keyup.enter="handleSave"/>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">确定</el-button>
      </template>
    </el-dialog>

    <!-- ========== 表达式编辑弹窗 ========== -->
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
import {getBoundDevices, getHazardPointGroups, getHazardPointPage} from '@/api/hazardPoint'
import {getDeviceSensors, getSensorDetail} from '@/api/sensor'
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
import CriteriaDetailPanel from './components/CriteriaDetailPanel.vue'

// ── 常量 ──
const alarmLevels = [
  {value: 1, label: '蓝色告警（注意级）', key: 'blue', color: '#409eff'},
  {value: 2, label: '黄色告警（警示级）', key: 'yellow', color: '#e6a23c'},
  {value: 3, label: '橙色告警（警戒级）', key: 'orange', color: '#e6902c'},
  {value: 4, label: '红色告警（严重级）', key: 'red', color: '#f56c6c'},
]

// ── Tab ──
const activeTab = ref('monitorType')

// ── 监测类型 ──
const selectedMonitorType = ref<number | null>(null)
const selectedMonitorTypeName = computed(() => monitorTypes.value.find(m => m.id === selectedMonitorType.value)?.name || '')
const monitorTypes = ref<MonitorTypeItem[]>([])
const monitorContents = ref<any[]>([])
const selectedTypeCriteriaId = ref<number | null>(null)

// ── 隐患点级联 ──
const cascade = reactive({
  hazardPointId: null as number | null,
  deviceId: null as number | null,
  sensorId: null as number | null,
  sensorMeta: null as any
})
const cascadeDevices = ref<any[]>([])
const cascadeSensors = ref<any[]>([])
const hazardPointTree = ref<any[]>([])
const selectedHazardPointName = ref('')
const selectedHazardPointId = computed(() => cascade.hazardPointId)
const selectedHpCriteriaId = ref<number | null>(null)

// ── 判据数据 ──
const allCriteria = ref<AlarmCriteriaItem[]>([])
const currentTypeCriteria = computed(() => allCriteria.value.filter(c => c.monitorTypeId === selectedMonitorType.value && !c.hazardPointId))
const currentHpCriteria = computed(() => {
  if (!cascade.hazardPointId || !cascade.sensorMeta?.monitorContentId) return []
  return allCriteria.value.filter(c => c.hazardPointId === cascade.hazardPointId && c.monitorContentId === cascade.sensorMeta.monitorContentId)
})

// ── 级别表单 ──
interface LevelFormState {
  expression: string;
  persistCount: number;
  silencePeriod: number
}
const levelForm = reactive<Record<string, LevelFormState>>({
  blue: {expression: '', persistCount: 1, silencePeriod: 0},
  yellow: {expression: '', persistCount: 1, silencePeriod: 0},
  orange: {expression: '', persistCount: 1, silencePeriod: 0},
  red: {expression: '', persistCount: 1, silencePeriod: 0},
})

function opToSymbol(op: string): string {
  const m: Record<string, string> = {GT: '>', GTE: '>=', LT: '<', LTE: '<=', EQ: '==', NEQ: '!=', BETWEEN: '~'}
  return m[op] || op
}

function initLevelForm(c: AlarmCriteriaItem) {
  let config: Record<string, { conditions?: any[]; description?: string }> = {}
  try {
    if (c.levelConfig && c.levelConfig !== '{}') config = JSON.parse(c.levelConfig)
  } catch {
  }
  for (const key of ['blue', 'yellow', 'orange', 'red'] as const) {
    const lc = config[key]
    levelForm[key].expression = lc?.conditions?.map((cond: any) =>
        `${cond.subject || ''} ${opToSymbol(cond.operator)} ${cond.threshold ?? ''}`).join(' ') || ''
    levelForm[key].persistCount = c.persistCount ?? 1
    levelForm[key].silencePeriod = c.silencePeriod ?? 0
  }
}

// ── 表达式弹窗 ──
const exprDialogVisible = ref(false);
const exprDialogLevelKey = ref('')
const exprDialogExpression = ref('');
const exprDialogDescription = ref('')
const exprDialogIndicators = computed(() => monitorContents.value.map(mc => ({
  code: mc.code,
  name: mc.name,
  unit: mc.unit
})))

function openExprDialog(key: string) {
  exprDialogLevelKey.value = key;
  exprDialogExpression.value = levelForm[key].expression;
  exprDialogVisible.value = true
}

function handleExprConfirm(p: { expression: string; description: string }) {
  if (exprDialogLevelKey.value) levelForm[exprDialogLevelKey.value].expression = p.expression
}

// ── 级联选择 ──
async function onHpSelect(val: number | string | null) {
  if (!val || String(val).startsWith('g_')) return  // skip group nodes
  const id = Number(val)
  cascade.hazardPointId = id;
  cascade.deviceId = null;
  cascade.sensorId = null;
  cascade.sensorMeta = null;
  cascadeSensors.value = []
  try {
    const res: any = await getBoundDevices(String(id));
    cascadeDevices.value = res?.data || res?.rows || res || []
  } catch {
    cascadeDevices.value = []
  }
}

async function onDeviceSelect(deviceId: number) {
  cascade.deviceId = deviceId;
  cascade.sensorId = null;
  cascade.sensorMeta = null
  try {
    const res: any = await getDeviceSensors(deviceId);
    cascadeSensors.value = res?.data || res?.rows || res || []
  } catch {
    cascadeSensors.value = []
  }
}

async function onSensorSelect(sensorId: number) {
  cascade.sensorId = sensorId
  try {
    const res: any = await getSensorDetail(sensorId);
    const s = res?.data || res
    const contentId = s?.attrList?.[0]?.monitorContentId
    const typeId = s?.monitorTypeId
    cascade.sensorMeta = {
      sensorName: s?.name || s?.sensorName || s?.sensorCode || '',
      monitorContentId: contentId,
      contentName: s?.monitorTypeName || '',
      monitorTypeId: typeId
    }
    if (typeId) await loadMonitorContents(typeId)
    await loadAllCriteria()
    const list = currentHpCriteria.value;
    selectedHpCriteriaId.value = list.length > 0 ? list[0].id : null
    if (selectedHpCriteriaId.value && list[0]) initLevelForm(list[0])
  } catch {
    cascade.sensorMeta = null
  }
}

// ── 判据弹窗（仅名称） ──
const dialogVisible = ref(false);
const dialogTitle = ref('');
const dialogContext = ref<'monitor' | 'hazard'>('monitor')
const editingId = ref<number | null>(null);
const saving = ref(false)
const formData = reactive<AlarmCriteriaCreatePayload & { id?: number }>({
  name: '',
  levelConfig: '{}',
  persistCount: 1,
  silencePeriod: 0,
  isEnabled: 1
})

function resetForm() {
  Object.assign(formData, {name: '', levelConfig: '{}', persistCount: 1, silencePeriod: 0, isEnabled: 1});
  editingId.value = null
}

function openCreateDialog(ctx: 'monitor' | 'hazard') {
  resetForm();
  dialogContext.value = ctx;
  dialogTitle.value = '新增告警判据'
  if (ctx === 'monitor') {
    formData.monitorTypeId = selectedMonitorType.value!;
    formData.hazardPointId = undefined
  } else {
    formData.hazardPointId = cascade.hazardPointId!;
    formData.monitorContentId = cascade.sensorMeta?.monitorContentId;
    formData.monitorTypeId = cascade.sensorMeta?.monitorTypeId
  }
  dialogVisible.value = true
}

function openEditDialog(c: AlarmCriteriaItem) {
  resetForm();
  dialogContext.value = c.hazardPointId ? 'hazard' : 'monitor';
  dialogTitle.value = '编辑判据名称';
  editingId.value = c.id;
  formData.name = c.name;
  dialogVisible.value = true
}

// ── 数据加载 ──
async function loadMonitorTypes() {
  try {
    const res: any = await getMonitorTypeList();
    monitorTypes.value = (res && res.rows) || (Array.isArray(res) ? res : res?.data) || []
  } catch {
  }
}
async function loadHazardPointTree() {
  try {
    const groups: any = await getHazardPointGroups();
    const gList = groups?.data || groups?.rows || groups || []
    const hps: any = await getHazardPointPage({pageNum: 1, pageSize: 1000});
    const hpList = hps?.data?.rows || hps?.rows || hps || []
    hazardPointTree.value = gList.map((g: any) => ({
      id: 'g_' + g.id,
      name: g.name,
      isGroup: true,
      children: hpList.filter((h: any) => h.groupId === g.id).map((h: any) => ({id: h.id, name: h.name}))
    }))
  } catch {
  }
}
async function loadMonitorContents(typeId: number) {
  try {
    const api = await import('@/api/monitorType');
    const res: any = await api.getMonitorTypeDetail(typeId);
    monitorContents.value = res?.contents || res?.data?.contents || []
  } catch {
    monitorContents.value = []
  }
}

async function loadAllCriteria() {
  try {
    const res: any = await getCriteriaList({pageSize: 1000});
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
  await loadMonitorContents(id);
  const list = currentTypeCriteria.value;
  selectedTypeCriteriaId.value = list.length > 0 ? list[0].id : null;
  if (selectedTypeCriteriaId.value && list[0]) initLevelForm(list[0])
}

async function handleTabChange() {
  selectedMonitorType.value = null;
  cascade.hazardPointId = null;
  cascade.deviceId = null;
  cascade.sensorId = null;
  cascade.sensorMeta = null;
  selectedTypeCriteriaId.value = null;
  selectedHpCriteriaId.value = null;
  await loadAllCriteria()
}

// ── 保存 ──
async function handleSave() {
  if (!formData.name) {
    ElMessage.warning('请输入判据名称');
    return
  }
  ;saving.value = true;
  try {
    let tid: number | null = null;
    if (editingId.value) {
      await updateCriteria(editingId.value, formData);
      tid = editingId.value;
      ElMessage.success('已更新')
    } else {
      const res: any = await createCriteria(formData);
      tid = res?.id ?? res?.data?.id ?? null;
      ElMessage.success('已创建，请在下方编辑判据条件')
    }
    ;dialogVisible.value = false;
    await loadAllCriteria();
    if (tid) {
      if (dialogContext.value === 'monitor') selectedTypeCriteriaId.value = tid; else selectedHpCriteriaId.value = tid;
      initLevelForm(allCriteria.value.find(c => c.id === tid)!)
    }
  } catch (e: any) {
    ElMessage.error(e?.message || '操作失败')
  } finally {
    saving.value = false
  }
}

function buildLevelConfigFromForm(): string {
  const config: Record<string, { logicOperator: string; conditions: any[]; description: string }> = {}
  for (const key of ['blue', 'yellow', 'orange', 'red'] as const) {
    const expr = levelForm[key].expression.trim();
    if (!expr) continue
    const parts = expr.split(/\s+(且|或)\s+/i);
    const logicOp = expr.match(/\s+(且)\s+/i) ? 'AND' : 'OR'
    config[key] = {
      logicOperator: logicOp, conditions: parts.filter(p => p !== '且' && p !== '或').map(part => {
        const m = part.match(/^(.+?)\s*(>=|<=|!=|==|>|<)\s*([0-9.]+)$/);
        if (m) return {subject: m[1].trim(), operator: mapOp(m[2]), threshold: parseFloat(m[3])};
        return {subject: part.trim(), operator: 'GT', threshold: 0}
      }), description: ''
    }
  }
  return JSON.stringify(config)
}

function mapOp(s: string): string {
  const m: Record<string, string> = {'>': 'GT', '<': 'LT', '>=': 'GTE', '<=': 'LTE', '==': 'EQ', '!=': 'NEQ'};
  return m[s] || 'GT'
}

async function handleSaveForm(context: 'monitor' | 'hazard') {
  const list = context === 'monitor' ? currentTypeCriteria.value : currentHpCriteria.value
  const active = list.find(c => c.id === (context === 'monitor' ? selectedTypeCriteriaId.value : selectedHpCriteriaId.value))
  if (!active) {
    ElMessage.warning('请先选择判据');
    return
  }
  ;saving.value = true
  try {
    const payload: AlarmCriteriaCreatePayload = {
      name: active.name,
      monitorTypeId: active.monitorTypeId,
      monitorContentId: active.monitorContentId,
      hazardPointId: active.hazardPointId,
      levelConfig: buildLevelConfigFromForm(),
      persistCount: levelForm.blue.persistCount,
      silencePeriod: levelForm.blue.silencePeriod,
      isEnabled: active.isEnabled
    };
    await updateCriteria(active.id, payload);
    ElMessage.success('判据已保存');
    await loadAllCriteria()
  } catch (e: any) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// ── 启停/删除 ──
async function handleToggle(c: AlarmCriteriaItem) {
  try {
    await apiToggleCriteria(c.id, c.isEnabled === 1 ? 0 : 1);
    ElMessage.success(c.isEnabled === 1 ? '已停用' : '已启用');
    await loadAllCriteria()
  } catch (e: any) {
    ElMessage.error(e?.message || '操作失败')
  }
}

async function handleDelete(c: AlarmCriteriaItem) {
  try {
    await ElMessageBox.confirm(`确定删除判据"${c.name}"？`, '确认删除', {type: 'warning'});
    await deleteCriteria(c.id);
    ElMessage.success('已删除');
    if (activeTab.value === 'monitorType') selectedTypeCriteriaId.value = null; else selectedHpCriteriaId.value = null;
    await loadAllCriteria()
  } catch {
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

.cascade-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 12px;
}

.cascade-row {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.cascade-label {
  font-size: 13px;
  color: #606266;
  font-weight: 500;
}

.cascade-meta {
  font-size: 12px;
  color: #409eff;
  padding: 8px;
  background: #ecf5ff;
  border-radius: 4px;
  margin-top: 4px;
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
</style>
