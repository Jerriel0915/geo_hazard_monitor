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

  // ── 分页状态 ──
  const tablePageNum = ref(1)
  const tablePageSize = ref(30)
  const tableTotal = ref(0)

  // ── 降采样控制 ──
  const downsampleEnabled = ref(false)
  const downsampleGranularity = ref('auto')

  // ── 筛选状态 ──
  const filter = reactive({
    deviceId: '' as string | number,
    sensorIds: [] as number[],
    attrCodes: [] as string[],
    valueType: 'current',
    timeRange: null as [string, string] | null,
  })

  // ── 降采样信息（仅在后端确实降采样时才展示）──
  const downsampleInfo = computed(() => {
    const sampled = chartSeries.value.find((s) => s.sampled && s.downsampleInterval)
    if (!sampled || !sampled.pointCount) return null
    return {
      interval: sampled.downsampleInterval!,
      pointCount: sampled.pointCount,
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

    filter.sensorIds = []
    filter.attrCodes = []
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
      // 默认选中第一个传感器
      const firstSensor = sensors.value[0]
      if (firstSensor?.id != null) {
        filter.sensorIds = [firstSensor.id]
        collectAttrs(filter.sensorIds)
      }
    } catch (error) {
      if ((error as { name?: string; code?: string })?.name !== 'AbortError' && (error as { code?: string })?.code !== 'ERR_CANCELED') {
        showRequestErrorMessage(error, '获取传感器列表失败')
      }
    }
  }

  // ── 合并多个传感器的指标列表（去重）──
  const collectAttrs = (sensorIds: number[]) => {
    const seen = new Set<string>()
    const merged: AttrItem[] = []
    for (const sid of sensorIds) {
      const sensor = sensorMap.get(sid)
      if (!sensor?.attrList) continue
      for (const a of sensor.attrList) {
        if (seen.has(a.attrCode)) continue
        seen.add(a.attrCode)
        merged.push({
          code: a.attrCode,
          label: `${a.attrName || a.attrCode}${a.unit ? ` (${a.unit})` : ''}`,
        })
      }
    }
    attrs.value = merged
    // 保留已选指标中仍有效的，否则默认全部选中
    const valid = filter.attrCodes.filter(c => seen.has(c))
    filter.attrCodes = valid.length > 0 ? valid : merged.map(a => a.code)
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

      const sensorIds = filter.sensorIds.length > 0 ? filter.sensorIds : []
      if (sensorIds.length === 0) {
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
        const seriesList: ChartData[] = []
        const flatRows: Array<{ attrCode: string; sensorId: number; sensorName: string; row: Record<string, unknown> }> = []

        // 遍历每个选中的传感器
        for (const sid of sensorIds) {
          const sensor = sensorMap.get(sid)
          if (!sensor) continue

          const dataMap: Record<string, any[]> = await (getSensorRange({
            deviceId,
            sensorCode: sensor.sensorCode,
            startTime,
            endTime,
          }) as any)

          // 筛选用户选中的指标
          const targetCodes = filter.attrCodes.length > 0 ? filter.attrCodes : Object.keys(dataMap)

          for (const attrCode of targetCodes) {
            const rows = dataMap[attrCode]
            if (!rows?.length) continue
            const sortedRows = [...rows].reverse()
            const labels: string[] = []
            const values: number[] = []
            let max = Number.NEGATIVE_INFINITY
            let min = Number.POSITIVE_INFINITY
            let sum = 0
            for (const r of sortedRows) {
              labels.push(formatChartLabel(r.dataTime ?? r.time))
              const v = r.value != null ? Number(r.value) : Number.NaN
              values.push(v)
              if (!Number.isNaN(v)) {
                max = Math.max(max, v)
                min = Math.min(min, v)
                sum += v
              }
            }
            const attrDef = sensor.attrList?.find((a) => a.attrCode === attrCode)
            const attrDisplayName = attrDef?.attrName || attrCode
            const seriesLabel = `${sensor.sensorName || sensor.sensorCode} - ${attrDisplayName}`
            seriesList.push({
              seriesName: seriesLabel,
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
            // 表格行
            for (const r of sortedRows) {
              flatRows.push({ attrCode, sensorId: sensor.id ?? 0, sensorName: sensor.sensorName, row: r })
            }
          }
        }
        chartSeries.value = seriesList

        tableData.value = flatRows.map(({ attrCode: code, sensorId, sensorName, row: r }) => {
          const sensor = sensorMap.get(sensorId)
          const attrDef = sensor?.attrList?.find((a) => a.attrCode === code)
          const attrDisplayName = attrDef?.attrName || code
          return {
            hazardPointId: 0,
            hazardPointName: '',
            dataTime: formatChartLabel((r as Record<string, unknown>).dataTime ?? (r as Record<string, unknown>).time),
            deviceId,
            deviceName: '',
            sensorId,
            sensorName,
            attrCode: code,
            attrName: attrDisplayName,
            value: (r as Record<string, unknown>).value,
            unit: attrDef?.unit || '',
            quality: (r as Record<string, unknown>).quality,
            qualityText: (r as Record<string, unknown>).quality === 0 || (r as Record<string, unknown>).quality == null ? '正常' : '异常',
          }
        }) as typeof tableData.value
        tableTotal.value = tableData.value.length
        ElMessage.success(`加载 ${tableData.value.length} 条数据`)
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

    const granularity = downsampleEnabled.value
      ? downsampleGranularity.value   // 'auto' 或 '1m'/'5m'/... 直接传给后端
      : 'raw'

    // 新查询条件 → 重置到第一页
    tablePageNum.value = 1

    const commonParams = {
      hazardPointId: hpId,
      deviceId: filter.deviceId ? Number(filter.deviceId) : undefined,
      sensorId: filter.sensorIds.length === 1 ? filter.sensorIds[0] : undefined,
      attrCode: filter.attrCodes.length === 1 ? filter.attrCodes[0] : undefined,
      valueType: filter.valueType || undefined,
      startTime,
      endTime,
    }

    loading.value = true
    try {
      const sensorIds = filter.sensorIds.length > 0 ? filter.sensorIds : []
      const attrCodes = filter.attrCodes.length > 0 ? filter.attrCodes : []

      // 多选时逐传感器+指标并发查询，合并结果
      if (sensorIds.length > 1 || attrCodes.length > 1) {
        const seriesList: ChartData[] = []
        const promises = sensorIds.flatMap(sid =>
          (attrCodes.length > 0 ? attrCodes : ['']).map(ac =>
            getChartData({ ...commonParams, sensorId: sid, attrCode: ac || undefined, granularity } as any)
              .then(data => {
                if (Array.isArray(data)) {
                  for (const s of data) {
                    const sensor = sensorMap.get(sid)
                    seriesList.push({ ...s, seriesName: sensor ? `${sensor.sensorName} - ${s.attrName || s.seriesName}` : s.seriesName })
                  }
                }
              })
          )
        )
        await Promise.all(promises)
        chartSeries.value = seriesList

        // 多选时表格取第一个传感器+指标的分页数据
        const pageRes = await getMonitorDataPage({
          ...commonParams,
          sensorId: sensorIds[0],
          attrCode: attrCodes[0] || undefined,
          pageNum: tablePageNum.value,
          pageSize: tablePageSize.value,
        })
        tableData.value = (pageRes as any).rows || []
        tableTotal.value = (pageRes as any).total ?? tableData.value.length
      } else {
        const [series, pageRes] = await Promise.all([
          getChartData({ ...commonParams, granularity } as any),
          getMonitorDataPage({ ...commonParams, pageNum: tablePageNum.value, pageSize: tablePageSize.value }),
        ])
        chartSeries.value = series || []
        tableData.value = (pageRes as any).rows || []
        tableTotal.value = (pageRes as any).total ?? tableData.value.length
      }
      ElMessage.success(`加载 ${tableData.value.length} 条数据`)
    } catch (error) {
      showRequestErrorMessage(error, '获取监测数据失败')
    } finally {
      loading.value = false
    }
  }

  /** 翻页查询 — 不重置 pageNum，直接以指定页码请求后端 */
  const queryPage = async (pageNum: number, pageSize: number) => {
    const hpId = toValue(opts.hazardPointId)
    const initDeviceId = toValue(opts.initialDeviceId)

    // 设备单独模式暂不支持分页，回退到 query()
    if (!hpId && (filter.deviceId || initDeviceId)) {
      return query()
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

    tablePageNum.value = pageNum
    tablePageSize.value = pageSize
    loading.value = true
    try {
      const res = await getMonitorDataPage({
        hazardPointId: hpId,
        deviceId: filter.deviceId ? Number(filter.deviceId) : undefined,
        sensorId: filter.sensorIds.length === 1 ? filter.sensorIds[0] : undefined,
        attrCode: filter.attrCodes.length === 1 ? filter.attrCodes[0] : undefined,
        valueType: filter.valueType || undefined,
        startTime,
        endTime,
        pageNum,
        pageSize,
      })
      tableData.value = (res as any).rows || []
      tableTotal.value = (res as any).total ?? tableData.value.length
      ElMessage.success(`加载 ${tableData.value.length} 条数据`)
    } catch (error) {
      showRequestErrorMessage(error, '获取监测数据失败')
    } finally {
      loading.value = false
    }
  }

  // ── 选择传感器变化时合并指标 ──
  const onSensorIdsChange = (ids: number[]) => {
    filter.sensorIds = ids
    collectAttrs(ids)
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
    filter.sensorIds = []
    filter.attrCodes = []
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
        confine: true,
        textStyle: { fontSize: 11 },
        valueFormatter: (value: unknown) => {
          if (value == null) return ''
          const n = Number(Number(value).toFixed(2))
          return hasUnit ? `${n} ${unit}` : String(n)
        },
      },
      legend: {
        bottom: 2,
        left: 'center',
        padding: [8, 0, 0, 0],
        textStyle: { fontSize: 10, fontWeight: 500 },
        itemWidth: 10,
        itemHeight: 10,
        itemGap: 10,
      },
      grid: {
        borderColor: '#e7e7e7',
        top: 12,
        right: 20,
        bottom: 90,
        left: 20,
      },
      xAxis: {
        type: 'category' as const,
        data: xCategories.map(formatXLabel),
        name: '时间',
        nameLocation: 'center',
        nameGap: 25,
        nameTextStyle: { fontSize: 13, fontWeight: 600, color: '#374151' },
        axisLabel: { rotate: 35, fontSize: 10, color: '#6b7280', hideOverlap: true, interval: 'auto', width: 60, overflow: 'truncate' },
        axisLine: { lineStyle: { color: '#d9d9d9' } },
      },
      yAxis: {
        type: 'value' as const,
        name: hasUnit ? `数值 (${unit})` : '数值',
        nameTextStyle: { fontSize: 13, fontWeight: 600, color: '#374151' },
        axisLabel: {
          fontSize: 10,
          color: '#6b7280',
          formatter: (val: number) => (val != null ? Number(val.toFixed(2)).toString() : ''),
        },
        splitLine: { lineStyle: { type: 'dashed', color: '#e8e8e8' } },
      },
      dataZoom: [
        { type: 'inside', xAxisIndex: 0 },
        { type: 'slider', xAxisIndex: 0, bottom: 20, height: 18 },
      ],
      toolbox: {
        feature: {
          dataZoom: { yAxisIndex: 'none' },
          restore: {},
          saveAsImage: { pixelRatio: 2 },
        },
        right: 10,
      },
      series: seriesData.map((s, i) => {
        const seriesColor = CHART_COLORS[i % CHART_COLORS.length]
        const r = parseInt(seriesColor.slice(1, 3), 16)
        const g = parseInt(seriesColor.slice(3, 5), 16)
        const b = parseInt(seriesColor.slice(5, 7), 16)
        return {
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
                { offset: 0, color: `rgba(${r},${g},${b},0.2)` },
                { offset: 1, color: `rgba(${r},${g},${b},0.01)` },
              ],
            },
          },
        }
      }),
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
    downsampleEnabled,
    downsampleGranularity,
    // 分页
    tablePageNum,
    tablePageSize,
    tableTotal,
    // 筛选
    filter,
    // 方法
    selectDevice,
    onSensorIdsChange,
    query,
    queryPage,
    reset,
    buildChartOptions,
    // 高级
    querySensorLatest,
    querySensorAggregate,
    querySensorCompleteness,
    querySensorTrend,
  }
}
