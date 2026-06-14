// web/src/composables/useMonitorData.ts
import { ref, reactive, watch, type MaybeRef, toValue } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getChartData,
  getMonitorDataPage,
  getSensorLatest,
  getSensorAggregate,
  getSensorCompleteness,
  getSensorTrend,
  type ChartData,
  type MonitorDataPageItem,
  type ExpressionSpec,
} from '@/api/monitorData'
import { getDeviceSensors, type SensorItem, type SensorAttrItem } from '@/api/sensor'
import { getBoundDevices } from '@/api/hazardPoint'
import { showRequestErrorMessage } from '@/utils/errorHandler'

export interface BoundDeviceItem {
  deviceId: number
  deviceName: string
  deviceCode: string
  sensors?: { id: number; name: string }[]
}

export interface AttrItem {
  code: string
  label: string
}

const CHART_COLORS = [
  '#3b82f6', '#ef4444', '#10b981', '#f59e0b', '#8b5cf6',
  '#06b6d4', '#ec4899', '#84cc16', '#f97316', '#6366f1',
]

export interface UseMonitorDataOptions {
  hazardPointId: MaybeRef<number | null>
}

export function useMonitorData(opts: UseMonitorDataOptions) {
  // ── 数据状态 ──
  const devices = ref<BoundDeviceItem[]>([])
  const sensors = ref<SensorItem[]>([])
  const attrs = ref<AttrItem[]>([])
  const chartSeries = ref<ChartData[]>([])
  const tableData = ref<MonitorDataPageItem[]>([])
  const loading = ref(false)
  const mode = ref<'chart' | 'table'>('chart')

  // ── 筛选状态 ──
  const filter = reactive({
    deviceId: '' as string | number,
    sensorId: '' as string | number,
    attrCode: '',
    valueType: 'current',
    timeRange: null as [string, string] | null,
  })

  // ── 缓存 ──
  const sensorMap = new Map<number, SensorItem>()

  // ── AbortController ──
  let abortController: AbortController | null = null

  // ── 加载设备列表 ──
  const loadDevices = async () => {
    const hpId = toValue(opts.hazardPointId)
    if (!hpId) {
      devices.value = []
      return
    }
    try {
      const res: any = await getBoundDevices(String(hpId))
      const list = res.data || res || []
      devices.value = (Array.isArray(list) ? list : []).map((d: any) => ({
        deviceId: d.deviceId,
        deviceName: d.deviceName,
        deviceCode: d.deviceCode,
        sensors: d.sensors || [],
      }))
    } catch {
      devices.value = []
    }
  }

  // ── 选择设备 → 加载传感器 ──
  const selectDevice = async (deviceId: number | string) => {
    abortController?.abort()
    abortController = new AbortController()

    filter.sensorId = ''
    filter.attrCode = ''
    sensors.value = []
    attrs.value = []

    if (!deviceId) return

    try {
      const list = await getDeviceSensors(Number(deviceId))
      sensorMap.clear()
      for (const s of list) {
        if (s.id != null) sensorMap.set(s.id, s)
      }
      sensors.value = list
    } catch (error: any) {
      if (error?.name !== 'AbortError' && error?.code !== 'ERR_CANCELED') {
        showRequestErrorMessage(error, '获取传感器列表失败')
      }
    }
  }

  // ── 选择传感器 → 提取指标 ──
  const selectSensor = (sensorId: number | string) => {
    filter.attrCode = ''
    if (!sensorId) {
      attrs.value = []
      return
    }
    const sensor = sensorMap.get(Number(sensorId))
    attrs.value = (sensor?.attrList || []).map((a: SensorAttrItem) => ({
      code: a.attrCode,
      label: `${a.attrName || a.attrCode}${a.unit ? ` (${a.unit})` : ''}`,
    }))
  }

  // ── 默认时间范围（最近 3 天）──
  const defaultTimeRange = (): [string, string] => {
    const end = new Date()
    const start = new Date(end.getTime() - 3 * 24 * 60 * 60 * 1000)
    const fmt = (d: Date) => {
      const pad = (n: number) => String(n).padStart(2, '0')
      return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
    }
    return [fmt(start), fmt(end)]
  }

  // ── 查询 ──
  const query = async () => {
    const hpId = toValue(opts.hazardPointId)
    if (!hpId) {
      ElMessage.warning('请先选择隐患点')
      return
    }

    let startTime: string
    let endTime: string
    if (filter.timeRange && filter.timeRange[0] && filter.timeRange[1]) {
      startTime = filter.timeRange[0]
      endTime = filter.timeRange[1]
    } else {
      ;[startTime, endTime] = defaultTimeRange()
    }

    const baseParams = {
      hazardPointId: hpId,
      deviceId: filter.deviceId ? Number(filter.deviceId) : undefined,
      sensorId: filter.sensorId ? Number(filter.sensorId) : undefined,
      attrCode: filter.attrCode || undefined,
      valueType: filter.valueType || undefined,
      startTime,
      endTime,
    }

    loading.value = true
    try {
      if (mode.value === 'chart') {
        const series = await getChartData(baseParams as any)
        chartSeries.value = series || []
        ElMessage.success(
          `加载 ${series.length} 条曲线，共 ${series[0]?.labels.length || 0} 个数据点`
        )
      } else {
        const res = await getMonitorDataPage({ ...baseParams, pageNum: 1, pageSize: 100 })
        tableData.value = (res as any).rows || []
        ElMessage.success(`加载 ${tableData.value.length} 条数据`)
      }
    } catch (error) {
      showRequestErrorMessage(error, '获取监测数据失败')
    } finally {
      loading.value = false
    }
  }

  // ── 高级方法 ──
  const querySensorLatest = (deviceId: number, sensorCode: string, attrCode?: string) =>
    getSensorLatest(deviceId, sensorCode, attrCode)

  const querySensorAggregate = (
    deviceId: number, sensorCode: string,
    startTime: string, endTime: string,
    expressions: ExpressionSpec[], granularity?: string,
  ) => getSensorAggregate(
    { deviceId, sensorCode, startTime, endTime, granularity },
    expressions,
  )

  const querySensorCompleteness = (
    deviceId: number, sensorCode: string, attrCode: string,
    startTime: string, endTime: string, expectedIntervalMs?: number,
  ) => getSensorCompleteness({ deviceId, sensorCode, attrCode, startTime, endTime, expectedIntervalMs })

  const querySensorTrend = (
    deviceId: number, sensorCode: string, attrCode: string,
    startTime: string, endTime: string,
  ) => getSensorTrend({ deviceId, sensorCode, attrCode, startTime, endTime })

  // ── 重置 ──
  const reset = () => {
    filter.deviceId = ''
    filter.sensorId = ''
    filter.attrCode = ''
    filter.valueType = 'current'
    filter.timeRange = null
    sensors.value = []
    attrs.value = []
    chartSeries.value = []
    tableData.value = []
  }

  // ── 构建图表配置 ──
  const buildChartOptions = (seriesData: ChartData[]) => {
    if (seriesData.length === 0) return { series: [] }
    const allLabels = new Set<string>()
    for (const s of seriesData) for (const l of s.labels) allLabels.add(l)
    const xCategories = Array.from(allLabels).sort()

    return {
      series: seriesData.map((s) => {
        const points = s.labels.map((l, i) => ({ x: l, y: s.values[i] }))
        return { name: s.seriesName, data: points }
      }),
      chart: {
        type: 'area' as const,
        height: '100%',
        fontFamily: 'inherit',
        toolbar: {
          tools: {
            download: true, selection: true, zoom: true,
            zoomin: true, zoomout: true, pan: true, reset: true,
          },
        },
        zoom: { enabled: true, type: 'x' as const },
        animations: { enabled: true, easing: 'easeinout' as const, speed: 800 },
      },
      colors: CHART_COLORS,
      dataLabels: { enabled: false },
      stroke: { curve: 'smooth' as const, width: 2 },
      fill: {
        type: 'gradient',
        gradient: { shadeIntensity: 1, opacityFrom: 0.2, opacityTo: 0.02, stops: [0, 100] },
      },
      markers: { size: 0, hover: { size: 5 } },
      grid: {
        borderColor: '#e7e7e7',
        strokeDashArray: 4,
        padding: { top: 10, right: 10, bottom: 5, left: 10 },
      },
      legend: {
        position: 'top' as const,
        horizontalAlign: 'center' as const,
        fontSize: '13px',
        fontWeight: 500,
        markers: { width: 12, height: 12, radius: 6, offsetX: -4 },
        itemMargin: { horizontal: 16, vertical: 4 },
        offsetY: -4,
      },
      xaxis: {
        type: 'category' as const,
        categories: xCategories,
        labels: { rotate: -30, style: { fontSize: '11px', colors: '#666' } },
        tickAmount: Math.min(xCategories.length, 10),
        tooltip: { enabled: false },
      },
      yaxis: {
        title: {
          text: seriesData[0]?.unit || '',
          style: { fontSize: '12px', color: '#888' },
        },
        labels: {
          formatter: (val: number) => val != null ? Number(val.toFixed(2)).toString() : '',
        },
      },
      tooltip: { shared: true, intersect: false },
    }
  }

  // ── 监听 hazardPointId 变化自动加载 ──
  watch(() => toValue(opts.hazardPointId), () => {
    reset()
    filter.timeRange = defaultTimeRange()
    loadDevices()
  }, { immediate: true })

  return {
    // 状态
    devices,
    sensors,
    attrs,
    chartSeries,
    tableData,
    loading,
    mode,
    // 筛选
    filter,
    // 方法
    selectDevice,
    selectSensor,
    query,
    reset,
    buildChartOptions,
    // 高级
    querySensorLatest,
    querySensorAggregate,
    querySensorCompleteness,
    querySensorTrend,
  }
}
