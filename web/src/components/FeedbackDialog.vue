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
        <div class="left-section">
          <div class="section-header">
            <span class="icon-wrapper"><Clock /></span>
            <span class="section-title">生命周期</span>
          </div>
          <div class="lifecycle-list">
            <div class="lifecycle-item">
              <div class="node-wrapper">
                <div class="lifecycle-node">
                  <el-icon><Box /></el-icon>
                </div>
                <div class="lifecycle-line"></div>
              </div>
              <div class="node-label">设备</div>
            </div>
            <div class="lifecycle-item">
              <div class="node-wrapper">
                <div class="lifecycle-node">
                  <el-icon><Connection /></el-icon>
                </div>
                <div class="lifecycle-line"></div>
              </div>
              <div class="node-label">物联网接入</div>
            </div>
            <div class="lifecycle-item">
              <div class="node-wrapper">
                <div class="lifecycle-node">
                  <el-icon><Coin /></el-icon>
                </div>
                <div class="lifecycle-line"></div>
              </div>
              <div class="node-label">数据存储</div>
            </div>
            <div class="lifecycle-item active">
              <div class="node-wrapper">
                <div class="lifecycle-node alarm">
                  <el-icon><Bell /></el-icon>
                </div>
                <div class="lifecycle-line"></div>
              </div>
              <div class="node-label">警报级</div>
            </div>
            <div class="lifecycle-item">
              <div class="node-wrapper">
                <div class="lifecycle-node">
                  <el-icon><View /></el-icon>
                </div>
                <div class="lifecycle-line"></div>
              </div>
              <div class="node-label">情况核查</div>
            </div>
            <div class="lifecycle-item">
              <div class="node-wrapper">
                <div class="lifecycle-node">
                  <el-icon><Check /></el-icon>
                </div>
                <div class="lifecycle-line"></div>
              </div>
              <div class="node-label">核查情况</div>
            </div>
            <div class="lifecycle-item">
              <div class="node-wrapper">
                <div class="lifecycle-node">
                  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="12" height="12"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
                </div>
              </div>
              <div class="node-label">关闭事件</div>
            </div>
          </div>
        </div>

        <div class="right-section">
          <div class="event-header">
            <div class="header-card">
              <div class="header-item">
                <div class="item-icon hazard"><MapLocation /></div>
                <div class="item-content">
                  <span class="item-label">隐患点</span>
                  <span class="item-value">{{ data.hazardPointName }}</span>
                </div>
              </div>
              <div class="header-item">
                <div class="item-icon device"><Monitor /></div>
                <div class="item-content">
                  <span class="item-label">设备名</span>
                  <span class="item-value">{{ data.deviceName || '未知设备' }}</span>
                </div>
              </div>
              <div class="header-item">
                <div class="item-icon level" :class="getAlarmLevelType(data.alarmLevel)"><WarnTriangleFilled /></div>
                <div class="item-content">
                  <span class="item-label">告警等级</span>
                  <span class="item-value level-value" :class="getAlarmLevelType(data.alarmLevel)">
                    {{ getAlarmLevelText(data.alarmLevel) }}级(警报)
                  </span>
                </div>
              </div>
              <div class="header-item">
                <div class="item-icon time"><Clock /></div>
                <div class="item-content">
                  <span class="item-label">发生时间</span>
                  <span class="item-value">{{ formatDuration(data.firstAlarmTime) }}</span>
                </div>
              </div>
            </div>
          </div>

          <div class="event-body">
            <div class="data-section">
              <div class="section-header">
                <span class="icon-wrapper"><Files /></span>
                <span class="section-title">资料展示</span>
              </div>
              
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
                    <span class="info-label">告警等级范围</span>
                    <span class="info-value">{{ getAlarmLevelText(data.alarmLevel) }}级-{{ getAlarmLevelText(data.alarmLevel) }}级</span>
                  </div>
                  <div class="info-item">
                    <span class="info-label">告警次数</span>
                    <span class="info-value">{{ data.alarmCount || 0 }}</span>
                  </div>
                </div>
              </div>

              <div class="basic-details">
                <div class="detail-title">基本资料</div>
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

              <div class="monitor-data">
                <div class="detail-title">监测数据</div>
                <div class="data-tabs">
                  <div class="tab" :class="{ active: activeDataTab === 'monitor' }" @click="activeDataTab = 'monitor'; updateChart()">监测数据</div>
                  <div class="tab" :class="{ active: activeDataTab === 'deduce' }" @click="activeDataTab = 'deduce'; updateChart()">推演数据</div>
                </div>
                <div class="data-content">
                  <div ref="chartRef" class="chart-container"></div>
                </div>
              </div>
            </div>

            <div class="timeline-section">
              <div class="section-header">
                <span class="icon-wrapper"><List /></span>
                <span class="section-title">处置时间线</span>
              </div>
              <div class="timeline-container">
                <div v-if="timelineData.length > 0" class="timeline">
                  <div v-for="(item, index) in timelineData" :key="index" class="timeline-item">
                    <div class="timeline-dot" :class="item.type"></div>
                    <div class="timeline-line" v-if="index < timelineData.length - 1"></div>
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
                <el-icon><Message /></el-icon>
                反馈
              </el-button>
              <el-button type="warning" size="small" @click="handleFalseAlarm">
                <el-icon><Warning /></el-icon>
                误报
              </el-button>
              <el-button type="danger" size="small" @click="handleCloseAlarm">
                <el-icon><CircleClose /></el-icon>
                消警
              </el-button>
              <el-button type="info" size="small" @click="handleNotify">
                <el-icon><Bell /></el-icon>
                通知
              </el-button>
              <el-button size="small" @click="handleClose">
                <el-icon><Close /></el-icon>
                关闭
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import {computed, nextTick, onMounted, onUnmounted, ref, watch} from 'vue'
import {ElMessage} from 'element-plus'
import {Bell, Box, Check, CircleClose, Clock, Coin, Connection, List, Monitor, View, Warning} from '@element-plus/icons-vue'
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
const activeDataTab = ref('monitor')
const chartRef = ref<HTMLDivElement | null>(null)
let chartInstance: echarts.ECharts | null = null

const timelineData = ref([
  { time: '2025-01-15 10:30:00', description: '告警触发', type: 'trigger' },
  { time: '2025-01-15 10:31:00', description: '系统自动响应', type: 'system' },
  { time: '2025-01-15 10:35:00', description: '值班人员确认', type: 'confirm' }
])

const generateChartData = () => {
  const alarmTime = props.data?.firstAlarmTime || new Date().toISOString()
  const alarmDate = new Date(alarmTime)
  const data: { time: string; value: number; isAlarm: boolean }[] = []
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
      
      data.push({ time: timeStr, value: Math.round(value * 10) / 10, isAlarm })
      
      const deduceBase = baseValue + (i > 0 ? i * 3 : 0)
      const deduceIsAlarm = i === 0 && hour >= 8 && hour <= 16 && Math.random() > 0.65
      const deduceValue = deduceIsAlarm ? deduceBase + 18 + Math.random() * 18 : deduceBase + Math.random() * 10
      deduceData.push({ time: timeStr, value: Math.round(deduceValue * 10) / 10, isAlarm: deduceIsAlarm })
    }
  }
  
  return { data, deduceData }
}

const initChart = () => {
  if (!chartRef.value) return
  
  if (chartInstance) {
    chartInstance.dispose()
  }
  
  chartInstance = echarts.init(chartRef.value)
  updateChart()
}

const updateChart = () => {
  if (!chartInstance) return
  
  const { data, deduceData } = generateChartData()
  const chartData = activeDataTab.value === 'monitor' ? data : deduceData
  
  const alarmPoints = chartData.filter(item => item.isAlarm)
  const alarmIndices = alarmPoints.map(p => chartData.indexOf(p))
  
  const option: echarts.EChartsOption = {
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        const data = params[0]
        const isAlarm = alarmIndices.includes(data.dataIndex)
        return `<div style="padding: 8px;">
          <div style="font-weight: bold; margin-bottom: 4px;">${data.name}</div>
          <div>数值: <span style="color: ${isAlarm ? '#f56c6c' : '#409eff'}">${data.value}</span></div>
          ${isAlarm ? '<div style="color: #f56c6c; margin-top: 4px;">⚠️ 告警数据点</div>' : ''}
        </div>`
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '15%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: chartData.map(item => item.time),
      axisLabel: {
        rotate: 45,
        fontSize: 10,
        color: '#666'
      },
      axisLine: {
        lineStyle: { color: '#ddd' }
      }
    },
    yAxis: {
      type: 'value',
      name: '监测值',
      axisLabel: {
        fontSize: 10,
        color: '#666'
      },
      axisLine: {
        lineStyle: { color: '#ddd' }
      },
      splitLine: {
        lineStyle: { color: '#f0f0f0' }
      }
    },
    series: [
      {
        name: activeDataTab.value === 'monitor' ? '监测数据' : '推演数据',
        type: 'line',
        data: chartData.map(item => item.value),
        smooth: true,
        symbol: 'circle',
        symbolSize: (value: number, params: any) => {
          return alarmIndices.includes(params.dataIndex) ? 10 : 6
        },
        itemStyle: {
          color: (params: any) => {
            return alarmIndices.includes(params.dataIndex) ? '#f56c6c' : '#409eff'
          }
        },
        lineStyle: {
          width: 2,
          color: '#409eff'
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(64, 158, 255, 0.3)' },
            { offset: 1, color: 'rgba(64, 158, 255, 0.05)' }
          ])
        },
        markPoint: {
          data: alarmPoints.map((point, index) => ({
            name: `告警点${index + 1}`,
            coord: [chartData.indexOf(point), point.value],
            value: point.value,
            itemStyle: {
              color: '#f56c6c'
            },
            symbol: 'pin',
            symbolSize: 40,
            label: {
              show: true,
              formatter: '⚠️',
              fontSize: 12
            }
          }))
        }
      }
    ],
    dataZoom: [
      {
        type: 'inside',
        start: 0,
        end: 100
      },
      {
        type: 'slider',
        show: true,
        start: 0,
        end: 100,
        height: 20,
        bottom: 0
      }
    ]
  }
  
  chartInstance.setOption(option, true)
}

const handleResize = () => {
  chartInstance?.resize()
}

watch(() => props.modelValue, async (visible) => {
  if (visible) {
    await nextTick()
    initChart()
  }
})

onMounted(() => {
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  chartInstance?.dispose()
})

const dialogTitle = computed(() => {
  if (!props.data) return '告警反馈'
  return `${props.data.hazardPointName}[${props.data.firstAlarmTime}]`
})

const formatDuration = (startTime: string) => {
  if (!startTime) return '0小时0分0秒'
  const start = new Date(startTime).getTime()
  const now = new Date().getTime()
  const diff = now - start
  const hours = Math.floor(diff / (1000 * 60 * 60))
  const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60))
  const seconds = Math.floor((diff % (1000 * 60)) / 1000)
  return `${hours}小时${minutes}分${seconds}秒前`
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

const handleFeedback = () => {
  emit('submit')
}

const handleFalseAlarm = () => {
  emit('false-alarm', props.data)
}

const handleCloseAlarm = () => {
  emit('close-alarm', props.data)
}

const handleNotify = () => {
  emit('notify', props.data)
  ElMessage.info('通知功能已触发')
}

const handleClose = () => {
  emit('update:modelValue', false)
  emit('close')
}
</script>

<style scoped>
.feedback-container {
  min-height: 500px;
}

.main-content {
  display: flex;
  gap: 16px;
}

.left-section {
  width: 140px;
  background: #f8f9fa;
  border-radius: 8px;
  padding: 12px;
  border: 1px solid #e9ecef;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #e9ecef;
}

.icon-wrapper {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #e8f4fd;
  border-radius: 6px;
  color: #409eff;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.lifecycle-list {
  display: flex;
  flex-direction: column;
}

.lifecycle-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 6px 0;
}

.node-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.lifecycle-node {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #adb5bd;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 12px;
  transition: all 0.3s ease;
}

.lifecycle-node.alarm {
  background: #f56c6c;
}

.lifecycle-item.active .lifecycle-node {
  transform: scale(1.15);
  box-shadow: 0 0 10px rgba(245, 108, 108, 0.5);
}

.lifecycle-line {
  width: 2px;
  height: 14px;
  background: #dee2e6;
  margin: 4px 0;
}

.node-label {
  font-size: 11px;
  color: #6c757d;
  margin-top: 4px;
}

.right-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.event-header {
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e9ecef;
}

.header-card {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  padding: 16px;
}

.header-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.item-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
}

.item-icon.hazard {
  background: #e8f5e9;
  color: #28a745;
}

.item-icon.device {
  background: #e3f2fd;
  color: #1976d2;
}

.item-icon.level {
  background: #fff3e0;
  color: #ff9800;
}

.item-icon.level.danger {
  background: #fee2e2;
  color: #dc3545;
}

.item-icon.level.warning {
  background: #fff3cd;
  color: #ffc107;
}

.item-icon.level.success {
  background: #d4edda;
  color: #28a745;
}

.item-icon.level.info {
  background: #d1ecf1;
  color: #17a2b8;
}

.item-icon.time {
  background: #f3e8ff;
  color: #6f42c1;
}

.item-content {
  display: flex;
  flex-direction: column;
}

.item-label {
  font-size: 11px;
  color: #909399;
  margin-bottom: 2px;
}

.item-value {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.item-value.level-value {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  color: #fff;
  font-size: 13px;
}

.item-value.level-value.danger {
  background: #f56c6c;
}

.item-value.level-value.warning {
  background: #e6a23c;
}

.item-value.level-value.success {
  background: #67c23a;
}

.item-value.level-value.info {
  background: #409eff;
}

.event-body {
  display: flex;
  gap: 12px;
}

.data-section {
  flex: 1;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e9ecef;
  padding: 12px;
}

.basic-info {
  margin-bottom: 12px;
}

.info-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}

.info-item {
  background: #f8f9fa;
  border-radius: 6px;
  padding: 8px;
}

.info-label {
  font-size: 11px;
  color: #909399;
  display: block;
  margin-bottom: 4px;
}

.info-value {
  font-size: 13px;
  font-weight: 500;
  color: #303133;
}

.basic-details {
  margin-bottom: 12px;
}

.detail-title {
  font-size: 12px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
  padding-bottom: 6px;
  border-bottom: 1px solid #f0f0f0;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  margin-bottom: 8px;
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.detail-label {
  font-size: 11px;
  color: #909399;
}

.detail-value {
  font-size: 12px;
  color: #303133;
}

.detail-desc {
  background: #f8f9fa;
  border-radius: 6px;
  padding: 8px;
}

.detail-desc label {
  font-size: 11px;
  color: #909399;
  display: block;
  margin-bottom: 4px;
}

.detail-desc p {
  font-size: 12px;
  color: #606266;
  line-height: 1.5;
  margin: 0;
}

.monitor-data {
  background: #f8f9fa;
  border-radius: 6px;
  padding: 8px;
}

.data-tabs {
  display: flex;
  gap: 12px;
  margin-bottom: 8px;
}

.data-tabs .tab {
  font-size: 12px;
  color: #606266;
  padding: 4px 12px;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
}

.data-tabs .tab.active {
  background: #409eff;
  color: #fff;
}

.data-content {
  height: 250px;
}

.chart-container {
  width: 100%;
  height: 100%;
}

.data-placeholder {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #909399;
}

.placeholder-icon {
  font-size: 24px;
  margin-bottom: 4px;
}

.data-placeholder p {
  font-size: 12px;
  margin: 0;
}

.timeline-section {
  width: 220px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e9ecef;
  padding: 12px;
}

.timeline-container {
  min-height: 150px;
}

.timeline {
  position: relative;
  padding-left: 14px;
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
  left: -14px;
  top: 2px;
  width: 10px;
  height: 10px;
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
  left: -10px;
  top: 12px;
  width: 2px;
  height: calc(100% - 8px);
  background: #d9d9d9;
}

.timeline-content {
  padding-left: 8px;
}

.timeline-time {
  font-size: 11px;
  color: #909399;
  margin-bottom: 2px;
}

.timeline-desc {
  font-size: 12px;
  color: #303133;
}

.timeline-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 120px;
  color: #909399;
}

.empty-icon {
  font-size: 24px;
  margin-bottom: 6px;
}

.timeline-empty p {
  font-size: 12px;
  margin: 0;
}

.event-footer {
  display: flex;
  gap: 12px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e9ecef;
  padding: 12px;
}

.action-left {
  flex: 1;
}

.remark-input {
  width: 100%;
  resize: none;
}

.remark-input :deep(.el-textarea__inner) {
  font-size: 12px;
  padding: 8px;
}

.action-right {
  display: flex;
  gap: 8px;
  align-items: flex-end;
}

.action-right :deep(.el-button) {
  padding: 6px 12px;
}

@media (max-width: 900px) {
  .header-card {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .event-body {
    flex-direction: column;
  }
  
  .timeline-section {
    width: 100%;
  }
  
  .info-row {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .detail-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
