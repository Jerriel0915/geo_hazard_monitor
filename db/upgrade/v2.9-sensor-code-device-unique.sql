-- ============================================================
-- 传感器编号约束：全局唯一 → 设备内唯一
-- 变更: device_sensor 表 sensor_code 唯一约束从全局唯一改为 (device_id, sensor_code) 复合唯一
-- 日期: 2026-06-23
-- 说明:
--   原约束 uk_device_sensor_code (sensor_code) 全局唯一，导致不同设备无法使用相同传感器编号。
--   新约束 uk_device_sensor (device_id, sensor_code) 允许跨设备重复，仅在同一设备内保持唯一。
--   下游 IoTDB 路径 (root.{db}.d{deviceId}.s{sensorCode}) 与告警引擎均已使用 (deviceId, sensorCode) 复合键，
--   本次变更对时序落库和告警判断无影响。
-- 数据安全:
--   原全局唯一约束保证不存在重复 (device_id, sensor_code) 对，DROP + ADD 无数据冲突风险。
-- ============================================================

ALTER TABLE `device_sensor`
    DROP INDEX `uk_device_sensor_code`;

ALTER TABLE `device_sensor`
    ADD UNIQUE KEY `uk_device_sensor` (`device_id`, `sensor_code`);

ALTER TABLE `device_sensor`
    MODIFY COLUMN `sensor_code` varchar(100) NOT NULL
        COMMENT '传感器编号（设备内唯一，MQTT 主题路由 / IoTDB 路径键）';
