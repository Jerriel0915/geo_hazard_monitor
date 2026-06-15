package com.zwei.iot.parser.domain;

import com.zwei.common.core.domain.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Setter
@Getter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class DataParseStrategy extends BaseEntity {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String name;
    private String sourceType;
    private String description;
    private Integer status;
    private String appScope;
    private String scriptCode;
    private Integer isPreset;
    private String lastRunTime;
}
