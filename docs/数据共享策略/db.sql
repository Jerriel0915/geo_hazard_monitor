
DROP TABLE IF EXISTS share_strategy;
CREATE TABLE share_strategy (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    code VARCHAR(50) NOT NULL COMMENT '策略编号',
    name VARCHAR(100) NOT NULL COMMENT '策略名称',
    description VARCHAR(500) DEFAULT NULL COMMENT '策略描述',
    method VARCHAR(30) NOT NULL COMMENT '分享方式',
    address VARCHAR(200) NOT NULL COMMENT '地址',
    topic VARCHAR(200) DEFAULT NULL COMMENT 'MQTT主题',
    username VARCHAR(100) DEFAULT NULL COMMENT '认证账号',
    password VARCHAR(100) DEFAULT NULL COMMENT '认证密码',
    params TEXT DEFAULT NULL COMMENT '其他参数',
    scope_type VARCHAR(30) NOT NULL COMMENT '数据范围类型',
    scope_ids TEXT DEFAULT NULL COMMENT '范围ID列表',
    cron VARCHAR(50) NOT NULL COMMENT 'Cron表达式',
    status VARCHAR(20) NOT NULL DEFAULT 'DISABLED' COMMENT '策略状态',
    success_count INT DEFAULT 0 COMMENT '成功分享量',
    last_run_time DATETIME DEFAULT NULL COMMENT '最近运行时间',
    last_run_status VARCHAR(20) DEFAULT NULL COMMENT '最近运行状态',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code),
    KEY idx_status (status),
    KEY idx_method (method)
) ENGINE=InnoDB COMMENT='共享策略表';

DROP TABLE IF EXISTS share_strategy_log;
CREATE TABLE share_strategy_log (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    strategy_id BIGINT NOT NULL COMMENT '关联策略ID',
    run_time DATETIME NOT NULL COMMENT '运行时间',
    status VARCHAR(20) NOT NULL COMMENT '运行状态',
    message TEXT DEFAULT NULL COMMENT '运行信息',
    data_count INT DEFAULT 0 COMMENT '处理数据条数',
    duration INT DEFAULT NULL COMMENT '耗时',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_strategy_id (strategy_id),
    KEY idx_run_time (run_time)
) ENGINE=InnoDB COMMENT='共享策略运行日志表';

DROP TABLE IF EXISTS share_strategy_script;
CREATE TABLE share_strategy_script (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    strategy_id BIGINT NOT NULL COMMENT '关联策略ID',
    script LONGTEXT DEFAULT NULL COMMENT 'Blockly脚本内容',
    variables TEXT DEFAULT NULL COMMENT '变量配置',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_strategy_id (strategy_id)
) ENGINE=InnoDB COMMENT='共享策略脚本表';

INSERT INTO `geo_hazard_monitor`.`share_strategy` (`id`, `code`, `name`, `description`, `method`, `address`, `topic`, `username`, `password`, `params`, `scope_type`, `scope_ids`, `cron`, `status`, `success_count`, `last_run_time`, `last_run_status`, `create_time`, `update_time`) VALUES (1, 'TEST002', '规自局监测数据分享', '规自局监测数据分享', 'UNIFIED_PUSH', '192.168.1.100:8080', 'test/topic', 'admin', 'admin123', NULL, 'HAZARD_POINT', NULL, '0 0 * * * ?', 'ENABLED', 0, NULL, NULL, '2026-06-17 23:12:03', '2026-06-18 00:15:23');
INSERT INTO `geo_hazard_monitor`.`share_strategy` (`id`, `code`, `name`, `description`, `method`, `address`, `topic`, `username`, `password`, `params`, `scope_type`, `scope_ids`, `cron`, `status`, `success_count`, `last_run_time`, `last_run_status`, `create_time`, `update_time`) VALUES (2, 'TEST003', '城运中心监测数据分享', '城运中心监测数据分享', 'UNIFIED_PUSH', '172.168.1.109:8080', 'packge/data', 'zh', 'dd', '{}', 'HAZARD_POINT', '[]', '1', 'DISABLED', 0, NULL, NULL, '2026-06-18 07:38:29', '2026-06-18 00:15:42');

