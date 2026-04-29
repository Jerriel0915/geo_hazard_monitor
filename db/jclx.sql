-- ===============================================================
-- 监测类型管理模块数据库设计
-- 地质灾害监测预警系统
-- ===============================================================

-- ---------------------------------------------------------------
-- 一、监测内容枚举表
-- 定义标准化的监测内容类型，为数据解析、阈值判断提供依据
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `monitor_content_enum`;
CREATE TABLE `monitor_content_enum` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `code` VARCHAR(50) NOT NULL COMMENT '监测内容编码',
  `name` VARCHAR(100) NOT NULL COMMENT '监测内容名称',
  `unit` VARCHAR(20) NOT NULL COMMENT '默认单位',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '描述',
  `sort_order` INT DEFAULT 0 COMMENT '排序号',
  `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '是否删除: 0-否, 1-是',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='监测内容枚举表';

-- 初始化监测内容枚举数据
INSERT INTO `monitor_content_enum` (`code`, `name`, `unit`, `description`, `sort_order`) VALUES
('temperature', '温度', '℃', '环境或物体温度监测', 1),
('humidity', '湿度', '%', '环境相对湿度监测', 2),
('rainfall', '雨量', 'mm', '降雨量监测', 3),
('displacement', '位移', 'mm', '地表或结构物位移监测', 4),
('velocity', '流速', 'm/s', '水流或气流速度监测', 5),
('water_level', '水位', 'm', '河流、湖泊、水库水位监测', 6),
('pressure', '压力', 'kPa', '土壤或岩体压力监测', 7),
('inclination', '倾角', '°', '结构物倾斜角度监测', 8),
('vibration', '振动', 'mm/s', '结构物振动监测', 9),
('noise', '噪音', 'dB', '环境噪音水平监测', 10),
('power', '电量', 'V', '电池或电源电压监测', 11),
('current', '电流', 'A', '电路电流监测', 12),
('soil_moisture', '土壤含水率', '%', '土壤水分含量监测', 13),
('gap', '裂缝', 'mm', '裂缝开合度监测', 14),
('subsidence', '沉降', 'mm', '地表或结构物沉降监测', 15);

-- ---------------------------------------------------------------
-- 二、监测类型主表
-- 定义物联网设备接入的标准化产品类型
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `monitor_type`;
CREATE TABLE `monitor_type` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `code` VARCHAR(50) NOT NULL COMMENT '监测类型编号',
  `name` VARCHAR(200) NOT NULL COMMENT '监测类型名称',
  `description` VARCHAR(1000) DEFAULT NULL COMMENT '描述',
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
  `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '是否作废: 0-正常, 1-已作废',
  `delete_time` DATETIME DEFAULT NULL COMMENT '作废时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_name` (`name`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='监测类型主表';

-- ---------------------------------------------------------------
-- 三、监测类型参数模型表
-- 定义每种监测类型的参数配置（监测内容、图标、维度、量程、单位）
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `monitor_type_param`;
CREATE TABLE `monitor_type_param` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `monitor_type_id` BIGINT NOT NULL COMMENT '监测类型ID',
  `monitor_content_code` VARCHAR(50) NOT NULL COMMENT '监测内容编码',
  `monitor_content_name` VARCHAR(100) NOT NULL COMMENT '监测内容名称',
  `icon` VARCHAR(500) DEFAULT NULL COMMENT '图标SVG',
  `dimension` VARCHAR(50) DEFAULT NULL COMMENT '维度: x,y,z或空',
  `range_min` DECIMAL(20,4) DEFAULT NULL COMMENT '量程最小值',
  `range_max` DECIMAL(20,4) DEFAULT NULL COMMENT '量程最大值',
  `unit` VARCHAR(20) DEFAULT NULL COMMENT '单位',
  `mutation_value` DECIMAL(20,4) DEFAULT 0 COMMENT '突变值',
  `mutation_threshold` DECIMAL(20,4) DEFAULT NULL COMMENT '突变阈值(变化率)',
  `sort_order` INT DEFAULT 0 COMMENT '排序号',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_monitor_type_id` (`monitor_type_id`),
  KEY `idx_monitor_content_code` (`monitor_content_code`),
  CONSTRAINT `fk_param_monitor_type` FOREIGN KEY (`monitor_type_id`) REFERENCES `monitor_type` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='监测类型参数模型表';

-- ---------------------------------------------------------------
-- 四、传感器表
-- 设备传感器，关联监测类型模板
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `sensor`;
CREATE TABLE `sensor` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `code` VARCHAR(100) NOT NULL COMMENT '传感器编号',
  `name` VARCHAR(200) NOT NULL COMMENT '传感器名称',
  `device_id` BIGINT NOT NULL COMMENT '所属设备ID',
  `monitor_type_id` BIGINT NOT NULL COMMENT '监测类型ID',
  `monitor_content_code` VARCHAR(50) NOT NULL COMMENT '监测内容编码',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 0-离线, 1-在线, 2-告警',
  `install_location` VARCHAR(500) DEFAULT NULL COMMENT '安装位置',
  `install_time` DATETIME DEFAULT NULL COMMENT '安装时间',
  `last_data_time` DATETIME DEFAULT NULL COMMENT '最后数据时间',
  `create_dept_id` BIGINT DEFAULT NULL COMMENT '创建部门ID',
  `create_dept_name` VARCHAR(200) DEFAULT NULL COMMENT '创建部门名称',
  `create_user_id` BIGINT DEFAULT NULL COMMENT '创建人员ID',
  `create_user_name` VARCHAR(100) DEFAULT NULL COMMENT '创建人员姓名',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '是否删除: 0-否, 1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_device_id` (`device_id`),
  KEY `idx_monitor_type_id` (`monitor_type_id`),
  KEY `idx_monitor_content_code` (`monitor_content_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='传感器表';

-- ---------------------------------------------------------------
-- 五、监测数据表
-- 存储传感器上报的监测数据
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `monitor_data`;
CREATE TABLE `monitor_data` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `sensor_id` BIGINT NOT NULL COMMENT '传感器ID',
  `sensor_code` VARCHAR(100) NOT NULL COMMENT '传感器编号',
  `device_id` BIGINT NOT NULL COMMENT '设备ID',
  `monitor_type_id` BIGINT NOT NULL COMMENT '监测类型ID',
  `monitor_content_code` VARCHAR(50) NOT NULL COMMENT '监测内容编码',
  `value` DECIMAL(20,4) NOT NULL COMMENT '监测值',
  `value_x` DECIMAL(20,4) DEFAULT NULL COMMENT 'X轴值(位移类)',
  `value_y` DECIMAL(20,4) DEFAULT NULL COMMENT 'Y轴值(位移类)',
  `value_z` DECIMAL(20,4) DEFAULT NULL COMMENT 'Z轴值(位移类)',
  `quality` VARCHAR(20) DEFAULT 'good' COMMENT '数据质量: good/normal/bad',
  `is_anomaly` TINYINT(1) DEFAULT 0 COMMENT '是否异常: 0-正常, 1-异常',
  `anomaly_reason` VARCHAR(200) DEFAULT NULL COMMENT '异常原因',
  `report_time` DATETIME NOT NULL COMMENT '数据上报时间',
  `receive_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '数据接收时间',
  PRIMARY KEY (`id`),
  KEY `idx_sensor_id` (`sensor_id`),
  KEY `idx_device_id` (`device_id`),
  KEY `idx_monitor_type_id` (`monitor_type_id`),
  KEY `idx_monitor_content_code` (`monitor_content_code`),
  KEY `idx_report_time` (`report_time`),
  KEY `idx_is_anomaly` (`is_anomaly`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='监测数据表';

-- ---------------------------------------------------------------
-- 六、视图：监测类型完整信息视图
-- ---------------------------------------------------------------
CREATE OR REPLACE VIEW `v_monitor_type_full` AS
SELECT
  mt.id,
  mt.code,
  mt.name,
  mt.description,
  mt.create_dept_name,
  mt.create_user_name,
  mt.create_time,
  mt.update_time,
  mt.is_deleted,
  mt.delete_time,
  GROUP_CONCAT(DISTINCT mtp.monitor_content_name ORDER BY mtp.sort_order SEPARATOR ', ') AS param_summary,
  COUNT(DISTINCT s.id) AS sensor_count
FROM monitor_type mt
LEFT JOIN monitor_type_param mtp ON mt.id = mtp.monitor_type_id
LEFT JOIN sensor s ON mt.id = s.monitor_type_id AND s.is_deleted = 0
GROUP BY mt.id;

-- ---------------------------------------------------------------
-- 七、视图：传感器实时状态视图
-- ---------------------------------------------------------------
CREATE OR REPLACE VIEW `v_sensor_realtime` AS
SELECT
  s.id,
  s.code,
  s.name,
  s.device_id,
  s.monitor_type_id,
  s.monitor_content_code,
  mce.name AS monitor_content_name,
  mce.unit,
  s.status,
  s.install_location,
  s.last_data_time,
  mt.name AS monitor_type_name,
  d.name AS device_name,
  d.location AS device_location
FROM sensor s
LEFT JOIN monitor_content_enum mce ON s.monitor_content_code = mce.code
LEFT JOIN monitor_type mt ON s.monitor_type_id = mt.id
LEFT JOIN device d ON s.device_id = d.id
WHERE s.is_deleted = 0;

-- ===============================================================
-- 存储过程：同步传感器数量到监测类型
-- ===============================================================
DROP PROCEDURE IF EXISTS `sp_sync_sensor_count`;
DELIMITER //
CREATE PROCEDURE `sp_sync_sensor_count`(IN p_monitor_type_id BIGINT)
BEGIN
  DECLARE v_count INT DEFAULT 0;
  SELECT COUNT(*) INTO v_count
  FROM sensor
  WHERE monitor_type_id = p_monitor_type_id AND is_deleted = 0;
  -- 由于sensor_count是视图计算字段，此存储过程预留备用
END //
DELIMITER ;

-- ===============================================================
-- 初始数据示例
-- ===============================================================
INSERT INTO `monitor_type` (`code`, `name`, `description`, `create_dept_name`, `create_user_name`) VALUES
('JCLX001', '雨量监测', '用于监测降雨量的标准化监测类型', '技术部', '张三'),
('JCLX002', '位移监测', '用于监测地表或结构物位移的标准化监测类型', '技术部', '李四'),
('JCLX003', '温湿度监测', '用于同时监测温度和湿度的标准化监测类型', '运维部', '王五'),
('JCLX004', '地表位移监测', '用于监测三维空间位移的标准化监测类型', '技术部', '赵六');

INSERT INTO `monitor_type_param` (`monitor_type_id`, `monitor_content_code`, `monitor_content_name`, `dimension`, `range_min`, `range_max`, `unit`, `mutation_value`, `sort_order`) VALUES
(1, 'rainfall', '雨量', '', 0, 500, 'mm', 50, 1),
(2, 'displacement', '位移', 'x,y', -100, 100, 'mm', 10, 1),
(3, 'temperature', '温度', '', -40, 80, '℃', 5, 1),
(3, 'humidity', '湿度', '', 0, 100, '%', 10, 2),
(4, 'displacement', '位移', 'x,y,z', -500, 500, 'mm', 20, 1);
