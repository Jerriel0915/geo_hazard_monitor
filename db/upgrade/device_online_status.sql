-- ============================================================
-- 设备在线状态运维指标独立存储
-- 用途：解耦运维指标与设备业务主表，支撑在线率统计与掉线历史追溯
-- ============================================================

-- 设备在线状态快照（运维专用，一行一设备，通过 UPSERT 维护）
CREATE TABLE IF NOT EXISTS device_online_status (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id           BIGINT NOT NULL UNIQUE           COMMENT '设备ID',
    client_id           VARCHAR(128)                    COMMENT '当前MQTT clientId',
    status              TINYINT DEFAULT 0               COMMENT '0=离线 1=在线',
    online_at           DATETIME                        COMMENT '本次上线时间',
    offline_at          DATETIME                        COMMENT '上次离线时间',
    last_report_at      DATETIME                        COMMENT '最后数据上报时间',
    session_duration_sec INT                            COMMENT '最近会话持续秒数',
    INDEX idx_status (status),
    INDEX idx_last_report (last_report_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备在线状态（运维指标独立存储）';

-- 设备上下线事件日志（历史明细，仅追加写入）
CREATE TABLE IF NOT EXISTS device_online_event_log (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id   BIGINT NOT NULL                          COMMENT '设备ID',
    event_type  VARCHAR(16) NOT NULL                     COMMENT '事件类型: ONLINE / OFFLINE / HEARTBEAT',
    client_id   VARCHAR(128)                            COMMENT 'MQTT clientId',
    client_ip   VARCHAR(64)                             COMMENT '客户端IP',
    event_time  DATETIME NOT NULL                       COMMENT '事件发生时间',
    reason      VARCHAR(255)                            COMMENT '掉线原因',
    INDEX idx_device_time (device_id, event_time),
    INDEX idx_event_time (event_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备上下线事件日志';

-- 传感器最后上报时间（用于活跃率统计与运维查询）
ALTER TABLE device_sensor
    ADD COLUMN IF NOT EXISTS last_report_time DATETIME COMMENT '最后数据上报时间';

CREATE INDEX IF NOT EXISTS idx_sensor_last_report
    ON device_sensor (last_report_time);
