-- 通知规则菜单按钮权限
-- 假设 alarm 父菜单 menu_id 在每个环境不同，先用变量查询
SET @alarmParentId = (SELECT menu_id FROM sys_menu
                       WHERE menu_name = '告警管理' AND menu_type = 'M' LIMIT 1);
-- 如果实际父菜单名不同（如 "告警中心"），实现时调整

INSERT INTO sys_menu(menu_name, parent_id, order_num, path, component, menu_type,
                     perms, icon, create_time)
SELECT '通知规则', @alarmParentId, 30, 'notification-setting',
       'alarm/NotificationSetting', 'C', 'alarm:dispatch:list', 'bell',
       NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menu WHERE perms = 'alarm:dispatch:list' AND menu_type = 'C'
);

SET @ruleMenuId = (SELECT menu_id FROM sys_menu WHERE perms = 'alarm:dispatch:list' LIMIT 1);

INSERT INTO sys_menu(menu_name, parent_id, menu_type, perms, create_time)
SELECT '通知规则新增', @ruleMenuId, 'F', 'alarm:dispatch:add', NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'alarm:dispatch:add');

INSERT INTO sys_menu(menu_name, parent_id, menu_type, perms, create_time)
SELECT '通知规则编辑', @ruleMenuId, 'F', 'alarm:dispatch:edit', NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'alarm:dispatch:edit');

INSERT INTO sys_menu(menu_name, parent_id, menu_type, perms, create_time)
SELECT '通知规则删除', @ruleMenuId, 'F', 'alarm:dispatch:remove', NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'alarm:dispatch:remove');

-- 给 admin 角色绑定（role_id=1）
INSERT INTO sys_role_menu(role_id, menu_id)
SELECT 1, menu_id FROM sys_menu
WHERE perms LIKE 'alarm:dispatch:%'
  AND menu_id NOT IN (SELECT menu_id FROM sys_role_menu WHERE role_id = 1);
