-- =====================================================================
-- v2.17 异常报文日志 (mqtt_exception_log)
-- 记录已通过 MQTT 认证但解析/报送失败的报文，用于服务状态页"异常报文"子页展示。
-- =====================================================================

CREATE TABLE IF NOT EXISTS `mqtt_exception_log`
(
    `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `receive_time`  datetime(3)  NOT NULL              COMMENT '报文接收时间(毫秒精度)',
    `client_id`     varchar(128) DEFAULT NULL           COMMENT 'MQTT clientId',
    `username`      varchar(64)  DEFAULT NULL           COMMENT '设备认证用户名',
    `device_id`     bigint       DEFAULT NULL           COMMENT '关联设备ID',
    `topic`         varchar(255) NOT NULL               COMMENT '发布主题',
    `payload`       varchar(500) DEFAULT NULL           COMMENT '报文内容(截断500字符)',
    `payload_size`  int          DEFAULT 0              COMMENT '原始报文字节数',
    `reject_stage`  varchar(32)  NOT NULL               COMMENT '失败阶段: TOPIC/FORMAT/STRATEGY/PARSE/UNKNOWN',
    `reject_reason` varchar(500) NOT NULL               COMMENT '报错内容(异常消息)',
    `error_stack`   text         DEFAULT NULL           COMMENT '异常堆栈(截断2000字符)',
    `create_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
    PRIMARY KEY (`id`),
    KEY `idx_exc_create_time` (`create_time`),
    KEY `idx_exc_client_id` (`client_id`),
    KEY `idx_exc_topic` (`topic`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='MQTT异常报文日志';

-- =====================================================================
-- 保留期配置 (sys_config) — 默认 60 天，可在系统设置中调整
-- =====================================================================
INSERT INTO `sys_config` (`config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `remark`)
VALUES ('异常报文保留天数', 'mqtt.exception.retention-days', '60', 'Y', 'system', NOW(), '异常报文日志保留天数，默认60')
ON DUPLICATE KEY UPDATE `config_value` = VALUES(`config_value`);

INSERT INTO `sys_config` (`config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `remark`)
VALUES ('异常报文清理开关', 'mqtt.exception.cleanup.enabled', 'true', 'Y', 'system', NOW(), '是否启用异常报文定时清理')
ON DUPLICATE KEY UPDATE `config_value` = VALUES(`config_value`);

INSERT INTO `sys_config` (`config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `remark`)
VALUES ('异常报文清理Cron', 'mqtt.exception.cleanup.cron', '0 0 3 * * ?', 'Y', 'system', NOW(), '异常报文清理 Quartz cron 表达式')
ON DUPLICATE KEY UPDATE `config_value` = VALUES(`config_value`);

-- =====================================================================
-- Quartz 定时任务 — 每天凌晨 3 点清理过期异常报文
-- job_id=101 紧接日志清理任务(100)；复合主键 (job_id, job_name, job_group) 保证唯一
-- 重复执行时更新全部字段（cron/invoke_target/status 等），避免配置漂移
-- =====================================================================
INSERT INTO `sys_job` (`job_id`, `job_name`, `job_group`, `invoke_target`, `cron_expression`,
                      `misfire_policy`, `concurrent`, `status`, `create_by`, `create_time`, `remark`)
VALUES (101, '异常报文自动清理', 'DEFAULT', 'exceptionLogCleanupTask.cleanExpiredLogs()', '0 0 3 * * ?',
        '3', '1', '0', 'admin', NOW(), '按保留天数清理 mqtt_exception_log 过期记录')
ON DUPLICATE KEY UPDATE
    `invoke_target`   = VALUES(`invoke_target`),
    `cron_expression` = VALUES(`cron_expression`),
    `misfire_policy`  = VALUES(`misfire_policy`),
    `concurrent`      = VALUES(`concurrent`),
    `status`          = VALUES(`status`),
    `remark`          = VALUES(`remark`);
