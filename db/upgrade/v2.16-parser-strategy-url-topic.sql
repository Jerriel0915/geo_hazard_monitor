-- ============================================================
-- zwei-iot-parser 策略表补充 server_url / topic 描述字段
-- 版本: v2.16
-- 描述:
--   为 iot_data_parse_strategy 新增 server_url / topic 两列，用于策略
--   描述展示（服务地址、订阅主题）。这两列仅作描述用途，不参与策略
--   匹配——匹配仍由 source_type + device_id 驱动。
-- 幂等: 判断列存在性，可重复执行
-- ============================================================

-- ---------- 1. server_url ----------
SET @col_exists := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'iot_data_parse_strategy' AND column_name = 'server_url');
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE iot_data_parse_strategy ADD COLUMN server_url varchar(255) DEFAULT NULL COMMENT ''MQTT服务地址(描述用)'' AFTER app_scope',
    'SELECT "server_url already exists" AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------- 2. topic ----------
SET @col_exists := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'iot_data_parse_strategy' AND column_name = 'topic');
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE iot_data_parse_strategy ADD COLUMN topic varchar(255) DEFAULT NULL COMMENT ''订阅主题(描述用)'' AFTER server_url',
    'SELECT "topic already exists" AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
