-- =====================================================
-- 告警类型拆分: ALARM → THRESHOLD / COMPREHENSIVE
-- 影响: alarm_dispatch_rule.event_type + alarm_notification.source_type
-- 执行前请备份这两张表
-- =====================================================

-- 0. 扩展 event_type 列长度 (varchar(10) → varchar(20))，容纳 COMPREHENSIVE (14 字符)
ALTER TABLE alarm_dispatch_rule MODIFY COLUMN event_type varchar(20) NOT NULL COMMENT '事件类型: THRESHOLD=阈值告警 / COMPREHENSIVE=综合告警 / OFFLINE=设备离线';

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

-- 2b. 兜底: source_id 为 NULL 或 alarm_record 已删除的遗留记录，无法 JOIN 判断类型，
--     统一回填为 threshold (历史告警在拆分前均为阈值告警，综合告警为新特性)
UPDATE alarm_notification SET source_type = 'threshold' WHERE source_type = 'alarm';
