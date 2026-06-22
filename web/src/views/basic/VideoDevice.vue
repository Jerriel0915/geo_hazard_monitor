<template>
  <div class="page">
    <div class="header">
      <div class="header__left">
        <h2 class="header__title">视频设备</h2>
        <span class="header__subtitle">视频监控设备管理与流媒体配置</span>
      </div>
      <div class="header__right">
        <el-button type="primary" @click="handleAdd">+ 新增</el-button>
        <el-button @click="handleExport">导出</el-button>
      </div>
    </div>

    <div class="search">
      <el-input
          v-model="searchKeyword"
          placeholder="搜索编号或名称"
          clearable
          @clear="handleSearch"
          @keyup.enter="handleSearch"
      />
      <el-select v-model="searchProtocol" placeholder="选择协议" clearable>
        <el-option label="RTMP" value="RTMP" />
        <el-option label="RTSP" value="RTSP" />
        <el-option label="ONVIF" value="ONVIF" />
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
          <el-table-column label="图标" width="80" align="center">
            <template #default="{ row }">
              <img v-if="row.iconPath" :src="row.iconPath" class="table-icon" alt="icon" />
              <span v-else class="empty-text">-</span>
            </template>
          </el-table-column>
          <el-table-column prop="code" label="编号" width="130" align="center" />
          <el-table-column prop="name" label="名称" min-width="160" align="center" />

          <el-table-column prop="protocolCode" label="协议类型" width="120" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.protocolCode" :type="getProtocolType(row.protocolCode)" effect="plain">
                {{ row.protocolCode }}
              </el-tag>
              <span v-else class="empty-text">-</span>
            </template>
          </el-table-column>

          <el-table-column prop="streamUrl" label="视频流地址" min-width="280" align="center">
            <template #default="{ row }">
              <span class="stream-url">{{ row.streamUrl || '-' }}</span>
            </template>
          </el-table-column>

          <el-table-column prop="hazardPointIds" label="关联隐患点" min-width="180" align="center">
            <template #default="{ row }">
              <div class="hazard-tags-wrapper">
                <span v-if="row.hazardPointIds && row.hazardPointIds.trim()" class="hazard-tags">
                  <el-tag 
                    v-for="hpId in row.hazardPointIds.split(',')" 
                    :key="hpId" 
                    size="small" 
                    class="hazard-tag"
                  >
                    {{ getHazardPointName(hpId.trim()) }}
                  </el-tag>
                </span>
                <el-tag v-else size="small" class="hazard-tag" type="info">无</el-tag>
              </div>
            </template>
          </el-table-column>

          <el-table-column prop="installTime" label="安装时间" min-width="170" align="center">
            <template #default="{ row }">
              <span v-if="row.installTime">{{ row.installTime }}</span>
              <span v-else class="empty-text">-</span>
            </template>
          </el-table-column>

          <el-table-column label="操作" width="200" fixed="right" align="center">
            <template #default="{ row }">
              <div class="op-cell">
                <el-button type="primary" text size="small" @click="handlePlay(row)">播放</el-button>
                <el-button type="primary" text size="small" @click="handleEdit(row)">编辑</el-button>
                <el-button type="danger" text size="small" @click="handleDelete(row)">删除</el-button>
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
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="关联隐患点">
              <el-select
                  v-model="formData.hazardPointId"
                  filterable
                  clearable
                  placeholder="请选择隐患点"
                  style="width: 100%"
              >
                <el-option
                    v-for="hp in hazardPointList"
                    :key="hp.id"
                    :label="hp.name"
                    :value="String(hp.id)"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="安装位置">
              <div class="install-location-wrap">
                <el-input
                    v-model="locationText"
                    size="small"
                    placeholder="经度,纬度（例如 104.063456, 30.671234）"
                    class="location-input"
                    @blur="onLocationBlur"
                />
                <el-button
                    size="small"
                    class="map-pick-btn"
                    title="在地图上获取坐标"
                    @click="openMapPicker"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                       stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="15" height="15">
                    <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/>
                    <circle cx="12" cy="10" r="3"/>
                  </svg>
                </el-button>
              </div>
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

    <!-- 地图坐标选择弹窗 -->
    <MapLocationPickerDialog
        v-model="mapDialogVisible"
        :initial-point="mapInitialPoint"
        :hazard-point-list="hazardPointList"
        :show-hp-overlay="true"
        :initial-hp-id="formData.hazardPointId"
        @confirm="onMapConfirm"
    />

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
            <span class="custom-spinner"></span>
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import { showRequestErrorMessage } from '@/utils/errorHandler'
import MapLocationPickerDialog from '@/components/map/MapLocationPickerDialog.vue'
import type { LatLng } from '@/lib/boundaryCoords'
import {
  getVideoDevicePage,
  getVideoDeviceDetail,
  createVideoDevice,
  updateVideoDevice,
  deleteVideoDevice,
  exportVideoDevices,
  type VideoDeviceItem,
  type VideoDevicePageParams,
} from '@/api/video'
import { getHazardPointPage } from '@/api/hazardPoint'

// ==================== 类型定义 ====================
interface HazardPointItem {
  id: string
  code?: string
  name: string
  longitude?: number
  latitude?: number
}

// ==================== 视频设备图标列表 ====================
const videoIconList = Array.from({ length: 10 }, (_, i) => {
  const num = i + 1
  return {
    code: `VIDIO${num}`,
    name: `视频图标${num}`,
    icon: `vidio${num}`,
    path: `/jc-icon/green/vidio${num}_green.png`
  }
})

// ==================== 状态 ====================
const loading = ref(false)
const submitLoading = ref(false)
const tableData = ref<VideoDeviceItem[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const searchKeyword = ref('')
const searchProtocol = ref('')
const hazardPointList = ref<HazardPointItem[]>([])

const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const formRef = ref()

const playDialogVisible = ref(false)
const playUrl = ref('')
const videoRef = ref<HTMLVideoElement | null>(null)
const videoWrapper = ref<HTMLDivElement | null>(null)
const videoLoaded = ref(false)
const videoError = ref(false)
const isPlaying = ref(false)

const mapDialogVisible = ref(false)
const locationText = ref('')
const videoIconDialogVisible = ref(false)

// ==================== 表单数据 ====================
const formData = reactive<{
  id?: string
  code: string
  name: string
  icon: string
  iconPath: string
  protocolCode: string
  streamUrl: string
  longitude: number | null
  latitude: number | null
  hazardPointId: string
  originalHazardPointId: string
}>({
  code: '',
  name: '',
  icon: '',
  iconPath: '',
  protocolCode: '',
  streamUrl: '',
  longitude: null,
  latitude: null,
  hazardPointId: '',
  originalHazardPointId: ''
})

const mapInitialPoint = computed<LatLng | null>(() =>
  formData.longitude != null && formData.latitude != null
    ? { lng: formData.longitude, lat: formData.latitude }
    : null
)

// ==================== 表单校验规则 ====================
const formRules = {
  code: [{ required: true, message: '请输入设备编号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入设备名称', trigger: 'blur' }],
  protocolCode: [{ required: true, message: '请选择协议类型', trigger: 'blur' }],
  streamUrl: [{ required: true, message: '请输入视频流地址', trigger: 'blur' }]
}

// ==================== 工具函数 ====================
const getProtocolType = (code: string) => {
  const types: Record<string, string> = {
    'RTMP': 'success',
    'RTSP': 'warning',
    'ONVIF': 'info'
  }
  return types[code] || 'default'
}

const getHazardPointName = (id: string) => {
  const hp = hazardPointList.value.find(item => item.id === id)
  return hp ? hp.name : id
}

// ==================== 隐患点列表 ====================
const loadHazardPointList = async () => {
  try {
    const res = await getHazardPointPage({ pageNum: 1, pageSize: 1000 })
    hazardPointList.value = res.data?.rows || []
  } catch (error) {
    console.error('获取隐患点列表失败:', error)
    hazardPointList.value = [
      { id: '1', name: '龙潭寺滑坡点' },
      { id: '2', name: '大坝监测点' },
      { id: '3', name: '边坡监测点' },
      { id: '4', name: '泥石流隐患点' },
      { id: '15', name: '隐患点15' },
      { id: '16', name: '隐患点16' },
    ]
  }
}

// ==================== 加载数据 ====================
const loadTableData = async () => {
  loading.value = true
  try {
    const params: VideoDevicePageParams = {
      pageNum: currentPage.value,
      pageSize: pageSize.value,
    }
    if (searchKeyword.value) {
      params.code = searchKeyword.value
      params.name = searchKeyword.value
    }
    if (searchProtocol.value) {
      params.protocolCode = searchProtocol.value
    }

    const res = await getVideoDevicePage(params)
    if (res.code === 200) {
      tableData.value = res.data?.rows || []
      total.value = res.data?.total || 0
    } else {
      ElMessage.error(res.msg || '获取数据失败')
    }
  } catch (error) {
    console.error('请求失败:', error)
    showRequestErrorMessage(error, '网络请求失败')
  } finally {
    loading.value = false
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
    streamUrl: '',
    longitude: null,
    latitude: null,
    hazardPointId: '',
    originalHazardPointId: ''
  })
  locationText.value = ''
  dialogVisible.value = true
}

const handleEdit = async (row: VideoDeviceItem) => {
  dialogTitle.value = '编辑视频设备'
  isEdit.value = true
  try {
    const res = await getVideoDeviceDetail(row.id)
    if (res.code === 200 && res.data) {
      const detail = res.data
      const initialHpId = detail.hazardPointIds ? detail.hazardPointIds.split(',').filter(Boolean)[0] || '' : ''
      Object.assign(formData, {
        id: detail.id,
        code: detail.code,
        name: detail.name,
        icon: detail.icon || '',
        iconPath: detail.iconPath || '',
        protocolCode: detail.protocolCode,
        streamUrl: detail.streamUrl,
        longitude: detail.longitude ?? null,
        latitude: detail.latitude ?? null,
        hazardPointId: initialHpId,
        originalHazardPointId: initialHpId
      })
      syncFormToText()
    }
    dialogVisible.value = true
  } catch (error) {
    showRequestErrorMessage(error, '获取详情失败')
  }
}

const handleDelete = (row: VideoDeviceItem) => {
  ElMessageBox.confirm(`确定要删除视频设备"${row.name}"吗?`, '删除确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const res = await deleteVideoDevice(row.id)
      if (res.code === 200) {
        ElMessage.success('删除成功')
        loadTableData()
      } else {
        ElMessage.error(res.msg || '删除失败')
      }
    } catch (error) {
      showRequestErrorMessage(error, '删除失败')
    }
  }).catch(() => {})
}

const handleExport = async () => {
  try {
    const response = await exportVideoDevices()
    const blob = response.data
    const disposition = String(response.headers['content-disposition'] || '')
    const fileName = disposition
      ? decodeURIComponent(disposition.split("filename*=UTF-8''")[1] || disposition.split('filename=')[1]?.replace(/"/g, '') || '视频设备数据.xlsx')
      : '视频设备数据.xlsx'
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = fileName
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (error: any) {
    ElMessage.error('导出失败: ' + (error?.message || '未知错误'))
  }
}

// ==================== 表单操作 ====================
const syncFormToText = () => {
  if (formData.longitude != null && formData.latitude != null) {
    locationText.value = `${Number(formData.longitude).toFixed(6)}, ${Number(formData.latitude).toFixed(6)}`
  } else {
    locationText.value = ''
  }
}

const onLocationBlur = () => {
  const raw = locationText.value.trim()
  if (!raw) {
    formData.longitude = null
    formData.latitude = null
    return
  }
  const parts = raw.split(/[,，\s]+/)
  if (parts.length >= 2) {
    const lng = Number(parts[0])
    const lat = Number(parts[1])
    if (!isNaN(lng) && !isNaN(lat) && lng >= -180 && lng <= 180 && lat >= -90 && lat <= 90) {
      formData.longitude = lng
      formData.latitude = lat
      locationText.value = `${lng.toFixed(6)}, ${lat.toFixed(6)}`
      return
    }
  }
  ElMessage.warning('坐标格式无效，请输入"经度,纬度"（例如 104.063456, 30.671234）')
}

const openMapPicker = () => {
  onLocationBlur()
  mapDialogVisible.value = true
}

const onMapConfirm = (point: LatLng) => {
  formData.longitude = point.lng
  formData.latitude = point.lat
  syncFormToText()
}

const handleSubmit = async () => {
  onLocationBlur()
  formRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    submitLoading.value = true
    try {
      if (formData.id) {
        const res = await updateVideoDevice(formData.id, {
          name: formData.name,
          icon: formData.icon,
          iconPath: formData.iconPath,
          protocolCode: formData.protocolCode,
          streamUrl: formData.streamUrl,
          longitude: formData.longitude,
          latitude: formData.latitude,
        })
        if (res.code === 200) {
          ElMessage.success('修改成功')
          dialogVisible.value = false
          loadTableData()
        } else {
          ElMessage.error(res.msg || '修改失败')
        }
      } else {
        const res = await createVideoDevice({
          code: formData.code,
          name: formData.name,
          icon: formData.icon,
          iconPath: formData.iconPath,
          protocolCode: formData.protocolCode,
          streamUrl: formData.streamUrl,
          longitude: formData.longitude,
          latitude: formData.latitude,
          status: 1
        })
        if (res.code === 200) {
          ElMessage.success('新增成功')
          dialogVisible.value = false
          loadTableData()
        } else {
          ElMessage.error(res.msg || '新增失败')
        }
      }
    } catch (error: any) {
      console.error('保存失败:', error)
      showRequestErrorMessage(error, '保存失败')
    } finally {
      submitLoading.value = false
    }
  })
}

const handleSelectVideoIcon = () => {
  videoIconDialogVisible.value = true
}

const handleVideoIconSelect = (item: { code: string; name: string; icon: string; path: string }) => {
  formData.icon = item.icon
  formData.iconPath = item.path
  videoIconDialogVisible.value = false
}

// ==================== 视频播放 ====================
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
  if (!videoRef.value) {
    ElMessage.warning('视频未加载')
    return
  }

  const video = videoRef.value

  if (video.videoWidth === 0 || video.videoHeight === 0) {
    ElMessage.warning('视频未开始播放或尺寸无效，请先播放视频')
    return
  }

  try {
    const canvas = document.createElement('canvas')
    canvas.width = video.videoWidth
    canvas.height = video.videoHeight

    const ctx = canvas.getContext('2d')
    if (!ctx) {
      ElMessage.error('无法创建截图')
      return
    }

    ctx.drawImage(video, 0, 0, canvas.width, canvas.height)

    try {
      const dataUrl = canvas.toDataURL('image/png')
      const link = document.createElement('a')
      link.download = `screenshot_${new Date().getTime()}.png`
      link.href = dataUrl
      link.click()
      ElMessage.success('截图已保存')
    } catch (securityError) {
      ElMessage.error('由于浏览器安全限制，无法截取跨域视频，请使用浏览器截图工具')
      console.error('跨域截图失败:', securityError)
    }
  } catch (error) {
    console.error('截图失败:', error)
    ElMessage.error('截图失败')
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

// ==================== 生命周期 ====================
onMounted(() => {
  loadTableData()
  loadHazardPointList()
})
</script>

<style scoped>
.table-icon {
  width: 28px;
  height: 28px;
  object-fit: contain;
}

.empty-text {
  color: #909399;
}

.hazard-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.hazard-tag {
  margin: 2px;
}

.hazard-tags-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
}

.stream-url {
  font-size: 12px;
  color: #606266;
  word-break: break-all;
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

.install-location-wrap {
  display: flex;
  align-items: center;
  gap: 6px;
  width: 100%;
}

.install-location-wrap :deep(.el-input) {
  width: 100%;
}

.map-pick-btn {
  flex-shrink: 0;
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

.custom-spinner {
  width: 24px;
  height: 24px;
  border: 3px solid rgba(255, 255, 255, 0.3);
  border-top-color: #409eff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
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