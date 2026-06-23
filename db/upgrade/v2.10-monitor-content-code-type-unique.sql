-- ============================================================
-- 监测内容编码约束：全局唯一 → 监测类型内唯一
-- 变更: monitor_content 表 code 唯一约束从全局唯一改为 (monitor_type_id, code) 复合唯一
-- 日期: 2026-06-23
-- 说明:
--   原约束 uk_monitor_content_code (code) 全局唯一，导致不同监测类型无法使用相同内容编码。
--   新约束 uk_monitor_content_code (monitor_type_id, code) 允许跨类型重复，仅在同一类型内保持唯一。
--   下游告警引擎使用 monitor_content_id（PK）匹配，不按 code 关联；传感器属性同步按 monitorTypeId 过滤，
--   本次变更对告警和时序落库均无影响。
-- 数据安全:
--   原全局唯一约束保证不存在重复 (monitor_type_id, code) 对，DROP + ADD 无数据冲突风险。
-- ============================================================

ALTER TABLE `monitor_content`
    DROP INDEX `uk_monitor_content_code`;

ALTER TABLE `monitor_content`
    ADD UNIQUE KEY `uk_monitor_content_code` (`monitor_type_id`, `code`);

ALTER TABLE `monitor_content`
    MODIFY COLUMN `code` varchar(100) NOT NULL
        COMMENT '监测内容编码（监测类型内唯一）';
