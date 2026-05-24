-- v1.3 系统日志模块建表脚本

CREATE TABLE IF NOT EXISTS `log_operation_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `event_id` bigint NOT NULL,
  `trace_id` varchar(64) DEFAULT NULL,
  `request_id` varchar(64) DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  `username` varchar(64) DEFAULT NULL,
  `dept_name` varchar(64) DEFAULT NULL,
  `title` varchar(128) DEFAULT NULL,
  `business_type` varchar(32) DEFAULT NULL,
  `api_path` varchar(255) DEFAULT NULL,
  `request_method` varchar(16) DEFAULT NULL,
  `controller_method` varchar(255) DEFAULT NULL,
  `client_ip` varchar(64) DEFAULT NULL,
  `client_location` varchar(255) DEFAULT NULL,
  `user_agent` varchar(512) DEFAULT NULL,
  `request_params` text,
  `response_body` text,
  `http_status` int DEFAULT NULL,
  `exec_status` varchar(16) DEFAULT NULL,
  `error_message` varchar(2000) DEFAULT NULL,
  `cost_time_ms` bigint DEFAULT NULL,
  `occurred_at` datetime(3) NOT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_log_operation_event_id` (`event_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='接口调用日志';

CREATE TABLE IF NOT EXISTS `log_auth_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `event_id` bigint NOT NULL,
  `trace_id` varchar(64) DEFAULT NULL,
  `request_id` varchar(64) DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  `username` varchar(64) DEFAULT NULL,
  `auth_event_type` varchar(32) NOT NULL,
  `auth_channel` varchar(32) DEFAULT NULL,
  `request_uri` varchar(255) DEFAULT NULL,
  `request_method` varchar(16) DEFAULT NULL,
  `client_ip` varchar(64) DEFAULT NULL,
  `client_location` varchar(255) DEFAULT NULL,
  `user_agent` varchar(512) DEFAULT NULL,
  `device_type` varchar(32) DEFAULT NULL,
  `http_status` int DEFAULT NULL,
  `result_status` varchar(16) DEFAULT NULL,
  `failure_code` varchar(64) DEFAULT NULL,
  `failure_message` varchar(1000) DEFAULT NULL,
  `token_id` varchar(128) DEFAULT NULL,
  `occurred_at` datetime(3) NOT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_log_auth_event_id` (`event_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='认证日志';

CREATE TABLE IF NOT EXISTS `log_runtime_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `event_id` bigint NOT NULL,
  `trace_id` varchar(64) DEFAULT NULL,
  `request_id` varchar(64) DEFAULT NULL,
  `level` varchar(16) NOT NULL,
  `logger_name` varchar(255) NOT NULL,
  `thread_name` varchar(128) DEFAULT NULL,
  `biz_module` varchar(64) DEFAULT NULL,
  `source_app` varchar(64) DEFAULT NULL,
  `host_name` varchar(64) DEFAULT NULL,
  `environment` varchar(32) DEFAULT NULL,
  `message` text NOT NULL,
  `message_digest` varchar(512) DEFAULT NULL,
  `exception_class` varchar(255) DEFAULT NULL,
  `stack_trace` mediumtext,
  `occurred_at` datetime(3) NOT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_log_runtime_event_id` (`event_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运行日志';

CREATE TABLE IF NOT EXISTS `log_stream_checkpoint` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `subscriber_key` varchar(128) NOT NULL,
  `last_event_id` bigint NOT NULL,
  `log_type` varchar(32) NOT NULL,
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_log_stream_checkpoint` (`subscriber_key`, `log_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='日志流断点记录';
