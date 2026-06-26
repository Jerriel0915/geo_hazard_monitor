-- ============================================================
-- 修复 iot_data_parse_strategy 表审计字段不一致
-- 版本: v2.15
-- 描述:
--   1) create_by / update_by 列类型从 bigint 改为 varchar(64)
--      （与 BaseEntity(String) 及全系统其他所有表对齐）
--   2) 修正列名 deleted → del_flag（与 Mapper XML + BaseEntity 对齐）
--   3) 补齐缺失的 create_time / update_time 默认值
-- 幂等: 每步判断列存在性，可重复执行
-- ============================================================

-- ---------- 1. create_by: bigint → varchar(64) ----------
SET @col_type := (SELECT DATA_TYPE FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'iot_data_parse_strategy' AND column_name = 'create_by');

SET @sql := IF(@col_type = 'bigint',
    'ALTER TABLE iot_data_parse_strategy MODIFY COLUMN create_by varchar(64) DEFAULT NULL COMMENT ''创建者''',
    'SELECT "create_by already varchar" AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------- 2. update_by: bigint → varchar(64) ----------
SET @col_type := (SELECT DATA_TYPE FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'iot_data_parse_strategy' AND column_name = 'update_by');

SET @sql := IF(@col_type = 'bigint',
    'ALTER TABLE iot_data_parse_strategy MODIFY COLUMN update_by varchar(64) DEFAULT NULL COMMENT ''更新者''',
    'SELECT "update_by already varchar" AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------- 3. 重命名 deleted → del_flag（与 Mapper XML 对齐）----------
SET @col_deleted := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'iot_data_parse_strategy' AND column_name = 'deleted');
SET @col_del_flag := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'iot_data_parse_strategy' AND column_name = 'del_flag');

SET @sql := IF(@col_deleted = 1 AND @col_del_flag = 0,
    'ALTER TABLE iot_data_parse_strategy CHANGE COLUMN deleted del_flag tinyint(1) NOT NULL DEFAULT 0 COMMENT ''删除标记: 0-正常, 1-删除''',
    'SELECT "del_flag rename skipped" AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------- 4. create_time / update_time 默认值修正 ----------
ALTER TABLE iot_data_parse_strategy
    MODIFY COLUMN create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';
