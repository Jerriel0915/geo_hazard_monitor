package com.zwei.terra.agent.domain;

import com.zwei.common.core.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.util.Date;

/** Terra 技能元数据 */
@Setter
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TerraSkill extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String skillKey;

    private String name;

    private String description;

    private String directoryPath;

    private String triggersSummary;

    private String toolsSummary;

    private Integer isPreset;

    private Integer isEnabled;

    private Date installedAt;

    private String installedBy;

    private Integer sortOrder;

    private String delFlag;
}
