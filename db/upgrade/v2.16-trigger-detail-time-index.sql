-- v2.16: 为 alarm_record_trigger_detail.trigger_time 新增索引
-- 支持按时间窗口查询触发趋势（selectMonthlyLevelCounts）
ALTER TABLE alarm_record_trigger_detail
    ADD INDEX idx_trigger_time (trigger_time);
