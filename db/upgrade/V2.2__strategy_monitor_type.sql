-- V2.2: 告警策略新增 monitor_type_id 兜底绑定
-- 当策略未绑定到具体隐患点时，通过监测类型ID匹配所有关联设备所属的隐患点
ALTER TABLE alarm_strategy
    ADD COLUMN monitor_type_id bigint DEFAULT NULL COMMENT '监测类型ID（NULL=仅按隐患点绑定生效；非NULL=适用所有关联该监测类型的隐患点）' AFTER description;
