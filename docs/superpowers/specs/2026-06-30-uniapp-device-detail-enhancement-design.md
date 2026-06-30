# UniApp 设备详情页增强 + 视频设备播放设计

> 日期: 2026-06-30
> 范围: `uniapp/src/` 前端小程序

## 1. 需求概述

完善 UniApp 小程序的 4 个功能：
1. **device-detail** 各属性最新数据展示（按传感器分组，无数据显示 `-`）
2. **device-detail** 点击属性展开内联 EChart 图表（手风琴模式，24h/7d 可切换）
3. **device-detail** "查看监测数据"跳转 `/pages/chart` 并预选隐患点、设备、传感器
4. **device** 页面支持视频设备 HLS 流播放

## 2. 设计决策

| 决策项 | 选择 | 理由 |
|--------|------|------|
| 最新数据源 | `GET /monitor-data/sensor/latest?deviceId=X&sensorCode=Y` | 不依赖隐患点绑定关系 |
| 视频协议 | HLS (m3u8) | 微信小程序 `<video>` 原生支持 |
| 内联图表时间范围 | 可切换 24h/7d | 覆盖快速查看和趋势分析 |

## 3. 后端 API 依赖

### 已有 API（无需后端改动）

| API | 方法 | 用途 |
|-----|------|------|
| `/api/v1/monitor-data/sensor/latest` | GET | 按传感器获取最新数据，返回 `Map<attrCode, {time, value, quality}>` |
| `/api/v1/monitor-data/chart` | GET | 时序图表数据 |
| `/api/v1/devices/{id}` | GET | 设备详情（含 `boundHazardPointId`） |
| `/api/v1/devices/{id}/sensors` | GET | 传感器列表（含 `sensorCode`, `attrList`） |
| `/api/v1/video-devices/page` | GET | 视频设备分页列表 |
| `/api/v1/video-devices/{id}` | GET | 视频设备详情（含 `streamUrl`） |
| `/api/v1/hazard-points/{id}/bound-devices` | GET | 隐患点绑定的设备列表 |

## 4. 功能设计

### 4.1 device-detail 各属性最新数据展示

**数据流**:
```
页面加载 → deviceApi.getById(id) → deviceApi.getSensors(id)
  → 遍历每个 sensor，调 monitorApi.getSensorLatest(deviceId, sensorCode)
  → 将返回的 {attrCode: {time, value, quality}} 合并到 sensor.attrs 上
```

**UI 结构变化**:
- 属性列表从扁平 `SensorAttr[]` 改为按传感器分组 `SensorGroup[]`
- 每个 `SensorGroup` 包含: `{ sensorName, monitorTypeName, attrs: AttrWithData[] }`
- `AttrWithData = SensorAttr & { latestValue?: number, latestTime?: string, quality?: number }`
- 属性卡片右侧从只显示单位 → 显示 **数值 + 单位**，无数据显示 `-`

**新增 API** (`utils/monitor.ts`):
```typescript
async getSensorLatest(deviceId: number, sensorCode: string): Promise<Record<string, {
  time: number
  value: number | null
  quality: number
}>>
```

### 4.2 device-detail 点击属性展开内联 EChart

**交互设计**:
- 属性卡片整体可点击
- 点击后卡片下方展开 `400rpx` 高度的 ECharts 区域
- 手风琴模式：同一时刻只展开一个属性
- 展开区域包含：`24小时` / `7天` Tab + ECharts 折线图
- 图表数据走 `monitorApi.getChart()`，需要 `hazardPointId`（从 `device.boundHazardPointId` 取）
- 未绑定隐患点时显示提示文案

**状态管理**:
```typescript
const expandedAttrKey = ref<string>('')  // `${sensorId}-${attrCode}`
const inlineTimeTab = ref<'24h' | '7d'>('24h')
const inlineChartOption = ref<any>(null)
const inlineLoading = ref(false)
```

**图表复用**: 使用现有 `components/echarts.vue` + `echarts.esm.min.js`

### 4.3 chart 页面跳转预加载

**跳转 URL**:
```
/pages/chart?deviceId=X&hazardPointId=Y
```
- `hazardPointId` 从 `device.boundHazardPointId` 取
- 如果设备未绑定隐患点，按钮置灰或提示

**chart.vue 改造**:
1. `onMounted` 解析 URL 参数时，如有 `hazardPointId`：
   - 在 `allHazards` 中匹配对应项，设置 `selectedHazardIndex`
   - 调用 `onHazardChange` 加载隐患点下的设备列表
   - 如有 `deviceId`，在加载完设备列表后自动选中
   - 调用 `loadAllCharts()` 加载图表数据
2. 不需要用户手动操作

### 4.4 device 页面视频设备播放

**device.vue 改造**:
1. 顶部增加 Tab 切换：`监测设备` / `视频设备`
2. 监测设备 Tab 保持现有逻辑
3. 视频设备 Tab：调用 `videoApi.getPage()` 加载列表
4. 卡片样式与监测设备一致，显示设备名、编号、状态
5. 点击视频设备卡片 → 跳转 `pages/video-player?id=X`

**新增页面** (`pages/video-player.vue`):
- 接收 `id` 参数
- 调用 `videoApi.getById(id)` 获取 `streamUrl`
- 使用 `<video>` 组件全屏播放 HLS 流
- 支持暂停、全屏切换、返回
- 页面样式与现有 detail 页一致（渐变头部 + 安全区适配）

**新增 API** (`utils/video.ts`):
```typescript
interface VideoDevice {
  id: number
  deviceName: string
  deviceCode: string
  streamUrl: string
  status: number  // 0-离线 1-在线
  manufacturer?: string
  deviceType?: string
}

videoApi.getPage(params?: { pageNum?: number; pageSize?: number }): Promise<{ rows: VideoDevice[], total: number }>
videoApi.getById(id: number): Promise<VideoDevice | undefined>
```

**pages.json**: 新增 `pages/video-player` 路由注册。

## 5. 文件变更清单

| 文件 | 变更类型 | 说明 |
|------|----------|------|
| `uniapp/src/utils/monitor.ts` | 修改 | 新增 `getSensorLatest()` |
| `uniapp/src/utils/video.ts` | **新增** | 视频设备 API |
| `uniapp/src/utils/device.ts` | 修改 | `DeviceInfo` 增加 `boundHazardPointId` 字段 |
| `uniapp/src/pages/device-detail.vue` | **重写** | 分组属性 + 最新数据 + 内联图表 + 跳转 |
| `uniapp/src/pages/chart.vue` | 修改 | URL 参数预加载逻辑 |
| `uniapp/src/pages/device.vue` | 修改 | 增加 Tab 切换 + 视频设备列表 |
| `uniapp/src/pages/video-player.vue` | **新增** | HLS 视频播放页 |
| `uniapp/src/pages.json` | 修改 | 注册 `pages/video-player` |

## 6. 错误处理

- 传感器最新数据获取失败：该属性值显示 `-`，不阻断其他属性
- 内联图表数据获取失败：显示"暂无数据"文案
- 视频流加载失败：显示错误提示 + 重试按钮
- 设备未绑定隐患点：图表区域显示提示，不报错
