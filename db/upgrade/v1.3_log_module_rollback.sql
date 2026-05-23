-- v1.3 系统日志模块回滚脚本
-- 仅删除新建表，不触碰旧 sys_oper_log / sys_logininfor

DROP TABLE IF EXISTS `log_stream_checkpoint`;
DROP TABLE IF EXISTS `log_runtime_record`;
DROP TABLE IF EXISTS `log_auth_record`;
DROP TABLE IF EXISTS `log_operation_record`;
