import {nextTick, reactive, ref, type Ref, watch} from 'vue'
import {ElMessage} from 'element-plus'
import {getDeviceSensors} from '@/api/sensor'
import type {ChartData, LatestDataItem, MonitorDataPageItem} from '@/api/monitorData'
import {getChartData, getLatestData, getMonitorDataPage} from '@/api/monitorData'
import {showRequestErrorMessage} from '@/utils/errorHandler'
import type {HazardPointItem} from './useHazardPointCrud'

// ---------------------------------------------------------------------------
// Chart config
// ---------------------------------------------------------------------------

const CHART_COLORS = [
    '#5470C6', '#91CC75', '#FAC858', '#EE6666', '#73C0DE',
    '#3BA272', '#FC8452', '#9A60B4', '#EA7CCC', '#909399',
]

// ---------------------------------------------------------------------------
// Composable
// ---------------------------------------------------------------------------

export interface UseHazardPointMonitorOptions {
    currentRow: Ref<HazardPointItem | null>
    /** 从何处被调用（detail dialog 内的 activeTab） */
    activeTab: Ref<string>
}

export function useHazardPointMonitor(opts: UseHazardPointMonitorOptions) {
    // ── State ──
    const dataDisplayMode = ref<'chart' | 'table'>('chart')
    const monitorDataList = ref<MonitorDataPageItem[]>([])
    const chartSeriesData = ref<ChartData[]>([])
    const latestDataList = ref<LatestDataItem[]>([])
    const monitorSensors = ref<{ id: number; name: string }[]>([])
    const monitorAttrs = ref<{ code: string; label: string }[]>([])
    const monitorSensorMap = ref<Map<number, any>>(new Map())

    const chartOptions = ref({
        series: [] as any[],
        chart: {} as any,
        xaxis: {} as any,
        yaxis: {} as any,
        stroke: {} as any,
        fill: {} as any,
        legend: {} as any,
        tooltip: {} as any,
        dataLabels: {} as any,
        grid: {} as any,
        colors: [] as string[],
        markers: {} as any,
    })

    const dataFilter = reactive({
        deviceId: '' as string | number,
        sensorId: '' as string | number,
        attrCode: '',
        valueType: 'current',
        timeRange: null as [string, string] | null,
    })

    // ── Latest data ──
    const initLatestData = async (hazardPointId: string) => {
        try {
            latestDataList.value = await getLatestData(Number(hazardPointId))
        } catch {
            latestDataList.value = []
        }
    }

    // ── Device → sensors ──
    const onDataDeviceChange = async (deviceId: string | number) => {
        dataFilter.sensorId = ''
        dataFilter.attrCode = ''
        monitorSensors.value = []
        monitorAttrs.value = []
        if (!deviceId) return
        try {
            const sensors = await getDeviceSensors(Number(deviceId))
            const map = new Map(monitorSensorMap.value)
            for (const s of sensors) {
                if (s.id != null) {
                    map.set(s.id, s)
                    monitorSensors.value.push({id: s.id, name: s.sensorName})
                }
            }
            monitorSensorMap.value = map
        } catch { /* ignore */
        }
    }

    // ── Sensor → attrs ──
    const onDataSensorChange = (sensorId: string | number) => {
        dataFilter.attrCode = ''
        if (!sensorId) {
            monitorAttrs.value = [];
            return
        }
        const sensor = monitorSensorMap.value.get(Number(sensorId))
        monitorAttrs.value = (sensor?.attrList || []).map((a: any) => ({
            code: a.attrCode,
            label: `${a.attrName || a.attrCode}${a.unit ? ` (${a.unit})` : ''}`,
        }))
    }

    // ── Chart ──
    const buildChartOptions = () => {
        const seriesData = chartSeriesData.value
        if (seriesData.length === 0) return
        const allLabels = new Set<string>()
        for (const s of seriesData) for (const l of s.labels) allLabels.add(l)
        const xCategories = Array.from(allLabels).sort()

        chartOptions.value = {
            chart: {
                type: 'area' as const,
                height: '100%',
                fontFamily: 'inherit',
                toolbar: {
                    tools: {
                        download: true,
                        selection: true,
                        zoom: true,
                        zoomin: true,
                        zoomout: true,
                        pan: true,
                        reset: true
                    }
                },
                zoom: {enabled: true, type: 'x' as const},
                animations: {enabled: true, easing: 'easeinout' as const, speed: 800}
            },
            colors: CHART_COLORS, dataLabels: {enabled: false},
            stroke: {curve: 'smooth' as const, width: 2},
            fill: {type: 'gradient', gradient: {shadeIntensity: 1, opacityFrom: 0.2, opacityTo: 0.02, stops: [0, 100]}},
            markers: {size: 0, hover: {size: 5}},
            grid: {borderColor: '#e7e7e7', strokeDashArray: 4, padding: {top: 10, right: 10, bottom: 5, left: 10}},
            legend: {
                position: 'top' as const,
                horizontalAlign: 'center' as const,
                fontSize: '13px',
                fontWeight: 500,
                markers: {width: 12, height: 12, radius: 6, offsetX: -4},
                itemMargin: {horizontal: 16, vertical: 4},
                offsetY: -4
            },
            xaxis: {
                type: 'category' as const,
                categories: xCategories,
                labels: {rotate: -30, style: {fontSize: '11px', colors: '#666'}},
                tickAmount: Math.min(xCategories.length, 10),
                tooltip: {enabled: false}
            },
            yaxis: {
                title: {text: seriesData[0]?.unit || '', style: {fontSize: '12px', color: '#888'}},
                labels: {formatter: (val: number) => val != null ? Number(val.toFixed(2)).toString() : ''}
            },
            tooltip: {shared: true, intersect: false},
            series: seriesData.map((s) => {
                const points = s.labels.map((l, i) => ({x: l, y: s.values[i]}))
                return {name: s.seriesName, data: points}
            }),
        }
    }

    // ── Query ──
    const handleQueryData = async () => {
        if (!opts.currentRow.value) {
            ElMessage.warning('请先选择隐患点');
            return
        }
        const baseParams: Record<string, unknown> = {
            hazardPointId: Number(opts.currentRow.value.id),
            valueType: dataFilter.valueType || undefined,
            startTime: dataFilter.timeRange?.[0] || undefined,
            endTime: dataFilter.timeRange?.[1] || undefined,
        }
        if (dataFilter.deviceId) baseParams.deviceId = Number(dataFilter.deviceId)
        if (dataFilter.sensorId) baseParams.sensorId = Number(dataFilter.sensorId)
        if (dataFilter.attrCode) baseParams.attrCode = dataFilter.attrCode

        if (dataDisplayMode.value === 'chart') {
            await queryChart(baseParams)
        } else {
            await queryPage(baseParams)
        }
    }

    const queryChart = async (baseParams: Record<string, unknown>) => {
        if (!baseParams.startTime || !baseParams.endTime) {
            ElMessage.warning('图表模式需要选择时间范围');
            return
        }
        try {
            const series = await getChartData({
                hazardPointId: baseParams.hazardPointId as number,
                deviceId: baseParams.deviceId as number | undefined,
                sensorId: baseParams.sensorId as number | undefined,
                attrCode: baseParams.attrCode as string | undefined,
                valueType: baseParams.valueType as string | undefined,
                startTime: baseParams.startTime as string,
                endTime: baseParams.endTime as string,
            })
            chartSeriesData.value = series
            ElMessage.success(`加载 ${series.length} 条曲线，共 ${series[0]?.labels.length || 0} 个数据点`)
            await nextTick();
            buildChartOptions()
        } catch (error) {
            showRequestErrorMessage(error, '获取图表数据失败')
        }
    }

    const queryPage = async (baseParams: Record<string, unknown>) => {
        try {
            const res = await getMonitorDataPage({
                hazardPointId: baseParams.hazardPointId as number,
                deviceId: baseParams.deviceId as number | undefined,
                sensorId: baseParams.sensorId as number | undefined,
                attrCode: baseParams.attrCode as string | undefined,
                valueType: baseParams.valueType as string | undefined,
                startTime: baseParams.startTime as string | undefined,
                endTime: baseParams.endTime as string | undefined,
                pageNum: 1, pageSize: 100,
            })
            monitorDataList.value = res.rows || []
            ElMessage.success(`加载 ${monitorDataList.value.length} 条数据`)
        } catch (error) {
            showRequestErrorMessage(error, '获取监测数据失败')
        }
    }

    const handleImportData = () => {
        ElMessage.info('导入功能开发中，敬请期待')
    }
    const handleExportData = () => {
        ElMessage.info('导出功能开发中，敬请期待')
    }

    // ── Watchers ──
    watch(dataDisplayMode, (mode) => {
        if (mode === 'chart') nextTick(() => buildChartOptions())
    })

    watch(() => opts.activeTab.value, (tab) => {
        if (tab === 'monitorData') {
            if (!dataFilter.timeRange) {
                const end = new Date()
                const start = new Date(end.getTime() - 3 * 24 * 60 * 60 * 1000)
                const fmt = (d: Date) => {
                    const pad = (n: number) => String(n).padStart(2, '0');
                    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
                }
                dataFilter.timeRange = [fmt(start), fmt(end)]
            }
            if (dataDisplayMode.value === 'chart') nextTick(() => buildChartOptions())
        }
    })

    return {
        dataDisplayMode,
        monitorDataList,
        chartSeriesData,
        chartOptions,
        latestDataList,
        monitorSensors,
        monitorAttrs,
        dataFilter,
        initLatestData,
        onDataDeviceChange,
        onDataSensorChange,
        handleQueryData,
        handleImportData,
        handleExportData,
    }
}
