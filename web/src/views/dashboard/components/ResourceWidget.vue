<template>
  <div class="panel-section resource-section">
    <div class="section-header">
      <span class="section-title-group">
        <el-icon class="section-icon" :size="18"><Box/></el-icon>
        <span class="section-title">资产情况</span>
      </span>
    </div>
    <div class="resource-compact">
      <div class="resource-main">
        <div class="resource-total">
          <div class="total-circle">
            <svg class="total-ring" viewBox="0 0 80 80">
              <circle class="ring-bg" cx="40" cy="40" r="35"/>
              <circle class="ring-hazard" cx="40" cy="40" r="35" :stroke-dasharray="`113 170`"
                      stroke-dashoffset="0"/>
              <circle class="ring-device" cx="40" cy="40" r="35" :stroke-dasharray="`142 141`"
                      stroke-dashoffset="-113"/>
            </svg>
            <div class="total-value">{{ resourceStats.totalResources }}</div>
          </div>
          <div class="total-label">资源总数</div>
        </div>
        <div class="resource-breakdown">
          <div class="breakdown-item hazard">
            <div class="breakdown-icon">
              <el-icon :size="14" color="#faad14"><Location/></el-icon>
            </div>
            <div class="breakdown-info">
              <span class="breakdown-value">{{ resourceStats.hazardTotal }}</span>
              <span class="breakdown-label">隐患点</span>
            </div>
          </div>
          <div class="breakdown-item device">
            <div class="breakdown-icon">
              <el-icon :size="14" color="#52c41a"><Setting/></el-icon>
            </div>
            <div class="breakdown-info">
              <span class="breakdown-value">{{ resourceStats.deviceTotal }}</span>
              <span class="breakdown-label">设备</span>
            </div>
          </div>
        </div>
      </div>
      <div class="device-type-section">
        <div class="list-header">
          <span class="list-title">设备分类</span>
          <span class="list-count">{{ resourceStats.deviceTypes.length }}类</span>
        </div>
        <div ref="chartRef" class="rose-chart"></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { Box, Location, Setting } from '@element-plus/icons-vue'
import * as echarts from 'echarts'

const props = defineProps<{
  resourceStats: {
    totalResources: number
    deviceTotal: number
    hazardTotal: number
    deviceTypes: { name: string; count: number }[]
  }
}>()

const chartRef = ref<HTMLElement | null>(null)
let chartInstance: echarts.ECharts | null = null
let resizeObserver: ResizeObserver | null = null

const ROSE_COLORS = ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272', '#fc8452', '#9a60b4']

const renderChart = () => {
  if (!chartRef.value) return
  const rect = chartRef.value.getBoundingClientRect()
  if (rect.height < 40 || rect.width < 40) return

  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value)
  }
  const data = props.resourceStats.deviceTypes.map((t, i) => ({
    name: t.name,
    value: t.count,
    itemStyle: { color: ROSE_COLORS[i % ROSE_COLORS.length] }
  }))

  chartInstance.setOption({
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c}台 ({d}%)',
      backgroundColor: 'rgba(255,255,255,0.95)',
      borderColor: 'rgba(0,0,0,0.08)',
      textStyle: { color: '#1d2129', fontSize: 12 }
    },
    legend: {
      orient: 'vertical',
      right: 0,
      top: 'middle',
      icon: 'circle',
      itemWidth: 8,
      itemHeight: 8,
      itemGap: 10,
      textStyle: { fontSize: 11, color: '#4e5969', lineHeight: 16 },
      formatter: (name: string) => {
        const item = props.resourceStats.deviceTypes.find(t => t.name === name)
        return item ? `${name}  ${item.count}` : name
      }
    },
    series: [{
      type: 'pie',
      roseType: 'area',
      radius: ['10%', '75%'],
      center: ['35%', '50%'],
      itemStyle: { borderRadius: 4, borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      emphasis: {
        label: { show: true, fontSize: 12, fontWeight: 'bold' }
      },
      data
    }]
  }, true)
}

watch(() => props.resourceStats.deviceTypes, () => {
  nextTick(renderChart)
}, { deep: true })

onMounted(() => {
  resizeObserver = new ResizeObserver(() => {
    if (chartInstance) {
      chartInstance.resize()
    } else {
      renderChart()
    }
  })
  if (chartRef.value) {
    resizeObserver.observe(chartRef.value)
  }
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (resizeObserver) {
    resizeObserver.disconnect()
    resizeObserver = null
  }
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
})

const handleResize = () => {
  chartInstance?.resize()
}
</script>

<style scoped>
.panel-section {
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(16px);
  border: 1px solid rgba(24, 144, 255, 0.08);
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.06);
  padding: 16px 18px;
  margin-bottom: 12px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  min-height: 0;
  transition: box-shadow 0.2s, border-color 0.2s;
}

.panel-section:hover {
  border-color: rgba(24, 144, 255, 0.15);
  box-shadow: 0 4px 16px rgba(24, 144, 255, 0.08);
}

.panel-section:last-child {
  margin-bottom: 0;
  flex-grow: 1;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 14px 10px;
  background: linear-gradient(135deg, rgba(82, 196, 26, 0.12) 0%, rgba(82, 196, 26, 0.03) 100%);
  border-radius: 8px 8px 0 0;
  border-bottom: 1px solid rgba(82, 196, 26, 0.12);
  margin: -16px -18px 16px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #1d2129;
  font-family: var(--font-display, inherit);
}

.section-title-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.section-icon {
  color: #52c41a;
}

.resource-compact {
  display: flex;
  flex-direction: column;
  gap: 14px;
  flex: 1;
  min-height: 0;
}

.resource-main {
  display: flex;
  align-items: center;
  gap: 16px;
}

.resource-total {
  flex-shrink: 0;
  text-align: center;
}

.total-circle {
  position: relative;
  width: 80px;
  height: 80px;
}

.total-ring {
  width: 100%;
  height: 100%;
  transform: rotate(-90deg);
}

.ring-bg {
  fill: none;
  stroke: rgba(0, 0, 0, 0.06);
  stroke-width: 10;
}

.ring-hazard {
  fill: none;
  stroke: #faad14;
  stroke-width: 10;
  stroke-linecap: round;
}

.ring-device {
  fill: none;
  stroke: #52c41a;
  stroke-width: 10;
  stroke-linecap: round;
}

.total-value {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 18px;
  font-weight: 700;
  color: #1d2129;
  font-family: var(--font-display, inherit);
}

.total-label {
  margin-top: 6px;
  font-size: 10px;
  color: #86909c;
}

.resource-breakdown {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
}

.breakdown-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  background: rgba(59, 130, 246, 0.06);
  border-radius: 8px;
  border: 1px solid rgba(59, 130, 246, 0.08);
}

.breakdown-icon {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 7px;
  flex-shrink: 0;
}

.breakdown-info {
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.breakdown-value {
  font-size: 18px;
  font-weight: 700;
  color: #1d2129;
  font-family: var(--font-display, inherit);
}

.breakdown-label {
  font-size: 11px;
  color: #86909c;
}

.device-type-section {
  border-top: 1px solid rgba(0, 0, 0, 0.06);
  padding-top: 12px;
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  flex-shrink: 0;
}

.list-title {
  font-size: 13px;
  font-weight: 600;
  color: #374151;
}

.list-count {
  font-size: 12px;
  color: #9ca3af;
  font-weight: 500;
}

.rose-chart {
  width: 100%;
  flex: 1;
  min-height: 0;
}
</style>
