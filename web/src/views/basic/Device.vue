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
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <!-- 新增/编辑/复制弹窗 -->
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
                <el-option label="正常" value="NORMAL" />
                <el-option label="故障" value="FAULT" />
                <el-option label="维修" value="REPAIR" />
                <el-option label="离线" value="OFFLINE" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="关联隐患点">
          <div class="hazard-bind-section">
            <el-select v-model="formData.hazardPointIds" multiple placeholder="请选择隐患点（非必填）" style="width: 100%" :disabled="isView" @change="handleHazardPointChange">
              <el-option v-for="hp in hazardPointList" :key="hp.id" :label="hp.name" :value="hp.id" />
            </el-select>
            <el-button v-if="formData.hazardPointIds.length > 0 && !isView" type="primary" size="small" @click="handleOpenMap" style="margin-top: 8px">
              在地图上指定安装位置
            </el-button>
          </div>
        </el-form-item>
      </el-form>

      <template #footer v-if="!isView">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
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
          <el-tag :type="getStatusType(currentRow?.status || '')" size="small">{{ currentRow?.statusName }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="运行状态">
          <el-tag :type="currentRow?.runStatus ? 'success' : 'danger'" size="small">
            {{ currentRow?.runStatus ? '在线' : '离线' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="关联隐患点" :span="2">{{ currentRow?.hazardPointNames || '-' }}</el-descriptions-item>
        <el-descriptions-item label="最近上报时间">{{ currentRow?.lastReportTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="传感器数量">{{ currentRow?.sensorCount || 0 }}</el-descriptions-item>
      </el-descriptions>

      <el-divider content-position="left">
        <span class="divider-title">传感器列表</span>
      </el-divider>

      <el-table :data="sensorList" border size="small">
        <el-table-column prop="sensorCode" label="传感器编号" width="150" align="center" />
        <el-table-column prop="sensorName" label="传感器名称" width="150" align="center" />
        <el-table-column prop="monitorTypeName" label="监测类型" width="150" align="center" />
        <el-table-column prop="attrSummary" label="属性配置" min-width="250" align="center">
          <template #default="{ row }">
            <div v-for="attr in row.attrList" :key="attr.attrCode" class="attr-item">
              {{ attr.attrName }}: {{ attr.initialValue }}{{ attr.unit }}
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status ? 'success' : 'info'" size="small">{{ row.status ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 传感器配置弹窗 -->
    <el-dialog
      v-model="sensorDialogVisible"
      title="传感器配置"
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
        <el-button type="primary" @click="handleSensorSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 地图弹窗：指定设备安装坐标 -->
    <el-dialog
      v-model="mapDialogVisible"
      title="指定设备安装位置"
      width="800px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <div class="map-container">
        <div id="device-map" ref="mapRef" style="width: 100%; height: 400px;"></div>
      </div>
      <div class="map-hazard-list">
        <div v-for="hp in selectedHazardPoints" :key="hp.id" class="map-hazard-item">
          <span class="hazard-name">{{ hp.name }}</span>
          <span class="hazard-coords" v-if="hp.installLng && hp.installLat">
            坐标: {{ hp.installLng.toFixed(6) }}, {{ hp.installLat.toFixed(6) }}
          </span>
          <span class="hazard-coords" v-else>点击地图设置坐标</span>
        </div>
      </div>
      <template #footer>
        <el-button @click="mapDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleMapConfirm">确定</el-button>
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
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'

interface DeviceItem {
  id: string
  code: string
  name: string
  icon: string
  iconPath: string
  status: string
  statusName: string
  runStatus: number
  hazardPointIds?: string
  hazardPointNames: string
  lastReportTime?: string
  sensorCount: number
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
  attrList: SensorAttrItem[]
  status: number
}

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

interface HazardPointItem {
  id: string
  name: string
  longitude?: number
  latitude?: number
  installLng?: number
  installLat?: number
}

interface MonitorTypeItem {
  id: string
  code: string
  name: string
  icon: string
  modelAttrs: ModelAttrItem[]
}

interface ModelAttrItem {
  attrCode: string
  attrName: string
  indicatorType: string
  indicatorTypeName: string
  rangeMin: number
  rangeMax: number
  unit: string
}

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

const loading = ref(false)
const tableData = ref<DeviceItem[]>([])
const sensorList = ref<SensorItem[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const searchKeyword = ref('')
const searchStatus = ref('')
const searchRunStatus = ref<number | ''>('')

const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const isView = ref(false)
const formRef = ref()

const detailDialogVisible = ref(false)
const currentRow = ref<DeviceItem | null>(null)

const sensorDialogVisible = ref(false)
const currentSensorDevice = ref<DeviceItem | null>(null)

const mapDialogVisible = ref(false)
const mapRef = ref<HTMLDivElement | null>(null)
let mapInstance: L.Map | null = null
let currentMarker: L.Marker | null = null
const selectedHazardPoints = ref<HazardPointItem[]>([])
const currentHazardPointIndex = ref(0)

const deviceIconDialogVisible = ref(false)

const hazardPointList = ref<HazardPointItem[]>([])
const monitorTypeList = ref<MonitorTypeItem[]>([])

const formData = reactive<{
  id?: string
  code: string
  name: string
  icon: string
  iconPath: string
  status: string
  runStatus: number
  lastReportTime: string
  hazardPointIds: string[]
  sensorList: SensorItem[]
}>({
  code: '',
  name: '',
  icon: '',
  iconPath: '',
  status: 'NORMAL',
  runStatus: 1,
  lastReportTime: '',
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
        icon: 'jy',
        iconPath: '/jc-icon/green/jy_green.png',
        status: 'NORMAL',
        statusName: '正常',
        runStatus: 1,
        hazardPointIds: '1',
        hazardPointNames: '龙潭寺滑坡隐患点',
        lastReportTime: '2024-01-20 14:30:00',
        sensorCount: 1,
        createDept: '运维部',
        createUser: '张三',
        createTime: '2024-01-10 10:00:00'
      },
      {
        id: '2',
        code: 'DEV002',
        name: '位移监测站-01',
        icon: 'bsw',
        iconPath: '/jc-icon/green/bsw_green.png',
        status: 'NORMAL',
        statusName: '正常',
        runStatus: 1,
        hazardPointIds: '1,2',
        hazardPointNames: '龙潭寺滑坡隐患点,青城山崩塌隐患点',
        lastReportTime: '2024-01-20 14:25:00',
        sensorCount: 2,
        createDept: '技术部',
        createUser: '李四',
        createTime: '2024-01-12 11:00:00'
      },
      {
        id: '3',
        code: 'DEV003',
        name: '温湿度监测站-01',
        icon: 'wd',
        iconPath: '/jc-icon/green/wd_green.png',
        status: 'FAULT',
        statusName: '故障',
        runStatus: 0,
        hazardPointIds: '2',
        hazardPointNames: '青城山崩塌隐患点',
        lastReportTime: '2024-01-19 10:00:00',
        sensorCount: 2,
        createDept: '运维部',
        createUser: '王五',
        createTime: '2024-01-15 09:00:00'
      },
      {
        id: '4',
        code: 'DEV004',
        name: '综合监测站-01',
        icon: 'gnss',
        iconPath: '/jc-icon/green/gnss_green.png',
        status: 'REPAIR',
        statusName: '维修',
        runStatus: 0,
        hazardPointIds: '3',
        hazardPointNames: '瓦屋山泥石流隐患点',
        lastReportTime: '',
        sensorCount: 0,
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
    { id: '1', name: '龙潭寺滑坡隐患点', longitude: 104.156789, latitude: 30.678901 },
    { id: '2', name: '青城山崩塌隐患点', longitude: 103.589234, latitude: 30.891234 },
    { id: '3', name: '瓦屋山泥石流隐患点', longitude: 102.891234, latitude: 29.589234 },
    { id: '4', name: '峨眉山边坡隐患点', longitude: 103.334567, latitude: 29.556789 }
  ]
}

const initMonitorTypeList = () => {
  monitorTypeList.value = [
    {
      id: '1', code: 'JCLX001', name: '雨量监测', icon: 'jy',
      modelAttrs: [
        { attrCode: 'rainfall', attrName: '降雨量', indicatorType: 'wy', indicatorTypeName: '位移', rangeMin: 0, rangeMax: 500, unit: 'mm' }
      ]
    },
    {
      id: '2', code: 'JCLX002', name: '位移监测', icon: 'bsw',
      modelAttrs: [
        { attrCode: 'displacement_x', attrName: 'X轴位移', indicatorType: 'wy', indicatorTypeName: '位移', rangeMin: -100, rangeMax: 100, unit: 'mm' },
        { attrCode: 'displacement_y', attrName: 'Y轴位移', indicatorType: 'wy', indicatorTypeName: '位移', rangeMin: -100, rangeMax: 100, unit: 'mm' }
      ]
    },
    {
      id: '3', code: 'JCLX003', name: '温湿度监测', icon: 'wd',
      modelAttrs: [
        { attrCode: 'temperature', attrName: '温度', indicatorType: 'wd', indicatorTypeName: '温度', rangeMin: -40, rangeMax: 80, unit: '℃' },
        { attrCode: 'humidity', attrName: '含水率', indicatorType: 'hsl', indicatorTypeName: '含水率', rangeMin: 0, rangeMax: 100, unit: '%' }
      ]
    },
    {
      id: '4', code: 'JCLX004', name: '地表位移监测', icon: 'gnss',
      modelAttrs: [
        { attrCode: 'displacement_x', attrName: 'X轴位移', indicatorType: 'wy', indicatorTypeName: '位移', rangeMin: -500, rangeMax: 500, unit: 'mm' },
        { attrCode: 'displacement_y', attrName: 'Y轴位移', indicatorType: 'wy', indicatorTypeName: '位移', rangeMin: -500, rangeMax: 500, unit: 'mm' },
        { attrCode: 'displacement_z', attrName: 'Z轴位移', indicatorType: 'wy', indicatorTypeName: '位移', rangeMin: -500, rangeMax: 500, unit: 'mm' }
      ]
    }
  ]
}

const initSensorList = (deviceId: string) => {
  if (deviceId === '1') {
    sensorList.value = [
      {
        id: '1', sensorCode: 'SENSOR001', sensorName: '雨量传感器', monitorTypeId: '1', monitorTypeCode: 'JCLX001', monitorTypeName: '雨量监测',
        attrList: [
          { attrCode: 'rainfall', attrName: '降雨量', indicatorType: 'wy', indicatorTypeName: '位移', initialValue: 0, unit: 'mm', rangeMin: 0, rangeMax: 500 }
        ],
        status: 1
      }
    ]
  } else if (deviceId === '2') {
    sensorList.value = [
      {
        id: '2', sensorCode: 'SENSOR002', sensorName: '位移传感器-X', monitorTypeId: '2', monitorTypeCode: 'JCLX002', monitorTypeName: '位移监测',
        attrList: [
          { attrCode: 'displacement_x', attrName: 'X轴位移', indicatorType: 'wy', indicatorTypeName: '位移', initialValue: 0, unit: 'mm', rangeMin: -100, rangeMax: 100 }
        ],
        status: 1
      },
      {
        id: '3', sensorCode: 'SENSOR003', sensorName: '位移传感器-Y', monitorTypeId: '2', monitorTypeCode: 'JCLX002', monitorTypeName: '位移监测',
        attrList: [
          { attrCode: 'displacement_y', attrName: 'Y轴位移', indicatorType: 'wy', indicatorTypeName: '位移', initialValue: 0, unit: 'mm', rangeMin: -100, rangeMax: 100 }
        ],
        status: 1
      }
    ]
  } else if (deviceId === '3') {
    sensorList.value = [
      {
        id: '4', sensorCode: 'SENSOR004', sensorName: '温度传感器', monitorTypeId: '3', monitorTypeCode: 'JCLX003', monitorTypeName: '温湿度监测',
        attrList: [
          { attrCode: 'temperature', attrName: '温度', indicatorType: 'wd', indicatorTypeName: '温度', initialValue: 25, unit: '℃', rangeMin: -40, rangeMax: 80 }
        ],
        status: 1
      },
      {
        id: '5', sensorCode: 'SENSOR005', sensorName: '湿度传感器', monitorTypeId: '3', monitorTypeCode: 'JCLX003', monitorTypeName: '温湿度监测',
        attrList: [
          { attrCode: 'humidity', attrName: '含水率', indicatorType: 'hsl', indicatorTypeName: '含水率', initialValue: 60, unit: '%', rangeMin: 0, rangeMax: 100 }
        ],
        status: 1
      }
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
    status: 'NORMAL',
    runStatus: 1,
    lastReportTime: '',
    hazardPointIds: [],
    sensorList: []
  })
  dialogVisible.value = true
}

const handleEdit = (row: DeviceItem) => {
  dialogTitle.value = '编辑设备'
  isEdit.value = true
  isView.value = false
  const hpIds = row.hazardPointIds ? row.hazardPointIds.split(',') : []
  Object.assign(formData, {
    id: row.id,
    code: row.code,
    name: row.name,
    icon: row.icon || '',
    iconPath: row.iconPath || '',
    status: row.status,
    runStatus: row.runStatus,
    lastReportTime: row.lastReportTime || '',
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

const handleCopy = (row: DeviceItem) => {
  dialogTitle.value = '复制设备'
  isEdit.value = false
  isView.value = false
  const hpIds = row.hazardPointIds ? row.hazardPointIds.split(',') : []
  Object.assign(formData, {
    id: undefined,
    code: '',
    name: row.name + '复制1',
    icon: row.icon || '',
    iconPath: row.iconPath || '',
    status: row.status,
    runStatus: row.runStatus,
    lastReportTime: row.lastReportTime || '',
    hazardPointIds: hpIds,
    sensorList: JSON.parse(JSON.stringify(sensorList.value))
  })
  dialogVisible.value = true
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
    attrList: [],
    status: 1
  })
}

const handleRemoveSensor = (index: number) => {
  formData.sensorList.splice(index, 1)
}

const handleMonitorTypeChange = (row: SensorItem) => {
  const mt = monitorTypeList.value.find(item => item.id === row.monitorTypeId)
  if (mt) {
    row.monitorTypeCode = mt.code
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
    // 自动更新设备图标（取第一个传感器的监测类型图标）
    if (formData.sensorList.length > 0 && !formData.icon) {
      const firstSensor = formData.sensorList[0]
      if (firstSensor.monitorTypeId) {
        const firstMt = monitorTypeList.value.find(item => item.id === firstSensor.monitorTypeId)
        if (firstMt) {
          formData.icon = firstMt.icon
          formData.iconPath = `/jc-icon/green/${firstMt.icon}_green.png`
        }
      }
    }
  }
}

const handleSensorSubmit = () => {
  ElMessage.success('传感器配置保存成功')
  sensorDialogVisible.value = false
}

const handleSelectDeviceIcon = () => {
  deviceIconDialogVisible.value = true
}

const handleDeviceIconSelect = (item: { code: string; name: string; icon: string; path: string }) => {
  formData.icon = item.icon
  formData.iconPath = item.path
  deviceIconDialogVisible.value = false
}

const handleHazardPointChange = () => {
  // 隐患点变更时，重置地图坐标
  selectedHazardPoints.value = hazardPointList.value.filter(hp => formData.hazardPointIds.includes(hp.id))
}

const handleOpenMap = () => {
  selectedHazardPoints.value = hazardPointList.value
    .filter(hp => formData.hazardPointIds.includes(hp.id))
    .map(hp => ({ ...hp }))
  mapDialogVisible.value = true
  nextTick(() => {
    initMap()
  })
}

const initMap = () => {
  if (!mapRef.value) return

  if (mapInstance) {
    mapInstance.remove()
  }

  mapInstance = L.map(mapRef.value).setView([30.67, 104.06], 10)

  // 天地图矢量底图
  L.tileLayer('https://t0.tianditu.gov.cn/vec_w/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=vec&STYLE=default&TILEMATRIXSET=w&FORMAT=tiles&TILEMATRIX={z}&TILEROW={y}&TILECOL={x}&tk=8dda07d4649c77efd0537a0ff0a1df13', {
    maxZoom: 18,
    attribution: '天地图'
  }).addTo(mapInstance)

  // 天地图注记
  L.tileLayer('https://t0.tianditu.gov.cn/cva_w/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=cva&STYLE=default&TILEMATRIXSET=w&FORMAT=tiles&TILEMATRIX={z}&TILEROW={y}&TILECOL={x}&tk=8dda07d4649c77efd0537a0ff0a1df13', {
    maxZoom: 18
  }).addTo(mapInstance)

  // 添加隐患点标记
  selectedHazardPoints.value.forEach((hp, index) => {
    if (hp.longitude && hp.latitude) {
      const marker = L.marker([hp.latitude, hp.longitude])
        .addTo(mapInstance!)
        .bindPopup(`${index + 1}. ${hp.name}`)
      marker.openPopup()
    }
  })

  // 点击地图设置设备安装坐标
  mapInstance.on('click', (e: L.LeafletMouseEvent) => {
    const hp = selectedHazardPoints.value[currentHazardPointIndex.value]
    if (hp) {
      hp.installLng = e.latlng.lng
      hp.installLat = e.latlng.lat

      if (currentMarker) {
        mapInstance!.removeLayer(currentMarker)
      }
      currentMarker = L.marker([e.latlng.lat, e.latlng.lng], {
        icon: L.divIcon({
          className: 'device-marker',
          html: `<div style="background:#409eff;color:#fff;padding:4px 8px;border-radius:4px;font-size:12px;">${hp.name}</div>`,
          iconSize: [100, 30],
          iconAnchor: [50, 15]
        })
      }).addTo(mapInstance!)

      currentHazardPointIndex.value = (currentHazardPointIndex.value + 1) % selectedHazardPoints.value.length
      ElMessage.info(`已设置 ${hp.name} 坐标，请设置下一个隐患点位置`)
    }
  })
}

const handleMapConfirm = () => {
  mapDialogVisible.value = false
  if (mapInstance) {
    mapInstance.remove()
    mapInstance = null
  }
  ElMessage.success('安装位置设置成功')
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

.table-icon {
  width: 28px;
  height: 28px;
  object-fit: contain;
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

/* 设备图标选择器 */
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

/* 隐患点绑定 */
.hazard-bind-section {
  display: flex;
  flex-direction: column;
}

/* 属性配置 */
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

/* 地图 */
.map-container {
  border: 1px solid #ebeef5;
  border-radius: 4px;
  overflow: hidden;
}

.map-hazard-list {
  margin-top: 15px;
}

.map-hazard-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: #f5f7fa;
  border-radius: 4px;
  margin-bottom: 8px;
}

.hazard-name {
  font-weight: bold;
  color: #303133;
}

.hazard-coords {
  color: #409eff;
  font-size: 13px;
}

/* 图标选择弹窗 */
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
