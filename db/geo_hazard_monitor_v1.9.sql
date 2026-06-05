-- MySQL dump 10.13  Distrib 8.0.42, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: geo_hazard_monitor
-- ------------------------------------------------------
-- Server version	8.0.42

/*!40101 SET @OLD_CHARACTER_SET_CLIENT = @@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS = @@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION = @@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE = @@TIME_ZONE */;
/*!40103 SET TIME_ZONE = '+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS = @@UNIQUE_CHECKS, UNIQUE_CHECKS = 0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS = @@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS = 0 */;
/*!40101 SET @OLD_SQL_MODE = @@SQL_MODE, SQL_MODE = 'NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES = @@SQL_NOTES, SQL_NOTES = 0 */;

--
-- Table structure for table `alarm_criteria`
--

DROP TABLE IF EXISTS `alarm_criteria`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `alarm_criteria`
(
    `id`                   bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `hazard_point_id`      bigint       NOT NULL COMMENT '隐患点ID',
    `name`                 varchar(200) NOT NULL COMMENT '判据名称',
    `device_id`            bigint       DEFAULT NULL COMMENT '设备ID',
    `device_name`          varchar(200) DEFAULT NULL COMMENT '设备名称',
    `monitor_type_id`      bigint       DEFAULT NULL COMMENT '监测类型ID',
    `monitor_type_name`    varchar(200) DEFAULT NULL COMMENT '监测类型名称',
    `monitor_content_code` varchar(100) DEFAULT NULL COMMENT '监测内容编码',
    `monitor_content_name` varchar(200) DEFAULT NULL COMMENT '监测内容名称',
    `blue_expression`      varchar(500) DEFAULT NULL COMMENT '蓝色预警表达式',
    `blue_description`     varchar(500) DEFAULT NULL COMMENT '蓝色预警描述',
    `yellow_expression`    varchar(500) DEFAULT NULL COMMENT '黄色预警表达式',
    `yellow_description`   varchar(500) DEFAULT NULL COMMENT '黄色预警描述',
    `orange_expression`    varchar(500) DEFAULT NULL COMMENT '橙色预警表达式',
    `orange_description`   varchar(500) DEFAULT NULL COMMENT '橙色预警描述',
    `red_expression`       varchar(500) DEFAULT NULL COMMENT '红色预警表达式',
    `red_description`      varchar(500) DEFAULT NULL COMMENT '红色预警描述',
    `is_enabled`           tinyint      DEFAULT '1' COMMENT '是否启用: 0-禁用, 1-启用',
    `create_by`            varchar(64)  DEFAULT NULL COMMENT '创建者',
    `create_time`          datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`            varchar(64)  DEFAULT NULL COMMENT '更新者',
    `update_time`          datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `del_flag`             tinyint      DEFAULT '0' COMMENT '删除标记: 0-正常, 1-删除',
    PRIMARY KEY (`id`),
    KEY `idx_alarm_criteria_hp_id` (`hazard_point_id`),
    KEY `idx_alarm_criteria_device_id` (`device_id`),
    KEY `idx_alarm_criteria_enabled` (`is_enabled`),
    KEY `idx_alarm_criteria_del_flag` (`del_flag`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='告警判据表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `alarm_criteria`
--

LOCK TABLES `alarm_criteria` WRITE;
/*!40000 ALTER TABLE `alarm_criteria`
    DISABLE KEYS */;
/*!40000 ALTER TABLE `alarm_criteria`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `alarm_dispatch_rule`
--

DROP TABLE IF EXISTS `alarm_dispatch_rule`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `alarm_dispatch_rule`
(
    `id`              bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `hazard_point_id` bigint       NOT NULL COMMENT '隐患点ID',
    `name`            varchar(200) NOT NULL COMMENT '规则名称',
    `type`            tinyint      DEFAULT '1' COMMENT '类型: 1-告警分发, 2-状态通知',
    `alarm_level`     varchar(200) DEFAULT NULL COMMENT '告警等级列表',
    `recipient_ids`   varchar(500) DEFAULT NULL COMMENT '接收人ID或设备ID列表',
    `channel`         varchar(200) DEFAULT NULL COMMENT '通知渠道: SYSTEM,SMS,EMAIL',
    `is_enabled`      tinyint      DEFAULT '1' COMMENT '是否启用: 0-禁用, 1-启用',
    `time_setting`    varchar(50)  DEFAULT NULL COMMENT '时间频率设置',
    `time_value`      varchar(100) DEFAULT NULL COMMENT '时间值列表：逗号分隔',
    `create_by`       varchar(64)  DEFAULT NULL COMMENT '创建者',
    `create_time`     datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`       varchar(64)  DEFAULT NULL COMMENT '更新者',
    `update_time`     datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `del_flag`        tinyint      DEFAULT '0' COMMENT '删除标记: 0-正常, 1-删除',
    PRIMARY KEY (`id`),
    KEY `idx_alarm_dispatch_hp_id` (`hazard_point_id`),
    KEY `idx_alarm_dispatch_type` (`type`),
    KEY `idx_alarm_dispatch_enabled` (`is_enabled`),
    KEY `idx_alarm_dispatch_del_flag` (`del_flag`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='告警分发规则表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `alarm_dispatch_rule`
--

LOCK TABLES `alarm_dispatch_rule` WRITE;
/*!40000 ALTER TABLE `alarm_dispatch_rule`
    DISABLE KEYS */;
/*!40000 ALTER TABLE `alarm_dispatch_rule`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `alarm_notification`
--

DROP TABLE IF EXISTS `alarm_notification`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `alarm_notification`
(
    `id`              bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `alarm_id`        bigint NOT NULL COMMENT '告警记录ID',
    `recipient_id`    bigint NOT NULL COMMENT '接收人ID',
    `recipient_name`  varchar(100) DEFAULT NULL COMMENT '接收人名称',
    `recipient_phone` varchar(20)  DEFAULT NULL COMMENT '接收人电话',
    `channel`         varchar(50)  DEFAULT NULL COMMENT '通知渠道',
    `status`          tinyint      DEFAULT '1' COMMENT '状态: 1-待发送, 2-已发送, 3-发送失败',
    `send_time`       datetime     DEFAULT NULL COMMENT '发送时间',
    `error_msg`       varchar(500) DEFAULT NULL COMMENT '错误信息',
    `create_by`       varchar(64)  DEFAULT NULL COMMENT '创建者',
    `create_time`     datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_alarm_notification_alarm_id` (`alarm_id`),
    KEY `idx_alarm_notification_recipient_id` (`recipient_id`),
    KEY `idx_alarm_notification_status` (`status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='告警通知记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `alarm_notification`
--

LOCK TABLES `alarm_notification` WRITE;
/*!40000 ALTER TABLE `alarm_notification`
    DISABLE KEYS */;
/*!40000 ALTER TABLE `alarm_notification`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `alarm_record`
--

DROP TABLE IF EXISTS `alarm_record`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `alarm_record`
(
    `id`                bigint  NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `hazard_point_id`   bigint  NOT NULL COMMENT '隐患点ID',
    `hazard_point_code` varchar(100)   DEFAULT NULL COMMENT '隐患点编号',
    `hazard_point_name` varchar(200)   DEFAULT NULL COMMENT '隐患点名称',
    `alarm_level`       tinyint NOT NULL COMMENT '告警等级: 1-蓝色, 2-黄色, 3-橙色, 4-红色',
    `alarm_level_text`  varchar(50)    DEFAULT NULL COMMENT '告警等级文本',
    `alarm_type`        varchar(100)   DEFAULT NULL COMMENT '告警类型: 1-阈值告警, 2-模型告警, 3-综合告警, 4-其他告警',
    `alarm_message`     text COMMENT '告警消息',
    `device_id`         bigint         DEFAULT NULL COMMENT '设备ID',
    `sensor_id`         bigint         DEFAULT NULL COMMENT '传感器ID',
    `monitor_type_id`   bigint         DEFAULT NULL COMMENT '监测类型ID',
    `current_value`     decimal(12, 2) DEFAULT NULL COMMENT '当前值',
    `alarm_criteria_id` bigint         DEFAULT NULL COMMENT '告警判据ID',
    `status`            tinyint        DEFAULT '1' COMMENT '状态: 1-待处理, 2-处理中, 3-已处理, 4-已忽略',
    `create_time`       datetime       DEFAULT CURRENT_TIMESTAMP COMMENT '告警时间',
    `handle_time`       datetime       DEFAULT NULL COMMENT '处理时间',
    `handle_person`     varchar(100)   DEFAULT NULL COMMENT '处理人',
    `handle_result`     text COMMENT '处理结果',
    PRIMARY KEY (`id`),
    KEY `idx_alarm_record_hp_id` (`hazard_point_id`),
    KEY `idx_alarm_record_level` (`alarm_level`),
    KEY `idx_alarm_record_status` (`status`),
    KEY `idx_alarm_record_create_time` (`create_time`),
    KEY `fk_alarm_record_criteria` (`alarm_criteria_id`),
    CONSTRAINT `fk_alarm_record_criteria` FOREIGN KEY (`alarm_criteria_id`) REFERENCES `alarm_criteria` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='告警记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `alarm_record`
--

LOCK TABLES `alarm_record` WRITE;
/*!40000 ALTER TABLE `alarm_record`
    DISABLE KEYS */;
/*!40000 ALTER TABLE `alarm_record`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `device`
--

DROP TABLE IF EXISTS `device`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `device`
(
    `id`               bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `code`             varchar(100) NOT NULL COMMENT '设备编号',
    `sn`               varchar(100)          DEFAULT NULL COMMENT '设备SN',
    `name`             varchar(200) NOT NULL COMMENT '设备名称',
    `device_type`      tinyint               DEFAULT NULL COMMENT '设备类型:0单参数,1多参数,2本地组网',
    `network_type`     tinyint               DEFAULT NULL COMMENT '网络类型:0蜂窝,1NB-Iot',
    `protocol_type`    varchar(20)  NOT NULL DEFAULT 'MQTT' COMMENT '接入协议:MQTT/HTTP/COAP',
    `register_source`  varchar(20)  NOT NULL DEFAULT 'MANUAL' COMMENT '注册来源:MANUAL/API/IMPORT',
    `vendor_name`      varchar(200)          DEFAULT NULL COMMENT '厂商名称',
    `auth_username`    char(6)               DEFAULT NULL COMMENT '设备接入用户名,固定6位',
    `auth_password`    varchar(32)           DEFAULT NULL COMMENT '设备接入密码,明文存储',
    `auth_status`      tinyint      NOT NULL DEFAULT '1' COMMENT '账号状态:1有效,2禁用',
    `icon`             varchar(200)          DEFAULT NULL COMMENT '设备图标',
    `icon_path`        varchar(500)          DEFAULT NULL COMMENT '图标路径',
    `status`           tinyint               DEFAULT '1' COMMENT '状态: 1-正常, 2-故障, 3-离线',
    `run_status`       tinyint               DEFAULT '0' COMMENT '运行状态: 0-未知, 1-运行中, 2-停止',
    `last_report_time` datetime              DEFAULT NULL COMMENT '最近上报时间',
    `registered_at`    datetime              DEFAULT NULL COMMENT '注册时间',
    `last_auth_time`   datetime              DEFAULT NULL COMMENT '最近鉴权时间',
    `last_auth_ip`     varchar(64)           DEFAULT NULL COMMENT '最近鉴权IP',
    `longitude`        double                DEFAULT NULL COMMENT '经度',
    `latitude`         double                DEFAULT NULL COMMENT '纬度',
    `create_by`        varchar(64)           DEFAULT NULL COMMENT '创建者',
    `create_time`      datetime              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`        varchar(64)           DEFAULT NULL COMMENT '更新者',
    `update_time`      datetime              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `del_flag`         tinyint               DEFAULT '0' COMMENT '删除标记: 0-正常, 1-删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_device_code` (`code`),
    UNIQUE KEY `uk_device_auth_username` (`auth_username`),
    KEY `idx_device_status` (`status`),
    KEY `idx_device_run_status` (`run_status`),
    KEY `idx_device_del_flag` (`del_flag`),
    KEY `idx_device_register_source` (`register_source`),
    KEY `idx_device_auth_status` (`auth_status`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 2
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='设备表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `device`
--

LOCK TABLES `device` WRITE;
/*!40000 ALTER TABLE `device`
    DISABLE KEYS */;
INSERT INTO `device`
VALUES (1, 'test_device_001', '123456789', '测试设备001', 0, 0, 'MQTT', 'MANUAL', NULL, 'NZMX40', 'FSg4n5Z2', 1, 'bsw',
        '/jc-icon/green/bsw_green.png', 1, 2, '2026-06-05 20:07:40', '2026-05-28 19:12:53', '2026-06-05 20:07:27',
        '127.0.0.1', NULL, NULL, 'admin', '2026-05-28 19:12:53', 'admin', '2026-06-05 20:08:08', 0);
/*!40000 ALTER TABLE `device`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `device_auth_log`
--

DROP TABLE IF EXISTS `device_auth_log`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `device_auth_log`
(
    `id`             bigint  NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `device_id`      bigint  NOT NULL COMMENT '设备ID',
    `auth_username`  char(6) NOT NULL COMMENT '设备用户名',
    `auth_result`    tinyint NOT NULL COMMENT '1成功,0失败',
    `client_id`      varchar(128) DEFAULT NULL COMMENT 'MQTT客户端ID',
    `client_ip`      varchar(64)  DEFAULT NULL COMMENT '客户端IP',
    `failure_reason` varchar(255) DEFAULT NULL COMMENT '失败原因',
    `create_time`    datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_device_auth_log_device` (`device_id`, `create_time`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 52
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='设备认证日志';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `device_auth_log`
--

LOCK TABLES `device_auth_log` WRITE;
/*!40000 ALTER TABLE `device_auth_log`
    DISABLE KEYS */;
/*!40000 ALTER TABLE `device_auth_log`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `device_hazard_point`
--

DROP TABLE IF EXISTS `device_hazard_point`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `device_hazard_point`
(
    `id`                bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `device_id`         bigint NOT NULL COMMENT '设备ID',
    `hazard_point_id`   bigint NOT NULL COMMENT '隐患点ID',
    `install_longitude` decimal(10, 6) DEFAULT NULL COMMENT '安装经度',
    `install_latitude`  decimal(10, 6) DEFAULT NULL COMMENT '安装纬度',
    `bind_time`         datetime       DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
    `create_by`         varchar(64)    DEFAULT NULL COMMENT '创建者',
    `create_time`       datetime       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`         varchar(64)    DEFAULT NULL COMMENT '更新者',
    `update_time`       datetime       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_device_hazard_point` (`device_id`, `hazard_point_id`),
    KEY `idx_device_hazard_point_device_id` (`device_id`),
    KEY `idx_device_hazard_point_hp_id` (`hazard_point_id`),
    KEY `idx_dhp_hp_bind_time` (`hazard_point_id`, `bind_time`, `device_id`),
    CONSTRAINT `fk_dhp_device` FOREIGN KEY (`device_id`) REFERENCES `device` (`id`),
    CONSTRAINT `fk_dhp_hp` FOREIGN KEY (`hazard_point_id`) REFERENCES `hazard_point` (`id`),
    CONSTRAINT `chk_dhp_lat` CHECK (((`install_latitude` is null) or
                                     ((`install_latitude` >= -(90)) and (`install_latitude` <= 90)))),
    CONSTRAINT `chk_dhp_lng` CHECK (((`install_longitude` is null) or
                                     ((`install_longitude` >= -(180)) and (`install_longitude` <= 180))))
) ENGINE = InnoDB
  AUTO_INCREMENT = 3
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='设备隐患点关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `device_hazard_point`
--

LOCK TABLES `device_hazard_point` WRITE;
/*!40000 ALTER TABLE `device_hazard_point`
    DISABLE KEYS */;
INSERT INTO `device_hazard_point`
VALUES (2, 1, 15, NULL, NULL, '2026-05-30 16:00:37', 'admin', '2026-05-30 16:00:37', 'admin', '2026-05-30 16:00:37');
/*!40000 ALTER TABLE `device_hazard_point`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `device_online_event_log`
--

DROP TABLE IF EXISTS `device_online_event_log`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `device_online_event_log`
(
    `id`         bigint      NOT NULL AUTO_INCREMENT,
    `device_id`  bigint      NOT NULL COMMENT '设备ID',
    `event_type` varchar(16) NOT NULL COMMENT '事件类型: ONLINE / OFFLINE / HEARTBEAT',
    `client_id`  varchar(128) DEFAULT NULL COMMENT 'MQTT clientId',
    `client_ip`  varchar(64)  DEFAULT NULL COMMENT '客户端IP',
    `event_time` datetime    NOT NULL COMMENT '事件发生时间',
    `reason`     varchar(255) DEFAULT NULL COMMENT '掉线原因',
    PRIMARY KEY (`id`),
    KEY `idx_device_time` (`device_id`, `event_time`),
    KEY `idx_event_time` (`event_time`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 27
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='设备上下线事件日志';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `device_online_event_log`
--

LOCK TABLES `device_online_event_log` WRITE;
/*!40000 ALTER TABLE `device_online_event_log`
    DISABLE KEYS */;
/*!40000 ALTER TABLE `device_online_event_log`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `device_online_status`
--

DROP TABLE IF EXISTS `device_online_status`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `device_online_status`
(
    `id`                   bigint NOT NULL AUTO_INCREMENT,
    `device_id`            bigint NOT NULL COMMENT '设备ID',
    `client_id`            varchar(128) DEFAULT NULL COMMENT '当前MQTT clientId',
    `status`               tinyint      DEFAULT '0' COMMENT '0=离线 1=在线',
    `online_at`            datetime     DEFAULT NULL COMMENT '本次上线时间',
    `offline_at`           datetime     DEFAULT NULL COMMENT '上次离线时间',
    `last_report_at`       datetime     DEFAULT NULL COMMENT '最后数据上报时间',
    `session_duration_sec` int          DEFAULT NULL COMMENT '最近会话持续秒数',
    PRIMARY KEY (`id`),
    UNIQUE KEY `device_id` (`device_id`),
    KEY `idx_status` (`status`),
    KEY `idx_last_report` (`last_report_at`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 19
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='设备在线状态（运维指标独立存储）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `device_online_status`
--

LOCK TABLES `device_online_status` WRITE;
/*!40000 ALTER TABLE `device_online_status`
    DISABLE KEYS */;
INSERT INTO `device_online_status`
VALUES (1, 1, 'mqttx_f93a890d', 1, '2026-06-05 20:07:28', NULL, '2026-06-05 20:07:49', NULL),
       (2, 0, 'mqttx_f93a890d', 0, '2026-06-05 20:07:28', '2026-06-05 20:08:08', NULL, 40);
/*!40000 ALTER TABLE `device_online_status`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `device_registration_log`
--

DROP TABLE IF EXISTS `device_registration_log`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `device_registration_log`
(
    `id`              bigint      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `request_id`      varchar(64) NOT NULL COMMENT '请求幂等ID',
    `register_code`   varchar(64)  DEFAULT NULL COMMENT '设备注册码',
    `register_source` varchar(20) NOT NULL COMMENT '注册来源',
    `vendor_name`     varchar(200) DEFAULT NULL COMMENT '厂商名称',
    `device_id`       bigint       DEFAULT NULL COMMENT '设备ID',
    `sn`              varchar(100) DEFAULT NULL COMMENT '设备SN',
    `result_status`   varchar(20) NOT NULL COMMENT 'SUCCESS/FAIL',
    `failure_reason`  varchar(500) DEFAULT NULL COMMENT '失败原因',
    `request_body`    json         DEFAULT NULL COMMENT '原始请求',
    `create_time`     datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_device_register_request_id` (`request_id`),
    KEY `idx_device_register_device_id` (`device_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='设备注册日志';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `device_registration_log`
--

LOCK TABLES `device_registration_log` WRITE;
/*!40000 ALTER TABLE `device_registration_log`
    DISABLE KEYS */;
/*!40000 ALTER TABLE `device_registration_log`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `device_sensor`
--

DROP TABLE IF EXISTS `device_sensor`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `device_sensor`
(
    `id`                bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `device_id`         bigint       NOT NULL COMMENT '设备ID',
    `device_code`       varchar(100) DEFAULT NULL COMMENT '设备编号',
    `sensor_code`       varchar(100) NOT NULL COMMENT '传感器编号',
    `sensor_no`         varchar(32)  NOT NULL COMMENT '传感器编号',
    `sensor_name`       varchar(200) NOT NULL COMMENT '传感器名称',
    `monitor_type_id`   bigint       NOT NULL COMMENT '监测类型ID',
    `monitor_type_code` varchar(100) DEFAULT NULL COMMENT '监测类型编码',
    `monitor_type_name` varchar(200) DEFAULT NULL COMMENT '监测类型名称',
    `status`            tinyint      DEFAULT '1' COMMENT '状态: 0-禁用, 1-启用',
    `create_by`         varchar(64)  DEFAULT NULL COMMENT '创建者',
    `create_time`       datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`         varchar(64)  DEFAULT NULL COMMENT '更新者',
    `update_time`       datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `del_flag`          tinyint      DEFAULT '0' COMMENT '删除标记: 0-正常, 1-删除',
    `last_report_time`  datetime     DEFAULT NULL COMMENT '最后数据上报时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_device_sensor_code` (`sensor_code`),
    UNIQUE KEY `uk_device_sensor_no` (`device_id`, `sensor_no`),
    KEY `idx_device_sensor_device_id` (`device_id`),
    KEY `idx_device_sensor_type_id` (`monitor_type_id`),
    KEY `idx_device_sensor_status` (`status`),
    KEY `idx_device_sensor_del_flag` (`del_flag`),
    KEY `idx_sensor_last_report` (`last_report_time`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 3
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='传感器表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `device_sensor`
--

LOCK TABLES `device_sensor` WRITE;
/*!40000 ALTER TABLE `device_sensor`
    DISABLE KEYS */;
INSERT INTO `device_sensor`
VALUES (2, 1, 'test_device_001', 'test_sensor_001', 'test_sensor_001', '测试传感器001', 1, 'JCLX001', '雨量监测', 1,
        'admin', '2026-05-29 15:32:42', NULL, '2026-06-05 20:07:49', 0, '2026-06-05 20:07:40');
/*!40000 ALTER TABLE `device_sensor`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `device_status_log`
--

DROP TABLE IF EXISTS `device_status_log`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `device_status_log`
(
    `id`          bigint  NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `device_id`   bigint  NOT NULL COMMENT '设备ID',
    `device_code` varchar(100) DEFAULT NULL COMMENT '设备编号',
    `old_status`  tinyint      DEFAULT NULL COMMENT '旧状态',
    `new_status`  tinyint NOT NULL COMMENT '新状态',
    `status_text` varchar(50)  DEFAULT NULL COMMENT '状态文本',
    `remark`      varchar(500) DEFAULT NULL COMMENT '备注',
    `create_by`   varchar(64)  DEFAULT NULL COMMENT '创建者',
    `create_time` datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
    PRIMARY KEY (`id`),
    KEY `idx_device_status_log_device_id` (`device_id`),
    KEY `idx_device_status_log_create_time` (`create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='设备状态日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `device_status_log`
--

LOCK TABLES `device_status_log` WRITE;
/*!40000 ALTER TABLE `device_status_log`
    DISABLE KEYS */;
/*!40000 ALTER TABLE `device_status_log`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `gen_table`
--

DROP TABLE IF EXISTS `gen_table`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `gen_table`
(
    `table_id`          bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
    `table_name`        varchar(200)  DEFAULT '' COMMENT '表名称',
    `table_comment`     varchar(500)  DEFAULT '' COMMENT '表描述',
    `sub_table_name`    varchar(64)   DEFAULT NULL COMMENT '关联子表的表名',
    `sub_table_fk_name` varchar(64)   DEFAULT NULL COMMENT '子表关联的外键名',
    `class_name`        varchar(100)  DEFAULT '' COMMENT '实体类名称',
    `tpl_category`      varchar(200)  DEFAULT 'crud' COMMENT '使用的模板（crud单表操作 tree树表操作）',
    `tpl_web_type`      varchar(30)   DEFAULT '' COMMENT '前端模板类型',
    `package_name`      varchar(100)  DEFAULT NULL COMMENT '生成包路径',
    `module_name`       varchar(30)   DEFAULT NULL COMMENT '生成模块名',
    `business_name`     varchar(30)   DEFAULT NULL COMMENT '生成业务名',
    `function_name`     varchar(50)   DEFAULT NULL COMMENT '生成功能名',
    `function_author`   varchar(50)   DEFAULT NULL COMMENT '生成功能作者',
    `form_col_num`      int           DEFAULT '1' COMMENT '表单布局（单列 双列 三列）',
    `gen_type`          char(1)       DEFAULT '0' COMMENT '生成代码方式（0zip压缩包 1自定义路径）',
    `gen_path`          varchar(200)  DEFAULT '/' COMMENT '生成路径（不填默认项目路径）',
    `options`           varchar(1000) DEFAULT NULL COMMENT '其它生成选项',
    `create_by`         varchar(64)   DEFAULT '' COMMENT '创建者',
    `create_time`       datetime      DEFAULT NULL COMMENT '创建时间',
    `update_by`         varchar(64)   DEFAULT '' COMMENT '更新者',
    `update_time`       datetime      DEFAULT NULL COMMENT '更新时间',
    `remark`            varchar(500)  DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`table_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='代码生成业务表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `gen_table`
--

LOCK TABLES `gen_table` WRITE;
/*!40000 ALTER TABLE `gen_table`
    DISABLE KEYS */;
/*!40000 ALTER TABLE `gen_table`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `gen_table_column`
--

DROP TABLE IF EXISTS `gen_table_column`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `gen_table_column`
(
    `column_id`      bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
    `table_id`       bigint       DEFAULT NULL COMMENT '归属表编号',
    `column_name`    varchar(200) DEFAULT NULL COMMENT '列名称',
    `column_comment` varchar(500) DEFAULT NULL COMMENT '列描述',
    `column_type`    varchar(100) DEFAULT NULL COMMENT '列类型',
    `java_type`      varchar(500) DEFAULT NULL COMMENT 'JAVA类型',
    `java_field`     varchar(200) DEFAULT NULL COMMENT 'JAVA字段名',
    `is_pk`          char(1)      DEFAULT NULL COMMENT '是否主键（1是）',
    `is_increment`   char(1)      DEFAULT NULL COMMENT '是否自增（1是）',
    `is_required`    char(1)      DEFAULT NULL COMMENT '是否必填（1是）',
    `is_insert`      char(1)      DEFAULT NULL COMMENT '是否为插入字段（1是）',
    `is_edit`        char(1)      DEFAULT NULL COMMENT '是否编辑字段（1是）',
    `is_list`        char(1)      DEFAULT NULL COMMENT '是否列表字段（1是）',
    `is_query`       char(1)      DEFAULT NULL COMMENT '是否查询字段（1是）',
    `query_type`     varchar(200) DEFAULT 'EQ' COMMENT '查询方式（等于、不等于，大于、小于、范围）',
    `html_type`      varchar(200) DEFAULT NULL COMMENT '显示类型（文本框、文本域、下拉框、复选框、单选框、日期控件）',
    `dict_type`      varchar(200) DEFAULT '' COMMENT '字典类型',
    `sort`           int          DEFAULT NULL COMMENT '排序',
    `create_by`      varchar(64)  DEFAULT '' COMMENT '创建者',
    `create_time`    datetime     DEFAULT NULL COMMENT '创建时间',
    `update_by`      varchar(64)  DEFAULT '' COMMENT '更新者',
    `update_time`    datetime     DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`column_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='代码生成业务表字段';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `gen_table_column`
--

LOCK TABLES `gen_table_column` WRITE;
/*!40000 ALTER TABLE `gen_table_column`
    DISABLE KEYS */;
/*!40000 ALTER TABLE `gen_table_column`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `hazard_point`
--

DROP TABLE IF EXISTS `hazard_point`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hazard_point`
(
    `id`           bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `code`         varchar(100) NOT NULL COMMENT '隐患点编号',
    `name`         varchar(200) NOT NULL COMMENT '隐患点名称',
    `group_id`     bigint         DEFAULT NULL COMMENT '分组ID',
    `longitude`    decimal(10, 6) DEFAULT NULL COMMENT '中心经度',
    `latitude`     decimal(10, 6) DEFAULT NULL COMMENT '中心纬度',
    `strike`       decimal(10, 2) DEFAULT NULL COMMENT '走向角度',
    `description`  text COMMENT '隐患描述',
    `status`       tinyint        DEFAULT '1' COMMENT '状态: 1-监测中, 2-停测中, 3-已完结',
    `device_count` int            DEFAULT '0' COMMENT '绑定设备数量',
    `create_by`    varchar(64)    DEFAULT NULL COMMENT '创建者',
    `create_time`  datetime       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`    varchar(64)    DEFAULT NULL COMMENT '更新者',
    `update_time`  datetime       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `del_flag`     tinyint        DEFAULT '0' COMMENT '删除标记: 0-正常, 1-删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_hazard_point_code` (`code`),
    KEY `idx_hazard_point_group_id` (`group_id`),
    KEY `idx_hazard_point_status` (`status`),
    KEY `idx_hazard_point_del_flag` (`del_flag`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 16
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='隐患点表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `hazard_point`
--

LOCK TABLES `hazard_point` WRITE;
/*!40000 ALTER TABLE `hazard_point`
    DISABLE KEYS */;
INSERT INTO `hazard_point`
VALUES (1, 'HP001', '隐患点A修改', 2, 104.156790, 30.678902, 50.00, '修改后的描述', 1, 0, 'admin',
        '2026-05-10 16:12:29', 'admin', '2026-05-28 11:26:43', 0),
       (2, 'HP002', '龙泉寺崩塌隐患点', 1, 104.234567, 31.678901, 120.30,
        '龙泉寺后方岩质边坡，岩体破碎，存在崩塌风险，已安装裂缝计监测。', 1, 0, 'admin', '2026-05-10 16:12:29', NULL,
        '2026-05-28 11:26:43', 0),
       (3, 'HP003', '清溪乡泥石流隐患点', 2, 104.345678, 31.789012, 0.00,
        '清溪乡沟谷型泥石流隐患点，汇水面积大，暴雨季节需重点关注。', 1, 0, 'admin', '2026-05-10 16:12:29', NULL,
        '2026-05-28 11:26:43', 0),
       (4, 'HP004', '工业园区地面沉降点', 2, 104.456789, 31.890123, 0.00,
        '工业园区由于地下水位下降导致地面沉降，需持续监测地面高程变化。', 1, 0, 'admin', '2026-05-10 16:12:29', NULL,
        '2026-05-28 11:26:43', 0),
       (5, 'HP005', '顺发铁矿边坡监测点', 5, 104.567890, 31.901234, 65.00,
        '顺发铁矿露天采场边坡，高度约120米，边坡角度55度，需重点监测位移变化。', 1, 0, 'admin', '2026-05-10 16:12:29', NULL,
        '2026-05-28 11:26:43', 0),
       (6, 'HP006', '古镇危岩治理点', 3, 104.678901, 32.012345, 200.00, '古镇后山危岩体，经治理后已稳定，现处于观测期。',
        3, 0, 'admin', '2026-05-10 16:12:29', NULL, '2026-05-28 11:26:43', 0),
       (7, 'HP007', '新城基坑监测点', 2, 104.789012, 31.123456, 0.00,
        '新城建设基坑工程，因施工暂停监测，预计3个月后恢复。', 2, 0, 'admin', '2026-05-10 16:12:29', NULL,
        '2026-05-10 16:12:29', 0),
       (8, 'HP999', '我的测试', 2, 104.000000, 30.000000, 50.00, '测试', 1, 0, 'admin', '2026-05-10 17:04:51', 'admin',
        '2026-05-28 11:37:30', 1),
       (9, 'HP9999', '测试', 2, 104.000000, 30.000000, 45.00, '测试', 1, 0, 'admin', '2026-05-10 17:20:23', NULL,
        '2026-05-28 11:37:30', 1),
       (11, 'HP888', '测试', 6, 104.060004, 30.670000, 6.00, 'haidhfaib', 1, 0, 'admin', '2026-05-10 17:35:51', 'admin',
        '2026-05-28 11:37:30', 1),
       (12, 'HP777', '测试', 6, 104.060000, 30.670000, 7.00, '测试', 1, 0, 'admin', '2026-05-10 17:40:08', 'admin',
        '2026-05-28 11:37:30', 1),
       (13, 'HP008', 'test', 2, 104.060000, 30.670000, 0.00, '', 1, 0, 'admin', '2026-05-23 16:06:12', NULL,
        '2026-05-28 11:37:30', 1),
       (15, 'HP009', 'test123', NULL, 104.060000, 30.670000, 0.00, '', 1, 1, 'admin', '2026-05-23 16:07:23', 'admin',
        '2026-05-30 16:00:37', 0);
/*!40000 ALTER TABLE `hazard_point`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `hazard_point_group`
--

DROP TABLE IF EXISTS `hazard_point_group`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hazard_point_group`
(
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `code`        varchar(100) NOT NULL COMMENT '分组编码',
    `name`        varchar(200) NOT NULL COMMENT '分组名称',
    `description` varchar(500) DEFAULT NULL COMMENT '分组描述',
    `sort_order`  int          DEFAULT '0' COMMENT '排序号',
    `status`      tinyint      DEFAULT '1' COMMENT '状态: 0-禁用, 1-启用',
    `create_by`   varchar(64)  DEFAULT NULL COMMENT '创建者',
    `create_time` datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`   varchar(64)  DEFAULT NULL COMMENT '更新者',
    `update_time` datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `del_flag`    tinyint      DEFAULT '0' COMMENT '删除标记: 0-正常, 1-删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_hazard_group_code` (`code`),
    KEY `idx_hazard_group_status` (`status`),
    KEY `idx_hazard_group_del_flag` (`del_flag`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 10
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='隐患点分组表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `hazard_point_group`
--

LOCK TABLES `hazard_point_group` WRITE;
/*!40000 ALTER TABLE `hazard_point_group`
    DISABLE KEYS */;
INSERT INTO `hazard_point_group`
VALUES (1, 'G001', '崩塌监测组', '崩塌地质灾害隐患点监测', 1, 1, 'admin', '2026-05-10 18:47:12', NULL,
        '2026-05-10 18:47:12', 0),
       (2, 'G002', '滑坡监测组', '滑坡地质灾害隐患点监测', 2, 1, 'admin', '2026-05-10 18:47:12', NULL,
        '2026-05-10 18:47:12', 0),
       (3, 'G003', '泥石流监测组', '泥石流地质灾害隐患点监测', 3, 1, 'admin', '2026-05-10 18:47:12', NULL,
        '2026-05-10 18:47:12', 0),
       (4, 'G004', '沉降监测组', '地面沉降地质灾害隐患点监测', 4, 1, 'admin', '2026-05-10 18:47:12', NULL,
        '2026-05-10 18:47:12', 0),
       (5, 'G005', '边坡监测组', '边坡地质灾害隐患点监测', 5, 1, 'admin', '2026-05-10 18:47:12', NULL,
        '2026-05-10 18:47:12', 0),
       (6, 'G006', '地址测试', '地址测试', 10, 1, 'admin', '2026-05-10 18:47:12', 'admin', '2026-05-24 19:42:50', 1),
       (7, 'G009', '测试分组', '测试', 10, 1, 'admin', '2026-05-10 19:15:15', NULL, '2026-05-10 19:19:38', 1),
       (8, 'G1778412223257', '测试', '收缩不啊对吧', 4, 1, 'admin', '2026-05-10 19:23:49', 'admin',
        '2026-05-10 19:26:12', 1),
       (9, 'G1779623007852', '测试分组1', NULL, 6, 1, 'admin', '2026-05-24 19:43:28', NULL, '2026-05-24 19:43:28', 0);
/*!40000 ALTER TABLE `hazard_point_group`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `log_auth_record`
--

DROP TABLE IF EXISTS `log_auth_record`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `log_auth_record`
(
    `id`              bigint      NOT NULL AUTO_INCREMENT,
    `event_id`        bigint      NOT NULL,
    `trace_id`        varchar(64)          DEFAULT NULL,
    `request_id`      varchar(64)          DEFAULT NULL,
    `user_id`         bigint               DEFAULT NULL,
    `username`        varchar(64)          DEFAULT NULL,
    `auth_event_type` varchar(32) NOT NULL,
    `auth_channel`    varchar(32)          DEFAULT NULL,
    `request_uri`     varchar(255)         DEFAULT NULL,
    `request_method`  varchar(16)          DEFAULT NULL,
    `client_ip`       varchar(64)          DEFAULT NULL,
    `client_location` varchar(255)         DEFAULT NULL,
    `user_agent`      varchar(512)         DEFAULT NULL,
    `device_type`     varchar(32)          DEFAULT NULL,
    `http_status`     int                  DEFAULT NULL,
    `result_status`   varchar(16)          DEFAULT NULL,
    `failure_code`    varchar(64)          DEFAULT NULL,
    `failure_message` varchar(1000)        DEFAULT NULL,
    `token_id`        varchar(128)         DEFAULT NULL,
    `occurred_at`     datetime(3) NOT NULL,
    `created_at`      datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_log_auth_event_id` (`event_id`),
    KEY `idx_log_auth_time` (`occurred_at` DESC),
    KEY `idx_log_auth_type_time` (`auth_event_type`, `occurred_at` DESC),
    KEY `idx_log_auth_user_time` (`user_id`, `occurred_at` DESC),
    KEY `idx_log_auth_status_time` (`result_status`, `occurred_at` DESC),
    KEY `idx_log_auth_trace` (`trace_id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 389
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='认证日志';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `log_auth_record`
--

LOCK TABLES `log_auth_record` WRITE;
/*!40000 ALTER TABLE `log_auth_record`
    DISABLE KEYS */;
/*!40000 ALTER TABLE `log_auth_record`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `log_operation_record`
--

DROP TABLE IF EXISTS `log_operation_record`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `log_operation_record`
(
    `id`                bigint      NOT NULL AUTO_INCREMENT,
    `event_id`          bigint      NOT NULL,
    `trace_id`          varchar(64)          DEFAULT NULL,
    `request_id`        varchar(64)          DEFAULT NULL,
    `user_id`           bigint               DEFAULT NULL,
    `username`          varchar(64)          DEFAULT NULL,
    `dept_name`         varchar(64)          DEFAULT NULL,
    `title`             varchar(128)         DEFAULT NULL,
    `business_type`     varchar(32)          DEFAULT NULL,
    `api_path`          varchar(255)         DEFAULT NULL,
    `request_method`    varchar(16)          DEFAULT NULL,
    `controller_method` varchar(255)         DEFAULT NULL,
    `client_ip`         varchar(64)          DEFAULT NULL,
    `client_location`   varchar(255)         DEFAULT NULL,
    `user_agent`        varchar(512)         DEFAULT NULL,
    `request_params`    text,
    `response_body`     text,
    `http_status`       int                  DEFAULT NULL,
    `exec_status`       varchar(16)          DEFAULT NULL,
    `error_message`     varchar(2000)        DEFAULT NULL,
    `cost_time_ms`      bigint               DEFAULT NULL,
    `occurred_at`       datetime(3) NOT NULL,
    `created_at`        datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_log_operation_event_id` (`event_id`),
    KEY `idx_log_operation_time` (`occurred_at` DESC),
    KEY `idx_log_operation_user_time` (`user_id`, `occurred_at` DESC),
    KEY `idx_log_operation_status_time` (`exec_status`, `occurred_at` DESC),
    KEY `idx_log_operation_api_time` (`api_path`, `occurred_at` DESC),
    KEY `idx_log_operation_trace` (`trace_id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 752
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='接口调用日志';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `log_operation_record`
--

LOCK TABLES `log_operation_record` WRITE;
/*!40000 ALTER TABLE `log_operation_record`
    DISABLE KEYS */;
/*!40000 ALTER TABLE `log_operation_record`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `log_runtime_record`
--

DROP TABLE IF EXISTS `log_runtime_record`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `log_runtime_record`
(
    `id`              bigint       NOT NULL AUTO_INCREMENT,
    `event_id`        bigint       NOT NULL,
    `trace_id`        varchar(64)           DEFAULT NULL,
    `request_id`      varchar(64)           DEFAULT NULL,
    `level`           varchar(16)  NOT NULL,
    `logger_name`     varchar(255) NOT NULL,
    `thread_name`     varchar(128)          DEFAULT NULL,
    `biz_module`      varchar(64)           DEFAULT NULL,
    `source_app`      varchar(64)           DEFAULT NULL,
    `host_name`       varchar(64)           DEFAULT NULL,
    `environment`     varchar(32)           DEFAULT NULL,
    `message`         text         NOT NULL,
    `message_digest`  varchar(512)          DEFAULT NULL,
    `exception_class` varchar(255)          DEFAULT NULL,
    `stack_trace`     mediumtext,
    `occurred_at`     datetime(3)  NOT NULL,
    `created_at`      datetime(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_log_runtime_event_id` (`event_id`),
    KEY `idx_log_runtime_time` (`occurred_at` DESC),
    KEY `idx_log_runtime_level_time` (`level`, `occurred_at` DESC),
    KEY `idx_log_runtime_logger_time` (`logger_name`, `occurred_at` DESC),
    KEY `idx_log_runtime_host_time` (`host_name`, `occurred_at` DESC),
    KEY `idx_log_runtime_trace` (`trace_id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 86
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='运行日志';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `log_runtime_record`
--

LOCK TABLES `log_runtime_record` WRITE;
/*!40000 ALTER TABLE `log_runtime_record`
    DISABLE KEYS */;
/*!40000 ALTER TABLE `log_runtime_record`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `log_stream_checkpoint`
--

DROP TABLE IF EXISTS `log_stream_checkpoint`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `log_stream_checkpoint`
(
    `id`             bigint       NOT NULL AUTO_INCREMENT,
    `subscriber_key` varchar(128) NOT NULL,
    `last_event_id`  bigint       NOT NULL,
    `log_type`       varchar(32)  NOT NULL,
    `updated_at`     datetime(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_log_stream_checkpoint` (`subscriber_key`, `log_type`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 76
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='日志流断点记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `log_stream_checkpoint`
--

LOCK TABLES `log_stream_checkpoint` WRITE;
/*!40000 ALTER TABLE `log_stream_checkpoint`
    DISABLE KEYS */;
/*!40000 ALTER TABLE `log_stream_checkpoint`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `monitor_content`
--

DROP TABLE IF EXISTS `monitor_content`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `monitor_content`
(
    `id`              bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `monitor_type_id` bigint       NOT NULL COMMENT '监测类型ID',
    `code`            varchar(100) NOT NULL COMMENT '监测内容编码',
    `name`            varchar(200) NOT NULL COMMENT '监测内容名称',
    `unit`            varchar(50)    DEFAULT NULL COMMENT '单位',
    `indicator_type`  varchar(50)    DEFAULT NULL COMMENT '指标类型',
    `icon`            varchar(200)   DEFAULT NULL COMMENT '图标路径',
    `range_min`       decimal(12, 2) DEFAULT NULL COMMENT '最小值范围',
    `range_max`       decimal(12, 2) DEFAULT NULL COMMENT '最大值范围',
    `sort_order`      int            DEFAULT '0' COMMENT '排序号',
    `create_by`       varchar(64)    DEFAULT NULL COMMENT '创建者',
    `create_time`     datetime       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`       varchar(64)    DEFAULT NULL COMMENT '更新者',
    `update_time`     datetime       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `del_flag`        tinyint        DEFAULT '0' COMMENT '删除标记: 0-正常, 1-删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_monitor_content_code` (`code`),
    KEY `idx_monitor_content_type_id` (`monitor_type_id`),
    KEY `idx_monitor_content_del_flag` (`del_flag`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 14
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='监测内容表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `monitor_content`
--

LOCK TABLES `monitor_content` WRITE;
/*!40000 ALTER TABLE `monitor_content`
    DISABLE KEYS */;
INSERT INTO `monitor_content`
VALUES (1, 1, 'rainfall_hour', '小时雨量', 'mm', 'yl', NULL, 0.00, 500.00, 0, NULL, '2026-05-08 22:06:01', NULL,
        '2026-05-08 22:06:01', 0),
       (2, 1, 'rainfall_day', '日雨量', 'mm', 'yl', NULL, 0.00, 1000.00, 0, NULL, '2026-05-08 22:06:01', NULL,
        '2026-05-08 22:06:01', 0),
       (3, 2, 'displacement_x', 'X轴位移', 'mm', 'wy', NULL, -1000.00, 1000.00, 0, NULL, '2026-05-08 22:06:01', NULL,
        '2026-05-08 22:06:01', 0),
       (4, 2, 'displacement_y', 'Y轴位移', 'mm', 'wy', NULL, -1000.00, 1000.00, 0, NULL, '2026-05-08 22:06:01', NULL,
        '2026-05-08 22:06:01', 0),
       (5, 2, 'displacement_z', 'Z轴位移', 'mm', 'wy', NULL, -1000.00, 1000.00, 0, NULL, '2026-05-08 22:06:01', NULL,
        '2026-05-08 22:06:01', 0),
       (6, 3, 'temperature', '温度', '℃', 'wd', NULL, -50.00, 100.00, 0, NULL, '2026-05-08 22:06:01', NULL,
        '2026-05-08 22:06:01', 0),
       (7, 3, 'humidity', '含水率', '%', 'hsl', NULL, 0.00, 100.00, 0, NULL, '2026-05-08 22:06:01', NULL,
        '2026-05-08 22:06:01', 0),
       (8, 4, 'water_level', '水位', 'm', 'sw', NULL, 0.00, 100.00, 0, NULL, '2026-05-08 22:06:01', NULL,
        '2026-05-08 22:06:01', 0),
       (9, 5, 'crack_width', '裂缝宽度', 'mm', 'lf', NULL, 0.00, 50.00, 0, NULL, '2026-05-08 22:06:01', NULL,
        '2026-05-08 22:06:01', 0),
       (10, 6, 'inclination_x', 'X方向倾角', '°', 'qx', NULL, -90.00, 90.00, 0, NULL, '2026-05-08 22:06:01', NULL,
        '2026-05-08 22:06:01', 0),
       (11, 6, 'inclination_y', 'Y方向倾角', '°', 'qx', NULL, -90.00, 90.00, 0, NULL, '2026-05-08 22:06:01', NULL,
        '2026-05-08 22:06:01', 0),
       (12, 7, 'soil_temp_10cm', '10cm地温', '℃', 'dw', NULL, -50.00, 100.00, 0, NULL, '2026-05-08 22:06:01', NULL,
        '2026-05-08 22:06:01', 0),
       (13, 8, 'soil_moisture', '土壤含水率', '%', 'hsl', NULL, 0.00, 100.00, 0, NULL, '2026-05-08 22:06:01', NULL,
        '2026-05-08 22:06:01', 0);
/*!40000 ALTER TABLE `monitor_content`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `monitor_data`
--

DROP TABLE IF EXISTS `monitor_data`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `monitor_data`
(
    `id`              bigint         NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `hazard_point_id` bigint         NOT NULL COMMENT '隐患点ID',
    `device_id`       bigint         NOT NULL COMMENT '设备ID',
    `device_code`     varchar(100) DEFAULT NULL COMMENT '设备编号',
    `sensor_id`       bigint         NOT NULL COMMENT '传感器ID',
    `sensor_code`     varchar(100) DEFAULT NULL COMMENT '传感器编号',
    `monitor_type_id` bigint         NOT NULL COMMENT '监测类型ID',
    `attr_code`       varchar(100)   NOT NULL COMMENT '属性编码',
    `attr_name`       varchar(200) DEFAULT NULL COMMENT '属性名称',
    `value`           decimal(12, 2) NOT NULL COMMENT '监测值',
    `unit`            varchar(50)  DEFAULT NULL COMMENT '单位',
    `direction`       varchar(10)  DEFAULT NULL COMMENT '方向: X/Y/Z',
    `data_time`       datetime       NOT NULL COMMENT '数据时间',
    `quality`         tinyint      DEFAULT '0' COMMENT '数据质量: 0-正常, 1-可疑, 2-无效',
    `create_by`       varchar(64)  DEFAULT NULL COMMENT '创建者',
    `create_time`     datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_monitor_data_hp_id` (`hazard_point_id`),
    KEY `idx_monitor_data_device_id` (`device_id`),
    KEY `idx_monitor_data_sensor_id` (`sensor_id`),
    KEY `idx_monitor_data_data_time` (`data_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='监测数据表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `monitor_data`
--

LOCK TABLES `monitor_data` WRITE;
/*!40000 ALTER TABLE `monitor_data`
    DISABLE KEYS */;
/*!40000 ALTER TABLE `monitor_data`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `monitor_type`
--

DROP TABLE IF EXISTS `monitor_type`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `monitor_type`
(
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `code`        varchar(100) NOT NULL COMMENT '监测类型编码',
    `name`        varchar(200) NOT NULL COMMENT '监测类型名称',
    `device_type` tinyint      DEFAULT '1' COMMENT '设备类型: 1-直连设备, 2-传感器, 3-RTU',
    `icon`        varchar(200) DEFAULT NULL COMMENT '图标路径',
    `description` varchar(500) DEFAULT NULL COMMENT '描述',
    `sort_order`  int          DEFAULT '0' COMMENT '排序号',
    `status`      tinyint      DEFAULT '1' COMMENT '状态: 0-禁用, 1-启用',
    `create_by`   varchar(64)  DEFAULT NULL COMMENT '创建者',
    `create_time` datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`   varchar(64)  DEFAULT NULL COMMENT '更新者',
    `update_time` datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `del_flag`    tinyint      DEFAULT '0' COMMENT '删除标记: 0-正常, 1-删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_monitor_type_code` (`code`),
    KEY `idx_monitor_type_device_type` (`device_type`),
    KEY `idx_monitor_type_status` (`status`),
    KEY `idx_monitor_type_del_flag` (`del_flag`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 10
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='监测类型表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `monitor_type`
--

LOCK TABLES `monitor_type` WRITE;
/*!40000 ALTER TABLE `monitor_type`
    DISABLE KEYS */;
INSERT INTO `monitor_type`
VALUES (1, 'JCLX001', '雨量监测', 2, '/jc-icon/green/wj_green.png', '', 0, 1, NULL, '2026-05-08 22:06:01', 'admin',
        '2026-05-22 11:48:01', 0),
       (2, 'JCLX002', '位移监测', 2, '/jc-icon/green/jsd_green.png', '', 0, 1, NULL, '2026-05-08 22:06:01', 'admin',
        '2026-05-22 10:57:29', 0),
       (3, 'JCLX003', '温湿度监测', 2, '/jc-icon/green/ky_green.png', '', 0, 1, NULL, '2026-05-08 22:06:01', 'admin',
        '2026-05-22 11:42:05', 0),
       (4, 'JCLX004', '水位监测', 2, '/jc-icon/green/sg_green.png', '', 0, 1, NULL, '2026-05-08 22:06:01', 'admin',
        '2026-05-22 11:42:12', 0),
       (5, 'JCLX005', '裂缝监测', 2, '/jc-icon/green/jsd_green.png', '', 0, 1, NULL, '2026-05-08 22:06:01', 'admin',
        '2026-05-22 10:50:24', 0),
       (6, 'JCLX006', '倾斜监测', 2, '/jc-icon/green/nw_green.png', '', 0, 1, NULL, '2026-05-08 22:06:01', 'admin',
        '2026-05-22 11:42:17', 0),
       (7, 'JCLX007', '地温监测', 2, '/jc-icon/green/gnss_green.png', '', 0, 1, NULL, '2026-05-08 22:06:01', 'admin',
        '2026-05-22 11:42:24', 0),
       (8, 'JCLX008', '含水率监测', 2, '/jc-icon/green/lf_green.png', '', 0, 1, NULL, '2026-05-08 22:06:01', 'admin',
        '2026-05-22 11:42:28', 0),
       (9, 'JCXL456', '测试', 1, '/jc-icon/green/wj_green.png', '', 0, 1, 'admin', '2026-05-21 20:28:01', 'admin',
        '2026-05-22 10:57:42', 0);
/*!40000 ALTER TABLE `monitor_type`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `report_record`
--

DROP TABLE IF EXISTS `report_record`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `report_record`
(
    `id`                bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `template_id`       bigint       NOT NULL COMMENT '模板ID',
    `template_name`     varchar(200) DEFAULT NULL COMMENT '模板名称',
    `hazard_point_id`   bigint       DEFAULT NULL COMMENT '隐患点ID',
    `hazard_point_code` varchar(100) DEFAULT NULL COMMENT '隐患点编号',
    `hazard_point_name` varchar(200) DEFAULT NULL COMMENT '隐患点名称',
    `report_name`       varchar(200) NOT NULL COMMENT '报告名称',
    `report_date`       datetime     NOT NULL COMMENT '报告日期',
    `content`           longtext COMMENT '报告内容(HTML)',
    `file_path`         varchar(500) DEFAULT NULL COMMENT '文件路径',
    `status`            tinyint      DEFAULT '1' COMMENT '状态: 1-生成中, 2-已生成, 3-生成失败',
    `create_by`         varchar(64)  DEFAULT NULL COMMENT '创建者',
    `create_time`       datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`         varchar(64)  DEFAULT NULL COMMENT '更新者',
    `update_time`       datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_report_record_template_id` (`template_id`),
    KEY `idx_report_record_hp_id` (`hazard_point_id`),
    KEY `idx_report_record_report_date` (`report_date`),
    KEY `idx_report_record_status` (`status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='报告记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `report_record`
--

LOCK TABLES `report_record` WRITE;
/*!40000 ALTER TABLE `report_record`
    DISABLE KEYS */;
/*!40000 ALTER TABLE `report_record`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `report_template`
--

DROP TABLE IF EXISTS `report_template`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `report_template`
(
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name`        varchar(200) NOT NULL COMMENT '模板名称',
    `code`        varchar(100) NOT NULL COMMENT '模板编码',
    `type`        tinyint     DEFAULT '1' COMMENT '类型: 1-日报, 2-周报, 3-月报, 4-季报, 5-年报, 6-自定义',
    `content`     longtext COMMENT '模板内容(HTML)',
    `params`      text COMMENT '参数配置(JSON)',
    `sort_order`  int         DEFAULT '0' COMMENT '排序号',
    `status`      tinyint     DEFAULT '1' COMMENT '状态: 0-禁用, 1-启用',
    `create_by`   varchar(64) DEFAULT NULL COMMENT '创建者',
    `create_time` datetime    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`   varchar(64) DEFAULT NULL COMMENT '更新者',
    `update_time` datetime    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `del_flag`    tinyint     DEFAULT '0' COMMENT '删除标记: 0-正常, 1-删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_report_template_code` (`code`),
    KEY `idx_report_template_type` (`type`),
    KEY `idx_report_template_status` (`status`),
    KEY `idx_report_template_del_flag` (`del_flag`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='报告模板表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `report_template`
--

LOCK TABLES `report_template` WRITE;
/*!40000 ALTER TABLE `report_template`
    DISABLE KEYS */;
/*!40000 ALTER TABLE `report_template`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sensor_attribute`
--

DROP TABLE IF EXISTS `sensor_attribute`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sensor_attribute`
(
    `id`                  bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `sensor_id`           bigint       NOT NULL COMMENT '传感器ID',
    `attr_code`           varchar(100) NOT NULL COMMENT '属性编码',
    `attr_name`           varchar(200) NOT NULL COMMENT '属性名称',
    `indicator_type`      varchar(50)    DEFAULT NULL COMMENT '指标类型',
    `indicator_type_name` varchar(100)   DEFAULT NULL COMMENT '指标类型名称',
    `initial_value`       decimal(12, 2) DEFAULT NULL COMMENT '初始值',
    `unit`                varchar(50)    DEFAULT NULL COMMENT '单位',
    `range_min`           decimal(12, 2) DEFAULT NULL COMMENT '最小值范围',
    `range_max`           decimal(12, 2) DEFAULT NULL COMMENT '最大值范围',
    `icon`                varchar(500)   DEFAULT NULL COMMENT '图标路径',
    `create_by`           varchar(64)    DEFAULT NULL COMMENT '创建者',
    `create_time`         datetime       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`           varchar(64)    DEFAULT NULL COMMENT '更新者',
    `update_time`         datetime       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sensor_attr_code` (`sensor_id`, `attr_code`),
    KEY `idx_sensor_attr_sensor_id` (`sensor_id`),
    KEY `idx_sensor_attr_attr_code` (`attr_code`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 6
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='传感器属性表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sensor_attribute`
--

LOCK TABLES `sensor_attribute` WRITE;
/*!40000 ALTER TABLE `sensor_attribute`
    DISABLE KEYS */;
INSERT INTO `sensor_attribute`
VALUES (4, 2, 'rainfall_hour', '小时雨量', 'yl', '压力', 0.00, 'mm', 0.00, 500.00, NULL, 'admin', '2026-05-29 15:32:42',
        NULL, '2026-05-29 15:32:42'),
       (5, 2, 'rainfall_day', '日雨量', 'yl', '压力', 0.00, 'mm', 0.00, 1000.00, NULL, 'admin', '2026-05-29 15:32:42',
        NULL, '2026-05-29 15:32:42');
/*!40000 ALTER TABLE `sensor_attribute`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_config`
--

DROP TABLE IF EXISTS `sys_config`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_config`
(
    `config_id`    int NOT NULL AUTO_INCREMENT COMMENT '参数主键',
    `config_name`  varchar(100) DEFAULT '' COMMENT '参数名称',
    `config_key`   varchar(100) DEFAULT '' COMMENT '参数键名',
    `config_value` varchar(500) DEFAULT '' COMMENT '参数键值',
    `config_type`  char(1)      DEFAULT 'N' COMMENT '系统内置（Y是 N否）',
    `create_by`    varchar(64)  DEFAULT '' COMMENT '创建者',
    `create_time`  datetime     DEFAULT NULL COMMENT '创建时间',
    `update_by`    varchar(64)  DEFAULT '' COMMENT '更新者',
    `update_time`  datetime     DEFAULT NULL COMMENT '更新时间',
    `remark`       varchar(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`config_id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 103
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='参数配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_config`
--

LOCK TABLES `sys_config` WRITE;
/*!40000 ALTER TABLE `sys_config`
    DISABLE KEYS */;
INSERT INTO `sys_config`
VALUES (1, '主框架页-默认皮肤样式名称', 'sys.index.skinName', 'skin-blue', 'Y', 'admin', '2026-05-08 22:05:59', '',
        NULL, '蓝色 skin-blue、绿色 skin-green、紫色 skin-purple、红色 skin-red、黄色 skin-yellow'),
       (2, '用户管理-账号初始密码', 'sys.user.initPassword', '123456', 'Y', 'admin', '2026-05-08 22:05:59', '', NULL,
        '初始化密码 123456'),
       (3, '主框架页-侧边栏主题', 'sys.index.sideTheme', 'theme-dark', 'Y', 'admin', '2026-05-08 22:05:59', '', NULL,
        '深色主题theme-dark，浅色主题theme-light'),
       (4, '账号自助-验证码开关', 'sys.account.captchaEnabled', 'true', 'Y', 'admin', '2026-05-08 22:05:59', '', NULL,
        '是否开启验证码功能（true开启，false关闭）'),
       (5, '账号自助-是否开启用户注册功能', 'sys.account.registerUser', 'false', 'Y', 'admin', '2026-05-08 22:05:59',
        '', NULL, '是否开启注册用户功能（true开启，false关闭）'),
       (6, '用户登录-黑名单列表', 'sys.login.blackIPList', '', 'Y', 'admin', '2026-05-08 22:05:59', '', NULL,
        '设置登录IP黑名单限制，多个匹配项以;分隔，支持匹配（*通配、网段）'),
       (7, '用户管理-初始密码修改策略', 'sys.account.initPasswordModify', '1', 'Y', 'admin', '2026-05-08 22:06:00', '',
        NULL, '0：初始密码修改策略关闭，没有任何提示，1：提醒用户，如果未修改初始密码，则在登录时就会提醒修改密码对话框'),
       (8, '用户管理-账号密码更新周期', 'sys.account.passwordValidateDays', '0', 'Y', 'admin', '2026-05-08 22:06:00',
        '', NULL,
        '密码更新周期（填写数字，数据初始化值为0不限制，若修改必须为大于0小于365的正整数），如果超过这个周期登录系统时，则在登录时就会提醒修改密码对话框'),
       (9, '用户管理-密码字符范围', 'sys.account.chrtype', '0', 'Y', 'admin', '2026-05-08 22:06:00', '', NULL,
        '默认任意字符范围，0任意（密码可以输入任意字符），1数字（密码只能为0-9数字），2英文字母（密码只能为a-z和A-Z字母），3字母和数字（密码必须包含字母，数字）,4字母数字和特殊字符（目前支持的特殊字符包括：~!@#$%^&*()-=_+）'),
       (100, '日志自动清理开关', 'log.cleanup.enabled', 'true', 'Y', 'admin', '2026-06-05 11:24:46', '', NULL,
        '是否启用日志定时清理任务'),
       (101, '日志保留天数', 'log.cleanup.retention-days', '30', 'Y', 'admin', '2026-06-05 11:24:46', '', NULL,
        '超过此天数的操作日志/认证日志/运行日志将被清理'),
       (102, '清理执行时间', 'log.cleanup.cron', '0 0 3 * * ?', 'Y', 'admin', '2026-06-05 11:24:46', '', NULL,
        'Quartz cron 表达式，默认每天凌晨3点');
/*!40000 ALTER TABLE `sys_config`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_dept`
--

DROP TABLE IF EXISTS `sys_dept`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_dept`
(
    `dept_id`     bigint NOT NULL AUTO_INCREMENT COMMENT '部门id',
    `code`        varchar(64)  DEFAULT NULL COMMENT '组织编码',
    `parent_id`   bigint       DEFAULT '0' COMMENT '父部门id',
    `ancestors`   varchar(50)  DEFAULT '' COMMENT '祖级列表',
    `parent_ids`  varchar(255) DEFAULT NULL COMMENT '父级路径，/0/1/ 格式',
    `level`       int          DEFAULT NULL COMMENT '组织层级',
    `dept_name`   varchar(30)  DEFAULT '' COMMENT '部门名称',
    `order_num`   int          DEFAULT '0' COMMENT '显示顺序',
    `leader`      varchar(20)  DEFAULT NULL COMMENT '负责人',
    `phone`       varchar(11)  DEFAULT NULL COMMENT '联系电话',
    `email`       varchar(50)  DEFAULT NULL COMMENT '邮箱',
    `region`      varchar(50)  DEFAULT NULL COMMENT '区域',
    `address`     varchar(200) DEFAULT NULL COMMENT '地址',
    `status`      char(1)      DEFAULT '0' COMMENT '部门状态（0正常 1停用）',
    `del_flag`    char(1)      DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
    `create_by`   varchar(64)  DEFAULT '' COMMENT '创建者',
    `create_time` datetime     DEFAULT NULL COMMENT '创建时间',
    `update_by`   varchar(64)  DEFAULT '' COMMENT '更新者',
    `update_time` datetime     DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`dept_id`),
    UNIQUE KEY `uk_sys_dept_code` (`code`),
    KEY `idx_sys_dept_parent_id` (`parent_id`),
    KEY `idx_sys_dept_status` (`status`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 200
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='部门表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_dept`
--

LOCK TABLES `sys_dept` WRITE;
/*!40000 ALTER TABLE `sys_dept`
    DISABLE KEYS */;
INSERT INTO `sys_dept`
VALUES (100, 'ORG000100', 0, '0', '/0/', 1, '若依科技', 0, '若依', '15888888888', 'ry@qq.com', NULL, NULL, '0', '0',
        'admin', '2026-05-08 22:05:51', '', NULL),
       (101, 'ORG000101', 100, '0,100', '/0/100/', 2, '深圳总公司', 1, '若依', '15888888888', 'ry@qq.com', NULL, NULL,
        '0', '0', 'admin', '2026-05-08 22:05:51', '', NULL),
       (102, 'ORG000102', 100, '0,100', '/0/100/', 2, '长沙分公司', 2, '若依', '15888888888', 'ry@qq.com', NULL, NULL,
        '0', '0', 'admin', '2026-05-08 22:05:51', '', NULL),
       (103, 'ORG000103', 101, '0,100,101', '/0/100/101/', 3, '研发部门', 1, '若依', '15888888888', 'ry@qq.com', NULL,
        NULL, '0', '0', 'admin', '2026-05-08 22:05:51', '', NULL),
       (104, 'ORG000104', 101, '0,100,101', '/0/100/101/', 3, '市场部门', 2, '若依', '15888888888', 'ry@qq.com', NULL,
        NULL, '0', '0', 'admin', '2026-05-08 22:05:51', '', NULL),
       (105, 'ORG000105', 101, '0,100,101', '/0/100/101/', 3, '测试部门', 3, '若依', '15888888888', 'ry@qq.com', NULL,
        NULL, '0', '0', 'admin', '2026-05-08 22:05:51', '', NULL),
       (106, 'ORG000106', 101, '0,100,101', '/0/100/101/', 3, '财务部门', 4, '若依', '15888888888', 'ry@qq.com', NULL,
        NULL, '0', '0', 'admin', '2026-05-08 22:05:51', '', NULL),
       (107, 'ORG000107', 101, '0,100,101', '/0/100/101/', 3, '运维部门', 5, '若依', '15888888888', 'ry@qq.com', NULL,
        NULL, '0', '0', 'admin', '2026-05-08 22:05:51', '', NULL),
       (108, 'ORG000108', 102, '0,100,102', '/0/100/102/', 3, '市场部门', 1, '若依', '15888888888', 'ry@qq.com', NULL,
        NULL, '0', '0', 'admin', '2026-05-08 22:05:51', '', NULL),
       (109, 'ORG000109', 102, '0,100,102', '/0/100/102/', 3, '财务部门', 2, '若依', '15888888888', 'ry@qq.com', NULL,
        NULL, '0', '0', 'admin', '2026-05-08 22:05:51', '', NULL);
/*!40000 ALTER TABLE `sys_dept`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_dict_data`
--

DROP TABLE IF EXISTS `sys_dict_data`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_dict_data`
(
    `dict_code`   bigint NOT NULL AUTO_INCREMENT COMMENT '字典编码',
    `dict_sort`   int          DEFAULT '0' COMMENT '字典排序',
    `dict_label`  varchar(100) DEFAULT '' COMMENT '字典标签',
    `dict_value`  varchar(100) DEFAULT '' COMMENT '字典键值',
    `dict_type`   varchar(100) DEFAULT '' COMMENT '字典类型',
    `css_class`   varchar(100) DEFAULT NULL COMMENT '样式属性',
    `list_class`  varchar(100) DEFAULT NULL COMMENT '表格回显样式',
    `is_default`  char(1)      DEFAULT 'N' COMMENT '是否默认（Y是 N否）',
    `status`      char(1)      DEFAULT '0' COMMENT '状态（0正常 1停用）',
    `create_by`   varchar(64)  DEFAULT '' COMMENT '创建者',
    `create_time` datetime     DEFAULT NULL COMMENT '创建时间',
    `update_by`   varchar(64)  DEFAULT '' COMMENT '更新者',
    `update_time` datetime     DEFAULT NULL COMMENT '更新时间',
    `remark`      varchar(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`dict_code`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 100
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='字典数据表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_dict_data`
--

LOCK TABLES `sys_dict_data` WRITE;
/*!40000 ALTER TABLE `sys_dict_data`
    DISABLE KEYS */;
INSERT INTO `sys_dict_data`
VALUES (1, 1, '男', '0', 'sys_user_sex', '', '', 'Y', '0', 'admin', '2026-05-08 22:05:57', '', NULL, '性别男'),
       (2, 2, '女', '1', 'sys_user_sex', '', '', 'N', '0', 'admin', '2026-05-08 22:05:57', '', NULL, '性别女'),
       (3, 3, '未知', '2', 'sys_user_sex', '', '', 'N', '0', 'admin', '2026-05-08 22:05:57', '', NULL, '性别未知'),
       (4, 1, '显示', '0', 'sys_show_hide', '', 'primary', 'Y', '0', 'admin', '2026-05-08 22:05:57', '', NULL,
        '显示菜单'),
       (5, 2, '隐藏', '1', 'sys_show_hide', '', 'danger', 'N', '0', 'admin', '2026-05-08 22:05:57', '', NULL,
        '隐藏菜单'),
       (6, 1, '正常', '0', 'sys_normal_disable', '', 'primary', 'Y', '0', 'admin', '2026-05-08 22:05:57', '', NULL,
        '正常状态'),
       (7, 2, '停用', '1', 'sys_normal_disable', '', 'danger', 'N', '0', 'admin', '2026-05-08 22:05:57', '', NULL,
        '停用状态'),
       (8, 1, '正常', '0', 'sys_job_status', '', 'primary', 'Y', '0', 'admin', '2026-05-08 22:05:57', '', NULL,
        '正常状态'),
       (9, 2, '暂停', '1', 'sys_job_status', '', 'danger', 'N', '0', 'admin', '2026-05-08 22:05:57', '', NULL,
        '停用状态'),
       (10, 1, '默认', 'DEFAULT', 'sys_job_group', '', '', 'Y', '0', 'admin', '2026-05-08 22:05:57', '', NULL,
        '默认分组'),
       (11, 2, '系统', 'SYSTEM', 'sys_job_group', '', '', 'N', '0', 'admin', '2026-05-08 22:05:57', '', NULL,
        '系统分组'),
       (12, 1, '是', 'Y', 'sys_yes_no', '', 'primary', 'Y', '0', 'admin', '2026-05-08 22:05:57', '', NULL,
        '系统默认是'),
       (13, 2, '否', 'N', 'sys_yes_no', '', 'danger', 'N', '0', 'admin', '2026-05-08 22:05:57', '', NULL, '系统默认否'),
       (14, 1, '通知', '1', 'sys_notice_type', '', 'warning', 'Y', '0', 'admin', '2026-05-08 22:05:58', '', NULL,
        '通知'),
       (15, 2, '公告', '2', 'sys_notice_type', '', 'success', 'N', '0', 'admin', '2026-05-08 22:05:58', '', NULL,
        '公告'),
       (16, 1, '正常', '0', 'sys_notice_status', '', 'primary', 'Y', '0', 'admin', '2026-05-08 22:05:58', '', NULL,
        '正常状态'),
       (17, 2, '关闭', '1', 'sys_notice_status', '', 'danger', 'N', '0', 'admin', '2026-05-08 22:05:58', '', NULL,
        '关闭状态'),
       (18, 99, '其他', '0', 'sys_oper_type', '', 'info', 'N', '0', 'admin', '2026-05-08 22:05:58', '', NULL,
        '其他操作'),
       (19, 1, '新增', '1', 'sys_oper_type', '', 'info', 'N', '0', 'admin', '2026-05-08 22:05:58', '', NULL,
        '新增操作'),
       (20, 2, '修改', '2', 'sys_oper_type', '', 'info', 'N', '0', 'admin', '2026-05-08 22:05:58', '', NULL,
        '修改操作'),
       (21, 3, '删除', '3', 'sys_oper_type', '', 'danger', 'N', '0', 'admin', '2026-05-08 22:05:58', '', NULL,
        '删除操作'),
       (22, 4, '授权', '4', 'sys_oper_type', '', 'primary', 'N', '0', 'admin', '2026-05-08 22:05:58', '', NULL,
        '授权操作'),
       (23, 5, '导出', '5', 'sys_oper_type', '', 'warning', 'N', '0', 'admin', '2026-05-08 22:05:58', '', NULL,
        '导出操作'),
       (24, 6, '导入', '6', 'sys_oper_type', '', 'warning', 'N', '0', 'admin', '2026-05-08 22:05:58', '', NULL,
        '导入操作'),
       (25, 7, '强退', '7', 'sys_oper_type', '', 'danger', 'N', '0', 'admin', '2026-05-08 22:05:58', '', NULL,
        '强退操作'),
       (26, 8, '生成代码', '8', 'sys_oper_type', '', 'warning', 'N', '0', 'admin', '2026-05-08 22:05:58', '', NULL,
        '生成操作'),
       (27, 9, '清空数据', '9', 'sys_oper_type', '', 'danger', 'N', '0', 'admin', '2026-05-08 22:05:58', '', NULL,
        '清空操作'),
       (28, 1, '成功', '0', 'sys_common_status', '', 'primary', 'N', '0', 'admin', '2026-05-08 22:05:58', '', NULL,
        '正常状态'),
       (29, 2, '失败', '1', 'sys_common_status', '', 'danger', 'N', '0', 'admin', '2026-05-08 22:05:58', '', NULL,
        '停用状态'),
       (30, 1, '一级(蓝色)', '1', 'alarm_level', '', 'primary', 'Y', '0', 'admin', '2026-05-08 22:05:58', '', NULL,
        '一级告警'),
       (31, 2, '二级(黄色)', '2', 'alarm_level', '', 'warning', 'N', '0', 'admin', '2026-05-08 22:05:59', '', NULL,
        '二级告警'),
       (32, 3, '三级(橙色)', '3', 'alarm_level', '', 'danger', 'N', '0', 'admin', '2026-05-08 22:05:59', '', NULL,
        '三级告警'),
       (33, 4, '四级(红色)', '4', 'alarm_level', '', 'danger', 'N', '0', 'admin', '2026-05-08 22:05:59', '', NULL,
        '四级告警'),
       (34, 1, '正常', '1', 'device_status', '', 'success', 'Y', '0', 'admin', '2026-05-08 22:05:59', '', NULL,
        '设备正常'),
       (35, 2, '故障', '2', 'device_status', '', 'danger', 'N', '0', 'admin', '2026-05-08 22:05:59', '', NULL,
        '设备故障'),
       (36, 3, '离线', '3', 'device_status', '', 'warning', 'N', '0', 'admin', '2026-05-08 22:05:59', '', NULL,
        '设备离线'),
       (37, 1, '监测中', '1', 'hazard_status', '', 'success', 'Y', '0', 'admin', '2026-05-08 22:05:59', '', NULL,
        '监测中'),
       (38, 2, '停测中', '2', 'hazard_status', '', 'warning', 'N', '0', 'admin', '2026-05-08 22:05:59', '', NULL,
        '停测中'),
       (39, 3, '已完结', '3', 'hazard_status', '', 'info', 'N', '0', 'admin', '2026-05-08 22:05:59', '', NULL,
        '已完结'),
       (40, 1, '系统消息', 'SYSTEM', 'notify_channel', '', 'primary', 'Y', '0', 'admin', '2026-05-08 22:05:59', '',
        NULL, '系统消息'),
       (41, 2, '短信通知', 'SMS', 'notify_channel', '', 'success', 'N', '0', 'admin', '2026-05-08 22:05:59', '', NULL,
        '短信通知'),
       (42, 3, '微信通知', 'WECHAT', 'notify_channel', '', 'success', 'N', '0', 'admin', '2026-05-08 22:05:59', '',
        NULL, '微信通知'),
       (43, 4, '电子邮件', 'EMAIL', 'notify_channel', '', 'info', 'N', '0', 'admin', '2026-05-08 22:05:59', '', NULL,
        '电子邮件');
/*!40000 ALTER TABLE `sys_dict_data`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_dict_type`
--

DROP TABLE IF EXISTS `sys_dict_type`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_dict_type`
(
    `dict_id`     bigint NOT NULL AUTO_INCREMENT COMMENT '字典主键',
    `dict_name`   varchar(100) DEFAULT '' COMMENT '字典名称',
    `dict_type`   varchar(100) DEFAULT '' COMMENT '字典类型',
    `status`      char(1)      DEFAULT '0' COMMENT '状态（0正常 1停用）',
    `create_by`   varchar(64)  DEFAULT '' COMMENT '创建者',
    `create_time` datetime     DEFAULT NULL COMMENT '创建时间',
    `update_by`   varchar(64)  DEFAULT '' COMMENT '更新者',
    `update_time` datetime     DEFAULT NULL COMMENT '更新时间',
    `remark`      varchar(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`dict_id`),
    UNIQUE KEY `dict_type` (`dict_type`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 100
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='字典类型表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_dict_type`
--

LOCK TABLES `sys_dict_type` WRITE;
/*!40000 ALTER TABLE `sys_dict_type`
    DISABLE KEYS */;
INSERT INTO `sys_dict_type`
VALUES (1, '用户性别', 'sys_user_sex', '0', 'admin', '2026-05-08 22:05:56', '', NULL, '用户性别列表'),
       (2, '菜单状态', 'sys_show_hide', '0', 'admin', '2026-05-08 22:05:56', '', NULL, '菜单状态列表'),
       (3, '系统开关', 'sys_normal_disable', '0', 'admin', '2026-05-08 22:05:56', '', NULL, '系统开关列表'),
       (4, '任务状态', 'sys_job_status', '0', 'admin', '2026-05-08 22:05:56', '', NULL, '任务状态列表'),
       (5, '任务分组', 'sys_job_group', '0', 'admin', '2026-05-08 22:05:56', '', NULL, '任务分组列表'),
       (6, '系统是否', 'sys_yes_no', '0', 'admin', '2026-05-08 22:05:56', '', NULL, '系统是否列表'),
       (7, '通知类型', 'sys_notice_type', '0', 'admin', '2026-05-08 22:05:56', '', NULL, '通知类型列表'),
       (8, '通知状态', 'sys_notice_status', '0', 'admin', '2026-05-08 22:05:56', '', NULL, '通知状态列表'),
       (9, '操作类型', 'sys_oper_type', '0', 'admin', '2026-05-08 22:05:56', '', NULL, '操作类型列表'),
       (10, '系统状态', 'sys_common_status', '0', 'admin', '2026-05-08 22:05:56', '', NULL, '登录状态列表'),
       (11, '告警等级', 'alarm_level', '0', 'admin', '2026-05-08 22:05:57', '', NULL, '告警等级字典'),
       (12, '设备状态', 'device_status', '0', 'admin', '2026-05-08 22:05:57', '', NULL, '设备状态字典'),
       (13, '隐患点状态', 'hazard_status', '0', 'admin', '2026-05-08 22:05:57', '', NULL, '隐患点状态字典'),
       (14, '监测类型', 'monitor_type', '0', 'admin', '2026-05-08 22:05:57', '', NULL, '监测类型字典'),
       (15, '通知渠道', 'notify_channel', '0', 'admin', '2026-05-08 22:05:57', '', NULL, '通知渠道字典');
/*!40000 ALTER TABLE `sys_dict_type`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_job`
--

DROP TABLE IF EXISTS `sys_job`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_job`
(
    `job_id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '任务ID',
    `job_name`        varchar(64)  NOT NULL DEFAULT '' COMMENT '任务名称',
    `job_group`       varchar(64)  NOT NULL DEFAULT 'DEFAULT' COMMENT '任务组名',
    `invoke_target`   varchar(500) NOT NULL COMMENT '调用目标字符串',
    `cron_expression` varchar(255)          DEFAULT '' COMMENT 'cron执行表达式',
    `misfire_policy`  varchar(20)           DEFAULT '3' COMMENT '计划执行错误策略（1立即执行 2执行一次 3放弃执行）',
    `concurrent`      char(1)               DEFAULT '1' COMMENT '是否并发执行（0允许 1禁止）',
    `status`          char(1)               DEFAULT '0' COMMENT '状态（0正常 1暂停）',
    `create_by`       varchar(64)           DEFAULT '' COMMENT '创建者',
    `create_time`     datetime              DEFAULT NULL COMMENT '创建时间',
    `update_by`       varchar(64)           DEFAULT '' COMMENT '更新者',
    `update_time`     datetime              DEFAULT NULL COMMENT '更新时间',
    `remark`          varchar(500)          DEFAULT '' COMMENT '备注信息',
    PRIMARY KEY (`job_id`, `job_name`, `job_group`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 101
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='定时任务调度表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_job`
--

LOCK TABLES `sys_job` WRITE;
/*!40000 ALTER TABLE `sys_job`
    DISABLE KEYS */;
INSERT INTO `sys_job`
VALUES (1, '系统默认（无参）', 'DEFAULT', 'ryTask.ryNoParams', '0/10 * * * * ?', '3', '1', '1', 'admin',
        '2026-05-08 22:06:00', '', NULL, ''),
       (2, '系统默认（有参）', 'DEFAULT', 'ryTask.ryParams(\'ry\')', '0/15 * * * * ?', '3', '1', '1', 'admin',
        '2026-05-08 22:06:00', '', NULL, ''),
       (3, '系统默认（多参）', 'DEFAULT', 'ryTask.ryMultipleParams(\'ry\', true, 2000L, 316.50D, 100)', '0/20 * * * * ?',
        '3', '1', '1', 'admin', '2026-05-08 22:06:00', '', NULL, ''),
       (100, '日志30天自动清理', 'DEFAULT', 'logCleanupTask.cleanExpiredLogs()', '0 0 3 * * ?', '3', '1', '0', 'admin',
        '2026-05-24 11:39:38', '', NULL, '每天凌晨3点清理30天前的操作日志、认证日志、运行日志和SSE断点');
/*!40000 ALTER TABLE `sys_job`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_job_log`
--

DROP TABLE IF EXISTS `sys_job_log`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_job_log`
(
    `job_log_id`     bigint       NOT NULL AUTO_INCREMENT COMMENT '任务日志ID',
    `job_name`       varchar(64)  NOT NULL COMMENT '任务名称',
    `job_group`      varchar(64)  NOT NULL COMMENT '任务组名',
    `invoke_target`  varchar(500) NOT NULL COMMENT '调用目标字符串',
    `job_message`    varchar(500)  DEFAULT NULL COMMENT '日志信息',
    `status`         char(1)       DEFAULT '0' COMMENT '执行状态（0正常 1失败）',
    `exception_info` varchar(2000) DEFAULT '' COMMENT '异常信息',
    `start_time`     datetime      DEFAULT NULL COMMENT '执行开始时间',
    `end_time`       datetime      DEFAULT NULL COMMENT '执行结束时间',
    `create_time`    datetime      DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (`job_log_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='定时任务调度日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_job_log`
--

LOCK TABLES `sys_job_log` WRITE;
/*!40000 ALTER TABLE `sys_job_log`
    DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_job_log`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_menu`
--

DROP TABLE IF EXISTS `sys_menu`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_menu`
(
    `menu_id`     bigint      NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
    `menu_name`   varchar(50) NOT NULL COMMENT '菜单名称',
    `parent_id`   bigint       DEFAULT '0' COMMENT '父菜单ID',
    `order_num`   int          DEFAULT '0' COMMENT '显示顺序',
    `path`        varchar(200) DEFAULT '' COMMENT '路由地址',
    `component`   varchar(255) DEFAULT NULL COMMENT '组件路径',
    `query`       varchar(255) DEFAULT NULL COMMENT '路由参数',
    `route_name`  varchar(50)  DEFAULT '' COMMENT '路由名称',
    `is_frame`    int          DEFAULT '1' COMMENT '是否为外链（0是 1否）',
    `is_cache`    int          DEFAULT '0' COMMENT '是否缓存（0缓存 1不缓存）',
    `menu_type`   char(1)      DEFAULT '' COMMENT '菜单类型（M目录 C菜单 F按钮）',
    `visible`     char(1)      DEFAULT '0' COMMENT '菜单状态（0显示 1隐藏）',
    `status`      char(1)      DEFAULT '0' COMMENT '菜单状态（0正常 1停用）',
    `perms`       varchar(100) DEFAULT NULL COMMENT '权限标识',
    `icon`        varchar(100) DEFAULT '#' COMMENT '菜单图标',
    `create_by`   varchar(64)  DEFAULT '' COMMENT '创建者',
    `create_time` datetime     DEFAULT NULL COMMENT '创建时间',
    `update_by`   varchar(64)  DEFAULT '' COMMENT '更新者',
    `update_time` datetime     DEFAULT NULL COMMENT '更新时间',
    `remark`      varchar(500) DEFAULT '' COMMENT '备注',
    PRIMARY KEY (`menu_id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 2012
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='菜单权限表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_menu`
--

LOCK TABLES `sys_menu` WRITE;
/*!40000 ALTER TABLE `sys_menu`
    DISABLE KEYS */;
INSERT INTO `sys_menu`
VALUES (1, '系统管理', 0, 1, 'system', NULL, '', '', 1, 0, 'M', '0', '0', '', 'system', 'admin', '2026-05-08 22:05:53',
        '', NULL, '系统管理目录'),
       (2, '系统监控', 0, 2, 'monitor', NULL, '', '', 1, 0, 'M', '0', '0', '', 'monitor', 'admin',
        '2026-05-08 22:05:53', '', NULL, '系统监控目录'),
       (3, '系统工具', 0, 3, 'tool', NULL, '', '', 1, 0, 'M', '0', '0', '', 'tool', 'admin', '2026-05-08 22:05:53', '',
        NULL, '系统工具目录'),
       (4, '若依官网', 0, 4, 'http://zwei.vip', NULL, '', '', 0, 0, 'M', '0', '0', '', 'guide', 'admin',
        '2026-05-08 22:05:53', '', NULL, '若依官网地址'),
       (100, '用户管理', 1, 1, 'user', 'system/user/index', '', '', 1, 0, 'C', '0', '0', 'system:user:list', 'user',
        'admin', '2026-05-08 22:05:53', '', NULL, '用户管理菜单'),
       (101, '角色管理', 1, 2, 'role', 'system/role/index', '', '', 1, 0, 'C', '0', '0', 'system:role:list', 'peoples',
        'admin', '2026-05-08 22:05:53', '', NULL, '角色管理菜单'),
       (102, '菜单管理', 1, 3, 'menu', 'system/menu/index', '', '', 1, 0, 'C', '0', '0', 'system:menu:list',
        'tree-table', 'admin', '2026-05-08 22:05:53', '', NULL, '菜单管理菜单'),
       (103, '部门管理', 1, 4, 'dept', 'system/dept/index', '', '', 1, 0, 'C', '0', '0', 'system:dept:list', 'tree',
        'admin', '2026-05-08 22:05:53', '', NULL, '部门管理菜单'),
       (104, '岗位管理', 1, 5, 'post', 'system/post/index', '', '', 1, 0, 'C', '0', '0', 'system:post:list', 'post',
        'admin', '2026-05-08 22:05:53', '', NULL, '岗位管理菜单'),
       (105, '字典管理', 1, 6, 'dict', 'system/dict/index', '', '', 1, 0, 'C', '0', '0', 'system:dict:list', 'dict',
        'admin', '2026-05-08 22:05:53', '', NULL, '字典管理菜单'),
       (106, '参数设置', 1, 7, 'config', 'system/config/index', '', '', 1, 0, 'C', '0', '0', 'system:config:list',
        'edit', 'admin', '2026-05-08 22:05:53', '', NULL, '参数设置菜单'),
       (107, '通知公告', 1, 8, 'notice', 'system/notice/index', '', '', 1, 0, 'C', '0', '0', 'system:notice:list',
        'message', 'admin', '2026-05-08 22:05:53', '', NULL, '通知公告菜单'),
       (108, '日志管理', 1, 9, 'log', '', '', '', 1, 0, 'M', '0', '0', '', 'log', 'admin', '2026-05-08 22:05:53', '',
        NULL, '日志管理菜单'),
       (109, '在线用户', 2, 1, 'online', 'monitor/online/index', '', '', 1, 0, 'C', '0', '0', 'monitor:online:list',
        'online', 'admin', '2026-05-08 22:05:53', '', NULL, '在线用户菜单'),
       (110, '定时任务', 2, 2, 'job', 'monitor/job/index', '', '', 1, 0, 'C', '0', '0', 'monitor:job:list', 'job',
        'admin', '2026-05-08 22:05:54', '', NULL, '定时任务菜单'),
       (111, '数据监控', 2, 3, 'druid', 'monitor/druid/index', '', '', 1, 0, 'C', '0', '0', 'monitor:druid:list',
        'druid', 'admin', '2026-05-08 22:05:54', '', NULL, '数据监控菜单'),
       (112, '服务监控', 2, 4, 'server', 'monitor/server/index', '', '', 1, 0, 'C', '0', '0', 'monitor:server:list',
        'server', 'admin', '2026-05-08 22:05:54', '', NULL, '服务监控菜单'),
       (113, '缓存监控', 2, 5, 'cache', 'monitor/cache/index', '', '', 1, 0, 'C', '0', '0', 'monitor:cache:list',
        'redis', 'admin', '2026-05-08 22:05:54', '', NULL, '缓存监控菜单'),
       (114, '缓存列表', 2, 6, 'cacheList', 'monitor/cache/list', '', '', 1, 0, 'C', '0', '0', 'monitor:cache:list',
        'redis-list', 'admin', '2026-05-08 22:05:54', '', NULL, '缓存列表菜单'),
       (115, '表单构建', 3, 1, 'build', 'tool/build/index', '', '', 1, 0, 'C', '0', '0', 'tool:build:list', 'build',
        'admin', '2026-05-08 22:05:54', '', NULL, '表单构建菜单'),
       (116, '代码生成', 3, 2, 'gen', 'tool/gen/index', '', '', 1, 0, 'C', '0', '0', 'tool:gen:list', 'code', 'admin',
        '2026-05-08 22:05:54', '', NULL, '代码生成菜单'),
       (117, '系统接口', 3, 3, 'swagger', 'tool/swagger/index', '', '', 1, 0, 'C', '0', '0', 'tool:swagger:list',
        'swagger', 'admin', '2026-05-08 22:05:54', '', NULL, '系统接口菜单'),
       (500, '操作日志', 108, 1, 'operlog', 'monitor/operlog/index', '', '', 1, 0, 'C', '0', '0',
        'monitor:operlog:list', 'form', 'admin', '2026-05-08 22:05:54', '', NULL, '操作日志菜单'),
       (501, '登录日志', 108, 2, 'logininfor', 'monitor/logininfor/index', '', '', 1, 0, 'C', '0', '0',
        'monitor:logininfor:list', 'logininfor', 'admin', '2026-05-08 22:05:54', '', NULL, '登录日志菜单'),
       (1000, '用户查询', 100, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:query', '#', 'admin',
        '2026-05-08 22:05:54', '', NULL, ''),
       (1001, '用户新增', 100, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:add', '#', 'admin',
        '2026-05-08 22:05:54', '', NULL, ''),
       (1002, '用户修改', 100, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:edit', '#', 'admin',
        '2026-05-08 22:05:54', '', NULL, ''),
       (1003, '用户删除', 100, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:remove', '#', 'admin',
        '2026-05-08 22:05:54', '', NULL, ''),
       (1004, '用户导出', 100, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:export', '#', 'admin',
        '2026-05-08 22:05:54', '', NULL, ''),
       (1005, '用户导入', 100, 6, '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:import', '#', 'admin',
        '2026-05-08 22:05:54', '', NULL, ''),
       (1006, '重置密码', 100, 7, '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:resetPwd', '#', 'admin',
        '2026-05-08 22:05:54', '', NULL, ''),
       (2000, '基础业务', 0, 5, 'basic', NULL, '', '', 1, 0, 'M', '0', '0', '', 'guide', 'admin', '2026-05-24 18:00:00',
        '', NULL, '基础业务目录'),
       (2001, '隐患点分组管理', 2000, 2, 'hazard-point-group', 'basic/hazard-point/index', '', 'HazardPointGroup', 1, 0,
        'C', '1', '0', 'basic:hazardPointGroup:list', 'tree', 'admin', '2026-05-24 18:00:00', '', NULL,
        '隐患点分组管理菜单'),
       (2002, '分组查询', 2001, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'basic:hazardPointGroup:query', '#', 'admin',
        '2026-05-24 18:00:00', '', NULL, ''),
       (2003, '分组新增', 2001, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'basic:hazardPointGroup:add', '#', 'admin',
        '2026-05-24 18:00:00', '', NULL, ''),
       (2004, '分组修改', 2001, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'basic:hazardPointGroup:edit', '#', 'admin',
        '2026-05-24 18:00:00', '', NULL, ''),
       (2005, '分组删除', 2001, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'basic:hazardPointGroup:remove', '#', 'admin',
        '2026-05-24 18:00:00', '', NULL, ''),
       (2006, 'MQTT监控', 2, 7, 'mqtt-monitor', 'iot/ServiceStatus', '', '', 1, 0, 'C', '0', '0', 'monitor:mqtt:list',
        'monitor', 'admin', '2026-06-03 10:00:00', '', NULL, 'MQTT服务器监控'),
       (2007, 'MQTT踢出', 2006, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'monitor:mqtt:kick', '#', 'admin',
        '2026-06-03 10:00:00', '', NULL, ''),
       (2008, '监控总览', 2, 0, 'monitor-overview', 'monitor/overview', '', '', 1, 0, 'C', '0', '0',
        'monitor:overview:list', 'dashboard', 'admin', '2026-06-03 10:00:00', '', NULL, '系统监控总览'),
       (2009, '日志查询', 108, 3, 'log-query', 'monitor/log-query/index', '', '', 1, 0, 'C', '0', '0',
        'monitor:operlog:list', 'log', 'admin', '2026-06-03 10:00:00', '', NULL, '操作/认证/运行日志查询'),
       (2010, '文件上传', 3, 4, 'file-upload', '', '', '', 1, 0, 'F', '0', '0', 'common:file:upload', 'upload', 'admin',
        '2026-06-03 10:00:00', '', NULL, ''),
       (2011, '文件下载', 3, 5, 'file-download', '', '', '', 1, 0, 'F', '0', '0', 'common:file:query', 'download',
        'admin', '2026-06-03 10:00:00', '', NULL, '');
/*!40000 ALTER TABLE `sys_menu`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_notice`
--

DROP TABLE IF EXISTS `sys_notice`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_notice`
(
    `notice_id`      int         NOT NULL AUTO_INCREMENT COMMENT '公告ID',
    `notice_title`   varchar(50) NOT NULL COMMENT '公告标题',
    `notice_type`    char(1)     NOT NULL COMMENT '公告类型（1通知 2公告）',
    `notice_content` longblob COMMENT '公告内容',
    `status`         char(1)      DEFAULT '0' COMMENT '公告状态（0正常 1关闭）',
    `create_by`      varchar(64)  DEFAULT '' COMMENT '创建者',
    `create_time`    datetime     DEFAULT NULL COMMENT '创建时间',
    `update_by`      varchar(64)  DEFAULT '' COMMENT '更新者',
    `update_time`    datetime     DEFAULT NULL COMMENT '更新时间',
    `remark`         varchar(255) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`notice_id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 10
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='通知公告表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_notice`
--

LOCK TABLES `sys_notice` WRITE;
/*!40000 ALTER TABLE `sys_notice`
    DISABLE KEYS */;
INSERT INTO `sys_notice`
VALUES (1, '温馨提醒：2018-07-01 若依新版本发布啦', '2', _binary '新版本内容', '0', 'admin', '2026-05-08 22:06:00', '',
        NULL, '管理员'),
       (2, '维护通知：2018-07-01 若依系统凌晨维护', '1', _binary '维护内容', '0', 'admin', '2026-05-08 22:06:00', '',
        NULL, '管理员');
/*!40000 ALTER TABLE `sys_notice`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_notice_read`
--

DROP TABLE IF EXISTS `sys_notice_read`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_notice_read`
(
    `read_id`   bigint   NOT NULL AUTO_INCREMENT COMMENT '已读主键',
    `notice_id` int      NOT NULL COMMENT '公告id',
    `user_id`   bigint   NOT NULL COMMENT '用户id',
    `read_time` datetime NOT NULL COMMENT '阅读时间',
    PRIMARY KEY (`read_id`),
    UNIQUE KEY `uk_user_notice` (`user_id`, `notice_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='公告已读记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_notice_read`
--

LOCK TABLES `sys_notice_read` WRITE;
/*!40000 ALTER TABLE `sys_notice_read`
    DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_notice_read`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_notify_instance`
--

DROP TABLE IF EXISTS `sys_notify_instance`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_notify_instance`
(
    `instance_id`   bigint       NOT NULL AUTO_INCREMENT,
    `template_code` varchar(64) DEFAULT NULL COMMENT '模板编码，NULL表示自定义通知',
    `notify_type`   varchar(32)  NOT NULL COMMENT 'alarm / business / system',
    `title`         varchar(255) NOT NULL COMMENT '通知标题',
    `content`       text COMMENT '通知内容',
    `priority`      tinyint     DEFAULT '0' COMMENT '优先级',
    `source_type`   varchar(32) DEFAULT NULL COMMENT '触发来源: alarm_engine / device_event / manual',
    `source_id`     varchar(64) DEFAULT NULL COMMENT '触发来源ID（链路追踪）',
    `create_by`     varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time`   datetime    DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (`instance_id`),
    KEY `idx_type_time` (`notify_type`, `create_time`),
    KEY `idx_source` (`source_type`, `source_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='通知实例';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_notify_instance`
--

LOCK TABLES `sys_notify_instance` WRITE;
/*!40000 ALTER TABLE `sys_notify_instance`
    DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_notify_instance`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_notify_target`
--

DROP TABLE IF EXISTS `sys_notify_target`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_notify_target`
(
    `target_id`    bigint NOT NULL AUTO_INCREMENT,
    `instance_id`  bigint NOT NULL COMMENT '通知实例ID',
    `user_id`      bigint NOT NULL COMMENT '目标用户ID',
    `channel`      varchar(16)  DEFAULT 'in_app' COMMENT '推送通道: in_app / email / sms',
    `send_status`  tinyint      DEFAULT '0' COMMENT '0=待发送 1=已发送 2=发送失败 3=已读 4=已归档',
    `send_time`    datetime     DEFAULT NULL COMMENT '发送时间',
    `read_time`    datetime     DEFAULT NULL COMMENT '阅读时间（in_app通道）',
    `archive_time` datetime     DEFAULT NULL COMMENT '归档时间',
    `retry_count`  int          DEFAULT '0' COMMENT '重试次数',
    `error_msg`    varchar(500) DEFAULT NULL COMMENT '发送失败原因',
    PRIMARY KEY (`target_id`),
    KEY `idx_user_status` (`user_id`, `send_status`),
    KEY `idx_instance` (`instance_id`),
    KEY `idx_channel_status` (`channel`, `send_status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='通知目标（下发/处理/归档全生命周期）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_notify_target`
--

LOCK TABLES `sys_notify_target` WRITE;
/*!40000 ALTER TABLE `sys_notify_target`
    DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_notify_target`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_notify_template`
--

DROP TABLE IF EXISTS `sys_notify_template`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_notify_template`
(
    `template_id`   bigint       NOT NULL AUTO_INCREMENT,
    `template_code` varchar(64)  NOT NULL COMMENT '模板编码: alarm_threshold / device_offline / system_maintenance',
    `template_name` varchar(100) NOT NULL COMMENT '模板名称',
    `notify_type`   varchar(32)  NOT NULL COMMENT '通知分类: alarm / business / system',
    `title_tpl`     varchar(255) DEFAULT NULL COMMENT '标题模板，支持 {变量} 替换',
    `content_tpl`   text COMMENT '内容模板，支持 {变量} 替换',
    `channels`      varchar(128) DEFAULT 'in_app' COMMENT '推送通道列表，逗号分隔: in_app,email,sms',
    `priority`      tinyint      DEFAULT '0' COMMENT '优先级: 0=普通 1=重要 2=紧急',
    `status`        char(1)      DEFAULT '0' COMMENT '状态: 0=启用 1=禁用',
    `create_by`     varchar(64)  DEFAULT '' COMMENT '创建者',
    `create_time`   datetime     DEFAULT NULL COMMENT '创建时间',
    `update_by`     varchar(64)  DEFAULT '' COMMENT '更新者',
    `update_time`   datetime     DEFAULT NULL COMMENT '更新时间',
    `remark`        varchar(255) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`template_id`),
    UNIQUE KEY `template_code` (`template_code`),
    KEY `idx_code` (`template_code`),
    KEY `idx_type` (`notify_type`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 4
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='通知模板';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_notify_template`
--

LOCK TABLES `sys_notify_template` WRITE;
/*!40000 ALTER TABLE `sys_notify_template`
    DISABLE KEYS */;
INSERT INTO `sys_notify_template`
VALUES (1, 'alarm_threshold', '监测数据超阈值告警', 'alarm', '{hazardPointName}监测{attrName}超过阈值',
        '{hazardPointName}（{hazardPointCode}）的{deviceName}设备{attrName}监测值({value}{unit})超过阈值范围[{rangeMin}{unit}, {rangeMax}{unit}]，请及时处理。',
        'in_app,sms', 2, '0', '', NULL, '', NULL, NULL),
       (2, 'device_offline', '设备离线告警', 'alarm', '{deviceName}设备离线',
        '{deviceName}（{deviceCode}）离线超过{offlineMinutes}分钟，请检查设备状态。', 'in_app', 1, '0', '', NULL, '', NULL,
        NULL),
       (3, 'system_maintenance', '系统维护通知', 'system', '系统维护通知',
        '系统将于{maintenanceTime}进行维护，预计持续{duration}分钟，届时部分功能可能不可用。', 'in_app,email', 0, '0', '',
        NULL, '', NULL, NULL);
/*!40000 ALTER TABLE `sys_notify_template`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_organization`
--

DROP TABLE IF EXISTS `sys_organization`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_organization`
(
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `code`        varchar(100) NOT NULL COMMENT '组织编码',
    `name`        varchar(200) NOT NULL COMMENT '组织名称',
    `parent_id`   bigint       DEFAULT '0' COMMENT '父组织ID，0为根节点',
    `parent_ids`  varchar(500) DEFAULT NULL COMMENT '父组织ID路径，如/0/1/2/',
    `level`       tinyint      DEFAULT '1' COMMENT '层级: 1-5级',
    `leader`      varchar(100) DEFAULT NULL COMMENT '负责人',
    `phone`       varchar(20)  DEFAULT NULL COMMENT '联系电话',
    `email`       varchar(100) DEFAULT NULL COMMENT '邮箱',
    `region`      varchar(200) DEFAULT NULL COMMENT '区域',
    `center`      varchar(200) DEFAULT NULL COMMENT '中心点坐标',
    `address`     varchar(500) DEFAULT NULL COMMENT '详细地址',
    `sort_order`  int          DEFAULT '0' COMMENT '排序号',
    `status`      tinyint      DEFAULT '1' COMMENT '状态: 0-禁用, 1-启用',
    `create_by`   varchar(64)  DEFAULT NULL COMMENT '创建者',
    `create_time` datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`   varchar(64)  DEFAULT NULL COMMENT '更新者',
    `update_time` datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `del_flag`    tinyint      DEFAULT '0' COMMENT '删除标记: 0-正常, 1-删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_org_code` (`code`),
    KEY `idx_sys_org_parent_id` (`parent_id`),
    KEY `idx_sys_org_level` (`level`),
    KEY `idx_sys_org_status` (`status`),
    KEY `idx_sys_org_del_flag` (`del_flag`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 4
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='组织架构表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_organization`
--

LOCK TABLES `sys_organization` WRITE;
/*!40000 ALTER TABLE `sys_organization`
    DISABLE KEYS */;
INSERT INTO `sys_organization`
VALUES (1, 'ROOT', '系统管理员', 0, '/0/', 1, NULL, NULL, NULL, NULL, NULL, NULL, 0, 1, NULL, '2026-05-08 22:06:01',
        NULL, '2026-05-08 22:06:01', 0),
       (2, 'DEPT001', '监测中心', 1, '/0/1/', 2, NULL, NULL, NULL, NULL, NULL, NULL, 0, 1, NULL, '2026-05-08 22:06:01',
        NULL, '2026-05-08 22:06:01', 0),
       (3, 'DEPT002', '运维部', 1, '/0/1/', 2, NULL, NULL, NULL, NULL, NULL, NULL, 0, 1, NULL, '2026-05-08 22:06:01',
        NULL, '2026-05-08 22:06:01', 0);
/*!40000 ALTER TABLE `sys_organization`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_post`
--

DROP TABLE IF EXISTS `sys_post`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_post`
(
    `post_id`     bigint      NOT NULL AUTO_INCREMENT COMMENT '岗位ID',
    `post_code`   varchar(64) NOT NULL COMMENT '岗位编码',
    `post_name`   varchar(50) NOT NULL COMMENT '岗位名称',
    `post_sort`   int         NOT NULL COMMENT '显示顺序',
    `status`      char(1)     NOT NULL COMMENT '状态（0正常 1停用）',
    `create_by`   varchar(64)  DEFAULT '' COMMENT '创建者',
    `create_time` datetime     DEFAULT NULL COMMENT '创建时间',
    `update_by`   varchar(64)  DEFAULT '' COMMENT '更新者',
    `update_time` datetime     DEFAULT NULL COMMENT '更新时间',
    `remark`      varchar(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`post_id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 5
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='岗位信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_post`
--

LOCK TABLES `sys_post` WRITE;
/*!40000 ALTER TABLE `sys_post`
    DISABLE KEYS */;
INSERT INTO `sys_post`
VALUES (1, 'ceo', '董事长', 1, '0', 'admin', '2026-05-08 22:05:52', '', NULL, ''),
       (2, 'se', '项目经理', 2, '0', 'admin', '2026-05-08 22:05:52', '', NULL, ''),
       (3, 'hr', '人力资源', 3, '0', 'admin', '2026-05-08 22:05:52', '', NULL, ''),
       (4, 'user', '普通员工', 4, '0', 'admin', '2026-05-08 22:05:52', '', NULL, '');
/*!40000 ALTER TABLE `sys_post`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_role`
--

DROP TABLE IF EXISTS `sys_role`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role`
(
    `role_id`             bigint       NOT NULL AUTO_INCREMENT COMMENT '角色ID',
    `role_name`           varchar(30)  NOT NULL COMMENT '角色名称',
    `role_key`            varchar(100) NOT NULL COMMENT '角色权限字符串',
    `role_sort`           int          NOT NULL COMMENT '显示顺序',
    `data_scope`          char(1)      DEFAULT '1' COMMENT '数据范围（1：全部数据权限 2：自定数据权限 3：本部门数据权限 4：本部门及以下数据权限）',
    `menu_check_strictly` tinyint(1)   DEFAULT '1' COMMENT '菜单树选择项是否关联显示',
    `dept_check_strictly` tinyint(1)   DEFAULT '1' COMMENT '部门树选择项是否关联显示',
    `status`              char(1)      NOT NULL COMMENT '角色状态（0正常 1停用）',
    `del_flag`            char(1)      DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
    `create_by`           varchar(64)  DEFAULT '' COMMENT '创建者',
    `create_time`         datetime     DEFAULT NULL COMMENT '创建时间',
    `update_by`           varchar(64)  DEFAULT '' COMMENT '更新者',
    `update_time`         datetime     DEFAULT NULL COMMENT '更新时间',
    `remark`              varchar(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`role_id`),
    UNIQUE KEY `uk_sys_role_role_name` (`role_name`),
    UNIQUE KEY `uk_sys_role_role_key` (`role_key`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 102
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='角色信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_role`
--

LOCK TABLES `sys_role` WRITE;
/*!40000 ALTER TABLE `sys_role`
    DISABLE KEYS */;
INSERT INTO `sys_role`
VALUES (1, '超级管理员', 'admin', 1, '1', 1, 1, '0', '0', 'admin', '2026-05-08 22:05:52', '', NULL, '超级管理员'),
       (2, '普通角色', 'common', 2, '2', 1, 1, '0', '0', 'admin', '2026-05-08 22:05:53', '', NULL, '普通角色'),
       (100, '监测管理员', 'MONITOR', 3, '2', 1, 1, '0', '0', 'admin', '2026-05-08 22:06:03', 'admin',
        '2026-05-24 21:22:31', '监测业务管理员'),
       (101, '操作员', 'OPERATOR', 4, '3', 1, 1, '0', '0', 'admin', '2026-05-08 22:06:03', 'admin',
        '2026-05-24 21:22:41', '普通操作员');
/*!40000 ALTER TABLE `sys_role`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_role_dept`
--

DROP TABLE IF EXISTS `sys_role_dept`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role_dept`
(
    `role_id` bigint NOT NULL COMMENT '角色ID',
    `dept_id` bigint NOT NULL COMMENT '部门ID',
    PRIMARY KEY (`role_id`, `dept_id`),
    KEY `idx_sys_role_dept_role_id` (`role_id`),
    KEY `idx_sys_role_dept_dept_id` (`dept_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='角色和部门关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_role_dept`
--

LOCK TABLES `sys_role_dept` WRITE;
/*!40000 ALTER TABLE `sys_role_dept`
    DISABLE KEYS */;
INSERT INTO `sys_role_dept`
VALUES (2, 100),
       (2, 101),
       (2, 105);
/*!40000 ALTER TABLE `sys_role_dept`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_role_menu`
--

DROP TABLE IF EXISTS `sys_role_menu`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role_menu`
(
    `role_id` bigint NOT NULL COMMENT '角色ID',
    `menu_id` bigint NOT NULL COMMENT '菜单ID',
    PRIMARY KEY (`role_id`, `menu_id`),
    KEY `idx_sys_role_menu_role_id` (`role_id`),
    KEY `idx_sys_role_menu_menu_id` (`menu_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='角色和菜单关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_role_menu`
--

LOCK TABLES `sys_role_menu` WRITE;
/*!40000 ALTER TABLE `sys_role_menu`
    DISABLE KEYS */;
INSERT INTO `sys_role_menu`
VALUES (2, 1),
       (2, 2),
       (2, 3),
       (2, 4),
       (2, 100),
       (2, 101),
       (2, 102),
       (2, 103),
       (2, 104),
       (2, 105),
       (2, 106),
       (2, 107),
       (2, 108),
       (2, 500),
       (2, 501),
       (2, 1000),
       (2, 1001),
       (2, 1002),
       (2, 1003),
       (2, 1004),
       (2, 1005),
       (2, 1006),
       (2, 2006),
       (2, 2007),
       (2, 2008),
       (2, 2009),
       (2, 2010),
       (2, 2011),
       (100, 2006),
       (100, 2007),
       (100, 2008),
       (100, 2009);
/*!40000 ALTER TABLE `sys_role_menu`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_user`
--

DROP TABLE IF EXISTS `sys_user`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user`
(
    `user_id`         bigint      NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `dept_id`         bigint       DEFAULT NULL COMMENT '部门ID',
    `user_name`       varchar(30) NOT NULL COMMENT '用户账号',
    `nick_name`       varchar(30) NOT NULL COMMENT '用户昵称',
    `user_type`       varchar(2)   DEFAULT '00' COMMENT '用户类型（00系统用户）',
    `email`           varchar(50)  DEFAULT '' COMMENT '用户邮箱',
    `phonenumber`     varchar(11)  DEFAULT '' COMMENT '手机号码',
    `sex`             char(1)      DEFAULT '0' COMMENT '用户性别（0男 1女 2未知）',
    `avatar`          varchar(100) DEFAULT '' COMMENT '头像地址',
    `password`        varchar(100) DEFAULT '' COMMENT '密码',
    `status`          char(1)      DEFAULT '0' COMMENT '账号状态（0正常 1停用）',
    `del_flag`        char(1)      DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
    `login_ip`        varchar(128) DEFAULT '' COMMENT '最后登录IP',
    `login_date`      datetime     DEFAULT NULL COMMENT '最后登录时间',
    `pwd_update_date` datetime     DEFAULT NULL COMMENT '密码最后更新时间',
    `create_by`       varchar(64)  DEFAULT '' COMMENT '创建者',
    `create_time`     datetime     DEFAULT NULL COMMENT '创建时间',
    `update_by`       varchar(64)  DEFAULT '' COMMENT '更新者',
    `update_time`     datetime     DEFAULT NULL COMMENT '更新时间',
    `remark`          varchar(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`user_id`),
    UNIQUE KEY `uk_sys_user_user_name` (`user_name`),
    UNIQUE KEY `uk_sys_user_phonenumber` (`phonenumber`),
    UNIQUE KEY `uk_sys_user_email` (`email`),
    KEY `idx_sys_user_dept_id` (`dept_id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 100
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='用户信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_user`
--

LOCK TABLES `sys_user` WRITE;
/*!40000 ALTER TABLE `sys_user`
    DISABLE KEYS */;
INSERT INTO `sys_user`
VALUES (1, 103, 'admin', '若依', '00', 'ry@163.com', '15888888888', '1', '',
        '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '127.0.0.1', '2026-06-05 19:22:28',
        '2026-05-08 22:05:52', 'admin', '2026-05-08 22:05:52', '', NULL, '管理员'),
       (2, 105, 'ry', '若依', '00', 'ry@qq.com', '15666666666', '1', '',
        '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '127.0.0.1', '2026-06-03 17:06:03',
        '2026-05-08 22:05:52', 'admin', '2026-05-08 22:05:52', '', NULL, '测试员');
/*!40000 ALTER TABLE `sys_user`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_user_post`
--

DROP TABLE IF EXISTS `sys_user_post`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user_post`
(
    `user_id` bigint NOT NULL COMMENT '用户ID',
    `post_id` bigint NOT NULL COMMENT '岗位ID',
    PRIMARY KEY (`user_id`, `post_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='用户与岗位关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_user_post`
--

LOCK TABLES `sys_user_post` WRITE;
/*!40000 ALTER TABLE `sys_user_post`
    DISABLE KEYS */;
INSERT INTO `sys_user_post`
VALUES (1, 1),
       (2, 2);
/*!40000 ALTER TABLE `sys_user_post`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_user_role`
--

DROP TABLE IF EXISTS `sys_user_role`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user_role`
(
    `user_id` bigint NOT NULL COMMENT '用户ID',
    `role_id` bigint NOT NULL COMMENT '角色ID',
    PRIMARY KEY (`user_id`, `role_id`),
    KEY `idx_sys_user_role_user_id` (`user_id`),
    KEY `idx_sys_user_role_role_id` (`role_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='用户和角色关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_user_role`
--

LOCK TABLES `sys_user_role` WRITE;
/*!40000 ALTER TABLE `sys_user_role`
    DISABLE KEYS */;
INSERT INTO `sys_user_role`
VALUES (1, 1),
       (1, 2),
       (2, 2);
/*!40000 ALTER TABLE `sys_user_role`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `video_device`
--

DROP TABLE IF EXISTS `video_device`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `video_device`
(
    `id`               bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `code`             varchar(100) NOT NULL COMMENT '设备编号',
    `name`             varchar(200) NOT NULL COMMENT '设备名称',
    `icon`             varchar(200) DEFAULT NULL COMMENT '图标代码',
    `icon_path`        varchar(500) DEFAULT NULL COMMENT '图标路径',
    `protocol_code`    varchar(50)  DEFAULT NULL COMMENT '协议类型编码',
    `protocol_name`    varchar(100) DEFAULT NULL COMMENT '协议类型名称',
    `stream_url`       varchar(500) DEFAULT NULL COMMENT '视频流地址',
    `status`           tinyint      DEFAULT '1' COMMENT '状态: 0-离线, 1-在线, 2-故障',
    `last_online_time` datetime     DEFAULT NULL COMMENT '最近在线时间',
    `install_time`     datetime     DEFAULT NULL COMMENT '安装时间',
    `longitude`        double       DEFAULT NULL COMMENT '经度',
    `latitude`         double       DEFAULT NULL COMMENT '纬度',
    `create_by`        varchar(64)  DEFAULT NULL COMMENT '创建者',
    `create_time`      datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`        varchar(64)  DEFAULT NULL COMMENT '更新者',
    `update_time`      datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `del_flag`         tinyint      DEFAULT '0' COMMENT '删除标记: 0-正常, 1-删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_video_device_code` (`code`),
    KEY `idx_video_device_status` (`status`),
    KEY `idx_video_device_del_flag` (`del_flag`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 2
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='视频设备表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `video_device`
--

LOCK TABLES `video_device` WRITE;
/*!40000 ALTER TABLE `video_device`
    DISABLE KEYS */;
INSERT INTO `video_device`
VALUES (1, 'test_video_device001', '测试视频设备001', 'vidio1', '/jc-icon/green/vidio1_green.png', 'RTMP', NULL,
        'https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8', 1, NULL, NULL, NULL, NULL, 'admin', '2026-05-28 19:15:12',
        'admin', '2026-05-28 19:16:56', 0);
/*!40000 ALTER TABLE `video_device`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `video_device_hazard_point`
--

DROP TABLE IF EXISTS `video_device_hazard_point`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `video_device_hazard_point`
(
    `id`                bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `video_device_id`   bigint NOT NULL COMMENT '视频设备ID',
    `hazard_point_id`   bigint NOT NULL COMMENT '隐患点ID',
    `install_longitude` decimal(10, 6) DEFAULT NULL COMMENT '安装经度',
    `install_latitude`  decimal(10, 6) DEFAULT NULL COMMENT '安装纬度',
    `bind_time`         datetime       DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
    `create_by`         varchar(64)    DEFAULT NULL COMMENT '创建者',
    `create_time`       datetime       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`         varchar(64)    DEFAULT NULL COMMENT '更新者',
    `update_time`       datetime       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_video_device_hazard_point` (`video_device_id`, `hazard_point_id`),
    KEY `idx_video_device_hp_device_id` (`video_device_id`),
    KEY `idx_video_device_hp_hp_id` (`hazard_point_id`),
    KEY `idx_vdhp_hp_bind_time` (`hazard_point_id`, `bind_time`, `video_device_id`),
    CONSTRAINT `fk_vdhp_hp` FOREIGN KEY (`hazard_point_id`) REFERENCES `hazard_point` (`id`),
    CONSTRAINT `fk_vdhp_video` FOREIGN KEY (`video_device_id`) REFERENCES `video_device` (`id`),
    CONSTRAINT `chk_vdhp_lat` CHECK (((`install_latitude` is null) or
                                      ((`install_latitude` >= -(90)) and (`install_latitude` <= 90)))),
    CONSTRAINT `chk_vdhp_lng` CHECK (((`install_longitude` is null) or
                                      ((`install_longitude` >= -(180)) and (`install_longitude` <= 180))))
) ENGINE = InnoDB
  AUTO_INCREMENT = 2
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='视频设备隐患点关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `video_device_hazard_point`
--

LOCK TABLES `video_device_hazard_point` WRITE;
/*!40000 ALTER TABLE `video_device_hazard_point`
    DISABLE KEYS */;
INSERT INTO `video_device_hazard_point`
VALUES (1, 1, 15, 104.060000, 30.670000, '2026-05-28 19:17:42', 'admin', '2026-05-28 19:17:42', NULL,
        '2026-05-28 19:17:42');
/*!40000 ALTER TABLE `video_device_hazard_point`
    ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE = @OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE = @OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS = @OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS = @OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT = @OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS = @OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION = @OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES = @OLD_SQL_NOTES */;

-- Dump completed on 2026-06-05 20:13:03
