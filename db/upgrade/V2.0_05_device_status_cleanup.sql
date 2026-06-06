-- ============================================================
-- V2.0_05_device_status_cleanup.sql
-- 设备状态模型统一 — 数据迁移 + 废弃列标记
-- 执行前提: V2.0_01~V2.0_04 已执行（可选）
--
-- 变更说明:
--   1. device.status: 3=离线 → 3=停用（离线改为系统自动检测，不再作为人工状态）
--   2. 迁移现有 status=3 的记录为 status=2(故障)
--   3. device.run_status 保留但不建议再写入（由 device_online_status 表替代）
-- ============================================================

-- Step 1: 迁移 status=3(离线) 的设备为 2(故障)
UPDATE `device` SET `status` = 2 WHERE `status` = 3 AND `del_flag` = 0;

-- Step 2: 验证迁移
SELECT 'INFO: 迁移后 status=3 的记录数 (应为 0):' AS `check`, COUNT(*) FROM `device` WHERE `status` = 3 AND `del_flag` = 0;
SELECT 'INFO: 当前设备状态分布:' AS `check`, `status`, COUNT(*) AS cnt FROM `device` WHERE `del_flag` = 0 GROUP BY `status`;
