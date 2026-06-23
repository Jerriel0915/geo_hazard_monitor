-- ============================================================
-- 统一经纬度精度为 decimal(10,6)
-- 变更: device / video_device 表 longitude, latitude 列从 double 改为 decimal(10,6)
-- 日期: 2026-06-23
-- ============================================================

ALTER TABLE `device`
    MODIFY COLUMN `longitude` decimal(10, 6) DEFAULT NULL COMMENT '经度',
    MODIFY COLUMN `latitude` decimal(10, 6) DEFAULT NULL COMMENT '纬度';

ALTER TABLE `video_device`
    MODIFY COLUMN `longitude` decimal(10, 6) DEFAULT NULL COMMENT '经度',
    MODIFY COLUMN `latitude` decimal(10, 6) DEFAULT NULL COMMENT '纬度';
