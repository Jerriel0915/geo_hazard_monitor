# DateTimeConditionPicker 组件设计

## 背景

ConditionRow.vue 中 DATETIME 类型字段的处理存在 bug：当操作符为 BETWEEN（介于时段）且模式为相对时间时，只展示单个相对时间输入，无法选择时间范围。

### 当前问题代码

```
DATETIME 渲染逻辑 (ConditionRow.vue:62-123):
  ├── mode-select (绝对/相对)
  ├── ABSOLUTE + 非 BETWEEN → el-date-picker datetime      ✅
  ├── ABSOLUTE + BETWEEN    → el-date-picker datetimerange  ✅
  └── RELATIVE (无论是否 BETWEEN) → 单组 [方向][值][单位]    ❌ BETWEEN 时缺范围
```

### 当前行内布局问题

DATETIME 行内元素过多，尤其 RELATIVE 模式下：mode-field + "当前时间" label + rel-dir + rel-value + rel-unit = 5 个控件。如果直接加第二组相对控件用于范围，行内将达 9 个控件，过于拥挤。

## 目标

封装一个独立的时间条件选择组件，在行内以紧凑输入框形式展示，点击后弹出 popover 面板进行绝对/相对切换和具体值输入。支持单值和范围两种模式。

## 设计

### 组件：`DateTimeConditionPicker.vue`

**路径：** `web/src/views/alarm/components/DateTimeConditionPicker.vue`

**Props:**

| Prop | 类型 | 说明 |
|---|---|---|
| `condition` | `Condition` | 完整条件对象，组件直接读写其中的时间相关字段 |
| `range` | `boolean` | 是否为范围模式（operator === 'BETWEEN'） |

**Emits:**

| 事件 | 参数 | 说明 |
|---|---|---|
| `update:condition` | `Condition` | 值变更时 emit 完整的更新后 condition |

### 行内紧凑显示

计算属性 `displayText` 根据 `thresholdMode` + `range` 生成：

| 模式 | 单值 (range=false) | 范围 (range=true) |
|---|---|---|
| 绝对 | `2026-06-24 10:00:00` | `06-24 08:00 ~ 06-24 12:00` |
| 相对 | `当前时间 - 5 时` | `当前时间 - 5 时 ~ - 1 时` |

- 相对单值 value=0 时显示 `当前时间`
- 绝对值为空时显示 placeholder `选择时间`
- 显示为可点击的 input 样式 div，右侧带 `Clock` 图标 suffix
- 宽度：单值 `180px` / 范围 `360px`

### Popover 面板

使用 `el-popover` (trigger=click, placement=bottom-start)，内容分两部分：

**顶部：模式切换**

`el-radio-group` (el-radio-button) 切换 `绝对 / 相对`，绑定 `condition.thresholdMode`。

**内容区：** 根据 mode × range 四种组合：

| Mode | range=false | range=true |
|---|---|---|
| ABSOLUTE | `el-date-picker` type=datetime | `el-date-picker` type=datetimerange |
| RELATIVE | `[方向 select][数值 input-number][单位 select]` | 起点 `[方向][值][单位]` ~ 终点 `[方向][值][单位]` |

面板内每次值变更即时 emit `update:condition`。Popover 点击外部自动关闭（无需确认按钮）。

**相对模式控件复用现有定义：**
- 方向 select: `-` / `+`
- 数值 input-number: min=0, step=1
- 单位 select: 秒(s) / 分(m) / 时(h) / 天(d)

### 数据模型扩展

`Condition` 接口 (`useIndicatorTree.ts`) 新增 3 个字段：

```ts
interface Condition {
  // ... 现有字段不变 ...
  /** DATETIME 范围终点相对模式字段（仅前端编辑用，序列化时合并入 thresholdMax） */
  relDirectionMax?: '+' | '-'
  relValueMax?: number
  relUnitMax?: 's' | 'm' | 'h' | 'd'
}
```

对应序列化规则：
- 单值：`threshold = "now-5h"` (从 relDirection/relValue/relUnit)
- 范围：`threshold = "now-5h"`, `thresholdMax = "now-1h"` (各自独立)

### 序列化扩展 (`AlarmCriteria.vue`)

**`hydrateCondition`** (加载时反序列化):

threshold 和 thresholdMax 分别检查 `"now"` 前缀：
- threshold 以 `"now"` 开头 → thresholdMode = RELATIVE，正则解析填充 relDirection/relValue/relUnit
- thresholdMax 以 `"now"` 开头 → 正则解析填充 relDirectionMax/relValueMax/relUnitMax
- threshold不以 `"now"` 开头 → thresholdMode = ABSOLUTE

正则：`/^now([+-])(\d+)([smhd])$/`

**`serializeCondition`** (保存时序列化):

RELATIVE 模式：
- threshold = `relValue > 0 ? "now" + relDirection + relValue + relUnit : "now"`
- 若 range：thresholdMax = `relValueMax > 0 ? "now" + relDirectionMax + relValueMax + relUnitMax : "now"`
- 剥离所有 rel*/rel*Max 和 thresholdMode 编辑态字段

ABSOLUTE 模式：threshold/thresholdMax 保持原值（日期字符串），剥离所有 rel* 字段。

### ConditionRow.vue 集成

**模板变更：** 替换 DATETIME 模板块（当前 62-123 行）为：

```html
<DateTimeConditionPicker
    v-else-if="valueType === 'DATETIME'"
    :condition="condition"
    :range="condition.operator === 'BETWEEN'"
    @update:condition="(c: Condition) => $emit('update:condition', c)"
/>
```

删除内容：mode-field select、datetime picker（单值）、datetimerange picker（范围）、相对模式全部控件（"当前时间" label、rel-dir、rel-value、rel-unit）、onDatetimeRangeChange 函数。

**逻辑变更：** `updateField` 中 BETWEEN 初始化逻辑补充：

```ts
if (field === 'operator' && value === 'BETWEEN' && updated.thresholdMax === undefined) {
    if (updated.valueType === 'NUMBER') updated.thresholdMax = 0
    else if (updated.valueType === 'DATETIME') {
        updated.thresholdMax = ''
        // 相对模式默认值
        updated.relDirectionMax = '-'
        updated.relValueMax = 0
        updated.relUnitMax = 'h'
    }
}
```

`onSubjectChange` 中 DATETIME 默认值也补充 relMax 初始化。

**CSS 变更：** 删除不再使用的 `.mode-field`、`.rel-dir-field`、`.rel-value-field`、`.rel-unit-field`、`.cond-now-label` 样式。

### 受影响文件清单

| 文件 | 变更 |
|---|---|
| `web/src/views/alarm/components/DateTimeConditionPicker.vue` | 新建 |
| `web/src/views/alarm/components/ConditionRow.vue` | 替换 DATETIME 块为组件 + 清理 |
| `web/src/views/alarm/composables/useIndicatorTree.ts` | Condition 接口增加 3 个字段 |
| `web/src/views/alarm/AlarmCriteria.vue` | hydrate/serialize 扩展 relMax |

### 兼容性

- 现有 DB 中 `threshold = "now-5h"` 的单值数据不受影响（hydrate 正常解析）
- 现有 DB 中 `threshold` 为绝对日期字符串的数据不受影响（thresholdMode 默认 ABSOLUTE）
- BETWEEN + 相对模式的新数据（`thresholdMax = "now-1h"`）是新增能力，不影响旧数据
- 后端 `CriteriaEvaluator` 已支持 `now` 表达式解析，thresholdMax 复用同一逻辑
