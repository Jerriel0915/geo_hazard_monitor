-- ===============================================================
-- 设备管理模块数据库设计
-- 地质灾害监测预警系统
-- ===============================================================

-- ---------------------------------------------------------------
-- 一、设备状态类型表
-- 定义设备的状态分类
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `device_status_type`;
CREATE TABLE `device_status_type` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `code` VARCHAR(20) NOT NULL COMMENT '状态编码',
  `name` VARCHAR(50) NOT NULL COMMENT '状态名称',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '状态描述',
  `sort_order` INT DEFAULT 0 COMMENT '排序号',
  `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '是否删除: 0-否, 1-是',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备状态类型表';

INSERT INTO `device_status_type` (`code`, `name`, `description`, `sort_order`) VALUES
('NORMAL', '正常', '设备正常运行', 1),
('FAULT', '故障', '设备发生故障', 2),
('REPAIR', '维修', '设备正在维修中', 3),
('OFFLINE', '离线', '设备已离线', 4);

-- ---------------------------------------------------------------
-- 二、设备主表
-- 存储物联网监测设备基本信息
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `device`;
CREATE TABLE `device` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `code` VARCHAR(100) NOT NULL COMMENT '设备编号',
  `name` VARCHAR(200) NOT NULL COMMENT '设备名称',
  `device_type` VARCHAR(50) DEFAULT NULL COMMENT '设备类型',
  `status` VARCHAR(20) DEFAULT 'NORMAL' COMMENT '设备状态: NORMAL/FAULT/REPAIR/OFFLINE',
  `run_status` TINYINT DEFAULT 1 COMMENT '运行状态: 0-离线, 1-在线',
  `hazard_point_ids` VARCHAR(500) DEFAULT NULL COMMENT '关联隐患点ID列表(多个用逗号分隔)',
  `hazard_point_names` VARCHAR(500) DEFAULT NULL COMMENT '关联隐患点名称列表',
  `ip_address` VARCHAR(50) DEFAULT NULL COMMENT '设备IP地址',
  `mac_address` VARCHAR(50) DEFAULT NULL COMMENT 'MAC地址',
  `manufacturer` VARCHAR(200) DEFAULT NULL COMMENT '生产厂家',
  `model` VARCHAR(100) DEFAULT NULL COMMENT '设备型号',
  `install_location` VARCHAR(500) DEFAULT NULL COMMENT '安装位置',
  `install_time` DATETIME DEFAULT NULL COMMENT '安装时间',
  `last_report_time` DATETIME DEFAULT NULL COMMENT '最近上报时间',
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
  KEY `idx_status` (`status`),
  KEY `idx_run_status` (`run_status`),
  KEY `idx_hazard_point_ids` (`hazard_point_ids`),
  KEY `idx_is_deleted` (`is_deleted`),
  KEY `idx_last_report_time` (`last_report_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备主表';

-- ---------------------------------------------------------------
-- 三、设备传感器表
-- 存储设备挂载的传感器配置信息
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `device_sensor`;
CREATE TABLE `device_sensor` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `device_id` BIGINT NOT NULL COMMENT '设备ID',
  `device_code` VARCHAR(100) NOT NULL COMMENT '设备编号',
  `sensor_code` VARCHAR(100) NOT NULL COMMENT '传感器编号',
  `sensor_name` VARCHAR(200) NOT NULL COMMENT '传感器名称',
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
  `mutation_value` DECIMAL(20,4) DEFAULT 0 COMMENT '突变值',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
  `install_location` VARCHAR(500) DEFAULT NULL COMMENT '安装位置',
  `install_time` DATETIME DEFAULT NULL COMMENT '安装时间',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_user_id` BIGINT DEFAULT NULL COMMENT '创建人员ID',
  `create_user_name` VARCHAR(100) DEFAULT NULL COMMENT '创建人员姓名',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_sensor` (`device_id`, `sensor_code`),
  KEY `idx_device_id` (`device_id`),
  KEY `idx_monitor_type_id` (`monitor_type_id`),
  KEY `idx_monitor_content_code` (`monitor_content_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备传感器表';

-- ---------------------------------------------------------------
-- 四、设备传感器历史初始值表
-- 记录传感器初始值的变更历史
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `device_sensor_history`;
CREATE TABLE `device_sensor_history` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `device_sensor_id` BIGINT NOT NULL COMMENT '设备传感器ID',
  `device_code` VARCHAR(100) NOT NULL COMMENT '设备编号',
  `sensor_code` VARCHAR(100) NOT NULL COMMENT '传感器编号',
  `initial_value` DECIMAL(20,4) NOT NULL COMMENT '历史初始值',
  `initial_value_x` DECIMAL(20,4) DEFAULT NULL COMMENT 'X轴历史初始值',
  `initial_value_y` DECIMAL(20,4) DEFAULT NULL COMMENT 'Y轴历史初始值',
  `initial_value_z` DECIMAL(20,4) DEFAULT NULL COMMENT 'Z轴历史初始值',
  `change_reason` VARCHAR(500) DEFAULT NULL COMMENT '变更原因',
  `change_user_id` BIGINT DEFAULT NULL COMMENT '变更人员ID',
  `change_user_name` VARCHAR(100) DEFAULT NULL COMMENT '变更人员姓名',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_device_sensor_id` (`device_sensor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备传感器历史初始值表';

-- ---------------------------------------------------------------
-- 五、设备历史数据表（时序数据）
-- 存储设备传感器上报的时序数据
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `device_data`;
CREATE TABLE `device_data` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `device_id` BIGINT NOT NULL COMMENT '设备ID',
  `device_code` VARCHAR(100) NOT NULL COMMENT '设备编号',
  `sensor_id` BIGINT NOT NULL COMMENT '传感器ID',
  `sensor_code` VARCHAR(100) NOT NULL COMMENT '传感器编号',
  `monitor_type_id` BIGINT NOT NULL COMMENT '监测类型ID',
  `monitor_type_code` VARCHAR(50) NOT NULL COMMENT '监测类型编码',
  `monitor_content_code` VARCHAR(50) NOT NULL COMMENT '监测内容编码',
  `value` DECIMAL(20,4) NOT NULL COMMENT '监测值',
  `value_x` DECIMAL(20,4) DEFAULT NULL COMMENT 'X轴值',
  `value_y` DECIMAL(20,4) DEFAULT NULL COMMENT 'Y轴值',
  `value_z` DECIMAL(20,4) DEFAULT NULL COMMENT 'Z轴值',
  `unit` VARCHAR(20) DEFAULT NULL COMMENT '单位',
  `quality` VARCHAR(20) DEFAULT 'good' COMMENT '数据质量: good/normal/bad',
  `is_anomaly` TINYINT(1) DEFAULT 0 COMMENT '是否异常: 0-正常, 1-异常',
  `anomaly_reason` VARCHAR(200) DEFAULT NULL COMMENT '异常原因',
  `report_time` DATETIME NOT NULL COMMENT '上报时间',
  `receive_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '接收时间',
  PRIMARY KEY (`id`),
  KEY `idx_device_id` (`device_id`),
  KEY `idx_sensor_id` (`sensor_id`),
  KEY `idx_monitor_type_code` (`monitor_type_code`),
  KEY `idx_report_time` (`report_time`),
  KEY `idx_device_report` (`device_id`, `report_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备历史数据表';

-- ---------------------------------------------------------------
-- 六、设备与隐患点关联表
-- 存储设备与隐患点的关联关系
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `device_hazard_point`;
CREATE TABLE `device_hazard_point` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `device_id` BIGINT NOT NULL COMMENT '设备ID',
  `device_code` VARCHAR(100) NOT NULL COMMENT '设备编号',
  `hazard_point_id` BIGINT NOT NULL COMMENT '隐患点ID',
  `hazard_point_code` VARCHAR(100) NOT NULL COMMENT '隐患点编号',
  `hazard_point_name` VARCHAR(200) NOT NULL COMMENT '隐患点名称',
  `bind_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
  `unbind_time` DATETIME DEFAULT NULL COMMENT '解绑时间',
  `is_active` TINYINT(1) DEFAULT 1 COMMENT '是否有效: 0-无效, 1-有效',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_hazard` (`device_id`, `hazard_point_id`),
  KEY `idx_device_id` (`device_id`),
  KEY `idx_hazard_point_id` (`hazard_point_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备与隐患点关联表';

-- ---------------------------------------------------------------
-- 七、视图：设备完整信息视图
-- ---------------------------------------------------------------
CREATE OR REPLACE VIEW `v_device_full` AS
SELECT
  d.id,
  d.code,
  d.name,
  d.device_type,
  d.status,
  dst.name AS status_name,
  d.run_status,
  d.hazard_point_ids,
  d.hazard_point_names,
  d.ip_address,
  d.mac_address,
  d.manufacturer,
  d.model,
  d.install_location,
  d.install_time,
  d.last_report_time,
  d.create_dept_name,
  d.create_user_name,
  d.create_time,
  d.update_time,
  d.is_deleted,
  COUNT(DISTINCT ds.id) AS sensor_count,
  GROUP_CONCAT(DISTINCT mt.name ORDER BY mt.name SEPARATOR ', ') AS monitor_types
FROM device d
LEFT JOIN device_status_type dst ON d.status = dst.code
LEFT JOIN device_sensor ds ON d.id = ds.device_id AND ds.status = 1
LEFT JOIN monitor_type mt ON ds.monitor_type_id = mt.id
WHERE d.is_deleted = 0
GROUP BY d.id;

-- ---------------------------------------------------------------
-- 八、视图：设备传感器实时状态视图
-- ---------------------------------------------------------------
CREATE OR REPLACE VIEW `v_device_sensor_realtime` AS
SELECT
  ds.id,
  ds.device_id,
  ds.device_code,
  ds.sensor_code,
  ds.sensor_name,
  ds.monitor_type_id,
  ds.monitor_type_name,
  ds.monitor_content_name,
  ds.dimension,
  ds.initial_value,
  ds.unit,
  ds.range_min,
  ds.range_max,
  ds.status,
  dd.value AS latest_value,
  dd.value_x AS latest_value_x,
  dd.value_y AS latest_value_y,
  dd.value_z AS latest_value_z,
  dd.report_time AS latest_report_time,
  dd.quality
FROM device_sensor ds
LEFT JOIN (
  SELECT sensor_id, value, value_x, value_y, value_z, report_time, quality,
         ROW_NUMBER() OVER (PARTITION BY sensor_id ORDER BY report_time DESC) as rn
  FROM device_data
) dd ON ds.id = dd.sensor_id AND dd.rn = 1
WHERE ds.status = 1;

-- ---------------------------------------------------------------
-- 九、存储过程：批量绑定隐患点
-- ---------------------------------------------------------------
DROP PROCEDURE IF EXISTS `sp_batch_bind_hazard_point`;
DELIMITER //
CREATE PROCEDURE `sp_batch_bind_hazard_point`(
  IN p_device_ids TEXT,
  IN p_hazard_point_id BIGINT,
  IN p_hazard_point_code VARCHAR(100),
  IN p_hazard_point_name VARCHAR(200),
  IN p_user_id BIGINT,
  IN p_user_name VARCHAR(100)
)
BEGIN
  DECLARE v_device_id BIGINT;
  DECLARE v_done INT DEFAULT FALSE;
  DECLARE v_cur CURSOR FOR SELECT id FROM JSON_TABLE(p_device_ids, '$[*]' COLUMNS (id BIGINT PATH '$')) AS dt;
  DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_done = TRUE;

  OPEN v_cur;
  read_loop: LOOP
    FETCH v_cur INTO v_device_id;
    IF v_done THEN
      LEAVE read_loop;
    END IF;

    INSERT INTO device_hazard_point (device_id, device_code, hazard_point_id, hazard_point_code, hazard_point_name)
    SELECT d.id, d.code, p_hazard_point_id, p_hazard_point_code, p_hazard_point_name
    FROM device d
    WHERE d.id = v_device_id
    ON DUPLICATE KEY UPDATE
      hazard_point_id = p_hazard_point_id,
      hazard_point_code = p_hazard_point_code,
      hazard_point_name = p_hazard_point_name,
      is_active = 1,
      unbind_time = NULL;

    UPDATE device_hazard_point
    SET is_active = 0, unbind_time = NOW()
    WHERE device_id = v_device_id AND hazard_point_id != p_hazard_point_id;

  END LOOP;
  CLOSE v_cur;
END //
DELIMITER ;

-- ---------------------------------------------------------------
-- 十、存储过程：设备复用（重新录入生成新编号）
-- ---------------------------------------------------------------
DROP PROCEDURE IF EXISTS `sp_device_reuse`;
DELIMITER //
CREATE PROCEDURE `sp_device_reuse`(
  IN p_old_device_id BIGINT,
  IN p_new_code VARCHAR(100),
  IN p_new_name VARCHAR(200),
  IN p_user_id BIGINT,
  IN p_user_name VARCHAR(100),
  OUT p_new_device_id BIGINT
)
BEGIN
  DECLARE v_old_device_id BIGINT;
  DECLARE v_new_id BIGINT;

  INSERT INTO device (
    code, name, device_type, status, run_status, hazard_point_ids, hazard_point_names,
    ip_address, mac_address, manufacturer, model, install_location, install_time,
    create_user_id, create_user_name
  )
  SELECT
    p_new_code, p_new_name, device_type, 'NORMAL', 0, hazard_point_ids, hazard_point_names,
    ip_address, mac_address, manufacturer, model, install_location, NOW(),
    p_user_id, p_user_name
  FROM device WHERE id = p_old_device_id;

  SET p_new_device_id = LAST_INSERT_ID();

  INSERT INTO device_sensor (
    device_id, device_code, sensor_code, sensor_name, monitor_type_id, monitor_type_code,
    monitor_type_name, monitor_content_code, monitor_content_name, dimension,
    initial_value, initial_value_x, initial_value_y, initial_value_z, unit,
    range_min, range_max, mutation_value, status, install_location, install_time,
    create_user_id, create_user_name
  )
  SELECT
    p_new_device_id, p_new_code, sensor_code, sensor_name, monitor_type_id, monitor_type_code,
    monitor_type_name, monitor_content_code, monitor_content_name, dimension,
    initial_value, initial_value_x, initial_value_y, initial_value_z, unit,
    range_min, range_max, mutation_value, 1, install_location, NOW(),
    p_user_id, p_user_name
  FROM device_sensor WHERE device_id = p_old_device_id;

END //
DELIMITER ;

-- ===============================================================
-- 初始数据示例
-- ===============================================================
INSERT INTO `device` (`code`, `name`, `device_type`, `status`, `run_status`, `hazard_point_ids`, `hazard_point_names`, `ip_address`, `install_location`, `install_time`, `last_report_time`, `create_dept_name`, `create_user_name`) VALUES
('DEV001', '雨量监测站-01', '监测站', 'NORMAL', 1, '1', '隐患点A', '192.168.1.101', '隐患点A区域', '2024-01-10 10:00:00', '2024-01-20 14:30:00', '运维部', '张三'),
('DEV002', '位移监测站-01', '监测站', 'NORMAL', 1, '1,2', '隐患点A,隐患点B', '192.168.1.102', '隐患点B区域', '2024-01-12 11:00:00', '2024-01-20 14:25:00', '技术部', '李四'),
('DEV003', '温湿度监测站-01', '监测站', 'FAULT', 0, '2', '隐患点B', '192.168.1.103', '隐患点B入口', '2024-01-15 09:00:00', '2024-01-19 10:00:00', '运维部', '王五'),
('DEV004', '综合监测站-01', '监测站', 'REPAIR', 0, '3', '隐患点C', '192.168.1.104', '隐患点C边坡', '2024-01-18 14:00:00', NULL, '技术部', '赵六');

INSERT INTO `device_sensor` (`device_id`, `device_code`, `sensor_code`, `sensor_name`, `monitor_type_id`, `monitor_type_code`, `monitor_type_name`, `monitor_content_code`, `monitor_content_name`, `dimension`, `initial_value`, `unit`, `range_min`, `range_max`, `mutation_value`, `status`) VALUES
(1, 'DEV001', 'SENSOR001', '雨量传感器', 1, 'JCLX001', '雨量监测', 'rainfall', '雨量', '', 0, 'mm', 0, 500, 50, 1),
(2, 'DEV002', 'SENSOR002', '位移传感器-X', 2, 'JCLX002', '位移监测', 'displacement', '位移', 'x', 0, 'mm', -100, 100, 10, 1),
(2, 'DEV002', 'SENSOR003', '位移传感器-Y', 2, 'JCLX002', '位移监测', 'displacement', '位移', 'y', 0, 'mm', -100, 100, 10, 1),
(3, 'DEV003', 'SENSOR004', '温度传感器', 3, 'JCLX003', '温湿度监测', 'temperature', '温度', '', 25, '℃', -40, 80, 5, 1),
(3, 'DEV003', 'SENSOR005', '湿度传感器', 3, 'JCLX003', '温湿度监测', 'humidity', '湿度', '', 60, '%', 0, 100, 10, 1);
