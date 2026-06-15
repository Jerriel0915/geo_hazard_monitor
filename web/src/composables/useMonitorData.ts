// web/src/composables/useMonitorData.ts
import {type MaybeRef, reactive, ref, toValue, watch} from 'vue'
import {ElMessage} from 'element-plus'
import {
  type ChartData,
  type ExpressionSpec,
  getChartData,
  getMonitorDataPage,
  getSensorAggregate,
  getSensorCompleteness,
  getSensorLatest,
  getSensorRange,
  getSensorTrend,
  type MonitorDataPageItem,
} from '@/api/monitorData'
import {getDeviceSensors, type SensorAttrItem, type SensorItem} from '@/api/sensor'
import {getBoundDevices} from '@/api/hazardPoint'
import {showRequestErrorMessage} from '@/utils/errorHandler'

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

/**
 * Format epoch millis (or any value) to a sortable, locale-friendly label.
 * Used to align device-side charts with the hazard-point chart format.
 */
function formatChartLabel(input: unknown): string {
  if (input == null) return ''
  // If already a formatted string, keep as-is.
  if (typeof input === 'string') return input
  // Treat numeric (or numeric string) as epoch millis.
  const n = typeof input === 'number' ? input : Number(input)
  if (!Number.isFinite(n) || n <= 0) return String(input)
  const d = new Date(n)
  if (Number.isNaN(d.getTime())) return String(input)
  const pad = (v: number) => String(v).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

export interface UseMonitorDataOptions {
  hazardPointId: MaybeRef<number | null>
  initialDeviceId?: MaybeRef<number | null>
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
    const initDeviceId = toValue(opts.initialDeviceId)

    // 设备单独模式：无 hazardPointId，但有 initialDeviceId（设备未绑定隐患点）
    if (!hpId && initDeviceId) {
      // 先查设备信息，再查其传感器列表
      try {
        const sensors = await getDeviceSensors(initDeviceId)
        devices.value = [{
          deviceId: initDeviceId,
          deviceName: '',
          deviceCode: '',
          sensors: sensors
            .filter((s: SensorItem) => s.id != null)
            .map((s: SensorItem) => ({ id: s.id!, name: s.sensorName })),
        }]
      } catch {
        devices.value = []
      }
      return
    }

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
    const initDeviceId = toValue(opts.initialDeviceId)

    // 设备单独模式：无 hazardPointId，直接用传感器级 API 按 deviceId 查询
    if (!hpId && (filter.deviceId || initDeviceId)) {
      const deviceId = filter.deviceId ? Number(filter.deviceId) : initDeviceId!
      if (!deviceId) return

      // 从 sensorMap 中查找 sensorCode
      const sensorId = filter.sensorId ? Number(filter.sensorId) : 0
      const sensor = sensorMap.get(sensorId)
      if (!sensor) {
        ElMessage.warning('请先选择传感器')
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

      loading.value = true
      try {
          const dataMap: Record<string, any[]> = await (getSensorRange({
          deviceId,
          sensorCode: sensor.sensorCode,
          attrCode: filter.attrCode || undefined,
          startTime,
          endTime,
          }) as any)

        if (mode.value === 'chart') {
          // dataMap 是 Map<attrCode, List<{time, value, quality}>>，需要按 attrCode 展开
            // 后端 ORDER BY TIME DESC（倒序），图表需正序（时间从左到右递增），所以反转
          const seriesList: ChartData[] = []
          for (const [attrCode, rows] of Object.entries(dataMap)) {
              const sortedRows = [...rows].reverse()
            const labels: string[] = []
            const values: number[] = []
            let max = Number.NEGATIVE_INFINITY
            let min = Number.POSITIVE_INFINITY
            let sum = 0
              for (const r of sortedRows) {
              labels.push(formatChartLabel(r.dataTime ?? r.time))
              values.push(r.value)
              if (r.value != null) {
                max = Math.max(max, r.value)
                min = Math.min(min, r.value)
                sum += r.value
              }
            }
            const attrDef = sensor.attrList?.find((a: any) => a.attrCode === attrCode)
            const attrDisplayName = attrDef?.attrName || attrCode
            seriesList.push({
              seriesName: attrDisplayName,
              deviceName: '',
              sensorName: sensor.sensorName || sensor.sensorCode,
              labels,
              values,
              unit: attrDef?.unit || '',
              attrName: attrDisplayName,
              maxValue: values.length ? max : null,
              minValue: values.length ? min : null,
              avgValue: values.length ? sum / values.length : null,
            })
          }
          chartSeries.value = seriesList
          const totalPoints = seriesList.reduce((sum, s) => sum + s.labels.length, 0)
          ElMessage.success(`加载 ${seriesList.length} 条曲线，共 ${totalPoints} 个数据点`)
        } else {
          // 表格模式：扁平化所有 attrCode 的数据行（保留 attrCode 归属）
            // 后端 ORDER BY TIME DESC，表格也按正序排列（时间由旧到新）
          const flatRows: Array<{ attrCode: string; row: any }> = []
          for (const [code, rows] of Object.entries(dataMap)) {
              for (const r of [...rows].reverse()) flatRows.push({attrCode: code, row: r})
          }
          tableData.value = flatRows.map(({ attrCode: code, row: r }) => {
            const attrDef = sensor.attrList?.find((a: any) => a.attrCode === code)
            const attrDisplayName = attrDef?.attrName || code
            return {
              hazardPointId: 0,
              hazardPointName: '',
              dataTime: formatChartLabel(r.dataTime ?? r.time),
              deviceId,
              deviceName: '',
              sensorId: sensor.id ?? 0,
              sensorName: sensor.sensorName,
              attrCode: code,
              attrName: attrDisplayName,
              value: r.value,
              unit: attrDef?.unit || '',
              quality: r.quality,
              qualityText: r.quality === 0 || r.quality == null ? '正常' : '异常',
            }
          })
          ElMessage.success(`加载 ${tableData.value.length} 条数据`)
        }
      } catch (error) {
        showRequestErrorMessage(error, '获取监测数据失败')
      } finally {
        loading.value = false
      }
      return
    }

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
        defaultLocale: 'zh-cn',
        locales: [{
          name: 'zh-cn',
          options: {
            months: ['一月','二月','三月','四月','五月','六月','七月','八月','九月','十月','十一月','十二月'],
            shortMonths: ['1月','2月','3月','4月','5月','6月','7月','8月','9月','10月','11月','12月'],
            days: ['星期日','星期一','星期二','星期三','星期四','星期五','星期六'],
            shortDays: ['周日','周一','周二','周三','周四','周五','周六'],
            toolbar: { download: '下载 SVG', selection: '选择', selectionZoom: '区域缩放', zoomIn: '放大', zoomOut: '缩小', pan: '平移', reset: '重置' }
          }
        }],
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
