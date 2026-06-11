<template>
  <div class="page">
    <div class="header">
      <div class="header__left">
        <h2 class="header__title">监测类型管理</h2>
        <span class="header__subtitle">监测大类、类型与监测内容字典维护</span>
      </div>
      <div class="header__right">
        <el-button type="primary" @click="handleAdd">+ 新增</el-button>
        <el-button @click="handleExport" :disabled="tableData.length === 0">导出当前页</el-button>
      </div>
    </div>

    <div class="search">
      <el-select v-model="searchType">
        <el-option label="按编号" value="code" />
        <el-option label="按名称" value="name" />
      </el-select>
      <el-input
        v-model="searchKeyword"
        :placeholder="searchType === 'code' ? '搜索监测类型编号' : '搜索监测类型名称'"
        clearable
        @clear="handleSearch"
        @keyup.enter="handleSearch"
      />
      <el-select v-model="searchStatus" clearable placeholder="状态">
        <el-option label="启用" :value="1" />
        <el-option label="禁用" :value="0" />
      </el-select>
      <el-button type="primary" @click="handleSearch">搜索</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>

    <div class="table-wrap">
      <div class="table-wrap__scroll">
        <el-table
          :data="tableData"
          border
          stripe
          v-loading="loading"
        >
          <el-table-column prop="code" label="编号" width="120" align="center" />
          <el-table-column label="图标" width="80" align="center">
            <template #default="{ row }">
              <img v-if="row.icon" :src="row.icon" class="table-icon" alt="icon" />
              <span v-else class="empty-text">-</span>
            </template>
          </el-table-column>
          <el-table-column prop="name" label="名称" min-width="160" align="center" />
          <el-table-column prop="categoryName" label="监测大类" width="120" align="center">
            <template #default="{ row }">
              <el-tag type="info" effect="plain">{{ row.categoryName || '-' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="description" label="描述" min-width="200" align="center">
            <template #default="{ row }">
              <span>{{ row.description || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="sortOrder" label="排序号" width="90" align="center" />
          <el-table-column prop="status" label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="plain">
                {{ row.status === 1 ? '启用' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" min-width="170" align="center" />
          <el-table-column label="操作" width="200" fixed="right" align="center">
            <template #default="{ row }">
              <div class="op-cell">
                <el-button type="primary" text size="small" @click="handleView(row)">查看</el-button>
                <el-button type="primary" text size="small" @click="handleEdit(row)">编辑</el-button>
                <el-dropdown trigger="hover" @command="(cmd: string) => handleMoreCommand(cmd, row)">
                  <el-button type="primary" text size="small">更多</el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="delete">
                        <span style="color: #f56c6c">删除</span>
                      </el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="table-wrap__pagination">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          prev-text="上一页"
          next-text="下一页"
          :disabled="total === 0"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </div>

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
            <el-form-item label="监测大类" prop="categoryId">
              <el-select
                v-model="formData.categoryId"
                placeholder="请选择监测大类"
                :disabled="isView"
                style="width: 100%"
              >
                <el-option
                  v-for="item in categoryOptions"
                  :key="item.id"
                  :label="item.name"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
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
          <el-col :span="12">
            <el-form-item label="状态">
              <el-tag :type="formData.status === 1 ? 'success' : 'info'" effect="plain">
                {{ formData.status === 1 ? '启用' : '禁用' }}
              </el-tag>
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
          />
        </el-form-item>

        <el-divider content-position="left">
          <span class="divider-title">监测模型</span>
        </el-divider>

        <el-alert
          v-if="!isView && isEdit"
          class="form-alert"
          type="info"
          :closable="false"
          title="已保存的监测内容支持修改名称、量程和单位；如需变更编码或指标类型，请删除后重新新增。"
        />

        <div class="param-table-container">
          <div class="param-toolbar" v-if="!isView">
            <el-button type="primary" size="small" @click="handleAddModelAttr">
              <span class="btn-icon">+</span> 添加监测内容
            </el-button>
          </div>
          <el-table
            :data="formData.modelAttrs"
            border
            size="small"
            empty-text="暂无监测内容，可按需添加"
            :header-cell-style="{ background: '#f5f7fa', color: '#303133' }"
          >
            <el-table-column label="内容编码" min-width="180" align="center">
              <template #default="{ row }">
                <el-input
                  v-model="row.code"
                  placeholder="如 rainfall_hour"
                  :disabled="isView || Boolean(row.id)"
                  maxlength="100"
                />
              </template>
            </el-table-column>
            <el-table-column label="内容名称" min-width="180" align="center">
              <template #default="{ row }">
                <el-input
                  v-model="row.name"
                  placeholder="如 小时雨量"
                  :disabled="isView"
                  maxlength="200"
                />
              </template>
            </el-table-column>
            <el-table-column label="指标类型" width="160" align="center">
              <template #default="{ row }">
                <el-select
                  v-model="row.indicatorType"
                  placeholder="请选择"
                  :disabled="isView || Boolean(row.id)"
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
            <el-table-column label="单位" width="120" align="center">
              <template #default="{ row }">
                <el-input v-model="row.unit" placeholder="自动带出" :disabled="true" />
              </template>
            </el-table-column>
            <el-table-column label="最小值" width="150" align="center">
              <template #default="{ row }">
                <el-input-number
                  v-model="row.rangeMin"
                  :disabled="isView"
                  :controls="false"
                  placeholder="最小值"
                  style="width: 100%"
                />
              </template>
            </el-table-column>
            <el-table-column label="最大值" width="150" align="center">
              <template #default="{ row }">
                <el-input-number
                  v-model="row.rangeMax"
                  :disabled="isView"
                  :controls="false"
                  placeholder="最大值"
                  style="width: 100%"
                />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="90" align="center" v-if="!isView">
              <template #default="{ $index }">
                <el-button type="text" size="small" class="danger-text" @click="handleRemoveModelAttr($index)">
                  删除
                </el-button>
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

    <el-dialog v-model="typeIconDialogVisible" title="选择监测类型图标" width="600px">
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
  </div>
</template>

<script setup lang="ts">
import {nextTick, onMounted, reactive, ref} from 'vue'
import {ElMessage, ElMessageBox, type FormInstance, type FormRules} from 'element-plus'
import {Search} from '@element-plus/icons-vue'
import request from '@/utils/request'
import {
  createMonitorContent,
  createMonitorType,
  getMonitorTypeDetail,
  getMonitorTypePage,
  type MonitorContentItem,
  type MonitorTypeItem,
  removeMonitorContent,
  removeMonitorType,
  updateMonitorContent,
  updateMonitorType
} from '@/api/monitorType'
import {getIconList, type IconItem} from '@/constants/monitorIcons'

type SearchType = 'code' | 'name'

const IndicatorTypeEnum = {
  WY: { code: 'wy', name: '位移', unit: 'mm' },
  WD: { code: 'wd', name: '温度', unit: '℃' },
  JD: { code: 'jd', name: '角度', unit: '°' },
  YL: { code: 'yl', name: '压力', unit: 'MPa' },
  SW: { code: 'sw', name: '水位', unit: 'm' },
  JSD: { code: 'jsd', name: '加速度', unit: 'm/s²' },
  HSL: { code: 'hsl', name: '含水率', unit: '%' },
  LJN: { code: 'ljn', name: '力矩', unit: 'N/m²' },
  ZDL: { code: 'zdl', name: '震动频率', unit: 'Hz' },
  DL: { code: 'dl', name: '电量', unit: 'V' },
  DX: { code: 'dx', name: '断线', unit: '1' },
  SG: { code: 'sg', name: '声光', unit: '1' },
  SP: { code: 'sp', name: '视频', unit: '1' }
} as const

const indicatorTypeOptions = Object.values(IndicatorTypeEnum)
const categoryOptions = ref<{ id: number; name: string }[]>([])
const typeIconList: IconItem[] = getIconList()

const loading = ref(false)
const submitLoading = ref(false)
const tableData = ref<MonitorTypeItem[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const searchType = ref<SearchType>('name')
const searchKeyword = ref('')
const searchStatus = ref<number | ''>('')
const dialogVisible = ref(false)
const dialogTitle = ref('')
const isView = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()
const typeIconDialogVisible = ref(false)
const originalContents = ref<MonitorContentItem[]>([])

const formData = reactive<{
  id?: number
  code: string
  name: string
  icon: string
  categoryId: number | null
  description: string
  sortOrder: number
  status: number
  modelAttrs: MonitorContentItem[]
}>({
  code: '',
  name: '',
  icon: '',
  categoryId: null,
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
  categoryId: [{ required: true, message: '请选择监测大类', trigger: 'change' }],
  description: [{ max: 500, message: '描述长度不能超过500个字符', trigger: 'blur' }]
}

const normalizeMonitorContent = (item: any): MonitorContentItem => ({
  id: item?.id ? Number(item.id) : undefined,
  code: String(item?.code || '').trim(),
  name: String(item?.name || '').trim(),
  indicatorType: String(item?.indicatorType || '').trim(),
  unit: String(item?.unit || '').trim(),
  icon: item?.icon || '',
  rangeMin: item?.rangeMin === null || item?.rangeMin === undefined ? null : Number(item.rangeMin),
  rangeMax: item?.rangeMax === null || item?.rangeMax === undefined ? null : Number(item.rangeMax)
})

const normalizeMonitorType = (item: any): MonitorTypeItem => ({
  id: Number(item?.id),
  code: String(item?.code || ''),
  categoryId: item?.categoryId ?? undefined,
  categoryName: item?.categoryName || '',
  name: String(item?.name || ''),
  icon: String(item?.icon || ''),
  description: String(item?.description || ''),
  sortOrder: Number(item?.sortOrder ?? 0),
  status: Number(item?.status ?? 1),
  createTime: String(item?.createTime || ''),
  contents: Array.isArray(item?.contents) ? item.contents.map(normalizeMonitorContent) : undefined
})

const getRequestErrorInfo = (error: any, fallbackMessage = '网络请求失败') => {
  const status = error?.response?.status
  const backendMessage = error?.response?.data?.msg
  const message = backendMessage || error?.message || fallbackMessage
  return { status, message }
}

const showRequestErrorMessage = (error: any, fallbackMessage = '网络请求失败') => {
  const { status, message } = getRequestErrorInfo(error, fallbackMessage)
  if (status === 400) {
    ElMessage.warning(message)
    return
  }
  ElMessage.error(message)
}

const resetFormData = () => {
  Object.assign(formData, {
    id: undefined,
    code: '',
    name: '',
    icon: '',
    categoryId: null,
    description: '',
    sortOrder: 0,
    status: 1,
    modelAttrs: []
  })
  originalContents.value = []
}

const openDialog = async () => {
  dialogVisible.value = true
  await nextTick()
  formRef.value?.clearValidate()
}

const buildQueryParams = () => {
  const params: Record<string, any> = {
    pageNum: currentPage.value,
    pageSize: pageSize.value
  }

  const keyword = searchKeyword.value.trim()
  if (keyword) {
    params[searchType.value] = keyword
  }
  if (searchStatus.value !== '') {
    params.status = searchStatus.value
  }
  return params
}

const loadTableData = async () => {
  loading.value = true
  try {
    const data = await getMonitorTypePage(buildQueryParams())
    tableData.value = (data.rows || []).map(normalizeMonitorType)
    total.value = Number(data.total || 0)
  } catch (error) {
    console.error('获取监测类型列表失败:', error)
    showRequestErrorMessage(error, '获取监测类型列表失败')
  } finally {
    loading.value = false
  }
}

const fillFormFromDetail = (detail: MonitorTypeItem) => {
  Object.assign(formData, {
    id: detail.id,
    code: detail.code,
    name: detail.name,
    icon: detail.icon || '',
    categoryId: detail.categoryId ?? null,
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
    fillFormFromDetail(normalizeMonitorType(detail))
    return true
  } catch (error) {
    console.error('获取监测类型详情失败:', error)
    showRequestErrorMessage(error, '获取监测类型详情失败')
    return false
  } finally {
    loading.value = false
  }
}

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

const buildMonitorTypeCreatePayload = () => ({
  code: formData.code.trim(),
  name: formData.name.trim(),
  categoryId: formData.categoryId as number,
  icon: formData.icon || '',
  description: formData.description.trim(),
  sortOrder: formData.sortOrder ?? 0,
  status: formData.status
})

const buildMonitorTypeUpdatePayload = () => ({
  name: formData.name.trim(),
  categoryId: formData.categoryId ?? undefined,
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
    rangeMax: item.rangeMax ?? null
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
          rangeMax: item.rangeMax
        })
        continue
      }

      if (
        oldItem.name !== item.name ||
        oldItem.unit !== item.unit ||
        (oldItem.icon || '') !== (item.icon || '') ||
        (oldItem.rangeMin ?? null) !== (item.rangeMin ?? null) ||
        (oldItem.rangeMax ?? null) !== (item.rangeMax ?? null)
      ) {
        await updateMonitorContent(item.id, {
          name: item.name,
          unit: item.unit,
          icon: item.icon,
          rangeMin: item.rangeMin,
          rangeMax: item.rangeMax
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
      rangeMax: item.rangeMax
    })
  }
}

const downloadCsv = (fileName: string, rows: Array<Array<string | number>>) => {
  const formatCsvCell = (value: string | number) => `"${String(value ?? '').replace(/"/g, '""')}"`
  const csv = `\uFEFF${rows.map((row) => row.map(formatCsvCell).join(',')).join('\n')}`
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

const handleSearch = () => {
  currentPage.value = 1
  loadTableData()
}

const handleReset = () => {
  searchType.value = 'name'
  searchKeyword.value = ''
  searchStatus.value = ''
  currentPage.value = 1
  loadTableData()
}

const handleSizeChange = () => {
  currentPage.value = 1
  loadTableData()
}

const handlePageChange = () => {
  loadTableData()
}

const handleAdd = async () => {
  dialogTitle.value = '新增监测类型'
  isView.value = false
  isEdit.value = false
  resetFormData()
  await openDialog()
}

const handleEdit = async (row: MonitorTypeItem) => {
  dialogTitle.value = '编辑监测类型'
  isView.value = false
  isEdit.value = true
  resetFormData()
  if (await fetchDetail(row.id)) {
    await openDialog()
  }
}

const handleView = async (row: MonitorTypeItem) => {
  dialogTitle.value = '查看监测类型'
  isView.value = true
  isEdit.value = false
  resetFormData()
  if (await fetchDetail(row.id)) {
    await openDialog()
  }
}

const handleDelete = (row: MonitorTypeItem) => {
  ElMessageBox.confirm(`确定要删除监测类型"${row.name}"吗？`, '删除确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    loading.value = true
    try {
      await removeMonitorType(row.id)
      ElMessage.success('删除成功')
      loadTableData()
    } catch (error) {
      console.error('删除监测类型失败:', error)
      showRequestErrorMessage(error, '删除监测类型失败')
    } finally {
      loading.value = false
    }
  }).catch(() => {})
}

const handleMoreCommand = (command: string, row: MonitorTypeItem) => {
  if (command === 'delete') {
    handleDelete(row)
  }
}

const handleExport = () => {
  const rows = [
    ['编号', '名称', '设备类型', '描述', '排序号', '状态', '创建时间'],
    ...tableData.value.map((item) => [
      item.code,
      item.name,
      item.categoryName || '',
      item.description || '',
      item.sortOrder,
      item.status === 1 ? '启用' : '禁用',
      item.createTime || ''
    ])
  ]
  downloadCsv(`monitor-types-${Date.now()}.csv`, rows)
  ElMessage.success('导出成功')
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
    loadTableData()
  } catch (error) {
    console.error('保存监测类型失败:', error)
    showRequestErrorMessage(error, '保存监测类型失败')
  } finally {
    submitLoading.value = false
  }
}

const handleAddModelAttr = () => {
  formData.modelAttrs.push({
    code: '',
    name: '',
    indicatorType: '',
    unit: '',
    icon: formData.icon || '',
    rangeMin: null,
    rangeMax: null
  })
}

const handleRemoveModelAttr = (index: number) => {
  const current = formData.modelAttrs[index]
  const displayName = current?.name || current?.code || `第 ${index + 1} 条监测内容`

  ElMessageBox.confirm(`确定要删除监测内容“${displayName}”吗？`, '删除确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    formData.modelAttrs.splice(index, 1)
  }).catch(() => {})
}

const handleIndicatorTypeChange = (row: MonitorContentItem) => {
  const type = indicatorTypeOptions.find((item) => item.code === row.indicatorType)
  row.unit = type?.unit || ''
}

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

const loadCategoryOptions = async () => {
  try {
    const res = await request.get('/monitor-categories')
    categoryOptions.value = (res.data || [])
  } catch { /* ignore */ }
}

onMounted(() => {
  loadTableData()
  loadCategoryOptions()
})
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

.form-alert {
  margin-bottom: 16px;
}

.param-table-container {
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  padding: 15px;
  background: #fafafa;
}

.param-toolbar {
  margin-bottom: 10px;
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
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 15px;
  padding: 10px;
}

.icon-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 10px;
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

</style>
