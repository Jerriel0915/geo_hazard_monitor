# 报告管理 (Report Management) 设计规格

| 项 | 值 |
|---|---|
| 创建日期 | 2026-06-17 |
| 状态 | 待审查 |
| 关联模块 | `zwei-iot-report` (新建) / `zwei-iot-hazard` / `zwei-iot-alarm` / `zwei-iot-timeseries` / `zwei-iot-device` / `zwei-admin` / `web` / `db` |
| 关联文件 | `db/geo_hazard_monitor_v2.0.sql` / `web/src/views/report/Report.vue` / `web/src/api/report.ts` |

---

## 1. 背景与目标

### 1.1 现状

- **前端**：`web/src/views/report/Report.vue` 已是完整 mock 实现（列表/查看/下载PDF/打印/删除），但仅支持周报/月报两种类型，全部数据由 `web/src/api/report.ts` 的 `generateMockReports()` 在前端生成。
- **数据库**：`report_template` 与 `report_record` 两张表已存在，但 `report_record` 缺 `del_flag`（违反项目逻辑删除规范）、无周期范围字段、无失败原因字段；`template_id NOT NULL` 假设强制模板化生成。
- **后端**：完全没有 Report 相关 Controller/Service/Mapper。
- **基础设施已就绪**：IoTDB 时序查询 (`IotdbTimeSeriesService` 含 `queryLatest`/`queryRange`/`queryAggregate`/`queryTrend`/`queryCompleteness`)、`@Scheduled` + Quartz 定时任务、Redis 分布式锁、前端 `html2canvas + jsPDF`。

### 1.2 目标

- 对处于**监测中** (`hazard_point.status = 1`) 的隐患点，按**周/月/季**三个类型定时生成报告记录（含 HTML 富文本内容）。
- 前端列表只展示**已生成**的报告结果，可浏览 HTML 内容、可下载 PDF（前端实时生成）。
- 三种报告内容差异化设计：
  - **周报**：监测设备 + 数据情况
  - **月报**：设备 + 风险情况
  - **季报**：风险 + 趋势

### 1.3 非目标

- 不做可视化模板编辑器（采用硬编码三种内置渲染器，`report_template` 表保留为未来扩展）。
- 不做报告对比、版本回滚、邮件推送。
- 不实现 IoTDB 历史数据补生成（首次上线只生成未来的报告，手动接口可补救历史）。
- 不重构 `Query.vue` / `Analysis.vue` 的现有 mock。
- 不做后端 PDF 生成（用户已选前端实时生成 PDF）。

---

## 2. 核心决策

| # | 决策点 | 结果 |
|---|---|---|
| 1 | 报告粒度 | 按隐患点生成（每个监测中隐患点每周/月/季一条 report_record） |
| 2 | PDF 生成方式 | 前端实时生成（`html2canvas + jsPDF`，复用现有逻辑）；后端只存 HTML，`file_path` 字段保留为空 |
| 3 | 模板机制 | 硬编码三种内置渲染器（策略模式 + Spring 自动注入），不引入 FreeMarker |
| 4 | 生成时机 | 定时生成不补历史：周一 02:00 / 月初 02:30 / 季初 03:00；提供手动触发接口作为补救 |
| 5 | 模块归属 | **方案 A**：新建独立模块 `zwei-iot-report`（与现有 6 个 IoT 子模块平级） |
| 6 | "风险"数据源 | `alarm_record` 表（按级别统计 + 最高级别 + Top N 事件），`hazard_point` 无等级字段 |
| 7 | "趋势"数据源 | `IotdbTimeSeriesService.queryTrend`（方向 + 斜率） |
| 8 | 并发控制 | Redis 分布式锁（多副本兜底） + DB UNIQUE 约束（并发兜底）双保险 |
| 9 | 失败处理 | 单隐患点失败不影响其他，`status=3 + error_msg` 记录失败原因，不自动重试 |
| 10 | 周期计算 | `java.time.LocalDate` + `DayOfWeek`（禁止 `java.util.Calendar`） |

---

## 3. 架构与组件

### 3.1 模块归属（方案 A）

新增独立 Maven 模块 `zwei-iot-report`，包名 `com.zwei.iot.report`。

```
server/zwei-iot-report/
└── src/main/java/com/zwei/iot/report/
    ├── controller/
    │   └── ReportController.java          # REST: /api/v1/report/records/*
    ├── service/
    │   ├── ReportRecordService.java       # CRUD (list/detail/delete)
    │   ├── ReportGenerationService.java   # 编排: 取数 → 渲染 → 入库
    │   └── ReportScheduleJob.java         # @Scheduled 三入口 (周/月/季)
    ├── render/
    │   ├── ReportRenderer.java            # 接口: render(ctx) → HTML
    │   ├── WeeklyReportRenderer.java
    │   ├── MonthlyReportRenderer.java
    │   └── QuarterlyReportRenderer.java
    ├── datasource/
    │   ├── ReportDataAssembler.java       # 按 hazardPointId + 周期 拉数 → ReportContext
    │   └── ReportContext.java             # DTO (设备/数据/告警/趋势汇总)
    ├── support/
    │   ├── ReportPeriod.java              # 周期计算工具 (lastWeek/lastMonth/lastQuarter)
    │   └── ReportType.java                # enum WEEKLY(2)/MONTHLY(3)/QUARTERLY(4)
    ├── mapper/
    │   └── ReportRecordMapper.java
    └── domain/
        ├── ReportRecord.java
        └── dto/
            ├── ReportRecordPageDTO.java
            ├── ReportRecordVO.java
            ├── ReportRecordDetailVO.java
            └── ReportGenerateDTO.java
```

### 3.2 跨模块依赖

```
zwei-iot-report 依赖:
  ├── zwei-iot-hazard      → IDeviceHazardRelationService.getDevicesByHazardPoint(hpId)  ★需新增
  │                          IHazardPointQueryService.listMonitoring()                   ★需新增
  ├── zwei-iot-device      → IDeviceStatService (已有 16 个统计方法)
  │                          IDeviceSensorQueryService (已有)
  ├── zwei-iot-timeseries  → IotdbTimeSeriesService.queryRange/queryAggregate/queryTrend/queryCompleteness (已有)
  └── zwei-iot-alarm       → IAlarmQueryService.summarizeByHazardPoint / listByHazardPoint  ★需新增
```

**架构准则**：与项目现有约定一致，`report` 是消费方，只通过 Service 接口调用其他模块，**不直接使用 Mapper**；`report` 不向外暴露 Service 接口（无其他模块需要反向调用），不发布事件。

### 3.3 数据流

```
@Scheduled (cron)
   ↓
ReportScheduleJob.weekly/monthly/quarterly()
   ↓
ReportGenerationService.generateAll(type)
   ↓
   ├── 1. Redis 分布式锁 report:gen:{type}:{period} (30 min TTL)
   ├── 2. IHazardPointQueryService.listMonitoring()  → status=1 隐患点
   ├── 3. 串行 (避免对 IoTDB 并发压力) per 隐患点:
   │      ├── 幂等检查 (status=2 已存在 → 跳过)
   │      ├── INSERT 占位 (status=1)
   │      ├── ReportDataAssembler.build(hp, period) → ReportContext
   │      │     ├── 设备/传感器快照   (IDeviceHazardRelationService / IDeviceSensorQueryService)
   │      │     ├── 在线率/完整率     (IDeviceStatService / IotdbTimeSeriesService.queryCompleteness)
   │      │     ├── 数据区间+聚合     (queryRange / queryAggregate)
   │      │     ├── 告警次数/最高级别 (IAlarmQueryService — 月报/季报)
   │      │     └── 趋势              (queryTrend — 季报)
   │      ├── Renderer.render(context) → HTML
   │      └── UPDATE status=2, content=HTML
   └── 4. 单点失败 try-catch 隔离,记录 status=3 + error_msg,继续下一个
```

---

## 4. 数据模型变更

### 4.1 `report_record` 表 — 升级脚本 `db/upgrade/v2.7-report-module.sql`

当前 schema 问题：
1. 缺 `del_flag`（违反项目逻辑删除规范）
2. `template_id NOT NULL`（与硬编码渲染器冲突）
3. 无 `type` 字段（类型需反查模板表，列表筛选低效）
4. 无周期范围列（只有 `report_date` 单点）
5. 无失败原因列

```sql
-- 报告记录表：补类型/周期/逻辑删除/失败原因
ALTER TABLE report_record
    ADD COLUMN type         tinyint     NOT NULL COMMENT '报告类型: 2-周报, 3-月报, 4-季报' AFTER report_name,
    ADD COLUMN period_start date        NOT NULL COMMENT '周期开始日 (含)' AFTER type,
    ADD COLUMN period_end   date        NOT NULL COMMENT '周期结束日 (含)' AFTER period_start,
    ADD COLUMN error_msg    varchar(1000) DEFAULT NULL COMMENT '生成失败原因 (status=3 时填)' AFTER status,
    ADD COLUMN del_flag     tinyint     NOT NULL DEFAULT 0 COMMENT '删除标记: 0-正常, 1-删除' AFTER error_msg,
    MODIFY COLUMN template_id bigint DEFAULT NULL COMMENT '模板ID (内置渲染器填 NULL)',
    ADD KEY idx_report_record_type (type),
    ADD KEY idx_report_record_period (period_start, period_end),
    ADD KEY idx_report_record_del_flag (del_flag);

-- 注释更新
ALTER TABLE report_record
    MODIFY COLUMN status tinyint DEFAULT '1'
    COMMENT '状态: 1-生成中, 2-已生成, 3-生成失败';

-- 防重复生成：每个周期同一隐患点同一类型只允许一条有效记录
ALTER TABLE report_record
    ADD UNIQUE KEY uk_report_record_unique
        (type, hazard_point_id, period_start, period_end, del_flag);
```

### 4.2 最终字段一览

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint PK | |
| template_id | bigint NULL | 内置渲染器固定 NULL |
| template_name | varchar(200) NULL | 内置渲染器填 `weekly`/`monthly`/`quarterly` |
| **type** | tinyint NOT NULL | 2-周报, 3-月报, 4-季报（与 `report_template.type` 枚举一致） |
| **period_start** | date NOT NULL | 周期起 (含) |
| **period_end** | date NOT NULL | 周期止 (含) |
| hazard_point_id / code / name | | 冗余快照 |
| report_name | varchar(200) | 例 `"龙泉寺崩塌隐患点 - 监测周报 (2026-06-08~2026-06-14)"` |
| report_date | datetime | 生成时间 |
| content | longtext | HTML 内容 |
| file_path | varchar(500) NULL | 本次实现留空（前端实时生成 PDF） |
| status | tinyint | 1-生成中, 2-已生成, 3-生成失败 |
| **error_msg** | varchar(1000) NULL | 失败原因 |
| **del_flag** | tinyint DEFAULT 0 | 逻辑删除 |
| create_by/time, update_by/time | | 审计 |

### 4.3 `report_template` 表

保留不动，本次不写入数据（未来扩展用）。

### 4.4 菜单与权限

当前 `sys_menu` 初始 SQL **没有**"报告报表"顶级菜单（前端路由 `/report/report` 已存在但运行时菜单未挂载）。本升级脚本需要先 INSERT 顶级菜单，再挂"报告管理"子菜单与按钮权限。所有 INSERT 使用幂等检查（`NOT EXISTS`），脚本可重复执行：

```sql
-- 1. 顶级菜单 "报告报表" (parent_id=0, menu_type=M) — 若不存在则插入
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '报告报表', 0, 7, 'report', NULL, '', 1, 0, 'M', '0', '0', '', 'documentation',
       'admin', NOW(), '报告报表目录'
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name = '报告报表' AND parent_id = 0 AND menu_type = 'M');

SET @report_parent_id = (SELECT menu_id FROM sys_menu
                         WHERE menu_name = '报告报表' AND parent_id = 0 AND menu_type = 'M' LIMIT 1);

-- 2. 子菜单 "报告管理" (menu_type=C) — 若不存在则插入
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '报告管理', @report_parent_id, 1, 'report', 'report/Report', '', 1, 0, 'C', '0', '0',
       'report:record:list', 'documentation', 'admin', NOW(), '报告管理菜单'
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'report:record:list' AND menu_type = 'C');

SET @report_menu_id = (SELECT menu_id FROM sys_menu WHERE perms = 'report:record:list' AND menu_type = 'C' LIMIT 1);

-- 3. 按钮权限 (menu_type=F) — 若不存在则批量插入
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT * FROM (
  SELECT '报告查询' AS n, @report_menu_id AS pid, 1 AS o, '' AS p, '' AS c, '' AS q, 1 AS f, 0 AS cache,
         'F' AS t, '0' AS v, '0' AS s, 'report:record:query' AS perm, '#' AS icon, 'admin', NOW(), '' AS rem
  UNION ALL SELECT '报告删除', @report_menu_id, 2, '', '', '', 1, 0, 'F', '0', '0', 'report:record:remove', '#', 'admin', NOW(), ''
  UNION ALL SELECT '报告导出', @report_menu_id, 3, '', '', '', 1, 0, 'F', '0', '0', 'report:record:export', '#', 'admin', NOW(), ''
  UNION ALL SELECT '报告生成', @report_menu_id, 4, '', '', '', 1, 0, 'F', '0', '0', 'report:record:generate', '#', 'admin', NOW(), ''
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = tmp.perm AND menu_type = 'F');

-- 4. 给 admin 角色 (role_id=1) 自动授权新菜单
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu
WHERE perms IN ('report:record:list', 'report:record:query', 'report:record:remove',
                'report:record:export', 'report:record:generate')
  AND menu_id NOT IN (SELECT menu_id FROM sys_role_menu WHERE role_id = 1);
```

> **说明**：顶级菜单 order_num=7 排在"告警管理"(3200, order_num=6) 之后；按钮权限 5 个对应 Controller 的 `@PreAuthorize`。脚本可重复执行（幂等）。

---

## 5. 定时任务设计

### 5.1 三个 Cron 入口（参考项目 `ComprehensiveAlarmJob` 风格，不走 Quartz 表）

```java
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "zwei.report.schedule.enabled", havingValue = "true", matchIfMissing = true)
public class ReportScheduleJob {

    private final ReportGenerationService generationService;

    /** 每周一 02:00 生成上一自然周 (周一 00:00 ~ 周日 23:59) */
    @Scheduled(cron = "0 0 2 * * MON")
    public void generateWeekly() { generationService.generateAll(ReportType.WEEKLY); }

    /** 每月 1 号 02:30 生成上一自然月 */
    @Scheduled(cron = "0 30 2 1 * *")
    public void generateMonthly() { generationService.generateAll(ReportType.MONTHLY); }

    /** 每季度首月 1 号 03:00 生成上一自然季度 (Q1=1-3 月, Q2=4-6 月, Q3=7-9 月, Q4=10-12 月) */
    @Scheduled(cron = "0 0 3 1 1,4,7,10")
    public void generateQuarterly() { generationService.generateAll(ReportType.QUARTERLY); }
}
```

**错峰设计**：02:00 / 02:30 / 03:00，避免月初 1 号同时触发月报和季报时撞车（10 月 1 号会触发月报 02:30 和季报 03:00，错开 30 分钟足够）。

### 5.2 周期计算（核心逻辑）

```java
public final class ReportPeriod {
    public static ReportPeriod lastWeek(LocalDate today) {
        LocalDate monday = today.minusWeeks(1).with(DayOfWeek.MONDAY);
        return new ReportPeriod(monday, monday.plusDays(6));
    }

    public static ReportPeriod lastMonth(LocalDate today) {
        LocalDate first = today.minusMonths(1).withDayOfMonth(1);
        return new ReportPeriod(first, first.withDayOfMonth(first.lengthOfMonth()));
    }

    public static ReportPeriod lastQuarter(LocalDate today) {
        LocalDate first = today.minusMonths(3).withDayOfMonth(1).with(IsoFields.DAY_OF_QUARTER, 1);
        LocalDate last = first.plusMonths(2).withDayOfMonth(first.plusMonths(2).lengthOfMonth());
        return new ReportPeriod(first, last);
    }
}
```

**强制约束**：用 `java.time` 三件套，**禁止** `java.util.Calendar`（项目无时区配置，默认 JVM zone）。

### 5.3 `ReportGenerationService.generateAll(type)` 编排

```java
public void generateAll(ReportType type) {
    ReportPeriod period = ReportPeriod.previous(type, LocalDate.now());
    log.info("[report] start {} {}~{}", type, period.start(), period.end());

    String lockKey = "report:gen:" + type + ":" + period.start() + ":" + period.end();
    boolean locked = redisLock.tryLock(lockKey, 30, TimeUnit.MINUTES);
    if (!locked) { log.info("[report] another instance is running, skip"); return; }

    int success = 0, fail = 0;
    try {
        List<HazardPointBrief> hps = hazardQueryService.listMonitoring();
        for (HazardPointBrief hp : hps) {
            try {
                generateOne(type, period, hp);
                success++;
            } catch (DuplicateKeyException e) {
                log.info("[report] skip exists hp={} type={} period={}", hp.id(), type, period);
            } catch (Exception e) {
                fail++;
                log.error("[report] fail hp={} type={} period={} reason={}", hp.id(), type, period, e.getMessage(), e);
                recordFailure(hp, type, period, e);
            }
        }
        log.info("[report] done type={} total={} success={} fail={}", type, hps.size(), success, fail);
    } finally {
        redisLock.unlock(lockKey);
    }
}
```

### 5.4 `generateOne()` 内部步骤

```
1. 幂等检查: SELECT * FROM report_record
             WHERE type=? AND hazard_point_id=? AND period_start=? AND period_end=?
               AND status=2 AND del_flag=0
   存在 → return (跳过)
2. INSERT 占位 (status=1, content=NULL)
3. ReportDataAssembler.build(hp, period) → ReportContext
4. Renderer.render(context) → HTML
5. UPDATE set status=2, content=HTML, report_date=NOW()
```

### 5.5 并发与幂等的两道防线

| 防线 | 实现 | 兜底场景 |
|---|---|---|
| Redis 分布式锁 | `report:gen:{type}:{periodStart}:{periodEnd}` 30 分钟 TTL | 多实例部署同时触发 |
| 数据库 UNIQUE | `(type, hazard_point_id, period_start, period_end, del_flag)` | Job 重试/手动触发并发 |

失败时 `status=3 + error_msg`，**不自动重试**（避免错误放大）。运维通过手动重新生成接口补救。

### 5.6 不补历史

Job 启动时不做任何"扫描历史缺失"逻辑；手动触发接口允许指定任意历史 `period`，作为补救手段。

---

## 6. 报告内容版式

### 6.1 公共报告头（三种报告共用）

```
┌─────────────────────────────────────────────────┐
│ 地质灾害监测{周/月/季}报                          │
│ ─────────────────────────────────────────────── │
│ 报告周期：YYYY-MM-DD ~ YYYY-MM-DD               │
│ 隐患点：{code} {name}                            │
│ 隐患点位置：经度, 纬度                            │
│ 生成时间：YYYY-MM-DD HH:mm:ss                    │
└─────────────────────────────────────────────────┘
```

### 6.2 周报章节（侧重：监测设备 + 数据情况）

| 章节 | 内容 | 数据来源 |
|---|---|---|
| 1. 设备运行列表 | 表格：设备编号 / 名称 / 类型 / 传感器数 / 在线状态 / 最近上报 | `IDeviceHazardRelationService.getDevicesByHazardPoint(hpId)` ★新增 + `device_online_status` |
| 2. 监测数据概况 | 表格：传感器属性 / 单位 / 周最新值 / 周最大 / 周最小 / 周均值 | `IotdbTimeSeriesService.queryAggregate(MAX/MIN/AVG/LAST)` |
| 3. 数据完整率 | 表格：传感器 / 应采条数 / 实采条数 / 完整率% | `IotdbTimeSeriesService.queryCompleteness` |
| 4. 异常数据 | 列表（无则显示"本周无异常数据"）：时间 / 属性 / 实测值 / 阈值 | `queryRange` 取值 + 与 `sensor_attribute.range_min/max` 对比 |
| 5. 分析与建议 | 短文本（基于完整率/最大变化量的模板化建议） | 模板化文案 |

### 6.3 月报章节（侧重：设备 + 风险）

| 章节 | 内容 | 数据来源 |
|---|---|---|
| 1. 设备运行汇总 | 总数 / 在线数 / 在线率 / 离线数 / 维修中数；各监测类型设备数 | `IDeviceStatService` + 按隐患点筛选 |
| 2. 监测数据汇总 | 表格：属性 / 单位 / 月最大 / 月最小 / 月均值 / 月累计变化量 / 与上月对比↑↓ | `queryAggregate` × 本月 + × 上月 |
| 3. 风险情况 | 告警次数按级别（红/橙/黄/蓝）+ 最高级别 + 待处理告警数 + 已销警/误报分布 | `IAlarmQueryService.summarizeByHazardPoint(hpId, start, end)` ★新增 |
| 4. 关键事件 | Top 10 告警事件列表（时间 / 级别 / 触发传感器 / 描述 / 当前处置状态） | `IAlarmQueryService.listByHazardPoint(...)` ★新增 |
| 5. 分析与建议 | 基于告警级别和频次的模板化建议 | 模板化文案 |

### 6.4 季报章节（侧重：风险 + 趋势）

| 章节 | 内容 | 数据来源 |
|---|---|---|
| 1. 季度风险总览 | 季度告警总数 + 各月告警数对比表（M1/M2/M3）+ 最高告警级别 | `IAlarmQueryService` 按月聚合 |
| 2. 趋势分析 | 表格：属性 / 趋势方向(↑上升 ↓下降 →稳定) / 斜率 / 季初值 / 季末值 / 变化幅度% | `IotdbTimeSeriesService.queryTrend` |
| 3. 告警分布 | 双维度表：按月 × 按级别（3×4 矩阵）+ Top 5 高频告警属性 | 同上 |
| 4. 设备运行汇总 | 季度平均在线率 + 离线事件 Top + 维护记录数 | `device_online_event_log` + `device_status_log` |
| 5. 风险评估与建议 | 综合评估：基于趋势方向 + 告警频次 + 设备健康度给出"低/中/高/极高"4 档综合风险评级 + 文字结论 | `RiskAssessor.assess()` 内部规则 |

### 6.5 HTML 风格约定

- 内联样式（PDF 转换友好，html2canvas 截图式渲染）
- 表格统一 `border-collapse:collapse;width:100%;text-align:center;font-size:12px`
- 表头底色 `#f0f5ff`，告警级别用色：红 `#ff4d4f` / 橙 `#fa8c16` / 黄 `#faad14` / 蓝 `#1890ff`
- 不引用外部 CSS / 字体（前端 html2canvas 截图时不加载外部资源）

### 6.6 渲染策略（策略模式）

```java
public interface ReportRenderer {
    ReportType type();
    String render(ReportContext ctx);
}

// Service 中按 type 分发
@Component
@RequiredArgsConstructor
public class ReportGenerationService {
    private final List<ReportRenderer> renderers;  // Spring 自动注入所有实现

    private ReportRenderer findRenderer(ReportType type) {
        return renderers.stream()
            .filter(r -> r.type() == type)
            .findFirst()
            .orElseThrow(() -> new ServiceException("无匹配渲染器: " + type));
    }
}
```

新增报告类型只需新增一个 `@Component` 渲染器类（**策略模式 + 开闭原则**）。

### 6.7 需新增的跨模块 Service 方法

| 接口 | 新增方法 | 实现位置 |
|---|---|---|
| `IDeviceHazardRelationService` | `List<DeviceBrief> getDevicesByHazardPoint(Long hpId)` | `zwei-iot-hazard` |
| `IHazardPointQueryService`（新建接口） | `List<HazardPointBrief> listMonitoring()` (status=1 AND del_flag=0) | `zwei-iot-hazard` |
| `IAlarmQueryService`（新建接口） | `AlarmSummary summarizeByHazardPoint(Long hpId, LocalDateTime start, LocalDateTime end)`<br>`List<AlarmEvent> listByHazardPoint(Long hpId, ..., Pageable)` | `zwei-iot-alarm` |

---

## 7. REST API

### 7.1 端点清单（统一前缀 `/api/v1/report/records`）

| Method | Path | 权限 | 用途 |
|---|---|---|---|
| GET | `/api/v1/report/records/page` | `report:record:list` | 分页查询 |
| GET | `/api/v1/report/records/{id}` | `report:record:query` | 详情（含 HTML content） |
| DELETE | `/api/v1/report/records/{id}` | `report:record:remove` | 逻辑删除 |
| POST | `/api/v1/report/records/generate` | `report:record:generate` | 手动触发生成（运维补救） |

**不提供**：列表页"导出 PDF"端点（前端实时生成）；不提供模板 CRUD（硬编码渲染器）。

### 7.2 列表查询参数

```
GET /api/v1/report/records/page
    ?pageNum=1
    &pageSize=20
    &type=2                  # 2-周报 3-月报 4-季报 (可选)
    &hazardPointId=5         # 按隐患点筛选 (可选)
    &periodStart=2026-05-01  # 周期起 (可选)
    &periodEnd=2026-06-30    # 周期止 (可选)
    &status=2                # 1-生成中 2-已生成 3-失败 (可选)
    &keyword=王家坪          # 模糊匹配 report_name (可选)
```

### 7.3 响应

**列表项**（不含 content 大字段）：
```json
{
  "code": 200, "msg": "success",
  "data": {
    "rows": [
      {
        "id": 1001, "type": 2, "typeDesc": "周报",
        "periodStart": "2026-06-08", "periodEnd": "2026-06-14",
        "hazardPointId": 1, "hazardPointCode": "HP002", "hazardPointName": "龙泉寺崩塌隐患点",
        "reportName": "龙泉寺崩塌隐患点 - 监测周报 (2026-06-08~2026-06-14)",
        "status": 2, "statusDesc": "已生成", "errorMsg": null,
        "createTime": "2026-06-15 02:00:12"
      }
    ],
    "total": 42, "pageNum": 1, "pageSize": 20
  }
}
```

**详情**（含 content）：
```json
{
  "code": 200, "msg": "success",
  "data": {
    "id": 1001, "type": 2, "typeDesc": "周报",
    "periodStart": "2026-06-08", "periodEnd": "2026-06-14",
    "hazardPointId": 1, "hazardPointCode": "HP002", "hazardPointName": "龙泉寺崩塌隐患点",
    "reportName": "...", "status": 2, "statusDesc": "已生成", "errorMsg": null,
    "content": "<h2>地质灾害监测周报</h2><p>...</p>",
    "createTime": "2026-06-15 02:00:12"
  }
}
```

### 7.4 手动触发生成

```
POST /api/v1/report/records/generate
Content-Type: application/json
{
  "type": 2,
  "hazardPointId": 1,
  "periodStart": "2026-05-01",
  "periodEnd": "2026-05-07"
}
```

**响应**：
- 成功：返回新生成的 `reportId`
- 已存在（同 type+hp+period）：`409 Conflict` + 既有 `reportId`
- 校验失败：`400 Bad Request`

**关键约束**（Controller 层校验）：
- `type ∈ {2,3,4}`
- `periodEnd >= periodStart`
- `periodEnd - periodStart ≤ 400 天`
- `hazardPointId` 必须存在且 `del_flag=0`
- 生成逻辑**同步执行**，单次 ≤ 30 秒；超时返回 500 + `status=3`

### 7.5 Controller 风格

遵循项目惯例（`BaseController` + `AjaxResult` + `@PreAuthorize` + `@Log` 审计）：

```java
@RestController
@RequestMapping("/api/v1/report/records")
@RequiredArgsConstructor
public class ReportController extends BaseController {
    private final ReportRecordService recordService;
    private final ReportGenerationService generationService;

    @PreAuthorize("@ss.hasPermi('report:record:list')")
    @GetMapping("/page")
    public AjaxResult page(ReportRecordPageDTO params) { ... }

    @PreAuthorize("@ss.hasPermi('report:record:query')")
    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable Long id) { ... }

    @PreAuthorize("@ss.hasPermi('report:record:remove')")
    @DeleteMapping("/{id}")
    @Log(title = "报告管理", businessType = BusinessType.DELETE)
    public AjaxResult remove(@PathVariable Long id) { ... }

    @PreAuthorize("@ss.hasPermi('report:record:generate')")
    @PostMapping("/generate")
    @Log(title = "报告管理", businessType = BusinessType.INSERT)
    public AjaxResult generate(@Validated @RequestBody ReportGenerateDTO dto) { ... }
}
```

### 7.6 定时任务开关

在 `application.yml` 增加配置开关，**默认开启**：

```yaml
zwei:
  report:
    schedule:
      enabled: true
      weekly-enabled: true
      monthly-enabled: true
      quarterly-enabled: true
```

各 Job 方法用 `@ConditionalOnProperty(name = "zwei.report.schedule.weekly-enabled", havingValue = "true", matchIfMissing = true)` 装配。

---

## 8. 前端改造

### 8.1 改造范围（仅 2 个文件）

| 文件 | 改造类型 |
|---|---|
| `web/src/api/report.ts` | **大改**：删除所有 mock 函数、改类型枚举、对接真实接口 |
| `web/src/views/report/Report.vue` | **中改**：加季报类型、改字段名、API 切换、加手动生成入口 |

`Query.vue` / `Analysis.vue` / `report.ts` 中的 query/analysis mock 部分**不动**。

### 8.2 `api/report.ts` 重写后

```typescript
export type ReportType = 'weekly' | 'monthly' | 'quarterly'   // ★ 新增 quarterly

export interface ReportItem {
  id: number
  type: ReportType
  typeDesc: string
  periodStart: string
  periodEnd: string
  hazardPointId: number
  hazardPointCode: string
  hazardPointName: string
  reportName: string
  status: 1 | 2 | 3
  statusDesc: string
  errorMsg: string | null
  createTime: string
  content?: string                  // 仅详情接口返回
}

export function getReportPage(params: ReportPageParams) {
  return request.get<PageResult<ReportItem>>('/api/v1/report/records/page', { params })
}
export function getReportDetail(id: number) {
  return request.get<ReportItem>(`/api/v1/report/records/${id}`)
}
export function deleteReport(id: number) {
  return request.delete<void>(`/api/v1/report/records/${id}`)
}
export function generateReport(data: {
  type: ReportType; hazardPointId: number; periodStart: string; periodEnd: string
}) {
  return request.post<{ reportId: number }>('/api/v1/report/records/generate', data)
}
```

**类型枚举映射**（后端数字 ↔ 前端字符串，在 api 层做转换，业务层无感知）：
```typescript
const TYPE_CODE_TO_STR = { 2: 'weekly', 3: 'monthly', 4: 'quarterly' }
const TYPE_STR_TO_CODE = { weekly: 2, monthly: 3, quarterly: 4 }
```

### 8.3 `Report.vue` 改动点

#### 8.3.1 类型下拉新增季报
```typescript
const typeOptions = [
  { value: '', label: '全部' },
  { value: 'weekly', label: '周报' },
  { value: 'monthly', label: '月报' },
  { value: 'quarterly', label: '季报' },    // ★ 新增
]
```

#### 8.3.2 列表列改造

| 新列 | 字段 |
|---|---|
| 报告名称 | `reportName`（原 `title`） |
| 类型 | `type` + tag（季报用紫色） |
| 隐患点 | `hazardPointName` ★新增 |
| 周期 | `periodStart ~ periodEnd` |
| 状态 | `statusDesc` ★新增（失败红色 + tooltip `errorMsg`） |
| 生成时间 | `createTime` |
| 操作 | 查看 / 下载PDF / 删除 / 重新生成（失败时） |

#### 8.3.3 状态展示规则

| status | 展示 | 操作 |
|---|---|---|
| 1-生成中 | 灰色 tag | 禁用查看/下载 |
| 2-已生成 | 绿色 tag | 查看可点，下载可点 |
| 3-生成失败 | 红色 tag + tooltip | 查看/下载禁用，提供"重新生成"按钮 |

#### 8.3.4 手动生成弹窗（管理员可见，顶部按钮）

字段：报告类型 / 隐患点 / 周期起 / 周期止；提交后调 `generateReport()`，成功刷新列表；409 提示"该周期已存在报告，是否查看？"；通过 `v-hasPermi="['report:record:generate']"` 控制可见性。

#### 8.3.5 详情与 PDF 下载

**完全复用**现有逻辑（已实现）：详情对话框 `v-html="currentReport.content"`；下载 PDF 用 `html2canvas + jsPDF`；打印用 `window.print()`。唯一改动：详情数据从 `getReportDetail(id)` 拉取（列表不返 content）。

### 8.4 不做的事（YAGNI）

- ❌ 不做报告模板编辑器
- ❌ 不做报告对比/版本回滚
- ❌ 不做报告邮件推送
- ❌ 不重构 `Query.vue` / `Analysis.vue` 的 mock

---

## 9. 错误处理、日志与测试

### 9.1 错误处理矩阵

| 场景 | 处理 | 用户可见反馈 |
|---|---|---|
| 定时任务 IoTDB 连接失败（某 hp） | try-catch per hp，记 `status=3 + error_msg`，继续下一个 | 列表"状态=失败"，tooltip 显示原因 |
| 定时任务 Redis 锁获取失败 | 跳过整批，log info | 无（运维通过日志查看） |
| 定时任务 UNIQUE 冲突 | catch DuplicateKeyException，跳过 | 无 |
| 手动生成 period 跨度 > 400 天 | Controller 400 校验失败 | 弹窗"周期跨度超出限制" |
| 手动生成 同周期已存在 | Service 返回既有 reportId，Controller 转 409 | 弹窗"该周期已存在报告" |
| 手动生成 隐患点不存在 | Controller 400 校验失败 | 弹窗提示 |
| 手动生成 渲染超时（>30s） | Service catch，记 status=3，返回 500 | 弹窗"生成失败，请缩短周期重试" |
| 列表查询 status=1 时无 content | DTO 字段不返回 content | 列表行只展示元数据，查看禁用 |

### 9.2 日志规范

**业务日志（slf4j）**：
```java
log.info("[report] start type={} period={}~{}", type, period.start(), period.end());
log.info("[report] skip exists hp={} type={} period={}", hp.id(), type, period);
log.error("[report] fail hp={} type={} period={} reason={}", hp.id(), type, period, e.getMessage(), e);
log.info("[report] done type={} total={} success={} fail={}", type, hps.size(), success, fail);
```

**审计日志**：通过项目已有的 `@Log(title = "报告管理", businessType = ...)` 注解自动落 `log_operation_record`。

**运行日志**：定时任务异常通过项目全局异常处理自动入 `log_runtime_record`。

### 9.3 测试策略

| 测试类 | 类型 | 覆盖要点 | Mock |
|---|---|---|---|
| `ReportPeriodTest` | 纯单元 | 上周/上月/上季度边界（年末跨年、季末跨季、闰年 2 月） | 无 |
| `ReportDataAssemblerTest` | 单元 | 各 Service 调用编排、空数据兜底、单设备单传感器场景 | Mock 全部依赖 Service |
| `WeeklyReportRendererTest` | 单元 | HTML 输出包含所有必填字段、空数据时显示"无异常"、表格格式正确 | 无 |
| `MonthlyReportRendererTest` | 单元 | 同上 + 风险等级映射、Top10 截断 | 无 |
| `QuarterlyReportRendererTest` | 单元 | 同上 + 趋势方向符号、3×4 告警矩阵 | 无 |
| `ReportGenerationServiceTest` | 单元 | 并发锁、幂等跳过、单点失败隔离、UNIQUE 冲突处理 | Mock Redis/Mapper/各 Service |
| `ReportControllerTest` | 集成 (MockMvc) | 4 端点契约、权限拦截、参数校验、409 冲突响应 | MockMvc + Spring Security mock |

测试位置：`zwei-iot-report/src/test/java/com/zwei/iot/report/...`，覆盖率目标 **80%+**。

### 9.4 关键边界 case 必须覆盖

```
ReportPeriodTest:
- lastWeek(2026-06-15) → 2026-06-08 ~ 2026-06-14       (周一为周首)
- lastWeek(2026-01-01) → 2025-12-22 ~ 2025-12-28       (跨年)
- lastMonth(2026-01-15) → 2025-12-01 ~ 2025-12-31      (跨年)
- lastMonth(2024-03-15) → 2024-02-01 ~ 2024-02-29      (闰年)
- lastQuarter(2026-01-15) → 2025-10-01 ~ 2025-12-31    (Q4 上季)
- lastQuarter(2026-04-15) → 2026-01-01 ~ 2026-03-31    (Q1 上季)
- lastQuarter(2026-07-15) → 2026-04-01 ~ 2026-06-30    (Q2 上季)
- lastQuarter(2026-10-15) → 2026-07-01 ~ 2026-09-30    (Q3 上季)

ReportGenerationServiceTest:
- 同 type+hp+period 已有成功记录 → 跳过，不调 Renderer
- 渲染抛异常 → status=3，error_msg 填充，下一个 hp 继续
- Redis 锁获取失败 → 整批跳过
- UNIQUE 约束冲突 → catch + 跳过
```

### 9.5 监控指标（运维观察）

不强制接入 Prometheus，但在日志中输出可观测字段：

- 每次任务执行的 `total / success / fail` 数量
- 单 hp 平均耗时（用于发现慢 hp，可能需要 IoTDB 索引优化）
- 周报生成记录数 ≈ 监测中隐患点数（异常时检查 `hazard_point.status`）

### 9.6 风险与降级

| 风险 | 降级策略 |
|---|---|
| 大量隐患点 + 单线程串行执行过长 | 后续可改用 `@Async + 限流线程池`（不在本期范围） |
| IoTDB 历史数据已过期（默认保留 1 年） | 历史报告生成接口允许部分缺失，Renderer 内对空数据兜底显示"无数据" |
| Redis 不可用 | 锁获取失败 → 任务跳过；下次任务恢复，不影响业务 |
| 报告生成失败累积过多 | 列表筛选 status=3 批量查看，手动重新生成 |

---

## 10. 实现顺序（粗粒度，详细计划由 writing-plans 输出）

1. **DB**：升级脚本 v2.7（DDL + 菜单）
2. **跨模块接口**：
   - `zwei-iot-hazard` 加 `IDeviceHazardRelationService.getDevicesByHazardPoint` + 新建 `IHazardPointQueryService`
   - `zwei-iot-alarm` 新建 `IAlarmQueryService`
3. **新模块骨架**：父 pom 加 `zwei-iot-report` + `zwei-admin` 加依赖 + 包结构 + `ReportRecord` 实体/Mapper + 基础 CRUD
4. **周期与编排**：`ReportPeriod` + `ReportGenerationService` + `ReportScheduleJob`（带 Redis 锁）
5. **数据装配**：`ReportDataAssembler` + `ReportContext`
6. **三种渲染器**：`WeeklyReportRenderer` / `MonthlyReportRenderer` / `QuarterlyReportRenderer`
7. **Controller**：4 个端点 + DTO + 权限 + 校验
8. **配置**：`application.yml` 加开关
9. **前端**：`api/report.ts` 重写 + `Report.vue` 改造
10. **测试**：按 9.3 表格执行
11. **验证**：编译 + 单测 + 手动触发接口冒烟

---

## 11. 验收标准

| # | 标准 |
|---|---|
| 1 | `mvn clean test` 全绿（含新模块 80%+ 覆盖率） |
| 2 | `npm run build` 前端编译通过 |
| 3 | 手动调用 `POST /api/v1/report/records/generate` 能成功生成周报，列表可见，详情 HTML 渲染正确，PDF 可下载 |
| 4 | 修改系统时间到周一 02:00（或调整 cron），Job 自动触发并按隐患点批量生成 |
| 5 | 同 type+hp+period 重复生成返回 409 |
| 6 | 单个隐患点 IoTDB 查询失败时，其他隐患点报告正常生成，失败行 `status=3` 且 tooltip 显示原因 |
| 7 | 前端季报筛选项与季报紫色 tag 显示正确 |
| 8 | 三种报告内容均包含公共报告头与对应章节 |
| 9 | 删除报告为逻辑删除（`del_flag=1`），列表不再显示 |
| 10 | 权限：无 `report:record:*` 权限的用户访问接口返回 403 |
