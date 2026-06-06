-- ============================================================
-- V2.0_04_monitor_model_cleanup.sql
-- 监测模型重构 — 废弃列清理
-- 执行状态: ✅ 已就绪（代码引用已于 2026-06-06 全部清理）
-- 执行前提:
--   1. V2.0_01 + V2.0_02 已执行（新增 monitor_category 表 + 数据迁移）
--   2. 已部署 feature/260606-module-split 分支代码（移除了所有 Java/Mapper XML 引用）
-- 废弃列:
--   1. monitor_type.device_type — 设备类型，已迁移到 device 表语义
--   2. sensor_attribute.indicator_type — 指标类型，改为通过 monitor_content 获取
--   3. sensor_attribute.indicator_type_name — 指标类型名称，同上
-- ============================================================

-- 1. 移除 monitor_type.device_type
ALTER TABLE `monitor_type` DROP COLUMN `device_type`;

-- 2. 移除 sensor_attribute 指标类型冗余列
ALTER TABLE `sensor_attribute`
    DROP COLUMN `indicator_type`,
    DROP COLUMN `indicator_type_name`;

-- 3. 验证清理
SELECT 'INFO: monitor_type.device_type 已移除' AS `status`
UNION ALL
SELECT 'INFO: sensor_attribute.indicator_type / indicator_type_name 已移除';
