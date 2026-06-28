ALTER TABLE algo_version ADD COLUMN work_path VARCHAR(500) DEFAULT NULL
    COMMENT '解压后的工作目录相对路径 (相对于 RuoYiConfig.profile)';
