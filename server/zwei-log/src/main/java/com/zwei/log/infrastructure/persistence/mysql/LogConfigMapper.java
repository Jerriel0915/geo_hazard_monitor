package com.zwei.log.infrastructure.persistence.mysql;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 日志模块专用配置读写。
 * <p>
 * 直接操作 sys_config 表中 log.cleanup.* 命名空间下的配置项，
 * 配合 RedisCache 实现热更新配置的持久化。
 */
@Mapper
public interface LogConfigMapper {
    int upsertConfig(@Param("configKey") String configKey,
                     @Param("configValue") String configValue,
                     @Param("remark") String remark);
}
