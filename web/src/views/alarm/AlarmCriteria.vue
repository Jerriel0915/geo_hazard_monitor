<template>
  <div class="alarm-criteria-page">
    <!-- 页头 -->
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">告警判据</h2>
        <span class="page-subtitle">监测阈值与多级告警规则配置</span>
      </div>
    </div>
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
                  <span v-if="getTypeCriteriaCount(item.id) > 0" class="type-badge configured" title="已配置判据">
                    <el-icon :size="16"><CircleCheckFilled /></el-icon>
                  </span>
                  <span v-else class="type-badge unconfigured" title="未配置判据">
                    <el-icon :size="16"><WarningFilled /></el-icon>
                  </span>
                </div>
              </div>
            </div>
            <div class="right-panel">
              <CriteriaDetailPanel
                  v-if="selectedMonitorType"
                  :title="selectedMonitorTypeName"
                  :criteria-list="currentTypeCriteria"
                  :context="'monitor'"
                  :alarm-levels="alarmLevels"
                  :monitor-contents="monitorContents"
                  :saving="saving"
                  :indicator-tree="indicatorTree"
                  :node-map="nodeMap"
                  @toggle="handleToggle"
                  @save-form="handleSaveForm('monitor')"
                  v-model:selected-id="selectedTypeCriteriaId"
                  :level-form="levelForm"
              />
              <el-empty v-else description="请在左侧选择一个监测类型，查看和编辑告警判据规则" :image-size="140" />
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
                  :indicator-tree="indicatorTree"
                  :node-map="nodeMap"
                  @toggle="handleToggle"
                  @save-form="handleSaveForm('hazard')"
                  v-model:selected-id="selectedHpCriteriaId"
                  :level-form="levelForm"
              />
              <el-empty v-else description="请依次选择隐患点 → 设备 → 传感器" :image-size="140" />
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import {
  toggleCriteria as apiToggleCriteria,
  createCriteria,
  deleteCriteria,
  getCriteriaList,
  updateCriteria,
  type AlarmCriteriaCreatePayload,
  type AlarmCriteriaItem
} from '@/api/alarm'
import { getBoundDevices, getHazardPointGroups, getHazardPointPage } from '@/api/hazardPoint'
import { getMonitorTypeList, type MonitorTypeItem } from '@/api/monitorType'
import { getDeviceSensors, getSensorDetail } from '@/api/sensor'
import { ElMessage } from 'element-plus'
import { CircleCheckFilled, WarningFilled } from '@element-plus/icons-vue'
import { computed, onMounted, reactive, ref } from 'vue'
import CriteriaDetailPanel from './components/CriteriaDetailPanel.vue'
import { useIndicatorTree, type ConditionGroup, type LevelFormState } from './composables/useIndicatorTree'

// ── 常量 ──
const alarmLevels = [
  {value: 1, label: '蓝色告警（注意级）', key: 'blue', color: '#409eff'},
  {value: 2, label: '黄色告警（警示级）', key: 'yellow', color: '#e6a23c'},
  {value: 3, label: '橙色告警（警戒级）', key: 'orange', color: '#e6902c'},
  {value: 4, label: '红色告警（严重级）', key: 'red', color: '#f56c6c'},
]

// ── Tab ──
const activeTab = ref('monitorType')

// ── 指标树 ──
const {treeData: indicatorTree, nodeMap, buildFromMonitorType, clear: clearIndicatorTree} = useIndicatorTree()

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
const selectedHpCriteriaId = ref<number | null>(null)

// ── 判据数据 ──
const allCriteria = ref<AlarmCriteriaItem[]>([])
const currentTypeCriteria = computed(() => allCriteria.value.filter(c => c.monitorTypeId === selectedMonitorType.value && !c.hazardPointId))
const currentHpCriteria = computed(() => {
  if (!cascade.hazardPointId || !cascade.sensorMeta?.monitorContentId) return []
  return allCriteria.value.filter(c => c.hazardPointId === cascade.hazardPointId && c.monitorContentId === cascade.sensorMeta.monitorContentId)
})

// ── 级别表单 ──
const emptyLevel = (): LevelFormState => ({
  groups: [], groupLogic: 'AND', persistCount: 1, silencePeriod: 0, description: ''
})
const levelForm = reactive<Record<string, LevelFormState>>({
  blue: emptyLevel(), yellow: emptyLevel(), orange: emptyLevel(), red: emptyLevel(),
})

function resetLevelForm() {
  for (const key of ['blue', 'yellow', 'orange', 'red'] as const) {
    Object.assign(levelForm[key], emptyLevel())
  }
}

/** 迁移旧 subject（无点分前缀）为 payload.current.xxx */
function migrateSubject(subject: string): string {
  if (!subject) return subject
  if (subject.includes('.')) return subject
  return `payload.current.${subject}`
}

/** 迁移旧格式 conditions → groups 结构 */
function migrateToGroups(lc: any): { groups: ConditionGroup[]; groupLogic: 'AND' | 'OR' } {
  if (Array.isArray(lc?.groups)) {
    return { groups: lc.groups, groupLogic: lc.groupLogic || 'AND' }
  }
  const conditions = (lc?.conditions || []).map((c: any) => ({
    ...c,
    subject: migrateSubject(c.subject),
  }))
  if (conditions.length === 0) return { groups: [], groupLogic: 'AND' }
  return { groups: [{ conditions, logicOperator: lc?.logicOperator || 'AND' }], groupLogic: 'AND' }
}

function initLevelForm(c: AlarmCriteriaItem) {
  let config: Record<string, any> = {}
  try {
    if (c.levelConfig && c.levelConfig !== '{}') config = JSON.parse(c.levelConfig)
  } catch {}
  for (const key of ['blue', 'yellow', 'orange', 'red'] as const) {
    const lc = config[key]
    const migrated = migrateToGroups(lc)
    Object.assign(levelForm[key], {
      groups: migrated.groups,
      groupLogic: migrated.groupLogic,
      persistCount: lc?.persistCount ?? c.persistCount ?? 1,
      silencePeriod: lc?.silencePeriod ?? c.silencePeriod ?? 0,
      description: lc?.description || '',
    })
  }
}

// ── 级联选择 ──
async function onHpSelect(val: number | string | null) {
  if (!val || String(val).startsWith('g_')) return
  const id = Number(val)
  cascade.hazardPointId = id; cascade.deviceId = null; cascade.sensorId = null; cascade.sensorMeta = null
  cascadeSensors.value = []
  try {
    const res: any = await getBoundDevices(String(id))
    cascadeDevices.value = res?.data || res?.rows || res || []
  } catch { cascadeDevices.value = [] }
}

async function onDeviceSelect(deviceId: number) {
  cascade.deviceId = deviceId; cascade.sensorId = null; cascade.sensorMeta = null
  try {
    const res: any = await getDeviceSensors(deviceId)
    cascadeSensors.value = res?.data || res?.rows || res || []
  } catch { cascadeSensors.value = [] }
}

async function onSensorSelect(sensorId: number) {
  cascade.sensorId = sensorId
  try {
    const res: any = await getSensorDetail(sensorId)
    const s = res?.data || res
    const contentId = s?.attrList?.[0]?.monitorContentId
    const typeId = s?.monitorTypeId
    cascade.sensorMeta = {
      sensorName: s?.name || s?.sensorName || s?.sensorCode || '',
      monitorContentId: contentId,
      contentName: s?.monitorTypeName || '',
      monitorTypeId: typeId
    }
    if (typeId) {
      await loadMonitorContents(typeId)
      await buildFromMonitorType(typeId)
    }
    await loadAllCriteria()
    const list = currentHpCriteria.value
    selectedHpCriteriaId.value = list.length > 0 ? list[0].id : null
    if (list.length > 0 && list[0]) initLevelForm(list[0])
    else resetLevelForm()
  } catch { cascade.sensorMeta = null }
}

// ── 数据加载 ──
async function loadMonitorTypes() {
  try {
    const res: any = await getMonitorTypeList()
    monitorTypes.value = (res && res.rows) || (Array.isArray(res) ? res : res?.data) || []
  } catch {}
}

async function loadHazardPointTree() {
  try {
    const groups: any = await getHazardPointGroups()
    const gList = groups?.data || groups?.rows || groups || []
    const hps: any = await getHazardPointPage({pageNum: 1, pageSize: 1000})
    const hpList = hps?.data?.rows || hps?.rows || hps || []
    hazardPointTree.value = gList.map((g: any) => ({
      id: 'g_' + g.id, name: g.name, isGroup: true,
      children: hpList.filter((h: any) => h.groupId === g.id).map((h: any) => ({id: h.id, name: h.name}))
    }))
  } catch {}
}

async function loadMonitorContents(typeId: number) {
  try {
    const api = await import('@/api/monitorType')
    const res: any = await api.getMonitorTypeDetail(typeId)
    monitorContents.value = res?.contents || res?.data?.contents || []
  } catch { monitorContents.value = [] }
}

async function loadAllCriteria() {
  try {
    const res: any = await getCriteriaList({pageSize: 1000})
    allCriteria.value = res?.rows || res?.data?.rows || []
  } catch { allCriteria.value = [] }
}

function getTypeCriteriaCount(typeId: number) {
  return allCriteria.value.filter(c => c.monitorTypeId === typeId && !c.hazardPointId).length
}

// ── 选择 ──
async function selectMonitorType(id: number) {
  selectedMonitorType.value = id
  await loadMonitorContents(id)
  await buildFromMonitorType(id)
  const list = currentTypeCriteria.value
  selectedTypeCriteriaId.value = list.length > 0 ? list[0].id : null
  if (list.length > 0 && list[0]) initLevelForm(list[0])
  else resetLevelForm()
}

async function handleTabChange() {
  selectedMonitorType.value = null
  cascade.hazardPointId = null; cascade.deviceId = null; cascade.sensorId = null; cascade.sensorMeta = null
  selectedTypeCriteriaId.value = null; selectedHpCriteriaId.value = null
  clearIndicatorTree()
  resetLevelForm()
  await loadAllCriteria()
}

// ── 保存逻辑：自动判断 新增 / 更新 / 删除 ──

function isFormEmpty(): boolean {
  return (['blue', 'yellow', 'orange', 'red'] as const).every(key => {
    const lf = levelForm[key]
    return lf.groups.length === 0 && !lf.description
  })
}

function buildLevelConfigFromForm(): string {
  const config: Record<string, any> = {}
  for (const key of ['blue', 'yellow', 'orange', 'red'] as const) {
    const lf = levelForm[key]
    if (lf.groups.length === 0 && !lf.description) continue
    config[key] = {
      groupLogic: lf.groupLogic, groups: lf.groups,
      description: lf.description, persistCount: lf.persistCount, silencePeriod: lf.silencePeriod,
    }
  }
  return JSON.stringify(config)
}

function buildCriteriaName(context: 'monitor' | 'hazard'): string {
  if (context === 'monitor') return selectedMonitorTypeName.value + ' — 告警判据'
  return (cascade.sensorMeta?.sensorName || '传感器') + ' — 设备判据'
}

function getActiveCriteria(context: 'monitor' | 'hazard'): AlarmCriteriaItem | undefined {
  const list = context === 'monitor' ? currentTypeCriteria.value : currentHpCriteria.value
  const selectedId = context === 'monitor' ? selectedTypeCriteriaId.value : selectedHpCriteriaId.value
  return list.find(c => c.id === selectedId) || list[0]
}

async function handleSaveForm(context: 'monitor' | 'hazard') {
  const existing = getActiveCriteria(context)
  const formEmpty = isFormEmpty()

  // 无数据 + 无已有判据 → 无操作
  if (formEmpty && !existing) {
    ElMessage.info('请先添加条件')
    return
  }

  // 清空后保存 → 删除已有判据
  if (formEmpty && existing) {
    saving.value = true
    try {
      await deleteCriteria(existing.id)
      ElMessage.success('判据已删除')
      if (context === 'monitor') selectedTypeCriteriaId.value = null
      else selectedHpCriteriaId.value = null
      resetLevelForm()
      await loadAllCriteria()
    } catch (e: any) {
      ElMessage.error(e?.message || '删除失败')
    } finally { saving.value = false }
    return
  }

  // 有数据 + 无已有判据 → 新增
  if (!formEmpty && !existing) {
    saving.value = true
    try {
      const payload: AlarmCriteriaCreatePayload = {
        name: buildCriteriaName(context),
        levelConfig: buildLevelConfigFromForm(),
        isEnabled: 1,
      }
      if (context === 'monitor') {
        payload.monitorTypeId = selectedMonitorType.value!
      } else {
        payload.hazardPointId = cascade.hazardPointId!
        payload.monitorContentId = cascade.sensorMeta?.monitorContentId
        payload.monitorTypeId = cascade.sensorMeta?.monitorTypeId
      }
      const res: any = await createCriteria(payload)
      const newId = res?.id ?? res?.data?.id ?? null
      if (context === 'monitor') selectedTypeCriteriaId.value = newId
      else selectedHpCriteriaId.value = newId
      ElMessage.success('判据已创建')
      await loadAllCriteria()
    } catch (e: any) {
      ElMessage.error(e?.message || '创建失败')
    } finally { saving.value = false }
    return
  }

  // 有数据 + 有已有判据 → 更新
  saving.value = true
  try {
    const payload: AlarmCriteriaCreatePayload = {
      name: existing!.name,
      monitorTypeId: existing!.monitorTypeId,
      monitorContentId: existing!.monitorContentId,
      hazardPointId: existing!.hazardPointId,
      levelConfig: buildLevelConfigFromForm(),
      persistCount: levelForm.blue.persistCount,
      silencePeriod: levelForm.blue.silencePeriod,
      isEnabled: existing!.isEnabled,
    }
    await updateCriteria(existing!.id, payload)
    ElMessage.success('判据已保存')
    await loadAllCriteria()
  } catch (e: any) {
    ElMessage.error(e?.message || '保存失败')
  } finally { saving.value = false }
}

// ── 启停 ──
const saving = ref(false)

async function handleToggle(c: AlarmCriteriaItem) {
  try {
    await apiToggleCriteria(c.id, c.isEnabled === 1 ? 0 : 1)
    ElMessage.success(c.isEnabled === 1 ? '已停用' : '已启用')
    await loadAllCriteria()
  } catch (e: any) {
    ElMessage.error(e?.message || '操作失败')
  }
}

// ── 初始化 ──
onMounted(async () => {
  await Promise.all([loadMonitorTypes(), loadHazardPointTree(), loadAllCriteria()])
})
</script>

<style scoped>
.alarm-criteria-page {
  padding: 20px;
  background: #f0f2f5;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-sizing: border-box;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  flex-shrink: 0;
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

.page-body {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.criteria-tabs {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.criteria-tabs :deep(.el-tabs__header) {
  margin: 0 0 16px 0;
  flex-shrink: 0;
}

.criteria-tabs :deep(.el-tabs__content) {
  flex: 1;
  min-height: 0;
}

.criteria-tabs :deep(.el-tab-pane) {
  height: 100%;
}

.criteria-container {
  display: flex;
  gap: 20px;
  height: 100%;
}

.left-panel {
  width: 260px;
  background: #fff;
  border: 1px solid #e5e6eb;
  border-radius: 8px;
  padding: 8px;
  overflow-y: auto;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.06);
  flex-shrink: 0;
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

.type-item.active .type-badge.configured {
  color: #b3e19d;
}

.type-item.active .type-badge.unconfigured {
  color: #ffcda4;
}

.type-name {
  font-size: 14px;
}

.type-badge {
  display: inline-flex;
  align-items: center;
  flex-shrink: 0;
}

.type-badge.configured {
  color: #67c23a;
}

.type-badge.unconfigured {
  color: #e6a23c;
}

.right-panel {
  flex: 1;
  background: #fff;
  border: 1px solid #e5e6eb;
  border-radius: 8px;
  padding: 20px;
  overflow-y: auto;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.06);
  min-width: 0;
}

.empty-hint {
  text-align: center;
  color: #999;
  padding: 40px 0;
  font-size: 14px;
}
</style>
