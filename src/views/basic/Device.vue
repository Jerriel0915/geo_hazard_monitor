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
        <el-button @click="handleBatchBind">批量绑定</el-button>
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
        <el-option label="正常" value="NORMAL" />
        <el-option label="故障" value="FAULT" />
        <el-option label="维修" value="REPAIR" />
        <el-option label="离线" value="OFFLINE" />
      </el-select>
      <el-select v-model="searchRunStatus" placeholder="运行状态" clearable class="run-status-select">
        <el-option label="在线" :value="1" />
        <el-option label="离线" :value="0" />
      </el-select>
      <el-button type="primary" @click="handleSearch">搜索</el-button>
    </div>

    <div class="table-container">
      <el-table
        :data="tableData"
        border
        stripe
        v-loading="loading"
        :header-cell-style="{ background: '#f5f7fa', color: '#303133', fontWeight: 'bold' }"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="code" label="编号" width="150" align="center" />
        <el-table-column prop="name" label="名称" min-width="180" align="center" />
        <el-table-column prop="statusName" label="设备状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" effect="plain">{{ row.statusName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="runStatus" label="运行状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.runStatus ? 'success' : 'danger'" effect="plain">
              {{ row.runStatus ? '在线' : '离线' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="hazardPointNames" label="关联隐患点" min-width="200" align="center">
          <template #default="{ row }">
            <span v-if="row.hazardPointNames" class="hazard-tags">
              <el-tag v-for="hp in row.hazardPointNames.split(',')" :key="hp" size="small" class="hazard-tag">{{ hp }}</el-tag>
            </span>
            <span v-else class="empty-text">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="monitorTypes" label="监测类型" min-width="150" align="center">
          <template #default="{ row }">
            <span v-if="row.monitorTypes">{{ row.monitorTypes }}</span>
            <span v-else class="empty-text">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="sensorCount" label="传感器数量" width="100" align="center">
          <template #default="{ row }">
            <el-tag type="info" effect="plain">{{ row.sensorCount || 0 }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastReportTime" label="最近上报" width="180" align="center">
          <template #default="{ row }">
            <span v-if="row.lastReportTime">{{ row.lastReportTime }}</span>
            <span v-else class="empty-text">-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="text" size="small" @click="handleView(row)">查看</el-button>
            <el-button type="text" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="text" size="small" @click="handleConfigSensors(row)">传感器</el-button>
            <el-button type="text" size="small" @click="handleReuse(row)">复用</el-button>
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
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="700px"
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
            <el-form-item label="设备类型" prop="deviceType">
              <el-input v-model="formData.deviceType" placeholder="请输入设备类型" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="设备状态" prop="status">
              <el-select v-model="formData.status" placeholder="请选择设备状态">
                <el-option label="正常" value="NORMAL" />
                <el-option label="故障" value="FAULT" />
                <el-option label="维修" value="REPAIR" />
                <el-option label="离线" value="OFFLINE" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="IP地址" prop="ipAddress">
              <el-input v-model="formData.ipAddress" placeholder="请输入IP地址" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="MAC地址" prop="macAddress">
              <el-input v-model="formData.macAddress" placeholder="请输入MAC地址" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="生产厂家" prop="manufacturer">
              <el-input v-model="formData.manufacturer" placeholder="请输入生产厂家" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="设备型号" prop="model">
              <el-input v-model="formData.model" placeholder="请输入设备型号" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="安装位置" prop="installLocation">
          <el-input v-model="formData.installLocation" placeholder="请输入安装位置" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="安装时间" prop="installTime">
              <el-date-picker v-model="formData.installTime" type="datetime" placeholder="选择安装时间" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="关联隐患点" prop="hazardPointNames">
              <el-select v-model="formData.hazardPointIds" multiple placeholder="请选择隐患点" style="width: 100%">
                <el-option v-for="hp in hazardPointList" :key="hp.id" :label="hp.name" :value="hp.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

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
        <el-descriptions-item label="设备类型">{{ currentRow?.deviceType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="设备状态">
          <el-tag :type="getStatusType(currentRow?.status || '')" size="small">{{ currentRow?.statusName }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="运行状态">
          <el-tag :type="currentRow?.runStatus ? 'success' : 'danger'" size="small">
            {{ currentRow?.runStatus ? '在线' : '离线' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="IP地址">{{ currentRow?.ipAddress || '-' }}</el-descriptions-item>
        <el-descriptions-item label="安装位置" :span="2">{{ currentRow?.installLocation || '-' }}</el-descriptions-item>
        <el-descriptions-item label="关联隐患点" :span="2">{{ currentRow?.hazardPointNames || '-' }}</el-descriptions-item>
        <el-descriptions-item label="最近上报时间">{{ currentRow?.lastReportTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="安装时间">{{ currentRow?.installTime || '-' }}</el-descriptions-item>
      </el-descriptions>

      <el-divider content-position="left">
        <span class="divider-title">传感器列表</span>
      </el-divider>

      <el-table :data="sensorList" border size="small">
        <el-table-column prop="sensorCode" label="传感器编号" width="150" align="center" />
        <el-table-column prop="sensorName" label="传感器名称" width="150" align="center" />
        <el-table-column prop="monitorTypeName" label="监测类型" width="150" align="center" />
        <el-table-column prop="monitorContentName" label="监测内容" width="100" align="center" />
        <el-table-column prop="dimension" label="维度" width="80" align="center">
          <template #default="{ row }">
            {{ row.dimension || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="initialValue" label="初始值" width="100" align="center">
          <template #default="{ row }">
            {{ row.initialValue }}{{ row.unit }}
          </template>
        </el-table-column>
        <el-table-column prop="rangeMin" label="量程范围" width="150" align="center">
          <template #default="{ row }">
            {{ row.rangeMin }} ~ {{ row.rangeMax }}{{ row.unit }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status ? 'success' : 'info'" size="small">{{ row.status ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog
      v-model="sensorDialogVisible"
      title="传感器配置"
      width="900px"
      :close-on-click-modal="false"
      destroy-on-close
    >
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
        <el-table-column label="监测内容" width="120" align="center">
          <template #default="{ row }">
            <el-select v-model="row.monitorContentCode" placeholder="选择内容">
              <el-option v-for="mc in monitorContentOptions" :key="mc.value" :label="mc.label" :value="mc.value" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="初始值" width="120" align="center">
          <template #default="{ row }">
            <el-input-number v-model="row.initialValue" :min="0" controls-position="right" />
          </template>
        </el-table-column>
        <el-table-column label="单位" width="80" align="center">
          <template #default="{ row }">
            <span>{{ row.unit }}</span>
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
        <el-button type="primary" @click="handleSensorSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="batchBindDialogVisible"
      title="批量绑定隐患点"
      width="500px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="batchBindFormRef" :model="batchBindFormData" label-width="100px">
        <el-form-item label="已选设备">
          <span>{{ selectedRows.length }} 台设备</span>
        </el-form-item>
        <el-form-item label="隐患点" prop="hazardPointId">
          <el-select v-model="batchBindFormData.hazardPointId" placeholder="请选择隐患点" style="width: 100%">
            <el-option v-for="hp in hazardPointList" :key="hp.id" :label="hp.name" :value="hp.id" />
          </el-select>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="batchBindDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleBatchBindSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="reuseDialogVisible"
      title="设备复用"
      width="500px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="reuseFormRef" :model="reuseFormData" :rules="reuseFormRules" label-width="100px">
        <el-form-item label="原设备">
          <span>{{ currentRow?.name }} ({{ currentRow?.code }})</span>
        </el-form-item>
        <el-form-item label="新设备编号" prop="newCode">
          <el-input v-model="reuseFormData.newCode" placeholder="请输入新设备编号" />
        </el-form-item>
        <el-form-item label="新设备名称" prop="newName">
          <el-input v-model="reuseFormData.newName" placeholder="请输入新设备名称" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="reuseDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleReuseSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

interface DeviceItem {
  id: string
  code: string
  name: string
  deviceType?: string
  status: string
  statusName: string
  runStatus: number
  hazardPointIds?: string
  hazardPointNames: string
  ipAddress?: string
  macAddress?: string
  manufacturer?: string
  model?: string
  installLocation?: string
  installTime?: string
  lastReportTime?: string
  sensorCount: number
  monitorTypes: string
  createDept?: string
  createUser?: string
  createTime?: string
}

interface SensorItem {
  id: string
  sensorCode: string
  sensorName: string
  monitorTypeId: string
  monitorTypeCode: string
  monitorTypeName: string
  monitorContentCode: string
  monitorContentName: string
  dimension?: string
  initialValue: number
  unit: string
  rangeMin: number
  rangeMax: number
  mutationValue: number
  status: number
}

interface HazardPointItem {
  id: string
  name: string
}

interface MonitorTypeItem {
  id: string
  code: string
  name: string
  monitorContentCode: string
  unit: string
}

const MonitorContentEnum = {
  TEMPERATURE: { value: 'temperature', label: '温度', unit: '℃' },
  HUMIDITY: { value: 'humidity', label: '湿度', unit: '%' },
  RAINFALL: { value: 'rainfall', label: '雨量', unit: 'mm' },
  DISPLACEMENT: { value: 'displacement', label: '位移', unit: 'mm' },
  VELOCITY: { value: 'velocity', label: '流速', unit: 'm/s' },
  WATER_LEVEL: { value: 'water_level', label: '水位', unit: 'm' }
}

const monitorContentOptions = Object.values(MonitorContentEnum)

const loading = ref(false)
const tableData = ref<DeviceItem[]>([])
const sensorList = ref<SensorItem[]>([])
const selectedRows = ref<DeviceItem[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const searchKeyword = ref('')
const searchStatus = ref('')
const searchRunStatus = ref<number | ''>('')

const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const formRef = ref()

const detailDialogVisible = ref(false)
const currentRow = ref<DeviceItem | null>(null)

const sensorDialogVisible = ref(false)
const currentSensorDevice = ref<DeviceItem | null>(null)

const batchBindDialogVisible = ref(false)
const batchBindFormRef = ref()
const batchBindFormData = reactive({
  hazardPointId: ''
})

const reuseDialogVisible = ref(false)
const reuseFormRef = ref()
const reuseFormData = reactive({
  newCode: '',
  newName: ''
})
const reuseFormRules = {
  newCode: [{ required: true, message: '请输入新设备编号', trigger: 'blur' }],
  newName: [{ required: true, message: '请输入新设备名称', trigger: 'blur' }]
}

const hazardPointList = ref<HazardPointItem[]>([])
const monitorTypeList = ref<MonitorTypeItem[]>([])

const formData = reactive<{
  id?: string
  code: string
  name: string
  deviceType: string
  status: string
  ipAddress: string
  macAddress: string
  manufacturer: string
  model: string
  installLocation: string
  installTime: string
  hazardPointIds: string[]
  sensorList: SensorItem[]
}>({
  code: '',
  name: '',
  deviceType: '',
  status: 'NORMAL',
  ipAddress: '',
  macAddress: '',
  manufacturer: '',
  model: '',
  installLocation: '',
  installTime: '',
  hazardPointIds: [],
  sensorList: []
})

const formRules = {
  code: [{ required: true, message: '请输入设备编号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入设备名称', trigger: 'blur' }]
}

const getStatusType = (status: string) => {
  const types: Record<string, string> = {
    'NORMAL': 'success',
    'FAULT': 'danger',
    'REPAIR': 'warning',
    'OFFLINE': 'info'
  }
  return types[status] || 'default'
}

const initTableData = () => {
  loading.value = true
  setTimeout(() => {
    tableData.value = [
      {
        id: '1',
        code: 'DEV001',
        name: '雨量监测站-01',
        deviceType: '监测站',
        status: 'NORMAL',
        statusName: '正常',
        runStatus: 1,
        hazardPointIds: '1',
        hazardPointNames: '隐患点A',
        ipAddress: '192.168.1.101',
        installLocation: '隐患点A区域',
        installTime: '2024-01-10 10:00:00',
        lastReportTime: '2024-01-20 14:30:00',
        sensorCount: 1,
        monitorTypes: '雨量监测',
        createDept: '运维部',
        createUser: '张三',
        createTime: '2024-01-10 10:00:00'
      },
      {
        id: '2',
        code: 'DEV002',
        name: '位移监测站-01',
        deviceType: '监测站',
        status: 'NORMAL',
        statusName: '正常',
        runStatus: 1,
        hazardPointIds: '1,2',
        hazardPointNames: '隐患点A,隐患点B',
        ipAddress: '192.168.1.102',
        installLocation: '隐患点B区域',
        installTime: '2024-01-12 11:00:00',
        lastReportTime: '2024-01-20 14:25:00',
        sensorCount: 2,
        monitorTypes: '位移监测',
        createDept: '技术部',
        createUser: '李四',
        createTime: '2024-01-12 11:00:00'
      },
      {
        id: '3',
        code: 'DEV003',
        name: '温湿度监测站-01',
        deviceType: '监测站',
        status: 'FAULT',
        statusName: '故障',
        runStatus: 0,
        hazardPointIds: '2',
        hazardPointNames: '隐患点B',
        ipAddress: '192.168.1.103',
        installLocation: '隐患点B入口',
        installTime: '2024-01-15 09:00:00',
        lastReportTime: '2024-01-19 10:00:00',
        sensorCount: 2,
        monitorTypes: '温湿度监测',
        createDept: '运维部',
        createUser: '王五',
        createTime: '2024-01-15 09:00:00'
      },
      {
        id: '4',
        code: 'DEV004',
        name: '综合监测站-01',
        deviceType: '监测站',
        status: 'REPAIR',
        statusName: '维修',
        runStatus: 0,
        hazardPointIds: '3',
        hazardPointNames: '隐患点C',
        ipAddress: '192.168.1.104',
        installLocation: '隐患点C边坡',
        installTime: '2024-01-18 14:00:00',
        sensorCount: 0,
        monitorTypes: '',
        createDept: '技术部',
        createUser: '赵六',
        createTime: '2024-01-18 14:00:00'
      }
    ]
    total.value = tableData.value.length
    loading.value = false
  }, 500)
}

const initHazardPointList = () => {
  hazardPointList.value = [
    { id: '1', name: '隐患点A' },
    { id: '2', name: '隐患点B' },
    { id: '3', name: '隐患点C' },
    { id: '4', name: '隐患点D' }
  ]
}

const initMonitorTypeList = () => {
  monitorTypeList.value = [
    { id: '1', code: 'JCLX001', name: '雨量监测', monitorContentCode: 'rainfall', unit: 'mm' },
    { id: '2', code: 'JCLX002', name: '位移监测', monitorContentCode: 'displacement', unit: 'mm' },
    { id: '3', code: 'JCLX003', name: '温湿度监测', monitorContentCode: 'temperature', unit: '℃' },
    { id: '4', code: 'JCLX004', name: '水位监测', monitorContentCode: 'water_level', unit: 'm' }
  ]
}

const initSensorList = (deviceId: string) => {
  if (deviceId === '1') {
    sensorList.value = [
      { id: '1', sensorCode: 'SENSOR001', sensorName: '雨量传感器', monitorTypeId: '1', monitorTypeCode: 'JCLX001', monitorTypeName: '雨量监测', monitorContentCode: 'rainfall', monitorContentName: '雨量', dimension: '', initialValue: 0, unit: 'mm', rangeMin: 0, rangeMax: 500, mutationValue: 50, status: 1 }
    ]
  } else if (deviceId === '2') {
    sensorList.value = [
      { id: '2', sensorCode: 'SENSOR002', sensorName: '位移传感器-X', monitorTypeId: '2', monitorTypeCode: 'JCLX002', monitorTypeName: '位移监测', monitorContentCode: 'displacement', monitorContentName: '位移', dimension: 'x', initialValue: 0, unit: 'mm', rangeMin: -100, rangeMax: 100, mutationValue: 10, status: 1 },
      { id: '3', sensorCode: 'SENSOR003', sensorName: '位移传感器-Y', monitorTypeId: '2', monitorTypeCode: 'JCLX002', monitorTypeName: '位移监测', monitorContentCode: 'displacement', monitorContentName: '位移', dimension: 'y', initialValue: 0, unit: 'mm', rangeMin: -100, rangeMax: 100, mutationValue: 10, status: 1 }
    ]
  } else if (deviceId === '3') {
    sensorList.value = [
      { id: '4', sensorCode: 'SENSOR004', sensorName: '温度传感器', monitorTypeId: '3', monitorTypeCode: 'JCLX003', monitorTypeName: '温湿度监测', monitorContentCode: 'temperature', monitorContentName: '温度', dimension: '', initialValue: 25, unit: '℃', rangeMin: -40, rangeMax: 80, mutationValue: 5, status: 1 },
      { id: '5', sensorCode: 'SENSOR005', sensorName: '湿度传感器', monitorTypeId: '3', monitorTypeCode: 'JCLX003', monitorTypeName: '温湿度监测', monitorContentCode: 'humidity', monitorContentName: '湿度', dimension: '', initialValue: 60, unit: '%', rangeMin: 0, rangeMax: 100, mutationValue: 10, status: 1 }
    ]
  }
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

const handleSelectionChange = (selection: DeviceItem[]) => {
  selectedRows.value = selection
}

const handleAdd = () => {
  dialogTitle.value = '新增设备'
  isEdit.value = false
  Object.assign(formData, {
    id: undefined,
    code: '',
    name: '',
    deviceType: '',
    status: 'NORMAL',
    ipAddress: '',
    macAddress: '',
    manufacturer: '',
    model: '',
    installLocation: '',
    installTime: '',
    hazardPointIds: [],
    sensorList: []
  })
  dialogVisible.value = true
}

const handleEdit = (row: DeviceItem) => {
  dialogTitle.value = '编辑设备'
  isEdit.value = true
  const hpIds = row.hazardPointIds ? row.hazardPointIds.split(',') : []
  Object.assign(formData, {
    id: row.id,
    code: row.code,
    name: row.name,
    deviceType: row.deviceType || '',
    status: row.status,
    ipAddress: row.ipAddress || '',
    macAddress: row.macAddress || '',
    manufacturer: row.manufacturer || '',
    model: row.model || '',
    installLocation: row.installLocation || '',
    installTime: row.installTime || '',
    hazardPointIds: hpIds,
    sensorList: []
  })
  dialogVisible.value = true
}

const handleView = (row: DeviceItem) => {
  currentRow.value = row
  initSensorList(row.id)
  detailDialogVisible.value = true
}

const handleDelete = (row: DeviceItem) => {
  ElMessageBox.confirm(`确定要删除设备"${row.name}"吗?`, '删除确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    const index = tableData.value.findIndex(item => item.id === row.id)
    if (index > -1) {
      tableData.value.splice(index, 1)
      total.value--
    }
    ElMessage.success('删除成功')
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
      ElMessage.success('保存成功')
      dialogVisible.value = false
      initTableData()
    }
  })
}

const handleConfigSensors = (row: DeviceItem) => {
  currentSensorDevice.value = row
  initSensorList(row.id)
  formData.sensorList = JSON.parse(JSON.stringify(sensorList.value))
  sensorDialogVisible.value = true
}

const handleAddSensor = () => {
  formData.sensorList.push({
    id: '',
    sensorCode: '',
    sensorName: '',
    monitorTypeId: '',
    monitorTypeCode: '',
    monitorTypeName: '',
    monitorContentCode: '',
    monitorContentName: '',
    dimension: '',
    initialValue: 0,
    unit: '',
    rangeMin: 0,
    rangeMax: 100,
    mutationValue: 0,
    status: 1
  })
}

const handleRemoveSensor = (index: number) => {
  formData.sensorList.splice(index, 1)
}

const handleMonitorTypeChange = (row: any) => {
  const mt = monitorTypeList.value.find(item => item.id === row.monitorTypeId)
  if (mt) {
    row.monitorTypeCode = mt.code
    row.monitorTypeName = mt.name
    row.monitorContentCode = mt.monitorContentCode
    row.unit = mt.unit
    const mc = monitorContentOptions.find(item => item.value === mt.monitorContentCode)
    if (mc) {
      row.monitorContentName = mc.label
    }
  }
}

const handleSensorSubmit = () => {
  ElMessage.success('传感器配置保存成功')
  sensorDialogVisible.value = false
}

const handleBatchBind = () => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请先选择设备')
    return
  }
  batchBindFormData.hazardPointId = ''
  batchBindDialogVisible.value = true
}

const handleBatchBindSubmit = () => {
  if (!batchBindFormData.hazardPointId) {
    ElMessage.warning('请选择隐患点')
    return
  }
  const hp = hazardPointList.value.find(item => item.id === batchBindFormData.hazardPointId)
  selectedRows.value.forEach(row => {
    row.hazardPointIds = batchBindFormData.hazardPointId
    row.hazardPointNames = hp?.name || ''
  })
  ElMessage.success('批量绑定成功')
  batchBindDialogVisible.value = false
}

const handleReuse = (row: DeviceItem) => {
  currentRow.value = row
  reuseFormData.newCode = ''
  reuseFormData.newName = ''
  reuseDialogVisible.value = true
}

const handleReuseSubmit = () => {
  reuseFormRef.value.validate((valid: boolean) => {
    if (valid) {
      ElMessage.success('设备复用成功，新设备编号: ' + reuseFormData.newCode)
      reuseDialogVisible.value = false
    }
  })
}

onMounted(() => {
  initTableData()
  initHazardPointList()
  initMonitorTypeList()
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

.hazard-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.hazard-tag {
  margin: 2px;
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

:deep(.el-form-item) {
  margin-bottom: 18px;
}

:deep(.el-descriptions) {
  margin-bottom: 20px;
}
</style>
