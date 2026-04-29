-- ===============================================================
-- 视频设备管理模块数据库设计
-- 地质灾害监测预警系统
-- ===============================================================

-- ---------------------------------------------------------------
-- 一、视频设备协议类型表
-- 定义支持的视频协议类型
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `video_protocol_type`;
CREATE TABLE `video_protocol_type` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `code` VARCHAR(20) NOT NULL COMMENT '协议编码',
  `name` VARCHAR(50) NOT NULL COMMENT '协议名称',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '协议描述',
  `sort_order` INT DEFAULT 0 COMMENT '排序号',
  `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '是否删除: 0-否, 1-是',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频设备协议类型表';

-- 初始化协议类型数据
INSERT INTO `video_protocol_type` (`code`, `name`, `description`, `sort_order`) VALUES
('RTMP', 'RTMP', '实时消息传输协议，用于实时视频流传输', 1),
('RTSP', 'RTSP', '实时流协议，用于控制流媒体服务器', 2),
('ONVIF', 'ONVIF', '开放网络视频接口论坛标准协议', 3);

-- ---------------------------------------------------------------
-- 二、视频设备主表
-- 存储视频设备基本信息
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `video_device`;
CREATE TABLE `video_device` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `code` VARCHAR(100) NOT NULL COMMENT '设备编号',
  `name` VARCHAR(200) NOT NULL COMMENT '设备名称',
  `protocol_code` VARCHAR(20) NOT NULL COMMENT '协议编码(RTMP/RTSP/ONVIF)',
  `stream_url` VARCHAR(1000) NOT NULL COMMENT '视频流地址',
  `hazard_point_id` BIGINT DEFAULT NULL COMMENT '所属隐患点ID',
  `hazard_point_name` VARCHAR(200) DEFAULT NULL COMMENT '所属隐患点名称',
  `ip_address` VARCHAR(50) DEFAULT NULL COMMENT '设备IP地址',
  `port` INT DEFAULT NULL COMMENT '端口号',
  `username` VARCHAR(100) DEFAULT NULL COMMENT '登录用户名',
  `password` VARCHAR(200) DEFAULT NULL COMMENT '登录密码(加密存储)',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 0-离线, 1-在线, 2-故障',
  `location` VARCHAR(500) DEFAULT NULL COMMENT '安装位置',
  `install_time` DATETIME DEFAULT NULL COMMENT '安装时间',
  `last_online_time` DATETIME DEFAULT NULL COMMENT '最后在线时间',
  `create_dept_id` BIGINT DEFAULT NULL COMMENT '创建部门ID',
  `create_dept_name` VARCHAR(200) DEFAULT NULL COMMENT '创建部门名称',
  `create_user_id` BIGINT DEFAULT NULL COMMENT '创建人员ID',
  `create_user_name` VARCHAR(100) DEFAULT NULL COMMENT '创建人员姓名',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '是否删除: 0-否, 1-是',
  `delete_time` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_protocol_code` (`protocol_code`),
  KEY `idx_hazard_point_id` (`hazard_point_id`),
  KEY `idx_status` (`status`),
  KEY `idx_is_deleted` (`is_deleted`),
  CONSTRAINT `fk_video_protocol` FOREIGN KEY (`protocol_code`) REFERENCES `video_protocol_type` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频设备主表';

-- ---------------------------------------------------------------
-- 三、视频截图记录表
-- 存储视频截图记录，用于告警处置附件
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
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注说明',
  `create_user_id` BIGINT DEFAULT NULL COMMENT '操作用户ID',
  `create_user_name` VARCHAR(100) DEFAULT NULL COMMENT '操作用户姓名',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_video_device_id` (`video_device_id`),
  KEY `idx_snapshot_time` (`snapshot_time`),
  CONSTRAINT `fk_snapshot_video_device` FOREIGN KEY (`video_device_id`) REFERENCES `video_device` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频截图记录表';

-- ---------------------------------------------------------------
-- 四、视图：视频设备完整信息视图
-- ---------------------------------------------------------------
CREATE OR REPLACE VIEW `v_video_device_full` AS
SELECT
  vd.id,
  vd.code,
  vd.name,
  vd.protocol_code,
  vpt.name AS protocol_name,
  vd.stream_url,
  vd.hazard_point_id,
  vd.hazard_point_name,
  vd.ip_address,
  vd.port,
  vd.status,
  vd.location,
  vd.install_time,
  vd.last_online_time,
  vd.create_dept_name,
  vd.create_user_name,
  vd.create_time,
  vd.update_time,
  vd.is_deleted
FROM video_device vd
LEFT JOIN video_protocol_type vpt ON vd.protocol_code = vpt.code
WHERE vd.is_deleted = 0;

-- ---------------------------------------------------------------
-- 五、视图：隐患点关联视频设备视图
-- ---------------------------------------------------------------
CREATE OR REPLACE VIEW `v_hazard_point_video` AS
SELECT
  hp.id AS hazard_point_id,
  hp.code AS hazard_point_code,
  hp.name AS hazard_point_name,
  vd.id AS video_device_id,
  vd.code AS video_device_code,
  vd.name AS video_device_name,
  vd.protocol_code,
  vd.stream_url,
  vd.status
FROM hazard_point hp
LEFT JOIN video_device vd ON hp.id = vd.hazard_point_id AND vd.is_deleted = 0;

-- ===============================================================
-- 存储过程：验证视频流地址有效性
-- ===============================================================
DROP PROCEDURE IF EXISTS `sp_validate_video_stream`;
DELIMITER //
CREATE PROCEDURE `sp_validate_video_stream`(
  IN p_stream_url VARCHAR(1000),
  IN p_protocol_code VARCHAR(20),
  OUT p_result INT,
  OUT p_message VARCHAR(500)
)
BEGIN
  SET p_result = 0;
  SET p_message = '验证中...';
  -- 实际实现需要调用外部服务或使用网络函数验证
  -- 这里作为预留存储过程
  SET p_result = 1;
  SET p_message = '视频流地址格式正确';
END //
DELIMITER ;

-- ===============================================================
-- 初始数据示例
-- ===============================================================
INSERT INTO `video_device` (`code`, `name`, `protocol_code`, `stream_url`, `hazard_point_name`, `ip_address`, `status`, `location`, `install_time`, `create_dept_name`, `create_user_name`) VALUES
('VD001', '隐患点A-摄像头1', 'RTSP', 'rtsp://admin:123456@192.168.1.101:554/Streaming/Channels/101', '隐患点A', '192.168.1.101', 1, '隐患点A区域', '2024-01-10 10:00:00', '运维部', '张三'),
('VD002', '隐患点A-摄像头2', 'RTMP', 'rtmp://192.168.1.102:1935/live/stream001', '隐患点A', '192.168.1.102', 1, '隐患点A入口', '2024-01-10 11:00:00', '运维部', '张三'),
('VD003', '隐患点B-摄像头1', 'ONVIF', 'http://192.168.1.103:8080/onvif/media', '隐患点B', '192.168.1.103', 2, '隐患点B区域', '2024-01-15 09:00:00', '运维部', '李四'),
('VD004', '隐患点C-摄像头1', 'RTSP', 'rtsp://admin:password@192.168.1.104:554/stream1', '隐患点C', '192.168.1.104', 0, '隐患点C边坡', '2024-01-20 14:00:00', '技术部', '王五');
