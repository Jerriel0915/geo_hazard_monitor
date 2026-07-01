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
    /** MQTT 服务地址（仅描述展示用，不参与策略匹配） */
    private String serverUrl;
    /** 订阅主题（仅描述展示用，不参与策略匹配） */
    private String topic;
    private String scriptCode;
    private Integer isPreset;
    private String lastRunTime;
}
