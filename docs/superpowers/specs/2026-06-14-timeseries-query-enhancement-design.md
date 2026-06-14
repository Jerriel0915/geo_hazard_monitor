# zwei-iot-timeseries 查询能力增强设计

> 状态: 已确认 | 日期: 2026-06-14
> 范围: `server/zwei-iot-timeseries/` 模块的 Service + Controller 层查询能力扩展
> 现有入口: 3 个 (`/latest` `/page` `/chart` 基于 hazardPointId)
> 新增入口: 5 个 (基于 sensorCode = deviceId+sensorCode)

## 一、目标

将 `zwei-iot-timeseries` 从"基础时序查询"升级为**通用、可配置、安全防护完整**的后端查询基础设施,支持:

1. 一次性返回某传感器下所有监测指标的数据(支持时间窗口)
2. 返回某传感器检测指标的(小时、日时间窗口)**首末差值** (delta)
3. 给定**数值**范围(非时间),筛选出符合/不符合区间的监测数据
4. **白名单**聚合函数 + **简单表达式**组合(无需 Groovy/无 SQL 注入风险)
5. 时序统计补充指标:百分位 / 标准差 / 极差 / 变异系数 / 完整度 / 端点斜率(变化率)

### 范围界定

**做:**
- 6 个新 Service 方法 + 5 个新 Controller 端点
- 7 个新 domain 类型(1 枚举 + 1 sealed interface + 1 record + 4 VO)
- 单元测试 + Testcontainers 集成测试
- 现有 3 个端点**完全不动**,前端零改动

**不做:**
- 不引入 Groovy 沙箱(由白名单 + 封闭类型保证安全)
- 不改动前端
- 不改动 `MonitorIngest*` 写入路径
- 不引入新的数据库表(完整度/趋势在 IoTDB 上计算或应用层派生)
- 不做缓存层(数据量大、查询灵活,缓存收益有限)

## 二、需求决策记录

| 决策点 | 选项 | 选定 | 理由 |
|---|---|---|---|
| 入口粒度 | sensorCode / hazardPointId / 两层 | **两层** | 保留向后兼容,新场景用更细粒度 |
| 变化值语义 | AVG / delta / 统计 / rate | **delta (LAST-FIRST)** | 地质灾害累计指标核心需求 |
| 数值范围筛选 | 手动 / 判据联动 / 都支持 | **手动阈值** | 简单,iot-alarm 模块独立 |
| 自定义函数边界 | 白名单 / 表达式组合 / Groovy | **白名单 + 表达式组合** | 安全与灵活平衡 |
| 补充指标 | 4 个选项可多选 | **全选** | 完整基础设施 |
| 使用场景 | 报表/大屏/后端调用/通用 | **后端通用基础设施** | 通用可配置 |
| 趋势斜率实现 | IoTDB 原生 / 应用层回归 | **端点斜率近似** | IoTDB 1.3/2.0 均无 SLOPE 函数 |
| 趋势粒度 | per hour / per day | **都返回** | 不同场景需要不同单位 |

## 三、模块定位与依赖

### 依赖关系

```
        (controller)            ← /api/v1/monitor-data/sensor/*
                  │
                  ▼
        ┌─────────────────┐
        │ MonitorData    │     (新 — 聚合 + delta)
        │ Aggregation    │
        │ Service        │
        └─────────────────┘
                  │
        ┌─────────────────┐
        │ MonitorData    │     (新 — 完整度 + 趋势)
        │ Analysis       │
        │ Service        │
        └─────────────────┘
                  │
                  ▼
        ┌─────────────────┐
        │ IotdbTime      │     (增强 — 新增 6 方法)
        │ SeriesService  │
        └─────────────────┘
                  │
                  ▼
        ┌─────────────────┐
        │ IotdbJdbcClient│     (不动)
        └─────────────────┘
```

| 新增 Service | 依赖 |
|---|---|
| `MonitorDataAggregationService` | `IotdbTimeSeriesService` + `IDeviceSensorService`(查 sensor 元数据) + `DeviceHazardPointMapper` |
| `MonitorDataAnalysisService` | 同上 |
| `MonitorDataSensorController` | 两个新 service |

**新 Service 不依赖** `MonitorDataQueryService` —— 互不耦合,各自单一职责。

## 四、Domain 层(7 个新文件)

### 4.1 `AggregationFunction` 枚举(白名单)

```java
public enum AggregationFunction {
    AVG("AVG"), MAX("MAX"), MIN("MIN"), SUM("SUM"), COUNT("COUNT"),
    FIRST_VALUE("FIRST_VALUE"), LAST_VALUE("LAST_VALUE"),
    EXTREME("EXTREME"), STDDEV("STDDEV"),
    P50("QUANTILE", 0.5), P95("QUANTILE", 0.95), P99("QUANTILE", 0.99);

    public String getIotdbExpr(String attrCode) {
        return quartileParam != null
            ? "QUANTILE(" + attrCode + ", " + quartileParam + ")"
            : iotdbFunc + "(" + attrCode + ")";
    }
}
```

**安全保证:** 封闭枚举,SQL 拼接只能从这里取值,无法注入任意 SQL。

### 4.2 `ExpressionSpec` DSL(sealed interface)

```java
public sealed interface ExpressionSpec {
    record FunctionCall(AggregationFunction func) implements ExpressionSpec {}
    record BinaryOp(ExpressionSpec left, BinaryOperator op, ExpressionSpec right) implements ExpressionSpec {}
    record Constant(double value) implements ExpressionSpec {}
    enum BinaryOperator { ADD("+"), SUB("-"), MUL("*"), DIV("/"); /* getter */ }
}
```

**典型组合映射:**

| DSL | IoTDB SQL 片段 | 业务含义 |
|---|---|---|
| `FunctionCall(AVG)` | `AVG(attr)` | 窗口平均 |
| `FunctionCall(MAX)` | `MAX(attr)` | 窗口最大 |
| `FunctionCall(LAST_VALUE)` | `LAST_VALUE(attr)` | 窗口终点值 |
| `BinaryOp(MAX, SUB, MIN)` | `MAX(attr) - MIN(attr)` | 极差 |
| `BinaryOp(LAST_VALUE, SUB, FIRST_VALUE)` | `LAST_VALUE(attr) - FIRST_VALUE(attr)` | **delta** |
| `BinaryOp(BinaryOp(MAX, SUB, MIN), DIV, FunctionCall(AVG))` | `(MAX(attr) - MIN(attr)) / AVG(attr)` | 变异系数 |
| `BinaryOp(FunctionCall(MAX), ADD, Constant(0.5))` | `MAX(attr) + 0.5` | 阈值偏移 |

**安全保证:** sealed interface 子类型在编译期固定,无法运行时注入新节点;`BinaryOperator` 是封闭枚举。

### 4.3 `TimeWindowSpec` record

```java
public record TimeWindowSpec(Long startTime, Long endTime, WindowGranularity granularity) {
    public enum WindowGranularity {
        RAW(null), HOUR("1h"), DAY("1d"), CUSTOM("?");
        public String toGroupByInterval(Long customMillis) { /* null for RAW, "1h" for HOUR, "PT" + ... */ }
    }
}
```

### 4.4 4 个 VO

```java
public record AggregationResultVO(
    Long deviceId, String sensorCode, String attrCode, String attrName, String unit,
    long time,                                       // 该分组的时间戳(RAW 时为 0)
    Map<String, Double> metrics                      // 表达式别名 → 值
) {}

public record SensorAggregationVO(
    Long deviceId, String sensorCode, String sensorName,
    List<AggregationResultVO> results
) {}

public record CompletenessReportVO(
    Long deviceId, String sensorCode, String attrCode,
    long expectedPoints, long actualPoints,
    double completenessRate, double missingRate,
    Long lastReportAt
) {}

public record TrendReportVO(
    Long deviceId, String sensorCode, String attrCode,
    long startTime, long endTime,
    Double slopePerMs,         // (LAST - FIRST) / (endTime - startTime)
    Double ratePerHour,        // slopePerMs * 3_600_000
    Double ratePerDay,         // slopePerMs * 86_400_000
    Double firstValue, Double lastValue,
    String trendDirection      // "rising" | "falling" | "stable" | "unknown"
) {}
```

## 五、IotdbTimeSeriesService 增强(IO 层)

### 5.1 6 个新 public 方法

| 方法签名 | 职责 | SQL 核心 |
|---|---|---|
| `List<IotdbQueryRow> queryLatestBySensor(Long deviceId, String sensorCode)` | 传感器下所有 attr 最新值 | 对每个 attrCode 调 `queryLatest`,汇总 |
| `Map<String, List<IotdbQueryRow>> queryRangeBySensor(Long deviceId, String sensorCode, List<String> attrCodes, TimeWindowSpec window, Double minValue, Double maxValue, int limit)` | 区间数据 + 数值范围 | 对每个 attrCode 调 `queryRange` 加 WHERE 数值范围 |
| `List<AggregationResultVO> queryAggregate(Long deviceId, String sensorCode, String attrCode, TimeWindowSpec window, List<ExpressionSpec> expressions, Double minValue, Double maxValue)` | 多表达式聚合(单 attr) | 见 5.2 |
| `List<AggregationResultVO> queryDelta(Long deviceId, String sensorCode, String attrCode, TimeWindowSpec window)` | delta 便捷方法 | 内部 `queryAggregate` 传 `BinaryOp(LAST_VALUE, SUB, FIRST_VALUE)` |
| `CompletenessReportVO queryCompleteness(Long deviceId, String sensorCode, String attrCode, TimeWindowSpec window, Long expectedIntervalMs)` | 完整度 | `SELECT COUNT(attr)`,期望点 = 时长/间隔 |
| `TrendReportVO queryTrend(Long deviceId, String sensorCode, String attrCode, TimeWindowSpec window)` | 端点斜率 | `LAST_VALUE - FIRST_VALUE` 与时长相除 |

### 5.2 `queryAggregate` SQL 构造流程(关键)

```java
public List<AggregationResultVO> queryAggregate(...) {
    // 1. ensureMeasurement
    ensureMeasurement(attrCode, deviceId, sensorCode, "DOUBLE", "GORILLA");

    // 2. 渲染表达式列表
    StringBuilder selectClause = new StringBuilder("SELECT ");
    List<String> aliases = new ArrayList<>();
    for (int i = 0; i < expressions.size(); i++) {
        ExpressionSpec expr = expressions.get(i);
        String sql = renderExpression(expr, attrCode);
        String alias = deriveAlias(expr);            // "AVG" / "MAX-MIN" / "DELTA" / "CONST_0.5"
        selectClause.append(sql).append(" AS `").append(alias).append("`");
        aliases.add(alias);
        if (i < expressions.size() - 1) selectClause.append(", ");
    }

    // 3. FROM
    String sensorPath = pathResolver.buildSensorPath(deviceId, sensorCode);
    selectClause.append(" FROM ").append(sensorPath);

    // 4. WHERE(时间 + 数值)
    List<String> where = new ArrayList<>();
    if (window.startTime() != null) where.add("time >= " + window.startTime());
    if (window.endTime() != null)   where.add("time < " + window.endTime());
    if (minValue != null)           where.add(attrCode + " >= " + minValue);
    if (maxValue != null)           where.add(attrCode + " <= " + maxValue);
    if (!where.isEmpty())           selectClause.append(" WHERE ").append(String.join(" AND ", where));

    // 5. GROUP BY(粒度非 RAW 时)
    if (window.granularity() != RAW) {
        long start = window.startTime() != null ? window.startTime() : 0L;
        long end   = window.endTime()   != null ? window.endTime()   : System.currentTimeMillis();
        selectClause.append(" GROUP BY ([").append(start).append(", ").append(end)
                     .append("), ").append(window.granularity().toGroupByInterval(null)).append(")");
    }

    // 6. ResultSet → List<AggregationResultVO>
    //    每行: time + 多个 metrics(alias → value)
}
```

### 5.3 `renderExpression` 递归渲染

```java
private String renderExpression(ExpressionSpec expr, String attrCode) {
    if (expr instanceof FunctionCall fc) {
        return fc.func().getIotdbExpr(attrCode);
    } else if (expr instanceof Constant c) {
        return String.valueOf(c.value());
    } else if (expr instanceof BinaryOp bo) {
        return "(" + renderExpression(bo.left(), attrCode)
             + " " + bo.op().getSymbol() + " "
             + renderExpression(bo.right(), attrCode) + ")";
    }
    throw new IllegalArgumentException("未知 ExpressionSpec: " + expr.getClass());
}
```

### 5.4 表达式别名生成规则

| 表达式 | 别名 |
|---|---|
| `FunctionCall(AVG)` | `AVG` |
| `BinaryOp(MAX, SUB, MIN)` | `MAX-MIN` |
| `BinaryOp(LAST_VALUE, SUB, FIRST_VALUE)` | `DELTA` |
| `BinaryOp(BinaryOp(MAX, SUB, MIN), DIV, FunctionCall(AVG))` | `(MAX-MIN)/AVG` |
| `Constant(0.5)` | `0.5` |

**别名用于 ResultSet 列名 + 返回 Map 的 key。** 别名计算需安全字符过滤(只允许字母数字 + `-_/()`),长度 ≤ 64。

## 六、Service 层(2 个新 service)

### 6.1 `MonitorDataAggregationService`

```java
@Service
public class MonitorDataAggregationService {
    private final IotdbTimeSeriesService iotdbService;
    private final IDeviceSensorService deviceSensorService;

    /** 单指标聚合 */
    public List<AggregationResultVO> aggregate(
        Long deviceId, String sensorCode, String attrCode,
        TimeWindowSpec window, List<ExpressionSpec> expressions,
        Double minValue, Double maxValue);

    /** 传感器下所有指标聚合(批量) */
    public SensorAggregationVO aggregateAllAttrs(
        Long deviceId, String sensorCode,
        TimeWindowSpec window, List<ExpressionSpec> expressions,
        Double minValue, Double maxValue);

    /** delta 便捷方法(等价于 aggregateAllAttrs 传 LAST_VALUE - FIRST_VALUE) */
    public SensorAggregationVO delta(
        Long deviceId, String sensorCode, TimeWindowSpec window);
}
```

**`aggregateAllAttrs` 实现要点:**
1. 解析 sensor 元数据 → 拿到 `List<SensorAttribute>`(含 attrCode/attrName/unit)
2. 校验 `!attrs.isEmpty()`,否则抛 `ServiceException("该传感器无监测指标")`
3. **串行**遍历 attrs,逐个调 `aggregate` 单指标版本
4. 合并为 `SensorAggregationVO` 返回

**为什么串行不并行:**
- `IotdbJdbcClient.getConnection()` 走单连接池,并发会争抢连接
- 单 sensor 通常 attr 数量 ≤ 10,串行 QPS 仍可接受
- 后续可改 `CompletableFuture` + 独立线程池,本期 YAGNI

### 6.2 `MonitorDataAnalysisService`

```java
@Service
public class MonitorDataAnalysisService {
    private final IotdbTimeSeriesService iotdbService;
    private final IDeviceSensorService deviceSensorService;

    public CompletenessReportVO completeness(
        Long deviceId, String sensorCode, String attrCode,
        TimeWindowSpec window, Long expectedIntervalMs);

    public TrendReportVO trend(
        Long deviceId, String sensorCode, String attrCode,
        TimeWindowSpec window);
}
```

**`completeness` 期望间隔推断逻辑:**
- `expectedIntervalMs` 非空 → 用传入值
- `expectedIntervalMs` 为空 → 查 `device_sensor.expected_interval`(若表里有)
- 仍为空 → 用窗口内 `count * 2`(粗估 50% 完整度对应实际间隔)→ 不行就用 60_000ms(1 分钟兜底)

**`trend` 方向判定:**
- `Math.abs(slopePerMs) < 1e-9` → `"stable"`
- `slopePerMs > 0` → `"rising"`
- `slopePerMs < 0` → `"falling"`
- 数据不足(0 或 1 个点) → `"unknown"`,slope 等数值字段为 null

## 七、Controller 层(5 个新端点)

| Method | Path | 权限 | 说明 |
|---|---|---|---|
| `GET` | `/api/v1/monitor-data/sensor/latest` | `basic:device:query` | 传感器下所有 attr 最新值 |
| `GET` | `/api/v1/monitor-data/sensor/range` | 同上 | 区间数据(可选数值范围) |
| `POST` | `/api/v1/monitor-data/sensor/aggregate` | 同上 | 多表达式聚合(POST 因 body) |
| `GET` | `/api/v1/monitor-data/sensor/completeness` | 同上 | 完整度 |
| `GET` | `/api/v1/monitor-data/sensor/trend` | 同上 | 趋势 |

**为什么 `/aggregate` 用 POST:**
- `expressions` 是结构化对象列表,可能嵌套很深
- GET URL 长度限制通常 2KB,复杂表达式超限
- 表达式是"计算意图"而非"幂等查询",用 POST 语义更合适

**入参时间格式:** 与现有接口一致(走 `DateUtils.parseDate` 返回毫秒)

## 八、错误处理

| 异常 | 触发 | HTTP 响应 |
|---|---|---|
| `ServiceException("传感器不存在: ...")` | `device_sensor` 表无该 (deviceId, sensorCode) | 500 |
| `ServiceException("该传感器无监测指标")` | `attrList` 为空 | 500 |
| `ServiceException("时间窗口无效")` | `startTime >= endTime` | 500 |
| `IllegalArgumentException("表达式列表不能为空")` | 表达式列表空 | 400 |
| `IllegalArgumentException("表达式嵌套过深")` | 嵌套 > 5 层 | 400 |
| `ServiceException("查询 IoTDB 失败")` | IoTDB 不可达 / SQL 语法错 | 500 |

**全局异常处理:** 由 `zwei-framework` 的 `GlobalExceptionHandler` 统一拦截,无需在 service 内捕获。

## 九、测试策略

### 9.1 单元测试(目标 80%+ 覆盖率)

| 测试类 | 用例数 | 覆盖 |
|---|---|---|
| `AggregationFunctionTest` | 12 | 枚举每个值的 `getIotdbExpr` 拼接 |
| `ExpressionSpecRendererTest` | 8 | 单函数 / 二元 / 嵌套 / 常量 / 别名生成 |
| `TimeWindowSpecTest` | 4 | RAW / HOUR / DAY / CUSTOM 粒度 |
| `MonitorDataAggregationServiceTest` | 8 | 正/反路径:空 attrCode / 多 attr / 数值范围 / 表达式空 / sensor 不存在 / delta 便捷 |
| `MonitorDataAnalysisServiceTest` | 6 | 完整度正常/0 数据/异常间隔 / 趋势上升/下降/稳定/单点 |
| `MonitorDataSensorControllerTest` | 10 | 5 端点 × 200/400/500,@PreAuthorize 校验 |

**测试框架:** JUnit 5 + Mockito + AssertJ

### 9.2 集成测试(Testcontainers IoTDB 2.0)

`@Testcontainers` + `iotdb:2.0.2-standalone` 容器,`mvn verify -P integration` 单独跑:

| 用例 | 验证 |
|---|---|
| 实际写入 → 聚合 → 数值正确 | 端到端 |
| 时间窗口边界 `time >= start AND time < end` | 半开区间 |
| `MAX - MIN`、`LAST - FIRST` 计算 | 表达式组合 |
| 完整度 expectedPoints vs actualPoints | 期望值 |
| 趋势:端点斜率 ≈ 实际增量/时长 | 精度 |

### 9.3 边界用例清单

- [ ] 空 attrList 传感器
- [ ] 时间窗口只给 startTime / 只给 endTime
- [ ] 数值范围只给 minValue / 只给 maxValue
- [ ] minValue > maxValue(参数错误)
- [ ] 单 attrCode sensor / 多 attrCode sensor
- [ ] 表达式列表:空 / 1 个 / 5 个
- [ ] 表达式嵌套 3 层: `((MAX - MIN) / AVG) + Constant(1.0)`
- [ ] 完整度:expectedIntervalMs 显式 / 推断 / 全空兜底
- [ ] 趋势:0 数据点 → unknown / 1 数据点 → unknown / 2+ 数据点 → 正常

## 十、安全与性能

### 10.1 SQL 注入防护

| 防护层 | 措施 |
|---|---|
| **白名单** | `AggregationFunction` 枚举,SQL 函数名只能从枚举取 |
| **封闭类型** | `ExpressionSpec` sealed interface,无法运行时注入新节点 |
| **路径安全** | `attrCode` / `sensorCode` 走 `IotdbPathResolver`,不直接拼 SQL 字符串 |
| **数值校验** | `minValue` / `maxValue` 是 `Double`,Java 类型保证是数字 |
| **深度限制** | 表达式嵌套 ≤ 5 层,防 DoS |
| **别名白名单** | 别名字符过滤:仅 `[a-zA-Z0-9_\-()/]`,长度 ≤ 64 |

### 10.2 性能考虑

- **连接池:** 现有 `IotdbJdbcClient` 单连接池,本期不扩展;多查询走串行
- **GROUP BY 粒度:** 客户端传 RAW 时无 GROUP BY,传 HOUR/DAY 时由 IoTDB 端降采样
- **数值范围 WHERE:** 走 IoTDB 索引列,无全表扫描
- **后续优化(本期不做):** 多 attrCode 并行查询 / 结果缓存 / 连接池升级

## 十一、文件清单

### 11.1 新增文件(16 个 .java + 1 文档 = 17 个)

```
server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/
├── domain/
│   ├── AggregationFunction.java          (枚举)
│   ├── ExpressionSpec.java               (sealed interface + 3 子 record + BinaryOperator)
│   ├── TimeWindowSpec.java               (record + 嵌套枚举)
│   ├── AggregationResultVO.java          (record)
│   ├── SensorAggregationVO.java          (record)
│   ├── CompletenessReportVO.java         (record)
│   └── TrendReportVO.java                (record)
├── service/
│   ├── MonitorDataAggregationService.java
│   └── MonitorDataAnalysisService.java
└── controller/
    └── MonitorDataSensorController.java

server/zwei-iot-timeseries/src/test/java/com/zwei/iot/timeseries/
├── domain/
│   ├── AggregationFunctionTest.java
│   ├── ExpressionSpecRendererTest.java
│   └── TimeWindowSpecTest.java
├── service/
│   ├── MonitorDataAggregationServiceTest.java
│   └── MonitorDataAnalysisServiceTest.java
└── controller/
    └── MonitorDataSensorControllerTest.java
```

### 11.2 修改文件(1 个)

```
server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/
└── service/
    └── IotdbTimeSeriesService.java       (新增 6 个 public 方法 + renderExpression 私有)
```

### 11.3 文档同步(4 个)

```
server/zwei-iot-timeseries/CLAUDE.md         (新增"查询能力矩阵"+"ExpressionSpec DSL"小节)
docs/superpowers/specs/2026-06-14-...md     (本文件,已 commit)
.claude/index.json                           (service 索引 +2)
CLAUDE.md                                    (模块索引表 +2)
```

## 十二、不确定性与开放问题

| 问题 | 当前处理 | 后续 |
|---|---|---|
| IoTDB 1.3.4 JDBC 驱动是否完全支持 QUANTILE 语法 | 信任 IoTDB 1.3+ 文档支持 | 实现时写集成测试验证 |
| 表达式别名长度限制是否影响嵌套表达 | 限制 64 字符 + 深度 5 | 集成测试覆盖 |
| 端点斜率 vs 真实最小二乘斜率偏差 | 文档明确"端点近似" | 用户反馈后决定是否引入应用层回归 |
| 并发查询对 IoTDB 单连接池影响 | 串行调用 | 实测后决定是否升级 HikariCP |
| 完整度"期望间隔"自动推断准确性 | 多级 fallback(显式 → device_sensor 表 → 粗估 → 60s 兜底) | 业务上线后用真实数据校准 |

## 十二.补充、推荐实施阶段(可由实现计划细化)

为降低风险,建议**分两阶段实施**,每个阶段独立可验证:

### 阶段 1:基础设施(预计 1 天)
- Domain 7 文件(枚举 / sealed interface / record / 4 VO)
- `IotdbTimeSeriesService` 新增 6 个方法
- `ExpressionSpec` 渲染单元测试
- `AggregationFunction` 单元测试
- **验证:** 通过 JUnit 直接调用 service 方法,确认 SQL 生成正确、ResultSet 解析无误

### 阶段 2:API 层(预计 1 天)
- `MonitorDataAggregationService` + `MonitorDataAnalysisService` 两个新 service
- `MonitorDataSensorController` 5 个新端点
- Service 单测 + Controller 单测
- Testcontainers IoTDB 集成测试
- 文档同步(server/zwei-iot-timeseries/CLAUDE.md、根 CLAUDE.md、.claude/index.json)
- **验证:** 启动完整服务,curl 5 个端点,验证返回结构与异常路径

## 十三、变更记录

| 时间 | 变更 |
|---|---|
| 2026-06-14 | 首次设计 |
