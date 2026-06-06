<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="emit('update:modelValue', $event)"
    :title="dialogTitle"
    width="90%"
    max-width="1000px"
    :close-on-click-modal="false"
  >
    <div class="feedback-container" v-if="data">
      <div class="main-content">
        <!-- 左侧生命周期 -->
        <div class="left-section">
          <div class="section-header">
            <span class="icon-wrapper"><Clock /></span>
            <span class="section-title">生命周期</span>
          </div>
          <div class="lifecycle-list">
            <div class="lifecycle-item" v-for="(node, i) in lifeNodes" :key="i" :class="{ active: node.active }">
              <div class="node-wrapper">
                <div class="lifecycle-node" :class="{ alarm: node.alarm }">
                  <el-icon><component :is="node.icon" /></el-icon>
                </div>
                <div v-if="i < lifeNodes.length - 1" class="lifecycle-line"></div>
              </div>
              <div class="node-label">{{ node.label }}</div>
            </div>
          </div>
        </div>

        <!-- 右侧内容区 -->
        <div class="right-section">
          <!-- 头部卡片：左右结构一行 -->
          <div class="event-header">
            <div class="hd-item">
              <div class="hd-icon hazard"><MapLocation /></div>
              <div class="hd-text">
                <span class="hd-label">隐患点</span>
                <span class="hd-val">{{ data.hazardPointName }}</span>
              </div>
            </div>
            <div class="hd-item">
              <div class="hd-icon device"><Monitor /></div>
              <div class="hd-text">
                <span class="hd-label">设备名</span>
                <span class="hd-val">{{ data.deviceName || '未知设备' }}</span>
              </div>
            </div>
            <div class="hd-item">
              <div class="hd-icon level" :class="levelCls"><WarnTriangleFilled /></div>
              <div class="hd-text">
                <span class="hd-label">告警等级</span>
                <span class="hd-val lv" :class="levelCls">{{ alarmLevelRange }}</span>
              </div>
            </div>
            <div class="hd-item">
              <div class="hd-icon time"><Clock /></div>
              <div class="hd-text">
                <span class="hd-label">发生时间</span>
                <span class="hd-val">{{ formatDuration(data.firstAlarmTime) }}</span>
              </div>
            </div>
            <div class="hd-item alarm-count-item" @click="switchToAlarmTab">
              <div class="hd-icon count"><Bell /></div>
              <div class="hd-text">
                <span class="hd-label">告警次数</span>
                <span class="hd-count">{{ data.alarmCount || 0 }}</span>
              </div>
            </div>
          </div>

          <div class="event-body">
            <!-- 数据区域 - 页签 -->
          <div class="data-section">
            <!-- 基本资料（无标题） -->
            <div class="basic-info">
              <div class="info-row">
                <div class="info-item">
                  <span class="info-label">初次告警</span>
                  <span class="info-value">{{ data.firstAlarmTime || '-' }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">最后告警</span>
                  <span class="info-value">{{ data.lastAlarmTime || '-' }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">告警等级</span>
                  <span class="info-value">{{ alarmLevelRange }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">告警次数</span>
                  <span class="info-value count-link" @click="switchToAlarmTab">{{ data.alarmCount || 0 }}</span>
                </div>
              </div>
            </div>

            <div class="basic-details">
              <!-- 基本资料 -->
              <div class="detail-grid">
                <div class="detail-item">
                  <span class="detail-label">所属分组</span>
                  <span class="detail-value">{{ data.groupName || '-' }}</span>
                </div>
                <div class="detail-item">
                  <span class="detail-label">隐患点</span>
                  <span class="detail-value">{{ data.hazardPointName }}</span>
                </div>
                <div class="detail-item">
                  <span class="detail-label">告警类型</span>
                  <span class="detail-value">{{ getAlarmTypeText(data.alarmType) }}</span>
                </div>
                <div class="detail-item">
                  <span class="detail-label">设备名称</span>
                  <span class="detail-value">{{ data.deviceName || '-' }}</span>
                </div>
              </div>
              <div class="detail-desc">
                <span class="detail-label">告警描述</span>
                <p>{{ data.alarmContent || '-' }}</p>
              </div>
            </div>

            <div class="data-tabs">
              <div class="tab" :class="{ active: activeTab === 'monitor' }" @click="activeTab = 'monitor'; nextTick(updateChart)">监测数据</div>
              <div class="tab" :class="{ active: activeTab === 'alarm' }" @click="activeTab = 'alarm'">告警次数</div>
              <div class="tab" :class="{ active: activeTab === 'notify' }" @click="activeTab = 'notify'">通知记录</div>
            </div>

            <!-- 监测数据 -->
            <div v-show="activeTab === 'monitor'" class="tab-content">
              <div class="monitor-sub-tabs">
                <span class="sub-tab" :class="{ active: activeDataTab === 'monitor' }" @click="activeDataTab = 'monitor'; updateChart()">监测曲线</span>
                <span class="sub-tab" :class="{ active: activeDataTab === 'deduce' }" @click="activeDataTab = 'deduce'; updateChart()">推演曲线</span>
              </div>
              <div ref="chartRef" class="chart-container"></div>
            </div>

            <!-- 告警次数 -->
            <div v-show="activeTab === 'alarm'" class="tab-content">
              <div class="tab-search">
                <el-input v-model="alarmFilter.desc" placeholder="描述模糊查询" size="small" clearable class="tab-sch-inp" />
                <el-date-picker v-model="alarmFilter.timeRange" type="daterange" range-separator="至"
                  start-placeholder="开始" end-placeholder="结束" value-format="YYYY-MM-DD" size="small" class="tab-sch-date" />
                <el-button size="small" @click="alarmFilter.desc=''; alarmFilter.timeRange=[]">重置</el-button>
              </div>
              <el-table :data="filteredAlarmList" border stripe size="small" max-height="280">
                <el-table-column prop="alarmTime" label="告警时间" width="160" />
                <el-table-column prop="alarmLevel" label="告警等级" width="90">
                  <template #default="{ row }">
                    <el-tag :type="getAlarmLevelType(row.alarmLevel)" size="small">{{ getAlarmLevelText(row.alarmLevel) }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="alarmContent" label="描述" min-width="220" show-overflow-tooltip />
              </el-table>
            </div>

            <!-- 通知记录 -->
            <div v-show="activeTab === 'notify'" class="tab-content">
              <div class="tab-search">
                <el-input v-model="notifyFilter.account" placeholder="账号模糊查询" size="small" clearable class="tab-sch-inp" />
                <el-date-picker v-model="notifyFilter.timeRange" type="daterange" range-separator="至"
                  start-placeholder="开始" end-placeholder="结束" value-format="YYYY-MM-DD" size="small" class="tab-sch-date" />
                <el-button size="small" @click="notifyFilter.account=''; notifyFilter.timeRange=[]">重置</el-button>
              </div>
              <el-table :data="filteredNotifyList" border stripe size="small" max-height="280">
                <el-table-column prop="notifyTime" label="通知时间" width="160" />
                <el-table-column prop="channelType" label="渠道类型" width="100" />
                <el-table-column prop="target" label="账号/电话/邮箱" width="160" show-overflow-tooltip />
                <el-table-column prop="content" label="内容" min-width="180" show-overflow-tooltip />
                <el-table-column prop="success" label="是否成功" width="90">
                  <template #default="{ row }">
                    <el-tag :type="row.success ? 'success' : 'danger'" size="small">{{ row.success ? '成功' : '失败' }}</el-tag>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </div>

          <!-- 处置时间线 -->
          <div class="timeline-section">
            <div class="section-header">
              <span class="icon-wrapper"><List /></span>
              <span class="section-title">时间线</span>
            </div>
            <div class="timeline-container">
              <div v-if="timelineData.length > 0" class="timeline">
                <div v-for="(item, index) in timelineData" :key="index" class="timeline-item">
                  <div class="timeline-dot" :class="item.type"></div>
                  <div v-if="index < timelineData.length - 1" class="timeline-line"></div>
                  <div class="timeline-content">
                    <div class="timeline-time">{{ item.time }}</div>
                    <div class="timeline-desc">{{ item.description }}</div>
                  </div>
                </div>
              </div>
              <div v-else class="timeline-empty">
                <el-icon class="empty-icon"><List /></el-icon>
                <p>暂无处置记录</p>
              </div>
            </div>
          </div>

          </div>
          <!-- /event-body -->

          <!-- 底部操作栏 -->
          <div class="event-footer">
            <div class="action-left">
              <el-input
                v-model="remark"
                type="textarea"
                :rows="2"
                placeholder="请输入处置备注..."
                class="remark-input"
              />
            </div>
            <div class="action-right">
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
              <el-button size="small" @click="handleClose">
                <el-icon><Close /></el-icon> 关闭
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Bell, Box, ChatDotRound, Check, CircleClose, Clock, Close, Coin, Connection, List, MapLocation, Monitor, View, Warning, WarnTriangleFilled } from '@element-plus/icons-vue'
import type { Component } from 'vue'
import * as echarts from 'echarts'

const props = defineProps<{
  modelValue: boolean
  data: any
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'close'): void
  (e: 'submit'): void
  (e: 'view-detail'): void
  (e: 'quick-response'): void
  (e: 'false-alarm', data: any): void
  (e: 'close-alarm', data: any): void
  (e: 'notify', data: any): void
}>()

const remark = ref('')
const activeTab = ref('monitor')
const activeDataTab = ref('monitor')
const chartRef = ref<HTMLDivElement | null>(null)
let chartInstance: echarts.ECharts | null = null

// 生命周期节点
const lifeNodes = [
  { icon: Box, label: '设备', alarm: false, active: false },
  { icon: Connection, label: '物联网接入', alarm: false, active: false },
  { icon: Coin, label: '数据存储', alarm: false, active: false },
  { icon: Bell, label: '警报级', alarm: true, active: true },
  { icon: View, label: '情况核查', alarm: false, active: false },
  { icon: Check, label: '核查情况', alarm: false, active: false },
]

const timelineData = ref([
  { time: '2025-01-15 10:30:00', description: '告警触发', type: 'trigger' },
  { time: '2025-01-15 10:31:00', description: '系统自动响应', type: 'system' },
  { time: '2025-01-15 10:35:00', description: '值班人员确认', type: 'confirm' }
])

// 告警等级 — 起止相同时只显示一个
const alarmLevelRange = computed(() => {
  const d = props.data
  if (!d) return '-'
  const minLevel = d.minAlarmLevel ?? d.alarmLevel
  const maxLevel = d.maxAlarmLevel ?? d.alarmLevel
  const minText = getAlarmLevelText(minLevel)
  const maxText = getAlarmLevelText(maxLevel)
  return minText === maxText ? minText : `${minText}-${maxText}`
})

const levelCls = computed(() => getAlarmLevelType(props.data?.alarmLevel))

// 告警列表 mock
const alarmList = ref<any[]>([
  { alarmTime: '2024-06-01 08:30:00', alarmLevel: '1', alarmContent: '边坡位移速率超过阈值12mm/h，当前值为12.5mm/h' },
  { alarmTime: '2024-06-01 12:45:00', alarmLevel: '1', alarmContent: '边坡位移速率超过阈值12mm/h，当前值为13.8mm/h' },
  { alarmTime: '2024-06-02 09:00:00', alarmLevel: '2', alarmContent: '降雨量超过预警值，当前35mm/h' },
  { alarmTime: '2024-06-02 18:20:00', alarmLevel: '1', alarmContent: '边坡位移加速，当前值14.2mm/h' },
  { alarmTime: '2024-06-03 06:10:00', alarmLevel: '3', alarmContent: '轻微倾斜预警，角度3.5度' },
  { alarmTime: '2024-06-03 14:25:00', alarmLevel: '1', alarmContent: '边坡位移速率超过阈值12mm/h，当前值为15.2mm/h' },
])

const alarmFilter = reactive({ desc: '', timeRange: [] as string[] })
const filteredAlarmList = computed(() => {
  let list = alarmList.value
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

// 通知记录 mock
const notifyList = ref<any[]>([
  { notifyTime: '2024-06-01 08:32:00', channelType: '短信', target: '138****1234', content: '边坡监测点A-01发生一级告警', success: true },
  { notifyTime: '2024-06-01 08:32:00', channelType: '邮件', target: 'zhangsan@abc.com', content: '边坡监测点A-01发生一级告警', success: true },
  { notifyTime: '2024-06-01 08:35:00', channelType: '电话', target: '138****1234', content: '边坡监测点A-01告警未响应，请及时处理', success: false },
  { notifyTime: '2024-06-02 09:05:00', channelType: '短信', target: 'lisi@abc.com', content: '边坡监测点A-01告警升级', success: true },
  { notifyTime: '2024-06-03 14:30:00', channelType: '邮件', target: 'admin@abc.com', content: '边坡监测点A-01持续告警，请关注', success: true },
])

const notifyFilter = reactive({ account: '', timeRange: [] as string[] })
const filteredNotifyList = computed(() => {
  let list = notifyList.value
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

const switchToAlarmTab = () => {
  activeTab.value = 'alarm'
}

// ---------- 图表 ----------
const generateChartData = () => {
  const alarmTime = props.data?.firstAlarmTime || new Date().toISOString()
  const alarmDate = new Date(alarmTime)
  const monitorData: { time: string; value: number; isAlarm: boolean }[] = []
  const deduceData: { time: string; value: number; isAlarm: boolean }[] = []

  for (let i = -3; i <= 3; i++) {
    const date = new Date(alarmDate)
    date.setDate(date.getDate() + i)
    for (let hour = 0; hour < 24; hour += 4) {
      date.setHours(hour, 0, 0, 0)
      const timeStr = date.toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
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
        return `<div style="padding:8px"><div style="font-weight:bold;margin-bottom:4px">${data.name}</div><div>数值: <span style="color:${isAlarm ? '#f56c6c' : '#409eff'}">${data.value}</span></div>${isAlarm ? '<div style="color:#f56c6c;margin-top:4px">⚠️ 告警数据点</div>' : ''}</div>`
      }
    },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '15%', containLabel: true },
    xAxis: {
      type: 'category', data: chartData.map(item => item.time),
      axisLabel: { rotate: 45, fontSize: 10, color: '#666' },
      axisLine: { lineStyle: { color: '#ddd' } }
    },
    yAxis: {
      type: 'value', name: '监测值',
      axisLabel: { fontSize: 10, color: '#666' },
      axisLine: { lineStyle: { color: '#ddd' } },
      splitLine: { lineStyle: { color: '#f0f0f0' } }
    },
    series: [{
      name: activeDataTab.value === 'monitor' ? '监测数据' : '推演数据',
      type: 'line',
      data: chartData.map(item => item.value),
      smooth: true, symbol: 'circle',
      symbolSize: (_v: number, params: any) => alarmIndices.includes(params.dataIndex) ? 10 : 6,
      itemStyle: { color: (params: any) => alarmIndices.includes(params.dataIndex) ? '#f56c6c' : '#409eff' },
      lineStyle: { width: 2, color: '#409eff' },
      areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(64,158,255,.3)' }, { offset: 1, color: 'rgba(64,158,255,.05)' }]) },
      markPoint: {
        data: alarmPoints.map((point, i) => ({
          name: `告警点${i + 1}`, coord: [chartData.indexOf(point), point.value],
          value: point.value, itemStyle: { color: '#f56c6c' }, symbol: 'pin', symbolSize: 40,
          label: { show: true, formatter: '⚠️', fontSize: 12 }
        }))
      }
    }],
    dataZoom: [{ type: 'inside', start: 0, end: 100 }, { type: 'slider', show: true, start: 0, end: 100, height: 20, bottom: 0 }]
  } as echarts.EChartsOption, true)
}

const handleResize = () => { chartInstance?.resize() }

watch(() => props.modelValue, async (visible) => {
  if (visible) {
    activeTab.value = 'monitor'
    activeDataTab.value = 'monitor'
    alarmFilter.desc = ''; alarmFilter.timeRange = []
    notifyFilter.account = ''; notifyFilter.timeRange = []
    await nextTick()
    initChart()
  }
})

onMounted(() => { window.addEventListener('resize', handleResize) })
onUnmounted(() => { window.removeEventListener('resize', handleResize); chartInstance?.dispose() })

// ---------- 工具函数 ----------
const dialogTitle = computed(() => {
  if (!props.data) return '告警反馈'
  return `${props.data.hazardPointName}[${props.data.firstAlarmTime}]`
})

const formatDuration = (startTime: string) => {
  if (!startTime) return '0小时0分0秒'
  const diff = Date.now() - new Date(startTime).getTime()
  const h = Math.floor(diff / 3600000)
  const m = Math.floor((diff % 3600000) / 60000)
  const s = Math.floor((diff % 60000) / 1000)
  return `${h}小时${m}分${s}秒前`
}

const getAlarmLevelType = (level: string) => ({ '1': 'danger', '2': 'warning', '3': 'success', '4': 'info' } as Record<string, string>)[level] || 'info'
const getAlarmLevelText = (level: string) => ({ '1': '一级', '2': '二级', '3': '三级', '4': '四级' } as Record<string, string>)[level] || level
const getAlarmTypeText = (type: string) => ({ 'threshold': '阈值预警', 'comprehensive': '综合预警' } as Record<string, string>)[type] || type

const handleFeedback = () => { emit('submit') }
const handleFalseAlarm = () => { emit('false-alarm', props.data) }
const handleCloseAlarm = () => { emit('close-alarm', props.data) }
const handleNotify = () => { emit('notify', props.data); ElMessage.info('通知功能已触发') }
const handleClose = () => { emit('update:modelValue', false); emit('close') }
</script>

<style scoped>
.feedback-container { min-height: 500px; }
.main-content { display: flex; gap: 12px; }

/* ====== 左侧生命周期 ====== */
.left-section {
  width: 100px;
  background: #f8f9fa;
  border-radius: 8px;
  padding: 10px 8px;
  border: 1px solid #e9ecef;
  flex-shrink: 0;
}
.section-header {
  display: flex; align-items: center; gap: 4px;
  margin-bottom: 8px; padding-bottom: 6px;
  border-bottom: 1px solid #e9ecef;
}
.icon-wrapper {
  width: 20px; height: 20px;
  display: flex; align-items: center; justify-content: center;
  background: #e8f4fd; border-radius: 4px; color: #409eff;
}
.section-title { font-size: 12px; font-weight: 600; color: #333; }
.lifecycle-list { display: flex; flex-direction: column; }
.lifecycle-item { display: flex; flex-direction: column; align-items: center; padding: 3px 0; }
.node-wrapper { display: flex; flex-direction: column; align-items: center; }
.lifecycle-node {
  width: 24px; height: 24px; border-radius: 50%;
  background: #adb5bd; display: flex; align-items: center; justify-content: center;
  color: #fff; font-size: 11px; transition: all .3s;
}
.lifecycle-node.alarm { background: #f56c6c; }
.lifecycle-item.active .lifecycle-node { transform: scale(1.15); box-shadow: 0 0 8px rgba(245,108,108,.5); }
.lifecycle-line { width: 2px; height: 10px; background: #dee2e6; margin: 2px 0; }
.node-label { font-size: 10px; color: #6c757d; margin-top: 2px; }

/* ====== 右侧 ====== */
.right-section { flex: 1; display: flex; flex-direction: column; gap: 10px; min-width: 0; }

/* ====== 头部卡片 - 一行显示 ====== */
.event-header {
  display: flex; align-items: center; gap: 0;
  background: #fff; border-radius: 8px; border: 1px solid #e9ecef;
  padding: 10px 14px;
}
.hd-item {
  display: flex; align-items: center; gap: 8px;
  flex: 1; min-width: 0;
  padding: 0 10px;
}
.hd-item + .hd-item { border-left: 1px solid #f0f0f0; }
.hd-icon {
  width: 34px; height: 34px; border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
  font-size: 16px; flex-shrink: 0;
}
.hd-icon.hazard { background: #e8f5e9; color: #28a745; }
.hd-icon.device { background: #e3f2fd; color: #1976d2; }
.hd-icon.level { background: #fff3e0; color: #ff9800; }
.hd-icon.level.danger { background: #fee2e2; color: #dc3545; }
.hd-icon.level.warning { background: #fff3cd; color: #ffc107; }
.hd-icon.level.success { background: #d4edda; color: #28a745; }
.hd-icon.level.info { background: #d1ecf1; color: #17a2b8; }
.hd-icon.time { background: #f3e8ff; color: #6f42c1; }
.hd-icon.count { background: #fef0f0; color: #dc3545; }
.hd-text { display: flex; flex-direction: column; gap: 1px; min-width: 0; }
.hd-label { font-size: 10px; color: #909399; }
.hd-val { font-size: 13px; font-weight: 600; color: #303133; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.hd-val.lv { display: inline-block; padding: 1px 6px; border-radius: 3px; color: #fff; font-size: 11px; width: fit-content; }
.hd-val.lv.danger { background: #f56c6c; }
.hd-val.lv.warning { background: #e6a23c; }
.hd-val.lv.success { background: #67c23a; }
.hd-val.lv.info { background: #409eff; }

.hd-count {
  font-size: 18px; font-weight: 700; color: #f56c6c;
  cursor: pointer; transition: transform .15s;
}
.hd-count:hover { transform: scale(1.15); }
.alarm-count-item { cursor: pointer; }

/* ====== 中部：数据区 + 时间线（左右） ====== */
.event-body {
  display: flex;
  gap: 10px;
}

/* ====== 数据区域 ====== */
.data-section {
  flex: 1;
  min-width: 0;
  background: #fff; border-radius: 8px; border: 1px solid #e9ecef;
  padding: 10px 12px;
  display: flex; flex-direction: column;
}

/* 基本资料行 */
.basic-info { margin-bottom: 8px; }
.info-row { display: flex; gap: 10px; }
.info-item {
  flex: 1;
  background: #f8f9fa; border-radius: 6px; padding: 6px 10px;
}
.info-label { font-size: 10px; color: #909399; display: block; margin-bottom: 2px; }
.info-value { font-size: 12px; font-weight: 500; color: #303133; }
.count-link { color: #409eff; cursor: pointer; text-decoration: underline; }
.count-link:hover { color: #66b1ff; }

/* 基本资料详情 */
.basic-details { margin-bottom: 8px; }
.detail-grid { display: flex; gap: 10px; margin-bottom: 6px; }
.detail-item { flex: 1; display: flex; flex-direction: column; gap: 1px; }
.detail-label { font-size: 10px; color: #909399; }
.detail-value { font-size: 12px; color: #303133; }
.detail-desc {
  background: #f8f9fa; border-radius: 6px; padding: 6px 10px;
}
.detail-desc .detail-label { display: block; margin-bottom: 2px; }
.detail-desc p { font-size: 12px; color: #606266; line-height: 1.5; margin: 0; }

.data-tabs {
  display: flex; gap: 16px; margin-bottom: 8px;
  padding-bottom: 6px; border-bottom: 1px solid #f0f0f0;
}
.data-tabs .tab {
  font-size: 12px; color: #606266; padding: 3px 0;
  cursor: pointer; border-bottom: 2px solid transparent;
  transition: all .2s;
}
.data-tabs .tab.active { color: #409eff; border-bottom-color: #409eff; font-weight: 600; }
.tab-content { flex: 1; min-height: 0; }

/* 监测数据子tab */
.monitor-sub-tabs { display: flex; gap: 12px; margin-bottom: 6px; }
.sub-tab { font-size: 11px; color: #909399; cursor: pointer; padding: 2px 8px; border-radius: 3px; }
.sub-tab.active { background: #409eff; color: #fff; }
.chart-container { width: 100%; height: 260px; }

/* 页签内搜索 */
.tab-search { display: flex; gap: 8px; align-items: center; margin-bottom: 8px; }
.tab-sch-inp { width: 180px; }
.tab-sch-date { width: 240px; }

/* ====== 处置时间线 ====== */
.timeline-section {
  width: 180px;
  flex-shrink: 0;
  background: #fff; border-radius: 8px; border: 1px solid #e9ecef;
  padding: 10px 12px;
}
.timeline-container { min-height: 80px; }
.timeline { position: relative; padding-left: 14px; }
.timeline-item { position: relative; padding-bottom: 14px; }
.timeline-item:last-child { padding-bottom: 0; }
.timeline-dot {
  position: absolute; left: -14px; top: 2px;
  width: 10px; height: 10px; border-radius: 50%; background: #d9d9d9;
}
.timeline-dot.trigger { background: #f56c6c; }
.timeline-dot.system { background: #409eff; }
.timeline-dot.confirm { background: #67c23a; }
.timeline-line {
  position: absolute; left: -10px; top: 12px;
  width: 2px; height: calc(100% - 8px); background: #d9d9d9;
}
.timeline-content { padding-left: 8px; }
.timeline-time { font-size: 11px; color: #909399; margin-bottom: 2px; }
.timeline-desc { font-size: 11px; color: #303133; }
.timeline-empty {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  height: 60px; color: #909399;
}
.empty-icon { font-size: 20px; margin-bottom: 4px; }
.timeline-empty p { font-size: 11px; margin: 0; }

/* ====== 底部操作栏 ====== */
.event-footer {
  display: flex; gap: 10px;
  background: #fff; border-radius: 8px; border: 1px solid #e9ecef;
  padding: 10px 12px;
}
.action-left { flex: 1; }
.remark-input { width: 100%; resize: none; }
.remark-input :deep(.el-textarea__inner) { font-size: 12px; padding: 8px; }
.action-right { display: flex; gap: 6px; align-items: flex-end; }
</style>
