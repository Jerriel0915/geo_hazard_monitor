-- ============================================================
-- 通知系统多通道扩展 — 模板 + 实例 + 目标 三表模型
-- 用途：支撑告警、业务、系统三类通知的模板匹配、多通道分发、
--       发送状态跟踪与生命周期管理
-- 状态：Phase 3 仅创建表结构，通道实现后续迭代
-- ============================================================

-- 通知模板表（管理所有通知类型及其推送配置）
CREATE TABLE IF NOT EXISTS sys_notify_template (
    template_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_code VARCHAR(64) NOT NULL UNIQUE COMMENT '模板编码: alarm_threshold / device_offline / system_maintenance',
    template_name VARCHAR(100) NOT NULL        COMMENT '模板名称',
    notify_type   VARCHAR(32) NOT NULL         COMMENT '通知分类: alarm / business / system',
    title_tpl     VARCHAR(255)                 COMMENT '标题模板，支持 {变量} 替换',
    content_tpl   TEXT                         COMMENT '内容模板，支持 {变量} 替换',
    channels      VARCHAR(128) DEFAULT 'in_app' COMMENT '推送通道列表，逗号分隔: in_app,email,sms',
    priority      TINYINT DEFAULT 0            COMMENT '优先级: 0=普通 1=重要 2=紧急',
    status        CHAR(1) DEFAULT '0'          COMMENT '状态: 0=启用 1=禁用',
    create_by     VARCHAR(64) DEFAULT ''       COMMENT '创建者',
    create_time   DATETIME                     COMMENT '创建时间',
    update_by     VARCHAR(64) DEFAULT ''       COMMENT '更新者',
    update_time   DATETIME                     COMMENT '更新时间',
    remark        VARCHAR(255)                 COMMENT '备注',
    INDEX idx_code (template_code),
    INDEX idx_type (notify_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知模板';

-- 预置告警通知模板（示例）
INSERT INTO sys_notify_template (template_code, template_name, notify_type, title_tpl, content_tpl, channels, priority, status) VALUES
('alarm_threshold', '监测数据超阈值告警', 'alarm',
 '{hazardPointName}监测{attrName}超过阈值',
 '{hazardPointName}（{hazardPointCode}）的{deviceName}设备{attrName}监测值({value}{unit})超过阈值范围[{rangeMin}{unit}, {rangeMax}{unit}]，请及时处理。',
 'in_app,sms', 2, '0'),
('device_offline', '设备离线告警', 'alarm',
 '{deviceName}设备离线',
 '{deviceName}（{deviceCode}）离线超过{offlineMinutes}分钟，请检查设备状态。',
 'in_app', 1, '0'),
('system_maintenance', '系统维护通知', 'system',
 '系统维护通知',
 '系统将于{maintenanceTime}进行维护，预计持续{duration}分钟，届时部分功能可能不可用。',
 'in_app,email', 0, '0');

-- 通知实例表（每条实际发送的通知）
CREATE TABLE IF NOT EXISTS sys_notify_instance (
    instance_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_code VARCHAR(64)                  COMMENT '模板编码，NULL表示自定义通知',
    notify_type   VARCHAR(32) NOT NULL         COMMENT 'alarm / business / system',
    title         VARCHAR(255) NOT NULL        COMMENT '通知标题',
    content       TEXT                         COMMENT '通知内容',
    priority      TINYINT DEFAULT 0            COMMENT '优先级',
    source_type   VARCHAR(32)                  COMMENT '触发来源: alarm_engine / device_event / manual',
    source_id     VARCHAR(64)                  COMMENT '触发来源ID（链路追踪）',
    create_by     VARCHAR(64) DEFAULT ''       COMMENT '创建者',
    create_time   DATETIME                     COMMENT '创建时间',
    INDEX idx_type_time (notify_type, create_time),
    INDEX idx_source (source_type, source_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知实例';

-- 通知-用户关联表（多对多，含发送状态与生命周期）
CREATE TABLE IF NOT EXISTS sys_notify_target (
    target_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    instance_id   BIGINT NOT NULL              COMMENT '通知实例ID',
    user_id       BIGINT NOT NULL              COMMENT '目标用户ID',
    channel       VARCHAR(16) DEFAULT 'in_app' COMMENT '推送通道: in_app / email / sms',
    send_status   TINYINT DEFAULT 0            COMMENT '0=待发送 1=已发送 2=发送失败 3=已读 4=已归档',
    send_time     DATETIME                     COMMENT '发送时间',
    read_time     DATETIME                     COMMENT '阅读时间（in_app通道）',
    archive_time  DATETIME                     COMMENT '归档时间',
    retry_count   INT DEFAULT 0                COMMENT '重试次数',
    error_msg     VARCHAR(500)                 COMMENT '发送失败原因',
    INDEX idx_user_status (user_id, send_status),
    INDEX idx_instance (instance_id),
    INDEX idx_channel_status (channel, send_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知目标（下发/处理/归档全生命周期）';
