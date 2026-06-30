-- v2026.06.30.001 移除 alarm_strategy.default_alarm_level 列
-- 原因: 告警等级由 Groovy 脚本返回值决定 (1-4)，default_alarm_level 字段在引擎中从未被使用
-- 影响范围: alarm_strategy 表; 后端 domain/DTO/mapper/controller; 前端 API 类型 + 表单

ALTER TABLE alarm_strategy DROP COLUMN default_alarm_level;
