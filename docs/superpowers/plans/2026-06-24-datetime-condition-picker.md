# DateTimeConditionPicker 组件实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 封装 DATETIME 时间条件选择为独立组件，修复 BETWEEN + 相对模式缺少范围输入的 bug。

**架构：** 新建 `DateTimeConditionPicker.vue` 组件，行内显示紧凑输入框（点击弹出 popover 面板），面板内含绝对/相对切换 + 单值/范围输入。Condition 接口扩展 3 个 relMax 字段，AlarmCriteria.vue 的 hydrate/serialize 相应扩展。

**技术栈：** Vue 3 + TypeScript + Element Plus 2.6 (el-popover, el-radio-group, el-date-picker, el-input-number, el-select)

---

## 文件结构

| 文件 | 操作 | 职责 |
|---|---|---|
| `web/src/views/alarm/composables/useIndicatorTree.ts` | 修改 | Condition 接口增加 relMax 字段 |
| `web/src/views/alarm/AlarmCriteria.vue` | 修改 | hydrate/serialize 扩展 thresholdMax 解析 |
| `web/src/views/alarm/components/DateTimeConditionPicker.vue` | 新建 | 时间条件选择组件（popover 面板 + 紧凑显示） |
| `web/src/views/alarm/components/ConditionRow.vue` | 修改 | 替换 DATETIME 内联块为组件调用 + 清理 |

---

### 任务 1：Condition 接口 + serialize/hydrate 扩展（数据层）

**文件：**
- 修改：`web/src/views/alarm/composables/useIndicatorTree.ts`（Condition 接口，约第 15-28 行）
- 修改：`web/src/views/alarm/AlarmCriteria.vue`（hydrateCondition 第 215-232 行 + serializeCondition 第 235-246 行）

- [ ] **步骤 1：扩展 Condition 接口**

在 `useIndicatorTree.ts` 的 Condition 接口中，在 `relUnit` 字段之后、接口闭合 `}` 之前，添加 3 个 relMax 字段：

```ts
export interface Condition {
  subject: string
  subjectType?: 'CONTENT' | 'DEVICE' | 'PACKET'
  valueType?: ValueType
  operator: string
  threshold: number | string | boolean
  thresholdMax?: number | string
  unit?: string
  /** DATETIME 编辑态字段（仅前端用，序列化时合并入 threshold 字符串） */
  thresholdMode?: 'ABSOLUTE' | 'RELATIVE'
  relDirection?: '+' | '-'
  relValue?: number
  relUnit?: 's' | 'm' | 'h' | 'd'
  /** DATETIME 范围终点相对模式字段（仅前端编辑用，序列化时合并入 thresholdMax） */
  relDirectionMax?: '+' | '-'
  relValueMax?: number
  relUnitMax?: 's' | 'm' | 'h' | 'd'
}
```

- [ ] **步骤 2：扩展 hydrateCondition**

在 `AlarmCriteria.vue` 中，将 `hydrateCondition` 函数（第 215-232 行）替换为以下完整实现。新版本同时解析 threshold 和 thresholdMax 的 `"now"` 前缀：

```ts
/** DATETIME 反序列化: 从 threshold/thresholdMax 字符串拆分出 rel*/rel*Max 编辑态字段 */
function hydrateCondition(c: Condition): Condition {
  if (c.valueType !== 'DATETIME' || !c.threshold) return c

  const parseRelative = (t: string) => {
    if (!t || !t.startsWith('now')) return null
    const match = /^now([+-])(\d+)([smhd])$/.exec(t)
    if (match) {
      return {
        direction: match[1] as '+' | '-',
        value: Number(match[2]),
        unit: match[3] as 's' | 'm' | 'h' | 'd',
      }
    }
    return { direction: '-' as const, value: 0, unit: 'h' as const }
  }

  const t = String(c.threshold)
  const tMax = c.thresholdMax != null ? String(c.thresholdMax) : ''
  const relStart = parseRelative(t)
  const relEnd = parseRelative(tMax)

  if (!relStart && !relEnd) {
    return { ...c, thresholdMode: 'ABSOLUTE' }
  }

  return {
    ...c,
    thresholdMode: 'RELATIVE',
    relDirection: relStart?.direction,
    relValue: relStart?.value,
    relUnit: relStart?.unit,
    relDirectionMax: relEnd?.direction,
    relValueMax: relEnd?.value,
    relUnitMax: relEnd?.unit,
  }
}
```

- [ ] **步骤 3：扩展 serializeCondition**

将 `serializeCondition` 函数（第 235-246 行）替换为以下完整实现。新版本同时序列化 rel*/rel*Max 到 threshold/thresholdMax：

```ts
/** DATETIME 序列化: 将 rel*/rel*Max 编辑态字段合并回 threshold/thresholdMax 字符串，剔除编辑态字段 */
function serializeCondition(c: Condition): Condition {
  if (c.valueType === 'DATETIME' && c.thresholdMode === 'RELATIVE') {
    const serRel = (dir: string, n: number, unit: string) => n > 0 ? `now${dir}${n}${unit}` : 'now'
    const {thresholdMode, relDirection, relValue, relUnit, relDirectionMax, relValueMax, relUnitMax, ...rest} = c
    const result: Condition = {
      ...rest,
      threshold: serRel(relDirection || '-', relValue || 0, relUnit || 'h'),
    }
    if (c.operator === 'BETWEEN') {
      result.thresholdMax = serRel(relDirectionMax || '-', relValueMax || 0, relUnitMax || 'h')
    }
    return result
  }
  // ABSOLUTE 或非 DATETIME: 剥离所有编辑态字段
  const {thresholdMode, relDirection, relValue, relUnit, relDirectionMax, relValueMax, relUnitMax, ...rest} = c
  return rest
}
```

- [ ] **步骤 4：类型检查**

运行：`cd web && npx vue-tsc --noEmit 2>&1 | head -20`
预期：无新增类型错误（原有错误如有需记录但不阻塞）

- [ ] **步骤 5：Commit**

```bash
git add web/src/views/alarm/composables/useIndicatorTree.ts web/src/views/alarm/AlarmCriteria.vue
git commit -m "refactor(alarm-web): Condition 接口扩展 relMax + hydrate/serialize 支持范围相对时间"
```

---

### 任务 2：创建 DateTimeConditionPicker.vue 组件

**文件：**
- 创建：`web/src/views/alarm/components/DateTimeConditionPicker.vue`

- [ ] **步骤 1：创建组件文件**

创建 `web/src/views/alarm/components/DateTimeConditionPicker.vue`，完整内容如下：

```vue
<template>
  <el-popover
      trigger="click"
      placement="bottom-start"
      :width="range ? 360 : 260"
      :hide-after="0"
  >
    <template #reference>
      <div class="dt-picker-input" :class="{ 'is-range': range }">
        <span class="dt-picker-text" :class="{ 'is-placeholder': !displayText }">
          {{ displayText || placeholder }}
        </span>
        <el-icon class="dt-picker-icon"><Clock /></el-icon>
      </div>
    </template>

    <div class="dt-panel">
      <!-- 模式切换 -->
      <el-radio-group
          :model-value="mode"
          size="small"
          @update:model-value="onModeChange"
      >
        <el-radio-button value="ABSOLUTE">绝对</el-radio-button>
        <el-radio-button value="RELATIVE">相对</el-radio-button>
      </el-radio-group>

      <!-- 绝对模式 -->
      <template v-if="mode === 'ABSOLUTE'">
        <el-date-picker
            v-if="!range"
            :model-value="(condition.threshold as string) || undefined"
            type="datetime"
            size="small"
            value-format="YYYY-MM-DDTHH:mm:ss"
            placeholder="选择时间"
            style="width: 100%"
            @update:model-value="onAbsoluteSingleChange"
        />
        <el-date-picker
            v-else
            :model-value="absoluteRange"
            type="datetimerange"
            size="small"
            value-format="YYYY-MM-DDTHH:mm:ss"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            style="width: 100%"
            @update:model-value="onAbsoluteRangeChange"
        />
      </template>

      <!-- 相对模式 -->
      <div v-else class="rel-section">
        <div v-if="range" class="rel-label">起点</div>
        <div class="rel-row">
          <span class="rel-now">当前时间</span>
          <el-select
              :model-value="condition.relDirection || '-'"
              size="small"
              style="width: 56px"
              @update:model-value="(v: string) => updateRelField('relDirection', v)"
          >
            <el-option label="-" value="-" />
            <el-option label="+" value="+" />
          </el-select>
          <el-input-number
              :model-value="condition.relValue || 0"
              size="small"
              :min="0"
              :step="1"
              controls-position="right"
              style="width: 100px"
              @update:model-value="(v: number | null) => updateRelField('relValue', v ?? 0)"
          />
          <el-select
              :model-value="condition.relUnit || 'h'"
              size="small"
              style="width: 64px"
              @update:model-value="(v: string) => updateRelField('relUnit', v)"
          >
            <el-option label="秒" value="s" />
            <el-option label="分" value="m" />
            <el-option label="时" value="h" />
            <el-option label="天" value="d" />
          </el-select>
        </div>

        <template v-if="range">
          <span class="rel-tilde">~</span>
          <div class="rel-label">终点</div>
          <div class="rel-row">
            <span class="rel-now">当前时间</span>
            <el-select
                :model-value="condition.relDirectionMax || '-'"
                size="small"
                style="width: 56px"
                @update:model-value="(v: string) => updateRelField('relDirectionMax', v)"
            >
              <el-option label="-" value="-" />
              <el-option label="+" value="+" />
            </el-select>
            <el-input-number
                :model-value="condition.relValueMax || 0"
                size="small"
                :min="0"
                :step="1"
                controls-position="right"
                style="width: 100px"
                @update:model-value="(v: number | null) => updateRelField('relValueMax', v ?? 0)"
            />
            <el-select
                :model-value="condition.relUnitMax || 'h'"
                size="small"
                style="width: 64px"
                @update:model-value="(v: string) => updateRelField('relUnitMax', v)"
            >
              <el-option label="秒" value="s" />
              <el-option label="分" value="m" />
              <el-option label="时" value="h" />
              <el-option label="天" value="d" />
            </el-select>
          </div>
        </template>
      </div>
    </div>
  </el-popover>
</template>

<script setup lang="ts">
import {computed} from 'vue'
import {Clock} from '@element-plus/icons-vue'
import type {Condition} from '../composables/useIndicatorTree'

const props = defineProps<{
  condition: Condition
  range: boolean
}>()

const emit = defineEmits<{
  'update:condition': [c: Condition]
}>()

const mode = computed(() => props.condition.thresholdMode || 'ABSOLUTE')

const placeholder = computed(() => props.range ? '选择时间范围' : '选择时间')

const UNIT_TEXT: Record<string, string> = {s: '秒', m: '分', h: '时', d: '天'}

function formatRelative(dir?: string, val?: number, unit?: string): string {
  if (!val || val === 0) return '当前时间'
  return `当前时间 ${dir || '-'} ${val} ${UNIT_TEXT[unit || 'h'] || '时'}`
}

function formatCompact(dt: string): string {
  if (!dt) return ''
  const d = new Date(dt)
  if (isNaN(d.getTime())) return dt
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  const hh = String(d.getHours()).padStart(2, '0')
  const mi = String(d.getMinutes()).padStart(2, '0')
  return `${mm}-${dd} ${hh}:${mi}`
}

const displayText = computed(() => {
  if (mode.value === 'ABSOLUTE') {
    if (props.range) {
      const s = props.condition.threshold as string
      const e = props.condition.thresholdMax as string
      return s && e ? `${formatCompact(s)} ~ ${formatCompact(e)}` : ''
    }
    const t = props.condition.threshold as string
    return t ? formatCompact(t) : ''
  }
  // RELATIVE
  if (props.range) {
    const s = formatRelative(props.condition.relDirection, props.condition.relValue, props.condition.relUnit)
    const e = formatRelative(props.condition.relDirectionMax, props.condition.relValueMax, props.condition.relUnitMax)
    return `${s} ~ ${e}`
  }
  return formatRelative(props.condition.relDirection, props.condition.relValue, props.condition.relUnit)
})

const absoluteRange = computed<[string, string] | null>(() => {
  const a = props.condition.threshold as string
  const b = props.condition.thresholdMax as string
  return a && b ? [a, b] : null
})

function serializeRelative(dir: string, val: number, unit: string): string {
  return val > 0 ? `now${dir}${val}${unit}` : 'now'
}

function onModeChange(m: string) {
  const updated: Condition = {...props.condition, thresholdMode: m as 'ABSOLUTE' | 'RELATIVE'}
  if (m === 'ABSOLUTE') {
    if (typeof updated.threshold === 'string' && updated.threshold.startsWith('now')) {
      updated.threshold = ''
    }
    if (typeof updated.thresholdMax === 'string' && updated.thresholdMax.startsWith('now')) {
      updated.thresholdMax = ''
    }
  } else {
    if (updated.relDirection === undefined) updated.relDirection = '-'
    if (updated.relValue === undefined) updated.relValue = 0
    if (updated.relUnit === undefined) updated.relUnit = 'h'
    updated.threshold = serializeRelative(updated.relDirection, updated.relValue, updated.relUnit)
    if (props.range) {
      if (updated.relDirectionMax === undefined) updated.relDirectionMax = '-'
      if (updated.relValueMax === undefined) updated.relValueMax = 0
      if (updated.relUnitMax === undefined) updated.relUnitMax = 'h'
      updated.thresholdMax = serializeRelative(updated.relDirectionMax, updated.relValueMax, updated.relUnitMax)
    }
  }
  emit('update:condition', updated)
}

function updateRelField(field: string, value: any) {
  const updated: Condition = {...props.condition, [field]: value}
  if (field === 'relDirection' || field === 'relValue' || field === 'relUnit') {
    const dir = updated.relDirection || '-'
    const n = updated.relValue || 0
    const unit = updated.relUnit || 'h'
    updated.threshold = serializeRelative(dir, n, unit)
  }
  if (field === 'relDirectionMax' || field === 'relValueMax' || field === 'relUnitMax') {
    const dir = updated.relDirectionMax || '-'
    const n = updated.relValueMax || 0
    const unit = updated.relUnitMax || 'h'
    updated.thresholdMax = serializeRelative(dir, n, unit)
  }
  emit('update:condition', updated)
}

function onAbsoluteSingleChange(v: string | null) {
  emit('update:condition', {...props.condition, threshold: v ?? ''})
}

function onAbsoluteRangeChange(v: [string, string] | null) {
  emit('update:condition', {
    ...props.condition,
    threshold: v?.[0] ?? '',
    thresholdMax: v?.[1] ?? '',
  })
}
</script>

<style scoped>
.dt-picker-input {
  display: inline-flex;
  align-items: center;
  justify-content: space-between;
  gap: 4px;
  height: 24px;
  padding: 0 8px;
  border: 1px solid var(--el-border-color, #dcdfe6);
  border-radius: 4px;
  cursor: pointer;
  background: var(--el-fill-color-blank, #fff);
  transition: border-color 0.2s;
  box-sizing: border-box;
}
.dt-picker-input:hover {
  border-color: var(--el-color-primary, #409eff);
}
.dt-picker-input.is-range {
  width: 360px;
}
.dt-picker-input:not(.is-range) {
  width: 180px;
}
.dt-picker-text {
  font-size: 12px;
  color: var(--el-text-color-primary, #303133);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.dt-picker-text.is-placeholder {
  color: var(--el-text-color-placeholder, #a8abb2);
}
.dt-picker-icon {
  color: var(--el-text-color-placeholder, #a8abb2);
  font-size: 14px;
  flex-shrink: 0;
}

.dt-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 4px 0;
}
.rel-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.rel-row {
  display: flex;
  align-items: center;
  gap: 6px;
}
.rel-now {
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
}
.rel-label {
  font-size: 12px;
  color: #606266;
  font-weight: 500;
}
.rel-tilde {
  text-align: center;
  font-size: 13px;
  color: #606266;
  margin: -4px 0;
}
</style>
```

- [ ] **步骤 2：类型检查**

运行：`cd web && npx vue-tsc --noEmit 2>&1 | head -20`
预期：DateTimeConditionPicker.vue 无类型错误

- [ ] **步骤 3：Commit**

```bash
git add web/src/views/alarm/components/DateTimeConditionPicker.vue
git commit -m "feat(alarm-web): 新建 DateTimeConditionPicker 组件"
```

---

### 任务 3：ConditionRow.vue 集成

**文件：**
- 修改：`web/src/views/alarm/components/ConditionRow.vue`

- [ ] **步骤 1：替换 DATETIME 模板块**

将 ConditionRow.vue 中 `<!-- DATETIME -->` 开始到对应 `</template>` 结束的整块（第 63-125 行）替换为：

```html
    <!-- DATETIME -->
    <DateTimeConditionPicker
        v-else-if="valueType === 'DATETIME'"
        :condition="condition"
        :range="condition.operator === 'BETWEEN'"
        @update:condition="(c: Condition) => emit('update:condition', c)"
    />
```

注意：删除的内容包括 mode-field select、absolute datetime picker、datetimerange picker、relative 控件组（"当前时间" label + rel-dir + rel-value + rel-unit selects）。全部由新组件接管。

- [ ] **步骤 2：添加组件 import**

在 `<script setup lang="ts">` 中，`import {Delete}` 行之后添加：

```ts
import DateTimeConditionPicker from './DateTimeConditionPicker.vue'
```

- [ ] **步骤 3：删除不再使用的函数和计算属性**

删除 `datetimeRange` computed（约第 234-238 行）和 `onDatetimeRangeChange` 函数（约第 240-247 行）。这些逻辑已迁移到 DateTimeConditionPicker 内部。

- [ ] **步骤 4：更新 onSubjectChange 清除 relMax 字段**

在 `onSubjectChange` 函数中，`relUnit: undefined,` 之后（约第 268 行），添加 relMax 清理：

```ts
    relDirection: undefined,
    relValue: undefined,
    relUnit: undefined,
    relDirectionMax: undefined,
    relValueMax: undefined,
    relUnitMax: undefined,
```

- [ ] **步骤 5：简化 updateField（删除 DATETIME 死代码分支）**

组件接管后，`updateField` 中 rel*/thresholdMode 的处理分支变为死代码（DATETIME 字段不再通过 updateField 修改）。将整个 `updateField` 函数替换为精简版：

```ts
function updateField(field: string, value: any) {
  const updated: Condition = {...props.condition, [field]: value}
  // 切换 operator 到 BETWEEN 时初始化 thresholdMax
  if (field === 'operator' && value === 'BETWEEN' && updated.thresholdMax === undefined) {
    if (updated.valueType === 'NUMBER') updated.thresholdMax = 0
    else if (updated.valueType === 'DATETIME') {
      updated.thresholdMax = ''
      updated.relDirectionMax = '-'
      updated.relValueMax = 0
      updated.relUnitMax = 'h'
    }
  }
  emit('update:condition', updated)
}
```

删除的内容：relDirection/relValue/relUnit 序列化分支、thresholdMode ABSOLUTE/RELATIVE 切换分支。这些逻辑已迁移到 DateTimeConditionPicker 内部。

- [ ] **步骤 6：清理不再使用的 CSS**

在 `<style scoped>` 中，删除以下不再使用的类（DATETIME 相关字段已移入组件内部）：

```css
.threshold-range-field { width: 320px; }
.mode-field { width: 72px; }
.rel-dir-field { width: 56px; }
.rel-value-field { width: 88px; }
.rel-unit-field { width: 64px; }
.cond-now-label { ... }
```

保留 `.cond-field`、`.subject-field`、`.operator-field`、`.threshold-field`、`.cond-unit`、`.cond-tilde`（NUMBER 仍使用 cond-tilde）。

- [ ] **步骤 7：类型检查**

运行：`cd web && npx vue-tsc --noEmit 2>&1 | head -20`
预期：无类型错误

- [ ] **步骤 8：Commit**

```bash
git add web/src/views/alarm/components/ConditionRow.vue
git commit -m "refactor(alarm-web): ConditionRow 集成 DateTimeConditionPicker + 清理内联 DATETIME 控件"
```

---

### 任务 4：构建验证 + 浏览器测试

**文件：** 无代码变更，仅验证

- [ ] **步骤 1：前端构建**

运行：`cd web && npm run build 2>&1 | tail -5`
预期：构建成功，无 TypeScript 错误

- [ ] **步骤 2：浏览器验证 — 绝对单值**

1. 打开 `http://localhost:5173/alarm/criteria`
2. 选择一个有 DATETIME 指标的判据（如"最后上报时间"）
3. 确认行内显示一个带时钟图标的紧凑输入框（宽度 180px）
4. 点击输入框，确认弹出 popover 面板
5. 默认"绝对"模式，选择一个日期时间
6. 确认输入框回显如 `06-24 10:00`

- [ ] **步骤 3：浏览器验证 — 相对单值**

1. 在 popover 中切换到"相对"
2. 设置 [-][5][时]
3. 确认输入框回显 `当前时间 - 5 时`
4. 关闭 popover，点击保存
5. 刷新页面，确认回显仍为 `当前时间 - 5 时`（hydrate 正常）

- [ ] **步骤 4：浏览器验证 — 绝对范围**

1. 切换操作符为"介于时段"
2. 确认输入框变宽（360px）
3. 点击展开，选择"绝对"模式
4. 选择一个时间范围
5. 确认回显如 `06-24 08:00 ~ 06-24 12:00`

- [ ] **步骤 5：浏览器验证 — 相对范围（核心 bug 修复）**

1. 在 popover 中切换到"相对"
2. 确认面板显示两组相对控件：起点 [-][5][时] ~ 终点 [-][1][时]
3. 设置起点=5时、终点=1时
4. 确认输入框回显 `当前时间 - 5 时 ~ 当前时间 - 1 时`
5. 点击保存
6. 刷新页面，确认回显正确（thresholdMax 也正确 hydrate）
7. 检查 DB 中 `level_config` JSON 的 threshold=`"now-5h"`, thresholdMax=`"now-1h"`

- [ ] **步骤 6：回归验证 — NUMBER 和 BOOLEAN 不受影响**

1. 切换到一个 NUMBER 指标，确认 BETWEEN 仍显示两个 input-number
2. 切换到一个 BOOLEAN 指标，确认仍显示 在线/离线 select

- [ ] **步骤 7：如有修复则 Commit**

如果步骤 1-6 发现问题并修复，提交修复。否则无需额外 commit。
