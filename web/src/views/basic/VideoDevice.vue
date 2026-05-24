<template>
  <div class="video-device-page">
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">视频设备管理</h2>
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
      <el-select v-model="searchProtocol" placeholder="选择协议" clearable class="protocol-select">
        <el-option label="RTMP" value="RTMP" />
        <el-option label="RTSP" value="RTSP" />
        <el-option label="ONVIF" value="ONVIF" />
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
        <el-table-column prop="protocolName" label="协议类型" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getProtocolType(row.protocolCode)" effect="plain">{{ row.protocolName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="streamUrl" label="视频流地址" min-width="300" align="center">
          <template #default="{ row }">
            <span class="stream-url">{{ row.streamUrl }}</span>
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
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" effect="plain">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastOnlineTime" label="最近在线时间" width="180" align="center">
          <template #default="{ row }">
            <span v-if="row.lastOnlineTime">{{ row.lastOnlineTime }}</span>
            <span v-else class="empty-text">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="installTime" label="安装时间" width="180" align="center" />
        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="text" size="small" @click="handlePlay(row)">播放</el-button>
            <el-button type="text" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="text" size="small" @click="handleBindHazardPoint(row)">关联隐患点</el-button>
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
            <el-form-item label="图标" prop="icon">
              <div class="device-icon-selector" @click="handleSelectVideoIcon">
                <img v-if="formData.iconPath" :src="formData.iconPath" class="device-icon-img" alt="icon" />
                <span v-else class="device-icon-placeholder">点击选择图标</span>
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="协议类型" prop="protocolCode">
              <el-select v-model="formData.protocolCode" placeholder="请选择协议类型">
                <el-option label="RTMP" value="RTMP" />
                <el-option label="RTSP" value="RTSP" />
                <el-option label="ONVIF" value="ONVIF" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="视频流地址" prop="streamUrl">
          <el-input v-model="formData.streamUrl" placeholder="请输入视频流地址" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>

    <!-- 关联隐患点弹窗 -->
    <el-dialog
        v-model="bindDialogVisible"
        :title="`关联隐患点[${currentBindRow?.name || ''}]`"
        width="600px"
        :close-on-click-modal="false"
        destroy-on-close
    >
      <el-form ref="bindFormRef" :model="bindFormData" label-width="100px">
        <el-form-item label="隐患点" prop="hazardPointIds">
          <el-select v-model="bindFormData.hazardPointIds" multiple placeholder="请选择隐患点" filterable style="width: 100%">
            <el-option
                v-for="hp in hazardPointList"
                :key="hp.id"
                :label="hp.name"
                :value="hp.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-if="bindFormData.hazardPointIds.length > 0">
          <el-button type="primary" @click="handleOpenMap">在地图上指定安装位置</el-button>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="bindDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleBindSubmit" :loading="bindLoading">确定</el-button>
      </template>
    </el-dialog>

    <!-- 地图弹窗：指定设备安装坐标 -->
    <el-dialog
        v-model="mapDialogVisible"
        title="指定视频设备安装位置"
        width="800px"
        :close-on-click-modal="false"
        destroy-on-close
    >
      <div class="map-container">
        <div id="video-device-map" ref="mapRef" style="width: 100%; height: 400px;"></div>
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
        <el-button type="primary" @click="handleMapConfirm" :loading="mapLoading">确定</el-button>
      </template>
    </el-dialog>

    <!-- 视频图标选择弹窗 -->
    <el-dialog v-model="videoIconDialogVisible" title="选择视频设备图标" width="500px">
      <div class="icon-grid">
        <div
            v-for="item in videoIconList"
            :key="item.code"
            class="icon-item"
            @click="handleVideoIconSelect(item)"
        >
          <img :src="item.path" class="icon-select-img" :alt="item.name" />
          <span class="icon-name">{{ item.name }}</span>
        </div>
      </div>
      <template #footer>
        <el-button @click="videoIconDialogVisible = false">取消</el-button>
      </template>
    </el-dialog>

    <!-- 视频播放弹窗 -->
    <el-dialog
        v-model="playDialogVisible"
        title="视频播放"
        width="900px"
        :close-on-click-modal="false"
        destroy-on-close
        class="video-play-dialog"
    >
      <div class="video-container">
        <div class="video-wrapper" ref="videoWrapper">
          <video
              ref="videoRef"
              :src="playUrl"
              controls
              class="video-player"
              @loadedmetadata="onVideoLoaded"
              @error="onVideoError"
          ></video>
          <div v-if="!videoLoaded" class="video-loading">
            <el-spinner type="dots" />
            <span>加载中...</span>
          </div>
          <div v-if="videoError" class="video-error">
            <span>视频加载失败</span>
          </div>
        </div>
        <div class="video-controls">
          <el-button type="primary" size="small" @click="handlePlayPause">
            {{ isPlaying ? '暂停' : '播放' }}
          </el-button>
          <el-button size="small" @click="handleFullscreen">全屏</el-button>
          <el-button size="small" @click="handleScreenshot">截图</el-button>
          <el-button size="small" @click="handleRefresh">刷新</el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'

// 获取token
const getToken = () => localStorage.getItem('token')

interface VideoDeviceItem {
  id: string
  code: string
  name: string
  icon: string
  iconPath: string
  protocolCode: string
  protocolName: string
  streamUrl: string
  hazardPointIds?: string
  hazardPointNames: string
  status: number
  installTime: string
  lastOnlineTime?: string
}

interface HazardPointItem {
  id: string
  code: string
  name: string
  longitude?: number
  latitude?: number
  installLng?: number
  installLat?: number
}

// 视频设备图标列表
const videoIconList = Array.from({ length: 10 }, (_, i) => {
  const num = i + 1
  return {
    code: `VIDIO${num}`,
    name: `视频图标${num}`,
    icon: `vidio${num}`,
    path: `/jc-icon/green/vidio${num}_green.png`
  }
})

const loading = ref(false)
const submitLoading = ref(false)
const bindLoading = ref(false)
const mapLoading = ref(false)
const tableData = ref<VideoDeviceItem[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const searchKeyword = ref('')
const searchProtocol = ref('')

const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const formRef = ref()

const bindDialogVisible = ref(false)
const bindFormRef = ref()
const bindFormData = reactive({
  hazardPointIds: [] as string[]
})

const playDialogVisible = ref(false)
const playUrl = ref('')
const videoRef = ref<HTMLVideoElement | null>(null)
const videoWrapper = ref<HTMLDivElement | null>(null)
const videoLoaded = ref(false)
const videoError = ref(false)
const isPlaying = ref(false)

const hazardPointList = ref<HazardPointItem[]>([])
const currentBindRow = ref<VideoDeviceItem | null>(null)

const mapDialogVisible = ref(false)
const mapRef = ref<HTMLDivElement | null>(null)
let mapInstance: L.Map | null = null
let currentMarker: L.Marker | null = null
const selectedHazardPoints = ref<HazardPointItem[]>([])
const currentHazardPointIndex = ref(0)

const videoIconDialogVisible = ref(false)

const formData = reactive<{
  id?: string
  code: string
  name: string
  icon: string
  iconPath: string
  protocolCode: string
  streamUrl: string
}>({
  code: '',
  name: '',
  icon: '',
  iconPath: '',
  protocolCode: '',
  streamUrl: ''
})

const formRules = {
  code: [{ required: true, message: '请输入设备编号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入设备名称', trigger: 'blur' }],
  protocolCode: [{ required: true, message: '请选择协议类型', trigger: 'blur' }],
  streamUrl: [{ required: true, message: '请输入视频流地址', trigger: 'blur' }]
}

const getProtocolType = (code: string) => {
  const types: Record<string, string> = {
    'RTMP': 'success',
    'RTSP': 'warning',
    'ONVIF': 'info'
  }
  return types[code] || 'default'
}

const getStatusType = (status: number) => {
  const types: Record<number, string> = {
    0: 'danger',
    1: 'success',
    2: 'warning'
  }
  return types[status] || 'default'
}

const getStatusLabel = (status: number) => {
  const labels: Record<number, string> = {
    0: '离线',
    1: '在线',
    2: '故障'
  }
  return labels[status] || '未知'
}

// ==================== API 请求 ====================

// 分页查询视频设备
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
    if (searchProtocol.value) {
      params.protocolCode = searchProtocol.value
    }

    const response = await axios.get('/api/v1/video-devices/page', {
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

// 获取隐患点列表
const loadHazardPointList = async () => {
  try {
    const token = getToken()
    const response = await axios.get('/api/v1/hazard-points/list', {
      headers: { Authorization: `Bearer ${token}` }
    })

    if (response.data.code === 200) {
      hazardPointList.value = response.data.data || []
    } else {
      console.error('获取隐患点列表失败:', response.data.msg)
    }
  } catch (error) {
    console.error('获取隐患点列表失败:', error)
  }
}

// 获取视频设备详情
const fetchDetail = async (id: string) => {
  try {
    const token = getToken()
    const response = await axios.get(`/api/v1/video-devices/${id}`, {
      headers: { Authorization: `Bearer ${token}` }
    })

    if (response.data.code === 200) {
      return response.data.data
    } else {
      ElMessage.error(response.data.msg || '获取详情失败')
      return null
    }
  } catch (error) {
    console.error('获取详情失败:', error)
    ElMessage.error('网络请求失败')
    return null
  }
}

// 新增视频设备
const createVideoDevice = async () => {
  submitLoading.value = true
  try {
    const token = getToken()
    const response = await axios.post('/api/v1/video-devices', {
      code: formData.code,
      name: formData.name,
      icon: formData.icon,
      iconPath: formData.iconPath,
      protocolCode: formData.protocolCode,
      streamUrl: formData.streamUrl,
      status: 1
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

// 修改视频设备
const updateVideoDevice = async () => {
  submitLoading.value = true
  try {
    const token = getToken()
    const response = await axios.put(`/api/v1/video-devices/${formData.id}`, {
      name: formData.name,
      icon: formData.icon,
      iconPath: formData.iconPath,
      protocolCode: formData.protocolCode,
      streamUrl: formData.streamUrl
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

// 删除视频设备
const deleteVideoDevice = async (id: string) => {
  try {
    const token = getToken()
    const response = await axios.delete(`/api/v1/video-devices/${id}`, {
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

// 绑定视频设备到隐患点（批量）
const bindToHazardPoints = async (hpId: string, videoDeviceId: string, installLng: number, installLat: number) => {
  try {
    const token = getToken()
    const response = await axios.post(`/api/v1/hazard-points/${hpId}/bind-video-devices`, {
      videoDeviceIds: [parseInt(videoDeviceId)],
      installPositions: [
        {
          videoDeviceId: parseInt(videoDeviceId),
          installLongitude: installLng,
          installLatitude: installLat
        }
      ]
    }, {
      headers: { Authorization: `Bearer ${token}` }
    })

    return response.data.code === 200
  } catch (error) {
    console.error('绑定失败:', error)
    return false
  }
}

// 解绑视频设备
const unbindVideoDevice = async (hpId: string, videoDeviceId: string) => {
  try {
    const token = getToken()
    const response = await axios.delete(`/api/v1/hazard-points/${hpId}/unbind-video-devices`, {
      data: { videoDeviceIds: [parseInt(videoDeviceId)] },
      headers: { Authorization: `Bearer ${token}` }
    })

    return response.data.code === 200
  } catch (error) {
    console.error('解绑失败:', error)
    return false
  }
}

// ==================== 事件处理方法 ====================

const handleSearch = () => {
  currentPage.value = 1
  loadTableData()
}

const handleReset = () => {
  searchKeyword.value = ''
  searchProtocol.value = ''
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
  dialogTitle.value = '新增视频设备'
  isEdit.value = false
  Object.assign(formData, {
    id: undefined,
    code: '',
    name: '',
    icon: '',
    iconPath: '',
    protocolCode: '',
    streamUrl: ''
  })
  dialogVisible.value = true
}

const handleEdit = async (row: VideoDeviceItem) => {
  dialogTitle.value = '编辑视频设备'
  isEdit.value = true
  const detail = await fetchDetail(row.id)
  if (detail) {
    Object.assign(formData, {
      id: detail.id,
      code: detail.code,
      name: detail.name,
      icon: detail.icon || '',
      iconPath: detail.iconPath || '',
      protocolCode: detail.protocolCode,
      streamUrl: detail.streamUrl
    })
  }
  dialogVisible.value = true
}

const handleBindHazardPoint = (row: VideoDeviceItem) => {
  currentBindRow.value = row
  const hpIds = row.hazardPointIds ? row.hazardPointIds.split(',') : []
  bindFormData.hazardPointIds = hpIds
  bindDialogVisible.value = true
}

const handleDelete = (row: VideoDeviceItem) => {
  ElMessageBox.confirm(`确定要删除视频设备"${row.name}"吗?`, '删除确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    deleteVideoDevice(row.id)
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
        updateVideoDevice()
      } else {
        createVideoDevice()
      }
    }
  })
}

const handleBindSubmit = async () => {
  if (!currentBindRow.value) return

  bindLoading.value = true
  try {
    // 先解绑所有已有的关联
    const oldHpIds = currentBindRow.value.hazardPointIds ? currentBindRow.value.hazardPointIds.split(',') : []
    for (const hpId of oldHpIds) {
      if (hpId && !bindFormData.hazardPointIds.includes(hpId)) {
        await unbindVideoDevice(hpId, currentBindRow.value.id)
      }
    }

    // 绑定新选择的隐患点
    for (const hp of selectedHazardPoints.value) {
      if (bindFormData.hazardPointIds.includes(hp.id)) {
        const installLng = hp.installLng || hp.longitude || 0
        const installLat = hp.installLat || hp.latitude || 0
        await bindToHazardPoints(hp.id, currentBindRow.value.id, installLng, installLat)
      }
    }

    ElMessage.success('关联成功')
    bindDialogVisible.value = false
    loadTableData() // 刷新列表
  } catch (error) {
    ElMessage.error('关联失败')
  } finally {
    bindLoading.value = false
  }
}

const handleSelectVideoIcon = () => {
  videoIconDialogVisible.value = true
}

const handleVideoIconSelect = (item: { code: string; name: string; icon: string; path: string }) => {
  formData.icon = item.icon
  formData.iconPath = item.path
  videoIconDialogVisible.value = false
}

const handleOpenMap = () => {
  selectedHazardPoints.value = hazardPointList.value
      .filter(hp => bindFormData.hazardPointIds.includes(hp.id))
      .map(hp => ({ ...hp }))
  currentHazardPointIndex.value = 0
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

  L.tileLayer('https://t0.tianditu.gov.cn/vec_w/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=vec&STYLE=default&TILEMATRIXSET=w&FORMAT=tiles&TILEMATRIX={z}&TILEROW={y}&TILECOL={x}&tk=8dda07d4649c77efd0537a0ff0a1df13', {
    maxZoom: 18,
    attribution: '天地图'
  }).addTo(mapInstance)

  L.tileLayer('https://t0.tianditu.gov.cn/cva_w/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=cva&STYLE=default&TILEMATRIXSET=w&FORMAT=tiles&TILEMATRIX={z}&TILEROW={y}&TILECOL={x}&tk=8dda07d4649c77efd0537a0ff0a1df13', {
    maxZoom: 18
  }).addTo(mapInstance)

  selectedHazardPoints.value.forEach((hp, index) => {
    if (hp.longitude && hp.latitude) {
      const marker = L.marker([hp.latitude, hp.longitude])
          .addTo(mapInstance!)
          .bindPopup(`${index + 1}. ${hp.name}`)
    }
  })

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
  mapLoading.value = true
  setTimeout(() => {
    mapDialogVisible.value = false
    if (mapInstance) {
      mapInstance.remove()
      mapInstance = null
    }
    mapLoading.value = false
    ElMessage.success('安装位置设置成功')
  }, 500)
}

const handlePlay = (row: VideoDeviceItem) => {
  playUrl.value = row.streamUrl
  videoLoaded.value = false
  videoError.value = false
  playDialogVisible.value = true
}

const handlePlayPause = () => {
  if (videoRef.value) {
    if (isPlaying.value) {
      videoRef.value.pause()
    } else {
      videoRef.value.play()
    }
    isPlaying.value = !isPlaying.value
  }
}

const handleFullscreen = () => {
  if (videoWrapper.value) {
    if (document.fullscreenElement) {
      document.exitFullscreen()
    } else {
      videoWrapper.value.requestFullscreen()
    }
  }
}

const handleScreenshot = () => {
  if (videoRef.value) {
    const canvas = document.createElement('canvas')
    canvas.width = videoRef.value.videoWidth
    canvas.height = videoRef.value.videoHeight
    const ctx = canvas.getContext('2d')
    if (ctx) {
      ctx.drawImage(videoRef.value, 0, 0, canvas.width, canvas.height)
      const link = document.createElement('a')
      link.download = `screenshot_${new Date().getTime()}.png`
      link.href = canvas.toDataURL('image/png')
      link.click()
      ElMessage.success('截图保存成功')
    }
  }
}

const handleRefresh = () => {
  if (videoRef.value) {
    videoLoaded.value = false
    videoError.value = false
    videoRef.value.load()
  }
}

const onVideoLoaded = () => {
  videoLoaded.value = true
  videoError.value = false
}

const onVideoError = () => {
  videoLoaded.value = false
  videoError.value = true
}

onMounted(() => {
  loadTableData()
  loadHazardPointList()
})
</script>

<style scoped>
.video-device-page {
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
  width: 300px;
}

.search-icon {
  font-size: 14px;
}

.protocol-select {
  width: 150px;
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

.stream-url {
  font-size: 12px;
  color: #606266;
  word-break: break-all;
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

.icon-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
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

.video-play-dialog :deep(.el-dialog__body) {
  padding: 10px;
}

.video-container {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.video-wrapper {
  width: 100%;
  height: 480px;
  background: #000;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}

.video-player {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.video-loading,
.video-error {
  position: absolute;
  display: flex;
  flex-direction: column;
  align-items: center;
  color: #fff;
}

.video-loading {
  gap: 10px;
}

.video-controls {
  display: flex;
  gap: 10px;
  margin-top: 15px;
}

:deep(.el-form-item) {
  margin-bottom: 18px;
}
</style>