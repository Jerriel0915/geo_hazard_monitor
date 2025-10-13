-- 创建产品变更日志表
CREATE TABLE `zw_iot_product_change_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键id',
  `product_id` bigint(20) NOT NULL COMMENT '产品ID',
  `product_key` varchar(64) NOT NULL COMMENT '产品密钥',
  `operation_type` tinyint(4) NOT NULL DEFAULT 0 COMMENT '操作类型：0-新增物模型 1-更新物模型',
  `status` tinyint(4) NOT NULL DEFAULT 0 COMMENT '执行状态：0-待执行 1-执行成功 2-执行失败',
  `error_message` varchar(500) DEFAULT NULL COMMENT '错误信息',
  `tsl_content` longtext NOT NULL COMMENT '物模型内容',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `execute_time` datetime DEFAULT NULL COMMENT '执行时间',
  PRIMARY KEY (`id`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_product_key` (`product_key`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='产品变更日志表';