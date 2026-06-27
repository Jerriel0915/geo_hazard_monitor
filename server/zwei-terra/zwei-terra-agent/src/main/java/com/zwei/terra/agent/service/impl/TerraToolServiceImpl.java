package com.zwei.terra.agent.service.impl;

import com.zwei.common.exception.ServiceException;
import com.zwei.terra.agent.domain.TerraTool;
import com.zwei.terra.agent.mapper.TerraToolMapper;
import com.zwei.terra.agent.service.ITerraToolService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Terra 工具管理 Service 实现
 */
@Service
@Slf4j
public class TerraToolServiceImpl implements ITerraToolService {

    @Autowired
    private TerraToolMapper toolMapper;

    @Override
    public List<TerraTool> selectList() {
        return toolMapper.selectList(new TerraTool());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TerraTool create(TerraTool tool, String operator) {
        if (toolMapper.selectByKey(tool.getToolKey()) != null) {
            throw new ServiceException("工具标识已存在");
        }
        // config 来源工具可通过界面创建，code 来源只能通过代码注册
        tool.setSource("config");
        tool.setIsPreset(0);
        tool.setIsEnabled(1);
        tool.setCreateBy(operator);
        toolMapper.insert(tool);
        return tool;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(TerraTool tool, String operator) {
        TerraTool existing = toolMapper.selectById(tool.getId());
        if (existing == null) {
            throw new ServiceException("工具不存在");
        }
        tool.setSource(existing.getSource());
        tool.setUpdateBy(operator);
        toolMapper.update(tool);
    }

    @Override
    public void delete(Long id) {
        TerraTool existing = toolMapper.selectById(id);
        if (existing == null) {
            throw new ServiceException("工具不存在");
        }
        if ("code".equals(existing.getSource())) {
            throw new ServiceException("代码注册的工具不可删除");
        }
        toolMapper.deleteById(id);
    }

    @Override
    public void toggle(Long id, String operator) {
        TerraTool existing = toolMapper.selectById(id);
        if (existing == null) {
            throw new ServiceException("工具不存在");
        }
        existing.setIsEnabled(existing.getIsEnabled() == 1 ? 0 : 1);
        existing.setUpdateBy(operator);
        toolMapper.update(existing);
    }
}
