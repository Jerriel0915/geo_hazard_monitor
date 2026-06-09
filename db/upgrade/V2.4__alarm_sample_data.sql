-- ============================================================
-- V2.4: 告警模块 — 样例数据
-- 说明: 基于已有的监测类型/监测内容/隐患点/设备/传感器/用户生成
-- 日期: 2026-06-09
-- ============================================================

-- ------------------------------------------------------------------
-- 1. 告警判据 — 样例数据
--    覆盖: 单阈值 / 多条件AND / 多条件OR / 全局判据 / 隐患点级判据
-- ------------------------------------------------------------------
INSERT INTO `alarm_criteria` (`id`, `name`, `monitor_type_id`, `monitor_type_name`, `monitor_content_id`,
                              `monitor_content_code`, `hazard_point_id`, `conditions_json`, `logic_operator`,
                              `blue_expression`, `blue_description`, `yellow_expression`, `yellow_description`,
                              `orange_expression`, `orange_description`, `red_expression`, `red_description`,
                              `persist_count`, `silence_period`, `is_enabled`, `version`, `create_by`, `create_time`,
                              `update_by`, `update_time`)
VALUES
-- 判据1: 雨量监测全局判据（小时雨量阈值）
(1, '小时雨量通用判据', 1, '雨量监测', 1, 'rainfall_hour', NULL,
 '[
   {
     "indicator": "rainfall_hour",
     "operator": "GT",
     "threshold": 10.0
   }
 ]',
 'AND',
 '{"operator":"GT","threshold":10.0}', '小时雨量 > 10mm',
 '{"operator":"GT","threshold":25.0}', '小时雨量 > 25mm',
 '{"operator":"GT","threshold":50.0}', '小时雨量 > 50mm',
 '{"operator":"GT","threshold":100.0}', '小时雨量 > 100mm',
 2, 36, 1, 1, 'admin', '2026-05-20 10:00:00', 'admin', '2026-05-25 14:30:00'),

-- 判据2: 龙泉寺隐患点位移监测（X轴+Z轴联合）
(2, '龙泉寺位移联合判据', 2, '位移监测', 3, 'displacement_x', 2,
 '[
   {
     "indicator": "displacement_x",
     "operator": "GT",
     "threshold": 20.0
   },
   {
     "indicator": "displacement_z",
     "operator": "GT",
     "threshold": 15.0
   }
 ]',
 'AND',
 '{"operator":"GT","threshold":15.0}', '单轴位移 > 15mm',
 '{"operator":"GT","threshold":25.0}', '单轴位移 > 25mm',
 '{"operator":"GT","threshold":40.0}', '单轴位移 > 40mm',
 '{"operator":"GT","threshold":60.0}', '单轴位移 > 60mm',
 1, 0, 1, 2, 'admin', '2026-05-22 09:00:00', 'admin', '2026-06-01 16:00:00'),

-- 判据3: 顺发铁矿边坡 — 裂缝宽度单阈值
(3, '顺发铁矿裂缝宽度判据', 5, '裂缝监测', 9, 'crack_width', 5,
 '[
   {
     "indicator": "crack_width",
     "operator": "GT",
     "threshold": 2.0
   }
 ]',
 'AND',
 '{"operator":"GT","threshold":2.0}', '裂缝宽度 > 2mm',
 '{"operator":"GT","threshold":5.0}', '裂缝宽度 > 5mm',
 '{"operator":"GT","threshold":10.0}', '裂缝宽度 > 10mm',
 '{"operator":"GT","threshold":20.0}', '裂缝宽度 > 20mm',
 3, 60, 1, 1, 'admin', '2026-05-18 08:00:00', NULL, '2026-05-18 08:00:00'),

-- 判据4: 清溪乡 — 小时雨量 OR 日雨量（多条件OR）
(4, '清溪乡雨量综合判据', 1, '雨量监测', 1, 'rainfall_hour', 3,
 '[
   {
     "indicator": "rainfall_hour",
     "operator": "GT",
     "threshold": 20.0
   },
   {
     "indicator": "rainfall_day",
     "operator": "GT",
     "threshold": 80.0
   }
 ]',
 'OR',
 '{"operator":"GT","threshold":20.0}', '小时雨量>20 或 日雨量>80',
 '{"operator":"GT","threshold":40.0}', '小时雨量>40 或 日雨量>120',
 '{"operator":"GT","threshold":60.0}', '小时雨量>60 或 日雨量>160',
 '{"operator":"GT","threshold":100.0}', '小时雨量>100 或 日雨量>200',
 1, 24, 1, 1, 'admin', '2026-06-01 07:00:00', 'admin', '2026-06-05 11:00:00'),

-- 判据5: 工业园区 — 水位监测（停用状态）
(5, '工业园区水位判据（已停用）', 4, '水位监测', 8, 'water_level', 4,
 '[
   {
     "indicator": "water_level",
     "operator": "GT",
     "threshold": 5.0
   }
 ]',
 'AND',
 '{"operator":"GT","threshold":5.0}', '水位 > 5m',
 '{"operator":"GT","threshold":8.0}', '水位 > 8m',
 '{"operator":"GT","threshold":12.0}', '水位 > 12m',
 '{"operator":"GT","threshold":20.0}', '水位 > 20m',
 2, 12, 0, 1, 'admin', '2026-05-10 15:00:00', 'admin', '2026-06-03 09:00:00'),

-- 判据6: 温湿度全局判据（温度过高）
(6, '温度异常判据', 3, '温湿度监测', 6, 'temperature', NULL,
 '[
   {
     "indicator": "temperature",
     "operator": "GT",
     "threshold": 45.0
   }
 ]',
 'AND',
 '{"operator":"GT","threshold":40.0}', '温度 > 40℃',
 '{"operator":"GT","threshold":45.0}', '温度 > 45℃',
 '{"operator":"GT","threshold":55.0}', '温度 > 55℃',
 '{"operator":"GT","threshold":70.0}', '温度 > 70℃',
 1, 0, 1, 1, 'admin', '2026-05-15 10:30:00', NULL, '2026-05-15 10:30:00');

-- ------------------------------------------------------------------
-- 2. 判据变更日志 — 样例数据
-- ------------------------------------------------------------------
INSERT INTO `alarm_criteria_log` (`id`, `criteria_id`, `version`, `change_type`, `old_value`, `new_value`, `create_by`,
                                  `create_time`)
VALUES (1, 1, 1, 'CREATE', NULL,
        '{
          "name": "小时雨量通用判据",
          "monitor_type_id": 1,
          "conditions_json": [
            {
              "indicator": "rainfall_hour",
              "operator": "GT",
              "threshold": 10.0
            }
          ],
          "persist_count": 2,
          "silence_period": 36,
          "is_enabled": 1
        }',
        'admin', '2026-05-20 10:00:00'),
       (2, 2, 1, 'CREATE', NULL,
        '{
          "name": "龙泉寺位移联合判据",
          "monitor_type_id": 2,
          "conditions_json": [
            {
              "indicator": "displacement_x",
              "operator": "GT",
              "threshold": 20.0
            }
          ],
          "persist_count": 1
        }',
        'admin', '2026-05-22 09:00:00'),
       (3, 2, 2, 'UPDATE',
        '{
          "conditions_json": [
            {
              "indicator": "displacement_x",
              "operator": "GT",
              "threshold": 20.0
            }
          ]
        }',
        '{
          "conditions_json": [
            {
              "indicator": "displacement_x",
              "operator": "GT",
              "threshold": 20.0
            },
            {
              "indicator": "displacement_z",
              "operator": "GT",
              "threshold": 15.0
            }
          ],
          "logic_operator": "AND"
        }',
        'admin', '2026-06-01 16:00:00'),
       (4, 5, 1, 'CREATE', NULL,
        '{
          "name": "工业园区水位判据",
          "monitor_type_id": 4,
          "monitor_content_id": 8
        }',
        'admin', '2026-05-10 15:00:00'),
       (5, 5, 1, 'TOGGLE',
        '{
          "is_enabled": 1
        }',
        '{
          "is_enabled": 0
        }',
        'admin', '2026-06-03 09:00:00');

-- ------------------------------------------------------------------
-- 3. 告警记录 — 样例数据
--    覆盖: 待处理/处理中/已销警/误报 四种状态，蓝/黄/橙/红 四级
-- ------------------------------------------------------------------
INSERT INTO `alarm_record` (`id`, `hazard_point_id`, `hazard_point_name`, `device_id`, `sensor_id`,
                            `monitor_content_id`, `alarm_level`, `alarm_level_text`, `alarm_type`, `alarm_message`,
                            `criteria_id`, `strategy_id`, `current_value`, `threshold_value`, `trigger_conditions`,
                            `first_trigger_time`, `last_trigger_time`, `trigger_count`, `status`, `status_name`,
                            `resolved_by`, `resolved_at`, `resolution_note`, `create_by`, `create_time`, `update_by`,
                            `update_time`)
VALUES
-- 记录1: 龙泉寺 — 橙色预警 — 待处理
(1, 2, '龙泉寺崩塌隐患点', 1, 2, 3, 3, '橙色预警', 'THRESHOLD',
 '位移X轴当前值 42.35mm 超过橙色阈值 40.00mm，触发橙色预警',
 2, NULL, 42.3500, 40.0000,
 '[
   {
     "indicator": "displacement_x",
     "currentValue": 42.35,
     "threshold": 40.0,
     "operator": "GT"
   },
   {
     "indicator": "displacement_z",
     "currentValue": 18.20,
     "threshold": 15.0,
     "operator": "GT"
   }
 ]',
 '2026-06-09 08:15:00', '2026-06-09 08:35:00', 5, 1, '待处理', NULL, NULL, NULL,
 'SYSTEM', '2026-06-09 08:15:00', NULL, '2026-06-09 08:35:00'),

-- 记录2: 清溪乡 — 黄色预警 — 处理中
(2, 3, '清溪乡泥石流隐患点', 1, 2, 1, 2, '黄色预警', 'THRESHOLD',
 '小时雨量 28.50mm 超过黄色阈值 25.00mm，触发黄色预警',
 1, NULL, 28.5000, 25.0000,
 '[
   {
     "indicator": "rainfall_hour",
     "currentValue": 28.50,
     "threshold": 25.0,
     "operator": "GT"
   }
 ]',
 '2026-06-09 07:30:00', '2026-06-09 08:00:00', 3, 2, '处理中', NULL, NULL, NULL,
 'SYSTEM', '2026-06-09 07:30:00', 'admin', '2026-06-09 08:00:00'),

-- 记录3: 顺发铁矿 — 蓝色预警 — 已销警
(3, 5, '顺发铁矿边坡监测点', 1, 2, 9, 1, '蓝色预警', 'THRESHOLD',
 '裂缝宽度 2.50mm 超过蓝色阈值 2.00mm，触发蓝色预警',
 3, NULL, 2.5000, 2.0000,
 '[
   {
     "indicator": "crack_width",
     "currentValue": 2.50,
     "threshold": 2.0,
     "operator": "GT"
   }
 ]',
 '2026-06-08 14:00:00', '2026-06-08 14:30:00', 7, 3, '已销警', 'admin', '2026-06-09 10:00:00',
 '现场核查裂缝已回缩至1.2mm，暂未发现进一步扩展趋势，解除告警并持续观察。',
 'SYSTEM', '2026-06-08 14:00:00', 'admin', '2026-06-09 10:00:00'),

-- 记录4: 龙泉寺 — 红色预警 — 待处理（高优先级）
(4, 2, '龙泉寺崩塌隐患点', 1, 2, 4, 4, '红色预警', 'THRESHOLD',
 '位移Y轴 28.70mm 超过红色阈值 25.00mm（单月累计位移超限），触发红色预警，请立即响应！',
 2, NULL, 28.7000, 25.0000,
 '[
   {
     "indicator": "displacement_y",
     "currentValue": 28.70,
     "threshold": 25.0,
     "operator": "GT"
   }
 ]',
 '2026-06-09 09:00:00', '2026-06-09 09:10:00', 3, 1, '待处理', NULL, NULL, NULL,
 'SYSTEM', '2026-06-09 09:00:00', NULL, '2026-06-09 09:10:00'),

-- 记录5: 工业园区 — 蓝色预警 — 误报
(5, 4, '工业园区地面沉降点', 1, 2, 1, 1, '蓝色预警', 'THRESHOLD',
 '小时雨量 12.00mm 超过蓝色阈值 10.00mm，触发蓝色预警',
 1, NULL, 12.0000, 10.0000,
 '[
   {
     "indicator": "rainfall_hour",
     "currentValue": 12.00,
     "threshold": 10.0,
     "operator": "GT"
   }
 ]',
 '2026-06-07 16:00:00', '2026-06-07 16:30:00', 1, 4, '误报', 'ry', '2026-06-08 09:00:00',
 '传感器临时故障导致数据跳变，设备已校准恢复正常，判定为误报。',
 'SYSTEM', '2026-06-07 16:00:00', 'ry', '2026-06-08 09:00:00'),

-- 记录6: 清溪乡 — 橙色预警 — 处理中（综合策略触发）
(6, 3, '清溪乡泥石流隐患点', 1, 2, 1, 3, '橙色预警', 'COMPREHENSIVE',
 '综合策略【清溪乡暴雨泥石流综合预警】触发：小时雨量60.0mm+日雨量145mm，土壤含水率超饱和，泥石流风险极高',
 NULL, 1, 60.0000, NULL,
 '[
   {
     "indicator": "rainfall_hour",
     "value": 60.0
   },
   {
     "indicator": "rainfall_day",
     "value": 145.0
   },
   {
     "indicator": "soil_moisture",
     "value": 82.5
   }
 ]',
 '2026-06-09 06:00:00', '2026-06-09 06:20:00', 4, 2, '处理中', NULL, NULL, NULL,
 'SYSTEM', '2026-06-09 06:00:00', 'admin', '2026-06-09 06:30:00'),

-- 记录7: 龙泉寺 — 黄色预警 — 已销警（历史记录）
(7, 2, '龙泉寺崩塌隐患点', 1, 2, 3, 2, '黄色预警', 'THRESHOLD',
 '位移X轴 27.00mm 超过黄色阈值 25.00mm，触发黄色预警',
 2, NULL, 27.0000, 25.0000,
 '[
   {
     "indicator": "displacement_x",
     "currentValue": 27.00,
     "threshold": 25.0,
     "operator": "GT"
   }
 ]',
 '2026-06-05 11:00:00', '2026-06-05 11:45:00', 10, 3, '已销警', 'admin', '2026-06-06 15:00:00',
 '支护加固施工已完成，后续监测数据恢复稳定。',
 'SYSTEM', '2026-06-05 11:00:00', 'admin', '2026-06-06 15:00:00'),

-- 记录8: 隐患点A — 蓝色预警 — 待处理
(8, 1, '隐患点A修改', 1, 2, 2, 1, '蓝色预警', 'THRESHOLD',
 '日雨量 82.00mm 超过蓝色阈值 80.00mm，触发蓝色预警',
 1, NULL, 82.0000, 80.0000,
 '[
   {
     "indicator": "rainfall_day",
     "currentValue": 82.00,
     "threshold": 80.0,
     "operator": "GT"
   }
 ]',
 '2026-06-08 20:00:00', '2026-06-08 20:00:00', 1, 1, '待处理', NULL, NULL, NULL,
 'SYSTEM', '2026-06-08 20:00:00', NULL, '2026-06-08 20:00:00');

-- ------------------------------------------------------------------
-- 4. 告警状态变更日志 — 样例数据
-- ------------------------------------------------------------------
INSERT INTO `alarm_record_log` (`id`, `alarm_id`, `from_status`, `to_status`, `operator`, `note`, `disposal_type`,
                                `disposal_result`, `create_time`)
VALUES
-- 记录1: 龙泉寺橙色 → 创建时 = 待处理
(1, 1, NULL, 1, 'SYSTEM', '告警自动生成', NULL, NULL, '2026-06-09 08:15:00'),
-- 记录2: 清溪乡黄色 → 从待处理 → 处理中
(2, 2, NULL, 1, 'SYSTEM', '告警自动生成', NULL, NULL, '2026-06-09 07:30:00'),
(3, 2, 1, 2, 'admin', '已指派现场核查人员', '开始处置', '已安排龙泉片区巡查员赶赴现场', '2026-06-09 08:00:00'),
-- 记录3: 顺发铁矿蓝色 → 待处理 → 处理中 → 已销警
(4, 3, NULL, 1, 'SYSTEM', '告警自动生成', NULL, NULL, '2026-06-08 14:00:00'),
(5, 3, 1, 2, 'admin', '开始核查裂缝情况', '开始处置', '裂缝计读数在波动，安排现场测量', '2026-06-09 08:30:00'),
(6, 3, 2, 3, 'admin', '现场核查裂缝已回缩，解除告警', '已销警', '裂缝回缩至1.2mm，持续观察中', '2026-06-09 10:00:00'),
-- 记录4: 龙泉寺红色 → 待处理（高优先级）
(7, 4, NULL, 1, 'SYSTEM', '红色预警自动生成，请立即响应！', NULL, NULL, '2026-06-09 09:00:00'),
-- 记录5: 工业园区 → 标记误报
(8, 5, NULL, 1, 'SYSTEM', '告警自动生成', NULL, NULL, '2026-06-07 16:00:00'),
(9, 5, 1, 4, 'ry', '传感器校准后数据恢复正常，判定为误报', '标记误报', '传感器临时故障，已校准恢复',
 '2026-06-08 09:00:00'),
-- 记录6: 清溪乡综合策略 → 待处理 → 处理中
(10, 6, NULL, 1, 'SYSTEM', '综合策略告警自动生成', NULL, NULL, '2026-06-09 06:00:00'),
(11, 6, 1, 2, 'admin', '启动泥石流应急响应预案', '开始处置', '已通知下游村庄做好撤离准备', '2026-06-09 06:30:00'),
-- 记录7: 龙泉寺历史黄色 → 已销警
(12, 7, NULL, 1, 'SYSTEM', '告警自动生成', NULL, NULL, '2026-06-05 11:00:00'),
(13, 7, 1, 2, 'admin', '启动支护加固评估', '开始处置', '联系施工队进行现场评估', '2026-06-05 13:00:00'),
(14, 7, 2, 3, 'admin', '支护加固施工完成，监测数据恢复稳定', '已销警', '支护施工完成，数据连续稳定48小时',
 '2026-06-06 15:00:00'),
-- 记录8: 隐患点A蓝色 → 待处理
(15, 8, NULL, 1, 'SYSTEM', '告警自动生成', NULL, NULL, '2026-06-08 20:00:00');

-- ------------------------------------------------------------------
-- 5. 综合告警策略 — 样例数据
-- ------------------------------------------------------------------
INSERT INTO `alarm_strategy` (`id`, `name`, `description`, `monitor_type_id`, `trigger_mode`, `cron_expression`,
                              `script_type`, `script_content`, `default_alarm_level`, `silence_minutes`,
                              `escalation_enabled`, `is_enabled`, `last_run_time`, `last_run_result`, `create_by`,
                              `create_time`, `update_by`, `update_time`)
VALUES
-- 策略1: 清溪乡暴雨泥石流综合预警（实时触发）
(1, '清溪乡暴雨泥石流综合预警',
 '当小时雨量 > 30mm 且日雨量 > 100mm 且土壤含水率 > 75% 时，综合判定泥石流风险并自动升级告警等级', 1, 'REALTIME', NULL,
 'GROOVY',
 '// 清溪乡暴雨泥石流综合判据
 def hourRain = getLatestValue("rainfall_hour", hazardPointId)
 def dayRain = getLatestValue("rainfall_day", hazardPointId)
 def soilMoisture = getLatestValue("soil_moisture", hazardPointId)

 if (hourRain == null || dayRain == null || soilMoisture == null) {
     return AlarmResult.noAlarm("数据不完整")
 }

 if (hourRain > 50 && dayRain > 120 && soilMoisture > 80) {
     return AlarmResult.red("小时雨量${hourRain}mm + 日雨量${dayRain}mm + 土壤含水率${soilMoisture}%，泥石流风险极高")
 } else if (hourRain > 30 && dayRain > 80 && soilMoisture > 70) {
     return AlarmResult.orange("小时雨量${hourRain}mm + 日雨量${dayRain}mm，泥石流风险较高")
 } else if (hourRain > 20 && dayRain > 50) {
     return AlarmResult.yellow("小时雨量${hourRain}mm + 日雨量${dayRain}mm，需关注")
 } else {
     return AlarmResult.noAlarm()
 }',
 3, 30, 1, 1, '2026-06-09 06:00:00', 'SUCCESS', 'admin', '2026-05-25 14:00:00', 'admin', '2026-06-09 06:20:00'),

-- 策略2: 龙泉寺多指标综合评估（Cron周期）
(2, '龙泉寺位移综合评估（每日）', '每天8:00综合评估前24小时位移数据，检测异常趋势', 2, 'CRON', '0 0 8 * * ?', 'GROOVY',
 '// 龙泉寺位移趋势分析
 def xDisplacement24h = getTrend("displacement_x", 24 * 60 * 60 * 1000)
 def zDisplacement24h = getTrend("displacement_z", 24 * 60 * 60 * 1000)

 if (xDisplacement24h == null || zDisplacement24h == null) {
     return AlarmResult.noAlarm("数据不足")
 }

 def totalTrend = Math.sqrt(xDisplacement24h * xDisplacement24h + zDisplacement24h * zDisplacement24h)

 if (totalTrend > 15) {
     return AlarmResult.red("24小时合成位移趋势 > 15mm，严重加速")
 } else if (totalTrend > 8) {
     return AlarmResult.orange("24小时合成位移趋势 > 8mm，加速明显")
 } else if (totalTrend > 3) {
     return AlarmResult.yellow("24小时合成位移趋势 > 3mm，有加速迹象")
 } else {
     return AlarmResult.noAlarm("位移趋势正常: ${totalTrend}mm")
 }',
 3, 1440, 0, 1, '2026-06-09 08:00:00', 'NO_ALARM', 'admin', '2026-05-28 10:00:00', NULL, '2026-06-09 08:00:00'),

-- 策略3: 温度异常跳变检测（已停用）
(3, '温度异常跳变检测（已停用）', '检测10分钟内温度变化超过15℃的异常跳变', 3, 'REALTIME', NULL, 'GROOVY',
 '// 温度跳变检测
 def currentTemp = getLatestValue("temperature", hazardPointId)
 def tenMinAgoTemp = getValueAt("temperature", hazardPointId, 10 * 60 * 1000)

 if (currentTemp == null || tenMinAgoTemp == null) {
     return AlarmResult.noAlarm("数据不足")
 }

 def delta = Math.abs(currentTemp - tenMinAgoTemp)
 if (delta > 25) {
     return AlarmResult.red("温度跳变${delta}℃，疑似传感器故障")
 } else if (delta > 15) {
     return AlarmResult.orange("温度跳变${delta}℃，请核实")
 } else {
     return AlarmResult.noAlarm("温度正常波动")
 }',
 3, 10, 0, 0, '2026-06-01 12:00:00', 'FAIL', 'admin', '2026-05-20 16:00:00', 'admin', '2026-06-02 10:00:00');

-- ------------------------------------------------------------------
-- 6. 策略-隐患点绑定 — 样例数据
-- ------------------------------------------------------------------
INSERT INTO `alarm_strategy_hazard_point` (`id`, `strategy_id`, `hazard_point_id`, `create_by`, `create_time`)
VALUES
-- 策略1绑定清溪乡泥石流隐患点
(1, 1, 3, 'admin', '2026-05-25 14:00:00'),
-- 策略2绑定龙泉寺
(2, 2, 2, 'admin', '2026-05-28 10:00:00'),
-- 策略3绑定顺发铁矿（已停用策略但绑定仍保留）
(3, 3, 5, 'admin', '2026-05-20 16:00:00');

-- ------------------------------------------------------------------
-- 7. 告警分发规则 — 样例数据
-- ------------------------------------------------------------------
INSERT INTO `alarm_dispatch_rule` (`id`, `name`, `hazard_point_id`, `alarm_levels`, `alarm_types`, `recipients_json`,
                                   `channels`, `time_window`, `is_enabled`, `create_by`, `create_time`, `update_by`,
                                   `update_time`)
VALUES
-- 规则1: 全局默认 — 蓝色仅系统通知
(1, '全局默认分发规则', NULL, '1', 'THRESHOLD,COMPREHENSIVE',
 '[
   {
     "userId": 2,
     "name": "若依",
     "phone": "15666666666"
   }
 ]',
 'SYSTEM', NULL, 1, 'admin', '2026-05-20 10:00:00', NULL, '2026-05-20 10:00:00'),

-- 规则2: 全局默认 — 黄色及以上 SMS+SYSTEM
(2, '全局黄色及以上分发规则', NULL, '2,3,4', 'THRESHOLD,COMPREHENSIVE',
 '[
   {
     "userId": 1,
     "name": "管理员",
     "phone": "15888888888"
   },
   {
     "userId": 2,
     "name": "若依",
     "phone": "15666666666"
   }
 ]',
 'SYSTEM,SMS', NULL, 1, 'admin', '2026-05-20 10:00:00', NULL, '2026-05-20 10:00:00'),

-- 规则3: 龙泉寺专项 — 红色+橙色 — 工作日白天
(3, '龙泉寺高等级告警分发', 2, '3,4', 'THRESHOLD,COMPREHENSIVE',
 '[
   {
     "userId": 1,
     "name": "管理员",
     "phone": "15888888888"
   }
 ]',
 'SYSTEM,SMS', '08:00-20:00', 1, 'admin', '2026-05-22 09:00:00', 'admin', '2026-06-01 16:00:00'),

-- 规则4: 清溪乡泥石流专项 — 全等级（已停用）
(4, '清溪乡泥石流专项分发', 3, '1,2,3,4', 'THRESHOLD,COMPREHENSIVE',
 '[
   {
     "userId": 1,
     "name": "管理员",
     "phone": "15888888888"
   },
   {
     "userId": 2,
     "name": "若依",
     "phone": "15666666666"
   }
 ]',
 'SYSTEM,SMS,EMAIL', NULL, 0, 'admin', '2026-05-25 14:30:00', 'admin', '2026-06-05 11:00:00');

-- ------------------------------------------------------------------
-- 8. 告警通知记录 — 样例数据
-- ------------------------------------------------------------------
INSERT INTO `alarm_notification` (`id`, `alarm_id`, `dispatch_rule_id`, `recipient_id`, `recipient_name`,
                                  `recipient_phone`, `channel`, `title`, `content`, `status`, `send_time`, `error_msg`,
                                  `create_time`)
VALUES
-- 通知龙泉寺橙色告警 → 发送给管理员
(1, 1, 3, 1, '管理员', '15888888888', 'SMS',
 '【橙色预警】龙泉寺崩塌隐患点 — 位移异常',
 '龙泉寺崩塌隐患点位移X轴当前值 42.35mm 超过橙色阈值 40.00mm，请立即处理。',
 2, '2026-06-09 08:15:05', NULL, '2026-06-09 08:15:00'),
(2, 1, 3, 1, '管理员', '15888888888', 'SYSTEM',
 '【橙色预警】龙泉寺崩塌隐患点 — 位移异常',
 '隐患点：龙泉寺崩塌隐患点（HP002）\n监测指标：X轴位移\n当前值：42.35mm\n阈值：40.00mm\n触发时间：2026-06-09 08:15:00\n累计触发：5次',
 2, '2026-06-09 08:15:01', NULL, '2026-06-09 08:15:00'),

-- 通知清溪乡黄色告警 → 系统通知给若依
(3, 2, 1, 2, '若依', '15666666666', 'SYSTEM',
 '【黄色预警】清溪乡泥石流隐患点 — 雨量异常',
 '隐患点：清溪乡泥石流隐患点（HP003）\n监测指标：小时雨量\n当前值：28.50mm\n阈值：25.00mm\n触发时间：2026-06-09 07:30:00',
 2, '2026-06-09 07:30:01', NULL, '2026-06-09 07:30:00'),

-- 通知顺发铁矿蓝色 → 仅系统通知
(4, 3, 1, 2, '若依', '15666666666', 'SYSTEM',
 '【蓝色预警】顺发铁矿边坡监测点 — 裂缝异常',
 '隐患点：顺发铁矿边坡监测点（HP005）\n监测指标：裂缝宽度\n当前值：2.50mm\n阈值：2.00mm\n触发时间：2026-06-08 14:00:00',
 2, '2026-06-08 14:00:01', NULL, '2026-06-08 14:00:00'),

-- 通知龙泉寺红色告警 → SMS+System 发送给管理员
(5, 4, 3, 1, '管理员', '15888888888', 'SMS',
 '【红色预警!!!】龙泉寺崩塌隐患点 — 位移严重超限',
 '龙泉寺位移Y轴28.70mm超过红色阈值25.00mm，单月累计位移超限，请立即响应！',
 2, '2026-06-09 09:00:05', NULL, '2026-06-09 09:00:00'),
(6, 4, 3, 1, '管理员', '15888888888', 'SYSTEM',
 '【红色预警!!!】龙泉寺崩塌隐患点 — 位移严重超限',
 '隐患点：龙泉寺崩塌隐患点（HP002）\n监测指标：Y轴位移\n当前值：28.70mm\n红色阈值：25.00mm\n触发时间：2026-06-09 09:00:00\n累计触发：3次\n\n请立即启动应急预案！',
 2, '2026-06-09 09:00:01', NULL, '2026-06-09 09:00:00'),

-- 通知工业园区蓝色 → 发送失败示例
(7, 5, 1, 2, '若依', '15666666666', 'SMS',
 '【蓝色预警】工业园区地面沉降点 — 雨量异常',
 '工业园区小时雨量12.00mm超过蓝色阈值10.00mm。',
 3, NULL, 'SMS网关连接超时，重试3次后放弃', '2026-06-07 16:00:00'),

-- 通知清溪乡综合策略橙色
(8, 6, 2, 1, '管理员', '15888888888', 'SYSTEM',
 '【橙色预警】清溪乡泥石流隐患点 — 综合策略触发',
 '综合策略【清溪乡暴雨泥石流综合预警】触发橙色预警。\n小时雨量：60.0mm\n日雨量：145mm\n土壤含水率：82.5%\n触发时间：2026-06-09 06:00:00',
 2, '2026-06-09 06:00:05', NULL, '2026-06-09 06:00:00'),
(9, 6, 2, 1, '管理员', '15888888888', 'SMS',
 '【橙色预警】清溪乡泥石流隐患点 — 综合策略触发',
 '清溪乡暴雨泥石流综合预警触发，小时雨量60mm+日雨量145mm，泥石流风险极高',
 2, '2026-06-09 06:00:10', NULL, '2026-06-09 06:00:00'),
(10, 6, 2, 2, '若依', '15666666666', 'SYSTEM',
 '【橙色预警】清溪乡泥石流隐患点 — 综合策略触发',
 '综合策略触发橙色预警，请协助跟进。',
 1, NULL, NULL, '2026-06-09 06:00:00');
