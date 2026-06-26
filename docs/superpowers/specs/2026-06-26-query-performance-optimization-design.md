# 监测数据查询性能优化设计

> 状态: 已确认 | 日期: 2026-06-26
> 范围: `server/zwei-iot-timeseries/` 查询链路 + `web/` 图表/列表/大屏 + Redis 缓存层
> 上游设计: [2026-06-14-timeseries-query-enhancement-design.md](./2026-06-14-timeseries-query-enhancement-design.md)
> 决策来源: 用户确认 — 优化范围=全部(P0-P3)，降采样=自动自适应，Redis 缓存=引入

---

## 一、目标

解决"历史数据量过高时，设备的数据查询十分消耗性能"的系统性问题。将监测数据查询从
**"全量物化进 JVM 堆再返回"** 升级为 **"边界受控、自动降采样、结果可缓存"** 的查询基础设施，
覆盖后端 IoTDB 查询、前端图表/列表/大屏三个层面。

**量化目标:**

| 场景 | 现状 | 目标 |
|---|---|---|
| 1 年 @ 1Hz 图表查询 (≈3150 万点) | OOM / 30s 超时 / 浏览器卡死 | < 1s 返回 ≤2000 降采样点 |
| 查询中心多属性分页 (深翻页) | 每测点 `pageNum*pageSize` 行常驻堆 | 内存 O(pageSize)，深翻页有界 |
| 大屏每分钟全量刷新 | 200 告警 + 200 隐患点全量拉取 | SSE 增量推送 + 统计轻量轮询 |
| 重复 latest/chart 查询 | 每次直查 IoTDB | Redis 命中，TTL 30-60s |

---

## 二、根因诊断（带 file:line 锚点）

### 后端

**P0 — 图表接口无降采样、无行数上限（最致命）**
`MonitorDataQueryService.chart()` (`server/zwei-iot-timeseries/.../MonitorDataQueryService.java:177-184`)
在 `valueType` 缺省时走 `queryRange` 的**原始路径**
(`IotdbTimeSeriesService.java:170-212`)：执行 `SELECT attr, quality FROM path WHERE time>=... AND time<...`，
**无 LIMIT、无采样**，把区间内每一个原始点全量拉回 Java 再全量序列化返回前端。

- 1 年 @ 1Hz ≈ 3150 万点 → ResultSet 全量物化进 `ArrayList` → OOM / 30s 超时。
- 降采样能力**已存在但非默认**：`valueType=hour/24h/72h` 走 IoTDB `GROUP BY`
  (`IotdbTimeSeriesService.java:226-276`)，但前端 `getChartData`
  (`web/src/api/monitorData.ts:78`) 的 `valueType` 可选，告警详情等调用方常不传 → 默认 raw。
- `IotdbProperties` (`config/IotdbProperties.java:41-42`) `fetchSize=500`、`queryTimeoutSeconds=30` 本身合理，
  问题在 SQL 无界。

**P1 — `page()` 多测点深分页内存放大**
`MonitorDataQueryService.java:122-138`：多测点时每个测点取 `pageNum * pageSize` 行入内存再合并排序。
翻到第 1000 页 × pageSize 20 = 每测点 2 万行常驻堆。单测点路径 (`:114`) 用 IoTDB 原生 LIMIT/OFFSET，
本身高效，问题仅在多测点合并分支。

**P1 — `resolveMeasurements` N+1**
`MonitorDataQueryService.java:238`：每个绑定设备一次 `selectSensorListByDeviceId`，无缓存。
`latest`/`page`/`chart` 三个入口都经此，隐患点下设备多时放大明显。

**P2 — 无查询结果缓存**：timeseries 模块全链路无 `@Cacheable` / Redis，最新值、聚合结果每次直查 IoTDB。

### 前端

- **无虚拟滚动**：全站 0 处 `el-table-v2` / `RecycleScroller`，大列表靠 `el-table`+`el-pagination`。
  设备传感器子弹表 (`web/src/views/basic/Device.vue:378`) 全量 `getDeviceSensors` 无分页。
- **Query.vue N+1 请求** (`web/src/views/report/Query.vue:301-305`)：多属性逐个串行请求；
  导出再 pages×attrs 双层循环 (`:365-381`)，上限 20000 行同步拉取。
- **大屏轮询全量拉取**：`Screen.vue:397`、`DisasterScreen.vue:762` `setInterval(loadAll, 60000)` 每分钟
  全量拉 200 告警 + 200 隐患点；`Alarm.vue:693` 5 分钟 mock 轮询。
- **Alarm.vue resize 监听泄漏** (`:781` 模块级 `addEventListener` 未随卸载移除，跨导航累积)。
- **report.ts 仍用 mock** (`getMockChartData:366`)：AnalysisDataGrid/AnalysisCorrelation 走假数据。
- 全站仅 `MonitorDataExplorer.vue:231` 有 >500 点客户端告警，其余图表无任何数据量防护。

---

## 三、决策记录

| 决策点 | 选项 | 选定 | 理由 |
|---|---|---|---|
| 优化范围 | 仅后端 / 后端+前端 / 全部 P0-P3 | **全部 P0-P3** | 端到端根治，含清理 mock |
| 降采样策略 | 前端显式传 valueType / 后端自动自适应 / 两者 | **后端自动自适应** | 前端无感，所有调用方受益，不依赖每个调用方正确传参 |
| Redis 缓存 | 引入 / 暂不引入 | **引入** | 项目已用 Redis，latest/聚合结果重复查询收益明确，TTL 30-60s |
| 降采样函数 | AVG / LAST_VALUE / MAX | **AVG** (地质累计指标保趋势) | 兼顾平滑与代表性；保留 max/min/avg 统计列 |
| 多测点分页 | 保留 offset / keyset 游标 / offset+上限守护 | **keyset 游标 + 上限守护** | 游标根治内存放大；上限守护防误用，向后兼容 |
| 缓存粒度 | 全局 / 按隐患点 / 按查询参数 | **按查询参数** | latest 按 hpId，chart 按全参数，measurements 按 hpId+筛选 |

---

## 四、优化清单（P0-P3）

| 级别 | # | 优化项 | 位置 | 收益 |
|---|---|---|---|---|
| P0 | 1 | 图表自动降采样：区间估算点数 > 阈值时自动切 `GROUP BY` 桶 | `MonitorDataQueryService.chart` + `IotdbTimeSeriesService` | 千万级→千级，根治卡死 |
| P0 | 2 | raw 路径 `LIMIT` 兜底 + 超限返回提示 | `IotdbTimeSeriesService.queryRange` | 防御性兜底 |
| P1 | 3 | `page()` 多测点改 keyset 游标分页 + offset 上限守护 | `MonitorDataQueryService.page` + Controller + 前端 | 深分页内存 O(n)→O(pageSize) |
| P1 | 4 | `resolveMeasurements` 加 Redis 缓存 (TTL 5min) | `MonitorDataQueryService` | 消除 N+1 DB |
| P1 | 5 | 前端 `Query.vue` 多属性 `Promise.all` 并行 + 导出流式 | `Query.vue:301,365` | 串行→并行；导出不阻塞 UI |
| P2 | 6 | 大屏轮询改 SSE 增量 + 统计轻量轮询 | `Screen.vue` / `DisasterScreen.vue` | 削减每分钟全量拉取 |
| P2 | 7 | 修复 `Alarm.vue` resize 泄漏 + 大列表 `el-table-v2` 虚拟滚动 | `Alarm.vue:781` / 设备/告警列表 | 消除泄漏 + 长列表渲染 |
| P2 | 8 | Redis 查询结果缓存 (latest 30s / chart 60s) | `MonitorDataQueryService` | 重复查询命中缓存 |
| P3 | 9 | 清理 `report.ts` mock，AnalysisDataGrid/Correlation 接真实聚合 | `report.ts` + 分析页 | 正确性 + 复用 ExpressionSpec DSL |

---

## 五、详细方案

### P0-1 图表自动降采样

**核心逻辑**（新增于 `MonitorDataQueryService.chart`，`IotdbTimeSeriesService` 提供底层支持）：

```
对每个 ResolvedMeasurement:
  if valueType 已是聚合 (hour/24h/72h):           // 用户显式选择，尊重
      走现有 queryRange(..., ValueType) 聚合路径
  else:
      estimatedPoints = (endMillis - startMillis)/1000 * estimateHz   // 默认 1Hz，可配
      if estimatedPoints <= maxChartPoints:       // 默认 2000
          raw 查询 + LIMIT (maxChartPoints*2) 兜底
          sampled = false
      else:
          interval = niceInterval(rangeMs / maxChartPoints)   // 对齐到 1m/5m/10m/30m/1h/6h/1d
          走 GROUP BY ([start,end), interval) + AVG(attr), AVG(quality)
          sampled = true, downsampleInterval = interval
```

**新增配置** (`IotdbProperties` 或新 `MonitorQueryProperties`，前缀 `iot.monitor.query`):

| 配置键 | 默认 | 说明 |
|---|---|---|
| `max-chart-points` | 2000 | 图表最大返回点数阈值 |
| `downsample-estimate-hz` | 1.0 | 点数估算频率（保守值） |
| `raw-limit-cap` | 4000 | raw 路径硬 LIMIT 上限 |
| `downsample-func` | `AVG` | 降采样聚合函数 |

**`niceInterval(ms)` 对齐表**（保证 IoTDB 可识别 + 视觉均匀）：

| 区间 (ms) | interval | 示例 |
|---|---|---|
| < 60s | `1s` | 高频近实时 |
| 60s-600s | `1m` / `5m` | — |
| 600s-1h | `10m` / `30m` | — |
| 1h-1d | `1h` | — |
| 1d-7d | `6h` | — |
| > 7d | `1d` | 长周期 |

**响应增强** — `ChartDataVO` 新增（向后兼容，可选字段）：

```java
public record ChartDataVO(String seriesName, String deviceName, String sensorName,
                          List<String> labels, List<Double> values, String unit, String attrName,
                          Double maxValue, Double minValue, Double avgValue,
                          boolean sampled, String downsampleInterval, long pointCount) {}
```

前端 `ChartData` 接口 (`web/src/api/monitorData.ts:20`) 同步加 `sampled?: boolean` +
`downsampleInterval?: string`，图表组件在 `sampled=true` 时显示"已降采样"提示角标。

**调用方适配**：`AlarmDetailDialog.vue:597`、`H5Disposal.vue` 等已传 startTime/endTime，无需改动即可
自动享受降采样（这是"自动自适应"相对"前端显式传"的核心优势）。

### P0-2 raw 路径 LIMIT 兜底

`IotdbTimeSeriesService.queryRange(deviceId, sensorCode, attrCode, start, end)` (无 ValueType 重载,
`:170-212`) 在 SQL 末尾追加 ` LIMIT {rawLimitCap}`（默认 4000）。即使估算失误，单次查询物化行数有硬上限，
杜绝 OOM。超限时日志 WARN 并在响应 `sampled=false, pointCount=cap` 提示前端"数据量过大，建议缩小范围"。

### P1-3 page() keyset 游标分页

**单测点** (`:114`)：已用 IoTDB 原生 LIMIT/OFFSET，保持不变（IoTDB 服务端分页，高效）。

**多测点** (`:122-138`) 重写为 keyset 游标：

- Controller 新增可选参数 `cursor`（上一页最后一行的时间戳，Long 毫秒）。
- 传 `cursor` 时走游标路径：每个测点 `WHERE time < cursor ORDER BY TIME DESC LIMIT pageSize`，
  合并 pageSize×measurements 行（有界），取前 pageSize 行，新 cursor = 末行时间。内存 O(measurements×pageSize)。
- 传 `pageNum`（旧行为）时保留 offset 路径但加 **上限守护**：`perMeasurementLimit = min(pageNum*pageSize,
  maxMergeRows)`（默认 5000），超出抛 `ServiceException("查询结果过多，请缩小筛选范围或使用游标分页")`。
- 前端 `Query.vue` 切换为游标模式（"下一页/上一页"，隐藏页码跳转）；保留 `pageNum` 供其它调用方兼容。

**新增配置** (`iot.monitor.query`): `max-merge-rows` = 5000。

### P1-4 resolveMeasurements Redis 缓存

`MonitorDataQueryService.resolveMeasurements` (`:228`) 加 `@Cacheable`：

```java
@Cacheable(value = "monitor:measurements",
           key = "#hazardPointId + ':' + (#deviceId?:'') + ':' + (#sensorId?:'') + ':' + (#attrCode?:'')",
           unless = "#result.isEmpty()")
private List<ResolvedMeasurement> resolveMeasurements(...) { ... }
```

- Redis Key: `cache:monitor:measurements::{hpId}:{dev}:{sen}:{attr}` (遵循 `cache:` 前缀约定，
  见 `coding-standards.md` §11.1)。
- TTL: 5 分钟（设备/传感器绑定变更低频；变更时由 device/hazard 模块的 `@CacheEvict` 失效，
  或接受 5min 最终一致）。
- 序列化：FastJSON2（`List<ResolvedMeasurement>` record，需确认 record 可序列化；否则缓存其字段投影）。
- 设备/传感器 CRUD 处补 `@CacheEvict(value="monitor:measurements", allEntries=true)`。

> ⚠️ 注意：`resolveMeasurements` 当前是 `private`。Spring Cache 代理不拦截 `private`/内部自调用。
> 需将其提升为 `public` 并通过注入的自身代理调用，或抽到独立 `MonitorMeasurementResolver` Bean。
> 方案选后者（独立 Bean），符合单一职责且避开自调用陷阱。

### P1-5 前端 Query.vue 并行 + 流式导出

- **多属性并行** (`Query.vue:301-305`)：`for...of await` → `Promise.all(attrCodes.map(...))`，
  合并结果。N 个串行 → N 个并行。
- **导出流式** (`:365-381`)：改用后端新增 `/api/v1/monitor-data/export` 流式端点（SSE 或 chunked
  Transfer-Encoding），前端 `fetch` + ReadableStream 拼接 CSV，避免同步拉 20000 行阻塞 UI。
  导出进度条替换全屏 loading。若后端流式成本高，降级为 Web Worker 内分页拉取（不阻塞主线程）。

### P2-6 大屏轮询改 SSE 增量

`Screen.vue` / `DisasterScreen.vue` 当前 `setInterval(loadAll, 60000)` 全量拉取。

- **告警**：复用已有 `/api/v1/alarm/stream` SSE (`AlarmStreamPublisher`)，大屏订阅增量推送，
  本地维护告警列表（上限 50/200，超限淘汰最旧），替代每分钟 `getRealtimeAlarmPage(50)`。
- **隐患点/统计**：保留轮询但降频至 5 分钟 + 仅刷新 `getDashboardFull()` 聚合统计（已是单端点），
  去除 `getHazardPointPage(200)` 全量拉取（地图点位改首次加载 + 局部更新）。
- `DisasterScreen.vue:441` 的 3D 饼图 `setInterval(3000)` 自动旋转改为 CSS 动画（`@keyframes`），
  消除 JS 定时器开销。

### P2-7 resize 泄漏修复 + 虚拟滚动

- **Alarm.vue resize 泄漏** (`:781`)：将模块级 `window.addEventListener('resize', ...)` 移入
  `onMounted`，`onBeforeUnmount` 配对 `removeEventListener`。
- **虚拟滚动**：设备列表 (`Device.vue`)、实时告警 (`RealtimeAlarm.vue`)、MQTT 消息日志等长列表
  评估切 `el-table-v2`（Element Plus 虚拟化表格）。保留 `el-table` 用于行数 < 100 的场景。
- 设备传感器子弹表 (`Device.vue:378`) 加分页或懒加载（传感器数通常 < 50，可加 100 行上限守护）。

### P2-8 Redis 查询结果缓存

| Key 模式 | 用途 | TTL | 序列化 |
|---|---|---|---|
| `cache:monitor:latest::{hpId}` | 隐患点最新值 | 30s | JSON (List) |
| `cache:monitor:chart::{hpId}:{dev}:{sen}:{attr}:{vt}:{start}:{end}` | 图表结果 | 60s | JSON (List) |
| `cache:monitor:measurements::{hpId}:{dev}:{sen}:{attr}` | 测点解析 | 5min | JSON (List) |

- 用 Spring `@Cacheable`（项目已用于 `cache:hazardPoint`，见 `coding-standards.md` §11.1）。
- `latest` 写入路径（`MonitorIngestConsumerService` 入库成功）可选 `@CacheEvict("monitor:latest", key=hpId)`
  或接受 30s 最终一致（推荐，避免写路径加复杂度）。
- `chart` 缓存 key 含 start/end：固定范围（"最近 7 天"快照）命中率高；相对"now"范围每次 miss，
  但大屏/仪表盘刷新多为固定窗口，收益仍在。
- 序列化器：复杂对象用 `JSON.toJSONString()` / `JSON.parseObject()`（FastJSON2，§11.2）。
- **必须**显式 TTL（§11.3），通过 `@Cacheable` 配合 `RedisCacheConfiguration` TTL 设置或自定义
  `CacheManager` 按 cacheName 设 TTL。

### P3-9 清理 report.ts mock + 接真实聚合

- 删除 `report.ts` 中 `getMockQueryData` (`:306`)、`getMockChartData` (`:366`) 及其调用
  (`getMonitorQueryData:446`、`getChartData:453`、`getGridChartData:463`)。
- `AnalysisDataGrid.vue`（3×3 网格图）：改调 `getSensorAggregate` (`monitorData.ts:135`)，
  用 `ExpressionSpec` DSL（`FunctionCall("AVG")` + `granularity`）按格子批量聚合。
- `AnalysisCorrelation.vue`（多传感器叠加）：`getSensorAggregate` 多次并行 + 前端 overlay。
- `getDeviceOptions` (`report.ts:426`) 去掉硬编码 `pageSize=20`，按需分页；`getHazardPointOptions`
  (`:124`) 的 `pageSize=500` 改懒加载搜索。

---

## 六、文件结构（创建/修改清单）

### 后端 `server/zwei-iot-timeseries/`

| 文件 | 操作 | 职责 |
|---|---|---|
| `config/MonitorQueryProperties.java` | **创建** | 降采样/分页/缓存阈值配置 (`iot.monitor.query.*`) |
| `service/IotdbTimeSeriesService.java` | 修改 `:170-276` | raw 路径加 LIMIT；新增 `queryRangeDownsampled(...)` 按 interval GROUP BY |
| `service/MonitorDataQueryService.java` | 修改 `:91-216` | `chart()` 自动降采样决策；`page()` 多测点 keyset + 上限守护；缓存注解 |
| `service/MonitorMeasurementResolver.java` | **创建** | 从 `MonitorDataQueryService` 抽出 `resolveMeasurements`（public Bean，供 `@Cacheable` 代理） |
| `domain/ChartDataVO.java` | 修改 | 加 `sampled` / `downsampleInterval` / `pointCount` 字段 |
| `domain/ValueType.java` | 修改 | 新增 `AUTO` 枚举值（前端可选传，后端按估算决定） |
| `controller/MonitorDataController.java` | 修改 `:57-96` | `page` 加 `cursor` 可选参；`chart` 响应透传降采样标记 |
| `config/CacheConfig.java`（或现有 Redis 配置类） | 修改 | 按 cacheName 设 TTL (`monitor:latest` 30s / `monitor:chart` 60s / `monitor:measurements` 5min) |
| 测试: `MonitorDataQueryServiceTest` / `IotdbTimeSeriesServiceTest` | **创建** | 降采样决策、LIMIT 兜底、keyset 分页、缓存命中（Testcontainers IoTDB） |

### 前端 `web/`

| 文件 | 操作 | 职责 |
|---|---|---|
| `src/api/monitorData.ts` | 修改 `:20,78` | `ChartData` 加 `sampled?`/`downsampleInterval?`；`getMonitorDataPage` 加 `cursor` 参 |
| `src/views/report/Query.vue` | 修改 `:301,365` | 多属性 `Promise.all`；游标分页 UI；导出流式/Web Worker |
| `src/views/alarm/components/AlarmDetailDialog.vue` | 修改 `:597` | 图表角标显示"已降采样"（读 `sampled` 字段） |
| `src/views/bigscreen/Screen.vue` | 修改 `:397` | 告警改 SSE 订阅；统计降频 5min |
| `src/views/bigscreen/DisasterScreen.vue` | 修改 `:441,762` | 3D 旋转改 CSS 动画；告警改 SSE |
| `src/views/holo-board/Alarm.vue` | 修改 `:693,781` | resize 监听入生命周期；mock 轮询清理 |
| `src/views/basic/Device.vue` | 修改 `:378` | 传感器子弹表加分页/上限；评估 `el-table-v2` |
| `src/views/alarm/RealtimeAlarm.vue` | 修改 | 评估 `el-table-v2` 虚拟滚动 |
| `src/api/report.ts` | 修改 `:306-475` | 删 mock；分析页接真实 `getSensorAggregate` |
| `src/views/report/components/AnalysisDataGrid.vue` | 修改 `:312` | 接 `getSensorAggregate` 批量聚合 |
| `src/views/report/components/AnalysisCorrelation.vue` | 修改 `:321` | 接真实聚合 + overlay |

### 设备/隐患点模块（缓存失效）

| 文件 | 操作 | 职责 |
|---|---|---|
| `zwei-iot-device` 设备/传感器 CRUD Service | 修改 | `@CacheEvict(value="monitor:measurements", allEntries=true)` |
| `zwei-iot-hazard` 绑定关系变更 Service | 修改 | 同上 |

---

## 七、验证策略

### 后端

```bash
cd server
mvn test -pl zwei-iot-timeseries -Dtest=MonitorDataQueryServiceTest     # 降采样/keyset/缓存
mvn test -pl zwei-iot-timeseries -Dtest=IotdbTimeSeriesServiceTest       # LIMIT 兜底/GROUP BY
mvn clean compile                                                        # 全量编译
```

- 降采样测试：构造 (start, end) 使 `estimatedPoints > maxChartPoints`，断言返回点数 ≤ 阈值 + `sampled=true`。
- LIMIT 兜底测试：raw 路径返回行数 ≤ `rawLimitCap`。
- keyset 测试：多测点翻页，cursor 连续性 + 内存有界（Mockito 验证 `queryRangePaged` 调用 limit=pageSize）。
- 缓存测试：二次调用命中（Mock IoTDB 调用次数 = 1）。
- Testcontainers IoTDB 集成测试：写入 10 万点 → 查询 1 年范围 → < 1s + 点数 ≤ 2000。

### 前端

```bash
cd web
npm run build    # vue-tsc 类型检查 + 构建
```

- 手动验证：查询中心选 1 年范围 → 图表秒开 + "已降采样"角标；多属性查询并行（Network 面板并发请求）；
  大屏告警 SSE 实时推送（不再每分钟全量刷新）；`Alarm.vue` 多次进出无 resize 监听堆积（DevTools
  Event Listeners 面板）。

### 性能基线对比

| 指标 | 测量方法 | 现状 | 目标 |
|---|---|---|---|
| 1 年图表 P95 延迟 | Testcontainers 10 万点 + JMeter | 超时 | < 1s |
| JVM 堆峰值（图表查询） | `-XX:+PrintGC` / 堆 dump | OOM 风险 | < 50MB |
| 大屏每分钟请求体积 | 浏览器 Network | ~200 行×N | SSE 增量 < 10KB |
| 重复 latest 命中率 | Redis `INFO stats` | 0% | > 70% (30s TTL) |

---

## 八、风险与回滚

| 风险 | 影响 | 缓解 |
|---|---|---|
| 降采样 AVG 抹平尖峰，告警复盘看不全 | 告警详情误判 | `AlarmDetailDialog` 时间窗固定 3 天，点数 < 阈值走 raw；超大窗可加"查看原始"按钮切 `valueType=current` |
| keyset 分页破坏"跳页"UX | 查询中心用户习惯 | 保留 `pageNum` 兼容路径 + 上限守护；游标模式渐进迁移，旧调用方不受影响 |
| Redis 缓存最终一致 (30s-5min) | 设备绑定变更后短暂旧数据 | latest 30s 可接受；measurements 变更点补 `@CacheEvict` |
| record 序列化 FastJSON2 兼容 | 缓存反序列化失败 | `ResolvedMeasurement` 若是 record 需验证 FastJSON2 支持；不支持则缓存字段投影 DTO |
| `@Cacheable` private 自调用失效 | 缓存不生效 | 抽 `MonitorMeasurementResolver` 独立 Bean（已纳入文件结构） |
| 大屏 SSE 重连风暴 | 网络抖动后洪泛 | 复用已修复的 SSE 重连定时器 + stopped 标志 (`layout/index.vue` 2026-06-25 方案) |

**回滚**: 每项独立 PR，按 P0→P1→P2→P3 顺序合入。任一项出问题可单独 revert，不影响其余。
降采样可通过配置 `max-chart-points=∞`（或大值）等效关闭；缓存可通过 `CacheManager` 临时禁用。

---

## 九、实施顺序（PR 拆分）

1. **PR1 (P0)**: 后端图表自动降采样 + raw LIMIT 兜底 + ChartDataVO 字段 + 配置。含测试。
2. **PR2 (P1)**: 后端 `page()` keyset + 上限守护 + Controller cursor 参。含测试。
3. **PR3 (P1)**: `MonitorMeasurementResolver` 抽出 + Redis 缓存（measurements/latest/chart）+ CacheConfig TTL。
4. **PR4 (P1)**: 前端 `Query.vue` 并行 + 游标分页 UI + 流式导出。
5. **PR5 (P2)**: 大屏 SSE 增量 + 3D CSS 动画 + resize 泄漏修复。
6. **PR6 (P2)**: 虚拟滚动 (`el-table-v2`) 评估落地 + 传感器子弹表分页。
7. **PR7 (P3)**: 清理 `report.ts` mock + 分析页接真实聚合。

> 注：前端 `ChartData.sampled` 角标可随 PR1 后端字段同步加（最小改动），不单独成 PR。

---

## 十、与既有设计的关系

本设计是 [2026-06-14-timeseries-query-enhancement-design.md](./2026-06-14-timeseries-query-enhancement-design.md)
的**性能向延续**：前者扩展查询**能力**（ExpressionSpec DSL / 完整度 / 趋势），本设计加固查询**性能边界**
（降采样 / 分页 / 缓存）。两者正交，无冲突：DSL 聚合走 `MonitorDataAggregationService`，本设计的降采样
守护 `MonitorDataQueryService.chart` 的 raw/默认路径。P3 清理 mock 时复用前者已建的 `getSensorAggregate`。
