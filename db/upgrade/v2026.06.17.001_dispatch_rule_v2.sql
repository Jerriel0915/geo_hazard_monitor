-- ============================================================
-- 通知规则迭代 v2 (2026-06-17)
-- 关联：docs/superpowers/specs/2026-06-17-alarm-dispatch-rule-iteration-design.md
-- ============================================================
-- 迁移映射说明：
--   旧表 alarm_dispatch_rule 无 type 字段 → 全部映射为 event_type='ALARM'
--   旧表 alarm_types / time_window / recipients_json 字段不迁移（按规格 §4.1 删除）
--   旧表无 remark 字段 → 迁移为 NULL
--   本脚本不可重入（RENAME 后二次执行会失败），由 DBA 手工控制升级时机
-- ============================================================

-- ---------- 1. 备份旧主表 ----------
RENAME TABLE `alarm_dispatch_rule` TO `alarm_dispatch_rule_bak`;

-- ---------- 2. 重建主表（精简） ----------
CREATE TABLE `alarm_dispatch_rule` (
    `id`           bigint       NOT NULL AUTO_INCREMENT COMMENT '规则ID',
    `name`         varchar(200) NOT NULL COMMENT '规则名称',
    `event_type`   varchar(10)  NOT NULL COMMENT '事件类型: ALARM=告警 / OFFLINE=设备离线',
    `alarm_levels` varchar(50)  DEFAULT NULL COMMENT '订阅告警等级（逗号分隔）: 1,2,3,4；OFFLINE 类型时为 NULL',
    `channels`     varchar(100) NOT NULL DEFAULT 'SYSTEM' COMMENT '通知渠道（逗号分隔）: SYSTEM,SMS,EMAIL',
    `is_enabled`   tinyint      DEFAULT 1 COMMENT '0=禁用 1=启用',
    `del_flag`     tinyint      DEFAULT 0,
    `create_by`    varchar(64)  DEFAULT '',
    `create_time`  datetime     DEFAULT CURRENT_TIMESTAMP,
    `update_by`    varchar(64)  DEFAULT '',
    `update_time`  datetime     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    `remark`       varchar(500) DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_dispatch_event_enabled` (`event_type`, `is_enabled`, `del_flag`)
) COMMENT='通知规则主表';

-- ---------- 3. 关联表：隐患点 ----------
CREATE TABLE `alarm_dispatch_rule_hazard_point` (
    `rule_id`         bigint      NOT NULL,
    `hazard_point_id` varchar(20) NOT NULL COMMENT '隐患点ID；"*" 表示全部',
    PRIMARY KEY (`rule_id`, `hazard_point_id`),
    KEY `idx_adrhp_hp` (`hazard_point_id`)
) COMMENT='通知规则-隐患点关联表';

-- ---------- 4. 关联表：设备（离线通知专用） ----------
CREATE TABLE `alarm_dispatch_rule_device` (
    `rule_id`    bigint      NOT NULL,
    `device_id`  varchar(20) NOT NULL COMMENT '设备ID；"*" 表示全部',
    PRIMARY KEY (`rule_id`, `device_id`),
    KEY `idx_adrd_dev` (`device_id`)
) COMMENT='通知规则-设备关联表';

-- ---------- 5. 关联表：接收人 ----------
CREATE TABLE `alarm_dispatch_rule_recipient` (
    `rule_id`        bigint      NOT NULL,
    `recipient_type` varchar(10) NOT NULL COMMENT 'ROLE / DEPT / USER',
    `recipient_id`   varchar(20) NOT NULL COMMENT '角色/部门/用户ID；"*" 表示该类型全部',
    PRIMARY KEY (`rule_id`, `recipient_type`, `recipient_id`),
    KEY `idx_adrr_type_id` (`recipient_type`, `recipient_id`)
) COMMENT='通知规则-接收人关联表';

-- ---------- 6. alarm_notification 表扩展（计划 B 用，此处一并升级） ----------
ALTER TABLE `alarm_notification`
    ADD COLUMN `read_time`   datetime     DEFAULT NULL COMMENT '已读时间',
    ADD COLUMN `source_type` varchar(20)  DEFAULT 'alarm' COMMENT 'alarm=告警 / offline=设备离线',
    ADD COLUMN `source_id`   bigint       DEFAULT NULL COMMENT '来源ID（alarm_record.id 或 device.id）',
    MODIFY COLUMN `error_msg` varchar(1000) DEFAULT NULL
        COMMENT '渠道发送错误信息，格式 [ERROR_CODE] 描述';

-- 注：uk_notif_dedup 唯一键在计划 B 中创建（避免本计划中存量数据冲突）

-- ---------- 7. 数据迁移 ----------
-- 7.1 主表迁移（旧表无 type 字段，全部视为 ALARM）
INSERT INTO alarm_dispatch_rule
    (id, name, event_type, alarm_levels, channels, is_enabled, del_flag,
     create_by, create_time, update_by, update_time, remark)
SELECT
    id, name,
    'ALARM' AS event_type,
    alarm_levels, channels, is_enabled, del_flag,
    create_by, create_time, create_by, create_time, NULL AS remark
FROM alarm_dispatch_rule_bak
WHERE del_flag = 0;

-- 7.2 隐患点关联迁移（NULL → '*'；旧表无 type，全部迁移）
INSERT INTO alarm_dispatch_rule_hazard_point (rule_id, hazard_point_id)
SELECT
    id,
    CASE WHEN hazard_point_id IS NULL THEN '*'
         ELSE CAST(hazard_point_id AS CHAR)
    END
FROM alarm_dispatch_rule_bak
WHERE del_flag = 0;

-- 7.3 接收人迁移（recipients_json → USER 类型）
INSERT INTO alarm_dispatch_rule_recipient (rule_id, recipient_type, recipient_id)
SELECT
    r.id, 'USER', CAST(jt.uid AS CHAR)
FROM alarm_dispatch_rule_bak r,
     JSON_TABLE(r.recipients_json, '$[*]'
        COLUMNS (uid BIGINT PATH '$.userId')) jt
WHERE r.del_flag = 0
  AND r.recipients_json IS NOT NULL
  AND JSON_LENGTH(r.recipients_json) > 0;

-- 7.4 设备关联：旧表未持久化 device_ids，无法迁移
--     （用户需在 UI 重新编辑 OFFLINE 规则）

-- ---------- 8. 校验 SQL（手工执行验证，不写入文件） ----------
-- SELECT COUNT(*) FROM alarm_dispatch_rule_bak WHERE del_flag=0;
-- SELECT COUNT(*) FROM alarm_dispatch_rule;
-- SELECT COUNT(DISTINCT rule_id) FROM alarm_dispatch_rule_hazard_point;
-- SELECT recipient_type, COUNT(*) FROM alarm_dispatch_rule_recipient GROUP BY recipient_type;

-- ---------- 9. 旧表暂保留（验证 1 周后由 DBA 手工 DROP） ----------
-- DROP TABLE alarm_dispatch_rule_bak;
