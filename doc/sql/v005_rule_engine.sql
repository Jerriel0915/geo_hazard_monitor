-- 规则引擎相关表

-- ----------------------------
-- 1. IoT规则表
-- ----------------------------
DROP TABLE IF EXISTS `zw_iot_rule`;
CREATE TABLE `zw_iot_rule`
(
    `rule_id`         bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '规则ID',
    `rule_name`       varchar(100) NOT NULL COMMENT '规则名称',
    `product_key`     varchar(64)  DEFAULT NULL COMMENT '产品标识',
    `device_key`      varchar(64)  DEFAULT NULL COMMENT '设备标识(可选)',
    `trigger_type`    varchar(20)  DEFAULT 'property' COMMENT '触发类型(property:属性上报, event:事件上报)',
    `rule_expression` text COMMENT '规则表达式(Aviator脚本)',
    `status`          char(1)      DEFAULT '1' COMMENT '状态（0停用 1启用）',
    `priority`        int(11)      DEFAULT 0 COMMENT '优先级',
    `remark`          varchar(500) DEFAULT NULL COMMENT '备注',
    `create_by`       varchar(64)  DEFAULT '' COMMENT '创建者',
    `create_time`     datetime     DEFAULT NOW() COMMENT '创建时间',
    `update_by`       varchar(64)  DEFAULT '' COMMENT '更新者',
    `update_time`     datetime     DEFAULT NOW() COMMENT '更新时间',
    PRIMARY KEY (`rule_id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 100
  DEFAULT CHARSET = utf8mb4 COMMENT ='IoT规则表';

-- ----------------------------
-- 2. IoT规则动作表
-- ----------------------------
DROP TABLE IF EXISTS `zw_iot_rule_action`;
CREATE TABLE `zw_iot_rule_action`
(
    `action_id`     bigint(20)  NOT NULL AUTO_INCREMENT COMMENT '动作ID',
    `rule_id`       bigint(20)  NOT NULL COMMENT '规则ID',
    `action_type`   varchar(50) NOT NULL COMMENT '动作类型(alert, device, kafka, etc)',
    `action_params` varchar(2000) DEFAULT NULL COMMENT '动作参数(JSON格式)',
    `create_time`   datetime      DEFAULT NOW() COMMENT '创建时间',
    PRIMARY KEY (`action_id`),
    KEY `idx_rule_id` (`rule_id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 100
  DEFAULT CHARSET = utf8mb4 COMMENT ='IoT规则动作表';
