# 设计文档：地图编辑 Composable 化重构（第二期）

| 字段 | 值 |
|---|---|
| 作者 | Claude (brainstorming session) |
| 创建日期 | 2026-06-11 |
| 状态 | 待评审 |
| 上一期设计 | [2026-06-11-map-boundary-component-design.md](./2026-06-11-map-boundary-component-design.md) |
| 涉及代码区域 | `web/src/components/map/`, `web/src/composables/`, `web/src/lib/`, `web/src/views/basic/{HazardPoint,Device}.vue` |

## 1. 摘要

在第一期（`<MapPointPicker>` / `<MapBoundaryEditor>` + Leaflet-Geoman 引擎）的基础上，**干掉 Geoman 依赖**，把所有地图编辑原语（顶点/中心/线段拖拽、选中、插入、删除、绘制、模式切换）抽到一个 `useMapEditor` Vue composable 中。`<MapBoundaryEditor>` 和 `<MapPointPicker>` 退化为纯壳组件（<200 行），只负责模板渲染和事件转发。同步抽出 `lib/mapGeometry.ts` 纯几何工具库（命中检测、自相交、插入/删除），独立单测。修复第一期遗留的"删除选中"按钮无效、`pm.enable()` 私有 API hack 等问题。

**范围限定：**
- ✅ 重写 `useMapEditor` composable（新增）
- ✅ 重写 `MapBoundaryEditor.vue`（瘦身 + 干掉 Geoman）
- ✅ 重写 `MapPointPicker.vue`（基于新 composable）
- ✅ 新增 `lib/mapGeometry.ts`（纯函数）
- ✅ 扩展 `lib/boundaryCoords.ts`（新增 `assertValidBoundary`）
- ❌ 不动 dashboard 三件套 `MapDrawToolbar / MapBusinessToolbar / MapAuxiliaryBar`（另一套体系，留作第三期）
- ❌ 不动 `MapCoordInput.vue`（沿用）

## 2. 问题陈述

### 2.1 现有问题

| 编号 | 问题 | 文件:行 |
|---|---|---|
| P1 | "删除选中"按钮是占位实现，只删最后一条辅助线 | `MapBoundaryEditor.vue:264-277` |
| P2 | 编辑已有多边形需要再次点击工具栏按钮进入 Geoman 编辑态，UX 割裂 | `MapBoundaryEditor.vue:200-217` |
| P3 | 顶点 marker 是 divIcon 但**不可拖**——拖顶点完全依赖 Geoman `pm.enable()` 私有 API | `MapBoundaryEditor.vue:121-130` |
| P4 | strike 和 aux 用两套不同交互（点击两次 vs Geoman 自由绘制），缺乏一致性 | `MapBoundaryEditor.vue:231-262` |
| P5 | 中心点 + 顶点的"可拖" vs 地图"可平移"无视觉区分——用户不知道点了会怎样 | `MapBoundaryEditor.vue:166-170` |
| P6 | `MapPointPicker` 和 `MapBoundaryEditor` 共享 useLeafletMap 但编辑原语 0 复用 | 两个文件 |
| P7 | 几何计算（命中检测、插入点）混在 `MapBoundaryEditor.vue` 里，无法独立单测 | `MapBoundaryEditor.vue:195-262` |

### 2.2 用户原话需求

1. "通过顶点确定区域，然后自动计算中心点，用户也可以拖拽中心点手动修改"
2. "用户可以自由绘制走向和辅助线"
3. "绘制完成后顶点需要标注出，并可以拖拽修改位置，要和地图的拖拽功能做出仔细的区分"

## 3. 决策汇总（来自 brainstorming 阶段）

| # | 决策 | 选项 |
|---|---|---|
| 1 | 范围 | B：抽 `useMapEditor` composable 作为通用编辑原语，两个组件基于它；dashboard 三件套暂不动 |
| 2 | 中心点 ↔ 多边形 | B：几何派生（质心）+ 手动拖拽后 `manualCenterLocked=true` + "重置中心"按钮解锁 |
| 3 | 顶点拖拽 vs 地图拖拽的消歧 | A：显式"编辑模式"开关，非编辑态只读 |
| 4 | 绘制流程 | B：自管全流程，干掉 Geoman；编辑态下点工具→点顶点→Enter/双击闭合 |
| 5 | 走向 vs 辅助线 | A：两套独立工具（走向=地质数据，辅助=工程标注） |
| 6 | 顶点视觉风格 | 1：28-30px 绿色编号圆点（沿用现状打磨） |
| 7 | 工具栏布局 | A：顶部水平 Element Plus button-group（沿用现状） |
| 8 | 删除 UX | B：点选（红色高亮环）+ Delete 键 / 工具栏"删除选中" |
| 9 | 实现方案 | A：纯 Composable（薄壳组件 + 自包含 composable） |

## 4. 架构

```
父页面 (HazardPoint.vue / Device.vue)
  │  props / emits
  ▼
组件层 (薄壳 · <200 行)
  ├─ MapBoundaryEditor.vue (boundary 模式)
  └─ MapPointPicker.vue    (point 模式)
  │  调用 useMapEditor()
  ▼
Composable 层 (核心 · 400-600 行)
  └─ useMapEditor(options) → { state, actions, refs }
     ├─ 状态机: IDLE → EDIT → DRAW-* → EDIT
     ├─ Leaflet 交互: 顶点/中心/线段拖拽、选中、绘制
     └─ 派生数据: canSave, strikeAngle, edgeMidpoints
  │
  ▼
基础设施层 (复用 + 新增)
  ├─ useLeafletMap       (已存在)
  ├─ lib/boundaryCoords  (扩展 + assertValidBoundary)
  └─ lib/mapGeometry     (新增 · 纯函数)
```

**关键边界：**
- Composable 自包含：拿到 map ref 后，所有 Leaflet layer/事件注册/清理都在 composable 内部完成
- 组件层不引用 `L.xxx`，不写任何几何/状态计算
- `lib/mapGeometry.ts` 0 Leaflet 依赖，0 Vue 依赖，可独立单测

## 5. Composable API

```ts
// composables/useMapEditor.ts

import type { Ref, ShallowRef, ComputedRef } from 'vue'
import type { Map as LMap } from 'leaflet'
import type { LatLng, BoundaryCoords } from '@/lib/boundaryCoords'

export type EditorMode = 'view' | 'edit'
export type EditorTool = null | 'polygon' | 'strike' | 'aux'
export type MapEditorVariant = 'boundary' | 'point'

export interface VertexId { kind: 'polygon-vertex'; index: number }
export interface StrikeId { kind: 'strike-endpoint'; index: 0 | 1 }
export interface AuxId { kind: 'aux-line'; index: number }
export type SelectableId = VertexId | StrikeId | AuxId

export interface UseMapEditorOptions {
  container: Ref<HTMLElement | null | undefined>
  variant: MapEditorVariant
  initialBoundary?: BoundaryCoords | null
  initialCenter?: LatLng | null
  initialPoint?: LatLng | null
  pointValue?: Ref<LatLng | null>
  overlayPolygon?: LatLng[] | null
  defaultCenter?: LatLng
  defaultZoom?: number
  readonly?: boolean
  tianditu?: boolean
  onChange?: (snapshot: EditorSnapshot) => void
  onCenterChange?: (center: LatLng | null) => void
}

export interface EditorSnapshot {
  mode: EditorMode
  polygon: LatLng[]
  strikeLine: [LatLng, LatLng] | null
  auxiliaryLines: LatLng[][]
  center: LatLng | null
  manualCenterLocked: boolean
  strikeAngle: number | null
}

export interface VertexHandle {
  id: SelectableId
  position: LatLng
}

export interface UseMapEditorReturn {
  // 地图底层
  mapRef: ShallowRef<LMap | null>
  isReady: Ref<boolean>
  containerRef: Ref<HTMLElement | null | undefined>
  invalidate: () => void
  setView: (p: LatLng, zoom?: number) => void
  destroy: () => void

  // 编辑器状态
  mode: Ref<EditorMode>
  tool: Ref<EditorTool>
  canEdit: ComputedRef<boolean>
  canSave: ComputedRef<boolean>

  // 数据
  polygon: Ref<LatLng[]>
  strikeLine: Ref<[LatLng, LatLng] | null>
  auxiliaryLines: Ref<LatLng[][]>
  center: Ref<LatLng | null>
  manualCenterLocked: Ref<boolean>
  strikeAngle: ComputedRef<number | null>

  // 选中
  selectedId: Ref<SelectableId | null>
  selectedVertex: ComputedRef<VertexHandle | null>

  // 行为
  enterEdit: () => void
  exitEdit: () => void
  toggleEdit: () => void
  activateTool: (t: EditorTool) => void
  cancelTool: () => void

  addVertex: (p: LatLng) => void
  moveVertex: (id: VertexId, p: LatLng) => void
  removeVertex: (id: VertexId) => void
  insertVertexAfter: (afterId: VertexId, p: LatLng) => void

  setStrike: (a: LatLng, b: LatLng) => void
  moveStrikeEndpoint: (idx: 0 | 1, p: LatLng) => void
  removeStrike: () => void

  addAuxLine: (points: LatLng[]) => void
  moveAuxPoint: (lineId: number, pointId: number, p: LatLng) => void
  removeAuxLine: (lineId: number) => void

  setCenter: (p: LatLng, manual?: boolean) => void
  moveCenter: (p: LatLng) => void
  resetCenter: () => void

  select: (id: SelectableId | null) => void
  clearSelection: () => void
  removeSelected: () => void

  clearAll: () => void

  // 状态快照
  snapshot: () => EditorSnapshot
}
```

**关键设计点：**
- `container` 必填 Ref，组件层用 `template ref` 传入；composable watch 它决定 init/destroy 时机
- `variant: 'boundary' | 'point'` 决定启用的能力子集；`point` 模式下 polygon/strike/aux 的 actions 调用是 no-op（带 dev warning）
- `pointValue: Ref<LatLng | null>` 是 v-model 桥接：父组件传 ref，composable 监听并同步内部 state
- IDs 是结构化对象（discriminated union）— TypeScript narrowing 友好
- `canSave` computed 反映数据是否合法：**polygon ≥ 3 顶点**（center 由 polygon 派生，无独立校验）。strike / aux 为可选，不阻塞保存。父组件用此控制"完成"按钮 disabled

## 6. 状态机

```
                     enterEdit()                 activateTool(t)
        ┌───────────────────────────┐    ┌─────────────────────────────┐
        │                           ▼    ▼                             │
   ┌────┴────┐    toggleEdit     ┌──────────┐   activateTool(t)  ┌─────┴─────┐
   │  IDLE   ├──────────────────▶│   EDIT   │───────────────────▶│ DRAW-*    │
   │  (只读)  │◀──────────────────┤  (主编辑) │◀──────────────────┤  (绘制中) │
   └─────────┘    toggleEdit     └──────────┘   完成/取消/Esc     └───────────┘
                                          │
                                          │ 点"完成编辑" / exitEdit()
                                          ▼
                                       (回到 IDLE)
```

**DRAW-* 子状态规则：**
- 地图拖动/缩放**禁用**（避免绘制中误触）
- polygon: 首个 ≥ 3 点后, Enter 或 dbl-click 第一点 → 闭合
- strike: 点 2 个点后自动完成
- aux: 任意点数后 Enter 或 dbl-click → 完成
- Esc → 取消当前绘制, 回到 EDIT

**异常路径：**
- EDIT 态尝试删最后一个多边形顶点 → 阻止, 提示"至少需要 3 个顶点"
- DRAW-* 中拖顶点 → 忽略（绘制时顶点不可拖）
- IDLE 态收到外部 props 变化 → 静默同步，不进入 EDIT
- `readonly=true` → 强制 IDLE，工具栏隐藏
- `canSave=false` 时点"完成" → 阻止 emit done，工具栏 hint 提示缺什么

**关键不变量（任何状态下必须满足）：**
- polygon 要么空，要么 ≥ 3 顶点
- strikeLine 要么 null，要么恰好 [a, b] 两个点
- auxiliaryLines 每条 ≥ 2 顶点
- center: 仅当 polygon ≥ 3 顶点时存在
- 任何顶点移动 → center 自动重算（除非 `manualCenterLocked`）

## 7. 组件层

### 7.1 MapBoundaryEditor.vue

`<200 行` 薄壳。模板包含：工具栏 / 地图 div / 提示条 / 导入抽屉 / 完成取消按钮。Script：调用 `useMapEditor({ variant: 'boundary', ... })`，把 composable 的 state/refs 绑到模板；监听 onChange 决定是否 emit `update`。键盘事件：Delete 删选中、Esc 退工具/退编辑。

### 7.2 MapPointPicker.vue

`<150 行` 薄壳。模板：地图 div + 坐标输入（沿用 `MapCoordInput`）。Script：调用 `useMapEditor({ variant: 'point', pointValue: localPoint, overlayPolygon })`，双向同步 props.modelValue。

**组件层 3 条铁律：**
1. 组件里没有任何 `L.xxx` 引用
2. 组件里没有任何几何/状态计算
3. 组件的 props 永远是初始值，双向同步走 v-model/emits，不在组件层写 watch+assign 反向污染

## 8. 数据流

```
用户操作 (mousedown/drag/click/key)
  │
  ▼
Leaflet 事件回调 (composable 内部注册)
  │
  ▼
composable action 函数 (addVertex / moveVertex / ...)
  │
  ▼
内部 reactive state 更新
  │
  ├─▶ watchEffect → 重新渲染 Leaflet layers
  │
  └─▶ 组件层 watch(state) → emit update / onChange 回调
        │
        ▼
      父页面更新 formData
```

**关键：**
- 单向数据流：Leaflet 事件 → action → state → watch → emit
- 反向同步走受控 props（pointValue ref / initialBoundary watch）
- 没有 `L.xxx` 在组件层调用

## 9. lib/mapGeometry.ts（新增）

纯函数模块，0 Leaflet 依赖，0 Vue 依赖：

```ts
export function midpoint(a: LatLng, b: LatLng): LatLng
export function edgeMidpoint(polygon: LatLng[], i: number): LatLng
export function edgeVertices(polygon: LatLng[], i: number): [LatLng, LatLng]
export function hitEdge(p: LatLng, polygon: LatLng[], i: number, toleranceMeters: number): boolean
export function hitVertex(p: LatLng, target: LatLng, toleranceMeters: number): boolean
export function hitPolyline(p: LatLng, line: LatLng[], toleranceMeters: number): boolean
export function isSelfIntersecting(polygon: LatLng[]): boolean
export function insertVertexAtEdge(polygon: LatLng[], edgeIndex: number, p: LatLng): LatLng[]
export function removeVertexSafe(polygon: LatLng[], index: number): LatLng[] | null
export function metersPerPixel(lat: number, zoom: number): number
```

**为什么独立成 lib：** 纯数学，0 框架依赖，脱离整个地图栈独立单测；未来若换地图库可原样搬走。

## 10. lib/boundaryCoords.ts 扩展

保留：LatLng、BoundaryCoords、EMPTY_BOUNDARY、serialize、deserialize、centroid、strikeAngle

新增：`assertValidBoundary(b: BoundaryCoords): string | null` — 返回错误信息或 null（用于 canSave 和后端 DTO 校验对齐）

## 11. 错误处理

| 场景 | 处理 |
|---|---|
| 多边形顶点 < 3 试图保存 | `canSave=false` → 完成按钮 disabled + hint 提示 |
| 删除最后一个有效顶点 | 阻止，ElMessage.warning 提示"至少需要 3 个顶点" |
| 自相交多边形 | `isSelfIntersecting` 检测到时不阻止绘制，但 `canSave=false` |
| 外部 props 变化（initialBoundary / initialPoint） | 静默同步内部 state，不弹提示 |
| readonly=true | 强制 IDLE 模式，工具栏隐藏 |
| 顶点移动后多边形面积 < 阈值 | 暂不校验，留作第三期 |
| Leaflet 容器未挂载 | `isReady=false`，工具栏隐藏，等待 ready |
| 模式切换时丢失未保存草稿 | DRAW-* 状态下切工具 = 提交草稿（按完成处理）；切回 IDLE = 丢弃（需 confirm） |

## 12. 测试策略

| 层 | 工具 | 覆盖目标 |
|---|---|---|
| lib/boundaryCoords.ts | Vitest (已有 22 用例) | 保留 |
| lib/coordParser.ts | Vitest (已有 21 用例) | 保留 |
| lib/mapGeometry.ts（新） | Vitest | 80%+ 覆盖，至少 25 个新用例 |
| composables/useMapEditor.ts | Vitest + @vue/test-utils | 状态转换、actions 副作用、computed 正确性 |
| MapBoundaryEditor.vue | Vitest + @vue/test-utils | props → emit done 流程、canSave 反映在按钮、键盘事件转发 |
| MapPointPicker.vue | Vitest + @vue/test-utils | v-model 双向同步、overlayPolygon 渲染 |
| E2E | Playwright（已有 .playwright-mcp/） | 关键流程：新增 HP、编辑 HP 顶点、删除顶点、添加/删除辅助线、设置走向、点位选择 |

**不引入：** Cypress、Testing Library、Happy DOM 等新框架。**不写 Geoman 测试**（已删除）。**不 mock Leaflet**——composable 测试用 `shallowRef<LMap | null>(null)` 跳过 Leaflet 调用，只测纯逻辑分支。

## 13. 迁移计划

按以下顺序提交，每个 PR 独立可发布：

1. **PR1**: 新增 `lib/mapGeometry.ts` + 单测；扩展 `lib/boundaryCoords.ts` 加 `assertValidBoundary` + 单测
2. **PR2**: 新增 `composables/useMapEditor.ts` + 单测；不改任何调用方
3. **PR3**: 重写 `MapBoundaryEditor.vue` 切到新 composable；保留旧版本导出作为 shim 1 个发布
4. **PR4**: 重写 `MapPointPicker.vue` 切到新 composable
5. **PR5**: 从 `HazardPoint.vue` 和 `Device.vue` 移除对旧 `@geoman-io/leaflet-geoman-free` 引用；从 package.json 移除（如果 dashboard 三件套也确认未用）

**回退策略：** 每个 PR 切换时保留旧组件路径（`@/components/map/MapBoundaryEditor.legacy.vue`），父页面切换 import 即可回退。PR3 验证通过后删除 legacy 文件。

## 14. 风险与缓解

| 风险 | 缓解 |
|---|---|
| 自管交互的拖拽性能（顶点/中心频繁重算） | 顶点移动用 `requestAnimationFrame` 批处理；center 重算用 debounce 50ms |
| 干掉 Geoman 后边界编辑手感变差 | 第一期有 Geoman 经验，参考其 UX；E2E 测试覆盖手感 |
| 触屏设备体验未覆盖 | 本期不优化触屏（沿用 Leaflet 默认），留作第三期 |
| `lib/mapGeometry.ts` 的命中检测容差 | 用 `metersPerPixel(lat, zoom)` 动态计算，zoom 大时容差大 |
| 旧组件的边角行为（如"删除最后一条辅助线"）被破坏 | E2E 覆盖每个旧功能点 |

## 15. 开放问题（第三期）

- 触屏/移动端支持
- 撤销/重做（undo/redo）
- 顶点的吸附（snapping to existing vertex/edge）
- 多选（multi-select）
- 辅助线的"标签"（给每条辅助线命名）
- 隐患点详情页的只读地图也用同一个 composable（readonly=true）
- dashboard 的三件套改造

## 16. 变更记录

| 时间 | 变更 |
|---|---|
| 2026-06-11 | 首版设计（brainstorming 完成后） |
