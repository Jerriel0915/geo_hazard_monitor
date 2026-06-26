<template>
  <el-dialog
    v-model="dialogVisible"
    :title="dialogTitle"
    width="900px"
    :close-on-click-modal="false"
    destroy-on-close
  >
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="编号" prop="code">
            <el-input
              v-model="formData.code"
              placeholder="请输入监测类型编号"
              :disabled="isView || isEdit"
              maxlength="100"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="名称" prop="name">
            <el-input
              v-model="formData.name"
              placeholder="请输入监测类型名称"
              :disabled="isView"
              maxlength="200"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="图标">
            <div class="type-icon-selector" :class="{ disabled: isView }" @click="!isView && handleSelectTypeIcon()">
              <img v-if="formData.icon" :src="formData.icon" class="type-icon-img" alt="icon" />
              <span v-else class="type-icon-placeholder">点击选择图标</span>
            </div>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="排序号" prop="sortOrder">
            <el-input-number
              v-model="formData.sortOrder"
              :min="0"
              :max="2147483647"
              :disabled="isView"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="描述" prop="description">
        <el-input
          v-model="formData.description"
          type="textarea"
          :rows="3"
          maxlength="500"
          show-word-limit
          placeholder="请输入描述"
          :disabled="isView"
          style="width: 100%"
        />
      </el-form-item>

      <el-divider content-position="left">
        <span class="divider-title">监测模型</span>
      </el-divider>

      <div>
        <div class="param-toolbar" v-if="!isView">
          <el-button type="primary" size="small" @click="handleAddModelAttr">
            <span class="btn-icon">+</span> 添加监测内容
          </el-button>
          <el-button type="primary" size="small" @click="handleAddModelAttr" class="param-toolbar__right-btn">
            <span class="btn-icon">+</span> 添加字段
          </el-button>
        </div>
        <el-table
          :data="formData.modelAttrs"
          border
          size="small"
          empty-text="暂无监测内容，可按需添加"
        >
          <el-table-column label="内容编码" min-width="120" align="center">
            <template #default="{ row }">
              <template v-if="isView">{{ row.code || '-' }}</template>
              <el-input
                v-else
                v-model="row.code"
                placeholder="如 rainfall_hour"
                :disabled="Boolean(row.id)"
                maxlength="100"
              />
            </template>
          </el-table-column>
          <el-table-column label="内容名称" min-width="130" align="center">
            <template #default="{ row }">
              <template v-if="isView">{{ row.name || '-' }}</template>
              <el-input
                v-else
                v-model="row.name"
                placeholder="如 小时雨量"
                maxlength="200"
              />
            </template>
          </el-table-column>
          <el-table-column label="指标类型" min-width="100" align="center">
            <template #default="{ row }">
              <template v-if="isView">{{ indicatorTypeLabel(row.indicatorType) || '-' }}</template>
              <el-select
                v-else
                v-model="row.indicatorType"
                placeholder="请选择"
                :disabled="Boolean(row.id)"
                @change="handleIndicatorTypeChange(row)"
              >
                <el-option
                  v-for="item in indicatorTypeOptions"
                  :key="item.code"
                  :label="item.name"
                  :value="item.code"
                />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="字段类型" width="110" align="center">
            <template #default="{ row }">
              <template v-if="isView">
                <el-tag v-if="row.fieldType === 'computed'" type="warning" size="small">计算属性</el-tag>
                <el-tag v-else type="info" size="small">固有属性</el-tag>
              </template>
              <el-select
                v-else
                v-model="row.fieldType"
                :disabled="Boolean(row.id)"
                placeholder="请选择"
                @change="handleFieldTypeChange(row)"
              >
                <el-option label="固有属性" value="inherent" />
                <el-option label="计算属性" value="computed" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="单位" min-width="64" align="center">
            <template #default="{ row }">
              <template v-if="isView">{{ row.unit || '-' }}</template>
              <el-input v-else v-model="row.unit" placeholder="自动带出" :disabled="true" />
            </template>
          </el-table-column>
          <el-table-column label="最小值" min-width="100" align="center">
            <template #default="{ row }">
              <template v-if="isView">{{ row.rangeMin !== null && row.rangeMin !== undefined ? row.rangeMin : '-' }}</template>
              <el-input-number
                v-else
                v-model="row.rangeMin"
                :controls="false"
                placeholder="最小值"
                style="width: 100%"
              />
            </template>
          </el-table-column>
          <el-table-column label="最大值" min-width="100" align="center">
            <template #default="{ row }">
              <template v-if="isView">{{ row.rangeMax !== null && row.rangeMax !== undefined ? row.rangeMax : '-' }}</template>
              <el-input-number
                v-else
                v-model="row.rangeMax"
                :controls="false"
                placeholder="最大值"
                style="width: 100%"
              />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="140" fixed="right" align="center" v-if="!isView">
            <template #default="{ row, $index }">
              <div class="op-cell">
                <el-button
                  v-if="row.fieldType === 'computed'"
                  type="primary"
                  text
                  size="small"
                  @click="handleEditScript(row, $index)"
                >脚本</el-button>
                <el-button type="text" size="small" class="danger-text" @click="handleRemoveModelAttr($index)">
                  删除
                </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-form>

    <template #footer v-if="!isView">
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
    </template>
    <template #footer v-else>
      <el-button @click="dialogVisible = false">关闭</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="typeIconDialogVisible" title="选择监测类型图标" width="800px">
    <div class="icon-grid">
      <div
        v-for="item in typeIconList"
        :key="item.code"
        class="icon-item"
        @click="handleTypeIconSelect(item)"
      >
        <img :src="item.path" class="icon-select-img" :alt="item.name" />
        <span class="icon-name">{{ item.name }}</span>
      </div>
    </div>
  </el-dialog>

  <CalcScriptEditor
    v-if="editingScriptRow"
    v-model="calcScriptDialogVisible"
    :attr-code="editingScriptRow.code"
    :attr-name="editingScriptRow.name || editingScriptRow.code"
    :unit="editingScriptRow.unit"
    :script="editingScriptRow.calcScript || ''"
    :monitor-type-id="formData.id || 0"
    @save="handleScriptSaved"
  />
</template>

<script setup lang="ts">
import { computed, nextTick, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  createMonitorContent,
  createMonitorType,
  getMonitorTypeDetail,
  type MonitorContentItem,
  type MonitorTypeItem,
  removeMonitorContent,
  updateMonitorContent,
  updateMonitorType
} from '@/api/monitorType'
import { getIconList, type IconItem } from '@/constants/monitorIcons'
import { showRequestErrorMessage } from '@/utils/errorHandler'
import { INDICATOR_TYPE_OPTIONS as indicatorTypeOptions, indicatorTypeLabel } from '@/utils/indicatorType'
import CalcScriptEditor from './CalcScriptEditor.vue'

// ==================== Props & Emits ====================

interface Props {
  visible: boolean
  mode: 'view' | 'edit' | 'create'
  monitorTypeId?: number
}

interface Emits {
  (e: 'update:visible', value: boolean): void
  (e: 'saved'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

// ==================== 派生状态 ====================

const isView = computed(() => props.mode === 'view')
const isEdit = computed(() => props.mode === 'edit')

const dialogTitle = computed(() => {
  switch (props.mode) {
    case 'view': return '查看监测类型'
    case 'edit': return '编辑监测类型'
    case 'create': return '新增监测类型'
  }
})

// ==================== 弹窗可见性双向同步 ====================

const dialogVisible = ref(false)

watch(() => props.visible, (val) => {
  dialogVisible.value = val
  if (val) {
    if (props.mode === 'create') {
      resetFormData()
    } else if (props.monitorTypeId) {
      resetFormData()
      fetchDetail(props.monitorTypeId)
    }
  }
})

watch(() => dialogVisible.value, (val) => {
  if (!val) {
    resetFormData()
  }
  emit('update:visible', val)
})

// ==================== 表单 ====================

const formRef = ref<FormInstance>()
const loading = ref(false)
const submitLoading = ref(false)
const originalContents = ref<MonitorContentItem[]>([])

const formData = reactive<{
  id?: number
  code: string
  name: string
  icon: string
  description: string
  sortOrder: number
  status: number
  modelAttrs: MonitorContentItem[]
}>({
  code: '',
  name: '',
  icon: '',
  description: '',
  sortOrder: 0,
  status: 1,
  modelAttrs: []
})

const formRules: FormRules = {
  code: [
    { required: true, message: '请输入监测类型编号', trigger: 'blur' },
    { max: 100, message: '监测类型编号长度不能超过100个字符', trigger: 'blur' }
  ],
  name: [
    { required: true, message: '请输入监测类型名称', trigger: 'blur' },
    { max: 200, message: '监测类型名称长度不能超过200个字符', trigger: 'blur' }
  ],
  description: [{ max: 500, message: '描述长度不能超过500个字符', trigger: 'blur' }]
}

const resetFormData = () => {
  Object.assign(formData, {
    id: undefined,
    code: '',
    name: '',
    icon: '',
    description: '',
    sortOrder: 0,
    status: 1,
    modelAttrs: []
  })
  originalContents.value = []
}

// ==================== 详情加载 ====================

const normalizeMonitorContent = (item: any): MonitorContentItem => ({
  id: item?.id ? Number(item.id) : undefined,
  code: String(item?.code || '').trim(),
  name: String(item?.name || '').trim(),
  indicatorType: String(item?.indicatorType || '').trim(),
  unit: String(item?.unit || '').trim(),
  icon: item?.icon || '',
  rangeMin: item?.rangeMin === null || item?.rangeMin === undefined ? null : Number(item.rangeMin),
  rangeMax: item?.rangeMax === null || item?.rangeMax === undefined ? null : Number(item.rangeMax),
  fieldType: item?.fieldType === 'computed' ? 'computed' : 'inherent',
  calcScript: item?.calcScript || ''
})

const normalizeMonitorType = (item: any): MonitorTypeItem => ({
  id: Number(item?.id),
  code: String(item?.code || ''),
  name: String(item?.name || ''),
  icon: String(item?.icon || ''),
  description: String(item?.description || ''),
  sortOrder: Number(item?.sortOrder ?? 0),
  status: Number(item?.status ?? 1),
  createTime: String(item?.createTime || ''),
  updateTime: String(item?.updateTime || ''),
  createBy: String(item?.createBy || ''),
  updateBy: String(item?.updateBy || ''),
  contents: Array.isArray(item?.contents) ? item.contents.map(normalizeMonitorContent) : undefined
})

const fillFormFromDetail = (detail: MonitorTypeItem) => {
  Object.assign(formData, {
    id: detail.id,
    code: detail.code,
    name: detail.name,
    icon: detail.icon || '',
    description: detail.description || '',
    sortOrder: detail.sortOrder ?? 0,
    status: detail.status ?? 1,
    modelAttrs: (detail.contents || []).map((item) => ({ ...item }))
  })
  originalContents.value = (detail.contents || []).map((item) => ({ ...item }))
}

const fetchDetail = async (id: number) => {
  loading.value = true
  try {
    const detail = await getMonitorTypeDetail(id)
    const normalized = normalizeMonitorType(detail)
    fillFormFromDetail(normalized)
    await nextTick()
    formRef.value?.clearValidate()
  } catch (error) {
    console.error('获取监测类型详情失败:', error)
    showRequestErrorMessage(error, '获取监测类型详情失败')
  } finally {
    loading.value = false
  }
}

// ==================== 图标选择 ====================

const typeIconDialogVisible = ref(false)
const typeIconList: IconItem[] = getIconList()

const handleSelectTypeIcon = () => {
  typeIconDialogVisible.value = true
}

const handleTypeIconSelect = (item: IconItem) => {
  formData.icon = item.path
  formData.modelAttrs = formData.modelAttrs.map((row) => ({
    ...row,
    icon: row.icon || item.path
  }))
  typeIconDialogVisible.value = false
}

// ==================== 监测内容管理 ====================

const handleAddModelAttr = () => {
  formData.modelAttrs.push({
    code: '',
    name: '',
    indicatorType: '',
    unit: '',
    icon: formData.icon || '',
    rangeMin: null,
    rangeMax: null,
    fieldType: 'inherent',
    calcScript: ''
  })
}

const handleRemoveModelAttr = (index: number) => {
  const current = formData.modelAttrs[index]
  const displayName = current?.name || current?.code || `第 ${index + 1} 条监测内容`
  const hasId = Boolean(current?.id)

  const warningText = hasId
    ? `删除监测内容「${displayName}」后，历史数据保留但不可查询，新数据也不再识别和解析该字段。确定删除？`
    : `确定要删除监测内容「${displayName}」吗？`

  ElMessageBox.confirm(warningText, '删除确认', {
    confirmButtonText: '确定删除',
    cancelButtonText: '取消',
    type: 'error',
    confirmButtonClass: 'el-button--danger'
  }).then(() => {
    formData.modelAttrs.splice(index, 1)
  }).catch(() => {})
}

const handleIndicatorTypeChange = (row: MonitorContentItem) => {
  const type = indicatorTypeOptions.find((item) => item.code === row.indicatorType)
  row.unit = type?.unit || ''
}

// ==================== 计算脚本 ====================

const calcScriptDialogVisible = ref(false)
const editingScriptIndex = ref<number>(-1)
const editingScriptRow = ref<MonitorContentItem | null>(null)

const handleFieldTypeChange = (row: MonitorContentItem) => {
  if (row.fieldType === 'computed' && !row.calcScript) {
    row.calcScript = `// 计算属性: ${row.code || '属性编码'}\n`
      + '// 可用变量 (Groovy 5 语法, 必须用 .get() 访问):\n'
      + `//   curData.get('properties').get('${row.code || 'attrCode'}')  当前值\n`
      + `//   prevData?.get('properties')?.get('${row.code || 'attrCode'}')  上一条值\n`
      + '// 返回: 计算结果 (数值类型)\n\n'
      + `return curData.get('properties').get('${row.code || 'attrCode'}')\n`
  }
}

const handleEditScript = (row: MonitorContentItem, index: number) => {
  editingScriptIndex.value = index
  editingScriptRow.value = row
  calcScriptDialogVisible.value = true
}

const handleScriptSaved = (script: string) => {
  if (editingScriptRow.value) {
    editingScriptRow.value.calcScript = script
  }
  calcScriptDialogVisible.value = false
}

// ==================== 校验 ====================

const validateModelAttrs = () => {
  const codeSet = new Set<string>()

  for (let index = 0; index < formData.modelAttrs.length; index += 1) {
    const row = formData.modelAttrs[index]
    row.code = row.code.trim()
    row.name = row.name.trim()

    if (!row.code) {
      ElMessage.warning(`第 ${index + 1} 行监测内容编码不能为空`)
      return false
    }
    if (!row.name) {
      ElMessage.warning(`第 ${index + 1} 行监测内容名称不能为空`)
      return false
    }
    if (!row.indicatorType) {
      ElMessage.warning(`第 ${index + 1} 行指标类型不能为空`)
      return false
    }
    if (!row.fieldType) {
      ElMessage.warning(`第 ${index + 1} 行字段类型不能为空`)
      return false
    }
    if (row.fieldType === 'computed' && !row.calcScript?.trim()) {
      ElMessage.warning(`第 ${index + 1} 行(${row.name || row.code})为计算属性, 必须设置计算脚本`)
      return false
    }
    if (row.code.length > 100) {
      ElMessage.warning(`第 ${index + 1} 行监测内容编码长度不能超过100个字符`)
      return false
    }
    if (row.name.length > 200) {
      ElMessage.warning(`第 ${index + 1} 行监测内容名称长度不能超过200个字符`)
      return false
    }
    if (codeSet.has(row.code)) {
      ElMessage.warning(`监测内容编码 "${row.code}" 重复，请调整后重试`)
      return false
    }
    if (row.rangeMin !== null && row.rangeMin !== undefined && Number.isNaN(Number(row.rangeMin))) {
      ElMessage.warning(`第 ${index + 1} 行最小值范围不合法`)
      return false
    }
    if (row.rangeMax !== null && row.rangeMax !== undefined && Number.isNaN(Number(row.rangeMax))) {
      ElMessage.warning(`第 ${index + 1} 行最大值范围不合法`)
      return false
    }
    if (
      row.rangeMin !== null &&
      row.rangeMin !== undefined &&
      row.rangeMax !== null &&
      row.rangeMax !== undefined &&
      Number(row.rangeMax) < Number(row.rangeMin)
    ) {
      ElMessage.warning(`第 ${index + 1} 行量程范围不合法，最大值不能小于最小值`)
      return false
    }
    codeSet.add(row.code)
  }

  return true
}

// ==================== 提交 ====================

const buildMonitorTypeCreatePayload = () => ({
  code: formData.code.trim(),
  name: formData.name.trim(),
  icon: formData.icon || '',
  description: formData.description.trim(),
  sortOrder: formData.sortOrder ?? 0,
  status: formData.status
})

const buildMonitorTypeUpdatePayload = () => ({
  name: formData.name.trim(),
  icon: formData.icon || '',
  description: formData.description.trim(),
  sortOrder: formData.sortOrder ?? 0
})

const syncMonitorContents = async (monitorTypeId: number) => {
  const currentRows = formData.modelAttrs.map((item) => ({
    id: item.id,
    code: item.code.trim(),
    name: item.name.trim(),
    indicatorType: item.indicatorType,
    unit: item.unit.trim(),
    icon: item.icon || '',
    rangeMin: item.rangeMin ?? null,
    rangeMax: item.rangeMax ?? null,
    fieldType: item.fieldType || 'inherent',
    calcScript: item.calcScript ?? ''
  }))

  const existingMap = new Map(originalContents.value.map((item) => [item.id, item]))
  const currentIds = new Set(currentRows.filter((item) => item.id).map((item) => item.id as number))

  for (const oldItem of originalContents.value) {
    if (oldItem.id && !currentIds.has(oldItem.id)) {
      await removeMonitorContent(oldItem.id)
    }
  }

  for (const item of currentRows) {
    if (item.id) {
      const oldItem = existingMap.get(item.id)
      if (!oldItem) {
        continue
      }

      if (oldItem.code !== item.code || oldItem.indicatorType !== item.indicatorType) {
        await removeMonitorContent(item.id)
        await createMonitorContent({
          monitorTypeId,
          code: item.code,
          name: item.name,
          unit: item.unit,
          indicatorType: item.indicatorType,
          icon: item.icon,
          rangeMin: item.rangeMin,
          rangeMax: item.rangeMax,
          fieldType: item.fieldType,
          calcScript: item.calcScript
        })
        continue
      }

      if (
        oldItem.name !== item.name ||
        oldItem.unit !== item.unit ||
        (oldItem.icon || '') !== (item.icon || '') ||
        (oldItem.rangeMin ?? null) !== (item.rangeMin ?? null) ||
        (oldItem.rangeMax ?? null) !== (item.rangeMax ?? null) ||
        (oldItem.calcScript ?? '') !== (item.calcScript ?? '')
      ) {
        await updateMonitorContent(item.id, {
          name: item.name,
          unit: item.unit,
          icon: item.icon,
          rangeMin: item.rangeMin,
          rangeMax: item.rangeMax,
          calcScript: item.calcScript
        })
      }
      continue
    }

    await createMonitorContent({
      monitorTypeId,
      code: item.code,
      name: item.name,
      unit: item.unit,
      indicatorType: item.indicatorType,
      icon: item.icon,
      rangeMin: item.rangeMin,
      rangeMax: item.rangeMax,
      fieldType: item.fieldType,
      calcScript: item.calcScript
    })
  }
}

const handleSubmit = async () => {
  if (submitLoading.value) {
    return
  }

  try {
    const valid = await formRef.value?.validate()
    if (!valid) {
      return
    }
  } catch {
    return
  }

  if (!validateModelAttrs()) {
    return
  }

  submitLoading.value = true
  try {
    let monitorTypeId = formData.id

    if (isEdit.value && monitorTypeId) {
      await updateMonitorType(monitorTypeId, buildMonitorTypeUpdatePayload())
    } else {
      const createResult = await createMonitorType(buildMonitorTypeCreatePayload())
      monitorTypeId = Number(createResult?.id)
    }

    if (!monitorTypeId) {
      throw new Error('保存监测类型失败')
    }

    await syncMonitorContents(monitorTypeId)

    ElMessage.success(isEdit.value ? '修改成功' : '新增成功')
    dialogVisible.value = false
    emit('saved')
  } catch (error) {
    console.error('保存监测类型失败:', error)
    showRequestErrorMessage(error, '保存监测类型失败')
  } finally {
    submitLoading.value = false
  }
}
</script>

<style scoped>
.table-icon {
  width: 28px;
  height: 28px;
  object-fit: contain;
}

.divider-title {
  font-size: 14px;
  font-weight: bold;
  color: #303133;
}

.param-toolbar {
  margin-bottom: 10px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.param-toolbar__right-btn {
  margin-left: auto;
}

.type-icon-selector {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 80px;
  height: 42px;
  border: 1px dashed #dcdfe6;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
}

.type-icon-selector:hover {
  border-color: #1890ff;
  background: #e6f7ff;
}

.type-icon-selector.disabled {
  cursor: not-allowed;
}

.type-icon-selector.disabled:hover {
  border-color: #dcdfe6;
  background: transparent;
}

.type-icon-img {
  width: 28px;
  height: 28px;
  object-fit: contain;
}

.type-icon-placeholder {
  color: #909399;
  font-size: 12px;
}

.icon-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  padding: 10px 5px;
  max-height: 380px;
  overflow-y: auto;
  justify-content: flex-start;
}

.icon-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 85px;
  padding: 8px 2px;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
}

.icon-item:hover {
  border-color: #1890ff;
  background: #e6f7ff;
}

.icon-select-img {
  width: 32px;
  height: 32px;
  object-fit: contain;
}

.icon-name {
  font-size: 12px;
  color: #606266;
  margin-top: 6px;
  text-align: center;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  width: 100%;
}

:deep(.el-form-item) {
  margin-bottom: 18px;
}

:deep(.el-divider) {
  margin: 20px 0 15px;
}

.op-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

.danger-text {
  color: #f56c6c !important;
}

.danger-text:hover {
  color: #f78989 !important;
}

/* ================== 最终极简版修复方案 ================== */

/* 1. 只保证容器宽度 100%，让它左侧和右侧严格对齐上方的标准 el-input */
:deep(.el-input-number) {
  width: 100% !important;
}

:deep(.el-input-number > .el-input) {
  width: 100% !important;
}

/* 2. 最内层的 wrapper 用 max-width 控制，防止破坏 box-sizing 导致挤出外部边界 */
:deep(.el-input-number > .el-input > .el-input__wrapper) {
  max-width: 100% !important;
  box-sizing: border-box !important;
  width: 100%; /* 填补按钮中间的空白区域 */
}
</style>