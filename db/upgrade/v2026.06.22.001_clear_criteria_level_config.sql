-- v2026.06.22.001_clear_criteria_level_config.sql
-- 告警判据 subject 格式迁移: 老格式 payload.current.attrCode 不再兼容
-- 新格式: [sensorCode.] {current|prev} {payload|device|packet} {attrCode}
-- 清空所有启用判据的 level_config, 用户在前端重新配置等级条件
-- 注: level_config 列为 JSON NOT NULL, 无法设为 NULL, 改用空 JSON 对象 '{}' 表示"未配置"

UPDATE alarm_criteria
SET level_config = CAST('{}' AS JSON),
    version = version + 1,
    update_time = NOW(),
    update_by = 'system-migration-20260622'
WHERE del_flag = 0
  AND JSON_LENGTH(level_config) > 0;
