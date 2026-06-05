<template>
  <div class="operation-view">
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-icon">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="#00d4ff" stroke-width="2"
               width="28" height="28">
            <path d="M22 12h-4l-3 9L9 3l-3 9H2"/>
          </svg>
        </div>
        <div class="stat-content">
          <span class="stat-value">{{ stats.totalDevices }}</span>
          <span class="stat-label">设备总数</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="#52c41a" stroke-width="2"
               width="28" height="28">
            <path
                d="M20 13V6a2 2 0 0 0-2-2H6a2 2 0 0 0-2 2v7m16 0v5a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2v-5m16 0h-2.586a1 1 0 0 0-.707.293l-2.414 2.414a1 1 0 0 1-.707.293h-3.172a1 1 0 0 1-.707-.293l-2.414-2.414A1 1 0 0 0 6.586 13H4"/>
          </svg>
        </div>
        <div class="stat-content">
          <span class="stat-value">{{ stats.onlineRate }}%</span>
          <span class="stat-label">设备在线率</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="#f5a623" stroke-width="2"
               width="28" height="28">
            <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/>
            <circle cx="12" cy="10" r="3"/>
          </svg>
        </div>
        <div class="stat-content">
          <span class="stat-value">{{ stats.repairRate }}%</span>
          <span class="stat-label">设备报修率</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="#ff6b6b" stroke-width="2"
               width="28" height="28">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
            <polyline points="14 2 14 8 20 8"/>
            <line x1="16" y1="13" x2="8" y2="13"/>
            <line x1="16" y1="17" x2="8" y2="17"/>
            <polyline points="10 9 9 9 8 9"/>
          </svg>
        </div>
        <div class="stat-content">
          <span class="stat-value">{{ stats.monitorTypes }}</span>
          <span class="stat-label">监测种类</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="#a855f7" stroke-width="2"
               width="28" height="28">
            <path d="M12 20V10"/>
            <path d="M18 20V4"/>
            <path d="M6 20v-6"/>
          </svg>
        </div>
        <div class="stat-content">
          <span class="stat-value">{{ stats.sensorOnlineRate }}%</span>
          <span class="stat-label">传感器在线率</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="#ec4899" stroke-width="2"
               width="28" height="28">
            <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/>
            <path d="M13.73 21a2 2 0 0 1-3.46 0"/>
          </svg>
        </div>
        <div class="stat-content">
          <span class="stat-value">{{ stats.hiddenDangerCount }}</span>
          <span class="stat-label">隐患点总数</span>
        </div>
      </div>
    </div>

    <div class="charts-row">
      <div class="chart-panel device-pie">
        <div class="panel-header">
          <span class="panel-title">设备占比</span>
        </div>
        <div class="panel-body">
          <div ref="pieChartRef" class="pie-echarts-container"></div>
        </div>
      </div>

      <div class="chart-panel bar-chart-panel">
        <div class="panel-header">
          <span class="panel-title">监测类型设备在线率</span>
        </div>
        <div class="panel-body">
          <div ref="barChartRef" class="echarts-container"></div>
        </div>
      </div>

      <div class="chart-panel pyramid-panel">
        <div class="panel-header">
          <span class="panel-title">传感器分类</span>
        </div>
        <div class="panel-body">
          <div ref="pyramidChartRef" class="echarts-container"></div>
        </div>
      </div>
    </div>

    <div class="table-row">
      <div class="table-panel">
        <div class="panel-header">
          <span class="panel-title">6小时在线率(6h有数据)</span>
        </div>
        <div class="panel-body">
          <table class="online-table">
            <thead>
            <tr>
              <th>类型</th>
              <th>总量</th>
              <th>在线</th>
              <th>在线率(%)</th>
            </tr>
            </thead>
            <tbody>
            <tr v-for="item in online6hData" :key="item.type">
              <td>{{ item.type }}</td>
              <td>{{ item.total }}</td>
              <td>{{ item.online }}</td>
              <td><span :class="['rate-badge', item.rate >= 90 ? 'high' : item.rate >= 70 ? 'medium' : 'low']">{{
                  item.rate
                }}%</span></td>
            </tr>
            </tbody>
          </table>
        </div>
      </div>
      <div class="table-panel">
        <div class="panel-header">
          <span class="panel-title">12小时在线率(12h有数据)</span>
        </div>
        <div class="panel-body">
          <table class="online-table">
            <thead>
            <tr>
              <th>类型</th>
              <th>总量</th>
              <th>在线</th>
              <th>在线率(%)</th>
            </tr>
            </thead>
            <tbody>
            <tr v-for="item in online12hData" :key="item.type">
              <td>{{ item.type }}</td>
              <td>{{ item.total }}</td>
              <td>{{ item.online }}</td>
              <td><span :class="['rate-badge', item.rate >= 90 ? 'high' : item.rate >= 70 ? 'medium' : 'low']">{{
                  item.rate
                }}%</span></td>
            </tr>
            </tbody>
          </table>
        </div>
      </div>
      <div class="table-panel">
        <div class="panel-header">
          <span class="panel-title">24小时在线率(24h有数据)</span>
        </div>
        <div class="panel-body">
          <table class="online-table">
            <thead>
            <tr>
              <th>类型</th>
              <th>总量</th>
              <th>在线</th>
              <th>在线率(%)</th>
            </tr>
            </thead>
            <tbody>
            <tr v-for="item in online24hData" :key="item.type">
              <td>{{ item.type }}</td>
              <td>{{ item.total }}</td>
              <td>{{ item.online }}</td>
              <td><span :class="['rate-badge', item.rate >= 90 ? 'high' : item.rate >= 70 ? 'medium' : 'low']">{{
                  item.rate
                }}%</span></td>
            </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {computed, onMounted, onUnmounted, ref} from 'vue'
import * as echarts from 'echarts'
import {
  type DashboardFullVO,
  type DashboardOverview,
  getDashboardFull,
  getDeviceActiveRate,
  type RateByTypeVO,
  type SensorDistributionVO
} from '@/api/monitor'

const overview = ref<DashboardOverview | null>(null)
const deviceOnline = ref<RateByTypeVO | null>(null)
const deviceActive6h = ref<RateByTypeVO | null>(null)
const deviceActive12h = ref<RateByTypeVO | null>(null)
const deviceActive24h = ref<RateByTypeVO | null>(null)
const sensorOnline = ref<RateByTypeVO | null>(null)
const sensorDist = ref<SensorDistributionVO | null>(null)

const stats = computed(() => ({
  totalDevices: overview.value?.device?.total ?? 0,
  onlineRate: deviceOnline.value?.onlineRate ?? 0,
  repairRate: deviceOnline.value ? Math.round((100 - deviceOnline.value.onlineRate) * 100) / 100 : 0,
  monitorTypes: overview.value?.monitorType?.total ?? 0,
  sensorOnlineRate: sensorOnline.value?.onlineRate ?? 0,
  hiddenDangerCount: overview.value?.hazardPoint?.total ?? 0
}))

const barChartRef = ref<HTMLDivElement>()
let barChartInstance: echarts.ECharts | null = null
const pyramidChartRef = ref<HTMLDivElement>()
let pyramidChartInstance: echarts.ECharts | null = null
const pieChartRef = ref<HTMLDivElement>()
let pieChartInstance: echarts.ECharts | null = null

const initBarChart = () => {
  if (!barChartRef.value) return

  barChartInstance = echarts.init(barChartRef.value)

  const option: echarts.EChartsOption = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      },
      formatter: (params: any) => {
        const data = params[0]
        return `${data.name}<br/>在线率: ${data.value}%`
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '15%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: barChartData.map(item => item.name),
      axisLabel: {
        interval: 0,
        rotate: 30,
        color: '#64748b',
        fontSize: 16
      },
      axisLine: {
        lineStyle: {
          color: '#e2e8f0'
        }
      }
    },
    yAxis: {
      type: 'value',
      max: 100,
      axisLabel: {
        formatter: '{value}%',
        color: '#64748b',
        fontSize: 16
      },
      splitLine: {
        lineStyle: {
          color: '#f1f5f9'
        }
      },
      axisLine: {
        show: false
      }
    },
    series: [
      {
        name: '在线率',
        type: 'bar',
        data: barChartData.map((item, index) => ({
          value: item.value,
          itemStyle: {
            color: barColors[index % barColors.length]
          }
        })),
        barWidth: '50%',
        label: {
          show: true,
          position: 'top',
          formatter: '{c}%',
          color: '#1e293b',
          fontSize: 16,
          fontWeight: 600
        }
      }
    ]
  }

  barChartInstance.setOption(option)
}

const initPieChart = () => {
  if (!pieChartRef.value) return

  pieChartInstance = echarts.init(pieChartRef.value)

  const option: echarts.EChartsOption = {
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b}: {c}% ({d}%)'
    },
    legend: {
      orient: 'vertical',
      right: '5%',
      top: 'center',
      textStyle: {
        color: '#334155',
        fontSize: 16
      },
      itemWidth: 14,
      itemHeight: 14,
      itemGap: 10
    },
    series: [
      {
        name: '设备占比',
        type: 'pie',
        radius: ['45%', '70%'],
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
          formatter: '{b}\n{c}%',
          color: '#334155',
          fontSize: 16
        },
        labelLine: {
          show: true,
          lineStyle: {
            color: '#94a3b8'
          }
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 12,
            fontWeight: 'bold'
          }
        },
        data: (sensorDist.value?.list ?? []).map((item, i) => ({
          name: item.monitorTypeName,
          value: item.sensorCount,
          itemStyle: {color: PIE_COLORS[i % PIE_COLORS.length]}
        }))
      }
    ]
  }

  pieChartInstance.setOption(option)
}

const initPyramidChart = () => {
  if (!pyramidChartRef.value) return

  pyramidChartInstance = echarts.init(pyramidChartRef.value)

  const option: echarts.EChartsOption = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      },
      formatter: (params: any) => {
        const data = params[0]
        return `${data.name}<br/>数量: ${data.value}`
      }
    },
    grid: {
      left: '3%',
      right: '25%',
      bottom: '3%',
      top: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'value',
      axisLabel: {
        color: '#64748b',
        fontSize: 16
      },
      axisLine: {
        lineStyle: {
          color: '#e2e8f0'
        }
      },
      splitLine: {
        lineStyle: {
          color: '#f1f5f9'
        }
      }
    },
    yAxis: {
      type: 'category',
      data: pyramidData.map(item => item.name).reverse(),
      axisLabel: {
        color: '#334155',
        fontSize: 16
      },
      axisLine: {
        lineStyle: {
          color: '#e2e8f0'
        }
      }
    },
    series: [
      {
        name: '数量',
        type: 'bar',
        data: pyramidData.map((item, index) => ({
          value: item.count,
          itemStyle: {
            color: pyramidColors[index]
          }
        })).reverse(),
        barWidth: '60%',
        label: {
          show: true,
          position: 'right',
          formatter: '{c}',
          color: '#1e293b',
          fontSize: 16,
          fontWeight: 600
        }
      }
    ]
  }

  pyramidChartInstance.setOption(option)
}

const handleResize = () => {
  barChartInstance?.resize()
  pyramidChartInstance?.resize()
  pieChartInstance?.resize()
}

onMounted(async () => {
  try {
    const [full, d6, d12, d24] = await Promise.all([
      getDashboardFull(60),
      getDeviceActiveRate(360),
      getDeviceActiveRate(720),
      getDeviceActiveRate(1440)
    ])
    const d = full.data
    overview.value = d.overview
    deviceOnline.value = d.deviceOnlineRate
    deviceActive6h.value = d6.data
    deviceActive12h.value = d12.data
    deviceActive24h.value = d24.data
    sensorOnline.value = d.sensorOnlineRate
    sensorDist.value = d.sensorDistribution
  } catch { /* use defaults */
  }
  initPieChart()
  initBarChart()
  initPyramidChart()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  barChartInstance?.dispose()
  pyramidChartInstance?.dispose()
  pieChartInstance?.dispose()
})

const PIE_COLORS = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899', '#06b6d4', '#84cc16', '#f97316']
const circumference = 2 * Math.PI * 70

const devicePieData = computed(() => {
  const types = sensorDist.value?.list ?? []
  if (types.length === 0) return []
  const total = types.reduce((s, t) => s + t.sensorCount, 0)
  let offset = 0
  return types.map((item, i) => {
    const percent = total > 0 ? item.sensorCount / total : 0
    const dashArray = `${percent * circumference} ${circumference}`
    const currentOffset = -offset
    offset += percent * circumference
    return {
      name: item.monitorTypeName,
      value: Math.round(percent * 10000) / 100,
      color: PIE_COLORS[i % PIE_COLORS.length],
      dashArray,
      offset: currentOffset
    }
  })
})

const barColors = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899']

const barChartData = [
  {name: '表面水平位移', value: 98.5},
  {name: '深部位移', value: 97.8},
  {name: '倾角', value: 96.2},
  {name: '加速度', value: 94.5},
  {name: '降雨量', value: 99.1},
  {name: '地下水水位', value: 95.3}
]

const pyramidColors = ['#ef4444', '#f59e0b', '#10b981', '#3b82f6', '#a855f7', '#ec4899', '#00d4ff', '#84cc16', '#f97316']

const pyramidData = [
  {name: '表面水平位移', count: 535, width: 100},
  {name: '深部位移', count: 515, width: 95},
  {name: '倾角', count: 479, width: 90},
  {name: '加速度', count: 415, width: 85},
  {name: '降雨量', count: 142, width: 70},
  {name: '地下水水位', count: 66, width: 50},
  {name: '表面沉降', count: 80, width: 55},
  {name: '温度', count: 27, width: 30},
  {name: '孔隙水压力', count: 3, width: 10}
]

const online6hData = computed(() =>
    (deviceActive6h.value?.byType ?? []).map(t => ({
      type: t.monitorTypeName,
      total: t.total,
      online: t.online,
      rate: t.onlineRate
    }))
)
const online12hData = computed(() =>
    (deviceActive12h.value?.byType ?? []).map(t => ({
      type: t.monitorTypeName,
      total: t.total,
      online: t.online,
      rate: t.onlineRate
    }))
)
const online24hData = computed(() =>
    (deviceActive24h.value?.byType ?? []).map(t => ({
      type: t.monitorTypeName,
      total: t.total,
      online: t.online,
      rate: t.onlineRate
    }))
)
</script>

<style scoped>
.operation-view {
  min-height: 100%;
  background: transparent;
  padding: 24px 0 0 0;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.stats-row {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
}

.stat-card {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 24px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.stat-icon {
  width: 52px;
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 212, 255, 0.1);
  border-radius: 10px;
}

.stat-icon svg {
  width: 28px;
  height: 28px;
}

.stat-content {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: #1e293b;
  line-height: 1.1;
}

.stat-label {
  font-size: 16px;
  color: #64748b;
  margin-top: 4px;
}

.charts-row {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
}

.chart-panel {
  flex: 1;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.panel-header {
  padding: 14px 18px;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
}

.panel-title {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
}

.panel-body {
  padding: 16px;
}

.pie-echarts-container {
  width: 100%;
  height: 320px;
}

.echarts-container {
  width: 100%;
  height: 320px;
}

.table-row {
  display: flex;
  gap: 16px;
}

.table-panel {
  flex: 1;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.online-table {
  width: 100%;
  border-collapse: collapse;
}

.online-table th {
  padding: 12px 12px;
  text-align: center;
  font-size: 15px;
  color: #64748b;
  font-weight: 600;
  border-bottom: 2px solid #e2e8f0;
}

.online-table td {
  padding: 12px 12px;
  text-align: center;
  font-size: 16px;
  color: #334155;
  border-bottom: 1px solid #f1f5f9;
}

.online-table tbody tr:hover {
  background: #f8fafc;
}

.rate-badge {
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 15px;
  font-weight: 500;
}

.rate-badge.high {
  background: rgba(16, 185, 129, 0.1);
  color: #10b981;
}

.rate-badge.medium {
  background: rgba(245, 158, 11, 0.1);
  color: #f59e0b;
}

.rate-badge.low {
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
}
</style>