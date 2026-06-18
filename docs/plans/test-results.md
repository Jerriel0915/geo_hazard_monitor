# 告警引擎逻辑调整 — 测试结果

> 日期：2026-06-17
> 环境：local profile，单机 MySQL/Redis/IoTDB/MQTT 均本地部署
> 设备：device_id=10 (`BP_YL_01`)，绑定 HP 17
> 传感器：sensor_id=13 (`YL_1`)，attrCode=`rainfall_hour` (monitor_content_id=1, monitor_type_id=1)

## 1. 数据准备摘要

### 1.1 修复发现的工程缺陷

测试过程中发现 `MonitorDataIngestedEvent` 在工程中**仅定义未发布**，导致告警引擎无法被触发。
本次同时修复：在 `MonitorIngestConsumerService` 中注入 `ApplicationEventPublisher`，
在 `processRecord` / `processParsedMessage` 的 IoTDB 写入成功分支逐点发布事件。

文件：`server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/service/MonitorIngestConsumerService.java`

### 1.2 数据修复

`sensor_attribute` 表中 device 10/4 的雨量属性原本 `monitor_content_id=NULL`，
已更新：

| attr_id | device | attr_code       | monitor_content_id |
|---------|--------|-----------------|--------------------|
| 11      | 4      | rainfall_hour   | 1                  |
| 12      | 4      | rainfall_day    | 2                  |
| 22      | 10     | rainfall_hour   | 1                  |
| 23      | 10     | rainfall_day    | 2                  |

### 1.3 测试判据 (alarm_criteria)

| id  | name                 | hazard_point_id | level_config                                                      | persist_count | silence_period |
|-----|----------------------|-----------------|-------------------------------------------------------------------|---------------|----------------|
| 11  | CR_HP_A_rain10_blue  | 17              | blue: rainfall_hour > 10                                          | 3             | 0              |
| 12  | CR_HP_B_rain50_red   | 17              | red:  rainfall_hour > 50                                          | 2             | 0              |
| 13  | CR_MT_C_rain5_blue   | NULL            | blue: rainfall_hour > 5 (兜底，预期永不触发)                       | 1             | 0              |

> 已禁用历史判据 id=1（`小时雨量通用判据`，与本测试 MT 判据语义重叠）。

## 2. 测试用例与结果

总计 **18 项断言 / 18 PASS / 0 FAIL**。

### 用例 1: 优先级排他

| 项 | 内容 |
|---|---|
| 输入序列 | `value=6`（仅满足 CR_MT_C 的 >5，不满足 HP 判据的 >10/>50） |
| 期望 | 0 条告警（HP 判据存在 → MT 判据不评估） |
| 实际 | 0 条告警 ✓ |

### 用例 2: 计数器独立累加

| 项 | 内容 |
|---|---|
| 输入序列 | `value=11, 11, 11` |
| 期望 | 第 3 次 CR_HP_A 达 persistCount=3 → 1 条 level=1 告警；CR_HP_B 从未满足，无计数 key |
| 实际 | 1 条 level=1 告警 (criteriaId=11)，Redis 中无 `:12:` 计数 key ✓ |

### 用例 3: 等级独立重置

| 项 | 内容 |
|---|---|
| 输入序列 | `value=60, 20, 60, 60` |
| 计数变化 | step1: blue=1, red=1 → step2: blue=2, red=0(重置) → step3: blue=3✓候选(level=1), red=1 → step4: red=2✓候选(level=4) |
| 期望 | 2 条告警 — CR_HP_A level=1，CR_HP_B level=4 |
| 实际 | 2 条告警，level 分别为 1 和 4 ✓ |

### 用例 4: 最高等级胜出

| 项 | 内容 |
|---|---|
| 输入序列 | `value=60, 60` |
| 期望 | CR_HP_B red 达 persistCount=2 触发；CR_HP_A blue 仅累到 2 未达 3 → 单条 level=4 |
| 实际 | 1 条 level=4 告警 (criteriaId=12) ✓ |

### 用例 5: 多判据候选合并

| 项 | 内容 |
|---|---|
| 输入序列 | `value=51, 51, 51` |
| 期望 | step2 CR_HP_B 触发 level=4；step3 CR_HP_A 触发 level=1 |
| 实际 | 2 条告警：id=13 (level=4, CR_HP_B), id=14 (level=1, CR_HP_A) ✓ |

## 3. 最终 DB 验证

`SELECT * FROM alarm_record WHERE device_id = 10`：

| id | alarm_level | alarm_level_text | criteria_id | trigger_count | alarm_message |
|----|-------------|------------------|-------------|---------------|---------------|
| 13 | 4           | 红色             | 12          | 1             | 阈值告警: CR_HP_B_rain50_red |
| 14 | 1           | 蓝色             | 11          | 1             | 阈值告警: CR_HP_A_rain10_blue |

## 4. 结论

- ✅ 隐患点判据存在时，监测类型兜底判据不被评估（用例 1）
- ✅ 不同判据的 persistCount 计数器完全独立（用例 2）
- ✅ 同一判据的不同等级计数器独立；某等级未满足时仅重置当前等级（用例 3 步骤 2 验证）
- ✅ 多判据同时成为候选时，选择最高等级写入 `alarm_record`（用例 4、5）
- ✅ 每条告警仅发布一次 `AlarmTriggeredEvent`，对应一条 alarm_record

## 5. 附带修复

- `MonitorIngestConsumerService` 增加 `MonitorDataIngestedEvent` 发布（填补历史遗漏）
- `sensor_attribute` 4 条记录补全 `monitor_content_id`

## 6. 测试脚本

`scripts/test_alarm_engine.py` — 一键跑全部 5 用例，使用 paho-mqtt + pymysql + redis。
重置逻辑：每个用例前清空 `alarm_record` 与 Redis `alarm:pre-trigger:*` / `alarm:last-trigger:*`。
