-- ============================================================
-- V2.0_03_monitor_model_rollback.sql
-- 监测模型重构 — 回滚
-- 执行前提: V2.0_01 + V2.0_02 已执行
-- 说明: 仅删除新增列和表，原有数据不受影响
-- ============================================================

-- 1. 移除 monitor_type 新增列
ALTER TABLE `monitor_type` DROP KEY `idx_monitor_type_category`;
ALTER TABLE `monitor_type` DROP COLUMN `category_id`;

-- 2. 移除 sensor_attribute 新增列
ALTER TABLE `sensor_attribute` DROP KEY `idx_sensor_attr_content`;
ALTER TABLE `sensor_attribute` DROP COLUMN `monitor_content_id`;

-- 3. 移除 monitor_category 表
DROP TABLE IF EXISTS `monitor_category`;

-- 4. 验证回滚
SELECT 'INFO: monitor_type.category_id 已移除' AS `status`
UNION ALL
SELECT 'INFO: sensor_attribute.monitor_content_id 已移除'
UNION ALL
SELECT 'INFO: monitor_category 表已删除';
