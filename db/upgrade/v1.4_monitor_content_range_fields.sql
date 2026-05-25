-- 监测内容表增加量程字段
ALTER TABLE `monitor_content`
    ADD COLUMN `range_min` decimal(12, 2) DEFAULT NULL COMMENT '最小值范围' AFTER `icon`,
    ADD COLUMN `range_max` decimal(12, 2) DEFAULT NULL COMMENT '最大值范围' AFTER `range_min`;

-- 初始化历史监测内容量程范围
UPDATE `monitor_content`
SET `range_min` = 0.00,
    `range_max` = 500.00
WHERE `code` = 'rainfall_hour'
  AND `del_flag` = 0;

UPDATE `monitor_content`
SET `range_min` = 0.00,
    `range_max` = 1000.00
WHERE `code` = 'rainfall_day'
  AND `del_flag` = 0;

UPDATE `monitor_content`
SET `range_min` = -1000.00,
    `range_max` = 1000.00
WHERE `code` IN ('displacement_x', 'displacement_y', 'displacement_z')
  AND `del_flag` = 0;

UPDATE `monitor_content`
SET `range_min` = -50.00,
    `range_max` = 100.00
WHERE `code` IN ('temperature', 'soil_temp_10cm')
  AND `del_flag` = 0;

UPDATE `monitor_content`
SET `range_min` = 0.00,
    `range_max` = 100.00
WHERE `code` IN ('humidity', 'water_level', 'soil_moisture')
  AND `del_flag` = 0;

UPDATE `monitor_content`
SET `range_min` = 0.00,
    `range_max` = 50.00
WHERE `code` = 'crack_width'
  AND `del_flag` = 0;

UPDATE `monitor_content`
SET `range_min` = -90.00,
    `range_max` = 90.00
WHERE `code` IN ('inclination_x', 'inclination_y')
  AND `del_flag` = 0;
