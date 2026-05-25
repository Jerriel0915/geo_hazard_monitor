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
    `alarm_criteria_id` decimal(12, 2) DEFAULT NULL COMMENT '阈值',
    `status`            tinyint        DEFAULT '1' COMMENT '状态: 1-待处理, 2-处理中, 3-已处理, 4-已忽略',
    `create_time`       datetime       DEFAULT CURRENT_TIMESTAMP COMMENT '告警时间',
    `handle_time`       datetime       DEFAULT NULL COMMENT '处理时间',
    `handle_person`     varchar(100)   DEFAULT NULL COMMENT '处理人',
    `handle_result`     text COMMENT '处理结果',
    PRIMARY KEY (`id`),
    KEY `idx_alarm_record_hp_id` (`hazard_point_id`),
    KEY `idx_alarm_record_level` (`alarm_level`),
    KEY `idx_alarm_record_status` (`status`),
    KEY `idx_alarm_record_create_time` (`create_time`)
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
    `name`             varchar(200) NOT NULL COMMENT '设备名称',
    `icon`             varchar(200) DEFAULT NULL COMMENT '设备图标',
    `icon_path`        varchar(500) DEFAULT NULL COMMENT '图标路径',
    `status`           tinyint      DEFAULT '1' COMMENT '状态: 1-正常, 2-故障, 3-离线',
    `run_status`       tinyint      DEFAULT '0' COMMENT '运行状态: 0-未知, 1-运行中, 2-停止',
    `last_report_time` datetime     DEFAULT NULL COMMENT '最近上报时间',
    `create_by`        varchar(64)  DEFAULT NULL COMMENT '创建者',
    `create_time`      datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`        varchar(64)  DEFAULT NULL COMMENT '更新者',
    `update_time`      datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `del_flag`         tinyint      DEFAULT '0' COMMENT '删除标记: 0-正常, 1-删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_device_code` (`code`),
    KEY `idx_device_status` (`status`),
    KEY `idx_device_run_status` (`run_status`),
    KEY `idx_device_del_flag` (`del_flag`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='设备表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `device`
--

LOCK TABLES `device` WRITE;
/*!40000 ALTER TABLE `device`
    DISABLE KEYS */;
/*!40000 ALTER TABLE `device`
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
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_device_hazard_point` (`device_id`, `hazard_point_id`),
    KEY `idx_device_hazard_point_device_id` (`device_id`),
    KEY `idx_device_hazard_point_hp_id` (`hazard_point_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='设备隐患点关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `device_hazard_point`
--

LOCK TABLES `device_hazard_point` WRITE;
/*!40000 ALTER TABLE `device_hazard_point`
    DISABLE KEYS */;
/*!40000 ALTER TABLE `device_hazard_point`
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
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_device_sensor_code` (`sensor_code`),
    KEY `idx_device_sensor_device_id` (`device_id`),
    KEY `idx_device_sensor_type_id` (`monitor_type_id`),
    KEY `idx_device_sensor_status` (`status`),
    KEY `idx_device_sensor_del_flag` (`del_flag`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='传感器表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `device_sensor`
--

LOCK TABLES `device_sensor` WRITE;
/*!40000 ALTER TABLE `device_sensor`
    DISABLE KEYS */;
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
VALUES (1, 'HP001', '隐患点A修改', 2, 104.156790, 30.678902, 50.00, '修改后的描述', 1, 3, 'admin',
        '2026-05-10 16:12:29', 'admin', '2026-05-21 17:47:23', 0),
       (2, 'HP002', '龙泉寺崩塌隐患点', 1, 104.234567, 31.678901, 120.30,
        '龙泉寺后方岩质边坡，岩体破碎，存在崩塌风险，已安装裂缝计监测。', 1, 2, 'admin', '2026-05-10 16:12:29', NULL,
        '2026-05-10 16:12:29', 0),
       (3, 'HP003', '清溪乡泥石流隐患点', 2, 104.345678, 31.789012, 0.00,
        '清溪乡沟谷型泥石流隐患点，汇水面积大，暴雨季节需重点关注。', 1, 4, 'admin', '2026-05-10 16:12:29', NULL,
        '2026-05-10 16:12:29', 0),
       (4, 'HP004', '工业园区地面沉降点', 2, 104.456789, 31.890123, 0.00,
        '工业园区由于地下水位下降导致地面沉降，需持续监测地面高程变化。', 1, 2, 'admin', '2026-05-10 16:12:29', NULL,
        '2026-05-10 16:12:29', 0),
       (5, 'HP005', '顺发铁矿边坡监测点', 5, 104.567890, 31.901234, 65.00,
        '顺发铁矿露天采场边坡，高度约120米，边坡角度55度，需重点监测位移变化。', 1, 5, 'admin', '2026-05-10 16:12:29', NULL,
        '2026-05-10 18:47:39', 0),
       (6, 'HP006', '古镇危岩治理点', 3, 104.678901, 32.012345, 200.00, '古镇后山危岩体，经治理后已稳定，现处于观测期。',
        3, 1, 'admin', '2026-05-10 16:12:29', NULL, '2026-05-10 18:47:39', 0),
       (7, 'HP007', '新城基坑监测点', 2, 104.789012, 31.123456, 0.00,
        '新城建设基坑工程，因施工暂停监测，预计3个月后恢复。', 2, 0, 'admin', '2026-05-10 16:12:29', NULL,
        '2026-05-10 16:12:29', 0),
       (8, 'HP999', '我的测试', 2, 104.000000, 30.000000, 50.00, '测试', 1, 0, 'admin', '2026-05-10 17:04:51', 'admin',
        '2026-05-10 17:19:33', 2),
       (9, 'HP9999', '测试', 2, 104.000000, 30.000000, 45.00, '测试', 1, 0, 'admin', '2026-05-10 17:20:23', NULL,
        '2026-05-10 17:39:50', 2),
       (11, 'HP888', '测试', 6, 104.060004, 30.670000, 6.00, 'haidhfaib', 1, 0, 'admin', '2026-05-10 17:35:51', 'admin',
        '2026-05-10 17:39:27', 2),
       (12, 'HP777', '测试', 6, 104.060000, 30.670000, 7.00, '测试', 1, 0, 'admin', '2026-05-10 17:40:08', 'admin',
        '2026-05-21 19:54:24', 2),
       (13, 'HP008', 'test', 2, 104.060000, 30.670000, 0.00, '', 1, 0, 'admin', '2026-05-23 16:06:12', NULL,
        '2026-05-23 16:06:46', 2),
       (15, 'HP009', 'test123', NULL, 104.060000, 30.670000, 0.00, '', 1, 0, 'admin', '2026-05-23 16:07:23', 'admin',
        '2026-05-24 10:59:45', 0);
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
  AUTO_INCREMENT = 81
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='认证日志';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `log_auth_record`
--

LOCK TABLES `log_auth_record` WRITE;
/*!40000 ALTER TABLE `log_auth_record`
    DISABLE KEYS */;
INSERT INTO `log_auth_record`
VALUES (1, 910000000000000100, NULL, NULL, NULL, 'admin', 'LOGIN_SUCCESS', 'LEGACY', NULL, NULL, '127.0.0.1', '内网IP',
        'Apifox 1.0.0 / ', NULL, NULL, 'SUCCESS', NULL, '登录成功', NULL, '2026-05-08 22:16:02.000',
        '2026-05-23 18:20:59.804'),
       (2, 910000000000000101, NULL, NULL, NULL, 'admin', 'LOGIN_FAIL', 'LEGACY', NULL, NULL, '127.0.0.1', '内网IP',
        'Edge 147 / Windows >=10', NULL, NULL, 'FAIL', 'LEGACY_LOGIN_FAIL', '用户不存在/密码错误', NULL,
        '2026-05-09 19:54:34.000', '2026-05-23 18:20:59.804'),
       (3, 910000000000000102, NULL, NULL, NULL, 'admin', 'LOGIN_SUCCESS', 'LEGACY', NULL, NULL, '127.0.0.1', '内网IP',
        'Edge 147 / Windows >=10', NULL, NULL, 'SUCCESS', NULL, '登录成功', NULL, '2026-05-09 19:54:54.000',
        '2026-05-23 18:20:59.804'),
       (4, 910000000000000103, NULL, NULL, NULL, 'admin', 'LOGIN_SUCCESS', 'LEGACY', NULL, NULL, '127.0.0.1', '内网IP',
        'Edge 147 / Windows >=10', NULL, NULL, 'SUCCESS', NULL, '登录成功', NULL, '2026-05-09 20:23:58.000',
        '2026-05-23 18:20:59.804'),
       (5, 910000000000000104, NULL, NULL, NULL, 'admin', 'LOGIN_SUCCESS', 'LEGACY', NULL, NULL, '127.0.0.1', '内网IP',
        'Edge 147 / Windows >=10', NULL, NULL, 'SUCCESS', NULL, '登录成功', NULL, '2026-05-09 20:24:07.000',
        '2026-05-23 18:20:59.804'),
       (6, 910000000000000105, NULL, NULL, NULL, 'admin', 'LOGIN_SUCCESS', 'LEGACY', NULL, NULL, '127.0.0.1', '内网IP',
        'Edge 147 / Windows >=10', NULL, NULL, 'SUCCESS', NULL, '登录成功', NULL, '2026-05-09 20:24:36.000',
        '2026-05-23 18:20:59.804'),
       (7, 910000000000000106, NULL, NULL, NULL, 'admin', 'LOGIN_FAIL', 'LEGACY', NULL, NULL, '127.0.0.1', '内网IP',
        'Edge 147 / Windows >=10', NULL, NULL, 'FAIL', 'LEGACY_LOGIN_FAIL', '验证码已失效', NULL,
        '2026-05-09 20:37:49.000', '2026-05-23 18:20:59.804'),
       (8, 910000000000000107, NULL, NULL, NULL, 'admin', 'LOGIN_SUCCESS', 'LEGACY', NULL, NULL, '127.0.0.1', '内网IP',
        'Edge 147 / Windows >=10', NULL, NULL, 'SUCCESS', NULL, '登录成功', NULL, '2026-05-09 20:38:04.000',
        '2026-05-23 18:20:59.804'),
       (9, 910000000000000108, NULL, NULL, NULL, 'zwei', 'LOGIN_FAIL', 'LEGACY', NULL, NULL, '172.27.55.242', '内网IP',
        'Chrome 147 / Windows10', NULL, NULL, 'FAIL', 'LEGACY_LOGIN_FAIL', '用户不存在/密码错误', NULL,
        '2026-05-10 15:34:27.000', '2026-05-23 18:20:59.804'),
       (10, 910000000000000109, NULL, NULL, NULL, 'zwei', 'LOGIN_FAIL', 'LEGACY', NULL, NULL, '172.27.55.242', '内网IP',
        'Chrome 147 / Windows10', NULL, NULL, 'FAIL', 'LEGACY_LOGIN_FAIL', '用户不存在/密码错误', NULL,
        '2026-05-10 15:34:27.000', '2026-05-23 18:20:59.804'),
       (11, 910000000000000110, NULL, NULL, NULL, 'admin', 'LOGIN_SUCCESS', 'LEGACY', NULL, NULL, '172.27.55.242',
        '内网IP', 'Chrome 147 / Windows10', NULL, NULL, 'SUCCESS', NULL, '登录成功', NULL, '2026-05-10 15:34:50.000',
        '2026-05-23 18:20:59.804'),
       (12, 910000000000000111, NULL, NULL, NULL, 'admin', 'LOGIN_FAIL', 'LEGACY', NULL, NULL, '127.0.0.1', '内网IP',
        'Edge 148 / Windows >=10', NULL, NULL, 'FAIL', 'LEGACY_LOGIN_FAIL', '验证码已失效', NULL,
        '2026-05-10 16:04:27.000', '2026-05-23 18:20:59.804'),
       (13, 910000000000000112, NULL, NULL, NULL, 'admin', 'LOGIN_SUCCESS', 'LEGACY', NULL, NULL, '127.0.0.1', '内网IP',
        'Edge 148 / Windows >=10', NULL, NULL, 'SUCCESS', NULL, '登录成功', NULL, '2026-05-10 16:04:27.000',
        '2026-05-23 18:20:59.804'),
       (14, 910000000000000113, NULL, NULL, NULL, 'admin', 'LOGIN_SUCCESS', 'LEGACY', NULL, NULL, '172.27.55.242',
        '内网IP', 'Apifox 1.0.0 / ', NULL, NULL, 'SUCCESS', NULL, '登录成功', NULL, '2026-05-10 16:09:08.000',
        '2026-05-23 18:20:59.804'),
       (15, 910000000000000114, NULL, NULL, NULL, 'admin', 'LOGIN_SUCCESS', 'LEGACY', NULL, NULL, '127.0.0.1', '内网IP',
        'Edge 148 / Windows >=10', NULL, NULL, 'SUCCESS', NULL, '登录成功', NULL, '2026-05-10 16:29:02.000',
        '2026-05-23 18:20:59.804'),
       (16, 910000000000000115, NULL, NULL, NULL, 'admin', 'LOGIN_FAIL', 'LEGACY', NULL, NULL, '127.0.0.1', '内网IP',
        'Edge 148 / Windows >=10', NULL, NULL, 'FAIL', 'LEGACY_LOGIN_FAIL', '验证码已失效', NULL,
        '2026-05-10 16:29:02.000', '2026-05-23 18:20:59.804'),
       (17, 910000000000000116, NULL, NULL, NULL, 'admin', 'LOGIN_FAIL', 'LEGACY', NULL, NULL, '127.0.0.1', '内网IP',
        'Edge 148 / Windows >=10', NULL, NULL, 'FAIL', 'LEGACY_LOGIN_FAIL', '验证码错误', NULL,
        '2026-05-10 16:29:06.000', '2026-05-23 18:20:59.804'),
       (18, 910000000000000117, NULL, NULL, NULL, 'admin', 'LOGIN_SUCCESS', 'LEGACY', NULL, NULL, '127.0.0.1', '内网IP',
        'Edge 148 / Windows >=10', NULL, NULL, 'SUCCESS', NULL, '登录成功', NULL, '2026-05-10 16:29:44.000',
        '2026-05-23 18:20:59.804'),
       (19, 910000000000000118, NULL, NULL, NULL, 'admin', 'LOGIN_SUCCESS', 'LEGACY', NULL, NULL, '127.0.0.1', '内网IP',
        'Edge 148 / Windows >=10', NULL, NULL, 'SUCCESS', NULL, '登录成功', NULL, '2026-05-10 16:29:58.000',
        '2026-05-23 18:20:59.804'),
       (20, 910000000000000119, NULL, NULL, NULL, 'admin', 'LOGIN_SUCCESS', 'LEGACY', NULL, NULL, '127.0.0.1', '内网IP',
        'Edge 148 / Windows >=10', NULL, NULL, 'SUCCESS', NULL, '登录成功', NULL, '2026-05-10 16:30:22.000',
        '2026-05-23 18:20:59.804'),
       (21, 910000000000000120, NULL, NULL, NULL, 'admin', 'LOGIN_FAIL', 'LEGACY', NULL, NULL, '127.0.0.1', '内网IP',
        'Edge 148 / Windows >=10', NULL, NULL, 'FAIL', 'LEGACY_LOGIN_FAIL', '验证码已失效', NULL,
        '2026-05-10 16:32:14.000', '2026-05-23 18:20:59.804'),
       (22, 910000000000000121, NULL, NULL, NULL, 'admin', 'LOGIN_SUCCESS', 'LEGACY', NULL, NULL, '127.0.0.1', '内网IP',
        'Edge 148 / Windows >=10', NULL, NULL, 'SUCCESS', NULL, '登录成功', NULL, '2026-05-10 16:32:14.000',
        '2026-05-23 18:20:59.804'),
       (23, 910000000000000122, NULL, NULL, NULL, 'admin', 'LOGIN_SUCCESS', 'LEGACY', NULL, NULL, '172.27.55.242',
        '内网IP', 'Chrome 147 / Windows10', NULL, NULL, 'SUCCESS', NULL, '登录成功', NULL, '2026-05-10 16:41:33.000',
        '2026-05-23 18:20:59.804'),
       (24, 910000000000000123, NULL, NULL, NULL, 'admin', 'LOGIN_FAIL', 'LEGACY', NULL, NULL, '172.27.55.242',
        '内网IP', 'Chrome 147 / Windows10', NULL, NULL, 'FAIL', 'LEGACY_LOGIN_FAIL', '验证码已失效', NULL,
        '2026-05-10 16:41:33.000', '2026-05-23 18:20:59.804'),
       (25, 910000000000000124, NULL, NULL, NULL, 'admin', 'LOGIN_SUCCESS', 'LEGACY', NULL, NULL, '172.27.55.242',
        '内网IP', 'Chrome 147 / Windows10', NULL, NULL, 'SUCCESS', NULL, '登录成功', NULL, '2026-05-10 16:44:14.000',
        '2026-05-23 18:20:59.804'),
       (26, 910000000000000125, NULL, NULL, NULL, 'admin', 'LOGIN_SUCCESS', 'LEGACY', NULL, NULL, '172.27.55.242',
        '内网IP', 'Chrome 147 / Windows10', NULL, NULL, 'SUCCESS', NULL, '登录成功', NULL, '2026-05-10 17:57:02.000',
        '2026-05-23 18:20:59.804'),
       (27, 910000000000000126, NULL, NULL, NULL, 'admin', 'LOGIN_SUCCESS', 'LEGACY', NULL, NULL, '172.27.55.242',
        '内网IP', 'Chrome 147 / Windows10', NULL, NULL, 'SUCCESS', NULL, '登录成功', NULL, '2026-05-10 18:47:02.000',
        '2026-05-23 18:20:59.804'),
       (28, 910000000000000127, NULL, NULL, NULL, 'admin', 'LOGIN_SUCCESS', 'LEGACY', NULL, NULL, '127.0.0.1', '内网IP',
        'Edge 148 / Windows >=10', NULL, NULL, 'SUCCESS', NULL, '登录成功', NULL, '2026-05-14 23:56:11.000',
        '2026-05-23 18:20:59.804'),
       (29, 910000000000000128, NULL, NULL, NULL, 'admin', 'LOGIN_SUCCESS', 'LEGACY', NULL, NULL, '127.0.0.1', '内网IP',
        'Apifox 1.0.0 / ', NULL, NULL, 'SUCCESS', NULL, '登录成功', NULL, '2026-05-16 16:58:54.000',
        '2026-05-23 18:20:59.804'),
       (30, 910000000000000129, NULL, NULL, NULL, 'admin', 'LOGIN_SUCCESS', 'LEGACY', NULL, NULL, '127.0.0.1', '内网IP',
        'Apifox 1.0.0 / ', NULL, NULL, 'SUCCESS', NULL, '登录成功', NULL, '2026-05-21 17:37:34.000',
        '2026-05-23 18:20:59.804'),
       (31, 910000000000000130, NULL, NULL, NULL, 'admin', 'LOGIN_FAIL', 'LEGACY', NULL, NULL, '192.168.51.48',
        '内网IP', 'Chrome 148 / Windows10', NULL, NULL, 'FAIL', 'LEGACY_LOGIN_FAIL', '用户不存在/密码错误', NULL,
        '2026-05-21 19:45:09.000', '2026-05-23 18:20:59.804'),
       (32, 910000000000000131, NULL, NULL, NULL, 'admin', 'LOGIN_SUCCESS', 'LEGACY', NULL, NULL, '192.168.51.48',
        '内网IP', 'Chrome 148 / Windows10', NULL, NULL, 'SUCCESS', NULL, '登录成功', NULL, '2026-05-21 19:45:28.000',
        '2026-05-23 18:20:59.804'),
       (33, 910000000000000132, NULL, NULL, NULL, 'admin', 'LOGIN_SUCCESS', 'LEGACY', NULL, NULL, '192.168.51.48',
        '内网IP', 'Apifox 1.0.0 / ', NULL, NULL, 'SUCCESS', NULL, '登录成功', NULL, '2026-05-21 20:44:38.000',
        '2026-05-23 18:20:59.804'),
       (34, 910000000000000133, NULL, NULL, NULL, 'admin', 'LOGIN_SUCCESS', 'LEGACY', NULL, NULL, '192.168.51.64',
        '内网IP', 'Chrome 148 / Windows10', NULL, NULL, 'SUCCESS', NULL, '登录成功', NULL, '2026-05-22 10:01:09.000',
        '2026-05-23 18:20:59.804'),
       (35, 910000000000000134, NULL, NULL, NULL, 'admin', 'LOGIN_SUCCESS', 'LEGACY', NULL, NULL, '192.168.51.64',
        '内网IP', 'Chrome 148 / Windows10', NULL, NULL, 'SUCCESS', NULL, '登录成功', NULL, '2026-05-22 10:36:06.000',
        '2026-05-23 18:20:59.804'),
       (36, 910000000000000135, NULL, NULL, NULL, 'admin', 'LOGIN_SUCCESS', 'LEGACY', NULL, NULL, '192.168.51.64',
        '内网IP', 'Apifox 1.0.0 / ', NULL, NULL, 'SUCCESS', NULL, '登录成功', NULL, '2026-05-22 10:38:05.000',
        '2026-05-23 18:20:59.804'),
       (37, 910000000000000136, NULL, NULL, NULL, 'admin', 'LOGIN_SUCCESS', 'LEGACY', NULL, NULL, '192.168.51.64',
        '内网IP', 'Chrome 148 / Windows10', NULL, NULL, 'SUCCESS', NULL, '登录成功', NULL, '2026-05-22 11:41:53.000',
        '2026-05-23 18:20:59.804'),
       (38, 910000000000000137, NULL, NULL, NULL, 'admin', 'LOGIN_FAIL', 'LEGACY', NULL, NULL, '127.0.0.1', '内网IP',
        'Edge 148 / Windows >=10', NULL, NULL, 'FAIL', 'LEGACY_LOGIN_FAIL', '验证码已失效', NULL,
        '2026-05-23 01:12:18.000', '2026-05-23 18:20:59.804'),
       (39, 910000000000000138, NULL, NULL, NULL, 'admin', 'LOGIN_FAIL', 'LEGACY', NULL, NULL, '127.0.0.1', '内网IP',
        'Edge 148 / Windows >=10', NULL, NULL, 'FAIL', 'LEGACY_LOGIN_FAIL', '验证码已失效', NULL,
        '2026-05-23 01:12:38.000', '2026-05-23 18:20:59.804'),
       (40, 910000000000000139, NULL, NULL, NULL, 'admin', 'LOGIN_FAIL', 'LEGACY', NULL, NULL, '127.0.0.1', '内网IP',
        'Edge 148 / Windows >=10', NULL, NULL, 'FAIL', 'LEGACY_LOGIN_FAIL', '验证码已失效', NULL,
        '2026-05-23 01:12:52.000', '2026-05-23 18:20:59.804'),
       (41, 910000000000000140, NULL, NULL, NULL, 'admin', 'LOGIN_FAIL', 'LEGACY', NULL, NULL, '127.0.0.1', '内网IP',
        'Edge 148 / Windows >=10', NULL, NULL, 'FAIL', 'LEGACY_LOGIN_FAIL', '验证码已失效', NULL,
        '2026-05-23 01:15:33.000', '2026-05-23 18:20:59.804'),
       (42, 910000000000000141, NULL, NULL, NULL, 'admin', 'LOGIN_FAIL', 'LEGACY', NULL, NULL, '127.0.0.1', '内网IP',
        'Edge 148 / Windows >=10', NULL, NULL, 'FAIL', 'LEGACY_LOGIN_FAIL', '验证码已失效', NULL,
        '2026-05-23 01:15:37.000', '2026-05-23 18:20:59.804'),
       (43, 910000000000000142, NULL, NULL, NULL, 'admin', 'LOGIN_FAIL', 'LEGACY', NULL, NULL, '127.0.0.1', '内网IP',
        'Edge 148 / Windows >=10', NULL, NULL, 'FAIL', 'LEGACY_LOGIN_FAIL', '验证码已失效', NULL,
        '2026-05-23 01:15:50.000', '2026-05-23 18:20:59.804'),
       (44, 910000000000000143, NULL, NULL, NULL, 'admin', 'LOGIN_SUCCESS', 'LEGACY', NULL, NULL, '127.0.0.1', '内网IP',
        'Edge 148 / Windows >=10', NULL, NULL, 'SUCCESS', NULL, '登录成功', NULL, '2026-05-23 01:21:52.000',
        '2026-05-23 18:20:59.804'),
       (45, 910000000000000144, NULL, NULL, NULL, 'admin', 'LOGIN_SUCCESS', 'LEGACY', NULL, NULL, '127.0.0.1', '内网IP',
        'Edge 148 / Windows >=10', NULL, NULL, 'SUCCESS', NULL, '登录成功', NULL, '2026-05-23 14:25:42.000',
        '2026-05-23 18:20:59.804'),
       (46, 910000000000000145, NULL, NULL, NULL, 'admin', 'LOGIN_SUCCESS', 'LEGACY', NULL, NULL, '127.0.0.1', '内网IP',
        'Edge 148 / Windows >=10', NULL, NULL, 'SUCCESS', NULL, '登录成功', NULL, '2026-05-23 15:26:42.000',
        '2026-05-23 18:20:59.804'),
       (47, 910000000000000146, NULL, NULL, NULL, 'admin', 'LOGIN_FAIL', 'LEGACY', NULL, NULL, '127.0.0.1', '内网IP',
        'Edge 148 / Windows >=10', NULL, NULL, 'FAIL', 'LEGACY_LOGIN_FAIL', '验证码错误', NULL,
        '2026-05-23 15:48:36.000', '2026-05-23 18:20:59.804'),
       (48, 910000000000000147, NULL, NULL, NULL, 'admin', 'LOGIN_FAIL', 'LEGACY', NULL, NULL, '127.0.0.1', '内网IP',
        'Edge 148 / Windows >=10', NULL, NULL, 'FAIL', 'LEGACY_LOGIN_FAIL', '验证码错误', NULL,
        '2026-05-23 15:48:36.000', '2026-05-23 18:20:59.804'),
       (49, 910000000000000148, NULL, NULL, NULL, 'admin', 'LOGIN_FAIL', 'LEGACY', NULL, NULL, '127.0.0.1', '内网IP',
        'Edge 148 / Windows >=10', NULL, NULL, 'FAIL', 'LEGACY_LOGIN_FAIL', '验证码错误', NULL,
        '2026-05-23 15:48:37.000', '2026-05-23 18:20:59.804'),
       (50, 910000000000000149, NULL, NULL, NULL, 'admin', 'LOGIN_SUCCESS', 'LEGACY', NULL, NULL, '127.0.0.1', '内网IP',
        'Edge 148 / Windows >=10', NULL, NULL, 'SUCCESS', NULL, '登录成功', NULL, '2026-05-23 15:48:49.000',
        '2026-05-23 18:20:59.804'),
       (64, 1779532378298002, NULL, NULL, NULL, NULL, 'UNAUTHORIZED', 'TOKEN', '/@vite/client', 'GET', '127.0.0.1',
        '内网IP',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Trae/1.107.1 Chrome/142.0.7444.235 Electron/39.2.7 Safari/537.36',
        NULL, 401, 'FAIL', 'UNAUTHORIZED', '请求访问：/@vite/client，认证失败，无法访问系统资源', NULL,
        '2026-05-23 18:33:11.666', '2026-05-23 18:33:11.672'),
       (65, 1779532378298003, NULL, NULL, NULL, NULL, 'UNAUTHORIZED', 'TOKEN', '/@vite/client', 'GET', '127.0.0.1',
        '内网IP',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Trae/1.107.1 Chrome/142.0.7444.235 Electron/39.2.7 Safari/537.36',
        NULL, 401, 'FAIL', 'UNAUTHORIZED', '请求访问：/@vite/client，认证失败，无法访问系统资源', NULL,
        '2026-05-23 18:33:33.972', '2026-05-23 18:33:33.976'),
       (66, 1779532378298004, NULL, NULL, NULL, NULL, 'UNAUTHORIZED', 'TOKEN', '/favicon.ico', 'GET', '127.0.0.1',
        '内网IP',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36 Edg/148.0.0.0',
        NULL, 401, 'FAIL', 'UNAUTHORIZED', '请求访问：/favicon.ico，认证失败，无法访问系统资源', NULL,
        '2026-05-23 18:33:40.082', '2026-05-23 18:33:40.088'),
       (67, 1779533020098002, NULL, NULL, NULL, NULL, 'UNAUTHORIZED', 'TOKEN', '/api/v1/logs/runtime/page', 'GET',
        '127.0.0.1', '内网IP', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.8457',
        NULL, 401, 'FAIL', 'UNAUTHORIZED', '请求访问：/api/v1/logs/runtime/page，认证失败，无法访问系统资源', NULL,
        '2026-05-23 18:43:46.718', '2026-05-23 18:43:46.723'),
       (68, 1779533020098011, '40c11ddc545d4ca9a53c0c996147131a', '92cb7b1034324967b821959837808ac0', NULL, 'admin',
        'LOGIN_SUCCESS', 'PASSWORD', '/api/v1/auth/login', 'POST', '127.0.0.1', '内网IP',
        'Apifox/1.0.0 (https://apifox.com)', NULL, NULL, 'SUCCESS', NULL, '登录成功', NULL, '2026-05-23 18:47:03.083',
        '2026-05-23 18:47:03.090'),
       (69, 1779533020098017, NULL, NULL, NULL, NULL, 'UNAUTHORIZED', 'TOKEN', '/api/v1/logs/runtime/page', 'GET',
        '127.0.0.1', '内网IP', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.8457',
        NULL, 401, 'FAIL', 'UNAUTHORIZED', '请求访问：/api/v1/logs/runtime/page，认证失败，无法访问系统资源', NULL,
        '2026-05-23 18:48:14.248', '2026-05-23 18:48:14.252'),
       (70, 1779533553906002, NULL, NULL, NULL, NULL, 'UNAUTHORIZED', 'TOKEN', '/api/v1/logs/runtime/page', 'GET',
        '127.0.0.1', '内网IP', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.8457',
        NULL, 401, 'FAIL', 'UNAUTHORIZED', '请求访问：/api/v1/logs/runtime/page，认证失败，无法访问系统资源', NULL,
        '2026-05-23 18:52:51.758', '2026-05-23 18:52:51.763'),
       (71, 1779533746895002, NULL, NULL, NULL, NULL, 'UNAUTHORIZED', 'TOKEN', '/api/v1/logs/runtime/page', 'GET',
        '127.0.0.1', '内网IP', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.8457',
        NULL, 401, 'FAIL', 'UNAUTHORIZED', '请求访问：/api/v1/logs/runtime/page，认证失败，无法访问系统资源', NULL,
        '2026-05-23 18:56:18.168', '2026-05-23 18:56:18.175'),
       (72, 1779533746895005, NULL, NULL, NULL, NULL, 'UNAUTHORIZED', 'TOKEN', '/error', 'GET', '127.0.0.1', '内网IP',
        'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.8457', NULL, 401, 'FAIL',
        'UNAUTHORIZED', '请求访问：/error，认证失败，无法访问系统资源', NULL, '2026-05-23 18:56:18.248',
        '2026-05-23 18:56:18.265'),
       (73, 1779533746895007, NULL, NULL, NULL, NULL, 'UNAUTHORIZED', 'TOKEN', '/api/v1/logs/runtime/page', 'GET',
        '127.0.0.1', '内网IP', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.8457',
        NULL, 401, 'FAIL', 'UNAUTHORIZED', '请求访问：/api/v1/logs/runtime/page，认证失败，无法访问系统资源', NULL,
        '2026-05-23 18:56:36.131', '2026-05-23 18:56:36.135'),
       (74, 921779534004781002, NULL, NULL, NULL, NULL, 'UNAUTHORIZED', 'TOKEN', '/api/v1/logs/runtime/page', 'GET',
        '127.0.0.1', '内网IP', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.8457',
        NULL, 401, 'FAIL', 'UNAUTHORIZED', '请求访问：/api/v1/logs/runtime/page，认证失败，无法访问系统资源', NULL,
        '2026-05-23 19:00:41.677', '2026-05-23 19:00:41.681'),
       (75, 921779534004781003, NULL, NULL, NULL, NULL, 'UNAUTHORIZED', 'TOKEN', '/api/v1/logs/runtime/page', 'GET',
        '127.0.0.1', '内网IP', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.8457',
        NULL, 401, 'FAIL', 'UNAUTHORIZED', '请求访问：/api/v1/logs/runtime/page，认证失败，无法访问系统资源', NULL,
        '2026-05-23 19:00:59.326', '2026-05-23 19:00:59.333'),
       (76, 921779534004781006, NULL, NULL, NULL, NULL, 'UNAUTHORIZED', 'TOKEN', '/api/v1/logs/runtime/page', 'GET',
        '127.0.0.1', '内网IP', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.8457',
        NULL, 401, 'FAIL', 'UNAUTHORIZED', '请求访问：/api/v1/logs/runtime/page，认证失败，无法访问系统资源', NULL,
        '2026-05-23 19:01:29.473', '2026-05-23 19:01:29.477'),
       (77, 921779534004781010, NULL, NULL, NULL, NULL, 'UNAUTHORIZED', 'TOKEN', '/error', 'GET', '127.0.0.1', '内网IP',
        'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.8457', NULL, 401, 'FAIL',
        'UNAUTHORIZED', '请求访问：/error，认证失败，无法访问系统资源', NULL, '2026-05-23 19:01:29.503',
        '2026-05-23 19:01:29.511'),
       (78, 921779538572496001, NULL, NULL, NULL, 'admin', 'LOGIN_SUCCESS', 'PASSWORD', '/api/v1/auth/login', 'POST',
        '127.0.0.1', '内网IP',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36 Edg/148.0.0.0',
        NULL, NULL, 'SUCCESS', NULL, '登录成功', NULL, '2026-05-23 20:16:34.115', '2026-05-23 20:16:34.513'),
       (79, 921779589853561001, NULL, NULL, NULL, 'admin', 'LOGIN_SUCCESS', 'PASSWORD', '/api/v1/auth/login', 'POST',
        '127.0.0.1', '内网IP',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36 Edg/148.0.0.0',
        NULL, NULL, 'SUCCESS', NULL, '登录成功', NULL, '2026-05-24 10:32:26.386', '2026-05-24 10:32:26.674'),
       (80, 921779622864396001, NULL, NULL, NULL, 'admin', 'LOGIN_SUCCESS', 'PASSWORD', '/api/v1/auth/login', 'POST',
        '127.0.0.1', '内网IP',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36 Edg/148.0.0.0',
        NULL, NULL, 'SUCCESS', NULL, '登录成功', NULL, '2026-05-24 19:41:29.137', '2026-05-24 19:41:29.390');
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
  AUTO_INCREMENT = 145
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='接口调用日志';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `log_operation_record`
--

LOCK TABLES `log_operation_record` WRITE;
/*!40000 ALTER TABLE `log_operation_record`
    DISABLE KEYS */;
INSERT INTO `log_operation_record`
VALUES (1, 900000000000000100, NULL, NULL, NULL, 'admin', '研发部门', '隐患点管理', '1', '/api/v1/hazard-points',
        'POST', 'com.zwei.iot.hazard_point.HazardPointController.add()', '172.27.55.242', '内网IP', NULL,
        '{\"code\":\"HP999\",\"createBy\":\"admin\",\"description\":\"测试新增\",\"groupId\":1,\"id\":8,\"latitude\":30.678901,\"longitude\":104.156789,\"name\":\"测试隐患点\",\"params\":{},\"status\":1,\"strike\":45} ',
        '{\"msg\":\"操作成功\",\"code\":200,\"timestamp\":1778403891184}', NULL, 'SUCCESS', NULL, 83,
        '2026-05-10 17:04:51.000', '2026-05-23 18:20:59.686'),
       (2, 900000000000000101, NULL, NULL, NULL, 'admin', '研发部门', '隐患点管理', '1', '/api/v1/hazard-points',
        'POST', 'com.zwei.iot.hazard_point.HazardPointController.add()', '172.27.55.242', '内网IP', NULL,
        '{\"code\":\"HP999\",\"description\":\"测试新增\",\"groupId\":1,\"latitude\":30.678901,\"longitude\":104.156789,\"name\":\"测试隐患点\",\"params\":{},\"strike\":45} ',
        '{\"msg\":\"新增隐患点失败，编号已存在\",\"code\":500,\"timestamp\":1778404103111}', NULL, 'SUCCESS', NULL, 19,
        '2026-05-10 17:08:23.000', '2026-05-23 18:20:59.686'),
       (3, 900000000000000102, NULL, NULL, NULL, 'admin', '研发部门', '隐患点管理', '2', '/api/v1/hazard-points/1',
        'PUT', 'com.zwei.iot.hazard_point.HazardPointController.edit()', '172.27.55.242', '内网IP', NULL,
        '1 {\"description\":\"修改后的描述\",\"groupId\":2,\"id\":1,\"latitude\":30.678902,\"longitude\":104.15679,\"name\":\"隐患点A修改\",\"params\":{},\"strike\":50,\"updateBy\":\"admin\"} ',
        '{\"msg\":\"操作成功\",\"code\":200,\"timestamp\":1778404106332}', NULL, 'SUCCESS', NULL, 11,
        '2026-05-10 17:08:26.000', '2026-05-23 18:20:59.686'),
       (4, 900000000000000103, NULL, NULL, NULL, 'admin', '研发部门', '隐患点管理', '2', '/api/v1/hazard-points/8',
        'PUT', 'com.zwei.iot.hazard_point.HazardPointController.edit()', '172.27.55.242', '内网IP', NULL,
        '8 {\"description\":\"测试\",\"groupId\":2,\"id\":8,\"latitude\":30,\"longitude\":104,\"name\":\"我的测试\",\"params\":{},\"strike\":50,\"updateBy\":\"admin\"} ',
        '{\"msg\":\"操作成功\",\"code\":200,\"timestamp\":1778404453337}', NULL, 'SUCCESS', NULL, 9,
        '2026-05-10 17:14:13.000', '2026-05-23 18:20:59.686'),
       (5, 900000000000000104, NULL, NULL, NULL, 'admin', '研发部门', '隐患点管理', '3', '/api/v1/hazard-points/8',
        'DELETE', 'com.zwei.iot.hazard_point.HazardPointController.remove()', '172.27.55.242', '内网IP', NULL, '8 ',
        '{\"msg\":\"操作成功\",\"code\":200,\"timestamp\":1778404773495}', NULL, 'SUCCESS', NULL, 31,
        '2026-05-10 17:19:33.000', '2026-05-23 18:20:59.686'),
       (6, 900000000000000105, NULL, NULL, NULL, 'admin', '研发部门', '隐患点管理', '1', '/api/v1/hazard-points',
        'POST', 'com.zwei.iot.hazard_point.HazardPointController.add()', '172.27.55.242', '内网IP', NULL,
        '{\"code\":\"HP9999\",\"createBy\":\"admin\",\"description\":\"测试\",\"groupId\":2,\"id\":9,\"latitude\":30,\"longitude\":104,\"name\":\"测试\",\"params\":{},\"status\":1,\"strike\":45} ',
        '{\"msg\":\"操作成功\",\"code\":200,\"timestamp\":1778404823181}', NULL, 'SUCCESS', NULL, 22,
        '2026-05-10 17:20:23.000', '2026-05-23 18:20:59.686'),
       (7, 900000000000000106, NULL, NULL, NULL, 'admin', '研发部门', '隐患点管理', '1', '/api/v1/hazard-points',
        'POST', 'com.zwei.iot.hazard_point.HazardPointController.add()', '172.27.55.242', '内网IP', NULL,
        '{\"code\":\"HP999\",\"createBy\":\"admin\",\"description\":\"我的测试\",\"groupId\":2,\"latitude\":30.67,\"longitude\":104.060004,\"name\":\"正式测试\",\"params\":{},\"status\":1,\"strike\":7} ',
        NULL, NULL, 'FAIL',
        '\r\n### Error updating database.  Cause: java.sql.SQLIntegrityConstraintViolationException: Duplicate entry \'HP999\' for key \'hazard_point.uk_hazard_point_code\'\r\n### The error may exist in file [D:\\Code\\Projects\\geo_hazard_monitor\\server\\zwei-iot\\target\\classes\\mapper\\iot\\HazardPointMapper.xml]\r\n### The error may involve com.zwei.iot.mapper.HazardPointMapper.insertHazardPoint-Inline\r\n### The error occurred while setting parameters\r\n### SQL: INSERT INTO hazard_point (              code,               name,               group_id,                             longitude,               latitude,               strike,               description,               status,               create_by,              create_time         ) VALUES (              ?,               ?,               ?,                             ?,               ?,               ?,               ?,               ?,               ?,              NOW()         )\r\n### Cause: java.sql.SQLIntegrityConstraintViolationException: Duplicate entry \'HP999\' for key \'hazard_point.uk_hazard_point_code\'\n; Duplicate entry \'HP999\' for key \'hazard_point.uk_hazard_point_code\'',
        227, '2026-05-10 17:35:35.000', '2026-05-23 18:20:59.686'),
       (8, 900000000000000107, NULL, NULL, NULL, 'admin', '研发部门', '隐患点管理', '1', '/api/v1/hazard-points',
        'POST', 'com.zwei.iot.hazard_point.HazardPointController.add()', '172.27.55.242', '内网IP', NULL,
        '{\"code\":\"HP888\",\"createBy\":\"admin\",\"description\":\"我的测试\",\"groupId\":2,\"id\":11,\"latitude\":30.67,\"longitude\":104.060004,\"name\":\"正式测试\",\"params\":{},\"status\":1,\"strike\":7} ',
        '{\"msg\":\"操作成功\",\"code\":200,\"timestamp\":1778405751429}', NULL, 'SUCCESS', NULL, 46,
        '2026-05-10 17:35:51.000', '2026-05-23 18:20:59.686'),
       (9, 900000000000000108, NULL, NULL, NULL, 'admin', '研发部门', '隐患点管理', '2', '/api/v1/hazard-points/11',
        'PUT', 'com.zwei.iot.hazard_point.HazardPointController.edit()', '172.27.55.242', '内网IP', NULL,
        '11 {\"description\":\"haidhfaib\",\"groupId\":5,\"id\":11,\"latitude\":30.67,\"longitude\":104.060004,\"name\":\"正式测试\",\"params\":{},\"strike\":7,\"updateBy\":\"admin\"} ',
        '{\"msg\":\"操作成功\",\"code\":200,\"timestamp\":1778405793607}', NULL, 'SUCCESS', NULL, 8,
        '2026-05-10 17:36:33.000', '2026-05-23 18:20:59.686'),
       (10, 900000000000000109, NULL, NULL, NULL, 'admin', '研发部门', '隐患点管理', '2', '/api/v1/hazard-points/11',
        'PUT', 'com.zwei.iot.hazard_point.HazardPointController.edit()', '172.27.55.242', '内网IP', NULL,
        '11 {\"description\":\"haidhfaib\",\"groupId\":6,\"id\":11,\"latitude\":30.67,\"longitude\":104.060004,\"name\":\"测试\",\"params\":{},\"strike\":6,\"updateBy\":\"admin\"} ',
        '{\"msg\":\"操作成功\",\"code\":200,\"timestamp\":1778405844144}', NULL, 'SUCCESS', NULL, 9,
        '2026-05-10 17:37:24.000', '2026-05-23 18:20:59.686'),
       (11, 900000000000000110, NULL, NULL, NULL, 'admin', '研发部门', '隐患点管理', '3', '/api/v1/hazard-points/11',
        'DELETE', 'com.zwei.iot.hazard_point.HazardPointController.remove()', '172.27.55.242', '内网IP', NULL, '11 ',
        '{\"msg\":\"操作成功\",\"code\":200,\"timestamp\":1778405967298}', NULL, 'SUCCESS', NULL, 8,
        '2026-05-10 17:39:27.000', '2026-05-23 18:20:59.686'),
       (12, 900000000000000111, NULL, NULL, NULL, 'admin', '研发部门', '隐患点管理', '3', '/api/v1/hazard-points/9',
        'DELETE', 'com.zwei.iot.hazard_point.HazardPointController.remove()', '172.27.55.242', '内网IP', NULL, '9 ',
        '{\"msg\":\"操作成功\",\"code\":200,\"timestamp\":1778405990192}', NULL, 'SUCCESS', NULL, 6,
        '2026-05-10 17:39:50.000', '2026-05-23 18:20:59.686'),
       (13, 900000000000000112, NULL, NULL, NULL, 'admin', '研发部门', '隐患点管理', '1', '/api/v1/hazard-points',
        'POST', 'com.zwei.iot.hazard_point.HazardPointController.add()', '172.27.55.242', '内网IP', NULL,
        '{\"code\":\"HP777\",\"createBy\":\"admin\",\"description\":\"测试\",\"groupId\":2,\"id\":12,\"latitude\":30.67,\"longitude\":104.06,\"name\":\"测试\",\"params\":{},\"status\":1,\"strike\":7} ',
        '{\"msg\":\"操作成功\",\"code\":200,\"timestamp\":1778406008221}', NULL, 'SUCCESS', NULL, 18,
        '2026-05-10 17:40:08.000', '2026-05-23 18:20:59.686'),
       (14, 900000000000000113, NULL, NULL, NULL, 'admin', '研发部门', '隐患点管理', '2', '/api/v1/hazard-points/12',
        'PUT', 'com.zwei.iot.controller.HazardPointController.edit()', '172.27.55.242', '内网IP', NULL,
        '12 {\"description\":\"测试\",\"groupId\":4,\"id\":12,\"latitude\":30.67,\"longitude\":104.06,\"name\":\"测试\",\"params\":{},\"strike\":7,\"updateBy\":\"admin\"} ',
        '{\"msg\":\"操作成功\",\"code\":200,\"timestamp\":1778411120985}', NULL, 'SUCCESS', NULL, 54,
        '2026-05-10 19:05:21.000', '2026-05-23 18:20:59.686'),
       (15, 900000000000000114, NULL, NULL, NULL, 'admin', '研发部门', '隐患点管理', '2', '/api/v1/hazard-points/12',
        'PUT', 'com.zwei.iot.controller.HazardPointController.edit()', '172.27.55.242', '内网IP', NULL,
        '12 {\"description\":\"测试\",\"groupId\":6,\"id\":12,\"latitude\":30.67,\"longitude\":104.06,\"name\":\"测试\",\"params\":{},\"strike\":7,\"updateBy\":\"admin\"} ',
        '{\"msg\":\"操作成功\",\"code\":200,\"timestamp\":1778411531139}', NULL, 'SUCCESS', NULL, 30,
        '2026-05-10 19:12:11.000', '2026-05-23 18:20:59.686'),
       (16, 900000000000000115, NULL, NULL, NULL, 'admin', '研发部门', '隐患点分组', '1', '/api/v1/hazard-point-groups',
        'POST', 'com.zwei.iot.controller.HazardPointGroupController.add()', '172.27.55.242', '内网IP', NULL,
        '{\"code\":\"G009\",\"createBy\":\"admin\",\"description\":\"测试\",\"id\":7,\"name\":\"测试分组\",\"params\":{},\"sortOrder\":10,\"status\":1} ',
        '{\"msg\":\"操作成功\",\"code\":200,\"data\":7,\"timestamp\":1778411715806}', NULL, 'SUCCESS', NULL, 23,
        '2026-05-10 19:15:15.000', '2026-05-23 18:20:59.686'),
       (17, 900000000000000116, NULL, NULL, NULL, 'admin', '研发部门', '隐患点分组', '2',
        '/api/v1/hazard-point-groups/6', 'PUT', 'com.zwei.iot.controller.HazardPointGroupController.edit()',
        '172.27.55.242', '内网IP', NULL,
        '6 {\"description\":\"地址测试\",\"id\":6,\"name\":\"地址测试\",\"params\":{},\"sortOrder\":10,\"status\":1,\"updateBy\":\"admin\"} ',
        '{\"msg\":\"操作成功\",\"code\":200,\"timestamp\":1778411932942}', NULL, 'SUCCESS', NULL, 23,
        '2026-05-10 19:18:52.000', '2026-05-23 18:20:59.686'),
       (18, 900000000000000117, NULL, NULL, NULL, 'admin', '研发部门', '隐患点分组', '3',
        '/api/v1/hazard-point-groups/7', 'DELETE', 'com.zwei.iot.controller.HazardPointGroupController.remove()',
        '172.27.55.242', '内网IP', NULL, '7 ', '{\"msg\":\"操作成功\",\"code\":200,\"timestamp\":1778411978765}', NULL,
        'SUCCESS', NULL, 10, '2026-05-10 19:19:38.000', '2026-05-23 18:20:59.686'),
       (19, 900000000000000118, NULL, NULL, NULL, 'admin', '研发部门', '隐患点分组', '1', '/api/v1/hazard-point-groups',
        'POST', 'com.zwei.iot.controller.HazardPointGroupController.add()', '172.27.55.242', '内网IP', NULL,
        '{\"code\":\"G1778412223257\",\"createBy\":\"admin\",\"description\":\"收缩不啊对吧\",\"id\":8,\"name\":\"测试\",\"params\":{},\"sortOrder\":4,\"status\":1} ',
        '{\"msg\":\"操作成功\",\"code\":200,\"data\":8,\"timestamp\":1778412229381}', NULL, 'SUCCESS', NULL, 11,
        '2026-05-10 19:23:49.000', '2026-05-23 18:20:59.686'),
       (20, 900000000000000119, NULL, NULL, NULL, 'admin', '研发部门', '隐患点分组', '2',
        '/api/v1/hazard-point-groups/8', 'PUT', 'com.zwei.iot.controller.HazardPointGroupController.edit()',
        '172.27.55.242', '内网IP', NULL,
        '8 {\"description\":\"收缩不啊对吧\",\"id\":8,\"name\":\"测试\",\"params\":{},\"sortOrder\":4,\"status\":1,\"updateBy\":\"admin\"} ',
        '{\"msg\":\"操作成功\",\"code\":200,\"timestamp\":1778412366783}', NULL, 'SUCCESS', NULL, 43,
        '2026-05-10 19:26:06.000', '2026-05-23 18:20:59.686'),
       (21, 900000000000000120, NULL, NULL, NULL, 'admin', '研发部门', '隐患点分组', '3',
        '/api/v1/hazard-point-groups/8', 'DELETE', 'com.zwei.iot.controller.HazardPointGroupController.remove()',
        '172.27.55.242', '内网IP', NULL, '8 ', '{\"msg\":\"操作成功\",\"code\":200,\"timestamp\":1778412373000}', NULL,
        'SUCCESS', NULL, 14, '2026-05-10 19:26:13.000', '2026-05-23 18:20:59.686'),
       (22, 900000000000000121, NULL, NULL, NULL, 'admin', '研发部门', '隐患点管理', '2',
        '/api/v1/hazard-points/batch/operate', 'PUT', 'com.zwei.iot.controller.HazardPointController.batchOperate()',
        '127.0.0.1', '内网IP', NULL, '{\"ids\":[12],\"operation\":\"pause\"} ', NULL, NULL, 'FAIL',
        'arraycopy: element type mismatch: can not cast one of the elements of java.lang.Object[] to the type of the destination array, java.lang.Long',
        36, '2026-05-15 00:13:37.000', '2026-05-23 18:20:59.686'),
       (23, 900000000000000122, NULL, NULL, NULL, 'admin', '研发部门', '隐患点管理', '2',
        '/api/v1/hazard-points/batch/operate', 'PUT', 'com.zwei.iot.controller.HazardPointController.batchOperate()',
        '127.0.0.1', '内网IP', NULL, '{\"ids\":[12],\"operation\":\"pause\"} ', NULL, NULL, 'FAIL',
        '\r\n### Error updating database.  Cause: org.apache.ibatis.binding.BindingException: Parameter \'list\' not found. Available parameters are [ids, param1, status, param2]\r\n### The error may exist in file [D:\\Code\\Projects\\geo_hazard_monitor\\server\\zwei-iot\\target\\classes\\mapper\\iot\\HazardPointMapper.xml]\r\n### The error may involve com.zwei.iot.mapper.HazardPointMapper.batchUpdateHazardPointStatus\r\n### The error occurred while executing an update\r\n### Cause: org.apache.ibatis.binding.BindingException: Parameter \'list\' not found. Available parameters are [ids, param1, status, param2]',
        27, '2026-05-15 00:16:43.000', '2026-05-23 18:20:59.686'),
       (24, 900000000000000123, NULL, NULL, NULL, 'admin', '研发部门', '隐患点管理', '2',
        '/api/v1/hazard-points/batch/operate', 'PUT', 'com.zwei.iot.controller.HazardPointController.batchOperate()',
        '127.0.0.1', '内网IP', NULL, '{\"ids\":[12],\"operation\":\"pause\"} ',
        '{\"msg\":\"操作成功\",\"code\":200,\"timestamp\":1778775519293}', NULL, 'SUCCESS', NULL, 61,
        '2026-05-15 00:18:39.000', '2026-05-23 18:20:59.686'),
       (25, 900000000000000124, NULL, NULL, NULL, 'admin', '研发部门', '隐患点管理', '2',
        '/api/v1/hazard-points/batch/operate', 'PUT', 'com.zwei.iot.controller.HazardPointController.batchOperate()',
        '127.0.0.1', '内网IP', NULL, '{\"ids\":[12],\"operation\":\"resume\"} ',
        '{\"msg\":\"操作成功\",\"code\":200,\"timestamp\":1778775527352}', NULL, 'SUCCESS', NULL, 9,
        '2026-05-15 00:18:47.000', '2026-05-23 18:20:59.686'),
       (26, 900000000000000125, NULL, NULL, NULL, 'admin', '研发部门', '隐患点管理', '2',
        '/api/v1/hazard-points/1/pause', 'PUT', 'com.zwei.iot.hazardpoint.controller.HazardPointController.pause()',
        '127.0.0.1', '内网IP', NULL, '1 true ', '{\"msg\":\"操作成功\",\"code\":200,\"timestamp\":1779356674255}', NULL,
        'SUCCESS', NULL, 142, '2026-05-21 17:44:34.000', '2026-05-23 18:20:59.686'),
       (27, 900000000000000126, NULL, NULL, NULL, 'admin', '研发部门', '隐患点管理', '2',
        '/api/v1/hazard-points/1/pause', 'PUT', 'com.zwei.iot.hazardpoint.controller.HazardPointController.pause()',
        '127.0.0.1', '内网IP', NULL, '1 true ', '{\"msg\":\"操作成功\",\"code\":200,\"timestamp\":1779356780855}', NULL,
        'SUCCESS', NULL, 13, '2026-05-21 17:46:20.000', '2026-05-23 18:20:59.686'),
       (28, 900000000000000127, NULL, NULL, NULL, 'admin', '研发部门', '隐患点管理', '2',
        '/api/v1/hazard-points/1/pause', 'PUT', 'com.zwei.iot.hazardpoint.controller.HazardPointController.pause()',
        '127.0.0.1', '内网IP', NULL, '1 false ', '{\"msg\":\"操作成功\",\"code\":200,\"timestamp\":1779356843919}',
        NULL, 'SUCCESS', NULL, 10, '2026-05-21 17:47:23.000', '2026-05-23 18:20:59.686'),
       (29, 900000000000000128, NULL, NULL, NULL, 'admin', '研发部门', '隐患点管理', '1', '/api/v1/hazard-points',
        'POST', 'com.zwei.iot.hazardpoint.controller.HazardPointController.add()', '192.168.51.48', '内网IP', NULL,
        '{\"code\":\"HP23165\",\"description\":\"测试\",\"groupId\":3,\"latitude\":30.67,\"longitude\":104.06,\"name\":\"测试2\",\"params\":{},\"strike\":0} ',
        NULL, NULL, 'FAIL',
        '\r\n### Error querying database.  Cause: java.sql.SQLSyntaxErrorException: You have an error in your SQL syntax; check the manual that corresponds to your MySQL server version for the right syntax to use near \'WHERE h.del_flag = 0 AND h.code = \'HP23165\'\' at line 13\r\n### The error may exist in file [D:\\Code\\Projects\\geo_hazard_monitor\\server\\zwei-iot\\target\\classes\\mapper\\iot\\hazardPoint\\HazardPointMapper.xml]\r\n### The error may involve com.zwei.iot.hazardpoint.mapper.HazardPointMapper.checkHazardPointCodeUnique-Inline\r\n### The error occurred while setting parameters\r\n### SQL: SELECT h.id, h.code, h.name, h.group_id, h.group_name, h.longitude, h.latitude, h.strike,                h.description, h.status, h.device_count, h.del_flag, h.create_by, h.create_time,                h.update_by, h.update_time,                CASE h.status                    WHEN 1 THEN \'监测中\'                    WHEN 2 THEN \'停测中\'                    WHEN 3 THEN \'已完结\'                    ELSE \'\'                END AS status_name         FROM hazard_point h         WHERE h.del_flag = \'0\'               WHERE h.del_flag = 0 AND h.code = ?\r\n### Cause: java.sql.SQLSyntaxErrorException: You have an error in your SQL syntax; check the manual that corresponds to your MySQL server version for the right syntax to use near \'WHERE h.del_flag = 0 AND h.code = \'HP23165\'\' at line 13\n; bad SQL grammar []',
        266, '2026-05-21 19:50:41.000', '2026-05-23 18:20:59.686'),
       (30, 900000000000000129, NULL, NULL, NULL, 'admin', '研发部门', '隐患点管理', '1', '/api/v1/hazard-points',
        'POST', 'com.zwei.iot.hazardpoint.controller.HazardPointController.add()', '192.168.51.48', '内网IP', NULL,
        '{\"code\":\"HP23165\",\"description\":\"测试\",\"groupId\":3,\"latitude\":30.67,\"longitude\":104.06,\"name\":\"测试2\",\"params\":{},\"strike\":0} ',
        NULL, NULL, 'FAIL',
        '\r\n### Error querying database.  Cause: java.sql.SQLSyntaxErrorException: You have an error in your SQL syntax; check the manual that corresponds to your MySQL server version for the right syntax to use near \'WHERE h.del_flag = 0 AND h.code = \'HP23165\'\' at line 13\r\n### The error may exist in file [D:\\Code\\Projects\\geo_hazard_monitor\\server\\zwei-iot\\target\\classes\\mapper\\iot\\hazardPoint\\HazardPointMapper.xml]\r\n### The error may involve com.zwei.iot.hazardpoint.mapper.HazardPointMapper.checkHazardPointCodeUnique-Inline\r\n### The error occurred while setting parameters\r\n### SQL: SELECT h.id, h.code, h.name, h.group_id, h.group_name, h.longitude, h.latitude, h.strike,                h.description, h.status, h.device_count, h.del_flag, h.create_by, h.create_time,                h.update_by, h.update_time,                CASE h.status                    WHEN 1 THEN \'监测中\'                    WHEN 2 THEN \'停测中\'                    WHEN 3 THEN \'已完结\'                    ELSE \'\'                END AS status_name         FROM hazard_point h         WHERE h.del_flag = \'0\'               WHERE h.del_flag = 0 AND h.code = ?\r\n### Cause: java.sql.SQLSyntaxErrorException: You have an error in your SQL syntax; check the manual that corresponds to your MySQL server version for the right syntax to use near \'WHERE h.del_flag = 0 AND h.code = \'HP23165\'\' at line 13\n; bad SQL grammar []',
        6, '2026-05-21 19:50:48.000', '2026-05-23 18:20:59.686'),
       (31, 900000000000000130, NULL, NULL, NULL, 'admin', '研发部门', '隐患点管理', '1', '/api/v1/hazard-points',
        'POST', 'com.zwei.iot.hazardpoint.controller.HazardPointController.add()', '192.168.51.48', '内网IP', NULL,
        '{\"code\":\"HP23165\",\"description\":\"测试\",\"groupId\":3,\"latitude\":30.67,\"longitude\":104.06,\"name\":\"测试2\",\"params\":{},\"strike\":0} ',
        NULL, NULL, 'FAIL',
        '\r\n### Error querying database.  Cause: java.sql.SQLSyntaxErrorException: You have an error in your SQL syntax; check the manual that corresponds to your MySQL server version for the right syntax to use near \'WHERE h.del_flag = 0 AND h.code = \'HP23165\'\' at line 13\r\n### The error may exist in file [D:\\Code\\Projects\\geo_hazard_monitor\\server\\zwei-iot\\target\\classes\\mapper\\iot\\hazardPoint\\HazardPointMapper.xml]\r\n### The error may involve com.zwei.iot.hazardpoint.mapper.HazardPointMapper.checkHazardPointCodeUnique-Inline\r\n### The error occurred while setting parameters\r\n### SQL: SELECT h.id, h.code, h.name, h.group_id, h.group_name, h.longitude, h.latitude, h.strike,                h.description, h.status, h.device_count, h.del_flag, h.create_by, h.create_time,                h.update_by, h.update_time,                CASE h.status                    WHEN 1 THEN \'监测中\'                    WHEN 2 THEN \'停测中\'                    WHEN 3 THEN \'已完结\'                    ELSE \'\'                END AS status_name         FROM hazard_point h         WHERE h.del_flag = \'0\'               WHERE h.del_flag = 0 AND h.code = ?\r\n### Cause: java.sql.SQLSyntaxErrorException: You have an error in your SQL syntax; check the manual that corresponds to your MySQL server version for the right syntax to use near \'WHERE h.del_flag = 0 AND h.code = \'HP23165\'\' at line 13\n; bad SQL grammar []',
        7, '2026-05-21 19:50:49.000', '2026-05-23 18:20:59.686'),
       (32, 900000000000000131, NULL, NULL, NULL, 'admin', '研发部门', '隐患点管理', '3', '/api/v1/hazard-points/12',
        'DELETE', 'com.zwei.iot.hazardpoint.controller.HazardPointController.remove()', '192.168.51.48', '内网IP', NULL,
        '12 ', '{\"msg\":\"操作成功\",\"code\":200,\"timestamp\":1779364464644}', NULL, 'SUCCESS', NULL, 150,
        '2026-05-21 19:54:24.000', '2026-05-23 18:20:59.686'),
       (33, 900000000000000132, NULL, NULL, NULL, 'admin', '研发部门', '隐患点管理', '1', '/api/v1/hazard-points',
        'POST', 'com.zwei.iot.hazardpoint.controller.HazardPointController.add()', '192.168.51.48', '内网IP', NULL,
        '{\"code\":\"HP22\",\"description\":\"\",\"groupId\":2,\"latitude\":30.67,\"longitude\":104.06,\"name\":\"ces\",\"params\":{},\"strike\":0} ',
        NULL, NULL, 'FAIL',
        '\r\n### Error querying database.  Cause: java.sql.SQLSyntaxErrorException: You have an error in your SQL syntax; check the manual that corresponds to your MySQL server version for the right syntax to use near \'WHERE h.del_flag = 0 AND h.code = \'HP22\'\' at line 13\r\n### The error may exist in file [D:\\Code\\Projects\\geo_hazard_monitor\\server\\zwei-iot\\target\\classes\\mapper\\iot\\hazardPoint\\HazardPointMapper.xml]\r\n### The error may involve com.zwei.iot.hazardpoint.mapper.HazardPointMapper.checkHazardPointCodeUnique-Inline\r\n### The error occurred while setting parameters\r\n### SQL: SELECT h.id, h.code, h.name, h.group_id, h.group_name, h.longitude, h.latitude, h.strike,                h.description, h.status, h.device_count, h.del_flag, h.create_by, h.create_time,                h.update_by, h.update_time,                CASE h.status                    WHEN 1 THEN \'监测中\'                    WHEN 2 THEN \'停测中\'                    WHEN 3 THEN \'已完结\'                    ELSE \'\'                END AS status_name         FROM hazard_point h         WHERE h.del_flag = \'0\'               WHERE h.del_flag = 0 AND h.code = ?\r\n### Cause: java.sql.SQLSyntaxErrorException: You have an error in your SQL syntax; check the manual that corresponds to your MySQL server version for the right syntax to use near \'WHERE h.del_flag = 0 AND h.code = \'HP22\'\' at line 13\n; bad SQL grammar []',
        7, '2026-05-21 19:54:35.000', '2026-05-23 18:20:59.686'),
       (34, 900000000000000133, NULL, NULL, NULL, 'admin', '研发部门', '隐患点管理', '1', '/api/v1/hazard-points',
        'POST', 'com.zwei.iot.hazardpoint.controller.HazardPointController.add()', '192.168.51.48', '内网IP', NULL,
        '{\"code\":\"HP222\",\"description\":\"dv\",\"groupId\":6,\"latitude\":30.67,\"longitude\":104.06,\"name\":\"2222\",\"params\":{},\"strike\":0} ',
        NULL, NULL, 'FAIL',
        '\r\n### Error querying database.  Cause: java.sql.SQLSyntaxErrorException: You have an error in your SQL syntax; check the manual that corresponds to your MySQL server version for the right syntax to use near \'WHERE h.del_flag = 0 AND h.code = \'HP222\'\' at line 13\r\n### The error may exist in file [D:\\Code\\Projects\\geo_hazard_monitor\\server\\zwei-iot\\target\\classes\\mapper\\iot\\hazardPoint\\HazardPointMapper.xml]\r\n### The error may involve com.zwei.iot.hazardpoint.mapper.HazardPointMapper.checkHazardPointCodeUnique-Inline\r\n### The error occurred while setting parameters\r\n### SQL: SELECT h.id, h.code, h.name, h.group_id, h.group_name, h.longitude, h.latitude, h.strike,                h.description, h.status, h.device_count, h.del_flag, h.create_by, h.create_time,                h.update_by, h.update_time,                CASE h.status                    WHEN 1 THEN \'监测中\'                    WHEN 2 THEN \'停测中\'                    WHEN 3 THEN \'已完结\'                    ELSE \'\'                END AS status_name         FROM hazard_point h         WHERE h.del_flag = \'0\'               WHERE h.del_flag = 0 AND h.code = ?\r\n### Cause: java.sql.SQLSyntaxErrorException: You have an error in your SQL syntax; check the manual that corresponds to your MySQL server version for the right syntax to use near \'WHERE h.del_flag = 0 AND h.code = \'HP222\'\' at line 13\n; bad SQL grammar []',
        4, '2026-05-21 19:55:34.000', '2026-05-23 18:20:59.686'),
       (35, 900000000000000134, NULL, NULL, NULL, 'admin', '研发部门', '隐患点管理', '1', '/api/v1/hazard-points',
        'POST', 'com.zwei.iot.hazardpoint.controller.HazardPointController.add()', '192.168.51.48', '内网IP', NULL,
        '{\"code\":\"HP222\",\"description\":\"dv\",\"groupId\":6,\"latitude\":30.67,\"longitude\":104.06,\"name\":\"2222\",\"params\":{},\"strike\":2} ',
        NULL, NULL, 'FAIL',
        '\r\n### Error querying database.  Cause: java.sql.SQLSyntaxErrorException: You have an error in your SQL syntax; check the manual that corresponds to your MySQL server version for the right syntax to use near \'WHERE h.del_flag = 0 AND h.code = \'HP222\'\' at line 13\r\n### The error may exist in file [D:\\Code\\Projects\\geo_hazard_monitor\\server\\zwei-iot\\target\\classes\\mapper\\iot\\hazardPoint\\HazardPointMapper.xml]\r\n### The error may involve com.zwei.iot.hazardpoint.mapper.HazardPointMapper.checkHazardPointCodeUnique-Inline\r\n### The error occurred while setting parameters\r\n### SQL: SELECT h.id, h.code, h.name, h.group_id, h.group_name, h.longitude, h.latitude, h.strike,                h.description, h.status, h.device_count, h.del_flag, h.create_by, h.create_time,                h.update_by, h.update_time,                CASE h.status                    WHEN 1 THEN \'监测中\'                    WHEN 2 THEN \'停测中\'                    WHEN 3 THEN \'已完结\'                    ELSE \'\'                END AS status_name         FROM hazard_point h         WHERE h.del_flag = \'0\'               WHERE h.del_flag = 0 AND h.code = ?\r\n### Cause: java.sql.SQLSyntaxErrorException: You have an error in your SQL syntax; check the manual that corresponds to your MySQL server version for the right syntax to use near \'WHERE h.del_flag = 0 AND h.code = \'HP222\'\' at line 13\n; bad SQL grammar []',
        6, '2026-05-21 19:55:40.000', '2026-05-23 18:20:59.686'),
       (36, 900000000000000135, NULL, NULL, NULL, 'admin', '研发部门', '监测类型', '3', '/api/v1/monitor-types/1',
        'DELETE', 'com.zwei.iot.monitor.controller.MonitorTypeController.remove()', '192.168.51.48', '内网IP', NULL,
        '1 ', '{\"msg\":\"操作成功\",\"code\":200,\"timestamp\":1779366075392}', NULL, 'SUCCESS', NULL, 43,
        '2026-05-21 20:21:15.000', '2026-05-23 18:20:59.686'),
       (37, 900000000000000136, NULL, NULL, NULL, 'admin', '研发部门', '监测类型', '1', '/api/v1/monitor-types', 'POST',
        'com.zwei.iot.monitor.controller.MonitorTypeController.add()', '192.168.51.48', '内网IP', NULL,
        '{\"code\":\"JCXL456\",\"createBy\":\"admin\",\"description\":\"\",\"deviceType\":1,\"icon\":\"/jc-icon/green/dw_green.png\",\"id\":9,\"name\":\"测试\",\"params\":{},\"sortOrder\":0,\"status\":1} ',
        '{\"msg\":\"操作成功\",\"code\":200,\"data\":9,\"timestamp\":1779366481905}', NULL, 'SUCCESS', NULL, 94,
        '2026-05-21 20:28:01.000', '2026-05-23 18:20:59.686'),
       (38, 900000000000000137, NULL, NULL, NULL, 'admin', '研发部门', '监测类型', '2', '/api/v1/monitor-types/9',
        'PUT', 'com.zwei.iot.monitor.controller.MonitorTypeController.edit()', '192.168.51.48', '内网IP', NULL,
        '9 {\"description\":\"\",\"deviceType\":1,\"icon\":\"/jc-icon/green/dw_green.png\",\"id\":9,\"name\":\"测试\",\"params\":{},\"sortOrder\":0,\"status\":1,\"updateBy\":\"admin\"} ',
        '{\"msg\":\"操作成功\",\"code\":200,\"timestamp\":1779367159081}', NULL, 'SUCCESS', NULL, 21,
        '2026-05-21 20:39:19.000', '2026-05-23 18:20:59.686'),
       (39, 900000000000000138, NULL, NULL, NULL, 'admin', '研发部门', '监测类型', '2', '/api/v1/monitor-types/7',
        'PUT', 'com.zwei.iot.monitor.controller.MonitorTypeController.edit()', '192.168.51.48', '内网IP', NULL,
        '7 {\"description\":\"\",\"deviceType\":2,\"icon\":\"\",\"id\":7,\"name\":\"地温监测\",\"params\":{},\"sortOrder\":0,\"status\":1,\"updateBy\":\"admin\"} ',
        '{\"msg\":\"操作成功\",\"code\":200,\"timestamp\":1779367944963}', NULL, 'SUCCESS', NULL, 18,
        '2026-05-21 20:52:24.000', '2026-05-23 18:20:59.686'),
       (40, 900000000000000139, NULL, NULL, NULL, 'admin', '研发部门', '监测类型', '2', '/api/v1/monitor-types/7',
        'PUT', 'com.zwei.iot.monitor.controller.MonitorTypeController.edit()', '192.168.51.48', '内网IP', NULL,
        '7 {\"description\":\"\",\"deviceType\":2,\"icon\":\"/green/tl_green.png\",\"id\":7,\"name\":\"地温监测\",\"params\":{},\"sortOrder\":0,\"status\":1,\"updateBy\":\"admin\"} ',
        '{\"msg\":\"操作成功\",\"code\":200,\"timestamp\":1779369354190}', NULL, 'SUCCESS', NULL, 30,
        '2026-05-21 21:15:54.000', '2026-05-23 18:20:59.686'),
       (41, 900000000000000140, NULL, NULL, NULL, 'admin', '研发部门', '监测类型', '2', '/api/v1/monitor-types/5',
        'PUT', 'com.zwei.iot.monitor.controller.MonitorTypeController.edit()', '192.168.51.48', '内网IP', NULL,
        '5 {\"description\":\"\",\"deviceType\":2,\"icon\":\"/green/ky_green.png\",\"id\":5,\"name\":\"裂缝监测\",\"params\":{},\"sortOrder\":0,\"status\":1,\"updateBy\":\"admin\"} ',
        '{\"msg\":\"操作成功\",\"code\":200,\"timestamp\":1779369594325}', NULL, 'SUCCESS', NULL, 11,
        '2026-05-21 21:19:54.000', '2026-05-23 18:20:59.686'),
       (42, 900000000000000141, NULL, NULL, NULL, 'admin', '研发部门', '监测类型', '2', '/api/v1/monitor-types/7',
        'PUT', 'com.zwei.iot.monitor.controller.MonitorTypeController.edit()', '192.168.51.64', '内网IP', NULL,
        '7 {\"description\":\"\",\"deviceType\":2,\"icon\":\"/jc-icon/green/jsd_green.png\",\"id\":7,\"name\":\"地温监测\",\"params\":{},\"sortOrder\":0,\"status\":1,\"updateBy\":\"admin\"} ',
        '{\"msg\":\"操作成功\",\"code\":200,\"timestamp\":1779418219782}', NULL, 'SUCCESS', NULL, 46,
        '2026-05-22 10:50:19.000', '2026-05-23 18:20:59.686'),
       (43, 900000000000000142, NULL, NULL, NULL, 'admin', '研发部门', '监测类型', '2', '/api/v1/monitor-types/5',
        'PUT', 'com.zwei.iot.monitor.controller.MonitorTypeController.edit()', '192.168.51.64', '内网IP', NULL,
        '5 {\"description\":\"\",\"deviceType\":2,\"icon\":\"/jc-icon/green/jsd_green.png\",\"id\":5,\"name\":\"裂缝监测\",\"params\":{},\"sortOrder\":0,\"status\":1,\"updateBy\":\"admin\"} ',
        '{\"msg\":\"操作成功\",\"code\":200,\"timestamp\":1779418224254}', NULL, 'SUCCESS', NULL, 20,
        '2026-05-22 10:50:24.000', '2026-05-23 18:20:59.686'),
       (44, 900000000000000143, NULL, NULL, NULL, 'admin', '研发部门', '监测类型', '2', '/api/v1/monitor-types/3',
        'PUT', 'com.zwei.iot.monitor.controller.MonitorTypeController.edit()', '192.168.51.64', '内网IP', NULL,
        '3 {\"description\":\"\",\"deviceType\":2,\"icon\":\"/jc-iconjc-icon/green/ky_green.png\",\"id\":3,\"name\":\"温湿度监测\",\"params\":{},\"sortOrder\":0,\"status\":1,\"updateBy\":\"admin\"} ',
        '{\"msg\":\"操作成功\",\"code\":200,\"timestamp\":1779418398977}', NULL, 'SUCCESS', NULL, 15,
        '2026-05-22 10:53:18.000', '2026-05-23 18:20:59.686'),
       (45, 900000000000000144, NULL, NULL, NULL, 'admin', '研发部门', '监测类型', '2', '/api/v1/monitor-types/4',
        'PUT', 'com.zwei.iot.monitor.controller.MonitorTypeController.edit()', '192.168.51.64', '内网IP', NULL,
        '4 {\"description\":\"\",\"deviceType\":2,\"icon\":\"/jc-iconjc-icon/green/sc_green.png\",\"id\":4,\"name\":\"水位监测\",\"params\":{},\"sortOrder\":0,\"status\":1,\"updateBy\":\"admin\"} ',
        '{\"msg\":\"操作成功\",\"code\":200,\"timestamp\":1779418408839}', NULL, 'SUCCESS', NULL, 12,
        '2026-05-22 10:53:28.000', '2026-05-23 18:20:59.686'),
       (46, 900000000000000145, NULL, NULL, NULL, 'admin', '研发部门', '监测类型', '2', '/api/v1/monitor-types/2',
        'PUT', 'com.zwei.iot.monitor.controller.MonitorTypeController.edit()', '192.168.51.64', '内网IP', NULL,
        '2 {\"description\":\"\",\"deviceType\":2,\"icon\":\"/jc-icongreen/sc_green.png\",\"id\":2,\"name\":\"位移监测\",\"params\":{},\"sortOrder\":0,\"status\":1,\"updateBy\":\"admin\"} ',
        '{\"msg\":\"操作成功\",\"code\":200,\"timestamp\":1779418421471}', NULL, 'SUCCESS', NULL, 19,
        '2026-05-22 10:53:41.000', '2026-05-23 18:20:59.686'),
       (47, 900000000000000146, NULL, NULL, NULL, 'admin', '研发部门', '监测类型', '2', '/api/v1/monitor-types/7',
        'PUT', 'com.zwei.iot.monitor.controller.MonitorTypeController.edit()', '192.168.51.64', '内网IP', NULL,
        '7 {\"description\":\"\",\"deviceType\":2,\"icon\":\"/jc-icongreen/sc_green.png\",\"id\":7,\"name\":\"地温监测\",\"params\":{},\"sortOrder\":0,\"status\":1,\"updateBy\":\"admin\"} ',
        '{\"msg\":\"操作成功\",\"code\":200,\"timestamp\":1779418427566}', NULL, 'SUCCESS', NULL, 21,
        '2026-05-22 10:53:47.000', '2026-05-23 18:20:59.686'),
       (48, 900000000000000147, NULL, NULL, NULL, 'admin', '研发部门', '监测类型', '2', '/api/v1/monitor-types/7',
        'PUT', 'com.zwei.iot.monitor.controller.MonitorTypeController.edit()', '192.168.51.64', '内网IP', NULL,
        '7 {\"description\":\"\",\"deviceType\":2,\"icon\":\"/jc-icon/jc-icon/green/ssw_green.png\",\"id\":7,\"name\":\"地温监测\",\"params\":{},\"sortOrder\":0,\"status\":1,\"updateBy\":\"admin\"} ',
        '{\"msg\":\"操作成功\",\"code\":200,\"timestamp\":1779418566506}', NULL, 'SUCCESS', NULL, 13,
        '2026-05-22 10:56:06.000', '2026-05-23 18:20:59.686'),
       (49, 900000000000000148, NULL, NULL, NULL, 'admin', '研发部门', '监测类型', '2', '/api/v1/monitor-types/7',
        'PUT', 'com.zwei.iot.monitor.controller.MonitorTypeController.edit()', '192.168.51.64', '内网IP', NULL,
        '7 {\"description\":\"\",\"deviceType\":2,\"icon\":\"/jc-icon/jc-icon/green/jsd_green.png\",\"id\":7,\"name\":\"地温监测\",\"params\":{},\"sortOrder\":0,\"status\":1,\"updateBy\":\"admin\"} ',
        '{\"msg\":\"操作成功\",\"code\":200,\"timestamp\":1779418576541}', NULL, 'SUCCESS', NULL, 13,
        '2026-05-22 10:56:16.000', '2026-05-23 18:20:59.686'),
       (50, 900000000000000149, NULL, NULL, NULL, 'admin', '研发部门', '监测类型', '2', '/api/v1/monitor-types/3',
        'PUT', 'com.zwei.iot.monitor.controller.MonitorTypeController.edit()', '192.168.51.64', '内网IP', NULL,
        '3 {\"description\":\"\",\"deviceType\":2,\"icon\":\"/jc-icon/jc-icon/green/sg_green.png\",\"id\":3,\"name\":\"温湿度监测\",\"params\":{},\"sortOrder\":0,\"status\":1,\"updateBy\":\"admin\"} ',
        '{\"msg\":\"操作成功\",\"code\":200,\"timestamp\":1779418582807}', NULL, 'SUCCESS', NULL, 12,
        '2026-05-22 10:56:22.000', '2026-05-23 18:20:59.686'),
       (51, 900000000000000150, NULL, NULL, NULL, 'admin', '研发部门', '监测类型', '2', '/api/v1/monitor-types/4',
        'PUT', 'com.zwei.iot.monitor.controller.MonitorTypeController.edit()', '192.168.51.64', '内网IP', NULL,
        '4 {\"description\":\"\",\"deviceType\":2,\"icon\":\"/jc-icon/jc-icon/green/lf_green.png\",\"id\":4,\"name\":\"水位监测\",\"params\":{},\"sortOrder\":0,\"status\":1,\"updateBy\":\"admin\"} ',
        '{\"msg\":\"操作成功\",\"code\":200,\"timestamp\":1779418589145}', NULL, 'SUCCESS', NULL, 16,
        '2026-05-22 10:56:29.000', '2026-05-23 18:20:59.686'),
       (52, 900000000000000151, NULL, NULL, NULL, 'admin', '研发部门', '监测类型', '2', '/api/v1/monitor-types/2',
        'PUT', 'com.zwei.iot.monitor.controller.MonitorTypeController.edit()', '192.168.51.64', '内网IP', NULL,
        '2 {\"description\":\"\",\"deviceType\":2,\"icon\":\"/jc-icon/green/jsd_green.png\",\"id\":2,\"name\":\"位移监测\",\"params\":{},\"sortOrder\":0,\"status\":1,\"updateBy\":\"admin\"} ',
        '{\"msg\":\"操作成功\",\"code\":200,\"timestamp\":1779418649432}', NULL, 'SUCCESS', NULL, 13,
        '2026-05-22 10:57:29.000', '2026-05-23 18:20:59.686'),
       (53, 900000000000000152, NULL, NULL, NULL, 'admin', '研发部门', '监测类型', '2', '/api/v1/monitor-types/1',
        'PUT', 'com.zwei.iot.monitor.controller.MonitorTypeController.edit()', '192.168.51.64', '内网IP', NULL,
        '1 {\"description\":\"\",\"deviceType\":2,\"icon\":\"/jc-icon/green/wj_green.png\",\"id\":1,\"name\":\"雨量监测\",\"params\":{},\"sortOrder\":0,\"status\":1,\"updateBy\":\"admin\"} ',
        '{\"msg\":\"操作成功\",\"code\":200,\"timestamp\":1779418656559}', NULL, 'SUCCESS', NULL, 10,
        '2026-05-22 10:57:36.000', '2026-05-23 18:20:59.686'),
       (54, 900000000000000153, NULL, NULL, NULL, 'admin', '研发部门', '监测类型', '2', '/api/v1/monitor-types/9',
        'PUT', 'com.zwei.iot.monitor.controller.MonitorTypeController.edit()', '192.168.51.64', '内网IP', NULL,
        '9 {\"description\":\"\",\"deviceType\":1,\"icon\":\"/jc-icon/green/wj_green.png\",\"id\":9,\"name\":\"测试\",\"params\":{},\"sortOrder\":0,\"status\":1,\"updateBy\":\"admin\"} ',
        '{\"msg\":\"操作成功\",\"code\":200,\"timestamp\":1779418662376}', NULL, 'SUCCESS', NULL, 9,
        '2026-05-22 10:57:42.000', '2026-05-23 18:20:59.686'),
       (55, 900000000000000154, NULL, NULL, NULL, 'admin', '研发部门', '监测类型', '2', '/api/v1/monitor-types/3',
        'PUT', 'com.zwei.iot.monitor.controller.MonitorTypeController.edit()', '192.168.51.64', '内网IP', NULL,
        '3 {\"description\":\"\",\"deviceType\":2,\"icon\":\"/jc-icon/green/ky_green.png\",\"id\":3,\"name\":\"温湿度监测\",\"params\":{},\"sortOrder\":0,\"status\":1,\"updateBy\":\"admin\"} ',
        '{\"msg\":\"操作成功\",\"code\":200,\"timestamp\":1779421325313}', NULL, 'SUCCESS', NULL, 52,
        '2026-05-22 11:42:05.000', '2026-05-23 18:20:59.686'),
       (56, 900000000000000155, NULL, NULL, NULL, 'admin', '研发部门', '监测类型', '2', '/api/v1/monitor-types/4',
        'PUT', 'com.zwei.iot.monitor.controller.MonitorTypeController.edit()', '192.168.51.64', '内网IP', NULL,
        '4 {\"description\":\"\",\"deviceType\":2,\"icon\":\"/jc-icon/green/sg_green.png\",\"id\":4,\"name\":\"水位监测\",\"params\":{},\"sortOrder\":0,\"status\":1,\"updateBy\":\"admin\"} ',
        '{\"msg\":\"操作成功\",\"code\":200,\"timestamp\":1779421332515}', NULL, 'SUCCESS', NULL, 20,
        '2026-05-22 11:42:12.000', '2026-05-23 18:20:59.686'),
       (57, 900000000000000156, NULL, NULL, NULL, 'admin', '研发部门', '监测类型', '2', '/api/v1/monitor-types/6',
        'PUT', 'com.zwei.iot.monitor.controller.MonitorTypeController.edit()', '192.168.51.64', '内网IP', NULL,
        '6 {\"description\":\"\",\"deviceType\":2,\"icon\":\"/jc-icon/green/nw_green.png\",\"id\":6,\"name\":\"倾斜监测\",\"params\":{},\"sortOrder\":0,\"status\":1,\"updateBy\":\"admin\"} ',
        '{\"msg\":\"操作成功\",\"code\":200,\"timestamp\":1779421337410}', NULL, 'SUCCESS', NULL, 21,
        '2026-05-22 11:42:17.000', '2026-05-23 18:20:59.686'),
       (58, 900000000000000157, NULL, NULL, NULL, 'admin', '研发部门', '监测类型', '2', '/api/v1/monitor-types/7',
        'PUT', 'com.zwei.iot.monitor.controller.MonitorTypeController.edit()', '192.168.51.64', '内网IP', NULL,
        '7 {\"description\":\"\",\"deviceType\":2,\"icon\":\"/jc-icon/green/gnss_green.png\",\"id\":7,\"name\":\"地温监测\",\"params\":{},\"sortOrder\":0,\"status\":1,\"updateBy\":\"admin\"} ',
        '{\"msg\":\"操作成功\",\"code\":200,\"timestamp\":1779421344217}', NULL, 'SUCCESS', NULL, 15,
        '2026-05-22 11:42:24.000', '2026-05-23 18:20:59.686'),
       (59, 900000000000000158, NULL, NULL, NULL, 'admin', '研发部门', '监测类型', '2', '/api/v1/monitor-types/8',
        'PUT', 'com.zwei.iot.monitor.controller.MonitorTypeController.edit()', '192.168.51.64', '内网IP', NULL,
        '8 {\"description\":\"\",\"deviceType\":2,\"icon\":\"/jc-icon/green/lf_green.png\",\"id\":8,\"name\":\"含水率监测\",\"params\":{},\"sortOrder\":0,\"status\":1,\"updateBy\":\"admin\"} ',
        '{\"msg\":\"操作成功\",\"code\":200,\"timestamp\":1779421348853}', NULL, 'SUCCESS', NULL, 21,
        '2026-05-22 11:42:28.000', '2026-05-23 18:20:59.686'),
       (60, 900000000000000159, NULL, NULL, NULL, 'admin', '研发部门', '监测类型', '2', '/api/v1/monitor-types/1',
        'PUT', 'com.zwei.iot.monitor.controller.MonitorTypeController.edit()', '192.168.51.64', '内网IP', NULL,
        '1 {\"description\":\"\",\"deviceType\":2,\"icon\":\"/jc-icon/green/wj_green.png\",\"id\":1,\"name\":\"雨量监测\",\"params\":{},\"sortOrder\":0,\"status\":1,\"updateBy\":\"admin\"} ',
        '{\"msg\":\"操作成功\",\"code\":200,\"timestamp\":1779421681128}', NULL, 'SUCCESS', NULL, 17,
        '2026-05-22 11:48:01.000', '2026-05-23 18:20:59.686'),
       (61, 900000000000000160, NULL, NULL, NULL, 'admin', '研发部门', '隐患点管理', '1', '/api/v1/hazard-points',
        'POST', 'com.zwei.iot.hazardpoint.controller.HazardPointController.add()', '127.0.0.1', '内网IP', NULL,
        '{\"code\":\"HP07\",\"description\":\"\",\"groupId\":2,\"latitude\":30.67,\"longitude\":104.06,\"name\":\"test\",\"params\":{},\"strike\":0} ',
        NULL, NULL, 'FAIL',
        '\r\n### Error querying database.  Cause: java.sql.SQLSyntaxErrorException: You have an error in your SQL syntax; check the manual that corresponds to your MySQL server version for the right syntax to use near \'WHERE h.del_flag = 0\n            AND h.code = \'HP07\'\' at line 13\r\n### The error may exist in file [D:\\Code\\Projects\\geo_hazard_monitor\\server\\zwei-iot\\target\\classes\\mapper\\iot\\hazardPoint\\HazardPointMapper.xml]\r\n### The error may involve com.zwei.iot.hazardpoint.mapper.HazardPointMapper.checkHazardPointCodeUnique-Inline\r\n### The error occurred while setting parameters\r\n### SQL: SELECT h.id, h.code, h.name, h.group_id, h.group_name, h.longitude, h.latitude, h.strike,                h.description, h.status, h.device_count, h.del_flag, h.create_by, h.create_time,                h.update_by, h.update_time,                CASE h.status                    WHEN 1 THEN \'监测中\'                    WHEN 2 THEN \'停测中\'                    WHEN 3 THEN \'已完结\'                    ELSE \'\'                END AS status_name         FROM hazard_point h         WHERE h.del_flag = \'0\'                WHERE h.del_flag = 0             AND h.code = ?\r\n### Cause: java.sql.SQLSyntaxErrorException: You have an error in your SQL syntax; check the manual that corresponds to your MySQL server version for the right syntax to use near \'WHERE h.del_flag = 0\n            AND h.code = \'HP07\'\' at line 13\n; bad SQL grammar []',
        242, '2026-05-23 14:26:59.000', '2026-05-23 18:20:59.686'),
       (62, 900000000000000161, NULL, NULL, NULL, 'admin', '研发部门', '隐患点管理', '1', '/api/v1/hazard-points',
        'POST', 'com.zwei.iot.hazardpoint.controller.HazardPointController.add()', '127.0.0.1', '内网IP', NULL,
        '{\"code\":\"HP007\",\"description\":\"\",\"groupId\":2,\"latitude\":30.67,\"longitude\":104.06,\"name\":\"test\",\"params\":{},\"strike\":0} ',
        '{\"msg\":\"新增隐患点失败，编号已存在\",\"code\":500,\"timestamp\":1779517852878}', NULL, 'SUCCESS', NULL, 54,
        '2026-05-23 14:30:53.000', '2026-05-23 18:20:59.686'),
       (63, 900000000000000162, NULL, NULL, NULL, 'admin', '研发部门', '隐患点管理', '1', '/api/v1/hazard-points',
        'POST', 'com.zwei.iot.hazardpoint.controller.HazardPointController.add()', '127.0.0.1', '内网IP', NULL,
        '{\"code\":\"HP007\",\"description\":\"\",\"groupId\":2,\"latitude\":30.67,\"longitude\":104.06,\"name\":\"test\",\"params\":{},\"strike\":0} ',
        '{\"msg\":\"新增隐患点失败，编号已存在\",\"code\":500,\"timestamp\":1779517860060}', NULL, 'SUCCESS', NULL, 9,
        '2026-05-23 14:31:00.000', '2026-05-23 18:20:59.686'),
       (64, 900000000000000163, NULL, NULL, NULL, 'admin', '研发部门', '隐患点管理', '1', '/api/v1/hazard-points',
        'POST', 'com.zwei.iot.hazardpoint.controller.HazardPointController.add()', '127.0.0.1', '内网IP', NULL,
        '{\"code\":\"HP008\",\"createBy\":\"admin\",\"description\":\"\",\"groupId\":2,\"id\":13,\"latitude\":30.67,\"longitude\":104.06,\"name\":\"test\",\"params\":{},\"status\":1,\"strike\":0} ',
        '{\"msg\":\"操作成功\",\"code\":200,\"timestamp\":1779523572795}', NULL, 'SUCCESS', NULL, 394,
        '2026-05-23 16:06:12.000', '2026-05-23 18:20:59.686'),
       (65, 900000000000000164, NULL, NULL, NULL, 'admin', '研发部门', '隐患点管理', '3', '/api/v1/hazard-points/13',
        'DELETE', 'com.zwei.iot.hazardpoint.controller.HazardPointController.remove()', '127.0.0.1', '内网IP', NULL,
        '13 ', '{\"msg\":\"操作成功\",\"code\":200,\"timestamp\":1779523606608}', NULL, 'SUCCESS', NULL, 18,
        '2026-05-23 16:06:46.000', '2026-05-23 18:20:59.686'),
       (66, 900000000000000165, NULL, NULL, NULL, 'admin', '研发部门', '隐患点管理', '1', '/api/v1/hazard-points',
        'POST', 'com.zwei.iot.hazardpoint.controller.HazardPointController.add()', '127.0.0.1', '内网IP', NULL,
        '{\"code\":\"HP008\",\"createBy\":\"admin\",\"description\":\"\",\"latitude\":30.67,\"longitude\":104.06,\"name\":\"test\",\"params\":{},\"status\":1,\"strike\":0} ',
        NULL, NULL, 'FAIL',
        '\r\n### Error updating database.  Cause: java.sql.SQLIntegrityConstraintViolationException: Duplicate entry \'HP008\' for key \'hazard_point.uk_hazard_point_code\'\r\n### The error may exist in file [D:\\Code\\Projects\\geo_hazard_monitor\\server\\zwei-iot\\target\\classes\\mapper\\iot\\hazardPoint\\HazardPointMapper.xml]\r\n### The error may involve com.zwei.iot.hazardpoint.mapper.HazardPointMapper.insertHazardPoint-Inline\r\n### The error occurred while setting parameters\r\n### SQL: INSERT INTO hazard_point (              code,               name,                                           longitude,               latitude,               strike,               description,               status,               create_by,              create_time         ) VALUES (              ?,               ?,                                           ?,               ?,               ?,               ?,               ?,               ?,              NOW()         )\r\n### Cause: java.sql.SQLIntegrityConstraintViolationException: Duplicate entry \'HP008\' for key \'hazard_point.uk_hazard_point_code\'\n; Duplicate entry \'HP008\' for key \'hazard_point.uk_hazard_point_code\'',
        286, '2026-05-23 16:07:05.000', '2026-05-23 18:20:59.686'),
       (67, 900000000000000166, NULL, NULL, NULL, 'admin', '研发部门', '隐患点管理', '1', '/api/v1/hazard-points',
        'POST', 'com.zwei.iot.hazardpoint.controller.HazardPointController.add()', '127.0.0.1', '内网IP', NULL,
        '{\"code\":\"HP009\",\"createBy\":\"admin\",\"description\":\"\",\"id\":15,\"latitude\":30.67,\"longitude\":104.06,\"name\":\"test\",\"params\":{},\"status\":1,\"strike\":0} ',
        '{\"msg\":\"操作成功\",\"code\":200,\"timestamp\":1779523643138}', NULL, 'SUCCESS', NULL, 21,
        '2026-05-23 16:07:23.000', '2026-05-23 18:20:59.686'),
       (68, 921779589853561002, '9e2659e36fef4bf4b0647ded322dc6a7', '78628ec6f1c44b97b76b9f9a822f9449', 1, 'admin',
        '研发部门', '接口访问', 'REQUEST', '/api/v1/hazard-point-groups', 'GET', 'FILTER', '127.0.0.1', '内网IP',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36 Edg/148.0.0.0',
        NULL, NULL, 200, 'SUCCESS', NULL, 98, '2026-05-24 10:34:39.713', '2026-05-24 10:34:39.721'),
       (69, 921779589853561003, 'a3c68d2a5cc44039af6a0cdd9fbc9fdd', 'b35ca3615b754b5ca0bbd039460c8c41', 1, 'admin',
        '研发部门', '接口访问', 'REQUEST', '/api/v1/hazard-points/page', 'GET', 'FILTER', '127.0.0.1', '内网IP',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36 Edg/148.0.0.0',
        NULL, NULL, 200, 'SUCCESS', NULL, 109, '2026-05-24 10:34:39.724', '2026-05-24 10:34:39.730'),
       (70, 921779589853561004, '7ce2d70fcc8348f39eb7b477d7db99b0', '167a262b49344b86b253b8d0ff19d5a7', 1, 'admin',
        '研发部门', '接口访问', 'REQUEST', '/api/v1/hazard-point-groups', 'GET', 'FILTER', '127.0.0.1', '内网IP',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36 Edg/148.0.0.0',
        NULL, NULL, 200, 'SUCCESS', NULL, 24, '2026-05-24 10:35:25.657', '2026-05-24 10:35:25.663'),
       (71, 921779589853561005, 'e20ae3b1a4c24ed4aab30873f79fc507', '32f76d75a42b4c9cb12bc3ac465f37ad', 1, 'admin',
        '研发部门', '接口访问', 'REQUEST', '/api/v1/hazard-points/page', 'GET', 'FILTER', '127.0.0.1', '内网IP',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36 Edg/148.0.0.0',
        NULL, NULL, 200, 'SUCCESS', NULL, 26, '2026-05-24 10:35:25.660', '2026-05-24 10:35:25.666'),
       (72, 921779591049666001, '635c5ec77b7940769f1a342b8ef29b45', '21d84f1055414988850bcac14bc2fe16', 1, 'admin',
        '研发部门', '接口访问', 'REQUEST', '/api/v1/hazard-point-groups', 'GET', 'FILTER', '127.0.0.1', '内网IP',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36 Edg/148.0.0.0',
        NULL, NULL, 200, 'SUCCESS', NULL, 141, '2026-05-24 10:52:02.461', '2026-05-24 10:52:02.759'),
       (73, 921779591049666002, 'a7a240d0aefc46a6a4700fcb78394668', '63d8fcefbac64272aa9de20548088075', 1, 'admin',
        '研发部门', '接口访问', 'REQUEST', '/api/v1/hazard-points/page', 'GET', 'FILTER', '127.0.0.1', '内网IP',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36 Edg/148.0.0.0',
        NULL, NULL, 200, 'SUCCESS', NULL, 204, '2026-05-24 10:52:02.534', '2026-05-24 10:52:02.759'),
       (74, 921779591049666003, '87daaad47fad460caa7b1abbcdd58e03', '6ece17021ab14cc6b87c314886d45ebc', 1, 'admin',
        '研发部门', '隐患点管理', 'UPDATE', '/api/v1/hazard-points/15', 'PUT',
        'com.zwei.iot.hazardpoint.controller.HazardPointController.edit()', '127.0.0.1', NULL, NULL,
        '15 {\"description\":\"\",\"id\":15,\"latitude\":30.67,\"longitude\":104.06,\"name\":\"test123\",\"params\":{},\"strike\":0,\"updateBy\":\"admin\"} ',
        '{\"msg\":\"操作成功\",\"code\":200,\"timestamp\":1779591168572}', 200, 'SUCCESS', NULL, 328,
        '2026-05-24 10:52:48.631', '2026-05-24 10:52:48.644'),
       (75, 921779591049666004, '3f9cd73a955c44e4b406f0592033a98f', '1dace41443a948eebac061ff84e431a5', 1, 'admin',
        '研发部门', '接口访问', 'REQUEST', '/api/v1/hazard-point-groups', 'GET', 'FILTER', '127.0.0.1', '内网IP',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36 Edg/148.0.0.0',
        NULL, NULL, 200, 'SUCCESS', NULL, 25, '2026-05-24 10:52:48.707', '2026-05-24 10:52:48.715'),
       (76, 921779591049666005, '09541e57ff4c4f6a9ee6e0f112c9d307', '7b37d64b253b42179b8e82012179e3c5', 1, 'admin',
        '研发部门', '接口访问', 'REQUEST', '/api/v1/hazard-points/page', 'GET', 'FILTER', '127.0.0.1', '内网IP',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36 Edg/148.0.0.0',
        NULL, NULL, 200, 'SUCCESS', NULL, 33, '2026-05-24 10:52:48.716', '2026-05-24 10:52:48.729'),
       (77, 921779591049666006, 'a30c143980cf4c68b55390a9640900e1', 'e90e3d3e7485457192f018b44fa91ec1', 1, 'admin',
        '研发部门', '隐患点管理', 'UPDATE', '/api/v1/hazard-points/15', 'PUT',
        'com.zwei.iot.hazardpoint.controller.HazardPointController.edit()', '127.0.0.1', NULL, NULL,
        '15 {\"description\":\"\",\"id\":15,\"latitude\":30.67,\"longitude\":104.06,\"name\":\"test111\",\"params\":{},\"strike\":0,\"updateBy\":\"admin\"} ',
        '{\"msg\":\"操作成功\",\"code\":200,\"timestamp\":1779591187485}', 200, 'SUCCESS', NULL, 10,
        '2026-05-24 10:53:07.485', '2026-05-24 10:53:07.494'),
       (78, 921779591049666007, '9e594cb72f1c4e76b622b214670ee8e6', '8b9703f9d90441c59ee3c720a823ea91', 1, 'admin',
        '研发部门', '接口访问', 'REQUEST', '/api/v1/hazard-point-groups', 'GET', 'FILTER', '127.0.0.1', '内网IP',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36 Edg/148.0.0.0',
        NULL, NULL, 200, 'SUCCESS', NULL, 27, '2026-05-24 10:53:07.551', '2026-05-24 10:53:07.556'),
       (79, 921779591049666008, '442f0aa740a1432685f3b5349de6eae7', '48d912929eed467ebcdaca35e58aaf45', 1, 'admin',
        '研发部门', '接口访问', 'REQUEST', '/api/v1/hazard-points/page', 'GET', 'FILTER', '127.0.0.1', '内网IP',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36 Edg/148.0.0.0',
        NULL, NULL, 200, 'SUCCESS', NULL, 41, '2026-05-24 10:53:07.565', '2026-05-24 10:53:07.572'),
       (80, 921779591049666009, 'd858cd688ba04f81a511e13b43b094b0', '929ca27fc17b40ab877545c63e1803bb', 1, 'admin',
        '研发部门', '隐患点管理', 'UPDATE', '/api/v1/hazard-points/15', 'PUT',
        'com.zwei.iot.hazardpoint.controller.HazardPointController.edit()', '127.0.0.1', NULL, NULL,
        '15 {\"description\":\"\",\"id\":15,\"latitude\":30.67,\"longitude\":104.06,\"name\":\"test123\",\"params\":{},\"strike\":0,\"updateBy\":\"admin\"} ',
        '{\"msg\":\"操作成功\",\"code\":200,\"timestamp\":1779591271583}', 200, 'SUCCESS', NULL, 16,
        '2026-05-24 10:54:31.584', '2026-05-24 10:54:31.590'),
       (81, 921779591049666010, '416cd2502a154481b1babe7c9dd4e290', '7b24bc73fc924bd5a716a1900f545d6d', 1, 'admin',
        '研发部门', '接口访问', 'REQUEST', '/api/v1/hazard-point-groups', 'GET', 'FILTER', '127.0.0.1', '内网IP',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36 Edg/148.0.0.0',
        NULL, NULL, 200, 'SUCCESS', NULL, 17, '2026-05-24 10:54:31.660', '2026-05-24 10:54:31.666'),
       (82, 921779591049666011, '7b0705b91d48424caf6684e4fb8d0a01', 'a08ef2fe29024508929e64f4d97eacd1', 1, 'admin',
        '研发部门', '接口访问', 'REQUEST', '/api/v1/hazard-points/page', 'GET', 'FILTER', '127.0.0.1', '内网IP',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36 Edg/148.0.0.0',
        NULL, NULL, 200, 'SUCCESS', NULL, 23, '2026-05-24 10:54:31.662', '2026-05-24 10:54:31.668'),
       (83, 921779591049666012, 'c058fe1f7c9d4b3d8bf82ab4c2a8b614', '5b10fea1f82e40a9b537fc22834aef4c', 1, 'admin',
        '研发部门', '隐患点管理', 'UPDATE', '/api/v1/hazard-points/15', 'PUT',
        'com.zwei.iot.hazardpoint.controller.HazardPointController.edit()', '127.0.0.1', NULL, NULL,
        '15 {\"description\":\"\",\"id\":15,\"latitude\":30.67,\"longitude\":104.06,\"name\":\"test111\",\"params\":{},\"strike\":0,\"updateBy\":\"admin\"} ',
        '{\"msg\":\"操作成功\",\"code\":200,\"timestamp\":1779591534375}', 200, 'SUCCESS', NULL, 15,
        '2026-05-24 10:58:54.376', '2026-05-24 10:58:54.382'),
       (84, 921779591049666013, 'e71847c184ab4ef3b73f2cf2a4c1caa6', '06bc12a81b0d4a6fb14742fa5eee255b', 1, 'admin',
        '研发部门', '接口访问', 'REQUEST', '/api/v1/hazard-point-groups', 'GET', 'FILTER', '127.0.0.1', '内网IP',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36 Edg/148.0.0.0',
        NULL, NULL, 200, 'SUCCESS', NULL, 18, '2026-05-24 10:58:54.440', '2026-05-24 10:58:54.446'),
       (85, 921779591049666014, '4bfb8d1f3efa4e7e8bb0fe90c6729b76', '4fe59745496c480584ca6e10d5371a99', 1, 'admin',
        '研发部门', '接口访问', 'REQUEST', '/api/v1/hazard-points/page', 'GET', 'FILTER', '127.0.0.1', '内网IP',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36 Edg/148.0.0.0',
        NULL, NULL, 200, 'SUCCESS', NULL, 29, '2026-05-24 10:58:54.453', '2026-05-24 10:58:54.461'),
       (86, 921779591049666015, '8cc14d46e0fb44ba9685eee34fc0538e', '74868ca5d761432caa8374e9b6165e88', 1, 'admin',
        '研发部门', '隐患点管理', 'UPDATE', '/api/v1/hazard-points/15', 'PUT',
        'com.zwei.iot.hazardpoint.controller.HazardPointController.edit()', '127.0.0.1', NULL, NULL,
        '15 {\"description\":\"\",\"id\":15,\"latitude\":30.67,\"longitude\":104.06,\"name\":\"test123\",\"params\":{},\"strike\":0,\"updateBy\":\"admin\"} ',
        '{\"msg\":\"操作成功\",\"code\":200,\"timestamp\":1779591585723}', 200, 'SUCCESS', NULL, 9,
        '2026-05-24 10:59:45.723', '2026-05-24 10:59:45.729'),
       (87, 921779591049666017, '8d22c71331f441d0855639c4a7ec1c82', '65d397905515412bb893c2d5fda5b34b', 1, 'admin',
        '研发部门', '接口访问', 'REQUEST', '/api/v1/hazard-points/page', 'GET', 'FILTER', '127.0.0.1', '内网IP',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36 Edg/148.0.0.0',
        NULL, NULL, 200, 'SUCCESS', NULL, 28, '2026-05-24 10:59:45.788', '2026-05-24 10:59:45.800'),
       (88, 921779591049666016, 'b602faca73ed4fe1bbc77a4072171328', '479379bf4922496fa0efd298acb684e6', 1, 'admin',
        '研发部门', '接口访问', 'REQUEST', '/api/v1/hazard-point-groups', 'GET', 'FILTER', '127.0.0.1', '内网IP',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36 Edg/148.0.0.0',
        NULL, NULL, 200, 'SUCCESS', NULL, 26, '2026-05-24 10:59:45.788', '2026-05-24 10:59:45.801'),
       (89, 921779622864396002, 'c14da60e8e4b4f4c917b70f9abd27a2c', '6d6e5beb557d4811a55724b6a437d108', 1, 'admin',
        '研发部门', '接口访问', 'REQUEST', '/api/v1/hazard-point-groups', 'GET', 'FILTER', '127.0.0.1', '内网IP',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36 Edg/148.0.0.0',
        NULL, NULL, 200, 'SUCCESS', NULL, 72, '2026-05-24 19:41:47.152', '2026-05-24 19:41:47.161'),
       (90, 921779622864396003, '05a912e1e5e04355bf387311ad3b68d4', '9e343f2e9e024c0dab1fa4c4fab85168', 1, 'admin',
        '研发部门', '接口访问', 'REQUEST', '/api/v1/hazard-points/page', 'GET', 'FILTER', '127.0.0.1', '内网IP',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36 Edg/148.0.0.0',
        NULL, NULL, 200, 'SUCCESS', NULL, 242, '2026-05-24 19:41:47.321', '2026-05-24 19:41:47.327'),
       (91, 921779622864396004, '073e5e8909084b01a41f40e431e81189', '1e4ec59298014f038481afc6368cac9d', 1, 'admin',
        '研发部门', '接口访问', 'REQUEST', '/api/v1/hazard-point-groups', 'GET', 'FILTER', '127.0.0.1', '内网IP',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36 Edg/148.0.0.0',
        NULL, NULL, 200, 'SUCCESS', NULL, 23, '2026-05-24 19:42:01.985', '2026-05-24 19:42:01.991'),
       (92, 921779622864396005, 'bd32b3f037234216a2ce854f7d8bb0cc', '542f4b070e224f24b6041063d5f899ff', 1, 'admin',
        '研发部门', '接口访问', 'REQUEST', '/api/v1/hazard-points/page', 'GET', 'FILTER', '127.0.0.1', '内网IP',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36 Edg/148.0.0.0',
        NULL, NULL, 200, 'SUCCESS', NULL, 31, '2026-05-24 19:42:01.993', '2026-05-24 19:42:01.998'),
       (93, 921779622864396006, 'b5f2e2c0913449e0b01569f4aa30682b', 'a60340d389bc46d2bbf6bf0d01ea4c9b', 1, 'admin',
        '研发部门', '接口访问', 'REQUEST', '/api/v1/hazard-point-groups', 'GET', 'FILTER', '127.0.0.1', '内网IP',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36 Edg/148.0.0.0',
        NULL, NULL, 200, 'SUCCESS', NULL, 33, '2026-05-24 19:42:23.833', '2026-05-24 19:42:23.839'),
       (94, 921779622864396007, '6b2269f9203f42818f0ebe447f71ba5b', '34e893d10dc94383ad948e8fcb6970e0', 1, 'admin',
        '研发部门', '接口访问', 'REQUEST', '/api/v1/hazard-points/page', 'GET', 'FILTER', '127.0.0.1', '内网IP',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36 Edg/148.0.0.0',
        NULL, NULL, 200, 'SUCCESS', NULL, 37, '2026-05-24 19:42:23.837', '2026-05-24 19:42:23.843'),
       (95, 921779622864396008, '09a2bba47fb04f8dbd7941ef6d2f9e49', '27b1c50ee8294f73b40bb8c9fc53b748', 1, 'admin',
        '研发部门', '接口访问', 'REQUEST', '/api/v1/hazard-points/page', 'GET', 'FILTER', '127.0.0.1', '内网IP',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36 Edg/148.0.0.0',
        NULL, NULL, 200, 'SUCCESS', NULL, 25, '2026-05-24 19:42:34.347', '2026-05-24 19:42:34.351'),
       (96, 921779622864396009, 'b85e470edad2495f833d2a78d51468af', 'f75d6dd06e4742509e21bfea4a623be1', 1, 'admin',
        '研发部门', '接口访问', 'REQUEST', '/api/v1/hazard-points/page', 'GET', 'FILTER', '127.0.0.1', '内网IP',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36 Edg/148.0.0.0',
        NULL, NULL, 200, 'SUCCESS', NULL, 20, '2026-05-24 19:42:35.559', '2026-05-24 19:42:35.564'),
       (97, 921779622864396010, '2232d5126643468bbb7a71b47b927f40', '04d7034df5d24eb7a9b904bbde8199dc', 1, 'admin',
        '研发部门', '接口访问', 'REQUEST', '/api/v1/hazard-points/page', 'GET', 'FILTER', '127.0.0.1', '内网IP',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36 Edg/148.0.0.0',
        NULL, NULL, 200, 'SUCCESS', NULL, 16, '2026-05-24 19:42:36.427', '2026-05-24 19:42:36.431'),
       (98, 921779622864396011, '3715860ec1104e9b95428709ad850275', '554bf05c255b45f7bd173ee9114cecdb', 1, 'admin',
        '研发部门', '接口访问', 'REQUEST', '/api/v1/hazard-points/page', 'GET', 'FILTER', '127.0.0.1', '内网IP',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36 Edg/148.0.0.0',
        NULL, NULL, 200, 'SUCCESS', NULL, 21, '2026-05-24 19:42:36.900', '2026-05-24 19:42:36.906'),
       (99, 921779622864396012, '93b99a4b4e144825a116cb65d06bdf4f', '40200f151d41408a99dbe237ad7acff9', 1, 'admin',
        '研发部门', '接口访问', 'REQUEST', '/api/v1/hazard-point-groups', 'GET', 'FILTER', '127.0.0.1', '内网IP',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36 Edg/148.0.0.0',
        NULL, NULL, 200, 'SUCCESS', NULL, 18, '2026-05-24 19:42:43.463', '2026-05-24 19:42:43.469'),
       (100, 921779622864396013, '0f2813b49d584b3ea13a89947d17f8ec', 'c13b596aa94e4c2a9f9cdd6059b28815', 1, 'admin',
        '研发部门', '接口访问', 'REQUEST', '/api/v1/hazard-points/page', 'GET', 'FILTER', '127.0.0.1', '内网IP',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36 Edg/148.0.0.0',
        NULL, NULL, 200, 'SUCCESS', NULL, 30, '2026-05-24 19:42:43.475', '2026-05-24 19:42:43.481'),
       (101, 921779622864396014, 'b920ee1aace448fdb7b1c6a07eefc1a5', 'c3cf626e96c84594aa8136072ad21816', 1, 'admin',
        '研发部门', '接口访问', 'REQUEST', '/api/v1/hazard-points/page', 'GET', 'FILTER', '127.0.0.1', '内网IP',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36 Edg/148.0.0.0',
        NULL, NULL, 200, 'SUCCESS', NULL, 25, '2026-05-24 19:42:45.335', '2026-05-24 19:42:45.341'),
       (102, 921779622864396015, 'c21e9215c6ee4e0b95973a3a6040964e', '08b848f915184485b86c9d2ec5759862', 1, 'admin',
        '研发部门', '接口访问', 'REQUEST', '/api/v1/hazard-points/page', 'GET', 'FILTER', '127.0.0.1', '内网IP',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36 Edg/148.0.0.0',
        NULL, NULL, 200, 'SUCCESS', NULL, 13, '2026-05-24 19:42:47.592', '2026-05-24 19:42:47.597'),
       (103, 921779622864396016, '1294dd884661438cb74e366eb5a3a88c', 'f43759e33fe749cab7d6d5a1e5bcc9f4', 1, 'admin',
        '研发部门', '隐患点分组', 'DELETE', '/api/v1/hazard-point-groups/6', 'DELETE',
        'com.zwei.iot.hazardpoint.controller.HazardPointGroupController.remove()', '127.0.0.1', NULL, NULL, '6 ',
        '{\"msg\":\"删除成功\",\"code\":200,\"timestamp\":1779622970722}', 200, 'SUCCESS', NULL, 59,
        '2026-05-24 19:42:50.745', '2026-05-24 19:42:50.751'),
       (104, 921779622864396017, 'c1b4bcac3345473c8f3b6ea9c121df4e', '6bbbf02c1fab41018aa40b1b3aacf682', 1, 'admin',
        '研发部门', '接口访问', 'REQUEST', '/api/v1/hazard-point-groups', 'GET', 'FILTER', '127.0.0.1', '内网IP',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36 Edg/148.0.0.0',
        NULL, NULL, 200, 'SUCCESS', NULL, 23, '2026-05-24 19:42:50.805', '2026-05-24 19:42:50.816'),
       (105, 921779622864396018, '54a425e8a81140b581ec84e38229180b', 'a2f8cd43c039458a926850cbb02841fb', 1, 'admin',
        '研发部门', '接口访问', 'REQUEST', '/api/v1/hazard-points/page', 'GET', 'FILTER', '127.0.0.1', '内网IP',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36 Edg/148.0.0.0',
        NULL, NULL, 200, 'SUCCESS', NULL, 19, '2026-05-24 19:42:52.051', '2026-05-24 19:42:52.055'),
       (106, 921779622864396019, '3f71da5c7d024a57af7e349a6609b4b4', '8fbe57e905444eb38831fcee61d341b6', 1, 'admin',
        '研发部门', '接口访问', 'REQUEST', '/api/v1/hazard-point-groups', 'GET', 'FILTER', '127.0.0.1', '内网IP',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36 Edg/148.0.0.0',
        NULL, NULL, 200, 'SUCCESS', NULL, 10, '2026-05-24 19:43:18.845', '2026-05-24 19:43:18.849'),
       (107, 921779622864396020, '9f2c01324ab443c5bc00dd69748a2085', 'ec16d0560e2e4ed487e8ba832bd3844a', 1, 'admin',
        '研发部门', '接口访问', 'REQUEST', '/api/v1/hazard-points/page', 'GET', 'FILTER', '127.0.0.1', '内网IP',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36 Edg/148.0.0.0',
        NULL, NULL, 200, 'SUCCESS', NULL, 18, '2026-05-24 19:43:18.855', '2026-05-24 19:43:18.861'),
       (108, 921779622864396021, '44df267b079c47329957b125e20959ab', 'ce565c83b23645c8937737f5d1d40a4b', 1, 'admin',
        '研发部门', '隐患点分组', 'INSERT', '/api/v1/hazard-point-groups', 'POST',
        'com.zwei.iot.hazardpoint.controller.HazardPointGroupController.add()', '127.0.0.1', NULL, NULL,
        '{\"code\":\"G1779623007852\",\"description\":\"\",\"name\":\"测试分组1\",\"sortOrder\":6,\"status\":1} ',
        '{\"msg\":\"新增成功\",\"code\":200,\"data\":{\"id\":9},\"timestamp\":1779623008006}', 200, 'SUCCESS', NULL, 18,
        '2026-05-24 19:43:28.011', '2026-05-24 19:43:28.015'),
       (109, 921779622864396022, '8c51037814f04026a04be97922a57b47', '2f9957c4897b4831a2b1ba1de07376e5', 1, 'admin',
        '研发部门', '接口访问', 'REQUEST', '/api/v1/hazard-point-groups', 'GET', 'FILTER', '127.0.0.1', '内网IP',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36 Edg/148.0.0.0',
        NULL, NULL, 200, 'SUCCESS', NULL, 12, '2026-05-24 19:43:28.055', '2026-05-24 19:43:28.058'),
       (110, 921779622864396023, '8daf514131194257a2e7a8010902fbb2', '57c4620f5b334e4e91103bddb6520fdc', 1, 'admin',
        '研发部门', '接口访问', 'REQUEST', '/api/v1/hazard-point-groups', 'GET', 'FILTER', '127.0.0.1', '内网IP',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36 Edg/148.0.0.0',
        NULL, NULL, 200, 'SUCCESS', NULL, 19, '2026-05-24 19:45:04.351', '2026-05-24 19:45:04.355'),
       (111, 921779622864396024, '3e8aed27b6c04fec932b9307ecbc65f8', 'f699b78334014b6aa4283f1675c0e39d', 1, 'admin',
        '研发部门', '接口访问', 'REQUEST', '/api/v1/hazard-points/page', 'GET', 'FILTER', '127.0.0.1', '内网IP',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36 Edg/148.0.0.0',
        NULL, NULL, 200, 'SUCCESS', NULL, 22, '2026-05-24 19:45:04.352', '2026-05-24 19:45:04.356'),
       (112, 921779628806843001, '36399205278b437aa9167dac817d0642', 'e94fcf827de84d7884e91c741aeebbb3', 1, 'admin',
        '研发部门', '接口访问', 'REQUEST', '/api/v1/organizations/tree', 'GET', 'FILTER', '127.0.0.1', '内网IP',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36 Edg/148.0.0.0',
        NULL, NULL, 200, 'SUCCESS', NULL, 400, '2026-05-24 21:21:30.509', '2026-05-24 21:21:30.793'),
       (113, 921779628806843002, '47ae4c3c59d94dd9be5885721cab983d', '9e1d20bf7b6b448db20930997547d623', 1, 'admin',
        '研发部门', '接口访问', 'REQUEST', '/api/v1/organizations/page', 'GET', 'FILTER', '127.0.0.1', '内网IP',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36 Edg/148.0.0.0',
        NULL, NULL, 200, 'SUCCESS', NULL, 400, '2026-05-24 21:21:30.509', '2026-05-24 21:21:30.793'),
       (114, 921779628806843003, '52eb97bd52314255b4321a7787ef979e', 'b360d218bb3946a2a0ee279c04df212e', 1, 'admin',
        '研发部门', '接口访问', 'REQUEST', '/api/v1/organizations/tree', 'GET', 'FILTER', '127.0.0.1', '内网IP',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36 Edg/148.0.0.0',
        NULL, NULL, 200, 'SUCCESS', NULL, 28, '2026-05-24 21:21:42.919', '2026-05-24 21:21:42.929'),
       (115, 921779628806843004, '7b78ebbdfea840c9b192c018052552ef', 'aa004b67403242a4856e741f1ffab7b9', 1, 'admin',
        '研发部门', '接口访问', 'REQUEST', '/api/v1/roles/optionselect', 'GET', 'FILTER', '127.0.0.1', '内网IP',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36 Edg/148.0.0.0',
        NULL, NULL, 200, 'SUCCESS', NULL, 70, '2026-05-24 21:21:42.958', '2026-05-24 21:21:42.966'),
       (116, 921779628806843005, '6a16336765ba4d23b4a408603b38b264', '2eb16f41567643aab09a34f36cfafbf6', 1, 'admin',
        '研发部门', '接口访问', 'REQUEST', '/api/v1/users/page', 'GET', 'FILTER', '127.0.0.1', '内网IP',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36 Edg/148.0.0.0',
        NULL, NULL, 200, 'SUCCESS', NULL, 79, '2026-05-24 21:21:42.969', '2026-05-24 21:21:42.975'),
       (117, 921779628806843006, 'a0c7eced625e45c9878c14ec9b574e54', '2af211dfa1ee4c8986bfa9d889b2f168', 1, 'admin',
        '研发部门', '接口访问', 'REQUEST', '/api/v1/users/1', 'GET', 'FILTER', '127.0.0.1', '内网IP',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36 Edg/148.0.0.0',
        NULL, NULL, 200, 'SUCCESS', NULL, 39, '2026-05-24 21:21:46.320', '2026-05-24 21:21:46.329'),
       (118, 921779628806843007, '97889037a48d43d99c0b2beb0df28de0', 'b5c6cf40fcd946acbdf2a9603ab4ac81', 1, 'admin',
        '研发部门', '接口访问', 'REQUEST', '/api/v1/organizations/tree', 'GET', 'FILTER', '127.0.0.1', '内网IP',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36 Edg/148.0.0.0',
        NULL, NULL, 200, 'SUCCESS', NULL, 21, '2026-05-24 21:21:53.437', '2026-05-24 21:21:53.443'),
       (119, 921779628806843008, '51947fa8cfbd4365b9a8eb9e16ab67cd', 'b894bc76b8c94b9abb583cf84bef6bd2', 1, 'admin',
        '研发部门', '接口访问', 'REQUEST', '/api/v1/organizations/page', 'GET', 'FILTER', '127.0.0.1', '内网IP',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36 Edg/148.0.0.0',
        NULL, NULL, 200, 'SUCCESS', NULL, 27, '2026-05-24 21:21:53.449', '2026-05-24 21:21:53.454'),
       (120, 921779628806843009, '49ed9a2b11c74053a961e70e1465cd9b', 'df9e1f2be37045e084bd8dd8b32b54ba', 1, 'admin',
        '研发部门', '接口访问', 'REQUEST', '/api/v1/roles/page', 'GET', 'FILTER', '127.0.0.1', '内网IP',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36 Edg/148.0.0.0',
        NULL, NULL, 200, 'SUCCESS', NULL, 86, '2026-05-24 21:21:56.513', '2026-05-24 21:21:56.526'),
       (121, 921779628806843010, '91e6f051076843b3bc8bdf79552f9d20', '2f98b5a492694103aa2ca69fabcacd14', 1, 'admin',
        '研发部门', '接口访问', 'REQUEST', '/api/v1/menus/tree', 'GET', 'FILTER', '127.0.0.1', '内网IP',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36 Edg/148.0.0.0',
        NULL, NULL, 200, 'SUCCESS', NULL, 139, '2026-05-24 21:21:56.565', '2026-05-24 21:21:56.575'),
       (122, 921779628806843011, 'a3a8fd90c04d46c782fb73ec7d026d4f', 'a5b31c4ac909441ab626f1b7f9cfd817', 1, 'admin',
        '研发部门', '接口访问', 'REQUEST', '/api/v1/roles/1', 'GET', 'FILTER', '127.0.0.1', '内网IP',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36 Edg/148.0.0.0',
        NULL, NULL, 200, 'SUCCESS', NULL, 12, '2026-05-24 21:22:03.699', '2026-05-24 21:22:03.705'),
       (123, 921779628806843012, 'd5c65a9241bf402e8e03429c69ef994c', '4124a18a741f4844b2604e85ba662471', 1, 'admin',
        '研发部门', '角色管理', 'UPDATE', '/api/v1/roles/1', 'PUT',
        'com.zwei.web.controller.system.SysRoleController.edit()', '127.0.0.1', NULL, NULL,
        '1 {\"code\":\"admin\",\"dataScope\":5,\"description\":\"超级管理员\",\"menuIds\":[],\"name\":\"超级管理员\",\"sortOrder\":1,\"status\":0} ',
        NULL, 200, 'FAIL', '不允许操作超级管理员角色', 38, '2026-05-24 21:22:11.980', '2026-05-24 21:22:11.990'),
       (124, 921779628806843013, 'c4fbf8d4da254e45b0596aa43c014016', '18fca0b4c5a741b2bc40b11c951747ef', 1, 'admin',
        '研发部门', '接口访问', 'REQUEST', '/api/v1/roles/page', 'GET', 'FILTER', '127.0.0.1', '内网IP',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36 Edg/148.0.0.0',
        NULL, NULL, 200, 'SUCCESS', NULL, 25, '2026-05-24 21:22:12.073', '2026-05-24 21:22:12.081'),
       (125, 921779628806843014, '6f6260a6338741b687f5abc45edcf9ee', 'b98dced18aa446cd837567f7abe9ef28', 1, 'admin',
        '研发部门', '接口访问', 'REQUEST', '/api/v1/roles/1', 'GET', 'FILTER', '127.0.0.1', '内网IP',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36 Edg/148.0.0.0',
        NULL, NULL, 200, 'SUCCESS', NULL, 20, '2026-05-24 21:22:13.762', '2026-05-24 21:22:13.774'),
       (126, 921779628806843015, 'ba455a64c32e4b29b2afdc05703c1293', '06a4aa138b374b6a9c1fde150097331e', 1, 'admin',
        '研发部门', '接口访问', 'REQUEST', '/api/v1/menus/tree', 'GET', 'FILTER', '127.0.0.1', '内网IP',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36 Edg/148.0.0.0',
        NULL, NULL, 200, 'SUCCESS', NULL, 25, '2026-05-24 21:22:17.955', '2026-05-24 21:22:17.959'),
       (127, 921779628806843016, '6fecbca7a0084069b921b86347d1c634', '232b769acec546c29cfae110a64639aa', 1, 'admin',
        '研发部门', '接口访问', 'REQUEST', '/api/v1/roles/page', 'GET', 'FILTER', '127.0.0.1', '内网IP',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36 Edg/148.0.0.0',
        NULL, NULL, 200, 'SUCCESS', NULL, 30, '2026-05-24 21:22:17.959', '2026-05-24 21:22:17.973'),
       (128, 921779628806843017, '3f455a19c81d4ce58190f03bb8cab090', 'fdb176550eac418892c0db8379734887', 1, 'admin',
        '研发部门', '接口访问', 'REQUEST', '/api/v1/roles/100', 'GET', 'FILTER', '127.0.0.1', '内网IP',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36 Edg/148.0.0.0',
        NULL, NULL, 200, 'SUCCESS', NULL, 10, '2026-05-24 21:22:27.843', '2026-05-24 21:22:27.848'),
       (129, 921779628806843018, '8cbcc19f3a544a07a0156c930237152f', '0db98216bd37439dbbd0b73d1483b3f8', 1, 'admin',
        '研发部门', '角色管理', 'UPDATE', '/api/v1/roles/100', 'PUT',
        'com.zwei.web.controller.system.SysRoleController.edit()', '127.0.0.1', NULL, NULL,
        '100 {\"code\":\"MONITOR\",\"dataScope\":2,\"description\":\"监测业务管理员\",\"menuIds\":[],\"name\":\"监测管理员\",\"sortOrder\":3,\"status\":0} ',
        '{\"msg\":\"修改成功\",\"code\":200,\"timestamp\":1779628952001}', 200, 'SUCCESS', NULL, 150,
        '2026-05-24 21:22:32.020', '2026-05-24 21:22:32.025'),
       (130, 921779628806843019, 'f69b56ceff474d67963e039d92d0a6ea', '9bceca71cb564baab6bbaedba55c4823', 1, 'admin',
        '研发部门', '接口访问', 'REQUEST', '/api/v1/roles/page', 'GET', 'FILTER', '127.0.0.1', '内网IP',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36 Edg/148.0.0.0',
        NULL, NULL, 200, 'SUCCESS', NULL, 26, '2026-05-24 21:22:32.106', '2026-05-24 21:22:32.110'),
       (131, 921779628806843020, 'd940275396dd4a27bd789f6877035aad', 'd6a44154bc6f41ef8e80e8a65c525174', 1, 'admin',
        '研发部门', '接口访问', 'REQUEST', '/api/v1/roles/101', 'GET', 'FILTER', '127.0.0.1', '内网IP',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36 Edg/148.0.0.0',
        NULL, NULL, 200, 'SUCCESS', NULL, 14, '2026-05-24 21:22:33.217', '2026-05-24 21:22:33.223'),
       (132, 921779628806843021, '9761bfe9d11a4f729895465f942c64e7', 'b4b65b5122c2456cbf44104f4219d621', 1, 'admin',
        '研发部门', '角色管理', 'UPDATE', '/api/v1/roles/101', 'PUT',
        'com.zwei.web.controller.system.SysRoleController.edit()', '127.0.0.1', NULL, NULL,
        '101 {\"code\":\"OPERATOR\",\"dataScope\":3,\"description\":\"普通操作员\",\"menuIds\":[],\"name\":\"操作员\",\"sortOrder\":5,\"status\":0} ',
        '{\"msg\":\"修改成功\",\"code\":200,\"timestamp\":1779628956197}', 200, 'SUCCESS', NULL, 78,
        '2026-05-24 21:22:36.198', '2026-05-24 21:22:36.204'),
       (133, 921779628806843022, 'b43351ce1a3b4afbbe8c8d62a78e2b18', 'b58ef8dd7a134a2b84d8d40db4db6624', 1, 'admin',
        '研发部门', '接口访问', 'REQUEST', '/api/v1/roles/page', 'GET', 'FILTER', '127.0.0.1', '内网IP',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36 Edg/148.0.0.0',
        NULL, NULL, 200, 'SUCCESS', NULL, 19, '2026-05-24 21:22:36.251', '2026-05-24 21:22:36.256'),
       (134, 921779628806843023, 'a92e769a150945c98fccfb4f6a2d5599', '43b4337f8a474288b4ffea1241896049', 1, 'admin',
        '研发部门', '接口访问', 'REQUEST', '/api/v1/menus/tree', 'GET', 'FILTER', '127.0.0.1', '内网IP',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36 Edg/148.0.0.0',
        NULL, NULL, 200, 'SUCCESS', NULL, 16, '2026-05-24 21:22:37.755', '2026-05-24 21:22:37.762'),
       (135, 921779628806843024, 'dbb118298401470d8b2dad09bc2689bf', '2c3c7ff1e1be4327a55189bb408d37c2', 1, 'admin',
        '研发部门', '接口访问', 'REQUEST', '/api/v1/roles/page', 'GET', 'FILTER', '127.0.0.1', '内网IP',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36 Edg/148.0.0.0',
        NULL, NULL, 200, 'SUCCESS', NULL, 22, '2026-05-24 21:22:37.762', '2026-05-24 21:22:37.767'),
       (136, 921779628806843025, '91e53fd0d323497bb116c6f0b0c655d8', 'f573a8e9617d44928c7cdff2d2065cdb', 1, 'admin',
        '研发部门', '接口访问', 'REQUEST', '/api/v1/roles/101', 'GET', 'FILTER', '127.0.0.1', '内网IP',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36 Edg/148.0.0.0',
        NULL, NULL, 200, 'SUCCESS', NULL, 9, '2026-05-24 21:22:40.033', '2026-05-24 21:22:40.038'),
       (137, 921779628806843026, '9cddd1fc0a844bd790243036dd828f0f', '8afe1b1e5b96428196f3cd48dfcb1d65', 1, 'admin',
        '研发部门', '角色管理', 'UPDATE', '/api/v1/roles/101', 'PUT',
        'com.zwei.web.controller.system.SysRoleController.edit()', '127.0.0.1', NULL, NULL,
        '101 {\"code\":\"OPERATOR\",\"dataScope\":3,\"description\":\"普通操作员\",\"menuIds\":[],\"name\":\"操作员\",\"sortOrder\":4,\"status\":0} ',
        '{\"msg\":\"修改成功\",\"code\":200,\"timestamp\":1779628962025}', 200, 'SUCCESS', NULL, 68,
        '2026-05-24 21:22:42.025', '2026-05-24 21:22:42.030'),
       (138, 921779628806843027, 'b8d8846d84f84bafb910f5989a323ef9', '422aaddb0b3f459e81a83b9f719c7316', 1, 'admin',
        '研发部门', '接口访问', 'REQUEST', '/api/v1/roles/page', 'GET', 'FILTER', '127.0.0.1', '内网IP',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36 Edg/148.0.0.0',
        NULL, NULL, 200, 'SUCCESS', NULL, 17, '2026-05-24 21:22:42.082', '2026-05-24 21:22:42.088'),
       (139, 921779628806843028, '708ee137421f43aabab73a176544f61a', '3078a9ebf4ef4415a6c0c19e3c2396b0', 1, 'admin',
        '研发部门', '接口访问', 'REQUEST', '/api/v1/organizations/tree', 'GET', 'FILTER', '127.0.0.1', '内网IP',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36 Edg/148.0.0.0',
        NULL, NULL, 200, 'SUCCESS', NULL, 13, '2026-05-24 21:23:01.978', '2026-05-24 21:23:01.984'),
       (140, 921779628806843029, 'eca5d3ef4bcb4068a4e65cd7fda4a647', 'a73a6901fdfc4957a44f924f3a540dcc', 1, 'admin',
        '研发部门', '接口访问', 'REQUEST', '/api/v1/organizations/page', 'GET', 'FILTER', '127.0.0.1', '内网IP',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36 Edg/148.0.0.0',
        NULL, NULL, 200, 'SUCCESS', NULL, 18, '2026-05-24 21:23:01.983', '2026-05-24 21:23:01.989'),
       (141, 921779628806843030, '3658e0848bfe4385bb84a65fad653d59', 'b3dbe7af1d0b4649bda9ca20344fe3d2', 1, 'admin',
        '研发部门', '接口访问', 'REQUEST', '/api/v1/organizations/100', 'GET', 'FILTER', '127.0.0.1', '内网IP',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36 Edg/148.0.0.0',
        NULL, NULL, 200, 'SUCCESS', NULL, 12, '2026-05-24 21:23:18.471', '2026-05-24 21:23:18.476'),
       (142, 921779628806843031, '33f275e403a04fb99a9447930f7a0036', '515d6faa1ad7459d8dfb80610121475f', 1, 'admin',
        '研发部门', '接口访问', 'REQUEST', '/api/v1/organizations/100', 'GET', 'FILTER', '127.0.0.1', '内网IP',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36 Edg/148.0.0.0',
        NULL, NULL, 200, 'SUCCESS', NULL, 11, '2026-05-24 21:23:27.193', '2026-05-24 21:23:27.197'),
       (143, 921779628806843032, '4a842cb127ae4dc1bb608cd5c449b45a', '609ff69757c144e5b22194e5f86f5a22', 1, 'admin',
        '研发部门', '接口访问', 'REQUEST', '/api/v1/organizations/100', 'GET', 'FILTER', '127.0.0.1', '内网IP',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36 Edg/148.0.0.0',
        NULL, NULL, 200, 'SUCCESS', NULL, 8, '2026-05-24 21:23:37.251', '2026-05-24 21:23:37.255'),
       (144, 921779628806843033, 'c9ee9a8adc854cbe8c560cb8f1c6f983', '570095d9ca4241228c0740fe45c8fe75', 1, 'admin',
        '研发部门', '接口访问', 'REQUEST', '/api/v1/organizations/101', 'GET', 'FILTER', '127.0.0.1', '内网IP',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36 Edg/148.0.0.0',
        NULL, NULL, 200, 'SUCCESS', NULL, 10, '2026-05-24 21:23:44.991', '2026-05-24 21:23:44.997');
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
INSERT INTO `log_runtime_record`
VALUES (1, 1779532378298001, NULL, NULL, 'WARN', 'org.springdoc.core.events.SpringDocAppInitializer', 'restartedMain',
        NULL, 'zwei-admin', NULL, NULL,
        'SpringDoc /v3/api-docs endpoint is enabled by default. To disable it in production, set the property \'springdoc.api-docs.enabled=false\'',
        'SpringDoc /v3/api-docs endpoint is enabled by default. To disable it in production, set the property \'springdoc.api-docs.enabled=false\'',
        NULL, NULL, '2026-05-23 18:33:04.457', '2026-05-23 18:33:04.613'),
       (2, 1779532378298005, NULL, NULL, 'WARN', 'net.dreamlu.mica.net.server.task.ServerHeartbeatTask',
        'DefaultTimerTaskService', NULL, 'zwei-admin', NULL, NULL,
        '\r\nMica-Mqtt-Server\r\n ├ 当前时间 :2026-05-23T18:35:04.336699200\r\n ├ 连接统计\r\n │ 	 ├ 共接受过连接数 :0\r\n │ 	 ├ 当前连接数 :0\r\n │ 	 └ 关闭过的连接数 :0\r\n ├ 消息统计\r\n │ 	 ├ 已处理消息 :0\r\n │ 	 ├ 已接收消息(packet/byte) :0/0\r\n │ 	 ├ 已发送消息(packet/byte) :0/0b\r\n │ 	 ├ 平均每次TCP包接收的字节数 :0.0\r\n │ 	 └ 平均每次TCP包接收的业务包 :0.0\r\n ├ 节点统计\r\n │ 	 ├ clientNodes :0\r\n │ 	 ├ 所有连接 :0\r\n │ 	 ├ 绑定user数 :0\r\n │ 	 ├ 绑定token数 :0\r\n │ 	 └ 等待同步消息响应 :0\r\n ├ 队列统计\r\n │ 	 ├ 解码队列总数 :0\r\n │ 	 ├ 处理队列总数 :0\r\n │ 	 └ 发送队列总数 :0\r\n └ 群组\r\n   	 └ groupmap: 0',
        '\r\nMica-Mqtt-Server\r\n ├ 当前时间 :2026-05-23T18:35:04.336699200\r\n ├ 连接统计\r\n │ 	 ├ 共接受过连接数 :0\r\n │ 	 ├ 当前连接数 :0\r\n │ 	 └ 关闭过的连接数 :0\r\n ├ 消息统计\r\n │ 	 ├ 已处理消息 :0\r\n │ 	 ├ 已接收消息(packet/byte) :0/0\r\n │ 	 ├ 已发送消息(packet/byte) :0/0b\r\n │ 	 ├ 平均每次TCP包接收的字节数 :0.0\r\n │ 	 └ 平均每次TCP包接收的业务包 :0.0\r\n ├ 节点统计\r\n │ 	 ├ clientNodes :0\r\n │ 	 ├ 所有连接 :0\r\n │ 	 ├ 绑定user数 :0\r\n │ 	 ├ 绑定token数 :0\r\n │ 	 └ 等待同步消息响应 :0\r\n ├ 队列统计\r\n │ 	 ├ 解码队列总数 :0\r\n │ 	 ├ 处理队列总数 :0\r\n │ 	 └ 发送队列总数 :0\r\n └ 群组\r\n   	 └ groupmap: 0',
        NULL, NULL, '2026-05-23 18:35:04.336', '2026-05-23 18:35:04.342'),
       (3, 1779532378298006, NULL, NULL, 'WARN', 'net.dreamlu.mica.net.server.task.ServerHeartbeatTask',
        'DefaultTimerTaskService', NULL, 'zwei-admin', NULL, NULL,
        'Mica-Mqtt-Server, 检查心跳, 共0个连接, 取锁耗时0ms, 循环耗时16ms, 心跳检测周期:120000ms, 心跳超时时间:90000ms',
        'Mica-Mqtt-Server, 检查心跳, 共0个连接, 取锁耗时0ms, 循环耗时16ms, 心跳检测周期:120000ms, 心跳超时时间:90000ms',
        NULL, NULL, '2026-05-23 18:35:04.352', '2026-05-23 18:35:04.355'),
       (4, 1779532378298007, NULL, NULL, 'ERROR', 'net.dreamlu.mica.net.utils.thread.pool.TioCallerRunsPolicy',
        'Thread-15', NULL, 'zwei-admin', NULL, NULL, 'sun.nio.ch.AsynchronousChannelGroupImpl$1',
        'sun.nio.ch.AsynchronousChannelGroupImpl$1', NULL, NULL, '2026-05-23 18:35:51.539', '2026-05-23 18:35:51.544'),
       (5, 1779532378298008, NULL, NULL, 'ERROR', 'net.dreamlu.mica.net.utils.thread.pool.TioCallerRunsPolicy',
        'Thread-16', NULL, 'zwei-admin', NULL, NULL, 'sun.nio.ch.AsynchronousChannelGroupImpl$1',
        'sun.nio.ch.AsynchronousChannelGroupImpl$1', NULL, NULL, '2026-05-23 18:35:51.550', '2026-05-23 18:35:51.554'),
       (6, 1779532378298009, NULL, NULL, 'ERROR', 'net.dreamlu.mica.net.utils.thread.pool.TioCallerRunsPolicy',
        'Thread-17', NULL, 'zwei-admin', NULL, NULL, 'sun.nio.ch.AsynchronousChannelGroupImpl$1',
        'sun.nio.ch.AsynchronousChannelGroupImpl$1', NULL, NULL, '2026-05-23 18:35:51.558', '2026-05-23 18:35:51.564'),
       (7, 1779532554804001, NULL, NULL, 'WARN', 'org.springdoc.core.events.SpringDocAppInitializer', 'restartedMain',
        NULL, 'zwei-admin', NULL, NULL,
        'SpringDoc /v3/api-docs endpoint is enabled by default. To disable it in production, set the property \'springdoc.api-docs.enabled=false\'',
        'SpringDoc /v3/api-docs endpoint is enabled by default. To disable it in production, set the property \'springdoc.api-docs.enabled=false\'',
        NULL, NULL, '2026-05-23 18:36:02.066', '2026-05-23 18:36:02.074'),
       (8, 1779532554804002, NULL, NULL, 'ERROR', 'net.dreamlu.mica.net.utils.thread.pool.TioCallerRunsPolicy',
        'Thread-31', NULL, 'zwei-admin', NULL, NULL, 'sun.nio.ch.AsynchronousChannelGroupImpl$1',
        'sun.nio.ch.AsynchronousChannelGroupImpl$1', NULL, NULL, '2026-05-23 18:36:03.484', '2026-05-23 18:36:03.492'),
       (9, 1779532554804003, NULL, NULL, 'ERROR', 'net.dreamlu.mica.net.utils.thread.pool.TioCallerRunsPolicy',
        'Thread-32', NULL, 'zwei-admin', NULL, NULL, 'sun.nio.ch.AsynchronousChannelGroupImpl$1',
        'sun.nio.ch.AsynchronousChannelGroupImpl$1', NULL, NULL, '2026-05-23 18:36:03.498', '2026-05-23 18:36:03.502'),
       (10, 1779532554804004, NULL, NULL, 'ERROR', 'net.dreamlu.mica.net.utils.thread.pool.TioCallerRunsPolicy',
        'Thread-33', NULL, 'zwei-admin', NULL, NULL, 'sun.nio.ch.AsynchronousChannelGroupImpl$1',
        'sun.nio.ch.AsynchronousChannelGroupImpl$1', NULL, NULL, '2026-05-23 18:36:03.506', '2026-05-23 18:36:03.511'),
       (11, 1779532567021001, NULL, NULL, 'WARN',
        'org.springframework.boot.web.server.servlet.context.AnnotationConfigServletWebServerApplicationContext',
        'restartedMain', NULL, 'zwei-admin', NULL, NULL,
        'Exception encountered during context initialization - cancelling refresh attempt: org.springframework.context.ApplicationContextException: Failed to start bean \'webServerStartStop\'',
        'Exception encountered during context initialization - cancelling refresh attempt: org.springframework.context.ApplicationContextException: Failed to start bean \'webServerStartStop\'',
        NULL, NULL, '2026-05-23 18:36:12.805', '2026-05-23 18:36:12.812'),
       (12, 1779533020098001, NULL, NULL, 'WARN', 'org.springdoc.core.events.SpringDocAppInitializer', 'restartedMain',
        NULL, 'zwei-admin', NULL, NULL,
        'SpringDoc /v3/api-docs endpoint is enabled by default. To disable it in production, set the property \'springdoc.api-docs.enabled=false\'',
        'SpringDoc /v3/api-docs endpoint is enabled by default. To disable it in production, set the property \'springdoc.api-docs.enabled=false\'',
        NULL, NULL, '2026-05-23 18:43:46.517', '2026-05-23 18:43:46.686'),
       (13, 1779533020098003, NULL, NULL, 'ERROR',
        'org.apache.catalina.core.ContainerBase.[Tomcat].[localhost].[/].[dispatcherServlet]', 'http-nio-8080-exec-2',
        NULL, 'zwei-admin', NULL, NULL,
        'Servlet.service() for servlet [dispatcherServlet] in context with path [] threw exception',
        'Servlet.service() for servlet [dispatcherServlet] in context with path [] threw exception',
        'com.zwei.common.exception.ServiceException', '获取用户信息异常', '2026-05-23 18:44:11.266',
        '2026-05-23 18:44:11.272'),
       (14, 1779533020098004, NULL, NULL, 'ERROR',
        'org.apache.catalina.core.ContainerBase.[Tomcat].[localhost].[/].[dispatcherServlet]', 'http-nio-8080-exec-2',
        NULL, 'zwei-admin', NULL, NULL, 'Servlet.service() for servlet [dispatcherServlet] threw exception',
        'Servlet.service() for servlet [dispatcherServlet] threw exception',
        'org.springframework.security.authorization.AuthorizationDeniedException', 'Access Denied',
        '2026-05-23 18:44:11.281', '2026-05-23 18:44:11.285'),
       (15, 1779533020098005, NULL, NULL, 'ERROR', 'org.apache.catalina.core.ContainerBase.[Tomcat].[localhost]',
        'http-nio-8080-exec-2', NULL, 'zwei-admin', NULL, NULL,
        'Exception Processing [ErrorPage[errorCode=0, location=/error]]',
        'Exception Processing [ErrorPage[errorCode=0, location=/error]]', 'jakarta.servlet.ServletException',
        'Unable to handle the Spring Security Exception because the response is already committed.',
        '2026-05-23 18:44:11.288', '2026-05-23 18:44:11.291'),
       (16, 1779533020098006, NULL, NULL, 'WARN', 'net.dreamlu.mica.net.server.task.ServerHeartbeatTask',
        'DefaultTimerTaskService', NULL, 'zwei-admin', NULL, NULL,
        '\r\nMica-Mqtt-Server\r\n ├ 当前时间 :2026-05-23T18:45:46.343445900\r\n ├ 连接统计\r\n │ 	 ├ 共接受过连接数 :0\r\n │ 	 ├ 当前连接数 :0\r\n │ 	 └ 关闭过的连接数 :0\r\n ├ 消息统计\r\n │ 	 ├ 已处理消息 :0\r\n │ 	 ├ 已接收消息(packet/byte) :0/0\r\n │ 	 ├ 已发送消息(packet/byte) :0/0b\r\n │ 	 ├ 平均每次TCP包接收的字节数 :0.0\r\n │ 	 └ 平均每次TCP包接收的业务包 :0.0\r\n ├ 节点统计\r\n │ 	 ├ clientNodes :0\r\n │ 	 ├ 所有连接 :0\r\n │ 	 ├ 绑定user数 :0\r\n │ 	 ├ 绑定token数 :0\r\n │ 	 └ 等待同步消息响应 :0\r\n ├ 队列统计\r\n │ 	 ├ 解码队列总数 :0\r\n │ 	 ├ 处理队列总数 :0\r\n │ 	 └ 发送队列总数 :0\r\n └ 群组\r\n   	 └ groupmap: 0',
        '\r\nMica-Mqtt-Server\r\n ├ 当前时间 :2026-05-23T18:45:46.343445900\r\n ├ 连接统计\r\n │ 	 ├ 共接受过连接数 :0\r\n │ 	 ├ 当前连接数 :0\r\n │ 	 └ 关闭过的连接数 :0\r\n ├ 消息统计\r\n │ 	 ├ 已处理消息 :0\r\n │ 	 ├ 已接收消息(packet/byte) :0/0\r\n │ 	 ├ 已发送消息(packet/byte) :0/0b\r\n │ 	 ├ 平均每次TCP包接收的字节数 :0.0\r\n │ 	 └ 平均每次TCP包接收的业务包 :0.0\r\n ├ 节点统计\r\n │ 	 ├ clientNodes :0\r\n │ 	 ├ 所有连接 :0\r\n │ 	 ├ 绑定user数 :0\r\n │ 	 ├ 绑定token数 :0\r\n │ 	 └ 等待同步消息响应 :0\r\n ├ 队列统计\r\n │ 	 ├ 解码队列总数 :0\r\n │ 	 ├ 处理队列总数 :0\r\n │ 	 └ 发送队列总数 :0\r\n └ 群组\r\n   	 └ groupmap: 0',
        NULL, NULL, '2026-05-23 18:45:46.343', '2026-05-23 18:45:46.355'),
       (17, 1779533020098007, NULL, NULL, 'WARN', 'net.dreamlu.mica.net.server.task.ServerHeartbeatTask',
        'DefaultTimerTaskService', NULL, 'zwei-admin', NULL, NULL,
        'Mica-Mqtt-Server, 检查心跳, 共0个连接, 取锁耗时0ms, 循环耗时20ms, 心跳检测周期:120000ms, 心跳超时时间:90000ms',
        'Mica-Mqtt-Server, 检查心跳, 共0个连接, 取锁耗时0ms, 循环耗时20ms, 心跳检测周期:120000ms, 心跳超时时间:90000ms',
        NULL, NULL, '2026-05-23 18:45:46.364', '2026-05-23 18:45:46.369'),
       (18, 1779533020098008, NULL, NULL, 'ERROR',
        'org.apache.catalina.core.ContainerBase.[Tomcat].[localhost].[/].[dispatcherServlet]', 'http-nio-8080-exec-7',
        NULL, 'zwei-admin', NULL, NULL,
        'Servlet.service() for servlet [dispatcherServlet] in context with path [] threw exception',
        'Servlet.service() for servlet [dispatcherServlet] in context with path [] threw exception',
        'com.zwei.common.exception.ServiceException', '获取用户信息异常', '2026-05-23 18:46:51.504',
        '2026-05-23 18:46:51.510'),
       (19, 1779533020098009, NULL, NULL, 'ERROR',
        'org.apache.catalina.core.ContainerBase.[Tomcat].[localhost].[/].[dispatcherServlet]', 'http-nio-8080-exec-7',
        NULL, 'zwei-admin', NULL, NULL, 'Servlet.service() for servlet [dispatcherServlet] threw exception',
        'Servlet.service() for servlet [dispatcherServlet] threw exception',
        'org.springframework.security.authorization.AuthorizationDeniedException', 'Access Denied',
        '2026-05-23 18:46:51.515', '2026-05-23 18:46:51.521'),
       (20, 1779533020098010, NULL, NULL, 'ERROR', 'org.apache.catalina.core.ContainerBase.[Tomcat].[localhost]',
        'http-nio-8080-exec-7', NULL, 'zwei-admin', NULL, NULL,
        'Exception Processing [ErrorPage[errorCode=0, location=/error]]',
        'Exception Processing [ErrorPage[errorCode=0, location=/error]]', 'jakarta.servlet.ServletException',
        'Unable to handle the Spring Security Exception because the response is already committed.',
        '2026-05-23 18:46:51.525', '2026-05-23 18:46:51.528'),
       (21, 1779533020098012, NULL, NULL, 'ERROR',
        'org.apache.catalina.core.ContainerBase.[Tomcat].[localhost].[/].[dispatcherServlet]', 'http-nio-8080-exec-5',
        NULL, 'zwei-admin', NULL, NULL,
        'Servlet.service() for servlet [dispatcherServlet] in context with path [] threw exception',
        'Servlet.service() for servlet [dispatcherServlet] in context with path [] threw exception',
        'com.zwei.common.exception.ServiceException', '获取用户信息异常', '2026-05-23 18:47:05.955',
        '2026-05-23 18:47:05.962'),
       (22, 1779533020098013, NULL, NULL, 'ERROR',
        'org.apache.catalina.core.ContainerBase.[Tomcat].[localhost].[/].[dispatcherServlet]', 'http-nio-8080-exec-5',
        NULL, 'zwei-admin', NULL, NULL, 'Servlet.service() for servlet [dispatcherServlet] threw exception',
        'Servlet.service() for servlet [dispatcherServlet] threw exception',
        'org.springframework.security.authorization.AuthorizationDeniedException', 'Access Denied',
        '2026-05-23 18:47:05.969', '2026-05-23 18:47:05.974'),
       (23, 1779533020098014, NULL, NULL, 'ERROR', 'org.apache.catalina.core.ContainerBase.[Tomcat].[localhost]',
        'http-nio-8080-exec-5', NULL, 'zwei-admin', NULL, NULL,
        'Exception Processing [ErrorPage[errorCode=0, location=/error]]',
        'Exception Processing [ErrorPage[errorCode=0, location=/error]]', 'jakarta.servlet.ServletException',
        'Unable to handle the Spring Security Exception because the response is already committed.',
        '2026-05-23 18:47:05.978', '2026-05-23 18:47:05.983'),
       (24, 1779533020098015, NULL, NULL, 'WARN', 'net.dreamlu.mica.net.server.task.ServerHeartbeatTask',
        'DefaultTimerTaskService', NULL, 'zwei-admin', NULL, NULL,
        '\r\nMica-Mqtt-Server\r\n ├ 当前时间 :2026-05-23T18:47:46.338427600\r\n ├ 连接统计\r\n │ 	 ├ 共接受过连接数 :0\r\n │ 	 ├ 当前连接数 :0\r\n │ 	 └ 关闭过的连接数 :0\r\n ├ 消息统计\r\n │ 	 ├ 已处理消息 :0\r\n │ 	 ├ 已接收消息(packet/byte) :0/0\r\n │ 	 ├ 已发送消息(packet/byte) :0/0b\r\n │ 	 ├ 平均每次TCP包接收的字节数 :0.0\r\n │ 	 └ 平均每次TCP包接收的业务包 :0.0\r\n ├ 节点统计\r\n │ 	 ├ clientNodes :0\r\n │ 	 ├ 所有连接 :0\r\n │ 	 ├ 绑定user数 :0\r\n │ 	 ├ 绑定token数 :0\r\n │ 	 └ 等待同步消息响应 :0\r\n ├ 队列统计\r\n │ 	 ├ 解码队列总数 :0\r\n │ 	 ├ 处理队列总数 :0\r\n │ 	 └ 发送队列总数 :0\r\n └ 群组\r\n   	 └ groupmap: 0',
        '\r\nMica-Mqtt-Server\r\n ├ 当前时间 :2026-05-23T18:47:46.338427600\r\n ├ 连接统计\r\n │ 	 ├ 共接受过连接数 :0\r\n │ 	 ├ 当前连接数 :0\r\n │ 	 └ 关闭过的连接数 :0\r\n ├ 消息统计\r\n │ 	 ├ 已处理消息 :0\r\n │ 	 ├ 已接收消息(packet/byte) :0/0\r\n │ 	 ├ 已发送消息(packet/byte) :0/0b\r\n │ 	 ├ 平均每次TCP包接收的字节数 :0.0\r\n │ 	 └ 平均每次TCP包接收的业务包 :0.0\r\n ├ 节点统计\r\n │ 	 ├ clientNodes :0\r\n │ 	 ├ 所有连接 :0\r\n │ 	 ├ 绑定user数 :0\r\n │ 	 ├ 绑定token数 :0\r\n │ 	 └ 等待同步消息响应 :0\r\n ├ 队列统计\r\n │ 	 ├ 解码队列总数 :0\r\n │ 	 ├ 处理队列总数 :0\r\n │ 	 └ 发送队列总数 :0\r\n └ 群组\r\n   	 └ groupmap: 0',
        NULL, NULL, '2026-05-23 18:47:46.338', '2026-05-23 18:47:46.347'),
       (25, 1779533020098016, NULL, NULL, 'WARN', 'net.dreamlu.mica.net.server.task.ServerHeartbeatTask',
        'DefaultTimerTaskService', NULL, 'zwei-admin', NULL, NULL,
        'Mica-Mqtt-Server, 检查心跳, 共0个连接, 取锁耗时0ms, 循环耗时18ms, 心跳检测周期:120000ms, 心跳超时时间:90000ms',
        'Mica-Mqtt-Server, 检查心跳, 共0个连接, 取锁耗时0ms, 循环耗时18ms, 心跳检测周期:120000ms, 心跳超时时间:90000ms',
        NULL, NULL, '2026-05-23 18:47:46.356', '2026-05-23 18:47:46.362'),
       (26, 1779533020098018, NULL, NULL, 'WARN', 'net.dreamlu.mica.net.server.task.ServerHeartbeatTask',
        'DefaultTimerTaskService', NULL, 'zwei-admin', NULL, NULL,
        '\r\nMica-Mqtt-Server\r\n ├ 当前时间 :2026-05-23T18:49:46.343596300\r\n ├ 连接统计\r\n │ 	 ├ 共接受过连接数 :0\r\n │ 	 ├ 当前连接数 :0\r\n │ 	 └ 关闭过的连接数 :0\r\n ├ 消息统计\r\n │ 	 ├ 已处理消息 :0\r\n │ 	 ├ 已接收消息(packet/byte) :0/0\r\n │ 	 ├ 已发送消息(packet/byte) :0/0b\r\n │ 	 ├ 平均每次TCP包接收的字节数 :0.0\r\n │ 	 └ 平均每次TCP包接收的业务包 :0.0\r\n ├ 节点统计\r\n │ 	 ├ clientNodes :0\r\n │ 	 ├ 所有连接 :0\r\n │ 	 ├ 绑定user数 :0\r\n │ 	 ├ 绑定token数 :0\r\n │ 	 └ 等待同步消息响应 :0\r\n ├ 队列统计\r\n │ 	 ├ 解码队列总数 :0\r\n │ 	 ├ 处理队列总数 :0\r\n │ 	 └ 发送队列总数 :0\r\n └ 群组\r\n   	 └ groupmap: 0',
        '\r\nMica-Mqtt-Server\r\n ├ 当前时间 :2026-05-23T18:49:46.343596300\r\n ├ 连接统计\r\n │ 	 ├ 共接受过连接数 :0\r\n │ 	 ├ 当前连接数 :0\r\n │ 	 └ 关闭过的连接数 :0\r\n ├ 消息统计\r\n │ 	 ├ 已处理消息 :0\r\n │ 	 ├ 已接收消息(packet/byte) :0/0\r\n │ 	 ├ 已发送消息(packet/byte) :0/0b\r\n │ 	 ├ 平均每次TCP包接收的字节数 :0.0\r\n │ 	 └ 平均每次TCP包接收的业务包 :0.0\r\n ├ 节点统计\r\n │ 	 ├ clientNodes :0\r\n │ 	 ├ 所有连接 :0\r\n │ 	 ├ 绑定user数 :0\r\n │ 	 ├ 绑定token数 :0\r\n │ 	 └ 等待同步消息响应 :0\r\n ├ 队列统计\r\n │ 	 ├ 解码队列总数 :0\r\n │ 	 ├ 处理队列总数 :0\r\n │ 	 └ 发送队列总数 :0\r\n └ 群组\r\n   	 └ groupmap: 0',
        NULL, NULL, '2026-05-23 18:49:46.343', '2026-05-23 18:49:46.348'),
       (27, 1779533020098019, NULL, NULL, 'WARN', 'net.dreamlu.mica.net.server.task.ServerHeartbeatTask',
        'DefaultTimerTaskService', NULL, 'zwei-admin', NULL, NULL,
        'Mica-Mqtt-Server, 检查心跳, 共0个连接, 取锁耗时0ms, 循环耗时10ms, 心跳检测周期:120000ms, 心跳超时时间:90000ms',
        'Mica-Mqtt-Server, 检查心跳, 共0个连接, 取锁耗时0ms, 循环耗时10ms, 心跳检测周期:120000ms, 心跳超时时间:90000ms',
        NULL, NULL, '2026-05-23 18:49:46.353', '2026-05-23 18:49:46.362'),
       (28, 1779533020098020, NULL, NULL, 'ERROR',
        'org.apache.catalina.core.ContainerBase.[Tomcat].[localhost].[/].[dispatcherServlet]', 'http-nio-8080-exec-14',
        NULL, 'zwei-admin', NULL, NULL, 'Servlet.service() for servlet [dispatcherServlet] threw exception',
        'Servlet.service() for servlet [dispatcherServlet] threw exception',
        'org.springframework.security.authorization.AuthorizationDeniedException', 'Access Denied',
        '2026-05-23 18:49:46.355', '2026-05-23 18:49:46.369'),
       (29, 1779533020098021, NULL, NULL, 'ERROR',
        'org.apache.catalina.core.ContainerBase.[Tomcat].[localhost].[/].[dispatcherServlet]', 'http-nio-8080-exec-14',
        NULL, 'zwei-admin', NULL, NULL,
        'Servlet.service() for servlet [dispatcherServlet] in context with path [] threw exception [Unable to handle the Spring Security Exception because the response is already committed.] with root cause',
        'Servlet.service() for servlet [dispatcherServlet] in context with path [] threw exception [Unable to handle the Spring Security Exception because the response is already committed.] with root cause',
        'org.springframework.security.authorization.AuthorizationDeniedException', 'Access Denied',
        '2026-05-23 18:49:46.378', '2026-05-23 18:49:46.381'),
       (30, 1779533020098022, NULL, NULL, 'WARN', 'net.dreamlu.mica.net.server.task.ServerHeartbeatTask',
        'DefaultTimerTaskService', NULL, 'zwei-admin', NULL, NULL,
        '\r\nMica-Mqtt-Server\r\n ├ 当前时间 :2026-05-23T18:51:46.336628\r\n ├ 连接统计\r\n │ 	 ├ 共接受过连接数 :0\r\n │ 	 ├ 当前连接数 :0\r\n │ 	 └ 关闭过的连接数 :0\r\n ├ 消息统计\r\n │ 	 ├ 已处理消息 :0\r\n │ 	 ├ 已接收消息(packet/byte) :0/0\r\n │ 	 ├ 已发送消息(packet/byte) :0/0b\r\n │ 	 ├ 平均每次TCP包接收的字节数 :0.0\r\n │ 	 └ 平均每次TCP包接收的业务包 :0.0\r\n ├ 节点统计\r\n │ 	 ├ clientNodes :0\r\n │ 	 ├ 所有连接 :0\r\n │ 	 ├ 绑定user数 :0\r\n │ 	 ├ 绑定token数 :0\r\n │ 	 └ 等待同步消息响应 :0\r\n ├ 队列统计\r\n │ 	 ├ 解码队列总数 :0\r\n │ 	 ├ 处理队列总数 :0\r\n │ 	 └ 发送队列总数 :0\r\n └ 群组\r\n   	 └ groupmap: 0',
        '\r\nMica-Mqtt-Server\r\n ├ 当前时间 :2026-05-23T18:51:46.336628\r\n ├ 连接统计\r\n │ 	 ├ 共接受过连接数 :0\r\n │ 	 ├ 当前连接数 :0\r\n │ 	 └ 关闭过的连接数 :0\r\n ├ 消息统计\r\n │ 	 ├ 已处理消息 :0\r\n │ 	 ├ 已接收消息(packet/byte) :0/0\r\n │ 	 ├ 已发送消息(packet/byte) :0/0b\r\n │ 	 ├ 平均每次TCP包接收的字节数 :0.0\r\n │ 	 └ 平均每次TCP包接收的业务包 :0.0\r\n ├ 节点统计\r\n │ 	 ├ clientNodes :0\r\n │ 	 ├ 所有连接 :0\r\n │ 	 ├ 绑定user数 :0\r\n │ 	 ├ 绑定token数 :0\r\n │ 	 └ 等待同步消息响应 :0\r\n ├ 队列统计\r\n │ 	 ├ 解码队列总数 :0\r\n │ 	 ├ 处理队列总数 :0\r\n │ 	 └ 发送队列总数 :0\r\n └ 群组\r\n   	 └ groupmap: 0',
        NULL, NULL, '2026-05-23 18:51:46.336', '2026-05-23 18:51:46.340'),
       (31, 1779533020098023, NULL, NULL, 'WARN', 'net.dreamlu.mica.net.server.task.ServerHeartbeatTask',
        'DefaultTimerTaskService', NULL, 'zwei-admin', NULL, NULL,
        'Mica-Mqtt-Server, 检查心跳, 共0个连接, 取锁耗时0ms, 循环耗时8ms, 心跳检测周期:120000ms, 心跳超时时间:90000ms',
        'Mica-Mqtt-Server, 检查心跳, 共0个连接, 取锁耗时0ms, 循环耗时8ms, 心跳检测周期:120000ms, 心跳超时时间:90000ms',
        NULL, NULL, '2026-05-23 18:51:46.344', '2026-05-23 18:51:46.348'),
       (32, 1779533020098024, NULL, NULL, 'ERROR', 'net.dreamlu.mica.net.utils.thread.pool.TioCallerRunsPolicy',
        'Thread-15', NULL, 'zwei-admin', NULL, NULL, 'sun.nio.ch.AsynchronousChannelGroupImpl$1',
        'sun.nio.ch.AsynchronousChannelGroupImpl$1', NULL, NULL, '2026-05-23 18:51:53.162', '2026-05-23 18:51:53.165'),
       (33, 1779533020098025, NULL, NULL, 'ERROR', 'net.dreamlu.mica.net.utils.thread.pool.TioCallerRunsPolicy',
        'Thread-16', NULL, 'zwei-admin', NULL, NULL, 'sun.nio.ch.AsynchronousChannelGroupImpl$1',
        'sun.nio.ch.AsynchronousChannelGroupImpl$1', NULL, NULL, '2026-05-23 18:51:53.168', '2026-05-23 18:51:53.170'),
       (34, 1779533020098026, NULL, NULL, 'ERROR', 'net.dreamlu.mica.net.utils.thread.pool.TioCallerRunsPolicy',
        'Thread-17', NULL, 'zwei-admin', NULL, NULL, 'sun.nio.ch.AsynchronousChannelGroupImpl$1',
        'sun.nio.ch.AsynchronousChannelGroupImpl$1', NULL, NULL, '2026-05-23 18:51:53.173', '2026-05-23 18:51:53.175'),
       (35, 1779533020098028, NULL, NULL, 'ERROR',
        'org.apache.catalina.core.ContainerBase.[Tomcat].[localhost].[/].[dispatcherServlet]', 'http-nio-8080-exec-15',
        NULL, 'zwei-admin', NULL, NULL,
        'Servlet.service() for servlet [dispatcherServlet] in context with path [] threw exception [Unable to handle the Spring Security Exception because the response is already committed.] with root cause',
        'Servlet.service() for servlet [dispatcherServlet] in context with path [] threw exception [Unable to handle the Spring Security Exception because the response is already committed.] with root cause',
        'org.springframework.security.authorization.AuthorizationDeniedException', 'Access Denied',
        '2026-05-23 18:52:23.979', '2026-05-23 18:52:24.007'),
       (36, 1779533020098029, NULL, NULL, 'ERROR',
        'org.apache.catalina.core.ContainerBase.[Tomcat].[localhost].[/].[dispatcherServlet]', 'http-nio-8080-exec-15',
        NULL, 'zwei-admin', NULL, NULL, 'Servlet.service() for servlet [dispatcherServlet] threw exception',
        'Servlet.service() for servlet [dispatcherServlet] threw exception',
        'org.springframework.security.authorization.AuthorizationDeniedException', 'Access Denied',
        '2026-05-23 18:52:24.011', '2026-05-23 18:52:24.022'),
       (37, 1779533020098030, NULL, NULL, 'ERROR', 'org.apache.catalina.core.ContainerBase.[Tomcat].[localhost]',
        'http-nio-8080-exec-15', NULL, 'zwei-admin', NULL, NULL,
        'Exception Processing [ErrorPage[errorCode=0, location=/error]]',
        'Exception Processing [ErrorPage[errorCode=0, location=/error]]', 'jakarta.servlet.ServletException',
        'Unable to handle the Spring Security Exception because the response is already committed.',
        '2026-05-23 18:52:24.025', '2026-05-23 18:52:24.036'),
       (38, 1779533545392001, NULL, NULL, 'WARN', 'org.springdoc.core.events.SpringDocAppInitializer', 'restartedMain',
        NULL, 'zwei-admin', NULL, NULL,
        'SpringDoc /v3/api-docs endpoint is enabled by default. To disable it in production, set the property \'springdoc.api-docs.enabled=false\'',
        'SpringDoc /v3/api-docs endpoint is enabled by default. To disable it in production, set the property \'springdoc.api-docs.enabled=false\'',
        NULL, NULL, '2026-05-23 18:52:30.185', '2026-05-23 18:52:30.189'),
       (39, 1779533545392002, NULL, NULL, 'ERROR', 'net.dreamlu.mica.net.utils.thread.pool.TioCallerRunsPolicy',
        'Thread-31', NULL, 'zwei-admin', NULL, NULL, 'sun.nio.ch.AsynchronousChannelGroupImpl$1',
        'sun.nio.ch.AsynchronousChannelGroupImpl$1', NULL, NULL, '2026-05-23 18:52:31.594', '2026-05-23 18:52:31.599'),
       (40, 1779533545392003, NULL, NULL, 'ERROR', 'net.dreamlu.mica.net.utils.thread.pool.TioCallerRunsPolicy',
        'Thread-32', NULL, 'zwei-admin', NULL, NULL, 'sun.nio.ch.AsynchronousChannelGroupImpl$1',
        'sun.nio.ch.AsynchronousChannelGroupImpl$1', NULL, NULL, '2026-05-23 18:52:31.606', '2026-05-23 18:52:31.611'),
       (41, 1779533545392004, NULL, NULL, 'ERROR', 'net.dreamlu.mica.net.utils.thread.pool.TioCallerRunsPolicy',
        'Thread-33', NULL, 'zwei-admin', NULL, NULL, 'sun.nio.ch.AsynchronousChannelGroupImpl$1',
        'sun.nio.ch.AsynchronousChannelGroupImpl$1', NULL, NULL, '2026-05-23 18:52:31.616', '2026-05-23 18:52:31.620'),
       (42, 1779533553906001, NULL, NULL, 'WARN', 'org.springdoc.core.events.SpringDocAppInitializer', 'restartedMain',
        NULL, 'zwei-admin', NULL, NULL,
        'SpringDoc /v3/api-docs endpoint is enabled by default. To disable it in production, set the property \'springdoc.api-docs.enabled=false\'',
        'SpringDoc /v3/api-docs endpoint is enabled by default. To disable it in production, set the property \'springdoc.api-docs.enabled=false\'',
        NULL, NULL, '2026-05-23 18:52:40.953', '2026-05-23 18:52:40.959'),
       (43, 1779533746895001, NULL, NULL, 'WARN', 'org.springdoc.core.events.SpringDocAppInitializer', 'restartedMain',
        NULL, 'zwei-admin', NULL, NULL,
        'SpringDoc /v3/api-docs endpoint is enabled by default. To disable it in production, set the property \'springdoc.api-docs.enabled=false\'',
        'SpringDoc /v3/api-docs endpoint is enabled by default. To disable it in production, set the property \'springdoc.api-docs.enabled=false\'',
        NULL, NULL, '2026-05-23 18:55:53.106', '2026-05-23 18:55:53.258'),
       (44, 1779533746895003, NULL, NULL, 'ERROR',
        'org.apache.catalina.core.ContainerBase.[Tomcat].[localhost].[/].[dispatcherServlet]', 'http-nio-8080-exec-6',
        NULL, 'zwei-admin', NULL, NULL,
        'Servlet.service() for servlet [dispatcherServlet] in context with path [] threw exception',
        'Servlet.service() for servlet [dispatcherServlet] in context with path [] threw exception',
        'java.lang.IllegalStateException',
        'Failed to send [org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter$DataWithMediaType@4f53eb4f, org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter$DataWithMediaType@7a4737f, org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter$DataWithMediaType@6043b696]',
        '2026-05-23 18:56:18.230', '2026-05-23 18:56:18.237'),
       (45, 1779533746895004, NULL, NULL, 'ERROR',
        'org.apache.catalina.core.ContainerBase.[Tomcat].[localhost].[/].[dispatcherServlet]', 'http-nio-8080-exec-2',
        NULL, 'zwei-admin', NULL, NULL, 'Servlet.service() for servlet [dispatcherServlet] threw exception',
        'Servlet.service() for servlet [dispatcherServlet] threw exception',
        'org.springframework.security.authorization.AuthorizationDeniedException', 'Access Denied',
        '2026-05-23 18:56:18.234', '2026-05-23 18:56:18.252'),
       (46, 1779533746895006, NULL, NULL, 'ERROR',
        'org.apache.catalina.core.ContainerBase.[Tomcat].[localhost].[/].[dispatcherServlet]', 'http-nio-8080-exec-2',
        NULL, 'zwei-admin', NULL, NULL,
        'Servlet.service() for servlet [dispatcherServlet] in context with path [] threw exception [Unable to handle the Spring Security Exception because the response is already committed.] with root cause',
        'Servlet.service() for servlet [dispatcherServlet] in context with path [] threw exception [Unable to handle the Spring Security Exception because the response is already committed.] with root cause',
        'org.springframework.security.authorization.AuthorizationDeniedException', 'Access Denied',
        '2026-05-23 18:56:18.260', '2026-05-23 18:56:18.324'),
       (47, 1779533746895008, NULL, NULL, 'WARN', 'net.dreamlu.mica.net.server.task.ServerHeartbeatTask',
        'DefaultTimerTaskService', NULL, 'zwei-admin', NULL, NULL,
        '\r\nMica-Mqtt-Server\r\n ├ 当前时间 :2026-05-23T18:57:52.936516700\r\n ├ 连接统计\r\n │ 	 ├ 共接受过连接数 :0\r\n │ 	 ├ 当前连接数 :0\r\n │ 	 └ 关闭过的连接数 :0\r\n ├ 消息统计\r\n │ 	 ├ 已处理消息 :0\r\n │ 	 ├ 已接收消息(packet/byte) :0/0\r\n │ 	 ├ 已发送消息(packet/byte) :0/0b\r\n │ 	 ├ 平均每次TCP包接收的字节数 :0.0\r\n │ 	 └ 平均每次TCP包接收的业务包 :0.0\r\n ├ 节点统计\r\n │ 	 ├ clientNodes :0\r\n │ 	 ├ 所有连接 :0\r\n │ 	 ├ 绑定user数 :0\r\n │ 	 ├ 绑定token数 :0\r\n │ 	 └ 等待同步消息响应 :0\r\n ├ 队列统计\r\n │ 	 ├ 解码队列总数 :0\r\n │ 	 ├ 处理队列总数 :0\r\n │ 	 └ 发送队列总数 :0\r\n └ 群组\r\n   	 └ groupmap: 0',
        '\r\nMica-Mqtt-Server\r\n ├ 当前时间 :2026-05-23T18:57:52.936516700\r\n ├ 连接统计\r\n │ 	 ├ 共接受过连接数 :0\r\n │ 	 ├ 当前连接数 :0\r\n │ 	 └ 关闭过的连接数 :0\r\n ├ 消息统计\r\n │ 	 ├ 已处理消息 :0\r\n │ 	 ├ 已接收消息(packet/byte) :0/0\r\n │ 	 ├ 已发送消息(packet/byte) :0/0b\r\n │ 	 ├ 平均每次TCP包接收的字节数 :0.0\r\n │ 	 └ 平均每次TCP包接收的业务包 :0.0\r\n ├ 节点统计\r\n │ 	 ├ clientNodes :0\r\n │ 	 ├ 所有连接 :0\r\n │ 	 ├ 绑定user数 :0\r\n │ 	 ├ 绑定token数 :0\r\n │ 	 └ 等待同步消息响应 :0\r\n ├ 队列统计\r\n │ 	 ├ 解码队列总数 :0\r\n │ 	 ├ 处理队列总数 :0\r\n │ 	 └ 发送队列总数 :0\r\n └ 群组\r\n   	 └ groupmap: 0',
        NULL, NULL, '2026-05-23 18:57:52.936', '2026-05-23 18:57:52.940'),
       (48, 1779533746895009, NULL, NULL, 'WARN', 'net.dreamlu.mica.net.server.task.ServerHeartbeatTask',
        'DefaultTimerTaskService', NULL, 'zwei-admin', NULL, NULL,
        'Mica-Mqtt-Server, 检查心跳, 共0个连接, 取锁耗时0ms, 循环耗时8ms, 心跳检测周期:120000ms, 心跳超时时间:90000ms',
        'Mica-Mqtt-Server, 检查心跳, 共0个连接, 取锁耗时0ms, 循环耗时8ms, 心跳检测周期:120000ms, 心跳超时时间:90000ms',
        NULL, NULL, '2026-05-23 18:57:52.944', '2026-05-23 18:57:52.947'),
       (49, 1779533746895010, NULL, NULL, 'ERROR', 'net.dreamlu.mica.net.utils.thread.pool.TioCallerRunsPolicy',
        'Thread-15', NULL, 'zwei-admin', NULL, NULL, 'sun.nio.ch.AsynchronousChannelGroupImpl$1',
        'sun.nio.ch.AsynchronousChannelGroupImpl$1', NULL, NULL, '2026-05-23 18:58:27.954', '2026-05-23 18:58:27.959'),
       (50, 1779533746895011, NULL, NULL, 'ERROR', 'net.dreamlu.mica.net.utils.thread.pool.TioCallerRunsPolicy',
        'Thread-16', NULL, 'zwei-admin', NULL, NULL, 'sun.nio.ch.AsynchronousChannelGroupImpl$1',
        'sun.nio.ch.AsynchronousChannelGroupImpl$1', NULL, NULL, '2026-05-23 18:58:27.965', '2026-05-23 18:58:27.967'),
       (51, 1779533746895012, NULL, NULL, 'ERROR', 'net.dreamlu.mica.net.utils.thread.pool.TioCallerRunsPolicy',
        'Thread-17', NULL, 'zwei-admin', NULL, NULL, 'sun.nio.ch.AsynchronousChannelGroupImpl$1',
        'sun.nio.ch.AsynchronousChannelGroupImpl$1', NULL, NULL, '2026-05-23 18:58:27.970', '2026-05-23 18:58:27.973'),
       (52, 921779534004781001, NULL, NULL, 'WARN', 'org.springdoc.core.events.SpringDocAppInitializer',
        'restartedMain', NULL, 'zwei-admin', NULL, NULL,
        'SpringDoc /v3/api-docs endpoint is enabled by default. To disable it in production, set the property \'springdoc.api-docs.enabled=false\'',
        'SpringDoc /v3/api-docs endpoint is enabled by default. To disable it in production, set the property \'springdoc.api-docs.enabled=false\'',
        NULL, NULL, '2026-05-23 19:00:11.262', '2026-05-23 19:00:11.417'),
       (53, 921779534004781004, NULL, NULL, 'ERROR',
        'org.apache.catalina.core.ContainerBase.[Tomcat].[localhost].[/].[dispatcherServlet]', 'http-nio-8080-exec-6',
        NULL, 'zwei-admin', NULL, NULL, 'Servlet.service() for servlet [dispatcherServlet] threw exception',
        'Servlet.service() for servlet [dispatcherServlet] threw exception',
        'org.springframework.security.authorization.AuthorizationDeniedException', 'Access Denied',
        '2026-05-23 19:00:59.350', '2026-05-23 19:00:59.358'),
       (54, 921779534004781005, NULL, NULL, 'ERROR',
        'org.apache.catalina.core.ContainerBase.[Tomcat].[localhost].[/].[dispatcherServlet]', 'http-nio-8080-exec-6',
        NULL, 'zwei-admin', NULL, NULL,
        'Servlet.service() for servlet [dispatcherServlet] in context with path [] threw exception [Unable to handle the Spring Security Exception because the response is already committed.] with root cause',
        'Servlet.service() for servlet [dispatcherServlet] in context with path [] threw exception [Unable to handle the Spring Security Exception because the response is already committed.] with root cause',
        'org.springframework.security.authorization.AuthorizationDeniedException', 'Access Denied',
        '2026-05-23 19:00:59.363', '2026-05-23 19:00:59.367'),
       (55, 921779534004781007, NULL, NULL, 'ERROR',
        'org.apache.catalina.core.ContainerBase.[Tomcat].[localhost].[/].[dispatcherServlet]', 'http-nio-8080-exec-9',
        NULL, 'zwei-admin', NULL, NULL, 'Servlet.service() for servlet [dispatcherServlet] threw exception',
        'Servlet.service() for servlet [dispatcherServlet] threw exception',
        'org.springframework.security.authorization.AuthorizationDeniedException', 'Access Denied',
        '2026-05-23 19:01:29.481', '2026-05-23 19:01:29.490'),
       (56, 921779534004781008, NULL, NULL, 'ERROR',
        'org.apache.catalina.core.ContainerBase.[Tomcat].[localhost].[/].[dispatcherServlet]', 'http-nio-8080-exec-20',
        NULL, 'zwei-admin', NULL, NULL,
        'Servlet.service() for servlet [dispatcherServlet] in context with path [] threw exception',
        'Servlet.service() for servlet [dispatcherServlet] in context with path [] threw exception',
        'java.lang.IllegalStateException',
        'Failed to send [org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter$DataWithMediaType@326b6541, org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter$DataWithMediaType@7769ed68, org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter$DataWithMediaType@57ab8732]',
        '2026-05-23 19:01:29.481', '2026-05-23 19:01:29.499'),
       (57, 921779534004781009, NULL, NULL, 'ERROR',
        'org.apache.catalina.core.ContainerBase.[Tomcat].[localhost].[/].[dispatcherServlet]', 'http-nio-8080-exec-9',
        NULL, 'zwei-admin', NULL, NULL,
        'Servlet.service() for servlet [dispatcherServlet] in context with path [] threw exception [Unable to handle the Spring Security Exception because the response is already committed.] with root cause',
        'Servlet.service() for servlet [dispatcherServlet] in context with path [] threw exception [Unable to handle the Spring Security Exception because the response is already committed.] with root cause',
        'org.springframework.security.authorization.AuthorizationDeniedException', 'Access Denied',
        '2026-05-23 19:01:29.493', '2026-05-23 19:01:29.507'),
       (58, 921779534004781011, NULL, NULL, 'WARN', 'net.dreamlu.mica.net.server.task.ServerHeartbeatTask',
        'DefaultTimerTaskService', NULL, 'zwei-admin', NULL, NULL,
        '\r\nMica-Mqtt-Server\r\n ├ 当前时间 :2026-05-23T19:02:11.136788800\r\n ├ 连接统计\r\n │ 	 ├ 共接受过连接数 :0\r\n │ 	 ├ 当前连接数 :0\r\n │ 	 └ 关闭过的连接数 :0\r\n ├ 消息统计\r\n │ 	 ├ 已处理消息 :0\r\n │ 	 ├ 已接收消息(packet/byte) :0/0\r\n │ 	 ├ 已发送消息(packet/byte) :0/0b\r\n │ 	 ├ 平均每次TCP包接收的字节数 :0.0\r\n │ 	 └ 平均每次TCP包接收的业务包 :0.0\r\n ├ 节点统计\r\n │ 	 ├ clientNodes :0\r\n │ 	 ├ 所有连接 :0\r\n │ 	 ├ 绑定user数 :0\r\n │ 	 ├ 绑定token数 :0\r\n │ 	 └ 等待同步消息响应 :0\r\n ├ 队列统计\r\n │ 	 ├ 解码队列总数 :0\r\n │ 	 ├ 处理队列总数 :0\r\n │ 	 └ 发送队列总数 :0\r\n └ 群组\r\n   	 └ groupmap: 0',
        '\r\nMica-Mqtt-Server\r\n ├ 当前时间 :2026-05-23T19:02:11.136788800\r\n ├ 连接统计\r\n │ 	 ├ 共接受过连接数 :0\r\n │ 	 ├ 当前连接数 :0\r\n │ 	 └ 关闭过的连接数 :0\r\n ├ 消息统计\r\n │ 	 ├ 已处理消息 :0\r\n │ 	 ├ 已接收消息(packet/byte) :0/0\r\n │ 	 ├ 已发送消息(packet/byte) :0/0b\r\n │ 	 ├ 平均每次TCP包接收的字节数 :0.0\r\n │ 	 └ 平均每次TCP包接收的业务包 :0.0\r\n ├ 节点统计\r\n │ 	 ├ clientNodes :0\r\n │ 	 ├ 所有连接 :0\r\n │ 	 ├ 绑定user数 :0\r\n │ 	 ├ 绑定token数 :0\r\n │ 	 └ 等待同步消息响应 :0\r\n ├ 队列统计\r\n │ 	 ├ 解码队列总数 :0\r\n │ 	 ├ 处理队列总数 :0\r\n │ 	 └ 发送队列总数 :0\r\n └ 群组\r\n   	 └ groupmap: 0',
        NULL, NULL, '2026-05-23 19:02:11.136', '2026-05-23 19:02:11.141'),
       (59, 921779534004781012, NULL, NULL, 'WARN', 'net.dreamlu.mica.net.server.task.ServerHeartbeatTask',
        'DefaultTimerTaskService', NULL, 'zwei-admin', NULL, NULL,
        'Mica-Mqtt-Server, 检查心跳, 共0个连接, 取锁耗时0ms, 循环耗时10ms, 心跳检测周期:120000ms, 心跳超时时间:90000ms',
        'Mica-Mqtt-Server, 检查心跳, 共0个连接, 取锁耗时0ms, 循环耗时10ms, 心跳检测周期:120000ms, 心跳超时时间:90000ms',
        NULL, NULL, '2026-05-23 19:02:11.146', '2026-05-23 19:02:11.149'),
       (60, 921779534004781013, NULL, NULL, 'WARN', 'net.dreamlu.mica.net.server.task.ServerHeartbeatTask',
        'DefaultTimerTaskService', NULL, 'zwei-admin', NULL, NULL,
        '\r\nMica-Mqtt-Server\r\n ├ 当前时间 :2026-05-23T19:04:11.137760500\r\n ├ 连接统计\r\n │ 	 ├ 共接受过连接数 :0\r\n │ 	 ├ 当前连接数 :0\r\n │ 	 └ 关闭过的连接数 :0\r\n ├ 消息统计\r\n │ 	 ├ 已处理消息 :0\r\n │ 	 ├ 已接收消息(packet/byte) :0/0\r\n │ 	 ├ 已发送消息(packet/byte) :0/0b\r\n │ 	 ├ 平均每次TCP包接收的字节数 :0.0\r\n │ 	 └ 平均每次TCP包接收的业务包 :0.0\r\n ├ 节点统计\r\n │ 	 ├ clientNodes :0\r\n │ 	 ├ 所有连接 :0\r\n │ 	 ├ 绑定user数 :0\r\n │ 	 ├ 绑定token数 :0\r\n │ 	 └ 等待同步消息响应 :0\r\n ├ 队列统计\r\n │ 	 ├ 解码队列总数 :0\r\n │ 	 ├ 处理队列总数 :0\r\n │ 	 └ 发送队列总数 :0\r\n └ 群组\r\n   	 └ groupmap: 0',
        '\r\nMica-Mqtt-Server\r\n ├ 当前时间 :2026-05-23T19:04:11.137760500\r\n ├ 连接统计\r\n │ 	 ├ 共接受过连接数 :0\r\n │ 	 ├ 当前连接数 :0\r\n │ 	 └ 关闭过的连接数 :0\r\n ├ 消息统计\r\n │ 	 ├ 已处理消息 :0\r\n │ 	 ├ 已接收消息(packet/byte) :0/0\r\n │ 	 ├ 已发送消息(packet/byte) :0/0b\r\n │ 	 ├ 平均每次TCP包接收的字节数 :0.0\r\n │ 	 └ 平均每次TCP包接收的业务包 :0.0\r\n ├ 节点统计\r\n │ 	 ├ clientNodes :0\r\n │ 	 ├ 所有连接 :0\r\n │ 	 ├ 绑定user数 :0\r\n │ 	 ├ 绑定token数 :0\r\n │ 	 └ 等待同步消息响应 :0\r\n ├ 队列统计\r\n │ 	 ├ 解码队列总数 :0\r\n │ 	 ├ 处理队列总数 :0\r\n │ 	 └ 发送队列总数 :0\r\n └ 群组\r\n   	 └ groupmap: 0',
        NULL, NULL, '2026-05-23 19:04:11.137', '2026-05-23 19:04:11.149'),
       (61, 921779534004781014, NULL, NULL, 'WARN', 'net.dreamlu.mica.net.server.task.ServerHeartbeatTask',
        'DefaultTimerTaskService', NULL, 'zwei-admin', NULL, NULL,
        'Mica-Mqtt-Server, 检查心跳, 共0个连接, 取锁耗时0ms, 循环耗时21ms, 心跳检测周期:120000ms, 心跳超时时间:90000ms',
        'Mica-Mqtt-Server, 检查心跳, 共0个连接, 取锁耗时0ms, 循环耗时21ms, 心跳检测周期:120000ms, 心跳超时时间:90000ms',
        NULL, NULL, '2026-05-23 19:04:11.158', '2026-05-23 19:04:11.160'),
       (62, 921779534004781015, NULL, NULL, 'WARN', 'net.dreamlu.mica.net.server.task.ServerHeartbeatTask',
        'DefaultTimerTaskService', NULL, 'zwei-admin', NULL, NULL,
        '\r\nMica-Mqtt-Server\r\n ├ 当前时间 :2026-05-23T19:06:11.136731600\r\n ├ 连接统计\r\n │ 	 ├ 共接受过连接数 :0\r\n │ 	 ├ 当前连接数 :0\r\n │ 	 └ 关闭过的连接数 :0\r\n ├ 消息统计\r\n │ 	 ├ 已处理消息 :0\r\n │ 	 ├ 已接收消息(packet/byte) :0/0\r\n │ 	 ├ 已发送消息(packet/byte) :0/0b\r\n │ 	 ├ 平均每次TCP包接收的字节数 :0.0\r\n │ 	 └ 平均每次TCP包接收的业务包 :0.0\r\n ├ 节点统计\r\n │ 	 ├ clientNodes :0\r\n │ 	 ├ 所有连接 :0\r\n │ 	 ├ 绑定user数 :0\r\n │ 	 ├ 绑定token数 :0\r\n │ 	 └ 等待同步消息响应 :0\r\n ├ 队列统计\r\n │ 	 ├ 解码队列总数 :0\r\n │ 	 ├ 处理队列总数 :0\r\n │ 	 └ 发送队列总数 :0\r\n └ 群组\r\n   	 └ groupmap: 0',
        '\r\nMica-Mqtt-Server\r\n ├ 当前时间 :2026-05-23T19:06:11.136731600\r\n ├ 连接统计\r\n │ 	 ├ 共接受过连接数 :0\r\n │ 	 ├ 当前连接数 :0\r\n │ 	 └ 关闭过的连接数 :0\r\n ├ 消息统计\r\n │ 	 ├ 已处理消息 :0\r\n │ 	 ├ 已接收消息(packet/byte) :0/0\r\n │ 	 ├ 已发送消息(packet/byte) :0/0b\r\n │ 	 ├ 平均每次TCP包接收的字节数 :0.0\r\n │ 	 └ 平均每次TCP包接收的业务包 :0.0\r\n ├ 节点统计\r\n │ 	 ├ clientNodes :0\r\n │ 	 ├ 所有连接 :0\r\n │ 	 ├ 绑定user数 :0\r\n │ 	 ├ 绑定token数 :0\r\n │ 	 └ 等待同步消息响应 :0\r\n ├ 队列统计\r\n │ 	 ├ 解码队列总数 :0\r\n │ 	 ├ 处理队列总数 :0\r\n │ 	 └ 发送队列总数 :0\r\n └ 群组\r\n   	 └ groupmap: 0',
        NULL, NULL, '2026-05-23 19:06:11.136', '2026-05-23 19:06:11.139'),
       (63, 921779534004781016, NULL, NULL, 'WARN', 'net.dreamlu.mica.net.server.task.ServerHeartbeatTask',
        'DefaultTimerTaskService', NULL, 'zwei-admin', NULL, NULL,
        'Mica-Mqtt-Server, 检查心跳, 共0个连接, 取锁耗时0ms, 循环耗时11ms, 心跳检测周期:120000ms, 心跳超时时间:90000ms',
        'Mica-Mqtt-Server, 检查心跳, 共0个连接, 取锁耗时0ms, 循环耗时11ms, 心跳检测周期:120000ms, 心跳超时时间:90000ms',
        NULL, NULL, '2026-05-23 19:06:11.147', '2026-05-23 19:06:11.153'),
       (64, 921779534004781017, NULL, NULL, 'ERROR',
        'org.apache.catalina.core.ContainerBase.[Tomcat].[localhost].[/].[dispatcherServlet]', 'http-nio-8080-exec-14',
        NULL, 'zwei-admin', NULL, NULL, 'Servlet.service() for servlet [dispatcherServlet] threw exception',
        'Servlet.service() for servlet [dispatcherServlet] threw exception',
        'org.springframework.security.authorization.AuthorizationDeniedException', 'Access Denied',
        '2026-05-23 19:06:34.243', '2026-05-23 19:06:34.249'),
       (65, 921779534004781018, NULL, NULL, 'ERROR',
        'org.apache.catalina.core.ContainerBase.[Tomcat].[localhost].[/].[dispatcherServlet]', 'http-nio-8080-exec-14',
        NULL, 'zwei-admin', NULL, NULL,
        'Servlet.service() for servlet [dispatcherServlet] in context with path [] threw exception [Unable to handle the Spring Security Exception because the response is already committed.] with root cause',
        'Servlet.service() for servlet [dispatcherServlet] in context with path [] threw exception [Unable to handle the Spring Security Exception because the response is already committed.] with root cause',
        'org.springframework.security.authorization.AuthorizationDeniedException', 'Access Denied',
        '2026-05-23 19:06:34.256', '2026-05-23 19:06:34.259'),
       (66, 921779534004781019, NULL, NULL, 'ERROR',
        'org.apache.catalina.core.ContainerBase.[Tomcat].[localhost].[/].[dispatcherServlet]', 'http-nio-8080-exec-14',
        NULL, 'zwei-admin', NULL, NULL, 'Servlet.service() for servlet [dispatcherServlet] threw exception',
        'Servlet.service() for servlet [dispatcherServlet] threw exception',
        'org.springframework.security.authorization.AuthorizationDeniedException', 'Access Denied',
        '2026-05-23 19:06:34.263', '2026-05-23 19:06:34.265'),
       (67, 921779534004781020, NULL, NULL, 'ERROR', 'org.apache.catalina.core.ContainerBase.[Tomcat].[localhost]',
        'http-nio-8080-exec-14', NULL, 'zwei-admin', NULL, NULL,
        'Exception Processing [ErrorPage[errorCode=0, location=/error]]',
        'Exception Processing [ErrorPage[errorCode=0, location=/error]]', 'jakarta.servlet.ServletException',
        'Unable to handle the Spring Security Exception because the response is already committed.',
        '2026-05-23 19:06:34.267', '2026-05-23 19:06:34.273'),
       (68, 921779534004781021, NULL, NULL, 'WARN', 'net.dreamlu.mica.net.server.task.ServerHeartbeatTask',
        'DefaultTimerTaskService', NULL, 'zwei-admin', NULL, NULL,
        '\r\nMica-Mqtt-Server\r\n ├ 当前时间 :2026-05-23T19:08:11.137824300\r\n ├ 连接统计\r\n │ 	 ├ 共接受过连接数 :0\r\n │ 	 ├ 当前连接数 :0\r\n │ 	 └ 关闭过的连接数 :0\r\n ├ 消息统计\r\n │ 	 ├ 已处理消息 :0\r\n │ 	 ├ 已接收消息(packet/byte) :0/0\r\n │ 	 ├ 已发送消息(packet/byte) :0/0b\r\n │ 	 ├ 平均每次TCP包接收的字节数 :0.0\r\n │ 	 └ 平均每次TCP包接收的业务包 :0.0\r\n ├ 节点统计\r\n │ 	 ├ clientNodes :0\r\n │ 	 ├ 所有连接 :0\r\n │ 	 ├ 绑定user数 :0\r\n │ 	 ├ 绑定token数 :0\r\n │ 	 └ 等待同步消息响应 :0\r\n ├ 队列统计\r\n │ 	 ├ 解码队列总数 :0\r\n │ 	 ├ 处理队列总数 :0\r\n │ 	 └ 发送队列总数 :0\r\n └ 群组\r\n   	 └ groupmap: 0',
        '\r\nMica-Mqtt-Server\r\n ├ 当前时间 :2026-05-23T19:08:11.137824300\r\n ├ 连接统计\r\n │ 	 ├ 共接受过连接数 :0\r\n │ 	 ├ 当前连接数 :0\r\n │ 	 └ 关闭过的连接数 :0\r\n ├ 消息统计\r\n │ 	 ├ 已处理消息 :0\r\n │ 	 ├ 已接收消息(packet/byte) :0/0\r\n │ 	 ├ 已发送消息(packet/byte) :0/0b\r\n │ 	 ├ 平均每次TCP包接收的字节数 :0.0\r\n │ 	 └ 平均每次TCP包接收的业务包 :0.0\r\n ├ 节点统计\r\n │ 	 ├ clientNodes :0\r\n │ 	 ├ 所有连接 :0\r\n │ 	 ├ 绑定user数 :0\r\n │ 	 ├ 绑定token数 :0\r\n │ 	 └ 等待同步消息响应 :0\r\n ├ 队列统计\r\n │ 	 ├ 解码队列总数 :0\r\n │ 	 ├ 处理队列总数 :0\r\n │ 	 └ 发送队列总数 :0\r\n └ 群组\r\n   	 └ groupmap: 0',
        NULL, NULL, '2026-05-23 19:08:11.137', '2026-05-23 19:08:11.146'),
       (69, 921779534004781022, NULL, NULL, 'WARN', 'net.dreamlu.mica.net.server.task.ServerHeartbeatTask',
        'DefaultTimerTaskService', NULL, 'zwei-admin', NULL, NULL,
        'Mica-Mqtt-Server, 检查心跳, 共0个连接, 取锁耗时0ms, 循环耗时16ms, 心跳检测周期:120000ms, 心跳超时时间:90000ms',
        'Mica-Mqtt-Server, 检查心跳, 共0个连接, 取锁耗时0ms, 循环耗时16ms, 心跳检测周期:120000ms, 心跳超时时间:90000ms',
        NULL, NULL, '2026-05-23 19:08:11.153', '2026-05-23 19:08:11.156'),
       (70, 921779534004781023, NULL, NULL, 'WARN', 'net.dreamlu.mica.net.server.task.ServerHeartbeatTask',
        'DefaultTimerTaskService', NULL, 'zwei-admin', NULL, NULL,
        '\r\nMica-Mqtt-Server\r\n ├ 当前时间 :2026-05-23T19:10:11.141761500\r\n ├ 连接统计\r\n │ 	 ├ 共接受过连接数 :0\r\n │ 	 ├ 当前连接数 :0\r\n │ 	 └ 关闭过的连接数 :0\r\n ├ 消息统计\r\n │ 	 ├ 已处理消息 :0\r\n │ 	 ├ 已接收消息(packet/byte) :0/0\r\n │ 	 ├ 已发送消息(packet/byte) :0/0b\r\n │ 	 ├ 平均每次TCP包接收的字节数 :0.0\r\n │ 	 └ 平均每次TCP包接收的业务包 :0.0\r\n ├ 节点统计\r\n │ 	 ├ clientNodes :0\r\n │ 	 ├ 所有连接 :0\r\n │ 	 ├ 绑定user数 :0\r\n │ 	 ├ 绑定token数 :0\r\n │ 	 └ 等待同步消息响应 :0\r\n ├ 队列统计\r\n │ 	 ├ 解码队列总数 :0\r\n │ 	 ├ 处理队列总数 :0\r\n │ 	 └ 发送队列总数 :0\r\n └ 群组\r\n   	 └ groupmap: 0',
        '\r\nMica-Mqtt-Server\r\n ├ 当前时间 :2026-05-23T19:10:11.141761500\r\n ├ 连接统计\r\n │ 	 ├ 共接受过连接数 :0\r\n │ 	 ├ 当前连接数 :0\r\n │ 	 └ 关闭过的连接数 :0\r\n ├ 消息统计\r\n │ 	 ├ 已处理消息 :0\r\n │ 	 ├ 已接收消息(packet/byte) :0/0\r\n │ 	 ├ 已发送消息(packet/byte) :0/0b\r\n │ 	 ├ 平均每次TCP包接收的字节数 :0.0\r\n │ 	 └ 平均每次TCP包接收的业务包 :0.0\r\n ├ 节点统计\r\n │ 	 ├ clientNodes :0\r\n │ 	 ├ 所有连接 :0\r\n │ 	 ├ 绑定user数 :0\r\n │ 	 ├ 绑定token数 :0\r\n │ 	 └ 等待同步消息响应 :0\r\n ├ 队列统计\r\n │ 	 ├ 解码队列总数 :0\r\n │ 	 ├ 处理队列总数 :0\r\n │ 	 └ 发送队列总数 :0\r\n └ 群组\r\n   	 └ groupmap: 0',
        NULL, NULL, '2026-05-23 19:10:11.141', '2026-05-23 19:10:11.145'),
       (71, 921779534004781024, NULL, NULL, 'WARN', 'net.dreamlu.mica.net.server.task.ServerHeartbeatTask',
        'DefaultTimerTaskService', NULL, 'zwei-admin', NULL, NULL,
        'Mica-Mqtt-Server, 检查心跳, 共0个连接, 取锁耗时0ms, 循环耗时8ms, 心跳检测周期:120000ms, 心跳超时时间:90000ms',
        'Mica-Mqtt-Server, 检查心跳, 共0个连接, 取锁耗时0ms, 循环耗时8ms, 心跳检测周期:120000ms, 心跳超时时间:90000ms',
        NULL, NULL, '2026-05-23 19:10:11.149', '2026-05-23 19:10:11.152'),
       (72, 921779534004781025, NULL, NULL, 'WARN', 'net.dreamlu.mica.net.server.task.ServerHeartbeatTask',
        'DefaultTimerTaskService', NULL, 'zwei-admin', NULL, NULL,
        '\r\nMica-Mqtt-Server\r\n ├ 当前时间 :2026-05-23T19:12:11.137108600\r\n ├ 连接统计\r\n │ 	 ├ 共接受过连接数 :0\r\n │ 	 ├ 当前连接数 :0\r\n │ 	 └ 关闭过的连接数 :0\r\n ├ 消息统计\r\n │ 	 ├ 已处理消息 :0\r\n │ 	 ├ 已接收消息(packet/byte) :0/0\r\n │ 	 ├ 已发送消息(packet/byte) :0/0b\r\n │ 	 ├ 平均每次TCP包接收的字节数 :0.0\r\n │ 	 └ 平均每次TCP包接收的业务包 :0.0\r\n ├ 节点统计\r\n │ 	 ├ clientNodes :0\r\n │ 	 ├ 所有连接 :0\r\n │ 	 ├ 绑定user数 :0\r\n │ 	 ├ 绑定token数 :0\r\n │ 	 └ 等待同步消息响应 :0\r\n ├ 队列统计\r\n │ 	 ├ 解码队列总数 :0\r\n │ 	 ├ 处理队列总数 :0\r\n │ 	 └ 发送队列总数 :0\r\n └ 群组\r\n   	 └ groupmap: 0',
        '\r\nMica-Mqtt-Server\r\n ├ 当前时间 :2026-05-23T19:12:11.137108600\r\n ├ 连接统计\r\n │ 	 ├ 共接受过连接数 :0\r\n │ 	 ├ 当前连接数 :0\r\n │ 	 └ 关闭过的连接数 :0\r\n ├ 消息统计\r\n │ 	 ├ 已处理消息 :0\r\n │ 	 ├ 已接收消息(packet/byte) :0/0\r\n │ 	 ├ 已发送消息(packet/byte) :0/0b\r\n │ 	 ├ 平均每次TCP包接收的字节数 :0.0\r\n │ 	 └ 平均每次TCP包接收的业务包 :0.0\r\n ├ 节点统计\r\n │ 	 ├ clientNodes :0\r\n │ 	 ├ 所有连接 :0\r\n │ 	 ├ 绑定user数 :0\r\n │ 	 ├ 绑定token数 :0\r\n │ 	 └ 等待同步消息响应 :0\r\n ├ 队列统计\r\n │ 	 ├ 解码队列总数 :0\r\n │ 	 ├ 处理队列总数 :0\r\n │ 	 └ 发送队列总数 :0\r\n └ 群组\r\n   	 └ groupmap: 0',
        NULL, NULL, '2026-05-23 19:12:11.137', '2026-05-23 19:12:11.143'),
       (73, 921779534004781026, NULL, NULL, 'WARN', 'net.dreamlu.mica.net.server.task.ServerHeartbeatTask',
        'DefaultTimerTaskService', NULL, 'zwei-admin', NULL, NULL,
        'Mica-Mqtt-Server, 检查心跳, 共0个连接, 取锁耗时0ms, 循环耗时15ms, 心跳检测周期:120000ms, 心跳超时时间:90000ms',
        'Mica-Mqtt-Server, 检查心跳, 共0个连接, 取锁耗时0ms, 循环耗时15ms, 心跳检测周期:120000ms, 心跳超时时间:90000ms',
        NULL, NULL, '2026-05-23 19:12:11.152', '2026-05-23 19:12:11.157'),
       (74, 921779534004781027, NULL, NULL, 'WARN', 'net.dreamlu.mica.net.server.task.ServerHeartbeatTask',
        'DefaultTimerTaskService', NULL, 'zwei-admin', NULL, NULL,
        '\r\nMica-Mqtt-Server\r\n ├ 当前时间 :2026-05-23T19:14:11.136978600\r\n ├ 连接统计\r\n │ 	 ├ 共接受过连接数 :0\r\n │ 	 ├ 当前连接数 :0\r\n │ 	 └ 关闭过的连接数 :0\r\n ├ 消息统计\r\n │ 	 ├ 已处理消息 :0\r\n │ 	 ├ 已接收消息(packet/byte) :0/0\r\n │ 	 ├ 已发送消息(packet/byte) :0/0b\r\n │ 	 ├ 平均每次TCP包接收的字节数 :0.0\r\n │ 	 └ 平均每次TCP包接收的业务包 :0.0\r\n ├ 节点统计\r\n │ 	 ├ clientNodes :0\r\n │ 	 ├ 所有连接 :0\r\n │ 	 ├ 绑定user数 :0\r\n │ 	 ├ 绑定token数 :0\r\n │ 	 └ 等待同步消息响应 :0\r\n ├ 队列统计\r\n │ 	 ├ 解码队列总数 :0\r\n │ 	 ├ 处理队列总数 :0\r\n │ 	 └ 发送队列总数 :0\r\n └ 群组\r\n   	 └ groupmap: 0',
        '\r\nMica-Mqtt-Server\r\n ├ 当前时间 :2026-05-23T19:14:11.136978600\r\n ├ 连接统计\r\n │ 	 ├ 共接受过连接数 :0\r\n │ 	 ├ 当前连接数 :0\r\n │ 	 └ 关闭过的连接数 :0\r\n ├ 消息统计\r\n │ 	 ├ 已处理消息 :0\r\n │ 	 ├ 已接收消息(packet/byte) :0/0\r\n │ 	 ├ 已发送消息(packet/byte) :0/0b\r\n │ 	 ├ 平均每次TCP包接收的字节数 :0.0\r\n │ 	 └ 平均每次TCP包接收的业务包 :0.0\r\n ├ 节点统计\r\n │ 	 ├ clientNodes :0\r\n │ 	 ├ 所有连接 :0\r\n │ 	 ├ 绑定user数 :0\r\n │ 	 ├ 绑定token数 :0\r\n │ 	 └ 等待同步消息响应 :0\r\n ├ 队列统计\r\n │ 	 ├ 解码队列总数 :0\r\n │ 	 ├ 处理队列总数 :0\r\n │ 	 └ 发送队列总数 :0\r\n └ 群组\r\n   	 └ groupmap: 0',
        NULL, NULL, '2026-05-23 19:14:11.136', '2026-05-23 19:14:11.143'),
       (75, 921779534004781028, NULL, NULL, 'WARN', 'net.dreamlu.mica.net.server.task.ServerHeartbeatTask',
        'DefaultTimerTaskService', NULL, 'zwei-admin', NULL, NULL,
        'Mica-Mqtt-Server, 检查心跳, 共0个连接, 取锁耗时0ms, 循环耗时16ms, 心跳检测周期:120000ms, 心跳超时时间:90000ms',
        'Mica-Mqtt-Server, 检查心跳, 共0个连接, 取锁耗时0ms, 循环耗时16ms, 心跳检测周期:120000ms, 心跳超时时间:90000ms',
        NULL, NULL, '2026-05-23 19:14:11.152', '2026-05-23 19:14:11.158'),
       (76, 921779534004781029, NULL, NULL, 'WARN', 'net.dreamlu.mica.net.server.task.ServerHeartbeatTask',
        'DefaultTimerTaskService', NULL, 'zwei-admin', NULL, NULL,
        '\r\nMica-Mqtt-Server\r\n ├ 当前时间 :2026-05-23T19:16:11.137607500\r\n ├ 连接统计\r\n │ 	 ├ 共接受过连接数 :0\r\n │ 	 ├ 当前连接数 :0\r\n │ 	 └ 关闭过的连接数 :0\r\n ├ 消息统计\r\n │ 	 ├ 已处理消息 :0\r\n │ 	 ├ 已接收消息(packet/byte) :0/0\r\n │ 	 ├ 已发送消息(packet/byte) :0/0b\r\n │ 	 ├ 平均每次TCP包接收的字节数 :0.0\r\n │ 	 └ 平均每次TCP包接收的业务包 :0.0\r\n ├ 节点统计\r\n │ 	 ├ clientNodes :0\r\n │ 	 ├ 所有连接 :0\r\n │ 	 ├ 绑定user数 :0\r\n │ 	 ├ 绑定token数 :0\r\n │ 	 └ 等待同步消息响应 :0\r\n ├ 队列统计\r\n │ 	 ├ 解码队列总数 :0\r\n │ 	 ├ 处理队列总数 :0\r\n │ 	 └ 发送队列总数 :0\r\n └ 群组\r\n   	 └ groupmap: 0',
        '\r\nMica-Mqtt-Server\r\n ├ 当前时间 :2026-05-23T19:16:11.137607500\r\n ├ 连接统计\r\n │ 	 ├ 共接受过连接数 :0\r\n │ 	 ├ 当前连接数 :0\r\n │ 	 └ 关闭过的连接数 :0\r\n ├ 消息统计\r\n │ 	 ├ 已处理消息 :0\r\n │ 	 ├ 已接收消息(packet/byte) :0/0\r\n │ 	 ├ 已发送消息(packet/byte) :0/0b\r\n │ 	 ├ 平均每次TCP包接收的字节数 :0.0\r\n │ 	 └ 平均每次TCP包接收的业务包 :0.0\r\n ├ 节点统计\r\n │ 	 ├ clientNodes :0\r\n │ 	 ├ 所有连接 :0\r\n │ 	 ├ 绑定user数 :0\r\n │ 	 ├ 绑定token数 :0\r\n │ 	 └ 等待同步消息响应 :0\r\n ├ 队列统计\r\n │ 	 ├ 解码队列总数 :0\r\n │ 	 ├ 处理队列总数 :0\r\n │ 	 └ 发送队列总数 :0\r\n └ 群组\r\n   	 └ groupmap: 0',
        NULL, NULL, '2026-05-23 19:16:11.137', '2026-05-23 19:16:11.140'),
       (77, 921779534004781030, NULL, NULL, 'WARN', 'net.dreamlu.mica.net.server.task.ServerHeartbeatTask',
        'DefaultTimerTaskService', NULL, 'zwei-admin', NULL, NULL,
        'Mica-Mqtt-Server, 检查心跳, 共0个连接, 取锁耗时0ms, 循环耗时16ms, 心跳检测周期:120000ms, 心跳超时时间:90000ms',
        'Mica-Mqtt-Server, 检查心跳, 共0个连接, 取锁耗时0ms, 循环耗时16ms, 心跳检测周期:120000ms, 心跳超时时间:90000ms',
        NULL, NULL, '2026-05-23 19:16:11.153', '2026-05-23 19:16:11.155'),
       (78, 921779534004781031, NULL, NULL, 'WARN', 'net.dreamlu.mica.net.server.task.ServerHeartbeatTask',
        'DefaultTimerTaskService', NULL, 'zwei-admin', NULL, NULL,
        '\r\nMica-Mqtt-Server\r\n ├ 当前时间 :2026-05-23T19:18:11.137576800\r\n ├ 连接统计\r\n │ 	 ├ 共接受过连接数 :0\r\n │ 	 ├ 当前连接数 :0\r\n │ 	 └ 关闭过的连接数 :0\r\n ├ 消息统计\r\n │ 	 ├ 已处理消息 :0\r\n │ 	 ├ 已接收消息(packet/byte) :0/0\r\n │ 	 ├ 已发送消息(packet/byte) :0/0b\r\n │ 	 ├ 平均每次TCP包接收的字节数 :0.0\r\n │ 	 └ 平均每次TCP包接收的业务包 :0.0\r\n ├ 节点统计\r\n │ 	 ├ clientNodes :0\r\n │ 	 ├ 所有连接 :0\r\n │ 	 ├ 绑定user数 :0\r\n │ 	 ├ 绑定token数 :0\r\n │ 	 └ 等待同步消息响应 :0\r\n ├ 队列统计\r\n │ 	 ├ 解码队列总数 :0\r\n │ 	 ├ 处理队列总数 :0\r\n │ 	 └ 发送队列总数 :0\r\n └ 群组\r\n   	 └ groupmap: 0',
        '\r\nMica-Mqtt-Server\r\n ├ 当前时间 :2026-05-23T19:18:11.137576800\r\n ├ 连接统计\r\n │ 	 ├ 共接受过连接数 :0\r\n │ 	 ├ 当前连接数 :0\r\n │ 	 └ 关闭过的连接数 :0\r\n ├ 消息统计\r\n │ 	 ├ 已处理消息 :0\r\n │ 	 ├ 已接收消息(packet/byte) :0/0\r\n │ 	 ├ 已发送消息(packet/byte) :0/0b\r\n │ 	 ├ 平均每次TCP包接收的字节数 :0.0\r\n │ 	 └ 平均每次TCP包接收的业务包 :0.0\r\n ├ 节点统计\r\n │ 	 ├ clientNodes :0\r\n │ 	 ├ 所有连接 :0\r\n │ 	 ├ 绑定user数 :0\r\n │ 	 ├ 绑定token数 :0\r\n │ 	 └ 等待同步消息响应 :0\r\n ├ 队列统计\r\n │ 	 ├ 解码队列总数 :0\r\n │ 	 ├ 处理队列总数 :0\r\n │ 	 └ 发送队列总数 :0\r\n └ 群组\r\n   	 └ groupmap: 0',
        NULL, NULL, '2026-05-23 19:18:11.137', '2026-05-23 19:18:11.144'),
       (79, 921779534004781032, NULL, NULL, 'WARN', 'net.dreamlu.mica.net.server.task.ServerHeartbeatTask',
        'DefaultTimerTaskService', NULL, 'zwei-admin', NULL, NULL,
        'Mica-Mqtt-Server, 检查心跳, 共0个连接, 取锁耗时0ms, 循环耗时12ms, 心跳检测周期:120000ms, 心跳超时时间:90000ms',
        'Mica-Mqtt-Server, 检查心跳, 共0个连接, 取锁耗时0ms, 循环耗时12ms, 心跳检测周期:120000ms, 心跳超时时间:90000ms',
        NULL, NULL, '2026-05-23 19:18:11.149', '2026-05-23 19:18:11.152'),
       (80, 921779534004781033, NULL, NULL, 'WARN', 'net.dreamlu.mica.net.server.task.ServerHeartbeatTask',
        'DefaultTimerTaskService', NULL, 'zwei-admin', NULL, NULL,
        '\r\nMica-Mqtt-Server\r\n ├ 当前时间 :2026-05-23T19:20:11.136576900\r\n ├ 连接统计\r\n │ 	 ├ 共接受过连接数 :0\r\n │ 	 ├ 当前连接数 :0\r\n │ 	 └ 关闭过的连接数 :0\r\n ├ 消息统计\r\n │ 	 ├ 已处理消息 :0\r\n │ 	 ├ 已接收消息(packet/byte) :0/0\r\n │ 	 ├ 已发送消息(packet/byte) :0/0b\r\n │ 	 ├ 平均每次TCP包接收的字节数 :0.0\r\n │ 	 └ 平均每次TCP包接收的业务包 :0.0\r\n ├ 节点统计\r\n │ 	 ├ clientNodes :0\r\n │ 	 ├ 所有连接 :0\r\n │ 	 ├ 绑定user数 :0\r\n │ 	 ├ 绑定token数 :0\r\n │ 	 └ 等待同步消息响应 :0\r\n ├ 队列统计\r\n │ 	 ├ 解码队列总数 :0\r\n │ 	 ├ 处理队列总数 :0\r\n │ 	 └ 发送队列总数 :0\r\n └ 群组\r\n   	 └ groupmap: 0',
        '\r\nMica-Mqtt-Server\r\n ├ 当前时间 :2026-05-23T19:20:11.136576900\r\n ├ 连接统计\r\n │ 	 ├ 共接受过连接数 :0\r\n │ 	 ├ 当前连接数 :0\r\n │ 	 └ 关闭过的连接数 :0\r\n ├ 消息统计\r\n │ 	 ├ 已处理消息 :0\r\n │ 	 ├ 已接收消息(packet/byte) :0/0\r\n │ 	 ├ 已发送消息(packet/byte) :0/0b\r\n │ 	 ├ 平均每次TCP包接收的字节数 :0.0\r\n │ 	 └ 平均每次TCP包接收的业务包 :0.0\r\n ├ 节点统计\r\n │ 	 ├ clientNodes :0\r\n │ 	 ├ 所有连接 :0\r\n │ 	 ├ 绑定user数 :0\r\n │ 	 ├ 绑定token数 :0\r\n │ 	 └ 等待同步消息响应 :0\r\n ├ 队列统计\r\n │ 	 ├ 解码队列总数 :0\r\n │ 	 ├ 处理队列总数 :0\r\n │ 	 └ 发送队列总数 :0\r\n └ 群组\r\n   	 └ groupmap: 0',
        NULL, NULL, '2026-05-23 19:20:11.136', '2026-05-23 19:20:11.142'),
       (81, 921779534004781034, NULL, NULL, 'WARN', 'net.dreamlu.mica.net.server.task.ServerHeartbeatTask',
        'DefaultTimerTaskService', NULL, 'zwei-admin', NULL, NULL,
        'Mica-Mqtt-Server, 检查心跳, 共0个连接, 取锁耗时0ms, 循环耗时14ms, 心跳检测周期:120000ms, 心跳超时时间:90000ms',
        'Mica-Mqtt-Server, 检查心跳, 共0个连接, 取锁耗时0ms, 循环耗时14ms, 心跳检测周期:120000ms, 心跳超时时间:90000ms',
        NULL, NULL, '2026-05-23 19:20:11.150', '2026-05-23 19:20:11.154'),
       (82, 921779534004781035, NULL, NULL, 'WARN', 'net.dreamlu.mica.net.server.task.ServerHeartbeatTask',
        'DefaultTimerTaskService', NULL, 'zwei-admin', NULL, NULL,
        '\r\nMica-Mqtt-Server\r\n ├ 当前时间 :2026-05-23T19:22:11.137244900\r\n ├ 连接统计\r\n │ 	 ├ 共接受过连接数 :0\r\n │ 	 ├ 当前连接数 :0\r\n │ 	 └ 关闭过的连接数 :0\r\n ├ 消息统计\r\n │ 	 ├ 已处理消息 :0\r\n │ 	 ├ 已接收消息(packet/byte) :0/0\r\n │ 	 ├ 已发送消息(packet/byte) :0/0b\r\n │ 	 ├ 平均每次TCP包接收的字节数 :0.0\r\n │ 	 └ 平均每次TCP包接收的业务包 :0.0\r\n ├ 节点统计\r\n │ 	 ├ clientNodes :0\r\n │ 	 ├ 所有连接 :0\r\n │ 	 ├ 绑定user数 :0\r\n │ 	 ├ 绑定token数 :0\r\n │ 	 └ 等待同步消息响应 :0\r\n ├ 队列统计\r\n │ 	 ├ 解码队列总数 :0\r\n │ 	 ├ 处理队列总数 :0\r\n │ 	 └ 发送队列总数 :0\r\n └ 群组\r\n   	 └ groupmap: 0',
        '\r\nMica-Mqtt-Server\r\n ├ 当前时间 :2026-05-23T19:22:11.137244900\r\n ├ 连接统计\r\n │ 	 ├ 共接受过连接数 :0\r\n │ 	 ├ 当前连接数 :0\r\n │ 	 └ 关闭过的连接数 :0\r\n ├ 消息统计\r\n │ 	 ├ 已处理消息 :0\r\n │ 	 ├ 已接收消息(packet/byte) :0/0\r\n │ 	 ├ 已发送消息(packet/byte) :0/0b\r\n │ 	 ├ 平均每次TCP包接收的字节数 :0.0\r\n │ 	 └ 平均每次TCP包接收的业务包 :0.0\r\n ├ 节点统计\r\n │ 	 ├ clientNodes :0\r\n │ 	 ├ 所有连接 :0\r\n │ 	 ├ 绑定user数 :0\r\n │ 	 ├ 绑定token数 :0\r\n │ 	 └ 等待同步消息响应 :0\r\n ├ 队列统计\r\n │ 	 ├ 解码队列总数 :0\r\n │ 	 ├ 处理队列总数 :0\r\n │ 	 └ 发送队列总数 :0\r\n └ 群组\r\n   	 └ groupmap: 0',
        NULL, NULL, '2026-05-23 19:22:11.137', '2026-05-23 19:22:11.149'),
       (83, 921779534004781036, NULL, NULL, 'WARN', 'net.dreamlu.mica.net.server.task.ServerHeartbeatTask',
        'DefaultTimerTaskService', NULL, 'zwei-admin', NULL, NULL,
        'Mica-Mqtt-Server, 检查心跳, 共0个连接, 取锁耗时0ms, 循环耗时18ms, 心跳检测周期:120000ms, 心跳超时时间:90000ms',
        'Mica-Mqtt-Server, 检查心跳, 共0个连接, 取锁耗时0ms, 循环耗时18ms, 心跳检测周期:120000ms, 心跳超时时间:90000ms',
        NULL, NULL, '2026-05-23 19:22:11.155', '2026-05-23 19:22:11.159'),
       (84, 921779534004781037, NULL, NULL, 'WARN', 'net.dreamlu.mica.net.server.task.ServerHeartbeatTask',
        'DefaultTimerTaskService', NULL, 'zwei-admin', NULL, NULL,
        '\r\nMica-Mqtt-Server\r\n ├ 当前时间 :2026-05-23T19:24:11.136851100\r\n ├ 连接统计\r\n │ 	 ├ 共接受过连接数 :0\r\n │ 	 ├ 当前连接数 :0\r\n │ 	 └ 关闭过的连接数 :0\r\n ├ 消息统计\r\n │ 	 ├ 已处理消息 :0\r\n │ 	 ├ 已接收消息(packet/byte) :0/0\r\n │ 	 ├ 已发送消息(packet/byte) :0/0b\r\n │ 	 ├ 平均每次TCP包接收的字节数 :0.0\r\n │ 	 └ 平均每次TCP包接收的业务包 :0.0\r\n ├ 节点统计\r\n │ 	 ├ clientNodes :0\r\n │ 	 ├ 所有连接 :0\r\n │ 	 ├ 绑定user数 :0\r\n │ 	 ├ 绑定token数 :0\r\n │ 	 └ 等待同步消息响应 :0\r\n ├ 队列统计\r\n │ 	 ├ 解码队列总数 :0\r\n │ 	 ├ 处理队列总数 :0\r\n │ 	 └ 发送队列总数 :0\r\n └ 群组\r\n   	 └ groupmap: 0',
        '\r\nMica-Mqtt-Server\r\n ├ 当前时间 :2026-05-23T19:24:11.136851100\r\n ├ 连接统计\r\n │ 	 ├ 共接受过连接数 :0\r\n │ 	 ├ 当前连接数 :0\r\n │ 	 └ 关闭过的连接数 :0\r\n ├ 消息统计\r\n │ 	 ├ 已处理消息 :0\r\n │ 	 ├ 已接收消息(packet/byte) :0/0\r\n │ 	 ├ 已发送消息(packet/byte) :0/0b\r\n │ 	 ├ 平均每次TCP包接收的字节数 :0.0\r\n │ 	 └ 平均每次TCP包接收的业务包 :0.0\r\n ├ 节点统计\r\n │ 	 ├ clientNodes :0\r\n │ 	 ├ 所有连接 :0\r\n │ 	 ├ 绑定user数 :0\r\n │ 	 ├ 绑定token数 :0\r\n │ 	 └ 等待同步消息响应 :0\r\n ├ 队列统计\r\n │ 	 ├ 解码队列总数 :0\r\n │ 	 ├ 处理队列总数 :0\r\n │ 	 └ 发送队列总数 :0\r\n └ 群组\r\n   	 └ groupmap: 0',
        NULL, NULL, '2026-05-23 19:24:11.136', '2026-05-23 19:24:11.140'),
       (85, 921779534004781038, NULL, NULL, 'WARN', 'net.dreamlu.mica.net.server.task.ServerHeartbeatTask',
        'DefaultTimerTaskService', NULL, 'zwei-admin', NULL, NULL,
        'Mica-Mqtt-Server, 检查心跳, 共0个连接, 取锁耗时0ms, 循环耗时8ms, 心跳检测周期:120000ms, 心跳超时时间:90000ms',
        'Mica-Mqtt-Server, 检查心跳, 共0个连接, 取锁耗时0ms, 循环耗时8ms, 心跳检测周期:120000ms, 心跳超时时间:90000ms',
        NULL, NULL, '2026-05-23 19:24:11.144', '2026-05-23 19:24:11.146');
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
INSERT INTO `log_stream_checkpoint`
VALUES (1, 'resume-test', 910000000000000149, 'AUTH', '2026-05-23 18:57:00.773'),
       (52, 'resume-test-3', 921779534004781003, 'AUTH', '2026-05-23 19:01:11.768'),
       (54, 'lastid-test', 921779534004781010, 'AUTH', '2026-05-23 19:01:33.502'),
       (57, 'web-log-console', 921779622864396022, 'OPERATION', '2026-05-24 19:45:04.368'),
       (66, 'web-log-console', 921779622864396001, 'AUTH', '2026-05-24 19:43:18.870');
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
    `unit`            varchar(50)  DEFAULT NULL COMMENT '单位',
    `indicator_type`  varchar(50)  DEFAULT NULL COMMENT '指标类型',
    `icon`            varchar(200) DEFAULT NULL COMMENT '图标路径',
    `range_min`       decimal(12, 2) DEFAULT NULL COMMENT '最小值范围',
    `range_max`       decimal(12, 2) DEFAULT NULL COMMENT '最大值范围',
    `sort_order`      int          DEFAULT '0' COMMENT '排序号',
    `create_by`       varchar(64)  DEFAULT NULL COMMENT '创建者',
    `create_time`     datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`       varchar(64)  DEFAULT NULL COMMENT '更新者',
    `update_time`     datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `del_flag`        tinyint      DEFAULT '0' COMMENT '删除标记: 0-正常, 1-删除',
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
    KEY `idx_sensor_attr_sensor_id` (`sensor_id`),
    KEY `idx_sensor_attr_attr_code` (`attr_code`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='传感器属性表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sensor_attribute`
--

LOCK TABLES `sensor_attribute` WRITE;
/*!40000 ALTER TABLE `sensor_attribute`
    DISABLE KEYS */;
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
  AUTO_INCREMENT = 100
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
        '默认任意字符范围，0任意（密码可以输入任意字符），1数字（密码只能为0-9数字），2英文字母（密码只能为a-z和A-Z字母），3字母和数字（密码必须包含字母，数字）,4字母数字和特殊字符（目前支持的特殊字符包括：~!@#$%^&*()-=_+）');
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
  AUTO_INCREMENT = 2006
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
        '2026-05-24 18:00:00', '', NULL, '');
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
       (2, 1006);
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
        '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '127.0.0.1', '2026-05-24 19:41:29',
        '2026-05-08 22:05:52', 'admin', '2026-05-08 22:05:52', '', NULL, '管理员'),
       (2, 105, 'ry', '若依', '00', 'ry@qq.com', '15666666666', '1', '',
        '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '127.0.0.1', '2026-05-08 22:05:52',
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
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='视频设备表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `video_device`
--

LOCK TABLES `video_device` WRITE;
/*!40000 ALTER TABLE `video_device`
    DISABLE KEYS */;
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
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_video_device_hazard_point` (`video_device_id`, `hazard_point_id`),
    KEY `idx_video_device_hp_device_id` (`video_device_id`),
    KEY `idx_video_device_hp_hp_id` (`hazard_point_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='视频设备隐患点关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `video_device_hazard_point`
--

LOCK TABLES `video_device_hazard_point` WRITE;
/*!40000 ALTER TABLE `video_device_hazard_point`
    DISABLE KEYS */;
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

-- Dump completed on 2026-05-24 21:40:24
