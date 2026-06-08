# 边坡监测预警小程序 实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 基于现有烟感监测框架重构为边坡监测预警系统，实现5个Tab页面及关联详情页

**Architecture:** 保留现有 Vue 3 + Pinia + Wot Design Uni + ECharts 框架，重写页面内容，复用通用组件（EmptyState、Skeleton、Echarts），页面内使用模拟数据

**Tech Stack:** uniapp, Vue 3 Composition API, TypeScript, SCSS, ECharts 5.5.0, Wot Design Uni

---

## Task 1: 配置文件与图标

**Files:**
- Modify: `src/pages.json`
- Modify: `src/manifest.json`
- Create: `src/static/icons/hazard.png`, `src/static/icons/hazard-active.png`
- Create: `src/static/icons/device-tab.png`, `src/static/icons/device-tab-active.png`

**Step 1: 更新 manifest.json 应用名称**

将 `"name": "烟感监测"` 改为 `"name": "边坡监测预警"`

**Step 2: 更新 pages.json**

替换整个 pages.json 内容：

```json
{
  "pages": [
    {
      "path": "pages/login",
      "style": {
        "navigationBarTitleText": "登录",
        "navigationStyle": "custom"
      }
    },
    {
      "path": "pages/index",
      "style": {
        "navigationBarTitleText": "事件大厅",
        "navigationStyle": "custom"
      }
    },
    {
      "path": "pages/hazard",
      "style": {
        "navigationBarTitleText": "隐患点",
        "navigationStyle": "custom"
      }
    },
    {
      "path": "pages/device",
      "style": {
        "navigationBarTitleText": "设备库",
        "navigationStyle": "custom"
      }
    },
    {
      "path": "pages/chart",
      "style": {
        "navigationBarTitleText": "监测数据",
        "navigationStyle": "custom"
      }
    },
    {
      "path": "pages/profile",
      "style": {
        "navigationBarTitleText": "个人中心",
        "navigationStyle": "custom"
      }
    },
    {
      "path": "pages/alarm-detail",
      "style": {
        "navigationBarTitleText": "告警详情",
        "navigationStyle": "custom"
      }
    },
    {
      "path": "pages/hazard-detail",
      "style": {
        "navigationBarTitleText": "隐患点详情",
        "navigationStyle": "custom"
      }
    },
    {
      "path": "pages/device-detail",
      "style": {
        "navigationBarTitleText": "设备详情",
        "navigationStyle": "custom"
      }
    }
  ],
  "globalStyle": {
    "backgroundColor": "#f7f8fc",
    "backgroundTextStyle": "dark",
    "navigationBarBackgroundColor": "#3068e4",
    "navigationBarTextStyle": "white",
    "navigationBarTitleText": "边坡监测预警",
    "navigationStyle": "default",
    "pullToRefresh": false,
    "bounce": "none"
  },
  "tabBar": {
    "color": "#6b7280",
    "selectedColor": "#3068e4",
    "backgroundColor": "#ffffff",
    "borderStyle": "black",
    "list": [
      {
        "pagePath": "pages/index",
        "text": "事件大厅",
        "iconPath": "static/icons/bell.png",
        "selectedIconPath": "static/icons/bell-active.png"
      },
      {
        "pagePath": "pages/hazard",
        "text": "隐患点",
        "iconPath": "static/icons/hazard.png",
        "selectedIconPath": "static/icons/hazard-active.png"
      },
      {
        "pagePath": "pages/device",
        "text": "设备库",
        "iconPath": "static/icons/device-tab.png",
        "selectedIconPath": "static/icons/device-tab-active.png"
      },
      {
        "pagePath": "pages/chart",
        "text": "监测数据",
        "iconPath": "static/icons/chart.png",
        "selectedIconPath": "static/icons/chart-active.png"
      },
      {
        "pagePath": "pages/profile",
        "text": "个人中心",
        "iconPath": "static/icons/user.png",
        "selectedIconPath": "static/icons/user-active.png"
      }
    ]
  },
  "subPackages": [],
  "easycom": {
    "autoscan": true,
    "custom": {
      "^zui-svg-icon$": "@/uni_modules/zui-svg-icon/components/zui-svg-icon/zui-svg-icon.vue",
      "^EmptyState$": "@/components/EmptyState.vue",
      "^Skeleton$": "@/components/Skeleton.vue",
      "^StatBubble$": "@/components/StatBubble.vue"
    }
  }
}
```

**Step 3: 创建 tab 图标占位**

复制现有 bell.png 为 hazard.png / device-tab.png 的占位（后续替换正式图标）：

```bash
cp src/static/icons/bell.png src/static/icons/hazard.png
cp src/static/icons/bell-active.png src/static/icons/hazard-active.png
cp src/static/icons/chart.png src/static/icons/device-tab.png
cp src/static/icons/chart-active.png src/static/icons/device-tab-active.png
```

---

## Task 2: 模拟数据

**Files:**
- Rewrite: `src/utils/alarm.ts`
- Create: `src/utils/hazard.ts`
- Rewrite: `src/utils/device.ts`

**Step 1: 重写 alarm.ts 模拟数据**

```typescript
// src/utils/alarm.ts

// 告警级别：1-蓝 2-黄 3-橙 4-红
export interface Alarm {
  id: number
  hazardId: number
  hazardName: string
  deviceId: number
  deviceName: string
  alarmType: string       // 阈值预警 | 综合预警
  alarmLevel: number      // 1-蓝 2-黄 3-橙 4-红
  alarmContent: string
  alarmValue: string
  status: number          // 0-待处理 1-已处理
  createTime: string
  handleTime?: string
  handleRemark?: string
  alarmCount: number      // 告警次数
  dispatchLogs: DispatchLog[]
}

export interface DispatchLog {
  id: number
  action: string
  operator: string
  time: string
}

const mockAlarms: Alarm[] = [
  {
    id: 1, hazardId: 1, hazardName: 'K15+200 右侧边坡',
    deviceId: 1, deviceName: 'GNSS-001',
    alarmType: '阈值预警', alarmLevel: 4,
    alarmContent: '深埋位移超过红色预警阈值，当前值 12.5mm，阈值 10mm',
    alarmValue: '12.5mm', status: 0,
    createTime: '2026-06-08 10:30:00',
    alarmCount: 5,
    dispatchLogs: [
      { id: 1, action: '系统自动告警', operator: '系统', time: '2026-06-08 10:30:00' },
      { id: 2, action: '短信通知责任人', operator: '系统', time: '2026-06-08 10:30:05' },
      { id: 3, action: 'app推送通知', operator: '系统', time: '2026-06-08 10:30:10' },
    ]
  },
  {
    id: 2, hazardId: 2, hazardName: 'K23+500 左侧边坡',
    deviceId: 4, deviceName: 'GNSS-004',
    alarmType: '综合预警', alarmLevel: 3,
    alarmContent: '综合分析：位移速率加快且雨量持续增大，综合评估为橙色预警',
    alarmValue: '-', status: 0,
    createTime: '2026-06-08 09:15:00',
    alarmCount: 3,
    dispatchLogs: [
      { id: 1, action: '综合评估触发', operator: '系统', time: '2026-06-08 09:15:00' },
      { id: 2, action: '短信通知责任人', operator: '系统', time: '2026-06-08 09:15:05' },
    ]
  },
  {
    id: 3, hazardId: 3, hazardName: 'K31+100 右侧边坡',
    deviceId: 7, deviceName: 'RAIN-001',
    alarmType: '阈值预警', alarmLevel: 2,
    alarmContent: '小时雨量超过黄色预警阈值，当前值 28mm/h，阈值 25mm/h',
    alarmValue: '28mm/h', status: 0,
    createTime: '2026-06-08 08:45:00',
    alarmCount: 2,
    dispatchLogs: [
      { id: 1, action: '系统自动告警', operator: '系统', time: '2026-06-08 08:45:00' },
      { id: 2, action: 'app推送通知', operator: '系统', time: '2026-06-08 08:45:05' },
    ]
  },
  {
    id: 4, hazardId: 1, hazardName: 'K15+200 右侧边坡',
    deviceId: 2, deviceName: 'GNSS-002',
    alarmType: '阈值预警', alarmLevel: 1,
    alarmContent: '表面位移变化量超过蓝色预警阈值，当前值 3.2mm，阈值 3mm',
    alarmValue: '3.2mm', status: 0,
    createTime: '2026-06-08 07:20:00',
    alarmCount: 1,
    dispatchLogs: [
      { id: 1, action: '系统自动告警', operator: '系统', time: '2026-06-08 07:20:00' },
    ]
  },
  {
    id: 5, hazardId: 4, hazardName: 'K42+800 左侧边坡',
    deviceId: 10, deviceName: 'GNSS-006',
    alarmType: '阈值预警', alarmLevel: 3,
    alarmContent: '深埋位移超过橙色预警阈值，当前值 8.3mm，阈值 7mm',
    alarmValue: '8.3mm', status: 0,
    createTime: '2026-06-08 06:50:00',
    alarmCount: 4,
    dispatchLogs: [
      { id: 1, action: '系统自动告警', operator: '系统', time: '2026-06-08 06:50:00' },
      { id: 2, action: '短信通知责任人', operator: '系统', time: '2026-06-08 06:50:05' },
    ]
  },
  {
    id: 6, hazardId: 2, hazardName: 'K23+500 左侧边坡',
    deviceId: 5, deviceName: 'INCLINE-001',
    alarmType: '阈值预警', alarmLevel: 2,
    alarmContent: '倾斜角度超过黄色预警阈值，当前值 0.15°，阈值 0.1°',
    alarmValue: '0.15°', status: 1,
    createTime: '2026-06-07 14:30:00',
    handleTime: '2026-06-07 15:00:00',
    handleRemark: '已现场核实，设备基座轻微松动，已加固处理',
    alarmCount: 1,
    dispatchLogs: [
      { id: 1, action: '系统自动告警', operator: '系统', time: '2026-06-07 14:30:00' },
      { id: 2, action: 'app推送通知', operator: '系统', time: '2026-06-07 14:30:05' },
      { id: 3, action: '处理完成', operator: '张工', time: '2026-06-07 15:00:00' },
    ]
  },
]

export const alarmApi = {
  getUnprocessed(): Alarm[] {
    return mockAlarms.filter(a => a.status === 0)
  },

  getStats(): { red: number; orange: number; yellow: number; blue: number } {
    const unprocessed = mockAlarms.filter(a => a.status === 0)
    return {
      red: unprocessed.filter(a => a.alarmLevel === 4).length,
      orange: unprocessed.filter(a => a.alarmLevel === 3).length,
      yellow: unprocessed.filter(a => a.alarmLevel === 2).length,
      blue: unprocessed.filter(a => a.alarmLevel === 1).length,
    }
  },

  getById(id: number): Alarm | undefined {
    return mockAlarms.find(a => a.id === id)
  },

  handle(alarmId: number, remark: string): boolean {
    const alarm = mockAlarms.find(a => a.id === alarmId)
    if (alarm) {
      alarm.status = 1
      alarm.handleTime = new Date().toISOString()
      alarm.handleRemark = remark
    }
    return true
  }
}

export default alarmApi
```

**Step 2: 创建 hazard.ts 模拟数据**

```typescript
// src/utils/hazard.ts

export interface Hazard {
  id: number
  name: string           // 隐患点名称
  level: string          // 风险等级：高风险/中风险/低风险
  location: string       // 位置描述
  status: string         // 状态：监测中/已处置
  deviceCount: number    // 关联设备数
  longitude?: number
  latitude?: number
  description?: string
  createTime: string
}

export interface HazardWithDevices extends Hazard {
  devices: import('./device').DeviceInfo[]
}

const mockHazards: Hazard[] = [
  { id: 1, name: 'K15+200 右侧边坡', level: '高风险', location: 'G65包茂高速K15+200右侧', status: '监测中', deviceCount: 3, description: '该边坡高度大于30m，岩体破碎，存在滑坡风险', createTime: '2025-03-15' },
  { id: 2, name: 'K23+500 左侧边坡', level: '中风险', location: 'G65包茂高速K23+500左侧', status: '监测中', deviceCount: 4, description: '该边坡坡度较陡，雨季存在溜坍风险', createTime: '2025-04-20' },
  { id: 3, name: 'K31+100 右侧边坡', level: '低风险', location: 'G65包茂高速K31+100右侧', status: '监测中', deviceCount: 2, description: '该边坡表层风化，需要持续关注', createTime: '2025-05-10' },
  { id: 4, name: 'K42+800 左侧边坡', level: '高风险', location: 'G65包茂高速K42+800左侧', status: '监测中', deviceCount: 3, description: '该边坡曾发生小型塌方，加固后持续监测', createTime: '2025-02-28' },
  { id: 5, name: 'K56+300 右侧边坡', level: '中风险', location: 'G65包茂高速K56+300右侧', status: '已处置', deviceCount: 2, description: '已完成锚固施工，持续监测稳定性', createTime: '2025-01-15' },
]

export const hazardApi = {
  getAll(): Hazard[] {
    return mockHazards
  },

  getById(id: number): HazardWithDevices | undefined {
    const hazard = mockHazards.find(h => h.id === id)
    if (!hazard) return undefined
    // 动态引入设备数据避免循环依赖
    const { deviceApi } = require('./device')
    const devices = deviceApi.getByHazardId(id)
    return { ...hazard, devices }
  },

  getStats(): { total: number; high: number; medium: number; low: number } {
    return {
      total: mockHazards.length,
      high: mockHazards.filter(h => h.level === '高风险').length,
      medium: mockHazards.filter(h => h.level === '中风险').length,
      low: mockHazards.filter(h => h.level === '低风险').length,
    }
  }
}

export default hazardApi
```

**Step 3: 重写 device.ts 模拟数据**

```typescript
// src/utils/device.ts

export interface DeviceInfo {
  id: number
  deviceName: string
  deviceCode: string
  deviceType: string      // GNSS | 雨量计 | 测斜仪 | 裂缝计 | 水位计
  status: string          // 在线 | 离线 | 故障
  lastReportTime: string
  hazardId: number
  hazardName: string
  installDate: string
  attributes: DeviceAttribute[]
}

export interface DeviceAttribute {
  property: string
  displayName: string
  unit: string
  currentValue?: number
}

const mockDevices: DeviceInfo[] = [
  {
    id: 1, deviceName: 'GNSS-001', deviceCode: 'GNSS-BP001-01',
    deviceType: 'GNSS', status: '在线',
    lastReportTime: '2026-06-08 10:28:00',
    hazardId: 1, hazardName: 'K15+200 右侧边坡',
    installDate: '2025-03-20',
    attributes: [
      { property: 'displacement_x', displayName: 'X方向位移', unit: 'mm', currentValue: 5.2 },
      { property: 'displacement_y', displayName: 'Y方向位移', unit: 'mm', currentValue: 3.8 },
      { property: 'displacement_z', displayName: 'Z方向位移', unit: 'mm', currentValue: 12.5 },
    ]
  },
  {
    id: 2, deviceName: 'GNSS-002', deviceCode: 'GNSS-BP001-02',
    deviceType: 'GNSS', status: '在线',
    lastReportTime: '2026-06-08 10:25:00',
    hazardId: 1, hazardName: 'K15+200 右侧边坡',
    installDate: '2025-03-20',
    attributes: [
      { property: 'surface_displacement', displayName: '表面位移', unit: 'mm', currentValue: 3.2 },
      { property: 'velocity', displayName: '位移速率', unit: 'mm/d', currentValue: 0.15 },
    ]
  },
  {
    id: 3, deviceName: 'CRACK-001', deviceCode: 'CRK-BP001-01',
    deviceType: '裂缝计', status: '在线',
    lastReportTime: '2026-06-08 10:26:00',
    hazardId: 1, hazardName: 'K15+200 右侧边坡',
    installDate: '2025-03-22',
    attributes: [
      { property: 'crack_width', displayName: '裂缝宽度', unit: 'mm', currentValue: 2.1 },
    ]
  },
  {
    id: 4, deviceName: 'GNSS-004', deviceCode: 'GNSS-BP002-01',
    deviceType: 'GNSS', status: '在线',
    lastReportTime: '2026-06-08 09:12:00',
    hazardId: 2, hazardName: 'K23+500 左侧边坡',
    installDate: '2025-04-25',
    attributes: [
      { property: 'displacement_x', displayName: 'X方向位移', unit: 'mm', currentValue: 4.1 },
      { property: 'displacement_y', displayName: 'Y方向位移', unit: 'mm', currentValue: 6.3 },
      { property: 'displacement_z', displayName: 'Z方向位移', unit: 'mm', currentValue: 7.8 },
    ]
  },
  {
    id: 5, deviceName: 'INCLINE-001', deviceCode: 'INC-BP002-01',
    deviceType: '测斜仪', status: '在线',
    lastReportTime: '2026-06-08 09:10:00',
    hazardId: 2, hazardName: 'K23+500 左侧边坡',
    installDate: '2025-04-28',
    attributes: [
      { property: 'angle', displayName: '倾斜角度', unit: '°', currentValue: 0.15 },
    ]
  },
  {
    id: 6, deviceName: 'RAIN-002', deviceCode: 'RAN-BP002-01',
    deviceType: '雨量计', status: '在线',
    lastReportTime: '2026-06-08 09:08:00',
    hazardId: 2, hazardName: 'K23+500 左侧边坡',
    installDate: '2025-04-25',
    attributes: [
      { property: 'hourly_rain', displayName: '小时雨量', unit: 'mm/h', currentValue: 15.2 },
      { property: 'daily_rain', displayName: '日累计雨量', unit: 'mm', currentValue: 45.6 },
    ]
  },
  {
    id: 7, deviceName: 'RAIN-001', deviceCode: 'RAN-BP003-01',
    deviceType: '雨量计', status: '在线',
    lastReportTime: '2026-06-08 08:40:00',
    hazardId: 3, hazardName: 'K31+100 右侧边坡',
    installDate: '2025-05-15',
    attributes: [
      { property: 'hourly_rain', displayName: '小时雨量', unit: 'mm/h', currentValue: 28.0 },
      { property: 'daily_rain', displayName: '日累计雨量', unit: 'mm', currentValue: 62.3 },
    ]
  },
  {
    id: 8, deviceName: 'GNSS-005', deviceCode: 'GNSS-BP003-01',
    deviceType: 'GNSS', status: '离线',
    lastReportTime: '2026-06-07 18:30:00',
    hazardId: 3, hazardName: 'K31+100 右侧边坡',
    installDate: '2025-05-15',
    attributes: [
      { property: 'displacement_x', displayName: 'X方向位移', unit: 'mm', currentValue: 1.2 },
      { property: 'displacement_z', displayName: 'Z方向位移', unit: 'mm', currentValue: 2.1 },
    ]
  },
  {
    id: 9, deviceName: 'GNSS-003', deviceCode: 'GNSS-BP004-01',
    deviceType: 'GNSS', status: '在线',
    lastReportTime: '2026-06-08 10:20:00',
    hazardId: 4, hazardName: 'K42+800 左侧边坡',
    installDate: '2025-03-05',
    attributes: [
      { property: 'displacement_x', displayName: 'X方向位移', unit: 'mm', currentValue: 6.7 },
      { property: 'displacement_z', displayName: 'Z方向位移', unit: 'mm', currentValue: 9.5 },
    ]
  },
  {
    id: 10, deviceName: 'GNSS-006', deviceCode: 'GNSS-BP004-02',
    deviceType: 'GNSS', status: '在线',
    lastReportTime: '2026-06-08 06:48:00',
    hazardId: 4, hazardName: 'K42+800 左侧边坡',
    installDate: '2025-03-05',
    attributes: [
      { property: 'deep_displacement', displayName: '深埋位移', unit: 'mm', currentValue: 8.3 },
      { property: 'velocity', displayName: '位移速率', unit: 'mm/d', currentValue: 0.35 },
    ]
  },
  {
    id: 11, deviceName: 'WATER-001', deviceCode: 'WAT-BP004-01',
    deviceType: '水位计', status: '故障',
    lastReportTime: '2026-06-06 14:00:00',
    hazardId: 4, hazardName: 'K42+800 左侧边坡',
    installDate: '2025-03-08',
    attributes: [
      { property: 'water_level', displayName: '地下水位', unit: 'm', currentValue: null },
    ]
  },
  {
    id: 12, deviceName: 'GNSS-007', deviceCode: 'GNSS-BP005-01',
    deviceType: 'GNSS', status: '在线',
    lastReportTime: '2026-06-08 10:15:00',
    hazardId: 5, hazardName: 'K56+300 右侧边坡',
    installDate: '2025-01-20',
    attributes: [
      { property: 'displacement_x', displayName: 'X方向位移', unit: 'mm', currentValue: 0.8 },
      { property: 'displacement_z', displayName: 'Z方向位移', unit: 'mm', currentValue: 1.2 },
    ]
  },
  {
    id: 13, deviceName: 'RAIN-003', deviceCode: 'RAN-BP005-01',
    deviceType: '雨量计', status: '离线',
    lastReportTime: '2026-06-07 22:00:00',
    hazardId: 5, hazardName: 'K56+300 右侧边坡',
    installDate: '2025-01-20',
    attributes: [
      { property: 'hourly_rain', displayName: '小时雨量', unit: 'mm/h', currentValue: null },
      { property: 'daily_rain', displayName: '日累计雨量', unit: 'mm', currentValue: 12.5 },
    ]
  },
]

export const deviceApi = {
  getAll(): DeviceInfo[] {
    return mockDevices
  },

  getById(id: number): DeviceInfo | undefined {
    return mockDevices.find(d => d.id === id)
  },

  getByHazardId(hazardId: number): DeviceInfo[] {
    return mockDevices.filter(d => d.hazardId === hazardId)
  },

  getHistoryData(deviceId: number, startTime: string, endTime: string, property?: string) {
    // 生成模拟历史数据
    const hours = Math.round((new Date(endTime).getTime() - new Date(startTime).getTime()) / 3600000)
    const data = []
    const device = mockDevices.find(d => d.id === deviceId)
    const attr = device?.attributes.find(a => a.property === property)
    const baseValue = attr?.currentValue || 5

    for (let i = 0; i < Math.min(hours, 72); i++) {
      const time = new Date(new Date(startTime).getTime() + i * 3600000)
      data.push({
        time: `${time.getFullYear()}-${String(time.getMonth()+1).padStart(2,'0')}-${String(time.getDate()).padStart(2,'0')} ${String(time.getHours()).padStart(2,'0')}:${String(time.getMinutes()).padStart(2,'0')}:00`,
        value: (baseValue + (Math.random() - 0.5) * baseValue * 0.4).toFixed(2)
      })
    }
    return data
  }
}

export default deviceApi
```

---

## Task 3: 事件大厅页面 (index.vue)

**Files:**
- Rewrite: `src/pages/index.vue`

**Step 1: 重写 index.vue**

完整代码见实施阶段。核心结构：
- 顶部渐变 header（蓝色主题，标题"事件大厅"，副标题"边坡监测 · 智能预警"）
- 红橙黄蓝四级告警数量统计（4个统计卡片，保持现有 stats-row 样式）
- 告警事件列表（卡片列表，左侧色条标识级别，显示隐患点名、告警类型、时间）
- 点击跳转 alarm-detail 页面
- 下拉刷新 + 骨架屏
- 移除：浮动扫码按钮、下载更新逻辑（移到 profile）

---

## Task 4: 告警详情页面 (alarm-detail.vue)

**Files:**
- Rewrite: `src/pages/alarm-detail.vue`

**Step 1: 重写 alarm-detail.vue**

核心结构：
- 头部返回 + 告警级别标签 + 时间
- 告警信息卡片：隐患点、告警类型（阈值/综合）、告警内容、告警数值、告警次数、状态
- 分发日志时间线
- 关联设备列表（阈值预警=单设备，综合预警=该隐患点下所有设备）
- 底部处理按钮（待处理状态才显示）

---

## Task 5: 隐患点列表页 (hazard.vue)

**Files:**
- Create: `src/pages/hazard.vue`

**Step 1: 创建 hazard.vue**

核心结构：
- 顶部渐变 header（标题"隐患点"，副标题"风险管控 · 安全监测"）
- 统计条：总数 + 高/中/低风险数量
- 卡片列表：隐患点名称、风险等级标签、位置、设备数量、状态标签
- 点击跳转 hazard-detail 页面
- 下拉刷新

---

## Task 6: 隐患点详情页 (hazard-detail.vue)

**Files:**
- Create: `src/pages/hazard-detail.vue`

**Step 1: 创建 hazard-detail.vue**

核心结构：
- 头部返回 + 隐患点名称
- 基本信息卡片：位置、风险等级、状态、设备数量、描述、创建时间
- 关联设备列表：设备名称、类型、状态、最后上报时间
- 点击设备跳转 device-detail

---

## Task 7: 设备库列表页 (device.vue)

**Files:**
- Create: `src/pages/device.vue`

**Step 1: 创建 device.vue**

核心结构：
- 顶部渐变 header（标题"设备库"，副标题"设备运维 · 状态监控"）
- 搜索栏
- 设备卡片列表：设备名称、编码、类型标签、状态标签（在线/离线/故障）、最后上报时间、所属隐患点
- 点击跳转 device-detail
- 下拉刷新

---

## Task 8: 设备详情页 (device-detail.vue)

**Files:**
- Create: `src/pages/device-detail.vue`

**Step 1: 创建 device-detail.vue**

核心结构：
- 头部返回 + 设备名称
- 设备配置信息卡片：编码、类型、所属隐患点、安装日期
- 连接状态卡片：状态、最后上报时间
- 监测参数列表：各属性当前值
- 按钮：查看监测数据（跳转 chart 页面）

---

## Task 9: 监测数据页面 (chart.vue)

**Files:**
- Modify: `src/pages/chart.vue`

**Step 1: 改造 chart.vue**

核心改动：
- 移除 CascadeSelector（集装箱→设备 级联选择）
- 改为设备多选模式：顶部显示已选设备标签，"添加设备"按钮打开设备选择弹窗
- 时间范围选择保持（24h/7d/30d）
- ECharts 图表区域：按设备分组显示图表
- 雨量类型属性用柱状图，其他用折线图
- 复用现有 echarts.vue 组件

---

## Task 10: 清理旧文件

**Files:**
- Delete or keep for reference: `src/pages/container-detail.vue`
- Delete or keep for reference: `src/pages/scan.vue`
- Delete or keep for reference: `src/pages/alarm-handle.vue`
- Delete or keep for reference: `src/utils/container.ts`
- Delete or keep for reference: `src/utils/dashboard.ts`
- Delete or keep for reference: `src/components/ContainerCard.vue`
- Delete or keep for reference: `src/components/CascadeSelector.vue`

**Step 1: 确认旧页面已不在 pages.json 路由中，旧文件暂时保留不影响运行**

旧文件不在路由配置中就不会被打包，暂时保留即可。

---

## 执行顺序与依赖

```
Task 1 (配置) → Task 2 (模拟数据) → Task 3-9 (页面，可并行) → Task 10 (清理)
```

Tasks 3-9 之间无依赖，可并行实施。
