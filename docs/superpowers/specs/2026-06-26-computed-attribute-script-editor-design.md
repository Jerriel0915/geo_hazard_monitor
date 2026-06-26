# 计算属性脚本编辑器优化设计

> **状态**: 已批准 (待实现)
> **日期**: 2026-06-26
> **作者**: Zwei Team
> **关联**: 2026-06-25 computed-attribute-tools-injection 完成（cache/sensor wrapper 注入）

## 背景与动机

2026-06-25 完成的 computed-attribute-tools-injection 让计算属性 Groovy 脚本可以通过 `cache.*` 与 `sensor.*` wrapper 访问 Redis 与 IoTDB。但前端 `CalcScriptEditor.vue` 现状存在三个问题：

1. **使用说明过时**：顶部 `el-alert` 仅列出 `curData`/`prevData`，未包含今天新增的 `cache`/`sensor` 工具
2. **无语法高亮**：使用 `el-input type="textarea"`，无行号、无括号匹配、无 Groovy 关键字着色
3. **无强制测试**：用户可以编辑脚本后直接保存，未运行测试验证，存在保存坏脚本的风险

本设计在 YAGNI 原则下，用最小依赖与改动解决这三个问题。

## 范围

### In Scope

- `web/src/views/basic/components/CalcScriptEditor.vue` 重构
- 引入 CodeMirror 6 + Groovy 语言包
- 内嵌静态 API 文档（侧栏）
- 状态机驱动的"测试通过才能保存"工作流

### Out of Scope

- 其他 4 处脚本编辑入口（综合告警 / 数据解析 / 共享策略 / 告警判据表达式）
- 后端 Groovy AST lint 接口
- IDE 风自动补全（`cache.` 后弹方法列表）
- API 文档动态拉取（侧栏硬编码）
- "重置为模板"按钮的模板更新

## 需求

### 功能性需求

| ID | 需求 | 优先级 |
|---|---|---|
| FR-1 | 替换 textarea 为 CodeMirror 6，启用 Groovy 语法高亮、行号、括号匹配 | P0 |
| FR-2 | 顶部黄条状态提示（未测试/测试失败/测试通过） | P0 |
| FR-3 | 右侧固定 API 文档侧栏，列出 `curData`/`prevData`/`cache`/`sensor` 全部公开方法签名 | P0 |
| FR-4 | 保存按钮状态机：未修改 → 可用；已修改未测试 → 禁用；测试失败 → 禁用；测试通过 → 可用 | P0 |
| FR-5 | 测试通过判定：前端检查后端响应 `success === true`。后端语义已覆盖三个条件（无执行异常 + 返回值非 null + 脚本可编译） | P0 |
| FR-6 | 在线测试面板（curData/prevData JSON 输入）默认折叠，点击展开 | P1 |
| FR-7 | 主题：one-dark（与 mockup 一致），等宽字体 Consolas/Monaco | P1 |

### 非功能性需求

- **包体积**: CodeMirror 6 + lang-groovy + theme-one-dark，生产 build gzip 后预计 ≤ 200 KB
- **零后端改动**: 仅前端重构，不新增 API
- **保留现有交互**: 取消按钮、关闭对话框行为不变
- **可访问性**: 编辑器支持键盘导航（Tab/方向键）

## 详细设计

### 组件结构

`CalcScriptEditor.vue` 拆分为 3 个子组件 + 1 个常量文件：

```
web/src/views/basic/components/
├── CalcScriptEditor.vue       # 主对话框（保留，重构内部）
├── script-editor/
│   ├── CodeMirrorGroovy.vue   # CodeMirror 6 包装（v-model + Groovy 语言）
│   ├── ApiDocsSidebar.vue     # 右侧 API 文档（纯展示，硬编码内容）
│   ├── TestPanel.vue          # 折叠的测试面板（curData/prevData 输入 + 结果）
│   └── script-api-docs.ts     # 静态 API 文档数据（cache/sensor/curData/prevData 方法签名）
```

**为什么拆分**: 单文件 200+ 行的 dialog 难以维护，3 个子组件各自独立可测，状态机集中在主组件。

### 状态机

主组件维护两个 ref：

```typescript
const initialScript = ref('')    // dialog 打开时父组件传入的 script 快照
const localScript = ref('')      // 当前编辑器内容（可能与 initial 不同）
const testedPassed = ref(false)  // 最近一次测试是否通过

// dirty 基于内容 diff（而非事件触发）—— 更符合直觉
const dirty = computed(() => localScript.value !== initialScript.value)

// canSave 派生：未改 OR 已测试通过
const canSave = computed(() => !dirty.value || testedPassed.value)
```

状态转移：

| 当前状态 | 触发动作 | 新状态 | UI 表现 |
|---|---|---|---|
| (初始打开) | - | `local=initial, tested=false` | dirty=false，保存可用 |
| 任意 | 编辑器输入使 `local !== initial` | `dirty=true, tested=false` | 保存禁用，黄条"未测试" |
| `dirty=true` | 点击运行测试 + 后端 success=true | `dirty=true, tested=true` | 保存变绿，绿条"测试通过" |
| `dirty=true` | 点击运行测试 + 后端 success=false | `dirty=true, tested=false` | 保存禁用，红条"测试失败 + 错误详情" |
| `dirty=true, tested=true` | 编辑器再次输入（内容仍 != initial） | `dirty=true, tested=false` | 保存禁用（再次失效） |
| 任意 | 编辑器输入使 `local === initial`（改回原样） | `dirty=false` | 保存可用（视为未修改） |
| 任意 | 点击"重置为模板"，模板内容 != initial | `dirty=true, tested=false` | 保存禁用 |
| dialog 重新打开 | 父组件传入新 script | `local=initial=新值, tested=false` | dirty=false，保存可用 |

**`dirty` 基于"内容是否与 initial 不同"**：用户改回原样自动解除 dirty，符合直觉。`testedPassed` 在编辑器 input 事件中清空（事件驱动，因为重新测试才能确认当前内容可用）。

**初始打开的 `dirty=false`**：脚本是从父组件传入的已保存版本，假定数据库里的脚本已验证过。允许直接保存（无修改场景）。

### UI 布局

```
┌──────────────────────────────────────────────────────────┐
│  计算脚本 - hour_change · hour_change              [×]    │  ← el-dialog header
├──────────────────────────────────────────────────────────┤
│ ⚠️ 未测试 — 修改后必须通过测试才能保存                      │  ← 状态条（黄/红/绿）
├────────────────────────────────┬─────────────────────────┤
│                                │ 📚 API 文档             │
│  1  // 计算属性: hour_change    │                        │
│  2  return curData?.props?..   │ 📦 curData              │
│  3                             │   .props.<code>         │
│                                │   .dataTime             │
│  [Groovy]  Ln 2, Col 32       │                        │
│                                │ 📦 prevData (可空)      │
│                                │                        │
│                                │ 🛠 cache (Redis)        │
│                                │   getInt(key, def)      │
│                                │   getString(key)        │
│                                │   ... 18 more           │
│                                │                        │
│                                │ 📡 sensor (IoTDB)       │
│                                │   query(d, s, t, a)     │
│                                │   → 异常返回 null       │
│                                │                        │
│                                │ 返回值必须是 Number     │
├────────────────────────────────┴─────────────────────────┤
│  ▶ 在线测试                                                │  ← 折叠面板
├──────────────────────────────────────────────────────────┤
│ ✅ 成功 · 耗时 586ms · 返回 25.0                          │  ← 测试结果（条件展示）
├──────────────────────────────────────────────────────────┤
│              [取消]  [重置为模板]  [✓ 保存]               │  ← 底部按钮
└──────────────────────────────────────────────────────────┘
```

**宽度分配**: Dialog 保持 800px（不变）。内边距 16px × 2 = 32px。右栏固定 240px。编辑器宽度 = 800 - 32 - 240 = **528px**（充足）。

### CodeMirror 6 配置

```typescript
import { basicSetup } from 'codemirror'
import { EditorView, keymap } from '@codemirror/view'
import { EditorState } from '@codemirror/state'
import { groovy } from '@codemirror/lang-groovy'
import { oneDark } from '@codemirror/theme-one-dark'

const extensions = [
  basicSetup,            // 行号、括号匹配、自动缩进、搜索
  groovy(),              // Groovy 语法
  oneDark,               // 暗色主题
  EditorView.lineWrapping,  // 长行换行
  EditorView.updateListener.of(v => {
    if (v.docChanged) emit('update', v.state.doc.toString())
  })
]
```

**v-model 绑定**: 子组件 `CodeMirrorGroovy.vue` 实现 `modelValue` + `update:modelValue`，主组件用 `<v-model>` 双向绑定到 `localScript`。

### API 文档数据来源

侧栏内容**硬编码**在 `script-api-docs.ts`：

```typescript
export interface ApiGroup {
  icon: string
  color: string
  name: string
  description?: string
  methods: Array<{ signature: string; note?: string }>
}

export const API_DOCS: ApiGroup[] = [
  {
    icon: '📦', color: '#409eff', name: 'curData',
    methods: [
      { signature: '.props.<attrCode>', note: '当前值' },
      { signature: '.dataTime', note: '数据时间戳 (ms)' },
    ]
  },
  {
    icon: '📦', color: '#409eff', name: 'prevData', description: '可空',
    methods: [
      { signature: '.props.<attrCode>', note: '上一条数据值' },
    ]
  },
  {
    icon: '🛠', color: '#67c23a', name: 'cache', description: 'Redis 二次封装',
    methods: [
      // 读取（14 方法，每种类型带/不带默认值两个重载）
      { signature: 'getInt(key, default?)' },
      { signature: 'getLong(key, default?)' },
      { signature: 'getDouble(key, default?)' },
      { signature: 'getFloat(key, default?)' },
      { signature: 'getBigDecimal(key, default?)' },
      { signature: 'getString(key, default?)' },
      { signature: 'getBoolean(key, default?)' },
      // 写入（2 方法）
      { signature: 'set(key, value)' },
      { signature: 'set(key, value, timeout, unit)' },
      // 管理（5 方法）
      { signature: 'delete(key) → boolean' },
      { signature: 'hasKey(key) → boolean' },
      { signature: 'expire(key, timeout, unit?) → boolean' },
      { signature: 'getExpire(key) → long' },
    ]
  },
  {
    icon: '📡', color: '#67c23a', name: 'sensor', description: 'IoTDB 查询',
    methods: [
      {
        signature: 'query(deviceId, sensorCode, time, attrCode)',
        note: '异常时返回 null，不中断脚本'
      },
    ]
  },
]
```

**为什么硬编码而非 API 拉取**: YAGNI。`ScriptCacheOps`/`ScriptSensorQuery` 方法签名稳定（每年变化 < 2 次），动态拉取需新增后端端点 + 缓存策略，过度工程化。硬编码 + 类型安全足够。

### 测试通过判定

复用现有 `testCalcScript` API。判定逻辑：

```typescript
const isTestPassed = (result: CalcScriptTestResult): boolean => {
  return result.success === true && result.result != null
}
```

后端 `ComputedAttributeTestController.testScript()` 的语义已经覆盖了三个条件：
- 有执行异常 → `success=false`（`__err_<attrCode>` 命中）
- 返回 null → `success=false`（`属性 'XXX' 未返回有效结果`）
- 正常返回值 → `success=true`

前端只需检查 `success === true`。返回值非 null 由后端保证。

### 依赖引入

新增 npm 依赖：

```json
{
  "dependencies": {
    "codemirror": "^6.0.0",
    "@codemirror/lang-groovy": "^6.0.0",
    "@codemirror/theme-one-dark": "^6.0.0"
  }
}
```

**不引入** `vue-codemirror` 封装 —— CodeMirror 6 的 imperative API 简洁，直接包装 50 行 Vue 组件即可，无需第三方封装。

预计包体积增量：≤ 200 KB gzip（与上述 NFR 一致）。

### 现有功能保留

| 现状 | 新版 |
|---|---|
| `el-dialog` 标题 + 关闭按钮 | 不变 |
| 顶部 `el-alert` 显示变量提示 | 删除（被状态条 + 侧栏取代） |
| `el-input type="textarea"` 编辑器 | 替换为 `<CodeMirrorGroovy v-model="localScript">` |
| `el-collapse` 在线测试面板 | 保留，迁移到 `<TestPanel>` 子组件 |
| "取消" / "重置为模板" / "确定" 按钮 | 保留，"确定"改为状态机控制的"保存" |
| `defaultTemplate` 计算属性 | 不变（不更新模板） |
| 测试结果 `el-alert` 显示 | 保留，迁移到 `<TestPanel>` |

### 边界情况

- **打开时脚本为空**: 使用 `defaultTemplate` 填充（现状逻辑保留）
- **打开后立即保存（未修改）**: `dirty=false`，保存可用，直接 emit save
- **保存后再次打开**: 父组件传入新 script，watcher 重置 dirty/tested
- **测试请求失败（网络/超时）**: 视为测试失败，红条显示错误消息
- **重置模板**: 视为修改，dirty=true，tested=false，保存禁用
- **关闭对话框（× 或取消）**: 不弹确认（保留现状），父组件决定后续

## 测试策略

### 单元测试（Vitest + @vue/test-utils）

```
web/src/views/basic/components/script-editor/__tests__/
├── CodeMirrorGroovy.spec.ts    # v-model 双向绑定
├── ApiDocsSidebar.spec.ts      # 静态文档渲染
└── TestPanel.spec.ts           # JSON 输入校验、运行测试点击
```

主组件测试：

```
web/src/views/basic/components/__tests__/
└── CalcScriptEditor.spec.ts
    ├── 状态机: dirty (内容 diff) × testedPassed → canSave
    ├── 初始打开 dirty=false 可保存
    ├── 修改使 local != initial → dirty=true, tested=false 保存禁用
    ├── 改回原样 local == initial → dirty=false 保存恢复可用
    ├── 测试成功 testedPassed=true 保存启用
    ├── 测试失败 testedPassed=false 保存禁用
    ├── 测试通过后再次修改 → testedPassed 重置为 false 保存禁用
    └── 重置模板（模板 != initial）→ dirty=true 保存禁用
```

### 手动验收

1. 在监测类型页面打开计算属性编辑器，看到 CodeMirror + 侧栏 + 状态条
2. 修改脚本，验证保存按钮变灰，黄条提示"未测试"
3. 点击运行测试，验证侧栏文档可见，测试结果显示
4. 测试成功后，保存按钮变绿
5. 测试失败（语法错误），保存按钮保持灰色，红条显示错误
6. 保存后重新打开，能看到保存的脚本

## 风险与缓解

| 风险 | 缓解 |
|---|---|
| CodeMirror 6 与 Vue 3 reactivity 冲突 | 子组件包装 imperative API，update:modelValue 显式触发 |
| lang-groovy 包缺失或损坏 | 先用 `npm view @codemirror/lang-groovy` 验证存在；fallback：用 lang-javascript |
| oneDark 主题与 Element Plus 视觉冲突 | mockup 验证颜色对比度；如不可调，用 CM6 默认浅色主题 |
| 用户关闭对话框未保存 | 保留现状（不确认），如时间允许加 dirty 确认弹窗 |
| 编辑器在 dialog destroy-on-close 后内存泄漏 | onUnmounted 时调用 EditorView.destroy() |

## 实施路径

预计 8-10 个 commit，分 4 个阶段：

1. **基础设施**: 新依赖、`CodeMirrorGroovy.vue` 子组件 + 单测
2. **侧栏 + 文档**: `ApiDocsSidebar.vue` + `script-api-docs.ts` 静态数据
3. **状态机**: 主组件重构，加入 dirty/tested，测试面板迁移
4. **测试 + 验收**: 状态机单测、手动验收、视觉打磨

详细 task 拆分由 writing-plans 阶段生成。

## 不动清单

明确本次**不会改动**的文件：

- `web/src/api/monitorType.ts`（API 客户端不变）
- `web/src/views/basic/MonitorType.vue`（父组件调用方式不变）
- 后端任何代码（含 `ComputedAttributeTestController.java`）
- `application.yml` / `application-local.yml`
- 其他 4 个脚本编辑入口

## 参考

- 现状代码: `web/src/views/basic/components/CalcScriptEditor.vue`
- 后端端点: `POST /api/v1/computed-attributes/test-script` (`ComputedAttributeTestController.java`)
- Wrapper 源头: `ScriptCacheOps.java` (21 方法) / `ScriptSensorQuery.java` (1 方法)
- CodeMirror 6 文档: https://codemirror.net/docs/
