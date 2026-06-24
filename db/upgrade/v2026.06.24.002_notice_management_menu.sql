-- =====================================================================
-- v2026.06.24.002_notice_management_menu.sql — 系统管理组新增「通知公告」菜单
--
-- 背景：
--   - 后端 SysNoticeController（/api/v1/system/notice）早已存在
--     · list / add / edit / listTop / markRead / markReadAll / readUsers
--   - 前端 /system/notice 路由 + SysNotice.vue 页面早已存在
--   - 缺失：sys_menu 表中没有对应入口，用户只能通过顶部铃铛查看，无法 CRUD 管理
--
-- 本次升级：
--   1. 在「系统管理」(M 父菜单) 下注册 C 类菜单「通知公告」
--      route_name = 'SysNotice'  ← 对齐 vue-router 的 name
--      path       = 'system/notice'
--      perms      = 'system:notice:list'
--   2. 注册 3 个 F 类按钮权限（与后端 @PreAuthorize 实际校验一致）：
--      system:notice:add / system:notice:edit
--      （后端未实现 DELETE 端点，故不注册 :remove）
--   3. 授予 admin 角色（role_id = 1）
--
-- 幂等：所有 INSERT 使用 WHERE NOT EXISTS，可重复执行。
-- =====================================================================

-- ---------------------------------------------------------------------
-- Step 1: 查询「系统管理」父菜单 ID（防御性：名字匹配 + M 类型）
-- ---------------------------------------------------------------------
SET @sysParentId = (
    SELECT menu_id FROM sys_menu
    WHERE menu_name = '系统管理' AND menu_type = 'M'
    LIMIT 1
);

-- ---------------------------------------------------------------------
-- Step 2: 主菜单（C 类型）—— 对齐 v2.12 动态菜单字段约定
--   order_num = 6：排在 Settings (5) 之后
-- ---------------------------------------------------------------------
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '通知公告', @sysParentId, 6, 'system/notice', 'SysNotice', 1, 0,
       'C', '0', '0', 'system:notice:list', 'message', 'admin', NOW(),
       '通知公告管理（CRUD + 已读追踪）'
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menu WHERE route_name = 'SysNotice' AND menu_type = 'C'
);

-- ---------------------------------------------------------------------
-- Step 3: 按钮权限（F 类型）
-- ---------------------------------------------------------------------
SET @noticeMenuId = (
    SELECT menu_id FROM sys_menu
    WHERE route_name = 'SysNotice' AND menu_type = 'C'
    LIMIT 1
);

INSERT INTO sys_menu (menu_name, parent_id, menu_type, perms, create_time)
SELECT '公告新增', @noticeMenuId, 'F', 'system:notice:add', NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:notice:add');

INSERT INTO sys_menu (menu_name, parent_id, menu_type, perms, create_time)
SELECT '公告编辑', @noticeMenuId, 'F', 'system:notice:edit', NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:notice:edit');

-- ---------------------------------------------------------------------
-- Step 4: 授予 admin 角色（role_id = 1）
--   覆盖 C 菜单本体 + 2 个 F 按钮
-- ---------------------------------------------------------------------
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu
WHERE perms IN ('system:notice:list', 'system:notice:add', 'system:notice:edit')
  AND menu_id NOT IN (SELECT menu_id FROM sys_role_menu WHERE role_id = 1);

-- =====================================================================
-- 验证查询（执行后手动检查）：
--
-- SELECT m.menu_id, p.menu_name AS parent, m.menu_name, m.menu_type,
--        m.order_num, m.route_name, m.path, m.icon, m.perms
-- FROM sys_menu m
-- LEFT JOIN sys_menu p ON m.parent_id = p.menu_id
-- WHERE m.route_name = 'SysNotice' OR m.perms LIKE 'system:notice:%'
-- ORDER BY m.menu_type, m.menu_id;
--
-- 预期：1 条 C（通知公告）+ 2 条 F（公告新增/公告编辑）= 3 条记录
-- =====================================================================
