-- ============================================================
-- v2.2: monitor_content 增加 sort_order 字段
-- 每个 monitor_type 内 sort_order 从 1 递增，monitor_type 之间独立
-- ============================================================

ALTER TABLE monitor_content ADD COLUMN sort_order INT NOT NULL DEFAULT 0 COMMENT '排序号(每个监测类型内从1递增)' AFTER unit;

-- 按现有 id 顺序初始化 sort_order（每个 monitor_type 内独立编号）
UPDATE monitor_content mc
SET sort_order = (
    SELECT rn FROM (
        SELECT id, ROW_NUMBER() OVER (PARTITION BY monitor_type_id ORDER BY id) AS rn
        FROM monitor_content WHERE del_flag = 0
    ) t WHERE t.id = mc.id
)
WHERE del_flag = 0;
