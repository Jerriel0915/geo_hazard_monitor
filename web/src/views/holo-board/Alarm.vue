<template>
  <div class="alarm-stats-view">
    <div class="stats-header">
      <div class="stat-card">
        <div class="stat-icon">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="#ef4444" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"
               width="20" height="20">
            <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9z"/>
            <line x1="12" y1="4" x2="12" y2="8"/>
          </svg>
        </div>
        <div class="stat-content">
          <span class="stat-value">{{ alarmStats.recentThreeMonthsAlarms }}</span>
          <span class="stat-label">近三月告警次数</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="#3b82f6" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"
               width="20" height="20">
            <circle cx="12" cy="12" r="10"/>
            <line x1="12" y1="8" x2="12" y2="12"/>
            <line x1="12" y1="16" x2="12.01" y2="16"/>
          </svg>
        </div>
        <div class="stat-content">
          <span class="stat-value highlight">{{ alarmStats.totalAlarms }}</span>
          <span class="stat-label">累计告警次数</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="#10b981" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"
               width="20" height="20">
            <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
          </svg>
        </div>
        <div class="stat-content">
          <span class="stat-value">{{ alarmStats.hazardPointCount }}</span>
          <span class="stat-label">关联隐患点</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="#8b5cf6" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"
               width="20" height="20">
            <rect x="2" y="3" width="20" height="14" rx="2" ry="2"/>
            <line x1="8" y1="21" x2="16" y2="21"/>
            <line x1="12" y1="17" x2="12" y2="21"/>
          </svg>
        </div>
        <div class="stat-content">
          <span class="stat-value">{{ alarmStats.deviceCount }}</span>
          <span class="stat-label">关联设备</span>
        </div>
      </div>
    </div>

    <div class="charts-row">
      <div class="chart-panel alarm-level">
        <div class="panel-header">
          <span class="panel-title">告警等级分布</span>
        </div>
        <div class="panel-body">
          <div ref="levelChartRef" class="echarts-container"></div>
        </div>
      </div>

      <div class="chart-panel alarm-trend">
        <div class="panel-header">
          <span class="panel-title">告警趋势分析</span>
          <span class="panel-subtitle">近12个月告警统计及未来预测</span>
        </div>
        <div class="panel-body">
          <div ref="trendChartRef" class="echarts-container echarts-container-trend"></div>
        </div>
      </div>
    </div>

    <div class="charts-row">
      <div class="chart-panel alarm-source">
        <div class="panel-header">
          <span class="panel-title">告警来源分布</span>
        </div>
        <div class="panel-body">
          <div ref="sourceChartRef" class="echarts-container echarts-container-lg"></div>
        </div>
      </div>

      <div class="chart-panel alarm-hazard">
        <div class="panel-header">
          <span class="panel-title">高风险隐患点</span>
        </div>
        <div class="panel-body">
          <div ref="hazardChartRef" class="echarts-container echarts-container-lg"></div>
        </div>
      </div>
    </div>

    <div class="alarm-list-section">
      <div class="list-header">
        <span class="list-title">实时告警事件</span>
        <span class="refresh-time">下次刷新时间：{{ nextRefreshTime }}</span>
      </div>
      <div class="alarm-list">
        <div v-for="alarm in alarmStats.recentAlarms" :key="alarm.id" class="alarm-item">
          <div class="alarm-level-dot" :class="alarm.level"></div>
          <div class="alarm-content">
            <div class="alarm-title">{{ alarm.title }}</div>
            <div class="alarm-meta">
              <span>{{ alarm.source }}</span>
              <span>·</span>
              <span>{{ alarm.time }}</span>
              <span v-if="alarm.priority" class="alarm-priority" :class="`priority-${alarm.priority}`">
                {{ alarm.priority === 1 ? '紧急' : alarm.priority === 2 ? '重要' : '一般' }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {onMounted, onUnmounted, ref} from 'vue'
import * as echarts from 'echarts'

const alarmStats = ref({
  recentThreeMonthsAlarms: 156,
  totalAlarms: 892,
  hazardPointCount: 28,
  deviceCount: 156,
  levelStats: [
    { name: '一级告警', key: 'level1', count: 112, rate: 12.6 },
    { name: '二级告警', key: 'level2', count: 205, rate: 23.0 },
    { name: '三级告警', key: 'level3', count: 318, rate: 35.6 },
    { name: '四级告警', key: 'level4', count: 257, rate: 28.8 }
  ],
  recentAlarms: [
    {
      id: 1,
      title: '边坡位移超阈值告警',
      source: 'GNSS监测点-A1',
      time: '2026-05-28 14:23',
      level: 'level1',
      priority: 1
    },
    {
      id: 2,
      title: '渗压异常告警',
      source: '渗压计-B3',
      time: '2026-05-28 13:45',
      level: 'level2',
      priority: 2
    },
    {
      id: 3,
      title: '雨量超限告警',
      source: '雨量计-C5',
      time: '2026-05-28 12:15',
      level: 'level3',
      priority: 3
    },
    {
      id: 4,
      title: '设备通讯中断',
      source: '裂缝计-D2',
      time: '2026-05-28 11:30',
      level: 'level4',
      priority: 3
    },
    {
      id: 5,
      title: '地下水位异常',
      source: '水位计-E7',
      time: '2026-05-28 10:05',
      level: 'level2',
      priority: 2
    }
  ]
})

const nextRefreshTime = ref('')
const levelChartRef = ref<HTMLDivElement | null>(null)
const trendChartRef = ref<HTMLDivElement | null>(null)
const sourceChartRef = ref<HTMLDivElement | null>(null)
const hazardChartRef = ref<HTMLDivElement | null>(null)

let levelChartInstance: echarts.ECharts | null = null
let trendChartInstance: echarts.ECharts | null = null
let sourceChartInstance: echarts.ECharts | null = null
let hazardChartInstance: echarts.ECharts | null = null

const updateNextRefreshTime = () => {
  const now = new Date()
  now.setMinutes(now.getMinutes() + 5)
  nextRefreshTime.value = formatTime(now)
}

const formatTime = (date: Date) => {
  const year = date.getFullYear()
  const month = (date.getMonth() + 1).toString().padStart(2, '0')
  const day = date.getDate().toString().padStart(2, '0')
  const hours = date.getHours().toString().padStart(2, '0')
  const minutes = date.getMinutes().toString().padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}`
}

const alarmTrendData = ref({
  months: ['2025-06', '2025-07', '2025-08', '2025-09', '2025-10', '2025-11', '2025-12', '2026-01', '2026-02', '2026-03', '2026-04', '2026-05'],
  level1: [5, 8, 12, 10, 15, 12, 14, 16, 12, 14, 10, 12],
  level2: [8, 12, 15, 14, 18, 15, 17, 20, 16, 18, 14, 15],
  level3: [3, 5, 7, 6, 8, 6, 7, 8, 6, 7, 5, 6],
  level4: [2, 3, 4, 3, 5, 4, 4, 5, 4, 4, 3, 4],
  total: [18, 28, 38, 33, 46, 37, 42, 49, 38, 43, 32, 37],
  forecastTotal: [35, 38],
  forecastLevel1: [11, 12],
  forecastLevel2: [16, 17],
  forecastLevel3: [6, 6],
  forecastLevel4: [4, 4]
})

const hazardData = ref([
  { name: '边坡A', count: 12, level: 'level1' },
  { name: '桥梁B', count: 8, level: 'level2' },
  { name: '隧道C', count: 5, level: 'level2' },
  { name: '路基D', count: 3, level: 'level3' }
])

const sourceDistribution = ref([
  { name: 'GNSS', count: 320, rate: 35.9 },
  { name: '裂缝计', count: 185, rate: 20.7 },
  { name: '渗压计', count: 156, rate: 17.5 },
  { name: '雨量计', count: 128, rate: 14.3 },
  { name: '水位计', count: 103, rate: 11.6 }
])

onMounted(() => {
  initLevelChart()
  initTrendChart()
  initSourceChart()
  initHazardChart()
  updateNextRefreshTime()

  // 开始周期性更新
  startAutoRefresh()
})

onUnmounted(() => {
  // 清理图表实例
  levelChartInstance?.dispose()
  trendChartInstance?.dispose()
  sourceChartInstance?.dispose()
  hazardChartInstance?.dispose()

  // 停止周期性更新
  stopAutoRefresh()
})

// 初始化告警等级分布图表
const initLevelChart = () => {
  if (!levelChartRef.value) return
  levelChartInstance = echarts.init(levelChartRef.value)

  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{a}<br/>{b}: {c}次 (占{d}%)'
    },
    legend: {
      orient: 'vertical',
      right: '5%',
      top: 'center',
      textStyle: {
        color: '#334155',
        fontSize: 14
      },
      itemWidth: 12,
      itemHeight: 12,
      itemGap: 8
    },
    series: [{
      name: '告警等级',
      type: 'pie',
      radius: ['40%', '65%'],
      center: ['35%', '50%'],
      avoidLabelOverlap: true,
      itemStyle: {
        borderRadius: 4,
        borderColor: '#ffffff',
        borderWidth: 2
      },
      label: {
        show: true,
        position: 'outside',
        formatter: '{b}\n{c}次 ({d}%)',
        color: '#334155',
        fontSize: 14
      },
      labelLine: {
        show: true,
        lineStyle: {
          color: '#94a3b8'
        }
      },
      data: alarmStats.value.levelStats.map(item => ({
        name: item.name,
        value: item.count,
        itemStyle: { color: getLevelColor(item.key) }
      }))
    }]
  }

  levelChartInstance.setOption(option)
}

// 初始化告警趋势图表
const initTrendChart = () => {
  if (!trendChartRef.value) return
  trendChartInstance = echarts.init(trendChartRef.value)

  const option = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#e2e8f0',
      borderWidth: 1,
      textStyle: {
        color: '#374151'
      },
      extraCssText: 'box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);'
    },
    legend: {
      data: ['一级告警', '二级告警', '三级告警', '四级告警', '合计', '预测一级', '预测二级', '预测三级', '预测四级', '预测合计'],
      bottom: 0,
      textStyle: {
        color: '#6b7280',
        fontSize: 12
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '18%',
      top: '5%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: [...alarmTrendData.value.months, '2026-06', '2026-07'],
      axisLine: {
        lineStyle: {
          color: '#e2e8f0'
        }
      },
      axisLabel: {
        color: '#6b7280',
        fontSize: 12
      }
    },
    yAxis: {
      type: 'value',
      name: '告警次数',
      nameTextStyle: {
        color: '#6b7280',
        fontSize: 12
      },
      axisLine: {
        show: false
      },
      axisLabel: {
        color: '#6b7280',
        fontSize: 12
      },
      splitLine: {
        lineStyle: {
          color: '#f3f4f6'
        }
      }
    },
    series: [
      {
        name: '一级告警',
        type: 'line',
        smooth: true,
        color: '#ef4444',
        data: [...alarmTrendData.value.level1, null, null]
      },
      {
        name: '二级告警',
        type: 'line',
        smooth: true,
        color: '#f97316',
        data: [...alarmTrendData.value.level2, null, null]
      },
      {
        name: '三级告警',
        type: 'line',
        smooth: true,
        color: '#eab308',
        data: [...alarmTrendData.value.level3, null, null]
      },
      {
        name: '四级告警',
        type: 'line',
        smooth: true,
        color: '#22c55e',
        data: [...alarmTrendData.value.level4, null, null]
      },
      {
        name: '合计',
        type: 'line',
        smooth: true,
        color: '#3b82f6',
        lineStyle: {
          width: 3
        },
        data: [...alarmTrendData.value.total, null, null]
      },
      {
        name: '预测一级',
        type: 'line',
        smooth: true,
        color: '#ef4444',
        lineStyle: {
          width: 2,
          type: 'dashed'
        },
        symbol: 'diamond',
        symbolSize: 6,
        data: [null, null, null, null, null, null, null, null, null, null, null, alarmTrendData.value.level1[11], ...alarmTrendData.value.forecastLevel1]
      },
      {
        name: '预测二级',
        type: 'line',
        smooth: true,
        color: '#f97316',
        lineStyle: {
          width: 2,
          type: 'dashed'
        },
        symbol: 'diamond',
        symbolSize: 6,
        data: [null, null, null, null, null, null, null, null, null, null, null, alarmTrendData.value.level2[11], ...alarmTrendData.value.forecastLevel2]
      },
      {
        name: '预测三级',
        type: 'line',
        smooth: true,
        color: '#eab308',
        lineStyle: {
          width: 2,
          type: 'dashed'
        },
        symbol: 'diamond',
        symbolSize: 6,
        data: [null, null, null, null, null, null, null, null, null, null, null, alarmTrendData.value.level3[11], ...alarmTrendData.value.forecastLevel3]
      },
      {
        name: '预测四级',
        type: 'line',
        smooth: true,
        color: '#22c55e',
        lineStyle: {
          width: 2,
          type: 'dashed'
        },
        symbol: 'diamond',
        symbolSize: 6,
        data: [null, null, null, null, null, null, null, null, null, null, null, alarmTrendData.value.level4[11], ...alarmTrendData.value.forecastLevel4]
      },
      {
        name: '预测合计',
        type: 'line',
        smooth: true,
        color: '#3b82f6',
        lineStyle: {
          width: 2,
          type: 'dashed'
        },
        symbol: 'diamond',
        symbolSize: 6,
        data: [null, null, null, null, null, null, null, null, null, null, null, alarmTrendData.value.total[11], ...alarmTrendData.value.forecastTotal]
      }
    ]
  }

  trendChartInstance.setOption(option)
}

// 初始化告警来源分布图表
const initSourceChart = () => {
  if (!sourceChartRef.value) return
  sourceChartInstance = echarts.init(sourceChartRef.value)

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      },
      formatter: (params: any) => {
        const data = params[0]
        return `${data.name}<br/>数量: ${data.value}次 (${data.percent}%)`
      }
    },
    grid: {
      left: '8%',
      right: '4%',
      bottom: '15%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: sourceDistribution.value.map(item => item.name),
      axisLabel: {
        color: '#64748b',
        fontSize: 14
      },
      axisLine: {
        lineStyle: {
          color: '#e2e8f0'
        }
      }
    },
    yAxis: {
      type: 'value',
      name: '告警次数',
      nameTextStyle: {
        color: '#6b7280',
        fontSize: 14
      },
      axisLabel: {
        color: '#6b7280',
        fontSize: 14
      },
      splitLine: {
        lineStyle: {
          color: '#f1f5f9'
        }
      }
    },
    series: [{
      name: '告警次数',
      type: 'bar',
      data: sourceDistribution.value.map((item, index) => ({
        value: item.count,
        percent: item.rate,
        itemStyle: { color: getSourceColor(index) }
      })),
      barWidth: '50%',
      label: {
        show: true,
        position: 'top',
        formatter: '{c}次 ({d}%)',
        color: '#1e293b',
        fontSize: 14,
        fontWeight: 600
      }
    }]
  }

  sourceChartInstance.setOption(option)
}

// 初始化高风险隐患点图表
const initHazardChart = () => {
  if (!hazardChartRef.value) return
  hazardChartInstance = echarts.init(hazardChartRef.value)

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      },
      formatter: (params: any) => {
        const data = params[0]
        return `${data.name}<br/>告警次数: ${data.value}次`
      }
    },
    grid: {
      left: '8%',
      right: '4%',
      bottom: '15%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: hazardData.value.map(item => item.name),
      axisLabel: {
        color: '#64748b',
        fontSize: 14
      },
      axisLine: {
        lineStyle: {
          color: '#e2e8f0'
        }
      }
    },
    yAxis: {
      type: 'value',
      name: '告警次数',
      nameTextStyle: {
        color: '#6b7280',
        fontSize: 14
      },
      axisLabel: {
        color: '#6b7280',
        fontSize: 14
      },
      splitLine: {
        lineStyle: {
          color: '#f1f5f9'
        }
      }
    },
    series: [{
      name: '告警次数',
      type: 'bar',
      data: hazardData.value.map((item, index) => ({
        value: item.count,
        itemStyle: { color: getLevelColor(item.level) }
      })),
      barWidth: '50%',
      label: {
        show: true,
        position: 'top',
        formatter: '{c}次',
        color: '#1e293b',
        fontSize: 14,
        fontWeight: 600
      }
    }]
  }

  hazardChartInstance.setOption(option)
}

// 获取告警等级颜色
const getLevelColor = (level: string) => {
  switch (level) {
    case 'level1': return '#ef4444'
    case 'level2': return '#f97316'
    case 'level3': return '#eab308'
    case 'level4': return '#22c55e'
    default: return '#64748b'
  }
}

// 获取告警来源颜色
const getSourceColor = (index: number) => {
  const colors = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6']
  return colors[index % colors.length]
}

// 周期性更新数据
const refreshInterval = ref<number | null>(null)
const startAutoRefresh = () => {
  stopAutoRefresh()
  refreshInterval.value = window.setInterval(() => {
    // 模拟数据更新，实际应用中应从API获取最新数据
    updateAlarmStats()

    // 更新图表
    levelChartInstance?.setOption({
      series: [{
        data: alarmStats.value.levelStats.map(item => ({
          name: item.name,
          value: item.count,
          itemStyle: { color: getLevelColor(item.key) }
        }))
      }]
    })

    trendChartInstance?.setOption({
      series: [
        { data: [...alarmTrendData.value.level1, null, null] },
        { data: [...alarmTrendData.value.level2, null, null] },
        { data: [...alarmTrendData.value.level3, null, null] },
        { data: [...alarmTrendData.value.level4, null, null] },
        { data: [...alarmTrendData.value.total, null, null] },
        { data: [null, null, null, null, null, null, null, null, null, null, null, alarmTrendData.value.level1[11], ...alarmTrendData.value.forecastLevel1] },
        { data: [null, null, null, null, null, null, null, null, null, null, null, alarmTrendData.value.level2[11], ...alarmTrendData.value.forecastLevel2] },
        { data: [null, null, null, null, null, null, null, null, null, null, null, alarmTrendData.value.level3[11], ...alarmTrendData.value.forecastLevel3] },
        { data: [null, null, null, null, null, null, null, null, null, null, null, alarmTrendData.value.level4[11], ...alarmTrendData.value.forecastLevel4] },
        { data: [null, null, null, null, null, null, null, null, null, null, null, alarmTrendData.value.total[11], ...alarmTrendData.value.forecastTotal] }
      ]
    })

    sourceChartInstance?.setOption({
      series: [{
        data: sourceDistribution.value.map((item, index) => ({
          value: item.count,
          percent: item.rate,
          itemStyle: { color: getSourceColor(index) }
        }))
      }]
    })

    hazardChartInstance?.setOption({
      series: [{
        data: hazardData.value.map(item => ({
          value: item.count,
          itemStyle: { color: getLevelColor(item.level) }
        }))
      }]
    })

    updateNextRefreshTime()
  }, 5 * 60 * 1000) // 每5分钟更新一次
}

// 停止周期性更新
const stopAutoRefresh = () => {
  if (refreshInterval.value) {
    clearInterval(refreshInterval.value)
    refreshInterval.value = null
  }
}

// 模拟更新告警统计数据
const updateAlarmStats = () => {
  // 实际应用中应从API获取最新数据
  // 这里仅做模拟更新
  const baseAlarms = 892
  const randomIncrease = Math.floor(Math.random() * 3) + 1
  alarmStats.value.totalAlarms = baseAlarms + randomIncrease

  // 更新等级分布
  const total = alarmStats.value.levelStats.reduce((sum, item) => sum + item.count, 0)
  alarmStats.value.levelStats = [
    { name: '一级告警', key: 'level1', count: Math.floor((baseAlarms + randomIncrease) * 0.12), rate: 12.0 },
    { name: '二级告警', key: 'level2', count: Math.floor((baseAlarms + randomIncrease) * 0.23), rate: 23.0 },
    { name: '三级告警', key: 'level3', count: Math.floor((baseAlarms + randomIncrease) * 0.35), rate: 35.0 },
    { name: '四级告警', key: 'level4', count: Math.floor((baseAlarms + randomIncrease) * 0.29), rate: 29.0 }
  ]
}

// 处理窗口大小变化
const handleResize = () => {
  levelChartInstance?.resize()
  trendChartInstance?.resize()
  sourceChartInstance?.resize()
  hazardChartInstance?.resize()
}

// 添加窗口大小变化监听
window.addEventListener('resize', handleResize)
</script>

<style scoped>
.alarm-stats-view {
  min-height: 100%;
  background: transparent;
  padding: 20px 0 0 0;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.stats-header {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}

.stat-card {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.stat-icon {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 212, 255, 0.1);
  border-radius: 8px;
}

.stat-content {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 16px;
  font-weight: 700;
  color: #1e293b;
  transition: all 0.3s ease;
}

.stat-value.highlight {
  color: #3b82f6;
}

.stat-label {
  font-size: 14px;
  color: #64748b;
  margin-top: 2px;
}

.charts-row {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}

.chart-panel {
  flex: 1;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  transition: all 0.3s ease;
}

.panel-header {
  padding: 10px 14px;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
}

.panel-title {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
}

.panel-subtitle {
  font-size: 14px;
  color: #64748b;
  margin-left: 8px;
}

.panel-body {
  padding: 12px;
}

.alarm-level .panel-body {
  padding-top: 42px;
}

.echarts-container {
  width: 100%;
  height: 180px;
}


.echarts-container-lg {
  height: 310px; /*  用来控制两个板块的高度 */
}

.echarts-container-trend {
  height: 260px;
}

.alarm-list-section {
  margin-top: 12px;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 14px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px 8px 0 0;
  border-bottom: none;
}

.list-title {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
}

.refresh-time {
  font-size: 14px;
  color: #64748b;
}

.alarm-list {
  border: 1px solid #e2e8f0;
  border-top: none;
  border-radius: 0 0 8px 8px;
  background: #ffffff;
}

.alarm-item {
  display: flex;
  padding: 12px 16px;
  border-bottom: 1px solid #f1f5f9;
  transition: background 0.2s ease;
}

.alarm-item:last-child {
  border-bottom: none;
}

.alarm-item:hover {
  background: #f8fafc;
}

.alarm-level-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 12px;
  align-self: center;
}

.alarm-level-dot.level1 {
  background: #ef4444;
}

.alarm-level-dot.level2 {
  background: #f97316;
}

.alarm-level-dot.level3 {
  background: #eab308;
}

.alarm-level-dot.level4 {
  background: #22c55e;
}

.alarm-content {
  display: flex;
  flex-direction: column;
}

.alarm-title {
  font-size: 14px;
  color: #1e293b;
  margin-bottom: 4px;
}

.alarm-meta {
  display: flex;
  font-size: 14px;
  color: #64748b;
}

.alarm-meta .alarm-priority {
  margin-left: 8px;
}

.alarm-meta .priority-1 {
  color: #ef4444;
  font-weight: 600;
  background: #fef2f2;
  padding: 2px 6px;
  border-radius: 4px;
}

.alarm-meta .priority-2 {
  color: #f97316;
  font-weight: 600;
  background: #fff7ed;
  padding: 2px 6px;
  border-radius: 4px;
}

.alarm-meta .priority-3 {
  color: #64748b;
  font-weight: 400;
  background: #f1f5f9;
  padding: 2px 6px;
  border-radius: 4px;
}
</style>