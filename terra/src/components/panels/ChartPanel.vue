<!-- ======================================== -->
<!-- ChartPanel - 数据图表面板组件 (基于 ECharts) -->
<!-- ======================================== -->

<template>
  <div
    class="chart-panel"
    :style="panelStyle"
  >
    <div class="panel-header">
      <h3 class="panel-title">{{ config.title }}</h3>
      <div class="panel-actions">
        <button class="action-btn" @click="handleMaximize" title="最大化">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
            <path d="M2 2h5v2H4v3H2V2zm7 0h5v5h-2V4H9V2zM2 9h2v3h3v2H2V9zm12 0h-2v3h-3v2h5V9z"/>
          </svg>
        </button>
        <button class="action-btn close" @click="handleClose" title="关闭">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
            <path d="M4.646 4.646a.5.5 0 0 1 .708 0L8 7.293l2.646-2.647a.5.5 0 0 1 .708.708L8.707 8l2.647 2.646a.5.5 0 0 1-.708.708L8 8.707l-2.646 2.647a.5.5 0 0 1-.708-.708L7.293 8 4.646 5.354a.5.5 0 0 1 0-.708z"/>
          </svg>
        </button>
      </div>
    </div>

    <div class="chart-container" ref="chartContainer"></div>

    <div v-if="loading" class="panel-loading">
      <div class="spinner"></div>
      <span>加载中...</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, computed, nextTick } from 'vue'
import * as echarts from 'echarts'
import { usePanelCommand } from '@/composables/usePanelCommand'
import type { PanelConfig } from '@/types'
import type { ECharts, EChartsOption } from 'echarts'

/**
 * Props
 */
interface Props {
  id: string
  config: PanelConfig
}

const props = defineProps<Props>()

/**
 * Emits
 */
interface Emits {
  (e: 'focus', panelId: string): void
  (e: 'close', panelId: string): void
}

const emit = defineEmits<Emits>()

/**
 * 状态
 */
const chartContainer = ref<HTMLElement>()
const loading = ref(false)
const isMaximized = ref(false)
let chartInstance: ECharts | null = null

/**
 * 面板样式
 */
const panelStyle = computed(() => {
  if (!props.config?.position) {
    console.warn('[ChartPanel] config.position is missing:', props.config)
    return {}
  }
  const { x, y, w, h } = props.config.position
  return {
    gridColumn: `${x} / span ${w}`,
    gridRow: `${y} / span ${isMaximized.value ? 999 : h}`
  }
})

/**
 * 获取图表数据（支持两种格式）
 */
function getChartData() {
  const chartData = props.config.data

  // 格式1: Chart.js 风格 (datasets + labels)
  if (chartData?.datasets && Array.isArray(chartData.datasets) && chartData.datasets.length > 0) {
    return {
      datasets: chartData.datasets,
      labels: chartData.labels || [],
      chartType: chartData.chartType || 'line'
    }
  }

  // 格式2: 简单格式 (values 数组)
  if (chartData?.values && Array.isArray(chartData.values)) {
    return {
      datasets: [{
        label: chartData.label || '',
        data: chartData.values,
        color: chartData.color || '#10b981'
      }],
      labels: chartData.labels || [],
      chartType: chartData.chartType || 'line'
    }
  }

  // 无数据
  return { datasets: [], labels: [], chartType: 'line' }
}

/**
 * 将 chartType 转换为 ECharts 的 series type
 */
function getEChartsType(chartType: string): string {
  const typeMap: Record<string, string> = {
    'line': 'line',
    'bar': 'bar',
    'area': 'line',
    'mixed': 'line'
  }
  return typeMap[chartType] || 'line'
}

/**
 * 构建 ECharts option
 */
function buildEChartsOption(): EChartsOption {
  const chartData = getChartData()
  const { datasets, labels, chartType } = chartData

  if (datasets.length === 0) {
    return {
      title: {
        text: '暂无数据',
        left: 'center',
        top: 'center',
        textStyle: { color: '#64748b', fontSize: 14 }
      }
    }
  }

  // 构建 series
  const series = datasets.map((dataset: any) => {
    const seriesType = dataset.type || (chartType === 'mixed' ? 'line' : getEChartsType(chartType))
    const isArea = chartType === 'area' || dataset.type === 'area'

    return {
      name: dataset.label || '',
      type: seriesType,
      data: dataset.data || [],
      smooth: true,
      // 面积图配置
      areaStyle: isArea ? {
        opacity: 0.3,
        color: dataset.color || '#10b981'
      } : undefined,
      // 折线图/柱状图样式
      itemStyle: {
        color: dataset.color || '#10b981'
      },
      lineStyle: seriesType === 'line' ? {
        color: dataset.color || '#10b981',
        width: 2
      } : undefined,
      // 标签
      label: {
        show: false,
        position: 'top'
      }
    }
  })

  // 构建 legend
  const legend = datasets.length > 1 ? {
    show: true,
    data: datasets.map((d: any) => d.label).filter(Boolean),
    textStyle: { color: '#94a3b8' },
    top: 10,
    right: 10
  } : { show: false }

  // 构建 xAxis
  const xAxis = {
    type: 'category',
    data: labels,
    axisLine: { lineStyle: { color: 'rgba(100, 116, 139, 0.3)' } },
    axisLabel: { color: '#64748b', fontSize: 10 }
  }

  // 构建 yAxis
  const yAxis = {
    type: 'value',
    axisLine: { show: false },
    axisTick: { show: false },
    splitLine: { lineStyle: { color: 'rgba(100, 116, 139, 0.1)' } },
    axisLabel: { color: '#64748b', fontSize: 10 }
  }

  // Tooltip 配置
  const tooltip = {
    trigger: 'axis',
    backgroundColor: 'rgba(0, 8, 20, 0.95)',
    borderColor: '#00d4ff',
    borderWidth: 1,
    textStyle: { color: '#e2e8f0', fontSize: 12 },
    axisPointer: {
      type: 'cross',
      lineStyle: { color: '#00d4ff', type: 'dashed' }
    }
  }

  // Grid 配置
  const grid = {
    left: 50,
    right: datasets.length > 1 ? 120 : 20,
    top: datasets.length > 1 ? 50 : 30,
    bottom: 40,
    containLabel: false
  }

  return {
    tooltip,
    legend,
    grid,
    xAxis,
    yAxis,
    series
  }
}

/**
 * 初始化图表
 */
function initChart() {
  if (!chartContainer.value) return

  // 如果已存在实例，先销毁
  if (chartInstance) {
    chartInstance.dispose()
  }

  // 创建 ECharts 实例
  chartInstance = echarts.init(chartContainer.value)

  // 设置 option
  const option = buildEChartsOption()
  chartInstance.setOption(option)

  console.log('[ChartPanel] ECharts initialized:', option)
}

/**
 * 最大化面板
 */
function handleMaximize() {
  isMaximized.value = !isMaximized.value
  nextTick(() => {
    if (chartInstance) {
      chartInstance.resize()
    }
  })
}

/**
 * 关闭面板
 */
function handleClose() {
  emit('close', props.id)
}

/**
 * 注册面板指令处理器
 */
const { registerAutoHandler } = usePanelCommand()

const chartOperations = {
  updateDataset: (params: { index?: number; data: any[] }) => {
    if (params.data && chartInstance) {
      const option = chartInstance.getOption()
      const series = option.series as any[]
      const index = params.index ?? 0

      if (series[index]) {
        series[index].data = params.data
        chartInstance.setOption({ series })
        return { success: true, count: params.data.length }
      }
    }
    return { success: false, error: 'No data provided' }
  },

  setData: (params: { data: any[]; labels?: string[] }) => {
    if (params.data && chartInstance) {
      const option = chartInstance.getOption()
      const series = option.series as any[]

      if (series[0]) {
        series[0].data = params.data
        if (params.labels) {
          (option.xAxis as any)[0].data = params.labels
        }
        chartInstance.setOption({
          series,
          xAxis: option.xAxis
        })
        return { success: true, count: params.data.length }
      }
    }
    return { success: false, error: 'No data provided' }
  },

  clearData: () => {
    if (chartInstance) {
      const option = chartInstance.getOption()
      option.series = []
      chartInstance.setOption({ series: [] })
      return { success: true }
    }
    return { success: false }
  }
}

registerAutoHandler('chart', 'updateDataset', (_, cmd) => chartOperations.updateDataset(cmd.params || {}))
registerAutoHandler('chart', 'setData', (_, cmd) => chartOperations.setData(cmd.params || {}))
registerAutoHandler('chart', 'clearData', () => chartOperations.clearData())

/**
 * 监听窗口大小变化
 */
function handleResize() {
  if (chartInstance) {
    chartInstance.resize()
  }
}

/**
 * 监听数据变化
 */
watch(() => props.config.data, () => {
  if (chartInstance) {
    const option = buildEChartsOption()
    chartInstance.setOption(option, true)  // notMerge = true
  }
}, { deep: true })

/**
 * 组件挂载
 */
onMounted(() => {
  initChart()
  window.addEventListener('resize', handleResize)
})

/**
 * 组件卸载
 */
onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
})
</script>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.chart-panel {
  background: $bg-secondary;
  backdrop-filter: $backdrop-blur;
  border: 1px solid $border-default;
  border-radius: $radius-sm;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-shadow: $shadow-sm;
  transition: all $transition-fast $ease-out;
  height: 100%;
  min-height: 240px;

  &:hover {
    border-color: $border-medium;
    box-shadow: $shadow-md;
  }
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: rgba(0, 8, 20, 0.8);
  border-bottom: 1px solid $border-default;
  position: relative;

  // 线框装饰
  &::after {
    content: '';
    position: absolute;
    bottom: -1px;
    left: 0;
    width: 30%;
    height: 1px;
    background: linear-gradient(
      to right,
      $border-accent,
      transparent
    );
  }

  .panel-title {
    font-family: $font-family-ui;
    font-size: 11px;
    font-weight: $font-weight-semibold;
    color: $color-primary;
    text-transform: uppercase;
    letter-spacing: 1px;
    text-shadow: $text-shadow-sm;
  }

  .panel-actions {
    display: flex;
    gap: 4px;

    .action-btn {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 24px;
      height: 24px;
      border: 1px solid $border-subtle;
      background: transparent;
      color: $text-secondary;
      border-radius: $radius-sm;
      cursor: pointer;
      transition: all $transition-fast $ease-out;

      &:hover {
        border-color: $border-accent;
        background: rgba($color-primary, 0.1);
        color: $color-primary;
        box-shadow: $glow-primary;
      }

      &.close:hover {
        border-color: $border-warning;
        background: rgba($terra-warning, 0.1);
        color: $terra-warning;
        box-shadow: $glow-warning;
      }
    }
  }
}

.chart-container {
  flex: 1;
  min-height: 0;
  position: relative;
  overflow: hidden;
  width: 100%;
  height: 100%;
}

.panel-loading {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  background: rgba($bg-secondary, 0.95);
  backdrop-filter: $backdrop-blur;
  color: $color-primary;
  font-family: $font-family-ui;
  font-size: $font-size-small;
  letter-spacing: 1px;
  text-transform: uppercase;

  .spinner {
    width: 28px;
    height: 28px;
    border: 2px solid $border-default;
    border-top-color: $border-accent;
    border-radius: 50%;
    animation: spin 1s linear infinite;
  }
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
