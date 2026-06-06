-- ============================================================
-- V2.0_01_monitor_model_refactor.sql
-- 监测模型重构 — 增量 DDL
-- 执行前提: geo_hazard_monitor v1.9 schema
-- 变更说明:
--   1. 新增 monitor_category 监测大类表（平级 8 类）
--   2. monitor_type 增加 category_id 外键
--   3. sensor_attribute 增加 monitor_content_id 外键
-- ============================================================

-- Step 1: 创建监测大类表
CREATE TABLE `monitor_category`
(
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `code`        varchar(100) NOT NULL COMMENT '大类编码',
    `name`        varchar(200) NOT NULL COMMENT '大类名称',
    `icon`        varchar(200) DEFAULT NULL COMMENT '图标路径',
    `sort_order`  int          DEFAULT '0' COMMENT '排序号',
    `status`      tinyint      DEFAULT '1' COMMENT '状态: 0-禁用, 1-启用',
    `create_by`   varchar(64)  DEFAULT NULL COMMENT '创建者',
    `create_time` datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`   varchar(64)  DEFAULT NULL COMMENT '更新者',
    `update_time` datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `del_flag`    tinyint      DEFAULT '0' COMMENT '删除标记: 0-正常, 1-删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_monitor_category_code` (`code`),
    KEY `idx_monitor_category_status` (`status`),
    KEY `idx_monitor_category_del_flag` (`del_flag`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='监测大类表';

-- Step 2: monitor_type 新增 category_id 列
ALTER TABLE `monitor_type`
    ADD COLUMN `category_id` bigint DEFAULT NULL COMMENT '监测大类ID' AFTER `code`,
    ADD KEY `idx_monitor_type_category` (`category_id`);

-- Step 3: sensor_attribute 新增 monitor_content_id 列
ALTER TABLE `sensor_attribute`
    ADD COLUMN `monitor_content_id` bigint DEFAULT NULL COMMENT '监测内容ID' AFTER `sensor_id`,
    ADD KEY `idx_sensor_attr_content` (`monitor_content_id`);
