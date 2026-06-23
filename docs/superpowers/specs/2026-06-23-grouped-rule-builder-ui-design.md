# GroupedRuleBuilder UI 调整与多值类型支持

**日期**: 2026-06-23
**状态**: 设计阶段
**作者**: Claude Opus 4.6（brainstorming）

## 背景与动机

`GroupedRuleBuilder.vue` 是告警判据配置界面的核心组件，让用户在 `AlarmCriteria.vue` 里以「条件组 + 条件行」的形式构建阈值规则。当前实现有 4 个体验问题：

1. **回显半中文半英文**：用户在指标树里选中「雨量计 / 当前值 / 数据载荷信息 / 小时雨量」后，输入框回显的是 `SNS001.current.小时雨量 (mm)` 这种 raw path 与中文混杂的字符串，与下拉里看到的不一致。
2. **指标框宽度固定 160px**：长中文路径被截断。
3. **操作符永远 6 个数值比较符**（GT/GTE/LT/LTE/EQ/NEQ）：对时间、字符串、布尔类型的指标不适用。
4. **阈值永远数值输入框**：无法输入日期、字符串、在线/离线。
5. **DATETIME 阈值只能存绝对时间**：对「设备超时未上报」这种典型场景无法配置——保存绝对时间第二天就失效了，需要相对时间（如「最后上报时间 < 当前时间 - 5h」）。

后端 `CriteriaEvaluator.java` 已实现 BETWEEN（用 `thresholdMax`），但全链路硬编码 `Map<String, Double>` 与 `LevelCondition.threshold: Double`，非数值类型在 `AlarmEvaluationEngine:118` 处被静默丢弃。

## 目标与非目标

### 目标

1. 指标下拉回显与树展示一致，全中文路径
2. 指标框宽度根据回显内容自适应（min/max 约束下）
3. 操作符与阈值输入控件按指标 value type 分化
4. 阈值输入后展示单位（已有，需保证对所有类型正确）
5. 后端 `CriteriaEvaluator` 支持多态比较（NUMBER/DATETIME/STRING/BOOLEAN）
6. `device.onlineStatus` BOOLEAN 与 `device.lastReportTime`/`packet.dataTime` DATETIME 真正参与判据评估
7. DATETIME 阈值支持**相对时间**（如「晚于 当前时间-5h」），用于超时未上报等场景

### 非目标

- ENUM 类型支持（包括 IN/NIN 操作符、enum_values 存储与编辑器）—— 留待后续迭代
- `alarm_strategy` 综合告警（Groovy 脚本）的改动
- 监测内容管理（`MonitorType.vue`）的表单增加 value type 编辑器 —— 复用现有 `indicator_type`
- 大屏/报表/H5 等其他模块的同步改造

## valueType 集合与映射

本轮支持 4 种 value type，**ENUM 推迟**。

### `indicator_type` → `valueType` 映射

利用现有 `monitor_content.indicator_type` 字段（**不新增 DB 列**），代码层定义映射。现有 13 个 indicator_type 分类：

| indicator_type code | 名称 | valueType | 备注 |
|---|---|---|---|
| `wy` | 位移 | NUMBER | unit=mm |
| `wd` | 温度 | NUMBER | unit=℃ |
| `jd` | 角度 | NUMBER | unit=° |
| `yl` | 压力 | NUMBER | unit=MPa |
| `sw` | 水位 | NUMBER | unit=m |
| `jsd` | 加速度 | NUMBER | unit=m/s² |
| `hsl` | 含水率 | NUMBER | unit=% |
| `ljn` | 力矩 | NUMBER | unit=N/m² |
| `zdl` | 震动频率 | NUMBER | unit=Hz |
| `dl` | 电量 | NUMBER | unit=V |
| `dx` | 断线 | BOOLEAN | unit 留空 |
| `sg` | 声光 | STRING | |
| `sp` | 视频 | STRING | |

未知 code 默认按 NUMBER 处理（向后兼容）。

### 维度硬编码（不依赖 indicator_type）

| subject 段 | valueType |
|---|---|
| `*.payload.{attrCode}` | 查 `MonitorContent.indicator_type` 上表映射 |
| `*.device.onlineStatus` | BOOLEAN |
| `*.device.lastReportTime` | DATETIME |
| `*.packet.dataTime` | DATETIME |

## 操作符与控件分化

### 操作符集合（per valueType）

| valueType | Operators | UI 文案 | 备注 |
|---|---|---|---|
| NUMBER | `GT` `GTE` `LT` `LTE` `EQ` `NEQ` `BETWEEN` | > ≥ < ≤ == != 介于 | BETWEEN 已实现，需前端补 UI |
| DATETIME | `GT` `LT` `BETWEEN` | 晚于 早于 介于时段 | 复用 GT/LT 操作符码；threshold 支持**绝对**（ISO-8601）或**相对**（`now±<n><unit>`）；BETWEEN 仅绝对 |
| STRING | `CONTAINS` `EQ` `NEQ` `GT` `LT` | 包含 等于 不等于 大于 小于(ASCII) | GT/LT 走 `String.compareTo` |
| BOOLEAN | `EQ` `NEQ` | 等于 不等于 | threshold 存 `1`/`0`（数字） |

### 相对时间语法

格式：`now` 或 `now{+|-}<n><unit>`，可叠加多个偏移。

| 单位 | 含义 |
|---|---|
| `s` | 秒 |
| `m` | 分钟 |
| `h` | 小时 |
| `d` | 天 |

示例：
- `now` → 当前时刻
- `now-5h` → 当前时刻前 5 小时（用于「晚于 5h 前」即最近 5h 内有上报）
- `now-1d` → 24 小时前
- `now+30m` → 30 分钟后
- `now-1d12h` → 1 天 12 小时前

判定规则：threshold 字符串以 `now` 开头 → 相对模式，否则按 ISO-8601 绝对时间解析。无 `thresholdMode` 字段，靠前缀判定，向后兼容。

操作符 code 后端复用，前端按 valueType 决定 UI 文案。

### 输入控件（per valueType × operator）

| valueType | 单值控件 | BETWEEN 控件 |
|---|---|---|
| NUMBER | `el-input-number` :precision=2 :step=0.1 | 两个 `el-input-number` 之间加「~」 |
| DATETIME（绝对模式） | `el-date-picker` type=datetime | `el-date-picker` type=datetimerange |
| DATETIME（相对模式） | `[当前时间] [+/−] [el-input-number] [el-select 单位 s/m/h/d]` | — （不支持） |
| STRING | `el-input` | — （不支持 BETWEEN） |
| BOOLEAN | `el-select` 在线/离线 / 是/否（label，存 1/0） | — |

DATETIME 控件前置一个「模式」`el-select`：`绝对` / `相对`。选「相对」时阈值输入区切换为「`当前时间 [−] [5] [小时▼]`」组合，序列化成 `now-5h` 字符串存入 threshold。

BOOLEAN 下拉选项根据 subject 派生：
- `*.device.onlineStatus` → 在线 / 离线
- 其他 BOOLEAN（如 `*.payload.{dx类}`）→ 是 / 否

## 数据模型变更

### `Condition` 类型（前端）

```ts
type TimeUnit = 's' | 'm' | 'h' | 'd'

interface Condition {
  subject: string
  subjectType?: 'CONTENT' | 'DEVICE' | 'PACKET'
  valueType?: 'NUMBER' | 'DATETIME' | 'STRING' | 'BOOLEAN'
  operator: string
  threshold: number | string | boolean      // union，按 valueType 解释
  thresholdMax?: number | string             // NUMBER/DATETIME BETWEEN 用
  unit?: string
  // DATETIME 相对模式专用（仅前端编辑态用，序列化时合并入 threshold 字符串）
  thresholdMode?: 'ABSOLUTE' | 'RELATIVE'    // DATETIME 默认 ABSOLUTE
  relDirection?: '+' | '-'                   // 相对方向
  relValue?: number                          // 相对数值
  relUnit?: TimeUnit                         // 相对单位
}
```

注：`thresholdMode`/`relDirection`/`relValue`/`relUnit` 仅是前端编辑态字段，用于把 UI 拆成多个输入控件；**保存到 `level_config` 时合并序列化为 threshold 字符串** `now-5h`，后端不感知这些字段。

### `LevelCondition.java`（后端）

```java
public class LevelCondition {
    private String subject;
    private String subjectType;       // 已存在
    private String valueType;         // 新增：派发用，值域 NUMBER|DATETIME|STRING|BOOLEAN
    private String operator;
    private Object threshold;         // Double → Object
    private Object thresholdMax;      // Double → Object
    // getter/setter 返回 Object
}
```

### `level_config` JSON 形态示例

```json
// NUMBER BETWEEN
{"subject": "current.payload.rainfall_hour", "valueType": "NUMBER",
 "operator": "BETWEEN", "threshold": 5.0, "thresholdMax": 10.0, "unit": "mm"}

// DATETIME GT (晚于绝对时间)
{"subject": "current.device.lastReportTime", "valueType": "DATETIME",
 "operator": "GT", "threshold": "2026-06-23T10:00:00"}

// DATETIME LT (相对：早于 当前时间-5h，即超过 5h 未上报)
{"subject": "current.device.lastReportTime", "valueType": "DATETIME",
 "operator": "LT", "threshold": "now-5h"}

// STRING CONTAINS
{"subject": "current.payload.device_id", "valueType": "STRING",
 "operator": "CONTAINS", "threshold": "sensor_001"}

// BOOLEAN EQ
{"subject": "current.device.onlineStatus", "valueType": "BOOLEAN",
 "operator": "EQ", "threshold": 1}
```

DATETIME 阈值格式：
- 绝对：ISO-8601 `yyyy-MM-dd'T'HH:mm:ss`
- 相对：`now` 或 `now{+|-}<n><unit>`（unit ∈ s/m/h/d，可叠加如 `now-1d12h`），以 `now` 前缀判定

BOOLEAN 存数字 `1`/`0`，兼容 `AlarmEvaluationEngine` 现有 `online = dev.online() ? 1.0 : 0.0` 逻辑。

## 后端改动清单

| 文件 | 改动 |
|---|---|
| `server/zwei-iot-alarm/.../LevelCondition.java` | threshold/thresholdMax: `Double`→`Object`；新增 `valueType` 字段 |
| `server/zwei-iot-alarm/.../AlarmEvaluationEngine.java` | `Map<String, Double>`→`Map<String, Object>`；按 `pv.value()` 实际类型放入；payload 叶子查 MonitorContent.indicator_type → valueType；device/packet 维度硬编码 valueType |
| `server/zwei-iot-alarm/.../CriteriaEvaluator.java` | `evaluateCondition(cond, Object value)` 按 valueType 分派；新增 `CONTAINS`；保留 GT/LT/EQ 兼容旧调用；未知 valueType 默认走 NUMBER；DATETIME 分支支持相对时间解析（`now±<n><unit>`） |
| `server/zwei-iot-alarm/.../RelativeTimeParser.java`（新建） | 解析 `now`、`now-5h`、`now+30m`、`now-1d12h` 等表达式为 `Instant`；不识别时抛 `IllegalArgumentException` |
| `server/zwei-iot-alarm/.../CriteriaEvaluatorTest.java` | 新增 STRING CONTAINS、BOOLEAN EQ、DATETIME GT 用例、DATETIME 相对时间 LT（5h 前 / 1d 前）、相对表达式解析正反用例 |
| `server/zwei-iot-monitor/.../IndicatorTypeMapping.java`（新建） | 静态 Map<code, valueType>，供 AlarmEvaluationEngine 与前端共享语义 |
| 跨模块接口 | `IDeviceQueryService.getBasicInfoById` 已返回 DeviceBasicInfo；评估 engine 用其字段派生 valueType 无需新增接口 |

**AlarmEvaluationEngine 关键代码思路**：

```java
// 构建双 key subjectValues 时，value 用 Object
Map<String, Object> subjectValues = new HashMap<>();

// payload 维度：根据 indicator_type 推断 valueType
for (var pv : current.payloadValues()) {
    String valueType = IndicatorTypeMapping.valueTypeOf(pv.indicatorType());
    Object v = coerce(pv.value(), valueType);
    if (v != null) {
        subjectValues.put(prefix + "current.payload." + pv.identifier(), v);
    }
}

// device 维度：硬编码 valueType
subjectValues.put(prefix + "current.device.onlineStatus", dev.online() ? 1 : 0);
subjectValues.put(prefix + "current.device.lastReportTime", dev.lastReportAt());  // Date or epoch millis

// packet 维度：硬编码 valueType
subjectValues.put(prefix + "current.packet.dataTime", event.getDataTime());
```

**CriteriaEvaluator 关键代码思路**：

```java
boolean evaluateCondition(LevelCondition cond, Object value) {
    if (value == null || cond == null || cond.getOperator() == null) return false;
    String valueType = cond.getValueType() != null ? cond.getValueType() : inferType(value);
    switch (valueType) {
        case "NUMBER":  return compareNumber(cond, value);
        case "DATETIME": return compareDatetime(cond, value);
        case "STRING":  return compareString(cond, value);
        case "BOOLEAN": return compareBoolean(cond, value);
        default:        return compareNumber(cond, value);  // 向后兼容
    }
}

boolean compareString(LevelCondition cond, Object value) {
    if (!(value instanceof String) || !(cond.getThreshold() instanceof String)) return false;
    String s = (String) value, t = (String) cond.getThreshold();
    switch (cond.getOperator().toUpperCase()) {
        case "CONTAINS": return s.contains(t);
        case "EQ":       return s.equals(t);
        case "NEQ":      return !s.equals(t);
        case "GT":       return s.compareTo(t) > 0;
        case "LT":       return s.compareTo(t) < 0;
        default:         return false;
    }
}

boolean compareDatetime(LevelCondition cond, Object value) {
    if (!(value instanceof TemporalAccessor)) return false;
    Instant v = Instant.from((TemporalAccessor) value);
    Instant t = resolveTime(cond.getThreshold());    // 绝对 ISO 或相对 now-Xh
    switch (cond.getOperator().toUpperCase()) {
        case "GT": return v.isAfter(t);
        case "LT": return v.isBefore(t);
        case "BETWEEN": {
            Instant tMax = resolveTime(cond.getThresholdMax());
            return !v.isBefore(t) && !v.isAfter(tMax);
        }
        default: return false;
    }
}

/** 解析 threshold 字符串：以 'now' 开头走相对，否则 ISO-8601 */
Instant resolveTime(Object threshold) {
    if (!(threshold instanceof String s)) return null;
    if (s.startsWith("now")) return RelativeTimeParser.resolve(s);
    return Instant.parse(s);
}
```

**RelativeTimeParser 设计**（新建文件 `server/zwei-iot-alarm/.../RelativeTimeParser.java`）：

```java
/** 解析 "now", "now-5h", "now+30m", "now-1d12h" → Instant
 *  正则: ^now(?:([+-])(\d+)([smhd]))*$
 *  每段独立累加（正→加，负→减）
 */
public final class RelativeTimeParser {
    private static final Pattern SEG = Pattern.compile("([+-])(\\d+)([smhd])");
    private static final Map<Character, ChronoUnit> UNITS = Map.of(
        's', ChronoUnit.SECONDS, 'm', ChronoUnit.MINUTES,
        'h', ChronoUnit.HOURS,   'd', ChronoUnit.DAYS);

    public static Instant resolve(String expr) {
        if (!expr.startsWith("now")) throw new IllegalArgumentException("not a relative expr: " + expr);
        Instant t = Instant.now();
        if (expr.length() == 3) return t;  // "now"
        Matcher m = SEG.matcher(expr.substring(3));
        while (m.find()) {
            long n = Long.parseLong(m.group(2));
            if (m.group(1).equals("-")) n = -n;
            t = t.plus(n, UNITS.get(m.group(3).charAt(0)));
        }
        return t;
    }
}
```

## 前端改动清单

| 文件 | 改动 |
|---|---|
| `web/src/utils/indicatorType.ts`（新建） | 从 `MonitorType.vue:381-395` 抽出 `INDICATOR_TYPE_META`，每项加 `valueType` 字段；提供 `getValueType(code)` helper |
| `web/src/views/basic/MonitorType.vue` | 删除内联 `IndicatorTypeEnum`，改 import 新 util |
| `web/src/api/monitorType.ts` | `MonitorContentItem` 加 `valueType?: string`（可选，用于 echo） |
| `web/src/views/alarm/composables/useIndicatorTree.ts` | ① `IndicatorTreeNode.meta` 加 `valueType`；② `buildDimensionChildren` 给 payload 叶子按 `indicator_type` 查映射注入；device/packet 硬编码注入；③ **重写 `prefixDisplayLabels(nodes, valuePrefix, labelPrefix)`**：递归遇到 disabled 中间节点时把自身 label 累加到 `labelPrefix`；叶子 `displayLabel` 用累加中文路径；调用方传 `'current'`→`'当前值'`、`'prev'`→`'上一值'`、`sensorCode`→`sensorName` |
| `web/src/views/alarm/components/ConditionRow.vue` | ① 按 `meta.valueType` 切换 operators 与 input 组件；② subject 宽度 `computed(() => Math.max(200, Math.min(500, label.length*14+40)))`；③ 单位 span 保留并仅对 NUMBER 显示；④ BOOLEAN/DATETIME 选 operator 后控件切换；⑤ **DATETIME 模式切换**：阈值前加 `el-select` 模式（绝对/相对），相对模式下控件变为 `[+/−][el-input-number][单位 el-select s/m/h/d]`，并在 condition 上维护 `thresholdMode/relDirection/relValue/relUnit` 编辑态字段 |
| `web/src/views/alarm/AlarmCriteria.vue` | `Condition.threshold` 类型签名扩为 union；保存时按 valueType 序列化；**DATETIME 相对模式序列化逻辑**：当 `thresholdMode==='RELATIVE'` 时把 `relDirection+relValue+relUnit` 拼成 `now-5h` 存入 threshold 字符串；回显时反向解析 threshold（前缀 `now` → 相对模式） |

## 兼容性

- 现有 `monitor_content` 行：`indicator_type` 已有值，按映射表派生 valueType，**无需迁移**
- 现有 `alarm_criteria.level_config` JSON：`threshold` 是数字，反序列化为 Object → Double → 走 `compareNumber`，**无破坏**
- 现有 subject 格式 `[sensorCode.] {current|prev} {payload|device|packet} {attrCode}`：不变
- `device.onlineStatus` 当前已是 1.0/0.0，新存储约定 1/0 等价

## 测试策略

### 后端单元测试（扩展 `CriteriaEvaluatorTest`）

新增 case：
- STRING CONTAINS（命中/不命中）
- STRING EQ / GT (ASCII)
- BOOLEAN EQ 1 == 1
- DATETIME GT（晚于基准时间，绝对）
- DATETIME LT（早于 `now-5h`，相对，mock clock）
- DATETIME BETWEEN（绝对范围）
- 未知 operator 安全降级为 false
- value 为 null 时安全降级
- `RelativeTimeParser`：`now` / `now-5h` / `now+30m` / `now-1d12h` / 非法表达式（`now-5x` / `5h` / 空）

### 前端手工验证场景

1. 监测类型模式选择「当前值 / 数据载荷信息 / 小时雨量」→ 回显 `当前值 / 数据载荷信息 / 小时雨量 (mm)`
2. 传感器模式选择「雨量计 SNS001」→「上一值 / 设备基础信息 / 最后上报时间」→ 回显全中文 + DATETIME 控件 + 「晚于」operator
3. 操作符切换为 BETWEEN → 出现两个 el-input-number（NUMBER）或 datetimerange（DATETIME）
4. 选 `device.onlineStatus` → 控件变 el-select 在线/离线
5. 单位 span 在 NUMBER 类型时显示，DATETIME/STRING/BOOLEAN 不显示
6. 长路径（雨量计+当前值+数据载荷+长 attrCode）→ 宽度撑开至最多 500px
7. **DATETIME 相对时间**：模式切「相对」→ 控件变 `[当前时间][−][5][小时▼]`；保存后回看仍为相对模式且参数正确；阈值存为 `now-5h`
8. **DATETIME 绝对/相对切换**不丢数据：从绝对切到相对再切回，原值应保留（或清空提示）

## 风险与开放问题

| 风险 | 缓解 |
|---|---|
| `threshold: Object` 反序列化时 Jackson 行为不确定（如 STRING 的 "5" 会不会变 Double） | 写一个 `LevelConditionDeserializer` 显式按 `valueType` 字段约束类型，或在 `evaluateCondition` 里做 try/catch + 类型转换 |
| 现有 `AlarmEvaluationEngine:118` 只取 `instanceof Number`，删除该限制后非数值进入 subjectValues 可能影响下游日志/调试 | 日志格式按 valueType 输出，添加 valueType 标记 |
| `sg`/`sp` 在 IndicatorTypeEnum 里 `unit='1'`，改成 STRING 后 unit 应留空 | 在 `INDICATOR_TYPE_META` 里覆盖 unit 为 `''`；存量数据不管 |
| 长期看 ENUM 还是要做（用户明确推迟，但未来需要） | 保留 valueType 字段设计，未来加 ENUM 不破坏现有结构 |

## 实现顺序建议

后端先行（数据模型稳定后前端好对接）：

1. `IndicatorTypeMapping` 常量类（后端 + 前端 util）
2. `LevelCondition` 字段改造 + Jackson 兼容
3. `AlarmEvaluationEngine` Map 类型替换 + valueType 注入
4. `RelativeTimeParser` 实现 + 单元测试
5. `CriteriaEvaluator` 多态分派（含 DATETIME 相对） + 测试
6. `useIndicatorTree.ts` valueType 注入 + 中文回显修复
7. `ConditionRow.vue` 控件分化 + 宽度自适应 + DATETIME 模式切换
8. `AlarmCriteria.vue` 序列化适配（含相对时间序列化/反序列化）
9. 端到端手工验证
