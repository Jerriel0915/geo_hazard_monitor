-- 通知中心（事件 Tab）菜单按钮权限
-- 说明：通知中心通过顶部铃铛入口访问（无独立路由），因此只注册 F 类按钮权限，
--      挂在"告警管理"父菜单下；同时为 admin 角色（role_id=1）授权。
SET @alarmParentId = (SELECT menu_id FROM sys_menu
                       WHERE menu_name = '告警管理' AND menu_type = 'M' LIMIT 1);

-- 通知列表（铃铛打开 + 事件 Tab 拉取列表）
INSERT INTO sys_menu(menu_name, parent_id, menu_type, perms, create_time)
SELECT '通知列表', @alarmParentId, 'F', 'alarm:notification:list', NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'alarm:notification:list');

-- 通知标记已读（单条 + 全部）
INSERT INTO sys_menu(menu_name, parent_id, menu_type, perms, create_time)
SELECT '通知标记已读', @alarmParentId, 'F', 'alarm:notification:read', NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'alarm:notification:read');

-- 给 admin 角色绑定（role_id=1）
INSERT INTO sys_role_menu(role_id, menu_id)
SELECT 1, menu_id FROM sys_menu
WHERE perms IN ('alarm:notification:list', 'alarm:notification:read')
  AND menu_id NOT IN (SELECT menu_id FROM sys_role_menu WHERE role_id = 1);
