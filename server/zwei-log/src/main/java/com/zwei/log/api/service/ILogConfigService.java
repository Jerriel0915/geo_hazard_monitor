package com.zwei.log.api.service;

/**
 * 日志模块配置服务 — 封装 sys_config 表中 log.cleanup.* 命名空间的配置读写。
 */
public interface ILogConfigService {

    /**
     * 插入或更新配置项。
     *
     * @param configKey   配置键
     * @param configValue 配置值
     * @param remark      备注
     * @return 影响行数
     */
    int upsertConfig(String configKey, String configValue, String remark);
}
