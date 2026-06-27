package com.zwei.terra.agent.service.impl;

import com.zwei.common.exception.ServiceException;
import com.zwei.terra.agent.domain.TerraPersonality;
import com.zwei.terra.agent.mapper.TerraPersonalityMapper;
import com.zwei.terra.agent.service.ITerraPersonalityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Terra 人格配置 Service 实现
 */
@Service
@Slf4j
public class TerraPersonalityServiceImpl implements ITerraPersonalityService {

    @Autowired
    private TerraPersonalityMapper personalityMapper;

    @Override
    public List<TerraPersonality> selectList() {
        return personalityMapper.selectList(new TerraPersonality());
    }

    @Override
    public String buildSystemPrompt() {
        StringBuilder sb = new StringBuilder();
        TerraPersonality core = personalityMapper.selectActiveCore();
        if (core != null) {
            sb.append(core.getContent());
        }
        List<TerraPersonality> roles = personalityMapper.selectActiveRoles();
        for (TerraPersonality role : roles) {
            sb.append("\n\n").append(role.getContent());
        }
        return sb.toString();
    }

    @Override
    public void updateRole(TerraPersonality personality, String operator) {
        TerraPersonality existing = personalityMapper.selectById(personality.getId());
        if (existing == null) {
            throw new ServiceException("人格配置不存在");
        }
        if ("core".equals(existing.getLayerType()) && existing.getIsPreset() == 1) {
            // 核心灵魂预设：只能改 content
            existing.setContent(personality.getContent());
        } else {
            // role 类型：可以改 name/content/sortOrder
            existing.setName(personality.getName());
            existing.setContent(personality.getContent());
            existing.setSortOrder(personality.getSortOrder());
        }
        existing.setUpdateBy(operator);
        personalityMapper.update(existing);
    }

    @Override
    public void toggleActive(Long id, String operator) {
        TerraPersonality p = personalityMapper.selectById(id);
        if (p == null) {
            throw new ServiceException("人格配置不存在");
        }
        if ("core".equals(p.getLayerType()) && p.getIsPreset() == 1) {
            throw new ServiceException("核心灵魂不可停用");
        }
        p.setIsActive(p.getIsActive() == 1 ? 0 : 1);
        p.setUpdateBy(operator);
        personalityMapper.update(p);
    }
}
