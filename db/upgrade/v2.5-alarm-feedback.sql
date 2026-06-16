-- v2.5: 新增告警反馈记录表
CREATE TABLE `alarm_feedback`
(
    `id`          bigint       NOT NULL AUTO_INCREMENT,
    `alarm_id`    bigint       NOT NULL COMMENT '告警记录ID',
    `content`     text         COMMENT '反馈文本内容',
    `files`       json         DEFAULT NULL COMMENT '附件列表 [{name,url,size}]',
    `operator`    varchar(64)  DEFAULT NULL COMMENT '反馈人',
    `create_time` datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '反馈时间',
    PRIMARY KEY (`id`),
    KEY `idx_feedback_alarm_id` (`alarm_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='告警反馈记录';
