-- =====================================================================
-- v2.12-dynamic-menu.sql — 动态菜单栏数据迁移
--
-- 删除所有旧菜单数据，重建为项目顶部导航栏所需的精确结构。
-- 前端 layout 从 getMenuTree API 动态加载菜单，
-- route_name 字段作为 vue-router 路由名（router.push({ name })）。
--
-- 目标结构（6 个顶级目录 + 25 个子菜单）:
--   全息看板 → 综合视图 / 告警视图 / 运营视图
--   基础管理 → 隐患点管理
--   告警中心 → 待办告警 / 历史告警 / 告警判据 / 综合告警 / 算法管理 / 通知设置
--   报告报表 → 报告管理 / 查询中心 / 数据分析 / 共享策略
--   物联网   → 监测类型 / 设备管理 / 视频设备 / 数据解析 / 服务状态
--   系统管理 → 组织管理 / 身份管理 / 权限管理 / 日志管理 / 系统设置
-- =====================================================================

-- =====================================================================
-- Phase 1: 清除所有旧菜单数据
-- =====================================================================

-- 清除角色-菜单关联
DELETE FROM sys_role_menu;

-- 清除所有菜单（F 按钮权限一并删除）
DELETE FROM sys_menu;
-- 重置自增计数器，确保新 ID 从 1 开始
ALTER TABLE sys_menu AUTO_INCREMENT = 1;

-- =====================================================================
-- Phase 2: 重建顶级目录（6 个 M 类型，parent_id = 0）
-- =====================================================================

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES
    ('全息看板', 0, 1, 'dashboard', '', 1, 0, 'M', '0', '0', '', 'dashboard',      'admin', NOW(), ''),
    ('基础管理', 0, 2, 'basic',     '', 1, 0, 'M', '0', '0', '', 'guide',          'admin', NOW(), ''),
    ('告警中心', 0, 3, 'alarm',     '', 1, 0, 'M', '0', '0', '', 'alarm',          'admin', NOW(), ''),
    ('报告报表', 0, 4, 'report',    '', 1, 0, 'M', '0', '0', '', 'documentation',  'admin', NOW(), ''),
    ('物联网',   0, 5, 'iot',       '', 1, 0, 'M', '0', '0', '', 'sensor',         'admin', NOW(), ''),
    ('系统管理', 0, 6, 'system',    '', 1, 0, 'M', '0', '0', '', 'system',         'admin', NOW(), '');

-- 基于 AUTO_INCREMENT 从 1 开始，ID 按插入顺序分配
SET @m1 = 1;
SET @m2 = 2;
SET @m3 = 3;
SET @m4 = 4;
SET @m5 = 5;
SET @m6 = 6;

-- =====================================================================
-- Phase 3: 重建子菜单（25 个 C 类型，route_name 匹配 vue-router）
-- =====================================================================

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES
    -- ========== 全息看板 ==========
    ('综合视图', @m1, 1, 'holo-board/comprehensive', 'Comprehensive', 1, 0, 'C', '0', '0', 'holo:comprehensive:view', 'chart', 'admin', NOW(), ''),
    ('告警视图', @m1, 2, 'holo-board/alarm',         'Alarm',         1, 0, 'C', '0', '0', 'holo:alarm:view',        'alarm', 'admin', NOW(), ''),
    ('运营视图', @m1, 3, 'holo-board/operation',      'Operation',     1, 0, 'C', '0', '0', 'holo:operation:view',     'online', 'admin', NOW(), ''),

    -- ========== 基础管理 ==========
    ('隐患点管理', @m2, 1, 'basic/hazard-point',     'HazardPoint',   1, 0, 'C', '0', '0', 'basic:hazardPoint:list',  'location', 'admin', NOW(), ''),

    -- ========== 告警中心 ==========
    ('待办告警',   @m3, 1, 'alarm/realtime',              'RealtimeAlarm',       1, 0, 'C', '0', '0', 'alarm:realtime:list',          'bell',          'admin', NOW(), ''),
    ('历史告警',   @m3, 2, 'alarm/notification',          'AlarmNotification',    1, 0, 'C', '0', '0', 'alarm:notification:list',      'documentation', 'admin', NOW(), ''),
    ('告警判据',   @m3, 3, 'alarm/criteria',              'AlarmCriteria',        1, 0, 'C', '0', '0', 'iot:alarm-criteria:list',      'rule',          'admin', NOW(), ''),
    ('综合告警',   @m3, 4, 'alarm/disposal',              'AlarmDisposal',        1, 0, 'C', '0', '0', 'iot:alarm-strategy:list',      'component',     'admin', NOW(), ''),
    ('算法管理',   @m3, 5, 'alarm/algo-library',          'AlgoLibrary',          1, 0, 'C', '0', '0', 'alarm:algo:list',               'tool',          'admin', NOW(), ''),
    ('通知设置',   @m3, 6, 'alarm/notification-setting',  'NotificationSetting',  1, 0, 'C', '0', '0', 'alarm:notification:setting',    'edit',          'admin', NOW(), ''),

    -- ========== 报告报表 ==========
    ('报告管理',   @m4, 1, 'report/report',          'Report',         1, 0, 'C', '0', '0', 'report:record:list',        'documentation', 'admin', NOW(), ''),
    ('查询中心',   @m4, 2, 'report/query',           'Query',          1, 0, 'C', '0', '0', 'report:query:list',         'eye',           'admin', NOW(), ''),
    ('数据分析',   @m4, 3, 'report/analysis',        'Analysis',       1, 0, 'C', '0', '0', 'report:analysis:list',      'chart',         'admin', NOW(), ''),
    ('共享策略',   @m4, 4, 'report/share-strategy',  'ShareStrategy',  1, 0, 'C', '0', '0', 'report:shareStrategy:list', 'link',          'admin', NOW(), ''),

    -- ========== 物联网 ==========
    ('监测类型',   @m5, 1, 'basic/monitor-type',     'MonitorType',    1, 0, 'C', '0', '0', 'basic:monitorType:list',    'component',     'admin', NOW(), ''),
    ('设备管理',   @m5, 2, 'basic/device',           'Device',         1, 0, 'C', '0', '0', 'basic:device:list',         'monitor',       'admin', NOW(), ''),
    ('视频设备',   @m5, 3, 'basic/video-device',     'VideoDevice',    1, 0, 'C', '0', '0', 'basic:videoDevice:list',    'video',         'admin', NOW(), ''),
    ('数据解析',   @m5, 4, 'iot/data-parse',         'DataParse',      1, 0, 'C', '0', '0', 'iot:dataParse:list',        'tool',          'admin', NOW(), ''),
    ('服务状态',   @m5, 5, 'iot/service-status',     'ServiceStatus',  1, 0, 'C', '0', '0', 'iot:serviceStatus:list',    'server',        'admin', NOW(), ''),

    -- ========== 系统管理 ==========
    ('组织管理',   @m6, 1, 'system/organization',    'Organization',   1, 0, 'C', '0', '0', 'system:organization:list',   'tree',          'admin', NOW(), ''),
    ('身份管理',   @m6, 2, 'system/identity',        'Identity',       1, 0, 'C', '0', '0', 'system:identity:list',       'peoples',       'admin', NOW(), ''),
    ('权限管理',   @m6, 3, 'system/permission',      'Permission',     1, 0, 'C', '0', '0', 'system:permission:list',     'system',        'admin', NOW(), ''),
    ('日志管理',   @m6, 4, 'system/log',             'Log',            1, 0, 'C', '0', '0', 'system:log:list',            'log',           'admin', NOW(), ''),
    ('系统设置',   @m6, 5, 'system/settings',        'Settings',       1, 0, 'C', '0', '0', 'system:settings:list',       'edit',          'admin', NOW(), '');

-- =====================================================================
-- Phase 4: 重置 AUTO_INCREMENT
-- =====================================================================
-- 当前最大 menu_id = 31（6 个 M + 25 个 C），重置自增从 100 开始留有余量
ALTER TABLE sys_menu AUTO_INCREMENT = 100;

-- =====================================================================
-- Phase 5: 授予 admin 角色 (role_id=1) 所有菜单
-- =====================================================================
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu;

-- =====================================================================
-- Phase 6: 验证查询（执行后手动检查）
-- =====================================================================
-- 查看完整菜单树:
-- SELECT m.menu_id,
--        CASE WHEN m.parent_id = 0 THEN '---' ELSE p.menu_name END AS parent,
--        m.menu_name,
--        m.menu_type,
--        m.order_num,
--        m.route_name,
--        m.path,
--        m.icon,
--        m.perms
-- FROM sys_menu m
-- LEFT JOIN sys_menu p ON m.parent_id = p.menu_id
-- ORDER BY m.parent_id, m.order_num;
--
-- 预期结果: 6 个 M (parent_id=0) + 25 个 C = 31 条记录
-- =====================================================================
