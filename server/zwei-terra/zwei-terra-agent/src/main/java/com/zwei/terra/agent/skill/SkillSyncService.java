package com.zwei.terra.agent.skill;

import com.zwei.terra.agent.config.TerraProperties;
import com.zwei.terra.agent.domain.TerraSkill;
import com.zwei.terra.agent.mapper.TerraSkillMapper;
import com.zwei.terra.core.skill.SkillManifest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

/**
 * 预置技能同步服务，应用启动时扫描 preset/ 目录，将新增的 SKILL.md 同步到数据库。
 *
 * <p>仅在 {@code terra.skills.base-path/preset/} 目录存在时生效，
 * 已存在的 skill_key 不会被覆盖。</p>
 */
@Component
@Slf4j
public class SkillSyncService {

    @Autowired
    private TerraProperties properties;

    @Autowired
    private TerraSkillMapper skillMapper;

    @Autowired
    private SkillResolver skillResolver;

    /**
     * 应用启动后扫描预置技能目录，将未注册的技能写入数据库。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void syncPresetSkills() {
        Path presetDir = Paths.get(properties.getSkills().getBasePath(), "preset");
        if (!Files.exists(presetDir)) {
            log.info("Terra preset skills directory not found: {}", presetDir);
            return;
        }
        try (Stream<Path> dirs = Files.list(presetDir)) {
            dirs.filter(Files::isDirectory).forEach(this::syncOne);
        } catch (IOException e) {
            log.error("Failed to scan preset skills", e);
        }
    }

    /**
     * 同步单个预置技能目录。
     */
    private void syncOne(Path skillDir) {
        Path skillMd = skillDir.resolve("SKILL.md");
        if (!Files.exists(skillMd)) {
            return;
        }
        try {
            SkillManifest manifest = skillResolver.parse(skillMd);
            String skillKey = skillDir.getFileName().toString();
            TerraSkill existing = skillMapper.selectByKey(skillKey);
            if (existing == null) {
                TerraSkill skill = new TerraSkill();
                skill.setSkillKey(skillKey);
                skill.setName(manifest.getName());
                skill.setDescription(manifest.getDescription());
                skill.setDirectoryPath("preset/" + skillKey);
                skill.setIsPreset(1);
                skill.setIsEnabled(1);
                skill.setInstalledBy("system");
                skillMapper.insert(skill);
                log.info("Synced preset skill: {}", skillKey);
            }
        } catch (Exception e) {
            log.error("Failed to sync preset skill: {}", skillDir, e);
        }
    }
}
