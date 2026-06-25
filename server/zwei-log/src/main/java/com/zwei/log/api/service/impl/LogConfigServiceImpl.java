package com.zwei.log.api.service.impl;

import com.zwei.log.api.service.ILogConfigService;
import com.zwei.log.infrastructure.persistence.mysql.LogConfigMapper;
import org.springframework.stereotype.Service;

/**
 * 日志模块配置服务实现。
 */
@Service
public class LogConfigServiceImpl implements ILogConfigService {

    private final LogConfigMapper mapper;

    public LogConfigServiceImpl(LogConfigMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public int upsertConfig(String configKey, String configValue, String remark) {
        return mapper.upsertConfig(configKey, configValue, remark);
    }
}
