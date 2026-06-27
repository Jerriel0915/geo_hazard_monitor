package com.zwei.terra.agent.skill;

import com.zwei.terra.core.skill.SkillManifest;
import com.zwei.terra.core.skill.SkillTrigger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SKILL.md 解析器，使用正则解析 YAML front matter + Markdown body。
 *
 * <p>避免引入 SnakeYAML 依赖，仅解析 name / description / triggers / tools 等必要字段。</p>
 */
@Component
@Slf4j
public class SkillResolver {

    private static final Pattern FRONT_MATTER =
        Pattern.compile("^---\\s*\\n(.*?)\\n---\\s*\\n(.*)$", Pattern.DOTALL);

    /**
     * 解析 SKILL.md 文件为 {@link SkillManifest}。
     *
     * @param skillMdPath SKILL.md 文件路径
     * @return 解析后的 SkillManifest
     * @throws IOException              文件读取失败
     * @throws IllegalArgumentException YAML front matter 缺失
     */
    public SkillManifest parse(Path skillMdPath) throws IOException {
        String content = Files.readString(skillMdPath);
        Matcher m = FRONT_MATTER.matcher(content);
        if (!m.find()) {
            throw new IllegalArgumentException(
                "Invalid SKILL.md: missing YAML front matter in " + skillMdPath);
        }
        String yaml = m.group(1);
        String body = m.group(2).trim();

        SkillManifest.SkillManifestBuilder b = SkillManifest.builder().instructions(body);

        List<SkillTrigger> triggers = new ArrayList<>();
        List<String> tools = new ArrayList<>();
        String currentSection = null;

        for (String line : yaml.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("name:")) {
                b.name(trimmed.substring(5).trim());
            } else if (trimmed.startsWith("description:")) {
                b.description(trimmed.substring(12).trim());
            } else if (trimmed.equals("triggers:")) {
                currentSection = "triggers";
            } else if (trimmed.equals("tools:")) {
                currentSection = "tools";
            } else if (trimmed.startsWith("- ") && currentSection != null) {
                String item = trimmed.substring(2).trim();
                if (currentSection.equals("tools")) {
                    tools.add(item);
                }
            }
        }
        b.triggers(triggers);
        b.tools(tools);
        return b.build();
    }
}
