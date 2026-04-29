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
        <el-table-column prop="hazardPointName" label="关联隐患点" width="150" align="center">
          <template #default="{ row }">
            <span v-if="row.hazardPointName" class="link-text" @click="handleViewHazardPoint(row)">{{ row.hazardPointName }}</span>
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
            <el-form-item label="协议类型" prop="protocolCode">
              <el-select v-model="formData.protocolCode" placeholder="请选择协议类型">
                <el-option label="RTMP" value="RTMP" />
                <el-option label="RTSP" value="RTSP" />
                <el-option label="ONVIF" value="ONVIF" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="设备IP" prop="ipAddress">
              <el-input v-model="formData.ipAddress" placeholder="请输入设备IP地址" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="端口" prop="port">
              <el-input-number v-model="formData.port" :min="1" :max="65535" placeholder="端口号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="安装位置" prop="location">
              <el-input v-model="formData.location" placeholder="请输入安装位置" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="视频流地址" prop="streamUrl">
          <el-input v-model="formData.streamUrl" placeholder="请输入视频流地址" type="textarea" :rows="3" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="登录用户名" prop="username">
              <el-input v-model="formData.username" placeholder="请输入登录用户名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="登录密码" prop="password">
              <el-input v-model="formData.password" placeholder="请输入登录密码" type="password" />
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
      v-model="bindDialogVisible"
      title="关联隐患点"
      width="500px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="bindFormRef" :model="bindFormData" :rules="bindFormRules" label-width="100px">
        <el-form-item label="隐患点" prop="hazardPointId">
          <el-select v-model="bindFormData.hazardPointId" placeholder="请选择隐患点" filterable>
            <el-option
              v-for="hp in hazardPointList"
              :key="hp.id"
              :label="hp.name"
              :value="hp.id"
            />
          </el-select>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="bindDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleBindSubmit">确定</el-button>
      </template>
    </el-dialog>

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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

interface VideoDeviceItem {
  id: string
  code: string
  name: string
  protocolCode: string
  protocolName: string
  streamUrl: string
  hazardPointId?: string
  hazardPointName: string
  ipAddress: string
  port?: number
  username?: string
  password?: string
  status: number
  location: string
  installTime: string
  lastOnlineTime?: string
}

interface HazardPointItem {
  id: string
  code: string
  name: string
}

const loading = ref(false)
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
  hazardPointId: ''
})
const bindFormRules = {
  hazardPointId: [{ required: true, message: '请选择隐患点', trigger: 'blur' }]
}

const playDialogVisible = ref(false)
const playUrl = ref('')
const videoRef = ref<HTMLVideoElement | null>(null)
const videoWrapper = ref<HTMLDivElement | null>(null)
const videoLoaded = ref(false)
const videoError = ref(false)
const isPlaying = ref(false)

const hazardPointList = ref<HazardPointItem[]>([])
const currentBindRow = ref<VideoDeviceItem | null>(null)

const formData = reactive<{
  id?: string
  code: string
  name: string
  protocolCode: string
  streamUrl: string
  ipAddress: string
  port?: number
  username?: string
  password?: string
  location: string
}>({
  code: '',
  name: '',
  protocolCode: '',
  streamUrl: '',
  ipAddress: '',
  location: ''
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

const initTableData = () => {
  loading.value = true
  setTimeout(() => {
    tableData.value = [
      {
        id: '1',
        code: 'VD001',
        name: '隐患点A-摄像头1',
        protocolCode: 'RTSP',
        protocolName: 'RTSP',
        streamUrl: 'rtsp://admin:123456@192.168.1.101:554/Streaming/Channels/101',
        hazardPointId: '1',
        hazardPointName: '隐患点A',
        ipAddress: '192.168.1.101',
        port: 554,
        username: 'admin',
        status: 1,
        location: '隐患点A区域',
        installTime: '2024-01-10 10:00:00',
        lastOnlineTime: '2024-01-20 14:30:00'
      },
      {
        id: '2',
        code: 'VD002',
        name: '隐患点A-摄像头2',
        protocolCode: 'RTMP',
        protocolName: 'RTMP',
        streamUrl: 'rtmp://192.168.1.102:1935/live/stream001',
        hazardPointId: '1',
        hazardPointName: '隐患点A',
        ipAddress: '192.168.1.102',
        port: 1935,
        status: 1,
        location: '隐患点A入口',
        installTime: '2024-01-10 11:00:00',
        lastOnlineTime: '2024-01-20 14:25:00'
      },
      {
        id: '3',
        code: 'VD003',
        name: '隐患点B-摄像头1',
        protocolCode: 'ONVIF',
        protocolName: 'ONVIF',
        streamUrl: 'http://192.168.1.103:8080/onvif/media',
        hazardPointId: '2',
        hazardPointName: '隐患点B',
        ipAddress: '192.168.1.103',
        port: 8080,
        status: 2,
        location: '隐患点B区域',
        installTime: '2024-01-15 09:00:00'
      },
      {
        id: '4',
        code: 'VD004',
        name: '隐患点C-摄像头1',
        protocolCode: 'RTSP',
        protocolName: 'RTSP',
        streamUrl: 'rtsp://admin:password@192.168.1.104:554/stream1',
        hazardPointName: '',
        ipAddress: '192.168.1.104',
        port: 554,
        username: 'admin',
        status: 0,
        location: '隐患点C边坡',
        installTime: '2024-01-20 14:00:00'
      }
    ]
    total.value = tableData.value.length
    loading.value = false
  }, 500)
}

const initHazardPointList = () => {
  hazardPointList.value = [
    { id: '1', code: 'HP001', name: '隐患点A' },
    { id: '2', code: 'HP002', name: '隐患点B' },
    { id: '3', code: 'HP003', name: '隐患点C' },
    { id: '4', code: 'HP004', name: '隐患点D' }
  ]
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
  dialogTitle.value = '新增视频设备'
  isEdit.value = false
  Object.assign(formData, {
    id: undefined,
    code: '',
    name: '',
    protocolCode: '',
    streamUrl: '',
    ipAddress: '',
    port: undefined,
    username: '',
    password: '',
    location: ''
  })
  dialogVisible.value = true
}

const handleEdit = (row: VideoDeviceItem) => {
  dialogTitle.value = '编辑视频设备'
  isEdit.value = true
  Object.assign(formData, {
    id: row.id,
    code: row.code,
    name: row.name,
    protocolCode: row.protocolCode,
    streamUrl: row.streamUrl,
    ipAddress: row.ipAddress,
    port: row.port,
    username: row.username || '',
    password: row.password || '',
    location: row.location
  })
  dialogVisible.value = true
}

const handleBindHazardPoint = (row: VideoDeviceItem) => {
  currentBindRow.value = row
  bindFormData.hazardPointId = row.hazardPointId || ''
  bindDialogVisible.value = true
}

const handleDelete = (row: VideoDeviceItem) => {
  ElMessageBox.confirm(`确定要删除视频设备"${row.name}"吗?`, '删除确认', {
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

const handleBindSubmit = () => {
  bindFormRef.value.validate((valid: boolean) => {
    if (valid && currentBindRow.value) {
      const hp = hazardPointList.value.find(item => item.id === bindFormData.hazardPointId)
      if (hp) {
        currentBindRow.value.hazardPointId = hp.id
        currentBindRow.value.hazardPointName = hp.name
      }
      ElMessage.success('关联成功')
      bindDialogVisible.value = false
    }
  })
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

const handleViewHazardPoint = (row: VideoDeviceItem) => {
  ElMessage.info(`查看隐患点: ${row.hazardPointName}`)
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
  initTableData()
  initHazardPointList()
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

.stream-url {
  font-size: 12px;
  color: #606266;
  word-break: break-all;
}

.link-text {
  color: #409eff;
  cursor: pointer;
}

.link-text:hover {
  text-decoration: underline;
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
