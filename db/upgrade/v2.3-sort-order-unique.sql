-- ============================================================
-- v2.3: monitor_content 增加 sort_order 唯一约束
-- 同 monitor_type 下 sort_order 不允许重复
-- 注意: 对于已有 sort_order=0 的脏数据，需手动修正后执行
-- ============================================================

-- 步骤1: 修正已有 sort_order=0 的脏数据 (用 ROW_NUMBER 重新分配)
-- 对于重复的 sort_order，按 id 顺序重新编号
UPDATE monitor_content mc
INNER JOIN (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY monitor_type_id ORDER BY sort_order, id) AS rn
    FROM monitor_content
    WHERE del_flag = 0
) t ON mc.id = t.id
SET mc.sort_order = t.rn
WHERE mc.del_flag = 0;

-- 步骤2: 添加唯一约束
ALTER TABLE monitor_content ADD CONSTRAINT uk_monitor_type_sort_order UNIQUE (monitor_type_id, sort_order);