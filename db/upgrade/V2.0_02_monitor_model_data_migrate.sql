-- ============================================================
-- V2.0_02_monitor_model_data_migrate.sql
-- 监测模型重构 — 数据迁移
-- 执行前提: V2.0_01_monitor_model_refactor.sql 已执行
-- ============================================================

-- Step 1: 预置 8 大监测类别
INSERT INTO `monitor_category` (`code`, `name`, `icon`, `sort_order`, `status`, `create_by`) VALUES
('RAINFALL',      '雨量',   '/jc-icon/green/wj_green.png',     1, 1, 'system'),
('DISPLACEMENT',  '位移',   '/jc-icon/green/jsd_green.png',    2, 1, 'system'),
('TEMPERATURE',   '温湿度', '/jc-icon/green/ky_green.png',     3, 1, 'system'),
('WATER_LEVEL',   '水位',   '/jc-icon/green/sg_green.png',     4, 1, 'system'),
('CRACK',         '裂缝',   '/jc-icon/green/jsd_green.png',    5, 1, 'system'),
('INCLINATION',   '倾斜',   '/jc-icon/green/nw_green.png',     6, 1, 'system'),
('SOIL_TEMP',     '地温',   '/jc-icon/green/gnss_green.png',   7, 1, 'system'),
('SOIL_MOISTURE', '含水率', '/jc-icon/green/lf_green.png',     8, 1, 'system');

-- Step 2: 回填 monitor_type.category_id
-- 基于现有 monitor_type 编码映射到对应 monitor_category
UPDATE `monitor_type` SET `category_id` = (SELECT mc.`id` FROM `monitor_category` mc WHERE mc.`code` = 'RAINFALL'      LIMIT 1) WHERE `code` = 'JCLX001' AND `del_flag` = 0 AND `category_id` IS NULL;
UPDATE `monitor_type` SET `category_id` = (SELECT mc.`id` FROM `monitor_category` mc WHERE mc.`code` = 'DISPLACEMENT'  LIMIT 1) WHERE `code` = 'JCLX002' AND `del_flag` = 0 AND `category_id` IS NULL;
UPDATE `monitor_type` SET `category_id` = (SELECT mc.`id` FROM `monitor_category` mc WHERE mc.`code` = 'TEMPERATURE'   LIMIT 1) WHERE `code` = 'JCLX003' AND `del_flag` = 0 AND `category_id` IS NULL;
UPDATE `monitor_type` SET `category_id` = (SELECT mc.`id` FROM `monitor_category` mc WHERE mc.`code` = 'WATER_LEVEL'   LIMIT 1) WHERE `code` = 'JCLX004' AND `del_flag` = 0 AND `category_id` IS NULL;
UPDATE `monitor_type` SET `category_id` = (SELECT mc.`id` FROM `monitor_category` mc WHERE mc.`code` = 'CRACK'         LIMIT 1) WHERE `code` = 'JCLX005' AND `del_flag` = 0 AND `category_id` IS NULL;
UPDATE `monitor_type` SET `category_id` = (SELECT mc.`id` FROM `monitor_category` mc WHERE mc.`code` = 'INCLINATION'   LIMIT 1) WHERE `code` = 'JCLX006' AND `del_flag` = 0 AND `category_id` IS NULL;
UPDATE `monitor_type` SET `category_id` = (SELECT mc.`id` FROM `monitor_category` mc WHERE mc.`code` = 'SOIL_TEMP'     LIMIT 1) WHERE `code` = 'JCLX007' AND `del_flag` = 0 AND `category_id` IS NULL;
UPDATE `monitor_type` SET `category_id` = (SELECT mc.`id` FROM `monitor_category` mc WHERE mc.`code` = 'SOIL_MOISTURE' LIMIT 1) WHERE `code` = 'JCLX008' AND `del_flag` = 0 AND `category_id` IS NULL;

-- 处理自定义/不在 8 类中的监测类型（如 JCXL456），归入"位移"大类
UPDATE `monitor_type` SET `category_id` = (SELECT mc.`id` FROM `monitor_category` mc WHERE mc.`code` = 'DISPLACEMENT' LIMIT 1) WHERE `category_id` IS NULL AND `del_flag` = 0;

-- Step 3: 回填 sensor_attribute.monitor_content_id
-- 通过 attr_code 与 monitor_content.code 匹配
UPDATE `sensor_attribute` sa
    INNER JOIN `monitor_content` mc ON sa.`attr_code` = mc.`code`
SET sa.`monitor_content_id` = mc.`id`
WHERE sa.`monitor_content_id` IS NULL;

-- Step 4: 验证迁移结果
SELECT 'INFO: monitor_type 中未匹配 category_id 的记录数 (应为 0):' AS `check`, COUNT(*) AS `cnt`
FROM `monitor_type` WHERE `category_id` IS NULL AND `del_flag` = 0
UNION ALL
SELECT 'INFO: sensor_attribute 中未匹配 monitor_content_id 的记录数:', COUNT(*)
FROM `sensor_attribute` WHERE `monitor_content_id` IS NULL
UNION ALL
SELECT 'INFO: monitor_category 记录数 (应为 8):', COUNT(*) FROM `monitor_category`;
