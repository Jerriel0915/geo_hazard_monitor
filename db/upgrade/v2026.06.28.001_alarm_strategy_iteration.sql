-- =====================================================
-- 综合告警功能迭代完善 DB 迁移
-- 版本: v2026.06.28.001
-- 日期: 2026-06-28
-- 描述:
--   1. alarm_strategy_hazard_point.hazard_point_id 列类型 BIGINT → VARCHAR(100)
--      支持 * / group:{id} / {数字} 三种范围语义
--   2. 新建 alarm_strategy_execution_log 综合策略执行日志表
-- 幂等:
--   - ALTER TABLE MODIFY 为幂等操作 (重复执行无副作用)
--   - CREATE TABLE IF NOT EXISTS 保证可重复执行
-- =====================================================

-- 1. alarm_strategy_hazard_point: hazard_point_id BIGINT → VARCHAR(100)
ALTER TABLE alarm_strategy_hazard_point
  MODIFY COLUMN hazard_point_id VARCHAR(100) NOT NULL
  COMMENT '范围值: *=全部隐患点 / group:{id}=按分组 / {数字}=指定隐患点ID';

-- 2. 新建执行日志表
CREATE TABLE IF NOT EXISTS alarm_strategy_execution_log (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    strategy_id      BIGINT       NOT NULL COMMENT '策略ID',
    trigger_type     VARCHAR(20)  NOT NULL COMMENT 'CRON/DATA_INGEST/ALARM_TRIGGER',
    trigger_source   TEXT         NULL     COMMENT '触发事件摘要 JSON',
    hazard_point_ids VARCHAR(500) NULL     COMMENT '解析后的隐患点ID列表 (逗号分隔)',
    result_level     INT          NULL     COMMENT '脚本返回等级 1-4',
    result_status    VARCHAR(20)  NOT NULL COMMENT 'SUCCESS/NO_ALARM/FAIL/TIMEOUT',
    duration_ms      BIGINT       NOT NULL DEFAULT 0,
    script_logs      TEXT         NULL     COMMENT '脚本内 log 工具收集的日志 (JSON数组)',
    error_message    TEXT         NULL     COMMENT '异常信息',
    triggered_count  INT          NOT NULL DEFAULT 0 COMMENT '触发告警记录数',
    create_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_strategy_create (strategy_id, create_time DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='综合告警策略执行日志';
