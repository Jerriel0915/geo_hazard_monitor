# 待办告警对接后端 API — 设计规格

> 日期: 2026-06-16
> 涉及模块: `server/zwei-iot-alarm`、`server/zwei-admin`、`web/src/views/alarm/`、`web/src/api/`
> 状态: 已批准，待编写实现计划

## 1. 背景与目标

`web/src/views/alarm/RealtimeAlarm.vue`（待办告警页）当前完全使用硬编码 `mockData`，未调用任何后端接口（已通过代码审查与 Playwright 网络监控证实）。本规格将其对接真实后端 API，并完成告警查看与处置闭环。

用户在此过程中扩展了范围：将告警日志体系从简单的「状态变更日志」升级为「全动作流水（ActionLog）」，并新增「告警触发明细」表以支撑详情弹窗的「告警记录」tab。

### 目标

- 删除 `RealtimeAlarm.vue` 的 mock，对接 `/alarm/records/*` 接口
- 重构 `AlarmRecordLog` → `AlarmRecordActionLog`（类/表重命名 + `actionType` 枚举 + 新字段）
- 新增 `alarm_record_trigger_detail` 表与接口
- 补齐引擎/处置/通知三处的动作日志写入
- 详情弹窗 4 个 tab + 时间线按后端能力对接（通知记录 tab 暂不对接）
- 不改动页面布局与样式

### 非目标

- 通知记录 tab 的对接（暂不管，保留 UI 显示「暂无数据」）
- 告警导出功能（后端不实现，前端按钮置灰）

## 2. 范围

三块工作，按依赖顺序实现：

1. **告警动作日志体系重构** — `AlarmRecordLog` → `AlarmRecordActionLog`
2. **告警触发明细** — 新增 `alarm_record_trigger_detail`
3. **前端对接** — 列表/搜索/详情/处置去 mock 化

## 3. 数据模型

### 3.1 新增表 `alarm_record_trigger_detail`（告警触发明细）

对应详情弹窗「告警记录」tab。引擎每次触发写一条数据快照。

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | bigint PK AUTO_INCREMENT | |
| `alarm_record_id` | bigint NOT NULL | 外键 → alarm_record.id |
| `trigger_time` | datetime NOT NULL | 告警时间 |
| `alarm_level` | tinyint | 触发时等级 1-4 |
| `alarm_type` | varchar(20) | THRESHOLD / COMPREHENSIVE |
| `alarm_message` | varchar(500) | 告警描述 |
| `create_time` | datetime DEFAULT CURRENT_TIMESTAMP | |

索引：`KEY idx_trigger_aid (alarm_record_id, trigger_time)`

### 3.2 改造表 `alarm_record_log` → `alarm_record_action_log`

由现有 `alarm_record_log` 改造（重命名 + 字段调整）。对应「处置记录」tab 与时间线。

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | bigint PK AUTO_INCREMENT | 沿用 |
| `alarm_record_id` | bigint NOT NULL | 沿用（原 `alarm_id` 改名） |
| `action_type` | varchar(30) NOT NULL | 枚举（见 3.3），原 `disposal_type` 改名 |
| `from_value` | varchar(20) | 变更前值，由 action_type 解释语义（等级或状态） |
| `to_value` | varchar(20) | 变更后值 |
| `remarks` | varchar(500) | 备注/反馈内容，原 `note` 改名 |
| `description` | varchar(500) | 描述内容（FEEDBACK 等动作附带） |
| `attachments` | varchar(1000) | 附件文件名，多个逗号分隔（`/common/upload` 返回的 `fileName`） |
| `operator` | varchar(64) | 操作人 |
| `create_time` | datetime DEFAULT CURRENT_TIMESTAMP | 操作时间 |

> 原 `from_status`/`to_status`/`disposal_type`/`note`/`disposal_result` 字段移除，语义并入 `from_value`/`to_value`/`action_type`/`remarks`/`description`。

### 3.3 `action_type` 枚举

| 值 | 含义 | from_value/to_value 语义 |
|---|---|---|
| `CREATE` | 引擎首次创建 | to_value = 等级 |
| `RE_TRIGGER` | 再次触发（同级） | — |
| `LEVEL_CHANGE` | 再次触发且等级变化 | from_value = 旧等级, to_value = 新等级 |
| `FEEDBACK` | 处置反馈（status→2） | to_value = 2 |
| `DISPOSE_CLOSE` | 销警（status→3） | to_value = 3 |
| `DISPOSE_FALSE_ALARM` | 误报（status→4） | to_value = 4 |
| `NOTIFY` | 通知发送 | — |

> `DESCRIPTION` / `ATTACHMENT` **不作为独立 action_type**；描述与附件是 FEEDBACK 等动作的附属字段。

## 4. 写入时机（数据流）

改造 `AlarmRecordServiceImpl.createOrUpdateAlarm`（引擎触发）、`dispose`/`batchDispose`（处置）、`AlarmNotifier`（通知）三处写入点。

| 触发场景 | alarm_record 主表 | trigger_detail | action_log |
|---|---|---|---|
| 引擎首次创建 | insert(status=1, triggerCount=1, first/lastTriggerTime) | +1 | CREATE(to_value=level) |
| 再次触发（同级） | update triggerCount+1, lastTriggerTime | +1 | RE_TRIGGER |
| 再次触发（等级变） | update **alarmLevel**, triggerCount+1, lastTriggerTime | +1 | RE_TRIGGER + LEVEL_CHANGE（**两条**，LEVEL_CHANGE 的 from/to 为旧/新等级） |
| 处置反馈（status=2） | update status=2 | — | FEEDBACK(to_value=2, remarks/description/attachments) |
| 销警（status=3） | update status=3 | — | DISPOSE_CLOSE(to_value=3) |
| 误报（status=4） | update status=4 | — | DISPOSE_FALSE_ALARM(to_value=4) |
| 批量处置 | 同上，逐条 | — | 同上，逐条 |
| 通知发送 | — | — | NOTIFY(remarks=渠道/接收人) |

### 现状差距（本次补齐）

- 再次触发：现状只 `updateTriggerCount`，**不写日志、不更新等级** → 补 RE_TRIGGER/LEVEL_CHANGE 日志 + 等级变化时更新主表
- 通知：`AlarmNotifier` 现状**不写日志** → 补 NOTIFY
- `disposalType` 现状为中文字符串 → 改为 action_type 枚举

## 5. 后端接口清单

| 接口 | 改动 |
|---|---|
| `GET /api/v1/alarm/records/pending` | **扩展筛选**：`hazardPointName`、`alarmLevels`(多选)、`alarmTypes`(多选)、`statusList`(多选)、`triggerTimeBegin`、`triggerTimeEnd` |
| `GET /api/v1/alarm/records/{id}` | 不变 |
| `PUT /api/v1/alarm/records/{id}/dispose` | 请求 DTO 扩展：`description` + `attachments` + `remarks`（FEEDBACK 时存） |
| `POST /api/v1/alarm/records/batch` | `BatchDisposeRequest` 同样扩展 `description`/`attachments`/`remarks`；`batchDispose` service 签名调整以传入（现状只传 ids/status/username，未传 note）；批量逐条写 action_log |
| `GET /api/v1/alarm/records/{id}/trigger-details` | **新增**：触发明细列表 |
| `GET /api/v1/alarm/records/{id}/action-logs` | **改名**自 `/logs`：动作日志列表 |
| 导出 | 后端不实现 |

### 权限

沿用现有 `iot:alarm-record:list` / `iot:alarm-record:dispose` / `iot:alarm-record:batch`。新增接口（trigger-details / action-logs）复用 `iot:alarm-record:list`。

## 6. 前端对接

### 6.1 `RealtimeAlarm.vue`（列表页）

- **删除** `mockData` 及 `filteredData`/`paginatedData` 计算属性
- **移除**「人员名称」搜索框；`queryParams` 去掉 `personName`、`alarmCountMin/Max`
- `onMounted` + 查询/翻页 → `getPendingAlarms({pageNum, pageSize, hazardPointName, alarmLevels, alarmTypes, statusList, triggerTimeBegin, triggerTimeEnd})`
- **字段映射**（mock → 真实）：`firstAlarmTime→firstTriggerTime`、`lastAlarmTime→lastTriggerTime`、`alarmCount→triggerCount`、`responderName→resolvedBy`、`responseTime→resolvedAt`、`alarmContent→alarmMessage`
- **枚举适配**：映射函数改吃数字；`alarmType` 比较改大写；tag 颜色按后端语义重映射
- **处置**：`handleFeedback` → `disposeAlarm(id,{status:2,description,attachments,remarks})`；批量反馈→`batchDisposeAlarms({ids,status:2,description,attachments,remarks})`；批量误报→`batchDisposeAlarms({ids,status:4})`；批量销警→`batchDisposeAlarms({ids,status:3})`。成功后重新拉列表
- **导出**：按钮 `disabled` + tooltip「暂未开放」

### 6.2 tag 颜色映射（按后端语义）

| 等级 | 颜色 | 状态 | 颜色 |
|---|---|---|---|
| 1 蓝色 | info | 1 待处理 | danger |
| 2 黄色 | warning | 2 处理中 | warning |
| 3 橙色 | warning | 3 已销警 | success |
| 4 红色 | danger | 4 误报 | info |

文本可直接用后端返回的 `alarmLevelText` / `statusName`。

### 6.3 `AlarmDetailDialog.vue`（详情弹窗，不改布局/样式）

- 打开时按 `data.id` 并发调 `getAlarmRecordDetail` / `getTriggerDetails` / `getActionLogs`
- 基础信息 tab：字段映射同 6.1
- 告警记录 tab：`getTriggerDetails(id)` → `{triggerTime, alarmLevel, alarmType, alarmMessage}`
- 处置记录 tab：`getActionLogs(id)` → `{createTime, actionType, operator, remarks/description}`
- 时间线：由 action_logs 按时间构造（trigger→红/notify→蓝/dispose→绿 圆点）
- 通知记录 tab：保留，显示「暂无数据」（暂不对接）
- 枚举映射函数同步适配数字/大写

### 6.4 `FeedbackDialog`（处置弹窗）

- 附件先调 `POST /api/v1/common/upload`（FormData），拿返回 `fileName` 多个逗号拼接为 `attachments`
- 提交 → `disposeAlarm(id,{status:2, description, attachments, remarks})` → 成功关闭 + 父组件刷新列表

### 6.5 `api/alarm.ts` / `realtimeAlarm.ts`

- `AlarmDisposePayload` 加 `description?: string`、`attachments?: string`、`remarks?: string`
- `AlarmRecordPageParams` 加 `alarmLevels?`、`alarmTypes?`、`statusList?`、`triggerTimeBegin?`、`triggerTimeEnd?`
- 新增 `getTriggerDetails(id)`
- `getAlarmRecordLogs` → `getActionLogs`（路径 `/alarm/records/{id}/action-logs`）

## 7. 验收标准

1. 待办告警页打开后发起 `GET /alarm/records/pending` 请求，表格展示真实数据，无 mock 残留
2. 筛选（隐患点名、等级/类型/状态多选、时间范围）生效，「人员名称」框已移除
3. 处置反馈/批量误报/批量销警调用对应接口，成功后列表刷新
4. 详情弹窗基础信息/告警记录/处置记录 tab 与时间线展示真实数据；通知记录 tab 显示「暂无数据」
5. 引擎再次触发同级告警 → 写 RE_TRIGGER；触发更高/低等级 → 写 RE_TRIGGER + LEVEL_CHANGE 且主表 alarmLevel 更新
6. 通知发送 → 写 NOTIFY
7. 导出按钮置灰 + tooltip
8. 后端单测覆盖 `createOrUpdateAlarm` 的三条触发分支与 dispose 的 action_log 写入

## 8. 风险与备注

- `alarm_record_log` → `alarm_record_action_log` 涉及表重命名与字段迁移，需提供升级 SQL（`db/upgrade/`），保留历史数据（字段语义映射）
- `action_type` 用 varchar 枚举而非 DB enum，便于扩展；Java 侧定义枚举常量类
- 附件 `attachments` 存 fileName（磁盘路径），不引入文件表；依赖现有 `/common/upload`
- 再次触发写两条日志（RE_TRIGGER + LEVEL_CHANGE）会使该告警 action_log 条数增长较快，索引 `(alarm_record_id, create_time)` 已覆盖
