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
          <el-icon class="search-icon"><Search /></el-icon>
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
      <el-button @click="handleRefresh" :loading="refreshing">刷新</el-button>
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
        <el-table-column prop="sn" label="SN" width="160" align="center">
          <template #default="{ row }">
            <span>{{ row.sn || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="authUsername" label="接入账号" width="120" align="center">
          <template #default="{ row }">
            <span>{{ row.authUsername || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="authStatus" label="账号状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.authStatus === 1 ? 'success' : 'danger'" effect="plain">
              {{ row.authStatus === 1 ? '有效' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
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
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <div class="op-cell">
              <el-button type="primary" text size="small" @click="handleView(row)">查看</el-button>
              <el-button type="primary" text size="small" @click="handleEdit(row)">编辑</el-button>
              <el-dropdown trigger="hover" @command="(cmd: string) => handleMoreCommand(cmd, row)">
                <el-button type="primary" text size="small">更多</el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="account">账号</el-dropdown-item>
                    <el-dropdown-item command="toggleAuth">{{ row.authStatus === 1 ? '禁用账号' : '启用账号' }}</el-dropdown-item>
                    <el-dropdown-item command="sensors">传感器</el-dropdown-item>
                    <el-dropdown-item command="copy">复制</el-dropdown-item>
                    <el-dropdown-item command="delete" divided>
                      <span style="color: #f56c6c">删除</span>
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
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
            <el-form-item label="设备SN" prop="sn">
              <el-input v-model="formData.sn" placeholder="请输入设备SN" :disabled="isView" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="厂商名称" prop="vendorName">
              <el-input v-model="formData.vendorName" placeholder="请输入厂商名称" :disabled="isView" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="设备类型" prop="deviceType">
              <el-select v-model="formData.deviceType" placeholder="请选择设备类型" :disabled="isView">
                <el-option label="单参数" :value="0" />
                <el-option label="多参数" :value="1" />
                <el-option label="本地组网" :value="2" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="网络类型" prop="networkType">
              <el-select v-model="formData.networkType" placeholder="请选择网络类型" :disabled="isView">
                <el-option label="蜂窝" :value="0" />
                <el-option label="NB-Iot" :value="1" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="接入协议" prop="protocolType">
              <el-select v-model="formData.protocolType" placeholder="请选择接入协议" :disabled="isView">
                <el-option label="MQTT" value="MQTT" />
                <el-option label="HTTP" value="HTTP" />
                <el-option label="COAP" value="COAP" />
              </el-select>
            </el-form-item>
          </el-col>
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

    <el-dialog
        v-model="authDialogVisible"
        :title="`设备账号[${currentAuthDevice?.name || ''}]`"
        width="640px"
        :close-on-click-modal="false"
        destroy-on-close
    >
      <el-descriptions :column="2" border v-if="authAccount">
        <el-descriptions-item label="设备编号">{{ currentAuthDevice?.code || '-' }}</el-descriptions-item>
        <el-descriptions-item label="设备名称">{{ currentAuthDevice?.name || '-' }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ authAccount.username || '-' }}</el-descriptions-item>
        <el-descriptions-item label="密码">{{ authAccount.password || '-' }}</el-descriptions-item>
        <el-descriptions-item label="账号状态">
          <el-tag :type="authAccount.authStatus === 1 ? 'success' : 'danger'" size="small">
            {{ authAccount.authStatus === 1 ? '有效' : '禁用' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="注册时间">{{ authAccount.registeredAt || '-' }}</el-descriptions-item>
        <el-descriptions-item label="最近鉴权">{{ authAccount.lastAuthTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="鉴权IP">{{ authAccount.lastAuthIp || '-' }}</el-descriptions-item>
      </el-descriptions>

      <template #footer>
        <el-button @click="authDialogVisible = false">关闭</el-button>
        <el-button
          :type="authAccount?.authStatus === 1 ? 'warning' : 'success'"
          @click="handleToggleAuthStatus(currentAuthDevice)"
          :loading="authStatusLoading"
        >
          {{ authAccount?.authStatus === 1 ? '禁用账号' : '启用账号' }}
        </el-button>
        <el-button type="primary" @click="handleResetPassword" :loading="authResetLoading">重置密码</el-button>
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
        <el-descriptions-item label="设备SN">{{ currentRow?.sn || '-' }}</el-descriptions-item>
        <el-descriptions-item label="接入协议">{{ currentRow?.protocolType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="注册来源">{{ currentRow?.registerSource || '-' }}</el-descriptions-item>
        <el-descriptions-item label="接入账号">{{ currentRow?.authUsername || '-' }}</el-descriptions-item>
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
      <el-table :data="sensorTableData" border size="small" v-loading="sensorLoading">
        <el-table-column prop="sensorCode" label="传感器编号" width="150" align="center" />
        <el-table-column prop="sensorName" label="传感器名称" width="150" align="center" />
        <el-table-column prop="monitorTypeName" label="监测类型" width="180" align="center" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="plain">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="属性配置" min-width="320" align="center">
          <template #default="{ row }">
            <div v-if="row.attrList?.length" class="attr-config-list">
              <div v-for="attr in row.attrList" :key="attr.id || attr.attrCode" class="attr-config-item">
                <span class="attr-name">{{ attr.attrName }}({{ attr.indicatorTypeName || '-' }}):</span>
                <span>{{ attr.initialValue ?? 0 }}</span>
                <span class="attr-unit">{{ attr.unit }}</span>
              </div>
            </div>
            <span v-else class="empty-text">暂无属性</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center">
          <template #default="{ row }">
            <el-button type="text" size="small" @click="handleEditSensor(row)">编辑</el-button>
            <el-button type="text" size="small" class="danger-text" @click="handleDeleteSensor(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <template #footer>
        <el-button @click="sensorDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog
        v-model="sensorFormDialogVisible"
        :title="sensorFormTitle"
        width="820px"
        :close-on-click-modal="false"
        destroy-on-close
    >
      <el-form ref="sensorFormRef" :model="sensorFormData" :rules="sensorFormRules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="传感器编号" prop="sensorCode">
              <el-input
                  v-model="sensorFormData.sensorCode"
                  placeholder="请输入传感器编号"
                  :disabled="sensorFormMode === 'edit'"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="传感器名称" prop="sensorName">
              <el-input v-model="sensorFormData.sensorName" placeholder="请输入传感器名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="监测类型" prop="monitorTypeId">
              <el-select
                  v-model="sensorFormData.monitorTypeId"
                  placeholder="请选择监测类型"
                  :disabled="sensorFormMode === 'edit'"
                  @change="handleMonitorTypeChange(sensorFormData)"
              >
                <el-option v-for="mt in monitorTypeList" :key="mt.id" :label="mt.name" :value="mt.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-select v-model="sensorFormData.status" placeholder="请选择状态">
                <el-option label="启用" :value="1" />
                <el-option label="禁用" :value="0" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">
          <span class="divider-title">属性配置</span>
        </el-divider>

        <el-table :data="sensorFormData.attrList" border size="small">
          <el-table-column prop="attrCode" label="属性编码" width="150" align="center" />
          <el-table-column prop="attrName" label="属性名称" width="150" align="center" />
          <el-table-column prop="indicatorTypeName" label="指标类型" width="120" align="center" />
          <el-table-column label="初始值" width="140" align="center">
            <template #default="{ row }">
              <el-input-number
                  v-model="row.initialValue"
                  :min="row.rangeMin"
                  :max="row.rangeMax"
                  controls-position="right"
                  size="small"
                  style="width: 110px"
              />
            </template>
          </el-table-column>
          <el-table-column label="量程范围" width="180" align="center">
            <template #default="{ row }">
              {{ row.rangeMin }} ~ {{ row.rangeMax }}
            </template>
          </el-table-column>
          <el-table-column prop="unit" label="单位" width="80" align="center" />
        </el-table>
      </el-form>

      <template #footer>
        <el-button @click="sensorFormDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSensorSubmit" :loading="sensorFormSubmitLoading">确定</el-button>
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
import {onMounted, reactive, ref} from 'vue'
import {ElMessage, ElMessageBox} from 'element-plus'
import {Search} from '@element-plus/icons-vue'
import {
  changeDeviceAuthStatus,
  copyDevice as copyDeviceApi,
  createDevice as createDeviceApi,
  deleteDevice as deleteDeviceApi,
  type DeviceAuthAccount,
  type DeviceItem,
  type DevicePageParams,
  getDeviceAuthAccount,
  getDeviceDetail,
  getDevicePage,
  resetDevicePassword,
  updateDevice as updateDeviceApi
} from '@/api/device'
import {getMonitorTypeListWithContents} from '@/api/monitorType'
import {
  createSensor,
  deleteSensor,
  getDeviceSensors,
  getSensorDetail,
  type SensorItem,
  updateSensor
} from '@/api/sensor'
import { getIconList } from '@/constants/monitorIcons'

const deviceIconList = getIconList()

interface SensorAttrItem {
  id?: number
  attrCode: string
  attrName: string
  indicatorType: string
  indicatorTypeName: string
  initialValue: number
  unit: string
  rangeMin: number
  rangeMax: number
  icon?: string
}

interface SensorFormModel {
  id?: number
  sensorCode: string
  sensorName: string
  monitorTypeId: number | null
  monitorTypeName: string
  status: number
  attrList: SensorAttrItem[]
}

interface MonitorTypeItem {
  id: number
  name: string
  modelAttrs: {
    attrCode: string
    attrName: string
    indicatorType: string
    indicatorTypeName: string
    rangeMin: number
    rangeMax: number
    unit: string
    icon?: string
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
const refreshing = ref(false)
const submitLoading = ref(false)
const sensorLoading = ref(false)
const sensorFormSubmitLoading = ref(false)
const authResetLoading = ref(false)
const authStatusLoading = ref(false)
const tableData = ref<DeviceItem[]>([])
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
const currentRow = ref<DeviceItem | null>(null)

const authDialogVisible = ref(false)
const currentAuthDevice = ref<DeviceItem | null>(null)
const authAccount = ref<DeviceAuthAccount | null>(null)

const sensorDialogVisible = ref(false)
const currentSensorDevice = ref<DeviceItem | null>(null)
const sensorTableData = ref<SensorItem[]>([])
const sensorFormDialogVisible = ref(false)
const sensorFormTitle = ref('新增传感器')
const sensorFormMode = ref<'add' | 'edit'>('add')

const deviceIconDialogVisible = ref(false)
const sensorFormRef = ref()

const formData = reactive<{
  id?: number
  code: string
  name: string
  sn: string
  deviceType: number | null
  networkType: number | null
  protocolType: string
  vendorName: string
  icon: string
  iconPath: string
  status: number
  sensorList: SensorItem[]
}>({
  code: '',
  name: '',
  sn: '',
  deviceType: 0,
  networkType: 0,
  protocolType: 'MQTT',
  vendorName: '',
  icon: '',
  iconPath: '',
  status: 1,
  sensorList: []
})

const sensorFormData = reactive<SensorFormModel>({
  sensorCode: '',
  sensorName: '',
  monitorTypeId: null,
  monitorTypeName: '',
  status: 1,
  attrList: []
})

const formRules = {
  code: [{ required: true, message: '请输入设备编号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入设备名称', trigger: 'blur' }]
}

const sensorFormRules = {
  sensorCode: [{ required: true, message: '请输入传感器编号', trigger: 'blur' }],
  sensorName: [{ required: true, message: '请输入传感器名称', trigger: 'blur' }],
  monitorTypeId: [{ required: true, message: '请选择监测类型', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
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
    const params: DevicePageParams = {
      pageNum: currentPage.value,
      pageSize: pageSize.value
    }
    if (searchKeyword.value) {
      params.code = searchKeyword.value
    }
    if (searchStatus.value !== '') params.status = searchStatus.value
    if (searchRunStatus.value !== '') params.runStatus = searchRunStatus.value
    const data = await getDevicePage(params)
    tableData.value = data.rows || []
    total.value = data.total || 0
  } catch (error) {
    console.error('请求失败:', error)
    ElMessage.error('网络请求失败')
  } finally {
    loading.value = false
  }
}

// 获取设备详情
const fetchDetail = async (id: number) => {
  loading.value = true
  try {
    return await getDeviceDetail(id)
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
    const result = await createDeviceApi({
      code: formData.code,
      name: formData.name,
      sn: formData.sn || undefined,
      deviceType: formData.deviceType,
      networkType: formData.networkType,
      protocolType: formData.protocolType,
      vendorName: formData.vendorName || undefined,
      icon: formData.icon,
      iconPath: formData.iconPath,
      status: formData.status
    })
    ElMessage.success('新增成功')
    dialogVisible.value = false
    await loadTableData()
    const row = tableData.value.find(item => item.id === result.id)
    await openAuthDialog(row || {
      id: result.id,
      code: formData.code,
      name: formData.name,
      status: formData.status
    }, {
      deviceId: result.id,
      username: result.username,
      password: result.password,
      authStatus: 1
    })
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
    await updateDeviceApi(Number(formData.id), {
      name: formData.name,
      sn: formData.sn || undefined,
      deviceType: formData.deviceType,
      networkType: formData.networkType,
      protocolType: formData.protocolType,
      vendorName: formData.vendorName || undefined,
      icon: formData.icon,
      iconPath: formData.iconPath,
      status: formData.status
    })
    ElMessage.success('修改成功')
    dialogVisible.value = false
    await loadTableData()
  } catch (error) {
    console.error('修改失败:', error)
    ElMessage.error('网络请求失败')
  } finally {
    submitLoading.value = false
  }
}

// 删除设备
const deleteDevice = async (id: number) => {
  try {
    await deleteDeviceApi(id)
    ElMessage.success('删除成功')
    await loadTableData()
  } catch (error) {
    console.error('删除失败:', error)
    ElMessage.error('网络请求失败')
  }
}

// 复制设备
const copyDevice = async (id: number) => {
  try {
    await copyDeviceApi(id)
    ElMessage.success('复制成功')
    await loadTableData()
  } catch (error) {
    console.error('复制失败:', error)
    ElMessage.error('网络请求失败')
  }
}

const openAuthDialog = async (device: DeviceItem, account?: DeviceAuthAccount) => {
  currentAuthDevice.value = device
  authAccount.value = account || await getDeviceAuthAccount(Number(device.id))
  authDialogVisible.value = true
}

// 获取监测类型列表（用于传感器配置）
// 使用批量接口一次加载所有类型及其内容，避免逐条拉取详情的 N+1 请求
const loadMonitorTypeList = async () => {
  try {
    const allTypes = await getMonitorTypeListWithContents()
    const details = (allTypes || [])
        .filter((item: any) => item.deviceType === 2)
        .map((item: any) => ({
          id: Number(item.id),
          name: item.name,
          modelAttrs: (item.contents || []).map((content: any) => ({
            attrCode: content.code,
            attrName: content.name,
            indicatorType: content.indicatorType,
            indicatorTypeName: indicatorTypeNameMap[content.indicatorType] || content.indicatorType || '-',
            rangeMin: content.rangeMin ?? 0,
            rangeMax: content.rangeMax ?? 999999,
            unit: content.unit || '',
            icon: content.icon || ''
          }))
        } as MonitorTypeItem))
    monitorTypeList.value = details
  } catch (error) {
    console.error('获取监测类型失败:', error)
    ElMessage.error('获取监测类型失败')
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

// 刷新页面
const handleRefresh = async () => {
  refreshing.value = true
  try {
    await loadTableData()
    ElMessage.success('刷新成功')
  } catch (error) {
    ElMessage.error('刷新失败')
  } finally {
    refreshing.value = false
  }
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
    sn: '',
    deviceType: 0,
    networkType: 0,
    protocolType: 'MQTT',
    vendorName: '',
    icon: '',
    iconPath: '',
    status: 1,
    sensorList: []
  })
  dialogVisible.value = true
}

const handleEdit = async (row: DeviceItem) => {
  dialogTitle.value = '编辑设备'
  isEdit.value = true
  isView.value = false
  Object.assign(formData, {
    id: row.id,
    code: row.code,
    name: row.name,
    sn: row.sn || '',
    deviceType: row.deviceType ?? 0,
    networkType: row.networkType ?? 0,
    protocolType: row.protocolType || 'MQTT',
    vendorName: row.vendorName || '',
    icon: row.icon || '',
    iconPath: row.iconPath || '',
    status: row.status,
    sensorList: []
  })
  dialogVisible.value = true
}

const handleView = async (row: DeviceItem) => {
  currentRow.value = row
  const detail = await fetchDetail(Number(row.id))
  if (detail) {
    currentRow.value = detail
    sensorList.value = detail.sensors || []
  }
  detailDialogVisible.value = true
}

const handleMoreCommand = (command: string, row: DeviceItem) => {
  const map: Record<string, () => void> = {
    account: () => handleViewAuth(row),
    toggleAuth: () => handleToggleAuthStatus(row),
    sensors: () => handleConfigSensors(row),
    copy: () => handleCopy(row),
    delete: () => handleDelete(row)
  }
  map[command]?.()
}

const handleDelete = (row: DeviceItem) => {
  ElMessageBox.confirm(`确定要删除设备"${row.name}"吗?`, '删除确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    deleteDevice(Number(row.id))
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

const handleCopy = (row: DeviceItem) => {
  ElMessageBox.confirm(`确定要复制设备"${row.name}"吗?`, '复制确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'info'
  }).then(() => {
    copyDevice(Number(row.id))
  }).catch(() => {})
}

const handleViewAuth = async (row: DeviceItem) => {
  try {
    await openAuthDialog(row)
  } catch (error) {
    console.error('获取设备账号失败:', error)
    ElMessage.error('获取设备账号失败')
  }
}

const handleToggleAuthStatus = async (row?: DeviceItem | null) => {
  if (!row?.id) {
    return
  }
  const currentStatus = row.authStatus ?? authAccount.value?.authStatus ?? 1
  const nextStatus = currentStatus === 1 ? 2 : 1
  const actionText = nextStatus === 1 ? '启用' : '禁用'
  try {
    const { value } = await ElMessageBox.prompt(`请输入${actionText}原因`, `${actionText}账号`, {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputPlaceholder: `例如：现场${actionText}账号`,
      inputValue: `现场${actionText}账号`
    })
    authStatusLoading.value = true
    const account = await changeDeviceAuthStatus(Number(row.id), nextStatus, value || undefined)
    if (currentAuthDevice.value?.id === row.id) {
      authAccount.value = account
      currentAuthDevice.value = {
        ...currentAuthDevice.value,
        authStatus: account.authStatus
      }
    }
    const tableRow = tableData.value.find(item => item.id === row.id)
    if (tableRow) {
      tableRow.authStatus = account.authStatus
    }
    ElMessage.success(`${actionText}成功`)
  } catch (error: any) {
    if (error === 'cancel' || error?.action === 'cancel' || error?.action === 'close') {
      return
    }
    console.error(`${actionText}账号失败:`, error)
    ElMessage.error(`${actionText}账号失败`)
  } finally {
    authStatusLoading.value = false
  }
}

const handleResetPassword = async () => {
  if (!currentAuthDevice.value?.id) {
    return
  }
  try {
    const { value } = await ElMessageBox.prompt('请输入重置原因', '重置密码', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputPlaceholder: '例如：现场更换设备',
      inputValue: '现场运维重置'
    })
    authResetLoading.value = true
    const result = await resetDevicePassword(Number(currentAuthDevice.value.id), value || undefined)
    authAccount.value = {
      deviceId: Number(currentAuthDevice.value.id),
      username: result.username,
      password: result.password,
      authStatus: authAccount.value?.authStatus ?? 1,
      registeredAt: authAccount.value?.registeredAt,
      lastAuthTime: authAccount.value?.lastAuthTime,
      lastAuthIp: authAccount.value?.lastAuthIp
    }
    ElMessage.success('密码已重置')
    await loadTableData()
  } catch (error: any) {
    if (error === 'cancel' || error?.action === 'cancel' || error?.action === 'close') {
      return
    }
    console.error('重置密码失败:', error)
    ElMessage.error('重置密码失败')
  } finally {
    authResetLoading.value = false
  }
}

const handleConfigSensors = async (row: DeviceItem) => {
  currentSensorDevice.value = row
  await loadSensorTableData(Number(row.id))
  sensorDialogVisible.value = true
}

const loadSensorTableData = async (deviceId: number) => {
  sensorLoading.value = true
  try {
    sensorTableData.value = await getDeviceSensors(deviceId)
  } catch (error) {
    console.error('获取传感器列表失败:', error)
    ElMessage.error('获取传感器列表失败')
    sensorTableData.value = []
  } finally {
    sensorLoading.value = false
  }
}

const resetSensorForm = () => {
  Object.assign(sensorFormData, {
    id: undefined,
    sensorCode: '',
    sensorName: '',
    monitorTypeId: null,
    monitorTypeName: '',
    status: 1,
    attrList: []
  })
}

const handleAddSensor = () => {
  sensorFormTitle.value = '新增传感器'
  sensorFormMode.value = 'add'
  resetSensorForm()
  sensorFormDialogVisible.value = true
}

const handleEditSensor = async (row: SensorItem) => {
  sensorFormTitle.value = '编辑传感器'
  sensorFormMode.value = 'edit'
  resetSensorForm()
  try {
    const detail = await getSensorDetail(Number(row.id))
    Object.assign(sensorFormData, {
      id: detail.id,
      sensorCode: detail.sensorCode,
      sensorName: detail.sensorName,
      monitorTypeId: detail.monitorTypeId,
      monitorTypeName: detail.monitorTypeName || '',
      status: detail.status,
      attrList: (detail.attrList || []).map((attr) => ({
        id: attr.id,
        attrCode: attr.attrCode,
        attrName: attr.attrName,
        indicatorType: attr.indicatorType || '',
        indicatorTypeName: attr.indicatorTypeName || '',
        initialValue: Number(attr.initialValue ?? 0),
        unit: attr.unit || '',
        rangeMin: Number(attr.rangeMin ?? 0),
        rangeMax: Number(attr.rangeMax ?? 999999),
        icon: attr.icon || ''
      }))
    })
    sensorFormDialogVisible.value = true
  } catch (error) {
    console.error('获取传感器详情失败:', error)
    ElMessage.error('获取传感器详情失败')
  }
}

const handleDeleteSensor = (row: SensorItem) => {
  ElMessageBox.confirm(`确定要删除传感器"${row.sensorName}"吗?`, '删除确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteSensor(Number(row.id))
      ElMessage.success('删除成功')
      await loadSensorTableData(Number(currentSensorDevice.value?.id))
    } catch (error) {
      console.error('删除传感器失败:', error)
      ElMessage.error('删除传感器失败')
    }
  }).catch(() => {})
}

const handleMonitorTypeChange = (row: SensorFormModel) => {
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
      rangeMax: attr.rangeMax,
      icon: attr.icon
    }))
  }
}

const validateSensorAttrs = () => {
  if (!sensorFormData.attrList.length) {
    ElMessage.warning('属性列表不能为空')
    return false
  }

  const codeSet = new Set<string>()
  for (let index = 0; index < sensorFormData.attrList.length; index += 1) {
    const attr = sensorFormData.attrList[index]
    attr.attrCode = attr.attrCode.trim()
    attr.attrName = attr.attrName.trim()
    if (!attr.attrCode) {
      ElMessage.warning(`第 ${index + 1} 行属性编码不能为空`)
      return false
    }
    if (!attr.attrName) {
      ElMessage.warning(`第 ${index + 1} 行属性名称不能为空`)
      return false
    }
    if (codeSet.has(attr.attrCode)) {
      ElMessage.warning(`属性编码 ${attr.attrCode} 重复`)
      return false
    }
    codeSet.add(attr.attrCode)
    if (attr.rangeMin > attr.rangeMax) {
      ElMessage.warning(`属性 ${attr.attrName} 的最小值不能大于最大值`)
      return false
    }
  }
  return true
}

const buildSensorPayload = () => ({
  sensorCode: sensorFormData.sensorCode.trim(),
  sensorName: sensorFormData.sensorName.trim(),
  monitorTypeId: Number(sensorFormData.monitorTypeId),
  status: sensorFormData.status,
  attrList: sensorFormData.attrList.map((attr) => ({
    id: attr.id,
    attrCode: attr.attrCode.trim(),
    attrName: attr.attrName.trim(),
    indicatorType: attr.indicatorType || undefined,
    indicatorTypeName: attr.indicatorTypeName || undefined,
    initialValue: attr.initialValue,
    unit: attr.unit || undefined,
    rangeMin: attr.rangeMin,
    rangeMax: attr.rangeMax,
    icon: attr.icon || undefined
  }))
})

const handleSensorSubmit = () => {
  sensorFormRef.value.validate(async (valid: boolean) => {
    if (!valid || !validateSensorAttrs()) {
      return
    }

    sensorFormSubmitLoading.value = true
    try {
      const payload = buildSensorPayload()
      if (sensorFormMode.value === 'add') {
        await createSensor(Number(currentSensorDevice.value?.id), payload)
        ElMessage.success('新增成功')
      } else if (sensorFormData.id) {
        await updateSensor(sensorFormData.id, {
          sensorName: payload.sensorName,
          status: payload.status,
          attrList: payload.attrList
        })
        ElMessage.success('修改成功')
      }
      sensorFormDialogVisible.value = false
      await loadSensorTableData(Number(currentSensorDevice.value?.id))
    } catch (error) {
      console.error('保存传感器失败:', error)
      ElMessage.error('保存传感器失败')
    } finally {
      sensorFormSubmitLoading.value = false
    }
  })
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
  border-color: #1890ff;
  background: #e6f7ff;
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

:deep(.el-descriptions) {
  margin-bottom: 20px;
}
</style>
