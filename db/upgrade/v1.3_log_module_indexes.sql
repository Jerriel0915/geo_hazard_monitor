-- v1.3 系统日志模块索引脚本

CREATE INDEX `idx_log_operation_time` ON `log_operation_record` (`occurred_at` DESC);
CREATE INDEX `idx_log_operation_user_time` ON `log_operation_record` (`user_id`, `occurred_at` DESC);
CREATE INDEX `idx_log_operation_status_time` ON `log_operation_record` (`exec_status`, `occurred_at` DESC);
CREATE INDEX `idx_log_operation_api_time` ON `log_operation_record` (`api_path`, `occurred_at` DESC);
CREATE INDEX `idx_log_operation_trace` ON `log_operation_record` (`trace_id`);

CREATE INDEX `idx_log_auth_time` ON `log_auth_record` (`occurred_at` DESC);
CREATE INDEX `idx_log_auth_type_time` ON `log_auth_record` (`auth_event_type`, `occurred_at` DESC);
CREATE INDEX `idx_log_auth_user_time` ON `log_auth_record` (`user_id`, `occurred_at` DESC);
CREATE INDEX `idx_log_auth_status_time` ON `log_auth_record` (`result_status`, `occurred_at` DESC);
CREATE INDEX `idx_log_auth_trace` ON `log_auth_record` (`trace_id`);

CREATE INDEX `idx_log_runtime_time` ON `log_runtime_record` (`occurred_at` DESC);
CREATE INDEX `idx_log_runtime_level_time` ON `log_runtime_record` (`level`, `occurred_at` DESC);
CREATE INDEX `idx_log_runtime_logger_time` ON `log_runtime_record` (`logger_name`, `occurred_at` DESC);
CREATE INDEX `idx_log_runtime_host_time` ON `log_runtime_record` (`host_name`, `occurred_at` DESC);
CREATE INDEX `idx_log_runtime_trace` ON `log_runtime_record` (`trace_id`);
