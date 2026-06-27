package com.zwei.terra.agent.service;

import com.zwei.terra.agent.domain.TerraTool;

import java.util.List;

/**
 * Terra 工具管理 Service
 */
public interface ITerraToolService {

    /**
     * 查询全部工具
     */
    List<TerraTool> selectList();

    /**
     * 新建工具（source 强制为 config）
     */
    TerraTool create(TerraTool tool, String operator);

    /**
     * 更新工具
     */
    void update(TerraTool tool, String operator);

    /**
     * 删除工具（仅限 source=config）
     */
    void delete(Long id);

    /**
     * 切换工具启用/停用状态
     */
    void toggle(Long id, String operator);
}
