-- =====================================================
-- 告警类型拆分: ALARM → THRESHOLD / COMPREHENSIVE
-- 影响: alarm_dispatch_rule.event_type + alarm_notification.source_type
-- 执行前请备份这两张表
-- =====================================================

-- 1. 分发规则: 原 ALARM → THRESHOLD
--    用户表述"原告警事件改为阈值告警"
UPDATE alarm_dispatch_rule
SET event_type = 'THRESHOLD'
WHERE event_type = 'ALARM';

-- 2. 通知记录: 原 alarm → threshold / comprehensive (按 alarm_record.alarm_type 判断)
UPDATE alarm_notification an
    JOIN alarm_record ar ON an.source_id = ar.id
SET an.source_type =
    CASE ar.alarm_type
        WHEN 'COMPREHENSIVE' THEN 'comprehensive'
        ELSE 'threshold'
    END
WHERE an.source_type = 'alarm';
