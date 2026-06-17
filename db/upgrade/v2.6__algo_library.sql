-- =====================================================================
-- 算法库功能 V20260617
-- 新增表：algo_info（算法信息）、algo_version（算法版本）
-- =====================================================================

-- ----------------------------
-- 1. 算法信息表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `algo_info` (
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `code`        varchar(64)  NOT NULL COMMENT '算法编码（全局唯一，用于程序引用）',
    `name`        varchar(128) NOT NULL COMMENT '算法名称',
    `description` varchar(500) DEFAULT NULL COMMENT '算法描述',
    `status`      tinyint      DEFAULT '1' COMMENT '状态: 0-停用, 1-启用',
    `del_flag`    tinyint      DEFAULT '0' COMMENT '删除标记: 0-正常, 1-删除',
    `create_by`   varchar(64)  DEFAULT NULL COMMENT '创建者',
    `create_time` datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`   varchar(64)  DEFAULT NULL COMMENT '更新者',
    `update_time` datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark`      varchar(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`, `del_flag`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='算法信息表';

-- ----------------------------
-- 2. 算法版本表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `algo_version` (
    `id`             bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `algo_id`        bigint       NOT NULL COMMENT '算法ID（关联 algo_info.id）',
    `version_no`     varchar(64)  NOT NULL COMMENT '版本号（用户输入，同一算法下唯一）',
    `file_name`      varchar(255) NOT NULL COMMENT '存储文件名（相对路径）',
    `original_name`  varchar(255) NOT NULL COMMENT '原始文件名',
    `file_size`      bigint       DEFAULT '0' COMMENT '文件大小（字节）',
    `sha256`         varchar(64)  DEFAULT NULL COMMENT 'SHA256 摘要',
    `del_flag`       tinyint      DEFAULT '0' COMMENT '删除标记: 0-正常, 1-删除',
    `create_by`      varchar(64)  DEFAULT NULL COMMENT '创建者',
    `create_time`    datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`      varchar(64)  DEFAULT NULL COMMENT '更新者',
    `update_time`    datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark`         varchar(500) DEFAULT NULL COMMENT '版本说明',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_algo_version` (`algo_id`, `version_no`, `del_flag`),
    KEY `idx_algo_id` (`algo_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='算法版本表';
