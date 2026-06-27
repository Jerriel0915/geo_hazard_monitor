package com.zwei.terra.agent.service.impl;

import com.zwei.common.exception.ServiceException;
import com.zwei.terra.agent.config.TerraProperties;
import com.zwei.terra.agent.domain.TerraSkill;
import com.zwei.terra.agent.mapper.TerraSkillMapper;
import com.zwei.terra.agent.service.ITerraSkillService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Terra 技能管理 Service 实现
 */
@Service
@Slf4j
public class TerraSkillServiceImpl implements ITerraSkillService {

    @Autowired
    private TerraSkillMapper skillMapper;

    @Autowired
    private TerraProperties properties;

    @Override
    public List<TerraSkill> selectList() {
        return skillMapper.selectList(new TerraSkill());
    }

    @Override
    public TerraSkill selectById(Long id) {
        return skillMapper.selectById(id);
    }

    @Override
    public void toggle(Long id, String operator) {
        TerraSkill skill = skillMapper.selectById(id);
        if (skill == null) {
            throw new ServiceException("技能不存在");
        }
        if (skill.getIsPreset() == 1) {
            throw new ServiceException("预置技能不可停用");
        }
        skill.setIsEnabled(skill.getIsEnabled() == 1 ? 0 : 1);
        skill.setUpdateBy(operator);
        skillMapper.update(skill);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void uninstall(Long id, String operator) {
        TerraSkill skill = skillMapper.selectById(id);
        if (skill == null) {
            throw new ServiceException("技能不存在");
        }
        if (skill.getIsPreset() == 1) {
            throw new ServiceException("预置技能不可卸载");
        }
        // 删除技能目录
        if (skill.getDirectoryPath() != null) {
            Path skillDir = Paths.get(properties.getSkills().getBasePath(), skill.getDirectoryPath());
            try {
                deleteRecursively(skillDir);
            } catch (IOException e) {
                log.error("Failed to delete skill directory: {}", skillDir, e);
                throw new ServiceException("删除技能目录失败");
            }
        }
        skillMapper.deleteById(id);
        log.info("Skill uninstalled: key={}, operator={}", skill.getSkillKey(), operator);
    }

    /**
     * 递归删除目录及其内容
     */
    private void deleteRecursively(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (var entries = Files.list(path)) {
                entries.forEach(entry -> {
                    try {
                        deleteRecursively(entry);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }
        Files.deleteIfExists(path);
    }
}
