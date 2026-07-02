package com.zwei.iot.parser.dto;

import lombok.Data;
import java.util.List;

@Data
public class DataParseStrategyDTO {
    private Long id;
    private String name;
    private String sourceType;
    private String description;
    private Integer status;
    private String appScope;
    /** MQTT 服务地址（仅描述展示用） */
    private String serverUrl;
    /** 订阅主题（仅描述展示用） */
    private String topic;
    private String scriptCode;
    private List<Long> vendorIds;
    private List<Long> deviceIds;
}
