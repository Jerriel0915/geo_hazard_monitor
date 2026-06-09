-- ============================================================
-- V2.1: 告警中心模块 — 数据库 Schema 重构
-- 说明: 旧表 alarm_criteria/alarm_record/alarm_notification/alarm_dispatch_rule
--       已有定义但无生产数据，采用 DROP + 重建策略。
-- 日期: 2026-06-09
-- ============================================================

-- ------------------------------------------------------------------
-- 1. 告警判据表（重构）
-- ------------------------------------------------------------------
DROP TABLE IF EXISTS `alarm_criteria`;
CREATE TABLE `alarm_criteria`
(
    `id`                   bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name`                 varchar(200) NOT NULL COMMENT '判据名称',
    `monitor_type_id`      bigint       DEFAULT NULL COMMENT '监测类型ID',
    `monitor_type_name`    varchar(200) DEFAULT NULL COMMENT '监测类型名称（冗余，查询提速）',
    `monitor_content_id`   bigint       DEFAULT NULL COMMENT '监测内容ID（精确到指标）',
    `monitor_content_code` varchar(100) DEFAULT NULL COMMENT '监测内容编码',
    `hazard_point_id`      bigint       DEFAULT NULL COMMENT '隐患点ID（NULL=监测类型下全局适用）',
    `conditions_json`      json         DEFAULT NULL COMMENT '判据条件列表 [{"indicator":"value","operator":"GT","threshold":10.5}]',
    `logic_operator`       varchar(10)  DEFAULT 'AND' COMMENT '多条件逻辑: AND / OR',
    `blue_expression`      varchar(500) DEFAULT NULL COMMENT '蓝色预警表达式或阈值',
    `blue_description`     varchar(500) DEFAULT NULL COMMENT '蓝色预警描述',
    `yellow_expression`    varchar(500) DEFAULT NULL COMMENT '黄色预警表达式或阈值',
    `yellow_description`   varchar(500) DEFAULT NULL COMMENT '黄色预警描述',
    `orange_expression`    varchar(500) DEFAULT NULL COMMENT '橙色预警表达式或阈值',
    `orange_description`   varchar(500) DEFAULT NULL COMMENT '橙色预警描述',
    `red_expression`       varchar(500) DEFAULT NULL COMMENT '红色预警表达式或阈值',
    `red_description`      varchar(500) DEFAULT NULL COMMENT '红色预警描述',
    `persist_count`        int          DEFAULT '1' COMMENT '持续触发N个数据周期后才生成告警（防误报）',
    `silence_period`       int          DEFAULT '0' COMMENT '静默周期（数据采集周期数），期内重复触发仅累加次数',
    `is_enabled`           tinyint      DEFAULT '1' COMMENT '是否启用: 0-禁用, 1-启用',
    `version`              int          DEFAULT '1' COMMENT '规则版本号（每次修改+1）',
    `create_by`            varchar(64)  DEFAULT NULL COMMENT '创建者',
    `create_time`          datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`            varchar(64)  DEFAULT NULL COMMENT '更新者',
    `update_time`          datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `del_flag`             tinyint      DEFAULT '0' COMMENT '删除标记: 0-正常, 1-删除',
    PRIMARY KEY (`id`),
    KEY `idx_criteria_type` (`monitor_type_id`),
    KEY `idx_criteria_content` (`monitor_content_id`),
    KEY `idx_criteria_hp` (`hazard_point_id`),
    KEY `idx_criteria_enabled` (`is_enabled`),
    KEY `idx_criteria_del` (`del_flag`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='告警判据表';

-- ------------------------------------------------------------------
-- 2. 判据变更日志（新增）
-- ------------------------------------------------------------------
DROP TABLE IF EXISTS `alarm_criteria_log`;
CREATE TABLE `alarm_criteria_log`
(
    `id`          bigint      NOT NULL AUTO_INCREMENT,
    `criteria_id` bigint      NOT NULL COMMENT '判据ID',
    `version`     int         NOT NULL COMMENT '变更后的版本号',
    `change_type` varchar(20) NOT NULL COMMENT 'CREATE / UPDATE / DELETE / TOGGLE',
    `old_value`   json        DEFAULT NULL COMMENT '变更前的值（JSON）',
    `new_value`   json        DEFAULT NULL COMMENT '变更后的值（JSON）',
    `create_by`   varchar(64) DEFAULT NULL,
    `create_time` datetime    DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_criteria_log_cid` (`criteria_id`, `version`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='告警判据变更日志';

-- ------------------------------------------------------------------
-- 3. 告警记录表（重构）
-- ------------------------------------------------------------------
DROP TABLE IF EXISTS `alarm_record`;
CREATE TABLE `alarm_record`
(
    `id`                 bigint      NOT NULL AUTO_INCREMENT,
    `hazard_point_id`    bigint      NOT NULL COMMENT '隐患点ID',
    `hazard_point_name`  varchar(200)         DEFAULT NULL COMMENT '隐患点名称',
    `device_id`          bigint               DEFAULT NULL COMMENT '触发设备ID',
    `sensor_id`          bigint               DEFAULT NULL COMMENT '触发传感器ID',
    `monitor_content_id` bigint               DEFAULT NULL COMMENT '触发监测内容ID',
    `alarm_level`        tinyint     NOT NULL COMMENT '告警等级: 1=蓝色 2=黄色 3=橙色 4=红色',
    `alarm_level_text`   varchar(50)          DEFAULT NULL COMMENT '告警等级文本',
    `alarm_type`         varchar(50) NOT NULL DEFAULT 'THRESHOLD' COMMENT 'THRESHOLD(阈值) / COMPREHENSIVE(综合)',
    `alarm_message`      text COMMENT '告警消息（含触发条件详情）',
    `criteria_id`        bigint               DEFAULT NULL COMMENT '触发的判据ID',
    `strategy_id`        bigint               DEFAULT NULL COMMENT '触发的综合策略ID',
    `current_value`      decimal(12, 4)       DEFAULT NULL COMMENT '当前测量值',
    `threshold_value`    decimal(12, 4)       DEFAULT NULL COMMENT '触发阈值',
    `trigger_conditions` json                 DEFAULT NULL COMMENT '触发时的完整条件快照（JSON）',
    `first_trigger_time` datetime             DEFAULT NULL COMMENT '首次触发时间',
    `last_trigger_time`  datetime             DEFAULT NULL COMMENT '最近一次触发时间',
    `trigger_count`      int                  DEFAULT '1' COMMENT '累计触发次数',
    `status`             tinyint              DEFAULT '1' COMMENT '警情状态: 1=待处理 2=处理中 3=已销警 4=误报',
    `status_name`        varchar(20)          DEFAULT '待处理' COMMENT '状态名称',
    `resolved_by`        varchar(64)          DEFAULT NULL COMMENT '处置人',
    `resolved_at`        datetime             DEFAULT NULL COMMENT '处置时间',
    `resolution_note`    text COMMENT '处置备注',
    `create_by`          varchar(64)          DEFAULT NULL COMMENT '创建者（通常为SYSTEM）',
    `create_time`        datetime             DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`          varchar(64)          DEFAULT NULL,
    `update_time`        datetime             DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_record_hp` (`hazard_point_id`),
    KEY `idx_record_level` (`alarm_level`),
    KEY `idx_record_status` (`status`),
    KEY `idx_record_type` (`alarm_type`),
    KEY `idx_record_criteria` (`criteria_id`),
    KEY `idx_record_strategy` (`strategy_id`),
    KEY `idx_record_device` (`device_id`),
    KEY `idx_record_trigger_time` (`first_trigger_time`),
    KEY `idx_record_create_time` (`create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='告警记录表';

-- ------------------------------------------------------------------
-- 4. 告警状态变更日志（新增）
-- ------------------------------------------------------------------
DROP TABLE IF EXISTS `alarm_record_log`;
CREATE TABLE `alarm_record_log`
(
    `id`          bigint  NOT NULL AUTO_INCREMENT,
    `alarm_id`    bigint  NOT NULL COMMENT '告警记录ID',
    `from_status` tinyint     DEFAULT NULL COMMENT '变更前状态',
    `to_status`   tinyint NOT NULL COMMENT '变更后状态',
    `operator`    varchar(64) DEFAULT NULL COMMENT '操作人',
    `note`        text COMMENT '操作备注',
    `create_time` datetime    DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_alarm_log_aid` (`alarm_id`, `create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='告警状态变更日志';

-- ------------------------------------------------------------------
-- 5. 综合告警策略（新增）
-- ------------------------------------------------------------------
DROP TABLE IF EXISTS `alarm_strategy`;
CREATE TABLE `alarm_strategy`
(
    `id`                  bigint       NOT NULL AUTO_INCREMENT,
    `name`                varchar(200) NOT NULL COMMENT '策略名称',
    `description`         text COMMENT '策略描述',
    `trigger_mode`        varchar(20)  NOT NULL DEFAULT 'REALTIME' COMMENT 'REALTIME(实时) / CRON(周期)',
    `cron_expression`     varchar(100)          DEFAULT NULL COMMENT 'Cron表达式（周期触发时必填）',
    `script_type`         varchar(20)           DEFAULT 'GROOVY' COMMENT '脚本类型: GROOVY / JAVASCRIPT',
    `script_content`      text COMMENT '脚本内容',
    `default_alarm_level` tinyint               DEFAULT '2' COMMENT '默认告警等级: 1=蓝 2=黄 3=橙 4=红',
    `silence_minutes`     int                   DEFAULT '0' COMMENT '静默周期（分钟），期内不重复生成告警',
    `escalation_enabled`  tinyint               DEFAULT '0' COMMENT '等级跃升提醒: 0=禁用 1=启用',
    `is_enabled`          tinyint               DEFAULT '1' COMMENT '启用状态: 0=禁用 1=启用',
    `last_run_time`       datetime              DEFAULT NULL COMMENT '最近执行时间',
    `last_run_result`     varchar(50)           DEFAULT NULL COMMENT '最近执行结果: SUCCESS / FAIL / NO_ALARM',
    `create_by`           varchar(64)           DEFAULT NULL,
    `create_time`         datetime              DEFAULT CURRENT_TIMESTAMP,
    `update_by`           varchar(64)           DEFAULT NULL,
    `update_time`         datetime              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `del_flag`            tinyint               DEFAULT '0',
    PRIMARY KEY (`id`),
    KEY `idx_strategy_mode` (`trigger_mode`),
    KEY `idx_strategy_enabled` (`is_enabled`),
    KEY `idx_strategy_del` (`del_flag`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='综合告警策略表';

-- ------------------------------------------------------------------
-- 6. 策略-隐患点绑定（新增）
-- ------------------------------------------------------------------
DROP TABLE IF EXISTS `alarm_strategy_hazard_point`;
CREATE TABLE `alarm_strategy_hazard_point`
(
    `id`              bigint NOT NULL AUTO_INCREMENT,
    `strategy_id`     bigint NOT NULL COMMENT '策略ID',
    `hazard_point_id` bigint NOT NULL COMMENT '隐患点ID',
    `create_by`       varchar(64) DEFAULT NULL,
    `create_time`     datetime    DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_strategy_hp` (`strategy_id`, `hazard_point_id`),
    KEY `idx_strategy_hp_sid` (`strategy_id`),
    KEY `idx_strategy_hp_hid` (`hazard_point_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='综合策略-隐患点绑定表';

-- ------------------------------------------------------------------
-- 7. 告警分发规则（重构）
-- ------------------------------------------------------------------
DROP TABLE IF EXISTS `alarm_dispatch_rule`;
CREATE TABLE `alarm_dispatch_rule`
(
    `id`              bigint       NOT NULL AUTO_INCREMENT,
    `name`            varchar(200) NOT NULL COMMENT '规则名称',
    `hazard_point_id` bigint       DEFAULT NULL COMMENT '隐患点ID（NULL=全局默认规则）',
    `alarm_levels`    varchar(50)  DEFAULT NULL COMMENT '适用告警等级: 逗号分隔 1,2,3,4',
    `alarm_types`     varchar(50)  DEFAULT NULL COMMENT '适用告警类型: THRESHOLD,COMPREHENSIVE',
    `recipients_json` json         DEFAULT NULL COMMENT '接收人列表 [{"userId":1,"name":"张三","phone":"138xxx"}]',
    `channels`        varchar(100) DEFAULT 'SYSTEM' COMMENT '通知渠道: SYSTEM,SMS,EMAIL（逗号分隔）',
    `time_window`     varchar(200) DEFAULT NULL COMMENT '时间窗口限制（如 08:00-20:00），NULL=全天',
    `is_enabled`      tinyint      DEFAULT '1',
    `create_by`       varchar(64)  DEFAULT NULL,
    `create_time`     datetime     DEFAULT CURRENT_TIMESTAMP,
    `update_by`       varchar(64)  DEFAULT NULL,
    `update_time`     datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `del_flag`        tinyint      DEFAULT '0',
    PRIMARY KEY (`id`),
    KEY `idx_dispatch_hp` (`hazard_point_id`),
    KEY `idx_dispatch_enabled` (`is_enabled`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='告警分发规则表';

-- ------------------------------------------------------------------
-- 8. 告警通知记录（保留优化）
-- ------------------------------------------------------------------
DROP TABLE IF EXISTS `alarm_notification`;
CREATE TABLE `alarm_notification`
(
    `id`               bigint      NOT NULL AUTO_INCREMENT,
    `alarm_id`         bigint      NOT NULL COMMENT '告警记录ID',
    `dispatch_rule_id` bigint       DEFAULT NULL COMMENT '匹配的分发规则ID',
    `recipient_id`     bigint      NOT NULL COMMENT '接收人ID（sys_user.id）',
    `recipient_name`   varchar(100) DEFAULT NULL,
    `recipient_phone`  varchar(20)  DEFAULT NULL,
    `channel`          varchar(50) NOT NULL COMMENT '通知渠道: SYSTEM / SMS / EMAIL',
    `title`            varchar(500) DEFAULT NULL COMMENT '通知标题',
    `content`          text COMMENT '通知内容',
    `status`           tinyint      DEFAULT '1' COMMENT '1=待发送 2=已发送 3=发送失败',
    `send_time`        datetime     DEFAULT NULL,
    `error_msg`        varchar(500) DEFAULT NULL,
    `create_time`      datetime     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_notif_alarm` (`alarm_id`),
    KEY `idx_notif_recipient` (`recipient_id`),
    KEY `idx_notif_status` (`status`),
    KEY `idx_notif_channel` (`channel`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='告警通知记录表';
