package com.zwei.terra.agent.domain;

import com.zwei.common.core.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.io.Serial;

/** Terra 人格配置 */
@Setter
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TerraPersonality extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    /** 层级类型: core, role */
    private String layerType;

    private String name;

    private String content;

    private Integer isActive;

    private Integer isPreset;

    private Integer sortOrder;

    private String delFlag;
}
