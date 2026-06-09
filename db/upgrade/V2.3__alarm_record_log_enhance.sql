-- V2.3: 告警处置记录增强 — 新增处置类型和处置结果字段
ALTER TABLE alarm_record_log
    ADD COLUMN disposal_type   varchar(50)  DEFAULT NULL COMMENT '处置类型: 开始处置/已销警/标记误报/批量销警/批量误报' AFTER to_status,
    ADD COLUMN disposal_result varchar(500) DEFAULT NULL COMMENT '处置结果描述' AFTER note;
