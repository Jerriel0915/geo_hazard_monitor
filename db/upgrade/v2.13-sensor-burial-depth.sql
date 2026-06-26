-- ============================================================
-- 传感器新增埋深字段
-- 变更: device_sensor 表新增 burial_depth 列
-- 日期: 2026-06-25
-- 说明:
--   埋深以地面为0点，向下为正，向上为负，单位米。
--   默认为0，非必须字段。
-- ============================================================

ALTER TABLE `device_sensor`
    ADD COLUMN `burial_depth` decimal(10, 2) NOT NULL DEFAULT 0 COMMENT '埋深(米)，地面为0点，向下为正，向上为负'
    AFTER `monitor_type_name`;
