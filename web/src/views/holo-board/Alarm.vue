<template>
  <div class="alarm-view">
    <div class="alarm-header">
      <div class="header-left">
        <div class="stat-card-inline red">
          <div class="stat-icon">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" width="16" height="16">
              <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/>
              <path d="M13.73 21a2 2 0 0 1-3.46 0"/>
            </svg>
          </div>
          <span class="stat-label">红色告警</span>
          <span class="stat-value">{{ alarmStats.red }}</span>
          <span class="stat-unit">处</span>
        </div>
        <div class="stat-card-inline orange">
          <div class="stat-icon">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" width="16" height="16">
              <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/>
              <path d="M13.73 21a2 2 0 0 1-3.46 0"/>
            </svg>
          </div>
          <span class="stat-label">橙色告警</span>
          <span class="stat-value">{{ alarmStats.orange }}</span>
          <span class="stat-unit">处</span>
        </div>
      </div>
      <div class="header-center">
        <div class="header-title">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="#f5222d" stroke-width="2" width="20" height="20">
            <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/>
            <path d="M13.73 21a2 2 0 0 1-3.46 0"/>
          </svg>
          <span>告警模型告警统计</span>
          <span class="header-subtitle">
            <span class="arrow-icon">↑</span>
            <span>今日新增</span>
          </span>
        </div>
      </div>
      <div class="header-right">
        <div class="stat-card-inline yellow">
          <div class="stat-icon">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" width="16" height="16">
              <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/>
              <path d="M13.73 21a2 2 0 0 1-3.46 0"/>
            </svg>
          </div>
          <span class="stat-label">黄色告警</span>
          <span class="stat-value">{{ alarmStats.yellow }}</span>
          <span class="stat-unit">处</span>
        </div>
        <div class="stat-card-inline blue">
          <div class="stat-icon">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" width="16" height="16">
              <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/>
              <path d="M13.73 21a2 2 0 0 1-3.46 0"/>
            </svg>
          </div>
          <span class="stat-label">蓝色告警</span>
          <span class="stat-value">{{ alarmStats.blue }}</span>
          <span class="stat-unit">处</span>
        </div>
      </div>
    </div>

    <div class="alarm-content">
      <div class="content-left">
        <div class="panel monthly-trend">
          <div class="panel-header">
            <span class="panel-title">近一个月新增情况</span>
          </div>
          <div class="panel-body">
            <div ref="chartRef" class="echarts-container"></div>
          </div>
        </div>

        <div class="panel alarm-list">
          <div class="panel-header">
            <span class="panel-title">隐患点告警列表</span>
          </div>
          <div class="panel-body">
            <table class="alarm-table">
              <thead>
                <tr>
                  <th>隐患点名称</th>
                  <th>告警时间</th>
                  <th>告警等级</th>
                  <th>是否响应</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="alarm in alarmList" :key="alarm.id">
                  <td>{{ alarm.name }}</td>
                  <td>{{ alarm.time }}</td>
                  <td><span :class="['level-badge', alarm.level]">{{ alarm.levelText }}</span></td>
                  <td><span :class="['response-badge', alarm.responded ? 'yes' : 'no']">{{ alarm.responded ? '已响应' : '未响应' }}</span></td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <div class="content-right">
        <div class="panel summary-table">
          <div class="panel-header">
            <span class="panel-title">隐患点、告警等级一览表</span>
          </div>
          <div class="panel-body">
            <table class="summary-table-content">
              <thead>
                <tr>
                  <th>隐患点/告警等级</th>
                  <th>红色预警</th>
                  <th>橙色预警</th>
                  <th>黄色预警</th>
                  <th>蓝色预警</th>
                  <th>合计</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in summaryData" :key="item.name">
                  <td>{{ item.name }}</td>
                  <td>{{ item.red }}</td>
                  <td>{{ item.orange }}</td>
                  <td>{{ item.yellow }}</td>
                  <td>{{ item.blue }}</td>
                  <td>{{ item.total }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <div class="panel pie-chart-panel">
          <div class="panel-header">
            <span class="panel-title">预警等级统计</span>
          </div>
          <div class="panel-body">
            <div ref="pieChartRef" class="pie-chart-container"></div>
            <div class="pie-legend">
              <div class="legend-item"><span class="legend-dot red"></span><span>红色 {{ alarmStats.red }}</span></div>
              <div class="legend-item"><span class="legend-dot orange"></span><span>橙色 {{ alarmStats.orange }}</span></div>
              <div class="legend-item"><span class="legend-dot yellow"></span><span>黄色 {{ alarmStats.yellow }}</span></div>
              <div class="legend-item"><span class="legend-dot blue"></span><span>蓝色 {{ alarmStats.blue }}</span></div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, computed, ref, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'

const chartRef = ref<HTMLElement | null>(null)
let chartInstance: echarts.ECharts | null = null

const pieChartRef = ref<HTMLElement | null>(null)
let pieChartInstance: echarts.ECharts | null = null

const alarmStats = reactive({
  red: 3,
  orange: 6,
  yellow: 0,
  blue: 1
})

const totalAlarmCount = computed(() => {
  return alarmStats.red + alarmStats.orange + alarmStats.yellow + alarmStats.blue
})

const generateXAxisLabels = () => {
  const labels: string[] = []
  const today = new Date()
  for (let i = 29; i >= 0; i--) {
    const date = new Date(today)
    date.setDate(today.getDate() - i)
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    labels.push(`${month}-${day}`)
  }
  return labels
}

const xAxisLabels = generateXAxisLabels()

const generateRandomData = () => {
  const data: number[] = []
  for (let i = 0; i < 30; i++) {
    data.push(Math.floor(Math.random() * 100) + 10)
  }
  return data
}

const redData = generateRandomData()
const orangeData = generateRandomData()
const yellowData = generateRandomData()
const blueData = generateRandomData()
const totalData = redData.map((_, i) => redData[i] + orangeData[i] + yellowData[i] + blueData[i])

const initChart = () => {
  if (!chartRef.value) return
  
  chartInstance = echarts.init(chartRef.value)
  
  const option: echarts.EChartsOption = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross'
      }
    },
    legend: {
      right: '10%',
      top: '5%',
      orient: 'horizontal'
    },
    grid: {
      left: '3%',
      right: '15%',
      bottom: '8%',
      top: '15%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      name: '日期',
      nameLocation: 'middle',
      nameGap: 30,
      nameTextStyle: {
        fontSize: 12,
        color: '#64748b'
      },
      boundaryGap: false,
      data: xAxisLabels,
      axisLabel: {
        fontSize: 10,
        color: '#94a3b8'
      }
    },
    yAxis: {
      type: 'value',
      name: '告警数（次）',
      nameTextStyle: {
        fontSize: 12,
        color: '#64748b'
      },
      axisLabel: {
        fontSize: 11,
        color: '#94a3b8'
      }
    },
    series: [
      {
        name: '合计',
        type: 'line',
        data: totalData,
        smooth: true,
        symbol: 'circle',
        symbolSize: 4,
        lineStyle: {
          width: 3,
          type: 'dashed',
          color: '#475569'
        },
        itemStyle: {
          color: '#475569'
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(71, 85, 105, 0.3)' },
            { offset: 1, color: 'rgba(71, 85, 105, 0)' }
          ])
        }
      },
      {
        name: '红色告警',
        type: 'line',
        data: redData,
        smooth: true,
        symbol: 'circle',
        symbolSize: 3,
        lineStyle: {
          width: 2,
          color: '#dc2626'
        },
        itemStyle: {
          color: '#dc2626'
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(220, 38, 38, 0.2)' },
            { offset: 1, color: 'rgba(220, 38, 38, 0)' }
          ])
        }
      },
      {
        name: '橙色告警',
        type: 'line',
        data: orangeData,
        smooth: true,
        symbol: 'circle',
        symbolSize: 3,
        lineStyle: {
          width: 2,
          color: '#ea580c'
        },
        itemStyle: {
          color: '#ea580c'
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(234, 88, 12, 0.2)' },
            { offset: 1, color: 'rgba(234, 88, 12, 0)' }
          ])
        }
      },
      {
        name: '黄色告警',
        type: 'line',
        data: yellowData,
        smooth: true,
        symbol: 'circle',
        symbolSize: 3,
        lineStyle: {
          width: 2,
          color: '#ca8a04'
        },
        itemStyle: {
          color: '#ca8a04'
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(202, 138, 4, 0.2)' },
            { offset: 1, color: 'rgba(202, 138, 4, 0)' }
          ])
        }
      },
      {
        name: '蓝色告警',
        type: 'line',
        data: blueData,
        smooth: true,
        symbol: 'circle',
        symbolSize: 3,
        lineStyle: {
          width: 2,
          color: '#0284c7'
        },
        itemStyle: {
          color: '#0284c7'
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(2, 132, 199, 0.2)' },
            { offset: 1, color: 'rgba(2, 132, 199, 0)' }
          ])
        }
      }
    ]
  }
  
  chartInstance.setOption(option)
}

const initPieChart = () => {
  if (!pieChartRef.value) return
  
  pieChartInstance = echarts.init(pieChartRef.value)
  
  const option: echarts.EChartsOption = {
    tooltip: {
      trigger: 'item',
      formatter: (params: any) => {
        return `${params.name}<br/>告警数量: ${params.value} 次`
      }
    },
    series: [
      {
        name: '预警等级统计',
        type: 'pie',
        radius: '70%',
        center: ['50%', '50%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 4,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: false
        },
        emphasis: {
          label: {
            show: false
          }
        },
        labelLine: {
          show: false
        },
        data: [
          { value: alarmStats.red, name: '红色告警', itemStyle: { color: '#dc2626' } },
          { value: alarmStats.orange, name: '橙色告警', itemStyle: { color: '#ea580c' } },
          { value: alarmStats.yellow, name: '黄色告警', itemStyle: { color: '#ca8a04' } },
          { value: alarmStats.blue, name: '蓝色告警', itemStyle: { color: '#0284c7' } }
        ]
      }
    ]
  }
  
  pieChartInstance.setOption(option)
}

const handleResize = () => {
  chartInstance?.resize()
  pieChartInstance?.resize()
}

onMounted(() => {
  initChart()
  initPieChart()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  chartInstance?.dispose()
  pieChartInstance?.dispose()
})

const alarmList = [
  { id: 1, name: '龙潭寺滑坡点', time: '2024-06-21 14:30', level: 'red', levelText: '红色告警', responded: false },
  { id: 2, name: '大坝监测点', time: '2024-06-21 13:20', level: 'orange', levelText: '橙色告警', responded: true },
  { id: 3, name: '边坡监测点', time: '2024-06-21 11:15', level: 'yellow', levelText: '黄色告警', responded: true },
  { id: 4, name: '泥石流隐患点', time: '2024-06-21 09:45', level: 'red', levelText: '红色告警', responded: false },
  { id: 5, name: '地面沉降点', time: '2024-06-20 16:30', level: 'blue', levelText: '蓝色告警', responded: true },
  { id: 6, name: '桥梁监测点', time: '2024-06-20 14:20', level: 'orange', levelText: '橙色告警', responded: true }
]

const summaryData = [
  { name: '龙潭寺滑坡点', red: 2, orange: 3, yellow: 1, blue: 0, total: 6 },
  { name: '大坝监测点', red: 1, orange: 2, yellow: 2, blue: 1, total: 6 },
  { name: '边坡监测点', red: 0, orange: 1, yellow: 3, blue: 2, total: 6 },
  { name: '泥石流隐患点', red: 0, orange: 0, yellow: 1, blue: 3, total: 4 },
  { name: '地面沉降点', red: 0, orange: 0, yellow: 0, blue: 2, total: 2 }
]

const circumference = 2 * Math.PI * 70
const total = totalAlarmCount.value
const redPercent = alarmStats.red / total
const orangePercent = alarmStats.orange / total
const yellowPercent = alarmStats.yellow / total
const bluePercent = alarmStats.blue / total

const redDashArray = `${redPercent * circumference} ${circumference}`
const orangeDashArray = `${orangePercent * circumference} ${circumference}`
const yellowDashArray = `${yellowPercent * circumference} ${circumference}`
const blueDashArray = `${bluePercent * circumference} ${circumference}`

const dashOffset = 0
const orangeOffset = -redPercent * circumference
const yellowOffset = -(redPercent + orangePercent) * circumference
const blueOffset = -(redPercent + orangePercent + yellowPercent) * circumference
</script>

<style scoped>
.alarm-view {
  min-height: 100%;
  background: transparent;
  padding: 0;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.alarm-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding: 12px 20px;
  background: #ffffff;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.header-left {
  display: flex;
  gap: 20px;
}

.header-center {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
}

.header-right {
  display: flex;
  gap: 20px;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
}

.header-subtitle {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #52c41a;
  font-size: 12px;
  margin-left: 10px;
  padding: 3px 10px;
  background: rgba(82, 196, 26, 0.1);
  border-radius: 16px;
}

.arrow-icon {
  font-size: 14px;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.stat-card-inline {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border-radius: 6px;
}

.stat-card-inline.red {
  background: rgba(245, 34, 45, 0.1);
}

.stat-card-inline.orange {
  background: rgba(255, 153, 0, 0.1);
}

.stat-card-inline.yellow {
  background: rgba(255, 204, 0, 0.1);
}

.stat-card-inline.blue {
  background: rgba(0, 153, 255, 0.1);
}

.stat-icon {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
}

.stat-card-inline.red .stat-icon {
  background: #f5222d;
}

.stat-card-inline.orange .stat-icon {
  background: #ff9900;
}

.stat-card-inline.yellow .stat-icon {
  background: #ffcc00;
}

.stat-card-inline.blue .stat-icon {
  background: #0099ff;
}

.stat-card-inline .stat-label {
  font-size: 12px;
  color: #475569;
}

.stat-card-inline .stat-value {
  font-size: 18px;
  font-weight: 700;
}

.stat-card-inline.red .stat-value {
  color: #dc2626;
}

.stat-card-inline.orange .stat-value {
  color: #ea580c;
}

.stat-card-inline.yellow .stat-value {
  color: #ca8a04;
}

.stat-card-inline.blue .stat-value {
  color: #0284c7;
}

.stat-card-inline .stat-unit {
  font-size: 12px;
  color: #94a3b8;
}

.alarm-stats {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  flex: 1;
  position: relative;
  padding: 20px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  gap: 16px;
  overflow: hidden;
}

.stat-card.red {
  background: rgba(245, 34, 45, 0.08);
  border: 1px solid rgba(245, 34, 45, 0.2);
}

.stat-card.orange {
  background: rgba(255, 153, 0, 0.08);
  border: 1px solid rgba(255, 153, 0, 0.2);
}

.stat-card.yellow {
  background: rgba(255, 204, 0, 0.08);
  border: 1px solid rgba(255, 204, 0, 0.2);
}

.stat-card.blue {
  background: rgba(0, 153, 255, 0.08);
  border: 1px solid rgba(0, 153, 255, 0.2);
}

.alarm-content {
  display: flex;
  gap: 12px;
  height: calc(100vh - 120px);
}

.content-left {
  flex: 2;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.content-right {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.panel {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.panel-header {
  padding: 10px 14px;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
}

.panel-title {
  font-size: 13px;
  font-weight: 600;
  color: #1e293b;
  display: flex;
  align-items: center;
  gap: 8px;
}

.panel-body {
  padding: 12px;
}

.monthly-trend {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.monthly-trend .panel-body {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.echarts-container {
  flex: 1;
  min-height: 200px;
}

.alarm-list {
  flex: 1;
  min-height: 200px;
}

.alarm-table {
  width: 100%;
  border-collapse: collapse;
}

.alarm-table th {
  padding: 10px 12px;
  text-align: left;
  font-size: 12px;
  color: #64748b;
  font-weight: 600;
  border-bottom: 2px solid #e2e8f0;
}

.alarm-table td {
  padding: 12px;
  font-size: 13px;
  color: #334155;
  border-bottom: 1px solid #f1f5f9;
}

.alarm-table tbody tr:hover {
  background: #f8fafc;
}

.level-badge {
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.level-badge.red { background: rgba(220, 38, 38, 0.1); color: #dc2626; }
.level-badge.orange { background: rgba(234, 88, 12, 0.1); color: #ea580c; }
.level-badge.yellow { background: rgba(202, 138, 4, 0.1); color: #ca8a04; }
.level-badge.blue { background: rgba(2, 132, 199, 0.1); color: #0284c7; }

.response-badge {
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.response-badge.yes { background: rgba(34, 197, 94, 0.1); color: #22c55e; }
.response-badge.no { background: rgba(220, 38, 38, 0.1); color: #dc2626; }

.summary-table {
  flex: 1;
}

.summary-table-content {
  width: 100%;
  border-collapse: collapse;
}

.summary-table-content th {
  padding: 10px;
  text-align: center;
  font-size: 11px;
  color: #64748b;
  font-weight: 600;
  border-bottom: 2px solid #e2e8f0;
}

.summary-table-content td {
  padding: 10px;
  text-align: center;
  font-size: 12px;
  color: #334155;
  border-bottom: 1px solid #f1f5f9;
}

.summary-table-content tbody tr:hover {
  background: #f8fafc;
}

.pie-chart-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.pie-chart-panel .panel-body {
  flex: 1;
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: center;
  gap: 30px;
}

.pie-chart-container {
  width: 200px;
  height: 200px;
  margin-bottom: 16px;
}

.pie-legend {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.pie-legend .legend-item {
  font-size: 13px;
}
</style>
