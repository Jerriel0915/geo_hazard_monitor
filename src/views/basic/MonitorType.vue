<template>
  <div class="monitor-type-page">
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">监测类型管理</h2>
      </div>
      <div class="header-right">
        <el-button type="primary" @click="handleAdd">
          <span class="btn-icon">+</span> 新增
        </el-button>
        <el-button @click="handleExport">
          <span class="btn-icon">↓</span> 导出
        </el-button>
      </div>
    </div>

    <div class="search-bar">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索编号或名称"
        class="search-input"
        clearable
        @clear="handleSearch"
        @keyup.enter="handleSearch"
      >
        <template #prefix>
          <span class="search-icon">🔍</span>
        </template>
      </el-input>
      <el-button type="primary" @click="handleSearch">搜索</el-button>
    </div>

    <div class="table-container">
      <el-table
        :data="tableData"
        border
        stripe
        v-loading="loading"
        :header-cell-style="{ background: '#f5f7fa', color: '#303133', fontWeight: 'bold' }"
      >
        <el-table-column prop="code" label="编号" width="150" align="center" />
        <el-table-column prop="name" label="名称" min-width="180" align="center" />
        <el-table-column prop="paramModel" label="参数模型概要" min-width="200" align="center">
          <template #default="{ row }">
            <span class="param-summary">{{ row.paramModel || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="sensorCount" label="传感器数量" width="120" align="center">
          <template #default="{ row }">
            <el-tag type="info" effect="plain">{{ row.sensorCount || 0 }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createDept" label="创建部门" width="150" align="center" />
        <el-table-column prop="createTime" label="创建时间" width="180" align="center" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.isDeleted ? 'danger' : 'success'" effect="plain">
              {{ row.isDeleted ? '已作废' : '正常' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="text" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="text" size="small" @click="handleView(row)">查看</el-button>
            <el-button type="text" size="small" class="danger-text" @click="handleDelete(row)">
              {{ row.isDeleted ? '启用' : '作废' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
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
              <el-input v-model="formData.code" placeholder="请输入编号" :disabled="isView" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="名称" prop="name">
              <el-input v-model="formData.name" placeholder="请输入名称" :disabled="isView" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="创建部门" prop="createDept">
              <el-input v-model="formData.createDept" placeholder="请输入创建部门" :disabled="isView" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="创建人员" prop="createUser">
              <el-input v-model="formData.createUser" placeholder="请输入创建人员" :disabled="isView" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">
          <span class="divider-title">参数模型</span>
        </el-divider>

        <div class="param-table-container">
          <div class="param-toolbar" v-if="!isView">
            <el-button type="primary" size="small" @click="handleAddParam">
              <span class="btn-icon">+</span> 添加参数
            </el-button>
          </div>
          <el-table
            :data="formData.paramList"
            border
            size="small"
            :header-cell-style="{ background: '#f5f7fa', color: '#303133' }"
          >
            <el-table-column label="监测内容" width="150" align="center">
              <template #default="{ row }">
                <el-select
                  v-model="row.monitorContent"
                  placeholder="请选择"
                  :disabled="isView"
                  @change="handleContentChange(row)"
                >
                  <el-option
                    v-for="item in monitorContentOptions"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="图标" width="100" align="center">
              <template #default="{ row }">
                <div class="icon-cell" @click="!isView && handleSelectIcon(row)">
                  <span v-if="row.icon" class="icon-display" v-html="row.icon"></span>
                  <span v-else class="icon-placeholder">选择</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="维度" width="120" align="center">
              <template #default="{ row }">
                <el-select v-model="row.dimension" placeholder="请选择" :disabled="isView" allow-create filterable>
                  <el-option label="无" value="" />
                  <el-option label="X" value="x" />
                  <el-option label="Y" value="y" />
                  <el-option label="Z" value="z" />
                  <el-option label="X,Y" value="x,y" />
                  <el-option label="X,Y,Z" value="x,y,z" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="量程范围" min-width="200" align="center">
              <template #default="{ row }">
                <div class="range-inputs" v-if="!isView">
                  <el-input-number v-model="row.rangeMin" :min="-999999" :max="row.rangeMax" controls-position="right" placeholder="最小值" />
                  <span class="range-separator">~</span>
                  <el-input-number v-model="row.rangeMax" :min="row.rangeMin" :max="999999" controls-position="right" placeholder="最大值" />
                </div>
                <span v-else>{{ row.rangeMin }} ~ {{ row.rangeMax }}</span>
              </template>
            </el-table-column>
            <el-table-column label="单位" width="100" align="center">
              <template #default="{ row }">
                <el-input v-model="row.unit" placeholder="单位" :disabled="isView" />
              </template>
            </el-table-column>
            <el-table-column label="突变值" width="120" align="center">
              <template #default="{ row }">
                <el-input-number v-model="row.mutationValue" :min="0" controls-position="right" placeholder="突变值" :disabled="isView" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="80" align="center" v-if="!isView">
              <template #default="{ $index }">
                <el-button type="text" size="small" class="danger-text" @click="handleRemoveParam($index)">
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-form>

      <template #footer v-if="!isView">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="iconDialogVisible" title="选择图标" width="500px">
      <div class="icon-grid">
        <div
          v-for="icon in iconList"
          :key="icon"
          class="icon-item"
          @click="handleIconSelect(icon)"
          v-html="icon"
        ></div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const MonitorContentEnum = {
  TEMPERATURE: { value: 'temperature', label: '温度', unit: '℃' },
  HUMIDITY: { value: 'humidity', label: '湿度', unit: '%' },
  RAINFALL: { value: 'rainfall', label: '雨量', unit: 'mm' },
  DISPLACEMENT: { value: 'displacement', label: '位移', unit: 'mm' },
  VELOCITY: { value: 'velocity', label: '流速', unit: 'm/s' },
  WATER_LEVEL: { value: 'water_level', label: '水位', unit: 'm' },
  PRESSURE: { value: 'pressure', label: '压力', unit: 'kPa' },
  INCLINATION: { value: 'inclination', label: '倾角', unit: '°' },
  VIBRATION: { value: 'vibration', label: '振动', unit: 'mm/s' },
  NOISE: { value: 'noise', label: '噪音', unit: 'dB' },
  POWER: { value: 'power', label: '电量', unit: 'V' },
  CURRENT: { value: 'current', label: '电流', unit: 'A' },
  SOIL_MOISTURE: { value: 'soil_moisture', label: '土壤含水率', unit: '%' },
  GAP: { value: 'gap', label: '裂缝', unit: 'mm' },
  SUBSIDENCE: { value: 'subsidence', label: '沉降', unit: 'mm' }
}

interface ParamItem {
  monitorContent: string
  icon: string
  dimension: string
  rangeMin: number
  rangeMax: number
  unit: string
  mutationValue: number
}

interface MonitorTypeItem {
  id: string
  code: string
  name: string
  createDept: string
  createUser: string
  createTime: string
  updateDept: string
  updateUser: string
  updateTime: string
  isDeleted: boolean
  deleteTime: string
  paramList: ParamItem[]
  paramModel: string
  sensorCount: number
}

const monitorContentOptions = Object.values(MonitorContentEnum).map(item => ({
  value: item.value,
  label: item.label,
  unit: item.unit
}))

const iconList = [
  '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 14.76V3.5a2.5 2.5 0 0 0-5 0v11.26a4.5 4.5 0 1 0 5 0z"/></svg>',
  '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2v20M2 12h20"/></svg>',
  '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/></svg>',
  '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/></svg>',
  '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 20V10M12 20V4M6 20v-6"/></svg>',
  '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 12h-4l-3 9L9 3l-3 9H2"/></svg>',
  '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="18" height="18" rx="2"/><path d="M3 9h18M9 21V9"/></svg>',
  '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>'
]

const loading = ref(false)
const tableData = ref<MonitorTypeItem[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const searchKeyword = ref('')
const dialogVisible = ref(false)
const dialogTitle = ref('')
const isView = ref(false)
const formRef = ref()
const iconDialogVisible = ref(false)
const currentEditRow = ref<ParamItem | null>(null)

const formData = reactive<{
  id?: string
  code: string
  name: string
  createDept: string
  createUser: string
  paramList: ParamItem[]
}>({
  code: '',
  name: '',
  createDept: '',
  createUser: '',
  paramList: []
})

const formRules = {
  code: [{ required: true, message: '请输入编号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }]
}

const initTableData = () => {
  loading.value = true
  setTimeout(() => {
    tableData.value = [
      {
        id: '1',
        code: 'JCLX001',
        name: '雨量监测',
        createDept: '技术部',
        createUser: '张三',
        createTime: '2024-01-15 10:30:00',
        updateDept: '技术部',
        updateUser: '张三',
        updateTime: '2024-01-15 10:30:00',
        isDeleted: false,
        deleteTime: '',
        paramList: [
          { monitorContent: 'rainfall', icon: '', dimension: '', rangeMin: 0, rangeMax: 500, unit: 'mm', mutationValue: 50 }
        ],
        paramModel: '雨量: 0~500mm',
        sensorCount: 128
      },
      {
        id: '2',
        code: 'JCLX002',
        name: '位移监测',
        createDept: '技术部',
        createUser: '李四',
        createTime: '2024-01-16 14:20:00',
        updateDept: '技术部',
        updateUser: '李四',
        updateTime: '2024-01-16 14:20:00',
        isDeleted: false,
        deleteTime: '',
        paramList: [
          { monitorContent: 'displacement', icon: '', dimension: 'x,y', rangeMin: -100, rangeMax: 100, unit: 'mm', mutationValue: 10 }
        ],
        paramModel: '位移(X,Y): -100~100mm',
        sensorCount: 64
      },
      {
        id: '3',
        code: 'JCLX003',
        name: '温湿度监测',
        createDept: '运维部',
        createUser: '王五',
        createTime: '2024-01-17 09:15:00',
        updateDept: '运维部',
        updateUser: '王五',
        updateTime: '2024-01-17 09:15:00',
        isDeleted: false,
        deleteTime: '',
        paramList: [
          { monitorContent: 'temperature', icon: '', dimension: '', rangeMin: -40, rangeMax: 80, unit: '℃', mutationValue: 5 },
          { monitorContent: 'humidity', icon: '', dimension: '', rangeMin: 0, rangeMax: 100, unit: '%', mutationValue: 10 }
        ],
        paramModel: '温度: -40~80℃; 湿度: 0~100%',
        sensorCount: 256
      },
      {
        id: '4',
        code: 'JCLX004',
        name: '地表位移监测',
        createDept: '技术部',
        createUser: '赵六',
        createTime: '2024-01-18 11:00:00',
        updateDept: '技术部',
        updateUser: '赵六',
        updateTime: '2024-01-18 11:00:00',
        isDeleted: true,
        deleteTime: '2024-02-01 16:30:00',
        paramList: [
          { monitorContent: 'displacement', icon: '', dimension: 'x,y,z', rangeMin: -500, rangeMax: 500, unit: 'mm', mutationValue: 20 }
        ],
        paramModel: '位移(X,Y,Z): -500~500mm',
        sensorCount: 0
      }
    ]
    total.value = tableData.value.length
    loading.value = false
  }, 500)
}

const handleSearch = () => {
  currentPage.value = 1
  initTableData()
}

const handleSizeChange = () => {
  initTableData()
}

const handlePageChange = () => {
  initTableData()
}

const handleAdd = () => {
  dialogTitle.value = '新增监测类型'
  isView.value = false
  Object.assign(formData, {
    id: undefined,
    code: '',
    name: '',
    createDept: '',
    createUser: '',
    paramList: []
  })
  dialogVisible.value = true
}

const handleEdit = (row: MonitorTypeItem) => {
  dialogTitle.value = '编辑监测类型'
  isView.value = false
  Object.assign(formData, JSON.parse(JSON.stringify(row)))
  dialogVisible.value = true
}

const handleView = (row: MonitorTypeItem) => {
  dialogTitle.value = '查看监测类型'
  isView.value = true
  Object.assign(formData, JSON.parse(JSON.stringify(row)))
  dialogVisible.value = true
}

const handleDelete = (row: MonitorTypeItem) => {
  const action = row.isDeleted ? '启用' : '作废'
  const confirmText = row.isDeleted
    ? `确定要启用监测类型"${row.name}"吗?`
    : `确定要作废监测类型"${row.name}"吗?`
  ElMessageBox.confirm(confirmText, `${action}确认`, {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    row.isDeleted = !row.isDeleted
    row.deleteTime = row.isDeleted ? new Date().toLocaleString() : ''
    ElMessage.success(`${action}成功`)
  }).catch(() => {})
}

const handleExport = () => {
  ElMessage.info('正在导出...')
  setTimeout(() => {
    ElMessage.success('导出成功')
  }, 1000)
}

const handleSubmit = () => {
  formRef.value.validate((valid: boolean) => {
    if (valid) {
      if (formData.paramList.length === 0) {
        ElMessage.warning('请至少添加一个参数')
        return
      }
      ElMessage.success('保存成功')
      dialogVisible.value = false
      initTableData()
    }
  })
}

const handleAddParam = () => {
  formData.paramList.push({
    monitorContent: '',
    icon: '',
    dimension: '',
    rangeMin: 0,
    rangeMax: 100,
    unit: '',
    mutationValue: 0
  })
}

const handleRemoveParam = (index: number) => {
  formData.paramList.splice(index, 1)
}

const handleContentChange = (row: ParamItem) => {
  const content = monitorContentOptions.find(opt => opt.value === row.monitorContent)
  if (content) {
    row.unit = content.unit
  }
}

const handleSelectIcon = (row: ParamItem) => {
  currentEditRow.value = row
  iconDialogVisible.value = true
}

const handleIconSelect = (icon: string) => {
  if (currentEditRow.value) {
    currentEditRow.value.icon = icon
  }
  iconDialogVisible.value = false
}

onMounted(() => {
  initTableData()
})
</script>

<style scoped>
.monitor-type-page {
  padding: 20px;
  background: #fff;
  border-radius: 8px;
  min-height: calc(100% - 40px);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-title {
  font-size: 18px;
  font-weight: bold;
  color: #303133;
  margin: 0;
}

.header-right {
  display: flex;
  gap: 10px;
}

.btn-icon {
  margin-right: 4px;
}

.search-bar {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
}

.search-input {
  width: 300px;
}

.search-icon {
  font-size: 14px;
}

.table-container {
  background: #fff;
}

.param-summary {
  color: #606266;
  font-size: 13px;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

.danger-text {
  color: #f56c6c !important;
}

.danger-text:hover {
  color: #f56c6c !important;
}

.divider-title {
  font-size: 14px;
  font-weight: bold;
  color: #303133;
}

.param-table-container {
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 15px;
  background: #fafafa;
}

.param-toolbar {
  margin-bottom: 10px;
}

.icon-cell {
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 32px;
}

.icon-display {
  display: flex;
  align-items: center;
}

.icon-display :deep(svg) {
  width: 20px;
  height: 20px;
}

.icon-placeholder {
  color: #909399;
  font-size: 12px;
}

.range-inputs {
  display: flex;
  align-items: center;
  gap: 8px;
}

.range-separator {
  color: #606266;
}

.icon-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 15px;
  padding: 10px;
}

.icon-item {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 60px;
  height: 60px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
}

.icon-item:hover {
  border-color: #409eff;
  background: #f0f7ff;
}

.icon-item :deep(svg) {
  width: 30px;
  height: 30px;
  color: #409eff;
}

:deep(.el-form-item) {
  margin-bottom: 18px;
}

:deep(.el-divider) {
  margin: 20px 0 15px;
}
</style>