-- ============================================
-- upgrade_v1.7_device_coordinates.sql
-- 设备与视频设备扩展地理坐标字段
-- 日期：2026-06-03
-- ============================================

-- 1. device 表
ALTER TABLE `device`
    ADD COLUMN `longitude` double DEFAULT NULL COMMENT '经度' AFTER `last_auth_ip`,
    ADD COLUMN `latitude`  double DEFAULT NULL COMMENT '纬度' AFTER `longitude`;

-- 2. video_device 表
ALTER TABLE `video_device`
    ADD COLUMN `longitude` double DEFAULT NULL COMMENT '经度' AFTER `install_time`,
    ADD COLUMN `latitude`  double DEFAULT NULL COMMENT '纬度' AFTER `longitude`;
