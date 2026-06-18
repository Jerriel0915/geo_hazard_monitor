-- 补充 alarm_notification 去重唯一键（计划 A 暂未创建以避免迁移冲突）
-- 注意: 必须先回填 source_id 再去重, 否则 NULL=NULL 会导致去重失效、ADD UNIQUE 失败

-- 1) 回填 source_type/source_id（兼容老数据: NULL source_id 但有 alarm_id）
UPDATE alarm_notification
SET source_type = 'alarm',
    source_id = alarm_id
WHERE source_id IS NULL
  AND alarm_id IS NOT NULL;

-- 2) 清理重复数据（保留最早一条, 即最小 id）
DELETE n1 FROM alarm_notification n1
INNER JOIN alarm_notification n2
  ON n1.source_type = n2.source_type
 AND n1.source_id = n2.source_id
 AND n1.recipient_id = n2.recipient_id
 AND n1.channel = n2.channel
 AND n1.id > n2.id;

-- 3) 创建唯一键
ALTER TABLE `alarm_notification`
    ADD UNIQUE KEY `uk_notif_dedup`
        (`source_type`, `source_id`, `recipient_id`, `channel`);
