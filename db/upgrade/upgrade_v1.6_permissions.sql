-- ============================================
-- upgrade_v1.6_permissions.sql
-- 权限控制完善：补齐缺失的菜单权限记录
-- 日期：2026-06-03
-- 适用于：geo_hazard_monitor
-- ============================================

-- 1. 新增菜单权限记录（sys_menu）
INSERT INTO `sys_menu`
VALUES
-- MQTT 监控主菜单（归属「系统监控」目录，parent_id=2）
(2006, 'MQTT监控', 2, 7, 'mqtt-monitor', 'iot/ServiceStatus', '',
 '', 1, 0, 'C', '0', '0', 'monitor:mqtt:list', 'monitor',
 'admin', '2026-06-03 10:00:00', '', NULL, 'MQTT服务器监控'),
-- MQTT 踢出按钮
(2007, 'MQTT踢出', 2006, 1, '', '', '', '',
 1, 0, 'F', '0', '0', 'monitor:mqtt:kick', '#',
 'admin', '2026-06-03 10:00:00', '', NULL, ''),
-- 监控总览（归属「系统监控」目录）
(2008, '监控总览', 2, 0, 'monitor-overview', 'monitor/overview', '',
 '', 1, 0, 'C', '0', '0', 'monitor:overview:list', 'dashboard',
 'admin', '2026-06-03 10:00:00', '', NULL, '系统监控总览'),
-- 日志查询（归属「系统监控」→「日志管理」分区，parent_id=108）
(2009, '日志查询', 108, 3, 'log-query', 'monitor/log-query/index', '',
 '', 1, 0, 'C', '0', '0', 'monitor:operlog:list', 'log',
 'admin', '2026-06-03 10:00:00', '', NULL, '操作/认证/运行日志查询'),
-- 文件上传（归属「系统工具」目录，parent_id=3）
(2010, '文件上传', 3, 4, 'file-upload', '', '',
 '', 1, 0, 'F', '0', '0', 'common:file:upload', 'upload',
 'admin', '2026-06-03 10:00:00', '', NULL, ''),
-- 文件下载
(2011, '文件下载', 3, 5, 'file-download', '', '',
 '', 1, 0, 'F', '0', '0', 'common:file:query', 'download',
 'admin', '2026-06-03 10:00:00', '', NULL, '');

-- 2. 为「普通角色」(role_id=2) 分配新菜单权限
INSERT INTO `sys_role_menu` (role_id, menu_id)
VALUES (2, 2006),
       (2, 2007),
       (2, 2008),
       (2, 2009),
       (2, 2010),
       (2, 2011);

-- 3. 为「监测管理员」(role_id=100) 分配 MQTT 监控权限
INSERT INTO `sys_role_menu` (role_id, menu_id)
VALUES (100, 2006),
       (100, 2007),
       (100, 2008),
       (100, 2009);
