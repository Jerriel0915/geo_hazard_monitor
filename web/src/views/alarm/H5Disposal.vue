<template>
  <div class="h5-disposal-page">
    <!-- 顶部导航 -->
    <div class="page-header">
      <div class="header-back" @click="handleBack">
        <el-icon><ArrowLeft /></el-icon>
      </div>
      <div class="header-title">告警处置</div>
      <div class="header-right"></div>
    </div>

    <!-- 内容区域 -->
    <div class="page-content" v-if="alarmData">
      <!-- 告警信息卡片 -->
      <div class="info-card alarm-info">
        <div class="card-header compact">
          <div class="card-icon alarm">
            <el-icon><WarnTriangleFilled /></el-icon>
          </div>
          <span class="card-title">告警信息</span>
        </div>
        <div class="card-body">
          <div class="info-row">
            <div class="info-item">
              <span class="info-label">告警等级</span>
              <span class="info-value level-badge" :class="getLevelClass(alarmData.alarmLevel)">
                {{ alarmLevelRange }}
              </span>
            </div>
            <div class="info-item">
              <span class="info-label">告警类型</span>
              <span class="info-value">{{ getAlarmTypeText(alarmData.alarmType) }}</span>
            </div>
          </div>
          <div class="info-row">
            <div class="info-item full">
              <span class="info-label">告警时间</span>
              <span class="info-value">{{ alarmData.firstAlarmTime }} ~ {{ alarmData.lastAlarmTime }}</span>
            </div>
          </div>
          <div class="info-row">
            <div class="info-item full">
              <span class="info-label">警情来源</span>
              <div class="source-wrapper">
                <span class="info-value source">
                  <el-icon><MapLocation /></el-icon>
                  {{ alarmData.hazardPointName }}
                  <el-icon><Monitor /></el-icon>
                  {{ alarmData.deviceName || '未知设备' }}
                </span>
                <div class="map-icon-btn" @click="handleOpenMap">
                  <el-icon><MapLocation /></el-icon>
                </div>
              </div>
            </div>
          </div>
          <div class="info-row">
            <div class="info-item full">
              <span class="info-label">告警描述</span>
              <p class="info-desc">{{ alarmData.alarmContent }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 项目信息卡片 -->
      <div class="info-card project-info">
        <div class="card-header compact">
          <div class="card-icon project">
            <el-icon><Location /></el-icon>
          </div>
          <span class="card-title">项目信息</span>
        </div>
        <div class="card-body">
          <div class="info-row">
            <div class="info-item full">
              <span class="info-label">地理位置</span>
              <span class="info-value">{{ alarmData.location || '暂无位置信息' }}</span>
            </div>
          </div>
          <div class="info-row">
            <div class="info-item full">
              <span class="info-label">所属分组</span>
              <span class="info-value">{{ alarmData.groupName || '未分组' }}</span>
            </div>
          </div>
          <div class="info-row">
            <div class="info-item full">
              <span class="info-label">隐患点描述</span>
              <p class="info-desc">{{ alarmData.hazardPointDesc || '暂无描述' }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 支撑数据卡片 -->
      <div class="info-card support-data">
        <!-- Tab切换 -->
        <div class="data-tabs">
          <div 
            class="tab-item" 
            :class="{ active: activeTab === 'monitor' }" 
            @click="switchTab('monitor')"
          >
            <el-icon><TrendCharts /></el-icon>
            监测数据
          </div>
          <div 
            class="tab-item" 
            :class="{ active: activeTab === 'alarm' }" 
            @click="switchTab('alarm')"
          >
            <el-icon><Bell /></el-icon>
            告警次数
            <span class="tab-badge">{{ alarmData.alarmCount || 0 }}</span>
          </div>
          <div 
            class="tab-item" 
            :class="{ active: activeTab === 'notify' }" 
            @click="switchTab('notify')"
          >
            <el-icon><ChatDotRound /></el-icon>
            通知记录
          </div>
        </div>

        <div class="card-body">
          <!-- 监测数据 -->
          <div v-show="activeTab === 'monitor'" class="tab-content">
            <div class="monitor-sub-tabs">
              <span 
                class="sub-tab" 
                :class="{ active: activeDataTab === 'monitor' }" 
                @click="switchDataTab('monitor')"
              >监测曲线</span>
              <span 
                class="sub-tab" 
                :class="{ active: activeDataTab === 'deduce' }" 
                @click="switchDataTab('deduce')"
              >推演曲线</span>
            </div>
            <div ref="chartRef" class="chart-container"></div>
          </div>

          <!-- 告警次数 -->
          <div v-show="activeTab === 'alarm'" class="tab-content">
            <div class="search-bar">
              <el-input 
                v-model="alarmFilter.desc" 
                placeholder="描述模糊查询" 
                clearable 
                class="search-input"
              />
              <el-date-picker 
                v-model="alarmFilter.timeRange" 
                type="daterange" 
                range-separator="至"
                start-placeholder="开始" 
                end-placeholder="结束" 
                value-format="YYYY-MM-DD" 
                class="search-date"
              />
            </div>
            <div class="table-wrapper">
              <div class="mobile-table">
                <div 
                  class="table-row" 
                  v-for="(item, index) in paginatedAlarmList" 
                  :key="index"
                >
                  <div class="row-header">
                    <span class="row-time">{{ item.alarmTime }}</span>
                    <el-tag :type="getAlarmLevelType(item.alarmLevel)" size="small">
                      {{ getAlarmLevelText(item.alarmLevel) }}
                    </el-tag>
                  </div>
                  <div class="row-content">{{ item.alarmContent }}</div>
                </div>
              </div>
            </div>
            <div class="pagination-wrapper" v-if="filteredAlarmList.length > alarmPageSize">
              <el-pagination
                v-model:current-page="alarmCurrentPage"
                :page-size="alarmPageSize"
                :total="filteredAlarmList.length"
                layout="prev, pager, next"
                small
              />
            </div>
          </div>

          <!-- 通知记录 -->
          <div v-show="activeTab === 'notify'" class="tab-content">
            <div class="search-bar">
              <el-input 
                v-model="notifyFilter.account" 
                placeholder="账号模糊查询" 
                clearable 
                class="search-input"
              />
              <el-date-picker 
                v-model="notifyFilter.timeRange" 
                type="daterange" 
                range-separator="至"
                start-placeholder="开始" 
                end-placeholder="结束" 
                value-format="YYYY-MM-DD" 
                class="search-date"
              />
            </div>
            <div class="table-wrapper">
              <div class="mobile-table">
                <div 
                  class="table-row notify-row" 
                  v-for="(item, index) in paginatedNotifyList" 
                  :key="index"
                >
                  <div class="row-header">
                    <span class="row-time">{{ item.notifyTime }}</span>
                    <el-tag :type="item.success ? 'success' : 'danger'" size="small">
                      {{ item.success ? '成功' : '失败' }}
                    </el-tag>
                  </div>
                  <div class="row-info">
                    <span class="channel-tag">{{ item.channelType }}</span>
                    <span class="target-text">{{ item.target }}</span>
                  </div>
                  <div class="row-content">{{ item.content }}</div>
                </div>
              </div>
            </div>
            <div class="pagination-wrapper" v-if="filteredNotifyList.length > notifyPageSize">
              <el-pagination
                v-model:current-page="notifyCurrentPage"
                :page-size="notifyPageSize"
                :total="filteredNotifyList.length"
                layout="prev, pager, next"
                small
              />
            </div>
          </div>
        </div>
      </div>

      <!-- 时间线 -->
      <div class="info-card timeline-card">
        <div class="card-header">
          <div class="card-icon timeline">
            <el-icon><Clock /></el-icon>
          </div>
          <span class="card-title">时间线</span>
        </div>
        <div class="card-body">
          <div class="timeline" v-if="timelineData.length > 0">
            <div class="timeline-item" v-for="(item, index) in timelineData" :key="index">
              <div class="timeline-dot" :class="item.type"></div>
              <div class="timeline-line" v-if="index < timelineData.length - 1"></div>
              <div class="timeline-content">
                <div class="timeline-time">{{ item.time }}</div>
                <div class="timeline-desc">{{ item.description }}</div>
              </div>
            </div>
          </div>
          <div class="timeline-empty" v-else>
            <el-icon><Clock /></el-icon>
            <p>暂无处置记录</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 地图弹窗 -->
    <el-dialog
      v-model="mapDialogVisible"
      title="位置信息"
      width="90%"
      :close-on-click-modal="false"
      class="h5-map-dialog"
    >
      <div class="map-container">
        <div class="map-placeholder">
          <el-icon><MapLocation /></el-icon>
          <div class="map-location">{{ alarmData?.location || '暂无位置信息' }}</div>
          <div class="map-coords">
            <span>经度: {{ alarmData?.longitude || '104.06' }}°</span>
            <span>纬度: {{ alarmData?.latitude || '30.57' }}°</span>
          </div>
          <div class="map-marker">
            <div class="marker-pin"></div>
            <div class="marker-label">{{ alarmData?.hazardPointName }}</div>
          </div>
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" size="small" @click="handleNavigate">导航</el-button>
          <el-button size="small" @click="mapDialogVisible = false">关闭</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 反馈弹窗 -->
    <el-dialog
      v-model="feedbackDialogVisible"
      title="反馈"
      width="90%"
      :close-on-click-modal="false"
      class="h5-feedback-dialog"
    >
      <div class="feedback-form">
        <div class="form-item">
          <div class="form-label">反馈内容</div>
          <el-input
            v-model="feedbackForm.content"
            type="textarea"
            :rows="3"
            placeholder="请输入反馈内容（非必填）..."
            resize="none"
          />
        </div>
        <div class="form-item">
          <div class="form-label">反馈文件</div>
          <div class="upload-area">
            <div 
              v-for="(file, index) in feedbackForm.files" 
              :key="index" 
              class="uploaded-file"
            >
              <div class="file-icon" :class="getFileClass(file)">
                <el-icon>{{ getFileIcon(file) }}</el-icon>
              </div>
              <span class="file-name">{{ file.name }}</span>
              <div class="file-remove" @click="removeFile(index)">
                <el-icon><CircleClose /></el-icon>
              </div>
            </div>
            <div 
              v-if="feedbackForm.files.length < maxFiles" 
              class="upload-btn"
              @click="triggerFileInput"
            >
              <el-icon><Plus /></el-icon>
              <span>添加文件</span>
            </div>
            <input 
              type="file" 
              multiple 
              accept="image/*,.pdf,.doc,.docx,.xls,.xlsx"
              class="file-input"
              ref="fileInputRef"
              @change="handleFileSelect"
            />
          </div>
          <span class="form-hint">支持图片、PDF、Word、Excel格式，最多上传{{ maxFiles }}个文件</span>
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button size="small" @click="feedbackDialogVisible = false">取消</el-button>
          <el-button type="primary" size="small" @click="handleSubmitFeedback">提交</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 底部操作栏 -->
    <div class="page-footer">
      <div class="action-buttons">
        <el-button type="primary" size="small" @click="handleFeedback">
          <el-icon><ChatDotRound /></el-icon> 反馈
        </el-button>
        <el-button type="warning" size="small" @click="handleFalseAlarm">
          <el-icon><Warning /></el-icon> 误报
        </el-button>
        <el-button type="danger" size="small" @click="handleCloseAlarm">
          <el-icon><CircleClose /></el-icon> 消警
        </el-button>
        <el-button type="info" size="small" @click="handleNotify">
          <el-icon><Bell /></el-icon> 通知
        </el-button>
      </div>
    </div>

    <!-- 通知弹窗 -->
    <el-dialog
      v-model="notifyDialogVisible"
      title="发送通知"
      width="90%"
      :close-on-click-modal="false"
      class="h5-notify-dialog"
    >
      <div class="notify-form">
        <div class="form-item">
          <div class="form-label">消息内容</div>
          <el-input
            v-model="notifyForm.content"
            type="textarea"
            :rows="3"
            placeholder="请输入通知消息内容..."
            resize="none"
          />
        </div>
        <div class="form-item">
          <div class="form-label">通知渠道</div>
          <el-checkbox-group v-model="notifyForm.channels" class="channel-group">
            <el-checkbox label="sms">短信</el-checkbox>
            <el-checkbox label="email">邮件</el-checkbox>
            <el-checkbox label="system">系统消息</el-checkbox>
          </el-checkbox-group>
        </div>
        <div class="form-item">
          <div class="form-label">通知人员</div>
          <el-select
            v-model="notifyForm.userIds"
            multiple
            placeholder="请选择通知人员"
            collapse-tags
            collapse-tags-tooltip
            class="user-select"
          >
            <el-option
              v-for="user in userList"
              :key="user.id"
              :label="user.name"
              :value="user.id"
            >
              <div class="user-option">
                <span class="user-name">{{ user.name }}</span>
                <span class="user-role">{{ user.role }}</span>
              </div>
            </el-option>
          </el-select>
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button size="small" @click="notifyDialogVisible = false">取消</el-button>
          <el-button type="primary" size="small" @click="handleSendNotify">发送</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { 
  ArrowLeft, Bell, ChatDotRound, CircleClose, Clock, MapLocation, Plus,
  Picture, Document, Files,
  Location, Monitor, TrendCharts, Warning, WarnTriangleFilled 
} from '@element-plus/icons-vue'
import * as echarts from 'echarts'

const route = useRoute()
const router = useRouter()

// 告警数据
const alarmData = ref<any>(null)
const activeTab = ref('monitor')
const activeDataTab = ref('monitor')
const chartRef = ref<HTMLDivElement | null>(null)
let chartInstance: echarts.ECharts | null = null

// 告警次数筛选
const alarmFilter = reactive({ desc: '', timeRange: [] as string[] })
const alarmCurrentPage = ref(1)
const alarmPageSize = 5

// 通知记录筛选
const notifyFilter = reactive({ account: '', timeRange: [] as string[] })
const notifyCurrentPage = ref(1)
const notifyPageSize = 5

// 通知弹窗
const notifyDialogVisible = ref(false)
const notifyForm = reactive({
  content: '',
  channels: ['system'] as string[],
  userIds: [] as string[]
})

// 地图弹窗
const mapDialogVisible = ref(false)

// 反馈弹窗
const feedbackDialogVisible = ref(false)
const feedbackForm = reactive({
  content: '',
  files: [] as any[]
})

const maxFiles = 3
const fileInputRef = ref<HTMLInputElement | null>(null)

// 用户列表
const userList = ref([
  { id: '1', name: '张三', role: '管理员', phone: '138****1234' },
  { id: '2', name: '李四', role: '运维人员', phone: '139****5678' },
  { id: '3', name: '王五', role: '安全员', phone: '137****9012' },
  { id: '4', name: '赵六', role: '值班员', phone: '136****3456' },
  { id: '5', name: '钱七', role: '技术员', phone: '135****7890' },
  { id: '6', name: '孙八', role: '监测员', phone: '134****2345' }
])

// Mock告警数据
const mockAlarmData = {
  id: '1',
  hazardPointName: '边坡监测点A-01',
  deviceName: '位移传感器-D001',
  alarmLevel: '1',
  minAlarmLevel: '1',
  maxAlarmLevel: '2',
  firstAlarmTime: '2024-06-01 08:30:00',
  lastAlarmTime: '2024-06-03 14:25:00',
  alarmCount: 15,
  alarmType: 'threshold',
  alarmContent: '边坡位移速率超过阈值12mm/h，当前值为15.2mm/h，请及时处理。监测数据显示该区域位移持续增大，建议立即采取加固措施。',
  groupName: '地质灾害监测组',
  location: '四川省成都市龙泉驿区某边坡',
  hazardPointDesc: '该隐患点位于山区道路旁，边坡高度约30米，坡度约45度，存在滑坡风险。已安装位移传感器、雨量计等监测设备。'
}

// 告警列表mock
const alarmList = ref<any[]>([
  { alarmTime: '2024-06-03 14:25:00', alarmLevel: '1', alarmContent: '边坡位移速率超过阈值12mm/h，当前值为15.2mm/h' },
  { alarmTime: '2024-06-02 18:20:00', alarmLevel: '1', alarmContent: '边坡位移加速，当前值14.2mm/h' },
  { alarmTime: '2024-06-02 09:00:00', alarmLevel: '2', alarmContent: '降雨量超过预警值，当前35mm/h' },
  { alarmTime: '2024-06-01 12:45:00', alarmLevel: '1', alarmContent: '边坡位移速率超过阈值12mm/h，当前值为13.8mm/h' },
  { alarmTime: '2024-06-01 08:30:00', alarmLevel: '1', alarmContent: '边坡位移速率超过阈值12mm/h，当前值为12.5mm/h' },
  { alarmTime: '2024-05-30 16:00:00', alarmLevel: '3', alarmContent: '轻微位移预警，当前值8.5mm/h' },
  { alarmTime: '2024-05-29 10:00:00', alarmLevel: '2', alarmContent: '降雨量达到预警阈值，当前28mm/h' },
])

// 通知记录mock
const notifyList = ref<any[]>([
  { notifyTime: '2024-06-03 14:30:00', channelType: '短信', target: '138****1234', content: '边坡监测点A-01持续告警，请关注', success: true },
  { notifyTime: '2024-06-02 09:05:00', channelType: '邮件', target: 'admin@abc.com', content: '边坡监测点A-01告警升级', success: true },
  { notifyTime: '2024-06-01 08:35:00', channelType: '电话', target: '138****1234', content: '边坡监测点A-01告警未响应，请及时处理', success: false },
  { notifyTime: '2024-06-01 08:32:00', channelType: '邮件', target: 'zhangsan@abc.com', content: '边坡监测点A-01发生一级告警', success: true },
  { notifyTime: '2024-06-01 08:32:00', channelType: '短信', target: '138****1234', content: '边坡监测点A-01发生一级告警', success: true },
])

// 时间线数据
const timelineData = ref([
  { time: '2024-06-03 14:25:00', description: '告警触发', type: 'trigger' },
  { time: '2024-06-03 14:26:00', description: '系统自动响应', type: 'system' },
  { time: '2024-06-03 14:30:00', description: '值班人员确认', type: 'confirm' }
])

// 计算属性
const alarmLevelRange = computed(() => {
  if (!alarmData.value) return '-'
  const minLevel = alarmData.value.minAlarmLevel ?? alarmData.value.alarmLevel
  const maxLevel = alarmData.value.maxAlarmLevel ?? alarmData.value.alarmLevel
  const minText = getAlarmLevelText(minLevel)
  const maxText = getAlarmLevelText(maxLevel)
  return minText === maxText ? minText : `${minText}-${maxText}`
})

const filteredAlarmList = computed(() => {
  let list = [...alarmList.value]
  if (alarmFilter.desc) {
    const kw = alarmFilter.desc.toLowerCase()
    list = list.filter(a => a.alarmContent.toLowerCase().includes(kw))
  }
  if (alarmFilter.timeRange?.length === 2) {
    const [s, e] = alarmFilter.timeRange
    list = list.filter(a => a.alarmTime >= s && a.alarmTime <= e + ' 23:59:59')
  }
  return list
})

const paginatedAlarmList = computed(() => {
  const start = (alarmCurrentPage.value - 1) * alarmPageSize
  return filteredAlarmList.value.slice(start, start + alarmPageSize)
})

const filteredNotifyList = computed(() => {
  let list = [...notifyList.value]
  if (notifyFilter.account) {
    const kw = notifyFilter.account.toLowerCase()
    list = list.filter(n => n.target.toLowerCase().includes(kw))
  }
  if (notifyFilter.timeRange?.length === 2) {
    const [s, e] = notifyFilter.timeRange
    list = list.filter(n => n.notifyTime >= s && n.notifyTime <= e + ' 23:59:59')
  }
  return list
})

const paginatedNotifyList = computed(() => {
  const start = (notifyCurrentPage.value - 1) * notifyPageSize
  return filteredNotifyList.value.slice(start, start + notifyPageSize)
})

// 方法
const switchTab = (tab: string) => {
  activeTab.value = tab
  if (tab === 'monitor') {
    nextTick(() => initChart())
  }
}

const switchDataTab = (tab: string) => {
  activeDataTab.value = tab
  nextTick(() => updateChart())
}

const handleBack = () => {
  router.back()
}

// 图表相关
const generateChartData = () => {
  const alarmTime = alarmData.value?.firstAlarmTime || new Date().toISOString()
  const alarmDate = new Date(alarmTime)
  const monitorData: { time: string; value: number; isAlarm: boolean }[] = []
  const deduceData: { time: string; value: number; isAlarm: boolean }[] = []

  for (let i = -3; i <= 3; i++) {
    const date = new Date(alarmDate)
    date.setDate(date.getDate() + i)
    for (let hour = 0; hour < 24; hour += 6) {
      date.setHours(hour, 0, 0, 0)
      const timeStr = `${date.getMonth() + 1}/${date.getDate()} ${hour}:00`
      const baseValue = 50 + Math.random() * 30
      const isAlarm = i === 0 && hour >= 8 && hour <= 16 && Math.random() > 0.7
      const value = isAlarm ? baseValue + 20 + Math.random() * 20 : baseValue
      monitorData.push({ time: timeStr, value: Math.round(value * 10) / 10, isAlarm })
      const deduceBase = baseValue + (i > 0 ? i * 3 : 0)
      const deduceIsAlarm = i === 0 && hour >= 8 && hour <= 16 && Math.random() > 0.65
      const deduceValue = deduceIsAlarm ? deduceBase + 18 + Math.random() * 18 : deduceBase + Math.random() * 10
      deduceData.push({ time: timeStr, value: Math.round(deduceValue * 10) / 10, isAlarm: deduceIsAlarm })
    }
  }
  return { monitorData, deduceData }
}

const initChart = () => {
  if (!chartRef.value) return
  if (chartInstance) chartInstance.dispose()
  chartInstance = echarts.init(chartRef.value)
  updateChart()
}

const updateChart = () => {
  if (!chartInstance) return
  const { monitorData, deduceData } = generateChartData()
  const chartData = activeDataTab.value === 'monitor' ? monitorData : deduceData
  const alarmPoints = chartData.filter(item => item.isAlarm)
  const alarmIndices = alarmPoints.map(p => chartData.indexOf(p))

  chartInstance.setOption({
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        const data = params[0]
        const isAlarm = alarmIndices.includes(data.dataIndex)
        return `<div style="padding:8px">
          <div style="font-weight:bold;margin-bottom:4px">${data.name}</div>
          <div>数值: <span style="color:${isAlarm ? '#f56c6c' : '#409eff'}">${data.value}</span></div>
          ${isAlarm ? '<div style="color:#f56c6c;margin-top:4px">⚠️ 告警数据点</div>' : ''}
        </div>`
      }
    },
    grid: { left: '12%', right: '5%', bottom: '15%', top: '10%' },
    xAxis: {
      type: 'category',
      data: chartData.map(item => item.time),
      axisLabel: { rotate: 45, fontSize: 10, color: '#666' },
      axisLine: { lineStyle: { color: '#ddd' } }
    },
    yAxis: {
      type: 'value',
      name: '监测值',
      nameTextStyle: { fontSize: 10 },
      axisLabel: { fontSize: 10, color: '#666' },
      axisLine: { lineStyle: { color: '#ddd' } },
      splitLine: { lineStyle: { color: '#f0f0f0' } }
    },
    series: [{
      name: activeDataTab.value === 'monitor' ? '监测数据' : '推演数据',
      type: 'line',
      data: chartData.map(item => item.value),
      smooth: true,
      symbol: 'circle',
      symbolSize: (_v: number, params: any) => alarmIndices.includes(params.dataIndex) ? 10 : 6,
      itemStyle: { color: (params: any) => alarmIndices.includes(params.dataIndex) ? '#f56c6c' : '#409eff' },
      lineStyle: { width: 2, color: '#409eff' },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(64,158,255,.3)' },
          { offset: 1, color: 'rgba(64,158,255,.05)' }
        ])
      },
      markPoint: {
        data: alarmPoints.map((point, i) => ({
          name: `告警点${i + 1}`,
          coord: [chartData.indexOf(point), point.value],
          value: point.value,
          itemStyle: { color: '#f56c6c' },
          symbol: 'pin',
          symbolSize: 30,
          label: { show: true, formatter: '⚠', fontSize: 10 }
        }))
      }
    }]
  } as echarts.EChartsOption, true)
}

const handleResize = () => { chartInstance?.resize() }

// 工具函数
const getLevelClass = (level: string) => {
  const map: Record<string, string> = {
    '1': 'level-1',
    '2': 'level-2',
    '3': 'level-3',
    '4': 'level-4'
  }
  return map[level] || 'level-4'
}

const getAlarmLevelType = (level: string) => {
  const map: Record<string, string> = {
    '1': 'danger',
    '2': 'warning',
    '3': 'success',
    '4': 'info'
  }
  return map[level] || 'info'
}

const getAlarmLevelText = (level: string) => {
  const map: Record<string, string> = {
    '1': '一级',
    '2': '二级',
    '3': '三级',
    '4': '四级'
  }
  return map[level] || level
}

const getAlarmTypeText = (type: string) => {
  const map: Record<string, string> = {
    'threshold': '阈值预警',
    'comprehensive': '综合预警'
  }
  return map[type] || type
}

// 操作方法
const handleFeedback = () => {
  // 打开反馈弹窗
  feedbackForm.content = ''
  feedbackForm.files = []
  feedbackDialogVisible.value = true
}

const triggerFileInput = () => {
  fileInputRef.value?.click()
}

const handleFileSelect = (event: Event) => {
  const target = event.target as HTMLInputElement
  const files = target.files
  if (!files) return
  
  const remaining = maxFiles - feedbackForm.files.length
  for (let i = 0; i < Math.min(files.length, remaining); i++) {
    feedbackForm.files.push(files[i])
  }
  
  // 清空input，以便可以再次选择相同文件
  target.value = ''
}

const getFileClass = (file: File) => {
  const ext = file.name.split('.').pop()?.toLowerCase()
  if (['jpg', 'jpeg', 'png', 'gif', 'webp'].includes(ext || '')) return 'image'
  if (['pdf'].includes(ext || '')) return 'pdf'
  if (['doc', 'docx'].includes(ext || '')) return 'word'
  if (['xls', 'xlsx'].includes(ext || '')) return 'excel'
  return 'other'
}

const getFileIcon = (file: File) => {
  const ext = file.name.split('.').pop()?.toLowerCase()
  if (['jpg', 'jpeg', 'png', 'gif', 'webp'].includes(ext || '')) return Picture
  if (['pdf'].includes(ext || '')) return Document
  return Files
}

const removeFile = (index: number) => {
  feedbackForm.files.splice(index, 1)
}

const handleSubmitFeedback = () => {
  console.log('提交反馈:', {
    content: feedbackForm.content,
    files: feedbackForm.files.map(f => f.name)
  })
  ElMessage.success('反馈提交成功')
  feedbackDialogVisible.value = false
}

const handleFalseAlarm = () => {
  ElMessage.success('已标记为误报')
}

const handleCloseAlarm = () => {
  ElMessage.success('消警成功')
}

const handleNotify = () => {
  // 初始化消息内容为告警消息
  notifyForm.content = alarmData.value?.alarmContent || ''
  notifyForm.channels = ['system']
  notifyForm.userIds = []
  notifyDialogVisible.value = true
}

const handleOpenMap = () => {
  mapDialogVisible.value = true
}

const handleNavigate = () => {
  const lat = alarmData.value?.latitude || '30.57'
  const lng = alarmData.value?.longitude || '104.06'
  const name = alarmData.value?.hazardPointName || '目的地'
  ElMessage.info(`正在打开导航到 ${name}`)
  // 实际项目中可以调用地图APP进行导航
  mapDialogVisible.value = false
}

const handleSendNotify = () => {
  if (!notifyForm.content.trim()) {
    ElMessage.warning('请输入通知消息内容')
    return
  }
  if (notifyForm.channels.length === 0) {
    ElMessage.warning('请选择至少一个通知渠道')
    return
  }
  if (notifyForm.userIds.length === 0) {
    ElMessage.warning('请选择至少一个通知人员')
    return
  }
  
  // 获取渠道标签
  const channelLabels = notifyForm.channels.map(c => {
    const map: Record<string, string> = { sms: '短信', email: '邮件', system: '系统消息' }
    return map[c]
  }).join('、')
  
  // 获取选中人员姓名
  const selectedUsers = userList.value
    .filter(u => notifyForm.userIds.includes(u.id))
    .map(u => u.name)
    .join('、')
  
  console.log('发送通知:', {
    content: notifyForm.content,
    channels: notifyForm.channels,
    channelLabels,
    userIds: notifyForm.userIds,
    selectedUsers
  })
  
  ElMessage.success(`已成功发送通知到 ${selectedUsers}`)
  notifyDialogVisible.value = false
}

// 生命周期
onMounted(() => {
  // 从路由参数获取告警ID
  const alarmId = route.params.id || route.query.id
  console.log('告警ID:', alarmId)
  
  // 加载告警数据
  alarmData.value = mockAlarmData
  
  // 初始化图表
  nextTick(() => {
    initChart()
  })
  
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  chartInstance?.dispose()
})
</script>

<style scoped>
.h5-disposal-page {
  min-height: 100vh;
  background: #f5f7fa;
  display: flex;
  flex-direction: column;
}

/* 顶部导航 */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 50px;
  background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);
  color: #fff;
  padding: 0 16px;
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-back {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  cursor: pointer;
}

.header-title {
  font-size: 18px;
  font-weight: 600;
}

.header-right {
  width: 40px;
}

/* 内容区域 */
.page-content {
  flex: 1;
  padding: 12px;
  padding-bottom: 180px;
  overflow-y: auto;
}

/* 信息卡片 */
.info-card {
  background: #fff;
  border-radius: 12px;
  margin-bottom: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  background: #fafafa;
}

.card-header.compact {
  padding-top: 7px;
  padding-bottom: 7px;
}

.card-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
}

.card-icon.alarm {
  background: #fef0f0;
  color: #f56c6c;
}

.card-icon.project {
  background: #e8f5e9;
  color: #67c23a;
}

.card-icon.data {
  background: #e3f2fd;
  color: #409eff;
}

.card-icon.timeline {
  background: #fff3e0;
  color: #e6a23c;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.card-body {
  padding: 12px 16px;
}

/* 信息行 */
.info-row {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}

.info-row:last-child {
  margin-bottom: 0;
}

.info-item {
  flex: 1;
  min-width: 0;
}

.info-item.full {
  flex: none;
  width: 100%;
}

.info-label {
  font-size: 12px;
  color: #909399;
  display: block;
  margin-bottom: 4px;
}

.info-value {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
}

.info-value.source {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
}

.info-value.source .el-icon {
  color: #909399;
  font-size: 14px;
}

.source-wrapper {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.map-icon-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #67c23a;
  border-radius: 50%;
  cursor: pointer;
  color: #fff;
  font-size: 16px;
  transition: all 0.2s;
}

.map-icon-btn:active {
  transform: scale(0.95);
}

.info-desc {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  margin: 0;
  background: #f8f9fa;
  padding: 8px 12px;
  border-radius: 6px;
}

/* 等级标签 */
.level-badge {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 4px;
  color: #fff;
  font-size: 12px;
}

.level-badge.level-1 {
  background: #f56c6c;
}

.level-badge.level-2 {
  background: #e6a23c;
}

.level-badge.level-3 {
  background: #67c23a;
}

.level-badge.level-4 {
  background: #909399;
}

/* Tab切换 */
.data-tabs {
  display: flex;
  border-bottom: 1px solid #f0f0f0;
  background: #fafafa;
}

.tab-item {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 12px 0;
  font-size: 13px;
  color: #606266;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  transition: all 0.2s;
}

.tab-item.active {
  color: #409eff;
  border-bottom-color: #409eff;
  font-weight: 600;
  background: #fff;
}

.tab-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  background: #f56c6c;
  color: #fff;
  font-size: 11px;
  border-radius: 9px;
  margin-left: 4px;
  font-weight: 500;
}

/* 监测数据子tab */
.monitor-sub-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.sub-tab {
  font-size: 12px;
  color: #909399;
  padding: 4px 12px;
  border-radius: 4px;
  cursor: pointer;
  background: #f5f7fa;
}

.sub-tab.active {
  background: #409eff;
  color: #fff;
}

.chart-container {
  width: 100%;
  height: 220px;
}

/* 搜索栏 */
.search-bar {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 12px;
}

.search-input {
  width: 100%;
}

.search-date {
  width: 100%;
}

/* 移动端表格 */
.table-wrapper {
  max-height: 300px;
  overflow-y: auto;
}

.mobile-table {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.table-row {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 10px 12px;
}

.row-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.row-time {
  font-size: 12px;
  color: #909399;
}

.row-content {
  font-size: 13px;
  color: #606266;
  line-height: 1.5;
}

.notify-row .row-info {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.channel-tag {
  font-size: 11px;
  color: #409eff;
  background: #e3f2fd;
  padding: 2px 6px;
  border-radius: 3px;
}

.target-text {
  font-size: 12px;
  color: #606266;
}

/* 分页 */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 12px;
}

/* 时间线 */
.timeline {
  position: relative;
  padding-left: 20px;
}

.timeline-item {
  position: relative;
  padding-bottom: 16px;
}

.timeline-item:last-child {
  padding-bottom: 0;
}

.timeline-dot {
  position: absolute;
  left: -20px;
  top: 4px;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #d9d9d9;
}

.timeline-dot.trigger {
  background: #f56c6c;
}

.timeline-dot.system {
  background: #409eff;
}

.timeline-dot.confirm {
  background: #67c23a;
}

.timeline-line {
  position: absolute;
  left: -15px;
  top: 16px;
  width: 2px;
  height: calc(100% - 8px);
  background: #e4e7ed;
}

.timeline-content {
  padding-left: 8px;
}

.timeline-time {
  font-size: 12px;
  color: #909399;
  margin-bottom: 2px;
}

.timeline-desc {
  font-size: 13px;
  color: #303133;
}

.timeline-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 24px 0;
  color: #909399;
}

.timeline-empty .el-icon {
  font-size: 32px;
  margin-bottom: 8px;
}

.timeline-empty p {
  font-size: 13px;
  margin: 0;
}

/* 底部操作栏 */
.page-footer {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: #fff;
  padding: 12px 16px;
  box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.08);
  z-index: 100;
}

.action-buttons {
  display: flex;
  gap: 8px;
  justify-content: space-between;
}

.action-buttons .el-button {
  flex: 1;
}

/* 通知弹窗 */
.notify-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-label {
  font-size: 13px;
  color: #303133;
  font-weight: 500;
}

.channel-group {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.channel-group .el-checkbox {
  margin-right: 0;
}

.user-select {
  width: 100%;
}

.user-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.user-name {
  font-weight: 500;
}

.user-role {
  font-size: 12px;
  color: #909399;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

/* 地图弹窗 */
.map-container {
  width: 100%;
  height: 280px;
  background: linear-gradient(180deg, #e8f5f3 0%, #d0e6e3 100%);
  border-radius: 8px;
  overflow: hidden;
  position: relative;
}

.map-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  position: relative;
}

.map-placeholder .el-icon {
  font-size: 48px;
  color: #67c23a;
  margin-bottom: 12px;
}

.map-location {
  font-size: 15px;
  color: #303133;
  font-weight: 500;
  margin-bottom: 8px;
  text-align: center;
  padding: 0 16px;
}

.map-coords {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: #606266;
}

.map-marker {
  position: absolute;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  flex-direction: column;
  align-items: center;
}

.marker-pin {
  width: 24px;
  height: 24px;
  background: #67c23a;
  border-radius: 50% 50% 50% 0;
  transform: rotate(-45deg);
  position: relative;
}

.marker-pin::after {
  content: '';
  width: 10px;
  height: 10px;
  background: #fff;
  border-radius: 50%;
  position: absolute;
  top: 5px;
  left: 5px;
}

.marker-label {
  font-size: 12px;
  color: #303133;
  margin-top: 8px;
  white-space: nowrap;
  background: rgba(255, 255, 255, 0.9);
  padding: 4px 8px;
  border-radius: 4px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

/* 反馈弹窗 */
.feedback-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.upload-area {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  min-height: 48px;
}

.uploaded-file {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  background: #f8f9fa;
  border-radius: 6px;
  max-width: calc(100% - 100px);
}

.file-icon {
  width: 28px;
  height: 28px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
}

.file-icon.image {
  background: #e8f5e9;
  color: #67c23a;
}

.file-icon.pdf {
  background: #fef0f0;
  color: #f56c6c;
}

.file-icon.word {
  background: #e3f2fd;
  color: #409eff;
}

.file-icon.excel {
  background: #fff3e0;
  color: #e6a23c;
}

.file-icon.other {
  background: #f5f7fa;
  color: #909399;
}

.file-name {
  font-size: 12px;
  color: #606266;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-remove {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
  cursor: pointer;
  border-radius: 50%;
  transition: all 0.2s;
}

.file-remove:hover {
  background: #f5f7fa;
  color: #f56c6c;
}

.upload-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 72px;
  height: 72px;
  border: 2px dashed #d9d9d9;
  border-radius: 8px;
  cursor: pointer;
  color: #909399;
  transition: all 0.2s;
}

.upload-btn:hover {
  border-color: #409eff;
  color: #409eff;
}

.upload-btn .el-icon {
  font-size: 20px;
  margin-bottom: 4px;
}

.upload-btn span {
  font-size: 12px;
}

.file-input {
  display: none;
}

.form-hint {
  font-size: 12px;
  color: #909399;
  margin-top: 8px;
  display: block;
}
</style>
