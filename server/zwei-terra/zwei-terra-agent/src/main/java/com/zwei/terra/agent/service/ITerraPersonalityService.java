package com.zwei.terra.agent.service;

import com.zwei.terra.agent.domain.TerraPersonality;

import java.util.List;

/**
 * Terra 人格配置 Service
 */
public interface ITerraPersonalityService {

    /**
     * 查询全部人格配置列表
     */
    List<TerraPersonality> selectList();

    /**
     * 构建系统提示词：active core + active roles 按 sort_order 拼接
     */
    String buildSystemPrompt();

    /**
     * 更新人格配置内容
     */
    void updateRole(TerraPersonality personality, String operator);

    /**
     * 切换启用/停用状态（core preset 不允许停用）
     */
    void toggleActive(Long id, String operator);
}
