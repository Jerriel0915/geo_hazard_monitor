-- v2.1: Add product table for TSL thing model
-- Run: mysql -u root -p geo_hazard_monitor < db/upgrade/v2.1_add_product_tsl.sql

CREATE TABLE IF NOT EXISTS `product` (
  `id`            bigint       NOT NULL AUTO_INCREMENT,
  `product_key`   varchar(64)  NOT NULL COMMENT '产品唯一标识，由device.code哈希生成',
  `device_id`     bigint       NOT NULL COMMENT '关联设备ID，当前1:1',
  `tsl_json`      json         NOT NULL COMMENT '完整TSL JSON（properties/events/services）',
  `tsl_version`   varchar(32)  DEFAULT '1.0' COMMENT 'TSL版本号',
  `create_by`     varchar(64)  DEFAULT NULL,
  `create_time`   datetime     DEFAULT CURRENT_TIMESTAMP,
  `update_by`     varchar(64)  DEFAULT NULL,
  `update_time`   datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag`      tinyint      DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_product_key` (`product_key`),
  UNIQUE KEY `uk_device_id` (`device_id`),
  KEY `idx_product_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='产品物模型表';
