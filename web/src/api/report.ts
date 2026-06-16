import request from '@/utils/request'
import type { PageResult } from './system'

// ---------------------------------------------------------------------------
// Interfaces
// ---------------------------------------------------------------------------

// --- Report ---
export interface ReportItem {
  id: number
  title: string
  type: 'weekly' | 'monthly'
  periodStart: string
  periodEnd: string
  createTime: string
  content: string // HTML rich text
}

export interface ReportPageParams {
  pageNum: number
  pageSize: number
  keyword?: string
  type?: 'weekly' | 'monthly' | ''
  startDate?: string
  endDate?: string
}

// --- Query ---
export interface MonitorQueryParams {
  hazardPointId?: number | ''
  deviceType?: number | ''
  deviceId?: number | ''
  attrCodes?: string[]
  startTime?: string
  endTime?: string
  pageNum: number
  pageSize: number
}

// --- Analysis ---
export interface SensorSeriesItem {
  id: string
  hazardPointId: number
  hazardPointName: string
  deviceId: number
  deviceName: string
  sensorId: number
  sensorName: string
  attrCode: string
  attrName: string
  unit: string
  color: string
}

export interface ChartDataItem {
  times: string[]
  values: number[]
}

// --- Options for dropdowns ---
export interface HazardPointOption {
  id: number
  name: string
}

export interface DeviceOption {
  id: number
  name: string
  deviceType: number
  boundHazardPointId: number
}

export interface DeviceTypeOption {
  value: number
  label: string
  attrs: { code: string; name: string; unit: string }[]
}

export interface GridChartItem {
  index: number
  sensorSeriesId?: string
  title?: string
  hazardPointId?: number
  deviceId?: number
  attrCode?: string
  attrName?: string
  unit?: string
}

// ---------------------------------------------------------------------------
// Deterministic pseudo-random helpers (seeded, no Math.random)
// ---------------------------------------------------------------------------

/** Simple seeded PRNG — returns a float in [0, 1) */
function seededRandom(seed: number): () => number {
  let s = seed
  return () => {
    s = (s * 16807 + 0) % 2147483647
    return (s - 1) / 2147483646
  }
}

/** Return a deterministic float in [min, max) using the given rng */
function randRange(rng: () => number, min: number, max: number): number {
  return min + rng() * (max - min)
}

/** Round to fixed decimals */
function toFixed(value: number, decimals: number): number {
  const factor = Math.pow(10, decimals)
  return Math.round(value * factor) / factor
}

// ---------------------------------------------------------------------------
// Mock data generators
// ---------------------------------------------------------------------------

const HAZARD_POINTS: HazardPointOption[] = [
  { id: 1, name: '王家坪滑坡' },
  { id: 2, name: '李家沟泥石流' },
  { id: 3, name: '赵家坡危岩体' },
  { id: 4, name: '张家湾崩塌' },
  { id: 5, name: '刘家坳滑坡' },
]

const DEVICE_TYPES: DeviceTypeOption[] = [
  {
    value: 1,
    label: '位移计',
    attrs: [
      { code: 'disp_x', name: 'X方向位移', unit: 'mm' },
      { code: 'disp_y', name: 'Y方向位移', unit: 'mm' },
      { code: 'disp_z', name: 'Z方向位移', unit: 'mm' },
    ],
  },
  {
    value: 2,
    label: '雨量计',
    attrs: [
      { code: 'rainfall', name: '降雨量', unit: 'mm' },
      { code: 'rainfall_daily', name: '日累计雨量', unit: 'mm' },
    ],
  },
  {
    value: 3,
    label: '倾角传感器',
    attrs: [
      { code: 'tilt_x', name: 'X倾角', unit: '°' },
      { code: 'tilt_y', name: 'Y倾角', unit: '°' },
    ],
  },
  {
    value: 4,
    label: '土压力计',
    attrs: [
      { code: 'earth_pressure', name: '土压力', unit: 'kPa' },
    ],
  },
]

// 12 devices spread across hazard points (3-4 per hazard point)
const DEVICES: DeviceOption[] = [
  { id: 101, name: 'WJP-WY-01', deviceType: 1, hazardPointId: 1 },
  { id: 102, name: 'WJP-YL-01', deviceType: 2, hazardPointId: 1 },
  { id: 103, name: 'WJP-QJ-01', deviceType: 3, hazardPointId: 1 },
  { id: 201, name: 'LJG-YL-01', deviceType: 2, hazardPointId: 2 },
  { id: 202, name: 'LJG-WY-01', deviceType: 1, hazardPointId: 2 },
  { id: 203, name: 'LJG-QJ-01', deviceType: 3, hazardPointId: 2 },
  { id: 204, name: 'LJG-TY-01', deviceType: 4, hazardPointId: 2 },
  { id: 301, name: 'ZJP-WY-01', deviceType: 1, hazardPointId: 3 },
  { id: 302, name: 'ZJP-QJ-01', deviceType: 3, hazardPointId: 3 },
  { id: 303, name: 'ZJP-TY-01', deviceType: 4, hazardPointId: 3 },
  { id: 401, name: 'ZJW-WY-01', deviceType: 1, hazardPointId: 4 },
  { id: 402, name: 'ZJW-YL-01', deviceType: 2, hazardPointId: 4 },
  { id: 403, name: 'ZJW-TY-01', deviceType: 4, hazardPointId: 4 },
  { id: 501, name: 'LJA-WY-01', deviceType: 1, hazardPointId: 5 },
  { id: 502, name: 'LJA-YL-01', deviceType: 2, hazardPointId: 5 },
]

/** Format a Date to YYYY-MM-DD */
function formatDate(d: Date): string {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

/** Format a Date to YYYY-MM-DD HH:mm:ss */
function formatDateTime(d: Date): string {
  const date = formatDate(d)
  const h = String(d.getHours()).padStart(2, '0')
  const min = String(d.getMinutes()).padStart(2, '0')
  const s = String(d.getSeconds()).padStart(2, '0')
  return `${date} ${h}:${min}:${s}`
}

function generateMockReports(): ReportItem[] {
  const now = new Date()
  const reports: ReportItem[] = []

  const weeklyAnalysisTexts = [
    '本周各监测点数据整体平稳，位移变化速率在正常范围内。王家坪滑坡X方向累计位移增长约0.3mm，需持续关注。',
    '本周降雨量较上周有所增加，李家沟泥石流监测点日累计雨量峰值达到38mm，建议加强巡视。',
    '本周赵家坡危岩体倾角传感器数据显示微小波动，X倾角变化不超过0.05°，暂无异常。',
    '本周张家湾崩塌区域土压力数据稳定，各监测点数值波动范围在±0.2kPa以内。',
    '本周刘家坳滑坡位移计数据呈缓慢增长趋势，Y方向累计位移增加0.5mm，建议密切关注。',
    '本周所有监测点设备运行正常，数据采集完整率达到99.2%，未发现异常数据。',
    '本周受降雨影响，部分监测点数据波动较大，已自动触发预警评估，暂无需人工干预。',
    '本周王家坪滑坡与刘家坳滑坡监测数据相关性分析表明，两者变形趋势基本一致。',
  ]

  const monthlyAnalysisTexts = [
    '本月各隐患点监测数据汇总分析如下：位移类传感器数据整体呈稳定趋势，仅在个别降雨集中时段出现短暂波动。雨量数据与位移变化具有明显相关性，建议在强降雨期间加密监测频率。本月未达到黄色预警阈值。',
    '本月综合分析结果表明，各监测点处于基本稳定状态。李家沟泥石流沟道在7月中旬经历一次强降雨过程，累计位移有所增加但仍在安全范围内。建议下月重点关注雨季期间的监测数据变化。',
    '本月赵家坡危岩体倾角数据无明显异常，土压力变化在合理范围内。设备在线率达到98.5%，数据质量良好。建议下月对2号倾角传感器进行现场标定校验。',
    '本月张家湾崩塌区域整体稳定，各监测指标变化量较小。本月进行了两次现场巡查，巡查结果与监测数据吻合。建议继续按当前频率进行监测。',
    '本月刘家坳滑坡位移持续缓慢增长，月累计位移量约为1.2mm，变形速率较上月略有增加。建议提高关注等级，加强人工巡查频次。',
    '本月全区域监测设备运行状况良好，数据完整率99.0%。共触发2次蓝色预警，经核实均为降雨引起的正常波动，已自动解除。下月将进入主汛期，需做好应急准备工作。',
    '本月综合监测报告：各隐患点整体安全。建议下月对王家坪滑坡布设的3台位移计进行年度检定，确保数据准确性。',
  ]

  // Generate weekly reports (8 reports going back ~2 months)
  for (let i = 0; i < 8; i++) {
    const endOfWeek = new Date(now)
    endOfWeek.setDate(now.getDate() - i * 7)
    const startOfWeek = new Date(endOfWeek)
    startOfWeek.setDate(endOfWeek.getDate() - 6)

    const periodStart = formatDate(startOfWeek)
    const periodEnd = formatDate(endOfWeek)
    const created = new Date(endOfWeek)
    created.setHours(9, 0, 0)

    const analysis = weeklyAnalysisTexts[i % weeklyAnalysisTexts.length]

    const content = buildWeeklyReportContent(periodStart, periodEnd, analysis)

    reports.push({
      id: 1000 + i,
      title: `监测周报 (${periodStart} ~ ${periodEnd})`,
      type: 'weekly',
      periodStart,
      periodEnd,
      createTime: formatDateTime(created),
      content,
    })
  }

  // Generate monthly reports (7 reports going back ~7 months)
  for (let i = 0; i < 7; i++) {
    const monthDate = new Date(now.getFullYear(), now.getMonth() - i, 1)
    const year = monthDate.getFullYear()
    const month = monthDate.getMonth()
    const periodStart = `${year}-${String(month + 1).padStart(2, '0')}-01`
    const lastDay = new Date(year, month + 1, 0).getDate()
    const periodEnd = `${year}-${String(month + 1).padStart(2, '0')}-${String(lastDay).padStart(2, '0')}`

    const created = new Date(year, month + 1, 2, 10, 0, 0)

    const analysis = monthlyAnalysisTexts[i % monthlyAnalysisTexts.length]

    const content = buildMonthlyReportContent(periodStart, periodEnd, analysis, i)

    reports.push({
      id: 2000 + i,
      title: `${year}年${month + 1}月监测月报`,
      type: 'monthly',
      periodStart,
      periodEnd,
      createTime: formatDateTime(created),
      content,
    })
  }

  return reports
}

function buildWeeklyReportContent(periodStart: string, periodEnd: string, analysis: string): string {
  return `<h2>地质灾害监测周报</h2>
<p><strong>报告周期：</strong>${periodStart} 至 ${periodEnd}</p>
<table border="1" cellpadding="6" cellspacing="0" style="border-collapse:collapse;width:100%;text-align:center;">
  <thead>
    <tr style="background:#f0f5ff;">
      <th>监测点</th>
      <th>设备类型</th>
      <th>数据完整率</th>
      <th>最大变化量</th>
      <th>预警状态</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>王家坪滑坡</td>
      <td>位移计</td>
      <td>99.5%</td>
      <td>0.32mm</td>
      <td>正常</td>
    </tr>
    <tr>
      <td>李家沟泥石流</td>
      <td>雨量计</td>
      <td>100%</td>
      <td>38.2mm</td>
      <td>正常</td>
    </tr>
    <tr>
      <td>赵家坡危岩体</td>
      <td>倾角传感器</td>
      <td>98.8%</td>
      <td>0.05°</td>
      <td>正常</td>
    </tr>
    <tr>
      <td>张家湾崩塌</td>
      <td>土压力计</td>
      <td>99.1%</td>
      <td>0.18kPa</td>
      <td>正常</td>
    </tr>
    <tr>
      <td>刘家坳滑坡</td>
      <td>位移计</td>
      <td>99.8%</td>
      <td>0.51mm</td>
      <td>关注</td>
    </tr>
  </tbody>
</table>
<h2>分析说明</h2>
<p>${analysis}</p>
<ul>
  <li>建议持续关注刘家坳滑坡变形趋势</li>
  <li>雨季期间适当加密监测频率</li>
  <li>确保各监测点设备供电及通信正常</li>
</ul>`
}

function buildMonthlyReportContent(periodStart: string, periodEnd: string, analysis: string, seed: number): string {
  const rng = seededRandom(seed * 37 + 7)
  const onlineRate = toFixed(randRange(rng, 97.5, 99.9), 1)
  const dataCompleteRate = toFixed(randRange(rng, 98.0, 99.8), 1)
  const blueAlertCount = Math.floor(randRange(rng, 0, 4))
  const yellowAlertCount = Math.floor(randRange(rng, 0, 2))

  return `<h2>地质灾害监测月报</h2>
<p><strong>报告周期：</strong>${periodStart} 至 ${periodEnd}</p>
<table border="1" cellpadding="6" cellspacing="0" style="border-collapse:collapse;width:100%;text-align:center;">
  <thead>
    <tr style="background:#f0f5ff;">
      <th>监测点</th>
      <th>监测设备数</th>
      <th>数据完整率</th>
      <th>月累计变化量</th>
      <th>最大日变化量</th>
      <th>预警次数</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>王家坪滑坡</td>
      <td>3</td>
      <td>${toFixed(randRange(rng, 98.5, 100), 1)}%</td>
      <td>${toFixed(randRange(rng, 0.3, 1.5), 2)}mm</td>
      <td>${toFixed(randRange(rng, 0.05, 0.2), 2)}mm</td>
      <td>${Math.floor(randRange(rng, 0, 2))}</td>
    </tr>
    <tr>
      <td>李家沟泥石流</td>
      <td>4</td>
      <td>${toFixed(randRange(rng, 99.0, 100), 1)}%</td>
      <td>${toFixed(randRange(rng, 0.5, 2.0), 2)}mm</td>
      <td>${toFixed(randRange(rng, 0.1, 0.5), 2)}mm</td>
      <td>${Math.floor(randRange(rng, 0, 3))}</td>
    </tr>
    <tr>
      <td>赵家坡危岩体</td>
      <td>3</td>
      <td>${toFixed(randRange(rng, 98.0, 99.5), 1)}%</td>
      <td>${toFixed(randRange(rng, 0.01, 0.1), 3)}°</td>
      <td>${toFixed(randRange(rng, 0.005, 0.03), 3)}°</td>
      <td>0</td>
    </tr>
    <tr>
      <td>张家湾崩塌</td>
      <td>3</td>
      <td>${toFixed(randRange(rng, 98.5, 99.8), 1)}%</td>
      <td>${toFixed(randRange(rng, 0.1, 0.5), 2)}kPa</td>
      <td>${toFixed(randRange(rng, 0.02, 0.1), 2)}kPa</td>
      <td>${Math.floor(randRange(rng, 0, 1))}</td>
    </tr>
    <tr>
      <td>刘家坳滑坡</td>
      <td>2</td>
      <td>${toFixed(randRange(rng, 99.0, 100), 1)}%</td>
      <td>${toFixed(randRange(rng, 0.8, 2.5), 2)}mm</td>
      <td>${toFixed(randRange(rng, 0.1, 0.4), 2)}mm</td>
      <td>${Math.floor(randRange(rng, 1, 3))}</td>
    </tr>
  </tbody>
</table>
<h2>综合统计</h2>
<table border="1" cellpadding="6" cellspacing="0" style="border-collapse:collapse;width:100%;text-align:center;">
  <thead>
    <tr style="background:#fff7e6;">
      <th>指标</th>
      <th>数值</th>
    </tr>
  </thead>
  <tbody>
    <tr><td>监测点总数</td><td>5</td></tr>
    <tr><td>在线设备数</td><td>15</td></tr>
    <tr><td>设备在线率</td><td>${onlineRate}%</td></tr>
    <tr><td>数据完整率</td><td>${dataCompleteRate}%</td></tr>
    <tr><td>蓝色预警次数</td><td>${blueAlertCount}</td></tr>
    <tr><td>黄色预警次数</td><td>${yellowAlertCount}</td></tr>
    <tr><td>橙色预警次数</td><td>0</td></tr>
    <tr><td>红色预警次数</td><td>0</td></tr>
  </tbody>
</table>
<h2>分析说明</h2>
<p>${analysis}</p>
<ul>
  <li>本月所有隐患点未触发橙色及以上级别预警</li>
  <li>设备运行状况良好，数据采集基本完整</li>
  <li>建议下月继续按照既定监测方案执行</li>
</ul>`
}

/** Generate mock query data (time-series rows) */
function getMockQueryData(params: MonitorQueryParams): PageResult<Record<string, any>> {
  const { deviceType = 1, pageNum = 1, pageSize = 20 } = params
  const rng = seededRandom(Number(deviceType) * 100 + pageNum * 7 + pageSize)

  const devType = DEVICE_TYPES.find((t) => t.value === Number(deviceType)) || DEVICE_TYPES[0]
  const filteredDevices = DEVICES.filter((d) => d.deviceType === Number(deviceType))
  if (filteredDevices.length === 0) return { rows: [], total: 0, pageNum, pageSize }

  const totalRows = 60
  const rows: Record<string, any>[] = []

  const startIdx = (pageNum - 1) * pageSize
  const endIdx = Math.min(startIdx + pageSize, totalRows)

  for (let i = startIdx; i < endIdx; i++) {
    const device = filteredDevices[i % filteredDevices.length]
    const timeOffset = (totalRows - i) * 2 // hours ago
    const time = new Date()
    time.setHours(time.getHours() - timeOffset)

    const row: Record<string, any> = {
      time: formatDateTime(time),
      deviceName: device.name,
      hazardPointName: HAZARD_POINTS.find((h) => h.id === device.hazardPointId)?.name || '',
    }

    for (const attr of devType.attrs) {
      let val: number
      switch (attr.code) {
        case 'disp_x':
        case 'disp_y':
        case 'disp_z':
          val = toFixed(randRange(rng, 0.1, 5.0), 2)
          break
        case 'rainfall':
          val = toFixed(randRange(rng, 0, 50), 1)
          break
        case 'rainfall_daily':
          val = toFixed(randRange(rng, 0, 80), 1)
          break
        case 'tilt_x':
        case 'tilt_y':
          val = toFixed(randRange(rng, -2.0, 2.0), 3)
          break
        case 'earth_pressure':
          val = toFixed(randRange(rng, 10, 120), 2)
          break
        default:
          val = toFixed(randRange(rng, 0, 100), 2)
      }
      row[attr.code] = val
    }

    rows.push(row)
  }

  return { rows, total: totalRows, pageNum, pageSize }
}

/** Generate mock chart data (100 points over 7 days) */
function getMockChartData(
  deviceId: number,
  attrCode: string,
  startTime: string,
  _endTime: string
): ChartDataItem {
  const rng = seededRandom(deviceId * 31 + attrCode.charCodeAt(0) * 17 + startTime.charCodeAt(5) * 3)

  // Parse startTime safely — replace space with 'T' for ISO 8601 compatibility
  const start = new Date(startTime.replace(' ', 'T'))
  const times: string[] = []
  const values: number[] = []

  // 100 points over 7 days
  const totalMinutes = 7 * 24 * 60
  const interval = totalMinutes / 100

  for (let i = 0; i < 100; i++) {
    const t = new Date(start.getTime() + i * interval * 60 * 1000)
    times.push(formatDateTime(t))

    let val: number
    switch (attrCode) {
      case 'disp_x':
      case 'disp_y':
      case 'disp_z':
        val = toFixed(randRange(rng, 0.1, 5.0), 2)
        break
      case 'rainfall':
        val = toFixed(randRange(rng, 0, 50), 1)
        break
      case 'rainfall_daily':
        val = toFixed(randRange(rng, 0, 80), 1)
        break
      case 'tilt_x':
      case 'tilt_y':
        val = toFixed(randRange(rng, -2.0, 2.0), 3)
        break
      case 'earth_pressure':
        val = toFixed(randRange(rng, 10, 120), 2)
        break
      default:
        val = toFixed(randRange(rng, 0, 100), 2)
    }
    values.push(val)
  }

  return { times, values }
}

// ---------------------------------------------------------------------------
// API functions (try real API, fall back to mock)
// ---------------------------------------------------------------------------

/** Fetch a page of reports (mock) */
export async function getReportPage(params: ReportPageParams): Promise<PageResult<ReportItem>> {

  //  return request.get<PageResult<ReportItem>>('/out/report/list', {params})

  // const all = generateMockReports()
  // let filtered = all
  // if (params.keyword) {
  //   const kw = params.keyword.toLowerCase()
  //   filtered = filtered.filter((r) => r.title.toLowerCase().includes(kw))
  // }
  // if (params.type) {
  //   filtered = filtered.filter((r) => r.type === params.type)
  // }
  // if (params.startDate) {
  //   filtered = filtered.filter((r) => r.periodStart >= params.startDate!)
  // }
  // if (params.endDate) {
  //   filtered = filtered.filter((r) => r.periodEnd <= params.endDate!)
  // }
  // const start = (params.pageNum - 1) * params.pageSize
  // const rows = filtered.slice(start, start + params.pageSize)
  // return { rows, total: filtered.length, pageNum: params.pageNum, pageSize: params.pageSize }
}

/** Fetch a single report detail (mock) */
export async function getReportDetail(id: number): Promise<ReportItem> {
  const all = generateMockReports()
  const found = all.find((r) => r.id === id)
  if (found) return found
  throw new Error(`Report ${id} not found`)
}

/** Delete a report (mock: no-op) */
export async function deleteReport(_id: number): Promise<void> {}

/** Fetch hazard point options (mock) */
export async function getHazardPointOptions(): Promise<HazardPointOption[]> {
  let msg = await request.get<HazardPointOption[]>('/hazard-points/page');
  let rst = msg.data.rows;
  return rst;
  // return HAZARD_POINTS
}

/** Fetch device type options with attributes (mock) */
export async function getDeviceTypeOptions(): Promise<DeviceTypeOption[]> {
  return DEVICE_TYPES
}

/** Fetch device options, optionally filtered (mock) */
export async function getDeviceOptions(params: {
  hazardPointId?: number
  deviceType?: number
}): Promise<DeviceOption[]> {
  // let filtered = [...DEVICES]
  let devices = await request.get<DeviceOption[]>(`/devices/page?pageNum=1&pageSize=20&boundHazardPointId=${params.hazardPointId}`);
    let filtered = devices.data.rows;
  // if (params.hazardPointId) {
  //   filtered = filtered.filter((d) => d.hazardPointId === params.hazardPointId)
  // }
  // if (params.deviceType) {
  //   filtered = filtered.filter((d) => d.deviceType === params.deviceType)
  // }



  return filtered
}

/** Fetch paginated monitor query data (mock) */
export async function getMonitorQueryData(
  params: MonitorQueryParams
): Promise<PageResult<Record<string, any>>> {
  return getMockQueryData(params)
}

/** Fetch chart data for a single device+attribute (mock) */
export async function getChartData(params: {
  deviceId: number
  attrCode: string
  startTime: string
  endTime: string
}): Promise<ChartDataItem> {
  return getMockChartData(params.deviceId, params.attrCode, params.startTime, params.endTime)
}

/** Fetch chart data for multiple grid items (mock) */
export async function getGridChartData(
  items: GridChartItem[],
  startTime: string,
  endTime: string
): Promise<Map<number, ChartDataItem>> {
  const result = new Map<number, ChartDataItem>()
  for (const item of items) {
    if (item.deviceId && item.attrCode) {
      result.set(item.index, getMockChartData(item.deviceId, item.attrCode, startTime, endTime))
    }
  }
  return result
}
