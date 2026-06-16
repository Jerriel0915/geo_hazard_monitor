# 告警处置 CRUD — 设计规格

> 日期: 2026-06-16 | 范围: 待办告警处置 + 历史告警归档 | 状态: 已确认

## 目标

实现告警中心待办告警（RealtimeAlarm）与历史告警（AlarmNotification）的基本 CRUD 功能。告警引擎生成逻辑不在本次范围内——前端已有的列表、筛选、处置、反馈、时间线等 UI 需要对接真实后端 API，替换 mock 数据。

## 数据库变更

### 新增表 `alarm_feedback`

独立于 `alarm_record_log`，存储不改变告警状态的纯反馈记录（可多次追加，带文件附件）：

```sql
CREATE TABLE `alarm_feedback` (
    `id`          bigint       NOT NULL AUTO_INCREMENT,
    `alarm_id`    bigint       NOT NULL COMMENT '告警记录ID',
    `content`     text         COMMENT '反馈文本内容',
    `files`       json         DEFAULT NULL COMMENT '附件列表 [{name,url,size}]',
    `operator`    varchar(64)  DEFAULT NULL COMMENT '反馈人',
    `create_time` datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '反馈时间',
    PRIMARY KEY (`id`),
    KEY `idx_feedback_alarm_id` (`alarm_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='告警反馈记录';
```

### 不改动的表

- `alarm_record` — 字段完整，`resolved_by`/`resolved_at` 直接映射前端"响应人员/响应时间"
- `alarm_record_log` — 继续作为时间线（状态变更）数据源
- `alarm_notification` — 本次只读（通知记录 tab），手动通知 API 不在范围

## 后端 API

### 新增接口

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| `GET` | `/api/v1/alarm/records/{id}/feedbacks` | `iot:alarm-record:list` | 查询告警的所有反馈记录 |
| `POST` | `/api/v1/alarm/records/{id}/feedback` | `iot:alarm-record:dispose` | 添加反馈（`{content, files}`） |

### 扩展现有接口

`GET /pending` 和 `GET /history` 查询参数增加：

| 参数 | 类型 | 说明 |
|------|------|------|
| `startTime` | String | 告警时间起始（first_trigger_time >=） |
| `endTime` | String | 告警时间截止（first_trigger_time <=） |
| `personName` | String | 处置人模糊匹配（resolved_by LIKE） |

### 不改动

- `AlarmRecordController` 已有端点（pending/history/detail/dispose/batch/logs）逻辑不变
- 判据/策略/分发/引擎代码不动
- 手动通知 API 不新增

### 新文件清单

| 文件 | 包 |
|------|-----|
| `AlarmFeedback.java` | `com.zwei.iot.alarm.domain` |
| `AlarmFeedbackMapper.java` | `com.zwei.iot.alarm.mapper` |
| `AlarmFeedbackMapper.xml` | `mapper/` |
| `IAlarmFeedbackService.java` | `com.zwei.iot.alarm.service` |
| `AlarmFeedbackServiceImpl.java` | `com.zwei.iot.alarm.service.impl` |

### 修改文件清单

| 文件 | 变更 |
|------|------|
| `AlarmRecordMapper.xml` | pending/history 查询增加 startTime/endTime/personName 动态条件 |

## 前端改动

### RealtimeAlarm.vue（待办告警）

- 删除 `mockData`，改用 `getPendingAlarms()` API
- 筛选从本地 `computed` 改为服务端分页（查询参数直接传 API）
- 状态值：`'pending'`→`1`, `'processing'`→`2`
- 字段映射：`resolvedBy`→`responderName`, `resolvedAt`→`responseTime`
- 处置/批量误报/批量销警对接已有 `disposeAlarm()`/`batchDisposeAlarms()`

### AlarmNotification.vue（历史告警）

- 删除 `mockData`，改用 `getHistoryAlarms()` API
- 状态值：`'false_alarm'`→`4`, `'closed'`→`3`
- 同上的筛选、字段映射

### FeedbackDialog.vue（处置弹窗）

| Tab/区域 | 变更 |
|----------|------|
| 时间线（右侧） | 调用 `getAlarmRecordLogs(id)` 替换 mock |
| 反馈历史 tab | 调用 `GET /{id}/feedbacks` 替换 mock |
| 提交反馈 | 调用 `POST /{id}/feedback` 替换本地 push |
| 通知记录 tab | **暂留 mock**（手动通知 API 未实现） |
| 监测数据/告警次数 tab | **暂留 mock**（需要 IoTDB 查询，不在范围） |

## 数据流

```
待办告警列表                         处置弹窗
┌─────────────────────┐      ┌──────────────────────────────────┐
│ GET /pending         │      │ GET /{id}/logs       → 时间线     │
│   ↓                  │      │ GET /{id}/feedbacks  → 反馈历史   │
│ alarm_record         │      │ POST /{id}/feedback  ← 提交反馈   │
│ (status=1,2)         │      │ PUT /{id}/dispose    ← 处置操作   │
└─────────────────────┘      └──────────────────────────────────┘
           │                               │
           │  处置后状态变更                   │ 写入
           │  status 1→2/3/4                ↓
           │                   ┌──────────────────────┐
           └───────────────────│ alarm_record_log      │ (状态流转)
                               │ alarm_feedback        │ (反馈记录)
                               └──────────────────────┘

历史告警列表
┌─────────────────────┐
│ GET /history         │
│   ↓                  │
│ alarm_record         │
│ (status=3,4)         │
└─────────────────────┘
```

## 不在范围内

- 告警引擎（AlarmEvaluationEngine）逻辑
- 手动通知 API（Notify 组件）
- 监测数据/告警次数 tab（需要 IoTDB 查询）
- 导出功能
- 判据/策略/分发规则 CRUD
