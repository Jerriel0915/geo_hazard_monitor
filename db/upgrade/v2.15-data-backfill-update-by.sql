-- ============================================================
-- 审计字段数据补齐脚本
-- 版本: v2.15 (数据修补)
-- 描述:
--   将全系统所有业务表中 update_by 为 NULL 或 '' 的记录
--   回填为 create_by（即记录的创建者），确保"修改人"列有值可显示。
--   同时将 update_time 为 NULL 的记录回填为 create_time。
-- 幂等: 仅修改 NULL/'' 行，已有值的行不受影响。
-- ============================================================

-- ==================== 告警域 ====================
UPDATE alarm_criteria     SET update_by = create_by WHERE update_by IS NULL OR update_by = '';
UPDATE alarm_criteria     SET update_time = create_time WHERE update_time IS NULL;
UPDATE alarm_dispatch_rule SET update_by = create_by WHERE update_by IS NULL OR update_by = '';
UPDATE alarm_dispatch_rule SET update_time = create_time WHERE update_time IS NULL;
UPDATE alarm_record       SET update_by = create_by WHERE update_by IS NULL OR update_by = '';
UPDATE alarm_record       SET update_time = create_time WHERE update_time IS NULL;
UPDATE alarm_strategy     SET update_by = create_by WHERE update_by IS NULL OR update_by = '';
UPDATE alarm_strategy     SET update_time = create_time WHERE update_time IS NULL;

-- ==================== 设备域 ====================
UPDATE device              SET update_by = create_by WHERE update_by IS NULL OR update_by = '';
UPDATE device              SET update_time = create_time WHERE update_time IS NULL;
UPDATE device_hazard_point SET update_by = create_by WHERE update_by IS NULL OR update_by = '';
UPDATE device_hazard_point SET update_time = create_time WHERE update_time IS NULL;
UPDATE device_sensor       SET update_by = create_by WHERE update_by IS NULL OR update_by = '';
UPDATE device_sensor       SET update_time = create_time WHERE update_time IS NULL;
UPDATE sensor_attribute    SET update_by = create_by WHERE update_by IS NULL OR update_by = '';
UPDATE sensor_attribute    SET update_time = create_time WHERE update_time IS NULL;

-- ==================== 隐患点域 ====================
UPDATE hazard_point       SET update_by = create_by WHERE update_by IS NULL OR update_by = '';
UPDATE hazard_point       SET update_time = create_time WHERE update_time IS NULL;
UPDATE hazard_point_group SET update_by = create_by WHERE update_by IS NULL OR update_by = '';
UPDATE hazard_point_group SET update_time = create_time WHERE update_time IS NULL;

-- ==================== 监测字典域 ====================
UPDATE monitor_content SET update_by = create_by WHERE update_by IS NULL OR update_by = '';
UPDATE monitor_content SET update_time = create_time WHERE update_time IS NULL;
UPDATE monitor_type    SET update_by = create_by WHERE update_by IS NULL OR update_by = '';
UPDATE monitor_type    SET update_time = create_time WHERE update_time IS NULL;

-- ==================== 报告域 ====================
UPDATE report_template SET update_by = create_by WHERE update_by IS NULL OR update_by = '';
UPDATE report_template SET update_time = create_time WHERE update_time IS NULL;
UPDATE report_record   SET update_by = create_by WHERE update_by IS NULL OR update_by = '';
UPDATE report_record   SET update_time = create_time WHERE update_time IS NULL;

-- ==================== 视频域 ====================
UPDATE video_device              SET update_by = create_by WHERE update_by IS NULL OR update_by = '';
UPDATE video_device              SET update_time = create_time WHERE update_time IS NULL;
UPDATE video_device_hazard_point SET update_by = create_by WHERE update_by IS NULL OR update_by = '';
UPDATE video_device_hazard_point SET update_time = create_time WHERE update_time IS NULL;

-- ==================== 系统 RBAC 域 ====================
UPDATE sys_config       SET update_by = create_by WHERE update_by IS NULL OR update_by = '';
UPDATE sys_config       SET update_time = create_time WHERE update_time IS NULL;
UPDATE sys_dept         SET update_by = create_by WHERE update_by IS NULL OR update_by = '';
UPDATE sys_dept         SET update_time = create_time WHERE update_time IS NULL;
UPDATE sys_dict_data    SET update_by = create_by WHERE update_by IS NULL OR update_by = '';
UPDATE sys_dict_data    SET update_time = create_time WHERE update_time IS NULL;
UPDATE sys_dict_type    SET update_by = create_by WHERE update_by IS NULL OR update_by = '';
UPDATE sys_dict_type    SET update_time = create_time WHERE update_time IS NULL;
UPDATE sys_job          SET update_by = create_by WHERE update_by IS NULL OR update_by = '';
UPDATE sys_job          SET update_time = create_time WHERE update_time IS NULL;
UPDATE sys_menu         SET update_by = create_by WHERE update_by IS NULL OR update_by = '';
UPDATE sys_menu         SET update_time = create_time WHERE update_time IS NULL;
UPDATE sys_notice       SET update_by = create_by WHERE update_by IS NULL OR update_by = '';
UPDATE sys_notice       SET update_time = create_time WHERE update_time IS NULL;
UPDATE sys_notify_template SET update_by = create_by WHERE update_by IS NULL OR update_by = '';
UPDATE sys_notify_template SET update_time = create_time WHERE update_time IS NULL;
UPDATE sys_organization SET update_by = create_by WHERE update_by IS NULL OR update_by = '';
UPDATE sys_organization SET update_time = create_time WHERE update_time IS NULL;
UPDATE sys_post         SET update_by = create_by WHERE update_by IS NULL OR update_by = '';
UPDATE sys_post         SET update_time = create_time WHERE update_time IS NULL;
UPDATE sys_role         SET update_by = create_by WHERE update_by IS NULL OR update_by = '';
UPDATE sys_role         SET update_time = create_time WHERE update_time IS NULL;
UPDATE sys_user         SET update_by = create_by WHERE update_by IS NULL OR update_by = '';
UPDATE sys_user         SET update_time = create_time WHERE update_time IS NULL;

-- ==================== 算法库 ====================
UPDATE algo_info    SET update_by = create_by WHERE update_by IS NULL OR update_by = '';
UPDATE algo_info    SET update_time = create_time WHERE update_time IS NULL;
UPDATE algo_version SET update_by = create_by WHERE update_by IS NULL OR update_by = '';
UPDATE algo_version SET update_time = create_time WHERE update_time IS NULL;

-- ==================== 解析策略 ====================
UPDATE iot_data_parse_strategy SET update_by = create_by WHERE update_by IS NULL OR update_by = '';
UPDATE iot_data_parse_strategy SET update_time = create_time WHERE update_time IS NULL;
