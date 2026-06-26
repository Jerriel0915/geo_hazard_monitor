// web/src/composables/useMonitorData.ts
import {type MaybeRef, computed, reactive, ref, toValue, watch} from 'vue'
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

  // ── 降采样信息 ──
  const downsampleInfo = computed(() => {
    const sampled = chartSeries.value.find((s) => s.sampled)
    if (!sampled) return null
    return {
      interval: sampled.downsampleInterval || '',
      pointCount: sampled.pointCount ?? 0,
    }
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
      const res = await getBoundDevices(String(hpId))
      const rawList = (res as Record<string, unknown>).data || res
      const list = (Array.isArray(rawList) ? rawList : []) as Array<Record<string, unknown>>
      devices.value = list.map((d) => ({
        deviceId: (d.deviceId ?? d.id ?? 0) as number,
        deviceName: (d.deviceName ?? '') as string,
        deviceCode: (d.deviceCode ?? '') as string,
        sensors: (Array.isArray(d.sensors) ? d.sensors : []) as BoundDeviceItem['sensors'],
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
      // 默认选中第一个传感器（并联动选中其第一个指标）
      const firstSensor = sensors.value[0]
      if (firstSensor?.id != null) {
        filter.sensorId = firstSensor.id
        selectSensor(firstSensor.id)
      }
    } catch (error) {
      if ((error as { name?: string; code?: string })?.name !== 'AbortError' && (error as { code?: string })?.code !== 'ERR_CANCELED') {
        showRequestErrorMessage(error, '获取传感器列表失败')
      }
    }
  }

  // ── 选择传感器 → 提取指标 ──
  const selectSensor = (sensorId: number | string) => {
    if (!sensorId) {
      filter.attrCode = ''
      attrs.value = []
      return
    }
    const sensor = sensorMap.get(Number(sensorId))
    attrs.value = (sensor?.attrList || []).map((a: SensorAttrItem) => ({
      code: a.attrCode,
      label: `${a.attrName || a.attrCode}${a.unit ? ` (${a.unit})` : ''}`,
    }))
    // 默认选中第一个指标
    filter.attrCode = attrs.value[0]?.code ?? ''
  }

  // ── 默认时间范围（最近 7 天，按自然日对齐：起始 00:00:00，结束 23:59:59）──
  const defaultTimeRange = (): [string, string] => {
    const pad = (n: number) => String(n).padStart(2, '0')
    const fmt = (d: Date) =>
      `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
    const end = new Date()
    end.setHours(23, 59, 59, 0)
    const start = new Date(end)
    start.setDate(start.getDate() - 6) // 含今日共 7 个自然日
    start.setHours(0, 0, 0, 0)
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
            const attrDef = sensor.attrList?.find((a) => a.attrCode === attrCode)
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
        } else {
          // 表格模式：扁平化所有 attrCode 的数据行（保留 attrCode 归属）
            // 后端 ORDER BY TIME DESC，表格也按正序排列（时间由旧到新）
          const flatRows: Array<{ attrCode: string; row: Record<string, unknown> }> = []
          for (const [code, rows] of Object.entries(dataMap)) {
              for (const r of [...rows].reverse()) flatRows.push({attrCode: code, row: r})
          }
          const raw = flatRows.map(({ attrCode: code, row: r }) => {
            const attrDef = sensor.attrList?.find((a) => a.attrCode === code)
            const attrDisplayName = attrDef?.attrName || code
            return {
              hazardPointId: 0,
              hazardPointName: '',
              dataTime: formatChartLabel((r as Record<string, unknown>).dataTime ?? (r as Record<string, unknown>).time),
              deviceId,
              deviceName: '',
              sensorId: sensor.id ?? 0,
              sensorName: sensor.sensorName,
              attrCode: code,
              attrName: attrDisplayName,
              value: (r as Record<string, unknown>).value,
              unit: attrDef?.unit || '',
              quality: (r as Record<string, unknown>).quality,
              qualityText: (r as Record<string, unknown>).quality === 0 || (r as Record<string, unknown>).quality == null ? '正常' : '异常',
            }
          })
          tableData.value = raw as typeof tableData.value
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

  // ── 构建 ECharts 配置 ──
  const buildChartOptions = (seriesData: ChartData[]): Record<string, unknown> => {
    if (seriesData.length === 0) return {}

    const allLabels = new Set<string>()
    for (const s of seriesData) for (const l of s.labels) allLabels.add(l)
    const xCategories = Array.from(allLabels).sort()

    const unit = seriesData[0]?.unit || ''
    const hasUnit = unit && unit.length > 0

    const formatXLabel = (label: string) => {
      const match = label.match(/^(\d{4})-(\d{2})-(\d{2})\s+(\d{2}):(\d{2}):(\d{2})$/)
      if (match) {
        const [, , month, day, hour, minute] = match
        return `${month}-${day} ${hour}:${minute}`
      }
      if (label.length <= 8) return label
      return label.length > 12 ? label.substring(0, 10) + '\u2026' : label
    }

    return {
      color: CHART_COLORS,
      tooltip: {
        trigger: 'axis' as const,
        valueFormatter: (value: unknown) => {
          if (value == null) return ''
          const n = Number(Number(value).toFixed(2))
          return hasUnit ? `${n} ${unit}` : String(n)
        },
      },
      legend: {
        top: 0,
        left: 'center',
        textStyle: { fontSize: 13, fontWeight: 500 },
        itemWidth: 12,
        itemHeight: 12,
        itemGap: 16,
      },
      grid: {
        borderColor: '#e7e7e7',
        top: 50,
        right: 20,
        bottom: 60,
        left: 20,
      },
      xAxis: {
        type: 'category' as const,
        data: xCategories.map(formatXLabel),
        name: '时间',
        nameTextStyle: { fontSize: 13, fontWeight: 600, color: '#374151' },
        axisLabel: { rotate: 30, fontSize: 11, color: '#6b7280', hideOverlap: true },
        axisLine: { lineStyle: { color: '#d9d9d9' } },
      },
      yAxis: {
        type: 'value' as const,
        name: hasUnit ? `数值 (${unit})` : '数值',
        nameTextStyle: { fontSize: 13, fontWeight: 600, color: '#374151' },
        axisLabel: {
          fontSize: 11,
          color: '#6b7280',
          formatter: (val: number) => (val != null ? Number(val.toFixed(2)).toString() : ''),
        },
        splitLine: { lineStyle: { type: 'dashed', color: '#e8e8e8' } },
      },
      dataZoom: [
        { type: 'inside', xAxisIndex: 0 },
        { type: 'slider', xAxisIndex: 0, bottom: 10, height: 20 },
      ],
      toolbox: {
        feature: {
          dataZoom: { yAxisIndex: 'none' },
          restore: {},
          saveAsImage: { pixelRatio: 2 },
        },
        right: 10,
      },
      series: seriesData.map((s) => ({
        name: s.seriesName,
        type: 'line' as const,
        data: xCategories.map((cat) => {
          const idx = s.labels.indexOf(cat)
          return idx !== -1 ? s.values[idx] : null
        }),
        smooth: true,
        symbol: 'none' as const,
        sampling: 'lttb' as const,
        large: true as const,
        largeThreshold: 2000,
        progressive: 400,
        areaStyle: {
          color: {
            type: 'linear' as const, x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(59,130,246,0.2)' },
              { offset: 1, color: 'rgba(59,130,246,0.01)' },
            ],
          },
        },
      })),
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
    downsampleInfo,
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
