-- ===============================================================
-- v1.2 传感器属性表新增 icon 字段（有条件执行）
-- ===============================================================
-- 执行时间: 2026-05-22
-- 执行原因: 支持传感器级别的图标配置，用于设备隐患点绑定功能
-- 注意: IF NOT EXISTS 兼容重复执行
-- ===============================================================

SET @column_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sensor_attribute'
      AND COLUMN_NAME = 'icon'
);

SET @sql = IF(@column_exists = 0,
    'ALTER TABLE `sensor_attribute` ADD COLUMN `icon` VARCHAR(500) DEFAULT NULL COMMENT ''图标路径'' AFTER `range_max`',
    'SELECT ''Column icon already exists, skipping''');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;