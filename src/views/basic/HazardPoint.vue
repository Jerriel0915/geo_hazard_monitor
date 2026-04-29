<template>
  <div class="hazard-point-page">
    <div class="page-container">
      <div class="group-panel" :style="{ width: groupPanelWidth + 'px' }">
        <div class="panel-header">
          <span class="panel-title">分组列表</span>
        </div>
        <div class="group-list">
          <div
            v-for="group in groupList"
            :key="group.id"
            :class="['group-item', { active: selectedGroupId === group.id }]"
            @click="handleSelectGroup(group)"
          >
            <span class="group-name">{{ group.name }}</span>
            <span class="group-count">({{ group.count }})</span>
          </div>
        </div>
      </div>

      <div class="resize-handle" @mousedown="startResize"></div>

      <div class="content-panel">
        <div class="page-header">
          <div class="header-left">
            <h2 class="page-title">隐患点管理</h2>
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
          <el-select v-model="searchStatus" placeholder="状态" clearable class="status-select">
            <el-option label="监测中" value="MONITORING" />
            <el-option label="停测中" value="PAUSED" />
            <el-option label="已完结" value="COMPLETED" />
          </el-select>
          <el-select v-model="searchRiskLevel" placeholder="风险等级" clearable class="risk-select">
            <el-option label="高" value="HIGH" />
            <el-option label="中" value="MEDIUM" />
            <el-option label="低" value="LOW" />
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
          >
            <el-table-column prop="code" label="编号" width="150" align="center" />
            <el-table-column prop="name" label="名称" min-width="200" align="center" />
            <el-table-column prop="statusName" label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)" :color="row.statusColor" effect="plain">
                  {{ row.statusName }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="riskLevel" label="风险等级" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="getRiskType(row.riskLevel)" size="small">{{ row.riskLevel }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="location" label="位置" min-width="200" align="center">
              <template #default="{ row }">
                <span>{{ row.location || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="deviceCount" label="设备数量" width="100" align="center">
              <template #default="{ row }">
                <el-tag type="info" effect="plain">{{ row.deviceCount || 0 }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="coordinates" label="经纬度" width="180" align="center">
              <template #default="{ row }">
                <span v-if="row.longitude && row.latitude">{{ row.longitude }}, {{ row.latitude }}</span>
                <span v-else class="empty-text">-</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="300" fixed="right" align="center">
              <template #default="{ row }">
                <el-button type="text" size="small" @click="handleView(row)">查看</el-button>
                <el-button type="text" size="small" @click="handleEdit(row)">编辑</el-button>
                <el-button type="text" size="small" @click="handleBindDevice(row)">绑定设备</el-button>
                <el-button type="text" size="small" @click="handleConfigAlarm(row)">告警配置</el-button>
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
      </div>
    </div>

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
              <el-input v-model="formData.code" placeholder="请输入隐患点编号" :disabled="isEdit" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="名称" prop="name">
              <el-input v-model="formData.name" placeholder="请输入隐患点名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="分组" prop="groupId">
              <el-select v-model="formData.groupId" placeholder="请选择分组">
                <el-option v-for="g in groupList" :key="g.id" :label="g.name" :value="g.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="风险等级" prop="riskLevel">
              <el-select v-model="formData.riskLevel" placeholder="请选择风险等级">
                <el-option label="高" value="HIGH" />
                <el-option label="中" value="MEDIUM" />
                <el-option label="低" value="LOW" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="经度" prop="longitude">
              <el-input-number v-model="formData.longitude" :precision="6" :step="0.000001" placeholder="经度" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="纬度" prop="latitude">
              <el-input-number v-model="formData.latitude" :precision="6" :step="0.000001" placeholder="纬度" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="海拔" prop="altitude">
              <el-input-number v-model="formData.altitude" :precision="2" placeholder="海拔高度(米)" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="走向" prop="strike">
              <el-input-number v-model="formData.strike" :min="0" :max="360" placeholder="走向角度(度)" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="位置描述" prop="location">
          <el-input v-model="formData.location" placeholder="请输入位置描述" />
        </el-form-item>
        <el-form-item label="详细地址" prop="address">
          <el-input v-model="formData.address" placeholder="请输入详细地址" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="formData.description" type="textarea" :rows="3" placeholder="请输入描述" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="detailDialogVisible"
      title="隐患点详情"
      width="1000px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-tabs v-model="activeTab">
        <el-tab-pane label="基本信息" name="basic">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="隐患点编号">{{ currentRow?.code }}</el-descriptions-item>
            <el-descriptions-item label="隐患点名称">{{ currentRow?.name }}</el-descriptions-item>
            <el-descriptions-item label="分组">{{ currentRow?.groupName }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="getStatusType(currentRow?.status || '')" size="small">{{ currentRow?.statusName }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="风险等级">
              <el-tag :type="getRiskType(currentRow?.riskLevel || '')" size="small">{{ currentRow?.riskLevel }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="经纬度" :span="2">
              {{ currentRow?.longitude }}, {{ currentRow?.latitude }}
            </el-descriptions-item>
            <el-descriptions-item label="海拔">{{ currentRow?.altitude }}米</el-descriptions-item>
            <el-descriptions-item label="走向">{{ currentRow?.strike }}°</el-descriptions-item>
            <el-descriptions-item label="位置描述" :span="2">{{ currentRow?.location || '-' }}</el-descriptions-item>
            <el-descriptions-item label="详细地址" :span="2">{{ currentRow?.address || '-' }}</el-descriptions-item>
            <el-descriptions-item label="描述" :span="2">{{ currentRow?.description || '-' }}</el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>

        <el-tab-pane label="绑定设备" name="devices">
          <el-table :data="boundDevices" border size="small">
            <el-table-column prop="deviceCode" label="设备编号" width="150" align="center" />
            <el-table-column prop="deviceName" label="设备名称" min-width="150" align="center" />
            <el-table-column prop="bindTime" label="绑定时间" width="180" align="center" />
            <el-table-column prop="status" label="设备状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="row.deviceStatus === 'NORMAL' ? 'success' : 'warning'" size="small">
                  {{ row.deviceStatus === 'NORMAL' ? '正常' : '故障' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100" align="center">
              <template #default="{ row }">
                <el-button type="text" size="small" class="danger-text" @click="handleUnbindDevice(row)">解绑</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="告警配置" name="alarms">
          <div class="alarm-toolbar">
            <el-button type="primary" size="small" @click="handleAddAlarmCriteria">
              <span class="btn-icon">+</span> 添加判据
            </el-button>
          </div>
          <el-table :data="alarmCriteriaList" border size="small">
            <el-table-column prop="name" label="判据名称" width="150" align="center" />
            <el-table-column prop="monitorTypeName" label="监测类型" width="120" align="center" />
            <el-table-column prop="monitorContentName" label="监测内容" width="100" align="center" />
            <el-table-column prop="thresholdValue" label="阈值" width="100" align="center">
              <template #default="{ row }">
                {{ row.thresholdValue }}{{ row.unit }}
              </template>
            </el-table-column>
            <el-table-column prop="alarmLevel" label="告警等级" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="getAlarmLevelType(row.alarmLevel)" size="small">{{ row.alarmLevel }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="isEnabled" label="状态" width="80" align="center">
              <template #default="{ row }">
                <el-switch v-model="row.isEnabled" @change="handleToggleAlarm(row)" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100" align="center">
              <template #default="{ row }">
                <el-button type="text" size="small" class="danger-text" @click="handleDeleteAlarm(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-divider content-position="left">
            <span class="divider-title">分发规则</span>
          </el-divider>

          <el-table :data="dispatchRules" border size="small">
            <el-table-column prop="name" label="规则名称" width="150" align="center" />
            <el-table-column prop="alarmLevel" label="告警等级" width="100" align="center" />
            <el-table-column prop="channel" label="通知渠道" width="100" align="center" />
            <el-table-column prop="recipientName" label="接收人" width="100" align="center" />
            <el-table-column prop="isEnabled" label="状态" width="80" align="center">
              <template #default="{ row }">
                <el-switch v-model="row.isEnabled" />
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="监测数据" name="monitorData">
          <div class="monitor-data-panel">
            <div class="data-filters">
              <el-select v-model="dataFilter.deviceId" placeholder="选择设备" clearable style="width: 150px">
                <el-option v-for="d in boundDevices" :key="d.deviceId" :label="d.deviceName" :value="d.deviceId" />
              </el-select>
              <el-select v-model="dataFilter.valueType" placeholder="值类型" clearable style="width: 150px">
                <el-option label="当前值" value="current" />
                <el-option label="小时变化" value="hour" />
                <el-option label="日变化" value="day" />
              </el-select>
              <el-button type="primary" size="small" @click="handleQueryData">查询</el-button>
            </div>
            <div class="data-placeholder">
              <span>监测数据图表区域</span>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>

      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
        <el-button v-if="currentRow?.status === 'MONITORING'" type="warning" @click="handlePauseMonitoring">暂停监测</el-button>
        <el-button v-if="currentRow?.status === 'PAUSED'" type="success" @click="handleResumeMonitoring">恢复监测</el-button>
        <el-button v-if="currentRow?.status !== 'COMPLETED'" type="info" @click="handleComplete">完结</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="bindDeviceDialogVisible"
      title="绑定设备"
      width="600px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-transfer
        v-model="selectedDeviceIds"
        :data="availableDevices"
        :titles="['可选设备', '已选设备']"
        filterable
        @change="handleDeviceTransferChange"
      />
      <template #footer>
        <el-button @click="bindDeviceDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleBindDeviceSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="alarmDialogVisible"
      title="添加告警判据"
      width="600px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="alarmFormRef" :model="alarmFormData" :rules="alarmFormRules" label-width="100px">
        <el-form-item label="判据名称" prop="name">
          <el-input v-model="alarmFormData.name" placeholder="请输入判据名称" />
        </el-form-item>
        <el-form-item label="监测类型" prop="monitorTypeId">
          <el-select v-model="alarmFormData.monitorTypeId" placeholder="请选择监测类型" @change="handleAlarmMonitorTypeChange">
            <el-option v-for="mt in monitorTypeList" :key="mt.id" :label="mt.name" :value="mt.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="监测内容" prop="monitorContentCode">
          <el-select v-model="alarmFormData.monitorContentCode" placeholder="请选择监测内容">
            <el-option v-for="mc in monitorContentOptions" :key="mc.value" :label="mc.label" :value="mc.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="阈值类型" prop="thresholdType">
          <el-select v-model="alarmFormData.thresholdType" placeholder="请选择阈值类型">
            <el-option label="绝对值" value="ABSOLUTE" />
            <el-option label="变化率" value="RATE" />
            <el-option label="百分比" value="PERCENT" />
          </el-select>
        </el-form-item>
        <el-form-item label="阈值" prop="thresholdValue">
          <el-input-number v-model="alarmFormData.thresholdValue" :min="0" />
        </el-form-item>
        <el-form-item label="告警等级" prop="alarmLevel">
          <el-select v-model="alarmFormData.alarmLevel" placeholder="请选择告警等级">
            <el-option label="提示" value="INFO" />
            <el-option label="警告" value="WARNING" />
            <el-option label="严重" value="CRITICAL" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="alarmDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAlarmSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

interface HazardPointItem {
  id: string
  code: string
  name: string
  groupId?: string
  groupName: string
  status: string
  statusName: string
  statusColor?: string
  riskLevel?: string
  longitude?: number
  latitude?: number
  altitude?: number
  location?: string
  address?: string
  description?: string
  strike?: number
  deviceCount: number
  deviceNames?: string
  createDept?: string
  createUser?: string
  createTime?: string
}

interface GroupItem {
  id: string
  name: string
  code: string
  count: number
}

interface BoundDevice {
  deviceId: string
  deviceCode: string
  deviceName: string
  bindTime: string
  deviceStatus: string
}

interface AlarmCriteria {
  id: string
  name: string
  monitorTypeId: string
  monitorTypeCode: string
  monitorTypeName: string
  monitorContentCode: string
  monitorContentName: string
  thresholdType: string
  thresholdValue: number
  unit: string
  alarmLevel: string
  isEnabled: boolean
}

interface DispatchRule {
  id: string
  name: string
  alarmLevel: string
  channel: string
  recipientName: string
  isEnabled: boolean
}

const MonitorContentEnum = {
  TEMPERATURE: { value: 'temperature', label: '温度', unit: '℃' },
  HUMIDITY: { value: 'humidity', label: '湿度', unit: '%' },
  RAINFALL: { value: 'rainfall', label: '雨量', unit: 'mm' },
  DISPLACEMENT: { value: 'displacement', label: '位移', unit: 'mm' }
}

const monitorContentOptions = Object.values(MonitorContentEnum)

const loading = ref(false)
const tableData = ref<HazardPointItem[]>([])
const groupList = ref<GroupItem[]>([])
const selectedGroupId = ref<string | null>(null)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const searchKeyword = ref('')
const searchStatus = ref('')
const searchRiskLevel = ref('')
const groupPanelWidth = ref(200)
const activeTab = ref('basic')

const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const formRef = ref()

const detailDialogVisible = ref(false)
const currentRow = ref<HazardPointItem | null>(null)
const boundDevices = ref<BoundDevice[]>([])
const alarmCriteriaList = ref<AlarmCriteria[]>([])
const dispatchRules = ref<DispatchRule[]>([])
const dataFilter = reactive({
  deviceId: '',
  valueType: 'current'
})

const bindDeviceDialogVisible = ref(false)
const availableDevices = ref<{ key: string; label: string }[]>([])
const selectedDeviceIds = ref<string[]>([])

const alarmDialogVisible = ref(false)
const alarmFormRef = ref()
const alarmFormData = reactive({
  name: '',
  monitorTypeId: '',
  monitorTypeCode: '',
  monitorTypeName: '',
  monitorContentCode: '',
  monitorContentName: '',
  thresholdType: 'ABSOLUTE',
  thresholdValue: 0,
  alarmLevel: 'WARNING'
})
const alarmFormRules = {
  name: [{ required: true, message: '请输入判据名称', trigger: 'blur' }],
  monitorTypeId: [{ required: true, message: '请选择监测类型', trigger: 'blur' }],
  thresholdValue: [{ required: true, message: '请输入阈值', trigger: 'blur' }],
  alarmLevel: [{ required: true, message: '请选择告警等级', trigger: 'blur' }]
}

const monitorTypeList = ref<{ id: string; code: string; name: string }[]>([])

const formData = reactive({
  code: '',
  name: '',
  groupId: '',
  riskLevel: '',
  longitude: 0,
  latitude: 0,
  altitude: 0,
  strike: 0,
  location: '',
  address: '',
  description: ''
})

const formRules = {
  code: [{ required: true, message: '请输入隐患点编号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入隐患点名称', trigger: 'blur' }]
}

const getStatusType = (status: string) => {
  const types: Record<string, string> = {
    'MONITORING': 'success',
    'PAUSED': 'warning',
    'COMPLETED': 'info'
  }
  return types[status] || 'default'
}

const getRiskType = (level: string) => {
  const types: Record<string, string> = {
    'HIGH': 'danger',
    'MEDIUM': 'warning',
    'LOW': 'success'
  }
  return types[level] || 'default'
}

const getAlarmLevelType = (level: string) => {
  const types: Record<string, string> = {
    'INFO': 'success',
    'WARNING': 'warning',
    'CRITICAL': 'danger'
  }
  return types[level] || 'default'
}

const initTableData = () => {
  loading.value = true
  setTimeout(() => {
    tableData.value = [
      {
        id: '1',
        code: 'HP001',
        name: '龙潭寺滑坡隐患点',
        groupId: '1',
        groupName: '未知分组',
        status: 'MONITORING',
        statusName: '监测中',
        statusColor: '#67C23A',
        riskLevel: 'HIGH',
        longitude: 104.156789,
        latitude: 30.678901,
        altitude: 520,
        location: '龙潭寺镇北侧',
        address: '成都市成华区龙潭寺路',
        description: '该区域存在滑坡风险，需要重点监测',
        deviceCount: 2,
        deviceNames: '雨量监测站-01, 位移监测站-01',
        createDept: '技术部',
        createUser: '张三',
        createTime: '2024-01-15 10:30:00'
      },
      {
        id: '2',
        code: 'HP002',
        name: '青城山崩塌隐患点',
        groupId: '2',
        groupName: '高风险区',
        status: 'MONITORING',
        statusName: '监测中',
        statusColor: '#67C23A',
        riskLevel: 'HIGH',
        longitude: 103.589234,
        latitude: 30.891234,
        altitude: 1200,
        location: '青城山景区',
        address: '都江堰市青城山镇',
        description: '岩石崩塌风险较高',
        deviceCount: 1,
        deviceNames: '温湿度监测站-01',
        createDept: '监测部',
        createUser: '李四',
        createTime: '2024-01-16 14:20:00'
      },
      {
        id: '3',
        code: 'HP003',
        name: '瓦屋山泥石流隐患点',
        groupId: '3',
        groupName: '中风险区',
        status: 'MONITORING',
        statusName: '监测中',
        statusColor: '#67C23A',
        riskLevel: 'MEDIUM',
        longitude: 102.891234,
        latitude: 29.589234,
        altitude: 1500,
        location: '瓦屋山脚',
        address: '眉山市洪雅县瓦屋山镇',
        description: '雨季可能出现泥石流',
        deviceCount: 1,
        createDept: '运维部',
        createUser: '王五',
        createTime: '2024-01-17 09:15:00'
      },
      {
        id: '4',
        code: 'HP004',
        name: '峨眉山边坡隐患点',
        groupId: '4',
        groupName: '低风险区',
        status: 'PAUSED',
        statusName: '停测中',
        statusColor: '#E6A23C',
        riskLevel: 'LOW',
        longitude: 103.334567,
        latitude: 29.556789,
        altitude: 800,
        location: '峨眉山景区',
        address: '乐山市峨眉山市峨眉山',
        description: '边坡稳定性较差',
        deviceCount: 0,
        createDept: '技术部',
        createUser: '赵六',
        createTime: '2024-01-18 11:00:00'
      }
    ]
    total.value = tableData.value.length
    loading.value = false
  }, 500)
}

const initGroupList = () => {
  groupList.value = [
    { id: 'all', name: '全部', code: 'ALL', count: 4 },
    { id: '1', name: '未知分组', code: 'DEFAULT', count: 1 },
    { id: '2', name: '高风险区', code: 'HIGH_RISK', count: 1 },
    { id: '3', name: '中风险区', code: 'MEDIUM_RISK', count: 1 },
    { id: '4', name: '低风险区', code: 'LOW_RISK', count: 1 }
  ]
}

const initMonitorTypeList = () => {
  monitorTypeList.value = [
    { id: '1', code: 'JCLX001', name: '雨量监测' },
    { id: '2', code: 'JCLX002', name: '位移监测' },
    { id: '3', code: 'JCLX003', name: '温湿度监测' }
  ]
}

const startResize = (e: MouseEvent) => {
  const startX = e.clientX
  const startWidth = groupPanelWidth.value

  const onMouseMove = (e: MouseEvent) => {
    const diff = e.clientX - startX
    groupPanelWidth.value = Math.max(150, Math.min(400, startWidth + diff))
  }

  const onMouseUp = () => {
    document.removeEventListener('mousemove', onMouseMove)
    document.removeEventListener('mouseup', onMouseUp)
  }

  document.addEventListener('mousemove', onMouseMove)
  document.addEventListener('mouseup', onMouseUp)
}

const handleSelectGroup = (group: GroupItem) => {
  selectedGroupId.value = group.id === 'all' ? null : group.id
  handleSearch()
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
  dialogTitle.value = '新增隐患点'
  isEdit.value = false
  Object.assign(formData, {
    code: '',
    name: '',
    groupId: '',
    riskLevel: '',
    longitude: 0,
    latitude: 0,
    altitude: 0,
    strike: 0,
    location: '',
    address: '',
    description: ''
  })
  dialogVisible.value = true
}

const handleEdit = (row: HazardPointItem) => {
  dialogTitle.value = '编辑隐患点'
  isEdit.value = true
  Object.assign(formData, {
    code: row.code,
    name: row.name,
    groupId: row.groupId || '',
    riskLevel: row.riskLevel || '',
    longitude: row.longitude || 0,
    latitude: row.latitude || 0,
    altitude: row.altitude || 0,
    strike: row.strike || 0,
    location: row.location || '',
    address: row.address || '',
    description: row.description || ''
  })
  dialogVisible.value = true
}

const handleView = (row: HazardPointItem) => {
  currentRow.value = row
  activeTab.value = 'basic'
  initBoundDevices(row.id)
  initAlarmCriteria(row.id)
  initDispatchRules(row.id)
  detailDialogVisible.value = true
}

const handleDelete = (row: HazardPointItem) => {
  ElMessageBox.confirm(`确定要删除隐患点"${row.name}"吗?`, '删除确认', {
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

const initBoundDevices = (hazardPointId: string) => {
  if (hazardPointId === '1') {
    boundDevices.value = [
      { deviceId: '1', deviceCode: 'DEV001', deviceName: '雨量监测站-01', bindTime: '2024-01-15 10:00:00', deviceStatus: 'NORMAL' },
      { deviceId: '2', deviceCode: 'DEV002', deviceName: '位移监测站-01', bindTime: '2024-01-16 11:00:00', deviceStatus: 'NORMAL' }
    ]
  } else if (hazardPointId === '2') {
    boundDevices.value = [
      { deviceId: '3', deviceCode: 'DEV003', deviceName: '温湿度监测站-01', bindTime: '2024-01-17 09:00:00', deviceStatus: 'FAULT' }
    ]
  } else {
    boundDevices.value = []
  }
}

const initAlarmCriteria = (hazardPointId: string) => {
  if (hazardPointId === '1') {
    alarmCriteriaList.value = [
      { id: '1', name: '雨量告警', monitorTypeId: '1', monitorTypeCode: 'JCLX001', monitorTypeName: '雨量监测', monitorContentCode: 'rainfall', monitorContentName: '雨量', thresholdType: 'ABSOLUTE', thresholdValue: 100, unit: 'mm', alarmLevel: 'WARNING', isEnabled: true },
      { id: '2', name: '位移X轴告警', monitorTypeId: '2', monitorTypeCode: 'JCLX002', monitorTypeName: '位移监测', monitorContentCode: 'displacement', monitorContentName: '位移', thresholdType: 'RATE', thresholdValue: 10, unit: 'mm', alarmLevel: 'CRITICAL', isEnabled: true }
    ]
  } else {
    alarmCriteriaList.value = []
  }
}

const initDispatchRules = (hazardPointId: string) => {
  if (hazardPointId === '1') {
    dispatchRules.value = [
      { id: '1', name: '重大告警通知', alarmLevel: 'CRITICAL', channel: 'SMS', recipientName: '张三', isEnabled: true },
      { id: '2', name: '重大告警通知', alarmLevel: 'CRITICAL', channel: 'WECHAT', recipientName: '张三', isEnabled: true },
      { id: '3', name: '一般告警通知', alarmLevel: 'WARNING', channel: 'SYSTEM', recipientName: '监测员', isEnabled: true }
    ]
  } else {
    dispatchRules.value = []
  }
}

const handleBindDevice = (row: HazardPointItem) => {
  currentRow.value = row
  availableDevices.value = [
    { key: '1', label: '雨量监测站-01' },
    { key: '2', label: '位移监测站-01' },
    { key: '3', label: '温湿度监测站-01' },
    { key: '4', label: '综合监测站-01' }
  ]
  selectedDeviceIds.value = boundDevices.value.map(d => d.deviceId)
  bindDeviceDialogVisible.value = true
}

const handleDeviceTransferChange = (val: string[]) => {
  selectedDeviceIds.value = val
}

const handleBindDeviceSubmit = () => {
  ElMessage.success('设备绑定成功')
  bindDeviceDialogVisible.value = false
}

const handleUnbindDevice = (row: BoundDevice) => {
  ElMessageBox.confirm(`确定要解绑设备"${row.deviceName}"吗?`, '解绑确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    ElMessage.success('解绑成功')
    if (currentRow.value) {
      initBoundDevices(currentRow.value.id)
    }
  }).catch(() => {})
}

const handleConfigAlarm = (row: HazardPointItem) => {
  currentRow.value = row
  initAlarmCriteria(row.id)
  initDispatchRules(row.id)
  activeTab.value = 'alarms'
  detailDialogVisible.value = true
}

const handleAddAlarmCriteria = () => {
  Object.assign(alarmFormData, {
    name: '',
    monitorTypeId: '',
    monitorTypeCode: '',
    monitorTypeName: '',
    monitorContentCode: '',
    monitorContentName: '',
    thresholdType: 'ABSOLUTE',
    thresholdValue: 0,
    alarmLevel: 'WARNING'
  })
  alarmDialogVisible.value = true
}

const handleAlarmMonitorTypeChange = (val: string) => {
  const mt = monitorTypeList.value.find(item => item.id === val)
  if (mt) {
    alarmFormData.monitorTypeCode = mt.code
    alarmFormData.monitorTypeName = mt.name
  }
}

const handleAlarmSubmit = () => {
  alarmFormRef.value.validate((valid: boolean) => {
    if (valid) {
      ElMessage.success('判据添加成功')
      alarmDialogVisible.value = false
      if (currentRow.value) {
        initAlarmCriteria(currentRow.value.id)
      }
    }
  })
}

const handleToggleAlarm = (row: AlarmCriteria) => {
  ElMessage.success(`判据${row.isEnabled ? '启用' : '停用'}成功`)
}

const handleDeleteAlarm = (row: AlarmCriteria) => {
  ElMessageBox.confirm(`确定要删除判据"${row.name}"吗?`, '删除确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    ElMessage.success('删除成功')
    if (currentRow.value) {
      initAlarmCriteria(currentRow.value.id)
    }
  }).catch(() => {})
}

const handleQueryData = () => {
  ElMessage.info('正在加载监测数据...')
}

const handlePauseMonitoring = () => {
  ElMessageBox.confirm('确定要暂停监测吗？暂停后不再进行预警和自动化报告，但可以接收监测数据和查询。', '暂停监测确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    if (currentRow.value) {
      currentRow.value.status = 'PAUSED'
      currentRow.value.statusName = '停测中'
    }
    ElMessage.success('已暂停监测')
  }).catch(() => {})
}

const handleResumeMonitoring = () => {
  ElMessageBox.confirm('确定要恢复监测吗？', '恢复监测确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'info'
  }).then(() => {
    if (currentRow.value) {
      currentRow.value.status = 'MONITORING'
      currentRow.value.statusName = '监测中'
    }
    ElMessage.success('已恢复监测')
  }).catch(() => {})
}

const handleComplete = () => {
  ElMessageBox.confirm('确定要完结此隐患点吗？完结后将停止监测，不再可以编辑、预警和自动化报告，但可以接收监测数据和查询。', '完结确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    if (currentRow.value) {
      currentRow.value.status = 'COMPLETED'
      currentRow.value.statusName = '已完结'
    }
    ElMessage.success('已完结')
    detailDialogVisible.value = false
  }).catch(() => {})
}

onMounted(() => {
  initTableData()
  initGroupList()
  initMonitorTypeList()
})
</script>

<style scoped>
.hazard-point-page {
  padding: 20px;
  background: #fff;
  border-radius: 8px;
  min-height: calc(100% - 40px);
}

.page-container {
  display: flex;
  height: calc(100vh - 180px);
}

.group-panel {
  background: #fafafa;
  border-right: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
}

.panel-header {
  padding: 15px;
  border-bottom: 1px solid #e4e7ed;
}

.panel-title {
  font-weight: bold;
  color: #303133;
}

.group-list {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
}

.group-item {
  padding: 12px 15px;
  cursor: pointer;
  border-radius: 4px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.group-item:hover {
  background: #f0f7ff;
}

.group-item.active {
  background: #e6f0ff;
  color: #409eff;
}

.group-name {
  font-size: 14px;
}

.group-count {
  font-size: 12px;
  color: #909399;
}

.resize-handle {
  width: 5px;
  cursor: col-resize;
  background: transparent;
}

.resize-handle:hover {
  background: #409eff;
}

.content-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding-left: 20px;
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
.risk-select {
  width: 120px;
}

.table-container {
  flex: 1;
  background: #fff;
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

.alarm-toolbar {
  margin-bottom: 15px;
}

.monitor-data-panel {
  padding: 10px 0;
}

.data-filters {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
}

.data-placeholder {
  height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fafafa;
  border: 1px dashed #dcdfe6;
  border-radius: 4px;
  color: #909399;
}

:deep(.el-form-item) {
  margin-bottom: 18px;
}

:deep(.el-descriptions) {
  margin-bottom: 20px;
}
</style>
