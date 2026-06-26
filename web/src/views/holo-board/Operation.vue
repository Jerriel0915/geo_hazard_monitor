<!-- 运营视图 -->

<template>
  <div class="operation-view">
    <div class="stats-row">
      <div
        v-for="card in statCards"
        :key="card.key"
        class="stat-card"
        :style="{ '--tc': card.color }"
      >
        <div class="stat-left">
          <svg
            v-if="card.key === 'totalDevices'"
            xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none"
            stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"
            width="28" height="28"
          >
            <path d="M22 12h-4l-3 9L9 3l-3 9H2"/>
          </svg>
          <svg
            v-else-if="card.key === 'onlineRate'"
            xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none"
            stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"
            width="28" height="28"
          >
            <path d="M20 13V6a2 2 0 0 0-2-2H6a2 2 0 0 0-2 2v7m16 0v5a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2v-5m16 0h-2.586a1 1 0 0 0-.707.293l-2.414 2.414a1 1 0 0 1-.707.293h-3.172a1 1 0 0 1-.707-.293l-2.414-2.414A1 1 0 0 0 6.586 13H4"/>
          </svg>
          <svg
            v-else-if="card.key === 'repairRate'"
            xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none"
            stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"
            width="28" height="28"
          >
            <circle cx="12" cy="12" r="10"/>
            <line x1="12" y1="8" x2="12" y2="12"/>
            <line x1="12" y1="16" x2="12.01" y2="16"/>
          </svg>
          <svg
            v-else-if="card.key === 'monitorTypes'"
            xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none"
            stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"
            width="28" height="28"
          >
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
            <polyline points="14 2 14 8 20 8"/>
            <line x1="16" y1="13" x2="8" y2="13"/>
            <line x1="16" y1="17" x2="8" y2="17"/>
            <polyline points="10 9 9 9 8 9"/>
          </svg>
          <svg
            v-else-if="card.key === 'sensorOnlineRate'"
            xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none"
            stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"
            width="28" height="28"
          >
            <path d="M12 20V10"/>
            <path d="M18 20V4"/>
            <path d="M6 20v-6"/>
          </svg>
          <svg
            v-else-if="card.key === 'hiddenDangerCount'"
            xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none"
            stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"
            width="28" height="28"
          >
            <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/>
            <path d="M13.73 21a2 2 0 0 1-3.46 0"/>
          </svg>
        </div>
        <div class="stat-right">
          <span v-if="card.auxiliary != null" class="stat-aux">{{ card.auxiliary }}</span>
          <span class="stat-value">{{ card.value }}</span>
          <span class="stat-label">{{ card.label }}</span>
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
          <div class="table-wrap">
            <div class="table-wrap__scroll">
              <el-table :data="online6hSorted" border stripe size="small">
                  <template #empty><EmptyState description="暂无 6 小时在线率数据" /></template>
                <el-table-column prop="type" label="监测类型" min-width="140" />
                <el-table-column prop="total" label="总量" width="80" align="center" />
                <el-table-column prop="online" label="在线" width="80" align="center" />
                <el-table-column prop="rate" label="在线率(%)" width="120" align="center">
                  <template #default="{ row }">
                    <span :class="['rate-badge', row.rate >= 90 ? 'high' : row.rate >= 70 ? 'medium' : 'low']">{{ row.rate }}%</span>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </div>
        </div>
      </div>
      <div class="table-panel">
        <div class="panel-header">
          <span class="panel-title">12小时在线率(12h有数据)</span>
        </div>
        <div class="panel-body">
          <div class="table-wrap">
            <div class="table-wrap__scroll">
              <el-table :data="online12hSorted" border stripe size="small">
                  <template #empty><EmptyState description="暂无 12 小时在线率数据" /></template>
                <el-table-column prop="type" label="监测类型" min-width="140" />
                <el-table-column prop="total" label="总量" width="80" align="center" />
                <el-table-column prop="online" label="在线" width="80" align="center" />
                <el-table-column prop="rate" label="在线率(%)" width="120" align="center">
                  <template #default="{ row }">
                    <span :class="['rate-badge', row.rate >= 90 ? 'high' : row.rate >= 70 ? 'medium' : 'low']">{{ row.rate }}%</span>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </div>
        </div>
      </div>
      <div class="table-panel">
        <div class="panel-header">
          <span class="panel-title">24小时在线率(24h有数据)</span>
        </div>
        <div class="panel-body">
          <div class="table-wrap">
            <div class="table-wrap__scroll">
              <el-table :data="online24hSorted" border stripe size="small">
                  <template #empty><EmptyState description="暂无 24 小时在线率数据" /></template>
                <el-table-column prop="type" label="监测类型" min-width="140" />
                <el-table-column prop="total" label="总量" width="80" align="center" />
                <el-table-column prop="online" label="在线" width="80" align="center" />
                <el-table-column prop="rate" label="在线率(%)" width="120" align="center">
                  <template #default="{ row }">
                    <span :class="['rate-badge', row.rate >= 90 ? 'high' : row.rate >= 70 ? 'medium' : 'low']">{{ row.rate }}%</span>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {computed, onMounted, onUnmounted, ref} from 'vue'
import echarts from '@/utils/echarts'
import EmptyState from '@/components/EmptyState.vue'
import {
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

const statCards = computed(() => {
  const onlineCount = deviceOnline.value?.online ?? 0
  const totalCount = deviceOnline.value?.total ?? 0
  const repairCount = totalCount - onlineCount
  const sensorOnlineCount = sensorOnline.value?.online ?? 0

  return [
    { key: 'totalDevices', label: '设备总数', color: '#00b8d4', value: stats.value.totalDevices.toLocaleString(), auxiliary: null },
    { key: 'onlineRate', label: '设备在线率', color: '#10b981', value: stats.value.onlineRate + '%', auxiliary: onlineCount.toLocaleString() },
    { key: 'repairRate', label: '设备报修率', color: '#f59e0b', value: stats.value.repairRate + '%', auxiliary: repairCount.toLocaleString() },
    { key: 'monitorTypes', label: '监测种类', color: '#ef4444', value: stats.value.monitorTypes.toLocaleString(), auxiliary: null },
    { key: 'sensorOnlineRate', label: '传感器在线率', color: '#8b5cf6', value: stats.value.sensorOnlineRate + '%', auxiliary: sensorOnlineCount.toLocaleString() },
    { key: 'hiddenDangerCount', label: '隐患点总数', color: '#ec4899', value: stats.value.hiddenDangerCount.toLocaleString(), auxiliary: null },
  ]
})

const barChartRef = ref<HTMLDivElement>()
let barChartInstance: echarts.ECharts | null = null
const pyramidChartRef = ref<HTMLDivElement>()
let pyramidChartInstance: echarts.ECharts | null = null
const pieChartRef = ref<HTMLDivElement>()
let pieChartInstance: echarts.ECharts | null = null

const initBarChart = () => {
  if (!barChartRef.value) return

  barChartInstance = echarts.init(barChartRef.value)

  const option: echarts.EChartsCoreOption = {
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
        show: true,
        lineStyle: { color: '#c0c4cc' }
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

  const option: echarts.EChartsCoreOption = {
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

  const option: echarts.EChartsCoreOption = {
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

// 汇总所有监测类型名称（6h + 12h + 24h 的并集）
const allMonitorTypes = computed(() => {
  const set = new Set<string>()
  ;[deviceActive6h.value, deviceActive12h.value, deviceActive24h.value].forEach(d => {
    (d?.byType ?? []).forEach(t => set.add(t.monitorTypeName))
  })
  return [...set]
})

function buildTimeWindowData(source: RateByTypeVO | null): { type: string; total: number; online: number; rate: number }[] {
  const map = new Map<string, { total: number; online: number; rate: number }>()
  ;(source?.byType ?? []).forEach(t => {
    map.set(t.monitorTypeName, { total: t.total, online: t.online, rate: t.onlineRate })
  })
  return allMonitorTypes.value
    .map(name => {
      const found = map.get(name)
      return { type: name, total: found?.total ?? 0, online: found?.online ?? 0, rate: found?.rate ?? 0 }
    })
    .sort((a, b) => b.rate - a.rate)
}

const online6hSorted = computed(() => buildTimeWindowData(deviceActive6h.value))
const online12hSorted = computed(() => buildTimeWindowData(deviceActive12h.value))
const online24hSorted = computed(() => buildTimeWindowData(deviceActive24h.value))
</script>

<style scoped>
.operation-view {
  height: 100%;
  background: transparent;
  padding: 15px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
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
  border-radius: 10px;
  overflow: hidden;
  /* 弱化阴影，靠色块和边框区分 */
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

/* ---- 左侧色块 ---- */
.stat-left {
  width: 38%;
  min-width: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--tc);
  border-radius: 10px 0 0 10px;
}

.stat-left svg {
  width: 28px;
  height: 28px;
}

/* ---- 右侧白色内容区 ---- */
.stat-right {
  flex: 1;
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #ffffff;
  border: 1.5px solid var(--tc);
  border-left: none;
  border-radius: 0 10px 10px 0;
  padding: 14px 10px;
  text-align: center;
}

.stat-value {
  font-size: 28px;
  font-weight: 800;
  color: var(--tc);
  line-height: 1.1;
}

.stat-label {
  font-size: 13px;
  color: var(--tc);
  margin-top: 3px;
  font-weight: 500;
}

.stat-aux {
  position: absolute;
  top: 8px;
  right: 10px;
  font-size: 12px;
  color: #94a3b8;
  font-weight: 500;
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
  padding: 0;
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
  flex: 1;
  min-height: 0;
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