package com.zwei.terra.agent.domain;

import com.zwei.common.core.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.io.Serial;

/** Terra 工具注册表 */
@Setter
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TerraTool extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String toolKey;

    private String name;

    private String description;

    /** 来源: code, config */
    private String source;

    /** 执行端: backend, frontend */
    private String execSide;

    /** 工具类型: query, action, navigate, report */
    private String toolType;

    /** JSON string */
    private String parametersSchema;

    private String endpoint;

    private Integer timeoutSeconds;

    private Integer isPreset;

    private Integer isEnabled;

    private Integer sortOrder;

    private String delFlag;
}
