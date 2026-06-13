-- ============================================================
-- 升级: 合并 sensor_code 与 sensor_no → sensor_code
-- 版本: v2.1.0
-- 日期: 2026-06-13
-- 说明:
--   1. sensor_code (全局唯一, API CRUD) 与 sensor_no (设备内唯一, MQTT/IoTDB)
--      在实际使用中始终为相同值，合并为 sensor_code 单一字段
--   2. 删除 sensor_no 列及其唯一索引
--   3. sensor_code 将同时承担 MQTT 主题路由和 IoTDB 路径键的职责
-- ============================================================

-- Step 1: 确保 sensor_code 与 sensor_no 一致（兜底，正常情况下始终相同）
UPDATE device_sensor
SET sensor_code = sensor_no
WHERE sensor_code != sensor_no
   OR sensor_code IS NULL;

-- Step 2: 删除 sensor_no 唯一索引
ALTER TABLE device_sensor
    DROP INDEX uk_device_sensor_no;

-- Step 3: 删除 sensor_no 列
ALTER TABLE device_sensor
    DROP COLUMN sensor_no;

-- Step 4: 更新 sensor_code 列注释
ALTER TABLE device_sensor
    MODIFY COLUMN sensor_code varchar(100) NOT NULL COMMENT '传感器编码（全局唯一，API CRUD 主标识 / MQTT 主题路由 / IoTDB 路径键）';
