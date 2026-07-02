<!-- src/components/MonitorChart.vue - 封装监测数据图表 -->
<template>
  <view class="monitor-chart" :style="{ height }">
    <view v-if="loading" class="chart-loading">
      <text class="loading-text">加载中...</text>
    </view>
    <view v-else-if="errorMsg" class="chart-error">
      <text class="error-text">{{ errorMsg }}</text>
    </view>
    <view v-else-if="!hasData" class="chart-empty">
      <text class="empty-text">暂无数据</text>
    </view>
    <EchartsComponent
      v-if="hasData && !loading"
      :key="chartKey"
      :onInit="onChartInit"
      :canvasId="canvasId"
      width="100%"
      :height="height"
    />
  </view>
</template>

<script setup lang="ts">
import type { ChartSeries } from '@/utils/monitor'
import { monitorApi, calcGranularity } from '@/utils/monitor'
import * as echartsLib from '@/components/echarts.esm.min.js'
import EchartsComponent from '@/components/echarts.vue'
import { ref, watch, computed, getCurrentInstance, nextTick } from 'vue'

interface Props {
  // 查询参数（自动获取模式）
  hazardPointId?: number
  deviceId?: number
  sensorId?: number
  attrCode?: string
  valueType?: string
  startTime?: string
  endTime?: string
  granularity?: string
  // 外部数据模式
  series?: ChartSeries[]
  // 渲染配置
  height?: string
  showLegend?: boolean
  colors?: string[]
  chartType?: 'line' | 'auto'
  // 告警标注
  alarmLabels?: string[]
}

const props = withDefaults(defineProps<Props>(), {
  valueType: 'current',
  height: '400rpx',
  showLegend: false,
  chartType: 'line',
  colors: () => ['#3068e4', '#52c41a', '#fa8c16', '#722ed1', '#13c2c2', '#eb2f96'],
})

const emit = defineEmits<{
  (e: 'loaded', series: ChartSeries[]): void
  (e: 'error', err: unknown): void
  (e: 'chart-ready', chart: any): void
}>()

const uid = getCurrentInstance()?.uid ?? 0
const canvasId = `mc-${uid}-${Date.now().toString(36)}`
const loading = ref(false)
const errorMsg = ref('')
const chartKey = ref(0)
const internalSeries = ref<ChartSeries[]>([])
const chartInstance = ref<any>(null)

// 是否有数据
const hasData = computed(() => {
  const s = props.series ?? internalSeries.value
  return s.length > 0 && s.some(x => x.values && x.values.length > 0)
})

// 查询参数拼接的 key，用于 watch 自动触发 fetchData
const queryKey = computed(() =>
  `${props.hazardPointId}|${props.deviceId}|${props.sensorId}|${props.attrCode}|${props.startTime}|${props.endTime}`
)

// 自动获取模式：当 queryKey 变化且必要参数存在时触发
watch(queryKey, () => {
  if (props.series !== undefined) return // 外部数据模式不自动获取
  if (!props.hazardPointId || !props.startTime || !props.endTime) return
  fetchData()
}, { immediate: true })

// 外部 series 变化时重渲染（等待 DOM 更新后再触发重新挂载）
watch(() => props.series, async () => {
  await nextTick()
  chartKey.value++
}, { deep: false })

async function fetchData() {
  if (!props.hazardPointId || !props.startTime || !props.endTime) return
  loading.value = true
  errorMsg.value = ''
  try {
    const granularity = props.granularity
      || calcGranularity(props.startTime, props.endTime)
    const list = await monitorApi.getChart({
      hazardPointId: props.hazardPointId,
      deviceId: props.deviceId,
      sensorId: props.sensorId,
      attrCode: props.attrCode,
      valueType: (props.valueType as any) || 'current',
      startTime: props.startTime,
      endTime: props.endTime,
      granularity,
    })
    internalSeries.value = list
    // 等待 v-if 将 canvas 渲染到 DOM 后再 increment key 触发重新挂载，
    // 确保 echarts.vue 的 boundingClientRect 能获取到正确尺寸
    await nextTick()
    chartKey.value++
    emit('loaded', list)
  } catch (err) {
    console.error('[MonitorChart] fetchData error:', err)
    errorMsg.value = '加载失败'
    emit('error', err)
  } finally {
    loading.value = false
  }
}

function getActiveSeries(): ChartSeries[] {
  return props.series ?? internalSeries.value
}

function buildOption(): any {
  const series = getActiveSeries()
  if (!series || series.length === 0) return null

  const allLabels = new Set<string>()
  series.forEach(s => s.labels.forEach(l => allLabels.add(l)))
  const categories = Array.from(allLabels)

  const isAuto = props.chartType === 'auto'
  const seriesList: any[] = series.map((s, i) => {
    const color = props.colors[i % props.colors.length]
    const valueMap = new Map<string, number>()
    s.labels.forEach((l, idx) => valueMap.set(l, s.values[idx]))
    const data = categories.map(c => valueMap.get(c) ?? null)
    const isRain = isAuto && (/rain/i.test(s.seriesName || '') || /rain/i.test(s.attrName || '') || /雨量/.test(s.attrName || ''))

    if (isRain) {
      return {
        name: s.seriesName || s.attrName,
        type: 'bar',
        data,
        yAxisIndex: 0,
        itemStyle: {
          color: {
            type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color },
              { offset: 1, color: hexToRgba(color, 0.4) },
            ],
          },
          borderRadius: [4, 4, 0, 0],
        },
        barWidth: '30%',
      }
    }

    return {
      name: s.seriesName || s.attrName,
      type: 'line',
      data,
      smooth: true,
      symbol: 'circle',
      symbolSize: 3,
      showSymbol: categories.length <= 24,
      lineStyle: { color, width: 2 },
      itemStyle: { color: '#fff', borderColor: color, borderWidth: 2 },
      areaStyle: {
        color: {
          type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: hexToRgba(color, 0.2) },
            { offset: 1, color: hexToRgba(color, 0.02) },
          ],
        },
      },
    }
  })

  // 告警 markPoint：在主 series 上标注
  if (props.alarmLabels && props.alarmLabels.length > 0 && seriesList.length > 0) {
    const mainSeries = seriesList[0]
    const mainLabels = categories
    const indices = findAlarmIndices(mainLabels, props.alarmLabels)
    if (indices.length > 0) {
      mainSeries.markPoint = {
        data: indices.map((idx, i) => ({
          name: `告警点${i + 1}`,
          coord: [idx, mainSeries.data[idx]],
          value: mainSeries.data[idx],
          itemStyle: { color: '#f56c6c' },
          symbol: 'pin',
          symbolSize: 30,
          label: { show: true, formatter: '⚠', fontSize: 10 },
        })),
      }
      // 高亮告警点
      mainSeries.symbolSize = (_v: number, params: any) =>
        indices.includes(params.dataIndex) ? 10 : 3
      mainSeries.itemStyle = {
        color: (params: any) =>
          indices.includes(params.dataIndex) ? '#f56c6c' : props.colors[0],
      }
    }
  }

  const hasBarAndLine = seriesList.some((s: any) => s.type === 'bar')
    && seriesList.some((s: any) => s.type === 'line')

  const legendConfig = props.showLegend
    ? {
        data: seriesList.map((s: any) => s.name),
        bottom: 0,
        textStyle: { color: '#6b7280', fontSize: 10 },
        itemWidth: 16,
        itemHeight: 10,
      }
    : undefined

  const bottomOffset = props.showLegend ? 22 : 18

  return {
    animation: true,
    legend: legendConfig,
    grid: {
      left: '3%',
      right: hasBarAndLine ? '8%' : '3%',
      bottom: `${bottomOffset}%`,
      top: '8%',
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      data: categories,
      boundaryGap: hasBarAndLine,
      axisLabel: { color: '#9ca3af', fontSize: 9, margin: 8 },
      axisLine: { lineStyle: { color: '#e5e7eb' } },
      axisTick: { show: false },
      splitLine: { show: false },
    },
    yAxis: hasBarAndLine
      ? [
          {
            type: 'value',
            axisLabel: { color: '#9ca3af', fontSize: 9 },
            axisLine: { show: false },
            axisTick: { show: false },
            splitLine: { lineStyle: { color: '#f3f4f6', type: 'dashed' } },
          },
          {
            type: 'value',
            axisLabel: { color: '#9ca3af', fontSize: 9 },
            axisLine: { show: false },
            axisTick: { show: false },
            splitLine: { show: false },
          },
        ]
      : {
          type: 'value',
          axisLabel: { color: '#9ca3af', fontSize: 9 },
          axisLine: { show: false },
          axisTick: { show: false },
          splitLine: { lineStyle: { color: '#f3f4f6', type: 'dashed' } },
        },
    series: seriesList,
    dataZoom: [
      {
        type: 'slider', xAxisIndex: 0, height: 18, bottom: 4,
        borderColor: '#e5e7eb', fillerColor: 'rgba(48,104,228,0.15)',
        handleSize: '60%', textStyle: { fontSize: 9, color: '#9ca3af' },
      },
      { type: 'inside', xAxisIndex: 0 },
    ],
    tooltip: {
      trigger: 'axis',
      triggerOn: 'mousemove|click',
      backgroundColor: 'rgba(0,0,0,0.8)',
      borderColor: 'rgba(0,0,0,0.8)',
      textStyle: { color: '#fff', fontSize: 11 },
    },
  }
}

function findAlarmIndices(categories: string[], alarmLabels: string[]): number[] {
  const indices: number[] = []
  for (const al of alarmLabels) {
    const alarmTs = new Date(String(al).replace(/-/g, '/')).getTime()
    if (isNaN(alarmTs)) continue
    let bestIdx = -1
    let bestDiff = Infinity
    categories.forEach((label, idx) => {
      const labelTs = new Date(String(label).replace(/-/g, '/')).getTime()
      if (isNaN(labelTs)) return
      const diff = Math.abs(labelTs - alarmTs)
      if (diff < bestDiff) { bestDiff = diff; bestIdx = idx }
    })
    if (bestIdx >= 0 && !indices.includes(bestIdx)) indices.push(bestIdx)
  }
  return indices
}

function onChartInit(canvas: any, width: number, height: number) {
  const option = buildOption()
  if (!option) return null
  const chart = echartsLib.init(canvas, null, { width, height })
  canvas.setChart(chart)
  chart.setOption(option)
  chartInstance.value = chart
  emit('chart-ready', chart)
  return chart
}

function hexToRgba(hex: string, alpha: number) {
  const r = parseInt(hex.slice(1, 3), 16)
  const g = parseInt(hex.slice(3, 5), 16)
  const b = parseInt(hex.slice(5, 7), 16)
  return `rgba(${r}, ${g}, ${b}, ${alpha})`
}
</script>

<style lang="scss" scoped>
.monitor-chart {
  width: 100%;
  position: relative;
}

.chart-loading,
.chart-error,
.chart-empty {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.loading-text {
  font-size: 26rpx;
  color: #9ca3af;
}

.error-text {
  font-size: 26rpx;
  color: #f53f3f;
}

.empty-text {
  font-size: 26rpx;
  color: #9ca3af;
}
</style>
