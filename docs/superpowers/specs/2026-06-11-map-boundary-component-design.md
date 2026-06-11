# 设计文档：统一地图编辑公共组件

| 字段 | 值 |
|---|---|
| 作者 | Claude (brainstorming session) |
| 创建日期 | 2026-06-11 |
| 状态 | 待评审 |
| 关联 issue | （TBD - 待用户创建） |
| 涉及代码区域 | `web/src/views/basic/*`, `web/src/components/`, `server/zwei-iot-hazard/`, `db/upgrade/` |

## 1. 摘要

新建 2 个 Vue 组件（`<MapPointPicker>` / `<MapBoundaryEditor>`）+ 1 个 composable（`useLeafletMap`）+ 2 个工具模块（`coordParser` / `boundaryCoords`），统一系统中 4+ 处分散的 Leaflet 地图编辑实现。新组件以 Leaflet-Geoman 为编辑引擎，提供"绘制走向"（含主线 + 辅助标注）、polygon 顶点拖动/单点删除/撤销、坐标粘贴导入、隐患点范围叠加显示等能力。同步修复两个已知 bug：Device 编辑时地图第二次打开空白、HazardPoint 编辑时已存在的范围不显示。后端引入 `BoundaryCoordsDTO` 强类型校验，移除冗余 `strike` 列，并修复 `IDeviceHazardRelationService` 返回的字段以支持 HP 关联反查。

## 2. 问题陈述

### 2.1 现有 Bug

- **B1（Device 第二次打开地图空白）**：`web/src/views/basic/Device.vue` 第 909-915 行 `initMapPicker` 用 `if (mapPickerInstance) { ...invalidateSize(); return }` 早返回；但弹窗 `destroy-on-close` 已销毁 DOM，残留的 `mapPickerInstance` 是僵尸引用，新 DOM 上无地图。
- **B2（HazardPoint 编辑范围时已存在范围不显示）**：`web/src/views/basic/HazardPoint.vue` 第 1898-1908 行已正确解析 `boundaryCoords` 到 `polygonCoords/strikeCoords/strikeAngle` 三个 ref；但 `initMap()`（第 2206-2235 行）只渲染中心点 marker，从未遍历这些 state 把几何元素画到地图上。用户感受是"上次画的全没了，必须重画"。

### 2.2 产品需求（原话）

1. 隐患点的范围编辑功要做到人性化，现在一个点标错了必须整个重来，这个功能需要重做。
2. 隐患点范围编辑时，还有"绘制走向"功能，允许用户自由标注绘制，现在没有相关后端字段和落库持久化。
3. 系统中涉及到的地图范围编辑体验都不一样，建议能不能统一抽成一个公共组件。
4. 设备编辑页面，在地图上标记安装位置时，应该加载出对应隐患点已有的隐患点范围，还要支持输入（导入）坐标标记。

### 2.3 当前实现盘点

| 文件 | 地图代码行数 | 实现方式 | 备注 |
|---|---|---|---|
| `views/basic/HazardPoint.vue` | ~150 行 | 手写 Leaflet click-only append | B2 在此 |
| `views/basic/Device.vue` | ~100 行 | 手写 Leaflet 单点选址 | B1 在此 |
| `views/basic/VideoDevice.vue` | ~80 行 | 手写 Leaflet 单点选址 | 无叠加 |
| `views/system/Settings.vue` | ? | leaflet-draw（另一套库） | 本期不动 |
| `views/dashboard/components/MapDrawToolbar.vue` | ? | leaflet-draw（另一套库） | 本期不动 |

## 3. 决策汇总（来自 brainstorming 阶段）

| # | 决策 | 选项 |
|---|---|---|
| 1 | 组件粒度 | 多组件 + 共用 composable（`<MapPointPicker>` + `<MapBoundaryEditor>` + `useLeafletMap`） |
| 2 | 编辑库 | Leaflet-Geoman（`@geoman-io/leaflet-geoman-free`） |
| 3 | 后端 schema | 单列 JSON + 强类型 DTO `BoundaryCoordsDTO`；删除 `hazard_point.strike` 冗余列 |
| 4 | 坐标导入范围 | 两个组件都支持 |
| 4b | 坐标导入格式 | PointPicker 单行智能粘贴；BoundaryEditor 多行粘贴。KML/CSV/GeoJSON 留扩展口 |
| 5 | HP 范围叠加 | 仅 Device 编辑设备主位置时显示（业务规则：1 设备 ≤ 1 HP） |
| 6 | 走向几何形态 | D：主线（2 点）+ 多条辅助折线 |
| 7 | 角度标签风格 | v4：端点旁小标签（白描边红字），不遮挡中央 |
| 8 | 中心点行为 | α + b：会话内"手动拖过就不再自动"，polygon 质心算法用带符号面积加权 |
| 9 | wire 格式 | 数组 `[lat,lng]`（节约 wire + 兼容老数据） |
| 10 | API 响应 | `boundaryCoords` 保持字符串（不动响应契约） |
| 11 | 测试 | Vitest 纯函数单测 + 详细手工 checklist |
| 12 | 实施分期 | 4 个独立 PR |
| ⏳ | 后端 `UNIQUE(device_id)` 业务校验 | 列入本期之后的紧随工单 |

## 4. 架构

### 4.1 三层结构

```
Layer 3: 消费方
  views/basic/HazardPoint.vue   →  <MapBoundaryEditor>
  views/basic/Device.vue        →  <MapPointPicker>
  views/basic/VideoDevice.vue   →  <MapPointPicker>

Layer 2: UI 组件
  components/map/MapPointPicker.vue
  components/map/MapBoundaryEditor.vue
  components/map/MapCoordInput.vue (内部)

Layer 1: 基础设施
  composables/useLeafletMap.ts
  lib/coordParser.ts
  lib/boundaryCoords.ts
```

### 4.2 设计原则

- **组件不直接调 API**：只通过 v-model emit 数据，外部 I/O 由消费方负责。
- **Geoman 实例生命周期由 composable 统一管理**：业务组件无需关心 `pm.disable()` 等清理。
- **bug 修复内化于 composable**：B1/B2 的根本原因都是"组件状态与 DOM 生命周期未对齐"——`useLeafletMap` 用 `watch(container)` 强制对齐。
- **未来抽 NPM 包友好**：除 Element Plus、Leaflet、Geoman 外不依赖 UI 库。

### 4.3 文件清单

**新增**：
- `web/src/lib/boundaryCoords.ts`
- `web/src/lib/coordParser.ts`
- `web/src/composables/useLeafletMap.ts`
- `web/src/components/map/MapPointPicker.vue`
- `web/src/components/map/MapBoundaryEditor.vue`
- `web/src/components/map/MapCoordInput.vue`
- `server/zwei-iot-hazard/src/main/java/com/zwei/iot/hazardpoint/domain/dto/BoundaryCoordsDTO.java`
- `server/zwei-iot-hazard/src/main/java/com/zwei/iot/hazardpoint/service/BoundaryCoordsValidator.java`
- `db/upgrade/2026-06-11-drop-strike-column.sql`

**修改**：
- `web/src/views/basic/HazardPoint.vue`（-150 / +30 行）
- `web/src/views/basic/Device.vue`（-100 / +40 行）
- `web/src/views/basic/VideoDevice.vue`（-80 / +20 行）
- `web/package.json`（新增 `@geoman-io/leaflet-geoman-free` + 开发依赖 `vitest`）
- `server/zwei-iot-hazard/src/main/java/com/zwei/iot/hazardpoint/controller/HazardPointController.java`（注入 validator）
- `server/zwei-iot-device/src/main/java/com/zwei/iot/device/service/IDeviceHazardRelationService.java`（扩展返回 `{id, name}`）

**不改**（列入 V2）：
- `web/src/views/system/Settings.vue`
- `web/src/views/dashboard/components/MapDrawToolbar.vue`

## 5. 组件公共 API

### 5.1 `<MapPointPicker>`

```typescript
interface MapPointPickerProps {
  modelValue: LatLng | null
  readonly?: boolean
  /** 当提供时，叠加该 polygon 作只读背景（由消费方传入，组件不调 API） */
  overlayPolygon?: LatLng[] | null
  defaultCenter?: LatLng                       // 默认 {lat:30.65, lng:104.10}
  defaultZoom?: number                         // 默认 12
  coordInputEnabled?: boolean                  // 默认 true
  height?: string | number                     // 默认 "400px"
}

interface MapPointPickerEvents {
  'update:modelValue': (value: LatLng | null) => void
}

interface MapPointPickerExposed {
  focusToCoord(lng: number, lat: number): void
  invalidate(): void
}
```

### 5.2 `<MapBoundaryEditor>`

```typescript
interface MapBoundaryEditorProps {
  modelValue: BoundaryCoords
  center: LatLng | null                        // v-model:center
  readonly?: boolean
  defaultCenter?: LatLng                       // 默认 {lat:30.67, lng:104.06}
  defaultZoom?: number                         // 默认 14
  coordInputEnabled?: boolean                  // 默认 true
  height?: string | number                     // 默认 "500px"
}

interface MapBoundaryEditorEvents {
  'update:modelValue': (value: BoundaryCoords) => void
  'update:center':     (value: LatLng | null) => void
}

interface MapBoundaryEditorExposed {
  invalidate(): void
  resetCenterToCentroid(): void
}
```

工具栏（组件内部渲染，与 Element Plus 风格一致）：

```
[⬣ 多边形] [↗ 走向] [⤴ 辅助线] [🗑 删除选中] [⌖ 重置中心] [📋 导入] [清空]
```

### 5.3 `<MapCoordInput>`（内部子组件）

```typescript
interface MapCoordInputProps {
  mode: 'single' | 'multiline'
  modelValue?: string
  placeholder?: string
}

interface MapCoordInputEvents {
  'update:modelValue': (text: string) => void
  'parse-success': (coords: LatLng | LatLng[]) => void
  'parse-error':   (reason: string, lineNumber?: number) => void
}
```

## 6. 数据模型

### 6.1 前端类型（`web/src/lib/boundaryCoords.ts`）

```typescript
export interface LatLng { lat: number; lng: number }   // 内存中始终用对象形式

export interface BoundaryCoords {
  polygon: LatLng[]                    // 0..N 顶点；非零时必须 ≥ 3
  strikeLine: [LatLng, LatLng] | null  // 主走向 2 点
  auxiliaryLines: LatLng[][]           // 0..N 条辅助折线；每条 ≥ 2 点
}

export const EMPTY_BOUNDARY: BoundaryCoords = {
  polygon: [],
  strikeLine: null,
  auxiliaryLines: []
}

/** 序列化：内存 {lat,lng} → wire 数组 [lat,lng] */
export function serialize(b: BoundaryCoords): string

/** 反序列化：兼容旧 key (`strikeCoords` → `strikeLine`，`strikeAngle` 弃) */
export function deserialize(json: string | null | undefined): BoundaryCoords

/** 多边形质心（算法 b：带符号面积加权），polygon.length < 3 返回 null */
export function centroid(polygon: LatLng[]): LatLng | null

/** 球面方位角（degrees，0=正北，顺时针），同点返回 0 */
export function strikeAngle(line: [LatLng, LatLng]): number
```

### 6.1b 坐标解析（`web/src/lib/coordParser.ts`）

```typescript
/** 解析单行 "lat,lng" / "lng,lat" / "lat lng"，含智能顺序识别 */
export function parseSingle(text: string): LatLng | null

/** 多行解析结果 */
export interface ParseMultilineResult {
  coords: LatLng[]                                   // 成功解析的顶点
  errors: Array<{ line: number; raw: string; reason: string }>
}

/** 解析多行文本（自动识别 lat,lng / tab / 分号 / 表头 / 注释行） */
export function parseMultiline(text: string): ParseMultilineResult
```

### 6.2 后端 DTO（`com.zwei.iot.hazardpoint.domain.dto.BoundaryCoordsDTO`）

```java
public record BoundaryCoordsDTO(
    List<List<BigDecimal>> polygon,                   // 每项 = [lat, lng]
    List<List<BigDecimal>> strikeLine,                // null 或 size=2
    List<List<List<BigDecimal>>> auxiliaryLines       // 0..N 条折线
) {
    public static BoundaryCoordsDTO empty() {
        return new BoundaryCoordsDTO(List.of(), null, List.of());
    }

    public void validate() throws ValidationException {
        // 见 §7.2 校验规则
    }
}
```

### 6.3 wire 格式（落库 JSON）

**新格式**（本期之后所有写入用此）：
```json
{
  "polygon":        [[30.67,104.05],[30.68,104.06],[30.67,104.07]],
  "strikeLine":     [[30.67,104.05],[30.68,104.07]],
  "auxiliaryLines": [[[30.671,104.061],[30.672,104.063]]]
}
```

**旧格式**（读时自动迁移）：
```json
{
  "polygon":      [[30.67,104.05],...],
  "strikeCoords": [[30.67,104.05],[30.68,104.07]],  ← 旧 key
  "strikeAngle":  152                                ← 旧 key, 弃
}
```

读时 `deserialize` 同时识别 `strikeLine`（新）和 `strikeCoords`（旧）；下次保存自动升级为新 key。

## 7. 后端变更

### 7.1 引入 `BoundaryCoordsDTO`

详见 §6.2。Java record 配合 Jackson 自动序列化，`auxiliaryLines` 字段如不存在则默认为 `[]`。

### 7.2 校验规则（`BoundaryCoordsValidator`）

```
- polygon 为空 OR polygon.size >= 3
- 每个 vertex.size == 2，且 lat ∈ [-90, 90]，lng ∈ [-180, 180]
- strikeLine == null OR strikeLine.size == 2 且每个 vertex 同上
- auxiliaryLines 中每条 polyline.size >= 2 且每个 vertex 同上
- polygon.size 上限 1000（防止 DoS）
- auxiliaryLines.size 上限 50（防止 DoS）
```

校验失败 → 抛 `ServiceException("boundary_coords: <reason>", 400)`。

### 7.3 Controller 接入

`HazardPointController.add/edit`（line 117-149）在调用 service 前调用 `validator.parseAndValidate(request.getBoundaryCoords())`。

### 7.4 数据库迁移

`db/upgrade/2026-06-11-drop-strike-column.sql`：

```sql
-- 删除冗余的 strike 列（角度数据已在 boundary_coords.strikeLine 中可计算）
ALTER TABLE hazard_point DROP COLUMN strike;
```

**不需要 UPDATE 老数据**：
- polygon 数组格式不变
- 旧 `strikeCoords` 在前端 `deserialize` 自动映射为 `strikeLine`
- 旧 `strikeAngle` 派生数据，直接弃
- `longitude/latitude` 列保留作为 center 存储位置

### 7.5 `IDeviceHazardRelationService` 扩展

```java
// 新方法（旧 getHazardPointNameByDeviceId 保留以兼容）
public record HazardPointRef(Long id, String name) {}

HazardPointRef getHazardPointByDeviceId(Long deviceId);
```

实现：在 `DeviceHazardRelationServiceImpl` 内查 `device_hazard_point` 表（最多一条，符合业务规则），返回关联 HP 的 id+name；无绑定时返回 null。

## 8. Composable 与 Bug 修复

### 8.1 `useLeafletMap` 实现要点

```typescript
export function useLeafletMap(opts: UseLeafletMapOptions): UseLeafletMapReturn {
  const map = shallowRef<L.Map | null>(null)
  const isReady = ref(false)

  // 关键：监听容器 DOM ref 的出现/消失
  watch(opts.container, (el) => {
    if (el && !map.value) initMap(el)            // 弹窗打开 → init
    else if (!el && map.value) destroyMap()      // 弹窗销毁 → cleanup
  }, { immediate: true, flush: 'post' })

  onBeforeUnmount(destroyMap)                    // 双保险

  function destroyMap() {
    map.value?.off()
    map.value?.remove()
    map.value = null
    isReady.value = false
  }
  // ... initMap / invalidate / setView 等
}
```

### 8.2 Bug 修复机制

| Bug | 修复点 |
|---|---|
| **B1** | `watch(container)` 自动 init/destroy，杜绝僵尸引用；新 DOM 出现立即重建 instance |
| **B2** | `<MapBoundaryEditor>` 通过 `watch([modelValue, center, isReady], ..., { immediate: true, deep: true })` 在 ready 后渲染全部几何元素到 Geoman 层 |

### 8.3 Tianditu key

继续硬编码（4 处合 1 处到 composable 内部常量）；本期不引入 `.env` 变量。

## 9. 关键行为细节

### 9.1 中心点（HP 编辑器）

| 场景 | 行为 |
|---|---|
| 新建 HP，无 polygon | 中心 = `defaultCenter` |
| 新建 HP，画完 polygon (≥3) | 中心自动 = `centroid(polygon)` |
| 编辑 HP，已有 polygon + center | 中心 = 持久化值 |
| 编辑 HP，polygon 改变，本会话未拖中心 | 中心自动重算 |
| 编辑 HP，本会话拖过中心 | 中心**不再**自动跟随 |
| 点 "⌖ 重置中心" | 清除"手动"标记，立即重算到当前 polygon 质心 |
| polygon 顶点 < 3 时改顶点 | 中心位置保留，不重算 |

**实现备注**：
- 内部 `manualCenterDragged: Ref<boolean>` 不持久化，关闭对话框即重置
- 持久化层 (`hazard_point.longitude/latitude`) 始终只存终态坐标

### 9.2 走向

- 点 "↗ 走向" → 鼠标十字 → 提示"请点击起点"
- 第 1 次点击 → 落起点 marker → 提示"请点击终点"
- 第 2 次点击 → 落终点 + 自动绘制连线 + 计算并显示 v4 端点小标签（白描边红字 "152°"）
- 自动退出工具模式
- 走向已存在时再次点 "↗ 走向" → 弹 ElMessageBox `[重新绘制 / 调整端点 / 取消]`
  - "调整端点"：直接进入 Geoman 端点拖拽模式
  - "重新绘制"：清除旧线，回到起点状态

### 9.3 辅助线

- 点 "⤴ 辅助线" → 鼠标十字 → 提示"点击下一个顶点；Enter / 双击完成；Esc 取消"
- 连续点击 N 个点
- Enter / 双击最后点 → 完成本条折线（橙色 #fa8c16 虚线）
- 工具**保持激活**，可继续画下一条
- Esc / 切换其他工具 → 退出
- 删除：进入编辑模式 → 点选某条 → 点工具栏 "🗑 删除选中" 按钮

### 9.4 坐标粘贴

**PointPicker 单点**（地图下方常驻输入框）：
```
[输入坐标 (lat,lng)]              [使用]
支持 "lat,lng" 或 "lng,lat" 智能识别
```

**BoundaryEditor 多行**（点 "📋 导入" 打开右侧抽屉 width=400）：
```
批量导入 polygon 顶点
每行一个 "lat,lng"，支持表头自动跳过：
┌────────────────────────────┐
│ 30.6712,104.0631           │  textarea
│ 30.6720,104.0640           │  ≥6 行
│ 30.6730,104.0635           │
└────────────────────────────┘
解析预览：3 个顶点 ✓
[替换现有 polygon] [追加到现有 polygon] [取消]
```

### 9.5 智能 lat/lng 识别（`smartParse`）

```typescript
function smartParse(a: number, b: number): LatLng | null {
  const isLat = (n: number) => n >= -90 && n <= 90
  const isLng = (n: number) => n >= -180 && n <= 180

  if (isLat(a) && isLng(b)) {
    // 二次判断：a 与 b 都能当 lat 时，按中国典型范围（lat~25-45, lng~73-135）翻转
    if (isLng(a) && isLat(b) && (b > 25 && b < 45 && a > 73 && a < 135)) {
      return { lat: b, lng: a }
    }
    return { lat: a, lng: b }
  }
  if (isLng(a) && !isLat(a) && isLat(b)) return { lat: b, lng: a }
  return null
}
```

## 10. 消费方迁移

### 10.1 `HazardPoint.vue`

删除：
- mapDialog 模板内手写 toolbar 与 click 处理（line 284-330）
- `polygonCoords` / `strikeCoords` / `strikeAngle` refs
- `initMap()` / `setDrawMode()` / `clearDraw()` / `handleMapConfirm()` 等方法

替换为：

```vue
<el-dialog v-model="mapDialogVisible" title="绘制隐患点范围" width="900px"
           destroy-on-close @opened="mapEditorRef?.invalidate()">
  <MapBoundaryEditor ref="mapEditorRef"
                     v-model="formData.boundaryCoords"
                     v-model:center="formCenter"
                     height="500px"/>
  <template #footer>
    <el-button @click="mapDialogVisible = false">取消</el-button>
    <el-button type="primary" @click="mapDialogVisible = false">完成</el-button>
  </template>
</el-dialog>
```

```typescript
formData.boundaryCoords = deserialize(row.boundaryCoords)   // handleEdit 内回填
formCenter.value = { lat: Number(row.latitude), lng: Number(row.longitude) }

// handleSubmit
const payload = {
  ...formData,
  longitude: formCenter.value.lng,
  latitude:  formCenter.value.lat,
  boundaryCoords: serialize(formData.boundaryCoords)
}
delete payload.strike
```

### 10.2 `Device.vue`

```vue
<el-dialog v-model="mapDialogVisible" :title="mapViewOnly ? '查看安装位置' : '在地图上选择安装位置'"
           width="700px" destroy-on-close @opened="pickerRef?.invalidate()">
  <MapPointPicker ref="pickerRef"
                  v-model="pickerLngLat"
                  :readonly="mapViewOnly"
                  :overlay-polygon="boundHpPolygon"
                  height="400px"/>
  <template #footer>...</template>
</el-dialog>
```

```typescript
const openMapPicker = async () => {
  mapViewOnly.value = false
  pickerLngLat.value = formData.longitude && formData.latitude
    ? { lng: formData.longitude, lat: formData.latitude } : null

  boundHpPolygon.value = null
  if (formData.boundHazardPointId) {
    try {
      const res = await getHazardPointDetail(String(formData.boundHazardPointId))
      const bc = deserialize(res.data.boundaryCoords)
      if (bc.polygon.length >= 3) boundHpPolygon.value = bc.polygon
    } catch { /* 静默：不阻塞选址 */ }
  }
  mapDialogVisible.value = true
}
```

`formData.boundHazardPointId` 来自后端 `IDeviceHazardRelationService.getHazardPointByDeviceId` 扩展返回（§7.5）。

### 10.3 `VideoDevice.vue`

```vue
<MapPointPicker v-model="pickerLngLat" height="400px"/>
```

不传 `overlayPolygon`（需求 4 仅指 Device.vue）。

## 11. 测试策略

### 11.1 引入 Vitest

```bash
npm i -D vitest @vitest/coverage-v8
```

`package.json` 加：
```json
"test": "vitest run",
"test:watch": "vitest",
"test:coverage": "vitest run --coverage"
```

测试文件位置：`web/src/lib/__tests__/`、`web/src/composables/__tests__/`。

### 11.2 单元测试详细用例

> 下方所有"#"对应的中文描述为**场景描述**，实际测试名以英文 `describe()`/`it()` 命名（如 `it('deserialize: legacy strikeCoords key → mapped to strikeLine')`）。

#### `lib/boundaryCoords.test.ts`

**`serialize` / `deserialize` 对称性**：
| # | 输入 | 期望输出 |
|---|---|---|
| 1 | `EMPTY_BOUNDARY` → serialize | `'{"polygon":[],"strikeLine":null,"auxiliaryLines":[]}'` |
| 2 | 完整边界 → serialize → deserialize | 与原对象 deep-equal |
| 3 | `deserialize(null)` | `EMPTY_BOUNDARY` |
| 4 | `deserialize(undefined)` | `EMPTY_BOUNDARY` |
| 5 | `deserialize('')` | `EMPTY_BOUNDARY` |
| 6 | `deserialize('invalid json')` | `EMPTY_BOUNDARY`（不抛异常） |
| 7 | `deserialize('{}')` | `EMPTY_BOUNDARY` |

**旧 key 兼容**：
| # | 输入 JSON | 期望 |
|---|---|---|
| 8 | `{"polygon":[...],"strikeCoords":[[a,b],[c,d]]}` | `strikeLine: [[a,b],[c,d]]`, `strikeCoords` 不出现在结果中 |
| 9 | `{"strikeAngle":152}` | strikeAngle 字段被忽略（结果中无此字段） |
| 10 | 同时有 `strikeLine` 和 `strikeCoords` | 优先用 `strikeLine`（新 key 胜出） |
| 11 | `strikeCoords` 长度 != 2 | `strikeLine: null` |

**polygon 对象格式防御**：
| # | 输入 | 期望 |
|---|---|---|
| 12 | `polygon: [{lat:1,lng:2},{lat:3,lng:4}]` | 归一化为 `[{lat:1,lng:2},{lat:3,lng:4}]` |

**`centroid` 算法 (b)**：
| # | 输入 polygon | 期望 |
|---|---|---|
| 13 | `[]` | `null` |
| 14 | 1 个点 | `null` |
| 15 | 2 个点 | `null` |
| 16 | 正三角形 `(0,0)(0,3)(3,0)` | `{lat:1, lng:1}`（精度 0.001） |
| 17 | 单位正方形 `(0,0)(0,1)(1,1)(1,0)` | `{lat:0.5, lng:0.5}` |
| 18 | 不规则凸四边形 | 用 NumPy 标准公式手算结果对比，精度 0.001 |
| 19 | 共线退化（3 点在一条线上） | `null`（带符号面积 = 0） |
| 20 | 顺时针 vs 逆时针 同形 | 结果相同（取绝对值） |

**`strikeAngle` 方位角**：
| # | 起点 → 终点 | 期望角度（度） |
|---|---|---|
| 21 | (0,0) → (1,0)（向北） | 0 |
| 22 | (0,0) → (0,1)（向东） | 90 |
| 23 | (0,0) → (-1,0)（向南） | 180 |
| 24 | (0,0) → (0,-1)（向西） | 270 |
| 25 | (0,0) → (1,1)（东北） | 45 |
| 26 | (0,0) → (-1,1)（东南） | 135 |
| 27 | (45°N, 179°E) → (45°N, -179°E)（跨经度 180°） | 接近 90（验证球面 atan2） |
| 28 | (0,0) → (0,0)（同点） | 0 或 NaN，文档明确 |

#### `lib/coordParser.test.ts`

**`parseSingle`**：
| # | 输入 | 期望 |
|---|---|---|
| 1 | `"30.67,104.06"` | `{lat:30.67, lng:104.06}` |
| 2 | `"30.67, 104.06"`（带空格） | 同上 |
| 3 | `"30.67  104.06"`（多空格） | 同上 |
| 4 | `"104.06,30.67"`（顺序反，中国典型） | smart-flip 为 `{lat:30.67, lng:104.06}` |
| 5 | `"30.67"`（仅一个数） | `null` |
| 6 | `"abc,def"` | `null` |
| 7 | `"200,30"`（200 越界） | `null` |
| 8 | `"-30.5,104.5"` | `{lat:-30.5, lng:104.5}` |
| 9 | `""` | `null` |
| 10 | `"   "` | `null` |
| 11 | `"30.67;104.06"`（分号分隔） | `{lat:30.67, lng:104.06}` |
| 12 | `"30.67\t104.06"`（Tab 分隔） | 同上 |
| 13 | `"(30.67, 104.06)"`（带括号） | 同上（剥离） |

**`parseMultiline`**：
| # | 输入（多行字符串） | 期望 |
|---|---|---|
| 14 | 3 行有效 lat,lng | 3 个 LatLng |
| 15 | 含表头 `lat,lng\n30.67,104.06` | 1 个 LatLng（表头跳过） |
| 16 | 含空行 | 空行跳过 |
| 17 | 含 `#` 开头注释行 | 注释行跳过 |
| 18 | 含 1 个无效行 + 4 个有效行 | 返回 4 个 LatLng + 报错行号 = 错行的 lineNo |
| 19 | 全是无效行 | 空数组 + 报错列表 |
| 20 | tab-separated 5 行 | 5 个 LatLng |
| 21 | 0 顶点（empty input） | `[]` |
| 22 | 1001 顶点 | 截断到 1000 或返回错误（文档定义） |

#### `composables/useLeafletMap.test.ts`

由于涉及 DOM，需要 `jsdom` 环境（Vitest 默认支持）。

| # | 场景 | 期望 |
|---|---|---|
| 1 | container ref = null | map 仍为 null，isReady 为 false |
| 2 | container ref → 设为 DOM 节点 | initMap 被调用，map 非 null，isReady 变 true |
| 3 | container ref 设为节点后再设回 null（模拟 destroy-on-close） | destroyMap 被调用，map 重新为 null |
| 4 | 同一 hook 实例先设节点 A → 再设节点 B | A 的 instance 被销毁，B 的 instance 被创建 |
| 5 | onBeforeUnmount 触发 | destroyMap 被调用 |
| 6 | invalidate() 调用 | nextTick 后 map.invalidateSize() 被调用 |

### 11.3 手工回归 Checklist

#### 走向 / 辅助线 / 中心点

| # | 场景 | 期望 |
|---|---|---|
| 1 | 编辑已有 polygon 的 HP | polygon/走向/辅助线/中心点全部正确显示 |
| 2 | 拖动 polygon 顶点 | 中心实时跟随质心 |
| 3 | 删除 polygon 单个顶点 | 中心实时跟随；剩 2 点时中心保留位置不重算 |
| 4 | 手动拖中心后再拖顶点 | 中心**不**再跟随 |
| 5 | 点 "↻ 重置中心" | 中心回到当前 polygon 质心；后续顶点变动恢复跟随 |
| 6 | 画走向（点两次） | 自动退出工具；端点标签 "152°" 显示在终点外 |
| 7 | 已存在走向时再次点 "↗ 走向" | 弹 `[重新绘制 / 调整端点 / 取消]` |
| 8 | 选"调整端点" | 进入端点拖拽模式，可拖任一端 |
| 9 | 选"重新绘制" | 旧线消失，从起点开始重画 |
| 10 | 画多条辅助线 | 颜色橙色 `#fa8c16` 虚线，无角度标签 |
| 11 | 选中辅助线后点 "🗑 删除选中" | 该条删除，其他保留 |
| 12 | 走向已存在时画辅助线 | 互不影响 |

#### 坐标粘贴

| # | 场景 | 期望 |
|---|---|---|
| 13 | PointPicker 粘贴 "30.67,104.06" + 回车 | marker 落点 + 地图聚焦该点 |
| 14 | PointPicker 粘贴 "104.06,30.67"（顺序反） | smart-flip 落到 lat=30.67,lng=104.06 |
| 15 | PointPicker 粘贴非法字符串 | 输入框红边 + tooltip 报错 |
| 16 | BoundaryEditor 多行粘贴 → "替换" | polygon 全替换为粘贴的顶点 |
| 17 | BoundaryEditor 多行粘贴 → "追加" | polygon 在原有基础上追加 |
| 18 | 含表头粘贴 | 表头跳过 |
| 19 | 含异常行粘贴 | 异常行红色高亮 + 行号提示 |

#### Bug 修复验证

| # | 场景 | 期望 |
|---|---|---|
| 20 | Device.vue 第一次打开地图 | 地图正常加载 |
| 21 | Device.vue 关闭再打开（连续 3 次） | 每次都正常（修复 B1） |
| 22 | Device.vue 绑定 HP 后打开地图 | HP polygon 淡色（透明度 30%）叠加 |
| 23 | Device.vue 未绑定 HP 时打开地图 | 仅显示选址，无叠加 |
| 24 | HazardPoint.vue 编辑已有边界的 HP，打开地图 | polygon + 走向 + 辅助线 + 中心**全部**显示（修复 B2） |
| 25 | 老数据（含 `strikeCoords` 旧 key）回显 | 自动映射为 `strikeLine` 渲染 |
| 26 | 编辑老数据后保存 → 重新打开 | 数据库内已升级为 `strikeLine` 新 key |

#### 后端校验

| # | 场景 | 期望 |
|---|---|---|
| 27 | 提交 polygon 仅 2 个顶点 | 400 `boundary_coords: polygon must have >=3 vertices` |
| 28 | 提交 strikeLine 长度 3 | 400 `boundary_coords: strikeLine must have exactly 2 points` |
| 29 | 提交 auxiliaryLine 长度 1 | 400 `boundary_coords: auxiliaryLine #N must have >=2 vertices` |
| 30 | 提交非法 JSON 字符串 | 400 `boundary_coords: invalid JSON` |
| 31 | 提交 lat=100 | 400 `boundary_coords: lat out of range` |
| 32 | 提交 polygon 1001 顶点 | 400 `boundary_coords: polygon size exceeds 1000` |
| 33 | 提交合法边界 → GET 回 | 返回 JSON 字符串，前端 deserialize 后等价 |

#### 防丢失

| # | 场景 | 期望 |
|---|---|---|
| 34 | 弹窗内有未保存编辑 → 点取消 | 弹"放弃编辑？" 确认 |
| 35 | 弹窗内未编辑 → 点取消 | 直接关闭，不弹确认 |

### 11.4 覆盖率目标

- 纯函数（`lib/*`）：≥ 90% 行覆盖
- composable：≥ 80%
- 组件：手工 checklist 覆盖所有 35 项

## 12. 实施顺序

```
PR 1: 基础设施 + 单测                          [零业务影响]
  - npm i @geoman-io/leaflet-geoman-free
  - npm i -D vitest @vitest/coverage-v8
  - lib/boundaryCoords.ts + 22 单测
  - lib/coordParser.ts + 22 单测
  - composables/useLeafletMap.ts + 6 单测
  合并条件：所有单测通过，build 不破

PR 2: MapPointPicker + VideoDevice 迁移        [小试牛刀]
  - components/map/MapPointPicker.vue
  - components/map/MapCoordInput.vue
  - views/basic/VideoDevice.vue 迁移
  - 手工回归 #13, #14, #15, #20, #21
  合并条件：VideoDevice 全场景正常

PR 3: Device.vue 迁移 + 后端接口扩展           [验证 overlay]
  - 后端: DeviceHazardRelationServiceImpl
    .getHazardPointByDeviceId(deviceId): HazardPointRef
  - views/basic/Device.vue 迁移 + boundHpPolygon 加载
  - 手工回归 #22, #23
  合并条件：B1 修复 + overlay 正确显示

PR 4: MapBoundaryEditor + HazardPoint 迁移 + 后端 DTO
  - components/map/MapBoundaryEditor.vue
  - 后端: BoundaryCoordsDTO + BoundaryCoordsValidator
  - HazardPointController 注入 validator
  - DB: ALTER TABLE hazard_point DROP COLUMN strike
  - views/basic/HazardPoint.vue 迁移
  - 手工回归 #1-#12, #16-#19, #24-#35
  合并条件：B2 修复 + 所有 checklist 项通过
```

PR 1-3 可并行（无依赖）；PR 4 依赖 PR 1。

## 13. 风险

| 风险 | 等级 | 影响 | 缓解 |
|---|---|---|---|
| Geoman 中文 i18n 不全 | MED | 部分提示残留英文 | 自写工具栏 + tooltip，Geoman 仅暴露原语 API |
| Geoman 与 leaflet-draw 共存 | LOW | Settings.vue / MapDrawToolbar 仍用 leaflet-draw | 两库独立挂载在不同 map 实例，无全局污染 |
| Bundle size +120KB | LOW | 首屏加载略增 | Vite 路由懒加载已分包，仅在 basic/* 路由加载 |
| 第三方消费方解析旧 `strikeCoords` key | MED | 已读不到走向 | 兼容期保留旧 key 输出 6 个月；列入对接通知 |
| 质心算法对凹多边形不友好 | LOW | 凹形 HP 中心点在多边形外 | 用户可手动拖中心；后续可升级 polylabel |
| 打开地图弹窗瞬间地图错位 | LOW | Leaflet 通病 | `@opened="...invalidate()"` 防御性补 |
| 未保存编辑被关闭丢失 | MED | 用户误关弹窗丢失工作 | `beforeClose` 钩子 detect dirty 状态弹确认 |
| Geoman 与 Leaflet 1.9 不兼容 | LOW | 安装即报错 | 已确认 `@geoman-io/leaflet-geoman-free@2.x` 支持 Leaflet ≥1.7 |

## 14. Out-of-Scope（列入 V2）

| 项 | 原因 |
|---|---|
| `Settings.vue` / `MapDrawToolbar.vue` 迁移 | 语义不同（临时画图 vs 业务边界），二期统一 |
| KML / CSV / GeoJSON 文件上传 | 现实多用 Excel 粘贴；扩展点已留 |
| 天地图 key 抽 `.env` | 收口到 composable 已大幅减风险 |
| HP 绑定设备对话框内的"安装位置"地图 UX（入口 2） | 当前数字传参，UI 工作量 +50% |
| `device_hazard_point` 加 `UNIQUE(device_id)` + 业务校验 | 已确认"前端做完再修后端" |
| `hazard_point.longitude/latitude` 合并进 `boundary_coords.center` | 当前作为查询性能优化保留 |
| Dashboard / 小程序只读地图统一 | 无编辑，本期不动 |

## 15. 开放问题 / 依赖

- **后端接口扩展依赖**：`IDeviceHazardRelationService.getHazardPointByDeviceId` 必须在 PR 3 中先于 Device.vue 迁移上线
- **数据迁移依赖**：PR 4 的 `ALTER TABLE` 在生产环境执行时间窗口需评估
- **第三方消费方通知**：如有外部系统读 `boundary_coords` 的 `strikeCoords` 旧 key，须在 PR 4 上线前通知配合升级

## 16. 附录

### 16.1 关键 import

```typescript
// 前端
import MapPointPicker from '@/components/map/MapPointPicker.vue'
import MapBoundaryEditor from '@/components/map/MapBoundaryEditor.vue'
import {
  EMPTY_BOUNDARY,
  serialize,
  deserialize,
  centroid,
  strikeAngle,
  type BoundaryCoords,
  type LatLng
} from '@/lib/boundaryCoords'
import { useLeafletMap } from '@/composables/useLeafletMap'
import { parseSingle, parseMultiline } from '@/lib/coordParser'
```

```java
// 后端
import com.zwei.iot.hazardpoint.domain.dto.BoundaryCoordsDTO;
import com.zwei.iot.hazardpoint.service.BoundaryCoordsValidator;
import com.zwei.iot.device.service.IDeviceHazardRelationService.HazardPointRef;
```

### 16.2 估算工作量

| 阶段 | 估算 |
|---|---|
| PR 1（基础设施 + 单测） | ~500 行新增（含测试） |
| PR 2（PointPicker + VideoDevice） | ~320 行净增 |
| PR 3（Device + 后端扩展） | ~120 行净增 |
| PR 4（BoundaryEditor + HP + 后端 DTO + DDL） | ~430 行净增 |
| **总计** | **~1370 行净增**（含测试），删 ~330 行旧代码 |

不做"时间估算"，按"切片可独立合并 + 每片有合并门禁"组织。

---

**END OF SPEC**
