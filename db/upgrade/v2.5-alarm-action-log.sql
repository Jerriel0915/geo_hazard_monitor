-- ============================================================
-- 告警动作日志体系升级
-- 版本: v2.5
-- 描述:
--   1) 新建 alarm_record_trigger_detail (告警触发明细)
--   2) 重命名 alarm_record_log → alarm_record_action_log
--   3) 字段调整: disposal_type→action_type, from/to_status→from/to_value,
--      note→remarks, disposal_result→description, 新增 attachments
--   4) 数据迁移: 旧 disposal_type 映射为新 action_type 枚举
-- 幂等: 每步判断存在性，可重复执行
-- ============================================================

-- ---------- 1. 新建触发明细表 ----------
CREATE TABLE IF NOT EXISTS `alarm_record_trigger_detail` (
    `id`              bigint        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `alarm_record_id` bigint        NOT NULL                COMMENT '告警记录ID',
    `trigger_time`    datetime      NOT NULL                COMMENT '告警时间',
    `alarm_level`     tinyint       DEFAULT NULL            COMMENT '触发时等级 1-4',
    `alarm_type`      varchar(20)   DEFAULT NULL            COMMENT 'THRESHOLD/COMPREHENSIVE',
    `alarm_message`   varchar(500)  DEFAULT NULL            COMMENT '告警描述',
    `create_time`     datetime      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_trigger_aid` (`alarm_record_id`, `trigger_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='告警触发明细';

-- ---------- 2. 重命名 log → action_log ----------
SET @t_exists := (SELECT COUNT(*) FROM information_schema.tables
    WHERE table_schema = DATABASE() AND table_name = 'alarm_record_log');
SET @a_exists := (SELECT COUNT(*) FROM information_schema.tables
    WHERE table_schema = DATABASE() AND table_name = 'alarm_record_action_log');

-- 仅当旧表存在且新表不存在时才 rename (避免覆盖已有 action_log)
SET @sql := IF(@t_exists = 1 AND @a_exists = 0,
    'RENAME TABLE alarm_record_log TO alarm_record_action_log',
    'SELECT "alarm_record_log rename skipped" AS msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------- 3. 新增字段（幂等：判断列是否存在）----------

-- action_type
SET @col := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'alarm_record_action_log' AND column_name = 'action_type');
SET @sql := IF(@col = 0,
    'ALTER TABLE alarm_record_action_log ADD COLUMN action_type varchar(30) NULL COMMENT ''动作类型'' AFTER alarm_id',
    'SELECT "action_type exists" AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- from_value
SET @col := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'alarm_record_action_log' AND column_name = 'from_value');
SET @sql := IF(@col = 0,
    'ALTER TABLE alarm_record_action_log ADD COLUMN from_value varchar(20) NULL COMMENT ''变更前值''',
    'SELECT "from_value exists" AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- to_value
SET @col := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'alarm_record_action_log' AND column_name = 'to_value');
SET @sql := IF(@col = 0,
    'ALTER TABLE alarm_record_action_log ADD COLUMN to_value varchar(20) NULL COMMENT ''变更后值''',
    'SELECT "to_value exists" AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- remarks
SET @col := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'alarm_record_action_log' AND column_name = 'remarks');
SET @sql := IF(@col = 0,
    'ALTER TABLE alarm_record_action_log ADD COLUMN remarks varchar(500) NULL COMMENT ''备注''',
    'SELECT "remarks exists" AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- description
SET @col := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'alarm_record_action_log' AND column_name = 'description');
SET @sql := IF(@col = 0,
    'ALTER TABLE alarm_record_action_log ADD COLUMN description varchar(500) NULL COMMENT ''描述''',
    'SELECT "description exists" AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- attachments
SET @col := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'alarm_record_action_log' AND column_name = 'attachments');
SET @sql := IF(@col = 0,
    'ALTER TABLE alarm_record_action_log ADD COLUMN attachments varchar(1000) NULL COMMENT ''附件文件名(逗号分隔)''',
    'SELECT "attachments exists" AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------- 4. 数据迁移 (旧字段 → 新字段) ----------
-- 仅当 action_type 全为 NULL 时执行 (即首次迁移)
SET @pending := (SELECT COUNT(*) FROM alarm_record_action_log WHERE action_type IS NULL);

-- 4.1 from_status → from_value
SET @col := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'alarm_record_action_log' AND column_name = 'from_status');
SET @sql := IF(@col = 1 AND @pending > 0,
    'UPDATE alarm_record_action_log SET from_value = from_status WHERE from_value IS NULL AND from_status IS NOT NULL',
    'SELECT "from_value migration skipped" AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4.2 to_status → to_value
SET @col := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'alarm_record_action_log' AND column_name = 'to_status');
SET @sql := IF(@col = 1 AND @pending > 0,
    'UPDATE alarm_record_action_log SET to_value = to_status WHERE to_value IS NULL AND to_status IS NOT NULL',
    'SELECT "to_value migration skipped" AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4.3 note → remarks (text 收窄入 varchar(500)，超长截断)
SET @col := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'alarm_record_action_log' AND column_name = 'note');
SET @sql := IF(@col = 1 AND @pending > 0,
    'UPDATE alarm_record_action_log SET remarks = LEFT(note, 500) WHERE remarks IS NULL AND note IS NOT NULL',
    'SELECT "remarks migration skipped" AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4.4 disposal_result → description
SET @col := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'alarm_record_action_log' AND column_name = 'disposal_result');
SET @sql := IF(@col = 1 AND @pending > 0,
    'UPDATE alarm_record_action_log SET description = disposal_result WHERE description IS NULL AND disposal_result IS NOT NULL',
    'SELECT "description migration skipped" AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4.5 disposal_type → action_type (CASE WHEN 映射)
SET @col := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'alarm_record_action_log' AND column_name = 'disposal_type');
SET @sql := IF(@col = 1 AND @pending > 0,
    'UPDATE alarm_record_action_log SET action_type = CASE disposal_type COLLATE utf8mb4_0900_ai_ci WHEN ''开始处置'' THEN ''FEEDBACK'' WHEN ''已销警'' THEN ''DISPOSE_CLOSE'' WHEN ''标记误报'' THEN ''DISPOSE_FALSE_ALARM'' WHEN ''批量销警'' THEN ''DISPOSE_CLOSE'' WHEN ''批量标记误报'' THEN ''DISPOSE_FALSE_ALARM'' WHEN ''批量误报'' THEN ''DISPOSE_FALSE_ALARM'' WHEN ''批量标记处理中'' THEN ''FEEDBACK'' ELSE ''CREATE'' END WHERE action_type IS NULL',
    'SELECT "action_type migration skipped" AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------- 5. 重命名 alarm_id → alarm_record_id + 删除旧字段 ----------

SET @col := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'alarm_record_action_log' AND column_name = 'alarm_id');
SET @sql := IF(@col = 1,
    'ALTER TABLE alarm_record_action_log CHANGE COLUMN alarm_id alarm_record_id bigint NOT NULL COMMENT ''告警记录ID''',
    'SELECT "alarm_id rename skipped" AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'alarm_record_action_log' AND column_name = 'from_status');
SET @sql := IF(@col = 1, 'ALTER TABLE alarm_record_action_log DROP COLUMN from_status', 'SELECT "drop from_status skipped" AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'alarm_record_action_log' AND column_name = 'to_status');
SET @sql := IF(@col = 1, 'ALTER TABLE alarm_record_action_log DROP COLUMN to_status', 'SELECT "drop to_status skipped" AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'alarm_record_action_log' AND column_name = 'disposal_type');
SET @sql := IF(@col = 1, 'ALTER TABLE alarm_record_action_log DROP COLUMN disposal_type', 'SELECT "drop disposal_type skipped" AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'alarm_record_action_log' AND column_name = 'disposal_result');
SET @sql := IF(@col = 1, 'ALTER TABLE alarm_record_action_log DROP COLUMN disposal_result', 'SELECT "drop disposal_result skipped" AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'alarm_record_action_log' AND column_name = 'note');
SET @sql := IF(@col = 1, 'ALTER TABLE alarm_record_action_log DROP COLUMN note', 'SELECT "drop note skipped" AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------- 6. 更新表注释 ----------
ALTER TABLE `alarm_record_action_log` COMMENT = '告警动作日志';
