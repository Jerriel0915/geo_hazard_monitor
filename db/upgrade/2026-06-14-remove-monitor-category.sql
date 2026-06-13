-- 移除监测大类 (monitor_category) — 三级字典简化为二级
-- 原因: 大类与类型几乎 1:1 映射, 无独立业务价值; MyBatis 层从未实际读写 category_id
-- 日期: 2026-06-14

-- 1. 删除 monitor_type 上的 category 索引
ALTER TABLE monitor_type
    DROP INDEX idx_monitor_type_category;

-- 2. 移除 monitor_type 的 category_id 列
ALTER TABLE monitor_type
    DROP COLUMN category_id;

-- 3. 删除 monitor_category 表
DROP TABLE IF EXISTS monitor_category;

-- 4. 移除 monitorCategory 菜单权限记录
DELETE
FROM sys_menu
WHERE perms LIKE 'basic:monitorCategory:%';
