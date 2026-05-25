<template>
  <div class="device-page">
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">设备管理</h2>
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
      <el-select v-model="searchStatus" placeholder="设备状态" clearable class="status-select">
        <el-option label="正常" :value="1" />
        <el-option label="故障" :value="2" />
        <el-option label="维修" :value="3" />
        <el-option label="离线" :value="4" />
      </el-select>
      <el-select v-model="searchRunStatus" placeholder="运行状态" clearable class="run-status-select">
        <el-option label="在线" :value="1" />
        <el-option label="离线" :value="0" />
      </el-select>
      <el-button type="primary" @click="handleSearch">搜索</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>

    <div class="table-container">
      <el-table
          :data="tableData"
          border
          stripe
          v-loading="loading"
          :header-cell-style="{ background: '#f5f7fa', color: '#303133', fontWeight: 'bold' }"
      >
        <el-table-column label="图标" width="80" align="center">
          <template #default="{ row }">
            <img v-if="row.iconPath" :src="row.iconPath" class="table-icon" alt="icon" />
            <span v-else class="empty-text">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="code" label="编号" width="150" align="center" />
        <el-table-column prop="name" label="名称" min-width="180" align="center" />
        <el-table-column prop="statusName" label="设备状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" effect="plain">{{ row.statusName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="runStatus" label="运行状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.runStatus === 1 ? 'success' : 'danger'" effect="plain">
              {{ row.runStatus === 1 ? '在线' : '离线' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastReportTime" label="最近上报" width="180" align="center">
          <template #default="{ row }">
            <span v-if="row.lastReportTime">{{ row.lastReportTime }}</span>
            <span v-else class="empty-text">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" align="center" />
        <el-table-column label="操作" width="280" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="text" size="small" @click="handleView(row)">查看</el-button>
            <el-button type="text" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="text" size="small" @click="handleConfigSensors(row)">传感器</el-button>
            <el-button type="text" size="small" @click="handleCopy(row)">复制</el-button>
            <el-button type="text" size="small" class="danger-text" @click="handleDelete(row)">删除</el-button>
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

    <!-- 新增/编辑弹窗 -->
    <el-dialog
        v-model="dialogVisible"
        :title="dialogTitle"
        width="800px"
        :close-on-click-modal="false"
        destroy-on-close
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="编号" prop="code">
              <el-input v-model="formData.code" placeholder="请输入设备编号" :disabled="isEdit" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="名称" prop="name">
              <el-input v-model="formData.name" placeholder="请输入设备名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="图标" prop="icon">
              <div class="device-icon-selector" @click="!isView && handleSelectDeviceIcon()">
                <img v-if="formData.iconPath" :src="formData.iconPath" class="device-icon-img" alt="icon" />
                <span v-else class="device-icon-placeholder">点击选择图标</span>
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="设备状态" prop="status">
              <el-select v-model="formData.status" placeholder="请选择设备状态" :disabled="isView">
                <el-option label="正常" :value="1" />
                <el-option label="故障" :value="2" />
                <el-option label="维修" :value="3" />
                <el-option label="离线" :value="4" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <template #footer v-if="!isView">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
      </template>
      <template #footer v-else>
        <el-button @click="dialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 设备详情弹窗 -->
    <el-dialog
        v-model="detailDialogVisible"
        title="设备详情"
        width="900px"
        :close-on-click-modal="false"
        destroy-on-close
    >
      <el-descriptions :column="2" border>
        <el-descriptions-item label="设备编号">{{ currentRow?.code }}</el-descriptions-item>
        <el-descriptions-item label="设备名称">{{ currentRow?.name }}</el-descriptions-item>
        <el-descriptions-item label="设备状态">
          <el-tag :type="getStatusType(currentRow?.status || 0)" size="small">{{ currentRow?.statusName }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="运行状态">
          <el-tag :type="currentRow?.runStatus === 1 ? 'success' : 'danger'" size="small">
            {{ currentRow?.runStatus === 1 ? '在线' : '离线' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="最近上报时间">{{ currentRow?.lastReportTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ currentRow?.createTime || '-' }}</el-descriptions-item>
      </el-descriptions>

      <el-divider content-position="left">
        <span class="divider-title">传感器列表</span>
      </el-divider>

      <el-table :data="sensorList" border size="small" v-loading="sensorLoading">
        <el-table-column prop="sensorCode" label="传感器编号" width="150" align="center" />
        <el-table-column prop="sensorName" label="传感器名称" width="150" align="center" />
        <el-table-column prop="monitorTypeName" label="监测类型" width="150" align="center" />
        <el-table-column label="属性配置" min-width="250" align="center">
          <template #default="{ row }">
            <div v-for="attr in row.attrList" :key="attr.attrCode" class="attr-item">
              {{ attr.attrName }}: {{ attr.initialValue }}{{ attr.unit }}
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 传感器配置弹窗 -->
    <el-dialog
        v-model="sensorDialogVisible"
        :title="`传感器配置[${currentSensorDevice?.name || ''}]`"
        width="900px"
        :close-on-click-modal="false"
        destroy-on-close
    >
      <div class="device-info-bar">
        <span class="info-label">设备编号:</span>
        <span class="info-value">{{ currentSensorDevice?.code }}</span>
        <span class="info-label">设备名称:</span>
        <span class="info-value">{{ currentSensorDevice?.name }}</span>
      </div>
      <div class="sensor-toolbar">
        <el-button type="primary" size="small" @click="handleAddSensor">
          <span class="btn-icon">+</span> 添加传感器
        </el-button>
      </div>
      <el-table :data="formData.sensorList" border size="small">
        <el-table-column label="传感器编号" width="150" align="center">
          <template #default="{ row }">
            <el-input v-model="row.sensorCode" placeholder="编号" />
          </template>
        </el-table-column>
        <el-table-column label="传感器名称" width="150" align="center">
          <template #default="{ row }">
            <el-input v-model="row.sensorName" placeholder="名称" />
          </template>
        </el-table-column>
        <el-table-column label="监测类型" width="180" align="center">
          <template #default="{ row }">
            <el-select v-model="row.monitorTypeId" placeholder="选择监测类型" @change="handleMonitorTypeChange(row)">
              <el-option v-for="mt in monitorTypeList" :key="mt.id" :label="mt.name" :value="mt.id" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="属性初始值" min-width="300" align="center">
          <template #default="{ row }">
            <div v-if="row.attrList && row.attrList.length > 0" class="attr-config-list">
              <div v-for="(attr, idx) in row.attrList" :key="idx" class="attr-config-item">
                <span class="attr-name">{{ attr.attrName }}({{ attr.indicatorTypeName }}):</span>
                <el-input-number v-model="attr.initialValue" :min="attr.rangeMin" :max="attr.rangeMax" controls-position="right" size="small" style="width: 100px" />
                <span class="attr-unit">{{ attr.unit }}</span>
              </div>
            </div>
            <span v-else class="empty-text">请先选择监测类型</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" align="center">
          <template #default="{ $index }">
            <el-button type="text" size="small" class="danger-text" @click="handleRemoveSensor($index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <template #footer>
        <el-button @click="sensorDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSensorSubmit" :loading="sensorSubmitLoading">确定</el-button>
      </template>
    </el-dialog>

    <!-- 设备图标选择弹窗 -->
    <el-dialog v-model="deviceIconDialogVisible" title="选择设备图标" width="600px">
      <div class="icon-grid">
        <div
            v-for="item in deviceIconList"
            :key="item.code"
            class="icon-item"
            @click="handleDeviceIconSelect(item)"
        >
          <img :src="item.path" class="icon-select-img" :alt="item.name" />
          <span class="icon-name">{{ item.name }}</span>
        </div>
      </div>
      <template #footer>
        <el-button @click="deviceIconDialogVisible = false">取消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'
import { getMonitorTypeDetail, getMonitorTypeList } from '@/api/monitorType'

// 获取token
const getToken = () => localStorage.getItem('token')

// 监测内容图标规范
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

const deviceIconList = Object.values(MonitorContentIconEnum).map(item => ({
  code: item.code,
  name: item.name,
  icon: item.icon,
  path: `/jc-icon/green/${item.icon}_green.png`
}))

interface SensorAttrItem {
  attrCode: string
  attrName: string
  indicatorType: string
  indicatorTypeName: string
  initialValue: number
  unit: string
  rangeMin: number
  rangeMax: number
}

interface SensorItem {
  id?: string
  sensorCode: string
  sensorName: string
  monitorTypeId: string
  monitorTypeName: string
  attrList: SensorAttrItem[]
}

interface MonitorTypeItem {
  id: string
  name: string
  modelAttrs: {
    attrCode: string
    attrName: string
    indicatorType: string
    indicatorTypeName: string
    rangeMin: number
    rangeMax: number
    unit: string
  }[]
}

const indicatorTypeNameMap: Record<string, string> = {
  wy: '位移',
  wd: '温度',
  jd: '角度',
  yl: '压力',
  sw: '水位',
  jsd: '加速度',
  hsl: '含水率',
  ljn: '力矩',
  zdl: '震动频率',
  dl: '电量',
  dx: '断线',
  sg: '声光',
  sp: '视频'
}

const loading = ref(false)
const submitLoading = ref(false)
const sensorLoading = ref(false)
const sensorSubmitLoading = ref(false)
const tableData = ref<any[]>([])
const sensorList = ref<SensorItem[]>([])
const monitorTypeList = ref<MonitorTypeItem[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const searchKeyword = ref('')
const searchStatus = ref<number | ''>('')
const searchRunStatus = ref<number | ''>('')

const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const isView = ref(false)
const formRef = ref()

const detailDialogVisible = ref(false)
const currentRow = ref<any>(null)

const sensorDialogVisible = ref(false)
const currentSensorDevice = ref<any>(null)

const deviceIconDialogVisible = ref(false)

const formData = reactive<{
  id?: string
  code: string
  name: string
  icon: string
  iconPath: string
  status: number
  sensorList: SensorItem[]
}>({
  code: '',
  name: '',
  icon: '',
  iconPath: '',
  status: 1,
  sensorList: []
})

const formRules = {
  code: [{ required: true, message: '请输入设备编号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入设备名称', trigger: 'blur' }]
}

const getStatusType = (status: number) => {
  const types: Record<number, string> = {
    1: 'success',
    2: 'danger',
    3: 'warning',
    4: 'info'
  }
  return types[status] || 'default'
}

// ==================== API 请求 ====================

// 分页查询设备
const loadTableData = async () => {
  loading.value = true
  try {
    const token = getToken()
    const params: any = {
      pageNum: currentPage.value,
      pageSize: pageSize.value
    }
    if (searchKeyword.value) {
      params.code = searchKeyword.value
      params.name = searchKeyword.value
    }
    if (searchStatus.value !== '') params.status = searchStatus.value
    if (searchRunStatus.value !== '') params.runStatus = searchRunStatus.value

    const response = await axios.get('/api/v1/devices/page', {
      params,
      headers: { Authorization: `Bearer ${token}` }
    })

    if (response.data.code === 200) {
      const data = response.data.data
      tableData.value = data.rows || []
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

// 获取设备详情
const fetchDetail = async (id: string) => {
  loading.value = true
  try {
    const token = getToken()
    const response = await axios.get(`/api/v1/devices/${id}`, {
      headers: { Authorization: `Bearer ${token}` }
    })

    if (response.data.code === 200) {
      const data = response.data.data
      return data
    } else {
      ElMessage.error(response.data.msg || '获取详情失败')
      return null
    }
  } catch (error) {
    console.error('获取详情失败:', error)
    ElMessage.error('网络请求失败')
    return null
  } finally {
    loading.value = false
  }
}

// 新增设备
const createDevice = async () => {
  submitLoading.value = true
  try {
    const token = getToken()
    const response = await axios.post('/api/v1/devices', {
      code: formData.code,
      name: formData.name,
      icon: formData.icon,
      iconPath: formData.iconPath,
      status: formData.status
    }, {
      headers: { Authorization: `Bearer ${token}` }
    })

    if (response.data.code === 200) {
      ElMessage.success('新增成功')
      dialogVisible.value = false
      loadTableData()
    } else {
      ElMessage.error(response.data.msg || '新增失败')
    }
  } catch (error) {
    console.error('新增失败:', error)
    ElMessage.error('网络请求失败')
  } finally {
    submitLoading.value = false
  }
}

// 修改设备
const updateDevice = async () => {
  submitLoading.value = true
  try {
    const token = getToken()
    const response = await axios.put(`/api/v1/devices/${formData.id}`, {
      name: formData.name,
      icon: formData.icon,
      iconPath: formData.iconPath
    }, {
      headers: { Authorization: `Bearer ${token}` }
    })

    if (response.data.code === 200) {
      ElMessage.success('修改成功')
      dialogVisible.value = false
      loadTableData()
    } else {
      ElMessage.error(response.data.msg || '修改失败')
    }
  } catch (error) {
    console.error('修改失败:', error)
    ElMessage.error('网络请求失败')
  } finally {
    submitLoading.value = false
  }
}

// 删除设备
const deleteDevice = async (id: string, name: string) => {
  try {
    const token = getToken()
    const response = await axios.delete(`/api/v1/devices/${id}`, {
      headers: { Authorization: `Bearer ${token}` }
    })

    if (response.data.code === 200) {
      ElMessage.success('删除成功')
      loadTableData()
    } else {
      ElMessage.error(response.data.msg || '删除失败')
    }
  } catch (error) {
    console.error('删除失败:', error)
    ElMessage.error('网络请求失败')
  }
}

// 复制设备
const copyDevice = async (id: string) => {
  try {
    const token = getToken()
    const response = await axios.post(`/api/v1/devices/${id}/copy`, {}, {
      headers: { Authorization: `Bearer ${token}` }
    })

    if (response.data.code === 200) {
      ElMessage.success('复制成功')
      loadTableData()
    } else {
      ElMessage.error(response.data.msg || '复制失败')
    }
  } catch (error) {
    console.error('复制失败:', error)
    ElMessage.error('网络请求失败')
  }
}

// 获取监测类型列表（用于传感器配置）
const loadMonitorTypeList = async () => {
  try {
    const monitorTypes = await getMonitorTypeList()
    const details = await Promise.all(
      (monitorTypes || []).map(async (item: any) => {
        const detail = await getMonitorTypeDetail(Number(item.id))
        return {
          id: String(detail.id),
          name: detail.name,
          modelAttrs: (detail.contents || []).map((content: any) => ({
            attrCode: content.code,
            attrName: content.name,
            indicatorType: content.indicatorType,
            indicatorTypeName: indicatorTypeNameMap[content.indicatorType] || content.indicatorType || '-',
            rangeMin: content.rangeMin ?? 0,
            rangeMax: content.rangeMax ?? 999999,
            unit: content.unit || ''
          }))
        } as MonitorTypeItem
      })
    )
    monitorTypeList.value = details
  } catch (error) {
    console.error('获取监测类型失败:', error)
    ElMessage.error('获取监测类型失败')
  }
}

// 保存传感器配置
const saveSensors = async () => {
  sensorSubmitLoading.value = true
  try {
    const token = getToken()
    // 这里根据实际后端接口调整
    const response = await axios.put(`/api/v1/devices/${currentSensorDevice.value?.id}/sensors`, {
      sensors: formData.sensorList
    }, {
      headers: { Authorization: `Bearer ${token}` }
    })

    if (response.data.code === 200) {
      ElMessage.success('传感器配置保存成功')
      sensorDialogVisible.value = false
    } else {
      ElMessage.error(response.data.msg || '保存失败')
    }
  } catch (error) {
    console.error('保存传感器失败:', error)
    ElMessage.error('网络请求失败')
  } finally {
    sensorSubmitLoading.value = false
  }
}

// ==================== 事件处理方法 ====================

const handleSearch = () => {
  currentPage.value = 1
  loadTableData()
}

const handleReset = () => {
  searchKeyword.value = ''
  searchStatus.value = ''
  searchRunStatus.value = ''
  currentPage.value = 1
  loadTableData()
}

const handleSizeChange = () => {
  loadTableData()
}

const handlePageChange = () => {
  loadTableData()
}

const handleAdd = () => {
  dialogTitle.value = '新增设备'
  isEdit.value = false
  isView.value = false
  Object.assign(formData, {
    id: undefined,
    code: '',
    name: '',
    icon: '',
    iconPath: '',
    status: 1,
    sensorList: []
  })
  dialogVisible.value = true
}

const handleEdit = async (row: any) => {
  dialogTitle.value = '编辑设备'
  isEdit.value = true
  isView.value = false
  Object.assign(formData, {
    id: row.id,
    code: row.code,
    name: row.name,
    icon: row.icon || '',
    iconPath: row.iconPath || '',
    status: row.status,
    sensorList: []
  })
  dialogVisible.value = true
}

const handleView = async (row: any) => {
  currentRow.value = row
  const detail = await fetchDetail(row.id)
  if (detail) {
    sensorList.value = detail.sensors || []
  }
  detailDialogVisible.value = true
}

const handleDelete = (row: any) => {
  ElMessageBox.confirm(`确定要删除设备"${row.name}"吗?`, '删除确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    deleteDevice(row.id, row.name)
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
      if (formData.id) {
        updateDevice()
      } else {
        createDevice()
      }
    }
  })
}

const handleCopy = (row: any) => {
  ElMessageBox.confirm(`确定要复制设备"${row.name}"吗?`, '复制确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'info'
  }).then(() => {
    copyDevice(row.id)
  }).catch(() => {})
}

const handleConfigSensors = async (row: any) => {
  currentSensorDevice.value = row
  const detail = await fetchDetail(row.id)
  if (detail) {
    formData.sensorList = detail.sensors || []
  } else {
    formData.sensorList = []
  }
  sensorDialogVisible.value = true
}

const handleAddSensor = () => {
  formData.sensorList.push({
    sensorCode: '',
    sensorName: '',
    monitorTypeId: '',
    monitorTypeName: '',
    attrList: []
  })
}

const handleRemoveSensor = (index: number) => {
  formData.sensorList.splice(index, 1)
}

const handleMonitorTypeChange = (row: SensorItem) => {
  const mt = monitorTypeList.value.find(item => item.id === row.monitorTypeId)
  if (mt) {
    row.monitorTypeName = mt.name
    row.attrList = mt.modelAttrs.map(attr => ({
      attrCode: attr.attrCode,
      attrName: attr.attrName,
      indicatorType: attr.indicatorType,
      indicatorTypeName: attr.indicatorTypeName,
      initialValue: 0,
      unit: attr.unit,
      rangeMin: attr.rangeMin,
      rangeMax: attr.rangeMax
    }))
  }
}

const handleSensorSubmit = () => {
  saveSensors()
}

const handleSelectDeviceIcon = () => {
  deviceIconDialogVisible.value = true
}

const handleDeviceIconSelect = (item: { code: string; name: string; icon: string; path: string }) => {
  formData.icon = item.icon
  formData.iconPath = item.path
  deviceIconDialogVisible.value = false
}

onMounted(() => {
  loadTableData()
  loadMonitorTypeList()
})
</script>

<style scoped>
.device-page {
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
  align-items: center;
  flex-wrap: wrap;
}

.search-input {
  width: 250px;
}

.search-icon {
  font-size: 14px;
}

.status-select,
.run-status-select {
  width: 120px;
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

.sensor-toolbar {
  margin-bottom: 15px;
}

.device-info-bar {
  background: #f5f7fa;
  padding: 10px 15px;
  border-radius: 4px;
  margin-bottom: 15px;
}

.info-label {
  color: #909399;
  margin-right: 6px;
}

.info-value {
  color: #303133;
  font-weight: bold;
  margin-right: 20px;
}

.device-icon-selector {
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

.device-icon-selector:hover {
  border-color: #409eff;
  background: #f0f7ff;
}

.device-icon-img {
  width: 28px;
  height: 28px;
  object-fit: contain;
}

.device-icon-placeholder {
  color: #909399;
  font-size: 12px;
}

.attr-config-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.attr-config-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.attr-name {
  color: #606266;
  white-space: nowrap;
}

.attr-unit {
  color: #909399;
}

.attr-item {
  font-size: 13px;
  color: #606266;
  padding: 2px 0;
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

:deep(.el-descriptions) {
  margin-bottom: 20px;
}
</style>
