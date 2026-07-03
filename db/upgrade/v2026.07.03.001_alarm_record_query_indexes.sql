-- ============================================================
-- alarm_record 统计查询索引优化
-- 版本: v2026.07.03.001
-- 描述:
--   为 countTriggerByHazardPoint 的 GROUP BY 新增复合索引
--   为 countPendingByMonitorType 的 LEFT JOIN 新增 sensor_id 索引
-- 幂等: 判断索引存在性，可重复执行
-- ============================================================

-- ---------- 1. idx_record_hpname_status — GROUP BY hazard_point_name + WHERE status ----------
SET @idx := (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'alarm_record'
      AND index_name = 'idx_record_hpname_status');
SET @sql := IF(@idx = 0,
    'ALTER TABLE alarm_record ADD INDEX idx_record_hpname_status (hazard_point_name, status)',
    'SELECT "idx_record_hpname_status exists" AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------- 2. idx_record_sensor — LEFT JOIN ar.sensor_id = ds.id ----------
SET @idx := (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'alarm_record'
      AND index_name = 'idx_record_sensor');
SET @sql := IF(@idx = 0,
    'ALTER TABLE alarm_record ADD INDEX idx_record_sensor (sensor_id)',
    'SELECT "idx_record_sensor exists" AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
