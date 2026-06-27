package com.zwei.terra.agent.service;

import com.zwei.terra.agent.domain.TerraSkill;

import java.util.List;

/**
 * Terra 技能管理 Service
 */
public interface ITerraSkillService {

    /**
     * 查询全部技能
     */
    List<TerraSkill> selectList();

    /**
     * 根据 ID 查询技能
     */
    TerraSkill selectById(Long id);

    /**
     * 切换技能启用/停用状态（预置技能不可停用）
     */
    void toggle(Long id, String operator);

    /**
     * 卸载技能（预置技能不可卸载）
     */
    void uninstall(Long id, String operator);
}
