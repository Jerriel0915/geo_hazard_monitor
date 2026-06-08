-- ============================================================
-- V2.0_07_device_status_log_expand.sql
-- 扩展 device_status_log 表，支持设备维修状态操作记录
-- ============================================================

ALTER TABLE `device_status_log`
    ADD COLUMN `operator_name`  varchar(64)  DEFAULT NULL COMMENT '操作人姓名' AFTER `status_text`,
    ADD COLUMN `operator_phone` varchar(20)  DEFAULT NULL COMMENT '操作人电话' AFTER `operator_name`,
    ADD COLUMN `operation_date` datetime     DEFAULT NULL COMMENT '操作日期' AFTER `operator_phone`,
    ADD COLUMN `description`    varchar(500) DEFAULT NULL COMMENT '操作描述' AFTER `operation_date`;
