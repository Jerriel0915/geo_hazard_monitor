package com.zwei.terra.agent.service;

import com.zwei.terra.agent.domain.TerraModelConfig;

import java.util.List;

/**
 * Terra 模型配置 Service
 */
public interface ITerraModelConfigService {

    /**
     * 查询全部模型配置（apiKey 脱敏）
     */
    List<TerraModelConfig> selectList();

    /**
     * 根据 ID 查询（apiKey 脱敏）
     */
    TerraModelConfig selectById(Long id);

    /**
     * 获取当前激活的配置（含完整 apiKey，供内部调用）
     */
    TerraModelConfig getActiveConfig();

    /**
     * 新建模型配置
     */
    TerraModelConfig create(TerraModelConfig config, String operator);

    /**
     * 更新模型配置
     */
    void update(TerraModelConfig config, String operator);

    /**
     * 删除模型配置（已激活的不可删除）
     */
    void delete(Long id);

    /**
     * 激活指定配置（先取消所有激活，再激活目标）
     */
    void activate(Long id, String operator);
}
