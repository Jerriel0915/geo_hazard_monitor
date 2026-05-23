-- v1.3 历史日志迁移脚本
-- 执行前请先完成 v1.3_log_module_schema.sql 与 v1.3_log_module_indexes.sql

INSERT INTO `log_operation_record`
(`event_id`, `trace_id`, `request_id`, `user_id`, `username`, `dept_name`, `title`, `business_type`,
 `api_path`, `request_method`, `controller_method`, `client_ip`, `client_location`, `user_agent`,
 `request_params`, `response_body`, `http_status`, `exec_status`, `error_message`, `cost_time_ms`,
 `occurred_at`, `created_at`)
SELECT
  900000000000000000 + `oper_id`,
  NULL,
  NULL,
  NULL,
  `oper_name`,
  `dept_name`,
  `title`,
  CAST(`business_type` AS CHAR),
  `oper_url`,
  `request_method`,
  `method`,
  `oper_ip`,
  `oper_location`,
  NULL,
  `oper_param`,
  `json_result`,
  NULL,
  CASE WHEN `status` = 0 THEN 'SUCCESS' ELSE 'FAIL' END,
  `error_msg`,
  `cost_time`,
  COALESCE(`oper_time`, NOW(3)),
  NOW(3)
FROM `sys_oper_log`;

INSERT INTO `log_auth_record`
(`event_id`, `trace_id`, `request_id`, `user_id`, `username`, `auth_event_type`, `auth_channel`,
 `request_uri`, `request_method`, `client_ip`, `client_location`, `user_agent`, `device_type`,
 `http_status`, `result_status`, `failure_code`, `failure_message`, `token_id`, `occurred_at`, `created_at`)
SELECT
  910000000000000000 + `info_id`,
  NULL,
  NULL,
  NULL,
  `user_name`,
  CASE
    WHEN `msg` LIKE '%退出%' THEN 'LOGOUT'
    WHEN `status` = '0' THEN 'LOGIN_SUCCESS'
    ELSE 'LOGIN_FAIL'
  END,
  'LEGACY',
  NULL,
  NULL,
  `ipaddr`,
  `login_location`,
  CONCAT_WS(' / ', `browser`, `os`),
  NULL,
  NULL,
  CASE WHEN `status` = '0' THEN 'SUCCESS' ELSE 'FAIL' END,
  CASE WHEN `status` = '0' THEN NULL ELSE 'LEGACY_LOGIN_FAIL' END,
  `msg`,
  NULL,
  COALESCE(`login_time`, NOW(3)),
  NOW(3)
FROM `sys_logininfor`;

-- 迁移校验建议：
-- 1. select count(*) from sys_oper_log;
-- 2. select count(*) from log_operation_record where event_id >= 900000000000000000;
-- 3. select count(*) from sys_logininfor;
-- 4. select count(*) from log_auth_record where event_id >= 910000000000000000;
