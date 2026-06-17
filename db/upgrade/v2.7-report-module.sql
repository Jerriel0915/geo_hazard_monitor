-- =====================================================================
-- v2.7-report-module.sql — 报告管理模块(周/月/季报)
-- 关联规格: docs/superpowers/specs/2026-06-17-report-management-design.md
-- =====================================================================

-- 1. report_record 表扩展: 加类型/周期/逻辑删除/失败原因
ALTER TABLE report_record
    ADD COLUMN type         tinyint       NOT NULL COMMENT '报告类型: 2-周报, 3-月报, 4-季报' AFTER report_name,
    ADD COLUMN period_start date          NOT NULL COMMENT '周期开始日 (含)' AFTER type,
    ADD COLUMN period_end   date          NOT NULL COMMENT '周期结束日 (含)' AFTER period_start,
    ADD COLUMN error_msg    varchar(1000) DEFAULT NULL COMMENT '生成失败原因 (status=3 时填)' AFTER status,
    ADD COLUMN del_flag     tinyint       NOT NULL DEFAULT 0 COMMENT '删除标记: 0-正常, 1-删除' AFTER error_msg,
    MODIFY COLUMN template_id bigint DEFAULT NULL COMMENT '模板ID (内置渲染器填 NULL)',
    MODIFY COLUMN status tinyint DEFAULT '1' COMMENT '状态: 1-生成中, 2-已生成, 3-生成失败',
    ADD KEY idx_report_record_type (type),
    ADD KEY idx_report_record_period (period_start, period_end),
    ADD KEY idx_report_record_del_flag (del_flag);

-- 2. 防重复生成: 每个周期同一隐患点同一类型只允许一条有效记录
ALTER TABLE report_record
    ADD UNIQUE KEY uk_report_record_unique
        (type, hazard_point_id, period_start, period_end, del_flag);

-- 3. 顶级菜单 "报告报表" (幂等)
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '报告报表', 0, 7, 'report', NULL, '', 1, 0, 'M', '0', '0', '', 'documentation',
       'admin', NOW(), '报告报表目录'
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name = '报告报表' AND parent_id = 0 AND menu_type = 'M');

SET @report_parent_id = (SELECT menu_id FROM sys_menu
                         WHERE menu_name = '报告报表' AND parent_id = 0 AND menu_type = 'M' LIMIT 1);

-- 4. 子菜单 "报告管理" (幂等)
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '报告管理', @report_parent_id, 1, 'report', 'report/Report', '', 1, 0, 'C', '0', '0',
       'report:record:list', 'documentation', 'admin', NOW(), '报告管理菜单'
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'report:record:list' AND menu_type = 'C');

SET @report_menu_id = (SELECT menu_id FROM sys_menu WHERE perms = 'report:record:list' AND menu_type = 'C' LIMIT 1);

-- 5. 按钮权限 (幂等)
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT * FROM (
  SELECT '报告查询' AS n, @report_menu_id AS pid, 1 AS o, '' AS p, '' AS c, '' AS q, 1 AS f, 0 AS cache,
         'F' AS t, '0' AS v, '0' AS s, 'report:record:query' AS perm, '#' AS icon, 'admin', NOW(), '' AS rem
  UNION ALL SELECT '报告删除', @report_menu_id, 2, '', '', '', 1, 0, 'F', '0', '0', 'report:record:remove', '#', 'admin', NOW(), ''
  UNION ALL SELECT '报告导出', @report_menu_id, 3, '', '', '', 1, 0, 'F', '0', '0', 'report:record:export', '#', 'admin', NOW(), ''
  UNION ALL SELECT '报告生成', @report_menu_id, 4, '', '', '', 1, 0, 'F', '0', '0', 'report:record:generate', '#', 'admin', NOW(), ''
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = tmp.perm AND menu_type = 'F');

-- 6. 给 admin 角色 (role_id=1) 自动授权新菜单 (含父级目录)
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id FROM sys_menu m
WHERE (m.menu_id = @report_parent_id
       OR m.perms IN ('report:record:list', 'report:record:query', 'report:record:remove',
                      'report:record:export', 'report:record:generate'))
  AND m.menu_id NOT IN (SELECT menu_id FROM sys_role_menu WHERE role_id = 1);
