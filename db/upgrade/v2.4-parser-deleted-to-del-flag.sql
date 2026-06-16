-- ============================================================
-- zwei-iot-parser 模块 del_flag 统一化迁移
-- 版本: v2.4
-- 描述: 将 iot_data_parse_strategy 表的 deleted 列重命名为 del_flag，
--       与项目其他所有表的逻辑删除字段保持一致。
-- ============================================================

ALTER TABLE `iot_data_parse_strategy`
    CHANGE COLUMN `deleted` `del_flag` tinyint(1) NOT NULL DEFAULT '0' COMMENT '删除标记 0-正常 1-删除';
