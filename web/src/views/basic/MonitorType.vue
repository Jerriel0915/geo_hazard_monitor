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
        <el-table-column label="图标" width="80" align="center">
          <template #default="{ row }">
            <img v-if="row.icon" :src="row.icon" class="table-icon" alt="icon" />
            <span v-else class="empty-text">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="名称" min-width="180" align="center" />
        <el-table-column prop="deviceTypeName" label="设备类型" width="120" align="center">
          <template #default="{ row }">
            <el-tag type="info" effect="plain">{{ row.deviceTypeName || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="modelSummary" label="监测模型概要" min-width="250" align="center">
          <template #default="{ row }">
            <span class="param-summary">{{ row.modelSummary || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="sensorCount" label="传感器数量" width="120" align="center">
          <template #default="{ row }">
            <el-tag type="info" effect="plain">{{ row.sensorCount || 0 }}</el-tag>
          </template>
        </el-table-column>
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
            <el-form-item label="图标" prop="icon">
              <div class="type-icon-selector" @click="!isView && handleSelectTypeIcon()">
                <img v-if="formData.icon" :src="formData.icon" class="type-icon-img" alt="icon" />
                <span v-else class="type-icon-placeholder">点击选择图标</span>
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="设备类型" prop="deviceType">
              <el-select v-model="formData.deviceType" placeholder="请选择设备类型" :disabled="isView" style="width: 100%">
                <el-option label="直连设备" value="DIRECT" />
                <el-option label="传感器" value="SENSOR" />
                <el-option label="RTU" value="RTU" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="创建人员" prop="createUser">
              <el-input v-model="formData.createUser" placeholder="请输入创建人员" :disabled="isView" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">
          <span class="divider-title">监测模型</span>
        </el-divider>

        <div class="param-table-container">
          <div class="param-toolbar" v-if="!isView">
            <el-button type="primary" size="small" @click="handleAddModelAttr">
              <span class="btn-icon">+</span> 添加属性
            </el-button>
          </div>
          <el-table
            :data="formData.modelAttrs"
            border
            size="small"
            :header-cell-style="{ background: '#f5f7fa', color: '#303133' }"
          >
            <el-table-column label="属性标识" width="150" align="center">
              <template #default="{ row }">
                <el-input v-model="row.attrCode" placeholder="如: displacement_x" :disabled="isView" />
              </template>
            </el-table-column>
            <el-table-column label="属性名称" width="150" align="center">
              <template #default="{ row }">
                <el-input v-model="row.attrName" placeholder="如: X轴位移" :disabled="isView" />
              </template>
            </el-table-column>
            <el-table-column label="指标类型" width="150" align="center">
              <template #default="{ row }">
                <el-select
                  v-model="row.indicatorType"
                  placeholder="请选择"
                  :disabled="isView"
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
                <el-input v-model="row.unit" placeholder="单位" :disabled="true" />
              </template>
            </el-table-column>

            <el-table-column label="操作" width="80" align="center" v-if="!isView">
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
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 监测类型图标选择弹窗 -->
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'


// 获取token
const getToken = () => localStorage.getItem('token')

// 指标类型规范
const IndicatorTypeEnum = {
  WY: { code: 'wy', name: '位移', unit: 'mm', unitName: '毫米' },
  WD: { code: 'wd', name: '温度', unit: '℃', unitName: '摄氏度' },
  JD: { code: 'jd', name: '角度', unit: '°', unitName: '度' },
  YL: { code: 'yl', name: '压力', unit: 'MPa', unitName: '兆帕斯' },
  SW: { code: 'sw', name: '水位', unit: 'm', unitName: '米' },
  JSD: { code: 'jsd', name: '加速度', unit: 'm/s²', unitName: '米/秒²' },
  HSL: { code: 'hsl', name: '含水率', unit: '%', unitName: '百分比' },
  LJN: { code: 'ljn', name: '力矩', unit: 'n/m²', unitName: '牛顿/米²' },
  ZDL: { code: 'zdl', name: '震动频率', unit: 'Hz', unitName: '赫兹' },
  DL: { code: 'dl', name: '电量', unit: 'V', unitName: '伏特' },
  DX: { code: 'dx', name: '断线', unit: '1', unitName: '1' },
  SG: { code: 'sg', name: '声光', unit: '1', unitName: '1' },
  SP: { code: 'sp', name: '视频', unit: '1', unitName: '1' }
}

// 监测内容图标规范（用于监测类型图标选择）
const MonitorContentIconEnum = {
  BSW: { code: 'BSW', name: '表面水平位移', icon: 'bsw' },
  SSW: { code: 'SSW', name: '深部水平位移', icon: 'ssw' },
  BC: { code: 'BC', name: '表面沉降', icon: 'bc' },
  QJ: { code: 'QJ', name: '倾角', icon: 'qj' },
  LF: { code: 'LF', name: '裂缝', icon: 'lf' },
  JY: { code: 'JY', name: '降雨量', icon: 'jy' },
  DW: { code: 'DW', name: '地下水水位', icon: 'dw' },
  KY: { code: 'KY', name: '孔隙水压力', icon: 'ky' },
  TL: { code: 'TL', name: '土压力', icon: 'tl' },
  SY: { code: 'SY', name: '渗透压力', icon: 'sy' },
  TH: { code: 'TH', name: '土体含水率', icon: 'th' },
  WD: { code: 'WD', name: '温度', icon: 'wd' },
  JSD: { code: 'JSD', name: '加速度', icon: 'jsd' },
  SC: { code: 'SC', name: '深部沉降', icon: 'sc' },
  LS: { code: 'LS', name: '形变-拉伸', icon: 'ls' },
  YS: { code: 'YS', name: '形变-压缩', icon: 'ys' },
  NQ: { code: 'NQ', name: '形变-挠曲', icon: 'nq' },
  ZL: { code: 'ZL', name: '轴力', icon: 'zl' },
  WJ: { code: 'WJ', name: '弯矩', icon: 'wj' },
  ZZL: { code: 'ZZL', name: '自振频率', icon: 'zzl' },
  GNSS: { code: 'GNSS', name: '表面位移（GNSS）', icon: 'gnss' },
  SP: { code: 'SP', name: '视频', icon: 'sp' },
  NW: { code: 'NW', name: '泥水位', icon: 'nw' },
  DX: { code: 'DX', name: '断线', icon: 'dx' },
  SG: { code: 'SG', name: '声光', icon: 'sg' }
}

const indicatorTypeOptions = Object.values(IndicatorTypeEnum).map(item => ({
  code: item.code,
  name: item.name,
  unit: item.unit,
  unitName: item.unitName
}))


const typeIconList = Object.values(MonitorContentIconEnum).map(item => ({
  code: item.code,
  name: item.name,
  icon: item.icon,
  path: `/jc-icon/green/${item.icon}_green.png`
}))

interface ModelAttrItem {
  attrCode: string
  attrName: string
  indicatorType: string
  rangeMin: number
  rangeMax: number
  unit: string
}

interface MonitorTypeItem {
  id: string
  code: string
  name: string
  icon: string
  deviceType: string
  deviceTypeName: string
  createUser: string
  createTime: string
  updateDept: string
  updateUser: string
  updateTime: string
  isDeleted: boolean
  deleteTime: string
  modelAttrs: ModelAttrItem[]
  modelSummary: string
  sensorCount: number
}

const loading = ref(false)
const tableData = ref<MonitorTypeItem[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const searchKeyword = ref('')
const dialogVisible = ref(false)
const dialogTitle = ref('')
const isView = ref(false)
const isEdit = ref(false)
const formRef = ref()
const typeIconDialogVisible = ref(false)

const formData = reactive<{
  id?: string
  code: string
  name: string
  icon: string
  deviceType: string
  createUser: string
  description: string,
  sortOrder: number,
  modelAttrs: ModelAttrItem[]
}>({
  code: '',
  name: '',
  icon: '',
  deviceType: '',
  createUser: '',
  description: '',
  sortOrder: 0,
  modelAttrs: []
})

const formRules = {
  code: [{ required: true, message: '请输入编号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }]
}

// 分页查询监测类型
const loadTableData = async () => {
  loading.value = true
  try {
    const token = getToken()
    const response = await axios.get('/api/v1/monitor-types/page', {
      params: {
        pageNum: currentPage.value,
        pageSize: pageSize.value,
        code: searchKeyword.value || undefined,
        name: searchKeyword.value || undefined
      },
      headers: { Authorization: `Bearer ${token}` }
    })

    if (response.data.code === 200) {
      const data = response.data.data
       tableData.value = (data.rows || []).map((item: any) => ({
        ...item,
        deviceTypeName: item.deviceType === 1 ? '直连设备' : item.deviceType === 2 ? '传感器' : item.deviceType === 3 ? 'RTU' : '-'
      }))
      total.value = data.total || 0
    } else {
      ElMessage.error(response.data.msg || '获取数据失败')
    }
  } catch (error) {
    console.error('请求失败:', error)
    ElMessage.error('网络请求失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  loadTableData()
}

const handleSizeChange = () => {
  loadTableData()
}

const handlePageChange = () => {
  loadTableData()
}

// 新增监测类型
const handleAdd = () => {
  dialogTitle.value = '新增监测类型'
  isView.value = false
  isEdit.value = false
  Object.assign(formData, {
    id: undefined,
    code: '',
    name: '',
    icon: '',
    deviceType: '',
    createUser: '',
    description: '',
    sortOrder: 0,
    modelAttrs: []
  })
  dialogVisible.value = true
}

// 获取监测类型详情
const fetchDetail = async (id: string) => {
  loading.value = true
  try {
    const token = localStorage.getItem('token')
    const response = await axios.get(`/api/v1/monitor-types/${id}`, {
      headers: { Authorization: `Bearer ${token}` }
    })

    if (response.data.code === 200) {
      const data = response.data.data
      
      // 转换设备类型：后端返回的数字转成前端的字符串
      let deviceType = ''
      if (data.deviceType === 1) deviceType = 'DIRECT'
      else if (data.deviceType === 2) deviceType = 'SENSOR'
      else if (data.deviceType === 3) deviceType = 'RTU'
      
      // 转换监测内容：后端的 contents 转成前端的 modelAttrs
      const modelAttrs = (data.contents || []).map((c: any) => ({
        attrCode: c.code,
        attrName: c.name,
        indicatorType: c.indicatorType || '',
        rangeMin: 0,
        rangeMax: 100,
        unit: c.unit || ''
      }))
      
      Object.assign(formData, {
        id: String(data.id),
        code: data.code,
        name: data.name,
        deviceType: deviceType,
        icon: data.icon || '',
        description: data.description || '',
        sortOrder: data.sortOrder || 0,
        createUser: data.createBy || '',   
        modelAttrs: modelAttrs
      })

      console.log('回填后的 formData:', JSON.parse(JSON.stringify(formData)))
    } else {
      ElMessage.error(response.data.msg || '获取详情失败')
    }
  } catch (error) {
    console.error('获取详情失败:', error)
    ElMessage.error('网络请求失败')
  } finally {
    loading.value = false
  }
}

const handleEdit = async (row: MonitorTypeItem) => {
  dialogTitle.value = '编辑监测类型'
  isView.value = false
  isEdit.value = true
  await fetchDetail(row.id)
  dialogVisible.value = true
}

const handleView = async (row: MonitorTypeItem) => {
  dialogTitle.value = '查看监测类型'
  isView.value = true
  isEdit.value = false
  await fetchDetail(row.id)
  dialogVisible.value = true
}

const handleDelete = (row: MonitorTypeItem) => {
  ElMessageBox.confirm(`确定要删除监测类型"${row.name}"吗？`, '删除确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const token = getToken()
      const response = await axios.delete(`/api/v1/monitor-types/${row.id}`, {
        headers: { Authorization: `Bearer ${token}` }
      })
      
      if (response.data.code === 200) {
        ElMessage.success('删除成功')
        loadTableData()
      } else {
        ElMessage.error(response.data.msg || '删除失败')
      }
    } catch (error) {
      ElMessage.error('网络请求失败')
    }
  }).catch(() => {})
}

const handleExport = () => {
  ElMessage.info('正在导出...')
  setTimeout(() => {
    ElMessage.success('导出成功')
  }, 1000)
}

const handleSubmit = async () => {
  formRef.value.validate(async (valid: boolean) => {
    if (valid) {
      try {
        const token = localStorage.getItem('token')
        let response

        if (formData.id) {
          response = await axios.put(`/api/v1/monitor-types/${formData.id}`, {
            name: formData.name,
            deviceType: formData.deviceType === 'SENSOR' ? 2 : formData.deviceType === 'RTU' ? 3 : 1,
            icon: formData.icon,
            description: formData.description,
            sortOrder: formData.sortOrder,
            status: 1
          }, {
            headers: { Authorization: `Bearer ${token}` }
          })
        } else {
          response = await axios.post('/api/v1/monitor-types', {
            code: formData.code,
            name: formData.name,
            deviceType: formData.deviceType === 'SENSOR' ? 2 : formData.deviceType === 'RTU' ? 3 : 1,
            icon: formData.icon,
            description: formData.description,
            sortOrder: formData.sortOrder,
            status: 1
          }, {
            headers: { Authorization: `Bearer ${token}` }
          })
        }
        
        if (response.data.code === 200) {
          ElMessage.success(formData.id ? '修改成功' : '新增成功')
          dialogVisible.value = false
          loadTableData()
        } else {
          ElMessage.error(response.data.msg || '操作失败')
        }
      } catch (error) {
        console.error('提交失败:', error)
        ElMessage.error('网络请求失败')
      }
    }
  })
}

const handleAddModelAttr = () => {
  formData.modelAttrs.push({
    attrCode: '',
    attrName: '',
    indicatorType: '',
    rangeMin: 0,
    rangeMax: 100,
    unit: ''
  })
}

const handleRemoveModelAttr = (index: number) => {
  formData.modelAttrs.splice(index, 1)
}

const handleIndicatorTypeChange = (row: ModelAttrItem) => {
  const type = indicatorTypeOptions.find(opt => opt.code === row.indicatorType)
  if (type) {
    row.unit = type.unit
  }
}

const handleSelectTypeIcon = () => {
  typeIconDialogVisible.value = true
}

const handleTypeIconSelect = (item: { code: string; name: string; icon: string; path: string }) => {
  formData.icon = item.path
  typeIconDialogVisible.value = false
}

onMounted(() => {
  loadTableData()
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

.table-icon {
  width: 28px;
  height: 28px;
  object-fit: contain;
}

.empty-text {
  color: #909399;
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

/* 监测类型图标选择器 */
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
  border-color: #409eff;
  background: #f0f7ff;
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
  border: 1px solid #ebeef5;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
}

.icon-item:hover {
  border-color: #409eff;
  background: #f0f7ff;
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