package com.zwei.terra.agent.service.impl;

import com.zwei.common.exception.ServiceException;
import com.zwei.terra.agent.domain.TerraModelConfig;
import com.zwei.terra.agent.mapper.TerraModelConfigMapper;
import com.zwei.terra.agent.service.ITerraModelConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Terra 模型配置 Service 实现
 */
@Service
@Slf4j
public class TerraModelConfigServiceImpl implements ITerraModelConfigService {

    @Autowired
    private TerraModelConfigMapper configMapper;

    @Override
    public List<TerraModelConfig> selectList() {
        List<TerraModelConfig> list = configMapper.selectList(new TerraModelConfig());
        // 脱敏：列表返回时隐藏 apiKey
        for (TerraModelConfig c : list) {
            c.setApiKey(maskApiKey(c.getApiKey()));
        }
        return list;
    }

    @Override
    public TerraModelConfig selectById(Long id) {
        TerraModelConfig c = configMapper.selectById(id);
        if (c != null) {
            c.setApiKey(maskApiKey(c.getApiKey()));
        }
        return c;
    }

    @Override
    public TerraModelConfig getActiveConfig() {
        return configMapper.selectActive();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TerraModelConfig create(TerraModelConfig config, String operator) {
        if (configMapper.checkNameUnique(config.getName(), null) != null) {
            throw new ServiceException("配置名称已存在");
        }
        config.setCreateBy(operator);
        configMapper.insert(config);
        return config;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(TerraModelConfig config, String operator) {
        TerraModelConfig existing = configMapper.selectById(config.getId());
        if (existing == null) {
            throw new ServiceException("配置不存在");
        }
        if (configMapper.checkNameUnique(config.getName(), config.getId()) != null) {
            throw new ServiceException("配置名称已存在");
        }
        config.setUpdateBy(operator);
        configMapper.update(config);
    }

    @Override
    public void delete(Long id) {
        TerraModelConfig existing = configMapper.selectById(id);
        if (existing == null) {
            throw new ServiceException("配置不存在");
        }
        if (existing.getIsActive() == 1) {
            throw new ServiceException("不能删除已激活的配置，请先切换到其他配置");
        }
        configMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void activate(Long id, String operator) {
        TerraModelConfig config = configMapper.selectById(id);
        if (config == null) {
            throw new ServiceException("配置不存在");
        }
        configMapper.deactivateAll();
        TerraModelConfig update = new TerraModelConfig();
        update.setId(id);
        update.setIsActive(1);
        update.setUpdateBy(operator);
        configMapper.update(update);
    }

    /**
     * API Key 脱敏：前4位 + **** + 后4位
     */
    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() < 8) {
            return "****";
        }
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
    }
}
