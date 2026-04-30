-- ===============================================================
-- 地质灾害监测预警系统 - 完整数据库结构设计
-- 数据库: geo_hazard_monitor
-- 版本: MySQL 8.0+
-- 字符集: utf8mb4
-- 引擎: InnoDB
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
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_level` (`level`),
  KEY `idx_status` (`status`),
  KEY `idx_del_flag` (`del_flag`)
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
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_org_id` (`org_id`),
  KEY `idx_status` (`status`),
  KEY `idx_del_flag` (`del_flag`)
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
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_status` (`status`),
  KEY `idx_del_flag` (`del_flag`)
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
  UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- ---------------------------------------------------------------
-- 1.5 菜单权限表
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `parent_id` BIGINT DEFAULT 0 COMMENT '父菜单ID',
  `name` VARCHAR(100) NOT NULL COMMENT '菜单名称',
  `path` VARCHAR(200) DEFAULT NULL COMMENT '路由路径',
  `component` VARCHAR(200) DEFAULT NULL COMMENT '组件路径',
  `icon` VARCHAR(100) DEFAULT NULL COMMENT '菜单图标',
  `permission` VARCHAR(200) DEFAULT NULL COMMENT '权限标识',
  `menu_type` TINYINT DEFAULT 1 COMMENT '菜单类型: 1-目录, 2-菜单, 3-按钮',
  `sort_order` INT DEFAULT 0 COMMENT '排序号',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `del_flag` TINYINT DEFAULT 0 COMMENT '删除标记: 0-正常, 1-删除',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_menu_type` (`menu_type`),
  KEY `idx_status` (`status`),
  KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单权限表';

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
  UNIQUE KEY `uk_role_menu` (`role_id`, `menu_id`),
  KEY `idx_role_id` (`role_id`),
  KEY `idx_menu_id` (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联表';

-- ---------------------------------------------------------------
-- 1.7 系统操作日志表
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `sys_log`;
CREATE TABLE `sys_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `log_type` TINYINT DEFAULT 1 COMMENT '日志类型: 1-登录, 2-新增, 3-修改, 4-删除, 5-导出, 6-告警处置, 7-参数配置',
  `title` VARCHAR(200) DEFAULT NULL COMMENT '操作标题',
  `method` VARCHAR(500) DEFAULT NULL COMMENT '请求方法',
  `request_url` VARCHAR(500) DEFAULT NULL COMMENT '请求URL',
  `request_params` TEXT DEFAULT NULL COMMENT '请求参数',
  `response_data` TEXT DEFAULT NULL COMMENT '响应数据',
  `ip_address` VARCHAR(50) DEFAULT NULL COMMENT 'IP地址',
  `user_agent` VARCHAR(500) DEFAULT NULL COMMENT '浏览器UA',
  `user_id` BIGINT DEFAULT NULL COMMENT '操作用户ID',
  `username` VARCHAR(50) DEFAULT NULL COMMENT '操作用户名',
  `org_id` BIGINT DEFAULT NULL COMMENT '所属组织ID',
  `org_name` VARCHAR(200) DEFAULT NULL COMMENT '所属组织名称',
  `execute_time` INT DEFAULT NULL COMMENT '执行时长(ms)',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 0-失败, 1-成功',
  `error_msg` TEXT DEFAULT NULL COMMENT '错误信息',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_log_type` (`log_type`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统操作日志表';

-- ---------------------------------------------------------------
-- 1.8 系统参数配置表
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `config_key` VARCHAR(100) NOT NULL COMMENT '参数键',
  `config_value` VARCHAR(500) DEFAULT NULL COMMENT '参数值',
  `category` VARCHAR(50) NOT NULL COMMENT '分类',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '参数说明',
  `sort_order` INT DEFAULT 0 COMMENT '排序号',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`),
  KEY `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统参数配置表';

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
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `del_flag` TINYINT DEFAULT 0 COMMENT '删除标记: 0-正常, 1-删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='隐患点分组表';

-- ---------------------------------------------------------------
-- 2.2 隐患点主表
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `hazard_point`;
CREATE TABLE `hazard_point` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `code` VARCHAR(100) NOT NULL COMMENT '隐患点编号',
  `name` VARCHAR(200) NOT NULL COMMENT '隐患点名称',
  `group_id` BIGINT DEFAULT NULL COMMENT '分组ID',
  `group_name` VARCHAR(200) DEFAULT NULL COMMENT '分组名称',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 1-监测中, 2-停测中, 3-已完结',
  `longitude` DECIMAL(10,6) DEFAULT NULL COMMENT '经度',
  `latitude` DECIMAL(10,6) DEFAULT NULL COMMENT '纬度',
  `altitude` DECIMAL(10,2) DEFAULT NULL COMMENT '海拔高度(米)',
  `location` VARCHAR(500) DEFAULT NULL COMMENT '位置描述',
  `address` VARCHAR(500) DEFAULT NULL COMMENT '详细地址',
  `boundary` TEXT DEFAULT NULL COMMENT '范围边界坐标JSON',
  `strike` DECIMAL(5,2) DEFAULT NULL COMMENT '走向角度(度)',
  `strike_arrow` TEXT DEFAULT NULL COMMENT '走向箭头坐标JSON',
  `description` VARCHAR(1000) DEFAULT NULL COMMENT '隐患点描述',
  `risk_level` TINYINT DEFAULT NULL COMMENT '风险等级: 1-高, 2-中, 3-低',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `del_flag` TINYINT DEFAULT 0 COMMENT '删除标记: 0-正常, 1-删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_group_id` (`group_id`),
  KEY `idx_status` (`status`),
  KEY `idx_risk_level` (`risk_level`),
  KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='隐患点主表';

-- ---------------------------------------------------------------
-- 2.3 监测内容枚举表
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `monitor_content`;
CREATE TABLE `monitor_content` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `code` VARCHAR(50) NOT NULL COMMENT '监测内容编码',
  `name` VARCHAR(100) NOT NULL COMMENT '监测内容名称',
  `unit` VARCHAR(20) NOT NULL COMMENT '默认单位',
  `icon` VARCHAR(100) DEFAULT NULL COMMENT '图标标识',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '描述',
  `sort_order` INT DEFAULT 0 COMMENT '排序号',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `del_flag` TINYINT DEFAULT 0 COMMENT '删除标记: 0-正常, 1-删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='监测内容枚举表';

-- ---------------------------------------------------------------
-- 2.4 监测类型主表
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `monitor_type`;
CREATE TABLE `monitor_type` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `code` VARCHAR(50) NOT NULL COMMENT '监测类型编号',
  `name` VARCHAR(200) NOT NULL COMMENT '监测类型名称',
  `device_type` TINYINT DEFAULT 1 COMMENT '设备类型: 1-直连设备, 2-传感器, 3-RTU',
  `description` VARCHAR(1000) DEFAULT NULL COMMENT '描述',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `del_flag` TINYINT DEFAULT 0 COMMENT '删除标记: 0-正常, 1-删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_device_type` (`device_type`),
  KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='监测类型主表';

-- ---------------------------------------------------------------
-- 2.5 监测类型参数表
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `monitor_type_param`;
CREATE TABLE `monitor_type_param` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `monitor_type_id` BIGINT NOT NULL COMMENT '监测类型ID',
  `monitor_content_code` VARCHAR(50) NOT NULL COMMENT '监测内容编码',
  `monitor_content_name` VARCHAR(100) NOT NULL COMMENT '监测内容名称',
  `icon` VARCHAR(200) DEFAULT NULL COMMENT '图标路径',
  `dimension` VARCHAR(50) DEFAULT NULL COMMENT '维度: x,y,z或空',
  `range_min` DECIMAL(20,4) DEFAULT NULL COMMENT '量程最小值',
  `range_max` DECIMAL(20,4) DEFAULT NULL COMMENT '量程最大值',
  `unit` VARCHAR(20) DEFAULT NULL COMMENT '单位',
  `sort_order` INT DEFAULT 0 COMMENT '排序号',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_monitor_type_id` (`monitor_type_id`),
  KEY `idx_monitor_content_code` (`monitor_content_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='监测类型参数表';

-- ---------------------------------------------------------------
-- 2.6 设备主表
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `device`;
CREATE TABLE `device` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `code` VARCHAR(100) NOT NULL COMMENT '设备编号',
  `name` VARCHAR(200) NOT NULL COMMENT '设备名称',
  `icon` VARCHAR(100) DEFAULT NULL COMMENT '设备图标标识',
  `icon_path` VARCHAR(500) DEFAULT NULL COMMENT '设备图标路径',
  `status` TINYINT DEFAULT 1 COMMENT '设备状态: 1-正常, 2-故障, 3-维修, 4-离线',
  `run_status` TINYINT DEFAULT 0 COMMENT '运行状态: 0-离线, 1-在线(系统自动维护)',
  `last_report_time` DATETIME DEFAULT NULL COMMENT '最近上报时间(系统自动维护)',
  `install_time` DATETIME DEFAULT NULL COMMENT '安装时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `del_flag` TINYINT DEFAULT 0 COMMENT '删除标记: 0-正常, 1-删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_status` (`status`),
  KEY `idx_run_status` (`run_status`),
  KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备主表';

-- ---------------------------------------------------------------
-- 2.7 设备传感器表
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `device_sensor`;
CREATE TABLE `device_sensor` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `device_id` BIGINT NOT NULL COMMENT '设备ID',
  `device_code` VARCHAR(100) NOT NULL COMMENT '设备编号',
  `code` VARCHAR(100) NOT NULL COMMENT '传感器编号',
  `name` VARCHAR(200) NOT NULL COMMENT '传感器名称',
  `monitor_type_id` BIGINT NOT NULL COMMENT '监测类型ID',
  `monitor_type_code` VARCHAR(50) NOT NULL COMMENT '监测类型编码',
  `monitor_type_name` VARCHAR(200) NOT NULL COMMENT '监测类型名称',
  `monitor_content_code` VARCHAR(50) NOT NULL COMMENT '监测内容编码',
  `monitor_content_name` VARCHAR(100) NOT NULL COMMENT '监测内容名称',
  `dimension` VARCHAR(50) DEFAULT NULL COMMENT '维度: x,y,z或空',
  `initial_value` DECIMAL(20,4) DEFAULT 0 COMMENT '初始值',
  `initial_value_x` DECIMAL(20,4) DEFAULT NULL COMMENT 'X轴初始值',
  `initial_value_y` DECIMAL(20,4) DEFAULT NULL COMMENT 'Y轴初始值',
  `initial_value_z` DECIMAL(20,4) DEFAULT NULL COMMENT 'Z轴初始值',
  `unit` VARCHAR(20) DEFAULT NULL COMMENT '单位',
  `range_min` DECIMAL(20,4) DEFAULT NULL COMMENT '量程最小值',
  `range_max` DECIMAL(20,4) DEFAULT NULL COMMENT '量程最大值',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
  `install_time` DATETIME DEFAULT NULL COMMENT '安装时间',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_sensor` (`device_id`, `code`),
  KEY `idx_device_id` (`device_id`),
  KEY `idx_monitor_type_id` (`monitor_type_id`),
  KEY `idx_monitor_content_code` (`monitor_content_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备传感器表';

-- ---------------------------------------------------------------
-- 2.8 视频设备主表
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `video_device`;
CREATE TABLE `video_device` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `code` VARCHAR(100) NOT NULL COMMENT '设备编号',
  `name` VARCHAR(200) NOT NULL COMMENT '设备名称',
  `icon` VARCHAR(100) DEFAULT NULL COMMENT '图标标识',
  `icon_path` VARCHAR(500) DEFAULT NULL COMMENT '图标路径',
  `protocol_code` VARCHAR(20) NOT NULL COMMENT '协议编码: RTMP/RTSP/ONVIF',
  `stream_url` VARCHAR(1000) NOT NULL COMMENT '视频流地址',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 0-离线, 1-在线, 2-故障',
  `last_online_time` DATETIME DEFAULT NULL COMMENT '最近在线时间',
  `install_time` DATETIME DEFAULT NULL COMMENT '安装时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `del_flag` TINYINT DEFAULT 0 COMMENT '删除标记: 0-正常, 1-删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_protocol_code` (`protocol_code`),
  KEY `idx_status` (`status`),
  KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频设备主表';

-- ---------------------------------------------------------------
-- 2.9 视频截图记录表
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `video_snapshot`;
CREATE TABLE `video_snapshot` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `video_device_id` BIGINT NOT NULL COMMENT '视频设备ID',
  `video_device_code` VARCHAR(100) NOT NULL COMMENT '视频设备编号',
  `snapshot_path` VARCHAR(500) NOT NULL COMMENT '截图文件路径',
  `snapshot_time` DATETIME NOT NULL COMMENT '截图时间',
  `image_width` INT DEFAULT NULL COMMENT '图片宽度',
  `image_height` INT DEFAULT NULL COMMENT '图片高度',
  `file_size` BIGINT DEFAULT NULL COMMENT '文件大小(字节)',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `creator` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  PRIMARY KEY (`id`),
  KEY `idx_video_device_id` (`video_device_id`),
  KEY `idx_snapshot_time` (`snapshot_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频截图记录表';

-- ===============================================================
-- 三、关联关系表
-- ===============================================================

-- ---------------------------------------------------------------
-- 3.1 设备与隐患点关联表
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `device_hazard_point`;
CREATE TABLE `device_hazard_point` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `device_id` BIGINT NOT NULL COMMENT '设备ID',
  `device_code` VARCHAR(100) NOT NULL COMMENT '设备编号',
  `device_name` VARCHAR(200) NOT NULL COMMENT '设备名称',
  `hazard_point_id` BIGINT NOT NULL COMMENT '隐患点ID',
  `hazard_point_code` VARCHAR(100) NOT NULL COMMENT '隐患点编号',
  `hazard_point_name` VARCHAR(200) NOT NULL COMMENT '隐患点名称',
  `install_longitude` DECIMAL(10,6) DEFAULT NULL COMMENT '设备安装经度',
  `install_latitude` DECIMAL(10,6) DEFAULT NULL COMMENT '设备安装纬度',
  `bind_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
  `unbind_time` DATETIME DEFAULT NULL COMMENT '解绑时间',
  `is_active` TINYINT DEFAULT 1 COMMENT '是否有效: 0-无效, 1-有效',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_hazard` (`device_id`, `hazard_point_id`),
  KEY `idx_device_id` (`device_id`),
  KEY `idx_hazard_point_id` (`hazard_point_id`),
  KEY `idx_is_active` (`is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备与隐患点关联表';

-- ---------------------------------------------------------------
-- 3.2 视频设备与隐患点关联表
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `video_device_hazard_point`;
CREATE TABLE `video_device_hazard_point` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `video_device_id` BIGINT NOT NULL COMMENT '视频设备ID',
  `video_device_code` VARCHAR(100) NOT NULL COMMENT '视频设备编号',
  `video_device_name` VARCHAR(200) NOT NULL COMMENT '视频设备名称',
  `hazard_point_id` BIGINT NOT NULL COMMENT '隐患点ID',
  `hazard_point_code` VARCHAR(100) NOT NULL COMMENT '隐患点编号',
  `hazard_point_name` VARCHAR(200) NOT NULL COMMENT '隐患点名称',
  `install_longitude` DECIMAL(10,6) DEFAULT NULL COMMENT '设备安装经度',
  `install_latitude` DECIMAL(10,6) DEFAULT NULL COMMENT '设备安装纬度',
  `bind_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
  `unbind_time` DATETIME DEFAULT NULL COMMENT '解绑时间',
  `is_active` TINYINT DEFAULT 1 COMMENT '是否有效: 0-无效, 1-有效',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_video_hazard` (`video_device_id`, `hazard_point_id`),
  KEY `idx_video_device_id` (`video_device_id`),
  KEY `idx_hazard_point_id` (`hazard_point_id`),
  KEY `idx_is_active` (`is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频设备与隐患点关联表';

-- ---------------------------------------------------------------
-- 3.3 隐患点绑定设备表（汇总视图用）
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `hazard_point_device`;
CREATE TABLE `hazard_point_device` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `hazard_point_id` BIGINT NOT NULL COMMENT '隐患点ID',
  `hazard_point_code` VARCHAR(100) NOT NULL COMMENT '隐患点编号',
  `hazard_point_name` VARCHAR(200) NOT NULL COMMENT '隐患点名称',
  `device_id` BIGINT NOT NULL COMMENT '设备ID',
  `device_code` VARCHAR(100) NOT NULL COMMENT '设备编号',
  `device_name` VARCHAR(200) NOT NULL COMMENT '设备名称',
  `bind_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
  `unbind_time` DATETIME DEFAULT NULL COMMENT '解绑时间',
  `is_active` TINYINT DEFAULT 1 COMMENT '是否有效: 0-无效, 1-有效',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_hazard_device` (`hazard_point_id`, `device_id`),
  KEY `idx_hazard_point_id` (`hazard_point_id`),
  KEY `idx_device_id` (`device_id`),
  KEY `idx_is_active` (`is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='隐患点绑定设备表';

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
  `monitor_type_id` BIGINT DEFAULT NULL COMMENT '监测类型ID',
  `monitor_type_code` VARCHAR(50) DEFAULT NULL COMMENT '监测类型编码',
  `monitor_type_name` VARCHAR(200) DEFAULT NULL COMMENT '监测类型名称',
  `monitor_content_code` VARCHAR(50) DEFAULT NULL COMMENT '监测内容编码',
  `monitor_content_name` VARCHAR(100) DEFAULT NULL COMMENT '监测内容名称',
  `dimension` VARCHAR(50) DEFAULT NULL COMMENT '维度: x,y,z或空',
  `threshold_type` TINYINT DEFAULT 1 COMMENT '阈值类型: 1-绝对值, 2-百分比, 3-变化率',
  `threshold_value` DECIMAL(20,4) NOT NULL COMMENT '阈值',
  `threshold_min` DECIMAL(20,4) DEFAULT NULL COMMENT '阈值最小值',
  `threshold_max` DECIMAL(20,4) DEFAULT NULL COMMENT '阈值最大值',
  `alarm_level` TINYINT NOT NULL COMMENT '告警等级: 1-蓝色/提示, 2-黄色/一般, 3-橙色/严重, 4-红色/紧急',
  `alarm_color` VARCHAR(20) DEFAULT NULL COMMENT '告警颜色',
  `is_enabled` TINYINT DEFAULT 1 COMMENT '是否启用: 0-禁用, 1-启用',
  `sort_order` INT DEFAULT 0 COMMENT '排序号',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `del_flag` TINYINT DEFAULT 0 COMMENT '删除标记: 0-正常, 1-删除',
  PRIMARY KEY (`id`),
  KEY `idx_hazard_point_id` (`hazard_point_id`),
  KEY `idx_monitor_type_id` (`monitor_type_id`),
  KEY `idx_alarm_level` (`alarm_level`),
  KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警判据表';

-- ---------------------------------------------------------------
-- 4.2 告警分发规则表
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `alarm_dispatch_rule`;
CREATE TABLE `alarm_dispatch_rule` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `hazard_point_id` BIGINT NOT NULL COMMENT '隐患点ID',
  `name` VARCHAR(200) NOT NULL COMMENT '规则名称',
  `rule_type` TINYINT DEFAULT 1 COMMENT '规则类型: 1-告警, 2-离线',
  `alarm_level` TINYINT NOT NULL COMMENT '告警等级: 1-蓝色, 2-黄色, 3-橙色, 4-红色',
  `channel` TINYINT NOT NULL COMMENT '通知渠道: 1-短信, 2-邮件, 3-微信, 4-系统消息',
  `recipient_type` TINYINT DEFAULT 1 COMMENT '接收人类型: 1-用户, 2-角色',
  `recipient_id` BIGINT NOT NULL COMMENT '接收人ID',
  `recipient_name` VARCHAR(100) NOT NULL COMMENT '接收人姓名',
  `is_enabled` TINYINT DEFAULT 1 COMMENT '是否启用: 0-禁用, 1-启用',
  `delay_seconds` INT DEFAULT 0 COMMENT '延迟发送秒数',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `del_flag` TINYINT DEFAULT 0 COMMENT '删除标记: 0-正常, 1-删除',
  PRIMARY KEY (`id`),
  KEY `idx_hazard_point_id` (`hazard_point_id`),
  KEY `idx_alarm_level` (`alarm_level`),
  KEY `idx_channel` (`channel`),
  KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警分发规则表';

-- ---------------------------------------------------------------
-- 4.3 告警记录表
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `alarm_record`;
CREATE TABLE `alarm_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `alarm_no` VARCHAR(100) NOT NULL COMMENT '告警编号',
  `hazard_point_id` BIGINT NOT NULL COMMENT '隐患点ID',
  `hazard_point_name` VARCHAR(200) DEFAULT NULL COMMENT '隐患点名称',
  `device_id` BIGINT DEFAULT NULL COMMENT '设备ID',
  `device_code` VARCHAR(100) DEFAULT NULL COMMENT '设备编号',
  `device_name` VARCHAR(200) DEFAULT NULL COMMENT '设备名称',
  `sensor_id` BIGINT DEFAULT NULL COMMENT '传感器ID',
  `sensor_code` VARCHAR(100) DEFAULT NULL COMMENT '传感器编号',
  `monitor_type_id` BIGINT DEFAULT NULL COMMENT '监测类型ID',
  `monitor_content_code` VARCHAR(50) DEFAULT NULL COMMENT '监测内容编码',
  `alarm_level` TINYINT NOT NULL COMMENT '告警等级: 1-蓝色, 2-黄色, 3-橙色, 4-红色',
  `alarm_type` TINYINT DEFAULT 1 COMMENT '告警类型: 1-阈值告警, 2-变化率告警, 3-离线告警',
  `alarm_value` DECIMAL(20,4) DEFAULT NULL COMMENT '告警值',
  `threshold_value` DECIMAL(20,4) DEFAULT NULL COMMENT '阈值',
  `alarm_content` VARCHAR(1000) DEFAULT NULL COMMENT '告警内容',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 1-未处理, 2-处理中, 3-已处理, 4-已关闭',
  `dispose_user_id` BIGINT DEFAULT NULL COMMENT '处置人ID',
  `dispose_user_name` VARCHAR(100) DEFAULT NULL COMMENT '处置人姓名',
  `dispose_time` DATETIME DEFAULT NULL COMMENT '处置时间',
  `dispose_result` VARCHAR(1000) DEFAULT NULL COMMENT '处置结果',
  `dispose_images` TEXT DEFAULT NULL COMMENT '处置图片JSON',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_alarm_no` (`alarm_no`),
  KEY `idx_hazard_point_id` (`hazard_point_id`),
  KEY `idx_device_id` (`device_id`),
  KEY `idx_alarm_level` (`alarm_level`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警记录表';

-- ---------------------------------------------------------------
-- 4.4 告警通知记录表
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `alarm_notification`;
CREATE TABLE `alarm_notification` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `alarm_id` BIGINT NOT NULL COMMENT '告警ID',
  `alarm_no` VARCHAR(100) NOT NULL COMMENT '告警编号',
  `channel` TINYINT NOT NULL COMMENT '通知渠道: 1-短信, 2-邮件, 3-微信, 4-系统消息',
  `recipient_id` BIGINT NOT NULL COMMENT '接收人ID',
  `recipient_name` VARCHAR(100) NOT NULL COMMENT '接收人姓名',
  `recipient_phone` VARCHAR(20) DEFAULT NULL COMMENT '接收人手机号',
  `content` VARCHAR(2000) DEFAULT NULL COMMENT '通知内容',
  `send_status` TINYINT DEFAULT 0 COMMENT '发送状态: 0-待发送, 1-发送中, 2-发送成功, 3-发送失败',
  `send_time` DATETIME DEFAULT NULL COMMENT '发送时间',
  `fail_reason` VARCHAR(500) DEFAULT NULL COMMENT '失败原因',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_alarm_id` (`alarm_id`),
  KEY `idx_channel` (`channel`),
  KEY `idx_send_status` (`send_status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警通知记录表';

-- ===============================================================
-- 五、监测数据模块
-- ===============================================================

-- ---------------------------------------------------------------
-- 5.1 监测数据主表（按天分表建议）
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `monitor_data`;
CREATE TABLE `monitor_data` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `sensor_id` BIGINT NOT NULL COMMENT '传感器ID',
  `sensor_code` VARCHAR(100) NOT NULL COMMENT '传感器编号',
  `device_id` BIGINT NOT NULL COMMENT '设备ID',
  `device_code` VARCHAR(100) NOT NULL COMMENT '设备编号',
  `hazard_point_id` BIGINT DEFAULT NULL COMMENT '隐患点ID',
  `monitor_type_id` BIGINT NOT NULL COMMENT '监测类型ID',
  `monitor_type_code` VARCHAR(50) NOT NULL COMMENT '监测类型编码',
  `monitor_content_code` VARCHAR(50) NOT NULL COMMENT '监测内容编码',
  `value` DECIMAL(20,4) NOT NULL COMMENT '监测值',
  `value_x` DECIMAL(20,4) DEFAULT NULL COMMENT 'X轴值(位移类)',
  `value_y` DECIMAL(20,4) DEFAULT NULL COMMENT 'Y轴值(位移类)',
  `value_z` DECIMAL(20,4) DEFAULT NULL COMMENT 'Z轴值(位移类)',
  `unit` VARCHAR(20) DEFAULT NULL COMMENT '单位',
  `quality` TINYINT DEFAULT 1 COMMENT '数据质量: 1-good, 2-normal, 3-bad',
  `is_anomaly` TINYINT DEFAULT 0 COMMENT '是否异常: 0-正常, 1-异常',
  `anomaly_reason` VARCHAR(200) DEFAULT NULL COMMENT '异常原因',
  `report_time` DATETIME NOT NULL COMMENT '数据上报时间',
  `receive_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '数据接收时间',
  PRIMARY KEY (`id`),
  KEY `idx_sensor_id` (`sensor_id`),
  KEY `idx_device_id` (`device_id`),
  KEY `idx_hazard_point_id` (`hazard_point_id`),
  KEY `idx_monitor_type_id` (`monitor_type_id`),
  KEY `idx_monitor_content_code` (`monitor_content_code`),
  KEY `idx_report_time` (`report_time`),
  KEY `idx_is_anomaly` (`is_anomaly`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='监测数据主表'
PARTITION BY RANGE (YEAR(report_time) * 100 + MONTH(report_time)) (
  PARTITION p202401 VALUES LESS THAN (202402),
  PARTITION p202402 VALUES LESS THAN (202403),
  PARTITION p202403 VALUES LESS THAN (202404),
  PARTITION p202404 VALUES LESS THAN (202405),
  PARTITION p202405 VALUES LESS THAN (202406),
  PARTITION p202406 VALUES LESS THAN (202407),
  PARTITION p202407 VALUES LESS THAN (202408),
  PARTITION p202408 VALUES LESS THAN (202409),
  PARTITION p202409 VALUES LESS THAN (202410),
  PARTITION p202410 VALUES LESS THAN (202411),
  PARTITION p202411 VALUES LESS THAN (202412),
  PARTITION p202412 VALUES LESS THAN (202501),
  PARTITION p_future VALUES LESS THAN MAXVALUE
);

-- ---------------------------------------------------------------
-- 5.2 设备状态记录表
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `device_status_log`;
CREATE TABLE `device_status_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `device_id` BIGINT NOT NULL COMMENT '设备ID',
  `device_code` VARCHAR(100) NOT NULL COMMENT '设备编号',
  `old_status` TINYINT DEFAULT NULL COMMENT '原状态',
  `new_status` TINYINT NOT NULL COMMENT '新状态',
  `old_run_status` TINYINT DEFAULT NULL COMMENT '原运行状态',
  `new_run_status` TINYINT NOT NULL COMMENT '新运行状态',
  `change_reason` VARCHAR(500) DEFAULT NULL COMMENT '变更原因',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_device_id` (`device_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备状态记录表';

-- ---------------------------------------------------------------
-- 5.3 传感器初始值历史表
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `sensor_initial_history`;
CREATE TABLE `sensor_initial_history` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `sensor_id` BIGINT NOT NULL COMMENT '传感器ID',
  `sensor_code` VARCHAR(100) NOT NULL COMMENT '传感器编号',
  `old_initial_value` DECIMAL(20,4) DEFAULT NULL COMMENT '原初始值',
  `new_initial_value` DECIMAL(20,4) NOT NULL COMMENT '新初始值',
  `old_initial_value_x` DECIMAL(20,4) DEFAULT NULL COMMENT '原X轴初始值',
  `new_initial_value_x` DECIMAL(20,4) DEFAULT NULL COMMENT '新X轴初始值',
  `old_initial_value_y` DECIMAL(20,4) DEFAULT NULL COMMENT '原Y轴初始值',
  `new_initial_value_y` DECIMAL(20,4) DEFAULT NULL COMMENT '新Y轴初始值',
  `old_initial_value_z` DECIMAL(20,4) DEFAULT NULL COMMENT '原Z轴初始值',
  `new_initial_value_z` DECIMAL(20,4) DEFAULT NULL COMMENT '新Z轴初始值',
  `change_reason` VARCHAR(500) DEFAULT NULL COMMENT '变更原因',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `creator` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  PRIMARY KEY (`id`),
  KEY `idx_sensor_id` (`sensor_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='传感器初始值历史表';

-- ===============================================================
-- 六、报告报表模块
-- ===============================================================

-- ---------------------------------------------------------------
-- 6.1 报告模板表
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `report_template`;
CREATE TABLE `report_template` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `code` VARCHAR(100) NOT NULL COMMENT '模板编码',
  `name` VARCHAR(200) NOT NULL COMMENT '模板名称',
  `template_type` TINYINT DEFAULT 1 COMMENT '模板类型: 1-日报, 2-周报, 3-月报, 4-年报, 5-专项报告',
  `content` MEDIUMTEXT DEFAULT NULL COMMENT '模板内容HTML',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '描述',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
  `sort_order` INT DEFAULT 0 COMMENT '排序号',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `del_flag` TINYINT DEFAULT 0 COMMENT '删除标记: 0-正常, 1-删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_template_type` (`template_type`),
  KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报告模板表';

-- ---------------------------------------------------------------
-- 6.2 报告记录表
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `report_record`;
CREATE TABLE `report_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `report_no` VARCHAR(100) NOT NULL COMMENT '报告编号',
  `template_id` BIGINT NOT NULL COMMENT '模板ID',
  `template_name` VARCHAR(200) DEFAULT NULL COMMENT '模板名称',
  `hazard_point_id` BIGINT DEFAULT NULL COMMENT '隐患点ID',
  `hazard_point_name` VARCHAR(200) DEFAULT NULL COMMENT '隐患点名称',
  `report_title` VARCHAR(500) DEFAULT NULL COMMENT '报告标题',
  `report_period_start` DATE DEFAULT NULL COMMENT '报告周期开始',
  `report_period_end` DATE DEFAULT NULL COMMENT '报告周期结束',
  `file_path` VARCHAR(500) DEFAULT NULL COMMENT 'PDF文件路径',
  `file_size` BIGINT DEFAULT NULL COMMENT '文件大小',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 1-生成中, 2-已完成, 3-失败',
  `generate_time` DATETIME DEFAULT NULL COMMENT '生成时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `del_flag` TINYINT DEFAULT 0 COMMENT '删除标记: 0-正常, 1-删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_report_no` (`report_no`),
  KEY `idx_template_id` (`template_id`),
  KEY `idx_hazard_point_id` (`hazard_point_id`),
  KEY `idx_status` (`status`),
  KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报告记录表';

-- ===============================================================
-- 七、初始化数据
-- ===============================================================

-- 初始化组织
INSERT INTO `sys_organization` (`code`, `name`, `parent_id`, `parent_ids`, `level`, `leader`, `phone`, `region`, `address`, `sort_order`, `status`) VALUES
('ROOT', '地质灾害监测预警中心', 0, '/0/', 1, '张三', '028-87654321', '四川成都', '成都市成华区龙潭寺', 1, 1),
('JSB', '技术部', 1, '/0/1/', 2, '李四', '028-87654322', '四川成都', '成都市成华区龙潭寺', 1, 1),
('JCB', '监测部', 1, '/0/1/', 2, '王五', '028-87654323', '四川成都', '成都市成华区龙潭寺', 2, 1),
('YWB', '运维部', 1, '/0/1/', 2, '赵六', '028-87654324', '四川成都', '成都市成华区龙潭寺', 3, 1);

-- 初始化角色
INSERT INTO `sys_role` (`code`, `name`, `description`, `data_scope`, `sort_order`, `status`) VALUES
('super_admin', '超级管理员', '系统超级管理员，拥有所有权限', 1, 1, 1),
('admin', '管理员', '系统管理员', 1, 2, 1),
('operator', '值班员', '负责日常监测和告警处置', 3, 3, 1),
('inspector', '巡检员', '负责设备巡检和维护', 3, 4, 1),
('readonly', '只读用户', '仅可查看数据', 4, 5, 1);

-- 初始化监测内容
INSERT INTO `monitor_content` (`code`, `name`, `unit`, `icon`, `description`, `sort_order`) VALUES
('wy', '位移', 'mm', 'wy', '地表或结构物位移监测', 1),
('wd', '温度', '℃', 'wd', '环境或物体温度监测', 2),
('jd', '角度', '°', 'jd', '结构物倾斜角度监测', 3),
('yl', '压力', 'MPa', 'yl', '土壤或岩体压力监测', 4),
('sw', '水位', 'm', 'sw', '河流、湖泊、水库水位监测', 5),
('jsd', '加速度', 'm/s^2', 'jsd', '结构物振动加速度监测', 6),
('hsl', '含水率', '%', 'hsl', '土壤水分含量监测', 7),
('ljl', '力矩', 'n/m^2', 'ljl', '结构物受力力矩监测', 8),
('zdl', '震动频率', 'Hz', 'zdl', '结构物振动频率监测', 9),
('dl', '电量', 'V', 'dl', '电池或电源电压监测', 10),
('dx', '断线', '1', 'dx', '断线状态监测', 11),
('sg', '声光', '1', 'sg', '声光报警状态监测', 12),
('sp', '视频', '1', 'sp', '视频监控状态', 13),
('jy', '降雨量', 'mm', 'jy', '降雨量监测', 14),
('dw', '地下水水位', 'm', 'dw', '地下水水位监测', 15);

-- 初始化隐患点分组
INSERT INTO `hazard_point_group` (`code`, `name`, `description`, `sort_order`) VALUES
('DEFAULT', '未知分组', '默认分组', 0),
('HIGH_RISK', '高风险区', '高风险隐患区域', 1),
('MEDIUM_RISK', '中风险区', '中风险隐患区域', 2),
('LOW_RISK', '低风险区', '低风险隐患区域', 3);

-- 初始化系统参数
INSERT INTO `sys_config` (`config_key`, `config_value`, `category`, `description`, `sort_order`) VALUES
('system.name', '地质灾害监测预警系统1.0', 'system', '系统名称', 1),
('system.logo', '/logo.png', 'system', '系统Logo', 2),
('system.copyright', 'ljstar-版权所有', 'system', '版权信息', 3),
('system.address', '四川成都成华区龙潭寺', 'system', '单位地址', 4),
('system.icp', '川ICP备12012345号', 'system', '备案号', 5),
('system.phone', '028-87654321', 'system', '联系电话', 6),
('alarm.data.retention.days', '365', 'alarm', '告警数据保留天数', 1),
('monitor.data.retention.days', '730', 'monitor', '监测数据保留天数', 1),
('log.retention.days', '365', 'system', '日志保留天数', 2);
