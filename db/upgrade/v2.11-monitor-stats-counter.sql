-- v2.11 累计监测数据点计数器持久化
-- Redis 热路径 INCR + 定时同步至 MySQL，启动时从 MySQL 回填 Redis

CREATE TABLE IF NOT EXISTS `monitor_stats` (
    `id`          bigint NOT NULL AUTO_INCREMENT,
    `stat_key`    varchar(64)  NOT NULL COMMENT '统计键',
    `stat_value`  bigint       NOT NULL DEFAULT 0 COMMENT '统计值',
    `update_time` datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_stat_key` (`stat_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='监测统计数据持久化表';

-- 预置累计监测次数行
INSERT IGNORE INTO `monitor_stats` (`stat_key`, `stat_value`) VALUES ('total_monitor_count', 0);
