-- ===============================================================
-- 地质灾害监测预警系统 - 数据库结构设计 V1.1
-- 数据库: geo_hazard_monitor
-- 版本: MySQL 8.0+
-- 字符集: utf8mb4
-- 引擎: InnoDB
-- 日期: 2026-05-08
-- 设计说明:
--   - 系统表采用若依框架(RuoYi)标准结构
--   - 业务表采用新版设计
--   - 创建人字段使用 create_by/update_by 格式
-- ===============================================================

USE `geo_hazard_monitor`;

-- ===============================================================
-- 一、系统管理模块 (基于若依框架)
-- ===============================================================

-- ----------------------------
-- 1.1 部门表 (sys_dept)
-- ----------------------------
DROP TABLE IF EXISTS `sys_dept`;
CREATE TABLE `sys_dept`
(
    `dept_id`     bigint(20) NOT NULL AUTO_INCREMENT COMMENT '部门id',
    `parent_id`   bigint(20)  DEFAULT 0 COMMENT '父部门id',
    `ancestors`   varchar(50) DEFAULT '' COMMENT '祖级列表',
    `dept_name`   varchar(30) DEFAULT '' COMMENT '部门名称',
    `order_num`   int(4)      DEFAULT 0 COMMENT '显示顺序',
    `leader`      varchar(20) DEFAULT NULL COMMENT '负责人',
    `phone`       varchar(11) DEFAULT NULL COMMENT '联系电话',
    `email`       varchar(50) DEFAULT NULL COMMENT '邮箱',
    `status`      char(1)     DEFAULT '0' COMMENT '部门状态（0正常 1停用）',
    `del_flag`    char(1)     DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
    `create_by`   varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime    DEFAULT NULL COMMENT '创建时间',
    `update_by`   varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime    DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`dept_id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 200 COMMENT ='部门表';

-- ----------------------------
-- 初始化-部门表数据
-- ----------------------------
INSERT INTO `sys_dept`
VALUES (100, 0, '0', '若依科技', 0, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', sysdate(), '', null);
INSERT INTO `sys_dept`
VALUES (101, 100, '0,100', '深圳总公司', 1, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', sysdate(), '', null);
INSERT INTO `sys_dept`
VALUES (102, 100, '0,100', '长沙分公司', 2, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', sysdate(), '', null);
INSERT INTO `sys_dept`
VALUES (103, 101, '0,100,101', '研发部门', 1, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', sysdate(), '',
        null);
INSERT INTO `sys_dept`
VALUES (104, 101, '0,100,101', '市场部门', 2, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', sysdate(), '',
        null);
INSERT INTO `sys_dept`
VALUES (105, 101, '0,100,101', '测试部门', 3, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', sysdate(), '',
        null);
INSERT INTO `sys_dept`
VALUES (106, 101, '0,100,101', '财务部门', 4, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', sysdate(), '',
        null);
INSERT INTO `sys_dept`
VALUES (107, 101, '0,100,101', '运维部门', 5, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', sysdate(), '',
        null);
INSERT INTO `sys_dept`
VALUES (108, 102, '0,100,102', '市场部门', 1, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', sysdate(), '',
        null);
INSERT INTO `sys_dept`
VALUES (109, 102, '0,100,102', '财务部门', 2, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', sysdate(), '',
        null);

-- ----------------------------
-- 1.2 用户信息表 (sys_user)
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`
(
    `user_id`         bigint(20)  NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `dept_id`         bigint(20)   DEFAULT NULL COMMENT '部门ID',
    `user_name`       varchar(30) NOT NULL COMMENT '用户账号',
    `nick_name`       varchar(30) NOT NULL COMMENT '用户昵称',
    `user_type`       varchar(2)   DEFAULT '00' COMMENT '用户类型（00系统用户）',
    `email`           varchar(50)  DEFAULT '' COMMENT '用户邮箱',
    `phonenumber`     varchar(11)  DEFAULT '' COMMENT '手机号码',
    `sex`             char(1)      DEFAULT '0' COMMENT '用户性别（0男 1女 2未知）',
    `avatar`          varchar(100) DEFAULT '' COMMENT '头像地址',
    `password`        varchar(100) DEFAULT '' COMMENT '密码',
    `status`          char(1)      DEFAULT '0' COMMENT '账号状态（0正常 1停用）',
    `del_flag`        char(1)      DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
    `login_ip`        varchar(128) DEFAULT '' COMMENT '最后登录IP',
    `login_date`      datetime     DEFAULT NULL COMMENT '最后登录时间',
    `pwd_update_date` datetime     DEFAULT NULL COMMENT '密码最后更新时间',
    `create_by`       varchar(64)  DEFAULT '' COMMENT '创建者',
    `create_time`     datetime     DEFAULT NULL COMMENT '创建时间',
    `update_by`       varchar(64)  DEFAULT '' COMMENT '更新者',
    `update_time`     datetime     DEFAULT NULL COMMENT '更新时间',
    `remark`          varchar(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`user_id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 100 COMMENT ='用户信息表';

-- ----------------------------
-- 初始化-用户信息表数据
-- ----------------------------
INSERT INTO `sys_user`
VALUES (1, 103, 'admin', '若依', '00', 'ry@163.com', '15888888888', '1', '',
        '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '127.0.0.1', sysdate(), sysdate(),
        'admin', sysdate(), '', null, '管理员');
INSERT INTO `sys_user`
VALUES (2, 105, 'ry', '若依', '00', 'ry@qq.com', '15666666666', '1', '',
        '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '127.0.0.1', sysdate(), sysdate(),
        'admin', sysdate(), '', null, '测试员');

-- ----------------------------
-- 1.3 岗位信息表 (sys_post)
-- ----------------------------
DROP TABLE IF EXISTS `sys_post`;
CREATE TABLE `sys_post`
(
    `post_id`     bigint(20)  NOT NULL AUTO_INCREMENT COMMENT '岗位ID',
    `post_code`   varchar(64) NOT NULL COMMENT '岗位编码',
    `post_name`   varchar(50) NOT NULL COMMENT '岗位名称',
    `post_sort`   int(4)      NOT NULL COMMENT '显示顺序',
    `status`      char(1)     NOT NULL COMMENT '状态（0正常 1停用）',
    `create_by`   varchar(64)  DEFAULT '' COMMENT '创建者',
    `create_time` datetime     DEFAULT NULL COMMENT '创建时间',
    `update_by`   varchar(64)  DEFAULT '' COMMENT '更新者',
    `update_time` datetime     DEFAULT NULL COMMENT '更新时间',
    `remark`      varchar(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`post_id`)
) ENGINE = InnoDB COMMENT ='岗位信息表';

INSERT INTO `sys_post`
VALUES (1, 'ceo', '董事长', 1, '0', 'admin', sysdate(), '', null, '');
INSERT INTO `sys_post`
VALUES (2, 'se', '项目经理', 2, '0', 'admin', sysdate(), '', null, '');
INSERT INTO `sys_post`
VALUES (3, 'hr', '人力资源', 3, '0', 'admin', sysdate(), '', null, '');
INSERT INTO `sys_post`
VALUES (4, 'user', '普通员工', 4, '0', 'admin', sysdate(), '', null, '');

-- ----------------------------
-- 1.4 角色信息表 (sys_role)
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role`
(
    `role_id`             bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '角色ID',
    `role_name`           varchar(30)  NOT NULL COMMENT '角色名称',
    `role_key`            varchar(100) NOT NULL COMMENT '角色权限字符串',
    `role_sort`           int(4)       NOT NULL COMMENT '显示顺序',
    `data_scope`          char(1)      DEFAULT '1' COMMENT '数据范围（1：全部数据权限 2：自定数据权限 3：本部门数据权限 4：本部门及以下数据权限）',
    `menu_check_strictly` tinyint(1)   DEFAULT 1 COMMENT '菜单树选择项是否关联显示',
    `dept_check_strictly` tinyint(1)   DEFAULT 1 COMMENT '部门树选择项是否关联显示',
    `status`              char(1)      NOT NULL COMMENT '角色状态（0正常 1停用）',
    `del_flag`            char(1)      DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
    `create_by`           varchar(64)  DEFAULT '' COMMENT '创建者',
    `create_time`         datetime     DEFAULT NULL COMMENT '创建时间',
    `update_by`           varchar(64)  DEFAULT '' COMMENT '更新者',
    `update_time`         datetime     DEFAULT NULL COMMENT '更新时间',
    `remark`              varchar(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`role_id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 100 COMMENT ='角色信息表';

INSERT INTO `sys_role`
VALUES (1, '超级管理员', 'admin', 1, 1, 1, 1, '0', '0', 'admin', sysdate(), '', null, '超级管理员');
INSERT INTO `sys_role`
VALUES (2, '普通角色', 'common', 2, 2, 1, 1, '0', '0', 'admin', sysdate(), '', null, '普通角色');

-- ----------------------------
-- 1.5 菜单权限表 (sys_menu)
-- ----------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu`
(
    `menu_id`     bigint(20)  NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
    `menu_name`   varchar(50) NOT NULL COMMENT '菜单名称',
    `parent_id`   bigint(20)   DEFAULT 0 COMMENT '父菜单ID',
    `order_num`   int(4)       DEFAULT 0 COMMENT '显示顺序',
    `path`        varchar(200) DEFAULT '' COMMENT '路由地址',
    `component`   varchar(255) DEFAULT NULL COMMENT '组件路径',
    `query`       varchar(255) DEFAULT NULL COMMENT '路由参数',
    `route_name`  varchar(50)  DEFAULT '' COMMENT '路由名称',
    `is_frame`    int(1)       DEFAULT 1 COMMENT '是否为外链（0是 1否）',
    `is_cache`    int(1)       DEFAULT 0 COMMENT '是否缓存（0缓存 1不缓存）',
    `menu_type`   char(1)      DEFAULT '' COMMENT '菜单类型（M目录 C菜单 F按钮）',
    `visible`     char(1)      DEFAULT 0 COMMENT '菜单状态（0显示 1隐藏）',
    `status`      char(1)      DEFAULT 0 COMMENT '菜单状态（0正常 1停用）',
    `perms`       varchar(100) DEFAULT NULL COMMENT '权限标识',
    `icon`        varchar(100) DEFAULT '#' COMMENT '菜单图标',
    `create_by`   varchar(64)  DEFAULT '' COMMENT '创建者',
    `create_time` datetime     DEFAULT NULL COMMENT '创建时间',
    `update_by`   varchar(64)  DEFAULT '' COMMENT '更新者',
    `update_time` datetime     DEFAULT NULL COMMENT '更新时间',
    `remark`      varchar(500) DEFAULT '' COMMENT '备注',
    PRIMARY KEY (`menu_id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 2000 COMMENT ='菜单权限表';

-- 一级菜单
INSERT INTO `sys_menu`
VALUES (1, '系统管理', 0, 1, 'system', null, '', '', 1, 0, 'M', '0', '0', '', 'system', 'admin', sysdate(), '', null,
        '系统管理目录');
INSERT INTO `sys_menu`
VALUES (2, '系统监控', 0, 2, 'monitor', null, '', '', 1, 0, 'M', '0', '0', '', 'monitor', 'admin', sysdate(), '', null,
        '系统监控目录');
INSERT INTO `sys_menu`
VALUES (3, '系统工具', 0, 3, 'tool', null, '', '', 1, 0, 'M', '0', '0', '', 'tool', 'admin', sysdate(), '', null,
        '系统工具目录');
INSERT INTO `sys_menu`
VALUES (4, '若依官网', 0, 4, 'http://zwei.vip', null, '', '', 0, 0, 'M', '0', '0', '', 'guide', 'admin', sysdate(), '',
        null, '若依官网地址');
-- 二级菜单
INSERT INTO `sys_menu`
VALUES (100, '用户管理', 1, 1, 'user', 'system/user/index', '', '', 1, 0, 'C', '0', '0', 'system:user:list', 'user',
        'admin', sysdate(), '', null, '用户管理菜单');
INSERT INTO `sys_menu`
VALUES (101, '角色管理', 1, 2, 'role', 'system/role/index', '', '', 1, 0, 'C', '0', '0', 'system:role:list', 'peoples',
        'admin', sysdate(), '', null, '角色管理菜单');
INSERT INTO `sys_menu`
VALUES (102, '菜单管理', 1, 3, 'menu', 'system/menu/index', '', '', 1, 0, 'C', '0', '0', 'system:menu:list',
        'tree-table', 'admin', sysdate(), '', null, '菜单管理菜单');
INSERT INTO `sys_menu`
VALUES (103, '部门管理', 1, 4, 'dept', 'system/dept/index', '', '', 1, 0, 'C', '0', '0', 'system:dept:list', 'tree',
        'admin', sysdate(), '', null, '部门管理菜单');
INSERT INTO `sys_menu`
VALUES (104, '岗位管理', 1, 5, 'post', 'system/post/index', '', '', 1, 0, 'C', '0', '0', 'system:post:list', 'post',
        'admin', sysdate(), '', null, '岗位管理菜单');
INSERT INTO `sys_menu`
VALUES (105, '字典管理', 1, 6, 'dict', 'system/dict/index', '', '', 1, 0, 'C', '0', '0', 'system:dict:list', 'dict',
        'admin', sysdate(), '', null, '字典管理菜单');
INSERT INTO `sys_menu`
VALUES (106, '参数设置', 1, 7, 'config', 'system/config/index', '', '', 1, 0, 'C', '0', '0', 'system:config:list',
        'edit', 'admin', sysdate(), '', null, '参数设置菜单');
INSERT INTO `sys_menu`
VALUES (107, '通知公告', 1, 8, 'notice', 'system/notice/index', '', '', 1, 0, 'C', '0', '0', 'system:notice:list',
        'message', 'admin', sysdate(), '', null, '通知公告菜单');
INSERT INTO `sys_menu`
VALUES (108, '日志管理', 1, 9, 'log', '', '', '', 1, 0, 'M', '0', '0', '', 'log', 'admin', sysdate(), '', null,
        '日志管理菜单');
INSERT INTO `sys_menu`
VALUES (109, '在线用户', 2, 1, 'online', 'monitor/online/index', '', '', 1, 0, 'C', '0', '0', 'monitor:online:list',
        'online', 'admin', sysdate(), '', null, '在线用户菜单');
INSERT INTO `sys_menu`
VALUES (110, '定时任务', 2, 2, 'job', 'monitor/job/index', '', '', 1, 0, 'C', '0', '0', 'monitor:job:list', 'job',
        'admin', sysdate(), '', null, '定时任务菜单');
INSERT INTO `sys_menu`
VALUES (111, '数据监控', 2, 3, 'druid', 'monitor/druid/index', '', '', 1, 0, 'C', '0', '0', 'monitor:druid:list',
        'druid', 'admin', sysdate(), '', null, '数据监控菜单');
INSERT INTO `sys_menu`
VALUES (112, '服务监控', 2, 4, 'server', 'monitor/server/index', '', '', 1, 0, 'C', '0', '0', 'monitor:server:list',
        'server', 'admin', sysdate(), '', null, '服务监控菜单');
INSERT INTO `sys_menu`
VALUES (113, '缓存监控', 2, 5, 'cache', 'monitor/cache/index', '', '', 1, 0, 'C', '0', '0', 'monitor:cache:list',
        'redis', 'admin', sysdate(), '', null, '缓存监控菜单');
INSERT INTO `sys_menu`
VALUES (114, '缓存列表', 2, 6, 'cacheList', 'monitor/cache/list', '', '', 1, 0, 'C', '0', '0', 'monitor:cache:list',
        'redis-list', 'admin', sysdate(), '', null, '缓存列表菜单');
INSERT INTO `sys_menu`
VALUES (115, '表单构建', 3, 1, 'build', 'tool/build/index', '', '', 1, 0, 'C', '0', '0', 'tool:build:list', 'build',
        'admin', sysdate(), '', null, '表单构建菜单');
INSERT INTO `sys_menu`
VALUES (116, '代码生成', 3, 2, 'gen', 'tool/gen/index', '', '', 1, 0, 'C', '0', '0', 'tool:gen:list', 'code', 'admin',
        sysdate(), '', null, '代码生成菜单');
INSERT INTO `sys_menu`
VALUES (117, '系统接口', 3, 3, 'swagger', 'tool/swagger/index', '', '', 1, 0, 'C', '0', '0', 'tool:swagger:list',
        'swagger', 'admin', sysdate(), '', null, '系统接口菜单');
-- 三级菜单
INSERT INTO `sys_menu`
VALUES (500, '操作日志', 108, 1, 'operlog', 'monitor/operlog/index', '', '', 1, 0, 'C', '0', '0',
        'monitor:operlog:list', 'form', 'admin', sysdate(), '', null, '操作日志菜单');
INSERT INTO `sys_menu`
VALUES (501, '登录日志', 108, 2, 'logininfor', 'monitor/logininfor/index', '', '', 1, 0, 'C', '0', '0',
        'monitor:logininfor:list', 'logininfor', 'admin', sysdate(), '', null, '登录日志菜单');
-- 用户管理按钮
INSERT INTO `sys_menu`
VALUES (1000, '用户查询', 100, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:query', '#', 'admin', sysdate(), '',
        null, '');
INSERT INTO `sys_menu`
VALUES (1001, '用户新增', 100, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:add', '#', 'admin', sysdate(), '',
        null, '');
INSERT INTO `sys_menu`
VALUES (1002, '用户修改', 100, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:edit', '#', 'admin', sysdate(), '',
        null, '');
INSERT INTO `sys_menu`
VALUES (1003, '用户删除', 100, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:remove', '#', 'admin', sysdate(),
        '', null, '');
INSERT INTO `sys_menu`
VALUES (1004, '用户导出', 100, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:export', '#', 'admin', sysdate(),
        '', null, '');
INSERT INTO `sys_menu`
VALUES (1005, '用户导入', 100, 6, '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:import', '#', 'admin', sysdate(),
        '', null, '');
INSERT INTO `sys_menu`
VALUES (1006, '重置密码', 100, 7, '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:resetPwd', '#', 'admin', sysdate(),
        '', null, '');

-- ----------------------------
-- 1.6 用户和角色关联表 (sys_user_role)
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role`
(
    `user_id` bigint(20) NOT NULL COMMENT '用户ID',
    `role_id` bigint(20) NOT NULL COMMENT '角色ID',
    PRIMARY KEY (`user_id`, `role_id`)
) ENGINE = InnoDB COMMENT ='用户和角色关联表';

INSERT INTO `sys_user_role`
VALUES ('1', '1');
INSERT INTO `sys_user_role`
VALUES ('2', '2');

-- ----------------------------
-- 1.7 角色和菜单关联表 (sys_role_menu)
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu`
(
    `role_id` bigint(20) NOT NULL COMMENT '角色ID',
    `menu_id` bigint(20) NOT NULL COMMENT '菜单ID',
    PRIMARY KEY (`role_id`, `menu_id`)
) ENGINE = InnoDB COMMENT ='角色和菜单关联表';

INSERT INTO `sys_role_menu`
VALUES ('2', '1');
INSERT INTO `sys_role_menu`
VALUES ('2', '2');
INSERT INTO `sys_role_menu`
VALUES ('2', '3');
INSERT INTO `sys_role_menu`
VALUES ('2', '4');
INSERT INTO `sys_role_menu`
VALUES ('2', '100');
INSERT INTO `sys_role_menu`
VALUES ('2', '101');
INSERT INTO `sys_role_menu`
VALUES ('2', '102');
INSERT INTO `sys_role_menu`
VALUES ('2', '103');
INSERT INTO `sys_role_menu`
VALUES ('2', '104');
INSERT INTO `sys_role_menu`
VALUES ('2', '105');
INSERT INTO `sys_role_menu`
VALUES ('2', '106');
INSERT INTO `sys_role_menu`
VALUES ('2', '107');
INSERT INTO `sys_role_menu`
VALUES ('2', '108');
INSERT INTO `sys_role_menu`
VALUES ('2', '500');
INSERT INTO `sys_role_menu`
VALUES ('2', '501');
INSERT INTO `sys_role_menu`
VALUES ('2', '1000');
INSERT INTO `sys_role_menu`
VALUES ('2', '1001');
INSERT INTO `sys_role_menu`
VALUES ('2', '1002');
INSERT INTO `sys_role_menu`
VALUES ('2', '1003');
INSERT INTO `sys_role_menu`
VALUES ('2', '1004');
INSERT INTO `sys_role_menu`
VALUES ('2', '1005');
INSERT INTO `sys_role_menu`
VALUES ('2', '1006');

-- ----------------------------
-- 1.8 角色和部门关联表 (sys_role_dept)
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_dept`;
CREATE TABLE `sys_role_dept`
(
    `role_id` bigint(20) NOT NULL COMMENT '角色ID',
    `dept_id` bigint(20) NOT NULL COMMENT '部门ID',
    PRIMARY KEY (`role_id`, `dept_id`)
) ENGINE = InnoDB COMMENT ='角色和部门关联表';

INSERT INTO `sys_role_dept`
VALUES ('2', '100');
INSERT INTO `sys_role_dept`
VALUES ('2', '101');
INSERT INTO `sys_role_dept`
VALUES ('2', '105');

-- ----------------------------
-- 1.9 用户与岗位关联表 (sys_user_post)
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_post`;
CREATE TABLE `sys_user_post`
(
    `user_id` bigint(20) NOT NULL COMMENT '用户ID',
    `post_id` bigint(20) NOT NULL COMMENT '岗位ID',
    PRIMARY KEY (`user_id`, `post_id`)
) ENGINE = InnoDB COMMENT ='用户与岗位关联表';

INSERT INTO `sys_user_post`
VALUES ('1', '1');
INSERT INTO `sys_user_post`
VALUES ('2', '2');

-- ----------------------------
-- 1.10 操作日志记录 (sys_oper_log)
-- ----------------------------
DROP TABLE IF EXISTS `sys_oper_log`;
CREATE TABLE `sys_oper_log`
(
    `oper_id`        bigint(20) NOT NULL AUTO_INCREMENT COMMENT '日志主键',
    `title`          varchar(50)   DEFAULT '' COMMENT '模块标题',
    `business_type`  int(2)        DEFAULT 0 COMMENT '业务类型（0其它 1新增 2修改 3删除）',
    `method`         varchar(200)  DEFAULT '' COMMENT '方法名称',
    `request_method` varchar(10)   DEFAULT '' COMMENT '请求方式',
    `operator_type`  int(1)        DEFAULT 0 COMMENT '操作类别（0其它 1后台用户 2手机端用户）',
    `oper_name`      varchar(50)   DEFAULT '' COMMENT '操作人员',
    `dept_name`      varchar(50)   DEFAULT '' COMMENT '部门名称',
    `oper_url`       varchar(255)  DEFAULT '' COMMENT '请求URL',
    `oper_ip`        varchar(128)  DEFAULT '' COMMENT '主机地址',
    `oper_location`  varchar(255)  DEFAULT '' COMMENT '操作地点',
    `oper_param`     varchar(2000) DEFAULT '' COMMENT '请求参数',
    `json_result`    varchar(2000) DEFAULT '' COMMENT '返回参数',
    `status`         int(1)        DEFAULT 0 COMMENT '操作状态（0正常 1异常）',
    `error_msg`      varchar(2000) DEFAULT '' COMMENT '错误消息',
    `oper_time`      datetime      DEFAULT NULL COMMENT '操作时间',
    `cost_time`      bigint(20)    DEFAULT 0 COMMENT '消耗时间',
    PRIMARY KEY (`oper_id`),
    KEY `idx_sys_oper_log_bt` (`business_type`),
    KEY `idx_sys_oper_log_s` (`status`),
    KEY `idx_sys_oper_log_ot` (`oper_time`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 100 COMMENT ='操作日志记录';

-- ----------------------------
-- 1.11 字典类型表 (sys_dict_type)
-- ----------------------------
DROP TABLE IF EXISTS `sys_dict_type`;
CREATE TABLE `sys_dict_type`
(
    `dict_id`     bigint(20) NOT NULL AUTO_INCREMENT COMMENT '字典主键',
    `dict_name`   varchar(100) DEFAULT '' COMMENT '字典名称',
    `dict_type`   varchar(100) DEFAULT '' COMMENT '字典类型',
    `status`      char(1)      DEFAULT '0' COMMENT '状态（0正常 1停用）',
    `create_by`   varchar(64)  DEFAULT '' COMMENT '创建者',
    `create_time` datetime     DEFAULT NULL COMMENT '创建时间',
    `update_by`   varchar(64)  DEFAULT '' COMMENT '更新者',
    `update_time` datetime     DEFAULT NULL COMMENT '更新时间',
    `remark`      varchar(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`dict_id`),
    UNIQUE (`dict_type`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 100 COMMENT ='字典类型表';

INSERT INTO `sys_dict_type`
VALUES (1, '用户性别', 'sys_user_sex', '0', 'admin', sysdate(), '', null, '用户性别列表');
INSERT INTO `sys_dict_type`
VALUES (2, '菜单状态', 'sys_show_hide', '0', 'admin', sysdate(), '', null, '菜单状态列表');
INSERT INTO `sys_dict_type`
VALUES (3, '系统开关', 'sys_normal_disable', '0', 'admin', sysdate(), '', null, '系统开关列表');
INSERT INTO `sys_dict_type`
VALUES (4, '任务状态', 'sys_job_status', '0', 'admin', sysdate(), '', null, '任务状态列表');
INSERT INTO `sys_dict_type`
VALUES (5, '任务分组', 'sys_job_group', '0', 'admin', sysdate(), '', null, '任务分组列表');
INSERT INTO `sys_dict_type`
VALUES (6, '系统是否', 'sys_yes_no', '0', 'admin', sysdate(), '', null, '系统是否列表');
INSERT INTO `sys_dict_type`
VALUES (7, '通知类型', 'sys_notice_type', '0', 'admin', sysdate(), '', null, '通知类型列表');
INSERT INTO `sys_dict_type`
VALUES (8, '通知状态', 'sys_notice_status', '0', 'admin', sysdate(), '', null, '通知状态列表');
INSERT INTO `sys_dict_type`
VALUES (9, '操作类型', 'sys_oper_type', '0', 'admin', sysdate(), '', null, '操作类型列表');
INSERT INTO `sys_dict_type`
VALUES (10, '系统状态', 'sys_common_status', '0', 'admin', sysdate(), '', null, '登录状态列表');
INSERT INTO `sys_dict_type`
VALUES (11, '告警等级', 'alarm_level', '0', 'admin', sysdate(), '', null, '告警等级字典');
INSERT INTO `sys_dict_type`
VALUES (12, '设备状态', 'device_status', '0', 'admin', sysdate(), '', null, '设备状态字典');
INSERT INTO `sys_dict_type`
VALUES (13, '隐患点状态', 'hazard_status', '0', 'admin', sysdate(), '', null, '隐患点状态字典');
INSERT INTO `sys_dict_type`
VALUES (14, '监测类型', 'monitor_type', '0', 'admin', sysdate(), '', null, '监测类型字典');
INSERT INTO `sys_dict_type`
VALUES (15, '通知渠道', 'notify_channel', '0', 'admin', sysdate(), '', null, '通知渠道字典');

-- ----------------------------
-- 1.12 字典数据表 (sys_dict_data)
-- ----------------------------
DROP TABLE IF EXISTS `sys_dict_data`;
CREATE TABLE `sys_dict_data`
(
    `dict_code`   bigint(20) NOT NULL AUTO_INCREMENT COMMENT '字典编码',
    `dict_sort`   int(4)       DEFAULT 0 COMMENT '字典排序',
    `dict_label`  varchar(100) DEFAULT '' COMMENT '字典标签',
    `dict_value`  varchar(100) DEFAULT '' COMMENT '字典键值',
    `dict_type`   varchar(100) DEFAULT '' COMMENT '字典类型',
    `css_class`   varchar(100) DEFAULT NULL COMMENT '样式属性',
    `list_class`  varchar(100) DEFAULT NULL COMMENT '表格回显样式',
    `is_default`  char(1)      DEFAULT 'N' COMMENT '是否默认（Y是 N否）',
    `status`      char(1)      DEFAULT '0' COMMENT '状态（0正常 1停用）',
    `create_by`   varchar(64)  DEFAULT '' COMMENT '创建者',
    `create_time` datetime     DEFAULT NULL COMMENT '创建时间',
    `update_by`   varchar(64)  DEFAULT '' COMMENT '更新者',
    `update_time` datetime     DEFAULT NULL COMMENT '更新时间',
    `remark`      varchar(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`dict_code`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 100 COMMENT ='字典数据表';

INSERT INTO `sys_dict_data`
VALUES (1, 1, '男', '0', 'sys_user_sex', '', '', 'Y', '0', 'admin', sysdate(), '', null, '性别男');
INSERT INTO `sys_dict_data`
VALUES (2, 2, '女', '1', 'sys_user_sex', '', '', 'N', '0', 'admin', sysdate(), '', null, '性别女');
INSERT INTO `sys_dict_data`
VALUES (3, 3, '未知', '2', 'sys_user_sex', '', '', 'N', '0', 'admin', sysdate(), '', null, '性别未知');
INSERT INTO `sys_dict_data`
VALUES (4, 1, '显示', '0', 'sys_show_hide', '', 'primary', 'Y', '0', 'admin', sysdate(), '', null, '显示菜单');
INSERT INTO `sys_dict_data`
VALUES (5, 2, '隐藏', '1', 'sys_show_hide', '', 'danger', 'N', '0', 'admin', sysdate(), '', null, '隐藏菜单');
INSERT INTO `sys_dict_data`
VALUES (6, 1, '正常', '0', 'sys_normal_disable', '', 'primary', 'Y', '0', 'admin', sysdate(), '', null, '正常状态');
INSERT INTO `sys_dict_data`
VALUES (7, 2, '停用', '1', 'sys_normal_disable', '', 'danger', 'N', '0', 'admin', sysdate(), '', null, '停用状态');
INSERT INTO `sys_dict_data`
VALUES (8, 1, '正常', '0', 'sys_job_status', '', 'primary', 'Y', '0', 'admin', sysdate(), '', null, '正常状态');
INSERT INTO `sys_dict_data`
VALUES (9, 2, '暂停', '1', 'sys_job_status', '', 'danger', 'N', '0', 'admin', sysdate(), '', null, '停用状态');
INSERT INTO `sys_dict_data`
VALUES (10, 1, '默认', 'DEFAULT', 'sys_job_group', '', '', 'Y', '0', 'admin', sysdate(), '', null, '默认分组');
INSERT INTO `sys_dict_data`
VALUES (11, 2, '系统', 'SYSTEM', 'sys_job_group', '', '', 'N', '0', 'admin', sysdate(), '', null, '系统分组');
INSERT INTO `sys_dict_data`
VALUES (12, 1, '是', 'Y', 'sys_yes_no', '', 'primary', 'Y', '0', 'admin', sysdate(), '', null, '系统默认是');
INSERT INTO `sys_dict_data`
VALUES (13, 2, '否', 'N', 'sys_yes_no', '', 'danger', 'N', '0', 'admin', sysdate(), '', null, '系统默认否');
INSERT INTO `sys_dict_data`
VALUES (14, 1, '通知', '1', 'sys_notice_type', '', 'warning', 'Y', '0', 'admin', sysdate(), '', null, '通知');
INSERT INTO `sys_dict_data`
VALUES (15, 2, '公告', '2', 'sys_notice_type', '', 'success', 'N', '0', 'admin', sysdate(), '', null, '公告');
INSERT INTO `sys_dict_data`
VALUES (16, 1, '正常', '0', 'sys_notice_status', '', 'primary', 'Y', '0', 'admin', sysdate(), '', null, '正常状态');
INSERT INTO `sys_dict_data`
VALUES (17, 2, '关闭', '1', 'sys_notice_status', '', 'danger', 'N', '0', 'admin', sysdate(), '', null, '关闭状态');
INSERT INTO `sys_dict_data`
VALUES (18, 99, '其他', '0', 'sys_oper_type', '', 'info', 'N', '0', 'admin', sysdate(), '', null, '其他操作');
INSERT INTO `sys_dict_data`
VALUES (19, 1, '新增', '1', 'sys_oper_type', '', 'info', 'N', '0', 'admin', sysdate(), '', null, '新增操作');
INSERT INTO `sys_dict_data`
VALUES (20, 2, '修改', '2', 'sys_oper_type', '', 'info', 'N', '0', 'admin', sysdate(), '', null, '修改操作');
INSERT INTO `sys_dict_data`
VALUES (21, 3, '删除', '3', 'sys_oper_type', '', 'danger', 'N', '0', 'admin', sysdate(), '', null, '删除操作');
INSERT INTO `sys_dict_data`
VALUES (22, 4, '授权', '4', 'sys_oper_type', '', 'primary', 'N', '0', 'admin', sysdate(), '', null, '授权操作');
INSERT INTO `sys_dict_data`
VALUES (23, 5, '导出', '5', 'sys_oper_type', '', 'warning', 'N', '0', 'admin', sysdate(), '', null, '导出操作');
INSERT INTO `sys_dict_data`
VALUES (24, 6, '导入', '6', 'sys_oper_type', '', 'warning', 'N', '0', 'admin', sysdate(), '', null, '导入操作');
INSERT INTO `sys_dict_data`
VALUES (25, 7, '强退', '7', 'sys_oper_type', '', 'danger', 'N', '0', 'admin', sysdate(), '', null, '强退操作');
INSERT INTO `sys_dict_data`
VALUES (26, 8, '生成代码', '8', 'sys_oper_type', '', 'warning', 'N', '0', 'admin', sysdate(), '', null, '生成操作');
INSERT INTO `sys_dict_data`
VALUES (27, 9, '清空数据', '9', 'sys_oper_type', '', 'danger', 'N', '0', 'admin', sysdate(), '', null, '清空操作');
INSERT INTO `sys_dict_data`
VALUES (28, 1, '成功', '0', 'sys_common_status', '', 'primary', 'N', '0', 'admin', sysdate(), '', null, '正常状态');
INSERT INTO `sys_dict_data`
VALUES (29, 2, '失败', '1', 'sys_common_status', '', 'danger', 'N', '0', 'admin', sysdate(), '', null, '停用状态');
-- 新设计业务字典数据
INSERT INTO `sys_dict_data`
VALUES (30, 1, '一级(蓝色)', '1', 'alarm_level', '', 'primary', 'Y', '0', 'admin', sysdate(), '', null, '一级告警');
INSERT INTO `sys_dict_data`
VALUES (31, 2, '二级(黄色)', '2', 'alarm_level', '', 'warning', 'N', '0', 'admin', sysdate(), '', null, '二级告警');
INSERT INTO `sys_dict_data`
VALUES (32, 3, '三级(橙色)', '3', 'alarm_level', '', 'danger', 'N', '0', 'admin', sysdate(), '', null, '三级告警');
INSERT INTO `sys_dict_data`
VALUES (33, 4, '四级(红色)', '4', 'alarm_level', '', 'danger', 'N', '0', 'admin', sysdate(), '', null, '四级告警');
INSERT INTO `sys_dict_data`
VALUES (34, 1, '正常', '1', 'device_status', '', 'success', 'Y', '0', 'admin', sysdate(), '', null, '设备正常');
INSERT INTO `sys_dict_data`
VALUES (35, 2, '故障', '2', 'device_status', '', 'danger', 'N', '0', 'admin', sysdate(), '', null, '设备故障');
INSERT INTO `sys_dict_data`
VALUES (36, 3, '离线', '3', 'device_status', '', 'warning', 'N', '0', 'admin', sysdate(), '', null, '设备离线');
INSERT INTO `sys_dict_data`
VALUES (37, 1, '监测中', '1', 'hazard_status', '', 'success', 'Y', '0', 'admin', sysdate(), '', null, '监测中');
INSERT INTO `sys_dict_data`
VALUES (38, 2, '停测中', '2', 'hazard_status', '', 'warning', 'N', '0', 'admin', sysdate(), '', null, '停测中');
INSERT INTO `sys_dict_data`
VALUES (39, 3, '已完结', '3', 'hazard_status', '', 'info', 'N', '0', 'admin', sysdate(), '', null, '已完结');
INSERT INTO `sys_dict_data`
VALUES (40, 1, '系统消息', 'SYSTEM', 'notify_channel', '', 'primary', 'Y', '0', 'admin', sysdate(), '', null,
        '系统消息');
INSERT INTO `sys_dict_data`
VALUES (41, 2, '短信通知', 'SMS', 'notify_channel', '', 'success', 'N', '0', 'admin', sysdate(), '', null, '短信通知');
INSERT INTO `sys_dict_data`
VALUES (42, 3, '微信通知', 'WECHAT', 'notify_channel', '', 'success', 'N', '0', 'admin', sysdate(), '', null,
        '微信通知');
INSERT INTO `sys_dict_data`
VALUES (43, 4, '电子邮件', 'EMAIL', 'notify_channel', '', 'info', 'N', '0', 'admin', sysdate(), '', null, '电子邮件');

-- ----------------------------
-- 1.13 参数配置表 (sys_config)
-- ----------------------------
DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config`
(
    `config_id`    int(5) NOT NULL AUTO_INCREMENT COMMENT '参数主键',
    `config_name`  varchar(100) DEFAULT '' COMMENT '参数名称',
    `config_key`   varchar(100) DEFAULT '' COMMENT '参数键名',
    `config_value` varchar(500) DEFAULT '' COMMENT '参数键值',
    `config_type`  char(1)      DEFAULT 'N' COMMENT '系统内置（Y是 N否）',
    `create_by`    varchar(64)  DEFAULT '' COMMENT '创建者',
    `create_time`  datetime     DEFAULT NULL COMMENT '创建时间',
    `update_by`    varchar(64)  DEFAULT '' COMMENT '更新者',
    `update_time`  datetime     DEFAULT NULL COMMENT '更新时间',
    `remark`       varchar(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`config_id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 100 COMMENT ='参数配置表';

INSERT INTO `sys_config`
VALUES (1, '主框架页-默认皮肤样式名称', 'sys.index.skinName', 'skin-blue', 'Y', 'admin', sysdate(), '', null,
        '蓝色 skin-blue、绿色 skin-green、紫色 skin-purple、红色 skin-red、黄色 skin-yellow');
INSERT INTO `sys_config`
VALUES (2, '用户管理-账号初始密码', 'sys.user.initPassword', '123456', 'Y', 'admin', sysdate(), '', null,
        '初始化密码 123456');
INSERT INTO `sys_config`
VALUES (3, '主框架页-侧边栏主题', 'sys.index.sideTheme', 'theme-dark', 'Y', 'admin', sysdate(), '', null,
        '深色主题theme-dark，浅色主题theme-light');
INSERT INTO `sys_config`
VALUES (4, '账号自助-验证码开关', 'sys.account.captchaEnabled', 'true', 'Y', 'admin', sysdate(), '', null,
        '是否开启验证码功能（true开启，false关闭）');
INSERT INTO `sys_config`
VALUES (5, '账号自助-是否开启用户注册功能', 'sys.account.registerUser', 'false', 'Y', 'admin', sysdate(), '', null,
        '是否开启注册用户功能（true开启，false关闭）');
INSERT INTO `sys_config`
VALUES (6, '用户登录-黑名单列表', 'sys.login.blackIPList', '', 'Y', 'admin', sysdate(), '', null,
        '设置登录IP黑名单限制，多个匹配项以;分隔，支持匹配（*通配、网段）');
INSERT INTO `sys_config`
VALUES (7, '用户管理-初始密码修改策略', 'sys.account.initPasswordModify', '1', 'Y', 'admin', sysdate(), '', null,
        '0：初始密码修改策略关闭，没有任何提示，1：提醒用户，如果未修改初始密码，则在登录时就会提醒修改密码对话框');
INSERT INTO `sys_config`
VALUES (8, '用户管理-账号密码更新周期', 'sys.account.passwordValidateDays', '0', 'Y', 'admin', sysdate(), '', null,
        '密码更新周期（填写数字，数据初始化值为0不限制，若修改必须为大于0小于365的正整数），如果超过这个周期登录系统时，则在登录时就会提醒修改密码对话框');
INSERT INTO `sys_config`
VALUES (9, '用户管理-密码字符范围', 'sys.account.chrtype', '0', 'Y', 'admin', sysdate(), '', null,
        '默认任意字符范围，0任意（密码可以输入任意字符），1数字（密码只能为0-9数字），2英文字母（密码只能为a-z和A-Z字母），3字母和数字（密码必须包含字母，数字）,4字母数字和特殊字符（目前支持的特殊字符包括：~!@#$%^&*()-=_+）');

-- ----------------------------
-- 1.14 系统访问记录 (sys_logininfor)
-- ----------------------------
DROP TABLE IF EXISTS `sys_logininfor`;
CREATE TABLE `sys_logininfor`
(
    `info_id`        bigint(20) NOT NULL AUTO_INCREMENT COMMENT '访问ID',
    `user_name`      varchar(50)  DEFAULT '' COMMENT '用户账号',
    `ipaddr`         varchar(128) DEFAULT '' COMMENT '登录IP地址',
    `login_location` varchar(255) DEFAULT '' COMMENT '登录地点',
    `browser`        varchar(50)  DEFAULT '' COMMENT '浏览器类型',
    `os`             varchar(50)  DEFAULT '' COMMENT '操作系统',
    `status`         char(1)      DEFAULT '0' COMMENT '登录状态（0成功 1失败）',
    `msg`            varchar(255) DEFAULT '' COMMENT '提示消息',
    `login_time`     datetime     DEFAULT NULL COMMENT '访问时间',
    PRIMARY KEY (`info_id`),
    KEY `idx_sys_logininfor_s` (`status`),
    KEY `idx_sys_logininfor_lt` (`login_time`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 100 COMMENT ='系统访问记录';

-- ----------------------------
-- 1.15 定时任务调度表 (sys_job)
-- ----------------------------
DROP TABLE IF EXISTS `sys_job`;
CREATE TABLE `sys_job`
(
    `job_id`          bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '任务ID',
    `job_name`        varchar(64)  DEFAULT '' COMMENT '任务名称',
    `job_group`       varchar(64)  DEFAULT 'DEFAULT' COMMENT '任务组名',
    `invoke_target`   varchar(500) NOT NULL COMMENT '调用目标字符串',
    `cron_expression` varchar(255) DEFAULT '' COMMENT 'cron执行表达式',
    `misfire_policy`  varchar(20)  DEFAULT '3' COMMENT '计划执行错误策略（1立即执行 2执行一次 3放弃执行）',
    `concurrent`      char(1)      DEFAULT '1' COMMENT '是否并发执行（0允许 1禁止）',
    `status`          char(1)      DEFAULT '0' COMMENT '状态（0正常 1暂停）',
    `create_by`       varchar(64)  DEFAULT '' COMMENT '创建者',
    `create_time`     datetime     DEFAULT NULL COMMENT '创建时间',
    `update_by`       varchar(64)  DEFAULT '' COMMENT '更新者',
    `update_time`     datetime     DEFAULT NULL COMMENT '更新时间',
    `remark`          varchar(500) DEFAULT '' COMMENT '备注信息',
    PRIMARY KEY (`job_id`, `job_name`, `job_group`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 100 COMMENT ='定时任务调度表';

INSERT INTO `sys_job`
VALUES (1, '系统默认（无参）', 'DEFAULT', 'ryTask.ryNoParams', '0/10 * * * * ?', '3', '1', '1', 'admin', sysdate(), '',
        null, '');
INSERT INTO `sys_job`
VALUES (2, '系统默认（有参）', 'DEFAULT', 'ryTask.ryParams(\'ry\')', '0/15 * * * * ?', '3', '1', '1', 'admin', sysdate(),
        '', null, '');
INSERT INTO `sys_job`
VALUES (3, '系统默认（多参）', 'DEFAULT', 'ryTask.ryMultipleParams(\'ry\', true, 2000L, 316.50D, 100)', '0/20 * * * * ?',
        '3', '1', '1', 'admin', sysdate(), '', null, '');

-- ----------------------------
-- 1.16 定时任务调度日志表 (sys_job_log)
-- ----------------------------
DROP TABLE IF EXISTS `sys_job_log`;
CREATE TABLE `sys_job_log`
(
    `job_log_id`     bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '任务日志ID',
    `job_name`       varchar(64)  NOT NULL COMMENT '任务名称',
    `job_group`      varchar(64)  NOT NULL COMMENT '任务组名',
    `invoke_target`  varchar(500) NOT NULL COMMENT '调用目标字符串',
    `job_message`    varchar(500) COMMENT '日志信息',
    `status`         char(1)       DEFAULT '0' COMMENT '执行状态（0正常 1失败）',
    `exception_info` varchar(2000) DEFAULT '' COMMENT '异常信息',
    `start_time`     datetime COMMENT '执行开始时间',
    `end_time`       datetime COMMENT '执行结束时间',
    `create_time`    datetime COMMENT '创建时间',
    PRIMARY KEY (`job_log_id`)
) ENGINE = InnoDB COMMENT ='定时任务调度日志表';

-- ----------------------------
-- 1.17 通知公告表 (sys_notice)
-- ----------------------------
DROP TABLE IF EXISTS `sys_notice`;
CREATE TABLE `sys_notice`
(
    `notice_id`      int(4)      NOT NULL AUTO_INCREMENT COMMENT '公告ID',
    `notice_title`   varchar(50) NOT NULL COMMENT '公告标题',
    `notice_type`    char(1)     NOT NULL COMMENT '公告类型（1通知 2公告）',
    `notice_content` longblob     DEFAULT null COMMENT '公告内容',
    `status`         char(1)      DEFAULT '0' COMMENT '公告状态（0正常 1关闭）',
    `create_by`      varchar(64)  DEFAULT '' COMMENT '创建者',
    `create_time`    datetime     DEFAULT NULL COMMENT '创建时间',
    `update_by`      varchar(64)  DEFAULT '' COMMENT '更新者',
    `update_time`    datetime     DEFAULT NULL COMMENT '更新时间',
    `remark`         varchar(255) DEFAULT null COMMENT '备注',
    PRIMARY KEY (`notice_id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 10 COMMENT ='通知公告表';

INSERT INTO `sys_notice`
VALUES ('1', '温馨提醒：2018-07-01 若依新版本发布啦', '2', '新版本内容', '0', 'admin', sysdate(), '', null, '管理员');
INSERT INTO `sys_notice`
VALUES ('2', '维护通知：2018-07-01 若依系统凌晨维护', '1', '维护内容', '0', 'admin', sysdate(), '', null, '管理员');

-- ----------------------------
-- 1.18 公告已读记录表 (sys_notice_read)
-- ----------------------------
DROP TABLE IF EXISTS `sys_notice_read`;
CREATE TABLE `sys_notice_read`
(
    `read_id`   bigint(20) NOT NULL AUTO_INCREMENT COMMENT '已读主键',
    `notice_id` int(4)     NOT NULL COMMENT '公告id',
    `user_id`   bigint(20) NOT NULL COMMENT '用户id',
    `read_time` datetime   NOT NULL COMMENT '阅读时间',
    PRIMARY KEY (`read_id`),
    UNIQUE KEY `uk_user_notice` (`user_id`, `notice_id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1 COMMENT ='公告已读记录表';

-- ===============================================================
-- 二、基础管理模块 (业务表 - 基于新设计)
-- ===============================================================

-- ----------------------------
-- 2.1 组织架构表 (sys_organization) - 新设计
-- ----------------------------
DROP TABLE IF EXISTS `sys_organization`;
CREATE TABLE `sys_organization`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `code`        VARCHAR(100) NOT NULL COMMENT '组织编码',
    `name`        VARCHAR(200) NOT NULL COMMENT '组织名称',
    `parent_id`   BIGINT       DEFAULT 0 COMMENT '父组织ID，0为根节点',
    `parent_ids`  VARCHAR(500) DEFAULT NULL COMMENT '父组织ID路径，如/0/1/2/',
    `level`       TINYINT      DEFAULT 1 COMMENT '层级: 1-5级',
    `leader`      VARCHAR(100) DEFAULT NULL COMMENT '负责人',
    `phone`       VARCHAR(20)  DEFAULT NULL COMMENT '联系电话',
    `email`       VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `region`      VARCHAR(200) DEFAULT NULL COMMENT '区域',
    `center`      VARCHAR(200) DEFAULT NULL COMMENT '中心点坐标',
    `address`     VARCHAR(500) DEFAULT NULL COMMENT '详细地址',
    `sort_order`  INT          DEFAULT 0 COMMENT '排序号',
    `status`      TINYINT      DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    `create_by`   VARCHAR(64)  DEFAULT NULL COMMENT '创建者',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`   VARCHAR(64)  DEFAULT NULL COMMENT '更新者',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `del_flag`    TINYINT      DEFAULT 0 COMMENT '删除标记: 0-正常, 1-删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_org_code` (`code`),
    KEY `idx_sys_org_parent_id` (`parent_id`),
    KEY `idx_sys_org_level` (`level`),
    KEY `idx_sys_org_status` (`status`),
    KEY `idx_sys_org_del_flag` (`del_flag`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='组织架构表';

INSERT INTO `sys_organization` (`id`, `code`, `name`, `parent_id`, `parent_ids`, `level`, `status`)
VALUES (1, 'ROOT', '系统管理员', 0, '/0/', 1, 1),
       (2, 'DEPT001', '监测中心', 1, '/0/1/', 2, 1),
       (3, 'DEPT002', '运维部', 1, '/0/1/', 2, 1);

-- ----------------------------
-- 2.2 隐患点分组表 (hazard_point_group)
-- ----------------------------
DROP TABLE IF EXISTS `hazard_point_group`;
CREATE TABLE `hazard_point_group`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `code`        VARCHAR(100) NOT NULL COMMENT '分组编码',
    `name`        VARCHAR(200) NOT NULL COMMENT '分组名称',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '分组描述',
    `sort_order`  INT          DEFAULT 0 COMMENT '排序号',
    `status`      TINYINT      DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    `create_by`   VARCHAR(64)  DEFAULT NULL COMMENT '创建者',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`   VARCHAR(64)  DEFAULT NULL COMMENT '更新者',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `del_flag`    TINYINT      DEFAULT 0 COMMENT '删除标记: 0-正常, 1-删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_hazard_group_code` (`code`),
    KEY `idx_hazard_group_status` (`status`),
    KEY `idx_hazard_group_del_flag` (`del_flag`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='隐患点分组表';

-- ----------------------------
-- 2.3 隐患点表 (hazard_point)
-- ----------------------------
DROP TABLE IF EXISTS `hazard_point`;
CREATE TABLE `hazard_point`
(
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `code`         VARCHAR(100) NOT NULL COMMENT '隐患点编号',
    `name`         VARCHAR(200) NOT NULL COMMENT '隐患点名称',
    `group_id`     BIGINT         DEFAULT NULL COMMENT '分组ID',
    `group_name`   VARCHAR(200)   DEFAULT NULL COMMENT '分组名称',
    `longitude`    DECIMAL(10, 6) DEFAULT NULL COMMENT '中心经度',
    `latitude`     DECIMAL(10, 6) DEFAULT NULL COMMENT '中心纬度',
    `strike`       DECIMAL(10, 2) DEFAULT NULL COMMENT '走向角度',
    `description`  TEXT           DEFAULT NULL COMMENT '隐患描述',
    `status`       TINYINT        DEFAULT 1 COMMENT '状态: 1-监测中, 2-停测中, 3-已完结',
    `device_count` INT            DEFAULT 0 COMMENT '绑定设备数量',
    `create_by`    VARCHAR(64)    DEFAULT NULL COMMENT '创建者',
    `create_time`  DATETIME       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`    VARCHAR(64)    DEFAULT NULL COMMENT '更新者',
    `update_time`  DATETIME       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `del_flag`     TINYINT        DEFAULT 0 COMMENT '删除标记: 0-正常, 1-删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_hazard_point_code` (`code`),
    KEY `idx_hazard_point_group_id` (`group_id`),
    KEY `idx_hazard_point_status` (`status`),
    KEY `idx_hazard_point_del_flag` (`del_flag`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='隐患点表';

-- ----------------------------
-- 2.4 监测类型表 (monitor_type)
-- ----------------------------
DROP TABLE IF EXISTS `monitor_type`;
CREATE TABLE `monitor_type`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `code`        VARCHAR(100) NOT NULL COMMENT '监测类型编码',
    `name`        VARCHAR(200) NOT NULL COMMENT '监测类型名称',
    `device_type` TINYINT      DEFAULT 1 COMMENT '设备类型: 1-直连设备, 2-传感器, 3-RTU',
    `icon`        VARCHAR(200) DEFAULT NULL COMMENT '图标路径',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '描述',
    `sort_order`  INT          DEFAULT 0 COMMENT '排序号',
    `status`      TINYINT      DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    `create_by`   VARCHAR(64)  DEFAULT NULL COMMENT '创建者',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`   VARCHAR(64)  DEFAULT NULL COMMENT '更新者',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `del_flag`    TINYINT      DEFAULT 0 COMMENT '删除标记: 0-正常, 1-删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_monitor_type_code` (`code`),
    KEY `idx_monitor_type_device_type` (`device_type`),
    KEY `idx_monitor_type_status` (`status`),
    KEY `idx_monitor_type_del_flag` (`del_flag`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='监测类型表';

INSERT INTO `monitor_type` (`code`, `name`, `device_type`, `status`)
VALUES ('JCLX001', '雨量监测', 2, 1),
       ('JCLX002', '位移监测', 2, 1),
       ('JCLX003', '温湿度监测', 2, 1),
       ('JCLX004', '水位监测', 2, 1),
       ('JCLX005', '裂缝监测', 2, 1),
       ('JCLX006', '倾斜监测', 2, 1),
       ('JCLX007', '地温监测', 2, 1),
       ('JCLX008', '含水率监测', 2, 1);

-- ----------------------------
-- 2.5 监测内容表 (monitor_content)
-- ----------------------------
DROP TABLE IF EXISTS `monitor_content`;
CREATE TABLE `monitor_content`
(
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `monitor_type_id` BIGINT       NOT NULL COMMENT '监测类型ID',
    `code`            VARCHAR(100) NOT NULL COMMENT '监测内容编码',
    `name`            VARCHAR(200) NOT NULL COMMENT '监测内容名称',
    `unit`            VARCHAR(50)  DEFAULT NULL COMMENT '单位',
    `indicator_type`  VARCHAR(50)  DEFAULT NULL COMMENT '指标类型',
    `icon`            VARCHAR(200) DEFAULT NULL COMMENT '图标路径',
    `sort_order`      INT          DEFAULT 0 COMMENT '排序号',
    `create_by`       VARCHAR(64)  DEFAULT NULL COMMENT '创建者',
    `create_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`       VARCHAR(64)  DEFAULT NULL COMMENT '更新者',
    `update_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `del_flag`        TINYINT      DEFAULT 0 COMMENT '删除标记: 0-正常, 1-删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_monitor_content_code` (`code`),
    KEY `idx_monitor_content_type_id` (`monitor_type_id`),
    KEY `idx_monitor_content_del_flag` (`del_flag`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='监测内容表';

INSERT INTO `monitor_content` (`monitor_type_id`, `code`, `name`, `unit`, `indicator_type`)
VALUES (1, 'rainfall_hour', '小时雨量', 'mm', 'yl'),
       (1, 'rainfall_day', '日雨量', 'mm', 'yl'),
       (2, 'displacement_x', 'X轴位移', 'mm', 'wy'),
       (2, 'displacement_y', 'Y轴位移', 'mm', 'wy'),
       (2, 'displacement_z', 'Z轴位移', 'mm', 'wy'),
       (3, 'temperature', '温度', '℃', 'wd'),
       (3, 'humidity', '含水率', '%', 'hsl'),
       (4, 'water_level', '水位', 'm', 'sw'),
       (5, 'crack_width', '裂缝宽度', 'mm', 'lf'),
       (6, 'inclination_x', 'X方向倾角', '°', 'qx'),
       (6, 'inclination_y', 'Y方向倾角', '°', 'qx'),
       (7, 'soil_temp_10cm', '10cm地温', '℃', 'dw'),
       (8, 'soil_moisture', '土壤含水率', '%', 'hsl');

-- ----------------------------
-- 2.6 设备表 (device)
-- ----------------------------
DROP TABLE IF EXISTS `device`;
CREATE TABLE `device`
(
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `code`             VARCHAR(100) NOT NULL COMMENT '设备编号',
    `name`             VARCHAR(200) NOT NULL COMMENT '设备名称',
    `icon`             VARCHAR(200) DEFAULT NULL COMMENT '设备图标',
    `icon_path`        VARCHAR(500) DEFAULT NULL COMMENT '图标路径',
    `status`           TINYINT      DEFAULT 1 COMMENT '状态: 1-正常, 2-故障, 3-离线',
    `run_status`       TINYINT      DEFAULT 0 COMMENT '运行状态: 0-未知, 1-运行中, 2-停止',
    `last_report_time` DATETIME     DEFAULT NULL COMMENT '最近上报时间',
    `create_by`        VARCHAR(64)  DEFAULT NULL COMMENT '创建者',
    `create_time`      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`        VARCHAR(64)  DEFAULT NULL COMMENT '更新者',
    `update_time`      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `del_flag`         TINYINT      DEFAULT 0 COMMENT '删除标记: 0-正常, 1-删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_device_code` (`code`),
    KEY `idx_device_status` (`status`),
    KEY `idx_device_run_status` (`run_status`),
    KEY `idx_device_del_flag` (`del_flag`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='设备表';

-- ----------------------------
-- 2.7 传感器表 (device_sensor)
-- ----------------------------
DROP TABLE IF EXISTS `device_sensor`;
CREATE TABLE `device_sensor`
(
    `id`                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `device_id`         BIGINT       NOT NULL COMMENT '设备ID',
    `device_code`       VARCHAR(100) DEFAULT NULL COMMENT '设备编号',
    `sensor_code`       VARCHAR(100) NOT NULL COMMENT '传感器编号',
    `sensor_name`       VARCHAR(200) NOT NULL COMMENT '传感器名称',
    `monitor_type_id`   BIGINT       NOT NULL COMMENT '监测类型ID',
    `monitor_type_code` VARCHAR(100) DEFAULT NULL COMMENT '监测类型编码',
    `monitor_type_name` VARCHAR(200) DEFAULT NULL COMMENT '监测类型名称',
    `status`            TINYINT      DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    `create_by`         VARCHAR(64)  DEFAULT NULL COMMENT '创建者',
    `create_time`       DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`         VARCHAR(64)  DEFAULT NULL COMMENT '更新者',
    `update_time`       DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `del_flag`          TINYINT      DEFAULT 0 COMMENT '删除标记: 0-正常, 1-删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_device_sensor_code` (`sensor_code`),
    KEY `idx_device_sensor_device_id` (`device_id`),
    KEY `idx_device_sensor_type_id` (`monitor_type_id`),
    KEY `idx_device_sensor_status` (`status`),
    KEY `idx_device_sensor_del_flag` (`del_flag`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='传感器表';

-- ----------------------------
-- 2.8 传感器属性表 (sensor_attribute)
-- ----------------------------
DROP TABLE IF EXISTS `sensor_attribute`;
CREATE TABLE `sensor_attribute`
(
    `id`                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `sensor_id`           BIGINT       NOT NULL COMMENT '传感器ID',
    `attr_code`           VARCHAR(100) NOT NULL COMMENT '属性编码',
    `attr_name`           VARCHAR(200) NOT NULL COMMENT '属性名称',
    `indicator_type`      VARCHAR(50)    DEFAULT NULL COMMENT '指标类型',
    `indicator_type_name` VARCHAR(100)   DEFAULT NULL COMMENT '指标类型名称',
    `initial_value`       DECIMAL(12, 2) DEFAULT NULL COMMENT '初始值',
    `unit`                VARCHAR(50)    DEFAULT NULL COMMENT '单位',
    `range_min`           DECIMAL(12, 2) DEFAULT NULL COMMENT '最小值范围',
    `range_max`           DECIMAL(12, 2) DEFAULT NULL COMMENT '最大值范围',
    `create_by`           VARCHAR(64)    DEFAULT NULL COMMENT '创建者',
    `create_time`         DATETIME       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`           VARCHAR(64)    DEFAULT NULL COMMENT '更新者',
    `update_time`         DATETIME       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_sensor_attr_sensor_id` (`sensor_id`),
    KEY `idx_sensor_attr_attr_code` (`attr_code`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='传感器属性表';

-- ----------------------------
-- 2.9 视频设备表 (video_device)
-- ----------------------------
DROP TABLE IF EXISTS `video_device`;
CREATE TABLE `video_device`
(
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `code`             VARCHAR(100) NOT NULL COMMENT '设备编号',
    `name`             VARCHAR(200) NOT NULL COMMENT '设备名称',
    `icon`             VARCHAR(200) DEFAULT NULL COMMENT '图标代码',
    `icon_path`        VARCHAR(500) DEFAULT NULL COMMENT '图标路径',
    `protocol_code`    VARCHAR(50)  DEFAULT NULL COMMENT '协议类型编码',
    `protocol_name`    VARCHAR(100) DEFAULT NULL COMMENT '协议类型名称',
    `stream_url`       VARCHAR(500) DEFAULT NULL COMMENT '视频流地址',
    `status`           TINYINT      DEFAULT 1 COMMENT '状态: 0-离线, 1-在线, 2-故障',
    `last_online_time` DATETIME     DEFAULT NULL COMMENT '最近在线时间',
    `install_time`     DATETIME     DEFAULT NULL COMMENT '安装时间',
    `create_by`        VARCHAR(64)  DEFAULT NULL COMMENT '创建者',
    `create_time`      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`        VARCHAR(64)  DEFAULT NULL COMMENT '更新者',
    `update_time`      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `del_flag`         TINYINT      DEFAULT 0 COMMENT '删除标记: 0-正常, 1-删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_video_device_code` (`code`),
    KEY `idx_video_device_status` (`status`),
    KEY `idx_video_device_del_flag` (`del_flag`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='视频设备表';

-- ===============================================================
-- 三、关联关系模块
-- ===============================================================

-- ----------------------------
-- 3.1 设备隐患点关联表 (device_hazard_point)
-- ----------------------------
DROP TABLE IF EXISTS `device_hazard_point`;
CREATE TABLE `device_hazard_point`
(
    `id`                BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `device_id`         BIGINT NOT NULL COMMENT '设备ID',
    `hazard_point_id`   BIGINT NOT NULL COMMENT '隐患点ID',
    `install_longitude` DECIMAL(10, 6) DEFAULT NULL COMMENT '安装经度',
    `install_latitude`  DECIMAL(10, 6) DEFAULT NULL COMMENT '安装纬度',
    `bind_time`         DATETIME       DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
    `create_by`         VARCHAR(64)    DEFAULT NULL COMMENT '创建者',
    `create_time`       DATETIME       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_device_hazard_point` (`device_id`, `hazard_point_id`),
    KEY `idx_device_hazard_point_device_id` (`device_id`),
    KEY `idx_device_hazard_point_hp_id` (`hazard_point_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='设备隐患点关联表';

-- ----------------------------
-- 3.2 视频设备隐患点关联表 (video_device_hazard_point)
-- ----------------------------
DROP TABLE IF EXISTS `video_device_hazard_point`;
CREATE TABLE `video_device_hazard_point`
(
    `id`                BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `video_device_id`   BIGINT NOT NULL COMMENT '视频设备ID',
    `hazard_point_id`   BIGINT NOT NULL COMMENT '隐患点ID',
    `install_longitude` DECIMAL(10, 6) DEFAULT NULL COMMENT '安装经度',
    `install_latitude`  DECIMAL(10, 6) DEFAULT NULL COMMENT '安装纬度',
    `bind_time`         DATETIME       DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
    `create_by`         VARCHAR(64)    DEFAULT NULL COMMENT '创建者',
    `create_time`       DATETIME       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_video_device_hazard_point` (`video_device_id`, `hazard_point_id`),
    KEY `idx_video_device_hp_device_id` (`video_device_id`),
    KEY `idx_video_device_hp_hp_id` (`hazard_point_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='视频设备隐患点关联表';

-- ===============================================================
-- 四、告警中心模块
-- ===============================================================

-- ----------------------------
-- 4.1 告警判据表 (alarm_criteria)
-- ----------------------------
DROP TABLE IF EXISTS `alarm_criteria`;
CREATE TABLE `alarm_criteria`
(
    `id`                   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `hazard_point_id`      BIGINT       NOT NULL COMMENT '隐患点ID',
    `name`                 VARCHAR(200) NOT NULL COMMENT '判据名称',
    `device_id`            BIGINT       DEFAULT NULL COMMENT '设备ID',
    `device_name`          VARCHAR(200) DEFAULT NULL COMMENT '设备名称',
    `monitor_type_id`      BIGINT       DEFAULT NULL COMMENT '监测类型ID',
    `monitor_type_name`    VARCHAR(200) DEFAULT NULL COMMENT '监测类型名称',
    `monitor_content_code` VARCHAR(100) DEFAULT NULL COMMENT '监测内容编码',
    `monitor_content_name` VARCHAR(200) DEFAULT NULL COMMENT '监测内容名称',
    `blue_expression`      VARCHAR(500) DEFAULT NULL COMMENT '蓝色预警表达式',
    `blue_description`     VARCHAR(500) DEFAULT NULL COMMENT '蓝色预警描述',
    `yellow_expression`    VARCHAR(500) DEFAULT NULL COMMENT '黄色预警表达式',
    `yellow_description`   VARCHAR(500) DEFAULT NULL COMMENT '黄色预警描述',
    `orange_expression`    VARCHAR(500) DEFAULT NULL COMMENT '橙色预警表达式',
    `orange_description`   VARCHAR(500) DEFAULT NULL COMMENT '橙色预警描述',
    `red_expression`       VARCHAR(500) DEFAULT NULL COMMENT '红色预警表达式',
    `red_description`      VARCHAR(500) DEFAULT NULL COMMENT '红色预警描述',
    `is_enabled`           TINYINT      DEFAULT 1 COMMENT '是否启用: 0-禁用, 1-启用',
    `create_by`            VARCHAR(64)  DEFAULT NULL COMMENT '创建者',
    `create_time`          DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`            VARCHAR(64)  DEFAULT NULL COMMENT '更新者',
    `update_time`          DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `del_flag`             TINYINT      DEFAULT 0 COMMENT '删除标记: 0-正常, 1-删除',
    PRIMARY KEY (`id`),
    KEY `idx_alarm_criteria_hp_id` (`hazard_point_id`),
    KEY `idx_alarm_criteria_device_id` (`device_id`),
    KEY `idx_alarm_criteria_enabled` (`is_enabled`),
    KEY `idx_alarm_criteria_del_flag` (`del_flag`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='告警判据表';

-- ----------------------------
-- 4.2 告警分发规则表 (alarm_dispatch_rule)
-- ----------------------------
DROP TABLE IF EXISTS `alarm_dispatch_rule`;
CREATE TABLE `alarm_dispatch_rule`
(
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `hazard_point_id` BIGINT       NOT NULL COMMENT '隐患点ID',
    `name`            VARCHAR(200) NOT NULL COMMENT '规则名称',
    `type`            TINYINT      DEFAULT 1 COMMENT '类型: 1-告警分发, 2-状态通知',
    `alarm_level`     VARCHAR(200) DEFAULT NULL COMMENT '告警等级列表',
    `recipient_ids`   VARCHAR(500) DEFAULT NULL COMMENT '接收人ID或设备ID列表',
    `channel`         VARCHAR(200) DEFAULT NULL COMMENT '通知渠道: SYSTEM,SMS,EMAIL',
    `is_enabled`      TINYINT      DEFAULT 1 COMMENT '是否启用: 0-禁用, 1-启用',
    `time_setting`    VARCHAR(50)  DEFAULT NULL COMMENT '时间频率设置',
    `time_value`      VARCHAR(100) DEFAULT NULL COMMENT '时间值列表：逗号分隔',
    `create_by`       VARCHAR(64)  DEFAULT NULL COMMENT '创建者',
    `create_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`       VARCHAR(64)  DEFAULT NULL COMMENT '更新者',
    `update_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `del_flag`        TINYINT      DEFAULT 0 COMMENT '删除标记: 0-正常, 1-删除',
    PRIMARY KEY (`id`),
    KEY `idx_alarm_dispatch_hp_id` (`hazard_point_id`),
    KEY `idx_alarm_dispatch_type` (`type`),
    KEY `idx_alarm_dispatch_enabled` (`is_enabled`),
    KEY `idx_alarm_dispatch_del_flag` (`del_flag`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='告警分发规则表';

-- ----------------------------
-- 4.3 告警记录表 (alarm_record)
-- ----------------------------
DROP TABLE IF EXISTS `alarm_record`;
CREATE TABLE `alarm_record`
(
    `id`                BIGINT  NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `hazard_point_id`   BIGINT  NOT NULL COMMENT '隐患点ID',
    `hazard_point_code` VARCHAR(100)   DEFAULT NULL COMMENT '隐患点编号',
    `hazard_point_name` VARCHAR(200)   DEFAULT NULL COMMENT '隐患点名称',
    `alarm_level`       TINYINT NOT NULL COMMENT '告警等级: 1-蓝色, 2-黄色, 3-橙色, 4-红色',
    `alarm_level_text`  VARCHAR(50)    DEFAULT NULL COMMENT '告警等级文本',
    `alarm_type`        VARCHAR(100)   DEFAULT NULL COMMENT '告警类型: 1-阈值告警, 2-模型告警, 3-综合告警, 4-其他告警',
    `alarm_message`     TEXT           DEFAULT NULL COMMENT '告警消息',
    `device_id`         BIGINT         DEFAULT NULL COMMENT '设备ID',
    `sensor_id`         BIGINT         DEFAULT NULL COMMENT '传感器ID',
    `monitor_type_id`   BIGINT         DEFAULT NULL COMMENT '监测类型ID',
    `current_value`     DECIMAL(12, 2) DEFAULT NULL COMMENT '当前值',
    `alarm_criteria_id` DECIMAL(12, 2) DEFAULT NULL COMMENT '阈值',
    `status`            TINYINT        DEFAULT 1 COMMENT '状态: 1-待处理, 2-处理中, 3-已处理, 4-已忽略',
    `create_time`       DATETIME       DEFAULT CURRENT_TIMESTAMP COMMENT '告警时间',
    `handle_time`       DATETIME       DEFAULT NULL COMMENT '处理时间',
    `handle_person`     VARCHAR(100)   DEFAULT NULL COMMENT '处理人',
    `handle_result`     TEXT           DEFAULT NULL COMMENT '处理结果',
    PRIMARY KEY (`id`),
    KEY `idx_alarm_record_hp_id` (`hazard_point_id`),
    KEY `idx_alarm_record_level` (`alarm_level`),
    KEY `idx_alarm_record_status` (`status`),
    KEY `idx_alarm_record_create_time` (`create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='告警记录表';

-- ----------------------------
-- 4.4 告警通知记录表 (alarm_notification)
-- ----------------------------
DROP TABLE IF EXISTS `alarm_notification`;
CREATE TABLE `alarm_notification`
(
    `id`              BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `alarm_id`        BIGINT NOT NULL COMMENT '告警记录ID',
    `recipient_id`    BIGINT NOT NULL COMMENT '接收人ID',
    `recipient_name`  VARCHAR(100) DEFAULT NULL COMMENT '接收人名称',
    `recipient_phone` VARCHAR(20)  DEFAULT NULL COMMENT '接收人电话',
    `channel`         VARCHAR(50)  DEFAULT NULL COMMENT '通知渠道',
    `status`          TINYINT      DEFAULT 1 COMMENT '状态: 1-待发送, 2-已发送, 3-发送失败',
    `send_time`       DATETIME     DEFAULT NULL COMMENT '发送时间',
    `error_msg`       VARCHAR(500) DEFAULT NULL COMMENT '错误信息',
    `create_by`       VARCHAR(64)  DEFAULT NULL COMMENT '创建者',
    `create_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_alarm_notification_alarm_id` (`alarm_id`),
    KEY `idx_alarm_notification_recipient_id` (`recipient_id`),
    KEY `idx_alarm_notification_status` (`status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='告警通知记录表';

-- ===============================================================
-- 五、监测数据模块
-- ===============================================================

-- ----------------------------
-- 5.1 监测数据表 (monitor_data)
-- ----------------------------
DROP TABLE IF EXISTS `monitor_data`;
CREATE TABLE `monitor_data`
(
    `id`              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `hazard_point_id` BIGINT         NOT NULL COMMENT '隐患点ID',
    `device_id`       BIGINT         NOT NULL COMMENT '设备ID',
    `device_code`     VARCHAR(100) DEFAULT NULL COMMENT '设备编号',
    `sensor_id`       BIGINT         NOT NULL COMMENT '传感器ID',
    `sensor_code`     VARCHAR(100) DEFAULT NULL COMMENT '传感器编号',
    `monitor_type_id` BIGINT         NOT NULL COMMENT '监测类型ID',
    `attr_code`       VARCHAR(100)   NOT NULL COMMENT '属性编码',
    `attr_name`       VARCHAR(200) DEFAULT NULL COMMENT '属性名称',
    `value`           DECIMAL(12, 2) NOT NULL COMMENT '监测值',
    `unit`            VARCHAR(50)  DEFAULT NULL COMMENT '单位',
    `direction`       VARCHAR(10)  DEFAULT NULL COMMENT '方向: X/Y/Z',
    `data_time`       DATETIME       NOT NULL COMMENT '数据时间',
    `quality`         TINYINT      DEFAULT 0 COMMENT '数据质量: 0-正常, 1-可疑, 2-无效',
    `create_by`       VARCHAR(64)  DEFAULT NULL COMMENT '创建者',
    `create_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_monitor_data_hp_id` (`hazard_point_id`),
    KEY `idx_monitor_data_device_id` (`device_id`),
    KEY `idx_monitor_data_sensor_id` (`sensor_id`),
    KEY `idx_monitor_data_data_time` (`data_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='监测数据表';

-- ----------------------------
-- 5.2 设备状态日志表 (device_status_log)
-- ----------------------------
DROP TABLE IF EXISTS `device_status_log`;
CREATE TABLE `device_status_log`
(
    `id`          BIGINT  NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `device_id`   BIGINT  NOT NULL COMMENT '设备ID',
    `device_code` VARCHAR(100) DEFAULT NULL COMMENT '设备编号',
    `old_status`  TINYINT      DEFAULT NULL COMMENT '旧状态',
    `new_status`  TINYINT NOT NULL COMMENT '新状态',
    `status_text` VARCHAR(50)  DEFAULT NULL COMMENT '状态文本',
    `remark`      VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `create_by`   VARCHAR(64)  DEFAULT NULL COMMENT '创建者',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
    PRIMARY KEY (`id`),
    KEY `idx_device_status_log_device_id` (`device_id`),
    KEY `idx_device_status_log_create_time` (`create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='设备状态日志表';

-- ===============================================================
-- 六、报告报表模块
-- ===============================================================

-- ----------------------------
-- 6.1 报告模板表 (report_template)
-- ----------------------------
DROP TABLE IF EXISTS `report_template`;
CREATE TABLE `report_template`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name`        VARCHAR(200) NOT NULL COMMENT '模板名称',
    `code`        VARCHAR(100) NOT NULL COMMENT '模板编码',
    `type`        TINYINT     DEFAULT 1 COMMENT '类型: 1-日报, 2-周报, 3-月报, 4-季报, 5-年报, 6-自定义',
    `content`     LONGTEXT    DEFAULT NULL COMMENT '模板内容(HTML)',
    `params`      TEXT        DEFAULT NULL COMMENT '参数配置(JSON)',
    `sort_order`  INT         DEFAULT 0 COMMENT '排序号',
    `status`      TINYINT     DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    `create_by`   VARCHAR(64) DEFAULT NULL COMMENT '创建者',
    `create_time` DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`   VARCHAR(64) DEFAULT NULL COMMENT '更新者',
    `update_time` DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `del_flag`    TINYINT     DEFAULT 0 COMMENT '删除标记: 0-正常, 1-删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_report_template_code` (`code`),
    KEY `idx_report_template_type` (`type`),
    KEY `idx_report_template_status` (`status`),
    KEY `idx_report_template_del_flag` (`del_flag`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='报告模板表';

-- ----------------------------
-- 6.2 报告记录表 (report_record)
-- ----------------------------
DROP TABLE IF EXISTS `report_record`;
CREATE TABLE `report_record`
(
    `id`                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `template_id`       BIGINT       NOT NULL COMMENT '模板ID',
    `template_name`     VARCHAR(200) DEFAULT NULL COMMENT '模板名称',
    `hazard_point_id`   BIGINT       DEFAULT NULL COMMENT '隐患点ID',
    `hazard_point_code` VARCHAR(100) DEFAULT NULL COMMENT '隐患点编号',
    `hazard_point_name` VARCHAR(200) DEFAULT NULL COMMENT '隐患点名称',
    `report_name`       VARCHAR(200) NOT NULL COMMENT '报告名称',
    `report_date`       DATETIME     NOT NULL COMMENT '报告日期',
    `content`           LONGTEXT     DEFAULT NULL COMMENT '报告内容(HTML)',
    `file_path`         VARCHAR(500) DEFAULT NULL COMMENT '文件路径',
    `status`            TINYINT      DEFAULT 1 COMMENT '状态: 1-生成中, 2-已生成, 3-生成失败',
    `create_by`         VARCHAR(64)  DEFAULT NULL COMMENT '创建者',
    `create_time`       DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`         VARCHAR(64)  DEFAULT NULL COMMENT '更新者',
    `update_time`       DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_report_record_template_id` (`template_id`),
    KEY `idx_report_record_hp_id` (`hazard_point_id`),
    KEY `idx_report_record_report_date` (`report_date`),
    KEY `idx_report_record_status` (`status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='报告记录表';

-- ===============================================================
-- 七、代码生成模块 (基于若依框架)
-- ===============================================================

-- ----------------------------
-- 7.1 代码生成业务表 (gen_table)
-- ----------------------------
DROP TABLE IF EXISTS `gen_table`;
CREATE TABLE `gen_table`
(
    `table_id`          bigint(20) NOT NULL AUTO_INCREMENT COMMENT '编号',
    `table_name`        varchar(200)  DEFAULT '' COMMENT '表名称',
    `table_comment`     varchar(500)  DEFAULT '' COMMENT '表描述',
    `sub_table_name`    varchar(64)   DEFAULT NULL COMMENT '关联子表的表名',
    `sub_table_fk_name` varchar(64)   DEFAULT NULL COMMENT '子表关联的外键名',
    `class_name`        varchar(100)  DEFAULT '' COMMENT '实体类名称',
    `tpl_category`      varchar(200)  DEFAULT 'crud' COMMENT '使用的模板（crud单表操作 tree树表操作）',
    `tpl_web_type`      varchar(30)   DEFAULT '' COMMENT '前端模板类型',
    `package_name`      varchar(100)  DEFAULT NULL COMMENT '生成包路径',
    `module_name`       varchar(30)   DEFAULT NULL COMMENT '生成模块名',
    `business_name`     varchar(30)   DEFAULT NULL COMMENT '生成业务名',
    `function_name`     varchar(50)   DEFAULT NULL COMMENT '生成功能名',
    `function_author`   varchar(50)   DEFAULT NULL COMMENT '生成功能作者',
    `form_col_num`      int(1)        DEFAULT 1 COMMENT '表单布局（单列 双列 三列）',
    `gen_type`          char(1)       DEFAULT '0' COMMENT '生成代码方式（0zip压缩包 1自定义路径）',
    `gen_path`          varchar(200)  DEFAULT '/' COMMENT '生成路径（不填默认项目路径）',
    `options`           varchar(1000) DEFAULT NULL COMMENT '其它生成选项',
    `create_by`         varchar(64)   DEFAULT '' COMMENT '创建者',
    `create_time`       datetime      DEFAULT NULL COMMENT '创建时间',
    `update_by`         varchar(64)   DEFAULT '' COMMENT '更新者',
    `update_time`       datetime      DEFAULT NULL COMMENT '更新时间',
    `remark`            varchar(500)  DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`table_id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1 COMMENT ='代码生成业务表';

-- ----------------------------
-- 7.2 代码生成业务表字段 (gen_table_column)
-- ----------------------------
DROP TABLE IF EXISTS `gen_table_column`;
CREATE TABLE `gen_table_column`
(
    `column_id`      bigint(20) NOT NULL AUTO_INCREMENT COMMENT '编号',
    `table_id`       bigint(20)   DEFAULT NULL COMMENT '归属表编号',
    `column_name`    varchar(200) DEFAULT NULL COMMENT '列名称',
    `column_comment` varchar(500) DEFAULT NULL COMMENT '列描述',
    `column_type`    varchar(100) DEFAULT NULL COMMENT '列类型',
    `java_type`      varchar(500) DEFAULT NULL COMMENT 'JAVA类型',
    `java_field`     varchar(200) DEFAULT NULL COMMENT 'JAVA字段名',
    `is_pk`          char(1)      DEFAULT NULL COMMENT '是否主键（1是）',
    `is_increment`   char(1)      DEFAULT NULL COMMENT '是否自增（1是）',
    `is_required`    char(1)      DEFAULT NULL COMMENT '是否必填（1是）',
    `is_insert`      char(1)      DEFAULT NULL COMMENT '是否为插入字段（1是）',
    `is_edit`        char(1)      DEFAULT NULL COMMENT '是否编辑字段（1是）',
    `is_list`        char(1)      DEFAULT NULL COMMENT '是否列表字段（1是）',
    `is_query`       char(1)      DEFAULT NULL COMMENT '是否查询字段（1是）',
    `query_type`     varchar(200) DEFAULT 'EQ' COMMENT '查询方式（等于、不等于，大于、小于、范围）',
    `html_type`      varchar(200) DEFAULT NULL COMMENT '显示类型（文本框、文本域、下拉框、复选框、单选框、日期控件）',
    `dict_type`      varchar(200) DEFAULT '' COMMENT '字典类型',
    `sort`           int          DEFAULT NULL COMMENT '排序',
    `create_by`      varchar(64)  DEFAULT '' COMMENT '创建者',
    `create_time`    datetime     DEFAULT NULL COMMENT '创建时间',
    `update_by`      varchar(64)  DEFAULT '' COMMENT '更新者',
    `update_time`    datetime     DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`column_id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1 COMMENT ='代码生成业务表字段';

-- ===============================================================
-- 初始化角色数据补充 (与业务表关联)
-- ===============================================================

-- 补充业务角色
INSERT INTO `sys_role` (`role_name`, `role_key`, `role_sort`, `data_scope`, `status`, `create_by`, `create_time`,
                        `remark`)
VALUES ('监测管理员', 'MONITOR', 3, 2, '0', 'admin', sysdate(), '监测业务管理员'),
       ('操作员', 'OPERATOR', 4, 3, '0', 'admin', sysdate(), '普通操作员');

-- 补充用户角色关联
INSERT INTO `sys_user_role` (`user_id`, `role_id`)
VALUES (1, 2);

COMMIT;

-- ===============================================================
-- 结束
-- ===============================================================
