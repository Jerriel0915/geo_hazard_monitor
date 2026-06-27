package com.zwei.terra.core.skill;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillManifest {
    private String name;
    private String description;
    private List<SkillTrigger> triggers;
    private List<String> tools;
    private String instructions;
}
