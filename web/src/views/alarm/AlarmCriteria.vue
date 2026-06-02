<template>
  <div class="page-content">
    <div class="page-title">告警判据</div>
    <div class="page-body">
      <el-tabs v-model="activeTab" class="criteria-tabs">
        <el-tab-pane label="监测类型" name="monitorType">
          <div class="criteria-container">
            <div class="left-panel">
              <el-select
                v-model="selectedMonitorType"
                placeholder="请选择监测类型"
                style="width: 100%"
                @change="handleMonitorTypeChange"
              >
                <el-option
                  v-for="item in monitorTypes"
                  :key="item.id"
                  :label="item.name"
                  :value="item.id"
                />
              </el-select>
              <div class="type-list">
                <div
                  v-for="item in monitorTypes"
                  :key="item.id"
                  class="type-item"
                  :class="{ active: selectedMonitorType === item.id }"
                  @click="selectedMonitorType = item.id"
                >
                  {{ item.name }}
                </div>
              </div>
            </div>
            <div class="right-panel">
              <div class="criteria-form">
                <div
                  v-for="(level, index) in [1, 2, 3, 4]"
                  :key="level"
                  class="level-section"
                >
                  <div class="level-header" :class="`level-${level}`">
                    <span class="level-number">{{ index + 1 }}、</span>
                    <span class="level-name">{{ getLevelName(level) }}</span>
                  </div>
                  <div class="criteria-items">
                    <div class="criteria-item">
                      <span class="criteria-label">多指标判据：</span>
                      <div class="criteria-display">
                        <div class="display-text">
                          {{ getMonitorCriteriaDisplay(selectedMonitorType, level) }}
                        </div>
                        <el-button type="primary" size="small" @click="openMonitorCriteriaEdit(level)">
                          修改
                        </el-button>
                      </div>
                    </div>
                    <div class="criteria-item">
                      <span class="criteria-label">告警持续时长：</span>
                      <el-input-number
                        v-model="getMonitorCriteriaData(selectedMonitorType, level).duration"
                        :min="1"
                        size="small"
                      />
                      <span class="unit">次</span>
                    </div>
                    <div class="criteria-item">
                      <span class="criteria-label">静默数据周期：</span>
                      <el-input-number
                        v-model="getMonitorCriteriaData(selectedMonitorType, level).silentPeriod"
                        :min="1"
                        size="small"
                      />
                      <span class="unit">次</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>
        <el-tab-pane label="隐患点" name="hazardPoint">
          <div class="criteria-container">
            <div class="left-panel">
              <el-tree
                :data="hazardPointTreeData"
                :props="treeProps"
                node-key="id"
                v-model="selectedHazardPointId"
                @node-click="handleHazardPointClick"
                default-expand-all
                highlight-current
              />
            </div>
            <div class="right-panel">
              <div class="criteria-form">
                <div
                  v-for="(level, index) in [1, 2, 3, 4]"
                  :key="level"
                  class="level-section"
                >
                  <div class="level-header" :class="`level-${level}`">
                    <span class="level-number">{{ index + 1 }}、</span>
                    <span class="level-name">{{ getLevelName(level) }}</span>
                  </div>
                  <div class="criteria-items">
                    <div class="criteria-item">
                      <span class="criteria-label">多指标判据：</span>
                      <div class="criteria-display">
                        <div class="display-text">
                          {{ getHazardCriteriaDisplay(selectedHazardPointId, level) }}
                        </div>
                        <el-button type="primary" size="small" @click="openHazardCriteriaEdit(level)">
                          修改
                        </el-button>
                      </div>
                    </div>
                    <div class="criteria-item">
                      <span class="criteria-label">告警持续时长：</span>
                      <el-input-number
                        v-model="getHazardCriteriaData(selectedHazardPointId, level).duration"
                        :min="1"
                        size="small"
                      />
                      <span class="unit">次</span>
                    </div>
                    <div class="criteria-item">
                      <span class="criteria-label">静默数据周期：</span>
                      <el-input-number
                        v-model="getHazardCriteriaData(selectedHazardPointId, level).silentPeriod"
                        :min="1"
                        size="small"
                      />
                      <span class="unit">次</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <el-dialog
      v-model="criteriaEditVisible"
      title="指标判据编辑"
      width="800px"
    >
      <div class="criteria-edit-container">
        <div class="criteria-display-area">
          <el-input
            v-model="editingCriteriaText"
            type="textarea"
            :rows="4"
            placeholder="判据表达式"
            readonly
          />
        </div>
        <div class="elements-area">
          <div class="operators-section">
            <div class="section-title">逻辑运算符</div>
            <div class="element-buttons">
              <el-button v-for="op in logicalOperators" :key="op" size="small" @click="insertElement(op)">
                {{ op }}
              </el-button>
            </div>
          </div>
          <div class="indicators-section">
            <div class="section-title">指标</div>
            <div class="element-buttons">
              <el-button
                v-for="indicator in availableIndicators"
                :key="indicator"
                size="small"
                @click="insertElement(indicator)"
              >
                {{ indicator }}
              </el-button>
            </div>
          </div>
        </div>
        <div class="description-area">
          <div class="section-title">说明</div>
          <el-input
            v-model="editingDescription"
            type="textarea"
            :rows="3"
            placeholder="说明"
          />
        </div>
      </div>
      <template #footer>
        <el-button @click="criteriaEditVisible = false">取消</el-button>
        <el-button type="primary" @click="saveCriteriaEdit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'

const activeTab = ref('monitorType')
const selectedMonitorType = ref('')
const selectedHazardPointId = ref('')

const criteriaEditVisible = ref(false)
const editingLevel = ref(0)
const editingContext = ref<'monitor' | 'hazard'>('monitor')
const editingCriteriaText = ref('')
const editingDescription = ref('')

const logicalOperators = ['或', '且']
const availableIndicators = [
  '水位(m)',
  '小时变化',
  '12小时变',
  '24小时变化(m)',
  '水位增幅(cm)',
  '状态',
  '流量(m³/s)',
  '雨量'
]

interface MonitorType {
  id: string
  name: string
}

interface CriteriaData {
  criteriaText: string
  description: string
  duration: number
  silentPeriod: number
}

const monitorTypes: MonitorType[] = [
  { id: '1', name: '表面位移' },
  { id: '2', name: '深部位移' },
  { id: '3', name: '表面沉降' },
  { id: '4', name: '深部沉降' },
  { id: '5', name: '温湿度' },
  { id: '6', name: '加速度' },
  { id: '7', name: '土体含水率' }
]

const treeProps = {
  children: 'children',
  label: 'name'
}

const hazardPointTreeData = [
  {
    id: 'g1',
    name: '长江流域-重庆段',
    children: [
      { id: 'h1', name: '边坡1' },
      { id: 'h2', name: '边坡2' },
      { id: 'h3', name: '边坡3' }
    ]
  },
  {
    id: 'g2',
    name: '长江中游段',
    children: [
      { id: 'h4', name: '危库1' },
      { id: 'h5', name: '危库2' }
    ]
  },
  {
    id: 'g3',
    name: '三峡库区',
    children: [
      { id: 'h6', name: '滑坡点A' },
      { id: 'h7', name: '滑坡点B' }
    ]
  }
]

const monitorCriteriaData = reactive<Record<string, Record<number, CriteriaData>>>({})
const hazardCriteriaData = reactive<Record<string, Record<number, CriteriaData>>>({})

const initMonitorCriteria = () => {
  monitorTypes.forEach(type => {
    monitorCriteriaData[type.id] = {
      1: { criteriaText: '水位(m) > 10 且 小时变化 > 5', description: '水位超过10m且小时变化大于5', duration: 2, silentPeriod: 3 },
      2: { criteriaText: '水位(m) > 8', description: '水位超过8m', duration: 3, silentPeriod: 4 },
      3: { criteriaText: '小时变化 > 3', description: '小时变化大于3', duration: 4, silentPeriod: 5 },
      4: { criteriaText: '雨量 > 50', description: '雨量超过50', duration: 5, silentPeriod: 6 }
    }
  })
}

const initHazardCriteria = () => {
  const hazardIds = ['h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'h7']
  hazardIds.forEach(id => {
    hazardCriteriaData[id] = {
      1: { criteriaText: '位移 > 50mm', description: '位移超过50mm', duration: 2, silentPeriod: 2 },
      2: { criteriaText: '位移 > 30mm', description: '位移超过30mm', duration: 3, silentPeriod: 3 },
      3: { criteriaText: '速率 > 10mm/h', description: '速率超过10mm/h', duration: 4, silentPeriod: 4 },
      4: { criteriaText: '速率 > 5mm/h', description: '速率超过5mm/h', duration: 5, silentPeriod: 5 }
    }
  })
}

const getLevelName = (level: number) => {
  const names: Record<number, string> = {
    1: '蓝色告警（注意级）',
    2: '黄色告警（警示级）',
    3: '橙色告警（警戒级）',
    4: '红色告警（警报级）'
  }
  return names[level] || ''
}

const getMonitorCriteriaDisplay = (typeId: string, level: number) => {
  if (!typeId || !monitorCriteriaData[typeId]) return '请选择监测类型'
  return monitorCriteriaData[typeId][level].criteriaText || '未设置'
}

const getHazardCriteriaDisplay = (hazardId: string, level: number) => {
  if (!hazardId || !hazardCriteriaData[hazardId]) return '请选择隐患点'
  return hazardCriteriaData[hazardId][level].criteriaText || '未设置'
}

const getMonitorCriteriaData = (typeId: string, level: number) => {
  if (!typeId || !monitorCriteriaData[typeId]) {
    return { criteriaText: '', description: '', duration: 1, silentPeriod: 1 }
  }
  return monitorCriteriaData[typeId][level]
}

const getHazardCriteriaData = (hazardId: string, level: number) => {
  if (!hazardId || !hazardCriteriaData[hazardId]) {
    return { criteriaText: '', description: '', duration: 1, silentPeriod: 1 }
  }
  return hazardCriteriaData[hazardId][level]
}

const handleMonitorTypeChange = () => {
  ElMessage.info(`已切换到${monitorTypes.find(t => t.id === selectedMonitorType.value)?.name}`)
}

const handleHazardPointClick = (data: any) => {
  if (data.children && data.children.length > 0) return
  selectedHazardPointId.value = data.id
  ElMessage.info(`已选择${data.name}`)
}

const openMonitorCriteriaEdit = (level: number) => {
  editingLevel.value = level
  editingContext.value = 'monitor'
  const data = getMonitorCriteriaData(selectedMonitorType.value, level)
  editingCriteriaText.value = data.criteriaText
  editingDescription.value = data.description
  criteriaEditVisible.value = true
}

const openHazardCriteriaEdit = (level: number) => {
  editingLevel.value = level
  editingContext.value = 'hazard'
  const data = getHazardCriteriaData(selectedHazardPointId.value, level)
  editingCriteriaText.value = data.criteriaText
  editingDescription.value = data.description
  criteriaEditVisible.value = true
}

const insertElement = (element: string) => {
  editingCriteriaText.value += element
}

const saveCriteriaEdit = () => {
  if (editingContext.value === 'monitor' && selectedMonitorType.value) {
    monitorCriteriaData[selectedMonitorType.value][editingLevel.value].criteriaText = editingCriteriaText.value
    monitorCriteriaData[selectedMonitorType.value][editingLevel.value].description = editingDescription.value
  } else if (editingContext.value === 'hazard' && selectedHazardPointId.value) {
    hazardCriteriaData[selectedHazardPointId.value][editingLevel.value].criteriaText = editingCriteriaText.value
    hazardCriteriaData[selectedHazardPointId.value][editingLevel.value].description = editingDescription.value
  }
  ElMessage.success('判据保存成功')
  criteriaEditVisible.value = false
}

onMounted(() => {
  initMonitorCriteria()
  initHazardCriteria()
  selectedMonitorType.value = monitorTypes[0].id
  selectedHazardPointId.value = 'h1'
})
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
  border-bottom: 1px solid #e8e8e8;
}

.page-body {
  padding: 0;
}

.criteria-tabs :deep(.el-tabs__header) {
  margin: 0 0 20px 0;
}

.criteria-container {
  display: flex;
  gap: 20px;
  min-height: 600px;
}

.left-panel {
  width: 280px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 10px;
}

.left-panel .el-select {
  margin-bottom: 10px;
}

.type-list {
  max-height: 550px;
  overflow-y: auto;
}

.type-item {
  padding: 10px 12px;
  cursor: pointer;
  border-radius: 4px;
  margin-bottom: 4px;
  transition: all 0.3s;
}

.type-item:hover {
  background: #f5f7fa;
}

.type-item.active {
  background: #409eff;
  color: #fff;
}

.right-panel {
  flex: 1;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 20px;
  overflow-y: auto;
  max-height: 600px;
}

.criteria-form {
  display: flex;
  flex-direction: column;
  gap: 30px;
}

.level-section {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.level-header {
  display: flex;
  align-items: center;
  font-size: 16px;
  font-weight: 500;
  padding: 10px 0;
  border-bottom: 2px solid #e8e8e8;
}

.level-header.level-1 {
  color: #409eff;
  border-bottom-color: #409eff;
}

.level-header.level-2 {
  color: #e6a23c;
  border-bottom-color: #e6a23c;
}

.level-header.level-3 {
  color: #f56c6c;
  border-bottom-color: #f56c6c;
}

.level-header.level-4 {
  color: #f00;
  border-bottom-color: #f00;
}

.level-number {
  margin-right: 5px;
}

.criteria-items {
  display: flex;
  flex-direction: column;
  gap: 15px;
  padding-left: 20px;
}

.criteria-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.criteria-label {
  min-width: 120px;
  font-size: 14px;
  color: #606266;
}

.criteria-display {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
}

.display-text {
  flex: 1;
  padding: 8px 12px;
  background: #f5f7fa;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-size: 14px;
  color: #303133;
  min-height: 36px;
  line-height: 20px;
}

.unit {
  font-size: 14px;
  color: #909399;
  margin-left: 5px;
}

.criteria-edit-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.criteria-display-area {
  width: 100%;
}

.elements-area {
  display: flex;
  gap: 20px;
}

.operators-section,
.indicators-section {
  flex: 1;
}

.section-title {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 10px;
}

.element-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.description-area {
  width: 100%;
}
</style>