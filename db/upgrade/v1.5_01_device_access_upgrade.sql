-- 设备接入闭环第一阶段数据库改造

ALTER TABLE `device`
    ADD COLUMN `sn` varchar(100) DEFAULT NULL COMMENT '设备SN' AFTER `code`,
    ADD COLUMN `device_type` tinyint DEFAULT NULL COMMENT '设备类型:0单参数,1多参数,2本地组网' AFTER `name`,
    ADD COLUMN `network_type` tinyint DEFAULT NULL COMMENT '网络类型:0蜂窝,1NB-Iot' AFTER `device_type`,
    ADD COLUMN `protocol_type` varchar(20) NOT NULL DEFAULT 'MQTT' COMMENT '接入协议:MQTT/HTTP/COAP' AFTER `network_type`,
    ADD COLUMN `register_source` varchar(20) NOT NULL DEFAULT 'MANUAL' COMMENT '注册来源:MANUAL/API/IMPORT' AFTER `protocol_type`,
    ADD COLUMN `vendor_name` varchar(200) DEFAULT NULL COMMENT '厂商名称' AFTER `register_source`,
    ADD COLUMN `auth_username` char(6) DEFAULT NULL COMMENT '设备接入用户名,固定6位' AFTER `vendor_name`,
    ADD COLUMN `auth_password` varchar(32) DEFAULT NULL COMMENT '设备接入密码,明文存储' AFTER `auth_username`,
    ADD COLUMN `auth_status` tinyint NOT NULL DEFAULT 1 COMMENT '账号状态:1有效,2禁用' AFTER `auth_password`,
    ADD COLUMN `registered_at` datetime DEFAULT NULL COMMENT '注册时间' AFTER `last_report_time`,
    ADD COLUMN `last_auth_time` datetime DEFAULT NULL COMMENT '最近鉴权时间' AFTER `registered_at`,
    ADD COLUMN `last_auth_ip` varchar(64) DEFAULT NULL COMMENT '最近鉴权IP' AFTER `last_auth_time`;

UPDATE `device`
SET `registered_at` = COALESCE(`registered_at`, `create_time`)
WHERE `registered_at` IS NULL;

ALTER TABLE `device`
    ADD UNIQUE KEY `uk_device_auth_username` (`auth_username`),
    ADD KEY `idx_device_register_source` (`register_source`),
    ADD KEY `idx_device_auth_status` (`auth_status`);

ALTER TABLE `device_sensor`
    ADD COLUMN `sensor_no` varchar(32) DEFAULT NULL COMMENT '传感器编号' AFTER `sensor_code`;

UPDATE `device_sensor`
SET `sensor_no` = COALESCE(`sensor_no`, `sensor_code`)
WHERE `sensor_no` IS NULL OR `sensor_no` = '';

ALTER TABLE `device_sensor`
    MODIFY COLUMN `sensor_no` varchar(32) NOT NULL COMMENT '传感器编号',
    ADD UNIQUE KEY `uk_device_sensor_no` (`device_id`, `sensor_no`);

CREATE TABLE `device_registration_log`
(
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `request_id` varchar(64) NOT NULL COMMENT '请求幂等ID',
    `register_code` varchar(64) DEFAULT NULL COMMENT '设备注册码',
    `register_source` varchar(20) NOT NULL COMMENT '注册来源',
    `vendor_name` varchar(200) DEFAULT NULL COMMENT '厂商名称',
    `device_id` bigint DEFAULT NULL COMMENT '设备ID',
    `sn` varchar(100) DEFAULT NULL COMMENT '设备SN',
    `result_status` varchar(20) NOT NULL COMMENT 'SUCCESS/FAIL',
    `failure_reason` varchar(500) DEFAULT NULL COMMENT '失败原因',
    `request_body` json DEFAULT NULL COMMENT '原始请求',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_device_register_request_id` (`request_id`),
    KEY `idx_device_register_device_id` (`device_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备注册日志';

CREATE TABLE `device_auth_log`
(
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `device_id` bigint NOT NULL COMMENT '设备ID',
    `auth_username` char(6) NOT NULL COMMENT '设备用户名',
    `auth_result` tinyint NOT NULL COMMENT '1成功,0失败',
    `client_id` varchar(128) DEFAULT NULL COMMENT 'MQTT客户端ID',
    `client_ip` varchar(64) DEFAULT NULL COMMENT '客户端IP',
    `failure_reason` varchar(255) DEFAULT NULL COMMENT '失败原因',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_device_auth_log_device` (`device_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备认证日志';

ALTER TABLE `sensor_attribute`
    ADD UNIQUE KEY `uk_sensor_attr_code` (`sensor_id`, `attr_code`);
