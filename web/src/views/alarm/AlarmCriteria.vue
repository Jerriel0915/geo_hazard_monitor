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
              <el-empty v-else description="请在左侧选择一个监测类型" :image-size="140" />
            </div>
          </div>
        </el-tab-pane>

        <!-- ========== 隐患点（设备判据） ========== -->
        <el-tab-pane label="隐患点" name="hazardPoint">
          <div class="hazard-three-col">

            <!-- 第一栏：隐患点树 -->
            <div class="col-hp-tree">
              <div class="col-header">隐患点</div>
              <el-tree
                  v-if="hazardPointTree.length > 0"
                  :data="hazardPointTree"
                  :props="{ children: 'children', label: 'name' }"
                  node-key="id"
                  highlight-current
                  default-expand-all
                  :expand-on-click-node="false"
                  @node-click="onHpNodeClick"
              />
              <div v-else class="empty-hint">加载中...</div>
            </div>

            <!-- 第二栏：设备列表 -->
            <div class="col-device-list">
              <div class="col-header">
                设备列表
                <span v-if="hpDevices.length > 0" class="col-header-count">{{ hpDevices.length }}</span>
              </div>
              <div v-if="!selectedHazardPointId" class="empty-hint">请先选择隐患点</div>
              <div v-else-if="hpDevicesLoading" class="empty-hint">加载中...</div>
              <div v-else-if="hpDevices.length === 0" class="empty-hint">暂无设备</div>
              <div v-else class="device-scroll">
                <div
                    v-for="device in hpDevices"
                    :key="device.deviceId"
                    class="device-capsule"
                    :class="{ selected: selectedDeviceId === device.deviceId }"
                    @click="onDeviceCardClick(device)"
                >
                  <div class="capsule-name-row">
                    <span class="capsule-name">{{ device.deviceName || device.deviceCode }}</span>
                    <span class="capsule-status" :class="device.onlineStatus">
                      <span class="capsule-status-dot"></span>
                      {{ device.onlineStatus === 'online' ? '在线' : '离线' }}
                    </span>
                  </div>
                  <div class="capsule-meta">
                    <span class="capsule-code">{{ device.deviceCode || '--' }}</span>
                  </div>
                  <div v-if="device.sensors && device.sensors.length" class="capsule-sensors">
                    <div v-for="sensor in device.sensors" :key="sensor.id || sensor.sensorCode" class="sensor-chip">
                      <span class="sensor-chip-name">{{ sensor.monitorTypeName || sensor.name }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- 第三栏：判据设置 -->
            <div class="col-criteria">
              <CriteriaDetailPanel
                  v-if="selectedDeviceId && currentHpCriteria.length >= 0"
                  :title="selectedDeviceName"
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
              <el-empty v-else description="请选择设备查看判据配置" :image-size="140" />
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
import { getDeviceSensors } from '@/api/sensor'
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
const {treeData: indicatorTree, nodeMap, buildFromMonitorType, buildFromSensors, clear: clearIndicatorTree} = useIndicatorTree()

// ── 监测类型 ──
const selectedMonitorType = ref<number | null>(null)
const selectedMonitorTypeName = computed(() => monitorTypes.value.find(m => m.id === selectedMonitorType.value)?.name || '')
const monitorTypes = ref<MonitorTypeItem[]>([])
const monitorContents = ref<any[]>([])
const selectedTypeCriteriaId = ref<number | null>(null)

// ── 隐患点（三栏） ──
const selectedHazardPointId = ref<number | null>(null)
const selectedDeviceId = ref<number | null>(null)
const selectedDeviceName = computed(() => {
  const d = hpDevices.value.find(d => d.deviceId === selectedDeviceId.value)
  return d ? (d.deviceName || d.deviceCode) + ' — 设备判据' : '设备判据'
})
const hpDevices = ref<any[]>([])
const hpDevicesLoading = ref(false)
const hazardPointTree = ref<any[]>([])
const selectedHpCriteriaId = ref<number | null>(null)

// 当前设备下第一个传感器的 monitorContentId，用于匹配判据
const currentSensorMeta = ref<any>(null)

// ── 判据数据 ──
const allCriteria = ref<AlarmCriteriaItem[]>([])
const currentTypeCriteria = computed(() => allCriteria.value.filter(c => c.monitorTypeId === selectedMonitorType.value && !c.hazardPointId))
const currentHpCriteria = computed(() => {
  if (!selectedHazardPointId.value || !currentSensorMeta.value?.monitorContentId) return []
  return allCriteria.value.filter(c =>
    c.hazardPointId === selectedHazardPointId.value &&
    c.monitorContentId === currentSensorMeta.value.monitorContentId
  )
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

function migrateSubject(subject: string): string {
  if (!subject) return subject
  if (subject.includes('.')) return subject
  return `payload.current.${subject}`
}

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

// ── 隐患点树点击 ──
async function onHpNodeClick(data: any) {
  if (data.isGroup) return  // 忽略分组节点
  const hpId = Number(data.id)
  if (selectedHazardPointId.value === hpId) return
  selectedHazardPointId.value = hpId
  selectedDeviceId.value = null
  currentSensorMeta.value = null
  hpDevicesLoading.value = true
  hpDevices.value = []
  try {
    const res: any = await getBoundDevices(String(hpId))
    const rawDevices = res?.data || res?.rows || res || []
    // 为每个设备加载传感器
    hpDevices.value = await Promise.all(rawDevices.map(async (d: any) => {
      let sensors: any[] = []
      try {
        const sRes: any = await getDeviceSensors(d.deviceId)
        sensors = sRes?.data || sRes?.rows || sRes || []
      } catch {}
      return {
        ...d,
        sensors,
        onlineStatus: d.deviceStatus === 0 ? 'online' : 'offline',
      }
    }))
  } catch {
    hpDevices.value = []
  } finally {
    hpDevicesLoading.value = false
  }
}

// ── 设备卡片点击 ──
async function onDeviceCardClick(device: any) {
  selectedDeviceId.value = device.deviceId
  if (device.sensors && device.sensors.length > 0) {
    const s = device.sensors[0]
    currentSensorMeta.value = {
      monitorContentId: s.monitorContentId || s.id,
      monitorTypeId: s.monitorTypeId,
    }
    // 用第一个传感器的监测类型加载 monitorContents
    if (s.monitorTypeId) await loadMonitorContents(s.monitorTypeId)
    // 构建含传感器层级的指标树
    await buildFromSensors(
      device.sensors.map((s: any) => ({
        sensorId: s.id || s.sensorId,
        sensorName: s.monitorTypeName || s.name || s.sensorName || '',
        monitorTypeId: s.monitorTypeId,
      }))
    )
  } else {
    currentSensorMeta.value = null
    clearIndicatorTree()
  }
  await loadAllCriteria()
  const list = currentHpCriteria.value
  selectedHpCriteriaId.value = list.length > 0 ? list[0].id : null
  if (list.length > 0 && list[0]) initLevelForm(list[0])
  else resetLevelForm()
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

// ── 监测类型选择 ──
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
  selectedHazardPointId.value = null; selectedDeviceId.value = null; currentSensorMeta.value = null
  selectedTypeCriteriaId.value = null; selectedHpCriteriaId.value = null
  hpDevices.value = []
  clearIndicatorTree()
  resetLevelForm()
  await loadAllCriteria()
}

// ── 保存逻辑 ──

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
  return selectedDeviceName.value
}

function getActiveCriteria(context: 'monitor' | 'hazard'): AlarmCriteriaItem | undefined {
  const list = context === 'monitor' ? currentTypeCriteria.value : currentHpCriteria.value
  const selectedId = context === 'monitor' ? selectedTypeCriteriaId.value : selectedHpCriteriaId.value
  return list.find(c => c.id === selectedId) || list[0]
}

async function handleSaveForm(context: 'monitor' | 'hazard') {
  const existing = getActiveCriteria(context)
  const formEmpty = isFormEmpty()

  if (formEmpty && !existing) {
    ElMessage.info('请先添加条件')
    return
  }

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
        payload.hazardPointId = selectedHazardPointId.value!
        payload.monitorContentId = currentSensorMeta.value?.monitorContentId
        payload.monitorTypeId = currentSensorMeta.value?.monitorTypeId
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

/* ===== 隐患点三栏布局 ===== */

.hazard-three-col {
  display: flex;
  gap: 16px;
  height: 100%;
}

.col-hp-tree {
  width: 220px;
  background: #fff;
  border: 1px solid #e5e6eb;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  overflow: hidden;
}

.col-device-list {
  width: 280px;
  background: #fff;
  border: 1px solid #e5e6eb;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  overflow: hidden;
}

.col-criteria {
  flex: 1;
  background: #fff;
  border: 1px solid #e5e6eb;
  border-radius: 8px;
  padding: 16px;
  min-width: 0;
  overflow: hidden;
}

.col-header {
  padding: 12px 14px;
  font-size: 14px;
  font-weight: 600;
  color: #1d2129;
  border-bottom: 1px solid #e5e6eb;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.col-header-count {
  font-size: 12px;
  color: #86909c;
  background: #f0f1f3;
  border-radius: 10px;
  padding: 0 8px;
  line-height: 20px;
}

/* 隐患点树 */
.col-hp-tree :deep(.el-tree) {
  background: transparent;
  padding: 4px;
}

.col-hp-tree :deep(.el-tree-node__content) {
  border-radius: 4px;
  height: 32px;
}

/* 设备卡片列表 */
.device-scroll {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.device-scroll::-webkit-scrollbar {
  width: 4px;
}

.device-scroll::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.12);
  border-radius: 2px;
}

.device-capsule {
  padding: 10px 12px;
  background: #f7f8fa;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid #e5e6eb;
}

.device-capsule:hover {
  background: #e8f4ff;
  border-color: #91caff;
}

.device-capsule.selected {
  background: #e8f4ff;
  border-color: #1677ff;
  box-shadow: 0 2px 8px rgba(22, 119, 255, 0.15);
}

.capsule-name-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.capsule-name {
  font-size: 13px;
  font-weight: 600;
  color: #1d2129;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
  min-width: 0;
}

.capsule-status {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 4px;
  flex-shrink: 0;
  font-weight: 500;
}

.capsule-status-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
}

.capsule-status.online {
  background: #f0ffe6;
  color: #237804;
}

.capsule-status.online .capsule-status-dot {
  background: #52c41a;
}

.capsule-status.offline {
  background: #f5f5f5;
  color: #86909c;
}

.capsule-status.offline .capsule-status-dot {
  background: #c9cdd4;
}

.capsule-meta {
  margin-top: 4px;
}

.capsule-code {
  font-size: 12px;
  color: #6b7785;
  font-family: 'SFMono-Regular', Consolas, monospace;
}

.capsule-sensors {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: 6px;
  padding-top: 6px;
  border-top: 1px solid #e5e6eb;
}

.sensor-chip {
  padding: 2px 8px;
  background: #ffffff;
  border: 1px solid #e5e6eb;
  border-radius: 4px;
  font-size: 11px;
  color: #4e5969;
}

.sensor-chip-name {
  white-space: nowrap;
}

/* ===== 通用 ===== */

.empty-hint {
  text-align: center;
  color: #999;
  padding: 40px 12px;
  font-size: 14px;
}
</style>
