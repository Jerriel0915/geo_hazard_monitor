[根目录](../../CLAUDE.md) > [server](../) > **zwei-iot-report**

# zwei-iot-report — 报告管理 (周/月/季报定时生成 + 渲染)

> 面包屑: [根目录](../../CLAUDE.md) > [server](../) > **zwei-iot-report**

## 模块职责

报告自动生成与管理系统, 负责"何时生成报告"与"报告内容如何渲染":

- **报告生成编排** (`ReportGenerationService`) — 按隐患点串行生成周报/月报/季报, Redis 分布式锁兜底多实例并发, 单 hp 失败不影响其他
- **周期计算** (`ReportPeriod`) — 不可变值对象, 用 `java.time` 计算上一自然周(周一~周日)/上月/上季度, 支持手动补跑历史数据
- **数据聚合** (`ReportDataAssembler`) — 跨 4 个模块 Service 聚合数据: 设备列表/传感器属性/时序聚合(MAX/MIN/AVG/SUM/LAST_VALUE)/完整度/趋势/告警摘要
- **报告渲染** (`ReportRenderer` 策略接口 + 3 个实现) — 将 `ReportContext` 渲染为内联样式 HTML (html2canvas 截图友好), 每周/月/季各有不同报告结构
- **风险评估** (`RiskAssessor`) — 根据告警总数/最高级别/趋势上升指标数/设备在线率计算综合风险评分 (4 级: 低/中/高/极高)
- **定时任务** (`ReportScheduleJob`) — 三入口错峰执行: 每周一 02:00 / 每月 1 号 02:30 / 每季度首月 1 号 03:00, 可通过 `zwei.report.schedule.*` 配置开关

## 关键依赖

- `zwei-common` (事件 + 基础 + Redis 分布式锁)
- `zwei-iot-device` (设备/传感器/告警查询 Service 接口)
- `zwei-iot-timeseries` (IoTDB 聚合/完整度/趋势查询)
- `zwei-iot-hazard` (隐患点查询)
- `zwei-iot-alarm` (告警查询)
- MyBatis + spring-boot-starter-web + spring-boot-starter-data-redis
- lombok + assertj + mockito + spring-boot-starter-test

## 主要子包

| 子包            | 职责                                                                                    |
|---------------|---------------------------------------------------------------------------------------|
| `controller`  | `ReportController` — 报告分页/详情/删除/单点生成/批量生成 REST 端点                                     |
| `service`     | `ReportGenerationService` (生成编排) / `ReportRecordService` (分页 + 幂等)                     |
| `domain`      | `ReportRecord` (实体) / `ReportType` (枚举: WEEKLY=2/MONTHLY=3/QUARTERLY=4)                 |
| `domain.dto`  | `ReportGenerateDTO` / `ReportGenerateAllDTO` / `ReportRecordVO` / `ReportRecordDetailVO` / `ReportRecordPageDTO` |
| `datasource`  | `ReportDataAssembler` (数据聚合) / `ReportContext` (渲染输入 record) / `MetricRow` (指标行 record)    |
| `render`      | `ReportRenderer` (策略接口) / `WeeklyReportRenderer` / `MonthlyReportRenderer` / `QuarterlyReportRenderer` / `ReportHtmlBuilder` (HTML 拼接工具) / `RiskAssessor` (风险评估) |
| `support`     | `ReportPeriod` (周期计算 record)                                                            |
| `job`         | `ReportScheduleJob` (三入口错峰定时任务, `@ConditionalOnProperty` 控制开关)                           |
| `mapper`      | `ReportRecordMapper` (MyBatis Mapper + XML)                                             |
| `config`      | `ReportModuleConfig` — `@MapperScan("com.zwei.iot.report.mapper")`                      |

## 对外接口 (Controller)

| 路径                            | 方法       | 权限                       | 职责                           |
|-------------------------------|----------|--------------------------|------------------------------|
| `/api/v1/report/records/page` | GET      | `report:record:list`     | 报告记录分页查询 (不含 content 大字段)     |
| `/api/v1/report/records/{id}` | GET      | `report:record:query`    | 报告详情 (含 HTML content)         |
| `/api/v1/report/records/{id}` | DELETE   | `report:record:remove`   | 逻辑删除报告记录                     |
| `/api/v1/report/records/generate` | POST | `report:record:generate` | 单隐患点单周期报告生成 (type 2/3/4, 含去重) |
| `/api/v1/report/records/generate-all` | POST | `report:record:generate` | 全量隐患点批量生成 (按参考日期计算上一周期)       |

## 核心实现类索引

### 生成编排 (P0)

| 类                         | 文件                                       | 职责                                                            |
|---------------------------|------------------------------------------|---------------------------------------------------------------|
| `ReportGenerationService` | `service/ReportGenerationService.java`   | 生成编排: Redis 锁 → 串行遍历 hp → 创建 placeholder → 聚合 → 渲染 → 更新状态 |
| `ReportRecordService`     | `service/ReportRecordService.java`       | 分页查询 + 详情 + 删除 + 幂等检查 (`findExisting`)                       |

### 数据聚合 (P0)

| 类                     | 文件                                         | 职责                                    |
|-----------------------|--------------------------------------------|---------------------------------------|
| `ReportDataAssembler` | `datasource/ReportDataAssembler.java`      | 跨模块聚合: 设备→传感器→时序(5聚合函数)→完整度→趋势→告警    |
| `ReportContext`       | `datasource/ReportContext.java`            | 渲染输入 record: type/period/hp/devices/metrics/告警/趋势 |

### 渲染层 (P0)

| 类                           | 文件                                        | 职责                           |
|-----------------------------|-------------------------------------------|------------------------------|
| `ReportRenderer`            | `render/ReportRenderer.java`              | 策略接口: `type()` + `render(ReportContext)` |
| `WeeklyReportRenderer`      | `render/WeeklyReportRenderer.java`        | 周报: 设备运行列表 + 监测数据概况 + 完整率 + 建议    |
| `MonthlyReportRenderer`     | `render/MonthlyReportRenderer.java`       | 月报: 设备汇总 + 数据汇总 + 风险情况 + Top 10 事件 |
| `QuarterlyReportRenderer`   | `render/QuarterlyReportRenderer.java`     | 季报: 风险总览 + 趋势分析 + 月度告警 + 风险评估     |
| `ReportHtmlBuilder`         | `render/ReportHtmlBuilder.java`           | HTML 拼接工具 (内联样式, PDF 截图友好)        |
| `RiskAssessor`              | `render/RiskAssessor.java`                | 6 因子风险评估: 评分 → 低/中/高/极高 4 级       |

### 基础设施

| 类                  | 文件                                         | 职责                             |
|--------------------|--------------------------------------------|--------------------------------|
| `ReportPeriod`     | `support/ReportPeriod.java`                | 不可变 record: `lastWeek/lastMonth/lastQuarter/previous` |
| `ReportScheduleJob` | `job/ReportScheduleJob.java`              | 三入口错峰定时任务 + `@ConditionalOnProperty` 开关 |
| `ReportModuleConfig` | `ReportModuleConfig.java`                 | `@MapperScan` MyBatis 配置        |

## 报告类型与生成流程

### 报告类型 (`ReportType`)

| 枚举值      | code | 描述   | 包含内容                                          | 定时触发         |
|----------|------|------|-----------------------------------------------|--------------|
| WEEKLY   | 2    | 周报   | 设备列表 + 数据概况 + 完整率 + 建议 (无告警/趋势)                 | 每周一 02:00    |
| MONTHLY  | 3    | 月报   | 设备汇总 + 数据汇总 + 风险情况(告警摘要+Top10) + 建议             | 每月 1 号 02:30 |
| QUARTERLY | 4    | 季报   | 风险总览 + 趋势分析(端点斜率法, 含 UP/DOWN/STABLE) + 月度告警 + 风险评估 | 每季首月 03:00  |

### 生成流程 (ReportGenerationService.generateOne)

1. **幂等检查**: 查 `report_record` 是否已有同 type+hp+period 的成功记录 (status=2)
2. **创建占位记录**: status=1 (生成中)
3. **数据聚合**: `ReportDataAssembler.build()` 跨 4 模块聚合
4. **渲染**: `ReportRenderer.render(ReportContext)` 生成 HTML
5. **更新状态**: status=2 (已生成, 写入 content) 或 status=3 (生成失败, 写入 errorMsg)

### 批量生成 (generateAll)

1. 获取所有启用监测的隐患点 (`IHazardPointQueryService.listMonitoring()`)
2. Redis 分布式锁 (key: `report:gen:{type}:{periodKey}`, TTL: 30 分钟)
3. 串行遍历, 单 hp 失败不中断
4. `DuplicateKeyException` 自动跳过

## 数据模型

- `report_record` — 报告记录 (id / templateId / type 2/3/4 / periodStart / periodEnd / hazardPointId / hazardPointCode / hazardPointName / reportName / reportDate / content (HTML) / filePath / status 1-生成中 2-已生成 3-生成失败 / errorMsg / delFlag)

## Redis Key 模式

| Key 模式                                  | 用途     | TTL   |
|------------------------------------------|--------|-------|
| `report:gen:{type}:{periodStart}_{periodEnd}` | 分布式锁 | 30 分钟 |

## 测试与质量

- 测试文件: 6 个 (controller/datasource/render/service/support 各 1+)
- 运行: `mvn test -pl zwei-iot-report`

## 相关文件清单

- `pom.xml`
- `src/main/java/com/zwei/iot/report/service/ReportGenerationService.java` (P0)
- `src/main/java/com/zwei/iot/report/service/ReportRecordService.java` (P0)
- `src/main/java/com/zwei/iot/report/datasource/ReportDataAssembler.java` (P0)
- `src/main/java/com/zwei/iot/report/datasource/ReportContext.java` (P0)
- `src/main/java/com/zwei/iot/report/render/WeeklyReportRenderer.java` (P0)
- `src/main/java/com/zwei/iot/report/render/MonthlyReportRenderer.java` (P0)
- `src/main/java/com/zwei/iot/report/render/QuarterlyReportRenderer.java` (P0)
- `src/main/java/com/zwei/iot/report/render/ReportHtmlBuilder.java` (P1)
- `src/main/java/com/zwei/iot/report/render/RiskAssessor.java` (P1)
- `src/main/java/com/zwei/iot/report/support/ReportPeriod.java` (P0)
- `src/main/java/com/zwei/iot/report/job/ReportScheduleJob.java` (P1)
- `src/main/java/com/zwei/iot/report/mapper/ReportRecordMapper.java`
- `src/main/resources/mapper/iot/report/ReportRecordMapper.xml`
