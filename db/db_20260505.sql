-- ===============================================================
-- 地质灾害监测预警系统 - 数据库结构设计
-- 数据库: geo_hazard_monitor
-- 版本: MySQL 8.0+
-- 字符集: utf8mb4
-- 引擎: InnoDB
-- 日期: 2026-05-05
-- ===============================================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `geo_hazard_monitor`
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE `geo_hazard_monitor`;

-- ===============================================================
-- 一、系统管理模块
-- ===============================================================

-- ---------------------------------------------------------------
-- 1.1 组织架构表
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `sys_organization`;
CREATE TABLE `sys_organization` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `code` VARCHAR(100) NOT NULL COMMENT '组织编码',
  `name` VARCHAR(200) NOT NULL COMMENT '组织名称',
  `parent_id` BIGINT DEFAULT 0 COMMENT '父组织ID，0为根节点',
  `parent_ids` VARCHAR(500) DEFAULT NULL COMMENT '父组织ID路径，如/0/1/2/',
  `level` TINYINT DEFAULT 1 COMMENT '层级: 1-5级',
  `leader` VARCHAR(100) DEFAULT NULL COMMENT '负责人',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `region` VARCHAR(200) DEFAULT NULL COMMENT '区域',
  `center` VARCHAR(200) DEFAULT NULL COMMENT '中心点坐标',
  `address` VARCHAR(500) DEFAULT NULL COMMENT '详细地址',
  `sort_order` INT DEFAULT 0 COMMENT '排序号',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `del_flag` TINYINT DEFAULT 0 COMMENT '删除标记: 0-正常, 1-删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_org_code` (`code`),
  KEY `idx_sys_org_parent_id` (`parent_id`),
  KEY `idx_sys_org_level` (`level`),
  KEY `idx_sys_org_status` (`status`),
  KEY `idx_sys_org_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组织架构表';

-- ---------------------------------------------------------------
-- 1.2 系统用户表
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名/登录账号',
  `password` VARCHAR(200) NOT NULL COMMENT '密码(BCrypt加密)',
  `real_name` VARCHAR(100) DEFAULT NULL COMMENT '真实姓名',
  `avatar` VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `org_id` BIGINT DEFAULT NULL COMMENT '所属组织ID',
  `org_name` VARCHAR(200) DEFAULT NULL COMMENT '所属组织名称',
  `status` TINYINT DEFAULT 0 COMMENT '状态: 0-正常, 1-禁用, 2-锁定, 3-过期',
  `login_fail_count` TINYINT DEFAULT 0 COMMENT '连续登录失败次数',
  `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
  `last_login_ip` VARCHAR(50) DEFAULT NULL COMMENT '最后登录IP',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `del_flag` TINYINT DEFAULT 0 COMMENT '删除标记: 0-正常, 1-删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_user_username` (`username`),
  KEY `idx_sys_user_org_id` (`org_id`),
  KEY `idx_sys_user_status` (`status`),
  KEY `idx_sys_user_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- ---------------------------------------------------------------
-- 1.3 角色表
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `code` VARCHAR(50) NOT NULL COMMENT '角色编码',
  `name` VARCHAR(100) NOT NULL COMMENT '角色名称',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '角色描述',
  `data_scope` TINYINT DEFAULT 1 COMMENT '数据范围: 1-全部, 2-本部门, 3-本部门及下级, 4-仅本人',
  `sort_order` INT DEFAULT 0 COMMENT '排序号',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `del_flag` TINYINT DEFAULT 0 COMMENT '删除标记: 0-正常, 1-删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_role_code` (`code`),
  KEY `idx_sys_role_status` (`status`),
  KEY `idx_sys_role_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- ---------------------------------------------------------------
-- 1.4 用户角色关联表
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_user_role` (`user_id`, `role_id`),
  KEY `idx_sys_user_role_user_id` (`user_id`),
  KEY `idx_sys_user_role_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- ---------------------------------------------------------------
-- 1.5 菜单表
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `parent_id` BIGINT DEFAULT 0 COMMENT '父菜单ID',
  `name` VARCHAR(100) NOT NULL COMMENT '菜单名称',
  `code` VARCHAR(100) DEFAULT NULL COMMENT '菜单编码',
  `path` VARCHAR(500) DEFAULT NULL COMMENT '路由路径',
  `component` VARCHAR(500) DEFAULT NULL COMMENT '组件路径',
  `icon` VARCHAR(200) DEFAULT NULL COMMENT '菜单图标',
  `sort_order` INT DEFAULT 0 COMMENT '排序号',
  `type` TINYINT DEFAULT 1 COMMENT '类型: 1-菜单, 2-按钮, 3-链接',
  `visible` TINYINT DEFAULT 1 COMMENT '是否显示: 0-隐藏, 1-显示',
  `perms` VARCHAR(200) DEFAULT NULL COMMENT '权限标识',
  `is_cache` TINYINT DEFAULT 0 COMMENT '是否缓存: 0-否, 1-是',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `del_flag` TINYINT DEFAULT 0 COMMENT '删除标记: 0-正常, 1-删除',
  PRIMARY KEY (`id`),
  KEY `idx_sys_menu_parent_id` (`parent_id`),
  KEY `idx_sys_menu_type` (`type`),
  KEY `idx_sys_menu_visible` (`visible`),
  KEY `idx_sys_menu_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单表';

-- ---------------------------------------------------------------
-- 1.6 角色菜单关联表
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  `menu_id` BIGINT NOT NULL COMMENT '菜单ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_role_menu` (`role_id`, `menu_id`),
  KEY `idx_sys_role_menu_role_id` (`role_id`),
  KEY `idx_sys_role_menu_menu_id` (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联表';

-- ---------------------------------------------------------------
-- 1.7 操作日志表
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `sys_oper_log`;
CREATE TABLE `sys_oper_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title` VARCHAR(200) DEFAULT NULL COMMENT '操作标题',
  `business_type` TINYINT DEFAULT 0 COMMENT '业务类型: 1-新增, 2-修改, 3-删除, 4-查询, 5-导入, 6-导出',
  `method` VARCHAR(200) DEFAULT NULL COMMENT '请求方法',
  `request_url` VARCHAR(500) DEFAULT NULL COMMENT '请求URL',
  `request_method` VARCHAR(10) DEFAULT NULL COMMENT '请求方式: GET/POST/PUT/DELETE',
  `request_param` TEXT DEFAULT NULL COMMENT '请求参数',
  `response_param` TEXT DEFAULT NULL COMMENT '响应参数',
  `operator` VARCHAR(100) DEFAULT NULL COMMENT '操作人',
  `operator_ip` VARCHAR(50) DEFAULT NULL COMMENT '操作IP',
  `status` TINYINT DEFAULT 0 COMMENT '状态: 0-成功, 1-失败',
  `error_msg` TEXT DEFAULT NULL COMMENT '错误信息',
  `oper_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_sys_oper_log_operator` (`operator`),
  KEY `idx_sys_oper_log_oper_time` (`oper_time`),
  KEY `idx_sys_oper_log_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- ---------------------------------------------------------------
-- 1.8 登录日志表
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `sys_login_log`;
CREATE TABLE `sys_login_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` VARCHAR(100) DEFAULT NULL COMMENT '登录用户名',
  `login_ip` VARCHAR(50) DEFAULT NULL COMMENT '登录IP',
  `login_location` VARCHAR(200) DEFAULT NULL COMMENT '登录地点',
  `status` TINYINT DEFAULT 0 COMMENT '状态: 0-成功, 1-失败',
  `error_msg` VARCHAR(500) DEFAULT NULL COMMENT '错误信息',
  `login_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
  PRIMARY KEY (`id`),
  KEY `idx_sys_login_log_username` (`username`),
  KEY `idx_sys_login_log_login_time` (`login_time`),
  KEY `idx_sys_login_log_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志表';

-- ===============================================================
-- 二、基础管理模块
-- ===============================================================

-- ---------------------------------------------------------------
-- 2.1 隐患点分组表
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `hazard_point_group`;
CREATE TABLE `hazard_point_group` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `code` VARCHAR(100) NOT NULL COMMENT '分组编码',
  `name` VARCHAR(200) NOT NULL COMMENT '分组名称',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '分组描述',
  `sort_order` INT DEFAULT 0 COMMENT '排序号',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `del_flag` TINYINT DEFAULT 0 COMMENT '删除标记: 0-正常, 1-删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_hazard_group_code` (`code`),
  KEY `idx_hazard_group_status` (`status`),
  KEY `idx_hazard_group_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='隐患点分组表';

-- ---------------------------------------------------------------
-- 2.2 隐患点表
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `hazard_point`;
CREATE TABLE `hazard_point` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `code` VARCHAR(100) NOT NULL COMMENT '隐患点编号',
  `name` VARCHAR(200) NOT NULL COMMENT '隐患点名称',
  `group_id` BIGINT DEFAULT NULL COMMENT '分组ID',
  `group_name` VARCHAR(200) DEFAULT NULL COMMENT '分组名称',
  `longitude` DECIMAL(10,6) DEFAULT NULL COMMENT '中心经度',
  `latitude` DECIMAL(10,6) DEFAULT NULL COMMENT '中心纬度',
  `strike` DECIMAL(10,2) DEFAULT NULL COMMENT '走向角度',
  `description` TEXT DEFAULT NULL COMMENT '隐患描述',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 1-监测中, 2-停测中, 3-已完结',
  `device_count` INT DEFAULT 0 COMMENT '绑定设备数量',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `del_flag` TINYINT DEFAULT 0 COMMENT '删除标记: 0-正常, 1-删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_hazard_point_code` (`code`),
  KEY `idx_hazard_point_group_id` (`group_id`),
  KEY `idx_hazard_point_status` (`status`),
  KEY `idx_hazard_point_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='隐患点表';

-- ---------------------------------------------------------------
-- 2.3 监测类型表
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `monitor_type`;
CREATE TABLE `monitor_type` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `code` VARCHAR(100) NOT NULL COMMENT '监测类型编码',
  `name` VARCHAR(200) NOT NULL COMMENT '监测类型名称',
  `device_type` TINYINT DEFAULT 1 COMMENT '设备类型: 1-直连设备, 2-传感器, 3-RTU',
  `icon` VARCHAR(200) DEFAULT NULL COMMENT '图标路径',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '描述',
  `sort_order` INT DEFAULT 0 COMMENT '排序号',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `del_flag` TINYINT DEFAULT 0 COMMENT '删除标记: 0-正常, 1-删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_monitor_type_code` (`code`),
  KEY `idx_monitor_type_device_type` (`device_type`),
  KEY `idx_monitor_type_status` (`status`),
  KEY `idx_monitor_type_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='监测类型表';

-- ---------------------------------------------------------------
-- 2.4 监测内容表
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `monitor_content`;
CREATE TABLE `monitor_content` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `monitor_type_id` BIGINT NOT NULL COMMENT '监测类型ID',
  `code` VARCHAR(100) NOT NULL COMMENT '监测内容编码',
  `name` VARCHAR(200) NOT NULL COMMENT '监测内容名称',
  `unit` VARCHAR(50) DEFAULT NULL COMMENT '单位',
  `indicator_type` VARCHAR(50) DEFAULT NULL COMMENT '指标类型',
  `icon` VARCHAR(200) DEFAULT NULL COMMENT '图标路径',
  `sort_order` INT DEFAULT 0 COMMENT '排序号',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `del_flag` TINYINT DEFAULT 0 COMMENT '删除标记: 0-正常, 1-删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_monitor_content_code` (`code`),
  KEY `idx_monitor_content_type_id` (`monitor_type_id`),
  KEY `idx_monitor_content_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='监测内容表';

-- ---------------------------------------------------------------
-- 2.5 设备表
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `device`;
CREATE TABLE `device` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `code` VARCHAR(100) NOT NULL COMMENT '设备编号',
  `name` VARCHAR(200) NOT NULL COMMENT '设备名称',
  `icon` VARCHAR(200) DEFAULT NULL COMMENT '设备图标',
  `icon_path` VARCHAR(500) DEFAULT NULL COMMENT '图标路径',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 1-正常, 2-故障, 3-离线',
  `run_status` TINYINT DEFAULT 0 COMMENT '运行状态: 0-未知, 1-运行中, 2-停止',
  `last_report_time` DATETIME DEFAULT NULL COMMENT '最近上报时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `del_flag` TINYINT DEFAULT 0 COMMENT '删除标记: 0-正常, 1-删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_code` (`code`),
  KEY `idx_device_status` (`status`),
  KEY `idx_device_run_status` (`run_status`),
  KEY `idx_device_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备表';

-- ---------------------------------------------------------------
-- 2.6 传感器表
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `device_sensor`;
CREATE TABLE `device_sensor` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `device_id` BIGINT NOT NULL COMMENT '设备ID',
  `device_code` VARCHAR(100) DEFAULT NULL COMMENT '设备编号',
  `sensor_code` VARCHAR(100) NOT NULL COMMENT '传感器编号',
  `sensor_name` VARCHAR(200) NOT NULL COMMENT '传感器名称',
  `monitor_type_id` BIGINT NOT NULL COMMENT '监测类型ID',
  `monitor_type_code` VARCHAR(100) DEFAULT NULL COMMENT '监测类型编码',
  `monitor_type_name` VARCHAR(200) DEFAULT NULL COMMENT '监测类型名称',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `del_flag` TINYINT DEFAULT 0 COMMENT '删除标记: 0-正常, 1-删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_sensor_code` (`sensor_code`),
  KEY `idx_device_sensor_device_id` (`device_id`),
  KEY `idx_device_sensor_type_id` (`monitor_type_id`),
  KEY `idx_device_sensor_status` (`status`),
  KEY `idx_device_sensor_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='传感器表';

-- ---------------------------------------------------------------
-- 2.7 传感器属性表
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `sensor_attribute`;
CREATE TABLE `sensor_attribute` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `sensor_id` BIGINT NOT NULL COMMENT '传感器ID',
  `attr_code` VARCHAR(100) NOT NULL COMMENT '属性编码',
  `attr_name` VARCHAR(200) NOT NULL COMMENT '属性名称',
  `indicator_type` VARCHAR(50) DEFAULT NULL COMMENT '指标类型',
  `indicator_type_name` VARCHAR(100) DEFAULT NULL COMMENT '指标类型名称',
  `initial_value` DECIMAL(12,2) DEFAULT NULL COMMENT '初始值',
  `unit` VARCHAR(50) DEFAULT NULL COMMENT '单位',
  `range_min` DECIMAL(12,2) DEFAULT NULL COMMENT '最小值范围',
  `range_max` DECIMAL(12,2) DEFAULT NULL COMMENT '最大值范围',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_sensor_attr_sensor_id` (`sensor_id`),
  KEY `idx_sensor_attr_attr_code` (`attr_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='传感器属性表';

-- ---------------------------------------------------------------
-- 2.8 视频设备表
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `video_device`;
CREATE TABLE `video_device` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `code` VARCHAR(100) NOT NULL COMMENT '设备编号',
  `name` VARCHAR(200) NOT NULL COMMENT '设备名称',
  `icon` VARCHAR(200) DEFAULT NULL COMMENT '图标代码',
  `icon_path` VARCHAR(500) DEFAULT NULL COMMENT '图标路径',
  `protocol_code` VARCHAR(50) DEFAULT NULL COMMENT '协议类型编码',
  `protocol_name` VARCHAR(100) DEFAULT NULL COMMENT '协议类型名称',
  `stream_url` VARCHAR(500) DEFAULT NULL COMMENT '视频流地址',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 0-离线, 1-在线, 2-故障',
  `last_online_time` DATETIME DEFAULT NULL COMMENT '最近在线时间',
  `install_time` DATETIME DEFAULT NULL COMMENT '安装时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `del_flag` TINYINT DEFAULT 0 COMMENT '删除标记: 0-正常, 1-删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_video_device_code` (`code`),
  KEY `idx_video_device_status` (`status`),
  KEY `idx_video_device_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频设备表';

-- ===============================================================
-- 三、关联关系模块
-- ===============================================================

-- ---------------------------------------------------------------
-- 3.1 设备隐患点关联表
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `device_hazard_point`;
CREATE TABLE `device_hazard_point` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `device_id` BIGINT NOT NULL COMMENT '设备ID',
  `hazard_point_id` BIGINT NOT NULL COMMENT '隐患点ID',
  `install_longitude` DECIMAL(10,6) DEFAULT NULL COMMENT '安装经度',
  `install_latitude` DECIMAL(10,6) DEFAULT NULL COMMENT '安装纬度',
  `bind_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_hazard_point` (`device_id`, `hazard_point_id`),
  KEY `idx_device_hazard_point_device_id` (`device_id`),
  KEY `idx_device_hazard_point_hp_id` (`hazard_point_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备隐患点关联表';

-- ---------------------------------------------------------------
-- 3.2 视频设备隐患点关联表
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `video_device_hazard_point`;
CREATE TABLE `video_device_hazard_point` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `video_device_id` BIGINT NOT NULL COMMENT '视频设备ID',
  `hazard_point_id` BIGINT NOT NULL COMMENT '隐患点ID',
  `install_longitude` DECIMAL(10,6) DEFAULT NULL COMMENT '安装经度',
  `install_latitude` DECIMAL(10,6) DEFAULT NULL COMMENT '安装纬度',
  `bind_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_video_device_hazard_point` (`video_device_id`, `hazard_point_id`),
  KEY `idx_video_device_hp_device_id` (`video_device_id`),
  KEY `idx_video_device_hp_hp_id` (`hazard_point_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频设备隐患点关联表';

-- ===============================================================
-- 四、告警中心模块
-- ===============================================================

-- ---------------------------------------------------------------
-- 4.1 告警判据表
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `alarm_criteria`;
CREATE TABLE `alarm_criteria` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `hazard_point_id` BIGINT NOT NULL COMMENT '隐患点ID',
  `name` VARCHAR(200) NOT NULL COMMENT '判据名称',
  `device_id` BIGINT DEFAULT NULL COMMENT '设备ID',
  `device_name` VARCHAR(200) DEFAULT NULL COMMENT '设备名称',
  `monitor_type_id` BIGINT DEFAULT NULL COMMENT '监测类型ID',
  `monitor_type_name` VARCHAR(200) DEFAULT NULL COMMENT '监测类型名称',
  `monitor_content_code` VARCHAR(100) DEFAULT NULL COMMENT '监测内容编码',
  `monitor_content_name` VARCHAR(200) DEFAULT NULL COMMENT '监测内容名称',
  `blue_expression` VARCHAR(500) DEFAULT NULL COMMENT '蓝色预警表达式',
  `blue_description` VARCHAR(500) DEFAULT NULL COMMENT '蓝色预警描述',
  `yellow_expression` VARCHAR(500) DEFAULT NULL COMMENT '黄色预警表达式',
  `yellow_description` VARCHAR(500) DEFAULT NULL COMMENT '黄色预警描述',
  `orange_expression` VARCHAR(500) DEFAULT NULL COMMENT '橙色预警表达式',
  `orange_description` VARCHAR(500) DEFAULT NULL COMMENT '橙色预警描述',
  `red_expression` VARCHAR(500) DEFAULT NULL COMMENT '红色预警表达式',
  `red_description` VARCHAR(500) DEFAULT NULL COMMENT '红色预警描述',
  `is_enabled` TINYINT DEFAULT 1 COMMENT '是否启用: 0-禁用, 1-启用',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `del_flag` TINYINT DEFAULT 0 COMMENT '删除标记: 0-正常, 1-删除',
  PRIMARY KEY (`id`),
  KEY `idx_alarm_criteria_hp_id` (`hazard_point_id`),
  KEY `idx_alarm_criteria_device_id` (`device_id`),
  KEY `idx_alarm_criteria_enabled` (`is_enabled`),
  KEY `idx_alarm_criteria_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警判据表';

-- ---------------------------------------------------------------
-- 4.2 告警分发规则表
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `alarm_dispatch_rule`;
CREATE TABLE `alarm_dispatch_rule` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `hazard_point_id` BIGINT NOT NULL COMMENT '隐患点ID',
  `name` VARCHAR(200) NOT NULL COMMENT '规则名称',
  `type` TINYINT DEFAULT 1 COMMENT '类型: 1-告警分发, 2-状态通知',
  `alarm_level` VARCHAR(200) DEFAULT NULL COMMENT '告警等级列表',
  `recipient_ids` VARCHAR(500) DEFAULT NULL COMMENT '接收人ID或设备ID列表',
  `channel` VARCHAR(200) DEFAULT NULL COMMENT '通知渠道: SYSTEM,SMS,EMAIL',
  `is_enabled` TINYINT DEFAULT 1 COMMENT '是否启用: 0-禁用, 1-启用',
  `time_setting` VARCHAR(50) DEFAULT NULL COMMENT '时间频率设置',
  `time_value` VARCHAR(100) DEFAULT NULL COMMENT '时间值列表：逗号分隔',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `del_flag` TINYINT DEFAULT 0 COMMENT '删除标记: 0-正常, 1-删除',
  PRIMARY KEY (`id`),
  KEY `idx_alarm_dispatch_hp_id` (`hazard_point_id`),
  KEY `idx_alarm_dispatch_type` (`type`),
  KEY `idx_alarm_dispatch_enabled` (`is_enabled`),
  KEY `idx_alarm_dispatch_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警分发规则表';

-- ---------------------------------------------------------------
-- 4.3 告警记录表
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `alarm_record`;
CREATE TABLE `alarm_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `hazard_point_id` BIGINT NOT NULL COMMENT '隐患点ID',
  `hazard_point_code` VARCHAR(100) DEFAULT NULL COMMENT '隐患点编号',
  `hazard_point_name` VARCHAR(200) DEFAULT NULL COMMENT '隐患点名称',
  `alarm_level` TINYINT NOT NULL COMMENT '告警等级: 1-蓝色, 2-黄色, 3-橙色, 4-红色',
  `alarm_level_text` VARCHAR(50) DEFAULT NULL COMMENT '告警等级文本',
  `alarm_type` VARCHAR(100) DEFAULT NULL COMMENT '告警类型: 1-阈值告警, 2-模型告警, 3-综合告警, 4-其他告警',
  `alarm_message` TEXT DEFAULT NULL COMMENT '告警消息',
  `device_id` BIGINT DEFAULT NULL COMMENT '设备ID',
  `sensor_id` BIGINT DEFAULT NULL COMMENT '传感器ID',
  `monitor_type_id` BIGINT DEFAULT NULL COMMENT '监测类型ID',
  `current_value` DECIMAL(12,2) DEFAULT NULL COMMENT '当前值',
  `alarm_criteria_id` DECIMAL(12,2) DEFAULT NULL COMMENT '阈值',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 1-待处理, 2-处理中, 3-已处理, 4-已忽略',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '告警时间',
  `handle_time` DATETIME DEFAULT NULL COMMENT '处理时间',
  `handle_person` VARCHAR(100) DEFAULT NULL COMMENT '处理人',
  `handle_result` TEXT DEFAULT NULL COMMENT '处理结果',
  PRIMARY KEY (`id`),
  KEY `idx_alarm_record_hp_id` (`hazard_point_id`),
  KEY `idx_alarm_record_level` (`alarm_level`),
  KEY `idx_alarm_record_status` (`status`),
  KEY `idx_alarm_record_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警记录表';

-- ---------------------------------------------------------------
-- 4.4 告警通知记录表
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `alarm_notification`;
CREATE TABLE `alarm_notification` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `alarm_id` BIGINT NOT NULL COMMENT '告警记录ID',
  `recipient_id` BIGINT NOT NULL COMMENT '接收人ID',
  `recipient_name` VARCHAR(100) DEFAULT NULL COMMENT '接收人名称',
  `recipient_phone` VARCHAR(20) DEFAULT NULL COMMENT '接收人电话',
  `channel` VARCHAR(50) DEFAULT NULL COMMENT '通知渠道',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 1-待发送, 2-已发送, 3-发送失败',
  `send_time` DATETIME DEFAULT NULL COMMENT '发送时间',
  `error_msg` VARCHAR(500) DEFAULT NULL COMMENT '错误信息',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_alarm_notification_alarm_id` (`alarm_id`),
  KEY `idx_alarm_notification_recipient_id` (`recipient_id`),
  KEY `idx_alarm_notification_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警通知记录表';

-- ===============================================================
-- 五、监测数据模块
-- ===============================================================

-- ---------------------------------------------------------------
-- 5.1 监测数据表(按月分区)
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `monitor_data`;
CREATE TABLE `monitor_data` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `hazard_point_id` BIGINT NOT NULL COMMENT '隐患点ID',
  `device_id` BIGINT NOT NULL COMMENT '设备ID',
  `device_code` VARCHAR(100) DEFAULT NULL COMMENT '设备编号',
  `sensor_id` BIGINT NOT NULL COMMENT '传感器ID',
  `sensor_code` VARCHAR(100) DEFAULT NULL COMMENT '传感器编号',
  `monitor_type_id` BIGINT NOT NULL COMMENT '监测类型ID',
  `attr_code` VARCHAR(100) NOT NULL COMMENT '属性编码',
  `attr_name` VARCHAR(200) DEFAULT NULL COMMENT '属性名称',
  `value` DECIMAL(12,2) NOT NULL COMMENT '监测值',
  `unit` VARCHAR(50) DEFAULT NULL COMMENT '单位',
  `direction` VARCHAR(10) DEFAULT NULL COMMENT '方向: X/Y/Z',
  `data_time` DATETIME NOT NULL COMMENT '数据时间',
  `quality` TINYINT DEFAULT 0 COMMENT '数据质量: 0-正常, 1-可疑, 2-无效',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_monitor_data_hp_id` (`hazard_point_id`),
  KEY `idx_monitor_data_device_id` (`device_id`),
  KEY `idx_monitor_data_sensor_id` (`sensor_id`),
  KEY `idx_monitor_data_data_time` (`data_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='监测数据表';

-- ---------------------------------------------------------------
-- 5.2 设备状态日志表
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `device_status_log`;
CREATE TABLE `device_status_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `device_id` BIGINT NOT NULL COMMENT '设备ID',
  `device_code` VARCHAR(100) DEFAULT NULL COMMENT '设备编号',
  `old_status` TINYINT DEFAULT NULL COMMENT '旧状态',
  `new_status` TINYINT NOT NULL COMMENT '新状态',
  `status_text` VARCHAR(50) DEFAULT NULL COMMENT '状态文本',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
  PRIMARY KEY (`id`),
  KEY `idx_device_status_log_device_id` (`device_id`),
  KEY `idx_device_status_log_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备状态日志表';

-- ===============================================================
-- 六、报告报表模块
-- ===============================================================

-- ---------------------------------------------------------------
-- 6.1 报告模板表
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `report_template`;
CREATE TABLE `report_template` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` VARCHAR(200) NOT NULL COMMENT '模板名称',
  `code` VARCHAR(100) NOT NULL COMMENT '模板编码',
  `type` TINYINT DEFAULT 1 COMMENT '类型: 1-日报, 2-周报, 3-月报, 4-季报, 5-年报, 6-自定义',
  `content` LONGTEXT DEFAULT NULL COMMENT '模板内容(HTML)',
  `params` TEXT DEFAULT NULL COMMENT '参数配置(JSON)',
  `sort_order` INT DEFAULT 0 COMMENT '排序号',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `del_flag` TINYINT DEFAULT 0 COMMENT '删除标记: 0-正常, 1-删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_report_template_code` (`code`),
  KEY `idx_report_template_type` (`type`),
  KEY `idx_report_template_status` (`status`),
  KEY `idx_report_template_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报告模板表';

-- ---------------------------------------------------------------
-- 6.2 报告记录表
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `report_record`;
CREATE TABLE `report_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `template_id` BIGINT NOT NULL COMMENT '模板ID',
  `template_name` VARCHAR(200) DEFAULT NULL COMMENT '模板名称',
  `hazard_point_id` BIGINT DEFAULT NULL COMMENT '隐患点ID',
  `hazard_point_code` VARCHAR(100) DEFAULT NULL COMMENT '隐患点编号',
  `hazard_point_name` VARCHAR(200) DEFAULT NULL COMMENT '隐患点名称',
  `report_name` VARCHAR(200) NOT NULL COMMENT '报告名称',
  `report_date` DATETIME NOT NULL COMMENT '报告日期',
  `content` LONGTEXT DEFAULT NULL COMMENT '报告内容(HTML)',
  `file_path` VARCHAR(500) DEFAULT NULL COMMENT '文件路径',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 1-生成中, 2-已生成, 3-生成失败',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_report_record_template_id` (`template_id`),
  KEY `idx_report_record_hp_id` (`hazard_point_id`),
  KEY `idx_report_record_report_date` (`report_date`),
  KEY `idx_report_record_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报告记录表';

-- ===============================================================
-- 七、数据字典表
-- ===============================================================

-- ---------------------------------------------------------------
-- 7.1 字典类型表
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `sys_dict_type`;
CREATE TABLE `sys_dict_type` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `code` VARCHAR(100) NOT NULL COMMENT '字典类型编码',
  `name` VARCHAR(200) NOT NULL COMMENT '字典类型名称',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '描述',
  `sort_order` INT DEFAULT 0 COMMENT '排序号',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `del_flag` TINYINT DEFAULT 0 COMMENT '删除标记: 0-正常, 1-删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_dict_type_code` (`code`),
  KEY `idx_sys_dict_type_status` (`status`),
  KEY `idx_sys_dict_type_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典类型表';

-- ---------------------------------------------------------------
-- 7.2 字典数据表
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `sys_dict_data`;
CREATE TABLE `sys_dict_data` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `type_code` VARCHAR(100) NOT NULL COMMENT '字典类型编码',
  `value` VARCHAR(200) NOT NULL COMMENT '字典值',
  `label` VARCHAR(200) NOT NULL COMMENT '字典标签',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '描述',
  `sort_order` INT DEFAULT 0 COMMENT '排序号',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_sys_dict_data_type_code` (`type_code`),
  KEY `idx_sys_dict_data_value` (`value`),
  KEY `idx_sys_dict_data_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典数据表';

-- ===============================================================
-- 八、系统参数表
-- ===============================================================

DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `code` VARCHAR(100) NOT NULL COMMENT '配置编码',
  `value` TEXT DEFAULT NULL COMMENT '配置值',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '描述',
  `type` VARCHAR(50) DEFAULT NULL COMMENT '配置类型',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `del_flag` TINYINT DEFAULT 0 COMMENT '删除标记: 0-正常, 1-删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_config_code` (`code`),
  KEY `idx_sys_config_type` (`type`),
  KEY `idx_sys_config_status` (`status`),
  KEY `idx_sys_config_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统参数表';

-- ===============================================================
-- 初始化数据
-- ===============================================================

-- 初始化组织架构
INSERT INTO `sys_organization` (`id`, `code`, `name`, `parent_id`, `parent_ids`, `level`, `status`) VALUES
(1, 'ROOT', '系统管理员', 0, '/0/', 1, 1),
(2, 'DEPT001', '监测中心', 1, '/0/1/', 2, 1),
(3, 'DEPT002', '运维部', 1, '/0/1/', 2, 1);

-- 初始化管理员用户
INSERT INTO `sys_user` (`id`, `username`, `password`, `real_name`, `status`) VALUES
(1, 'admin', '$2a$10$N9qo8uLOickgx2ZMRZoMye.IjzqAKL9xL5jvMFVdNJHvGCgTq/VEq', '系统管理员', 0);

-- 初始化角色
INSERT INTO `sys_role` (`id`, `code`, `name`, `description`, `data_scope`, `status`) VALUES
(1, 'ADMIN', '超级管理员', '系统超级管理员', 1, 1),
(2, 'MONITOR', '监测管理员', '监测业务管理员', 2, 1),
(3, 'OPERATOR', '操作员', '普通操作员', 3, 1);

-- 初始化用户角色关联
INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES
(1, 1);

-- 初始化字典类型
INSERT INTO `sys_dict_type` (`code`, `name`, `description`) VALUES
('ALARM_LEVEL', '告警等级', '告警等级字典'),
('DEVICE_STATUS', '设备状态', '设备状态字典'),
('HAZARD_STATUS', '隐患点状态', '隐患点状态字典'),
('MONITOR_TYPE', '监测类型', '监测类型字典'),
('NOTIFY_CHANNEL', '通知渠道', '通知渠道字典');

-- 初始化字典数据
INSERT INTO `sys_dict_data` (`type_code`, `value`, `label`) VALUES
('ALARM_LEVEL', '1', '一级(蓝色)'),
('ALARM_LEVEL', '2', '二级(黄色)'),
('ALARM_LEVEL', '3', '三级(橙色)'),
('ALARM_LEVEL', '4', '四级(红色)'),
('DEVICE_STATUS', '1', '正常'),
('DEVICE_STATUS', '2', '故障'),
('DEVICE_STATUS', '3', '离线'),
('HAZARD_STATUS', '1', '监测中'),
('HAZARD_STATUS', '2', '停测中'),
('HAZARD_STATUS', '3', '已完结'),
('NOTIFY_CHANNEL', 'SYSTEM', '系统消息'),
('NOTIFY_CHANNEL', 'SMS', '短信通知'),
('NOTIFY_CHANNEL', 'WECHAT', '微信通知'),
('NOTIFY_CHANNEL', 'EMAIL', '电子邮件');

-- 初始化监测类型
INSERT INTO `monitor_type` (`code`, `name`, `device_type`, `status`) VALUES
('JCLX001', '雨量监测', 2, 1),
('JCLX002', '位移监测', 2, 1),
('JCLX003', '温湿度监测', 2, 1),
('JCLX004', '水位监测', 2, 1),
('JCLX005', '裂缝监测', 2, 1),
('JCLX006', '倾斜监测', 2, 1),
('JCLX007', '地温监测', 2, 1),
('JCLX008', '含水率监测', 2, 1);

-- 初始化监测内容
INSERT INTO `monitor_content` (`monitor_type_id`, `code`, `name`, `unit`, `indicator_type`) VALUES
(1, 'rainfall_hour', '小时雨量', 'mm', 'yl'),
(1, 'rainfall_day', '日雨量', 'mm', 'yl'),
(2, 'displacement_x', 'X轴位移', 'mm', 'wy'),
(2, 'displacement_y', 'Y轴位移', 'mm', 'wy'),
(2, 'displacement_z', 'Z轴位移', 'mm', 'wy'),
(3, 'temperature', '温度', '℃', 'wd'),
(3, 'humidity', '含水率', '%', 'hsl'),
(4, 'water_level', '水位', 'm', 'sw'),
(5, 'crack_width', '裂缝宽度', 'mm', 'lf'),
(6, 'inclination_x', 'X方向倾角', '°', 'qx'),
(6, 'inclination_y', 'Y方向倾角', '°', 'qx'),
(7, 'soil_temp_10cm', '10cm地温', '℃', 'dw'),
(8, 'soil_moisture', '土壤含水率', '%', 'hsl');

COMMIT;

-- ===============================================================
-- 结束
-- ===============================================================
