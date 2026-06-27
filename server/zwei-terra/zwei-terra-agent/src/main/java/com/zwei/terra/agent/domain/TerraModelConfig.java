package com.zwei.terra.agent.domain;

import com.zwei.common.core.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.math.BigDecimal;

/** Terra 模型服务商配置 */
@Setter
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TerraModelConfig extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String name;

    private String baseUrl;

    private String apiKey;

    private String modelName;

    private Integer maxTokens;

    private BigDecimal temperature;

    private Integer isActive;

    private Integer sortOrder;

    private String delFlag;
}
