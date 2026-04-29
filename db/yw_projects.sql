-- ===============================================================
-- 隐患点管理模块数据库设计
-- 地质灾害监测预警系统
-- ===============================================================

-- ---------------------------------------------------------------
-- 一、隐患点分组表
-- 管理隐患点的分组信息
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `hazard_point_group`;
CREATE TABLE `hazard_point_group` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` VARCHAR(200) NOT NULL COMMENT '分组名称',
  `code` VARCHAR(100) DEFAULT NULL COMMENT '分组编码',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '分组描述',
  `sort_order` INT DEFAULT 0 COMMENT '排序号',
  `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '是否删除: 0-否, 1-是',
  `create_dept_id` BIGINT DEFAULT NULL COMMENT '创建部门ID',
  `create_dept_name` VARCHAR(200) DEFAULT NULL COMMENT '创建部门名称',
  `create_user_id` BIGINT DEFAULT NULL COMMENT '创建人员ID',
  `create_user_name` VARCHAR(100) DEFAULT NULL COMMENT '创建人员姓名',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='隐患点分组表';

INSERT INTO `hazard_point_group` (`name`, `code`, `description`, `sort_order`) VALUES
('未知分组', 'DEFAULT', '默认分组', 0),
('高风险区', 'HIGH_RISK', '高风险隐患区域', 1),
('中风险区', 'MEDIUM_RISK', '中风险隐患区域', 2),
('低风险区', 'LOW_RISK', '低风险隐患区域', 3);

-- ---------------------------------------------------------------
-- 二、隐患点状态类型表
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `hazard_point_status_type`;
CREATE TABLE `hazard_point_status_type` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `code` VARCHAR(20) NOT NULL COMMENT '状态编码',
  `name` VARCHAR(50) NOT NULL COMMENT '状态名称',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '状态描述',
  `color` VARCHAR(20) DEFAULT NULL COMMENT '显示颜色',
  `sort_order` INT DEFAULT 0 COMMENT '排序号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='隐患点状态类型表';

INSERT INTO `hazard_point_status_type` (`code`, `name`, `description`, `color`, `sort_order`) VALUES
('MONITORING', '监测中', '正在监测中', '#67C23A', 1),
('PAUSED', '停测中', '暂停监测', '#E6A23C', 2),
('COMPLETED', '已完结', '监测完结', '#909399', 3);

-- ---------------------------------------------------------------
-- 三、隐患点主表
-- 存储隐患点基本信息
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `hazard_point`;
CREATE TABLE `hazard_point` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `code` VARCHAR(100) NOT NULL COMMENT '隐患点编号',
  `name` VARCHAR(200) NOT NULL COMMENT '隐患点名称',
  `group_id` BIGINT DEFAULT NULL COMMENT '分组ID',
  `group_name` VARCHAR(200) DEFAULT NULL COMMENT '分组名称',
  `status` VARCHAR(20) DEFAULT 'MONITORING' COMMENT '状态: MONITORING/PAUSED/COMPLETED',
  `longitude` DECIMAL(12,6) DEFAULT NULL COMMENT '经度',
  `latitude` DECIMAL(12,6) DEFAULT NULL COMMENT '纬度',
  `altitude` DECIMAL(10,2) DEFAULT NULL COMMENT '海拔高度(米)',
  `location` VARCHAR(500) DEFAULT NULL COMMENT '位置描述',
  `address` VARCHAR(500) DEFAULT NULL COMMENT '详细地址',
  `boundary` TEXT DEFAULT NULL COMMENT '范围边界坐标JSON',
  `strike` DECIMAL(5,2) DEFAULT NULL COMMENT '走向角度(度)',
  `strike_arrow` TEXT DEFAULT NULL COMMENT '走向箭头坐标JSON',
  `description` VARCHAR(1000) DEFAULT NULL COMMENT '隐患点描述',
  `risk_level` VARCHAR(20) DEFAULT NULL COMMENT '风险等级: HIGH/MEDIUM/LOW',
  `create_dept_id` BIGINT DEFAULT NULL COMMENT '创建部门ID',
  `create_dept_name` VARCHAR(200) DEFAULT NULL COMMENT '创建部门名称',
  `create_user_id` BIGINT DEFAULT NULL COMMENT '创建人员ID',
  `create_user_name` VARCHAR(100) DEFAULT NULL COMMENT '创建人员姓名',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_dept_id` BIGINT DEFAULT NULL COMMENT '修改部门ID',
  `update_dept_name` VARCHAR(200) DEFAULT NULL COMMENT '修改部门名称',
  `update_user_id` BIGINT DEFAULT NULL COMMENT '修改人员ID',
  `update_user_name` VARCHAR(100) DEFAULT NULL COMMENT '修改人员姓名',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '是否删除: 0-否, 1-是',
  `delete_time` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_group_id` (`group_id`),
  KEY `idx_status` (`status`),
  KEY `idx_risk_level` (`risk_level`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='隐患点主表';

-- ---------------------------------------------------------------
-- 四、隐患点绑定设备表
-- 存储隐患点与设备的关联关系
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
  `is_active` TINYINT(1) DEFAULT 1 COMMENT '是否有效: 0-无效, 1-有效',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_hazard_device` (`hazard_point_id`, `device_id`),
  KEY `idx_hazard_point_id` (`hazard_point_id`),
  KEY `idx_device_id` (`device_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='隐患点绑定设备表';

-- ---------------------------------------------------------------
-- 五、告警判据表
-- 存储隐患点的告警阈值配置
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
  `threshold_type` VARCHAR(20) NOT NULL COMMENT '阈值类型: ABSOLUTE/PERCENT/RATE',
  `threshold_value` DECIMAL(20,4) NOT NULL COMMENT '阈值',
  `threshold_min` DECIMAL(20,4) DEFAULT NULL COMMENT '阈值最小值',
  `threshold_max` DECIMAL(20,4) DEFAULT NULL COMMENT '阈值最大值',
  `alarm_level` VARCHAR(20) NOT NULL COMMENT '告警等级: INFO/WARNING/CRITICAL',
  `alarm_color` VARCHAR(20) DEFAULT NULL COMMENT '告警颜色',
  `is_enabled` TINYINT(1) DEFAULT 1 COMMENT '是否启用: 0-禁用, 1-启用',
  `sort_order` INT DEFAULT 0 COMMENT '排序号',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_user_id` BIGINT DEFAULT NULL COMMENT '创建人员ID',
  `create_user_name` VARCHAR(100) DEFAULT NULL COMMENT '创建人员姓名',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_hazard_point_id` (`hazard_point_id`),
  KEY `idx_monitor_type_id` (`monitor_type_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警判据表';

-- ---------------------------------------------------------------
-- 六、告警分发规则表
-- 存储告警的分发配置
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `alarm_dispatch_rule`;
CREATE TABLE `alarm_dispatch_rule` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `hazard_point_id` BIGINT NOT NULL COMMENT '隐患点ID',
  `name` VARCHAR(200) NOT NULL COMMENT '规则名称',
  `alarm_level` VARCHAR(20) NOT NULL COMMENT '告警等级: INFO/WARNING/CRITICAL',
  `channel` VARCHAR(50) NOT NULL COMMENT '通知渠道: SMS/EMAIL/WECHAT/SYSTEM',
  `recipient_type` VARCHAR(20) NOT NULL COMMENT '接收人类型: USER/ROLE',
  `recipient_id` BIGINT NOT NULL COMMENT '接收人ID',
  `recipient_name` VARCHAR(100) NOT NULL COMMENT '接收人姓名',
  `is_enabled` TINYINT(1) DEFAULT 1 COMMENT '是否启用: 0-禁用, 1-启用',
  `delay_seconds` INT DEFAULT 0 COMMENT '延迟发送秒数',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_user_id` BIGINT DEFAULT NULL COMMENT '创建人员ID',
  `create_user_name` VARCHAR(100) DEFAULT NULL COMMENT '创建人员姓名',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_hazard_point_id` (`hazard_point_id`),
  KEY `idx_alarm_level` (`alarm_level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警分发规则表';

-- ---------------------------------------------------------------
-- 七、隐患点监测数据表
-- 存储隐患点汇总的监测数据
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `hazard_point_monitor_data`;
CREATE TABLE `hazard_point_monitor_data` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `hazard_point_id` BIGINT NOT NULL COMMENT '隐患点ID',
  `hazard_point_code` VARCHAR(100) NOT NULL COMMENT '隐患点编号',
  `device_id` BIGINT NOT NULL COMMENT '设备ID',
  `device_code` VARCHAR(100) NOT NULL COMMENT '设备编号',
  `sensor_id` BIGINT NOT NULL COMMENT '传感器ID',
  `sensor_code` VARCHAR(100) NOT NULL COMMENT '传感器编号',
  `monitor_content_code` VARCHAR(50) NOT NULL COMMENT '监测内容编码',
  `dimension` VARCHAR(50) DEFAULT NULL COMMENT '维度',
  `value` DECIMAL(20,4) NOT NULL COMMENT '当前值',
  `value_hour_change` DECIMAL(20,4) DEFAULT NULL COMMENT '小时变化量',
  `value_day_change` DECIMAL(20,4) DEFAULT NULL COMMENT '24小时变化量',
  `value_3day_change` DECIMAL(20,4) DEFAULT NULL COMMENT '72小时变化量',
  `unit` VARCHAR(20) DEFAULT NULL COMMENT '单位',
  `quality` VARCHAR(20) DEFAULT 'good' COMMENT '数据质量',
  `is_anomaly` TINYINT(1) DEFAULT 0 COMMENT '是否异常',
  `report_time` DATETIME NOT NULL COMMENT '数据时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_hazard_point_id` (`hazard_point_id`),
  KEY `idx_device_id` (`device_id`),
  KEY `idx_sensor_id` (`sensor_id`),
  KEY `idx_report_time` (`report_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='隐患点监测数据表';

-- ---------------------------------------------------------------
-- 八、视图：隐患点完整信息视图
-- ---------------------------------------------------------------
CREATE OR REPLACE VIEW `v_hazard_point_full` AS
SELECT
  hp.id,
  hp.code,
  hp.name,
  hp.group_id,
  hp.group_name,
  hp.status,
  hpst.name AS status_name,
  hpst.color AS status_color,
  hp.longitude,
  hp.latitude,
  hp.altitude,
  hp.location,
  hp.address,
  hp.boundary,
  hp.strike,
  hp.risk_level,
  hp.description,
  hp.create_dept_name,
  hp.create_user_name,
  hp.create_time,
  hp.update_time,
  hp.is_deleted,
  COUNT(DISTINCT hpd.device_id) AS device_count,
  GROUP_CONCAT(DISTINCT d.name ORDER BY d.name SEPARATOR ', ') AS device_names
FROM hazard_point hp
LEFT JOIN hazard_point_status_type hpst ON hp.status = hpst.code
LEFT JOIN hazard_point_device hpd ON hp.id = hpd.hazard_point_id AND hpd.is_active = 1
LEFT JOIN device d ON hpd.device_id = d.id
WHERE hp.is_deleted = 0
GROUP BY hp.id;

-- ---------------------------------------------------------------
-- 九、视图：隐患点设备绑定视图
-- ---------------------------------------------------------------
CREATE OR REPLACE VIEW `v_hazard_point_device` AS
SELECT
  hpd.id,
  hpd.hazard_point_id,
  hpd.hazard_point_name,
  hpd.device_id,
  hpd.device_code,
  hpd.device_name,
  hpd.bind_time,
  hpd.is_active,
  d.status AS device_status,
  d.run_status AS device_run_status,
  d.last_report_time
FROM hazard_point_device hpd
LEFT JOIN device d ON hpd.device_id = d.id
WHERE hpd.is_active = 1;

-- ---------------------------------------------------------------
-- 十、视图：告警判据配置视图
-- ---------------------------------------------------------------
CREATE OR REPLACE VIEW `v_alarm_criteria` AS
SELECT
  ac.*,
  hp.name AS hazard_point_name,
  hpst.name AS status_name,
  hpst.color AS status_color
FROM alarm_criteria ac
LEFT JOIN hazard_point hp ON ac.hazard_point_id = hp.id
LEFT JOIN hazard_point_status_type hpst ON hp.status = hpst.code;

-- ===============================================================
-- 存储过程：暂停监测（不删除数据）
-- ===============================================================
DROP PROCEDURE IF EXISTS `sp_pause_monitoring`;
DELIMITER //
CREATE PROCEDURE `sp_pause_monitoring`(IN p_hazard_point_id BIGINT)
BEGIN
  UPDATE hazard_point
  SET status = 'PAUSED',
      update_time = NOW()
  WHERE id = p_hazard_point_id;
END //
DELIMITER ;

-- ===============================================================
-- 存储过程：恢复监测
-- ===============================================================
DROP PROCEDURE IF EXISTS `sp_resume_monitoring`;
DELIMITER //
CREATE PROCEDURE `sp_resume_monitoring`(IN p_hazard_point_id BIGINT)
BEGIN
  UPDATE hazard_point
  SET status = 'MONITORING',
      update_time = NOW()
  WHERE id = p_hazard_point_id;
END //
DELIMITER ;

-- ===============================================================
-- 存储过程：完结隐患点
-- ===============================================================
DROP PROCEDURE IF EXISTS `sp_complete_hazard_point`;
DELIMITER //
CREATE PROCEDURE `sp_complete_hazard_point`(IN p_hazard_point_id BIGINT)
BEGIN
  UPDATE hazard_point
  SET status = 'COMPLETED',
      update_time = NOW()
  WHERE id = p_hazard_point_id;
END //
DELIMITER ;

-- ===============================================================
-- 初始数据示例
-- ===============================================================
INSERT INTO `hazard_point` (`code`, `name`, `group_id`, `group_name`, `status`, `longitude`, `latitude`, `location`, `address`, `strike`, `risk_level`, `description`, `create_dept_name`, `create_user_name`) VALUES
('HP001', '龙潭寺滑坡隐患点', 1, '未知分组', 'MONITORING', 104.156789, 30.678901, '龙潭寺镇北侧', '成都市成华区龙潭寺路', 45.5, 'HIGH', '该区域存在滑坡风险，需要重点监测', '技术部', '张三'),
('HP002', '青城山崩塌隐患点', 2, '高风险区', 'MONITORING', 103.589234, 30.891234, '青城山景区', '都江堰市青城山镇', 120.0, 'HIGH', '岩石崩塌风险较高', '监测部', '李四'),
('HP003', '瓦屋山泥石流隐患点', 3, '中风险区', 'MONITORING', 102.891234, 29.589234, '瓦屋山脚', '眉山市洪雅县瓦屋山镇', 90.0, 'MEDIUM', '雨季可能出现泥石流', '运维部', '王五'),
('HP004', '峨眉山边坡隐患点', 4, '低风险区', 'PAUSED', 103.334567, 29.556789, '峨眉山景区', '乐山市峨眉山市峨眉山', 180.0, 'LOW', '边坡稳定性较差', '技术部', '赵六');

INSERT INTO `alarm_criteria` (`hazard_point_id`, `name`, `monitor_type_code`, `monitor_type_name`, `monitor_content_code`, `monitor_content_name`, `threshold_type`, `threshold_value`, `alarm_level`, `alarm_color`, `is_enabled`) VALUES
(1, '雨量告警', 'JCLX001', '雨量监测', 'rainfall', '雨量', 'ABSOLUTE', 100, 'WARNING', '#E6A23C', 1),
(1, '位移X轴告警', 'JCLX002', '位移监测', 'displacement', '位移', 'RATE', 10, 'CRITICAL', '#F56C6C', 1),
(2, '倾角告警', 'JCLX005', '倾角监测', 'inclination', '倾角', 'ABSOLUTE', 15, 'WARNING', '#E6A23C', 1),
(3, '水位告警', 'JCLX004', '水位监测', 'water_level', '水位', 'ABSOLUTE', 50, 'INFO', '#67C23A', 1);

INSERT INTO `alarm_dispatch_rule` (`hazard_point_id`, `name`, `alarm_level`, `channel`, `recipient_type`, `recipient_id`, `recipient_name`, `is_enabled`) VALUES
(1, '重大告警通知', 'CRITICAL', 'SMS', 'USER', 1, '张三', 1),
(1, '重大告警通知', 'CRITICAL', 'WECHAT', 'USER', 1, '张三', 1),
(1, '一般告警通知', 'WARNING', 'SYSTEM', 'ROLE', 2, '监测员', 1),
(2, '告警通知', 'WARNING', 'EMAIL', 'USER', 2, '李四', 1);
