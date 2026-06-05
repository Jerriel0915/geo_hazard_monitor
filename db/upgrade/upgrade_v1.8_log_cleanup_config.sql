-- ============================================
-- upgrade_v1.8_log_cleanup_config.sql
-- 日志清理自定义配置：允许通过系统设置界面配置清理策略
-- 日期：2026-06-04
-- ============================================

-- 1. 新增 SysConfig 配置项
INSERT INTO `sys_config` (`config_id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`,
                          `create_time`, `remark`)
VALUES (100, '日志自动清理开关', 'log.cleanup.enabled', 'true', 'Y', 'admin', NOW(), '是否启用日志定时清理任务'),
       (101, '日志保留天数', 'log.cleanup.retention-days', '30', 'Y', 'admin', NOW(),
        '超过此天数的操作日志/认证日志/运行日志将被清理'),
       (102, '清理执行时间', 'log.cleanup.cron', '0 0 3 * * ?', 'Y', 'admin', NOW(),
        'Quartz cron 表达式，默认每天凌晨3点');
